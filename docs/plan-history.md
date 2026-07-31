# Plan history — archived working sections (moved out of plan.md 2026-07-31)

Everything here is FROZEN evidence and superseded planning, moved verbatim from
docs/plan.md to keep the live plan readable. Nothing here governs current work; the
live rules and current state are in docs/plan.md. Companion frozen records:
docs/cycles/ (cycle specs + retros), docs/replay/ (per-measurement analyses),
docs/judge-verdict-inventory-2026-07-26.md (the 228-verdict population).

Structure:
- PART 0 — Pre-restructure era (2026-06 → 07-17): the July-16 failure-analysis
  write-up and the full pre-restructure plan text (formerly
  `semantic-recall-history.md`, merged here 2026-07-31).
- PART 1 — The by-station plan era (2026-07-18 → 07-20): DONE ledger, scoreboard,
  station TO-DOs (contains the full P4.1/P4.2 specs), the menu finding, the replay
  retraction/CORRECTION, batch3–6 results, full30v2, falsefix13, attribution,
  focused-synthesis, J1, foc15/b, harn6, the A/B verdict.
- PART 2 — The cycle era (2026-07-26 → 07-30): cycle-4+ candidate ledger, the
  night20 action plan with its embedded adjudications, cycle-5/6 outcomes as recorded
  in-plan, the 30-leg pre-commitment v1 and both refreshes, Lang-63 decomposition,
  cycle-7 plan and its kill records, repair-in-place validation and measurement.

=============================================================================
PART 0 — PRE-RESTRUCTURE ERA (2026-06 → 07-17) — formerly semantic-recall-history.md
=============================================================================

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


=============================================================================
PART 1 — BY-STATION PLAN ERA (2026-07-18 → 07-20)
=============================================================================

## DONE — shipped and validated, by station

**Current state (2026-07-18): dev set 10 of 16 overfits caught at ZERO
false alarms** (13 correct legs clean; Math-57-c unscored because its
harness generation is fragile). The 2026-07-17 baseline was 9/16 with 1
false alarm — but 3 of those 9 catches were later shown to be luck.
Run history, one line each: p1base 9/16, 13/14 clean → p23gate (Phase
2+3 turned on at once) REGRESSED to 6/14 with 4 false alarms →
minfix_w1 repaired the correct side (6/6 clean; 0/5 catches — found
the latent-auto-dismiss mistake) → minfix_w2/w2b recovered 5 catches
(incl. Lang-50-o for the first time ever) and exposed 3 more
mechanical bugs → math2pair2 proved the deterministic replay catch
(Math-2-o) → full30: precision 1.00, recall 8/16 → recov2 added
Closure-92-o → c62confirm added Closure-62-o = 10/16 at precision
1.00.

**Station 1 — Setup.**
- Patch applier hardening (P0.1): edit blocks applied one at a time,
  sorted; any failure aborts. (Lang-50's reversed-order patch was
  silently half-applied for weeks; Math-2's truncated patch never
  applied at all while scoring counted the do-nothing runs as passes.)
- Safety net (P0.1): the bug's trigger test must FAIL on buggy and
  PASS on patched before anything else runs; otherwise the leg is
  marked `bug_not_reproduced` / `bad_patch`, never silently scored.
- *Quality check (2026-07-18): this station works well and needs no
  fixing. One criticism: it LEARNS useful things and then throws them
  away. When it runs the failing test on the buggy program, that run
  produces (a) the test's failure message — which literally says which
  value came out wrong and what it should have been — and (b) the
  exact inputs that trigger the bug. Today neither is passed on to the
  later stations. Two TO DO items reuse them: H2 passes the failure
  message to the harness writer, JD1 passes the trigger inputs to the
  patched-side fuzzing.*

**Station 2 — Rule-writing.**
- Direction grounding (P2.1): the failing test's source and expected
  values are the top trusted block; the buggy code is labeled BUGGY
  (it was mislabeled "Patched" for the project's whole history, which
  made Lang-7's rules come out exactly backwards); the diff keeps its
  +/- markers.
- Anchoring: first rule constrains the changed method's documented
  contract; the failing-test-neighbourhood methods are a SECONDARY,
  advisory anchor (as a mandate they re-aimed synthesis at internals
  and lost Closure-33's compile-level winner — p23gate).
- Formula-first: if a touched-class numeric getter's javadoc states a
  closed-form formula, the FIRST rule must be that formula (Math-2's
  formula — the strongest rule class — otherwise appears only by
  luck; it skipped 2 of 4 runs before this).
- Numeric tolerances: generous magnitude-scaled tolerance with a
  looser floor for large-integer arithmetic (a 1e-12 tolerance once
  false-fired on the CORRECT Math-2 patch at billion-scale
  parameters).
- JSON double-escape recovery (literal \n → newline): rules used to
  arrive corrupted and die at compile.

**Station 3 — Rule screening.**
- Self-swallow lint + forced-alarm canary (P0.2): a rule whose alarm
  throw sits inside its own catch-everything block is rejected before
  compiling (about half of all Lang-7-era checks silently swallowed
  their own alarms and screened as "well-behaved").
- Direction check (P2.2): every rule is replayed on exactly the
  failing test's input literals, twice (also a determinism test).
  Fires there = direction-confirmed → ranked first, exempt from the
  fire-rate cap.
- INVERTED demotion, not deletion: "silent on the trigger corpus but
  loud on random inputs" cannot distinguish a backwards rule from a
  sound rule whose violation region random bytes never reach — the
  hard drop once deleted Math-2's mean-formula. Such rules become
  replay-only (never prompt-injected).
- Constraint parity (P2.3): rules that need a forbidden custom
  subclass are rejected at screening instead of silently dropped by
  the harness writer later (that silent drop cost Math-2 its convicting
  rule once).
- Negative-modulo lint (also a station-4 gate):
  `Math.abs(consumeInt()) % n` goes negative at Integer.MIN_VALUE —
  this exact harness bug produced Lang-41-o's only firing in one run
  and cost the verdict.
- Survivor cap raised to 8; the harness prompt is sliced separately
  (see station 4), the rest feed the pool and replay.

**Station 4 — Harness writing.**
- Prompt thinning: at most 2 rules injected, own-leg only, never
  pooled ones — injected sibling-leg rule mass crowded out the
  free-form capacity check that convicts Lang-60 (p23gate). Plus an
  explicit "keep inventing your own checks" instruction naming the two
  historically-winning shapes (hidden-state and sibling-agreement
  checks).
- Dynamic oracle IDs accepted: `"[oracle:" + id + "]"` is valid at
  runtime; the static gate that rejected it cost Chart-3-o all three
  harness attempts in one run (zero harnesses, leg unscoreable).
- Every alarm format must carry an ID: the `"metamorphic violation:"`
  format was invisible to every per-check mechanism (latent scan,
  crash pinning, judge notes) and produced a Chart-26-c false alarm.
- Whitespace-normalized text lifts: expected code/text strings are
  compared with whitespace collapsed. Raw-string comparison fired on
  formatting deltas and handed the judge a legitimate dismissal that
  buried the real content difference (all three Closure-92-o firings
  in full30); the normalized version surfaced the actual difference —
  an unrewritten `goog.provide` — and convicted.
- Extreme-magnitude fence: fuzzed numeric parameters are capped to
  moderate ranges unless the contract covers extremes — at
  billion-scale a CORRECT implementation's double arithmetic
  legitimately degrades; three separate Math-2-c false accusations
  came from checks at such magnitudes (a NaN probability at N≈2^31,
  constructor validation on overflowed ranges, twice).
- Setup-faithfulness instruction: replicate the test's environment
  (registered files, locales, modes) exactly or drop that check. (The
  instruction version; the mechanical version is H1–H3 in TO DO.)

**Station 5 — Acceptance.**
- Per-check bookkeeping (P0.4): acceptance records WHICH named check
  fired on the buggy build; checks that never fired are flagged LATENT
  (a never-exercised check once met its first-ever execution on the
  correct patch and false-alarmed — Chart-26).
- Buggy-side crash identity per check (P3.3 data): which exception
  types stood behind each check's firings on buggy.

**Station 6 — Judgment day.**
- Replay (P3.2, `--replay_relations_on_patched`): every screened rule
  (own-leg; pooling removed 2026-07-19) is compiled unchanged against
  the patched build
  and run two ways: on the failing test's own inputs (deterministic
  tier) and on 20k fuzzed inputs; firings go to the judge like any
  other accusation. The biggest recall mechanism shipped: contributed
  to 5 of 8 catches in full30 and made Math-2-o deterministic (the
  mean-formula fired 7,144/20,000 on the Arja build, two runs in a
  row).
- Within-run pooling (P3.2) — REMOVED 2026-07-19 (see the tightened
  no-pooling ground rule): every leg is now fully self-contained;
  synthesis stochasticity is compensated by more own rules per leg
  (--synth_max_rules 8) and by replaying every screened survivor.
- Phantom-crash fix: libFuzzer `slow-unit-*` artifacts no longer count
  as crashes (a clean exit-0 run was once scored as a false alarm).
- Crash-type pinning (P3.3): a must-not-crash check only counts on the
  patched build if its underlying exception TYPES overlap the ones
  recorded on buggy — a different crash wearing the same alarm is
  dismissed. (Type-level on purpose: a half-fix that moves the same
  exception one frame stays a catch.)

**Station 7 — The judge.**
- Cause-chain rule + differential replay (P0.3, broadened): an alarm
  wrapping a caught crash must carry the original as its cause; an
  escaped exception whose exact input reproduces the same crash on
  buggy is pre-existing surface — dismissed. Broadened to ALL escaped
  non-alarm exceptions on semantic legs (a `NotPositiveException` from
  junk fuzzed constructor input was twice kept as a "conviction" of
  the correct Math-2 patch before this).
- Latent-firing fact: a latent check firing on patched triggers a
  mechanical replay of that exact input on the buggy build; the judge
  is told "fires there too — the patch did not change this behaviour"
  or "quiet there — the patch introduced it". (The first version
  auto-dismissed latents outright and killed the true Lang-60-o
  capacity catch.)
- Symmetric-firing fact: "this check also fired on buggy — keep only
  if the violated contract belongs to the reported bug's own behaviour
  family." Kills the Chart-26-c axis-entity false-alarm class while
  keeping Lang-41-o, whose TRUE catch has the same fires-on-both
  shape — which is exactly why this could not be a mechanical
  dismissal.
- Trigger-test-lift fact (matched in check IDs AND message text): when
  a fired check is a copy of a trigger test, the judge is told the
  REAL test passes on this build — a faithful replay cannot
  legitimately fire. Killed the Closure-62-c / Closure-73-c
  false-alarm class.
- Escaped-exception fact: "this firing carries no check ID — it is not
  one of the harness's checks; junk-input validation is the usual
  cause."
- "Dismissal wins" reconciliation (P4.3), scoped: the same check judged
  unsound on one firing and kept on another → the dismissal wins.
  Scope: across harnesses only for injected-rule names; a generic
  model-invented ID (`lifted-test`) names DIFFERENT checks in
  different harnesses, and the unscoped version transferred an unsound
  verdict onto the sound Closure-62-o catch and killed it (full30).
- Trust hierarchy: "the shown code may be the bug — where a trusted
  failing-test value pins a behaviour, the test outranks the shown
  body's guard logic." The judge had read the buggy `charno < length`
  guard as the contract when that boundary IS Closure-62's bug.
  Confirmed: Closure-62-o flipped to a catch, correct-side guard
  clean.

---

## SCOREBOARD — where every dev leg stands (2026-07-18)

Overfit legs (16):
- **Caught, stable (each confirmed on at least 2 consecutive runs):**
  Chart-7 (5 of 5 runs since Phase 0), Chart-26 (5/5), Closure-73,
  Lang-41 (4 of last 5; the one miss was a harness bug now linted),
  Lang-50 (3/3 since first caught — never caught before this cycle),
  Lang-60 (4/4 since the latent fix), Math-2 (2/2 since replay —
  deterministic via the mean-formula), Time-4 (always).
- **Caught, one confirm still pending (rule #2):** Closure-92 (via
  normalized text lifts), Closure-62 (via the trust hierarchy). Both
  get their repeat for free in the next full pass.
- **Missed, fix owned:** Math-53 → OBS below (the library's own
  equals() treats all-NaN values as equal, hiding the 3-output
  divergence; only field-level reads see it). Math-57 → BND below
  (float-vs-double width, visible only near 1e20; also its harness
  generation died in 3 of 5 runs — javac failures in the repair loop).
  Chart-3 → P4.1 below (missed 4 consecutive runs; its baseline
  "catch" was a lucky loose test-copy — a faithful copy can never
  catch it because the overfit passes the faithful scenario by
  construction). Closure-33 → R3/R4 below (its winning check was
  invented in 3 of 6 runs; pure dice today).
- **Missed, no mechanism yet:** Lang-27 (crash-shaped bug; the overfit
  suppresses the crash everywhere, and the buggy build never returned
  a value there, so there is no trusted answer to compare — candidate
  ideas under CRASH below).
- **Permanent by policy:** Time-11 (cross-thread; one cheap experiment
  listed before final acceptance — T11 below).

Correct legs (14): 13 clean in full30 — ZERO false alarms, the first
full run ever without one (the baseline had 1, and p23gate had 4).
Math-57-c unscored (no harnesses built). Every false-alarm class ever
observed now has a named mechanical guard (see Station 7 DONE); the
residual risk is a brand-new unsound check with no matching fact,
which is what J1 below measures.

---

## TO DO — by station, ordered by impact vs risk

**IMPLEMENTATION STATUS (2026-07-19 eod — read this before the list).**
Much of the list below is now BUILT. The detailed descriptions are kept
verbatim for their rationale, but their current state is:
- SHIPPED & validated: H1 (whole test), H2 (real failure message), H3
  (setup-fidelity gate — armed after the hfix11 extractor fixes), JD1
  (buggy-input seeds), R-THROWS (Math-53-o caught via
  `add_null_rejected_with_nullargumentexception` in fpfix6 — first ever),
  J3 (judge sees the failing test), the replay flag repair, harden fix,
  pooling removal, synth-8 + all-tripwires.
- SHIPPED, awaiting measurement: H1/H2 extended to rule synthesis;
  multi-line constant capture; H4 (readable state) + H5 (look-alike
  methods / factory families) as one mechanical block into both harness
  and synthesis prompts; keep_going on patched fuzz; SYM-2 (crashing
  defect-family fact) + SYM-2b (semantic input-replay fact) — the fixes
  for the fpfix6 Lang-27-c and Closure-62-c false alarms; R-THROWS
  receiver-state variation; BND part (a) (numeric literals from the
  failing test into the screening + replay corpora).
- NOT yet built: BND part (b) (documented-limit inputs — 1e20/2^31/NaN),
  ACC1 (shadowed-check rescan), R-INH (inherited javadoc), R2 (anchor
  quota), R3 (doc-poor passing-test anchoring — the biggest remaining
  build, targets Closure-33/92), OBS (demoted — its target Math-53 turned
  out to be an @throws bug, not a field-read bug), P4.1 (Chart-3, gated on
  the offline false-flag study), J1/J2, CRASH remainder, T11, the offline
  P4.2/P4.4 tooling.
- KILLED: R4 menu (deleted), all rule pooling (deleted), soundness-harden
  (fixed but off, on a prove-it-or-delete clock), prompt-exhortation arms.

The next measurement batch validates the "awaiting measurement" group;
then BND(b)/R3 are the remaining recall builds before the full30 confirm
→ freeze → held-out sequence.

How this section is organised: the LIST below is ordered by what to
do first — most expected benefit for least risk — so it deliberately
jumps between stations. The DETAILED descriptions after the list are
in station order (2, 3, 4/5, 6, 7, then things outside the pipeline).
Every item has a short reference code in parentheses (H1, OBS, ACC1
…); the code carries no meaning by itself — it only exists so other
parts of the doc can point at the item. Items marked NEW come from the
2026-07-18 quality check.

Do in this order:

1. [Show the harness writer the WHOLE test](#show-the-harness-writer-the-whole-test-h1) — setup, helpers,
   constants, fixture files (H1); [show it the real failure message](#show-the-real-failure-message-h2)
   (H2); and [reject harnesses that rebuild the test wrong](#reject-harnesses-that-rebuild-the-test-wrong-h3) (H3).
   Stations 4/5.
2. [Let rules fix their own compile errors](#let-rules-fix-their-own-compile-errors-r1) (R1). Station 2.
3. NEW: [reuse the bug-triggering inputs on the patched program](#reuse-the-bug-triggering-inputs-on-the-patched-program-jd1-new)
   (JD1). Station 6. Cheap and broad.
4. [Measure the judge offline on archived decisions](#measure-the-judge-offline-j1) (J1) and
   [show the judge the failing test itself](#show-the-judge-the-failing-test-j3-new) (J3). Station 7.
5. [Six rules with a variety requirement](#six-rules-with-a-variety-requirement-r2) (R2);
   [name the input kind plus the fixed list of harmless variations](#name-the-input-kind-with-a-fixed-list-of-harmless-variations-r4)
   (R4); NEW: [show the inherited interface documentation](#show-the-inherited-interface-documentation-r-inh-new)
   (R-INH). Station 2.
6. Hand the harness writer ready-made raw material:
   [the list of readable state](#list-the-readable-state-h4) (H4),
   [the list of look-alike methods](#list-the-look-alike-methods-h5) (H5),
   [the list of known pre-existing crashes](#list-the-known-pre-existing-crashes-h6) (H6). Station 4.
7. NEW: [a second buggy-side scan with the always-firing checks switched off](#second-scan-for-shadowed-checks-acc1-new)
   (ACC1). Station 5.
8. [Generate the state-observer checks as code](#generate-the-state-observer-checks-as-code-obs) (OBS).
   Station 4; targets Math-53.
9. [Doc-poor mode: build rules from the project's own passing tests](#doc-poor-mode-build-rules-from-passing-tests-r3)
   (R3). Station 2; targets Closure-33 and Closure-92.
10. [Better inputs for screening and replay: the test's numbers plus the documented limits](#better-inputs-the-tests-numbers-and-the-documented-limits-bnd)
    (BND). Stations 3/6; targets Math-57.
11. [One aimed extra harness attempt when a whole kind of check is missing](#one-aimed-extra-attempt-when-a-check-kind-is-missing-retry)
    (RETRY). Station 4.
12. [Compare the patch's behaviour to the buggy program's](#compare-the-patch-to-the-buggy-program-p41)
    ("your change did nothing, yet the test passes") (P4.1). New
    stage; targets Chart-3; only after an offline false-alarm
    measurement.
13. [Rules aimed at the crash's own inputs](#rules-aimed-at-the-crashs-own-inputs-crash), for bugs whose
    only symptom is a crash (CRASH). Stations 2/6; targets Lang-27;
    uncertain payoff.
14. [Let a failing-test value bypass the judge](#let-a-failing-test-value-bypass-the-judge-j2) (J2). Station 7;
    parked until J1 and H3 exist.
15. [The one-hour initialization-order experiment for the thread bug](#the-one-hour-experiment-for-the-thread-bug-t11)
    (T11).
16. Offline tooling: [the certifier probe split](#offline-split-the-certifier-probe-machinery-p42) (P4.2) and
    [labeling the 205 unlabeled patches](#offline-label-the-205-unlabeled-patch-files-p44) (P4.4). Never mixed
    into a measured run.
17. A full 30-leg confirm of everything accumulated → then
    [THE HELD-OUT RUN](#the-final-held-out-run-final) (spent once; the number that counts).

### Station 2 — Rule-writing

*Quality check (2026-07-18). The question asked: are the rules bad,
too few, or starved of information?* The answer: the rule-writer is
only as good as the documentation we feed it — the model itself is not
the weak point. Where the code is well documented (the math and text
libraries), the rules it writes are good enough that running them
directly against the patched program produced 5 of our 8 catches in
the last full run. Where the code is barely documented (the Closure
compiler's internals), the same model produced ZERO usable rules in 4
out of 5 attempts — there was simply no written contract to build
rules from. Given that: yes, we are also short on QUANTITY for
well-documented code (the same class produced completely different
rule sets in different runs, proving more good rules exist than the
four slots we ask for — item R2), and we are short on two pieces of
INFORMATION: the documentation of the interface a method implements
(where Java convention actually puts the promises — item R-INH), and a
plain statement of what KIND of data the code consumes (item R4). One
warning for all information additions: do not dump more of the
codebase into the prompt wholesale. We have measured twice that piling
material into a prompt makes the model perform worse, not better. Every
addition must be a specific, mechanically-chosen piece (the parent
interface's documentation, the input kind, the list of look-alike
methods) — never "here is more of the repository".

Grounding data (full30, 28 synthesizing legs): 4 candidates per leg,
~1.7 survive screening; 25 of ~112 candidates never compile — and
rules have NO repair round today, a compile error is death; 76 of 78
survivors never fire on random buggy-side inputs (the trigger corpus
and the patched-side replay are where rules work; random fuzzing only
validates them); rules-through-replay contributed to 5 of the 8 full30
catches. Doc-rich legs (Math/Lang/Time) keep 2–5 rules; Closure
synthesis rounds ended with ZERO survivors in 4 of 5 cases.

#### Let rules fix their own compile errors (R1)

Pure recovery; do first. On a compile
failure, feed the compiler's error plus the candidate back ONCE for a
corrected version — exactly what the harness repair loop already does.
Recovers up to ~22% of all candidates at one cheap call each.
Validate: compile-death rate halves on a 4-leg micro-suite, survivor
quality unchanged (screening still judges them). Risk: none.

#### Six rules with a variety requirement (R2)

Each rule
declares its anchor: (a) the changed method's documented
contract/formula, (b) methods the failing test reads, (c)
sibling-agreement between overloads, (d) domain-level transformation
(see R4). If all candidates share one tag, one retry asks for the
missing kinds; the tag rides into the screen log so we can MEASURE
which anchors actually convict. Why a quota beats raw count: Math-2's
runs produced disjoint 4-rule sets run to run — the contract holds
8–10 distinct rules, but four slots aimed at one anchor never span
them. Validate: doc-rich micro-pair (Math-53): anchor spread rises,
no new false alarm. Risk: low.

#### Name the input kind, with a fixed list of harmless variations (R4)

Feeds R2 and R3. Two parts:

(a) *Input-kind as context, stated at the TOP of rule-writing.* Today
the rule-writer sees code, docs and tests but nothing NAMES what kind
of data the code consumes. HOW THE KIND IS DETECTED (honest version —
a signature gives the type, not the meaning): a three-tier hybrid.
Tier 1, purely mechanical: type-shaped kinds read straight off
signatures — numeric parameters = number, Collection/List/array =
collection, a documented format*/parse* method pair = encode/decode
pair. Tier 2, one CONSTRAINED classification call for the ambiguous
cases (a String input could be program text, a query, or a person's
name — no grep settles that): a dedicated model call with its own
small fixed prompt — input: the entry-point signatures, class/package
names, the first javadoc lines, and the failing test's call shape;
required output: a SET of labels from the closed vocabulary (usually
one, possibly several — a date formatter consumes numbers AND is a
format/parse pair; empty set = unknown), plus one justification line
per label for the log. The set stays auditable exactly like a single
label — what matters is the closed vocabulary, not the count. Temperature 0;
cached per BUG (both legs share the same buggy entry points, so the
second leg reuses the label free); cost ~2k tokens against a leg's
~50-100k. The labels are the ONLY thing that flows onward — they select
fixed TEMPLATE sentences plus the matching menu entries, taken as the
UNION over the detected kinds, deduplicated, prioritized by the kinds
of the touched method's own parameters, and CAPPED at three entries
(the measured injected-mass lesson applies to menu text like anything
else), so
the classifier has no channel to smuggle free-form advice; its worst
failure is injecting the wrong FIXED text, which the entry's own
APPLIES-ONLY-IF condition, screening, and the judge each defuse. This is a model
used as a narrow detector (like the judge), not as an advice-follower
— the forbidden thing remains putting the whole menu in the
rule-writing prompt and letting the model pick mid-generation. Tier 3,
the fail-safe: "unknown" injects nothing. Layered defense even against
a wrong label: every entry carries its own APPLIES-ONLY-IF condition
the rule-writer must verify against the docs, and the resulting rule
still has to survive screening and the judge. The detected kind is
then stated first in the context: "the public entry points consume
JavaScript source text."

(b) *The closed menu:* a short FIXED list, one entry per broad input
kind — not generated per bug, not composed per task. WHEN AND HOW IT
IS ADDED: the pipeline's input-kind detection from part (a) picks the
ONE entry matching the detected kind and injects it into the
rule-writing instructions when that prompt is built (same for the
harness writer). The model never sees the whole menu and never chooses
a category itself — that would be advice plus a judgment call, the
exact pattern this design avoids. If the detector matches no entry,
nothing is injected (the fail-safe below). "Closed" means the model
may only USE the injected variation, never invent its own: a
freely-invented "harmless" change that is not actually harmless is
exactly how unsound rules are born.
WHERE THE ENTRIES COME FROM — and an admitted bias to correct: the
five starting entries were written by looking at what our tasks
consume, so their SELECTION is benchmark-flavoured even though each
entry is a universal fact (this does not distort dev-vs-held-out
comparisons — held-out is the same five projects — but it narrows
usefulness beyond this benchmark). The fix before the menu is
finalized: populate it from an INDEPENDENT source — the metamorphic-
testing literature's published catalogs of standard relation patterns
— imported wholesale, so our tasks merely activate a subset rather
than dictate the list.
Every entry has three mandatory fields — the variation, the CONDITION
under which it applies (checkable from the task's own docs/grammar,
never assumed), and its KNOWN EXCEPTIONS. The starting entries, with
their conditions and exceptions spelled out:

- *Program/markup text* — inserting spaces or comments changes no
  meaning, CONDITION: the input language's grammar defines whitespace
  and comments as insignificant (true for Java/JS/C-family; NOT true
  for Python, YAML, Markdown — check, don't assume). KNOWN EXCEPTION,
  demonstrated inside our own dev set: inserting a NEWLINE changes
  line numbers, and outputs that report source positions (error
  formatters — Closure-62's output literally contains "line 6")
  legitimately change with them. So: insert only same-line spaces or
  same-line comments when the asserted property could reference
  positions, or assert only position-independent properties (counts,
  kinds, semantic content of the output).
- *Parse/print pairs* — print then re-parse must preserve the input,
  CONDITION: at the level the documentation promises. Printers
  legitimately normalize; assert semantic equivalence or the
  documented normal form, never byte-for-byte identity unless the
  docs promise that.
- *Numbers* — only invariances the docs state (scaling, translation,
  symmetry); no invented algebra.
- *Collections* — order must not matter, CONDITION: only where the
  docs say order does not matter.
- *Formatters* — the output must parse back to the input, CONDITION: a
  parser for the format exists in the library and the docs claim
  compatibility.

*DONE 2026-07-18: the menu is BUILT — not a five-item sketch but a
full literature mine. Five parallel research passes (numerical, string,
collections, datetime, program-text, web-API, security) produced 62
relation families, consolidated into `src/java/variation_menu.json`
(the operational menu) with the cited provenance in
`suites/menu-candidates.md`. 38 are universal (hold for any correct
implementation), 24 documented-property. Every entry carries a
checkable soundness CONDITION and an EXCEPTIONS list (the false-alarm
suppressors — e.g. Java split's trailing-empty drop, Turkish-i case
folding, DST gap/overlap, EMI's undefined-behavior void, line-number
shifts under inserted newlines). Coverage per detected kind: number 19,
plain_text 15, collection 15, query_or_filter 10, datetime 8, web_api
8, security 6, program_text 5, encode_decode_pair 4. The web-API and
security families are REAL (SMRL's IDOR / injection / session /
workflow / CSRF relations with their guard preconditions), not the
deleted placeholder. The `variation_menu.py` loader injects per detected
kind, universal-before-documented, priority-ranked, capped at 3. Key
source anchors per domain:*
- Segura et al., "A Survey on Metamorphic Testing" (IEEE TSE 2016) —
  the field survey; its corpus of published relations across domains
  is the primary import source.
  https://eprints.whiterose.ac.uk/id/eprint/110335/1/segura16-tse.pdf
- Chen et al., "Metamorphic Testing: A Review of Challenges and
  Opportunities" (ACM Computing Surveys 2018).
  https://dl.acm.org/doi/10.1145/3143561
- Segura et al., "Metamorphic Relation Patterns for Query-Based
  Systems" (MET 2019) — seven abstract relation patterns plus six
  output patterns (equivalence, equality, subset, disjoint, complete,
  difference).
  https://personales.us.es/sergiosegura/files/papers/segura19-met.pdf
- "Metamorphic Relation Generation: State of the Art and Research
  Directions" (ACM TOSEM 2025) — recent overview incl. pattern
  hierarchies that organize prior catalogs into one structure.
  https://arxiv.org/pdf/2406.05397
- Ying et al., "Metamorphic Relation Patterns for Metamorphic Testing,
  Exploration and Robustness" (STVR 2025) — symmetry-based patterns as
  reusable abstractions. https://onlinelibrary.wiley.com/doi/10.1002/stvr.70003
- For the PROGRAM-TEXT entry specifically: Le, Afshari & Su, "Compiler
  Validation via Equivalence Modulo Inputs" (PLDI 2014) — the
  principled generalization of our whitespace/comment idea: variants
  that mutate only code paths a given input never executes must not
  change that input's output (the Orion/Athena/Hermes family found
  140+ GCC/LLVM bugs this way).
  https://www.vuminhle.com/pdf/pldi14-emi.pdf
- For rule SHAPES beyond metamorphic: Hughes, "How to Specify It!" —
  five property families (invariants, postconditions, metamorphic,
  inductive, model-based) with round-trip guidance; measured: the
  metamorphic and model-based families catch the most.
  https://research.chalmers.se/publication/517894/file/517894_Fulltext.pdf
- For the NUMBERS/COLLECTIONS entries: Murphy et al.'s six classes for
  numeric and collection data (additive, multiplicative, permutative,
  invertive, inclusive, exclusive), used and validated in Kanewala &
  Bieman's scientific-software work.
  https://onlinelibrary.wiley.com/doi/10.1002/stvr.1594 and the
  fault-detection effectiveness study
  https://arxiv.org/pdf/1904.07348

(b-SAFETY, added 2026-07-18 per review) *Detection is ADVISORY for
relation selection ONLY — it never constrains fuzzing.* Detecting
`number` does not stop the fuzzer feeding strings; input generation is
untouched by the detected kind. The kind only picks which menu
relations are OFFERED to the rule-writer as candidates, and every
candidate is condition-checked, screened on the buggy build, and
judged. A String that holds a number (createNumber) is still fuzzed
with arbitrary strings. Worst case of a wrong kind: a less-relevant
candidate in one of the 3 slots — never a narrowed input space, never
a changed verdict.

(b-EXACT) *The precise mechanism — what decides which entries reach
which leg, deterministic parts vs the one LLM call. This is the answer
to "how do we choose".*

STEP 1 — detect the input kind(s). DETERMINISTIC, no model, over the
entry-point signatures we already extract (touched methods + the
failing test's called methods). For every parameter and return type:
- numeric type (int/long/double/float/BigInteger/BigDecimal and arrays
  of them) -> emit `number`.
- array, or a type whose simple name is in a fixed list
  {List, Set, Map, Collection, Iterable, ...} -> emit `collection`.
- type whose simple name is in a fixed date/time list {Date, Calendar,
  Instant, LocalDate, LocalDateTime, ZonedDateTime, Duration, Period,
  TimeZone, DateTime, DateTimeZone, ...} -> emit `datetime`.
- two entry methods whose names match format-side
  /(format|to|write|encode|serialize|print)/i and parse-side
  /(parse|from|read|decode|deserialize|valueOf)/i over the same type ->
  emit `encode_decode_pair`.
- a String parameter with none of the above resolving it -> mark
  STRING-AMBIGUOUS and go to step 2. (A String could be program text,
  a query, or a person's name -- the TYPE cannot tell them apart, which
  is the one place determinism genuinely cannot decide.)
Everything except the String case is settled here with zero model
calls.

STEP 2 -- resolve the String-ambiguous case only. ONE LLM call,
temperature 0, cached per (project,bug). Its fixed prompt contains:
the entry-point signatures (names + types), the class and package
name, the first ~500 chars of the class javadoc, and one or two
example calls from the failing test. Its instruction: "these methods
consume String input(s); reply with a JSON array of labels drawn ONLY
from [program_text, plain_text, query_or_filter], or [] if unsure, one
short reason each." The output is parsed and INTERSECTED with that
closed set -- anything else, or [], contributes no string-kind (the
fail-safe). The label(s) are the only thing that leaves this call: a
narrow classifier used like the judge, never an advice channel.

STEP 3 -- select the entries. DETERMINISTIC
(variation_menu.entries_for_kinds): union of menu entries whose
input_kinds intersect the detected kinds, deduplicated by id, ranked
(status `menu` before `menu_optional`, then each entry's `priority`
field so the strongest fit for the kind survives the cap), capped at 3
to respect the measured injected-mass limit. Unknown/empty kind -> no
entry (fail-safe).

STEP 4 -- render and inject. DETERMINISTIC: each selected entry becomes
its statement + its APPLIES-ONLY-IF condition + its DO-NOT-APPLY-TO
exceptions + the one example matching the detected kind (never the
statement alone). The detected kinds also produce a fixed TEMPLATE
sentence stated at the top of the context ("The public entry points
consume numeric values and date-time values") -- assembled from the
labels by a lookup table, NOT written by any model.

STEP 5 -- who checks each entry's CONDITION. The kind-match (steps 1-3)
is deterministic and coarse: it says "a monotonicity rule is POSSIBLE
for a number leg", not "this method has a monotone quantity". The
fine check -- does the entry's condition actually hold here? -- is done
by the RULE-WRITER model, which is handed the condition text and
instructed to verify it against the shown docs and SKIP the entry if
it does not hold. Backstops if the model gets it wrong: screening
drops a rule that fires indiscriminately on the buggy build, and the
judge drops an unsound one. So: coarse relevance = deterministic
(kind); fine relevance = LLM-with-two-mechanical-backstops (condition).
OPTIONAL future refinement (not built): a deterministic keyword
pre-filter on the touched javadoc (e.g. drop `documented-monotonicity`
unless the docs contain cumulative/sorted/non-decreasing/monotone)
before the cap, to spend the 3 slots better -- a pure precision aid,
never a soundness mechanism.

(c) *Safeguards that keep the menu general (the anti-overfitting
contract). When each applies: the first three are an EDIT-TIME
checklist — they gate every addition or change to the menu; the next
two are enforced BY THE PIPELINE at run time; the last is a one-time
checkpoint at the held-out boundary.*
- **Provenance rule (edit-time).** An entry may only be added with a written
  justification citing a universal definition — a language grammar, a
  mathematical identity, a documented API-contract category. "Because
  it would catch bug X" is never a justification; an entry whose only
  known use is one benchmark bug family does not belong.
- **Independent-derivability test (edit-time).** Every entry must be one that a
  competent test engineer with NO access to our benchmark would put on
  the same list (all the starting entries are standard metamorphic-
  testing practice). If an entry needs our bugs to be explained, it is
  benchmark-shaped — reject it.
- **Checkable condition (run-time, pipeline-enforced).** An entry applies only when its stated
  condition is verifiable from the task's own artifacts (the language,
  the docs, the signature) at the time of use. No condition, no use.
- **Size discipline (edit-time), stated precisely.** What must stay
  small is the number of distinct PATTERNS and, above all, the rule
  that growth NEVER comes from our misses — a menu that grows an entry
  per newly-missed bug is the overfitting smell in its purest form.
  What MAY grow, from the literature only: the input KINDS each
  pattern covers and worked examples across contexts (the 2026-07-18
  expansion added idempotence and identity-element — both standard
  QuickCheck-tradition properties, one of them literally this doc's
  own trust-source-#4 example — plus datetime/plain-text kinds as new
  coverage of EXISTING patterns). Prompt mass is controlled at
  injection (at most 3 entries for the detected kinds), not by
  starving the menu.
- **Freeze before held-out (one-time checkpoint).** The menu is frozen before the held-out
  run and may not be edited based on anything seen there — same
  hygiene as prompts and thresholds (measurement rule 4).
- **Fail-safe (run-time, pipeline-enforced).** If a task's input kind is not on the list, R4
  contributes nothing for that task — no rule rather than a wrong
  rule.

The list sits on the same trust tier as "sorting twice = sorting once"
(source #4 in the ranking above). Risk after these safeguards: low.

#### Show the inherited interface documentation (R-INH, NEW)

When a touched method implements or overrides an interface/superclass
method, the contract usually lives on the PARENT's javadoc — classic
Java style documents the interface, not each implementation — and
today only the touched class's own docs are shown. Consequence: some
legs we treat as "doc-poor" may be doc-rich one level up, and rules
that ARE justified ("contains(char) answers membership over the
logical content" may be specified on the parent type) currently cannot
cite their justification. Fix, mechanical: when a touched method has
an @Override or matches a signature in an implemented interface /
extended class, fetch THAT declaration's javadoc and show it beside
the method, labeled "inherited contract". Applies to stations 2 AND 4
(the harness writer has the same blind spot). Risk: low — it is
selected context, not bulk context; keep it to the direct parents.
Validate: count rules citing inherited contracts on a Closure
micro-pair; watch the false-alarm guards.

#### Doc-poor mode: build rules from passing tests (R3)

Medium risk; decisive targets Closure-33 and Closure-92. Mechanically measure how much documentation the touched methods have
(javadoc characters per touched method — already extracted, so
measuring is free). BELOW a threshold — the Closure situation, where
4 of 5 synthesis rounds produced zero surviving rules — rule-writing
pivots its primary anchor from "the documented contract" (which barely
exists there) to the project's OWN PASSING TESTS near the changed code
(already mined and shown as usage examples today — wasted as a spec):
each rule takes one passing test's scenario and asserts that the
property the test checks stays true under a harmless variation of the
input, where "harmless" may come ONLY from the R4 menu. This is
exactly the shape of Closure-33's historical winner (append a comment
to testIssue700's program — the compiler's warning count must not
change), which today gets invented in about half the runs by pure
luck.
*Is this general, or benchmark-shaped? The argument, made before
building so held-out can falsify it:* (i) it uses only artifacts every
real task has — a project's own test suite — no bug shapes, nothing
dataset-specific; (ii) the trust logic is structural, not learned: a
passing test holds on the buggy build AND on any correct patch by
definition, so a rule built on it can only convict a patch that breaks
previously-working behaviour beyond the reported bug — a general
failure mode of automated patching; (iii) it is the same trust move
the pipeline already makes with the FAILING test ("tests are
specification"), extended from one test to the suite; (iv) "vary a
tested scenario harmlessly, re-check its property" is standard
metamorphic testing, not something invented for this benchmark. What
it does NOT cover, said plainly: overfits whose damage lies far from
every existing test's neighbourhood — for those, the contract rules
(doc-rich path) remain the only net. Honest caveat: the design is
general but the supporting evidence so far is one benchmark leg; the
held-out run is where the generality claim gets tested, and nothing in
the mechanism may be tuned per-bug on the way there. Validate:
Closure-33-o (its winner becomes derivable-by-recipe instead of a
lucky roll); Closure-92-o second target; Closure-62-c as the
false-alarm guard.

### Station 3 — Rule screening

*Quality check (2026-07-18). The question asked: is 20,000 random
tries the right amount, and are they the right tries?* The amount is
fine; the TRIES are the weak point. Of the 78 rules that survived
screening in the last full run, 76 never fired even once during their
20,000 random tries — random inputs almost never wander into the
narrow situations where a rule would object. So the number screening
ranks rules by ("how often did it fire?") is usually zero-vs-zero and
says little. The measurements that DO carry information are the runs
on the failing test's own inputs. And there we found a hole: we
collect those inputs by pulling the QUOTED TEXT out of the failing
test — but for the Math-2 bug the important inputs are NUMBERS
(a population of 43,130,568 and so on), and numbers are currently not
collected at all, so rules about numeric behaviour get tested without
the one set of inputs known to matter. Fix folded into item BND. More
random tries would not help any of this — wrong-tries is not fixed by
more-tries.

#### Better inputs: the test's numbers and the documented limits (BND)

Targets Math-57; fixes the station-3 input gap. Two parts:
(a) extract NUMERIC literals from the failing test alongside the
quoted strings, so the direction check and the replay's deterministic
tier can steer toward the trigger values (honest note: fuzzer inputs
are bytes, so seeded values steer generation rather than guarantee
exact replay — still far better than blind); (b) mechanically add
inputs at the DOCUMENTED limits — range endpoints from @param, values
around 2^31, 1e20, NaN where the contract admits it — so
width/overflow rules get exercised deterministically instead of hoping
random bytes reach 1e20 (they essentially never do: Math-57 is a
float-vs-double difference visible only near 1e20 and has missed in
every run). Risk: medium — extremes are exactly where CORRECT
implementations degrade (three Math-2-c false accusations lived
there), so every boundary firing carries a "boundary input" note to
the judge and never convicts alone at extreme magnitudes. Interplay
with the station-4 fence: harness fuzzing stays capped; replay rules
whose stated contract covers extremes may use them.

#### Rules aimed at the crash's own inputs (CRASH)

Targets Lang-27; uncertain payoff — try after everything above. The problem in plain words:
for this bug the only trusted signal is "this input makes the program
crash". The overfit wraps the code so it never crashes — anywhere.
Nothing fires. And "did it return the RIGHT value instead?" has no
trusted answer, because the buggy program never returned any value
there — it always crashed. Two ideas: (a) when a patch removes a
crash, the method now RETURNS something on the crash-input family —
and the laziest patches return nonsense. Rules do not need the one
right answer to condemn nonsense: a round-trip rule ("format it,
parse it back, you must get the same thing") or a documented-contract
rule works without it. Mechanically seed the screening/replay corpora
with the captured crash inputs and their mutations so the rules get
evaluated exactly where the nonsense lives. (b) Where the javadoc
documents "@throws for malformed input", a patch that silently
returns something on literally-named malformed shapes is convictable.
Honest limit: a patch that removes the crash AND returns
contract-respecting values everywhere is close to a real fix and may
belong in `suites/labels/incorrect_labels.md` rather than on this backlog. Risk:
medium-high — contracts are often silent near crash boundaries (that
is why the code crashed there in the first place).

### Stations 4/5 — Harness writing & acceptance

*Quality check (2026-07-18). The question asked: is the problem that
we write too FEW harnesses, or that the harnesses are not GOOD enough
— and if quality, are we giving the writer the right information?*

For station 4 the answer is clear: quality, not quantity. We measured
both directions. Writing the same harnesses again in a new run does
sometimes catch a bug the previous run missed (that is how Closure-33
and Lang-41 came back) — but every additional harness is also one more
chance for a wrong check to accuse a correct patch, and our
zero-false-alarm result was measured with exactly three harnesses per
leg. So simply writing more harnesses buys a little and risks a lot.
The quality problems are two, both measured: (a) the harness rebuilds
the failing test's situation slightly wrong, and that DIFFERENCE — not
the patch — makes checks go off (fixed by giving the writer the
missing information, items H1/H2, plus an automatic comparison, item
H3); (b) which checks the writer invents varies run to run (helped by
handing it ready-made raw material, items H4/H5/H6, and by generating
the routine checks as code, item OBS). One more missing piece of
information, shared with station 2: in Java the promised behaviour of
a method is often written on the INTERFACE the class implements, not
on the class itself — and the writer is never shown the interface's
documentation (item R-INH).

For station 5 the answer is: it works, with one real measurement gap.
When we test which checks fire on the buggy program, the test run
stops at the FIRST check that fires for each input. So a check that
sits behind an always-firing check never gets a turn, and we record it
as "never fired on buggy" — which really means "never got to run", not
"stays quiet". This one gap caused mistakes in both directions: a
GOOD check looked unexercised and an earlier version of our code threw
its accusation away (that cost us the Lang-60 catch once), and a BAD
check looked unexercised and the judge believed its first-ever firing
(that caused the Lang-7 false alarm once). Item ACC1 closes the gap by
re-running the test with the always-firing checks switched off, so the
shadowed checks actually get their turn on the buggy program.

#### Show the harness writer the whole test (H1)

The highest-value single fix; do first. The problem, found by direct inspection of the prompts: the
harness writer is shown ONLY the failing test's method body.
Closure-62's test method calls `formatter("assert (1;")` — a helper
defined elsewhere in the test class that performs exactly the setup
harnesses keep rebuilding wrong (it wires the source-text provider) —
and uses `FOO_TYPE`, a class constant. Neither appears anywhere in the
prompt; the model must improvise the setup, and every setup-divergence
failure follows from that. Observed cost so far: the two p23gate false
alarms (Closure-62-c, Closure-73-c — lifted checks firing over missing
source wiring / a trailing semicolon), two of the three Closure-62-o
drops in full30/recov2 (judged setup-divergent — correctly!), and
Chart-26's improvised entity wiring across at least 3 runs. The fix is
mechanical: resolve the identifiers the test method uses against its
test class and include what they refer to — setUp()/@Before methods,
helper methods, class constants — plus the content of any fixture
FILE the test references by a path-like string literal. Validate: the
Closure-62 pair (the overfit side finally gets a faithfully-built
scenario; the correct side stays clean) and Chart-26-c as guard. Risk:
low — showing the true setup can only reduce improvisation; watch
prompt size on big test classes.

#### Show the real failure message (H2)

Do together with H1. Station 1
already runs the failing test on the buggy build; its JUnit message
("expected:<X> but was:<Y>") names the exact observable that diverges
AND the wrong value the bug produces — and today we throw it away.
Put it in the harness prompt. Cost ~zero (the run already happens).
Risk: none identified.

#### Reject harnesses that rebuild the test wrong (H3)

Needs H2. At acceptance: a
test-copy check firing on the buggy build must observe the SAME wrong
value the real test observed there. A different observed value means
the harness's scenario is NOT the test's scenario — reject with
exactly that message into the existing repair loop. This converts "is
this firing just setup divergence?" — today a judgment call made at
station 7, fallibly — into a station-5 string comparison. Validate:
the archived Closure-62-c false-alarm harness must be auto-rejected.
Risk: low; normalize values that legitimately vary (whitespace — the
same normalization station 4 now mandates).

#### List the readable state (H4)

The raw material
of hidden-state checks — the kind that convicts Lang-60 ("capacity
went from 43 to 6 after a call documented as read-only") — is buried
in a truncated class skeleton today. Mechanically list the public
no-argument getters: "state you can read: capacity(), length(),
size()". Risk: none. Feeds OBS.

#### List the look-alike methods (H5)

Mechanically list same-name overloads
and doc-identical method pairs of the touched class:
"getPackageName(Class) and getPackageName(String) are documented to
agree". Sibling-agreement checks convict Lang-41 — and in 2 of 7 runs
the model did not notice the pair on its own and the leg missed.
Risk: none.

#### List the known pre-existing crashes (H6)

Acceptance and the latent scan already OBSERVE the generic
crashes that live in the buggy build — e.g. the text-measuring crash
behind every Chart-26 flag-pattern false alarm (observed in at least 4
runs across the project's history). Collect their identities and state
them in the harness prompt: "these exceptions exist on the buggy build
and are NOT the bug — never convert them into an alarm"; give the
same list to the judge. Kills that class at the source instead of at
judgment. Risk: low — for crashing bugs, exclude the bug's OWN crash
from the list.

#### Generate the state-observer checks as code (OBS)

The P3.1 check-shapes delivered as code, not advice; targets Math-53. Math-53's divergence (3 outputs)
is invisible to `equals()` because commons-math defines all-NaN
complex numbers as equal; only field-level reads (`getReal()` is NaN
vs 4.0) can see it — which is why every run's NaN rules screened
"silent" and the leg missed every time. Rather than asking the model
to please read fields (advice — the same lossy channel that muted
Phase 2), generate the observer block as CODE in the harness template:
call every public no-argument getter before/after each API call,
compare per-field with NaN-bitwise and tolerance semantics. Risk:
medium — an observer with side effects would perturb the scenario
(restrict to getters whose docs read as pure, and say so in the
limitation notes).

#### One aimed extra attempt when a check kind is missing (RETRY)

Do NOT raise the blanket harness count. After the harnesses are written, mechanically
list which check KINDS are present (test-copies / rules /
sibling-agreement / hidden-state — readable from check IDs and
shapes); if an applicable kind is missing, spend ONE extra attempt
asking for exactly the missing kind. The evidence cuts both ways on
"more harnesses": rerolls DO flip flaky legs (minfix_w2's rerolls
recovered Closure-33 and Lang-41 after w1 missed them), but every
extra harness is also a false-alarm lottery ticket on the correct
sibling — each false-alarm class this cycle arrived via ONE harness in
ONE roll — and zero-false-alarms was measured at n=3. Risk:
low-medium (kind detection is heuristic; keep it advisory).

#### Second scan for shadowed checks (ACC1, NEW)

Fix the station-5 blind spot at its source: after the normal scan,
take each harness's LATENT checks and rerun the buggy-side scan with
the checks that fired DISABLED (mechanically commented out by their
recorded IDs), so the shadowed checks actually execute on the buggy
build. A latent check then becomes either "fires on buggy when
reached" (the Lang-60 capacity case — its later patched-side firing is
the classic overfit signature, symmetric evidence in hand) or "quiet
on buggy even when reached" (the Lang-7 hex case — a first-ever firing
on patched now carries REAL buggy-side evidence against it, not
absence of evidence). Cost: one extra compile plus a short fuzz per
harness that has latents — minutes of VM time, zero model calls.
Risk: low — it only upgrades "unmeasured" to "measured"; the disabled
variant never influences acceptance itself. Validate: rerun the
archived Lang-60-o and Lang-7-c fixtures — the capacity check must
measure as fires-on-buggy, the hex check as quiet-on-buggy.

### Station 6 — Judgment day

*Quality check (2026-07-18), continued.* Station 6 (running everything against the patched
program): the newest part — running rules directly — is the healthiest
piece of the whole pipeline. The older part — running the harnesses —
wastes evidence we already hold: fuzzing on the patched program starts
from scratch, even though station 5 saved the exact inputs that made
checks fire on the buggy program, and those are precisely the inputs
most worth trying first on the patched one (item JD1).

#### Reuse the bug-triggering inputs on the patched program (JD1, NEW)

The inputs that actually fired checks
on the buggy build (their crash artifacts are already saved at
acceptance) are exactly the inputs most likely to still fire on an
overfit that special-cased only the reported input — and today the
patched-side fuzz rediscovers them by luck or not at all. Fix: pass
the buggy-side artifact files as the seed corpus of the patched-side
fuzz run. Within-run, firewall-clean, ~zero cost (the files exist; the
fuzzer accepts a corpus directory). Risk: low; one care point — a seed
that fires via a pre-existing generic crash will fire immediately on
patched too, so the existing attribution/differential-replay guards
must stay in the path (they do). Validate: on the archived flaky legs
(Closure-33-o, Lang-41-o), the catch rate across repeats should rise;
correct-side guards stay clean.

### Station 7 — The judge

*Quality check (2026-07-18). The question asked: how often does the
judge get it wrong, and does it have what it needs?* The pattern from
this whole cycle: whenever we HAND the judge a hard, machine-computed
fact about the accusation ("this same check also fired on the buggy
program", "the real test passes on this patched program", "this
exception is not one of the harness's own checks"), it decides
correctly almost every time — and every class of wrong decision we
found was fixed by adding exactly one such fact. When no fact applies,
the verdict rests on one model opinion, and that is where the
remaining wrong decisions live. Two things follow. First, a cheap
information gap: the judge is told the VALUES the failing test expects
but is never shown the failing test itself — seeing the actual test
line would have prevented at least one wrong dismissal (item J3).
Second, we honestly do not know whether asking the judge several times
and taking a majority would help, because that was only ever measured
BEFORE the facts existed — item J1 measures it properly, offline, on
archived decisions where we know the right answer.*

#### Show the judge the failing test (J3, NEW)

In the Closure-62 backwards judgment the judge weighed the
buggy guard code against a bare literal; the test's own source
(assertEquals with the caret string, on an error placed at
end-of-line) would have made the trust hierarchy concrete instead of
abstract. Fix: include the trigger test's source in the judge's
context next to the trusted values. Cost ~zero (the source is already
extracted for stations 2 and 4). Risk: low — it is trust-source #1;
the one care point is prompt length on multi-test bugs (include the
test the fired check lifts, not all of them).

#### Measure the judge offline (J1)

Zero pipeline risk, highest information per effort; do early. This cycle's
forensics named, for dozens of archived keep/drop decisions, what the
RIGHT decision was — runs-archive holds them all. Replay those
decisions through the existing `verifier_replay` tool under different
configurations — 1 vote vs 3 diverse lenses, with and without each
computed fact — and MEASURE keep-error and drop-error rates. The old
"majority voting doesn't help" result predates the computed facts and
deserves re-measurement WITH them. Whatever measurably wins becomes
the configuration.

#### Let a failing-test value bypass the judge (J2)

Parked behind J1 and H3. Where an accusation's expected value is literally one the failing test
asserts, bypass the judge — the test outranks it. Tempting, but the
Closure-62-c false alarms were EXACTLY such values fired from a
badly-rebuilt scenario; with H3's fidelity gate in place this becomes
safer. Enable only if J1's measurements show the judge is the weak
link on precisely these.

### New stage / offline / last

#### Compare the patch to the buggy program (P4.1)

Targets Chart-3; impactful but with unmeasured false-alarm risk — measure offline first. Chart-3's overfit passes the faithful test scenario by construction
and its generalized checks stay latent; it has missed 4 consecutive
runs and its baseline "catch" was a loose reconstruction that
happened to fire. Its signature is exactly P4.1's: "the edited region
behaves identically to the buggy build everywhere except on the
trigger inputs — your change did nothing, yet the test passes."
Computable with the buggy build only, so firewall-clean. BUT a
correct refactor can also be behaviour-preserving, so before this may
influence any verdict: run it offline over the verified-correct
patches and measure the false-flag rate. Ship only with a measured
rate near zero, and initially as an ESCALATION trigger (spend the
aimed retry on flagged legs) rather than a verdict.

#### The one-hour experiment for the thread bug (T11)

The untrustworthy part of thread bugs is TIMING; if this
bug is really about initialization ORDER (which class got set up
first), order can be forced deterministically: run twice in separate
processes with a different forced first-touch order and compare.
Unvetted; may not match the defect's actual shape; explicitly not in
the plan until someone spends the hour.

#### Offline: split the certifier probe machinery (P4.2)

The model only
constructs interesting objects and call sequences; a fixed piece of
our code enumerates and prints every public observable before/after
each step. Known validation: the five wrongly-cleared patches
(Chart-7, Lang-41, Lang-60, Closure-62, Math-57) must flip to
"difference found" with NO prompt changes. Offline tooling — never
mixed into a measured run.

#### Offline: label the 205 unlabeled patch files (P4.4)

Dataset growth, not the pipeline: one file per (bug, tool) first; "difference
found" verdicts trustworthy directly; "no difference" only after the
deep-dive protocol; manual spot-check before anything enters the
pinned set.

#### The final held-out run (FINAL)

After the above stabilize and one more
full30 confirms the accumulated changes: run the 71 held-out legs
ONCE, flagship model. Targets fixed in advance (no goalpost-moving):
at least 70% of the 28 held-out overfits caught, at most 1 false
alarm on the 43 held-out correct legs. Why the zero-false-alarm work
matters more than it looks: the baseline's 1-in-14 false-alarm rate
would project to ~3 held-out false alarms against a budget of 1; the
current measured rate is 0-in-13.

---

## DIRECTION-CHANGING FINDING (2026-07-18): the menu covers only ~18% of
## what the model freely invents — free exploration is the stronger engine

Two experiments settled how much rule-writing should SUGGEST relations
from the R4 menu vs let the model INVENT them from the code
(study/rank_eval.py, study/coverage_eval.py; 25 diverse methods).

**Ranking test.** Deterministic keyword ranking of menu entries agreed
with a nano ranking only 1.26/3 and fell back to bad static defaults
(trig injected for KMeans / Base64 / MessageDigest) in ~7/19 cases.
Fixes applied (demote narrow number relations out of the default; add
Complex/matrix types; add a nano content-aware selector with keyword
fallback). But the next test undercut the premise.

**Coverage test — the decisive one.** For 11 methods the flagship model
FREELY invented metamorphic relations from the code alone (signature +
javadoc + class), no menu shown; each was then mapped to a menu family
or marked NOVEL. Result: **88 relations invented, 16 covered by the
84-entry menu (18%), 72 NOVEL (82%)** — and the novel ones are BETTER:
specific to the exact method's contract where the menu is generic.
- Math-2: the model invented complement-symmetry (swapping
  successes/failures complements the mean), all-successes, full-draw,
  sample-linearity, population-scaling — the menu had only generic
  "distribution-invariants". Complement-symmetry is the very shape that
  convicts the -49.76 mean.
- Lang-7: invented hex-case-invariance (literally the meta-hex-case
  relation from the real runs), plus-sign, leading-zeros. Menu 0/8.
- Closure-62: caret-column-correctness, no-excerpt-without-source-line
  — the actual Closure-62 bug relations. Menu 0/8.
- Lang-60: capacity-irrelevance (the read-only/capacity convictor),
  empty-builder-false, indexOf-equivalence. Menu 2/8.

**What it means (and it reconciles p23gate).** Free invention given the
code is the stronger engine by ~5x and yields the more discriminating
relations. That is exactly WHY injecting menu/pool rule-mass regressed
p23gate — it displaced the model's own better free-form checks. So:
- Relation synthesis stays PRIMARY and free; the model explores the
  contract itself — that is where convicting relations come from.
- The menu is NOT a relation source to inject wholesale. Two defensible
  roles remain: (a) a small CATEGORY-CHECKLIST backstop — only when the
  free output omits an applicable CATEGORY the model measurably forgets
  (hidden-state/read-only, sibling-agreement), spend ONE targeted nudge
  (the RETRY item), never a bulk list; (b) DOMAIN REFERENCE for kinds the
  model may not know well (security SMRL, reflection JLS traps, geometry
  degenerate-shape traps) — at most 1-2, only for the matching kind.
- R4 is therefore DEMOTED from "inject relevant relations" to "category
  checklist + domain reference". Keep the artifact (cheap; the soundness
  conditions/exceptions are genuinely useful reference), but do NOT build
  the pipeline around injecting it. Re-scope R4 to: (1) mechanically
  detect whether free synthesis already emitted each applicable category
  (from oracle shapes); (2) one targeted retry for a missing category;
  (3) optionally show 1 domain relation for a security/reflection/
  geometry leg. The input-kind detector + menu stay as the mechanism for
  (3) only.
- Caveat: the mapper was strict (marked capacity-irrelevance NOVEL though
  it matches read-only), so true coverage is perhaps ~25%. Still low; the
  conclusion stands. And the bigger reassurance for the whole project:
  the existing FREE synthesis is doing the real work (rules-through-replay
  convicted 5/8 in full30), consistent with this finding.

## CORRECTION (2026-07-19): the "relations aren't a detection
## contributor" finding is RETRACTED — it was measured with replay off

The 2026-07-19 commit concluded the rule pipeline contributes nothing
to detection and demoted it to opt-in. That conclusion does not
survive a check against our own archives. Three faults, in order of
severity:

1. **Both ablation arms ran with the replay stage switched off.** Every
   cases file written since the evening of 2026-07-18 (`onefull`,
   `math53_full`, `abl_norel`, `abl_withrel`) was missing
   `--replay_relations_on_patched` — the exact launch-check failure
   measurement rule 8 warns about. So the ablation compared "rules
   injected into the harness prompt" against "no rules" with the one
   mechanism that makes rules matter disabled in BOTH arms. It ablated
   the channel we already knew was weak (p23gate: injection displaces
   the model's own checks) and attributed the null result to the whole
   pipeline.
2. **The 5-bug sample was blind to the question.** Chart-3, Lang-41,
   Lang-60, Math-53, Math-57 — of these, only Lang-41 ever had a
   relation contribute a catch (trigger-tier, redundant with the lifted
   test). All five legs where replay convictions were kept in full30
   (Chart-7, Chart-26, Lang-41, Math-2, Time-4) were absent except
   Lang-41.
3. **The "full30 retrospective: 0/8 caught by a relation" claim is
   contradicted by full30's own records.** `relation_replay_kept` is
   non-empty on 5 of the 8 caught overfits, and Math-2-o's run.log
   verdict line reads "2 verifier-kept relation conviction(s) — patch
   flagged as overfitting". Math-2 is caught ONLY this way: the
   relation fires on the fuzzed tier and is silent on the trigger
   literals, because the overfit passes the trigger scenario by
   construction — no lifted-test check can ever see it. Weeks of
   pre-replay history (math2pair etc.) confirm the harness alone
   missed it every time.

**The corrected statement:** rule INJECTION into harness prompts is
not a detection contributor (the ablation is valid evidence for that,
and it agrees with p23gate). Rule REPLAY is a detection contributor —
load-bearing for Math-2 and margin on four more. Rules stay ON
(with `--replay_relations_on_patched`) in every full-pipeline suite;
injection stays minimal as already shipped.

**Collateral fix 1 — the soundness-harden pass was destroying
convictors.** The `onefull` trace shows Math-53's field-level NaN rule
(the exact check the OBS item exists to build — it fired on 54% of
buggy-side inputs, i.e. it detects the bug) being probed ON THE BUGGY
BUILD, its bug-caused firings read as "fired on ordinary inputs =
strong evidence of unsoundness", and the model rewriting it into
`!z.isNaN()` — a form that is blind to this bug, because the buggy
result keeps NaN in one part. The rewrite was accepted because the
"repair still catches the bug" guard defaults to TRUE when the failing
test has no string literals (Math-53's is numeric-only). Fixed
mechanically in relation_screen: (a) a rule that fired on the buggy
build during screening is never hardened — probe firings on that build
are bug evidence, not unsoundness evidence; (b) a repair that cannot
be verified to still catch the bug (no trigger corpus) is discarded
and the original kept. Same lesson as ever: never auto-weaken on an
ambiguous signal; the replay verifier judges soundness downstream.

**Collateral fix 2 —** `--replay_relations_on_patched` added to every
full-pipeline cases file that synthesizes relations (14 files;
`abl_norel` deliberately left relation-free; `--rulegen_only` suites
replay unconditionally and need no flag).

**Process note:** the ablation runs were never archived to
`runs-archive/`, so the retracted conclusion cannot even be re-audited
— archive every measured run, including (especially) the ones that
justify a direction change.

**Validation result (struggle10, 2026-07-19, 10 legs):** TP=1 FN=4
FP=0 TN=5 — precision 1.00 held, and **Lang-27-o was caught for the
first time in project history** (harness-invented metamorphic
type-contract checks: `"0e0D"` must parse as Double, the overfit
returns BigDecimal; judge kept it with correct contract reasoning).
The repair is mechanically confirmed in every trace: replay runs on
every leg (quiet on all five correct legs — no precision cost), and R1
compile-repair produced 2 surviving Closure rules where history had
zero. Full leg-by-leg reading in
`runs-archive/runs/struggle10_20260719_073304/ANALYSIS.md`. Two
findings that CHANGE the plan:
- **Math-53-o is NOT an OBS bug.** The DeepRepair overfit's NaN
  handling equals the real fix; its only certified divergence (3×
  exception-class) is returning `Complex.NaN` from `add(null)` where
  the javadoc says throw NullArgumentException. No NaN rule of any
  shape can catch it. The mechanism is a documented-@throws rule —
  item R-THROWS below.
- **Lang-27's catch was structural luck, not new invention.** full30's
  harnesses had the same suffix-type checks but let the seed's
  NumberFormatException escape uncaught, so the patched-side fuzz died
  on the first (dismissible) crash before any metamorphic check ran.
  This roll fenced it. H6 makes the fencing deterministic; the
  confirming repeat rides in hfix11.

#### Documented-@throws rules (R-THROWS, NEW 2026-07-19)

Station 2. When a touched method's javadoc declares `@throws X` for a
named input class (null argument, malformed input, out-of-range), one
synthesized rule must construct exactly that input class and assert
the documented throw ("calling add(null) must throw
NullArgumentException; completing normally violates the contract").
Same trust tier as formula-first (the documented contract, source #3).
Shape properties: silent on buggy whenever the buggy build honours the
@throws (a tripwire — the screening change that keeps ALL silent
tripwires is a prerequisite, shipped 2026-07-19), fires on an overfit
whose reordered/added guard swallows the throw. Targets Math-53-o
(direct trace evidence) and generalizes Lang-27's idea (b) from CRASH.
Validate on the Math-53 pair: overfit convicts via replay, correct leg
stays quiet (the correct fix checks null FIRST, so the rule never
fires there). Risk: low — the input class is stated by the docs, not
guessed; the judge still reviews every firing.

## batch3 RESULT + the fix batch it forced (2026-07-19 eve)

batch3 (8 legs, same-day): TP=2 FN=1 FP=2 TN=3. Confirmed and now
stable: the Math-53 pair (R-THROWS 2nd consecutive), Closure-62-o (3rd
consecutive), Chart-26-c / Math-2-c guards (keep_going cost no
precision on the old FP classes). All three hard-pair targets failed,
and the traces name the causes — none of which is "the shipped
mechanism didn't work":

- **Lang-27-c FP — the judge OVERRODE a computed dismiss fact.** SYM-2
  fired correctly on `explicit-float-suffix` ("input does not
  reproduce the defect on buggy — dismiss") and the judge answered
  SOUND anyway, reasoning from the API contract. The older
  [symmetric firing] escape hatch ("keep if it belongs to the failing
  test's methods and observables") sat above the new fact and offered
  the looser criterion — everything in the patched method qualifies.
- **Closure-62-c FP — the right fact never fired.** The FP moved from
  the relation path (fpfix6) to a harness test-copy named generically
  `lifted-test`, which dodged the trigger-method name-matching of the
  [trigger-test lift] fact; SYM-2b's escape hatch ("unless it asserts
  the failing test's own pinned behaviour") is exactly what an
  unfaithful test-copy claims, so the judge kept it — although the
  real test was rerun on that build and PASSES, which mechanically
  proves the copy diverges.
- **Lang-27-o FN — invention now works, the judge is the bottleneck.**
  H4/H5 delivered the create*-family checks, keep_going ran them, they
  fired — and the judge dismissed 10/10, in the same batch where it
  kept the equivalent check shape on the CORRECT sibling. Strongest
  evidence yet that J1 (offline judge measurement) outranks R3.

Fixes applied (all general; validate via suites/batch4.cases, same 8
legs as batch3 + Math-2-o as canonical-replay regression guard):

1. **Hardened mechanical dismissal for the crashing symmetric case**
   (replaces the advisory note the judge overrode, and hardens the
   first auto-drop draft). Drop ONLY when all three computed conditions
   hold: (a) the buggy replay itself succeeded — an errored replay is
   ABSTAIN, never "no crash" (the draft conflated them via
   `replay_input(...) or ''`); (b) no defect exception appears
   ANYWHERE in the buggy replay output — headlines, cause chains,
   fenced rethrows (`exception_types_in_output`; headline-signature
   matching alone would let a harness that fences the defect and
   rethrows under its own alarm type fake "no repro" and delete a true
   catch); (c) the SAME check fires on buggy at the exact firing input
   (`replay_input_report`) — symmetric-in-scan alone proves it fired
   at SOME input, not this one. Anything less stays a fact for the
   judge. Consistent with the crash-pin precedent: a drop is allowed
   because every leg of the signal is computed, none judged.
2. **[trigger-test lift] fact now fires on generically-named lifts**
   (`lift`/`seed-test` id patterns), with the observed-value-vs-real-
   failure-message cross-check spelled out (the other agent's Part B,
   kept).
3. **Escape hatches rewritten to close the test-copy loophole.**
   [symmetric firing]: "same observable the failing test shows is
   wrong, not merely the same method". SYM-2b: states that the real
   test passing settles the test's own scenario in the patch's
   favour; keepable only for the bug's own observable at inputs the
   real test does NOT itself exercise (this is precisely the Math-2
   complement-symmetry shape, so the true-catch path stays open).
4. **De-overfitting sweep of prompt strings** (rule: abstract schemas,
   no bug-shaped examples). Removed from prompts: the
   createNumber/"L"-suffix example (valid-by-construction block), the
   "documented type-suffix selects the type" e.g.-clause (H5 families
   block — Lang-27's winning check verbatim), the add(null)+NaN
   receiver example (R-THROWS variation — Math-53 verbatim), and the
   `[oracle:mean-formula]` id example in two places (Math-2's convictor
   name). Each replaced with a neutral phrasing of the same principle.
   Audit method: AST scan of all non-docstring string literals for
   dev-set tokens; remaining hits are generic IEEE-754 edge-value
   enumerations only.
5. **[buggy-replay fact] on EVERY patched-side firing** (added after
   the first four, prompted by the question "anything else general?").
   The batch3 Lang-27-o trace has ZERO buggy-replay facts: the plain
   (neither latent nor symmetric) case computed nothing, and a
   dismissal WHY invented the missing fact ("already occurs on the
   buggy build too" — never checked). Now one replay of the exact
   firing input on the buggy build runs for every firing with a
   persisted input, and one unified fact covers all cases:
   fires-on-both (+ defect present = patch-failed-to-fix; absent =
   settled-scenario wording), quiet-on-buggy (clean run = existence
   proof: the buggy build itself satisfies the check here, so UNSOUND
   requires a contradicted documented contract, not an imagined
   implementation; shadowed-by-other-check and other-exception cases
   stated as inconclusive), replay-error = ABSTAIN. The hardened
   crashing drop now applies uniformly (scan-symmetry no longer gates
   it — the replay itself is the evidence). This deduplicated the
   three previous per-branch replay implementations.
6. **[replay comparison fact] on fuzzed-tier relation convictions** —
   the open FP channel (fpfix6 62-c) closed WITHOUT auto-dismissal.
   Screening already measures every relation on the buggy build;
   `screen_direction` is now persisted as data and the verifier gets
   the computed comparison: direction-confirmed + silent-at-trigger
   on patched = defect fixed at the trigger, keep only for
   same-defect-beyond-trigger firings; fires-on-both-builds
   off-trigger = decide by CONTRACT SOURCE (documented behaviour the
   bug is about → patch-failed-to-fix, KEEP — the Math-2 shape
   survives by construction; invented premise → pre-existing,
   DISMISS — the caret shape dies); silent-tripwire = keep only with
   a documented source (the R-THROWS shape survives by construction).

Still OPEN after this batch (unchanged): J1 — now the top item; the
fuzzed-tier relation-replay path still has no buggy-side computed fact
(the Closure-62-c FP class could reappear THERE; any fix must be
validated against Math-2-o, whose true catch has the same
fires-on-both-builds shape); R3, BND-b, P4.1, T11.

## batch4 RESULT (2026-07-19 late): TP=2 FN=2 FP=2 TN=2, 62-c UNSCORED

Validated again: Math-53 pair (R-THROWS 3rd consecutive — now fires on
the TRIGGER tier deterministically via the receiver-state variation),
Closure-62-o (4th), Math-2-c. The three problems, each diagnosed from
the trace and fixed the same evening:

- **Lang-27-c FP, third costume.** fpfix6: signed-zero via the loose
  family test; batch3: symmetric contract-override; batch4: LATENT path
  — the -0.0 sign-bit check's input crashes buggy with the defect, so
  the new crash-suppression fact fired and its wording ("strong
  evidence against the patch") prejudged the judge's only real question.
  Every correct fix has the buggy-crashes/patched-completes signature at
  former crash inputs. FIX: wording neutralized — fix and overfit differ
  only in whether the completed value is correct. LESSON: at defect
  inputs, c-vs-o discrimination is irreducibly the completed-value
  soundness question; no mechanical drop can take it from the judge.
- **Chart-26-c FP (regression vs batch3).** The old axis-entity class
  returned through the latent+shadowed path, whose notes carried no
  bug-family framing (the symmetric note's criterion never applied).
  FIX: family criterion added to the shadowed/other-exception notes.
- **Closure-62-c UNSCORED — H3 rejected all 8 harnesses.** The prompt
  mandates whitespace-REMOVAL in check messages; H3 compared with
  collapse-to-one-space, so multi-word values could never match and
  the gate rejected FAITHFUL harnesses. FIX: _values_match also
  compares whitespace-removed (verified on the exact rejected
  headline; true divergences still reject). The widened gate had
  turned a scoring leg into a non-scoring one — gates need the same
  normalization as the artifacts they judge.
- **Math-2-o FN (catch lost vs full30 era).** Not a facts problem:
  synthesis never PROPOSED a mean-shaped relation (6/6 survivors were
  sample/invcum — anchored on Arja's patched method), while the -c
  leg's roll produced three mean relations. Fresh, strong evidence for
  R2 (variety/coverage quota over distinct documented observables).
  The fuzzed-tier comparison fact went untested here (nothing fired);
  on 62-o it dropped two invented-premise caret rules while the
  harness path still convicted — no harm observed.

Score note: the mechanical drop went UNEXERCISED (no eligible firing
shape recurred) — the FP generator keeps shapeshifting to whichever
path lacks framing. The judge is now the single dominant error source
in both directions; J1 outranks everything else.

## batch5 RESULT (2026-07-19 night): TP=2 FN=2 FP=1 TN=4 — P=0.67 R=0.50

The day's headline: **Math-2-o CAUGHT for the first time since full30**
— `hypergeom-mean-formula`, deterministic TRIGGER-tier replay
conviction (2/2), synthesized only because the R2 test-subject
skeletons put the subject class's documented formula in front of the
synthesizer. The mechanism chain (context → formula-first → screening
→ replay → verifier) worked end to end. Also validated: Lang-27-c
finally TN (the three-costume FP cleared; RETRY fired on this correct
leg and cost no precision), Closure-62-c scores AND is clean (H3
parity fix confirmed), Math-53 pair stable (R-THROWS 4th consecutive).

The three remaining errors, each diagnosed:

- **Chart-26-c FP (2nd consecutive).** The fires-on-both fact was
  present THREE times and the judge kept sibling-chart-type
  null-info-draw checks anyway — and the fact's own escape hatch
  permits it: "the very behaviour the failing test shows is wrong, at
  inputs the test does not exercise" is satisfied by pre-existing
  sibling surface (same observable family, different chart type,
  crashes on both builds from an UNRELATED underlying exception).
  Missing discriminator, now named (NEXT-1): for a fires-on-both check
  that wraps an exception, compare the underlying exception types at
  the firing input (from the buggy replay output, per_oracle_crash
  parsing) against the DEFECT's own underlying types (the buggy-side
  types of the trigger-firing oracles) — disjoint = a different
  pre-existing crash wearing the family's clothes, computed dismissal
  fact. This generalizes crash-pin to latent/replay-sourced types and
  to semantic bugs whose defect manifests as a wrapped exception.
- **Closure-62-o FN (after 4 consecutive TP).** NOT a dismissal
  regression: nothing fired anywhere (3 harnesses quiet on patched,
  relations quiet, 3 survivors without the error/warning-asymmetry
  shape). Pure synthesis/fuzz roll. The reliable convictor shape for
  this leg is the sibling-agreement RELATION (formatError vs
  formatWarning) — R2's sibling-agreement slot should raise the odds;
  it did not fire this roll.
- **Lang-27-o FN (invention now SOLVED, reach is not).** All seven
  family checks were invented and accepted (double/float-suffix-agree,
  int-family, anchor-types …) — the exact struggle10-winning shapes —
  but stayed LATENT: the 20s patched fuzz never reached the
  discriminating suffix inputs. The bottleneck moved from invention
  (fixed by H4/H5+RETRY) to CORPUS REACH. Named (NEXT-2): seed the
  patched-side harness fuzz corpus with the failing test's literals
  plus mechanical suffix/case/sign variations (the H4/H5 family
  vocabulary applied to corpus seeding, not just prompts) — the
  deterministic version of the fuzz luck that made struggle10's catch.

Standing: J1 remains the top structural item (Chart-26-c is another
judge-override-with-facts-present data point). NEXT-1 and NEXT-2 are
the two mechanical builds; both are general (no bug-shaped content).

## batch6 RESULT (2026-07-20): TP=2 FN=2 FP=2 TN=3 — the split works;
## two known walls remain

Two-judge split, first live outing — the harm-watch PASSED and the
mechanism behaved exactly as designed where facts were decisive:
- Math-2-o and Math-53-o both KEPT through attribution (the
  documented-formula / @throws clauses held by construction).
- Chart-26-c TN (2-run FP streak ended).
- Attribution's first NOT_ATTRIBUTED was correct (62-c's
  fires-on-both seed check, dismissed with the right reasoning).

The four errors, decomposed:
- Lang-27-c FP: attribution said ATTRIBUTED for an escaped defect-type
  crash shared with buggy ("same observable as the failing test — duty
  to fix"). This is the diag-24 label-boundary class: a correct-labeled
  patch retaining defect-type crashes on junk inputs. The judge was
  never shown THE INPUT — the one datum that decides junk-vs-valid.
  NEXT: include the firing input's actual bytes/string in the fact.
- Closure-62-c FP: attribution went 1 NOT_ATTRIBUTED (right), 1
  INCONCLUSIVE (shadowed replay, fails open) — the INCONCLUSIVE one
  convicted. Shadowed-replay conclusiveness is the remaining
  mechanical gap (rerun with the shadowing check suppressed, or J1).
- Closure-62-o FN: the family mandate produced A family relation but
  not THE pair (JSError.format-vs-formatter instead of
  formatError-vs-formatWarning; the overfit preserves the former).
  Mandate needs "one relation PER same-prefix sibling pair of the
  patched method", not "one family relation".
- Lang-27-o FN: three firings, none kept; corpus fingerprint not
  verifiable from trace (prints don't land in trace.md — add a
  record_event for corpus seeding).

Standing read after six same-day batches on the same nine legs:
mechanical facts + the split have fixed every fixable class once, but
the residual errors now rotate among judge-discretion cases
(INCONCLUSIVE fail-open, duty-to-fix scope, input validity). That is
J1's territory, and further same-nine iteration has clearly
diminishing returns — next session: J1 offline on six batches of
archived decisions, then the full30 confirm.

## TO DO v2 (2026-07-20, post-batch6) — consolidated, by station

SHIPPED this session (context-insufficiency sweep, all general): firing
input bytes into the buggy-replay facts; the failing test's assert
lines AND the defect's underlying exception identity (types beneath
the trigger-firing checks on buggy) into both judges' bug summaries;
attribution answers restructured (quote the decisive FACT line, cite
the rule number; shadowed-replay = rule 3, not inconclusive);
soundness judge five-step reasoning protocol (restate -> source ->
strongest counterexample -> test vs catch/skip+trusted -> verdict);
synthesis deep-dive enumeration (list documented observables with doc
lines, mark patch-affectable, then spend slots) + sibling-PAIR
agreement mandate (per close pair, up to two); corpus-seed
record_event.

### Station 1 — bug context (EXACT SPEC, build next)
**Defect-identity capture at extraction time.** Run the trigger test
once against the buggy checkout at extraction (station 1 already
parses its failure); record structured: full expected/actual values
(whole, not ComparisonFailure fragments), the headline exception, and
the COMPLETE cause chain (type@frame per link). Store on FailureTest
(e.g. .defect_exception_chain, .expected_actual_pairs) and thread into:
H3's real_wrong_values (replaces fragment parsing), both judge bug
summaries (replaces the verdict-time reconstruction shipped today,
which only sees types the acceptance scan happened to record), and the
crash-identity comparison. One mechanism, four consumers.

### Station 2 — rule writing
- **Per-source synthesis passes (multi-LLM).** N small independent
  calls, each owning ONE contract source — formula-only, @throws-only,
  family-agreement-only, state/read-only — blind to each other; union
  screened as usual (screening dedupes). Coverage becomes structural
  instead of quota-enforced. Respects the p23gate lesson: nothing is
  injected into anyone's prompt.
- R-INH inherited javadoc: verify supertype skeletons actually carry
  the parent declaration's @throws/@param for the touched method; add
  interface files if missing.
- R3 (doc-poor passing-test anchoring): unchanged, still the only
  mechanism for Closure-33/92-class legs.

### Station 3 — screening
- BND-b: documented-limit probes (MAX_VALUE, 1e20-scale, NaN, empty)
  in screening + replay corpora; shares the literal_variations
  generator. Targets the Math-57 class of divergences.

### Stations 4/5 — harness + acceptance (EXACT SPEC, build next)
**Deterministic slot roles.** The -n 3 harness slots get assigned
roles in the prompt instead of hoping diversity emerges: slot 1 =
test-copy + trigger reproduction (the acceptance anchor); slot 2 =
family/metamorphic checks ONLY (sibling agreement, input variations,
documented selection rules) plus minimal trigger reproduction; slot 3
= state/consistency checks ONLY (read-only guarantees, recompute-and-
compare aggregates) plus minimal trigger reproduction. RETRY remains
as the backstop when a role-slot fails acceptance. Implementation:
role paragraph appended per-slot in the campaign's prompt_factory;
acceptance gates unchanged.
- Math-57 harness-generation robustness (javac repair loop) — that leg
  is unmeasurable until fixed.

### Station 6 — patched-side execution
- **Single-check replay for shadowed firings.** Extract the fired
  check (its oracle id names the throw site; take the enclosing
  method/block), wrap it in the relation-screen single-check template,
  compile against buggy, replay the exact input. Converts the
  shadowed INCONCLUSIVE (batch6 62-c FP) into a computed fact. Reuses
  _screen_harness_source machinery.
- Quiet-leg bounded extension: one extra 60s fuzz pass when zero
  checks fired anywhere on patched (keys on observed silence, not
  labels).
- P4.1 do-nothing detector: still the only mechanism for
  behaviour-preserving overfits (Chart-3), still gated on the offline
  false-flag study.

### Station 7 — judges (EXACT SPEC for the next steps)
- **J1 offline judge study (FIRST, before any further judge change).**
  Materials exist: six archived batches with VERDICT/WHY and
  ATTRIBUTION/FACT/WHY lines plus known ground truth. Measure: (a)
  soundness flip-rate on near-identical checks; (b) fact-override
  rate; (c) attribution rule-citation accuracy; (d) whether the
  3-lens vote ensemble (already implemented, votes=1 default) helps
  WITH facts present — the old null result was measured without them.
  Output decides: enable votes, add verdict categories, or stop.
- Attribution INCONCLUSIVE handling after single-check replay lands:
  re-examine whether any inconclusive class remains.
- Two-judge split: shipped and validated (batch6); no further split
  without J1 evidence.

### Cross-cutting
- The next full run is the FULL30 confirm (pinned_dev set), not a
  7th same-nine batch — six same-day tuning rounds on nine legs is
  selection pressure even with fully general code. Framing: context-
  sufficiency audit — for every judge call in the run, does the
  prompt contain what a careful human would need? Grep targets:
  verdicts whose WHY cites information NOT present in the prompt
  (hallucinated grounds), INCONCLUSIVE attributions, UNSOUND verdicts
  with no surviving counterexample stated.
- Held-out discipline: FINAL only after full30 is stable.

## full30v2 RESULT (2026-07-20): TP=3 FN=13 FP=1 TN=13 — the audit
## run caught a regression MY OWN prompt optimization introduced

P=0.75 (only Lang-27-c FP — precision machinery generalized to the
full 30). R=0.19 — a recall COLLAPSE against the historical full30
(8/16). Root cause found within the hour, from the traces:

**The five-step soundness protocol's step 4 tested counterexamples
against catch/skip + trusted values but NOT against the observed
firing values.** Instructed to "construct the strongest
counterexample", the judge became a systematic counterexample
generator, and every conviction with ANY conceivable edge-case story
died — the Math-2-o formula conviction (DIRECTION-CONFIRMED,
deterministic 2/2 trigger replay, observed -49.76 vs +49.82) was
dismissed on a tolerance hypothetical that cannot produce a
100-unit sign-flipped divergence. The numbered protocol operationally
overrode the prose "observed evidence beats hypotheticals" guidance.
FIXED same hour: step 4 is now a two-part test — (a) catch/skip +
trusted values, (b) the counterexample must be able to produce THE
OBSERVED disagreement in kind and magnitude (tolerance stories cannot
explain sign flips; nondeterminism stories cannot explain 2/2
deterministic replays). Verified discriminating against the traces:
it revives Math-2-o while correctly leaving the KMeans
randomness dismissal dead (that counterexample CAN produce the
observed disagreement).

Context-sufficiency verdict (the question this run was framed to
answer): the sampled wrong verdicts cite only in-prompt information —
no hallucinated grounds found; the remaining failures were reasoning
CALIBRATION, not missing context. The context sweep held up.

Separate open diagnosis: Math-53-o's @throws relation (correct shape,
screened, replayed) was QUIET on the patched build this run — 4 prior
runs fired 20000/20000. Not a judge issue; needs trace-level
debugging of the replay wrapper before the next run.

Process note: a 3M-token confirm run after single-batch prompt
changes was the right order — it caught the regression before FINAL.
Protocol for the future: any change to judge REASONING structure
(not facts) gets a mini-suite validation (the 9 legs) BEFORE a full30.

## falsefix13 RESULT (2026-07-20): the two-judge split's ATTRIBUTION
## stage is a recall killer — REVERTED to off-by-default

falsefix13 (13 wrong legs rerun): TP=1 FN=11 FP=0 — worse than
full30v2. But the trace tabulation is the real result and it is
decisive: on EVERY leg with a SOUND check (Closure-73, Closure-92,
Lang-41, Lang-50, Lang-60, Math-2 — 13 sound findings across 6 legs),
attribution returned NOT_ATTRIBUTED. 100%. The only attribution
survivor in the whole run was Math-53 (via R-THROWS).

Two separate results, do not conflate:
1. Step-4b WORKED. Soundness now correctly rules Math-2-o's
   mean-formula SOUND (was the full30v2 regression). Keep it.
2. The ATTRIBUTION judge, as specified, vetoes ~100% of sound
   GENERALIZATION catches. Its rule 3 ("same-on-both-builds ->
   attributed only for the test's own observable or a quoted
   documented guarantee") rejects exactly the catch shape the whole
   system produces: an overfit is caught precisely BY a check on a
   DIFFERENT observable than the one it special-cased (Lang-41
   sibling-agreement, Lang-60 capacity, Math-2 mean). A
   DIRECTION-CONFIRMED relation is MECHANICALLY proven to detect the
   defect on buggy; routing it through an attribution LLM that then
   says 'different observable, not the patch's duty' throws that
   proof away. batch6 hid this (9 legs, the rules happened to pass
   Math-2/53); the full catch-shape spread exposes it.

ACTION: --attribution_judge added, DEFAULT OFF. Both call sites gated.
Reverts to the batch5 single-verifier state (caught Math-2-o at zero
FP). The split's precision motivation (Chart-26-c) is already covered
by the computed crash-identity fact + crash-pin; attribution added
little precision and destroyed recall. Do NOT re-enable without J1
offline proof that per-shape precision gain > recall cost.

DIRECTION VERDICT (the standing question): computed facts + context =
right direction, every such fix durable. LLM JUDGMENT LAYERS added on
top of facts = wrong direction, now demonstrated twice in one session
(the 5-step protocol regression AND the attribution veto). The
discipline holds: add facts and context, REMOVE judgment where a fact
already decides. Next: revalidate falsefix legs with attribution off
(should restore the full30-era catches), then ceiling-raising builds
(R3, BND-b, P4.1) — NOT more judge layers — then J1.

## ATTRIBUTION FIX VALIDATED + the real bottleneck exposed (2026-07-20)

The attribution rebuild is DONE and WORKS. Evidence:
- attr2 iter-2 (2 legs): Math-2-o TP via the mechanical
  direction-confirmed keep (no LLM); Lang-27-c (the persistent 6-run
  FP) TN via a CORRECT attribution NOT_ATTRIBUTED that read the
  firing-input bytes and called them non-numeric junk. Both right
  reasons.
- attr5 (5 legs): Lang-41-o flipped FN->TP via a correct ATTRIBUTED.
  So where a catch FIRES, the re-resourced symmetric deep-dive prompt
  + mechanical keep now attribute correctly — the falsefix13 collapse
  (attribution vetoing ~100%) is fixed.
Config: --attribution_judge, still OFF by default; the shadowed-note
dismissal bias in run.py was neutralised.

THE REAL BOTTLENECK (attr5 exposed it): recall is dominated by
SYNTHESIS ROLL-VARIANCE, not the judges. attr5's three FN legs
(Math-2-o, Lang-60-o, Math-53-o) each had replay_fired=[] and
harnesses_crashed=0 — NOTHING reached attribution. Math-2-o: this
roll synthesis proposed 6 relations and NONE was the mean-formula
(it wrote hypergeom-VARIANCE-formula + inverse-cdf instead); the one
relation that convicts this overfit was simply not proposed. It WAS
proposed in iter-2/batch5/batch6. Same code, different roll.
'formula-first MUST' is satisfied by ANY documented formula, not the
one the patch breaks.

DIRECTION VERDICT (sharpened, the standing question): the judge work
this cycle was necessary cleanup (attribution is now net-positive
when a catch fires) but recall is gated UPSTREAM, at synthesis: the
convicting relation is a per-roll coin flip. The next lever is
SYNTHESIS RELIABILITY, a mechanism, not more judge prose:
- PER-SOURCE / MULTI-ROLL SYNTHESIS (TODO item, now top priority):
  run synthesis as N independent passes (or one pass per contract
  source — formula / @throws / family / state), union the screened
  survivors. The union reliably contains the convicting relation
  where a single roll misses it. Respects p23gate (no injection) and
  the no-pooling rule (within-leg only). This directly kills the
  Math-2-o variance-vs-mean roll variance.
- Only measure attribution's aggregate value AFTER synthesis is
  reliable — otherwise attribution's contribution is masked by
  catches that never fire. J1 still applies to the judge decisions.

Do NOT keep expanding attr* sets: they have already answered their
question (attribution works when catches fire). Next build is
per-source synthesis; validate it on the SAME legs (Math-2-o must
fire the mean-formula every roll).
## FOCUSED SYNTHESIS: recall WIN, precision cost exposed (2026-07-20)

Built --focused_synthesis: 4 narrow per-source passes (formula /
throws / family / state), unioned, screened as one. Validated across
foc2 (Math-2 pair, 4 iterations to clean) and foc5 (5 legs).

RECALL — clear win, the goal achieved:
- foc5: 4/4 overfits caught, EACH by its own pass — Math-2-o (formula),
  Math-53-o (throws), Lang-60-o (family), Lang-41-o (sibling). The
  roll-variance that made these coin flips is gone; the convicting
  relation is now reliably PROPOSED. This is better recall than any
  full30 (which caught ~8/16).

PRECISION — a real cost, honestly: Math-2-c FP'd FOUR distinct ways
across the iterations, one per pass:
  1. throws: ctor must throw NotPositiveException (multi-constraint
     input) -> ISOLATION RULE (break only one precondition).
  2. harness bounds: mean vs integer support off by 2e-12 rounding ->
     ROUNDING FLOOR in soundness (sub-1e-9-relative = rounding).
  3. relation bounds/exact-formula: no-tolerance checks -> restored the
     SHARED FENCING block the passes had dropped.
  4. state: 'no public reader changes across getNumericalMean()' fires
     on correct code (lazy cache / call-order reader). STILL OPEN.
Each fix was general and correct, but the pattern is the point: MORE
candidate checks (the mechanism of focused synthesis) means MORE FP
surface on a pathological correct leg (Math-2-c has billion-scale
inputs + lazy caching + degenerate n=N). In the real full30, Math-2-c
was TN under single-call synthesis; focused synthesis is what makes it
FP. Precision now depends on the SOUNDNESS JUDGE catching unsound
checks regardless of source — and it passed all four (said SOUND on a
'no reader changes' check that fires on correct code).

CONCLUSIONS (do not whack-a-mole a 5th fence):
1. KEEP focused synthesis (behind --focused_synthesis) — the recall
   win is real and it is the answer to roll-variance.
2. The STATE pass is the weakest link (broad 'no reader changed'
   checks are FP-prone via lazy caching, and Lang-60 was caught by the
   FAMILY pass, not state) — make it conservative (only assert a
   SPECIFIC documented read-only guarantee, never a blanket
   all-readers snapshot) or gate it off.
3. J1 is now DOUBLY confirmed as top priority: as candidate volume
   grows, the soundness judge's reliability IS the precision ceiling.
   It let four unsound checks through. Measure it offline (7+ archived
   batches) before trusting focused synthesis on the full set.
4. The recall-vs-precision tradeoff of focused synthesis can only be
   judged on the FULL set, not 5 legs — but ONLY after (2) and (3),
   or the pathological-correct-leg FPs will dominate the number.
## J1 RESULT (2026-07-20): the recall misses, quantified — synthesis
## reach is the biggest bucket, soundness over-dismissal the second

Ran study/j1_judge_audit.py offline over all archived traces (158 legs,
no VM). Two headline outputs:

MISSED OVERFITS (56 FN legs) decompose into three buckets:
  26 (46%)  NOTHING reached a judge — the convicting check was never
            synthesized or never fired (synthesis roll-variance +
            fuzz reach). THE BIGGEST bucket.
  20 (36%)  soundness dismissed ALL checks (UNSOUND with no SOUND) —
            counterexample-generation over-dismissal (the full30v2
            5-step regression class: the judge invents an edge/lazy/
            reject counterexample that does NOT reproduce the observed
            firing, e.g. 'a correct StrBuilder could lazily eval
            contains(char)').
  10 (18%)  attribution killed a sound check (NOT_ATTRIBUTED) — the
            rule-3 over-restriction (already rebuilt).

FALSE POSITIVES: soundness said SOUND on a correct leg 27 times;
clustered by WHY, the fixable groups are bounds/support (9, the
no-tolerance mean-within-support class) and exception-class (4, the
multi-constraint @throws class). Both now have fixes (rounding floor,
isolation rule) — J1 is the way to confirm they shrink these cells.

DIRECTION VERDICT — J1 makes it quantitative, not intuitive:
1. Synthesis reach is the #1 recall lever (46% of misses). Focused
   synthesis + variation-seeded corpus attack exactly this, and foc5
   showed 4/4. This session's biggest effort targeted the biggest
   bucket — right direction, confirmed by data.
2. Soundness OVER-dismissal is #2 (36%). The lever is NOT 'dismiss
   less' (it must still dismiss the 27 FP-risk checks on correct legs)
   — it is CALIBRATION: a dismissal is valid only if the counterexample
   reproduces the OBSERVED firing (step-4b), extended beyond numerics
   to boolean/exception checks (a 'could lazily eval' counterexample
   must be shown to actually produce the observed true/exception, not
   asserted). This is the top judge build.
3. Attribution is #3 (18%) and already rebuilt; keep off until foc15
   confirms the rebuild in aggregate.

NEXT (evidence-ordered): confirm foc15 (synthesis reach on 15 legs) ->
extend step-4b calibration to non-numeric checks (soundness bucket) ->
re-run J1 to confirm the FP/FN cells shrank -> THEN ceiling builds.
## foc15 RESULT (15 legs): R=0.89 best-of-cycle, P=0.73 — the
## focused-synthesis tradeoff, measured

TP=8 FN=1 FP=3 TN=3. Recall 0.89 (8/9 mechanism-reachable overfits) is
the HIGHEST of the whole cycle (historical full30 ~0.50). Focused
synthesis + variation corpus + the state-pass fix delivers the recall
win at scale, not just on 5 legs. Precision 0.73 (3 FP of 6 correct).

The state-pass fix WORKED: Math-2-c is now TN (the 4-iteration foc2 FP).

The 3 FPs, by source:
- Closure-62-c: RELATION caret-at-end-of-line-error — the chronic
  Closure-62 caret-premise class, pre-dates focused synthesis.
- Time-4-c: RELATION with_vs_withField_valid_absent_field — a FAMILY
  pass sibling-agreement asserting with()==withField() where they
  legitimately differ on an ABSENT field. NEW, focused-synthesis-
  introduced, fixable (family pass must exclude cases where the
  documented difference between siblings applies).
- Lang-60-c: HARNESS oracle (not a relation) — harness-path FP,
  independent of focused synthesis.
The one FN (Lang-60-o) was a bad roll: the correct sibling FP'd while
the overfit's convicting check did not fire.

READ: focused synthesis added ONE new FP class (family-pass absent-
field); the other two FPs are pre-existing/harness-path. So the
precision cost of focused synthesis SPECIFICALLY is smaller than the
raw 3 suggests. The recall gain is large and real.

This is consistent with J1: FP surface grows with candidate volume and
is gated by the soundness judge (which passed all three). NEXT per the
evidence order: (1) family pass — exclude the documented-difference
case (sibling agreement holds only where docs say the members agree);
(2) the soundness calibration build (J1's 36% bucket) — extend step-4b
so a dismissal's counterexample must reproduce the OBSERVED firing for
boolean/exception checks too, which should shrink BOTH the FN
over-dismissal and stop future volume FPs; (3) re-run J1 + foc15 to
confirm cells shrank; THEN ceiling builds. Focused synthesis is worth
shipping behind its flag; default-on decision waits on (1)-(3).
## foc15b RESULT: family + soundness fixes -> P 0.73->0.80, R held 0.89

Same 15 legs, before/after:
  foc15  TP=8 FN=1 FP=3 TN=3  P=0.73 R=0.89 F1=0.80
  foc15b TP=8 FN=1 FP=2 TN=4  P=0.80 R=0.89 F1=0.84
Recall HELD (the stricter soundness dismissal did not false-keep into
lost catches); precision UP; F1 up. Both fixes landed:
- Family documented-difference rule: Time-4-c CLEARED (was the family
  with/withField absent-element FP) -> TN.
- Lang-60-c also cleared -> TN.

THE RESIDUAL FPs ARE NOW BOTH HARNESS-PATH, not relations — the
synthesis/relation side is CLEAN:
- Closure-62-c: HARNESS caret lifted check (chronic).
- Math-53-c: NEW — HARNESS lifted check `lifted-w-real` asserting
  x.add(Complex(1,NaN)).getReal()==NaN, but a correct add returns
  real=4.0 (only the IMAGINARY part is NaN); the check is unsound and
  soundness kept it. WATCH: this may be a side effect of the step-4b
  boolean/exception extension making dismissal stricter (foc15 had
  Math-53-c TN); could also be harness roll-variance. One targeted
  re-run of the Math-53 pair would disambiguate.

Lang-60-o FN: replay_fired=[] harnesses_crashed=0 — NOTHING fired
(J1's 46% synthesis-reach bucket), not a judge dismissal. The
soundness fix cannot help a check that never fired; focused synthesis
still missed the Lang-60 capacity check THIS roll.

NET READ: the synthesis+judge work has driven the RELATION path clean
(0 relation FPs) at R=0.89. The frontier moves to (a) HARNESS-path
lifted-check soundness (both residual FPs), and (b) synthesis reach
for the specific legs that still roll-miss (Lang-60). Confirm Math-53-c
is roll-variance not a step-4b overcorrection before shipping the
soundness extension wider; then re-run J1 to confirm the cells shrank.
## harn6 RESULT: NaN-artifact fix validated; Closure-62 caret is the
## last stubborn FP class (harness/relation reconstruction, not my rule)

harn6 (6 legs): Math-53-c NaN-artifact FP -> TN (FIXED); Math-53-o,
Math-2-o, Lang-41-o all TP (recall held). The soundness rule 'a
structurally-impossible firing (immutable operand changed) is the
check's NaN==NaN comparison broken, not a defect' works, and confirmed
the foc15b Math-53-c FP was harness roll-variance NOT a step-4b
overcorrection.

Closure-62 pair inverted this roll: 62-c FP (HARNESS caret check),
62-o FN (a caret RELATION fired 6240/20000 but was not kept). NOT my
regression — the NaN-rule text in the trace is prompt-template (3x =
one per soundness call), zero actual drops cite it. This is the
chronic CLOSURE-62 CARET-RECONSTRUCTION class: the caret checks
(harness lifted AND focused-synthesis) rebuild the formatter's exact
output (source line + per-char space padding + caret), and the
reconstruction diverges from the real formatter, so the same check
can fire on BOTH builds and be kept on one leg / dropped on the other
(a judge flip). It is separate from every FP fixed today and is
genuinely hard (H3 faithfulness territory).

STANDING after today's judge/synthesis push: FOUR FP classes fixed
(family absent-element, state blanket-snapshot, bounds/@throws
tolerance, NaN==NaN structural artifact); relation path otherwise
clean at R~0.89. The one unresolved precision class is the Closure-62
caret RECONSTRUCTION (a check that rebuilds formatter output instead
of reading a post-condition). NEXT if continuing precision: forbid /
fence output-reconstruction checks (assert a post-condition read off
the API — a caret is present/absent, a column index — never a rebuilt
full string), which is the general form of H3's lesson. Otherwise the
core recall+precision work is in a good, shippable-behind-flags state;
re-run J1 to quantify the net cell movement.
## A/B RESULT (clean, same 12 legs): focused+attribution does NOT beat
## the single-call baseline — the real win was the JUDGE calibration

The decisive measurement. Both arms carry today's UNCONDITIONAL
soundness fixes (rounding floor, NaN structural-artifact rule, step-4b,
@throws isolation); the ONLY difference is the two flags.

  ab_off (single-call, NO attribution): TP=5 FN=1 FP=0 TN=6
      P=1.00 R=0.83 F1=0.91   1.06M tokens
  ab_on  (focused + attribution):       TP=5 FN=1 FP=2 TN=4
      P=0.71 R=0.83 F1=0.77   1.57M tokens

Per-leg diff (on vs off): Lang-41-o off-FN->on-TP (+1 recall);
Lang-60-o off-TP->on-FN (-1 recall); Math-2-c and Closure-62-c
off-TN->on-FP (-2 precision). Closure-92-o (a WALL) was caught by
BOTH arms — single-call got it, not focused synthesis.

VERDICT: flags-on shows NO net recall gain (the one win and one loss
are roll-variance — recall count is identical and within noise) and a
robust PRECISION COST (2 distinct FP mechanisms), at +48% tokens. On
this set the single-call baseline is strictly better.

The honest reframing of the whole session: the recall gains attributed
to focused synthesis (foc15 R=0.89, the J1 0.28->0.90 cut) were
CONFOUNDED — most of the lift came from the UNCONDITIONAL judge
calibration, which also lifts the baseline to R=0.83 P=1.00. foc15/
foc15b never had a flags-OFF control; this A/B provides it and
disconfirms the focused-synthesis-specific benefit.

DECISIONS:
1. KEEP --focused_synthesis and --attribution_judge OFF by default
   (they already are). The A/B says they do not earn their cost/FP.
2. SHIP + RELY ON the unconditional soundness-judge calibration —
   baseline R=0.83 P=1.00 on 12 legs (incl. a wall) is the strong,
   genuine deliverable of this session.
3. Focused synthesis stays available behind its flag for the narrow
   case where a SPECIFIC convicting relation's reach is the proven
   bottleneck (it does reliably produce e.g. Math-2's mean-formula) —
   not as a default.
4. Do NOT overclaim from 12 legs: recall is within noise; the
   precision cost is the more robust signal. A larger held-out A/B
   would tighten it, but the direction is clear enough to set defaults.

Frontier now: the judge calibration is the proven lever. Next real
recall work is the untouched WALLS (R3/BND-b/P4.1), which neither arm
addresses.


=============================================================================
PART 2 — CYCLE ERA (2026-07-26 → 07-30)
=============================================================================

## 2026-07-26 — candidate ledger for cycle 4+ (from the full commit-history audit)

Context: Retro #3 established that run-to-run variance is now the dominant problem —
identical code swung R 0.29 vs 0.57 on identical legs, and catches ride the "generation
lottery" (whether a given roll invents the right check at all). This section is the result
of re-reading every campaign commit and run with that finding in hand: what is confirmed
dead, one verdict that no longer meets our own evidence standard, and ranked candidates.
Everything here respects the meta-rule (mechanisms, never prompt advice) and the standing
user rules (no cross-run pooling; no dataset-shaped fixes).

### Confirmed dead since the rejected-ideas list was last updated

- **The relation-family menu as a diversity source** — built 2026-07-18, demoted the same
  day by its own coverage test (menu covered ~18% of what free invention produces).
  Already on the delete list; recorded here so "force each round to pick a different
  family from a list" is never re-proposed as a variance fix.
- **Judge majority voting** — now dead in both regimes (entry above updated).
- **The attribution judge as a veto** — falsefix13: vetoed ~100% of sound catches; the
  fresh-bug arm C scored 0.29. Off by default with a do-not-re-enable condition; the
  surviving piece is the mechanical direction-confirmed keep (attr5-validated).

### A kill verdict that no longer meets our own standard

**Focused per-source synthesis** (formula/throws/family/state passes, unioned) produced
the campaign's best recall evidence: foc5 caught 4/4 by-pass targets, foc15 hit R 0.89,
and the foc2 iterations made the Math-2 mean-formula relation appear in EVERY roll — the
exact anti-lottery property cycle 4 wants. It was switched off by the 2026-07-21
ab_off/ab_on A/B (F1 0.91 vs 0.77, 12 legs, ONE roll per arm). Retro #3 has since banned
single-roll comparisons: ±4 catches of pure noise on 30 legs is bigger than the gap that
killed the flag. The kill may still be right (its FPs were real), but the evidence is
void by our current rules. Re-adjudicate under the paired rule — or cheaper, via the
invention-rate instrument below — before treating focused synthesis as dead.

Related inconsistency to resolve the same way: the harness-width raise to n=5 (f18ac9a)
sits in tension with the recorded "blanket increase of harnesses" rejection (zero false
alarms was measured at n=3), and the width5 suite contains only overfit legs — the
precision cost of n=5 is currently UNMEASURED. The next width measurement must include
correct legs.

### Ranked candidates

1. **Trace-mine per-leg invention rates — free, do first.** poolA, poolB, pool30 and
   width5 are 3–4 independent rolls of the same legs, already archived, and each trace.md
   records every relation invented. Tabulate per leg: was the known catching shape
   invented, screened-in, fired, kept? Output: each leg classified reliable /
   coin-flip / never-invents. This tells cycle 4 where width helps and where only a
   different mechanism can (J1 did exactly this for the judge; generation has no
   equivalent yet).
2. **Generation-replay harness** — the missing cheap instrument, twin of
   `verifier_replay.py` (whose ledger entry reads "run this before trusting any verifier
   change"). Builds cached; rerun synthesis+screen only, K rolls per leg; report
   invention rate of the catching shape. Generation changes then get measured in pennies,
   and paired 30-leg pools become confirmation-only.
3. **Re-adjudicate focused synthesis** using instrument 2 (or a paired pool if the
   instrument slips): does the flag raise invention rates on the coin-flip legs without
   raising junk-invention on correct legs? The mechanism is already built and flag-gated;
   this is measurement only.
4. **P4.1 (did-nothing-patch detector) offline false-flag measurement — its blocker has
   dissolved.** P4.1 was gated on "measure the false-flag rate over verified-correct
   patches first"; the 2026-07-21 correct-side certification (3c6b3ff) produced 142
   confirmed-correct patches — exactly that measurement set. Buggy-build-only, so
   firewall-clean; ship only at a measured near-zero rate, initially as an escalation
   trigger per the original spec.
5. **Cycle-4b conviction confirmation + the silent-leg mirror.** Convict only when an
   independent second roll of the leg also convicts (kills the accusation lottery; cost
   per accusation, not per leg). Mirror for recall: re-roll only legs whose first roll
   produced NO firing at all (cost 1 + fraction-silent). Both are within-verdict re-rolls
   of one leg with nothing persisted across runs — but confirm that reading against the
   no-pooling rule with the user before building.
6. **Small precedented adds:** (a) repair-instead-of-drop for screened relations that die
   mechanically (compile failure, indiscriminate fire) — R1 compile-repair is the
   validated precedent (+10 relations, zero false-fire cost), and the poolA Closure-92
   broken-but-correctly-aimed oracle is the motivating case; (b) sharpen Spec N's
   convergence gate from "zero non-seed power" to per-observable coverage: mechanically
   list patch-affectable observables, aim the bounded extra rounds at those with zero
   surviving relations.

Longer-horizon direction (no cycle slot yet): **verdict-as-decision-table**. Cycle 2d
already made one fact terminal; the fact-priority rule ranks the rest. A J1-style pass
over archived verdicts would count how many are fully fact-determined — every such
verdict can become deterministic code, permanently shrinking the judge's coin-flip
surface (the Lang-60 flip class).

Process pre-commitment (Retro-#1 lesson, restated because the three costliest wrong turns
were all measurements that contradicted an already-recorded rule): for items 3–5, write
the measurement design (paired / offline, success criteria) into this doc BEFORE building
anything.

---

## 2026-07-26 — immediate action plan (post-night20), in execution order

Handoff plan for the working agent. Evidence base: night20 (8/14 catches — exactly the
7.7 the three prior rolls predict, so NOT yet an improvement signal), plus a trace audit
of the novelty gate: **7 family-novelty rejections run-wide, 6 of them vacuous** (the
rejected harness's extracted family list was EMPTY — the extractor bug — so the gate has
only done its designed job once so far). Every item below has a done-criterion; nothing
launches a paired pool until steps 1–4 are done. Standing rules apply throughout: no
cross-run pooling; certification data may inform OFFLINE diagnosis only, never a
pipeline input; fresh12 stays gated on the user's explicit go.

### Fixes first (small, evidenced, do immediately)

1. **Fix the family extractor.** A harness whose check families extract as EMPTY must
   never be rejected for "adding no new family" — empty extraction is a parse failure,
   not redundancy. Treat empty-extraction as fail-open (accept-eligible, log a
   `family-extract-failed` event through record_event) and fix the extraction itself
   (night20 traces hold 6 real failing examples to build fixtures from).
   *Done when:* unit tests over those 6 archived harness sources extract non-empty
   families, and a vacuous rejection is impossible by construction.
2. **Event-log the gate's decisions with the family lists** (if not already fully
   trace-surviving): every reject must record harness_families + accepted_families so
   the next audit is grep-able. (night20 already mostly does this — verify, close gaps.)

### Cheap diagnoses before any new machinery (free / pennies; they decide steps 5–6)

3. **Four-roll invention-rate tabulation (candidate-ledger item 1, now with 4 rolls).**
   pool30, poolA, poolB, night20 share 14 overfit legs. From the traces, tabulate per
   leg × roll: was a check of the known catching family (a) invented, (b) accepted/
   screened-in, (c) fired on patched, (d) kept by the judge? Output: one table, legs
   classified reliable / coin-flip / never-invents / invents-but-never-fires.
   *Done when:* the table exists in this doc and answers: did night20 (width 7 + gate +
   cycle-3b directives) invent catching shapes on legs where prior rolls didn't?
4. **Chart-19 fire-capability replay (offline).** Take night20's accepted Chart-19
   checks, compile against the PATCHED build, and drive them at the certification's
   known discriminating inputs — offline analysis only. Question: can ANY accepted
   check fire on this overfit in principle?
   - If yes → the miss is reach/budget; step 5 is justified.
   - If no → the miss is check-shape (e.g. receiver states built deterministically, so
     fuzz time only varies cosmetic strings); step 5 is pointless for this class and
     the fix is generation-side (state variation drawn from `data`, not constants).
   *Done when:* a one-paragraph verdict with the replay evidence is recorded here.

   **VERDICT (2026-07-26 — offline replay, evidence recorded): NO — the accepted null-family
   checks are STRUCTURALLY INCAPABLE of firing on this overfit; the miss is check-shape, not
   reach/budget. Step 5 (fuzz-budget raise) is pointless for this class; the fix is
   generation-side.** The Arja overfit adds one line to `AbstractObjectList.indexOf`: a
   post-scan-loop `if (object == null) throw new IllegalArgumentException(...)`. So
   `indexOf(null)` throws ONLY when the scan completes with no early return — i.e. when the
   axis `ObjectList` has NO null hole (dense/contiguous). With a null hole it returns that
   index early and never throws. The developer FIX instead guards at the top of
   `CategoryPlot.getDomainAxisIndex`/`getRangeAxisIndex` (`if (axis==null) throw ...`), so the
   fix throws for null UNCONDITIONALLY and leaves `indexOf` returning -1. Therefore
   overfit≠fix (overfit fails to throw) ONLY on SPARSE receivers with a null hole, e.g. after
   `setDomainAxis(2, …)` which leaves index 1 null. (Note: the item's parenthetical had the
   direction inverted — the overfit fails-to-throw on SPARSE plots, not dense/fresh; dense is
   where it accidentally matches the fix.) STATIC: all four accepted harnesses
   (attempt_003/010/013/015) probe `getRangeAxisIndex(null)`/`getDomainAxisIndex(null)` only on
   DETERMINISTIC dense receivers — `new CategoryPlot(null, axis, axis, null)` with at most a
   fixed `setRangeAxis(1,…)`/`setDomainAxis(1,…)` (contiguous, no holes); fuzz `data` feeds only
   axis-label strings (`consumeAsciiString`), never index placement. The fuzz-controlled sparse
   `ObjectList`s that ARE built (`consumeInt` slots with gaps, e.g. attempt_010/013) are never
   probed with `null` — they only call `indexOf`/`getRangeAxisIndex` with non-null objects. The
   null path and the hole-creating path are disjoint in every harness, so no fuzz input can
   construct the discriminating (null-probe × sparse-receiver) combination. EMPIRICAL (VM,
   defects4j Chart-19b + Arja patch = overfit build, vs 19f = fix build; driver reproducing the
   accepted receivers, `/home/code/scratch/item4/`): on the OVERFIT build every dense accepted
   receiver THREW IllegalArgumentException (identical to fix → accepted checks read "contract
   honoured" → no fire); the sparse receivers `setDomainAxis(2,…)`/`setRangeAxis(2,…)` then
   `getXAxisIndex(null)` COMPLETED returning index 1 (diverges from fix, which throws — verified:
   the fix build throws IAE on the same sparse call). Secondary missed discriminator: a direct
   `ObjectList.indexOf(null)` on a no-hole list throws on the overfit but returns -1 on the fix;
   no accepted objectlist check ever calls `indexOf(null)`. FIX DIRECTION: generation must draw
   receiver hole-structure from `data` (fuzz the axis install index / leave gaps) AND route the
   null probe through the fuzzed sparse receiver — not merely vary label strings on a constant
   fresh plot. Budget raises cannot help because the discriminating state is a compile-time
   constant in these checks.

### Item-3 result: four-roll invention tabulation (2026-07-26)

Cell = furthest stage reached that roll: **I** invented (catching-family check in
synthesized relations / harness oracles) · **A** accepted/screened-in (family check
built into a live harness) · **F** fired on patched · **K** kept by judge (leg TP).
Source: each leg's `result.jsonl` (`accepted_trigger_details`, `latent_oracles`,
`relations_not_implemented`, `crashed_on_patch`) + the run `summary.md` outcome. No
`trace.md` full reads needed — the oracle-id/relation lists in `result.jsonl` carry the
family signal. Dir map: pool/night `_o` overfit legs (night20 dirs 01–14).

| Leg (catching family) | pool30 | poolA | poolB | night20 | Classification |
|---|---|---|---|---|---|
| Math-2 (support-bound/nonnegative) | A | K | K | K | RELIABLE (3/4) |
| Lang-41 (Class-vs-String sibling-agree) | K | A | K | K | RELIABLE (3/4) |
| Lang-50 (locale/lifted-format) | K | K | K | K | RELIABLE (4/4) |
| Lang-60 (contains/readonly capacity) | K | A | K | A | COIN-FLIP (2/4) |
| Chart-7 (getMaxMiddleIndex/time-period) | A | A | K | K | COIN-FLIP (2/4) |
| Closure-92 (provide/parent-before-child order) | K | A | A | A | COIN-FLIP (1/4) |
| Chart-19 (rangeAxisIndex-null indep. of receiver) | A | A | A | A | INVENTS-BUT-NEVER-FIRES |
| Closure-38 (printer whitespace/format-exact) | A | A | A | A | INVENTS-BUT-NEVER-FIRES |
| Lang-63 (borrow-path date months observable) | A | A | A | A | INVENTS-BUT-NEVER-FIRES |
| Math-68 (trusted regression params/RMS) | K | A | K | K | RELIABLE (3/4) |
| Math-73 (nonbracketing @throws) | K | K | A | K | RELIABLE (3/4) |
| Math-74 (integrate end-time / evals-count) | A | A | K | K | COIN-FLIP (2/4) |
| Math-82 (LP feasibility/constraint) | K | K | K | K | RELIABLE (4/4) |
| Math-104 (regularizedGamma exact-tolerance) | A | A | A | A | INVENTS-BUT-NEVER-FIRES |

**Two structural facts hold in all 56 cells.** (1) *Invention is never the bottleneck.*
The known catching family was invented **and** screened into a live harness (stage A) in
**every leg × every roll** — even the four never-caught legs. No cell stalls at I-only or
below; `NEVER-INVENTS` is empty. (2) *The judge never kills a fired overfit trigger* —
zero `crashed=True + FN` rows across the four runs — so `INVENTS-FIRES-JUDGE-KILLS` is
also empty, and F⟺K for these legs. The only axis that separates TP from FN is **whether
the accepted check fires on the patched build**, and in every TP the firing oracle is a
**lifted/seed test-derived** oracle (`lifted-*`, `seed-*`, `testMath1021-*`, `math288-*`,
`jira-lang-281`, `lang295-contains`, `lifted-format3-locale`, …) — i.e. structural luck
off the known failing test. The *freely-invented general* catching-family relations
(`mean-formula`, `maxMiddleIndex_matches_*`, `rangeAxisIndex-null-rejected-independent-of-
plot-state`, `feasibility-constraints`, `period-months-exact-added-months`) are almost
always **accepted-but-latent** — they never fire. Classification counts: RELIABLE 6,
COIN-FLIP 4, INVENTS-BUT-NEVER-FIRES 4, NEVER-INVENTS 0, INVENTS-FIRES-JUDGE-KILLS 0.

**CORRECTION (2026-07-26, main-session audit — two claims above are wrong; the traces
refute them):**

1. **INVENTS-FIRES-JUDGE-KILLS is NOT empty.** `crashed_on_patch` in `result.jsonl` is a
   POST-JUDGE field, so judge-killed firings are invisible to the tabulation's method
   ("no trace.md reads needed" is exactly the shortcut that hid them). poolA Lang-60:
   `[oracle:contains-readonly-capacity]` FIRED on the patched build
   (`beforeCapacity=32 afterCapacity=0`, trace event [30]) and the judge killed it with
   the lazy-compaction hypothetical (VERDICT: UNSOUND, ~line 4769) — the exact "pure
   drift" kill Retro #3 triaged. Retro #3 also records DUTY:NO dismissal events on
   poolA Math-68 and Chart-19. So those poolA cells are fired-and-killed, not "A", and
   "the judge never kills a fired overfit trigger" is false for poolA; what IS true is
   that no kill has been observed since the family-duty defusals (poolB, night20) —
   i.e. the defusals are what closed this loss mode, and it stays closed only while
   they hold. Rule going forward: the F-vs-K distinction MUST be read from trace
   events, never from post-judge jsonl fields.
2. **"Every TP fired via a lifted/seed oracle" is false.** pool30 Lang-41's kept TP
   fired a freely-invented overload-agreement RELATION via the P3.2 replay path
   (0/20000 on buggy → 4259/20000 on patched; trace ~line 2630), consistent with the
   commit-audit's "P3.2 replay is the biggest recall mechanism". Invented general
   relations do fire and convict; they under-fire, but "almost always latent"
   overstates it.

What survives the correction: NEVER-INVENTS = 0 stands; the night20 no-new-frontier
finding stands; fire-capability remains the DOMINANT bottleneck and the item-4 verdict
is untouched. But the residual loss modes are two, not one: (a) accepted checks that
cannot fire (dominant, items 4/5/8), and (b) judge drift on fired checks — historically
real as recently as poolA, currently suppressed by the defusals, worth one standing
guard (the verdict-lint idea in the candidate ledger) rather than "not a factor".

**Key question — did night20 (width 7 + gate + cycle-3b directives) invent catching
shapes on legs the three prior rolls didn't?** No. There is **no leg where all three
priors were FN and night20 was TP**: every night20 catch (Math-2, Lang-41, Lang-50,
Chart-7, Math-68, Math-73, Math-74, Math-82) was also caught by ≥1 prior roll, and each
fired through the *same lifted/seed test oracle* the priors used — not a newly-invented
general relation. Width-7 demonstrably *did* raise invention **breadth** (synth_survivors
10–14 vs 5–6; richer `latent`/`not_impl` lists), and on **Chart-19** it uniquely invented
and screened-in the sharpest family match yet — `rangeAxisIndex-null-rejected-independent-
of-plot-state` / `-on-fresh-plot` (the documented "rejection-independent-of-receiver-
state" shape), where priors only had a plain null-rejection — **but it stayed latent and
never fired** (exactly the case item-4's fire-capability replay is built to probe). On the
other three never-caught legs night20 did **not** advance the frontier: on **Lang-63**
poolB reached A on the borrow-path observable (`period-months-exact-added-months`) while
night20's equivalent (`period_months_across_year_end_is_nine`) fell to
`relations_not_implemented`; on **Math-104** pool30/poolA screened-in a gamma exact-value
oracle (`gamma-positive-positive`) that night20 did not. Verdict: **width/diversity moved
invention, not fires.** The instrument says the next lever is fire-capability (item 4) and
budget/state-variation (items 5, 8), not more invention.

### NEXT UP (2026-07-26, post items 1–4) — execution order for the working agent

Item 5 is DEAD by its own gate (item 4: check-shape, not reach). The live sequence:

1. **Extend the item-4 fire-capability replay to Closure-38, Lang-63, Math-104**
   (offline, same method as Chart-19: accepted checks vs overfit build at the
   certified discriminating inputs). Output: does one structure-from-data fix cover
   all four never-fire legs, or do they split into classes (exact-tolerance and
   formatting-compare shapes may differ)? Record per-leg verdicts here.
2. **Generation-side fix licensed by item 4: fuzz-driven structural state.** Stated
   generally — receiver/container STRUCTURE (install indices, gaps, sizes, element
   placement) must be drawn from `data`, not compile-time constants; labels-from-data
   alone is cosmetic. Mechanical tooth per the meta-rule: screen-side lint — a
   rejection/state-family check whose receiver construction consumes zero fuzz bytes
   is flagged/demoted, never silently accepted. Fixtures from the night20 harness
   sources. Scope set by step 1's class split.
   **DONE (2026-07-28).** Part A: `STANDING STRATEGY — STRUCTURE FROM DATA` in
   relation_synth._INSTRUCTIONS, next to REJECTION INDEPENDENCE (dataset-neutral,
   asserted by test). Part B: `java_source.constant_receiver_state(check)` — flags
   only when the property is receiver-state-dependent AND zero consume* value
   reaches an index/count/loop bound/construction-gating branch; wired into
   `screen_relations` as a DEMOTION (`screen_demotion` suffix appended to
   `screen_note` by `_set_note`), never a drop. Archive-wide 5.3% of checks
   (161/3047), concentrated in the container/rejection legs; both ACCEPTED
   Chart-19 checks flag, the same leg's `setRangeAxis(consumeInt(0,3), …)`
   candidate does not. tests/test_structure_from_data.py (19, offline).
3. **Item 6 rerun of night20.cases at HEAD.** Pre-committed interpretation: (a) gate
   valid iff zero vacuous rejections in trace events (PASS/reject activity logged);
   (b) counts as the width-7 paired read; (c) frontier attribution — any firing on
   Chart-19/Closure-38/Lang-63/Math-104 is attributable to step 2, since nothing ever
   fired there in four rolls; pooled F1 is secondary to per-leg fire/invention rates.
4. **Item 7 milestone (paired 30-leg) only if step 3 moves the fire frontier.**
   Otherwise it re-measures the known band at ~7M tokens for no decision.

Not bundled: the judge verdict-lint (see the item-3 CORRECTION — drift is suppressed
by the defusals, not gone) stays queued as a standalone standing guard. fresh12
remains gated on the user's explicit go.

### ⚠️ STEP-1: TWO CONTRADICTORY ANALYSES BELOW — ADJUDICATION REQUIRED BEFORE ANY SPEC

The two same-titled sections below were committed 40 minutes apart (3c01129 = analysis A,
3bf1aa5 = analysis B) from the SAME night20 traces and reach OPPOSITE conclusions on the
decisive question for Closure-38 and Lang-63:
- **Analysis A (second section below):** the judge dismissals were DEFENSIBLE — the checks
  assert under-specified dimensions (unpinned whitespace; a seed answer generalized to
  inputs with no independent oracle — the invented-contract/G5 shape). Fix is
  ORACLE-side (independent correct-value oracle; contract-pinned observable) or accept.
- **Analysis B (first section below):** the same dismissals were JUDGE DRIFT killing
  genuine catches. Fix is JUDGE-side (diff-class fact + pinned-domain fence).
Neither is adopted. This is analysis-roll variance — the retro-#3 lesson applied to our
own tooling — and the sampled Lang-63 dismissal text currently leans A (explicit
invented-generalization wording; at the firing input endDay 1 < startDay 31 forces a
borrow, so actual=08 may simply be CORRECT).

**Adjudication protocol (offline, minutes per leg, BLOCKING):** for each dismissed
firing input, run the DEVELOPER FIX at that exact input (offline diagnosis only — never
a pipeline input):
1. Dev fix ALSO violates the check → check unsound, judge was right → oracle-side work
   or accept-as-hard (analysis A wins for that leg).
2. Dev fix satisfies the check, only the overfit violates → the check discriminates;
   then compute mechanically whether the divergence is in the failing test's own diff
   class. If yes → genuine judge-drift kill, judge-side fact fix licensed (analysis B
   wins for that leg).
Per-leg answers may split. Record verdicts here; only then spec the winning fix. The
GO previously discussed in chat is WITHDRAWN pending this adjudication.

### Step-1 result: fire-capability of the 3 other dead legs (2026-07-26) — ANALYSIS B (3bf1aa5: judge-over-dismissal reading)

Method note: derived from the night20 traces directly (they already contain the
patched-build firings + judge verdicts); no VM compile needed. Two probe agents
died on API connection errors mid-run; the analysis was completed inline by grep.

**The premise "these legs never fire" is FALSE for all three.** In night20, each
invented a check that FIRED on the patched build with the correct discriminating
value — and each was then DISMISSED by the judge. So Chart-19 (item 4) is the ONLY
structure-constant / never-fire leg. The other three are a different problem: the
judge over-dismisses a genuine firing via a hypothetical-correctness story. The
four dead legs split into THREE classes, and structure-from-data (step 2) fixes
only ONE of them:

| leg | fired on patched? | verdict cause | class |
|---|---|---|---|
| Chart-19 | NO (discriminating state is a compile-time constant) | never reached judge | **STRUCTURE-CONSTANT** → step-2 structure-from-data |
| Closure-38 | YES (`x-0` vs `x-0.0`, the exact whitespace/format bug; 12 UNSOUND) | judge: "a correct CodePrinter could legally print `x-0` as the equivalent `x-0.0`" — format-freedom hypothetical | **JUDGE-OVER-DISMISSAL** |
| Lang-63 | YES (`expected=09 actual=08`, the borrow bug; 6 UNSOUND) | judge: "formatPeriod is timezone/DST-sensitive, a correct impl could differ" — out-of-domain hypothetical | **JUDGE-OVER-DISMISSAL** |
| Math-104 | YES (P vs 1−Q differ at ~8e-10; 10 UNSOUND) | judge: "iterative/approximate, documented epsilon as loose as 1e-6, so a correct impl may differ" | **SUB-TOLERANCE** (the dismissal is arguably CORRECT — the check IS unsound at that gap) |

**Consequences for the sequence:**
- **Step 2 (structure-from-data) covers Chart-19 ONLY.** Do not expect it to move
  Closure-38/Lang-63/Math-104. Its scope is one leg; still worth doing, but sized
  accordingly.
- **Closure-38 + Lang-63 are the real recall prize, and they are a JUDGE/EVIDENCE
  problem, not a generation problem.** The checks already fire with the right
  answer. The fix is the diff-class evidence (G4: the divergence IS in the failing
  test's own diff class — whitespace for Closure-38) + trust-domain fencing (G2:
  Lang-63's check ran at a fixed timezone; the DST hypothetical is outside the
  check's own input domain). This is the hypothetical-correctness enemy the
  campaign has fought since day one — "suppressed by defusals, not gone" (item-3
  CORRECTION), and here it is un-suppressed on two legs. This should likely
  PRE-EMPT step 2 in priority: two legs vs one, and it's the higher-value class.
- **Math-104 is likely a genuine trustworthy-recall discount** (sub-floor numeric,
  the check's own tolerance can't distinguish 8e-10) — do not chase without a
  sharper discriminating input; accept as a hard leg.

**Revised recommendation:** before step 2, spec a judge-side diff-class/domain-fence
fix for the JUDGE-OVER-DISMISSAL class (Closure-38 + Lang-63) — bigger prize, and it
re-opens the oldest open wound with concrete new fixtures (two firing-but-dismissed
legs). Structure-from-data (Chart-19) and the sub-tolerance accounting (Math-104)
follow. All three still gate the night20 rerun / milestone as before.

### Step-1 result: fire-capability of the 3 other dead legs (2026-07-26) — ANALYSIS A (3c01129: dismissals-defensible reading)

Method = item-4 Chart-19 template applied to night20 (`night20_20260725_155442`),
offline/static: read each leg's ACCEPTED checks + the `replay-on-patched` and
`patched-fuzz` events + the judge verdicts in trace.md. **No VM compile needed** — the
traces already record every fire on the overfit build and the judge's soundness ruling,
which is decisive. The headline finding overturns the prior read: on all three legs the
discriminating check REACHED AND FIRED on the overfit at (or near) the certified input —
the miss is NOT reach and NOT structure-from-constants. It is a JUDGE / oracle-soundness
dismissal, and each leg's sub-mechanism is DISTINCT.

- **Closure-38 — OTHER-SHAPE (judge formatting-latitude dismissal). NOT structure-constant.**
  Relations `minus_positive_zero_has_no_forced_space` ("x-0") and
  `minus_positive_integer_has_no_forced_space` ("x-"+n, n from `consumeInt(1,9)`) survived
  screening as silent-on-buggy tripwires and, on `replay-on-patched` [74]/[75], FIRED
  (deterministic 2/2, fuzzed 20000/20000) — the overfit emits the spurious "x- 0"/"x- 5".
  Harnesses also fired on patched ("do- -0", "public - -0"). Every fire was ruled UNSOUND:
  keyword inputs ("do","public") give parse errors where a correct printer may return "",
  and for valid "x-"+n the judge held the contract only promises "compact javascript code",
  so "x- 1"/"x - 1" is a legal rendering — whitespace after '-' is not contract-pinned.
  Prior read said "compare-time whitespace erasure" — WRONG: harness compares used
  `replaceAll("\\s+"," ").trim()` (single-space, signal preserved) and the raw relations used
  exact equals; the signal reached the check and fired. Erasure is at the JUDGE, not compare.
  A structure-from-data generation fix does nothing here (checks already fire).

- **Lang-63 — OTHER-SHAPE (off-seed oracle-knowledge). NOT structure-constant.**
  Harnesses drew borrow dates FROM fuzz data (fire evidence: `expected=09 actual=08
  startYear=1900 endMonthDelta=9 endDay=1` — endDay 1 < startDay 31 = a real field borrow)
  and FIRED on patched [48 attempt_001, 50 attempt_003]. So reach + data-driven structure
  are already present — this DISPROVES the prior "possibly STRUCTURE-CONSTANT" guess. Every
  fire ruled UNSOUND: the seed answer ("09" for Dec-31→Oct-6) was generalized to other
  borrow dates whose correct months value the harness cannot independently compute, and the
  judge showed a correct day-borrowing impl legitimately returns "08" there. At the ONE
  input with a trusted answer (the seed) the plausible overfit passes, so the sound
  seed-lifted oracle stays quiet. Missing ingredient: an independent oracle for the correct
  value at borrow inputs off the seed — not more structure variation.

- **Math-104 — OTHER-SHAPE (rounding-floor tolerance). NOT structure-constant.**
  P+Q=1 complement checks FIRED on patched (attempt_001 `[oracle:default-complement]`:
  P=0.5150504305 vs 1-Q=0.5150504297, ~8.2e-10, at a=x=78.08; replay [64]/[70] FIRED). The
  overfit (`Math.sqrt(an)` vs `Math.abs(an)` in the convergence test) diverges by ~6e-11–8e-10.
  Every complement fire ruled UNSOUND under the judge's explicit ROUNDING FLOOR (~1e-9): a
  correct iterative impl reproduces that mismatch, so any tolerance tight enough to catch it
  (1e-12) is unsound. The pinned trusted-value checks at (1,1) that ARE tight-and-sound
  (tol 1e-13/1e-14) went QUIET — the overfit agrees at (1,1); the divergence lives at other
  inputs where no exact answer is known. Confirms the prior "tolerance" guess.

**Class split — answer to the key question: NO, one structure-from-data fix does NOT cover
all four.** It covers exactly ONE — Chart-19 (STRUCTURE-CONSTANT: the null×sparse-hole
discriminator is a compile-time constant, the check never reaches the divergence). The other
three are each OTHER-SHAPE and, more sharply, all JUDGE-DISMISSAL cases: the check already
reaches and fires on the overfit, and the soundness gate correctly kills it because the
divergence lives in an UNDER-SPECIFIED dimension the contract doesn't pin — formatting
whitespace (Closure-38), an off-seed value with no independent oracle (Lang-63), sub-1e-9
numerical error (Math-104). These are three DISTINCT sub-mechanisms, none of them reach- or
structure-limited.

**Consequence for step 2: it is NOT one fix — it is one fix plus three separate problems.**
The step-2 structure-from-data generation fix is licensed for Chart-19 ONLY. The other three
are not addressable generation-side (their checks fire already); they need oracle/judge-side
work and each differs: Closure-38 wants a content-only canonical comparison the judge accepts
as sound (or a contract-pinned separator observable); Lang-63 wants an independent
correct-value oracle for arbitrary borrow dates (recompute months two ways); Math-104 wants a
sound way to expose a >1e-9 observable (or acceptance that the ~1e-10 overfit is below any
sound tolerance and is genuinely uncatchable by a value oracle). Do NOT bundle them under
step 2's structure-from-data banner.

### Step-1 ADJUDICATION result (dev-fix replay, 2026-07-26)

BLOCKING adjudication executed per the protocol above: for each dismissed night20
firing, the defects4j DEVELOPER FIX (Closure-38f, Lang-63f, Math-104f) was checked out,
built, and driven at the EXACT dismissed-firing input on the VM (`/home/code/scratch/adjud/`,
since removed). The dev-fix observed value is the deciding evidence.

| leg | dismissed check (fired on overfit) | firing input | dev-fix value at that input | overfit value | A or B | why |
|---|---|---|---|---|---|---|
| Closure-38 | `minus_positive_zero_has_no_forced_space` ("x-0"→"x-0") and `minus_positive_integer_has_no_forced_space` ("x-"+n→"x-"+n); judge [83]/[84]: "a correct printer could print `x-0` as `x-0.0`" / "compact JS could print `x- 1`" | compact-print `x-0`, `x-5`, `x-9` | `x-0`, `x-5`, `x-9` — **no space; SATISFIES the check** | overfit emits spurious `x- 0` / `x- 5` (fired 2/2, 20000/20000) | **B** | Dev fix produces exactly the check's expected value; only the overfit inserts the space. The divergence is whitespace after `-`, which IS the failing test's own diff class (`testMinusNegativeZero`: `x-[ ]-0.0` vs `x-[]-0.0`, a single space). Judge's format-freedom hypothetical is empirically false for the developer's own correct printer. Judge-drift kill of a genuine catch. |
| Lang-63 | `constructed-month-answer` (formatPeriod "MM", expected 09) | Dec-31-1900 00:00 → Sep-1-1901 00:00 (MONTH idx 8, endDay 1; default TZ) | **`08` — VIOLATES the check** (dev fix returns the same value the overfit did) | `08` (fired, attempt_001/003) | **A** | Dev fix returns `08` at this input: end-day 1 < start-day 31 forces a day-borrow, so a correct impl legitimately yields 8 months, not 9. Seed (Dec-31-2005→Oct-6-2006) correctly returns `09`. The check's expected `09` is an unsound generalization of `monthDelta` that ignores the day-borrow. Check UNSOUND, judge was RIGHT. |
| Math-104 | `default-complement` (P(a,x) vs 1−Q(a,x), tol 1e-12) | a=x=78.08 | P=0.5150504305333261, 1−Q=0.5150504305333193, **diff=6.77e-15 ≪ 1e-12 — SATISFIES the check** (also a=x=51.21: diff=1.04e-14) | P=…472 vs 1−Q=…297114178, diff=8.2e-10 (fired) | **B** | Dev fix's P and Q agree to ~7e-15, far below the 1e-12 tolerance — the check does NOT fire on the correct build; only the overfit's broken-Q convergence (8.2e-10) trips it. This OVERTURNS both prior "sub-tolerance/rounding-floor" readings: the judge's "a correct impl using DEFAULT_EPSILON≈1e-9 gives 2.6e-10/8.2e-10 mismatch" counterexample is empirically false (dev fix gives 1e-14). The generic 1e-9 rounding-floor doctrine was mis-applied to a function that actually converges to ~1e-14; the 1e-12 tolerance sits correctly between the correct floor and the defect signal. Judge-drift kill. *Caveat:* soundness for EVERY correct impl is not contractually guaranteed (DEFAULT_EPSILON permits looser impls), so this is the most borderline B; the fix should pin the tolerance in the documented ~1e-11–1e-12 band, which the check already does. |

**Split verdict: B, A, B.** Corrected fix direction per leg:
- **Closure-38 → JUDGE-side (Analysis B).** The check discriminates; spec the diff-class
  fact (G4: the divergence is whitespace, the failing test's own diff class) so the judge
  stops dismissing it via format-freedom hypotheticals. Genuine recall prize.
- **Lang-63 → ORACLE-side / accept-as-hard (Analysis A).** The check is genuinely unsound
  (dev fix returns `08`). Do NOT license a judge-side fact fix here. If pursued, needs an
  independent correct-value oracle that computes the month field WITH day-borrowing (two
  ways) rather than the seed-lifted `monthDelta` literal. Otherwise accept as hard.
- **Math-104 → judge-drift CONFIRMED but PARKED (design-open). ⚠️ FIREWALL WARNING.**
  The check discriminates on the dev fix (7e-15 vs the overfit's 8.2e-10) — but that
  7e-15 figure comes FROM the dev fix and is adjudication evidence ONLY. At detection
  time no correct-build mismatch is EVER available; any floor "paired with the observed
  correct-build mismatch" would smuggle dev-fix knowledge into a verdict — a firewall
  breach. A sound detection-time source for a tighter floor (e.g. the buggy build's own
  convergence at the firing input, if the defect provably doesn't touch it there) is an
  OPEN DESIGN QUESTION. Do not build until a firewall-clean design is written and
  reviewed here. Until then this leg stays in the hard column.

This **revises ANALYSIS A's Math-104 conclusion (was: uncatchable sub-tolerance) and
ANALYSIS B's Closure-38+Lang-63 pairing (Lang-63 is A, not B).** Two of three dismissals
were judge-drift (Closure-38, Math-104); one (Lang-63) was a correct kill of an unsound check.

### Population inventory result (2026-07-26) → the cycle-5 package

Full data: `docs/judge-verdict-inventory-2026-07-26.md` (228 judge verdicts on fired
checks across pool30/poolA/poolB/night20/width5; the rows are the replay fixture
population). Headline findings that supersede the single-leg "diff-class fact" idea:

1. **The FN mechanism is the judge, almost always.** Of 23 FN overfit legs with judged
   firings, 22 had EVERY verdict UNSOUND. Recall is not lost to missing firings; it is
   lost at the verdict. (Many kills are CORRECT — bad checks are real — but the kill
   step is where the decision happens.)
2. **The cleanest drift-kill signature is mechanical and recurring:** silent-on-buggy
   ~0/20k + deterministic 2/2 on the failing test's own literals + ~100% patched fire
   rate, killed by an uncorroborated "a correct implementation could…" hypothetical
   (inventory §c rows 1–4: Closure-38 ×3 across two runs, Lang-60 width5). In 4 of the
   6 UNSOUND verdicts carrying this profile the hypothetical overrode it; 1 of 6 was a
   justified kill (a genuine `!=` check bug), so the rule must require a positive
   shown-contract/broken-check citation, not auto-keep.
3. **Two of our own facts miscoach the judge in exactly that profile:** the [fire-rate
   fact]'s "100% indicts the check" wording (100% on PATCHED with ~0% on buggy is
   maximal discrimination, not indiscriminateness) and the [trigger-tier fact]'s
   dismiss-pushing wording. Plus a denominator bug (2997/1000 — normalize per input).
4. **The precision failure is symmetric:** all 26 SOUND-on-correct verdicts sit in FP
   legs, and ~8 of them kept a firing DESPITE fires-on-buggy / IDENTICAL-ON-BOTH facts
   — trusted-lift provenance overriding mechanical facts, the exact inversion of the
   "mechanical facts outrank provenance" rule; one (night20 Math-30
   canonical-parity-closed-form) was kept despite the cycle-2d TERMINAL identical fact,
   proving the terminal rule is not mechanically enforced.
5. **Step-4b is violated in the wild:** Lang-63 day-shift killed via a DST hypothetical
   that cannot produce the observed 2-day delta; the pinned-UTC Lang-63 relations killed
   via calendar latitude their own `UTC_TIME_ZONE` fencing excludes. The Lang-50 locale
   family is the positive control (pins honored 14/15).
6. **Verdict variance at check granularity confirmed:** the same chi² relation judged
   SOUND in three runs and UNSOUND in one on the same evidence shape.

**The cycle-5 package (all population-evidenced, all mechanical):**
- **5A — fact repairs (cheapest, first):** two-sided fire-rate wording (distinguish
  "high on both = indiscriminate" from "≈0 buggy / high patched = patch-introduced
  discrimination"), per-input denominator normalization, trigger-tier wording
  neutralized to symmetric.
- **5B — recall-side dismissal lint (step-4b enforcement, mechanical where possible):**
  (i) pinned-environment fact — syntactically extract what the check source pins
  (UTC_TIME_ZONE, Locale.setDefault, fixed seeds); a dismissal whose counterexample
  varies a pinned parameter is void → verdict re-asked with the fact stated;
  (ii) under the §c drift-kill signature, UNSOUND requires citing a SHOWN contract or a
  demonstrable check bug — an uncited "could" hypothetical is inadmissible there.
- **5C — precision-side enforcement (the mirror):** identical-is-terminal enforced
  MECHANICALLY at every judge site (route through the Spec-J family-duty ladder; no
  discretionary keep on IDENT), and a SOUND keep on a firing carrying
  fires-on-buggy/IDENT facts is void unless the family-duty question answers YES —
  provenance alone cannot override a mechanical fact.
- **5D — validation gate:** two-sided offline verifier_replay with fixtures drawn from
  the inventory: §c drift-kills 1–4 must flip to kept; the justified kill (row 6,
  Lang-50 `!=` bug) must stay dead; the 26 FP keeps must strictly decrease (the ~8
  provenance-override keeps are the direct targets); the 44 correct dismissals on
  correct legs must not flip. Ship nothing that fails any leg of this.
- **Small adjudication rider:** dev-fix replay (offline) of the two pinned-UTC Lang-63
  relations at their firing inputs — if the dev fix satisfies them, they join the
  drift-kill fixture set; if it violates them, they stay correctly dead.
- Chart-19 structure-from-data (step 2) unchanged and disjoint; the night20 rerun and
  milestone sequence unchanged. The earlier "diff-class fact" framing is RETIRED in
  favor of 5A–5C: Closure-38's rescue comes from the drift-kill-signature rule, not
  from whitespace classification.

### Dials and re-validation (only as licensed by 3–4)

5. **Fuzz-budget raise for accepted checks — only if step 4 says "reach".** And it must
   be measured two-sided: the same raised budget runs on correct legs, where every
   unsound check gets more chances to fire. Measurement: the 6-leg trap set (night20's
   correct legs) + the 5 catch-legs, same config twice (paired). No blanket adoption on
   a recall read alone.
6. **Re-validate the novelty gate now that it actually functions.** After step 1 the
   gate has effectively never been tested. Cheapest honest test: rerun the night20
   cases file once (same width, fixed gate) — this also serves as night20's pair, so
   one run buys both the gate check and the paired read of width-7.
   *Interpretation rule:* per-leg invention rates from step 3, not the pooled F1, are
   the primary readout.

### The milestone measurement (after 1–6)

7. **Paired pool on the 30-leg burned set** with whatever config survives steps 5–6.
   Two rolls, identical config, report both + mean, per the paired rule. This is the
   number that decides whether the width/diversity direction moved anything. Target to
   beat honestly: paired mean F1 ≈ 0.49 (poolA/B); catches band 4–8/14.

### Bigger changes queued behind the milestone (specs in the candidate ledger above)

8. **P4.1 did-nothing-patch detector, offline false-flag measurement** over the 142
   certified-correct patches. Unblocked since 2026-07-21; targets the never-invents /
   never-fires legs (Chart-19, Closure-38, Lang-63, Math-104 stayed missed in all 4
   rolls — width demonstrably does not reach them). Escalation-trigger only at first.
9. **Focused-synthesis re-adjudication** (the void single-roll kill): once step 3's
   tabulation exists, measure the flag's effect on invention rate per leg (or run its
   own paired mini-pool). If it raises invention on coin-flip legs without raising
   junk on trap legs, re-enable.
10. **Cycle-4b conviction confirmation + silent-leg re-roll** (accusation and
    generation lotteries; per-accusation / per-silent-leg cost). Confirm the
    no-pooling reading with the user before building.
11. **Repair-instead-of-drop for mechanically-dead relations** (R1 precedent) and the
    **per-observable sharpening of Spec N's convergence gate** — small, precedented,
    ride along with whichever cycle touches those files next.

Pre-commitment: write the measurement design (what's compared, how many rolls, success
criterion) into this section BEFORE building items 5, 6, 8, 9. The exam (fresh12)
launches only on the user's word, after a paired milestone score they accept.

## 2026-07-29 — PRE-COMMITMENT for the 30-leg measurement (written BEFORE launch)

Binding reading rules, fixed in advance so nothing is re-litigated after the numbers land.

**Config (Decision 1): width 5, not 7.** The baseline to beat — paired mean F1 ≈ 0.49 from the
July-25 poolA/poolB pair — was measured at width 5, so 5 makes the comparison clean and roughly
halves the cost (~7M vs ~13M). *Caveat recorded in advance:* Chart-19's two-roll win happened at
width 7. Its mechanism (fuzz-derived container structure) does not depend on harness count, but
**if Chart-19 misses at width 5 that is a config note, not a mechanism failure** — and it does not
by itself retract the two-roll result.

**Protocol (Decision 2):**
1. **Both rolls at the SAME commit, zero code changes between them.** If roll 1 looks bad, it
   still stands — no peeking and fixing. Any change invalidates the pair and restarts it.
2. **Report both scores plus the mean, verbatim** from the summary line. No arithmetic on
   remembered baselines (four counts have drifted that way this week).
3. **Compare per-leg against the archived pool tables**, not just totals — the totals sit inside
   a measured ±2-leg variance band (5 of 10 untouched replay rows flip between identical draws).
4. **Named expected residuals — pre-declared so they are not re-argued afterwards:**
   - **Math-30** false accusation: EXPECTED to persist. Characterised as a judging residual
     (`docs/replay/smoke30b_analysis.md`); the rate reaches the judge and it convicts anyway.
     Not evidence of a plumbing regression.
   - **Closure-38, Math-104**: EXPECTED misses (hard column — unpinned formatting; sub-noise
     precision floor).
   - **Lang-60, Lang-63**: known coin-flips. Lang-63 is 1-for-2 and gets a third data point from
     today's pre-flight smoke.
   - **Math-73-c**: still convicting as of night20c; not yet characterised.
5. **Success is not a single number.** The measurement's value is that it is *attributable*: all
   six cycle-6 mechanisms now emit permanent considered/decided events, so any movement can be
   traced to a mechanism or explicitly marked unexplained.

**Pre-flight gates (must pass before launch):** the two never-observed mechanisms confirmed live —
the diverted-replay fix on Chart-26-correct, and 6C's different-values protection on Lang-63.

---

## 2026-07-29 — Lang-63 three-roll decomposition (pre-milestone diagnosis, traces only)

Question: why is Lang-63 1-for-3? Read all three rolls' traces (night20b won / night20c
missed / preflight2 smoke missed). The two misses have DIFFERENT causes:

| roll | month-family check invented? | construction | fired on patched? | outcome |
|---|---|---|---|---|
| night20b | yes (`constructed-known-months`) | clone-and-shift, fuzz deltas, independent expected value | YES (actual=-9) | TP |
| night20c | yes (`constructed-months` + 2 siblings, 7 harnesses) | clone-and-shift, fuzz deltas | NO — armed but silent all run | FN |
| preflight2 smoke | NO — zero month-family ids in 5 harnesses | absolute set() only | nothing fired | FN |

So Lang-63 is not simply "reach-limited": it loses to BOTH lotteries stacked —
invention (the family isn't always proposed) AND reach (when armed, firing depends on
drawing a discriminating borrow combination). Residuals entry for the milestone:
**expected miss in both rolls; catch = pleasant surprise, not signal.** Cycle-7 shape:
this needs either (a) generation-side family persistence per leg (careful: within-run
only, no cross-run pooling) or (b) seeded borrow-shaped date deltas in the corpus
(mechanical, general: end-day < start-day combinations) — NOT judge work; no verdict
was ever wrong here.

No pipeline changes made; diagnosis only, per the hard-stop rule.

---

## 2026-07-29 — CYCLE 7 PLAN (post-milestone; fresh12 deliberately parked by user)

Evidence base: final30A/B pair (mean F1 0.685 vs 0.49; recall moved, precision did not),
the correction commit c300a8e (rule-diversity claim retracted; the 3-way `None`
conflation found), the per-leg pair table, and all prior ledgers. Ordering principle
unchanged: observability → free forensics → mechanical levers gated by fixture replay →
one measurement. No judge-prompt tuning (measured dead), no fresh12.

### 7A — Observability completion (BLOCKS EVERYTHING; ~a day; zero risk)
Split `indiscriminate_buggy_rate`'s `None` into its three real cases in trace events
(never-measured / measured-below-bar-healthy / parse-failed). Then a one-pass audit for
any OTHER readout keyed on absent-or-ambiguous text (this is defect class #6 now: same
shape six times). Done when: every gate decision in a trace names which of its input
facts existed and what they said.

### 7B — Pair forensics (free; traces + live events already on disk; each output
feeds exactly one lever below)
1. **Chronic-FP event chains** (Closure-62 ×2, Math-30 ×2, Math-65 ×2): for each
   surviving accusation, walk the now-live event chain — measured rates, duty answers,
   J-ladder rungs consulted — and name the one place the intended machinery stopped.
   Watch specifically: did the setup-divergence rung (J-ladder a) ever run for
   Closure-62? Its checks rebuild the test scenario and the real test passes — that
   rung was built for exactly this shape.
2. **New-FP alarms**: Math-39-c (roll A, historically ALWAYS clean), Lang-60-c and
   Math-73-c (roll B). Were any kept via a cycle-6 path (6C keep / 5B-inadmissible /
   citation re-ask)? A yes = first regression evidence against cycle-6 → that path
   gets a fixture row and a fix before anything else ships. A no = variance, recorded.
3. **Closure-38 roll-B catch autopsy**: what fired and what kept it? If the keep rests
   on unpinned-formatting latitude, it is a precision-first violation that happened to
   land on a fake — flag the check shape, do NOT celebrate it.
4. **Chart-19 width autopsy**: was the winning relation family proposed at -m 12 and
   starved, or never proposed? Distinguishes relation-budget (-m) from harness-count
   (-n) dependency. Feeds 7D-1.
5. **Roll-A recall losses** (Lang-60, Math-73-o): invention / reach / judging split,
   same method as the Lang-63 three-roll decomposition.

### 7C — Variance lever (mechanical, small, high leverage)
**Within-run verdict memoization**: identical (check identity, fact profile) inside one
leg gets ONE judge verdict, reused — never re-rolled. Fixture: the Math-65 leg where
the same relation got 2×SOUND and 2×UNSOUND in a single run. This is subtraction (fewer
coin flips), not judging harder; no cross-run state (pooling rule untouched). Gate:
228-fixture replay + the pair's new rows.

### 7D — Recall levers (each gated by its 7B output; all general-shaped)
1. **Relation-budget experiment** (cheap, decisive): the 5-catch-leg suite at -m 12 vs
   -m 16, invention-rate readout per leg (not pooled F1). If -m is what Chart-19 needs,
   raise the standard -m — that is a measured config change, not tuning.
2. **Silent-leg re-roll**: a leg whose patched-build run produced ZERO firings gets ONE
   extra generation round (bounded, within-run). Attacks the invention lottery at its
   cheapest point; expected cost ≈ 1 extra round × silent fraction.
3. **Structural corpus seeding**: general boundary-crossing shapes (end<start, empty,
   hole-at-k, size-1) as seed inputs — the Lang-63 reach half; statement-test general.

### 7E — Precision levers (STRICTLY gated by 7B-1 findings; Math-30 stays a named
residual unless 7B-1 shows its machinery stopped somewhere fixable)
Anticipated shapes (build only what 7B-1 evidences): un-stopped J-ladder rung for
Closure-62; a mechanically-computed implementation-definition fact for Math-65 (the
shown impl line IS the contract source the honest verdicts cited — deliver it as a
fact instead of hoping the judge reads it). The absence-argument gap stays parked.

### 7F — Measurement discipline
Fixture grows: add the pair's judged firings (with gold from the pair analyses) to the
replay population. Every 7C/7D/7E change: fixture replay + one-leg smoke with events
checked live, BEFORE any suite. Next paired 30-leg only after the full batch lands.
Success bar for cycle 7, pre-committed: paired mean > 0.685 with FP count < 5 in at
least one roll and no new-FP legs; stop-loss: 3 iterations per lever, then park.

## Cycle-7 pre-build diagnostics (both free, both design-changing)

**Vote precondition for within-run answer reuse — CLEAR.**
A diverse-lens ensemble exists (`relation_verifier.py`, `for i in range(self.votes)`,
strict-majority-UNSOUND to drop, ties fail open) but `RELATION_VERIFIER_VOTES`
defaults to 1 and no archived leg shows a multi-lens verdict. There is no
ensemble to collapse, so reuse is safe. Design constraint carried forward: key the
cache on the FULL prompt including the lens suffix, so that if voting is ever
switched on, different lenses remain different cache entries by construction.

**Placement audit — NEGATIVE, and that is the useful answer.**
Prompted by Math-65, where the decisive code line sat once at char 27,051 of a
59,830-char prompt. Audited all 230 archived judge prompts for where each computed
fact physically sits relative to the firing.

Result: every fact that is actually delivered goes into the `<evidence>` block,
which sits at a median of **15%** of the prompt — before the `<codebase_context>`
dump, adjacent to the firing. Placement of computed facts is correct and needs no
change.

The first cut of this audit appeared to show three fact types landing at 85% of
the prompt, after the dump. That was an artifact: the guidance boilerplate (which
follows the dump) mentions those tag names. Delivered-vs-boilerplate counts:
trigger-test lift 13 delivered / 183 boilerplate; differential 6 / 190;
buggy-replay 113 / 83. The 13 independently matches item 2a's count of delivered
lift notes.

Consequence for the Math-65 fix: it is a targeted addition, not a repair of a
systemic placement bug. The code line belongs in the code dump; what is missing is
a fact block that also states it. Duplicate it into `<evidence>`, leave the dump
untouched. Do NOT re-engineer the assembly order — it is already right.

Also observed, not yet investigated: 34 of 230 judge prompts carry no
`<evidence>` block at all. May simply mean no fact applied. Parked.

### Standing rule (adopted cycle 7): measure each change ALONE

Every change is measured on its own against the recorded cases before it ships,
even when it will be shipped bundled with others.

The case, in one example. Item 2a licensed two extractor fixes in a single
paragraph. Bundled they measure 2 right / 1 wrong — "net positive, ship".
Separated: fix (i) is 1 right / 0 wrong, fix (ii) is 0 right / 1 wrong. Fix (ii)
would have shipped hidden inside fix (i)'s win. Cost of separating: nothing.

### Named confusion to avoid: `gold` describes the CHECK, not the PATCH

`gold=SOUND` means the fired check is legitimate and the finding should be KEPT
(`score_replay.py`: "over-kill (gold=SOUND dropped)"). It does NOT mean the patch
is good — in fact a sound check on a fake patch is exactly a legitimate catch, so
gold=SOUND correlates with FAKE patches, not correct ones.

This was gotten backwards once already, in prose rather than in code: the 10
non-numeric rows were described as "all correct patches, so fixing can only help
precision", when verification showed all 10 are one fake patch
(`patch1-Lang-41-Arja-plausible_o`, label `overfitting`) whose findings should be
kept. The wrong reading licensed a fix that was then rejected on measurement.

Same family as the label bugs items 1a/1b fixed — a value read in the opposite
sense — but located in a write-up rather than a log line. Population descriptions
get verified against the fixture, not narrated from a remembered label.

## Item 3 (within-run answer reuse) — NOT BUILT. Premise not supported.

Measured before building, per the standing rule.

**In production, identical questions never recur.** Across the ENTIRE archive —
103 runs, 1,616 judge calls — byte-identical judge prompts repeat **0 times**. A
cache keyed on the prompt would never hit once. Restricting to the two paired
runs: 230 calls, 196 of them soundness questions, and the same fired assertion is
re-judged only 4 times (all pairs), never with an identical prompt, with 1 verdict
disagreement between them.

**Where the premise came from.** `verifier_replay.py:236` runs
`for rep in range(args.repeats)` (default 3) calling `adjudicate` with *identical*
arguments. That is the stability-measurement tool asking the same question N times
on purpose, and it is the only place identical prompts occur. Caching there would
not fix instability — it would erase the measurement, returning the first answer N
times and reporting perfect stability.

So the change is a no-op in production and destructive in the one place its
trigger condition is met.

**The variance it was meant to address is elsewhere.** Item 2b already located it:
Lang-60's correct patch had every pre-reviewer station identical across the two
rolls, then 0 judge calls in one and 2 in the other. The 8-of-30 flip rate comes
from *which alarms get raised*, not from the same alarm being judged twice.

Not built. Revisit only if a production run is ever observed re-issuing an
identical prompt.

## Pre-registered for the pair: repair-in-place is a width increase in disguise

Recorded BEFORE the paired measurement so that a precision dip, if one comes, is
attributed to the right cause instead of surprising us afterwards.

Repair-in-place is filed as a recall lever, but mechanically it raises effective
harness width. Roughly 4 rejections per case at a 36% full-clear rate is about
one extra accepted harness per case — **and that applies to correct patches
exactly as much as to fake ones.** Every additional alarm raised against a correct
patch is another chance to be wrongly accused. That is the same argument that was
used against blanket width raises, and it applies here whether or not the extra
harnesses are individually good.

Two things genuinely mitigate it, and neither makes it disappear:

* a repaired harness still passes every detector the gate applies, so it is not
  a lower-quality harness — it is a harness that was thrown away for a fixable
  structural defect;
* the precision machinery is stronger than when width was last raised (the
  revived dismissal rule, the disputed-computation fact, the cycle-6 gates now
  actually firing).

**Honest expectation: a recall gain with a possible precision tax.** The two-tier
bar already prices this — the PASS tier requires *zero accusations on historically
clean cases*, which is precisely the clause that fails if the extra alarms start
convicting good patches.

If the pair shows recall up and false accusations up, that is this effect, not a
regression in the precision work, and the response is to gate repair-in-place on
the fake-patch side only if that can be done without the pipeline knowing the
label — which it cannot. So the real response would be to accept the trade or
raise the repair's acceptance bar.

## Repair-in-place: offline validation complete

Two independent gates, both green.

**Detector clearance** (the project's own gate functions, over 235 archived
rejected harnesses recovered from the two paired runs):

    swallowed-alarm         65 present -> 65 cleared
    missing-alarm-id        39 present -> 27 cleared
    rethrow-without-cause    7 present ->  5 cleared
    boolean-swallow         77 present ->  0 (deferred)
  fully cleared: 84 of 235   regressions: 0

**Compilation** (javac against the real jazzer-api-0.22.1 jar on the VM, 96
original/repaired pairs): 0 repaired harnesses with more errors than their
original, 0 error strings appearing only after repair.

The compile gate found three defects that detector-clearance was structurally
incapable of seeing, all of which would otherwise have shipped into the pair:
a global str.replace that corrupted a literal in the wrong location; runtime-built
oracle IDs being tagged a second time; and a cause appended to an alarm
constructor that already had two arguments.

It also produced one false alarm of its own: 110 "incompatible types" errors that
came from a hand-written stub declaring FuzzerSecurityIssueLow as extending Error.
The real class extends RuntimeException. Lesson recorded because it recurred all
week: **when a measurement disagrees with the code, suspect the measurement's own
scaffolding first** — every wrong reading this cycle came from a proxy we built
(a stub, a regex, a field name), never from the code under test.

boolean-swallow stays deferred. The reason for deferring it (no compiler) is now
gone, but it is the riskiest transform and the pre-registered width-increase
concern applies to it more strongly than to the three shipped repairs. Revisit
after the pair, not before.

# ===========================================================================
# PAIR PRE-COMMITMENT — REFRESHED (supersedes the earlier expected-effects list)
# ===========================================================================

The shipping build changed shape substantially during cycle 7. Three of the
planned items died or deferred on measurement BEFORE shipping, which is the
process working, but it means the earlier pre-commitment described a build that
does not exist. This is the one that governs.

## What is actually in the build

**Shipped**
1. Extractor fix (i) — project-defined assertion helpers recognised. Revives the
   dismissal rule that had never once reached its dismissal branch.
2. Fail-loud field access + the `gold` rename to keep-finding / dismiss-finding.
3. The Math-65 disputed-computation fact — the code's own computation of a
   disputed quantity, duplicated beside the firing.
4. Repair-in-place for mechanically-diagnosed harness rejections, with a
   `harness-repair` trace marker on every repaired harness.
5. The item-1 trace-label splits (five rate states, alarm-already-discarded,
   the 6C two-way split).

**Dead on measurement, not built**
* Answer-reuse cache (item 3) — 0 byte-identical judge prompts in 1,616 calls
  across the whole archive; the only place they repeat is the stability tool,
  where caching would erase the measurement.
* Silent-case retry (item 5) — silence is the pipeline's most reliable signal
  (silent correct legs cleared 14/14); the retry would wake 14 clean legs into a
  56% accusation lottery to reach 2 fake ones.
* Seed shapes (item 6) — 7:1 leg exposure against, and the motivating case
  (Lang-63) had 4 alarms reviewed, so it was never starved of firings.

**Deferred with its measurement recorded**
* Non-numeric value comparison (fix ii) — a wash on the decisive instruction
  (2 correct / 1 wrong) with a recall-leaning side effect that would confound a
  precision-themed pair.

## Expected effects, stated before the run

**Precision movement** should come from the revived dismissal rule (aimed at
Closure-62) and the disputed-computation fact (aimed at Math-65). Plausible
outcome 5 -> 3 false accusations. **Math-30 stays** — it is the named accepted
residual, its mechanism identified (universal-property checks in a degenerate
regime at the int-overflow boundary), and we already refused to bend the
threshold for it.

**Recall movement** should come from repair-in-place, concentrated on the legs
that exhausted their attempt budget short of the harness target — 11 of 60 legs,
with **Chart-19's fake leg short in BOTH rolls** and first in line.

**Neither is verified live.** All of the above is offline measurement. The three
smokes exist to convert them into observed behaviour before the pair.

## The tripwire, pre-registered

**Any accusation on a previously-silent leg gets greped for the `harness-repair`
marker before it is attributed to anything else.** Repair-in-place is the only
shipped change that can turn a silent leg loud, and silent correct legs were
cleared 14/14, so a new accusation there is the specific failure this build could
introduce. The marker exists so that question is answered by a grep rather than
an argument.

## The bar (unchanged, two tiers)

* **PASS** — mean F1 above 0.685 AND zero accusations on historically clean cases.
* **STRONG PASS** — mean F1 >= 0.72 AND mean false accusations below 5 across
  both rolls.

Per-item stop-loss stands: three attempts per lever, then park it.

## Two kinds of death (taxonomy — keep permanently)

Cycle 7 killed three items, and they did NOT die the same way. The distinction
licenses different futures, so the verdict is recorded per item:

* **Premise false** — the item's stated reason for existing is contradicted by
  measurement. It does not come back. *Item 5 (silent-case retry): "a silent run
  learned nothing" is inverted; silent correct legs were cleared 14/14, so silence
  is the pipeline's most reliable signal.* *Item 3 (answer-reuse): 0 byte-identical
  judge prompts in 1,616 calls across the whole archive.*
* **Premise true, price fatal** — the benefit is real and measured; the cost is
  worse. It CAN return if the price changes. *Item 6 (seed shapes): genuinely
  fixes the armed-but-silent mode — one of the three modes Lang-63 has shown
  across five rolls — but at 7:1 leg exposure with no budget ceiling absorbing the
  additions. Returns if a firing on a correct patch ever costs less than 56%.*

A deferral is a third thing again: *fix (ii) is a measured wash whose numbers
expire when its dependencies move* — which they already did once, when fix (i)
shipped and changed its population from 10 rows on one leg to 27 across three.

# ===========================================================================
# PRE-COMMITMENT, SECOND REFRESH — after the pre-pair smoke
# ===========================================================================

The smoke (`smoke7_20260730_104806`, ~500k) invalidated BOTH precision claims in
the first refresh. Corrected here before the pair, not after.

## Math-65 is reclassified: expected-fixed -> NAMED RESIDUAL

The disputed-computation fact was **delivered — 4 times — and ignored.** The
accusing verdict still asserted `getChiSquare()` must equal "the sum of squared
residuals times the supplied weights" (the inverse of what the code does), with
`CITATION: NONE`, while that method's own source sat verbatim beside the firing.

Placement was the hypothesis. It is now measured wrong. One roll is not proof, so
the fact block STAYS — it is symmetric, costless, and may help at the margin
across rolls — but the claim can no longer be "plausible 5 -> 3".

**Math-65 joins Math-30 as a named residual.**

### Filed as the second data point on the 2b research question

Item 2b established that accusations face no evidence requirement (90% uncited vs
6% of dismissals; the prompt says "For a SOUND verdict, CITATION: NONE is fine").
This smoke adds: **even adjacent, verbatim, contradicting source code does not
bind an accusation.** Better delivery is not the answer, and neither is better
placement — the placement audit already showed facts sit adjacent to the firing at
a median 15% of the prompt.

Whatever eventually fixes the accusation side will be ENFORCEMENT, not delivery —
the same shape cycle 6 already proved on the dismissal side, where persuasion was
exhausted after three prompt rewordings and the decision had to move into code.
That is a cycle-8 design problem, not a this-week patch.

## Revised expected effects

* **Precision: 5 -> 4**, resting entirely on Closure-62 via the literal-
  concatenation fold. Residuals: **Math-30 and Math-65**.
* **Recall: repair-in-place**, now VERIFIED LIVE — repaired harnesses compiled and
  reached acceptance in production (3 repair events on Closure-62, 1 on Math-65,
  both legs reaching 5 accepted). The compilation assumption is retired.
* **Tripwire unchanged**: any accusation on a previously-silent leg is greped for
  the `harness-repair` marker before being attributed to anything else.
* **Bar unchanged** (two tiers).

## Runbook change, earned

**Smoke before pair is now permanent.** ~500k caught, in one run: an inert
mechanism (the dismissal rule still 0-for-2), a structurally unreachable claim
(fix (i) could never reach Closure-62), a failed hypothesis (placement did not
bind the accusation), and a live repair bug (double-tagged oracle IDs). Every one
of those would have silently corrupted the interpretation of the 7M measurement.

## Closure-62 is a THIRD residual — the dismissal rule was never the right tool

The re-smoke (`resmoke_c62_20260730_113508`) plus an offline check settles this,
and it invalidates the second refresh's precision claim as well.

**The fold worked.** Trusted values now extract and reach the judge: 4 TRUSTED
blocks in the re-smoke against 0 in the first smoke. Extraction-empty over the 228
records went 173 -> 161. That fix is good and stays.

**But extraction was never the binding constraint.** With the fold, Closure-62's
12 recorded rows still compare as `unknown`, and they still do WITH fix (ii)'s
token comparison added. Zero of them reach `matches`, which is the only verdict
that licenses a dismissal.

The reason is principled, not incidental:

```
test pins  : 'javascript/complex.js:1: ERROR - error description here\n
              assert (1;\n          ^\n'          <- spaces and newlines
alarm says : 'lhs=javascript/complex.js:1:ERROR-errordescriptionhere'
                                                  <- whitespace stripped
```

The harness normalises whitespace before comparing, so the fired value is a
whitespace-stripped derivative of the pinned value. They match on content and
differ only in whitespace — **and Closure-62's defect IS a whitespace defect**
(the caret position at end-of-line, and the spaces preceding it).

So a comparison loose enough to call these equal would dismiss a whitespace alarm
by ignoring whitespace. The value-matching mechanism cannot clear this leg without
being wrong for the wrong reason.

**Closure-62 joins Math-30 and Math-65 as a named residual. The batch's expected
precision movement is 5 -> 5.**

### What this costs the pair, stated plainly

The batch now has **no working precision lever**. Shipped and effective:
repair-in-place (recall, verified live) and the observability work. Shipped and
measured ineffective on their targets: the disputed-computation fact (delivered,
ignored) and the extractor fixes (extraction improved, but extraction was not the
constraint).

A pair run now measures repair-in-place against an unchanged precision baseline.
That is a legitimate thing to measure — the recall lever is real and its risk is
pre-registered — but it is NOT the precision-batch measurement the two-tier bar
was written for, and the PASS tier's "zero accusations on historically clean
cases" clause becomes the only precision content in it.

### The three residuals now share one shape

Math-30: universal-property checks in a degenerate regime. Math-65: an accusation
that ignores the contradicting source. Closure-62: an accusation whose value the
dismissal machinery cannot legitimately match. **All three are accusations that no
delivered fact can dislodge** — which is the cycle-8 enforcement question, now
with three data points rather than two.

## Boolean-swallow repair: VALIDATED and shipped

The largest rejection bucket (77 of 240), deferred only for want of a compiler.

    boolean-swallow          77 present -> 32 cleared
    swallowed-alarm          65 present -> 65 cleared
    missing-alarm-id         25 present -> 13 cleared
    rethrow-without-cause     7 present ->  5 cleared
  fully cleared: 101 of 235 (was 84)   detector regressions: 0
  compile (real jazzer jar, 111 pairs): 0 with more errors, 0 new error strings

The compile gate caught two defects in this transform that detector-clearance
could not, both in the same submission:

1. `rfind('try')` matched the substring inside words like `geometry` and `entry`,
   so the holder declaration landed in an unrelated block that had already closed.
2. More fundamentally, the alarm is frequently in a DIFFERENT METHOD from the
   catch — one archived harness catches at line 38 and alarms at line 138 — which
   no local declaration can bridge. The design was wrong independently of (1).

Both resolved by making the holder a class-level static field, in scope wherever
the alarm lives. Jazzer drives one input at a time, so a single holder is sound.

**Running tally of what the compile gate has caught that offline detector-clearance
structurally could not: four defects** — a global-replace corruption, double-tagged
oracle IDs, an alarm-constructor arity error, and this scope bug. Clearing a
detector and producing valid Java are different properties, and only one of them
is checkable without a compiler.

## STANDING RULE: source transforms validate against the compiler, not detectors

Any change that rewrites generated source validates against the VM compiler
(`javac` with the real `jazzer-api` jar), not against the project's detectors
alone. **Clearing a detector and producing valid Java are different properties,
and only one of them is checkable without a compiler.**

Earned over two validation rounds, four defect classes the detectors could not
see and that would each have shipped on offline evidence alone:

1. a global `str.replace` that corrupted a literal in the wrong location;
2. oracle IDs tagged a second time when already named at runtime;
3. a cause appended to an alarm constructor that already had two arguments;
4. a holder declared in a scope the alarm could not reach — because `rfind('try')`
   matched substrings inside words, and because the alarm is often in a different
   method from the catch entirely.

The gate also produced one false alarm of its own (a hand-written stub declaring
FuzzerSecurityIssueLow as extending Error rather than RuntimeException), so the
companion rule holds: when a measurement disagrees with the code, suspect the
measurement's own scaffolding first.

## STANDING RULE: build the guard population before the mechanism

Before designing any precision mechanism, assemble the population it must NOT
fire on, and use it as the design's first test — not its last.

Track record in one cycle, all before a token was spent:

1. **"Does the shown source contradict the check's premise?"** — killed. On a
   suspect patch the shown source has no authority; it may be the bug. So "source
   disagrees with premise" is the *definition of a genuine catch*, and the
   question would void the 67 guards.
2. **"Does the JAVADOC contradict the premise?"** — killed. Docs are largely
   patch-invariant and present in 228/228 rows, which is why it looked sound, but
   Math-65's javadoc ("variances are the reciprocal of the weights") reads
   naively as the MULTIPLY form the accusers asserted. The honest dismissals
   relied on the code, not the docs.
3. **Naive arbitration** (evaluate candidate formulas against the observed value)
   — reordered. Anchoring on the patched build's arithmetic has the same
   label-dependence one level down: on a fake patch the observed value matches the
   buggy formula, so arbitration would void genuine catches. Buggy-anchored
   arbitration fixes the authority model.
4. **Buggy-anchored arbitration** — cost re-estimated upward twice. The values it
   needs are captured **0 times in 1,452 buggy-side steps**, so it is a probing
   change, not plumbing. And the "it pays for itself by shrinking 6C's
   not-compared bucket" argument fails on measurement: that bucket is **3 of 161
   (2%)**; 6C's real distribution is 60% alarm-already-discarded and 34% no
   confirmation at all.

**The generalisable principle behind all four: code-as-authority is
label-dependent evidence.** Any mechanism that treats the patched artefact — its
source, its arithmetic, its output — as the standard of correctness will void
genuine catches, because on a fake patch that artefact IS the defect. Only
artefacts the defect cannot touch (the buggy build for quantities it does not
affect, the failing test's own pins) are label-independent authorities.

The guard set is now the standing safety test for every precision mechanism, and
it is cheap: assembling it cost one query.

### The admissible-authority list (the positive half of the rule)

This pipeline has exactly **three** label-independent authorities — sources of
truth the defect cannot touch, and therefore the only things a mechanism may
treat as the standard of correctness:

1. **The failing test's pinned expectations.** The test predates the patch and
   states what correct behaviour is for its own inputs. Strongest.
2. **The buggy build's behaviour on observables the defect does not touch.** The
   incumbent semantics of everything the bug leaves alone. Requires the
   family-duty boundary: on the failing test's OWN observable the buggy build is
   authoritative for nothing, and a mechanism must abstain.
3. **The documentation.** Largely patch-invariant, and measured present in 228/228
   fixture rows — but also measured *ambiguous* where it matters most (Math-65's
   javadoc reads naively as the formula the accusers asserted). Weakest of the
   three; usable as corroboration, not as a sole basis.

Every mechanism that has shipped and worked anchors on one of these: family-duty
on (1), the indiscriminate-rate gate and the fires-on-both gate on (2), the
trigger-lift dismissal on (1). Every design killed this cycle anchored, somewhere
in its chain, on **the patched artefact judging itself**.

**Design-review question, from now on: what is your authority, and can the defect
touch it?** If the answer involves the patched source, the patched arithmetic, or
the patched output, the mechanism will void genuine catches and the guard set will
prove it.

## Repair-in-place: measured (repairA/repairB, 9 legs x 2 rolls, git dcf681c)

Both rolls, rescored after a label error in the suite file (my `-c` for all nine
cases; the right patches ran, only the scoring label was wrong — verified the
label reaches only record-writing, never detection):

    Roll A:  TP=3 FN=1 FP=3 TN=2   P=0.50 R=0.75
    Roll B:  TP=3 FN=1 FP=3 TN=2   P=0.50 R=0.75

### Outcome-neutral — no result depended on a repaired harness

Attempt-tag attribution over all 15 legs with firings, both rolls:
**`ONLY-REPAIRED` is false on every one.** Every repaired harness that fired had
an unrepaired co-firer, so no catch required repair and none is attributable to it.

Roll A's Chart-19 is the sharpest case: the firing harness was `attempt_007`; the
repaired attempts were 4, 8, 9, 10. Zero overlap. An earlier claim in this
conversation called that catch strong circumstantial evidence for repair — it was
wrong, and the attempt-tag grep is what refuted it.

**Tripwire result: repair caused no false accusation either.** Math-39, the one
accuser with a worsening history, fired from attempts 3/8/11 (roll A) and 2/6
(roll B) against repaired attempts 4/5/7/12 and 3/4 — no overlap in either roll.

**Scope of the null claim:** it proves no outcome *depended* on a repaired
harness. It cannot prove what would have happened without repair, because repair
changes the sequence — an early repaired acceptance fills a slot, so later attempts
are never generated.

### Cost-negative — the benefit the outcome measure does not capture

Attempts-to-target per leg, same 9 legs, baseline pair vs this pair:

    mean 14.8 -> 12.5   (-2.3 attempts per leg; 8 of 9 legs improved)

Each attempt is an LLM call, so harness recovery buys **the same outcomes for
fewer calls**. Attribution caveat: this build also carries the alarm-ID gate
correction (14 previously mis-rejected harnesses now accepted), which pushes the
same direction. The -2.3 belongs to the harness-recovery work as a whole, not to
repair alone.

**Package line: outcome-neutral, cost-negative.**

### Chart-19's flip is still unexplained, but narrowed

It was missed in both baseline rolls and caught in both pricing rolls. Not repair
(above). Not the alarm-ID gate correction either — both firing harnesses
(roll A `attempt_007`, roll B `attempt_002`) pass the OLD buggy detector as well
as the new one, tested directly against the archive. Remaining candidates: suite
composition, or variance. Recorded as open rather than attached to the nearest
available cause.

### Watch list addition

**Math-39** — historically clean, one variance accusation in the big pair, now
accused in both pricing rolls. Cleared of repair involvement, so it is judge
lottery on a leg that raises alarms; but 2-for-2 after a clean history earns a
named place beside the three chronics pending an event-chain read.

---

