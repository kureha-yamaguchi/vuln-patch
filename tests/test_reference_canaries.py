"""8.2 stage 0 — the two mirror canaries, EXECUTED end to end.

These are the tests that separate a working mechanism from an elaborate way of
agreeing with whatever it is shown. They run a real reference through the real
adapter and the real screen, with only the JVM stubbed.

  CANARY 1  fake patch + CORRECT check  -> the reference must side with the CHECK
  CANARY 2  correct patch + WRONG check -> the reference must side with the PATCH

Both must pass. Each catches a mirror the other cannot: a reference that always
agrees with the patched artefact passes canary 2 and fails canary 1; one that
always agrees with the check passes canary 1 and fails canary 2. Only an
independent reference passes both.

Canary 2 is the Math-65 shape and the reason stage 1 exists at all: a mechanism
that cannot side with a correct patch cannot exonerate one.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from java.relations.reference_impl import (                     # noqa: E402
    held_out_keys, mirror_canary, mirror_canary_correct_patch, pin_check,
    reference_comparison_fact, screen_reference)
from java.relations.reference_run import (                      # noqa: E402
    END_MARKER, build_driver, run_reference)


class _B:
    compiled, classpath, class_name = True, 'cp', 'D'
    harness_path, returncode = '/tmp/x/D.java', 0


class _Builder:
    def build(self, source, buggy_dir, output_subdir='', extra_classpath=()):
        return _B()


def _run_with(monkeypatch, lines):
    """Execute the adapter with a scripted JVM stdout."""
    import java.relations.reference_run as rr

    class _P:
        stdout = '\n'.join(lines) + f'\n{END_MARKER}\n'
        stderr, returncode = '', 0
    monkeypatch.setattr(rr.subprocess, 'run', lambda *a, **k: _P())
    return run_reference(_Builder(), '/d', 'ref', 'drv')


# --- CANARY 1: fake patch, correct check ----------------------------------

def test_canary1_end_to_end_a_faithful_reference_sides_with_the_check(monkeypatch):
    """The reference computes the DOCUMENTED answer; the fake patch does not."""
    ref, why = _run_with(monkeypatch, ['obsA=1.0', 'obsB=2.0', 'obsC=3.0',
                                       'obsD=GOOD'])
    assert ref is not None, why
    buggy = {'obsA': ['1.0'], 'obsB': ['2.0'], 'obsC': ['3.0'], 'obsD': ['BAD']}
    patched = {'obsD': ['BAD']}          # the fake patch left it wrong
    check_expected = {'obsD': ['GOOD']}

    held = held_out_keys(ref, shown_examples=[])
    ok, screen_why = screen_reference(ref, buggy, off_defect_keys=set(held) - {'obsD'})
    assert ok is True, screen_why

    passed, canary_why = mirror_canary(ref, check_expected, patched)
    assert passed is True, canary_why

    fact = reference_comparison_fact('f', ok, screen_why, patched, ref,
                                     screened_count=3)
    assert 'DIFFERENT value' in fact


# --- CANARY 2: correct patch, wrong check (the Math-65 shape) -------------

def test_canary2_end_to_end_a_faithful_reference_sides_with_the_patch(monkeypatch):
    """The check demands the WRONG formula; the correct patch and an
    independent reference agree against it. This is what exoneration is."""
    ref, why = _run_with(monkeypatch, ['obsA=1.0', 'obsB=2.0', 'obsC=3.0',
                                       'obsD=3.3'])
    assert ref is not None, why
    buggy = {'obsA': ['1.0'], 'obsB': ['2.0'], 'obsC': ['3.0'], 'obsD': ['9.9']}
    patched = {'obsD': ['3.3']}          # the correct patch
    check_expected = {'obsD': ['9.9']}   # the wrong check

    held = held_out_keys(ref, shown_examples=[])
    ok, screen_why = screen_reference(ref, buggy, off_defect_keys=set(held) - {'obsD'})
    assert ok is True, screen_why

    passed, canary_why = mirror_canary_correct_patch(ref, patched, check_expected)
    assert passed is True, canary_why

    fact = reference_comparison_fact('f', ok, screen_why, patched, ref,
                                     screened_count=3)
    assert 'SAME value' in fact, 'the agreement side is what exonerates'


# --- the mirrors both canaries exist to catch -----------------------------

def test_a_patch_mirror_passes_canary2_and_FAILS_canary1():
    """A reference that just echoes the patched build."""
    ref = {'obsD': ['BAD']}
    assert mirror_canary_correct_patch(ref, {'obsD': ['BAD']},
                                       {'obsD': ['GOOD']})[0] is True
    assert mirror_canary(ref, {'obsD': ['GOOD']}, {'obsD': ['BAD']})[0] is False


def test_a_check_mirror_passes_canary1_and_FAILS_canary2():
    """A reference that just echoes whatever the check demands."""
    ref = {'obsD': ['9.9']}
    assert mirror_canary(ref, {'obsD': ['9.9']}, {'obsD': ['3.3']})[0] is True
    assert mirror_canary_correct_patch(ref, {'obsD': ['3.3']},
                                       {'obsD': ['9.9']})[0] is False


# --- a bug-copying reference is caught by validator 3, not the screen ------

def test_a_bug_copying_reference_passes_the_SCREEN_and_is_caught_by_the_PIN(monkeypatch):
    """THE STRUCTURAL BLIND SPOT, demonstrated. A reference that copied the
    buggy implementation agrees with the buggy build EVERYWHERE -- including at
    the defect -- so the off-defect screen admits it. Only the failing test's
    pinned answer catches it."""
    ref, _ = _run_with(monkeypatch, ['obsA=1.0', 'obsB=2.0', 'obsC=3.0',
                                     'obsD=9.9'])
    buggy = {'obsA': ['1.0'], 'obsB': ['2.0'], 'obsC': ['3.0'], 'obsD': ['9.9']}
    held = held_out_keys(ref, shown_examples=[])

    ok, _why = screen_reference(ref, buggy, off_defect_keys=set(held) - {'obsD'})
    assert ok is True, 'the screen cannot see bug-copying — that is the point'

    pinned_ok, pin_why = pin_check(ref, {'obsD': ['3.3']}, ['obsD'])
    assert pinned_ok is False and 'copied the defect' in pin_why
