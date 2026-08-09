# Reportable-exception replay study — phase 1 (execution only) — 2026-08-09

**Station:** the relation body shape that `src/java/relations/relation_synth.py`
mandates, executed through the shipped screen/replay path in
`src/java/relations/relation_screen.py` (`measure_single_check` on the buggy
build, `replay_on_patched` on the patched build). The counting wrapper, the
budgets and the decision code are untouched.

**Failure mode addressed:** a relation calls the patch-changed class on an
input it declared valid by construction, the patched build throws, and the
relation's mandated `catch (Exception e) { return; }` swallows the throw, so
the relation reports nothing. That is the Chart-19 mechanism read in
`docs/draw05-routing-reread-2026-08-09.md` §4 (FORK-ORACLE).

**What this is:** the offline replay named in
`docs/reportable-exception-prereg-2026-08-09.md` ("The replay study"). It
answers one question: if the 29 archived legs' kept relations had used the
two-tier catch, which legs would fire on the patched build, and does any
CORRECT leg start firing? Zero LLM calls were made. Phase 2 (the verdict-level
question) is not run here; its input file is produced below.

**Where everything is:** `runs-archive/runs/rex_replay_20260809_074539/` —
`results.jsonl` (one row per relation, 340 rows), `report.md` (the full
per-relation table), `summary.json` (run parameters and the seven builds),
`study_input.jsonl` (exactly what was extracted from the traces), and
`phase2_cases.jsonl` (11 cases in `verifier_replay.py` input format).
Code: `src/java/studies/rex_replay.py`.

---

## The answer, up front

**G-P (precision, the hard stop): PASS. Zero.** No correct leg gained a
firing from the rewrite. Across the 13 correct legs (Math-65-c ×7,
Math-2-SOFix-c ×3, Chart-26-c ×3) and their 151 kept relations, the number of
new tier-2 firings is 0. Two correct-leg relations do fire on the patched
build and not on the buggy build, but both were already firing in the archived
run and neither carries a tier-2 message — details and verbatim text in
"The hard-stop table" below.

**G-R (recall, the point): 2 archived misses converted, both Chart-19, plus 4
already-caught Chart-19 legs that gained an extra firing.** Chart-19 had
exactly two legs whose archived outcome was `overfit MISSED`; the rewrite
converts both. No Lang-63, Lang-41, Math-2 or Math-65 leg changes state.

The prediction in the pre-registration was "Chart-19 legs are the prediction".
That is what happened, and nothing else did.

---

## 1. What was done

### 1.1 Extraction (each leg's own artifacts only)

For each of the 29 legs, its `trace.md` was read start to finish and the kept
relations recovered by replaying the trace's own bookkeeping: every rule
synthesis output supplies relation objects (name / kind / contract / input /
check); every compile-repair output replaces the check of the relation whose
name appears in the repaired snippet; every `screen · <name>` step whose
output is `**kept**` records the relation as it stood at that moment. Nothing
from any other leg was used, and nothing from this study is banked for a
future run.

The cross-check is the leg's own `replay-on-patched` step count: the pipeline
replays exactly the kept set, so the two must agree. **They agree for all 29
legs**, and every kept relation's check body was recovered.

| | count |
|---|---|
| legs | 29 |
| kept-relation screen decisions | 340 |
| `replay-on-patched` steps | 340 |
| kept set matches replay set | 29 / 29 legs |
| check bodies recovered | 340 |
| check bodies missing | 0 |

One extraction detail worth naming, because it would have silently dropped
rows: a screen step writes the relation name truncated to a fixed width, so a
name longer than that never matches its synthesis record exactly. Those are
resolved by unique-prefix match; without it 13 of the 340 bodies would have
gone missing. The per-leg accounting table is in §5.

### 1.2 The transform

Mechanical, label-blind, and identical for correct and overfit legs. Labels
are used only to score, never to decide what to rewrite.

The patch-changed class is read from the patch file's own `---` header (the
patch files on the VM under `/home/code/drr/Patches/`), and the leg directory
name supplies which patch file. **Subtype receivers count**: the checkout's
own sources are walked for `class X ... extends Y`, closed transitively, and
every subclass of the patch-changed class is treated as a probe receiver. This
matters directly for the Chart-19 result and is confirmed below.

Inside each relation's check body:

- A try block whose catch is a broad `catch (Exception|Throwable e)` is a
  candidate. A try with a targeted catch (`catch (IllegalArgumentException
  ok)`) is an expected-rejection contract and is left alone, as the
  pre-registration requires. A broad catch that already throws is a check that
  already reports what the call under test did; also left alone.
- A PROBE statement is a top-level simple statement in such a try whose text
  contains a call whose static owner or receiver type is the patch-changed
  class or one of its subclasses. Constructor calls do not count: the
  pre-registration puts "build inputs/receivers" in tier 1.
- Each probe statement is wrapped in its own try whose catch rethrows
  `RuntimeException("relation <name> violated: unexpected " +
  e.getClass().getName() + " on valid-by-construction input: " +
  e.getMessage())`, and every enclosing broad catch gets a one-line guard so a
  `violated` RuntimeException escapes instead of being turned into a return.
  A `T v = init;` probe has its declaration hoisted out of the new try so `v`
  keeps its scope; definite assignment still holds because the new catch
  always throws.
- Setup statements keep catch-and-return. Anything the transform cannot
  isolate is SKIPPED with a recorded reason.

### 1.3 Execution

Seven distinct (bug, patch) pairs cover the 29 legs. Each was checked out and
patched once, through the pipeline's own `PatchSelector` and
`PatchedProjectBuilder` (trigger verification on, diff-hit instrumentation
off — it only adds counters and cannot change whether a relation fires).
Every leg then ran its own relation set against the shared build; builds
depend only on the patch and the dataset, never on run artifacts.

Each transformed relation was screened on the BUGGY build with
`measure_single_check` and replayed on the PATCHED build with
`replay_on_patched` — the shipped functions, the shipped counting wrapper, and
the budget the archived runs used (`--screen_runs` was not passed in any of
the three suites, so the code default applies):

```
"runs_budget": 20000,
"timeout_seconds": 45,
```

Relations the transform left unchanged were not re-executed: the source is
byte-identical to what the archived run already ran on the same builds, so
re-running them would buy nothing. Their archived replay outcome is carried
into the tables and marked as such.

Wall time, pasted from `summary.json`: `"elapsed_seconds": 2544.3` with four
concurrent builds.

**Subtype confirmation.** The transform did count subtype receivers. From
`summary.json`, the receiver types that counted as the patch-changed class for
Chart-19:

```
"patched_class": "org.jfree.chart.util.AbstractObjectList",
"subclass_names": ["AbstractObjectList", "BooleanList", "ObjectList",
                   "PaintList", "ShapeList", "StrokeList"]
```

`ObjectList` is in that list, which is what makes `new ObjectList().indexOf(
null)` a probe. Every one of the eleven new firings is a call on an
`ObjectList` receiver, so the Chart-19 conversion claim rests on subtype
resolution having worked, and it did. The same closure ran for the other six
builds — for example Chart-26's patch-changed class `Axis` resolved to 17
receiver types including `CategoryAxis` and `NumberAxis`, and Math-65's
`AbstractLeastSquaresOptimizer` resolved to `GaussNewtonOptimizer` and
`LevenbergMarquardtOptimizer`.

---

## 2. Transform and skip accounting

Nothing was silently dropped. Every one of the 340 relations has a row in
`results.jsonl` with its status.

| status | relations | what it means |
|---|---|---|
| transformed | 244 | at least one probe call rewritten |
| untouched-no-probe | 72 | no call on the patch-changed class inside a catch-and-return try |
| untouched-expected-rejection | 14 | the check uses a targeted catch — an expected-rejection contract, left as-is per the pre-registration |
| untouched-no-try | 8 | the check has no try with a broad catch |
| untouched-already-reports | 1 | a broad catch already throws a violation |
| skipped-probe-not-isolable | 1 | fail-closed: the only patched-class calls sit inside a brace-delimited statement that cannot be separated from setup |
| **total** | **340** | |

Every transformed relation compiled on both builds: there are no
`skipped-does-not-compile` rows.

The single fail-closed skip is
`varbase_20260808_183839/08_patch1-Chart-26-Jaid_c ·
drawLabel-does-not-mutate-axis-observables`, whose only `Axis` calls sit
inside a multi-line `new Object[] { ... }` initialiser. The recorded reason is
in `results.jsonl`.

### Per bug

| bug (label) | patch-changed class | legs | relations | transformed | fires-buggy | fires-patched | fires-both | firings the rewrite created |
|---|---|---|---|---|---|---|---|---|
| Chart-19-o | AbstractObjectList | 7 | 92 | 51 | 1 | 12 | 1 | 11 |
| Chart-26-c | Axis | 3 | 45 | 7 | 2 | 0 | 0 | 0 |
| Lang-41-o | ClassUtils | 1 | 11 | 11 | 1 | 4 | 0 | 0 |
| Lang-63-o | DurationFormatUtils | 7 | 74 | 73 | 15 | 3 | 3 | 0 |
| Math-2-c (SOFix) | HypergeometricDistribution | 3 | 31 | 31 | 11 | 0 | 0 | 0 |
| Math-2-o (Arja) | AbstractIntegerDistribution | 1 | 12 | 10 | 3 | 0 | 0 | 0 |
| Math-65-c | AbstractLeastSquaresOptimizer | 7 | 75 | 61 | 22 | 13 | 11 | 0 |

Reading the two columns that matter for precision: the rewrite does make more
relations fire, but on the correct legs it makes them fire on BOTH builds
(Math-65-c: 22 fire on buggy, 13 on patched, 11 on both; Math-2-c: 11 fire on
buggy, 0 on patched). Firing on both is already a dismissal fact in the
shipped decision code, which is exactly the precision guard the
pre-registration named.

---

## 3. The hard-stop table (G-P)

Every correct leg is Math-65-c ×7, Math-2-SOFix-c ×3, Chart-26-c ×3 — 13 legs,
151 kept relations, 99 of them transformed and executed.

**New tier-2 firings on correct legs: 0. G-P passes.**

Two correct-leg relations fire on the patched build without firing on the
buggy build. Both are on the same leg and both fail the "new" test twice over:
they were already firing in the archived run, and neither message is a tier-2
unexpected-exception message — they are the relation's own value comparison,
which the rewrite cannot manufacture.

| leg | relation | archived replay | tier-2? | firing message (verbatim, first line) |
|---|---|---|---|---|
| varbase_20260808_183839/01_patch1-Math-65-CapGen_c | chiSquare_matches_documented_formula | `**FIRED [trigger]**` | no | `[relfire] relation chiSquare_matches_documented_formula violated: actual=1000.0 expected=1.0E9 __consumed=i:1\|i:-1000000\|i:1 __rcvstate opt:LevenbergMarquardtOptimizer solvedCols=0 diagR=null jacNorm=null beta=null permutation=null rank=0 lmPar=0.0 lmDir=null initialStepBoundFactor=100.0 costRelativeTolerance=1.0E-10 parRelativeTolerance=1.0E-10 orthoTolerance=1.0E-10 qrRankingThreshold=2.22507385` |
| varbase_20260808_183839/01_patch1-Math-65-CapGen_c | chiSquare_is_inverse_in_uniform_weight_scale | `**FIRED [trigger]**` | no | `[relfire] relation chiSquare_is_inverse_in_uniform_weight_scale violated: chi2=500.0 expected=2000.0 s=0.5 __consumed=i:1\|i:-1000000\|i:1\|i:1 __rcvstate opt:LevenbergMarquardtOptimizer solvedCols=0 diagR=null jacNorm=null beta=null permutation=null rank=0 lmPar=0.0 lmDir=null initialStepBoundFactor=100.0 costRelativeTolerance=1.0E-10 parRelativeTolerance=1.0E-10 orthoTolerance=1.0E-10 qrRankingThre` |

That leg's archived outcome is `correctly quiet (no false alarm)`, so these
two firings were already present and were already handled downstream. This is
the Math-65-c leg's known behaviour (`docs/plan.md` 8.29 / 8.36), not
something the rewrite introduced.

Stated plainly, so the gate is not read as stronger than it is: G-P as
registered asks for zero new tier-2 firings that survive the existing
attribution facts. This phase shows zero new tier-2 firings at all, so the
attribution facts never had to be consulted for a correct leg.

---

## 4. Conversions (G-R)

Eleven firings across the corpus are ones the rewrite created — a tier-2
message on a relation that was quiet in its archived replay and is quiet on
the buggy build. All eleven are Chart-19, all eleven carry the same exception,
and all eleven are deterministic on the failing test's own input literals.

| # | leg | relation | fuzzed rate on patched |
|---|---|---|---|
| 1 | diffcov_reach/01 | objectList-indexOf-null-absent-is-minus1 | 20000/20000 |
| 2 | invdiv/04 | objectlist_indexOf_null_absent_is_minus_one | 20000/20000 |
| 3 | invdiv/04 | objectlist-indexof-absent-null-is-minus-one | 20000/20000 |
| 4 | invdiv/04 | objectlist-indexof-does-not-mutate-state | 7772/20000 |
| 5 | **invdiv/05** | objectlist-indexof-null-absent-is-minus-one | 20000/20000 |
| 6 | invdiv/06 | objectlist-indexof-null-absent | 20000/20000 |
| 7 | invdiv/06 | objectlist-null-index-matches-first-null-slot | 7565/20000 |
| 8 | invdiv/06 | objectlist-indexof-is-read-only | 1137/20000 |
| 9 | **varbase/11** | objectlist-indexof-null-absent-returns-minus1 | 20000/20000 |
| 10 | **varbase/11** | empty-list-null-lookup-is-minus-one | 20000/20000 |
| 11 | varbase/12 | objectlist-empty-null-index-minus-one | 20000/20000 |

Every one of them was silent on the buggy build (`0/20000`) and every one is
`trigger` tier, deterministic across 2/2 replays. The message shape is the
same throughout; here is the draw-05 exemplar verbatim, the relation the
FORK-ORACLE read predicted:

```
[relfire] relation objectlist-indexof-null-absent-is-minus-one violated: unexpected java.lang.IllegalArgumentException on valid-by-construction input: Null 'object' argument. __consumed=i:0 __rcvstate list:ObjectList size=0 increment=8
```

Read it against the patch: the receiver is an `ObjectList` of size 0, the
argument is null, no null is stored, so the patch's added line runs and throws
`IllegalArgumentException("Null 'object' argument.")` where the unpatched
build returned -1. That is the exact distinguishing input, now reported
instead of swallowed.

### Which legs change state

A leg counts as CONVERTED only if its archived outcome was `overfit MISSED`
and it now has a rewrite-created firing.

| leg | bug | archived outcome | rewrite-created firings | converted? |
|---|---|---|---|---|
| invdiv_20260808_203424/05_patch1-Chart-19-Arja-plausible_o | Chart-19-o | overfit MISSED | 1 | **YES** |
| varbase_20260808_183839/11_patch1-Chart-19-Arja-plausible_o | Chart-19-o | overfit MISSED | 2 | **YES** |
| diffcov_reach_20260808_233005/01_patch1-Chart-19-Arja-plausible_o | Chart-19-o | OVERFIT CAUGHT | 1 | already caught (upside) |
| invdiv_20260808_203424/04_patch1-Chart-19-Arja-plausible_o | Chart-19-o | OVERFIT CAUGHT | 3 | already caught (upside) |
| invdiv_20260808_203424/06_patch1-Chart-19-Arja-plausible_o | Chart-19-o | OVERFIT CAUGHT | 3 | already caught (upside) |
| varbase_20260808_183839/12_patch1-Chart-19-Arja-plausible_o | Chart-19-o | OVERFIT CAUGHT | 1 | already caught (upside) |

Chart-19 has seven legs in the corpus. Two of them missed. Both convert. The
remaining five were already caught, and four of those gain an independent
second route to the same conviction, which matters for stability: draw 05's
miss was a lottery between draws (8.35), and this removes the lottery for six
of the seven legs.

**Nothing else converts.** The seven archived misses in the corpus are two
Chart-19 legs and five Lang-63 legs (invdiv/08, invdiv/09, diffcov_reach/02,
varbase/14, varbase/15). The five Lang-63 misses are unchanged: 73 of their 74
relations were transformed, they were screened and replayed, and not one
produced a tier-2 firing. That is consistent with the 8.36 measurement — the
patched method is entered roughly two million times per harness and reports
nothing — and it says the Lang-63 miss is a different oracle-side gap
(which observable is checked), not exception swallowing. Mechanism A does not
touch it.

Lang-41-o is worth one line because its four patched-only firings look like a
conversion and are not: all four were already firing in the archived run, and
none carries a tier-2 message. That leg was already a catch.

---

## 5. Extraction accounting, per leg

| leg | kept-screen decisions | replay steps | agree? | bodies recovered | bodies missing | synthesis rounds | compile repairs |
|---|---|---|---|---|---|---|---|
| invdiv_20260808_203424/01_patch1-Math-65-CapGen_c | 10 | 10 | yes | 10 | 0 | 2 | 0 |
| invdiv_20260808_203424/02_patch1-Math-65-CapGen_c | 10 | 10 | yes | 10 | 0 | 2 | 0 |
| invdiv_20260808_203424/03_patch1-Math-65-CapGen_c | 10 | 10 | yes | 10 | 0 | 2 | 0 |
| invdiv_20260808_203424/04_patch1-Chart-19-Arja-plausible_o | 12 | 12 | yes | 12 | 0 | 2 | 0 |
| invdiv_20260808_203424/05_patch1-Chart-19-Arja-plausible_o | 16 | 16 | yes | 16 | 0 | 3 | 3 |
| invdiv_20260808_203424/06_patch1-Chart-19-Arja-plausible_o | 12 | 12 | yes | 12 | 0 | 2 | 0 |
| invdiv_20260808_203424/07_patch1-Lang-63-Arja-plausible_o | 10 | 10 | yes | 10 | 0 | 2 | 0 |
| invdiv_20260808_203424/08_patch1-Lang-63-Arja-plausible_o | 11 | 11 | yes | 11 | 0 | 2 | 0 |
| invdiv_20260808_203424/09_patch1-Lang-63-Arja-plausible_o | 11 | 11 | yes | 11 | 0 | 2 | 0 |
| varbase_20260808_183839/01_patch1-Math-65-CapGen_c | 11 | 11 | yes | 11 | 0 | 2 | 0 |
| varbase_20260808_183839/02_patch1-Math-65-CapGen_c | 10 | 10 | yes | 10 | 0 | 2 | 0 |
| varbase_20260808_183839/03_patch1-Math-65-CapGen_c | 12 | 12 | yes | 12 | 0 | 2 | 0 |
| varbase_20260808_183839/04_patch1-Math-2-SOFix_c | 9 | 9 | yes | 9 | 0 | 2 | 0 |
| varbase_20260808_183839/05_patch1-Math-2-SOFix_c | 11 | 11 | yes | 11 | 0 | 2 | 0 |
| varbase_20260808_183839/06_patch1-Math-2-SOFix_c | 11 | 11 | yes | 11 | 0 | 2 | 0 |
| varbase_20260808_183839/07_patch1-Chart-26-Jaid_c | 12 | 12 | yes | 12 | 0 | 2 | 0 |
| varbase_20260808_183839/08_patch1-Chart-26-Jaid_c | 17 | 17 | yes | 17 | 0 | 3 | 1 |
| varbase_20260808_183839/09_patch1-Chart-26-Jaid_c | 16 | 16 | yes | 16 | 0 | 3 | 0 |
| varbase_20260808_183839/10_patch1-Chart-19-Arja-plausible_o | 11 | 11 | yes | 11 | 0 | 2 | 2 |
| varbase_20260808_183839/11_patch1-Chart-19-Arja-plausible_o | 18 | 18 | yes | 18 | 0 | 3 | 3 |
| varbase_20260808_183839/12_patch1-Chart-19-Arja-plausible_o | 11 | 11 | yes | 11 | 0 | 2 | 0 |
| varbase_20260808_183839/13_patch1-Lang-63-Arja-plausible_o | 11 | 11 | yes | 11 | 0 | 2 | 6 |
| varbase_20260808_183839/14_patch1-Lang-63-Arja-plausible_o | 9 | 9 | yes | 9 | 0 | 2 | 0 |
| varbase_20260808_183839/15_patch1-Lang-63-Arja-plausible_o | 11 | 11 | yes | 11 | 0 | 2 | 0 |
| diffcov_reach_20260808_233005/01_patch1-Chart-19-Arja-plausible_o | 12 | 12 | yes | 12 | 0 | 2 | 2 |
| diffcov_reach_20260808_233005/02_patch1-Lang-63-Arja-plausible_o | 11 | 11 | yes | 11 | 0 | 2 | 0 |
| diffcov_reach_20260808_233005/03_patch1-Lang-41-Arja-plausible_o | 11 | 11 | yes | 11 | 0 | 2 | 0 |
| diffcov_reach_20260808_233005/04_patch1-Math-2-Arja-plausible_o | 12 | 12 | yes | 12 | 0 | 2 | 0 |
| diffcov_reach_20260808_233005/05_patch1-Math-65-CapGen_c | 12 | 12 | yes | 12 | 0 | 2 | 0 |
| **total (29 legs)** | **340** | **340** | all agree | **340** | **0** | | |

No leg was skipped. Every compile-repair output was matched to its relation
(0 unmatched across all 29 legs).

---

## 6. Phase-2 input

`runs-archive/runs/rex_replay_20260809_074539/phase2_cases.jsonl` — 11 cases,
one per rewrite-created firing, in the format `src/java/verifier_replay.py`
documents (`id`, `harness_source` = the rewritten check, `fired_assertion`,
`concrete_evidence` including the buggy-side screen counts and a note saying
the only edit was the two-tier catch, `failing_test` = the leg's real failing
test block rebuilt in the shape `run.py::_j3_failing_test_block` renders,
`label`, `note`). All 11 are labelled `overfitting`, because G-P produced no
correct-leg case to ask about.

Run it with:

```
uv run python java/verifier_replay.py \
    --cases <results dir>/phase2_cases.jsonl \
    --out /home/code/scratch/runs/rex_verify_<stamp> --repeats 3
```

That answers the question this phase cannot: would the verifier KEEP these
findings, i.e. would the verdict flip, not merely "would something fire".

---

## 7. What this study does not show

- **Mechanism A only.** Replaying archived relations cannot test Mechanism B
  (rejection-probe-after-mutation), because that changes what gets invented.
  The pre-registration says so up front; it stands.
- **Relations that were never invented.** This measures what the existing
  relation bodies would have reported. A draw that never wrote a null-absent
  relation gains nothing here, and nothing in this study speaks to how often
  such a relation gets invented.
- **Verdicts.** Eleven firings is not eleven catches. Every firing still has
  to pass the verifier and the attribution facts. Phase 2 is the measurement;
  this is only the input to it.
- **One patch shape.** All eleven new firings come from a single overfit
  pattern: a patch that ADDS a throw. How much of the overfit pool has that
  shape is unmeasured here and bounds how much Mechanism A buys, as the
  draw-05 read already flagged.
- **Fuzz variance.** These are single measurements at the 20,000-run budget.
  All eleven fire on the trigger literals deterministically (2/2 replays),
  which is the strongest tier available, but the fuzzed rates in §4 are one
  sample each.
- **A judgement call, stated so it can be argued with.** The transform treats
  every non-constructor call on the patch-changed class as tier 2, which is
  the pre-registration's own wording. That includes state-mutating calls such
  as `list.set(...)`, which one could argue is setup. Excluding them would not
  change any result here — all eleven new firings come from `indexOf` calls —
  but on some other patch it would matter, and it is a choice, not a
  derivation.
