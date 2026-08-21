#!/usr/bin/env bash
# Stage A of the LLM-judge baseline's dev iteration protocol: the blind
# bake-off between the three independent prompt designs, then the comparison
# that names the winner. See src/baseline_llmjudge/README.md section 6.
#
#   uv run -m baseline_llmjudge.defects4j.evaluate --side dev --kind <kind> \
#       --prompt_version <design>          # once per design
#   uv run -m baseline_llmjudge.defects4j.compare  --stage A --kind <kind>
#
# Usage:
#   scripts/llmjudge_stage_a.sh [-h]                 # the crashing pool
#   KIND=semantic scripts/llmjudge_stage_a.sh        # the semantic pool
#
# Each pool has its own three designs and its own frozen split, so KIND picks
# both at once. The default VERSIONS are that pool's three stage-A designs.
#
# Env overrides:
#   KIND=crashing         bug pool: crashing or semantic
#   VERSIONS="v1 v2 v3"   designs to run, in order (default: the pool's three)
#   SAMPLES=5             samples per patch (evaluate's default when unset)
#   MODEL=                override config.LOCAL_LLM_MODEL
#   PROJECTS=             restrict to some projects, e.g. "Lang Math"
#   CACHE_DIR=            evidence cache (default: evaluate's per-pool default)
#   DRY_RUN=1             build each queue and stop, before any model call
#
# Every design's stdout+stderr is kept, plus the comparison's:
#   results/llmjudge_stageA_<kind>_<YYYYMMDD_HHMMSS>/
#     stage_a.log       this script's own commentary
#     evaluate_v1.log   full output of that design's dev pass (one per version)
#     compare_A.log     the comparison that names the winner
#     run_dirs.txt      "<version> <exit code> <the run dir evaluate created>"
#
# The scored artifacts themselves stay where evaluate.py puts them
# (results/llmjudge_dev_<version>_<ts>/), because compare.py finds runs by
# globbing that name. This directory holds the logs and the index into them.
#
# Two protocol rules are enforced here rather than left to the operator:
#
#   1. Stage A is blind. compare.py runs only if all three designs finished, so
#      a stage-A winner is never named off a partial bake-off. A design that
#      fails is reported and the comparison is skipped.
#   2. The evidence never moves. One cache dir is passed to every pass and
#      --refresh_context is never passed, so all three designs read the same
#      byte-identical evidence (checkable afterwards via evidence_sha256).
#
# No error log is read here. Reading one before all three designs have run is
# what makes a bake-off unblind; errors.py belongs to stage B.

set -uo pipefail

cd "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

while getopts "h" opt; do
  case "$opt" in
    h) sed -n '2,/^$/{s/^# \{0,1\}//;p;}' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "try -h" >&2; exit 1 ;;
  esac
done

KIND="${KIND:-crashing}"
case "$KIND" in
  crashing|semantic) ;;
  *) echo "KIND must be crashing or semantic, got '$KIND'" >&2; exit 2 ;;
esac

# The pool's three stage-A designs come from prompts.py, so this script never
# holds a second copy of the list that could drift out of step with it.
DEFAULT_VERSIONS="$(cd src && uv run python -c \
  "from baseline_llmjudge.defects4j import prompts; print(' '.join(prompts.base_versions('$KIND')))" \
  2>/dev/null)"
if [[ -z "${VERSIONS:-}" && -z "$DEFAULT_VERSIONS" ]]; then
  echo "could not read the $KIND designs from prompts.py — set VERSIONS" >&2
  exit 1
fi
VERSIONS=(${VERSIONS:-$DEFAULT_VERSIONS})
SAMPLES="${SAMPLES:-}"
MODEL="${MODEL:-}"
PROJECTS="${PROJECTS:-}"
# Empty by default, so evaluate.py picks the cache directory of this pool. One
# shared cache would let a crashing rendering answer a semantic request.
CACHE_DIR="${CACHE_DIR:-}"
DRY_RUN="${DRY_RUN:-0}"

TS="$(date +%Y%m%d_%H%M%S)"
LOGDIR="results/llmjudge_stageA_${KIND}_${TS}"
mkdir -p "$LOGDIR"
LOG="${LOGDIR}/stage_a.log"
RUN_DIRS="${LOGDIR}/run_dirs.txt"
: > "$RUN_DIRS"

# Optional flags are collected into an array so an unset override contributes
# no argument at all — passing `--samples ""` would fail argparse's int().
EXTRA=()
[[ -n "$SAMPLES"   ]] && EXTRA+=(--samples "$SAMPLES")
[[ -n "$MODEL"     ]] && EXTRA+=(--model "$MODEL")
[[ -n "$PROJECTS"  ]] && EXTRA+=(--projects "$PROJECTS")
[[ -n "$CACHE_DIR" ]] && EXTRA+=(--cache_dir "$CACHE_DIR")
[[ "$DRY_RUN" == "1" ]] && EXTRA+=(--dry_run)

{
  echo "==== llmjudge stage A — blind bake-off ===="
  echo "started   : $(date -Is)"
  echo "bug pool  : $KIND"
  echo "designs   : ${VERSIONS[*]}"
  echo "cache dir : ${CACHE_DIR:-the per-pool default from evaluate.py}"
  echo "extra args: ${EXTRA[*]:-(none)}"
  echo "git       : $(git rev-parse --short HEAD 2>/dev/null || echo '(not under git)')"
  echo "log dir   : $LOGDIR"
  echo ""
} | tee -a "$LOG"

failed=()
for v in "${VERSIONS[@]}"; do
  vlog="${LOGDIR}/evaluate_${v}.log"
  echo "---- $v : dev pass (log: $vlog)" | tee -a "$LOG"
  ( cd src && uv run -m baseline_llmjudge.defects4j.evaluate \
      --side dev \
      --kind "$KIND" \
      --prompt_version "$v" \
      "${EXTRA[@]}" ) 2>&1 | tee "$vlog"
  rc=${PIPESTATUS[0]}

  # evaluate.py prints "Output dir     : results/llmjudge_dev_<v>_<ts>" first.
  run_dir="$(sed -n 's/^Output dir *: *//p' "$vlog" | head -n 1)"
  echo "${v} ${rc} ${run_dir:-unknown}" >> "$RUN_DIRS"

  if [[ $rc -ne 0 ]]; then
    echo "  FAILED (exit $rc) — see $vlog" | tee -a "$LOG"
    failed+=("$v")
    continue
  fi
  echo "  done (exit 0)${run_dir:+, run dir: $run_dir}" | tee -a "$LOG"

  # One line of headline numbers per design, so the iteration-log table in the
  # README can be filled in without opening three summary.json files.
  if [[ "$DRY_RUN" != "1" && -n "$run_dir" && -f "${run_dir}/summary.json" ]] \
     && command -v jq >/dev/null 2>&1; then
    jq -r '"  headline (\(.headline_rule)): P=\(.headline.precision) "
           + "R=\(.headline.recall) F1=\(.headline.f1) "
           + "FP=\(.headline.FP) FN=\(.headline.FN) "
           + "parse_failures=\(.parse_failures) "
           + "agreement=\(.mean_sample_agreement)"' \
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
    echo "    cd src && uv run -m baseline_llmjudge.defects4j.compare --stage A" \
         "--kind $KIND"
    echo "logs           : $LOGDIR"
  } | tee -a "$LOG"
  exit 1
fi

if [[ "$DRY_RUN" == "1" ]]; then
  echo "DRY_RUN=1 — queues built, no model called, compare.py skipped." | tee -a "$LOG"
  exit 0
fi

CMPLOG="${LOGDIR}/compare_A.log"
echo "---- compare --stage A --kind $KIND (log: $CMPLOG)" | tee -a "$LOG"
( cd src && uv run -m baseline_llmjudge.defects4j.compare --stage A --kind "$KIND" ) \
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
  echo "            then stage B — errors.py on the winner's DEV records.jsonl."
} | tee -a "$LOG"

exit $cmp_rc
