"""Spec E: fact-priority instruction in the soundness-judge prompt.

These are pure string-presence assertions over the module-level guidance
constant that relation_verifier feeds to the verify prompt. No LLM, no I/O.
"""
from java.relations.relation_verifier import _GUIDANCE

NEW_BLOCK_TITLE = "MECHANICAL FACTS OUTRANK PROVENANCE"
OBSERVED_TITLE = "OBSERVED EVIDENCE BEATS HYPOTHETICALS"


def test_new_block_present():
    assert NEW_BLOCK_TITLE in _GUIDANCE


def test_new_block_follows_observed_evidence():
    observed_at = _GUIDANCE.find(OBSERVED_TITLE)
    new_at = _GUIDANCE.find(NEW_BLOCK_TITLE)
    assert observed_at != -1, "observed-evidence rule missing"
    assert new_at != -1, "new fact-priority rule missing"
    assert new_at > observed_at, "new block must appear AFTER the observed-evidence rule"


def test_new_block_mentions_provenance():
    assert "provenance" in _GUIDANCE


def test_new_block_states_computed_fact_override():
    # The rule must name the bracketed computed facts and the override-only-by-another-fact intent.
    assert "[buggy-replay fact]" in _GUIDANCE
    assert "[differential replay]" in _GUIDANCE
    assert "[trigger-test lift]" in _GUIDANCE
    assert "identical on both builds" in _GUIDANCE


def test_preexisting_calibration_strings_untouched():
    # Spec E is a single-block insertion; these pre-existing calibration
    # strings must remain present and unchanged.
    assert "OBSERVED EVIDENCE BEATS HYPOTHETICALS" in _GUIDANCE
    assert "ROUNDING FLOOR" in _GUIDANCE
    assert "TEST the counterexample, twice" in _GUIDANCE
