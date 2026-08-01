# Semantic-bug detection — the plan

Restructured 2026-07-18: ground rules first, finished work as a list by
pipeline station, current scoreboard, then remaining work by station
ordered by impact-vs-risk, rejected ideas at the bottom. The full
pre-restructure text (with the long per-phase case histories) is
preserved verbatim in `plan-history.md` (PART 0).
Companion docs: `suites/DATASET_AUDIT.md` (inventory + verdicts),
`suites/labels/incorrect_labels.md` (exclusion evidence),
`suites/pinned_tasks.jsonl` (the verified task set),
`suites/label_annotations.jsonl` (label corrections).

---

## The problem

We are given a bug and a candidate patch for it. Some patches are real
fixes; some are **overfit** — they make the bug's failing test pass
without actually fixing the underlying behavior. Our pipeline writes
small fuzzing programs (**harnesses**) full of **checks** ("this call
must not crash", "the mean must equal n·m/N") and runs them against the
patched program. The checks must be:

- **safe**: they never accuse a genuinely correct patch, and
- **sharp**: they do catch the overfit one.

An overfit patch passes every existing test by construction, so it can
only be caught on inputs no test covers — where "what is correct?" must
be reconstructed from indirect evidence, ranked by trust:

1. the bug's original failing test (definitive, but only for its inputs)
2. the buggy program itself (correct everywhere except at the bug)
3. the documentation comments (the promised contract)
4. universal math/logic rules ("sorting twice = sorting once"), if
   genuinely universal
5. the patch's own code — **least trusted; it may be the overfit**

Every **miss** (an overfit we don't catch) means we failed to check
something the overfit gets wrong. Every **false alarm** (a correct
patch we accuse) means we checked something a correct program is
actually allowed to do differently.

## The pipeline in seven stations (referenced throughout this doc)

(One "leg" = one candidate patch being judged, start to finish. A bug
usually has two legs in our data: its correct patch and its overfit
patch. Every leg passes through these seven stations.)

1. **Setup** — apply the patch; prove the bug's test fails before it
   and passes after it.
2. **Rule-writing** — a model reads the changed code, its docs and the
   failing test, and proposes general rules a correct program must
   obey.
3. **Rule screening** — each rule is compiled and run ~20,000 times
   against the buggy program to weed out rules that accuse everything.
4. **Harness writing** — a model writes three fuzzing programs full of
   checks: copies of the failing test, the screened rules, and checks
   it invents itself.
5. **Acceptance** — each harness must prove it fires on the buggy
   program; we record which check fired and what crash was underneath.
6. **Judgment day** — the harnesses fuzz the PATCHED program, and every
   screened rule is also compiled on its own and run directly against
   the patched program ("replay"). Any check that fires here is an
   accusation.
7. **The judge** — a model reviews each accusation: would EVERY correct
   program satisfy this check, or could a correct one trip it? Only
   kept accusations count.

---

## GROUND RULES — read before running or changing anything

**Firewall.** The developer's real fix may be used ONLY offline — for
cleaning the dataset, verifying labels, and understanding misses
afterwards. Never in any decision the pipeline makes.

**Substrate.** All experiments run against `pinned_tasks.jsonl`, where
every overfit patch is verified to actually behave differently from the
real fix and every correct label is double-checked. On the pinned set,
every miss is a real technique failure and every false alarm a real
safety failure.

**No pooling AT ALL — HARD RULE (tightened 2026-07-19).** Never share
harnesses, oracle checks, relations, corpora or verdicts between legs
or between runs — every leg is fully self-contained. The 2026-07-18
version of this rule permitted within-run sharing between a bug's legs
(P3.2 pooling); the user closed that boundary on 2026-07-19: a leg
convicting via a sibling leg's rules is still a verdict the leg did
not earn from the bug alone, and nothing transfers to a deployment
that sees one patch. Pooling was removed from run.py the same day.
The sanctioned compensation for synthesis randomness is MORE OWN
rules per leg (`--synth_max_rules` default 8; every screened survivor
feeds the patched-build replay — the prompt stays capped at 2). Side
effect: with no pooling there is no correct-leg-before-overfit-leg
ordering constraint — suites may run fully parallel at any size.

**No dataset overfitting.** Mechanisms may encode general categories
("read-only calls must not mutate state", "program text tolerates
whitespace insertion") — never the shape of a specific benchmark bug.
Anything motivated by staring at a dev bug must still be justified as a
category before it ships.

**Measurement rules.**
1. Change one thing per measurement point. We once turned on several
   untested changes together (p23gate) and could not tell which change
   caused what until a day of forensics.
2. A changed outcome (miss→catch or new false alarm) is believed only
   after one confirming repeat — harness generation is partly random.
3. Results are tied to the environment (JVM: OpenJDK 11.0.31). The
   unwinnable-task list is environment-specific.
4. Held-out hygiene: fixing plumbing on a held-out failure is fine;
   adjusting prompts/checks/thresholds from held-out output silently
   converts a held-out bug into a dev bug. The held-out set is spent
   ONCE, at the very end.
5. Suite mechanics: run legs 4-way parallel (up to 6 for small
   projects; beyond that the model API is the bottleneck). Check free
   disk first (a Chart/Closure-heavy suite once filled the disk and 18
   legs died at checkout). (The old pooling ordering constraint —
   correct leg before overfit sibling — is GONE with pooling itself,
   2026-07-19: suites of any size may run fully parallel.) After
   every suite: delete working copies, archive
   results to the Mac under `runs-archive/`, verify the archive, prune
   VM runs.
6. Iterate cheap: day-to-day iteration uses the 2–6 legs relevant to
   the change; between-phase gates use the 30-leg dev set; the
   flagship full sweep is for final confirms only.
7. Stop rule: if two consecutive iterations change no dev outcome,
   stop tuning that area — further tuning fits noise.
8. Launch check: the replay stage needs `--replay_relations_on_patched`
   in the suite's COMMON flags. It is NOT in old cases files — a full30
   was once launched without it and had to be killed and relaunched.

**Two meta-rules distilled from the 2026-07-17/18 cycle.**
- *A mechanism beats an instruction, everywhere.* A recall idea only
  counts if its check reaches the patched build mechanically (replay,
  generated code, within-run pooled rules). Anything delivered as
  prompt advice is multiplied by the harness writer's implementation
  rate and the fuzzer's input luck, and that product is small: Math-2's
  convicting formula was synthesized, screened and pooled for weeks and
  still missed until the replay stage executed it directly.
- *The judge needs computed facts, not exhortations.* Every false-alarm
  class was fixed by computing a fact and putting it in the judge's
  evidence ("this check also fired on buggy", "the real test passes on
  this build", "this exception is not one of your checks") — never by
  asking it to be more careful. And never auto-dismiss on an ambiguous
  signal: both mechanical auto-dismissals we tried (latent firings;
  same-name reconciliation) each killed a true catch and had to be
  narrowed to replay-the-fact-and-let-the-judge-decide.

---

## REJECTED / DEAD ENDS — do not revisit without new evidence

- **Pooling of harnesses/oracles/relations, in ANY form (REJECTED —
  cross-run 2026-07-18, within-run 2026-07-19; a hard NO by decision,
  not an evidence question).** Persisting or sharing accepted
  instruments between runs farms the benchmark; sharing them between a
  bug's legs within one run still hands a leg a verdict it did not
  derive from the bug alone. Every leg is fully self-contained; the
  compensation for synthesis randomness is more own rules per leg.
- **Mechanical auto-dismissal of latent firings**: a check behind an
  always-firing seed check is latent on buggy precisely because the
  scan stops at the first firing per input — its first real chance to
  run comes when the overfit silences the seed. The auto-dismissal
  killed the true Lang-60-o capacity catch (minfix_w1). Compute the
  buggy-replay fact and let the judge decide.
- **Unscoped "dismissal wins" reconciliation**: transferring an
  unsound verdict across harnesses by check NAME killed the true
  Closure-62-o catch (full30) — a generic name like `lifted-test`
  labels different checks in different harnesses. Cross-harness
  transfer only for injected-rule names.
- **Asking the prompt nicely to "explore beyond the seed input"**:
  tried twice (diag2, diagf), 3 false alarms each time — the
  instruction is ignored in practice.
- **Voting across a bug's several patches** ("if most patches behave
  the same way, trust that behavior"): repair tools tend to make the
  SAME mistake in all their patches for a bug, so agreement proves
  nothing. (Rule pooling shares the checks, never the verdicts.)
- **Coverage-guided differential fuzzing for certification**:
  considered twice; every wrong "no difference" we ever found came
  from looking at the wrong OUTPUT, never from failing to find the
  right INPUT — P4.2 fixes the actual cause.
- **Judge majority voting**: dead in BOTH regimes. Without computed
  facts (2026-07-15 replay): error rate unmoved at 3× the cost. WITH
  the full fact stack (2026-07-25, cycle-4a, reverted in ad65fdc):
  offline replay A/B measured identical over-kill and leak at 3× the
  cost. The leaky verdicts are wrong for reasons every lens shares;
  redundancy cannot fix them. Do not propose a third time.
- **Raw-string comparison of lifted code/text outputs**: fires on
  formatting deltas, hands the judge a legitimate dismissal, and
  buries the content difference the same comparison would have caught
  (all three Closure-92-o firings in full30). Always
  whitespace-normalize.
- **Blanket increase of harnesses per leg**: every extra harness is a
  false-alarm lottery ticket on the correct sibling; zero false alarms
  was measured at n=3. Extra attempts only when aimed (RETRY).
- **Spending effort on the unwinnable tasks**: Lang-7, Lang-22,
  Math-30, Math-59, Closure-115, Closure-123 (and the mislabeled
  correct sides of Lang-41 / Lang-10) are proven behaviorally
  identical to the real fix in our environment or wrongly labeled —
  there is nothing to catch there.

---

## STANDING RULES (the distilled rulebook — full origin text in plan-history.md)

1. **Label-independent authorities only.** A mechanism may anchor on: the failing
   test's pinned expectations; the buggy build's behavior on observables the defect
   does not touch (family-duty boundary decides); documentation (weakest — measured
   ambiguous). NEVER the patched artifact judging itself — its source, arithmetic, or
   output. This killed four designs in one day; check it first in every design review.
2. **Guard population before mechanism.** The 67 kept genuine catches are the standing
   safety set; any precision mechanism must show ~zero wrongful voids on it BEFORE
   anything else is measured. Killed/reordered five designs pre-spend.
3. **Measure each change alone; measurements expire.** A shipped change invalidates
   dependent measurements (fix (ii)'s population moved under it). Population-pinning
   tests enforce re-measurement.
4. **Two kinds of death.** Premise-false (never comes back: silent-case retry) vs
   price-fatal (may return if the price changes: seed shapes, fix (ii)). Record which.
5. **Source transforms validate against the VM compiler,** not detectors alone —
   the compiler caught four defect classes detectors structurally cannot.
6. **Smoke before pair.** Every mechanism observed firing live (trace events, not
   verdicts) before any paired measurement includes it.
7. **Raw results committed before interpretation. Verbatim numbers from bare pytest.
   Populations verified against fixtures, never narrated from memory.**
8. **Record raw, compare processed.** Comparison transforms must not destroy the
   original value (the whitespace lesson; cycle-8 item 8.4 implements it).
9. **Vocabulary that can't be read backwards.** gold = keep-finding / dismiss-finding.
   Fixture access via field() raises MissingField — never .get() on fixtures.
10. **Every mechanism decision emits a trace event.** Print-only diagnostics are
    invisible by design (run.log is deleted on success).
11. **No cross-run pooling** (hard user rule). Within one run only.
12. **No judge-prompt wording iteration** — measured dead repeatedly; changes to the
    judge are structural (facts, gates, output format) or nothing.
13. **fresh12 launches only on the user's literal phrase.** No inferred permission.
14. **LLM-assigned names are not identifiers.** Rule/family/check names are
    generated fresh per roll with no stable vocabulary; any mechanism or
    measurement keyed on them (novelty steering, persistence, cross-roll matching)
    must define identity semantically — by asserted property — or it will read
    name drift as absence. 8.14b's count inverted under a threshold change and was
    retracted the same day (2026-08-01); the verified false absent is Chart-19's
    winning family vs its own roll's categoryplot proposals.
15. **Assert a guard's inputs exist, not just its outputs.** A guard evaluated
    before (or without) its inputs is fail-open while looking armed — it reports
    success BECAUSE it guarded nothing, so testing outputs alone cannot catch it.
    Three measured instances share the shape and the antidote: the family-duty
    escape asked 141 times with an empty failing-test block (cycle 6); the
    terminal-marker substring firing on prompt template text (cycle 5); the label
    assert placed before CASES was sourced (8.8, caught pre-ship). Every one dies
    to the same one-line check: assert the input population is non-empty and
    well-formed before trusting any pass. Applied prospectively to 8.12(b)'s
    recovered-corpus check the same day it was promoted (2026-08-01) — and it
    LANDED: the corpus did not exist (0 of 14 crashcheck legs have a trace.md;
    crashtrace1 yields n=0 repairable), and without the assert the study would
    have reported "0 regressions, 0 new errors" over nothing. Caught its
    next instance within hours: an 8.12(a) test stub declared `family_duty(**kw)`
    against a positional call — every invocation threw, took the fail-open path,
    and three tests passed while testing nothing; found by checking assertion
    directions, not pass counts.

## CURRENT STATE (2026-07-31)

- **Score:** paired mean F1 0.685 (final30A/B, identical commits) vs 0.49 July
  baseline. Recall 8/14 stable both rolls. Repair-in-place: outcome-neutral,
  cost-negative (−2.3 attempts/leg).
- **Precision ceiling ADOPTED: ~5 accusations on the trap set** under the current
  architecture — over measured refutations: no recorded feature separates kept catches
  from kept false accusations; the deciding fact is present in 6 of 8 decisive cases
  (8.6 added Math-39 as the sixth) but case-specific (no general gate); adjacent
  verbatim delivery is ignored; the narrow quote-forced question voids 22% of guards;
  authority — tier AND scope — does not separate either (8.15, 2026-08-01: 58/60,
  29/28, 12/12; the largest scope gap runs the wrong way). Three independent
  dimensions tested, three negatives. Remaining directions are architectural
  (cycle-8 items 8.1–8.3).
- **Residuals (chronic):** Closure-62, Math-30, Math-65, Math-39 (fourth — 8.6,
  2026-08-01) — one shape: accusations no delivered fact dislodges. Math-39 is the
  hardest variant: the invoked tier-1 authority is REAL (testTooLargeFirstStep pins
  the property) but its scope does not extend to the firing; the deciding fact was
  delivered on every chain and read as corroboration. (8.6 fully closed 2026-08-01:
  final30A shows the same mechanism — three rolls, nine accusations, one mechanism.)
  **Hard column:** Closure-38; Math-104 (CHARACTERIZED 2026-08-01 by 8.18: merited
  near-tolerance dismissals — harness comparisons at 10e-15 against the
  implementation's DEFAULT_EPSILON 10e-9; unwinnable at current tolerance; parked
  floor question). **Coin-flips:** Lang-60,
  Math-68, Math-73, Closure-92. **Multi-mode:** Lang-63 (all three failure modes on
  record).
- **Miss stations (8.14 + 8.14b, 2026-08-01, corrected counts):** across the three
  current-config runs, 10 of 14 misses reach a judge and are dismissed (both
  tracks counted); 3 build but never fire; 0 die at harness construction. At
  family level (8.14c, semantic read against frozen properties — after 8.14b's
  string-matching count was retracted): invention is the MINOR station — 1
  confirmed never-proposed, ≤4 with every ambiguous row against, vs the judge
  class at up to 10; the invented-vs-too-weak boundary is not archivally
  drawable, so invention levers gate on live A/Bs only. The classes OVERLAP and
  their levers are disjoint. 8.18 then closed the loop (2026-08-01): 94% of the
  judge's miss-side dismissals are CORRECT — grounded refutations, wrong-family
  checks, near-tolerance — with ungrounded hypotheticals at 2 of 34. **Every
  recall station is now measured and no large addressable class exists**: the
  misses are distributed across small causes, and the binding recall constraint
  is CHECK SHARPNESS at stations 2–4 (checks that fire but deserve dismissal) —
  the exact boundary 8.14c showed is not archivally drawable. The judge is
  simultaneously the recall station where misses become visible and the capped
  precision component (under-dismissal) — opposite directions, so no single
  strictness knob fixes both; the lever must be evidence-shaped or architectural.
- **Honestly open:** Chart-19's missed-twice→caught-twice flip in the pricing pair —
  not repair (attempt-tag grep), not the gate correction (old detector passes both
  firing harnesses); remaining candidates composition/variance.

## CURRENT MEASUREMENT PROTOCOL (the next 30-leg pair)

Runs only after the cycle-8 batch lands, on the user's word. Width 5 / -m 12 (pending
8.5), both rolls same commit, zero changes between, per-leg reading, tripwire: any new
accuser greped for the harness-repair marker before attribution.
**PASS = paired mean > 0.685 AND ≤5 accusations per roll AND zero accusations on
historically clean legs.** The historically-clean set excludes the four named
residuals — Math-39 left it via 8.6 (2026-08-01). The former sub-5 "strong" tier is
RETIRED — see the ceiling evidence above and in plan-history.md.

## PARKED (pointers; full specs in plan-history.md)

- P4.1 did-nothing-patch detector & P4.2 probe splitter (station TO-DO era specs).
- Accusation-side evidence requirement (the citation asymmetry) — architectural
  research question; three failed treatments on record.
- Math-104 floor refinement — firewall warning applies (the 7e-15 is adjudication
  evidence, never a detection input).
- Fix (ii) non-numeric comparison — deferred, park condition recorded with both
  measurements.
- Absence-argument detector (judge quotes text to prove what it doesn't say).

---

## 2026-07-31 — CYCLE 8 EXECUTION PLAN (self-contained handoff)

Context in one paragraph: the cycle-7 close measured a precision ceiling — ~5 false
accusations on the trap set survive every delivery-side fix, because the judge needs no
evidence to accuse (measured 90% uncited) and three interventions failed (adjacent
verbatim source ignored; citation-requirement simulation costs 60 catches for 23 saves;
the isolated contradiction question voided 22% of genuine catches). Recall is at 8/14
stable with the gaps mapped. Repair-in-place shipped: outcome-neutral, cost-negative
(−2.3 attempts/leg). Standing rules apply to every item below: statement test (nothing
bug-shaped in any requirement); guard set BEFORE mechanism (the 67 kept genuine catches
are the standing safety population); label-independent authorities only (failing test's
pins, buggy build off-defect, docs — never the patched artifact judging itself);
measure each change alone; verbatim numbers from bare pytest; raw results committed
before interpretation. fresh12: LOCKED, only on the user's literal phrase.

### 8.1 Judge-model swap experiment (gpt-5.5 replacing the incumbent gpt-5.4) — FIRST; needs user go-phrase
**Target:** the judge LLM behind `relation_verifier.verify()` (and `family_duty`).
**Failure mode:** the ~5-accusation ceiling — specifically the 5 evidence-present-but-
ignored accusations and the 22% wrong-void behavior. Question: model quirk or
task-architecture?
**Steps:**
1. Pre-launch: confirm the gpt-5.5 deployment exists and answers (one probe call);
   verify `reask_verdict_usable()` recognizes THIS deployment's error/sentinel strings
   (a silently fail-open judge is the July-15 bug; add its error format to the
   sentinel list if absent). Commit this check before spending.
2. Part A: `verifier_replay.py --cases tests/fixtures/cases228.jsonl` (plus the 67-row
   guard fixture) with the judge model set to gpt-5.5 (incumbent judge: gpt-5.4), votes=1, repeats=1. Same prompts,
   zero prompt edits.
3. Part B: the frozen narrow contradiction question (exact phrasing from the failed
   engagement experiment — do NOT reword) over the same 24 accusing checks + 67 guards,
   model gpt-5.5.
4. Noise floor (REQUIRED before reading part A): the incumbent flips verdicts between
   identical single draws — 5 of 10 untouched rows flipped between two draws in the
   cycle-5 close. Establish the same-model flip rate first: one incumbent (gpt-5.4)
   re-run on the same fixture at votes=1 repeats=1, per-case flips counted against its
   recorded verdicts (archived verifier_replay repeats data may substitute where it
   covers the rows). A gpt-5.5 flip counts as signal only where the flip rate exceeds
   this floor, or where it lands on one of the 5 ignored-evidence cases with an
   engagement shape no recorded incumbent draw ever produced.
5. Commit raw outputs BEFORE scoring (both parts and the floor run).
**Read-out (pre-registered):** per-case flips vs the incumbent's recorded verdicts, not
totals — gpt-5.5 is newer than the incumbent gpt-5.4, so aggregate movement in
either direction is plausible; the aggregate is NOT the signal either way. Decision table:
same failure shape (ignores Math-65's adjacent source; plausible-irrelevant quotes;
guard voids) → ceiling is architectural; record and close the model question. Different
shape (engages on any of the 5 ignored-evidence cases) → model-dependence shown; opens
(a) re-test on any future stronger model, (b) cross-model agreement as a NEW candidate
(different from same-model voting, which is dead: same model = same blind spots ×3).
**Cost:** ~1.5–2.5M, API-only, no VM. One shot per part, no iteration.
**Throughput:** add retry/backoff on rate limits — the engagement experiment lost
25 of 91 calls to throttling; denominators must be complete this time.
**Follow-on note:** if part A shows model-dependence, the dormant model-escalation
config (`config.py`, unused in every measured run per the commit audit) is the natural
wiring point for a per-role model choice — do not build it speculatively.

### 8.2 Reimplementation-as-evidence — design study ONLY (no build) — free
**Target:** a NEW evidence generator in the execution layer (peer of
`relation_screen.py`), output threaded into evidence assembly in `run.py` as a computed
fact.
**Failure mode:** accusations resting on misremembered contracts (Math-65 class):
disputes about what correct behavior IS, which survived delivery, placement, and
questioning.
**Design to write (one doc, committed):**
1. Mechanism: for a disputed observable (detected the way `disputed_computation_fact`
   already detects recomputations), generate an independent implementation FROM THE
   DOCUMENTATION (never from patched source); run it on the same inputs.
2. The authority screen (MANDATORY, this is the whole design): validate the reference
   implementation against the BUGGY build on observables the defect does not touch
   (family-duty boundary decides "does not touch"). Disagreement there → discard the
   reference, emit nothing. Only a buggy-validated reference may generate the fact
   "patched output disagrees with a doc-derived reference that matches the incumbent
   semantics elsewhere."
3. The mirror canary as a spec'd test: fake patch + correct check — the reference must
   side WITH the check, not the patched code. (Same-model risk: the generator may
   misremember the same javadoc the accusers misremember — the buggy screen is the
   only defense; state it.)
4. Dependencies: needs 8.3 (buggy-side observed values). Price: LLM cost per disputed
   observable, screen cost, expected reach (count applicable rows in cases228).
5. Prior art to read first (plan-history.md PART 1, station TO-DOs): the P4.2 "split
   certifier probe" spec — model constructs scenarios, FIXED code enumerates and
   prints every observable — and the certifier's divergence-kind classifier
   (value_ulp / exception_generic_latent WEAK kinds). The reference-implementation
   generator should reuse that architecture rather than reinvent it.
6. External prior art (`docs/related-work-scan-2026-08-01.md`): Differential
   Prompting (ASE 2023) validates the mechanism class — a reference implementation
   synthesized from inferred INTENT, not from the code under test, found
   failure-inducing inputs at 75% vs 28.8% for direct prompting, and their reason is
   our reason (a reference derived from the code inherits its bug — same defense as
   our buggy-side screen). Poracle (TOSEM 2023) is the published form of our
   preservation-condition authority. Read both summaries before writing the design.
7. Scope note to include: whether the same buggy-validated machinery can ground
   DISMISSAL-side hypotheticals ("a correct implementation could return X" — the
   measured drift-kill signature, wrong in 4 of 6 measured instances). Design
   consideration only; if ever built it gets its own guard population (the
   correctly-dismissed firings on correct legs), since it pushes toward keeping.
**Done when:** the design doc exists with guard-set test plan and priced reach; user
decides build/no-build on it.

### 8.3 Buggy-value collector — build AFTER 8.1/8.2 confirm direction — small
**Target:** `fuzz_runner.py` replay functions (`replay_input_report`,
`replay_input_muted`) and the recorded facts they emit.
**Failure mode:** 0 of 1,452 recorded buggy-side steps carry an observed VALUE (only
fired/counts) — makes 8.2 untestable and forces 6C's values-not-compared abstentions.
**Steps:** capture the fired message's key=value observations on buggy-side replays
(the values are printed only when a check fires there; silent-on-buggy stays valueless,
which fails SAFE — arbitration abstains); thread into the recorded fact via
`record_event`; extend `compare_fired_values` consumption where applicable. Offline
tests from archived raw outputs where any exist; otherwise fixture-built. Validation
rides the next live run passively — no dedicated run. If this ships, ONE re-ask of
8.15's scope dimension is licensed (firing input vs the test's pinned scenario,
properly measured instead of keyword-classified) — recorded there, 2026-08-01.

### 8.4 Raw-value recording — independent, small, ships with any batch
**Target:** check-writing instructions in the harness codegen prompts (`prompts.py`):
any check that normalizes text before comparing MUST also print the pre-normalization
value in its fired message (compare normalized, RECORD raw).
**Failure mode:** the setup-divergence dismissal rung in `run.py` is structurally dead
for the ~17% of checks that normalize — their reported values can never equal the
test's literal (why Closure-62 was unreachable). General rule being implemented:
transformations for comparison must not destroy the original.
**Validation:** prompt-compliance smoke on ~20 generations (do fired messages carry
both forms?), then the rung's extractor extended to read the raw field; measured alone
on cases228 (no verdict may flip except via newly-possible legitimate matches).
**Scope decision (2026-08-01):** the Math-104 tolerance-hygiene line (no check
tighter than the implementation's own documented epsilon) does NOT bundle into this
prompt change — it would widen the compliance-smoke surface and violate
measure-one-change. Noted here as a design candidate for a later batch; its payoff
is wasted-firing reduction only (no current verdict flips on it, per 8.18).

### 8.5 Relation-budget experiment (-m 12 vs -m 16) — cheap, anytime (~1M)
**Target:** the `-m` relation-synthesis cap in suite COMMON flags.
**Failure mode:** Chart-19 caught twice at -m 16, missed twice at -m 12; its winning
family was proposed-then-died (2d), so the budget may starve it at standard config.
**Steps:** the width5 catch-leg suite EXTENDED with the Chart-19 and Lang-63 overfit
legs (7 legs total — the unmodified suite omits the experiment's own motivating case),
one roll each at -m 12 and -m 16, same commit; read INVENTION RATES of known winning
families per leg from traces (not pooled scores). Side benefit: two more Chart-19
rolls inform the open composition-vs-variance question at zero extra cost. If -m 16 materially raises invention, adopt it as standard and record the
config change; if not, Chart-19's fragility attributes to variance/composition and
stays open. 8.14 outcome note (2026-08-01): leg-level construction death measured
ZERO, so read the -m comparison as the Chart-19-specific budget question. Third arm
(focused per-source synthesis — 07-20 kill verdict void under the single-roll ban;
best recall evidence on record: foc5 4/4 by-pass targets, foc15 R=0.89): 8.14c
found invention the MINOR station (1 confirmed, ≤4 of 14), so class size alone no
longer justifies the arm's cost — run it only as a deliberately-priced live A/B,
and only if the judge-side levers (8.18 → 8.1) stall or report negative.

### 8.6 Math-39 event-chain read — DONE 2026-08-01: named mechanism
**Target:** archived pricing-pair traces, both Math-39 accusing verdicts' full evidence
chains (facts delivered, gates consulted, judge WHY/CITATION).
**Failure mode:** an unexplained NEW repeat accuser (2-for-2 after clean history;
repair ruled out by attempt-tag grep). Outcome: either a named mechanism (joins the
chronic list / licenses a fix) or documented judge-lottery (watch list entry closed).
**Outcome (`docs/replay/backtrack/8.6-MATH39.md`, verified against traces):** named
mechanism, sixth bucket-(a) case — five accusing verdicts across the pricing pair,
all CITATION: NONE with the trigger-lift and buggy-replay facts delivered. The
invoked tier-1 authority is REAL and in-scope claims stop at the test's own
scenario; the firing sits outside it (two fuzzed values). Consequences applied:
Math-39 → fourth residual; 8.15 gains the authority-SCOPE dimension. Remnant closed
(6ffa9f0): final30A carries 4 more accusations, same shape, same delivered facts,
all uncited — three rolls, nine accusations, one mechanism.

### 8.7 Marker-field fix — DONE 2026-08-01 (ad41e38)
**Target:** `campaign.py` acceptance bookkeeping + the `harness-repair` trace event.
**Failure mode:** repair attribution needed attempt-tag archaeology this week; record
per accepted harness whether it came from a repaired attempt (and which repairs), so
attribution is one field lookup. Include repaired-source reconstructability note (the
transform is deterministic over the recorded pre-repair output).
**Outcome:** shipped — the acceptance event carries `[FROM REPAIRED ATTEMPT: <kinds>]`
with from_repaired_attempt / repairs_applied / repaired_source_reconstructable
detail (`campaign.py:865`). Write-before-read ordering checked explicitly (standing
rule 15 applied); the provenance map is run-local and test-pinned against cross-run
drift. 566 passed, 7 skipped.

### 8.8 Suite-file label check — DONE 2026-08-01 (fd67182)
**Target:** `run_suite.sh` case-file loading.
**Failure mode:** the -c-on-fakes typo class (happened once; firewall held, full
rescore required). Assert each case's label matches `suites/pinned_tasks.jsonl`;
refuse to launch on mismatch.
**Outcome:** shipped — refuses on BOTH mismatch directions (flag-vs-patch-path and
flag-vs-pinned-tasks), correct labels pass. Placement lesson recorded: the check
sits immediately after `source "$CASES_FILE"` with an ordering assertion — the
first placement refused only after creating run state, the second ran before
CASES existed and silently passed everything. A guard evaluated before its inputs
exist is fail-open while looking armed — the same defect family as the
empty-failing-test gate (cycle 6) and the terminal-marker substring (cycle 5).

### 8.9 Family-persistence design note — paper only; gate pends 8.14c
**Target:** `relation_synth.py` round structure + harness generation loop in
`campaign.py`.
**Failure mode:** the invention lottery — check families proposed but landing in
`relations_not_implemented` (Lang-63's winning family in one roll), absent next roll.
(8.14c settled what the retracted 8.14b could not: final30B Lang-60 is the ONE
confirmed never-proposed miss — nothing that roll asserts contains()
non-mutation; night20c Lang-60 is ambiguous. Gate: live A/B only, priced against
the minor-station class size of 1–4 legs.) Design constraint from the retraction:
persistence must track families by ASSERTED PROPERTY, not by generated name —
names have no stable vocabulary across rolls.
**Must price:** slot competition (persisted families crowd out new ones) and the
interaction with the novelty gate (which steers AWAY from covered families — these two
must not fight). Run-local only; nothing crosses runs.

### 8.10 Pair pre-commitment upkeep — free, continuous
**Target:** this plan's measurement protocol section.
Keep current: re-scoped bar (PASS = paired mean > 0.685 AND ≤5 accusations per roll AND
zero accusations on historically clean legs; the old <5 strong tier retired with a
pointer to the ceiling evidence), residuals (Closure-62, Math-30, Math-65, Math-39 —
8.6 confirmed the fourth), Chart-19 recorded honestly open (two causes eliminated:
not repair, not the gate correction).

### 8.11 (optional) Repo housekeeping from the commit audit — anytime, zero risk
**Target:** dead modules the 2026-07-19 commit audit marked for deletion, still
pending: `variation_menu.py` + `variation_menu.json`, `input_kind.py`,
`context_study.py`, `suites/rulegen_B.cases` + `rulegen_P.cases` (reference argparse
flags removed in f2ea4a8 — running them dies at startup).
**Failure mode:** none — inert code that makes every repo-wide grep noisier. Git
history preserves everything. NOTE: `test_oracle_miner.py` stays until after fresh12
per the audit's original condition.

### 8.12 Crashing-bug exposure checks — free parts now, rerun before the next pair
**Target:** shared components crashing legs run through — the crashing-only
defect-family dismissal at `run.py:2486` and the buggy/muted-replay machinery feeding
it, the terminal-identical and intrinsic-rate gates in `adjudicate()`,
`harness/repair.py`, `instrument_diversion`. (NOTE: `classify_differential_replay` is
semantic-ONLY by design — for a crashing bug the same-crash-on-buggy pattern is the
TP condition, see the exposure doc §3a.)
**Failure mode:** no crashing suite has run since 2026-07-16 (crashcheck: P=0.86
R=0.86), which predates all seven cycles; shared code was reworked under them with
semantic-only validation data. Full analysis:
`docs/crashing-bug-exposure-2026-07-31.md`.
**Steps:** (a) unit tests pinning gate inertness on crash-leg evidence and
cause-signature preservation through `repair_rethrow_without_cause` (free);
(b) run `repair_harness()` over harnesses recovered from the archived crashcheck run
and compile-check on the VM (free) — standing rule 15 applies: the crashcheck
archive predates all seven cycles, so ASSERT the recovered corpus is non-empty and
parses under the current extraction before reading "0 regressions" as a pass;
(c) reconstruct `suites/crash14.cases` (deleted in
db38bd5, in git history) and rerun once (~1–2M), comparing PER LEG against the 07-16
result — 14 legs, ~7 points per leg, so aggregates mislead. Note the crashing pool is
largely uncertified, so weak per-leg differences are not evidence.
**Gate (amended 2026-08-01):** (a) DONE before batch close ✓; (b) DEFERRED with
reason — runs after (c), from (c)'s output; (c) before the next full 30-leg
pair is described as the pipeline's official state.
**8.12(a) outcome (9ef2ec9 — DONE 2026-08-01): risk 3 upgraded from "unobserved"
to CONFIRMED REAL.** `judge_decision.py` has no `bug_kind` reference and the
semantic guard does not enclose either `adjudicate()` call site — crash legs reach
the 6B/6C gates, where the polarity inverts (fires-on-both = semantic drop
condition = crashing CATCH condition). Pinned: 6C duty-NO drops a crash catch,
duty-YES spares, duty-error fails open ✓; 6B inert without a rate block ✓, drops
with one. Crash-catch safety rests entirely on family-duty answering YES — a judge
answer, not a guarantee; now documented, and the exposure doc corrected. 8.12(c)'s
per-leg rerun must read the 6C/duty events on every crash leg.
`repair_rethrow_without_cause` withdrawn as a risk BY TEST (attaches the cause,
never strips one). 573 passed, 7 skipped.
**8.12(b) outcome (f37f7f3 — DEFERRED 2026-08-01, with reason): the corpus does
not exist.** crashcheck predates the one-file-trace change — 0 of 14 legs carry a
trace.md, and its run.log format has no ACCEPTED/REJECTED markers or fenced
sources, so rejected harnesses are unrecoverable. crashtrace1 (current format)
extracts cleanly but yields n=0 repairable defects. The rule-15 assert is the only
reason this reads as BLOCKED instead of "0 regressions" over an empty corpus.
**Dependency INVERTED:** (b) runs AFTER (c), from (c)'s current-format output —
(c) is now a corpus source as well as a measurement. Residual coverage stated
honestly: compile safety established on 111 semantic pairs (transforms are
kind-agnostic); cause-signature preservation pinned by (a); the crash-leg
repairable-defect RATE is unmeasurable from any existing archive.

### 8.13 Split the `not-applicable` gate bucket — small, ships with next build
**Target:** the trace events around `indiscriminate_buggy_rate()` / the 6B/6C gate
call sites in `judge_decision.py` (station 6→7 boundary).
**Failure mode:** the final30 analysis's own top-priority finding, dropped from this
plan until now: the gates' `not-applicable` covers three different situations —
never measured, buggy side unmeasured, measured-and-healthy — and the event text
reads as the first. ~90% of the 161 gate-reaching rule-firings in the pair landed in
this bucket, which is why the rule-diversity claim had to be retracted: with the
bucket unsplit, no statement about where recall is lost is trustworthy.
**Steps:** three distinct event reasons, observability only (no decision change);
population-pinning test over the pair fixtures; read-out rides the next live run.
**Outcome (2026-08-01, rung-2 audit): ALREADY SHIPPED in cycle 7** — the
five-state rate split (`RATE_STATES`, `evidence_facts.py:1978`: no-measurement /
buggy-side-unmeasured / below-bar / catch-profile-skipped / at-or-above-bar) plus
the 6C two-way split. This item was written before the audit noticed; no new code.
The rung-2 smoke verifies the states appear on the LIVE path rather than assuming
cycle-7's tests cover it.

### 8.14 Miss ledger — free, BEFORE any recall lever is chosen
**Target:** archived traces of final30A/B + night20c (the current-config runs);
recall side.
**Failure mode:** the precision side has its 7-case decisive-fact table; the recall
side has no per-miss station-of-death ledger under current config. The live recall
candidates (8.5 budget, 8.9 persistence, the focused-synthesis re-adjudication,
8.17 invariant mining) target DIFFERENT stations; choosing among them without this
ledger is guessing.
**Steps:** for every missed overfit leg in the three runs, classify from the trace:
family never invented / invented but died at harness construction / built but never
fired / fired but judge-dismissed — with the trace line. One table, committed raw
before interpretation. Feeds the 8.5 read-out (including its focused-synthesis arm
decision) and the 8.17 build/no-build gate.
**Outcome (`docs/replay/backtrack/8.14-MISS-LEDGER.md`, raw table committed first —
DONE 2026-08-01, CORRECTED same day after a column bug: the fired column had
counted only harness-track events; the table is now split by track):** 14 missed
leg-instances: fired-ALL-dismissed 8, built-never-fired 3, fired-mixed 2,
fired-never-judged 1, died-at-construction 0. The dominant miss station is the
JUDGE: **10 of 14** misses reach a judge and are dismissed (wholly or partly) —
recall lost to OVER-dismissal, the mirror of the precision ceiling's
under-dismissal; the reach class (3) is smaller than the judge class by more than
3×. Same component, opposite directions: no single strictness knob can fix both.
This replicates the cycle-5 inventory finding (22 of 23 FN legs all-UNSOUND) under
current config. Stated limitation: the ledger is LEG-level and cannot see "never
invented" (it would hide inside the built-never-fired legs); the two known
winning-family fates on record ARE invention-shaped (Chart-19's family died at
harness construction per 2d; Lang-63's is a roll lottery per a75012d) — so the
invention items are DEFERRED to 8.14b, not killed.
**Follow-up 8.14b (free, REQUIRED):** per-missed-leg winning-family fate read — was
a decisive family proposed, screened, built, fired? 8.14b, not 8.14, now gates
8.5's third arm and 8.17.
**8.14b outcome — RETRACTED same day (`docs/replay/backtrack/8.14b-FAMILY-FATE.md`,
retraction 8f86ac4): the count is method-dependent and therefore no count.**
Changing only the string-matching threshold inverts the finding (≥2 shared name
tokens → 5 of 11 never-proposed; shared leading class token → 1 of 11). The
Chart-19/2d reconciliation resolved as a VERIFIED FALSE ABSENT: the winning family
(`categoryplot_getRangeAxisIndex_null_rejected…`) shares only one token with the 6
categoryplot-* proposals final30A actually made. LLM-assigned rule names drift
between rolls with no stable vocabulary — string matching over them cannot resolve
whether a conceptual family was proposed. **The invention gate is UNRESOLVED:
8.17, 8.9, and 8.5's third arm are neither licensed nor killed.**
What survives: the joint OVERLAP reading with 8.14 (judge is where misses are
observed; some outcomes fixed upstream; the two levers are disjoint); Chart-19's
family WAS proposed and died at construction (two independent routes agree with
2d); final30A Lang-63 is the strongest never-proposed candidate (4 winner
families, 0 matches even under the loose rule); Math-104 = family-unobservable.
**Follow-up 8.14c (free, a reading job — resolves the gate):** semantic
adjudication per missed leg: does any proposed rule assert the SAME PROPERTY as
the winning check, regardless of name? Pre-commitment: write down each winner's
asserted property (observable + relation) for all legs BEFORE opening any missed
roll's proposal list, and commit that property table first — raw-before-
interpretation applied to a reading job.
**8.14c outcome (DONE 2026-08-01, properties frozen first in dbd8017): invention
is the MINOR station, and the archival gate design cannot work.** Against the
frozen definitions: proposed 7 · not-proposed 1 (final30B Lang-60 — nothing
asserts contains() non-mutation) · ambiguous 3. So 1 confirmed never-proposed, at
most 4 if every ambiguous row resolves against — versus the judge class at up to
10 of 14. Chart-19 splits across rolls (proposed in B, ambiguous in A) and died at
construction, not invention. The 3 ambiguous rows share one shape — a proposed
rule on the SAME observable with a narrower/adjacent relation — and whether those
would have caught depends on what the generated harness exercised, a
post-invention station: **the invented-vs-too-weak boundary is not drawable from
the archive.** Consequence: 8.9 / 8.17 / 8.5's third arm re-gated from archival
evidence to a LIVE A/B (does persistence/mining change the catch rate), priced
against an expected-payoff class of 1–4 legs. Reader judgment disclosed in the doc
(an alternate reader gets 7/0/4; both conclusions unchanged).
**Math-104 (3 instances, "never caught"):** not a lever-less class — 2 of its 3
instances are fired-and-dismissed rows, i.e. inside 8.18's read population, and
the step-1 adjudication (1d02859: drift-kill B, most borderline; dev-fix 6.77e-15
satisfies, overfit 8.2e-10 violates) is on record. 8.18's read of its dismissing
verdicts decides which it is: merited near-tolerance dismissals (→ honestly
unwinnable at current tolerance; the parked floor question) or drift (→ the judge
lever covers it).

### 8.15 Authority-tier separating study — offline, free, after 8.14
**Target:** the kept verdicts of the final30 pair (67 genuine-catch / 23
false-accusation); evidence assembly in `run.py` only IF it separates.
**Failure mode:** the separating-fact study tested delivery-side features
(corroboration, firing location, rates, replay confirmation) — none separate. It
never tested WHICH AUTHORITY the checked property derives from: the failing test's
pins / the buggy build off-defect / a grounded doc quote / the model's memory or
the patch itself. The chronic accusations assert remembered contracts (Math-65's
inverse formula, CITATION: NONE); the trust hierarchy exists today only as prompt
guidance — the channel the meta-rule says is weak. 8.6 added a HARD requirement
(2026-08-01): tier alone mislabels Math-39 — its accusation invokes tier 1 and is
entitled to; what fails is SCOPE. The study must classify authority-tier AND
authority-scope (does the invoked authority's pinned property extend to the firing
input? Math-39: exact test setup, but two fuzzed values leave the pinned scenario).
**Steps:** classify each kept verdict's property (mechanically where possible:
test-literal match, doc-quote grounding, buggy-replay presence; by hand where not);
commit the table raw; read separation against BOTH populations — a tier that voids
guard-set catches fails, per the guard rule. Separates → design a computed
authority-tier fact. Doesn't → record and close, same standard as the separating
study. Side product: an ADDITIVE kept-verdict fixture from the pair rolls
(cases228 stays untouched — 8.1 is pre-registered against it).
**Outcome (`docs/replay/backtrack/8.15-AUTHORITY.md` — DONE 2026-08-01, CLOSED
NEGATIVE on both dimensions):** authority tier does not separate (model-memory /
unattributed 58% FP vs 60% TP; documentation 29/28; failing-test pins 12/12) and
scope fares no better — the largest gap (13 points, replay-confirmation) runs the
WRONG way for a precision gate (genuine catches are MORE replay-confirmed).
Math-39's exact shape appears at 2 rows vs 1 — not a measurement. No computed
authority fact is licensed; the guard question is moot (no candidate to test).
The ceiling now rests on THREE independent negative dimensions: delivery features
(cycle-7 separating study), narrow structured questioning (engagement experiment),
and authority (this). Limit honestly stated: both dimensions were keyword-
classified over rationales; a real scope measure needs the firing input compared
against the test's pinned scenario — 8.3's territory. If 8.3 ships, ONE re-ask of
the scope dimension is licensed; otherwise this question is closed as askable from
the current record.

### 8.16 Passing-suite behavior-delta fact — design note only, after 8.2
**Target:** a new evidence generator (station 6 peer of `relation_screen.py`);
judge evidence assembly as consumer.
**Failure mode:** the separating study's honest close — the accusation side may
need a different KIND of evidence than we collect. One label-independent kind is
measured nowhere in our pipeline: how much the patch disturbs the behavior of the
project's PASSING tests. Published as PATCH-SIM (ICSE 2018): correct patches barely
move passing-test behavior, overfit patches move it more; a later replication found
moderate power, so this enters as a computed FACT for the judge, never a gate.
**Must price:** trace-capture cost on Defects4J-scale suites; the moderate power;
interaction with the family-duty boundary (legitimate fix propagation also moves
behavior). Guard set before mechanism. Pointers:
`docs/related-work-scan-2026-08-01.md`.

### 8.17 Deterministic invariant mining as a rule source — design note only
**Target:** `relations/relation_synth.py` (station 2) — a second, DETERMINISTIC
candidate source feeding the same screen → replay machinery.
**Failure mode:** the invention lottery is the measured recall-variance driver
(RELIABLE 6 / COIN-FLIP 4 / INVENTS-BUT-NEVER-FIRES 4; Lang-63's winning family
present one roll, absent the next). LLM synthesis is currently the ONLY candidate
source, and it re-rolls per leg. Daikon-style invariant mining over buggy-build
executions is deterministic — same leg, same candidates, every roll (Invalidator,
TSE 2023, adapted: their developer-patch side is firewalled; only buggy-side
inference is admissible).
**Must address:** the mined54 precedent (a mined flood cost Lang-7's TP — mining
feeds the screen, never bypasses it, and survivors compete for the same
`--synth_max_rules` slots); template reach on our observable shapes; interaction
with 8.9 (both are anti-lottery mechanisms — never measured together). Gate re-set
by 8.14c (2026-08-01): archival reading CANNOT decide this (the invented-vs-
too-weak boundary is not drawable from traces); build only behind a live A/B
priced against the measured class — invention is the minor station (1 confirmed +
3 ambiguous of 14 misses). Design constraint from the 8.14b retraction: family
identity in this mechanism must be defined SEMANTICALLY (asserted property), never
by generated name string.

### 8.18 Dismissal-side authority study — free, the largest miss class's first lever
**Target:** the 8 judge-dismissal miss instances in 8.14's ledger (station 7,
dismissal direction); `relation_verifier.verify()` dismissing verdicts.
**Failure mode:** 8.14 found the largest miss class — genuine catches dismissed by
the judge — has NO queued lever. Cycle-5's drift-kill read (uncorroborated "a
correct implementation could…" hypotheticals, wrong in 4 of 6 measured instances)
predates every current gate; whether it still describes these 8 under current
config is unmeasured.
**Steps:** mirror 8.15 on the dismissal side: for each dismissing verdict on the 8
instances, classify the counterexample's authority (grounded in shown source /
docs / buggy behavior — or hypothetical, CITATION: NONE) and its scope. Committed
raw. If ungrounded hypotheticals dominate, the lever candidates are 8.2's
scope-note mechanism (ground the hypothetical against buggy/docs) and the 8.1
outcome; if grounded dismissals dominate, the loss is upstream of any judge fix
and the class is honestly closed.
**Guard (INVERTED):** any keep-pushing mechanism this licenses must show
~zero cost on the correctly-dismissed firings of correct legs BEFORE anything else
is measured — the two-directions finding makes this the sharpest guard rule in the
plan: the same component is failing both ways, so a fix for one direction is
presumed to damage the other until measured. That guard population does NOT exist
yet as a fixture (the 67-row set is genuine catches, not correct dismissals) —
building it is 8.18's FIRST step, before any classification is interpreted
(standing rule 2).
**Outcome (`docs/replay/backtrack/8.18-RESULT.md`, categories frozen first in
c677a36 — DONE 2026-08-01, CLOSED NEGATIVE):** 34 dismissals classified: grounded
refutation 20 · wrong-family (couldn't convict anyway) 5 · near-tolerance 6 ·
setup divergence 1 · **ungrounded hypothetical 2 (6%) — the only lever category,
below the pre-registered bar.** The dominant miss STATION is not a dominant miss
CAUSE: in 94% of dismissals the judge is correctly dismissing something that
should not convict (several refutations cite fires-on-both — the cycle-6
machinery working as designed). Guard question moot — nothing licensed to test
against the 38 rows. Math-104 SETTLED: nine dismissals, grounded-refutation and
near-tolerance, none ungrounded, repeatedly citing DEFAULT_EPSILON = 10e-9
against the harness's 10e-15 comparison — a harness defect, not a judge failure;
its class is merited near-tolerance dismissal, joining the parked tolerance-floor
question; step-1's borderline-B prior is confirmed-as-near-tolerance, and the
judge lever does not cover it.

### Execution ladder (replaces the sequencing paragraph, 2026-08-01)

Ordered by effort-vs-expected-effect: free information first (it re-ranks
everything below it), then tiny code that protects every later measurement, then
the one licensed precision fix, then paid experiments, then gated designs and
builds. Honest expectation-setting for the ordering: precision is ceiling-capped
(~5) under the current architecture, so the biggest expected SCORE movement is
recall-side (rungs 1 and 4); the biggest RISK reduction is measurement-side
(rung 2). Each rung ends with the check that says whether its method is good —
nothing below a rung runs until its check is read.

**RUNG 1 — free reads, zero code (today, ~0 tokens).**
8.6 Math-39 event chains · 8.14 miss ledger · 8.15 authority-tier study.
Why first: each produces a committed table that re-orders the rungs below —
8.14 gates 8.5's third arm and 8.17; 8.15 either licenses the only free
precision candidate or closes it; 8.6 settles the historically-clean-leg list
the PASS bar depends on.
**CHECK:** tables committed raw before interpretation; 8.14 names the dominant
miss station; 8.15 is read against BOTH populations (a tier that voids guard
catches fails).
*Rung extended 2026-08-01 by 8.14's outcome:* 8.6 DONE (fourth residual,
fully closed), 8.14 DONE (dominant miss station = the judge; construction
death zero), 8.14b RETRACTED same day (name matching cannot resolve the
invention gate), 8.14c DONE (invention is the MINOR station: 1 confirmed,
≤4 of 14; the archival gate design cannot work — invention levers re-gated
to live A/Bs, deprioritized below the judge thread; A/B-undetectability
priced in 3d7414e), 8.15 DONE (CLOSED NEGATIVE on tier and scope — the
ceiling's third independent negative dimension; one re-ask licensed if 8.3
ships), 8.18 DONE (CLOSED NEGATIVE: 94% of miss-side dismissals are correct
judgment; ungrounded hypotheticals 2 of 34; Math-104 settled as merited
near-tolerance; the correct-dismissals guard built as a permanent asset on
the way).
**RUNG 1 COMPLETE (2026-08-01). Check: PASSED** — every table committed raw
before interpretation, both pre-commitments honored (frozen properties,
frozen categories), one retraction executed same-day, both guard
populations tested. Net result: every recall station measured, no large
addressable class; the precision ceiling triple-confirmed; every cheap
alternative to 8.1 is now measured dead — 8.1 is the single largest open
question and rung 4's case for it is at maximum strength. Proceed to
RUNG 2.

**RUNG 2 — tiny code that protects every later measurement (one mini-batch;
~10–50 lines each; no behavior change on semantic legs).**
*Contents updated 2026-08-01 by the rung-2 audit:* 8.8 label assert (DONE,
fd67182 — refuses both mismatch directions) · 8.7 repair marker field (DONE,
ad41e38 — provenance at acceptance, one lookup) · 8.12(a) DONE (9ef2ec9 —
gates confirmed kind-blind, exposure doc corrected) · 8.12(b) DEFERRED with
reason (f37f7f3 — no corpus exists; runs after 8.12(c), from its output).
8.13 was found ALREADY SHIPPED in cycle 7 (five-state RATE_STATES split) —
struck from the rung; the smoke verifies it live instead. Standing rule 15
(guard inputs must exist) promoted out of this rung's lessons and landed
twice within the rung. The rung closes on the 2-leg smoke.
**CHECK (smoke-before-pair rule, amended 2026-08-01):** one 2-leg smoke
(~300–500k) on the batch build. The mislabel criterion CANNOT be tested by a
live run — a refusing launch produces no run to inspect; 8.8's evidence is
the committed offline both-directions test (exit 2 each way), and the gate
claims no more than that. The smoke tests three things: the repair marker
live on a repaired-accepted harness; the five-state rate split observed on
the LIVE path; zero verdict changes vs the smoke7 baseline (Closure-62 TN,
Math-65 FP). Authorized 2026-08-01.

**RUNG 3 — the one licensed precision fix (small code + one prompt rule).**
8.4 raw-value recording (compare normalized, RECORD raw; ~17% of harnesses).
**CHECK:** 20-generation compliance smoke — do fired messages carry both forms?
Then the extractor extension measured ALONE on cases228: no verdict may flip
except via newly-possible legitimate matches; Closure-62's rows are the named
target (today all `unknown`; success = any move to `matches`/`differs`).

**RUNG 4 — paid experiments, cheapest information per token first.**
1. 8.1 judge swap + REQUIRED noise floor (~2–3M total; launches only on the
   user's phrase) — the biggest open question: is the ceiling the architecture
   or the model? Read-out pre-registered (per-case flips vs the floor).
2. 8.5 relation budget −m 12 vs −m 16, plus the now-8.14b-gated
   focused-synthesis third arm (~1–1.5M) — demoted 2026-08-01 by 8.14's
   outcome: read as the Chart-19-specific budget question, not a general
   recall-lever test.
   **CHECK:** invention rates of known winning families per leg, from traces,
   never pooled scores; two-roll rule for any claimed flip.
3. 8.12(c) crash14 rerun (~1–2M) — **CHECK:** per-leg vs the 07-16 baseline
   (P=0.86 R=0.86); runs before the next pair is called the official state.

**RUNG 5 — design docs, free, written only after the rungs above have
reported (their content depends on it).**
8.2 (after 8.1; cites 8.14/8.15 where relevant) · 8.9 · 8.16 and 8.17 (only
for the miss classes 8.14 shows are material).
**CHECK:** every doc carries a guard-set test plan and priced reach; user
decides build/no-build per doc — a design doc with no guard plan is returned,
not built.

**RUNG 6 — gated builds.**
8.3 buggy-value collector (after 8.1/8.2 confirm direction); validation rides
the next live run passively — no dedicated run.

**Anytime:** 8.11 housekeeping (zero risk) · 8.10 pre-commitment upkeep
(continuous).

**FINAL GATE (unchanged):** assemble the cycle-8 batch → smoke on the final
build → the full 30-leg pair (~7M, user's word) against the 8.10 bar →
fresh12 decision (user's literal phrase only).

## The two guard populations (permanent, versioned, population-pinned)

Standing rule 2 (*guard set before mechanism*) is only mechanically enforceable if
the populations are frozen assets. Both now are, pinned by
`tests/test_guard_fixtures.py`.

| guard | rows | what it holds | guards against |
|---|---|---|---|
| genuine catches | 67 | kept alarms on legs that ended as real catches | any **dismiss-pushing** mechanism — voiding these destroys recall |
| correct dismissals | 38 | dismissed alarms on CORRECT-patch legs | any **keep-pushing** mechanism — overturning these manufactures false accusations |

`tests/fixtures/correct_dismissals.jsonl` — 38 rows across 10 bugs and both rolls
of the final30 pair. Definition: on a correct patch the alarm IS a false alarm, so
the dismissal was unambiguously right. 35 of 38 carry a citation.

**Why the second guard had to exist:** 8.14 measured the judge as *both* the
largest recall class (over-dismissal, ~10 of 14 misses) and the capped precision
component (under-dismissal). The 67-row set cannot guard a keep-pushing fix — it
holds keeps. Every future keep-pushing proposal (8.18's output, 8.2's
dismissal-grounding scope note, anything 8.1 licenses, any drift-kill fix) is
**presumed to damage precision until measured against this fixture**.

Population pins carry the fix-(ii) lesson: if a count moves, studies built on it
are re-run, not inherited — fix (ii)'s numbers expired when its population went
from 10 rows on one leg to 27 across three.
