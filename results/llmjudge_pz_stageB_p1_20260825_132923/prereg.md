# Pre-registration — Project Zero stage B, base `p1`, crashing pool

Written before the first holdout pass. Every field below is a copy of a
`summary.json` field, so this note is checkable against the runs it precedes.

## What is fixed before any holdout number exists

| Field | Value |
|---|---|
| Dataset | `project_zero`, the variant-pair dataset in `src/db/project_zero/pairs/` |
| Pool | `crashing` only (`--bug_kind crashing`) |
| Split | `suites/splits/project_zero_split.jsonl`, frozen per root-cause group |
| Model | `gpt-5.4` |
| Reasoning effort | `low` (`OPENAI_REASONING_EFFORT`) |
| Samples per fix | 5 |
| Headline rule | `majority`, 3 of 5 |
| Sensitivity rules | `any` and `unanimous`, reported and never a second headline |
| Unparsed sample counts as | `correct`, the negative class |
| Parse retries | 1, then the sample is a parse failure |
| Base under refinement | `p1` |
| Selection criterion | highest **holdout** F1, ties broken by fewer false positives |

## The population of each side

Measured, and recorded here so a later fetch cannot quietly move it.

| Side | Rows | Overfitting | Correct | Positive prior |
|---|---|---|---|---|
| dev | 21 | 11 | 10 | 0.52 |
| holdout | 23 | 15 | 8 | 0.65 |

**The prior differs by 0.13 between the sides.** A version tuned against the
dev prior can over-predict the positive class on the holdout. So read precision
and recall, never accuracy. The split balances the prior over the whole
population, not inside one pool, and the crashing pool is a subset of it.

## The floors that read no code

Any F1 from this experiment must be quoted beside these. They are recomputed
per run and stored in `summary.json` under `baselines`.

| Side | Always-positive F1 | Best size-rule F1 |
|---|---|---|
| dev | 0.688 | 0.645 |
| holdout | to be recorded by the first holdout pass | same |

A version that does not beat the higher floor by more than 0.05 is unproven,
whatever its rank. `compare.py` marks such a row.

## The stage-A result this refines

All three stage-A designs scored **F1 0.000** on the crashing dev side.

| Version | P | R | F1 | FP | FN | Parse failures |
|---|---|---|---|---|---|---|
| `p1` | n/a | 0.000 | 0.000 | 0 | 11 | 0 |
| `p2` | n/a | 0.000 | 0.000 | 0 | 11 | 0 |
| `p3` | 0.000 | 0.000 | 0.000 | 1 | 11 | 0 |

**`p1` and `p2` tie exactly.** The tie-break is the false-positive count, and
that is equal too, so the registry order decided it. The choice of base carries
no information, and no claim in the write-up may rest on `p1` being "the best
design".

## The registered iterations

| Version | What it changes |
|---|---|
| `p1.1` | Breaks the inference "generic, therefore complete", and states that the shown source may not contain the sibling |

Further turns are written after their parent's dev pass, and this note is
amended with each one before its holdout pass runs.

## Two predictions, recorded so they can be wrong

1. `p1.1` will raise recall above 0.000. The dev failure was one reasoning
   step, and `p1.1` contradicts that step directly.
2. `p1.1` will not clear the holdout floor. The dev log shows the sibling of
   several pairs living in a file the evidence never renders, and no wording
   can supply code that is absent.

If prediction 1 fails as well, the conclusion is about the evidence and not
about the prompt, and the next step is the fetch step rather than a `p1.2`.

## Known defects in this population, unfixed at pre-registration time

1. **One fix is counted twice.** `pz-99d649ce` and `pz-81824d3f` are the same
   commit: `CVE-2022-3723__CVE-2022-4906/fix0` records the SHA, and
   `chromium-1378239__CVE-2022-3723/fix0` records `CL/3981277`, which resolves
   to it. `fix_id` keys on the raw metadata string, so the dedup misses it.
   Both rows are positive, so the crashing dev side holds 10 distinct
   positives, not 11.
2. **Six of the ten dev negatives are siblings of one prior CVE.** They are the
   six later fixes of `CVE-2019-13720`. The negative class is less diverse than
   its count suggests.

Both are recorded here rather than fixed, because fixing either one now would
change the population between stage A and stage B and make the two
incomparable. They are repaired after this stage closes.
