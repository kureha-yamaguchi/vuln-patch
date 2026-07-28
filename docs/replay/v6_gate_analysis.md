# Cycle-6 enforcement gate (2026-07-28) — INVALID for the escape path; do NOT act on its cost side

Run `v6_gate_2014`, raw: `v6_gate_20260728.md`. Scored with the committed scorer.

## Headline (as measured)
over-kill 10 (2 5C + 8 genuine) · leak 27. 6B fired **10 times**: **6 on false accusations
(correct drops)** and **4 on genuine catches (rows 32, 33, 122, 133)**.

## The 4 killed catches are a REPLAY ARTIFACT, not a 6B failure

Quoted from a killed catch's own reason:

> "...the failing test's own observable. **The real failing test is not provided here**, so there
> is no basis to show that this fired check matches the very same observable behavior that the
> test demonstrates as wrong."

The family-duty escape's question is *"is this check asserting the failing test's own
observable?"* — unanswerable without the failing test. Measured: **0 of 141 fixture rows carry a
failing-test block** (`failing_test`/`failing_block` empty for every case), so
`verifier_replay` passes `failing_block=''` and family_duty can essentially never answer YES.
**The gate systematically disables the very escape that protects catches**, then reports the
resulting catch-kills as if they were the rule's cost.

In production run.py passes `_j3_failing_test_block(failure_tests)` — the escape has its input.

**This is the second instance of the same class**: iteration 1's `fd_prior=None` artifact
(docs/replay/v5d_iter1_analysis.md) also made the replay stricter than production and produced
phantom over-kill. Same lesson, new input: **a gate is only valid where it is faithful; an
unfaithful gate manufactures evidence against the code under test** — structurally identical to
the false-fact bug we fixed in the pipeline itself this morning.

## What the run DOES validly show
- **6B's benefit side is real and measured**: 6 false accusations dropped mechanically, including
  the chronic Closure-62 (`end-of-line-caret`, `groundtruth`) and Math-30
  (`canonical-parity-closed-form`) keeps that persuasion never moved. Those drops did not need the
  escape, so the artifact does not touch them.
- **6C never fired** (0 events) — untested by this run either way.
- The leak count (27 vs iteration-2's 23) is within the measured verdict-variance band (5/10
  untouched rows flip between draws) and is not attributable to these rules.

## Required before any verdict on 6B
1. ~~**Fix the fixture**~~ **DONE (2026-07-28)**: extract each case's failing-test block from its
   trace (it is present — run.py renders `_j3_failing_test_block` into the judge prompt, so it is
   recoverable verbatim) and add it to all 228 cases. Without it, no family-duty-escaped rule can
   ever be measured.
   `scripts/backfill_failing_test.py` recovers the block per LEG from
   `runs-archive/runs/<run>_*/<leg>/trace.md` (two independent renderings agree: the family-duty
   prompt's `<failing_test>` tags and the tail of the judge prompt's `<evidence>`). **228/228 cases
   now carry a non-empty `failing_test`; 0 legs lack a block.** Rows 32/33/122/133 all carry a real
   test body (row 122 = `BrentSolverTest::testBadEndpoints`, the very Math-73 trigger test its
   relation mirrors). `tests/test_fixture_fidelity.py` pins this so the artifact cannot come back.
2. **Point the gate at the repo fixture, not the scratch copy.** This run used the stale
   pre-reclassification VM copy (143 rows; contested rows 21/80 still scored). Process fix: sync
   the fixture from the repo at launch, never rely on an earlier `scp`.
3. Re-run the gate. Only then is 6B's cost side measurable.

**Do NOT revert 6B on this run.** Its measured cost is an artifact of the harness, not the rule.
