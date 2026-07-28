# night20 rerun (night20b, 2026-07-28) — first run carrying all cycle-5 + Chart-19 fixes

20 legs, width 7 (matched to night20 for a valid pair), 4.95M tokens.
Raw: `night20b_20260728.md`. Archive: `runs-archive/runs/night20b_20260728_153138`.

## Result vs the night20 pair

| | night20 (pre-fixes) | night20b | delta |
|---|---|---|---|
| catches (of 14) | 8 | **11** | +3 |
| recall | 0.57 | **0.79** | +0.22 |
| clean correct legs (of 6) | 3 | **1** | −2 |
| precision | 0.73 | 0.69 | −0.04 |
| F1 | 0.64 | **0.73** | +0.09 |

**Best F1 measured on this set.** Recall is the largest jump of the campaign; precision paid part
of it back.

## Recall: three legs caught that had NEVER been caught in five prior rolls

Chart-19, Lang-63, Closure-92 — all three were in the "invents but never fires" class from the
four-roll tabulation. Chart-19's mechanism is attributable: its checks now draw *structural*
values from the fuzzer (`consumeInt(0,6)`, `consumeInt(1,3)`, `consumeInt(0,4)` governing element
counts and install indices), which is precisely the sparse/gapped container state the overfit
needs to be exposed — the state the item-4 replay proved was unreachable when structure came from
constants. One constant-structure check on the same leg was demoted, so the lint discriminates
rather than blanket-flags.

Lang-63 and Closure-92 are NOT attributed yet — neither was a target of the structure-from-data
fix. Their catches need a trace read before being credited to anything.

Still missed: Lang-60, Closure-38, Math-104. Closure-38's miss is consistent with the cycle-5
close — its divergence lives in unpinned formatting and its rows were reclassified contested, so
a dismissal there is defensible rather than a failure.

## Precision: 5 false accusations, dominated by the known leak class

Closure-62, Chart-26, Math-30, Math-65, Math-73-c. Math-30 and Math-65 are the chronic
invented-contract FPs. The two regressions vs night20 fired on:
- Chart-26: `bar-chart-null-info` — draw-with-null throwing StringIndexOutOfBounds (the leg's
  long-standing trusted-lift pattern);
- Math-73-c: `endpoint-min-return` / `function-value-state-consistency` — the endpoint-root
  family, which appears in the iteration-1/2 leak lists repeatedly.

Neither is obviously a structure-from-data artifact; both look like the **invented-contract leak
class**, which is exactly what cycle 6's delivery work targets (those checks' fire-rate facts are
never delivered into the evidence the judge reads). That attribution is a hypothesis, not
established — it needs the same per-firing triage discipline, and it is the first thing to check
before crediting or blaming the new generation directive.

## Caveats that bound every number above

- **Single roll.** Measured variance on this pipeline: 5 of 10 *untouched* rows flipped verdict
  between two identical replay draws. A one-roll delta of ±2 legs is inside that band.
- The two-roll rule applies: nothing here is "fixed" until a second roll agrees, with the
  mechanism visible in the trace.

## Next
1. Trace-read Chart-19 (confirm the convicting check fired via a fuzz-derived gap), Lang-63 and
   Closure-92 (unattributed), and the two precision regressions.
2. Cycle-6 delivery work — the leak class.
3. Second night20 roll for the two-roll rule, then the paired 30-leg measurement.

### night20b mechanism attribution (2026-07-28)

Trace-level attribution of the three new catches and the two precision regressions, plus lint
stats. Everything below is quoted from the run's own `trace.md` files.

#### A. Chart-19 (dir 07) — **ATTRIBUTED to structure-from-data**

The overfit appends a null check *after* the search loop in `AbstractObjectList.indexOf`, whose
loop is reference-identity:

```java
protected int indexOf(Object object) {
    for (int index = 0; index < this.size; index++) {
        if (this.objects[index] == object) { return (index); }
    }
    return -1;                        // <- the patch inserts its throw just above this
}
```

So `indexOf(null)` returns early on the **first null slot** and never reaches the throw. A null
slot exists only when an axis is installed at index >= 2, leaving a hole at index 1.

(i) The convicting check draws exactly that install index from `data`
(`relation_replay_kept` = `categoryplot-null-range-axis-rejected-in-all-states`):

```java
int idx = data.consumeInt(1, 4);
...
p2.setRangeAxis(idx, new org.jfree.chart.axis.NumberAxis(y1));
```

Its sibling `categoryplot-getRangeAxisIndex-null-throws` (which produced the patched-side crash)
uses `int k = data.consumeInt(1, 3);` and builds every label with
`data.consumeAsciiString(data.consumeInt(0, 6))`. The install index — the container's *structure* —
is fuzz-drawn, not a literal.

(ii) All four patched-build firings are on the **populated** (sparse) receiver, never the fresh one:

```
FIRED — [oracle:categoryplot-getRangeAxisIndex-null-throws-populated] ... populated plot completed normally
FIRED — [oracle:categoryplot-domain-null-throws-populated] ... but populated plot completed normally
FIRED — [oracle:range-axis-null-populated] ... populated plot.getRangeAxisIndex(null) should throw
```

The night20 pre-fix roll had the *same relation concept* with a **constant** index —
`rangeAxisIndex-null-rejected-independent-of-plot-state`, "one additionally mutated by
`setRangeAxis(1, axis)`" — index 1 leaves no hole, and that roll was `overfit MISSED` with every
harness and every replay quiet. Constant index -> dense -> quiet; fuzzed index -> hole -> fires.
The causal chain the item-4 replay predicted is confirmed end to end.

One caveat and one correction:
- **Correction to the section above:** Chart-19 had **zero** `constant_receiver_state` demotions
  (`demoted=0` in dir 07). The earlier claim that "one constant-structure check on the same leg was
  demoted" is wrong — the discrimination happened at *generation*, not at the lint.
- The keep also **required the rate-5C revert**. The kept firing carries
  `[fire-rate fact] buggy build 20000/20000 = 100%; patched build 12051/20000 = 60%`, and
  `fire_rate_is_terminal` fires at `buggy >= INTRINSIC_FIRE_RATIO (0.95)`. With the rate-based 5C
  terminal path still wired, this catch would have been dropped.

#### B. Lang-63 (dir 09) — **mechanism-attributed: structure-from-data (fresh-vs-mutated clause)**

Convicting check `constructed-known-months`, single judge call, `VERDICT: SOUND`, observed
`chosenMonths=02 actual=-9 monthDelta=2 extraDays=1`. Its receiver is built by **clone-and-mutate
from fuzz-drawn deltas**:

```java
int monthDelta = data.consumeInt(1, 11);
int extraDays  = data.consumeInt(1, 20);
Calendar end = (Calendar) start.clone();
end.add(Calendar.MONTH, monthDelta);
end.add(Calendar.DAY_OF_MONTH, extraDays);
```

night20's version of the same idea set the end **absolutely** and was dismissed:

```java
e.set(Calendar.YEAR, startYear + 1);
e.set(Calendar.MONTH, monthDelta - 1);
e.set(Calendar.DAY_OF_MONTH, endDay);
```
> VERDICT: UNSOUND — "a correct `formatPeriod` ... can legitimately return `"08"` for the observed
> input Dec 31, 1900 -> Oct 1, 1901 (since end-day 1 is before start-day 31)"

Absolute field-setting admits day-borrow counterexamples; add-whole-months-then-add-days does not,
so the known answer becomes provable and the judge found no counterexample. This is the directive's
"whether it is freshly built or already mutated, and in what ORDER those operations happened"
clause applied outside containers. Secondary support: the firing carries
`[fact:fires-both-different-values]`, which is in `_NON_TERMINAL_FACT_TAGS`, so `terminal_profile`
returns `None` and the prose "the SAME check fires on BOTH builds" cannot trip the terminal gate.
night20 had **zero** `[fact:` tags anywhere.

#### C. Closure-92 (dir 06) — **synthesis luck** (not mechanism-attributable)

Convicting family is the prefix-order / `rename-boundary` family. Its structural values are
**hard-coded literals deliberately straddling the patch's `indexOf('.','.')` boundary** (`'.'` is
46), i.e. the *opposite* of the directive:

```java
String root45 = repeat('a', 45);
String root46 = repeat('b', 46);
...
if (!canon45.equals(canon46)) { ... "root-length rename invariance across the patched indexOf boundary" ... }
```

Only the segment names are fuzzed (`sanitizeIdent(data.consumeAsciiString(12), "foo")`) — textbook
cosmetic label fuzzing. The keep rests on a documented contract that was **equally available in
night20** ("The prefix namespaces must be registered in order from shortest to longest" appears 21x
in the night20 trace). Novelty/family-coverage pressure was identical in both rolls (6 rejection
messages each, same "receiver-state axis" wording), no re-ask fired, and the STRUCTURE FROM DATA
directive reaches relation synthesis, not the harness generator that produced this check. In
night20 all five patched-side firings were the brittle exact-string `lifted-seed` family, all
correctly dismissed on the missing-semicolon literal. Verdict: **synthesis luck** — a
patch-directed boundary check no prior roll invented.

#### D. Regression Chart-26 (dir 17) — **H1, invented-contract leak**

Convicting firing (the `bar-chart-null-info` firing was not the one that survived triage):

```
FIRED — [oracle:axis-draw-preserves-equality-at-null-owner] consistency violation:
        axis no longer equals an identically configured twin after draw
VERDICT: SOUND
WHY: The fired check only runs when `axis.draw(...)` completes successfully, and for this harness
     setup a correct implementation has no shown contract-based reason to mutate an axis so that it
     no longer `equals` an identically configured twin ...
CITATION: NONE
```

**H1 confirmed, H2 refuted.** The evidence block (lines 15260-15321) contains **no `[fire-rate
fact]`, no `[buggy-scan fact]`, and no `[fact:...]` tag at all** — only `[buggy-replay fact]`
(shadowed) and a `[muted-replay fact]`. There is no container in the check: the receiver is
`new NumberAxis(label)` and a twin, with only labels/tooltip/url/cursor/edge drawn from `data` —
cosmetic, no element count, index or gap; this leg had zero demotions. The check family did not
exist in night20 (`0` occurrences), so it is a newly invented contract, not a directive artifact.

Worth flagging separately: the one mechanical fact that *was* delivered is wrong here —
"with the shadowing check(s) `linechart3d-null-info-groundtruth` silenced, the buggy build runs this
exact input WITHOUT firing this check — the patch introduced the violation here". The buggy build
did not satisfy the property; it **threw inside `axis.draw` and was swallowed** by the harness's
`catch (Exception e) { return; }`, because the pre-patch code dereferenced the null owner. The
muted-replay fact cannot distinguish "did not fire" from "was skipped", and here it manufactured a
pro-keep signal. That is a cycle-6 delivery bug in its own right.

#### E. Regression Math-73-Arja_c (dir 20) — **H1 (invented contract), realised through the rate-5C revert**

```
FIRED — [oracle:endpoint-min-return] semantic mismatch: expected returned root=0.5 but got -0.0
VERDICT: SOUND
WHY: The fired check only runs when `solve(quintic, 0.5, endpointMax, endpointInitial)` completes,
     and for this input class a correct solver must return the root location `min` itself—here
     exactly `0.5` ... so completing with `-0.0` cannot be correct.
CITATION: NONE
```

The asserted contract is invented from a paraphrased code comment ("Contract visible in the target
method body: 'return the first endpoint if it is good enough'") and compared **bit-exactly**
(`Double.doubleToLongBits(returned) != Double.doubleToLongBits(endpointMin)`), with the degenerate
part — `endpointMin = 0.5`, an exact root of `QuinticFunction` — a hard-coded literal; only
`endpointMax`, `endpointInitial` and `absAcc` are fuzzed. Not a container check, no fuzz-derived
receiver structure -> **H2 refuted, H1 confirmed**.

Unlike Chart-26, the fact here *was* delivered — and ignored:
`[fire-rate fact] buggy build 999/1000 = 100% of random valid inputs. fires on essentially every
input on the buggy build (100%) — the firing is intrinsic to the check/setup construction, not a
detection of the defect.` `0.999 >= INTRINSIC_FIRE_RATIO` -> `fire_rate_is_terminal` -> **the
reverted rate-based 5C path would have dropped this firing**, and `CITATION: NONE` means the
"keep only with a shown contract" escape does not apply. So the honest accounting is symmetric:
**the same rate-5C revert bought the Chart-19 catch and paid for this false alarm.** Neither
regression is a cost of the structure-from-data directive.

(Note the same leg's second firing, `function-value-state-consistency`, was correctly dismissed
with `CITATION: "[fact:identical-on-both] ..."` — the structured-citation + fact-tag mechanism
working as designed.)

#### F. Lint activity — `constant_receiver_state`

| leg | demotions | screened relations |
|---|---|---|
| 05 Chart-7 | 2 | 14 |
| 08 Closure-38 | 1 | 9 |
| 11 Math-73-ACS | 2 | 12 |
| 20 Math-73-Arja_c | 2 | 14 |
| all other 16 legs | 0 | — |

**4 of 20 legs saw a demotion; 7 demotion events out of 226 screened relations (3%).** No leg had
all its receiver-state checks demoted — the maximum is 2 of 9-14, so the lint is not blanket-flagging
and nothing suggests over-aggression on the drop side.

Two qualifications:
- **Scope drift.** 4 of the 7 demotions (legs 11 and 20) are on `BrentSolver` — "receiver-state
  probe `solver.solve(...)` runs against a container whose STRUCTURE is compile-time constant:
  `f = new SinFunction(...)`, `solver = new BrentSolver(...)`". A numeric solver is not a container;
  there is no element count, index or gap to draw. The advice ("Draw the structure from `data`: the
  number of elements, the indices they are installed at ...") is meaningless there. The receiver-state
  detector is matching too broadly outside collection-like types.
- **Zero teeth where it was aimed.** Chart-19 — the class the lint was written for — produced no
  demotion at all. The Chart-19 recovery is entirely PART A (the generation directive); PART B (the
  screen-side lint) contributed nothing to it on this roll.
