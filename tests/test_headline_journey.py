"""The JOURNEY from a fired alarm to the mechanical consumer that reads it.

Batch-8 smoke finding. Every piece of 8.4 was verified in isolation and every
piece was correct: the prompt emits the Raw keys, the harness records them, the
comparison reads them, the lint catches violations. Nobody checked that the keys
SURVIVE THE TRIP. Measured live: of 4 headlines reporting a normalized value,
only 1 still carried `actualRaw=` by the time the comparison ran.

TWO cutters sit on that trip, and the first diagnosis found only one of them.

  * `exception_headlines` caps every headline at 200 characters. Real, pinned
    below -- but NOT what broke the smoke.
  * `_HEADLINE_RES` matches a LINE (`(.+)`, and `.` excludes newline). 8.4's raw
    form of formatted text CONTAINS newlines, so the message breaks and every
    key after the first one is never captured at all.

The second was found by testing the first fix against the smoke's own data
before spending a re-run on it: the headlines that lost their keys are 312 and
314 characters with NO ellipsis, so nothing had capped them.

Rule 15's seventh instance, and the sentence that generalizes it:
**existence is a property of the producer; arrival is a property of the
journey, and only an end-to-end path tests the journey.**
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from java.execution.oracle_strength import (                    # noqa: E402
    exception_headline_pairs, exception_headlines)
from java.relations.evidence_facts import (                     # noqa: E402
    _captured_raw_observed, fired_value_vs_trusted,
    raw_value_vs_pinned)


def _alarm(pad):
    """A realistic 8.4 alarm whose Raw keys sit past the 200-char cap."""
    return ('com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: '
            '[oracle:print-roundtrip] semantic mismatch: ' + pad
            + ' expectedNormalized=x--0.0 actualNormalized=x--0.0'
            ' expectedRaw=x- -0.0 actualRaw=x--0.0')


def _jazzer_output(alarm):
    return f'== Java Exception: {alarm}\n\tat Foo.bar(Foo.java:1)\n'


# --- 1. THE JOURNEY TEST ---------------------------------------------------

def test_a_long_raw_carrying_alarm_reaches_the_comparison_INTACT():
    """The end-to-end pin the smoke's finding demands: an alarm well over the
    cap must still deliver its Raw keys to `fired_value_vs_trusted`."""
    alarm = _alarm('x' * 300)
    assert len(alarm) > 200

    pairs = exception_headline_pairs(_jazzer_output(alarm))
    assert pairs, 'the headline must be recognised at all'
    capped, full = pairs[0]

    # what every OTHER consumer sees: capped, and missing the keys
    assert capped.endswith('…')
    assert 'actualRaw=' not in capped

    # what the comparison now sees: the whole thing
    assert 'actualRaw=' in full
    assert _captured_raw_observed(full) == ('x--0.0', False)

    # and it produces a real verdict rather than "unknown"
    assert raw_value_vs_pinned(full, ['x- -0.0']) == 'differs'
    assert fired_value_vs_trusted(full, ['x- -0.0']) == 'differs'

    # the regression this test exists to prevent
    assert fired_value_vs_trusted(capped, ['x- -0.0']) == 'unknown', (
        'the capped form is what used to be passed; if this ever stops being '
        'unknown the test has lost its meaning')


def test_the_runner_stashes_the_full_form_for_the_capped_one():
    """The mapping the run threads is keyed by the capped headline, because
    that is what the loop iterates over."""
    alarm = _alarm('y' * 300)
    pairs = exception_headline_pairs(_jazzer_output(alarm))
    mapping = {c: f for c, f in pairs}
    capped = pairs[0][0]
    assert 'actualRaw=' in mapping[capped]


# --- 2. AN ELLIPSIS INPUT READS DOUBTFUL -----------------------------------

def test_a_truncated_message_can_never_produce_a_confident_verdict():
    """Defence in depth: the caller now passes the uncapped form, but if a
    capped string ever reaches the comparison again the failure must be LOUD --
    'unknown' -- rather than a confident verdict computed from a prefix."""
    truncated = ('[oracle:x] semantic mismatch: expectedNormalized=a '
                 'actualRaw=x--0.0 and then it got cut…')
    _val, ambiguous = _captured_raw_observed(truncated)
    assert ambiguous is True
    assert raw_value_vs_pinned(truncated, ['zzz']) == 'unknown'
    assert raw_value_vs_pinned(truncated, ['x--0.0']) == 'unknown', (
        'a prefix must not match by accident')


def test_an_untruncated_message_is_still_confident():
    """The doubt rule must not swallow the normal case."""
    clean = '[oracle:x] semantic mismatch: actualRaw=x--0.0'
    assert _captured_raw_observed(clean) == ('x--0.0', False)
    assert raw_value_vs_pinned(clean, ['x- -0.0']) == 'differs'


def test_ascii_ellipsis_counts_too():
    msg = '[oracle:x] actualRaw=abc...'
    assert _captured_raw_observed(msg)[1] is True


# --- 3. THE CAPPED CONSUMERS ARE UNCHANGED ---------------------------------

def test_exception_headlines_returns_exactly_what_it_returned_before():
    """Prompt size and display are why the cap exists; they keep it."""
    alarm = _alarm('z' * 300)
    out = exception_headlines(_jazzer_output(alarm))
    assert len(out) == 1
    assert len(out[0]) == 201 and out[0].endswith('…')


def test_short_headlines_are_untouched():
    short = ('com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: '
             '[oracle:s] semantic mismatch: tiny')
    out = exception_headlines(_jazzer_output(short))
    assert out == [short]
    assert not out[0].endswith('…')


def test_dedup_still_keys_on_the_CAPPED_form():
    """The behaviour most at risk from this change: two alarms identical for
    their first 200 characters and differing after must still collapse to ONE
    headline, exactly as before. De-duplicating on the full form instead would
    silently multiply the judge's workload."""
    a = _alarm('q' * 300)
    b = _alarm('q' * 300) + ' TRAILING DIFFERENCE'
    out = exception_headlines(_jazzer_output(a) + _jazzer_output(b))
    assert len(out) == 1, 'dedup must still key on the capped form'
    # the pair form keeps the same single entry, with ONE full text
    pairs = exception_headline_pairs(_jazzer_output(a) + _jazzer_output(b))
    assert len(pairs) == 1


def test_max_len_is_still_honoured_when_passed_explicitly():
    alarm = _alarm('w' * 300)
    out = exception_headlines(_jazzer_output(alarm), max_len=50)
    assert len(out[0]) == 51 and out[0].endswith('…')


def test_empty_output_is_still_empty():
    assert exception_headlines('') == []
    assert exception_headline_pairs('') == []


# --- 4. THE ACTUAL DEFECT: a newline inside a raw value cuts the message ----
#
# The first diagnosis blamed the 200-char cap. Testing that fix against the
# smoke's own data refuted it: the headlines that lost their keys are 312 and
# 314 characters and carry NO ellipsis, so nothing capped them. They stop
# exactly where the raw value's first embedded newline sits.
#
#   _HEADLINE_RES = re.compile(r'==\s*Java Exception:\s*(.+)')
#
# `.` does not match a newline, so the capture is a LINE. Closure-62's pinned
# value is multi-line formatted output, so its raw form breaks the message and
# everything after -- including actualRaw= -- is never captured at all.

def test_a_real_newline_in_a_raw_value_truncates_the_headline():
    """The defect, pinned. This is what the smoke actually found."""
    alarm = ('com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: '
             '[oracle:fmt] semantic mismatch: expectedNormalized=a '
             'actualNormalized=b expectedRaw=line one\nline two '
             'actualRaw=x')
    pairs = exception_headline_pairs(_jazzer_output(alarm))
    _capped, full = pairs[0]
    assert 'actualRaw=' not in full, (
        'the capture stops at the embedded newline -- raising the cap cannot '
        'help, because nothing was capped')
    assert full.endswith('expectedRaw=line one')


def test_escaped_newlines_keep_the_message_intact():
    """The fix: the prompt requires `\\n` as two characters, so the message
    stays one line and every key survives."""
    alarm = ('com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow: '
             '[oracle:fmt] semantic mismatch: expectedNormalized=a '
             'actualNormalized=b expectedRaw=line one\\nline two '
             'actualRaw=got\\nthis')
    _capped, full = exception_headline_pairs(_jazzer_output(alarm))[0]
    assert 'actualRaw=' in full
    assert _captured_raw_observed(full)[0] == 'got\\nthis'


def test_the_comparison_decodes_BOTH_sides():
    """An escaped raw value and an escaped source literal denote the same
    string and must compare equal. Closure-62's exact shape."""
    pinned = r'javascript/complex.js:1: ERROR - here\nassert (1;\n     ^\n'
    alarm = ('[oracle:fmt] semantic mismatch: expectedNormalized=x '
             'actualNormalized=y actualRaw='
             r'javascript/complex.js:1: ERROR - here\nassert (1;\n     ^\n')
    assert raw_value_vs_pinned(alarm, [pinned]) == 'matches'


def test_decoding_both_sides_does_not_create_false_matches():
    """The dismissal asymmetry survives the change: a genuinely different raw
    value must still read 'differs', not 'matches'."""
    pinned = r'expected\nvalue'
    alarm = r'[oracle:x] actualRaw=different\nvalue'
    assert raw_value_vs_pinned(alarm, [pinned]) == 'differs'


def test_decoding_is_identity_on_escape_free_values():
    """Everything that worked before must still work."""
    assert raw_value_vs_pinned('[oracle:x] actualRaw=x--0.0',
                               ['x--0.0']) == 'matches'
    assert raw_value_vs_pinned('[oracle:x] actualRaw=x--0.0',
                               ['x- -0.0']) == 'differs'
