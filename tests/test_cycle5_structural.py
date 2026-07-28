"""Cycle-5 iteration 3: the two prose-matching mechanisms replaced by
structural ones. Fully offline — a stubbed verifier, no JVM, no LLM, no
tokens.

PART A — demand the QUOTE, not the vibe. The soundness judge must now answer a
third line, ``CITATION: "<verbatim quote>" | NONE``. A dismissal under the
drift-kill signature is void when the judge declares NONE, or when the quote it
gives is not LITERALLY present in the material it was shown. Keyword matching
of the judge's prose survives only as the fallback for a missing/garbled line.

PART B — carry the fact as a FACT. The note builders stamp a machine-readable
``[fact:...]`` tag into their own text at emission, and the terminal gate keys
on the TAG; the marker+veto prose path runs only for untagged (older/replayed)
evidence.

Both replace mechanisms that failed three times in this cycle by reading a
substring in the opposite of its plain sense.
"""
import pytest

from java.relations.evidence_facts import (
    citation_is_grounded,
    citation_line_status,
    citation_void_decision,
    fact_tags,
    muted_replay_note,
    parse_citation_line,
    semantic_buggy_replay_note,
    strip_citation_line,
    terminal_profile,
)
from java.relations.judge_decision import _guarded_verify, adjudicate
from java.relations.relation_verifier import RelationVerifier

# The full drift-kill signature (all three profile flags).
_DRIFT = {'buggy_silent': True, 'deterministic_trigger': True,
          'patched_firing': True}
_PARTIAL = {'buggy_silent': False, 'deterministic_trigger': True,
            'patched_firing': True}

# A stand-in for the harness/code material the judge is shown.
_CONTEXT = (
    "public void check(Locale la, Locale lb) {\n"
    "    // Returns the canonical form; may return any value in the range.\n"
    "    if (la != lb) throw new AssertionError(\"locales differ\");\n"
    "}\n"
)


class _StubVerifier:
    """Scripted verify() results; counts calls. No network."""

    def __init__(self, verify_results):
        self._vr = list(verify_results)
        self.verify_calls = 0
        self.kwargs_seen = []

    def verify(self, **kwargs):
        self.kwargs_seen.append(kwargs)
        r = self._vr[min(self.verify_calls, len(self._vr) - 1)]
        self.verify_calls += 1
        return r

    def family_duty(self, *a, **k):
        return True, "family duty applies"


def _guard(v, profile=_DRIFT, context=_CONTEXT, **extra):
    kw = dict(harness_source=context, concrete_evidence="", code_context=None,
              trusted_values=None)
    kw.update(extra)
    return _guarded_verify(v, kw, pinned=None, evidence_profile=profile)


def _why(text, citation):
    """A verdict WHY as RelationVerifier._parse now returns it."""
    return text if citation is None else (text + "\nCITATION: " + citation)


# ---------------------------------------------------------------------------
# (a) parse_citation_line: quoted / NONE / missing / garbled
# ---------------------------------------------------------------------------
def test_parse_citation_line_quoted():
    assert parse_citation_line(
        'WHY: x\nCITATION: "if (la != lb) throw"') == 'if (la != lb) throw'
    # typographic quotes, and an unquoted line, both yield the passage
    assert parse_citation_line('CITATION: “may return any value”') \
        == 'may return any value'
    assert parse_citation_line('CITATION: may return any value') \
        == 'may return any value'
    # the LAST line wins (a re-ask answer appended after an earlier one)
    assert parse_citation_line(
        'CITATION: "first"\nWHY: y\nCITATION: "second"') == 'second'


def test_parse_citation_line_none():
    assert parse_citation_line('WHY: x\nCITATION: NONE') is None
    assert parse_citation_line('CITATION: none') is None
    assert parse_citation_line('CITATION: NONE (nothing shown supports it)') \
        is None
    assert citation_line_status('CITATION: NONE')[0] == 'none'


@pytest.mark.parametrize("text", [
    "",
    None,
    "WHY: a correct implementation could do otherwise",   # no line at all
    "CITATION:",                                          # header, nothing after
    "CITATION:    ",
    'CITATION: "<verbatim quote from the shown material>"',  # placeholder
    "CITATION: n/a",
])
def test_parse_citation_line_missing_or_garbled(text):
    assert parse_citation_line(text) is None
    assert citation_line_status(text)[0] == 'missing'


def test_strip_citation_line_leaves_the_why_alone():
    why = _why("a correct printer could emit a separator", '"shown"')
    assert strip_citation_line(why) == "a correct printer could emit a separator"


# ---------------------------------------------------------------------------
# (b) citation_is_grounded: verbatim yes, paraphrase no, hallucination no
# ---------------------------------------------------------------------------
def test_grounded_on_a_verbatim_substring():
    assert citation_is_grounded("if (la != lb) throw", _CONTEXT) is True
    # normalisation only: case and whitespace runs
    assert citation_is_grounded("IF (la   !=   lb)\n throw", _CONTEXT) is True


def test_not_grounded_on_a_paraphrase():
    assert citation_is_grounded(
        "the check compares locales by object identity", _CONTEXT) is False


def test_not_grounded_on_a_hallucinated_quote():
    assert citation_is_grounded(
        "@throws IllegalArgumentException if the locale is null",
        _CONTEXT) is False


def test_grounded_across_any_supplied_context_including_lists():
    assert citation_is_grounded("2.5e-7", None, "", ["1.0", "2.5e-7"]) is True
    assert citation_is_grounded("nothing", None, "") is False
    assert citation_is_grounded("", _CONTEXT) is False
    assert citation_is_grounded(None, _CONTEXT) is False


# ---------------------------------------------------------------------------
# (c) UNSOUND + CITATION NONE -> void -> re-ask -> still NONE -> 5B keep
# ---------------------------------------------------------------------------
def test_declared_none_twice_is_inadmissible_keep():
    first = _why("a correct implementation could return the other form", "NONE")
    second = _why("a correct printer might legitimately differ here", "NONE")
    v = _StubVerifier([(False, first), (False, second)])
    ok, why = _guard(v)
    assert ok is True
    assert v.verify_calls == 2
    assert why.startswith("[5B-INADMISSIBLE keep]")
    assert "twice uncited under drift-kill signature" in why


def test_declared_none_decision_event():
    void, event = citation_void_decision(
        _DRIFT, _why("could differ", "NONE"), (_CONTEXT,))
    assert (void, event) == (True, 'citation-declared-none')


def test_declared_none_off_the_signature_is_never_void():
    why = _why("a correct implementation could return the other form", "NONE")
    v = _StubVerifier([(False, why)])
    ok, out = _guard(v, profile=_PARTIAL)
    assert ok is False and v.verify_calls == 1 and out == why
    assert citation_void_decision(_PARTIAL, why, (_CONTEXT,)) \
        == (False, 'not-signature')


# ---------------------------------------------------------------------------
# (d) UNSOUND + GROUNDED citation -> the dismissal STANDS
# ---------------------------------------------------------------------------
@pytest.mark.parametrize("quote", [
    # the object-identity check-bug guard shape
    'if (la != lb) throw',
    # the documented-range guard shape
    'may return any value in the range',
])
def test_grounded_citation_lets_the_dismissal_stand(quote):
    why = _why("a correct implementation could legitimately trip this",
               '"' + quote + '"')
    v = _StubVerifier([(False, why)])
    ok, out = _guard(v)
    assert ok is False
    assert v.verify_calls == 1          # no re-ask: nothing was void
    assert out == why
    assert citation_void_decision(_DRIFT, why, (_CONTEXT,)) \
        == (False, 'citation-grounded')


def test_grounded_citation_on_the_reask_kills_the_inadmissible_keep():
    first = _why("a correct implementation could differ", "NONE")
    second = _why("the shown body compares by identity",
                  '"if (la != lb) throw"')
    v = _StubVerifier([(False, first), (False, second)])
    ok, why = _guard(v)
    assert ok is False
    assert "citation-void re-ask" in why
    assert "INADMISSIBLE" not in why


def test_the_reask_statement_cannot_ground_a_citation():
    # The citation-void re-ask statement is appended to concrete_evidence; the
    # grounding check must use the ORIGINAL evidence, so quoting our own
    # injected text is not a citation.
    injected = "an uncited 'a correct implementation could...' hypothetical"
    first = _why("could differ", "NONE")
    second = _why("could differ", '"' + injected + '"')
    v = _StubVerifier([(False, first), (False, second)])
    ok, why = _guard(v, concrete_evidence="[buggy-replay fact] nothing here")
    assert injected in v.kwargs_seen[1]['concrete_evidence']   # it WAS injected
    assert ok is True                                          # yet still void
    assert why.startswith("[5B-INADMISSIBLE keep]")


# ---------------------------------------------------------------------------
# (e) UNSOUND + a quote that is NOT in the shown material -> void
# ---------------------------------------------------------------------------
def test_hallucinated_quote_is_void():
    why = _why("a correct implementation could return null",
               '"@return never null, per the contract"')
    void, event = citation_void_decision(_DRIFT, why, (_CONTEXT,))
    assert (void, event) == (True, 'citation-ungrounded')
    v = _StubVerifier([(False, why), (False, why)])
    ok, out = _guard(v)
    assert v.verify_calls == 2
    assert ok is True and out.startswith("[5B-INADMISSIBLE keep]")


def test_uncheckable_with_no_context_falls_back_to_keywords():
    # Nothing to check against -> we may not claim ungroundedness. The keyword
    # path decides, exactly as before.
    hedged = _why("a correct implementation could differ", '"whatever"')
    void, event = citation_void_decision(_DRIFT, hedged, (None, "", []))
    assert void is True and event.startswith('citation-uncheckable-no-context')
    cited = _why("the javadoc documents this tolerance", '"whatever"')
    void, event = citation_void_decision(_DRIFT, cited, (None, "", []))
    assert void is False and event.endswith('-ok')


# ---------------------------------------------------------------------------
# (f) a MISSING citation line never voids on format alone
# ---------------------------------------------------------------------------
def test_missing_citation_line_falls_back_to_the_keyword_path():
    # keyword path says VOID (an uncited hedge) — as it did before iteration 3
    hedge = "a correct printer could emit the optional separator"
    void, event = citation_void_decision(_DRIFT, hedge, (_CONTEXT,))
    assert void is True
    assert event == 'citation-format-noncompliant-keyword-void'
    # ...and keyword path says NOT void (a cited dismissal) -> stands
    cited = ("the javadoc documents a 1e-6 tolerance, so the check demands "
             "more than the contract")
    void, event = citation_void_decision(_DRIFT, cited, (_CONTEXT,))
    assert void is False
    assert event == 'citation-format-noncompliant-keyword-ok'


def test_format_noncompliance_alone_never_voids():
    # A dismissal with NO hedge and NO citation line: the keyword path leaves
    # it alone, so the missing format cannot by itself void it.
    plain = "the check recomputes the aggregate wrongly (off-by-one index)"
    assert citation_void_decision(_DRIFT, plain, (_CONTEXT,))[0] is False
    v = _StubVerifier([(False, plain)])
    ok, out = _guard(v)
    assert ok is False and v.verify_calls == 1 and out == plain


def test_llm_error_on_the_reask_returns_the_original_verdict():
    first = _why("a correct implementation could differ", "NONE")
    v = _StubVerifier([(False, first), (True, "verifier error (boom); "
                                              "keeping finding")])
    ok, out = _guard(v)
    assert ok is False and out == first      # ORIGINAL verdict, no flip


def test_sound_verdicts_are_never_touched():
    v = _StubVerifier([(True, _why("holds for every implementation", "NONE"))])
    ok, _ = _guard(v)
    assert ok is True and v.verify_calls == 1


# --- the answer format actually carries the citation out of _parse ---------
def test_parse_carries_the_citation_line_through():
    ok, why = RelationVerifier._parse(
        'VERDICT: UNSOUND\nWHY: a correct impl could differ\n'
        'CITATION: "if (la != lb) throw"')
    assert ok is False
    assert parse_citation_line(why) == 'if (la != lb) throw'
    assert strip_citation_line(why) == 'a correct impl could differ'
    ok2, why2 = RelationVerifier._parse('VERDICT: SOUND\nWHY: it holds')
    assert (ok2, why2) == (True, 'it holds')


def test_prompt_demands_the_third_line():
    from java.relations import relation_verifier as rv
    assert "Answer on three lines EXACTLY:" in rv._GUIDANCE
    assert 'CITATION: "<verbatim quote from the shown material>" | NONE' \
        in rv._GUIDANCE
    assert "CITATION: NONE" in rv._GUIDANCE


# ---------------------------------------------------------------------------
# (g) PART B — tag-first terminal profile, whatever the surrounding prose
# ---------------------------------------------------------------------------
# Prose deliberately chosen to point the OPPOSITE way from each tag, so only a
# tag-keyed reader gets these right.
_PROSE_SAYS_DIFFERENT = (
    "the SAME check fires on BOTH builds but with DIFFERENT observed values; "
    "this firing remains evidence against the patch")
_PROSE_SAYS_IDENTICAL = (
    "behaviour at this input is identical on both builds; the patch did not "
    "cause this")


def test_identical_tag_is_terminal_whatever_the_prose():
    assert terminal_profile("[fact:identical-on-both] " + _PROSE_SAYS_DIFFERENT) \
        == 'identical-on-both'
    assert terminal_profile("[fact:fires-on-buggy-scan] " + _PROSE_SAYS_DIFFERENT) \
        == 'identical-on-both'


def test_non_terminal_tags_are_not_terminal_whatever_the_prose():
    assert terminal_profile(
        "[fact:fires-both-different-values] " + _PROSE_SAYS_IDENTICAL) is None
    assert terminal_profile(
        "[fact:not-compared] " + _PROSE_SAYS_IDENTICAL) is None


def test_deny_first_when_several_tags_share_one_blob():
    blob = ("[fact:fires-on-buggy-scan] scan hit\n"
            "[fact:fires-both-different-values] partial fix")
    assert terminal_profile(blob) is None


def test_fact_tags_extraction():
    assert fact_tags("[fact:not-compared] x [FACT:Identical-On-Both]") \
        == {'not-compared', 'identical-on-both'}
    assert fact_tags("[buggy-replay fact] no tags here") == set()


# ---------------------------------------------------------------------------
# (h) no tag -> the marker+veto fallback, unchanged
# ---------------------------------------------------------------------------
def test_untagged_text_falls_back_to_markers_and_veto():
    assert terminal_profile(_PROSE_SAYS_IDENTICAL) == 'identical-on-both'
    assert terminal_profile(_PROSE_SAYS_DIFFERENT) is None
    assert terminal_profile("[buggy-scan fact] recorded on the buggy build") \
        == 'identical-on-both'
    assert terminal_profile("nothing terminal in here") is None


def test_unrecognised_tags_do_not_hijack_the_fallback():
    assert terminal_profile("[fact:some-future-thing] " + _PROSE_SAYS_IDENTICAL) \
        == 'identical-on-both'


# --- the builders actually stamp the tags ---------------------------------
def _sem(value_verdict):
    return semantic_buggy_replay_note(
        fired_ids={'o1'}, breplay_status="crashed", breplay_ids={'o1'},
        bt_all=set(), bt_defect=set(), esc_type=None,
        value_verdict=value_verdict, buggy_msg_excerpt="a",
        patched_msg_excerpt="b")


def _muted(value_verdict):
    return muted_replay_note(
        target_ids={'o1'}, muted_ids={'o2'}, status="crashed",
        fired_ids={'o1'}, esc_type=None, bt_all=set(),
        value_verdict=value_verdict, buggy_msg_excerpt="a",
        patched_msg_excerpt="b")


@pytest.mark.parametrize("build", [_sem, _muted])
@pytest.mark.parametrize("verdict,tag,expected", [
    ("identical", 'identical-on-both', 'identical-on-both'),
    ("different", 'fires-both-different-values', None),
    ("unknown", 'not-compared', None),
])
def test_note_builders_stamp_the_fact_tag(build, verdict, tag, expected):
    note = build(verdict)
    assert tag in fact_tags(note)
    assert terminal_profile(note) == expected
