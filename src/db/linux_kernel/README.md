# CVE Sibling Database — Linux Kernel

Ground-truth dataset of Linux kernel CVE pairs where an incomplete or incorrect initial fix (Fix-0) caused a follow-up vulnerability (Fix-1). Used to evaluate how well fuzzing harnesses detect incomplete patches.

**Source:** Liu et al., *"Characteristics, Root Causes, and Detection of Incomplete Security Bug Fixes in the Linux Kernel"*, arXiv:2511.17799 (Nov 2025). Dataset: [doi.org/10.5281/zenodo.6423844](https://doi.org/10.5281/zenodo.6423844)

## Dataset

26 pairs covering the period 2005–2021:
- **15 incorrect** — the fix itself was wrong (e.g. misidentified root cause, introduced semantic error)
- **11 incomplete** — the fix was right but missed analogous code paths or related modules
- **1 pair** (`CVE-2012-3552__CVE-2013-2224`) has no kernel commit for Fix-1 (only a Bugzilla attachment); excluded from checkout/fuzzing use

## Directory structure

```
linux_kernel/
├── README.md
├── liu_seeds.json        source-of-truth pair list (CVEs, commits, CWEs,
│                         dates, fix type) from arXiv:2511.17799
├── tools/                scripts that materialise the per-pair artifacts
│   ├── fetch_patches.py
│   ├── fetch_context.py
│   ├── enrich_metadata.py
│   └── checkout_pair.py
└── pairs/                one subdir per CVE pair (26 total)
    └── CVE-2011-1017__CVE-2011-2182/
        ├── metadata.json       CVEs, commit hashes, CWE codes, dates,
        │                       fix type, affected files, etc.
        ├── fix0.patch          diff of the incomplete fix (Fix-0)
        ├── fix1.patch          diff of the corrective fix (Fix-1)
        ├── fix0_context/       source files changed by Fix-0, at the
        │                       Fix-0 commit state
        └── fix1_context/       source files changed by Fix-1, at the
                                Fix-1 commit state
```

`fix0_context/` and `fix1_context/` contain only the directly modified `.c`/`.h` files — useful for browsing and lightweight LLM prompting. For harness generation and fuzzing you need the full kernel checkout (see below).

## `metadata.json` schema

| Field | Description |
|---|---|
| `prior_cve` / `later_cve` | The CVE pair |
| `source` | `"liu_et_al_2022"` |
| `confirmed` | `true` — manually verified by Liu et al. |
| `incomplete_fix_type` | `"incorrect"` or `"incomplete"` |
| `interval_days` | Days between Fix-0 and Fix-1 |
| `fix0_date` / `fix1_date` | ISO 8601 dates |
| `repo_url` | Linux kernel git URL |
| `fix0_commit` / `fix1_commit` | Git commit hashes for Fix-0 and Fix-1 |
| `prior_patch_url` / `later_patch_url` | kernel.org cgit links for the diffs |
| `cwe_fix0` / `cwe_fix1` | CWE classification at each fix |
| `cwe_fix0_detail` / `cwe_fix1_detail` | CWE description |
| `affected_files_fix0` | List of file paths changed in Fix-0 (parsed from `fix0.patch` diff headers) |
| `affected_functions_fix0` | List of function names patched in Fix-0 (parsed from hunk headers; best-effort) |
| `kernel_subsystem` | Top-level subsystem, e.g. `fs/partitions`, `net/sctp` (first 2 path components of primary file) |
| `fix0_parent_commit` | SHA of `fix0_commit^1` — the unpatched baseline state, where the original bug is fully present |
| `fuzzing_excluded` | `true` if `fix1_commit` is null; pipeline should skip this pair |
| `required_kconfig` | `CONFIG_` symbols mentioned in `fix0.patch` (best-effort; `[]` if none found) |

## Scripts

All scripts live under [`tools/`](tools/) and read/write to `pairs/`
relative to the project root.

### `tools/fetch_patches.py` — download patch diffs

Fetches `fix0.patch`, `fix1.patch`, and `metadata.json` for all pairs from kernel.org. Idempotent.

```bash
python3 tools/fetch_patches.py              # all pairs
python3 tools/fetch_patches.py --force      # re-download everything
python3 tools/fetch_patches.py --pair CVE-2011-1017__CVE-2011-2182
```

### `tools/fetch_context.py` — download changed source files

Parses each patch to find modified files and fetches them individually from kernel.org at the corresponding commit. Produces `fix0_context/` and `fix1_context/`. Idempotent.

```bash
python3 tools/fetch_context.py
python3 tools/fetch_context.py --pair CVE-2011-1017__CVE-2011-2182
```

> **Scope:** only directly modified files. Not sufficient for harness generation or building — use `tools/checkout_pair.py` for that.

### `tools/checkout_pair.py` — full kernel working trees

Checks out the full Linux kernel source at Fix-0 and Fix-1 commits as independent working trees, ready for building and fuzzing.

Uses a **shared bare blobless clone** (~400 MB, fetched once) with git worktrees on top — much more efficient than 52 separate clones.

```bash
# single pair
python3 tools/checkout_pair.py --pair CVE-2011-1017__CVE-2011-2182

# all pairs
python3 tools/checkout_pair.py --all

# reuse an existing kernel repo
LINUX_KERNEL_REPO=/path/to/linux.git python3 tools/checkout_pair.py --all

# one state only
python3 tools/checkout_pair.py --pair CVE-2016-9576__CVE-2016-10088 --only fix0
```

Worktrees are created under `/tmp/cve_sibling_checkouts/<pair>/fix0` and `.../fix1` by default. Override with `--dest`.

## Intended use for patch verification

```
fix0/ tree  →  build + run harness  →  harness SHOULD trigger   (incomplete fix)
fix1/ tree  →  build + run harness  →  harness should NOT trigger (corrective fix)
```

A harness that triggers on Fix-0 but not Fix-1 confirms it targets the root cause of the original vulnerability — the signal that the patch was incomplete.

## Pipeline order

```
fetch_patches.py   →  fetch_context.py  →  checkout_pair.py (on demand)
      ↓                     ↓                      ↓
 patches + metadata    changed source files    full build trees
 (lightweight, ~KB)    (browsing / LLM)        (harness gen + fuzzing)
```

## Known gaps

### Full codebase context fetched at the wrong commit

`fetch_context.py` downloads source files at `fix0_commit` and `fix1_commit`. For harness generation the relevant state is `fix0_commit^` — the parent commit, i.e. the kernel *before* the incomplete fix was applied. That is the state where the original vulnerability is still fully present and where a correct harness must trigger. The current `fix0_context/` directories reflect post-fix0 code, which may already partially obscure the vulnerable pattern.

Until this is addressed: use `checkout_pair.py --only fix0` and manually `git checkout fix0_commit^` inside the worktree to reach the pre-fix baseline.

### Metadata completeness

One field is not yet populated and requires understanding code semantics rather than text parsing:

| Missing field | Impact |
|---|---|
| `vulnerability_trigger_type` | Which kernel interface reaches the bug (`ioctl`, `syscall`, `network_packet`, `sysfs`, `block_io`, `mount`, etc.). CWE alone does not convey this; it is the most useful signal for LLM harness design. Must be filled manually or via LLM analysis of the patch context |

### Oracle validity for `incorrect` vs `incomplete` fix type

15 of 26 pairs are typed `incorrect` — the initial fix misidentified the root cause entirely. For these, a harness targeting the Fix-0 code path may trigger on both Fix-0 *and* Fix-1 (because the vulnerable code is structurally different, not just incompletely patched), making the trigger/no-trigger oracle ambiguous. The pipeline should handle `incorrect` and `incomplete` pairs differently; this branching logic does not yet exist.

### Toolchain compatibility

Pairs from 2005–2012 are unlikely to build with a modern gcc or clang. No per-pair compiler hints or container images are provided. Expect build failures for the oldest entries until toolchain guidance is added.
