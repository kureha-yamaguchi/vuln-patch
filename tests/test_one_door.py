"""Spec K (cycle-3): tests for the pure one-door relation matcher.

Pinned API (implemented in the harness-track wiring's companion helper):

    java.relations.evidence_facts.match_oracle_to_relation(
        oracle_id, fired_msg, relation_names) -> str|None

Motivation (RETRO2 Math-73-c): the replay-track verifier receives the
fire-rate / screen-decision facts for a screened relation and rules it UNSOUND;
the harness-track verifier judges the SAME underlying check with none of those
facts and keeps it -> FP. Fact parity requires first MATCHING a harness firing
to the screened relation it duplicates. This matcher must never guess: it
returns a relation name only on an exact normalized-id equality or a single
distinctive (>=6-char) token that belongs to exactly one relation name.

The realistic pair from the retro (a hyphenated oracle id shadowing a camelCase
relation name) drives the token cases: oracle 'exact-endpoint-root' vs relation
'endpointRootConsistency', where the >=6-char token 'endpoint' is decisive.

Assertions are on the returned name / None, never on internals.
"""
from java.relations.evidence_facts import match_oracle_to_relation


def test_exact_normalized_match():
    """Lowercase + strip-non-alnum equality between the oracle id and a
    relation name matches even across hyphen/camelCase spelling."""
    names = ["exactEndpointRoot", "someUnrelatedContract"]
    assert match_oracle_to_relation(
        "exact-endpoint-root", "relation exact-endpoint-root violated",
        names) == "exactEndpointRoot"


def test_distinctive_token_unique_matches_name():
    """A >=6-char token shared by the oracle id and exactly one relation name
    (as a substring of its normalized form) selects that relation."""
    names = ["endpointRootConsistency", "meanValueBound"]
    assert match_oracle_to_relation(
        "exact-endpoint-root",
        "relation exact-endpoint-root violated: endpoint mismatch",
        names) == "endpointRootConsistency"


def test_token_from_fired_message_only():
    """The token pool is drawn from the fired message as well as the oracle
    id; a distinctive token present only in the message still matches."""
    names = ["endpointRootConsistency", "meanValueBound"]
    assert match_oracle_to_relation(
        "check-3",
        "relation check-3 violated: endpoint drift observed",
        names) == "endpointRootConsistency"


def test_ambiguous_token_returns_none():
    """A distinctive token shared by TWO relation names is ambiguous and must
    not resolve to either (conservative: None)."""
    names = ["endpointRootConsistency", "endpointSlopeCheck"]
    assert match_oracle_to_relation(
        "exact-endpoint-root",
        "relation exact-endpoint-root violated: endpoint mismatch",
        names) is None


def test_short_tokens_do_not_match():
    """Tokens under 6 chars never license a token match; with no exact id
    equality the result is None."""
    names = ["rootConsistencyContract", "slopeBound"]
    # 'exact'(5), 'root'(4) are the only shared-ish tokens and both <6.
    assert match_oracle_to_relation(
        "exact-root", "relation exact-root violated", names) is None


def test_no_match_returns_none():
    """Neither an id equality nor a distinctive shared token -> None."""
    names = ["endpointRootConsistency", "meanValueBound"]
    assert match_oracle_to_relation(
        "monotonic-series", "relation monotonic-series violated",
        names) is None


def test_empty_relation_names_returns_none():
    """No relations in scope -> nothing to match."""
    assert match_oracle_to_relation("anything", "msg", []) is None
    assert match_oracle_to_relation("anything", "msg", None) is None
