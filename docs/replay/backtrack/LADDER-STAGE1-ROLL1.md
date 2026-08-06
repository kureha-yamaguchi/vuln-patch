# Ladder stage 1, roll 1 — the chain stopped at step 2, five times, on javadoc.

`ladder1b_20260806_222036`, git `08dd5b2`, Math-65 correct leg,
`--reference_impl` on. 257,361 tokens.

## Per-event read (the stage-1 deliverable)

```
[77]  reference-impl · getChiSquare   disputed observable detected
                                       detail: {'candidates': ['getChiSquare']}
[78]  reference-impl · getChiSquare   REFUSED: implementation leak
      reason: section 'skeleton' contains what looks like an implementation
              body ('return RMS value\n    public double getRMS() { /* body
              withhe...')
```

…repeated identically at [105], [134], [165], [193]. **10 events, 5 chains, all
stopped at step 2.**

## What worked

* **The detector is right.** `getChiSquare` is exactly the disputed observable —
  Math-65's chi-square formula is the contested computation.
* **`strip_bodies` worked** — the skeleton shows `{ /* body withheld */ }`.
* **The refusal was LOUD and carried its reason.** Diagnosis took one read of
  the trace, with no re-run and no guessing, because every exit records why.
* **8.23 earned its keep on the previous roll**: when the run died before
  producing a result, the suite refused to write `summary.md` and wrote
  `summary-INCOMPLETE.md` naming the missing leg instead of a clean-looking
  empty table.

## What failed, and it is the detector, not the design

`_BODY_MARKERS` contains `\breturn\b[^;]*;`. The skeleton's javadoc carries
`@return RMS value`, and the match ran from inside that comment across into the
next statement's semicolon.

**So the prompt refused itself on DOCUMENTATION** — the one material the
information rule says to carry maximally ("blind to implementations, MAXIMAL on
specification"). The guard was doing its job in the wrong direction.

This is the predicted first failure: upstream of anything interesting, and
exactly what a ~250k iteration is for.

## Fix (iteration 1 → 2)

Comments are stripped before the body markers run. Javadoc is specification, not
implementation. Pinned three ways:

* the roll-1 skeleton must now build a prompt without raising;
* a real body is still caught, **including one hiding behind a comment**
  (`{ /*x*/ return a+b; }`) — the fix must not blind the detector;
* genuinely commented-out code is not a leak.

670 passed, 7 skipped.

## Gate status: NOT YET MET, no criterion contradicted

(a) canaries — not exercised live (the chain never reached generation)
(b) zero facts from discarded references — **HELD**: 5 discards, 0 facts
(c) fact engages the disputed formula — not reached
(d) rule 7 — this is iteration 1 with a *changed* mechanism, so the two-strike
    counter has not started.

## The leg's own outcome, recorded not scored

Math-65 came out **FP**, which is its long-standing residual behaviour. The
mechanism never ran, so it neither caused nor could have prevented this.
