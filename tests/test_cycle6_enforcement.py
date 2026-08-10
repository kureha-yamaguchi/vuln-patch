"""Cycle-6 enforcement: two measured facts the judge demonstrably ignored,
turned into decisions the CODE makes.

Background (docs/replay/night20b_analysis.md, "Chronic-FP classification"):
of the eight convicting firings on three CORRECT patches, five had the
clearing fact delivered in the evidence block and were kept anyway, several
with ``CITATION: NONE``. Persuasion failed, so the mechanical facts now decide:

  PART 1  ``fire_rate_fact`` stamps the branch it took as a machine tag, and
          every consumer keys on the TAG (prose only as fallback).
  PART 2  a measured buggy-side fire rate >= INTRINSIC_FIRE_RATIO voids a
          SOUND keep unless the Spec-J family-duty question answers YES.
  PART 3  a CONFIRMED fires-on-both is resolved by ``compare_fired_values``
          BEFORE anything is dropped: identical -> drop (with the same escape),
          different -> the partial-fix conviction, never dropped;
          not-compared -> unknown, never dropped.

Everything here is offline: a stubbed verifier, no JVM, no LLM, zero tokens.
"""
import pytest

from java.relations import evidence_facts as ef
from java.relations.judge_decision import (
    _confirmed_fires_on_both_gate, _indiscriminate_rate_gate, adjudicate)
from java.verifier_replay import reconstruct_evidence_profile


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
# What RelationVerifier.family_duty returns on an LLM error: it fails OPEN.
_DUTY_ERROR = (True, "family-duty check unavailable")


def _adjudicate(v, evidence, fd_prior=None, is_direction_confirmed=False):
    return adjudicate(
        v, harness_source="src", fired_assertion="fired", trusted_values=None,
        concrete_evidence=evidence, code_context=None, pinned_source="src",
        evidence_profile=None, failing_block="block", check_source="src",
        fd_prior=fd_prior, is_direction_confirmed=is_direction_confirmed)


# ---------------------------------------------------------------------------
# PART 1 — the three rate tags
# ---------------------------------------------------------------------------
def test_rate_tag_catch_signal():
    """buggy <= SILENT_FIRE_RATIO, patched high: the discrimination signal."""
    note = ef.fire_rate_fact(20000, 200, 20000, 18000, "")
    assert ef.RATE_CATCH_FACT_TAG in note
    assert ef.rate_profile(note) == 'catch-signal'


@pytest.mark.parametrize("counts", [
    (20000, 15000, 20000, 9000),   # both high
    (1000, 999, None, None),       # buggy >= INTRINSIC_FIRE_RATIO, alone
])
def test_rate_tag_indiscriminate(counts):
    note = ef.fire_rate_fact(*counts, "")
    assert ef.RATE_INDISCRIMINATE_FACT_TAG in note
    assert ef.rate_profile(note) == 'indiscriminate'


@pytest.mark.parametrize("counts", [
    (20000, 3953, 20000, 20000),   # 19.8%: low but NOT silent (the 5A gap)
    (None, None, 20000, 18000),    # patched high, buggy UNMEASURED
])
def test_rate_tag_ambiguous(counts):
    note = ef.fire_rate_fact(*counts, "")
    assert ef.RATE_AMBIGUOUS_FACT_TAG in note
    assert ef.rate_profile(note) == 'ambiguous'


def test_every_emitted_fire_rate_note_carries_exactly_one_rate_tag():
    """No branch may ship untagged — the tag is the interface now."""
    all_tags = {ef.RATE_CATCH_FACT_TAG, ef.RATE_INDISCRIMINATE_FACT_TAG,
                ef.RATE_AMBIGUOUS_FACT_TAG}
    for counts in [(20000, 200, 20000, 18000), (20000, 15000, 20000, 9000),
                   (1000, 999, None, None), (20000, 3953, 20000, 20000),
                   (None, None, 20000, 18000)]:
        note = ef.fire_rate_fact(*counts, "")
        assert note, counts
        assert sum(t in note for t in all_tags) == 1, note


def test_no_fact_means_no_tag():
    """Below every threshold there is still no note at all (no noise)."""
    assert ef.fire_rate_fact(20000, 100, 20000, 100, "") is None


# ---------------------------------------------------------------------------
# PART 1 — tag-first reconstruction, prose only as fallback
# ---------------------------------------------------------------------------
_PROSE_CATCH = ("[fire-rate fact] buggy build 200/20000 = 1%; patched build "
                "18000/20000 = 90% of random valid inputs. fires on 90% of "
                "random valid inputs on the PATCHED build but only 1% on the "
                "buggy build — the check is silent (or near-silent) on the "
                "known-broken code and loud on the patch.")
_PROSE_INDISCRIMINATE = ("[fire-rate fact] buggy build 999/1000 = 100% of "
                         "random valid inputs. fires on essentially every "
                         "input on the buggy build (100%) — the firing is "
                         "intrinsic to the check/setup construction, not a "
                         "detection of the defect.")


def test_rate_profile_prose_fallback_for_untagged_text():
    """Pre-cycle-6 notes (older runs, replayed fixtures) still resolve."""
    assert ef.rate_profile(_PROSE_CATCH) == 'catch-signal'
    assert ef.rate_profile(_PROSE_INDISCRIMINATE) == 'indiscriminate'
    assert ef.rate_profile("nothing rate-shaped in here") is None
    assert ef.rate_profile("") is None


def test_rate_profile_tag_beats_contradicting_prose():
    """Same discipline as [fact:identical-on-both]: the tag decides."""
    blob = ("[fire-rate fact] buggy build 999/1000 = 100% of random valid "
            "inputs. " + ef.RATE_INDISCRIMINATE_FACT_TAG + " the check is "
            "silent (or near-silent) on the known-broken code and loud on "
            "the patch.")
    assert ef.rate_profile(blob) == 'indiscriminate'


def test_rate_profile_denies_first_across_blocks():
    blob = ef.fire_rate_fact(20000, 200, 20000, 18000, "") + "\n" + \
        ef.fire_rate_fact(1000, 999, None, None, "")
    assert ef.rate_profile(blob) == 'indiscriminate'


def test_reconstructed_profile_keys_on_the_tag_not_the_prose():
    """verifier_replay's drift-kill signature was the last prose-parsing site.

    An indiscriminate note must never reconstruct as buggy_silent=True, even
    when the blob elsewhere contains a silence-shaped phrase."""
    tagged = ef.fire_rate_fact(1000, 999, None, None, "")
    profile, missing = reconstruct_evidence_profile(
        tagged + " silent on the buggy build")
    assert profile['buggy_silent'] is False
    assert 'buggy_silent' not in missing

    catch = ef.fire_rate_fact(20000, 200, 20000, 18000, "")
    profile, missing = reconstruct_evidence_profile(catch)
    assert profile['buggy_silent'] is True
    assert 'buggy_silent' not in missing


def test_reconstructed_profile_prose_fallback_survives():
    """No rate tag anywhere -> the pre-existing keyword path is untouched."""
    profile, missing = reconstruct_evidence_profile(
        "the buggy build runs this exact input without firing this check")
    assert profile['buggy_silent'] is True
    profile, missing = reconstruct_evidence_profile("no signals at all")
    assert profile['buggy_silent'] is False
    assert 'buggy_silent' in missing


# ---------------------------------------------------------------------------
# PART 2 — the indiscriminate drop
# ---------------------------------------------------------------------------
# night20b Math-73-c / Closure-62 `null-source-eol-caret`: buggy 999/1000.
_MATH73C = ef.fire_rate_fact(1000, 999, None, None, "")
# night20b Math-30 `overflow-boundary-monotone` / Chart-19's convicting
# relation: buggy 20000/20000 = 100%.
_HUNDRED_PCT = ef.fire_rate_fact(20000, 20000, 20000, 19000, "")
# Just under the intrinsic bar.
_UNDER_BAR = ef.fire_rate_fact(20000, 18000, 20000, 9000, "")


@pytest.mark.parametrize("evidence", [_MATH73C, _HUNDRED_PCT])
def test_indiscriminate_drop_fires_at_or_above_the_intrinsic_bar(evidence):
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, evidence)
    assert ok is False
    assert "[6B-INDISCRIMINATE-DROP]" in why
    assert v.fd_calls == 1


def test_math73c_shape_drops_duty_no():
    """The exact night20b firing: 999/1000 = 100% delivered, kept anyway."""
    assert ef.indiscriminate_buggy_rate(_MATH73C) == pytest.approx(0.999)
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, _MATH73C)
    assert ok is False and "[6B-INDISCRIMINATE-DROP]" in why


def test_chart19_shape_survives_via_family_duty_yes():
    """GUARD. Chart-19's convicting relation also measures buggy 100%; it
    survives because it asserts the failing test's OWN observable. If this
    ever fails, the rule is wrong — the escape must NOT be weakened."""
    assert ef.indiscriminate_buggy_rate(_HUNDRED_PCT) == 1.0
    v = _StubVerifier(fd_results=[_DUTY_YES])
    ok, why = _adjudicate(v, _HUNDRED_PCT)
    assert ok is True and why == "oracle judged sound"
    assert v.fd_calls == 1


def test_chart19_shape_survives_on_fd_prior_yes_without_asking():
    v = _StubVerifier(fd_results=[])
    ok, why = _adjudicate(v, _HUNDRED_PCT, fd_prior=True)
    assert ok is True and why == "oracle judged sound" and v.fd_calls == 0


def test_indiscriminate_drop_does_not_fire_under_the_bar():
    v = _StubVerifier(fd_results=[])
    ok, why = _adjudicate(v, _UNDER_BAR)
    assert ok is True and v.fd_calls == 0


def test_indiscriminate_drop_never_fires_on_the_catch_profile():
    """buggy LOW / patched high is the 5A catch signal, at any patched rate."""
    catch = ef.fire_rate_fact(20000, 200, 20000, 20000, "")
    assert ef.indiscriminate_buggy_rate(catch) is None
    v = _StubVerifier(fd_results=[])
    ok, why = _adjudicate(v, catch)
    assert ok is True and v.fd_calls == 0


def test_indiscriminate_rate_needs_a_measurement():
    """An unmeasured buggy side is never a drop."""
    assert ef.indiscriminate_buggy_rate(
        ef.fire_rate_fact(None, None, 20000, 18000, "")) is None
    assert ef.indiscriminate_buggy_rate("no rates here") is None
    assert ef.indiscriminate_buggy_rate("") is None


def test_indiscriminate_drop_never_flips_a_dismissal_into_a_keep():
    v = _StubVerifier(verify_results=[(False, "UNSOUND: invented contract")],
                      fd_results=[])
    ok, why = _adjudicate(v, _HUNDRED_PCT)
    assert ok is False and why == "UNSOUND: invented contract"
    assert v.fd_calls == 0


# ---------------------------------------------------------------------------
# ITEM 1 — the rate label used to mean five different things at once.
# ``indiscriminate_buggy_rate`` returned a bare None for four distinct
# situations and the trace reported all four as "no rate found", which reads as
# "we never measured" when the commonest case is the opposite. These tests pin
# each situation to its own name, and pin the drop decision to be UNCHANGED.
# ---------------------------------------------------------------------------
_RATE_CASES = [
    ("no-measurement", "", None),
    ("no-measurement", "no rates here", None),
    ("buggy-side-unmeasured", ef.fire_rate_fact(None, None, 20000, 18000, ""),
     None),
    ("below-bar", _UNDER_BAR, 0.90),
    ("at-or-above-bar", _MATH73C, 0.999),
    ("at-or-above-bar", _HUNDRED_PCT, 1.0),
    # Hand-built, NOT via fire_rate_fact: a real catch-signal block has a buggy
    # rate below SILENT_FIRE_RATIO by construction, so it can never reach the
    # intrinsic bar. This pins the belt-and-braces branch that the original
    # function also carried for a combination the fact-builder cannot emit.
    ("catch-profile-skipped",
     "[fire-rate fact] buggy build 20000/20000 = 100%; patched build "
     "200/20000 = 1% of random valid inputs. [fact:rate-catch-signal]",
     1.0),
]


@pytest.mark.parametrize("state,evidence,rate", _RATE_CASES)
def test_rate_diagnosis_names_which_situation_it_saw(state, evidence, rate):
    d = ef.indiscriminate_rate_diagnosis(evidence)
    assert d['state'] == state, f"{state} misreported as {d['state']}"
    assert d['state'] in ef.RATE_STATES
    if rate is None:
        assert d['rate'] is None
    else:
        assert d['rate'] == pytest.approx(rate, abs=1e-3)
    # The detail is what a human reads in the trace; it must not be empty and
    # must never claim nothing was measured when something was.
    assert d['detail']
    if d['rate'] is not None:
        assert 'never measured' not in d['detail']


@pytest.mark.parametrize("state,evidence,rate", _RATE_CASES)
def test_rate_diagnosis_does_not_change_the_drop_decision(state, evidence,
                                                          rate):
    """The whole fix is observability. drop_rate must equal what the old
    single-purpose function returned, for every situation."""
    d = ef.indiscriminate_rate_diagnosis(evidence)
    assert d['drop_rate'] == ef.indiscriminate_buggy_rate(evidence)
    assert (d['drop_rate'] is not None) == (state == 'at-or-above-bar')


def test_below_bar_is_reported_as_measured_and_healthy():
    """The case that caused the misreading: measured, fine, and the old label
    said "no rate found". A reader must be able to tell this from a miss."""
    d = ef.indiscriminate_rate_diagnosis(_UNDER_BAR)
    assert d['state'] == 'below-bar'
    assert d['rate'] == pytest.approx(0.90, abs=1e-3)
    assert 'below' in d['detail'] and 'discriminates' in d['detail']


# ---------------------------------------------------------------------------
# PART 3 — confirmed fires-on-both, resolved by the value comparison
# ---------------------------------------------------------------------------
def _muted(value_verdict):
    """The shipped muted-replay note for a CONFIRMED fires-on-both."""
    return ef.muted_replay_note(
        {"target"}, {"shadow"}, "crashed", {"target"}, None, set(),
        value_verdict=value_verdict,
        buggy_msg_excerpt="expected=2.5 actual=3.5",
        patched_msg_excerpt="expected=2.5 actual=3.5")


def _buggy_replay(value_verdict):
    """The shipped same-check buggy-replay note for a CONFIRMED
    fires-on-both."""
    return ef.semantic_buggy_replay_note(
        {"target"}, "crashed", {"target"}, set(), set(), None,
        value_verdict=value_verdict,
        buggy_msg_excerpt="expected=2.5 actual=3.5",
        patched_msg_excerpt="expected=2.5 actual=9.9")


@pytest.mark.parametrize("build", [_muted, _buggy_replay])
def test_confirmation_tag_is_stamped_by_both_replay_sites(build):
    for verdict in ("identical", "different", "unknown"):
        assert ef.CONFIRMED_BOTH_FACT_TAG in build(verdict)


@pytest.mark.parametrize("build", [_muted, _buggy_replay])
def test_confirmed_verdicts_are_read_from_tags(build):
    assert ef.confirmed_fires_on_both_verdict(
        build("identical")) == 'identical'
    assert ef.confirmed_fires_on_both_verdict(
        build("different")) == 'different'
    assert ef.confirmed_fires_on_both_verdict(
        build("unknown")) == 'not-compared'


def test_no_confirmation_means_no_verdict():
    assert ef.confirmed_fires_on_both_verdict(
        "[fact:identical-on-both] but nothing confirmed it") is None
    assert ef.confirmed_fires_on_both_verdict("") is None
    # Confirmed, but with no value verdict attached: nothing to decide.
    assert ef.confirmed_fires_on_both_verdict(
        ef.CONFIRMED_BOTH_FACT_TAG + " fires on both") is None


@pytest.mark.parametrize("build", [_muted, _buggy_replay])
def test_confirmed_identical_drops(build):
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, build("identical"))
    assert ok is False
    assert v.fd_calls == 1


def test_confirmed_identical_drop_owns_the_deny_first_blind_spot():
    """5C resolves its tags deny-first, so a confirmed-identical fact from one
    site plus an unconfirmed not-compared from another reads as NON-terminal
    there. The CONFIRMED measurement is stronger and 6C decides it."""
    blob = _muted("identical") + "\n[buggy-replay fact] [fact:not-compared] " \
        "values were not compared."
    assert ef.terminal_profile(blob) is None      # 5C stands down
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, blob)
    assert ok is False and "[6C-FIRES-ON-BOTH-DROP]" in why


def test_confirmed_identical_is_escaped_by_family_duty_yes():
    v = _StubVerifier(fd_results=[_DUTY_YES])
    ok, why = _adjudicate(v, _muted("identical"))
    assert ok is True and why == "oracle judged sound"


@pytest.mark.parametrize("build", [_muted, _buggy_replay])
def test_lang63_shape_different_values_is_never_dropped(build):
    """The partial-fix conviction — the strongest evidence the pipeline has.
    It is never dropped by this path, and family-duty is not even consulted."""
    evidence = build("different")
    assert ef.confirmed_fires_on_both_verdict(evidence) == 'different'
    v = _StubVerifier(fd_results=[_DUTY_NO])       # would drop if consulted
    ok, why = _adjudicate(v, evidence)
    assert ok is True and why == "oracle judged sound"
    assert v.fd_calls == 0
    # ...and directly at the gate, with the keep already in hand.
    ok, why = _confirmed_fires_on_both_gate(
        True, "kept", evidence, v, "fired", "block", "src",
        {'value': False, 'why': 'duty NO'})
    assert ok is True and why == "kept"


def test_different_values_wins_over_an_identical_claim_elsewhere():
    """Sites disagreeing resolves deny-first: never a drop."""
    blob = _muted("different") + "\n" + _buggy_replay("identical")
    assert ef.confirmed_fires_on_both_verdict(blob) == 'different'


@pytest.mark.parametrize("build", [_muted, _buggy_replay])
def test_not_compared_is_unknown_and_never_dropped(build):
    """Dropping on unknown is how the marker bug happened; it is not
    relocated into this rule."""
    evidence = build("unknown")
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, evidence)
    assert ok is True and why == "oracle judged sound"
    assert v.fd_calls == 0


def test_not_compared_stays_non_terminal():
    assert 'not-compared' in ef._NON_TERMINAL_FACT_TAGS
    assert ef.terminal_profile(_muted("unknown")) is None
    assert ef.terminal_profile(_buggy_replay("unknown")) is None


def test_confirmed_gate_never_flips_a_dismissal_into_a_keep():
    v = _StubVerifier(verify_results=[(False, "UNSOUND: no contract")],
                      fd_results=[])
    ok, why = _adjudicate(v, _muted("identical"))
    assert ok is False and why == "UNSOUND: no contract"
    assert v.fd_calls == 0


# ---------------------------------------------------------------------------
# Fail-open: no error and no missing measurement may manufacture a drop
# ---------------------------------------------------------------------------
@pytest.mark.parametrize("evidence", [_HUNDRED_PCT, None])
def test_family_duty_llm_error_never_manufactures_a_drop(evidence):
    """RelationVerifier.family_duty fails OPEN to (True, ...) on an LLM
    error; the gates honour that sentinel."""
    v = _StubVerifier(fd_results=[_DUTY_ERROR])
    ok, why = _adjudicate(v, evidence or _muted("identical"))
    assert ok is True and why == "oracle judged sound"


@pytest.mark.parametrize("evidence_builder", [
    lambda: _HUNDRED_PCT, lambda: _muted("identical")])
def test_family_duty_exception_never_manufactures_a_drop(evidence_builder):
    v = _StubVerifier(fd_raises=True)
    ok, why = _adjudicate(v, evidence_builder())
    assert ok is True and why == "oracle judged sound"


def test_verify_transport_error_is_passed_through_unchanged():
    """verify() itself fails open to KEEP; no gate may turn that into a drop
    (no measurement is present in the sentinel text)."""
    v = _StubVerifier(verify_results=[(True, "verifier error (Timeout); "
                                             "keeping finding")])
    ok, why = _adjudicate(v, "")
    assert ok is True and why.startswith("verifier error")
    assert v.fd_calls == 0


def test_rate_gate_returns_the_original_verdict_on_a_parse_error(monkeypatch):
    # Patches indiscriminate_rate_diagnosis, which is what the gate now calls.
    # It used to patch indiscriminate_buggy_rate; when the gate moved to the
    # diagnosis function this test silently stopped exercising the fail-open
    # path and started asserting on a live family-duty call instead. Patch the
    # function the gate actually calls, or the fail-open guard is untested.
    monkeypatch.setattr(ef, 'indiscriminate_rate_diagnosis',
                        lambda t: (_ for _ in ()).throw(ValueError("boom")))
    v = _StubVerifier(fd_results=[])
    ok, why = _indiscriminate_rate_gate(
        True, "kept", _HUNDRED_PCT, v, "fired", "block", "src", {'value': None})
    assert ok is True and why == "kept" and v.fd_calls == 0


def test_confirmed_gate_returns_the_original_verdict_on_a_parse_error(
        monkeypatch):
    monkeypatch.setattr(ef, 'confirmed_fires_on_both_verdict',
                        lambda t: (_ for _ in ()).throw(ValueError("boom")))
    v = _StubVerifier(fd_results=[])
    ok, why = _confirmed_fires_on_both_gate(
        True, "kept", _muted("identical"), v, "fired", "block", "src",
        {'value': None})
    assert ok is True and why == "kept" and v.fd_calls == 0


def test_gates_are_skipped_when_the_firing_is_direction_confirmed():
    """Singly flagged — direction-confirmed, no rate fact: the bypass stands,
    byte for byte. This blob would be dropped by 6C if the gates ran."""
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, _muted("identical"), is_direction_confirmed=True)
    assert ok is True and v.fd_calls == 0


def test_family_duty_is_asked_at_most_once_across_all_gates():
    """Both cycle-6 facts on one firing must not cost two judge calls."""
    blob = _HUNDRED_PCT + "\n" + _muted("identical")
    v = _StubVerifier(fd_results=[_DUTY_NO])
    ok, why = _adjudicate(v, blob)
    assert ok is False
    assert v.fd_calls == 1
