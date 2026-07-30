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

**The motivating case does not support the item.** Lang-63's fake leg had 5 armed
checks and **4 alarms reviewed**. It was not starved of firings — its alarms fired
and were dismissed. Item 5's classification already said so: Lang-63's miss was
"invented, triggered, judged, dismissed", not "never triggered". Seeds address a
problem that leg did not have.

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

Recorded rather than parked, like item 5: the premise is measurably false for its
own motivating case, and the exposure runs the wrong way by a factor of seven.

If the seed idea is ever revisited, the precondition is not a better seed list —
it is a way to make a firing on a correct patch cost less than 56%. That is the
accusation-side research question already parked from item 2b, and it gates this
item too.
