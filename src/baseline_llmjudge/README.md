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

The model must end its answer with `VERDICT: OVERFITTING` or `VERDICT: CORRECT`.
[`verdict.py`](verdict.py) reads the last such line, because a scaffolded answer
can name the other class on its way to a conclusion.

**One word per class.** The two class names are the two ground-truth labels, in
upper case. So a per-patch line reads
`Lang-27 (SimFix) [correct]: 5/5 overfitting -> majority=overfitting`: the
bracket is the truth, the word after `majority=` is the prediction, and the two
compare directly with no translation step. `verdict.class_name` is the one
place that maps the decision bit to its word.

Records carry `predicted_overfitting` and mirror it into `crashed_on_patch`,
the field the pipeline's aggregator reads. One scoring function then serves both
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

Two stages. Stage A is a blind bake-off between three independent designs.
Stage B refines the winner three times, and only stage B reads an error log.

```
STAGE A — blind. No error log is read.
    v1  ──run on dev──┐
    v2  ──run on dev──┼──→  best dev F1 wins        (say v2 wins)
    v3  ──run on dev──┘

STAGE B — refinement of the winner, three turns. Each turn reads errors.
    v2  ──read v2's errors──→   v2.1  ──run on dev
        ──read v2.1's errors──→ v2.2  ──run on dev
        ──read v2.2's errors──→ v2.3  ──run on dev
                                  │
    best dev F1 of v2.1 / v2.2 / v2.3  ──→  freeze  ──→  holdout ONCE
```

Six dev passes in total, then one holdout pass.

### Stage A — the three designs

They are authored blind, before any run, and they differ in how much method the
request supplies. **The numbers are labels, not an order**, because the three
run independently. A fourth draft was dropped before any run, because it was a
strict subset of `v2`.

| Design | What the request supplies | The bet |
|---|---|---|
| `v1` | The evidence, the question, the output contract. Nothing else. | The floor: what the model does unaided |
| `v2` | Definitions, the plausibility premise, a five-step method, two calibration rules | A method plus guard rails beats an unaided answer |
| `v3` | Definitions, the plausibility premise, five **required** output sections | A form the answer must fill in beats a method it may skip |

**Stage A is blind on purpose.** If one design's errors were read before the
others ran, the later designs would carry information the earlier ones did not,
and the bake-off would no longer compare designs — it would compare designs
plus unequal hindsight. `errors.py` prints a warning when it is pointed at a
stage-A run.

```bash
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v1
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v2
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v3

uv run -m baseline_llmjudge.compare --stage A     # names the winner
```

### Stage B — three refinement turns

Each turn is: read the previous run's errors, write the next iteration to repair
the dominant error class, run it on dev. An iteration is named `<winner>.<n>`,
so the lineage of every score is readable from its name.

```bash
# turn 1 — read the stage-A winner's errors, then write v2.1 in prompts.py
uv run -m baseline_llmjudge.errors \
    --records ../results/llmjudge_dev_v2_<ts>/records.jsonl
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v2.1

# turns 2 and 3 — same shape, reading the previous turn's records
uv run -m baseline_llmjudge.errors \
    --records ../results/llmjudge_dev_v2.1_<ts>/records.jsonl
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v2.2
```

`errors.py` prints the false-positive and false-negative counts, names the
dominant class, then prints each error with the model's own reasoning. The two
classes need opposite repairs:

| Dominant class | What it means | What the next iteration should do |
|---|---|---|
| FP | A correct patch was called overfitting | Raise the bar for the positive class: demand a named surviving input, forbid style objections |
| FN | An overfitting patch was called correct | Name the patterns being missed: guard on the reported value, swallowed error, unfixed sibling path, off-by-one bound |

Add the iteration to [`prompts.py`](prompts.py) with `register(...)`, and set
its `hypothesis` to the error class it repairs. The file carries a worked
example to copy.

### Selection and the holdout

```bash
uv run -m baseline_llmjudge.compare --stage B --base v2
uv run -m baseline_llmjudge.evaluate --side holdout --prompt_version v2.2 \
    --confirm_holdout --tool_records ../results/eval_holdout_<ts>/records.jsonl
```

Both stages select the same way: **highest dev F1, ties broken by the lower
false-positive count.** Stage B selects among the three iterations. The stage-A
winner appears in the comparison as a reference row, so a refinement that made
things worse is visible before the holdout is spent on it. If no iteration beat
the base, record that in the iteration log — it is a finding about the method,
not a failure of the run.

### The rules

1. **Stage A is blind.** Run all three designs before reading any error log.
2. **One change per stage-B turn.** The system message never moves, and only one
   wording change goes into each iteration, so a score difference has one cause.
3. **The evidence never moves.** It is extracted once per patch and cached, so
   every version reads one byte-identical string. Every record carries
   `evidence_sha256`, which makes that a check rather than an assumption:

   ```bash
   diff <(jq -r '"\(.patch) \(.evidence_sha256)"' ../results/llmjudge_dev_v2_*/records.jsonl | sort) \
        <(jq -r '"\(.patch) \(.evidence_sha256)"' ../results/llmjudge_dev_v2.1_*/records.jsonl | sort)
   ```

   Keep one `--cache_dir` across every dev pass, and do not pass
   `--refresh_context` between them. The renderer version catches a change to
   `evidence.py`; only this digest catches a changed Defects4J checkout.
4. **A version is frozen once it has been run on dev.** Its recorded score
   refers to its text. A later idea becomes a new iteration, never an edit.
   `register()` refuses to overwrite a registered name, and every record carries
   `prompt_sha256`, so a silent edit is detectable.
5. **Three stage-B turns maximum.** Stop earlier when an iteration gains less
   than two points of dev F1.
6. **Publish all six dev passes**, including the two losing designs and any
   iteration that regressed. The iteration log below is that record, and it is
   the evidence that the baseline was tuned honestly rather than tuned until it
   lost.
7. **The holdout is read once**, after the winner is frozen. `--side holdout`
   refuses to run without `--confirm_holdout`.

**Before the holdout run, write down:** the model, the effort setting, the
sample count, the vote rule, the parse-failure default, and the population.
Every one of them is already in `summary.json`, so the note is a copy, not a
new claim.

### Iteration log

Fill each row in from that run's `summary.json`. An empty row means the pass was
never made, which is itself a fact a reader needs.

**Stage A — blind bake-off**

| Design | Dev P | Dev R | Dev F1 | FP | FN | Parse failures | Agreement | Run directory |
|---|---|---|---|---|---|---|---|---|
| v1 | | | | | | | | |
| v2 | | | | | | | | |
| v3 | | | | | | | | |

Stage-A winner: _(version, and its F1)_

**Stage B — refinement of the winner**

| Iteration | Error class it repaired | Dev P | Dev R | Dev F1 | FP | FN | Run directory |
|---|---|---|---|---|---|---|---|
| `<winner>.1` | | | | | | | |
| `<winner>.2` | | | | | | | |
| `<winner>.3` | | | | | | | |

Frozen version: _(the best of the three iterations, and its F1 against the
stage-A winner's)_

**Holdout — one pass**

| Version | P | R | F1 | FP | FN | Paired vs pipeline | McNemar p | Run directory |
|---|---|---|---|---|---|---|---|---|
| | | | | | | | | |

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

Measured on `gpt-5.4` with an early draft, whose instruction was about 830
characters — between `v1` (206) and `v2` (1,746), so these figures sit in the
middle of the three designs' range. One call on `patch1-Lang-27-DeepRepair`:
**3,686 prompt and 1,792 completion tokens.** Per call on the five Chart-9
patches: about **7,060 prompt with 850–1,270 completion tokens.** Evidence size
varies by bug, so treat 4k–8k prompt tokens per call as the working range.

| Stage | Passes | Calls | Prompt tokens | Completion tokens |
|---|---|---|---|---|
| Stage A — three blind designs | 3 | 465 | 1.9M – 3.7M | ~0.84M |
| Stage B — three refinement turns | 3 | 465 | 1.9M – 3.7M | ~0.84M |
| Holdout — one pass | 1 | 255 | 1.0M – 2.0M | ~0.46M |
| **total** | **7** | **1,185** | **4.8M – 9.4M** | **~2.1M** |

One dev pass is 31 patches at 5 samples, so 155 calls. Six dev passes plus one
holdout pass is 1,185 calls before any retry.

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

Report the three spends separately: stage A, stage B, and the holdout. State
that two stage-A designs and up to two stage-B iterations were discarded. The
baseline gets six dev passes before its one holdout pass, and the accounting
must say so rather than quoting the holdout pass alone.

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
| [`prompts.py`](prompts.py) | The three stage-A designs, and the stage-B iterations |
| [`verdict.py`](verdict.py) | Parse one verdict; combine samples under the three vote rules |
| [`budget.py`](budget.py) | Tokens to cost, with the price and cache rules above |
| [`errors.py`](errors.py) | Print one dev pass's errors — the input to the next stage-B iteration |
| [`compare.py`](compare.py) | Compare dev runs and name the stage winner |
| [`run_one.py`](run_one.py) | One patch, N samples, one record |
| [`evaluate.py`](evaluate.py) | One side of the split: records, summary, budget, head-to-head |

Guards live in [`tests/test_llmjudge_baseline.py`](../../tests/test_llmjudge_baseline.py).

## 11. Usage

Run from `src/`.

```bash
# print the dev queue and stop, before any model call
uv run -m baseline_llmjudge.evaluate --side dev --dry_run

# STAGE A — the three designs, blind, in any order
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v1
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v2
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v3
uv run -m baseline_llmjudge.compare  --stage A

# STAGE B — read the winner's errors, write the next iteration, run it
uv run -m baseline_llmjudge.errors \
  --records ../results/llmjudge_dev_v2_<timestamp>/records.jsonl
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v2.1
uv run -m baseline_llmjudge.compare  --stage B --base v2

# one patch, to reproduce a single error by hand
uv run -m baseline_llmjudge.run_one -o \
  --patch_file ../drr/Patches/Doverfitting/DeepRepair/Lang/patch1-Lang-27-DeepRepair.patch \
  --prompt_version v2 --samples 5

# the holdout, once, with the frozen winner and a paired comparison
uv run -m baseline_llmjudge.evaluate --side holdout --prompt_version v2.2 \
  --confirm_holdout \
  --tool_records ../results/eval_holdout_<timestamp>/records.jsonl
```

| Option | Meaning |
|---|---|
| `--side dev\|holdout` | Which side of the frozen split to score |
| `--prompt_version` | A stage-A design (`v1`, `v2`, `v3`) or a stage-B iteration (`v2.1`) |
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
