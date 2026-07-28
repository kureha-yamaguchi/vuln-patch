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


def test_veto_does_not_block_the_rate_path():
    # A denial of the TEXTUAL claim must not suppress an independent measured
    # fires-on-both rate fact in the same evidence blob.
    blob = PARTIAL_FIX + "\n[fire-rate fact] buggy build 19000/20000 = 95%; " \
                         "patched build 9000/20000 = 45% of random valid inputs."
    assert terminal_profile(blob) == 'fires-on-both-rate'
