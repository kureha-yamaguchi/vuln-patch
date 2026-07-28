"""Cycle-6 item 4: plumbing for the two NEVER-COLLECTED measurements.

Fully offline — a stubbed replay function, no JVM, no LLM, no tokens.

PART A — the muted re-replay ITERATES. Silencing the shadowing check and
replaying answers "does THIS check fire on the buggy build at this exact
input?" only when the muted run gets far enough for the target to speak. When
it instead crashes at yet ANOTHER sibling alarm, that sibling is a new shadow:
add it to the mute set and replay again (night20b: Closure-62
``end-of-line-caret``, Math-65 ``chiSquare-inversely-…`` shadowed by
``circle-dense-errors-0`` — both measurements were simply never collected).
Bounded to 3 passes beyond the first, stopping early when the mute set stops
growing or a pass errors; exhausting the passes keeps the honest UNKNOWN
wording and never manufactures a fact.

PART B — a KNOWN buggy-side fire rate must reach the harness track's evidence
regardless of whether the patched-side counts exist. No new threshold:
``fire_rate_fact``'s own branches decide, and a genuinely unremarkable rate
still yields no note.
"""
import inspect
import os
import re

from java.execution.fuzz_runner import (
    MAX_EXTRA_MUTED_PASSES,
    MUTED_PASS_LOG_PREFIX,
    iterate_muted_replay,
)
from java.relations.evidence_facts import fire_rate_fact, muted_replay_note

_TARGET = "end-of-line-caret"


class _ScriptedReplay:
    """Return a scripted `(status, fired_ids, output, diverted)` per pass and
    record the mute set each pass was given. Stands in for
    `FuzzRunner.replay_input_muted` — no JVM."""

    def __init__(self, script):
        self._script = list(script)
        self.mute_sets = []
        self.pass_indices = []

    def __call__(self, mute_ids, pass_index):
        self.mute_sets.append(set(mute_ids))
        self.pass_indices.append(pass_index)
        idx = min(len(self.mute_sets) - 1, len(self._script) - 1)
        step = self._script[idx]
        if isinstance(step, Exception):
            raise step
        return step


def _crashed_at(*ids):
    return ("crashed", set(ids), "output", None)


# --------------------------------------------------------------------------
# PART A — the iteration rule and its bound.
# --------------------------------------------------------------------------

def test_each_new_shadow_is_added_and_the_bound_stops_it():
    # Every pass crashes at a FRESH sibling, so the mute set grows forever;
    # only the bound may stop it.
    replay = _ScriptedReplay([
        _crashed_at("shadow-b"),
        _crashed_at("shadow-c"),
        _crashed_at("shadow-d"),
        _crashed_at("shadow-e"),
        _crashed_at("shadow-f"),      # must never be reached
    ])
    logs = []
    status, fired, out, diverted, muted, passes = iterate_muted_replay(
        replay, {_TARGET}, {"shadow-a"}, log=logs.append)

    # One first pass + at most MAX_EXTRA_MUTED_PASSES more.
    assert MAX_EXTRA_MUTED_PASSES == 3
    assert passes == 1 + MAX_EXTRA_MUTED_PASSES == 4
    assert len(replay.mute_sets) == 4
    # Each pass was handed the previous mute set plus the sibling that had
    # just shadowed the target.
    assert replay.mute_sets[0] == {"shadow-a"}
    assert replay.mute_sets[1] == {"shadow-a", "shadow-b"}
    assert replay.mute_sets[2] == {"shadow-a", "shadow-b", "shadow-c"}
    assert replay.mute_sets[3] == {"shadow-a", "shadow-b", "shadow-c",
                                   "shadow-d"}
    assert muted == replay.mute_sets[-1]
    # Fail-open: the answer is still unknown, so no fact is manufactured.
    assert muted_replay_note({_TARGET}, muted, status, fired,
                             None, set()) is None


def test_every_pass_is_logged_with_the_mute_set_size():
    replay = _ScriptedReplay([_crashed_at("shadow-b"),
                              _crashed_at("shadow-c"),
                              _crashed_at("shadow-d"),
                              _crashed_at("shadow-e")])
    logs = []
    iterate_muted_replay(replay, {_TARGET}, {"shadow-a"}, log=logs.append)

    assert len(logs) == 4                      # one audit line per pass
    sizes = []
    for i, line in enumerate(logs, start=1):
        assert MUTED_PASS_LOG_PREFIX in line   # greppable
        assert "pass=%d/" % i in line
        m = re.search(r"mute_set_size=(\d+)", line)
        assert m, line
        sizes.append(int(m.group(1)))
    assert sizes == [1, 2, 3, 4]               # the growth is auditable
    assert "bound reached" in logs[-1]


def test_stops_early_when_the_mute_set_stops_growing():
    # Pass 2 crashes at a sibling that is ALREADY muted: another pass would
    # replay the very same build, so iteration ends.
    replay = _ScriptedReplay([
        _crashed_at("shadow-b"),
        _crashed_at("shadow-b"),
        _crashed_at("shadow-c"),      # must never be reached
    ])
    logs = []
    status, fired, out, diverted, muted, passes = iterate_muted_replay(
        replay, {_TARGET}, {"shadow-a"}, log=logs.append)

    assert passes == 2 < 1 + MAX_EXTRA_MUTED_PASSES
    assert muted == {"shadow-a", "shadow-b"}
    assert "stopped growing" in logs[-1]
    assert muted_replay_note({_TARGET}, muted, status, fired,
                             None, set()) is None


def test_an_erroring_pass_ends_iteration_and_yields_unknown():
    replay = _ScriptedReplay([
        _crashed_at("shadow-b"),
        ("error", None, "", None),
        _crashed_at("shadow-c"),      # must never be reached
    ])
    status, fired, out, diverted, muted, passes = iterate_muted_replay(
        replay, {_TARGET}, {"shadow-a"}, log=lambda _l: None)

    assert passes == 2
    assert status == "error"
    note = muted_replay_note({_TARGET}, muted, status, fired, None, set())
    assert note == ("[muted-replay fact] a muted re-replay was attempted and "
                    "unavailable.")


def test_a_raising_pass_ends_iteration_and_yields_unknown():
    replay = _ScriptedReplay([
        _crashed_at("shadow-b"),
        RuntimeError("jazzer blew up"),
        _crashed_at("shadow-c"),      # must never be reached
    ])
    status, fired, out, diverted, muted, passes = iterate_muted_replay(
        replay, {_TARGET}, {"shadow-a"}, log=lambda _l: None)

    assert passes == 2
    assert status == "error"          # never "clean", never a fabricated fact
    assert muted_replay_note({_TARGET}, muted, status, fired, None,
                             set()).endswith("unavailable.")


def test_mute_failed_ends_iteration_immediately():
    replay = _ScriptedReplay([("mute_failed", None, "", None),
                              _crashed_at("shadow-b")])
    status, _f, _o, _d, muted, passes = iterate_muted_replay(
        replay, {_TARGET}, {"shadow-a"}, log=lambda _l: None)
    assert (status, passes) == ("mute_failed", 1)
    assert muted == {"shadow-a"}


def test_crashing_kind_legs_keep_the_single_pass():
    # max_extra_passes=0 is how run.py skips the iteration for crashing bugs:
    # exactly the one Jazzer run it always spent.
    replay = _ScriptedReplay([_crashed_at("shadow-b"),
                              _crashed_at("shadow-c")])
    _s, _f, _o, _d, muted, passes = iterate_muted_replay(
        replay, {_TARGET}, {"shadow-a"}, max_extra_passes=0,
        log=lambda _l: None)
    assert passes == 1
    assert muted == {"shadow-a"}


# --------------------------------------------------------------------------
# PART A — the ANSWER is reachable, and its semantics are unchanged.
# --------------------------------------------------------------------------

def test_target_firing_on_pass_two_yields_the_pass_one_fact():
    # The Closure-62 shape: pass 1 is shadowed by a sibling, pass 2 (that
    # sibling muted too) lets the target speak.
    iterated = _ScriptedReplay([_crashed_at("shadow-b"),
                                _crashed_at(_TARGET)])
    status, fired, out, diverted, muted, passes = iterate_muted_replay(
        iterated, {_TARGET}, {"shadow-a"}, log=lambda _l: None)
    assert (passes, status) == (2, "crashed")
    assert fired == {_TARGET}

    # The counterfactual: the very same outcome on the FIRST pass, with the
    # same mute set. The fact must be identical, tags and all — iteration only
    # makes the answer reachable, it never changes the note.
    one_pass = _ScriptedReplay([_crashed_at(_TARGET)])
    _s2, f2, _o2, _d2, muted2, passes2 = iterate_muted_replay(
        one_pass, {_TARGET}, muted, log=lambda _l: None)
    assert (passes2, muted2) == (1, muted)

    for verdict in ("identical", "different", "unknown"):
        note_iter = muted_replay_note(
            {_TARGET}, muted, status, fired, None, set(),
            value_verdict=verdict, buggy_msg_excerpt="b",
            patched_msg_excerpt="p")
        note_one = muted_replay_note(
            {_TARGET}, muted2, "crashed", f2, None, set(),
            value_verdict=verdict, buggy_msg_excerpt="b",
            patched_msg_excerpt="p")
        assert note_iter == note_one
        assert "[fact:fires-on-both-confirmed]" in note_iter


def test_a_clean_pass_answers_and_stops():
    replay = _ScriptedReplay([_crashed_at("shadow-b"),
                              ("clean", set(), "out", False),
                              _crashed_at("shadow-c")])
    status, fired, _o, diverted, muted, passes = iterate_muted_replay(
        replay, {_TARGET}, {"shadow-a"}, log=lambda _l: None)
    assert (passes, status, diverted) == (2, "clean", False)
    note = muted_replay_note({_TARGET}, muted, status, fired, None, set(),
                             diverted=diverted)
    assert "the patch introduced the violation here" in note


def test_escaped_exception_target_is_recognised_by_type():
    # No oracle id (an escaped throwable): the target "spoke" when its
    # exception type shows up in the muted run's output — same rule the note
    # itself applies.
    out = ("== Java Exception: java.lang.NullPointerException\n"
           "\tat com.example.Thing.run(Thing.java:1)\n")
    replay = _ScriptedReplay([_crashed_at("shadow-b"),
                              ("crashed", set(), out, None)])
    status, fired, _o, _d, muted, passes = iterate_muted_replay(
        replay, set(), {"shadow-a"}, esc_type="NullPointerException",
        log=lambda _l: None)
    assert passes == 2                        # stopped: the target spoke
    assert muted == {"shadow-a", "shadow-b"}
    note = muted_replay_note(set(), muted, status, fired,
                             "NullPointerException",
                             {"NullPointerException"})
    assert "[fact:fires-on-both-confirmed]" in note


# --------------------------------------------------------------------------
# PART B — a known buggy rate reaches the evidence; an unremarkable one does
# not become a note.
# --------------------------------------------------------------------------

def test_known_buggy_rate_reaches_evidence_without_patched_counts():
    # The patched-side replay runs LATER in the pipeline, so the harness track
    # routinely has buggy counts and no patched counts. A known, remarkable
    # rate must still be stated.
    note = fire_rate_fact(20000, 19980, None, None, '')
    assert note is not None
    assert "buggy build 19980/20000" in note
    assert "patched build" not in note        # nothing invented for the gap


def test_unremarkable_buggy_only_rate_still_yields_no_note():
    # Math-65's 3953/20000 = 19.8%: known, delivered to fire_rate_fact, and
    # matching none of its branches — correctly no note, no invented threshold.
    assert fire_rate_fact(20000, 3953, None, None, '') is None
    assert fire_rate_fact(None, None, None, None, '') is None


def test_buggy_rate_delivery_is_wired_into_the_harness_track():
    # Structural: the universal screen must no longer be gated on the one-door
    # MATCH (which suppressed the only measurement on the Math-65 harness
    # track), and a known rate must be delivered on its own path.
    run_py = os.path.join(os.path.dirname(os.path.dirname(
        os.path.abspath(__file__))), "src", "java", "run.py")
    with open(run_py) as fh:
        source = fh.read()
    assert "not _rate_known" in source
    assert "not _one_door_matched" not in source   # the old suppressing gate
    assert "[buggy-rate delivery]" in source
    assert "_buggy_rate_counts" in source


def test_iterate_muted_replay_is_wired_into_the_run():
    run_py = os.path.join(os.path.dirname(os.path.dirname(
        os.path.abspath(__file__))), "src", "java", "run.py")
    with open(run_py) as fh:
        source = fh.read()
    assert "iterate_muted_replay as _imr" in source
    # crashing-kind legs keep their single pass.
    assert "0 if bug_kind == 'crashing' else 3" in source
    # Each pass builds its own muted variant.
    sig = inspect.signature(
        __import__("java.execution.fuzz_runner", fromlist=["x"])
        .FuzzRunner.replay_input_muted)
    assert "variant_tag" in sig.parameters
