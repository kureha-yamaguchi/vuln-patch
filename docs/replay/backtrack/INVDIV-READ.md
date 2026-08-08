# Invention-diversity + p1b recording — the read

`invdiv_20260808_203424`, `git=57b6dea`, 9 runs (3 unstable legs × 3 draws),
`-n 8 -m 20`, `PARALLEL=6`, 52 minutes. Raw committed before this was written.

## Fail-closed checks — both clear

**Zero `conviction VOIDED` on every leg**, overfit and correct alike. No hard
stop.

**Zero facts from discarded references.** Facts appear only where an admission
happened: Math-65 draws 1 and 2 (1 admission each, `getValueRef`); the other
seven legs have 0 admissions and 0 facts against 14 discards between them.

## Outcomes — recorded, NON-comparable, nothing banked

| leg | draws |
|---|---|
| Math-65-c | FP / FP / TN |
| Chart-19-o | TP / FN / TP |
| Lang-63-o | TP / FN / FN |

Per the pre-registration these are not compared to the baseline and nothing is
attributed to `-n 8` or to the recording change. Recorded only.

## (1) The invent-more read — invention-level, upstream, attributable

Harness count moved as configured: `built=5` → `built=8` on all nine runs.

| leg | metric | baseline (−n 5) | invdiv (−n 8) |
|---|---|---|---|
| Math-65 | distinct check names / 3 draws | 68 | 53 |
| | names in ≥2 draws | 17 | 9 |
| | tokens | 230k–268k | 145k–337k |
| Chart-19 | distinct check names | 38 | 46 |
| | names in ≥2 draws | 2 | 5 |
| | tokens | 111k–279k | 283k–322k |
| Lang-63 | distinct check names | 11 | 11 |
| | names in ≥2 draws | 3 | 4 |
| | tokens | 71k–133k | 119k–207k |

### Killer-check families

| leg | family | baseline | invdiv |
|---|---|---|---|
| Chart-19 | range-axis | present in **2/3** draws (1–2 checks) | present in **3/3** draws (**6–7 checks**) |
| Lang-63 | period/duration/month | 3/3 (1–4 checks) | 2/3 (0–2 checks) |
| Math-65 | chiSquare-formula | 3/3 (3–4) | 3/3 (3 each) |

**Chart-19's killer family did become far more reliably invented** — from
present-in-two-draws at one or two checks each, to present-in-all-three at six
or seven. That is the clean `-n 8` effect, and it is the one the run was
launched to measure.

**And within this run, that was not sufficient.** Chart-19 draw 05 carries **6
range-axis checks and still crashed 0 harnesses** on the patched build. The
family was invented, in quantity, and the patch was not caught. This is a
within-run observation — no baseline outcome is being compared — and it says
inventing more of the right *family* does not by itself convert a miss.

Lang-63 moved the other way: its family appeared in fewer draws at `-n 8` than
at `-n 5`. With three draws per arm, neither direction here is separable from
draw-to-draw noise; the Chart-19 magnitude (2→3 draws, 1–2→6–7 checks) is the
only movement large enough to read.

Math-65 shows **fewer** distinct names at `-n 8` (68 → 53) with more harnesses.
Recorded, not explained.

## (2) The p1b corpus — 15 distinct `[relfire]` lines, verbatim

**Collection defect found: `__rcvstate` was never recorded.** It occurs **0
times across all nine traces**, though the pre-registration describes firings as
carrying `[relfire] <message> __consumed=… __rcvstate …`. Every line below ends
at `__consumed=`. The corpus is therefore **half of what p1b was meant to
sample** — receiver state is absent. Not fixed here, per instructions; filed.

The trailing `**` is the trace's markdown bold marker, not line content; each
line is followed by a `- note:` line that is part of the same record. Verified
against the raw text rather than assumed.

### Math-65-c — the target sample

    [relfire] relation chiSquare_matches_documented_weighted_formula violated: expected=36.9614512422353 actual=55.10204082656628 __consumed=-12|-13|-15|9|1|1|-10|-10
    - note: fires on the failing test's OWN input literals on the patched build (deterministic, 2/2 replays); fuzzed: 15753/20000

    [relfire] relation rms_matches_documented_mean_weighted_square_formula violated: expected=7.649894219235105 actual=6.95627609037991 __consumed=-13|-15|-8|3|1|1|-10|-10

    [relfire] relation uniform_weight_scaling_preserves_point_and_scales_fit_metrics violated on point[0]: -13.333333333333332 vs -13.333328797247848 __consumed=-20|-20|-20|1|1|1|2|-10|-10

    [relfire] relation chiSquare_matches_weighted_residual_sum violated: 419.41682878521533 vs 405.7629551032988 __consumed=2|-15|6|-4|1|-10

### Chart-19-o

    [relfire] relation categoryplot_getRangeAxisIndex_null_rejected_independent_of_axis_state violated: completed normally __consumed=     ||3|5||1||1|

    [relfire] relation categoryplot-getrangeaxisindex-null-throws violated: completed normally __consumed=true|4|   

    [relfire] relation categoryplot-getRangeAxisIndex-null-always-throws violated: second plot completed normally __consumed=9YYY|YYYY|    |YYYY|4|4||0||0||0|

### Lang-63-o

    [relfire] relation positive_period_month_field_is_not_negative violated: -9 __consumed=2010|11|31|3|4

Draws 03, 05, 08 and 09 produced no `[relfire]` lines at all.

Collected, not interpreted, per the terms. Two properties are visible without
interpretation and matter for anyone designing against this corpus: the
`__consumed=` payloads are **untyped pipe-joined scalars with no field names**,
and several carry **empty positions and whitespace runs** (`     ||3|5||1||1|`,
`true|4|   `, `9YYY|YYYY|    |YYYY|…`) rather than clean values.

## Filed, not fixed

1. `__rcvstate` is absent from every recorded firing — the corpus is half-built.
2. `__consumed=` payloads are positional and unlabelled, and some contain empty
   or whitespace fields.
3. Still outstanding from the variance read: no per-run sha in `result.jsonl`.
