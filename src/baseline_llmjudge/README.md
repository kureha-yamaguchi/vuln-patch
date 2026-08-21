# `baseline_llmjudge` — the one-shot LLM baseline

A strong comparison target for the harness pipeline in [`src/java/`](../java).

The pipeline decides whether an automated-repair patch is complete by writing
Jazzer harnesses, running them against the patched build, and asking whether any
of them fires. This package answers the same question about the same patches
with the same model and the same evidence, in one shot, with no harness and no
execution of the patched code.

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

## Two datasets

Sections 1 to 10 describe the Defects4J baseline, over the drr patches. That is
the main experiment.

### Package layout

Three subpackages, and the split is the dataset.

```
baseline_llmjudge/
├── shared/         what both datasets use — see the table below
├── defects4j/      the Defects4J baseline, over the drr patches (sections 1-10)
└── project_zero/   the Project Zero baseline (section 11)
```

A dataset subpackage imports from `shared/`. Neither dataset subpackage imports
from the other, so a change to one cannot move a number in the other.

**A module belongs in `shared/` when the two baselines must not be able to
disagree about it.** Six do.

| Module | What it fixes for both datasets |
|---|---|
| [`shared/verdict.py`](shared/verdict.py) | The output space: one bit, two classes, the parse rule, the three vote rules, and what an unparsed sample counts as |
| [`shared/scoring.py`](shared/scoring.py) | The confusion matrix, the headline rule, and the printed summary form |
| [`shared/budget.py`](shared/budget.py) | Tokens to cost, with the price and cache rules |
| [`shared/blocks.py`](shared/blocks.py) | The evidence block type, and the rule that joins blocks into one prompt string |
| [`shared/version.py`](shared/version.py) | The `PromptVersion` shape, and the output contract |
| [`shared/provenance.py`](shared/provenance.py) | Which commit produced a record |

**What deliberately stays out of `shared/`.** Each dataset keeps its own
`SYSTEM` message, its own prompt registry and its own `version_sha256`. That is
not tidiness. `version_sha256` digests `SYSTEM` together with the version's own
wording, so one shared `SYSTEM` would tie every recorded digest of one dataset
to an edit made for the other. `CONTRACT` is shared and safe to share, because
the digest covers its VALUE and a move between modules does not change that.

Each dataset also keeps its own `summarise`. The two differ in what they group
by and in which registry they read, so one shared function would need a flag
per difference.

**One duplication remains, and it is known.** The two prompt modules each carry
their own iteration-name grammar, `register` and `resolve`, about 40 lines
apiece. A shared registry would need `SYSTEM` injected into it, so it would put
the digest machinery back into one place. The duplication is the safer trade
today.

[Section 11](#11-the-project-zero-dataset) describes a second baseline, over the
Project Zero variant-pair dataset. It answers the same question about real
upstream security fixes in C and C++. It shares the output space, the vote rules
and the confusion matrix. It shares no population, no prompt and no split. Its
population is 21 fixes, so it is a pilot and not a result.

## Two pools

The dataset has two bug kinds, and the baseline covers both. A bug kind is the
coarse split of how a bug reports itself:

- A **crashing bug** is reported at run time by a throwable. Its trigger test
  fails because an exception escapes.
- A **semantic bug** is reported by nothing. Its trigger test fails a JUnit
  assertion, because the code returns a wrong value.

The question, the output space, the vote rule and the protocol are the same for
both. Four things differ, and each one is a separate frozen artifact:

| Per pool | Crashing | Semantic |
|---|---|---|
| Frozen split | `suites/splits/crashing_split.jsonl` | `suites/splits/semantic_split.jsonl` |
| Certification files | `suites/labels/crashing/` | `suites/labels/` |
| Evidence renderer | [`evidence.py`](defects4j/evidence.py) | [`evidence_semantic.py`](defects4j/evidence_semantic.py) |
| Stage-A designs | `v1`, `v2`, `v3` | `s1`, `s2`, `s3` |

`--kind crashing|semantic` selects the pool, and it defaults to `crashing`. The
version name says which pool a run belongs to, and `summary.json` records it as
`bug_kind`. A design of one pool is refused against the other pool's split,
because that would report a population change as a wording result.

---

## 1. Same population

The queue comes from [`build_split_queue.py`](../java/dataset/build_split_queue.py),
run as a subprocess. That script crosses the frozen split with the certification
files and emits one `-c` or `-o` line per patch. The pipeline's own evaluator
builds its queue with the same script, so a second implementation cannot let the
two populations drift apart. `--kind` picks which pool it reads, and the pool's
`kind` field also filters the certification rows: the semantic label files carry
a few crashing and unknown rows, so that filter is not cosmetic.

**Crashing pool.**

| Side | Bugs | Patches | Overfitting | Correct | Positive prior |
|---|---|---|---|---|---|
| dev | 8 | 31 | 15 | 16 | 0.48 |
| holdout | 10 | 51 | 14 | 37 | 0.27 |

**Semantic pool.**

| Side | Bugs | Patches | Overfitting | Correct | Positive prior |
|---|---|---|---|---|---|
| dev | 43 | 110 | 39 | 71 | 0.35 |
| holdout | 27 | 69 | 23 | 46 | 0.33 |

`verified_correct.jsonl` means "the drr label was audited and accepted". It holds both classes.

**One patch, one queue line.** `verified_correct.jsonl` holds 210 semantic rows
but only 180 distinct patches: 30 rows repeat a `(project, bug, tool, patch)`
already listed. The builder collapses a repeat and prints how many it dropped,
because a repeated row would put one patch in the queue twice — counted twice in
the confusion matrix, and paid for twice. The crashing file has no repeats, so
this changes nothing for that pool.

**The split file's own leg counts are inflated, and the split itself is not.**
`semantic_split.jsonl` records 92 correct dev legs and 56 correct holdout legs.
The queue finds 71 and 46. The gap has two causes and no third: 21 dev and 8
holdout repeats collapsed, and Math-63 lost both of its correct legs to
`verified_incorrect.jsonl` after the split froze. The side assignment is per
bug, so the holdout is still exactly the 27 bugs it was frozen with. Only the
leg counts in the split rows, and the table in `suites/splits/README.md`, are
stale.

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

Each pool has its own renderer, because the pipeline has its own prompt branch
per pool. The two renderers share three things: the `Block` type, the rule that
joins blocks into one string, and the renderer for the root-cause reachable set.
They do not share a renderer version. The context cache keys on that number, so one
shared constant would mean a semantic rendering change invalidated every cached
crashing entry, and every published crashing `evidence_sha256` would then point
at a cache that no longer exists.

### 2.1 The crashing renderer

The pipeline's harness prompt is the crashing branch of `PromptBuilder.build`
([`prompts.py`](../java/harness/prompts.py)). It joins ten kinds of section.
Five carry facts about the patch and the bug. Five teach Jazzer harness
authorship. [`evidence.py`](defects4j/evidence.py) rebuilds the factual five and drops
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

### 2.2 The semantic renderer

The pipeline's semantic prompt is `_build_semantic`, the branch `build` takes
when `bug_kind == "semantic"`. It joins up to eleven sections.
[`evidence_semantic.py`](defects4j/evidence_semantic.py) rebuilds the factual ones.

| Block | Pipeline section | Origin here |
|---|---|---|
| `patch` | `_patch_block` | reused verbatim |
| `source_imports` | `_imports_block` | reused verbatim |
| `touched_function:<name>` | `_function_block` | reused verbatim |
| `class_skeletons` | `_class_context_block` | re-rendered |
| `documented_contract` | `_preconditions_block` | re-rendered |
| `sibling_and_state` | `sibling_and_state_hints` | re-rendered |
| `trigger_tests` | `_lifted_assertion_block` | re-rendered |
| `root_cause_reachable` | `_variant_analysis_block` | shared with the crashing renderer |

Five blocks are re-rendered, each because the pipeline fuses facts with
instructions inside one section:

- `_class_context_block` wraps the class skeletons in a request to hunt for a
  cross-member consistency oracle. The skeletons stay, under a neutral heading.
- `_preconditions_block` lists the `@param` and `@throws` lines of the touched
  methods, then states the rejection-oracle ordering rule. The javadoc lines
  stay, under the same 900-character cap and the same tag selection. A guard
  test asserts that the two blocks keep the same lines, so a renderer cannot
  give the baseline one line more or one line fewer than the pipeline gets.
- `sibling_and_state_hints` lists the overload groups, the method families and
  the no-argument readers of the touched class. Each of its three headings ends
  with advice about the check to write. The lists stay, and each heading is
  trimmed at its first bracket.
- `_lifted_assertion_block` is mostly instruction: lift every assertion,
  reconstruct each call, throw on a mismatch. It also calls `_metamorphic_block`
  on its last line, so the method cannot be reused at all. The re-render keeps
  the same facts, from the same fields, in the same order: the reported wrong
  value, the public API class the test drives, the chosen test's body under the
  same 1500-character cap, that test's support source, and the names of the
  bug's other trigger tests.
- `_variant_analysis_block` is the same section on both branches, so both
  renderers call one function for it.

**The reported wrong value is the semantic counterpart of the throwable.** The
safety-net run already records what each trigger test printed on the buggy build
— for example `expected:<NaN> but was:<4.0>`. It names the observable that
diverges and the value the buggy code produced for it. A renderer that dropped
it would ask the baseline a weaker question than the pipeline asks.

Dropped outright: `_hard_constraints`, `_intro`, `_metamorphic_block`,
`_fdp_reference`, `_skeleton_block`. Each states a rule about the `.java` file
the model must emit.

**Two kinds of evidence are withheld for a different reason.** Each needs work
the baseline never does, so no wording could close the gap. Both are listed in
every record under `withheld_pipeline_evidence`:

- `_synthesized_relations_block`. A relation candidate comes from a separate
  model call, and only a candidate that survives a compile and a run on the
  buggy build ever reaches a prompt.
- The `--divcap` divergence facts. Collecting one needs the patched build.

They sit beside execution evidence on the list of things the pipeline observes
and the baseline does not. Report them there.

### 2.3 The audit trail

Every record carries a `parity_manifest`: each block's name, its origin, its
character count, the list of dropped sections, and — on the semantic side — the
list of withheld evidence. The parity claim is auditable from the artifact, not
from this document.

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
the safety-net gate, and the crash capture. On the semantic side the crash
capture is replaced by four local parses of the buggy sources, and the failure
message is read out of the safety-net run rather than paid for again. What the
baseline never does is build the patched code, compile a harness, or fuzz.
Section 7 accounts for both halves.

**Label leakage.** The ground truth sits in the patch path
(`drr/Patches/Dcorrect/…` against `…/Doverfitting/…`), and the certification
rows state the verdict outright. `tests/test_llmjudge_baseline.py` asserts that
no rendered evidence and no built prompt contains any of it. The label reaches
the selector only, because the selector needs it to find the file.

## 3. Same model

[`run_one.py`](defects4j/run_one.py) resolves the model as the pipeline does — `--model`
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
[`verdict.py`](shared/verdict.py) reads the last such line, because a scaffolded answer
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
second headline. `HEADLINE_RULE` in [`evaluate.py`](defects4j/evaluate.py) is the single
place that choice lives.

Each patch also reports `agreement`, the share of sample pairs that agree. That
is the baseline's own variance, and it is the fair counterpart to the pipeline's
run-to-run variance.

## 6. Iteration protocol

Two stages. Stage A is a blind bake-off between three independent designs, and
it is scored on dev. Stage B refines the stage-A winner three times, and it
selects on the holdout.

The two sides do different jobs in stage B, and that division is the protocol:

| Side | Its job | What it hands back |
|---|---|---|
| dev | Refinement. Each turn reads the previous iteration's dev errors. | The error text, in full |
| holdout | Selection. Every iteration is scored on it. | One F1 per iteration |

```
STAGE A — blind. No error log is read. Scored on dev.
    v1  ──run on dev──┐
    v2  ──run on dev──┼──→  best dev F1 wins        (say v2 wins)
    v3  ──run on dev──┘

STAGE B — refinement of the winner, three turns.

  refinement reads DEV only:
      v2   ──read v2's DEV errors──→   v2.1
      v2.1 ──read v2.1's DEV errors──→ v2.2
      v2.2 ──read v2.2's DEV errors──→ v2.3

  every iteration runs on BOTH sides:
      v2.1 ──dev──→ error log, read to write v2.2  ──holdout──→ F1
      v2.2 ──dev──→ error log, read to write v2.3  ──holdout──→ F1
      v2.3 ──dev──→ error log, published only      ──holdout──→ F1

  the highest of the three holdout F1 values is the reported version
```

Six dev passes, and four holdout passes: the three iterations, plus the
stage-A winner as a reference row.

### Why selection reads the holdout

A stage-B iteration is written from a dev error log. Its dev score is therefore
the score of a version tuned against the very patches it is then scored on, and
it climbs whether or not the new wording generalises. Selection on that number
would pick the iteration that best memorised the dev errors. The holdout score
carries no such loop, because nothing a holdout pass produces reaches any
prompt: `errors.py` refuses holdout records, and the selector reads one field
of `summary.json`, `headline.f1`.

**The bias that does remain.** The winning iteration's holdout F1 is a maximum
over three iterations, so it is optimistic. The size of that optimism is about
the spread between the three rows. Publish all four holdout rows, and never
quote the winner's F1 as an unbiased estimate. The honest sentence names the
population the number was selected from: "of three iterations refined on dev,
the best scored F1=0.71 on holdout; the three scored 0.71, 0.66 and 0.64."

### The refinement turn is a manual edit

**The dev error log never enters a model call.** A person reads it. That person
writes one new `PromptVersion` in [`prompts.py`](defects4j/prompts.py) by hand. About a
bug, the model still sees exactly what it saw before: the patch, the trigger
tests, and the reachable set.

That is structural, not a matter of discipline. Two facts make it so:

1. `errors.py` is imported by no module in this package. It is a reader for the
   operator, and it writes to a terminal.
2. `prompts.build_messages` takes a version name and the evidence text. There
   is no third parameter, so an error log has no way in:

   ```python
   def build_messages(name: str, evidence_text: str) -> List[Dict[str, str]]:
       v = resolve(name)
       return [
           {'role': 'system', 'content': SYSTEM},
           {'role': 'user',
            'content': '\n\n'.join([v.task, evidence_text, v.instruction])},
       ]
   ```

So the model sees four strings per call, and a turn moves one of them:

| Part | Source | Moves between iterations? |
|---|---|---|
| `SYSTEM` | A module constant | Never |
| `v.task` | The `PromptVersion` you write | Rarely |
| `evidence_text` | The patch, the trigger tests, the reachable set | Never — it is cached, and digested as `evidence_sha256` |
| `v.instruction` | The `PromptVersion` you write | This is the one thing a turn changes |

A turn is therefore an offline edit between two runs of the tool. Write the
child as a `.replace()` on its parent's instruction, so the single change is a
literal line in the source rather than a promise:

```python
register(PromptVersion(
    name='v2.1',
    hypothesis='v2 produced 7 FP and 2 FN on dev. It called a correct patch '
               'overfitting whenever the fix was narrower than the developer '
               'fix, so require the surviving input to be spelled out.',
    task=V2.task,
    instruction=V2.instruction.replace(
        'Answer OVERFITTING only when you can name a concrete input',
        'Answer OVERFITTING only when you WRITE OUT a concrete input'),
))
```

`hypothesis` is the audit trail. It names the dev error class the change
repairs. A `hypothesis` that cites a holdout error is itself the evidence of a
leak, so cite the dev log only.

### Stage A — the three designs

They are authored blind, before any run, and they differ in how much method the
request supplies. **The numbers are labels, not an order**, because the three
run independently.

Each pool has its own three, and the three bets are the same in both. So a
difference between the pools is a difference in the bugs, and not in the design
space that was searched.

| Crashing | Semantic | What the request supplies | The bet |
|---|---|---|---|
| `v1` | `s1` | The evidence, the question, a gloss of each class, the output contract | The floor: what the model does unaided |
| `v2` | `s2` | Definitions, the plausibility premise, a five-step method, two calibration rules | A method plus guard rails beats an unaided answer |
| `v3` | `s3` | Definitions, the plausibility premise, five **required** output sections | A form the answer must fill in beats a method it may skip |

On the crashing side a fourth draft was dropped before any run, because it was a
strict subset of `v2`.

**What the failure shape changes.** The two families share `SYSTEM`, `CONTRACT`
and `GROUND_RULES` byte for byte. The class definitions differ, because a
crashing bug's fault is a throwable and a semantic bug's fault is a wrong value.
So `v2` asks for an input that still reaches the throw site, and `s2` asks for an
input that still gets a wrong value out. Step 4's failure shapes differ for the
same reason: a swallowed throwable cannot be the shape of a semantic overfitting
patch, and a hard-coded expected value can.

Reading one design's errors before the others ran would give the later designs
information the earlier ones did not, so the bake-off would compare designs
plus unequal hindsight. `errors.py` prints a warning when it is pointed at a
stage-A run.

```bash
# the crashing pool
uv run -m baseline_llmjudge.defects4j.evaluate --side dev --prompt_version v1
uv run -m baseline_llmjudge.defects4j.evaluate --side dev --prompt_version v2
uv run -m baseline_llmjudge.defects4j.evaluate --side dev --prompt_version v3

uv run -m baseline_llmjudge.defects4j.compare --stage A     # names the winner, on dev F1

# the winner on the holdout, as stage B's reference row
uv run -m baseline_llmjudge.defects4j.evaluate --side holdout --prompt_version v2 \
    --confirm_holdout

# the semantic pool — the same three passes against the semantic split
uv run -m baseline_llmjudge.defects4j.evaluate --side dev --kind semantic \
    --prompt_version s1
uv run -m baseline_llmjudge.defects4j.evaluate --side dev --kind semantic \
    --prompt_version s2
uv run -m baseline_llmjudge.defects4j.evaluate --side dev --kind semantic \
    --prompt_version s3

uv run -m baseline_llmjudge.defects4j.compare --stage A --kind semantic
```

**Measure before you spend.** A semantic prompt is several times the size of a
crashing one, because the class skeletons and the test support come with it.
`--samples 0` extracts the evidence, caches it, prints its size, and calls no
model. Run it on a few patches of different projects, read the character counts,
then decide what a pass costs:

```bash
uv run -m baseline_llmjudge.defects4j.run_one -c \
  --patch_file ../drr/Patches/Dcorrect/ACS/Lang/patch1-Lang-7-ACS.patch \
  --prompt_version s1 --samples 0 \
  --cache_dir ../results/llmjudge_cache_semantic
```

### Stage B — three refinement turns

Each turn is three steps:

1. Read the previous iteration's **dev** errors.
2. Write the next iteration by hand, to repair the dominant error class.
3. Run it on dev, then run it on the holdout.

An iteration is named `<winner>.<n>`, so the lineage of every score is readable
from its name.

```bash
# say v2 wins
# turn 1 — read the stage-A winner's DEV errors, then write v2.1 in prompts.py
uv run -m baseline_llmjudge.defects4j.errors \
    --records ../results/llmjudge_dev_v2_<ts>/records.jsonl

# the dev pass feeds turn 2; the holdout pass is v2.1's selection score
uv run -m baseline_llmjudge.defects4j.evaluate --side dev     --prompt_version v2.1
uv run -m baseline_llmjudge.defects4j.evaluate --side holdout --prompt_version v2.1 \
    --confirm_holdout

# turns 2 and 3 — same shape, reading the previous turn's DEV records
uv run -m baseline_llmjudge.defects4j.errors \
    --records ../results/llmjudge_dev_v2.1_<ts>/records.jsonl
```

Point `errors.py` at a dev run. Pointed at a holdout run it exits 2, because a
holdout error log read here would turn the selection side into a second
refinement side.

`errors.py` prints the false-positive and false-negative counts, names the
dominant class, then prints each error with the model's own reasoning. The two
classes need opposite repairs:

| Dominant class | What it means | What the next iteration should do |
|---|---|---|
| FP | A correct patch was called overfitting | Raise the bar for the positive class: demand a named surviving input, forbid style objections |
| FN | An overfitting patch was called correct | Name the patterns being missed: guard on the reported value, swallowed error, unfixed sibling path, off-by-one bound |

### The driver scripts, and where a stage's logs land

Each stage has a driver script. The script runs the passes of that stage in
order. It keeps every log of the stage in one folder under `results/`.

```bash
scripts/llmjudge_stage_a.sh                 # the crashing pool
KIND=semantic scripts/llmjudge_stage_a.sh   # the semantic pool
scripts/llmjudge_stage_b.sh v1       # the base's holdout reference row
scripts/llmjudge_stage_b.sh v1.1     # turn 1: dev, then holdout
scripts/llmjudge_stage_b.sh s1.1     # the semantic pool, same shape
scripts/llmjudge_stage_b.sh -h       # the options, and the folder layout
```

`KIND` picks the pool for stage A. Stage B takes no such variable: it reads the
pool out of the version name, through `prompts.kind_of`. Neither script holds a
second copy of a pool's design list, so a list cannot drift out of step with
`prompts.py`.

| Stage | Folder | What the folder holds |
|---|---|---|
| A | `results/llmjudge_stageA_<kind>_<ts>/` | `stage_a.log`, one `evaluate_<version>.log` per design, `compare_A.log`, `run_dirs.txt` |
| B | `results/llmjudge_stageB_<base>_<ts>/` | `stage_b.log`, `prereg.md`, one `evaluate_<version>_<side>.log` per pass, `compare_B.log`, `run_dirs.txt` |

Stage B keeps one folder per base. Turn 1 creates the folder. Each later turn
appends to the same folder, so the whole stage stays one record.

The scored artifacts stay where `evaluate.py` writes them, under
`results/llmjudge_<side>_<version>_<ts>/`. `compare.py` finds a run by that
name. The stage folder holds the logs, plus `run_dirs.txt` as the index into
the run directories.

The stage-B script enforces three rules of the protocol:

1. No holdout pass runs until the pre-registration note exists.
2. `compare.py` runs only after every registered iteration has a holdout pass.
   Rule 6 below is three turns, with no early stop.
3. Every pass reads one evidence cache, and no pass sets `--refresh_context`.

### Selection

```bash
uv run -m baseline_llmjudge.defects4j.compare --stage B --base v2
```

| Stage | Selects on | Tie break |
|---|---|---|
| A | Highest **dev** F1 | Fewer false positives |
| B | Highest **holdout** F1 | Fewer false positives |

`compare --stage B` reads one pool, and `--kind` names it. It prints each
iteration's dev F1 beside its holdout row. The
dev column is there for the record, and it does not select. The stage-A
winner's own holdout row appears as a reference, so a refinement that made
things worse on holdout is visible rather than inferred. If no iteration beat
the base, record that in the iteration log. It is a finding about the method,
not a failure of the run.

### The rules

1. **Stage A is blind.** Run all three designs before reading any error log.
2. **Refinement reads dev only.** Every sentence that enters a prompt comes
   from a dev error log. The holdout contributes numbers, never text.
3. **One change per stage-B turn.** The system message never moves, and only
   one wording change goes into each iteration, so a score difference has one
   cause.
4. **The evidence never moves.** It is extracted once per patch and cached, so
   every version reads one byte-identical string. Every record carries
   `evidence_sha256`, which makes that a check rather than an assumption:

   ```bash
   diff <(jq -r '"\(.patch) \(.evidence_sha256)"' ../results/llmjudge_dev_v2_*/records.jsonl | sort) \
        <(jq -r '"\(.patch) \(.evidence_sha256)"' ../results/llmjudge_dev_v2.1_*/records.jsonl | sort)
   ```

   Keep one `--cache_dir` across every pass on both sides, and do not pass
   `--refresh_context` between them. The renderer version catches a change to
   the renderer; only this digest catches a changed Defects4J checkout. Each
   pool has its own cache directory, and a cache entry records the pool it was
   rendered for, so a crashing rendering can never answer a semantic request.
5. **A version is frozen once it has been run on either side.** Its recorded
   score refers to its text. A later idea becomes a new iteration, never an
   edit. `register()` refuses to overwrite a registered name, and every record
   carries `prompt_sha256`, so a silent edit is detectable.
6. **Three stage-B turns, and all three are run.** There is no early stop.
   Selection compares three holdout rows, so a turn not taken is a candidate
   missing from the comparison. An iteration that gains nothing on dev is still
   written, still run on both sides, and still published.
7. **Publish every pass**, including the two losing stage-A designs, every
   iteration that regressed, and all four holdout rows. The iteration log below
   is that record, and it is the evidence that the baseline was tuned honestly
   rather than tuned until it lost.
8. **The holdout is scored, never read.** Do not open a holdout
   `records.jsonl` by hand either. The rule is about what reaches the next
   prompt, and a person reading it breaks the rule exactly as a script would.

**Before the first holdout pass, write down:** the model, the effort setting,
the sample count, the vote rule, the parse-failure default, and the population.
Every one of them is already in `summary.json`, so the note is a copy, not a
new claim.

Write the note to `results/llmjudge_stageB_<base>_prereg.md`.
`scripts/llmjudge_stage_b.sh` copies it into the stage folder as `prereg.md`,
and it refuses to run any pass without it.

### Iteration log — crashing pool

Fill each row in from that run's `summary.json`. An empty row means the pass was
never made, which is itself a fact a reader needs.

**Stage A — blind bake-off, scored on dev**

| Design | Dev P | Dev R | Dev F1 | FP | FN | Parse failures | Agreement | Run directory |
|---|---|---|---|---|---|---|---|---|
| v1 | 0.650 | 0.867 | **0.743** | 7 | 2 | 0 | 0.981 | `llmjudge_dev_v1_20260818_140450` |
| v2 | 0.571 | 0.533 | 0.552 | 6 | 7 | 0 | 0.897 | `llmjudge_dev_v2_20260818_145011` |
| v3 | 0.643 | 0.600 | 0.621 | 5 | 6 | 0 | 0.890 | `llmjudge_dev_v3_20260818_154305` |

Stage-A winner: `v1`, dev F1 = 0.743. The floor design won. Neither the method
of `v2` nor the required sections of `v3` beat an unaided answer on dev.

**Stage B — refinement on dev**

| Iteration | Dev error class it repaired | Dev P | Dev R | Dev F1 | FP | FN | Run directory |
|---|---|---|---|---|---|---|---|
| `v1` (base) | — | 0.650 | 0.867 | 0.743 | 7 | 2 | `llmjudge_dev_v1_20260818_140450` |
| `v1.1` | FP. 6 of 7 FP were Lang-6 patches rejected for a collateral reason — wrong index base, "no human would write this" — and none named an input reaching the reported fault. | 0.875 | 0.467 | 0.609 | 1 | 8 | `llmjudge_dev_v1.1_20260818_205505` |
| `v1.2` | FN. All 8 FN quoted `v1.1`'s own bar back at it: "per the criterion you gave, I cannot name such an input". The excused patches only disabled the failing path — always-true disjunct (Lang-51), always-false type comparison (Math-32), deleted loop (Lang-39). | 0.611 | 0.733 | 0.667 | 7 | 4 | `llmjudge_dev_v1.2_20260818_214249` |
| `v1.3` | FP. 6 of 7 FP were Lang-6 again. Each did name an input, but it produced a skipped character, not the reported `StringIndexOutOfBoundsException`. | 0.733 | 0.733 | 0.733 | 4 | 4 | `llmjudge_dev_v1.3_20260818_222209` |

No iteration beat the base on dev either. The dev column does not select, so
this is context, not the result.

**Stage B — selection on holdout**

| Version | P | R | F1 | FP | FN | Run directory |
|---|---|---|---|---|---|---|
| `v1` (reference) | 0.923 | 0.857 | **0.889** | 1 | 2 | `llmjudge_holdout_v1_20260818_205502` |
| `v1.1` | 0.615 | 0.571 | 0.593 | 5 | 6 | `llmjudge_holdout_v1.1_20260818_214152` |
| `v1.2` | 0.857 | 0.857 | 0.857 | 2 | 2 | `llmjudge_holdout_v1.2_20260818_220631` |
| `v1.3` | 0.591 | 0.929 | 0.722 | 9 | 1 | `llmjudge_holdout_v1.3_20260818_222223` |

Parse failures: 0 in every pass on both sides. Agreement on holdout: 0.925,
0.878, 0.898, 0.859 in row order.

Every log behind the two stage-B tables is in
`results/llmjudge_stageB_v1_20260818_205455/`: the pre-registration note, one
`evaluate_<version>_<side>.log` per pass, `compare_B.log`, and `run_dirs.txt`
as the index into the seven run directories. Stage A's own logs are in
`results/llmjudge_stageA_20260818_140449/`. Those passes ran by hand, before
`scripts/llmjudge_stage_b.sh` existed, so the folder carries no `stage_b.log`
and `run_dirs.txt` records no exit code.

Selected version: **`v1.2`, holdout F1 = 0.857**, the best of the three
iterations. It is 0.032 **below** the stage-A winner's 0.889. The three
iterations scored 0.857, 0.722 and 0.593, so the spread is 0.264 — wider than
the gap between the winner and the base.

**The finding: no refinement beat the base.** Three hand-written turns, each
repairing the dominant dev error class of the version before it, all landed
below `v1` on the selection side. Two readings are consistent with these rows,
and the run cannot separate them:

1. `v1`'s holdout score is itself a single draw, and 0.889 against 0.857 is
   within the noise of 51 patches at 14 positives.
2. Each turn traded one error class for the other rather than removing either.
   `v1.1` cut dev FP from 7 to 1 and raised FN from 2 to 8. `v1.3` cut dev FP to
   4 but pushed holdout FP to 9, its recall to 0.929, and its precision to
   0.591. The see-saw is visible in every row.

The dev FP class never yielded. Six of the seven dev FP are `Lang-6` patches in
all of `v1`, `v1.2` and `v1.3`. Ground truth calls them correct because the
crash is gone. The model calls them overfitting because the patched traversal is
wrong elsewhere. That disagreement is about what "the same fault" means, and
three wordings did not settle it.

Because selection returned no improvement, the honest sentence is: *of three
iterations refined on dev, the best scored F1=0.857 on holdout, below the
unrefined `v1` at 0.889; the three scored 0.857, 0.722 and 0.593.* Report `v1`
as the baseline, and report `v1.2` as the best refinement of it.

**Stage-B spend.** 8 passes, 1,640 calls, 4,093,050 prompt tokens and 1,602,298
completion tokens, at a cache hit rate of 0.67–0.73. Split by job: the stage-A
winner's holdout reference pass, 255 calls; stage B on dev, 465 calls over three
iterations; stage B on holdout, 765 calls over the same three; and `v1`'s dev
pass, 155 calls, which stage A already paid for. Two stage-A designs and two
stage-B iterations were discarded, and every one of them is published above.
No price was set, so no cost figure is claimed.


### Iteration log — semantic pool

Fill each row in from that run's `summary.json`. An empty row means the pass was
never made, which is itself a fact a reader needs.

**Stage A — blind bake-off, scored on dev**

| Design | Dev P | Dev R | Dev F1 | FP | FN | Parse failures | Agreement | Run directory |
|---|---|---|---|---|---|---|---|---|
| s1 | 0.596 | 0.872 | 0.708 | 23 | 5 | 0 | 0.953 | `llmjudge_dev_s1_20260819_161219` |
| s2 | 0.620 | 0.795 | 0.697 | 19 | 8 | 0 | 0.918 | `llmjudge_dev_s2_20260819_195739` |
| s3 | 0.630 | 0.872 | **0.731** | 20 | 5 | 0 | 0.885 | `llmjudge_dev_s3_20260819_222336` |

Stage-A winner: `s3`, dev F1 = 0.731. The required-sections form won. All three
designs sit inside 0.034 of F1, so the bake-off separated them weakly.

**The three designs share one error profile.** Every one of them produced far
more false positives than false negatives: 23 against 5, 19 against 8, and 20
against 5. So on semantic bugs the unrefined baseline over-predicts the positive
class, whatever method the request supplies. That is the opposite of a spread
between designs, and it is what stage B has to attack.

Every log is in `results/llmjudge_stageA_semantic_20260819_161219/`.

**Stage B — refinement on dev**

| Iteration | Dev error class it repaired | Dev P | Dev R | Dev F1 | FP | FN | Run directory |
|---|---|---|---|---|---|---|---|
| `s3` (base) | — | 0.630 | 0.872 | 0.731 | 20 | 5 | `llmjudge_dev_s3_20260819_222336` |
| `s3.1` | FP. All 20 FP named a real residual imperfection in or near the patched method, and none named the REPORTED fault. Math-59 cited signed-zero semantics against a reported reversed result; Closure-62 cited an extra caret the patch itself adds; Math-2 cited a second arithmetic issue. Nine of the 20 are the Math-59 and Math-30 clusters, all unanimous. | 0.629 | 0.564 | 0.595 | 13 | 17 | `llmjudge_dev_s3.1_20260820_151848` |
| `s3.2` | FN. `s3.1` cut FP to 13 but raised FN to 17, and recall fell from 0.872 to 0.564. Three wording faults in `s3.1` caused it: SIBLING PATHS asked a sibling to produce the reported fault, which no sibling can do (Math-53, Lang-41, Chart-12, Chart-3, Lang-60); "at the same computed step" was tighter than an overfitting patch has to be; and the CORRECT rule had no counterweight, so a patch keyed on the reported symptom passed unanimously (Math-82 twice, Math-73 twice, Closure-38, Chart-26). | 0.595 | 0.641 | 0.617 | 17 | 14 | `llmjudge_dev_s3.2_20260820_205114` |
| `s3.3` | FP, and the two-turn trend. Dev precision reads 0.630, 0.629 and 0.595 across `s3`, `s3.1` and `s3.2`, so the added sections bought no precision and cost recall twice. `PATCH SCOPE` handed the whole Math-59 cluster back: all six returned as FP, because the model reads "the patch repairs only the `a > b` branch" as keying on the reported input. Nine of the 17 FP are Math-59 and Math-30, and each names a wrong value of a different kind from the reported one. So `s3.3` is the `s3` form plus one paragraph, and its parent is `s3`. | 0.614 | 0.692 | 0.651 | 17 | 12 | `llmjudge_dev_s3.3_20260821_014541` |

**Stage B — selection on holdout**

| Version | P | R | F1 | FP | FN | Run directory |
|---|---|---|---|---|---|---|
| `s3` (reference) | 0.778 | 0.913 | 0.840 | 6 | 2 | `llmjudge_holdout_s3_20260820_125315` |
| `s3.1` | 0.778 | 0.609 | 0.683 | 4 | 9 | `llmjudge_holdout_s3.1_20260820_184703` |
| `s3.2` | 0.750 | 0.783 | 0.766 | 6 | 5 | `llmjudge_holdout_s3.2_20260821_001525` |
| `s3.3` | 0.773 | 0.739 | 0.756 | 5 | 6 | `llmjudge_holdout_s3.3_20260821_050539` |

Selected iteration: `s3.2`, holdout F1 = 0.766. **No iteration beat the base.**
`s3` scored 0.840 on the holdout, and the best iteration scored 0.074 below it.
The honest sentence names the population the number was selected from: of three
iterations refined on the dev side, the best scored F1 = 0.766 on the holdout;
the three scored 0.766, 0.756 and 0.683. That maximum over three is optimistic,
and the 0.083 spread between the three rows is the size of the optimism.

**The two sides agree on the ranking.** Dev orders the iterations 0.651, 0.617,
0.595 and the holdout orders them 0.756, 0.766, 0.683. `s3.2` and `s3.3` swap,
by 0.010 on the holdout and 0.034 on dev, and `s3.1` is last on both. So the dev
signal that drove each turn was not noise, and it still did not produce a
holdout gain.

**Every turn traded the same way.** Recall fell in all three iterations, from
the base's 0.872 to 0.564, 0.641 and 0.692 on dev, while precision never rose
above the base's 0.630. Each turn added a reason to answer CORRECT, each one
spent recall, and none bought precision. The base's high-recall behaviour is
what carries its F1 at this class prior.

**The false-positive core does not move.** Math-59 six times and Math-30 three
times are 9 of the 20 base FP, and the same nine survive every wording. `s3.1`
excluded a wrong value of a different kind and broke their unanimity without
removing them. `s3.2` handed all six Math-59 back through `PATCH SCOPE`. `s3.3`
targeted them with one sentence and changed nothing: its dev FP set is still
Math-59 six times and Math-30 three times.

In those nine cases the model names a real residual defect in the patched code,
and the certification calls the patch correct. `FastMath.max(+0.0f, -0.0f)` does
return the wrong zero after the patch, and `n1 * n2` does still overflow at
n = 50000. The disagreement is about where one patch's responsibility ends, and
not about how carefully the model read the evidence. A prompt cannot settle it,
because the prompt is not what the two parties disagree about. That is the
finding of this stage, and it is a result about the method rather than a failed
run.

Parse failures: 0 in every pass on both sides. Agreement on holdout: 0.956,
0.929, 0.935, 0.959 in row order.

**Both pools reached the same answer.** On the crashing side the base `v1`
scored 0.889 on holdout and the best iteration scored 0.857. On the semantic
side the base `s3` scored 0.840 and the best iteration scored 0.766. Six
refinement turns across two pools, and not one of them beat the design it
refined. The stage-A form is what the baseline is, and hand refinement against
a dev error log did not improve it.

## 7. Budget disclosure

Token usage comes from the shared recorder in [`llm.py`](../llm.py), the same
one the pipeline reports. [`budget.py`](shared/budget.py) turns it into a cost, under
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
| Stage A — three blind designs, on dev | 3 | 465 | 1.9M – 3.7M | ~0.84M |
| Stage A winner — the holdout reference row | 1 | 255 | 1.0M – 2.0M | ~0.46M |
| Stage B — three refinement turns, on dev | 3 | 465 | 1.9M – 3.7M | ~0.84M |
| Stage B — the same three iterations, on holdout | 3 | 765 | 3.1M – 6.1M | ~1.38M |
| **total** | **10** | **1,950** | **7.9M – 15.5M** | **~3.5M** |

One dev pass is 31 patches at 5 samples, so 155 calls. One holdout pass is 51
patches, so 255 calls. Six dev passes and four holdout passes is 1,950 calls
before any retry.

### What a semantic pass costs

A semantic prompt is far larger than a crashing one. These are measured
character counts, from `--samples 0` on five patches of five projects. No model
was called for them, so they cost nothing.

| Pool | Evidence, min | Evidence, median | Evidence, max |
|---|---|---|---|
| crashing (82 cached patches) | 2,979 | 9,370 | 21,135 |
| semantic (5 probed patches) | 23,273 | 45,424 | 69,357 |

The class skeletons are 69 to 75 percent of every semantic total: 16,870 chars
on Math-35, up to 48,167 on Chart-19. Nothing else comes close. The trigger-test
block is 1,551 to 2,879 chars, and the sibling-and-state block is 183 to 1,400.

Two single-sample calls on `gpt-5.4` pin the character-to-token rate at 4.1 to
4.4, so the counts above convert directly:

| Probe | Prompt chars | Prompt tokens | Completion tokens |
|---|---|---|---|
| Math-35, the smallest probed | 24,019 | 5,798 | 614 |
| Chart-19, the largest probed | 64,728 | 14,723 | 659 |

So the working range is about **6,000 to 15,000 prompt tokens per semantic
call**, against 4,000 to 8,000 for a crashing one, with a median near 11,000.
Completion is small and flat, near 650 tokens.

One semantic dev pass is 110 patches at 5 samples, so 550 calls. One semantic
holdout pass is 69 patches, so 345 calls. Stage A on dev is three passes, so
1,650 calls, and roughly **10M to 25M prompt tokens before the cache discount**.

**Wall clock.** One semantic call measured 13 seconds end to end on the largest
probed patch, and that figure includes the `uv` start-up. The crashing stage A
took 2 h 22 m for 465 calls. So expect stage A on the semantic dev side to take
roughly 6 to 8 hours: about 5 hours of model calls, plus about 2 hours of
Defects4J checkouts and safety-net runs during the first design's pass. The
second and third designs read the evidence cache, so they pay no extraction.

Two things pull the invoice down, and both are already reported:

1. The five samples of one patch send a byte-identical prompt, so the provider
   serves most of that input from its prompt cache. The crashing runs measured a
   hit rate of 0.67 to 0.73.
2. Patches of one bug share every block except the diff, and the semantic pool
   averages 2.6 patches per bug on dev. The class skeletons — the expensive
   three quarters — are identical across them.

`cache_hit_rate` is in every summary, so quote the cached figure and the
full-rate upper bound together, exactly as the crashing rows do.

**If that is more than the budget allows**, cut the population rather than the
evidence. `--projects "Lang Math"` scores fewer projects and leaves the parity
claim intact. Capping the class skeletons instead would break per-prompt parity
with the pipeline's own consistency slot, and the manifest would have to say so.

**What the selection rule costs.** Selecting on the holdout needs four holdout
passes where selecting on dev would need one. The difference is three passes,
765 calls, and roughly 3.1M–6.1M prompt tokens. That is the price of removing
the tuned-and-scored-on-the-same-side loop, and it belongs in the accounting
rather than in a footnote.

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

Report four spends separately: stage A on dev, the stage-A winner's holdout
reference pass, stage B on dev, and stage B on holdout. State that two stage-A
designs and two stage-B iterations were discarded. The baseline gets six dev
passes and four holdout passes, and the accounting must say so rather than
quoting the selected iteration's holdout pass alone.

## 8. Threats to validity

- **Contamination.** Defects4J and drr are public, and the developer fixes are
  in the training data of any recent model. This helps the baseline, not the
  pipeline, so it works against the claim — the safe direction. Probe it
  separately: ask the model to name the bug and the fix, and report the recall
  rate.
- **The trigger test is a strong hint.** Its failure message often names the
  root cause. On a semantic bug it names the wrong value outright. Both sides
  see it, so parity holds, but the baseline may exploit it more directly.
- **The two pools are two experiments.** Each has its own frozen split, its own
  designs and its own iteration log. A crashing number and a semantic number are
  never pooled into one figure, and neither is evidence about the other.
- **The semantic evidence is dominated by one block.** The class skeletons are
  about three quarters of every semantic prompt. So a semantic result is partly a
  result about how well the model reads a long context, and the crashing result
  is not.
- **Two kinds of semantic evidence are withheld from the baseline.** The
  screened relations and the divergence facts need a compile, a run, or the
  patched build. That widens the honest asymmetry on the semantic side relative
  to the crashing one, and it widens it in the pipeline's favour.
- **Five samples is a small sample count.** The agreement figure carries a wide
  interval at n=5.

## 9. Files

| File | Responsibility |
|---|---|
| [`defects4j/context.py`](defects4j/context.py) | Extract the evidence for one patch, by the steps of its pool; cache it as rendered text |
| [`defects4j/evidence.py`](defects4j/evidence.py) | Render the crashing evidence blocks; build the parity manifest |
| [`defects4j/evidence_semantic.py`](defects4j/evidence_semantic.py) | Render the semantic evidence blocks; list the withheld evidence |
| [`defects4j/prompts.py`](defects4j/prompts.py) | Each pool's three stage-A designs, and the hand-written stage-B iterations. Builds the messages: system, task, evidence, instruction |
| [`defects4j/errors.py`](defects4j/errors.py) | Print one dev pass's errors — the input to the next stage-B iteration. Refuses holdout records |
| [`defects4j/compare.py`](defects4j/compare.py) | Name the stage winner of one pool: stage A on dev F1, stage B on holdout F1 |
| [`defects4j/run_one.py`](defects4j/run_one.py) | One patch, N samples, one record. `--samples 0` extracts and stops |
| [`defects4j/evaluate.py`](defects4j/evaluate.py) | One side of one pool's split: records, summary, budget |
| [`build_split_queue.py`](../java/dataset/build_split_queue.py) | The queue for one side of one pool, from the frozen split and the labels |
| [`llmjudge_stage_a.sh`](../../scripts/llmjudge_stage_a.sh) | Run stage A: the pool's three designs on dev, then the comparison. Logs land in `results/llmjudge_stageA_<kind>_<ts>/` |
| [`llmjudge_stage_b.sh`](../../scripts/llmjudge_stage_b.sh) | Run one stage-B turn: dev, then holdout, then the comparison once all three turns are in. Logs land in `results/llmjudge_stageB_<base>_<ts>/` |

The shared modules are listed under **Package layout** near the top of this
file. Guards live in
[`tests/test_llmjudge_baseline.py`](../../tests/test_llmjudge_baseline.py).

## 10. Usage

Run from `src/`. Section 6 carries the protocol's command sequence in order.
These are the entry points and their options. The two driver scripts of section 6 —
`scripts/llmjudge_stage_a.sh` and `scripts/llmjudge_stage_b.sh` — wrap these
commands, and they keep the logs of one stage in one folder. Run a script from
the repository root.

```bash
# print a queue and stop, before any model call
uv run -m baseline_llmjudge.defects4j.evaluate --side dev --dry_run
uv run -m baseline_llmjudge.defects4j.evaluate --side dev --kind semantic --dry_run

# one side of one pool, one prompt version
uv run -m baseline_llmjudge.defects4j.evaluate --side dev --prompt_version v1
uv run -m baseline_llmjudge.defects4j.evaluate --side holdout --prompt_version v2.1 \
  --confirm_holdout
uv run -m baseline_llmjudge.defects4j.evaluate --side dev --kind semantic \
  --prompt_version s1

# a dev pass's errors — the input to the next hand-written iteration
uv run -m baseline_llmjudge.defects4j.errors \
  --records ../results/llmjudge_dev_v2_<timestamp>/records.jsonl

# name the stage winner: stage A on dev F1, stage B on holdout F1
uv run -m baseline_llmjudge.defects4j.compare --stage A
uv run -m baseline_llmjudge.defects4j.compare --stage A --kind semantic
uv run -m baseline_llmjudge.defects4j.compare --stage B --base v2

# one patch, to reproduce a single error by hand
uv run -m baseline_llmjudge.defects4j.run_one -o \
  --patch_file ../drr/Patches/Doverfitting/DeepRepair/Lang/patch1-Lang-27-DeepRepair.patch \
  --prompt_version v2 --samples 5

# one patch, to measure its evidence size — no model is called
uv run -m baseline_llmjudge.defects4j.run_one -c \
  --patch_file ../drr/Patches/Dcorrect/ACS/Lang/patch1-Lang-7-ACS.patch \
  --prompt_version s1 --samples 0 \
  --cache_dir ../results/llmjudge_cache_semantic

# the queue on its own, for either pool
python3 java/dataset/build_split_queue.py --side dev --kind semantic
```

| Option | Meaning |
|---|---|
| `--side dev\|holdout` | Which side of the frozen split to score |
| `--kind crashing\|semantic` | Which pool to score (default `crashing`) |
| `--prompt_version` | A stage-A design (`v1`…`v3`, `s1`…`s3`) or a stage-B iteration (`v2.1`). The default is the pool's first design |
| `--samples N` | Samples per patch (default 5). `0` extracts the evidence and stops |
| `--projects "Lang Math"` | Restrict to some projects |
| `--model` | Override the model; the default is `config.LOCAL_LLM_MODEL` |
| `--cache_dir` | Where extracted evidence is cached. The default is `results/llmjudge_cache` for the crashing pool and `results/llmjudge_cache_semantic` for the semantic one |
| `--refresh_context` | Re-extract even when cached |
| `--confirm_holdout` | Required for `--side holdout` |
| `--out_dir` | Override the run directory; the default name carries the side |
| `--dry_run` | Build the queue and stop |

Output lands in `results/llmjudge_<side>_<version>_<timestamp>/`:
`records.jsonl`, `summary.json`, `queue.txt`, and a copy of the split with its
git provenance — so a number stays traceable to the split and the code that
produced it.

---

# 11. The Project Zero dataset

Sections 1 to 10 describe the Defects4J baseline. This section describes a
second dataset, and a second set of modules that score it. The two do not share
a population, a prompt or a split. They share the output space, the vote rules
and the confusion matrix.

**Read this section before you quote any number from it.** The population is 21
fixes. That is a pilot, not a result.

## 11.1 What the dataset holds

The Project Zero dataset is a set of variant pairs. It lives in
[`src/db/project_zero/pairs/`](../db/project_zero/pairs/). A **variant pair** is
two upstream security fixes for one root cause:

- `fix0` is the **prior fix**. It shipped. It left a sibling bug behind, and a
  later CVE reported that sibling bug.
- `fix1` is the **later fix**. It removed what `fix0` missed. No third CVE
  followed it.

So `fix0` is an overfitting patch by the definition in
[`.claude/CONTEXT.md`](../../.claude/CONTEXT.md). It removed the reported
symptom without the root cause. `fix1` is the correct patch of that pair.

Three properties separate this dataset from drr, and each one changes the
design:

1. There is no label file. The pair convention *is* the label.
2. There is no trigger test, no proof-of-concept input and no buildable tree.
   The dataset stores two diffs and one metadata file per pair.
3. The code is C and C++. So the parity target is the C/C++ front-end, not the
   Java front-end.

## 11.2 The population, and why the unit is one fix

**The unit is one fix, never one pair.** 43 pair directories hold only 35
distinct `fix0` commits. One prior CVE, `CVE-2019-13720`, carries six pairs on
its own. Every pair of that CVE renders the same diff. A per-pair queue would
enter one fix into the confusion matrix six times, and pay for it five times
over.

[`project_zero/queue.py`](project_zero/queue.py) collapses a repeat by commit, and prints how many
it dropped.

| Class | Rule | Rows |
|---|---|---|
| `overfitting` | Every distinct `fix0` | 32 |
| `correct` | Every distinct `fix1`, less two exclusions | 22 |
| | **Total, positive prior 0.59** | **54** |

That count is after `resolve_gerrit.py` ran on every repository. Before it, 21
of the 43 `fix0` ids were Gerrit change numbers and the population was 21 rows.
Section 11.6 explains why.

**Two exclusions apply to the negative class.** Each one removes a fix that is
known to be incomplete:

1. A `fix1` commit that is also a `fix0` somewhere in the dataset.
2. A `fix1` whose CVE is a prior CVE somewhere in the dataset.

Five CVE ids of this dataset act in both roles: `CVE-2019-11707`,
`CVE-2019-13720`, `CVE-2021-30551`, `CVE-2022-1096` and `CVE-2022-3723`. A fix
that left a sibling bug cannot serve as an example of one that did not.

**The rows cluster by project.** 45 of the 54 rows carry the `chrome` codebase
label, which the dataset uses for v8 as well. Rows of one project share a
coding style, a review culture and a fault vocabulary. So `summary.json`
carries a `by_codebase` breakdown, and the split balances that label across the
two sides. Any interval computed as though 54 rows were independent is too
narrow.

## 11.3 The negative-class confound

The negative class is the later fix of the same pair. A later fix is later in
time, and it had more scrutiny. So a judge can score well on this population by
detection of recency rather than of completeness.

The firewall in section 11.4 removes every date, every identifier and the whole
commit message. No token states the order. The firewall cannot remove a
difference in coding style, and fix size is the readable half of that.

**So every summary carries two baselines that read no code.** Both take their
best form on the scored rows themselves, so both are floors and neither is a
competitor.

1. `always_positive` calls every fix an overfitting patch. The prior of 0.57
   alone gives it F1 0.727. Any result on this population must beat that first.
2. `size_rule` calls a fix an overfitting patch when its diff is shorter than
   T. This is the proxy control for the confound.

**A degenerate threshold is excluded from the size rule.** The largest diff
length makes the rule predict every row positive, which is `always_positive`
under another name. The measured best honest threshold is 11,414 characters,
and it scores F1 0.688 — below the always-positive floor. **So fix size does
not separate the two classes on this population.** That is a measured result
about the confound, and it is the reassuring direction.

Read `headline.f1` against the higher of the two floors.

A stronger negative class exists, and this plan does not build it: security
fixes from the same repositories with no later CVE against the same files. That
needs a new harvest, and its label is weak. "No known sibling bug" is not "no
sibling bug".

## 11.4 The leakage firewall

The pair convention is written all over the data. So
[`project_zero/firewall.py`](project_zero/firewall.py) is the one module that reads `metadata.json`
or a raw `.patch` file. It returns two views:

- `PairRecord` is the **selector view**. It carries the CVE ids, both commits
  and both repository URLs. The queue builder and the fetch tool read it.
  Nothing renders it.
- `Fix` is the **clean view**. It carries the diff, the touched files, the
  fetched source and the codebase label. Only this view reaches a prompt.

Six leak channels exist. Each one has its own rule.

| Channel | The leak | The rule |
|---|---|---|
| Directory name | `<PRIOR>__<LATER>` names the later CVE | The clean view carries `fix_id`, the first eight hex digits of the commit digest |
| File name | `fix0.patch` against `fix1.patch` | The clean view has no file name |
| Metadata fields | `relationship_kind` states the verdict; `deep_reasoning` explains it in prose | `KEPT_METADATA` is an allow-list of two fields |
| Dates | `fix0_date` and `fix1_date` order the two fixes | Neither view carries a date |
| Commit message | See below | The clean view starts at the first `diff --git` line |
| Diff body | One patch adds a ChangeLog entry that reads "This is CVE-2020-15999" | `scrub` masks every CVE id, tracker id, ISO date and blob hash |

**The commit message is the richest channel, so the firewall drops it whole.**
Four real examples from this dataset: "This is CVE-2020-15999";
"commit 5eeb2ca0 upstream"; "Bug 1607443 - Fix some alias sets"; and a `Fixes:`
tag that names the commit under repair. Message presence is not even symmetric:
only 49 of the 86 patch files carry one. So presence alone would separate the
two classes without one line of code read.

That rule costs real evidence. `withheld_pipeline_evidence` lists
`commit_message` in every record, so the cost stays visible.

**Two path rules deserve a note.** v8 names a regression test after the bug it
covers, for example `test/mjsunit/compiler/regress-crbug-1228407.js`. A bug
number rises over time, so it orders the two fixes. The firewall therefore masks
a 6-to-8 digit run on a diff header line, and it leaves a numeric constant in
code alone. It also keeps two sets of paths: the real paths address the disk,
and the masked paths go in the prompt.

**Four Bugzilla attachments hold a second changeset header inside the diff.** So
`drop_metadata_lines` removes a metadata line wherever it appears, not only in
the leading message. Every pattern anchors at column 0. A diff content line
always begins with `+`, `-` or a space, so no pattern can match code.

**The guard is a sweep, not a sample.**
[`tests/test_llmjudge_pz.py`](../../tests/test_llmjudge_pz.py) checks all 86
clean views against 12 leak patterns. A new pair cannot enter the dataset with a
leak the tests never saw.

## 11.5 The bug-kind gate

`.claude/CONTEXT.md` fixes the two words. A **crashing bug** is one that
something at run time reports by itself. A **semantic bug** is one that nothing
reports.

[`project_zero/bugkind.py`](project_zero/bugkind.py) classifies each distinct fix in two passes:

1. A rule pass over the added lines of the diff. Seven marker groups stand for
   a fault the run time reports: an assert, a bounds check, a null guard, a
   lifetime or refcount change, a type-cast guard, an overflow guard, and an
   alias or side-effect model.
2. A model pass on every fix the rule pass leaves unsure.

**The two passes are asymmetric on purpose.** "Nothing reports this at run time"
is the absence of a marker, so no regex can assert it. The rule pass can say
`crashing` or say nothing. `decided_by` records which pass ruled on each fix.

**The measured result.** 59 distinct fixes: 47 crashing, 12 semantic. The rule
pass decided 29, and the model pass decided 30.

Inside the scored population of 54 rows:

| Side | Rows | Crashing | Semantic |
|---|---|---|---|
| dev | 27 | 21 | 6 |
| holdout | 27 | 23 | 4 |
| whole | 54 | 44 | 10 |

`bugkind.py` classifies every fix in the dataset, so a larger population needs
no rerun. Only these counts move.

**So this dataset does not get two pools.** Four semantic rows on the holdout
side do not support a pool of their own. One run scores every row, and
`summary.json` carries a `by_bug_kind` breakdown. Read the semantic row as a
count, never as an F1. `--bug_kind` filters the population when you want one
pool, and `BUG_KIND` passes it through either driver.

## 11.6 The fetch step

The dataset stores no source tree, so
[`fetch_context.py`](../db/project_zero/tools/fetch_context.py) fetches the
touched files of each fix at that fix's own commit. It is a port of the Linux
twin, and it writes the same layout:

```
pairs/<PAIR>/fix0_context/<path/to/file.cc>
pairs/<PAIR>/fix1_context/<path/to/file.cc>
```

The scope is the directly modified files only. That is enough for a one-shot
judgement. It is not enough for harness generation, which needs the whole tree,
the transitive headers and the build system. Chromium's tree alone is 61 GB.

**A Gerrit change number, not repository size, caps the population.** A raw-file
endpoint addresses a file at a commit, so it needs a git SHA. 21 of the 43
`fix0_commit` values are Gerrit change numbers such as `CL/1888103`, and 13 of
the `fix1_commit` values are.

| Tier | Rule | Pairs |
|---|---|---|
| 1 (default) | A SHA on both sides, a supported host, and not `chromium/src` | 14 |
| all, before any resolution | Every pair with a SHA and a supported host | 16 |
| all, after `resolve_gerrit.py` | The same rule, with 24 change numbers resolved | **39** |

Six raw-file endpoints are supported: Gitiles, cgit on kernel.org, cgit on
savannah, Mercurial, GitHub and GitLab. Bugzilla is not. Its `fix0_commit` is an
attachment id, so no path-addressed route exists, and its four pairs stay out.

**One mirror is configured.** `git.savannah.gnu.org` does not answer from every
network. A git mirror shares the object ids of its origin, so a SHA addresses
the same file in both. `MIRRORS` maps freetype to its GitHub mirror.

**Two caps apply, and the tool prints both.** It skips a fix that touches more
than 15 files. It skips a file over 1 MB.

**The measured result.** 172 files in place over 39 pairs. Three permanent
failures: two in Skia and one elsewhere, each a file that does not exist at
that commit.

**Tier 2 raises the population.**
[`resolve_gerrit.py`](../db/project_zero/tools/resolve_gerrit.py) resolves a
change number to its merged SHA through the Gerrit REST API. It writes a
separate override file, `gerrit_resolved.json`, and it never rewrites
`metadata.json`. Two reasons, and the second matters more than tidiness:

1. The dataset stays as the harvester produced it.
2. `project_zero/firewall.py` derives `fix_id` from the commit id in
   `metadata.json`. A
   resolution written back would change every fix id, and `bug_kind.jsonl`
   would stop to match.

`--only_repo` defaults to `v8/v8`, which unlocks 11 changes. `--only_repo ''`
does every repository, and that resolved all 24. Both runs are done: the
population went from 21 rows to 54.

`chromium/src` stays out of tier 1 for cost, and `--tier all` includes it. Its
trees are the largest here, and a file-level fetch of two pairs is cheap.

## 11.7 The evidence, and the parity target

The Defects4J baseline rebuilds four of the five factual sections of the Java
harness prompt. That claim does not transfer. This dataset is C and C++, so the
counterpart is `LibFuzzerPromptBuilder.build`
([`oss_fuzz/prompts.py`](../oss_fuzz/prompts.py)).

| Block | Pipeline section | Origin here |
|---|---|---|
| `patch` | `_patch_block` | reused verbatim |
| `touched_files` | (none) | baseline only |
| `touched_source` | `_function_block` | baseline only |
| `codebase` | (none) | baseline only |

`patch` calls the pipeline's own method, so its wording and its
6000-character cap cannot drift. A test asserts the equality.

**`touched_source` is `baseline_only`, not `rendered`, and the distinction
matters.** The pipeline's `_function_block` carries one function, extracted by a
parser, and it states why that function is in the prompt. No C or C++ function
extraction runs on this path, so this block carries whole files instead. To call
it `rendered` would claim a parity that does not exist.

**The evidence gap is wider here than on the Defects4J side.** The Defects4J
baseline is missing execution evidence alone. This one is missing the
root-cause region as well, because no tree is checked out and no call graph is
built. Every record lists all of it:

| Withheld | Why |
|---|---|
| `_original_crash_block` | Needs the crash report and a proof-of-concept input. The dataset stores neither |
| `_routes_block` | Needs a checked-out tree and a call graph |
| `_reference_harness_block`, `_known_includes_block` | Need the project's own OSS-Fuzz harness |
| `compile_result`, `fuzz_result` | Need a build of the patched code |
| `commit_message` | Dropped by the firewall. See section 11.4 |

**Do not compare an F1 from this population with an F1 from the Defects4J
population.** The two judges did not see comparable evidence.

## 11.8 The prompts

[`project_zero/prompts.py`](project_zero/prompts.py) is a separate module, and the reason is
mechanical. `prompts.version_text` joins `SYSTEM` with a version's own wording,
and `version_sha256` digests the result. One edit to `prompts.SYSTEM` would
change the recorded digest of every scored Defects4J version. So this module
holds its own system message, its own registry and its own `resolve`. It imports
the `PromptVersion` shape and the output contract. Nothing here can move a
Defects4J number.

**The question changes, and the class names do not.** The Defects4J prompt shows
a failing test and asks whether a candidate patch fixes its root cause. There is
no test here, and no candidate patch either. Every fix shipped. So the question
becomes: did this fix remove the whole root cause, or did it leave a sibling bug?
`OVERFITTING` and `CORRECT` stay the two class names, so `verdict.py` parses
both datasets with one function.

The three stage-A designs mirror the three Defects4J bets one for one:

| Version | What the request supplies | The bet |
|---|---|---|
| `p1` | The evidence, the question, a gloss of each class, the output contract | The floor: what the model does unaided |
| `p2` | Definitions, the shipped-fix premise, a five-step method, two calibration rules | A method plus guard rails beats an unaided answer |
| `p3` | Definitions, the shipped-fix premise, five **required** output sections | A form the answer must fill in beats a method it may skip |

**Two premises the model must be told.** The fix is real and it shipped, so
"this would not compile" is never a valid reason. The fix stopped the
vulnerability reported at the time, so "the proof-of-concept still works" is not
one either. Neither premise reveals the label. Both classes shipped, and both
stopped the vulnerability reported at the time.

A third sentence tells the model that dates and identifiers are masked, and that
it must not guess them. Without it the model can read `DATE-MASKED` as a signal.

A stage-B iteration is `<base>.<n>`, for example `p2.1`. Register it by hand,
after you read the dev errors. `build_messages` takes a version name and the
evidence text. There is no third parameter, so an error log has no way in.

## 11.9 What is reused

Four things carry over by import, not by copy. So the two datasets cannot drift
apart on any of them.

| From | What |
|---|---|
| [`shared/verdict.py`](shared/verdict.py) | The one-bit output space, the last-`VERDICT:`-line parse, the three vote rules, `PARSE_FAILURE_COUNTS_AS`, `PARSE_RETRIES`, `DEFAULT_SAMPLES` |
| [`shared/scoring.py`](shared/scoring.py) | `confusion`, `RULES`, `HEADLINE_RULE`, `print_summary` |
| [`shared/blocks.py`](shared/blocks.py) | `Block`, and the rule that joins blocks into one prompt string |
| [`shared/version.py`](shared/version.py) | `PromptVersion`, and the output contract |
| [`shared/budget.py`](shared/budget.py) | The spend report and the price rules |
| [`shared/provenance.py`](shared/provenance.py) | Which commit produced a record |

`summarise` is local, and so is the Defects4J one. The two differ in what they
group by and in which prompt registry they read.

**There is no evidence cache.** The Defects4J side caches because extraction
needs a checkout and two test runs. Here the render is a few local file reads.
The record still carries `evidence_sha256`, so the claim that every prompt
version reads byte-identical evidence stays checkable.

## 11.10 The frozen split

The split lives in
[`suites/splits/project_zero_split.jsonl`](../../suites/splits/project_zero_split.jsonl),
and [`project_zero/split.py`](project_zero/split.py) freezes it.

**The split unit is a root-cause group.** The Defects4J splits assign a side
per bug, so every candidate patch of one bug lands on one side. A group is the
counterpart, and the reason is stronger than convention: the two fixes of a
pair repair one root cause, and they often touch one file. A judge tuned on one
fix of a pair and then scored on the other would be scored on code it was tuned
against.

Two pairs join one group when they share any identifier — a CVE id in either
role, or a commit on either side. The union is transitive. 43 pairs collapse to
20 groups. Three real reasons why:

1. Six pairs share one prior fix, `CVE-2019-13720`.
2. Five CVE ids act as a prior in one pair and a later in another, so those
   pairs chain: `CVE-2016-5128` to `CVE-2022-1096` to `CVE-2022-1232`.
3. Three Mozilla pairs share one later CVE, `CVE-2020-6820`.

**The balance rule adds three gaps, and each term earns its place.**

```
cost = |rows|  +  |positives|  +  sum over codebases of |rows of it|
```

| Terms | What it produced |
|---|---|
| Rows alone | 29/30 rows, priors 0.55 and 0.68 |
| Rows and positives | The prior gap closed, and the dev side came out entirely `chrome` |
| All three | The split below |

The second row is the interesting failure. A dev side of one project gives a
one-line `by_codebase` breakdown. Worse, a prompt tuned only on browser C++
would meet a Mali driver for the first time on the selection side.

A hill-climb refines the greedy sweep, over single moves and over swaps of one
dev group for one holdout group. Both shapes are needed: single moves alone got
stuck, because the improvement needed one group to go each way at once. The
climb is a local search, and it is not proved optimal.

**What is frozen, measured.**

| Side | Groups | Rows at freeze | Prior | Scored rows | Codebases |
|---|---|---|---|---|---|
| dev | 10 | 31 | 0.58 | **27** | chrome 23, apple-webkit 2, qualcomm-android 2 |
| holdout | 10 | 28 | 0.64 | **27** | chrome 22, mozilla-gecko 3, mali-gpu-driver 2 |

The scored priors are 0.56 and 0.63. A gap of 0.07 is well inside what the
protocol tolerates elsewhere: the Defects4J crashing pool carries 0.21.

**The side is frozen per group, not per row.** A group keeps its side when a
later context fetch adds a row to it. So `rows_at_freeze` can go stale, and
`queue.py` always recounts. The Defects4J splits have the same property, and
section 1 records the same kind of drift there.

**A frozen split is frozen.** `split.py` refuses to overwrite one without
`--force`. A split rewritten after a version was scored against it would
silently change what that score refers to.

### Read the size before you read any F1

27 rows a side. That is enough to rank three designs. It is not enough to
separate two of them. **No F1 difference under about 0.2 is real here.**
Section 11.13 carries the rest of the threats.

## 11.11 The two-stage protocol, and its drivers

The protocol is the one section 6 describes, with one dataset swapped in.

```
STAGE A — blind. No records file is read. Scored on dev.
    p1  ──run on dev──┐
    p2  ──run on dev──┼──→  best dev F1 wins
    p3  ──run on dev──┘

STAGE B — refinement of the winner.
  refinement reads DEV only:
      p2 ──read p2's DEV records──→ p2.1 ──read p2.1's DEV records──→ p2.2 ...
  every iteration runs on BOTH sides:
      p2.1 ──dev──→ records, read to write p2.2  ──holdout──→ F1
  the highest holdout F1 is the reported version
```

Two drivers wrap it, and they mirror `llmjudge_stage_a.sh` and
`llmjudge_stage_b.sh`:

| Script | What it does |
|---|---|
| [`llmjudge_pz_stage_a.sh`](../../scripts/llmjudge_pz_stage_a.sh) | Run every stage-A design on dev, then the comparison. Logs in `results/llmjudge_pz_stageA_<ts>/` |
| [`llmjudge_pz_stage_b.sh`](../../scripts/llmjudge_pz_stage_b.sh) | Run one stage-B turn: dev, then holdout. Then the comparison, once every iteration has a holdout pass. Logs in `results/llmjudge_pz_stageB_<base>_<ts>/` |

```bash
DRY_RUN=1 scripts/llmjudge_pz_stage_a.sh    # build the populations, spend nothing
scripts/llmjudge_pz_stage_a.sh              # the bake-off, then compare

scripts/llmjudge_pz_stage_b.sh p2           # the winner's holdout reference row
scripts/llmjudge_pz_stage_b.sh p2.1         # turn 1: dev, then holdout
```

**Three differences from the Defects4J drivers, and each one is a property of
this dataset:**

1. **There is no `KIND`.** The bug-kind gate found too few semantic fixes for a
   pool of its own, so one run scores every row. `BUG_KIND` can still filter.
2. **There is no evidence cache and no `errors.py`.** The render is a few local
   file reads. A turn is written from the dev run's `records.jsonl`, read by a
   person.
3. **Every row prints its floor.** [`project_zero/compare.py`](project_zero/compare.py)
   adds a `floor` column — the higher of the two baselines that read no code —
   and it marks a design whose F1 does not clear that floor by more than 0.05.
   A design can win the bake-off and still be unproven, and those are two
   different claims.

**Two protocol rules the drivers enforce rather than leave to the operator:**

- Stage A is blind. `compare.py` runs only when every design finished, so a
  winner is never named off a partial bake-off.
- The pre-registration comes first. No holdout pass runs until a prereg note
  exists, because a holdout number written before the design is fixed is not a
  held-out number.

## 11.12 Iteration log

One stage-A design ran. Two are written and frozen, and neither has run.

### `p1` — the floor. Dev, 21 fixes, 5 samples, `gpt-5.4`, effort `low`

| Figure | Value |
|---|---|
| Headline rule | `majority` |
| TP / FN / FP / TN | 0 / 12 / 0 / 9 |
| Precision, recall, F1 | n/a, 0.000, 0.000 |
| Specificity | 1.000 |
| `any` rule | P 0.667, R 0.333, F1 0.444 |
| Mean sample agreement | 0.867 |
| Parse failures | 0 of 105 |
| Spend | 105 calls, 650,605 in (459,776 cached), 113,895 out |

**`p1` never answers OVERFITTING under the majority rule.** Not once, on any of
the 21 fixes. The vote counts show how one-sided it is. On the 12 positives the
votes were eight zeros, two ones and two twos. On the 9 negatives they were
seven zeros and two ones. So no fix reached the 3-of-5 threshold.

**This is a collapse, not a parse failure.** All 105 samples parsed. The
answers are substantive: the model names the root cause, quotes the guard the
fix adds, and argues that the guard removes the condition rather than the one
input. It reads the code and then it clears the fix.

**The `any` rule shows the signal is not zero.** At 1 of 5 the model reaches
F1 0.444 with precision 0.667. So `p1` does raise doubt about some positives.
It never raises enough doubt to carry a majority.

**The diagnosis, and it points at the prompt.** `p1` is the floor design. It
supplies no premise, no method and no required sections. The evidence tells it
that upstream maintainers wrote and shipped this fix. With no instruction to
look for a sibling bug, "a reviewed upstream fix is correct" is the answer that
costs the least. `p2` and `p3` both carry the premise, the class definitions
and either a method or a required form. They exist to test exactly this.

**What it does not yet show.** The evidence question from the plan stays open.
A judge that answers one class every time tells us nothing about whether the
diff plus the touched source carries enough signal. Run `p2` and `p3` before
you conclude anything about the evidence.

**The blind bake-off still holds.** `p2` and `p3` were authored and frozen
before any run, and `prompt_sha256` records their text. So this read-out cannot
have shaped them.

**The run directory of this pass was deleted by accident, and the raw records
are gone.** The table above, the vote counts and the baselines are the whole of
what survives. The 105 model answers do not. So `errors.py` has nothing to read
for this pass, and a stage-B turn on `p1` would need the pass to run again.
Re-run `p1` before you write any `p1.n`, or start stage B from whichever of
`p2` and `p3` wins instead.

## 11.13 Threats to validity

- **The negative class is the later fix of the same pair.** See section 11.3.
  The proxy control bounds this threat. It does not remove it.
- **Each side holds 27 rows in 3 codebase labels.** No F1 difference under
  about 0.2 is real. Two prompt versions cannot be separated on one side.
- **The always-positive floor is high.** The dev prior is 0.56 and the holdout
  prior is 0.63, so a rule that answers OVERFITTING every time already scores
  F1 near 0.72. Quote the floor beside any F1 from this population.
- **The stage-B winner's holdout F1 is a maximum over the iterations**, so it
  is optimistic. Publish every holdout row. `compare.py` prints the sentence
  that states this honestly.
- **The split's hill-climb is a local search.** A better-balanced assignment
  may exist. The split is frozen, so this is a property of the population every
  score refers to, not a per-run variation.
- **A negative label is an absence of evidence.** No third CVE followed `fix1`.
  That is not proof that `fix1` left no sibling bug.
- **Contamination.** Every CVE here is public, and the fixes are in the
  training data of any recent model. This helps the baseline, not the pipeline,
  so it works against the claim. That is the safe direction.
- **The bug kind of 30 fixes came from a model, not a rule.** The
  `by_bug_kind` breakdown rests on that classification.
- **The evidence is thinner than on the Defects4J side.** See section 11.7.
- **The pipeline cannot run on these codebases yet.** So this baseline has no
  comparison target on this dataset. It measures the baseline alone.

## 11.14 Files

| File | Responsibility |
|---|---|
| [`project_zero/firewall.py`](project_zero/firewall.py) | The one reader of `metadata.json` and the raw diffs. Returns the selector view and the clean view |
| [`project_zero/bugkind.py`](project_zero/bugkind.py) | Classify each fix as crashing or semantic, by rule then by model. Writes `bug_kind.jsonl` |
| [`project_zero/split.py`](project_zero/split.py) | Freeze the dev/holdout split. One side per root-cause group |
| [`project_zero/queue.py`](project_zero/queue.py) | The scored population: one row per distinct fix commit, with the two negative-class exclusions. Filters by side and by pool |
| [`project_zero/evidence.py`](project_zero/evidence.py) | Render the four evidence blocks; build the parity manifest and the withheld list |
| [`project_zero/prompts.py`](project_zero/prompts.py) | The three stage-A designs, the stage-B registry, and the message builder |
| [`project_zero/run_one.py`](project_zero/run_one.py) | One fix, N samples, one record. `--samples 0` renders and stops |
| [`project_zero/evaluate.py`](project_zero/evaluate.py) | One side of the split: records, summary, the two breakdowns, the two baselines |
| [`project_zero/compare.py`](project_zero/compare.py) | Name the stage winner: stage A on dev F1, stage B on holdout F1. Prints each row's floor |
| [`fetch_context.py`](../db/project_zero/tools/fetch_context.py) | Fetch the touched files of each fix, at that fix's own commit |
| [`resolve_gerrit.py`](../db/project_zero/tools/resolve_gerrit.py) | Resolve a Gerrit change number to its merged SHA. Writes `gerrit_resolved.json` |
| [`llmjudge_pz_stage_a.sh`](../../scripts/llmjudge_pz_stage_a.sh) | Run stage A: every design on dev, then the comparison |
| [`llmjudge_pz_stage_b.sh`](../../scripts/llmjudge_pz_stage_b.sh) | Run one stage-B turn: dev, then holdout, then the comparison when every iteration is in |
| [`project_zero_split.jsonl`](../../suites/splits/project_zero_split.jsonl) | The frozen split: one row per root-cause group |

Guards live in
[`tests/test_llmjudge_pz.py`](../../tests/test_llmjudge_pz.py).

## 11.15 Usage

Run the two dataset tools from `src/db/project_zero/`. Run everything else from
`src/`.

Steps 1 to 4 are the setup. They run once, and step 4 is the only one that
cannot be repeated freely.

```bash
# 1. raise the population: resolve every Gerrit change number to a SHA
cd db/project_zero
uv run python tools/resolve_gerrit.py --dry_run
uv run python tools/resolve_gerrit.py --only_repo ''

# 2. fetch the source of the touched files of every reachable fix
uv run python tools/fetch_context.py --tier all --dry_run
uv run python tools/fetch_context.py --tier all

# 3. classify the bug kind of every fix — the gate
cd ../..
uv run -m baseline_llmjudge.project_zero.bugkind --rules_only   # no model call
uv run -m baseline_llmjudge.project_zero.bugkind                # model pass

# 4. freeze the split. --force is needed to overwrite a frozen one
uv run -m baseline_llmjudge.project_zero.split --dry_run
uv run -m baseline_llmjudge.project_zero.split
```

Then the protocol. Prefer the drivers of section 11.11 — they keep one stage's
logs in one folder and they enforce two protocol rules.

```bash
# 5. print each population and stop, before any model call
uv run -m baseline_llmjudge.project_zero.queue --side dev
uv run -m baseline_llmjudge.project_zero.evaluate --side dev --dry_run

# 6. stage A: every design on dev, then the comparison
cd ..
DRY_RUN=1 scripts/llmjudge_pz_stage_a.sh
scripts/llmjudge_pz_stage_a.sh

# 7. stage B, one turn at a time. p2 stands for the stage-A winner
scripts/llmjudge_pz_stage_b.sh p2      # the winner's holdout reference row
scripts/llmjudge_pz_stage_b.sh p2.1    # turn 1: dev, then holdout

# or drive one pass by hand
cd src
uv run -m baseline_llmjudge.project_zero.evaluate --side dev --prompt_version p1
uv run -m baseline_llmjudge.project_zero.evaluate --side holdout \
    --prompt_version p1 --confirm_holdout
uv run -m baseline_llmjudge.project_zero.compare --stage A

# 8. one fix, to reproduce a single error by hand
uv run -m baseline_llmjudge.project_zero.run_one \
  --pair CVE-2021-30551__CVE-2022-1096 --which fix0 \
  --prompt_version p1 --samples 5

# 9. one fix, to measure its evidence size — no model is called
uv run -m baseline_llmjudge.project_zero.run_one \
  --pair CVE-2021-30551__CVE-2022-1096 --which fix0 --samples 0
```

| Option | Meaning |
|---|---|
| `--prompt_version` | A stage-A design (`p1`…`p3`) or a stage-B iteration (`p2.1`). The default is `p1` |
| `--samples N` | Samples per fix (default 5). `0` renders the evidence and stops |
| `--bug_kind crashing\|semantic` | Score one pool only. The default scores every row |
| `--model` | Override the model. The default is `config.LOCAL_LLM_MODEL` |
| `--out_dir` | Override the run directory |
| `--allow_missing_source` | Keep a fix whose context fetch produced no file |
| `--dry_run` | Build the population and stop |
| `--side dev\|holdout` | Which side of the frozen split to score (default `dev`) |
| `--confirm_holdout` | Required for `--side holdout` |

Output lands in `results/llmjudge_pz_<side>_<version>_<timestamp>/`. The side
is in the name, because `compare.py` finds the runs of one side by globbing it.
Each directory holds `records.jsonl`, `summary.json`, `queue.txt`,
`population.json`, a copy of the frozen split, and the git provenance of both
the split and the pairs directory — so a number never loses its population.
