# 5D subset replay — iteration 1 (2026-07-27)

Run: `v5d_subset_2202` on hetzner · 143 hard-criteria cases · repeats=1 votes=1 · gpt-5.4.
Raw per-row results: `v5d_subset_iter1_20260727.md`. Scorer (reproducible):
`score_replay.py <summary.md> [fixture.jsonl]`.

## Result: DID NOT PASS. Iteration 1 of the 3-iteration stop-loss.

```
scored rows: 143  (SOUND 64 / UNSOUND 79)
over-kill (gold SOUND dropped): 11
  - fd_prior gate artifact (IDENT-carrying): 6
  - genuine over-kill:                       5
leak (gold UNSOUND kept): 21  [on correct legs: 20]
```

## Decomposition

**(1) fd_prior gate-fidelity artifact — 6 rows (32, 33, 89, 94, 99, 138), NOT regressions.**
The replay passes `fd_prior=None`, so for a catch carrying an identical-on-both fact the 5C
terminal gate freshly asks family-duty, gets NO, and drops it. Production does NOT do this:
run.py's J-ladder hits the trigger-input exemption and sets `fd_prior=True` (commit 4efdeb0).
The gate is therefore STRICTER THAN PRODUCTION and penalises catches the pipeline keeps.
"Conservative" was the wrong call for a measurement — it corrupts the over-kill number on
every scored patch-failed-to-fix catch, not only the unresolved Lang-63 rows. Fix: reconstruct
`fd_prior` per case from the J-ladder events recorded in the original run's trace; where no
ladder event exists, mark the case unresolved rather than guess.

**(2) Genuine over-kill — 5 rows.** Two are the Closure-38 format drift-kills (21, 80) — the
rule's target class, still failing (see 3a). Three are Lang-60 capacity rows (136, 197, 200)
whose gold was set by the de-circularisation pass; single-draw, so noise is not excluded.

**(3) Leaks — 21 (20 on correct legs).** ~15 are invented-contract keeps (chi², p-value-in-
[0,1], endpoint-root, asymptotic-formula). Neither 5B nor 5C addresses them today: they carry
no identical-on-both fact and do not match the drift-kill signature.

## Findings that survive the caveats

**3a. 5B is INCOMPLETE against its own spec, not merely weak.** The spec says an uncited
"a correct implementation could…" is inadmissible under the drift-kill signature. The code
re-asks once and then accepts whatever returns — including a second uncited hypothetical,
which is what Closure-38 produced. Completing the stated semantics (under the signature, a
re-asked dismissal that is again citation-void does not stand → keep, loudly flagged) is
finishing the spec, not tuning to the fixture. Guard against over-keeping: the two
signature-complete gold=dismiss rows (Lang-50 `!=` bug, Math-74 rounding) both have genuine
citations available and must stay dead.

**3b. The invented-contract leak class is probably IN scope via rates.** Per the inventory
most of these fire heavily on the buggy build too (chi² relations 13–19k/20k). A check that
condemns the known-broken build at high rate reports something pre-existing — the same
terminal logic as identical-on-both, measured by rate instead of byte-comparison. Extending
5C's terminal detector to the measured fires-on-both profile, family-duty escape intact
(Math-2's mean relation also fires on both and must survive via family-duty YES), would put
~15 of 21 leaks in scope mechanically. This makes the fd_prior fidelity fix doubly
load-bearing.

## Addendum (2026-07-28) — the reconstruction refutes decomposition item (1)

`scripts/reconstruct_fd_prior.py` recovered, per case, what run.py's Spec-J
ladder actually handed the 5C gate in the original run, from that run's trace.
The fix shipped (`fd_prior` is now a fixture field and `verifier_replay.py`
passes it), but the six rows above are **not** fd_prior artifacts:

* **Rows 99, 138** are relation-replay firings. run.py's relation judge site
  passes `fd_prior=None` itself (that track has no ladder), so the replay's None
  was already exactly what production does. Row 99's own evidence records
  "NOT direction-confirmed", so the gate is not skipped there either.
* **Rows 32, 33, 89, 94** are semantic firings whose recorded buggy-replay note
  says the values were **not** identical — rows 32/89 "observed values were not
  compared, so no identical-value claim is made", rows 33/94 "with DIFFERENT
  observed values ... the partial-fix pattern; this firing remains evidence
  against the patch". `_value_verdict` was therefore never `"identical"`, the
  ladder never armed, and production's `fd_prior` for those firings was None.

What actually drops those four is a different bug, present in production too:
`carries_terminal_identical_fact` matches the bare substring `on both builds`,
so it fires the 5C terminal gate on notes that explicitly deny an identical-value
claim — including the DIFFERENT-values note that says the firing still convicts
the patch. The terminal detector needs the value verdict as a fact, not a
substring. That, not fd_prior, is the over-kill lead for iteration 2.

Secondary gap of the same family, left unfixed and unmeasured: the replay also
hard-codes `is_direction_confirmed=False`, while run.py skips 5C entirely for a
direction-confirmed relation (3 fixture cases, 1 of them IDENT-carrying).

## Sequence (agreed)
fidelity fix (reconstructed fd_prior) → targeted re-rolls of the ~32 disputed rows only
(majority-of-3 per disputed row, ~⅓ the cost of a blanket repeats=2) → one change-set with
the 5B semantics completion + rate-based 5C extension → subset re-run = iteration 2.
Nothing with the new judge runs on the VM pipeline until the gate passes.

---

## CORRECTION (2026-07-28): the fd_prior artifact hypothesis was WRONG

The fidelity investigation (commit 808ead5) reconstructed `fd_prior` from the original runs and
found **zero of the 6 rows were an fd_prior artifact**. Section (1) above is superseded:
- rows 99, 138 are relation-replay firings, where production itself passes `fd_prior=None`;
- rows 32, 89 carry "observed values were not compared, so no identical-value claim is made";
- rows 33, 94 carry "the SAME check fires on BOTH builds but with DIFFERENT observed values …
  the partial-fix pattern; this firing remains evidence against the patch".
So the J-ladder never armed for any of them — `_value_verdict` was never `"identical"`.

**The real cause — a live PRODUCTION bug, not a replay bug.** `_TERMINAL_IDENTICAL_MARKERS`
contained the bare substring `'on both builds'` (and `'same check fires on both'`), which
matches both note families above — including the partial-fix note whose own text says the
firing **convicts** the patch. The 5C terminal gate therefore voided keeps on catches whose
evidence was evidence *against* the patch. Fixed by a deny-first veto
(`_TERMINAL_IDENTICAL_VETO`) plus tightened affirmative markers; regression test
`tests/test_terminal_marker_veto.py`. The measured fires-on-both RATE path is unaffected by
the veto (a denial of the textual claim must not suppress an independent measurement).

Process note: the fd_prior hypothesis was stated in chat with confidence before the
reconstruction existed, and the committed decomposition inherited it. Both the earlier verbal
"3 artifact rows" (scorer said 6) and this hypothesis were corrected only because the artifact
and the reconstruction were committed and re-checked. Interpretation ahead of evidence, twice.

**Iteration-2 expectation, restated honestly:** the over-kill fix is the marker veto (not
fd_prior); genuine over-kill to watch is Closure-38 rows 21/80 (targeted by the 5B completion)
and the Lang-60 rows 136/197/200 (single-draw, re-roll to separate noise). Leak side is
targeted by the rate-based 5C extension.
