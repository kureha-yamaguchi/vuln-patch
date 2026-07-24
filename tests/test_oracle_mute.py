"""Spec G-G1: tests for the pure oracle-silencing source transform.

Pinned API (module implemented by a parallel agent; import failure while it is
not yet landed is the expected transient state):

    java.execution.oracle_mute.mute_oracles(java_source, mute_ids=None,
                                            mute_all=False) -> str

Assertions are on STRUCTURE and SUBSTRINGS, not exact wording -- the muted
comment text may be polished. Real harness sources are loaded from
tests/fixtures/harness_sources.json (exact copies of archived trace <harness>
blocks; provenance recorded there).
"""
import json
import os

import pytest

from java.execution import oracle_mute

_FIX = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures")


def _load_harnesses():
    with open(os.path.join(_FIX, "harness_sources.json"), "r", encoding="utf-8") as fh:
        return json.load(fh)


_H = _load_harnesses()
_MATH30 = _H["math30_multi_oracle"]["source"]
_CLOSURE70 = _H["closure70_oracle_and_escape"]["source"]


# A synthetic harness: two [oracle:] throws (one with a multi-line, '+'-
# concatenated message) plus a plain input-rejection throw that carries NO
# oracle tag and must never be touched.
_SYNTH = """\
package demo;

import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int n = data.consumeInt(0, 10);
        if (n < 0) {
            throw new IllegalArgumentException("bad");
        }

        double a = compute(n);
        if (!(a > 0.1)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:positive-result] semantic mismatch: expected > 0.1 but got "
                            + a + " for n=" + n);
        }

        double b = other(n);
        if (b != a) {
            throw new FuzzerSecurityIssueLow("[oracle:agreement] mismatch: a=" + a + " b=" + b);
        }
    }
}
"""


def _balanced(s):
    return s.count("{") == s.count("}") and s.count("(") == s.count(")")


# --------------------------------------------------------------------------
# Synthetic source: precise structural behaviour
# --------------------------------------------------------------------------

def test_synthetic_mute_one_id_removes_only_that_throw():
    out = oracle_mute.mute_oracles(_SYNTH, mute_ids={"positive-result"})

    # The targeted oracle's throw statement is gone ...
    assert "throw new FuzzerSecurityIssueLow(\n" not in out or \
        "[oracle:positive-result]" not in out
    assert "[oracle:positive-result]" not in out
    # ... replaced by a muted marker.
    assert "muted" in out and "positive-result" in out

    # Sibling oracle throw is intact.
    assert "[oracle:agreement]" in out
    assert 'throw new FuzzerSecurityIssueLow("[oracle:agreement]' in out

    # Plain, un-tagged input-rejection throw is intact.
    assert 'throw new IllegalArgumentException("bad")' in out

    # Structural sanity: braces/parens still balanced.
    assert _balanced(out)
    assert _balanced(_SYNTH)  # fixture itself is well-formed


def test_synthetic_mute_all_removes_both_oracle_throws_keeps_plain():
    out = oracle_mute.mute_oracles(_SYNTH, mute_all=True)

    assert "[oracle:positive-result]" not in out
    assert "[oracle:agreement]" not in out
    # No FuzzerSecurityIssueLow throw statements remain.
    assert "throw new FuzzerSecurityIssueLow" not in out

    # Plain input-rejection throw untouched.
    assert 'throw new IllegalArgumentException("bad")' in out

    assert _balanced(out)


def test_synthetic_multiline_message_fully_consumed():
    """The multi-line '+'-concatenated oracle message must be swallowed whole --
    no dangling ' + a + ...' fragment left behind."""
    out = oracle_mute.mute_oracles(_SYNTH, mute_ids={"positive-result"})
    assert 'expected > 0.1 but got' not in out
    assert '+ a + " for n=" + n' not in out
    assert _balanced(out)


def test_synthetic_mute_unknown_id_is_noop_on_throws():
    out = oracle_mute.mute_oracles(_SYNTH, mute_ids={"does-not-exist"})
    assert "[oracle:positive-result]" in out
    assert "[oracle:agreement]" in out
    assert 'throw new IllegalArgumentException("bad")' in out


# --------------------------------------------------------------------------
# Real Math-30 multi-oracle harness
# --------------------------------------------------------------------------

def test_math30_mute_one_real_oracle():
    out = oracle_mute.mute_oracles(_MATH30, mute_ids={"midpoint-pvalue"})

    # Output actually changed.
    assert out != _MATH30

    # The muted id's throw is gone.
    assert "[oracle:midpoint-pvalue]" not in out

    # At least one sibling oracle id still present.
    assert "[oracle:swap-symmetry]" in out
    assert "[oracle:u-sum]" in out
    assert "[oracle:lifted-big-dataset]" in out

    # Non-alarm code (method signature, class shell) preserved.
    assert "public static void fuzzerTestOneInput" in out
    assert "MannWhitneyUTest testStatistic = new MannWhitneyUTest();" in out

    assert _balanced(out)


def test_math30_mute_all_clears_every_oracle():
    out = oracle_mute.mute_oracles(_MATH30, mute_all=True)
    for oid in ("lifted-big-dataset", "midpoint-pvalue", "swap-symmetry", "u-sum"):
        assert "[oracle:%s]" % oid not in out
    assert "throw new FuzzerSecurityIssueLow" not in out
    # Body computation still present.
    assert "public static void fuzzerTestOneInput" in out
    assert _balanced(out)


# --------------------------------------------------------------------------
# Real Closure-70 harness (oracle throws + escaped-ISE second call)
# --------------------------------------------------------------------------

def test_closure70_mute_all_clears_alarms_keeps_process_call():
    out = oracle_mute.mute_oracles(_CLOSURE70, mute_all=True)

    # No FuzzerSecurityIssue throw statements remain.
    assert "FuzzerSecurityIssue" not in out or "throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow" not in out
    assert "throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow" not in out

    # The processForTesting call chain (both sites, incl. the escaped-crash
    # replay site) is untouched.
    assert out.count("processForTesting(null, n") == 2
    assert ".processForTesting(null, n)" in out

    assert _balanced(out)


def test_closure70_mute_all_preserves_non_alarm_rethrows():
    """mute_all silences alarm throws and 'relation ... violated' RuntimeExceptions
    only; plain input/parse-error RuntimeExceptions are NOT alarms and stay."""
    out = oracle_mute.mute_oracles(_CLOSURE70, mute_all=True)
    assert 'throw new RuntimeException("parse failed:' in out
    assert 'throw new RuntimeException("unexpected errors after typecheck:' in out


# --------------------------------------------------------------------------
# Escaped-quote safety: an oracle message literal containing an escaped quote
# --------------------------------------------------------------------------

_ESCAPED_QUOTE = r'''package demo;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int n = data.consumeInt(0, 10);
        if (n == 7) {
            throw new FuzzerSecurityIssueLow("[oracle:quote-in-msg] mismatch: expected \"ok\" but got other n=" + n);
        }
        keepGoing(n);
    }
}
'''


def test_escaped_quote_in_message_is_handled():
    out = oracle_mute.mute_oracles(_ESCAPED_QUOTE, mute_ids={"quote-in-msg"})
    # The whole throw (including the literal with the embedded \" ) is removed.
    assert "[oracle:quote-in-msg]" not in out
    assert 'throw new FuzzerSecurityIssueLow' not in out
    # Following statement survived -- the scan did not run away past the ';'.
    assert "keepGoing(n);" in out
    assert _balanced(out)
