# Item 6 (seed the generator with generally-nasty shapes) — NOT BUILT

Measured before building. Same death as item 5, for the same reason, and the
silence statistic is again what decides it.

## What item 6 proposed

Give the fuzzer a starter kit of boundary-crossing inputs, stated generally:
end-before-start ranges, empty containers, single elements, gaps at position k.
The motivation: Lang-63's correct alarm sat armed through a whole run but never
got fed an input where the fake misbehaves.

## Why it dies

Seeds make **existing checks fire more often**. They are label-blind — the
pipeline cannot know which patches are fake — so they reach every leg equally.

By the item-5 finding, a correct patch that fires even one alarm is **wrongly
accused 56% of the time**, while one that stays silent was cleared 14 times out of
14. So anything that converts silent correct legs into firing ones is buying
lottery tickets at 56% odds against.

**Armed checks currently sitting unfired** (a direct measure of what seeds could
reach):

| | legs | armed checks |
|---|---|---|
| silent CORRECT legs | 14 | 147 |
| missed FAKE legs | 8 | 69 |

That is a 2.1:1 exposure against, on check counts. (An earlier cut of this put it
at 12:1 by comparing only against the 2 *silent* fake legs — too thin a
denominator, and corrected here.)

**But the per-leg cut is the one that matters, and it is worse.** A leg needs only
ONE check to fire to enter the lottery, and legs carry ~10 armed checks each. So
the question is how many legs would wake, not how many checks.

Of the 8 missed-fake legs, only **2 are genuinely silent** — Closure-38 (5 armed,
0 reviewed) and Lang-60 (7 armed, 0 reviewed). The other 6 already had alarms
reviewed and dismissed:

```
A Math-73-ACS-plausible_o       armed=14  alarms reviewed=3
A Math-104-Elixir-plausible_o   armed= 7  alarms reviewed=3
A Chart-19-Arja-plausible_o     armed=11  alarms reviewed=1
A Lang-63-Arja-plausible_o      armed= 5  alarms reviewed=4   <- the motivating case
B Math-104-Elixir-plausible_o   armed= 7  alarms reviewed=7
B Chart-19-Arja-plausible_o     armed=13  alarms reviewed=5
A Closure-38-SequenceR_o        armed= 5  alarms reviewed=0   <- genuinely silent
B Lang-60-Arja-plausible_o      armed= 7  alarms reviewed=0   <- genuinely silent
```

**The motivating case, stated accurately.** In the pair, Lang-63's fake leg had 5
armed checks and **4 alarms reviewed** in roll A (all dismissed) and 7 in roll B
(caught). In neither pair roll was it starved of firings.

But the pair is not the whole record, and an earlier draft of this note wrongly
concluded "seeds address a problem that leg did not have". The committed
three-roll decomposition (`a75012d`) shows Lang-63 failing a DIFFERENT way in each
roll:

| roll | family invented? | fired? | outcome |
|---|---|---|---|
| night20b | yes | yes | caught |
| night20c | yes | **armed but silent all run** | missed |
| preflight2 | **never proposed** | nothing fired | missed |
| pair roll A | yes | fired 4x, all dismissed | missed |
| pair roll B | yes | fired 7x | caught |

So across five rolls Lang-63 has displayed every failure mode the pipeline has.
**Seeds genuinely address one of them** — night20c's armed-but-silent roll — and
that decomposition's own cycle-7 suggestion was exactly this item. The benefit is
real; it is one mode out of three on the leg's history, and zero out of two in the
pair.

The kill therefore rests on the COST, not on absence of benefit.

So the real trade is: seeds could wake at most **2** missed-fake legs, while
risking **14** silent correct legs at 56%. That is 7:1 against on the count that
decides outcomes.

## And unlike repair-in-place, nothing absorbs the addition

Repair-in-place survived its own version of this argument because the campaign
loop stops at `target_successes` — on 49 of 60 legs a repaired harness *displaces*
a later attempt rather than adding one. There is no equivalent ceiling here. A
seeded corpus feeds every accepted harness on every leg; the additions are real
everywhere.

## Not built

Recorded rather than parked, like item 5 — but for a different reason than item 5.
Item 5's premise was measurably FALSE. Item 6's premise is TRUE and its benefit is
real; it loses on price. The exposure runs the wrong way by a factor of seven, with
no budget ceiling absorbing the additions.

If the seed idea is ever revisited, the precondition is not a better seed list —
it is a way to make a firing on a correct patch cost less than 56%. That is the
accusation-side research question already parked from item 2b, and it gates this
item too.

## The sibling option that survives this argument

The same decomposition offered a second cycle-7 shape for Lang-63: **generation-
side family persistence per leg** — when a check family is invented once in a run,
keep proposing it (within-run only; no cross-run pooling).

That does NOT have the cost profile that kills seeds. Seeds make existing checks
fire more often, which is what buys tickets in the 56% lottery. Persistence makes
the same family get *proposed* more consistently; on a correct leg, persisting a
family that does not fire changes nothing. It targets the invention lottery —
which is what killed Lang-63 in preflight2 and what item 2e found behind Math-73's
roll-A miss (the roll invented a non-discriminating rule; the other roll invented
a discriminating one).

Not built, not measured, and NOT smuggled into this cycle. Recorded as the
successor idea with a better prior than the one being killed.
