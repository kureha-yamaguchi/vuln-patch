"""8.2 stage 0 — the two-sided fact, the holdout split, and validator 3.

Every path here fails CLOSED. The mechanism's whole safety is that a generated
reference is a GUESS by the same kind of model that produced the accusation, so
it must earn admissibility mechanically before it may speak.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from java.relations.reference_impl import (                     # noqa: E402
    held_out_keys, mirror_canary, mirror_canary_correct_patch, pin_check,
    reference_comparison_fact)


# --- the holdout split ----------------------------------------------------

def test_shown_examples_are_excluded_from_the_exam():
    """What the generator was shown is an open book: reproducing it proves
    transcription, not understanding."""
    assert held_out_keys({'a': ['1'], 'b': ['2'], 'c': ['3']}, ['a']) == ['b', 'c']


def test_showing_everything_leaves_no_exam():
    assert held_out_keys({'a': ['1']}, ['a']) == []


def test_no_examples_shown_means_everything_is_held_out():
    assert held_out_keys({'a': ['1'], 'b': ['2']}, []) == ['a', 'b']


# --- VALIDATOR 3: the bug-copying catch -----------------------------------

def test_a_reference_contradicting_the_tests_pinned_answer_is_DISCARDED():
    """THE BLIND SPOT. A bug-copying reference agrees with the buggy build
    everywhere INCLUDING the defect, so the off-defect screen cannot see it.
    The failing test pins the right answer; contradicting it is the tell."""
    ok, why = pin_check({'chi': ['9.9']}, {'chi': ['3.3']}, ['chi'])
    assert ok is False
    assert 'copied the defect' in why and 'DISCARDED' in why


def test_a_reference_matching_the_pinned_answer_passes():
    ok, why = pin_check({'chi': ['3.3']}, {'chi': ['3.3']}, ['chi'])
    assert ok is True and 'matches' in why


def test_no_overlap_ABSTAINS_and_says_so():
    """'Could not check' must never read as 'passed' — rule 15's family."""
    ok, why = pin_check({'a': ['1']}, {'b': ['2']}, ['a'])
    assert ok is True
    assert 'ABSTAINS' in why and 'did not pass' in why


def test_missing_reference_values_fail_CLOSED():
    assert pin_check({}, {'a': ['1']})[0] is False
    assert pin_check(None, {'a': ['1']})[0] is False


def test_a_scalar_pinned_value_is_accepted_not_crashed():
    assert pin_check({'a': ['3.3']}, {'a': '3.3'}, ['a'])[0] is True


# --- the ONE two-sided fact -----------------------------------------------

def test_no_fact_from_a_discarded_reference():
    assert reference_comparison_fact(
        'f', False, 'discarded', {'a': ['1']}, {'a': ['2']}) is None


def test_the_disagreement_side():
    f = reference_comparison_fact('chiSquare', True, 'reproduces buggy on 5',
                                  {'chi': ['9.9']}, {'chi': ['3.3']},
                                  screened_count=5)
    assert 'DIFFERENT value' in f and 'chiSquare' in f
    assert "buggy build's LIVE behaviour" in f
    assert 'documented sibling observables' in f
    # State-twin correction (2026-08-07): everything runs at the failing
    # test's own state, whose INPUTS the generator was shown by design. The
    # honest claim is open-input/closed-output, not "never shown".
    assert "failing test's own state" in f
    assert 'sibling VALUES it was not' in f


def test_the_AGREEMENT_side_exists_and_is_the_same_sentence_shape():
    """Math-65's need: the fact must be able to say 'I independently compute
    exactly what the patch computes'."""
    f = reference_comparison_fact('chiSquare', True, 'reproduces buggy on 5',
                                  {'chi': ['3.3']}, {'chi': ['3.3']},
                                  screened_count=5)
    assert 'SAME value' in f
    assert 'independent implementation' in f      # same opening as disagreement


def test_neither_side_carries_verdict_language():
    """Cycle 8 measured four wording-side mechanisms that leaned on the judge;
    all four failed. This one states a computed result and stops."""
    for patched, ref in (({'a': ['1']}, {'a': ['2']}),
                         ({'a': ['1']}, {'a': ['1']})):
        f = reference_comparison_fact('f', True, 'ok', patched, ref).lower()
        for banned in ('must be dismissed', 'unsound', 'sound', 'dismiss',
                       'therefore the patch', 'is overfit', 'is correct'):
            assert banned not in f, f'verdict language leaked: {banned!r}'
        assert 'not a verdict' in f


def test_a_weak_kind_difference_counts_as_AGREEMENT():
    """Inherited semantics: a value_ulp-scale difference is agreement."""
    f = reference_comparison_fact('f', True, 'ok', {'a': ['1.0000000000000002']},
                                  {'a': ['1.0']}, {'a': 'value_ulp'})
    assert 'SAME value' in f


def test_nothing_comparable_says_nothing():
    assert reference_comparison_fact('f', True, 'ok', {'a': ['1']},
                                     {'b': ['2']}) is None


# --- BOTH canaries --------------------------------------------------------

def test_canary1_fake_patch_correct_check_must_side_with_the_check():
    assert mirror_canary({'v': ['BAD']}, {'v': ['GOOD']}, {'v': ['BAD']})[0] is False
    assert mirror_canary({'v': ['GOOD']}, {'v': ['GOOD']}, {'v': ['BAD']})[0] is True


def test_canary2_correct_patch_wrong_check_must_side_with_the_patch():
    """The Math-65 shape. A mechanism that cannot do this cannot exonerate,
    which is the entire reason stage 1 exists."""
    ok, why = mirror_canary_correct_patch(
        reference_obs={'v': ['WRONG']}, patched_obs={'v': ['RIGHT']},
        check_expected={'v': ['WRONG']})
    assert ok is False and 'cannot exonerate' in why
    assert mirror_canary_correct_patch(
        {'v': ['RIGHT']}, {'v': ['RIGHT']}, {'v': ['WRONG']})[0] is True


def test_both_canaries_report_when_they_could_not_run():
    assert mirror_canary({}, {'v': ['G']}, {'v': ['B']})[0] is False
    ok, why = mirror_canary_correct_patch({}, {'v': ['G']}, {'v': ['B']})
    assert ok is False and 'could not be run' in why
