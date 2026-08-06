"""8.2 stage 0 — the compile-and-run adapter. Every failure path fails CLOSED.

A reference we could not run is a reference with NO STANDING. The dangerous
reading is "no difference found", which is precisely what P4.2 measured going
wrong: half the certifier's no-difference answers were wrong, and every one came
from a probe that never actually looked.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from java.relations.reference_run import (                      # noqa: E402
    END_MARKER, build_driver, run_reference)


class _Build:
    def __init__(self, compiled=True, classpath='cp', class_name='D',
                 harness_path='/tmp/x/D.java'):
        self.compiled = compiled
        self.classpath = classpath
        self.class_name = class_name
        self.harness_path = harness_path
        self.returncode = 0


class _Builder:
    """Scripted builder: a list of BuildResults, returned in order."""

    def __init__(self, *results):
        self._r = list(results)
        self.calls = 0

    def build(self, source, buggy_dir, output_subdir=''):
        self.calls += 1
        return self._r.pop(0)


# --- the driver: OUR code chooses the inputs ------------------------------

def test_the_driver_calls_every_input_the_CALLER_chose():
    """P4.2: the model writes the reference, our code decides what is
    compared. A driver that asked the model to print things would inherit the
    bug that made half the certifier's answers wrong."""
    src = build_driver('ReferenceImpl', 'chiSquare', ['1, 2', '3, 4'])
    assert 'ReferenceImpl.chiSquare(1, 2)' in src
    assert 'ReferenceImpl.chiSquare(3, 4)' in src
    assert 'obs0=' in src and 'obs1=' in src
    assert END_MARKER in src


def test_the_driver_records_a_throw_as_an_observable():
    """An exception IS an observable — a reference that throws where the patch
    returns is a real difference, not a missing datum."""
    src = build_driver('ReferenceImpl', 'f', ['bad'])
    assert 'catch (Throwable t)' in src and 'EX:' in src


def test_the_driver_can_be_packaged():
    assert 'package org.x;' in build_driver('R', 'f', ['1'], package='org.x')


# --- every failure path returns None ---------------------------------------

def test_reference_compile_failure_fails_closed():
    obs, why = run_reference(_Builder(_Build(compiled=False)), '/d', 'src', 'drv')
    assert obs is None and 'did not compile' in why


def test_driver_compile_failure_fails_closed():
    obs, why = run_reference(
        _Builder(_Build(), _Build(compiled=False)), '/d', 'src', 'drv')
    assert obs is None and 'driver did not compile' in why


def test_a_raising_builder_fails_closed():
    class _Boom:
        def build(self, *a, **k):
            raise RuntimeError('javac exploded')
    obs, why = run_reference(_Boom(), '/d', 'src', 'drv')
    assert obs is None and 'raised' in why


def test_a_run_without_the_end_marker_fails_closed(monkeypatch):
    """Partial output must never be read as a completed comparison."""
    import java.relations.reference_run as rr

    class _P:
        stdout, stderr, returncode = 'obs0=1\n', '', 1
    monkeypatch.setattr(rr.subprocess, 'run', lambda *a, **k: _P())
    obs, why = run_reference(_Builder(_Build(), _Build()), '/d', 's', 'd')
    assert obs is None and 'did not complete' in why


def test_a_timeout_fails_closed(monkeypatch):
    import java.relations.reference_run as rr

    def _boom(*a, **k):
        raise rr.subprocess.TimeoutExpired('java', 60)
    monkeypatch.setattr(rr.subprocess, 'run', _boom)
    obs, why = run_reference(_Builder(_Build(), _Build()), '/d', 's', 'd')
    assert obs is None and 'timed out' in why


def test_completed_but_unparseable_output_fails_closed(monkeypatch):
    import java.relations.reference_run as rr

    class _P:
        stdout, stderr, returncode = f'nothing useful\n{END_MARKER}\n', '', 0
    monkeypatch.setattr(rr.subprocess, 'run', lambda *a, **k: _P())
    obs, why = run_reference(_Builder(_Build(), _Build()), '/d', 's', 'd')
    assert obs is None and 'no parseable observables' in why


def test_a_good_run_returns_the_observables(monkeypatch):
    import java.relations.reference_run as rr

    class _P:
        stdout = f'obs0=3.3\nobs1=EX:IllegalArgumentException\n{END_MARKER}\n'
        stderr, returncode = '', 0
    monkeypatch.setattr(rr.subprocess, 'run', lambda *a, **k: _P())
    obs, why = run_reference(_Builder(_Build(), _Build()), '/d', 's', 'd')
    assert obs == {'obs0': ['3.3'], 'obs1': ['EX:IllegalArgumentException']}
    assert '2 observable' in why


def test_output_after_the_marker_is_ignored(monkeypatch):
    """JVM shutdown noise must not become an observable."""
    import java.relations.reference_run as rr

    class _P:
        stdout = f'obs0=1\n{END_MARKER}\nPicked up JAVA_TOOL_OPTIONS=x\n'
        stderr, returncode = '', 0
    monkeypatch.setattr(rr.subprocess, 'run', lambda *a, **k: _P())
    obs, _why = run_reference(_Builder(_Build(), _Build()), '/d', 's', 'd')
    assert obs == {'obs0': ['1']}
