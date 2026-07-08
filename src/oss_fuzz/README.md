# OSS-Fuzz / libFuzzer front-end

Variant analysis for OSS-Fuzz projects: take the most recent **public CVE**,
generate libFuzzer harnesses on the **vulnerable** version (gated so each one
actually crashes there), then run them on **HEAD**. A crash on HEAD is a
*sibling* input the fix failed to cover.

This is the C/C++ analogue of `src/java` (Defects4J + Jazzer). It reuses the
shared LLM backend (`src/llm.py`), config (`src/config.py`), and — importantly
— the identical variant-analysis steering (`src/variant.py`), so the research
heuristic can't drift between the two front-ends. It does **not** build on the
out-of-date `src/linux`.

## Pipeline

| Stage | Module | Job |
|-------|--------|-----|
| CVE selection | `osv.py` | newest public, CVE-bearing OSV entry → fix commit, repo, PoC ref |
| Substrate | `ossfuzz.py` | clone repo, worktree `vuln`(=fix~1) & `head`, `helper.py` build/run/reproduce |
| Analysis | `analysis.py` | fix diff → touched functions; fuzz-introspector call graph → bounded reachable set (heuristic fallback) |
| Prompt | `prompts.py` | libFuzzer prompt + shared steering (`variant.py`) |
| LLM | `../llm.py` | shared `HarnessGenerator` |
| Campaign | `campaign.py` | generate → build → trigger-gate on the vuln build |
| Sibling hunt | `run.py` | build accepted harnesses on HEAD, run, report crashes |

The one non-obvious trick: to compile a *new* harness for an arbitrary
project, `ossfuzz.py` cribs the compile flags from an existing
`$LIB_FUZZING_ENGINE` line in the project's `build.sh` and swaps in our
source/output names, so we inherit its include/link flags instead of guessing.
The project's `build.sh` is edited under try/finally and restored.

## Requirements (real runs)

- A local `google/oss-fuzz` checkout — set `OSS_FUZZ_DIR` (default `~/oss-fuzz`).
- Docker (used by `infra/helper.py`).
- An LLM backend, same as the Java pipeline: `OPENAI_API_KEY`, or Azure, or a
  local server (see `src/config.py`).

## Usage

```bash
export OSS_FUZZ_DIR=~/oss-fuzz OPENAI_API_KEY=sk-...
uv run -m oss_fuzz.run --project libxml2 -n 5 --fuzz-timeout 300
```

Pin a specific CVE, or supply the original PoC for a pre-flight sanity check:

```bash
uv run -m oss_fuzz.run --project libxml2 --cve CVE-2022-XXXXX \
    --reproducer ./testcase --sanitizer address
```

### Offline wiring check (no Docker / network / LLM)

```bash
uv run -m oss_fuzz.run --project demo \
    --osv-json oss_fuzz/tests/fixture_osv.json --dry-run -n 1 -m 2
```

`--dry-run` prints every external command and uses a stub harness, so you can
verify the control flow before spending a real fuzzing budget. `--osv-json`
loads OSV records from a file instead of the network (also good for
reproducible runs).

## Tests

```bash
python src/oss_fuzz/tests/test_offline.py     # or: pytest src/oss_fuzz/tests
```

Covers CVE selection, diff→function extraction, the `build.sh` crib, crash
detection/signatures, source extraction, prompt assembly, and Java/shared
steering parity.

## Notes & limits

- **Reproducer availability.** OSS-Fuzz testcases are embargoed until
  disclosure and OSV doesn't always embed a stable download URL, so the PoC is
  *optional*: the pipeline re-derives triggering harnesses from the fix diff
  and gates on its own crash check. Pass `--reproducer <path>` if you have the
  testcase and want the pre-flight sanity reproduce.
- **Reachable set.** The call graph comes from fuzz-introspector's light
  (tree-sitter, no-build) frontend — a bounded BFS over `base_callsites`
  scoped to project functions, unioned with project-resolved source callees,
  exactly as in `src/java`. It needs the introspector extra
  (`uv sync --extra introspector`); without it, or on timeout, the analyzer
  falls back to the brace-match heuristic and reports which was used
  (`reachable_source` in the printed context). Function *extraction* itself is
  still brace-matching, not a full parse; the trigger gate — not the analysis
  — decides harness validity.
- **HEAD build drift.** If the library's public API changed between the vuln
  commit and HEAD, a harness may not compile against HEAD; those are skipped
  and reported, not counted as clean.

## Responsible disclosure

A crash on HEAD is a live, unfixed issue. Report it to the project and to
OSS-Fuzz through coordinated disclosure; don't publish the sibling input
before a fix ships.