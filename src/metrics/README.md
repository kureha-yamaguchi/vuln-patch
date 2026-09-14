# `src/metrics` — root-cause region metrics

Five metrics, measured on one harness set per bug:

| Metric | Formula | What it shows |
|---|---|---|
| Root-cause coverage, RCC(H) | $\lvert \hat{\mathbb{R}} \cap \mathbb{F}(H) \rvert \,/\, \lvert \hat{\mathbb{R}} \rvert$ | How much of the root-cause region is covered. |
| Root-cause recovery, RCR | $\lvert \hat{\mathbb{R}} \cap \mathbb{P} \rvert \,/\, \lvert \hat{\mathbb{R}} \rvert$ | How well $\mathbb{P}$ covers $\hat{\mathbb{R}}$. |
| Root-cause precision, RCP(H) | $\lvert \hat{\mathbb{R}} \cap \mathbb{F}(H) \rvert \,/\, \lvert \mathbb{F}(H) \rvert$ | How much of the fuzzing budget lands in $\hat{\mathbb{R}}$. |
| Patch-derived set coverage, PSC(H) | $\lvert \mathbb{P} \cap \mathbb{F}(H) \rvert \,/\, \lvert \mathbb{P} \rvert$ | How much of the budget lands in $\mathbb{P}$. |
| Crash-site match, CSM(H) | $\lvert \lbrace\, c \in C : \mathrm{site}(c) \in \hat{\mathbb{R}} \,\rbrace \rvert \,/\, \lvert C \rvert$ | Are the crashes the right crashes? |

All four set metrics are function-level. Every set is a set of
`metrics.keys.MethodKey`, so a metric is a set intersection and a division.

- $\hat{\mathbb{R}}$: extracted statically from the developer-written fix — the changed methods.
- $\mathbb{P}$: extracted statically from the APR patch under analysis — its
  changed methods, plus the project functions statically reachable from them
  under the pipeline's own call-graph budget.
- $\mathbb{F}(H)$: measured dynamically by running the harnesses. (The accepted
  harnesses are executed on the buggy build, and runtime coverage is collected
  using JaCoCo. The `.exec` coverage dumps are merged across all harnesses, then
  converted to `jacoco.xml`. Any method observed executing becomes part of
  $\mathbb{F}(H)$.)
- $C$: the Jazzer findings of that same measurement run, one per
  `== Java Exception` banner, with the method each crash happened in.
- $H_R$ is the root-cause conditioned harness set. Only crashing bugs, the
  heldout set, and overfitting patches are analysed so far.

TODOs (not necessarily in order)

1. extend to semantic bugs and correct patches as well
2. report an F1 score alongside the five metrics. **Would ideally like a single bash script that runs the pipeline on the whole heldout set for $n$ repetitions, and then report the average and confidence intervals for the 5 set-theory metrics + F1 score**
3. explore edge-level representations of $\mathbb{P}$, $\mathbb{R}$, and $\mathbb{F}(H)$
4. implement the naïve baseline (without patch context) to evaluate $RCC(H_N)$
5. I think it would be interesting to swap out patch diff for Poc, but feeding in \{PoC\} + \{triggering tests\} + \{extracted PoC-derived set\} to the system. Because then, the quality of $\mathbb{R} \cap \mathbb{P}$ is not depended on whether the patch-derived set is from an overfitting or correct patch. I know we propagrate up and down the patch touched functions to draw a neighbourhood region, so the patch-derived set should still be informative even if extracting from an overfitting patch- BUT, I still think taking the PoC ensures more consistency. Also, just taking the PoC, means we can split the whole workflow into 2 steps (i) for a given PoC, perform variant analysis on that bug class by generating set of fuzzing harnesses (ii) run fuzzing engine over the harness set to verify patch correctness, for a given patch. We can then baseline the variant analysis part of our workflow against lightweight static bug-detection tools like FindBugs, PMD, JLint, and Lint4j.

## What each metric currently means

**RCC** is method-level recall of the developer-fix sites. It answers whether the harnesses reached the methods that the developer repaired. It does not yet measure coverage of a broader semantic root-cause region or a caller/callee-expanded neighbourhood.

The denominator is $|\hat{\mathbb{R}}|$, not $|\mathbb{F}(H)|$. For example, if $\hat{\mathbb{R}}$ contains two methods and the harnesses execute both plus 100 unrelated methods, RCC is still $2/2=1$. The unrelated methods neither help nor hurt RCC.

**RCP** is the same numerator over the other denominator. It falls when the harness set runs a lot of code outside the root-cause region. On a small $\hat{\mathbb{R}}$ it is small by construction, so read it as a comparison between harness sets and never as an absolute score.

**RCR** uses no runtime evidence at all. It scores the EXTRACTION: how much of the developer's fix region the APR patch plus its static neighbourhood recovers. A harness set cannot change it. It is the ceiling any patch-conditioned method can reach, so a low RCR caps everything downstream.

**PSC** is the one metric with no $\hat{\mathbb{R}}$ in it. It asks whether the harness set actually covered the region it was steered across. RCC says the harnesses hit the true fault; PSC says whether they also worked the rest of the neighbourhood, which is where a sibling bug would be.

**CSM** is the only metric about findings rather than reach. One crash is one Jazzer finding. `site(c)` is the method the crash really happened in: the parser follows the cause chain to its deepest link, then takes the first frame that is neither JDK, nor Jazzer, nor the harness class itself. A crash whose trace names only the harness has no site — that is an oracle firing on a wrong VALUE, with nothing thrown — and it stays in the denominator. So does a library frame that resolves to no method, which is a plumbing failure and is counted apart from the first case.

RCC, RCP and PSC are reachability metrics. Crashes determine which harnesses enter the current $H_R$, but crash counts and crash sites appear only in CSM.

## How one bug's five values are calculated

For each bug, `sweep_full.py` does the following:

1. Selects one APR patch to produce $H_R$. It prefers an overfitting patch when one exists; otherwise it selects a correct patch. Selection within either class is deterministic.
2. Builds $\hat{\mathbb{R}}$ independently from the Defects4J developer fix.
3. Runs the bug's triggering tests on the buggy build and checks that they reach every method in $\hat{\mathbb{R}}$. A failed check excludes the bug because its numbers would not be trustworthy. This gate is not part of any numerator.
4. Runs the harness-generation pipeline on the selected APR patch and collects the accepted harness set $H_R$.
5. Reruns every accepted harness on the buggy build with a fixed input-count budget and collects dynamic coverage and Jazzer output.
6. Unions coverage across the harnesses to obtain $\mathbb{F}(H_R)$.
7. Builds $\mathbb{P}$ from the same APR patch, and $C$ from the Jazzer output of step 5.
8. Divides, five times.

The result is one value per metric per bug. Each reported mean is the unweighted arithmetic mean of the defined per-bug values, not a pooled `sum(numerator) / sum(denominator)` score. The five populations are not the same size, because the metrics have different denominators and an empty denominator leaves a bug out of that one metric only.

`rescore.py` recomputes all five for a run that `sweep_full.py` already finished. Steps 7 and 8 need no fuzzer and no model, and steps 5 and 6 left their artefacts on disk, so a finished run can be re-scored for the cost of one static analysis per bug.

## Static extraction of $\hat{\mathbb{R}}$

Defects4J stores each developer patch in `framework/projects/<Project>/patches/<id>.src.patch`. The patch runs from fixed code to buggy code, so its post-patch side corresponds to the buggy checkout used for measurement.

`region.py` passes the patch to `diffcov.changed_methods`, which:

- maps each changed post-patch line to its smallest enclosing method or constructor using `javalang`;
- deduplicates methods touched by several changed lines; and
- records imports, fields, class-level declarations, abstract methods, and unparseable locations as `unmapped` rather than silently treating them as methods.

A bug with no mapped method has an empty $\hat{\mathbb{R}}$ and is excluded rather than assigned RCC zero.

This proxy is intentionally narrow. A sibling bug may be one call away from a changed method and therefore belong to the true root-cause region without appearing in the current $\hat{\mathbb{R}}$.

## Static extraction of $\mathbb{P}$

$\mathbb{P}$ comes from the patch under analysis — the APR patch. The pipeline never sees the developer fix, so $\mathbb{P}$ is everything the pipeline itself can know about where the fault lives.

`patchset.py` reuses the pipeline's own `TargetAnalyzer`, so $\mathbb{P}$ is the region the harness set was actually steered across. It has two parts:

1. the TOUCHED functions — every method whose body the APR patch changes, from the same javalang pass;
2. their NEIGHBOURHOOD — the project functions statically reachable from a touched function in the fuzz-introspector call graph, under the same budget the pipeline used (`REACHABLE_NODE_CAP`, `REACHABLE_MAX_DEPTH`).

Two differences from the `root_cause_reachable` list the prompt sees, and both are deliberate. The touched functions stay in, because they are the part of $\mathbb{P}$ most likely to be in $\hat{\mathbb{R}}$; the prompt drops them only because it already prints them in full. And the members are `MethodKey`s, not display labels, because a label loses the parameter types and two overloads then read as one function.

JDK functions are dropped by the same receiver-package test the pipeline uses. `Math.exp` is not part of anybody's root-cause region.

When fuzz-introspector is unavailable or times out, the neighbourhood is empty and $\mathbb{P}$ falls back to the touched functions alone. That is recorded as a note (`introspector_unavailable`) on the bug, so an empty neighbourhood is never confused with a leaf function.

## The crash set $C$

$C$ is read from the measurement pass's own Jazzer output, which `collect.harness_coverage` saves as `harness/jazzer_output.txt`. One `== Java Exception` banner is one crash.

`crashes.py` finds `site(c)` with two rules:

1. Follow the cause chain to its deepest `Caused by:`. A harness that catches a library throwable and rethrows it as its own oracle alarm puts ITSELF at the top of the trace, and the deepest link is the library fault that started it.
2. Skip the harness class. The harness sits in the project package, so a plain "first project frame" rule would name the harness on every crash the harness itself throws. The class names come from the run record.

The frame is then resolved to a method by line, against the same `jacoco.xml`, exactly as `reached_from_stack` resolves a frame. A frame carries no parameter types, so the line is what tells two overloads apart.

Jazzer deduplicates findings within one process, not across processes. Two harnesses that find the same fault therefore report it twice, and CSM is a share of REPORTS rather than of distinct faults.

## Dynamic measurement of $\mathbb{F}(H)$

The runtime path is:

1. Jazzer runs each accepted harness on the buggy build and writes a JaCoCo `.exec` dump.
2. The JaCoCo CLI merges the dumps and produces one `jacoco.xml` report.
3. Fuzz Introspector reads the JaCoCo XML and decodes JVM method descriptors.
4. Every instrumented project method with a positive runtime hit is added to $\mathbb{F}(H)$.

The measurement pass currently uses `-runs=20000` per harness and `--keep_going=1000`. The fixed run count is the fuzzing budget; the wall-clock timeout is only a safety limit.

JaCoCo can miss a method that is entered but throws before its exit probe executes. To repair that case, `reached_from_stack` resolves runtime stack frames to methods using class, method, and source line, then unions those methods with the probe-derived set. The two evidence sources remain separate in the output record.

A missing coverage dump or a report that decodes to no methods is an infrastructure error, never zero coverage.

## Matching methods across the two sets

The static AST and dynamic bytecode coverage use different spellings for the same method. Both are normalized to a `MethodKey` containing the fully qualified class, method name, and parameter types.

Normalization handles nested classes, qualified type names, varargs, arrays, and constructors. Overloads remain distinct. If exact parameter types do not match, there is a reported fallback using class, method name, and argument count; fallback matches on $\hat{\mathbb{R}} \cap \mathbb{F}(H)$ are stored in `rcc_by_arity_only`.

Every intersection in `scores.py` uses that one rule, so the four set metrics are counted the same way. $\mathbb{P}$ is matched with it too, because $\mathbb{P}$ mixes AST spellings (the touched functions) with call-graph spellings (the neighbourhood).

## Exclusions and recorded output

An unavailable measurement is not converted into zero:

| Status | Meaning |
|---|---|
| `excluded_empty_region` | The developer fix mapped to no method body. |
| `excluded_gate_failed` | The triggering tests did not reach all of $\hat{\mathbb{R}}$. |
| `no_harnesses` | The pipeline accepted no harness, so there is no $H$ to measure. |
| `infra_error` | Checkout, build, execution, or coverage collection failed. |

Only records with a numeric value enter that metric's mean. Reports must therefore give the scored population and every exclusion alongside the mean. Three denominators can be empty on their own, and each takes the bug out of ONE metric:

| Empty denominator | Metric left undefined |
|---|---|
| $\hat{\mathbb{R}}$ | RCC and RCR |
| $\mathbb{F}(H)$ | RCP |
| $\mathbb{P}$ | PSC |
| $C$ | CSM |

The JSONL record includes:

- `region_size` and `region`;
- `patch_size`, `patch_touched_size` and `patch_notes`;
- `harness_set_size` and `fuzzer_reached_size`;
- `rcc`, `rcc_covered`, `rcc_missed` and `rcc_by_arity_only`;
- `rcr`, `rcp`, `psc` and `region_in_patch`;
- `csm`, `crashes_total`, `crashes_in_region`, `crashes_off_region`, `crashes_no_frame` and `crashes_unresolved`;
- probe-only and stack-frame-added coverage diagnostics; and
- `status` plus infrastructure-error details when applicable.

The four crash counts never overlap, and with `crashes_in_region` they add up to `crashes_total`.

`per_harness` currently records execution outcomes and dump availability. It does not calculate a separate value for each harness.

## Interpreting the current result

One run so far: `results/rcc_hr_crashing_holdout_20260904_001615`. Its five-metric table and reading are in `metrics_summary.md` beside the run. The means over its 9 scored bugs are RCC 1.000, RCR 0.889, RCP 0.162, PSC 0.583, CSM 0.706. The tenth bug, Lang-43, accepted no harness and is scored on nothing.

The current crashing pipeline accepts a harness only after it crashes the buggy build. In the measured holdout, the crash path includes the method changed by the developer. Acceptance therefore already implies reaching $\hat{\mathbb{R}}$ in almost every scored case. A saturated $RCC(H_R)$ confirms that region extraction, runtime coverage, and method matching are connected correctly, but it does not establish that $H_R$ covers a broad root-cause region or outperforms $H_N$.

Nine denominators of RCC contained one method and the remaining one contained two, making the per-bug score almost binary. Math-70 required stack-frame evidence for the relevant method because JaCoCo's probes alone missed the throwing path.

The other four metrics separate bugs that RCC cannot.

- **RCR is 0.000 on Math-58.** Its overfitting patch changes `LevenbergMarquardtOptimizer.determineLMParameter`, and the developer fix changes `GaussianFitter.fit()` in another package, so $\mathbb{P} \cap \hat{\mathbb{R}}$ is empty. RCC is still 1.000 there, because the fitter lies on the path to the optimizer. RCR is the ceiling on any patch-conditioned method, so read it first.
- **CSM is 0.000 on Math-70.** All three crashes throw inside `solve(f, min, max)`, and $\hat{\mathbb{R}}$ is the four-argument overload `solve(f, min, max, initial)` whose body drops `f`. The fault site and the throw site are two frames apart. This is the narrow-$\hat{\mathbb{R}}$ caveat above, now measured rather than predicted.
- **PSC has the widest spread**, 0.280 to 0.923. It moves against $|\mathbb{P}|$: the bug with the largest neighbourhood has the lowest PSC. So it currently measures the width of the static neighbourhood as much as the harness set's effort. Compare it between harness sets on ONE bug.
- **RCP is small by construction**, mean 0.162. Its numerator is at most $|\hat{\mathbb{R}}|$, which is 1 on eight of nine bugs, so it is close to $1 / |\mathbb{F}(H)|$. It is usable as a paired comparison and not as an absolute score.

Every crash site in that run resolved to a real method, so none of those numbers is a plumbing artefact.

There is not yet an $H_N$ implementation, so the hypothesis $RCC(H_N) \ll RCC(H_R)$ remains untested.

## Running a sweep

First validate region extraction and the triggering-test gate without paying for harness generation:

```bash
python src/metrics/sweep_gate.py \
  --split suites/splits/crashing_split.jsonl \
  --side holdout \
  --out results/rcc_crashing_holdout
```

Then run one end-to-end $H_R$ sweep, which reports all five metrics:

```bash
python src/metrics/sweep_full.py \
  --split suites/splits/crashing_split.jsonl \
  --side holdout \
  --out results/rcc_hr_crashing_holdout \
  --drr drr \
  --model gpt-5.4 \
  --targets 3 \
  --attempts 8 \
  --fuzz_timeout 20 \
  --runs 20000
```

This runs one repetition and does not calculate confidence intervals.

To re-score a run that finished before the other four metrics existed:

```bash
python src/metrics/rescore.py \
  --run results/rcc_hr_crashing_holdout_20260904_001615 \
  --drr drr
```

It writes `metrics.jsonl` beside the run's `rcc.jsonl`, and it recomputes RCC as a check. A recomputed RCC that differs from the recorded one means the run directory and the checkout no longer agree.

Both need Java 11 and Defects4J on the environment:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@11
export PATH="$JAVA_HOME/bin:$PATH"
export PERL5LIB="$HOME/perl5/lib/perl5"
```

## Files

| File | Purpose |
|---|---|
| `region.py` | Builds $\hat{\mathbb{R}}$ from the developer fix. |
| `reached.py` | Builds dynamic $\mathbb{F}(H)$ from JaCoCo reports and stack frames. |
| `patchset.py` | Builds $\mathbb{P}$ from the APR patch and its static neighbourhood. |
| `crashes.py` | Builds $C$ and each crash's site from the Jazzer output. |
| `keys.py` | Normalizes static and dynamic method names. |
| `scores.py` | Computes the five metrics and applies the triggering-test gate. |
| `collect.py` | Builds buggy projects and collects test and harness coverage. |
| `sweep_gate.py` | Checks $\hat{\mathbb{R}}$ and the gate across a split. No pipeline. |
| `sweep_full.py` | Runs the end-to-end $H_R$ experiment and reports all five. |
| `rescore.py` | Re-scores a finished run without repeating the pipeline. |
| `score_one.py` | Scores one bug from reports already on disk. |

Regression tests are in `tests/test_metrics.py`.
