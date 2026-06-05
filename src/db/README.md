# `db/` — CVE-sibling datasets

This directory holds the project's two CVE-sibling datasets — one
harvested from Project Zero's public surfaces, one imported from the
Liu et al. (arXiv:2511.17799) Linux-kernel study. Both encode the same
underlying phenomenon: **a patch that was not properly fixed and so a
follow-up CVE was needed.** Every pair in either dataset is, by
definition, an example of a *semantically incorrect* security patch:
the original fix didn't eliminate the bug class, and reality proved
that by producing a second CVE in the same code.

## Layout

```
db/
├── README.md                    ← this file
│
├── project_zero/                ← Harvested from Project Zero
│   ├── README.md                  (input sources, pipeline phases)
│   ├── cve_scan/                  the Python harvester package
│   └── findings/                  the dataset it produces
│       ├── README.md
│       ├── seeds_table.md         primary human-readable report
│       ├── pipeline/              machine-readable JSON/CSV
│       └── verified/              human-curated verification + URLs
│
└── linux_kernel/                ← Linux-kernel ground truth (Liu et al.)
    ├── README.md
    ├── liu_seeds.json             source-of-truth pair list from
    │                              arXiv:2511.17799 (zenodo 6423844)
    ├── tools/                     scripts that fetch patches / context
    │                              for each pair
    └── pairs/                     one subdir per CVE pair, with
                                   metadata.json + fix0/fix1 patches +
                                   affected-file context
```

## Why two datasets

| dimension | `project_zero/` | `linux_kernel/` |
|---|---|---|
| Source | PZ Google Sheet + RCA repo + narrative posts | Liu et al. arXiv:2511.17799 |
| Scope | Vendor-mixed (Chrome, Mozilla, Microsoft, Apple, Mali, Qualcomm, ...) | Linux kernel only |
| Acquisition | Automated harvester (this repo) | Manual download |
| Size | 120 confirmed pairs, 24 source-complete | 26 hand-curated pairs |
| Open-source coverage | ~24 with full source on both sides | All 26 with full source |
| Bug-class diversity | Wide (web, browser engines, GPU drivers, kernel, fonts) | Kernel subsystems |

Together they give roughly **50 pairs with both fix-commit patches and
the affected-file context** — the actionable set for downstream
semantic-incorrectness analysis.

## Running the pipeline

```bash
cd src/db/project_zero
source ~/.zshrc                                  # for OPENAI_API_KEY
export GITHUB_TOKEN="$(gh auth token)"            # for GitHub API rate limits

# Phase 1 — harvest + rough verify
uv run --no-project --with openai --python 3.12 \
    -m cve_scan.run_p0_harvest --budget-usd 1

# Phase 2 — deep diff-relatability
uv run --no-project --with openai --python 3.12 \
    -m cve_scan.run_diff_relate --budget-usd 1

# Phase 3 — codebase audit + Markdown table
uv run --no-project --with openai --python 3.12 \
    -m cve_scan.run_inspect_unsure
uv run --no-project --with openai --python 3.12 \
    -m cve_scan.make_seeds_table
```

Outputs land in `db/project_zero/findings/` relative to the CWD.
