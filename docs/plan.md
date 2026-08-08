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
**RESULT (pairA8/pairB8_20260802, raw committed before scoring — FAIL on all
three criteria, f1e2024):**
```
CYCLE-8 A  TP= 9 FN= 5 FP= 7 TN= 9   F1=0.60   accusations=7
CYCLE-8 B  TP=10 FN= 4 FP= 5 TN=11   F1=0.69   accusations=5
PAIRED MEAN 0.6448  vs bar >0.685 (July pair: 0.6881)
```
Accusations per roll: A=7 FAIL. Historically clean legs accused: Math-2 (BOTH
rolls — not noise, per the two-roll rule), Math-86 (one roll). Cutting the
other way: July's Lang-60-c and Math-73-c accusations disappeared. The
statistically honest sentence, recorded: within-pair spread (0.09 both pairs)
exceeds the between-pair difference (0.043) — neither regression nor
improvement is established; but the bar is a pre-committed threshold, not a
significance test, and it is MISSED. Official state remains the July pair
(0.6881) — this pair does not replace it.
**The other five reads (none implicates the batch):** 8.4 dismiss branch: ZERO
matches firings across 60 legs — no catch voided, the fail condition never
triggered. differs: 5 firings, all Closure-62 (already a residual in July) —
8.4 works, is guarded, and is nearly inert at this scale. Repair provenance:
73 of 299 accepted harnesses (24%) came from a repaired attempt — a large,
previously invisible dependency, the strongest positive. Value channel: 224
events, 72 with values (baseline 0 of 1,452). Read 6 (8.2's reach recovery):
UNRESOLVED on principle — the pre-registered denominator ("% of trigger
rows") is not computable because result.jsonl carries no code_context;
substituting a denominator the rule didn't name would be un-pre-registration;
8.2's expensive half stays unbuilt for a STATED reason, not a measured one.
Fix is small: record code_context (→ 8.21(c)); same gap blocks the
throughput check.
**Forensic thread #1: Math-2's two-roll accusation.** Named suspect to check
FIRST (hypothesis, not attribution): the 8.1 step-1(b) sentinel fix changed
what unparseable judge responses become in `family_duty` — garbage previously
read as "duty does not apply" (escape NOT taken → drop proceeded); post-fix,
more duty answers parse → escape taken → accusation KEPT. On correct legs a
spared accusation is a false accusation. One grep decides: family-duty escape
and re-ask events on Math-2-c's chains, both rolls, vs the July pair's.
**FORENSICS COMPLETE (2026-08-03, three-reader pass over all four runs):**
- **Sentinel hypothesis REFUTED.** Zero parse-failure/re-ask/sentinel events
  in any of the 120 legs across all four runs; every family-duty answer in
  every Math-2-c trace parsed cleanly (all NO, both eras). The fix's code
  path was never exercised live.
- **Math-2-c is the accusation lottery, not one mechanism.** The two new
  accusations are DIFFERENT checks via DIFFERENT old routes: pairA8
  `inverse-cdf-fixed-point-general` (surfaced by muted replay, kept by 6C's
  different-values keep); pairB8 `readers_stable_across_mean_call`
  (relation-replay conviction, gates skipped by design). Both CITATION: NONE
  — ceiling-class uncited contracts. July-A's three firings were all
  dismissed via computed facts (1-ulp citation, javadoc citation,
  fires-on-both); the new rolls drew checks the fact machinery happens to
  have no computed defense against. No batch mechanism involved.
- **Closure-92's unanimous catch→miss IS plausibly batch-linked, via a side
  channel no read pre-named:** all firings in both rolls dismissed by
  hypotheticals that QUOTE the new raw values ("a harmless trailing
  semicolon… exactly as observed in actualRaw"); Closure-38's dismissal
  likewise builds on the raw record (`x- -736E3` vs `x- -736000`). Read 2
  cleared 8.4's COMPARISON (matches=0, confirmed); it did not cover 8.4's
  raw values entering the judge's PROMPT as dismissal material. New named
  channel: **raw-values-as-hypothetical-fodder** — recall-side risk, two
  suspected instances, needs its own read before any 8.4 verdict.
- **July's disappeared accusations (Lang-60-c, Math-73-c): the accusing
  relations were never synthesized this time** — differently-named analogues
  stayed quiet. Not dismissed; never fired. Lottery, both directions.
  Bonus: pairB8's Math-73-c had a SOUND harness verdict correctly KILLED by
  the family-duty terminal (fd_prior=False) — the precision machinery
  working.
- **Math-86-c (one roll): two replay-track convictions, gates skipped by
  design, CITATION: NONE** — coin-flip class pending a second observation.
- **First-ever catches in pairB8:** Math-104 (complement-contract check kept
  — consistent with the step-1 adjudication that its 8.2e-10 violation IS
  catchable; softens "unwinnable at current tolerance"), Chart-19 at
  standard width (the null-family fired and was kept clean), Lang-60-o (a
  generalized form of the failing test's own observable).
- **Loose end:** pairA8's summary covers 29 legs (Lang-22-c finished after
  summary generation; TN either way, F1 unaffected) and its Lang-22 leg has
  the only non-empty relation_replay_fired of all 120 legs without a scored
  outcome — one check whether it was judged-and-dismissed or never judged.

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
**Pure core BUILT (60b3553, 2026-08-02; 679 passed, 7 skipped):**
`reference_impl.py` — the authority screen as pure logic, no JVM/IO/LLM. Fails
closed everywhere (missing data, no off-defect keys, <3 shared off-defect
observables, any non-weak disagreement); the fact returns None whenever the
reference wasn't admitted (a hedged fact on a discarded reference is an
uncited accusation with extra steps); the mirror canary returns False when it
couldn't run, never a pass; OUR code picks the observables from 8.3's values
(P4.2 as mechanism, not instruction).
**Reach MEASURED, and it collapsed the estimate:** trigger 54/220 (25%) → ≥1
comparable observable 22 (10%) → ≥3 (screen minimum) 19 = **8.6% ceiling**,
an upper bound twice over (admission + actual disagreement still required).
32 of 54 trigger rows record NO comparable observable — but the archives
predate 8.3/8.4, so this may be an archive property, not a mechanism
property.
**DECISION (2026-08-02): the expensive half (generation prompt + execution
adapter) is HELD until the two identical 30-leg runs populate the value
channel live.** That run decides whether reach recovers (build against a real
number) or stays ~9% (skip the cost). The 30-leg runs gain a SIXTH pre-named
question: does live value-recording recover 8.2's observable rate?

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
5. Then the two identical 30-leg runs on the user's word — budget from actuals
   (~12M: 5,973,680 + 6,038,623) — now carrying SIX pre-named questions:
   (1) the 8.10 bar; (2) 8.4's live safety guard, per-event; (3) repair
   provenance at scale; (4) 8.4's differs-branch rate on correct legs;
   (5) does the 8.3 value channel populate live; (6) does live recording
   recover 8.2's observable rate (decides 8.2's expensive half).
   Board status 2026-08-02: 8.12(c) skipped (user), 8.20 closed negative,
   8.3 complete, 8.2 core built + held — the 30-leg runs are the one thing
   that unblocks the rest.
Then the fresh12 decision (user's literal phrase only). 8.11 lands in the
quiet after the pair. 8.19 (anchored generation) is generation-side and
CANNOT be fixture-validated — its own later batch, never bundled into this
pair.

### 8.26 THE THROWS-CLAUSE + QUOTING-CAP REACH REPAIR — built 2026-08-08 (stage-2 roll 2's finding)
Stage-2 roll 2 (stage2b, raw 502928c, read ed9b1f4): perfect score
(TP=1 FP=0 — the Math-65 TN is the lottery again, 0 gate events, not
banked), gates S2-c/S2-d still unmet, stage 4 does not fire; rule 7
applied — no third draw, fix the measured constraint. The constraint:
the patched method's FULL body was in the context and `_method_body`
returned None for TWO independent reasons. (1) THROWS — the header
matcher required the brace to follow the parameter list; `throws
OutOfRangeException {` made 913 of 976 throws-declaring definitions in
the untruncated fixture corpus invisible (93.5%) — the largest measured
reach constraint yet found, and the SIXTH check-your-matcher instance.
(2) THE CAP — the 900-char QUOTING budget applied to an EXISTENCE check
(this body is 1,518 chars). Two locks on one door; either alone changed
nothing. FIXED: one shared `_METHOD_BODY_OPEN` header tail used by
`_defined_methods`, `_method_body` and `fields_read_by`; `_method_body`
gains `max_chars` (quoting path keeps its 900 default — judge prompts
unchanged; the detector passes None). Fixtures modeled on the real
blocked method, both locks tested separately and together; elided-body
declines unchanged. Suite 807/7; corpus scan unchanged (3 voids, no
regression); full rewalk8 replay green on stage2b's OWN artifacts — a
THIRD independent generation, one ulp off the earlier two on the errors
vector, absorbed by per-element agreement exactly as designed.
**OPEN AT THE GATE (not decided unilaterally): even with both locks
fixed, Math-2's S2-c stays blocked — the DISPUTED method
(getNumericalMean) is not the PATCHED method, so its body is elided by
the context assembler's only-patched-methods-keep-bodies rule, and the
detector's body-shown requirement drops it. Whether the reference chain
should require the disputed body AT ALL (the generator never sees it —
information rule; the mapper degrades gracefully to []) or whether the
context assembler should keep fired-named methods' bodies too, is a
design decision with judge-prompt-size and information-rule
implications. Decide before stage-2 roll 3.**
**DECIDED — OPTION A (user, 2026-08-08), built same day: the detector's
filter is DECLARED-IN-CONTEXT (signature visible; body shown, elided, or
abstract), not body-shown. The body requirement was inherited from the
quoting feature, which KEEPS its own — judge prompts are untouched. The
one internal body consumer (`fields_read_by`) already degrades to [];
a wrongly-started chain ends in a reasoned discard at the screen, never
wrong evidence; cost is bounded by the per-method-per-leg memo.
Consequence accepted with eyes open: Math-2's `sample` firings now
trigger too (a stochastic method — expect an honest discard; that
discard is itself reach data). Regressions: suite 807/7, corpus scan
unchanged (3 voids), full rewalk8 replay green on stage2b artifacts
(06be7bb). STAGE-2 ROLL 3 IS GO — same suite, same five gates;
expected on Math-2: detection at getNumericalMean (replay door, via
check source), then the first generation/screen attempt on held-out
material.**
*STAGE-2 ROLL 3 (2026-08-08, stage2c, raw before read, read 48e796a):
**THE MECHANISM DID ITS JOB ON HELD-OUT MATERIAL — stage 2 PASSES.**
Math-2: the chain ran end to end on its first new bug — detection at
getNumericalMean via the check source (the 8.26 fixes working live),
generation for a distribution class, twin from testMath1021, standing
earned on 6 off-defect siblings computed from documented formulas, pin
ABSTAINED rather than borrowing a literal — and emitted the
disagreement fact: patched −49.759350398538686 vs independent
reference 49.821236993679285 (a hypergeometric mean cannot be
negative; sign flip, not tolerance). S2-a PASSED NON-VACUOUSLY: the
admitted reference did not void the catch. S2-b/S2-c PASS; the sample
trigger accepted under option A fired and died honestly at compile,
zero facts leaked (gate (b) now 3 stages standing). **FIRST ATTRIBUTED
JUDGE ENGAGEMENT**: call 138's WHY consumes the reference's value as
its "expected" — a number occurring EXACTLY TWICE in the trace (the
fact, and that sentence; verified independently). ASYMMETRY RECORDED:
the first engagement is on the CONVICTION side; roll 12's agreement
facts were ignored 9-of-9 — consistent with the judge's measured
keep-bias, and exactly why the verdict gate exists for the exoneration
side. S2-d not banked (Math-65 crashed=False, third straight upstream
lottery; the correct-patch side still has NOTHING attributable).
**ADVANCEMENT JUDGMENT (user's word, recorded): STAGE 4 FIRES** — not
despite the correct-side gap but because of it: stage 4's fresh
correct legs (Math-30, the design's own exact-arithmetic agreement
test; Math-53 as live clean guard) are the correct-side test that more
Math-65 draws are not. `suites/cases/ladder_stage4.cases` + gates
pre-registered in STAGE4-HOW-TO-READ.md before launch: S4-a clean-leg
hard stop, S4-b Math-30 attributable evidence, S4-c Math-2 regression
(lost catch = hard stop), S4-d Math-65 attribution rule, S4-e zero
facts from discards, S4-f rule 7, plus pre-registered denominators
(admission rate, cost/leg, both signs).*
*STAGE-4 ROLL 1 (2026-08-08, raw 8bde6bc, read 2a6183f): S4-a no
trigger (clean leg: 0 facts/admissions/voids; its accusation is real but
not ours), S4-c PASS (Math-2 fact re-emitted, catch intact), S4-e PASS
(8 discards, zero leaks — gate (b) now 18-for-18), S4-d held. S4-b
PARTIAL: Math-30's chain ran twice and banked nothing — one honest cheap
death (mannWhitneyUTest, too thin to screen, killed before the JVM) and
one FALSE TRIGGER: `_method_declared` accepted a CALL as a declaration
(`return 2*standardNormal.cumulativeProbability(z);` ends in `);` and
satisfied the `[{;]` tail; fixture scope 3,858/18,496 = 20.9% calls).
Fixed same day (f8e4b52): a declaration's name is preceded by its return
type — type-ish token + whitespace, never a receiver dot; `return
foo(x);` excluded by keyword; verbatim fixture. Fail-closed held (spend
and noise, never a fact). **THE FINDING WEIGHTED ABOVE EVERYTHING: the
STATELESS-RECEIVER CEILING.** MannWhitneyUTest takes its data as
arguments and keeps no computable sibling state — screening surface 0
vs bar 3: no reference for such a class can EVER be admitted, however
good. Both admissions on record are stateful receivers. The mechanism's
reach tracks RECEIVER STATEFULNESS, not bug difficulty. Admission rate
2/10 triggered (20%), cost 131-250k/leg. AGREEMENT SIDE: still
untested, NOT refuted — Math-30 never reached it. **ROLL-2 SELECTION
(decision recorded): shape-selected — Math-30 out (shape-blocked;
exact-arithmetic test deferred), Math-2-c (SOFix) in: the agreement
test on the ONE class where admission is proven, isolating the
agreement question from the admission question. Cases + read doc
amended; gates otherwise unchanged. Stage-4 roll 2 is GO.***

*STAGE-4 ROLL 2 (2026-08-08, raw before read, read 5f7aa26): S4-a no
trigger (clean leg zero mechanism events), S4-c PASS, S4-e PASS (5
generations, 1 admission, 4 discards, zero leaks — gate (b) 19-for-19),
S4-d held. BANKED: the Math-2-Arja fact is DIGIT-IDENTICAL to stage-2
roll 3 across runs and days — independent generations now 3-for-3
convergent. S4-b FAILED and RULE 7 TRIGGERED: the agreement side went
untested a second time, for a second unrelated cause — `disputed[0]`
was the entire attempt policy, and on the SOFix leg both firings'
position 0 was a stored-field accessor incidentally named by the
message; getNumericalMean (documented closed form, twice-admitted on
this exact class) sat in BOTH candidate lists, attempted in NEITHER,
and the memo cached the failures. Richest screening surface ever
recorded (7 siblings) produced nothing — not reach, not shape, not the
screen: attempt POLICY. Fixed per rule 7 without a re-roll: candidate
ORDER is ranked by signal strength (message∩check first — two
independent routes agreeing, Math-65's shape; check-called next, in
call order — the check calls what it disputes, Math-2's shape;
message-only words LAST — they produced both wasted attempts), and the
chain now FALLS BACK through up to three candidates, each attempt
memoized individually. Verified offline against both recorded shapes;
the walkthrough drives a first-candidate failure through to a
second-candidate fact. Admission-rate denominator note: 2/10 then 1/5
triggered references admitted (20% both rolls). 810 passed, 7 skipped.
**Stage-4 roll 3 is GO — same legs, same gates; the agreement side's
third attempt, now with the productive candidate reachable.***
*ROLL-3 WIDENED (user direction 2026-08-08): the four stage-4 legs keep
their decisive gates; FOUR SCOUT LEGS appended as pre-registered
denominators for stage 8, selected from the pair runs' record —
Closure-62-c (STABLE FP in both pairs: the second live precision target
after Math-65; does the chain reach a Closure-shaped receiver?),
Math-39-c (plan-named stage-8 leg), Lang-41-o (stable catch; hard-stop
extension: zero voids), Time-4-c (stable TN; second clean guard,
hard-stop extension). The pair record also reframes the target
population: Math-2-c is a STABLE FP in both pairs — the precision
problem was never one bug. Eight legs serial, ~1.2-1.6M, gates and
scout reads pre-registered in STAGE4-HOW-TO-READ.md before launch.*
*WIDENED AGAIN (user challenge, recorded): "why stable ones only?"
Answer split by what one draw can attribute. STABLE legs = attribution
(a flip with events is evidence; roll-13's lottery rule); UNSTABLE legs
(Chart-26-c, Chart-19-o, Lang-63-o) need a repeated-measures design —
deferred to stage-16/pair, stated not skipped. But the challenge caught
a real under-sample: the NEVER-SOLVED population. Stable FNs verified
DISMISSAL-TYPE from the pair traces (firings reached the judge, all
dismissed — 8.14's largest miss class): Closure-92-o (5-6 judge calls
per pair) and Closure-38-o (3 per pair) ADDED as recall scouts — the
judge engages disagreement facts (stage-2 roll 3), so an FN→TP with
attribution would be the first RECALL win. Lang-63-o excluded with its
reason (1 then 0 judge calls: miss is upstream of the mechanism).
Blanket scout rule: zero gate voids on ANY overfit leg. Ten legs,
~1.5-2M.*
*STAGE-4 ROLL 3 (2026-08-08, raw 49e1117, read e698525; 10 legs, 107
min, 2.21M tokens, PARALLEL): **STAGE 4 PASSES.** All hard stops clear;
zero voids on all ten legs; S4-a no trigger on both guards; S4-c PASS.
S4-b PASS — the ordering fix worked exactly as built (getNumericalMean
attempted FIRST and ONLY: one generation, one admission, zero discards,
vs roll 2's two wasted accessor attempts; standing earned on 7
siblings, strongest recorded) and produced THE A/B, in one run: same
class, same documentation, reference computes 49.821236993679285 on
BOTH legs — DISAGREES with the fake patch (which computes −49.759...),
AGREES with the correct one (identical to the last digit). The central
claim, shown on both signs; fourth digit-identical reproduction across
independent generations. **THE ASYMMETRY IS NOW A MEASUREMENT, not a
suspicion: conviction-side engagement 1/1, agreement-side 0/11, across
TWO bugs** — the agreement fact reached the judge twice on SOFix and
was ignored twice (occurrence-count standard: the value appears only
inside the fact block). The TN does not bank (leg already clean; no
gate decision point — roll-13 rule). RECALL SCOUTS: both stable FNs
(Closure-92-o, Closure-38-o) converted to TP but WITHOUT mechanism
attribution — recorded as lottery per the standing rule, not banked.
REACH, now population-measured: admission 3/30 generations (10%; Math-
only rolls were 20%); 11 of 27 discards are too-thin-to-screen and
EVERY ONE is on Closure or Time — the stateless-receiver ceiling is a
population property, not a one-leg inference; signature mapping is the
next class (6). Both-signs ledger: nothing gained or lost traceable to
the mechanism; all three FPs have zero gate events behind them (no
admitted reference on those legs). **PENDING: the stage-8 decision.**
What roll 3 sharpens for it: the mechanism is PROVEN on both signs; the
judge's keep-bias is measured; therefore the precision path runs
through the DETERMINISTIC gate only — and the gate's binding constraint
is now clearly its reach (value-coincidence + admission on FP legs),
i.e. the parked 8.4 firing-state extension, not more evidence quality.*





### 8.4x THE FIRING-STATE EXTENSION — design note (user go 2026-08-08); build after offline replay gates
**What:** when a rule FIRES in the harness, the firing message also
reflection-prints the receiver's state fields (same `_FMT_HELPER`, same
`__param_` discipline as the twin — identical formatting on both sides is
the whole point, defect-8 lesson). At the verdict gate, a kept conviction
with an admitted reference reconstructs those inputs (existing
`java_literal` machinery) and calls the ALREADY-COMPILED ReferenceImpl
once at the firing's own input. Sides-with-patch → VOID (precision, now
at every firing instead of value-coincidence luck). Sides-with-relation →
conviction stands, CORROBORATED (the direction removed from phase 1 as
unsound becomes sound: state recovery kills the different-situation
ambiguity).
**Where:** Station 1 (harness generation prompt/template: the failure
branch prints state) + the gate's abstain branch (judging station).
**Why now (measured):** judge keep-bias is a measurement (1/1 vs 0/11,
two bugs) → precision runs through the gate only; roll 3's three FPs had
zero gate decisions because every conviction fired at a fuzz state.
**Pre-registered gates before build:** (a) offline replay over the four
rolls' recorded firings: state-printing simulated from twin fields,
reconstruction round-trips, zero voids anywhere a void would be wrong;
(b) 67-row genuine-catch fixture logic re-argued for the corroborate
direction; (c) zero voids on any overfit leg, standing; (d) cost note:
one extra JVM run per gated conviction, bounded by admission (10%).
**Limits stated:** helps only admitted-reference legs; stateless ceiling
untouched (see 8.27); lhs/rhs-only relations reachable only where state
recovery succeeds.

### 8.29 VARIANCE BASELINE — measured 2026-08-08 (varbase, raw before read, read 2ce8bbf; 15 runs, 76 min, frozen code e9868f2)
**The lottery has a size and a station.** 3 of 5 legs move under
repetition, BOTH directions: Math-65-c FP/TN/FP, Chart-19-o TP/FN/TP,
Lang-63-o TP/FN/FN — two of the three unstable legs are RECALL legs (a
fake patch escaped 1-in-3 and 2-in-3 draws). Every recall number this
project has quoted is a single draw; roll 3's R=1.00 was four draws,
not four guarantees. **THE STATION IS ORACLE INVENTION — the FIRST
model call**, observed directly: harness counts identical on all
fifteen runs (built=5 run=5 triggers=5 everywhere); what differs is
WHICH checks get invented (Chart-19's FN draw never probed the null
range axis; Math-65's draws invented different chiSquare formula
relations). **THE NUANCE ANY FIX MUST BE BUILT AROUND: invention
variance is UNIVERSAL, outcome variance is NOT** — the stable legs
invent just as differently (Math-2-SOFix: three disjoint oracle sets,
TN every time). A leg is stable when its defect is reachable by MANY
different checks and unstable when detection hinges on inventing ONE
particular check — a property of the BUG, not the run. The design
question is therefore "invent more, or select better?" — open, gated,
connects to parked 8.19. GAP FILED AND FIXED same day: per-run
artifacts carried no sha (suite config.json does; leg dirs on their
own did not) — result.jsonl now records git_sha and the trace header
prints it (GITSHA exported by run_suite.sh from VERSION). Repeated-
draw designs now rest on artifact-checkable provenance. CONSEQUENCE
FOR REPORTING, standing: single-draw outcomes on the three unstable
legs are DRAWS; stage-16 and any headline P/R must be multi-draw on
unstable legs.*

### 8.28 TWO DESK READS (2026-08-08, free, from recorded material) — findings recorded
**(a) The six signature-mapping discards, read.** Two classes. MODEL-
DEVIATION SHAPES (4): a literal `...` signature, a bundled
`ReferenceImpl.State` struct parameter, a `double p]` bracket-parse
artifact — parse-hygiene fixable, small, mechanical. BARE TYPES WITH NO
VISIBLE BODY (2): option A admits detection on elided-body methods, but
`fields_read_by` then has no read-order, so a nameless `double` against
72 fields is an HONEST discard — the only lever is the prompt's existing
name-your-parameters demand; accepted as a known loss, not a defect.
**(b) The nameless-firing population, measured: 88%.** 2,520 of 2,877
fired messages across the archive print NO named observable (only
expected/actual/lhs/rhs/N-style keys). The gate's value-coincidence
parser and detection's message route are structurally blind on 88% of
firings — far larger than assumed from single traces. CONSEQUENCE
(priority reorder): the print-naming requirement is no longer a minor
lint — it rides INTO the 8.4x build as the same Station-1 template
change (the harness/relation failure branch that learns to print state
also learns to print `observable=value` by name), one combined change,
one roll to validate both. Keep/drop behavior untouched (measure-first;
any screen-time demotion of nameless relations is a separate gated
decision with the 67-row fixture as referee).

### 8.4x PROGRESS — p1a BUILT AND VM-VALIDATED (2026-08-08)
The replay-track wrapper now RECORDS (user go): `RecFDP`, a full
recording FuzzedDataProvider delegate emitted by `_screen_harness_source
(record_firings=True)`, logs every consumed value in order; a firing
prints `[relfire] <thrown message> __consumed=<values>` (capped 5/proc);
`replay_on_patched` harvests the lines into `findings['fired_lines']`;
run.py's replay `_fired` — previously a SYNTHESIZED NAMELESS string
("relation X violated [replay-on-patched, trigger tier]"), the 88%
problem's replay half — now carries the real firing message with its
values and inputs. Feeds the judge, the detector's message route, and
the verdict gate's value comparison at once. VM-VALIDATED blind-Java:
compiles clean against jazzer-api-0.22.1 (-encoding UTF-8, nested class
and all) and the runtime smoke prints `[relfire] relation demo
violated: n=100 x=1.0 arr=[1.0, 2.0] __consumed=100|1.0`. Buggy-side
screening counting is byte-semantics-identical (non-record variant;
pinned by test). 813 passed, 7 skipped. RECORDING HALF COMPLETE (same day): part 2 adds RECEIVER CAPTURE
(`capture_receivers`: `__rcv.put` after every locally-constructed
object, pattern-scoped, fail-closed) and a reflection STATE PRINTER —
on a firing the line now carries the receiver's primitive/array fields
BY NAME. VM smoke, real javac+JVM: `[relfire] relation demo violated:
actual=100.0 expected=100.0 __consumed=100|1.0 __rcvstate dist:Demo
n=100 w=1.0 arr=[1.0, 2.0]`. 815 passed, 7 skipped. REMAINING for
8.4x: p1b — the GATE-SIDE decision logic (map __rcvstate fields to the
admitted reference's parameters, run at the firing's state,
void/corroborate). Deliberately AFTER the next mechanism roll: the
recorded corpus predates state-carrying firings, so p1b's decision
rules get designed against the first roll's REAL recorded lines — the
design-against-real-material rule that every sound fix this month
followed.
**COORDINATION NOTE (standing until invdiv lands): the VM stays at
32b5787 behavior (the A/B baseline); invention_diversity.cases was
copied surgically. DO NOT push-to-vm until the invdiv suite has
launched or completed — p1a changes replay behavior and would
contaminate the A/B.**

### 8.30 INVDIV COMBINED RUN — read 817858f (2026-08-08); capture defect fixed same day
**Fail-closed clear:** zero VOIDs, zero facts from discards (facts only
on the two admitting Math-65 draws vs 14 discards elsewhere).
**INVENT-MORE (the only baseline-comparable read): Chart-19's killer
family responded strongly** — range-axis checks from 2-of-3 draws at
1-2 checks each to 3-of-3 at 6-7. Clean -n 8 attribution (upstream of
the recording). **AND the within-run finding that matters more: draw
05 carried SIX range-axis checks and crashed ZERO patched harnesses —
inventing the right family, in quantity, did not convert the miss.**
The bottleneck moved downstream of invention: check shape or
fuzz-input reach. Sharpens invent-more-vs-select-better: count alone
is insufficient. QUEUED DESK READ (free): why draw 05's six checks
never fired on patched. Lang-63 (3/3→2/3) and Math-65's name-count
drop are unreadable at n=3 draws. Outcomes recorded, not compared,
nothing banked, per the narrowed pre-registration.
**P1B CORPUS, half-collected:** 15 [relfire] lines with typed inputs —
but `__rcvstate` 0-of-9 traces. COLLECTION DEFECT (seventh
check-your-matcher): the capture pattern required `Type var = new
Uppercase(...)` in ONE statement; production constructs are
FULLY-QUALIFIED (lowercase package first) and SPLIT declaration from
assignment — the verbatim Math-2 shape, present in our own test
fixtures all along. FIXED: assignment-form anchor covers both shapes,
qualified names allowed; PLUS typed consumed labels (i:/d:/q:"...") so
`||3|5||` positional blanks are readable. VM-revalidated on the
verbatim shapes: `__consumed=i:100|i:100|q:"s" __rcvstate dist:Dist
populationSize=100 successes=100 cache=[100.0, 100.0]`. 817 passed, 7
skipped. The p1b corpus re-collects on the next mechanism roll; p1b's
design waits for it.

### 8.31 FIRST STATE-CARRYING ROLL — corpus 2feb27d (2026-08-08); collection gaps fixed same day
**Safety clear** (Math-2 caught, zero VOIDs, Math-53 silent, zero facts
from 15 discards — gate (b) unbroken). **The recording works**:
`__rcvstate` on every firing (vs 0-of-9 in invdiv), `__consumed` typed.
**Corpus vs criterion: 4 distinct firings, not a dozen** (Math-65-c: 3,
Math-2-Arja: 1, other legs silent) **and none complete** — the 700-char
harvest truncated mid-array AND the capped trace copy was the ONLY copy
(result.jsonl carried nothing). FIXED (same day): harvest keeps the
whole ~4.9k snapshot (6000-char width) and the UNTRUNCATED lines
persist in result.jsonl at BOTH replay sites (full pipeline + rulegen-
only). The durable artifact now holds the p1b corpus, not a display
preview. **THE SHAPE INVENTORY (the read's real value — four of five
shapes would break rules written against firing 1 alone):** two
receivers in one firing (opt1:/opt2:); UNINITIALISED receivers
(iterations=0, point=null, jacobian=null — the relation fired on an
optimizer that NEVER RAN, 2 of Math-65's 3 firings); NaN with
numericalVarianceIsCalculated=false (uncomputed cache, not corruption);
mid-array truncation; unlabeled consumed widths (now typed). **DESIGN
ANSWER (taken, fail-closed, narrow): the uninitialised-receiver
distinction IS one the gate must respect, in the conservative
direction only — a firing whose captured state shows the receiver
never ran carries no state at which the reference can be evaluated, so
p1b's rules must classify it ABSTAIN (mechanically detectable:
null/zero sentinel fields). Whether such firings deserve their own
treatment BEYOND abstention (they are a distinct alarm kind —
"contract violated at construction" vs "at computation") is deferred
to p1b design with the fuller corpus; nothing acts on it yet.** The
corpus keeps growing with every future roll at zero marginal cost; p1b
design proceeds when it has both completeness (fixed) and enough
distinct firings.

### 8.27 SHAPE-ADAPTIVE SCREENING for stateless receivers — design sketch only
The stateless-receiver ceiling is population-measured (11/27 discards,
all Closure/Time). But the ROLL-4 LESSON INVERTS for pure utilities: the
original vector-based screen died because STATEFUL objects cannot be
constructed from signatures — a STATELESS utility's methods take their
data as ARGUMENTS, so synthesized input vectors are exactly right there.
Sketch: screen = reference vs buggy build on N held-out argument vectors
through the disputed method's siblings-by-arity (or the method itself at
non-trigger inputs), open-input/closed-output as ever. Decision gates to
write before any build: what counts as off-defect for an argument-space
screen; bar; canary pair. NOT started — recorded so the ceiling has a
named candidate fix.

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

## CYCLE 9 — the 8.2 ladder + remaining judgments (2026-08-06, user-approved)

The delete batch is DONE (10/10, verified). The codebase is the clean state:
measured mechanisms, contracted infrastructure, two parked items. This is the
next work package, as todos for the executing agent.

### Small items first
- [x] **9.0 reask_verdict_usable disposal** — DONE 2026-08-06. Function gone;
  the 7 sentinel-emission pins KEPT and re-pointed at a test-local predicate
  (deliberate: the contract stays stated where no shared-code edit can
  silently weaken it). A blanket replace inverted two assertions; the suite
  caught both. 576 passed.
- [x] **9.1 H3 five-case read** — DONE 2026-08-06, and it re-ordered the
  queue. Four rejections correct (the Closure-62 ones normalised away the
  whitespace under test — right rejections; Math-68's cost nothing). The
  fifth is a mechanical wrong rejection: `_values_match` cannot bridge an
  escaped two-char \n against a literal newline. CRITICAL interaction: that
  shape was occasional in July; **8.4 now MANDATES it** for exactly the
  lifted text-comparing checks H3 polices — the wrong-rejection rate will
  rise.
- [x] **9.1b H3 escape-decode fix — DONE 2026-08-06 (7b02a4d), and the sweep
  paid twice.** (1) The requested comparator sweep found the FOURTH
  escape-blind instance in `reference_impl._values_agree` — 8.2's own
  authority screen, caught BEFORE the ladder builds on it; there a false
  disagreement discards a reference that actually reproduced the buggy
  build, the screen's worst failure. Fixed together. (2) The obvious
  decode-only fix contained a trap the full re-measurement caught: the
  `actual=(.{1,600})` DOTALL capture over-runs into later `expected=`
  clauses, and decoding turned that over-capture into FALSE AGREEMENT —
  silently excusing the divergence the gate exists to catch (the second
  archived flip was wrong). Fixed by stopping extraction at the expected=
  half; final replay over the five archived rejections: abstain 1 / reject
  4 — exactly the 9.1 read. The pin that matters: a diverging actual-half
  must still reject even when the expected-half quotes the wrong value
  verbatim. 584 passed. Shipping on "it flips the identified case" would
  have reopened the Closure-62-c false-alarm hole while reading as a fix —
  re-measuring ALL five is what separated them.
- [x] **9.2 A5 preconditions test — DONE 2026-08-06 (348304f): promotion
  REFUSED at the root, deletion NOT justified, judged pending a decidable
  test.** The block was never delivered on any Lang-27 leg (0 of 22 traced,
  of 44) — the promotion hypothesis dies definitively, not ambiguously. But
  A5 reaches 191 of 648 archived traces (29%) — not novelty-gate inertness.
  The use-grep was REFUSED on its control: donor javadoc tokens appear in
  57% of legs that never received the block (ordinary domain vocabulary), so
  the 40/40 "used" score is a matcher that cannot fail — the same fingerprint
  that inflated four counts this cycle, now a named tell: **a perfect score
  is the fingerprint of a matcher that cannot fail.** Queued instead: a
  block-alone A/B comparing accepted-harness behaviour on inputs the javadoc
  declares invalid (what the block actually asks for; invisible to grep);
  rides any run already happening. Carrying cost bounded: 29% of legs,
  ~900 chars capped.
- [ ] (rides any future 30-leg run, no dedicated spend: A4's re-test.)

### 9.3 THE 8.2 LADDER — build the expensive half, then grow it 1→2→4→8→16→30
**What:** the reimplementation checker's generation prompt + execution adapter
(the LLM writes a reference implementation from the DOCS; our code compiles
and runs it; the screen from `reference_impl.py` decides admission; an
admitted reference's comparison becomes a judge fact). Iterate at small n;
each stage's gates are written BEFORE the stage runs; advance only on pass.

- [x] **Stage 0 — BUILT 2026-08-06 (no task runs).** `reference_gen.py`
  (prompt), `reference_run.py` (compile-and-run adapter), stage-0 additions to
  `reference_impl.py` (the two-sided fact, `held_out_keys`, `pin_check`,
  `mirror_canary_correct_patch`). 41 tests; suite 584 -> 625 passed, 7 skipped (verbatim).
  *The information rule is a REFUSAL, not an instruction:*
  `build_reference_prompt` raises `ImplementationLeak` if any non-test section
  carries a method body — P4.2 measured models ignoring "print everything", so
  "do not look at the implementation" gets a check instead. Tests are exempt by
  name: they are the executable spec and tier-1 authority.
  *Both canaries EXECUTE end to end* through the real adapter and screen (JVM
  stubbed), plus the two mirrors they exist to catch: a patch-echoing reference
  passes canary 2 and fails canary 1; a check-echoing one does the reverse.
  Only an independent reference passes both.
  *The blind spot is demonstrated, not asserted:* a bug-copying reference is
  ADMITTED by the off-defect screen (it agrees with buggy everywhere, including
  at the defect) and caught only by `pin_check` against the failing test's
  tier-1 answer.
  *Adapter fails closed on every path* — compile failure, driver failure,
  raising builder, missing end-marker, timeout, unparseable output. "Could not
  run" never reads as "no difference found" (the P4.2 error).

  **STAGE-1 GATE, pre-registered before any roll:**
  (a) both canaries pass on the live artefacts;
  (b) ZERO facts emitted from a reference the screen or pin-check discarded;
  (c) the fact visibly engages the disputed formula in the judge's evidence;
  (d) measurement rule 7 — two iterations with no change → stop and report.
  Read-out is per-event: disputed observable detected → reference generated →
  each validator's decision WITH its reason → fact emitted → judge engagement.

- [ ] ~~Stage 0 (original brief)~~ Generation prompt + a standalone
  compile-and-run adapter. The mirror canary becomes a real executed test
  (fake patch + correct check → the reference must side WITH the check).
  Every fail-closed path unit-tested. One design question to settle HERE,
  because stage 1 depends on it: **is the fact two-sided?** The current core
  emits only disagreement facts; Math-65's false accusation needs the
  AGREEMENT side ("an independent doc-derived implementation computes exactly
  what the patch computes") — symmetric, like the Math-65 fact block.
  ~50k tokens of prompt shakeout.
  *Design guidance settled with the main session (2026-08-06), for the doc:*
  (1) Prefer ONE two-sided fact over two facts — same sentence shape, the
  comparison result differs ("an independent implementation derived from the
  documentation, matching the buggy build on N off-defect observables,
  computes X at the disputed point; the patched build computes X / computes
  Y"). Computed-fact wording only, no dismissal or keep instruction — the
  judge decides. (2) The GUARD SPLITS BY OUTCOME, not by fact: the
  agreement outcome pushes toward exoneration → guarded by the 67-row
  genuine-catch fixture; the disagreement outcome pushes toward accusation →
  guarded by the 38-row correct-dismissals fixture + clean legs. (3) TWO
  canaries, one per direction: the existing mirror (fake patch + correct
  check → side with the check) AND its twin (correct patch + wrong check,
  the Math-65 shape → side with the patch). (4) Agreement inherits
  WEAK_KINDS/_close semantics — a value_ulp difference IS agreement.
  (5) All of this rides on the screen's integrity — which is why the screen
  is the design, and why its escape-decode fix (9.1b) had to precede this.
  (6) *The information rule (user-shaped 2026-08-06): BLIND TO
  IMPLEMENTATIONS, MAXIMAL ON SPECIFICATION.* Never shown: the patched
  source (rank-5 trap) or the BUGGY implementation body — a bug-copying
  reference agrees with buggy everywhere INCLUDING the defect, so the
  off-defect screen structurally cannot catch it, and it then disagrees
  with a correct patch at exactly the disputed point. Shown, as richly as
  available: the failing test's full source (tier-1 — leaking it leaks
  only truth), the class's OTHER tests (the project's executable spec —
  note this is the deleted mined-oracles MATERIAL in a different, legitimate
  use: spec context, not copied assertions), the whole documentation
  surface (class-level javadoc, sibling-method contracts, inherited
  interface docs, cited formulas/algorithm names — textbook knowledge of
  the algorithm is wanted), and the class skeleton (all signatures and
  fields, no bodies). (7) *Observed-behavior examples with a HOLDOUT:* a
  few of 8.3's recorded off-defect input→output examples may be shown so
  the reference gets conventions right (return −1 vs throw, units) — but
  the screen validates ONLY on observables the generator was NOT shown;
  what it saw is an open book and proves nothing. (8) *Third validator:*
  where the disputed point overlaps the failing test's own inputs, the
  reference must match the test's PINNED answer (tier-1) or be discarded —
  catches bug-copying, the screen's one structural blind spot.
- [ ] **Stage 1 — n=1: Math-65 correct leg** (the motivating case). Per-event
  read: disputed observable detected? reference generated? screen
  admitted/discarded (and WHY)? fact emitted? did the judge engage it?
  Iterate the prompt/adapter here where a roll costs ~150–250k. Gate to
  stage 2: canary passes; zero facts from discarded references; the fact
  visibly engages the disputed formula in the judge's evidence.
  **MAIN-SESSION DEBUG PASS (2026-08-07, 1b802dc) — the chain rebuilt on the
  STATE-TWIN architecture before roll 5.** Roll 4's call-site lag was one of
  FOUR integration defects: the screen compared the reference to ITSELF
  (always-admit, fail-open — the worst); the pin check's keys could never
  overlap (permanent silent abstention); the fact's keys could never match
  (no fact, ever). Root cause under all four: the chain never settled WHAT
  STATE the three programs compare at. Answer now built: one twin driver
  replays the failing test's setup on BOTH builds; the reference's inputs
  come from the twin's reflection-printed state; screen =
  reference-vs-buggy on siblings, fact = reference-vs-patched at the same
  state, pin = the test's own answer at its own state (validator 3 is now
  real). The screen's exam is open-INPUT/closed-OUTPUT and the fact's
  standing sentence says so. Signature handling per the roll-4 finding:
  nominal matching to canonical state fields, unmappable = discard;
  build_driver now raises TypeError on a str (the roll-4 bug made
  impossible). Per-leg memo: both doors reuse one resolution. The
  production-path walkthrough is now a TEST (drives run.py's own function
  with stubbed generator/JVM; roll 4's five signatures are fixtures). 704
  passed, 7 skipped. **Before roll 5 the agent re-walks on the VM** — the
  local pass proves wiring and logic; javac and the real twin compiles it
  cannot (no local JRE), and the twin's test-setup compile is the likeliest
  next failure (missing test-class imports/helpers → honest discard).
  *VM re-walk #1 found three compile failures; all fixed at the root
  (3a649d3, main session): (1) input shape — the chain receives annotated
  BLOBS, so the method is now isolated brace-matched, the receiver comes
  from sibling-call counts (testCircleFitting never calls getChiSquare),
  and fixture classes the setup needs (`new Circle()`) are extracted from
  the test FILE and emitted beside the driver; (2) encoding — every
  non-ASCII char becomes its unicode escape, semantically identical under
  javac's pre-lexical escape processing; (3) structure — assertion
  stripping is statement-aware (line-dropping ate closing braces), and
  both setup and emitted twin are brace-balance-guarded, raising for an
  honest discard rather than emitting invalid Java. Eight new pins from
  the real Math-65 material. 712 passed, 7 skipped. VM re-walk #2 is
  roll 5's gate.*
  *VM re-walk #2 (2026-08-07): all three prior failures GONE; two new ones,
  both fixed at root (bbc0039, main session). (a) RECEIVER — usage-pattern
  selection deleted entirely: "last-constructed" picked `center` (roll 5),
  "most-called" picked the optimizer's RESULT object (re-walk #2). The
  receiver must DECLARE the disputed observable — `types_declaring()` walks
  the class context plus one `extends` level, a by-call candidate whose
  declared type is NOT a declaring type is now VETOED (closing the
  silent-wrong-state case: a same-named method on another type would have
  compiled and read the wrong object), and no type evidence = discard with
  the declaring types named. (b) PACKAGE — the twin is emitted into the test
  file's own package, restoring the simple-name resolution the original test
  method had. 717 passed, 7 skipped; pushed. VM re-walk #3 is roll 5's gate.
  Residual risks named for that walk: a fixture helper referencing test-class
  FIELDS (honest discard), and whatever `optimize(...)` needs at runtime
  (twin runs, `__construct0` reports).*
  *VM re-walk #3 (2026-08-07): everything else held (package, helpers,
  escaping, braces); one blocker — `types_declaring` returned {'name'} six
  times because the class context is XML-WRAPPED and the bare pattern
  matched the ATTRIBUTE, hiding the actual declarer. Fixed (6952492, main
  session): both shapes parsed, transitive extends closure, bare pass
  requires an uppercase-initial name not preceded by '<' (javadoc prose was
  yielding 'for'); verified against ladder1e's REAL context → exactly
  {AbstractLeastSquaresOptimizer, LevenbergMarquardtOptimizer}. Plus the
  agent's ask: `plausible_class_names()` + a `declaring-type PARSE BROKEN`
  discard, because a broken extractor and "no declaring type in this leg"
  are different failures and only one is about the leg. THIRD instance of
  the check-your-matcher lesson (16 observables read as 2; 57%-token
  matching; now 6 class names read as 'name') — all fixture sets in the
  state-twin test file are now real recorded material. 722 passed, 7
  skipped; pushed. VM re-walk #4 is roll 5's gate.*
  *VM re-walk #4 (2026-08-07): everything from #1–#3 held; the last compile
  error plus one defect the walk's harness masked, both fixed (8e575d2,
  main session). (a) UNIQUE READS — r0/r1/… instead of reusing `Object r`
  in all three emitters (already try-scoped, but a name that cannot collide
  removes the mode entirely). (b) SIBLINGS SCOPED TO THE RECEIVER'S TYPE —
  the agent attributed the `getPoint` error to their hand-written list, but
  the production extractor had it too: an unscoped scan of the context
  returns collaborators' observables (getPoint/getPointRef/getArgument live
  on the RESULT object), which the twin then calls on the optimizer and
  cannot compile. Now scoped to the declaring types' blocks, resolved
  BEFORE the screening surface so the two always agree: 8 unscoped (3
  uncallable) → 6 scoped on ladder1e's real context. (c) STORED SETTINGS
  EXCLUDED (agent's design point, accepted) — getMax*/getDefault* agree for
  free, so counting them made "8 observables" really "6 plus 2 free
  passes"; exclusion can push a leg below the bar, which is the honest
  outcome. 726 passed, 7 skipped; pushed. VM re-walk #5 is roll 5's gate —
  and if the twin compiles, its run is the first real test of whether
  `optimize(...)` completes, the last unknown before step 6.*
  *VM re-walk #5 (2026-08-07): **GATE CLEARED** — the twin compiles and
  runs, `optimize(...)` completes at test state, the disputed observable
  and siblings compute, and reflection recovers the state fields
  (`__construct0=OK`, getChiSquare=1.5633763529538318, residuals/weights/
  cost recovered). The last unknown is closed. One defect only a run could
  show: `String.valueOf(double[])` printed `[D@19469ea2`, a per-invocation
  identity hash — 2 of the 6 siblings are arrays, so both would have
  "disagreed" permanently and the effective surface was silently 4. Fixed
  (8082487, main session) with ONE formatter injected into all three
  emitters AND the reflection path, since identical formatting on both
  sides is the entire point of the comparison. 737 passed, 7 skipped;
  pushed. **Roll 5 is GO** — the chain is end-to-end runnable; the desk
  walkthrough stays standing practice for stage 2+ (5 walks, 8 distinct
  defects, each of which would otherwise have surfaced as an opaque
  mid-roll discard at ~250k).*
  *ROLL 5 (2026-08-07): deepest yet — steps 1–5 all worked on the real leg
  (2,455-char multi-observable reference; signature named residuals /
  residualsWeights / cost, Math-65's true dependency; 6 computed siblings
  resolved), then discarded at the omits-the-observable validator. Cause
  was SPELLING, not semantics: the model wrote `compute_chiSquare` where
  the chain wanted `compute_getChiSquare` (guessParametersErrors matched
  only because it has no prefix to drop). Fixed (51991f7, main session)
  the way the codebase already solved it — `_methods_named_by` has matched
  chiSquare→getChiSquare since P0: matching is normalized both ways, and
  the driver now CALLS the declared name while KEYING by the canonical one
  (they legitimately differ after normalization; the EX: path carries the
  canonical key too). Gate (b) held again: 11-for-11, zero facts from
  discarded references. 742 passed, 7 skipped; pushed. **Roll 6 is GO** —
  next unknown is the screen itself: does a doc-derived reference actually
  reproduce the buggy build on the 6 siblings?*
  *ROLL 6 (2026-08-07): 8 chain steps — deepest yet. CONFIRMED WORKING on
  real material: name normalization (chiSquare→getChiSquare, RMS→getRMS,
  `cost` correctly declared-only and never called), signature mapping (2
  params → real state fields), setup extraction (receiver `optimizer`),
  twin build (1 helper, 13 imports, real package), and per-leg memoization
  (3 later firings reused the resolution). Discarded at the twin RUN: exit
  1, no end marker — while the same twin source ran fine by hand in
  re-walk #5, localising it to the seam. CAUSE (found by reading build.py,
  not by re-rolling): `HarnessBuilder.build` compiles with `-d <fuzz_dir>`
  but `BuildResult.classpath` carries only project cp + jazzer API jar, so
  `java -cp` could not find the class it had just built; the pipeline's own
  Jazzer runner has always appended the harness dir. Ninth integration
  defect, same shape as the other eight. Fixed (343d887, main session) for
  run_twin AND run_reference via one shared `_runtime_classpath()` (cwd
  anchored too), plus the agent's ask: the JVM's own stdout/stderr now
  rides in the discard reason (`_jvm_failure_reason`), so a missing class,
  a thrown exception and a silent exit are distinguishable in one read.
  Gate (b) 12-for-12. 748 passed, 7 skipped; pushed. **Roll 7 is GO** —
  the screen question is still the next unknown, now one seam closer.*
  *ROLL 7 (2026-08-07): discarded one step EARLIER than roll 6, unrelated
  cause — model variance in the declaration line (roll 6 named its
  parameters, roll 7 declared bare `double[], double[], double`). Note the
  classpath fix from roll 6 was never reached, so it remains untested.
  Fixed (0116e6f, main session) the roll-5 way — accommodate the deviation
  mechanically: `fields_read_by()` derives the state a computation consumes
  from the BUGGY body in code order (getChiSquare → rows, residuals,
  residualsWeights; legitimate rank-2 authority read by OUR code, the
  generator still never sees it), and unnamed parameters map positionally
  against that. Two silent-wrong-input paths closed on the way, both found
  by the new tests rather than by a roll: a NAMED parameter no longer falls
  back to type (`residuals` is a substring of `residualsWeights` — a type
  fallback maps one onto the other and feeds the reference the wrong array
  while compiling and running perfectly), and an UNNAMED parameter maps
  only to fields the method reads. Roll 7's exact signature still discards
  (its third `double` answers to nothing getChiSquare reads) with the read
  fields named. Also corrected: the error truncated the field list at 8,
  which made `residuals` look absent — it is field 10 of 39, so roll 6's
  mapping was exact all along. Gate (b) 13-for-13; rule 7 still unstarted
  (every roll a different mechanism). 755 passed, 7 skipped; pushed.
  **Roll 8 is GO** — the screen question is STILL the next unknown, and
  the untested classpath seam sits directly before it.*
  *ROLL 8 (2026-08-07): discarded at signature mapping again — and the
  improved message diagnosed it in one read (`Fields read by the method:
  []`). Cause: `fields_read_by` was built, tested and wired at ZERO call
  sites; `match_parameters` took `read_order` as an OPTIONAL third
  argument and the call site passed two, so production ran with None for a
  whole roll while unit tests stayed green. Eleventh integration defect,
  and the SECOND of exactly this shape after roll 2's Spec K. Fixed
  (44d6469, main session): call site wired; `read_order` made REQUIRED so
  omission is a TypeError rather than a silent degradation (callers with
  no visible body pass `[]`, stating the fact instead of defaulting into
  it); and a generalized SEAM TEST — every helper the chain imports from
  reference_run/reference_impl must actually be called in the chain, since
  a mechanism imported but never invoked is a mechanism that does not
  exist. **Pattern now worth stating as a rule: all eleven defects in this
  ladder have been SEAMS, never pieces — the pieces were right on first
  writing almost without exception. Tests that call functions cannot see
  this; only tests that read or drive the call site can.** Gate (b)
  14-for-14; rule 7 still unstarted. 759 passed, 7 skipped; pushed.
  **Roll 9 is GO** — unchanged stack: the twin's first production run,
  then the screen question.*
  *PRE-ROLL-9 WALK (2026-08-07, second session): the recorded rolls
  replayed through the code roll 9 would run, BEFORE spending it. Three
  seams found and fixed. (1) SWAP — the wired mapper would have fed roll
  8's reference its two arrays REVERSED: the comment line is bare types,
  but the model's own `compute_*` declarations name both parameters, in
  the OPPOSITE order from the buggy body's read order. Same type, same
  length: compiles, runs, computes garbage, and the screen's discard
  would then have read as "a doc-derived reference cannot reproduce the
  buggy build" — a wrong answer to the ladder's own question. Fixed:
  `merge_declared_parameter_names` adopts the declarations' names into
  unnamed comment positions, gated on every name resolving to a canonical
  field (a model that names its parameters `r, w` has named nothing a
  field answers to; the merge declines and read-order stays in play).
  (2) THIN — rolls 6/7/8 all declared ONE countable sibling
  ({chiSquare, RMS(, cost)}) against a screen bar of THREE, because the
  prompt never named the siblings that count. A mechanically perfect roll
  9 would still have discarded at the screen. Fixed: the surface is
  resolved BEFORE the generation (a broken declaring-type parse now costs
  zero model calls) and the prompt names the exact siblings, the bar, and
  the counter caveat (getEvaluations/getIterations/getJacobianEvaluations
  are bookkeeping, not derivable from state — the realistic shared set is
  getRMS + getCovariances + guessParametersErrors, exactly the bar).
  (3) LATE BAR — MIN_SCREENED_OBSERVABLES was enforced only inside
  `screen_reference`, after the twin build and two JVM runs, though the
  count is knowable at the match step; `too_thin_to_screen` now decides
  it there, fail-closed, same sign. Roll 8's verbatim reference is the
  fixture, driven chain-level: names recovered, thin discard, zero JVM
  calls. Seam tests extended to ORDER (siblings resolved before the
  prompt is built; merge before mapping; thin bar before run_twin).
  Also confirmed on the way: the 719-vs-759 test-count scare is the
  documented invocation-scope phantom (bare `pytest` collects both
  roots), not a regression. 772 passed, 7 skipped. **Roll 9 stays GO,
  now with the swap dead, the bar reachable, and the untested seams
  reduced to the genuinely runtime ones: the twin's first production
  run (classpath), then the screen.***
  *VM RE-WALK #7 (2026-08-07): **the twin runs in PRODUCTION** — run_twin
  through HarnessBuilder and `_runtime_classpath` found its own compiled
  class, ran to the end marker, returned 11 keys. The roll-6 classpath
  seam (343d887) and the shared formatter (8082487) are both proven in
  production for the first time — getCovariances and
  guessParametersErrors print by value, nested arrays deeply, not
  `[[D@...`; those two were the permanent-false-disagreement hazard.
  Two verbatim cross-validations, both against tier-1 material: the
  twin's buggy-build `guessParametersErrors[0] = 0.0019737107108948474`
  is character-for-character the value the failing test itself reported
  (`expected:<0.004> but was:<0.0019737107108948474>`) — the twin is
  provably AT the failing assertion's own state, reproducing the bug's
  recorded symptom through a different path (twin+reflection vs JUnit);
  and `getChiSquare = 1.5633763529538318` equals re-walk #5's hand-run
  value. The whole upstream chain held on real material: declaring
  types, six scoped siblings, receiver `optimizer`, helper + package
  assembly, `__construct0=OK` (optimize completed at test state).
  Twelve integration defects found so far, eleven at desk cost; gate (b)
  14-for-14. **Roll 9 is GO with ONE unknown left, the substantive one:
  does a doc-derived reference reproduce the buggy build on those six
  siblings?** Named reach caveat, not a defect: if the model again
  implements only {RMS, chiSquare}, roll 9 discards at the thin bar,
  honestly and before the JVM — getCovariances/guessParametersErrors
  are the hard asks the new prompt names but cannot compel.*
  *ROLL 9 (2026-08-07): TEN steps deep, the furthest by far, and all
  three pre-walk fixes worked on real material — the surface resolved
  before generation, the model implemented the HARD siblings
  (getCovariances, guessParametersErrors, getRMS — the reach question
  answered favourably; 3,820 chars vs roll 8's 634), the bar read
  reachable at 3 shared, the six-parameter signature (incl.
  `double[][] jacobian`) mapped, and the buggy twin ran INSIDE a
  production roll (14 keys — the classpath seam now proven twice).
  Died at step 11: `driver did not compile`, reason carrying no javac
  output. Desk replay of the verbatim signature found the killer
  without a re-roll (70e6875, second session): `java_literal` stripped
  every `[]` from a multi-dimensional type and passed deepToString's
  inner text through — `new double[]{[...], ...}` — non-None, so the
  chain proceeded and javac refused. Roll 9's reference was the FIRST
  to take a 2-D parameter; the literal builder had never been asked.
  Fixed: nested brackets → nested braces, full dimensionality on the
  `new`, numeric/boolean elements only (their text never contains a
  bracket), String/Object fail closed. Second seam, same read: all
  three COMPILE branches returned bare messages while
  BuildResult.stderr held javac's words — the roll-6 attribution
  treatment covered only the RUN phase; `_compile_failure_reason` now
  rides the javac head into every compile discard, seam-tested against
  bypass. The roll-9 parameter list and re-walk #7's real covariances
  matrix are the fixtures. Gate (b) 15-for-15; rule 7 still unstarted
  (fourteenth defect, fourteenth distinct mechanism). 777 passed, 7
  skipped; pushed. **Roll 10 is GO — the remaining stack is exactly
  one step: the reference RUNS, and the screen finally answers.**
  Named residual risks for the read: `Infinity`/`NaN` in a printed
  value would not compile as a literal (unhandled, now visible in the
  javac words if it fires), and the reference's own compile is the
  same class of first-time seam.*
  *ROLL 10 (2026-08-07): TWELVE steps, deepest yet; the attribution fix
  worked immediately (javac's words in the discard), name recovery ran
  live (bare 5-type comment → named, 5-of-5 mapped), the 2-D literal
  emitted valid Java, the twin ran again in production (13 keys).
  Died: `cannot find symbol` at the driver's first call to
  ReferenceImpl. DECIDED BY READING build(), no re-roll (d183077,
  second session): javac's `-d` output dir was never on the COMPILE
  classpath, so the driver could not see the class the previous
  build() call had just produced — roll 6's seam ONE PHASE OVER
  (runtime fixed, compile not; the twin never hit it because a twin is
  one self-contained source, and reference+driver is the chain's only
  two-artifact compile). Fifteenth defect. build() gains
  `extra_classpath` (prepended to -cp); run_reference hands the driver
  the reference's class dir — the same dir the runtime path already
  appends. Sixteenth, found by the same read: the compile reason's raw
  head spent its character budget on javac's source-line echo (the
  long literal argument) and cut off `symbol:`/`location:` — the two
  lines that decide class-missing vs method-missing.
  `_compile_failure_reason` now keeps the structural lines, capped per
  line; roll 10's verbatim stderr is the fixture (symbol and location
  survive, the literal does not). Seam-tested at both levels. Gate (b)
  16-for-16; rule 7 still unstarted. 780 passed, 7 skipped; pushed.
  **Roll 11 is GO — same one-step stack as before, now with the
  compile seam closed: the reference runs, and the screen answers.***
  *VM RE-WALK #8 (2026-08-07, second session, user-directed: "debug
  without burning rolls") — THE WHOLE remaining path replayed on the VM
  with roll 10's RECORDED artifacts (`scripts/rewalk8_replay.py`;
  ladder1k's checkout, reference, and twin were still on disk).
  Production functions only, zero generation cost, iterated to green.
  The method change it encodes: every prior walk verified up to the
  frontier, not THROUGH it — that is why each roll bought exactly one
  seam. Findings: DEFECT 17 (would have killed roll 11 on any
  classpath): `code too large` — ~630 fitted points → ~15KB per array
  literal, inlined once per observable call, main() over the JVM's 64KB
  bytecode-per-method cap; roll 10's `cannot find symbol` was merely
  the first error javac reported, this one was stacked behind it.
  Fixed (76d1be6): literals HOISTED, one static field + one initializer
  method each, call sites reference the field; oversized single
  literals discard loudly. DEFECT 18 (on real values): the buggy
  build's OWN covariance matrix is one ulp asymmetric in print, and
  exact string comparison read it as semantic disagreement — arrays now
  agree per element within the rounding floor; the real divergences
  still disagree. THE SCREEN'S FIRST REAL ANSWER, the walk's prize:
  getRMS agrees EXACTLY, covariances agree modulo one ulp, and the two
  true disagreements are the DISPUTED observable itself (ref = 4.000×
  buggy — the multiply-vs-divide weighting that IS Math-65's defect)
  and guessParametersErrors, where the reference computes ~0.00395 —
  the failing test's own expected 0.004±0.001, against buggy's 0.00197.
  The reference sides with tier-1 authority exactly where the defect
  reaches. Current design discards there: the sibling surface contains
  an observable the failing test itself shows diverging, so Math-65's
  reference can NEVER be admitted as-is. **DESIGN DECISION AT THE GATE
  (not taken unilaterally): (a) exclude defect-reached siblings
  (mechanically knowable from the failure message) — shared drops to 2,
  below the bar of 3, honest thin-discard but the mechanism never helps
  Math-65; or (b) a sibling where the reference matches the TEST'S
  pinned value against buggy counts as pro-admission corroboration
  (pin-check logic extended to siblings) — converts the strongest
  apparent counter-signal into the strongest evidence. Roll 11 without
  this decision will run clean and discard at the screen, honestly.**
  Pin check verified to ABSTAIN on no overlap. 784 passed, 7 skipped;
  pushed. Eighteen defects total; 17 and 18 found by the same walk, at
  desk cost.*
  *RE-WALK #8 ADDENDUM — THE PATCHED SIDE (user-prompted: "then we
  check the patch, right?"). `scripts/rewalk8_patched_side.py` ran the
  recorded twin on ladder1k's PATCHED build — the chain's final step,
  never yet reached in a roll because the screen discards first. The
  would-be fact is total agreement, DIGIT FOR DIGIT: getChiSquare
  patched=6.253505411815327 = reference exactly; guessParametersErrors
  patched=[0.003947421421789695, 0.003953773486615504] = reference
  exactly (and inside the failing test's 0.004±0.001); getRMS all
  three builds identical. An independent documentation-derived
  implementation computes EXACTLY what the patch computes at the
  disputed point and everywhere else it reports — the exoneration
  evidence 8.2 was designed to produce for Math-65, sitting one
  screen-design decision away from the judge.*
  *THE GATE DECISION — TAKEN (user, 2026-08-07): OPTION B, implemented
  (14851e0) and verified by rerunning re-walk #8 with production
  functions on verbatim recorded material. A defect-reached sibling
  re-grades against the failing test's own asserted literal, with both
  conditions required: the reference matches the test's value within
  the TEST'S OWN tolerance AND the buggy build fails the same pin (the
  second contains the open-book concern — the test's answer overrides
  the buggy build only where buggy is demonstrably wrong). Attribution
  is mechanical: a pin attaches only where the failure message's
  `was:<...>` value appears verbatim in the twin's buggy print (the
  re-walk-#7 state identity). Walk result: pins resolve to
  {guessParametersErrors: (0.004, 0.001)}, THE SCREEN ADMITS — 3
  shared: getRMS exact, getCovariances per-element, errors re-graded —
  and the admit reason names the re-graded sibling. With the addendum's
  patched-side run (digit-for-digit agreement), the COMPLETE
  exoneration chain for Math-65 is verified end to end on recorded
  material. 789 passed, 7 skipped; pushed. **Roll 11 is GO — for the
  first time with zero known mechanical unknowns: the only new event
  is a fresh generation's content, and the read is whether the live
  chain produces the fact and the judge engages it (stage-1 gate (c),
  at last).***
  *ROLL 11 (2026-08-07): **THE SCREEN ADMITTED — a first.** Option B ran
  exactly as designed: 3 shared, the re-graded sibling named, the two
  evidence kinds kept separate. Died one step later: DEFECT 19, the
  first inside the mechanism's judgement rather than its plumbing — the
  pin check discarded the correctly-diverging reference (6.253505411815327
  = 4.000× buggy, the defect's own weighting) against 1.768262623567235,
  a literal asserted on `Math.sqrt(circle.getN()) * rms`: an RMS line's
  answer key, mapped onto getChiSquare because the chain fed
  `{method: trusted_values}` by construction. Fixed (4e5d9cb, second
  session): `pins_for_disputed` gives validator 3 the SAME attribution
  discipline as corroboration — state identity (was-value verbatim in
  the twin's buggy print for THAT observable) or a direct assertion on
  the disputed method; pins carry the test's own tolerance and
  `pin_check` honours it; no attribution → ABSTAIN, stated. The
  bug-copy catch is preserved (direct-assertion pins still attach;
  canaries green). Walk rerun against roll 11's OWN artifacts: the
  fresh generation — different parameter order — computes the same
  values DIGIT FOR DIGIT as roll 10's (two independent generations
  converging: reproducibility the mechanism did not have before);
  screen ADMITS, pins {} → ABSTAIN. Gate (b) 17-for-17 (zero facts
  after the discard, as specified). 793 passed, 7 skipped; pushed.
  **Roll 12 is GO — the remaining read is the stage-1 gate itself:
  fact emitted, judge engagement.***
  *ROLL 12 (2026-08-07): **THE CHAIN COMPLETED END TO END — and gate (c)
  FAILED.** Every mechanical step worked live: screen ADMITTED (3
  off-defect, 1 re-graded), no pin attaches → explicit ABSTAIN, patched
  twin ran (13 keys), FACT EMITTED and delivered into 9 of 17 judge
  prompts. Engagement: ZERO of 9. No WHY mentions the independent
  implementation; no CITATION quotes the fact. The dismissals the judge
  did reach ([5],[13],[16]) answer the disputed multiply-vs-divide
  question FROM THE JAVADOC — "chi-square as Σ w·(target−f)² (because
  the variance is 1/w)" — with the digit-for-digit fact unquoted in the
  same prompt; and the judge is INTERNALLY INCONSISTENT on the same
  formula (kept [10] endorses the divide form as "the documented
  contract"). Final outcome FP (relation-replay-conviction). Read: the
  judge is not evidence-starved but evidence-blind on this axis — the
  FIFTH independent negative (cycle 8 measured delivery, placement,
  questioning, judge model; this adds a new EVIDENCE KIND) and the
  strongest yet that the ceiling is architectural, not informational.
  The mechanism itself is fully positive: admission works, two
  independent generations converge digit-for-digit, the exoneration
  chain is real and verified. **STAGE-1 GATE (c) IS NOT MET; per the
  pre-registered ladder, no advance without an explicit decision.
  PENDING USER: (i) close 8.2 as measured-and-recorded; (ii) override
  the gate and run stage 2's conviction direction (a different ask —
  disagreement fact on a fake patch vs agreement fact against a trusted
  javadoc); (iii) repurpose the ADMITTED fact as a DETERMINISTIC gate
  (the mechanism-beats-instruction route: an admitted, thrice-validated
  reference that agrees with the patch digit-for-digit on the convicted
  observable voids/downgrades that conviction mechanically — design
  note + both guard-fixture populations BEFORE any build; recall risk
  is the 67-row genuine-catch fixture's to referee). Either way the
  outstanding generality checks stand: guard fixtures vs the option-B
  screen, and stage-2 material as held-out validation.***
- [ ] **Stage 2 — n=2: + Math-2 overfit leg** (documented mean formula — the
  conviction direction). Gate: both directions work — agreement-side on the
  correct leg, disagreement-side on the fake, zero facts where the screen
  discarded.
- [ ] **Stage 4 — + Math-30 correct leg** (the exact-arithmetic extension:
  a BigInteger reference disagrees with int arithmetic exactly at overflow
  boundaries — this is the test of that design) **+ one always-clean correct
  leg** (e.g. Math-53) as the live guard. Gate: no fact and no accusation on
  the clean leg; Math-30's evidence visibly changed.
- [ ] **Stage 8 — + Math-39-c, Math-2-c, two stable catches from the 58-row
  trigger-reach list, one more clean leg.** Gate (pre-register exact
  wording before running): ≥1 residual verdict moves the right way OR its
  kept-accusation evidence measurably engages the fact; ZERO clean-leg
  damage; admission rate and cost/leg recorded.
- [ ] **Stage 16 — half the 30-leg set**, all in-reach residuals + guard mix.
  Same gates + the reach measurement (code_context now recorded, so the
  8.2 reach question finally gets its pre-registered denominator).
- [ ] **Stage 30 — only if stage 16 passes its gates.** Then the batch
  assembles and the next full pair (on the user's word) measures it against
  the 8.10 bar.
**Stop rules:** measurement rule 7 (two iterations with no change → stop);
each stage's gates written before it runs; a clean-leg accusation traceable
to the fact at ANY stage is a hard stop pending diagnosis. Read-outs carry
BOTH SIGNS (catches gained/lost AND accusations gained/lost) per the
lottery lesson.

### 8.25 THE REFERENCE VERDICT GATE — design note ONLY (user-approved direction 2026-08-07; no build without fixture gates)
**Decision context.** Roll 12: the chain is fully correct and the judge is
evidence-blind to its fact (9 deliveries, 0 engagements — fifth negative on
the persuasion axis). User decision: stop persuading; make the admitted
fact DETERMINISTIC. This note is the required paper pass.

**The naive rule fails the recall guard BY CONSTRUCTION — recorded first
so it cannot be built by accident.** "Admitted reference agrees with the
patch at TEST STATE → void the conviction" is unsound: an overfit patch is
typically CORRECT at the test's own inputs (that is what overfitting to a
test means), so the reference would agree with a FAKE patch at test state
too, and the gate would void genuine catches — exactly the 67-row
population's damage mode. Agreement at test state discriminates
correct-vs-buggy, NOT correct-vs-overfit.

**The sound rule: the reference answers at the ACCUSER'S chosen input.**
A conviction is a relation firing at a concrete input on the patched
build. The reference is a pure function; replay the FIRING'S OWN input
through it:
  * reference sides with the PATCH at the firing input → the relation
    encodes the wrong contract (Math-65's [16]: relation asserts sum r^2/w,
    patch and reference both compute sum w*r^2) → VOID/downgrade;
  * reference sides with the RELATION against the patch → the patch is
    wrong at that input → conviction STANDS, now corroborated (a recall
    GAIN, not just a precision fix — both signs, per the lottery lesson).
Admission prerequisites unchanged and mandatory: screen (3+ siblings,
option-B re-grade), corroboration attribution, pin discipline. The gate
consumes only ADMITTED references.

**The build question (why this is a note, not a diff): state recovery at
the firing.** The twin recovers state at TEST state via reflection; a
firing's state (post-optimize with fuzz-chosen inputs) is not currently
recorded. Two candidate paths, decide at design review: (a) extend 8.4
raw-value recording so the harness reflection-prints the reference's
declared state fields AT the firing (a firing-twin: same _FMT_HELPER,
same __param_ discipline); (b) restrict phase 1 to firings whose message
already carries every input the reference's signature needs (scalar
metamorphic relations) and measure reach before widening. (b) is the
cheap honest start.

**Pre-registered gates, BEFORE any build:**
  (a) 67-row genuine-catch fixture: ZERO voids. Rows lack reference-chain
      data, so the run is a REPLAY of the gate's predicate over each row's
      firing input with the leg's reference where one exists, abstain
      where none — an abstain is a pass, a void is a fail.
  (b) 38-row correct-dismissals: untouched (the gate never manufactures a
      keep; sides-with-relation only CORROBORATES an existing keep).
  (c) Math-65 ladder leg: FP -> TN with the gate live, traceable to the
      gate's event in the trace.
  (d) A clean-leg accusation traceable to the gate at ANY point = hard
      stop (standing ladder rule).
  (e) Rule 7: two no-change iterations -> stop and report.
**PHASE 1 BUILT (5ba0508, second session, same day).** Predicate as
refined at build time: VOID only on self-proving value coincidence — all
firing-reported observables match the admitted reference's test-state
values, at least one DISCRIMINATING (reference differs from buggy there);
the 16-digit coincidence is itself the proof of same-state. Everything
else ABSTAINS with a split reason. THE DRY RUN CAUGHT THE UNSOUND HALF
BEFORE FIRST LIVE USE: 'disagreement -> corroborate' cannot distinguish
patch-wrong from state-different (fuzz-input firings legitimately differ
from test-state values), so corroboration is REMOVED from phase 1 and
deferred to the 8.4 firing-state extension — void is self-proving,
corroborate is not; the asymmetry is the honest shape. Gates run:
(a) 67-row zero voids (all abstain BY REACH — no admitted reference on
those legs; disclosed as abstention, not exoneration); (b) 38-row
untouched BY CONSTRUCTION (gate runs only inside the kept branch;
structural test pins it). Gate (c) = roll 13's read, honestly uncertain:
roll 12's three kept firings would all ABSTAIN (fuzz-input states) while
rolls 10/11's kept relations fired at test-coincident values and would
VOID — which relations a roll synthesizes and keeps is stochastic. Thin
reach -> 8.4 extension is the recorded next step, not another predicate
tweak. 799 passed, 7 skipped; pushed. **Roll 13 is GO.**
  *ROLL 13 (2026-08-07): TN — AND IT IS NOT THE GATE'S DOING (the
  launching session's stop-call, confirmed): zero harness crashes on the
  patched build, one judge call (vs 17), zero reference-chain admissions,
  zero gate events. A mechanism that never executed cannot cause a flip
  — this is the invention lottery's signature, and banking it would have
  been the Chart-19 attribution error at verdict-affecting stakes.
  Recorded as: no gate datum, one weak fail-closed datum (the gate was
  inert with its inputs absent), and one structural reach fact — the
  reference chain triggers only on a firing that reaches the judge, so
  legs that never accuse produce no reference (acceptable: the
  mechanism's job is judging accusations).*
  *CORPUS MEASUREMENT INSTEAD OF DRAWS (same day,
  `scripts/gate_corpus_scan.py`, event-level extraction after a first
  regex harvest returned empty strings and nearly recorded a false
  zero): the gate over EVERY recorded value-bearing firing, both doors,
  13 rolls. REPLAY door (where phase 1 is wired): 31 firings, 0 void —
  synthesized-relation replays fire at fuzz inputs; values never
  coincide with test state. HARNESS door (NOT wired): 26 patched-build
  firings, 2 VOID — ladder1g `circle-dense-chi-formula` and ladder1k
  `constant-weight-rms-chi`; the fuzz harness replays the failing
  test's scenario, so ITS firings carry test-state-coincident values.
  **THE DOOR WITH REACH IS THE UNWIRED ONE — Spec-K one-door parity,
  now with measured stakes.** Neither roll flips outright: ladder1g's
  second patched firing prints `getChiSquare()=` with the REFERENCE'S
  EXACT VALUE but the `()` on the key defeats parsing (mechanical
  accommodation away from a full void); ladder1k's prints only lhs/rhs
  (unreachable without 8.4). SCOPED NEXT, in order: (i) wire the gate
  at the harness-track adjudication site — door parity; (ii) key-parse
  accommodation for the `obs()=value` shape; (iii) the 8.4
  firing-state extension for the lhs/rhs population. Measurement
  before mechanism, as the rules require — none built yet.*
  *ITEMS (i) AND (ii) BUILT (same day, user go): the gate is wired at
  the harness-track adjudication site — same contract as the replay
  door (runs only on a keep, void records a drop reason and skips the
  keep, abstain changes nothing) — and the gate's parser now reads the
  harness's method-call echo (`getChiSquare()=`) and truncates scalar
  values at the formula echo (`6.25 sum((...` was one "value" and a
  matching number read as a mismatch). Corpus re-scan: replay 31/0
  unchanged; harness-patched 26 firings now 3 VOID — ladder1g's leg
  FULLY voided (both patched firings), ladder1k still partial (its
  second firing prints only lhs/rhs — the 8.4 population, still
  parked). Zero voids anywhere a void would be wrong. Both-door seam
  test pins the wiring. 801 passed, 7 skipped.*
  *STAGE 2 PREPPED (user-approved): `suites/cases/ladder_stage2.cases`
  — TWO legs, SERIAL (Math-65-c + Math-2-o Arja), and
  `docs/replay/backtrack/STAGE2-HOW-TO-READ.md` with the gates
  pre-registered: S2-a ZERO voids on the fake-patch leg (any void =
  gate destroys a catch = HARD STOP), S2-b TP retained, S2-c the
  disagreement direction observed, S2-d Math-65 counts for the gate
  ONLY via a traceable void event (roll 13's attribution lesson,
  standing), S2-e rule 7. The scaling answer to "test multiple": this
  IS the pre-registered n=2; stage 4 widens to four legs only on a
  stage-2 pass.*
  *STAGE 2 ROLL 1 (2026-08-07, stage2_20260807_213510): **DOES NOT
  PASS — stage 4 does not fire.** Math-65 FP / Math-2 TP. S2-a passed
  VACUOUSLY (0 voids, but by "no admitted reference", not by looking
  and declining); S2-b passed (catch intact); S2-c NOT OBSERVED;
  S2-d NOT MET (0 voids; Math-65's kept firings printed observables
  the reference doesn't compute — the 8.4 fuzz-state population, as
  predicted). THE SUBSTANTIVE FINDING: reference-chain reach on the
  first held-out bug was ZERO — one detect event, "the firing names
  no method whose body is shown". DIAGNOSIS (free trace read, same
  day): two distinct mechanical causes, neither a reach ceiling.
  (1) Harness door: firings name `sample`, whose body the context
  ELIDES (the Arja patch didn't touch it) — correct fail-closed
  decline. (2) Replay door, the real gap: the kept relation's check
  CALLS `dist.getNumericalMean()` and recomputes the documented mean
  n*m/N — the exact dispute the leg exists for, body SHOWN, 86
  context mentions — but its fired MESSAGE prints only
  `actual=/expected=`, and the detector read only the message. The
  mechanism's home-turf case was present and detectable; the detector
  was reading the least informative artifact while the most
  informative (the check source, which the judge already receives)
  sat unread in the same call. FIXED (same day): `disputed_observables`
  gains `check_source` — methods the check CALLS (exact call syntax,
  the narrow matcher) join the candidates; body-shown requirement
  unchanged (the `sample` decline stands); both doors pass their
  check source through. Verbatim Math-2 fixtures. 804 passed, 7
  skipped. **NEXT: re-roll stage 2 (the ladder's iterate-at-stage
  loop), same gates. Expected on Math-2 with the widened detector:
  detection → generation → the first screen/admission attempt on
  held-out material — each step is new reach territory and may fail
  honestly; the read is per-event as ever.***

**Reach honesty:** the gate exists only where the chain admits a
reference (disputed-observable detection measured 25.4% of rows; admission
is rarer). Phase-1 (b)-scope reach is measured and reported with the
first fixture run — a silent-cap disclosure, not a footnote.

### Parked, unchanged
Focused synthesis (both-signs live A/B, expensive, not now) · fresh12
(user's literal phrase only) · the next full 30-leg pair (user's word, after
the ladder says 8.2 is worth measuring at scale).

## POST-PAIR FIX BATCH (2026-08-04, user-approved) — the next work package

Order: free reads first (8.22's confirming read, 8.24), then the tiny code
(8.23, 8.21a, 8.21c — one mini-batch, unit-tested, no verdict surface), then
8.22's fix measured ALONE. Nothing here is a revert; the cycle-8 batch stands.

### 8.22 Raw-value containment — GATED on its confirming read
**Target:** evidence assembly for the judge (station 7 input): the alarm text
quoted into judge prompts, fed from station 4's fired messages.
**Failure mode:** 8.4's raw values were built for the CODE comparison (safe —
zero wrongful dismissals) but also travel inside the alarm text the judge
reads, where they became material for "a correct implementation could…"
dismissals: Closure-92 (caught both July rolls) missed BOTH new rolls with
every firing dismissed quoting actualRaw; Closure-38's dismissal builds on the
raw spelling (`x- -736E3` vs `x- -736000`). The judge should receive
conclusions COMPUTED from raw values, never the raw strings as prompt text.
**Gate (free, FIRST):** side-by-side read of Closure-92's July keep-chains vs
the new dismiss-chains — confirm the raw text is the load-bearing difference
(not, e.g., a different check shape). If not confirmed, stop here and record.
**GATE OUTCOME (2026-08-04): NOT CONFIRMED — the fix dies for free.** Three
strands: (1) the one check judged in both eras (`lifted-seed`) got the SAME
verdict, UNSOUND, with raw cited in the new roll and without it in July — the
raw text flipped nothing; (2) roll B missed with raw cited in only 1 of 6
dismissals and still kept nothing; (3) the check sets barely overlap (3 of
~12 distinct oracles shared) — July's keeps came from checks the new rolls
never drew. Roll A's 5-of-5 raw citations were CO-PRESENCE, not causation:
8.4 puts raw values in every normalizing alarm, so every dismissal of one
quotes them regardless — the change manufactures its own correlation, the
exact trap the gate existed to catch. Honest limit recorded: strand 1 is a
single shared-check comparison; strands 2 and 3 are independent of it, and
not-confirmed is the conservative direction (stops a build). **Closure-92 is
reclassified: the invention lottery, same as Math-2 — July drew checks that
survived, these rolls drew checks that didn't. The
"raw-values-as-hypothetical-fodder" channel (named in the 44faf24 forensics)
is DEMOTED to a watch item: re-openable only if a with/without replay ever
flips a specific verdict; no evidence currently licenses that spend. 8.22's
containment is NOT built.**
**Fix (if confirmed):** consumer split, third application: the comparison
keeps raw; the judge-facing alarm quote carries the normalized form plus the
computed verdict line only ("raw output differs from / matches what the test
pins"). No wording iteration — this is a structural change to what enters the
prompt (rule 12 compliant).
**Validation, claims scoped per population (the 8.4 lesson):** offline —
re-judge the affected chains (pairA8/B8 Closure-92, Closure-38, plus any
67-row guard rows whose evidence text would change) with and without the
containment; the guard read is REGRESSION-only where rows carry no raw keys.
Live — per-event read on the next run. Verdict-affecting → measured alone,
never bundled.

### 8.23 Summary must refuse to generate on an incomplete run — ten lines
**Target:** `run_suite.sh` scoring/summary step.
**Failure mode:** pairA8's summary was written while leg 18 (Lang-22-c) was
still running — the official table covered 29 of 30 legs. Harmless this time;
a silently under-counting report is rule 15's family (a report that looks
complete and isn't).
**Fix:** count result.jsonl files vs the cases file before writing summary.md;
refuse loudly on mismatch. Test: deliberately withhold one leg.

### 8.24 The unscored replay firing — one look, then decide
**Target:** the relation-replay → judgment path (station 6→7).
**Failure mode (suspected):** pairA8's Lang-22-c carries the only non-empty
`relation_replay_fired` of all 120 legs (1915/20000 on patched, "silent on the
trigger literals") with no visible judgment and no outcome effect. If it was
judged-and-dismissed: fine, close. If replay firings can bypass judgment:
that's a hole in the conviction path — spec the fix then, not before.
**CLOSED 2026-08-04: judged, not bypassed.** Full chain recorded: fired [63] →
judged UNSOUND [68] → gates entered with no standing alarm [69–73]. The
"unscored" appearance came from reading `relation_replay_kept` (a KEEP-list)
as if it were a judgment-list — the absence reflects the dismissal, not a
bypass. Small trap noted in the record's own shape (a field name that reads
as more than it records — record-vs-thing, mild form). The dismissal itself
is CITED (javadoc quote), so it sits in the healthy class; Lang-22-c is
correct, the TN was right. No fix specced. Both pair-raised suspicions
(8.22, 8.24) retired at zero cost.

### 8.21(a) and 8.21(c) are PROMOTED into this batch (from "anytime"):
(a) trace-writer truncation — now charged with one corrupted count, one
corrupted diagnosis, and 5 permanently unrepairable dataset rows;
(c) record code_context into result.jsonl — a ~12M pair could not answer a
pre-registered question for want of this field.

### 8.21 Small hygiene, anytime
(a) Trace-writer truncation: record alarm text in full or mark elision loudly —
ellipsis-truncated records have corrupted one count and one diagnosis
(record-vs-thing #5). (b) The 34 of 230 judge prompts carrying no evidence
block (parked cycle 7, unexplained) — one read decides plumbing hole vs benign.
(c) Record code_context into result.jsonl — its absence made the pair's read 6
(8.2 reach recovery) unresolvable under its own pre-registered denominator and
blocks the throughput check (2026-08-03).

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
