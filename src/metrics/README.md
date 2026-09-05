# `src/metrics` — root-cause coverage

Current implementation of calculating $RCC(H_R)$:
- currently just an implementation of $RCC(H_R)$, where $H_R$ is the root-cause conditioned harness set
- taking function-level representations of $\mathbb{P}$, $\mathbb{R}$, and $\mathbb{F}(H)$
- $\hat{\mathbb{R}}$: extracted statically from developer-written fix changed methods
- $\mathbb{F}(H)$: measured dynamically by running the harnesses. (The accepted harnesses are executed on the buggy build, and runtime coverage is collected using JaCoCo. The `.exec` coverage dumps are merged across all harnesses, then converted to `jacoco.xml`. Any method observed executing becomes part of $\mathbb{F}(H)$.)
- $\mathbb{P}$: statically derived from the patch neighborhood, although, it's not currently used in RCC.
- only analysing crashing bugs, heldout set, overfitting patches 



TODOs (not necessarily in order)

1. extend to semantic bugs and correct patches as well
2. report the other metrics, as well as F1 score for the same run. **Would ideally like a single bash script that runs the pipeline on the whole heldout set for $n$ repetitions, and then report the average and confidence intervals for the 5 set-theory metrics + F1 score**
3. explore edge-level representations of $\mathbb{P}$, $\mathbb{R}$, and $\mathbb{F}(H)$
4. implement the naïve baseline (without patch context) to evaluate $RCC(H_N)$
5. I think it would be interesting to swap out patch diff for Poc, but feeding in \{PoC\} + \{triggering tests\} + \{extracted PoC-derived set\} to the system. Because then, the quality of $\mathbb{R} \cap \mathbb{P}$ is not depended on whether the patch-derived set is from an overfitting or correct patch. I know we propagrate up and down the patch touched functions to draw a neighbourhood region, so the patch-derived set should still be informative even if extracting from an overfitting patch- BUT, I still think taking the PoC ensures more consistency. Also, just taking the PoC, means we can split the whole workflow into 2 steps (i) for a given PoC, perform variant analysis on that bug class by generating set of fuzzing harnesses (ii) run fuzzing engine over the harness set to verify patch correctness, for a given patch. We can then baseline the variant analysis part of our workflow against lightweight static bug-detection tools like FindBugs, PMD, JLint, and Lint4j.

## What RCC currently means

The implemented metric is:

$$
RCC(H) = \frac{|\hat{\mathbb{R}} \cap \mathbb{F}(H)|}{|\hat{\mathbb{R}}|}.
$$

This is method-level recall of the developer-fix sites. It answers whether the harnesses reached the methods that the developer repaired. It does not yet measure coverage of a broader semantic root-cause region or a caller/callee-expanded neighbourhood.

The denominator is $|\hat{\mathbb{R}}|$, not $|\mathbb{F}(H)|$. For example, if $\hat{\mathbb{R}}$ contains two methods and the harnesses execute both plus 100 unrelated methods, RCC is still $2/2=1$. The unrelated methods neither help nor hurt RCC.

RCC itself is a reachability metric. Crashes determine which harnesses enter the current $H_R$, but crash counts and crash sites do not appear in the RCC formula.

## How one RCC value is calculated

For each bug, `rcc_sweep.py` does the following:

1. Selects one APR patch to produce $H_R$. It prefers an overfitting patch when one exists; otherwise it selects a correct patch. Selection within either class is deterministic.
2. Builds $\hat{\mathbb{R}}$ independently from the Defects4J developer fix.
3. Runs the bug's triggering tests on the buggy build and checks that they reach every method in $\hat{\mathbb{R}}$. A failed check excludes the bug because its RCC would not be trustworthy. This gate is not part of the numerator.
4. Runs the harness-generation pipeline on the selected APR patch and collects the accepted harness set $H_R$.
5. Reruns every accepted harness on the buggy build with a fixed input-count budget and collects dynamic coverage.
6. Unions coverage across the harnesses to obtain $\mathbb{F}(H_R)$, intersects it with $\hat{\mathbb{R}}$, and divides by $|\hat{\mathbb{R}}|$.

The result is one RCC value per bug. The reported mean is the unweighted arithmetic mean of the defined per-bug values, not a pooled `sum(covered) / sum(region size)` score.

## Static extraction of $\hat{\mathbb{R}}$

Defects4J stores each developer patch in `framework/projects/<Project>/patches/<id>.src.patch`. The patch runs from fixed code to buggy code, so its post-patch side corresponds to the buggy checkout used for measurement.

`region.py` passes the patch to `diffcov.changed_methods`, which:

- maps each changed post-patch line to its smallest enclosing method or constructor using `javalang`;
- deduplicates methods touched by several changed lines; and
- records imports, fields, class-level declarations, abstract methods, and unparseable locations as `unmapped` rather than silently treating them as methods.

A bug with no mapped method has an empty $\hat{\mathbb{R}}$ and is excluded rather than assigned RCC zero.

This proxy is intentionally narrow. A sibling bug may be one call away from a changed method and therefore belong to the true root-cause region without appearing in the current $\hat{\mathbb{R}}$.

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

Normalization handles nested classes, qualified type names, varargs, arrays, and constructors. Overloads remain distinct. If exact parameter types do not match, RCC has a reported fallback using class, method name, and argument count; fallback matches are stored in `rcc_by_arity_only`.

## Exclusions and recorded output

An unavailable measurement is not converted into zero:

| Status | Meaning |
|---|---|
| `excluded_empty_region` | The developer fix mapped to no method body. |
| `excluded_gate_failed` | The triggering tests did not reach all of $\hat{\mathbb{R}}$. |
| `no_harnesses` | The pipeline accepted no harness, so there is no $H$ to measure. |
| `infra_error` | Checkout, build, execution, or coverage collection failed. |

Only records with a numeric `rcc` value enter the mean. Reports must therefore give the scored population and every exclusion alongside the mean.

The JSONL record includes:

- `region_size` and `region`;
- `harness_set_size` and `fuzzer_reached_size`;
- `rcc`, `rcc_covered`, and `rcc_missed`;
- `rcc_by_arity_only`;
- probe-only and stack-frame-added coverage diagnostics; and
- `status` plus infrastructure-error details when applicable.

`per_harness` currently records execution outcomes and dump availability. It does not calculate a separate RCC for each harness.

## Interpreting the current result

The current crashing pipeline accepts a harness only after it crashes the buggy build. In the measured holdout, the crash path includes the method changed by the developer. Acceptance therefore already implies reaching $\hat{\mathbb{R}}$ in almost every scored case. A saturated $RCC(H_R)$ confirms that region extraction, runtime coverage, and method matching are connected correctly, but it does not establish that $H_R$ covers a broad root-cause region or outperforms $H_N$.

In the current holdout run, 9 of 10 bugs were scored and all nine had RCC $1.0$. The remaining bug produced no accepted harness. Nine denominators contained one method and the remaining denominator contained two, making the per-bug score almost binary. Math-70 required stack-frame evidence for the relevant method because JaCoCo's probes alone missed the throwing path.

There is not yet an $H_N$ implementation, so the hypothesis $RCC(H_N) \ll RCC(H_R)$ remains untested.

## Running the current sweep

First validate region extraction and the triggering-test gate without paying for harness generation:

```bash
python src/metrics/sweep.py \
  --split suites/splits/crashing_split.jsonl \
  --side holdout \
  --out results/rcc_crashing_holdout
```

Then run one end-to-end $RCC(H_R)$ sweep:

```bash
python src/metrics/rcc_sweep.py \
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

## Files

| File | Purpose |
|---|---|
| `region.py` | Builds $\hat{\mathbb{R}}$ from the developer fix. |
| `reached.py` | Builds dynamic $\mathbb{F}(H)$ from JaCoCo reports and stack frames. |
| `keys.py` | Normalizes static and dynamic method names. |
| `rcc.py` | Computes RCC and applies the triggering-test gate. |
| `collect.py` | Builds buggy projects and collects test and harness coverage. |
| `sweep.py` | Checks $\hat{\mathbb{R}}$ and the gate across a split. |
| `rcc_sweep.py` | Runs the end-to-end $RCC(H_R)$ experiment. |
| `cli.py` | Computes RCC for one bug from existing reports. |

Regression tests are in `tests/test_metrics_rcc.py`.
