"""Cycle-7 item 2a fix (i): the expected-value extractor recognises projects'
own equality-assertion helpers, not only the three JUnit names.

Why this matters: those expected values feed the one rule permitted to dismiss a
firing as "this just replays the test's own scenario". Measured over the 228
recorded cases, the extractor returned nothing for 204 (89%), so that rule
reached its dismissal branch zero times in 60 leg-runs. See
docs/replay/ITEM2A-CHRONIC-FPS.md.

The JUnit pass must keep its exact prior behaviour — these tests pin that as
hard as they pin the new capability.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'src'))

from java.parsing.java_source import expected_assert_literals as EAL  # noqa: E402


# --- the JUnit forms, unchanged -------------------------------------------

def test_plain_assert_equals_literal_first():
    assert EAL('assertEquals("expected-name", actual);') == ['expected-name']


def test_message_first_overload_takes_the_second_arg():
    """The heuristic requires arg1 to be NON-quoted, so it only disambiguates
    when the expected value is not itself a string."""
    src = 'assertEquals("a helpful message", 2.5, actual);'
    assert EAL(src) == ['2.5']


def test_KNOWN_LIMITATION_message_and_expected_both_strings():
    """Pre-existing, NOT introduced here, and recorded rather than silently
    fixed: when both the message and the expected value are strings, the
    message-first heuristic cannot tell them apart and takes the MESSAGE.

    That is a wrong trusted value, and a wrong trusted value can only cause a
    spurious "matches" — i.e. a wrong dismissal. Left alone in this batch
    because changing it changes existing behaviour and so needs its own
    measurement over the 228 records; pinned here so the behaviour is known
    rather than assumed."""
    src = 'assertEquals("a helpful message", "expected-name", actual);'
    assert EAL(src) == ['a helpful message']


def test_delta_overload_takes_the_first_arg():
    assert EAL('assertEquals(2.5, actual, 0.001);') == ['2.5']


def test_numeric_suffix_is_stripped():
    assert EAL('assertEquals(2.5f, actual);') == ['2.5']


def test_computed_expected_is_skipped():
    """A non-literal is not trustworthy provenance — deliberately skipped."""
    assert EAL('assertEquals(computeExpected(x), actual);') == []


def test_trivial_literals_are_dropped():
    """Short literals match spuriously as substrings of a fired message."""
    assert EAL('assertEquals(0, actual); assertEquals(-1, other);') == []


def test_assert_same_and_array_equals_still_work():
    src = ('assertSame("the-instance", actual);\n'
           'assertArrayEquals("the-array", other);')
    assert EAL(src) == ['the-instance', 'the-array']


# --- the new capability ---------------------------------------------------

def test_project_defined_helper_is_recognised():
    """Closure's tests assert through assertPrint, not assertEquals — 14 of the
    228 recorded cases. Same expected-value-first convention."""
    assert EAL('assertPrint("x- -0.0;", parsed);') == ['x- -0.0;']


def test_arbitrary_assert_prefixed_helper_is_recognised():
    assert EAL('assertNodeEquality("the-expected-tree", built);') == \
        ['the-expected-tree']


def test_check_and_verify_prefixed_helpers_are_recognised():
    src = ('checkResult("expected-alpha", got);\n'
           'verifyOutput("expected-beta", other);')
    assert EAL(src) == ['expected-alpha', 'expected-beta']


def test_single_argument_asserts_still_yield_nothing():
    """assertTrue/assertNull pin no expected value at all — 69 of the 204
    misses. No extractor change can help these; they need a different
    mechanism, and must not be faked into producing one."""
    assert EAL('assertTrue(builder.contains(c)); assertNull(result);') == []


def test_a_helper_call_is_not_double_counted_with_the_junit_pass():
    """assertEquals matches BOTH regexes; the literal must appear once."""
    assert EAL('assertEquals("expected-name", actual);') == ['expected-name']


def test_mixed_junit_and_project_helpers_both_contribute():
    src = ('assertEquals("from-junit", a);\n'
           'assertPrint("from-helper", b);')
    assert EAL(src) == ['from-junit', 'from-helper']


def test_non_assertion_calls_are_not_harvested():
    """A method merely starting with a matched prefix but taking no
    expected/actual pair must not contribute."""
    assert EAL('expectation.record(x);') == []


# --- the recorded negative result -----------------------------------------

def test_non_numeric_comparison_is_deliberately_not_shipped():
    """Item 2a fix (ii) was measured and NOT shipped: it produces exactly one
    "must be dismissed" instruction over the 228 recorded cases, and that one is
    wrong (the case's gold label says the finding was legitimate). The 2a
    write-up licensed it with the sign inverted — gold=SOUND means KEEP.

    This pins the numeric-only behaviour so the rejected fix cannot reappear
    without someone re-running the measurement in the module note."""
    from java.relations.evidence_facts import fired_value_vs_trusted
    # Pinned value is a bare token; the fired message contains it verbatim.
    # A token-aware comparison would say "matches" (and license a dismissal).
    assert fired_value_vs_trusted(
        'relation shortClassName_agree violated: got ClassUtils',
        ['ClassUtils', 'Map.Entry']) == 'unknown'


# --- literal-concatenation folding (cycle-7 smoke finding) ----------------

def test_a_chain_of_string_literals_is_folded():
    """Closure-62's shape. A chain of string literals is a COMPILE-TIME CONSTANT
    in Java, so folding it is a correctness fix, not a loosening.

    The batch's stated precision claim was aimed at Closure-62 and was
    structurally unreachable without this: its expected value is a multi-line
    concatenation, so the extractor skipped it and the dismissal rule aimed at
    that leg could never fire. Only a live smoke surfaced it."""
    src = ('assertEquals("first line\\n" +\n'
           '    "second line\\n" +\n'
           '    "third\\n", actual);')
    got = EAL(src)
    assert len(got) == 1
    assert got[0] == 'first line\\nsecond line\\nthird\\n'


def test_concatenation_inherits_the_known_message_first_limitation():
    """NOT a new defect — the same limitation pinned above. When both the
    message and the expected value are strings, the message-first heuristic
    cannot tell them apart and takes the MESSAGE, concatenation or not.

    Pinned so the behaviour is known rather than assumed. Fixing it changes
    existing behaviour and needs its own measurement over the 228 records."""
    src = 'assertEquals("a message", "exp-part-one" + "-part-two", actual);'
    assert EAL(src) == ['a message']


def test_concatenation_folds_in_the_two_argument_form():
    """The form that actually matters — Closure-62's — has no message argument."""
    src = 'assertEquals("exp-part-one" + "-part-two", actual);'
    assert EAL(src) == ['exp-part-one-part-two']


def test_a_computed_operand_is_still_skipped():
    """The deliberate skip on genuinely computed expected values must survive —
    folding applies ONLY when every operand is a plain string literal."""
    assert EAL('assertEquals(compute(x) + "suffix", actual);') == []
    assert EAL('assertEquals("prefix" + variable, actual);') == []
    assert EAL('assertEquals("prefix" + 42, actual);') == []


def test_a_single_literal_is_not_treated_as_a_concatenation():
    assert EAL('assertEquals("just-one", actual);') == ['just-one']


def test_folding_preserves_escapes_verbatim():
    """The folded value is compared against fired messages by literal presence,
    so it must not be normalised."""
    got = EAL(r'assertEquals("a\tb" + "\nc", actual);')
    assert got == [r'a\tb\nc']
