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
| Evidence renderer | [`evidence.py`](evidence.py) | [`evidence_semantic.py`](evidence_semantic.py) |
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

### 2.2 The semantic renderer

The pipeline's semantic prompt is `_build_semantic`, the branch `build` takes
when `bug_kind == "semantic"`. It joins up to eleven sections.
[`evidence_semantic.py`](evidence_semantic.py) rebuilds the factual ones.

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
writes one new `PromptVersion` in [`prompts.py`](prompts.py) by hand. About a
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
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v1
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v2
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v3

uv run -m baseline_llmjudge.compare --stage A     # names the winner, on dev F1

# the winner on the holdout, as stage B's reference row
uv run -m baseline_llmjudge.evaluate --side holdout --prompt_version v2 \
    --confirm_holdout

# the semantic pool — the same three passes against the semantic split
uv run -m baseline_llmjudge.evaluate --side dev --kind semantic \
    --prompt_version s1
uv run -m baseline_llmjudge.evaluate --side dev --kind semantic \
    --prompt_version s2
uv run -m baseline_llmjudge.evaluate --side dev --kind semantic \
    --prompt_version s3

uv run -m baseline_llmjudge.compare --stage A --kind semantic
```

**Measure before you spend.** A semantic prompt is several times the size of a
crashing one, because the class skeletons and the test support come with it.
`--samples 0` extracts the evidence, caches it, prints its size, and calls no
model. Run it on a few patches of different projects, read the character counts,
then decide what a pass costs:

```bash
uv run -m baseline_llmjudge.run_one -c \
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
uv run -m baseline_llmjudge.errors \
    --records ../results/llmjudge_dev_v2_<ts>/records.jsonl

# the dev pass feeds turn 2; the holdout pass is v2.1's selection score
uv run -m baseline_llmjudge.evaluate --side dev     --prompt_version v2.1
uv run -m baseline_llmjudge.evaluate --side holdout --prompt_version v2.1 \
    --confirm_holdout

# turns 2 and 3 — same shape, reading the previous turn's DEV records
uv run -m baseline_llmjudge.errors \
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
uv run -m baseline_llmjudge.compare --stage B --base v2
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
| `s3.1` | FP. All 20 FP named a real residual imperfection in or near the patched method, and none named the REPORTED fault. Math-59 cited signed-zero semantics against a reported reversed result; Closure-62 cited an extra caret the patch itself adds; Math-2 cited a second arithmetic issue. Nine of the 20 are the Math-59 and Math-30 clusters, all unanimous. | | | | | | |
| `s3.2` | | | | | | | |
| `s3.3` | | | | | | | |

**Stage B — selection on holdout**

| Version | P | R | F1 | FP | FN | Run directory |
|---|---|---|---|---|---|---|
| `s3` (reference) | | | | | | |
| `s3.1` | | | | | | |
| `s3.2` | | | | | | |
| `s3.3` | | | | | | |

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
| [`context.py`](context.py) | Extract the evidence for one patch, by the steps of its pool; cache it as rendered text |
| [`evidence.py`](evidence.py) | Render the crashing evidence blocks; build the parity manifest |
| [`evidence_semantic.py`](evidence_semantic.py) | Render the semantic evidence blocks; list the withheld evidence |
| [`prompts.py`](prompts.py) | Each pool's three stage-A designs, and the hand-written stage-B iterations. Builds the messages: system, task, evidence, instruction |
| [`verdict.py`](verdict.py) | Parse one verdict; combine samples under the three vote rules |
| [`budget.py`](budget.py) | Tokens to cost, with the price and cache rules above |
| [`errors.py`](errors.py) | Print one dev pass's errors — the input to the next stage-B iteration. Refuses holdout records |
| [`compare.py`](compare.py) | Name the stage winner of one pool: stage A on dev F1, stage B on holdout F1 |
| [`run_one.py`](run_one.py) | One patch, N samples, one record. `--samples 0` extracts and stops |
| [`evaluate.py`](evaluate.py) | One side of one pool's split: records, summary, budget |
| [`build_split_queue.py`](../java/dataset/build_split_queue.py) | The queue for one side of one pool, from the frozen split and the labels |
| [`llmjudge_stage_a.sh`](../../scripts/llmjudge_stage_a.sh) | Run stage A: the pool's three designs on dev, then the comparison. Logs land in `results/llmjudge_stageA_<kind>_<ts>/` |
| [`llmjudge_stage_b.sh`](../../scripts/llmjudge_stage_b.sh) | Run one stage-B turn: dev, then holdout, then the comparison once all three turns are in. Logs land in `results/llmjudge_stageB_<base>_<ts>/` |

Guards live in [`tests/test_llmjudge_baseline.py`](../../tests/test_llmjudge_baseline.py).

## 10. Usage

Run from `src/`. Section 6 carries the protocol's command sequence in order.
These are the entry points and their options. The two driver scripts of section 6 —
`scripts/llmjudge_stage_a.sh` and `scripts/llmjudge_stage_b.sh` — wrap these
commands, and they keep the logs of one stage in one folder. Run a script from
the repository root.

```bash
# print a queue and stop, before any model call
uv run -m baseline_llmjudge.evaluate --side dev --dry_run
uv run -m baseline_llmjudge.evaluate --side dev --kind semantic --dry_run

# one side of one pool, one prompt version
uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v1
uv run -m baseline_llmjudge.evaluate --side holdout --prompt_version v2.1 \
  --confirm_holdout
uv run -m baseline_llmjudge.evaluate --side dev --kind semantic \
  --prompt_version s1

# a dev pass's errors — the input to the next hand-written iteration
uv run -m baseline_llmjudge.errors \
  --records ../results/llmjudge_dev_v2_<timestamp>/records.jsonl

# name the stage winner: stage A on dev F1, stage B on holdout F1
uv run -m baseline_llmjudge.compare --stage A
uv run -m baseline_llmjudge.compare --stage A --kind semantic
uv run -m baseline_llmjudge.compare --stage B --base v2

# one patch, to reproduce a single error by hand
uv run -m baseline_llmjudge.run_one -o \
  --patch_file ../drr/Patches/Doverfitting/DeepRepair/Lang/patch1-Lang-27-DeepRepair.patch \
  --prompt_version v2 --samples 5

# one patch, to measure its evidence size — no model is called
uv run -m baseline_llmjudge.run_one -c \
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
