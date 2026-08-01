"""8.1 step 1(b) — a fail-open judge must never look like it answered.

Pre-launch check for the judge-model swap, run BEFORE any spend because it is
the July-15 bug's shape: `RelationVerifier.verify` fails open on an LLM
error or an unparseable response, and `reask_verdict_usable` exists so callers
can tell a real verdict from that fail-open. If the sentinel is missing, the
caller adopts a manufactured verdict as if the judge had ruled.

MODEL-SWAP SENSITIVE, which is why it belongs to 8.1 rather than to general
hygiene. The incumbent emits `VERDICT:` as its first line, so an unparsed
response almost always carries no `WHY:` either and the sentinel appeared by
luck. A model that writes a preamble, reorders the fields, or gets truncated
after WHY: would have been silently fail-open on every such call.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from java.relations.evidence_facts import reask_verdict_usable   # noqa: E402
from java.relations.relation_verifier import RelationVerifier    # noqa: E402


UNPARSEABLE = [
    'I could not determine this.',
    'Why: the harness looks unusual to me\n(no verdict line)',
    'Let me think about it.\nWHY: unclear\n',
    'WHY: truncated mid-ans',
    '',
]


def test_every_unparseable_response_is_reported_UNUSABLE():
    """THE CHECK. A response with no parseable verdict must never read as a
    verdict, whatever else it contains."""
    for out in UNPARSEABLE:
        ok, why = RelationVerifier._parse(out)
        assert ok is True, 'no-verdict must fail open to KEEP'
        assert reask_verdict_usable(why) is False, (
            f'unparseable response reported USABLE: {out!r} -> {why!r}')


def test_the_models_own_words_are_preserved_not_discarded():
    """The sentinel is APPENDED, not substituted — the fix must not cost the
    diagnostic value of what the model actually said."""
    _ok, why = RelationVerifier._parse('WHY: the harness looks unusual')
    assert 'no verdict parsed' in why
    assert 'the harness looks unusual' in why


def test_well_formed_verdicts_are_still_usable():
    for out, expect in (('VERDICT: SOUND\nWHY: fine', True),
                        ('VERDICT: UNSOUND\nWHY: counterexample x', False)):
        ok, why = RelationVerifier._parse(out)
        assert ok is expect
        assert reask_verdict_usable(why) is True


class _Gen:
    """Stub generator returning a scripted response."""

    def __init__(self, out):
        self._out = out

    def generate(self, _messages):
        return self._out


def _duty(out):
    v = RelationVerifier.__new__(RelationVerifier)
    v._gen = _Gen(out)
    return v.family_duty('[oracle:x] fired', 'test block', 'check src')


def test_duty_no_verdict_branch_carries_its_sentinel():
    """This branch DROPS, so a suppressed sentinel lets an unparsed response
    read as a deliberate 'duty does not apply' — the same shape as `_parse`,
    in the direction that costs a finding."""
    ok, why = _duty('WHY: I am not sure')
    assert ok is False
    assert 'no DUTY verdict parsed' in why
    assert 'I am not sure' in why


def test_duty_still_parses_a_well_formed_answer():
    assert _duty('DUTY: YES\nWHY: same observable')[0] is True
    assert _duty('DUTY: NO\nWHY: unrelated')[0] is False


def test_duty_fails_OPEN_on_an_api_error():
    """An API error may never cause a drop."""
    class _Boom:
        def generate(self, _m):
            raise RuntimeError('429')
    v = RelationVerifier.__new__(RelationVerifier)
    v._gen = _Boom()
    ok, why = v.family_duty('a', 'b', 'c')
    assert ok is True
    assert reask_verdict_usable(why) is False


def test_error_path_sentinel_keys_on_OUR_wording_not_the_providers():
    """Why a new deployment's error strings cannot evade the check: the
    exception is wrapped into our own 'verifier error (...)' prefix, so the
    sentinel does not depend on the provider's error text at all."""
    assert reask_verdict_usable(
        'verifier error (SomeBrandNewProviderError: 429 whatever); '
        'keeping finding') is False
