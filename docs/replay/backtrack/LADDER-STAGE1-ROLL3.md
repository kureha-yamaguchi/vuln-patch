# Ladder stage 1, roll 3 — roll 1's bug again. Then a desk walkthrough found
# the rest for free.

`ladder1d_20260806_234616`, git `7d6b9cc`. Math-65 correct leg.

## The read

```
[66]  reference-impl · detect          no disputed observable
                                        (this firing names no shown method)
[85]  reference-impl · getChiSquare    disputed observable detected
[86]  reference-impl · getChiSquare    REFUSED: implementation leak
```

Both doors now fire — the Spec K fix worked, and [66] is the replay door
correctly reporting *nothing to do* rather than being silent.

But [86] is **roll 1's failure again**. My javadoc fix was incomplete, not a
third distinct class. That is worse than a new bug: a fix that looked complete
and was not.

## Why the first fix missed

`assemble_class_context` delivers javadoc with its `/**` and `*/` delimiters
**already stripped**, leaving bare continuation lines:

```
     * Get the Root Mean Square value.
     * @return RMS value
    public double getRMS() { … }
```

`strip_comments` matched `/*…*/` and `//…`. With no opener, it removed nothing,
so `@return RMS value` survived and matched `\breturn\b[^;]*;` — `[^;]*` spans
newlines, so it ran to the next semicolon anywhere in the file. **332 such lines
in Math-65 alone.**

Fixed by also stripping lines whose first non-whitespace character is `*`.

## THE DESK WALKTHROUGH — the commitment kept, and it paid immediately

Rather than launch roll 4, the whole chain was run offline against this roll's
**real recorded class context** (28,209 chars):

```
STEP 1 detect               ['getChiSquare']          correct
STEP 2 strip_bodies         28,209 -> 26,150 chars    body removed, correct
STEP 3 leak check           None (after the fix)      now passes
STEP 4 prompt built         29,818 chars              no body, doc surface present
STEP 6 screen               needs >= 3 observables
       driver produces      ReferenceImpl.getChiSquare()  -> ONE
```

**The second blocker, found at zero cost: the driver would supply ONE observable
against a screen requiring three.** Every reference would be discarded on
observable supply regardless of quality — roll 4 would have burned ~250k to
learn nothing about the mechanism.

That is the walkthrough earning its price on its first use, and it confirms the
signature prediction made before roll 2: `getChiSquare()` takes no arguments and
reads instance state, so a static no-arg entry can neither vary nor compute.

## Fixes applied

1. **bare-star comment stripping** (the roll-1/3 bug, properly this time).
2. **FUNCTIONALIZED entry point.** The prompt now demands a single pure
   `compute` taking the state as PARAMETERS, with the rationale stated to the
   model: *a reference that reads no input cannot be run on different inputs,
   and one that cannot be varied cannot be checked.*
3. **The generator declares its own signature** on a required first line
   (`// compute(<types>)`), and our code READS it. An unreadable signature is a
   DISCARD with its reason recorded — never an assumed call shape.

## Open design question, surfaced rather than decided

The driver now knows the signature but not **what to pass**. Input vectors need
a principled source, and the choice determines what the reference is compared
on:

* the failing test's own literals (tier-1, but few, and they are the disputed
  point itself — poor for OFF-defect screening);
* 8.3's recorded off-defect observations (the right shape, but the holdout rule
  says anything shown to the generator cannot also be the exam);
* fuzzed vectors from the harness's own generator (plentiful and genuinely
  held-out, but they must be valid under the documented preconditions or the
  reference and the patch both throw and nothing is compared).

My inclination is the third with a validity filter, precisely because it is the
only one that supplies enough held-out observables to clear
`MIN_SCREENED_OBSERVABLES`. It is a real decision and it belongs to the user.

## Gate status

(a) canaries — still unexercised live
(b) zero facts from discarded references — **HELD**, now across 3 rolls
(c) fact engages the disputed formula — unreached
(d) rule 7 — roll 3 was NOT a no-change repeat (the wiring changed between 2 and
    3), but roll 3 re-ran roll 1's failure, so the next roll must not.
