# suites/splits — semantic dev/holdout split

`semantic_split.jsonl` — the frozen **60/40 bug-level split** of the certified
semantic pool. One row per bug: `{project, bug_id, side, overfit_legs,
correct_legs, used_in_tuning}`.

## Method (frozen 2026-07-21, seed `20260721`)
- **Population:** the 70 semantic bugs with ≥1 usable certified patch in
  `suites/labels/verified_correct.jsonl` (`kind=="semantic"`). Mislabels
  (`verified_incorrect`) and `excluded` patches are dropped, so a bug counts
  only for the legs it can legitimately contribute.
- **Whole-bug holdout:** every patch of a bug goes to the same side — no
  patch-level leakage across dev/holdout.
- **Already-used bugs forced to DEV.** 16 semantic bugs that appear in the
  active tuning cases files (`suites/cases/*.cases`) have been *seen* during
  development, so they cannot be in the holdout — they are pinned to `dev`.
  Used set: Chart-3, Chart-7, Chart-26, Closure-33, Closure-62, Closure-73,
  Closure-92, Lang-7, Lang-41, Lang-50, Lang-60, Math-2, Math-53, Math-57,
  Time-4, Time-11.
- **Stratified by project:** holdout target ≈ 40% of each project's total
  bugs, drawn (seeded shuffle) from that project's **unused** bugs only. This
  keeps the holdout representative and guarantees it is entirely unseen.

## Result
| Side | Bugs | Overfit (recall) legs | Correct (precision) legs |
|---|---|---|---|
| **dev** (tune) | 43 (61%) | 39 | 92 |
| **holdout** (final eval) | 27 (39%) | 23 | 56 |

Per project (total / dev / holdout / used→dev): Chart 11/7/4/3 · Closure
13/8/5/4 · Lang 11/7/4/4 · Math 31/19/12/3 · Time 4/2/2/2.

The holdout is **entirely unseen** (0 used bugs — verified).

## How to consume
- **Tune / iterate** only on `side=="dev"` bugs. Report the final, headline
  numbers on `side=="holdout"` — touch it as rarely as possible.
- For a run's legs, resolve each split bug's patches from
  `verified_correct.jsonl` (recall = `drr_label=="overfitting"`, precision =
  `drr_label=="correct"`); never source a patch listed in
  `verified_incorrect.jsonl` / `excluded.jsonl`.
- Crashing bugs are out of scope here (see `suites/labels/crashing/`).

## Regenerating
Deterministic from the label truth + the used-bug list at the seed above. If
the certified set or the used-bug set changes, re-freeze with a new dated seed
and note it here (don't silently reshuffle — that would re-leak the holdout).
