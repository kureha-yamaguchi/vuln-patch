"""8.4 — the raw-vs-pinned comparison, the consumer the raw recording exists for.

The rung asks: does the fired value equal a value the failing test pins? For a
normalizing check the reported value used to be a normalized derivative, which
can never equal the test's raw literal — so the rung was structurally dead for
those checks. These tests pin the comparison that reads the raw value instead,
and above all pin the ASYMMETRY it is built around: "matches" is the only
verdict that licenses a dismissal, so every uncertainty must resolve away from
it.
"""
import glob
import json
import re
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

import java.relations.evidence_facts as ef                      # noqa: E402
from java.relations.evidence_facts import (                     # noqa: E402
    _captured_raw_observed, _decode_java_literal, fired_value_vs_trusted,
    raw_value_vs_pinned)


# --- decoding is decoding, not normalization ------------------------------

def test_java_escapes_decode_to_the_value_they_denote():
    assert _decode_java_literal(r'a\nb') == 'a\nb'
    assert _decode_java_literal(r'q\tw\\e') == 'q\tw\\e'
    assert _decode_java_literal(r'AB') == 'AB'


def test_decoding_collapses_nothing():
    """The checks this serves are the ones whose defects ARE whitespace
    defects, so the one transformation applied must preserve every character."""
    assert _decode_java_literal('x-  -0.0 ') == 'x-  -0.0 '
    assert _decode_java_literal(r'a\n\n  b') == 'a\n\n  b'


def test_unknown_escapes_are_left_verbatim_not_guessed():
    assert _decode_java_literal(r'a\qb') == r'a\qb'


# --- the capture -----------------------------------------------------------

def test_a_raw_value_containing_spaces_is_captured_whole():
    """`x- -0.0` is Closure-38's shape: the space IS the defect, so a
    capture that stopped at whitespace would destroy the thing being read."""
    assert _captured_raw_observed('m expectedRaw=q actualRaw=x- -0.0') == (
        'x- -0.0', False)


def test_trailing_metadata_keys_do_not_leak_into_the_value():
    """Observed in the compliance smoke: `actualRaw=x--0.0 parseErrorCount=0`.
    Stopping only at the four 8.4 keys captured the metadata into the value."""
    val, ambiguous = _captured_raw_observed('m actualRaw=x--0.0 parseErrorCount=0')
    assert val == 'x--0.0'
    assert ambiguous is True, 'an unknown stop key must mark the capture doubtful'


def test_stopping_at_a_KNOWN_key_is_not_ambiguous():
    assert _captured_raw_observed('m actualRaw=a b expectedRaw=z') == ('a b', False)


# --- THE ASYMMETRY: every uncertainty resolves away from "matches" ---------

def test_absent_key_is_unknown_so_the_whole_comparison_is_inert_pre_8_4():
    assert raw_value_vs_pinned('[oracle:x] fired: expected=1 actual=2', ['1']) \
        == 'unknown'


def test_ambiguous_capture_cannot_produce_differs():
    """A value that may have swallowed following text does not support the
    positive claim 'differs from every value the test pins'."""
    assert raw_value_vs_pinned('m actualRaw=abc otherKey=1', ['zzz']) == 'unknown'


def test_but_an_exact_hit_still_wins_through_ambiguity():
    """A real hit is a real hit: exact equality is trustworthy however the
    capture ended."""
    assert raw_value_vs_pinned('m actualRaw=abc otherKey=1', ['abc']) == 'matches'


def test_multiline_raw_that_does_not_match_is_unknown_not_differs():
    assert raw_value_vs_pinned('m actualRaw=line1\nline2', ['zzz']) == 'unknown'


def test_multiline_raw_that_matches_a_pinned_literal_IS_a_match():
    """Closure-62's shape: the test pins a multi-line literal with `\\n`
    escapes; the runtime value carries real newlines. Decoding the pinned side
    is what lets them meet."""
    assert raw_value_vs_pinned('m actualRaw=a\nb  ^\n', [r'a\nb  ^\n']) == 'matches'


def test_no_tolerance_and_no_trimming():
    """The rounding floor the numeric comparison rightly applies has no
    analogue here — these are strings whose differences ARE the finding."""
    assert raw_value_vs_pinned('m actualRaw=x--0.0', ['x- -0.0']) == 'differs'
    assert raw_value_vs_pinned('m actualRaw=abc ', ['abc']) == 'differs'


def test_expectedRaw_is_never_the_compared_field():
    """`expectedRaw` is the check's own reference, and for a lifted test that
    reference is frequently DERIVED from the pinned literal — comparing it
    would match by construction and dismiss every lifted firing. Same trap
    `_observed_numbers` was narrowed to avoid, reached from the string side."""
    msg = 'm expectedRaw=x- -0.0 actualRaw=x--0.0'
    assert raw_value_vs_pinned(msg, ['x- -0.0']) == 'differs', \
        'the check\'s own expected value must not license a dismissal'


def test_no_pinned_values_is_unknown():
    assert raw_value_vs_pinned('m actualRaw=abc', []) == 'unknown'
    assert raw_value_vs_pinned('m actualRaw=abc', None) == 'unknown'


# --- precedence at the shipped entrypoint ---------------------------------

def _numeric_only(fired, tv):
    """The pre-8.4 behaviour, recomputed rather than remembered."""
    fn, tn = ef._fired_numbers(fired), ef._trusted_numbers(tv)
    if not fn or not tn:
        return 'unknown'
    return ('matches' if any(ef._close(a, b) for a in fn for b in tn)
            else 'differs')


def test_raw_decides_when_present():
    msg = 'm actualRaw=x--0.0 trailing 777'
    assert raw_value_vs_pinned(msg, ['x- -0']) != 'unknown'
    assert fired_value_vs_trusted(msg, ['x- -0']) == raw_value_vs_pinned(
        msg, ['x- -0'])


def test_numeric_path_is_byte_identical_when_no_raw_value_exists():
    for msg, tv in (('[oracle:a] actual=4.94 expected=6.99', ['4.94']),
                    ('[oracle:b] got=12', ['99']),
                    ('[oracle:c] nothing numeric', ['5']),
                    ('[oracle:d] trailing 3.0', [])):
        assert fired_value_vs_trusted(msg, tv) == _numeric_only(msg, tv)


def test_the_numeric_coincidence_this_closes():
    """MEASURED, on real alarms from `c84_20260801_174840`, leg
    `02_patch1-Closure-38-SequenceR_o` (a FAKE patch, where every alarm should
    be KEPT).

    The leg's single pinned literal is `x- -0`. Two alarms reported
    `actualRaw=x--0.0`. Numerically, the `-0.0` in the message is within the
    rounding floor of the `0` inside `x- -0`, so the numeric comparison
    answered "matches" — the one verdict that instructs the judge to dismiss.
    The actual strings differ by exactly the missing separator space, which IS
    Closure-38's defect.

    So the old comparison could license a mechanical dismissal on a fake patch
    from a coincidence of digits. It did not do so in that run — the lift
    detector never fired on that leg, so no note was delivered (verified: 0
    occurrences of "[trigger-test lift] this oracle lifts" in the trace). This
    is therefore a LATENT false dismissal, demonstrated on real data and now
    closed — not a catch that was observed being lost."""
    pinned = ['x- -0']
    msg = ('[oracle:boundary-negative-zero] semantic mismatch: '
           'expectedNormalized=x--0.0 actualNormalized=x--0.0 '
           'expectedRaw=x- -0.0 actualRaw=x--0.0')
    assert _numeric_only(msg, pinned) == 'matches'      # the old answer
    assert fired_value_vs_trusted(msg, pinned) == 'differs'   # the new one


# --- inertness on every archived population (the regression claim) --------

@pytest.mark.parametrize('path', [
    'tests/fixtures/cases228.jsonl',
    'tests/fixtures/correct_dismissals.jsonl',
    'docs/replay/backtrack/guard_population.json',
])
def test_no_archived_verdict_moves(path):
    """333 archived rows across three populations. None carries an
    `actualRaw=` key and none can — the field postdates every one of them. So
    this proves NO REGRESSION and nothing else; it is not a safety claim about
    the dismissal direction, which only a live suite can exercise."""
    p = ROOT / path
    if not p.exists():
        pytest.skip(f'{path} not present')
    rows = (json.load(p.open()) if p.suffix == '.json'
            else [json.loads(l) for l in p.open()])
    rows = rows if isinstance(rows, list) else rows.get('rows', [])
    assert rows
    for r in rows:
        fired = r.get('fired') or r.get('fired_assertion') or ''
        tv = r.get('trusted_values')
        assert 'actualRaw=' not in fired
        assert raw_value_vs_pinned(fired, tv) == 'unknown'
        assert fired_value_vs_trusted(fired, tv) == _numeric_only(fired, tv)


def test_it_runs_on_REAL_raw_carrying_alarms_from_the_smoke():
    """Rule 15's corollary: exercise it where it CAN fire. Alarm-scoped
    extraction, so prompt text naming the keys cannot inflate the count — the
    inflation direction of rule 8, met four times this cycle."""
    traces = glob.glob(str(ROOT / 'runs-archive' / 'runs' / 'c84_*'
                           / '*Closure-38*' / 'trace.md'))
    if not traces:
        pytest.skip('compliance-smoke archive not present')
    txt = open(traces[0], errors='ignore').read()
    alarms = [m.group(1) for m in re.finditer(
        r'FuzzerSecurityIssue\w*: (\[oracle:[^\n]{0,600})', txt)]
    raws = [a for a in alarms if 'actualRaw=' in a]
    assert raws, 'no raw-carrying alarm in the archive'
    for a in raws:
        v = raw_value_vs_pinned(a, ['x- -0'])
        assert v in ('matches', 'differs', 'unknown')
        val, _amb = _captured_raw_observed(a)
        assert val is not None and '\n' not in val
