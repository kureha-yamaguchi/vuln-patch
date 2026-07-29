# Items 2c, 2d, 2e — the rest of the pair forensics

Evidence: the two archived paired runs. No new runs, no LLM calls.

---

## 2c — how was Closure-38 caught, and did we cross our own safety line?

**The line held.** The catch is legitimate.

Closure-38's fake patch drew 7 soundness reviews in roll B. Five were dismissed,
two were kept, and the split falls exactly where it should.

The two kept alarms are both anchored on the value the failing test itself pins —
compact-printing `x- -0` must produce `x- -0.0;`. The reviewer's own words: one
"asserts the trusted test-pinned contract", the other asserts "the exact behavior
required by the trusted regression test" on "the pinned input `x- -0`".

The five dismissed alarms were all about formatting the test does *not* pin, and
were dismissed for precisely that reason:

* a missing trailing semicolon at end-of-file
* behaviour at `x- -1` — "a different observable (negative integer literal
  separation)"
* an artefact of `normalizeCode` stripping whitespace
* a separate AST-side expectation rather than the parsed-source one
* preserving a unary plus in `x-+0`

Two further questions asked whether those unpinned observables carried family
duty; both answered NO, both for the right reason.

So the concern behind 2c — that we might have caught a fake by keeping an alarm
about formatting the tests don't pin — does not apply. The reviewer separated
pinned from unpinned correctly and convicted only on the pinned observable.

---

## 2d — was Chart-19 starved by the rule budget?

**No. Do not run the 12-versus-16 experiment; its precondition fails.**

Chart-19's winning mechanism concerns the populated container receiver — the
installed-axis-index family. Those rules were **proposed in both rolls**: 12
distinct `categoryplot-*` rules in roll A, 14 in roll B, including
`categoryplot_installed_range_axis_index`,
`categoryplot-setrangeaxis-roundtrip-index` and
`categoryplot-rangeaxis-roundtrip-same-reference`.

**Not one of them reached the reviewer.** Exactly one rule per roll did, and it
was an `objectlist` rule both times.

They did not die at a cap. They died at harness construction.

### The general finding, which is the important part

Across both rolls, all 30 cases:

```
harness build attempts        750
  accepted                    285  (38%)
  rejected                    240  (32%)
  novelty-gate pass           225  (30%)
```

**Why the 240 were rejected:**

| reason | count | share |
|---|---|---|
| the alarm is thrown inside a `try` whose `catch (Throwable)` swallows it — "the alarm can never be heard" | 65 | 27% |
| catches an exception into a bare boolean flag, then raises the alarm on the flag | 65 | 27% |
| did not compile | 40 | 17% |
| an alarm has no oracle ID / name | 34 | 14% |
| compiled, but did not trigger the bug on the known-buggy build | 18 | 8% |
| re-throws a caught exception without preserving the original | 6 | 2% |

The first, second and last are one family — alarms and exceptions tangled up in
try/catch — and together with the unnamed-alarm case they are **170 of 240
rejections (71%)**. The bare-boolean mistake alone appears in **30 of 60
leg-runs**, so it is systemic, not a Chart-19 quirk.

Every one of these is mechanically detected and precisely diagnosed — the
rejection messages name the line and the fix. The attempt is then **discarded
rather than repaired**.

That reframes the whole budget question. The nominal budget is not the binding
constraint; the waste inside it is. Roughly a third of all build attempts are
thrown away, and about seven in ten of those are the same handful of structural
mistakes made over and over. Raising the cap from 12 to 16 buys more attempts
that fail at the same rate. Recovering the wasted ones costs nothing extra per
run.

Stated generally, so it names no bug: *when a generated artefact is rejected for
a mechanically-identified structural defect that the rejection message can
already locate, repair it in place instead of discarding the attempt.*

This is now the strongest available recall lever, and it displaces item 4.

---

## 2e — classifying the two flipping misses

A correction to the question first: roll A **caught** Lang-60 and **missed**
Math-73; roll B did the reverse. So the two misses to classify are Math-73 in
roll A and Lang-60 in roll B.

### Math-73's fake patch, missed in roll A — *triggered, then correctly dropped*

The reviewer was called 3 times. One alarm was judged trustworthy: that
`solve(f, min, max, initial)` must not complete when the function values at all
three points share a sign. Then the family-duty question answered NO, and the
indiscriminate-rate gate dropped it.

The drop was **right on the evidence**. That check fires on the known-broken
build on essentially every input, so it cannot distinguish the two builds and
proves nothing. What was missing was a *discriminating* check, and in roll B one
was written — a different rule showing different observed values on the two
builds, which was kept and produced the catch.

So: not a judging failure, and not a gate failure. Both behaved correctly on what
they were given. The miss is a rule-quality problem.

### Lang-60's fake patch, missed in roll B — *never triggered*

The reviewer was called **0 times**. Fuzzing ran normally (6 patched-fuzz passes,
13 replays) and nothing ever complained. No alarm was raised, so there was
nothing to judge.

Worth noting alongside 2b: Lang-60's *correct* patch also drew 0 reviews in
roll A. This is a case where the checks written rarely fire at all, in either
direction — which is why it flips both ways on luck.

---

## What changes as a result

* **Item 4 (rule budget 12 vs 16) — cancelled.** Its precondition fails: the
  rules were proposed, not starved.
* **New lever, ranked above item 4's old slot:** repair mechanically-diagnosed
  harness rejections in place rather than discarding the attempt. ~71% of
  rejections are already precisely located by the existing messages.
* **Items 2c and 2e produce no new work.** 2c confirms the safety line held. 2e
  confirms both misses are upstream of judging — one a non-discriminating rule,
  one no alarm at all. Both point at the same place as 2d: the quality and
  survival of the checks we write.
