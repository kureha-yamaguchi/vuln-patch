"""Cycle-6 observability: every cycle-6 decision leaves a PERMANENT event.

Why this file exists (docs/replay/night20c_analysis.md): the cycle-6 mechanisms
announced themselves with ``print``, but ``run_suite.sh`` DELETES ``run.log``
on a successful leg, and ``trace.md`` is built solely from ``llm.record_event``
output. So after a full 20-leg green run the mechanism checklist read 0 for
every cycle-6 item — which is NOT evidence they were quiet, because their only
output channel had been deleted. Worse, the two surviving false accusations
were exactly the two the 6B rule drops offline, so at least one mechanism was
suspected INERT in production and there was no way to confirm or refute it.

The fix is the one already applied to the one-door / universal-screen
diagnostics: route each decision through ``record_event``. The contract these
tests pin, for each of the six mechanisms:

  * a CONSIDERED event fires whenever the code path executes at all — so
    "ran and found nothing" is distinguishable from "never ran";
  * a DECIDED event fires on BOTH branches (fired and not-fired);
  * the recorder can never break the pipeline (fail-open).

Offline: stubbed verifier, stubbed replays, no JVM, no LLM, zero tokens.
"""
import pytest

import llm
from java.relations import evidence_facts as ef
from java.relations import judge_decision as jd
from java.relations.judge_decision import (
    _confirmed_fires_on_both_gate, _family_duty_escape,
    _indiscriminate_rate_gate, adjudicate)


# ---------------------------------------------------------------------------
# Capture harness — the events the pipeline WOULD write into trace.md.
# ---------------------------------------------------------------------------
@pytest.fixture
def events(monkeypatch):
    """Capture every ``record_event`` call made through the real recorder.

    Every cycle-6 site imports ``record_event`` from ``llm`` INSIDE the call,
    so patching the module attribute captures all of them.
    """
    seen = []

    def _capture(kind, **fields):
        seen.append(dict(kind=kind, **fields))

    monkeypatch.setattr(llm, 'record_event', _capture)
    return seen


def methods(seen):
    return [e.get('method') for e in seen]


def one(seen, method):
    """The single event with this method (fails loudly if 0 or >1)."""
    hits = [e for e in seen if e.get('method') == method]
    assert len(hits) == 1, f"expected exactly 1 {method}, got {len(hits)}"
    return hits[0]


def some(seen, method):
    return [e for e in seen if e.get('method') == method]


class _StubVerifier:
    """Scripted verify()/family_duty(); counts calls. No network."""

    def __init__(self, verify_results=None, fd_results=None, fd_raises=False):
        self._vr = list(verify_results or [(True, "oracle judged sound")])
        self._fd = list(fd_results or [])
        self.verify_calls = 0
        self.fd_calls = 0
        self._fd_raises = fd_raises

    def verify(self, **kwargs):
        r = self._vr[min(self.verify_calls, len(self._vr) - 1)]
        self.verify_calls += 1
        return r

    def family_duty(self, *a, **k):
        self.fd_calls += 1
        if self._fd_raises:
            raise RuntimeError("transport blew up")
        if not self._fd:
            raise AssertionError("family_duty asked but no result scripted")
        return self._fd[min(self.fd_calls - 1, len(self._fd) - 1)]


_DUTY_YES = (True, "the violated property IS the failing test's observable")
_DUTY_NO = (False, "unrelated observable")

# A measured buggy-side rate at/above the intrinsic bar (what 6B keys on).
_INDISCRIMINATE = ef.fire_rate_fact(1000, 999, None, None, "")
# A measured rate that is NOT the indiscriminate profile.
_CATCH = ef.fire_rate_fact(20000, 200, 20000, 18000, "")


def _muted(value_verdict):
    """The shipped muted-replay note for a CONFIRMED fires-on-both."""
    return ef.muted_replay_note(
        {"target"}, {"shadow"}, "crashed", {"target"}, None, set(),
        value_verdict=value_verdict,
        buggy_msg_excerpt="expected=2.5 actual=3.5",
        patched_msg_excerpt="expected=2.5 actual=3.5")


def _adjudicate(v, evidence, fd_prior=None, is_direction_confirmed=False):
    return adjudicate(
        v, harness_source="src", fired_assertion="[oracle:target] fired",
        trusted_values=None, concrete_evidence=evidence, code_context=None,
        pinned_source="src", evidence_profile=None, failing_block="block",
        check_source="src", fd_prior=fd_prior,
        is_direction_confirmed=is_direction_confirmed)


# ---------------------------------------------------------------------------
# 0 — the recorder is real, and these events do reach trace.md
# ---------------------------------------------------------------------------
def test_the_cycle6_events_actually_reach_trace_md(tmp_path):
    """End to end through the REAL recorder and the REAL trace writer: a live
    6B drop must be greppable in `trace.md` — the file that survives a green
    leg. This is the assertion night20c could not make."""
    from java.run import _write_trace_md
    llm.enable_recording()
    llm.reset_events()
    try:
        v = _StubVerifier(fd_results=[_DUTY_NO])
        ok, _ = _adjudicate(v, _INDISCRIMINATE)
        assert ok is False
        out = tmp_path / 'trace.md'
        _write_trace_md(str(out), 'Math-30', 'correct', llm.get_events())
        text = out.read_text()
        for method in ('cycle6_gates_entry',
                       'cycle6_6B_indiscriminate_considered',
                       'cycle6_6B_indiscriminate_decided',
                       'cycle6_family_duty_considered',
                       'cycle6_family_duty_decided',
                       'cycle6_6C_fires_on_both_considered',
                       'cycle6_6C_fires_on_both_decided'):
            assert method in text, f"{method} never reached trace.md"
        # ...and the `reason` field renders, not just the method name.
        assert 'reason: 6B-INDISCRIMINATE-DROP' in text
    finally:
        llm.reset_events()


def test_every_cycle6_site_records_through_the_same_llm_module(events):
    """A second `llm` module object would silently swallow every event (the
    recorder flag is module-global). Behavioural check: each module's audit
    helper must land in the `llm` the test patched."""
    import java.run as run_mod
    import java.execution.fuzz_runner as fr
    jd._ev('probe_jd')
    fr._ev('probe_fr')
    run_mod._cycle6_ev('probe_run')
    assert methods(events) == ['probe_jd', 'probe_fr', 'probe_run']


def test_the_recorder_is_enabled_exactly_where_the_pipeline_runs():
    """`enable_recording()` is called in run.py::main() and nowhere else, so
    every in-run call site is covered. The OFFLINE replay harness
    (verifier_replay.py) calls `adjudicate` with the recorder OFF — it writes
    no trace.md, so the events are simply no-ops there. Pinned so a future
    entrypoint that forgets `enable_recording()` is noticed."""
    import java.run as run_mod
    import java.verifier_replay as vr
    assert 'enable_recording()' in open(run_mod.__file__).read()
    assert 'enable_recording' not in open(vr.__file__).read()


# ---------------------------------------------------------------------------
# 1 — 6B indiscriminate drop
# ---------------------------------------------------------------------------
def test_6b_records_considered_and_decided_when_it_drops(events):
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, _INDISCRIMINATE)
    assert ok is False and "[6B-INDISCRIMINATE-DROP]" in why
    considered = one(events, 'cycle6_6B_indiscriminate_considered')
    assert considered['output'].startswith('at-or-above-bar · rate=0.99')
    assert one(events, 'cycle6_6B_indiscriminate_decided')['output'] == 'dropped'


def test_6b_records_the_family_duty_escape_as_its_decision(events):
    v = _StubVerifier(fd_results=[_DUTY_YES])
    ok, _ = _adjudicate(v, _INDISCRIMINATE)
    assert ok is True
    assert one(events, 'cycle6_6B_indiscriminate_considered')
    assert one(events, 'cycle6_6B_indiscriminate_decided')['output'] == 'escaped'


def test_6b_records_that_it_ran_and_the_rate_was_healthy(events):
    """THE point of this file: a quiet 6B must still be visible.

    Renamed from ``..._found_no_rate``. On this evidence the rate IS measured —
    it is the catch profile, buggy side near zero — and the old label said
    "rate=None / no rate found", which reads as a missing measurement. That
    misreading is the whole reason for item 1, and it was pinned by a test
    asserting the wrong words."""
    v = _StubVerifier()
    ok, _ = _adjudicate(v, _CATCH)
    assert ok is True
    considered = one(events, 'cycle6_6B_indiscriminate_considered')['output']
    assert considered.startswith('below-bar · rate=')
    assert 'rate=None' not in considered
    decided = one(events, 'cycle6_6B_indiscriminate_decided')
    assert decided['output'] == 'not-applicable · below-bar'
    # It must say the check discriminates, NOT that nothing was measured.
    assert 'discriminates' in decided['reason']
    assert 'never measured' not in decided['reason']


def test_6b_records_no_measurement_distinctly_from_a_healthy_rate(events):
    """The state the old label was mistaken FOR must be reachable and distinct
    from the state it actually usually meant."""
    v = _StubVerifier()
    _adjudicate(v, "no facts here at all")
    considered = one(events, 'cycle6_6B_indiscriminate_considered')['output']
    assert considered == 'no-measurement · rate=None'
    decided = one(events, 'cycle6_6B_indiscriminate_decided')
    assert decided['output'] == 'not-applicable · no-measurement'
    assert 'never measured' in decided['reason']


def test_6b_records_a_not_applicable_when_the_alarm_was_already_discarded(
        events):
    """Renamed from ``..._when_the_verdict_is_already_unsound``. In this file
    `ok` is the status of the FIRING: ok=False means the alarm was already
    explained away, NOT that the patch is unsound. The old label said
    "already UNSOUND", which reads as the opposite."""
    v = _StubVerifier(verify_results=[(False, "judge says unsound")])
    ok, _ = _indiscriminate_rate_gate(
        False, "judge says unsound", _INDISCRIMINATE, v, "fired", "block",
        "src", {'value': None, 'why': None})
    assert ok is False
    assert one(events, 'cycle6_6B_indiscriminate_considered')['output'] == \
        'alarm-already-discarded'
    decided = one(events, 'cycle6_6B_indiscriminate_decided')
    assert decided['output'] == 'not-applicable · alarm-already-discarded'
    assert 'no standing alarm' in decided['reason']


# ---------------------------------------------------------------------------
# 2 — 6C confirmed fires-on-both
# ---------------------------------------------------------------------------
@pytest.mark.parametrize("verdict,expect_output,expect_decision", [
    ("identical", 'verdict=identical', 'dropped'),
    ("different", 'verdict=different', 'kept'),
    # "we compared and could not tell" is now distinct from "there was nothing
    # to compare" — see test_6c_records_none_when_there_is_no_confirmation.
    ("unknown", 'verdict=not-compared',
     'not-applicable · values-not-comparable'),
])
def test_6c_records_the_resolved_value_verdict_and_its_decision(
        events, verdict, expect_output, expect_decision):
    """At the gate itself (end to end, `identical` is usually already dropped
    upstream by 5C — see the deny-first test below for the live shape)."""
    v = _StubVerifier(fd_results=[_DUTY_NO])
    _confirmed_fires_on_both_gate(
        True, "kept", _muted(verdict), v, "[oracle:target] fired", "block",
        "src", {'value': None, 'why': None})
    assert one(events,
               'cycle6_6C_fires_on_both_considered')['output'] == expect_output
    assert one(events,
               'cycle6_6C_fires_on_both_decided')['output'] == expect_decision


def test_6c_records_a_live_drop_through_adjudicate(events):
    """The deny-first blob 5C stands down on, so 6C really is the decider."""
    blob = (_muted("identical")
            + "\n[buggy-replay fact] [fact:not-compared] values were not "
              "compared.")
    assert ef.terminal_profile(blob) is None      # 5C stands down
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, blob)
    assert ok is False and "[6C-FIRES-ON-BOTH-DROP]" in why
    assert one(events,
               'cycle6_6C_fires_on_both_considered')['output'] == \
        'verdict=identical'
    assert one(events, 'cycle6_6C_fires_on_both_decided')['output'] == 'dropped'


def test_6c_records_none_when_there_is_no_confirmation_at_all(events):
    """Ran, saw nothing — and says so, in words distinct from "compared and
    could not tell"."""
    v = _StubVerifier()
    _adjudicate(v, "no facts here at all")
    assert one(events,
               'cycle6_6C_fires_on_both_considered')['output'] == 'verdict=none'
    decided = one(events, 'cycle6_6C_fires_on_both_decided')
    assert decided['output'] == 'not-applicable · no-fires-on-both-confirmation'
    assert 'nothing to compare' in decided['reason']


def test_6c_records_the_family_duty_escape(events):
    v = _StubVerifier(fd_results=[_DUTY_YES])
    ok, _ = _confirmed_fires_on_both_gate(
        True, "kept", _muted("identical"), v, "fired", "block", "src",
        {'value': None, 'why': None})
    assert ok is True
    assert one(events,
               'cycle6_6C_fires_on_both_decided')['output'] == 'escaped'


def test_adjudicate_records_that_the_gates_were_skipped(events):
    """direction-confirmed WITHOUT the rate fact skips 5C/6B/6C entirely.
    Without this event a trace with no 6B/6C step is ambiguous between
    'skipped by design' and 'the code never ran' — which is exactly the
    night20c ambiguity."""
    v = _StubVerifier()
    _adjudicate(v, _CATCH, is_direction_confirmed=True)
    assert one(events, 'cycle6_gates_entry')['output'] == 'skipped'
    assert 'cycle6_6B_indiscriminate_considered' not in methods(events)


def test_adjudicate_records_the_rerouted_bypass_and_names_both_flags(events):
    """Build A: direction-confirmed AND rate-indiscriminate runs the gates,
    and the entry event says which two flags sent it there — otherwise a trace
    cannot tell a reroute from an ordinary non-confirmed firing."""
    v = _StubVerifier()
    _adjudicate(v, _INDISCRIMINATE, is_direction_confirmed=True)
    entry = one(events, 'cycle6_gates_entry')
    assert entry['output'] == 'running'
    assert 'direction-confirmed' in entry['reason']
    assert ef.RATE_INDISCRIMINATE_FACT_TAG in entry['reason']
    assert 'cycle6_6B_indiscriminate_considered' in methods(events)


def test_adjudicate_records_that_the_gates_ran(events):
    v = _StubVerifier()
    _adjudicate(v, _CATCH)
    assert one(events, 'cycle6_gates_entry')['output'] == 'running'


# ---------------------------------------------------------------------------
# 3 — the shared family-duty consultation
# ---------------------------------------------------------------------------
def test_family_duty_records_the_question_and_the_answer(events):
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, _ = _family_duty_escape(v, "[oracle:target] fired", "block", "src",
                                {'value': None, 'why': None})
    assert ok is False and v.fd_calls == 1
    considered = one(events, 'cycle6_family_duty_considered')
    assert considered['output'] == 'prior=None'
    assert 'asking' in considered['reason']
    decided = one(events, 'cycle6_family_duty_decided')
    assert decided['output'] == 'NO' and 'source=asked' in decided['reason']


def test_family_duty_records_a_yes(events):
    v = _StubVerifier(fd_results=[_DUTY_YES])
    ok, _ = _family_duty_escape(v, "fired", "block", "src",
                                {'value': None, 'why': None})
    assert ok is True
    assert one(events, 'cycle6_family_duty_decided')['output'] == 'YES'


@pytest.mark.parametrize("prior,answer", [(True, 'YES'), (False, 'NO')])
def test_family_duty_records_the_skip_when_fd_prior_is_known(
        events, prior, answer):
    """The judge is NOT asked — and the record says why, so a trace showing one
    family-duty LLM call for three gates is explicable."""
    v = _StubVerifier()
    ok, _ = _family_duty_escape(v, "fired", "block", "src",
                                {'value': prior, 'why': None})
    assert ok is prior and v.fd_calls == 0
    considered = one(events, 'cycle6_family_duty_considered')
    assert considered['output'] == f'prior={prior}'
    assert 'skipped' in considered['reason']
    decided = one(events, 'cycle6_family_duty_decided')
    assert decided['output'] == answer and 'source=prior' in decided['reason']


def test_family_duty_records_the_fail_open_on_an_error(events):
    v = _StubVerifier(fd_raises=True)
    ok, _ = _family_duty_escape(v, "fired", "block", "src",
                                {'value': None, 'why': None})
    assert ok is True
    decided = one(events, 'cycle6_family_duty_decided')
    assert decided['output'] == 'YES' and 'fail-open' in decided['reason']


def test_family_duty_is_asked_once_and_the_reuse_is_visible(events):
    """Two gates sharing one `fd_state`: ONE judge call, but TWO recorded
    consultations — so the audit sees the sharing instead of inferring it from
    a missing event."""
    v = _StubVerifier(fd_results=[_DUTY_NO])
    fd_state = {'value': None, 'why': None}
    _indiscriminate_rate_gate(True, "kept", _INDISCRIMINATE, v,
                              "[oracle:target] fired", "block", "src", fd_state)
    _confirmed_fires_on_both_gate(True, "kept", _muted("identical"), v,
                                  "[oracle:target] fired", "block", "src",
                                  fd_state)
    assert v.fd_calls == 1
    consulted = some(events, 'cycle6_family_duty_considered')
    asked = [e for e in consulted if e['output'] == 'prior=None']
    reused = [e for e in consulted if e['output'] != 'prior=None']
    assert len(asked) == 1 and len(reused) == 1
    assert 'skipped' in reused[0]['reason']
    assert len(some(events, 'cycle6_family_duty_decided')) == 2


# ---------------------------------------------------------------------------
# 4 — diversion instrumentation (muted + report replays)
# ---------------------------------------------------------------------------
class _StubBuilder:
    """Compiles (or refuses to compile) without touching javac."""

    def __init__(self, compiles=True):
        self.compiles = compiles

    def build(self, source, buggy_dir, output_subdir):
        class _BR:
            compiled = self.compiles
            class_name = 'HarnessX'
            harness_path = '/tmp/does-not-exist/HarnessX.java'
        return _BR()


def _fuzz_runner():
    from java.execution.fuzz_runner import FuzzRunner
    return FuzzRunner.__new__(FuzzRunner)


def _stub_jazzer(monkeypatch, triggered=False, output='', raises=False):
    import java.execution.fuzz_runner as fr

    class _Outcome:
        combined_output = output
        timed_out = False
        returncode = 0

    _Outcome.triggered = triggered

    def _run(**kwargs):
        if raises:
            raise RuntimeError("jazzer exploded")
        return _Outcome()

    monkeypatch.setattr(fr, 'run_jazzer', _run)


@pytest.mark.parametrize("skipped_line,expect_diverted", [
    ("[relscreen] skipped=2", 'diverted=True'),
    ("[relscreen] skipped=0", 'diverted=False'),
])
def test_diversion_records_that_it_was_applied_on_the_muted_replay(
        events, monkeypatch, tmp_path, skipped_line, expect_diverted):
    import java.execution.fuzz_runner as fr
    from java.execution import oracle_mute as om
    _stub_jazzer(monkeypatch, output=skipped_line)
    monkeypatch.setattr(om, 'mute_oracles', lambda s, **k: s)
    monkeypatch.setattr(om, 'instrument_diversion', lambda s: s + "//div")
    src = tmp_path / 'HarnessX.java'
    src.write_text('class HarnessX {}')
    r = _fuzz_runner()
    r.jazzer_standalone_jar = r.jazzer_api_jar = None
    r.expected_exceptions = []
    status, fired, out, diverted = fr.FuzzRunner.replay_input_muted(
        r, str(src), 'HarnessX', 'cp', str(tmp_path / 'in'),
        mute_ids={'shadow'}, builder=_StubBuilder(), buggy_dir=str(tmp_path))
    considered = one(events, 'cycle6_diversion_considered')
    assert considered['output'] == 'instrumented=True'
    decided = one(events, 'cycle6_diversion_decided')
    assert decided['output'] == expect_diverted == f'diverted={diverted}'


def test_diversion_records_that_it_was_NOT_applied(events, monkeypatch,
                                                   tmp_path):
    """The not-fired branch: the transform never got in, so `diverted` is None
    — previously indistinguishable from 'no diversion happened'."""
    import java.execution.fuzz_runner as fr
    from java.execution import oracle_mute as om
    _stub_jazzer(monkeypatch, output='no counters here')
    monkeypatch.setattr(om, 'mute_oracles', lambda s, **k: s)
    monkeypatch.setattr(
        om, 'instrument_diversion',
        lambda s: (_ for _ in ()).throw(ValueError("nope")))
    src = tmp_path / 'HarnessX.java'
    src.write_text('class HarnessX {}')
    r = _fuzz_runner()
    r.jazzer_standalone_jar = r.jazzer_api_jar = None
    r.expected_exceptions = []
    fr.FuzzRunner.replay_input_muted(
        r, str(src), 'HarnessX', 'cp', str(tmp_path / 'in'),
        mute_ids={'shadow'}, builder=_StubBuilder(), buggy_dir=str(tmp_path))
    assert one(events,
               'cycle6_diversion_considered')['output'] == 'instrumented=False'
    assert one(events,
               'cycle6_diversion_decided')['output'] == 'diverted=None'


def test_diversion_records_both_events_on_the_report_replay(events,
                                                            monkeypatch,
                                                            tmp_path):
    import java.execution.fuzz_runner as fr
    _stub_jazzer(monkeypatch, output='clean')
    r = _fuzz_runner()
    r.jazzer_standalone_jar = r.jazzer_api_jar = None
    r.expected_exceptions = []
    src = tmp_path / 'HarnessX.java'
    src.write_text('class HarnessX {}')
    # No builder/buggy_dir -> the probe cannot run; that must be RECORDED.
    fr.FuzzRunner.replay_input_report(
        r, str(src), 'HarnessX', 'cp', str(tmp_path / 'in'))
    assert one(events,
               'cycle6_diversion_considered')['output'] == 'instrumented=False'
    assert one(events,
               'cycle6_diversion_decided')['output'] == 'diverted=None'


# ---------------------------------------------------------------------------
# 5 — the iterated muted replay
# ---------------------------------------------------------------------------
def test_iterated_muted_replay_records_one_event_per_pass(events):
    from java.execution.fuzz_runner import iterate_muted_replay
    seq = [("crashed", {"s1"}, "", None),
           ("crashed", {"s2"}, "", None),
           ("crashed", {"target"}, "", None)]

    def replay(mute_ids, pass_index):
        return seq[pass_index - 1]

    status, fired, out, div, muted, passes = iterate_muted_replay(
        replay, {"target"}, {"shadow"}, log=lambda *_a: None)
    assert passes == 3
    assert one(events, 'cycle6_muted_replay_considered')['output'] == \
        'mute_set_size=1'
    per_pass = some(events, 'cycle6_muted_replay_pass')
    assert len(per_pass) == 3
    assert 'mute_set_size=' in per_pass[0]['output']
    assert 'continue: new shadow' in per_pass[0]['reason']
    decided = one(events, 'cycle6_muted_replay_decided')
    assert 'passes=3' in decided['output']
    assert decided['reason'] == 'stop: target fired (answered)'


def test_iterated_muted_replay_records_the_quiet_single_pass(events):
    """The not-fired branch: one pass, nothing learned, iteration never grew
    the mute set — still a permanent record."""
    from java.execution.fuzz_runner import iterate_muted_replay

    def replay(mute_ids, pass_index):
        return ("mute_failed", None, "", None)

    iterate_muted_replay(replay, {"target"}, {"shadow"}, log=lambda *_a: None)
    assert one(events, 'cycle6_muted_replay_considered')
    assert len(some(events, 'cycle6_muted_replay_pass')) == 1
    decided = one(events, 'cycle6_muted_replay_decided')
    assert 'passes=1' in decided['output']
    assert 'UNKNOWN kept' in decided['reason']


def test_iterated_muted_replay_records_a_raising_pass(events):
    from java.execution.fuzz_runner import iterate_muted_replay

    def replay(mute_ids, pass_index):
        raise RuntimeError("boom")

    status, *_ = iterate_muted_replay(replay, {"target"}, set(),
                                      log=lambda *_a: None)
    assert status == "error"
    decided = one(events, 'cycle6_muted_replay_decided')
    assert 'status=error' in decided['output'] and 'raised' in decided['reason']


# ---------------------------------------------------------------------------
# 6 — buggy-rate delivery
# ---------------------------------------------------------------------------
def _deliver(*a, **k):
    from java.run import _deliver_buggy_rate
    return _deliver_buggy_rate(*a, **k)


def test_buggy_rate_delivery_records_an_attached_fact(events):
    note = _deliver({"target"}, {"target": (1000, 999)}, False,
                    (None, None), '')
    assert note and note.startswith("[fire-rate fact]")
    considered = one(events, 'cycle6_buggy_rate_considered')
    assert considered['output'] == 'rate_known=True'
    assert one(events, 'cycle6_buggy_rate_decided')['output'] == 'attached'


def test_buggy_rate_delivery_records_that_no_rate_was_known(events):
    """The inertness question 6B keys on: was a rate ever delivered at all?"""
    note = _deliver({"target"}, {}, False, (None, None), '')
    assert note is None
    assert one(events,
               'cycle6_buggy_rate_considered')['output'] == 'rate_known=False'
    decided = one(events, 'cycle6_buggy_rate_decided')
    assert decided['output'] == 'none' and 'no known rate' in decided['reason']


def test_buggy_rate_delivery_records_the_skip_when_already_attached(events):
    note = _deliver({"target"}, {"target": (1000, 999)}, True,
                    (None, None), '')
    assert note is None
    assert one(events, 'cycle6_buggy_rate_considered')['output'] == \
        'rate_known=True'
    assert one(events, 'cycle6_buggy_rate_decided')['output'] == 'skipped'


def test_buggy_rate_delivery_records_an_unremarkable_rate_as_none(events):
    note = _deliver({"target"}, {"target": (1000, 500)}, False,
                    (None, None), '')
    considered = one(events, 'cycle6_buggy_rate_considered')
    assert considered['output'] == 'rate_known=True'
    decided = one(events, 'cycle6_buggy_rate_decided')
    assert (note is None) == (decided['output'] == 'none')


def test_buggy_rate_delivery_never_raises(events):
    """Malformed counts must not take the leg down — and the failure is
    recorded rather than silent."""
    note = _deliver({"target"}, {"target": "not-a-tuple"}, False,
                    (None, None), '')
    assert note is None
    assert one(events, 'cycle6_buggy_rate_decided')['output'] == 'none'


# ---------------------------------------------------------------------------
# Fail-open: a broken recorder can never break a decision
# ---------------------------------------------------------------------------
def _explode(*a, **k):
    raise RuntimeError("recorder is on fire")


def test_a_raising_recorder_cannot_break_adjudicate(monkeypatch):
    monkeypatch.setattr(llm, 'record_event', _explode)
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, _INDISCRIMINATE)
    assert ok is False and "[6B-INDISCRIMINATE-DROP]" in why
    v2 = _StubVerifier(fd_results=[_DUTY_YES])
    ok2, why2 = _adjudicate(v2, _muted("identical"))
    assert ok2 is True


def test_a_raising_recorder_cannot_break_the_iterated_replay(monkeypatch):
    monkeypatch.setattr(llm, 'record_event', _explode)
    from java.execution.fuzz_runner import iterate_muted_replay

    def replay(mute_ids, pass_index):
        return ("clean", set(), "", False)

    status, *_ = iterate_muted_replay(replay, {"target"}, set(),
                                      log=lambda *_a: None)
    assert status == "clean"


def test_a_raising_recorder_cannot_break_the_rate_delivery(monkeypatch):
    monkeypatch.setattr(llm, 'record_event', _explode)
    note = _deliver({"target"}, {"target": (1000, 999)}, False,
                    (None, None), '')
    assert note and note.startswith("[fire-rate fact]")
