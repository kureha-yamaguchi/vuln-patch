"""8.2 — the authority screen, and the canary that proves it is not a mirror.

The mechanism's whole safety rests on one idea: a generated reference is a GUESS
by the same kind of model that produced the accusation, so it must earn
admissibility mechanically -- by reproducing the BUGGY build where the defect
does not reach -- before it may speak about where it does.

Everything here fails CLOSED. An unscreened reference is inadmissible, exactly
as an unscreened relation is uninjected.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from java.relations.reference_impl import (                     # noqa: E402
    MIN_SCREENED_OBSERVABLES, WEAK_KINDS, disputed_observables,
    enumerate_observables, mirror_canary, reference_disagreement_fact,
    screen_reference)

OK3 = {'a': ['1.0'], 'b': ['2.0'], 'c': ['x- -0.0']}
OFF = {'a', 'b', 'c'}


# --- the screen admits only a reference that reproduces the incumbent ------

def test_a_reference_that_reproduces_the_buggy_build_is_admitted():
    ok, why = screen_reference(OK3, dict(OK3), OFF)
    assert ok is True and 'reproduces the buggy build on 3' in why


def test_one_off_defect_disagreement_DISCARDS_the_reference():
    """Not a weaker fact, not a hedged fact -- discarded."""
    buggy = dict(OK3, a=['9.0'])
    ok, why = screen_reference(OK3, buggy, OFF)
    assert ok is False and 'DISCARDED' in why


def test_agreement_on_too_few_observables_is_not_agreement():
    """Two matching values could be two constants. The screen has to be able
    to fail before its passing means anything."""
    ok, why = screen_reference({'a': ['1']}, {'a': ['1']}, {'a'})
    assert ok is False
    assert f'{MIN_SCREENED_OBSERVABLES} required' in why


def test_no_off_defect_observable_fails_CLOSED():
    """Screening ON the defect would require the reference to reproduce the
    BUG, which is backwards -- so with nothing safe to screen on, nothing is
    admitted."""
    ok, why = screen_reference(OK3, dict(OK3), set())
    assert ok is False and 'family-duty' in why


def test_missing_data_on_either_side_fails_CLOSED():
    assert screen_reference({}, OK3, OFF)[0] is False
    assert screen_reference(OK3, {}, OFF)[0] is False
    assert screen_reference(None, None, OFF)[0] is False


def test_only_off_defect_keys_are_screened():
    """A disagreement ON the defect must not discard the reference -- that is
    the region the reference is supposed to disagree about."""
    ref = dict(OK3, defectkey=['1.0'])
    buggy = dict(OK3, defectkey=['999.0'])
    ok, _why = screen_reference(ref, buggy, OFF)   # defectkey not in OFF
    assert ok is True


def test_weak_divergence_kinds_are_not_disagreement():
    """A last-ulp float difference and a generic-exception mismatch are noise
    on this comparison; reporting them as disagreement manufactures false
    screens. Inherited from the certifier's classifier, which learned it."""
    buggy = dict(OK3, a=['1.0000000000000002'])
    assert screen_reference(OK3, buggy, OFF,
                            {'a': 'value_ulp'})[0] is True
    assert 'value_ulp' in WEAK_KINDS


# --- the fact is emitted ONLY on an admitted reference --------------------

def test_no_fact_is_emitted_when_the_reference_was_discarded():
    """A hedged fact on a discarded reference hands the judge a claim whose
    authority was never established -- the exact shape of the uncited
    accusations this mechanism exists to reduce."""
    assert reference_disagreement_fact(
        'foo', False, 'discarded', {'a': ['1']}, {'a': ['2']}) is None


def test_a_fact_is_emitted_on_a_real_disagreement():
    fact = reference_disagreement_fact(
        'chiSquare', True, 'reproduces the buggy build on 4 observable(s)',
        {'chi': ['9.9']}, {'chi': ['3.3']})
    assert fact and 'chiSquare' in fact
    assert 'patched=' in fact and 'reference=' in fact


def test_agreement_produces_no_fact_either_way():
    """Agreement is not evidence for the patch any more than for the check."""
    assert reference_disagreement_fact(
        'foo', True, 'ok', {'a': ['1']}, {'a': ['1']}) is None


def test_the_fact_never_states_a_verdict():
    fact = reference_disagreement_fact(
        'foo', True, 'ok', {'a': ['1']}, {'a': ['2']})
    low = fact.lower()
    assert 'evidence about the observable, not a verdict' in low
    assert 'unsound' not in low and 'must be dismissed' not in low


def test_the_fact_states_its_own_provenance_and_screen():
    """The judge must be able to see WHY this reference has standing, or the
    fact is just another uncited assertion."""
    fact = reference_disagreement_fact(
        'foo', True, 'reproduces the buggy build on 5 observable(s)',
        {'a': ['1']}, {'a': ['2']})
    assert 'never from the code under review' in fact
    assert 'reproduces the buggy build on 5' in fact
    assert 'discarded outright' in fact


# --- THE MIRROR CANARY ----------------------------------------------------

def test_canary_fails_a_reference_that_sides_with_the_patched_build():
    """Fake patch + correct check. A reference agreeing with the patched
    artefact against the check is a mirror, and siding with the patched
    artefact is the label-dependent reasoning the firewall forbids."""
    ok, why = mirror_canary(reference_obs={'v': ['BAD']},
                            check_expected={'v': ['GOOD']},
                            patched_obs={'v': ['BAD']})
    assert ok is False and 'mirror' in why


def test_canary_passes_a_reference_that_sides_with_the_check():
    ok, _why = mirror_canary(reference_obs={'v': ['GOOD']},
                             check_expected={'v': ['GOOD']},
                             patched_obs={'v': ['BAD']})
    assert ok is True


def test_canary_reports_when_it_could_not_run():
    """'Could not run' must never read as 'passed' -- rule 15's whole family."""
    ok, why = mirror_canary({}, {'v': ['G']}, {'v': ['B']})
    assert ok is False and 'could not be run' in why


# --- the trigger, and its measured reach ----------------------------------

def test_the_trigger_is_the_existing_detector_not_a_new_guess():
    ctx = 'public double chiSquare(double[] a) { return 1.0; }'
    assert disputed_observables('chiSquare disagreed: got=1', ctx) == ['chiSquare']
    assert disputed_observables('nothing named here', ctx) == []
    assert disputed_observables('chiSquare', None) == []


def test_our_code_picks_the_observables_not_the_model():
    """The P4.2 lesson: a reference compared only where the model remembered to
    look has P4.2's bug in new clothes."""
    obs = enumerate_observables('[oracle:x] mismatch: expected=1.0 actual=2.0')
    assert obs == {'expected': ['1.0'], 'actual': ['2.0']}
