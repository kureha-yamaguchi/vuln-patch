# Crashing bugs and the semantic-bug campaign: what changed under them

Written 2026-07-31 for a reader who knows the codebase but has not followed the
July semantic-bug cycles. Question answered: **the last seven cycles targeted
semantic bugs — what did they do to the crashing-bug path, and what needs checking
before we trust it again?**

Short version: the crashing path was never *rewritten*, but it is not untouched
either. Roughly half the pipeline is shared, several shared components were
substantially reworked, and **no crashing-bug suite has been run since 2026-07-16** —
which predates every one of those changes. Nothing here is a known defect. It is an
unmeasured surface with four specific places worth a cheap check.

---

## 1. Why the two bug kinds diverge inside the pipeline

Everything below follows from one difference.

**Semantic bug.** The buggy program completes and returns a *wrong value*. Nothing
observable happens unless the harness supplies an expectation. So a semantic harness
is a comparison machine: call the API, establish what the answer must be, compare,
and on disagreement throw **our own alarm type**,
`com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow`, carrying an `[oracle:<id>]`
tag. Such harnesses deliberately wrap API calls in broad `catch` blocks that `return`
— an exception means "this fuzzed input was invalid", not "the patch is wrong".
Detection event = **our alarm escapes**.

**Crashing bug.** The buggy program throws (IndexOutOfBounds, NPE, …). No expectation
is needed; the JVM supplies the event. The harness's job is to *reach* the faulty
state. Detection event = **a foreign exception escapes** and Jazzer reports it.

So: for semantic legs the escaping exception is our own instrument; for crashing legs
it is the program's own failure. Every risk in §4 is a place where shared code
assumes the first situation.

---

## 2. What crashing legs never touch

These are gated on `bug_kind == "semantic"` in `run.py` and are simply absent from a
crashing run — no risk, but also no benefit from any work done on them:

| Component | Gate | What it is |
|---|---|---|
| `relation_synth` → `relation_screen` → patched-side replay | `run.py:1150` | The second detection engine: propose general rules, screen each against the buggy build, replay survivors on the patched build. The single biggest recall mechanism for semantic bugs. |
| `test_oracle_miner` (mined sibling oracles) | `run.py:1062` | Off by default anyway. |
| Semantic class-context assembly | `run.py:1008` | Extra context for value-comparison checks. |
| Lifted-assertion path + trigger-test-lift note | `run.py:1297/1315` | Lifting the failing test's `assertEquals` expectations. Meaningless without expected values. |
| Identical-drop ladder + `relation_verifier.family_duty()` | `run.py:1487/1975/2972` | "Is this the failing test's own observable?" — the escape hatch for the precision gates. |

Conversely, crashing-only paths exist too: the trigger-gate handling at `run.py:1034`
and `1567`, and expected-exception matching at `2486`.

**Consequence worth stating plainly:** most of the machinery that drives the semantic
score — synthesized relations, screening statistics, fire-rate evidence — produces
*nothing* on a crashing leg. A crashing run is a much thinner pipeline: context →
harness generation → fuzz → differential replay → judge.

---

## 3. What is shared, and which of it was reworked

"Shared" = the code executes for both kinds. Reworked-this-month items are flagged.

**`harness/campaign.py` — harness generation and acceptance.** One bug-kind reference
in the whole module, so crashing legs run the identical loop: generate, compile,
require a crash on the buggy build, then the acceptance gates — the self-swallow lint,
the alarm-ID requirement, gate 0b (a caught-and-rethrown crash must carry its cause),
and the family-novelty steering. *Reworked:* the novelty gate (new in July, plus a
significant extractor bug fixed on 07-26), and repair-in-place was wired in ahead of
the gates.

**`harness/repair.py` — repair-in-place (NEW, 2026-07-29/30).** Three mechanically
detected defects are now fixed rather than discarding the attempt: alarm thrown inside
a swallowing catch; alarm constructed without an `[oracle:]` tag; alarm raised inside a
catch without attaching the caught exception as cause. Measured offline: 101 of 235
archived rejected harnesses fully cleared, 0 compile regressions over 111 pairs.
Measured live: outcome-neutral on the 9-leg pricing pair, −2.3 generation attempts per
leg. **All validation data came from semantic runs.**

**`execution/oracle_mute.py` — `mute_oracles` and `instrument_diversion`.** Muting
silences alarm throws so a shadowed replay can be re-run; the diversion counter
(2026-07-30) instruments `catch` blocks containing a bare `return` so that
"execution never reached the check" can be distinguished from "the check did not
fire". *Reworked heavily.*

**`execution/fuzz_runner.py` — replay and signatures.** `replay_input_report`,
`replay_input_muted`, and the new bounded `iterate_muted_replay` (2026-07-30);
`crash_signature` (exception type + first project frame) and `cause_signature` (root
of the `Caused by:` chain). *Replay reworked; signature functions unchanged.*

**`relations/evidence_facts.py` — computed facts.** Includes
`classify_differential_replay()`, which is the crashing path's core judgement input:
replay the exact crashing input on the buggy build and classify INTRODUCED /
PREEXISTING / SHADOWED / ABSTAIN. *Reworked repeatedly* — the SHADOWED/ABSTAIN states,
the muted re-replay upgrade, and gating of its "buggy ran this cleanly → the patch
introduced it" wording on the new diversion signal (that wording was demonstrably
fabricating evidence against a correct patch before the fix).

**`relations/judge_decision.py` — `adjudicate()` (NEW as a shared entrypoint).** Both
production judge sites and the offline replay tool now go through one function: run
`relation_verifier.verify()`, then apply deterministic overrides — a re-ask lint when
a dismissal cites nothing or contradicts something the check itself pins; an automatic
drop when the buggy-side fire rate is ≥95% (escape: `family_duty`); a terminal drop
when a replay confirmed the check fires on both builds with *identical* values, with
an explicit keep when the values *differ*.

---

## 4. Where interference is plausible — and why that is not the same as "shared"

Interference risk is a *subset* of shared code. Shared is a fact (it runs); risk is a
property (it assumes something that only holds for semantic legs). Ranked:

**(1) `classify_differential_replay()` — the crashing path's core, rewritten, unmeasured.**
This decides whether a crash on the patched build is the patch's fault. Every change
to it this month was motivated, tested and validated on semantic-leg fixtures. Its
crash-leg behaviour is unexercised since 07-16. This is the highest-value thing to
re-verify, not because a defect is suspected but because it is the component that most
directly determines crashing-bug verdicts.

**(2) The terminal identical gate inside `adjudicate()`.** It reads fires-on-both facts
out of the evidence text. Crashing legs *do* produce differential-replay facts, so
unlike the rate gate this is not obviously inert. If it were ever to fire on a crash
leg, its escape hatch (`family_duty`) is semantic-only and could not rescue the
finding. Needs a pinned invariant, not an assumption.

**(3) The intrinsic-rate drop inside `adjudicate()`.** Reads `[fire-rate fact]` blocks,
which are produced by relation screening — skipped on crashing legs — so it *should*
always be a no-op there. Same argument as (2): probably inert, unproven, and with a
semantic-only escape hatch.

**(4) `run.py:2407`: `_real_test_passes = (bug_kind != 'crashing')`.** A kind-dependent
constant feeding evidence construction, sitting in code that changed repeatedly. The
crash branch has not been exercised since the changes around it landed.

**(5) `instrument_diversion` stderr output on crash-output parsing.** The transform is
behaviour-preserving (a counter plus a `System.err` line) and skips rethrowing and
alarm-throwing catches. The only question is whether an extra stderr line can disturb
crash-signature parsing, which scans Jazzer output.

### Two risks I initially assumed and then withdrew after reading the code

- **`repair_swallowed_alarm` was not a crash-detection risk.** It inserts
  `if (e instanceof FuzzerSecurityIssueLow) throw (FuzzerSecurityIssueLow) e;` at the
  top of broad catch bodies — it rethrows *only our own alarm class*. A genuine
  `IndexOutOfBoundsException` from a crashing bug is still caught exactly as before.
  Behaviour for the crash signal is unchanged.
- **`repair_rethrow_without_cause` is aligned with the crashing design, not against it.**
  It attaches the caught exception as the alarm's cause — which is precisely what
  campaign gate 0b already requires (`campaign.py:538`, "caught-crash re-throws must
  carry a cause") and what `cause_signature()` exists to read: its docstring states
  that a harness catching a library crash and rethrowing it as our own alarm type
  *hides* the crash from the headline signature, and the attached cause is what
  preserves its identity for attribution. The repair implements the standing
  requirement. Residual note only: nothing has verified this on real crash-leg
  harnesses.

---

## 5. Measurement status

- **Last crashing measurement:** `crashcheck` (2026-07-16, 14 legs, archived at
  `runs-archive/runs/crashcheck_20260716_012430/`), **TP=6 FN=1 FP=1 TN=6 · P=0.86
  R=0.86 F1=0.86**. Predates all seven cycles.
- **Suite file:** `suites/crash14.cases` was deleted in the 07-21 suites cleanup
  (commit `db38bd5`) as a spent one-off; recoverable from git history, or
  reconstructable from the archived run's leg list.
- **Label quality caveat:** the crashing pool is largely *uncertified*. The July
  certification work (developer-fix comparison, divergence-kind classification) covered
  the semantic pool; `suites/labels/crashing/` holds what exists. A crashing rerun
  therefore scores against weaker ground truth than a semantic one, and small
  per-leg differences should not be over-read.

---

## 6. Recommended checks, cheapest first

1. **Two unit tests (minutes, free).** (a) `indiscriminate_buggy_rate()` and
   `terminal_profile()` return None / no-op for representative crash-leg evidence
   blobs — turns risks (2) and (3) from assumptions into pinned invariants. (b) A
   harness repaired by `repair_rethrow_without_cause` still yields the same
   `cause_signature()` the pinning path expects.
2. **Offline repair check on crash-shaped harnesses (free).** Run `repair_harness()`
   over harnesses recovered from the archived `crashcheck` run, compile-check on the
   VM as was done for the semantic corpus (0 regressions over 111 pairs there).
3. **Rerun the crashing suite once (~1–2M tokens).** Reconstruct `crash14.cases`,
   run on the current build, compare **per leg** against the 07-16 result — the
   aggregate is 14 legs and moves ~7 points per leg, so per-leg reading is the only
   sound comparison. Do this before any statement that the pipeline's current state
   covers both bug kinds, and before the next full 30-leg semantic pair is treated as
   the pipeline's official state.

Not recommended: any crashing-specific development before step 3 says whether
something regressed. The point of these checks is to price an unmeasured surface, not
to open a new work front.

---

## 7. One-paragraph summary for someone in a hurry

The July cycles were semantic-bug work, but they landed in shared code: harness
generation and acceptance, the repair-in-place transforms, the muted/diverted replay
machinery, the differential-replay classifier that crashing bugs depend on most, and
the new single judge entrypoint with its deterministic override gates. Crashing legs
skip the entire relation-synthesis engine and the lifted-assertion path, so much of the
semantic scoreboard does not apply to them. Two of the shared changes I initially
flagged as risky turned out to be safe or actively aligned with the crashing design
once read. What remains is an unmeasured surface with four modest risks, closable by
two unit tests, one free offline repair check, and a single 14-leg rerun compared per
leg against the 2026-07-16 baseline of P=0.86 / R=0.86.
