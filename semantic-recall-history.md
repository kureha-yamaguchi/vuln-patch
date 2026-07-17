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
`suites/UNDETECTABLE.md`; diag24 header updated (3 expected permanent FNs:
Lang-7, Lang-22, Math-70).

**B3 expansion-pool sweep + zero-divergence deep-dive (same day, pm):**
61 legs probed across all 33 paired bugs (~575k tokens? no — ~350k; exact
totals in b3_sweep.log). Headline findings, full detail in
`suites/UNDETECTABLE.md`:
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
  instruction candidates recorded in UNDETECTABLE.md.
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
failure worth debugging; any FN on an UNDETECTABLE.md leg is expected.

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
- **Certifier probe-v3** (already drafted in UNDETECTABLE.md).
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
