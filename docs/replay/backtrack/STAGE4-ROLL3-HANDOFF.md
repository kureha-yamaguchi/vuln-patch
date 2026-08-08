# Stage-4 roll 3 — full handoff (launch + read + decision rules)

Self-contained: assumes no memory of any conversation. Written 2026-08-08
at HEAD `fc5ec88`.

## 0. What changed since the last roll you launched

Three commits matter; all are on `main` and already rsynced to the VM:

* `f8e4b52` — `_method_declared` no longer accepts a CALL as a
  declaration (roll 2's false trigger: `standardNormal.cumulativeProbability(z);`).
* `a3f30fa` — candidate ORDER is ranked (message∩check → check-called →
  message-only) and the chain FALLS BACK through up to three candidates,
  each memoized. `disputed[0]` is no longer the whole attempt policy.
  The chain body now lives in a nested `_attempt(method)`.
* `daec407` + `fc5ec88` — the suite is TEN legs (four decisive stage-4 +
  six scouts). Suite: 810 passed, 7 skipped at HEAD.

## 1. Pre-launch checks (all on the VM)

```bash
cd /home/code/experiments-vuln-patch
cat VERSION                      # must correspond to fc5ec88 (re-run
                                 # ./push-to-vm.sh from the Mac if stale)
grep -c patchfile suites/cases/ladder_stage4.cases    # must print 10
df -h /home/code/scratch         # need comfortable headroom; 10 legs of
                                 # checkouts (protocol: df precheck)
# No env prechecks needed: run_suite.sh sources its own environment
# (defects4j PATH etc.); shell-level `which`/`java` checks mislead in
# non-login shells (roll-3 launcher's correction).
```

## 2. Launch

```bash
cd /home/code/experiments-vuln-patch
PARALLEL=6 nohup ./run_suite.sh stage4r3 suites/cases/ladder_stage4.cases \
    > /home/code/scratch/stage4r3.log 2>&1 &
```

Run PARALLEL (user rule: never serial; the machine is big enough — cap
at ~6, where the shared gpt-* API starts throttling per run_suite.sh's
own note). Expect roughly 1–1.5 hours wall-clock and ~1.5–2M tokens for
ten legs (roll-2 baseline: 131–250k/leg). Progress: `tail -f /home/code/scratch/stage4r3.log`. The
run lands in `/home/code/scratch/runs/stage4r3_<STAMP>/`.

## 3. Post-run protocol (unchanged since roll 5 — raw before read)

```bash
# from the Mac
rsync -az -e ssh hetzner:/home/code/scratch/runs/stage4r3_<STAMP>/ \
    runs-archive/runs/stage4r3_<STAMP>/
mkdir -p docs/replay/backtrack/ladder4-raw/roll3
cp runs-archive/runs/stage4r3_<STAMP>/{summary.md,config.json} \
    docs/replay/backtrack/ladder4-raw/roll3/
# commit the raw BEFORE reading anything
git add -A && git commit -m "Stage 4 roll 3 RAW, committed before the read" && git push
```

Then read per event with the script in `STAGE4-HOW-TO-READ.md` (its LEGS
list covers all ten). Never read from totals.

## 4. Gates and scout reads (pre-registered; do not reinterpret)

DECISIVE (stage 4 passes/fails on these four legs only):

| gate | leg | rule |
|---|---|---|
| S4-a | Math-53-c | HARD STOP: any accusation traceable to the reference fact or verdict gate. |
| S4-b | Math-2-SOFix-c | THE AGREEMENT TEST. Expect the chain to attempt `getNumericalMean` now (ordering fix). Pass needs: admission + agreement fact emitted; if the leg convicts, gate decisions present. TN banks ONLY with attributable events. |
| S4-c | Math-2-Arja-o | Catch retained AND zero voids. Lost catch = HARD STOP. |
| S4-d | Math-65-c | Record, don't bank (lottery rule). |
| S4-e | all | Zero facts from discarded references (19-for-19 so far). |
| S4-f | — | Rule 7: two no-change iterations → stop and report. |

SCOUTS (inform stage 8; they do NOT gate stage 4):

| read | leg | rule |
|---|---|---|
| SCOUT-a | Lang-41-o | HARD STOP on any gate void. |
| SCOUT-b | Time-4-c | HARD STOP on any mechanism-traceable accusation. |
| SCOUT-c | Closure-62-c | Stable FP both pairs — the 2nd precision target. Read: trigger? admit? fact/gate events beside any conviction? |
| SCOUT-d | Math-39-c | Reach datum only. |
| SCOUT-e | Closure-92-o, Closure-38-o | Stable dismissal-type FNs — the RECALL read. Does the chain trigger at the dismissal site, does a disagreement fact emit and ENGAGE (attribution: a value the judge could only get from the fact — the roll-3 standard: count occurrences in the whole trace)? FN→TP with attribution = first recall win. FN staying FN = status quo, record. |
| SCOUT-f | every overfit leg | BLANKET HARD STOP: zero gate voids on any fake patch. |

Also record, per leg (pre-registered denominators): chain triggered? /
candidates in attempted order / admission or discard step + reason /
fact emitted + which side / gate decisions / tokens. Plus the both-signs
ledger vs each leg's pair-run history (this file's table header lists
the historical outcomes: Math-2-c FP/FP, Closure-62-c FP/FP, Lang-41-o
TP/TP, Time-4-c TN/TN, Closure-92-o FN/FN, Closure-38-o FN/FN,
Math-53-c TN/TN).

## 5. Decision rules after the read

* ANY hard stop above → stop, diagnose in the trace, commit the read,
  and hand back with the finding. Do not launch anything further.
* Mechanism defects found by the read (reference chain, gate, screen,
  detector): record the read FIRST (raw already committed), then either
  fix offline if it is a one-seam mechanical fix testable against
  recorded material (the standing pattern: verbatim fixture + suite +
  `scripts/gate_corpus_scan.py` + `scripts/rewalk8_replay.py` where
  relevant), or hand back if it is a design question (anything touching
  screen semantics, bar heights, information rule, or outcome flags —
  those are gated decisions).
* First-contact discards on scout legs are EXPECTED (one seam per new
  bug is this ladder's constant). Each is one read; none is a surprise.
* Rule 7 across rolls applies to the DECISIVE gates only: if S4-b fails
  again with no change in cause, stop and report rather than re-roll.
* Do not update `MEMORY`/plan verdicts beyond the read entry; leave
  advancement judgments (stage 8 firing) to the main session/user.

## 6. Report format (what to bring back)

1. Scoreboard + tokens + minutes, then the gates table with one line
   each (PASS/FAIL/no-trigger + the evidence pointer).
2. The scout denominators table (trigger/admit/fact/gate per leg).
3. Attribution calls, explicitly argued (the roll-3 standard).
4. Anything filed as a defect: the exact trace line + your diagnosis
   depth (which is usually one `grep` away from the cause, per the
   19-defect record).
