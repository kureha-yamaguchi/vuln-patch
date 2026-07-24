"""Cycle-3 item 1: the MECHANICAL identical-drop family-duty gate.

RelationVerifier.family_duty asks ONE narrow judged question — never the
general soundness rubric — when a kv-certified fact proves the observed
behaviour is identical on both builds. These tests pin:
  * the narrow prompt's content (the question is present; the two load-bearing
    phrases are present; the full-rubric markers are absent),
  * the strict parser (DUTY: YES -> keep; DUTY: NO / garbage -> drop),
  * fail-open (an LLM error keeps the finding, never drops it).

A monkeypatched generator is used throughout — no real LLM, no I/O.
"""
from java.relations.relation_verifier import RelationVerifier


class _CaptureGen:
    """Fake HarnessGenerator: records the messages it is handed and returns a
    canned reply (or raises, when `raises=True`)."""

    def __init__(self, reply="", raises=False):
        self.reply = reply
        self.raises = raises
        self.calls = []

    def generate(self, messages):
        self.calls.append(messages)
        if self.raises:
            raise RuntimeError("boom — simulated API failure")
        return self.reply


def _user_text(gen):
    """The user-role content of the single captured call."""
    assert len(gen.calls) == 1, "family_duty must make exactly one LLM call"
    msgs = gen.calls[0]
    return next(m['content'] for m in msgs if m['role'] == 'user')


# --------------------------------------------------------------------------
# Prompt-content assertions
# --------------------------------------------------------------------------

def test_prompt_contains_the_narrow_question():
    gen = _CaptureGen(reply="DUTY: YES\nWHY: ok")
    rv = RelationVerifier(generator=gen)
    rv.family_duty("assert x == y", "failing test source", "check src")
    user = _user_text(gen)
    assert "The ONLY ground to keep this finding is the patch-failed-to-fix" \
        " pattern" in user
    assert "DUTY: YES | NO" in user
    assert "WHY:" in user


def test_prompt_contains_load_bearing_phrases():
    gen = _CaptureGen(reply="DUTY: YES\nWHY: ok")
    rv = RelationVerifier(generator=gen)
    rv.family_duty("msg", "ft", "cs")
    user = _user_text(gen)
    assert "THE VERY behaviour" in user
    assert "beyond the test's own" in user


def test_prompt_omits_full_soundness_rubric_markers():
    # The whole point of cycle 3: this gate must NOT re-run the general
    # soundness rubric. Its signature markers must be absent from both the
    # user prompt and the system prompt.
    gen = _CaptureGen(reply="DUTY: NO\nWHY: unrelated")
    rv = RelationVerifier(generator=gen)
    rv.family_duty("msg", "ft", "cs")
    msgs = gen.calls[0]
    whole = "\n".join(m['content'] for m in msgs)
    assert "ROUNDING FLOOR" not in whole
    assert "STRUCTURALLY IMPOSSIBLE" not in whole
    assert "REASONING PROTOCOL" not in whole


def test_prompt_threads_all_three_inputs():
    gen = _CaptureGen(reply="DUTY: YES\nWHY: ok")
    rv = RelationVerifier(generator=gen)
    rv.family_duty("FIRED_MSG_SENTINEL", "FAILTEST_SENTINEL", "CHECKSRC_SENTINEL")
    user = _user_text(gen)
    assert "FIRED_MSG_SENTINEL" in user
    assert "FAILTEST_SENTINEL" in user
    assert "CHECKSRC_SENTINEL" in user


# --------------------------------------------------------------------------
# Strict parser
# --------------------------------------------------------------------------

def test_parse_duty_yes_keeps():
    rv = RelationVerifier(generator=_CaptureGen(
        reply="DUTY: YES\nWHY: the check asserts the failing test's own value"))
    keep, why = rv.family_duty("m", "ft", "cs")
    assert keep is True
    assert "failing test" in why


def test_parse_duty_no_drops():
    rv = RelationVerifier(generator=_CaptureGen(
        reply="DUTY: NO\nWHY: unrelated pre-existing surface"))
    keep, why = rv.family_duty("m", "ft", "cs")
    assert keep is False
    assert "pre-existing" in why


def test_parse_garbage_drops():
    # Anything not affirmatively DUTY: YES -> drop.
    rv = RelationVerifier(generator=_CaptureGen(
        reply="I think maybe it could be considered relevant, hard to say."))
    keep, _ = rv.family_duty("m", "ft", "cs")
    assert keep is False


def test_parse_empty_drops():
    rv = RelationVerifier(generator=_CaptureGen(reply=""))
    keep, _ = rv.family_duty("m", "ft", "cs")
    assert keep is False


def test_parse_yes_no_space_keeps():
    rv = RelationVerifier(generator=_CaptureGen(reply="DUTY:YES\nWHY: x"))
    keep, _ = rv.family_duty("m", "ft", "cs")
    assert keep is True


def test_parse_leading_noise_then_no_drops():
    rv = RelationVerifier(generator=_CaptureGen(
        reply="Let me think.\nDUTY: NO\nWHY: different observable"))
    keep, _ = rv.family_duty("m", "ft", "cs")
    assert keep is False


# --------------------------------------------------------------------------
# Fail-open: an LLM error must NEVER cause a drop
# --------------------------------------------------------------------------

def test_llm_error_fails_open_keep():
    rv = RelationVerifier(generator=_CaptureGen(raises=True))
    keep, why = rv.family_duty("m", "ft", "cs")
    assert keep is True
    assert "unavailable" in why
    assert "fail open" in why
