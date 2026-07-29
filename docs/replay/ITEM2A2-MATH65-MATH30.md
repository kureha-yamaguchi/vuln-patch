# Math-65 and Math-30 — completing the chronic-accusation forensics

The two chronic false accusations item 2a did not answer. Evidence: the two
archived paired runs. No new runs, no LLM calls.

They turn out to have **different causes**, and only one of them licenses a fix.

---

## Math-65 — licensed. The predicted pattern is exactly right.

The whole dispute is one formula. Does `getChiSquare()` compute
`Σ weight[i]·residual[i]²`, or `Σ residual[i]²/weight[i]`? The code divides. The
harness's check recomputes it multiplying. So the check is wrong, and the only
question is whether the reviewer notices.

**The verdicts split precisely on whether they quote the code's own line.**

Dismissals — correct, since this patch is good — all cite the real line:

```
"chiSquare += residual * residual / residualsWeights[i];"
```

One of them says so outright: the API's actual behaviour is "summing
`residual^2 / weight` rather than the harness's **invented** `weight * residual^2`".

Accusations — all wrong — cite **NONE**, every one, and assert the inverse from
memory:

| accusing verdict's claim | citation |
|---|---|
| "justified directly by the documented formulas" | NONE |
| "must equal Σ weight[i]·residual[i]^2 … the class javadoc explicitly defines" | NONE |
| "documented as the chi-square/weighted-residual criterion" | NONE |
| "must equal the recomputed weighted sum `w1*r0^2 + w2…`" | NONE |

Four accusations, four appeals to a remembered javadoc, zero quotes. Three
dismissals, three verbatim quotes of the line that settles it.

### The fix this licenses — and the important correction to it

The obvious fix is "hand the reviewer that line." **It is already handed the
line.** Citations are mechanically checked for literal presence in the shown
material, so the dismissals quoting it prove it is there.

I located it: the line appears **exactly once, at character 27,051 of a 59,830
character prompt**, inside the context dump. Whether the reviewer finds it is
chance — which is precisely the coin-flip we observe.

So this is the *collected-but-ignored* class, not a delivery gap, and the fix has
to be placement rather than delivery:

> When a fired check recomputes a quantity that the code under test also
> computes, locate the code's own computation of that quantity and restate it
> verbatim as a dedicated fact block adjacent to the firing, instead of leaving
> it buried in the context dump.

Mechanical and general: the check names a method of the class under test, that
method exists in the shown source, extract the statements that assign the
returned quantity, emit as a fact. It names no bug and no project.

This is the established winning pattern — compute a fact into the evidence rather
than asking the reviewer to try harder — with the twist that here the fact
already exists and needs *relocating*, not computing.

**Earns a batch slot.**

---

## Math-30 — confirmed residual. No fix licensed.

Math-30 is a different animal, and the machinery is largely working.

Six of its alarms were correctly dismissed, and every one of those dismissals
cites a computed fact — `[fact:fires-on-both-confirmed]`,
`[fact:identical-on-both]`, `[fact:rate-indiscriminate]`. The facts we built are
doing their job here.

What survives is four accusations asserting properties that sound universally
true of the statistic involved:

* a p-value must lie in [0,1]
* a p-value must be symmetric when the two samples are swapped
* a p-value must not be NaN
* at the midpoint the p-value must be exactly 1

All cite NONE. And they fire because the fuzzer drives the input sizes into a
regime where the correct implementation legitimately produces NaN.

### The regime is exactly an arithmetic overflow boundary

The firing inputs are at length 46341. That is not arbitrary:

```
46340² = 2,147,395,600   fits in a Java int
46341² = 2,147,488,281   exceeds int max (2,147,483,647)
```

**46341 is the first integer whose square overflows a Java int.** The fuzzer
found the boundary precisely, and at that point "what a correct implementation
must return" is genuinely undefined. The checks assert a universal property
anyway, and the reviewer has no fact telling it the input sits on an overflow
edge.

### Why this does not become a batch item

The tempting fix is a new computed fact: state when a firing input sits at or
beyond a representable-arithmetic boundary. It is mechanical and it would be
general in form.

But I checked how often the pattern actually occurs before proposing it. Across
all 30 cases in both rolls, near-overflow magnitudes appear in the reviewed
firings of **2 legs** — Math-30's and Math-2's correct patches. Two cases, and
one of them (Math-2) was correctly cleared anyway.

**n=2 is too thin to build on.** That is the same standard used to reject the
citation filter in 2b, and the same mistake I made this morning generalising a
recall theory from a single leg. Recorded as a parked research question with the
evidence attached, not as work.

Math-30 therefore stands as the pre-declared, accepted false accusation. We
already refused to bend the 95% threshold for it; nothing here changes that. Its
mechanism is now named, which is the useful outcome: checks asserting universal
properties in degenerate input regimes the failing test never touches.

---

## Forensics complete — the final batch

Every chronic accusation is now explained and every build item is evidence-backed.

**Main batch, one validation pass:**

1. Within-run reuse of the reviewer's answer (item 3) — *first* confirm no site
   relies on repeated sampling as a vote.
2. Recognise project-defined assertion helpers in the expected-value extractor
   (2a; ~34% of cases).
3. Compare non-numeric expected values, not only numbers (2a; 10 cases, all
   correct patches).
4. Fail loudly on missing data fields instead of returning nothing — kills the
   bug class that produced three near-misses in one day.
5. **New, from Math-65:** relocate the code's own computation of a disputed
   quantity into a dedicated fact block beside the firing.

**Separate gate, own validation:** repair mechanically-diagnosed harness
rejections in place rather than discarding the attempt (2d). Validated offline by
running the repair over the 170 archived rejected harnesses and counting how many
then compile and pass acceptance — no fixture and no run needed.

**Then:** items 5 and 6, then the paired measurement against the two-tier bar.

**Parked, on the record, not being worked:** the accusation-side evidence
asymmetry as a named research question (the eventual answer is accusation-side
facts, not a citation filter); the arithmetic-boundary fact at n=2; Math-30's
threshold; and the previously parked items.
