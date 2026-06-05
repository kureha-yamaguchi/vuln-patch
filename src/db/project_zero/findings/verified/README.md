# `verified/` — human-curated verification + patch-URL resolution

The pipeline outputs (one directory up) are everything the
`discover/*.py` modules can produce automatically. This directory holds
the **human-curated layer on top**: per-pair verification, patch-URL
resolution that the pipeline couldn't fully automate, and the helper
script behind the URL table.

## Files

### `patch_url_overrides.json`

Hand-curated map of `bug-id → fix-commit URL`, consulted **first** by
`CodeOverlapChecker` (in `discover/code_overlap.py`) before any
scraping or searching. This is how verified URLs from `findings_urls.md`
flow back into the pipeline so SUSPECTED pairs graduate to READY.

```json
{
  "_comment": "...",
  "overrides": {
    "CVE-2022-1096":    "https://chromium.googlesource.com/v8/v8/+/0981e91a...",
    "chromium:1315901": "https://chromium-review.googlesource.com/c/v8/v8/+/3755102",
    "CVE-2020-11261":   "https://git.codelinaro.org/clo/la/kernel/msm-4.9/-/commit/d236d315...",
    ...
  }
}
```

To add a pair: verify the fix commit by hand (browse the bug tracker /
RCA), then add `"<bug-id>": "<url>"` and re-run the harvester. Override
URLs are fully trusted (no source-files filter, no trusted-owner
check) since a human vetted them. The resolver normalises a few
browser-frontend / shortcut URL forms automatically
(`source.chromium.org/.../+/sha`, `git.kernel.org/linus/sha`,
`source.codeaurora.org/.../commit/?id=`).

### `findings_table_claude.md`

Per-row verification of every pair in the pipeline's `confirmed` set
(~120 rows). Each row carries one of:

- `CORRECT` — relationship real and patch resolution accurate.
- `WRONG_RESOLUTION` — relationship real but the patch URL points at
  the wrong repo (commonly: random user fork that matched the CVE
  string in a commit message).
- `WRONG_DIRECTION` — later/prior swapped.
- `WRONG_KIND` — relationship exists but the label is wrong
  (e.g. should be `same_exploit_flow`, not `same_root_cause`).
- `NOT_SIBLINGS` — different codebases or unrelated; should be DROPPED.
- `SELF_PAIR` — later and prior resolve to the same fix.
- `DUPLICATE` — same pair appears elsewhere with the correct direction.

Plus a `correct_software` column when the pipeline's software label
needs fixing (e.g. `apple-ios` → `apple-coretext` for the libType1Scaler
cluster).

This is the **authoritative verification** of the dataset.

### `findings_urls.md`

Three-tier patch-URL table covering all confirmed pairs:

- **Tier 1**: both patches fully resolved on both sides (ready to
  ingest).
- **Tier 2**: one side resolved + bug-tracker URL for the other (one
  click away from the missing CL).
- **Tier 3**: closed-source pairs (Microsoft Windows / Apple iOS /
  Adobe Reader / WinRAR / Samsung NPU) — relationship real but no
  public source diff available.

Plus an **Iteration 2 appendix** with the URLs resolved by
`resolve_remaining_cls.py` (Gerrit `bug:NNN` search, GHSL advisory →
crbug → Gerrit chain, NVD `Patch`-tagged refs, Mozilla MFSA →
Bugzilla).

### `resolve_remaining_cls.py`

The script that produces the iteration-2 entries in `findings_urls.md`.
Re-run when new pairs join the pipeline:

```bash
cd src/db/project_zero
uv run --no-project --with requests --python 3.12 \
    python3 findings/verified/resolve_remaining_cls.py
```

Writes `resolved_cls.json` next to itself (gitignored — the
human-readable summary in `findings_urls.md` is what survives).

Tries, in order, for each unresolved bug-id:

1. **Gerrit REST** `?q=bug:NNN+status:merged` — finds Chromium CLs
   whose `Bug:` trailer references the bug. Falls back through
   `tr:chromium:NNN`, `message:"BUG=NNN"`, `message:"crbug.com/NNN"`,
   `message:"crbug/NNN"` for older commits.
2. **GHSL advisory page** → extracts the crbug id → loops back to
   step 1 to find the actual fix CL.
3. **NVD REST** `?cveId=...` → keeps references whose host looks like
   a source mirror (googlesource, codelinaro, kernel.org, hg.mozilla,
   bugzilla.mozilla) or carry the `patch` tag.
4. **Mozilla MFSA** + Bugzilla follow-up for `mozilla:NNNN` priors.
5. **Gerrit message-search** by CVE id as a last resort.

Final hit rate: ~19/34 on the current set. The remaining 15 are
old crbug issues without a fix CL, JS-rendered Project Zero pages,
or CVEs whose NVD entries don't have `Patch`-tagged references.

## Workflow

1. Run the pipeline to produce `findings/*.{json,csv,md}`.
2. Manually verify each pair in `findings_table_claude.md` against the
   PZ RCAs.
3. Run `resolve_remaining_cls.py` to fill in missing patch URLs.
4. Maintain `findings_urls.md` as the canonical patch-URL reference.
