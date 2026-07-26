# cases228 consistency pass (judge-replay gold de-circularisation)

Deterministic, offline, no LLM. Fixes the circularity whereby `_o`-leg gold labels
(other than the 4 confirmed drift-kills and the already-unresolved rows) were set equal
to the historical judge's own SOUND/UNSOUND reasoning class -- so the SAME physical check
carried gold=keep in some rolls and gold=dismiss in others, which no consistent scorer
can satisfy.

Method: group `_o`-leg rows by (bug, physical-check PROPERTY) -- merging across differing
oracle ids where the fired_assertion asserts the same invariant, and deliberately NOT
merging same-named checks that assert different invariants. Any group carrying BOTH
keep(SOUND) and dismiss(UNSOUND) golds is a CONFLICT, resolved to ONE gold using recorded
evidence only (a roll where the check fired and the leg was caught/TP; the 1d02859 dev-fix
replay adjudication; the docs/plan.md rejected-ideas ledger). Where no recorded evidence
resolves a conflict the whole group would be marked unresolved -- none of the four groups
below needed that.

Changed rows carry a `gold_resolution` field. `label` (overfitting/correct/unresolved) was
flipped in lockstep with `gold` (SOUND/UNSOUND/UNRESOLVED). 10 rows changed.

## Conflict groups found and resolved (`*` = row changed)

### 1. Lang-60 -- contains(char) is observationally read-only (capacity/indexOf/hidden state) -> KEEP (SOUND)
Old golds: SOUND{67,135,136,197,200} vs UNSOUND{10,12,68,104,196,198}.
Evidence: poolB kept it and caught the overfit TWICE (rows135,136 TP); pool30 kept row67 (TP).
Rejected-ideas ledger records auto-dismissal "killed the true Lang-60-o capacity catch (minfix_w1)".
Drift-kill relation row200 (silent-on-buggy 0/20k + deterministic 2/2) + row197 (observed-impossible)
confirm capacity is an explicit observable a correct contains cannot change; the "lazy compaction"
dismissals are uncorroborated "could" hypotheticals. Excluded (different properties, stay UNSOUND):
row11 token-seed-removed-char-contains, row199 constructed-absence (broken-check).
* row 10 night20 oracle:contains-readonly-seed                           -> SOUND
* row 12 night20 oracle:contains-capacity-stable-seed                    -> SOUND
  row 67 pool30  oracle:contains-readonly                                -> SOUND
* row 68 pool30  oracle:contains-readonly-capacity                       -> SOUND
* row104 poolA   oracle:contains-readonly-capacity                       -> SOUND
  row135 poolB   oracle:contains-readonly-index                          -> SOUND
  row136 poolB   oracle:contains-readonly                                -> SOUND
* row196 width5  oracle:contains-readonly-capacity                       -> SOUND
  row197 width5  oracle:contains-readonly-capacity                       -> SOUND
* row198 width5  oracle:contains-capacity                                -> SOUND
  row200 width5  relation contains_does_not_change_capacity              -> SOUND

### 2. Chart-7 -- getMaxMiddleIndex points to the maximum middle -> KEEP (SOUND)
Old golds: SOUND{13,137,138,201,203,205,206} vs UNSOUND{69,105}.
Evidence: kept + caught (TP) in night20/poolB/width5, backed by trusted test testGetMaxMiddleIndex;
width5 row202 shows the overfit is genuinely broken (patch-introduced IndexOutOfBounds, TP).
Dismissals row69 (alt-definition "could") and row105 (broken-check) are contradicted by the catches;
no dev-fix adjudication marks the property unsound. Excluded: row204 overload-eq, row202 crash.
  row 13 night20 oracle:constructed-middle                               -> SOUND
* row 69 pool30  relation maxMiddleIndex_points_to_a_maximum_middle      -> SOUND
* row105 poolA   oracle:constructed-family                               -> SOUND
  row137 poolB   oracle:max-middle-maximum                               -> SOUND
  row138 poolB   relation max_middle_index_points_to_a_maximum_middle    -> SOUND
  row201 width5  relation adding_higher_end_but_lower_middle_does_not_change_maxMiddle -> SOUND
  row203 width5  oracle:higher-end-lower-middle                          -> SOUND
  row205 width5  relation adding_higher_end_but_lower_middle_does_not_change_maxMiddle -> SOUND
  row206 width5  relation adding_higher_middle_but_lower_end_must_change_maxMiddle -> SOUND

### 3. Lang-41 -- getPackageName(Class) agrees with getPackageCanonicalName(String) -> KEEP (SOUND)
Old golds: SOUND{190,191} vs UNSOUND{189}. Same-leg conflict (width5 Lang-41 leg = these 3 only;
leg TP entirely via this family). row190 (observed-impossible, concrete Map.Entry input) and row191
(trusted-lift) caught the overfit; row189's "canonical can't disambiguate nested classes" is a generic
"could" hypothetical rebutted by row190's concrete nested-class input. No unsound evidence.
* row189 width5  oracle:packageName-class-vs-canonical                   -> SOUND
  row190 width5  oracle:pkg-class-canonical-agreement                    -> SOUND
  row191 width5  relation packageName_class_overload_agrees_with_canonical_string_over -> SOUND

### 4. Closure-92 -- lifted toSource() must equal a hard-coded exact literal -> DISMISS (UNSOUND)
Old golds: SOUND{71} vs UNSOUND{14,15,16,17,72,106,107,139,140,141}.
Evidence: rejected-ideas ledger -- "Raw-string comparison of lifted outputs ... fires on formatting
deltas, hands the judge a legitimate dismissal ... Always whitespace-normalize." Every member carries
fires-on-buggy-same-check (non-discriminating); several expected literals are malformed (missing ';';
row140 "hard-codes wrong expected string"). The lone SOUND keep (row71) is a provenance-override of
those mechanical facts and the pool30 leg-TP was a lucky non-discriminating fire. Excluded: row70
constructed-star, row142 invalid_provide_name_reports_error.
  row 14 night20 oracle:lifted-testProvideInIndependentModules4          -> UNSOUND
  row 14 night20 oracle:lifted-testProvideInIndependentModules4          -> UNSOUND
  row 15 night20 oracle:lifted-seed-exact                                -> UNSOUND
  row 16 night20 oracle:lifted-bug261                                    -> UNSOUND
  row 17 night20 oracle:lifted-independent-modules4                      -> UNSOUND
* row 71 pool30  oracle:lifted-test                                      -> UNSOUND
  row 72 pool30  oracle:testProvideInIndependentModules4                 -> UNSOUND
  row106 poolA   oracle:lifted-pairs                                     -> UNSOUND
  row107 poolA   oracle:lifted-seed                                      -> UNSOUND
  row139 poolB   oracle:lifted-testProvideInIndependentModules4          -> UNSOUND
  row140 poolB   oracle:seed-regression                                  -> UNSOUND
  row141 poolB   oracle:lifted-test                                      -> UNSOUND

## Deliberate NON-MERGES (naive name/stem collisions that are NOT one physical check; unchanged)

- Lang-50 rows 8 vs 195 (relation dateInstance_default_overload_agrees_with_explicit_default_l):
  row8 (night20) is a documented genuine check bug -- compares Locale objects with `!=` (identity)
  (inventory 5c #6; cycle-5 5D requires this "justified kill" to STAY UNSOUND). row195 (width5) is a
  sound instance of the positive-control agreement property (harness pins Locale.setDefault; family kept
  SOUND 14/15). Distinct implementations under a collided truncated name -> row8 UNSOUND, row195 SOUND.
- Math-73 rows 89 vs 121 (oracle:overload-agreement): different invariants by fired_assertion --
  row89 = both overloads must THROW on a non-bracketing triple (throw-duty, SOUND); row121 = the two
  overloads return the same numeric ROOT within 1e-6 (tolerance-floor, UNSOUND). No conflict.

## Gold distribution: old vs new

| gold      | old | new | delta |
|-----------|-----|-----|-------|
| SOUND (keep/overfitting)   | 65  | 73  | +8 |
| UNSOUND (dismiss/correct)  | 145 | 137 | -8 |
| UNRESOLVED                 | 18  | 18  | +0 |
| total                      | 228 | 228 | 0 |

Post-condition verified: no (bug, physical-property) group has mixed keep/dismiss golds among
scored (non-unresolved) rows. The only remaining same-stem SOUND/UNSOUND coexistences are the two
documented non-merges above (genuinely different checks). Untouched: all 70 `_c` rows, the 4 drift-kills
(rows 21/22/80 Closure-38, row200 Lang-60), and the 18 already-unresolved rows (Lang-63, Math-104).
