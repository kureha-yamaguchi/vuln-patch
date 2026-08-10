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
import glob
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional, Tuple

from java.harness.build import BuildResult
sys.path.insert(0, str(Path(__file__).parent.parent))
import config


# ---------------------------------------------------------------------------
# Permanent audit trail for the cycle-6 replay instrumentation.
#
# `run_suite.sh` deletes `run.log` on a SUCCESSFUL leg, so the `print`
# diagnostics below cannot reach `trace.md` (which is built purely from
# `record_event`; see docs/replay/night20c_analysis.md). Every cycle-6 replay
# decision therefore also emits a recorded event — one where it is CONSIDERED,
# one where it DECIDES — so a green run can prove whether the diversion probe
# and the iterated muted replay actually ran. Prints stay: they are the record
# on a FAILED leg, where run.log survives.
#
# Fail-silent by construction: never raises into a replay.
# ---------------------------------------------------------------------------

def _ev(method, target=None, output=None, reason=None):
    """Record one cycle-6 audit event. Never raises."""
    try:
        from llm import record_event
        record_event('deterministic', method=method,
                     target=('' if target is None else str(target)),
                     output=('' if output is None else str(output)),
                     reason=('' if reason is None else str(reason)))
    except Exception:  # pragma: no cover - defensive
        pass


@dataclass
class JazzerOutcome:
    """Raw result of running Jazzer once against some classpath."""
    triggered: bool
    timed_out: bool
    returncode: int
    stdout: str
    stderr: str
    crash_reason: Optional[str] = None  # why we classified this as a crash
    # {method-id: hits} from the diff-hit instrumentation, or None when the
    # --diffcov flag is off. MEASUREMENT ONLY — see _collect_diffcov.
    diffcov: Optional[dict] = None

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
)
# Artifact evidence counts ONLY for crash-* files. libFuzzer also writes
# slow-unit-* / timeout-* / oom-* artifacts on runs with NO finding — a
# 21s-slow input on the Math-2 correct leg printed "Test unit written to
# .../slow-unit-..." plus "artifact_prefix=..." on a clean exit-0 run and
# was scored as a crash (p0gate leg 03, 2026-07-17). The bare
# 'Test unit written to' and 'artifact_prefix' markers are therefore
# phantom-crash generators, not crash evidence.
_CRASH_ARTIFACT_RE = re.compile(r'Test unit written to \S*crash-')


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

    if _CRASH_ARTIFACT_RE.search(combined):
        return "crash artifact written (crash-* file)"

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
               diffcov_out: Optional[str] = None,
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

    # `env` stays None unless the diff-hit instrumentation is on, so the
    # subprocess call is byte-for-byte what it was when the flag is off.
    env = None
    if diffcov_out:
        from java.execution import diffcov as diffcov_mod
        try:
            os.unlink(diffcov_out)   # never read a previous run's dump
        except OSError:
            pass
        env = dict(os.environ)
        env[diffcov_mod.OUT_ENV_VAR] = diffcov_out

    timed_out = False
    try:
        proc = subprocess.run(
            cmd, capture_output=True, text=True,
            timeout=timeout_seconds + 15, env=env,
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
        diffcov=(_collect_diffcov(diffcov_out, combined)
                 if diffcov_out else None),
    )


def _collect_diffcov(diffcov_out: str, combined: str) -> dict:
    """`{method-id: hits}` for one Jazzer execution.

    MEASUREMENT ONLY, and deliberately isolated here: diffcov exists so a
    human can read whether generated inputs REACHED the patch-changed code.
    It must never be added to an LLM prompt, to verifier evidence, or to any
    gate or verdict — doing so would make the pipeline's decision depend on
    a signal it has not been validated against.

    The FILE is preferred over stderr because neither of this runner's two
    normal exits runs JVM shutdown hooks: `subprocess.run(timeout=...)`
    SIGKILLs the JVM on the wall-clock cap above, and libFuzzer terminates a
    finding run from native code. The instrumented build therefore flushes
    the counters to `diffcov_out` on a timer; stderr is only the fallback for
    a clean exit.
    """
    from java.execution import diffcov as diffcov_mod
    counts = diffcov_mod.read_diffcov_file(diffcov_out)
    return counts if counts else diffcov_mod.parse_diffcov(combined)


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


_CAUSE_RE = re.compile(r'Caused by:\s+([\w.$]+)')


def cause_signature(output: str) -> Optional[str]:
    """Signature (`class@first-project-frame`) of the ROOT cause in a
    Java `Caused by:` chain — the deepest entry, i.e. the crash that
    actually started it all. None when the trace has no cause chain.

    P0.3: a harness that catches a library crash and re-throws it as its
    own alarm type hides the crash from the headline signature; the
    attached cause (mandated by the campaign gate) preserves its
    identity so attribution can still ask "does this underlying crash
    also happen on the unpatched buggy build?" (the Chart-26 launder)."""
    matches = list(_CAUSE_RE.finditer(output or ''))
    if not matches:
        return None
    last = matches[-1]
    tail = output[last.start():]
    cls = last.group(1)
    top_frame = None
    for frame_cls, method in _FRAME_RE.findall(tail):
        if (frame_cls.startswith('com.code_intelligence.jazzer')
                or frame_cls.startswith('java.')
                or frame_cls.startswith('jdk.')
                or frame_cls.startswith('sun.')):
            continue
        top_frame = f"{frame_cls}.{method}"
        break
    return f"{cls}@{top_frame}" if top_frame else cls


# P3.3 crash-site pinning — per-oracle underlying-crash identity.
#
# A "must not crash" check fires because SOME exception happened underneath;
# its alarm looks identical whether that exception is the bug's own crash or
# an unrelated pre-existing one (Chart-26: the axis-label NPE the bug is
# about vs the text-measuring crash that exists on every build). The alarm
# message can't tell them apart; the underlying exception's TYPE can. We
# record, per oracle ID, which exception types stood behind its firings on
# the BUGGY build; on the patched build, a firing of the same oracle whose
# underlying types share nothing with the buggy-side set is a DIFFERENT
# crash wearing the same alarm — dismissed mechanically. Type-level (not
# type@frame) comparison on purpose: a half-fix that moves the same
# exception a frame deeper must stay a catch.

# Exception-ish names embedded in an alarm's own MESSAGE text (the
# flag-pattern variant P0.3's cause chain can't see).
_MSG_EXC_RE = re.compile(r'\b((?:[a-z][\w.]*\.)?[A-Z]\w*(?:Exception|Error))\b')
# Alarm/wrapper types that say nothing about the underlying crash.
_ALARM_TYPES = ('FuzzerSecurityIssue', 'RuntimeException', 'AssertionError',
                'Throwable', 'Exception', 'Error')


def _underlying_crash_types(chunk: str) -> set:
    """Exception types plausibly UNDERLYING one firing: the cause chain
    (P0.3), any non-alarm headline type, and exception names embedded in
    the alarm message text — minus the generic alarm wrappers."""
    types = set(_CAUSE_RE.findall(chunk or ''))
    m = _EXC_RE.search(chunk or '')
    if m:
        types.add(m.group(1))
    types.update(_MSG_EXC_RE.findall(chunk or ''))
    out = set()
    for t in types:
        simple = t.rsplit('.', 1)[-1]
        if any(simple == a or simple.startswith('FuzzerSecurityIssue')
               for a in _ALARM_TYPES):
            continue
        out.add(simple)
    return out


def per_oracle_crash_types(output: str) -> dict:
    """Map oracle ID -> set of underlying exception type names, from a
    fuzzing output that may contain several firings (keep_going or plain).
    Chunks are split on Jazzer's exception banner; a chunk with no oracle
    ID contributes nothing."""
    from java.parsing.java_source import oracle_ids_in_text
    result: dict = {}
    text = output or ''
    starts = [m.start() for m in _EXC_RE.finditer(text)]
    if not starts:
        return result
    starts.append(len(text))
    for a, b in zip(starts, starts[1:]):
        chunk = text[a:b]
        ids = oracle_ids_in_text(chunk)
        if not ids:
            continue
        types = _underlying_crash_types(chunk)
        if not types:
            continue
        for oid in ids:
            result.setdefault(oid, set()).update(types)
    return result


def exception_types_in_output(output: str) -> set:
    """Every exception type (simple name) discernible in a run's crash
    reports: each banner headline, each cause chain, and exception names
    embedded in alarm message text — generic alarm wrappers excluded.
    Unlike crash_signature this reads ALL banners and their causes, so a
    defect exception that a harness fences and rethrows under its own
    alarm type (named only in the message or the cause chain) is still
    seen. Text before the first banner is ignored so launcher noise
    (e.g. an expected-exception option string) cannot masquerade as an
    observed crash."""
    text = output or ''
    m = _EXC_RE.search(text)
    if not m:
        return set()
    tail = text[m.start():]
    types = _underlying_crash_types(tail)
    # _underlying_crash_types reads only the FIRST banner's headline; a
    # keep-going run has several banners — collect every headline too.
    for bm in _EXC_RE.finditer(tail):
        simple = bm.group(1).rsplit('.', 1)[-1]
        if not any(simple == a or simple.startswith('FuzzerSecurityIssue')
                   for a in _ALARM_TYPES):
            types.add(simple)
    return types


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


def is_generic_cause(cause_sig: Optional[str]) -> bool:
    """True iff a cause signature ('class@frame' from cause_signature)
    names a generic JDK escape class — i.e. the underlying crash behind a
    harness-own alarm is the kind that may be pre-existing library
    surface rather than evidence about the patch."""
    if not cause_sig:
        return False
    return cause_sig.split('@', 1)[0] in GENERIC_ESCAPE_EXCEPTIONS


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
    # {method-id: hits} for the patch-changed methods, or None with
    # --diffcov off. MEASUREMENT ONLY (see _collect_diffcov).
    diffcov: Optional[dict] = None


class PatchApplyError(RuntimeError):
    """The patch file could not be FULLY applied: malformed, truncated,
    reversed, or a hunk failed. A partial apply must never survive —
    Lang-50 (silently dropped out-of-order hunk) and Math-2/SOFix
    (reversed+truncated file that never applied) both produced weeks of
    results about programs that weren't what the pipeline believed."""


class TriggerVerificationError(RuntimeError):
    """The trigger-test safety net failed.

    status is one of:
      'bug_not_reproduced' — the bug's own failing test does NOT fail on
          the unpatched buggy checkout: the bug doesn't exist in our
          environment, so no verdict about a patch for it means anything
          (the Lang-7 lesson).
      'bad_patch' — the failing test still fails on the patched build:
          the patch didn't fully apply or doesn't do what a plausible
          patch must. Catches half-applied patches end-to-end.
    """

    def __init__(self, status: str, detail: str):
        super().__init__(f"{status}: {detail}")
        self.status = status


_HUNK_HEADER_RE = re.compile(r'^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@')


def _parse_unified_patch(text: str) -> List[Tuple[List[str], List[Tuple[int, List[str]]]]]:
    """Split a unified diff into file sections with COUNTED hunks.

    Returns [(header_lines, [(old_start_line, hunk_lines), ...]), ...].

    Counted parsing: each hunk consumes exactly the number of old/new
    lines its `@@ -a,b +c,d @@` header promises, so a truncated file
    (the original Math-2/SOFix patch ended mid-hunk) or garbage in the
    middle raises PatchApplyError instead of being silently mis-read.
    """
    lines = text.splitlines()
    sections: list = []
    header: List[str] = []
    hunks: List[Tuple[int, List[str]]] = []
    i, n = 0, len(lines)
    while i < n:
        m = _HUNK_HEADER_RE.match(lines[i])
        if m:
            if not header and not hunks and not sections:
                raise PatchApplyError(
                    f'{lines[i]!r}: hunk appears before any file header')
            old_start = int(m.group(1))
            old_count = int(m.group(2)) if m.group(2) is not None else 1
            new_count = int(m.group(4)) if m.group(4) is not None else 1
            body = [lines[i]]
            i += 1
            seen_old = seen_new = 0
            while seen_old < old_count or seen_new < new_count:
                if i >= n:
                    # Common APR-generator artifact: the hunk's LAST line
                    # is an empty line at end-of-file, which vanishes in
                    # line-splitting and leaves the counts exactly one
                    # short. Synthesize it; anything beyond one line is a
                    # real truncation. (12 of 1263 drr files need this;
                    # the apply-time reverse-verify still guards the
                    # synthesized content.)
                    deficit = (old_count - seen_old, new_count - seen_new)
                    fill = {(1, 1): '', (0, 1): '+', (1, 0): '-'}.get(deficit)
                    if fill is not None:
                        body.append(fill)
                        break
                    raise PatchApplyError(
                        f'truncated patch: hunk at old line {old_start} ends '
                        f'mid-way ({seen_old}/{old_count} old, '
                        f'{seen_new}/{new_count} new lines present)')
                line = lines[i]
                if line.startswith('\\'):
                    pass  # "\ No newline at end of file" — counts nothing
                elif line.startswith(' ') or line == '':
                    seen_old += 1
                    seen_new += 1
                elif line.startswith('-'):
                    seen_old += 1
                elif line.startswith('+'):
                    seen_new += 1
                else:
                    raise PatchApplyError(
                        f'malformed line inside hunk at old line '
                        f'{old_start}: {line!r}')
                if seen_old > old_count or seen_new > new_count:
                    raise PatchApplyError(
                        f'malformed hunk at old line {old_start}: body has '
                        f'more lines than the @@ header promises '
                        f'({seen_old}/{old_count} old, '
                        f'{seen_new}/{new_count} new)')
                body.append(line)
                i += 1
            if i < n and lines[i].startswith('\\'):
                body.append(lines[i])
                i += 1
            hunks.append((old_start, body))
        else:
            if hunks:
                sections.append((header, hunks))
                header, hunks = [], []
            header.append(lines[i])
            i += 1
    if hunks:
        sections.append((header, hunks))
    elif any(line.startswith('--- ') for line in header):
        raise PatchApplyError(
            'file header with no hunks at end of patch (truncated file?)')
    if not sections:
        raise PatchApplyError('no hunks found — not a unified diff?')
    return sections


def _file_sections(text: str) -> List[Tuple[List[str], List[Tuple[int, List[str]]]]]:
    """Group parsed sections by TARGET FILE, merging (a) repeated
    sections for the same file and (b) header-less continuation sections
    — Lang-50's descending second hunk sits after junk lines with no new
    ---/+++ header, so naive per-section handling never sees the two
    hunks side by side. Returns [(header_lines, hunks)] with one entry
    per file; junk lines outside headers/hunks are dropped."""
    files: List[list] = []
    key_to_idx: dict = {}
    for header, hunks in _parse_unified_patch(text):
        file_lines = [l for l in header
                      if l.startswith(('--- ', '+++ ', 'diff ', 'Index:'))]
        if not any(l.startswith('--- ') for l in file_lines):
            # no file header at all: these hunks continue the previous file
            if not files:
                raise PatchApplyError('hunks appear before any file header')
            files[-1][1].extend(hunks)
            continue
        key = next(l for l in reversed(file_lines)
                   if l.startswith(('+++ ', '--- ')))
        if key in key_to_idx:
            files[key_to_idx[key]][1].extend(hunks)
        else:
            key_to_idx[key] = len(files)
            files.append([file_lines, list(hunks)])
    return [(hdr, hks) for hdr, hks in files]


def _normalized_patch_text(text: str) -> str:
    """Rewrite a unified diff with each file's hunks sorted ascending by
    source line. `patch` applied Lang-50's descending-order hunks first
    hunk only, silently; ascending order is what every applier expects."""
    out: List[str] = []
    for header, hunks in _file_sections(text):
        out.extend(header)
        for _start, body in sorted(hunks, key=lambda h: h[0]):
            out.extend(body)
    return '\n'.join(out) + '\n'


class PatchedProjectBuilder:
    """Copy a buggy Defects4J checkout, apply a DRR patch, and compile it.

    DRR patches use `/src/...` path prefixes (no `a/`/`b/`), so we use
    `patch -p1` which strips the leading `/` to produce a relative path
    matching the project layout inside the checkout directory.

    Safety nets (P0.1): the patch must FULLY apply (PatchApplyError
    otherwise), and unless verify_trigger=False the bug's own trigger
    tests must fail on the buggy checkout and pass on the patched build
    (TriggerVerificationError otherwise).
    """

    def __init__(self, patched_root: str = config.D4J_CHECKOUT_ROOT,
                 diffcov: bool = False, divcap: bool = False):
        self.patched_root = patched_root
        # --diffcov: inject a hit counter into every patch-changed method
        # before compiling. Off by default; when off nothing below runs and
        # the build is byte-for-byte the one it always was.
        self.diffcov = diffcov
        self.diffcov_plan = None
        # --divcap: inject the divergence-observation calls instead (same
        # station, same flag discipline, its own directory). Off by default.
        self.divcap = divcap
        self.divcap_plan = None
        self._classpath_cache: dict = {}

    def build_patched_dir(self, buggy_dir: str, patch_path: str,
                          verify_trigger: bool = True) -> str:
        """Return a compiled patched copy of buggy_dir with the DRR patch
        applied. Idempotent: skips copy/patch/compile if the directory
        already exists. A failed copy/apply/compile removes the directory
        again — a half-built tree left behind would be silently reused as
        "already built" on the next run."""
        patched_dir = self._patched_dir_path(buggy_dir, patch_path)
        if not os.path.isdir(patched_dir):
            print(f"Copying {buggy_dir} → {patched_dir}")
            try:
                shutil.copytree(buggy_dir, patched_dir)
                self._apply_patch(patched_dir, patch_path)
                if self.diffcov:
                    self._instrument(patched_dir, patch_path)
                if self.divcap:
                    self._instrument_divcap(patched_dir, patch_path)
                subprocess.run(
                    ['defects4j', 'compile'],
                    cwd=patched_dir, check=True,
                )
            except BaseException:
                shutil.rmtree(patched_dir, ignore_errors=True)
                raise
        if self.diffcov and self.diffcov_plan is None:
            self._load_diffcov_plan(patched_dir)
        if self.divcap and self.divcap_plan is None:
            self._load_divcap_plan(patched_dir)
        if verify_trigger:
            self._verify_trigger_tests(buggy_dir, patched_dir)
        return patched_dir

    # ---- diff-hit instrumentation (--diffcov, measurement only) --------

    def _instrument(self, patched_dir: str, patch_path: str) -> None:
        """Inject the per-method hit counters into the patched WORKING COPY,
        after the patch applied and before it is compiled. Best-effort: an
        instrumentation failure must not cost the run its patched build, so
        it is reported and the build proceeds uninstrumented."""
        from java.execution import diffcov as diffcov_mod
        try:
            plan = diffcov_mod.instrument_patched_dir(
                patched_dir, patch_path, config.DIFFCOV_FLUSH_SECONDS)
        except Exception as exc:
            print(f"  [diffcov] instrumentation skipped: {exc}")
            return
        # Stored in its serialised form — the same shape a cached patched
        # dir hands back through _load_diffcov_plan.
        self.diffcov_plan = plan.as_dict()
        print(f"  [diffcov] instrumented {len(plan.methods)} changed "
              f"method(s); {len(plan.unmapped)} changed line(s) mapped to "
              f"no method")

    def _load_diffcov_plan(self, patched_dir: str) -> None:
        """Read back the plan a previous (idempotent-skip) build wrote."""
        try:
            with open(os.path.join(patched_dir,
                                   '.diffcov_methods.json')) as fh:
                self.diffcov_plan = json.load(fh)
        except (OSError, ValueError):
            self.diffcov_plan = None

    # ---- divergence capture (--divcap) ---------------------------------

    def _instrument_divcap(self, patched_dir: str, patch_path: str) -> None:
        """Inject the observation calls into the patched WORKING COPY, after
        the patch applied and before it is compiled. Best-effort, exactly
        like the diffcov twin: a failure leaves the build uninstrumented
        rather than costing the run its patched tree."""
        from java.execution import divcap as divcap_mod
        try:
            plan = divcap_mod.instrument_patched_dir(
                patched_dir, patch_path, config.DIVCAP_FLUSH_SECONDS,
                config.DIVCAP_MAX_SHAPES)
        except Exception as exc:
            print(f"  [divcap] instrumentation skipped: {exc}")
            return
        self.divcap_plan = plan.as_dict()
        print(f"  [divcap] instrumented {len(plan.targets)} changed "
              f"method(s); {len(plan.skipped)} without a capturable "
              f"observable")

    def _load_divcap_plan(self, patched_dir: str) -> None:
        from java.execution import divcap as divcap_mod
        try:
            with open(os.path.join(patched_dir,
                                   divcap_mod.PLAN_FILE)) as fh:
                self.divcap_plan = json.load(fh)
        except (OSError, ValueError):
            self.divcap_plan = None

    def build_divcap_buggy_dir(self, buggy_dir: str,
                               patched_dir: str) -> str:
        """The BUGGY twin of the instrumented patched build: the unpatched
        sources, the same methods watched, compiled.

        The signature list comes from the patched tree (that is where the
        diff maps), so a method the patch ADDED simply has no counterpart
        here and is recorded as unfound rather than guessed at. Same
        idempotence and same clean-up-on-failure rule as the patched build:
        a half-built tree left behind would be reused as "already built" and
        the capture would come back silently empty."""
        from java.execution import divcap as divcap_mod
        target = os.path.join(
            self.patched_root,
            f'{os.path.basename(buggy_dir.rstrip("/"))}_divcap_buggy')
        if not os.path.isdir(target):
            print(f"Copying {buggy_dir} → {target}")
            try:
                shutil.copytree(buggy_dir, target)
                divcap_mod.instrument_dir(
                    target, divcap_mod.read_wanted(patched_dir),
                    config.DIVCAP_FLUSH_SECONDS, config.DIVCAP_MAX_SHAPES)
                subprocess.run(['defects4j', 'compile'],
                               cwd=target, check=True)
            except BaseException:
                shutil.rmtree(target, ignore_errors=True)
                raise
        return target

    # ---- P0.1b: trigger-test safety net --------------------------------

    def verify_bug_reproduces(self, buggy_dir: str) -> None:
        """Cheap early gate: every d4j trigger test must FAIL on the
        unpatched buggy checkout, else TriggerVerificationError
        ('bug_not_reproduced'). Cached in the checkout via a marker file
        — the answer never changes for a given checkout.

        Side product (H2): the failure MESSAGE of each trigger test
        ("expected:<X> but was:<Y>", or the thrown exception) is saved to
        `.d4j_failure_messages.json` beside the marker. It names the
        exact observable that diverges and the wrong value the buggy
        build produces — the harness writer and the H3 acceptance gate
        read it via `trigger_failure_messages`."""
        marker = os.path.join(buggy_dir, '.d4j_bug_reproduced')
        msg_path = os.path.join(buggy_dir, '.d4j_failure_messages.json')
        if os.path.exists(marker) and os.path.exists(msg_path):
            return
        triggers = self._trigger_tests(buggy_dir)
        messages: dict = {}
        failing = self._failing_tests(buggy_dir, triggers,
                                      messages_out=messages)
        passing = sorted(set(triggers) - failing)
        if passing:
            raise TriggerVerificationError(
                'bug_not_reproduced',
                f'trigger test(s) PASS on the unpatched buggy checkout '
                f'{buggy_dir}: {passing} — the bug does not exist in this '
                f'environment; any harness verdict for it is meaningless')
        try:
            with open(msg_path, 'w') as fh:
                json.dump(messages, fh, indent=1)
        except OSError:
            pass   # message capture is best-effort; the gate stands alone
        with open(marker, 'w') as fh:
            fh.write('\n'.join(sorted(failing)) + '\n')

    @staticmethod
    def trigger_failure_messages(buggy_dir: str) -> dict:
        """`{'Class::method': failure message}` captured when
        verify_bug_reproduces ran the trigger tests on this checkout.
        Empty dict if the checkout predates message capture (its marker
        exists but no json) — callers must degrade gracefully."""
        msg_path = os.path.join(buggy_dir, '.d4j_failure_messages.json')
        try:
            with open(msg_path) as fh:
                data = json.load(fh)
            return data if isinstance(data, dict) else {}
        except (OSError, ValueError):
            return {}

    def _verify_trigger_tests(self, buggy_dir: str, patched_dir: str) -> None:
        """Both halves of the net, cached per patched build: the bug
        reproduces on buggy, and the patch makes the trigger tests pass."""
        marker = os.path.join(patched_dir, '.d4j_trigger_verified')
        if os.path.exists(marker):
            return
        self.verify_bug_reproduces(buggy_dir)
        triggers = self._trigger_tests(buggy_dir)
        still_failing = self._failing_tests(patched_dir, triggers)
        if still_failing:
            raise TriggerVerificationError(
                'bad_patch',
                f'trigger test(s) still FAIL on the patched build '
                f'{patched_dir}: {sorted(still_failing)} — the patch did '
                f'not fully apply or does not fix the bug')
        with open(marker, 'w') as fh:
            fh.write('\n'.join(sorted(triggers)) + '\n')

    @staticmethod
    def _trigger_tests(buggy_dir: str) -> List[str]:
        """`defects4j export -p tests.trigger` → ['Class::method', ...]."""
        result = subprocess.run(
            ['defects4j', 'export', '-p', 'tests.trigger'],
            cwd=buggy_dir, capture_output=True, text=True,
        )
        tests = [t.strip() for t in result.stdout.splitlines()
                 if '::' in t]
        if result.returncode != 0 or not tests:
            raise TriggerVerificationError(
                'bug_not_reproduced',
                f'could not export trigger tests from {buggy_dir}: '
                f'{(result.stderr or result.stdout).strip()[:300]}')
        return tests

    @staticmethod
    def _failing_tests(project_dir: str, tests: List[str],
                       messages_out: Optional[dict] = None) -> set:
        """Run each named test via `defects4j test -t`; return the subset
        that fails. A test whose run doesn't complete cleanly counts as
        failing — loudly, never silently.

        When `messages_out` is given, the failure detail defects4j writes
        to `<project_dir>/failing_tests` (the throwable line and message,
        e.g. 'AssertionFailedError: expected:<NaN> but was:<4.0>') is
        stored under the test's 'Class::method' key. The file is
        overwritten per `test -t` run, so it is read immediately after
        each one."""
        failing = set()
        detail_path = os.path.join(project_dir, 'failing_tests')
        for t in tests:
            result = subprocess.run(
                ['defects4j', 'test', '-t', t],
                cwd=project_dir, capture_output=True, text=True,
            )
            output = (result.stdout or '') + (result.stderr or '')
            m = re.search(r'Failing tests:\s*(\d+)', output)
            if result.returncode != 0 or m is None:
                print(f"  trigger net: `defects4j test -t {t}` did not "
                      f"complete cleanly in {project_dir} "
                      f"(rc={result.returncode}) — counting as failing")
                failing.add(t)
            elif int(m.group(1)) > 0:
                failing.add(t)
            if messages_out is not None and t in failing:
                try:
                    with open(detail_path, encoding='utf-8',
                              errors='replace') as fh:
                        detail = fh.read()
                except OSError:
                    continue
                # Keep the headline + message lines, stop at the stack
                # trace — the message is the information, frames are bulk.
                msg_lines = []
                for line in detail.splitlines():
                    if line.lstrip().startswith('at '):
                        break
                    if line.strip():
                        msg_lines.append(line.rstrip())
                    if len(msg_lines) >= 12:
                        break
                if msg_lines:
                    messages_out[t] = '\n'.join(msg_lines)[:1500]
        return failing

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
        # An instrumented build gets its OWN directory. build_patched_dir is
        # idempotent on directory existence alone, so sharing the path would
        # let a cached uninstrumented tree be reused as "already built" and
        # the measurement would silently come back empty.
        suffix = ('_diffcov' if self.diffcov else '') + (
            '_divcap' if self.divcap else '')
        return os.path.join(self.patched_root,
                            f'{base}_patched_{patch_stem}{suffix}')

    @staticmethod
    def _apply_patch(target_dir: str, patch_path: str) -> None:
        """Fully apply the patch or raise PatchApplyError.

        Guarantees, in order:
        1. counted parsing rejects truncated/malformed/empty patch files
           up front (the original Math-2/SOFix file);
        2. hunks are re-sorted ascending by source line per file before
           applying (`patch` silently dropped Lang-50's out-of-order
           second hunk);
        3. the applier's exit code is checked and any *.rej file left
           behind is an error, never a warning;
        4. after applying, the WHOLE patch must reverse-apply cleanly in
           a dry run — the strongest available "everything landed" check
           (a reversed input patch also dies here: its forward apply
           fails step 3).
        """
        with open(patch_path, encoding='utf-8', errors='replace') as fh:
            raw = fh.read()
        try:
            normalized = _normalized_patch_text(raw)
        except PatchApplyError as exc:
            raise PatchApplyError(f'{patch_path}: {exc}') from None
        fd, norm_path = tempfile.mkstemp(suffix='.patch')
        try:
            with os.fdopen(fd, 'w') as fh:
                fh.write(normalized)
            # DRR patches have `/src/...` paths; -p1 strips the leading
            # `/`. git apply is atomic (all-or-nothing), so try it first;
            # fall back to `patch` for the diffs git rejects entirely.
            git_result = subprocess.run(
                ['git', 'apply', '--whitespace=fix', norm_path],
                cwd=target_dir, capture_output=True, text=True,
            )
            if git_result.returncode != 0:
                # --forward is load-bearing: plain `patch --batch` answers
                # "Assume -R?" with YES on a reversed patch and silently
                # applies it BACKWARDS (measured on the Math-2 .bak — the
                # "patched" tree became the dev fix). --forward refuses
                # with a nonzero exit instead; the reverse-verify below
                # backstops any hunk it merely skips.
                patch_result = subprocess.run(
                    ['patch', '-p1', '--forward', '--batch',
                     '--input', norm_path],
                    cwd=target_dir, capture_output=True, text=True,
                )
                if patch_result.returncode != 0:
                    raise PatchApplyError(
                        f'{patch_path} failed to apply.\n'
                        f'git apply: {git_result.stderr.strip()[:500]}\n'
                        f'patch:     {(patch_result.stdout + patch_result.stderr).strip()[:500]}')
            rejects = [os.path.join(root, f)
                       for root, _dirs, files in os.walk(target_dir)
                       for f in files if f.endswith('.rej')]
            if rejects:
                raise PatchApplyError(
                    f'{patch_path}: applier left reject files (some hunks '
                    f'did NOT apply): {rejects}')
            verify = subprocess.run(
                ['patch', '-p1', '--reverse', '--dry-run', '--batch',
                 '--ignore-whitespace', '--input', norm_path],
                cwd=target_dir, capture_output=True, text=True,
            )
            if verify.returncode != 0:
                raise PatchApplyError(
                    f'{patch_path}: applied without error but the tree is '
                    f'NOT in the fully-patched state (reverse dry-run '
                    f'failed):\n'
                    f'{(verify.stdout + verify.stderr).strip()[:500]}')
        finally:
            os.unlink(norm_path)


class FuzzRunner:
    """Run Jazzer on each compiled harness against a patched project and
    report whether it still finds a crash."""

    def __init__(self,
                 jazzer_standalone_jar: str,
                 timeout_seconds: int = config.FUZZ_TIMEOUT_SECONDS,
                 expected_exceptions: Optional[List[str]] = None,
                 jazzer_api_jar: Optional[str] = None,
                 seed_literals: Optional[List[str]] = None,
                 diffcov: bool = False):
        self.jazzer_standalone_jar = jazzer_standalone_jar
        self.timeout_seconds = timeout_seconds
        self.expected_exceptions = expected_exceptions or []
        # API jar (FuzzedDataProvider) for the runtime classpath; see
        # run_jazzer. Defaults there to config.JAZZER_API_JAR if None.
        self.jazzer_api_jar = jazzer_api_jar
        # Literal seeds (the failing test's own literals plus their
        # mechanical variations — java_source.literal_variations) written
        # into the patched-side seed corpus. A short fuzz budget then
        # tries the discriminating input NEIGHBOURHOOD deterministically
        # instead of hoping random bytes reach it (batch5: every
        # invented check was present and stayed latent because 20s of
        # fuzz never generated an exponent-plus-suffix string).
        self.seed_literals = list(seed_literals or [])
        # --diffcov: count entries into the patch-changed methods during the
        # patched-side fuzz. Off by default; measurement only.
        self.diffcov = diffcov
        self.diffcov_plan = None

    def run_all(self,
                successful_results: List[BuildResult],
                patch_path: str,
                buggy_dir: str) -> List[FuzzRunResult]:
        """Apply patch, compile patched project, then fuzz every harness."""
        builder = PatchedProjectBuilder(diffcov=self.diffcov)
        patched_dir = builder.build_patched_dir(buggy_dir, patch_path)
        self.diffcov_plan = builder.diffcov_plan
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
        from java.execution.oracle_strength import exception_headline_pairs
        # Consumer split (batch-8 smoke finding): the returned list stays
        # CAPPED, because every existing consumer expects that. The uncapped
        # text is stashed alongside for the one MECHANICAL reader that needs
        # it — 8.4's raw-vs-pinned comparison, whose input sits at the end of
        # the message and was being deleted by the cap. Run-local, reset on
        # every call, so a stale mapping can never be read as this call's.
        self.last_full_headlines = {}
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
        pairs = exception_headline_pairs(
            outcome.stdout + '\n' + outcome.stderr)
        self.last_full_headlines = {capped: full for capped, full in pairs}
        return [capped for capped, _full in pairs]

    def keep_going_output(self,
                          harness_path: str,
                          class_name: str,
                          project_cp: str,
                          keep_going: int = 16,
                          timeout_seconds: int = 45) -> str:
        """Fuzz ONE harness against an arbitrary classpath with
        --keep_going and return the raw output. P0.4 uses this on the
        BUGGY classpath at acceptance to record which named oracles ever
        fire there — a check that never fires on buggy is flagged latent
        instead of meeting its first-ever execution on the patched
        build. Returns '' on any error (caller treats that as
        no-information, never as 'all oracles exercised')."""
        try:
            outcome = run_jazzer(
                jazzer_standalone_jar=self.jazzer_standalone_jar,
                target_class=class_name,
                harness_dir=os.path.dirname(harness_path),
                project_cp=project_cp,
                timeout_seconds=timeout_seconds,
                expected_exceptions=self.expected_exceptions,
                jazzer_api_jar=self.jazzer_api_jar,
                keep_going=keep_going,
            )
        except Exception as exc:
            print(f"  (buggy keep-going run failed: {exc})")
            return ''
        return outcome.stdout + '\n' + outcome.stderr

    def _run_one(self, build_result: BuildResult,
                 patched_cp: str) -> FuzzRunResult:
        harness_dir = os.path.dirname(build_result.harness_path)
        started_at = time.time()
        # JD1: seed the patched-side fuzz with the exact inputs that fired
        # this harness's checks on the BUGGY build (the crash-* artifacts
        # the acceptance gate wrote to <harness_dir>/crashes/). Those are
        # precisely the inputs most likely to still fire on an overfit
        # that special-cased only the reported input — previously the
        # patched fuzz had to rediscover them by luck. Copied into a
        # separate seed dir so patched-side artifacts never mix with the
        # buggy-side evidence. Firewall-clean: buggy-side data only.
        seed_dir = None
        buggy_artifacts = [
            p for p in glob.glob(os.path.join(harness_dir, 'crashes',
                                              'crash-*'))
            if os.path.getmtime(p) < started_at]
        if buggy_artifacts:
            seed_dir = os.path.join(harness_dir, 'seeds_from_buggy')
            os.makedirs(seed_dir, exist_ok=True)
            for p in buggy_artifacts:
                shutil.copy2(p, os.path.join(seed_dir, os.path.basename(p)))
            print(f"  [JD1] seeding patched fuzz with "
                  f"{len(buggy_artifacts)} buggy-side firing input(s)")
        if self.seed_literals:
            if seed_dir is None:
                seed_dir = os.path.join(harness_dir, 'seeds_from_buggy')
                os.makedirs(seed_dir, exist_ok=True)
            _n = 0
            for i, lit in enumerate(self.seed_literals):
                try:
                    with open(os.path.join(seed_dir, f'lit_{i:03d}'),
                              'w', encoding='utf-8',
                              errors='replace') as fh:
                        fh.write(lit)
                    _n += 1
                except OSError:
                    continue
            if _n:
                print(f"  [corpus] {_n} literal-variation seed(s) "
                      f"added to the patched fuzz")
                try:
                    from llm import record_event
                    record_event(
                        'deterministic', method='corpus-seed',
                        target=os.path.basename(harness_dir),
                        output=(f'{_n} literal-variation seeds into the '
                                f'patched-side fuzz corpus'),
                        detail={'sample': self.seed_literals[:8]})
                except Exception:
                    pass
        # keep_going: without it the patched fuzz STOPS at the first crash
        # — and with JD1 seeding, the first input is often a buggy-side
        # artifact whose junk rejection (a dismissible NFE) would end the
        # run before any real check executes. Lang-27 was caught or missed
        # across runs on exactly this coin flip (struggle10 vs full30 vs
        # hfix11): same checks, different fencing luck. Continuing past
        # early findings lets every oracle have its turn; the judge
        # already handles multiple headlines (collect_fired_oracles).
        outcome = run_jazzer(
            jazzer_standalone_jar=self.jazzer_standalone_jar,
            target_class=build_result.class_name,
            harness_dir=harness_dir,
            project_cp=patched_cp,
            timeout_seconds=self.timeout_seconds,
            expected_exceptions=self.expected_exceptions,
            jazzer_api_jar=self.jazzer_api_jar,
            corpus_dir=seed_dir,
            keep_going=8,
            diffcov_out=(os.path.join(harness_dir, 'diffcov.out')
                         if self.diffcov else None),
        )
        if outcome.diffcov is not None:
            _hit = sum(1 for n in outcome.diffcov.values() if n)
            print(f"  [diffcov] {_hit}/{len(outcome.diffcov)} changed "
                  f"method(s) reached: {outcome.diffcov}")
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
            diffcov=outcome.diffcov,
        )

    def replay_input_result(self,
                            harness_path: str,
                            class_name: str,
                            project_cp: str,
                            input_file: str,
                            timeout_seconds: int = 30
                            ) -> Tuple[str, Optional[str]]:
        """Execute ONE persisted input against `project_cp` and report a
        THREE-way outcome (status, sig):

          * "crashed" — the input triggered a crash; sig = crash_signature.
          * "clean"   — the replay ran to completion without triggering.
          * "error"   — run_jazzer raised, the subprocess timed out, or it
            exited nonzero without a recognised finding; sig = None.

        Spec B (G1): a differential replay must never say "clean" when it
        errored or never arrived — an infrastructure failure must not
        manufacture evidence against the patch. run_jazzer collapses "ran
        clean" and several infra-failure modes onto triggered=False, so we
        separate them here: only a run that COMPLETED (returncode 0, no
        timeout) and did not trigger is "clean"; every other non-triggering
        outcome is "error". run_jazzer cannot distinguish a genuinely clean
        nonzero exit from a Jazzer/JVM startup failure, so that ambiguity
        maps to "error" (never to "clean"), per the never-manufacture rule.
        The caller passes the BUGGY classpath to ask whether a firing is
        pre-existing rather than patch-caused."""
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
            return "error", None
        if outcome.triggered:
            return "crashed", crash_signature(outcome.combined_output)
        if outcome.timed_out or outcome.returncode != 0:
            # Did not trigger, but did not cleanly complete either — a hung
            # or failed replay, not evidence the input runs clean on buggy.
            return "error", None
        return "clean", None

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
        ask whether a firing is pre-existing rather than patch-caused.

        Thin wrapper over replay_input_result for callers that only need the
        crash signature: "clean" and "error" both collapse to None (the
        historical behaviour), while replay_input_result keeps them apart."""
        status, sig = self.replay_input_result(
            harness_path, class_name, project_cp, input_file,
            timeout_seconds)
        return sig if status == "crashed" else None

    def replay_input_oracles(self,
                             harness_path: str,
                             class_name: str,
                             project_cp: str,
                             input_file: str,
                             timeout_seconds: int = 30) -> Optional[set]:
        """Replay ONE persisted crashing input and return the set of
        oracle IDs that fire (empty set = ran clean), or None when the
        replay itself errors (caller must treat None as ABSTAIN).

        P0.4 latent firings need this: the buggy-side acceptance scan
        stops at the first firing oracle per input, so a sound check
        sitting behind an always-firing seed oracle looks latent
        (Lang-60's capacity check). Whether ITS exact input fires it on
        the buggy build is the evidence latency alone cannot give."""
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
            return set()
        from java.parsing.java_source import oracle_ids_in_text
        return oracle_ids_in_text(outcome.combined_output)

    def replay_input_signatures_result(self,
                                       harness_path: str,
                                       class_name: str,
                                       project_cp: str,
                                       input_file: str,
                                       timeout_seconds: int = 30
                                       ) -> Tuple[str, Optional[str],
                                                  Optional[str]]:
        """Like replay_input_result, but returns (status,
        headline_signature, cause_signature) — the P0.3 laundering check
        needs the cause chain of the buggy-build replay, not just its
        headline. Same Spec B rule as replay_input_result: a replay that
        errored or never completed must report "error", never read as a
        clean buggy run (which would manufacture evidence against the
        patch)."""
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
            return "error", None, None
        if outcome.triggered:
            return ("crashed",
                    crash_signature(outcome.combined_output),
                    cause_signature(outcome.combined_output))
        if outcome.timed_out or outcome.returncode != 0:
            return "error", None, None
        return "clean", None, None

    def replay_input_signatures(self,
                                harness_path: str,
                                class_name: str,
                                project_cp: str,
                                input_file: str,
                                timeout_seconds: int = 30
                                ) -> Tuple[Optional[str], Optional[str]]:
        """Thin wrapper over replay_input_signatures_result for callers that
        do not need the error/clean distinction (both collapse to
        (None, None), the historical behaviour)."""
        _status, sig, cause = self.replay_input_signatures_result(
            harness_path, class_name, project_cp, input_file,
            timeout_seconds)
        return sig, cause

    def _build_diversion_variant(self,
                                 source: str,
                                 builder,
                                 project_dir: str,
                                 src_path: str,
                                 subdir_leaf: str):
        """Compile a DIVERSION-INSTRUMENTED copy of `source` (see
        `oracle_mute.instrument_diversion`) under `<project_dir>/fuzz/...`.

        Returns the BuildResult, or None when the transform does not apply or
        the variant does not compile. Every failure path returns None so the
        caller degrades to "diversion unknown" — never to "ran clean"."""
        if builder is None or project_dir is None or not source:
            return None
        try:
            from java.execution.oracle_mute import instrument_diversion
            instrumented = instrument_diversion(source)
        except Exception as exc:
            print(f"  (diversion transform raised: {exc})")
            return None
        if not instrumented:
            return None
        harness_dir = os.path.dirname(os.path.abspath(src_path))
        fuzz_root = os.path.join(os.path.abspath(project_dir), 'fuzz')
        rel = os.path.relpath(harness_dir, fuzz_root)
        if rel.startswith('..') or os.path.isabs(rel):
            rel = os.path.basename(os.path.normpath(harness_dir)) or 'harness'
        try:
            build_result = builder.build(instrumented, project_dir,
                                         os.path.join(rel, subdir_leaf))
        except Exception as exc:
            print(f"  (diversion variant build raised: {exc})")
            return None
        if not build_result.compiled:
            return None
        return build_result

    def replay_input_report(self,
                            harness_path: str,
                            class_name: str,
                            project_cp: str,
                            input_file: str,
                            timeout_seconds: int = 30,
                            builder=None,
                            buggy_dir: str = None
                            ) -> Tuple[Optional[set], str, Optional[bool]]:
        """Replay ONE persisted input and return (fired_oracle_ids,
        full_output, diverted). ids None = the replay itself ERRORED (caller
        must ABSTAIN — never a substitute for 'ran clean'); empty set = ran
        clean. The full output is returned so the caller can look for
        exception types anywhere in the run's crash reports
        (exception_types_in_output) — the headline signature alone
        misses a defect exception the harness fences and rethrows under
        its own alarm type.

        `diverted` (cycle-6) answers the question a bare replay cannot: did
        execution actually REACH the checks, or did one of the harness's own
        `catch (...) { return; }` swallows fire and return early?

          * True  — a swallow-return catch fired on this input, so anything
            BELOW it was never evaluated. "No oracle fired" means nothing.
          * False — no swallow fired; the run really did reach the checks.
          * None  — unknown (no `builder`/`buggy_dir` given, the harness could
            not be instrumented, the variant did not compile, or the stats
            line never appeared). Callers must treat None as unknown and must
            NOT emit the "ran clean, so the patch introduced it" claim.

        When `builder` and `buggy_dir` are supplied the instrumented variant is
        what actually runs (it is behaviourally identical bar the counters), so
        the diversion fact costs one extra javac and no extra Jazzer run. Any
        failure falls back to replaying the ORIGINAL harness with
        diverted=None — never worse than before."""
        from java.execution.oracle_mute import parse_skipped

        run_class, run_dir = class_name, os.path.dirname(harness_path)
        instrumented = False
        if builder is not None and buggy_dir is not None:
            src_path = harness_path
            if os.path.isdir(harness_path):
                javas = glob.glob(os.path.join(harness_path, '*.java'))
                src_path = javas[0] if javas else None
            source = None
            if src_path:
                try:
                    with open(src_path, encoding='utf-8',
                              errors='replace') as fh:
                        source = fh.read()
                except OSError as exc:
                    print(f"  (diversion probe: could not read harness "
                          f"source: {exc})")
            if source:
                br = self._build_diversion_variant(
                    source, builder, buggy_dir, src_path, 'diverted_0')
                if br is not None:
                    run_class = br.class_name
                    run_dir = os.path.dirname(br.harness_path)
                    instrumented = True
        # AUDIT (cycle-6): did the diversion transform actually get applied on
        # this replay? A trace with no `diverted` claim is otherwise ambiguous
        # between "no swallow fired" and "the probe never ran".
        _ev('cycle6_diversion_considered', target=class_name,
            output=f'instrumented={instrumented}',
            reason=('report-replay; diversion variant compiled and is what '
                    'runs' if instrumented else
                    'report-replay; NOT instrumented (no builder/buggy_dir, '
                    'unreadable source, or the variant did not compile) — '
                    'diverted will be None'))

        try:
            outcome = run_jazzer(
                jazzer_standalone_jar=self.jazzer_standalone_jar,
                target_class=run_class,
                harness_dir=run_dir,
                project_cp=project_cp,
                timeout_seconds=timeout_seconds,
                expected_exceptions=self.expected_exceptions,
                jazzer_api_jar=self.jazzer_api_jar,
                input_file=input_file,
            )
        except Exception as exc:
            print(f"  (single-input replay failed: {exc})")
            _ev('cycle6_diversion_decided', target=class_name,
                output='diverted=None',
                reason=f'report-replay raised ({type(exc).__name__}: {exc}) — '
                       f'nothing measured')
            return None, '', None
        out = outcome.combined_output
        diverted = None
        if instrumented:
            skipped = parse_skipped(out)
            if skipped is not None:
                diverted = skipped > 0
        _ev('cycle6_diversion_decided', target=class_name,
            output=f'diverted={diverted}',
            reason=('report-replay; swallow-return counters read from the run'
                    if instrumented else
                    'report-replay; not instrumented — diversion UNKNOWN'))
        if not outcome.triggered:
            return set(), out, diverted
        from java.parsing.java_source import oracle_ids_in_text
        return oracle_ids_in_text(out), out, diverted

    def replay_input_muted(self,
                           harness_path: str,
                           class_name: str,
                           project_cp: str,
                           input_file: str,
                           mute_ids=None,
                           mute_all: bool = False,
                           builder=None,
                           buggy_dir: str = None,
                           timeout_seconds: int = 30,
                           variant_tag: str = "0"
                           ) -> Tuple[str, Optional[set], str, Optional[bool]]:
        """Replay ONE input against a MUTED build of the harness and report
        `(status, fired_ids, output, diverted)`.

        Silencing the harness's shadowing alarm throws (`oracle_mute`) and
        re-replaying the SAME input computes the per-input fact a raw replay
        cannot when a shadowing throw ends the run first: does THIS check
        fire / crash on `project_cp` once the shadow is muted?

        `status` is one of:
          * "crashed"     — the muted build triggered on the input;
            `fired_ids` = the oracle ids in the output (a set, possibly
            empty when a non-oracle crash escaped).
          * "clean"       — the muted build ran the input to completion
            without triggering; `fired_ids` = empty set.
          * "error"       — run_jazzer raised, the subprocess timed out, or
            it exited nonzero without a recognised finding; `fired_ids`
            None. Same never-manufacture rule as replay_input_result: an
            ambiguous non-clean outcome is "error", never "clean".
          * "mute_failed" — no `builder`, the harness source could not be
            read, or the muted variant did not COMPILE (removing a
            guaranteed throw can break Java's definite-return analysis).
            `fired_ids` None; the caller keeps its pre-existing
            UNKNOWN/SHADOWED fact unchanged — never worse than today.

        `diverted` (cycle-6) is True/False/None exactly as in
        `replay_input_report`: True when one of the harness's own
        `catch (...) { return; }` swallows fired on this input (so execution
        never reached the checks below it and a quiet run proves nothing),
        False when none did, None when unknown. The diversion counters are
        folded into the SAME muted variant, so the fact is free; if the
        combined transform fails to compile, the plain muted variant is built
        instead and `diverted` degrades to None — the muted replay itself is
        never lost.

        `builder` must be a HarnessBuilder; the muted variant is compiled
        against `buggy_dir` (the project dir the caller wants — the buggy
        build for a shadowed-fact check) via `builder.build`, which writes
        under `<buggy_dir>/fuzz/<subdir>`. `output` is Jazzer's combined
        stdout+stderr (empty when compile/setup failed)."""
        if builder is None or buggy_dir is None:
            return "mute_failed", None, '', None

        from java.execution.oracle_mute import (mute_oracles,
                                                instrument_diversion,
                                                parse_skipped)

        # harness_path is the .java in the run.py flow (BuildResult.harness_path);
        # tolerate a directory too and pick the harness .java inside it.
        src_path = harness_path
        if os.path.isdir(harness_path):
            javas = [p for p in glob.glob(os.path.join(harness_path, '*.java'))]
            if not javas:
                return "mute_failed", None, '', None
            src_path = javas[0]
        try:
            with open(src_path, encoding='utf-8', errors='replace') as fh:
                source = fh.read()
        except OSError as exc:
            print(f"  (muted replay: could not read harness source: {exc})")
            return "mute_failed", None, '', None

        muted_source = mute_oracles(source, mute_ids=mute_ids,
                                    mute_all=mute_all)

        # Place the muted variant beside the original when it lives under
        # <buggy_dir>/fuzz (the run.py layout); build() joins buggy_dir/fuzz/
        # <output_subdir>, so we derive that subdir. A harness outside the
        # fuzz tree falls back to a uniquely-named subdir.
        harness_dir = os.path.dirname(os.path.abspath(src_path))
        fuzz_root = os.path.join(os.path.abspath(buggy_dir), 'fuzz')
        rel = os.path.relpath(harness_dir, fuzz_root)
        if rel.startswith('..') or os.path.isabs(rel):
            rel = os.path.basename(os.path.normpath(harness_dir)) or 'harness'
        # `variant_tag` keeps one build directory per muted PASS (cycle-6 item
        # 4: the mute set grows across passes, so each pass compiles a
        # different source and must not overwrite the previous pass's build).
        output_subdir = os.path.join(rel, 'muted_%s' % variant_tag)

        # Cycle-6: fold the diversion counters into the SAME variant so the
        # swallow fact costs nothing extra. Strictly fail-open — if the
        # combined source does not compile we fall back to the plain muted
        # variant and report diversion as unknown, so this can never cost us
        # the muted replay itself.
        build_result = None
        instrumented = False
        try:
            combined = instrument_diversion(muted_source)
        except Exception as exc:
            print(f"  (diversion transform raised: {exc})")
            combined = None
        if combined:
            try:
                _br = builder.build(combined, buggy_dir,
                                    os.path.join(rel,
                                                 'muted_div_%s' % variant_tag))
            except Exception as exc:
                print(f"  (muted+diversion variant build raised: {exc})")
                _br = None
            if _br is not None and _br.compiled:
                build_result = _br
                instrumented = True
        # AUDIT (cycle-6): whether the muted variant is ALSO carrying the
        # diversion counters. Survives a green leg; the print does not.
        _ev('cycle6_diversion_considered', target=class_name,
            output=f'instrumented={instrumented}',
            reason=('muted-replay pass %s; muted+diversion variant compiled '
                    'and is what runs' % variant_tag if instrumented else
                    'muted-replay pass %s; diversion transform unavailable or '
                    'the combined variant did not compile — falling back to '
                    'the plain muted variant, diverted will be None'
                    % variant_tag))

        if build_result is None:
            try:
                build_result = builder.build(muted_source, buggy_dir,
                                             output_subdir)
            except Exception as exc:
                print(f"  (muted variant build raised: {exc})")
                _ev('cycle6_diversion_decided', target=class_name,
                    output='diverted=None',
                    reason=f'muted variant build raised '
                           f'({type(exc).__name__}) — nothing measured')
                return "mute_failed", None, '', None
            if not build_result.compiled:
                # Expected sometimes: silencing a guaranteed throw can break
                # definite-return analysis. Caller keeps its cycle-1 fact.
                _ev('cycle6_diversion_decided', target=class_name,
                    output='diverted=None',
                    reason='muted variant did not compile — nothing measured')
                return "mute_failed", None, '', None

        try:
            outcome = run_jazzer(
                jazzer_standalone_jar=self.jazzer_standalone_jar,
                target_class=build_result.class_name,
                harness_dir=os.path.dirname(build_result.harness_path),
                project_cp=project_cp,
                timeout_seconds=timeout_seconds,
                expected_exceptions=self.expected_exceptions,
                jazzer_api_jar=self.jazzer_api_jar,
                input_file=input_file,
            )
        except Exception as exc:
            print(f"  (muted single-input replay failed: {exc})")
            _ev('cycle6_diversion_decided', target=class_name,
                output='diverted=None',
                reason=f'muted replay raised ({type(exc).__name__}: {exc}) — '
                       f'nothing measured')
            return "error", None, '', None

        out = outcome.combined_output
        diverted = None
        if instrumented:
            skipped = parse_skipped(out)
            if skipped is not None:
                diverted = skipped > 0
        _ev('cycle6_diversion_decided', target=class_name,
            output=f'diverted={diverted}',
            reason=('muted-replay pass %s; swallow-return counters read from '
                    'the run' % variant_tag if instrumented else
                    'muted-replay pass %s; not instrumented — diversion '
                    'UNKNOWN' % variant_tag))
        if outcome.triggered:
            from java.parsing.java_source import oracle_ids_in_text
            return "crashed", oracle_ids_in_text(out), out, diverted
        if outcome.timed_out or outcome.returncode != 0:
            return "error", None, out, diverted
        return "clean", set(), out, diverted

    def replay_input_isolated(self,
                              harness_path: str,
                              class_name: str,
                              project_cp: str,
                              input_file: str,
                              target_id: str,
                              builder=None,
                              buggy_dir: str = None,
                              timeout_seconds: int = 30,
                              variant_tag: str = "0"
                              ) -> Tuple[str, Optional[str], str]:
        """Replay ONE input against an ISOLATED build of the harness — only the
        `target_id` check can raise — and report `(status, message, output)`.

        The muted replay silences the shadowing checks it has SEEN fire, one
        pass at a time, and gives up when the mute set stops growing. This does
        the whole job in one shot: `oracle_mute.instrument_for_counting`
        (M-v2) mutes EVERY sibling alarm — tagged or untagged — and turns the
        target's own throw into a tally that also PRINTS its message, so
        nothing in the harness can end the run before the target speaks and
        the target's own throw cannot end it either. What comes back is the
        value the check observed on `project_cp` at this exact input, which is
        the reading a shadowed replay never produces.

        `status` is one of:
          * "fired"          — the target's alarm was reached; `message` is
            its text (the first `[relfire]` line, marker stripped).
          * "silent"         — the isolated run completed and the target never
            fired; `message` None. Evidence, but not a value.
          * "error"          — run_jazzer raised, timed out, or exited nonzero
            without producing the target's message; `message` None.
          * "isolate_failed" — no `builder`/`buggy_dir`, the harness source
            could not be read, the transform did not apply (no such target id,
            no entrypoint), or the isolated variant did not COMPILE.

        Every non-"fired" status carries `message=None`, so a caller can only
        ever reason from a reading it actually obtained. Same never-manufacture
        rule as the rest of this class: an ambiguous outcome is a failure
        status, never a clean reading."""
        if builder is None or buggy_dir is None or not target_id:
            return "isolate_failed", None, ''

        from java.execution.oracle_mute import instrument_for_counting
        from java.relations.relation_screen import harvest_relfire_lines

        src_path = harness_path
        if os.path.isdir(harness_path):
            javas = glob.glob(os.path.join(harness_path, '*.java'))
            if not javas:
                return "isolate_failed", None, ''
            src_path = javas[0]
        try:
            with open(src_path, encoding='utf-8', errors='replace') as fh:
                source = fh.read()
        except OSError as exc:
            print(f"  (isolated replay: could not read harness source: {exc})")
            return "isolate_failed", None, ''

        try:
            isolated = instrument_for_counting(source, target_id,
                                               record_firing=True)
        except Exception as exc:
            print(f"  (isolation transform raised: {exc})")
            return "isolate_failed", None, ''
        if not isolated:
            return "isolate_failed", None, ''

        harness_dir = os.path.dirname(os.path.abspath(src_path))
        fuzz_root = os.path.join(os.path.abspath(buggy_dir), 'fuzz')
        rel = os.path.relpath(harness_dir, fuzz_root)
        if rel.startswith('..') or os.path.isabs(rel):
            rel = os.path.basename(os.path.normpath(harness_dir)) or 'harness'
        try:
            build_result = builder.build(
                isolated, buggy_dir,
                os.path.join(rel, 'isolated_%s' % variant_tag))
        except Exception as exc:
            print(f"  (isolated variant build raised: {exc})")
            return "isolate_failed", None, ''
        if not build_result.compiled:
            # Expected sometimes: replacing a guaranteed throw can break
            # Java's definite-return analysis, exactly as muting one can.
            return "isolate_failed", None, ''

        try:
            outcome = run_jazzer(
                jazzer_standalone_jar=self.jazzer_standalone_jar,
                target_class=build_result.class_name,
                harness_dir=os.path.dirname(build_result.harness_path),
                project_cp=project_cp,
                timeout_seconds=timeout_seconds,
                expected_exceptions=self.expected_exceptions,
                jazzer_api_jar=self.jazzer_api_jar,
                input_file=input_file,
            )
        except Exception as exc:
            print(f"  (isolated single-input replay failed: {exc})")
            return "error", None, ''

        out = outcome.combined_output
        lines = harvest_relfire_lines(out, cap=1)
        if lines:
            msg = lines[0][len('[relfire]'):].strip()
            if msg:
                return "fired", msg, out
            return "error", None, out
        if outcome.timed_out or outcome.returncode != 0:
            return "error", None, out
        return "silent", None, out


# ---------------------------------------------------------------------------
# Cycle-6 item 4, PART A — iterate the mute set instead of giving up after one
# pass.
#
# `replay_input_muted` answers "does THIS check fire on the buggy build at this
# exact input?" by silencing the shadowing check(s) and replaying. When the
# muted run crashes at yet ANOTHER sibling alarm the target still never got to
# speak, and a single bounded pass gives up with the honest UNKNOWN wording
# (night20b: Closure-62 `end-of-line-caret`, Math-65 `chiSquare-inversely-…`
# shadowed by `circle-dense-errors-0` — both measurements NEVER COLLECTED).
# That sibling is simply a NEW shadow: add it to the mute set and replay again.
#
# Strictly bounded (each pass costs a Jazzer run): at most
# `MAX_EXTRA_MUTED_PASSES` passes beyond the first, and iteration also stops
# early the moment the mute set stops growing or a pass errors. Exhausting the
# passes returns the last result unchanged, which yields the current UNKNOWN
# wording — never a fabricated fact.
# ---------------------------------------------------------------------------

MAX_EXTRA_MUTED_PASSES = 3

# Greppable prefix for the per-pass audit line (see `iterate_muted_replay`).
MUTED_PASS_LOG_PREFIX = "[muted-replay pass]"


def _muted_target_spoke(target_ids, esc_type, fired_ids, output) -> bool:
    """Did the TARGET firing reproduce on this muted pass?

    Same rule `muted_replay_note` applies when it words the result: an oracle
    firing is identified by id; an ESCAPED exception (no oracle id) by its
    exception type appearing among the run's observed types."""
    target_ids = set(target_ids or ())
    fired_ids = set(fired_ids or ())
    if target_ids:
        return bool(target_ids & fired_ids)
    if esc_type:
        try:
            return esc_type in exception_types_in_output(output or '')
        except Exception:
            return False
    return False


def iterate_muted_replay(replay_fn, target_ids, mute_ids, esc_type=None,
                         max_extra_passes: int = MAX_EXTRA_MUTED_PASSES,
                         log=None):
    """Replay one input against progressively larger mute sets.

    `replay_fn(mute_ids, pass_index)` must run ONE muted replay and return
    `replay_input_muted`'s 4-tuple `(status, fired_ids, output, diverted)`;
    `pass_index` is 1-based and exists so the caller can give each pass its own
    build directory.

    Iteration rule — after a pass whose status is "crashed" and in which the
    target stayed quiet, every fired id that is not already muted is a NEW
    shadow: it is added to the mute set and the input is replayed again.
    Iteration stops on the FIRST of:

      * the target fired (question answered),
      * the run completed clean (question answered — the existence proof),
      * status "error"/"mute_failed", or `replay_fn` raising (nothing learned;
        the caller's UNKNOWN wording stands),
      * no new shadow id — the mute set stopped growing, so another pass would
        replay exactly the same build,
      * `max_extra_passes` passes beyond the first have been spent.

    Returns `(status, fired_ids, output, diverted, mute_ids_final, passes)`.
    The first four are exactly what `replay_input_muted` returned on the LAST
    pass, so `muted_replay_note`'s semantics are untouched — only the answer
    becomes reachable; `mute_ids_final` is the mute set that produced them (the
    set the note should name as silenced) and `passes` the number of Jazzer
    replays spent.

    Never fabricates: a bound hit returns the last real result, and every exit
    is one of `replay_input_muted`'s own statuses.

    AUDIT (cycle-6 observability): the per-pass line was print-only, so it died
    with `run.log` on every successful leg. It is now ALSO recorded —
    `cycle6_muted_replay_considered` once on entry, `cycle6_muted_replay_pass`
    once per pass (mute-set size + the pass's stop/continue reason), and
    `cycle6_muted_replay_decided` on the single exit — so a green trace.md
    shows how many Jazzer replays this actually spent and why it stopped."""
    emit = log or print
    mute_set = set(mute_ids or ())
    target_ids = set(target_ids or ())
    try:
        max_extra_passes = max(0, int(max_extra_passes))
    except (TypeError, ValueError):
        max_extra_passes = 0
    passes = 0
    _tgt = ",".join(sorted(target_ids)) or (esc_type or 'firing')
    _ev('cycle6_muted_replay_considered', target=_tgt,
        output='mute_set_size=%d' % len(mute_set),
        reason='iterating the muted re-replay; up to %d pass(es), starting '
               'mute set %s' % (max_extra_passes + 1,
                                ",".join(sorted(mute_set)) or "-"))

    def _line(pass_no, status, fired, diverted, outcome):
        emit("      %s pass=%d/%d mute_set_size=%d muted=%s status=%s "
             "fired=%s diverted=%s -> %s"
             % (MUTED_PASS_LOG_PREFIX, pass_no, max_extra_passes + 1,
                len(mute_set), ",".join(sorted(mute_set)) or "-", status,
                sorted(fired or ()) if fired is not None else "unknown",
                diverted, outcome))
        _ev('cycle6_muted_replay_pass', target=_tgt,
            output='pass=%d/%d mute_set_size=%d status=%s diverted=%s'
                   % (pass_no, max_extra_passes + 1, len(mute_set), status,
                      diverted),
            reason='muted=%s fired=%s -> %s'
                   % (",".join(sorted(mute_set)) or "-",
                      sorted(fired or ()) if fired is not None else "unknown",
                      outcome))

    def _done(status, fired, out, diverted, outcome):
        """Single exit: log the pass, record the decision, return the tuple."""
        _line(passes, status, fired, diverted, outcome)
        _ev('cycle6_muted_replay_decided', target=_tgt,
            output='status=%s passes=%d mute_set_size=%d diverted=%s'
                   % (status, passes, len(mute_set), diverted),
            reason=outcome)
        return status, fired, out, diverted, set(mute_set), passes

    while True:
        passes += 1
        try:
            status, fired, out, diverted = replay_fn(set(mute_set), passes)
        except Exception as exc:
            # The pass label says `raised(...)`, but the RETURNED status is
            # "error" exactly as before — behaviour unchanged.
            _line(passes, "raised(%s)" % type(exc).__name__, None, None,
                  "stop: pass raised, UNKNOWN kept")
            _ev('cycle6_muted_replay_decided', target=_tgt,
                output='status=error passes=%d mute_set_size=%d diverted=None'
                       % (passes, len(mute_set)),
                reason='stop: pass raised (%s: %s), UNKNOWN kept'
                       % (type(exc).__name__, exc))
            return "error", None, '', None, set(mute_set), passes
        fired = set(fired) if fired is not None else None

        if status in ("error", "mute_failed"):
            return _done(status, fired, out, diverted,
                         "stop: replay unavailable, UNKNOWN kept")
        if status == "clean":
            return _done(status, fired, out, diverted,
                         "stop: ran to completion (answered)")
        if _muted_target_spoke(target_ids, esc_type, fired, out):
            return _done(status, fired, out, diverted,
                         "stop: target fired (answered)")

        new_shadows = (fired or set()) - mute_set
        if not new_shadows:
            return _done(status, fired, out, diverted,
                         "stop: mute set stopped growing, UNKNOWN kept")
        if passes - 1 >= max_extra_passes:
            return _done(status, fired, out, diverted,
                         "stop: pass bound reached (%d extra), UNKNOWN kept"
                         % max_extra_passes)
        _line(passes, status, fired, diverted,
              "continue: new shadow(s) %s added to the mute set"
              % ",".join(sorted(new_shadows)))
        mute_set |= new_shadows


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