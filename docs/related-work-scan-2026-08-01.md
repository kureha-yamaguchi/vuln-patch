# Related-work scan: patch-correctness assessment (2026-08-01)

One web pass over the published work on exactly our problem — deciding whether a
patch that passes all tests is a real fix or an overfit — mapped onto our
pipeline stations and standing rules. Purpose: steal what is admissible, record
why the rest is not, and give the cycle-8 items external prior-art anchors.
The field calls our problem **automated patch correctness assessment (APCA)**;
"overfitting patch" is the standard term for what we call an overfit leg.
Survey anchor: *Patch Correctness Assessment: A Survey* (TOSEM 2025).

## Adopt / adapt — admissible under our rules

**PATCH-SIM (ICSE 2018) — behavior change on the PASSING test suite.**
Runs the existing tests on buggy and patched builds and measures how much the
*passing* tests' execution behavior changes: correct patches barely disturb
passing-test behavior; overfit patches disturb it more. Fully
label-independent (buggy build + the project's own tests — authority ranks 1–2
in our hierarchy). We measure this NOWHERE: our pipeline uses the failing test
and generated harnesses, never the passing suite as a behavioral anchor. This
is a genuinely new evidence KIND for the accusation side — exactly the gap the
separating-fact study named. Caveat: later replication (ASE 2020, "how far are
we?") found moderate discriminative power — so it enters as a computed FACT
for the judge, never a gate. → cycle-8 item 8.16 (design note).

**Invalidator (TSE 2023) — likely-invariants (Daikon) as overfit evidence.**
Infers invariants from executions; flags a patch that violates
correct-version invariants or preserves the buggy version's error behavior.
Their correct-version half is FIREWALLED for us (it uses the developer patch).
The admissible half: invariant mining over BUGGY-build executions is a
**deterministic** candidate-rule source — same leg, same candidates, every
roll. That attacks our measured recall-variance driver (the station-2
invention lottery) at its root, something no amount of LLM re-rolling fixes.
Precedent caution: mined54 measured a mined-oracle flood costing a true
catch — mining must feed the existing screen, never bypass it. → cycle-8 item
8.17 (design note).

**Differential Prompting (ASE 2023) — LLM reference implementation as oracle.**
Infers the *intended* functionality, synthesizes a reference implementation,
and differentially tests against it; 75% success finding failure-inducing
inputs on QuixBugs vs 28.8% for direct prompting. Direct external validation
of item 8.2's mechanism class, including its key design choice: the reference
must be generated from intent/documentation, NOT from the code under test
(else it inherits the same bug — their finding and our buggy-validation screen
are the same defense). → cited in 8.2's prior art.

**Poracle (TOSEM 2023) — preservation conditions.** Tests patches under
explicit conditions describing behavior the patch must PRESERVE from the
original program. This is the published form of our authority rank 2 (buggy
build off-defect, family-duty boundary). Nothing new to build; useful
vocabulary and evidence that the approach class holds up. → cited in 8.2.

## Reject — inadmissible under standing rules, with the rule that kills each

- **DiffTGen (ISSTA 2017)** — generates tests exposing behavioral differences
  from the FIXED version. Uses the developer fix as oracle → firewall. We
  already do the admissible offline version (dev-fix certification).
- **Learned classifiers (ODS, APPT, Attention-dataset, ComPass, BATS,
  LLM4PatchCorrectness / LLM-based APCA)** — supervised or few-shot on
  labeled benchmark patches; the current LLM-based APCA state of the art is
  this shape. Trains on the benchmark's bug/patch distribution → the
  no-dataset-overfitting rule kills the whole family. Also: our fresh-bug
  arm C measured what benchmark-fitted judging does out of distribution
  (0.29).
- **Test-based patch clustering (EMSE 2024)** — groups a bug's candidate
  patches by behavior and trusts the majority cluster → our rejected
  voting-across-patches idea (repair tools make the same mistake in all
  their patches; agreement proves nothing).
- **Opad / Fix2Fit (fuzzing with crash & memory-safety oracles)** — implicit
  oracles only; that IS our crashing-bug path. Nothing for semantic legs,
  where the oracle problem is the whole game.

## What the scan says about our position

The published field splits into: ground-truth-dependent (firewalled for us),
benchmark-learned (rule-banned for us), implicit-oracle (our crashing path),
and behavior/invariant-based label-independent signals. Our pipeline already
sits at the frontier of the last class (synthesized relations + screening +
replay is more machinery than any published APCA system runs), but two
label-independent signals from the literature are genuinely uncollected here:
passing-suite behavior deltas (PATCH-SIM) and deterministically mined
invariants (Invalidator's admissible half). Both enter cycle 8 as design
notes, both as facts-for-the-judge, never gates.

## Sources

- Patch Correctness Assessment: A Survey — https://dl.acm.org/doi/10.1145/3702972
- PATCH-SIM: Identifying Patch Correctness in Test-Based Program Repair — https://xiongyingfei.github.io/papers/ICSE18a.pdf
- Automated patch correctness assessment: how far are we? — https://dl.acm.org/doi/10.1145/3324884.3416590
- Invalidator (invariants + syntax) — https://arxiv.org/abs/2301.01113
- Differential Prompting (LLM reference implementation) — https://arxiv.org/abs/2304.11686
- Poracle: preservation conditions — https://dl.acm.org/doi/10.1145/3625293
- DiffTGen — https://www.researchgate.net/publication/318375339
- Opad/Fix2Fit context (patch/input-space fuzzing) — https://arxiv.org/pdf/2308.00666
- Test-based patch clustering — https://pmc.ncbi.nlm.nih.gov/articles/PMC11269383/
