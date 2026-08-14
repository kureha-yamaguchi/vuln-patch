# Anatomy of the crashing-bug harness prompt

Reference for what goes into the LLM prompt on the **crashing-bug** path of
the Java pipeline, in order, and why each piece is there.

Assembled in [`harness/prompts.py`](harness/prompts.py) —
`PromptBuilder.build()`, the `bug_kind == "crashing"` branch (`prompts.py:106-155`).
Sections are built as lists of lines and joined with blank lines into a single
user message. (`bug_kind == "semantic"` takes a different branch,
`_build_semantic`, and is not covered here.)

The prompt is **rebuilt for every harness** in a campaign. Everything below is
fixed per bug except block 9, whose covered-functions / found-signatures /
accepted-families / assigned-strategy fields evolve as the campaign accepts
harnesses. Call site: `run.py:2161` (a closure the campaign invokes per attempt).

---

## The frame

### 0. System message — `prompts.py:148-155`
"You are an expert security engineer who writes Jazzer harnesses. Return a
single compilable .java file — no markdown fences, no prose." Output format
only.

## The rules of the game

### 1. Hard constraints — `_hard_constraints`, `prompts.py:424`
The non-negotiables: class named exactly `FuzzHarness`, one exact entrypoint
signature, this exact package (so package-private members are reachable without
reflection), raw Java only, must compile with `javac`.

Plus the honesty rule: reach the fault through the library's **real** code — no
mock, stub, subclass or anonymous class of the patched class or its callees. A
harness that manufactures the crash with its own implementation proves nothing
about real usage and is rejected.

### 2. Intro — `_intro`, `prompts.py:457`
One sentence naming the codebase and stating that the harness must call the
functions the patch touches.

## What the bug actually is (evidence)

### 3. The patch — `_patch_block`, `prompts.py:464`
The diff under analysis, verbatim, in `<patch>` tags. The thing being judged.

### 4. Imports — `_imports_block`, `prompts.py:472`
Import lines lifted from the modified file, so types are spelled correctly
instead of guessed.

### 5. One block per touched function — `_function_block`, `prompts.py:481`
Four nested things:

- **signature + full source** of the touched function;
- **call-site examples** (`xrefs`) — real callers in the codebase, as a template
  for constructing the target call;
- **related callees** — `_related_callees_block`, `prompts.py:539`. The methods
  this function calls and whose behaviour the patched code depends on, with
  their signatures, code (or `<contract>` when abstract) and concrete
  implementations. To reach the fault you usually need to drive the target
  through one of these with an input that makes its return value exercise the
  patched path.
- **state coupling / field siblings** — `_field_siblings_block`, `prompts.py:506`.
  Members of the same class that share a *field* with the touched function but
  have no call edge between them. State one writes is what the other reports, so
  a defect in that shared state stays observable through the sibling even when
  the patched function's own output looks right.

### 6. The failing-test block — `_failure_test_block`, `prompts.py:669`
The largest and most load-bearing section. Sub-parts:

- **Anchor-then-explore strategy.** (1) Call the target with the exact input
  from the test first — the guaranteed crash on the buggy build. (2) Identify
  the input *property* that triggers the patched line and fuzz many varied real
  inputs with that property. Overfitting patches special-case the seed.

- **Real entry point hint** — `_entry_point_hint`, `prompts.py:860`. Derives the
  production API class from the test class name (`StringUtilsTest` →
  `StringUtils`), steering the harness through the real call chain rather than
  poking the internal patched method directly.

- **Propagate-vs-swallow rules.** The false-positive firewall. A fixed version is
  *supposed* to reject invalid input, so any throwable that is a deliberate
  rejection — recognised by exception **family and context**, never by exact
  class or message text — must be caught and returned from. Propagate only when
  BOTH hold: the throwable signals the root cause (matches the ground-truth
  class, or is the harness's own assertion), AND its stack trace passes through
  a patched or `<root_cause_reachable>` frame. The block hands over the
  `isRootCause(t)` code shape explicitly.

- **Valid-by-construction rule.** When the ground-truth throwable is a
  validation exception, or a generic JDK runtime exception a method commonly
  leaks on malformed input (`StringIndexOutOfBounds`, `NullPointerException`,
  `ClassCastException`, `ArithmeticException`, …), its signature *cannot*
  distinguish the bug from correct rejection — a correct fix legitimately throws
  the same exception at the same line when the input really is invalid. Named in
  the code as the **#1 false-positive source**. In that case the exception may
  escape only for inputs built to satisfy the documented preconditions by
  construction (order the values, force the sign, supply non-null elements).
  Rule of thumb given: if a careful correct version would still throw there, it
  is not a bug.

- **Ground-truth crash** — `_crash_input_block`, `prompts.py:895`. Runtime
  evidence captured by actually running the trigger test on the buggy checkout:
  exception type, detail message, throw site, and the anchor literal to hard-code
  as the first call. Explicitly outranks anything inferred from the test body.
  The observed exception type also takes precedence over the statically declared
  one when building the crash-type list (`prompts.py:691-693`).

- **The failing test source**, with trigger calls highlighted into a
  `<key_calls>` block (`highlight_trigger_calls`), the body in `<failing_test>`
  (truncated at 1500 chars), and companions from `_test_context_blocks`
  (`prompts.py:823`):
  - `<real_failure_message>` — what the test actually printed on the buggy
    build. Ground truth naming the diverging observable and the wrong value; a
    faithful copy must reproduce exactly that value.
  - `<test_support>` — the test class's `setUp`, helpers, constants and fixtures.
    Replicating this instead of improvising setup is what prevents
    setup-divergence false alarms.

### 7. Documented preconditions — `_preconditions_block`, `prompts.py:595`
The `@param` / `@throws` / `@exception` lines from the touched methods' javadoc
(capped at ~900 chars). Closes a real gap: the valid-by-construction rule told
the model to satisfy the documented preconditions, but the documentation itself
was never in the prompt — it had to guess them. Renders empty when javadoc is
absent, leaving the rule standing alone.

Also carries the **rejection-ordering rule**: a documented rejection must be
re-probed after *every* state-changing call (install, register, add, remove,
clear), with the slot/key/index and the number of mutations drawn from the
`FuzzedDataProvider` — never literals. A probe placed only on the freshly built
object is silent on both builds for a state-conditional patch.

### 8. Sibling / state hints — `sibling_and_state_hints`, [`parsing/java_source.py:1243`](parsing/java_source.py#L1243)
Mechanically extracted from the touched file: same-name overload groups,
shared-prefix method families (3+ members), and the class's public no-arg
readers. Raw material for the two historically-winning invented check shapes —
sibling agreement and hidden-state mutation. Built once at `run.py:1807` and fed
to both this prompt and rule synthesis.

## How to be more than a crash reproducer

### 9. Variant analysis — `_variant_analysis_block`, `prompts.py:1006`
The part that changes between harnesses in a campaign:

- `<root_cause_reachable>` — the reachable region around the root cause, capped
  by `config.MAX_REACHABLE_IN_PROMPT`, with an explicit count of what was
  omitted.
- **Already covered** — functions hit and crash signatures found by earlier
  accepted harnesses, plus the uncovered remainder to steer toward. Re-triggering
  an already-found signature adds no evidence, so such a harness must instead win
  through a post-condition or metamorphic assertion.
- **Family novelty** — the `[oracle:<id>]` family stems already covered. The
  harness is rejected unless it fires a check outside them. Deliberately
  name-free and example-free: the model must invent a family, not copy one.
- **Independent oracle required** — once the set has any trigger, every new
  harness must carry at least one check that would still fire if the known
  symptom disappeared but a related quantity stayed wrong. A band-aid patch
  silences exactly the known symptom.
- **Variant strategy menu** — `_variant_strategy_menu`, `prompts.py:1179`:
  (a) different reachable function, (b) consistency cross-check on a helper
  masked by a self-correcting downstream step, (c) flip the patched condition
  (overfit on the seed). The campaign **assigns** one per harness so the set
  covers all three rather than the model repeatedly picking its favourite. The
  guardrail wording softens when `verifier_enabled` — with a soundness reviewer
  downstream, hedging to vacuous checks is the bigger risk.
- **Consistency checks** — `_consistency_hint_block`, `prompts.py:1112`. A
  *schema* with `<placeholder>`s, deliberately not a worked instance: a concrete
  example would hand over the answer whenever the evaluated bug happens to match
  it, overfitting to the dataset. Includes an explicit "what does NOT count"
  list (finite / non-NaN / non-null / no-throw) closing the loophole where the
  model satisfied the hint with the weakest member of the sound set, and three
  sound ways to obtain the independent value: limits the object itself states,
  recomputation from the object's own output, a second identically-constructed
  object.

### 10. Post-condition / metamorphic check — `_metamorphic_block`, `prompts.py:1266`
**Mandatory**, framed as "assume the adversary". The patch may (a) delete the
throw, (b) guard the crashing branch into unreachability, or (c) replace the
failing operation with one that silently does the wrong thing. In all three
worlds no exception fires and a crash-only harness passes the patch — the
dominant false-negative mode observed in evaluation. So the harness must also
assert one observable contract post-condition, or one metamorphic relation:
round-trip/inverse, idempotence, equivalent inputs, composition/split, or
oracle-from-the-input.

Attached hygiene rules, each learned from a false positive:
- if either side of a relation throws, the check does not apply — catch, skip,
  return. Never convert a caught exception into a violation.
- cite the documented guarantee behind each assertion in a comment; if you
  cannot cite one, do not assert.
- **fence degenerate inputs** — contracts are routinely silent about
  empty/blank cases.
- **fence extreme magnitudes** — at billion-scale parameters correct double
  arithmetic legitimately degrades (NaN, log-gamma saturation), and assertions
  that only fire there accuse correct code.
- **conditional side effects are not unconditional** — assert a guarded side
  effect only after establishing every guard on the path to it.
- real library calls on both sides, no hand-rolled reference implementation.
- every alarm carries an `[oracle:<short-id>]` prefix — checked mechanically;
  un-named alarms are invisible to the per-check acceptance machinery.

## The shape of the answer

### 11. FuzzedDataProvider reference — `_fdp_reference`, `prompts.py:1393`
An explicit whitelist of the ten `FuzzedDataProvider` methods that exist. The
model otherwise invents overloads that do not compile.

### 12. Skeleton — `_skeleton_block`, `prompts.py:1411`
A fill-in-the-blank file with a single `// >>> YOUR CODE HERE <<<` region, so
package, imports, class name and entrypoint cannot drift.

---

## Not present on this path

These blocks belong to `_build_semantic` only, because for a crashing bug the
escaping throwable *is* the oracle:

- `_lifted_assertion_block` (`prompts.py:249`) — lift expected values out of the
  trigger test's `assertEquals` and throw on mismatch.
- `_synthesized_relations_block` (`prompts.py:928`) — screened synthesized
  relation candidates.
- `_class_context_block` (`prompts.py:577`) — class-level skeletons, injected
  only into the `consistency` mechanism slot.
