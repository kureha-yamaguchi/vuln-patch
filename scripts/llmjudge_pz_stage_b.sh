#!/usr/bin/env bash
# Stage B of the Project Zero baseline's iteration protocol: the refinement
# turns of the stage-A winner. Each turn is run here; the prompt itself is
# written by hand between turns. See src/baseline_llmjudge/README.md section 11.
#
#   uv run -m baseline_llmjudge.project_zero.evaluate --side dev     --prompt_version p1.1
#   uv run -m baseline_llmjudge.project_zero.evaluate --side holdout --prompt_version p1.1 \
#       --confirm_holdout
#   uv run -m baseline_llmjudge.project_zero.compare --stage B --base p1
#
# Usage:
#   scripts/llmjudge_pz_stage_b.sh p1        # the base's holdout reference row
#   scripts/llmjudge_pz_stage_b.sh p1.1      # turn 1: dev, then holdout
#   scripts/llmjudge_pz_stage_b.sh p1.2      # turn 2, and so on
#   scripts/llmjudge_pz_stage_b.sh [-h]
#
# The counterpart of llmjudge_stage_b.sh, which drives the Defects4J baseline.
# Three things differ, and each one is a property of this dataset:
#
#   1. There is no KIND. One frozen split serves the whole population, so no
#      pool is derived from the version name.
#   2. There is no evidence cache, and no errors.py. A turn is written from the
#      dev run's records.jsonl, read by a person.
#   3. Each side holds about twenty rows. So the closing note repeats the size
#      warning, and compare.py prints each row's floor beside its F1.
#
# The base version runs on holdout only, because its dev pass is a stage-A
# artifact. An iteration runs on dev first, then on holdout.
#
# Env overrides:
#   PREREG=<path>         the pre-registration note, required before the first
#                         holdout pass (default:
#                         results/llmjudge_pz_stageB_<base>_prereg.md)
#   STAGEB_DIR=<path>     append to this stage-B folder instead of the newest
#   NEW_DIR=1             start a new stage-B folder
#   SAMPLES=5             samples per fix (evaluate's default when unset)
#   MODEL=                override config.LOCAL_LLM_MODEL
#   BUG_KIND=             score one pool only: crashing or semantic
#   DRY_RUN=1             build each population and stop, before any model call
#
# One folder holds every stage-B artifact of one base. The first turn creates
# it and each later turn appends to it:
#   results/llmjudge_pz_stageB_<base>_<YYYYMMDD_HHMMSS>/
#     stage_b.log                  this script's own commentary, all turns
#     prereg.md                    the pre-registration note, copied in
#     evaluate_p1_holdout.log      the base's reference pass
#     evaluate_p1.1_dev.log        one log per version per side
#     evaluate_p1.1_holdout.log
#     compare_B.log                the comparison that names the winner
#     run_dirs.txt                 "<version> <side> <exit code> <run dir>"
#
# The scored artifacts stay where evaluate.py puts them
# (results/llmjudge_pz_<side>_<version>_<ts>/), because compare.py finds runs by
# globbing that name. This directory holds the logs and the index into them.
#
# Two protocol rules are enforced here rather than left to the operator:
#
#   1. The pre-registration comes first. No holdout pass runs until a prereg
#      note exists, because a holdout number written before the design is
#      fixed is not a held-out number.
#   2. Selection is never partial. compare.py runs only once every registered
#      iteration of the base has a holdout pass.
#
# This script never reads a holdout record file. A turn is written from the
# previous turn's DEV records only, and the path to them is printed after each
# dev pass.

set -uo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

while getopts "h" opt; do
  case "$opt" in
    h) sed -n '2,/^$/{s/^# \{0,1\}//;p;}' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "try -h" >&2; exit 1 ;;
  esac
done
shift $((OPTIND - 1))

if [[ $# -eq 0 ]]; then
  echo "name at least one version, e.g. p1.1 — try -h" >&2
  exit 2
fi

SPLIT="suites/splits/project_zero_split.jsonl"
if [[ ! -f "$SPLIT" ]]; then
  echo "no frozen split at $SPLIT" >&2
  echo "run: cd src && uv run -m baseline_llmjudge.project_zero.split" >&2
  exit 2
fi

VERSIONS=("$@")
BASE="${VERSIONS[0]%%.*}"
SAMPLES="${SAMPLES:-}"
MODEL="${MODEL:-}"
BUG_KIND="${BUG_KIND:-}"
DRY_RUN="${DRY_RUN:-0}"
PREREG="${PREREG:-results/llmjudge_pz_stageB_${BASE}_prereg.md}"

# prompts.py is the authority on which bases exist, so an unknown base stops
# the turn here rather than after the first model call.
if ! ( cd src && uv run python -c \
  "import sys
from baseline_llmjudge.project_zero import prompts
sys.exit(0 if '$BASE' in prompts.BASE_VERSIONS else 1)" ) 2>/dev/null; then
  echo "unknown base '$BASE' — prompts.py does not register it" >&2
  exit 2
fi

for v in "${VERSIONS[@]}"; do
  if [[ "${v%%.*}" != "$BASE" ]]; then
    echo "every version in one invocation must share a base. Got $v, base is $BASE." >&2
    exit 2
  fi
done

# One folder per base. A later turn appends to the folder the first turn made,
# so the whole stage reads as one record rather than as six loose logs.
#
# The glob is anchored to a DIGIT and matched against directories only. The
# default pre-registration path also starts with the same prefix, and 'p' sorts
# after a digit, so a bare `_*` glob would let the note win `tail -1`.
if [[ -n "${STAGEB_DIR:-}" ]]; then
  LOGDIR="$STAGEB_DIR"
else
  LOGDIR="$(find results -maxdepth 1 -type d \
              -name "llmjudge_pz_stageB_${BASE}_[0-9]*" 2>/dev/null \
            | sort | tail -n 1)"
  if [[ -z "$LOGDIR" || "${NEW_DIR:-0}" == "1" ]]; then
    LOGDIR="results/llmjudge_pz_stageB_${BASE}_$(date +%Y%m%d_%H%M%S)"
  fi
fi

# A stale path, or one that names a file, must stop the turn rather than let
# every later `tee` fail into nothing.
if [[ -e "$LOGDIR" && ! -d "$LOGDIR" ]]; then
  echo "log dir '$LOGDIR' exists and is not a directory — refusing" >&2
  exit 2
fi
# Rule 1: the pre-registration note comes first. Every field in it is a copy of
# a summary.json field. This check runs before the folder is made, so a refused
# turn leaves no empty folder behind.
if [[ ! -f "${LOGDIR}/prereg.md" && ! -f "$PREREG" && "$DRY_RUN" != "1" ]]; then
  {
    echo "no pre-registration note at $PREREG, and none in $LOGDIR."
    echo "Write it before the first holdout pass. It records the model, the"
    echo "reasoning effort, the samples per fix, the vote rule, the"
    echo "parse-failure default, and the population of each side. Every one"
    echo "of those is already a summary.json field, so the note is a copy."
    echo "Then rerun, or pass PREREG=<path>."
  } >&2
  exit 1
fi

if ! mkdir -p "$LOGDIR"; then
  echo "could not create the log dir '$LOGDIR' — refusing" >&2
  exit 2
fi
LOG="${LOGDIR}/stage_b.log"
RUN_DIRS="${LOGDIR}/run_dirs.txt"
touch "$RUN_DIRS"

if [[ ! -f "${LOGDIR}/prereg.md" && -f "$PREREG" ]]; then
  cp "$PREREG" "${LOGDIR}/prereg.md"
  echo "prereg copied: $PREREG -> ${LOGDIR}/prereg.md" | tee -a "$LOG"
fi

# Optional flags are collected into an array so an unset override contributes
# no argument at all — passing `--samples ""` would fail argparse's int().
EXTRA=()
[[ -n "$SAMPLES"  ]] && EXTRA+=(--samples "$SAMPLES")
[[ -n "$MODEL"    ]] && EXTRA+=(--model "$MODEL")
[[ -n "$BUG_KIND" ]] && EXTRA+=(--bug_kind "$BUG_KIND")
[[ "$DRY_RUN" == "1" ]] && EXTRA+=(--dry_run)

{
  echo "==== llmjudge stage B — Project Zero, refinement turns of $BASE ===="
  echo "started   : $(date -Is)"
  echo "dataset   : project_zero"
  echo "versions  : ${VERSIONS[*]}"
  echo "split     : $SPLIT"
  echo "extra args: ${EXTRA[*]:-(none)}"
  echo "git       : $(git rev-parse --short HEAD 2>/dev/null || echo '(not under git)')"
  echo "log dir   : $LOGDIR"
  echo ""
} | tee -a "$LOG"

failed=()
for v in "${VERSIONS[@]}"; do
  # The base's dev pass belongs to stage A. Rerunning it here would score the
  # same frozen text twice and add a second dev row for one version.
  if [[ "$v" == "$BASE" ]]; then
    sides=(holdout)
    echo "---- $v : holdout only — the base's dev pass is a stage-A artifact" \
      | tee -a "$LOG"
  else
    sides=(dev holdout)
  fi

  for side in "${sides[@]}"; do
    vlog="${LOGDIR}/evaluate_${v}_${side}.log"
    echo "---- $v : $side pass (log: $vlog)" | tee -a "$LOG"
    CONFIRM=()
    [[ "$side" == "holdout" ]] && CONFIRM+=(--confirm_holdout)
    ( cd src && uv run -m baseline_llmjudge.project_zero.evaluate \
        --side "$side" \
        --prompt_version "$v" \
        "${CONFIRM[@]}" \
        "${EXTRA[@]}" ) 2>&1 | tee "$vlog"
    rc=${PIPESTATUS[0]}

    # evaluate.py prints "Output dir     : results/llmjudge_pz_<side>_<v>_<ts>".
    run_dir="$(sed -n 's/^Output dir *: *//p' "$vlog" | head -n 1)"
    echo "${v} ${side} ${rc} ${run_dir:-unknown}" >> "$RUN_DIRS"

    if [[ $rc -ne 0 ]]; then
      echo "  FAILED (exit $rc) — see $vlog" | tee -a "$LOG"
      failed+=("${v}/${side}")
      continue
    fi
    echo "  done (exit 0)${run_dir:+, run dir: $run_dir}" | tee -a "$LOG"

    # One line of headline numbers per pass, plus the floor, so the iteration
    # log in the README can be filled in without opening a summary.json.
    if [[ "$DRY_RUN" != "1" && -n "$run_dir" && -f "${run_dir}/summary.json" ]] \
       && command -v jq >/dev/null 2>&1; then
      jq -r '"  headline (\(.headline_rule)): P=\(.headline.precision) "
             + "R=\(.headline.recall) F1=\(.headline.f1) "
             + "FP=\(.headline.FP) FN=\(.headline.FN) "
             + "parse_failures=\(.parse_failures) "
             + "agreement=\(.mean_sample_agreement)",
             "  floors: always_positive=\(.baselines.always_positive.f1) "
             + "size_rule=\(.baselines.size_rule.f1 // "n/a")"' \
         "${run_dir}/summary.json" 2>/dev/null | tee -a "$LOG"
    fi

    # The next turn is written from THIS dev run, and from no other.
    if [[ "$side" == "dev" && "$DRY_RUN" != "1" && -n "$run_dir" ]]; then
      {
        echo "  next turn reads these DEV records, and no others:"
        echo "    ${run_dir}/records.jsonl"
      } | tee -a "$LOG"
    fi
    echo "" | tee -a "$LOG"
  done
done

if [[ ${#failed[@]} -gt 0 ]]; then
  {
    echo "==== stage B turn INCOMPLETE ===="
    echo "failed passes : ${failed[*]}"
    echo "compare.py NOT run. Fix the failure, rerun that pass, then rerun"
    echo "this script for the remaining versions."
    echo "logs          : $LOGDIR"
  } | tee -a "$LOG"
  exit 1
fi

if [[ "$DRY_RUN" == "1" ]]; then
  echo "DRY_RUN=1 — populations built, no model called, compare.py skipped." \
    | tee -a "$LOG"
  exit 0
fi

# Rule 2: selection compares every registered iteration, so a missing holdout
# pass means a candidate missing from the comparison.
ITERATIONS="$(cd src && uv run python -c \
  "from baseline_llmjudge.project_zero import prompts
print(' '.join(n for n in prompts.known_versions()
               if prompts.is_iteration(n) and prompts.base_of(n) == '$BASE'))" \
  2>/dev/null)"
missing=()
for it in $ITERATIONS; do
  compgen -G "results/llmjudge_pz_holdout_${it}_*/summary.json" >/dev/null \
    || missing+=("$it")
done

if [[ ${#missing[@]} -gt 0 ]]; then
  {
    echo "==== stage B turn done, selection not yet possible ===="
    echo "finished       : $(date -Is)"
    echo "no holdout pass: ${missing[*]}"
    echo "compare.py waits for every registered iteration. Write the next one"
    echo "in prompts.py from the dev records named above, then rerun this"
    echo "script for it."
    echo "logs           : $LOGDIR"
  } | tee -a "$LOG"
  exit 0
fi

CMPLOG="${LOGDIR}/compare_B.log"
echo "---- compare --stage B --base $BASE (log: $CMPLOG)" | tee -a "$LOG"
( cd src && uv run -m baseline_llmjudge.project_zero.compare --stage B \
    --base "$BASE" ) 2>&1 | tee "$CMPLOG"
cmp_rc=${PIPESTATUS[0]}
[[ $cmp_rc -ne 0 ]] && echo "  compare FAILED (exit $cmp_rc)" | tee -a "$LOG"

{
  echo ""
  echo "==== stage B done ===="
  echo "finished  : $(date -Is)"
  echo "run dirs  : $RUN_DIRS"
  echo "logs      : $LOGDIR"
  echo "next      : fill the iteration log in"
  echo "            src/baseline_llmjudge/README.md, and publish every holdout"
  echo "            row — the winner's F1 is a maximum over the iterations."
  echo "warning   : each side holds about twenty rows, so no F1 difference"
  echo "            under about 0.2 is real. Read the floor column too."
} | tee -a "$LOG"

exit $cmp_rc
