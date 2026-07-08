"""Thin wrapper around OSS-Fuzz ``infra/helper.py`` for C/C++ libFuzzer targets.

Only four operations are needed: build the image, build fuzzers from a *local*
source checkout (so we can pin an arbitrary commit), reproduce a testcase, and
run a fuzzer for a fixed time budget. Crashes are detected from the exit code,
sanitizer markers in the output, and any new ``crash-*`` artifact on disk.
"""
from __future__ import annotations

import glob
import subprocess
import sys
from pathlib import Path

CRASH_MARKERS = (
    "ERROR: AddressSanitizer",
    "ERROR: LeakSanitizer",
    "WARNING: MemorySanitizer",
    "SUMMARY: MemorySanitizer",
    "ERROR: HWAddressSanitizer",
    "SUMMARY: UndefinedBehaviorSanitizer",
    "runtime error:",
    "ERROR: libFuzzer",
    "libFuzzer: deadly signal",
    "SEGV on unknown address",
)


def has_crash(text: str) -> bool:
    return any(m in text for m in CRASH_MARKERS)


class OssFuzz:
    def __init__(self, oss_fuzz_dir, sanitizer: str = "address"):
        self.dir = Path(oss_fuzz_dir).resolve()
        self.sanitizer = sanitizer
        if not (self.dir / "infra" / "helper.py").exists():
            raise FileNotFoundError(f"infra/helper.py not found under {self.dir}")

    def _run(self, args, timeout=None):
        return subprocess.run(
            [sys.executable, "infra/helper.py", *args],
            cwd=self.dir,
            text=True,
            capture_output=True,
            timeout=timeout,
        )

    def out_dir(self, project: str) -> Path:
        return self.dir / "build" / "out" / project

    def build_image(self, project: str):
        cp = self._run(["build_image", "--pull", project])
        return cp.returncode == 0, cp.stdout + cp.stderr

    def build_fuzzers(self, project: str, source_path) -> tuple:
        cp = self._run([
            "build_fuzzers",
            "--sanitizer", self.sanitizer,
            "--engine", "libfuzzer",
            project,
            str(Path(source_path).resolve()),
        ])
        return cp.returncode == 0, cp.stdout + cp.stderr

    def reproduce(self, project: str, target: str, testcase) -> tuple:
        cp = self._run(["reproduce", project, target, str(Path(testcase).resolve())])
        log = cp.stdout + cp.stderr
        return (cp.returncode != 0 or has_crash(log)), log

    def run_fuzzer(self, project: str, target: str, seconds: int) -> tuple:
        out = self.out_dir(project)
        before = set(glob.glob(str(out / "crash-*")))
        cp = self._run(
            [
                "run_fuzzer", project, target, "--",
                f"-max_total_time={int(seconds)}",
                "-artifact_prefix=/out/",
            ],
            timeout=seconds + 600,
        )
        log = cp.stdout + cp.stderr
        new = sorted(set(glob.glob(str(out / "crash-*"))) - before)
        crashed = bool(new) or has_crash(log)
        return crashed, new, log
