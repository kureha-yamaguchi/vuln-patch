# night20 rerun (night20b, 2026-07-28) — first run carrying all cycle-5 + Chart-19 fixes

20 legs, width 7 (matched to night20 for a valid pair), 4.95M tokens.
Raw: `night20b_20260728.md`. Archive: `runs-archive/runs/night20b_20260728_153138`.

## Result vs the night20 pair

| | night20 (pre-fixes) | night20b | delta |
|---|---|---|---|
| catches (of 14) | 8 | **11** | +3 |
| recall | 0.57 | **0.79** | +0.22 |
| clean correct legs (of 6) | 3 | **1** | −2 |
| precision | 0.73 | 0.69 | −0.04 |
| F1 | 0.64 | **0.73** | +0.09 |

**Best F1 measured on this set.** Recall is the largest jump of the campaign; precision paid part
of it back.

## Recall: three legs caught that had NEVER been caught in five prior rolls

Chart-19, Lang-63, Closure-92 — all three were in the "invents but never fires" class from the
four-roll tabulation. Chart-19's mechanism is attributable: its checks now draw *structural*
values from the fuzzer (`consumeInt(0,6)`, `consumeInt(1,3)`, `consumeInt(0,4)` governing element
counts and install indices), which is precisely the sparse/gapped container state the overfit
needs to be exposed — the state the item-4 replay proved was unreachable when structure came from
constants. One constant-structure check on the same leg was demoted, so the lint discriminates
rather than blanket-flags.

Lang-63 and Closure-92 are NOT attributed yet — neither was a target of the structure-from-data
fix. Their catches need a trace read before being credited to anything.

Still missed: Lang-60, Closure-38, Math-104. Closure-38's miss is consistent with the cycle-5
close — its divergence lives in unpinned formatting and its rows were reclassified contested, so
a dismissal there is defensible rather than a failure.

## Precision: 5 false accusations, dominated by the known leak class

Closure-62, Chart-26, Math-30, Math-65, Math-73-c. Math-30 and Math-65 are the chronic
invented-contract FPs. The two regressions vs night20 fired on:
- Chart-26: `bar-chart-null-info` — draw-with-null throwing StringIndexOutOfBounds (the leg's
  long-standing trusted-lift pattern);
- Math-73-c: `endpoint-min-return` / `function-value-state-consistency` — the endpoint-root
  family, which appears in the iteration-1/2 leak lists repeatedly.

Neither is obviously a structure-from-data artifact; both look like the **invented-contract leak
class**, which is exactly what cycle 6's delivery work targets (those checks' fire-rate facts are
never delivered into the evidence the judge reads). That attribution is a hypothesis, not
established — it needs the same per-firing triage discipline, and it is the first thing to check
before crediting or blaming the new generation directive.

## Caveats that bound every number above

- **Single roll.** Measured variance on this pipeline: 5 of 10 *untouched* rows flipped verdict
  between two identical replay draws. A one-roll delta of ±2 legs is inside that band.
- The two-roll rule applies: nothing here is "fixed" until a second roll agrees, with the
  mechanism visible in the trace.

## Next
1. Trace-read Chart-19 (confirm the convicting check fired via a fuzz-derived gap), Lang-63 and
   Closure-92 (unattributed), and the two precision regressions.
2. Cycle-6 delivery work — the leak class.
3. Second night20 roll for the two-roll rule, then the paired 30-leg measurement.
