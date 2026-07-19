# Commit audit — all 152 commits reviewed (2026-07-19)

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
