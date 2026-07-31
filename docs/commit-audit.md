# Commit audit — all 152 commits reviewed (2026-07-19)

> **Scope note (2026-07-26):** covers history through 2026-07-19 only. The cycle-1→4
> campaign that followed is documented in `docs/cycles/`; a follow-up audit of that
> period lives in the plan doc's 2026-07-26 candidate ledger (`docs/plan.md`).
>
> **Scope note (2026-07-31):** the cycle era (2026-07-24 → 07-31) is now audited in
> the addendum at the bottom of this file, same question and same verdict standard.

Requested question: looking over everything committed so far, what is
validated progress, what was never individually validated and should be
checked later, and what is dead weight worth deleting. Sources: the full
git log, the plan doc's DONE list, and the run archives under
`runs-archive/` (the evidence for "validated" is a named catch, a named
false-alarm class killed, or a measured A/B — never a feeling).

## The story in five phases

1. **Bootstrap (Apr 30 – May)** — oss-fuzz-gen experiments, then a
   from-scratch LLM harness generator wired to Defects4J + the drr patch
   dataset. Foundation; all still in use (analysis/build/campaign/llm).
2. **Side quests (Jun 2–5)** — the Project Zero variant-pair harvester
   (`src/project_zero`, `src/db`), a Linux-kernel pipeline (`src/linux`),
   OSS-Fuzz variant driver (`oss-variant`, Jul 8). A different research
   thread entirely; dormant since June. ~10 MB of repo weight.
3. **Crashing-bug era (Jun 9 – Jul 9)** — compile+crash harness loop,
   metamorphic prompt block, FP reduction, Azure/gpt-5.4 switch, first
   semantic-bug work, reachability fixes.
4. **The overhaul (Jul 14–17)** — the current pipeline: token
   accounting, relation synthesis + buggy-screen + verifier, per-check
   bookkeeping, dataset audit + pinned tasks, P0–P3 mechanisms,
   p23gate negative result + remediation. Ended at **10/16 recall,
   precision 1.00** on dev — the best measured state.
5. **The rules detour (Jul 18–19)** — R4 menu built then demoted by its
   own coverage test; rulegen cheap loop (R1 kept, four exhortation arms
   reverted); trace.md observability (big win); soundness-harden (shipped
   broken, fixed 07-19); the wrong "rules are useless" ablation
   (retracted 07-19). Net: one kept mechanism, one kept tool, two
   corrected mistakes, no scoreboard movement.

## Validated — keep, evidence on file

Each line: mechanism → the evidence that it earns its place.

- **Patch applier hardening + trigger safety net (P0.1)** → Lang-50's
  half-applied patch and Math-2's never-applied patch, both silently
  scored for weeks before this.
- **Self-swallow lint + forced-alarm canary (P0.2)** → half of all
  Lang-7-era checks swallowed their own alarms.
- **Cause-chain + differential replay (P0.3/A2)** → killed the Math-2-c
  junk-input FP class and mechanically drops Lang-27-c's generic leak.
- **Per-check bookkeeping + latent flag (P0.4)** → enabler for the
  latent/symmetric judge facts; Chart-26 FP + Lang-60 catch both hinge
  on it.
- **Direction grounding, BUGGY labeling (P2.1)** → Lang-7's rules came
  out backwards for the project's whole prior history.
- **Screening direction-check + INVERTED demotion (P2.2)** → the hard
  drop once deleted Math-2's mean-formula; demotion preserved it.
- **Relation replay on patched (P3.2)** → the biggest recall mechanism:
  kept convictions on 5/8 full30 catches; Math-2-o is caught ONLY via
  replay. (Re-validated 07-19 after the false retraction.)
- **Crash-type pinning (P3.3)** → Lang-41 kept while a different-crash
  impostor is dismissed.
- **Judge facts (symmetric-firing, latent-replay, trigger-test-lift,
  escaped-exception, scoped dismissal-wins, trust hierarchy)** → each
  one killed a named FP class or revived a named true catch
  (Chart-26-c, Lang-60-o, Closure-62/73-c, Math-2-c, Closure-62-o).
- **Whitespace-normalized text lifts** → surfaced Closure-92-o's real
  content difference that raw comparison buried.
- **Extreme-magnitude fence + magnitude-scaled tolerances** → three
  Math-2-c false accusations lived at billion-scale.
- **Negative-modulo lint, dynamic oracle IDs, alarm-ID requirement,
  phantom-crash fix, UTF-8 javac, JSON double-escape recovery** → each
  traced to a specific lost verdict or dead leg.
- **R1 rule compile-repair** → measured: +10 relations across 4 legs,
  flipped Closure-33 in the rulegen loop, zero false-fire cost.
- **Metamorphic prompt block (June, ee8d78b)** → prompt advice, but the
  Lang-27-o first-ever catch (07-19) came exactly from harness-invented
  metamorphic type-contract checks. Promoted to validated.
- **One-file trace.md (07-19)** → paid for itself same-day: the harden
  sabotage, the missing replay flag, and the wrong ablation were all
  found by reading traces.

## Unvalidated — worth a cheap check later

Ordered by how cheap the check is. None of these is known-harmful; each
is a prompt/context addition whose individual effect was never isolated
(the A1–A7 batch shipped in one evening and was validated as a batch at
best).

- **A5 documented-preconditions block** — plausibly co-responsible for
  the Lang-27 catch (the type-suffix contract lives in javadoc). Check:
  read the Lang-27-o trace prompt; if the @throws/@param text appears in
  the winning harness's cited justification, promote to validated.
- **A4 field-coupling context (FieldSibling)** — unit-checked only.
  Check: grep archived traces for the STATE COUPLING block being USED by
  any accepted harness check; if zero uses across dev runs, delete.
- **A3 boundary-probing instruction** — prompt-only; the meta-rule says
  instructions are the weak channel, and BND is the mechanical version
  of the same idea. Check: same trace-grep; delete once BND ships.
- **A6 class skeleton in the consistency slot** — motivated by the
  context study; never isolated. Cheap ablation on 2 doc-rich pairs.
- **A1 skeleton-absence verifier guidance** — had a named replay
  criterion (chart19_o must flip to KEPT); confirm the replay was run
  and archived; if not, run verifier_replay once.
- **Mechanism rotation (consistency/pairs/relations slots)** —
  structural, plausible, unmeasured. Leave until stable, then one
  rotation-off ablation.
- **--rule_soundness_harden (canned probe)** — fixed 07-19 but has
  never demonstrably rescued a rule, and pre-fix it destroyed one
  convictor. Stays opt-in-off; DELETE after the next two suites if it
  still has zero rescues on record. `canned_probe.py` goes with it.
- **Model escalation (nano primary → flagship)** — inactive in every
  measured run (all suites force --model gpt-5.4). Either run one suite
  with escalation on to price the saving, or delete the machinery.

## Deletion candidates — dead now

- ~~`special_corpus` import~~ — REMOVED 07-19: `relation_screen`
  imported a module that was never committed (existed only as an
  uncommitted leftover on the VM from the reverted rulegen_special arm).
  A fresh clone would not even import.
- **`variation_menu.py` (177 lines) + `variation_menu.json` +
  `input_kind.py` (225 lines)** — INERT: zero references from run.py,
  relation_synth, or prompts. Built 07-18 morning, demoted the same
  day by the coverage test (menu covers ~18% of free invention). Keep
  `suites/menu-candidates.md` as the literature reference; delete the
  code (git history preserves it). The one argued-for residual use
  (1–2 domain relations for security/reflection legs) can be
  reintroduced from history if a held-out-adjacent need ever appears.
- **`suites/rulegen_B.cases` and `suites/rulegen_P.cases`** — reference
  flags (`--synth_diverse`, `--pin_trigger_inputs`) that were REMOVED
  in the f2ea4a8 revert; running either today dies at argparse. Delete
  (history keeps the record of those arms).
- **`test_oracle_miner.py` + the --mined_oracles path** — measured
  neutral-to-negative (mined54: cracked no miss, a mined flood cost
  Lang-7's TP); off by default since. Delete after held-out (keeping it
  off costs nothing until then, and the held-out freeze should not be
  preceded by a churn commit).
- **`context_study.py`** — one-shot study tool (07-15), conclusions
  absorbed into A6. Attic/delete.
- **`evaluate.sh`, `evaluate_crashing.sh`, `run.sh`** — crash-era
  drivers superseded by `run_suite.sh` + cases files (last real use
  June/early-July). Confirm nothing scripts against them, then delete.
- **Side quests: `src/db` (9.9 MB), `src/linux`, `src/project_zero`,
  `src/oss_fuzz`, `variant.py`, `oss-variant/`** — a separate research
  thread, dormant 6+ weeks. Recommendation: move to their own repo (or
  a `sidequests/` branch) rather than delete — they are real datasets/
  tools, just not this pipeline. They also make every repo-wide grep
  noisier than it needs to be.
- **`relation_pool.py`** — already deleted 07-19 with the no-pooling
  rule.

## Process observations from the full log

- Every validated mechanism above is MECHANICAL (computed fact, gate,
  replay). Every reverted or demoted item (R2/R4 exhortations, mined
  mass, pin-trigger, derive) was ADVICE injected into a prompt. The
  meta-rule has held for the entire project history without exception.
- The three costliest wrong turns (p23gate bundling, the R4 menu day,
  the pooled-ablation retraction) share one root: a measurement whose
  setup contradicted an already-recorded rule (one change per point;
  free invention beats menus; rule 8's launch check). The rules were
  right each time; the failure was not consulting them.
- 21 of 152 commits are docs/plan restructures. That ratio is healthy —
  the plan doc is why the retraction was provable months of context
  later.

---

# Addendum — the cycle era audited (2026-07-24 → 07-31, 156 commits; written 2026-07-31)

Same question as above: what is validated progress, what was never individually
validated, and what is dead weight. Evidence standard unchanged: a named catch, a
named false-alarm class killed, or a measured A/B — never a feeling. Sources: the
git log, `docs/cycles/` (four cycle files + three retros), `docs/plan-history.md`
PART 2, `docs/replay/` (raw-committed-before-scoring measurement docs), and the
archives `night20*`, `final30A/B`, `repairA/B` plus the smokes.

The 07-20→23 interlude (focused synthesis foc2/foc5/foc15, the attribution-judge
gating, the 3-arm fresh-15 experiment) is documented in
`docs/abc-analysis-unified.md` and plan-history PART 1 and is not re-audited here,
with one exception carried into the loose-ends list: the focused-synthesis kill,
whose verdict is void under the rules adopted since.

## The story in seven phases

1. **Cycles 1–2 (07-24→25)** — evidence facts extracted into `evidence_facts.py`;
   replay honesty (crashed/clean/error, SHADOWED verdicts, no manufactured
   evidence); muted per-check replay; fire-rate facts; "identical" must be earned
   by a value comparison. Two first-ever clean verdicts on chronic false-alarm
   legs (Closure-70, Math-30), one self-caused regression found and fixed
   (Math-68).
2. **Breadth + retros (07-25)** — the pool30 sweep and the paired pool run:
   catches swung ±4 legs between identical rolls (paired mean F1 0.49).
   Single-roll comparisons banned; both mechanical dismissals defused to facts
   after each killed a genuine catch.
3. **Cycles 3–4 (07-25)** — one-door fact parity validated; universal screening
   v1 measured inert; judge majority voting shipped and reverted the same day;
   harness width standardized at -n 5 with its precision cost unmeasured.
4. **Inventory + cycle 5 (07-26→28)** — the 228-verdict population inventory
   ("the FN mechanism is the judge, almost always": 22 of 23 missed-overfit legs
   with judged firings had every verdict UNSOUND); the fixture-replay loop that
   found two live text-matching bugs (terminal-marker veto, negated citations)
   and rejected its own rate-based extension on measurement.
5. **Cycle 6 + night20b/c (07-28→29)** — structure-from-data directive; 6B/6C
   enforcement gates shipped only after the fixture was made faithful (the
   empty-failing-test defect had invalidated the first gate run and every earlier
   replay measurement); the diverted-replay bug that fabricated evidence against
   a correct patch fixed.
6. **The milestone (07-29)** — final30 pair, identical code both rolls:
   F1 0.64 / 0.73, mean **0.685** vs the 0.49 pre-campaign reference. Precision
   flat at exactly 5 false accusations in both rolls. 8 of 30 legs flipped
   between identical runs (27%).
7. **Cycle 7 + the ceiling (07-29→30)** — free forensics killed three planned
   builds before any spend; repair-in-place shipped (outcome-neutral,
   cost-negative); every remaining precision lever measured dead one at a time;
   the ~5-accusation ceiling adopted over four measured refutations. Cycle 8
   planned 07-31 — design and docs only, no measurements yet.

## Did it help — the scoreboard answer

Yes on recall, no on precision, and the campaign's own instruments say so
plainly. The paired mean went 0.49 → 0.685 (final30A TP=9 FN=5 FP=5 TN=11,
final30B TP=11 FN=3 FP=5 TN=11). Every point of that came from catching more
overfits (stable core 8/14, with Closure-92 newly steady); the false-accusation
count was exactly 5 in both rolls, unmoved by cycles 5 and 6, which were aimed
at it. The precision work still earned its keep — it killed two
fabricated-evidence bugs, defused three catch-killing mechanisms, and produced
the guard set — but its scoreboard effect was zero, and that fact is now
formalized as the adopted ceiling rather than argued around.

## Validated — helped, evidence on file

- **Muted per-check replay (Spec G)** → Closure-70-c and Math-30-c first-ever
  TNs, both mechanism-attributed in trace (crash pre-exists behind the
  harness's own alarms; check fires on buggy once the shadowing check is
  silenced).
- **Identical-earned-by-values (Spec I)** → fixed the Math-68 FN regression
  cycle 2 itself caused; Math-68 2/2 in both confirming rolls.
- **One-door fact parity (Spec K)** → Math-73-c TN, proven by a within-leg
  controlled comparison (same behaviour judged twice; only the track with the
  facts dismissed correctly).
- **Replay-status honesty (Specs B/C)** → SHADOWED notes verbatim on every
  shadowed replay; the "already establishes" extrapolation gone from live
  traces.
- **Defusing mechanical dismissals to facts** → the identical-drop fired once
  in 30 legs and killed a genuine catch (zero precision wins); the family-duty
  rungs killed Math-68 + Chart-19 in poolA. Both defused; both catches
  recovered. Origin of the kill-switch and guard-population rules.
- **Terminal-marker veto + negated-citation fixes** → live production
  catch-killers found by fixture replay: a bare `'on both builds'` substring
  fired the terminal gate on notes DENYING the identical claim; `'document'`
  matched inside "undocumented".
- **Fixture failing-test backfill** → 0 of 141 rows carried a failing test
  (family-duty could never answer YES), invalidating gate 1 and every earlier
  replay measurement; now 228/228, pinned by test.
- **6B intrinsic-rate drop with family-duty escape** → gate 2 (faithful): 2
  correct drops, 1 catch killed; live in final30 it dropped accusations on 3
  legs (A) / 5 legs (B); Chart-26's roll-B clean verdict is the one attributed
  precision win; Math-30's chronic drop now happens in code rather than by
  persuasion.
- **6C fires-on-both keep (values differ)** → rescued Lang-63 in final30 roll
  B (the one attributed recall win); the Math-73 flip analysis showed both
  gates behaving correctly with the flip one station upstream.
- **Diverted-replay false-fact fix** → the pipeline had delivered "the buggy
  build runs this exact input WITHOUT firing" against a correct patch when the
  buggy build actually threw and was swallowed. Fabricated-evidence class;
  gone from both final rolls.
- **Rate-based-5C revert** → the reverted extension traded ~4 genuine catches
  for ~0 leaks; the revert is load-bearing for Chart-19's night20b catch
  (fire-rate 100% would have auto-dropped it) and paid for one Math-73-c FP.
  Net strongly positive and honestly priced.
- **Structure-from-data directive** → Chart-19: the first catch of the
  campaign to clear the two-roll bar with a named mechanism (fuzz-controlled
  sparse index vs the constant dense receiver that missed everything).
  Lang-63's credit was demoted to coin-flip by roll two — the demotion is the
  audit trail working.
- **Repair-in-place + boolean-swallow repair** → offline: 101 of 235 archived
  rejections fully cleared, 0 detector regressions, 0 compile regressions over
  111 pairs; live: outcome-neutral (ONLY-REPAIRED false on every firing leg,
  both rolls), cost-negative (attempts-to-target 14.8 → 12.5, shared credit
  with the alarm-ID gate correction). The compile gate caught four defect
  classes detectors structurally could not — now a standing rule.
- **Observability as a mechanism** → every cycle-6 decision emits a recorded
  event; smoke-before-pair made permanent after one ~500k smoke caught an
  inert mechanism, an unreachable claim, a failed hypothesis, and a live
  repair bug before a ~10M measurement; "grep the recorded event, never the
  print" adopted after two false claims from print-greps.
- **Cycle-7 free forensics** → the citation asymmetry measured (accusations
  90% uncited, dismissals 6%); the citation-requirement remedy rejected on its
  own numbers (saves 23 wrong accusations, costs 60 correct catches); the
  Closure-62 dismissal rule found to be dead code (13 notes, all
  undetermined); Math-30's firing inputs identified as the int-overflow
  boundary (46341², not built at n=2).
- **Three builds killed before spend** (item 3: zero byte-identical judge
  prompts in 1,616 archived calls; item 5: silent legs are 14 correct / 2
  fake, and a correct patch that raises any alarm is wrongly accused 56% of
  the time; item 6: 7:1 against on legs, price-fatal). The two-kinds-of-death
  taxonomy comes from here.
- **The ceiling adoption itself** → separating-fact study (no recorded fact
  separates 67 kept genuine catches from 23 kept false accusations),
  ground-truth backtrack (deciding fact case-specific; ceiling NOT imposed by
  the firewall), Math-65 adjacency test (fact delivered 4×, ignored), and the
  one-shot engagement experiment (2 of ≥7 required hits; 10 wrong guard voids
  vs ≤3 allowed; voided 5 non-contradiction cases). Four independent
  refutations, all pre-committed, all committed raw before scoring.
- **Paired measurement + pre-commitment protocol** → the 0.685 number exists
  because both rolls ran the same commit under a bar written before launch;
  the 27% flip rate is itself a first-class finding that reshaped every
  later read-out.

## Reverted or rejected for cause — correct kills, evidence on file

Premise-false (do not come back): judge majority voting (identical over-kill
and leak at 3× cost, dead in both regimes); item-3 verdict memoization; item-5
silent-case retry; the Closure-62 dismissal-rule route (extraction was never
the binding constraint — the defect IS a whitespace defect, so a comparison
loose enough to match would dismiss a whitespace alarm by ignoring
whitespace); the Math-65 placement hypothesis (delivered 4×, ignored).

Price-fatal (may return if the price changes, recorded as such): rate-based 5C
terminal (~4 catches for ~0 leaks); item-6 seed shapes (7:1 against on legs,
but seeds do address one of Lang-63's three modes); fix (ii) non-numeric
comparison (2-correct/1-wrong wash — deferred with both measurements
recorded); the citation requirement (60-for-23).

Mechanism-inert, replaced or absorbed: universal screening v1 (extraction
compile-hostile, near-zero coverage; M-v2 via the muting infrastructure is the
live successor); 5B inadmissibility (zero firings ever recorded, reach
structurally capped at 10 of 143 rows).

## Inert or unmeasured — carried forward, cheap checks named

- **Structure-constant lint** — 7 demotions in its first live run, 4 on
  non-container receivers, 0 on Chart-19 (the class it was written for).
  Decorative on current evidence. Keep (fail-soft), do not credit, narrow the
  detector or delete after two more suites of zero on-target demotions.
- **N-1 convergence gate** — armed, zero fires (legs all had non-seed power).
  Its target class (pool30's toothless Math-74 roll) is real; leave armed.
- **N-2 receiver-state directive** — never isolated; absorbed into the
  structure-from-data family. No separate check needed.
- **Spec D lift value gate** — the guarded path has never executed (oracle
  names never matched). Landmine-or-dead: one archived-trace grep decides.
- **Family-novelty gate** — after the vacuous-rejection fix it has done its
  designed job once. Unproven benefit; its rejection pressure is priced into
  the 2d rejection-reason table. Watch, don't invest.
- **Width -n 5 standard** — still in tension with the recorded n=3
  zero-false-alarm measurement; the precision cost of n=5 has never been
  isolated. Flagged 07-26, still true. Any future width change should measure
  this first.
- **Focused per-source synthesis (the 07-20 era kill)** — verdict void under
  current rules: killed by a ONE-roll-per-arm A/B (F1 0.91 vs 0.77) before
  the single-roll ban, while holding the best recall evidence in the campaign
  (foc5 4/4 by-pass targets; foc15 R=0.89; foc2 made Math-2's mean-formula
  relation appear in every roll). Re-adjudication is required and is NOT in
  the cycle-8 plan. This is the largest unresolved recall lever on the books.
- **`not-applicable` bucket split** — final30's own top-priority follow-up
  (the trace cannot distinguish "never measured" from "measured and healthy",
  which is what sank the rule-diversity claim) has no cycle-8 item. Cheap,
  observability-only, prerequisite for any future recall-bottleneck claim.
- **Model-escalation config** — still dormant in every measured run; now
  correctly referenced by 8.1 as a wiring point, not built speculatively.
- **34 of 230 judge prompts carry no `<evidence>` block** — parked in cycle 7,
  still unexplained.

## Process observations from the cycle era

- **24 claims were corrected or retracted in eight days**, and every single
  correction was found mechanically — a trace grep, an attempt-tag grep, a
  compile gate, a fixture pin — never by re-arguing. The observability
  investment (trace.md, recorded events, raw-before-scoring) is what made a
  campaign this fast auditable at all.
- **One defect class recurs more than any other: a rule keyed on text read in
  the opposite sense** (one-door delivery, terminal markers, negated
  citations, "no rate found", print-vs-event). At least five instances, each
  with its own standing rule. The class survives rule-making because the
  rules are themselves textual; the durable fix each time was a typed,
  recorded event (8.7 continues this).
- **The 07-19 meta-rule held and gained a sharper edge.** Everything validated
  above is a computed fact placed in evidence or a deterministic gate with a
  narrow escape; everything reverted was either prompt advice or a mechanical
  auto-dismissal. The cycle era's addition: a mechanical DROP is as dangerous
  as an instruction — all three tried (identical-drop, family-duty rungs,
  rate-terminal) each killed at least one genuine catch, and the only shape
  that shipped safely (6B) went out behind a guard-population measurement and
  an escape hatch.
- **Negative results were bought at the right price.** Items 3, 5, 6 died on
  free archive greps; four designs died on the guard population before any
  token; the engagement experiment spent ~1M once, pre-committed, and closed
  an entire hypothesis family. Compare: the p23gate era bought its negative
  results with full suites and a day of forensics.
- **The one measurement-hygiene failure mode that still slipped through** was
  suite-file labels (`-c` on the four fake repair legs — full rescore
  required, firewall held). 8.8's ten-line assert is the correct-shaped fix
  and should ship with the next build.
