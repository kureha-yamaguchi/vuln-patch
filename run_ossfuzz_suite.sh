#!/usr/bin/env bash
#
# Run the OSS-Fuzz sibling-bug pipeline over a list of C++ projects, one project
# at a time, keeping a log and an exit code for each.
#
# Crashing bugs only (--skip-semantic): a project whose newest disclosed bugs
# are all non-crashing is reported as 'no-target' instead of being run with a
# harness that could never fire. See src/oss_fuzz/README.md.
#
# Output: runs/ossfuzz_<YYYYMMDD_HHMMSS>/
#   logs/<project>.log   full stdout+stderr of the run
#   logs/<project>.rc    its exit code (also the "already done" marker)
#   results.jsonl        appended by the pipeline itself
#   summary.md           status table
#
# Usage:
#   ./run_ossfuzz_suite.sh                     # the default project list
#   ./run_ossfuzz_suite.sh libxml2 expat       # just these
#   ./run_ossfuzz_suite.sh -f suites/my.projects
#   ./run_ossfuzz_suite.sh -d                  # dry run: no Docker/LLM/network
#   ./run_ossfuzz_suite.sh -o runs/ossfuzz_20260810_120000   # resume that dir
#
# Not parallel on purpose: every run builds into the single shared
# $OSS_FUZZ_DIR/build/out tree, so two at once fight over Docker and the disk.
# Start long sweeps under tmux or nohup.

set -uo pipefail

# --- 1. settings ------------------------------------------------------------
# Pipeline tuning lives here; override any of them from the environment.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_SUCCESSES="${TARGET_SUCCESSES:-3}"   # accepted harnesses per project
MAX_ATTEMPTS="${MAX_ATTEMPTS:-15}"          # LLM calls per project
VERIFY_TIMEOUT="${VERIFY_TIMEOUT:-120}"     # secs per harness, vulnerable build
FUZZ_TIMEOUT="${FUZZ_TIMEOUT:-600}"         # secs per accepted harness on HEAD
MAX_TARGET_TRIES="${MAX_TARGET_TRIES:-8}"   # OSV records to walk per project
PROJECT_TIMEOUT="${PROJECT_TIMEOUT:-7200}"  # hard wall-clock cap per project
export OSS_FUZZ_DIR="${OSS_FUZZ_DIR:-$ROOT_DIR/oss-fuzz}"

# --- 2. command line --------------------------------------------------------
PROJECTS_FILE="$ROOT_DIR/suites/ossfuzz_cpp20.projects"
RUN_DIR=""
DRY_RUN=0

while getopts "f:o:dh" opt; do
  case "$opt" in
    f) PROJECTS_FILE="$OPTARG" ;;
    o) RUN_DIR="$OPTARG" ;;
    d) DRY_RUN=1 ;;
    h) sed -n '2,/^$/s/^# \?//p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "try -h" >&2; exit 1 ;;
  esac
done
shift $((OPTIND - 1))

# --- 3. build the project list ----------------------------------------------
# Positional arguments win over the file, so one project can be re-run without
# editing anything.
PROJECTS=("$@")
if [ "${#PROJECTS[@]}" -eq 0 ]; then
  [ -f "$PROJECTS_FILE" ] || { echo "FATAL: no project list '$PROJECTS_FILE'" >&2; exit 1; }
  while IFS= read -r line; do
    line="${line%%#*}"                                # strip comments
    line="${line//[[:space:]]/}"
    [ -n "$line" ] && PROJECTS+=("$line")
  done < "$PROJECTS_FILE"
fi
[ "${#PROJECTS[@]}" -gt 0 ] || { echo "FATAL: no projects to run" >&2; exit 1; }

# --- 4. preflight -----------------------------------------------------------
# Fail the whole sweep in seconds rather than one project at a time, hours in.
[ -f "$OSS_FUZZ_DIR/infra/helper.py" ] || {
  echo "FATAL: \$OSS_FUZZ_DIR ('$OSS_FUZZ_DIR') is not a google/oss-fuzz clone" >&2
  echo "  git clone --depth 1 https://github.com/google/oss-fuzz $ROOT_DIR/oss-fuzz" >&2
  exit 1
}
command -v uv >/dev/null || { echo "FATAL: uv not on PATH" >&2; exit 1; }
if [ "$DRY_RUN" -eq 0 ]; then
  docker info >/dev/null 2>&1 || { echo "FATAL: Docker daemon not reachable" >&2; exit 1; }
fi
for p in "${PROJECTS[@]}"; do
  [ -f "$OSS_FUZZ_DIR/projects/$p/project.yaml" ] || {
    echo "FATAL: '$p' is not in the oss-fuzz checkout" >&2; exit 1; }
done

# --- 5. create the run directory --------------------------------------------
# -o an existing directory to resume: projects with a .rc there are skipped.
[ -n "$RUN_DIR" ] || RUN_DIR="$ROOT_DIR/runs/ossfuzz_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RUN_DIR/logs" || exit 1
RUN_DIR="$(cd "$RUN_DIR" && pwd)"
RESULTS="$RUN_DIR/results.jsonl"
SUMMARY="$RUN_DIR/summary.md"

# --- 6. run each project ----------------------------------------------------
# The pipeline's exit code IS the result, so it is recorded per project:
# 3 = confirmed siblings, 4 = unconfirmed oracle claims, 2 = broken environment,
# 124 = our cap fired, 1 = catch-all (usually "nothing eligible to run on").
status_for() {
  local rc="$1" log="$2"
  case "$rc" in
    0) echo "clean" ;;
    2) echo "infra-error" ;;
    3) echo "SIBLINGS" ;;
    4) echo "oracle-claims" ;;
    124|137) echo "timeout" ;;
    1) if grep -qE 'No usable public|None of the .* OSV record|is not a target' "$log" 2>/dev/null
       then echo "no-target"; else echo "error(1)"; fi ;;
    *) echo "error($rc)" ;;
  esac
}

CMD=(uv run -m oss_fuzz.run --skip-semantic
     -n "$TARGET_SUCCESSES" -m "$MAX_ATTEMPTS"
     --verify-timeout "$VERIFY_TIMEOUT" --fuzz-timeout "$FUZZ_TIMEOUT"
     --max-target-tries "$MAX_TARGET_TRIES" --results-json "$RESULTS")
[ "$DRY_RUN" -eq 1 ] && CMD+=(--dry-run)

cd "$ROOT_DIR/src" || exit 1
declare -A STATUS=()
i=0
for p in "${PROJECTS[@]}"; do
  i=$((i + 1))
  LOG="$RUN_DIR/logs/$p.log"
  RC_FILE="$RUN_DIR/logs/$p.rc"

  if [ -f "$RC_FILE" ]; then
    STATUS[$p]="$(status_for "$(cat "$RC_FILE")" "$LOG")"
    echo "[$i/${#PROJECTS[@]}] $p — already done (${STATUS[$p]}), skipping"
    continue
  fi

  echo
  echo "=== [$i/${#PROJECTS[@]}] $p  ($(date +%H:%M:%S), cap ${PROJECT_TIMEOUT}s) ==="
  # --foreground so Ctrl-C reaches this script too; -k KILLs a build that
  # ignores TERM.
  timeout --foreground -k 60 "$PROJECT_TIMEOUT" "${CMD[@]}" --project "$p" 2>&1 | tee "$LOG"
  rc="${PIPESTATUS[0]}"

  echo "$rc" > "$RC_FILE"
  STATUS[$p]="$(status_for "$rc" "$LOG")"
  echo "--> $p: ${STATUS[$p]} (exit $rc) — log: $LOG"
done

# --- 7. write the summary ---------------------------------------------------
{
  echo "# OSS-Fuzz crashing-bug suite — $(basename "$RUN_DIR")"
  echo
  echo "\`-n $TARGET_SUCCESSES -m $MAX_ATTEMPTS --skip-semantic\`, cap ${PROJECT_TIMEOUT}s/project."
  echo
  echo "| project | status |"
  echo "|---|---|"
  for p in "${PROJECTS[@]}"; do
    echo "| $p | ${STATUS[$p]:-not-run} |"
  done
} > "$SUMMARY"

echo
echo "suite done — $RUN_DIR"
printf '  %-16s %s\n' "summary:" "$SUMMARY"
printf '  %-16s %s\n' "results:" "$RESULTS"

# Exit 3 if any project found a confirmed sibling, mirroring the pipeline's own
# code so a wrapper can branch on the sweep the same way.
for p in "${PROJECTS[@]}"; do
  [ "${STATUS[$p]:-}" = "SIBLINGS" ] && exit 3
done
exit 0
