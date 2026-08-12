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
#   artifacts/<project>/ the evidence behind that project's line in the table:
#     inputs/            what the harness generator was steered by — the fix
#                        diff, the original bug's triggering evidence (crash
#                        type + crashing stack, and the PoC when --reproducer
#                        supplied one), and the extracted reachable-function
#                        set, plus generation-input.json tying them together
#     prompts/           the exact messages sent to the LLM, one file per
#                        attempt (they differ: the campaign re-steers them)
#     harnesses/         every generated harness, accepted or not
#     fuzz/              the fuzzing engine's own output, per run:
#                        verify_* on the vulnerable build (the acceptance
#                        gate), head_* on HEAD (the sibling claim)
#     crashes/<run>/     the input each run stopped on, copied out of
#                        build/out/ before the next build wipes it
#     build/             compiler output for builds that failed
#
# logs/<project>.log is the pipeline's commentary; artifacts/<project>/ is what
# that commentary is about. A sibling claim in the table cites a signature the
# log prints in one line — the sanitizer report behind it, and the engine stats
# behind every harness that instead ran clean, are only in fuzz/.
#
# The project list is not a checked-in file. oss_fuzz.select_projects derives it
# from the OSS-Fuzz checkout, so nobody has to maintain a list by hand: by
# default it takes the projects whose newest disclosed OSV record is the most
# recent (SELECT_ORDER=shuffle for the old seeded sample). The resolved names are
# written to projects.list in the run directory, and a resumed run reuses that
# file rather than re-selecting -- otherwise pulling the checkout, or OSV
# disclosing a bug mid-sweep, would silently change what the run was about.
#
# Usage:
#   ./run_ossfuzz_suite.sh                     # the 5 freshest C++ projects
#   ./run_ossfuzz_suite.sh libxml2 expat       # just these
#   ./run_ossfuzz_suite.sh -f suites/my.projects            # an explicit list
#   NUM_PROJECTS=20 ./run_ossfuzz_suite.sh                  # the 20 freshest
#   SELECT_ORDER=shuffle SELECT_SEED=7 ./run_ossfuzz_suite.sh   # a random sample
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
MAX_ATTEMPTS="${MAX_ATTEMPTS:-30}"          # LLM calls per project
VERIFY_TIMEOUT="${VERIFY_TIMEOUT:-120}"     # secs per harness, vulnerable build
FUZZ_TIMEOUT="${FUZZ_TIMEOUT:-600}"         # secs per accepted harness on HEAD
MAX_TARGET_TRIES="${MAX_TARGET_TRIES:-8}"   # OSV records to walk per project
PROJECT_TIMEOUT="${PROJECT_TIMEOUT:-7200}"  # hard wall-clock cap per project
MIN_FREE_GB="${MIN_FREE_GB:-40}"            # refuse to start below this
NUM_PROJECTS="${NUM_PROJECTS:-5}"           # projects to select when none given
# 'recent' takes the projects whose newest disclosed bug is the freshest, which
# is what keeps a run off the source/build-recipe skew that costs a slot before
# a harness is even written (llamacpp, 20260811). 'shuffle' is the old seeded
# sample, for an unbiased look at the ecosystem; SELECT_SEED only bites there.
SELECT_ORDER="${SELECT_ORDER:-recent}"      # recent | shuffle
SELECT_SEED="${SELECT_SEED:-42}"            # shuffle seed; fixes which ones
export OSS_FUZZ_DIR="${OSS_FUZZ_DIR:-$ROOT_DIR/oss-fuzz}"
# Clones and vuln/HEAD worktrees. config.py defaults this to ~/.cache, which on
# this box is the small root disk; a sweep needs tens of GB, so keep it on the
# data disk. Kept outside $ROOT_DIR so it stays out of the git tree.
export OSS_FUZZ_WORK_DIR="${OSS_FUZZ_WORK_DIR:-/datadrive/vuln-patch-cache/oss-fuzz}"

# An OSS-fuzz project.yaml main_repo that has been deleted, renamed or made private is indistinguishable from one needing auth --
# GitHub answers both by asking for a username -- so an unattended sweep parks on
# a password prompt until it is killed. cryptofuzz is the live example: its repo
# 404s, and no credential exists that would clone it.
#   PROMPT=0      : fail immediately instead of prompting.
#   credential.*  : blank the helper for every child git. ~/.gitconfig sets
#                   'store' here, and a stale token in it answers the prompt with
#                   'Invalid username or token', burying the real 404 behind an
#                   auth error that sends you looking for credentials you do not
#                   need. GIT_CONFIG_COUNT is git >= 2.31 (this box has 2.43).
#   *_ASKPASS     : the third door, and the one the 20260811 sweep went through.
#                   VS Code's integrated terminal exports GIT_ASKPASS pointing at
#                   its own helper, which hands git a GitHub token; askpass is
#                   consulted ahead of both settings above, so the run still got
#                   'Invalid username or token' for a repo that simply 404s.
#                   Cleared, the same clone says 'could not read Username',
#                   which is git for 'that repo is gone'.
export GIT_TERMINAL_PROMPT=0
export GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=credential.helper GIT_CONFIG_VALUE_0=
unset GIT_ASKPASS SSH_ASKPASS

# Python block-buffers stdout when it is a pipe, and every project's output goes
# through 'tee' into a log. So the pipeline's own commentary sat in a buffer for
# the hour a project ran while git's and Docker's unbuffered stderr went straight
# through -- a log that was 0 bytes until the project ended, showing only the
# scariest lines and none of the "...that failed, recovering" around them. That
# is what made the 20260811 cryptofuzz clone look fatal when it was handled.
export PYTHONUNBUFFERED=1

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
# Per-project subdirectories are created by the pipeline as it writes, so a
# resumed run adds to this tree rather than replacing it.
ARTIFACTS="$RUN_DIR/artifacts"

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
  # A dry run promises no network. Both the probes and the recency ranking are
  # network, so a dry run falls back to the seeded shuffle -- the point of a dry
  # run is the wiring, and an unprobed sample exercises all of it.
  if [ "$DRY_RUN" -eq 1 ]; then
    echo "selecting $NUM_PROJECTS C++ projects (dry run: seeded shuffle, seed $SELECT_SEED)"
    SELECT_ARGS=(-n "$NUM_PROJECTS" --order shuffle --seed "$SELECT_SEED" --no-probe)
  else
    echo "selecting $NUM_PROJECTS C++ projects (order: $SELECT_ORDER)"
    SELECT_ARGS=(-n "$NUM_PROJECTS" --order "$SELECT_ORDER" --seed "$SELECT_SEED")
  fi
  # Names on stdout, provenance on stderr (so it lands in the terminal). The
  # subshell cd is what lets 'uv run -m' resolve the package without moving
  # this script's cwd, which section 6 still needs.
  mapfile -t PROJECTS < <(cd "$ROOT_DIR/src" && uv run -m oss_fuzz.select_projects \
                            "${SELECT_ARGS[@]}")
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
    # rc=0 covers both "ran and found no sibling" and "never got a harness to
    # build", which are opposite outcomes; 'clean' for the latter reads as
    # success and hid 30 wasted build attempts in the 20260811 sweep.
    0) if grep -qE '^== campaign: 0/' "$log" 2>/dev/null
       then echo "no-harness"; else echo "clean"; fi ;;
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
     --max-target-tries "$MAX_TARGET_TRIES" --results-json "$RESULTS"
     --artifacts-dir "$ARTIFACTS")
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
  echo "| project | status | evidence |"
  echo "|---|---|---|"
  for p in "${PROJECTS[@]}"; do
    # The evidence column is only meaningful once the project has run; a
    # not-run row would otherwise link to a directory that does not exist.
    if [ -d "$ARTIFACTS/$p" ]; then
      echo "| $p | ${STATUS[$p]:-not-run} | \`artifacts/$p/\` |"
    else
      echo "| $p | ${STATUS[$p]:-not-run} | — |"
    fi
  done
  echo
  echo "Each \`artifacts/<project>/\` holds what the generator was given"
  echo "(\`inputs/\`, \`prompts/\`) and what the fuzzing engine said back"
  echo "(\`fuzz/verify_*.log\` on the vulnerable build, \`fuzz/head_*.log\` on HEAD)."
} > "$SUMMARY"

echo
echo "suite done — $RUN_DIR"
printf '  %-16s %s\n' "summary:" "$SUMMARY"
printf '  %-16s %s\n' "results:" "$RESULTS"
printf '  %-16s %s\n' "artifacts:" "$ARTIFACTS"

# Exit 3 if any project found a confirmed sibling, mirroring the pipeline's own
# code so a wrapper can branch on the sweep the same way.
for p in "${PROJECTS[@]}"; do
  [ "${STATUS[$p]:-}" = "SIBLINGS" ] && exit 3
done
exit 0
