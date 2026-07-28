"""Replay gate-fidelity — the offline replay must measure the decision
PRODUCTION makes, not a stricter one.

The bug this locks down: ``verifier_replay.py`` used to hard-code
``fd_prior=None`` into ``adjudicate``. For a firing carrying an
identical-on-both-builds fact, the 5C terminal gate then freshly asked
family-duty, got NO, and dropped a catch the pipeline KEEPS — run.py's Spec-J
ladder had already settled that firing (trigger-input exemption sets
``fd_prior=True``, commit 4efdeb0). The replay was therefore stricter than the
thing it claims to measure.

Both tests are fully offline (stubbed verifier / no LLM, no subprocess): ZERO
tokens.

  (a) decision  — fd_prior=True + IDENT-carrying evidence + a SOUND base
      verdict SURVIVES the 5C gate, and family_duty is never consulted.
  (b) plumbing  — verifier_replay passes the case's reconstructed fd_prior
      through to adjudicate instead of a hard-coded None.
"""
import json
import os
import sys

import pytest

from java.relations.evidence_facts import carries_terminal_identical_fact

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FIXTURE = os.path.join(REPO, 'tests', 'fixtures', 'cases228.jsonl')

# Wording lifted from the shipped buggy-replay note; the 5C detector treats it
# as the terminal identical fact.
IDENT_EVIDENCE = (
    "[buggy-replay fact] the exact firing input fires the SAME check on the "
    "BUGGY build with the SAME observed values — behaviour at this input is "
    "identical on both builds.")


class _StubVerifier:
    """Scripted verifier — no LLM. Records whether family_duty was consulted."""

    def __init__(self, verdict, fd_result=(False, "duty does not apply")):
        self._verdict = verdict
        self._fd = fd_result
        self.verify_calls = 0
        self.fd_calls = 0

    def verify(self, **kwargs):
        self.verify_calls += 1
        return self._verdict

    def family_duty(self, *a, **k):
        self.fd_calls += 1
        return self._fd


def _adjudicate(fd_prior, verifier):
    from java.relations.judge_decision import adjudicate
    return adjudicate(
        verifier,
        harness_source="class FuzzHarness {}",
        fired_assertion="[oracle:x] semantic mismatch: expected 1 but was 2",
        trusted_values=None,
        concrete_evidence=IDENT_EVIDENCE,
        code_context=None,
        pinned_source="class FuzzHarness {}",
        evidence_profile=None,
        failing_block="public void testX() {}",
        check_source="class FuzzHarness {}",
        fd_prior=fd_prior)


def test_ident_evidence_is_gate_relevant():
    """Guard the premise: this evidence really does arm the 5C gate."""
    assert carries_terminal_identical_fact(IDENT_EVIDENCE)


def test_fd_prior_true_survives_the_terminal_gate():
    """fd_prior=True + IDENT + SOUND -> KEPT, and family_duty is not re-asked."""
    v = _StubVerifier((True, "SOUND: documented formula violated"))
    ok, why = _adjudicate(True, v)
    assert ok is True, f"reconstructed keep was dropped: {why}"
    assert v.fd_calls == 0, "family_duty re-asked despite a settled prior"


def test_fd_prior_none_reproduces_the_old_stricter_drop():
    """The regression itself: with no prior the gate asks and drops."""
    v = _StubVerifier((True, "SOUND: documented formula violated"))
    ok, why = _adjudicate(None, v)
    assert ok is False
    assert v.fd_calls == 1
    assert "TERMINAL" in why


def test_verifier_replay_passes_fd_prior_through(monkeypatch, tmp_path):
    """Call-site check: the replay hands adjudicate the case's fd_prior."""
    sys.path.insert(0, os.path.join(REPO, 'src'))
    import java.verifier_replay as vr

    seen = {}

    def _fake_adjudicate(verifier, **kw):
        seen.setdefault('fd_prior', []).append(kw.get('fd_prior'))
        return True, "stub"

    class _FakeRV:
        def __init__(self, *a, **k):
            pass

    monkeypatch.setattr(vr, 'adjudicate', _fake_adjudicate)
    monkeypatch.setattr(vr, 'RelationVerifier', _FakeRV)
    monkeypatch.setattr(vr, 'usage_totals', lambda: {
        'total_tokens': 0, 'prompt_tokens': 0, 'completion_tokens': 0,
        'calls': 0})
    monkeypatch.setattr(vr, 'token_usage', lambda: {})

    cases = tmp_path / 'cases.jsonl'
    with open(cases, 'w', encoding='utf-8') as fh:
        for cid, fd, unres in (('keeps_prior', True, False),
                               ('no_prior', None, False),
                               ('unresolved', None, True)):
            fh.write(json.dumps({
                'id': cid, 'harness_source': 'class H {}',
                'fired_assertion': 'boom', 'label': 'overfitting',
                'concrete_evidence': IDENT_EVIDENCE,
                'fd_prior': fd, 'fd_prior_unresolved': unres}) + '\n')
    out = tmp_path / 'out'
    monkeypatch.setattr(sys, 'argv',
                        ['verifier_replay.py', '--cases', str(cases),
                         '--out', str(out), '--repeats', '1'])
    vr.main()

    assert seen['fd_prior'] == [True, None, None], (
        f"fd_prior not threaded through: {seen}")
    summary = (out / 'summary.md').read_text()
    # the unresolved case is run and reported, but kept out of the totals
    assert 'unresolved-ladder (1 cases' in summary
    assert 'scored: 2 cases' in summary
    results = [json.loads(x) for x in
               (out / 'results.jsonl').read_text().splitlines()]
    assert {r['id'] for r in results} == {'keeps_prior', 'no_prior',
                                          'unresolved'}


@pytest.mark.parametrize('field', ['fd_prior', 'fd_prior_source',
                                   'fd_prior_unresolved'])
def test_fixture_carries_reconstructed_fd_prior(field):
    """Every fixture case must carry the reconstruction, streamed line by line
    (the file is ~8 MB — never load it whole)."""
    n = 0
    with open(FIXTURE, encoding='utf-8') as fh:
        for line in fh:
            if not line.strip():
                continue
            c = json.loads(line)
            assert field in c, f"case {c.get('id')} missing {field}"
            n += 1
    assert n == 228


def test_unresolved_cases_are_exactly_the_unrecoverable_ones():
    """An unresolved case must have no reconstructed prior, and a resolved one
    must never be silently guessed."""
    with open(FIXTURE, encoding='utf-8') as fh:
        for line in fh:
            if not line.strip():
                continue
            c = json.loads(line)
            if c['fd_prior_unresolved']:
                assert c['fd_prior'] is None
                assert 'no family-duty event recorded' in c['fd_prior_source']
            elif c['fd_prior'] is not None:
                assert 'family-duty DUTY:' in c['fd_prior_source']
