"""Collect the coverage artifacts one bug's RCC number needs.

Everything here runs against the BUGGY build. The root cause lives there,
and so do the sibling bugs.

Two jobs:

  1. `trigger_coverage` runs the bug's OWN triggering test under the JaCoCo
     agent. `metrics.rcc.trigger_gate` reads the result. The test is run
     through `defects4j test -t`, so exactly the triggering method runs and
     not its whole class. A whole class would cover more, which would make
     the gate easier to pass and therefore weaker.

     The agent reaches the forked test JVM through `JAVA_TOOL_OPTIONS`,
     which every JVM honours. Ant's own JVM picks it up too, but
     `--classfiles` limits the report to the project's classes, so that
     costs nothing.

     It also returns the test's failure stack trace, which Defects4J writes
     to `failing_tests`. The trace is not a convenience: JaCoCo's probe sits
     after a method's exit, so a method that throws through it reads as
     missed. See the probe limitation in `metrics.reached`.

  2. `harness_coverage` runs an accepted harness set and returns F(H). See
     that function for the three rules a measurement run must follow.

MEASUREMENT ONLY.
"""
import os
import shutil
import subprocess
from dataclasses import dataclass
from typing import List, Optional

import config
from metrics import reached

# `defects4j test -t` on a bug whose test fails is normal, so a non-zero
# exit is not by itself an error. A missing dump is.
_EXPORT_TIMEOUT = 120
_TEST_TIMEOUT = 900
_BUILD_TIMEOUT = 1800


class CollectionError(RuntimeError):
    """The bug could not be built or run. Not a result about coverage."""


def d4j_bin() -> str:
    """The `defects4j` command, from PATH or from the checkout in config."""
    found = shutil.which('defects4j')
    if found:
        return found
    local = os.path.join(config.D4J_HOME, 'framework', 'bin', 'defects4j')
    if os.path.isfile(local):
        return local
    raise CollectionError('defects4j is not on PATH and not under D4J_HOME')


def d4j_export(buggy_dir: str, prop: str) -> str:
    """One `defects4j export -p <prop>` value, stripped."""
    result = subprocess.run([d4j_bin(), 'export', '-p', prop],
                            cwd=buggy_dir, capture_output=True, text=True,
                            timeout=_EXPORT_TIMEOUT)
    if result.returncode != 0:
        raise CollectionError(f'export {prop} failed: {result.stderr.strip()}')
    return result.stdout.strip()


def ensure_buggy_build(project: str, bug_id) -> str:
    """Check out the buggy version and compile it. Returns its directory.

    A checkout already at the right version is reused. The version is
    verified from `.defects4j.config`, because a stale fixed-version
    checkout in the buggy path would otherwise be trusted silently.
    """
    os.makedirs(config.D4J_CHECKOUT_ROOT, exist_ok=True)
    buggy_dir = os.path.join(config.D4J_CHECKOUT_ROOT,
                             f'{project}_{bug_id}_buggy')
    config_file = os.path.join(buggy_dir, '.defects4j.config')
    want = f'vid={bug_id}b'
    cached = False
    if os.path.isfile(config_file):
        with open(config_file) as handle:
            cached = want in handle.read()
    if not cached:
        if os.path.isdir(buggy_dir):
            shutil.rmtree(buggy_dir)
        result = subprocess.run(
            [d4j_bin(), 'checkout', '-p', project, '-v', f'{bug_id}b',
             '-w', buggy_dir],
            capture_output=True, text=True, timeout=_BUILD_TIMEOUT)
        if result.returncode != 0:
            raise CollectionError(
                f'{project}-{bug_id} checkout failed: {result.stderr.strip()}')

    result = subprocess.run([d4j_bin(), 'compile'], cwd=buggy_dir,
                            capture_output=True, text=True,
                            timeout=_BUILD_TIMEOUT)
    if result.returncode != 0:
        raise CollectionError(
            f'{project}-{bug_id} compile failed: {result.stderr.strip()}')
    return buggy_dir


def classes_dir(buggy_dir: str) -> str:
    """Absolute path of the compiled project classes."""
    return os.path.join(buggy_dir, d4j_export(buggy_dir, 'dir.bin.classes'))


def source_dir(buggy_dir: str) -> str:
    """Absolute path of the project's own sources."""
    return os.path.join(buggy_dir, d4j_export(buggy_dir, 'dir.src.classes'))


def trigger_tests(buggy_dir: str) -> List[str]:
    """The bug's triggering tests, as `Class::method` strings."""
    return d4j_export(buggy_dir, 'tests.trigger').split()


def package_prefix(buggy_dir: str, region_methods) -> str:
    """A Jazzer `--instrumentation_includes` glob for this project.

    Taken from the package of the first method in R-hat, cut to three
    components (`org.apache.commons`), which is the same rule
    `call_graph.project_prefix` uses to tell project code from the JDK.
    """
    if not region_methods:
        return ''
    parts = region_methods[0].class_name.split('.')[:-1]
    return '.'.join(parts[:3]) + '.**' if parts else ''


@dataclass
class TriggerRun:
    """What one triggering-test run leaves behind."""
    report: str      # path to jacoco.xml
    trace: str       # the tests' failure stack traces, concatenated


def trigger_coverage(buggy_dir: str, out_dir: str,
                     tests: Optional[List[str]] = None) -> TriggerRun:
    """Run the bug's triggering tests under JaCoCo.

    The tests FAIL — that is what makes them triggering tests — so a
    non-zero exit is expected and is not an error here."""
    tests = tests if tests is not None else trigger_tests(buggy_dir)
    if not tests:
        raise CollectionError('the bug lists no triggering test')

    # ABSOLUTE, because `defects4j test` runs with cwd=buggy_dir and the
    # agent resolves `destfile` against the JVM's own working directory.
    out_dir = os.path.abspath(out_dir)
    os.makedirs(out_dir, exist_ok=True)
    dump = os.path.join(out_dir, 'trigger.exec')
    if os.path.isfile(dump):
        os.unlink(dump)          # never read a previous run's dump

    env = dict(os.environ)
    env['JAVA_TOOL_OPTIONS'] = (
        f'-javaagent:{reached.ensure_agent_jar()}='
        f'destfile={dump},append=true')
    failing = os.path.join(buggy_dir, 'failing_tests')
    traces = []
    for test in tests:
        if os.path.isfile(failing):
            os.unlink(failing)   # `defects4j test` rewrites it per run
        subprocess.run([d4j_bin(), 'test', '-t', test], cwd=buggy_dir,
                       capture_output=True, text=True, env=env,
                       timeout=_TEST_TIMEOUT)
        if os.path.isfile(failing):
            with open(failing, encoding='utf-8', errors='replace') as handle:
                traces.append(handle.read())

    report = reached.exec_to_xml([dump], classfiles=classes_dir(buggy_dir),
                                 out_dir=out_dir)
    trace = '\n'.join(traces)
    with open(os.path.join(out_dir, 'failing_tests.txt'), 'w') as handle:
        handle.write(trace)
    return TriggerRun(report=report, trace=trace)


@dataclass
class HarnessRun:
    """What one measurement pass over a harness set leaves behind."""
    report: str                 # path to jacoco.xml, all harnesses merged
    trace: str                  # every harness's Jazzer output, concatenated
    per_harness: List[dict]     # one entry per accepted harness


def harness_coverage(buggy_dir: str, accepted: List[dict], out_dir: str,
                     includes: str = '', runs: int = 20000,
                     keep_going: int = 1000,
                     timeout_seconds: int = 300) -> HarnessRun:
    """Run an accepted harness set for coverage, and return F(H)'s report.

    Three rules, and each one exists for a reason.

      1. `--keep_going` is large. An accepted harness crashes the buggy
         build by design — that is the acceptance test — and a crash would
         otherwise end the run on its first input. `oracle_mute` does not
         help here: on a crashing bug the throwable is the LIBRARY's, not
         the harness's own alarm, so muting the alarm changes nothing.
      2. The budget is `-runs=N`, not wall-clock. The number must not
         depend on machine load. `timeout_seconds` is only a safety net,
         and it is set well above the run budget.
      3. Every harness writes its own dump, and `exec_to_xml` merges them.
         The union is F(H) for the whole set; each dump on its own is F for
         one harness.

    Jazzer writes the dump from a shutdown hook, and it does run after a
    finding — measured, not assumed. A killed JVM still writes nothing, so
    a missing dump stays an error.
    """
    from java.execution.fuzz_runner import run_jazzer

    out_dir = os.path.abspath(out_dir)
    os.makedirs(out_dir, exist_ok=True)
    dumps, traces, per_harness = [], [], []

    for index, entry in enumerate(accepted):
        harness_path = entry.get('harness_path', '')
        label = entry.get('attempt_label') or f'harness_{index}'
        dump = os.path.join(out_dir, f'{label}.exec')
        outcome = run_jazzer(
            jazzer_standalone_jar=config.JAZZER_STANDALONE_JAR,
            target_class=entry.get('class_name', ''),
            harness_dir=os.path.dirname(harness_path),
            project_cp=entry.get('classpath', ''),
            timeout_seconds=timeout_seconds,
            keep_going=keep_going,
            extra_libfuzzer_args=[f'-runs={runs}'],
            coverage_dump=dump,
            instrumentation_includes=includes,
        )
        text = f'{outcome.stdout}\n{outcome.stderr}'
        traces.append(text)
        wrote = os.path.isfile(dump)
        if wrote:
            dumps.append(dump)
        per_harness.append({'label': label, 'class_name':
                            entry.get('class_name', ''),
                            'dump_written': wrote,
                            'timed_out': outcome.timed_out,
                            'triggered': outcome.triggered})

    if not dumps:
        raise CollectionError(
            'no harness produced a coverage dump; F(H) is unknown, '
            'which is not the same as empty')

    report = reached.exec_to_xml(dumps, classfiles=classes_dir(buggy_dir),
                                out_dir=out_dir)
    trace = '\n'.join(traces)
    with open(os.path.join(out_dir, 'jazzer_output.txt'), 'w') as handle:
        handle.write(trace)
    return HarnessRun(report=report, trace=trace, per_harness=per_harness)
