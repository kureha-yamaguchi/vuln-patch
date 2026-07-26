"""Spec H: tests for the pure fire-rate evidence-fact builder.

Pinned API (implemented by a parallel agent; import failure while unlanded is
the expected transient state):

    java.relations.evidence_facts.fire_rate_fact(
        buggy_checked, buggy_violated, patched_checked, patched_violated,
        screen_outcome_reason) -> str|None

Real counts (provenance in comments) come from archived armA traces:
  * Math-65  patched 9674/20000 (48.4%) fuzz replay, buggy screen 14832/20000
      runs-archive/runs/armA_off_20260721_122647/10_patch1-Math-65-CapGen_c/trace.md
      lines 8367 / 8449 (patched fuzz + composed replay note); buggy screen 1330.
  * Chart-19 buggy 20000/20000 screen (fires on essentially every input)
      runs-archive/runs/armA_off_20260721_122647/02_patch1-Chart-19-Arja-plausible_o/trace.md
      lines 1438 (buggy screen) / 4990 / 5047 (patched fuzz 20000/20000).
  * Low-rate illustrative example 285/20000 (~1.4%) -- below MAX_FIRE_RATIO (0.20).

Assertions are on the "[fire-rate fact]" marker, the percentage, and the
interpretation wording family, not exact phrasing.
"""
import pytest

from java.relations import evidence_facts


# --------------------------------------------------------------------------
# Math-65: patched-side rate ~48% -> indictment wording
# --------------------------------------------------------------------------

def test_math65_patched_high_rate_indicts_check():
    note = evidence_facts.fire_rate_fact(
        buggy_checked=20000,
        buggy_violated=14832,
        patched_checked=20000,
        patched_violated=9674,
        screen_outcome_reason=None,
    )
    assert note is not None
    assert "[fire-rate fact]" in note
    low = note.lower()
    # 9674 / 20000 = 48.4%
    assert "48" in note
    assert "%" in note
    # Cycle-5A: both builds fire high (buggy 74%, patched 48%) => indiscriminate
    # indictment (reworded from the old "indicts the check" phrasing).
    assert "indiscriminate" in low
    assert "intrinsic" in low
    assert "not a detection" in low


def test_asymmetric_buggy_silent_patched_high_is_a_catch_signal():
    # Cycle-5A, THE bug this fixes: silent on the broken build, loud on the
    # patch = the patch INTRODUCED the divergence — the strongest catch
    # signal, which the old note wrongly coached as an indictment.
    note = evidence_facts.fire_rate_fact(
        buggy_checked=20000,
        buggy_violated=0,
        patched_checked=20000,
        patched_violated=20000,
        screen_outcome_reason=None,
    )
    assert note is not None
    low = note.lower()
    assert "indicts the check" not in low
    assert "patch introduced" in low
    assert "strong discrimination signal" in low


def test_multi_firing_rate_capped_not_over_100():
    # Cycle-5A arithmetic fix: 2997/1000 must not render as 300%.
    note = evidence_facts.fire_rate_fact(
        buggy_checked=1000,
        buggy_violated=2997,
        patched_checked=1000,
        patched_violated=2997,
        screen_outcome_reason=None,
    )
    assert note is not None
    assert "300%" not in note
    assert "multi-firing" in note.lower()


def test_math65_screen_outcome_reason_appears_verbatim():
    reason = "above-ratio-cap / inverted (replay-only)"
    note = evidence_facts.fire_rate_fact(
        buggy_checked=20000,
        buggy_violated=14832,
        patched_checked=20000,
        patched_violated=9674,
        screen_outcome_reason=reason,
    )
    assert note is not None
    # The demotion reason must be carried through verbatim.
    assert reason in note


# --------------------------------------------------------------------------
# Chart-19: buggy fires on essentially every input -> intrinsic wording
# --------------------------------------------------------------------------

def test_chart19_buggy_full_rate_is_intrinsic():
    # Screening context: the buggy-side screen counted 20000/20000; the patched
    # replay-fuzz has not run yet at this point, so patched counts are absent.
    # (The patched-indictment branch takes priority over intrinsic, so a real
    # intrinsic reading requires the patched rate to be unavailable/low.)
    note = evidence_facts.fire_rate_fact(
        buggy_checked=20000,
        buggy_violated=20000,
        patched_checked=None,
        patched_violated=None,
        screen_outcome_reason=None,
    )
    assert note is not None
    assert "[fire-rate fact]" in note
    low = note.lower()
    # buggy 20000/20000 -> intrinsic-to-the-check wording.
    assert "intrinsic" in low


# --------------------------------------------------------------------------
# Low-rate case -> None or at least no indictment wording
# --------------------------------------------------------------------------

def test_low_rate_no_indictment():
    note = evidence_facts.fire_rate_fact(
        buggy_checked=20000,
        buggy_violated=285,
        patched_checked=20000,
        patched_violated=285,
        screen_outcome_reason=None,
    )
    if note is not None:
        low = note.lower()
        assert "indicts the check" not in low
        assert "intrinsic" not in low


def test_all_none_returns_none():
    note = evidence_facts.fire_rate_fact(
        buggy_checked=None,
        buggy_violated=None,
        patched_checked=None,
        patched_violated=None,
        screen_outcome_reason=None,
    )
    assert note is None
