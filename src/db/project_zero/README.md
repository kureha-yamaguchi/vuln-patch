# `project_zero/` — Project Zero variant-pair dataset

Harvests CVE-sibling pairs from Project Zero's public surfaces and
verifies them through a layered pipeline (prose-LLM verifier +
URL/file-overlap verifier + deep gpt-5-mini diff-relatability +
codebase audit).

## Data flow

Two stages. `discover/` **finds and verifies** the pairs (the extra step
linux_kernel doesn't need, since its seeds came pre-made from Liu et al);
`tools/` **packages** the verified result into the per-pair dataset. Same
role `linux_kernel/tools/` plays there.

```
  discover/  ──►  findings/  ──►  tools/   ──►  pairs/
  mine P0 +       the verified    materialize    per-pair
  verify pairs    pair report     the dataset    patches + metadata
```

## Layout

```
project_zero/
├── README.md                  ← this file
├── discover/                  STAGE 1 — discovery + verification engine
│   ├── README.md                pipeline phases, module map, run order
│   ├── config.py                env-driven config (sheet ID,
│   │                            narrative URLs, model, budget)
│   ├── classifier.py            OpenAI client + cache + budget cap
│   ├── code_overlap.py          patch fetchers + overlap computation
│   ├── p0_harvest.py            source fetchers + pair extraction
│   ├── diff_relate.py           deep diff-relatability LLM
│   ├── inspect_unsure.py        codebase-claim audit
│   ├── make_seeds_table.py      Markdown report generator
│   └── run_*.py                 CLIs
├── findings/                  output of stage 1 (the verified pair list)
│   ├── README.md                output schemas, what each file holds
│   ├── seeds_table.md           primary human-readable report
│   ├── pipeline/                machine-readable JSON/CSV
│   └── verified/                human-curated verification + URLs
├── tools/                     STAGE 2 — dataset materialization
│   ├── build_pairs.py           findings/ + patch cache → pairs/
│   ├── fetch_context.py         touched files of each fix, at that commit
│   └── resolve_gerrit.py        CL number → merged SHA (writes the file below)
├── gerrit_resolved.json       {"CL/1888103": "<sha>"} — written by the tool
│                              above, read by fetch_context.py. Absent until
│                              resolve_gerrit.py runs.
├── bug_kind.jsonl             crashing or semantic, one row per distinct fix.
│                              Written by baseline_llmjudge.project_zero.bugkind.
└── pairs/                     the dataset — one dir per READY pair
    ├── README.md                layout + metadata.json schema
    └── <PRIOR>__<LATER>/        fix0.patch, fix1.patch, metadata.json,
                                 fix0_context/, fix1_context/
```

## Stage 3 — the source of each fix

`tools/fetch_context.py` fetches the files each fix touches, at that fix's own
commit. It writes `fix0_context/` and `fix1_context/` per pair, the same layout
`linux_kernel/pairs/` uses. Only the directly modified files are fetched. That
is enough for a one-shot LLM judgement, and it is not enough for harness
generation.

**A Gerrit change number blocks a fetch.** A raw-file endpoint needs a git SHA.
21 of the 43 `fix0_commit` values are change numbers such as `CL/1888103`. Run
`tools/resolve_gerrit.py` first for those pairs. It writes a separate override
file and never rewrites `metadata.json`.

```bash
uv run python tools/fetch_context.py --dry_run    # list the reachable pairs
uv run python tools/fetch_context.py              # tier 1: 14 pairs
uv run python tools/resolve_gerrit.py             # v8 change numbers
uv run python tools/fetch_context.py --tier all
```

The evaluation that consumes this output is
[`src/baseline_llmjudge/README.md`](../../baseline_llmjudge/README.md),
section 11.

## Input sources

All three are pulled by `discover/p0_harvest.py`. See
[discover/README.md](discover/README.md) for the per-source extraction
strategy.

| Source | URL | What it contributes |
|---|---|---|
| 0-day Google Sheet (year-tabs) | https://docs.google.com/spreadsheets/d/1lkNJ0uQwbeC1ZTRrxdtuPLCIl7mlUreoKfSIgajnSyY/ | Authoritative CVE list with vendor / product / RCA links |
| 0days-in-the-wild repo | https://github.com/googleprojectzero/0days-in-the-wild | Per-CVE root-cause-analysis Markdown |
| Mind the Gap | https://projectzero.google/2022/11/mind-the-gap.html | Full-year 2022 variant analysis |
| 2022 0-day…so far | https://projectzero.google/2022/06/2022-0-day-in-wild-exploitationso-far.html | H1 2022 variants |
| Déjà vu-lnerability | https://projectzero.google/2021/02/deja-vu-lnerability.html | 2020 year-in-review, densest variant-pair source |
| Detection Deficit | https://projectzero.google/2020/07/detection-deficit-year-in-review-of-0.html | 2019 year-in-review |
| rca.html | https://googleprojectzero.github.io/0days-in-the-wild/rca.html | Rendered RCA index |

## Quick start

From this directory (`src/db/project_zero/`):

```bash
source ~/.zshrc
export GITHUB_TOKEN="$(gh auth token)"

uv run --no-project --with openai --python 3.12 \
    -m discover.run_p0_harvest --budget-usd 1
uv run --no-project --with openai --python 3.12 \
    -m discover.run_diff_relate --budget-usd 1
uv run --no-project --with openai --python 3.12 \
    -m discover.run_inspect_unsure
uv run --no-project --with openai --python 3.12 \
    -m discover.make_seeds_table
```

The bucketed Markdown report at
[findings/seeds_table.md](findings/seeds_table.md) is the primary
human-readable deliverable.
