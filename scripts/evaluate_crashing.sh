#!/usr/bin/env bash
# Like evaluate.sh, but broader: runs the pipeline over a random sample of
# patch files from the drr dataset (Dcorrect + Doverfitting), then scores
# the same confusion matrix. The sample is balanced: SAMPLE_SIZE/2 patches
# are drawn from Dcorrect and SAMPLE_SIZE/2 from Doverfitting (each shuffled
# and capped independently). Set SAMPLE_SIZE=0 to run every patch instead
# of sampling.
#
# This pipeline only evaluates crashing bugs, so semantic bugs are filtered
# out of the queue BEFORE sampling (via src/java/dataset/classify_bugs.py, which reads
# defects4j's static trigger_tests files — no checkout needed). That keeps
# SAMPLE_SIZE meaning what it says: N crashing-bug patches, not N patches of
# which some unpredictable fraction turn out semantic. run.py is still
# passed --skip_semantic as a belt-and-suspenders check (status
# "semantic_skip", exit 4, not counted in the matrix) in case its own
# classification — sourced from `defects4j info`, read after checkout —
# ever disagrees with the static pre-filter.
#
# Decision rule (the classifier under test):
#   a patch is FLAGGED AS OVERFITTING  <=>  at least one harness still
#   crashes the PATCHED code (vulnerability still reachable).
#
#   ground truth | flagged overfitting | not flagged
#   -------------+---------------------+-------------
#   overfitting  |   TP                |   FN
#   correct      |   FP                |   TN
#
# Note: jq must be installed on your machine (apt-get install jq / brew install jq)

set -uo pipefail

# ---- config (override via env) -------------------------------------------
PROJECTS=(${PROJECTS:-Chart Closure Lang Math Time})
NUM_HARNESSES="${NUM_HARNESSES:-5}"       # target set size (-n)
MAX_ATTEMPTS="${MAX_ATTEMPTS:-30}"        # -m
FUZZ_TIMEOUT="${FUZZ_TIMEOUT:-60}"
VERIFY_TIMEOUT="${VERIFY_TIMEOUT:-60}"
DRY_RUN="${DRY_RUN:-0}"                   # 1 = build the queue and stop
SAMPLE_SIZE="${SAMPLE_SIZE:-60}"          # 0 = run every queued patch; else split 50/50 correct/overfitting
SEED="${SEED:-42}"                        # for reproducible sampling

DRR_PATCHES="drr/Patches"

TS="$(date +%Y%m%d_%H%M%S)"
OUTDIR="results/eval_all_${TS}"
mkdir -p "$OUTDIR"
RESULTS="${OUTDIR}/records.jsonl"
: > "$RESULTS"
RESULTS_ABS="$(cd "$(dirname "$RESULTS")" && pwd)/$(basename "$RESULTS")"
LOG="${OUTDIR}/run.log"
QUEUE="${OUTDIR}/queue.txt"

echo "Output dir : $OUTDIR"
echo "Projects   : ${PROJECTS[*]}"
echo ""

# ---- classify every bug up front (crashing vs semantic), from defects4j's
#      static trigger_tests files -- no checkout, no defects4j subprocess
#      per bug. Only "crashing" bugs are eligible for the queue below, so
#      semantic bugs never consume a slot in SAMPLE_SIZE. ------------------
CLASS_CSV="${OUTDIR}/bug_classes.csv"
python3 src/java/dataset/classify_bugs.py --csv "$CLASS_CSV" > "${OUTDIR}/bug_classes.log"
declare -A IS_CRASHING
{
  read -r _header
  while IFS=',' read -r proj bug_id cls; do
    [[ "$cls" == "crashing" ]] && IS_CRASHING["${proj}|${bug_id}"]=1
  done
} < <(tr -d '\r' < "$CLASS_CSV")   # csv.writer emits \r\n; strip it for bash `read`

# ---- build the queue: one "<flag> <patch_path>" line per CRASHING patch --
: > "$QUEUE"
for class in Dcorrect Doverfitting; do
  flag="-c"; [[ "$class" == "Doverfitting" ]] && flag="-o"
  while IFS= read -r f; do
    proj="$(basename "$(dirname "$f")")"
    for p in "${PROJECTS[@]}"; do
      if [[ "$proj" == "$p" ]]; then
        bug_id="$(basename "$f" | awk -F'-' '{print $3}')"
        if [[ -n "${IS_CRASHING[${proj}|${bug_id}]:-}" ]]; then
          echo "$flag $f" >> "$QUEUE"
        fi
        break
      fi
    done
  done < <(find "$DRR_PATCHES/$class" -name '*.patch' | sort)
done

AVAILABLE=$(wc -l < "$QUEUE")
echo "Patches available (crashing only) : $AVAILABLE"
awk '{print $1}' "$QUEUE" | sort | uniq -c | sed 's/^/  /'

# ---- balanced random-sample: SAMPLE_SIZE split 50/50 between correct (-c)
#      and overfitting (-o) patches, each shuffled and capped independently
#      (SAMPLE_SIZE=0 disables sampling) ------------------------------------
if [[ "$SAMPLE_SIZE" != "0" ]]; then
  CORRECT_Q="${OUTDIR}/queue.correct.txt"
  OVERFIT_Q="${OUTDIR}/queue.overfitting.txt"
  grep '^-c ' "$QUEUE" > "$CORRECT_Q"
  grep '^-o ' "$QUEUE" > "$OVERFIT_Q"
  N_CORRECT=$(wc -l < "$CORRECT_Q")
  N_OVERFIT=$(wc -l < "$OVERFIT_Q")

  HALF_C=$(( SAMPLE_SIZE / 2 ))
  HALF_O=$(( SAMPLE_SIZE - HALF_C ))   # odd SAMPLE_SIZE gives the extra slot to overfitting
  [[ "$HALF_C" -gt "$N_CORRECT" ]] && HALF_C="$N_CORRECT"
  [[ "$HALF_O" -gt "$N_OVERFIT" ]] && HALF_O="$N_OVERFIT"

  SHUFFLED_C="${OUTDIR}/queue.correct.shuffled.txt"
  SHUFFLED_O="${OUTDIR}/queue.overfitting.shuffled.txt"
  awk -v seed="$SEED" 'BEGIN{srand(seed)} {print rand() "\t" $0}' "$CORRECT_Q" \
    | sort -k1,1n | cut -f2- > "$SHUFFLED_C"
  awk -v seed="$SEED" 'BEGIN{srand(seed+1)} {print rand() "\t" $0}' "$OVERFIT_Q" \
    | sort -k1,1n | cut -f2- > "$SHUFFLED_O"

  { head -n "$HALF_C" "$SHUFFLED_C"; head -n "$HALF_O" "$SHUFFLED_O"; } \
    | awk -v seed="$SEED" 'BEGIN{srand(seed+2)} {print rand() "\t" $0}' \
    | sort -k1,1n | cut -f2- > "${QUEUE}.tmp"
  mv "${QUEUE}.tmp" "$QUEUE"
  echo "Sampled ${HALF_C} correct + ${HALF_O} overfitting patch(es) (seed=${SEED})"
fi

TOTAL=$(wc -l < "$QUEUE")
echo "Patches queued : $TOTAL"
awk '{print $1}' "$QUEUE" | sort | uniq -c | sed 's/^/  /'
echo "Queue file     : $QUEUE"
echo ""

if [[ "$DRY_RUN" == "1" ]]; then
  echo "DRY_RUN=1 — stopping before invoking run.py."
  exit 0
fi

# ---- run the pipeline once per queued patch -------------------------------
i=0
while IFS=' ' read -r flag patch; do
  i=$((i + 1))
  echo "[$i/$TOTAL] $flag $patch" | tee -a "$LOG"
  patch_abs="$(cd "$(dirname "$patch")" && pwd)/$(basename "$patch")"
  ( cd src && uv run java/run.py "$flag" \
      --patch_file "$patch_abs" \
      --skip_semantic \
      -n "$NUM_HARNESSES" \
      -m "$MAX_ATTEMPTS" \
      --fuzz_timeout "$FUZZ_TIMEOUT" \
      --verify_timeout "$VERIFY_TIMEOUT" \
      --results_json "$RESULTS_ABS" ) >>"$LOG" 2>&1
  rc=$?
  case $rc in
    0|2) status=$(tail -n 1 "$RESULTS" | jq -r '.status')
         echo "  done (exit $rc, status=$status)" | tee -a "$LOG" ;;
    3)   echo "  skipped: no bug-triggering test" | tee -a "$LOG" ;;
    4)   echo "  skipped: semantic bug" | tee -a "$LOG" ;;
    *)   echo "  ERROR (exit $rc)" | tee -a "$LOG" ;;
  esac
done < "$QUEUE"

# ---- aggregate ------------------------------------------------------------
echo ""
echo "==== aggregating ===="
jq -s '
  map(select(.status == "evaluated")) as $rows
  | ($rows | map(select(.label=="overfitting"))) as $ovf
  | ($rows | map(select(.label=="correct")))     as $cor
  | ($ovf | map(select(.crashed_on_patch))    | length) as $tp
  | ($ovf | map(select(.crashed_on_patch|not))| length) as $fn
  | ($cor | map(select(.crashed_on_patch))    | length) as $fp
  | ($cor | map(select(.crashed_on_patch|not))| length) as $tn
  | {
      overfitting_evaluated: ($ovf|length),
      correct_evaluated:     ($cor|length),
      TP: $tp, FN: $fn, FP: $fp, TN: $tn,
      precision:   (if ($tp+$fp)>0 then ($tp/($tp+$fp)) else null end),
      recall:      (if ($tp+$fn)>0 then ($tp/($tp+$fn)) else null end),
      specificity: (if ($tn+$fp)>0 then ($tn/($tn+$fp)) else null end),
      accuracy:    (if ($tp+$fn+$fp+$tn)>0 then (($tp+$tn)/($tp+$fn+$fp+$tn)) else null end),
      f1:          (if (2*$tp+$fp+$fn)>0 then (2*$tp/(2*$tp+$fp+$fn)) else null end)
    }
' "$RESULTS" | tee "${OUTDIR}/summary.json"

echo ""
echo "Per-run records : $RESULTS"
echo "Summary         : ${OUTDIR}/summary.json"
echo "Full log        : $LOG"
