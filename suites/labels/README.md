# Label verification — authoritative per-(bug,tool,patch) truth for the drr Java dataset

The drr dataset's directory labels (`Dcorrect/` vs `Doverfitting/`) are NOT
always right. A patch is only usable in the eval if its label matches its
actual behaviour vs the DEVELOPER FIX (defects4j `<id>f`). These files are the
single machine-readable source of that truth. **Do not re-derive labels
elsewhere — consume these.**

## SCOPE: SEMANTIC ONLY (crashing moved to labels/crashing/)
The detection pipeline (and the eval split) is for **semantic** bugs. The
certification itself is kind-agnostic, so the sweep also touched a few
crashing bugs. Every row carries a `kind` field: `semantic` | `crashing` |
`unknown` (the 15 fresh bugs not in the July semantic audit — run a quick
`defects4j` kind check before using them in a semantic eval). **Filter to
`kind=="semantic"` when building the semantic eval.**

## Files (machine-readable, one JSON object per triplet)
- **`verified_correct.jsonl`** — the drr label is CONFIRMED CORRECT:
  a `Doverfitting` patch behaviourally distinct from the dev fix (detectable),
  or a `Dcorrect` patch behaviourally == the dev fix.
- **`verified_incorrect.jsonl`** — the drr label is WRONG (fix before scoring):
  `equivalent_to_dev_fix` = an "overfit" that actually equals the dev fix;
  `actually_overfitting` = a "correct" patch that is really overfit.
  Each row has `basis` (the evidence) and `verification` (pointer to the run
  / witness / narrative that proves it) — per requirement.
- **`excluded.jsonl`** — real but UNUSABLE: `detectable_but_no_sound_oracle`
  (divergence exists only via a dev-fix bug / formatting / env ceiling) or
  `deprecated_bug` (not in the installed Defects4J).
- `_source_july_annotations.jsonl.deprecated` — the old July mislabel list,
  now fully merged into `verified_incorrect.jsonl`. Kept for provenance only.

### Row schema
`{project, bug_id, apr_tool, patch, kind, drr_label, verdict|correct_label,
  label_error, basis, verification}`

## Coverage (as of 2026-07-21)
- OVERFIT side: ALL 92 overfit patches of the 50 scoreable bugs certified
  (probe) + every div=0 deep-dived. COMPLETE.
- CORRECT side: one pinned patch per bug (DATASET_AUDIT §3b) — NOT an
  exhaustive per-patch sweep of correct files yet.

## Where the EVIDENCE lives (referenced by each row's `verification`, not duplicated here)
- `suites/DATASET_AUDIT.md` — master inventory + methodology + human-readable verdict tables.
- `suites/labels/incorrect_labels.md` — detailed equivalence / false-zero writeups (July).
- `runs-archive/certification/2026-07-21_scoreable-overfits/` — the 92-overfit
  probe (`overfit_detectability.jsonl`), worker logs, and `deepdive_verdicts.md`
  (the 16 per-patch deep-dives with witnesses).
- `runs-archive/certification/2026-07-15to17_eval-set-curation/` — the July sweep artifacts.

## How to consume (eval denominators)
Recall denominator = `verified_correct.jsonl` rows with `drr_label=="overfitting"`
and `kind=="semantic"`. Precision set = `verified_correct.jsonl` `drr_label=="correct"`.
Drop everything in `verified_incorrect.jsonl` and `excluded.jsonl`.
