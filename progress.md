# Progress — semantic-bug findability

Living record of what's been added, what was tried, what worked/didn't, and
where the runs + results live on the Hetzner VM. Nothing here is committed yet
(all work is uncommitted on `main`).

Last updated: 2026-07-15 (afternoon — t4syn analysed, tier-1/2 overhaul
implemented; see §8).

---

## 1. Goal

Generate Jazzer harnesses that detect **overfitting** APR patches for
Defects4J bugs — including **semantic** (assertion-failing, non-crashing)
bugs, where there is no crash to catch and the harness must SUPPLY the oracle.
Metric: per-patch confusion matrix (TP = overfit caught, FP = correct patch
wrongly flagged, etc.). Precision must stay high (a false accusation of a
correct patch is the worst outcome); the open gap is **recall**.

---

## 2. What's been added (uncommitted)

### Kept / active
| Measure | File | Status |
|---|---|---|
| Lifted-assertion oracle (lift EVERY assertion) | `prompts.py` `_lifted_assertion_block` | **Foundational** — all semantic TPs |
| Metamorphic/post-condition block (crashing) | `prompts.py` `_metamorphic_block` | Useful for crashing recall |
| Relation verifier (LLM-critic soundness filter) | `relation_verifier.py` | Active but leaky |
| Time-4 trigger extraction fix | `failure_test.py`, `patches.py` | Effective infra fix |
| Two-tier model escalation (nano→gpt-5.4) | `config.py`, `campaign.py`, `run.py` | Cost control |
| **Mined sibling-test oracles** (NEW) | `test_oracle_miner.py` + `prompts._mined_oracle_block` | Sound; **neutral** on gpt-5.4 (see §4) |
| **Relation synthesis** (NEW) | `relation_synth.py` + `prompts._synthesized_relations_block` | Built; wired behind `--synthesize_relations`; being tested |
| **Token-usage logging** (NEW) | `llm.py` (`token_usage`/`usage_totals`), `run.py` | Records exact tokens per run into `--results_json` |

### Deleted this session
- **Oracle-strength gate (gate 2.5)** — fired 0× in 24 runs; gated on a
  Math-shaped name whitelist. Removed from `oracle_strength.py` / `campaign.py`
  / `run.py`.
- **Diff-targeting block** (`_changed_region_block`) — caused false positives
  fenced *and* unfenced. Removed.

### Still to retire
- **`_STAT_PATTERNS`** whitelist (`prompts.py`) — Math-shaped; synthesis
  subsumes it (verified: synthesis reproduces mean/variance/bounds/monotonicity
  + the CDF contract). Delete once synthesis is wired-in-by-default and screened.

---

## 3. Design: mine → synthesize → screen → inject

Recall needs a *mechanism*, not prompt exhortation (proven: diff-targeting
failed because the model rationalizes unsound invariants as "trusted").

1. **Mine** (`test_oracle_miner.mine_sibling_tests`) — harvest sibling test
   methods that exercise the patched class/method (method-name tokens, falling
   back to the class name for private-helper patches). Trusted by construction.
2. **Synthesize** (`relation_synth.RelationSynthesizer`) — LLM proposes
   invariants / metamorphic relations over the API, grounded in the mined
   tests. Covers inputs no test touches.
3. **Screen** (Stage 3, in progress) — validate each relation mechanically:
   the **cross-patch consensus** design — replay the firing input across N
   sibling patches (unlabeled); an oracle that fires *broadly* is out-of-domain
   / unsound → drop; one that fires on a *minority* is discriminating → keep.
4. **Inject** survivors into the harness prompt as trusted oracles.

---

## 4. What was tried — results (good/bad)

### Diagnostics (gpt-5.4, 24 runs, both labels)
- **v1 baseline** (`diag`): **P 0.88 · R 0.58 · F1 0.70**; semantic P 1.00 / R 0.50.
  The whole gap is recall. FNs are the harness anchoring on the seed but not
  probing the patch's changed region.
- **v2 diff-targeting** (`diag2`): **regressed to F1 0.50** — un-fenced
  "explore beyond the seed" produced 3 false positives (unsound invented
  oracles). **Reverted.**
- **v3 guarded fence** (`diagf`, focused 12): fence followed in words, ignored
  in effect — same 3 FPs. **Reverted for good. Diff-targeting is a dead end.**

### The one FP that mattered (Lang-27)
- Proven a **true FP** (not a mislabel): swept 15,119 inputs — SimFix's
  "correct" patch is behaviorally identical to the developer fix; the harness
  leaked a pre-existing `StringIndexOutOfBounds`. **Dataset "Dcorrect" labels
  are trustworthy.** Fix: broadened the valid-by-construction rule to generic
  runtime exceptions.

### Mined oracles
- **nano batch** (`mined`): confounded — nano invents unsound out-of-domain
  oracles (5 FPs), all *invented*, none from mined pairs. Nano too weak for
  precision measurement.
- **gpt-5.4 A/B** (`mined54`) vs v1: **P 1.00→0.80 · R 0.50→0.50 · F1 0.67→0.62.**
  Mining is **sound but neutral-to-slightly-negative** — it cracked no hard FN
  and plausibly *distracted* the generator (Lang-7 TP→FN with 36 mined
  assertions injected). → Mining should be **off by default / hard-capped**,
  not always-on.

### Triage of the persistent FNs (are they even detectable?)
Behavioral overfit-vs-correct sweeps:
- **Lang-22: undetectable** — 0/256 divergences; overfit is output-equivalent
  (the deleted `abs≤1` guard is redundant with the general GCD loop).
- **Chart-1: likely undetectable** — 0 divergences on the natural legend path.
- **Time-4: DETECTABLE** — 4+ discriminating inputs (`with(clockhourOfDay,24)`
  must throw; `with(clockhourOfHalfday,6)` must succeed) that exist in **no
  test**. Only synthesized beyond-seed relations can reach them. **This is the
  concrete justification for synthesis+screening.**

**Conclusion:** part of the recall gap is a genuine ceiling (Lang-22, Chart-1);
part is real, addressable headroom (Time-4). Realistic ceiling on this 8-bug
set is ~5–6/8, not 8/8.

---

## 5. Runs on Hetzner — where results live

**Storage is NOT date-foldered.** Everything is flat in `/home/code/scratch/`:
- `<batch>.jsonl` (or `<batch>_A.jsonl`/`_B.jsonl` for parallel halves) — one
  JSON record per run (label, status, bug_kind, crashed_on_patch, and now
  `tokens_total`/`tokens_by_model`).
- `<batch>_logs/<Proj>_<bug>_<o|c>.log` — full per-run log (prompt, harness,
  differential).
Files carry filesystem timestamps, so `ls -lt /home/code/scratch/*.jsonl`
sorts by date; the batch name + timestamp is the index.

| Batch | Date | What it was | Headline result |
|---|---|---|---|
| `sem`, `sem2`, `sem3` | Jun 29 – Jul 3 | early semantic batches | — |
| `pin_math2`, `vtest`, `vtestv`, `m2x3` | Jul 4 | Math-2 / verifier probing | Math-2 masked-symptom FN |
| `newpipe` | Jul 5 | pipeline shakeout | — |
| `ab_base`, `ab_fix` | Jul 14 | xref-dedupe A/B | same-or-better perf confirmed |
| `gen`, `nano`, `esc` | Jul 14 | generation / nano / escalation | escalation validated |
| `diag` | Jul 14 | **v1 diagnostic (24, gpt-5.4)** | P0.88 R0.58 F1 0.70 |
| `diag2` | Jul 14 | v2 diff-targeting (24) | regressed to F1 0.50 |
| `diag3` | Jul 15 | v3 guarded (killed at 1) | superseded by `diagf` |
| `diagf` | Jul 15 | v3 focused (12) | fence failed, same FPs |
| `mined` | Jul 15 | mined oracles, nano (16) | confounded (nano invents FPs) |
| `mined54` | Jul 15 | **mined oracles, gpt-5.4 A/B (16)** | P0.80 R0.50 F1 0.62 (neutral) |
| `t4syn` | Jul 15 | Time-4 synthesis test | in progress |

Local analysis copies of the key batches are in the scratchpad
(`diag_all.jsonl`, `mined.jsonl`, `mined54.jsonl`).

---

## 6. Cost note

Token usage was **not logged** before 2026-07-15, so exact spend for the first
two days is unavailable — ballpark **low tens of USD**, dominated by the
gpt-5.4 flagship batches (`diag`+`diag2`+`diagf` ≈ 60 runs). Token logging is
now in `llm.py`/`run.py`; future batches record exact tokens. Iterate on a
**small set or nano**; reserve full gpt-5.4 sweeps for final confirmation.

---

## 7. Open next steps (superseded by §8 — kept for history)

- ~~Finish + wire the cross-patch consensus screen (Stage 3)~~ → replaced by
  the **buggy-build screen** (`relation_screen.py`, built): consensus is
  confounded (APR sibling patches cluster on the same overfit mode, so
  "fires broadly" also describes the BEST oracles) and needs N builds; the
  buggy build is free and correct almost everywhere.
- Retire `_STAT_PATTERNS` once synthesis is default + screened. (Still open.)
- ~~Default mining OFF or hard-cap it~~ → done (opt-in + 10-assertion cap).
- Rebuild the eval set with genuinely-detectable overfits → tooling built
  (`eval_candidates.py` + `certify_detectability.py`); running it is now the
  top priority.

---

## 8. 2026-07-15 (pm): t4syn post-mortem + tier-1/2 overhaul

### t4syn changed the diagnosis
Both legs completed (~45k tokens). **The overfit leg was a raw TP that the
relation verifier un-caught**: harness 1's metamorphic clockhour oracle
fired on the Arja patch (a `Partial` holding both `hourOfDay=10` AND
`clockhourOfDay=24` — an impossible object state), and the verifier dropped
it reasoning "a correct impl may legitimately throw here" — ignoring that
the harness catches-and-skips exceptions, so a throwing correct impl can
never cause a firing. **The Time-4 recall gap is a VERIFIER bug, not a
generation gap** — progress.md §4's "only synthesis can reach Time-4" is
falsified; the existing metamorphic block reached it unaided. Also: on the
overfit leg synthesis never ran at all (introspector died → `functions: []`
→ silent `return []`), and no screening stage existed — the "SCREENED SOUND
RELATIONS" prompt block was injecting UNSCREENED candidates with a false
provenance claim. The verifier is now proven leaky in BOTH directions
(passes FPs: Lang-27/SimFix, Math-2/SOFix; kills TPs: Time-4/Arja).

### Implemented (all uncommitted, synced to VM)
Fixes to existing machinery:
- **Verifier** (`relation_verifier.py`): exception-skip guidance (a
  swallowed throw cannot explain a firing), concrete-evidence channel (the
  crash block, via `oracle_strength.crash_excerpt`), optional diverse-lens
  ensemble (`RELATION_VERIFIER_VOTES`, default 1), docstring now records
  both error modes.
- **trusted_values fixed**: was feeding INPUT literals
  (`candidate_anchor_literals`); now feeds EXPECTED values
  (`PromptBuilder.expected_assert_literals`, assertEquals first-arg
  literals, trivial <3-char literals excluded from the substring
  short-circuit).
- **Honest synthesis block**: relations presented as "mechanically
  pre-screened candidates" with per-relation screen stats; unscreened
  candidates can no longer reach a prompt (screen failure ⇒ inject nothing).
- **Synthesis pinned to the flagship** (`HARNESS_MODEL_ESCALATION`), was
  defaulting to nano under escalation; + one parse retry; + grounded in the
  patch diff and touched-method javadoc (`relation_synth.javadoc_for`).
- **Mining opt-in** (`--mined_oracles`, default OFF per mined54) and capped
  at 10 total assertions (the Lang-7 flood was 36).
- **Degraded-context handling**: loud warning + `context_degraded` in the
  jsonl when no touched function extracts; regex+brace-matching fallback in
  `analysis.py` when the javalang pass comes up empty (the t4syn failure).
- **Prompt contradictions resolved**: mined block's "assert ONLY lifted
  pairs" now explicitly scopes to value-equality pairs (consistency/
  metamorphic/relation checks remain required); throw types unified on
  FuzzerSecurityIssueLow; "unsound assertions cost nothing" overpromise
  replaced with justify-in-a-comment wording everywhere.
- **collect_fired_oracles** now unions the originally-captured headline
  (re-fuzz nondeterminism could drop the original sound firing).
- **Escalation is stall-based** (no new accept in N attempts), not
  zero-accepted-only — one early weak nano accept no longer blocks the
  flagship forever.

New mechanisms:
- **`relation_screen.py`** — the buggy-build screen: each candidate is
  wrapped in a counting harness (violations tallied, never surfaced),
  compiled against the buggy checkout, run for a fixed `-runs` budget;
  drop if fire-ratio > 20% (out-of-domain) or unrunnable; survivors ranked
  selective-firing > silent, capped at 3.
- **Mechanism-per-harness rotation** (`oracle_mechanism`): each semantic
  harness gets the lifted block + ONE of {mined pairs, consistency,
  screened relations} instead of all blocks stacked.
- **Corpus seeding**: string literals from trigger + mined tests written as
  a libFuzzer seed corpus for the buggy-version trigger gate.
- **`verifier_replay.py`** — offline replay of logged (harness, fired
  oracle, label) cases through verifier variants; measures OVER-KILL and
  LEAK rates for pennies. Run this before trusting any verifier change.
- **`eval_candidates.py` + `certify_detectability.py`** — eval-set
  expansion: enumerate semantic-bug patches, then certify each overfit as
  behaviorally-distinguishable-from-the-developer-fix via an LLM-written
  deterministic printer probe run on both builds (DATASET CONSTRUCTION
  ONLY — reads the fixed checkout; never part of a verdict). Known-answer
  validation: Time-4/Arja must certify >0, Lang-22/Arja must be 0.
- **`run_suite.sh <name> [cases_file]`** + version-controlled `suites/`
  (t4.cases now; semantic8/diag24 being extracted from the VM manifests).
  ALL runs — including one-offs — go through this; no more ad-hoc drivers
  (t4syn.sh produced no jsonl and had to be reconstructed by grepping).

### First measurements with the new machinery (same day)

**Verifier replay** (`runs/vreplay_20260715_072333`; 11 logged cases × 2
repeats, gpt-5.4, 61.5k tokens — cases at `scratch/replay/cases.jsonl`):
- **The t4syn over-kill is FIXED**: kept 2/2 (new reasoning correctly cites
  the impossible hourOfDay+clockhourOfDay object state).
- **Over-kill 3/12 (25%)**: chart26_jaid_o dropped 2/2 ("a correct
  drawLabel may treat an empty label as no label"), lang7_arja_o 1/2. TP
  drop decisions are not yet stable → candidates for the 3-lens ensemble
  (`RELATION_VERIFIER_VOTES=3`) or per-case prompt work.
- **Leak 8/10 (80%)** on the historical FP cases: only time4_elixir_c was
  dropped. Context: several of these FP modes are now prevented UPSTREAM
  (generic-exception rule, no-invented-pairs, mining off) so the verifier
  sees fewer of them — but as a last line of defence it is weak. Precision
  still rests mainly on the generator-side rules.
- Replay is the iteration tool: pennies, no builds, ~1 min. Rerun it after
  every verifier prompt change (`verifier_replay.py --cases ... --model X`).

**Time-4 pair rerun** (`runs/t4fix_*`, `t4fix2_*`): TP+TN both times
(P=R=1.0 on the pair), the historical over-kill did not recur. But the
runs exposed three more bugs, all fixed the same afternoon:
1. **run_suite.sh silently fell back to inline defaults** on a relative
   cases path (t4fix ran WITHOUT synthesis unnoticed). Now: cases file
   resolved before cd, hard exit if missing.
2. **The in-pipeline verifier never ran** — default HarnessGenerator read a
   stale `.env` deployment (gpt-5.2), 404'd, and fail-opened on every call;
   the TPs were kept by the error fallback, not a review. Now: run.py
   threads `args.model or HARNESS_MODEL_ESCALATION` into the verifier and
   prints `[verifier] model=...`.
3. **Degraded context persisted on the overfit leg** — real root cause
   found: the Arja hunk header starts in the JAVADOC above the method
   (line 135; the change is at 138), and extraction keyed on hunk-START
   lines. `_parse_patch` now records the actual changed (-) lines
   (insertion point for pure-addition hunks). Validated against the exact
   malformed patch shape.
Also observed: the screen kept 4 silent relations then capped to 3
(messaging clarified), and none fired on buggy — the screen proves
non-noisiness, not detection power; a "selective firer" has yet to be seen.

### t4fix4 — the full chain works end to end (runs/t4fix4_20260715_075330)

(t4fix3 was invalidated — it hit the residual run_suite path bug and ran on
inline defaults; 45k tokens burned, bug then fully fixed: cases file is
always absolutized, fatal if missing.)

**t4fix4 (39.4k tokens): TP + TN, and the TP is a NAMED synthesized-relation
detection.** Every stage ran for the first time on the overfit leg:
- Extraction fixed: `getMinimumValue` recovered (changed-line patch parse);
  no DEGRADED warning. (The Arja patch's trailing-space header still prints
  a harmless `error: ... No such file or directory` during application —
  GNU patch recovers; cosmetic only.)
- Synthesis grounded: all 4 candidates are about `getMinimumValue`
  semantics (vs the generic `Partial.with` relations when the function was
  missing).
- Screen: 4 kept (all silent on buggy — CORRECT here: the buggy build's
  `getMinimumValue` is fine, the Arja patch is what breaks it; silent
  survivors are precisely the regression-catchers), top 3 injected.
- Fired on the patched build: `relation min-partial-values-is-one violated:
  expected=1 actual=24 hour=0` — the injected relation catching the exact
  defect (`getWrappedField().getMaximumValue()+1 = 24`).
- Verifier actually reviewed (model threaded, `[verifier] model=gpt-5.4`,
  zero 404s) and KEPT both findings, with the exception-skip guidance
  visibly applied ("the harness swallows any exception here, so this
  finding is only about a completed return value").
Time-4 has gone from recurring FN (verifier over-kill + broken extraction)
to a direct, named, verifier-upheld detection. Both fuzz attempts crashed
the patch (2/2, previously 1/2-1/3).

Screen semantics note for future readers: the keep-criterion is "not
indiscriminate on buggy", NOT "fires on buggy" — silent-on-buggy survivors
catch patch-introduced regressions (as here); selective firers would catch
incomplete fixes. Both are wanted.

### Priority order (evidence-based)
1. ~~Verifier replay~~ done — over-kill fixed; leak documented. Next
   verifier iteration: ensemble A/B via replay (cheap).
2. Eval-set expansion (certify-detectable semantic overfits; current 8-bug
   set quantizes recall at 12.5%/bug with 2 undetectable) — `eval_candidates.py`
   then `certify_detectability.py`, validating on Time-4 (>0) / Lang-22 (0).
3. ~~t4fix: first true end-to-end~~ done (t4fix4, above).
4. Then a semantic8 iteration batch with the new stack (this measures the
   CURRENT numbers — everything in §4 predates the overhaul); diag24
   flagship sweep last, as final confirmation.

### Eval-expansion + certifier validation + ensemble A/B (same day, +112k)

- **Expansion pool** (`eval_candidates.py`, free, 12s): 392 semantic
  candidate patches, 74 distinct semantic bugs — 66 new, **25 with both an
  overfitting and a correct patch** (the paired-design pool). Files at
  hetzner `scratch/eval_expansion/candidates.jsonl`.
- **Certifier known-answer test PASSED — and overturned a triage verdict.**
  Time-4/Arja: 1,764 divergences (certified). Lang-22/Arja: 0/909 (correctly
  not certified). **Chart-1/Arja patch1: 472 divergences on natural-path
  probes** — the patch clobbers `itemLabelGeneratorList` as a side effect;
  the earlier "likely undetectable" triage most plausibly examined a
  DIFFERENT one of the six Arja Chart-1 patch files (old batches sampled
  patch files randomly; suites pin patch1 via `head -1`). Ceiling on the
  8-bug set may be 7/8, and the Chart-1 FN is potentially addressable.
  Certifying the other five Chart-1 patches ≈ 50k tokens if wanted.
- **Ensemble verdict: keep votes=1.** votes=3 halved over-kill (25%→17%,
  thin sample) but the LEAK rate did not move at all (80%, same 4 cases) at
  3× cost — those oracles are judged sound by every lens because they ARE
  sound-looking; soundness review cannot catch them by construction.
  The chart26_o over-kill turned out to be defensible reasoning (oracle
  fired on an empty-label edge case) → fixed prompt-side instead: new
  FENCE DEGENERATE INPUTS hygiene rule in `_metamorphic_block` (assert only
  on non-degenerate-by-construction inputs unless the contract explicitly
  covers the degenerate case).
- **`.env` deployment fixed**: `AZURE_OPENAI_DEPLOYMENT` gpt-5.2 (dead,
  the source of the verifier 404 fail-open) → gpt-5.4, synced to VM.

**HELD (deliberate, 2026-07-15):** the two big runs are queued but not
launched — certification sweep over the 25 paired new bugs (~250-350k
tokens) and the semantic8 post-overhaul baseline (~300-350k). Launch order
when resumed: certification first.

Cost of the whole 2026-07-15 pm effort: ~330k tokens gpt-5.4 (replay,
4 suite runs — one invalidated by the path bug — certifier validation,
ensemble A/B), all itemised in the suite summary.md files.
