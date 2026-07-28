# Iteration-3 compliance pre-check (2026-07-28) — 20 rows, ~50k tokens

Run `v5d_compliance_1446`. Raw: `v5d_compliance20_20260728.md`. Sample: the 9 signature-bearing
rows (where 5B can act) + 11 drawn at random (seed 20260728).

## The format works: 100% compliance

**20/20 verdicts emitted the `CITATION:` line** — 14 `NONE`, 6 quoted. Zero
`citation-format-noncompliant` events. The known format-compliance hazard did not materialise,
and the fail-safe path was never needed. The mechanism itself is sound: grounding is a literal
substring test against the material the judge was shown, no interpretation.

## But the pre-check also changed the expected value of the full run

Two findings, both bearing on whether iteration 3's headline criterion is reachable:

**1. 5B's reach is structurally capped at 10 of 143 rows.** The citation rule is scoped to the
full drift-kill signature (buggy-silent + deterministic-trigger + patched-firing) — by design,
to keep it from over-keeping. Measured over the subset, only **10/143 rows carry that
signature**. Of the 12 iteration-2 over-kill rows, exactly **3 are inside 5B's reach (21, 80,
200)**; the other 9 (28, 32, 33, 66, 103, 122, 133, 136, 197) cannot be affected by it at all.
So the maximum possible over-kill improvement from this mechanism is 3 rows — and 4 of those 9
should return on their own now that the rate-based 5C path is reverted.

**2. On its flagship target the judge produced a GROUNDED citation, so the dismissal legitimately
stands.** Row 21 (Closure-38, a designated drift-kill that gold says must flip to kept) answered:

> CITATION: `"assertPrint(\"x- -0\", \"x- -0.0\");"`

That is a real line from the trusted test, and it supports the judge's actual argument — the test
pins the space for NEGATIVE zero, not for positive zero. Under the new rule this is a cited, not
a hypothetical, dismissal: it stands, correctly, by the mechanism's own definition.

## Consequence for the pre-committed criterion

The iteration-3 criterion was "the drift-kills flip to kept". The pre-check shows that on at
least one drift-kill the judge can ground its dismissal in real quoted text — so the criterion
may be unreachable *not because the mechanism failed but because it is working*. That in turn
puts a question mark over the gold label for row 21: if the trusted test genuinely pins the space
only for negative zero, "SOUND" may be the wrong gold, and the earlier drift-kill classification
(derived from the inventory's reasoning-class, the same circularity fixed for the `_o` rows in
844715f) would be the thing at fault.

**This is a decision point, not a result.** Options:
- (a) Run the full 143-row subset anyway (~1.8M): measures the revert + tags + citations
  end-to-end and gives a clean final number for cycle 5, accepting that ~3 rows is the ceiling
  for the citation mechanism specifically.
- (b) Re-adjudicate row 21's gold first (offline, free — the same dev-fix replay protocol used in
  1d02859), since a wrong gold makes the criterion unachievable by construction.
- (c) End cycle 5 here: ship what is independently evidenced (5A, the two marker-class fixes, the
  rate revert, the structural citation+tag mechanism at 100% compliance) and open cycle 6 on the
  delivery problem, which is where the leak class actually lives.

Recorded without a recommendation being acted on; the spend is gated.
