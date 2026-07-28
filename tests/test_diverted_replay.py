"""Cycle-6: the DIVERTED-replay fix — never claim "the buggy build ran this
input clean" when the harness swallowed an exception and returned early.

The bug this pins (night20b, leg 17_patch1-Chart-26-Jaid_c, a CORRECT patch
wrongly convicted): the buggy-side replay threw inside `axis.draw(...)`, the
harness's own `catch (Exception e) { return; }` swallowed it and returned, so
the check below was NEVER EVALUATED. The replay observed "no firing" and the
note builder worded it as

    "the buggy build handles this exact input cleanly WITHOUT firing this
     check — the patch INTRODUCED the violation here, and the buggy build is
     an existence proof ..."

which is false, and false in the dangerous direction (evidence manufactured
AGAINST a correct patch).

Two halves are tested, both offline and JVM-free:

  * `oracle_mute.instrument_diversion` — the source transform that makes the
    swallow observable (a `__vpSkipped` counter printed on the shared
    `[relscreen]` stats line);
  * `evidence_facts.semantic_buggy_replay_note` / `.muted_replay_note` — the
    three-way quiet-run branch keyed on `diverted` (True / False / None).

Fixture provenance is recorded in the header of
tests/fixtures/chart26_swallow_harness.java.
"""
import os

import pytest

from java.execution import oracle_mute
from java.relations import evidence_facts

_FIX = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures")


def _chart26():
    with open(os.path.join(_FIX, "chart26_swallow_harness.java"),
              "r", encoding="utf-8") as fh:
        return fh.read()


_CHART26 = _chart26()


# ---------------------------------------------------------------------------
# The fixture really is the shape we claim it is.
# ---------------------------------------------------------------------------

def test_fixture_carries_the_three_catch_shapes():
    """One alarm-throw catch and two swallow-return catches — the exact mix
    the transform has to tell apart."""
    blocks = oracle_mute.find_catch_blocks(_CHART26)
    assert len(blocks) == 3
    bodies = [_CHART26[o + 1:c] for _kw, o, c in blocks]
    swallows = [b for b in bodies if oracle_mute.is_swallow_catch(b)]
    assert len(swallows) == 2
    # The one that is NOT a swallow is the harness's own alarm throw.
    others = [b for b in bodies if not oracle_mute.is_swallow_catch(b)]
    assert len(others) == 1
    assert "FuzzerSecurityIssueLow" in others[0]
    assert "linechart3d-null-info-groundtruth" in others[0]
    # And the swallow-return catch guarding axis.draw is present verbatim.
    assert "catch (Exception e) {\n            return;\n        }" in _CHART26


# ---------------------------------------------------------------------------
# (a) the transform instruments swallow-returns and skips rethrow/alarm catches
# ---------------------------------------------------------------------------

def test_transform_instruments_only_the_swallow_returns():
    out = oracle_mute.instrument_diversion(_CHART26)
    assert out is not None
    # Exactly the two swallow-return catches got a counter bump...
    assert out.count("__vpSkipped++;") == 2
    # ...and the static field + shutdown hook landed once, so a run with NO
    # diversion still reports skipped=0 (the "could not tell" vs "did not
    # divert" distinction the whole fix rests on).
    assert out.count("static long __vpSkipped") == 1
    assert out.count("addShutdownHook") == 1
    # Shared [relscreen] stats-line protocol.
    assert '"[relscreen] skipped=" + __vpSkipped' in out
    # The harness's own alarm catch is untouched: its throw still immediately
    # follows the catch brace, with no counter spliced in front of it.
    assert ("} catch (Exception e) {\n"
            "            throw new "
            "com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(") in out
    # Nothing else in the harness was rewritten: every original alarm throw
    # survives.
    for oid in ("axis-draw-preserves-equality-at-null-owner",
                "axis-draw-preserves-axis-metadata-at-null-owner",
                "linechart3d-info-image-equivalence",
                "linechart3d-null-info-groundtruth"):
        assert out.count("[oracle:%s]" % oid) == _CHART26.count(
            "[oracle:%s]" % oid)


def test_transform_puts_the_counter_before_the_return():
    """The increment must run on the way out, not after it."""
    out = oracle_mute.instrument_diversion(_CHART26)
    idx = out.find("__vpSkipped++;")
    assert idx != -1
    while idx != -1:
        tail = out[idx:idx + 400]
        assert "return;" in tail          # the swallow's return follows it
        assert tail.index("return;") > 0  # ...and it is not before it
        idx = out.find("__vpSkipped++;", idx + 1)


# ---------------------------------------------------------------------------
# (e) a catch that RETHROWS is never instrumented
# ---------------------------------------------------------------------------

_RETHROW_HARNESS = """
public class FuzzHarness {
    public static void fuzzerTestOneInput(
            com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            work(data.consumeInt());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("wrapped", e);
        }
    }
}
"""

_ALARM_HARNESS = """
public class FuzzHarness {
    public static void fuzzerTestOneInput(
            com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            work(data.consumeInt());
        } catch (Exception e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:o1] boom");
        }
    }
}
"""


@pytest.mark.parametrize("src", [_RETHROW_HARNESS, _ALARM_HARNESS])
def test_rethrow_and_alarm_catches_are_not_instrumented(src):
    out = oracle_mute.instrument_diversion(src)
    assert out is not None
    assert "__vpSkipped++;" not in out
    # The field/hook still land, so the harness truthfully reports skipped=0
    # rather than degrading to "unknown".
    assert "static long __vpSkipped" in out


def test_a_catch_that_returns_a_value_after_rethrow_is_still_skipped():
    """Belt and braces: any `throw` anywhere in the body disqualifies it."""
    src = _RETHROW_HARNESS.replace("throw e;", "if (flag) { throw e; } return;")
    out = oracle_mute.instrument_diversion(src)
    assert out is not None
    assert out.count("__vpSkipped++;") == 0


def test_return_inside_a_string_or_comment_is_not_a_swallow():
    src = """
public class FuzzHarness {
    public static void fuzzerTestOneInput(
            com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            work(data.consumeInt());
        } catch (Exception e) {
            // return; -- commented out on purpose
            log("return;");
        }
    }
}
"""
    out = oracle_mute.instrument_diversion(src)
    assert out is not None
    assert "__vpSkipped++;" not in out


def test_catch_word_inside_a_literal_is_not_a_catch_block():
    src = """
public class FuzzHarness {
    public static void fuzzerTestOneInput(
            com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String s = "catch (Exception e) { return; }";
        work(s);
    }
}
"""
    assert oracle_mute.find_catch_blocks(src) == []


def test_transform_declines_when_there_is_no_entrypoint():
    """Fail-open: no anchor to hang the counter on -> None, so the caller
    reports diversion UNKNOWN rather than 'clean'."""
    assert oracle_mute.instrument_diversion("class X { void f() {} }") is None
    assert oracle_mute.instrument_diversion("") is None


# ---------------------------------------------------------------------------
# Brace-balance guard on the real fixture
# ---------------------------------------------------------------------------

def _brace_balance(src):
    """Net brace count, ignoring braces inside strings, chars and comments."""
    depth = 0
    i = 0
    n = len(src)
    while i < n:
        c = src[i]
        if c == '/' and i + 1 < n and src[i + 1] == '/':
            j = src.find('\n', i)
            i = n if j < 0 else j
            continue
        if c == '/' and i + 1 < n and src[i + 1] == '*':
            j = src.find('*/', i + 2)
            i = n if j < 0 else j + 2
            continue
        if c == '"':
            i = oracle_mute._string_end(src, i)
            continue
        if c == "'":
            i = oracle_mute._char_end(src, i)
            continue
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
        i += 1
    return depth


def test_transform_preserves_brace_balance_on_the_real_fixture():
    assert _brace_balance(_CHART26) == 0
    out = oracle_mute.instrument_diversion(_CHART26)
    assert _brace_balance(out) == 0
    # Same number of lines-with-code structure: the transform only inserts.
    assert len(out) > len(_CHART26)
    assert _CHART26.count("class ") == out.count("class ")


def test_transform_composes_with_mute_oracles_without_unbalancing():
    muted = oracle_mute.mute_oracles(_CHART26, mute_all=True)
    assert _brace_balance(muted) == 0
    both = oracle_mute.instrument_diversion(muted)
    assert both is not None
    assert _brace_balance(both) == 0
    assert both.count("__vpSkipped++;") == 2


# ---------------------------------------------------------------------------
# parse_skipped — the shared stats-line protocol
# ---------------------------------------------------------------------------

def test_parse_skipped_reads_the_relscreen_line():
    assert oracle_mute.parse_skipped("noise\n[relscreen] skipped=0\n") == 0
    assert oracle_mute.parse_skipped("[relscreen] skipped=1\n"
                                     "[relscreen] skipped=3\n") == 3


def test_parse_skipped_is_none_when_the_line_never_appeared():
    """None = UNKNOWN. A run that halted before the hook must not read as
    'did not divert'."""
    assert oracle_mute.parse_skipped("") is None
    assert oracle_mute.parse_skipped(None) is None
    assert oracle_mute.parse_skipped("[relscreen] checked=10 violated=2") is None


def test_skipped_line_cannot_be_mistaken_for_the_counting_line():
    from java.relations.relation_screen import _STATS_RE
    assert _STATS_RE.search("[relscreen] skipped=4") is None


# ---------------------------------------------------------------------------
# The three note branches — semantic_buggy_replay_note
# ---------------------------------------------------------------------------

def _sem(diverted):
    """The Chart-26 shape: our check fired on patched, the buggy replay was
    quiet (no oracle ids, no exception types, status clean)."""
    return evidence_facts.semantic_buggy_replay_note(
        fired_ids={"axis-draw-preserves-equality-at-null-owner"},
        breplay_status="clean",
        breplay_ids=set(),
        bt_all=set(),
        bt_defect=set(),
        esc_type=None,
        idline="",
        diverted=diverted,
    )


# (b) diverted=True -> no INTRODUCED, no existence proof, says never reached.
def test_diverted_true_makes_no_attribution_claim():
    note = _sem(True)
    assert note is not None
    assert "INTRODUCED" not in note
    assert "existence proof" not in note.lower()
    assert "introduc" not in note.lower()
    low = note.lower()
    assert "never reached" in low
    assert "diverted" in low
    assert "swallow" in low
    # Explicitly symmetric: no attribution in either direction.
    assert "either direction" in low


# (c) diverted=False -> the legitimate case, wording unchanged verbatim.
_INTRODUCED_VERBATIM = (
    "[buggy-replay fact] the buggy build handles this exact "
    "input cleanly WITHOUT firing this check — the patch "
    "INTRODUCED the violation here, and the buggy build is an "
    "existence proof that real code satisfies the asserted "
    "property on this input. 'A correct implementation might "
    "legitimately violate it' is not available as grounds; to "
    "answer UNSOUND you must point at a documented contract the "
    "assertion contradicts.")


def test_diverted_false_keeps_the_introduced_wording_verbatim():
    assert _sem(False) == _INTRODUCED_VERBATIM


# (d) diverted=None -> the conservative unavailable wording.
def test_diverted_unknown_is_conservative_not_introduced():
    note = _sem(None)
    assert note is not None
    assert "INTRODUCED" not in note
    assert "existence proof" not in note.lower()
    low = note.lower()
    assert "unavailable" in low
    assert "judge on soundness alone, sceptically" in low
    assert "do not read the quiet run as evidence" in low


def test_default_call_cannot_manufacture_the_introduced_claim():
    """An unthreaded caller (no `diverted` kwarg at all) must land on the
    conservative branch — the fail-safe default."""
    note = evidence_facts.semantic_buggy_replay_note(
        {"o1"}, "clean", set(), set(), set(), None)
    assert "INTRODUCED" not in note
    assert "unavailable" in note.lower()


# The escaped-exception branch carries the same claim and the same gate.
def _esc(diverted):
    return evidence_facts.semantic_buggy_replay_note(
        fired_ids=set(),
        breplay_status="clean",
        breplay_ids=set(),
        bt_all=set(),
        bt_defect=set(),
        esc_type="NullPointerException",
        diverted=diverted,
    )


def test_escaped_branch_is_gated_the_same_way():
    assert "INTRODUCED" in _esc(False)
    assert "INTRODUCED" not in _esc(True)
    assert "never reached" in _esc(True).lower()
    assert "INTRODUCED" not in _esc(None)
    assert "unavailable" in _esc(None).lower()


# ---------------------------------------------------------------------------
# The three note branches — muted_replay_note (the site that actually fired
# on Chart-26: the shadowing check was muted, then the run was diverted).
# ---------------------------------------------------------------------------

def _muted(diverted):
    return evidence_facts.muted_replay_note(
        target_ids={"axis-draw-preserves-equality-at-null-owner"},
        muted_ids={"linechart3d-null-info-groundtruth"},
        status="clean",
        fired_ids=set(),
        esc_type=None,
        bt_all=set(),
        diverted=diverted,
    )


def test_muted_diverted_true_makes_no_attribution_claim():
    note = _muted(True)
    assert note is not None
    assert "introduc" not in note.lower()
    low = note.lower()
    assert "never reached" in low
    assert "either direction" in low
    # Still names the silenced check so the mechanical context survives.
    assert "linechart3d-null-info-groundtruth" in note


def test_muted_diverted_false_keeps_the_introduced_wording():
    note = _muted(False)
    low = note.lower()
    assert "without firing" in low
    assert "introduc" in low
    assert "identical on both builds" not in low


def test_muted_diverted_unknown_is_conservative():
    note = _muted(None)
    low = note.lower()
    assert "introduc" not in low
    assert "unavailable" in low
    assert "judge on soundness alone, sceptically" in low


def test_muted_default_call_cannot_manufacture_the_claim():
    note = evidence_facts.muted_replay_note(
        {"o1"}, {"o2"}, "clean", set(), None, set())
    assert "introduc" not in note.lower()


# ---------------------------------------------------------------------------
# The gate is confined to the quiet-run branch: every other branch is
# untouched by `diverted` (no collateral rewording).
# ---------------------------------------------------------------------------

@pytest.mark.parametrize("diverted", [True, False, None])
def test_same_check_branch_is_unaffected_by_diverted(diverted):
    note = evidence_facts.semantic_buggy_replay_note(
        fired_ids={"o1"}, breplay_status="crashed", breplay_ids={"o1"},
        bt_all=set(), bt_defect=set(), esc_type=None,
        value_verdict="identical", diverted=diverted)
    assert "identical on both builds" in note


@pytest.mark.parametrize("diverted", [True, False, None])
def test_shadowed_branch_is_unaffected_by_diverted(diverted):
    note = evidence_facts.semantic_buggy_replay_note(
        fired_ids={"o1"}, breplay_status="crashed", breplay_ids={"o2"},
        bt_all=set(), bt_defect=set(), esc_type=None, diverted=diverted)
    assert "UNKNOWN" in note
    assert "o2" in note
