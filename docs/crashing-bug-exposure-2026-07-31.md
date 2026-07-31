# Crashing bugs and the semantic-bug campaign: what changed under them

Written 2026-07-31 for a reader who knows the codebase but has not followed the
July semantic-bug cycles. Question answered: **the last seven cycles targeted
semantic bugs — what did they do to the crashing-bug path, and what needs checking
before we trust it again?**

Short version: the crashing path was never *rewritten*, but it is not untouched
either. Roughly half the pipeline is shared, several shared components were
substantially reworked, and **no crashing-bug suite has been run since 2026-07-16** —
which predates every one of those changes. Nothing here is a known defect. It is an
unmeasured surface with five specific places worth a cheap check.

**Verification note.** Every code claim below (gate locations, line numbers, which
module is kind-aware, constant values, emitted log keys) was re-checked against the
source on 2026-07-31, after an earlier draft of this document stated the opposite of
the truth about differential replay (§3a) and cited three wrong line numbers.
Measured numbers taken from prior runs are marked where they were not independently
re-derived. If you find a discrepancy, trust the code and correct this file.

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
| Identical-drop ladder (`expected_is_test_literal` / `fired_at_test_input`, `IDENTICAL-DISMISSED` reasons) + `relation_verifier.family_duty()` | `run.py:2614` (`bug_kind != 'crashing'`), calls at `2671`/`2851` | "Is this the failing test's own observable?" — the escape hatch for the precision gates. |

Conversely, crashing-only paths exist too: the trigger-gate handling at `run.py:1034`
and `1567`, and expected-exception matching at `2486`.

**Consequence worth stating plainly:** most of the machinery that drives the semantic
score — synthesized relations, screening statistics, fire-rate evidence — produces
*nothing* on a crashing leg. A crashing run is a much thinner pipeline: context →
harness generation (accepted only if it crashes the buggy build) → fuzz the patched
build → buggy-side replay of the firing input → the defect-family dismissal
(`run.py:2486`) → judge. Note this does NOT include the differential-replay
classifier — see §3a.

---

## 3. What is shared, and which of it was reworked

"Shared" = the code executes for both kinds. Reworked-this-month items are flagged.

**`harness/campaign.py` — harness generation and acceptance.** Contains **no
`bug_kind` reference at all** (verified by grep; the single "semantic" hit is a word
inside a prompt string at line 579), so it is entirely kind-agnostic and crashing legs
run the identical loop: generate, compile,
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
leg. (Those figures are as reported in `docs/replay/` and the commit messages; the
per-leg attribution was independently re-checked, the offline counts were not.)
**All validation data came from semantic runs.**

**`execution/oracle_mute.py` — `mute_oracles` and `instrument_diversion`.** Muting
silences alarm throws so a shadowed replay can be re-run; the diversion counter
(2026-07-30) instruments `catch` blocks containing a bare `return` so that
"execution never reached the check" can be distinguished from "the check did not
fire". *Reworked heavily.*

**`execution/fuzz_runner.py` — replay and signatures.** `replay_input_report`,
`replay_input_muted`, and the new bounded `iterate_muted_replay` (2026-07-30);
`crash_signature` (exception type + first project frame) and `cause_signature` (root
of the `Caused by:` chain). *Replay reworked; signature functions unchanged.*

**`relations/evidence_facts.py` — computed facts.** Shared in general, with one
important exception: **`classify_differential_replay()` is SEMANTIC-ONLY** and is
deliberately not run for crashing bugs (gate at `run.py:1975`). See §3a — this is a
design decision, not an oversight, and it is the single most important thing to
understand about crashing-bug verdicts.

**`relations/judge_decision.py` — `adjudicate()` (NEW as a shared entrypoint).** Both
production judge sites and the offline replay tool now go through one function: run
`relation_verifier.verify()`, then apply deterministic overrides — a re-ask lint when
a dismissal cites nothing or contradicts something the check itself pins; an automatic
drop when the buggy-side fire rate reaches `INTRINSIC_FIRE_RATIO` (0.95, in
`evidence_facts.py:38`; the indiscriminate cap `MAX_FIRE_RATIO` is 0.20), escape via
`_family_duty_escape` in `judge_decision.py:168`; a terminal drop
when a replay confirmed the check fires on both builds with *identical* values, with
an explicit keep when the values *differ*.

---

### 3a. The one thing to get right: crashing bugs do NOT use differential replay

It is natural to assume the differential replay — "replay the crashing input on the
buggy build; if it crashes there too, the crash is pre-existing and not the patch's
fault" — is the crashing path's core check. **It is the opposite: it is gated
`bug_kind == "semantic"` and must never run on a crashing leg.** The reasoning is
spelled out at `run.py:1952-1974` and is worth internalising:

- Every accepted harness reproduces the crash on the buggy build **by construction** —
  that is the acceptance gate for a crashing bug.
- An overfitting patch that fails to fix the defect therefore crashes on the patched
  build with the **same signature** as on the buggy build.
- So "the same crash reproduces on the buggy build" is precisely the **true-positive
  condition** for a crashing bug. A differential-replay check would read that exact
  pattern as "pre-existing surface" and flip every genuine catch into a miss.

On semantic legs the same fact means the opposite thing: our alarm firing identically
on both builds means the alarm is describing behaviour the patch never changed, so the
finding is not evidence about the patch. Same measurement, inverted meaning — which is
why the gate exists.

What crashing legs use instead: the **defect-family dismissal** at `run.py:2486`. It
dismisses a finding only when the replayed input reproduces the same check or the same
exception type on buggy **and** no defect-family exception type is present — i.e. it
tries to separate "the bug's own crash family, still crashing" (a catch) from
"unrelated pre-existing crash surface the fuzzer stumbled into" (noise). The
`expected_exceptions` list built at `run.py:1567` from the failing tests' exception
types is what defines the defect family.

## 4. Where interference is plausible — and why that is not the same as "shared"

Interference risk is a *subset* of shared code. Shared is a fact (it runs); risk is a
property (it assumes something that only holds for semantic legs). Ranked:

**(1) The crashing-only defect-family dismissal (`run.py:2486`) — the crash path's
own verdict mechanism, sitting in heavily-reworked code.** This is the crashing
counterpart to differential replay (see §3a): it dismisses a finding when the exact
firing input, replayed on the buggy build, fires the same check or raises the same
exception type there *and* no defect-family type is present. It reads
`_breplay_ids`, `_bt_all`, `_bt_defect` and `_esc_type` — all produced by the
buggy-replay and muted-replay machinery that was rewritten this month. The dismissal
logic itself is crash-specific and old; its *inputs* are new. Unexercised since 07-16.

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
behaviour-preserving (a counter plus `System.err.println("[relscreen] skipped=" + …)`,
`oracle_mute.py:417`) and skips rethrowing and alarm-throwing catches. Two things to
check rather than assume: whether an extra stderr line can disturb `crash_signature()`
/ `cause_signature()`, which scan the same Jazzer output; and that the `[relscreen]`
prefix is shared with the screening counter line (`checked=`/`violated=`,
`oracle_mute.py:256`) while the keys differ — the parsers key on `skipped=` vs
`checked=`, so they should not collide, but that separation is deliberate and worth
preserving if either line is ever edited.

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
machinery whose outputs feed the crashing path's own defect-family dismissal, and the
new single judge entrypoint with its deterministic override gates. Note that the
differential-replay classifier is semantic-only by design (§3a) — for a crashing bug,
"the same crash reproduces on buggy" is the catch condition, not a refutation. Crashing legs
skip the entire relation-synthesis engine and the lifted-assertion path, so much of the
semantic scoreboard does not apply to them. Two of the shared changes I initially
flagged as risky turned out to be safe or actively aligned with the crashing design
once read. What remains is an unmeasured surface with four modest risks, closable by
two unit tests, one free offline repair check, and a single 14-leg rerun compared per
leg against the 2026-07-16 baseline of P=0.86 / R=0.86.
