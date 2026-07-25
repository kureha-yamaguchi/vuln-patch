"""Spec M (cycle-3b): tests for universal screening's pure pieces.

Three surfaces, all unit-testable without a JVM:

  * java.parsing.java_source.extract_oracle_check(harness_source, oracle_id)
    -> str|None — the per-oracle check-body extractor, exercised on TWO real
    archived harness sources (tests/fixtures/harness_sources.json).
  * java.relations.evidence_facts.never_held_fact(checked) -> str — wording.
  * java.relations.relation_screen — the measuring helper needs a JVM, so we
    only assert the module still imports and its existing screen entry points
    keep their signatures (import-and-getattr), plus measure_single_check is
    exposed.

The extracted block for BOTH fixtures legitimately SPANS its siblings: each
fixture puts every oracle in ONE method body with no per-oracle sub-block, so
the smallest brace block holding both a throw and its feeding computation IS
that whole method body. Per the spec that is acceptable; we therefore assert
the returned block CONTAINS the target throw and is a brace-balanced substring
(and document the spanning by asserting a sibling is present too).
"""
import inspect
import json
import os

from java.parsing.java_source import (extract_oracle_check, match_brace,
                                      strip_comments)
from java.relations.evidence_facts import never_held_fact

_FIXTURES = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "fixtures", "harness_sources.json")


def _load():
    with open(_FIXTURES) as fh:
        return json.load(fh)


def _brace_balanced(block):
    """A block that starts at '{' whose matching close is its final char.

    Comments are stripped first: match_brace is literal-aware but NOT
    comment-aware, and a `//` comment carrying an apostrophe (`library's`)
    would otherwise be mis-read as a char literal. The extractor itself uses a
    comment-aware scanner, so this only affects the test's own check."""
    if block is None or not block.startswith("{") or not block.rstrip().endswith("}"):
        return False
    stripped = strip_comments(block).rstrip()
    return match_brace(stripped, 0) == len(stripped) - 1


# --------------------------------------------------------------------------
# extract_oracle_check — fixture 1: math30 multi-oracle (single method).
# --------------------------------------------------------------------------

def test_extract_math30_swap_symmetry():
    src = _load()["math30_multi_oracle"]["source"]
    block = extract_oracle_check(src, "swap-symmetry")
    assert block is not None
    # Contains the target oracle's throw.
    assert "[oracle:swap-symmetry]" in block
    # Brace-balanced substring of the harness source.
    assert block in src
    assert _brace_balanced(block)
    # Documented spanning: the conservative block is the whole
    # fuzzerTestOneInput body, so a sibling oracle rides along.
    assert "[oracle:u-sum]" in block


def test_extract_math30_lifted_big_dataset():
    src = _load()["math30_multi_oracle"]["source"]
    block = extract_oracle_check(src, "lifted-big-dataset")
    assert block is not None
    assert "[oracle:lifted-big-dataset]" in block
    assert block in src
    assert _brace_balanced(block)
    # The feeding computation for this oracle is inside the returned block.
    assert "mannWhitneyUTest(d1, d2)" in block


# --------------------------------------------------------------------------
# extract_oracle_check — fixture 2: closure70 helper-method harness.
# --------------------------------------------------------------------------

def test_extract_closure70_warning_array_consistency():
    src = _load()["closure70_oracle_and_escape"]["source"]
    block = extract_oracle_check(src, "warning-array-consistency")
    assert block is not None
    assert "[oracle:warning-array-consistency]" in block
    assert block in src
    assert _brace_balanced(block)
    # runOracle's feeding computation is present; siblings ride along (the
    # block is runOracle's whole body — documented spanning).
    assert "compiler.parseTestCode(js)" in block
    assert "[oracle:fresh-compiler-agreement]" in block


def test_extract_closure70_duplicate_local_var_init():
    src = _load()["closure70_oracle_and_escape"]["source"]
    block = extract_oracle_check(src, "duplicate-local-var-init")
    assert block is not None
    assert "[oracle:duplicate-local-var-init]" in block
    assert _brace_balanced(block)


# --------------------------------------------------------------------------
# extract_oracle_check — ambiguous / missing -> None.
# --------------------------------------------------------------------------

def test_extract_missing_id_returns_none():
    src = _load()["math30_multi_oracle"]["source"]
    assert extract_oracle_check(src, "no-such-oracle") is None


def test_extract_empty_inputs_return_none():
    src = _load()["math30_multi_oracle"]["source"]
    assert extract_oracle_check(src, "") is None
    assert extract_oracle_check("", "swap-symmetry") is None
    assert extract_oracle_check(None, "swap-symmetry") is None


def test_extract_repeated_tag_is_ambiguous():
    # The same tag appearing twice cannot be resolved to one throw -> None.
    dup = ('public class H { void f() {'
           ' throw new RuntimeException("[oracle:dup] a");'
           ' throw new RuntimeException("[oracle:dup] b"); } }')
    assert extract_oracle_check(dup, "dup") is None


def test_extract_single_oracle_isolated_block():
    # A single-oracle harness (the residual-FP target case): the returned
    # block is the fuzzerTestOneInput body, contains the throw, and does NOT
    # span any sibling (there is none).
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
    block = extract_oracle_check(src, "only")
    assert block is not None
    assert "[oracle:only]" in block
    assert "data.consumeInt()" in block          # feeding computation present
    assert _brace_balanced(block)
    # Not the bare guard: the block is larger than just `{ throw ...; }`.
    assert "consumeInt" in block


# --------------------------------------------------------------------------
# never_held_fact wording.
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
# relation_screen: existing entry points unchanged; helper exposed.
# --------------------------------------------------------------------------

def test_relation_screen_imports_and_entry_points_unchanged():
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


def test_measure_single_check_exposed():
    import java.relations.relation_screen as rs

    assert callable(getattr(rs, 'measure_single_check', None))
    # Signature leads with the check source, then builder/dir/jars, per spec.
    params = list(inspect.signature(rs.measure_single_check).parameters)
    assert params[:5] == [
        'check_source', 'builder', 'buggy_dir', 'jazzer_standalone_jar',
        'jazzer_api_jar']
    assert 'runs' in params
