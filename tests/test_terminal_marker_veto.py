"""Regression: the 5C terminal detector must not fire on notes that DENY the
identical-value claim.

Iteration-1 lead (docs/replay/v5d_iter1_analysis.md addendum): the marker set
contained the bare substring 'on both builds', which matched two note families
whose whole point is the opposite of terminal —

  * the partial-fix note: "…fires on BOTH builds but with DIFFERENT observed
    values … this firing remains evidence AGAINST the patch" (a CONVICTION);
  * the unknown note: "…fires on both builds (observed values were not
    compared, so no identical-value claim is made)" (no fact either way).

Both were being treated as terminal, so the gate voided keeps on catches whose
own evidence convicted the patch. This was live in production, not only in the
offline replay.
"""
from java.relations.evidence_facts import (
    carries_terminal_identical_fact,
    terminal_profile,
)

PARTIAL_FIX = (
    "[buggy-replay fact] the SAME check fires on BOTH builds but with "
    "DIFFERENT observed values (buggy: actual=08 vs patched: actual=09) — the "
    "patch changed behaviour at this input without restoring the expected "
    "value: the partial-fix pattern; this firing remains evidence against the "
    "patch."
)
UNKNOWN = (
    "[muted-replay fact] with the shadowing check(s) silenced, THIS check "
    "fires on the BUGGY build at this exact input — the same check fires on "
    "both builds (observed values were not compared, so no identical-value "
    "claim is made); judge the check's soundness on the shown contract."
)
TRUE_IDENTICAL = (
    "[buggy-replay fact] the exact firing input fires the SAME check on the "
    "BUGGY build with the SAME observed values — behaviour at this input is "
    "identical on both builds; the patch did not cause this."
)


def test_partial_fix_conviction_is_not_terminal():
    assert carries_terminal_identical_fact(PARTIAL_FIX) is False
    assert terminal_profile(PARTIAL_FIX) is None


def test_values_not_compared_is_not_terminal():
    assert carries_terminal_identical_fact(UNKNOWN) is False
    assert terminal_profile(UNKNOWN) is None


def test_genuine_identical_still_terminal():
    assert carries_terminal_identical_fact(TRUE_IDENTICAL) is True
    assert terminal_profile(TRUE_IDENTICAL) == 'identical-on-both'


def test_buggy_scan_fact_still_terminal():
    note = ("[buggy-scan fact] the acceptance scan recorded this oracle "
            "firing on the buggy build")
    assert carries_terminal_identical_fact(note) is True


def test_rate_path_is_reverted_not_terminal():
    # REVERTED 2026-07-28: the rate-based terminal path is no longer consulted
    # (iteration-2 evidence: it dropped 4 confirmed catches and gained ~0
    # leaks — the rates live in the inventory, not in delivered evidence).
    # A blob carrying ONLY a fire-rate fact must now be non-terminal.
    blob = ("[fire-rate fact] buggy build 19000/20000 = 95%; patched build "
            "9000/20000 = 45% of random valid inputs.")
    assert terminal_profile(blob) is None
    # ...and a denial of the textual claim stays non-terminal too.
    assert terminal_profile(PARTIAL_FIX + "\n" + blob) is None


# --- 5B citation detector: negated citations are not citations -------------
# Iteration 2 (2026-07-28) found 5B never firing on its own targets: the
# citation matcher counted 'document' INSIDE "undocumented", and counted
# "not contradicted by any shown contract or trusted test" as a citation.
# Same failure shape as the terminal-marker bug above.
from java.relations.evidence_facts import verdict_needs_citation

_SIG = {'buggy_silent': True, 'deterministic_trigger': True,
        'patched_firing': True}

ROW21 = (
    "The fired check asserts an undocumented exact-printing contract for "
    "`x-0`, but a correct `CodePrinter` could legitimately choose a "
    "conservative compact form like `x- 0` after binary minus, and the "
    "trusted test only requires the space for negative zero, not for "
    "positive zero."
)


def test_undocumented_is_not_a_citation():
    assert verdict_needs_citation(_SIG, ROW21) is True


def test_no_shown_contract_is_not_a_citation():
    why = ("A correct printer could emit a separator; this is not "
           "contradicted by any shown contract or trusted test.")
    assert verdict_needs_citation(_SIG, why) is True


def test_genuine_citations_still_stand():
    # The two signature-complete gold=DISMISS guards must NOT be voided.
    lang50 = ("check requires la != lb — Locale object identity; "
              "equal-but-distinct instances legal (real check bug)")
    math74 = ("fp time-reversibility is only accurate to 8.7e-7 for this "
              "adaptive method; the check demands bit-exact equality")
    javadoc = ("the javadoc documents this method may return any value in "
               "the range, so the check demands more than the contract")
    for why in (lang50, math74, javadoc):
        assert verdict_needs_citation(_SIG, why) is False


def test_negation_strip_cannot_swallow_a_later_clause():
    # A negated span is clause-bounded: a genuine citation after the comma
    # must still register (so the dismissal stands).
    why = ("this is an undocumented contract, but the javadoc specifies the "
           "tolerance is 1e-6 and the check demands exact equality")
    assert verdict_needs_citation(_SIG, why) is False


def test_without_the_signature_nothing_is_voided():
    assert verdict_needs_citation({}, ROW21) is False
