#!/usr/bin/env bash
# Clean, dated, self-contained result storage for a batch of runs.
#
# Layout (one folder per suite, timestamp in the name so it sorts by date and
# never collides):
#   scratch/runs/<suite>_<YYYYMMDD_HHMMSS>/
#     config.json         suite metadata: model, flags, git SHA, cases
#     manifest.jsonl      every run's result record, rolled up (one line each)
#     summary.md          confusion matrix + P/R/F1 (generated at the end)
#     NN_<Proj>-<bug>_<tool>_<o|c>/
#       result.jsonl      this run's record (incl. exact token usage)
#       run.log           full log (prompt, harness, differential)
#   scratch/co/<suite>_<STAMP>/<tag>/   isolated checkout per run
#
# Usage: run_suite.sh <suite_name> [cases_file]
#
# The cases file is a small sourced bash fragment defining MODEL, COMMON and
# CASES — keep named task sets in suites/ (version-controlled) so defining a
# task set NEVER means editing this script:
#   ./run_suite.sh t4fix suites/t4.cases
# Without a cases_file the inline defaults below run (kept as a smoke test).
# Each CASE is either:
#   "<-o|-c> <project> <bug> <tool>"        (samples that tool's patch), or
#   "<-o|-c> patchfile:/abs/path/to.patch"  (evaluate an explicit patch file)
set -uo pipefail

SUITE="${1:-suite}"
CASES_FILE="${2:-}"
# Resolve the cases file BEFORE any cd, and refuse to run without it when
# one was asked for — a bad path must never silently fall back to the
# inline defaults (that once turned a synthesis experiment into a plain
# rerun without anyone noticing until the logs were read). Accepts a path
# relative to the CALLER'S cwd or to this script's directory, and always
# ABSOLUTIZES it: the script cd's into src/ before sourcing, so a relative
# path that exists now would still fail at source time.
if [ -n "$CASES_FILE" ]; then
  if [ ! -f "$CASES_FILE" ]; then
    CASES_FILE="$(cd "$(dirname "$0")" && pwd)/$2"
  fi
  if [ ! -f "$CASES_FILE" ]; then
    echo "FATAL: cases file '$2' not found (also tried '$CASES_FILE')" >&2
    exit 1
  fi
  CASES_FILE="$(cd "$(dirname "$CASES_FILE")" && pwd)/$(basename "$CASES_FILE")"
fi

source /home/code/vpenv.sh
cd /home/code/experiments-vuln-patch/src

STAMP="$(date +%Y%m%d_%H%M%S)"
ROOT=/home/code/scratch/runs/${SUITE}_${STAMP}
mkdir -p "$ROOT"
MANIFEST="$ROOT/manifest.jsonl"; : > "$MANIFEST"

# ---- defaults (overridden by the sourced cases file) ----
MODEL="gpt-5.4"                    # or gpt-5.4-nano (with escalation)
COMMON="-n 3 -m 8 --fuzz_timeout 20 --verify_timeout 20 --verify_relations"
CASES=(
  "-o Time 4 Arja"
  "-c Time 4 Elixir"
)
if [ -n "$CASES_FILE" ]; then
  # shellcheck disable=SC1090
  source "$CASES_FILE"
  # Provenance: the exact case set this suite ran, next to its results.
  cp "$CASES_FILE" "$ROOT/cases.sourced"
fi
# ---------------------------------------------------------

GITSHA="$(git -C /home/code/experiments-vuln-patch rev-parse --short HEAD 2>/dev/null || echo unknown)"
# config.json — makes the folder self-explanatory months later.
{
  printf '{\n'
  printf '  "suite": "%s",\n' "$SUITE"
  printf '  "stamp": "%s",\n' "$STAMP"
  printf '  "cases_file": "%s",\n' "${CASES_FILE:-inline-defaults}"
  printf '  "model": "%s",\n' "$MODEL"
  printf '  "common_flags": "%s",\n' "$COMMON"
  printf '  "git_sha": "%s",\n' "$GITSHA"
  printf '  "n_cases": %d,\n' "${#CASES[@]}"
  printf '  "cases": [%s]\n' "$(printf '"%s",' "${CASES[@]}" | sed 's/,$//')"
  printf '}\n'
} > "$ROOT/config.json"
echo "suite root: $ROOT  (model=$MODEL git=$GITSHA)"

idx=0
for spec in "${CASES[@]}"; do
  set -- $spec; flag=$1
  idx=$((idx+1))
  if [[ "$2" == patchfile:* ]]; then
    pf="${2#patchfile:}"
    base=$(basename "$pf" .patch); tag=$(printf "%02d_%s_%s" "$idx" "$base" "${flag#-}")
    PATCHARG=(--patch_file "$pf")
  else
    proj=$2; bug=$3; tool=${4:-}
    tag=$(printf "%02d_%s-%s_%s_%s" "$idx" "$proj" "$bug" "$tool" "${flag#-}")
    # Resolve the tool's patch file (overfit vs correct dir from the flag).
    cls=Doverfitting; [ "$flag" = "-c" ] && cls=Dcorrect
    pf=$(ls /home/code/drr/Patches/$cls/$tool/$proj/*-$proj-$bug-* 2>/dev/null | head -1)
    [ -z "$pf" ] && pf=$(ls /home/code/drr/Patches/$cls/*/$proj/*-$proj-$bug-* 2>/dev/null | head -1)
    PATCHARG=(--patch_file "$pf")
  fi
  rundir="$ROOT/$tag"; mkdir -p "$rundir"
  ckout="/home/code/scratch/co/${SUITE}_${STAMP}/${tag}"
  rm -rf "$ckout"; mkdir -p "$(dirname "$ckout")"
  echo "@@@@ [$idx/${#CASES[@]}] $tag start $(date +%H:%M:%S) @@@@"
  D4J_CHECKOUT_ROOT="$ckout" PYTHONUNBUFFERED=1 \
    uv run python -u java/run.py $flag "${PATCHARG[@]}" --model "$MODEL" $COMMON \
      --results_json "$rundir/result.jsonl" \
      > "$rundir/run.log" 2>&1
  ec=$?
  [ -f "$rundir/result.jsonl" ] && cat "$rundir/result.jsonl" >> "$MANIFEST"
  echo "  exit=$ec  rec: $(tail -1 "$rundir/result.jsonl" 2>/dev/null)"
done

# summary.md — confusion matrix + P/R/F1 straight from the manifest.
uv run python - "$MANIFEST" "$ROOT/summary.md" "$SUITE" "$STAMP" <<'PY'
import json, sys, collections
manifest, out, suite, stamp = sys.argv[1:5]
rows = [json.loads(l) for l in open(manifest)] if __import__('os').path.exists(manifest) else []
cm = collections.Counter(); tok = 0
lines = [f"# {suite} ({stamp})\n", f"{len(rows)} runs\n", "| bug | label | kind | crashed | outcome |", "|---|---|---|---|---|"]
for r in rows:
    lab = 'overfit' if 'over' in str(r.get('label','')).lower() else 'correct'
    crashed = bool(r.get('crashed_on_patch'))
    out_ = ('TP' if crashed else 'FN') if lab=='overfit' else ('FP' if crashed else 'TN')
    cm[out_] += 1
    tok += (r.get('tokens_total') or {}).get('total_tokens', 0)
    lines.append(f"| {r.get('project')}-{r.get('bug_id')} | {lab} | {r.get('bug_kind')} | {crashed} | {out_} |")
tp,fn,fp,tn = cm['TP'],cm['FN'],cm['FP'],cm['TN']
p = tp/(tp+fp) if tp+fp else float('nan'); rc = tp/(tp+fn) if tp+fn else float('nan')
f1 = 2*p*rc/(p+rc) if p and rc and (p+rc) else float('nan')
lines += ["", f"**TP={tp} FN={fn} FP={fp} TN={tn}**  P={p:.2f} R={rc:.2f} F1={f1:.2f}",
          f"\nTotal tokens: {tok:,}"]
open(out,'w').write("\n".join(lines)+"\n")
print("summary:", out)
PY
echo ">>> SUITE $SUITE DONE $(date)  root=$ROOT"
