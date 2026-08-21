# Stage B pre-registration — base v1

Written before the first holdout pass. Every value is a copy of a field in a
`summary.json`, not a new claim.

| Item | Value | Source |
|---|---|---|
| Base design (stage-A winner) | `v1`, dev F1 = 0.743, FP = 7, FN = 2 | `llmjudge_stageA_20260818_140449/compare_A.log` |
| Model | `gpt-5.4` | `summary.json:model` |
| Reasoning effort | `low` | `summary.json:reasoning_effort` |
| Samples per patch | 5 | `summary.json:samples_per_patch` |
| Headline vote rule | `majority` (3 of 5) | `summary.json:headline_rule` |
| Parse-failure default | counts as `correct` (the negative class); never dropped | `summary.json:parse_failure_counts_as` |
| Dev population | 31 patches, 8 bugs, positive prior 0.484 | dev `summary.json` |
| Holdout population | 51 patches, 10 bugs, positive prior 0.275 (14 overfitting, 37 correct) | holdout queue |
| Iterations | `v1.1`, `v1.2`, `v1.3` — all three are written and run, no early stop | README rule 6 |
| Refinement input | the previous iteration's **dev** error log only | README rule 2 |
| Selection | highest **holdout** F1; ties break on fewer FP | `compare.py:SELECTION_SIDE` |
| Reported bias | the winner's holdout F1 is a maximum over three, so all four holdout rows are published | README section 6 |

## One deviation, and its reason

`--tool_records` is not passed on any holdout pass. No pipeline holdout run
exists in `results/` — every `eval_*` run is a dev run. So the paired
head-to-head and the McNemar counts cannot be computed on this side, and those
columns of the iteration log stay empty. That is a missing input, not a choice
about the protocol.
