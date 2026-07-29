# Item 2a — why the chronic false accusations survive

Evidence: the two archived paired runs (`final30A_20260729_121819`,
`final30B_20260729_145001`) and the 228 recorded judge decisions in
`tests/fixtures/cases228.jsonl`. No new runs, no LLM calls.

## The Closure-62 question, answered

The todo list said: a rule exists that should dismiss Closure-62's alarms — "the
alarm just rebuilds the test's own scenario, and the real test passes" — find out
why it never fired.

That rule is `trigger_lift_note` in `evidence_facts.py`. It has three branches,
and **only one of them is allowed to say "must be dismissed"**: the branch where
the value the alarm reported *matches* a value the failing test itself pins. The
other two say "this is a real catch, keep it" and "undetermined, judge on
soundness alone".

The rule is wired up and it is delivered. It is not a plumbing failure.

**It never reaches the dismissal branch.** Across both rolls, 13 of these notes
were delivered to a reviewer, on 6 different legs. All 13 landed on
*undetermined*. Zero reached dismiss. Zero reached keep.

| branch | delivered across both rolls |
|---|---|
| matches → "must be dismissed" | **0** |
| differs → "keep, real catch" | 0 |
| undetermined → "judge on soundness alone" | 13 |

In roll A specifically, Closure-62 got no note at all — the six trace matches
there are prompt boilerplate that happens to mention the tag, not delivered
facts. Roll B delivered two, both undetermined.

## Why it is always undetermined

The branch is chosen by `fired_value_vs_trusted`, which compares the alarm's
reported value against the values the failing test pins. Those pinned values come
from `expected_assert_literals`, which lifts the expected argument out of the
test's `assertEquals` calls.

Reconstructed over all 228 recorded cases, using the production extractor on the
recorded failing-test text:

```
cases                                                    228
  failing-test text missing                                0
  extractor returned NOTHING                             204   (89%)
  extractor returned something                            24
      of those: contains a number                         14
                non-numeric only                          10

resulting comparison verdicts:  unknown 226 (99%) · differs 2 (1%) · matches 0
```

**The comparison returns "unknown" for 99% of recorded decisions, and "matches"
— the only verdict that licenses a dismissal — for none of them.** One
extractor's miss rate silently disables the only rule in the system permitted to
say "this alarm is just replaying the test's own scenario".

### Why the extractor misses, for the 204

| reason | count | share |
|---|---|---|
| `assertEquals` present, but the expected argument is computed or a local, not a hard-coded literal | 66 | 32% |
| no JUnit assertion form at all — the test asserts through a project-specific helper | 55 | 27% |
| only `assertTrue` — there is no expected value to lift | 50 | 25% |
| only `fail` — an exception-expectation test | 19 | 9% |
| only `assertPrint` — another project-specific helper | 14 | 7% |

These are five different situations and they do not all have the same fix:

* **69 cases (34%) — `assertTrue` and `fail`.** These tests pin no expected
  value at all. No amount of extractor work helps; there is nothing to compare
  against. For this third of the corpus the value-matching approach is the wrong
  mechanism, and the dismissal rule needs a different basis or must stay silent.
* **69 cases (34%) — project-specific assertion helpers** (`assertPrint` and the
  55 with no JUnit form). Addressable: these are real assertions with real
  expected arguments, and the extractor simply does not recognise the call names.
  The 55 were checked by hand — the recorded text is the complete verbatim test
  body, so this is a genuine extractor gap, not a recording artifact.
* **66 cases (32%) — computed expected values.** Deliberately skipped, and the
  existing docstring's caution is sound: a non-literal is not trustworthy
  provenance. Resolving these would mean evaluating Java expressions. Not
  recommended.
* **10 cases — values extracted, but non-numeric**, and the comparison only
  understands numbers. One sample has pinned values
  `['ClassUtils','Map.Entry','String[]',…]` against an alarm about short class
  names agreeing. A string comparison would resolve it.

  > **CORRECTED — this bullet licensed a fix that was built and then rejected.**
  > It originally ended "All 10 are gold=SOUND, so fixing this can only help
  > precision", and the plain-English summary rendered that as "all 10 are good
  > fixes". Both were wrong.
  >
  > Verified against the fixture: all 10 rows are the **same leg** —
  > `patch1-Lang-41-Arja-plausible_o`, a **fake** patch, label `overfitting`,
  > gold=SOUND on all ten. So the population is ten firings of one bug (n=1 leg),
  > and every one is a finding that should be **kept**.
  >
  > The error was reading `gold=SOUND` as "good fix". `gold` describes the
  > soundness of the **check**, not the correctness of the **patch**, and the two
  > run opposite — a sound check on a fake patch is a legitimate catch. Per
  > `score_replay.py`: "over-kill (gold=SOUND dropped)", i.e. gold=SOUND means
  > keep.
  >
  > So there was never precision upside here. Making these rows comparable
  > creates dismissal pressure on a leg whose findings are correct. Measured
  > alone, the fix produced exactly one dismissal instruction across all 228
  > records and it was the wrong one. Not shipped — see the NOT SHIPPED note in
  > `evidence_facts.py`.

## What this licenses under item 7

Two changes, both general, both validatable offline against the 228 recorded
cases with no LLM spend:

1. **Recognise project-defined assertion helpers** in the extractor — the single
   largest addressable slice (~69 cases, 34%). Stated generally: an assertion is
   any call whose name begins `assert` (plus a configurable set), not only
   `assertEquals`.
2. ~~**Compare non-numeric values** as well as numbers (~10 cases, all correct
   patches). Exact token equality is enough; no inference.~~
   **WITHDRAWN — built, measured alone, rejected.** The "all correct patches"
   description was false: all 10 rows are one *fake* patch
   (`patch1-Lang-41-Arja-plausible_o`) whose findings should be kept. Measured
   in isolation it produced one dismissal instruction across all 228 records,
   and that one was wrong. See the correction above and the NOT SHIPPED note in
   `evidence_facts.py`.

Item 1 makes the dismissal rule fire more often only where a verdict genuinely
exists; whether the alarm is then dismissed still depends on the values actually
matching. Measured alone, it fires the dismissal instruction once, correctly,
with zero wrong firings, and moves extraction-empty from 204 to 173.

**Process note.** These two were licensed by the same paragraph and would have
shipped as one change. Bundled, they measure 2 right / 1 wrong and look like a
net win. Separated, item 1 is 1/0 and item 2 is 0/1. Per-item measurement before
shipping is now the standing rule for the rest of the batch.

Expected effect, stated honestly: this makes an unreachable branch reachable for
roughly a third of cases. It does **not** follow that a third of false
accusations disappear — most of those cases will resolve to *differs* (keep) or
still fail to match. The claim being made is only that the rule stops being dead
code.

## Math-30 and Math-65 — not yet answered

This pass answered the Closure-62 question because that is the one with a named
rule that should have fired. The other two have different shapes and have not
been traced step by step yet:

* **Math-30** remains the pre-declared, accepted false accusation. We already
  refused to bend the 95% threshold for it. Nothing here changes that.
* **Math-65** is untouched by the above; the standing hypothesis from the todo
  list is that honest verdicts cite the code line defining the disputed formula
  and coin-flip verdicts ignore it, which points at handing that line over
  mechanically rather than hoping the reviewer reads the code. Not yet verified
  against the traces.

Both stay open.

## A correction, recorded because it nearly shipped

While measuring the above I first reported that trusted values were empty in 89%
of cases by reading a `trusted_values` field on the fixture rows. **That field
does not exist in the fixture.** Every row returned `None`, so the number
described nothing. It is the same artifact that invalidated earlier replay
measurements when the failing-test block turned out to be empty in every row.

The 89% figure in this document is a different measurement: the production
extractor run over the recorded `failing_test` text, a field that does exist.
It happens to land on the same number, which is exactly the kind of coincidence
that would have let the invalid version pass unnoticed.
