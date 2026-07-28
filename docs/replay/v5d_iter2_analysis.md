# 5D subset replay — iteration 2 (2026-07-28)

Run `v5d_iter2_1340` · 143 cases · repeats=1 votes=1 · gpt-5.4. Raw: `v5d_subset_iter2_20260728.md`.
Scored with the committed `score_replay.py`. Changes since iteration 1: marker veto (2e71197),
5B inadmissibility completion + rate-based 5C (fdbc2f1), fd_prior fidelity (808ead5).

## Result: DID NOT PASS, and WORSE than iteration 1. Iteration 2 of 3.

| | iter 1 | iter 2 |
|---|---|---|
| over-kill (gold SOUND dropped) | 11 | **12** |
| leak (gold UNSOUND kept) | 21 | **23** |
| unresolved-ladder (unscored) | — | 0 |

## What each change actually did

**Marker veto — WORKED.** Rows 89, 94, 99, 138 left the over-kill set; they were dropped in
iteration 1 by the bare-substring `on both builds` match and are now kept. Confirms the
production catch-killer is fixed. (Rows 32/33 still drop via a genuine textual identical fact.)

**Rate-based 5C — NET-NEGATIVE, revert candidate.** It introduced 5 new over-kills, 4 of which
are `fires-on-both-rate` terminal: rows 66, 103, 133 (Lang-50 default-locale family, buggy rate
~0.58) and row 122 (Math-73 same-sign-throw, buggy rate 1.0). These are the exact rows flagged
in advance as newly routed through family-duty; family-duty did not save them.

And it gained nothing on the leak side. Measured over the fixture: **of the 16 leak rows, only 3
carry a `[fire-rate fact]` at all and only 1 is rate-terminal.** The ~15-leaks-in-scope estimate
came from rates in the INVENTORY table, but those rates were never DELIVERED into the
`concrete_evidence` the judge reads. Across the whole subset only 58/143 rows carry a fire-rate
fact.

So the extension traded ~4 genuine catches for ~0 leaks.

**5B completion — no visible effect on its targets.** Closure-38 rows 21 and 80 still drop.
Either the drift-kill signature is not reconstructing for them in replay (the profile is rebuilt
from evidence text) or the re-asked dismissal is now landing a citation the detector accepts.
Unresolved; needs a per-row trace read, not another rule.

## Diagnosis: this is a design error, not under-tuning

Two of the three changes failed for the SAME structural reason, and it is the reason cycle 3
already hit once (the "one-door" gap): **a rule was keyed on a fact that is not present in the
evidence the judge actually receives.** The rate-based terminal reasons about fire rates that
live in the inventory rather than in the delivered evidence; where they are delivered, they are
delivered on the wrong rows.

Per the standing stop-loss ("more than ~3 iterations means the rule design is wrong, not
under-tuned"), the correct move is NOT iteration 3 with adjusted thresholds.

## Recommendation

1. **Revert the rate-based 5C extension.** Evidenced, not tuning: it costs 4 confirmed catches
   and buys ~0 leaks on the measured fixture. Restores over-kill to ~8.
2. **Keep the marker veto** (fixes a live production bug, independently verified).
3. **Keep 5B's completion** (correct per spec, no measured harm; its targets need diagnosis).
4. **The leak class is a DELIVERY problem, not a judging problem.** Before any rule keyed on
   fire rates, the rates must reach the firings' evidence — the same fix shape as one-door.
   That is a separate, evidenced piece of work, and it should be measured on the fixture
   (offline, free) BEFORE any further judge-rule change.

---

## ADDENDUM (2026-07-28): why 5B had "no visible effect" — a third instance of the same bug

Diagnosed offline, no tokens spent. The iteration-2 run recorded **zero** `citation-void` and
**zero** `5B-INADMISSIBLE` events across all 143 cases, so 5B never fired at all. Chain:

1. **Not a threading gap.** `evidence_profile` IS passed into `adjudicate` (verifier_replay.py
   line 216), and the drift-kill signature reconstructs on 10 subset rows — including BOTH 5B
   targets (rows 21 and 80, `sig_full=True`, no missing signals). So unlike the fd_prior case,
   the rule was properly armed.
2. **The citation detector accepted negations as citations.** The judge's actual iteration-2
   dismissals (from `results.jsonl`):
   - row 21: *"…asserts an **undocumented** exact-printing contract … the **trusted test only
     requires** the space for negative zero, not for positive zero."*
   - row 80: *"…is **not contradicted by any** shown contract or trusted test…"*
   Bare-substring matching counted `'document'` (inside "**un**documented"), `'contract'`, and
   `'trusted'` as citations — in spans whose plain meaning is that NO citation exists. With
   `has_citation` true, `verdict_needs_citation` returned False and the re-ask never happened.

**This is the same failure shape as the 5C terminal-marker bug fixed hours earlier**: a bare
substring matcher reading text that means the opposite. Third instance of the pattern in this
cycle (one-door delivery, terminal markers, citation markers).

**Fixed:** `_NEGATED_CITATION_RE` strips negated-citation spans (clause-bounded, so a genuine
citation in a later clause still registers) before citation matching. Regression tests in
`tests/test_terminal_marker_veto.py`. Verified: row 21 now voids; the three guard shapes
(Lang-50 `!=` bug, Math-74 tolerance floor, javadoc-cited) all still stand; without the full
signature nothing is voided. 243 tests green.

**Left UNFIXED deliberately — a judgment call, not a bug.** Row 80 still does not void because
`'observed'` survives, in *"matches the **observed** patched output pattern"* — the judge saying
its hypothetical is consistent with what was seen, not citing an authority. Whether `'observed'`
belongs in `_CITATION_MARKERS` at all is a marker-quality decision with blast radius on other
rows; I stopped rather than take a third tuning pass at a keyword matcher.

**Standing conclusion unchanged:** the recurring defect is *rules keyed on text/facts that are
absent, undelivered, or read in the opposite sense*. Keyword-matching the judge's prose to
decide whether it cited something is inherently fragile — worth a design discussion before more
patches. The iteration-2 recommendation (revert rate-based 5C; keep the veto; treat the leak
class as a delivery problem) is unaffected by this addendum and still awaits a decision.
