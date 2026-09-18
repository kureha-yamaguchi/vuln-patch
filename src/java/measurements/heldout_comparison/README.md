# Heldout RCC comparison

Run the comparison and generate the report with one command:

```bash
scripts/compare_heldout_rcc.sh -N 30
```

`-N N` (or `--attempts N`) sets candidate attempts per patch per arm and
must be positive; the default is 30. Output goes automatically to
`results/heldout_rcc_{N}` under the repository root. Move an existing run
before starting another with the same settings; results are not overwritten.

Use the pipeline's normal Java, Defects4J, Jazzer and LLM environment,
including the Python fuzz-introspector dependency. The wrapper uses `$PYTHON`,
then the repository virtualenv, then `python3`. Defaults are model `gpt-5.4`,
a 20-second buggy acceptance budget, and one campaign per patch/arm.
`--model`, `--verify-timeout`, and `--repetitions` override these; see `--help`.

## Shorter runs and existing artifacts

Run all three arms on the first 10 certified semantic patches, with 30 candidate
attempts per patch/arm:

```bash
scripts/compare_heldout_rcc.sh -N 30 --kind semantic --max-patches 10
```

Output: `results/heldout_rcc_30_semantic_patches10/`.
`--kind` accepts `crashing`, `semantic`, or `both` (default).
`--max-patches K` caps patches **per selected kind**, keeping every arm and
repetition. Without a cap, all certified patches run.

To measure 10 completed crashing patches from the stopped run, without generating
new harnesses:

```bash
scripts/compare_heldout_rcc.sh -N 30 --kind crashing --max-patches 10 \
  --measure-existing results/heldout_rcc_30
```

Output: `results/heldout_rcc_30_crashing_patches10_measured/`.
This selects the first 10 complete patch groups in the source manifest's order.
All three arms and repetitions must have successful generation records containing
exactly N attempts. Failed or unfinished groups are skipped and recorded in the
new manifest; insufficient complete groups cause an error. N must match the
source run. Without a cap, all complete groups of the selected kind are measured.

The measurement pass reuses the original generation settings and checkouts;
keep the source directory and checkouts available. It adds measurement artifacts
to the original campaign directories. New logs, candidate scores and reports go
to the separate output directory. It makes no LLM calls and does not rerun harness
generation or candidate fuzzing; triggering tests and coverage processing still run.

Selection follows queue/manifest order, not RCC scores. These are ordered subsets,
not random samples or full-heldout evaluations. Reports show selected versus full
patch/bug counts. Equal patch counts need not mean equal bug counts, and confidence
intervals describe the selected bugs only.

## Arms and population

| Arm | Context given to the generator |
|---|---|
| `HR` | Full root-cause conditioning |
| `HN` — level B | Patch and failing tests retained; neighbourhood removed |
| `HN_C` — level C | Function source and fuzzer scaffolding; patch, failing tests and neighbourhood removed |

The naïve levels use the existing `--naive neighbourhood` and `--naive function`
prompts. Level C repair messages also withhold the removed context. Acceptance
checks stay the same. Semantic level C has no lifted-test oracle, so its
crash/acceptance rate measures findings without that oracle.

By default, every certified patch in the frozen holdouts runs, including correct and
overfitting patches. Exclusions and duplicate certification rows are handled
by the existing split-queue builder. Crashing and semantic results are separate.

| Kind | Bugs | Patches | Campaigns across three arms |
|---|---:|---:|---:|
| Crashing | 10 | 51 | 153 |
| Semantic | 27 | 69 | 207 |

Full-run candidate attempts: **360 × N × repetitions** (10,800 at N=30).
Each campaign has an isolated checkout. Execution is serial, with arm order
rotating across patches. Invalid responses and repair responses count toward N;
preparation/synthesis calls do not. The campaign stops after buggy acceptance,
before extra retries and patched-build evaluation. It produces no F1 score.

## Measurements

For each patch and arm, the report includes:

- Compilation proportion: candidates that compiled / N.
- Buggy-crash proportion: compiled candidates that crashed / N.
- Acceptance proportion: candidates passing the full acceptance gate / N.
- Five method-level set metrics, per candidate and per harness set.

A crash can still fail a later acceptance check, so crash and acceptance
proportions are recorded separately. Every set metric uses each candidate's
buggy acceptance execution. The report reuses the measurement CLI, JaCoCo
parsing and stack-frame repair. Developer-fix measurements run after
generation has finished for all arms.

### The five set metrics

| Metric | Formula | Reads |
|---|---|---|
| RCC | \|R0 n F\| / \|R0\| | coverage |
| RCP | \|R0 n F\| / \|F\| | coverage |
| PSC | \|P n F\| / \|P\| | coverage, `patch_derived.json` |
| RCR | \|R0 n P\| / \|R0\| | `patch_derived.json` only |
| \|F(H)\| | count of project methods the set ran | coverage |

**R0, not the full region.** R is the seed ring of `root_cause.json` — the
methods the developer fix changed. The full ringed region is not used
because the archived `root_cause.json` files disagree between arms on their
caller/callee rings (8 of the 10 crashing patch groups) while their seed
rings are identical in all 10. A denominator that changes with the arm
cannot compare arms.

**P is arm-independent.** `patch_derived.json` is written by the analysis
step, which runs before any prompt is built, so it is byte-identical in all
three arms. RCR therefore describes the patch, not the arm. The arms differ
in how much of P the prompt *showed* the model, which is what PSC measures.
P is filtered exactly as `metrics.core.definitions` filters it: JDK callees
are removed, mislabelled-receiver accessors are removed, and members that do
not resolve against the coverage population are counted (`p_unmatched`) and
excluded from the denominator.

**Two denominators, stated per column.** RCC and PSC count a pre-execution
reject as zero: it ran nothing, so it reached none of R0 and none of P. RCP
divides by \|F\|, so a candidate that ran nothing leaves RCP *undefined*
rather than zero. Every per-candidate mean is therefore over compiled
candidates only, except the legacy `candidate_rcc`, which keeps its
all-attempts denominator. `candidate_rcc_compiled` is the same quantity on
the compiled denominator, and the two columns sit side by side so that a
compile-rate gap is never read as a coverage gap.

Accepted-set RCP is undefined for a leg that accepted nothing. Each estimate
carries `defined_bugs`, the number of bugs its interval describes, and the
report names any metric that falls short of the full bug count.

Missing coverage, unresolved R0 methods, empty R0 or a failed
triggering-test gate are unknown measurements, never zeros.

Means give bugs equal weight, averaging patches/repetitions within each bug.
**95% bootstrap confidence intervals** resample whole bugs, preserving all arms
and candidates together. The JSON report includes paired `HR-HN` and `HR-HN_C`
differences for every metric, and those paired differences are what separates
the arms; three overlapping per-arm intervals over 8 or 10 bugs do not.
The Markdown report shows the three arms in three tables: outcomes and RCC,
then |F(H)| and RCP, then PSC, RCR and the set sizes. A per-bug table lists
|R0|, |P| and RCR, so the size of each denominator is visible next to the
ratio built on it. These intervals describe variation across bugs, not independent
candidate draws or run-to-run randomness. Defaults are 10,000 resamples and
seed 20260916, configurable with `--bootstrap` and `--seed`.

## Output

- `comparison.md` and `comparison.json`: separate crashing/semantic tables.
- `candidate_metrics.jsonl`: campaign scores with individual candidate details.
- `manifest.json` and `provenance/`: settings, job list, frozen splits and labels.
- `{crashing,semantic}/{HN,HN_C,HR}/`: per-campaign logs and scores;
  measurement artifacts stay with the source campaigns when using `--measure-existing`.
- `checkouts/`: retained project checkouts, configurable with `--checkout-root`.

Failed jobs remain recorded while other jobs continue. If any measurement is
missing, the command exits with status 2 and writes `comparison-INCOMPLETE.md`
instead of a complete report. A partial bug-kind population is never reported
as the full heldout set.

## Rescore an existing run

To recompute every metric from a finished run's own artifacts, without
generation, measurement or model calls:

```bash
PYTHONPATH=src .venv/bin/python -m java.measurements.heldout_comparison.cli \
  --report-only results/heldout_rcc_30_semantic_patches10
```

This rewrites `comparison.md`, `comparison.json` and
`candidate_metrics.jsonl` in place from `manifest.json` and the artifacts it
points at. Use it after a change to the metric definitions. It exits 2 and
writes `comparison-INCOMPLETE.md` if any leg cannot be scored.

## Replay missing rejected-candidate coverage

When a compiled, rejected candidate timed out without saving coverage, replay its
saved Java source without LLM calls:

```bash
PYTHONPATH=src .venv/bin/python -u -m java.measurements.heldout_comparison.replay \
  results/heldout_rcc_30_crashing_patches10_measured --workers 4
```

Output is the input directory name plus `_replayed`. Originals are preserved.
Only rejected, compiled candidates lacking both original XML and raw coverage
are rerun; accepted candidates and existing coverage are retained. Each replay
recompiles the saved source in its own directory, uses the original random seed,
and copies the final archived corpus. The original per-attempt corpus was not
saved, so this is a new execution, not an exact reconstruction.

A measurement-only Java watchdog saves Jazzer's live probes at the original
outer timeout (fuzz budget plus 15 seconds) before the parent kills a hung JVM.
This uses the installed Jazzer coverage API, validated with version 0.22.1.
Acceptance and compilation outcomes remain those of the original experiment.
The new report labels the mixture of original and replayed coverage, and each
candidate records its coverage origin. The output includes source hashes,
commands, logs, corpus hashes and `replay_progress.json`. Missing replay coverage
still makes the report incomplete; it never becomes zero.
