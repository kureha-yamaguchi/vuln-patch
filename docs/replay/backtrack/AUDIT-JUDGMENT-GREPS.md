# Audit step 1 — the six free judgment greps. 120 legs, four archived 30-leg runs.

`final30A`, `final30B`, `pairA8`, `pairB8`. No tokens spent.

## Result: one JOINS the delete list, one is SAVED from it, four are unchanged

| # | item | measured | verdict |
|---|---|---|---|
| 4 | family-novelty gate | 458 evaluations, **0 rejections** | **DELETE** |
| 5 | forced extra synthesis round | rounds 2–3 produced **732 screened, 69 fired** | **KEEP** |
| 6 | H3 setup-fidelity gate | **5 rejections** | alive — needs the per-case read |
| 1a | A4 STATE COUPLING block | delivered 17/120 legs, used in **1** | near-inert, not zero |
| 1b | A6 class-skeleton block | 120/120, **structural** | not the same question |
| 2 | A5 preconditions (Lang-27) | **0 Lang-27 legs** in these runs | test needs another corpus |

## 4 — family-novelty gate: DELETE, on a clean zero

```
   458  novelty-gate PASS
     0  novelty-gate REJECT
   legs with >=1 rejection: 0 of 120
```

It ran 458 times and never once acted. The audit's criterion was "zero
contribution in 120 legs → delete", and this is that, unambiguously. It is a
post-hoc filter invisible to the generator, so zero rejections cannot be
deterrence — it is simply inert while costing family extraction on every
attempt.

## 5 — the forced extra round: KEEP. The hypothesis is refuted.

```
   relations screened, by round : {1: 630, 2: 619, 3: 113}
   relations that FIRED on the patched build : {1: 69, 2: 59, 3: 10}
```

The audit proposed dropping `min_extra_rounds` to 0 **if round 2+ never
contributes**. It contributes almost as much as round 1 — 619 screened and 59
firing against 630 and 69. Every leg runs at least 2 rounds (96 legs ran 2, 24
ran 3), and the second round is not padding.

**This is the grep earning its place: it saved a mechanism from deletion rather
than confirming one.** Had the delete batch run first on the assumption, it
would have removed roughly half the relation supply.

## 6 — H3 setup-fidelity: alive, 5 rejections

```
   4x  Closure-62   REJECTED (H3: lifted check observed a different wrong
                     value than the real test — setup divergence)
   1x  Math-68
```

Not inert, so it does not join the delete list on a count. Whether those five
were *correct* rejections is a separate, smaller read — and worth noting that
four sit on Closure-62, a named residual that was still accused in both new
rolls, so H3's rejections did not prevent that accusation.

## 1a — A4 STATE COUPLING: near-inert, but NOT zero

```
   legs where the block was DELIVERED                        : 17 of 120
   legs where a named sibling appears in a generated harness :  1
```

The criterion was "zero uses across 120 legs → delete". This is **1**, not 0, so
the criterion is not met and I am not deleting it on a near-miss — the same
discipline applied to 8.2's 16%-versus-20%. The detection is also heuristic (a
sibling name appearing in harness source could be coincidence), which argues for
caution in the same direction.

**Recommendation:** re-test on the next 30-leg run rather than delete now. It
costs tokens in 17 of 120 legs, which bounds the waste.

## 1b — A6 class-skeleton: a different question entirely

Delivered in 120 of 120 legs, and it is **structural** — it defines the package,
class name and entrypoint every generated harness must fill in. "Never
validated" is true and beside the point: deleting it does not remove an
unproven hint, it removes the output contract. Reclassify as infrastructure, not
as an unvalidated block.

## 2 — A5 documented-preconditions: not testable here

**0 Lang-27 legs** appear in these four runs (`pool30` does not include it).
There are 44 Lang-27 legs elsewhere in the archive, so the test is runnable —
just not from this corpus, and not for free in the sense the audit assumed.

## Consequence for the delete batch

The delete list gains **one** item (the family-novelty gate) and the audit's
count moves from 10 to 11. Nothing else moves, and one candidate for a
cost-saving change (min_extra_rounds → 0) is now refuted by measurement.
