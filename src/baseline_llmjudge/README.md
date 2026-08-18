# `baseline_llmjudge` — the one-shot LLM baseline

A strong comparison target for the harness pipeline in [`src/java/`](../java).

The pipeline decides whether an automated-repair patch is complete by writing
Jazzer harnesses, running them against the patched build, and asking whether any
of them still crashes. This package answers the same question about the same
patches with the same model and the same evidence, in one shot, with no harness
and no execution of the patched code.

The comparison is only worth reporting if the baseline is strong and the setup
is fair. Four things are held equal by construction. The one difference that
remains is the measurement itself.

| Held equal | How |
|---|---|
| Population | The same queue builder, the same frozen split, the same certified patches |
| Evidence | The pipeline's own prompt sections, three of them byte-identical |
| Model | The same `HarnessGenerator`, the same model, the same effort setting |
| Output space | One bit, two classes, no abstain, no score |
| **Not equal** | The pipeline observes execution. The baseline does not. |

---

## 1. Same population

The queue comes from [`build_split_queue.py`](../java/dataset/build_split_queue.py),
run as a subprocess. That script crosses the frozen split with the certification
files and emits one `-c` or `-o` line per patch. The pipeline's own evaluator
builds its queue with the same script, so a second implementation cannot let the
two populations drift apart.

| Side | Bugs | Patches | Overfitting | Correct | Positive prior |
|---|---|---|---|---|---|
| dev | 8 | 31 | 15 | 16 | 0.48 |
| holdout | 10 | 51 | 14 | 37 | 0.27 |

`verified_correct.jsonl` means "the drr label was audited and accepted". It holds both classes.

Two properties of this population change how the results must be read:

1. **Patches cluster by bug.** 31 dev patches come from 8 bugs. Every patch of
   one bug shares every evidence block except the diff. Inside one bug, the diff
   is the baseline's only discriminating input. `summary.json` therefore carries
   a `by_bug` breakdown, and any interval computed on 31 independent patches is
   too narrow.
2. **The class prior shifts between the sides**, from 0.48 to 0.27. A prompt
   tuned against a near-balanced dev prior can over-predict the positive class
   on the holdout. Read precision and recall, not accuracy. The prior is printed
   in every summary so the shift is visible rather than inferred.

## 2. Same evidence

The pipeline's harness prompt is the crashing branch of `PromptBuilder.build`
([`prompts.py`](../java/harness/prompts.py)). It joins ten kinds of section.
Five carry facts about the patch and the bug. Five teach Jazzer harness
authorship. [`evidence.py`](evidence.py) rebuilds the factual five and drops
the other five.

| Block | Pipeline section | Origin here |
|---|---|---|
| `patch` | `_patch_block` | reused verbatim |
| `source_imports` | `_imports_block` | reused verbatim |
| `touched_function:<name>` | `_function_block` | reused verbatim |
| `trigger_tests` | `_failure_test_block` | re-rendered |
| `root_cause_reachable` | `_variant_analysis_block` | re-rendered |

Reused blocks call the pipeline's own methods. A copy of their text would drift
the moment the harness prompt changed, and the parity claim would quietly become
false. `test_reused_blocks_are_the_pipelines_own_text` asserts the equality.

Two blocks are re-rendered because the pipeline fuses facts with instructions
inside one section:

- `_failure_test_block` wraps the trigger test in an ANCHOR/EXPLORE strategy,
  and its crash-evidence sub-block ends every anchor with "hard-code this
  verbatim as your first call, then fuzz". The re-render keeps the same facts,
  from the same fields, in the same order: the observed throwable and its
  precedence over the declared one, the failure message, the throw site, the
  observed literals, the highlighted trigger call lines, and the test bodies
  under the same 1500-character cap.
- `_variant_analysis_block` lists the root-cause reachable set, then tells the
  harness which part of it earlier harnesses already covered. The list is
  evidence and stays, under the same `MAX_REACHABLE_IN_PROMPT` cap. The coverage
  steering coordinates one harness with the rest of a set, and means nothing for
  a one-shot decision.

Dropped outright: `_hard_constraints`, `_intro`, `_metamorphic_block`,
`_fdp_reference`, `_skeleton_block`. Each states a rule about the `.java` file
the model must emit.

Every record carries a `parity_manifest`: each block's name, its origin, its
character count, and the list of dropped sections. The parity claim is auditable
from the artifact, not from this document.

**Two facts the model is told that the pipeline holds implicitly.** The
candidate patch compiles, and it already passes the whole test suite including
the shown failing test. Without those two sentences the baseline can reason from
a false premise and answer "the test still fails". Neither sentence reveals the
label.

**The honest asymmetry.** The pipeline also gets evidence from execution: a
compile result, a buggy-build trigger, and a patched-build fuzz result. The
baseline gets none of it. That gap is the thing the experiment measures.

**What the baseline is not.** It is not free of execution. The evidence comes
from a Defects4J checkout, and two of its steps run tests on the *buggy* build:
the safety-net gate, and the crash capture. What it never does is build the
patched code, compile a harness, or fuzz. Section 6 accounts for both halves.

**Label leakage.** The ground truth sits in the patch path
(`drr/Patches/Dcorrect/…` against `…/Doverfitting/…`), and the certification
rows state the verdict outright. `tests/test_llmjudge_baseline.py` asserts that
no rendered evidence and no built prompt contains any of it. The label reaches
the selector only, because the selector needs it to find the file.

## 3. Same model

[`run_one.py`](run_one.py) resolves the model as the pipeline does — `--model`
or `config.LOCAL_LLM_MODEL` — and calls it through `llm.HarnessGenerator`, the
same wrapper the harness campaign uses, with the same sampling arguments
(`temperature=0.6, top_p=1.0`). One client, one timeout policy, one retry
policy, one usage recorder.

The current default is `gpt-5.4`, a reasoning model. `llm.py` therefore omits
`temperature` and `top_p`, because the API only accepts their defaults, and
sends `reasoning_effort` instead. One consequence must be stated whenever the
variance figure is quoted: **the five samples of a patch do not vary through a
temperature knob.** They vary through the provider's own nondeterminism at the
default sampling setting. Every summary records the model and the effort
setting.

## 4. Same output space

One bit. Two classes. No abstain option, no confidence score, no severity. A
wider output space would need a threshold, and a threshold tuned on dev is a
free parameter the pipeline does not have.

The model must end its answer with `VERDICT: INCOMPLETE` or `VERDICT: CORRECT`.
[`verdict.py`](verdict.py) reads the last such line, because a scaffolded answer
can name the other class on its way to a conclusion.

Records carry `predicted_incomplete` and mirror it into `crashed_on_patch`, the
field the pipeline's aggregator reads. One scoring function then serves both
sides, so the arithmetic cannot drift between them.

**Parse failures.** One retry, then the sample counts as the negative class and
`n_parse_failures` records it. An unparsed sample is never dropped: dropping it
would hand the baseline a filter the pipeline never gets, because a pipeline run
that produces no usable harness is still scored. `summary.json` also carries
`headline_excluding_parse_failures` when any occurred, so the cost of that
default is visible.

## 5. Five samples, and the vote rule

All five verdicts are stored. Three rules are reported for every patch:

| Rule | Threshold | Why it is reported |
|---|---|---|
| `majority` | 3 of 5 | The standard self-consistency rule. **This is the pre-registered headline.** |
| `any` | 1 of 5 | Matches the pipeline's own rule: one harness that fires decides the patch. |
| `unanimous` | 5 of 5 | The high-precision end of the curve. |

`majority` is the headline. The other two are a sensitivity curve, never a
second headline. `HEADLINE_RULE` in [`evaluate.py`](evaluate.py) is the single
place that choice lives.

Each patch also reports `agreement`, the share of sample pairs that agree. That
is the baseline's own variance, and it is the fair counterpart to the pipeline's
run-to-run variance.

## 6. Dev iteration protocol

The protocol is bounded and declared in advance, so the baseline cannot be
accused of being either under-tuned or tuned against the holdout.

**The budget: at most four prompt versions, each scored once on the full dev
side.** [`prompts.py`](prompts.py) holds them. Each version records the
hypothesis that distinguishes it from the one before.

| Version | Hypothesis |
|---|---|
| `v0` | Floor. Evidence, the question, and the output contract only. |
| `v1` | v0 leaves the class boundary and the plausibility premise to be guessed. State both, and ask for a concrete surviving input. |
| `v2` | A free-form answer drifts toward style review, and toward calling a narrow patch incomplete without evidence. Fix the decision procedure, and require a named input for the positive class. |
| `v3` | v2's steps are advice the answer can skip. Make them required output sections. |

**The rules:**

1. The system message is identical across versions. The task wording is the only
   variable, so a dev score difference cannot come from two changes at once.
2. Evidence is extracted once per patch and cached. All versions read one
   byte-identical evidence string, so a score difference can only come from the
   prompt wording.
3. A version is frozen once it has been scored on dev. A new idea becomes a new
   version. Do not edit a scored version, because the recorded score refers to
   its text.
4. `v2` and `v3` were designed before the first dev run. The protocol allows
   either to be **replaced** — never edited — once the v0/v1 dev errors are
   read. Record any replacement in the CHANGELOG at the top of `prompts.py`,
   with the error class that motivated it.
5. Stop early when a version improves dev F1 by less than two points.
6. Select the winner by dev F1. Break a tie by the lower false-positive count.
7. Publish every version's dev summary, including the discarded ones. That table
   is the evidence that the baseline was tuned honestly.
8. The holdout is read once, after the winner is frozen. `--side holdout`
   refuses to run without `--confirm_holdout`.

**Before the holdout run, write down:** the model, the effort setting, the
sample count, the vote rule, the parse-failure default, and the population.
Every one of them is already in `summary.json`, so the note is a copy, not a
new claim.

### Iteration log

Fill one row in after each dev pass, from that run's `summary.json`. This table
is the published record required by rule 7. An empty row means the version was
never scored, which is itself a fact a reader needs.

| Version | Dev P | Dev R | Dev F1 | FP | Parse failures | Agreement | Run directory |
|---|---|---|---|---|---|---|---|
| v0 | | | | | | | |
| v1 | | | | | | | |
| v2 | | | | | | | |
| v3 | | | | | | | |

Winner: _(record the version and the reason: highest dev F1, ties broken by
fewer false positives)_

Holdout run: _(one row, added after the single holdout pass)_

## 7. Budget disclosure

Token usage comes from the shared recorder in [`llm.py`](../llm.py), the same
one the pipeline reports. [`budget.py`](budget.py) turns it into a cost, under
two rules:

1. **No invented prices.** A price comes from the environment, or the report
   says the price was not set. A cost figure with a guessed rate behind it is
   worse than no cost figure.
2. **Cached input is stated, never hidden.** The five samples of one patch send
   a byte-identical prompt, so the provider serves most of that input from its
   prompt cache. Patches of the same bug also share every block except the diff,
   so the cache carries across patches too — a measured run showed 6,912 of
   7,062 prompt tokens served from cache on the fourth patch of one bug.
   `prompt_tokens` still counts cached tokens in full, so
   `cost_usd_full_rate` is an **upper bound** on the invoice.
   `cost_usd_with_cache_rate` is the closer estimate, and it appears once a
   cached-input rate is set. `cache_hit_rate` is reported either way.

```bash
export LLMJUDGE_PRICE_IN_USD_PER_MTOK=...
export LLMJUDGE_PRICE_OUT_USD_PER_MTOK=...
export LLMJUDGE_PRICE_CACHED_IN_USD_PER_MTOK=...   # optional
```

### Measured cost

Measured with `v1` on `gpt-5.4`: **3,686 prompt and 1,792 completion tokens**
for one call on `patch1-Lang-27-DeepRepair`, and about **7,060 prompt with
850–1,270 completion tokens** per call on the five Chart-9 patches. Evidence
size varies by bug, so treat 4k–8k prompt tokens per call as the working range.

| Stage | Calls | Prompt tokens | Completion tokens |
|---|---|---|---|
| dev, one prompt version | 155 | 0.6M – 1.2M | ~0.28M |
| dev, four versions | 620 | 2.5M – 4.8M | ~1.1M |
| holdout, one pass | 255 | 1.0M – 2.0M | ~0.46M |
| **total** | **875** | **3.5M – 6.8M** | **~1.6M** |

For scale, one pipeline dev pass measured 209 calls, 1,713,259 prompt tokens and
669,707 completion tokens on the same model.

### Report both halves of the cost

A token-only table flatters the pipeline, because tokens are not where its cost
sits. Report all four rows:

| Cost | Pipeline | This baseline |
|---|---|---|
| Model tokens | yes | yes |
| Checkout + trigger-test runs on the buggy build | yes | yes (cached per patch) |
| Patched-build compile, harness compile, Jazzer time | yes | **no** |
| Wall-clock per patch | minutes | seconds after the first extraction |

Also report the dev-iteration spend separately from the holdout spend, and state
how many prompt versions were discarded. The baseline gets four dev passes, and
the accounting must say so.

## 8. Head-to-head

Pass a pipeline run's `records.jsonl` with `--tool_records` and the summary gains
a `head_to_head` block:

- `baseline_on_paired` — the matrix restricted to the patches the pipeline also
  scored. This is the paired comparison.
- `tool_only_right` and `baseline_only_right` — the discordant pairs.
- `mcnemar_exact_two_sided_p` — the exact McNemar p-value. The decisions are
  paired per patch, so two independent proportion tests would be the wrong test.

Report the full-population matrix too. It is the baseline's own number, and the
difference between the two shows what the pipeline's `no_harnesses` exclusion
costs. At these sample sizes a few points of F1 will not reach significance. Say
so before the numbers arrive.

## 9. Threats to validity

- **Contamination.** Defects4J and drr are public, and the developer fixes are
  in the training data of any recent model. This helps the baseline, not the
  pipeline, so it works against the claim — the safe direction. Probe it
  separately: ask the model to name the bug and the fix, and report the recall
  rate.
- **The trigger test is a strong hint.** Its failure message often names the
  root cause. Both sides see it, so parity holds, but the baseline may exploit
  it more directly.
- **Crashing bugs only.** That is what the split covers. The semantic split is
  out of scope here.
- **Five samples is a small sample count.** The agreement figure carries a wide
  interval at n=5.

## 10. Files

| File | Responsibility |
|---|---|
| [`context.py`](context.py) | Extract the evidence for one patch; cache it as rendered text |
| [`evidence.py`](evidence.py) | Render the evidence blocks; build the parity manifest |
| [`prompts.py`](prompts.py) | The four frozen prompt versions |
| [`verdict.py`](verdict.py) | Parse one verdict; combine samples under the three vote rules |
| [`budget.py`](budget.py) | Tokens to cost, with the price and cache rules above |
| [`run_one.py`](run_one.py) | One patch, N samples, one record |
| [`evaluate.py`](evaluate.py) | One side of the split: records, summary, budget, head-to-head |

Guards live in [`tests/test_llmjudge_baseline.py`](../../tests/test_llmjudge_baseline.py).

## 11. Usage

Run from `src/`.

```bash
# print the dev queue and stop, before any model call
uv run -m baseline_llmjudge.evaluate --side dev --dry_run

# one dev pass for one prompt version (repeat per version)
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v0
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v1

# one patch, for error analysis between versions
uv run -m baseline_llmjudge.run_one -o \
  --patch_file ../drr/Patches/Doverfitting/DeepRepair/Lang/patch1-Lang-27-DeepRepair.patch \
  --prompt_version v1 --samples 5

# the holdout, once, with the frozen winner and a paired comparison
uv run -m baseline_llmjudge.evaluate --side holdout --prompt_version v2 \
  --confirm_holdout \
  --tool_records ../results/eval_holdout_<timestamp>/records.jsonl
```

| Option | Meaning |
|---|---|
| `--side dev\|holdout` | Which side of the frozen split to score |
| `--prompt_version v0..v3` | Which frozen prompt to use |
| `--samples N` | Samples per patch (default 5) |
| `--projects "Lang Math"` | Restrict to some projects |
| `--model` | Override the model; the default is `config.LOCAL_LLM_MODEL` |
| `--cache_dir` | Where extracted evidence is cached (default `results/llmjudge_cache`) |
| `--refresh_context` | Re-extract even when cached |
| `--tool_records` | A pipeline `records.jsonl`, for the paired head-to-head |
| `--confirm_holdout` | Required for `--side holdout` |
| `--dry_run` | Build the queue and stop |

Output lands in `results/llmjudge_<side>_<version>_<timestamp>/`:
`records.jsonl`, `summary.json`, `queue.txt`, and a copy of the split with its
git provenance — so a number stays traceable to the split and the code that
produced it.
