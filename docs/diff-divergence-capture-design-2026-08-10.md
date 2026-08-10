# Design sketch — divergence capture at the diff boundary (NOT BUILT)

**Status:** proposal for user review. No code, no pre-registration yet —
the gates get written only if the design survives discussion.
**Target station:** evidence assembly for relation synthesis (between
bug-context/fuzz and `relation_synth`), plus a small instrumentation
reuse from diffcov at patched-build time.
**Failure mode it targets (the Lang-63 residual, the one named gap left
after 8.35–8.39):** a leg where reach is saturated and invention is
absent. 8.36 measured ~2M entries per harness into
`DurationFormatUtils#reduceAndCorrect` with zero firings; 8.37's replay
showed 73/74 of its relations transform and execute without a single
tier-2 event; 8.38's fresh roll moved nothing. The relations probe the
class's documented observables — but never the one the patch distorts,
because nothing in the pipeline's evidence says WHICH observable the
patch touches. Invention is guessing, and for this leg it guesses wrong
in every draw.

## The idea in one sentence

Run the same generated inputs through the changed method on BOTH builds
the pipeline already has (buggy and patched), record the observable at
the diff boundary (return value, mutated receiver/argument state), diff
the two logs mechanically, and hand synthesis the top divergences as
FACTS — "on inputs shaped like X, this method's return moved from A to
B" — so invention aims at the observable the patch actually moves
instead of guessing.

## Why this is sound to attempt (and where the danger is)

- It uses ONLY the two builds and the patch text — no ground truth, no
  witnesses, no labels. Firewall-clean by construction; per-leg,
  within-run, nothing persisted.
- Divergence is NOT evidence of overfit. A correct patch diverges from
  buggy too — that is what fixing means. So divergence facts must NEVER
  become oracles. **The rule that keeps this sound: divergence steers
  ATTENTION (which observable, which input region); the CONTRACT — what
  the value must actually be — still comes only from documentation, the
  same as every relation today.** The relation that results asserts the
  documented behaviour of the nominated observable; it never asserts
  either observed value.
- The known temptation, named as a lint before any build: a model shown
  "buggy returned 09, patched returned -2" will want to write
  `expected "09"` — that is asserting buggy behaviour and is wrong on
  every correct patch. Mechanical guard: reject/demote any relation
  whose expected literal equals a logged buggy-side value that differs
  on the patched side (computable, both-signs testable on the guard
  fixtures).

## Mechanics (mostly existing pieces)

1. **Capture:** extend the diffcov injection (same station, same
   mechanical diff→method mapping, same flag discipline: default off)
   to log, for the first N distinct input shapes per changed method,
   `[diffobs] method=<id> args=<summary> ret=<summary> state=<summary>`
   — bounded like the 8.31 harvest (width-capped, typed, whole-value).
2. **Both sides:** the screen already compiles identical sources against
   the buggy build and the replay against the patched build; the same
   double-run applied to the capture harness gives paired logs at zero
   new machinery — pairing keyed on the recorded consumed-input vector
   (RecFDP already records it).
3. **Diff:** mechanical — same consumed vector, different ret/state ⇒
   one divergence record. Rank by input-shape frequency; take top-K.
4. **Feed:** a new evidence block in the synthesis prompt: the changed
   method, the divergent observable, and the input SHAPES (never the
   values as expectations) — worded as "the patch moves THIS observable
   HERE; write relations asserting its DOCUMENTED contract."

## What would make this fail, honestly

- Lang-63's divergence might only appear on trigger-adjacent inputs the
  fuzzer already reaches — then the facts point at the same observable
  the trigger test already names, and invention still has to write a
  calendar-shaped relation the model has so far never produced. The
  mechanism narrows the search; it cannot write the relation.
- Ret/state summaries for date/calendar objects need care (a Calendar's
  state is big); the 8.31 truncation lesson applies — whole values,
  typed, capped count not capped width.
- Cost: one extra bounded fuzz pass per leg when the flag is on.

## Decisions needed before any pre-registration

1. Is divergence-fact-to-prompt acceptable at all, or does it cross a
   line (evidence derived from the patched build steering invention)?
   My read: it is the same legitimacy class as replay-relations-on-
   patched — the patched build already speaks to the pipeline; this
   only makes the speech structured. But it is a design call, not mine.
2. Capture scope: changed methods only (diffcov's set), or also their
   direct callers (Lang-63's distortion may only be visible in
   `formatPeriod`'s composed output, one frame up)?
3. K and the ranking rule (frequency vs diversity of input shapes).
4. Whether the anti-anchoring lint (expected-equals-logged-value) is a
   drop or a demote.

If the answer to (1) is yes, the pre-registration would gate on: guard
fixtures both signs; clean-leg hard-stop; Math-65-c and Chart-26-c as
correct-leg canaries; and the honest prediction that Lang-63 converts
only if its divergence is reachable AND the nominated observable has a
documented contract to assert — either half can fail independently, and
the read must say which.
