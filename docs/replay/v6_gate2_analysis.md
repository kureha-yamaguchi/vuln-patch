# Faithful enforcement gate (gate2, 2026-07-28)

Run `v6_gate2_2048` · 141 scored rows · repo fixture (contested rows absent) · every case carrying
its real failing-test block. Raw: `v6_gate2_20260728.md`. First measurement in which the
family-duty escape had the input it needs.

## Headline
over-kill 11 · leak 25 · **6B fired 3 times (was 10 under the blind gate): 2 correct drops, 1
catch killed.**

## Against the reading pre-committed BEFORE the run (994f231)

**The escape works now.** Three of the four catches killed by the blind gate — rows 33, 122, 133 —
are **no longer dropped**. Row 122's block is `BrentSolverTest::testBadEndpoints`, the very
trigger test its relation mirrors, so the escape can now answer YES where before it was asked
blind. That is the artifact resolving exactly as diagnosed.

**The Closure-62 prediction is confirmed.** Its four 6B drops (rows 46, 143_1, 144, 149) have
vanished — with the real failing test in hand, the escape recognises those checks as touching the
behaviour the test pins (its caret line in the error output). Per the pre-commitment this is
**the escape being honest, not the rule regressing**, and Closure-62 now sits in the same family
as Closure-38: checks in the right neighbourhood that overreach into what the test does not pin.
Those accusations return to the delivery/plumbing bucket. **We do not weaken the escape to force
the drops back** — ruled out in advance.

**Math-30's chronic drop HELD** (row 55, `canonical-parity-closed-form`) — the single most
argued-past false accusation of the campaign, now dismissed by code rather than persuasion. Row
175 also dropped correctly.

## The honest limit: 3 events cannot establish net benefit

6B's measured effect is **2 correct drops and 1 catch killed** (row 32,
`mirrored-negative-nonbracket`, whose duty answered NO). Against this pipeline's measured
verdict-variance — 5 of 10 *untouched* rows flip between identical draws — a 3-event, 2:1 split is
well inside noise. The aggregate numbers cannot arbitrate either: **both baselines
(iteration-2's 12/23 and gate1's 10/27) were themselves measured under the blind-escape defect**,
so no before/after comparison here is sound.

What IS established, and does not rest on counts:
- the escape functions when fed (3 catches recovered, mechanism visible per row);
- 6B mechanically dismisses Math-30's chronic keep, which no prompt wording ever moved;
- 6C never fired in either gate — still untested, ship-neutral.

## Verdict
Pre-committed outcome **2** ("catches kept, some chronic drops vanish → still ship"), with one
deviation to record honestly: outcome 2 assumed no catch would still be killed, and row 32 was.
That single kill is not sufficient to revert (it is one event, and its sibling row 33 was saved by
the same escape), but it is **not** covered by the pre-commitment either, so it is logged as an
open question rather than waved through: if the second 20-leg run shows Math-73-ACS-family catches
dying, 6B is the first suspect.

**Ship 6B/6C**, proceed to the plumbing item and the second 20-leg run. Do not cite gate1 or the
escape-dependent parts of the cycle-5 close.
