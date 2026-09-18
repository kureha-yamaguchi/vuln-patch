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
- Each candidate's RCC and the mean across N candidates.
- Accepted-set RCC: coverage of the union of accepted candidates.

A crash can still fail a later acceptance check, so crash and acceptance
proportions are recorded separately. RCC is method-level coverage of R0, the
methods changed by the developer fix, using each candidate's buggy acceptance
execution. It reuses the measurement CLI, JaCoCo parsing and stack-frame repair.
Developer-fix measurements run after generation has finished for all arms.

Pre-execution rejects and empty accepted sets score zero when R0 is defined.
Missing coverage, unresolved methods, empty R0 or a failed triggering-test gate
are unknown measurements, never zeros.

Means give bugs equal weight, averaging patches/repetitions within each bug.
**95% bootstrap confidence intervals** resample whole bugs, preserving all arms
and candidates together. The JSON report includes paired `HR-HN` and `HR-HN_C`
differences. The Markdown table shows only the three arms, with compilation,
acceptance, mean candidate RCC and accepted-set RCC. These intervals describe variation across bugs, not independent
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
