"""Dynamic-oracle-id family extraction (night20 vacuous-rejection fix).

night20 logged 7 family-novelty rejections; 6 were VACUOUS — the rejected
harness's extracted family set was EMPTY, so the gate bounced a harness for
"adding no new family" when in truth the extractor simply could not read its
checks. Root cause: those harnesses build their oracle id at runtime via the
template `throw new ...("[oracle:" + id + "] ...")` and never write a literal
`[oracle:<id>]`, so `_ORACLE_ID_RE` matched nothing. The id string literals are
present statically though — assigned to the sink variable (Lang-50) or passed
as the sink parameter of a helper (Chart-19 `fail`, Math-65
`requireApprox`/`assertClose`) — and `_dynamic_oracle_ids` now recovers them.

The six real rejected sources are archived verbatim in
tests/fixtures/dynamic_oracle_harnesses.json (provenance recorded there). These
tests assert:
  * every archived source now extracts NON-EMPTY families (all six are the
    fixable, statically-recoverable kind); and
  * the fail-open contract for the genuinely-dynamic kind that CANNOT be
    recovered statically — empty extraction is always accept-eligible, never a
    rejection — which is why fail-open must exist at all.

No LLM, no I/O beyond loading the fixture file.
"""
import json
import os

import pytest

from java.parsing.java_source import oracle_families, oracle_ids_in_text
from java.harness.campaign import novelty_verdict

_FIX = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures")


def _load():
    with open(os.path.join(_FIX, "dynamic_oracle_harnesses.json"),
              "r", encoding="utf-8") as fh:
        return json.load(fh)


_H = _load()

# Every archived source, and a couple of ids each that MUST be recovered — a
# non-emptiness check alone could pass on a single stray literal, so we pin the
# real oracle ids the harness meant to name.
_EXPECT = {
    # Values are FAMILY STEMS (trailing/enumerating digits stripped by
    # oracle_family_stem), not raw ids: `lifted-format1-locale` stems to
    # `lifted-format-locale`, `lifted-rangeAxis1` to `lifted-rangeaxis`.
    "lang50_dynamic_assign_a": {"lifted-format-locale",
                                "global-defaults-unchanged"},
    "lang50_dynamic_assign_b": {"date-default-pattern-us",
                                "getinstance-jdk-render-de"},
    "chart19_fail_helper_a": {"lifted-rangeaxis", "writer-reader-slot"},
    "chart19_fail_helper_b": {"rangeaxis-writer-reader-a",
                              "test-range-axis-index-primary"},
    "math65_requireapprox": {"chi-known-answer", "lifted-cov"},
    "math65_assertclose": {"rms-chi-square-consistency", "lifted-evaluations"},
}


@pytest.mark.parametrize("key", sorted(_H))
def test_archived_source_extracts_nonempty_families(key):
    """Every one of the six night20-vacuous sources now extracts a NON-EMPTY
    family set — so the family-novelty gate has real evidence to judge and the
    vacuous rejection that actually happened can no longer happen."""
    fams = oracle_families(_H[key]["source"])
    assert fams, (
        f"{key} still extracts NO families — the vacuous-rejection bug would "
        f"reappear. provenance: {_H[key]['provenance']['trace']}")


@pytest.mark.parametrize("key", sorted(_EXPECT))
def test_archived_source_recovers_expected_family_stems(key):
    fams = oracle_families(_H[key]["source"])
    missing = _EXPECT[key] - fams
    assert not missing, f"{key}: expected family stems not recovered: {missing}"


def test_all_six_would_have_passed_the_gate():
    """Directly the plan's done-criterion: none of the six extracts empty, so
    for none of them does the gate's empty-extraction branch (fail-open) or a
    vacuous reject apply — each carries families the gate can weigh."""
    empties = {k for k in _H if not oracle_families(_H[k]["source"])}
    assert not empties, f"still empty (would be fail-open, not clean): {empties}"


# --------------------------------------------------------------------------
# The genuinely-dynamic kind that CANNOT be recovered statically -> fail-open.
# --------------------------------------------------------------------------

# The oracle id here is COMPUTED (`"cov-" + i + "-" + j`), so there is no string
# literal to recover. This is the class the extractor legitimately cannot read,
# and precisely why the gate must fail open rather than reject.
_TRULY_DYNAMIC = """
package demo;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                if (cov[i][j] != cov[j][i]) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:cov-" + i + "-" + j + "] semantic mismatch");
                }
            }
        }
    }
}
"""


def test_truly_dynamic_id_extracts_empty():
    # No static literal flows into the sink -> nothing to recover. Expected.
    assert oracle_families(_TRULY_DYNAMIC) == set()
    assert oracle_ids_in_text(_TRULY_DYNAMIC) == set()


def test_truly_dynamic_id_gate_path_fails_open():
    # This is the whole point of fail-open: an unrecoverable (empty) family set
    # is accept-eligible at the pure-predicate boundary the campaign gate uses,
    # no matter how many rejections have been spent.
    fams = oracle_families(_TRULY_DYNAMIC)
    assert fams == set()
    assert novelty_verdict(fams, {"some-accepted-family"}, 0) == "accept"
    assert novelty_verdict(fams, {"some-accepted-family"}, 99) == "accept"


def test_novelty_verdict_empty_is_accept_regression():
    # The plan's explicit regression: novelty_verdict(set(), anything, 0)
    # == 'accept'. A vacuous rejection is impossible by construction.
    assert novelty_verdict(set(), {"a", "b"}, 0) == "accept"
    assert novelty_verdict(set(), set(), 0) == "accept"
