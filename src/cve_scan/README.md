# `cve_scan` — variant-CVE harvester

Mines Project Zero's public surfaces for **CVEs that are variants of
previously-patched vulnerabilities** — same-root-cause bugs, regressions,
and especially patches that weren't comprehensive enough. Produces a
labelled seed dataset for downstream code-level analysis.

## Pipeline

The package is three sequential phases, each runnable as its own CLI:

```
  harvest         deep-relate         codebase-audit
     │                │                     │
     ▼                ▼                     ▼
 candidates.json   deep_relate.json    codebase_audit.json
 seeds.json
 seeds.csv
```

### 1. Harvest + rough verify — `cve_scan.run_p0_harvest`

Pulls every input source:

| Source | What it contributes |
|--------|---------------------|
| **P0 0-day Google Sheet** (year-tabs via `gviz/tq?tqx=out:csv`) | Authoritative CVE list with vendor / product / RCA links |
| **`googleprojectzero/0days-in-the-wild` repo** | Per-CVE root-cause-analysis Markdown — variant relationships in prose |
| **Narrative blog posts** (Mind the Gap, Déjà vu-lnerability, Detection Deficit, H1-2022 update, rca.html) | Editorial enumerations of named variant pairs, often as HTML tables |

Then runs two **rough verification signals in parallel** on every candidate:

1. **Prose-LLM verifier** — sends `(later, prior, evidence-quote)` to
   `gpt-5-mini` with a structured-output schema. Classifies as
   `incomplete_fix | regression | same_root_cause | exploit_chain |
   see_also | unrelated`, plus `same_codebase`.
2. **URL/file-overlap verifier** — resolves each side's fix patch via
   GitHub commit URLs found in RCAs, GitHub commit-search fallback,
   Bugzilla attachments, Gerrit (chromium-review), or git.kernel.org;
   parses the unified diff for touched files; computes intersection.

A pair is `confirmed=True` if EITHER signal is positive (LLM = strict
incomplete_fix / regression / same_root_cause-with-same_codebase, OR
file-level overlap on non-boilerplate paths).

### 2. Deep diff-relate — `cve_scan.run_diff_relate`

For each confirmed seed where both sides' patches were fetched, sends
the actual unified diffs (truncated to ~3k tokens each) to `gpt-5-mini`
with the LLM-prose context. Classifies code-level relatedness:
`incomplete_fix_confirmed | same_root_cause_confirmed | one_extends_other
| unrelated | insufficient_data`.

### 3. Codebase audit — `cve_scan.run_inspect_unsure`

Cross-references each pair's LATER CVE against the P0 sheet's
Vendor/Product columns and each PRIOR identifier against bug-tracker
prefixes to verify the LLM's `same_codebase` claim heuristically.

## Module map

| File | Responsibility |
|------|----------------|
| `config.py` | All env-driven config (sheet ID, narrative URLs, model, cache dirs, budget, pricing) |
| `classifier.py` | OpenAI client with on-disk response cache + budget cap |
| `code_overlap.py` | Patch fetchers (GitHub, kernel.org, Gerrit, Bugzilla), GitHub commit-search fallback, repo denylist, file-overlap computation |
| `p0_harvest.py` | Source fetchers (sheet, RCA repo, narratives), pair extraction (sheet cells, RCA prose, narrative sentences, narrative HTML tables), LLM verify loop |
| `diff_relate.py` | Deep gpt-5-mini diff-relatedness LLM, patch truncation |
| `inspect_unsure.py` | Codebase-claim audit |
| `run_p0_harvest.py` | CLI for phase 1 |
| `run_diff_relate.py` | CLI for phase 2 |
| `run_inspect_unsure.py` | CLI for phase 3 |

## Run order

```bash
cd src
source ~/.zshrc                                  # for OPENAI_API_KEY
export GITHUB_TOKEN="$(gh auth token)"            # for GitHub API rate limits

# Phase 1 — harvest + rough verify (~10 min with cold cache, < 1 min after)
uv run --no-project --with openai --python 3.12 \
  -m cve_scan.run_p0_harvest --budget-usd 1

# Phase 2 — deep diff-relatability (~2 min on the small subset with both patches)
uv run --no-project --with openai --python 3.12 \
  -m cve_scan.run_diff_relate --budget-usd 1

# Phase 3 — codebase audit (instant, no LLM)
uv run --no-project --with openai --python 3.12 \
  -m cve_scan.run_inspect_unsure
```

Outputs land in `./cve_scan_out/`; see `cve_scan_out/README.md` for the
per-file schema.

## Why `uv --no-project`

The parent `vuln-patch` project's `pyproject.toml` pulls `fuzz-introspector`
which transitively depends on `atheris`, which fails to build on Apple
Silicon without a custom LLVM. The harvester only needs `openai`, so
`uv run --no-project --with openai` sidesteps that.

## Known patch-resolution gaps

- **Chromium issue tracker** (`bugs.chromium.org/p/chromium`) now
  redirects to `issuetracker.google.com`, which doesn't statically
  scrape; many `chromium-p0:NNNN` and `chromium:NNNN` priors fail patch
  resolution unless the fix commit is also linked from an RCA.
- **MSRC / Apple security / Android Bulletin** patches are not on
  GitHub. No public diff to fetch.
- **GitHub commit-search** returns many commits from vulnerability-tracker
  repos that just *mention* a CVE ID; the package denylists known
  trackers and rejects diffs that only touch metadata files, but the
  long tail is not exhaustively covered.

These limit the deep-relate phase to ~15 pairs out of ~100 confirmed for
the current Project Zero corpus.
