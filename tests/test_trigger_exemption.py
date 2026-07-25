"""Spec J: mechanical flags for the re-armed identical-drop trigger-input exemption.

Covers the two pure helpers pinned in the cycle-3 plan:
  * expected_is_test_literal(fired_msg, trusted_values) -> bool
      The fired message's EXPECTED-side value matches one of the failing
      test's own assert literals, under the distinctiveness rule (numeric
      match needs >=4 significant digits; string match needs >=8 chars after
      whitespace normalization).
  * fired_at_test_input(fired_msg, trigger_literals) -> bool
      Any DISTINCTIVE trigger seed literal (same rule) appears among the
      fired message's key=value values.

Fixtures are real firing strings pulled from archived pool30 traces (see
tests/fixtures/trigger_exemption.json for per-case provenance); two synthetic
cases prove the distinctiveness guard fires the other way and that bare
literals never qualify.

The module under test may not exist yet while a parallel agent implements the
helpers; in that case these tests fail with a collection/import error only,
which is the expected transient state per the cycle plan.
"""
import json
import os

import pytest

from java.relations import evidence_facts

_FIX = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures")


def _load(name):
    with open(os.path.join(_FIX, name + ".json"), "r", encoding="utf-8") as fh:
        return json.load(fh)


_TE = _load("trigger_exemption")


# --------------------------------------------------------------------------
# Case 1 -- setup-divergence: the seed-test copy re-asserts the test's own
# pinned expected string. From pool30 Closure-62-c (trace line 3170); the
# expected side, after whitespace normalization, equals the test's assertEquals
# literal exactly. String >= 8 chars => distinctive => expected_is_test_literal
# is True (the decision ladder then mechanically dismisses because the real
# failing test passes on the patched build).
# --------------------------------------------------------------------------
def test_setup_divergence_expected_matches_test_literal():
    case = _TE["setup_divergence"]
    assert evidence_facts.expected_is_test_literal(
        case["fired_msg"], case["trusted_values"]) is True


def test_setup_divergence_requires_the_matching_literal():
    # Distinctiveness alone is not enough: with an unrelated (but long) trusted
    # value the expected side does NOT match, so the flag must be False.
    case = _TE["setup_divergence"]
    assert evidence_facts.expected_is_test_literal(
        case["fired_msg"],
        ["totally different long expected string that is not the test literal"],
    ) is False


# --------------------------------------------------------------------------
# Case 2 -- root defect (patch failed to fix). From pool30 Math-2-o (trace line
# 3084): "[oracle:mean-within-support] ... mean=-49.759350398538686 lower=0
# upper=50". Recorded facts (see fixture note):
#   (a) the message has NO expected= tag and its bound values 0/50 are
#       non-distinctive (<4 sig digits) => expected_is_test_literal is False.
#   (b) the failing test's distinctive literals (43130568, 42976365) do NOT
#       appear among the message values, and -49.759... does not accidentally
#       match, so fired_at_test_input on the REAL Math-2 message is ALSO False.
# The positive direction of the distinctiveness guard is proven by the
# synthetic root_defect_synthetic case below.
# --------------------------------------------------------------------------
def test_root_defect_expected_is_not_a_test_literal():
    case = _TE["root_defect"]
    # bound values 0/50 are non-distinctive; nothing on the expected side
    # qualifies as one of the test's assert literals.
    assert evidence_facts.expected_is_test_literal(
        case["fired_msg"], case["trusted_values"]) is False


def test_root_defect_real_message_not_at_distinctive_test_input():
    # Math-2's distinctive test literals fall OUTSIDE the message values, so the
    # real firing is NOT flagged as fired-at-test-input. (Comment of record:
    # this is the "distinctive literals absent" direction of the guard.)
    case = _TE["root_defect"]
    assert evidence_facts.fired_at_test_input(
        case["fired_msg"], case["trigger_literals"]) is False


def test_root_defect_observed_value_does_not_accidentally_match():
    # Guard against the -49.759... observed value being mistaken for a test
    # literal: even if we (wrongly) treat the observed value as a trigger
    # literal, it is not among the failing test's distinctive literals, so
    # restricting to the real distinctive literals keeps the flag False.
    case = _TE["root_defect"]
    assert case["observed_value"] not in case["distinctive_test_literals"]
    assert evidence_facts.fired_at_test_input(
        case["fired_msg"], case["distinctive_test_literals"]) is False


def test_root_defect_synthetic_distinctive_literal_fires():
    # Complementary direction: a distinctive literal (46341.0, 5 sig digits)
    # present in BOTH the trigger literals AND the message values must set the
    # fired-at-test-input flag True.
    case = _TE["root_defect_synthetic"]
    assert evidence_facts.fired_at_test_input(
        case["fired_msg"], case["trigger_literals"]) is True


# --------------------------------------------------------------------------
# Case 3 -- trivial-literal guards. Bare 50/0/1.0 (all <4 significant digits)
# must never qualify for either helper; a string shorter than 8 chars must
# never qualify for expected_is_test_literal.
# --------------------------------------------------------------------------
def test_trivial_numeric_never_qualifies_expected():
    g = _TE["trivial_guards"]
    assert evidence_facts.expected_is_test_literal(
        g["numeric_expected_msg"], g["numeric_trusted_values"]) is False


def test_trivial_numeric_never_qualifies_trigger():
    g = _TE["trivial_guards"]
    assert evidence_facts.fired_at_test_input(
        g["numeric_trigger_msg"], g["numeric_trigger_literals"]) is False


def test_short_string_never_qualifies_expected():
    g = _TE["trivial_guards"]
    assert evidence_facts.expected_is_test_literal(
        g["short_string_expected_msg"], g["short_string_trusted_values"]) is False


@pytest.mark.parametrize("bad", [0, 1, 50, "1.0"])
def test_trivial_scalar_matches_do_not_qualify_either_helper(bad):
    # Direct, fixture-free assertion that a bare 0/1/50/1.0 appearing on both
    # sides is never enough to trip either flag.
    msg = "[oracle:x] semantic mismatch: expected=%s actual=%s" % (bad, bad)
    assert evidence_facts.expected_is_test_literal(msg, [str(bad)]) is False
    assert evidence_facts.fired_at_test_input(msg, [str(bad)]) is False
