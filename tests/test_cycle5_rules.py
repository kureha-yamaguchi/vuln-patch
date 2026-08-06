"""Cycle-5 judge rules 5A (trigger-tier wording), 5B (recall-side dismissal
lint) and 5C (precision-side terminal mirror).

Everything asserted here is a PURE decision predicate or a pure note builder,
plus the two fail-open wrapper helpers in run.py driven by a fake verifier.
No JVM, no LLM, no network.

Coverage:
  * pinned_parameters              (5B(i): UTC / Locale / seed extraction)
  * dismissal_invokes_pinned       (5B(i): void predicate)
  * verdict_needs_citation         (5B(ii): drift-kill citation predicate)
  * carries_terminal_identical_fact(5C: terminal-fact detector)
  * trigger_tier_note              (5A: neutral wording, no dismiss lean)
  * pinned_environment_note        (5B(i): the attached fact)
  * _guarded_verify                (5B: void-and-re-ask, fails open)
  * _terminal_identical_gate       (5C: family-duty routing, fails open)
"""
import pytest

from java.parsing.java_source import pinned_parameters
from java.relations import evidence_facts as ef


# --------------------------------------------------------------------------
# 5B(i) — pinned_parameters extraction
# --------------------------------------------------------------------------
def test_pinned_parameters_utc_timezone():
    src = ("cal.setTimeZone(DateUtils.UTC_TIME_ZONE);\n"
           "DateTime dt = new DateTime(ms, DateTimeZone.UTC);")
    pins = pinned_parameters(src)
    assert 'timezone' in pins
    assert any('UTC_TIME_ZONE' in s for s in pins['timezone'])


def test_pinned_parameters_locale():
    src = "Locale.setDefault(Locale.US); DateFormat.getDateInstance(0, Locale.GERMANY);"
    pins = pinned_parameters(src)
    assert 'locale' in pins


def test_pinned_parameters_fixed_seed():
    src = "Random rng = new Random(42L); rng.nextInt();"
    pins = pinned_parameters(src)
    assert 'seed' in pins


def test_pinned_parameters_nonseed_random_not_pinned():
    # An unseeded Random pins nothing (no literal seed).
    assert 'seed' not in pinned_parameters("Random rng = new Random();")


def test_pinned_parameters_empty_when_nothing_pinned():
    assert pinned_parameters("int x = a + b; assertEquals(x, y);") == {}


# --------------------------------------------------------------------------
# 5B(i) — dismissal_invokes_pinned
# --------------------------------------------------------------------------












# --------------------------------------------------------------------------
# 5B(ii) — verdict_needs_citation
# --------------------------------------------------------------------------
_DRIFT = {'buggy_silent': True, 'deterministic_trigger': True,
          'patched_firing': True}










# --------------------------------------------------------------------------
# 5C — carries_terminal_identical_fact
# --------------------------------------------------------------------------
def test_terminal_detector_positive_identical():
    assert ef.carries_terminal_identical_fact(
        "behaviour at this input is identical on both builds") is True


def test_terminal_detector_positive_buggy_scan():
    assert ef.carries_terminal_identical_fact(
        "[buggy-scan fact] the same check fires on both builds") is True


def test_terminal_detector_negative_asymmetric_catch():
    # The asymmetric fire-rate CATCH wording must NOT read as terminal.
    txt = ("fires on 90% of random valid inputs on the PATCHED build but only "
           "3% on the buggy build")
    assert ef.carries_terminal_identical_fact(txt) is False


def test_terminal_detector_negative_trigger_tier_note():
    # The neutralized 5A note must not trip the 5C detector.
    assert ef.carries_terminal_identical_fact(ef.trigger_tier_note()) is False


# --------------------------------------------------------------------------
# --------------------------------------------------------------------------


# --------------------------------------------------------------------------
# 5A — trigger_tier_note neutral wording (prompt-block presence)
# --------------------------------------------------------------------------
def test_trigger_tier_note_states_fact():
    note = ef.trigger_tier_note()
    assert "[trigger-tier fact]" in note
    assert "OWN" in note and "input literals" in note


def test_trigger_tier_note_has_no_dismiss_lean():
    low = ef.trigger_tier_note().lower()
    assert "dismiss" not in low
    assert "dismiss unless" not in low


# --------------------------------------------------------------------------
# 5B(i) — pinned_environment_note (the attached fact)
# --------------------------------------------------------------------------
def test_pinned_environment_note_present():
    note = ef.pinned_environment_note({'timezone': ['UTC_TIME_ZONE']})
    assert note is not None
    assert "[pinned-environment fact]" in note and "timezone" in note


def test_pinned_environment_note_none_when_empty():
    assert ef.pinned_environment_note({}) is None


# --------------------------------------------------------------------------
# 5B / 5C — the two fail-open wrapper helpers, driven by a fake verifier
# --------------------------------------------------------------------------
from java.relations.judge_decision import (  # noqa: E402
    _guarded_verify, _terminal_identical_gate)


class _FakeVerifier:
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


_PIN = {'timezone': ['UTC_TIME_ZONE']}
_KW = dict(harness_source="src", concrete_evidence="e")












def test_terminal_gate_voids_on_family_duty_no():
    v = _FakeVerifier([], fd_results=[(False, "unrelated observable")])
    ok, why = _terminal_identical_gate(
        True, "kept", "identical on both builds", v, "fired", "block", "src")
    assert ok is False and "TERMINAL" in why and v.fd_calls == 1


def test_terminal_gate_keeps_on_family_duty_yes():
    v = _FakeVerifier([], fd_results=[(True, "is the test's own observable")])
    ok, why = _terminal_identical_gate(
        True, "kept", "identical on both builds", v, "fired", "block", "src")
    assert ok is True and why == "kept"


def test_terminal_gate_no_fact_no_call():
    v = _FakeVerifier([], fd_results=[(False, "x")])
    ok, why = _terminal_identical_gate(
        True, "kept", "just a tolerance note", v, "fired", "block", "src")
    assert ok is True and v.fd_calls == 0


def test_terminal_gate_fails_open_on_family_duty_error():
    # family_duty fails open to YES on error -> never a manufactured drop.
    v = _FakeVerifier([], fd_results=[(True, "family-duty check unavailable")])
    ok, why = _terminal_identical_gate(
        True, "kept", "identical on both builds", v, "fired", "block", "src")
    assert ok is True


def test_terminal_gate_reuses_prior_no_without_asking():
    v = _FakeVerifier([], fd_results=[])
    ok, why = _terminal_identical_gate(
        True, "kept", "identical on both builds", v, "fired", "block", "src",
        fd_prior=False)
    assert ok is False and v.fd_calls == 0


def test_terminal_gate_skips_when_already_yes():
    v = _FakeVerifier([], fd_results=[])
    ok, why = _terminal_identical_gate(
        True, "kept", "identical on both builds", v, "fired", "block", "src",
        fd_prior=True)
    assert ok is True and why == "kept" and v.fd_calls == 0


def test_terminal_gate_ignores_unsound():
    v = _FakeVerifier([], fd_results=[(True, "x")])
    ok, why = _terminal_identical_gate(
        False, "dropped", "identical on both builds", v, "f", "b", "s")
    assert ok is False and why == "dropped" and v.fd_calls == 0
