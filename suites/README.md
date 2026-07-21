# suites/ — eval task sets, dataset audit, and label truth

## Layout
- **`labels/`** — AUTHORITATIVE per-(bug,tool,patch) label truth (see `labels/README.md`).
  `verified_correct.jsonl` / `verified_incorrect.jsonl` / `excluded.jsonl`, kind-tagged.
- **`DATASET_AUDIT.md`** — master inventory of the drr Java dataset + audit methodology
  + human-readable verdict tables (the narrative behind `labels/`).
- **`suites/labels/incorrect_labels.md`** — detailed equivalence / probe-false-zero writeups.
- **`pinned_tasks.jsonl`** — the pinned semantic dev task set (canonical; only copy).
- **`cases/`** — active `run_suite.sh` configs:
  - `pinned_dev.cases` — the semantic dev set
  - `full30v2.cases` — the 30-leg full eval
  - `ab_off.cases` / `ab_on.cases` — the flags-off-vs-on A/B (2026-07-21)

Spent one-off experiment `.cases` (batch*/attr*/foc*/rulegen*/minfix* etc.) were
deleted 2026-07-21 — they are preserved in git history if ever needed.

## Label coverage (2026-07-21)
117 bugs have a Dcorrect/Doverfitting patch (74 semantic, 43 crashing).
**`labels/` is now SEMANTIC-ONLY: 74 semantic bugs labeled.** The 18 crashing
bugs that had been certified alongside them were moved to **`labels/crashing/`**
(kept, but out of the semantic lists). Kind is authoritative: from
`pinned_tasks.jsonl`'s `bug_kind` and, for the rest, the Defects4J trigger-test
root cause (real exception = crashing, assertion failure = semantic).

**Scope caveat:** the audit and this label set are effectively a SEMANTIC
effort. Crashing bugs are a separate pool (43 total, see DATASET_AUDIT §7)
and are MOSTLY UNCERTIFIED — only 4 crashing bugs happen to be labeled here
because they were scoreable. Filter to `kind=="semantic"` for the semantic eval.

Verified coverage is asymmetric: OVERFIT side is complete (all patches of
every scoreable bug), CORRECT side was one-pinned-patch-per-bug (full
152-patch sweep 2026-07-21 in progress).

**25 bugs uncovered** (unpaired or unlabeled, never audited): Chart-4/13/14/17/24,
Closure-40, Lang-33/35/57/61, Math-3/4/8/28/31/49/60/61/81/89/90/97/98, Time-7/14.
