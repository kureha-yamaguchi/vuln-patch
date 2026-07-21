"""Adapter to the vuln-patch harness generator.

vuln-patch is your pipeline; this module just defines the contract and gives a
runnable fallback so the differential loop works before it is wired in.

Contract: your generator is invoked as

    <vuln-patch-cmd> --context <context.json> --out <dir>

and must write one or more libFuzzer harness source files (``.c``/``.cc``/...)
into ``<dir>``. Each file is a *drop-in replacement* for the existing harness
(``context["base_harness"]``); the driver overwrites that file with each variant
so the project's own build.sh compiles it unchanged. context.json contains:
project, cve, osv_id, repo, vuln_commit, fixed_commit, fuzz_target, vuln_src,
base_harness, fix_diff, crash_log, num_variants.
"""
from __future__ import annotations

import json
import shlex
import shutil
import subprocess
from pathlib import Path

_SRC_EXTS = (".c", ".cc", ".cpp", ".cxx", ".C")


def generate(context: dict, out_dir: Path, vuln_patch_cmd: "str | None") -> list:
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    ctx_path = out_dir / "context.json"
    ctx_path.write_text(json.dumps(context, indent=2))

    if vuln_patch_cmd:
        cmd = shlex.split(vuln_patch_cmd) + ["--context", str(ctx_path), "--out", str(out_dir)]
        subprocess.run(cmd, check=True)
        return sorted(p for p in out_dir.iterdir() if p.suffix in _SRC_EXTS)

    # Fallback: one no-op variant (a copy of the base harness) so the pipeline
    # runs end-to-end. Real signal comes from vuln-patch variants.
    base = Path(context["base_harness"])
    dst = out_dir / ("variant_000" + base.suffix)
    shutil.copyfile(base, dst)
    return [dst]
