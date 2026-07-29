# Math-30 smoke test (2026-07-29) — the observability fix works; the inertness hypotheses are BOTH wrong

One case, Math-30's correct patch, ~250k tokens. Run `smoke30_20260729_083751`.
Verdict: still **FP** (accusation survived).

## The observability fix is confirmed live

Every cycle-6 mechanism now leaves a permanent, greppable event in `trace.md`:

| event | count |
|---|---|
| `cycle6_gates_entry` | 8 |
| `cycle6_6B_indiscriminate_considered` / `_decided` | 6 / 6 |
| `cycle6_buggy_rate_considered` / `_decided` | 6 / 6 |
| `cycle6_6C_fires_on_both_considered` | 6 |
| `cycle6_muted_replay_pass` | 7 |
| `cycle6_diversion_decided` | 13 |
| `cycle6_family_duty_decided` | 2 |

Before this fix all of these were invisible after a successful leg. That ambiguity is gone.

## Both pre-registered hypotheses are REFUTED

- **(i) "6B never ran live"** — false. It ran 6 times and **dropped two firings**:
  `rate=0.9990` and `rate=1.0000`, both `family-duty NO`, reason
  `6B-INDISCRIMINATE-DROP: buggy rate 100% >= intrinsic bar`.
- **(ii) "no buggy rate was delivered"** — false in general. 12 `[fire-rate fact]` blocks are
  present; the delivery step correctly reports `skipped — a [fire-rate fact] was already attached
  upstream` in the cases where one existed.

## The actual cause — a THIRD hypothesis neither of us listed

The conviction survives on a firing for which **no buggy-side rate exists at all**. Its 6B event
reads:

> `output: not-applicable` · `reason: no rate found — verdict unchanged`

The surviving SOUND verdict is the `mannWhitneyUTest` vs `mannWhitneyU` asymptotic-formula check,
kept with **`CITATION: NONE`** — an uncited hypothetical about what "no correct implementation
could" do, on a firing carrying no rate for 6B to act on. Corroborating: `universal-screen
measured: 0` in this leg — the measurement that would have produced a rate for an unmatched
harness oracle never ran.

So the enforcement rule is sound and active; it simply has nothing to enforce on the one firing
that matters, because that firing's clearing measurement was never taken. **This is the
"never-collected" class (a) from the chronic-FP classification, for this specific firing** — item
4's plumbing was aimed at exactly this and evidently does not cover this path (its universal-screen
route did not fire here at all).

## Consequence for the plan

The 30-leg measurement is still not worth spending: we now know precisely why Math-30 survives,
and it is a fixable delivery gap, not a judging question. The targeted next step is to make the
universal screen actually produce a rate for an unmatched harness oracle on this path — then
re-run this same ~250k smoke and see the accusation drop.

Cost of learning this: ~250k, versus 10M for the official measurement. The smoke-test-first
instinct was correct.

---

## CORRECTION (2026-07-29) — two claims in this document were WRONG

A diagnose-first pass refuted this document's own premise. Both errors were mine, and both came
from the same habit: **grepping for a `print` string instead of the recorded event.**

**1. "no buggy-side rate exists at all" — FALSE.** The surviving firing (`u-complement-small`)
HAD a measured buggy-side rate: `violated=18420/20000` (92%), recorded at
`cycle6_buggy_rate_considered`. Verified independently: `18420` appears in the trace.

**2. "universal-screen measured: 0" — FALSE.** The screen measured **two** oracles in that leg
(`[universal-screen fact] … 0/781`, `0/773`). My check grepped the print string
`universal-screen] measured`, which — exactly as this cycle has established twice already — never
reaches `trace.md`. Grepping the event/fact text gives 2.

**The real cause, one step later than I claimed:** `fire_rate_fact` requires a PATCHED-side rate in
every branch except the intrinsic one (buggy >= 0.95). At harness-judging time the patched replay
has not run yet, and 18420/20000 = 0.921 < 0.95 — so a rate the pipeline had already paid Jazzer
for was discarded, and 6B truthfully reported "no rate found". Not a collection gap: a
**delivery** gap in the fact builder.

**Fix (37d9019):** the mirror of the existing patched-high/buggy-unknown branch — buggy measured
at/above the shipped `MAX_FIRE_RATIO` with patched unmeasured, tagged `[fact:rate-ambiguous]`.
No new threshold. Plus `cycle6_universal_screen_decided` (skipped/cached/capped/not-instrumented/
compile-failed/no-counts/measured/raised) and `cycle6_rate_absent`, so every "no rate" path now
says why.

**Honest caveat carried forward:** this fix is **decision-neutral by construction** — 92% is below
6B's 0.95 bar, so it changes what the judge SEES, not what the code DECIDES. Whether Math-30's
accusation drops is therefore a judging outcome and not guaranteed. The ~250k re-smoke is the
confirming step, and a negative result there would mean the remaining Math-30 conviction is a
judging problem, not a plumbing one.

**Process note:** this is the second doc premise of mine overturned in two days, both by looking
closer rather than by new data. Both times the tell was a `print`-based grep. Standing rule now:
when checking whether a mechanism ran, grep the RECORDED EVENT, never the print.
