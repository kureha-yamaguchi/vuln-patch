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
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional

from build import BuildResult
sys.path.insert(0, str(Path(__file__).parent.parent))
import config


@dataclass
class JazzerOutcome:
    """Raw result of running Jazzer once against some classpath."""
    triggered: bool
    timed_out: bool
    returncode: int
    stdout: str
    stderr: str
    crash_reason: Optional[str] = None  # why we classified this as a crash

    @property
    def combined_output(self) -> str:
        return f"{self.stdout}\n{self.stderr}"


# Output markers that mean "Jazzer caught an uncaught throwable from the
# harness". The original detector only looked for the first of these, but a
# harness that throws deterministically on the *first* input often aborts
# during libFuzzer's warmup and surfaces one of the later forms instead of
# the clean `== Java Exception` finding banner.
_CRASH_MARKERS = (
    '== Java Exception',
    '== libFuzzer crashing input',
    'ERROR: libFuzzer: deadly signal',
    'ERROR: libFuzzer: fuzz target exited',
    'Uncaught exception',
    'Test unit written to',        # crash artifact persisted
    'artifact_prefix',             # crash artifact path echoed on a finding
)


def _looks_like_crash(returncode: int,
                      combined: str,
                      expected_exceptions: Optional[List[str]]) -> Optional[str]:
    """Return a short human-readable reason if `combined` (stdout+stderr)
    shows a genuine crash, else None.

    Detection is layered from most to least certain:

      1. Jazzer's dedicated finding exit code.
      2. Any known crash/finding marker in the output.
      3. The *expected* throwable type appears in the output together with
         a project stack frame. This is the key fix for the `rc=1` case: a
         deterministic first-input crash can exit nonzero without printing
         the finding banner, but if we see the exception we were told to
         expect being thrown from project code, it is unambiguously the
         crash we are gating on — not a Jazzer startup error.
    """
    if returncode == config.JAZZER_CRASH_EXIT_CODE:
        return f"exit code {returncode} (Jazzer finding)"

    for marker in _CRASH_MARKERS:
        if marker in combined:
            return f"output marker: {marker!r}"

    # Expected-exception evidence. Requires BOTH the throwable type and a
    # stack frame so we don't fire on the type merely being named in a log
    # line. _FRAME_RE is defined below this function but resolved at call
    # time, so the forward reference is fine.
    if expected_exceptions:
        has_frame = bool(_FRAME_RE.search(combined))
        for exc in expected_exceptions:
            if exc and exc in combined and has_frame:
                return f"expected throwable {exc!r} with stack frame"

    return None


def run_jazzer(jazzer_standalone_jar: str,
               target_class: str,
               harness_dir: str,
               project_cp: str,
               timeout_seconds: int,
               expected_exceptions: Optional[List[str]] = None,
               jazzer_api_jar: Optional[str] = None
               ) -> JazzerOutcome:
    """Run one Jazzer harness against `project_cp` and report whether it
    crashed within `timeout_seconds`. Shared by the buggy-version gate
    and the patched-version overfitting check so crash detection is
    defined in exactly one place.

    `expected_exceptions` is an optional list of throwable names (e.g.
    ['java.lang.NullPointerException', 'NullPointerException']) used to
    recognise a deterministic first-input crash even when Jazzer exits
    without its usual finding banner.

    `jazzer_api_jar` is the jazzer-api jar containing FuzzedDataProvider.
    The standalone driver jar does NOT bundle the API classes in every
    release (0.22.1 does not), so the API jar must be on the *runtime*
    classpath too — not just the compile classpath — or Jazzer fails to
    reflect on the harness entrypoint with ClassNotFoundException on
    com.code_intelligence.jazzer.api.FuzzedDataProvider. Defaults to
    config.JAZZER_API_JAR.
    """
    if jazzer_api_jar is None:
        jazzer_api_jar = config.JAZZER_API_JAR

    artifact_dir = os.path.join(harness_dir, 'crashes')
    os.makedirs(artifact_dir, exist_ok=True)

    classpath = os.pathsep.join([
        jazzer_standalone_jar,
        jazzer_api_jar,
        project_cp,
        harness_dir,
    ])
    cmd = [
        'java', '-cp', classpath,
        'com.code_intelligence.jazzer.Jazzer',
        f'--target_class={target_class}',
        '--',
        f'-max_total_time={timeout_seconds}',
        # Ensure the harness body is actually entered (so a deterministic
        # first-input throw is reported as a finding, not a warmup abort)
        # and that any crashing input is persisted where we can see it.
        '-runs=100000',
        f'-artifact_prefix={artifact_dir}{os.sep}',
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

    combined = f"{stdout}\n{stderr}"
    crash_reason = (None if timed_out
                    else _looks_like_crash(returncode, combined,
                                           expected_exceptions))
    return JazzerOutcome(
        triggered=crash_reason is not None,
        timed_out=timed_out,
        returncode=returncode,
        stdout=stdout,
        stderr=stderr,
        crash_reason=crash_reason,
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
                 timeout_seconds: int = config.FUZZ_TIMEOUT_SECONDS,
                 expected_exceptions: Optional[List[str]] = None,
                 jazzer_api_jar: Optional[str] = None):
        self.jazzer_standalone_jar = jazzer_standalone_jar
        self.timeout_seconds = timeout_seconds
        self.expected_exceptions = expected_exceptions or []
        # API jar (FuzzedDataProvider) for the runtime classpath; see
        # run_jazzer. Defaults there to config.JAZZER_API_JAR if None.
        self.jazzer_api_jar = jazzer_api_jar

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
            expected_exceptions=self.expected_exceptions,
            jazzer_api_jar=self.jazzer_api_jar,
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
                 timeout_seconds: int = config.VERIFY_TIMEOUT_SECONDS,
                 expected_exceptions: Optional[List[str]] = None,
                 jazzer_api_jar: Optional[str] = None):
        self.jazzer_standalone_jar = jazzer_standalone_jar
        self.buggy_classpath = buggy_classpath
        self.timeout_seconds = timeout_seconds
        # Throwable names this bug is expected to raise (FQ and/or simple
        # name). Lets run_jazzer recognise a deterministic first-input
        # crash even when Jazzer exits without its finding banner.
        self.expected_exceptions = expected_exceptions or []
        # API jar (FuzzedDataProvider) for the runtime classpath; see
        # run_jazzer. Defaults there to config.JAZZER_API_JAR if None.
        self.jazzer_api_jar = jazzer_api_jar

    def verify(self, build_result: BuildResult) -> VerificationResult:
        harness_dir = os.path.dirname(build_result.harness_path)
        outcome = run_jazzer(
            jazzer_standalone_jar=self.jazzer_standalone_jar,
            target_class=build_result.class_name,
            harness_dir=harness_dir,
            project_cp=self.buggy_classpath,
            timeout_seconds=self.timeout_seconds,
            expected_exceptions=self.expected_exceptions,
            jazzer_api_jar=self.jazzer_api_jar,
        )
        combined = outcome.combined_output
        if outcome.triggered:
            print(f"  ↳ crash detected ({outcome.crash_reason})")
        elif not outcome.timed_out:
            # Non-crash: dump the tail of Jazzer's output so a misclassified
            # or genuinely-non-triggering harness can be diagnosed instead
            # of silently burning an attempt.
            print(f"  ↳ no crash (rc={outcome.returncode}). "
                  "Jazzer output tail:")
            tail = combined.strip().splitlines()[-15:]
            for line in tail:
                print(f"      {line}")
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