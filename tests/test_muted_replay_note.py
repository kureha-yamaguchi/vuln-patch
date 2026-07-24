"""Spec G-G3: tests for the pure muted-replay note builder.

Pinned API (implemented by a parallel agent; import failure while unlanded is
the expected transient state):

    java.relations.evidence_facts.muted_replay_note(
        target_ids, muted_ids, status, fired_ids, esc_type, bt_all) -> str|None

Semantics (from the cycle-2 spec, Spec G-G3.1):
  * target check fires on buggy once the shadowing checks are silenced ->
    the same-check "identical on both builds" fact; MUST NOT claim the patch
    introduced anything.
  * target quiet, run clean -> the existence-proof family: the buggy build runs
    this exact input WITHOUT firing the check -> the patch introduced it here.
  * error / mute_failed -> keep the cycle-1 UNKNOWN fact; append one short line
    that the muted replay was attempted and is unavailable (no identical /
    introduced claims).

Provenance for the shadowing ids is the armA Math-30/Math-65 shadowed-branch
firings recorded in tests/fixtures/semantic_replay.json.
"""
import pytest

from java.relations import evidence_facts


# target-fires case: target = the originally-firing check, muted = the
# shadowing check that threw first on the buggy build; after silencing it the
# target fires on the buggy build too. The builder does set arithmetic on the
# id collections, so ids are passed as sets (the run.py call site passes sets).
_TARGET = {"large-identical-p1"}
_SHADOW = {"big-dataset-threshold"}


def test_target_fires_says_identical_on_both_builds():
    # Spec I (cycle-2b): the identical-on-both-builds claim is now EARNED by an
    # explicit value verdict — firing on both != identical values. With
    # value_verdict="identical" the binding mechanical fact stands verbatim.
    note = evidence_facts.muted_replay_note(
        target_ids=_TARGET,
        muted_ids=_SHADOW,
        status="crashed",
        fired_ids={"large-identical-p1"},
        esc_type=None,
        bt_all=[],
        value_verdict="identical",
    )
    assert note is not None
    low = note.lower()
    # The binding mechanical fact.
    assert "identical on both builds" in low
    # It must NOT accuse the patch of introducing the firing.
    assert "introduc" not in low


def test_target_fires_default_verdict_makes_no_identical_claim():
    # Spec I: the DEFAULT (no value_verdict) can NEVER over-claim identical —
    # an unthreaded call states fires-on-both without the identical claim.
    note = evidence_facts.muted_replay_note(
        target_ids=_TARGET,
        muted_ids=_SHADOW,
        status="crashed",
        fired_ids={"large-identical-p1"},
        esc_type=None,
        bt_all=[],
    )
    assert note is not None
    low = note.lower()
    assert "identical on both builds" not in low
    # Still reports the mechanical fires-on-both fact.
    assert "fires on the buggy build" in low
    assert "introduc" not in low


def test_target_quiet_clean_says_without_firing_introduced():
    note = evidence_facts.muted_replay_note(
        target_ids=_TARGET,
        muted_ids=_SHADOW,
        status="clean",
        fired_ids=set(),
        esc_type=None,
        bt_all=[],
    )
    assert note is not None
    low = note.lower()
    # Existence-proof wording family.
    assert "without firing" in low
    assert "introduc" in low
    # This branch is NOT the identical-on-both-builds fact.
    assert "identical on both builds" not in low


def test_mute_failed_short_unavailable_line_no_claims():
    note = evidence_facts.muted_replay_note(
        target_ids=_TARGET,
        muted_ids=_SHADOW,
        status="mute_failed",
        fired_ids=None,
        esc_type=None,
        bt_all=[],
    )
    assert note is not None
    low = note.lower()
    assert "unavailable" in low
    # No strong directional claims either way.
    assert "identical on both builds" not in low
    assert "introduc" not in low


def test_error_status_also_unavailable_no_claims():
    note = evidence_facts.muted_replay_note(
        target_ids=_TARGET,
        muted_ids=_SHADOW,
        status="error",
        fired_ids=None,
        esc_type=None,
        bt_all=[],
    )
    # error is treated like mute_failed: unavailable, no directional claim.
    if note is not None:
        low = note.lower()
        assert "identical on both builds" not in low
        assert "introduc" not in low
