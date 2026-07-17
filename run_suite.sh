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

# Relation pool (P3.2) isolation: the pool persists across runs when left at
# its default (~/.vuln_patch_relation_pool), so relations screened by an OLD
# suite would silently feed every later one and the measurement stops being
# attributable. Each suite gets a fresh pool inside its own run folder;
# in-run sharing between a bug's legs (the point of pooling) still works.
export RELATION_POOL_DIR="$ROOT/relation_pool"
mkdir -p "$RELATION_POOL_DIR"

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

# --- concurrency -----------------------------------------------------------
# PARALLEL=N runs up to N cases at once (default 1 = serial, unchanged). Each
# case is fully isolated (own rundir, run.log, D4J_CHECKOUT_ROOT), so the only
# shared state is the manifest — assembled in index order AFTER all runs
# finish (phase 3), never by concurrent append. Budget ~2 GB RAM + ~1 core
# per concurrent case (a Jazzer JVM + a checkout); on the current small box
# keep PARALLEL=1. The gpt-* API is shared across all cases, so past ~4–6
# concurrent the model's rate limit, not this knob, caps throughput.
PARALLEL="${PARALLEL:-1}"

run_one() {
  local idx="$1" total="$2" tag="$3" flag="$4" pf="$5"
  local rundir="$ROOT/$tag"; mkdir -p "$rundir"
  local ckout="/home/code/scratch/co/${SUITE}_${STAMP}/${tag}"
  rm -rf "$ckout"; mkdir -p "$(dirname "$ckout")"
  echo "@@@@ [$idx/$total] $tag start $(date +%H:%M:%S) @@@@"
  D4J_CHECKOUT_ROOT="$ckout" PYTHONUNBUFFERED=1 \
    uv run python -u java/run.py $flag --patch_file "$pf" --model "$MODEL" $COMMON \
      --results_json "$rundir/result.jsonl" \
      > "$rundir/run.log" 2>&1
  local ec=$?
  echo "  [$idx/$total] $tag exit=$ec  rec: $(tail -1 "$rundir/result.jsonl" 2>/dev/null)"
}

# Phase 1 — resolve every case to a work item (serial, cheap). A case with no
# patch file is skipped HERE so it never enters the worklist (can't dangle a
# job or a rundir). Worklist is TAB-separated: idx, tag, flag, patchfile.
WORKLIST="$ROOT/.worklist.tsv"; : > "$WORKLIST"
idx=0
for spec in "${CASES[@]}"; do
  set -- $spec; flag=$1
  idx=$((idx+1))
  if [[ "$2" == patchfile:* ]]; then
    pf="${2#patchfile:}"
    base=$(basename "$pf" .patch); tag=$(printf "%02d_%s_%s" "$idx" "$base" "${flag#-}")
  else
    proj=$2; bug=$3; tool=${4:-}
    tag=$(printf "%02d_%s-%s_%s_%s" "$idx" "$proj" "$bug" "$tool" "${flag#-}")
    # Resolve the tool's patch file (overfit vs correct dir from the flag).
    cls=Doverfitting; [ "$flag" = "-c" ] && cls=Dcorrect
    pf=$(ls /home/code/drr/Patches/$cls/$tool/$proj/*-$proj-$bug-* 2>/dev/null | head -1)
    [ -z "$pf" ] && pf=$(ls /home/code/drr/Patches/$cls/*/$proj/*-$proj-$bug-* 2>/dev/null | head -1)
    if [ -z "$pf" ]; then
      # Loud skip — an empty --patch_file would just error inside the run
      # log where nobody looks, and the suite totals would silently shrink.
      echo "@@@@ [$idx/${#CASES[@]}] $tag SKIPPED: no patch matches $cls/$tool/$proj/*-$proj-$bug-* @@@@"
      continue
    fi
  fi
  printf '%d\t%s\t%s\t%s\n' "$idx" "$tag" "$flag" "$pf" >> "$WORKLIST"
done
TOTAL=${#CASES[@]}

# Phase 2 — run the worklist, up to PARALLEL at a time. Throttle by polling
# the live background-job count, so the cap holds even where `wait -n` is
# unavailable (older bash) — there we just poll a touch slower. With
# PARALLEL=1 each launch blocks until its job finishes: identical to the old
# serial loop. (Validated: peak concurrency == PARALLEL exactly, manifest
# stays in index order regardless of finish order.)
echo "running ${PARALLEL}-way parallel over $(wc -l < "$WORKLIST" | tr -d ' ') case(s)"
while IFS=$'\t' read -r idx tag flag pf; do
  run_one "$idx" "$TOTAL" "$tag" "$flag" "$pf" &
  while [ "$(jobs -rp | wc -l | tr -d ' ')" -ge "$PARALLEL" ]; do
    wait -n 2>/dev/null || sleep 0.3
  done
done < "$WORKLIST"
wait

# Phase 3 — assemble the manifest in index order (race-free: nothing appended
# to it during the parallel runs).
: > "$MANIFEST"
while IFS=$'\t' read -r idx tag flag pf; do
  [ -f "$ROOT/$tag/result.jsonl" ] && cat "$ROOT/$tag/result.jsonl" >> "$MANIFEST"
done < "$WORKLIST"

# summary.md — confusion matrix + P/R/F1 straight from the manifest.
uv run python - "$MANIFEST" "$ROOT/summary.md" "$SUITE" "$STAMP" <<'PY'
import json, math, sys, collections
manifest, out, suite, stamp = sys.argv[1:5]
rows = [json.loads(l) for l in open(manifest)] if __import__('os').path.exists(manifest) else []
cm = collections.Counter(); tok = 0
lines = [f"# {suite} ({stamp})\n", f"{len(rows)} runs\n", "| bug | label | kind | crashed | outcome |", "|---|---|---|---|---|"]
for r in rows:
    lab = 'overfit' if 'over' in str(r.get('label','')).lower() else 'correct'
    crashed = bool(r.get('crashed_on_patch'))
    tok += (r.get('tokens_total') or {}).get('total_tokens', 0)
    # Only runs that actually fuzzed the patched build carry a verdict. A
    # status like no_harnesses (e.g. the patch failed to apply, so the
    # patched fuzz never ran) must show as NOT-EVALUATED, not sneak into
    # the matrix as a TN/FN via crashed_on_patch defaulting to False.
    if r.get('status') != 'evaluated':
        lines.append(f"| {r.get('project')}-{r.get('bug_id')} | {lab} | {r.get('bug_kind')} | - | NOT-EVALUATED ({r.get('status')}) |")
        continue
    out_ = ('TP' if crashed else 'FN') if lab=='overfit' else ('FP' if crashed else 'TN')
    cm[out_] += 1
    lines.append(f"| {r.get('project')}-{r.get('bug_id')} | {lab} | {r.get('bug_kind')} | {crashed} | {out_} |")
tp,fn,fp,tn = cm['TP'],cm['FN'],cm['FP'],cm['TN']
p = tp/(tp+fp) if tp+fp else float('nan'); rc = tp/(tp+fn) if tp+fn else float('nan')
# F1 is 0.0 (not nan) when P and R are defined but zero (tp==0 with
# nonempty denominators); nan only when a denominator is empty.
f1 = (float('nan') if math.isnan(p) or math.isnan(rc)
      else (2*p*rc/(p+rc) if (p+rc) else 0.0))
nev = len(rows) - (tp+fn+fp+tn)
lines += ["", f"**TP={tp} FN={fn} FP={fp} TN={tn}**  P={p:.2f} R={rc:.2f} F1={f1:.2f}"
          + (f"  ({nev} not evaluated)" if nev else ""),
          f"\nTotal tokens: {tok:,}"]
open(out,'w').write("\n".join(lines)+"\n")
print("summary:", out)
PY
echo ">>> SUITE $SUITE DONE $(date)  root=$ROOT"
