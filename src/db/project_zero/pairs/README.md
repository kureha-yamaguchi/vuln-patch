# P0 variant-pair artifacts (`pairs/`)

Per-pair patches + metadata for the **READY** Project Zero variant-pairs —
the 43 pairs where both fix-commit patches resolved AND the deep diff verifier
confirmed the relationship at code level (after alias-dedup). Mirrors the
`linux_kernel/pairs/` layout so the same downstream tooling shape applies.

Materialised by [`../tools/build_pairs.py`](../tools/build_pairs.py) from the
pipeline outputs in [`../findings/`](../findings/) and the resolver's patch
cache. Re-run after the harvester changes the READY set:

```bash
cd src/db/project_zero
uv run --no-project --with openai --python 3.12 -m tools.build_pairs          # new pairs only
uv run --no-project --with openai --python 3.12 -m tools.build_pairs --force  # rewrite all
```

## Layout

```
pairs/<PRIOR>__<LATER>/
    fix0.patch      diff of the PRIOR fix — the incomplete one (Fix-0)
    fix1.patch      diff of the LATER fix — the corrective one  (Fix-1)
    metadata.json
```

**Convention (same as linux_kernel): `fix0 = prior`, `fix1 = later`.** The
prior CVE's patch was incomplete; that incompleteness caused the later CVE.
So a ground-truth harness should trigger at the prior (Fix-0) state and stop
triggering at the later (Fix-1) state. Bug ids containing `:` are sanitised
for the directory name (`chromium:1234` → `chromium-1234`).

## `metadata.json` schema

| field | description |
|---|---|
| `prior_cve` / `later_cve` | the pair (CVE id or tracker id) |
| `source` | `"project_zero_0day_itw"` |
| `confirmed` | `true` |
| `relationship_kind` | `incomplete_fix` \| `same_root_cause` \| `one_extends_other` (deep verdict, normalised) |
| `deep_diff_kind` | raw deep verdict (`*_confirmed`) |
| `deep_confidence` | deep verifier confidence 0–1 |
| `deep_cited_change` / `deep_reasoning` | the deep verifier's quoted change + rationale |
| `llm_relationship_kind` / `llm_confidence` | the earlier prose-LLM verdict |
| `codebase` / `software` | inferred codebase key + human label (best-effort; see caveat) |
| `repo_url` / `later_repo_url` | canonical upstream repo for Fix-0 / Fix-1 |
| `fix0_commit` / `fix1_commit` | bare commit id — a git SHA, or `CL/<n>` for a Gerrit change |
| `prior_patch_url` / `later_patch_url` | full resolved fix-commit URLs |
| `fix0_date` / `fix1_date` | commit date if the patch carries a `Date:` header, else `null` |
| `affected_files_fix0` / `affected_files_fix1` | files touched by each patch |
| `shared_files` | files touched by BOTH (empty for most same_root_cause siblings) |
| `evidence_url` / `cited_sentence` | the upstream document (RCA / blog) the pair was extracted from |
| `fuzzing_excluded` | `true` for closed-source codebases (no buildable tree) |

**Codebase caveat:** the `codebase` label is inferred from the bug-id prefix
/ P0 sheet and is imperfect for non-Chrome priors — e.g. some Android-kernel
(binder) and WebKit pairs are tagged `chrome`. Trust `affected_files_*` and
`repo_url` over the label when routing a pair to a build.

## Not included (deliberately)

No source trees or per-pair context dirs. The codebases are too heavy to pull
wholesale (chromium/src alone is 61 GB; see [`../findings/`](../findings/) and
the project memory note). Per-pair affected-file context, and buildable
checkouts for verification, are a future step — analogous to how linux_kernel
splits `fetch_patches.py` (cheap, done here) from `fetch_context.py` /
`checkout_pair.py` (heavy, on demand).
