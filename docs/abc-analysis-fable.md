# 3-Arm Fresh-Bug Run (2026-07-21) — Independent Deep-Dive (Fable)

> **Provenance.** Written 2026-07-23 by Claude Fable 5, from nine parallel trace audits of the
> archived runs `armA_off_20260721_122647`, `armB_focused_20260721_133026`,
> `armC_attr_20260721_150801` (one audit per robust failure, one per cross-arm flip, one on the
> arm-C recall collapse, one on the July 17–21 iteration history). Every claim below is backed by
> quoted trace lines; where I checked source code, the file is named. I read the earlier
> `abc-flag-experiment-analysis.md` midway through; §7 lists exactly where this analysis confirms,
> extends, or **contradicts** it. Everything else was derived independently from the traces.

## 1. What was tested and what happened

15 bugs the pipeline had never been tuned on (dev-split, `used_in_tuning: false`; the 27-bug
holdout stays untouched), 18 legs (8 overfit patches to catch, 10 correct patches to pass), three
arms differing only in two experimental flags:

| Arm | TP | FN | FP | TN | P | R | F1 | tokens |
|---|---|---|---|---|---|---|---|---|
| A — flags off (baseline) | 5 | 3 | 3 | 7 | 0.62 | 0.62 | 0.62 | 1.94M |
| B — `--focused_synthesis` | 5 | 3 | 5 | 5 | 0.50 | 0.62 | 0.56 | 2.65M |
| C — `--attribution_judge` | 2 | 6 | 4 | 6 | 0.33 | 0.25 | 0.29 | 1.81M |

For calibration: on the *tuned* 15-leg set the pipeline had reached P=0.80 R=0.89 (foc15b), and
the pre-iteration diagnostic (diag-24, also on then-familiar bugs) was P=0.88 R=0.58. So on
genuinely fresh bugs, precision gave back everything the July fixes had bought on the tuned set,
while recall roughly held.

One-roll-per-arm means single-sample noise moves F1 by ~0.1. The trustworthy signal is what
repeats across all three arms:

- **Robust FPs (all 3 arms): Closure-70, Math-30, Math-65.**
- **Robust FNs (all 3 arms): Closure-38, Lang-63.**
- **Robust TPs: Math-74, Math-82 (all arms); Math-68, Math-73 wherever their oracles got rolled.**
- Everything else that moved between arms (Chart-19-o, Math-104, Math-68, Math-73-o, Math-39,
  Math-73-c) is dominated by synthesis-roll variance, with two exceptions worth keeping
  (Chart-19-o's arm-B catch and Math-39's arm-B/C FP are mechanistically attributable — §3, §5).

## 2. Did the July iteration help? Split verdict

**What transferred to fresh bugs — the structural mechanisms.** The three-to-four robust catches
all ride on machinery built during the iteration: documented-`@throws` relations (Math-73-o:
`solve` on a non-bracketing interval must throw, deterministic 20000/20000), trusted lifted
regression values (Math-68: MINPACK parameter off by 1.2e-5 at tol 1e-9), replay-relations-on-
patched (which made these deterministic), and judge calibration (without which full30v2-style
recall collapse would have eaten everything). Recall on unseen bugs (0.62 headline, ~0.44
trustworthy) is at or slightly above diag-24's semantic recall (0.50) *on bugs it had already
seen*. That is real transfer.

**What did not transfer — the precision guards.** Every fresh FP belongs to a class the iteration
already "fixed": Closure-70 is the pre-existing-crash/generic-exception-leak class, Math-30 and
Math-65 are the latent/patch-invariant-check class. The guards exist; each was evaded not because
the *idea* was bug-shaped but because the **fact-computation layer has implementation holes** —
the mechanically computed fact the verifier depends on was masked, unreached, extrapolated, or
overridable by narrative. Fresh bugs found the holes; the tuned bugs never exercised them. In
other words: the July strategy ("compute a fact into evidence, never ask the judge to judge
harder") was right, and is *reaffirmed* by this run — but the facts are currently computed
incompletely, and the judge is still allowed to out-argue them.

## 3. Root cause per robust failure (one story each)

**Closure-70 (correct, FP in all arms) — the crash the buggy build was never allowed to reach.**
The firing is not an oracle at all: harness attempt_007 re-runs the compiler a second time
(`processForTesting(null, n2)` at FuzzHarness.java:109) but forgets the AST block-wrapper the
first run had, so that statement throws `IllegalStateException` on *every* build whenever it
executes. It only executes after all four warning oracles pass — i.e. only on a correct build; on
the buggy build an oracle throws first. So the buggy-replay fact came back as "the buggy build
handles this exact input WITHOUT raising IllegalStateException — the patch INTRODUCED this
exception", which is literally true and completely misleading: the buggy replay *died earlier at
its own oracle*, so the crash site was never reached. The verifier, handed that mis-stated fact,
convicted. Note attempt_003's identical crash *was* correctly dismissed ("reproduces identically
on the buggy build") — the guard works when the site is reachable. The class is new only as a
costume: **oracle-shadowed buggy replay** (any harness statement placed after the oracles is
structurally unreachable on the buggy build, because firing-on-buggy is the acceptance
criterion).

**Math-30 (correct, FP in all arms) — a "must be 1.0" that no implementation satisfies there.**
The check: identical samples of length n≥46350 must give Mann-Whitney p-value 1.0. But n² for
n≥46350 overflows 32-bit int in a multiply the *developer fix does not touch* — buggy, patch, and
dev fix all produce NaN in that regime. The check had been direction-confirmed at n=1500 (a
different regime) and that confirmation was extrapolated; the per-check replay-on-buggy at the
actual firing input was never computed because "a DIFFERENT check fired first on the buggy build" —
first-firing-wins scanning masked it. Buggy NaN == patched NaN at the firing input; the observable
is patch-invariant; the firing is zero evidence.

**Math-65 (correct, FP in all arms) — same class, same masking, plus a rate nobody looked at.**
The invented invariant (`getChiSquare()` equals the weighted residual sum recomputed at the
returned point) is false for this implementation on irreducible-residual problems on *every*
build: it fired on 14832/20000 buggy fuzz inputs **and 9674/20000 patched fuzz inputs** — a
"contract" violated by half of all valid inputs indicts the check, not the patch. That patched-side
fire-rate was printed in the trace and never used. Per-check buggy replay: again masked by
first-firing-wins. One variant even carried a computed "[symmetric firing] ALSO fired on the buggy
build" fact — overridden by the "same observable as the bug ⇒ keep" exception.

**Math-39 (correct, TN in A, FP in B and C) — a trusted test stretched outside its own domain.**
The trigger test pins "derivative evaluations stay within the interval" for integration over
[0, 0.001]. Arm B's focused synthesis "generalized around the boundary" down to interval length
1e-6 — where the a-priori trial step in `initializeStep` (untouched by the *developer fix* too)
overshoots on every implementation. The pipeline **computed the right fact and said so**:
"identical rejection on both builds; the patch did not change this behaviour. Pre-existing
input-rejection surface — dismiss." The verifier kept it anyway: "This assertion encodes the
library's own trusted regression test… SOUND." Trust inherited from a test is only valid inside
the input domain the test actually pins; outside it, the oracle is model-invented. This is the
same error as Math-30's n=1500→n=46350 extrapolation, and the same family as the diag-24
CHANGED-REGION failures.

**Closure-38 (overfit, FN in all arms) — the pipeline caught it and talked itself out of it.**
The overfit inserts a space after *every* `-` (instead of only before negative numbers), so
compact output regresses: `0-0` becomes `0- 0`. The harness fired on exactly this, twice, plus a
deterministic 20000/20000 replay. The verifier dismissed every firing: "a correct compact printer
could legitimately emit the semantically equivalent string `0- 0`" — hypothetical-correctness
reasoning, reinforced by a prompt rule that orders whitespace normalization. But the trigger
test's own expected-vs-actual diff is `x-[ ]-0.0` vs `x-[]-0.0` — a **whitespace insertion**, the
same diff class. The defect's own ground truth proves whitespace is a pinned observable for this
API, and that fact was never computed into evidence.

**Lang-63 (overfit, FN in all arms) — every oracle either can't see the branch or can't see at all.**
The spurious `end.add(DATE,-1)` only executes on a field borrow (endValue < startValue). The
screen-fuzz numbers tell the whole story: the seed-literal relation fired at ratio 1.0 (it *is*
the seed), and all five "generalizing" relations fired at ratio 0.0 — kept anyway as "silent on
buggy (tripwire)". Two are delegation tautologies (3-arg `formatPeriod` vs the 5-arg overload it
delegates to — both sides run the same code, vacuous on every build); the rest pin day-1/midnight
endpoints that never enter the borrow branch. Fuzzing *did* reach the borrow branch (attempt_003
fuzzed `endDay < startDay`) — with only the vacuous check attached. Non-seed discriminating power
across all surviving relations: zero. The campaign converged anyway.

**Chart-19 (overfit, FN in A and C, TP in B) — right observable, wrong receiver state.**
The overfit relocates the null-rejection into `AbstractObjectList.indexOf` *after* the scan loop,
so it throws only when the list has no null holes. Arm A synthesized `indexOf(null) == -1` but its
harness built a sparse list with holes — fires on every build; the verifier correctly called it
unsound; meanwhile the mandated catch-and-skip structure swallowed the patch's *new* IAE as an
"input rejection". Arm A's screen had also kept this rule despite fire-ratio 1.0 on buggy
("direction-confirmed"). Arm B's `@throws`-focused pass produced the clean discriminator — hold
the null argument fixed, *vary the receiver state* (install a second axis at index 1–3) — and it
fired 7310/20000, judged sound via the trusted test. That is a genuinely better-targeted oracle
(two independent harnesses fired), not roll luck — but note the same "generalize the trusted test"
instinct produced the Math-39 FP; the difference is Chart-19-B varied *setup state within the
contract's domain* while Math-39-B *extrapolated the domain itself*.

**Arm C's recall collapse (Math-68/73-o/104, Chart-19-o) — not the flag.** The attribution LLM
(the only thing `--attribution_judge` gates) ran on 6 legs and **flipped zero outcomes**; it
answered ATTRIBUTED on every persisting FP. The four lost catches died upstream: Math-73-o and
Math-104 were oracle-construction roll variance (C's roll produced weaker/tighter-epsilon variants
judged — correctly — unsound), and Math-68 was killed by an **un-gated** injected note (run.py
~line 2522): "[trigger-test lift] … the REAL test … PASSES. If this firing replays the test's own
scenario/inputs, it … must be dismissed." It attached in C only because C's oracle was *named*
`lifted-param-1` (regex on `/lift/`; arm A's `jennrich-seed-p1` escaped it) and it treats
test-passage as exoneration even with an observed value 1.2e-5 off the pinned literal. **This
landmine is live in every arm and is name-triggered** — it will keep killing Math-68-style catches
at random.

## 4. The general diagnosis: five mechanical gaps

Every one of the eight wrong verdicts (plus the B/C-only ones) reduces to one of five gaps. None
of the fixes below mentions any specific bug — that is the non-overfit test — and none asks the
judge to "be more careful".

**G1 — Per-check, full-scan, unshadowed replay of the exact firing input on the buggy build.**
Today the buggy-side scan stops at the first check that fires (masked Math-30 and Math-65), and
replay runs the whole harness so earlier oracle throws hide later crash sites (inverted the
Closure-70 fact). Fix: for *the* firing check, replay with all other oracles suppressed
(catch-and-continue) so its site is reached, and record the observed **values/exceptions on both
builds**, NaN-safe. "Buggy observable == patched observable at the firing input" ⇒ the patch
didn't change this behavior ⇒ zero evidence. Recall-safe: a genuine overfit divergence shows
*different* values at the firing input (a partially-unfixed region shows buggy==patched only where
a correct patch would differ — which stays a keep under the existing bug's-own-family rule; G1
computes the fact, it does not auto-dismiss).

**G2 — Trust-domain fencing.** Every trusted artifact (lifted test, doc value, direction
confirmation) carries the input domain in which it was actually established, recorded
mechanically (the test's literal parameters/ranges; the screen input that confirmed direction). A
check used **outside** its trust domain loses the "trusted" trump and must meet the
invented-oracle bar. Kills the Math-39 and Math-30 extrapolations; would have demoted Math-65's
javadoc leap. This is the fence diag-24's prompt-level attempts failed to build — it works here
because it is a recorded fact, not an exhortation.

**G3 — Discrimination facts at screening.** Three cheap numbers per relation, all computable from
runs already happening: (a) buggy-build fire-ratio (~1.0 = fires for reasons intrinsic to its own
construction, not the defect — Chart-19's sparse list); (b) patched-build fire-ratio (double-digit
percent = the check is wrong, not the patch — Math-65's 48%); (c) **non-seed discriminating
power** — buggy firings attributable to inputs other than the pinned trigger literals (zero across
all survivors = the campaign has only memorized the seed — Lang-63). Plus a static vacuity check:
an agreement relation whose two sides resolve to the same implementation is a tautology (Lang-63's
overload pair; visible in the call graph). Convergence may not be declared while non-seed
discriminating power is zero — that is recall pressure applied at the *screen*, where it cannot
manufacture unsound assertions (unlike prompt-level "probe harder", which diag-24 proved toxic).

**G4 — Diff-class evidence from the defect's own ground truth.** Compute the trigger test's
expected-vs-actual diff class (whitespace-only / numeric-with-magnitude / exception-identity /
count / …) and each firing's buggy-vs-patched diff class, and put both in evidence. A firing whose
diff class matches the trigger test's proves that observable is contract-pinned for this API
(Closure-38: whitespace *is* the bug); hypothetical-correctness stories ("some correct printer
could…") cannot survive it. This also answers the reference-differential dilemma (§7): the
reference that needs no developer fix is **the buggy build plus the trigger test's own diff** —
convict on "patched differs from buggy, in the defect's own diff class, outside the trigger
input"; dismiss on "patched equals buggy at the firing input" (G1).

**G5 — Facts outrank narrative.** Twice in this run a correctly computed fact was overridden by
prose ("identical rejection on both builds — dismiss" lost to "encodes the trusted regression
test" in Math-39; the /lift/ note's unconditional "must be dismissed" overrode a 1.2e-5 divergence
in Math-68-C). Make the trump order explicit: a computed differential fact can only be overridden
by a *stronger computed fact*, never by a plausibility argument; and fix the run.py ~2522 note —
scope it off the name regex and add a far-beyond-jitter carve-out. This is the cheapest fix on the
list and removes one robust FN pattern (Closure-38's doctrine-override) and one live random-FN
landmine (Math-68).

Mapping: Closure-70 → G1. Math-30 → G1+G2. Math-65 → G1+G3. Math-39-B/C → G2+G5. Closure-38 →
G4+G5. Lang-63 → G3. Chart-19 → G3 (+ port of B's receiver-state variation, fenced by G2).
Math-68-C → G5. Math-104 is knife-edge sub-floor numerics (6.4e-11 against a 1e-9 floor) — count
it as unreliable recall by design, don't chase it.

## 5. The flags

- **`--attribution_judge`: retire.** It flipped zero outcomes here, approved every FP it saw,
  fails open, and its criterion (is the divergence also on the buggy build / documented?) is
  orthogonal to the question that decides our FPs. Its one observed "kill" mechanism operated
  through an un-gated note that isn't even part of the flag (§3, G5).
- **`--focused_synthesis`: don't ship the flag; port one behavior.** Its one real win (Chart-19)
  came from the rejection-contract pass that varies receiver state under a fixed rejected input;
  its two real costs (Math-39, Math-73-c) came from domain extrapolation and sibling-agreement
  brittleness. Port receiver-state variation into base synthesis **gated by G2**, and down-weight
  sibling/overload agreement when the patched code is shared by both sides (structurally blind) —
  then drop the flag. +37% tokens for the 4-pass union is not justified by one fenced behavior.

## 6. What to do next (order matters)

1. **G5 + G1 first** — pure plumbing, no new judgment: fix the /lift/ note, set the fact-trump
   rule, make firing-input replay per-check/full-scan/unshadowed with value capture. Expected on
   this set: all three robust FPs → TN, Math-68 stops being roll-fragile. Highest
   confidence-per-token on the board.
2. **G3 at the screen** — discrimination facts + the non-seed-power convergence gate. This is the
   only recall lever that doesn't route through "prompt the model to assert more" (which diag-24
   killed). Targets the Lang-63/Chart-19 miss class.
3. **G2, then G4** — trust-domain fences, then diff-class evidence. G4 is the most novel and
   should be validated most carefully (its conviction side must stay scoped to the defect's own
   diff class, or it becomes the v2 CHANGED-REGION failure again).
4. **Validate cheap, then fresh, then holdout — in that order.** Iterate each gap on the burned
   sets (tuned 15 + these 15, now a 30-leg regression pool) with the nano model; when the pool is
   clean, one flagship confirm on the ~12 still-unseen dev bugs; only then, once, the 27-bug
   holdout for the headline number. These 15 bugs are burned as of this run — never tune on them
   and call the result "fresh" again.
5. **N≥3 rolls, paired, for any future config comparison.** One roll per arm cannot rank arms;
   this experiment re-proved it. Also consider majority-of-3-rolls conviction as a production
   variance dampener — but measure it after G1–G5, not before, or the votes will share the same
   systematic errors.

Answer to "should we iteratively fix the others too?": yes — but iterate on **mechanisms, not
cases**. All eight wrong verdicts fit five general gaps; not one needs a bug-shaped rule. If a
future failure doesn't fit a general gap, leave it as measured error rather than buy it back with
a narrow rule — that is the discipline that makes the next fresh set a real test.

## 7. Where this differs from `abc-flag-experiment-analysis.md`

Independent confirmations: variance dominates single-roll arm ranking; the arm-C collapse was not
the attribution flag; retire attribution; trustworthy recall ≈0.44; Chart-19/Lang-63/Closure-38
FN mechanics; Math-39/Math-73-c brittleness as focused-synthesis costs; Math-104 sub-floor.

Material deltas:
1. **Closure-70 diagnosis corrected.** It is not a "brittle lifted warning-count oracle" — the
   warning-count oracles were quiet on the patched build. The conviction came from a latent
   harness crash *behind* the oracles, reachable only on correct builds, with the buggy-replay
   fact inverted by oracle shadowing (§3). Different fix: unshadowed replay (G1), not
   oracle-brittleness rules.
2. **RC-1's open question ("is reference-differential viable without the dev fix?") — yes**, and
   no synthesized second patch or eval-only compromise is needed: the reference is the buggy build
   (dismiss side, G1) plus the trigger test's own diff class (convict side, G4). Both are
   computable at detection time.
3. **The pipeline's stated root cause needs sharpening.** "Hypothetical-correctness reasoning" is
   the symptom; in four of the six robust failures the pipeline *had computed or printed* the
   decisive fact and it was masked, unreached, extrapolated, or overridden. The root cause is that
   facts are computed incompletely and don't bind (G1/G5), which is more actionable than a judging
   philosophy.
4. **New finding: the /lift/-triggered dismissal note (run.py ~2522) is a live, name-dependent FN
   landmine in all arms**, unrelated to either flag.
5. **Symmetric-firing policy nuance.** The doc's fix list would hard-dismiss build-symmetric
   firings; the standing rule ("symmetric is also a catch pattern") is right — a partially-unfixed
   overfit fires symmetrically at still-broken inputs. G1+G2 resolve the tension without a hard
   rule: compute the per-input fact, fence the trust domain, and let the bug's-own-family
   exception operate only inside the fence.

## Appendix — per-leg map

| Leg | Truth | A | B | C | Root cause (gap) |
|---|---|---|---|---|---|
| Chart-11-CapGen | correct | TN | TN | TN | — |
| Chart-19-Arja | overfit | FN | TP | FN | receiver-state fence (G3) |
| Chart-19-ACS | correct | TN | TN | TN | — |
| Closure-38-SequenceR | overfit | FN | FN | FN | diff-class + doctrine (G4, G5) |
| Closure-70-Jaid | correct | FP | FP | FP | oracle-shadowed replay (G1) |
| Lang-22-DeepRepair | correct | TN | TN | TN | — |
| Lang-63-Arja | overfit | FN | FN | FN | zero non-seed power, vacuous relations (G3) |
| Math-30-CapGen | correct | FP | FP | FP | masked replay + extrapolated trust (G1, G2) |
| Math-39-Arja | correct | TN | FP | FP | domain extrapolation, fact overridden (G2, G5) |
| Math-65-CapGen | correct | FP | FP | FP | masked replay + fire-rate ignored (G1, G3) |
| Math-68-Arja | overfit | TP | TP | FN | /lift/ note landmine (G5) |
| Math-73-ACS | overfit | TP | TP | FN | roll variance |
| Math-73-Arja | correct | TN | FP | TN | sibling-agreement blindness (flag cost) |
| Math-74-Arja | overfit | TP | TP | TP | fuzz-fragile (1.4% hit) — robustify via G3 stats |
| Math-82-HDRepair | overfit | TP | TP | TP | — |
| Math-82-ACS | correct | TN | TN | TN | — |
| Math-86-Arja | correct | TN | TN | TN | — |
| Math-104-Elixir | overfit | TP | FN | FN | sub-floor knife-edge — accept as unreliable |
