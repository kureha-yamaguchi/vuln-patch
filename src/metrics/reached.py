"""F(H) — the set of methods a harness set actually ran.

The chain has three links, and each one is somebody else's tool:

  1. Jazzer writes a JaCoCo ``.exec`` file when it exits, if
     ``--coverage_dump`` is set.
  2. The JaCoCo command line turns that binary dump into ``jacoco.xml``.
  3. fuzz-introspector reads the XML and decodes each method's bytecode
     descriptor into the name ``[pkg.Class].method(argTypes)``.

We enter fuzz-introspector at step 3 and no higher. That loader is the one
piece of it that does work we would otherwise repeat, because a hand-written
descriptor decoder is exactly where a silent mismatch would come from.
Everything above it — call trees, fuzz blockers, HTML overlays — needs a
fuzzer profile per harness, and answers a different question.

Two things this module refuses to guess.

  * A missing ``.exec`` file is an ERROR, never zero coverage. Jazzer writes
    the dump from a JVM shutdown hook, so a process that dies hard writes
    nothing at all. From the outside that looks the same as a harness that
    ran and reached nothing.
  * `load_jvm_coverage` walks a DIRECTORY tree for a file whose name starts
    with ``jacoco.xml`` and takes the first one it finds. So each report
    gets its own directory, with one report in it.

`load_jvm_coverage` also skips a method when the class carries no debug
information, or when it cannot find the declaration line in the source-file
map. It skips it without a warning. That is why `metrics.rcc.trigger_gate`
exists.

THE PROBE LIMITATION, and it matters most for exactly our population.
JaCoCo records a line as covered when a PROBE on it executes, and it places
a method's probe after the method's exit. A method whose body is a single
`return other(x);` therefore reads as MISSED when `other` throws, because
the probe after the call never runs. Math-70 is the recorded case: the stack
trace names `BisectionSolver.solve` at line 72, and JaCoCo reports line 72
as never covered.

Every bug in the crashing split ends in a throw, so this under-reports the
crashing path — the one path the metric is about. `reached_from_stack`
repairs it. A stack frame is proof that the method was entered, so the two
sources union: probes give the lower bound, frames add what the probes
provably missed. Callers keep the two apart in their records, so a
frame-only hit is never mistaken for a probe hit.

MEASUREMENT ONLY.
"""
import os
import re
import subprocess
import urllib.request
from typing import Iterable, Optional, Set, Tuple

import config
from metrics.keys import MethodKey, key_from_mangled

REPORT_NAME = 'jacoco.xml'


class CoverageUnavailable(RuntimeError):
    """The run produced no readable coverage. Never read this as zero."""


def ensure_cli_jar(jar_path: Optional[str] = None) -> str:
    """Download the JaCoCo command line jar if it is not on disk."""
    jar_path = jar_path or config.JACOCO_CLI_JAR
    if os.path.isfile(jar_path):
        return jar_path
    os.makedirs(os.path.dirname(jar_path), exist_ok=True)
    print(f"Downloading JaCoCo CLI from {config.JACOCO_CLI_URL}")
    urllib.request.urlretrieve(config.JACOCO_CLI_URL, jar_path)
    return jar_path


def ensure_agent_jar(jar_path: Optional[str] = None) -> str:
    """Download the JaCoCo agent jar if it is not on disk."""
    jar_path = jar_path or config.JACOCO_AGENT_JAR
    if os.path.isfile(jar_path):
        return jar_path
    os.makedirs(os.path.dirname(jar_path), exist_ok=True)
    print(f"Downloading JaCoCo agent from {config.JACOCO_AGENT_URL}")
    urllib.request.urlretrieve(config.JACOCO_AGENT_URL, jar_path)
    return jar_path


def exec_to_xml(exec_paths: Iterable[str], classfiles: str, out_dir: str,
                sourcefiles: Optional[str] = None) -> str:
    """Turn one or more Jazzer ``.exec`` dumps into one ``jacoco.xml``.

    Several dumps in, one report out: that is how the per-harness sets are
    unioned into the set for the whole harness set. Returns the report path.
    """
    paths = [p for p in exec_paths]
    missing = [p for p in paths if not os.path.isfile(p)]
    if not paths or missing:
        raise CoverageUnavailable(
            f"no readable .exec dump: {missing or 'none given'}. "
            "Jazzer writes it from a shutdown hook, so a hard kill leaves "
            "none. This is an infrastructure error, not zero coverage.")

    os.makedirs(out_dir, exist_ok=True)
    report = os.path.join(out_dir, REPORT_NAME)
    cmd = ['java', '-jar', ensure_cli_jar(), 'report'] + paths + [
        '--classfiles', classfiles, '--xml', report,
    ]
    if sourcefiles:
        cmd += ['--sourcefiles', sourcefiles]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0 or not os.path.isfile(report):
        raise CoverageUnavailable(
            f"jacococli report failed ({result.returncode}): "
            f"{result.stderr.strip() or result.stdout.strip()}")
    return report


def reached_from_report(report_path: str) -> Set[MethodKey]:
    """Every method the report shows as executed at least once."""
    if os.path.basename(report_path) != REPORT_NAME:
        raise CoverageUnavailable(
            f"the report must be named {REPORT_NAME} and sit alone in its "
            f"own directory; got {report_path}")
    if not os.path.isfile(report_path):
        raise CoverageUnavailable(f"no report at {report_path}")

    from fuzz_introspector import code_coverage

    profile = code_coverage.load_jvm_coverage(os.path.dirname(report_path))
    if not profile.covmap:
        raise CoverageUnavailable(
            f"{report_path} decoded to no methods at all. Check that the "
            "classes were compiled with debug information, and that "
            "--instrumentation_includes covered the project package.")

    reached: Set[MethodKey] = set()
    for mangled, lines in profile.covmap.items():
        if not any(hits > 0 for _, hits in lines):
            continue
        key = key_from_mangled(mangled)
        if key is not None:
            reached.add(key)
    return reached


def reached_from_reports(report_paths: Iterable[str]) -> Set[MethodKey]:
    """The union of several reports — F(H) for a whole harness set.

    Use this when each harness produced its own report. When the dumps are
    merged first with `exec_to_xml`, one report already holds the union."""
    paths = list(report_paths)
    if not paths:
        raise CoverageUnavailable("no reports given")
    union: Set[MethodKey] = set()
    for path in paths:
        union |= reached_from_report(path)
    return union


# One JVM stack frame: `at pkg.Class.method(File.java:123)`. Written here
# rather than reused from `fuzz_runner.covered_functions`, because that one
# drops the line number and the line number is what separates two overloads.
_FRAME_RE = re.compile(
    r'^\s*at\s+([\w$.]+)\.([\w$<>]+)\([\w$]+\.java:(\d+)\)', re.MULTILINE)


def stack_frames(text: str) -> Set[Tuple[str, str, int]]:
    """Every project stack frame in some output, as (class, method, line)."""
    frames = set()
    for cls, method, line in _FRAME_RE.findall(text or ''):
        if cls.startswith(('java.', 'javax.', 'jdk.', 'sun.', 'junit.',
                           'org.junit.', 'com.code_intelligence.jazzer')):
            continue
        frames.add((cls.replace('$', '.'), method, int(line)))
    return frames


def reached_from_stack(report_path: str, text: str) -> Set[MethodKey]:
    """Methods named by a stack trace, resolved against the report.

    A frame gives a class, a method name and a LINE, but no parameter types.
    The report gives each method the lines it owns. Matching on the line is
    therefore exact: it tells two overloads of one name apart, which a
    name-only match could not.
    """
    from fuzz_introspector import code_coverage

    if os.path.basename(report_path) != REPORT_NAME:
        raise CoverageUnavailable(
            f"the report must be named {REPORT_NAME}; got {report_path}")
    profile = code_coverage.load_jvm_coverage(os.path.dirname(report_path))

    frames = stack_frames(text)
    if not frames:
        return set()

    found: Set[MethodKey] = set()
    for mangled, lines in profile.covmap.items():
        key = key_from_mangled(mangled)
        if key is None:
            continue
        owned = {line for line, _ in lines}
        for cls, method, line in frames:
            if cls == key.class_name and method == key.method_name \
                    and line in owned:
                found.add(key)
                break
    return found
