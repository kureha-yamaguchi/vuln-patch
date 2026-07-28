# Cycle 5 — close (2026-07-28)

Closing measurement: `v5d_close_1456`, 30 two-sided rows, ~0.35M tokens (not the 1.8M full
subset). Raw: `v5d_close30_20260728.md`. Scored with the committed `score_replay.py`.

## Closing numbers

```
scored rows: 30  (must-keep 18 / must-dismiss 12)
over-kill (must-keep dropped): 3   -> rows 136, 197, 200, all one family (Lang-60 capacity)
must-dismiss held:             7/12
```

**Over-kill criterion (≤8) is met: 3.** Read with the caveat below, and note the sample is the
targeted closing set, not the full 143.

**The rate-revert restored everything it was supposed to.** All seven rows the rate-based 5C had
wrongly dropped in iteration 2 (28, 32, 33, 66, 103, 122, 133) are kept again. That decision is
now evidenced end-to-end: it cost 4+ confirmed catches and bought ~0 leaks, and reverting it
recovered them.

**The 5 "regressions" on the dismiss side are judge noise, not the new mechanism.** Rows 60, 144,
159, 175, 143 flipped dismiss→keep versus iteration 2 — but *none of them carries the drift-kill
signature*, and the run logged **zero** `5B-INADMISSIBLE` events, so no new code path touched
them. The held rows are signature-False too, i.e. identical treatment. This is a fresh, direct
measurement of the campaign's central problem: **5 of 10 untouched rows flipped verdict between
two single draws.** Every number in this cycle sits on top of that variance.

## The decisive test, and what it revealed

Row 200 (Lang-60 capacity) was the mechanism's true must-flip test: the "lazy compaction" story
was supposed to have nothing to cite. It cited this, and the quote is real:

> CITATION: `"     * Checks if the string builder contains the specified char."`

The javadoc line exists; the judge used it to argue the contract *specifies only the search
result, not that capacity is preserved*. So the dismissal stands under the rule as written.

**This is the mechanism working and finding its own next gap.** The citation is grounded, but it
is cited for what the quoted text does NOT say. The rubric already forbids exactly that move in
prose ("ABSENCE OF EVIDENCE IS NOT UNSOUNDNESS"), and the grounding check — a literal substring
test — cannot tell a quote that supports a claim from a quote cited to prove an absence. That is
the precise next lever, and it is a *narrow, structural* one, not another keyword list.

Rows 136 and 197 answered `CITATION: NONE` and still stand, because they do not carry the
signature: 5B is scoped to 12/143 rows by design. Its reach, not its logic, is the limit.

## What cycle 5 actually produced

Shipped, each independently evidenced:
- **5A** — two-sided fire-rate note + per-input denominator fix (the note had been coaching the
  judge to discard its best catches; the "2997/1000 = 300%" arithmetic bug is gone).
- **Terminal-marker veto** — a live production catch-killer: the gate fired on notes whose text
  *denies* the identical claim, including one that says the firing convicts the patch.
- **Negated-citation fix** — `'document'` was matching inside "**un**documented"; "not
  contradicted by any shown contract" counted as a citation.
- **Rate-based 5C revert** — measured net-negative, reverted with the evidence in the code.
- **Structured citation + fact tags** — 100% format compliance across two runs (20/20, then
  this one); citations are now verified by literal substring against what the judge was shown,
  and our own notes carry `[fact:…]` tags instead of being re-parsed as prose.
- **Replay fidelity** — `fd_prior` reconstructed from the original runs' recorded ladder
  decisions; unresolvable cases excluded rather than guessed.

Three of those six are the *same bug*: **a rule keyed on text read in the opposite sense**
(one-door delivery → terminal markers → citation markers). The structural replacement (quote +
tag) is the durable answer, and it is now in place.

## What cycle 5 did not do, stated plainly

- It did not move the leak class. That was never in reach: the facts those rules need are not
  delivered into the evidence the judge sees. **This is cycle 6's subject.**
- It did not flip the drift-kills. Two (rows 21/80) were reclassified **contested** — the
  mechanism produced grounded dismissals on an unpinned observable, which under precision-first
  is correct behaviour, not drift. The third (row 200) grounds its dismissal via the
  absence-argument gap above.
- Closure-38 joins Lang-63 and Math-104 in the hard column: its certified divergence lives in
  unpinned formatting, so a value-oracle catch of it inherently risks convicting correct
  printers. That is the dataset being honest, not the pipeline failing.

## Cycle 6 opens on

1. **Delivery** — get the measured facts (fire rates, per-oracle screen stats) into the evidence
   for shadowed/oracle-track firings. The entire leak class lives here.
2. **The absence-argument gap** — a grounded citation used to prove what the text does *not* say.
3. **Variance** — 5/10 untouched rows flipping between draws remains the dominant source of
   uncertainty in every measurement we take.

Downstream sequence unchanged and gated on the user's word: night20 rerun, then the paired
milestone, then fresh12 (burn-once).
