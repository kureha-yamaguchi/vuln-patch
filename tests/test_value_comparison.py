"""Spec I (cycle-2b hotfix): "identical" requires a VALUE comparison.

The same-check buggy-replay fact and the muted-replay fires-on-both fact used to
claim "behaviour at this input is identical on both builds" knowing only that
the SAME check FIRED on both builds. Firing on both != identical values: a
partially-unfixed overfit fires the same check on both builds with DIFFERENT
observed values (both wrong, differently). With the MECHANICAL-FACTS rule
binding, that over-claim mechanically dismissed a genuine catch (c2flag
Math-68). These tests pin the new contract:

    evidence_facts.compare_fired_values(patched_msg, buggy_msg)
        -> "identical" | "different" | "unknown"

and the three-way wording of semantic_buggy_replay_note (same-check branch) and
muted_replay_note (target-fires branch).

Fixture provenance is REAL trace strings from the c2flag validation run
(runs-archive/runs/c2flag_20260724_095810/); where the buggy-side value is not
recoverable as "different" from the trace it is synthesized and marked.
"""
import pytest

from java.relations import evidence_facts


# ---------------------------------------------------------------------------
# Math-68 "different" pair (partial-fix pattern).
#
# Provenance: c2flag run 04_patch1-Math-68-Arja-plausible_o/trace.md.
#   * PATCHED firing (REAL, trace line 6625 / 2843):
#       "[oracle:fr-1-rms] semantic mismatch: actual=4.948952097518721
#        expected=6.99887517584575"
#   * The BUGGY replay at the c2flag zero/empty input (trace line 6906)
#     coincidentally emitted the byte-for-byte IDENTICAL message
#     (actual=4.948952097518721), so the real trace pair is NOT itself a
#     "different" example. To exercise Spec I's partial-fix path — the SAME
#     check firing on BOTH builds with DIFFERENT observed values — the buggy
#     message below is SYNTHESIZED with a plausibly-different converged value.
#
# Note on extraction: _fired_numbers reads BOTH the tagged actual=... and the
# bare trailing number (here the shared "expected=" reference). For the verdict
# to be "different" the synthetic buggy message must therefore share NO
# extractable number with the patched one, so its observed value AND the
# reference it prints both differ from the patched firing's 4.9489 / 6.9989.
_M68_PATCHED = ("[oracle:fr-1-rms] semantic mismatch: "
                "actual=4.948952097518721 expected=6.99887517584575")
_M68_BUGGY_DIFF = ("[oracle:fr-1-rms] semantic mismatch: "
                   "actual=5.472103886640215 expected=6.883014771590284")


# ---------------------------------------------------------------------------
# Math-30 NaN pair (identical). Provenance: Math-30 midpoint p-value check —
# both builds report the same "got NaN" message at this input. No numeric is
# extractable (NaN is not matched by the number regex), so identity rests on
# the textual-identity rule.
_M30_PATCHED = ("[oracle:midpoint-pvalue] expected midpoint asymptotic "
                "p-value 1.0 but got NaN")
_M30_BUGGY_SAME = ("[oracle:midpoint-pvalue] expected midpoint asymptotic "
                   "p-value 1.0 but got NaN")


# ---------------------------------------------------------------------------
# Non-numeric pair (unknown): two exception-only messages, no observed value.
_NONNUM_PATCHED = "[oracle:npe-guard] NullPointerException at Foo.bar"
_NONNUM_BUGGY = "[oracle:npe-guard] IllegalStateException at Baz.qux"


# ===========================================================================
# compare_fired_values
# ===========================================================================

def test_math68_pair_is_different():
    assert evidence_facts.compare_fired_values(
        _M68_PATCHED, _M68_BUGGY_DIFF) == "different"


def test_math30_nan_pair_is_identical():
    # Textually identical NaN messages -> identical even with no extractable
    # number (NaN == NaN is honoured).
    assert evidence_facts.compare_fired_values(
        _M30_PATCHED, _M30_BUGGY_SAME) == "identical"


def test_nonnumeric_pair_is_unknown():
    assert evidence_facts.compare_fired_values(
        _NONNUM_PATCHED, _NONNUM_BUGGY) == "unknown"


def test_matching_value_within_floor_is_identical():
    # Same oracle at the same input carries the SAME expected= reference
    # (expected derives from the input/constants); only the observed actual=
    # jitters within the 1e-9 floor -> identical.
    a = "[oracle:x] actual=4.948952097518721 expected=6.99887517584575"
    b = "[oracle:x] actual=4.948952097518800 expected=6.99887517584575"
    assert evidence_facts.compare_fired_values(a, b) == "identical"


# ===========================================================================
# semantic_buggy_replay_note — same-check/no-defect branch, three-way
# ===========================================================================

def _same_check_note(value_verdict, buggy=None, patched=None):
    return evidence_facts.semantic_buggy_replay_note(
        fired_ids={"fr-1-rms"},
        breplay_status="crashed",
        breplay_ids={"fr-1-rms"},
        bt_all=set(),
        bt_defect=set(),
        esc_type=None,
        idline="",
        value_verdict=value_verdict,
        buggy_msg_excerpt=buggy,
        patched_msg_excerpt=patched,
    )


def test_same_check_different_uses_partial_fix_wording():
    note = _same_check_note(
        "different", buggy=_M68_BUGGY_DIFF, patched=_M68_PATCHED)
    assert note is not None
    assert "DIFFERENT observed values" in note
    assert "partial-fix pattern" in note
    assert "remains evidence" in note
    # The over-claim that produced the Math-68 FN must be gone.
    assert "identical on both builds" not in note


def test_same_check_identical_keeps_current_wording():
    note = _same_check_note("identical")
    assert note is not None
    assert "identical on both builds" in note
    assert "the patch did not cause" in note


def test_same_check_unknown_drops_identical_keeps_guidance():
    note = _same_check_note("unknown")
    assert note is not None
    # No over-claim without a value comparison.
    assert "identical on both builds" not in note
    # Still reports the mechanical fires-on-both fact.
    assert "SAME check on the BUGGY build" in note
    # Keep/dismiss guidance preserved.
    assert "dismiss" in note.lower()
    assert "keep this finding only" in note.lower()


# ===========================================================================
# muted_replay_note — target-fires branch DEFAULT must not over-claim
# ===========================================================================

def test_muted_default_makes_no_identical_claim():
    # No value_verdict threaded -> the unthreaded call can NEVER over-claim
    # identical (Spec I requirement 3 / task note 3).
    note = evidence_facts.muted_replay_note(
        target_ids={"fr-1-rms"},
        muted_ids={"shadow-check"},
        status="crashed",
        fired_ids={"fr-1-rms"},
        esc_type=None,
        bt_all=set(),
    )
    assert note is not None
    assert "identical on both builds" not in note.lower()


def test_muted_different_uses_partial_fix_wording():
    note = evidence_facts.muted_replay_note(
        target_ids={"fr-1-rms"},
        muted_ids={"shadow-check"},
        status="crashed",
        fired_ids={"fr-1-rms"},
        esc_type=None,
        bt_all=set(),
        value_verdict="different",
        buggy_msg_excerpt=_M68_BUGGY_DIFF,
        patched_msg_excerpt=_M68_PATCHED,
    )
    assert note is not None
    assert "DIFFERENT observed values" in note
    assert "partial-fix pattern" in note
    assert "remains evidence" in note
    assert "identical on both builds" not in note


def test_muted_identical_earns_identical_wording():
    note = evidence_facts.muted_replay_note(
        target_ids={"fr-1-rms"},
        muted_ids={"shadow-check"},
        status="crashed",
        fired_ids={"fr-1-rms"},
        esc_type=None,
        bt_all=set(),
        value_verdict="identical",
    )
    assert note is not None
    assert "identical on both builds" in note.lower()


def test_shared_expected_reference_does_not_fake_identity():
    """The regression the first Spec-I implementation would have shipped:
    both builds' messages share the same expected= reference literal but the
    OBSERVED actual= values differ (real Math-68 fr-1-rms shape). Comparing
    all extracted numbers would match on the shared 6.99887... and call this
    "identical", re-killing a partial-fix catch. Observed-vs-observed must
    say "different"."""
    from java.relations.evidence_facts import compare_fired_values
    patched = ("[oracle:fr-1-rms] semantic mismatch: "
               "actual=4.948952097518721 expected=6.99887517584575")
    buggy = ("[oracle:fr-1-rms] semantic mismatch: "
             "actual=4.601233010847492 expected=6.99887517584575")
    assert compare_fired_values(patched, buggy) == "different"


def test_kv_pairwise_nan_observed_identical():
    """Real c2b Math-30 asymptotic-formula shape (trace line 2416): the
    observed value prints as p=NaN (no actual= tag), so tagged extraction
    fails — the key=value pairwise ladder must certify identity when every
    shared key (p, expected, tol, n, u) matches NaN-safely."""
    from java.relations.evidence_facts import compare_fired_values
    patched = ("[oracle:asymptotic-formula] consistency violation: p-value "
               "disagrees with independent recomputation; n=46341 "
               "u=3.221204618E9 p=NaN expected=0.0 tol=NaN")
    buggy = ("[oracle:asymptotic-formula] consistency violation: p-value "
             "disagrees with independent recomputation; n=46341 "
             "u=3.221204618E9 p=NaN expected=0.0 tol=NaN")
    assert compare_fired_values(patched, buggy) == "identical"


def test_kv_pairwise_differing_observed_key_is_different():
    """Same format, one shared observed key differs -> different (the
    partial-fix keep path), even though expected= and n= match."""
    from java.relations.evidence_facts import compare_fired_values
    patched = ("[oracle:u-statistic] semantic mismatch: n=46340 "
               "expectedU=1.07367463E9 actualU=1.07372097E9")
    buggy = ("[oracle:u-statistic] semantic mismatch: n=46340 "
             "expectedU=1.07367463E9 actualU=1.07369999E9")
    assert compare_fired_values(patched, buggy) == "different"


def test_kv_pairwise_infinity_token():
    from java.relations.evidence_facts import compare_fired_values
    assert compare_fired_values("got x=Infinity n=5",
                                "got x=Infinity n=5") == "identical"
    assert compare_fired_values("got x=Infinity n=5",
                                "got x=-Infinity n=5") == "different"
