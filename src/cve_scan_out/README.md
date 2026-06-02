# `cve_scan_out/` — outputs

All files here are produced by the `cve_scan` package. Each phase's output
is independent JSON/CSV so you can re-run a single phase without
re-running the others (their disk caches under `~/.cache/cve_scan/`
handle that).

## Pipeline order

```
        run_p0_harvest                run_diff_relate            run_inspect_unsure
              │                              │                            │
              ▼                              ▼                            ▼
       candidates.json              deep_relate.json             codebase_audit.json
       seeds.json
       seeds.csv
       seeds_table.md
```

## Files

| File | What it is | Produced by | Row count |
|------|------------|-------------|-----------|
| `candidates.json` | Every `(later, prior)` pair extracted from the Project Zero sources before any verification. Each row carries the evidence quote(s), vendor/product (when from sheet), and any commit/advisory URLs noticed in the prose. | Phase 1 always | typically ~200 |
| `seeds.json` | Same set of pairs after running the two rough verifiers (prose-LLM + URL/file-overlap). Adds `confirmed` (the union signal), per-signal verdicts (`llm_*`, `overlap_*`), and the patches' commit URLs. | Phase 1 always | same as candidates |
| `seeds.csv` | Flat view of confirmed-only seeds with one row per pair. Useful for spreadsheet inspection. | Phase 1 always | ~100 |
| `seeds_table.md` | Human-readable Markdown table of confirmed seeds, grouped by evidence strength (STRONG / MEDIUM / UNSURE). Generated from `seeds.json` + `deep_relate.json`. | Helper script (`make_seeds_table.py`) | ~100 |
| `deep_relate.json` | Per-pair verdict from the deep diff-relatability LLM pass. For each confirmed seed, records whether both patches were fetchable and (when so) the LLM's read of the actual diffs. | Phase 2 always | one per confirmed seed |
| `deep_relate.csv` | Flat view of only the deep-related pairs (`diff_related=True`). | Phase 2 always | typically tiny (few rows) |
| `codebase_audit.json` | Cross-checks every confirmed seed's LLM `same_codebase` claim against the P0 sheet's Vendor/Product columns and the bug-id prefix conventions. Flags claims the heuristic disagrees with. | Phase 3 always | one per confirmed seed |
| `README.md` | This file | manually | n/a |

## How to read each row

### `seeds.json` schema (per row)

```json
{
  "later_cve":      "CVE-2022-1232",
  "prior_cve":      "CVE-2022-1096",       // may be bug-tracker id e.g. "chromium-p0:2280"
  "evidence":       [{"url": "...", "quote": "...", "source_kind": "rca|narrative|sheet"}],
  "sheet_year":     2022,
  "vendor":         "Google",
  "product":        "Chrome",
  "confirmed":      true,                  // llm_confirmed OR overlap_status=="overlap"
  "llm_confirmed":         true,           // prose verifier verdict
  "llm_relationship_kind": "incomplete_fix",
  "llm_same_codebase":     true,
  "llm_is_incomplete_fix_cause": true,
  "llm_confidence":        0.95,
  "llm_reasoning":         "The RCA explicitly states ...",
  "llm_best_evidence_url": "https://github.com/.../CVE-2022-1096.md",
  "llm_cited_sentence":    "CVE-2022-1096 was incompletely fixed.",
  "overlap_status":  "no_patches",         // overlap | no_overlap | partial | no_patches
  "overlap_files":   [],
  "later_patch_url": null,
  "prior_patch_url": null,
  "upstream_commits":     [...],
  "upstream_advisories":  [...]
}
```

### `deep_relate.json` schema (per row)

```json
{
  "later_cve":      "CVE-2020-6820",
  "prior_cve":      "mozilla:1655115",
  "later_patch_available": true,
  "prior_patch_available": true,
  "later_patch_url": "https://...",
  "prior_patch_url": "https://...",
  "later_files":    [".../path.cpp", ...],
  "prior_files":    [".../path.cpp", ...],
  "diff_related":   true,
  "diff_kind":      "one_extends_other",   // incomplete_fix_confirmed | same_root_cause_confirmed | one_extends_other | unrelated | insufficient_data
  "confidence":     0.90,
  "shared_files":   [".../path.cpp"],
  "cited_change":   "Subject: [PATCH] ...",
  "reasoning":      "Both patches apply the same changes ...",
  "skip_reason":    "",                    // e.g. "self-pair (later and prior resolved to same commit)"
  "llm_cached":     false,
  "llm_cost_usd":   0.0021
}
```

### `codebase_audit.json` schema (per row)

```json
{
  "later":              "CVE-2020-27930",
  "prior":              "CVE-2020-16009",
  "llm_same_codebase_claim": true,
  "llm_confidence":     0.75,
  "deep_kind":          null,
  "later_codebase":     "apple-ios",       // inferred from sheet/RCA
  "later_source":       "sheet (Apple/iOS)",
  "prior_codebase":     "chrome",
  "prior_source":       "sheet (Google/Chrome)",
  "same_codebase_observed": false,
  "agrees_with_llm":    false,
  "verdict":            "disagrees",       // agrees | disagrees | unknown_codebase
  "note":               "LLM said same_codebase=True but we infer different codebases (apple-ios vs chrome)."
}
```

## What to look at first

- **For the dataset**: `seeds.csv` (the confirmed pairs) and `seeds_table.md`
  (Markdown view grouped by evidence strength).
- **For LLM disagreements worth manual review**:
  `codebase_audit.json` filtered to `verdict == "disagrees"`.
- **For deep code-level confirmations**:
  `deep_relate.csv` (only related rows).
