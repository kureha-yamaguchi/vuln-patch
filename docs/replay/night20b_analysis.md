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

---

## CORRECTIONS to this document (2026-07-28, from the attribution pass)

1. **"One constant-structure check on the same leg was demoted" (Chart-19) — WRONG.** Chart-19
   had **zero** demotions. My grep counted the phrase "structural blind spot" appearing in the
   DIRECTIVE'S OWN PROMPT TEXT (the directive ends "...is reported as a structural blind spot"),
   not a lint event. Verified: the single hit in that trace is inside the injected instruction
   block. Chart-19's recovery is entirely the generation directive (PART A); the lint contributed
   nothing there — on the very class it was written for.
2. **The Chart-19 catch also REQUIRED the rate-5C revert.** Its convicting relation carries
   `[fire-rate fact] buggy build 20000/20000 = 100%`, above INTRINSIC_FIRE_RATIO (0.95) — the
   reverted rate path would have dropped it. So the revert is load-bearing for the campaign's
   headline recall gain, not merely a neutral rollback.
3. **The same revert also cost a false accusation.** Math-73-Arja_c's `endpoint-min-return` FP
   carries `buggy 999/1000 = 100% … intrinsic to the check/setup construction` — delivered, and
   ignored by the judge. The reverted path would have dropped it. One revert bought Chart-19 and
   paid for Math-73-c; that is the honest ledger of that decision, and it argues the rate signal
   is real but belongs in a **mechanical** drop with a family-duty escape, not in the judge's
   discretion.

## NEW BUG found by the attribution pass (Chart-26) — a false fact, dangerous direction

Chart-26's FP evidence carries this delivered fact:

> "the buggy build runs this exact input WITHOUT firing this check — the patch introduced the
> violation here"

**It is false.** The buggy build *threw inside `axis.draw`* and the exception was swallowed by the
harness's `catch (Exception e) { return; }`. The muted-replay cannot distinguish "the check did
not fire" from "execution never reached the check because it was skipped" — so it reported a
skip as a clean run and manufactured evidence AGAINST a correct patch.

This is the same failure shape as the cycle-2 Closure-70 inverted replay fact (a shadowed replay
read as exculpatory), in a new place, and it fails in the dangerous direction. It belongs at the
top of cycle 6 alongside delivery.

## Lint assessment (its first live run)

7 demotions across 4 of 20 legs, 226 screened relations (~3%) — not over-aggressive, no leg fully
demoted. But two real qualifications: **4 of 7 fired on `BrentSolver`/`SinFunction`**, which are
not containers at all (the receiver-state detector is too broad outside collection-like types),
and it produced **zero** demotions on Chart-19. On this evidence the lint is currently
decorative: it has not yet been shown to change any outcome, and it mis-targets more often than
it targets. Keep (it is fail-soft and cheap) but do not credit it, and narrow the detector.

## Revised attribution of the recall gain

- **Chart-19 — ATTRIBUTED** to structure-from-data + the rate revert. The full chain closes:
  item-4 proved constants made the discriminating state unreachable; the directive made install
  indices fuzz-derived (`consumeInt(1,4)`); all four patched firings occurred on the populated
  (sparse) receiver; night20's literal-index version was dense and missed everything.
- **Lang-63 — ATTRIBUTED** to the directive's fresh-vs-mutated clause: the receiver is now built
  clone-and-mutate from fuzz deltas, whereas night20 set fields absolutely and the judge
  dismissed it via a day-borrow counterexample that absolute setting admits.
- **Closure-92 — SYNTHESIS LUCK, not credited.** Its convicting check hard-codes
  `repeat('a',45)`/`repeat('b',46)`, i.e. cosmetic label fuzzing — the opposite of the directive
  — and its citation was equally available in the prior roll.

So of the three never-before-caught legs, **two are mechanism-attributed and one is luck.**

### Chronic-FP classification (2026-07-28, night20b)

Per-firing triage of the three chronic false accusations: dirs `16_patch1-Closure-62-Jaid_c`,
`18_patch1-Math-30-CapGen_c`, `19_patch1-Math-65-CapGen_c` — all CORRECT patches, all convicted.
Eight firings survived triage as `VERDICT: SOUND` across the three legs; every quote below is from
the run's own `trace.md`.

#### Leg 16 — Closure-62 (4 convicting firings: 1× (a), 3× (b))

**(a) `end-of-line-caret`** (call [96], verdict L12523). Evidence block carries **only**
`[buggy-replay fact]`, and it says the question is unanswered:

> "[buggy-replay fact] on this exact input a DIFFERENT check fired first on the buggy build
> (lifted-test), so whether THIS check fires there is UNKNOWN — the replay is shadowed, not
> confirming."

Missing: `[fire-rate fact]`, `[buggy-scan fact]`, `[universal-screen fact]`, `[muted-replay fact]`,
`[screen-decision fact]`, any `[fact:` tag. `one_door_entry` matched=**False** (so no screen-decision
/ one-door fire-rate path); `universal_screen_entry` matched=False means the universal screen *did*
run and attached nothing — fail-open, no diagnostic. And the muted re-replay produced no note at
all: `muted_replay_note` returns `None` when the target stayed quiet but the muted run crashed at
*another* sibling oracle, and the re-replay is "**bounded to one muted re-replay**"
(`src/java/run.py:2496`). Verdict `CITATION: NONE`.

**(b) `lifted-seed`** (verdict L13174). The clearing fact was delivered:

> "[buggy-replay fact] [fact:not-compared] the exact firing input fires the SAME check on the BUGGY
> build … [screen-decision fact] … whose screening decision was: silent on buggy (tripwire)."

Judge: `SOUND … a correct implementation cannot legitimately return the observed output`,
`CITATION: NONE`. Nothing mechanical stopped it: `[fact:not-compared]` is in
`_NON_TERMINAL_FACT_TAGS`, so `terminal_profile()` returns `None` by construction.

**(b) `null-source-eol-caret`** (verdict L21336). Delivered:

> "[fire-rate fact] buggy build 999/1000 = 100% of random valid inputs. fires on essentially every
> input on the buggy build (100%) — the firing is intrinsic to the check/setup construction, not a
> detection of the defect."

Judge: `SOUND … the trusted tests pin exactly that boundary behavior`, `CITATION: NONE`.
100% ≥ `INTRINSIC_FIRE_RATIO` (0.95) — `fire_rate_is_terminal` would drop this, but the rate path is
not consulted (reverted 2026-07-28).

**(b) relation-replay track** (call [149], verdict L21889). Delivered:

> "[replay comparison fact] this relation fired on 5% of fuzzed inputs on the BUGGY build at
> screening and was NOT direction-confirmed there … it fires on BOTH builds away from the trigger,
> so the patch did not change the behaviour it measures. … (b) it has no documented source (an
> invented plausibility) … pre-existing surface: DISMISS."

Judge: `SOUND`, `CITATION: NONE` — i.e. no documented source shown, which is that fact's own
branch (b) DISMISS condition, and it kept anyway.

#### Leg 18 — Math-30 (2 convicting firings: 2× (b))

**(b) `overflow-boundary-monotone`** (call [66], verdict L4977). This is the *best-instrumented*
evidence block in the three legs — `[latent oracle]`, `[buggy-replay fact]`, `[muted-replay fact]`,
`[fire-rate fact]`, `[screen-decision fact]`, `[fact:not-compared]` all present. Two independent
clearing facts:

> "[muted-replay fact] [fact:not-compared] with the shadowing check(s) lifted-big-data-set silenced,
> THIS check fires on the BUGGY build at this exact input — the same check fires on both builds"

> "[fire-rate fact] buggy build 20000/20000 = 100% … intrinsic to the check/setup construction, not
> a detection of the defect."

Judge: `SOUND — For the specific completed calls … a correct Mann-Whitney asymptotic p-value
implementation must compute a finite value with z=-sqrt(3/(2n+1)) … returning NaN … cannot be
correct behavior`, `CITATION: NONE`. A pure from-first-principles assertion, no grounding, both
mechanical facts overridden. Nothing was missing; nothing mechanical fired
(`not-compared` = non-terminal, rate path reverted).

**(b) relation-replay track** (call [79], verdict L5443). Delivered:

> "[fire-rate fact] buggy build 712/1385 = 51%; patched build 1384/1384 = 100% … indiscriminate;
> the firing is intrinsic to the check/setup construction … **Keep only with a shown contract that
> makes every one of those inputs a genuine violation.**"

Judge: `SOUND`, `CITATION: NONE`. The fact stated its own keep-condition and the judge kept without
meeting it.

#### Leg 19 — Math-65 (2 convicting firings: 1× (a), 1× (c))

**(a) `chiSquare-inversely-scales-with-uniform-weight-factor`, harness track** (call [81], verdict
L10821). Present: `[buggy-replay fact]` (shadowed by `circle-dense-errors-0`, "UNKNOWN") and
`[screen-decision fact] … kept: direction-confirmed`. Missing: `[fire-rate fact]`,
`[muted-replay fact]`, `[buggy-scan fact]`, `[universal-screen fact]`, every `[fact:` tag.
Three concrete reasons, all mechanical:
1. `one_door_entry` matched=**True**, so the universal-screen path is gated off
   (`if _fired_ids and not _one_door_matched …`, `run.py:2999`).
2. The one-door path called `fire_rate_fact` with buggy 3953/20000 = **19.8%** and patched counts
   `None` — the replay pass runs *later* in `main` (`run.py:2910` comment) — so no branch matched
   (0.198 < `MAX_FIRE_RATIO` 0.20 and < `INTRINSIC_FIRE_RATIO`) and it returned `None`.
3. This leg has **zero** `[muted-replay fact]` anywhere in the trace: same single-pass muted-replay
   limitation as Closure-62.

Overlay: the SAME relation was judged `UNSOUND` twice in the same run (L11921, L13443) citing
`"chiSquare += residual * residual / residualsWeights[i];"`, so the soundness evidence was in front
of the judge and sufficient — 2 SOUND / 2 UNSOUND on one relation, i.e. the 5/10-flip variance.

**(c) relation-replay track** (call [97], verdict L12713). A delivered fact is factually wrong:

> "[fire-rate fact] buggy build 3953/20000 = 20%; patched build 20000/20000 = 100% … the check is
> **silent (or near-silent) on the known-broken code** and loud on the patch, i.e. the PATCH
> introduced this divergence. This is a strong discrimination signal, NOT grounds to indict the
> check."

19.8% — roughly one in five random valid inputs on the known-broken build — is neither silent nor
near-silent. The `asymmetric` branch of `fire_rate_fact` fires on *any* buggy rate below
`MAX_FIRE_RATIO` (0.20) and asserts silence, then converts that into an explicit pro-keep
instruction. Same shape as the Chart-26 false fact, different site. **c34b8f9 does not fix this** —
that commit only touches the diverted-replay "ran clean" claim in `muted_replay_note`.

#### The concrete missing measurement, per leg (input to item 4)

- **Closure-62 / `end-of-line-caret`:** does this check fire on the **buggy** build at this exact
  input? Delivered as `[muted-replay fact]` on the **harness/one-door** track. Requires the muted
  re-replay to *iterate*: mute the shadowing set; if the muted run still crashes at another sibling,
  add it to the mute set and repeat until the target fires or the run completes. Today it is one
  bounded pass and returns `None` on "crashed elsewhere". (Its same-family sibling
  `null-source-eol-caret` already measured buggy 999/1000 = 100%, so the answer is near-certain.)
- **Math-30:** nothing is missing. Both convicting firings already carry the clearing fact.
- **Math-65 / harness track:** (i) the same iterated muted buggy re-replay for
  `chiSquare-inversely-scales-…` shadowed by `circle-dense-errors-0`; (ii) plumb the buggy screen
  rate to the one-door track **unconditionally** — 3953/20000 on the known-broken build is an
  indictment in its own right, but `fire_rate_fact` emits nothing below 0.20.
- **Math-65 / replay track:** not a delivery item — a correctness fix. Gate the "silent (or
  near-silent)" wording on a genuine near-zero buggy rate; state rates neutrally in between.

#### Dominant classification and what it scopes item 4 to

**Tally of the 8 convicting firings: (a) 2 · (b) 5 · (c) 1.** Dominant = **(b) COLLECTED BUT
IGNORED**, and the two (a)s are the *weaker* half of the evidence — in both, a same-family sibling
firing in the same leg already carried the fact.

Every (b) shares one signature: `CITATION: NONE` plus a from-first-principles "no correct
implementation could…" argument, over the top of a delivered mechanical fact that states the
opposite. Math-30's `overflow-boundary-monotone` is the proof case: it has the richest evidence
block of any firing in the three legs — muted-replay fires-on-both **and** buggy 100% fire rate —
and it still convicted a correct patch.

**Item 4 is therefore NOT delivery.** More facts will not move these legs; the facts are already
there and are being argued past. Item 4 must be **mechanical enforcement**:
- the item-3 rate rule as a **drop**, not a note: buggy ≥ `INTRINSIC_FIRE_RATIO` ⇒ dropped with the
  family-duty escape (this alone kills Closure-62 `null-source-eol-caret` at 100% and Math-30
  `overflow-boundary-monotone` at 100%);
- a **terminal-style drop for the fires-on-both profile**, i.e. promote `[fact:not-compared]`
  out of `_NON_TERMINAL_FACT_TAGS` when the muted replay confirms the target fires on the buggy
  build (this kills Closure-62 `lifted-seed` and Math-30 `overflow-boundary-monotone`);
- enforce `CITATION: NONE` + "keep only with a shown contract" as a **mechanical** conjunction
  rather than prose the judge may decline (Math-30 replay track, Closure-62 replay track).

Plumbing (the iterated muted re-replay, and the unconditional buggy-rate delivery to the one-door
track) is a **secondary** item, worth ~2 of 8 firings, and both Math-65 harness-track and
Closure-62 `end-of-line-caret` would still need the mechanical drop above to convert the fact into
a dismissal once delivered. The single (c) is a third, independent correctness fix in
`fire_rate_fact`'s `asymmetric` branch.
