# Semantic-bug detection — failure analysis & fix brainstorm

Written 2026-07-16 after the `fixconfirm` run. Purpose: record *precisely*
what the pipeline did on each hard task on the VM, diagnose the underlying
problem, and lay out the general approaches to fix it.

Evidence base:
- `scratch/runs/fixconfirm_20260716_023210/` — validation run of the
  2026-07-16 prompt/context fixes (anchoring, changed-line distillation,
  conditional-side-effect fence, class-imports, silent-survivor cap) on the
  three diagnostic bugs, `PARALLEL=4`.
- `scratch/runs/sem8_v2_20260715_124137/` — the prior B2 gate for the same
  bugs.
- Developer fixes read from `defects4j/framework/projects/*/patches/*.src.patch`.

---

## TL;DR

The prompt/context fixes worked *internally* (synthesis now anchors on the
right region; distracting oracles are capped) but **did not change a single
outcome** — `fixconfirm` scored TP=1 FN=2 FP=1 TN=1 (Math-2/correct
regressed to *not-evaluated*), identical-or-worse vs `sem8_v2`. The reason:
the failures **relocated downstream**, and the deep-dive shows both surviving
failures share one root cause — **the pipeline sources its notion of
"correct" from the patch-under-test itself** (which may be the overfit), and
**over-extends point-sample truths past where they hold.**

> **⚠ 2026-07-16 (later) — MAJOR CORRECTIONS.** A log-level deep dive
> (all six fixconfirm legs + sem8_v2 counterparts + the pipeline source)
> falsified several factual claims below — the Lang-7 narrative is inverted
> (dev-fix patches are stored fixed→buggy and were read backwards), the
> Chart-26 FP has a different mechanism than claimed, and the Math-2
> regression is a corrupt dataset file. The per-task sections are kept for
> the record; read the **Addendum** at the bottom before acting on anything
> above it. The "Recommendation & next experiment" section is superseded.

---

## The core problem, stated precisely

The pipeline must build a fuzz-harness oracle that is **sound** (never fires
on a correct patch) yet **discriminating** (fires on the overfit), *without*
the developer fix — the one artifact that defines "correct."

The crux: a *plausible* overfit **passes every existing test by
construction.** So discrimination can only happen on **untested inputs** —
exactly where there is no ground truth for what "correct" means. The whole
task is reconstructing correct behaviour on untested inputs from indirect
signals. Ranked by trustworthiness:

1. **Failing / passing test assertions** — trusted, but only *point samples*,
   and they cluster on normal inputs.
2. **The buggy build** — correct *everywhere except the bug*; powerful, but
   you must know which inputs are "the bug."
3. **Javadoc / contract** — sound but usually silent on edge cases.
4. **Metamorphic relations** (round-trip, monotonicity, sum-to-one) — sound
   without knowing the exact output, *if genuinely universal*.
5. **The patch-under-test's own code** — **untrusted**; it may be the overfit.

Every false negative is "we failed to assert something the overfit violates";
every false positive is "we asserted something a correct impl can violate."

---

## Per-task: precisely what happened on the VM

### Lang-7 — FALSE NEGATIVE (overfit missed)

**The bug / dev fix.** `NumberUtils.createNumber` mishandles `"--"`-prefixed
strings. Dev fix adds `if (str.startsWith("--")) return null;` to
`createNumber` — i.e. **a `"--"`-prefixed string is invalid → returns null.**

**The overfit under test (Arja, `patch1-Lang-7-Arja-plausible`).** *Removes*
the `return null;` line, leaving an empty block:
```java
if (str.startsWith("--")) {
}
```
So on Arja, a `"--"` string falls through and is parsed as a normal number
→ throws `NumberFormatException`.

**What the pipeline did (fixconfirm, `01_Lang-7_Arja_o`):**
- Synthesis proposed, as its **first** relation,
  `double-minus-never-returns-null` — the anchoring fix worked: it is now
  probing the exact `"--"` boundary the patch changed (in `sem8_v2` it
  proposed only `createInteger`/hex round-trips, blind to `"--"`).
- Screen: the relation was **silent** on the buggy build (0/20000) and was
  the single injected survivor.
- The generated harness fired during acceptance with:
  `semantic mismatch: createNumber("--1.1E-700F") should throw
  NumberFormatException` / `expected NumberFormatException ... but returned
  null`.
- Result: `crashed_on_patch=False` → **FN**.

**Why it failed — the relation is BACKWARDS.** Correct behaviour is *returns
null*. But the harness asserts *should throw NFE* (never return null). On the
Arja overfit, `createNumber("--1.1E-700F")` **throws NFE** → satisfies the
(wrong) assertion → no fire → FN. On the *correct* patch it returns null →
the same assertion would **fire → a latent false positive.**

**Root cause.** Synthesis read the expected *output direction* from the
untrusted patch code (source 5). Arja *removed* `return null`, so synthesis
inferred "shouldn't return null." The **sound** oracle ("`--` returns null")
would have caught Arja cleanly.

**Decisive fact.** The Lang-7 failing test (`testCreateNumber`) asserts only
**valid-number** cases (`createNumber("1234.5") == Float(1234.5)`, …) — it
**never touches the `"--"` case.** So `"--"→null` exists *only* in the
developer fix, nowhere the pipeline may legitimately look. The correct
direction is not soundly derivable from test or contract here.

### Chart-26 — FALSE POSITIVE (correct patch flagged)

**The bug / dev fix.** `Axis.drawLabel` guards entity-adding with
`if (owner != null) { ... entities.add(new AxisLabelEntity(...)) }`. Dev fix
removes the `owner != null` wrapper so the entity is added whenever
`plotState`/`hotspot`/`entities` are non-null.

**What the pipeline did (fixconfirm, `06_Chart-26_Jaid_c`):**
- Synthesis proposed guard-aware relations
  (`owner-null-plotstate-preserves-axisstate`, `null-state-rejected`,
  `blank-label-noop`, …) — i.e. it **respected the conditional-side-effect
  fence**. All were silent on buggy; silent-cap injected 1.
- The false positive did **not** come from a synthesized relation. It came
  from a **generator-invented** oracle that fired on the patched (correct)
  build:
  `semantic mismatch: BarChart draw(g2, ..., null, null) expected
  success=true` / `GanttChartTests.testDrawWithNullInfo expected ... to
  succeed, but got java.lang.NullPointerException`.

**Why it failed — over-generalized lifted assertion.** The generator lifted
`testDrawWithNullInfo`'s "draw with null info succeeds" assertion (valid for
the **BarChart** type the test exercises) and applied it to a **different
chart type (GanttChart)**, where a correct implementation legitimately throws
NPE. The assertion is a *point sample* of correct behaviour; extended past its
tested type it is unsound → fires on the correct patch → FP.

**Root cause.** Generation extended a trusted point-sample (source 1) beyond
the domain where it is known to hold. Note this is a *different* unsound shape
than the `sem8_v2` Chart-26 FP (which was an invented "≥1 AxisLabelEntity
after draw" oracle) — the conditional-side-effect fence closed that shape, and
the generator simply invented another. Per-shape prompt patches are
whack-a-mole.

### Math-2 — FALSE NEGATIVE / NOT-EVALUATED (near-ceiling)

**The overfit under test (Arja) patches a DIFFERENT method than the bug.**
The bug is in `HypergeometricDistribution.getNumericalMean` (a `n*m/N`
reassociation). The Arja overfit instead edits
`AbstractIntegerDistribution.inverseCumulativeProbability`
(`upper = ceil(tmp)-1` → `lower -= 1`) — it passes the failing test by
coincidence while corrupting the bisection bounds.

**Why it is near-ceiling.**
- The original-bug signal (`getNumericalMean == n*m/N`) is the **last-ulp**
  float difference B1 measured — *unsound to assert exactly* (a correct
  reassociation differs in the last ulp; asserting equality FPs the correct
  leg).
- The overfit's own breakage (`inverseCumulativeProbability`) needs a specific
  input to manifest; synthesis proposed `inverse_cdf_monotone` /
  `quantile_brackets_p` (right area) but they did not reach the broken path.
- In `fixconfirm`, the Math-2 *correct* leg additionally built **no harnesses**
  (not-evaluated) — a slight regression from its `sem8_v2` TN, worth a look
  but not the main story.

---

## The general approaches to fix

### Problem 1 — synthesis trusting the patch's output (Lang-7 class)

- **1a. Source expected output only from test + explicit contract, never from
  patch code.** "The patched code may be wrong; assert an expected value only
  if a test asserts it or the javadoc explicitly guarantees it." *Effect on
  Lang-7:* it would **abstain** (nothing grounds `--→null`) rather than guess
  backwards — removes the latent FP but does not recover the FN. Cheap, sound,
  low recall payoff.
- **1b. Anchor on location, show the *buggy* code there, not the patched
  code.** Give synthesis the changed line *numbers* but display the buggy
  version at that spot, so it reasons about the contract, not the overfit's
  implementation. Same limitation when the contract is silent.
- **1c. Screen synthesized relations against the trusted test assertions.** A
  relation that *contradicts* a lifted test assertion is provably unsound →
  drop it. Free, always-correct soundness filter. (Would not catch Lang-7 —
  no `"--"` test exists — but catches other backwards relations.)
- **1d. Differential-on-changed-region — the most promising, currently
  absent.** *A plausible patch that modifies a code region but produces
  behaviour identical to the buggy build across that region's input domain has
  made a no-op change there* — it passed the failing test by coincidence, not
  by fixing the region. An overfit signature that needs **no** knowledge of
  the correct output.
  - *Lang-7:* anchoring already lands `"--"` inputs in the changed block. Run
    them on buggy **and** patched: buggy throws NFE; Arja (empty block) also
    throws NFE → **identical → no-op in its own changed region → flag overfit
    (TP).** Correct patch returns null → differs → not flagged. Never needed
    to know "correct returns null."
  - *Soundness condition:* holds when the correct fix changes the region
    **uniformly** (Lang-7: all `"--"` → null). FP risk if a correct fix
    changes only part of its touched region.
  - *Coverage limit:* fires only when the overfit actually *touches* the bug
    region. Math-2's Arja patched a different method → signal N/A there.
  - Composes directly with the working anchoring fix; this is plausibly the
    recall mechanism the pipeline has been missing for the "patched the region
    wrongly" class of overfits.

### Problem 2 — generation over-extending lifted assertions (Chart-26 class)

- **2a. Fence lifted assertions to the exact type/config the test
  exercised.** Capture the concrete receiver (BarChart) when lifting; forbid
  the harness from varying it. Sound; sacrifices some generalization.
- **2b. Screen generator-invented oracles through the buggy build (the "G3"
  fix).** On the buggy build, "draw(Gantt, null) succeeds" fires broadly (the
  buggy build throws for Gantt, unrelated to the bug) → dropped as
  out-of-domain. Mechanically catches the over-extension. Harder: the
  generator emits a whole harness, so the invented oracle must be extracted and
  compiled separately.
- **2c. Concentrate all beyond-seed oracles into the screened synthesis path;
  stop the generator inventing its own.** Generator's job becomes "reproduce
  the test + assert the *screened* relations." Removes the unscreened channel.
  Risk: loses whatever TPs only invention caught — needs an ablation first.

### Problem 3 — the irreducible core

Some bugs have **no sound, dev-fix-free oracle** on the discriminating inputs
(Lang-7's `--→null` is only in the dev fix; not in any test, barely in the
contract). Honest options:
- **Metamorphic relations** that sidestep exact outputs — did not help Lang-7
  (no round-trip/monotonicity structure in "reject `--`").
- **Cross-patch consensus** (previously rejected as leaky): if N unlabeled APR
  patches agree on an output and the candidate is the outlier, flag it.
  Recovers a signal without the dev fix, at the cost of needing multiple
  patches and a bias argument.
- **Accept the ceiling, measure breadth.** Exclude provably-uncatchable bugs
  from the certified-detectable denominator and spend effort on coverage.

---

## Recommendation & next experiment

Highest-leverage, soundest, least-explored: **1d (differential-on-changed-
region).** Pair with **1c** (test-contradiction screen — free soundness) and
**2a** (domain-fence lifted assertions — directly kills the Chart-26 FP class).
Hold **2b/2c** until an ablation shows how much recall the generator-invention
channel earns. Treat **Math-2 and "no sound oracle exists" cases as genuine
ceiling** rather than chase them.

**First experiment:** prototype 1d as a decision signal — over anchored
changed-region inputs, compare the patched build to the buggy build; flag when
they are identical across the domain. Test on the **Lang-7 pair** first: it
either flips Lang-7 to TP with the correct leg staying TN (validating the whole
idea) or reveals an FP mode we must bound. That one run tells us whether the
differential is the recall mechanism the semantic pipeline has been missing.

---
---

# ADDENDUM — 2026-07-16 (later): log-level deep dive, corrections, revised plan

Method: full read of all six fixconfirm `run.log`s + the four sem8_v2
counterparts, the pipeline source (`run.py`, `campaign.py`, `prompts.py`,
`relation_synth.py`, `relation_screen.py`, `relation_verifier.py`,
`fuzz_runner.py`, `analysis.py`, `certify_detectability.py`), plus direct
inspection of `defects4j/framework/projects/Lang/patches/7.src.patch` and
`drr/Patches/Dcorrect/SOFix/Math/patch1-Math-2-SOFix.patch` on the VM.
Every claim below is verified against a specific log line or file.

## A. Corrections to the account above

### A1. The Lang-7 narrative is inverted (patch-direction misread)

Defects4J `patches/*.src.patch` files are stored **fixed→buggy** — the `+`
lines are what gets ADDED to *produce the buggy version*. Verified on the VM:
`Lang/patches/7.src.patch` **adds** `if (str.startsWith("--")) return null;`
to `createNumber`, i.e. that block is the BUG. Correct behaviour for a
`"--"` string is **throw NumberFormatException** (the fixed `createBigDecimal`
carries the `throw new NumberFormatException(str + " is not a valid number")`
guard). Consequences, each verified in the logs:

- **The "decisive fact" above is false.** The failing test DOES cover the
  `"--"` case — its final block is literally
  `NumberUtils.createNumber("--1.1E-700F"); fail("Expected NumberFormatException");`
  (Arja run.log:860-861). The correct direction IS soundly derivable from the
  test; nothing needed to come from the dev fix.
- **`double-minus-never-returns-null` is not backwards** — it points the same
  way as the test. The genuinely backwards relation appeared on the **ACS
  (correct) leg**: `double-minus-prefix-returns-null`, whose logged rationale
  reads the buggy `return null` branch off source shown to it under the header
  "Patched method(s):" (see A7). So Problem 1 (direction sourced from shown
  code, not test/contract) is real — but its observed harm is a *latent-FP
  shape on the correct leg*, not the Lang-7 FN.
- **The Lang-7/Arja FN is (very likely) an environment ceiling, not an oracle
  bug.** Arja's empty-if falls through to `new BigDecimal("--1.1E-700F")`,
  which throws NFE on this JVM — extensionally identical to the dev fix. The
  overfit label encodes an OS-X-era JVM quirk (BigDecimal accepting `--`) that
  this VM does not reproduce. Arja **passes the lifted NFE oracle itself**
  (all 3 harnesses `clean run` on the patch). No black-box harness can
  separate them here. → **MEASURED (2026-07-16, follow-up):** a manual
  differential probe (Arja build from the run's checkout vs a fresh
  `Lang-7f` dev-fix build, JVM 11.0.31; ~50 shaped inputs through both
  `createNumber` and `createBigDecimal` — all suffix forms, hex,
  whitespace variants, multi-sign shapes, controls; probe + outputs at
  hetzner `/tmp/l7probe/`) found **zero strong divergences**: every
  difference is `NumberFormatException` on BOTH sides with different
  message text only (BigDecimal's parser message vs the dev guard's
  "X is not a valid number."). Message-only = WEAK kind by the B1
  classifier — exactly the divergence class that must never certify,
  since asserting message text FPs any correct patch that rewords.
  Lang-7/Arja is confirmed not-certified-detectable in this environment
  → move it out of the recall denominator, like Lang-22. (E2 done, in
  effect — worth re-running through `certify_detectability` proper only
  as a known-answer test of the tool itself.)
- **This also kills the 1d flagship example**: buggy returns null for `--`,
  Arja throws NFE — they DIFFER in the changed region, so
  differential-on-changed-region would *not* flag Arja. (1d survives on other
  evidence — see C3.)

### A2. The Chart-26 FP is a laundered pre-existing crash, not a type transplant

`GanttChartTests::testDrawWithNullInfo` is itself one of the 22 trigger
tests — it was never "lifted from BarChart and extended to Gantt." The FP
mechanism (c-leg run.log:5128): the harness's catch-all no-throw oracle
wrapped the **pre-existing `StringIndexOutOfBoundsException` in
`G2TextMeasurer.getStringWidth`** — triggered by malformed-Unicode fuzzed
titles (raw `consumeString` produces isolated surrogates) — into
`FuzzerSecurityIssueLow("... expected chart.draw(...) to succeed, but got
java.lang.StringIndexOutOfBounds...")`. Three compounding failures:

1. **A2-attribution bypass**: the attribution check skips harness-own
   exception classes (`FuzzerSecurityIssue*`) by design; wrapping a generic
   escape in an oracle exception launders the *known pre-existing* crash
   (sem8_v2 had explicitly classified this exact SIOOBE as pre-existing!)
   past the only mechanical defence. This is the Lang-27 FP class again,
   in a costume.
2. **Unfenced Unicode**: any no-throw oracle over raw `consumeString` chart
   text is a guaranteed eventual FP on every build.
3. **Verifier variance at votes=1**: within the SAME run it dropped
   attempt_002 with exactly the right reasoning ("nothing guarantees
   malformed Unicode must be renderable", log:5123) and kept attempt_004 for
   the same exception class from the same cause (log:5129).

Also: the o-leg TP is REAL and sound — 2 of 3 harnesses fired the injected
synthesized relation `nonempty-label-adds-axis-label-entity` (before=0
after=0 on the `hotspot=null;` patch). The silent-survivor cap + imports fix
demonstrably worked here; the `accepted_trigger_details` Gantt entries are
buggy-gate triggers, not the TP-deciding firings. And the conditional-side-
effect fence did close the old "≥1 entity after draw" shape — the recurring
FP class is the *no-throw-lift-over-fuzzed-inputs*, which was never fenced.

### A3. Math-2/correct is a corrupt dataset file, not a pipeline regression

`patch1-Math-2-SOFix.patch` is **truncated** ("corrupt patch at line 11 …
patch unexpectedly ends in middle of line") **and stored reversed** (its
post-state equals the buggy code; `patch --forward` says "Reversed (or
previously applied) patch detected"). It has NEVER applied — sem8_v2's log
contains the byte-identical failure. **sem8_v2's Math-2 TN was a phantom**:
its summary mapped `crashed_on_patch=False` → TN without checking
`status=no_harnesses`. fixconfirm merely reports honestly (`NOT-EVALUATED`).
Bonus hazard: the reversed patch also means the LLM was shown the overflow
formula as "what the correct patch installs." → Actions: fix the file;
dry-run-apply every drr patch at suite-sourcing time and fail loudly;
recount historical aggregates for phantom TNs (sem8_v2 P=0.80 is inflated).

### A4. NEW systemic bug: relations self-swallow their own violations

Every logged synthesized snippet — both runs, both legs, both bugs — has the
shape:

```java
try {
  ... call API ...
  if (violation) throw new RuntimeException("relation X violated: ...");
} catch (Throwable t) { return; }   // catches its own violation throw
```

The catch eats the violation before Jazzer sees it. Consequences:
- **Screen fire-ratios are corrupted.** Lang-7's `double-minus-never-
  returns-null` should have fired ~20000/20000 on buggy (buggy returns null
  for nearly every `--` input); measured **0/20000** and injected as the one
  "silent survivor." The screen certified a vacuous check.
- The pattern propagates into generated harnesses (~half of relation
  implementations across the runs are inert at fuzz time; Math-2 o-leg
  attempt 001 lost its acceptance to it).
- The prompt's own hygiene rule ("wrap the API calls in try/catch and SKIP
  on any caught exception") invites exactly this scoping error.

"Silent on buggy" currently conflates three states: *holds on correct code*,
*encodes the bug itself* (ACS-leg backwards relation), and *structurally
vacuous* (self-swallow). Only the first deserves injection.

### A5. First-oracle shadowing + gate spoofing

The acceptance gate is whole-harness: any one oracle firing on buggy accepts
ALL of the harness's oracles. On the buggy build an early always-firing block
makes every later block **dead code** — Chart-26 c-leg attempt_003's FP
oracle was executed for the first time ever on the correct-patch build.
Worse, o-leg attempt_001 passed the buggy gate via the pre-existing SIOOBE,
not the bug. Per-oracle attribution exists only as inert metadata
(`accepted_trigger_details`).

### A6. Math-2/Arja FN anatomy (three separate causes)

1. The harnesses DID hammer `inverseCumulativeProbability`; but Arja's
   `lower -= 1` preserves the bisection bracket invariants
   (`cdf(lower)<p ≤ cdf(upper)`) on every reachable input. Discriminating
   regimes — support lower bound `Integer.MIN_VALUE` (underflow →
   `lower>upper` → wrong quantile) or int-overflowing `n*m` — were outside
   every harness's fuzz ranges (≤1e6×256 < 2^31).
2. **The one discriminating relation was injected but un-implementable**:
   `minValueLowerBoundMedianQuantile` needs an anonymous
   `AbstractIntegerDistribution` subclass; the harness rules forbid
   subclassing the patched class; the model silently dropped it.
   Injected ≠ implemented. (The screen compiled it fine — screen and harness
   operate under different constraints. Also: a real library subclass,
   `UniformIntegerDistribution(Integer.MIN_VALUE, hi)`, reaches the same
   code without violating the rule — nobody told synthesis.)
3. **The convicting oracle existed one leg over.** The SOFix leg synthesized
   `mean_matches_documented_formula` (javadoc-grounded, tolerance-gated —
   sound and dev-fix-free). Arja's build still returns
   `getNumericalMean = -49.76`; that relation IS the Arja TP. Patch-relative
   anchoring never pointed the Arja leg at `getNumericalMean` because Arja
   patched a different method. This is the general blind spot: **an overfit
   that patches away from the root cause escapes every oracle anchored on
   its own diff.**

### A7. Context-layer defects found in the source (all cheap to fix)

- **Synthesis never sees the failing test** — `trigger_summary` is passed as
  `''` (run.py) and mining excludes trigger tests. The single most trusted
  direction source is withheld from the stage that needs it most.
- **Source shown to synthesis is the BUGGY body under the header "Patched
  method(s):"** (relation_synth.py:172-174) — the model is told the buggy
  code is the patch's code. Direct cause of the ACS-leg backwards relation.
- **Changed-line distillation strips the +/- signs** (relation_synth.py:
  204-208) — added and deleted lines are indistinguishable in the block that
  says "your first relation must target the behaviour THESE govern."
- **Class-skeleton truncation cut `createNumber`'s own javadoc** (skeleton
  truncated after `toShort`) — the documented null/NFE contract existed and
  was never shown. Truncation should prioritise touched-method javadoc.
- **Lifted tests are shown without their `setUp()`/receiver construction**
  (Chart-26: `this.chart` construction invisible) — forcing the model to
  invent fuzzed constructions, which is exactly where the FP entered.
- relation_synth.py's docstring claims diff-grounding "is safe ONLY because
  every candidate is mechanically screened downstream" — false: the screen
  is a fire-ratio filter; silent-but-backwards candidates are precisely what
  the silent-survivor slot injects.
- Minor: the unused 5th prompt is fully rendered+logged after convergence
  (~10-12k tokens/leg waste); `parallel` consistency-slot starvation when
  mechanism list has one entry (skeleton only renders under 'consistency').

## B. Revised problem statement

The fixconfirm scoreboard decomposes into FOUR unrelated causes, none of
which is the "irreducible core" of §Problem 3:

| Outcome | Real cause | Class |
|---|---|---|
| Lang-7 FN | JVM-dependent extensional equivalence (dataset/env ceiling) | measure it, don't chase it |
| Chart-26 FP | laundered pre-existing crash + unfenced Unicode + verifier variance | mechanical, fixable |
| Math-2 FN | diff-only anchoring + un-implementable relation + narrow fuzz ranges | scoping, fixable |
| Math-2 n/e | corrupt+reversed dataset patch file | dataset, fixable |

The headline P=0.50/R=0.33 is dominated by mechanical bugs and dataset
defects, not by oracle-hardness. The "sound oracle doesn't exist" wall is
real but was not what these three bugs hit.

## C. Revised fix list (priority-ordered)

### Tier 0 — mechanical correctness (cheap; do before any new mechanism)

- **M1 self-swallow lint + screen canary.** Reject (or auto-rewrite) any
  relation snippet / harness assert block whose violation throw is caught by
  its own `catch (Throwable)`. Syntactic check, no LLM. Companion: a screen
  plumbing canary — compile a forced-violation variant of each candidate and
  require the counter to register ≥1, so a vacuous check can never measure
  "silent." (Fixes A4; makes every screen statistic trustworthy.)
- **M2 cause-chaining + attribution unwrap.** Oracles must attach the caught
  exception as cause (`new FuzzerSecurityIssueLow(msg, e)`) — prompt rule +
  mechanical check that the catch variable is passed. Extend the A2
  attribution step to unwrap the cause (or parse the trailing "got
  java.lang.X"), replay the firing input on buggy, and drop when the same
  generic crash reproduces underneath. Kills the Chart-26 FP class = the
  generalized Lang-27 fix. (Fixes A2-1.)
- **M3 per-oracle IDs + full oracle enumeration on buggy.** Unique ID in
  every violation message; acceptance fuzzing continues past the first crash
  (fork mode / `-ignore_crashes`, or per-block sub-harnesses) and records
  WHICH oracle IDs ever fire on buggy. Any oracle never exercised on buggy
  is *latent dead code* → deliberately exercise or strip it before the
  patched run. This gives EVERY oracle — generator-invented included — the
  buggy-build screen, mechanically implementing option 2b without LLM
  oracle-extraction. (Fixes A5; would have exposed both Chart-26 FP oracles
  before they met the correct patch.)
- **M4 dataset integrity gate.** Dry-run-apply every drr patch (and the
  defects4j dev patches used for certification) at suite-sourcing; fail
  loudly. Fix `patch1-Math-2-SOFix.patch`. Recount historical summaries
  excluding phantom TNs. (Fixes A3.)
- **M5 well-formed-string fence.** Fuzzed strings default to valid UTF-16
  (fence `consumeString`, or post-filter surrogates) unless the documented
  contract covers arbitrary byte strings. (Closes the recurring SIOOBE
  entrance; general, not bug-shaped.)

### Tier 1 — direction & contract grounding for synthesis

- **G1 feed synthesis the trusted direction sources.** Pass the failing
  test(s) + their assertion/expected literals into the synthesis prompt;
  guarantee touched-method javadoc survives skeleton truncation; fix the
  "Patched method(s):" mislabel (label it BUGGY); keep diff signs in the
  distillation; additionally render the patch-applied "after" view next to
  the buggy "before" view (mechanical patch application to `func_source`).
  Include lifted-test `setUp()`/receiver construction. (Fixes A7.)
- **G2 direction screen — 1c in executable form.** Run each candidate's
  check on the failing test's own trigger inputs against the buggy build:
  a sound relation must agree with the test's verdict there (fire where the
  test fails on buggy; hold where it passes). Kills backwards relations
  (the ACS-leg shape) mechanically, before injection. Cheap: inputs are
  known literals, build already exists.
- **G3 constraint parity + implementability.** The screen compiles
  candidates under the same rules the harness must obey (no anonymous
  subclass of the patched class, etc.), and the synthesis prompt carries
  those constraints plus the escape hatch ("use existing library subclasses
  — e.g. a concrete distribution whose support starts at
  Integer.MIN_VALUE — not anonymous ones"). Track injected→implemented:
  if the generator drops an injected relation, say so in the log. (Fixes
  A6-2.)
- **G4 symptom pinning for lifted no-throw oracles.** When a lifted
  assertion is exception-shaped ("must not throw"), record on the buggy
  build WHAT actually fired (class@frame, e.g. `NPE@Axis.drawLabel`) and pin
  the generalized oracle to that signature: on the patched build it fires
  only for the same class@frame. Sound (a correct patch cannot re-produce
  the bug's own signature; an incomplete fix on other inputs can), and it
  converts the catch-all no-throw lift — the recurring FP channel of BOTH
  Chart-26 runs — into a targeted regression oracle. Complements M2.

### Tier 2 — recall mechanisms

- **R1 root-region anchoring (new; predicted Math-2/Arja TP).** Anchor
  synthesis on the UNION of (the patch's changed region) ∪ (the failing
  test's implicated region — the class/methods its assertions exercise on
  the buggy build). Overfits frequently patch away from the root cause
  (Math-2/Arja class); the root region's documented contract (the javadoc
  mean formula) remains violated on the overfit build and is fully
  dev-fix-free. This is the cheapest genuinely-new recall mechanism and it
  reuses existing machinery (synthesis+screen) with a wider anchor.
- **R2 bug-level relation pooling.** Synthesis inputs are patch-independent
  except for anchoring (buggy source, javadoc, tests); pool screened
  relations across ALL candidate patches of the same bug — every leg gets
  the union. No label leakage: all candidates are unlabeled, and anchoring
  on other candidates' changed regions is legitimate "where might behaviour
  differ" evidence (much weaker than the rejected consensus-voting). Gets
  `mean_matches_documented_formula` onto the Arja leg even without R1.
- **R3 buggy-differential (1d, revised expectations).** Keep the idea; fix
  the claims: it can flag Math-2/Arja (its changed region is behaviorally
  ≈ buggy everywhere reachable — a no-op-in-region) but NOT Lang-7/Arja
  (null→NFE is a real change). Implementation is ~80% existing plumbing:
  `certify_detectability`'s probe machinery with the dev-fix classpath
  swapped for the buggy classpath, reusing its divergence-kind classifier
  (ulp / message-only / generic-latent must not count as "changed
  behaviour", or a correct numeric fix reads as a no-op). Decision rule must
  EXCLUDE the failing test's own inputs — every plausible patch differs from
  buggy there by construction. FP bound to characterize: a correct fix whose
  entire behavioural footprint is inside the excluded neighbourhood.
- **R4 verifier consistency grouping.** Group fired oracles by (oracle ID,
  cause class@frame); one verdict per group, applied uniformly; a KEEP for a
  group that a sibling DROP already judged unsound must reconcile (default
  drop). Directly fixes the A2-3 within-run contradiction; cheaper and more
  targeted than votes=3 (which the replay already showed doesn't move leak).
- **R5 behavioural adjudication for direction (creative, LLM-cheap).** When
  synthesis/generation wants an expected-value direction the test doesn't
  pin: run the concrete inputs on the buggy build, and show the LLM the
  OBSERVED tuple (input, buggy output, javadoc, test expectations) — asking
  "which direction is contract-consistent?" — instead of letting it infer
  direction from code reading. Converts the direction problem from static
  inference into judging concrete observations; the Lang-7 ACS case
  (buggy=null, javadoc says invalid→NFE, test expects NFE) becomes trivial.

### Tier 3 — evaluation hygiene & tests to build

- **E1 dev-fix soundness audit (offline eval ONLY — same firewall as
  certify_detectability).** Run every accepted harness against the
  dev-fixed build; ANY firing = unsound oracle, labeled per oracle ID.
  Free, immediate FP ground truth for iterating prompts/verifier — no more
  waiting for a correct-leg FP to expose a channel. Never in the verdict
  path.
- **E2 certify Lang-7/Arja** (vs dev fix): expect 0 strong divergences →
  reclassify as not-certified-detectable; fixconfirm's honest ceiling is
  then Math-2 + Chart-26, both addressable.
- **E3 unit tests**: self-swallow lint fixtures; screen canary; direction
  screen on the known backwards relation (ACS-leg snippet is a perfect
  fixture); M2 unwrap on the logged Chart-26 crash; dataset dry-run gate.
- **E4 replay-driven verifier iteration** continues (verifier_replay), now
  with cause-grouped cases from M2/M3 telemetry.

## D. Literature anchors (general methods, not bug-shaped)

- **PatchSim** (Xiong et al., ICSE'18) — dev-fix-free patch correctness via
  execution-behaviour similarity: correct patches preserve passing-test
  behaviour and change failing-test behaviour. Our buggy-differential (R3)
  and symptom pinning (G4) are the fuzzing generalizations of its two
  halves.
- **Opad** (Yang et al., FSE'17) — fuzz + implicit oracles (crash/memory)
  to reject overfit patches. Exactly this pipeline's crashing-bug story;
  its silence on semantic bugs is why value-level differentials vs the
  buggy build (R3) are the natural extension.
- **DiffTGen** (Xin & Reiss, ISSTA'17) — test generation targeting the
  syntactic delta's behaviour difference vs a reference patch.
  `certify_detectability` is its dataset-side twin; R3 swaps the reference
  from dev-fix to buggy.
- **Repair anti-patterns** (Tan et al., 2016) — functionality-deletion,
  null-assignment, early-return insertions are overfit-shaped. Chart-26-o
  (`hotspot=null;` insertion) and Lang-7/Arja (branch emptied) are textbook
  instances. Worth a *static prior* that routes suspicious patches to a
  bigger probe budget (never a verdict on its own — deletion patches CAN be
  correct, e.g. the Lang-7 dev fix itself deletes the null-return).
- **Invalidator** (Le-Cong et al., 2023) / **ODS** (Ye et al., 2021) —
  invariant- and static-feature-based overfit classifiers; corroborate that
  behavioural invariants + syntactic priors carry real signal without the
  dev fix.
- **Metamorphic testing** (Chen et al., survey 2018) — the relations
  machinery; MT literature's standard "validate MRs against known outcomes
  before use" step is exactly G2, which the pipeline has been missing.

## E. Superseded recommendation → new experiment queue

The previous "prototype 1d on the Lang-7 pair first" is RETRACTED: 1d cannot
flag Lang-7/Arja (its changed-region behaviour genuinely differs from buggy),
and the pair is likely undetectable in this environment anyway (A1). New
queue, cheapest-decisive-first, all on the 3-bug set before any flagship
sweep (per the iterate-cheap rule):

1. **M4 + M1 + M2** (dataset fix, self-swallow lint, cause-chain unwrap),
   then rerun fixconfirm. Prediction: Chart-26 FP→TN, Math-2/correct→real
   TN, Lang-7 unchanged → P=1.0 on the suite with zero new mechanisms.
   If Chart-26 c-leg still FPs, the residual channel is the unexplained
   attempt_003 entity-identity firing (worth replaying its crashing input
   regardless — see agent note: it should be impossible on the shown patch).
2. **E2** certify Lang-7/Arja → denominator decision (expect: excluded).
3. **R1 + G2** (root-region anchoring + direction screen), rerun the Math-2
   pair. Prediction: Arja FN→TP via the mean-formula relation; SOFix leg
   (post-M4) TN via tolerance gate.
4. **R3** differential prototype on the Math-2 pair (now that SOFix
   applies): does no-op-in-region flag Arja while sparing SOFix? This is
   the clean test of 1d — Math-2, not Lang-7, is its natural first case.
5. Only then: mechanism-bearing semantic8/diag24 sweep for headline numbers.

## F. Follow-up notes (2026-07-16, same evening — design discussion)

### F1. The no-subclass rule: keep the intent, narrow the scope

The rule (prompts.py:398-404, "do NOT write your own subclass, anonymous
class, mock, or stub of the patched class or any of its callees") exists for
a sound reason — a harness that hand-builds a stand-in can manufacture its
own crash and the firing proves nothing about the real patch. Keep that. But
it is too blunt for ABSTRACT patched classes: implementing only the
*abstract* members of `AbstractIntegerDistribution` (cdf, support bounds)
while inheriting the patched concrete `inverseCumulativeProbability`
executes the real patched code — the standard way to test an abstract class.
The dangerous act is specifically **overriding a concrete/touched method**,
not instantiating the abstraction. Refinement (extends G3):

- Allow anonymous/local subclasses that implement ONLY abstract members;
  forbid overriding any concrete method of the patched class or its callees.
- Make it mechanical, not prompt-text: scan the harness for
  `new TouchedClass(...) {` bodies and diff the overridden method names
  against the abstract-member list.
- Keep the prompt escape hatch: "prefer an existing library subclass"
  (Math-2: `UniformIntegerDistribution(Integer.MIN_VALUE, hi)` reaches the
  underflow path with zero rule tension).

### F2. Self-swallow fix: effort estimate + a second-order screen trap

Effort ≈ half a day, three layers (refines M1):

1. **Prompt/template** (minutes): violation throws must be
   `FuzzerSecurityIssueLow`; every catch must rethrow it first —
   `catch (Throwable t) { if (t instanceof FuzzerSecurityIssueLow) throw
   (FuzzerSecurityIssueLow) t; return; }`. The model already produces this
   idiom sometimes (Lang-7 Arja attempt 001).
2. **Mechanical lint** (~50–100 lines; javalang already a dependency):
   reject any snippet/harness where a throw whose message contains
   `violated`/`semantic mismatch` sits inside a try whose catch swallows it
   without rethrow; wire rejections into the existing repair-turn loop. The
   logged Lang-7 snippet is a ready-made fixture.
3. **Screen canary** (~30 lines): compile a forced-violation variant of each
   candidate and require the counter to register ≥1 before trusting any
   "silent" verdict. (The screen wrapper itself is fine — it counts at the
   runCheck call site; the candidate's own catch eats the throw first.)

**Trap — fix M1 and the screen cap together.** Post-fix, Lang-7's relation
fires ~20000/20000 on buggy, and `MAX_FIRE_RATIO = 0.20`
(relation_screen.py:50) would drop it as out-of-domain. The 20% cap assumes
"buggy is correct almost everywhere", but an ANCHORED relation concentrates
its inputs on the changed boundary — where buggy is wrong everywhere. So the
lint alone converts vacuous-keeps into good-relation-drops. Correct
semantics: a high fire ratio disqualifies only when it CONTRADICTS the
failing test's direction; when the relation agrees with the test (G2), a
high ratio on buggy is confirmation. I.e. the screen verdict must become
direction-aware: (ratio, G2-agreement) → keep/drop, not ratio alone.

### F2b. Suite-wide certification sweep — RESULTS (2026-07-16, ~45k tokens)

All 7 previously-unexamined suite overfit legs certified (JVM 11.0.31
stamped; records in `scratch/eval_expansion/certified_suite{,_v2}.jsonl`):

| Leg | Strong div | Verdict |
|---|---|---|
| Lang-27 / DeepRepair | 30 | detectable |
| Lang-43 / Arja | 39 | detectable |
| Lang-55 / Arja | 214 | detectable |
| Chart-5 / DeepRepair | 396 | detectable |
| Chart-19 / Arja | 14 | detectable |
| Math-2 / Arja | **0 → 117** (see below) | detectable |
| Math-70 / SketchFix | 0 (v1 and v2) | **undetectable** |

**Math-2/Arja was a FALSE ZERO that validated the A6/R1 diagnosis at the
certifier level.** The v1 probe (0 div / 549 lines) anchored on the patched
method — whose outputs are identical on both builds even at overflow
parameters (`icdf(0.5)` agrees; verified by hand-probe). The divergence is
visible ONLY via `getNumericalMean` (Arja −49.76 vs dev fix +49.82 at the
failing test's own parameters) — a sibling observable the patch never
touched. Fix applied to `_PROBE_INSTRUCTIONS`: probes must cover TWO
surfaces — (a) the patched method's boundary grid AND (b) the failing
test's object surface (every public accessor at the test's exact
constructor args, plus magnitude variations). Re-run: 117 strong value
divergences. **Corollary: a differential FUZZER would NOT have caught
this** — a fuzzer anchored on the patched method inherits the same blind
spot. The under-coverage failure mode is anchoring scope, not input
randomness; the fuzzer build (F-question from the design discussion) is
therefore NOT currently justified. Revisit only if a widened-surface probe
zero is ever shown false.

**Math-70/SketchFix is a genuine Lang-7-class exclusion** — the patch IS
the dev fix plus a dead disjunct (`|| i < 0` on a loop counter starting at
0). Extensionally identical on every JVM. Added to
`suites/labels/incorrect_labels.md`; diag24 header updated (3 expected permanent FNs:
Lang-7, Lang-22, Math-70).

**B3 expansion-pool sweep + zero-divergence deep-dive (same day, pm):**
61 legs probed across all 33 paired bugs (~575k tokens? no — ~350k; exact
totals in b3_sweep.log). Headline findings, full detail in
`suites/labels/incorrect_labels.md`:
- 14/28 overfit legs certified detectable outright; 11 zeros; 3 infra.
- Deep-dive split the zeros 5/5: **five GENUINE equivalences** — of which
  **Math-59/SequenceR's "overfitting" patch IS the developer fix
  textually**, and Closure-115/ssFix + Closure-86/SequenceR-patch2 are
  extensionally ≡ the dev fix (dead leftover code) — these are drr LABEL
  ERRORS, not environment ceilings; and **five FALSE ZEROS** with
  empirically executed witnesses (Chart-7, Lang-41/Arja, Lang-60,
  Closure-62, Math-57) — even the v2 widened probe misses divergences that
  live on sibling overloads, observer state (a query method that mutates
  capacity), non-default configs, or float-width extremes. Probe-v3
  instruction candidates recorded in suites/labels/incorrect_labels.md.
- Correct-side final verdicts: **Lang-50/SimFix = pipeline patch-APPLIER
  bug** (hunks in descending line order → the applier silently applied
  only the first; the certified build even fails the trigger test; with
  both hunks applied: 0/518 divergences, label stands). The applier is
  shared with the main pipeline → **fix out-of-order hunk handling, add a
  run-trigger-tests-on-patched-build invariant, and audit all multi-hunk
  drr patches for silent drops.** **Lang-41/SimFix = TRUE MISLABEL**
  (partial fix leaving the root-cause String helpers broken) — and the
  Doverfitting Lang-41/Arja patch is **byte-identical** to it: drr labels
  the same patch both correct and overfitting. Net: 31/33 correct labels
  stood on their merits, 1 true mislabel found, 1 pipeline bug found — the
  mislabel probe is earning its keep and should run over the whole
  Dcorrect pool.
- Closure-63 is deprecated in the installed Defects4J → out of the pool.
- Estimated true undetectable-in-env rate among overfit legs: ~20%
  (5–6 of ~25 evaluable), and more than half of those are label errors
  rather than platform contingencies.

Updated denominators: every ACTIVE o-leg in semantic8 is now certified
detectable; diag24 has 3 certified-undetectable legs kept only for
baseline comparability. Any future FN on a certified leg is a technique
failure worth debugging; any FN on an suites/labels/incorrect_labels.md leg is expected.

### F2c. CORRECTION to F2b's fuzzer decision rule (2026-07-16, late)

F2b said: "the fuzzer build is NOT currently justified — revisit only if a
widened-surface probe zero is ever shown false." **That condition has now
fired five times**: the B3 sweep ran with the v2 widened-surface probe
instructions, and 5 of its 10 zeros were false (Chart-7, Lang-41/Arja,
Lang-60, Closure-62, Math-57 — all witnessed). The honest re-read: the
verdict on fuzzing still stands, but for a sharper reason. Every one of
the five misses was a SURFACE-SELECTION failure (the LLM probe didn't
call the sibling overload, didn't re-read `capacity()` after a query,
didn't try the non-default constructor, didn't push magnitudes past float
range) — not an input-search failure. A coverage-guided fuzzer pointed at
the same surfaces the probe chose would have missed them identically.
The remedy is MECHANICAL SURFACE ENUMERATION, not smarter input search —
see ADDENDUM 2 §N3 (reflection-sweep probes). Prompt exhortation ("print
EVERY public accessor") demonstrably did not survive contact with the
LLM; the same mechanism-over-prompt lesson the pipeline already learned.

### F3. Math-2/SOFix repair: staged, verified — and ADOPTED (2026-07-16)

**Update:** the corrected patch has been swapped into
`drr/Patches/Dcorrect/SOFix/Math/patch1-Math-2-SOFix.patch` (original kept
as `.bak.orig-reversed-truncated`), dry-run-verified against the real
`Math_2_buggy` checkout. A static sweep found 165/1,263 drr patches lack a
trailing newline — mostly benign (GNU patch fallback tolerates it; e.g.
patch1-Chart-26-Jaid applied fine); the dangerous defects (reversal,
truncation) are only catchable by the dry-run-apply gate (M4), which
remains the actionable item. Original staging notes below.

The file's two defects are confirmed minimal: stored REVERSED (its `+` side
is the buggy overflow formula — byte-identical to the buggy checkout's
line) and missing the trailing newline on the final context line (the
"corrupt patch at line 11 / ends in middle of line" error). A corrected
patch (swap `-`/`+`, terminate the file) is staged on the VM at
`/tmp/m2sofix_fixed.patch` and dry-run-applies cleanly against a buggy
checkout (`patch -p1 --dry-run --forward` → OK). The intended fix is
semantically the dev fix (`n * (m / (double) N)` with extra parens), so the
reconstruction is faithful. To adopt: back up the original, copy the staged
file over `drr/Patches/Dcorrect/SOFix/Math/patch1-Math-2-SOFix.patch`,
rerun the leg (predicted: real TN — all three harnesses use the
tolerance-gated mean oracle, and the true fix differs from the harness
formula by ~1–2 ulp, far under the 1e-12 gate). Two companions (extends
M4): check drr's upstream copy — if upstream is intact the truncation is
local transfer damage and other files may be silently damaged too; and run
a one-off dry-run-apply sweep over ALL drr patches — every broken Dcorrect
file is a phantom TN or a wasted leg in the historical numbers.

---
---

# ADDENDUM 2 — 2026-07-16 (late): post-audit critical revision

Written after the full dataset audit (DATASET_AUDIT.md): ~75 certification
legs against developer fixes, 13 deep-dives with executed witnesses, two
dataset-defect discoveries, one pipeline bug. This section re-reads the
document above against that evidence: what to correct, what to do
differently, and what is genuinely new.

## Status of the §E experiment queue (what actually happened)

1. ~~M4 dataset fix~~ **partially done**: Math-2/SOFix repaired+adopted;
   the dry-run-apply GATE in patches.py is still not implemented — and the
   audit showed the gate as specified is INSUFFICIENT (see M6 below: it
   cannot catch out-of-order-hunk half-application or missing-hunk files;
   only a plausibility preflight can). M1 (self-swallow lint) and M2
   (cause-chain unwrap) are still unimplemented — they remain the top
   pipeline items.
2. ~~E2 certify Lang-7/Arja~~ **done** — excluded (type B). Also done far
   beyond plan: every paired + suite + unpaired leg (DATASET_AUDIT §3).
3. R1+G2 (root-region anchoring + direction screen): **not started**;
   Math-2/Arja remains the validation pair, now with the added confidence
   that the bug is certified detectable at 117 strong divergences.
4. R3 differential prototype: not started; the audit strengthened its
   Math-2 case (Arja's changed region measured ≈ buggy: identical
   `icdf(0.5)` even at overflow params).
5. fixconfirm rerun prediction "P=1.0 with zero new mechanisms" must be
   restated: the suite itself changed (Lang-7-o excluded). Expected now:
   Chart-26 FP persists until M2; Math-2-c becomes a real TN (file fixed);
   Math-2-o stays FN until R1. Rerun AFTER M1+M2+M6, on the pinned-task
   set, not before.

## New corrections to the analysis above

- **F2b's fuzzer decision rule fired and is re-resolved in F2c**: probe
  misses are surface-selection failures; the fix is mechanical enumeration
  (§N3), not fuzzing.
- **A new Tier-0 item outranks parts of the old list — M6, the
  plausibility preflight + applier fix.** The Lang-50 discovery
  (PatchedProjectBuilder silently drops out-of-order hunks; the "patched"
  build failed its own trigger test) means some historical multi-hunk legs
  evaluated half-applied patches. The cheap invariant that catches this
  ENTIRE class (broken files, reversed files, half-application,
  implausible patches, env-nonreproducible bugs) is: **before any
  fuzzing/probing, run the d4j trigger tests twice — they must FAIL on the
  buggy build and PASS on the patched build; abort with a distinct status
  otherwise.** This subsumes the old M4 dry-run gate and the layer-1
  preflight idea, in one mechanism, and it protects both the pipeline and
  the certifier. Do it with the M1/M2 batch.
- **The eval substrate changed under this document.** Recall/precision
  numbers cited above were computed against denominators now known to be
  polluted (2 undetectable legs in semantic8, a phantom TN, a mislabeled
  Dcorrect patch elsewhere in the pool). All future runs draw from
  `suites/pinned_tasks.jsonl` (every task pre-verified: overfit legs
  certified detectable, correct legs label-verified). Consequence worth
  stating loudly: **on the pinned set, every FN is a real technique
  failure and every FP is a real soundness failure** — no more arguing
  with the dataset.

## New insights (N-series)

### N1. The empirical "where overfits hide" taxonomy — use it on BOTH sides

~75 dev-fix comparisons produced, for the first time, MEASURED knowledge
of where plausible-but-wrong patches actually differ from correct ones.
The divergence surfaces, ranked by what we hit:

1. **Sibling overloads / sibling methods of the same class** — the patch
   fixes the symptomatic path, leaves the shared/parallel path broken
   (Lang-41 String vs Class overloads; Lang-60 `indexOf` vs `contains`;
   Math-2 `getNumericalMean` vs `inverseCumulativeProbability`).
2. **Observer state after "read-only" calls** — the patch mutates state a
   query must not touch (Lang-60: `contains()` shrank `capacity()`).
3. **Non-default configurations** — correct on the default path, wrong
   one constructor flag away (Closure-62 REGION vs LINE formatter).
4. **Type-width / magnitude boundaries** — float-vs-double, int overflow;
   invisible until inputs cross the representation edge (Math-57, Math-2).
5. **Structurally irregular inputs** — overlapping periods, descriptor
   strings; the "happy manifold" hides the divergence (Chart-7, Lang-41).

This taxonomy is dataset-DERIVED but not dataset-SHAPED — the categories
are general API-design facts, safe under the no-overfitting rule. It has
two consumers:
- **Certifier probe-v3** (already drafted in suites/labels/incorrect_labels.md).
- **The harness generator and relation synthesis — this is the new part.**
  The generator's variant-strategy menu and the synthesis anchor should
  enumerate exactly these five surfaces for the touched class: propose
  relations on sibling overloads/methods, on observer state, across
  configs, at width boundaries, on irregular inputs. Today's prompts say
  "explore the reachable region" generically; the taxonomy converts that
  into five concrete, checkable instructions.

### N2. Two NEW sound oracle classes for the semantic pipeline

Both fall out of the taxonomy, both are metamorphic-style (no expected
value needed), both screenable by the existing machinery:

- **Observer-state invariance**: after any documented-read-only call
  (`contains`, `indexOf`, getters), all cheap observers of the receiver
  (`size()`, `capacity()`, `toString()`) must be unchanged. Sound whenever
  the javadoc frames the method as a query. Catches the Lang-60 class
  outright — and note the pipeline's harness would have needed exactly
  this to convict Lang-60/Arja without the dev fix.
- **Overload/representation consistency**: `f(x)` and `f(convert(x))`
  must agree when the API documents both forms as the same semantics —
  `getShortClassName(cls)` vs `getShortClassName(cls.getName())`
  (Lang-41), value vs string parses, etc. This is the existing
  "consistency" mechanism slot, which until now had a schema but no
  empirically-grounded shapes; these are its shapes.

### N3. Mechanism over prompt, round two: reflection-sweep probes

The v2 probe prompt said "print EVERY public observable"; the LLM didn't.
Stop asking. Generate the probe in two parts: the LLM writes only the
INPUT CONSTRUCTION (objects, parameter grids), and a fixed mechanical
driver reflects over every constructed object — calls every public
zero-arg method (plus toString/equals/hashCode), prints each value —
before/after every mutating call. Surface enumeration becomes exhaustive
by construction; the LLM contributes only what it is good at (building
interesting inputs). The same trick applies to the HARNESS consistency
slot: a reflective before/after observer sweep needs no per-bug prompting.
(Determinism guard: skip methods returning non-value types; sort method
names; the existing kind-classifier already absorbs ulp/message noise.)

### N4. Divergence breadth as an FN triage lens (offline only)

The certifier's strong-divergence counts spread over three orders of
magnitude (3 → 1764). Use them to triage future FNs on the pinned set:
an FN on a broad leg (>100 divergences: Time-4, Math-71, Chart-1/5/12,
Lang-50/55, Math-73, Chart-3, Math-2...) means harness GENERATION failed —
almost any input distribution hits the divergence, so the oracle side is
what's missing. An FN on a narrow leg (<20: Math-53 at 3, Math-80 at 4,
Closure-73 at 7, Chart-26 at 10, Chart-19 at 14, Math-33 at 16) means
ANCHORING failed — the divergence lives on a sliver only boundary-targeted
inputs reach. Different failures, different fixes; today they'd be
indistinguishable FNs. FIREWALL NOTE: breadth numbers derive from the dev
fix — they may steer eval diagnosis and prompt iteration, never a verdict.

### N5. What the label errors imply for the verdict pipeline itself

drr's human assessment — done by researchers, with the dev fix in hand —
still produced Math-59 (dev fix labeled overfitting), the Lang-41
byte-identical contradiction, and at least two more equivalent-patch
mislabels. Two consequences:
- Treat any single-source label as soft. The scorer should consume
  `label_annotations.jsonl` (already wired into the audit) rather than
  raw directory names — done, keep it that way for every future dataset.
- The certifier (a ~5k-token mechanical differential) outperformed human
  assessment on exactly the cases humans get wrong (dead code, redundant
  guards, textual equivalence). That is an argument for machine-labeling
  the Dunassessed pool CONFIDENTLY for detectable/divergent verdicts —
  and cautiously (deep-dive required) only for zeros.

### N6. Revised next-experiment queue (supersedes §E)

1. **M6 + M1 + M2 together** (plausibility preflight + applier fix;
   self-swallow lint + screen direction fix; cause-chain + A2 unwrap).
   All mechanical, all cheap, protect everything downstream.
2. **Rerun the semantic suite on `pinned_tasks.jsonl`** (replaces the old
   fixconfirm rerun). Predictions: Chart-26-c FP→TN (M2); Math-2-c real
   TN; remaining FNs triaged by N4 into generation-vs-anchoring.
3. **N1/N2 prompt upgrade** (surface checklist into variant menu +
   consistency slot; observer-state oracle) — then measure recall delta on
   the narrow-divergence legs specifically.
4. **R1 root-region anchoring** on the Math-2 pair (unchanged, still the
   single most promising recall mechanism for the patched-elsewhere
   class).
5. **G2 direction screen + R3 buggy-differential** prototypes (unchanged
   rationale).
6. Certifier probe-v3 as reflection-sweep (N3) + re-certify the five
   false-zero legs with it as the known-answer test; then the Dunassessed
   machine-labeling batch (N5) when eval-set growth is wanted.

---

# ARCHIVED: full plan text as of 2026-07-18 (pre-restructure), preserved verbatim before the by-station rewrite of the plan doc (now docs/plan.md)

# Semantic-bug detection — the plan

Rewritten 2026-07-16 (late). Full forensic history in
`semantic-recall-history.md`. Companion docs: `suites/DATASET_AUDIT.md`
(inventory + verdicts), `suites/labels/incorrect_labels.md` (exclusion evidence),
`suites/pinned_tasks.jsonl` (the verified task set),
`suites/label_annotations.jsonl` (label corrections).

---

## The problem

We are given a bug and a candidate patch for it. Some patches are real
fixes; some are **overfit** — they make the bug's failing test pass
without actually fixing the underlying behavior. Our pipeline writes a
small fuzzing program (a **harness**) full of **checks** ("this call must
not crash", "the mean must equal n·p") and runs it against the patched
program. We need the checks to be:

- **safe**: they never accuse a genuinely correct patch, and
- **sharp**: they do catch the overfit one.

And we must do this **without ever consulting the developer's real fix**
in the decision — that would be cheating (in the real world there is no
reference fix). An overfit patch passes every existing test by
construction, so the only place it can be caught is on inputs no test
covers — where "what is correct?" must be reconstructed from indirect
evidence, ranked by how much we trust it:

1. the bug's original failing test (definitive, but only for its inputs)
2. the buggy program itself (correct everywhere except at the bug)
3. the documentation comments (the promised contract)
4. universal math/logic rules ("sorting twice = sorting once"), if
   genuinely universal
5. the patch's own code — **least trusted; it may be the overfit**

Every **miss** (an overfit we don't catch) means we failed to check
something the overfit gets wrong. Every **false alarm** (a correct patch
we accuse) means we checked something a correct program is actually
allowed to do differently.

**Firewall rule**: the developer fix may be used ONLY offline — for
cleaning the dataset, verifying labels, and understanding our misses
afterwards. Never in any decision the pipeline makes.

**Substrate rule**: all experiments run against `pinned_tasks.jsonl`,
where every overfit patch has been verified to actually behave
differently from the real fix, and every correct patch has had its label
double-checked. The audit showed the raw dataset lies: "overfitting"
patches that are literally the developer fix (Math-59), the same file
labeled correct in one folder and overfitting in another (Lang-41), and
"correct" patches that are behaviorally wrong (Lang-10). On the pinned
set, every miss is a real technique failure and every false alarm a real
safety failure — the numbers mean something.

---

## The plan

Four phases. Phase boundaries are measurement points — never turn on
several untested changes at once (we did that once and could no longer
tell which change caused what). Each item: Case (why, with the real
story) / How (file-level) / Validate / Effort.

---

### PHASE 0 — Foundations: make every downstream number trustworthy (~3–4 days)

All four are mechanical plumbing; none changes what the checks assert;
nothing measured later is interpretable until they land.

**P0.1 Verify every patch really got applied, and really fixes the test**
- *Background, in plain words:* a patch file is a list of edits to source
  code; each edit block ("change these lines near line 300") is applied
  separately by the `patch` tool. Some patch files contain several edit
  blocks.
- *Case:* Lang-50's correct patch lists its two edit blocks in reverse
  line order — and our code, which hands the whole file to `patch` in one
  go, applied the FIRST edit and silently skipped the second. No error.
  We then built and tested a program carrying only half the patch: it
  failed the bug's own original test, and certification recorded 43
  phantom behavior differences for it (the fully-applied patch shows
  zero). Separately, Math-2/SOFix's patch file was stored backwards AND
  cut off mid-line — it never applied at all in any run, and a scoring
  bug counted the resulting do-nothing runs as passes for weeks. Two
  different file problems, same root failure: the pipeline tested
  programs that were never what it believed they were.
- *How:* (a) in `fuzz_runner.PatchedProjectBuilder`: apply each edit
  block one at a time, sorted by line number; if ANY single edit fails to
  apply, stop with an error — never continue with a partly-applied patch.
  Afterwards, sanity-check that the number of changed lines in the code
  matches what the patch file promised. (b) the safety net, added to both
  the pipeline (`run.py`) and the certifier: before using any patched
  program, run the bug's original failing test twice — it must FAIL on
  the unpatched code (proves the bug is present in our environment;
  otherwise status `bug_not_reproduced`) and PASS on the patched code
  (proves the patch applied fully and does what a plausible patch must;
  otherwise status `bad_patch`). The unpatched-side result is cached per
  bug since it never changes.
- *Validate:* known answers — the original Lang-50 patch file must be
  caught (`bad_patch`) before the applier fix and sail through after it;
  the archived broken Math-2/SOFix file must be caught. Then sweep all
  patches with multiple edit blocks for the same silent half-application
  in past runs, and redo any affected certifications.
- *Effort:* ~1 day. **Do first — it protects even the audit tooling.**

**P0.2 Make sure a check's alarm can't be silenced by its own error handling**
- *Case:* A check raises its alarm by throwing an error ("rule
  violated"). In the Lang-7 runs, every generated check wrapped its whole
  body — including that alarm throw — inside a try/catch that catches
  everything and quietly gives up. So the alarm was thrown, immediately
  caught by the check's own catch clause, and discarded. A rule that
  should have fired on all 20,000 inputs against the buggy program
  registered zero firings — and the pipeline read "zero firings on buggy"
  as "well-behaved rule, keep it" and promoted the useless check into the
  harness. About half of all generated harnesses had this pattern,
  because our own prompt wording ("wrap in try/catch and skip on
  exception") invites the mistake. Asking the model more firmly won't fix
  what a mechanical code check can.
- *How:* new `relation_screen._violation_swallowed(body)` (parse the
  Java; fall back to brace matching): flag any alarm throw that sits
  inside a try block whose catch clause catches everything and does not
  re-throw the alarm. Apply it at both entry points for candidate checks
  (the screening format gate and the `campaign.py` structural gate) —
  rejection feeds the existing retry loop with a pointed error message.
  Plus a canary in `screen_relations`: for each candidate, also compile a
  variant that is FORCED to raise its alarm and confirm the counting
  machinery records at least one firing — proving the alarm can actually
  be heard end to end.
- *Validate:* the broken snippet from the Lang-7 logs is the test case:
  rejected as-is; the corrected version (alarm re-thrown) passes and
  fires on ~100% of inputs. NOTE: today's rule "discard any check that
  fires on more than 20% of inputs" would then delete exactly that good
  check — until P2.2 fixes that rule, log such checks loudly instead of
  deleting them.
- *Effort:* ~½ day + fixtures.

**P0.3 When a harness re-labels a crash, look at what actually crashed underneath**
- *Case:* The Chart-26 false alarm. The chart library has a pre-existing
  crash that has nothing to do with the bug: give the text-measuring code
  a title containing malformed characters and it crashes on EVERY version
  — buggy, patched, or fixed. Our safety net normally recognizes exactly
  this ("the same crash happens on the unpatched program too, so it's not
  the patch's fault") — but only when the crash reaches it undisguised.
  One harness caught this crash and re-threw it as its own alarm type,
  and the safety net deliberately doesn't question the harness's own
  alarms — so the disguised pre-existing crash walked straight past the
  only mechanical defense and was blamed on the patch. Whether a harness
  re-wraps a crash or reports it directly is model coin-flip, which is
  why the same crash was correctly dismissed one day and wrongly counted
  the next.
- *How:* (a) a rule, checked mechanically in `campaign.py`: when a
  harness converts a caught crash into its alarm, it must attach the
  original crash as the alarm's "caused by" record (Java's standard way
  to say "this error came from that one"). (b) `fuzz_runner` parses that
  record, so every alarm carries the identity of the underlying crash
  (exception type + the library code line it came from) alongside the
  alarm itself. (c) extend the safety-net check in `run.py`: if an
  alarm's underlying crash is a generic library crash, replay the same
  input on the unpatched buggy program — if the same crash appears there
  too, it is pre-existing and the alarm is dismissed.
- *Validate:* replay the saved Chart-26 crashing input — it must now be
  dismissed. The genuine Chart-26 catch on the overfit side (a
  synthesized rule with no underlying library crash) must survive.
- *Effort:* ~1 day. **Highest-value safety fix; predicted to remove the
  one known false alarm on its own.**

**P0.4 Know which individual check earned its place — and which never ran**
- *Case:* A harness usually contains several checks that run one after
  another. In one Chart-26 attempt, the first check fired on every single
  input on the buggy program — so execution never got past it, and the
  checks written after it never ran even once. Our acceptance step only
  asks "did the harness AS A WHOLE fire on the buggy program?", so it
  accepted the harness on the strength of check #1 alone. One of the
  never-ran checks then met its first-ever execution against the correct
  patch — and promptly raised a false alarm. (The same coarseness let
  another harness pass acceptance via the pre-existing crash from P0.3
  rather than via the actual bug.)
- *How:* rule: every alarm message starts with a short name for its check
  (`[oracle:mean-formula]` — checked at the structural gate), so firings
  can be told apart. At acceptance, run the buggy program in keep-going
  mode (already supported as `collect_fired_oracles`) and record which
  named checks ever fire. Step 1: checks that never fired on the buggy
  program are flagged loudly as "never exercised" and the flag is passed
  to the verifier as context. Step 2 (only if step 1 proves useful): cut
  never-exercised checks out of the harness before running it against the
  patch.
- *Validate:* the specific never-ran Chart-26 check must come out flagged
  on a rerun.
- *Effort:* ~1 day for step 1.

---

### PHASE 1 — Baseline measurement (dev set only, ~100k tokens)

We do NOT run the full pinned set here — that costs too much per
iteration, and we need untouched bugs for an honest final number. The
pinned dataset carries a `split` field dividing it in two:

- **dev (30 legs, 17 bugs)** — the working set for all iteration. It
  contains the 12 bugs whose failures we studied in depth to design the
  fixes (Math-2, Chart-26, Lang-7, …). Those HAVE to be here: because we
  built the fixes by staring at exactly these bugs, a good result on
  them can never prove the method generalizes — they are spent as
  evidence. Also included: the 3 bugs named as validation targets in
  Phases 1–3, and 2 untouched pairs (Chart-3, Closure-33) as a smoke
  check that changes don't break ordinary tasks.
- **held-out (71 legs)** — never touched during development: no runs, no
  prompt tuning, no debugging against it. It is spent ONCE, at the very
  end, with the flagship model. That run is the number that counts.
  Expect dev numbers to look better than held-out numbers — that gap is
  itself the measure of how much we over-tuned.

The split is at bug level (both sides of a bug stay on the same side of
the split), because P3.2 shares synthesized rules between patches of the
same bug — a bug with one foot in each set would leak.

**P1.1** Generate `suites/pinned_dev.cases` from the `split: dev` rows of
`pinned_tasks.jsonl` (`patchfile:` case specs; run_suite supports them).
Score against the pinned labels plus `label_annotations.jsonl`.

**P1.2** Run the dev set with the Phase-0 fixes on. Record per task:
outcome, which named checks fired, and every dismissed alarm with its
reason. Between-phase gates rerun only the dev set; day-to-day iteration
should use even less (the 2–6 legs relevant to the change, per the
iterate-cheap rule). Two task-specific notes:
- *Lang-50 correct side* only becomes runnable AFTER the P0.1 applier
  fix — until then the pipeline builds it half-patched and any result is
  meaningless (it's flagged BLOCKED in the dataset).
- *Expected permanent miss:* Time-11's overfit is only visible from a
  second thread, and we deliberately keep threads out of harnesses (a
  check that sometimes fires and sometimes doesn't, depending on thread
  timing, can't be trusted either way). So the realistic dev ceiling is
  15 of 16 overfits, not 16 — don't spend iterations chasing the last
  one (that's the Lang-7 lesson: know which tasks are unwinnable and
  stop paying for them).

**P1.3 Sort the misses by how big the behavior difference is (offline;
uses the dev fix, so never in the decision path)**
- *Case:* For every overfit patch we certified, we know how many probe
  outputs differ between it and the real fix — anywhere from 3 lines to
  2,679. That number tells us what a miss means. If we miss a patch whose
  behavior differs on thousands of inputs (Time-4: 1,764), then almost
  ANY inputs would have hit the difference — our inputs were fine and we
  simply never CHECKED the property that differs. If we miss a patch that
  differs on only a handful of inputs (Math-53: 3; Closure-73: 7), the
  opposite: our checks may be fine but our inputs
  never reached the tiny corner where behavior differs. Today both look
  like the same miss; sorted this way, they demand opposite fixes — and
  the split decides whether Phase 3 starts with better checks (P3.1) or
  better aim (P3.2).
- *Third bucket — the witness-only patches.* Seven dev overfits (Chart-7,
  Lang-41, Lang-60, Closure-62, Math-57, Closure-92, Time-11) have a
  probe count of ZERO — even our certifier's probe missed them, and
  they're only proven catchable by a hand-written program that looks at
  exactly the right thing (overlapping time periods, the buffer's
  capacity, one specific formatter mode…). We EXPECTED these to be the
  hardest. **The baseline falsified that: the pipeline caught 5 of the 7
  (Chart-7, Lang-41, Lang-60, Closure-62, Closure-92) — only Math-57
  (float width) and Time-11 (cross-thread, the expected permanent miss)
  stayed misses.** The lesson: "witness-only" describes the CERTIFIER's
  single probe, not the pipeline — the pipeline generates several diverse
  harnesses and finds surfaces the one probe didn't. Don't treat
  witness-only as pipeline-hard.

- *What the baseline actually showed about the buckets (2026-07-17).*
  Refinement to the dichotomy above. The two BROAD misses (Lang-50: 225
  divergences; Math-2: 117) were NOT "we never checked the property" —
  in both, the discriminating check WAS generated, but it was either
  LATENT (never fired on the buggy build, so unanchored — P0.4 flags this
  exactly) or STOCHASTIC (see below). So a broad-divergence miss is
  three-way, and P0.4's latent data tells them apart: (a) the
  discriminator was never generated; (b) generated but latent →
  P2.1/P2.2 (feed direction, screen on the trigger inputs) + P3.2
  (anchor at the bug); (c) generated but flaky. In EVERY one of the
  seven baseline misses the check that DID fire on buggy was the
  lifted-seed oracle — the reported input, which the overfit
  special-cased — so the patched build passes it. That single mechanism
  unifies the misses and points squarely at Phase 2 as the next step.

- *Stochastic-oracle miss (new; Math-2-o).* Math-2's overfit was a catch
  at the P0 gate and a miss at the baseline — a real flip-flop, not
  noise. Cause: the check that fires on buggy is built on `sample()`, a
  RANDOM draw, so its verdict depends on fuzzing luck (the RNG must hit
  the overflow path AND produce a negative sample). The reliable
  discriminator is `getNumericalMean()` = −49.76, which is DETERMINISTIC
  and which the Arja patch never touches — and it sat latent. Lesson: an
  oracle anchored on a nondeterministic method gives flaky verdicts;
  prefer a deterministic discriminator. This is what P2.2 must add (a
  reproducibility check) and what P3.2's pooled mean-formula rule
  delivers.

*Predictions:* Chart-26 correct side flips false-alarm → clean from P0.3
alone; Math-2 correct side is genuinely clean now (file repaired +
tolerance in the check); Math-2 overfit side is caught only FLAKILY until
P3.2 makes it deterministic; no previously-caught overfit regresses.

---

### PHASE 2 — Direction & grounding: make check-writing trustworthy (~2–3 days)

**P2.1 Give check-writing the one trusted truth we have — the failing test**
- *Case:* To write a good check, the model must know which direction is
  correct: does `createNumber("--1.1")` correctly return null, or
  correctly throw an error? The bug's original failing test answers this
  definitively (it expects the error). But our pipeline never shows that
  test to the check-writing step — the field meant to carry it has been
  empty since forever. Worse: the code that step DOES see is shown under
  the heading "Patched method(s):" while actually being the BUGGY
  version, and the diff we show has its plus/minus markers stripped, so
  added lines and deleted lines look identical. On Lang-7 the model read
  the buggy body, believed the heading that said it was the fix, and
  wrote its rule exactly backwards ("--" must return null). We withheld
  the most trusted source from the one step that needed it, and fed it
  mislabeled code instead.
- *How:* `run.py` passes the failing test's source and its expected
  values into `relation_synth`, displayed at the top as the most trusted
  block ("the patch itself may be the overfit; where the failing test
  pins down a behavior, the test wins"). Fix the heading to "BUGGY
  method(s) (pre-patch):". Keep the plus/minus markers in the diff. Also
  show what the method looks like AFTER the patch (apply it
  mechanically). And in `code_context.assemble_class_context`, when long
  files get trimmed to fit, keep the changed method's documentation
  comment first — Lang-7's method doc states the exact contract, and it
  was trimmed away.
- *Validate:* rerun the Lang-7 correct side (one cheap task): the
  backwards rules are gone and the new candidates cite the test's
  expected error.
- *Result (2026-07-17 p2val, 3 legs): mechanism confirmed, no
  regression.* On Math-2's CORRECT leg synthesis proposed the
  mean-formula and the direction check ranked
  `mean_matches_documented_formula` FIRST as direction-confirmed
  (fires on the failing test's inputs, 20000/20000 on random) — the
  exact deterministic discriminator that was latent at the baseline —
  and the leg stayed clean (the formula holds on the fixed build, so no
  false alarm). Lang-7-c stayed clean too. Math-2's OVERFIT leg,
  however, is STILL a miss — because synthesis that run proposed
  different relations (quantile/sample) and never generated the
  mean-formula on that leg. The convicting relation exists on the
  sibling leg but is not shared → this is the P3.2 pooling gap, not a
  Phase-2 failure. Phase 2 does what it controls; P3.2 is what closes
  Math-2-o.

**P2.2 Test every candidate rule on the failing test's own inputs before trusting it**
- *Case:* two failures, one fix. (a) The backwards Lang-7 rule survived
  screening because screening only asks "does the rule fire on the buggy
  program?" — and a backwards rule stays QUIET on the buggy program,
  because the buggy behavior is exactly what it (wrongly) demands. So
  "quiet on buggy" currently lumps together three very different things:
  rules that are right, rules that encode the bug itself as the truth,
  and rules that never really ran. All three get promoted the same way.
  (b) Once P0.2 un-silences alarms, a correct rule aimed straight at the
  bug will fire on nearly EVERY input on the buggy program — and today's
  "discard anything firing on more than 20% of inputs" rule would delete
  precisely our best rules.
- *How:* in `relation_screen`, a second measurement per candidate, this
  time feeding it the failing test's own input values (the machinery for
  seeding chosen inputs already exists). Outcomes: fires on those inputs
  ⇒ the rule points the right way — rank it first and exempt it from the
  20% discard rule; stays quiet on those inputs while claiming to cover
  the changed behavior ⇒ probably backwards — drop it; the test's inputs
  never reach the rule ⇒ today's unknown case, keep with today's rules.
  The 20% discard rule remains only for unconfirmed rules.
- *Validate:* from saved logs — the backwards Lang-7 rule drops; the
  corrected one ranks first. Depends on: P0.2, P2.1.
- *Add a reproducibility check (new, from the baseline).* Math-2's
  overfit was caught once and missed once because its firing check reads
  `sample()`, a random draw — the verdict depended on fuzzing luck. When
  the trigger-input measurement above runs, run it a few times: a check
  that fires on the trigger inputs on some repeats and not others is
  FLAKY. Rank a deterministic discriminator ahead of a flaky one, and
  when only a flaky check is available, say so in the record (so a
  changed outcome for that leg is known to need the confirm-repeat from
  measurement rule #2). The reliable Math-2 discriminator
  (`getNumericalMean` = −49.76, deterministic) was generated but latent;
  ranking determinism first is what surfaces it.

**P2.3 Don't approve a rule the harness is forbidden to contain**
- *Case:* For Math-2, check-writing actually produced the ONE rule that
  convicts the overfit (the mean of the distribution must match the
  textbook formula). It passed screening — but implementing it in the
  harness required writing a small custom subclass of the patched class,
  which the harness rules forbid (screening compiles under NO such
  rules, so nothing objected). The model resolved the contradiction
  silently: it accepted the rule and then simply didn't put it in the
  harness. Nobody noticed that handed-over ≠ implemented, and the miss
  followed. The irony: a standard library class
  (`UniformIntegerDistribution`) reaches the same behavior perfectly
  legally — nobody told the model to look for one.
- *How:* screening compiles candidates under the same restrictions the
  harness must obey (no custom subclasses of the patched classes — a
  simple syntax check), so an unimplementable rule fails early; the
  check-writing prompt states the restriction and suggests using existing
  library classes; and `run.py` compares the rules we handed to harness
  generation against what actually appears in the harness source, logging
  any rule that was handed over but never implemented — loudly.
- *Validate:* Math-2 overfit side rerun — the convicting rule either
  arrives in implementable form or is rejected at screening; never
  silently dropped.

---

### PHASE 3 — Catching more overfits (~3–4 days; internal order decided by P1.3)

**P3.1 Teach checks the six places overfits actually hide**
- *Case:* Comparing ~75 patches against their real fixes gave us
  measured knowledge of where the behavior differences live: (1) in
  sibling methods that do the same job — Lang-41 fixed the variants of a
  method that take a Class object but left the String variants broken, so
  the two now disagree with each other about the same input; (2) in
  hidden state changed by supposedly read-only calls — Lang-60's
  `contains(char)` silently shrinks the buffer's capacity from 32 to 3,
  and a later lookup reads leftover stale characters; (3) in non-default
  settings — Closure-62 misbehaves only in one particular error-message
  formatting mode; (4) at extreme number sizes — Math-57 changed a
  `double` to a `float`, which only matters for values around 10^20; (5)
  on irregular inputs — Chart-7 misbehaves only when time periods
  OVERLAP; (6) across threads — Time-11 crashes only when called from a
  different thread than the one that loaded the class. The first two are
  catchable WITHOUT the real fix by two generic checks: "a call that
  documents itself as read-only must not change what the object reports
  about itself" and "two methods documented to do the same thing must
  agree". These are facts about APIs in general, not shapes of particular
  bugs — so they respect the no-dataset-overfitting rule.
- *How:* the prompt building blocks in `prompts.py`
  (`_variant_strategy_menu`, `_consistency_hint_block`) enumerate the six
  places in category language. New `_observer_state_block`: after any
  call whose documentation reads like a question (get*/is*/contains/
  indexOf/size, and no mention of modifying), re-read the object's cheap
  properties and assert they didn't change. Check-writing instructions
  gain the sibling-agreement shape (`f(x)` must equal `f(convert(x))`
  where the docs say they're the same). The cross-thread idea stays OUT
  of harnesses (too flaky) and is used only in offline certification.
  And one standing rule for ALL of these: any check that compares
  numbers compares them with a tolerance, never exact equality. Fresh
  evidence for why: Math-39's CORRECT patch differs from the developer
  fix at the 13th significant digit of the integrator's output — two
  correct implementations legitimately disagree at that scale, so an
  exact-equality check there is a guaranteed false alarm
  (`label_annotations.jsonl` has the record).
- *Validate:* measured as the Phase-1-vs-Phase-3 improvement specifically
  on the dev-set patches whose behavior difference is tiny (Math-53: 3
  differing outputs, Closure-73: 7, Chart-26: 10) — the analysis predicts
  those benefit most. The held-out set has its own tiny-difference
  patches (Math-80: 4, Chart-19: 14); they stay untouched and will show
  at the end whether the improvement generalizes.

**P3.2 Aim checks at where the bug lives, not where the patch edited — and share rules between patches of the same bug**
- *Case:* Math-2, the canonical case of a patch that edits the wrong
  place. The real bug is an arithmetic overflow inside
  `getNumericalMean`. The Arja patch edits a DIFFERENT method entirely
  and passes the failing test by coincidence — `getNumericalMean` still
  returns the impossible −49.76 on the patched build. Our checks are
  aimed at whatever the patch touched, so on the Arja task every check
  stared at the edited (irrelevant) method and none ever looked at the
  broken one. Meanwhile, on the OTHER patch for the same bug,
  check-writing produced exactly the rule that would convict Arja — but
  rules aren't shared between tasks, so the Arja task never saw it. Even
  our own certifier made the same aiming mistake once: its first probe
  stared at the patched method (identical outputs on both builds, even at
  overflow-triggering inputs) and reported "no difference"; probing the
  objects the failing test actually uses revealed 117 differences. One
  aiming lesson, proven twice independently.
- *How:* (a) also aim at the failing test's neighborhood: take the class
  and method names that appear in the failing test's body, intersect with
  the project's classes, and add their public methods as a second aiming
  block for check-writing (`analysis.py`). (b) pool rules per bug:
  persist every screened rule keyed by (project, bug), and give every
  patch of the same bug the whole pool. The rules carry no labels or
  verdicts, so nothing leaks between tasks.
- *Validate:* THE decisive experiment — run the Math-2 pair. Prediction:
  the Arja miss becomes a catch via the mean-formula rule; the correct
  SOFix patch stays clean because the rule compares with a tolerance
  rather than exact equality. If this fails, stop and debug before
  building anything more on aiming.
- *Result (2026-07-17 p32val2): mechanism validated, one real bug found
  and one tolerance bug found.* A direct deterministic probe settled the
  ground truth: getNumericalMean at the overflow params is −49.76 on
  BOTH the buggy and Arja-overfit builds (Arja edits elsewhere, never
  fixes it) and 49.82 on the SOFix-correct build (= the formula, = the
  dev fix); sample() is likewise identical between SOFix and the dev fix
  (always in-support) and all-negative on buggy. So the deterministic
  mean-formula SEPARATES the pair perfectly at normal params. The
  pooling/anchoring/direction machinery all fired correctly (pool
  save→load→re-screen confirmed; a relation came back
  direction-confirmed). TWO real bugs surfaced, both now understood:
  (1) the JSON double-escape that made the mean-formula fail to compile
  (fixed — literal `\n` recovery); (2) **the synthesized mean-formula
  used too-tight tolerance (1e-12 relative) and FALSE-FIRED on the
  correct SOFix build at extreme parameters (N=n≈2.1 billion), where
  double rounding exceeds 1e-12** — the FP. This is the standing
  tolerance rule not being honoured generously enough; the fix is in the
  synthesis prompt (magnitude-scaled tolerance, looser floor for
  large-integer intermediates). Math-2-o's residual FN is a separate
  input-coverage issue: the harness must feed the overflow-inducing
  large parameters to the mean-formula on the patched build for it to
  fire there. **Net: the P3.2 hypothesis (a deterministic discriminator
  cleanly separates the pair) is confirmed true by probe; the pipeline
  needs the tolerance fix + reliable large-param generation to realise
  it. Math-2 is a NOISY binary-gate target — its verdict swings on
  tolerance and input luck — so treat the probe as the real evidence.**

**P3.3 A "must not crash" check should insist on the SAME crash it saw before**
- *Case:* Chart-26's other half. One check says "drawing the chart must
  never crash". On the buggy program it fired because of the bug's actual
  crash (a null-pointer error inside axis-label drawing). On the correct
  patch the SAME check fired again — but this time from the unrelated
  pre-existing text-measuring crash (see P0.3). A catch-everything check
  cannot tell those apart; the crash's identity (its exception type and
  the code location it came from) can. And pinning the check to the
  original crash site is still a fair generalization of the failing test:
  a half-fix that still crashes at the same place on OTHER inputs is
  exactly what we want to catch.
- *How:* builds on the P0.3/P0.4 plumbing — at acceptance, record which
  underlying crash made each "must not crash" check fire on the buggy
  program; on the patched build, that check only counts if the crash
  matches the same type and location; a firing from a different crash
  site is ignored.
- *Validate:* ablation — rerun the Chart-26 correct side with P3.3 but
  WITHOUT P0.3: the pinning alone should also remove the false alarm.
  Two independent defenses against the same failure.

---

### PHASE 4 — Advanced (only after Phases 0–3 are measured)

**P4.1 Compare the patch to the BUGGY program — no real fix needed**
- *Case:* Math-2/Arja's edited region behaves IDENTICALLY to the buggy
  program (measured — same outputs even at the overflow-triggering
  inputs). In plain terms: the patch changed code without changing
  behavior, and passed the failing test by pure coincidence. "Your change
  did nothing, yet the test now passes" is a strong overfit signature —
  and it is computable using only the buggy build we already have, so
  using it in decisions doesn't violate the firewall.
- *How:* let the certifier run with the buggy build as the comparison
  point instead of the fixed one (`--baseline buggy`, ~30 lines); rule:
  if the edited region behaves identically to buggy everywhere except on
  the failing test's own inputs (ignoring rounding-level and
  message-text-only differences), flag as suspicious.
- *Validate:* the Math-2 pair (Arja flagged, SOFix clearly different from
  buggy), then measure the false-alarm rate on 3–4 verified-correct
  patches before this may influence any verdict.

**P4.2 Stop asking the probe-writer to be thorough — make the machinery thorough**
- *Case:* Half of our certifier's "no difference found" answers turned
  out to be wrong — and every one of them came from probes where the
  prompt demanded "print EVERY public observable" and the model simply
  didn't do it (never called `capacity()`, never the sibling method,
  never used a second thread). Same lesson as everywhere else in this
  project: a mechanism beats an instruction.
- *How:* split probe writing in two. The model only constructs
  interesting input objects and call sequences. A fixed piece of our own
  code then automatically finds and calls every public no-argument method
  on those objects (sorted, values only) before and after each step, and
  prints all the results. The model cannot forget what it never had to
  remember.
- *Validate:* known answers — the five wrongly-cleared patches (Chart-7,
  Lang-41, Lang-60, Closure-62, Math-57) must flip to "difference found"
  with NO prompt changes; then re-run all remaining "no difference"
  verdicts.

**P4.3 Give the verifier one decision per crash, not one per firing**
- *Case:* In one Chart-26 run the LLM verifier reviewed two firings that
  were the SAME exception from the SAME code location — it correctly
  dismissed the first, then kept the second, one call later. We already
  measured that majority voting (asking three times) doesn't help: cost
  tripled, the error rate didn't move. The fix is structural: stop asking
  the same question twice and hoping for consistency.
- *How:* group firings that share the same check name and the same
  underlying crash identity (both exist once P0.3/P0.4 land); ONE
  verifier call per group; the verdict applies to the whole group; if
  contradictions somehow remain, the dismissal wins.
- *Validate:* replay saved verifier decisions (`verifier_replay`) and
  compare wrong-keep and wrong-dismiss rates before enabling.

**P4.4 Use the certifier to label the 205 unlabeled patch files we can check cheaply** (dataset growth, not pipeline)
- *Case:* The certifier out-performed human labeling exactly where humans
  err — dead code, redundant safety checks, code that is textually
  different but does the same thing — and 205 unlabeled patch files
  belong to bugs whose reference builds we already have cached, so
  checking them is cheap.
- *How:* certify one file per (bug, tool) first (~150k tokens);
  "difference found" verdicts can be trusted directly; "no difference"
  verdicts must go through the deep-dive protocol before anyone believes
  them (we know first-pass zeros are unreliable).
- *Validate:* manually spot-check a sample of machine labels before any
  of them enter the pinned task set.

---

## Rules for every measurement (so the numbers stay believable)

1. **No "before" baseline run.** Don't pay for a pre-Phase-0 measurement
   — the old fixconfirm and diag-24 runs already are the before picture,
   and their failures are fully explained. The first paid run is the
   Phase-1 baseline, with Phase-0 fixes on.
2. **A changed outcome must be confirmed once before we believe it.**
   Harness generation is partly random — we've measured the same patch
   file getting 0 behavior differences from one generated probe and 5
   from the next, with nothing changed. So when a task changes outcome at
   a gate (miss → catch, or a new false alarm), rerun that ONE task once;
   only call it changed if the repeat agrees. Never repeat the whole set
   for this.
3. **Results are tied to the environment they ran in.** Every run records
   the JVM (currently OpenJDK 11.0.31). The unwinnable-task list is
   environment-specific — Lang-7 is unwinnable precisely because OUR JVM
   doesn't have the old parsing quirk. If the JVM or OS image ever
   changes, the exclusion list and the equivalence verdicts must be
   re-checked before comparing numbers across the change.
4. **Held-out hygiene has one exception.** If a held-out task fails for
   plumbing reasons (build broke, patch didn't apply), fixing the
   plumbing is fine. What is never fine: adjusting prompts, checks, or
   thresholds based on anything seen in held-out output. A held-out bug
   we debug against silently becomes a dev bug, whether we relabel it or
   not.
5. **Suite-run mechanics (learned the hard way on 2026-07-17).** Run
   legs as parallel as the model API allows (4-way default, up to 6 for
   the small projects — beyond that the shared API is the bottleneck,
   not the machine). Before launching, check free disk: every leg keeps
   its own working copy of the project (~100 MB for Lang/Math/Time, ~1
   GB for Closure/Chart) until the suite ends — a Chart/Closure-heavy
   suite filled the whole disk mid-run and 18 legs died at checkout.
   After every suite: delete the working copies (pure scratch, always
   reproducible), archive the results (tiny — all history so far
   compresses to ~2 MB) to the Mac under `runs-archive/`, verify the
   archive's checksum (a tar written on a full disk truncates
   silently), and keep on the VM only the suites still needed for
   comparison.
6. **Success targets, fixed now so we can't move the goalposts later.**
   Starting point was 58% of overfits caught. Dev-set goal after Phase 3:
   at least 13 of 16 overfits caught (the ceiling is below 16 — see the
   expected-miss note in Phase 1) with ZERO false alarms on the 14
   correct legs. Held-out goal at the final run: at least 70% of the 28
   overfits caught, at most 1 false alarm on the 43 correct legs. And a
   stop rule against endless tuning: if two consecutive iterations don't
   change any dev outcome, stop iterating that phase — more tuning at
   that point is just fitting noise.

---

### Why this order, one line each

- Phase 0 first: Lang-50 and Math-2 proved that even our AUDIT was
  standing on sand without it — and P0.3 alone should remove the one
  known false alarm.
- A measurement pass between every phase: we once turned on several
  untested changes together and could no longer tell which change caused
  what.
- Phase 2 before Phase 3: the Phase-3 mechanisms amplify whatever
  direction check-writing believes in — ground the direction first, or
  we amplify backwards rules.
- Phase 4 last: powerful but with unmeasured false-alarm risk (P4.1) or
  pure offline tooling (P4.2/P4.4) — never mixed into a run that is also
  measuring Phases 2–3.

---

## Post-full30 critical triage (2026-07-17): what the remediation showed,
## what fixes what, and where we have no mechanism yet

**Why performance is nearly flat vs the baseline although a lot was
added — the honest accounting.** Baseline 9/16 R, 0.90 P; now 9-10/16 R,
1.00 P. Three reasons the recall barely moved:

1. **The baseline 9 was a lucky draw from a high-variance process, not a
   floor.** Chart-3-o has missed four consecutive runs since (its
   baseline catch was a loosely-reconstructed lift that happened to
   generalise); Closure-33-o flips run to run (which metamorphic gets
   invented is a coin flip). Rerunning the BASELINE configuration today
   would likely not reproduce 9. Today's 9 are each confirmed ≥2×.
   The distribution's mean moved a little; its variance collapsed.
2. **Nearly all the added machinery bought precision and validity, not
   recall — by design.** Phase 0 (4 commits) was explicitly trust;
   the remediation's eight Wave-1 fixes plus P4.3/crash-pinning/format
   gates all close FALSE-ALARM classes (p23gate's 4 FPs → 0, and
   Chart-26-c clean for the first time ever). Recall-side additions were
   only: prompt-thinning (recovered Lang-60-o), formula-first synthesis,
   and the replay (Math-2-o). Gains (+Math-2, +Lang-50, +Closure-73)
   ≈ losses (−Chart-3, −Closure-33 luck; −Closure-62 verifier) → flat.
3. **Until the replay existed, every recall idea travelled through a
   lossy channel.** Synthesis direction (P2), pooling (P3.2a), anchoring
   (P3.2b) all delivered their output as PROMPT TEXT to the harness
   generator, which implements a fraction of it, and patched-side
   fuzzing then has to find the right inputs — two coin flips that
   multiplied any upstream improvement by a small factor (Math-2's
   formula was synthesized, screened, pooled and STILL missed for
   weeks). The replay is the first mechanism that bypasses the channel;
   it is why Math-2-o is now deterministic. Lesson for everything that
   follows: a recall mechanism must either bypass harness generation or
   make its check mechanically mandatory — instructions multiply, they
   don't add.

**Where the residual misses live, and which planned fix owns each:**
- Closure-62-o → verifier trust hierarchy (the shown buggy body's guard
  outranked the trigger test's pinned value in its judgment) — fix
  landed in relation_verifier, being confirmed (c62confirm).
- Math-53-o → P3.1 field-level observers (the library's own equals()
  treats all-NaN as equal; the divergence is only visible on getReal()).
- Math-57-o → P3.1 extreme-width probing (float-vs-double at ~1e20) plus
  its harness repair-loop fragility (no_harnesses in 2 of 4 runs).
- Chart-3-o → P4.1 compare-to-buggy ("edited region behaves identically
  to buggy except on the trigger inputs" is exactly its signature; the
  faithful-lift path can never catch it because the overfit passes the
  faithful scenario by construction).
- Closure-33-o → NO OWNED FIX YET. Its winning oracle (a compile-level
  whitespace metamorphic) gets invented in some runs and not others;
  this is harness-generation stochasticity. (Cross-run harness pooling
  was proposed for this and REJECTED as benchmark farming — see Dead
  ends. Remaining honest levers, all within-run: the oracle-category
  checklist, diversity-gated attempts, observer codegen — idea pool
  §Gap 2/4 at the end of this doc.)
- Lang-27-o → NO MECHANISM. The overfit suppresses the crash everywhere,
  so crash-site pinning has nothing to pin on the patched side, and the
  bug is crash-shaped so there is no wrong VALUE to assert. P4.1 does
  not apply (behaviour differs — crash vs no crash). Genuinely open.
- Time-11-o → permanent by policy (cross-thread).

**Ceiling arithmetic:** 9 stable + Closure-62 (verifier fix) + Math-53 +
Math-57 (P3.1) + Chart-3 (P4.1) = 13 of 16 — exactly the dev target,
with Closure-33 as the swing leg and Lang-27/Time-11 out of reach. The
zero-false-alarm result matters more than it looks for held-out: 43
correct legs there, so the baseline's 1-in-14 FP rate would project to
~3 held-out false alarms against a budget of 1.

## Dead ends — do not revisit without new evidence

- **Cross-run harness/oracle pooling (REJECTED 2026-07-18 — this one is
  a hard NO, not an evidence question)**: persisting accepted harnesses
  or their checks between runs and rerunning them on later legs of the
  same bug. It farms the benchmark: dev recall inflates monotonically
  with repeated attempts while nothing transfers to a bug seen once —
  the held-out set and the real world both see each bug exactly once.
  The permitted boundary stays: sharing WITHIN one run between a bug's
  legs (P3.2 relation pooling, per-suite pool wiped each run) mirrors
  what one real deployment could do with one bug report and several
  candidate patches. Nothing may cross the run boundary.
- **Asking the prompt nicely to "explore beyond the seed input"**: tried
  twice (diag2, diagf), 3 false alarms each time — the instruction is
  ignored in practice.
- **Voting across a bug's several patches** ("if most patches behave the
  same way, trust that behavior"): repair tools tend to make the SAME
  mistake in all their patches for a bug, so agreement proves nothing.
  (Rule POOLING — P3.2 — shares the checks between patches, not the
  verdicts.)
- **Coverage-guided differential fuzzing for certification**: considered
  twice; every wrong "no difference" we ever found came from looking at
  the wrong OUTPUT, never from failing to find the right INPUT — P4.2
  fixes the actual cause.
- **Verifier majority voting**: measured; error rate unmoved at 3× the
  cost.
- **Spending effort on the unwinnable tasks**: Lang-7, Lang-22, Math-30,
  Math-59, Closure-115, Closure-123 (and the mislabeled correct sides of
  Lang-41 / Lang-10) are proven either behaviorally identical to the real
  fix in our environment or wrongly labeled — there is nothing to catch
  there.

## Standing predictions (falsifiable; check at each measurement gate)

**P0 gate outcome (2026-07-17, 5 legs, gpt-5.4, 350k tokens): 2 catches,
0 misses, 1 clean, 2 false alarms — both false alarms mechanistically
explained on the spot.**

**PHASE 2+3 COMBINED GATE (2026-07-17, p23gate, full dev set): NEGATIVE
RESULT — the interventions REGRESSED the pipeline.** Recall 0.56 → 0.43
(9/16 → 6/14); correct-side clean 13/14 → 8/12 (four false alarms).
Prior true-positives flipped to misses (Closure-33, Lang-41, Lang-60);
new false positives appeared (Closure-62, Closure-73, Lang-7); one gain
(Closure-73-o); Math-57 built no harness.

**CORRECTED FORENSICS (2026-07-17, full log audit of p1base vs p23gate —
the first-pass mechanism note below was wrong in two places):**
- **Lang-60-o (the crowding case, mechanism CORRECTED):** the gate
  harnesses did NOT lose the seed-anchored lifted oracle — all three
  carried `testLang295-contains` and it fired on buggy. What they lost
  is the free-form CAPACITY check (`lang295-capacity`: "capacity after
  deleteFirst/contains expected=43 actual=6") that the baseline model
  invented on its own and that was the actual patched-side convictor.
  The 3 injected relations (from a 4-own + 3-pooled merge, every one a
  `contains`-semantics relation) consumed the implementation slots, and
  no harness observed hidden state. So pooling hurt via CROWDING OUT
  free-form invention — not via displacing the seed witness.
- **The four FPs were NOT caused by pooling.** The gate ran correct legs
  FIRST precisely to seed the pool, so every correct leg ran with an
  EMPTY pool (logs confirm: correct legs only SAVED, never loaded).
  Their causes: Closure-62-c and Closure-73-c are lifted trigger-test
  oracles whose reconstruction diverges from the real test's setup
  (missing source-excerpt wiring; a trailing-semicolon mismatch) — they
  fired on the patched build although P0.1 had just PROVEN the real
  trigger test passes there, and the verifier kept them because it is
  never told that fact. Lang-7-c is a LATENT oracle (`meta-hex-case`,
  never fired on buggy) firing on an input that throws on any build —
  the verifier kept it despite the latent note (P0.4 step 2 evidence).
- **Chart-3-o (catch → NO harness, previously unexplained):** all three
  generation attempts were rejected by the P0.4 "unnamed oracle" static
  gate because the model built IDs dynamically
  (`"[oracle:" + id + "] ..."`) — valid at runtime, rejected statically.
  A latent gate bug surfaced by the higher check count, not by pooling.
- **Closure-33-o:** root-region anchoring re-aimed synthesis at
  `matchConstraint` internals; the resulting relation was unsound and
  the verifier RIGHTLY dropped it; baseline's winning compile-level
  whitespace-invariant was simply never proposed. **Lang-41-o:** the
  only patched-side firing was a genuine harness bug
  (`Math.abs(consumeInt()) % n` → negative index), rightly dropped.
  **Lang-50-c:** infrastructure death mid-run (log truncated during
  prompt assembly, no error) — not a method failure. **Math-57-c/o:**
  harness repair-loop exhaustion (javac fails / self-swallow /
  no-trigger).
The strict-improvement fixes from this phase (JSON double-escape
recovery, numeric tolerance, the direction/heading corrections) are
keepers. The remediation (implemented after this gate): pooled
relations become screening/replay-only (never prompt-injected), ≤2
own-leg relations in the prompt with an explicit keep-inventing-your-own
instruction, root-region anchoring demoted to advisory, dynamic oracle
IDs accepted, the trigger-test-passes fact passed to the verifier, and a
negative-modulo lint. Latent-oracle patched-side firings were FIRST
auto-dismissed mechanically — minfix_w1 proved that wrong (it killed the
true Lang-60-o capacity catch: a check behind an always-firing seed
oracle is latent on buggy precisely because the scan stops at the first
firing per input, and its first real chance to run comes when the
overfit silences the seed). Revised: replay the latent firing's EXACT
input on the buggy build mechanically and hand the verifier the outcome
(fires-on-buggy-too = patch didn't change the behaviour — sound if the
violated contract IS the reported bug; quiet-on-buggy =
patch-introduced violation).
**minfix_w1 (2026-07-17): correct side FULLY repaired — 6/6 clean
(Closure-62-c, Closure-73-c, Lang-7-c FPs gone; Lang-50-c and both
Math-57 legs evaluated again). Overfit side 0/5 — Lang-60-o explained
above (fix in place); Chart-3-o, Closure-33-o, Lang-41-o missed on
generation luck (the convicting check shape wasn't rolled this time;
Closure-33's seed-lift drops were CORRECT — the firing was the
harness's own externs artifact). These three are the target of the
P3.2 replay mechanism (relations execute standalone on both builds).** The deterministic probe still shows the Phase-2 direction logic is
individually correct — the failures were systemic (oracle mass, missing
facts at the verdict), not per-relation.

**PHASE-1 BASELINE (2026-07-17, full 30-leg dev set, gpt-5.4, ~2.5M
tokens across p1base + b + c): recall 9/16 = 56%, correct-side 13/14
clean (the one FP is the expected Chart-26-c flag pattern), positive-
prediction precision 9/10 = 90%. No unexpected false alarm anywhere.**
This is the number every later phase diffs against. Recall ≈ the old
pre-Phase-0 58% — as designed: Phase 0 bought TRUST (every miss now
explained, zero mystery FPs, full per-oracle attribution), not recall;
the recall gain is Phases 2–3. The 7 misses: Lang-50 & Math-2 (broad
divergence, discriminator generated-but-latent/stochastic), Lang-27
(crashing, lifted-crash-only), Math-53 & Closure-73 (narrow), Math-57
(witness, float-width), Time-11 (witness, cross-thread — the expected
permanent miss). The one unifying cause: the check that fired on buggy
was always the lifted seed, which the overfit special-cased. That is
precisely Phase 2's target, so the baseline validates the plan's
ordering without change.

**POST-REMEDIATION FULL DEV RUN (2026-07-17, full30/full30b, 30 legs,
gpt-5.4, ~2.0M tokens; config = all remediation fixes + P3.2 replay
[--replay_relations_on_patched] + P3.3 pinning + formula-first
synthesis): PRECISION 1.00 — ZERO false alarms on 13 evaluated correct
legs (Chart-26-c clean for the first time in ANY full run; Math-57-c
no_harnesses), recall 8/16.** The dev-goal "zero false alarms" is met.
The catch SET shifted vs baseline: three NEW catches that baseline never
had (Math-2-o — deterministic, via the pooled mean-formula replayed
against the patched build, confirmed twice; Lang-50-o, confirmed three
times; Closure-73-o), while four baseline catches missed this run:
Chart-3-o (stable miss, 4 consecutive — the baseline catch was a lucky
loose lift), Closure-33-o (flaky oracle roll; caught twice in minfix
confirms), and two SELF-INFLICTED losses diagnosed in-run with fixes:
Closure-62-o (the P4.3 dismissal-wins reconciliation transferred an
unsound verdict on one harness's generic `lifted-test` ID onto a
DIFFERENT check with the same name in another harness — scoping fix:
cross-harness transfer only for injected-relation names) and
Closure-92-o (raw-string lifts of compiler output fire on formatting
deltas, giving the verifier a legitimate dismissal that buries the
content difference — fix: whitespace-normalized text comparisons,
mandated in the lift instructions). Both fixes were confirmed on a
4-leg suite (recov2): **Closure-92-o flipped to a CATCH** (the
normalized comparison surfaced the real content difference — an
unrewritten `goog.provide` — undeniably) with both guards clean
(Lang-60-c, Closure-62-c). Closure-62-o stayed a miss, and the residual
mechanism is now precisely identified: two of its three firings were
CORRECTLY dropped (the harnesses genuinely mis-reproduced the test
setup), and the third — the true catch, caret at charno==length, the
very boundary the bug is about — was dropped because the verifier read
the BUGGY body's `charno < length` guard as the contract. That is a
trust-hierarchy gap at the VERIFIER (the P2.1 direction rule — "where
the failing test pins a behaviour, the test wins over the shown buggy
body" — was applied to synthesis but never to the verifier's own
prompt). **DONE same day (c62confirm): the hierarchy sentence was added
to relation_verifier's guidance and Closure-62-o flipped to a CATCH
with the Closure-62-c guard clean — effective dev state 10/16 caught at
precision 1.00.** (Per measurement rule #2 this flip still wants its
confirm-repeat; it gets one for free in the next full pass.) Remaining misses map to planned work:
Math-53/Math-57 → P3.1 (field-level observers, extreme widths),
Lang-27 → P3.3 full, Chart-3 → P4.1 (baseline-buggy comparison),
Time-11 → permanent (cross-thread). NOTE for later runs: the pool is
now per-suite (RELATION_POOL_DIR set by run_suite.sh) and pooling needs
the correct leg to FINISH before its overfit sibling starts — true at
PARALLEL=4 on 30 legs, NOT true for 2-leg suites (run those serial).

1. The Chart-26 correct side flips false-alarm → clean from P0.3 alone.
   **PARTIALLY FALSIFIED at the P0 gate**: P0.3 DID mechanically drop the
   wrapped-crash variant (the historical FP), but a second harness
   smuggled the same pre-existing crash through as MESSAGE TEXT with the
   alarm thrown outside any catch (a "flag pattern" — no cause chain for
   P0.3 to see). That variant is exactly what P3.3's crash-site pinning
   addresses; until then Chart-26-c remains an expected false alarm.
2. The Math-2 correct side is genuinely clean now (file repaired + the
   check uses a tolerance). **CONFIRMED after one more P0-class fix**:
   the gate's Math-2-c "false alarm" was a PHANTOM — libFuzzer wrote a
   `slow-unit-*` artifact on a clean exit-0 run and the crash classifier
   counted "Test unit written to"/"artifact_prefix" as crash markers.
   Fixed (crash-* artifacts only); no real oracle fired on the patched
   build.
3. The Math-2 overfit side stays a miss until P3.2, then flips to a
   catch via the mean-formula rule. **RESOLVED with a twist: caught
   FLAKILY.** Math-2-o was a catch at the P0 gate and a miss at the
   baseline — a real flip-flop. The firing check reads `sample()`, a
   random draw, so the verdict depends on fuzzing luck. The spirit of
   the prediction holds — P3.2 is still needed — but not because it is a
   hard FN: because the deterministic discriminator (`getNumericalMean`
   = −49.76) is generated-but-latent, and P3.2/P2.2 must surface it to
   make the catch RELIABLE rather than lucky.
4. The patches with tiny behavior differences show the lowest catch rate
   before P3.1 and the largest improvement from it. **PARTIALLY BORNE
   OUT at the baseline**: the two narrow legs (Math-53: 3, Closure-73:
   7) are both misses — but so are two BROAD legs (Lang-50, Math-2), so
   narrowness is not the sole predictor of a miss; latent/stochastic
   discriminators miss regardless of breadth. The narrow legs remain the
   cleanest P3.1 targets.
5. The five wrongly-cleared patches flip to "difference found" under
   P4.2 with no prompt changes.
6. The witness-only overfits are all misses at the Phase-1 baseline, and
   P3.1's two new check types flip at least Lang-60 and Lang-41.
   **FALSIFIED — in the good direction.** The pipeline caught 5 of the 7
   witness-only overfits at baseline ALREADY (Chart-7, Lang-41, Lang-60,
   Closure-62, Closure-92); only Math-57 (float-width) and Time-11
   (cross-thread, expected) missed. "Witness-only" describes the
   certifier's single probe, not the pipeline's several diverse
   harnesses — the pipeline finds surfaces the one probe didn't. P3.1's
   remaining witness target is really just Math-57.

---

## Idea pool for the uncovered gaps (rewritten 2026-07-18 in plain
## words; nothing here is committed work until it earns a phase slot)

To say WHERE each gap lives, here is the assembly line one leg goes
through, numbered once so the gaps below can point at stations:

1. **Setup** — apply the patch, prove the bug's test fails before it
   and passes after it. *(solid)*
2. **Rule-writing** — a model reads the changed code, its docs and the
   failing test, and proposes up to 4 general rules a correct program
   must obey. *(works; upgrade plan R1–R5 below)*
3. **Rule screening** — each rule is compiled and run 20,000 times
   against the buggy program to weed out rules that accuse everything.
   *(solid)*
4. **Harness writing** — a model writes three fuzzing programs full of
   checks: copies of the failing test, the screened rules, and checks
   it invents itself. *(the biggest remaining weakness: WHICH checks it
   invents is a dice roll per run)*
5. **Acceptance** — each harness must prove it fires on the buggy
   program; we record which check fired and what crash was underneath.
   *(solid)*
6. **Judgment day** — the harnesses fuzz the PATCHED program, and —
   separately — every screened rule is compiled on its own and run
   directly against the patched program ("replay"). Any check that
   fires here is an accusation. *(replay is the healthiest new part)*
7. **The judge** — a model reviews every accusation: would EVERY
   correct program satisfy this check, or could a correct one trip it?
   Only kept accusations count. *(much better since it gets computed
   facts; still one model opinion when no fact applies)*

### Gap 1 — bugs whose only symptom is a crash, where the overfit
### deletes the crash (Lang-27). Lives at stations 2 and 4.

The problem in plain words: for this bug our only trusted signal is
"this input makes the program crash". The overfit patch wraps the code
so it never crashes — anywhere. Now nothing fires. And we cannot ask
"did it return the RIGHT value instead?" because the buggy program
never returned any value there — it always crashed — so there is no
trusted answer to compare against.

- **Idea 1a — aim the value rules at the crash's inputs.** When a
  patch removes a crash, the method now RETURNS something on those
  inputs — and the laziest patches return nonsense. We cannot know the
  one right answer, but the rules from station 2 don't need one: a
  round-trip rule ("format it, parse it back, you must get the same
  thing") or a documented-contract rule can condemn nonsense without
  knowing the right value. Today those rules are exercised on random
  inputs; the idea is to make the crash's own inputs (which we already
  capture) part of the inputs every rule is tried on. Cheap: it is
  just seeding station 3/6 with inputs we already have.
- **Idea 1b — "this input MUST be rejected" rules.** Where the
  documentation says "throws an error for malformed input", a patch
  that silently returns something on clearly-malformed input is
  convictable. Only usable where the docs literally say it, otherwise
  we would be inventing our own idea of "malformed".
- **Honest limit:** a patch that removes the crash AND returns
  sensible, contract-respecting values everywhere is close to a real
  fix, and these ideas will not catch it — such a patch may belong in
  the UNDETECTABLE list, not on the backlog.

### Gap 2 — the dice roll in harness writing (Closure-33 keeps
### flipping). Lives at station 4.

The problem: the winning check for some bugs is one the model INVENTS —
and whether it invents it in a given run is luck. Closure-33's winning
check ("append a comment to the program; the compiler's warning count
must not change") was invented in two runs out of five.

- **Idea 2a — keep proven harnesses across runs: REJECTED (2026-07-18),
  do not build, do not re-propose.** It would inflate our benchmark
  numbers by farming the same bugs across repeated runs while telling
  us nothing about a bug seen once — which is what the held-out set
  and the real world are. Recorded in Dead ends. The permitted
  boundary: sharing between a bug's several patches WITHIN one run
  (that mirrors a real deployment holding one bug report and several
  candidate patches at once).
- **Idea 2b — checklist of check-kinds, with one targeted retry.**
  After the harnesses are written, mechanically list which KINDS of
  check they contain (test-copies / rules / sibling-comparisons /
  hidden-state checks — readable from the check names). If a kind that
  applies is missing, spend ONE extra attempt asking for exactly the
  missing kind. This turns "please write diverse checks" (advice,
  often ignored) into a counted condition with a consequence.
- **Idea 2c — extra attempts only when diversity is low.** Cheaper
  cousin of 2b: only roll more dice when the first rolls came up
  same-ish. Note: more dice from the same cup mostly repeats the same
  distribution — 2b aims the extra roll, 2c just adds one.

### Gap 3 — the judge is still one model opinion when no computed fact
### applies. Lives at station 7.

- **Idea 3a — measure the judge before tuning it (best
  effort-to-insight ratio on this list).** This week's forensics named,
  for dozens of archived judge decisions, what the RIGHT decision was.
  We already have a tool (verifier_replay) that re-asks the judge
  offline. So: replay those decisions under different judge settings
  (1 vote vs 3 diverse votes; with and without each computed fact) and
  MEASURE the error rates, instead of believing the old "voting doesn't
  help" result that predates the computed facts. Zero risk — offline.
- **Idea 3b — let the failing test's own values override the judge.**
  Where an accusation's expected value is literally one the failing
  test asserts, the judge should not be able to dismiss it. Tempting —
  but the Closure-62-c false alarms were EXACTLY such values fired
  from a badly-rebuilt scenario, so this needs the measurement from 3a
  first. Parked behind 3a.

### Gap 4 — the planned P3.1 checks must not be delivered as advice.
### Lives at station 4 (delivery), for the benefit of stations 2/6.

The lesson this cycle taught repeatedly: advice into station 4 gets
implemented sometimes; code and machinery always run. So:

- **Idea 4a — generate the observer code, don't describe it.** P3.1's
  "after a read-only call, re-check the object's cheap properties"
  should be produced as actual Java (call every public no-argument
  getter before and after; compare) that the harness template already
  contains — not as a paragraph asking the model to please do that.
  Limit: only getters the docs mark as pure, or the observer itself
  perturbs the scenario.
- **Idea 4b — a boundary corpus built from the docs.** For replay
  (station 6), mechanically add inputs at the documented limits (range
  endpoints, values near 2^31, 1e20) instead of hoping random bytes
  get there. Firings at such extremes carry a warning to the judge,
  because correct programs legitimately lose precision out there.

### Gap 5 — Time-11, visible only from a second thread. Outside the
### pipeline by policy.

- **Idea 5a — one cheap experiment before declaring it permanent
  forever:** the flaky part of thread bugs is TIMING. But if this bug
  is really about initialization ORDER (which class got set up first),
  order can be controlled deterministically: run the program twice in
  separate processes with a different forced setup order and compare.
  Unvetted; may not match this bug's actual shape; explicitly NOT in
  the plan until someone spends the one hour to try it.


### Rule-synthesis upgrade plan (R1–R5) — the concrete "what to do about
### rules", superseding the earlier three-item ordering

Grounding data (full30, 28 synthesizing legs): 4 candidates per leg,
~1.7 survive screening; rules-through-replay contributed to 5 of the 8
catches. Losses: 25 of ~112 candidates never compile (no repair round
exists for rules — a compile error is death), 12 fire too broadly, 4
direction-suspect. 76 of 78 survivors never fire on random buggy-side
inputs — the trigger-literal corpus and the patched-side replay are
where rules do their work, random fuzzing only validates them. The
doc-rich/doc-poor split is stark: Math/Lang/Time legs routinely keep
2–5 rules; 3 of 4 Closure synthesis rounds ended with ZERO survivors.

**R1 — compile-repair round for rule candidates (do first; pure
recovery).** When a candidate fails to compile at screening, feed the
javac error + the candidate back to the synthesis model ONCE for a
corrected version, exactly like the harness repair loop. Recovers up to
~22% of all candidates for one cheap call each. Validate: count of
compile-deaths across a 4-leg micro-suite drops by more than half; no
change in survivor quality (screening still judges them).

**R2 — six candidates with an ANCHOR-DIVERSITY quota (doc-rich boost).**
Raise "up to 4" to "up to 6", and require each rule to carry a declared
anchor tag: (a) documented contract/formula of the changed method,
(b) methods the failing test reads, (c) sibling-agreement between
overloads/variants, (d) domain-level input-transformation (see R4).
Mechanical gate: if all candidates share one tag, one retry asks for
the missing categories (tag counting is trivial; the tag also rides
into the screen log so we can measure which anchors actually convict).
Why quota beats raw count: successive rules from ONE anchor are
variants — Math-2's runs produced disjoint 4-sets across runs, showing
the contract holds 8–10 distinct rules, but 4 slots from one anchor
never span them. Validate: doc-rich micro-pair (Math-53) — survivor
count and anchor spread both rise; no new FP.

**R3 — doc-density mode switch + passing-test extension (the doc-poor
answer).** Mechanically measure how much documentation the touched
methods actually have (javadoc characters per touched method — already
extracted for the prompt, so measuring is free). BELOW a threshold,
rule-writing pivots its primary anchor from "the documented contract"
(which barely exists there) to the project's OWN PASSING TESTS near the
changed code (already mined and shown as usage examples today — wasted
as a spec): each rule takes one passing test's scenario and asserts
that the property the test checks stays true under a harmless
variation of the input — where "harmless" may come ONLY from the R4
menu, never from free invention.
*Is this general, or benchmark-shaped? The argument, made before
building so held-out can falsify it:* (i) it uses only artifacts every
real task has — a project's own test suite — no bug shapes, nothing
specific to this dataset; (ii) the trust argument is structural, not
empirical: a passing test holds on the buggy program AND on any
correct patch by definition, so a rule built on it can only convict a
patch that breaks previously-working behaviour beyond the reported bug
— a general failure mode of automated patching, not a quirk of these
bugs; (iii) it is the same trust move the pipeline already makes with
the FAILING test ("tests are specification"), extended from one test
to the suite; (iv) "vary a tested scenario harmlessly, re-check its
property" is standard metamorphic testing, not something invented for
this benchmark. What it does NOT cover, said plainly: overfits whose
damage lies far from every existing test's neighbourhood — for those
the contract rules (doc-rich path) remain the only net. Honest caveat:
the design is general but our supporting evidence so far is one
benchmark leg (Closure-33's historical winner has exactly this shape —
append a comment to testIssue700's program, the warning count must not
change); the held-out run is where the generality claim gets tested,
and nothing in the mechanism may be tuned per-bug on the way there.
Validate: Closure-33-o (its winner becomes derivable-by-recipe instead
of a lucky roll); Closure-92-o second target; Closure-62-c as the FP
guard.

**R4 — name the input's KIND up front, and attach its fixed list of
harmless variations (feeds R2 and R3).** Two parts, and the first is a
context change, not a menu:
(a) *Input-kind as context, stated at the TOP of rule-writing.* Today
the rule-writer sees code, docs and tests but nothing that NAMES what
kind of data the code consumes. That is mechanically readable from the
entry point's signature (a String fed to a compiler = program text;
int/double parameters = numbers; a List = a collection), so compute it
and state it first: "the public entry points consume JavaScript source
text". Knowing the kind changes which rules even make sense to
propose — this belongs at the start of the context, not as an
afterthought.
(b) *The "closed menu": a short FIXED list, written once into the
pipeline's standing instructions — not generated per bug, not learned
from this benchmark.* One entry per broad input kind, each a
universally-true harmless variation for that kind of data:
program/markup text → inserting whitespace or a comment changes no
meaning; parse/print pairs → print then re-parse must give the thing
back; numbers → only invariances the docs state (scaling,
translation); collections → order must not matter where the docs say
order does not matter; formatters → the output must parse back to the
input. "Closed" means the model may only PICK from this list when it
needs a harmless variation (for an R3 rule), never invent its own —
because a freely-invented "harmless" change that is not actually
harmless is precisely how unsound rules are born. Where does the list
come from? From universal facts about kinds of data — the same trust
tier as "sorting twice = sorting once" (source #4 in this doc's
ranking) — and it may only ever be extended with entries of that
universality, never with anything derived from a benchmark bug. If a
task's input kind is not on the list, R4 simply contributes nothing
for that task (fail-safe: no rule rather than a wrong rule). These are
facts about input DOMAINS, not about bugs, so they respect the
no-dataset-overfitting rule the same way the read-only/sibling
categories in P3.1 do.

**R5 — validation ladder for all of the above.** One doc-rich pair
(Math-53), one doc-poor pair (Closure-33 + Closure-92 overfits with
Closure-62-c and Chart-26-c as FP guards), iterate-cheap; then the
standing rule — any flipped outcome gets its confirm repeat — then the
next full30. Success criteria: compile-death rate halved (R1), anchor
spread visible in screen logs (R2), at least one doc-poor leg convicted
by a rule with a test-extension or domain tag (R3/R4), zero new FPs.

Relationship to earlier items in this pool: R2 subsumes idea 2b for the
rule path (the checklist idea remains relevant for harness-side check
categories); R1/R3/R4 are new; the earlier informal ordering
(compile-repair → diversity → count) is superseded by R1→R2→R3→R4→R5.

### Harness-generation upgrade plan (H1–H7) — station 4's "what to do",
### parallel to the rules plan above

Quality verdict first, from the run data: accepted harnesses reliably
fire on the buggy build and their test-copies are faithful. The two
SYSTEMATIC weaknesses are (a) scenario-rebuild fidelity — the harness
rebuilds the failing test's scenario slightly wrong and the difference,
not the patch, makes checks fire (the source of every lifted-check
false alarm this week), and (b) check diversity — which self-invented
checks appear is a dice roll (the source of the flaky catches). The
plan attacks (a) with context and a mechanical gate, (b) with aimed
context; more dice come last.

**H1 — complete the test's context (highest value; found by direct
inspection).** The harness prompt today shows ONLY the failing test's
method body. Closure-62's method calls `formatter("assert (1;")` — a
helper defined elsewhere in the test class that performs exactly the
setup the harness keeps getting wrong — and uses `FOO_TYPE`, a class
constant. Neither is shown; the model improvises the setup and every
setup-divergent failure follows. Fix mechanically: resolve the
identifiers the test method uses against its test class and include
what they refer to — setUp()/@Before methods, helper methods, class
constants — plus any fixture FILE the test references by a string
literal that matches a repo path (include its content). Validate:
Closure-62-c stays clean and its o-side lifts finally rebuild the true
scenario; Chart-26-c's entity wiring stops being improvised.

**H2 — show the real failure output.** Setup (station 1) already runs
the failing test on the buggy build; its JUnit message ("expected:<X>
but was:<Y>") names the exact observable that diverges AND the wrong
value the bug produces. Capture it and put it in the harness prompt.
Costs nothing (the run already happens); grounds the writer's choice
of what to observe.

**H3 — mechanical setup-fidelity gate (kills the setup-divergence FP
class at the source).** With H2's value in hand, acceptance (station
5) can check: a test-copy check firing on the buggy build must observe
the SAME wrong value the real test observed. If the harness's copy
observes something different, its scenario is NOT the test's scenario
— reject with exactly that message and let the repair loop fix the
setup. This turns the "if the firing replays the test's scenario…"
judgment we currently ask of station 7 into a station-5 mechanical
comparison.

**H4 — list the touched class's cheap observables.** Mechanically list
the public no-argument getters of the touched class ("state you can
read: capacity(), length(), size()") in the prompt. This is the raw
material of hidden-state checks (the kind that convicts Lang-60), and
today the model must dig it out of a truncated class skeleton. Pairs
with observer CODEGEN (idea 4a) which puts the before/after reads into
the template as code.

**H5 — list the sibling pairs.** Mechanically list same-name overloads
and doc-identical method pairs of the touched class
("getPackageName(Class) / getPackageName(String) are documented to
agree"). Sibling-agreement checks convict Lang-41; today they exist
only when the model notices the pair on its own.

**H6 — tell the writer (and the judge) about known pre-existing
crashes.** Acceptance and the latent scan already OBSERVE the generic
crashes that live in the buggy build (the text-measuring crash behind
every Chart-26 flag-pattern false alarm). Collect their identities and
state them: "these exceptions exist on the buggy build and are not the
bug — never convert them into an alarm". Same list rides to the
station-7 judge. Kills the flag-pattern class at the source instead of
at judgment.

**H7 — more harnesses? Only aimed, never blanket.** Measured, both
directions: rerolls DO flip flaky legs (the w2 rerolls recovered
Closure-33 and Lang-41 after w1 missed them) — but every extra harness
is also a false-alarm lottery ticket on the correct sibling (each FP
class this week arrived via ONE harness in ONE roll), and the current
zero-false-alarm state was measured at n=3. So: keep n=3 as default;
spend extra attempts only through the check-kind checklist (idea 2b —
one aimed retry when an applicable kind is missing); prefer H1–H6,
which raise the quality of every roll, over raising the number of
rolls. Ordering: H1+H2 first (one context change, evidence already in
hand), H3 with them (it depends on H2), then H4–H6, then 2b.

### Meta-rule distilled from this whole cycle (applies to every idea
### above)

A recall mechanism only counts if its check reaches the patched build
MECHANICALLY (replay, codegen, within-run pooled relations — never
anything carried across runs, see Dead ends) — anything delivered as
prompt advice gets multiplied by the harness generator's implementation
rate and the fuzzer's input luck, and history shows that product is
small. And every new conviction path must arrive WITH its mechanical
fact for the verifier, or it will be spent on one plausible dismissal.
