# Code audit 2026-08-04 — keep / delete / judge, file by file

Requested question: go through the code and classify every mechanism — known
helped (keep), known not-helped (delete), never judged (make a judging plan).
Sources: the two-reader census of src/java (every gate, lint, fact, transform
and flag, with wiring verified), the dead-surface inventory (references
grepped), the commit audit + addendum, plan.md's validated/refuted ledgers,
and the run archives. Evidence standard unchanged: a named catch, a named
false-alarm class killed, or a measured A/B — never a feeling.

Headline counts: ~60 live mechanisms censused. KEEP with evidence: ~45.
DELETE with evidence: 10 items (~10.8 MB, ~1,200 lines of live-path code,
3 CLI flags). JUDGE: 7 items, each with its cheapest decisive test.

---

## DELETE — measured not-helped, or dead, with the evidence

Each deletion lands as its own commit; git history preserves everything.
Two deletions carry a REQUIRED validation step, marked ⚠.

1. **Side-quest trees** — `src/db` (10M), `src/linux`, `src/project_zero`,
   `src/oss_variant`, `src/variant.py`. Zero imports from anything live.
   A separate research thread, dormant 2 months. Also `src/oss_fuzz` (344K):
   its only thread to the world is `pyproject.toml` testpaths (8 tests) —
   remove the testpaths entry with it (the 563-vs-555 pytest confusion came
   from exactly this entry).
2. **Old drivers** — `scripts/evaluate.sh`, `scripts/evaluate_crashing.sh`,
   `scripts/run.sh`. Unreferenced; superseded by `run_suite.sh` + cases files.
3. **`src/java/studies/`** (120K) — one-off offline analyses; conclusions
   absorbed into docs long ago; nothing imports it.
4. **Mined oracles** — `harness/test_oracle_miner.py`, `--mined_oracles`,
   `_mined_oracle_block` in prompts.py. Measured neutral-to-negative
   (mined54: a mined flood cost Lang-7's true catch); off by default since.
   The old park condition ("after fresh12") is overtaken: fresh12 is
   indefinitely locked and the mechanism is measured harmful, not merely
   unproven.
5. **Soundness-harden path** — `--rule_soundness_harden`,
   `relation_synth.harden_for_soundness`, `relation_screen._harden_survivor`,
   `harness/canned_probe.py` + its top-level import in relation_screen.
   Pre-fix it destroyed a convictor; post-fix it has ZERO recorded rescues
   across every suite since 07-19 — the audit's own deletion condition
   ("delete after two suites of zero rescues") is met several times over.
   NOTE: the canned_probe import is unconditional at relation_screen.py:43 —
   remove import + usages (all inside `_harden_survivor`) together.
6. ⚠ **5B dismissal-inadmissibility** — the pin-void/citation-void re-ask
   branches and the `5B-INADMISSIBLE` keep in `judge_decision._guarded_verify`
   + `citation_void_decision`, `pinned_environment_note`-as-5B-input,
   `dismissal_invokes_pinned`, `has_drift_kill_signature` (where used only
   here). ZERO firings ever: zero in the 143-row fixture iterations, zero in
   every archived live run including both new pairs — after its matcher bug
   was found and fixed. Reach was structurally capped at 10 of 143 rows.
   KEEP `reask_verdict_usable` and the base re-ask-on-error guard (the
   sentinel fix — load-bearing). ⚠ Validation: one offline replay of
   cases228_untruncated (minus the 8 flagged) with 5B removed must show ZERO
   verdict changes — it never fired, so removal must be verdict-neutral by
   construction; prove it, don't assume it (rule 15).
7. **Model escalation** — `HARNESS_MODEL_PRIMARY/ESCALATION/ESCALATE_AFTER`
   in config.py, their run.py wiring, campaign's stall-based two-tier
   escalation generator. Inert in every measured run (defaults equal → guard
   false); the enabling hypothesis (nano primary) was measured dead in
   cycle 1 ("nano cannot exercise this machinery" — Closure-70 ended
   no_harnesses); 8.1 closed the adjacent model question.
8. **Attribution judge** — `--attribution_judge` + the second-judge path at
   run.py:3559/3926 and the mechanical direction-confirmed attribution that
   exists only for it. Measured vetoing ~100% of sound catches (falsefix13);
   in plan.md's REJECTED list; off since 07-20.
9. **Rate-terminal remnants** — `fire_rate_is_terminal` /
   `carries_terminal_fire_rate_fact` in evidence_facts.py + their tests.
   The 5D rate-based extension was reverted on measurement (traded ~4
   genuine catches for ~0 leaks); these two functions were "kept pure and
   tested, not consulted" — dead code with a maintenance bill. Price-fatal
   class: recorded here so it can return if the price ever changes; the code
   itself goes.
10. **Refactor leftover** — `scripts/refactor_java_package.sh` (one-shot
    migration script, already executed; it is also the last referencer of
    two deleted names).

## KEEP — validated, evidence on file (grouped; pointers in the ledgers)

- **Acceptance gates** (campaign.py): invalid-response gate, self-swallow
  lint, rethrow-without-cause, alarm-ID requirement, gate 0c2 raw-key lint,
  negative-modulo, boolean-swallow, compile, trigger-on-buggy, H3
  setup-fidelity (see JUDGE 6), repair-in-place with back-out rule +
  provenance marker. Each traces to a named lost verdict or dead leg;
  repair measured outcome-neutral/cost-negative with 24% of accepted
  harnesses now flowing through it.
- **The judge door** (judge_decision.py): single adjudicate() entrypoint,
  6B intrinsic-rate drop + family-duty escape, 6C fires-on-both with the
  different-values keep, 5C terminal + marker veto, direction-confirmed
  skip, reask_verdict_usable sentinel. 6B/6C each carry an attributed win
  in final30; the sentinel fix pre-empted 8.1 measuring its own parser.
- **Computed facts** (evidence_facts.py): differential-replay classifier,
  buggy-replay ladder + diverted notes, muted-replay fact + iterate,
  fire-rate fact + five-state RATE_STATES, one-door parity, universal
  screen M-v2 + unconditional rate delivery, trigger-lift note + 8.4
  raw-vs-pinned comparison (live, guarded, closed a cycle-1 residual),
  Math-65 disputed-computation block (symmetric, costless — kept per the
  refresh-2 decision even though delivery is measured non-binding),
  symmetric/latent/escaped/crash-pin notes, 8.3 observed_values recorder.
- **Synthesis + screening core** (relation_synth/screen): the instruction
  blocks (anchor, documented formulas, @throws shape, rejection
  independence, STRUCTURE FROM DATA — the campaign's only two-roll-confirmed
  mechanism catch), all screening lints, P0.2 canary, MIN_CHECKED, direction
  check + INVERTED demotion, R1 compile-repair (+10 relations, zero cost),
  ratio buckets, replay-on-patched (the single biggest recall mechanism).
- **Execution** (fuzz_runner/oracle_mute): P0.1 patch-apply safety net,
  crash/cause signatures, per-oracle crash types, mute/count/diversion
  instrumentation, replay report/muted/iterate with the 3-pass bound.
- **8.2 pure core** (reference_impl.py): not wired by design — held pending
  the reach question (8.21c unblocks it). Explicitly NOT dead code: a
  decision is scheduled on it.
- **Dev tools**: `--rulegen_only`, `--skip_semantic`, aimed-retry campaign.

## JUDGE — never individually validated; cheapest decisive test each

1. **A4 STATE COUPLING block + A6 class-skeleton block** (prompts.py:526,
   1436) — the two heavyweight unvalidated prompt blocks (real token cost
   every prompt). Test (free): grep the four recent 30-leg runs' accepted
   harnesses + judge citations for any use of sibling-state or skeleton
   facts. Zero uses across 120 legs → delete next cycle; any use → promote.
2. **A5 documented-preconditions block** (prompts.py:604) — plausibly
   co-responsible for the Lang-27 first catch (type-suffix contract lives in
   javadoc). Test (free): read the Lang-27-o winning-harness citation; if
   the @throws text appears, promote to validated and stop asking.
3. **A3 boundary instruction + consistency-hint + strategy-menu rotation**
   (one-liners) — judging cost exceeds carrying cost; class them
   "keep, cost-negligible, unattributable" and stop re-litigating, OR fold
   into the A4/A6 grep for free since the same traces are open.
4. **Family-novelty gate** (campaign.py:803) — did its designed job once
   since the extractor fix. Test (free): across the four 30-leg runs, list
   every novelty rejection and whether the leg's eventual convicting check
   came from a post-rejection different-family harness. Zero contribution
   in 120 legs → delete; the rejection-pressure it adds is not free.
5. **Convergence gate + forced width round** (campaign.py:178, Spec N +
   cycle-4a) — armed-zero-fires for the loop; the forced round costs one
   synthesis round EVERY leg. Test (free): from the four runs' traces,
   which round produced each kept/convicting relation; if round-2+ never
   contributes, drop min_extra_rounds to 0 and keep the gate armed for its
   real target (toothless rolls).
6. **H3 setup-fidelity gate** (campaign.py:741) — from the G-fixes; never
   individually measured. Test (free): grep the four runs for H3 rejections;
   read the ≤handful for whether the rejected lift was genuinely infidel.
7. **Focused synthesis** (`--focused_synthesis`) — kill verdict VOID under
   the single-roll ban; best recall evidence on file (foc5 4/4, foc15
   R=0.89). Stays parked, flag-off. Judge only via the pre-registered live
   A/B with BOTH-SIGNS read-outs (catches gained/lost AND accusations
   gained/lost, both guard populations) — the pair showed the lottery cuts
   both ways inside one change-set, so a single-sign read would flatter it.

## Ordering note

The JUDGE tests 1–6 are all free greps over the four archived 30-leg runs —
they should run BEFORE the delete batch lands, in the same pass, because
several deletions (novelty gate, A-blocks) may join the delete list on their
results, and one delete (5B) already carries its own fixture-replay proof.
Everything above is consistent with the standing rules: deletions are
history-preserved, the two risky removals carry rule-15-style proofs, and
nothing verdict-affecting ships unmeasured.
