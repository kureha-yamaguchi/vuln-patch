"""Cycle-6 item 6 — the universal screen's DELIVERY path, and the events that
make a missing rate impossible to overlook.

Why this file exists (docs/replay/smoke30_analysis.md + the diagnosis that
followed it): Math-30's surviving accusation was a harness firing whose 6B
event read ``no rate found — verdict unchanged``. The analysis inferred from
that, wrongly, that the buggy-side measurement had never been taken. It HAD
been: the archived trace's ``universal_screen_entry`` for that firing reads
``matched=True rate_known=True`` and the delivery event names the counts,
``buggy=18420/20000 patched=(None, None) -> none (rate unremarkable)``. The
rate was measured, known, and then silently DISCARDED, because
``fire_rate_fact`` had no branch for "buggy side measured, patched side not
measured, below the intrinsic bar" — every other branch needs a patched rate.

Two things are pinned here:

  1. DELIVERY — a buggy-side rate that IS known reaches the judging evidence,
     and does so without moving any mechanical decision (6B still needs
     ``INTRINSIC_FIRE_RATIO``; the 5D two-sided profile still needs a patched
     rate). No new threshold: the new branch uses the shipped ``MAX_FIRE_RATIO``
     indiscriminate cap.
  2. VISIBILITY — ``_universal_screen_step`` records WHY on every path that
     produces nothing (skipped / cached / capped / not-instrumented /
     compile-failed / no-counts / raised), and ``_deliver_buggy_rate`` records
     ``cycle6_rate_absent`` whenever a firing reaches judging with no rate.
     Before this, a missing target id, a non-compiling counting variant and a
     countless run were indistinguishable from the outside.

Offline: stubbed instrument/compile/count callables, no JVM, no LLM, zero
tokens.
"""
import pytest

import llm
from java.relations import evidence_facts as ef
from java.run import (_UNIVERSAL_SCREEN_CAP, _deliver_buggy_rate,
                      _universal_screen_step)


# ---------------------------------------------------------------------------
# Capture harness — the events the pipeline WOULD write into trace.md.
# ---------------------------------------------------------------------------
@pytest.fixture
def events(monkeypatch):
    seen = []

    def _capture(kind, **fields):
        seen.append(dict(kind=kind, **fields))

    monkeypatch.setattr(llm, 'record_event', _capture)
    return seen


def of(seen, method):
    return [e for e in seen if e.get('method') == method]


def one(seen, method):
    hits = of(seen, method)
    assert len(hits) == 1, f"expected exactly 1 {method}, got {len(hits)}"
    return hits[0]


def decided(seen):
    return one(seen, 'cycle6_universal_screen_decided')


# Stub callables. `_ok_*` succeed; `_boom` raises.
def _instrument_ok(source, oid):
    return "class Counting_%s {}" % oid.replace('-', '_')


def _instrument_none(source, oid):
    return None


def _compile_ok(instrumented):
    return object()


def _compile_fail(instrumented):
    return None


def _count(checked, violated):
    return lambda build: (checked, violated)


def _count_none(build):
    return None


def _boom(*a, **k):
    raise RuntimeError("stubbed failure")


def _step(oid='target', source='class H {}', rate_known=False, cache=None,
          measured=0, instrument=_instrument_ok, compile_variant=_compile_ok,
          count=None, cap=_UNIVERSAL_SCREEN_CAP):
    return _universal_screen_step(
        oid, source, rate_known, {} if cache is None else cache, measured,
        instrument, compile_variant,
        _count(1000, 900) if count is None else count, cap=cap)


# ---------------------------------------------------------------------------
# 1 — the "already known" question is per-ORACLE, never per-leg.
# ---------------------------------------------------------------------------
def test_a_known_rate_for_this_oracle_skips_a_new_measurement(events):
    notes, measured, counts, outcome = _step(rate_known=True)
    assert (notes, counts, outcome) == ([], None, 'skipped')
    assert measured == 0, "a skip must not spend measurement budget"
    assert 'already known for THIS oracle' in decided(events)['reason']


def test_another_oracles_known_rate_does_not_suppress_this_one(events):
    """The per-leg/per-oracle distinction. `rate_known` is computed by the
    caller as `oid in _buggy_rate_counts`; the step consults `cache` under the
    given oid ONLY, so a sibling oracle's entry cannot gate this measurement
    off."""
    buggy_rate_counts = {'sibling': (1000, 999)}
    cache = {'sibling': ["[fire-rate fact] sibling's note"]}
    oid = 'target'
    rate_known = oid in buggy_rate_counts          # the call site's own gate
    assert rate_known is False

    notes, measured, counts, outcome = _step(
        oid=oid, rate_known=rate_known, cache=cache, count=_count(1000, 900))
    assert outcome == 'measured' and counts == (1000, 900) and measured == 1
    assert cache['sibling'] == ["[fire-rate fact] sibling's note"]
    assert notes and notes[0].startswith('[fire-rate fact]')


def test_a_repeat_firing_reattaches_the_cached_facts_without_remeasuring(
        events):
    cache = {'target': ['[fire-rate fact] measured earlier']}
    notes, measured, counts, outcome = _step(
        cache=cache, rate_known=True, instrument=_boom)
    assert outcome == 'cached'
    assert notes == ['[fire-rate fact] measured earlier']
    assert measured == 0 and counts is None
    assert 'no new measurement' in decided(events)['reason']


# ---------------------------------------------------------------------------
# 2 — the cap, and what it spends its budget on.
# ---------------------------------------------------------------------------
def test_the_cap_names_the_fired_oracle_it_skipped(events):
    notes, measured, counts, outcome = _step(measured=_UNIVERSAL_SCREEN_CAP,
                                             oid='u-complement-small')
    assert (notes, counts, outcome) == ([], None, 'capped')
    reason = decided(events)['reason']
    assert 'u-complement-small' in reason, "the skipped oracle must be named"
    assert 'no rate' in reason


def test_the_cap_budget_is_spent_only_on_oracles_that_reach_measurement():
    """Every oracle the screen considers has already FIRED (the screen is lazy
    at judging), so prioritisation reduces to not burning budget on oracles
    that cost nothing: a skip, a cache hit and a failed instrumentation must
    not consume a slot that a measurable fired oracle could use."""
    cache = {'cached-oid': []}
    assert _step(rate_known=True)[1] == 0
    assert _step(oid='cached-oid', cache=cache)[1] == 0
    assert _step(instrument=_instrument_none)[1] == 0
    # Only a variant that actually got built spends a slot.
    assert _step(compile_variant=_compile_fail)[1] == 1
    assert _step()[1] == 1


def test_under_the_cap_the_screen_still_measures(events):
    _, measured, counts, outcome = _step(measured=_UNIVERSAL_SCREEN_CAP - 1,
                                         count=_count(781, 781))
    assert outcome == 'measured' and counts == (781, 781)
    assert measured == _UNIVERSAL_SCREEN_CAP


# ---------------------------------------------------------------------------
# 3 — every path that produces no rate says WHY (and fails open).
# ---------------------------------------------------------------------------
@pytest.mark.parametrize("kwargs,outcome,phrase", [
    (dict(oid=None), 'skipped', 'no oracle id'),
    (dict(instrument=_instrument_none), 'not-instrumented', 'not found'),
    (dict(compile_variant=_compile_fail), 'compile-failed', 'did not compile'),
    (dict(count=_count_none), 'no-counts', 'no [relscreen] counts'),
    (dict(instrument=_boom), 'raised', 'RuntimeError'),
    (dict(compile_variant=_boom), 'raised', 'RuntimeError'),
    (dict(count=_boom), 'raised', 'RuntimeError'),
])
def test_every_absent_path_records_why(events, kwargs, outcome, phrase):
    notes, _measured, counts, got = _step(**kwargs)
    assert got == outcome
    ev = decided(events)
    assert ev['output'] == outcome
    assert phrase in ev['reason'], ev['reason']
    # FAIL-OPEN: nothing measured, nothing manufactured, in either direction.
    assert notes == [] and counts is None


def test_a_failure_never_raises_into_the_pipeline():
    for kwargs in (dict(instrument=_boom), dict(compile_variant=_boom),
                   dict(count=_boom), dict(cache={'x': None}, oid='x')):
        notes, _m, counts, _o = _step(**kwargs)
        assert counts is None or isinstance(counts, tuple)
        assert isinstance(notes, list)


def test_a_broken_recorder_never_breaks_the_step(monkeypatch):
    monkeypatch.setattr(llm, 'record_event', _boom)
    notes, measured, counts, outcome = _step(count=_count(1000, 990))
    assert outcome == 'measured' and counts == (1000, 990) and measured == 1
    assert notes and notes[0].startswith('[fire-rate fact]')


# ---------------------------------------------------------------------------
# 4 — a measured screen produces the facts it paid for.
# ---------------------------------------------------------------------------
def test_a_measurement_records_its_counts_and_caches_them(events):
    cache = {}
    notes, measured, counts, outcome = _step(cache=cache,
                                             count=_count(773, 773))
    assert outcome == 'measured' and counts == (773, 773) and measured == 1
    assert cache['target'] == notes
    assert any(n.startswith('[universal-screen fact]') for n in notes), \
        "violated == checked is the never-held fact"
    assert 'violated=773/773' in decided(events)['reason']


def test_a_measurement_with_no_checked_inputs_reports_no_counts(events):
    """checked == 0 is not a rate — deliver nothing rather than divide by it."""
    notes, _m, counts, outcome = _step(count=_count(0, 0))
    assert outcome == 'measured' and counts is None
    assert notes == []


# ---------------------------------------------------------------------------
# 5 — the DELIVERY gap this cycle closes (Math-30 u-complement-small).
# ---------------------------------------------------------------------------
_MATH30 = (20000, 18420)      # checked, violated — 92%, patched side unmeasured


def test_a_known_buggy_only_rate_now_reaches_the_evidence(events):
    """The regression itself: before this fix `fire_rate_fact` returned None
    for a measured 92% buggy rate with no patched counts, so the firing reached
    the judge carrying no rate and 6B reported 'no rate found'."""
    note = _deliver_buggy_rate({'target'}, {'target': _MATH30}, False,
                               (None, None), '')
    assert note is not None, "a KNOWN rate must reach the judging evidence"
    assert note.startswith('[fire-rate fact]')
    assert '18420/20000' in note and '92%' in note
    assert one(events, 'cycle6_buggy_rate_decided')['output'] == 'attached'
    assert of(events, 'cycle6_rate_absent') == []




def test_the_intrinsic_and_silent_readings_are_unchanged():
    """Guard rails on the new branch: the intrinsic branch still owns >= 0.95
    (6B's bar), and Math-65's 19.8% buggy-only rate still delivers nothing —
    the shipped decision that 'under the cap' is not a statement."""
    intrinsic = ef.fire_rate_fact(20000, 20000, None, None, '')
    assert ef.indiscriminate_buggy_rate(intrinsic) == 1.0
    assert ef.fire_rate_fact(20000, 3953, None, None, '') is None
    assert ef.fire_rate_fact(None, None, None, None, '') is None


def test_a_firing_with_no_measured_rate_records_why_it_is_absent(events):
    note = _deliver_buggy_rate({'target'}, {}, False, (None, None), '')
    assert note is None
    absent = one(events, 'cycle6_rate_absent')
    assert absent['output'] == 'no-rate'
    assert 'no buggy-side counts exist' in absent['reason']
    assert 'cycle6_universal_screen_decided' in absent['reason'], \
        "the absence event must point at the step that declined"


def test_a_known_but_unstatable_rate_records_why_it_is_absent(events):
    """Below every branch's bar (5% buggy, patched unmeasured): still nothing
    to say, but now the silence is on the record instead of invisible."""
    note = _deliver_buggy_rate({'target'}, {'target': (20000, 1000)}, False,
                               (None, None), '')
    assert note is None
    absent = one(events, 'cycle6_rate_absent')
    assert '1000/20000' in absent['reason']
    assert 'reaches judging with no rate' in absent['reason']


def test_a_rate_already_attached_upstream_is_not_an_absence(events):
    _deliver_buggy_rate({'target'}, {'target': _MATH30}, True, (None, None),
                        '')
    assert one(events, 'cycle6_buggy_rate_decided')['output'] == 'skipped'
    assert of(events, 'cycle6_rate_absent') == [], \
        "a rate IS in the evidence — that is not an absent rate"


def test_delivery_failure_is_recorded_as_an_absence_and_never_raises(events):
    class Exploding(dict):
        def get(self, *a, **k):
            raise RuntimeError("stubbed failure")

    note = _deliver_buggy_rate({'target'}, Exploding(target=_MATH30), False,
                               (None, None), '')
    assert note is None
    absent = one(events, 'cycle6_rate_absent')
    assert 'raised' in absent['reason'] and 'fail-open' in absent['reason']
