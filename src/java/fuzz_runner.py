"""Run compiled Jazzer harnesses against a project and report whether
they crash.

This module serves two callers:

  * `HarnessVerifier` (used *inside* the campaign) runs a harness for a
    short budget against the *buggy* checkout. A harness is only accepted
    into the set if it crashes here — proof it actually reaches the root
    cause rather than merely compiling.

  * `FuzzRunner` (used *after* the campaign) applies the DRR patch to a
    copy of the checkout and runs each accepted harness against the
    *patched* code. A harness that still crashes is evidence the patch is
    overfitting — it didn't address the root cause the harness exercises.

Both share `run_jazzer`, which is the single place that knows how to
invoke Jazzer and decide "did this crash". `crash_signature` distils a
crash into a stable string so the campaign can tell sibling bugs apart.

Pipeline (post-campaign):
    PatchedProjectBuilder   copy buggy dir, apply patch, compile
    FuzzRunner              run Jazzer per harness, collect FuzzRunResult
"""
import os
import re
import shutil
import subprocess
from dataclasses import dataclass
from typing import List, Optional

from build import BuildResult
import config


@dataclass
class JazzerOutcome:
    """Raw result of running Jazzer once against some classpath."""
    triggered: bool
    timed_out: bool
    returncode: int
    stdout: str
    stderr: str

    @property
    def combined_output(self) -> str:
        return f"{self.stdout}\n{self.stderr}"


def run_jazzer(jazzer_standalone_jar: str,
               target_class: str,
               harness_dir: str,
               project_cp: str,
               timeout_seconds: int) -> JazzerOutcome:
    """Run one Jazzer harness against `project_cp` and report whether it
    crashed within `timeout_seconds`. Shared by the buggy-version gate
    and the patched-version overfitting check so crash detection is
    defined in exactly one place."""
    classpath = os.pathsep.join([
        jazzer_standalone_jar,
        project_cp,
        harness_dir,
    ])
    cmd = [
        'java', '-cp', classpath,
        'com.code_intelligence.jazzer.Jazzer',
        f'--target_class={target_class}',
        '--',
        f'-max_total_time={timeout_seconds}',
    ]

    timed_out = False
    try:
        proc = subprocess.run(
            cmd, capture_output=True, text=True,
            timeout=timeout_seconds + 15,
        )
        returncode = proc.returncode
        stdout, stderr = proc.stdout, proc.stderr
    except subprocess.TimeoutExpired as exc:
        timed_out = True
        returncode = -1
        stdout = exc.stdout or ''
        stderr = exc.stderr or ''
        # subprocess may hand back bytes on timeout depending on platform.
        if isinstance(stdout, bytes):
            stdout = stdout.decode('utf-8', 'replace')
        if isinstance(stderr, bytes):
            stderr = stderr.decode('utf-8', 'replace')

    triggered = (
        not timed_out and (
            returncode == config.JAZZER_CRASH_EXIT_CODE or
            '== Java Exception' in stderr or
            '== Java Exception' in stdout
        )
    )
    return JazzerOutcome(
        triggered=triggered,
        timed_out=timed_out,
        returncode=returncode,
        stdout=stdout,
        stderr=stderr,
    )


# Jazzer prints the offending throwable on a line like
#   == Java Exception: java.lang.ArrayIndexOutOfBoundsException: ...
_EXC_RE = re.compile(r'==\s*Java Exception:\s*([\w.$]+)')
# Stack frames look like `\tat pkg.Class.method(File.java:NN)`.
_FRAME_RE = re.compile(r'\bat\s+([\w.$]+)\.([\w$<>]+)\(')


def crash_signature(output: str) -> Optional[str]:
    """Distil a Jazzer crash into a stable signature: the exception type
    plus the first application stack frame (`Class.method`). Used by the
    campaign to tell whether a new harness found a *different* bug than
    the ones already in the set — the core signal for surfacing siblings
    rather than re-finding the same fault. Returns None if no crash is
    discernible in `output`."""
    exc_match = _EXC_RE.search(output)
    if not exc_match:
        return None
    exc_type = exc_match.group(1)

    top_frame = None
    for cls, method in _FRAME_RE.findall(output):
        # Skip Jazzer/JDK frames; the first project frame is the most
        # informative anchor for "where" the crash happened.
        if (cls.startswith('com.code_intelligence.jazzer')
                or cls.startswith('java.')
                or cls.startswith('jdk.')
                or cls.startswith('sun.')):
            continue
        top_frame = f"{cls}.{method}"
        break

    return f"{exc_type}@{top_frame}" if top_frame else exc_type


def covered_functions(output: str) -> List[str]:
    """Best-effort list of project functions named in a crash's stack
    trace (`Class.method`), JDK/Jazzer frames removed. These are
    functions the harness demonstrably *reached*, used to update the
    set-coverage context fed to subsequent generations."""
    out: List[str] = []
    seen = set()
    for cls, method in _FRAME_RE.findall(output):
        if (cls.startswith('com.code_intelligence.jazzer')
                or cls.startswith('java.')
                or cls.startswith('jdk.')
                or cls.startswith('sun.')):
            continue
        key = f"{cls}.{method}"
        if key not in seen:
            seen.add(key)
            out.append(key)
    return out


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
        outcome = run_jazzer(
            jazzer_standalone_jar=self.jazzer_standalone_jar,
            target_class=build_result.class_name,
            harness_dir=harness_dir,
            project_cp=patched_cp,
            timeout_seconds=self.timeout_seconds,
        )
        return FuzzRunResult(
            harness_path=build_result.harness_path,
            class_name=build_result.class_name,
            attempt_label=build_result.attempt_label,
            triggered=outcome.triggered,
            timed_out=outcome.timed_out,
            returncode=outcome.returncode,
            stdout=outcome.stdout,
            stderr=outcome.stderr,
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


@dataclass
class VerificationResult:
    """Outcome of the short in-campaign run against the *buggy* checkout.

    `crashed` is the acceptance gate: only harnesses that crash the
    known-buggy code are admitted to the set. `signature` and
    `reached_functions` feed the variant-analysis context so subsequent
    generations can steer toward *different* faults and *uncovered*
    reachable functions."""
    crashed: bool
    timed_out: bool
    returncode: int
    signature: Optional[str]
    reached_functions: List[str]
    stdout: str
    stderr: str


class HarnessVerifier:
    """Run a freshly compiled harness against the *buggy* checkout for a
    short budget and report whether it crashes.

    This is the teeth behind the "compiles AND triggers" convergence
    criterion. A harness that compiles but cannot crash the buggy code
    does not interrogate the root cause and is worthless for detecting
    overfitting on the patched code, so the campaign rejects it.

    The buggy classpath is the one the harness was compiled against, so
    we reuse `HarnessBuilder`'s cache rather than recomputing it.
    """

    def __init__(self,
                 jazzer_standalone_jar: str,
                 buggy_classpath: str,
                 timeout_seconds: int = config.VERIFY_TIMEOUT_SECONDS):
        self.jazzer_standalone_jar = jazzer_standalone_jar
        self.buggy_classpath = buggy_classpath
        self.timeout_seconds = timeout_seconds

    def verify(self, build_result: BuildResult) -> VerificationResult:
        harness_dir = os.path.dirname(build_result.harness_path)
        outcome = run_jazzer(
            jazzer_standalone_jar=self.jazzer_standalone_jar,
            target_class=build_result.class_name,
            harness_dir=harness_dir,
            project_cp=self.buggy_classpath,
            timeout_seconds=self.timeout_seconds,
        )
        combined = outcome.combined_output
        return VerificationResult(
            crashed=outcome.triggered,
            timed_out=outcome.timed_out,
            returncode=outcome.returncode,
            signature=crash_signature(combined) if outcome.triggered else None,
            reached_functions=(covered_functions(combined)
                               if outcome.triggered else []),
            stdout=outcome.stdout,
            stderr=outcome.stderr,
        )