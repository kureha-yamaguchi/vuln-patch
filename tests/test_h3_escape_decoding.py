"""9.1b — H3 must not reject a faithful harness over newline SPELLING.

The 9.1 read judged five archived H3 rejections: four right, one wrong. The wrong
one observed the correct content but spelled the newline as the two characters
`\\n`, while the real JUnit failure message carries a literal newline.
Whitespace normalisation cannot bridge those.

That was one error in 120 legs when harnesses escaped newlines by choice. 8.4 now
REQUIRES escaping for exactly the checks H3 polices -- lifted, text-comparing
checks on formatted output -- so the shape became mandatory and the error rate
would have risen.

THE OVER-CORRECTION THIS FILE ALSO PINS. Decoding escapes ALONE flipped TWO of
the five, and the second flip was wrong: `actual=(.{1,600})` with DOTALL runs to
end-of-message, so on `expected=X actual=Y ... expected=Z` it swallows a later
expected clause -- and the expected half naturally contains the real wrong value
as a substring (expected = actual + the missing caret line). Decoding then turned
that over-capture into a FALSE agreement, silently excusing the exact divergence
the gate exists to catch. The module already refuses headline-wide containment
for this reason; over-capture was the back door to it.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from java.execution.oracle_strength import (                    # noqa: E402
    _values_match, lifted_observed_mismatch)

REALS = ['ion here\nassert (1;', 'iption here\nif (foo']


# --- the fix ---------------------------------------------------------------

def test_an_escaped_newline_matches_a_literal_one():
    assert _values_match(r'a\nb', 'a\nb') is True
    assert _values_match('a\nb', r'a\nb') is True


def test_escaped_tabs_too():
    assert _values_match(r'a\tb', 'a\tb') is True


def test_genuinely_different_values_still_differ():
    """The fix must not make everything match."""
    assert _values_match(r'a\nb', 'x\ny') is False
    assert _values_match('4.94', '0.257') is False


# --- the OVER-CORRECTION guard --------------------------------------------

def test_the_expected_half_cannot_excuse_a_divergence():
    """THE REGRESSION THIS FILE EXISTS FOR. A harness whose ACTUAL half diverges
    must still be rejected even though the message's EXPECTED half contains the
    real wrong value verbatim."""
    headline = ('[oracle:lifted-seed] semantic mismatch: '
                r'actual=javascript/complex.js:1: ERROR - error description here\n'
                r' expected=javascript/complex.js:1: ERROR - error description '
                r'here\nassert (1;\n          ^\n')
    got = lifted_observed_mismatch(headline, REALS)
    assert got is not None, (
        'the expected half must not be read as the observed value -- that is '
        'the Closure-62-c false-alarm hole')
    assert 'expected=' not in got


def test_a_faithful_escaped_harness_is_no_longer_rejected():
    """The 9.1 case: the actual half carries the right content, escaped."""
    headline = ('[oracle:lifted-seed] semantic mismatch: '
                r'actual=javascript/complex.js:1: ERROR - error description '
                r'here\nassert (1;\n'
                r' expected=javascript/complex.js:1: ERROR - error description '
                r'here\nassert (1;\n          ^\n')
    assert lifted_observed_mismatch(headline, REALS) is None


def test_the_whitespace_stripped_harness_is_still_rejected():
    """The two Closure-62 cases H3 was built for: the check normalised away the
    whitespace that IS the defect."""
    headline = ('[oracle:lifted-seed] semantic mismatch: '
                'actual=javascript/complex.js:1:ERROR-errordescriptionhere')
    assert lifted_observed_mismatch(headline, REALS) is not None


def test_a_different_numeric_value_is_still_rejected():
    """The Math-68 case."""
    headline = ('[oracle:lifted-x] semantic mismatch: '
                'actual=4.948952097518721 tol=6.998875175845751E-10')
    assert lifted_observed_mismatch(
        headline, ['0.25781992663680675', '11.41300466147456']) is not None


def test_non_lifted_checks_are_still_out_of_scope():
    assert lifted_observed_mismatch(
        '[oracle:roundtrip] mismatch: actual=9', ['1']) is None
