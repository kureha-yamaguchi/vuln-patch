"""Spec M (cycle-3b) / M-v2: tests for universal screening's pure pieces.

M-v2 measures a fired oracle by INSTRUMENTING the whole (known-compilable)
harness — mute every sibling alarm, replace the target oracle's throw with a
`__vpViolated++` tally, inject the relscreen counting scaffold — instead of
extracting a single check body (v1, which near-never compiled on real
multi-oracle harnesses; see 2026-07-25-cycle3b.md "Cycle outcome"). These are
all unit-testable without a JVM:

  * java.execution.oracle_mute.instrument_for_counting(source, target_id)
    -> str|None — the pure source transform, exercised on the TWO real
    archived harness sources (tests/fixtures/harness_sources.json).
  * java.relations.evidence_facts.never_held_fact(checked) -> str — wording.
  * java.relations.relation_screen — the run path needs a JVM, so we only
    assert the primitives the M-v2 path relies on (_run_counting_fuzz, the
    _STATS_RE the injected print must satisfy) still exist, plus the existing
    screen entry points keep their signatures.
"""
import inspect
import json
import os
import re

from java.execution.oracle_mute import instrument_for_counting
from java.relations.evidence_facts import never_held_fact

_FIXTURES = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "fixtures", "harness_sources.json")


def _load():
    with open(_FIXTURES) as fh:
        return json.load(fh)


# --------------------------------------------------------------------------
# instrument_for_counting — fixture 1: math30 multi-oracle (single method).
# --------------------------------------------------------------------------

def test_instrument_math30_swap_symmetry():
    src = _load()["math30_multi_oracle"]["source"]
    out = instrument_for_counting(src, "swap-symmetry")
    assert out is not None
    # The target's throw is replaced by the tally, so its own alarm message
    # (the only place the tag appears) is gone and no longer thrown.
    assert "[oracle:swap-symmetry]" not in out
    assert "{ __vpViolated++; }" in out
    # Every SIBLING alarm is muted, one `; /* muted:<id> */` marker each.
    for sib in ("lifted-big-dataset", "midpoint-pvalue", "u-sum"):
        assert "; /* muted:%s */" % sib in out
        assert "[oracle:%s]" % sib not in out       # sibling throw gone too
    # Counting scaffold injected exactly once.
    assert out.count("static long __vpChecked = 0, __vpViolated = 0;") == 1
    # fuzzerTestOneInput survives; braces still balanced.
    assert "fuzzerTestOneInput" in out
    assert out.count("{") == out.count("}")


def test_instrument_math30_each_target_isolated():
    # Whichever oracle is the target, exactly that one becomes the tally and
    # every other becomes a muted `;`.
    src = _load()["math30_multi_oracle"]["source"]
    ids = ["lifted-big-dataset", "midpoint-pvalue", "swap-symmetry", "u-sum"]
    for target in ids:
        out = instrument_for_counting(src, target)
        assert out is not None, target
        assert "[oracle:%s]" % target not in out
        assert "{ __vpViolated++; }" in out
        for sib in ids:
            if sib != target:
                assert "; /* muted:%s */" % sib in out


# --------------------------------------------------------------------------
# instrument_for_counting — fixture 2: closure70 helper-method harness.
# --------------------------------------------------------------------------

def test_instrument_closure70_helper_method_oracle():
    src = _load()["closure70_oracle_and_escape"]["source"]
    # The target oracle throws inside the helper runOracle, not directly in
    # fuzzerTestOneInput — the static tally field is still reachable there.
    out = instrument_for_counting(src, "warning-array-consistency")
    assert out is not None
    assert "[oracle:warning-array-consistency]" not in out
    assert "{ __vpViolated++; }" in out
    for sib in ("duplicate-local-var-count", "duplicate-local-var-redef",
                "duplicate-local-var-init", "fresh-compiler-agreement"):
        assert "; /* muted:%s */" % sib in out
        assert "[oracle:%s]" % sib not in out
    assert out.count("static long __vpChecked = 0, __vpViolated = 0;") == 1
    assert "fuzzerTestOneInput" in out
    assert out.count("{") == out.count("}")
    # The untagged RuntimeException error-throws are NOT alarms -> untouched.
    assert 'throw new RuntimeException("parse failed:' in out


def test_instrument_closure70_mutes_untagged_fuzzer_security_issue():
    # The archived closure70 fixture happens to tag every FuzzerSecurityIssue
    # with an [oracle:] id, so to exercise untagged-alarm muting we splice one
    # bare (untagged) FuzzerSecurityIssue throw into the same real source and
    # confirm it is muted to `; /* muted:all */` (it would otherwise surface
    # and pollute the count).
    src = _load()["closure70_oracle_and_escape"]["source"]
    injected = src.replace(
        "    private static String normalize(String s) {",
        "    private static void bare() {\n"
        "        throw new com.code_intelligence.jazzer.api"
        ".FuzzerSecurityIssueLow(\"untagged boom\");\n"
        "    }\n"
        "    private static String normalize(String s) {",
        1)
    assert injected != src                       # the splice landed
    out = instrument_for_counting(injected, "warning-array-consistency")
    assert out is not None
    assert "; /* muted:all */" in out
    assert '"untagged boom"' not in out          # the untagged throw is gone
    assert out.count("{") == out.count("}")


# --------------------------------------------------------------------------
# The injected periodic print must satisfy relation_screen._STATS_RE.
# --------------------------------------------------------------------------

def test_injected_print_matches_relscreen_stats_re():
    from java.relations.relation_screen import _STATS_RE

    out = instrument_for_counting(
        _load()["math30_multi_oracle"]["source"], "u-sum")
    assert out is not None
    # The injected code prints exactly the relscreen line shape.
    assert 'System.err.println("[relscreen] checked=" + __vpChecked' in out
    assert '+ " violated=" + __vpViolated' in out
    # A concrete line that the injected println would emit at runtime parses
    # with relation_screen's own regex (checked=N violated=M).
    sample = "[relscreen] checked=%d violated=%d" % (1000, 3)
    m = _STATS_RE.search(sample)
    assert m is not None
    assert (int(m.group(1)), int(m.group(2))) == (1000, 3)


# --------------------------------------------------------------------------
# instrument_for_counting — failure modes -> None (fail-open at call site).
# --------------------------------------------------------------------------

def test_instrument_missing_target_returns_none():
    src = _load()["math30_multi_oracle"]["source"]
    assert instrument_for_counting(src, "no-such-oracle") is None


def test_instrument_no_fuzzer_entrypoint_returns_none():
    # A source with an alarm throw but no fuzzerTestOneInput cannot be
    # instrumented -> None.
    src = ('public class H {'
           '  void g() {'
           '    throw new com.code_intelligence.jazzer.api'
           '.FuzzerSecurityIssueLow("[oracle:only] mismatch");'
           '  }'
           '}')
    assert instrument_for_counting(src, "only") is None


def test_instrument_empty_inputs_return_none():
    src = _load()["math30_multi_oracle"]["source"]
    assert instrument_for_counting(src, "") is None
    assert instrument_for_counting("", "swap-symmetry") is None
    assert instrument_for_counting(None, "swap-symmetry") is None


def test_instrument_single_oracle_harness():
    # A single-oracle harness (the residual-FP target shape): the sole oracle
    # becomes the tally, no siblings to mute, scaffold injected, balanced.
    src = ('public class H {'
           '  public static void fuzzerTestOneInput('
           '      com.code_intelligence.jazzer.api.FuzzedDataProvider data) {'
           '    int x = data.consumeInt();'
           '    if (x == x + 1) {'
           '      throw new com.code_intelligence.jazzer.api'
           '.FuzzerSecurityIssueLow("[oracle:only] semantic mismatch");'
           '    }'
           '  }'
           '}')
    out = instrument_for_counting(src, "only")
    assert out is not None
    assert "[oracle:only]" not in out
    assert "{ __vpViolated++; }" in out
    assert "; /* muted:" not in out              # no siblings
    assert out.count("static long __vpChecked = 0, __vpViolated = 0;") == 1
    assert "__vpChecked++;" in out
    assert out.count("{") == out.count("}")


# --------------------------------------------------------------------------
# never_held_fact wording (unchanged — evidence_facts not touched by M-v2).
# --------------------------------------------------------------------------

def test_never_held_fact_wording():
    fact = never_held_fact(150)
    assert "NEVER been observed to hold" in fact
    assert "0/150" in fact
    # It states the domain framing that makes it a fact, not a plea.
    assert "declared domain" in fact


def test_never_held_fact_counts_are_verbatim():
    assert "0/2000" in never_held_fact(2000)
    assert "0/1" in never_held_fact(1)


# --------------------------------------------------------------------------
# relation_screen: existing entry points unchanged; M-v2 primitives present.
# --------------------------------------------------------------------------

def test_relation_screen_entry_points_and_primitives():
    import java.relations.relation_screen as rs

    # Existing screen entry points still present with unchanged signatures.
    screen_params = list(
        inspect.signature(rs.screen_relations).parameters)
    assert screen_params == [
        'candidates', 'builder', 'buggy_dir', 'jazzer_standalone_jar',
        'package', 'imports', 'jazzer_api_jar', 'trigger_literals', 'runs',
        'timeout_seconds', 'max_keep', 'repair_fn', 'harden_fn']

    replay_params = list(
        inspect.signature(rs.replay_on_patched).parameters)
    assert replay_params == [
        'relations', 'builder', 'patched_dir', 'jazzer_standalone_jar',
        'package', 'imports', 'jazzer_api_jar', 'trigger_literals', 'runs',
        'timeout_seconds']

    # The compile-and-measure primitive and the stats regex the M-v2 universal
    # path reuses.
    assert callable(getattr(rs, '_run_counting_fuzz', None))
    assert rs._STATS_RE.search("[relscreen] checked=5 violated=1")
