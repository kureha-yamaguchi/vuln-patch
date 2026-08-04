# docs/ — what each file is and whether it's alive

Two kinds of documents live here: **living** (updated as work happens; read these first)
and **frozen** (historical evidence; never updated, kept because other files cite them).

## Living

- **`plan.md`** — THE plan doc, restructured 2026-07-31 to live content only: the
  problem + pipeline stations, ground rules, the standing rulebook, current state,
  the rejected-ideas ledger, the current measurement protocol, parked pointers, and
  the active cycle's execution plan. When a commit message says "plan doc", it means
  this file. Everything it accumulated before the restructure is in `plan-history.md`.
- **`plan-history.md`** — frozen: the by-station era (07-18→20, incl. full P4.1/P4.2
  specs and all batch results) and the cycle era (07-26→30, pre-commitments,
  adjudications, kill records), moved verbatim out of plan.md.
- **`replay/`** — per-measurement analysis + raw-result docs (gates, iterations,
  night20b/c, smokes), committed raw-before-scoring.
- **`cycles/`** — the improvement-campaign log, one file per cycle (specs + outcome)
  plus the numbered retrospectives. The standing retro protocol is defined at the
  bottom of `RETRO1-2026-07-25.md`. Current state of play: `RETRO3-2026-07-25.md`
  (variance is the problem; cycle-4 stability plan).

## Frozen — reference only

- **`what-we-tried-2026-08-03.md`** — the plain-English onboarding retrospective:
  every change attempted between Kureha's last commit (2026-06-09) and 2026-08-03,
  grouped by era, each written as *which station · what it's for · why the edit ·
  expected benefit · actual outcome*, with the worked/rejected/undecided master
  tables and the methodology rules. Written for someone with no prior exposure to
  the project. Not authoritative for the current design — `plan.md` is.
- **`early-analysis-2026-07-04.md`** — the oldest layer: the June-29 critique notes
  and the per-run readthrough, merged and frozen (moved out of `src/java/`
  2026-07-31). Covers the era before `progress.md` starts; several of its claims are
  now false and the banner lists them.
- **`progress.md`** — the June-15→July-15 era record (pre-overhaul through the A1–A7
  batch and B1 mislabel probe). Frozen; cited by `suites/DATASET_AUDIT.md` and the
  certifier's docstrings.
- **`commit-audit.md`** — full review of the first 152 commits (through 2026-07-19):
  validated / unvalidated / delete lists. Several deletion candidates it names are
  still pending.
- **`crashing-bug-exposure-2026-07-31.md`** — what the semantic-bug cycles changed
  under the crashing-bug path: shared vs semantic-only components, four unmeasured
  risks, and the recommended checks. Hand this to anyone picking up crashing bugs.
- **`judge-verdict-inventory-2026-07-26.md`** — population inventory of all 228 judge
  verdicts on fired checks across the five pool-era runs; the fixture source for any
  judge-side change (cycle-5 package in `plan.md` is derived from it).
- **`abc-analysis-unified.md`** — the adjudicated analysis of the 3-arm fresh-bug
  experiment (2026-07-21), source of the G1–G5 fix plan. The two independent
  deep-dives it merged were deleted 2026-07-26 (git history has them).

## Where other truth lives

- Dataset inventory, labels, splits: `suites/` (`DATASET_AUDIT.md`, `labels/`,
  `splits/`, `README`).
- Run evidence: `runs-archive/runs/<name>/` (one `trace.md` per leg + `summary.md`),
  certification evidence in `runs-archive/certification/`.
- Standing user rules (no cross-run pooling, no dataset overfitting, run-log
  isolation, …): the project memory, mirrored where relevant into the plan's ground
  rules.
