# docs/ — what each file is and whether it's alive

Two kinds of documents live here: **living** (updated as work happens; read these first)
and **frozen** (historical evidence; never updated, kept because other files cite them).

## Living

- **`plan.md`** — THE plan doc (formerly `semantic-recall-brainstorm.md`, renamed
  2026-07-26 because it long ago stopped being a brainstorm). Ground rules, finished
  work by pipeline station, current scoreboard, remaining work ranked by
  impact-vs-risk, rejected ideas, and the dated candidate ledgers at the end. When a
  commit message says "plan doc", it means this file.
- **`cycles/`** — the improvement-campaign log, one file per cycle (specs + outcome)
  plus the numbered retrospectives. The standing retro protocol is defined at the
  bottom of `RETRO1-2026-07-25.md`. Current state of play: `RETRO3-2026-07-25.md`
  (variance is the problem; cycle-4 stability plan).

## Frozen — reference only

- **`semantic-recall-history.md`** — the July-16 failure-analysis write-up plus the
  full pre-restructure plan text (archived verbatim when the plan was rewritten
  by-station on 2026-07-18). The forensic per-bug case histories live here.
- **`progress.md`** — the June-15→July-15 era record (pre-overhaul through the A1–A7
  batch and B1 mislabel probe). Frozen; cited by `suites/DATASET_AUDIT.md` and the
  certifier's docstrings.
- **`commit-audit.md`** — full review of the first 152 commits (through 2026-07-19):
  validated / unvalidated / delete lists. Several deletion candidates it names are
  still pending.
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
