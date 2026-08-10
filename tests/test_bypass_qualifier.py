"""The bypass qualifier (build A) and the isolation re-aim (build B).

Pre-registered in ``docs/math65-formula-read-2026-08-10.md``,
"Pre-registration round 2"; the mechanism they fix is plan item 8.42's
RESOLVED note. Leg 03 of ``gs1_isolation_20260810_175503`` convicted a correct
Math-65 patch on two relations that entered ``cycle6_gates_entry`` with
``skipped`` — direction-confirmed, so every value gate was skipped by design —
while the same firings carried ``[fact:rate-indiscriminate]``. The
shadow-isolation reading lives BEHIND those gates, so it never had a lane; and
the isolation hook, when it did run, measured ``sorted(_fired_ids)[0]`` rather
than the relation that convicted.

  BUILD A  direction-confirmed AND rate-indiscriminate no longer bypasses:
           it routes through the ordinary value path, exactly as a
           non-direction-confirmed firing does. No new dismissal exists —
           the gates are the shipped ones, escapes included.
  BUILD B  the hook isolates EVERY relation the firing names, and the reading
           gains the pairwise-agreement shape (two or more shared observables,
           no expected yardstick) that left gs1's two `fired` isolations
           ambiguous with real numbers in hand.

Offline: stubbed verifier, no JVM, no LLM, zero tokens.
"""
import os
import re

import pytest

from java.relations import evidence_facts as ef
from java.relations.judge_decision import (adjudicate,
                                           direction_confirmed_bypass)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# The two rate profiles, in the shipped wording, from the shipped builder.
_INDISCRIMINATE = ef.fire_rate_fact(1000, 999, None, None, "")   # buggy 99.9%
_BOTH_HIGH = ef.fire_rate_fact(20000, 15000, 20000, 9000, "")    # 75% / 45%
_CATCH = ef.fire_rate_fact(20000, 200, 20000, 18000, "")         # 1% / 90%

_DUTY_YES = (True, "the violated property IS the failing test's observable")
_DUTY_NO = (False, "unrelated observable")


def _muted(value_verdict):
    """The shipped muted-replay note for a CONFIRMED fires-on-both."""
    return ef.muted_replay_note(
        {"target"}, {"shadow"}, "crashed", {"target"}, None, set(),
        value_verdict=value_verdict,
        buggy_msg_excerpt="expected=2.5 actual=3.5",
        patched_msg_excerpt="expected=2.5 actual=3.5")


class _StubVerifier:
    """Scripted verify()/family_duty(); counts calls. No network."""

    def __init__(self, fd_results=None):
        self._fd = list(fd_results or [])
        self.verify_calls = 0
        self.fd_calls = 0

    def verify(self, **kwargs):
        self.verify_calls += 1
        return True, "oracle judged sound"

    def family_duty(self, *a, **k):
        self.fd_calls += 1
        if not self._fd:
            raise AssertionError("family_duty asked but no result scripted")
        return self._fd[min(self.fd_calls - 1, len(self._fd) - 1)]


def _adjudicate(v, evidence, is_direction_confirmed=False):
    return adjudicate(
        v, harness_source="src", fired_assertion="[oracle:target] fired",
        trusted_values=None, concrete_evidence=evidence, code_context=None,
        pinned_source="src", evidence_profile=None, failing_block="block",
        check_source="src", is_direction_confirmed=is_direction_confirmed)


# ---------------------------------------------------------------------------
# A1 — the routing predicate itself, on the three combinations.
# ---------------------------------------------------------------------------

def test_doubly_flagged_does_not_bypass(monkeypatch):
    import config
    monkeypatch.setattr(config, "REROUTE_INDISCRIMINATE_BYPASS", True)
    bypass, reason = direction_confirmed_bypass(True, _INDISCRIMINATE)
    assert bypass is False
    # The entry event has to say WHICH two flags sent it down the value path,
    # or a trace cannot tell a reroute from an ordinary non-confirmed firing.
    assert 'direction-confirmed' in reason
    assert ef.RATE_INDISCRIMINATE_FACT_TAG in reason


@pytest.mark.parametrize("evidence", [
    _CATCH,                       # measured, and the opposite profile
    ef.fire_rate_fact(20000, 3953, 20000, 20000, ""),   # rate-ambiguous
    _muted("identical"),          # a value fact, no rate fact at all
    "",
    None,
])
def test_singly_flagged_keeps_the_bypass_byte_for_byte(evidence):
    bypass, reason = direction_confirmed_bypass(True, evidence)
    assert bypass is True
    assert reason == ('direction-confirmed firing (mechanical buggy-build '
                      'catch) — 5C/6B/6C all skipped by design')


@pytest.mark.parametrize("evidence", [_INDISCRIMINATE, _CATCH, None])
def test_a_firing_that_is_not_direction_confirmed_is_unchanged(evidence):
    """Neither flag, or the rate flag alone: the gates run, as they always
    have, and the predicate adds no wording of its own."""
    assert direction_confirmed_bypass(False, evidence) == (False, None)


def test_the_qualifier_reads_the_TAG_and_not_the_prose():
    """Tag-only, deliberately: untagged evidence (older runs, replayed
    fixtures) keeps the old bypass rather than being rerouted on a keyword."""
    prose = ("[fire-rate fact] buggy build 999/1000 = 100% of random valid "
             "inputs. The check is indiscriminate; the firing is intrinsic to "
             "the check/setup construction, not a detection of the defect.")
    assert ef.rate_profile(prose) == 'indiscriminate'   # prose reads as one
    assert ef.RATE_INDISCRIMINATE_FACT_TAG not in prose
    assert direction_confirmed_bypass(True, prose)[0] is True


def test_the_qualifier_fails_toward_todays_behaviour(monkeypatch):
    import config
    monkeypatch.setattr(config, "REROUTE_INDISCRIMINATE_BYPASS", True)
    monkeypatch.setattr(ef, 'fact_tags',
                        lambda t: (_ for _ in ()).throw(ValueError("boom")))
    bypass, reason = direction_confirmed_bypass(True, _INDISCRIMINATE)
    assert bypass is True
    assert 'could not be read' in reason


# ---------------------------------------------------------------------------
# A2 — the same three combinations end to end through `adjudicate`.
# ---------------------------------------------------------------------------

def test_the_doubly_flagged_firing_meets_the_existing_value_gates(monkeypatch):
    import config
    monkeypatch.setattr(config, "REROUTE_INDISCRIMINATE_BYPASS", True)
    """6B's own rule, on a firing that used to skip it entirely."""
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, _INDISCRIMINATE, is_direction_confirmed=True)
    assert ok is False
    assert '[6B-INDISCRIMINATE-DROP]' in why
    assert v.fd_calls == 1


def test_the_reroute_adds_no_dismissal_of_its_own():
    """The whole point of build A: rerouting is not a drop. This firing is
    doubly flagged — and its measured rates (buggy 75%, patched 45%, the shape
    the Math-65 legs actually carry) clear none of the three gates, so it
    survives without the judge being asked anything at all."""
    v = _StubVerifier(fd_results=[])
    ok, why = _adjudicate(v, _BOTH_HIGH, is_direction_confirmed=True)
    assert ok is True and why == "oracle judged sound"
    assert v.fd_calls == 0


def test_the_family_duty_escape_survives_the_reroute(monkeypatch):
    import config
    monkeypatch.setattr(config, "REROUTE_INDISCRIMINATE_BYPASS", True)
    v = _StubVerifier(fd_results=[_DUTY_YES])
    ok, _ = _adjudicate(v, _INDISCRIMINATE, is_direction_confirmed=True)
    assert ok is True and v.fd_calls == 1


def test_the_singly_flagged_firing_still_skips_every_gate():
    """The genuine catch shape the exemption was written for: a blob 6C would
    drop outright still keeps, and the judge is never asked."""
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, _muted("identical") + "\n" + _CATCH,
                          is_direction_confirmed=True)
    assert ok is True and why == "oracle judged sound"
    assert v.fd_calls == 0


def test_a_non_confirmed_firing_is_judged_exactly_as_before():
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, _INDISCRIMINATE)
    assert ok is False and '[6B-INDISCRIMINATE-DROP]' in why


# ---------------------------------------------------------------------------
# B1 — the pairwise-agreement reading.
# ---------------------------------------------------------------------------

def test_an_agreement_check_reading_the_same_on_both_builds_is_identical():
    """Two observables, no expected value: the check asserts they agree with
    each other. Same values on both builds -> the patch changed nothing this
    check sees, which is dismissal condition (i)."""
    msg = '[oracle:chi-vs-rms] violated: chi=63.3035 rms=5.6259 n=2'
    read = ef.isolated_value_reading(msg, msg.replace('n=2', 'n=2 '))
    assert read['reading'] == 'identical'
    assert ef.isolation_dismisses(read)


def test_an_agreement_check_reading_differently_is_recorded_not_dismissed():
    """gs1's two `fired` isolations, in one case: real numbers, no yardstick.
    The reading is stated as a fact and dismisses nothing — with no expected
    value there is no direction to read."""
    patched = '[oracle:chi-vs-rms] violated: chi=63.3035 rms=5.6259'
    buggy = '[oracle:chi-vs-rms] violated: chi=41.7530 rms=5.6259'
    read = ef.isolated_value_reading(patched, buggy)
    assert read['reading'] == 'buggy-differs'
    assert not ef.isolation_dismisses(read)
    fact = ef.isolation_reading_fact(read, {'chi-vs-rms'})
    assert ef.ISOLATION_FACT_TAG in fact
    for number in ('63.3035', '41.753', '5.6259'):
        assert number in fact
    assert 'settles nothing on its own' in fact
    # It must not read as a terminal identical-on-both fact: that would drop
    # the firing it was only supposed to describe.
    assert ef.terminal_profile(fact) is None


def test_the_agreement_reading_needs_two_shared_observables():
    """One observable and no yardstick stays exactly as ambiguous as it was —
    a single number that differs says nothing without something to compare it
    to."""
    read = ef.isolated_value_reading('[oracle:c] violated: actual=4.0',
                                     '[oracle:c] violated: actual=9.0')
    assert read['reading'] == 'ambiguous'
    assert not ef.isolation_dismisses(read)
    assert ef.isolation_reading_fact(read) is None


def test_a_stated_expected_value_still_takes_the_closeness_path():
    """The agreement branch is for checks with NO yardstick. When one is
    stated, the shipped closeness reading decides, unchanged — including its
    refusal when more than one observable differs."""
    closer = ef.isolated_value_reading(
        '[oracle:chi] violated: chi=4.5636 n=2 expected=3.9375',
        '[oracle:chi] violated: chi=0.0564 n=2 expected=3.9375')
    assert closer['reading'] == 'patched-closer'
    two = ef.isolated_value_reading(
        '[oracle:c] violated: a=4.0 b=7.0 expected=1.0',
        '[oracle:c] violated: a=9.0 b=2.0 expected=1.0')
    assert two['reading'] == 'ambiguous'


def test_the_agreement_reading_never_fires_on_reference_keys_alone():
    """`expected=`/`tol=` are equal on both builds by construction, so a pair
    of messages sharing only those must not read as two agreeing
    observables."""
    read = ef.isolated_value_reading(
        '[oracle:c] violated: patchedSide=4.0 expected=1.0 tol=1.0E-9',
        '[oracle:c] violated: buggySide=9.0 expected=1.0 tol=1.0E-9')
    assert read['reading'] == 'ambiguous'
    assert 'reference keys' in read['detail']


def test_a_tolerance_alone_does_not_block_the_agreement_reading():
    """A `tol=` is not a yardstick for closeness — only an `expected=` is."""
    read = ef.isolated_value_reading(
        '[oracle:agree] violated: chi=63.30 rms=5.62 tol=1.0E-9',
        '[oracle:agree] violated: chi=41.75 rms=5.62 tol=1.0E-9')
    assert read['reading'] == 'buggy-differs'


def test_every_reading_name_is_declared():
    for name in ('identical', 'patched-closer', 'buggy-closer',
                 'buggy-differs', 'ambiguous'):
        assert name in ef.ISOLATION_READINGS
    assert not ef.isolation_dismisses('buggy-differs')


# ---------------------------------------------------------------------------
# B2 — the hook targeting. Pinned by reading the source: the enclosing loop
#      needs a whole live run (JVM, builder, Jazzer) to execute.
# ---------------------------------------------------------------------------

def _isolation_block(code_only=False):
    with open(os.path.join(ROOT, 'src', 'java', 'run.py')) as fh:
        body = fh.read()
    block = body[body.index('SHADOW-ISOLATION READING'):]
    block = block[:block.index('# The firing INPUT itself')]
    if code_only:
        # The comments in this block quote the old targeting they replaced.
        block = '\n'.join(ln for ln in block.splitlines()
                          if not ln.lstrip().startswith('#'))
    return block


def test_the_hook_targets_every_convicting_relation_the_firing_names():
    block = _isolation_block(code_only=True)
    assert re.search(r'for _iso_target in sorted\(_fired_ids\):', block), (
        'the isolation hook lost its per-relation loop')
    assert 'sorted(_fired_ids)[0]' not in block, (
        'the hook is back to isolating the first id by name — gs1 leg 03 '
        'showed that misses the convicting relation')
    # Each reading is attributed to the relation it was measured on, not to
    # every id the firing happens to mention.
    assert '_irf(_iso_read, {_iso_target})' in block


def test_the_hook_records_one_event_per_target():
    block = _isolation_block()
    assert block.count("method='isolated-buggy-replay'") == 1
    assert block.count('record_event(') == 1
    # ...inside the loop, so one event per isolated relation reaches trace.md.
    assert block.index('for _iso_target in sorted(_fired_ids):') \
        < block.index('record_event(')
    assert "'targets': sorted(_fired_ids)" in block


def test_the_hook_still_has_exactly_one_dismissal_path():
    """Build B re-aims the measurement; it adds no way to drop a firing."""
    block = _isolation_block()
    assert block.count('drop_reasons.append') == 1
    assert block.count('_idis(') == 1
    assert 'args.' not in block          # no flag consulted
    assert 'kept_reason' not in block    # nothing here can convict


def test_reroute_is_disabled_by_default_after_gb_failure():
    """8.43: G-B failed both gates (1/19 genuine catches lost), so the
    doubly-flagged reroute is OFF unless config.REROUTE_INDISCRIMINATE_BYPASS
    is set inside a pre-registered validation run."""
    from java.relations.judge_decision import direction_confirmed_bypass

    bypass, reason = direction_confirmed_bypass(
        True, "evidence with [fact:rate-indiscriminate] stamped")
    assert bypass is True
    assert "skipped by design" in reason
