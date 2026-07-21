> **AUTHORITATIVE machine-readable label truth: `suites/labels/*.jsonl`** (verified_correct / verified_incorrect / excluded, one row per bug-tool-patch, with `kind` for semantic-vs-crashing). The tables/writeups below are the human-readable EVIDENCE those rows point to — if they ever disagree, the jsonl wins.

# drr Java dataset — full inventory, audit coverage, and verdicts

> **PINNING: every verdict in this document is for the `patch1` file of
> its (bug, tool) pair** — the first patch in sorted order, the same file
> the suites resolve via `head -1` (sole exception: Closure-86, certified
> on patch2 because Doverfitting/SequenceR has no patch1). Sibling patch
> files (§6) are NOT covered by these verdicts and can differ — verdicts
> are per FILE, not per bug. Since 2026-07-16 pinning is enforced in every
> code path (`run_suite.sh` and `PatchSelector`), so future runs evaluate
> exactly the files audited here unless a sibling is named explicitly.

> **UNIT OF EVALUATION.** The TASK is per bug: (bug, candidate patch) vs
> that bug's ground truth (dev fix + trigger tests, shared by all tools'
> patches). The APR **tool is provenance, not task identity** — the
> pipeline never uses it; it only namespaces files (patch numbering
> restarts per tool). Accordingly: the unit of JUDGMENT is the patch
> file; the unit of SCORING is the bug (one confusion-matrix row per bug
> per label; recall denominator = bugs with ≥1 certified-detectable
> overfit patch); and the certification sweeps selected **one
> representative file per (bug, label)** — the sorted-first (tool,
> patch1) across all tools — NOT one per (bug, tool). No bug is counted
> twice because two tools attacked it. (Lang-41 is the cautionary tale
> for taking the tool axis seriously: Arja and SimFix emitted
> byte-identical files that drr labeled oppositely.)

Status as of 2026-07-16 (evening). Environment for all behavioral verdicts:
OpenJDK 11.0.31, Hetzner VM. Records: hetzner
`scratch/eval_expansion/{certified_known,certified_suite,certified_suite_v2,b3_certified_overfit,b3_mislabel_correct,b1_mislabel}.jsonl`;
deep-dive witnesses `/tmp/wit/`, `/tmp/d4j/witness57/`,
`/tmp/d4j/Lang_50_fullpatch_check`. Per-case detail: `suites/labels/incorrect_labels.md`.

---

## 1. Dataset inventory (everything in `drr/Patches/`)

| Category | Patch files | Distinct bugs | Meaning |
|---|---|---|---|
| **Doverfitting** | 381 | 77 | human-labeled overfitting (plausible-but-wrong) APR patches |
| **Dcorrect** | 257 | 91 | human-labeled correct APR patches |
| **Dunassessed** | 625 | 135 | no ground-truth label (Nopol2017, Cardumen, …) — unusable for eval scoring as-is |
| Total | 1,263 | — | |

The labeled set (Doverfitting ∪ Dcorrect) covers **117 distinct bugs**,
which split by bug kind:

| Bug kind | Bugs | Overfit files | Correct files | Determined by |
|---|---|---|---|---|
| **Semantic** (assertion-failing, non-crashing) | 74 | 228 | 164 | `eval_candidates.py` (trigger-test throwable ∈ JUnit assertion types) |
| **Crashing / other** (exception-throwing trigger, or not enumerable) | 43 | 153 | 93 | complement |

Note "files ≫ bugs": one bug attracts many patches (Math-80 has 31
overfitting files; Chart-1 has 16). **Verdicts are per patch FILE, not per
bug** — proven by Closure-86, where patch2 ≡ dev fix but patch3/patch5 are
not equivalent.

## 2. The semantic pool, partitioned (74 bugs)

| Partition | Bugs | Members |
|---|---|---|
| **Paired** (≥1 overfit AND ≥1 correct patch — usable for precision+recall) | 33 | Chart-1,3,7,12,19,26 · Closure-18,33,62,63,73,86,115 · Lang-7,22,41,50,55,60 · Math-2,5,30,33,50,53,57,59,63,71,73,80,82 · Time-4 |
| **Overfit-only** (recall-only; no correct partner) | 15 | Chart-15,25 · Closure-38,92,93,123 · Lang-63 · Math-6,20,56,68,74,88,104 · Time-11 |
| **Correct-only** (precision-only; no overfit partner) | 26 | Chart-8,11,20 · Closure-14,31,57,70,126 · Lang-10,21,24,26,38 · Math-22,25,34,35,39,41,65,75,86,93,99 · Time-15,19 |

## 3. What has been checked (the audit), and the verdicts

Coverage: **SEMANTIC POOL ONLY** (§2, 74 bugs). Every bug IN THE SEMANTIC
POOL has a verdict (or an explicit pending/excluded status) for one pinned
patch file per side. **Crashing bugs (§7, 43 bugs) are a SEPARATE pool and
are mostly UNCERTIFIED — they are NOT covered by the tables here.** The
tables below are GENERATED from `pinned_tasks.jsonl` (semantic) — the
dataset is the source of truth; paired/unpaired is a scoring property (§2),
not a verdict category, so all semantic bugs appear in the same tables.
Sibling patch files
remain uncovered (§6).

### 3a. Overfit side — one pinned file per bug (52 bugs)

| Bug | Kind | Pinned file (rank) | Verdict | Source | Strong div | Notes |
|---|---|---|---|---|---|---|
| Math-68 | seman | patch1-Math-68-Arja-plausible (1) | detectable | probe | 2679 |  |
| Time-4 | seman | patch1-Time-4-Arja-plausible (1) | detectable | probe | 1764 |  |
| Math-71 | seman | patch1-Math-71-Arja-plausible (1) | detectable | probe | 562 |  |
| Chart-1 | seman | patch1-Chart-1-Arja-plausible (1) | detectable | probe | 472 |  |
| Chart-15 | seman | patch1-Chart-15-Arja-plausible (1) | detectable | probe | 404 |  |
| Chart-5 | crash | patch1-Chart-5-DeepRepair (1) | detectable | probe | 396 |  |
| Math-88 | seman | patch1-Math-88-SimFix-plausible (1) | detectable | probe | 254 |  |
| Chart-12 | seman | patch1-Chart-12-Arja-plausible (1) | detectable | probe | 242 |  |
| Math-74 | seman | patch1-Math-74-Arja-plausible (1) | detectable | probe | 231 |  |
| Lang-50 | seman | patch1-Lang-50-Arja-plausible (1) | detectable | probe | 225 |  |
| Lang-63 | seman | patch1-Lang-63-Arja-plausible (1) | detectable | probe | 216 |  |
| Closure-18 | seman | patch1-Closure-18-SequenceR (1) | detectable | probe | 210 |  |
| Math-56 | seman | patch1-Math-56-Arja-plausible (1) | detectable | probe | 202 |  |
| Math-6 | seman | patch1-Math-6-Arja-plausible (1) | detectable | probe | 181 |  |
| Math-104 | seman | patch1-Math-104-Elixir-plausible (1) | detectable | probe | 160 |  |
| Chart-25 | seman | patch2-Chart-25-Arja-plausible (1) | detectable | probe | 150 |  |
| Math-73 | seman | patch1-Math-73-ACS-plausible (1) | detectable | probe | 134 |  |
| Chart-3 | seman | patch1-Chart-3-Elixir-plausible (1) | detectable | probe | 121 |  |
| Math-20 | seman | patch1-Math-20-Arja-plausible (1) | detectable | probe | 120 |  |
| Math-2 | seman | patch1-Math-2-Arja-plausible (1) | detectable | probe | 117 |  |
| Math-50 | seman | patch1-Math-50-HDRepair (1) | detectable | probe | 81 |  |
| Math-5 | seman | patch1-Math-5-CapGen-plausible (1) | detectable | probe | 79 |  |
| Math-82 | seman | patch1-Math-82-HDRepair (1) | detectable | probe | 67 |  |
| Lang-55 | seman | patch1-Lang-55-Arja-plausible (1) | detectable | probe | 65 |  |
| Closure-86 | seman | patch3-Closure-86-SequenceR (2) | detectable | probe | 55 |  |
| Math-63 | seman | patch2-Math-63-CapGen-plausible (2) | detectable | probe | 46 |  |
| Lang-43 | crash | patch1-Lang-43-Arja-plausible (1) | detectable | probe | 39 |  |
| Closure-33 | seman | patch1-Closure-33-Jaid-plausible (1) | detectable | probe | 38 |  |
| Lang-27 | crash | patch1-Lang-27-DeepRepair (1) | detectable | probe | 30 |  |
| Math-70 | crash | patch2-Math-70-SketchFix-plausible (2) | detectable | probe | 27 |  |
| Closure-38 | seman | patch1-Closure-38-SequenceR (1) | detectable | probe | 18 |  |
| Math-33 | seman | patch1-Math-33-SketchFix-plausible (1) | detectable | probe | 16 |  |
| Chart-19 | seman | patch1-Chart-19-Arja-plausible (1) | detectable | probe | 14 |  |
| Chart-26 | seman | patch1-Chart-26-Jaid-plausible (1) | detectable | probe | 10 |  |
| Closure-73 | seman | patch1-Closure-73-SequenceR (1) | detectable | probe | 7 |  |
| Math-80 | seman | patch1-Math-80-Arja-plausible (1) | detectable | probe | 4 |  |
| Math-53 | seman | patch1-Math-53-DeepRepair (1) | detectable | probe | 3 |  |
| Chart-7 | seman | patch1-Chart-7-Arja-plausible (1) | detectable | witness+deep-dive | 0 |  |
| Closure-62 | seman | patch1-Closure-62-Jaid-plausible (1) | detectable | witness+deep-dive | 0 |  |
| Closure-92 | seman | patch1-Closure-92-SequenceR (1) | detectable | witness+deep-dive | 0 | witness: indexOf char-widening trap; divergence on multi-module compilation surface |
| Lang-41 | seman | patch1-Lang-41-Arja-plausible (1) | detectable | witness+deep-dive | 0 |  |
| Lang-60 | seman | patch1-Lang-60-Arja-plausible (1) | detectable | witness+deep-dive | 0 |  |
| Math-57 | seman | patch1-Math-57-ssFix-plausible (1) | detectable | witness+deep-dive | 0 |  |
| Time-11 | seman | patch1-Time-11-Arja-plausible (1) | detectable | witness+deep-dive | 0 | witness: broken ThreadLocal remains; verbose() NPEs cross-thread |
| Closure-63 | — | — | **EXCLUDED** | — | — | bug deprecated in installed Defects4J |
| Closure-93 | — | — | **EXCLUDED** | — | — | bug deprecated in installed Defects4J |
| Closure-115 | — | — | **UNPINNABLE** | — | — | single overfit file equivalent to dev fix (label error) tried: patch1-ssFix-plausible |
| Closure-123 | — | — | **UNPINNABLE** | — | — | sole file; divergence is redundant-parens formatting only - no sound oracle may  tried: patch1-SequenceR |
| Lang-7 | — | — | **UNPINNABLE** | — | — | patch1 env-ceiling, patch2 also 0-strong; 5 untried siblings, same-block Arja va tried: patch1-Arja-plausible,patch2-Arja-plausible |
| Lang-22 | — | — | **UNPINNABLE** | — | — | patch1+patch2 both equivalent to dev fix; 2 untried siblings tried: patch1-Arja-plausible,patch2-Arja-plausible |
| Math-30 | — | — | **UNPINNABLE** | — | — | single overfit file equivalent to dev fix tried: patch1-ssFix-plausible |
| Math-59 | — | — | **UNPINNABLE** | — | — | single overfit file IS the dev fix (label error) tried: patch1-SequenceR |

### 3b. Correct side — one pinned file per bug (60 bugs)

| Bug | Kind | Pinned file (rank) | Verdict | Source | Strong div | Notes |
|---|---|---|---|---|---|---|
| Lang-27 | crash | patch1-Lang-27-SimFix (1) | label_stands | probe | 631 | 631 div all exception_generic_latent (B1 refined classifier) - label stands |
| Lang-50 | seman | patch1-Lang-50-SimFix (1) | label_stands | probe | 43 | 43-div record was an applier artifact (out-of-order hunks); fully applied = 0/518. P0.1 applier fix landed 2026-07-17, build+trigger verified — UNBLOCKED |
| Chart-1 | seman | patch1-Chart-1-CapGen (1) | label_stands | probe | 0 |  |
| Chart-3 | seman | patch1-Chart-3-Arja (1) | label_stands | probe | 0 |  |
| Chart-7 | seman | patch1-Chart-7-SimFix (1) | label_stands | probe | 0 |  |
| Chart-8 | seman | patch1-Chart-8-CapGen (1) | label_stands | probe | 0 |  |
| Chart-11 | seman | patch1-Chart-11-CapGen (1) | label_stands | probe | 0 |  |
| Chart-12 | seman | patch1-Chart-12-Arja (1) | label_stands | probe | 0 |  |
| Chart-19 | seman | patch1-Chart-19-ACS (1) | label_stands | probe | 0 |  |
| Chart-20 | seman | patch1-Chart-20-SimFix (1) | label_stands | probe | 0 |  |
| Chart-26 | seman | patch1-Chart-26-Jaid (1) | label_stands | probe | 0 |  |
| Closure-14 | seman | patch1-Closure-14-SimFix (1) | label_stands | probe | 0 |  |
| Closure-18 | seman | patch1-Closure-18-Jaid (1) | label_stands | probe | 0 |  |
| Closure-31 | seman | patch1-Closure-31-Jaid (1) | label_stands | probe | 0 |  |
| Closure-33 | seman | patch1-Closure-33-Jaid (1) | label_stands | probe | 0 |  |
| Closure-57 | seman | patch1-Closure-57-SimFix (1) | label_stands | probe | 0 |  |
| Closure-62 | seman | patch1-Closure-62-Jaid (1) | label_stands | probe | 0 |  |
| Closure-70 | seman | patch1-Closure-70-Jaid (1) | label_stands | probe | 0 | resolved 2026-07-17 (final3 retry; earlier probe compile-fails were probe-side) |
| Closure-73 | seman | patch1-Closure-73-Jaid (1) | label_stands | probe | 0 |  |
| Closure-86 | seman | patch1-Closure-86-SequenceR (1) | label_stands | probe | 0 |  |
| Closure-115 | seman | patch1-Closure-115-SimFix (1) | label_stands | probe | 0 |  |
| Closure-126 | seman | patch1-Closure-126-Jaid (1) | label_stands | probe | 0 |  |
| Lang-7 | seman | patch1-Lang-7-ACS (1) | label_stands | probe | 0 |  |
| Lang-21 | seman | patch1-Lang-21-ssFix (1) | label_stands | probe | 0 |  |
| Lang-22 | seman | patch1-Lang-22-DeepRepair (1) | label_stands | probe | 0 |  |
| Lang-24 | seman | patch1-Lang-24-ACS (1) | label_stands | probe | 0 |  |
| Lang-26 | seman | patch1-Lang-26-CapGen (1) | label_stands | probe | 0 |  |
| Lang-38 | seman | patch1-Lang-38-Elixir (1) | label_stands | probe | 0 |  |
| Lang-55 | seman | patch1-Lang-55-Jaid (1) | label_stands | probe | 0 |  |
| Lang-60 | seman | patch1-Lang-60-SimFix (1) | label_stands | probe | 0 |  |
| Math-2 | seman | patch1-Math-2-SOFix (1) | label_stands | probe | 0 | patch file was repaired 2026-07-16 (was reversed+truncated) |
| Math-5 | seman | patch1-Math-5-ACS (1) | label_stands | probe | 0 |  |
| Math-22 | seman | patch1-Math-22-Arja (1) | label_stands | probe | 0 |  |
| Math-25 | seman | patch1-Math-25-ACS (1) | label_stands | probe | 0 |  |
| Math-30 | seman | patch1-Math-30-CapGen (1) | label_stands | probe | 0 |  |
| Math-33 | seman | patch1-Math-33-CapGen (1) | label_stands | probe | 0 |  |
| Math-34 | seman | patch1-Math-34-Elixir (1) | label_stands | probe | 0 |  |
| Math-35 | seman | patch1-Math-35-ACS (1) | label_stands | probe | 0 |  |
| Math-39 | seman | patch1-Math-39-Arja (1) | label_stands | probe | 7 | the 7 "value" divergences are ~1e-13-relative integrator noise, far inside the requested 1e-6 accuracy — two correct integrators legitimately differ here; harness oracles MUST use tolerance |
| Math-41 | seman | patch1-Math-41-SimFix (1) | label_stands | probe | 0 |  |
| Math-50 | seman | patch1-Math-50-Arja (1) | label_stands | probe | 0 |  |
| Math-53 | seman | patch1-Math-53-Arja (1) | label_stands | probe | 0 |  |
| Math-57 | seman | patch1-Math-57-CapGen (1) | label_stands | probe | 0 |  |
| Math-59 | seman | patch1-Math-59-CapGen (1) | label_stands | probe | 0 |  |
| Math-63 | seman | patch1-Math-63-CapGen (1) | label_stands | probe | 0 |  |
| Math-65 | seman | patch1-Math-65-CapGen (1) | label_stands | probe | 0 |  |
| Math-71 | seman | patch1-Math-71-SimFix (1) | label_stands | probe | 0 |  |
| Math-73 | seman | patch1-Math-73-Arja (1) | label_stands | probe | 0 |  |
| Math-75 | seman | patch1-Math-75-CapGen (1) | label_stands | probe | 0 |  |
| Math-80 | seman | patch1-Math-80-CapGen (1) | label_stands | probe | 0 |  |
| Math-82 | seman | patch1-Math-82-ACS (1) | label_stands | probe | 0 |  |
| Math-86 | seman | patch1-Math-86-Arja (1) | label_stands | probe | 0 |  |
| Math-93 | seman | patch1-Math-93-ACS (1) | label_stands | probe | 0 | resolved 2026-07-17 (final3 retry with 600s timeout; one probe still timed out, the other ran clean) |
| Math-99 | seman | patch1-Math-99-ACS (1) | label_stands | probe | 0 |  |
| Time-4 | seman | patch1-Time-4-Elixir (1) | label_stands | probe | 0 |  |
| Time-15 | seman | patch1-Time-15-ACS (1) | label_stands | probe | 0 |  |
| Time-19 | seman | patch1-Time-19-HDRepair (1) | label_stands | probe | 0 |  |
| Closure-63 | — | — | **EXCLUDED** | — | — | bug deprecated in installed Defects4J |
| Lang-10 | — | — | **UNPINNABLE** | — | — | TRUE MISLABEL: diverges from dev fix AND SimpleDateFormat; sole correct patch -> tried: patch1-DeepRepair |
| Lang-41 | — | — | **UNPINNABLE** | — | — | sole correct patch is a true mislabel tried: patch1-SimFix |

### 3c. Audit cohorts (provenance of the verdicts above)

Chronological batches, all 2026-07-15/16, JVM 11.0.31: known-answer set
(Time-4, Lang-22, Chart-1) → active-suite sweep (7 legs, incl. the Math-2
false-zero → v2 probe fix) → B3 paired sweep (28 o + 33 c) → infra
retries (6/6 succeeded; Closure-18 → 210, Math-71 → 562 strong) →
unpaired sweep (15 o + 26 c) → fallback certifications for failed-patch1
bugs (Math-63/Math-70/Closure-86 rescued at rank 2; Lang-7/Lang-22 still
equivalent) → 13 witness deep-dives resolving every zero and correct-side
anomaly. Raw records: `scratch/eval_expansion/*.jsonl` on the VM; witness
programs under `/tmp/wit*/` and `/tmp/d4j/wit_anom/`.

Headline rates (FINAL, 2026-07-17 — nothing pending): overfit side 44/50
evaluable bugs detectable (88%); 6 unpinnable (4 dev-fix-equivalent label
errors, 1 env-ceiling, 1 no-sound-oracle); 2 deprecated. Correct side:
57 pinned; 2 TRUE MISLABELS found (Lang-41, Lang-10); every other label
stood. The last three stragglers resolved on the final3 heavy retry
(600s timeout, 2 probes): Closure-70 and Math-93 clean, Math-39 stands
with a tolerance caveat (its 7 "value" divergences are ~1e-13-relative
integrator noise, far inside the requested 1e-6 accuracy).

## 4. The verdict taxonomy, explained

**DETECTABLE** = there exists an input where the overfit patch's behavior
differs (strongly: value or exception-class) from the developer fix in our
environment. A perfect harness could catch it; an FN on such a leg is a
technique failure worth debugging.

**UNDETECTABLE type A — genuine equivalence.** The patch is extensionally
identical to the developer fix **on every JVM**: it is the dev fix plus
dead code (Math-70's `|| i < 0`, Closure-86-p2's never-matching predicate),
a redundant guard (Math-63, Lang-22), a refactoring-equivalent expression
(Math-30), or literally the dev fix reformatted (Math-59). **No reattempt
is possible or meaningful — no input in any environment can distinguish
the two programs.** These are best understood as drr LABEL ERRORS (a
"correct" patch filed under overfitting) and should be reported upstream.
Permanently excluded from recall denominators.

**UNDETECTABLE type B — environment ceiling.** The patch and the dev fix
DO differ — but only under conditions our environment cannot produce.
Lang-7/Arja differs from the dev fix only on a JVM whose
`BigDecimal("--…")` silently parses (the old Apple JVM quirk the guard
defends against); on OpenJDK the difference collapses to exception-message
text, which is not an admissible oracle. **Reattempt path exists but means
changing the ENVIRONMENT, not the oracles**: run that leg on a JVM/platform
exhibiting the quirk (old JDK 6/OS X image or a shimmed BigDecimal).
Excluded from OUR recall denominator; the label itself is defensible.

**UNEVALUABLE — infrastructure, each with a concrete reattempt path:**
- **Closure-63 (both legs)**: the bug is DEPRECATED in Defects4J 2.x
  (`defects4j bids -p Closure` skips 63) — no checkout possible. Reattempt
  only by installing a legacy Defects4J 1.x side-by-side. Low value; drop.
- **Closure-18/SequenceR-o**: the LLM-written probe hung on its FIRST input
  (0 output lines in 300s; 400MB RSS) — a probe-generation defect, not a
  build problem (the correct leg of the same bug ran fine). Reattempt:
  regenerate the probe (`--probes 2`); ~5k tokens.
- **Math-71/Arja-o** and the 4 correct-side compile/run failures
  (Chart-12-c, Chart-26-c, Closure-62-c, Math-53-c): one-shot LLM
  API-version near-misses against old library versions (3 repair rounds
  exhausted) or a probe array-index bug outside the per-input try/catch.
  The patches and builds are all healthy. Reattempt: rerun the certifier
  (fresh generation is near-independent), optionally repair rounds 3→5;
  ~5k tokens each, high success likelihood.

So of the 8 "missing" verdicts, 6 are cheap retries (~30–40k tokens
total), 2 (Closure-63 ×2) are permanent drops.

## 4b. The pinned task set — `suites/pinned_tasks.jsonl`

The PRIMARY source for all future pipeline testing. One row per (bug,
side): pin the sorted-first patch file if the audit gave it a TRUE verdict
(overfit → detectable; correct → label stands); otherwise fall back to the
next candidate, certify, pin at `pin_rank: 2`. Rows carry verdict,
verdict_source (probe / witness+deep-dive), strong divergences, env, date,
notes (Lang-50/SimFix was BLOCKED on the applier fix; unblocked 2026-07-17 when P0.1 landed and its full build passed trigger verification).
Statuses: `pinned` / `unpinnable` (with reason and tried-files list) /
`excluded` (deprecated bugs). FINAL headline (2026-07-17, nothing
pending): **101 pinned legs — 44 overfit + 57 correct**. Consume THIS
file (plus `label_annotations.jsonl`), never raw drr directory names.

**The `split` field — development vs held-out.** Every pinned row also
carries `split: dev` or `split: heldout`. We iterate on the dev set only
and touch the held-out set once, at final confirmation with the flagship
model — running everything per iteration costs too much, and numbers from
bugs we tuned on prove nothing. The assignment is at BUG level (both
sides of a bug always share a split), because relation pooling (plan
P3.2) shares synthesized rules between patches of the same bug — a bug
straddling the split would leak information across it.

- **dev (30 legs, 17 bugs)**: the 12 bugs whose failure mechanisms we
  deep-dived and designed the Phase-0–3 fixes around (Math-2, Chart-26,
  Lang-7, Lang-50, Lang-27, Lang-41, Lang-60, Chart-7, Closure-62,
  Closure-92, Math-57, Time-11) — these are design-contaminated and can
  NEVER serve as honest held-out evidence, so they must live here — plus
  the 3 bugs the plan names as validation targets (Math-53 and Closure-73
  narrow-divergence, Time-4 broad-divergence), plus 2 untouched pairs
  (Chart-3, Closure-33) as a fresh-signal smoke check against
  over-tuning. 16 overfit / 14 correct.
- **heldout (71 legs)**: everything else — 28 overfit / 43 correct.
  Never used for iteration, prompt tuning, or debugging. Expect dev
  numbers to overstate improvements (the fixes were designed on those
  bugs); the held-out run is the number that counts.

## 4c. Phase-1 baseline on the dev set (2026-07-17)

First scored pipeline run after Phase-0 fixes, on the 30 `split:dev`
legs (16 overfit / 14 correct). Model gpt-5.4, ~2.5M tokens across three
sub-runs (p1base + p1base_b + p1base_c; the first hit the disk-full wall
at 12/30 and was completed after cleanup + the pin-order fix). Archived
under `runs-archive/runs/p1base*`.

**Headline: overfit recall 9/16 = 56%; correct-side 13/14 clean; the one
false alarm is Chart-26-c (the expected flag-pattern launder, awaits
P3.3). Positive-prediction precision 9/10 = 90%. No unexpected FP.**

| overfit leg | outcome | | overfit leg | outcome |
|---|---|---|---|---|
| Chart-3 | TP | | Lang-50 | FN (broad, latent discriminator) |
| Chart-7 | TP | | Lang-60 | TP |
| Chart-26 | TP | | Math-2 | FN (broad, stochastic oracle — flaky) |
| Closure-33 | TP | | Math-53 | FN (narrow, 3 div) |
| Closure-62 | TP | | Math-57 | FN (witness, float-width) |
| Closure-73 | FN (narrow, 7 div) | | Time-4 | TP |
| Closure-92 | TP | | Time-11 | FN (witness, cross-thread — EXPECTED) |
| Lang-27 | FN (crashing, lifted-crash-only) | | Lang-41 | TP |

Correct legs: 13 TN, 1 FP (Chart-26-c). All 7 misses share one
mechanism: the check that fired on the buggy build was the LIFTED SEED
(the reported input/crash), which the overfit special-cased, so the
patched build passes it; the discriminating generalization was latent
(P0.4 flags it), stochastic, or on an untouched surface. Full triage +
plan implications in `semantic-recall-brainstorm.md` (P1.3 + predictions
ledger). Two findings worth flagging here:
- **The pipeline beats our own certifier probe on witness-only bugs**: 5
  of 7 zero-probe-divergence overfits (Chart-7, Lang-41, Lang-60,
  Closure-62, Closure-92) were caught at baseline. "Witness-only" is a
  property of the certifier's single probe, not of the pipeline's
  several diverse harnesses.
- **Math-2-o is caught only flakily** (TP at the P0 gate, FN here)
  because its firing oracle reads `sample()`, a random draw. The
  reliable discriminator (`getNumericalMean` = −49.76, deterministic) is
  generated but latent — a P2.2/P3.2 target.

## 5. Why unpaired bugs were deferred — NOW DONE (see §3d)

Phase 1 targeted the paired pool because the immediate purpose was
**eval-set construction**, and a scored suite leg needs both labels per
bug: the overfit leg measures recall, the correct leg measures precision,
and a bug with only one side cannot contribute a complete
confusion-matrix row.

But unpaired bugs are still useful and SHOULD eventually be certified:
- **Overfit-only bugs (15)** can serve as recall-only eval cases (every
  detection is a TP; no FP risk measurable on that bug). Certification
  cost: 15 probes ≈ 75k tokens.
- **Correct-only bugs (26)** can serve as precision-only cases AND as the
  broader Dcorrect label audit (the mislabel probe found 1 true mislabel
  in 30 — worth knowing the rate over all 26+). Cost ≈ 130k tokens.

## 6. Sibling patch files (largest uncovered surface)

**What a sibling is.** A patch file is identified by (bug × tool ×
attempt): `patch2-Math-80-Arja-plausible.patch` is a *sibling* of patch1 —
same bug, same tool, a different candidate patch. ~191 overfitting and
~130 correct sibling files exist beyond the certified `patch1` per bug.

**Why there are so many.** APR tools don't emit "the fix"; they emit every
candidate that passes the test suite:
- Search-based tools (Arja, GenProg family) output the whole surviving
  population of a genetic search — one run on one bug can yield many
  distinct plausible patches, multiplied by random seeds (hence 16
  overfitting Chart-1 Arja files, 31 for Math-80).
- Ranking tools (Jaid, SimFix, CapGen) keep the top-k ranked candidates.
- Neural tools (SequenceR) keep the top-k decoded suggestions that
  compile and pass.
The dataset intentionally preserves them all: each sibling is a DIFFERENT
way to fool the same test suite, and a high sibling count is itself a
signal that the bug's test suite is weak. On the Dcorrect side, siblings
are multiple human-judged-equivalent correct variants — useful precision
stressors (different-looking programs a harness must NOT flag).

**Pinning convention — now ENFORCED everywhere (2026-07-16).** All suites
resolve their patch via `head -1` (patch1 by sort), so ALL current eval
legs and ALL certifications in §3 are patch1 files — except Closure-86,
where Doverfitting/SequenceR has no patch1 and patch2 was certified.
Additionally, `PatchSelector` (src/java/patches.py) — the fallback used
by ad-hoc `run.py` invocations without `--patch_file` — previously
`random.choice()`d both the APR tool and the patch file; as of 2026-07-16
both picks are sorted-first deterministic, so NO code path can select an
unpinned patch anymore. Pre-suite historical batches DID sample patch
files randomly; per-bug comparisons against those numbers are not
apples-to-apples (this caused the Chart-1 triage confusion).

**Why siblings matter (proven today):** verdicts are per-FILE —
Closure-86 patch2 ≡ dev fix while siblings patch3/patch5 genuinely
diverge; and Chart-1's old "likely undetectable" triage had examined a
different file than the pinned patch1 (which certifies at 472 strong).

**Coverage options.** Full sibling certification ≈ 1.5M tokens (phase 2 —
separate budget decision). Cheaper targeted slice with immediate payoff:
certify only the siblings of the §3a UNDETECTABLE legs (~30 files) — any
detectable sibling lets that bug rejoin the eval set by re-pinning the
suite to it.

## 7. Crashing bugs (43 bugs; 153 overfit + 93 correct files)

Not enumerated by `eval_candidates.py` (semantic-only), and mostly
uncertified — but NOT exempt from the same failure modes: 3 of the 4
crashing legs we did certify were detectable (Lang-27 30, Lang-43 39,
Chart-5 396 strong), and **Math-70/SketchFix — a crashing bug — is one of
the type-A equivalences**, proving crashing bugs can carry
undetectable/mislabeled overfits too (the crash oracle does not save you
when the "overfit" IS the dev fix). Active crashing suite (crash14.cases):
o-legs Lang-27, Lang-39, Lang-45, Math-8, Math-49, Math-85, Math-97;
c-legs Chart-5, Lang-6, Lang-51, Lang-57, Math-4, Math-70, Time-7 —
**10 of these 14 legs are uncertified.** Recommended: certify the crash14
legs next (~70k tokens) so the crashing metrics get the same denominator
hygiene as the semantic ones.

## 8. The unlabeled set — Dunassessed (625 files, 135 bugs)

No ground-truth labels — cannot contribute to precision/recall scoring
as-is. Composition:

| Tool | Files | Bugs |
|---|---|---|
| Cardumen | 285 | 75 |
| JGenProg2017 | 143 | 44 |
| Nopol2017 | 92 | 92 |
| jKali | 53 | 20 |
| jMutRepair | 52 | 20 |
| by project (files) | Math 302 · Closure 117 · Chart 111 · Lang 50 · Time 45 | |

Why a whole category is unlabeled: **the drr human assessment was done
per TOOL's output, not per bug** — patches from Arja/Jaid/SimFix/… were
manually compared to the dev fix and sorted into Dcorrect/Doverfitting;
the five tools above never received that treatment, regardless of bug.
Two consequences worth stating explicitly:
- **Every bug in the dataset HAS a developer fix** — all are Defects4J
  bugs, which always ship buggy + dev-fixed checkouts and trigger tests.
  "Unlabeled" means only that nobody ever checked the patch against that
  (available) dev fix. This is exactly why machine-labeling via the
  certifier is possible.
- Overlapping Dunassessed patches are NOT "siblings" in the §6 sense
  (same bug × same tool); they are same-bug/DIFFERENT-tool candidates.
  Once machine-labeled they function like additional patch options on an
  already-usable bug.

Overlap structure (the part that makes labeling cheap). NOTE: "overlap"
is at the BUG level, never the file level — no patch file is in two
categories; the same Defects4J bug simply has patches from both assessed
tools (→ Dcorrect/Doverfitting) and unassessed tools (→ Dunassessed),
e.g. Math-2 has labeled Arja/SOFix patches AND an unlabeled Nopol2017 one.
- **66 of the 135 bugs also carry labeled (Dcorrect/Doverfitting) patches**
  — infrastructure (checkouts, trigger tests) already known to work.
- **28 bugs overlap our CERTIFIED set** (Chart-1,3,5,7,12,19,26;
  Closure-18,33,62,63,115; Lang-7,22,27; Math-2,5,30,33,50,53,57,63,70,
  73,80,82; Time-4) — **205 Dunassessed patch files** sit on bugs where
  the dev-fix build is already cached and per-bug detectability is known.
  Machine-labeling those via the certifier (strong divergence ⇒
  behaviorally-wrong; repeated widened-probe zero + §4 scrutiny ⇒
  equivalent-to-fix) costs ~5k tokens/file ≈ 1M for all 205, or ~150k for
  one file per (bug, tool) — the cheapest route to a big eval-set
  expansion.
- 69 bugs are unlabeled-only (would need fresh checkouts + kind
  classification first).

## 8b. Audit-derived label corrections (new labels this audit produced)

The audit effectively RE-labeled several patches whose dataset label does
not match observed behavior. Machine-readable copy for the scorer:
`suites/label_annotations.jsonl` (one JSON object per finding; consume it
when computing denominators instead of trusting raw drr labels).

| Patch (file, as-labeled) | drr label | Audit verdict | Basis |
|---|---|---|---|
| patch1-Math-59-SequenceR | overfitting | **equivalent-to-dev-fix (label error)** | textually the dev fix |
| patch1-Closure-115-ssFix-plausible | overfitting | **equivalent-to-dev-fix (label error)** | dead-pure leftover only |
| patch2-Closure-86-SequenceR | overfitting | **equivalent-to-dev-fix (label error)** | never-matching predicate ≡ `return false` |
| patch1-Math-30-ssFix | overfitting | equivalent-to-dev-fix | cast-at-use ≡ cast-at-declaration |
| patch1-Math-63-CapGen | overfitting | equivalent-to-dev-fix | redundant `\|\| x==y` disjunct |
| patch1-Math-70-SketchFix-plausible | overfitting | equivalent-to-dev-fix | dev fix + dead `\|\| i<0` |
| patch1-Lang-22-Arja-plausible | overfitting | equivalent-to-dev-fix | redundant deleted guard (0/909 + 0/1700) |
| patch1-Lang-7-Arja-plausible | overfitting | **environment-conditional** | wrongness only on LANG-822-quirk JVMs; ≡ dev fix on OpenJDK |
| patch1-Lang-41-SimFix | correct | **overfitting (TRUE mislabel; partial fix)** | String helpers left broken; byte-identical to Doverfitting Lang-41-Arja |
| patch1-Lang-41-Arja-plausible | overfitting | overfitting (confirmed) — but byte-identical to the Dcorrect file above | dataset self-contradiction |
| patch1-Math-2-SOFix (file) | correct | label stands; FILE was reversed+truncated — repaired locally | `.bak.orig-reversed-truncated` kept |
| patch1-Lang-50-SimFix | correct | label stands; 43-div record is an APPLIER artifact (out-of-order hunks) | re-probe after applier fix |

Additions from the unpaired sweep (full records in
`label_annotations.jsonl`):

| Patch | drr label | Audit verdict | Basis |
|---|---|---|---|
| patch1-Lang-10-DeepRepair | correct | **overfitting (TRUE mislabel)** | first-char whitespace collapse ≠ dev fix ≠ SimpleDateFormat; passes all tests |
| patch1-Closure-123-SequenceR | overfitting | **no-sound-oracle divergence** | redundant-parens formatting only; identical ASTs |
| patch1-Closure-92-SequenceR | overfitting | confirmed (witness) | char-widening `indexOf` trap; multi-module surface |
| patch1-Time-11-Arja | overfitting | confirmed (witness) | dead-code edit; ThreadLocal NPE on other threads |

### 8b-supplement — per-patch sweep of ALL overfit files (2026-07-21)

The July audit certified ONE patch per bug; this sweep certified all 92
scoreable-bug overfit files, then deep-dived every div=0 (probe false-zeros
are common — see the coverage lesson). New per-patch findings:

**NEW MISLABELS (overfit file behaviorally == dev fix; exclude from recall):**
| Patch | Basis (evidence: deep-dive 2026-07-21, agent + witness) |
|---|---|
| patch1-Lang-43-CapGen-plausible | injected `getQuotedString(...,false)` reduces to a single `next(pos)` (first char guaranteed QUOTE) = dev fix |
| patch1-Lang-45-Jaid-plausible | empty-check reshaped as `else` of same clamp; sole divergent path returns "" like dev fix; 3200 inputs 0 div |
| patch1-Lang-51-Arja-plausible | equalsIgnoreCase chain recognizes identical {true,on,yes}; false/off/no branches dead; total function = dev fix |
| patch1-Lang-39-Elixir-plausible | `i>searchList.length` makes size loop dead (capacity-only); output+exceptions identical to dev fix |
| patch1-Math-50-Jaid-plausible | recompute block dead (contradictory else/if guard); redundant `x1=x` overwritten post-switch; 4800 cases 0 div |

**NEW DETECTABLE (probe false-zeros — real divergence; usable eval legs the
one-per-bug audit never covered):**
| Patch | Witness |
|---|---|
| patch1-Math-85-CapGen-plausible | uses param `upperBound` for function value `fb` → spurious ConvergenceException on valid brackets |
| patch1-Chart-12-Arja-plausible | deletes ctor `pieChart.setTitle(seriesTitle)` → title null vs BOTTOM/Bold-12 (getPieChart().getTitle()) |
| patch1-Closure-18-SequenceR | `A\|\|B` vs dev `A` → spurious CIRCULAR_DEPENDENCY_ERROR when closurePass on + dep-mgmt off |
| patch1-Lang-39-Nopol2015-plausible | `replaceEachRepeatedly` (repeat=true) NPEs on null entry vs dev "zz" (sibling-method false-zero) |
| patch1-Math-50-HDRepair | keeps x0-nudge block → converged root vs dev TooManyEvaluationsException (1218/4800 div) |
| patch1-Math-32-Jaid-plausible | `tree==tree.getCut()` always false → whole-space size 0 vs dev Infinity |
| patch1-Math-32-Elixir-plausible | `tree.getPlus().getAttribute()` NPEs on leaf tree vs dev Infinity/0 |

**DETECTABLE-BUT-NO-SOUND-ORACLE (exclude, like Lang-7/Closure-123):**
| Patch | Basis |
|---|---|
| patch1-Lang-20-Arja-plausible | differs only via dev-fix's `noOfItems*16` int-overflow (NegativeArraySizeException at 2^27 elems); a sound oracle cannot assert the dev-fix overflow bug |

Upstream-report candidates: the three patch≡dev-fix **label error** rows,
the Lang-41 self-contradiction pair, and **Lang-10** (correct-labeled
patch behaviorally wrong vs both dev fix and JDK reference behavior).

## 9. Deep-analysis index — bugs with a full workup beyond the probe

Where to find the detailed reasoning and reproducible evidence per bug.
"Addendum" = `semantic-recall-brainstorm.md` ADDENDUM sections; "UND" =
`suites/labels/incorrect_labels.md`; VM paths are on hetzner. Run logs for the pipeline
(not certifier) analyses are the local scratchpad copies of
`fixconfirm_20260716_023210` / `sem8_v2_20260715_124137`.

| Bug | Legs analyzed | Key finding | Evidence |
|---|---|---|---|
| **Lang-7** | Arja-o, ACS-c (pipeline logs, all runs) + manual differential | Doc's original narrative was inverted (d4j dev patches are stored fixed→buggy); failing test DOES pin the `--`→NFE direction; Arja ≡ dev fix on modern JVMs (type-B ceiling, LANG-822 quirk); backwards synthesized relation appeared on the ACS leg (direction read off mislabeled "Patched method(s)" source); self-swallow pattern discovered here | Addendum §A1, §A4, §A7; UND row; probe+outputs `/tmp/l7probe/` |
| **Math-2** | Arja-o, SOFix-c (pipeline logs) + certifier v1/v2 | Arja FN = 3 causes: patch-relative anchoring missed `getNumericalMean` (still −49.76 on Arja build), injected relation un-implementable under no-subclass rule, fuzz ranges excluded MIN_VALUE/overflow regimes. SOFix leg: drr patch file truncated AND stored reversed → repaired (`.bak.orig-reversed-truncated` kept); sem8_v2's TN was a phantom (`no_harnesses` mislabeled). Certifier v1 zero was a FALSE ZERO (probe anchored on patched method whose outputs are identical even at overflow params — `icdf(0.5)` agrees) → v2 widened-surface probe: 117 strong | Addendum §A3, §A6, §F2b, §F3; mini-probe `/tmp/M2Probe.java`; `certified_suite_v2.jsonl` |
| **Chart-26** | Jaid-o (TP), Jaid-c (FP) (pipeline logs, both runs) | FP = pre-existing `SIOOBE@G2TextMeasurer` on malformed-Unicode fuzzed titles, WRAPPED in FuzzerSecurityIssueLow → launders past A2 attribution (skips harness-own classes); GanttChartTests is a legit trigger test (not a type transplant); TP is real (injected relation fired 2/3); verifier judged the same unsound class opposite ways in one run; first-oracle shadowing left FP oracles unscreened dead code on buggy | Addendum §A2, §A5; run logs `05_/06_Chart-26_Jaid_*` |
| **Lang-50** | Arja-o (225 strong), SimFix-c (43 → artifact) | **Pipeline patch-applier bug**: hunks in descending line order → applier silently applied only the first (no .rej); certified build fails the trigger test; fully applied = 0/518 divergences, label stands. Applier shared with main pipeline → all historical multi-hunk legs suspect | UND correct-leg verdicts; `/tmp/d4j/Lang_50_fullpatch_check`, `/tmp/l50_*.txt`, `/tmp/l50check/T.java` |
| **Lang-41** | Arja-o (false zero), SimFix-c (true mislabel) | Both patches are **byte-identical** — drr labels the same semantics both correct and overfitting. The patch is a partial fix (Class overloads rerouted; root-cause String helpers left broken: `getShortClassName("[Ljava.lang.String;")` → `String;` vs `String[]`). 0div-vs-5div asymmetry was probe-generation variance | UND both tables; witnesses `/tmp/wit/WLang41.java`, `/tmp/l41_*.txt` |
| **Math-57** | ssFix-o (false zero) | `float sum` vs dev `double sum`: k-means++ seeding diverges on ~50% of seeds with 1e20-scale coordinates (d² overflows float); probe missed because the test's point class caps coords at int | UND false-zero table; `/tmp/d4j/witness57/W57.java` + out files |
| **Lang-60** | Arja-o (false zero) | Two divergence families: `contains(char)` destructively reallocates buffer (capacity 32→3 — documented read-only), and unfixed sibling `indexOf` reads stale chars after `delete` (fixed −1 vs Arja 2) | UND; `/tmp/wit/WLang60.java` |
| **Chart-7** | Arja-o (false zero) | Arja rewired `getMaxMiddleIndex()` to return `maxEndIndex`; indices coincide on non-overlapping periods (all the probe generated); overlapping `SimpleTimePeriod`s separate: fixed 1 vs Arja 0 | UND; `/tmp/wit/WChart.java` |
| **Closure-62** | Jaid-o (false zero), Jaid-c (probe compile, retry) | Jaid's `\|\| charno==len` escapes the LINE guard: in REGION/FULL excerpt mode at charno==len it prints a caret the dev fix doesn't (real in-tree config); LINE-mode identical everywhere | UND; `/tmp/wit/WC62.java`, `out_Closure_62_*.txt` |
| **Closure-115** | ssFix-o (type A) | Dev fix deleted computation+use; ssFix deleted only the use — leftover block is dead-pure (guarded unreachable by `isDirectCallNodeReplacementPossible`; pure static analyses) → ≡ dev fix any JVM; suspected mislabel | UND type-A table |
| **Closure-86** | SequenceR-o patch2 (type A) | `return isImmutableValue(NEW-node)` can never match Token.NEW → ≡ dev fix's `return false`. **Siblings patch3/patch5 NOT equivalent** — the per-file-verdict proof case | UND type-A table |
| **Math-59** | SequenceR-o (type A / label error) | The "overfitting" patch IS the developer fix textually (ternary right-associativity; whitespace/parens only). Report upstream | UND type-A table |
| **Math-30** | ssFix-o (type A) | Cast-at-use ≡ dev fix's cast-at-declaration (int→double widening exact at both use sites); residual `n1*n2` int overflow exists identically in the dev fix | UND type-A table |
| **Math-63** | CapGen-o (type A) | Patch = dev fix + redundant `\|\| x == y` (implied by ulp-equals incl. ±0.0 SGN_MASK mapping; NaN rejected on both) | UND type-A table |
| **Math-70** | SketchFix-o (type A; crashing bug) | Patch = dev fix (`solve(f,min,max)`) + dead disjunct `\|\| i < 0` (loop counter starts at 0) — proof crashing bugs also carry dev-fix-equivalent "overfits" | UND main table |
| **Lang-22** | Arja-o (type A), DeepRepair-c (B1) | Deleted `abs≤1` guard redundant with general GCD loop: 0/909 certifier + 0/~1,700 B1 probe lines | UND main table; progress.md §4/§B1 |
| **Lang-27** | DeepRepair-o (30 strong), SimFix-c (B1) | Historical FP proven benign: dev fix incidentally fixed a latent SIOOBE on `"0.eE"`-style inputs that SimFix's minimal patch left — 631 divergences all latent-crash surface; label stands; validated A2's kind-scoping | progress.md B1 section; `b1_mislabel.jsonl` |
| **Chart-1** | Arja-o patch1 | Certification (472 strong: patch clobbers `itemLabelGeneratorList`) OVERTURNED the old "likely undetectable" triage, which had examined a different one of the six Arja patch files | UND "NOT undetectable" list; progress.md §8 |
| **Time-4** | Arja-o, Elixir-c (t4syn/t4fix history) | The recurring FN was a VERIFIER over-kill + broken changed-line extraction, not a generation gap; first end-to-end synthesized-relation TP (t4fix4); certified 1,764 divergences | progress.md §8; `certified_known.jsonl` |
| **Closure-18 / Closure-63 / Math-71 / Math-53** | infra deep-dive | Probe hang on first input (regenerate, timeout alone insufficient) / deprecated in d4j 2.x (permanent) / probe-compile API near-miss (retry) / probe array-index outside try/catch (retry). RETRIES (2026-07-16 pm): Closure-18 → 210 strong, Math-71 → 562 strong (both detectable); Chart-12/Chart-26/Closure-62/Math-53 correct legs all 0-div — every prediction held | UND infra section; `b3_sweep.log`; `retry_certified.jsonl` |
| **Lang-10** | DeepRepair-c (TRUE MISLABEL — 2nd Dcorrect overturned) | Keeps whitespace-collapse the dev fix deletes, emits first char instead of `\s*+`; witnessed diverging from BOTH dev fix and SimpleDateFormat (`parse("3  Tue","M  E")`) while passing the full FastDateParserTest incl. trigger — human-assessed "correct", certifier said no | `/tmp/d4j/wit_anom/WLang10.java`; label_annotations.jsonl |
| **Closure-92** | SequenceR-o (false zero → witness) | `indexOf('.', '.')` = `indexOf('.', 46)` → −1 for namespaces <47 chars; implicit-namespace declaration placed differently, runtime-visible; probes never built a multi-module JSModule graph | `/tmp/d4j/wit_anom/WC92.java` |
| **Time-11** | Arja-o (false zero → witness) | Dead-code edit deletes the one `verbose()` call the trigger test traverses; broken ThreadLocal remains — `verbose()` NPEs from any non-classloading thread. 6,500 probe lines all single-threaded → taxonomy surface #6: cross-thread state | `/tmp/d4j/wit_anom/WTime11.java` |
| **Closure-123** | SequenceR-o (new category: `no_sound_oracle_divergence`) | Hardcoded IN_FOR_INIT_CLAUSE adds REDUNDANT parens (`f(a?(b in c):d)`); outputs parse to identical ASTs — formatting-only divergence a sound oracle may not assert. Sole file → unpinnable; label defensible only under a textual oracle | `/tmp/d4j/wit_anom/WC123.java` |

## 10. Known systemic caveats over ALL of the above

1. **Probe zeros are not self-certifying.** 5 of 10 zero verdicts in the
   B3 sweep were FALSE (real divergences on sibling overloads, observer
   state, non-default configs, float-magnitude extremes). Until probe-v3
   encodes those four rules, any new zero requires a deep-dive before it
   is trusted. All CURRENT type-A/B verdicts already survived that
   scrutiny (code-level mechanism identified for each).
2. **The patch-applier bug (Lang-50) taints history.** The shared
   `PatchedProjectBuilder` silently drops out-of-order hunks. Until it is
   fixed and a trigger-tests-on-patched-build gate is added, any past
   result on a multi-hunk patch is suspect; an audit of multi-hunk drr
   patches is queued.
3. All verdicts are per (patch file, environment JVM 11.0.31); type-B
   verdicts do not transfer across JVMs.
