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
#   projects.list        the resolved project list, reused when resuming
#
# The project list is not a checked-in file. oss_fuzz.select_projects samples it
# from the OSS-Fuzz checkout, so NUM_PROJECTS/SELECT_SEED reproduce a sweep and
# nobody has to maintain a list by hand. The resolved names are written to
# projects.list in the run directory, and a resumed run reuses that file rather
# than re-sampling -- otherwise pulling the checkout mid-sweep would silently
# change which projects the run was about.
#
# Usage:
#   ./run_ossfuzz_suite.sh                     # 20 C++ projects, seed 42
#   ./run_ossfuzz_suite.sh libxml2 expat       # just these
#   ./run_ossfuzz_suite.sh -f suites/my.projects            # an explicit list
#   NUM_PROJECTS=5 SELECT_SEED=7 ./run_ossfuzz_suite.sh     # a different sample
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
MIN_FREE_GB="${MIN_FREE_GB:-40}"            # refuse to start below this
NUM_PROJECTS="${NUM_PROJECTS:-5}"          # projects to sample when none given
SELECT_SEED="${SELECT_SEED:-42}"            # sampling seed; fixes which ones
export OSS_FUZZ_DIR="${OSS_FUZZ_DIR:-$ROOT_DIR/oss-fuzz}"
# Clones and vuln/HEAD worktrees. config.py defaults this to ~/.cache, which on
# this box is the small root disk; a sweep needs tens of GB, so keep it on the
# data disk. Kept outside $ROOT_DIR so it stays out of the git tree.
export OSS_FUZZ_WORK_DIR="${OSS_FUZZ_WORK_DIR:-/datadrive/vuln-patch-cache/oss-fuzz}"

# --- 2. command line --------------------------------------------------------
PROJECTS_FILE=""                            # -f: an explicit list, else sample
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

# --- 3. preflight -----------------------------------------------------------
# Fail the whole sweep in seconds rather than one project at a time, hours in.
# Environment only; the per-project checks wait until section 5 has a list.
[ -f "$OSS_FUZZ_DIR/infra/helper.py" ] || {
  echo "FATAL: \$OSS_FUZZ_DIR ('$OSS_FUZZ_DIR') is not a google/oss-fuzz clone" >&2
  echo "  git clone --depth 1 https://github.com/google/oss-fuzz $ROOT_DIR/oss-fuzz" >&2
  exit 1
}
command -v uv >/dev/null || { echo "FATAL: uv not on PATH" >&2; exit 1; }
if [ "$DRY_RUN" -eq 0 ]; then
  docker info >/dev/null 2>&1 || { echo "FATAL: Docker daemon not reachable" >&2; exit 1; }
fi

# A sweep pulls tens of GB of base images and writes clones, worktrees and
# build/out trees. Out of space shows up as 'no space left on device' from
# whatever ran first -- a docker pull, a git clone -- so check up front.
# Careful when reading 'docker info': with the containerd image store
# (driver-type io.containerd.snapshotter.v1) layers land under containerd's
# own root, NOT the 'data-root' that Docker Root Dir reports. Both live on
# the data disk here, which is the filesystem OSS_FUZZ_WORK_DIR checks.
mkdir -p "$OSS_FUZZ_WORK_DIR" || exit 1
for path in "$OSS_FUZZ_WORK_DIR" "$OSS_FUZZ_DIR"; do
  avail="$(df -BG --output=avail "$path" 2>/dev/null | tail -1 | tr -dc '0-9')"
  [ -n "$avail" ] || continue
  [ "$avail" -ge "$MIN_FREE_GB" ] || {
    echo "FATAL: only ${avail}G free on the filesystem holding '$path'" >&2
    echo "  need >= ${MIN_FREE_GB}G (override with MIN_FREE_GB)" >&2
    exit 1; }
done

# --- 4. create the run directory --------------------------------------------
# -o an existing directory to resume: projects with a .rc there are skipped.
[ -n "$RUN_DIR" ] || RUN_DIR="$ROOT_DIR/runs/ossfuzz_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RUN_DIR/logs" || exit 1
RUN_DIR="$(cd "$RUN_DIR" && pwd)"
RESULTS="$RUN_DIR/results.jsonl"
SUMMARY="$RUN_DIR/summary.md"
PROJECTS_LIST="$RUN_DIR/projects.list"

# --- 5. resolve the project list ---------------------------------------------
# Precedence: positional args, then -f, then this run's own projects.list, then
# a fresh sample. Positionals win so one project can be re-run without editing
# anything; projects.list beats sampling so resuming a run cannot change what
# the run was about, even if the OSS-Fuzz checkout moved underneath it.
read_list() {                            # strip '#' comments and whitespace
  while IFS= read -r line; do
    line="${line%%#*}"
    line="${line//[[:space:]]/}"
    [ -n "$line" ] && PROJECTS+=("$line")
  done < "$1"
}

PROJECTS=("$@")
if [ "${#PROJECTS[@]}" -eq 0 ] && [ -n "$PROJECTS_FILE" ]; then
  [ -f "$PROJECTS_FILE" ] || { echo "FATAL: no project list '$PROJECTS_FILE'" >&2; exit 1; }
  read_list "$PROJECTS_FILE"
fi
if [ "${#PROJECTS[@]}" -eq 0 ] && [ -s "$PROJECTS_LIST" ]; then
  echo "reusing the selection already recorded in $PROJECTS_LIST"
  read_list "$PROJECTS_LIST"
fi
if [ "${#PROJECTS[@]}" -eq 0 ]; then
  echo "sampling $NUM_PROJECTS C++ projects (seed $SELECT_SEED)"
  # Names on stdout, provenance on stderr (so it lands in the terminal). The
  # subshell cd is what lets 'uv run -m' resolve the package without moving
  # this script's cwd, which section 6 still needs.
  mapfile -t PROJECTS < <(cd "$ROOT_DIR/src" && uv run -m oss_fuzz.select_projects \
                            -n "$NUM_PROJECTS" --seed "$SELECT_SEED")
fi
[ "${#PROJECTS[@]}" -gt 0 ] || { echo "FATAL: no projects to run" >&2; exit 1; }

for p in "${PROJECTS[@]}"; do
  [ -f "$OSS_FUZZ_DIR/projects/$p/project.yaml" ] || {
    echo "FATAL: '$p' is not in the oss-fuzz checkout" >&2; exit 1; }
done

# Written before the first (long) project, so an interrupted sweep still records
# what it set out to do.
printf '%s\n' "${PROJECTS[@]}" > "$PROJECTS_LIST"

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
  echo "Projects: ${#PROJECTS[@]} (seed $SELECT_SEED, oss-fuzz \`$(git -C "$OSS_FUZZ_DIR" rev-parse --short HEAD 2>/dev/null || echo unknown)\`)."
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
