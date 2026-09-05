# RCC(H_R) — crashing split, holdout side

Measurement only. One leg per bug, an overfitting patch in every case.
Harness budget `-n 3 -m 8 --fuzz_timeout 20`; measurement pass
`-runs=20000` with `--keep_going=1000`, on the buggy build.

`R-hat` is the set of methods the Defects4J developer fix changes.
`F(H)` is the set of methods the accepted harness set ran.

| bug | leg | \|R-hat\| | \|H\| | \|F(H)\| | RCC |
|---|---|---:|---:|---:|---:|
| Chart-5 | overfitting | 1 | 3 | 17 | 1.000 |
| Lang-16 | overfitting | 1 | 3 | 7 | 1.000 |
| Lang-20 | overfitting | 2 | 3 | 14 | 1.000 |
| Lang-43 | overfitting | 1 | 0 | – | – |
| Lang-45 | overfitting | 1 | 3 | 2 | 1.000 |
| Lang-58 | overfitting | 1 | 3 | 7 | 1.000 |
| Lang-59 | overfitting | 1 | 3 | 3 | 1.000 |
| Math-58 | overfitting | 1 | 3 | 82 | 1.000 |
| Math-70 | overfitting | 1 | 3 | 10 | 1.000 |
| Math-85 | overfitting | 1 | 3 | 48 | 1.000 |

**mean RCC(H_R) = 1.000 over 9 scored bugs.**

Lang-43 is not scored. Its leg accepted no harness in 8 attempts, so H is
empty and RCC is undefined. That is a pipeline outcome, not a low score.

## Read this number with three caveats

1. **The result is saturated by construction.** The pipeline accepts a
   harness only when it CRASHES the buggy build. The crash is the bug, and
   the bug lives in the method the developer fix changed. R-hat appears in
   the crash stack trace on all 9 scored bugs. So acceptance implies reach,
   and RCC(H_R) = 1 follows. The number confirms the plumbing. It does not
   yet discriminate between harness sets.

2. **The denominators are 1.** |R-hat| is 1 on 9 of 10 bugs and 2 on the
   tenth. Per bug the metric is therefore near-binary, and the mean is a
   hit rate, not a coverage fraction.

3. **One bug needed the stack-frame repair to score at all.** JaCoCo places
   a method's probe after the method's exit, so Math-70's
   `BisectionSolver.solve` reads as missed from probes alone. Without the
   repair its RCC would have been a false 0.000. See the probe limitation
   in `src/metrics/README.md`.

## What this does not measure

There is no H_N in this repo, so the comparison RCC(H_N) << RCC(H_R) is
still untested. H_N carries no crash-acceptance rule, so caveat 1 does not
apply to it, and that is where the metric should separate the two sets.
