# Semantic-bug detection — the plan

Restructured 2026-07-18: ground rules first, finished work as a list by
pipeline station, current scoreboard, then remaining work by station
ordered by impact-vs-risk, rejected ideas at the bottom. The full
pre-restructure text (with the long per-phase case histories) is
preserved verbatim at the end of `semantic-recall-history.md`.
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

## CURRENT STATE (2026-07-31)

- **Score:** paired mean F1 0.685 (final30A/B, identical commits) vs 0.49 July
  baseline. Recall 8/14 stable both rolls. Repair-in-place: outcome-neutral,
  cost-negative (−2.3 attempts/leg).
- **Precision ceiling ADOPTED: ~5 accusations on the trap set** under the current
  architecture — over measured refutations: no recorded feature separates kept catches
  from kept false accusations; the deciding fact is present in 5 of 7 cases but
  case-specific (no general gate); adjacent verbatim delivery is ignored; the narrow
  quote-forced question voids 22% of guards. Remaining directions are architectural
  (cycle-8 items 8.1–8.3).
- **Residuals (chronic):** Closure-62, Math-30, Math-65 — one shape: accusations no
  delivered fact dislodges. **Watch:** Math-39 (2-for-2, repair ruled out, event read
  = 8.6). **Hard column:** Closure-38, Math-104. **Coin-flips:** Lang-60, Math-68,
  Math-73, Closure-92. **Multi-mode:** Lang-63 (all three failure modes on record).
- **Honestly open:** Chart-19's missed-twice→caught-twice flip in the pricing pair —
  not repair (attempt-tag grep), not the gate correction (old detector passes both
  firing harnesses); remaining candidates composition/variance.

## CURRENT MEASUREMENT PROTOCOL (the next 30-leg pair)

Runs only after the cycle-8 batch lands, on the user's word. Width 5 / -m 12 (pending
8.5), both rolls same commit, zero changes between, per-leg reading, tripwire: any new
accuser greped for the harness-repair marker before attribution.
**PASS = paired mean > 0.685 AND ≤5 accusations per roll AND zero accusations on
historically clean legs.** The former sub-5 "strong" tier is RETIRED — see the ceiling
evidence above and in plan-history.md.

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

### 8.1 Judge-model swap experiment (gpt-5) — FIRST; needs user go-phrase
**Target:** the judge LLM behind `relation_verifier.verify()` (and `family_duty`).
**Failure mode:** the ~5-accusation ceiling — specifically the 5 evidence-present-but-
ignored accusations and the 22% wrong-void behavior. Question: model quirk or
task-architecture?
**Steps:**
1. Pre-launch: confirm the gpt-5 deployment exists and answers (one probe call);
   verify `reask_verdict_usable()` recognizes THIS deployment's error/sentinel strings
   (a silently fail-open judge is the July-15 bug; add its error format to the
   sentinel list if absent). Commit this check before spending.
2. Part A: `verifier_replay.py --cases tests/fixtures/cases228.jsonl` (plus the 67-row
   guard fixture) with the judge model set to gpt-5, votes=1, repeats=1. Same prompts,
   zero prompt edits.
3. Part B: the frozen narrow contradiction question (exact phrasing from the failed
   engagement experiment — do NOT reword) over the same 24 accusing checks + 67 guards,
   model gpt-5.
4. Commit raw outputs BEFORE scoring (both parts).
**Read-out (pre-registered):** per-case flips vs the incumbent's recorded verdicts, not
totals — gpt-5 is expected WORSE overall; that is not the signal. Decision table:
same failure shape (ignores Math-65's adjacent source; plausible-irrelevant quotes;
guard voids) → ceiling is architectural; record and close the model question. Different
shape (engages on any of the 5 ignored-evidence cases) → model-dependence shown; opens
(a) re-test on any future stronger model, (b) cross-model agreement as a NEW candidate
(different from same-model voting, which is dead: same model = same blind spots ×3).
**Cost:** ~1.5–2.5M, API-only, no VM. One shot per part, no iteration.

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
rides the next live run passively — no dedicated run.

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

### 8.5 Relation-budget experiment (-m 12 vs -m 16) — cheap, anytime (~1M)
**Target:** the `-m` relation-synthesis cap in suite COMMON flags.
**Failure mode:** Chart-19 caught twice at -m 16, missed twice at -m 12; its winning
family was proposed-then-died (2d), so the budget may starve it at standard config.
**Steps:** width5 catch-leg suite (5 legs), one roll each at -m 12 and -m 16, same
commit; read INVENTION RATES of known winning families per leg from traces (not pooled
scores). If -m 16 materially raises invention, adopt it as standard and record the
config change; if not, Chart-19's fragility attributes to variance/composition and
stays open.

### 8.6 Math-39 event-chain read — free, BEFORE any next measurement
**Target:** archived pricing-pair traces, both Math-39 accusing verdicts' full evidence
chains (facts delivered, gates consulted, judge WHY/CITATION).
**Failure mode:** an unexplained NEW repeat accuser (2-for-2 after clean history;
repair ruled out by attempt-tag grep). Outcome: either a named mechanism (joins the
chronic list / licenses a fix) or documented judge-lottery (watch list entry closed).

### 8.7 Marker-field fix — small, ships with next build
**Target:** `campaign.py` acceptance bookkeeping + the `harness-repair` trace event.
**Failure mode:** repair attribution needed attempt-tag archaeology this week; record
per accepted harness whether it came from a repaired attempt (and which repairs), so
attribution is one field lookup. Include repaired-source reconstructability note (the
transform is deterministic over the recorded pre-repair output).

### 8.8 Suite-file label check — ten lines, ships with next build
**Target:** `run_suite.sh` case-file loading.
**Failure mode:** the -c-on-fakes typo class (happened once; firewall held, full
rescore required). Assert each case's label matches `suites/pinned_tasks.jsonl`;
refuse to launch on mismatch.

### 8.9 Family-persistence design note — paper only, LAST of design items
**Target:** `relation_synth.py` round structure + harness generation loop in
`campaign.py`.
**Failure mode:** the invention lottery — check families proposed but landing in
`relations_not_implemented` (Lang-63's winning family in one roll), absent next roll.
**Must price:** slot competition (persisted families crowd out new ones) and the
interaction with the novelty gate (which steers AWAY from covered families — these two
must not fight). Run-local only; nothing crosses runs.

### 8.10 Pair pre-commitment upkeep — free, continuous
**Target:** this plan's measurement protocol section.
Keep current: re-scoped bar (PASS = paired mean > 0.685 AND ≤5 accusations per roll AND
zero accusations on historically clean legs; the old <5 strong tier retired with a
pointer to the ceiling evidence), residuals (Closure-62, Math-30, Math-65 + Math-39
pending 8.6), Chart-19 recorded honestly open (two causes eliminated: not repair, not
the gate correction).

### Sequencing and gates
Today: 8.1 (on user's phrase: "Run the judge-model swap experiment with gpt-5, parts A
and B as pre-registered") + 8.6 + 8.7 + 8.8. This week: 8.2 and 8.9 design docs; 8.4;
8.5 when convenient. 8.3 after 8.1/8.2 outcomes. Then: assemble the cycle-8 batch →
smoke on final build → the full 30-leg pair (~7M, user's word) against the 8.10 bar →
fresh12 decision (user's literal phrase only).
