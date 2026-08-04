# What we tried, and what worked — the semantic-bug era

**Covers:** everything after Kureha's last commit (`6da5d92`, 2026-06-09) through
2026-08-03. That is 400 commits.

**Who this is for:** someone with a computer-science background who has never seen
this project. No prior knowledge of the codebase, the dataset, or the vocabulary is
assumed. Terms are defined where they first appear.

**How to read it:** Parts 1–3 are the setup — the problem, the machine, and the
starting line. Part 4 is the score timeline in one table. Part 5 is the bulk: every
change we made, grouped by era, each one written in the same shape —

> *what part of the machine it targets · what that part is for · what went wrong ·
> what we expected the change to buy · what it actually bought.*

Parts 6–8 are the summary tables, the methodology lessons, and the current state.

This file is a retrospective. It is not a spec and it is not authoritative for the
current design — `docs/plan.md` is. Where this file and `plan.md` disagree, `plan.md`
wins.

---

# Part 1 — The big picture

## The problem

Automated program repair (APR) tools take a buggy program plus a failing test, and
produce a **patch** — a code change meant to fix the bug. Sometimes the patch is a
real fix. Sometimes it is an **overfit patch**: a change that makes the one failing
test pass without actually fixing the underlying behavior. It papers over the symptom
the test happens to look at.

Our job: given a bug and a candidate patch, decide **real fix or overfit** — without
ever looking at the developer's actual fix.

## Why this is hard

An overfit patch passes every test that exists. That is the definition. So it can only
be caught on **inputs no test covers** — and out there, nobody has written down what
"correct" means. The whole difficulty is reconstructing the intended behavior from
indirect evidence.

The project ranks that evidence by how much it can be trusted:

| rank | source of truth | why it is trusted, and how far |
|---|---|---|
| 1 | the bug's original failing test | definitive — but only for the exact inputs it uses |
| 2 | the buggy program itself | it is correct *everywhere except at the bug* |
| 3 | the documentation comments | the promised contract, often vague |
| 4 | universal math/logic rules | "sorting twice = sorting once", if genuinely universal |
| 5 | the patch's own code | **least trusted — it might be the overfit** |

Rank 5 is the trap the whole project is organized around. If you ask a language model
"is this patch correct?" and let it read the patch, it will read the patch's logic as
the specification and declare it correct. Every design rule in this codebase exists to
keep the patch from being its own judge.

## What the machine actually builds

For each (bug, candidate patch) pair the pipeline writes small fuzzing programs called
**harnesses**. A harness feeds thousands of semi-random inputs into the patched code
and contains **checks** — assertions about what must be true. Examples of checks:

- "this call must not throw"
- "the mean of this sample must equal n·m/N"
- "asking for the index of a null axis must be rejected regardless of what else is
  installed on the plot"

A check that fires (fails) on the patched program is an **accusation**: evidence the
patch is an overfit.

Checks must be two things at once, and these pull in opposite directions:

- **safe** — never fire on a genuinely correct patch;
- **sharp** — actually fire on the overfit.

Two failure modes follow directly:

- a **miss** (also: false negative, FN) — an overfit we failed to catch. It means we
  never checked something the overfit gets wrong.
- a **false accusation** (also: false positive, FP) — a correct patch we flagged. It
  means we checked something a correct program is allowed to do differently.

False accusations are the worse of the two: telling a developer their correct fix is
broken destroys trust in the tool faster than missing a bad patch does.

## Vocabulary you will need

- **leg** — one candidate patch being judged from start to finish. A bug normally
  contributes two legs to an experiment: its correct patch and its overfit patch. Legs
  are named like `Math-65-c` (correct) and `Math-2-o` (overfit).
- **roll** — one execution of a leg. Harness generation involves a language model, so
  two rolls of the same leg on the same code can produce different answers. This turns
  out to matter enormously.
- **firing** — a check failing during a run. A firing on the buggy program is expected
  and good; a firing on the patched program is an accusation.
- **the judge** — a language model that reviews each accusation and rules it genuine
  or spurious.
- **buggy build / patched build** — the program before the patch, and after it.
- **tripwire** — a check that stays completely silent on the buggy program. It detects
  nothing about the known bug, but it will catch damage the patch newly introduces.
- **residual** — a wrong answer we have measured, explained, and decided to live with
  for now, as opposed to one still under investigation.
- **the firewall** — a hard project rule: the developer's real fix may be used only
  *offline*, for cleaning the dataset and understanding misses after the fact. It may
  never enter a decision the pipeline makes. Several otherwise-good ideas were killed
  purely because they leaked across this line.

## How experiments are run, and what they are called

Four words for four sizes of experiment. They recur constantly in Part 5.

| word | what it means | typical size |
|---|---|---|
| **smoke** | a tiny trial run whose only job is to confirm a new mechanism actually fires in production, before anyone measures a score with it | 2–5 legs |
| **suite** | a named, version-controlled list of legs run together, so an experiment can be repeated exactly | 5–30 legs |
| **pair** | the same suite run **twice on identical code**, back to back, with no edit permitted in between. Both scores are reported, and the mean is the result | 30 legs × 2 |
| **sweep** | a broad run over every leg in a pool, used to check that a change did not break something elsewhere | 30+ legs |

The **pair** is the important one, and the reason it exists is in Part 4: two identical
runs of the same code produced recall 0.29 and 0.57. Anything measured once cannot be
distinguished from that noise.

Two more conventions worth knowing:

- **Held-out bugs are spent once.** Two sets of bugs have never been tuned on: 12 dev
  bugs (`fresh12`) and a 27-bug final set. Looking at their output at all converts them
  into tuning data, so they are locked and launch only on an explicit instruction.
- **Raw results are committed to git before anyone scores them.** This stops the
  scoring from drifting toward the hoped-for answer.

## The dataset

Defects4J — real bugs from real Java projects (Apache Commons Math, Commons Lang,
JFreeChart, Google Closure Compiler), each with the buggy version, the developer fix,
and the failing tests. Patches come from the `drr` dataset of APR-tool outputs labeled
overfitting or correct.

Bug names throughout this document (`Math-65`, `Closure-38`, `Lang-63`, …) are
Defects4J identifiers. They recur constantly because the same handful of bugs turned
out to be the informative ones.

---

# Part 2 — The pipeline in seven stations

Every leg passes through these seven stations in order. The rest of this document
refers to them by number, because "which station does this change target" is the single
most useful question to ask about any edit.

| # | station | what it does | main modules |
|---|---|---|---|
| 1 | **Setup** | apply the patch; prove the bug's test fails before and passes after | `bug_context/` |
| 2 | **Rule-writing** | a model reads the **buggy** method body, the failing test, the class around it, and the patch **as a diff** (visible, but framed as a target, not an authority), and proposes general rules a correct program must obey | `relations/relation_synth.py` |
| 3 | **Rule screening** | each proposed rule is compiled and run ~20,000 times against the **buggy** program, to weed out rules that accuse everything | `relations/relation_screen.py` |
| 4 | **Harness writing** | a model writes fuzzing programs containing: copies of the failing test, the screened rules, and checks it invents itself | `harness/prompts.py`, `harness/campaign.py` |
| 5 | **Harness acceptance** | each harness must prove it fires on the **buggy** program before it is allowed to say anything about the patched one | `harness/campaign.py` |
| 6 | **Run against the patched program** | harnesses fuzz the **patched** program; every screened rule is also compiled on its own and run directly against it (called a *replay*). Anything that fires here is an accusation | `execution/fuzz_runner.py`, `run.py` |
| 7 | **The judge** | a model reviews each accusation with a package of mechanically computed facts attached, and rules it genuine or spurious | `relations/relation_verifier.py`, `relations/judge_decision.py` |

## Three things that are easy to get backwards

These three follow directly from the trust hierarchy in Part 1, and all three are
counterintuitive on first reading.

### (a) The rule-writer's *source block* is the BUGGY code; the patched code arrives only as a diff

The model at station 2 is shown, in this order (`relation_synth.py:503–577`):

1. **the bug's own failing test** — placed first and framed as authoritative, because
   the patch may be the overfit and where the test pins a direction the test wins;
2. **the buggy method body**, in full, explicitly labeled *"PRE-PATCH source — this is
   the code WITH the bug… Use it to see the code shape and API, NEVER as a model of
   correct behaviour"*;
3. **class-level context** — supertypes, collaborators, the classes the failing test
   constructs — also read from the buggy checkout
   (`assemble_class_context(buggy_dir, …)`);
4. **the patch**, as the complete diff file verbatim between `<patch>` tags, *plus* a
   distilled list of every changed line tagged `ADDED:` / `REMOVED:`;
5. javadocs, the class's own imports, the reachable API surface, mined sibling-test
   oracles.

**So the patched code is visible.** Every line the patch added appears twice — inside
the diff and again in the distilled list — and since the buggy body is shown in full,
the changed region of the patched method is reconstructible from the two. It would be
wrong to say the model cannot see the patched code.

What is separated is not *visibility* but **authority**. The two blocks are labeled for
opposite purposes:

| block | how it is framed to the model |
|---|---|
| buggy body | the source for code shape and API — *"NEVER as a model of correct behaviour"* |
| the patch | the thing to attack — *"propose relations that the CHANGED expressions could violate… the overfit may have changed these WRONGLY"* |

No block in the prompt is presented as an authority on what correct behavior *is*,
except the failing test and the javadocs. The patch is presented as a **target**: the
changed condition, boundary token or formula is where an overfit and a correct fix
diverge, so that is where the first relation must aim.

That framing is a prompt-level intention, and by this project's own standing meta-rule
(*a mechanism beats an instruction*) it should not be trusted on its own. The mechanical
enforcement lives downstream, at station 3: rules are screened on the **buggy** build,
so a rule earns its keep by how it behaves against known-mostly-correct code, never by
agreeing with the patch. A rule that merely restates what the patched code does gets no
credit anywhere in the pipeline for doing so.

*(Naming hazard if you read the source: the parameter is called `patched_sources`, but
`run.py:1213` fills it from `context.functions`, which `analysis.py:403` extracts from
`buggy_dir`. The name is a legacy misnomer and the code comment above it says so.)*

**Why the arrangement is this way:** the two failure modes are symmetric, and one of
each has actually happened. Show the patch as an authority and the model reads the
overfit's logic as the specification. Show only the buggy body and the model reads *the
bug* as the specification — which is exactly what happened before the failing test was
added to this prompt: relations came out **inverted**, asserting the buggy direction
(Lang-7). The failing test at the top exists to fix the second failure; the labeling and
the buggy-build screen exist to limit the first.

### (b) Screening decides on the buggy build; the patched build is measured too, but it never filters

Screening is buggy-only **by design**, and the reason is stated at the top of
`relation_screen.py`: a sound reference implementation to validate against does not
exist without cheating. The developer fix is off-limits. Sibling APR patches are
unlabeled and their overfit modes cluster, so cross-patch consensus would
preferentially kill the *most* discriminating rules. What is available for free is the
buggy checkout, which passes its entire test suite except the trigger tests — so it is
correct almost everywhere. That gives a cheap mechanical signal:

| buggy-build fire rate | reading | outcome |
|---|---|---|
| large fraction of random valid inputs | contradicts behavior we know is mostly correct — it would flag any implementation | drop |
| small fraction | consistent with detecting the localized defect region | keep, ranked first |
| never fires | consistent with everything we can check; can still catch a patch-introduced regression | keep, ranked last (one only) |

**The same relations are then run against the patched build** — `replay_on_patched()`,
which lives in the same module and uses the same counting wrapper, at two tiers: the
failing test's own input literals (replayed twice, as a determinism check) and the same
fuzzing budget the buggy-side screen used.

But that patched-side run is **the detection event, not a filter**. Nothing is ever
dropped because of how it behaves on the patched build — a firing there is the
accusation, and every one of them goes to the judge at station 7. The module's own
docstring is explicit: *"replay never convicts on its own."* The only patched-side
exclusion is mechanical: a relation that fails to compile against the patched checkout
is skipped.

This is the distinction the pipeline is built around, and it is worth stating in one
line: **the buggy build is where checks are judged; the patched build is where patches
are judged.** Using the patched build to filter checks would let the artifact under
suspicion decide which questions get asked about it.

One nuance that shows the two are separate gates: a rule that is silent on the failing
test's own inputs but loud on random inputs is marked `INVERTED-SUSPECT` and **demoted,
not dropped** — kept out of the harness prompt, but still sent to the patched-side
replay, where the judge is told about the inversion suspicion and decides. The screen
therefore produces two independent verdicts per rule: *may this be injected into a
prompt*, and *may this be replayed*.

### (c) Station 5's acceptance requirement makes part of every harness unreachable on the buggy build

A harness is accepted only if it fires on the buggy build. So any code placed *after*
the checks can never execute there — a check always fires first and stops the run. A
defect in that late code therefore shows up **only on correct patches**. That geometry
convicted an innocent patch (Closure-70) in all three arms of the fresh-15 experiment
before anyone noticed it.

**Is it fixed? The geometry is not; the damage is.** These are two different things and
it is worth keeping them apart.

The geometry itself is **permanent and deliberate**. It falls straight out of the
acceptance rule, and the acceptance rule is what makes a harness credible in the first
place — "this harness demonstrably sees this bug". Removing it would let harnesses that
have never been shown to detect anything go on to accuse patches. Nothing has changed
here and nothing is planned.

What was fixed is the pipeline *reading the asymmetry as evidence*, in three layers:

1. **Cycle 1 (Spec B) — stop asserting the false conclusion.** When the buggy replay
   dies at the harness's own alarm before reaching the site that crashed on the patched
   build, the comparison is uninformative, not exculpatory. It is now classified
   `SHADOWED` (`evidence_facts.py:150`), which attaches a note saying so and **never
   drops mechanically** — previously this path emitted "the crash is introduced by the
   patch".
2. **Cycle 2 (Spec G) — compute the answer instead.** Mechanically silence *every* alarm
   throw in the harness and replay that exact input on the buggy build
   (`run.py:2147–2170`). If the same crash signature reproduces, the crash pre-existed
   and was merely hidden behind the harness's own checks → upgrade to `PREEXISTING` and
   drop. This is what actually cleared Closure-70.
3. **Cycle 6 — don't trust a quiet muted run either.** A clean muted replay only counts
   as exculpatory when `diverted is False`, i.e. execution genuinely reached the site
   rather than being swallowed by a `catch (...) { return; }` (Part 5, C.8).

**Evidence it worked:** Closure-70's correct patch moved from a false accusation in
every prior roll to correctly cleared in cycle 2, attributed to the mute-all re-replay
in the trace, and held clear in pool30 and c3val. The cycle-2 ledger records the class
as dead — the one later Closure-70 false accusation came from a different,
invented-contract check, not from this geometry.

**Caveat, stated because nobody is watching it:** Closure-70 is not in the current
30-leg pair set (`pinned_dev.cases`), so this class has not been re-measured under
current code since late July. It is not a known residual, but it is also not under
observation.

---

# Part 3 — The starting line

## What existed at Kureha's last commit (2026-06-09)

A working pipeline for **crashing bugs** — bugs whose symptom is an exception or a
crash. The parts already in place:

- LLM harness generation with a build-and-repair loop (up to 3 repair turns on a
  compile failure, then reset);
- Jazzer (a Java coverage-guided fuzzer) as the execution engine;
- running compiled harnesses against both the buggy and the patched code;
- filtering that kept only crashing bugs, described in its own commit message as "a
  temp solution".

For a crashing bug the oracle is free: the program either crashes or it doesn't. The
harness only has to *reach* the bug.

## What changed when we moved to semantic bugs

A **semantic bug** produces a wrong *value*, not a crash. Nothing throws. Nothing
looks wrong from the outside. The harness must **supply the oracle** — it has to know
what the right answer is, for inputs no test covers.

This is a different problem, and everything in Part 5 is a consequence of it.

## June 9 – July 14: the infrastructure interlude

Roughly five weeks of work that was not about detection quality at all:

- moving the model backend to Azure Microsoft Foundry (`gpt-5.4`), with timeouts,
  retries and error handling for idle connections and deprecated bugs;
- making `defects4j` checkouts robust (stale directories, missing config files,
  classpath export failures on patched directories);
- reachability analysis via fuzz-introspector — adapted to the current JVM API,
  then bounded with a breadth-first search and a budget after it hung, then fixed
  again when it stalled on Math-2;
- throughput work for evaluating crashing bugs at scale (pre-built queues, filtering
  before checkout);
- an `.env` loader and configuration hygiene.

None of this moved a score. All of it was load-bearing — measurements that take eight
hours and die at hour seven produce nothing.

**Where semantic detection actually stood at the end of this era:** F1 ≈ 0.55 with
6 false accusations out of 10 correct patches. The oracle was a "lifted assertion" —
copy the `assertEquals` out of the failing test, reconstruct the call, throw on
mismatch. It worked for the test's own inputs and generalized badly.

---

# Part 4 — The score timeline

Read this table for shape, not for precision. Different rows use different bug sets,
different sizes, and different numbers of repeats. The columns that matter are the
**last two**: what was tried, and whether it stuck.

Precision = of the patches we accused, how many were really overfit.
Recall = of the overfit patches, how many we caught.
F1 = the harmonic mean of the two.

| date | measurement | result | what it was testing | verdict |
|---|---|---|---|---|
| early Jul | first semantic runs | F1 0.55, FP 6/10 | lifted assertions alone | oracle too narrow |
| 07-14 | `diag` 24-run | P 0.88 · R 0.58 · F1 0.70 | baseline diagnostics | recall is the gap |
| 07-17 | `p23gate` | recall 0.56→0.43, precision 13/14→8/12 | four changes bundled together | **negative** — and untraceable |
| 07-18 | `p23gate` remediation + replay | precision 1.00, recall 10/16 (dev) | replaying rules directly on the patched build | **the biggest single recall win** |
| 07-20 | `foc15` | R 0.89, P 0.73 | focused per-source rule synthesis | best recall on record; precision cost |
| 07-21 | `foc15b` | P 0.73→0.80, R held 0.89 | family + soundness fixes on top | precision recovered |
| 07-21 | A/B, 12 legs | 0.91 vs 0.77 | focused synthesis on vs off | killed focused synthesis — **later voided** (single roll) |
| 07-21 | 3-arm fresh-15 | A 0.62/0.62 · B 0.50/0.62 · C 0.33/0.25 | 15 **unseen** bugs | tuning didn't transfer; produced the G1–G5 plan |
| 07-25 | `pool30` | P 0.64 · R 0.50 · F1 0.56 | first broad sweep after cycles 1–2 | precision core improved |
| 07-25 | `poolA`/`poolB` | mean F1 **0.49**, R 0.29 vs 0.57 | **the same code, twice** | variance is the dominant problem |
| 07-25 | `night20` | 8/14 caught | width 7 + novelty gate | no frontier movement |
| 07-29 | `final30A`/`final30B` | A F1 0.64 · B F1 0.73, mean **0.685** | the milestone pair | recall moved; precision flat at exactly 5 |
| 08-02 | `pairA8`/`pairB8` | A F1 0.60 · B F1 0.69, mean **0.6448** | cycle-8 batch | **FAIL** against the pre-set bar of >0.685 |

The two paired rows are the important ones. `poolA`/`poolB` were byte-identical code
run twice, and recall came out 0.29 and 0.57. That single observation reframed the
entire project: from that point on, no claim based on one run was believed.

Official state today remains the July pair, **0.685**. The August pair did not replace
it, because it missed the bar.

---

# Part 5 — Everything we tried

## Era A — Building the machine (2026-07-15 → 07-18)

At the start of this era the pipeline had stations 1, 4, 5, 6 and a weak version of 7.
Stations 2 and 3 — rule-writing and rule-screening — did not exist. This era built
them.

---

### A.1 Dataset audit, pinned tasks, and label certification

**Target:** station 0, before the pipeline — `suites/`,
`dataset/eval_candidates.py`, `dataset/certify_detectability.py`.

**What that is for:** the benchmark itself. Every score is measured against labels
saying "this patch is overfit" or "this patch is correct". If the labels are wrong, so
is every number computed from them.

**Why the edit:** the labels were wrong often enough to poison measurement, in both
directions. Some patches labeled overfit turned out to be behaviorally identical to the
developer's fix — there was nothing to catch, so every "miss" on them was fictional.
Lang-41 was the case that made this undeniable: two different APR tools emitted
**byte-identical patch files** that the dataset labeled oppositely.

**What we expected:** a set of tasks where every miss is a real technique failure and
every false accusation a real safety failure, rather than a labeling artifact.

#### How the audit was actually done

**Step 1 — inventory.** Everything in `drr/Patches/`: 1,263 patch files.

| category | files | distinct bugs | usable? |
|---|---|---|---|
| `Doverfitting` — human-labeled overfit | 381 | 77 | yes |
| `Dcorrect` — human-labeled correct | 257 | 91 | yes |
| `Dunassessed` — no ground-truth label | 625 | 135 | **no** — unusable for scoring |

The labeled part covers **117 distinct bugs**. Note that files vastly outnumber bugs —
one bug attracts many patches (Math-80 has 31 overfitting files, Chart-1 has 16).

**Step 2 — classify each bug by kind, mechanically.** `eval_candidates.py` inspects the
trigger test's thrown exception: if it is a JUnit assertion type, the bug is
**semantic** (a wrong value); anything else is **crashing**. Result: 74 semantic bugs,
43 crashing. All the work in this document is on the semantic pool.

**Step 3 — partition by what each bug can measure.** A bug with only overfit patches can
test recall but not precision, and vice versa:

| partition | bugs | what it measures |
|---|---|---|
| paired (≥1 overfit **and** ≥1 correct patch) | 33 | precision and recall |
| overfit-only | 15 | recall only |
| correct-only | 26 | precision only |

**Step 4 — decide the unit of evaluation, and pin it.** Three separate decisions that
are easy to conflate:

- the **task** is per bug — (bug, candidate patch) against that bug's ground truth;
- the **unit of judgment** is the patch *file*, because patches for the same bug differ.
  Closure-86 proves the point: its `patch2` is equivalent to the developer fix while
  `patch3` and `patch5` are not;
- the **unit of scoring** is the bug, so no bug is counted twice because two tools
  attacked it. The APR tool is provenance, not task identity — the pipeline never uses
  it, it only namespaces filenames.

Every verdict is therefore recorded for **one pinned file** — the first in sorted order
— and since 2026-07-16 that pinning is enforced in code (`run_suite.sh`, `PatchSelector`),
so later runs evaluate exactly the files that were audited.

**Step 5 — the detectability probe.** This is the core mechanism, in
`certify_detectability.py`, and it is the one tool in the repository licensed to read
the developer's fix. Its docstring carries the firewall warning in a banner: dataset
construction only, never a pipeline input.

The method deliberately avoids needing an oracle:

1. A model writes a **printer probe** — a plain `main()` with *no test oracle and no
   judgment*. It enumerates at least 200 deterministic inputs across two surfaces:
   - **(a) the patched method's domain** — a fixed grid over the relevant parameter
     ranges, densely covering the boundary values of the condition the patch changed;
   - **(b) the failing test's own surface** — construct the exact objects the test
     constructs, then print the result of *every* public accessor of those objects, not
     just the patched method. This surface exists because an overfit frequently silences
     the test's symptom while leaving a sibling observable of the same object broken.
2. The probe prints one stable line per input: the returned value, or the thrown
   exception class and message.
3. It is compiled once and run against two builds — the **overfit-patched** build and
   the **developer-fixed** build — and the outputs are line-diffed.

The count of differing lines is the **divergence count**:

- **> 0 → certified detectable.** A perfect harness could catch this patch, so any miss
  on it is a real technique failure worth debugging.
- **== 0 → not certified.** More probes may find something (`--probes N`), but repeated
  zeros mean the bug leaves the recall denominator entirely.

**Step 6 — classify divergences by kind, because not all differences are real.** A
naive line-diff over-reports badly, so each divergence is typed:

| kind | example | counts? |
|---|---|---|
| **strong** | a different value, or a different exception *class* | yes |
| **weak** | same exception class, different message *wording* | **no** — both builds rejected the input; wording legitimately varies |
| float noise | last-bits differences | no |

Math-39 is the instructive case: its 7 "value" divergences are ~1e-13 relative
integrator noise, far inside the 1e-6 accuracy the API actually requests. Two correct
integrators legitimately differ there. That observation also became a pipeline
requirement — harness checks on that surface must use a tolerance.

**Step 7 — the mirror probe on the correct side.** The same differential is run with
`--label correct`: a patch labeled *correct* against the developer fix. Strong
divergences there mean the patch is behaviorally distinct from the fix — mislabeled, or
at least behaviorally wrong. This matters directly for scoring: a "false accusation"
counted against a bad label is partly the pipeline being *right*. The 2026-07-21
correct-side sweep covered 152 patches across 50 bugs (32 semantic, 18 crashing) and
found, on the semantic side, 90 correct, 2 mislabels (Lang-41, Math-63/SimFix), 2 probe
false positives (Lang-55), 2 excluded bugs (Closure-63).

**Step 8 — abstain instead of guessing.** The certifier was later changed to abstain
when a probe fails to launch or prints nothing. Before that, a probe producing no output
was indistinguishable from a probe finding no divergence — silence read as "identical".

**Step 9 — hand deep-dives where the probe found nothing.** A zero from the probe is not
proof of equivalence. Seven bugs are certified detectable on a hand-constructed
**witness** rather than a probe: Chart-7, Closure-62, Closure-92, Lang-41, Lang-60,
Math-57, Time-11. Two examples — Closure-92's witness is an `indexOf` character-widening
trap; Time-11's is a broken `ThreadLocal` that survives the patch and throws a null
pointer exception when touched from another thread.

**What happened:** it worked, and it was foundational. `pinned_tasks.jsonl` became the
only task set experiments run on. Every overfit patch in it is verified to actually
behave differently from the real fix; every correct label is double-checked. The
machine-readable truth lives in `suites/labels/*.jsonl`, with the human-readable
evidence in `DATASET_AUDIT.md` and the exclusion cases in `labels/incorrect_labels.md`.

Two by-products were worth as much as the labels:

- **The unwinnable list.** Lang-7, Lang-22, Math-30, Math-59, Closure-115, Closure-123,
  and the mislabeled correct sides of Lang-41 and Lang-10 are provably indistinguishable
  from the real fix in our environment, or wrongly labeled. Effort spent on them is
  wasted by construction, and they are excluded from the recall denominator rather than
  quietly depressing it.
- **Two data bugs found in the files themselves.** Math-2's correct patch file was
  stored reversed and truncated (repaired 2026-07-16). Lang-50's recorded 43 divergences
  turned out to be an artifact of the patch applier processing hunks out of order —
  fully applied, it is 0 of 518.

**Verdict: worked. Nothing downstream would have been believable without it. It is also
the single clearest instance of the project's recurring lesson — when a measurement
disagrees with the code, suspect the measurement's own scaffolding first.**

---

### A.2 Relation synthesis — station 2, new

**Target:** `relations/relation_synth.py`. New station.

**What that is for:** a model reads the **buggy** method body, the failing test, the
class around it, and the patch as a diff (see Part 2(a) — the patched code is visible
via the diff, but framed as a target rather than as an authority on correct behavior),
and proposes general rules a correct program must obey. These are called
**relations** — statements like "solve() must throw if the interval doesn't bracket a
root" or "these two ways of computing the same quantity must agree".

Two later fixes to this station's *inputs* are worth naming here, because both were
about direction rather than volume:

- **P2.1 — hand synthesis the failing test.** The trigger-test block was empty for the
  whole project's history, so synthesis read only the buggy body and wrote relations
  **backwards** — asserting the buggy behavior as correct (Lang-7). The test now goes
  first and is framed as authoritative.
- **Keep the diff's `+`/`-` markers.** Stripping them made added and deleted code
  indistinguishable. Which lines the patch *added* is exactly the direction signal.

**Why the edit:** the lifted-assertion oracle could only check the failing test's own
inputs. Overfits by definition survive those. Something had to generate claims about
inputs no test touches.

**How it works:** `RelationSynthesizer.synthesize()` assembles the labeled context blocks
listed in Part 2(a) and asks for up to `--synth_max_rules` (default 8) candidates. Each
candidate comes back as a **Relation** — a name plus a compilable Java `check` body, not
prose — so that station 3 can execute it. A `--focused_synthesis` mode replaces the one
broad call with several narrow passes, one per contract source (formula, documented
`@throws`, sibling-family agreement, state), unioned afterwards (see B.5). Two supporting
mechanisms sit alongside: `repair_check()` gets one attempt to fix a candidate that fails
to compile, and `harden_for_soundness()` narrows a candidate that turns out too broad.

**What we expected:** the recall engine.

**What happened:** it became exactly that — and simultaneously the source of every
false accusation in the project. Both facts are permanent. The 3-arm experiment's
audit put it plainly: *"This is the engine of every real catch we got. It also invented
every false accusation. The problem is never that it invents; it's that bad inventions
survive the next step."*

**Verdict: worked, and is irreplaceable. Its failure mode moved quality control
downstream, where the rest of this document lives.**

---

### A.3 Buggy-build screening — station 3, new

**Target:** `relations/relation_screen.py`. New station.

**What that is for:** compile each proposed rule and run it ~20,000 times against the
**buggy** program. A rule that fires on nearly every input is measuring its own setup,
not the bug. A rule that fires specifically on **the failing test's own input
literals** is "direction-confirmed" — it fires exactly where the test says the buggy
code is wrong, so it is aimed at this defect rather than at the implementation in
general. Direction confirmation is a buggy-build property; it says nothing about the
patched build (Part 2(b)).

**Why the edit:** stations 2 and 4 invent freely; something had to filter.

**How it works:** each candidate's check body is wrapped in a **counting harness** — a
generated Java class where the check's violation throw is caught and tallied rather than
surfaced to the fuzzer, so one run yields a rate instead of stopping at the first
failure. It is compiled with the normal `HarnessBuilder` against the buggy classpath and
run for a fixed `-runs` budget. A shutdown hook prints one line, `[relscreen] checked=N
violated=M`, which is parsed back out. Below `MIN_CHECKED = 100` executed checks the run
is treated as uninformative (typically the body's own input fences rejected everything).
Above `MAX_FIRE_RATIO = 0.20` the rule is out of domain. Anything that fails to compile
or run is dropped outright — a rule we cannot execute is a rule we cannot screen, and
unscreened means uninjected.

Each rule is measured twice, on two different input sets: the **failing test's own input
literals** (which is what produces the `DIRECTION-CONFIRMED` label) and a **random fuzz
corpus** (which produces the fire rate). The two answers together drive the ladder in
Part 2(b).

**What we expected:** a safety gate that kills junk rules before they can accuse
anybody.

**What happened:** the mechanism was right, and for months the gate was a rubber stamp.
The 3-arm audit found it had kept: a rule firing on **100% of inputs** on the buggy
build (Chart-19 — it was detecting its own setup); rules firing on **0** inputs, kept
as tripwires (Lang-63); and a rule comparing a function against another function
that merely calls the first one, so the two sides literally run the same code and can
never disagree (Lang-63 again). The numbers needed to catch every one of these were
already being computed and printed — they were simply never used.

**Verdict: right idea, under-used for months. Fixing it was the "G3" work in cycles 2–6,
and it eventually became the fire-rate facts (C.2).**

**Companion that just worked:** `rule_compile_repair` — rules that almost compile get
one repair attempt. About 22% of candidates were rescued, at no measured false-fire
cost. Cheap, real, kept.

---

### A.4 The relation verifier — station 7, new

**Target:** `relations/relation_verifier.py`. New station.

**What that is for:** the judge. It reads an accusation plus an evidence package and
asks: would *every* correct program satisfy this check, or could a correct one trip it?

**Why the edit:** with stations 2 and 3 generating claims, something had to separate
real alarms from spurious ones. There is no mechanical substitute — no developer fix
is available at decision time.

**How it works:** `verify()` is called once per firing. It is handed the harness source,
the fired alarm message, the relevant source and documentation, and — the part that grew
over the whole project — a block of **mechanically computed facts** about that specific
firing: did this check also fire on the buggy build at this exact input, does the real
failing test pass on this build, what fraction of random inputs does it fire on, was the
buggy-side replay shadowed. The model returns a `VERDICT:` line, a `WHY:` line, and (from
cycle 5) a `CITATION:` line that must hold a passage copied verbatim from the material it
was shown.

One structural detail in that prompt turned out to matter enormously later. The citation
requirement is **asymmetric**, stated in the prompt itself: a dismissal must quote
something, but *"for a SOUND verdict, CITATION: NONE is fine"* — a SOUND verdict being
the one that convicts the patch. Measured consequence: **90% of accusations cite nothing,
versus 6% of dismissals.** The accusation side faces no evidence requirement at all, and
that asymmetry is the root of the precision ceiling described in Era D.

A `votes` parameter supports asking several times with different review angles. It was
measured twice and is dead both times (C.6).

**What we expected:** a soundness filter.

**What happened:** it is necessary and it is the weakest link, and it stayed the weakest
link through every subsequent cycle. It is also where the project's central lesson was
learned, stated in `plan.md` as a standing meta-rule:

> *The judge needs computed facts, not exhortations.* Every false-alarm class was fixed
> by computing a fact and putting it in the judge's evidence — never by asking it to be
> more careful.

**Verdict: kept, permanently constrained. Everything in Eras C and D is either about
computing a new fact for it, or about taking a decision away from it.**

---

### A.5 The P3.2 replay stage — station 6

**Target:** `run.py`, the `--replay_relations_on_patched` path.

**What that is for:** at station 6, where the patched program is finally run, every rule
that survived screening is compiled as a standalone program and executed directly
against the patched build, with fixed inputs repeated deterministically — rather than
only being embedded in a harness and hoping the fuzzer happens to hit the right input.

**Why the edit:** a rule delivered as advice inside a harness prompt only fires if
(a) the model implements it correctly and (b) the fuzzer stumbles onto a triggering
input. That is a product of two small probabilities.

**How it works:** `replay_on_patched()` reuses the exact counting-harness wrapper from
station 3, but compiles it against the **patched** checkout instead of the buggy one, and
measures each rule at two tiers:

- **trigger tier** — replay only the failing test's own input literals, with the fuzzer
  budget set to zero, **twice**. Running it twice is the determinism check: if the two
  runs disagree, the rule is flaky and is labeled as such. This tier exists because an
  overfit only guarantees the test's own *assertions* pass, not that other contract
  relations hold at those same inputs. Math-2 is the example: `sample()` passes at the
  trigger parameters while `getNumericalMean()` is still −49.76 there.
- **fuzzed tier** — the same `-runs` budget the buggy-side screen used, so the two sides
  are comparable.

Any rule that fires returns a finding dict, and the caller must pass every one through
the judge. Replay never convicts on its own.

**What we expected:** rules would actually get executed instead of merely being
proposed.

**What happened:** **the single biggest recall win in the project.** It made
`Math-2-o`'s convicting formula fire — a formula that had been synthesized, screened
and delivered for weeks and never once caught anything, because nothing executed it
directly. Precision on the dev set went to 1.00 with recall 10/16.

This produced the second standing meta-rule:

> *A mechanism beats an instruction, everywhere.* A recall idea only counts if its
> check reaches the patched build mechanically. Anything delivered as prompt advice is
> multiplied by the model's implementation rate and the fuzzer's luck, and that product
> is small.

**Verdict: worked, decisively. Also the origin of a recurring operational hazard — the
flag is not in older suite definition files, and a 30-leg run was once launched without
it and had to be killed and restarted.**

---

### A.6 Relation pooling — proposed, shipped, then removed by user rule

**Target:** `run.py`. Sharing screened rules between legs.

**Why the edit:** rule synthesis is random. If a bug's correct-patch leg invents a good
rule and the overfit leg doesn't, pooling within the run would let both legs use it —
a cheap fix for the invention lottery.

**What we expected:** a large recall gain for free.

**What happened:** removed on 2026-07-19 by user decision, and elevated to a hard rule.
Two reasons, neither of them about measured effect:

1. Sharing instruments *between runs* farms the benchmark — the pipeline accumulates a
   library of known-good checks for known bugs and its score stops meaning anything.
2. Sharing *within* a run, between a bug's two legs, still hands a leg a verdict it did
   not earn from that patch alone. A real deployment sees one patch, not a matched pair.

The sanctioned compensation is **more of a leg's own rules** (`--synth_max_rules` 8,
every screened survivor feeding replay). A useful side effect: with pooling gone there
is no ordering constraint between legs, so suites can run fully parallel at any size.

**Verdict: rejected on principle, not on evidence. Permanent. Listed first in the
rejected-ideas ledger.**

---

### A.7 The metamorphic-family menu (R4) — station 2

**Target:** `relation_synth.py` — a curated menu of 62–84 metamorphic relation families
mined from the research literature, offered to the model to pick from.

**Why the edit:** free invention seemed likely to be narrow and repetitive. A menu
would guarantee diversity.

**What we expected:** broader coverage of check shapes.

**How it was tested — the part worth copying.** Before wiring the menu into the pipeline,
it was measured against the thing it was supposed to improve: take the relations free
invention had *already* produced across archived runs, and ask what fraction of them fall
into one of the menu's families. That is a coverage question answerable offline for
roughly zero cost, and it kills the idea outright if the answer is low.

**What happened:** built 2026-07-18 and demoted **the same day** by that coverage test.
The menu covered roughly **18% of what free invention already produces**. The model's
unconstrained inventions were far more diverse than the curated list — the menu would
have *narrowed* generation while appearing to broaden it.

**Verdict: dead. Recorded explicitly so "force each round to pick a different family
from a list" is never re-proposed as a variance fix.**

---

### A.8 Context enrichment A1–A7 — station 1 and station 2 inputs

A batch of seven changes to what the models are shown:

| item | change | outcome |
|---|---|---|
| A1 | the verifier must not read a partial class skeleton as a complete one | kept — it was inferring "method doesn't exist" from truncation |
| A2 | differential-firing attribution check — is this firing patch-caused? | later became the attribution judge, which was dropped (B.4) |
| A3 | synthesis probes the code's own input-domain boundaries | kept |
| A4 | field-coupling context — surface shared-state siblings with no call edge | kept |
| A5 | feed documented preconditions to the harness generator | kept |
| A6 | class skeleton into the consistency-harness slot | kept |
| A7 | retire the `_STAT_PATTERNS` name whitelist | **removed** — it was a Math-shaped name list, i.e. dataset overfitting |

A7 is the one worth noting. It matched method names against a hand-written list of
statistics-flavored patterns. It worked on Commons Math and would have transferred to
nothing. Deleting it is an instance of the standing **no-dataset-overfitting** rule:
mechanisms may encode general categories, never the shape of a specific benchmark bug.

**Verdict: mostly kept; A7 removed on principle; A2 later dropped on measurement.**

---

### A.9 The p23gate lesson — measurement discipline

**Target:** not code. Process.

On 2026-07-17 four changes were turned on together and measured as one. The result was
negative — recall 0.56→0.43, precision 13/14→8/12 — and **nothing could be attributed**.
It took a full day of forensics to work out which change caused what.

The resulting rule, now first in the measurement rulebook: **change one thing per
measurement point.** It has been violated a handful of times since and cost a day each
time.

**Verdict: the most expensive process lesson in the project, and it stuck.**

---

## Era B — Prompt and judge iteration (2026-07-19 → 07-21)

This era tried to fix quality by changing what the models are told. Its most valuable
output is a clear demonstration of where that approach's ceiling is.

---

### B.1 Show the whole test, and the real failure message (H1/H2/H3)

**Target:** station 4 harness prompts, extended to station 2 rule synthesis.

**Why:** the models were being shown fragments — a truncated test, a paraphrased
failure. They were guessing at the pinned expectations.

**What we expected:** better-grounded checks.

**What happened:** kept. Gated on fidelity — if the real text cannot be recovered, do
not substitute a paraphrase.

**Verdict: worked, uncontroversially.**

---

### B.2 R-THROWS — mandatory documented-`@throws` tripwires

**Target:** station 2.

**Why:** Math-53 is a bug about an exception a method is documented to throw and
doesn't. No rule was ever proposed about documented exception contracts, because
nothing asked for them.

**What we expected:** coverage of a whole class of contract bugs.

**What happened:** kept — and it produced Lang-27's first-ever catch. It also produced
a false-accusation class immediately: a check saying "input X must throw" fires on a
correct program if X also violates a *different* documented condition. That needed the
**throws-pass isolation rule** — the input must break exactly one documented condition
and no other.

**Verdict: worked, after one follow-up fix. A good example of the general pattern —
a new check family gains recall and immediately costs precision until it is fenced.**

---

### B.3 JD1 — seed the patched-side fuzz with the buggy-side firing inputs

**Target:** station 6.

**Why:** if an input made a check fire on the buggy build, that input is interesting.
The patched-side fuzz was starting from scratch.

**What we expected:** less reliance on fuzzing luck.

**Verdict: kept. Small, cheap, no downside found.**

---

### B.4 The attribution judge — dropped

**Target:** station 7 — a *second* judge, behind `--attribution_judge`, asking "is this
firing really attributable to the patch?"

**Why:** the soundness judge sometimes convicts on firings that have nothing to do with
the patch.

**What we expected:** a precision filter.

**What happened:** in the 3-arm experiment it examined 6 cases, **changed zero
verdicts, and approved every wrongful conviction it saw**. Worse, on the `falsefix13`
suite it vetoed close to **100% of sound catches**. It also fails open — anything short
of an explicit veto keeps the conviction — so on genuine catches it can only subtract.
Arm C, the arm with this flag on, scored 0.33/0.25.

The structural diagnosis: its question — "did the buggy version also misbehave / is
this documented?" — is not the question that separates real alarms from false ones.
No amount of retuning fixes a judge asking the wrong question.

**Verdict: dropped, off by default, with a do-not-re-enable condition recorded. The
one surviving piece is the mechanical direction-confirmed keep, which was validated
separately.**

---

### B.5 Focused per-source synthesis — the kill that was later voided

**Target:** station 2. Instead of one synthesis call producing all rules, run separate
passes per evidence source — a formula pass, a documented-`@throws` pass, a
defect-family pass, a state pass — and union the results.

**Why:** one call mixes sources and under-covers most of them. Asked for eight rules in
one breath, the model produces eight rules about whatever it found most salient —
typically the formula — and never gets to the documented exceptions or the sibling
methods at all.

**How it works:** `RelationSynthesizer(focused=True)` runs one narrow call per contract
source, each told to enumerate *all* of that one source's constraints rather than to
produce a general assortment. The four passes are:

| pass | what it is told to enumerate |
|---|---|
| formula | closed-form/arithmetic properties of the patched method |
| throws | every documented `@throws` condition, as a tripwire |
| family | agreement relations against sibling methods that share documented behavior |
| state | read-only and receiver-state properties |

The results are unioned and go through the same screen as always. Cost is roughly one
model call per pass instead of one per leg.

**What we expected:** more, better-grounded rules.

**What happened, in three acts:**

1. `foc5`: caught **4 of 4** targeted misses. Best recall evidence in the project.
   Also produced a fourth false accusation.
2. `foc15`: **R = 0.89**, the highest recall ever recorded here, at P = 0.73. After
   family and soundness fixes, `foc15b` brought precision to 0.80 with recall holding.
3. `ab_off` vs `ab_on`, 12 legs, one roll each: F1 0.91 vs 0.77. **Turned off.**

Then, on 2026-07-25, the paired pool measurement showed that ±4 catches of pure noise
on 30 legs is *larger than the gap that killed the flag*. Single-roll comparisons were
banned retroactively. The kill verdict does not meet the project's own evidence
standard any more.

**Verdict: undecided, and it is the largest voided decision on record.** The kill may
still be right — its false accusations were real. But it has never been re-measured
under the paired rule. It sits in cycle 8 as an optional third experimental arm,
deprioritized only because 8.14c later showed the class it would help is small.

---

### B.6 The soundness protocol — step 4b, the rounding floor, the NaN rule

Three judge-side rules, all at station 7, all shipped, all still in place:

- **Step 4b — counterexamples must explain the observed firing.** The judge kept
  dismissing accusations with hypotheticals that could not have produced the number
  actually observed. Example: Chart-7 was dismissed via an "integer rounding"
  counterexample, when the observed difference was index 1 versus index 3 — not a
  rounding effect at all.
- **The rounding floor.** A numeric violation within about 1e-9 relative is floating
  point noise, not a defect. This came from Math-2's correct patch being accused over
  a difference in the last bits.
- **The NaN-artifact rule.** A firing that is structurally impossible indicates a
  broken check comparison, not a defect.

**Verdict: all worked. All are examples of the pattern that kept working — encode a
mechanical constraint, do not ask for more care.**

---

### B.7 The 3-arm fresh-15 experiment — the honest exam

**Target:** the whole pipeline. 15 bugs it had **never been tuned on**, 18 legs,
three configurations differing only in two flags.

| arm | flags | caught | missed | wrongly convicted | passed | P | R |
|---|---|---|---|---|---|---|---|
| A | both off | 5 | 3 | 3 | 7 | 0.62 | 0.62 |
| B | +focused synthesis | 5 | 3 | 5 | 5 | 0.50 | 0.62 |
| C | +attribution judge | 2 | 6 | 4 | 6 | 0.33 | 0.25 |

Three findings, each of which changed the project:

1. **On tuned bugs the pipeline scored 0.80/0.89. On fresh bugs, 0.62/0.62.** Every
   precision gain from July's tuning evaporated on unseen bugs.
2. **Honest recall was lower than 0.62 — about 0.44.** One catch was luck (a check
   that fires on 1.4% of fuzz runs); one hung on a difference of 0.00000000006, below
   the system's own trust threshold, and vanished in two arms.
3. **The arm ranking itself was unreliable.** A dedicated audit showed arm C's collapse
   was mostly re-roll luck, not its flag. What *was* reliable was what repeated across
   all three arms — the same 3 wrongful convictions and the same 2 misses everywhere.

The experiment produced the **G1–G5 fix plan**, which is the agenda for all of Era C:

| | class | meaning |
|---|---|---|
| **G1** | masked / shadowed facts | facts computed backwards or never computed at all |
| **G2** | trust-domain fencing | invented "contracts" the judge finds plausible and cannot verify |
| **G3** | screening facts | measurements that exist but never reach the judge |
| **G4** | diff-class evidence | is the observed divergence the same *kind* as the failing test's? |
| **G5** | fact priority | a computed fact must not be overridable by a plausible story |

**Verdict: this is the most valuable measurement in the project. It cost 15 fresh bugs
— burned permanently, since looking at their output turns them into tuning data — and
it bought an accurate picture of where the pipeline actually stood.**

---

## Era C — The cycle campaign (2026-07-24 → 07-31)

Seven numbered cycles, each with written specs, a validation run, and a recorded
outcome. This is where the project's method matured. Three working rules were adopted
here and held for the rest of the project:

- **Write the requirement without naming any bug.** If a change cannot be stated as a
  general rule — "a replay that errored must not be reported as clean" — and only as
  "make Closure-70 pass", it is tuning to the benchmark and is rejected. Bug names are
  allowed only as the *provenance* of a test fixture.
- **Every wrong behavior seen on the VM becomes an offline test the same day**, built
  from the real recorded output that showed it. The offline test suite grew from 0 to
  several hundred cases this way, and each one is a real observed defect rather than an
  imagined one.
- **No leg counts as fixed until two consecutive runs agree** *and* the responsible
  mechanism is visible in the recorded trace. A flipped verdict on its own is not
  evidence.

---

### C.1 Cycle 1 — make the facts honest (G1 + G5)

**Target:** station 6→7 evidence assembly. Created `relations/evidence_facts.py`.

**What that is for:** when a check fires on the patched build, the judge does not get
the bare accusation. It gets a package of **mechanically computed facts**: did the same
thing happen on the buggy build? does the real failing test pass on this build? what
did the documentation promise? These facts are what keep the judge from arguing from
first principles.

**Why the edit:** the facts were being built inline inside `run.py`, could not be unit
tested, and three wrong-fact bugs had shipped. Worse, some of them were **wrong in the
dangerous direction** — manufacturing evidence against correct patches.

Four concrete defects fixed:

- **Spec B — "clean" that wasn't clean.** `replay_input` returned `None` both when the
  buggy replay ran fine *and when the replay itself failed to run*. The caller then
  emitted "this crash is introduced by the patch". An infrastructure error was
  manufacturing evidence against the patch. Split into
  `crashed | clean | error`, with `error` → abstain.
- **Spec B, part 2 — oracle shadowing.** When the buggy replay crashed at the harness's
  *own* alarm before reaching the code that crashed on the patched build, the comparison
  is uninformative — but the code reported it as "introduced by the patch". This is the
  station-5 geometry from Part 2, and it convicted the correct Closure-70 patch in all
  three arms of the fresh-15 experiment.
- **Spec C — extrapolation.** A note told the judge "a screening result of
  DIRECTION-CONFIRMED already establishes the buggy build violates this check". But
  that was established at *screening* inputs, not at this firing's input. The sentence
  licensed generalizing across input regimes and did so in two of the three robust
  wrongful convictions (Math-30, Math-65).
- **Spec D — the lift-note landmine.** When a check's *name* matched `lift|seed[-_]?test`,
  both branches of the attached note ended in dismissal: value matches the test →
  dismiss; setup diverges → also dismiss. But a divergent setup producing a value far
  from the test's own is the *definition* of a generalization catch. This wording killed
  one. Fixed by computing a real value comparison first, and permitting dismissal
  wording only in the `matches` branch.
- **Spec E — fact priority.** A prompt rule: a computed fact can only be overruled by
  another computed fact, never by provenance ("this check encodes the library's own
  trusted regression test").

**What we expected:** the false accusations that ride on wrong facts to disappear.

**What happened:** every mechanism fired correctly under test — the new wording appears
verbatim in live traces, the extrapolation sentence is gone — and **the headline
verdicts did not change**. Closure-70, Math-30 and Math-65 were still false accusations.

The diagnosis was the useful part: with the fact honestly UNKNOWN, the judge **filled
the vacuum with ideal-math stories**. Math-30's rationale literally blamed an
`int n1*n2` overflow that is present in the developer's fix too.

**Verdict: necessary, not sufficient. "Facts computed wrong" was fixed. "Facts not
computed" remained, and hypothetical correctness rushed into the gap. That set cycle 2's
agenda.**

---

### C.2 Cycle 2 — compute the missing facts (muted replay + fire rates)

**Target:** station 6. New module `execution/oracle_mute.py`.

**The idea.** When a check fires on the patched program, the most valuable thing to know
is whether the *same* check also fires on the buggy program at that same input — if it
does, the behavior is not the patch's fault. The way to find out is to re-run that exact
input on the buggy program. But often some *other* check throws first and ends the run
before the one we care about is ever reached. Cycle 1 could only report that honestly and
leave the question open.

The insight: the harness is just Java source that we generated, and alarm throws are
syntactically recognisable. So **delete the throws that are getting in the way, recompile,
and run the input again.**

**How the transform works** (`execution/oracle_mute.py`, a pure function with no file or
network access, so it is fully unit-testable):

- It scans each `throw` statement from the keyword to the `;` that ends it, tracking
  bracket depth and staying outside string literals, character literals and comments. So
  a multi-line alarm message built with `+` is treated as one statement, and a `;`
  inside a string is never mistaken for the end.
- It only touches **alarm** throws, identified by one of three markers: the message
  starts with the mandated `[oracle:<id>]` prefix; the thrown type contains
  `FuzzerSecurityIssue` (the fuzzer's own alarm family); or the message matches the
  `relation <name> violated` shape used by the screening wrapper. Ordinary rethrows and
  input-rejection throws carry none of these and are never modified.
- The matched throw is replaced by a bare `;` plus a `/* muted:<id> */` comment.

There is a known Java hazard: removing a guaranteed throw can break the compiler's
"every path returns a value" analysis. The transform does not try to be clever about it —
the recompile catches it, and the caller falls back to the honest "unknown" answer rather
than to a guess. Two modes exist: silence one named check, or silence all of them (used
when the patched-side failure was a crash rather than a check firing).

This is a source-to-source transform on already-generated Java. No prompt change, no
model involved. It computes a fact that was previously uncomputable.

**The second half — deliver the numbers that were already being computed.** The screening
station at step 3 already knows, for every check, what fraction of random valid inputs it
fires on. The patched-side run knows the same number for the patched program. Neither was
reaching the judge in usable form. They now arrive as a labeled block stating the raw
counts, the percentages, and what each pattern means:

| pattern | what it says |
|---|---|
| fires on ≥20% of random valid inputs on the **patched** program | a contract violated by a fifth of all valid inputs indicts the check, not the patch — real defect detectors fire rarely and asymmetrically |
| fires on ~100% of inputs on the **buggy** program | the firing is intrinsic to how the check is written, not a detection of the defect |
| low on both | no block emitted, so the judge is not given noise |

No automatic dropping anywhere in this — the numbers go into the evidence and the judge
still decides. The thresholds live in one place and are shared with the screening code,
so the two cannot drift apart.

**What we expected:** the false accusations caused by unanswerable buggy-side questions
to die.

**What happened — the first verdict flips of the campaign:**

- **Closure-70 → correctly cleared** (had been a false accusation in every prior roll).
  The mute-all re-replay proved the crash pre-exists, hidden behind the harness's own
  alarms.
- **Math-30 → correctly cleared.** The muted replay showed the check fires on the
  buggy build too.
- **Math-65 → still a false accusation.** The fire-rate fact *did* attach — 82% buggy /
  73% patched with indictment wording — and sibling invented checks were kept anyway via
  documented-contract stories.
- **Math-68 → a regression.** The new "identical on both builds" fact was an
  over-claim: it knew only that the same check *fired* on both builds, not that the
  *values* matched. A partially-unfixed overfit fires the same check on both builds with
  different wrong values. This mechanically dismissed a genuine catch.

**The Math-68 regression produced cycle 2b/2c/2d,** three same-day iterations:
identity must be *earned* by an observed-value comparison (2b); key=value pairwise
cross-build comparison with NaN and infinity handling (2c); identical-on-both-builds is
*terminal* — no contract argument can rescue it (2d).

**And 2d produced the finding that shaped cycles 3–6.** Its judge overrode a fully
earned, value-certified, terminally-worded identical fact using exactly the argument the
rule pre-empts: *"no correct implementation could return NaN"* — while the buggy build
returns it. Three different wordings, three overrides.

> **The G5 prompt route is exhausted.** When the earned identical fact exists, the drop
> must be mechanical.

**Verdict: cycle 2 is the most successful single cycle in the project.** The
shadowed-replay evidence vacuum is closed; the harness-crash-behind-oracles class is
dead. It also cleanly isolated the two residual classes that everything afterwards
fights: (1) invented contracts the judge finds plausible, (2) recall lost to synthesis
roll variance.

---

### C.3 The retrospectives — the loop examines itself

Three numbered retrospectives, 2026-07-25. These are about the *process*, not the
pipeline, and they produced rules that are still binding.

**Retro #1 — the breadth gap.** Every validation since cycle 1 had run the **same 4
legs**, while cycles 2–2d changed shared evidence paths affecting *every* leg. 26 of
~30 legs had never been re-run under the new code. New rule: **after two consecutive
hotfixes on the same thread, the next action must be a breadth measurement, not a third
hotfix.**

**Retro #2 — the pool30 sweep, and one finding that paid for the whole run.**
Math-73's correct patch had the *same* bogus behavior judged **twice in one leg**: the
replay-track judge, handed the fire-rate and identical facts, correctly ruled it
spurious; the harness-track judge, which never receives those facts, kept it and
produced a false accusation.

That is a controlled experiment inside a single leg: **the facts work; one of the two
tracks just doesn't get them.** It became cycle 3's top item.

**Retro #3 — variance is the problem.** `poolA` and `poolB`: identical code, identical
30 legs, recall 0.29 vs 0.57. Catches swung ±4 legs between identical rolls. Every
single-roll score in the campaign to that point carried that noise.

The decomposition:

1. **Generation lottery, dominant.** Whether a fake is caught is substantially "did
   this roll invent the right check". Lang-41's catching relation simply wasn't invented
   in roll A. Closure-92 invented a *broken* string-compare check instead of the order
   checks that catch it — and the fire-rate fact correctly dismissed the broken one.
2. **Judge nondeterminism.** Lang-60: byte-identical firing and evidence as pool30's
   keep, flipped to dismiss via a "lazy compaction" hypothetical the protocol's own
   rules forbid. Pure drift.
3. **Accusation lottery.** False-accusation count 2 vs 6 across the pair.
4. **The fire-rate machinery was exonerated** — zero observed suppression of genuine
   catches.

**Verdict: single-roll comparisons banned permanently. The paired rule is load-bearing
for everything after this point. This is also the moment the project's ambition changed
from "raise the score" to "understand the variance".**

---

### C.4 Cycle 3 — one door, and the mechanical-drop lesson

**Target:** stations 6 and 7.

Three changes.

**Spec K — give both routes the same facts.** A check can reach the judge by two
different routes: embedded in a harness, or executed directly as a standalone rule. Only
the second route was carrying the measured facts with it. The fix gives a harness-borne
firing the same fact package by mechanically matching it back to the screened rule it
corresponds to — first by normalized name, then by distinctive shared words in the check
text, and if neither matches, no fact is attached rather than a guessed one. The name
"one door" is shorthand for the intended property: *however a check got here, it is
judged on the same evidence.*

**Spec J — an exception for firings at the test's own inputs.** A check that behaves
identically on both the buggy and patched programs is normally measuring pre-existing
behavior and should be dropped. But if it fires at the failing test's *own input values*,
on something the test does not itself check, that is the signature of a patch that did
not actually fix the bug — and it must be kept. Both conditions are computed
mechanically, with a guard against trivial coincidences: a numeric match counts only if
the literal has at least 4 significant digits, a string match only if it is at least 8
characters.

**Spec L — reject checks that destroy the evidence.** A check that catches an exception
and records only a bare flag (`catch (...) { success = false; }`) throws away the
exception's identity, which several downstream safety checks need in order to tell a
pre-existing crash from a new one. A static detector now rejects that shape at screening
and at harness acceptance, so the model regenerates.

**What happened:** the recall side came out fully clean, the erased Math-2 catch was
restored, and Spec K was confirmed working in production — Math-73's correct patch was
cleared, because the "fires on 100% of buggy inputs" fact now reached the harness route
and the judge used it correctly.

Precision converged to **one class**: fresh-roll invented contracts. Every remaining
false accusation was a newly invented check shape that structurally dodged machinery
built from earlier shapes.

**The mechanical-drop lesson, learned twice.** The first version of the identical-drop
(cycle 3.1) fired exactly once across 30 legs — and that one firing **killed a genuine
catch** (Math-2). It was defused to an evidence fact the same night. This is now a
standing rule:

> Never auto-dismiss on an ambiguous signal. Both mechanical auto-dismissals we tried —
> latent firings, and same-name reconciliation — each killed a true catch and had to be
> narrowed to compute-the-fact-and-let-the-judge-decide.

The same night produced a process rule: **any change that silently discards findings
ships with a shut-off condition written down in advance.** Before the run, state what
result would mean the change is doing more harm than good — here, "it dropped a finding
on an overfit patch that then went uncaught" — and if that happens, the change is
automatically demoted from a decision to a piece of evidence. That night the demotion was
done by hand; the rule exists so the next one is not.

**Verdict: giving both routes the same facts worked and is permanent. The automatic drop
worked only after the exception was added. The detector worked. The class of false
accusation that remained needed a different kind of answer.**

---

### C.5 Cycle 3b — universal screening (built, and inert)

**Target:** station 3, extended to cover checks invented inside harnesses.

**Why:** rules that go through station 3 get measured — we know their fire rate on the
buggy program. Checks the model invents *directly inside a harness* never pass through
station 3 at all, so they arrive at the judge with no measurements whatsoever. And every
remaining false accusation was riding on exactly such an unmeasured invention. The fix
looked like pure coverage: measure those too.

**Version 1 — cut the check out and screen it.** Parse the harness source, extract the
code region that produces each alarm, and feed that fragment through the same counting
harness station 3 uses.

**What happened: effectively nothing.** A check pulled out of a real multi-check harness
almost always references sibling checks, helper methods and fields declared elsewhere in
the file, so the extracted fragment does not compile. A compile failure means no fact is
produced (correctly — it fails safe), so the mechanism was safe, correct, and covered
almost nothing. It shipped and did essentially zero work.

**Version 2 — measure it where it already compiles.** Rather than cutting the check out
of the harness, leave it in the harness and silence everything around it, using the
muting transform from cycle 2. The full harness is known to compile, since it was
accepted. Silence every sibling alarm, run it against the buggy program with the counting
wrapper, and the surviving alarm's fire rate is the measurement. Every piece already
existed; the work was connecting them.

**Also in this cycle — a stopping condition for generation.** A run may not finish while
every check it has produced fires *only* on the failing test's own literal values. Such a
set can confirm the known bug and detect nothing else, so the campaign requests another
round of rule-writing, up to a bounded limit, printing a line each time. It was armed and
fired zero times in validation, because every leg in that set already had checks with
reach beyond the test. That is the gate behaving correctly — it targets weak runs, and
there were none in the sample.

**Verdict: v1 failed on a mundane engineering reality; v2 was the right shape. The
episode is the clearest example of a recurring pattern — a mechanism that is safe,
correct, and covers nothing.** It later became standing rule 15 (see Part 7).

---

### C.6 Cycle 4 — the stability cycle, mostly negative

**Target:** stations 2 and 7. Aimed directly at the variance Retro #3 measured.

- **4a-i: generation width.** Raise the number of harnesses and synthesized rules per
  leg, so every leg gets a bigger arsenal. Standardized at `-n 5 -m 12` by user
  decision. Note the tension: an earlier finding recorded that *blanket* increases in
  harness count are a false-alarm lottery ticket on correct patches, and zero false
  alarms had been measured at n=3. The precision cost of n=5 was, at that point,
  unmeasured — the width suite contained only overfit legs.
- **4a-ii: judge majority-of-3 voting.** Have the judge vote three times and take the
  majority, to suppress drift.

**What happened to voting:** reverted the same day. An offline replay A/B measured
**identical over-kill and identical leakage at three times the cost.**

This was the *second* time voting was measured dead — the first was 2026-07-15, before
computed facts existed. `plan.md` records the conclusion bluntly: the leaky verdicts are
wrong for reasons every lens shares, so redundancy cannot fix them. **Do not propose a
third time.**

**Verdict: width kept (with a recorded caveat); voting dead in both regimes.**

---

### C.7 Cycle 5 — the judge's evidence gets structure

**Target:** station 7.

**How the work was scoped — this is the method worth copying.** Rather than guessing at
what the judge gets wrong, every judge verdict on record was collected into one table:
**228 verdicts across five runs**, each with the check that fired, the facts it was
shown, the ruling, and the stated reason (`docs/judge-verdict-inventory-2026-07-26.md`).
That table then became a permanent test fixture — a change to the judge can be replayed
against all 228 recorded cases offline, for the price of the model calls and no VM time
at all. Nearly every judge-side experiment after this point runs against that fixture
first.

The inventory's headline findings:

1. **Misses are lost at the verdict, not at the firing.** Of 23 missed overfit legs
   with judged firings, **22 had every single verdict come back "spurious"**. The checks
   fired. The judge dismissed them.
2. **The wrongest dismissals have a recognisable numeric fingerprint.** A check that is
   completely silent on the buggy program (~0 firings in 20,000 inputs), fires
   deterministically on the failing test's own values (2 out of 2), and then fires on
   ~100% of patched inputs, is about as sharp a discriminator as this pipeline can
   produce. Six verdicts carried exactly that profile, and **4 of the 6 were dismissed**
   on an unsupported "a correct implementation could…" story. Because the profile is
   made of numbers the pipeline already computes, it can be detected in code — which is
   what made this actionable rather than just annoying.
3. **Two of our own facts were miscoaching the judge.** The fire-rate note's "100%
   indicts the check" wording is wrong when the 100% is on the *patched* build with ~0%
   on buggy — that is maximal discrimination, the opposite of indiscriminate. Plus an
   arithmetic bug printing rates like `2997/1000 = 300%`.

**What shipped:**

- **5A — fact repairs.** Two-sided fire-rate wording, per-input denominator fix,
  neutralized trigger-tier wording.
- **Structured citations.** The judge must now emit a `CITATION:` line, and citations
  are verified by literal substring match against what it was actually shown. Our own
  notes carry machine-readable `[fact:…]` tags instead of being re-parsed as prose.
  100% format compliance across two runs.
- **The terminal-marker veto** — a live production catch-killer, found and fixed: the
  gate was matching on notes whose text *denies* the identical claim, including one
  saying the firing convicts the patch.
- **The negated-citation fix** — `'document'` was matching inside "**un**documented",
  and "not contradicted by any shown contract" was being counted as a citation.

**The single most instructive result.** Row 200, a Lang-60 capacity check, was the
mechanism's designed must-flip test — the "lazy compaction" story was supposed to have
nothing to cite. It cited this, and the quote is real:

> `"     * Checks if the string builder contains the specified char."`

The javadoc line exists. The judge used it to argue that the contract specifies only
the search result and says nothing about capacity being preserved. **The dismissal
stands under the rule as written** — the citation is grounded, but it is cited for what
the quoted text does *not* say. A literal substring test cannot distinguish a quote that
supports a claim from a quote cited to prove an absence.

**Verdict: cycle 5 fixed the judge's plumbing and found its own next gap. Three of its
six fixes were the same bug — a rule keyed on text read in the opposite sense. The
structural replacement (machine tags rather than prose matching) is the durable answer.
The "absence argument" gap it discovered is still open and parked.**

---

### C.8 Cycle 6 — persuasion ends, code decides

**Target:** station 7, `judge_decision.py`. The most decisive shift in the campaign.

**The evidence that forced it:** in the chronic-false-accusation triage, **5 of 8 bad
keeps had the clearing fact delivered in the evidence block and were kept anyway**,
several with `CITATION: NONE`. The fact arrived. The judge read past it.

**What shipped, and how it works:**

**1. Machine-readable tags on every fact.** The code that writes a fact already knows
which case it is describing, so it now stamps that in: `[fact:rate-catch-signal]` when
the check is silent on the buggy program and loud on the patched one,
`[fact:rate-indiscriminate]` when it fires broadly on both, `[fact:rate-ambiguous]`
otherwise. Anything downstream reads the tag, never the sentence. This matters because
three separate bugs in cycle 5 came from code trying to recognise a fact by searching its
prose for keywords, and getting the sense backwards each time.

**2. Two decisions moved out of the judge and into code.** Both are narrow:

- If the check is measured firing on essentially every input of the buggy program, a
  ruling of "genuine" is **overridden**, unless one specific question — "is this
  observable part of the bug's own defect family?" — comes back yes. The one escape
  exists because a check aimed squarely at the defect legitimately fires everywhere.
- When the same check fires on both programs, the pipeline now **compares the actual
  reported values before doing anything**. Identical values → drop it, the patch changed
  nothing here. Different values → this is a patch that moved the behavior without fixing
  it, which is a genuine catch and is never dropped. Values not comparable → unknown, and
  again never dropped.

**3. Every gate fails open.** A missing measurement, an unparseable response, or a
network failure returns the judge's original verdict. A broken gate can neither
manufacture an accusation nor erase one. All three share a single answer to the
defect-family question, log a distinct searchable tag, and are skipped entirely for
checks already confirmed to be aimed at this defect.

The whole cycle was validated by 46 offline tests with the model stubbed out — zero
tokens, no VM.

**The diverted-replay fix — an example worth reading in full.** Chart-26's *correct*
patch was convicted by a delivered fact that was simply untrue:

> "the buggy build runs this exact input WITHOUT firing this check — the patch
> INTRODUCED the violation here, and the buggy build is an existence proof"

What actually happened on the buggy build: execution threw inside `axis.draw(...)`, the
harness's own `catch (Exception e) { return; }` swallowed it and returned early, and
the check was never evaluated at all. **"Never ran" was reported as "ran clean."**

The fix makes the early return **observable** instead of guessing about it. A second
source-to-source transform (same module, same safety properties) finds every catch block
whose body just returns without rethrowing, and inserts a counter increment plus a print.
A static counter and a shutdown hook print the same line at exit, so a run that never
took such a path reports **zero** — which is what distinguishes "execution reached the
check" from "we cannot tell". Catch blocks that rethrow, and the harness's own alarm
catches, are never touched.

The claim "the patch introduced this" now requires that counter to be zero. If the
information is missing entirely, the default is the cautious wording, so a caller that
forgets to pass it through cannot accidentally produce the strong claim.

That is the same defect shape as cycle 1's inverted replay fact, and it fails in the
dangerous direction: manufactured evidence against a correct patch.

**Verdict: cycle 6 is where the project stopped trying to convince the judge and started
constraining it. The dismissal side moved. The accusation side did not — see cycle 7.**

---

### C.9 Cycle 7 — three items killed before they shipped

**Target:** mixed. Notable mostly for what it *refused* to build.

**Shipped:**

1. **Extractor fix (i)** — project-defined assertion helpers recognized. This revived a
   dismissal rule that had never once reached its dismissal branch in production.
2. **Fail-loud field access** — fixture fields are read through `field()`, which raises
   on a missing key, instead of `.get()`, which silently returns `None`. Plus renaming
   the confusing `gold` label to `keep-finding` / `dismiss-finding`.
3. **The disputed-computation fact** — where an accusation disputes what a quantity
   should be, put the code's own computation of that quantity verbatim beside the
   firing.
4. **Repair-in-place** — harnesses rejected for mechanically diagnosable defects get
   repaired instead of discarded, with a `harness-repair` marker on every repaired
   harness.
5. **Trace label splits** — five distinct rate states instead of one ambiguous `None`.

**Killed on measurement, not built:**

- **Answer-reuse cache.** The premise was that the judge answers the same question
  repeatedly. Measured across the entire archive — **103 runs, 1,616 judge calls, zero
  byte-identical prompts.** The cache would never hit once. The only place identical
  prompts occur is the stability-measurement tool, which asks the same question N times
  *on purpose* — caching there would erase the measurement and report perfect stability.
- **Silent-case retry.** The premise was "a run that found nothing learned nothing".
  Inverted: silence is the pipeline's most reliable signal — silent correct legs were
  cleared 14 out of 14. The retry would wake 14 clean legs into a 56% accusation lottery
  in order to reach 2 fake ones.
- **Seed shapes** — 7:1 leg exposure against, and the motivating case had 4 alarms
  reviewed already, so it was never starved of firings.

**The result that mattered most.** The disputed-computation fact was aimed at Math-65,
the chronic false accusation. In the pre-pair smoke it was **delivered four times and
ignored.** The accusing verdict still asserted that `getChiSquare()` must equal "the sum
of squared residuals times the supplied weights" — the inverse of what the code does —
with `CITATION: NONE`, while that method's own source sat verbatim beside the firing.

Placement was the hypothesis. It was measured wrong. Combined with the earlier finding
that accusations face no evidence requirement (**90% uncited, versus 6% of
dismissals**), the conclusion was recorded before the pair ran:

> Whatever eventually fixes the accusation side will be **enforcement, not delivery** —
> the same shape cycle 6 already proved on the dismissal side.

**Verdict: the three kills are the cycle's real product. Each cost a few hours of
measurement and saved a build plus a measurement plus the risk of shipping a
regression. This is the cycle where "measure the premise before building" became
routine.**

---

### C.10 The milestone — `final30A` / `final30B` (2026-07-29)

Two identical runs, same commit, same width, queued back to back so no edit could slip
between them. 30 legs each: 14 patches that are secretly wrong, 16 that are genuinely
correct.

```
ROLL A:  TP=9  FN=5  FP=5  TN=11   P=0.64 R=0.64 F1=0.64
ROLL B:  TP=11 FN=3  FP=5  TN=11   P=0.69 R=0.79 F1=0.73
```

Mean F1 **0.685**, against the pre-campaign reference of **0.49**. Tokens: 5,973,680 +
6,038,623.

**What moved: recall. Precision did not — flat at exactly 5 false accusations in both
rolls.** Every point of improvement came from catching more bad patches, none from
accusing fewer good ones. Cycles 5 and 6 were aimed at precision, and cycle 6 did not
move the false-accusation count.

Per-bug, across both rolls:

- **Caught in both rolls — 8 of 14, the reliable core:** Math-2, Lang-41, Lang-50,
  Chart-7, Closure-92, Math-68, Math-74, Math-82.
- **Caught in one roll only — 4:** Lang-60, Closure-38, Lang-63, Math-73.
- **Missed in both — 2:** Chart-19 (a pre-recorded config caveat: its win was
  established at width 7 and this ran at width 5), Math-104 (pre-declared).
- **Correct patches accused in both rolls — 3, the chronic three:** Closure-62,
  Math-30, Math-65.

**Verdict: the campaign's honest number. It also drew the map for cycle 8 — precision
was visibly stuck, and nothing in cycles 5–7 had moved it.**

---

## Era D — Cycle 8 (2026-08-01 → 08-03): mapping the ceiling

Cycle 8 changed the question. Instead of "what fix moves precision", it asked "is the
~5-false-accusation floor a property of the model, the evidence, or the architecture?"
The answer was pursued through cheap offline studies first, each of which produced a
committed table before any interpretation was written.

**How the cycle was ordered, and why that mattered.** Work was sequenced strictly by
cost, cheapest first, with a written check at the end of each stage that had to be read
before the next one started:

1. **Free studies over data already on disk** — no model calls, no VM. These re-rank
   everything below them, so doing them first prevents paying for an experiment that a
   free reading would have made unnecessary.
2. **Small code that protects later measurements** — a check that refuses to launch on a
   mislabeled input file, a field recording where a harness came from.
3. **The one code fix the free studies licensed.**
4. **Paid experiments**, cheapest information per token first.
5. **Design documents** for anything larger, written only after the stages above
   reported, since their content depends on those results.
6. **Builds**, gated on those documents.

That ordering did its job: by the time the expensive model-swap experiment ran, every
cheaper alternative had been measured and found dead, so the experiment was the single
largest open question rather than one option among several.

---

### D.1 The four negative studies — is the ceiling real?

Four independent dimensions were tested, and **all four came back negative**. That
convergence is the cycle's main result.

| study | question | answer |
|---|---|---|
| **8.1** judge-model swap | is the ceiling a quirk of `gpt-5.4`? | **no — architectural** |
| **8.15** authority tier & scope | do genuine catches derive from more trustworthy sources than false accusations? | **no separation** |
| **8.18** dismissal-side authority | are the judge's miss-side dismissals wrong? | **94% are correct** |
| **8.14/b/c** miss ledger | where do misses actually die? | **distributed, no large fixable class** |

**8.1 — the model swap.** Replace the judge model with a newer one, change nothing else,
and re-run all 228 recorded cases from the cycle-5 fixture offline. No VM, no harness
generation — just the judge, twice, on identical questions.

The measurement is **per-case verdict flips**, not totals. Totals move for uninteresting
reasons when you change models; what matters is whether the *same cases* come out
differently.

**The step that made the result trustworthy: measure the noise first.** Before looking at
the new model at all, run the *existing* model twice on the same 228 cases and count how
often it disagrees with itself. Answer: **21 of 228, 9.2%**. That is the floor — any
difference smaller than this is not evidence of anything.

Against that floor, the new model's disagreement rate was 23/228 (10.1%) against one
draw and 32/228 (14.0%) against the other. **At the noise floor.** Without having
established the floor first, 14% would have looked like a real effect.

The flips were not randomly signed: the new model dismisses less, moving recall
81.7%→95.8% and specificity 79.6%→74.5%, with extra keeps splitting about 1:1 between
genuine catches and false alarms. That is **a shifted dismissal threshold, not better
discrimination** — accuracy is unmoved, and a threshold does not require a model swap to
buy. On the pipeline's own bar, +9 accusations would blow the ≤5 cap outright.

Part B replayed the frozen narrow contradiction question and **failed identically** —
same counts, same wrong cases (Closure-62, Math-30). Both models produce plausible
quotes rather than real contradictions.

Cost: ~8.5M tokens, of which ~3M was lost to two harness faults with one shared cause —
work held in memory until the end is work that can be destroyed.

**Pre-registered decision applied: same failure shape → record and close.** The model
question is closed and neither conditional follow-on opens.

**8.14 → 8.14b → 8.14c — the miss ledger, including a retraction.** For every missed
overfit leg across three current-config runs, classify from the trace where it died:
family never invented / died at harness construction / built but never fired / fired but
judge-dismissed.

Result: 14 missed leg-instances — fired-and-all-dismissed 8, built-never-fired 3,
fired-mixed 2, fired-never-judged 1, **died at construction 0**. The dominant miss
station is **the judge: 10 of 14** misses reach a judge and are dismissed.

The follow-up, 8.14b, tried to answer "was the winning check family ever proposed?" by
matching rule names across rolls. It was **retracted the same day**: changing only the
string-matching threshold inverted the finding (5 of 11 never-proposed under one rule,
1 of 11 under another). This produced standing rule 14 — **LLM-assigned names are not
identifiers.** Rule names are generated fresh per roll with no stable vocabulary; any
mechanism keyed on them will read name drift as absence.

8.14c redid it properly: write down each winning check's **asserted property** for every
leg *before* opening any missed roll's proposal list, then adjudicate semantically. The
result: invention is the **minor** station — 1 confirmed never-proposed, at most 4 if
every ambiguous case resolves against, versus the judge class at up to 10. And the
invented-versus-too-weak boundary is **not drawable from the archive at all**, which
re-gated three queued recall levers from "archival evidence" to "live A/B only".

**8.18 — the mirror study.** If the judge is where misses happen, are its dismissals
wrong? 34 dismissals classified against frozen categories: grounded refutation 20,
wrong-family 5, near-tolerance 6, setup divergence 1, **ungrounded hypothetical 2 (6%)**
— below the pre-registered bar.

> The dominant miss **station** is not a dominant miss **cause**. In 94% of dismissals
> the judge is correctly dismissing something that should not convict.

Several of those refutations cite "fires on both builds" — the cycle-6 machinery working
as designed. It also settled Math-104: nine dismissals repeatedly citing the
implementation's own documented `DEFAULT_EPSILON = 10e-9` against the harness's `10e-15`
comparison. **That is a harness defect, not a judge failure** — the check is tighter than
the implementation's own promise.

**The combined picture:** every recall station is now measured and no large addressable
class exists. The binding recall constraint is **check sharpness** at stations 2–4 —
checks that fire but deserve dismissal. And the judge is simultaneously the largest
recall loss (over-dismissal) and the capped precision component (under-dismissal).
Opposite directions, same component, so **no single strictness knob fixes both**.

---

### D.2 8.4 — raw-value recording, and five lessons about plumbing

**Target:** the check-writing instructions at station 4, plus one dismissal test at
station 7 in `run.py`.

**Background — what that dismissal test does.** When a check fires, one of the questions
the pipeline asks before believing the accusation is: *does the value this check reported
match a value the failing test itself expects?* If it does, the check has merely rebuilt
the test's own scenario and has found nothing new, so the accusation is dropped.

**The failure mode.** About 17% of generated checks normalize text before comparing —
collapsing runs of whitespace, for instance. For those checks that question can never be
answered yes, because a normalized value can never equal the test's raw literal. The test
was **structurally dead** for exactly the checks it most needed to cover, and that is why
Closure-62 could never be reached.

**The general rule being implemented:** a transformation performed for comparison must
not destroy the original. Compare normalized, **record raw**.

**Why this item is worth reading in detail:** it is small, obviously correct, and it took
five separate corrections to actually work — each one an instance of the same defect
family.

1. **The prompt half shipped.** Fired messages now carry named keys
   `expectedNormalized= actualNormalized= expectedRaw= actualRaw=`. Raw keys emit **only
   when normalization happened**, so absence means "no normalization occurred", never
   "forgot to record".
2. **The consumer didn't exist.** Testing the code that was supposed to read the new
   values revealed it is **numeric-only** — and normalizing checks are by definition over strings. Extending
   the extractor alone would have shipped a no-op. A new raw-versus-pinned comparison had
   to be built.
3. **A "38% compliance" figure was struck the same day.** It was a truncation artifact:
   the count read the alarm's *record* rather than the alarm, and any alarm whose raw
   keys fell past the truncation point scored as non-compliant. Measured at source,
   compliance is **46 of 46**.
4. **The lint was built and unwired.** A detector nothing calls guards nothing. Wired
   into acceptance as gate 0c2, with a load-bearing rejection message: it says *keep the
   comparison normalized and add the raw values*, because a model reading "record raw" as
   "compare raw" would trade a dead dismissal test for formatting false positives.
5. **The batch smoke failed, twice, for two different reasons.** First diagnosis: a
   200-character headline cap strips the raw keys, which sit last. That was real but
   **secondary**. The primary cause was an **embedded newline** — the capture regex stops
   at the first newline, and the raw expected output of exactly the checks 8.4 exists for
   is multi-line by nature. The target population and the plumbing's single-line
   assumption collided. Fixed by escaping newlines at emission so the alarm stays one
   line, plus splitting the consumer so the comparison gets the uncapped text while
   prompts keep the cap.

Second trial run: **3 of 3**, up from 1 of 4. The dead test is alive — the note now
reads "the fired value differs from every value the test itself pins", a real
comparison, and the previously-only-reachable branch ("no numeric value could be
compared") appears zero times.

**One real find along the way.** The comparison caught a **latent false dismissal**: two
alarms moved from "matches" to "differs" because the numeric comparator's rounding floor
had called a message's `-0.0` a match for the `0` inside a pinned `x- -0`, while the raw
strings differ by exactly Closure-38's separator-space defect. A coincidence of digits
stood ready to void a genuine catch. This closes cycle 1's very first recorded residual,
observed live for the first time.

**Verdict: worked, after five corrections. It revived a check that could never fire. It promises no
verdict movement, and the pair confirmed that — the dismiss branch matched zero firings
across 60 legs, and the differs branch fired 5 times, all on Closure-62, a bug that was
already a residual.**

---

### D.3 The supporting cast — 8.7, 8.8, 8.3, 8.2, 8.20

**8.7 repair provenance (station 5).** Record on each accepted harness whether it came
from a repaired attempt. Motivation: attributing a new accusation to harness repair had
required attempt-tag archaeology. **Outcome:** shipped, and the pair revealed something
large — **73 of 299 accepted harnesses (24%) came from a repaired attempt.** A
previously invisible dependency, and the strongest positive read of the pair.

**8.8 suite-file label assert (station 0).** Refuse to launch when a case's label flag
disagrees with the pinned task set. Motivation: a mislabeled case file had happened once.
**Outcome:** shipped, refusing on both mismatch directions. Its *placement* is the
lesson — the first attempt refused only after creating run state; the second ran before
the case file was even loaded and silently passed everything.

**8.3 buggy-value collector (station 6).** Record the actual observed *values* on
buggy-side replays, not just "fired / didn't fire". Motivation: **0 of 1,452** recorded
buggy-side steps carried a value, which made two proposed mechanisms untestable.
**Outcome:** shipped on both replay paths. Yield on the repaired test data: 127 of 220
rows carry at least one value (58%). A deliberately string-preserving extractor adds 42
rows over a numeric-only one — recording is not judging, and a value the numeric
comparator cannot parse still matters to other consumers.

Its step 1 earned its keep on day one: before writing any new code, the function 8.3
depends on was checked, and it carried **both** cutters — the 200-character cap *and* the
first-newline stop — each demonstrated to lose the raw value entirely. Building the
dependent items first would have fed them truncated values.

The same sweep found a **fourth instance** of the same defect, in the offline measurement
test data itself: **78 of 228 rows** of the standing fixture end in an ellipsis at
exactly 201 characters. Every offline measurement over message *content* had been reading
prefixes. Repaired non-destructively into a separate file, with 8 rows permanently
truncated and flagged rather than silently kept.

The consequences were checked rather than assumed: truncation is confined to one field;
the evidence and code-context fields are whole; the "delivered and ignored" claims rest on
those, so they stand. But 36 of the 78 clipped headlines sit on the 7 decisive bugs, and
8.1's absolute figures now carry a caveat noting that 34% of rows were judged with a
201-character headline prefix (with full evidence alongside).

**8.2 reimplementation-as-evidence (new evidence generator, station 6).** For a disputed
quantity, generate an independent implementation **from the documentation** — never from
the patched source — and run it on the same inputs. The whole design is the **authority
screen**: validate that reference against the *buggy* build on observables the defect does
not touch. Disagreement there → discard the reference and emit nothing.

External prior art supports the class: Differential Prompting (ASE 2023) found
failure-inducing inputs at 75% versus 28.8% for direct prompting using a reference
synthesized from inferred intent rather than from the code under test — and their reason
is ours (a reference derived from the code inherits its bug).

**Outcome:** the pure core is built and fails closed everywhere. Reach was **measured, and
it collapsed the estimate**: 54 of 220 rows trigger (25%), 22 have at least one comparable
observable (10%), 19 clear the screen's three-observable minimum — an **8.6% ceiling**,
and an upper bound twice over. The expensive half is **held** pending live data.

**8.20 authority-scope fact — closed negative, and the entry is worth reading.** The idea:
compute whether a firing's actual parameters sit inside the scenario the cited test pins.
It died twice over. First, its own motivating case contradicted it — the Math-39 harness
reproduces the test's setup *exactly*, so a scope fact would score it in-scope and never
fire. Second, run mechanically on real values across 85 findings: **80% undetermined**
(no test literal or no recorded value — a property of the mechanism, capping its reach at
about 20% of firings), and on the 17 determined cases the separation ran the right
direction but at 4 counts versus 3, p = 0.162.

The residual "maybe reword the note" was disposed of by standing rule 12 — judge-prompt
wording iteration is measured dead; judge changes are structural or nothing.

---

### D.4 The cycle-8 pair — a recorded failure

Pre-registered bar, written before launch:

> **PASS = paired mean F1 > 0.685 AND ≤5 accusations per roll AND zero accusations on
> historically clean legs.**

Result, raw output committed before scoring:

```
CYCLE-8 A  TP= 9 FN= 5 FP= 7 TN= 9   F1=0.60   accusations=7
CYCLE-8 B  TP=10 FN= 4 FP= 5 TN=11   F1=0.69   accusations=5
PAIRED MEAN 0.6448  vs bar >0.685 (July pair: 0.6881)
```

**Failed on all three criteria.** Roll A had 7 accusations. Two historically clean legs
were accused — Math-2 in both rolls (so not noise, per the two-roll rule) and Math-86 in
one.

The honest statistical sentence was recorded alongside: **the within-pair spread (0.09 in
both pairs) exceeds the between-pair difference (0.043)** — neither regression nor
improvement is established. But the bar was a pre-committed threshold, not a significance
test, and it was missed. Official state remains the July pair.

**The other five pre-named reads, none of which implicates the batch:**

| read | result |
|---|---|
| 8.4 dismiss branch | zero matches across 60 legs — no catch voided |
| 8.4 differs branch | 5 firings, all Closure-62, already a residual |
| repair provenance | **73 of 299 accepted harnesses (24%) were repaired** |
| 8.3 value channel | 224 events, 72 carrying values (baseline: 0 of 1,452) |
| 8.2 reach recovery | **unresolvable** — the pre-registered denominator is not computable because `result.jsonl` carries no code context |

That last row is itself a result. Rather than substitute a denominator the pre-registration
did not name, the question was left open and the missing field filed as a small fix.

**Verdict: the batch is honest, well-instrumented, and moved no score. The forensic
thread is open — the leading suspect for Math-2's two-roll accusation is 8.1's own
sentinel fix, which changed what an unparseable judge response becomes in one gate, in the
direction that spares accusations. One grep decides it.**

---

# Part 6 — Master summary

## Worked, and is still in the pipeline

| change | station | one-line reason it worked |
|---|---|---|
| Dataset audit + pinned tasks + label certification | 0 | made every downstream number mean something |
| Relation synthesis | 2 | the recall engine; nothing replaces it |
| Buggy-build screening | 3 | the only structural safety filter |
| Rule compile-repair | 3 | rescues ~22% of candidates for free |
| **Replay rules directly on the patched build** | 6 | the single biggest recall win |
| Show the whole test and the real failure message | 2, 4 | grounding beats paraphrase |
| Documented-`@throws` tripwires (with isolation rule) | 2 | new bug class covered |
| Seed patched fuzz with buggy firing inputs | 6 | less fuzz luck |
| Pure, unit-testable evidence-fact module | 6→7 | three wrong-fact bugs had shipped from inline code |
| Error ≠ clean in replay results | 6 | stopped manufacturing evidence against correct patches |
| Muted per-check replay | 6 | computed a fact that was previously uncomputable; cleared two chronic false accusations |
| Value comparison before claiming "identical" | 6 | protects partial-fix catches |
| Fire-rate facts, two-sided wording | 3→7 | numbers that indict broken checks now reach the judge correctly |
| Same facts whichever route a check arrived by | 7 | proven by a controlled experiment inside a single leg |
| Boolean-swallow lint | 3, 5 | preserves the exception identity downstream guards need |
| Structured citations + machine fact tags | 7 | replaced prose keyword-matching, which failed three times |
| Diverted-replay detection | 6 | "never ran" was being reported as "ran clean" |
| Cycle-6 enforcement gates | 7 | decisions the judge kept ignoring became code |
| Repair-in-place for rejected harnesses | 5 | 24% of accepted harnesses turn out to come from it |
| Raw-value recording (8.4) | 4→7 | revived a dismissal test that could never fire |
| Repair provenance marker (8.7) | 5 | turned three greps into one field lookup |
| Suite-file label assert (8.8) | 0 | refuses to launch on a mislabeled case file |
| Buggy-side value channel (8.3) | 6 | 0 of 1,452 recorded values → 58% of rows |
| One-file `trace.md` per run | all | the run log is deleted on success; print-only diagnostics were invisible by design |

## Tried and rejected

| change | station | why it died |
|---|---|---|
| **Pooling of rules/harnesses/oracles**, any form | 2, 6 | user rule — farms the benchmark; a leg must earn its verdict from its own bug |
| Metamorphic family menu (62–84 families) | 2 | covered 18% of what free invention already produces |
| `_STAT_PATTERNS` name whitelist | 4 | dataset-shaped; would transfer to nothing |
| The attribution judge | 7 | 0 verdicts changed, ~100% of sound catches vetoed, asks the wrong question |
| Judge majority voting | 7 | **dead twice** — identical error at 3× cost, with and without computed facts |
| Mechanical auto-dismissal of latent firings | 7 | killed a genuine catch |
| Same-name dismissal reconciliation | 7 | generic names label different checks in different harnesses; killed a genuine catch |
| Asking the prompt to "explore beyond the seed input" | 4 | tried twice, 3 false alarms each time, instruction ignored |
| Voting across a bug's several patches | 6 | repair tools make the same mistake in all their patches |
| Coverage-guided differential fuzzing for certification | — | every wrong "no difference" came from looking at the wrong output, never from missing the right input |
| Raw-string comparison of text output | 4 | fires on formatting deltas and buries the real content difference |
| Blanket increase of harnesses per leg | 4 | every extra harness is a false-alarm lottery ticket on the correct sibling |
| Universal screening v1 (extract check bodies) | 3 | extracted fragments don't compile; safe and inert |
| Answer-reuse cache | 7 | 0 byte-identical prompts in 1,616 calls |
| Silent-case retry | 4 | silence is the most reliable signal; would wake 14 clean legs to reach 2 fake ones |
| Structural seed shapes | 6 | 7:1 leg exposure against |
| Judge-model swap | 7 | flip rate at the same-model noise floor; a threshold shift, not better judgment |
| Authority tier and scope as a precision gate | 7 | does not separate; the largest gap runs the wrong way |
| Authority-scope computed fact (8.20) | 6→7 | 80% undetermined; premise contradicted by its own motivating case |
| Judge-prompt wording iteration | 7 | measured dead repeatedly — standing rule 12 |
| Spending effort on the unwinnable list | — | proven behaviorally identical to the real fix, or mislabeled |

## Undecided, parked, or open

| item | status |
|---|---|
| **Focused per-source synthesis** | kill verdict **void** (single roll); best recall evidence on record (R 0.89); never re-measured under the paired rule |
| 8.2 reimplementation-as-evidence | core built; expensive half held pending live reach data (measured ceiling 8.6%) |
| 8.9 family persistence | design doc only; gated on a live A/B against a 1–4 leg class |
| 8.16 passing-suite behavior delta | design doc only; published as PATCH-SIM, moderate power; must be a fact, never a gate |
| 8.17 deterministic invariant mining | design doc only; same gate; must define family identity semantically, never by name |
| 8.19 buggy-anchored check generation | the unowned big one — feed observed buggy-side values into generation so checks cite authority #2 rather than model memory |
| The absence-argument gap | a grounded citation used to prove what the text does *not* say; parked since cycle 5 |
| Math-104 tolerance floor | parked with a firewall warning — the adjudicating number came from the developer fix |
| Crashing-bug path | **unmeasured since 2026-07-16.** Skipped by user decision. Every claim in this document covers semantic bugs only |
| Chart-19's missed-twice → caught-twice flip | honestly open; two causes eliminated, composition and variance remain |
| fresh12 (12 unseen dev bugs) | **locked**, launches only on the user's literal phrase |
| The 27-bug holdout | sealed, never touched |

---

# Part 7 — The methodology that came out of it

The pipeline changes above are half the output. The other half is a set of rules about
how to work on a system like this, most of them bought with a wasted day or a wasted
measurement. These generalize past this project.

**1. A mechanism beats an instruction, everywhere.** An idea only counts if its check
reaches the target build mechanically. Prompt advice is multiplied by the model's
implementation rate and the fuzzer's luck, and that product is small.

**2. The judge needs computed facts, not exhortations.** Every false-alarm class was
fixed by computing a fact and putting it in the evidence. None was fixed by asking for
more care.

**3. Never auto-dismiss on an ambiguous signal.** Both mechanical auto-dismissals tried
killed a true catch and had to be narrowed to compute-the-fact-and-let-the-judge-decide.

**4. Change one thing per measurement point.** Violating this once cost a full day of
forensics.

**5. Single-roll comparisons are worthless here.** Identical code on identical legs gave
recall 0.29 and 0.57. Anything smaller than that gap, measured once, is noise.

**6. Two consecutive rolls before any "fixed" claim,** with the mechanism visible in the
trace — not just the flipped verdict.

**7. Guard population before mechanism.** Two frozen fixtures now exist and are
population-pinned: 67 genuine catches (guards any dismiss-pushing change) and 38 correct
dismissals on correct patches (guards any keep-pushing change). The second had to exist
because the judge turned out to fail in *both* directions — so a fix for one direction is
presumed to damage the other until measured.

**8. Assert a guard's inputs exist, not just its outputs.** A guard evaluated before, or
without, its inputs is fail-open *while looking armed* — it reports success **because** it
guarded nothing. Seven measured instances share the shape: a question asked 141 times with
an empty input block; a marker matching against prompt template text; a label check placed
before the file it checks was loaded; a test stub whose signature mismatch made every call
throw into the fail-open path while three tests passed; a corpus study that would have
reported "0 regressions" over zero files. All die to the same one-line check.

The seventh instance sharpened it: **inputs must not merely exist — they must arrive.**
8.4's raw keys were emitted, recorded, and stripped in transit. Every piece was verified
in isolation; the journey never was. Existence is a property of the producer. Arrival is a
property of the journey. Only an end-to-end run tests the journey.

**9. Record raw, compare processed.** A transformation performed for comparison must not
destroy the original. This applies to measurement inputs too: a truncated record is a
display transform, and measuring over it silently deflates counts. Tools reading records
must refuse to answer on truncation rather than undercount. This principle failed **five
times** in this project — four inflating counts, one corrupting a diagnosis.

**10. Raw results committed before interpretation.** Verbatim numbers pasted from the
tool. Populations verified against fixtures, never narrated from memory. Three test-count
drifts came from computing off remembered baselines.

**11. Pre-register the read-out.** For every paid experiment, write down what will be
measured and what each outcome licenses *before* launching. 8.1's decision table was
written first, so its negative result closed a question instead of starting an argument.

**12. Measure the premise before building.** Three cycle-7 items died this way for the
cost of a few hours each.

**13. Record which kind of death.** *Premise false* items never come back. *Premise true,
price fatal* items can return if the price changes. Recording the distinction stops the
same idea being re-argued from scratch.

**14. LLM-assigned names are not identifiers.** Rule and check names are regenerated per
roll with no stable vocabulary. Any mechanism keyed on them reads name drift as absence.
A finding built on name matching was retracted the day it was made.

**15. When a measurement disagrees with the code, suspect the measurement's own
scaffolding first.** Every wrong reading in cycle 7 came from a proxy we built — a stub, a
regex, a field name — never from the code under test.

**16. A zero-delta criterion needs a baseline differing by exactly the change under
test.** Otherwise it measures accumulated drift and reports it as a regression. The mirror
of rule 8: there a check passes while testing nothing; here one fails while testing
something else.

---

# Part 8 — Where it stands

**The number:** paired mean F1 **0.685** (July), against a pre-campaign reference of
**0.49**. The August pair scored 0.6448 and did not replace it.

**Precision appears to have a floor of about 5 false accusations** on the 16 correct
patches in the 30-leg set, under the current design. That is not a guess — four
independent attempts to find something separating genuine catches from false accusations
all came back empty: the delivery-side features of the evidence do not separate them;
asking the judge a narrow structured question destroys 22% of genuine catches while
catching little; the trustworthiness of the source a check derives from does not
separate them; and swapping in a different judge model does not move it. Contradicting
source code placed verbatim beside the accusation is read and ignored. 90% of
accusations cite nothing, versus 6%
of dismissals.

**The chronic residuals** — Closure-62, Math-30, Math-65, Math-39 — are one shape:
accusations that no delivered fact dislodges. Math-39 is the hardest variant, because the
authority it invokes is *real* (a test genuinely pins the property) and merely does not
extend to the firing input.

**Recall is at 8 of 14 stable across rolls,** and every station is now measured. There is
no large addressable class left. Misses are distributed across small causes, and the
binding constraint is check **sharpness** at stations 2–4 — checks that fire but deserve
dismissal. That boundary is not drawable from archived data, so the remaining recall
levers have to be settled by live A/B experiments priced against a class of 1–4 legs.

**The structural bind, stated plainly:** the judge is simultaneously the station where
most misses become visible (over-dismissal) and the capped precision component
(under-dismissal). Opposite directions in one component. No strictness knob fixes both.
Any real fix has to be evidence-shaped or architectural.

**The one direction with a live case** is 8.19 — buggy-anchored check generation. The
argument: the recall side's disease and the precision side's disease are the same disease.
Checks that fire and deserve dismissal assert contracts from model *memory*; accusations
that cannot be dislodged assert contracts from model *memory*. The only two-roll-confirmed
mechanism catch in the project (Chart-19) came from structure derived from data, and 8.19
is that generalized: feed observed buggy-side values into generation so that checks are
born citing authority #2 — the buggy build's own behavior away from the defect — instead
of what a model remembers a javadoc says.

**What is deliberately not claimed.** Everything above concerns **semantic bugs**. The
crashing-bug path has not been measured since 2026-07-16, which predates all eight cycles,
and the rerun was skipped by user decision. Shared components were reworked under those
cycles with semantic-only validation data. One confirmed exposure is on record: crash legs
reach gates where the polarity inverts, and crash-catch safety rests on one judge answer
rather than a guarantee.
