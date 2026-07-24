"""Spec D (G5): the trigger-test-lift note must compare values before it may
say "dismiss".

`trigger_lift_note(lifted_names, generic_lift, value_verdict)` words the note by
the mechanical value comparison, never on name evidence alone:
  * differs -> keep-flavoured note (candidate generalization catch beyond the
    test's inputs); NO "must be dismissed".
  * matches -> dismissal-leaning wording is allowed.
  * unknown -> neutral fact only; NO "must be dismissed".
  * neither lifted_names nor generic_lift -> no note (lift provenance
    undetected).

Assertions are on VERDICT-driven substrings, not full wording.
"""
import json
import os

from java.relations import evidence_facts

_FIX = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures")


def _load(name):
    with open(os.path.join(_FIX, name + ".json"), "r", encoding="utf-8") as fh:
        return json.load(fh)


_LIFT = _load("lift_note")

_DISMISS = "must be dismissed"


def _note(fx):
    return evidence_facts.trigger_lift_note(
        fx["lifted_names"], fx["generic_lift"], fx["expected_value_verdict"])


def test_math68_armC_differs_is_keep_flavoured():
    """Real armC Math-68: generic-lift oracle whose fired value differs from
    every trusted literal by >floor. The note must NOT dismiss and must frame it
    as a generalization catch beyond the test's own inputs."""
    fx = _LIFT["math68_armC_differs"]
    note = _note(fx)

    assert note is not None
    low = note.lower()
    assert _DISMISS not in low
    # It flags this as reaching beyond the test's inputs / a generalization.
    assert ("generaliz" in low) or ("beyond the test" in low)
    # Test-passage does not exonerate a diverging value.
    assert "test" in low


def test_math68_armA_lift_undetected_yields_no_note():
    """Real armA Math-68 oracle 'jennrich-seed-p1' does not match the
    lift|seed[-_]?test detector, so lift provenance is undetected -> no note at
    all today. (Widening detection is a separate change; when detected with a
    'differs' verdict it must be keep-flavoured, exercised above.)"""
    fx = _LIFT["math68_armA_undetected"]
    note = _note(fx)
    assert note is None


def test_matches_may_dismiss():
    """Synthetic: value matches a trusted literal within floor -> the observed
    value IS the test's own scenario; dismissal-leaning wording is allowed. We
    only require a well-formed note that references the lift."""
    fx = _LIFT["synthetic_matches"]
    note = _note(fx)
    assert note is not None
    assert "lift" in note.lower()


def test_unknown_is_neutral_no_dismissal():
    """Synthetic: non-numeric firing -> unknown -> neutral fact only, no
    'must be dismissed'."""
    fx = _LIFT["synthetic_unknown_nonnumeric"]
    note = _note(fx)
    assert note is not None
    assert _DISMISS not in note.lower()


def test_no_dismissal_on_name_evidence_alone():
    """Detector fired (generic_lift True) but the value comparison is 'differs':
    name-based lift detection must NOT license dismissal wording."""
    note = evidence_facts.trigger_lift_note([], True, "differs")
    assert note is not None
    assert _DISMISS not in note.lower()
