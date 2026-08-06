# Ladder stage 1, roll 2 — ZERO events, and the cause is Spec K repeating.

`ladder1c_20260806_225213`, git `3e75334` (the javadoc detector fix),
Math-65 correct leg. 176,575 tokens.

## The read

```
  reference-impl events recorded : 0
```

Not "detected nothing" — **nothing at all**, not even the
`no disputed observable` event the helper records when it finds nothing. The
chain was never entered.

## Why, established rather than guessed

The flag WAS passed (`--reference_impl` is in the run's `common_flags`), and the
disputed-computation fact appears 4 times in this leg's judge prompts. So the
material was there and the mechanism simply never ran.

`disputed_computation_fact` is called at **two** sites in `run.py`:

```
  3527  _dc   — the HARNESS track
  3884  _dc2  — the REPLAY-conviction track
```

**I wired one.** Roll 1's leg convicted on the harness track and produced 10
events; roll 2's convicted on the replay track and produced none. Same leg, same
bug, different door.

## This is Spec K, and this codebase already paid for it once

> *one-door fact parity. A harness-track firing of the SAME underlying check the
> replay track screens must carry the same facts, or the judge convicts here
> where the replay track correctly rules pre-existing (Math-73-c: the identical
> bogus endpoint-root check, ruled UNSOUND on the replay track that got the
> facts, kept on the harness track that did not).*

I attached a new fact at one door and not the other — the exact shape that rule
exists to prevent, reproduced within one cycle of reading it.

## Fix

The mechanism is now at both doors, using each track's own firing and trusted
values (`_fired`/`_tvals` on the replay side — copy-paste across doors is how
the wrong variables get read). Two pins:

* the mechanism appears at **exactly 2** sites, matching
  `disputed_computation_fact`'s own count;
* the replay-side call uses `_fired` and `_tvals`, not the harness-side names.

672 passed, 7 skipped.

## Gate status: unchanged, nothing contradicted

* (a) canaries — still unexercised live
* (b) zero facts from discarded references — **HELD trivially** (no chain ran)
* (c) fact engages the disputed formula — unreached
* (d) rule 7 — **counter still not started.** Roll 1 changed the detector; roll 2
  changed the wiring. Two consecutive *no-change* iterations is what stops the
  stage, and neither of these was one.

## What this cost, honestly

Two rolls (~434k) with the chain never reaching generation. Both failures were
upstream plumbing rather than the mechanism's substance, and both were found in
one trace read each because every exit records its reason — but neither was
predicted, and the second was predictable: `disputed_computation_fact`'s two call
sites are visible in a two-line grep I did not run before wiring.

**The check I should have run at stage 0**, now standing as a test: any new
judge-facing fact must appear at every door its trigger appears at.
