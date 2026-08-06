"""Deterministic proof that removing 5B changes nothing.

WHY NOT A LIVE REPLAY. The requested proof was "replay the fixture, show zero
verdict changes". A live replay CANNOT show that: the noise floor measured the
same model flipping 9.2% of verdicts between two identical draws, so ~20 rows
would differ for reasons unrelated to 5B, and the proof would be unreadable at a
cost of ~5M tokens.

So the verifier is SCRIPTED. Every row gets a fixed verdict, the deterministic
decision path runs for real, and the comparison is exact. That removes the only
source of nondeterminism and makes "zero changes" a checkable claim rather than
a statistical one.

ADVERSARIAL BY DESIGN. 5B only engages on a DISMISSAL, and its citation-void
branch only on a dismissal with no usable citation. So the script returns exactly
that -- an uncited dismissal -- on every row. This is the input shape most likely
to wake 5B, not a representative one. If 5B stays silent here, it is inert.

Output: one JSON line per row with the verdict and the full event stream.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

import llm                                                       # noqa: E402
from java.relations.judge_decision import adjudicate             # noqa: E402

FIXTURE = ROOT / 'tests' / 'fixtures' / 'cases228_untruncated.jsonl'


class ScriptedVerifier:
    """Always dismisses, never cites -- the shape 5B exists to catch."""

    def __init__(self):
        self.calls = 0

    def verify(self, **kw):
        self.calls += 1
        return False, ("a correct implementation could legitimately return a "
                       "different value here\nCITATION: NONE")

    def family_duty(self, fired_assertion, failing_test_block, check_source):
        return False, 'unrelated observable'


# POSITIVE CONTROL. A zero over inputs that cannot reach the mechanism proves
# nothing (rule 15's corollary). This row carries the full drift-kill signature
# -- buggy silent + deterministic trigger + patched firing -- which is the ONLY
# state in which 5B's citation-void branch engages at all. Before the deletion
# it MUST fire here; after, it must not. That is what makes the 220 real rows'
# "no change" a measurement rather than an assumption: it proves the harness can
# see 5B when 5B is there.
CONTROL = {
    'id': '__positive_control__',
    'harness_source': 'class FuzzHarness { void f() { assertTrue(x); } }',
    'fired_assertion': '[oracle:ctl] semantic mismatch: expected=1 actual=2',
    'trusted_values': ['1'],
    'concrete_evidence': '== Java Exception: [oracle:ctl] mismatch',
    'code_context': 'public int f() { return 2; }',
    'failing_test': 'public void testF() { assertEquals(1, f()); }',
    '__profile__': {'buggy_silent': True, 'deterministic_trigger': True,
                    'patched_firing': True},
}


def main(out_path):
    rows = [json.loads(l) for l in FIXTURE.open()]
    rows = [r for r in rows if not r.get('_still_truncated')]
    rows = [CONTROL] + rows
    n_5b = 0
    with open(out_path, 'w') as f:
        for r in rows:
            llm.enable_recording()
            llm.reset_events()
            v = ScriptedVerifier()
            try:
                ok, why = adjudicate(
                    v,
                    harness_source=r.get('harness_source'),
                    fired_assertion=r.get('fired_assertion'),
                    trusted_values=r.get('trusted_values'),
                    concrete_evidence=r.get('concrete_evidence'),
                    code_context=r.get('code_context'),
                    pinned_source=r.get('harness_source'),
                    evidence_profile=r.get('__profile__'),
                    failing_block=r.get('failing_test') or '',
                    check_source=r.get('harness_source') or '',
                    fd_prior=None,
                )
                err = None
            except Exception as e:
                ok, why, err = None, None, f'{type(e).__name__}: {e}'
            events = [{k: ev.get(k) for k in ('kind', 'method', 'output')}
                      for ev in llm.get_events()]
            if why and '5B-INADMISSIBLE' in str(why):
                n_5b += 1
            f.write(json.dumps({
                'id': r['id'], 'ok': ok, 'why': why, 'error': err,
                'verify_calls': v.calls, 'events': events}) + '\n')
    print(f'rows: {len(rows)}   5B-INADMISSIBLE keeps: {n_5b}')


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'proof.jsonl')
