# Heldout RCC comparison

Run the comparison and generate the report with one command:

```bash
scripts/compare_heldout_rcc.sh -N 30
```

`-N N` (or `--attempts N`) sets candidate attempts per patch per arm and
must be positive; the default is 30. Output goes automatically to
`results/heldout_rcc_{N}` under the repository root. Move an existing run
before starting another with the same N; results are not overwritten.

Use the pipeline's normal Java, Defects4J, Jazzer and LLM environment,
including the Python fuzz-introspector dependency. The wrapper uses `$PYTHON`,
then the repository virtualenv, then `python3`. Defaults are model `gpt-5.4`,
a 20-second buggy acceptance budget, and one campaign per patch/arm.
`--model`, `--verify-timeout`, and `--repetitions` override these; see `--help`.

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

Every certified patch in the frozen holdouts runs, including correct and
overfitting patches. Exclusions and duplicate certification rows are handled
by the existing split-queue builder. Crashing and semantic results are separate.

| Kind | Bugs | Patches | Campaigns across three arms |
|---|---:|---:|---:|
| Crashing | 10 | 51 | 153 |
| Semantic | 27 | 69 | 207 |

Total candidate attempts: **360 × N × repetitions** (10,800 at N=30).
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
and candidates together. The report includes paired `HR-HN` and `HR-HN_C`
differences. These intervals describe variation across bugs, not independent
candidate draws or run-to-run randomness. Defaults are 10,000 resamples and
seed 20260916, configurable with `--bootstrap` and `--seed`.

## Output

- `comparison.md` and `comparison.json`: separate crashing/semantic tables.
- `candidate_metrics.jsonl`: campaign scores with individual candidate details.
- `manifest.json` and `provenance/`: settings, job list, frozen splits and labels.
- `{crashing,semantic}/{HN,HN_C,HR}/`: per-campaign logs and measurement artifacts.
- `checkouts/`: retained project checkouts, configurable with `--checkout-root`.

Failed jobs remain recorded while other jobs continue. If any measurement is
missing, the command exits with status 2 and writes `comparison-INCOMPLETE.md`
instead of a complete report. A partial bug-kind population is never reported
as the full heldout set.
