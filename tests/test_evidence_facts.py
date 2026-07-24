"""Spec B + Spec C: regression fence for the pure evidence-fact functions.

Covers:
  * classify_differential_replay  (Spec B: no "clean" when it errored/never
    arrived; harness-alarm shadowing must not read as exculpatory-for-buggy).
  * semantic_buggy_replay_note     (Spec C: the shadowed relation-replay
    branch must not extrapolate a screening confirmation to this input).
  * fired_value_vs_trusted         (Spec D numeric-comparison helper, exercised
    on the real Math-68 firing pair).

Assertions are on VERDICTS and key SUBSTRINGS, not full note text, since the
wording may be polished. Every wrong-fact fixture asserts the NEW behaviour, so
each test FAILS against the shipped (pre-cycle) inline logic -- a true fence.

The module under test may not exist yet while a parallel agent implements it;
in that case these tests fail with a collection/import error only, which is the
expected transient state per the cycle plan.
"""
import json
import os

import pytest

from java.relations import evidence_facts

_FIX = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures")


def _load(name):
    with open(os.path.join(_FIX, name + ".json"), "r", encoding="utf-8") as fh:
        return json.load(fh)


_DIFF = _load("differential_replay")
_SEM = _load("semantic_replay")
_LIFT = _load("lift_note")


# --------------------------------------------------------------------------
# Spec B -- classify_differential_replay
# --------------------------------------------------------------------------

def _classify(fx):
    return evidence_facts.classify_differential_replay(
        fx["patched_sig"], fx["buggy_status"], fx["buggy_sig"])


def test_closure70_harness_alarm_is_shadowed_not_introduced():
    """Real armA Closure-70 pair: patched escaped IllegalStateException vs a
    buggy-side harness-own FuzzerSecurityIssue alarm. The buggy run died at its
    own check BEFORE reaching the patched crash site -> SHADOWED, uninformative;
    must NOT be read as "the patch introduced the crash"."""
    fx = _DIFF["closure70_shadowed"]
    verdict, note = _classify(fx)

    assert verdict == "SHADOWED"
    # The note must explain the buggy-side check fired before the crash site,
    # and must NOT assert the patch introduced the crash.
    low = note.lower()
    assert "shadow" in low
    assert "before" in low  # a check fired before execution reached the site
    assert fx["old_wording_must_not_appear"] not in low
    assert "introduced by the patch" not in low


def test_error_status_abstains():
    """An infrastructure error must never manufacture evidence against the
    patch: status 'error' -> ABSTAIN (today it was conflated with clean ->
    INTRODUCED)."""
    fx = _DIFF["error_status_abstain"]
    verdict, note = _classify(fx)

    assert verdict == "ABSTAIN"
    low = note.lower()
    # It must not AFFIRM introduction (the word may appear only under negation).
    assert "the crash is introduced by the patch" not in low
    assert "introduced by the patch" not in low
    # It should flag the replay as unavailable / abstained / no attribution fact.
    assert "unavailable" in low or "abstain" in low or "no attribution" in low


def test_identical_signature_is_preexisting():
    fx = _DIFF["identical_sig_preexisting"]
    verdict, _note = _classify(fx)
    assert verdict == "PREEXISTING"


def test_buggy_clean_is_introduced():
    """'clean' now genuinely means ran-to-completion, so INTRODUCED is
    trustworthy here."""
    fx = _DIFF["buggy_clean_introduced"]
    verdict, note = _classify(fx)
    assert verdict == "INTRODUCED"
    assert "introduced" in note.lower()


def test_different_nonalarm_signature_is_introduced():
    fx = _DIFF["different_nonalarm_introduced"]
    verdict, _note = _classify(fx)
    assert verdict == "INTRODUCED"


# --------------------------------------------------------------------------
# Spec C -- semantic_buggy_replay_note (shadowed relation-replay branch)
# --------------------------------------------------------------------------

def _sem_note(fx):
    # Spec I (cycle-2b): the same-check branch now takes a value verdict. The
    # fixtures thread it (default "unknown"); a fixture that locks the
    # identical-on-both-builds wording sets value_verdict="identical".
    return evidence_facts.semantic_buggy_replay_note(
        set(fx["fired_ids"]),
        fx["breplay_status"],
        (set(fx["breplay_ids"]) if fx["breplay_ids"] is not None else None),
        set(fx["bt_all"]),
        set(fx["bt_defect"]),
        fx["esc_type"],
        fx.get("idline", ""),
        value_verdict=fx.get("value_verdict", "unknown"),
        buggy_msg_excerpt=fx.get("buggy_msg_excerpt"),
        patched_msg_excerpt=fx.get("patched_msg_excerpt"),
    )


@pytest.mark.parametrize("key", ["math30_shadowed_branch", "math65_shadowed_branch"])
def test_shadowed_branch_says_unknown_not_already_establishes(key):
    """Real armA Math-30 / Math-65 firings: a DIFFERENT check fired first on
    buggy. Whether THIS check fires there is UNKNOWN. The note must NOT tell the
    judge a screening DIRECTION-CONFIRMED result 'already establishes' the buggy
    build violates this check (that extrapolation drove two wrongful
    convictions)."""
    fx = _SEM[key]
    note = _sem_note(fx)

    assert note is not None
    low = note.lower()
    assert fx["must_contain_ci"] in low            # "unknown"
    assert fx["must_not_contain"] not in low       # "already establishes"
    # It still names the shadowing check id so the judge has the mechanical fact.
    assert fx["breplay_ids"][0] in note


def test_same_check_branch_keeps_current_wording():
    """Regression: fired_ids & breplay_ids non-empty -> same-check branch keeps
    its current wording verbatim (run.py ~2170-2186)."""
    fx = _SEM["same_check_regression"]
    note = _sem_note(fx)

    assert note is not None
    for needle in fx["must_contain"]:
        assert needle in note


def test_error_status_keeps_unavailable_wording():
    """Regression: an errored/absent replay keeps the current 'unavailable'
    wording and does not manufacture an attribution fact."""
    fx = _SEM["error_status_regression"]
    note = _sem_note(fx)

    assert note is not None
    low = note.lower()
    assert fx["must_contain_ci"] in low            # "unavailable"
    assert "already establishes" not in low


# --------------------------------------------------------------------------
# Spec D -- fired_value_vs_trusted (numeric comparison helper)
# --------------------------------------------------------------------------

def test_real_math68_pair_differs():
    """Real armC Math-68 firing: actual=0.25781765926522887 vs trusted MINPACK
    literal 0.257829976764542 (~1.2e-5 apart) -> differs. A divergent-setup
    value far from the test's own is the definition of a generalization catch,
    not a replay of the test."""
    fx = _LIFT["math68_armC_differs"]
    verdict = evidence_facts.fired_value_vs_trusted(
        fx["fired_msg"], fx["trusted_values"])
    assert verdict == "differs"


def test_real_math68_armA_pair_differs():
    fx = _LIFT["math68_armA_undetected"]
    verdict = evidence_facts.fired_value_vs_trusted(
        fx["fired_msg"], fx["trusted_values"])
    assert verdict == "differs"


def test_equal_within_floor_matches():
    """Synthetic: actual within 1e-12 of a trusted literal -> matches."""
    fx = _LIFT["synthetic_matches"]
    verdict = evidence_facts.fired_value_vs_trusted(
        fx["fired_msg"], fx["trusted_values"])
    assert verdict == "matches"


def test_nonnumeric_firing_is_unknown():
    """Synthetic: exception-only message with no numerics -> unknown."""
    fx = _LIFT["synthetic_unknown_nonnumeric"]
    verdict = evidence_facts.fired_value_vs_trusted(
        fx["fired_msg"], fx["trusted_values"])
    assert verdict == "unknown"
