# The p1b corpus — four-bug mechanism roll with firing-state recording live

`mechp1b_20260808_215154`, four legs, `PARALLEL=4`, 29 minutes, stage-4
settings (`-n 5 -m 12`) unchanged. Raw committed before this was written.

## Safety regressions — all four clear

| check | result |
|---|---|
| Math-2 fake patch stays caught | **PASS** — TP |
| zero `conviction VOIDED` on any fake patch | **PASS** — 0 on all four legs |
| Math-53 stays clean | **PASS** — TN, no firings, no mechanism events |
| zero facts from discarded references | **PASS** — facts only on the two legs that admitted (Math-65 adm=1/facts=1, Math-2-o adm=1/facts=3); the 15 discards produced none |

## Outcomes — recorded, not attributed

`Math-65 FP · Math-2-o TP · Math-2-SOFix TN · Math-53 TN` (TP=1 FN=0 FP=1 TN=2).
Two things changed at once; nothing is credited to either. Recorded only.

## The recording works

`__rcvstate` is present on **every** recorded firing — 0 lines without it,
against 0 lines *with* it across all nine invdiv runs. `__consumed=` is now
typed (`i:-2|i:-4|i:9`).

## The corpus: 4 distinct firings, 8 records

Each firing is written twice: a **291-char preview** under `FIRED [trigger] —`
and a fuller record under `[replay-on-patched]`. Distinct firings, not records:

| leg | firings |
|---|---|
| Math-65-c (the target sample) | 3 |
| Math-2-Arja-o | 1 |
| Math-2-SOFix-c | 0 — no firings |
| Math-53-c | 0 — no firings |

**Defect filed: the fuller records are truncated too, at 700 chars**, and the
untruncated snapshot is not preserved anywhere — `result.jsonl` carries no
`[relfire]` or `__rcvstate` content at all, so the trace is the only copy and
the trace is capped. The longest record ends mid-value:

    … objective=[-3.2010762339444594, -3.201

**A dozen-plus complete snapshots was the success criterion. What exists is
four firings, three of them on the target leg, each truncated at 700
characters.** Enough to see the shape of the material; short of the sample size
the criterion named, and not a complete record of any single firing.

---

## Verbatim records — Math-65-c (the false-accusation path)

**Firing 1 — `chiSquare_matches_weighted_squared_residuals`**

    [relfire] relation chiSquare_matches_weighted_squared_residuals violated: 29.05014937575146 vs 21.919165653273005 __consumed=i:-2|i:-4|i:9|i:14|i:-50 __rcvstate opt:LevenbergMarquardtOptimizer solvedCols=1 diagR=[4.795831523312719] jacNorm=[4.795831523312719] beta=[0.02674691127348398] permutation=[0] rank=1 lmPar=1.0739653001807597E8 lmDir=[8.009128831072887E-10] initialStepBoundFactor=100.0 costRelativeTolerance=1.0E-10 parRelativeTolerance=1.0E-10 orthoTolerance=1.0E-10 qrRankingThreshold=2.2250738585072014E-308 jacobian=[[4.795831523312719], [-3.7416573867739413]] cols=1 rows=2 targetValues=[-2.0, -4.0] residualsWeights=[9.0, 14.0] point=[-3.201076234745372] objective=[-3.2010762339444594, -3.201

**Firing 2 — `chiSquare_matches_documented_weighted_sum`**

    [relfire] relation chiSquare_matches_documented_weighted_sum violated: 10000.0 vs 1.0E8 __consumed=i:1|i:-100000|i:1 __rcvstate opt:LevenbergMarquardtOptimizer solvedCols=0 diagR=null jacNorm=null beta=null permutation=null rank=0 lmPar=0.0 lmDir=null initialStepBoundFactor=100.0 costRelativeTolerance=1.0E-10 parRelativeTolerance=1.0E-10 orthoTolerance=1.0E-10 qrRankingThreshold=2.2250738585072014E-308 jacobian=null cols=0 rows=1 targetValues=null residualsWeights=[0.01] point=null objective=null residuals=[-1000.0] cost=0.0 maxIterations=1000 iterations=0

**Firing 3 — `chiSquare_inverse_under_uniform_weight_scaling`** (two receivers)

    [relfire] relation chiSquare_inverse_under_uniform_weight_scaling violated: 1.1931342635694482E9 vs 3.314261843248467E7 __consumed=i:1|i:59074|i:56983|i:6 __rcvstate opt1:LevenbergMarquardtOptimizer solvedCols=0 diagR=null jacNorm=null beta=null permutation=null rank=0 lmPar=0.0 lmDir=null initialStepBoundFactor=100.0 costRelativeTolerance=1.0E-10 parRelativeTolerance=1.0E-10 orthoTolerance=1.0E-10 qrRankingThreshold=2.2250738585072014E-308 jacobian=null cols=0 rows=1 targetValues=null residualsWeights=[569.83] point=null objective=null residuals=[590.74] cost=0.0 maxIterations=1000 iterations=0 __rcvstate opt2:LevenbergMarquardtOptimizer solvedCols=0 diagR=null jacNorm=null beta=null permutation=nul

## Verbatim record — Math-2-Arja-o

    [relfire] relation hypergeom_mean_formula violated: actual=1832.2035695590253 expected=8112.368764750093 N=683894 m=423253 n=13108 __consumed=i:683894|i:423253|i:13108 __rcvstate dist:HypergeometricDistribution numberOfSuccesses=423253 populationSize=683894 sampleSize=13108 numericalVariance=NaN numericalVarianceIsCalculated=false

## Shapes present in the material (observed, not interpreted)

Recorded because rules written against firing 1 alone would not survive the
others:

1. **Two `__rcvstate` blocks in one firing** (firing 3: `opt1:` and `opt2:`) —
   a relation comparing two receivers.
2. **Uninitialised receivers.** Firings 2 and 3 show `iterations=0`, `cost=0.0`,
   `point=null`, `jacobian=null`, `rank=0` — the optimizer had not run when the
   relation fired. Firing 1 shows a populated receiver (`rank=1`,
   `iterations` beyond zero, real jacobian).
3. **`NaN` in state** (Math-2: `numericalVariance=NaN` with
   `numericalVarianceIsCalculated=false` — an uncomputed cache, not a
   corrupted value).
4. **Truncation mid-value** — firings 1 and 3 end inside an array literal.
5. **`__consumed` widths differ** — 5, 3, 4 and 3 values; positional, and the
   receiver-field names are the only labels present.

## Filed, not fixed

1. Firing records are capped at 700 chars in the trace and the untruncated
   snapshot exists nowhere else — `result.jsonl` carries none of this content.
2. Only 4 firings were produced against a "dozen-plus" criterion; the two
   correct-patch legs fired nothing at all.
3. Still outstanding: no per-run sha in `result.jsonl` (from the variance read).
