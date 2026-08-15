# suites/splits — crashing dev/holdout split

`crashing_split.jsonl` — the frozen **60/40 bug-level split** of the certified
crashing pool. One row per bug: `{project, bug_id, side, overfit_legs,
correct_legs, used_in_tuning}`. Same schema as the semantic `semantic_split.jsonl`
(see `README.md`); this is the crashing counterpart.

Unlike the semantic split, this one **has a generator**:
`src/java/dataset/make_crashing_split.py` (deterministic; re-running reproduces
the file byte-for-byte).

## Method (frozen 2026-08-14, seed `20260814`)
- **Population:** the 18 crashing bugs with ≥1 usable certified patch in
  `suites/labels/crashing/verified_correct.jsonl` (`kind=="crashing"`). Mislabels
  (`verified_incorrect.jsonl`) and `excluded.jsonl` patches are dropped, so a bug
  counts only for the legs it can legitimately contribute. These are exactly the
  18 bugs of the 2026-07-21 correct-side sweep — the other **25 labelled crashing
  bugs are UNCERTIFIED** (`DATASET_AUDIT.md` §7) and are OUT of this split until
  they are certified.
- **Whole-bug holdout:** every patch of a bug goes to the same side — no
  patch-level leakage across dev/holdout.
- **Already-used bugs forced to DEV.** The crashing pipeline's tuning trail is
  **`logs/`** — the 9 experiment logs it was iterated on — NOT `suites/cases/*.cases`
  (that is the *semantic* tuning trail). 5 pool bugs appear there and are pinned
  to `dev`. Used set: **Lang-6, Lang-27, Lang-39, Lang-44, Lang-51**.
  (`logs/` also touches Lang-55 and Lang-61; neither is in the certified pool, so
  neither constrains the split. Two logs name only a trigger test — the generator
  recovers their bug id by matching it against defects4j's `trigger_tests` files.)
- **Stratified by project:** holdout target = `round(0.6 × project total)`, drawn
  (seeded shuffle, per-project seed so adding a project cannot reshuffle the
  others) from that project's **unused** bugs only.

### Why 0.6 and not the semantic split's 0.4
Two reasons, both consequences of the crashing pool being small and leg-poor:
1. Only **5 of 18** bugs are contaminated here (vs 16 of 70 semantic), so dev
   needs a much smaller share of the pool to do its job.
2. The whole pool carries only **29 overfit legs**. At 40% the holdout kept just
   6 of them across 5 bugs — too few to read a recall number off (the semantic
   holdout has 23). At 60% it keeps 14 across 8 bugs, while dev still retains 3
   untouched bugs (Chart-9, Math-32, Math-79) as smoke checks, mirroring the
   2 untouched pairs the semantic dev set deliberately kept.

## Result
| Side | Bugs | Overfit (recall) legs | Correct (precision) legs |
|---|---|---|---|
| **dev** (tune) | 8 (44%) | 15 | 17 |
| **holdout** (final eval) | 10 (56%) | 14 | 37 |

Per project (total / dev / holdout / used→dev): Chart 2/1/1/0 · Lang 11/5/6/5 ·
Math 5/2/3/0.

- **dev:** Chart-9, Lang-6\*, Lang-27\*, Lang-39\*, Lang-44\*, Lang-51\*, Math-32, Math-79
- **holdout:** Chart-5, Lang-16, Lang-20, Lang-43, Lang-45, Lang-58, Lang-59, Math-58, Math-70, Math-85

(\* = seen in `logs/`, forced to dev.) The holdout is **entirely unseen**
(0 used bugs — asserted by the generator, which fails if a tuned bug lands there).

## Known limits — read before quoting a number off this split
- **Recall denominator is 8 bugs / 14 legs.** Lang-20 and Math-70 carry no
  certified overfit patch, so they contribute to precision only. Treat holdout
  recall as a coarse signal, not a precise rate.
- **Math-70 is a known type-A equivalence** (`DATASET_AUDIT.md:572`): its
  SketchFix "overfit" IS the dev fix plus a dead disjunct. It sits in the holdout
  on its correct legs only (`overfit_legs=0`), so it cannot poison recall — but do
  not later "fix" its label without re-freezing.
- **25 crashing bugs are excluded for want of certification.** Certifying them
  (`DATASET_AUDIT.md` recommends ~70k tokens for the crash14 legs) would roughly
  triple the pool. That requires a **new dated seed and a re-freeze** — and a
  re-freeze re-leaks the current holdout, so do it before spending this one, not
  after.

## How to consume
- **Tune / iterate** only on `side=="dev"` bugs. Report the final, headline
  numbers on `side=="holdout"` — touch it as rarely as possible.
- Resolve each split bug's patches from `suites/labels/crashing/verified_correct.jsonl`
  (recall = `drr_label=="overfitting"`, precision = `drr_label=="correct"`); never
  source a patch listed in that directory's `verified_incorrect.jsonl` / `excluded.jsonl`.
- The crashing pipeline entry point is `scripts/evaluate_crashing.sh`, which is
  split-aware via `-s`:

  ```bash
  scripts/evaluate_crashing.sh -s dev        # tune / iterate here  (8 bugs, 32 patches)
  scripts/evaluate_crashing.sh -s holdout    # final numbers only  (10 bugs, 51 patches)
  ```

  With `-s` the queue is built from this file joined against
  `verified_correct.jsonl`, and **every** certified patch on that side runs —
  `SAMPLE_SIZE`/`SEED` do not apply, and the `classify_bugs.py` pre-filter is
  skipped (this split's population is already crashing-only). Run it with
  `DRY_RUN=1` to print the queue and stop.

  Without `-s` you get the legacy balanced random sample (`SAMPLE_SIZE=60`,
  `SEED=42`), which is **not** split-aware and straddles both sides — a smoke
  test, never a reported number.

  `-s` is a flag rather than an env var deliberately: an exported `SPLIT=holdout`
  would persist for a whole shell and leak the holdout into later runs unnoticed,
  and a mistyped *variable name* is undetectable — it would silently fall through
  to the random sample. A bad `-s` value hard-fails.
- Semantic bugs are out of scope here (see `semantic_split.jsonl` / `README.md`).

## Regenerating
`python3 src/java/dataset/make_crashing_split.py [--seed 20260814] [--dry-run]`.
Deterministic from the label truth + `logs/` at the seed above. If the certified
set or the tuning trail changes, re-freeze with a new dated seed and note it here
(don't silently reshuffle — that would re-leak the holdout).

**Run this by hand only.** `evaluate_crashing.sh -s` *reads* `crashing_split.jsonl`
and never regenerates it. The generator is deterministic given its inputs, but
those inputs are live: it globs `logs/` and reads the label files, so certifying
a bug or dropping in one experiment log changes the draw. Certifying a single
extra Math bug, for instance, moves Math-32 and Math-79 (the untouched dev smoke
checks) into the holdout and Math-70 out of it. Regenerating inside the eval path
would make that happen automatically, on a path nobody inspects.

Each `-s` run therefore copies the split it used into its output directory
alongside `split_provenance.txt` (the file's git commit), so an old result stays
interpretable after a future re-freeze.
