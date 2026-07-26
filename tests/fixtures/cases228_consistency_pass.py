#!/usr/bin/env python3
"""Deterministic consistency pass over tests/fixtures/cases228.jsonl.

De-circularises the `_o`-leg gold labels: any (bug, physical-check-PROPERTY) group
that carried BOTH keep(SOUND) and dismiss(UNSOUND) golds is resolved to a single gold
using RECORDED evidence only (no LLM, no invented verdicts).  See the report for the
per-group evidence.  Rows left untouched: all `_c` rows, the 4 confirmed drift-kills,
already-unresolved rows, and rows whose property was never in conflict.
"""
import json, collections, os

SRC = "tests/fixtures/cases228.jsonl"

GOLD_TO_LABEL = {"SOUND": "overfitting", "UNSOUND": "correct", "UNRESOLVED": "unresolved"}

# Each conflict group: target gold + the set of inventory rows that flip, plus metadata.
GROUPS = [
    dict(
        key="Lang-60 / contains(char) is observationally read-only "
            "(capacity + indexOf + hidden state)",
        target="SOUND",
        flip_rows={10, 12, 68, 104, 196, 198},   # UNSOUND -> SOUND
        members=[10, 12, 67, 68, 104, 135, 136, 196, 197, 198, 200],
        reason="Same physical check (contains(char) must not change the builder's "
               "observable state) was judged SOUND in some rolls and UNSOUND in others; "
               "recorded evidence resolves it to a genuine catch -> keep.",
        evidence="poolB kept this family and caught the overfit TWICE (row135 "
                 "contains-readonly-index, row136 contains-readonly; both TP) and pool30 "
                 "kept row67 (TP). Rejected-ideas ledger (docs/plan.md 'Mechanical "
                 "auto-dismissal of latent firings') states the auto-dismissal 'killed "
                 "the true Lang-60-o capacity catch (minfix_w1)'. width5 row197 "
                 "(observed-impossible) plus the drift-kill relation row200 "
                 "(contains_does_not_change_capacity: silent-on-buggy 0/20k + "
                 "deterministic 2/2 on the failing test, inventory 5c drift-kill #4) show "
                 "capacity is an explicit observable a correct contains(char) cannot "
                 "change. The 'lazy compaction / minimizeCapacity first' dismissals are "
                 "uncorroborated 'could' hypotheticals (the drift signature). "
                 "MERGE NOTE: grouped across differing ids "
                 "(contains-readonly[-seed/-capacity/-index], contains-capacity"
                 "[-stable-seed], contains_does_not_change_capacity) because every "
                 "fired_assertion asserts the same invariant (capacity/indexOf unchanged "
                 "by a completed contains query). EXCLUDED as different properties: "
                 "row11 token-seed-removed-char-contains (absence-after-delete; premise "
                 "broken) and row199 constructed-absence (broken-check, needle "
                 "reintroduced) -> both stay UNSOUND.",
    ),
    dict(
        key="Chart-7 / getMaxMiddleIndex points to the maximum middle",
        target="SOUND",
        flip_rows={69, 105},                       # UNSOUND -> SOUND
        members=[13, 69, 105, 137, 138, 201, 203, 205, 206],
        reason="The max-middle-index invariant was judged SOUND in some rolls and "
               "UNSOUND in two; recorded catches resolve it to keep.",
        evidence="Kept and caught (TP) across night20 (row13), poolB (rows137,138) and "
                 "width5 (rows201,203,205,206), backed by the trusted regression test "
                 "testGetMaxMiddleIndex (rows138,205,206). width5 row202 shows the "
                 "overfit patch is genuinely broken (patch-introduced IndexOutOfBounds, "
                 "TP). The two dismissals are contradicted by those concrete catches: "
                 "row69 'integer-average middle could differ' (alt-definition 'could' "
                 "hypothetical) and row105 constructed-family 'check internally wrong "
                 "about its own constructed periods' (broken-check) -- no dev-fix "
                 "adjudication marks the property unsound. MERGE NOTE: row105 "
                 "(id 'constructed-family', no 'middle' token) grouped by fired_assertion "
                 "('fixes max-middle index at 1'). EXCLUDED: row204 overload-eq "
                 "(Number-subtype equivalence) and row202 crash -> different properties.",
    ),
    dict(
        key="Lang-41 / getPackageName(Class) agrees with "
            "getPackageCanonicalName(canonical String)",
        target="SOUND",
        flip_rows={189},                            # UNSOUND -> SOUND
        members=[189, 190, 191],
        reason="Same-leg, same-property conflict (width5 Lang-41 leg = rows 189/190/191 "
               "only); the property caught the overfit -> keep.",
        evidence="The width5 Lang-41 leg outcome is TP entirely via this canonical-name "
                 "agreement family. Rows190 (observed-impossible: 'for observed "
                 "java.util.Map.Entry input the values are determined') and 191 "
                 "(trusted-lift) were kept and caught the overfit. Row189's dismissal "
                 "'canonical-name string alone can't disambiguate nested classes' is a "
                 "generic 'could' hypothetical that row190 rebuts with the concrete "
                 "nested-class input (Map.Entry). No recorded evidence the property is "
                 "unsound.",
    ),
    dict(
        key="Closure-92 / lifted toSource() must equal a hard-coded exact literal "
            "(provideInIndependentModules)",
        target="UNSOUND",
        flip_rows={71},                             # SOUND -> UNSOUND
        members=[14, 15, 16, 17, 71, 72, 106, 107, 139, 140, 141],
        reason="The lifted exact-literal string comparison was judged UNSOUND in every "
               "roll but one; recorded evidence shows the check is non-discriminating -> "
               "dismiss.",
        evidence="Rejected-ideas ledger (docs/plan.md): 'Raw-string comparison of lifted "
                 "code/text outputs ... fires on formatting deltas, hands the judge a "
                 "legitimate dismissal ... all three Closure-92-o firings in full30. "
                 "Always whitespace-normalize.' Every member carries the mechanical fact "
                 "fires-on-buggy-same-check (fires on the ORIGINAL buggy build too -> "
                 "non-discriminating), and several members' expected literals are "
                 "themselves malformed (missing trailing ';' vs the trusted compiler "
                 "output; row140 seed-regression 'hard-codes wrong expected string', "
                 "broken-check). The lone SOUND keep (row71, trusted-lift) is a "
                 "provenance-override of those mechanical facts; the pool30 leg-TP was a "
                 "non-discriminating (lucky) fire, not a sound catch. EXCLUDED as "
                 "different properties: row70 constructed-star (reserved-token parse "
                 "latitude) and row142 invalid_provide_name_reports_error.",
    ),
]

# Documented deliberate NON-MERGES (naive name/stem collisions that are NOT one check).
NON_MERGES = [
    ("Lang-50 rows 8 vs 195 (relation dateInstance_default_overload_agrees_with_"
     "explicit_default_l): NOT merged. row8 (night20) is a documented genuine check bug "
     "-- it compares Locale objects with '!=' (identity) (inventory 5c drift-kill-"
     "candidate #6; cycle-5 5D requires this 'justified kill' to STAY UNSOUND). row195 "
     "(width5) is a sound instance of the positive-control agreement property (harness "
     "pins Locale.setDefault; Lang-50 family kept SOUND 14/15). Per the rejected-ideas "
     "rule 'a generic name labels different checks in different harnesses', these are "
     "distinct implementations under a collided truncated name. row8 stays UNSOUND, "
     "row195 stays SOUND."),
    ("Math-73 rows 89 vs 121 (oracle:overload-agreement): NOT merged. Fired-assertions "
     "show different physical checks -- row89 asserts both overloads must THROW on a "
     "non-bracketing triple ('non-bracketing overloads disagree', throw-duty, SOUND); "
     "row121 asserts the two overloads return the same numeric ROOT "
     "('solve(f,-1,1,0.011)=3.357...', tolerance-floor: only 1e-6 accuracy promised, "
     "UNSOUND). Distinct properties; no conflict."),
]


def main():
    rows = [json.loads(l) for l in open(SRC)]
    old_dist = collections.Counter(r["gold"] for r in rows)

    # inventory_row -> target gold, group key/reason/evidence
    flip = {}
    for g in GROUPS:
        for ir in g["flip_rows"]:
            flip[ir] = g

    changed = 0
    for r in rows:
        ir = r["provenance"]["inventory_row"]
        if ir in flip and r["provenance"].get("leg_label") == "o":
            g = flip[ir]
            assert r["gold"] != g["target"], f"row{ir} already {g['target']}"
            old = r["gold"]
            r["gold"] = g["target"]
            r["label"] = GOLD_TO_LABEL[g["target"]]
            r["gold_resolution"] = {
                "reason": g["reason"],
                "group_key": g["key"],
                "evidence": g["evidence"],
                "old_gold": old,
                "new_gold": g["target"],
            }
            changed += 1

    with open(SRC, "w") as fh:
        for r in rows:
            fh.write(json.dumps(r, ensure_ascii=False) + "\n")

    new_dist = collections.Counter(r["gold"] for r in rows)
    print("changed rows:", changed)
    print("old gold dist:", dict(old_dist))
    print("new gold dist:", dict(new_dist))
    return rows, old_dist, new_dist, changed


if __name__ == "__main__":
    main()
