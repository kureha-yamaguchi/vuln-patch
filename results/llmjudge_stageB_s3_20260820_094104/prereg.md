# Pre-registration — semantic stage B, base `s3`

Written before the first semantic holdout pass. Every field below is a copy of a
field in `results/llmjudge_dev_s3_20260819_222336/summary.json`. Nothing here is
a new claim.

## What is fixed before any holdout number exists

| Item | Value |
|---|---|
| Bug pool | `semantic` |
| Frozen split | `suites/splits/semantic_split.jsonl` |
| Base design | `s3`, `prompt_sha256` `6737e7ca082fdb41e17455aa5dfabf2d725070fd0c0fcb58893ae34c6b4faf6b` |
| Model | `gpt-5.4` |
| Reasoning effort | `low` |
| Samples per patch | 5 |
| Headline vote rule | `majority` (3 of 5) |
| Reported sensitivity rules | `any` (1 of 5), `unanimous` (5 of 5) |
| Parse failure counts as | `correct` (the negative class); never dropped |
| Parse retries | 1 |
| Positive class | ground truth `overfitting` |
| Selection rule, stage B | highest **holdout** F1, ties broken by fewer false positives |
| Turns | three, with no early stop |
| Code | git `923ffce05faa1c20315b7500b60db0b985b5e723` |

## Population

| Side | Bugs | Patches | Overfitting | Correct | Positive prior |
|---|---|---|---|---|---|
| dev | 43 | 110 | 39 | 71 | 0.355 |
| holdout | 27 | 69 | 23 | 46 | 0.333 |

Patch counts are de-duplicated: `suites/labels/verified_correct.jsonl` repeats
30 semantic rows, and `build_split_queue.py` queues a patch once. The split
file's own `correct_legs` fields are stale — see section 1 of the package README.

## Stage-A result this stage refines

| Design | Dev P | Dev R | Dev F1 | FP | FN | Parse failures | Agreement |
|---|---|---|---|---|---|---|---|
| s1 | 0.596 | 0.872 | 0.708 | 23 | 5 | 0 | 0.953 |
| s2 | 0.620 | 0.795 | 0.697 | 19 | 8 | 0 | 0.918 |
| s3 | 0.630 | 0.872 | **0.731** | 20 | 5 | 0 | 0.885 |

Winner `s3`, on highest dev F1. All three designs ran blind, and all three are
published above.

## What each side does

| Side | Its job | What it hands back |
|---|---|---|
| dev | Refinement. Each turn reads the previous iteration's dev errors. | The error text, in full |
| holdout | Selection. Every iteration is scored on it. | One F1 per iteration |

No holdout output reaches any prompt. `errors.py` refuses holdout records, and
`prompts.build_messages` takes a version name and the evidence text only.

## What will be reported

Four holdout rows: `s3` as the reference, plus `s3.1`, `s3.2` and `s3.3`. The
selected iteration's holdout F1 is a maximum over three, so it is optimistic,
and the spread between the three rows is the size of that optimism. The honest
sentence names the population the number was selected from.

If no iteration beats `s3` on holdout, that is recorded as the finding. It is a
result about the method, not a failed run.
