"""Judge canaries — lock the guard<->replay connection through the SINGLE
shared entrypoint ``judge_decision.adjudicate``.

Both tests are fully offline: a stubbed verifier returns scripted verdicts,
so ZERO tokens are spent. They assert that the two shipped guard directions
compose correctly through ``adjudicate`` — the exact decision run.py's judge
sites and verifier_replay.py now both route through.

  (a) Math-2 direction  — an IDENTICAL-ON-BOTH firing whose base verdict is
      UNSOUND on an uncited drift-kill hypothetical is RESCUED by the 5B
      citation-void re-ask, and the 5C terminal gate KEEPS it because
      family-duty already answered YES (fd_prior=True). => ok=True (KEEP).

  (b) Math-30 direction — an IDENTICAL-ON-BOTH / fires-on-buggy firing whose
      base verdict is SOUND is DROPPED by the 5C terminal gate because
      family-duty does NOT apply (fd_prior=False). => ok=False (DROP).
"""
from java.relations.judge_decision import adjudicate


class _StubVerifier:
    """Scripted verifier — no LLM. ``verify`` returns the next scripted
    (ok, why); ``family_duty`` returns the scripted result. Never networks."""

    def __init__(self, verify_results, fd_result=None):
        self._vr = list(verify_results)
        self._fd = fd_result
        self.verify_calls = 0
        self.fd_calls = 0

    def verify(self, **kwargs):
        r = self._vr[min(self.verify_calls, len(self._vr) - 1)]
        self.verify_calls += 1
        return r

    def family_duty(self, *a, **k):
        self.fd_calls += 1
        return self._fd


_DRIFT = {'buggy_silent': True, 'deterministic_trigger': True,
          'patched_firing': True}
_IDENTICAL_EVID = ("[buggy-replay fact] the exact firing input fires the SAME "
                   "check on the BUGGY build with the SAME observed values — "
                   "behaviour at this input is identical on both builds.")




def test_canary_math30_direction_drops_via_terminal_gate():
    # Base verdict SOUND, but the firing carries the IDENTICAL-ON-BOTH /
    # fires-on-buggy fact and family-duty does NOT apply (fd_prior=False) ->
    # 5C terminal gate voids the keep.
    v = _StubVerifier([(True, "oracle judged sound")])
    ok, why = adjudicate(
        v,
        harness_source="// identical-on-both check",
        fired_assertion="[oracle:some-check] fires on buggy build too",
        trusted_values=None,
        concrete_evidence=_IDENTICAL_EVID,
        code_context=None,
        pinned_source=None,
        evidence_profile=None,
        failing_block="",
        check_source="// identical-on-both check",
        fd_prior=False,
    )
    assert ok is False                      # DROP
    assert v.verify_calls == 1              # SOUND base -> no re-ask
    assert v.fd_calls == 0                  # fd_prior=False -> no family_duty ask
    assert "TERMINAL" in why
