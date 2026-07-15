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
import time
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
               jazzer_api_jar: Optional[str] = None,
               keep_going: int = 0,
               extra_libfuzzer_args: Optional[List[str]] = None,
               corpus_dir: Optional[str] = None,
               input_file: Optional[str] = None,
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
        f'--reproducer_path={artifact_dir}',
    ]
    if keep_going > 0:
        # Continue past the first finding and collect up to `keep_going`
        # DISTINCT crashes (deduped by Jazzer on stack signature). This is
        # how we discover EVERY oracle a multi-oracle harness fires on the
        # patched code — not just the first the fuzzer happens to surface —
        # so a sound oracle isn't hidden behind an unsound sibling that
        # fired on some other input.
        cmd.append(f'--keep_going={keep_going}')
    if input_file:
        # Single-input REPLAY: a regular file passed positionally is
        # executed once, not fuzzed (mirrors the corpus_dir positional
        # below, but for one input). Used by the attribution check to ask
        # "does the EXACT input that fired on the patched build reproduce
        # on the buggy build?". No -max_total_time — the run is one
        # input; the subprocess timeout below still bounds a hung target.
        cmd += [
            '--',
            '-runs=1',
            f'-artifact_prefix={artifact_dir}{os.sep}',
            input_file,
        ]
    else:
        cmd += [
            '--',
            f'-max_total_time={timeout_seconds}',
            # Ensure the harness body is actually entered (so a deterministic
            # first-input throw is reported as a finding, not a warmup abort)
            # and that any crashing input is persisted where we can see it.
            '-runs=100000',
            f'-artifact_prefix={artifact_dir}{os.sep}',
        ]
        # Caller-supplied libFuzzer flags come AFTER the defaults so they win
        # (libFuzzer takes the last occurrence of a flag) — the relation screen
        # uses this to run a fixed `-runs=N` budget instead of a time budget.
        if extra_libfuzzer_args:
            cmd += list(extra_libfuzzer_args)
        if corpus_dir and os.path.isdir(corpus_dir):
            # A positional corpus directory: libFuzzer starts from these seeds
            # (literals mined from the project's own tests) instead of from
            # nothing, concentrating the early search near known-valid inputs
            # — the neighbourhood an overfit special-cased.
            cmd.append(corpus_dir)

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
# libFuzzer persists the crashing input and prints its path:
#   Test unit written to /path/to/crashes/crash-<sha1>
_ARTIFACT_RE = re.compile(r'Test unit written to\s+(\S+)')


def extract_artifact_path(output: str,
                          harness_dir: str,
                          not_before: float = 0.0) -> Optional[str]:
    """Path of the crashing input Jazzer persisted for a run, so the exact
    input can be REPLAYED (the attribution check replays it on the buggy
    build). Primary: the first 'Test unit written to <path>' line in the
    output. Fallback: the newest crash-* file under the harness's crashes/
    dir (the -artifact_prefix target) — but only one modified at/after
    `not_before`: the SAME harness dir was already fuzzed against the buggy
    build by the acceptance gate, and silently returning that stale
    artifact would make the differential replay compare an input from the
    wrong run. Returns None when nothing trustworthy exists (no crash, or
    only stale artifacts)."""
    m = _ARTIFACT_RE.search(output or '')
    if m and os.path.isfile(m.group(1)):
        return m.group(1)
    crashes_dir = os.path.join(harness_dir, 'crashes')
    try:
        candidates = [
            os.path.join(crashes_dir, f) for f in os.listdir(crashes_dir)
            if f.startswith('crash-')
        ]
        candidates = [p for p in candidates
                      if os.path.getmtime(p) >= not_before]
    except OSError:
        return None
    return max(candidates, key=os.path.getmtime) if candidates else None


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


# Generic JDK runtime exceptions that commonly ESCAPE library code on
# malformed / out-of-domain input (mirrors the valid-by-construction rule in
# prompts.py). For a SEMANTIC bug the defect is a wrong value, so a firing of
# this kind that also reproduces on the buggy build is pre-existing crash
# surface, not evidence about the patch. Deliberately EXCLUDES everything a
# harness uses for its own oracles (FuzzerSecurityIssue*, RuntimeException
# with a relation/consistency message): those firing on buggy is the TP
# signal — the patch failed to fix that family member — and must never be
# auto-dropped.
GENERIC_ESCAPE_EXCEPTIONS = frozenset({
    'java.lang.StringIndexOutOfBoundsException',
    'java.lang.ArrayIndexOutOfBoundsException',
    'java.lang.IndexOutOfBoundsException',
    'java.lang.NullPointerException',
    'java.lang.ClassCastException',
    'java.lang.ArithmeticException',
    'java.lang.NegativeArraySizeException',
})


def is_generic_escape(headline: Optional[str]) -> bool:
    """True iff a fired headline ('<class>: <message>') is a bare generic
    JDK runtime exception escaping the code under test — i.e. eligible for
    the differential-firing attribution check. Membership is decided by the
    exception CLASS alone, so the harness's own throws (FuzzerSecurityIssue*,
    RuntimeException oracle messages) can never classify as generic."""
    if not headline:
        return False
    cls = headline.split(':', 1)[0].strip()
    return cls in GENERIC_ESCAPE_EXCEPTIONS


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
    # Persisted crashing input (crashes/crash-<sha1>) for THIS run's
    # firing, when it could be located — the attribution check replays it
    # on the buggy build. None when the run didn't crash or the artifact
    # couldn't be attributed to this run.
    artifact_path: Optional[str] = None


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

    def classpath(self, patched_dir: str,
                  fallback_buggy_dir: str | None = None) -> str:
        if patched_dir not in self._classpath_cache:
            result = subprocess.run(
                ['defects4j', 'export', '-p', 'cp.test'],
                cwd=patched_dir,
                capture_output=True, text=True,
            )
            if result.returncode == 0:
                cp = result.stdout.strip()
            elif fallback_buggy_dir is not None:
                # export can fail on a copytree'd dir for some projects.
                # cp.test is structurally identical between buggy and patched
                # versions (same JARs, same compiled-class subdirs), so we
                # derive it by substituting paths from the buggy dir's export.
                print(f"  classpath export failed in patched dir "
                      f"({result.stderr.strip()}); deriving from buggy dir")
                fallback = subprocess.run(
                    ['defects4j', 'export', '-p', 'cp.test'],
                    cwd=fallback_buggy_dir, check=True,
                    capture_output=True, text=True,
                ).stdout.strip()
                cp = os.pathsep.join(
                    e.replace(fallback_buggy_dir, patched_dir)
                    for e in fallback.split(os.pathsep)
                )
            else:
                raise subprocess.CalledProcessError(
                    result.returncode,
                    result.args,
                    result.stdout,
                    result.stderr,
                )
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
        patched_cp = builder.classpath(patched_dir, fallback_buggy_dir=buggy_dir)

        results = []
        for br in successful_results:
            print(f"\n--- fuzzing {br.class_name} "
                  f"({br.attempt_label or 'harness'}) ---")
            r = self._run_one(br, patched_cp)
            results.append(r)
            _print_fuzz_result(r)
        return results

    def collect_fired_oracles(self,
                              harness_path: str,
                              class_name: str,
                              patch_path: str,
                              buggy_dir: str,
                              keep_going: int = 8,
                              timeout_seconds: Optional[int] = None
                              ) -> List[str]:
        """Re-fuzz ONE already-triggering harness against the patched code
        with `--keep_going`, and return the list of DISTINCT throwable
        headlines it fires (e.g. the exact `semantic mismatch: …` /
        `metamorphic violation: …` messages). Used by relation verification
        to judge EVERY oracle that fires on the patched code, not just the
        first one Jazzer surfaced. Returns [] on any error (caller then
        falls back to the single already-captured headline)."""
        from oracle_strength import exception_headlines
        try:
            builder = PatchedProjectBuilder()
            patched_dir = builder.build_patched_dir(buggy_dir, patch_path)
            patched_cp = builder.classpath(patched_dir,
                                           fallback_buggy_dir=buggy_dir)
            outcome = run_jazzer(
                jazzer_standalone_jar=self.jazzer_standalone_jar,
                target_class=class_name,
                harness_dir=os.path.dirname(harness_path),
                project_cp=patched_cp,
                timeout_seconds=(timeout_seconds
                                 if timeout_seconds is not None
                                 else self.timeout_seconds),
                expected_exceptions=self.expected_exceptions,
                jazzer_api_jar=self.jazzer_api_jar,
                keep_going=keep_going,
            )
        except Exception as exc:
            print(f"  (keep-going re-fuzz failed: {exc})")
            return []
        return exception_headlines(outcome.stdout + '\n' + outcome.stderr)

    def _run_one(self, build_result: BuildResult,
                 patched_cp: str) -> FuzzRunResult:
        harness_dir = os.path.dirname(build_result.harness_path)
        started_at = time.time()
        outcome = run_jazzer(
            jazzer_standalone_jar=self.jazzer_standalone_jar,
            target_class=build_result.class_name,
            harness_dir=harness_dir,
            project_cp=patched_cp,
            timeout_seconds=self.timeout_seconds,
            expected_exceptions=self.expected_exceptions,
            jazzer_api_jar=self.jazzer_api_jar,
        )
        artifact = None
        if outcome.triggered:
            # not_before guards against picking up a stale artifact the
            # buggy-gate fuzz of this same harness dir left behind.
            artifact = extract_artifact_path(
                outcome.combined_output, harness_dir,
                not_before=started_at - 1.0)
        return FuzzRunResult(
            harness_path=build_result.harness_path,
            class_name=build_result.class_name,
            attempt_label=build_result.attempt_label,
            triggered=outcome.triggered,
            timed_out=outcome.timed_out,
            returncode=outcome.returncode,
            stdout=outcome.stdout,
            stderr=outcome.stderr,
            artifact_path=artifact,
        )

    def replay_input(self,
                     harness_path: str,
                     class_name: str,
                     project_cp: str,
                     input_file: str,
                     timeout_seconds: int = 30) -> Optional[str]:
        """Execute ONE persisted crashing input against `project_cp` and
        return the resulting crash_signature, or None if it does not crash
        (or the replay itself errors — the attribution check must ABSTAIN
        on doubt, never block). The caller passes the BUGGY classpath to
        ask whether a firing is pre-existing rather than patch-caused."""
        try:
            outcome = run_jazzer(
                jazzer_standalone_jar=self.jazzer_standalone_jar,
                target_class=class_name,
                harness_dir=os.path.dirname(harness_path),
                project_cp=project_cp,
                timeout_seconds=timeout_seconds,
                expected_exceptions=self.expected_exceptions,
                jazzer_api_jar=self.jazzer_api_jar,
                input_file=input_file,
            )
        except Exception as exc:
            print(f"  (single-input replay failed: {exc})")
            return None
        if not outcome.triggered:
            return None
        return crash_signature(outcome.combined_output)


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
                 jazzer_api_jar: Optional[str] = None,
                 corpus_dir: Optional[str] = None):
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
        # Optional seed-corpus directory (literals mined from the project's
        # own tests). Only the buggy-version gate uses it: the gate's job is
        # to reach the trigger quickly, and known-valid inputs start the
        # search in the right neighbourhood.
        self.corpus_dir = corpus_dir

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
            corpus_dir=self.corpus_dir,
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