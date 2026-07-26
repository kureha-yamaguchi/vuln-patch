"""Family-novelty harness steering.

Later harnesses in a set are supposed to interrogate the root cause from
DIFFERENT angles; the observed failure (width5 20260725) is that a later
harness re-skins an earlier check under a new [oracle:<id>] — same observable,
new name — adding no evidence. These tests pin the three mechanical pieces:

  * `oracle_family_stem` / `oracle_families` — the id -> FAMILY stem rule that
    collapses instance-numbered ids into one family while keeping genuinely
    distinct checks distinct;
  * `novelty_verdict` — the pure accept/reject decision (with its bounded
    fail-open) the campaign gate is built on;
  * the prompt block — the mechanically-computed family list is present in the
    later-attempt steering block.

No LLM, no I/O.
"""
from java.parsing.java_source import oracle_family_stem, oracle_families
from java.harness.campaign import novelty_verdict
from java.harness.prompts import PromptBuilder


# --------------------------------------------------------------------------
# oracle_family_stem — the stemming rule
# --------------------------------------------------------------------------

def test_stem_strips_trailing_dash_number_suffix():
    assert oracle_family_stem("lifted-after-4") == "lifted-after"
    assert oracle_family_stem("lifted-after-1") == "lifted-after"


def test_stem_lifted_after_family_is_one_family():
    # -1 and -4 are the SAME family (that is the whole point).
    assert oracle_family_stem("lifted-after-1") == \
        oracle_family_stem("lifted-after-4")


def test_stem_strips_trailing_digits_inside_a_token():
    assert oracle_family_stem("jennrich-param1") == "jennrich-param"
    assert oracle_family_stem("jennrich-param0") == "jennrich-param"
    assert oracle_family_stem("jennrich-param0") == \
        oracle_family_stem("jennrich-param1")


def test_stem_strips_mid_token_digit_run():
    # Digits in a NON-trailing token are stripped too.
    assert oracle_family_stem("fr-case1-rms") == "fr-case-rms"
    assert oracle_family_stem("fr-case2-rms") == "fr-case-rms"


def test_stem_lowercases():
    assert oracle_family_stem("Lifted-After-4") == "lifted-after"
    assert oracle_family_stem("indexOf-agrees") == "indexof-agrees"


def test_distinct_families_stay_distinct():
    # No enumerating digits: unchanged, and different observables differ.
    assert oracle_family_stem("contains-capacity") == "contains-capacity"
    assert oracle_family_stem("contains-length") == "contains-length"
    assert oracle_family_stem("contains-capacity") != \
        oracle_family_stem("contains-length")
    assert oracle_family_stem("inverse-cdf-lower") != \
        oracle_family_stem("inverse-cdf-upper")


def test_stem_meaningful_word_tokens_are_kept():
    # 'lifted' is a real family word, never dropped as generic.
    assert oracle_family_stem("lifted") == "lifted"
    assert oracle_family_stem("lifted-empty") == "lifted-empty"


def test_stem_pure_number_and_empty_collapse():
    assert oracle_family_stem("") == ""
    assert oracle_family_stem("4") == ""
    assert oracle_family_stem("oracle-123") == "oracle"


# --------------------------------------------------------------------------
# oracle_families — extraction over harness text
# --------------------------------------------------------------------------

_HARNESS_FIXTURE = '''
package org.apache.commons.lang3;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        if (bad1) throw new com.code_intelligence.jazzer.api
            .FuzzerSecurityIssueLow("[oracle:lifted-after-1] semantic mismatch");
        if (bad2) throw new com.code_intelligence.jazzer.api
            .FuzzerSecurityIssueLow("[oracle:lifted-after-2] semantic mismatch");
        if (bad3) throw new RuntimeException(
            "[oracle:contains-capacity] consistency violation");
        if (bad4) throw new RuntimeException("relation add-overloads-agree violated: x");
    }
}
'''


def test_oracle_families_extracts_and_stems():
    fams = oracle_families(_HARNESS_FIXTURE)
    # The two lifted-after-N ids collapse to ONE family.
    assert "lifted-after" in fams
    assert "contains-capacity" in fams
    # Relation-format alarms are extracted too (reuses oracle_ids_in_text).
    assert "add-overloads-agree" in fams
    # lifted-after-1 and lifted-after-2 do not appear as separate families.
    assert "lifted-after-1" not in fams
    assert "lifted-after-2" not in fams
    assert len(fams) == 3


def test_oracle_families_empty_text():
    assert oracle_families("") == set()
    assert oracle_families("no oracles here") == set()


# --------------------------------------------------------------------------
# novelty_verdict — the pure gate decision
# --------------------------------------------------------------------------

def test_verdict_accepts_a_new_family():
    assert novelty_verdict({"contains-length"}, {"contains-capacity"}, 0) \
        == "accept"


def test_verdict_rejects_no_new_family_within_budget():
    assert novelty_verdict({"contains-capacity"}, {"contains-capacity"}, 0) \
        == "reject"
    assert novelty_verdict({"contains-capacity"}, {"contains-capacity"}, 1) \
        == "reject"


def test_verdict_fails_open_at_budget():
    # After two novelty-rejections the gate accepts a redundant harness.
    assert novelty_verdict({"contains-capacity"}, {"contains-capacity"}, 2) \
        == "accept"
    assert novelty_verdict({"contains-capacity"}, {"contains-capacity"}, 3) \
        == "accept"


def test_verdict_accepts_when_some_family_is_new():
    # A partly-overlapping harness still adds a family -> accept.
    assert novelty_verdict({"contains-capacity", "getter-readonly"},
                           {"contains-capacity"}, 0) == "accept"


def test_verdict_empty_family_set_always_accepts():
    # FAIL-OPEN ON EMPTY EXTRACTION (belt and braces): an empty candidate
    # family set is a parse failure, never redundancy — it can NEVER be a
    # rejection, regardless of the rejection budget. (This is the direct fix
    # for night20's six vacuous family-novelty rejections, whose rejected
    # harnesses all extracted to {}.)
    assert novelty_verdict(set(), {"contains-capacity"}, 0) == "accept"
    assert novelty_verdict(set(), {"contains-capacity"}, 1) == "accept"
    assert novelty_verdict(set(), {"contains-capacity"}, 2) == "accept"
    # No accepted families yet either -> still accept.
    assert novelty_verdict(set(), set(), 0) == "accept"


def test_verdict_custom_max_rejections():
    assert novelty_verdict({"a"}, {"a"}, 0, max_rejections=1) == "reject"
    assert novelty_verdict({"a"}, {"a"}, 1, max_rejections=1) == "accept"


# --------------------------------------------------------------------------
# Prompt block presence
# --------------------------------------------------------------------------

def test_variant_block_lists_covered_families():
    pb = PromptBuilder()
    block = pb._variant_analysis_block(
        reachable=["org.example.Foo.bar"],
        covered=["org.example.Foo.bar"],
        signatures=[],
        accepted_families=["contains-capacity", "lifted-after"],
    )
    assert "Check FAMILIES already covered by accepted harnesses:" in block
    assert "contains-capacity" in block
    assert "lifted-after" in block
    assert "REJECTED unless it fires at least one check OUTSIDE these " \
        "families" in block


def test_variant_block_no_families_omits_the_line():
    pb = PromptBuilder()
    block = pb._variant_analysis_block(
        reachable=["org.example.Foo.bar"],
        covered=[],
        signatures=[],
        accepted_families=[],
    )
    assert "Check FAMILIES already covered" not in block
