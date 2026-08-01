"""8.12(a) — pin gate behaviour on CRASH-leg evidence.

Why this file exists. For a **semantic** bug, a check that fires on both builds
is indiscriminate — that is the 6B/6C drop condition, and dropping it is right.
For a **crashing** bug the same pattern is the **catch** condition: the buggy
build crashes by definition, so a harness that also fires on the patched build is
evidence the patch did not fix it.

The gates cannot tell these apart. `judge_decision.py` contains no `bug_kind`
reference, and the semantic guard in `run.py` does **not** enclose either
`adjudicate()` call site — so crash legs reach the same gates as semantic legs.

The crashing-bug exposure analysis (2026-07-31) recorded this as still-open risk
3, noting that the trace showed no gate events but "the judge was never called, so
they had no opportunity" — consistent with inertness, proving nothing. These tests
give it the opportunity and record what happens.

No crashing suite has run since 2026-07-16, which predates all seven cycles of
work on this code, so these are pins on unexercised paths.
"""
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

import java.relations.evidence_facts as ef                      # noqa: E402
from java.relations.judge_decision import (                     # noqa: E402
    _confirmed_fires_on_both_gate, _indiscriminate_rate_gate)


class _Stub:
    """Minimal verifier: records family-duty calls, answers as scripted."""

    def __init__(self, duty=None):
        self._duty = duty
        self.calls = 0

    def family_duty(self, fired_assertion, failing_test_block, check_source):
        self.calls += 1
        if self._duty is None:
            raise RuntimeError('family_duty asked but not scripted')
        return self._duty


DUTY_YES = (True, 'the crash IS the failing test\'s own observable')
DUTY_NO = (False, 'unrelated observable')


def _crash_fires_on_both():
    """Crash-leg shape: the same crash observed on both builds.

    On a crashing bug this is the CATCH condition, not the indiscriminate one.
    """
    return ef.muted_replay_note(
        {'target'}, {'shadow'}, 'crashed', {'target'}, None, set(),
        value_verdict='identical',
        buggy_msg_excerpt='java.lang.ArrayIndexOutOfBoundsException: 5',
        patched_msg_excerpt='java.lang.ArrayIndexOutOfBoundsException: 5')


# --- 6C, the fires-on-both gate -------------------------------------------

def test_6C_would_DROP_a_crash_catch_when_family_duty_says_NO():
    """THE RISK, pinned. Identical crash on both builds is a crashing bug's
    catch signature, and 6C drops exactly that shape unless family duty rescues
    it. This test documents that the gate is NOT inert on crash evidence — its
    safety depends entirely on the family-duty escape."""
    v = _Stub(duty=DUTY_NO)
    ok, why = _confirmed_fires_on_both_gate(
        True, 'kept', _crash_fires_on_both(), v,
        '[oracle:t] fired', 'block', 'src', fd_state={'value': None})
    assert ok is False, 'expected the drop — if this changes, re-read 8.12(a)'
    assert '6C-FIRES-ON-BOTH-DROP' in why


def test_6C_spares_the_crash_catch_when_family_duty_says_YES():
    """The only thing standing between a crashing catch and a mechanical drop.
    For a crashing bug the crash IS the failing test's observable, so duty
    SHOULD answer YES — but that is the judge's answer, not a guarantee."""
    v = _Stub(duty=DUTY_YES)
    ok, why = _confirmed_fires_on_both_gate(
        True, 'kept', _crash_fires_on_both(), v,
        '[oracle:t] fired', 'block', 'src', fd_state={'value': None})
    assert ok is True
    assert v.calls == 1


def test_6C_fails_open_when_family_duty_errors_on_crash_evidence():
    """An LLM error must never manufacture a drop — the fail-open rule, checked
    on the crash shape specifically."""
    v = _Stub(duty=None)          # raises when asked
    ok, _why = _confirmed_fires_on_both_gate(
        True, 'kept', _crash_fires_on_both(), v,
        '[oracle:t] fired', 'block', 'src', fd_state={'value': None})
    assert ok is True


# --- 6B, the intrinsic-rate gate ------------------------------------------

def test_6B_is_inert_when_the_crash_carries_no_rate_measurement():
    """Crash legs do not carry fire-rate blocks (the rate machinery is fed by
    semantic screening), so 6B has nothing to act on and must leave the verdict
    untouched."""
    v = _Stub(duty=None)
    ok, why = _indiscriminate_rate_gate(
        True, 'kept', _crash_fires_on_both(), v,
        '[oracle:t] fired', 'block', 'src', fd_state={'value': None})
    assert ok is True and why == 'kept'
    assert v.calls == 0, 'a rateless firing must not even consult family duty'


def test_6B_would_drop_a_crash_leg_that_DID_carry_a_high_buggy_rate():
    """If a crash leg ever acquires a fire-rate block with a high buggy-side
    rate, 6B treats it exactly as a semantic indiscriminate firing. Pinned so
    the coupling is visible rather than discovered later."""
    ev = _crash_fires_on_both() + ef.fire_rate_fact(20000, 20000, 20000, 19000, '')
    v = _Stub(duty=DUTY_NO)
    ok, why = _indiscriminate_rate_gate(
        True, 'kept', ev, v, '[oracle:t] fired', 'block', 'src',
        fd_state={'value': None})
    assert ok is False
    assert '6B-INDISCRIMINATE-DROP' in why


# --- cause-signature preservation through the repair ----------------------

def test_repair_rethrow_without_cause_preserves_the_crash_signature():
    """`cause_signature()` reads the `Caused by:` chain to identify WHICH crash
    fired. The repair attaches the caught exception as the alarm's cause, which
    is what makes that chain exist — so the repair is aligned with the crashing
    design, not against it. Pinned because the exposure analysis withdrew this
    as a risk by reading, not by test."""
    from java.harness.repair import repair_rethrow_without_cause
    A = 'com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow'
    src = f"""public class FuzzHarness {{
      static void f() {{
        try {{ target(); }}
        catch (IllegalArgumentException e) {{
          throw new {A}("[oracle:x] relation violated");
        }}
      }}
    }}"""
    out = repair_rethrow_without_cause(src)
    assert ', e)' in out, 'the caught exception must reach the alarm as its cause'


def test_repair_never_strips_an_existing_cause():
    """A crash alarm that already carries its cause must be left alone."""
    from java.harness.repair import repair_rethrow_without_cause
    A = 'com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow'
    src = f"""public class FuzzHarness {{
      static void f() {{
        try {{ target(); }}
        catch (IllegalArgumentException e) {{
          throw new {A}("[oracle:x] relation violated", e);
        }}
      }}
    }}"""
    assert repair_rethrow_without_cause(src) == src
