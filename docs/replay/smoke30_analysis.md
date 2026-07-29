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
