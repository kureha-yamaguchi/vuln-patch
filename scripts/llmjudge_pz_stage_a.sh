#!/usr/bin/env bash
# Stage A of the Project Zero baseline's iteration protocol: the blind bake-off
# between the stage-A prompt designs, then the comparison that names the
# winner. See src/baseline_llmjudge/README.md section 11.
#
#   uv run -m baseline_llmjudge.project_zero.evaluate --side dev \
#       --prompt_version <design>          # once per design
#   uv run -m baseline_llmjudge.project_zero.compare --stage A
#
# Usage:
#   scripts/llmjudge_pz_stage_a.sh [-h]
#   DRY_RUN=1 scripts/llmjudge_pz_stage_a.sh
#
# The counterpart of llmjudge_stage_a.sh, which drives the Defects4J baseline.
# Three things differ, and each one is a property of this dataset:
#
#   1. There is no KIND. The bug-kind gate found too few semantic fixes for a
#      pool of its own, so one run scores every row and summary.json carries a
#      by_bug_kind breakdown instead. BUG_KIND can still filter.
#   2. There is no evidence cache. The render is a few local file reads, so
#      there is no cache directory to hold steady between designs.
#   3. Every design's row prints its own floor — the higher of the two
#      baselines that read no code. compare.py marks a design that does not
#      clearly beat its floor.
#
# Env overrides:
#   VERSIONS="p1 p2 p3"   designs to run, in order (default: prompts_pz's own)
#   SAMPLES=5             samples per fix (evaluate's default when unset)
#   MODEL=                override config.LOCAL_LLM_MODEL
#   BUG_KIND=             score one pool only: crashing or semantic
#   DRY_RUN=1             build each population and stop, before any model call
#
# Every design's stdout+stderr is kept, plus the comparison's:
#   results/llmjudge_pz_stageA_<YYYYMMDD_HHMMSS>/
#     stage_a.log       this script's own commentary
#     evaluate_p1.log   full output of that design's dev pass (one per version)
#     compare_A.log     the comparison that names the winner
#     run_dirs.txt      "<version> <exit code> <the run dir evaluate created>"
#
# The scored artifacts stay where evaluate.py puts them
# (results/llmjudge_pz_dev_<version>_<ts>/), because compare.py finds runs by
# globbing that name. This directory holds the logs and the index into them.
#
# Two protocol rules are enforced here rather than left to the operator:
#
#   1. Stage A is blind. compare.py runs only if every design finished, so a
#      stage-A winner is never named off a partial bake-off.
#   2. The population never moves. One frozen split serves every pass, and
#      evaluate.py copies it into each run directory with its git provenance.
#
# No records file is read here. Reading one before every design has run is what
# makes a bake-off unblind; that reading belongs to stage B.

set -uo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

while getopts "h" opt; do
  case "$opt" in
    h) sed -n '2,/^$/{s/^# \{0,1\}//;p;}' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "try -h" >&2; exit 1 ;;
  esac
done

SPLIT="suites/splits/project_zero_split.jsonl"
if [[ ! -f "$SPLIT" ]]; then
  echo "no frozen split at $SPLIT" >&2
  echo "run: cd src && uv run -m baseline_llmjudge.project_zero.split" >&2
  exit 2
fi

# The stage-A designs come from prompts.py, so this script never holds a second
# copy of the list that could drift out of step with it.
DEFAULT_VERSIONS="$(cd src && uv run python -c \
  "from baseline_llmjudge.project_zero import prompts; print(' '.join(prompts.BASE_VERSIONS))" \
  2>/dev/null)"
if [[ -z "${VERSIONS:-}" && -z "$DEFAULT_VERSIONS" ]]; then
  echo "could not read the designs from prompts.py — set VERSIONS" >&2
  exit 1
fi
VERSIONS=(${VERSIONS:-$DEFAULT_VERSIONS})
SAMPLES="${SAMPLES:-}"
MODEL="${MODEL:-}"
BUG_KIND="${BUG_KIND:-}"
DRY_RUN="${DRY_RUN:-0}"

TS="$(date +%Y%m%d_%H%M%S)"
LOGDIR="results/llmjudge_pz_stageA_${TS}"
mkdir -p "$LOGDIR"
LOG="${LOGDIR}/stage_a.log"
RUN_DIRS="${LOGDIR}/run_dirs.txt"
: > "$RUN_DIRS"

# Optional flags are collected into an array so an unset override contributes
# no argument at all — passing `--samples ""` would fail argparse's int().
EXTRA=()
[[ -n "$SAMPLES"  ]] && EXTRA+=(--samples "$SAMPLES")
[[ -n "$MODEL"    ]] && EXTRA+=(--model "$MODEL")
[[ -n "$BUG_KIND" ]] && EXTRA+=(--bug_kind "$BUG_KIND")
[[ "$DRY_RUN" == "1" ]] && EXTRA+=(--dry_run)

{
  echo "==== llmjudge stage A — Project Zero, blind bake-off ===="
  echo "started   : $(date -Is)"
  echo "dataset   : project_zero"
  echo "designs   : ${VERSIONS[*]}"
  echo "split     : $SPLIT"
  echo "extra args: ${EXTRA[*]:-(none)}"
  echo "git       : $(git rev-parse --short HEAD 2>/dev/null || echo '(not under git)')"
  echo "log dir   : $LOGDIR"
  echo ""
} | tee -a "$LOG"

failed=()
for v in "${VERSIONS[@]}"; do
  vlog="${LOGDIR}/evaluate_${v}.log"
  echo "---- $v : dev pass (log: $vlog)" | tee -a "$LOG"
  ( cd src && uv run -m baseline_llmjudge.project_zero.evaluate \
      --side dev \
      --prompt_version "$v" \
      "${EXTRA[@]}" ) 2>&1 | tee "$vlog"
  rc=${PIPESTATUS[0]}

  # evaluate.py prints "Output dir     : results/llmjudge_pz_dev_<v>_<ts>".
  run_dir="$(sed -n 's/^Output dir *: *//p' "$vlog" | head -n 1)"
  echo "${v} ${rc} ${run_dir:-unknown}" >> "$RUN_DIRS"

  if [[ $rc -ne 0 ]]; then
    echo "  FAILED (exit $rc) — see $vlog" | tee -a "$LOG"
    failed+=("$v")
    continue
  fi
  echo "  done (exit 0)${run_dir:+, run dir: $run_dir}" | tee -a "$LOG"

  # One line of headline numbers per design, plus the floor, so the iteration
  # log in the README can be filled in without opening three summaries.
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
  echo "" | tee -a "$LOG"
done

if [[ ${#failed[@]} -gt 0 ]]; then
  {
    echo "==== stage A INCOMPLETE ===="
    echo "failed designs : ${failed[*]}"
    echo "compare.py NOT run: a stage-A winner off a partial bake-off would be"
    echo "selected from fewer designs than the protocol registers. Fix the"
    echo "failure, rerun those designs, then run:"
    echo "    cd src && uv run -m baseline_llmjudge.project_zero.compare --stage A"
    echo "logs           : $LOGDIR"
  } | tee -a "$LOG"
  exit 1
fi

if [[ "$DRY_RUN" == "1" ]]; then
  echo "DRY_RUN=1 — populations built, no model called, compare.py skipped." \
    | tee -a "$LOG"
  exit 0
fi

CMPLOG="${LOGDIR}/compare_A.log"
echo "---- compare --stage A (log: $CMPLOG)" | tee -a "$LOG"
( cd src && uv run -m baseline_llmjudge.project_zero.compare --stage A ) \
  2>&1 | tee "$CMPLOG"
cmp_rc=${PIPESTATUS[0]}
[[ $cmp_rc -ne 0 ]] && echo "  compare FAILED (exit $cmp_rc)" | tee -a "$LOG"

{
  echo ""
  echo "==== stage A done ===="
  echo "finished  : $(date -Is)"
  echo "run dirs  : $RUN_DIRS"
  echo "logs      : $LOGDIR"
  echo "next      : fill the stage-A table in src/baseline_llmjudge/README.md,"
  echo "            run the winner once on holdout as stage B's reference row,"
  echo "            then stage B — read the winner's DEV records.jsonl."
  echo "warning   : each side holds about twenty rows, so no F1 difference"
  echo "            under about 0.2 is real. Read the floor column too."
} | tee -a "$LOG"

exit $cmp_rc
