"""Reportable patched-only exceptions (Mechanism A) and rejection-probe
ordering (Mechanism B) — the prompt directives and their screen-side teeth.

The failure this closes: a relation builds an input it has DECLARED valid by
construction, calls the patch-changed class with it, and the patched build
throws. The mandated body shape caught that exception and returned, so the
check measured 0/N on both builds and read as a well-behaved tripwire — a
patch that ADDS or MOVES a throw was invisible to every relation shaped that
way. Mechanism A splits the catch in two: setup exceptions stay rejections,
probe exceptions are re-thrown as the violation. Mechanism B closes the
ordering half of the receiver-state gap: a rejection probe must run again
after every state change, not once on the freshly built receiver.

Three parts, all offline:

  * PART A — the synthesis / repair / harness prompt text (asserted present,
    voiced as the mechanism requires, and dataset-neutral).
  * PART B — java.parsing.java_source.patched_probe_swallowed (the lint),
    rethrow_patched_probe (the mechanical normalisation) and
    probe_before_last_mutation (the ordering lint).
  * PART C — the wiring: the screen rewrites/demotes, run.py supplies the
    patch-changed classes, and the counting wrapper is untouched (the
    rewrite speaks the "violated" message it already recognises).
"""
import inspect

from java.harness.prompts import PromptBuilder
from java.parsing.java_source import (boolean_swallow,
                                      patched_probe_swallowed,
                                      probe_before_last_mutation,
                                      rethrow_patched_probe,
                                      violation_swallowed)
from java.relations import relation_screen, relation_synth

_BANNED = ("chart", "axis", "jfreechart", "plot", "closure", "lang-",
           "math-", "commons", "jfree", "indexof", "objectlist")


# --------------------------------------------------------------------------
# PART A — the prompt directives.
# --------------------------------------------------------------------------

def _two_tier():
    text = relation_synth._INSTRUCTIONS
    start = text.index("TWO-TIER CATCH")
    return text[start:text.index("STRUCTURE RULE", start)]


def test_two_tier_catch_directive_present():
    block = _two_tier()
    assert "TIER 1 — SETUP" in block
    assert "TIER 2 — THE PROBE" in block
    assert "PATCH-CHANGED class" in block


def test_tier_one_keeps_catch_and_return():
    assert "catch (Exception e) { return; }" in _two_tier()


def test_tier_two_rethrows_with_class_and_message():
    block = _two_tier()
    assert "violated: unexpected" in block
    assert "e.getClass().getName()" in block
    assert "e.getMessage()" in block
    assert "valid-by-construction input" in block


def test_tier_two_probe_try_is_not_nested_in_the_setup_try():
    """A rethrow inside the setup try lands in the setup catch and dies —
    the existing violation_swallowed lint would drop it."""
    assert "NOT nested inside the" in _two_tier()
    assert "must not be nested inside" in relation_synth._INSTRUCTIONS


def test_expected_rejection_contracts_keep_the_targeted_catch():
    block = _two_tier()
    assert "CONTRACT IS a rejection" in block
    assert "catch (TheDocumentedException ok)" in block


def test_the_old_blanket_mandate_is_gone():
    """The sentence that swallowed the signal: an exception is ALWAYS a
    rejection. It must not survive anywhere in the synthesis prompt."""
    text = (relation_synth._INSTRUCTIONS + relation_synth._FOCUSED_FENCING
            + relation_synth._SYSTEM)
    assert "an exception is a rejection, never a" not in text
    assert "RETURN (skip) on ANY caught exception" not in text


def test_two_tier_reaches_the_focused_passes():
    """_OUTPUT_SPEC is reused verbatim by every focused pass; the shape rule
    lives in the OUTPUT half, so all four passes carry it."""
    assert "TWO-TIER CATCH" in relation_synth._OUTPUT_SPEC


def test_focused_fencing_no_longer_says_skip_every_exception():
    fencing = relation_synth._FOCUSED_FENCING
    assert "Do NOT skip an exception from the PROBE call" in fencing
    assert "a rejection is never a violation" not in fencing


def test_compile_repair_may_not_reintroduce_the_blanket_catch():
    src = inspect.getsource(relation_synth.RelationSynthesizer.repair_check)
    assert "do NOT " in src and "blanket" in src
    assert "catch (Exception e) { return; }" in src
    assert "TWO-TIER CATCH" in src


def test_synthesis_context_names_the_patch_changed_class():
    src = inspect.getsource(relation_synth.RelationSynthesizer.synthesize)
    assert "THE PATCH-CHANGED CLASS: " in src
    assert "{class_name}" in src


def test_rejection_independence_carries_the_ordering_rule():
    text = relation_synth._INSTRUCTIONS
    start = text.index("STANDING STRATEGY — REJECTION INDEPENDENCE")
    block = text[start:text.index("STANDING STRATEGY — STRUCTURE FROM DATA",
                                  start)]
    assert "ORDERING (mandatory" in block
    assert "Mutate, then probe" in block
    assert "AFTER EVERY state-changing call" in block
    assert "ordering blind spot" in block


def test_harness_channel_gets_the_same_ordering_rule():
    block = PromptBuilder()._preconditions_block(
        ["@param x the value\n@throws IllegalArgumentException if absent"])
    assert "RE-PROBE AFTER EVERY STATE CHANGE" in block
    assert "Mutate, then probe" in block
    assert "FuzzedDataProvider" in block
    assert "never from literals" in block


def test_harness_ordering_rule_is_absent_without_a_documented_contract():
    """The block renders only where there IS a documented rejection
    contract; with no javadoc it stays empty, as before."""
    assert PromptBuilder()._preconditions_block([]) == ''
    assert PromptBuilder()._preconditions_block(["no tags here"]) == ''


def test_new_prompt_text_is_dataset_neutral():
    text = (_two_tier() + relation_synth._FOCUSED_FENCING
            + PromptBuilder()._preconditions_block(
                ["@throws IllegalArgumentException if absent"])).lower()
    for banned in _BANNED:
        assert banned not in text, f"prompt text leaked '{banned}'"


# --------------------------------------------------------------------------
# PART B — the lints, on synthetic minimal cases.
# --------------------------------------------------------------------------

_BLANKET_PROBE = """
Container box = new Container(4);
int n = data.consumeInt(0, 20);
int found;
try {
    for (int i = 0; i < n; i++) { box.set(i, new Object()); }
    found = box.lookup(null);
} catch (Exception e) { return; }
if (found != -1) throw new RuntimeException("relation r violated: " + found);
"""

_TWO_TIER_PROBE = """
Container box;
try { box = new Container(data.consumeInt(1, 8)); }
catch (Exception e) { return; }
int found;
try { found = box.lookup(null); }
catch (Exception e) {
    throw new RuntimeException("relation r violated: unexpected "
        + e.getClass().getName() + " on valid-by-construction input: "
        + e.getMessage());
}
if (found != -1) throw new RuntimeException("relation r violated: " + found);
"""

_TARGETED_REJECTION = """
Container box = new Container(4);
boolean violated = false;
try { box.lookup(null); violated = true; }
catch (IllegalArgumentException ok) { }
catch (Throwable t) { violated = true; }
if (violated) throw new RuntimeException("relation r violated: rejected");
"""

_OTHER_CLASS_ONLY = """
java.util.List<String> box = new java.util.ArrayList<String>();
int found;
try { box.add("a"); found = box.indexOf("absent"); }
catch (Exception e) { return; }
if (found != -1) throw new RuntimeException("relation r violated: " + found);
"""

_RETHROWING_BROAD_CATCH = """
Container box = new Container(4);
int found;
try { found = box.lookup(null); }
catch (Exception e) { throw new RuntimeException("relation r violated: " + e); }
if (found != -1) throw new RuntimeException("relation r violated: " + found);
"""


def test_blanket_catch_around_a_patched_probe_is_flagged():
    reason = patched_probe_swallowed(_BLANKET_PROBE, ["Container.java"])
    assert reason
    assert "box.lookup(...)" in reason           # names the read, not the set
    assert "Container" in reason
    assert "VALID BY CONSTRUCTION" in reason


def test_the_new_two_tier_shape_passes_every_structure_lint():
    for src in (_TWO_TIER_PROBE, _RETHROWING_BROAD_CATCH):
        assert patched_probe_swallowed(src, ["Container"]) is None
        assert violation_swallowed(src) is None
        assert boolean_swallow(src) is None


def test_expected_rejection_shape_is_never_flagged():
    """The convicting documented-@throws shape names its exception; that is
    evidence, not a swallow."""
    assert patched_probe_swallowed(_TARGETED_REJECTION, ["Container"]) is None


def test_probe_on_another_class_is_not_flagged():
    assert patched_probe_swallowed(_OTHER_CLASS_ONLY, ["Container"]) is None


def test_lint_is_inert_without_patched_classes():
    assert patched_probe_swallowed(_BLANKET_PROBE, []) is None
    assert patched_probe_swallowed(_BLANKET_PROBE, None) is None
    assert rethrow_patched_probe(_BLANKET_PROBE, []) is None


def test_class_names_accept_paths_packages_and_java_suffixes():
    for form in ("source/org/x/Container.java", "org.x.Container",
                 "Container.java", "Container"):
        assert patched_probe_swallowed(_BLANKET_PROBE, [form]), form


def test_rewrite_inserts_a_frame_guarded_rethrow():
    out = rethrow_patched_probe(_BLANKET_PROBE, ["Container"],
                                relation_name="my-relation")
    assert out
    assert "getStackTrace()" in out
    assert 'relation my-relation violated: unexpected' in out
    assert 'e.getClass().getName()' in out
    # Constructor frames stay rejections: a receiver refusing its arguments
    # is setup, not a probe result.
    assert '"<init>".equals(' in out
    # The original body survives untouched.
    assert 'box.lookup(null)' in out
    assert 'if (found != -1)' in out


def test_rewrite_silences_the_lint_and_trips_no_other_one():
    out = rethrow_patched_probe(_BLANKET_PROBE, ["Container"],
                                relation_name="r")
    assert patched_probe_swallowed(out, ["Container"]) is None
    assert violation_swallowed(out) is None
    assert boolean_swallow(out) is None


def test_rewrite_keeps_the_skip_path_for_every_other_exception():
    """Only frames in the patch-changed class rethrow; everything else falls
    through to the original catch body, which still returns."""
    out = rethrow_patched_probe(_BLANKET_PROBE, ["Container"],
                                relation_name="r")
    assert out.rstrip().count("return;") == _BLANKET_PROBE.count("return;")


def test_rewrite_covers_every_changed_class_named():
    out = rethrow_patched_probe(_BLANKET_PROBE, ["Container", "Helper"],
                                relation_name="r")
    assert '.Container"' in out and '.Helper"' in out


def test_lints_return_none_on_empty_and_garbage():
    for src in ("", None, "   ", "not java at all"):
        assert patched_probe_swallowed(src, ["Container"]) is None
        assert rethrow_patched_probe(src, ["Container"]) is None
        assert probe_before_last_mutation(src) is None


# --- Mechanism B: probe ordering -------------------------------------------

_PROBE_THEN_MUTATE = """
Container box = new Container(4);
int n = data.consumeInt(0, 4);
try { box.lookup(null); }
catch (IllegalArgumentException ok) { }
box.set(data.consumeInt(0, n), new Object());
throw new RuntimeException("relation r violated: rejection was accepted");
"""

_MUTATE_THEN_PROBE = """
Container box = new Container(4);
int n = data.consumeInt(0, 4);
box.set(data.consumeInt(0, n), new Object());
try { box.lookup(null); }
catch (IllegalArgumentException ok) { }
throw new RuntimeException("relation r violated: rejection was accepted");
"""

_NO_REJECTION_PROBE = """
Container box = new Container(4);
int a;
try { a = box.size(); } catch (Exception e) { return; }
box.set(0, new Object());
if (a < 0) throw new RuntimeException("relation r violated: " + a);
"""


def test_probe_before_last_mutation_is_flagged():
    reason = probe_before_last_mutation(_PROBE_THEN_MUTATE)
    assert reason
    assert "box.lookup(...)" in reason
    assert "box.set(...)" in reason
    assert "re-run the probe after each state-changing call" in reason.lower()


def test_probe_after_the_last_mutation_is_not_flagged():
    assert probe_before_last_mutation(_MUTATE_THEN_PROBE) is None


def test_ordering_lint_needs_a_rejection_probe_to_fire():
    """No rejection contract, nothing to re-run — a plain read followed by a
    mutation is not an ordering gap."""
    assert probe_before_last_mutation(_NO_REJECTION_PROBE) is None


# --------------------------------------------------------------------------
# PART C — the wiring.
# --------------------------------------------------------------------------

def test_screen_rewrites_then_demotes_but_never_drops():
    src = inspect.getsource(relation_screen.screen_relations)
    i = src.index("_swallow = patched_probe_swallowed(")
    window = src[i:src.index("# P0.2 self-swallow lint", i)]
    assert "rethrow_patched_probe(" in window
    assert "screen_demotion" in window
    assert "dropped" not in window
    assert "continue" not in window


def test_the_rewrite_runs_before_the_other_structure_lints():
    """Every downstream lint must see the body that will be compiled."""
    src = inspect.getsource(relation_screen.screen_relations)
    assert (src.index("patched_probe_swallowed(")
            < src.index("violation_swallowed(")
            < src.index("boolean_swallow("))


def test_ordering_lint_is_wired_as_a_demotion():
    src = inspect.getsource(relation_screen.screen_relations)
    i = src.index("probeorder = probe_before_last_mutation(")
    window = src[i:src.index("cls = f'RelScreen", i)]
    assert "DEMOTED" in window
    assert "screen_demotion" in window
    assert "dropped" not in window
    assert "continue" not in window


def test_run_supplies_the_patch_changed_classes():
    import java.run as run_mod

    src = inspect.getsource(run_mod)
    assert "_patched_classes = list(_syn_cls)" in src
    # Both screening call sites (first pass and the convergence rounds).
    assert src.count("patched_classes=_patched_classes") == 2


def test_counting_wrapper_is_unchanged_and_hears_the_rewrite():
    """Why the wrapper needed no change: it counts a firing when the escaping
    message contains 'violated', and the inserted rethrow says exactly
    that."""
    wrapper = inspect.getsource(relation_screen._screen_harness_source)
    assert 'm.contains("violated")' in wrapper
    assert "FuzzerSecurityIssue" in wrapper
    out = rethrow_patched_probe(_BLANKET_PROBE, ["Container"],
                                relation_name="r")
    assert "violated" in out.split("getStackTrace()", 1)[1]


# --- subtype delegation (the Chart-19 shape: patch in the base class,
# probes written against its public subclass) ------------------------------

def test_subclasses_in_tree_finds_transitive_subtypes(tmp_path):
    from java.parsing.java_source import subclasses_in_tree

    src = tmp_path / "source" / "org" / "x"
    src.mkdir(parents=True)
    (src / "Base.java").write_text(
        "package org.x;\npublic abstract class Base {}\n")
    (src / "Mid.java").write_text(
        "package org.x;\npublic class Mid extends Base {}\n")
    (src / "Leaf.java").write_text(
        "package org.x;\npublic class Leaf extends org.x.Mid {}\n")
    (src / "Other.java").write_text(
        "package org.x;\npublic class Other {}\n")
    tests = tmp_path / "tests"
    tests.mkdir()
    (tests / "FakeSub.java").write_text(
        "public class FakeSub extends Base {}\n")

    subs = subclasses_in_tree(str(tmp_path), ["Base"])
    assert subs == ["Leaf", "Mid"]          # transitive, sorted
    assert "FakeSub" not in subs            # test trees are skipped


def test_subtype_receiver_probe_is_flagged_and_rewritten():
    """A probe on the SUBCLASS must count once the subtype is in the class
    list — the delegating shape the mechanism was built for."""
    check = (
        "Sub s;\n"
        "int r;\n"
        "try { s = new Sub(); r = s.lookup(null); }\n"
        "catch (Exception e) { return; }\n"
        "if (r != -1) throw new RuntimeException(\"relation x violated: \" + r);\n"
    )
    # Diff headers alone (base class only): inert — the gap this fix closes.
    assert patched_probe_swallowed(check, ["Base"]) is None
    # With the subtype counted, the lint flags and the rewrite lands, and
    # the runtime guard still tests BOTH names (the base class is the frame
    # that actually throws under delegation).
    reason = patched_probe_swallowed(check, ["Base", "Sub"])
    assert reason is not None and "Sub" in reason
    out = rethrow_patched_probe(check, ["Base", "Sub"], relation_name="x")
    assert out is not None
    guard = out.split("getStackTrace()", 1)[1]
    assert '"Base"' in guard and '"Sub"' in guard
