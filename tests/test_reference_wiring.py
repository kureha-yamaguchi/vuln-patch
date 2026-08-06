"""8.2 stage 1 — the pipeline wiring. Every exit records WHY.

A step that produced nothing must SAY nothing-and-why. An absent event is
indistinguishable from a step that silently failed, and this cycle met that
failure six times -- the stage read-out is per-event precisely so it cannot
happen again here.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

SRC = (ROOT / 'src' / 'java' / 'run.py').read_text()


def _helper():
    i = SRC.index('def _reference_impl_fact(')
    j = SRC.index('\ndef parse_args(')
    return SRC[i:j]


def test_the_flag_exists_and_is_OFF_by_default():
    """Ladder-gated: it must not affect any run that did not ask for it."""
    assert '"--reference_impl", action="store_true"' in SRC
    assert 'reference_impl=True' not in SRC


def test_the_call_site_is_gated_on_the_flag():
    assert "getattr(args, 'reference_impl', False)" in SRC


def test_every_exit_path_records_an_event():
    """The per-event read is the stage-1 deliverable; a silent return would
    make a discarded reference indistinguishable from an absent one."""
    h = _helper()
    returns = len(re.findall(r'\n        return None', h))
    events = len(re.findall(r'_re\(', h))
    assert returns >= 7, f'expected the fail-closed exits, found {returns}'
    assert events >= returns, (
        f'{returns} early exits but only {events} recorded events -- some exit '
        f'is silent')


def test_each_validator_records_its_REASON_not_just_its_outcome():
    h = _helper()
    for step in ('screen ADMITTED', 'screen DISCARDED',
                 'pin-check PASSED', 'pin-check DISCARDED'):
        assert step in h, f'missing outcome: {step}'
    assert h.count('reason=') >= 6, 'outcomes recorded without reasons'


def test_an_implementation_leak_is_recorded_and_fails_closed():
    h = _helper()
    assert 'except ImplementationLeak' in h
    assert 'REFUSED: implementation leak' in h


def test_the_skeleton_is_body_stripped_before_the_prompt():
    """assemble_class_context KEEPS the patched method's body; passing it raw
    would leak exactly the artefact the reference must be blind to."""
    h = _helper()
    assert 'strip_bodies(ctx)' in h
    assert 'skeleton=skeleton' in h


def test_no_fact_can_be_returned_after_a_discard():
    """Ordering pin: every discard returns before the fact is built."""
    h = _helper()
    fact_at = h.index('reference_comparison_fact(')
    for marker in ('screen DISCARDED', 'pin-check DISCARDED',
                   'reference DISCARDED'):
        assert h.index(marker) < fact_at, f'{marker} is recorded after the fact'


def test_the_generator_is_the_run_model_not_a_default():
    """A stale default deployment once 404'd every verify call and the stage
    silently became a no-op."""
    assert 'model=args.model or config.LOCAL_LLM_MODEL' in _helper()


def test_BOTH_judge_doors_carry_the_mechanism():
    """Spec K, one-door fact parity -- learned once on Math-73-c and re-learned
    on stage-1 roll 2, which recorded ZERO reference-impl events because the leg
    convicted on the REPLAY track while the wiring sat only on the harness
    track. A fact attached at one door and not the other makes the two tracks
    judge the same check differently."""
    n = SRC.count("getattr(args, 'reference_impl', False)")
    assert n == 2, (
        f'expected the mechanism at BOTH judge doors, found {n}. '
        f'disputed_computation_fact is called at both; so must this be.')


def test_the_two_doors_use_their_own_firing_and_trusted_values():
    """Copy-paste across doors is how the wrong variables get read: the replay
    track's firing is `_fired` and its trusted values `_tvals`."""
    i = SRC.index('(replay track)')
    seg = SRC[i - 900:i]
    assert 'fired=_fired' in seg and 'trusted_values=_tvals' in seg
