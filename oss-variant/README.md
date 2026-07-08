# oss-variant

Minimal driver for **incomplete-patch / variant analysis** on an OSS-Fuzz
project. It:

1. finds the **most recent public, CVE-tagged** vulnerability for the project via
   OSV, and resolves the vulnerable commit (`<fixed>^`) and the fix diff;
2. asks your **vuln-patch** generator for variant libFuzzer harnesses conditioned
   on the root cause (patch diff + crash), each a drop-in replacement for the
   existing harness;
3. gates each variant on the **vulnerable** version (must crash there), then runs
   it on **HEAD**. A crash on HEAD is a **sibling input the fix failed to cover**.

Pure Python standard library — no pip installs. Uses OSS-Fuzz `infra/helper.py`
for all builds/runs, so it works for C/C++ libFuzzer targets.

## Requirements

- Docker + a local `google/oss-fuzz` checkout
- `git`, Python 3.8+
- Network access to `api.osv.dev` and the target's git host

## Usage

```bash
git clone https://github.com/google/oss-fuzz
python -m oss_variant \
  --project <oss-fuzz-project> \
  --oss-fuzz ./oss-fuzz \
  --reproducer ./poc_testcase \
  --fuzz-target <fuzz_target_name> \
  --vuln-patch-cmd "python /path/to/vuln-patch/generate.py"
```

Omit `--vuln-patch-cmd` for a dry run: it emits one no-op variant (a copy of the
base harness) so you can validate the plumbing before wiring in vuln-patch.

Key options: `--sanitizer` (address/undefined/memory), `--num-variants`,
`--vuln-budget`/`--head-budget` (seconds of fuzzing per variant), `--skip-gate`,
`--base-harness` (if auto-detection misses).

Outputs land in `--work` (default `./work`): `fix.diff`, baseline logs,
`variants/`, per-variant HEAD logs, saved crash inputs, and `siblings.json`.

## The vuln-patch contract

Your generator is invoked as:

```
<vuln-patch-cmd> --context <work>/variants/context.json --out <work>/variants
```

`context.json` provides `project, cve, osv_id, repo, vuln_commit, fixed_commit,
fuzz_target, vuln_src, base_harness, fix_diff, crash_log, num_variants`. Write one
or more harness source files (`.c`/`.cc`/...) into `--out`. Keep the same file
**extension** as `base_harness` — the driver overwrites the existing harness file
in place with each variant, so the project's own `build.sh` compiles it unchanged
(this is what generalises the workflow to arbitrary C/C++ projects without
touching per-project build logic).

## Caveats (kept deliberately small)

- **PoC download is manual.** OSS-Fuzz testcase downloads sit behind
  `oss-fuzz.com` and aren't a clean public API even once a bug is public. The
  tool prints the report link; download the testcase and pass `--reproducer`.
  Without it, baseline checks are skipped (the gate still works by fuzzing).
- **Cost.** Each variant triggers a full `build_fuzzers` on vuln and/or HEAD.
  Fine for research; batch/parallelise later if needed.
- **Gate is blind fuzzing** with no seed corpus, so raise `--vuln-budget` (or add
  a corpus) for deep bugs. `--skip-gate` bypasses it.
- **Sanitizer must match the bug class** (e.g. `--sanitizer undefined` for UBSan
  crashes); default is `address`.
- Projects with submodules or unusual `build.sh` may need extra care when built
  from a local `source_path` checkout.

## Disclose responsibly

A crash on HEAD is a real, unfixed issue. Report it to the upstream project and
OSS-Fuzz through coordinated disclosure before publishing the sibling input.
