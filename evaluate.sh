#!/usr/bin/env bash
# Evaluate how well the generated harness set catches overfitting patches.
#
# Runs the pipeline over N distinct *overfitting* and N distinct *correct*
# crashing-bug patches, then scores the confusion matrix.
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
# Non-crashing bugs (run.py exit 3) are skipped and don't count toward N.
set -uo pipefail

# ---- config (override via env) -------------------------------------------
N="${N:-20}"                                   # distinct patches per class
PROJECTS=(${PROJECTS:-Chart Closure Lang Math Time})
NUM_HARNESSES="${NUM_HARNESSES:-10}"           # target set size (-n)
MAX_ATTEMPTS="${MAX_ATTEMPTS:-50}"             # -m
FUZZ_TIMEOUT="${FUZZ_TIMEOUT:-60}"
VERIFY_TIMEOUT="${VERIFY_TIMEOUT:-60}"
MAX_RUNS_PER_CLASS="${MAX_RUNS_PER_CLASS:-120}" # safety cap (random sampling)

TS="$(date +%Y%m%d_%H%M%S)"
OUTDIR="results/eval_${TS}"
mkdir -p "$OUTDIR"
RESULTS="${OUTDIR}/records.jsonl"
: > "$RESULTS"
RESULTS_ABS="$(cd "$(dirname "$RESULTS")" && pwd)/$(basename "$RESULTS")"
LOG="${OUTDIR}/run.log"

echo "Output dir : $OUTDIR"
echo "Per class  : $N distinct crashing-bug patches"
echo "Projects   : ${PROJECTS[*]}"
echo ""

# Collect N distinct evaluated patches for a given class flag (-o / -c).
# Distinctness key = project|bug_id|apr_tool, taken from the JSON record.
collect() {
  local flag="$1" label="$2"
  local got=0 runs=0
  declare -A seen=()
  while (( got < N && runs < MAX_RUNS_PER_CLASS )); do
    local proj="${PROJECTS[$((RANDOM % ${#PROJECTS[@]}))]}"
    runs=$((runs + 1))
    echo "[$label] run $runs (have $got/$N distinct) -> project $proj" | tee -a "$LOG"

    # One line is appended to $RESULTS per invocation (any non-error exit).
    local before
    before=$(wc -l < "$RESULTS")
    ( cd src && uv run java/run.py "$flag" \
        --project_name "$proj" \
        -n "$NUM_HARNESSES" \
        -m "$MAX_ATTEMPTS" \
        --fuzz_timeout "$FUZZ_TIMEOUT" \
        --verify_timeout "$VERIFY_TIMEOUT" \
        --results_json "$RESULTS_ABS" ) >>"$LOG" 2>&1
    local after
    after=$(wc -l < "$RESULTS")

    # No record written (hard error before any exit hook) -> just retry.
    (( after > before )) || { echo "  (no record emitted; retrying)" | tee -a "$LOG"; continue; }

    # Inspect the record we just appended.
    local rec status key
    rec=$(tail -n 1 "$RESULTS")
    status=$(echo "$rec" | jq -r '.status')
    key=$(echo "$rec" | jq -r '[.project,.bug_id,.apr_tool] | join("|")')

    if [[ "$status" != "evaluated" ]]; then
      echo "  skipped ($status)" | tee -a "$LOG"
      continue
    fi
    if [[ -n "${seen[$key]:-}" ]]; then
      echo "  duplicate patch ($key); dropping record" | tee -a "$LOG"
      # Drop the duplicate line so aggregation counts each patch once.
      head -n -1 "$RESULTS" > "${RESULTS}.tmp" && mv "${RESULTS}.tmp" "$RESULTS"
      continue
    fi
    seen[$key]=1
    got=$((got + 1))
    echo "  accepted ($key) [$got/$N]" | tee -a "$LOG"
  done

  if (( got < N )); then
    echo "WARNING: only collected $got/$N distinct $label patches in $runs runs" | tee -a "$LOG"
  fi
}

echo "==== collecting OVERFITTING patches ===="
collect "--overfitting" overfitting
echo ""
echo "==== collecting CORRECT patches ===="
collect "--correct" correct

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
      f1:          (if (2*$tp+$fp+$fn)>0 then (2*$tp/(2*$tp+$fp+$fn)) else null end),
      avg_harnesses_crashed_on_overfitting:
        (if ($ovf|length)>0 then (($ovf|map(.harnesses_crashed)|add)/($ovf|length)) else null end),
      avg_harnesses_crashed_on_correct:
        (if ($cor|length)>0 then (($cor|map(.harnesses_crashed)|add)/($cor|length)) else null end)
    }
' "$RESULTS" | tee "${OUTDIR}/summary.json"

echo ""
echo "Per-run records : $RESULTS"
echo "Summary         : ${OUTDIR}/summary.json"
echo "Full log        : $LOG"