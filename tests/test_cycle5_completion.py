"""Cycle-5D: the 5B semantics completion and the rate-based 5C extension.

Two changes, both pure/offline (a stubbed verifier, no JVM, no LLM, no tokens):

  5B completion — under the FULL drift-kill signature, a dismissal that is
  citation-void TWICE (original AND re-ask) is inadmissible, so the finding is
  KEPT with an explicit "5B: dismissal inadmissible" why. Off the full
  signature, and on the pin-void path, nothing changes.

  5C/5D rate extension — the terminal fires-on-both fact may arrive MEASURED,
  as a [fire-rate fact] with a genuinely high buggy-side rate, instead of as
  the textual "identical on both builds" marker. Same terminal treatment, same
  family-duty escape. The 5A asymmetric CATCH profile (buggy LOW / patched
  high) must never be terminal.
"""
import pytest

from java.relations import evidence_facts as ef
from java.relations.judge_decision import (
    _guarded_verify, _terminal_identical_gate, adjudicate)


# The full drift-kill signature (all three profile flags).
_DRIFT = {'buggy_silent': True, 'deterministic_trigger': True,
          'patched_firing': True}
# ...and a signature-INCOMPLETE profile (one flag missing).
_PARTIAL = {'buggy_silent': False, 'deterministic_trigger': True,
            'patched_firing': True}

_UNCITED = "a correct printer could emit the optional separator `x- 1`"
_UNCITED_2 = "a correct implementation might legitimately choose that form"


class _StubVerifier:
    """Scripted verify()/family_duty() results; counts calls. No network."""

    def __init__(self, verify_results, fd_results=None):
        self._vr = list(verify_results)
        self._fd = list(fd_results or [])
        self.verify_calls = 0
        self.fd_calls = 0

    def verify(self, **kwargs):
        r = self._vr[min(self.verify_calls, len(self._vr) - 1)]
        self.verify_calls += 1
        return r

    def family_duty(self, *a, **k):
        r = self._fd[min(self.fd_calls, len(self._fd) - 1)]
        self.fd_calls += 1
        return r


def _adjudicate(v, *, evidence='', profile=None, fd_prior=None):
    return adjudicate(
        v, harness_source="src", fired_assertion="fired",
        trusted_values=None, concrete_evidence=evidence, code_context=None,
        pinned_source="raw source string (no pin dict)",
        evidence_profile=profile, failing_block="block", check_source="src",
        fd_prior=fd_prior)


# ---------------------------------------------------------------------------
# (a) 5B: signature-complete + TWICE-uncited dismissal -> KEEP
# ---------------------------------------------------------------------------
def test_5b_twice_uncited_under_signature_keeps():
    v = _StubVerifier([(False, _UNCITED), (False, _UNCITED_2)])
    ok, why = _adjudicate(v, profile=_DRIFT)
    assert ok is True
    assert v.verify_calls == 2
    assert "5B-INADMISSIBLE" in why
    assert "dismissal inadmissible" in why
    assert "twice uncited under drift-kill signature" in why
    # the re-asked text is carried through for the trace
    assert _UNCITED_2 in why


def test_5b_twice_uncited_flip_is_greppable_in_helper_too():
    v = _StubVerifier([(False, _UNCITED), (False, _UNCITED_2)])
    ok, why = _guarded_verify(v, dict(harness_source="s",
                                      concrete_evidence="e"),
                              pinned=None, evidence_profile=_DRIFT)
    assert ok is True and why.startswith("[5B-INADMISSIBLE keep]")


# ---------------------------------------------------------------------------
# (b) 5B guard: signature-complete + re-ask returns a CITED dismissal -> dead
# ---------------------------------------------------------------------------
@pytest.mark.parametrize("cited", [
    # Lang-50 shape: a demonstrable `!=` / object-identity check bug.
    "check requires `la != lb` — Locale object identity; equal-but-distinct "
    "instances are legal, a real check bug",
    # Math-74 shape: a rounding / tolerance floor.
    "the solver is approximate only; the 8.7e-7 gap is within its legitimate "
    "error and the check may not demand more",
    "default accuracy is 1e-6, so the check's exactness is not guaranteed",
    "fp round-off: time-reversibility is not guaranteed for an adaptive method",
    # documented contract
    "the documented javadoc contract permits either order",
])
def test_5b_cited_reask_dismissal_stands(cited):
    v = _StubVerifier([(False, _UNCITED), (False, cited)])
    ok, why = _adjudicate(v, profile=_DRIFT)
    assert ok is False
    assert "citation-void re-ask" in why
    assert "INADMISSIBLE" not in why


@pytest.mark.parametrize("cited", [
    "check requires `la != lb` — Locale object identity; equal-but-distinct "
    "instances are legal, a real check bug",
    "the solver is approximate only; the 8.7e-7 gap is within its legitimate "
    "error and the check may not demand more",
    "too-tight accuracy demanded; the same deviation is legal here",
    "bit-exact == on n*m/N; a different evaluation order is legal",
])
def test_5b_cited_first_verdict_never_reasked(cited):
    """The guard cases are not even VOID: no re-ask, the drop stands."""
    assert ef.verdict_needs_citation(_DRIFT, cited) is False
    v = _StubVerifier([(False, cited)])
    ok, why = _adjudicate(v, profile=_DRIFT)
    assert ok is False and v.verify_calls == 1 and why == cited


def test_5b_uncited_closure38_shapes_are_still_void():
    """The rule's target class must stay void (the citation set did not grow
    so broad that the format-drift hypotheticals now read as cited)."""
    for why in ("a correct printer could emit x-0.0 vs the hard-coded x-0",
                '"compact" doesn\'t forbid the optional separator `x- 1`',
                "the printer could legally emit `1- 2` instead",
                "contains could legally call minimizeCapacity first"):
        assert ef.verdict_needs_citation(_DRIFT, why) is True


# ---------------------------------------------------------------------------
# (c) 5B scope: uncited dismissal WITHOUT the full signature -> unchanged
# ---------------------------------------------------------------------------
def test_5b_uncited_without_full_signature_stays_dropped():
    v = _StubVerifier([(False, _UNCITED), (False, _UNCITED_2)])
    ok, why = _adjudicate(v, profile=_PARTIAL)
    assert ok is False
    assert v.verify_calls == 1          # no void, so no re-ask at all
    assert why == _UNCITED


def test_5b_no_profile_stays_dropped():
    v = _StubVerifier([(False, _UNCITED), (False, _UNCITED_2)])
    ok, why = _adjudicate(v, profile=None)
    assert ok is False and v.verify_calls == 1 and why == _UNCITED


def test_5b_pin_void_path_never_flips_to_inadmissible():
    """Only the citation-void path can produce the keep; a pin-void re-ask
    that comes back UNSOUND again still drops."""
    v = _StubVerifier([(False, "a DST transition could shift the day"),
                       (False, "a DST transition could still shift the day")])
    ok, why = _guarded_verify(v, dict(harness_source="s",
                                      concrete_evidence="e"),
                              pinned={'timezone': ['UTC_TIME_ZONE']},
                              evidence_profile=_DRIFT)
    assert ok is False and "INADMISSIBLE" not in why


def test_5b_reask_transport_error_returns_original_verdict():
    """Fail-open unchanged: an LLM/transport error returns the ORIGINAL
    verdict, never a manufactured keep."""
    v = _StubVerifier([(False, _UNCITED),
                       (True, "verifier error (Timeout); keeping finding")])
    ok, why = _adjudicate(v, profile=_DRIFT)
    assert ok is False and why == _UNCITED


# ---------------------------------------------------------------------------
# 5D rate parsing / profile predicate (pure)
# ---------------------------------------------------------------------------
def _fr(buggy, bden, patched=None, pden=None):
    """Build a [fire-rate fact] block in the shipped wording."""
    parts = ["buggy build {}/{} = {:.0%}".format(buggy, bden, buggy / bden)]
    if patched is not None:
        parts.append("patched build {}/{} = {:.0%}".format(
            patched, pden, patched / pden))
    return ("[fire-rate fact] " + "; ".join(parts)
            + " of random valid inputs. (interpretation elided)")


def test_parse_fire_rate_facts_both_sides():
    assert ef.parse_fire_rate_facts(_fr(13000, 20000, 9000, 20000)) == [
        (0.65, 0.45)]


def test_parse_fire_rate_facts_multi_firing_clamped():
    assert ef.parse_fire_rate_facts(_fr(2997, 1000)) == [(1.0, None)]


def test_parse_fire_rate_facts_none_when_absent():
    assert ef.parse_fire_rate_facts("no rates here") == []


@pytest.mark.parametrize("b,p,terminal", [
    (1.0, None, True),          # intrinsic on the broken build
    (0.99, 0.10, True),         # intrinsic stands alone
    (0.70, 0.42, True),         # measured fires-on-both
    (0.58, 0.25, True),         # just over the both-bar
    (0.50, 1.00, False),        # buggy side not genuinely high
    (0.70, 0.05, False),        # patched side under the cap: not "both"
    (0.00, 1.00, False),        # 5A asymmetric CATCH profile
    (0.03, 0.90, False),        # 5A asymmetric CATCH profile
    (None, 0.90, False),        # buggy side unmeasured
])
def test_fire_rate_is_terminal(b, p, terminal):
    assert ef.fire_rate_is_terminal(b, p) is terminal


def test_terminal_bar_sits_between_the_two_shipped_constants():
    assert ef.MAX_FIRE_RATIO < ef.TERMINAL_BOTH_FIRE_RATIO
    assert ef.TERMINAL_BOTH_FIRE_RATIO < ef.INTRINSIC_FIRE_RATIO


def test_terminal_profile_labels():
    assert ef.terminal_profile("identical on both builds") == \
        'identical-on-both'
    assert ef.terminal_profile(_fr(19000, 20000, 9000, 20000)) == \
        'fires-on-both-rate'
    assert ef.terminal_profile(_fr(0, 20000, 20000, 20000)) is None


def test_real_asymmetric_fire_rate_fact_is_not_terminal():
    """The shipped 5A note text for the catch profile, end to end."""
    note = ef.fire_rate_fact(20000, 200, 20000, 18000, "")
    assert note and "PATCHED build but only" in note
    assert ef.carries_terminal_identical_fact(note) is False


def test_real_both_high_fire_rate_fact_is_terminal():
    note = ef.fire_rate_fact(20000, 19000, 20000, 9000, "")
    assert note and "BOTH" in note
    assert ef.carries_terminal_identical_fact(note) is True


# ---------------------------------------------------------------------------
# (d) 5C rate: high buggy rate + family-duty NO -> keep voided
# (e) 5C escape: family-duty YES -> keep survives
# (f) 5C anti-trigger: asymmetric profile -> never terminal
# ---------------------------------------------------------------------------
_BOTH_HIGH = _fr(19000, 20000, 9000, 20000)      # buggy 95%, patched 45%
_MAJORITY_BOTH = _fr(14000, 20000, 8400, 20000)  # buggy 70%, patched 42%
_ASYMMETRIC = _fr(600, 20000, 18000, 20000)      # buggy 3%, patched 90%


@pytest.mark.parametrize("evidence", [_BOTH_HIGH, _MAJORITY_BOTH])
def test_5d_rate_terminal_voids_keep_on_family_duty_no(evidence):
    v = _StubVerifier([], fd_results=[(False, "unrelated observable")])
    ok, why = _terminal_identical_gate(
        True, "kept", evidence, v, "fired", "block", "src")
    assert ok is False
    assert "FIRES-ON-BOTH RATE TERMINAL [5D-rate]" in why
    assert v.fd_calls == 1


@pytest.mark.parametrize("evidence", [_BOTH_HIGH, _MAJORITY_BOTH])
def test_5d_rate_terminal_survives_on_family_duty_yes(evidence):
    v = _StubVerifier([], fd_results=[(True, "the test's own observable")])
    ok, why = _terminal_identical_gate(
        True, "kept", evidence, v, "fired", "block", "src")
    assert ok is True and why == "kept" and v.fd_calls == 1


def test_5d_rate_terminal_survives_on_fd_prior_yes_without_asking():
    """The Math-2 escape route: fires on both, family-duty already YES."""
    v = _StubVerifier([], fd_results=[])
    ok, why = _terminal_identical_gate(
        True, "kept", _BOTH_HIGH, v, "fired", "block", "src", fd_prior=True)
    assert ok is True and why == "kept" and v.fd_calls == 0


@pytest.mark.parametrize("fd", [(True, "duty"), (False, "no duty")])
def test_5d_asymmetric_profile_is_never_terminal(fd):
    """Buggy-LOW / patched-high is the 5A CATCH signal: not terminal, and
    family-duty is not even consulted, whatever it would answer."""
    v = _StubVerifier([], fd_results=[fd])
    ok, why = _terminal_identical_gate(
        True, "kept", _ASYMMETRIC, v, "fired", "block", "src")
    assert ok is True and why == "kept" and v.fd_calls == 0


def test_5d_rate_gate_fails_open_on_family_duty_error():
    v = _StubVerifier([], fd_results=[(True, "family-duty check unavailable")])
    ok, why = _terminal_identical_gate(
        True, "kept", _BOTH_HIGH, v, "fired", "block", "src")
    assert ok is True and why == "kept"


def test_5d_rate_gate_through_adjudicate():
    v = _StubVerifier([(True, "oracle judged sound")],
                      fd_results=[(False, "unrelated observable")])
    ok, why = _adjudicate(v, evidence=_BOTH_HIGH)
    assert ok is False and "5D-rate" in why


def test_5d_rate_gate_skipped_when_direction_confirmed():
    v = _StubVerifier([(True, "oracle judged sound")],
                      fd_results=[(False, "unrelated observable")])
    ok, why = adjudicate(
        v, harness_source="src", fired_assertion="fired", trusted_values=None,
        concrete_evidence=_BOTH_HIGH, code_context=None, pinned_source="s",
        evidence_profile=None, failing_block="b", check_source="src",
        is_direction_confirmed=True)
    assert ok is True and v.fd_calls == 0
