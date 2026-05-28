"""Apply a DRR patch to a copy of the buggy checkout, then run each
compiled Jazzer harness against the patched code to detect whether the
harness still triggers a crash — the key signal for overfitting patches.

Pipeline:
    PatchedProjectBuilder   copy buggy dir, apply patch, compile
    FuzzRunner              run Jazzer per harness, collect FuzzRunResult
"""
import os
import shutil
import subprocess
from dataclasses import dataclass
from typing import List

from build import BuildResult
import config


@dataclass
class FuzzRunResult:
    harness_path: str
    class_name: str
    attempt_label: str
    triggered: bool   # Jazzer found a crash on the patched code
    timed_out: bool
    returncode: int
    stdout: str
    stderr: str


class PatchedProjectBuilder:
    """Copy a buggy Defects4J checkout, apply a DRR patch, and compile it.

    DRR patches use `/src/...` path prefixes (no `a/`/`b/`), so we use
    `patch -p1` which strips the leading `/` to produce a relative path
    matching the project layout inside the checkout directory.
    """

    def __init__(self, patched_root: str = config.D4J_CHECKOUT_ROOT):
        self.patched_root = patched_root
        self._classpath_cache: dict = {}

    def build_patched_dir(self, buggy_dir: str, patch_path: str) -> str:
        """Return a compiled patched copy of buggy_dir with the DRR patch
        applied. Idempotent: skips copy/patch/compile if the directory
        already exists."""
        patched_dir = self._patched_dir_path(buggy_dir, patch_path)
        if not os.path.isdir(patched_dir):
            print(f"Copying {buggy_dir} → {patched_dir}")
            shutil.copytree(buggy_dir, patched_dir)
            self._apply_patch(patched_dir, patch_path)
            subprocess.run(
                ['defects4j', 'compile'],
                cwd=patched_dir, check=True,
            )
        return patched_dir

    def classpath(self, patched_dir: str) -> str:
        if patched_dir not in self._classpath_cache:
            cp = subprocess.run(
                ['defects4j', 'export', '-p', 'cp.test'],
                cwd=patched_dir, check=True,
                capture_output=True, text=True,
            ).stdout.strip()
            self._classpath_cache[patched_dir] = cp
        return self._classpath_cache[patched_dir]

    def _patched_dir_path(self, buggy_dir: str, patch_path: str) -> str:
        patch_stem = os.path.splitext(os.path.basename(patch_path))[0]
        base = os.path.basename(buggy_dir.rstrip('/'))
        return os.path.join(self.patched_root, f'{base}_patched_{patch_stem}')

    @staticmethod
    def _apply_patch(target_dir: str, patch_path: str) -> None:
        abs_patch = os.path.abspath(patch_path)
        # DRR patches have `/src/...` paths; -p1 strips the leading `/`.
        # Try git apply first (d4j checkouts are git repos), fall back to patch.
        git_result = subprocess.run(
            ['git', 'apply', '--whitespace=fix', abs_patch],
            cwd=target_dir,
        )
        if git_result.returncode != 0:
            subprocess.run(
                ['patch', '-p1', '--forward', '--input', abs_patch],
                cwd=target_dir, check=True,
            )


class FuzzRunner:
    """Run Jazzer on each compiled harness against a patched project and
    report whether it still finds a crash."""

    def __init__(self,
                 jazzer_standalone_jar: str,
                 timeout_seconds: int = config.FUZZ_TIMEOUT_SECONDS):
        self.jazzer_standalone_jar = jazzer_standalone_jar
        self.timeout_seconds = timeout_seconds

    def run_all(self,
                successful_results: List[BuildResult],
                patch_path: str,
                buggy_dir: str) -> List[FuzzRunResult]:
        """Apply patch, compile patched project, then fuzz every harness."""
        builder = PatchedProjectBuilder()
        patched_dir = builder.build_patched_dir(buggy_dir, patch_path)
        patched_cp = builder.classpath(patched_dir)

        results = []
        for br in successful_results:
            print(f"\n--- fuzzing {br.class_name} "
                  f"({br.attempt_label or 'harness'}) ---")
            r = self._run_one(br, patched_cp)
            results.append(r)
            _print_fuzz_result(r)
        return results

    def _run_one(self, build_result: BuildResult,
                 patched_cp: str) -> FuzzRunResult:
        harness_dir = os.path.dirname(build_result.harness_path)
        classpath = os.pathsep.join([
            self.jazzer_standalone_jar,
            patched_cp,
            harness_dir,
        ])
        cmd = [
            'java', '-cp', classpath,
            'com.code_intelligence.jazzer.Jazzer',
            f'--target_class={build_result.class_name}',
            '--',
            f'-max_total_time={self.timeout_seconds}',
        ]

        timed_out = False
        try:
            proc = subprocess.run(
                cmd, capture_output=True, text=True,
                timeout=self.timeout_seconds + 15,
            )
            returncode = proc.returncode
            stdout, stderr = proc.stdout, proc.stderr
        except subprocess.TimeoutExpired as exc:
            timed_out = True
            returncode = -1
            stdout = exc.stdout or ''
            stderr = exc.stderr or ''

        triggered = (
            not timed_out and (
                returncode == config.JAZZER_CRASH_EXIT_CODE or
                '== Java Exception' in stderr or
                '== Java Exception' in stdout
            )
        )

        return FuzzRunResult(
            harness_path=build_result.harness_path,
            class_name=build_result.class_name,
            attempt_label=build_result.attempt_label,
            triggered=triggered,
            timed_out=timed_out,
            returncode=returncode,
            stdout=stdout,
            stderr=stderr,
        )


def _print_fuzz_result(r: FuzzRunResult) -> None:
    if r.timed_out:
        print("  ⏱  timed out — no crash found within time limit")
    elif r.triggered:
        print(f"  ✗  CRASH FOUND (exit {r.returncode})"
              " — vulnerability still reachable on patched code")
    else:
        print(f"  ✓  clean run (exit {r.returncode})"
              " — vulnerability not triggered on patched code")
