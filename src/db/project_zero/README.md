# `project_zero/` — Project Zero variant-pair dataset

Harvests CVE-sibling pairs from Project Zero's public surfaces and
verifies them through a layered pipeline (prose-LLM verifier +
URL/file-overlap verifier + deep gpt-5-mini diff-relatability +
codebase audit).

## Layout

```
project_zero/
├── README.md                  ← this file
├── cve_scan/                  Python harvester package
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
└── findings/                  Pipeline output (the dataset itself)
    ├── README.md                output schemas, what each file holds
    ├── seeds_table.md           primary human-readable report
    ├── pipeline/                machine-readable JSON/CSV
    └── verified/                human-curated verification + URLs
```

## Input sources

All three are pulled by `cve_scan/p0_harvest.py`. See
[cve_scan/README.md](cve_scan/README.md) for the per-source extraction
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
    -m cve_scan.run_p0_harvest --budget-usd 1
uv run --no-project --with openai --python 3.12 \
    -m cve_scan.run_diff_relate --budget-usd 1
uv run --no-project --with openai --python 3.12 \
    -m cve_scan.run_inspect_unsure
uv run --no-project --with openai --python 3.12 \
    -m cve_scan.make_seeds_table
```

The bucketed Markdown report at
[findings/seeds_table.md](findings/seeds_table.md) is the primary
human-readable deliverable.
