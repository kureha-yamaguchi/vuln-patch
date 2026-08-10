# Divergence capture at the diff boundary (`--divcap`) — 2026-08-10

**Status:** built, unit-tested on the Mac, NOT yet run on the VM. Nothing in
this build turns the flag on; the queued flagship sweep is unaffected.
Implements the pre-registration at the bottom of
`docs/diff-divergence-capture-design-2026-08-10.md` (decisions 1–4 taken).

**Target station:** evidence assembly for relation synthesis — `src/java/run.py`
step 4.55, immediately before `RelationSynthesizer.synthesize` — plus a
build-time instrumentation pass that is a sibling of diffcov's
(`PatchedProjectBuilder.build_patched_dir`, between "patch applied" and
`defects4j compile`).

**Failure mode it targets:** a leg where reach is saturated and invention is
absent. 8.36 measured ~2M entries per harness into the patch-changed method
with zero firings; 8.37's replay showed 73/74 relations transform and execute
without a tier-2 event. The relations probe the class's documented observables
— but never the one the patch distorts, because nothing in the pipeline's
evidence says WHICH observable the patch touches. Invention is then guessing.

**The soundness rule, and it is the whole design:** a divergence steers
ATTENTION (which observable, which input region). It is NEVER an oracle. A
correct patch diverges from the buggy build too — that is what fixing means —
so neither recorded value may become an expected value. Two things enforce it:
the prompt block says so explicitly (§4, verbatim), and the screen demotes any
relation that anchors on a pre-patch value anyway (§5).

---

## 1. Which methods, and what is observable about them

`divcap.wanted_from_patch(patch_text, patched_dir)` — the scope is decided by
**diffcov's `changed_methods`, imported, not re-derived**. Same post-patch line
mapping, same smallest-enclosing-declaration rule, same overload-separating
method id `<fq.Class>#<name>(<Type,Type>)`. `tests/test_divcap.py` asserts the
identity of the two function objects, so a future copy of that parser fails the
suite rather than drifting.

What `changed_methods` hands over is a **signature triple** `(class, method,
param_types)`, not an offset. That is what pairs the two trees: the same
declaration sits at a different offset in the buggy sources, and the signature
is the only stable key.

Per method, one of two capture shapes:

| method shape | observable | mechanism |
|---|---|---|
| returns a value | its return value | a local declared at entry + each of its own `return <expr>;` rewritten |
| `void` instance method, or a constructor | the receiver's primitive/array field state AFTER the call | body wrapped in `try { … } finally { DivObs.state(…, this) }` |
| `static void` | none | skipped, recorded as `static void (no observable)` |

Everything unrewritable is **recorded, not dropped** — abstract/native/interface
declarations, a declaration the other tree does not contain (a method the patch
ADDED), a body containing a lambda, a body with no value-returning statement, a
file javalang cannot parse. Without that, an empty capture reads as "the patch
moves nothing" when it means "there was nothing to watch."

The receiver-state path IS built (the instruction allowed dropping it). It
reuses the reflection state-printer pattern from `relation_screen._STATE_PRINTER`
— primitive and primitive-array fields only — rewritten in the old Java dialect
and with the fields sorted BY NAME, because `getDeclaredFields()` order is
unspecified and an ordering difference between two class files would read as a
divergence.

## 2. The capture site, and its honest limits

**Chosen site: the bug's own trigger tests, driven through two instrumented
trees, before synthesis runs.**

- `<checkout>_patched_<stem>_divcap` — the patched sources, instrumented.
- `<checkout>_divcap_buggy` — the unpatched sources, the same signatures
  instrumented, from the list the patched pass wrote to `.divcap_wanted.json`.
- `defects4j test -t <trigger>` on each, once per trigger test, with
  `VULNPATCH_DIVCAP_OUT` pointing at `divobs.out` in that tree.

**Why this site and not the screen/replay pair.** The relation screen (buggy
build) and `replay_on_patched` (patched build) do run the same compiled harness
against both builds over the same trigger-literal corpus, and the RecFDP
consumed vector would be a perfect pairing key there. But both run AFTER
synthesis. Divergence facts collected there could only reach a SECOND synthesis
round, which the pipeline only performs conditionally
(`converge_nonseed_arsenal`) — so on most legs the mechanism would never fire.
The point of the mechanism is to steer invention; a site downstream of
invention is a measurement, not a mechanism. The trigger tests are the one
execution this pipeline already sends through both builds with byte-identical
inputs, no fuzzing luck in between, and no dependency on anything the LLM has
produced yet.

**Its limits, stated plainly:**

1. **The input set is narrow and already named.** The failing test's own
   execution is the only driver. Whatever the patch moves outside the trigger
   region is invisible. This is exactly the risk the design doc predicted for
   Lang-63: "the facts point at the same observable the trigger test already
   names." The mechanism narrows the search; it cannot write the relation.
2. **The consumed-input vector is never available here**, so the pairing key is
   always the logged argument tuple (the prereg's fallback). `parse_divobs` and
   `diff_observations` are agnostic about where the key came from, so a later
   screen/replay extension can supply `__consumed` without touching the diff
   logic — but today, nothing does.
3. **Cost:** two extra `copytree` + `defects4j compile` per leg, plus one
   `defects4j test -t` per trigger test per tree. Bounded, flag-gated, and
   nothing else in the pipeline shares these trees.
4. **Argument-only mutation is not observed.** A non-void method that mutates
   its receiver or its arguments is captured by its return value alone.
5. **Non-determinism inside a build produces no facts, by design** — see §3.
6. **Object identity hashes are normalised away** (`Foo@1a2b3c` → `Foo@`) or
   every object without a `toString()` would read as a divergence. A value whose
   `toString()` embeds a timestamp or a hash-ordered map still can, and the
   within-build stability check is the only thing catching it.

**Not built (named future extension, one variable per mechanism):** direct
callers of the changed methods (prereg decision 2), and the screen/replay
capture site with the consumed vector as the key.

## 3. Pairing, the diff, and the ranking

**Pairing key: `(method_id, rendered argument tuple)`.** The Java side keeps one
record per distinct argument tuple per method, up to `DIVCAP_MAX_SHAPES` (64),
and counts repeat observations up to `MAX_OBS` (200,000) before the method goes
inert — after which `args()` is one map lookup and a null return.

**Whole values, typed, capped COUNT — the 8.31 lesson.** Values carry the
pipeline's existing type tags (`i:`, `d:`, `q:"…"`, `ia:[…]`, `o:<class>(…)`)
and are never width-clipped. The only width bound is a 20,000-char per-LINE
safety valve, and when it bites it says `[divcap-line-truncated]` in the line
and marks the record unstable, so a clipped value can never be paired silently.

**Fail-closed at every ambiguity.** A pair becomes a divergence only when:
- both sides observed the SAME argument tuple (an unpaired tuple is an unknown,
  and an unknown must not steer invention);
- both sides were STABLE within their own build (the same tuple never produced
  two different values in one JVM, and the merge of several trigger-test runs
  agreed) — a value that is not stable inside one build cannot evidence a
  difference between builds;
- both sides watched the same observable kind;
- and the values differ.

`count` is `min(buggy, patched)` — the number of times BOTH builds were observed
on that tuple. Claiming the larger side would overstate it.

**Ranking (prereg decision 3): K=8, distinct-shape diversity first, frequency
second.** Diversity is applied twice: methods are ordered by how many distinct
argument tuples diverge under them, then the eight slots are filled
ROUND-ROBIN across methods. So eight slots cannot all be eaten by one method's
near-identical tuples while a second changed method goes unmentioned. Within a
method, most-frequent tuple first, tie-broken on the shape string so the order
is deterministic.

## 4. The synthesis prompt block — verbatim

Rendered into the grounding context ONLY when there are divergences, so a run
without the flag (and a run with the flag that observed nothing) sees a
byte-identical prompt. It is in the shared context, so the focused per-source
passes see it too. Header:

> OBSERVED DIVERGENCES AT THE CHANGED CODE (mechanical measurement, NOT a
> specification). The pipeline ran the same inputs through the build WITHOUT the
> patch and the build WITH it, and recorded, at each method the patch changed,
> the value of the named observable. Listed below are the observables whose
> value MOVED, with the input tuple they moved on. This tells you WHICH
> observable of WHICH method the patch actually changes, and on what shape of
> input — that is the only thing it tells you:

then one `<divergences>` block of rows shaped

```
    method=<id> observable=<return value|receiver state after the call> input=<typed tuple> value_without_the_patch=<typed> value_with_the_patch=<typed>
```

then, verbatim:

> HOW TO USE THIS — the difference between a sound relation and a false
> accusation:
>   * USE it to CHOOSE the observable and the input region your relations
> target. An observable listed here is one the patch demonstrably moves; a
> relation that never reads it cannot tell the two builds apart, which is how
> whole sets of otherwise-sound relations end up silent.
>   * DO NOT use either recorded value as an expected value, in any relation,
> ever. Neither side is ground truth. The value from the build WITHOUT the patch
> is the value of code KNOWN TO BE WRONG here; the value from the build WITH it
> is the value of the very code you are being asked to judge. A check that
> asserts either one is asserting an implementation instead of a contract: it
> fires on a correct fix and goes quiet on a wrong one. This is checked
> mechanically at screening — a check whose expected literal equals a recorded
> pre-patch value is demoted and the match is reported.
>   * Every relation must still assert the DOCUMENTED contract of the observable
> you nominate — the javadoc formula, the declared @throws, the documented
> range, the documented family agreement — exactly as required everywhere else
> in these instructions. If the nominated observable has no documented contract
> you can assert, nominate a different observable; never fall back to the
> recorded values.
>   * A value MOVING is expected of a correct fix — that is what fixing is.
> Divergence here is not evidence of a defect and must never be reported as one.

The values themselves appear (prereg decision 1): an observable named without
its magnitudes is not enough to aim a relation at the right input region. No bug
id, project name, tool name or dataset identifier appears anywhere in the block
text — pinned by a test.

## 5. The anti-anchoring lint

`java_source.anchors_buggy_value(check, buggy_values)`, wired in
`relation_screen.screen_relations(divergence_values=…)` next to the other static
lints, in the existing `constant_receiver_state` / `probe_before_last_mutation`
demote pattern.

- **Input:** the BARE (untagged) pre-patch values of this leg's divergences. For
  a return-value divergence that is the value itself; for a receiver-state
  divergence the field dump is split back into fields and only the fields that
  actually MOVED contribute — no relation compares against a whole field dump.
  Every value in the list is by construction one that DIFFERS patched-side,
  which is the lint's precondition.
- **Match:** a literal the check COMPARES against — `x == "09"`, `n != -2`,
  `a.equals("09")`, `"09".equals(a)`, `a.compareTo(-2)`. Numerics are compared
  numerically, so `-2`, `-2L` and `-2.0` all match a logged `-2`.
- **Effect: DEMOTE + judge-visible fact, never a drop** (prereg decision 4).
  `rel.screen_demotion` is appended, which `_set_note` folds into
  `screen_note` — the note the prompt and the judge already read. A
  `record_event('screen', output='demoted')` lands in the trace.
- **Scope, honestly:** every comparison literal in the check body, not only the
  one compared against the probe result. Deciding mechanically which value IS
  the probe result is the analysis this codebase has repeatedly got wrong on
  real check shapes; the cost of the wider net is a note on a sound check, not a
  lost check — which is precisely why the prereg made this a demotion.
- **Inert without the flag**: `divergence_values` empty (its default) is a zero
  code path, pinned both signs by tests.

## 6. Artifact schema

In `result.jsonl` under `divcap`, once per leg, and as a `method=divcap` trace
event:

```json
{"divcap": {"status": "ok",
            "buggy_observations": 12, "patched_observations": 12,
            "divergences": [
              {"method": "org.example.Widget#indexOf(Object)",
               "observable": "return value",
               "input_shape": "q:\"abc\"|i:3",
               "buggy_value": "i:-1", "patched_value": "i:2",
               "count": 4}]}}
```

and the plan once per leg:

```json
{"divcap_methods": {
   "methods": [{"method_id": "org.example.Widget#indexOf(Object)",
                "file": "source/org/example/Widget.java", "line": 21,
                "observable": "ret"}],
   "skipped": [{"method": "org.example.Widget#reset()",
                "file": "source/org/example/Widget.java",
                "reason": "static void (no observable)"}]}}
```

The same plan is written into each build as `.divcap_methods.json`, so an
idempotent-skip rebuild hands it back without re-deriving it.

**The boundary, stated at the collection site (`run._record_divcap`):** the
divergences go to the relation-SYNTHESIS prompt (that is the mechanism) and to
the run artifacts. They are deliberately NOT passed to the relation verifier's
evidence, to the judge, or to any gate or verdict computation — a divergence is
not evidence of a defect, so a decision consuming one would be reading a signal
that means nothing about correctness. The only thing crossing into judge-visible
territory is the anti-anchoring DEMOTION note, which is a statement about the
relation, not about the patch. `tests/test_divcap.py` greps
`java.harness.prompts`, `java.relations.relation_verifier` and
`java.relations.judge_decision` for `divcap`/`divobs` — that test is the thing
that has to be deleted first if anyone ever wants to change this.

## 7. Enabling it, and what OFF means

```
uv run python src/java/run.py … --divcap
```

or `VULNPATCH_DIVCAP=1`. `DIVCAP_MAX_SHAPES` (64), `DIVCAP_TOP_K` (8) and
`DIVCAP_FLUSH_SECONDS` (2) tune it.

Default is OFF, and off is zero code path: no extra tree, no instrumentation,
no environment variable on any subprocess, no key in `result.jsonl`, no block in
any prompt, the screen lint inert, and `PatchedProjectBuilder`'s directory name
unchanged. The frozen guard fixtures and every historical baseline are
untouched. `--divcap` and `--diffcov` compose (`_diffcov_divcap` suffix) but the
capture builds its OWN trees either way, so the main fuzz path never runs
instrumented sources.

## 8. What is NOT verified yet — the first VM run must confirm

There is no JDK on the Mac, so nothing here has been compiled. Mac-side checks
are: javalang parses every instrumented source and the generated helper, the
helper is pure ASCII, generics-free, for-each-free and annotation-free, the
injection lands at the right offsets, every line keeps its number, and the
parse/pair/diff/rank/lint logic behaves on synthetic logs (47 tests).

The first VM run must confirm five things the Mac cannot:

1. **`defects4j compile` accepts `vulnpatch/DivObs.java`** on the oldest
   `-source` project in the suite (Chart / Lang), and picks the new file up from
   the derived source root. Same question diffcov's §6.1 asked, but this helper
   uses reflection, `TreeMap` and nine `ret` overloads.
2. **The rewritten METHOD BODIES compile** — this is the new risk diffcov did
   not carry. Specifically: the raw cast on a generic return type
   (`return (List) DivObs.ret(…)` where the method returns `List<String>`) must
   be an unchecked-conversion WARNING and not an error under the project's ant
   build; and the `try { … } finally { … }` wrap of a void body must not trip
   definite-assignment or unreachable-code analysis anywhere.
3. **The trigger-test safety net still passes on the instrumented patched
   build** — proof the injected calls are semantics-neutral end to end. If the
   instrumented tree fails a trigger the patched tree passes, the injection is
   changing behaviour and the mechanism is void.
4. **`VULNPATCH_DIVCAP_OUT` reaches the forked JUnit JVM** through
   `defects4j test` → ant → the junit task. If it does not, `divobs.out` is
   empty and the collector falls back to stderr — which is reliable at THIS site
   (a `defects4j test` JVM exits normally, so the shutdown hook runs, unlike the
   fuzz runner's SIGKILL path). Confirm which channel actually carried the
   lines; if it is stderr, confirm d4j does not swallow it.
5. **The trigger test actually enters the changed method on both builds** and
   produces at least one PAIRED argument tuple. Zero pairs is the honest
   negative result for this site (§2 limit 1) and must be readable as such —
   `buggy_observations`/`patched_observations` in the record separate "nothing
   ran" from "nothing agreed on a tuple" from "nothing moved".

Gates for the first validation roll are unchanged from the pre-registration
(G-V1 both signs on the frozen guard fixtures, G-V2 correct-leg canaries
Math-65-c and Chart-26-c, G-V3 the two-part Lang-63 prediction). Nothing in this
build turns the flag on.

## 9. Files

| file | role |
|---|---|
| `src/java/execution/divcap.py` | new — targeting, instrumentation, helper generation, `[divobs]` parsing, pairing/diff/ranking, the collection pass |
| `src/java/execution/fuzz_runner.py` | `PatchedProjectBuilder(divcap=…)`, `_instrument_divcap`, `_load_divcap_plan`, `build_divcap_buggy_dir`, directory suffix |
| `src/java/relations/relation_synth.py` | `divergence_block`, `synthesize(divergences=…)` |
| `src/java/relations/relation_screen.py` | `screen_relations(divergence_values=…)` + the anti-anchoring demotion |
| `src/java/parsing/java_source.py` | `comparison_literals`, `anchors_buggy_value` |
| `src/java/run.py` | `--divcap`, the step-4.55 collection site, `_record_divcap` (the boundary) |
| `src/config.py` | `DIVCAP`, `DIVCAP_FLUSH_SECONDS`, `DIVCAP_MAX_SHAPES`, `DIVCAP_TOP_K` |
| `tests/test_divcap.py` | 47 tests across targeting, injection, pairing/diff/ranking, the prompt block, the lint (both signs), and the two boundaries |
| `tests/test_universal_screen.py` | signature pin updated for the appended `divergence_values` |
