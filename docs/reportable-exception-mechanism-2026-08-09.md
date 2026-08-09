# Reportable patched-only exceptions + rejection-probe ordering — 2026-08-09

**Status:** built, unit-tested on the Mac (875 passed, 7 skipped), NOT run on
the VM, NOT committed. Implements
`docs/reportable-exception-prereg-2026-08-09.md` as registered: Mechanism A
(two-tier catch) and Mechanism B (rejection-probe-after-mutation). Evidence:
`docs/draw05-routing-reread-2026-08-09.md` §4–§5; plan items 8.35, 8.36.

**Target — Mechanism A:** the relation-synthesis station
(`src/java/relations/relation_synth.py`: the `check:` output spec and
`_FOCUSED_FENCING`, which every focused pass reuses) plus the relation-screen
lint station (`src/java/parsing/java_source.py` lints, applied in
`src/java/relations/relation_screen.py:screen_relations`). The patch-changed
class names come from `src/java/run.py` (`_syn_cls`, already parsed off the
diff headers for `class_name`).

**Target — Mechanism B:** the same synthesis station (the REJECTION
INDEPENDENCE standing strategy), the harness-generation station
(`src/java/harness/prompts.py:_preconditions_block`, the block that already
carries the documented rejection contract), and one advisory screen lint.

**Failure mode addressed:** a relation builds an input it has itself declared
valid by construction, calls the patch-changed class with it, and the PATCHED
build throws. The old mandate caught that exception and returned, so the check
measured 0/N on both builds and was ranked as a well-behaved silent tripwire.
A patch that ADDS or MOVES a throw — refusing an input the contract requires
it to accept — was invisible to every relation shaped that way. Second failure
mode (B): a rejection oracle that probes once on the freshly built receiver
and only then mutates it has tested exactly the state a state-conditional
patch still gets right, however well the container is fuzzed.

**What is NOT touched:** the counting wrapper, the verifier, the judge, the
gates, the guard fixtures, every dismissal rule. No new dismissal logic was
added; precision comes from machinery that already exists (buggy-side
screening, fires-on-both attribution, the judge).

---

## 1. Mechanism A — the prompt diff

### 1a. The mandate that swallowed the signal (`_INSTRUCTIONS`, `check:` spec)

BEFORE (verbatim):

> ... and `throw new RuntimeException("relation <name> violated: "+...)` on
> disagreement. It MUST wrap the API calls in try/catch and RETURN (skip) on
> ANY caught exception — an exception is a rejection, never a violation.

AFTER (verbatim, the whole replacement):

> ... on disagreement.
> TWO-TIER CATCH (mandatory; checked mechanically). The two tiers treat an
> exception differently, because they mean different things:
> TIER 1 — SETUP (constructing the inputs and the receivers):
> `try { ... } catch (Exception e) { return; }`. An input or receiver that
> will not build is an input this relation skips — there a caught exception IS
> a rejection.
> TIER 2 — THE PROBE (the call(s) on the PATCH-CHANGED class named in the
> context above): its OWN separate try, NOT nested inside the setup try, whose
> catch RE-THROWS instead of returning:
> `try { r = changed.method(validInput); }`
> `catch (Exception e) { throw new RuntimeException("relation <name> violated:
> unexpected " + e.getClass().getName() + " on valid-by-construction input: "
> + e.getMessage()); }`
> You have already declared this input VALID BY CONSTRUCTION, so an exception
> raised inside the changed code on it is a RESULT the relation must report,
> not a rejection to skip. A blanket catch-and-return around the probe makes
> the check quiet on BOTH builds, so a patch that ADDS or MOVES a throw —
> refusing an input the contract requires it to accept — is invisible to every
> relation shaped that way. Setup exceptions stay rejections; probe exceptions
> are reportable.
> EXCEPTION TO TIER 2: a relation whose CONTRACT IS a rejection (the
> documented-@throws shapes above) keeps its TARGETED
> `catch (TheDocumentedException ok) { }` — that catch names the ONE exception
> the contract promises, which is evidence, not a swallow. Never a blanket
> catch-and-return around a probe either way.

The spec lives in the OUTPUT half of `_INSTRUCTIONS`, so `_OUTPUT_SPEC` —
reused verbatim by all four focused passes — carries it unchanged.

### 1b. STRUCTURE RULE (same block, kept, extended)

The rule that the violation throw sits outside the try is UNCHANGED for the
setup tier; one clause was added because the tier-2 rethrow makes nesting
newly dangerous:

> (this is why the tier-2 try must not be nested inside the tier-1 try: its
> rethrow would land in the setup catch and die there)

and the worked pattern now shows both tiers. Not decoration: a nested tier-2
rethrow IS caught by the setup catch, and the existing `violation_swallowed`
lint drops it — the prompt says why before the lint has to.

### 1c. `_FOCUSED_FENCING` (every focused pass)

BEFORE:

> - VALID BY CONSTRUCTION: build every input to satisfy the documented
> preconditions yourself (order/clamp/force valid before the call); catch and
> skip any exception — a rejection is never a violation.

AFTER:

> - VALID BY CONSTRUCTION: build every input to satisfy the documented
> preconditions yourself (order/clamp/force valid before the call). Catch and
> skip exceptions from the SETUP calls — a receiver that will not build is an
> input to skip. Do NOT skip an exception from the PROBE call on the
> patch-changed class: you have declared that input valid, so re-throw it as
> the violation with its class and message attached (the two-tier catch in the
> output spec below). Swallowing it makes the check quiet on both builds.

### 1d. The compile-repair path (`repair_check`)

`--rule_compile_repair` asks the model for a corrected snippet and can quietly
restore the old shape (draw-05 step [18] shows repair returning a body with no
try/catch at all). Added to its system message:

> KEEP THE TWO-TIER CATCH the snippet uses and do NOT introduce a blanket
> `catch (Exception e) { return; }` around a call on the patch-changed class:
> setup exceptions are rejections to skip, but an exception from the probe on
> a valid-by-construction input is the violation and must be re-thrown as
> `new RuntimeException("relation <name> violated: unexpected " +
> e.getClass().getName() + " on valid-by-construction input: " +
> e.getMessage())`.

### 1e. Naming the probe tier (context, `synthesize`)

"The call on the patch-changed class" is unsayable unless the prompt names the
class. `class_name` was a parameter of `synthesize` that reached nothing; it
is now rendered:

> THE PATCH-CHANGED CLASS: <name>. Calls on THIS class are the PROBE tier of
> the two-tier catch in the output spec: once you have declared the input
> valid by construction, an exception from one of them is a reportable result,
> never a rejection to skip. Calls on any OTHER class are setup, where a
> caught exception still means skip this input.

The name comes from the diff's `+++` headers, which the run already parses —
no new dataset knowledge, and no bug-shaped example anywhere in the new prompt
text (`test_new_prompt_text_is_dataset_neutral` enforces it).

---

## 2. Mechanism A — the lint and the normalisation

`src/java/parsing/java_source.py`, in the house lint style (pure string level,
fails soft, conservative, returns a reason a human can act on):

* **`patched_probe_swallowed(check_source, patched_classes)`** — the lint.
  Flags a try whose body calls a method ON a patch-changed receiver (static
  type from a declaration or a `new`) or statically on a patch-changed class,
  and whose broad catch (`Throwable`/`Exception`/`RuntimeException`) swallows
  the outcome (empty body, or a body that returns and never throws). Three
  conservative exits, each deliberate:
  - a try with ANY targeted (non-broad) catch is an expected-rejection
    contract — the shape that convicted draw-04 — and is never flagged;
  - a broad catch that rethrows, or that records the outcome in a flag the
    check later alarms on (the mandated documented-@throws shape), is not a
    swallow;
  - a CONSTRUCTOR call is not a probe: building the receiver is setup.
  With an empty `patched_classes` the lint is inert.

* **`rethrow_patched_probe(check_source, patched_classes, relation_name)`** —
  the mechanical normalisation the screen applies when the lint fires. It
  inserts, at the HEAD of the swallowing catch body, a frame guard:

  ```java
  catch (Exception e) {
      for (StackTraceElement __rpf : e.getStackTrace()) {
          String __rpc = __rpf.getClassName();
          if ((__rpc.equals("C") || __rpc.endsWith(".C") || __rpc.endsWith("$C"))
                  && !"<init>".equals(__rpf.getMethodName()))
              throw new RuntimeException("relation <name> violated: unexpected "
                  + e.getClass().getName() + " thrown by the patch-changed class"
                  + " on a valid-by-construction input: " + e.getMessage());
      }
      return;                    // everything else: still a rejection
  }
  ```

  **Why this shape and not a statement-level split.** The pre-registered
  replay-study transform rewrites "only calls whose static owner is the
  patch-changed class"; doing that textually means splitting a try body around
  one statement, and Java block scoping (locals declared inside the try are
  invisible after it) makes that transform miscompile on a large fraction of
  real bodies. The frame guard is the runtime twin of the same predicate —
  the exception was raised INSIDE the patch-changed class — needs no
  isolation, cannot mis-split, leaves every statement the model wrote in
  place, and is strictly narrower than "any exception from the probe try" (a
  setup call to another class that throws still returns). Constructor frames
  are excluded so a receiver refusing its own arguments stays a rejection. The
  same function is what the pre-registered replay study should import, so the
  study and the shipped pipeline cannot disagree about what gets rewritten.

* **Handling — fail-closed toward keeping the check.** In `screen_relations`
  the normalisation runs FIRST, before every other static lint, so all of them
  see the body that will actually be compiled. If the rewrite cannot be
  produced (no usable catch variable, unparseable clause), the candidate is
  KEPT and DEMOTED with the reason recorded on `screen_demotion` — never
  dropped. A blanket catch costs recall, not soundness; dropping would delete
  sound checks to punish a shape the prompt only just started asking for.
  Rewrites and demotions both emit a `record_event('deterministic',
  method='screen', ...)` line, so the trace shows every one.

**Why the counting wrapper needed no change.** `_screen_harness_source` counts
a firing when the escaping exception's message contains `violated` or its
class name contains `FuzzerSecurityIssue` (the `catch (RuntimeException e)`
arm). The tier-2 rethrow — model-written or inserted — is a `RuntimeException`
whose message starts `relation <name> violated: unexpected ...`. So the
counter, the `[relfire]` recording (message + consumed inputs + receiver
state), the buggy-side fire ratio, the replay attribution facts and the judge
all see an ordinary firing that happens to carry an exception class and
message. Nothing downstream learned a new case; the wrapper's semantics are
byte-for-byte what they were.
`test_counting_wrapper_is_unchanged_and_hears_the_rewrite` pins both halves.

**Where precision comes from (unchanged, all pre-existing):** a rewritten
relation that fires on the BUGGY build goes through the same fire-ratio /
direction machinery as any other firing; a patched-side firing the buggy build
also produces is dismissed by the existing fires-on-both / buggy-replay
attribution; the judge sees the exception class and message as the firing
message. No new dismissal rule, and nothing auto-dismisses on an ambiguous
signal.

---

## 3. Mechanism B — ordering

### 3a. Relation channel (`REJECTION INDEPENDENCE`, appended)

> ORDERING (mandatory, and the half most checks get wrong): re-run the
> rejection probe AFTER EVERY state-changing call in the check body, not once
> on the freshly built receiver. Mutate, then probe; mutate again, then probe
> again — asserting the same documented outcome each time. A probe that runs
> before the mutations has only ever observed the state a state-conditional
> patch still gets right, so the check is silent on both builds no matter how
> well the container is fuzzed; the divergence lives in the state AFTER the
> container has been filled, emptied, or left with a gap. Draw WHICH
> slot/key/index each mutation targets, and how many mutations happen, from
> `data` (never from literals), so the sequence of states the probe sees
> varies across iterations. This is checked mechanically at screening: a
> rejection probe with no probe after the receiver's last state change is
> reported as an ordering blind spot.

### 3b. Harness channel (`prompts._preconditions_block`, appended)

The harness channel never had the rejection-independence companion at all —
8.35's second leg. The standing instruction now renders with the documented
rejection contract it belongs to:

> REJECTION ORACLES — RE-PROBE AFTER EVERY STATE CHANGE. When you assert a
> documented rejection (asking for something absent, invalid or out of range
> MUST throw), run that probe again AFTER every call that changes the
> receiver's state — installing, registering, adding, removing, clearing — not
> once on the freshly built object. Mutate, then probe; mutate again, then
> probe again, asserting the SAME documented outcome each time. A correct
> rejection depends only on the probe itself being absent or invalid, never on
> unrelated receiver state, so it must hold in every state the object passes
> through; a patch that makes the rejection conditional on the container's
> contents, its size, or which slots are occupied diverges ONLY in the mutated
> states, so a probe placed before the mutations is silent on the buggy and
> the patched build alike. Draw WHICH slot/key/index each mutation targets,
> and HOW MANY mutations happen, from the FuzzedDataProvider — never from
> literals: fixed targets rebuild one shape every iteration, and the states
> where such a patch misbehaves (a gap between filled slots, an emptied
> container, a larger one) are never reached.

Placement bound, stated: the block renders only when the touched methods have
`@param`/`@throws` javadoc. Undocumented code gets no ordering rule from this
site. That matches the block's existing contract (it augments, never replaces)
and was preferred over stapling the paragraph onto an unconditional block that
is about something else.

### 3c. The lint (`probe_before_last_mutation`) — it did fit

Cheap, in the `constant_receiver_state` style, reusing the same helpers
(`_constructed_locals`, `_MUTATOR_PREFIXES`, `_REJECTION_EXC_SUBSTRINGS`):
flags a rejection probe (a call on a locally constructed receiver inside a try
whose catch names a rejection exception type) that is followed by a
state-changing call on that SAME receiver with no probe after it. Wired as a
DEMOTION, not a drop — its twin's handling, for the same reason: the check may
be sound and catch something else, and textual ordering is an approximation (a
probe inside a loop that mutates after it does re-run and is nonetheless
flagged). `constant_receiver_state` is the SHAPE half of this blind spot; this
is the ORDERING half. Both can fire on one check; the demotion suffixes
concatenate.

---

## 4. Files touched

| File | Change |
| --- | --- |
| `src/java/relations/relation_synth.py` | two-tier catch spec, structure-rule clause, focused-pass fencing, repair-prompt clause, patch-changed-class context line, REJECTION INDEPENDENCE ordering rule |
| `src/java/parsing/java_source.py` | `patched_probe_swallowed`, `rethrow_patched_probe`, `probe_before_last_mutation` + private helpers |
| `src/java/relations/relation_screen.py` | `patched_classes` parameter; Mechanism-A normalise/demote before the other lints; Mechanism-B demotion beside `constant_receiver_state` |
| `src/java/harness/prompts.py` | rejection-ordering standing instruction in `_preconditions_block` |
| `src/java/run.py` | `_patched_classes` off the diff headers, passed to both `screen_relations` call sites |
| `tests/test_reportable_exception.py` | 33 new tests (prompt text, lints, rewrite, wiring, wrapper-unchanged) |
| `tests/test_universal_screen.py` | the screen-signature guard now expects the appended `patched_classes` |

---

## 5. What the next fresh roll must read to validate Mechanism B

Mechanism A is replay-testable (the pre-registered study, phase 1 + 2).
Mechanism B changes what gets INVENTED, so no replay can show it; the next
fresh roll is the only evidence. Read, in this order:

1. **Did the shape appear at all?** In each leg's `trace.md`, the synthesis
   output: count relations carrying a tier-2 rethrow (`violated: unexpected`)
   and relations carrying a rejection probe that runs after a mutation. Zero
   of either means the prompt text did not land and nothing downstream is
   informative.
2. **Screen lines, per leg.** `[screen] <name>: PATCHED-PROBE SWALLOWED — ...
   — REWRITTEN` counts how often the model reverted to the blanket catch
   (prompt-adherence rate) and how often the normalisation had to carry it;
   `... — DEMOTED (rewrite not applicable ...)` is the fail-closed skip rate,
   which must stay small or the transform needs work.
   `[screen] <name>: PROBE BEFORE LAST MUTATION — ... — DEMOTED` is the
   Mechanism-B adherence counter: it should FALL relative to the last
   comparable roll; a leg where every rejection relation is demoted means the
   ordering rule was read and ignored.
3. **Harness channel.** In accepted harnesses on legs that HAVE documented
   `@throws` (the block renders only there), whether any rejection oracle
   re-probes after a state-changing call, and whether the mutation target is
   drawn from the fuzzer rather than a literal. That is the specific draw-05
   second-leg gap, and it is the one thing only a fresh roll can answer.
4. **Both signs.** The frozen guard fixtures (38-row
   `tests/fixtures/correct_dismissals.jsonl`, 67-row
   `docs/replay/backtrack/guard_population.json`) must decide identically
   through unchanged decision code, and the clean-leg hard-stop applies as to
   every mechanism. G-P remains the hard stop: any new patched-only tier-2
   firing on an archived CORRECT leg that survives the existing attribution
   facts means the mechanism does not ship as-is.

## 6. Deliberately not done

* **The harness channel's own blanket mandate.** `prompts.py` still tells the
  harness generator, in `_synthesized_relations_block` and
  `_metamorphic_block`, that "an exception is a rejection, not a wrong
  answer". The pre-registration scopes Mechanism A to the relation body and
  the relation screen, and the harness channel reports an escaping exception
  through a different path (crash attribution) with different precision
  properties. Changing it is a separate, separately-gated decision.
* **No verifier / judge / gate edits**, no new dismissal logic, no counting
  wrapper change — as registered.
* **No commit, no VM push.**

---

## Addendum (same day, main-agent verification pass): subtype delegation

The as-built probe-tier check matched receiver types against the DIFF
HEADER class names only. The dataset's normal delegation shape — the patch
lands in a base class whose protected methods a public subclass re-exposes
(Chart-19: `AbstractObjectList` patched, every archived relation probes
`ObjectList`) — would never have been flagged, which is exactly the shape
the mechanism was built for.

Fix: `subclasses_in_tree(root_dir, class_names)` in
`src/java/parsing/java_source.py` scans the buggy checkout for `extends`
declarations (textual, transitive to a fixpoint, test trees skipped), and
`run.py` extends `_patched_classes` with the result before the screen call
(printed as `[synth] probe tier includes patched-class subtypes: [...]`).
The runtime stack-frame guard already tested every name in the list, so
under delegation it still rethrows on the BASE class's frame — the frame
that actually throws. Tests: `test_subclasses_in_tree_finds_transitive_
subtypes`, `test_subtype_receiver_probe_is_flagged_and_rewritten`
(877 passed, 7 skipped).
