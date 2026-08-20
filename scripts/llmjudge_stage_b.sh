#!/usr/bin/env bash
# Stage B of the LLM-judge baseline's iteration protocol: the refinement turns
# of the stage-A winner. Each turn is run here; the prompt itself is written by
# hand between turns. See src/baseline_llmjudge/README.md section 6.
#
#   uv run -m baseline_llmjudge.evaluate --side dev     --prompt_version v1.1
#   uv run -m baseline_llmjudge.evaluate --side holdout --prompt_version v1.1 \
#       --confirm_holdout
#   uv run -m baseline_llmjudge.compare  --stage B --base v1
#
# Usage:
#   scripts/llmjudge_stage_b.sh v1          # the base's holdout reference row
#   scripts/llmjudge_stage_b.sh v1.1        # turn 1: dev, then holdout
#   scripts/llmjudge_stage_b.sh v1.2        # turn 2, and so on
#   scripts/llmjudge_stage_b.sh s1.1        # the semantic pool, same shape
#   scripts/llmjudge_stage_b.sh [-h]
#
# The bug pool is read from the version name, so it is never passed by hand:
# v1, v2 and v3 and their iterations judge crashing bugs, s1, s2 and s3 and
# theirs judge semantic ones. prompts.py is the authority.
#
# The base version runs on holdout only, because its dev pass is a stage-A
# artifact. An iteration runs on dev first, then on holdout.
#
# Env overrides:
#   PREREG=<path>         the pre-registration note, required before the first
#                         holdout pass (default:
#                         results/llmjudge_stageB_<base>_prereg.md)
#   STAGEB_DIR=<path>     append to this stage-B folder instead of the newest
#   NEW_DIR=1             start a new stage-B folder
#   SAMPLES=5             samples per patch (evaluate's default when unset)
#   MODEL=                override config.LOCAL_LLM_MODEL
#   PROJECTS=             restrict to some projects, e.g. "Lang Math"
#   CACHE_DIR=            evidence cache (default: evaluate's per-pool default)
#   DRY_RUN=1             build each queue and stop, before any model call
#
# One folder holds every stage-B artifact of one base. The first turn creates
# it and each later turn appends to it:
#   results/llmjudge_stageB_<base>_<YYYYMMDD_HHMMSS>/
#     stage_b.log                  this script's own commentary, all turns
#     prereg.md                    the pre-registration note, copied in
#     evaluate_v1_holdout.log      the base's reference pass
#     evaluate_v1.1_dev.log        one log per version per side
#     evaluate_v1.1_holdout.log
#     compare_B.log                the comparison that names the winner
#     run_dirs.txt                 "<version> <side> <exit code> <run dir>"
#
# The scored artifacts themselves stay where evaluate.py puts them
# (results/llmjudge_<side>_<version>_<ts>/), because compare.py finds runs by
# globbing that name. This directory holds the logs and the index into them.
#
# Three protocol rules are enforced here rather than left to the operator:
#
#   1. The pre-registration comes first. No holdout pass runs until a prereg
#      note exists, because a holdout number written before the design is
#      fixed is not a held-out number.
#   2. Selection is never partial. compare.py runs only once every registered
#      iteration of the base has a holdout pass. Rule 6 of the protocol is
#      three turns with no early stop.
#   3. The evidence never moves. One cache dir is passed to every pass and
#      --refresh_context is never passed, so every version reads the same
#      byte-identical evidence (checkable afterwards via evidence_sha256).
#
# No holdout error log is read here, and this script never reads one either. A
# turn is written from the previous turn's DEV errors only. After each dev pass
# the errors.py command for the next turn is printed.

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
  echo "name at least one version, e.g. v1.1 — try -h" >&2
  exit 2
fi

VERSIONS=("$@")
BASE="${VERSIONS[0]%%.*}"
SAMPLES="${SAMPLES:-}"
MODEL="${MODEL:-}"
PROJECTS="${PROJECTS:-}"
# Empty by default, so evaluate.py picks the cache directory of this pool. One
# shared cache would let a crashing rendering answer a semantic request.
CACHE_DIR="${CACHE_DIR:-}"
DRY_RUN="${DRY_RUN:-0}"
PREREG="${PREREG:-results/llmjudge_stageB_${BASE}_prereg.md}"

# The pool comes from the base name, read out of prompts.py. Deriving it here
# rather than taking it as an argument removes a way to score a version against
# the other pool's frozen split.
KIND="$(cd src && uv run python -c \
  "from baseline_llmjudge import prompts; print(prompts.kind_of('$BASE'))" \
  2>/dev/null)"
if [[ -z "$KIND" ]]; then
  echo "unknown base '$BASE' — prompts.py cannot say which pool it judges" >&2
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
# default pre-registration path is results/llmjudge_stageB_<base>_prereg.md,
# which a bare `_*` glob also matches — and 'p' sorts after a digit, so the note
# would win `tail -1` and the script would try to write its logs inside a file.
if [[ -n "${STAGEB_DIR:-}" ]]; then
  LOGDIR="$STAGEB_DIR"
else
  LOGDIR="$(find results -maxdepth 1 -type d \
              -name "llmjudge_stageB_${BASE}_[0-9]*" 2>/dev/null \
            | sort | tail -n 1)"
  if [[ -z "$LOGDIR" || "${NEW_DIR:-0}" == "1" ]]; then
    LOGDIR="results/llmjudge_stageB_${BASE}_$(date +%Y%m%d_%H%M%S)"
  fi
fi

# A stale path, or one that names a file, must stop the turn rather than let
# every later `tee` fail into nothing.
if [[ -e "$LOGDIR" && ! -d "$LOGDIR" ]]; then
  echo "log dir '$LOGDIR' exists and is not a directory — refusing" >&2
  exit 2
fi
# Rule 1: the pre-registration note comes first. It is written by hand, and
# every field in it is a copy of a summary.json field. This check runs before
# the folder is made, so a refused turn leaves no empty folder behind.
if [[ ! -f "${LOGDIR}/prereg.md" && ! -f "$PREREG" && "$DRY_RUN" != "1" ]]; then
  {
    echo "no pre-registration note at $PREREG, and none in $LOGDIR."
    echo "Write it before the first holdout pass. It records the model, the"
    echo "reasoning effort, the samples per patch, the vote rule, the"
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
[[ -n "$SAMPLES"   ]] && EXTRA+=(--samples "$SAMPLES")
[[ -n "$MODEL"     ]] && EXTRA+=(--model "$MODEL")
[[ -n "$PROJECTS"  ]] && EXTRA+=(--projects "$PROJECTS")
[[ -n "$CACHE_DIR" ]] && EXTRA+=(--cache_dir "$CACHE_DIR")
[[ "$DRY_RUN" == "1" ]] && EXTRA+=(--dry_run)

{
  echo "==== llmjudge stage B — refinement turns of $BASE ===="
  echo "started   : $(date -Is)"
  echo "bug pool  : $KIND"
  echo "versions  : ${VERSIONS[*]}"
  echo "cache dir : ${CACHE_DIR:-the per-pool default from evaluate.py}"
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
    ( cd src && uv run -m baseline_llmjudge.evaluate \
        --side "$side" \
        --kind "$KIND" \
        --prompt_version "$v" \
        "${CONFIRM[@]}" \
        "${EXTRA[@]}" ) 2>&1 | tee "$vlog"
    rc=${PIPESTATUS[0]}

    # evaluate.py prints "Output dir     : results/llmjudge_<side>_<v>_<ts>".
    run_dir="$(sed -n 's/^Output dir *: *//p' "$vlog" | head -n 1)"
    echo "${v} ${side} ${rc} ${run_dir:-unknown}" >> "$RUN_DIRS"

    if [[ $rc -ne 0 ]]; then
      echo "  FAILED (exit $rc) — see $vlog" | tee -a "$LOG"
      failed+=("${v}/${side}")
      continue
    fi
    echo "  done (exit 0)${run_dir:+, run dir: $run_dir}" | tee -a "$LOG"

    # One line of headline numbers per pass, so the iteration-log tables in the
    # README can be filled in without opening a summary.json.
    if [[ "$DRY_RUN" != "1" && -n "$run_dir" && -f "${run_dir}/summary.json" ]] \
       && command -v jq >/dev/null 2>&1; then
      jq -r '"  headline (\(.headline_rule)): P=\(.headline.precision) "
             + "R=\(.headline.recall) F1=\(.headline.f1) "
             + "FP=\(.headline.FP) FN=\(.headline.FN) "
             + "parse_failures=\(.parse_failures) "
             + "agreement=\(.mean_sample_agreement)"' \
         "${run_dir}/summary.json" 2>/dev/null | tee -a "$LOG"
    fi

    # Rule 2 of the protocol: the next turn is written from THIS dev log.
    if [[ "$side" == "dev" && "$DRY_RUN" != "1" && -n "$run_dir" ]]; then
      {
        echo "  next turn reads these DEV errors, and no others:"
        echo "    cd src && uv run -m baseline_llmjudge.errors \\"
        echo "        --records ${run_dir}/records.jsonl"
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
  echo "DRY_RUN=1 — queues built, no model called, compare.py skipped." \
    | tee -a "$LOG"
  exit 0
fi

# Rule 2: selection compares every registered iteration, so a missing holdout
# pass means a candidate missing from the comparison.
ITERATIONS="$(cd src && uv run python -c \
  "from baseline_llmjudge import prompts; print(' '.join(prompts.iterations_of('$BASE')))" \
  2>/dev/null)"
missing=()
for it in $ITERATIONS; do
  compgen -G "results/llmjudge_holdout_${it}_*/summary.json" >/dev/null || missing+=("$it")
done

if [[ ${#missing[@]} -gt 0 ]]; then
  {
    echo "==== stage B turn done, selection not yet possible ===="
    echo "finished     : $(date -Is)"
    echo "no holdout pass: ${missing[*]}"
    echo "Three turns are run, with no early stop, so compare.py waits for"
    echo "all of them. Write the next iteration in prompts.py from the dev"
    echo "log named above, then rerun this script for it."
    echo "logs         : $LOGDIR"
  } | tee -a "$LOG"
  exit 0
fi

CMPLOG="${LOGDIR}/compare_B.log"
echo "---- compare --stage B --kind $KIND --base $BASE (log: $CMPLOG)" \
  | tee -a "$LOG"
( cd src && uv run -m baseline_llmjudge.compare --stage B \
    --kind "$KIND" --base "$BASE" ) 2>&1 | tee "$CMPLOG"
cmp_rc=${PIPESTATUS[0]}
[[ $cmp_rc -ne 0 ]] && echo "  compare FAILED (exit $cmp_rc)" | tee -a "$LOG"

{
  echo ""
  echo "==== stage B done ===="
  echo "finished  : $(date -Is)"
  echo "run dirs  : $RUN_DIRS"
  echo "logs      : $LOGDIR"
  echo "next      : fill the two stage-B tables in"
  echo "            src/baseline_llmjudge/README.md, and publish all four"
  echo "            holdout rows — the winner's F1 is a maximum over three."
} | tee -a "$LOG"

exit $cmp_rc
