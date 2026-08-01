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


# --- 8.4: compare normalized, RECORD raw ---------------------------------

def test_codegen_prompt_requires_raw_values_alongside_normalized():
    """The setup-divergence rung asks whether the reported value equals a value
    the failing test pins — and the test pins the RAW form. A message carrying
    only the normalized form can never match, so the rung is structurally dead
    for every normalizing check (17% of accepted harnesses; why Closure-62 was
    unreachable)."""
    from java.harness import prompts
    src = open(prompts.__file__).read()
    assert 'expectedRaw=' in src and 'actualRaw=' in src
    assert 'expectedNormalized=' in src and 'actualNormalized=' in src


def test_the_raw_keys_are_named_not_positional():
    """Standing rule 15's family, in a component whose failure mode is silent:
    an extractor reading the wrong form would be fail-open and invisible. The
    prompt must forbid positional reporting explicitly."""
    from java.harness import prompts
    src = open(prompts.__file__).read()
    assert 'Named keys, never positional' in src


def test_raw_keys_are_conditional_so_their_absence_is_meaningful():
    """Emitting Raw only when normalization happened makes absence mean 'no
    normalization', not 'normalized but forgot to record'."""
    from java.harness import prompts
    src = open(prompts.__file__).read()
    assert 'ONLY when you actually' in src


def test_the_normalize_instruction_itself_is_unchanged():
    """8.4 adds recording; it must NOT relax the comparison. Normalizing before
    comparing is still correct and still required."""
    from java.harness import prompts
    src = open(prompts.__file__).read()
    assert 'never compare raw strings — normalize BOTH sides' in src
