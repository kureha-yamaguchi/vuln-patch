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
- **Judge-model swap as a precision lever (8.1, CLOSED 2026-08-01)**: per-case
  flip rate sits at the same-model noise floor (9.2%); the real difference is a
  shifted dismissal threshold (extra keeps ~1:1 catches:false-alarms, accuracy
  unmoved, would blow the ≤5 accusation cap); the frozen contradiction question
  fails identically in count AND cases across models. The ceiling is
  architectural — model-independent. Do not re-propose absent a different
  failure shape, which the pre-registered decision table already defines.
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
   original value (the whitespace lesson; cycle-8 item 8.4 implements it). Same
   principle for MEASUREMENT inputs (2026-08-01): a truncated record is a display
   transform of the original — measuring over it deflates counts for late-message
   content (the struck 38% compliance figure). Tools reading records must refuse
   to answer on truncation, never undercount; measure at source where possible.
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
    directions, not pass counts. Seventh instance (2026-08-02, the batch
    smoke): inputs must not merely EXIST — they must ARRIVE. 8.4's Raw keys
    were emitted, recorded, and stripped in transit by a 200-char display cap
    before the comparison saw them; every piece was verified in isolation and
    the journey never was. Existence is a property of the producer; arrival is
    a property of the journey; only an end-to-end run tests the journey.
    Fifth instance (2026-08-01): 8.4's planned
    67-row guard measurement — archived rows cannot carry the new Raw keys, so
    the dismiss-pushing comparison would no-op on all 67 and the guard would
    pass unexercised; claims re-scoped per population. Corollary applied since:
    choose the test population so the mechanism CAN fire (the compliance smoke's
    formatter legs), rather than accepting whatever ran.
16. **A zero-delta criterion is only meaningful against a baseline differing by
    exactly the change under test.** Otherwise it measures accumulated drift and
    reports it as regression — the mirror of rule 15: there a check passes while
    testing nothing, here one fails while testing something else. Corollary of
    rule 3 (measurements expire) applied to GATE CRITERIA: before writing "zero
    changes vs baseline X", verify X differs from the build under test by only
    the change the gate exists to check. Origin: the rung-2 smoke's criterion 4
    (2026-08-01) — its baseline predated three verdict-affecting cycle-7 commits,
    and Closure-62's flip was drift inside the measured 27%, not a rung-2 defect.

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
  29/28, 12/12; the largest scope gap runs the wrong way); and the ceiling is
  MODEL-INDEPENDENT (8.1, 2026-08-01: flip rate at the same-model floor, part B
  failures identical in count and cases). Four independent dimensions tested,
  four negatives — the ceiling is architectural, confirmed. Remaining directions
  are evidence-side only (8.2/8.3, plus 8.4 now shipped).
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
  measurements. NOTE (2026-08-01): 8.4's raw-vs-pinned comparison is DISTINCT and
  does not unpark this — fix (ii) compared arbitrary tokens on any evidence
  (measured a wash); 8.4's comparison asks only raw-output-vs-test-pinned-literal,
  guard-measured on its own. The park condition here stands.
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
   **Step 1(b) DONE pre-spend (b7e5d40, 2026-08-01) — and it found the hole it
   existed to find.** Two producers passed `why or "<sentinel>"`, substituting the
   model's own text for the sentinel whenever a response carried a WHY: line
   without a parseable verdict — usable=True on garbage. In `_parse` that fails
   open to KEEP; in family_duty's inline parse it reads as a deliberate
   "duty does not apply", the direction that COSTS a finding. Model-swap
   sensitive by construction: the incumbent's format compliance made the sentinel
   appear by luck; a model that preambles, reorders VERDICT:/WHY:, or truncates
   after WHY would fail open on every such call — the swap's per-case flip
   read-out would have measured its own parser. Fixed: sentinel emitted
   unconditionally, model text appended, never substituted. 617 passed, 7
   skipped. Error path settled WITHOUT a probe: exceptions are wrapped into our
   own `verifier error ({e})` prefix, so the sentinel keys on our wording, not
   the provider's — "add its error format" has nothing to add; the exposure was
   never the provider's strings but our own suppression. Rule 15, sixth
   instance: a guard whose input can be SUPPRESSED by the very text it guards
   against is fail-open while looking armed.
   **Still gated on the user's phrase: step 1(a) probe, parts A and B, the noise
   floor.**
**8.1 DONE AND CLOSED (5971360, 2026-08-01; raw committed before scoring, floor
read before part A): SAME FAILURE SHAPE → the ceiling is ARCHITECTURAL. The
model question is closed.**
- Floor: the incumbent disagrees with ITSELF on 21/228 (9.2%) between identical
  draws. gpt-5.5 vs draw0: 23/228 (10.1%); vs draw1: 32/228 (14.0%) — the flip
  RATE sits at the floor.
- The flips are not randomly signed: floor 8 drop→keep / 13 keep→drop (sign test
  p=0.3833); gpt-5.5 21 drop→keep / 2 keep→drop (p=0.0001). gpt-5.5 dismisses
  less: recall 81.7%→95.8%, specificity 79.6%→74.5%, extra keeps 10 genuine
  catches + 9 false alarms (~1:1). (Caveat added 2026-08-02: on 34% of rows
  the judged alarm headline was a 201-char truncated prefix — full evidence
  alongside; same-shape conclusion unaffected, absolute figures carry this
  note. See the cases228 substrate repair under 8.3.) A SHIFTED DISMISSAL THRESHOLD, not better
  discrimination; accuracy unmoved — and a threshold does not require a model
  swap to buy. (On the pipeline's own bar, +9 accusations would blow the ≤5
  cap — not adoptable as-is even if wanted.)
- Part B: fails all three criteria IDENTICALLY to the incumbent — 2 vs ≥7
  contradiction hits; 10 wrong guard voids (85.1%) each; 5 non-contradiction
  voids each, landing on the SAME wrong cases (Closure-62, Math-30). Both
  models produce plausible quotes, not real contradictions.
- Pre-registered decision applied: same shape → record and close. NEITHER
  follow-on opens (future-model re-test and cross-model agreement were both
  conditional on a different shape).
- Cost: ~8.5M, of which ~3M lost to two harness faults (floor directory
  collision; part B deadlock), one shared cause: work held in memory until the
  end is work that can be destroyed. Part B runner now flushes per case and
  resumes by skip. `verifier_replay` still TRUNCATES an occupied output
  directory rather than refusing — recorded as a guard to add, deliberately NOT
  slipped into the closed batch.
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
`record_event`; extend `compare_fired_values` consumption where applicable.
**Step 1 DONE (55c39a7, 2026-08-02):** before writing new code, the function 8.3
depends on was checked — and `_extract_oracle_msg`, the buggy-side twin of the
path the batch smoke fixed, carried BOTH cutters (200-char cap AND
first-newline stop), each demonstrated to lose actualRaw= entirely. Same
selection effect as before: until 8.3, nothing downstream ever wanted the
message's tail, so a bug visible only at the tail could not surface. Fixed and
pinned (8 tests; 643 passed, 7 skipped). Building 8.2/8.20 first would have fed
both of them truncated values — the cheapest-first ordering earned its keep on
day one. Remaining: the recording half (thread values via `record_event`), then
offline tests from archived raw output where any exists. NOTE: 8.3's passive
validation now rides the pair — the pair gains a FIFTH read (does the value
channel populate live?), and 8.2/8.20 land against a channel whose first live
exercise is that same run.
**The sweep found a FOURTH cutter instance — in the offline substrate itself
(d1e7b93, 2026-08-02): 78 of 228 cases228 rows end in an ellipsis at exactly
201 characters** — the 200-cap's fingerprint; every offline measurement over
message CONTENT has been reading prefixes. Repaired NON-mutating into
`tests/fixtures/cases228_untruncated.jsonl` (cases228 untouched, so all prior
recorded numbers stay comparable to the substrate they were computed on):
70 repaired from archived traces, 8 permanently truncated (the archive stored
the capped form too — `_still_truncated=True`, flagged never silently kept;
this is 8.21(a)'s cost made concrete and raises its priority). Three build
errors caught pre-ship, disclosed: an [oracle:] search matching inside
prompts/harness source (rule 8's inflation direction — first time it nearly
reached a VERSIONED asset; caught because 3000 chars was the window size, not
a message boundary; extraction now alarm-scoped); a stripped
FuzzerSecurityIssueLow: prefix changing row shape (restoring it also took
recovery 36→70); two shape-test drafts narrower than the data's four
legitimate shapes (the data was right both times).
**Consequences:** 8.20 validates against `cases228_untruncated.jsonl` MINUS
the 8 flagged rows. And one cheap check BEFORE that validation: list which
prior offline studies read message CONTENT (vs row metadata) and whether any
of the 7 decisive-case rows sit among the 78 — model-comparison conclusions
(8.1) are internally consistent either way (both models saw the same
substrate), but any content-dependent claim on a truncated decisive row
deserves a one-grep re-check. 651 passed, 7 skipped.
**Check DONE (2026-08-02) — clean for the claim class that mattered, with
bounds:** truncation is confined to ONE field, `fired_assertion` (78/228 at
max 201 chars); `concrete_evidence` (max 7,187), `code_context` (max 47,913),
`failing_test`, `harness_source` all whole. "Delivered and ignored" rests on
evidence/context, not the headline — Math-65's code_context is 28,207 chars,
the exact text the character-27,051 read was performed against; all 12
Math-65 rows carry full evidence. BUT 36 of the 78 clipped headlines sit on
the 7 decisive bugs (Math-73 9, Math-30 8, Closure-62 7, Lang-60 7, Math-65
3, Chart-26 1, Math-39 1); 31 repaired, 5 permanent including one Math-65 and
one Math-39 row. Per-consumer bounds: 8.14/8.15/8.18 read judge OUTPUT blocks
in traces — unaffected (nuance kept: a trace holds both capped alarm records
and uncapped judge output, so "verified against traces" is not
self-certifying; these are clean because they read the latter).
`verifier_replay` passes fired_assertion straight to the judge
(verifier_replay.py:257), so 8.1 part A and the floor judged a 201-char alarm
prefix on 34% of rows WITH full evidence alongside — the same-failure-shape
conclusion stands (both models, same substrate, complete evidence); the
absolute figures (80.3%/81.7% etc.) carry this caveat when cited. STANDING:
all future replays use `cases228_untruncated.jsonl` minus the 8 flagged rows;
the original is never corrected in place.
**Recording half DONE (2ddffe1, 2026-08-02; 659 passed, 7 skipped).** Two
pieces: `observed_values(msg)`, a STRING-PRESERVING key=value extractor,
deliberately wider than the numeric-only `_kv_values` — one extractor per
consumer, because recording is not judging (a value the numeric comparison
can't parse is correctly invisible to IT, but dropping it from the RECORD
would discard exactly what 8.2's screen and 8.20's scope fact consume); `{}`
means nothing-recorded, never nothing-existed. And a `record_event` at the
buggy-replay site carrying buggy/patched values, the value verdict, whether a
buggy message existed, and replay status — recording only, no verdict reads
it, wrapped so it cannot break a run, pinned by a wiring test.
Silent-on-buggy stays valueless by construction (fails SAFE — arbitration
abstains). Yield on the repaired substrate (220 rows): ≥1 recorded value 127
(58%); ≥1 numeric 85 (39%) — the string extractor ADDS 42 rows, half again
the coverage; 43 of the 70 repaired rows yield more keys than pre-repair
(the substrate work and this work COMPOUND). **Remaining, ordered BEFORE the
8.20 handoff:** mirror the recording at the muted-replay site (run.py ~2811,
`_cfv2` computes the same values, records nothing) — a one-path channel is
rule 15's half-armed shape, and worse for 8.20 specifically: muting exists
FOR the shadowed chronic legs, so a one-path handoff would bias 8.20's
validation against exactly its target rows.
**8.3 COMPLETE (250123e, 2026-08-02): muted-replay mirror done — the value
channel records on both buggy-side paths.** 8.3 earns its keep independent of
8.20's death: it is 8.2's prerequisite, and the channel is real. Passive live
validation rides the pair (fifth read). Offline
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
**Prompt half DONE (6cfb5c9, 2026-08-01):** fired messages now carry named keys
`expectedNormalized= actualNormalized= expectedRaw= actualRaw=` — named, never
positional (the consumer is the setup-divergence extractor, where a wrong-field
read is rule-15's family in a component whose failure mode is "the rung stays dead
and nobody notices"). Raw keys emit ONLY when normalization happened, so absence
means no-normalization, never forgot-to-record. Four tests pin the change,
including that the normalize INSTRUCTION is untouched — 8.4 adds recording and
must not relax the comparison. 577 passed, 7 skipped.
**Still owed — sequence REVISED 2026-08-01 after testing the intended consumer
(two dependencies the design missed):**
(1) The rung cannot read what 8.4 produces: normalizing checks are by definition
over strings, and `fired_value_vs_trusted` is NUMERIC-only — verified on
Closure-62's exact shape with full Raw keys, `_fired_numbers` returns `[]`,
verdict stays `unknown`. Extending the extractor alone would ship a no-op (the
fourth mechanism this cycle caught inert BEFORE shipping). What is needed is a
raw-vs-pinned comparison. **This is NOT fix (ii) revived** — fix (ii) compared
arbitrary tokens on any evidence and measured a wash; this compares the raw
output against the literal the test pins, the rung's exact question, answerable
exactly. It is a new mechanism increment and needs its own measurement against
the 67-row guard.
(2) cases228 cannot validate the extractor half: 0 of 228 rows carry a Raw= key
and none CAN (the field postdates every archived case). cases228 is demoted to
regression-only; the compliance smoke's output is the ONLY corpus in which the
extractor can be validated — load-bearing, not a formality.
**Revised sequence:** compliance smoke (~20 generations, authorized 2026-08-01;
launched as c84_20260801_174840 at 298d9d9, formatter/printer legs CHOSEN so the
conditional Raw keys can actually appear — a numeric leg would exercise nothing
and produce a clean, meaningless zero) → build the raw-vs-pinned comparison →
validate, with claims SCOPED PER POPULATION (2026-08-01, after the step-3 guard
claim was caught vacuous — archived rows cannot carry Raw keys, so the comparison
no-ops on all 67 and the guard would pass unexercised; fifth rule-15 instance):
archived 67-row guard = REGRESSION ONLY (does not fire where Raw is absent);
compliance-smoke output = keys emitted + first actionable inputs, NOT a guard
population; the NEXT LIVE SUITE = the real guard, read per-event before the
rung's verdict surface is trusted. Until then the comparison ships as
observably-inert-on-old-data — a regression claim, never a safety one. cases228
regression only. Honest expectation unchanged: this makes a dead rung LIVE; it
promises no verdict movement.
**Compliance smoke result (c84_20260801_174840 — 2026-08-01, CORRECTED same
day): mechanism PROVEN, compliance 100%.** A first read reported 38% — STRUCK: it
was a truncation artifact. 12 of 24 fired-alarm records are visibly truncated
(ending in …), and the count read the alarm's RECORD instead of the alarm itself,
scoring any alarm whose Raw keys fell past the truncation point as
non-compliant. Measured at source level, which truncation cannot affect: 46 of 46
alarms reporting a Normalized value also report both Raw keys; the acceptance
lint agrees independently (0 violations across all 18 recovered harnesses, 0
false flags). The thesis demonstration stands: `expectedNormalized=x--0.0
actualNormalized=x--0.0` identical while `expectedRaw=x- -0.0 actualRaw=x--0.0`
differ — the raw record preserved exactly the Closure-38 separator-space defect
normalization erased.
**Iteration-2 authorization WITHDRAWN UNUSED** (no re-smoke, no ~400k). The lint
still ships — not to fix compliance but so absence-of-Raw is trustworthy BY
CONSTRUCTION for the extractor's three-state read, independent of any one roll's
rate. Remaining 8.4 work: the raw-vs-pinned comparison (claims stay scoped per
population as above). Correction carried forward: any measurement over
fired-alarm RECORDS is unreliable for late-message content — the record is a
truncated copy, the inverse of the four contamination inflations;
`count_in_fired_alarms_only` gains a truncation guard that REFUSES to answer on
ellipsis-terminated records rather than undercounting (rule 8's principle: a
display transform must not silently stand in for the original).
**Shipped (b59ba3b): guard raises `TruncatedRecord` (silent undercount is
indistinguishable from real absence — the whole lesson); explicit
`on_truncation='count'` opt-out for callers who've established the needle can't
appear late; `tests/test_record_vs_thing.py` pins both directions (inflation:
prompt/constructing text not counted; deflation: truncated record refused) plus
the lint's three cases. Struck reasoning kept legible below the corrected design,
not deleted. 585 passed, 7 skipped.
STATUS: lint is built but UNWIRED — a detector nothing calls guards nothing
(rule 15's shape, named rather than left sitting). Wiring into acceptance is the
immediate next step, BEFORE the raw-vs-pinned comparison build.
→ WIRED (427ccd5): gate 0c2 at acceptance (`campaign.py:601`), 589 passed 7
skipped. The rejection diagnostic is load-bearing and test-pinned: it says keep
the comparison NORMALIZED and add the raw values — a model reading "record raw"
as "compare raw" would undo the normalization and trade a dead dismissal rung
for formatting false positives. Four tests: lint-is-called, gate-before-
acceptance (the 8.8 placement lesson), diagnostic-preserves-comparison, and a
REAL archived compliant harness (silent as generated, objects when its Raw keys
are stripped — the population-that-can-fire corollary). Remaining: the
raw-vs-pinned comparison, shipping at best as observably-inert-on-old-data plus
mechanism-correct-on-new-data — neither is a safety claim; the real guard is the
next full suite read per-event.
→ **8.4 COMPLETE (4fa3d03, 2026-08-01): prompt · gate 0c2 · comparison. 610
passed, 7 skipped.** Measured exactly as pre-scoped: claim 1, zero verdict
changes across all 333 archived rows (cases228 + 38 dismissals + 67 catches; 0
rows carry actualRaw=, inert by construction); claim 2, mechanism-correct on the
smoke's 5 real alarms — where it found a LATENT FALSE DISMISSAL: two alarms move
matches→differs because the numeric comparator's rounding floor called the
message's -0.0 a match for the 0 inside pinned `x- -0`, while the raw strings
differ by exactly Closure-38's separator-space defect. A coincidence of digits
stood ready to void a genuine catch. Limit checked, not assumed: zero lift notes
reached that leg's judge, so no dismissal was actually lost — latent, closed,
not inflated. This closes cycle-1's FIRST recorded residual
("fired_value_vs_trusted can false-match"), observed live for the first time.
Parser hardening from real alarms: trailing metadata keys after the raw pair
would have been captured into the value by a spec-derived parser (synthetic
tests would all have passed); an unknown stop key now marks the capture DOUBTFUL
rather than trusted — ambiguity degrades confidence, never resolves silently.
**BATCH ASSEMBLED: 8.4 (all three parts) + 8.7 + 8.8 + 8.12(a) pins, one build.
Status: observably-inert-on-old-data + mechanism-correct-on-new-data; the real
guard is the next live suite read per-event.**
**BATCH SMOKE (batch8_20260802_123712 at 2cc051f, 343,328 tokens): FAILED —
batch REOPENED.** Build runs end to end; 8.7's marker fired live (Closure-38);
8.4's prompt half compliant (lint: 0 of 9 normalizing harnesses flagged, gate
0c2 correctly silent). But 8.4's comparison is DEFEATED in transit:
`oracle_strength.exception_headlines` caps every headline at 200 chars, the
capped string becomes `fired_all → fired` — the comparison's exact input — and
the Raw keys sit last, so the cap strips them first. Measured on the run: 4
normalizing firings reach the comparison, 1 still carries actualRaw= (at 198
chars, two under the cap — the only non-unknown result), 2 visibly truncated.
Every piece verified correct in isolation; nobody checked the journey. Verdict
table recorded NOT scored (rule 16: only baseline differs by the whole batch).
**FIX DECIDED (2026-08-02): split the consumers.** The cap's reasons (prompt
size, dedup) are prompt/display-side; the comparison is code with no token
budget — it gets the UNCAPPED headline (full output is available at the
extraction site); prompt/dedup keep 200 unchanged, pinned. The defect is 8.4's
own thesis violated one level up: a display transform standing in for the
record. Fallback only if dual-form proves invasive: middle-truncation
preserving whole trailing key/value pairs, elision marked. Requirements: (1)
journey test pinned — a >200-char Raw-carrying alarm reaches
`fired_value_vs_trusted` intact end-to-end; (2) ellipsis-bearing comparison
input reads as DOUBTFUL, never trusted; (3) dedup/prompt behavior pinned
unchanged. **Second smoke authorized (~350k); the 30-leg pair HELD until it
passes — the pair is 8.4's live guard, and a mechanism inert on 3 of 4 firings
guards nothing.**
**DIAGNOSIS CORRECTED before the second smoke spent (26cb728): the cap was
real but SECONDARY — the primary cause is an embedded NEWLINE.** The lost
headlines are 312/314 chars with no ellipsis; the capture regex
(`==\s*Java Exception:\s*(.+)`, `.` excludes newline) stops at the first
embedded newline, and Closure-62's raw expected output is multi-line BY
NATURE — the checks 8.4 exists for are exactly the ones the capture could not
carry (the target population and the plumbing's single-line assumption
collide). How the wrong diagnosis happened: `exception_headlines` was run over
TRACE text whose records the trace writer had already ellipsis-truncated, and
that truncation was attributed to the cap — record-vs-thing a FIFTH time, the
first corrupting a diagnosis rather than a count; the cap fix passed every
unit test and would have passed a structural re-smoke while changing nothing.
**Fix as built (635 passed, 7 skipped):** raw values emitted with \n/\t
ESCAPED so the alarm stays one line (matching the form of the test's own
source literal); comparison decodes both sides; the real-newline doubt check
runs on the CAPTURED text BEFORE decoding (decode-first would report every
correctly-escaped value unknown — that ordering bug fired in test and is
pinned); consumer split kept, cap still pinned by the journey test as the
second cutter. **What the second smoke decides:** of the alarms whose checks
normalize, how many still carry actualRaw= at the comparison — first smoke 1
of 4; anything short of near-all means the escaping instruction didn't take,
and the answer is then a MECHANISM, not a rewording.
**SECOND SMOKE PASSES — BATCH CLOSED, second and final time
(batch8b_20260802_135255 at 26cb728, 370,764 tokens, 79e91a1).**
Pre-registered number: **3/3 = 100%** (was 1/4). The run separates the fix's
two halves, each doing its own job: the two long Closure-62 alarms (414/440
chars) carry escaped newlines (instruction took) AND are flagged as ones the
capped form would have lost (consumer split delivered) — either fix alone
recovers neither, retroactively justifying keeping the cap work after the
diagnosis correction. n=3 is an existence proof the path is open, NOT a rate —
the rate belongs to the pair. The rung is ALIVE: the lift note now reads "the
fired value differs from every value the test itself pins" (a real
comparison); "no numeric value could be compared" — previously this rung's
only reachable branch — appears 0 times. 8.7 markers live on both legs; gate
0c2 correctly silent, lint agreeing.
**Flagged, deliberately NOT attributed:** Closure-62 TN→FP with
crashed_on_patch=True for the first time — least stable leg in the corpus
(4 FP / 3 TN over seven observations) and a separate accusation channel
appeared this run, so no attribution to 8.4. But it names something real: 8.4
made a dead rung live, and a live rung has two branches — the guarding
concentrated on `matches` (dismiss-pushing); the `differs` branch is
KEEP-leaning, and its first live firing landed on a correct patch. Not
evidence of harm; it is the pair's FOURTH question: how often `differs` fires
on correct-patch legs, and whether those legs accuse.

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
**(c) SKIPPED by user decision (2026-08-02).** Consequence recorded, not
resolved: the crashing-bug path stays unmeasured since the 2026-07-16
crashcheck run — any statement about the pipeline covers SEMANTIC bugs only,
and the exposure doc's caveat stands. (b) stays parked indefinitely (its
unlock was (c)'s output). The unit-test pins from (a) remain the crashing
path's only current-code verification.
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
**RUNG 2 CLOSED (0768e86, 2026-08-01): passes on the two live criteria.**
Repair marker live at first exercise (7 markers, 10 detail fields; includes
boolean-swallow's FIRST live firing after offline-only validation). Rate
states live (4 distinct states in 2 legs; the 2 absent are defensive
branches). Criterion 1 offline-verified as pre-narrowed. Criterion 4
WITHDRAWN as ill-posed: the smoke7 baseline (f307dd9) differs from this
build by THREE verdict-affecting changes, so "zero verdict changes" measured
accumulated drift, not the rung — Closure-62's TN→FP sits at 3 FP / 2 TN
across five observations, inside the 27% variance, with the concatenation
fold confirmed active (5 TRUSTED blocks vs 0 in smoke7). Bonus confirmation:
the fold delivers trusted values yet reaches ZERO dismissals — the fired
value is normalized and cannot match the raw literal — independently
confirming 8.4 is aimed at the actual gap. Origin of standing rule 16.

**RUNG 3 — the one licensed precision fix (small code + one prompt rule).**
8.4 raw-value recording (compare normalized, RECORD raw; ~17% of harnesses).
**CHECK:** 20-generation compliance smoke — do fired messages carry both forms?
Then the extractor extension measured ALONE on cases228: no verdict may flip
except via newly-possible legitimate matches; Closure-62's rows are the named
target (today all `unknown`; success = any move to `matches`/`differs`).
*RUNG 3 COMPLETE (2026-08-01):* 8.4 done in three parts (see item) — 100%
compliance (after striking a truncation-artifact 38%), gate 0c2 wired and
pinned, comparison measured under pre-scoped claims (333 archived rows, 0
changes; latent false dismissal found and closed on new data). Batch
assembled: 8.4 + 8.7 + 8.8 + 8.12(a). The board holds one card: 8.1 on the
user's phrase.

**RUNG 4 — paid experiments, cheapest information per token first.**
1. 8.1 judge swap + REQUIRED noise floor (~2–3M total; launches only on the
   user's phrase) — the biggest open question: is the ceiling the architecture
   or the model? Read-out pre-registered (per-case flips vs the floor).
   *DONE AND CLOSED 2026-08-01 (~8.5M incl. ~3M harness-fault waste): same
   failure shape — the ceiling is ARCHITECTURAL, model-independent. See the
   item and the rejected-ideas entry. The board's one card is spent; remaining
   work is rung 5 (free design docs) and the final gate (user's word for the
   30-leg pair; fresh12 locked).*
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
*RUNG 5 COMPLETE (dd04743, 2026-08-02): four docs in
`docs/replay/backtrack/8.{2,9,16,17}-DESIGN.md`, each with station tag,
failure mode, guard-before-mechanism, and pricing. Check PASSES.* Headlines:
8.2's authority screen IS the design (reference discarded outright on
buggy-side disagreement; trigger reach MEASURED not estimated — 58/228 rows,
25.4%, 9 bugs, an upper bound; P4.2's fixed-code-decides-observables named
as the load-bearing prior art). 8.9: identity by asserted property, never
name; prices slot competition vs the novelty gate. 8.16: fact never gate;
its hard problem named early — fix propagation also moves passing-test
behavior, so it needs a family-duty split or it accuses correct patches for
fixing properly. 8.17: buggy-side only, feeds the existing screen, no own
budget; plausibly damages BOTH guards. 8.9 and 8.17 state in their own text
that the justification is weak against a 1–4 leg class. Alongside: the
`verifier_replay` occupied-directory guard shipped OUTSIDE the closed batch,
with a test walking src/ to prove no shipped module imports it (claim
checked, not asserted). 8.11 deliberately HELD until after the batch smoke —
rule 16 applied prospectively: the smoke's baseline stays the immediately
preceding commit with nothing incidental between. 620 passed, 7 skipped.

**RUNG 6 — gated builds.**
8.3 buggy-value collector (after 8.1/8.2 confirm direction); validation rides
the next live run passively — no dedicated run.

**Anytime:** 8.11 housekeeping (zero risk) · 8.10 pre-commitment upkeep
(continuous).

**FINAL GATE (re-sequenced 2026-08-02 on the user's decision):** the pair is
DEFERRED until the substantive builds land — the closed batch contains no
score levers, so the user chose to build before measuring. New order:
1. **8.3 build** (small; collection-only, rides passively; unblocks 8.2's
   screen, 8.15's licensed scope re-ask, and 8.20).
2. **8.2 build/no-build on the design doc** (user's call), with two design
   extensions to evaluate first: an exact-arithmetic reference (subsumes the
   Math-30 overflow class as a category — 46341² is the first int² overflow)
   and, as a priced phase-2 only, the buggy-validated reference as an
   input-finder for the 3 reach misses.
   *Validation correction (2026-08-02): 8.2 canNOT be fixture-validated for
   ~zero tokens — the authority screen requires generating and EXECUTING a
   reference against the buggy build, which no archived fixture contains or
   can. Its trigger reach is offline-measurable (58/228, done); its SAFETY is
   not — first real validation is a run, and its cost is budgeted as one.*
3. **8.20 scope fact** (rides 8.3; measured alone on fixtures before joining —
   unlike 8.2, this one IS offline-validatable: it computes a fact from values
   already recorded).
4. In parallel on the VM: **8.12(c)** crashing rerun (unparks 8.12(b), needed
   before any "official state" claim).
5. Then the full 30-leg pair on the user's word — budget from actuals (~12M:
   5,973,680 + 6,038,623) — answering the four pre-named questions PLUS
   whatever 8.2/8.20 add, each having been measured alone on fixtures first.
Then the fresh12 decision (user's literal phrase only). 8.11 lands in the
quiet after the pair. 8.19 (anchored generation) is generation-side and
CANNOT be fixture-validated — its own later batch, never bundled into this
pair.

### 8.19 Buggy-anchored check generation — design doc first, the unowned big one
**Target:** stations 2–4 (`relation_synth.py` + harness codegen prompts),
consuming 8.3's recorded buggy-side values.
**Failure mode:** the binding recall constraint (rung-1 synthesis): checks fire
but deserve dismissal because they assert contracts from model MEMORY — the
same weak-authority disease as the precision side's 90%-uncited accusations.
Structure-from-data is the only two-roll-confirmed mechanism catch (Chart-19)
and this is its generalization: feed observed incumbent values into generation
so checks are born citing authority #2 (buggy behavior off-defect), not memory.
**Rules that bind:** design doc + guard plan before any build; generation-side,
so validation is smoke-then-live, never fixtures; no dataset-shaped anchors —
values delivered as data, never as curated examples.

### 8.20 Authority-scope fact — NOT BUILT; closed NEGATIVE 2026-08-02 (688c710)
**Target was:** evidence assembly (station 6→7): compute whether the firing's
actual parameters sit inside the scenario the cited test pins (needs 8.3's
values).
**Why it died, twice over:** (1) Premise contradicted by 8.6 on its own
motivating case — 8.6's mechanism paragraph found the Math-39 harness
reproduces the test's setup EXACTLY (fuzzing only a boolean and an int), so a
scope fact would score Math-39 in-scope and never fire; the real deciding fact
("the real failing test passes on this build") was already delivered on all
nine accusations and read as corroboration — not a missing fact. 8.6's
consequence-3 ("what distinguishes it is scope") is hereby SUPERSEDED by its
own mechanism paragraph, adjudicated by this re-ask; 8.20 and 8.15's scope
requirement were built on the superseded sentence. (2) The licensed re-ask,
run mechanically on 8.3's values (85 kept findings, larger than 8.15's 91):
**80% UNDETERMINED** (68/85 — no ≥3-char test literal or no recorded value: a
property of the MECHANISM capping reach at ~20% of firings), and on the 17
determined: FA in-scope 4/6 vs GC 3/11, Fisher p=0.162 — right direction, 4
counts vs 3; at these denominators only near-perfect separation reaches
p<0.05. Undetermined four times in five, non-separating on the fifth.
**The single re-ask 8.3 licensed is SPENT** — another population would be
phrasing-iteration wearing measurement's clothes.
**The residual open question, disposed by standing rule:** rewording the lift
note so provenance reads as a limit is a judge-prompt WORDING change — rule 12
(wording iteration measured dead; judge changes are structural or nothing)
plus four in-cycle wording negatives say NO. Math-39 folds fully into bucket
(a): deciding fact delivered, not binding — the adopted ceiling class. Only
the user can override rule 12 here.

### 8.21 Small hygiene, anytime
(a) Trace-writer truncation: record alarm text in full or mark elision loudly —
ellipsis-truncated records have corrupted one count and one diagnosis
(record-vs-thing #5). (b) The 34 of 230 judge prompts carrying no evidence
block (parked cycle 7, unexplained) — one read decides plumbing hole vs benign.

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
