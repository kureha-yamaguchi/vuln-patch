# suites/ — eval task sets, dataset audit, and label truth

## Layout
- **`labels/`** — AUTHORITATIVE per-(bug,tool,patch) label truth (see `labels/README.md`).
  `verified_correct.jsonl` / `verified_incorrect.jsonl` / `excluded.jsonl`, kind-tagged.
- **`DATASET_AUDIT.md`** — master inventory of the drr Java dataset + audit methodology
  + human-readable verdict tables (the narrative behind `labels/`).
- **`UNDETECTABLE.md`** — detailed equivalence / probe-false-zero writeups.
- **`pinned_tasks.jsonl`** — the pinned semantic dev task set (canonical; only copy).
- **`cases/`** — active `run_suite.sh` configs:
  - `pinned_dev.cases` — the semantic dev set
  - `full30v2.cases` — the 30-leg full eval
  - `ab_off.cases` / `ab_on.cases` — the flags-off-vs-on A/B (2026-07-21)

Spent one-off experiment `.cases` (batch*/attr*/foc*/rulegen*/minfix* etc.) were
deleted 2026-07-21 — they are preserved in git history if ever needed.

## Label coverage (2026-07-21)
117 bugs have a Dcorrect/Doverfitting patch. **91 are labeled** in `labels/`
(overfit side: all 92 scoreable patches certified + deep-dived, plus the
§3a-audited singletons). **25 are uncovered** (unpaired or unlabeled, never
audited): Chart-4/13/14/17/24, Closure-40, Lang-33/35/57/61, Math-3/4/8/28/31/
49/60/61/81/89/90/97/98, Time-7/14.
