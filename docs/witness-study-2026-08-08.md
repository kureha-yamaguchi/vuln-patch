# Witness Study — input kinds that distinguish correct from overfit patches (2026-08-08)

**Status:** complete. Plan items 8.33 (design) and 8.34 (synthesis). HEAD at write time: `ef7f1cf`.
**Purpose:** decide, from ground truth rather than intuition, what kinds of INPUT a general (bug-agnostic) fuzz seeder must produce to expose overfit patches — and which kinds are out of reach.
**Method:** 6 Opus agents classified the DiffTGen witness tests shipped in the `drr` dataset (`/home/code/drr/DiffTGen/result/<Bug>/…/testcase/*Test.java` on the VM). 137 witness rows across 28 bugs (3 assigned bugs had no witness).

---

## 0. Why this study exists (the chain that led here)

- The pipeline detects overfit repair patches by fuzzing the patched build and checking synthesized relations. It MISSES some overfit patches.
- The variance baseline (plan 8.29) showed misses on unstable legs are a lottery at **oracle invention** (the first model call): which checks get invented varies per draw.
- The invention-diversity run (8.30, `-n 8`) raised invention but did NOT convert a miss: Chart-19 draw 05 invented 6 range-axis checks and still crashed 0 patched harnesses.
- The draw-05 desk read (8.32) concluded the bottleneck moved **downstream of invention** — into patched-side INPUT REACH. (Caveat: 8.32's positive conclusion is FLAGGED for re-read, see §6.)
- User direction: the dataset already knows which inputs distinguish correct vs overfit (that's how labels were assigned). Read those witnesses and understand the input kinds — WITHOUT letting witness content steer any run (the firewall, §5).

---

## 1. Classification schema (shared across all 6 agents — labels are exact so results merge)

**PRIMARY input kind** (one per witness, optional secondary):
- `constructor-scale` — a non-default numeric constructor/size argument reaching a capacity/size-dependent branch
- `null-arg` — null passed where behavior must reject/handle it
- `index-boundary` — an index at 0 / -1 / size / size±1
- `numeric-boundary` — a numeric value at 0 / ±1 / MIN / MAX / NaN / infinity / empty
- `operation-sequence` — requires a specific ORDER of mutating calls before the probe
- `collection-shape` — specific collection contents / emptiness / duplicates
- `string-shape` — specific string content / format / empty / unicode
- `type-specific` — a specific subclass / concrete type instance is required
- `other` — described inline

**GENERAL generator that would cover it** (exact labels):
- `lattice` — a universal boundary lattice (0, ±1, size, size±1, null, empty, MIN, MAX, NaN, out-of-range enum int) would hit it
- `diff-derived` — reading the patch's CHANGED LINES (their constants + the changed method's parameter types) would target it
- `within-run-sequence` — cross-harness seeding or operation-sequence exploration within ONE run finds it
- `none` — no general, bug-agnostic generator plausibly produces it (the gaps)

---

## 2. Raw classification — all six partitions

### Partition A — Chart13, Chart15, Chart19, Chart25, Chart26 (16 rows)

| bug | patch | witness | primary | secondary | covering | evidence |
|---|---|---|---|---|---|---|
| Chart13 | patch1_arja | DiffTGen0Test | operation-sequence | type-specific (RectangleEdge) | within-run-sequence | `add(bc, RectangleEdge.TOP); arrangeFN(bc,null,0.0)` |
| Chart13 | patch1_elixir | DiffTGen0Test | operation-sequence | type-specific (EmptyBlock+LEFT) | within-run-sequence | `add(new EmptyBlock(2650.6,2.0), RectangleEdge.LEFT); arrangeFN(...)` |
| Chart13 | patch2_arja | DiffTGen0Test | operation-sequence | type-specific (ColorBlock+RIGHT) | within-run-sequence | `add(new ColorBlock(...), RectangleEdge.RIGHT); arrangeFN(...)` |
| Chart13 | patch3_arja | DiffTGen0Test | operation-sequence | type-specific | within-run-sequence | `add(bc, RectangleEdge.TOP); arrangeFN(bc,null,0.0)` |
| Chart13 | patch4_arja | DiffTGen0Test | operation-sequence | type-specific | within-run-sequence | `add(bc, RectangleEdge.BOTTOM); arrangeFN(...)` |
| Chart13 | patch5_arja | DiffTGen0Test | operation-sequence | type-specific | within-run-sequence | `add(bc, RectangleEdge.BOTTOM); arrangeFN(...)` |
| Chart13 | patch6_arja | DiffTGen0Test | operation-sequence | type-specific | within-run-sequence | `add(bc, RectangleEdge.BOTTOM); arrangeFN(...)` |
| Chart15 | patch1_jgenprog2015 | DiffTGen0Test | type-specific | collection-shape (empty) | **none** | `new MultiplePiePlot().getPieChart().createBufferedImage(1885,2336)` (full render) |
| Chart19 | patch1_arja | DiffTGen0Test | null-arg | numeric-boundary (empty list) | lattice | `new AbstractObjectList(668); indexOf((Object) null)` — correct throws IAE, overfit returns -1. **Capacity 668 is INCIDENTAL (empty list); the NULL ARG is the distinguisher.** |
| Chart25 | patch2_arja | DiffTGen0Test | null-arg | operation-sequence | lattice | `add(3,3,null,null); getColumnCount(); add(null,null,null,null)` |
| Chart25 | patch3_arja | DiffTGen0Test | numeric-boundary | type-specific | lattice | `add(0,(byte)12,minute,ohlc); getValue(0,0)` (mean = 0) |
| Chart25 | patch4_arja | DiffTGen0Test | null-arg | numeric-boundary | lattice | `add((byte)20,6,null,month); getValue(0,0)` (null row key) |
| Chart25 | patch5_arja | DiffTGen0Test | string-shape | numeric-boundary | lattice | `add(8,1023,"",""); getMeanValue(0,0)` (empty-string keys) |
| Chart25 | patch1_deeprepair | DiffTGen2Test | type-specific | collection-shape (empty) | **none** | `new JFreeChart("QD hzu", new CategoryPlot()).createBufferedImage(917,939)` → `calculateRangeAxisSpace`=0.0 |
| Chart26 | patch1_jaid | DiffTGen0Test | type-specific | collection-shape (empty FastScatterPlot) | **none** | `new JFreeChart("SansSerif", new FastScatterPlot()).createBufferedImage(10,10,info)` → `Axis.drawLabel` |
| Chart26 | patch2_jaid | DiffTGen0Test | type-specific | string-shape | **none** | `new JFreeChart("6\`{lV-U\"", new FastScatterPlot()).createBufferedImage(3018,3018,info)` → `Axis.drawLabel` |

### Partition B — Chart3, Chart5, Chart7, Chart9 (Lang20 = no witness) (6 rows)

| bug | patch | witness | primary | secondary | covering | evidence |
|---|---|---|---|---|---|---|
| Chart3 | patch1 (Elixir) | DiffTGen0Test (test97) | operation-sequence | index-boundary | within-run-sequence | `new TimeSeries(week0)`, `add(week0,-10.6)`, then `createCopy(week0,week0)` — copy of a size-1 range after an add |
| Chart5 | patch1 (DeepRepair) | DiffTGen0Test (test03) | operation-sequence | collection-shape | within-run-sequence | `XYSeries(...,true,true)`, 3× `add(40,...)` (duplicate x) + `add(21,21)`, then `addOrUpdate(-1799.38,0.0)` |
| Chart7 | patch1 (DeepRepair) | DiffTGen2Test (test05) | operation-sequence | index-boundary | within-run-sequence | `new TimePeriodValues(...,"","")`, `add(day0, 1668051567)` (index 0), then `getMaxMiddleIndex()` |
| Chart9 | patch1 (Jaid) | DiffTGen0Test (test059) | index-boundary | operation-sequence | lattice | series holds only `hour1=hour0.previous()`; `createCopy(hour0,hour0)` requests a period one past last → getIndex→size |
| Chart9 | patch2 (Jaid) | DiffTGen0Test (test059) | index-boundary | operation-sequence | lattice | byte-identical witness to patch1 |
| Chart9 | patch3 (Jaid) | DiffTGen2Test (test42) | collection-shape | index-boundary | lattice | **empty** series; `createCopy(millisecond0, second0)` over empty data |

### Partition C — Lang22, Lang27, Lang39, Lang45, Lang50 (9 rows)

| bug | patch | witness | primary | secondary | covering | evidence |
|---|---|---|---|---|---|---|
| Lang22 | patch1-Arja | DiffTGen1Test | numeric-boundary | — | lattice | `getReducedFraction(Integer.MIN_VALUE, 114)` then hashCode; gcd overflow at MIN_VALUE, expects gcd=2 |
| Lang27 | patch1-DeepRepair | DiffTGen1Test | string-shape | — | **none** | `createNumber(")q8Bmo[N5EFUnKebN")` must throw NFE |
| Lang27 | patch-ssFix | DiffTGen2Test | string-shape | — | **none** | `createNumber("Minimummabbreviation [idt3 Es 4")` must throw NFE |
| Lang39 | patch1-Elixir | DiffTGen1Test | collection-shape | null-arg | **none** | `replaceEachRepeatedly(text, arr, arr)` — self-referential cyclic search/replace; expects IllegalStateException |
| Lang39 | patch1-Arja | DiffTGen1Test | null-arg | collection-shape | lattice | `replaceEach("JAVA_1_1", arr, arr)` where `arr[1]` unset (null element); input returned unchanged |
| Lang45 | patch1-Jaid | DiffTGen1Test | index-boundary | numeric-boundary | lattice | `abbreviate(" ", 2896, -1, "user.name")` — lower≫len, upper=-1; expects "" |
| Lang45 | patch2-Jaid | DiffTGen1Test | index-boundary | numeric-boundary | lattice | `abbreviate("Fz3{/aH6D;@16xXW", 503, -1, ...)` — upper=-1 sentinel |
| Lang50 | patch1-Arja | DiffTGen0Test | numeric-boundary | type-specific | lattice | `getDateInstance(0)` minimal style, default TZ + Locale.CANADA; length field expected 31 |
| Lang50 | patch3-Arja | DiffTGen0Test | numeric-boundary | other (invalid enum int) | lattice | `getDateInstance(212)` out-of-range → IllegalArgumentException "Illegal date style 212" |

### Partition D — Lang51, Lang55, Math2, Math32 (Math50 = no witness) (24 rows)

| bug | patch | witness | primary | secondary | covering | evidence |
|---|---|---|---|---|---|---|
| Lang51 | patch1-Jaid | DiffTGen0Test | string-shape | — | diff-derived | `toBoolean("T!t")` — near-miss of a bool keyword; true fix → false |
| Lang51 | patch1-Elixir | DiffTGen0Test | string-shape | — | diff-derived | `toBoolean("~es")` — "es" suffix of "yes", char-0 perturbed |
| Lang51 | patch1-Nopol2015 | DiffTGen0Test | string-shape | — | diff-derived | `toBoolean("Ies")` — single-char sub of "yes" → false |
| Lang51 | patch2-Jaid | DiffTGen0Test | string-shape | — | diff-derived | `toBoolean("Y\`'")` — starts like "yes", tail invalid |
| Lang51 | patch2-Arja | DiffTGen0Test | string-shape | — | diff-derived | `toBoolean("T!t")` |
| Lang51 | patch3-Jaid | DiffTGen0Test | string-shape | — | diff-derived | `toBoolean("Y\`'")` |
| Lang55 | patch1-Arja | DiffTGen0Test | operation-sequence | — | within-run-sequence | `start();suspend();stop()` → stopTime field must be 2 |
| Lang55 | patch2-Arja | DiffTGen0Test | operation-sequence | — | within-run-sequence | `start();suspend();stop()` — suspend-before-stop |
| Lang55 | patch3-Arja | DiffTGen0Test | operation-sequence | — | within-run-sequence | `start();suspend();stop()` |
| Lang55 | patch4-Arja | DiffTGen0Test | operation-sequence | — | within-run-sequence | `start();stop();getSplitTime()` — ordering probe |
| Lang55 | patch5-Arja | DiffTGen0Test | operation-sequence | — | within-run-sequence | `start();suspend();stop()` |
| Math2 | patch5-Arja | DiffTGen0Test | numeric-boundary | constructor-scale | lattice | `Poisson(0.16,-1424.1,1).inverseCumulativeProbability(0.16)` → 0 |
| Math2 | patch6-Arja | DiffTGen0Test | numeric-boundary | constructor-scale | lattice | `Poisson(0.075,0.075,-456).inverseCumulativeProbability(0.075)` → 0 |
| Math2 | patch1-SOFix | DiffTGen0Test | numeric-boundary | null-arg | lattice | `Geometric(null, 2.22e-16).inverseCumulativeProbability(0.9026)` → Integer.MAX_VALUE (overflow) |
| Math2 | patch1-Elixir | DiffTGen1Test | numeric-boundary | constructor-scale | within-run-sequence | `Binomial(2,0.838).sample(804)` → inv-CDF = n = 2 |
| Math2 | patch1-Elixir | DiffTGen2Test | numeric-boundary | constructor-scale | within-run-sequence | `Geometric(rng,1e-9).sample(1456)` → MAX_VALUE |
| Math2 | patch2-SOFix | DiffTGen0Test | numeric-boundary | constructor-scale | within-run-sequence | `Pascal(2338, 8.9e-9).sample(2338)` → MAX_VALUE |
| Math2 | patch3-Arja | DiffTGen2Test | constructor-scale | numeric-boundary | within-run-sequence | `Zipf(2,2).sample(328)` → inv-CDF = 1 |
| Math2 | patch4-Arja | DiffTGen2Test | constructor-scale | numeric-boundary | within-run-sequence | `Zipf(2,2).sample(328)` → 1 |
| Math2 | patch1-Arja | DiffTGen0Test | constructor-scale | numeric-boundary | within-run-sequence | `Zipf(rng,2,2).sample(166)` → 1 |
| Math2 | patch7-Arja | DiffTGen2Test | constructor-scale | numeric-boundary | within-run-sequence | `Binomial(rng,1753,0.606).sample(1753)` → inv-CDF #144 = 1032 (large n) |
| Math2 | patch8-Arja | DiffTGen2Test | constructor-scale | numeric-boundary | within-run-sequence | `Binomial(rng,1753,0.606).sample(1753)` → 1032 |
| Math2 | patch1-JGenProg2015 | DiffTGen2Test | constructor-scale | numeric-boundary | within-run-sequence | `Pascal(538,0.134).sample(1350)` → inv-CDF #242 = 3636 (large r) |
| Math32 | patch1-Jaid | DiffTGen0Test | numeric-boundary | other (degenerate geometry) | within-run-sequence | `new PolygonsSet().getBarycenter()` → +Infinity (default = unbounded whole-plane) |

### Partition E — Math53, Math59, Math63, Math8 (Math6 = no witness) (18 rows)

| bug | patch | witness | primary | secondary | covering | evidence |
|---|---|---|---|---|---|---|
| Math53 | patch1-DeepRepair | DiffTGen0Test | null-arg | numeric-boundary | lattice | `complex1.add((Complex) null)` must raise NullArgumentException |
| Math59 | patch1-SequenceR | DiffTGen1Test | numeric-boundary | type-specific | lattice | `FastMath.max(0.0F,(float)(-1662L))` must return 0.0 (float overload) |
| Math63 | patch1-CapGen | DiffTGen1Test | numeric-boundary | collection-shape | lattice | `arr[2]=Float.NaN; MathUtils.equals(arr,arr)` → equals(NaN,NaN) must be false |
| Math63 | patch1-Elixir | DiffTGen1Test | numeric-boundary | — | lattice | `MathUtils.equals((double)0, 1171.15)` — NaN path, must be false |
| Math63 | patch2-CapGen | DiffTGen1Test | numeric-boundary | collection-shape | lattice | `arr[0..2]=Float.NaN; equals(arr,arr)` false |
| Math63 | patch3-CapGen | DiffTGen0Test | numeric-boundary | collection-shape | lattice | `arr[2]=Double.NaN; equals(arr,arr)` false |
| Math63 | patch4-CapGen | DiffTGen1Test | numeric-boundary | — | lattice | `equals(Double.NaN, Double.NaN)` must be false |
| Math63 | patch5-CapGen | DiffTGen1Test | numeric-boundary | collection-shape | lattice | `arr[1,3,7]=Float.NaN; equals(arr,arr)` false |
| Math63 | patch6-CapGen | DiffTGen1Test | numeric-boundary | collection-shape | lattice | `arr[1]=Float.NaN; equals(arr,arr)` false |
| Math63 | patch7-CapGen | DiffTGen1Test | numeric-boundary | collection-shape | lattice | `arr[3]=Double.NaN; equals(arr,arr)` false |
| Math63 | patch8-CapGen | DiffTGen1Test | numeric-boundary | collection-shape | lattice | `arr[2]=Double.NaN; equals(arr,arr)` false |
| Math8 | patch1-Arja | DiffTGen0Test | type-specific | collection-shape | **none** | `Long[] x = new DiscreteDistribution<Long>(...).sample(1)` — result must be Long[], not Object[] |
| Math8 | patch1-SimFix | DiffTGen0Test | type-specific | collection-shape | **none** | `Double[] x = DiscreteDistribution<Double>.sample(3453)` |
| Math8 | patch2-Arja | DiffTGen0Test | type-specific | collection-shape | **none** | `String[] x = DiscreteDistribution<String>.sample(1210)` |
| Math8 | patch3-Arja | DiffTGen0Test | type-specific | collection-shape | **none** | `String[] x = DiscreteDistribution<String>.sample(1)` |
| Math8 | patch4-Arja | DiffTGen0Test | type-specific | collection-shape | **none** | `sample(1)[0]` must be Integer (concrete runtime component type) |
| Math8 | patch5-Arja | DiffTGen0Test | type-specific | collection-shape | **none** | identical to patch4 |
| Math8 | patch6-Arja | DiffTGen0Test | type-specific | collection-shape | **none** | `Long[] x = DiscreteDistribution<Long>.sample(290)` |

### Partition F — Math80, Math81, Math82, Math85, Math97, Time4 (64 rows)

**Math80** — `EigenDecompositionImpl(mainDiag[], secondaryDiag[], shift)` → `flipIfWarranted`. 29 patches, ALL `collection-shape` / `none`; the distinguisher is always the specific `double[]` matrix contents (secondary tags vary: operation-sequence for re-decompose-to-asymmetric, index-boundary for out-of-range `getEigenvector(N)`, numeric-boundary for the shift value). One bug, one input SHAPE, 29 rows.

**Math81** — same class → `computeShiftIncrement`. 3 patches, all `collection-shape` / `none` (specific diag arrays; one `getEigenvector(-4043)` index-boundary secondary).

**Math82** — `SimplexSolver.doIteration` → `getPivotRow`. 1 patch (SimFix): `collection-shape` / **lattice** — **empty** LinkedList constraints + negative objective coeffs → unbounded, getPivotRow returns null.

**Math85** — `UnivariateRealSolverUtils.bracket(fn, initial, lower, upper[, maxIter])`. 21 patches, all `numeric-boundary`. Split: `lattice` (9) when args are clean boundary values (init==lower or init==upper zero-width interval, default/≈MAX iteration cap); **none** (12) when a finite `maxIter` is tuned to arbitrary mid-range bounds so the loop halts exactly at the changed convergence guard.

**Math97** — `BrentSolver.solve(min, max)` on near-flat polynomial. 2 patches (ACS), `numeric-boundary` / **none** — needs poly coeff ≈ `-1e-30` / `1e-15` (near-flat) so the solver's changed guard triggers.

**Time4** — `ZeroIsMaxDateTimeField.getMinimumValue/getMaximumValue`. 8 patches, all `type-specific` / **none** — every witness assembles a specific joda stack (Chronology → LenientChronology → LenientDateTimeField → `ZeroIsMaxDateTimeField(field, DateTimeFieldType)`); one has a `null` arg secondary (NPE in GJChronology), one `YearMonth(-1L, Buddhist)` numeric secondary. The distinguisher is the type/domain-object configuration, not a value.

---

## 3. Aggregate — READ PER BUG, NOT PER PATCH

A raw per-patch tally (137 rows, 54 `none`) is dominated by whichever bugs attracted many APR attempts (Math80 alone = 29 patches of ONE bug/one input shape). The design question is "how many KINDS of distinguishing input exist," so **distinct bugs are the unit**. 28 bugs have witnesses (Lang20, Math50, Math6 have none).

**Per-bug coverage by the general generators:**

| coverage class | count | bugs |
|---|---|---|
| `lattice` (universal boundary atoms) | 10 | Chart9, Chart19, Chart25, Lang22, Lang45, Lang50, Math53, Math59, Math63, Math82 |
| `within-run-sequence` (build-state / call-ordering then probe) | 6 | Chart3, Chart5, Chart7, Chart13, Lang55, Math32 |
| both lattice + sequence | 1 | Math2 (scalar extremes = lattice; large-n sample()-driven = sequence) |
| `diff-derived` (seed from patch's changed constants) | 1 | Lang51 (near-keyword strings) |
| PARTIAL (some patches covered, some gap) | 2 | Lang39 (null elem = lattice; cyclic = gap); Math85 (degenerate interval = lattice; tuned maxIter = gap) |
| STRUCTURAL GAP | 8 | Chart15, Chart26, Lang27, Math8, Math80, Math81, Math97, Time4 |

**~18 of 28 (64%) cleanly covered; 2 partial; 8 (29%) genuine gaps.**

---

## 4. The 8 gaps collapse to FOUR families (the design menu for later)

1. **CHART-RENDER** — construct a concrete `Plot` subtype and call `createBufferedImage`; divergence lives deep in the draw pipeline (`Axis.drawLabel`, `calculateRangeAxisSpace`). *(Chart15, Chart26)*
2. **TYPE-GRAPH CONSTRUCTION** — compose a specific subclass/domain-object stack: joda `Chronology→…→ZeroIsMaxDateTimeField` *(Time4)*, or generic `Distribution<T>` + typed-array capture forcing a ClassCastException *(Math8)*.
3. **SPECIFIC NUMERIC CONTENT** — a particular matrix spectrum / near-flat polynomial / interval that drives one algorithm branch; a boundary value can't synthesize it. *(Math80, Math81, Math97, Math85-partial)* — may be **fundamentally beyond general generation**; candidate permanent tail.
4. **STRUCTURED STRING/COLLECTION CONTENT** — cyclic self-referential arrays, invalid-number strings, near-keyword strings. *(Lang27, Lang39-Elixir, Lang51)*

---

## 5. FIREWALL (non-negotiable — the study is legitimate ONLY under this)

- Witness content is GROUND-TRUTH-DERIVED. It must NEVER enter a prompt, a fuzz run, a corpus, or any per-leg artifact. The pipeline stays label-blind.
- Only AGGREGATE, bug-agnostic conclusions (e.g. "vary constructor ints over a boundary lattice", "seed near-keyword mutations from the patch's own string constants") inform the general generators.
- Any generator built from this must derive its inputs from ONLY: (a) the universal boundary lattice, (b) the per-leg PATCH TEXT the pipeline already reads, (c) within-run cross-harness sharing. Never from a witness.
- Every generator validates BOTH SIGNS against the frozen guard fixtures (67-row genuine-catch, 38-row correct-dismissals) and the clean-leg hard-stop, like every mechanism before it.
- Using ground truth to UNDERSTAND/EVALUATE = same legitimacy as scoring runs against labels. Using it to STEER a run = benchmark farming. The line sits exactly there.

---

## 6. Corrections and open flags carried by this study

- **Chart-19 correction (verified against witness + patch source).** Earlier plan text called Chart-19 a `constructor-scale` exemplar. WRONG. The patch adds `if (object == null) throw new IllegalArgumentException(...)` in `indexOf`; the witness distinguisher is `indexOf(null)` — a **null argument** (lattice's first atom). `new AbstractObjectList(668)` capacity is incidental (empty list).
- **8.32 draw-05 read is FLAGGED, not resolved.** Because Chart-19's distinguisher is a null arg and draw 05 DID invent null probes that went QUIET on the patched build, the miss points at **probe ROUTING** (did the invented null probe reach `AbstractObjectList.indexOf`, or a different method?), not input-value reach. 8.32's eliminations (not-invention, not-selection) still stand; its POSITIVE conclusion (input reach) needs a re-read before any seeder design rests on it.
- **`constructor-scale` is real** — just not in Chart-19. It appears genuinely in Math2's large-n sample()-driven overflow cases.

---

## 7. Design conclusion + build spec for the next agent

**Build ONE general seeder** (its own mechanism, its own pre-registered gates; firewall §5 applies):

1. **Universal boundary lattice** — for every fuzz-drawn scalar/array/string arg, bias the draw toward: `0, ±1, MIN, MAX, NaN, ±Infinity, empty, size, size±1, null`, and for enum-int args, an out-of-range value. Covers 10 bugs outright.
2. **Diff-derived constant seeding** — regenerated FRESH each run from that run's own patch text: numeric/string literals on changed lines become seeds; the changed method's parameter types drive type-directed values; string constants get single-char near-miss mutations (covers Lang51). Never persisted across runs.
3. **Within-run operation-sequence exploration** — seed short mutating call sequences before the probe, and vary call ORDER and enum arguments (covers Chart3/5/7/13, Lang55, Math32). Cross-harness: inputs one harness finds interesting seed the same run's other harnesses on the same leg (permitted within-run sharing).

Expected coverage: ~64% of distinct bugs, zero bug-specific knowledge.

**BEFORE believing it helps: build the diff-hit instrumentation FIRST** (parked, plan). At patched-build time, inject a hit counter into each patch-changed method (mechanical, our code) and print `[diffcov] method=… hits=N` at shutdown. This measures whether generated inputs actually REACH the changed code — the load-bearing caveat below. Measure reach, THEN build/tune the seeder.

**Defer the four gap-family generators** (§4) — each is a much larger bespoke build for lower marginal coverage; family 3 may be permanent tail. Named, not pursued.

---

## 8. Four honest caveats (bound the claim)

- **(a) Per-patch skew** — flagged and avoided; per-bug is the reported stat.
- **(b) "Coverable" ≠ "reached."** This study proves the lacking inputs CAN be generated cheaply. It does NOT prove our fuzzer's distribution reaches them or that seeding fixes recall. The diff-hit instrumentation (§7) is what tests the outcome. This study sized the DESIGN, not the result.
- **(c) Witness ≠ our path.** Witnesses expose divergence in a TEST harness; our pipeline uses fuzz + relations. The mapping is not 1:1.
- **(d) Our own missed legs.** Lang-63 and Lang-41 (the legs we actually miss) have NO witness in this set — the study can't speak to them directly. But the studied unstable legs it CAN (Chart-19 = null/lattice, Chart-7 = sequence/within-run) fall in the covered set, so a lattice+sequence seeder is on-target for the miss class IF reach — not routing (§6) — is the bottleneck.

---

## 9. Pointers

- Full plan record: `docs/plan.md` items **8.29** (variance baseline), **8.30** (invention diversity), **8.32** (draw-05, flagged), **8.33** (study design + firewall), **8.34** (this synthesis).
- Witnesses on VM: `/home/code/drr/DiffTGen/result/<Bug>/<patch-subdir>/…/testcase/*Test.java`; label CSV `/home/code/drr/DiffTGen/Result.csv`.
- Patches: `/home/code/drr/Patches/{Dcorrect,Doverfitting}/<Tool>/<Proj>/`.
- Suites that reference these legs: `suites/cases/ladder_stage4.cases`, `variance_baseline.cases`, `invention_diversity.cases`.
- Guard fixtures: `tests/fixtures/correct_dismissals.jsonl` (38), `docs/replay/backtrack/guard_population.json` (67).
