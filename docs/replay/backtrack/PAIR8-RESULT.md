# Cycle-8 closing pair — RESULT. The bar FAILS on all three criteria.

`pairA8_20260802_163734` + `pairB8_20260802_200421`, git `9ca9d70`,
`PARALLEL=4`, `pool30.cases` verified byte-identical to the July pair.
Raw committed before scoring (`1255dfa`, `a6ad6a2`).

## READ 1 — the 8.10 PASS bar

```
  CYCLE-8 A  TP= 9 FN= 5 FP= 7 TN= 9   P=0.56 R=0.64 F1=0.60   accusations=7
  CYCLE-8 B  TP=10 FN= 4 FP= 5 TN=11   P=0.67 R=0.71 F1=0.69   accusations=5
  July  A    TP= 9 FN= 5 FP= 5 TN=11   P=0.64 R=0.64 F1=0.64   accusations=5
  July  B    TP=11 FN= 3 FP= 5 TN=11   P=0.69 R=0.79 F1=0.73   accusations=5

  PAIRED MEAN F1   cycle-8 = 0.6448    July = 0.6881    bar > 0.685
```

| criterion | required | measured | |
|---|---|---|---|
| 1. paired mean | > 0.685 | **0.6448** | **FAIL** |
| 2. accusations per roll | ≤ 5 | A=**7**, B=5 | **FAIL** |
| 3. zero accusations on historically clean legs | 0 | **Math-2, Math-86** | **FAIL** |

**The cycle does not clear its own bar, and it is slightly below where July was.**

## What moved, precisely

```
  accused in cycle-8, NEVER in July : Math-2, Math-86
  accused in July, never in cycle-8 : Lang-60, Math-73
```

**Math-2 is accused in BOTH rolls** — that is not noise. Math-86 appears in one
of two. Two July accusations (Lang-60, Math-73) are gone, which cuts the other
way.

Net: July 5+5 = 10 accusations, cycle-8 7+5 = 12. Recall 20 catches → 19.

## The honest statistical statement

The within-pair spread is **larger than the between-pair difference**:

* July rolls: 0.64 and 0.73 → spread 0.09
* Cycle-8 rolls: 0.60 and 0.69 → spread 0.09
* Difference between pair means: 0.043

So this does **not** establish a regression. It equally does not establish
improvement, and the bar is a pre-committed threshold rather than a significance
test — a threshold you miss is missed. The one solid new fact is Math-2 accused
in both rolls.

## READ 2 — 8.4's dismiss branch. The fail condition did NOT trigger.

```
  FAKE-patch legs    : matches=0  differs=0  unknown=5
  CORRECT-patch legs : matches=0  differs=5  unknown=2
```

**Zero `matches` firings anywhere, so zero dismissals were licensed by the raw
comparison, so no genuine catch was voided by it.** The pre-registered fail
condition is not met. 8.4 did not cause this pair's precision loss — it cannot
have, because its dismissal branch never fired.

## READ 3 — 8.4's keep branch, the thing I flagged as unmeasured

`differs` fired **5 times, all on correct-patch legs, all on Closure-62** (both
rolls). Closure-62 is a named residual and was accused in July too, so the
keep-leaning branch did not create a new accusation here.

But note what this means honestly: **8.4's entire live effect across 60 legs was
5 firings on one already-failing leg.** The mechanism works, is guarded, and is
very nearly inert at this scale.

## READ 4 — repair provenance at scale

```
  accepted harnesses: 299    from a repaired attempt: 73 = 24%
```

Nearly a quarter of accepted harnesses required a repair. That is a large,
previously invisible dependency, and it is the strongest positive result in this
pair.

## READ 5 — 8.3's value channel, live

```
  buggy-side-observed-values events: 224
  ...carrying at least one value   :  72
  (archived baseline: 0 of 1,452)
```

The channel populates. From zero to 224 recorded steps, 72 with values.

## READ 6 — 8.2 build/no-build: UNRESOLVED, and that is a recording gap

The pre-registered rule was *"live rows reaching ≥3 observables ≥20% **of
trigger rows**"*. **That denominator cannot be computed from what the run
recorded**: `result.jsonl` has no `code_context`, and the trigger needs it.

Over ALL accepted firings — a different, larger denominator — the rate is:

```
  299 accepted firings; observable counts: 0->74  1->49  2->129  3->29  4->11  5->1  6->6
  >=3 observables: 47 = 16%
```

**I am not converting that into a decision.** Substituting a denominator the
rule did not name, to resolve a rule I wrote in advance, is precisely the move
pre-registration exists to prevent. Offline the trigger-restricted rate was
19/54 = 35%; over all firings live it is 16%; these are not comparable and
neither answers the question as asked.

**8.2's execution half stays unbuilt**, now for a stated reason rather than a
measured one, and the fix is small: record `code_context` (or a trigger flag)
in `result.jsonl` so the pre-registered denominator is computable next time.

## READ 7 — the parallelism confound

`PARALLEL=4` was introduced for this pair. One leg (roll A, Lang-22) died on an
uncaught `httpx.RemoteProtocolError` with `LLM_MAX_RETRIES=0`; it was re-run and
the error did not recur across 30 further legs in roll B. Weak evidence that
concurrency was not the cause, and not enough to clear it.

Throughput could not be compared against July per-leg because executed-input
counts are not in `result.jsonl` either — **the same recording gap as read 6**.

## Verdict

**The cycle-8 batch does not pass its pre-committed bar.** Three criteria, three
failures, on a paired measurement against an identical suite.

What the reads establish about the mechanisms themselves:

* 8.4 **did not** cause it — its dismissal branch never fired.
* 8.7 is real and larger than expected (24% of accepted harnesses).
* 8.3's channel works.
* 8.2's decision is unanswerable from this run's record.

So the batch is not implicated by the reads, and the bar still failed. The
honest reading is that this pair is a **wash within noise, sitting on the wrong
side of a threshold** — and the threshold is what was agreed in advance.
