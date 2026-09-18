# Heldout harness comparison

Method-level set metrics against the developer-changed methods (R0), on the buggy build.
RCC = |R0 n F| / |R0|; RCP = |R0 n F| / |F|; PSC = |P n F| / |P|; RCR = |R0 n P| / |R0|; |F(H)| is the count of project methods the set ran.
Means weight bugs equally, then patches/repetitions equally within each bug.
95% percentile CIs resample paired bugs; they do not treat adaptive candidates as independent.
Noncompiled candidates and empty accepted sets score zero for RCC and PSC.
RCP divides by |F|, so a candidate that ran nothing leaves it undefined, and every per-candidate mean except the legacy RCC column is over compiled candidates only.
P is the analysis step's extracted set. It is byte-identical across the three arms, so RCR describes the patch, not the arm; the arms differ in how much of P the prompt showed.
Missing coverage or failed region gates make the report incomplete.

HN = level B (no neighbourhood); HN_C = level C (function-only; no patch, failing tests, or neighbourhood); HR = full context.
Semantic level C has no lifted-test oracle; its acceptance/crash rate measures escaping findings without that oracle.

Semantic — **Subset**: 10/69 certified patches; 10/27 frozen bugs.
Selection: first patches in certified queue order. Subset intervals describe only the selected bugs.

## Semantic

10 bugs; 10 patch/repetition legs per arm.

### Outcomes and root-cause coverage

| Arm | Compiled / tries | Accepted / tries | Mean candidate RCC | Mean RCC, compiled only | Accepted-set RCC |
|---|---|---|---|---|---|
| H_N | 0.7867 [0.6767, 0.8867] | 0.7700 [0.6600, 0.8733] | 0.7667 [0.6567, 0.8733] | 0.9760 [0.9280, 1.0000] | 1.0000 [1.0000, 1.0000] |
| H_N_C | 0.8200 [0.7333, 0.8967] | 0.7233 [0.6067, 0.8300] | 0.6517 [0.4633, 0.8100] | 0.7873 [0.5718, 0.9495] | 1.0000 [1.0000, 1.0000] |
| H_R | 0.8067 [0.6733, 0.9233] | 0.7233 [0.6067, 0.8400] | 0.7950 [0.6617, 0.9133] | 0.9860 [0.9580, 1.0000] | 1.0000 [1.0000, 1.0000] |

### Fuzzer-reachable set and precision

| Arm | Mean candidate \|F(H)\| | Accepted-set \|F(H)\| | Mean candidate RCP | Accepted-set RCP |
|---|---|---|---|---|
| H_N | 34.1246 [14.3620, 56.8614] | 41.1000 [20.7975, 63.8025] | 0.1864 [0.0384, 0.3871] | 0.1758 [0.0259, 0.3782] |
| H_N_C | 36.7824 [12.0278, 67.4038] | 107.4000 [39.7000, 189.3000] | 0.1969 [0.0485, 0.4056] | 0.1384 [0.0165, 0.3411] |
| H_R | 38.0279 [17.0164, 61.6640] | 80.0000 [31.7975, 135.9000] | 0.1811 [0.0382, 0.3837] | 0.1411 [0.0199, 0.3430] |

### Patch-derived set

| Arm | Mean candidate PSC | Accepted-set PSC | RCR | Mean \|R0\| | Mean \|P\| |
|---|---|---|---|---|---|
| H_N | 0.6958 [0.5131, 0.8679] | 0.7217 [0.5471, 0.8821] | 1.0000 [1.0000, 1.0000] | 1.1000 [1.0000, 1.3000] | 5.8000 [3.8000, 8.0000] |
| H_N_C | 0.5074 [0.3310, 0.6867] | 0.7573 [0.5857, 0.9099] | 1.0000 [1.0000, 1.0000] | 1.1000 [1.0000, 1.3000] | 5.8000 [3.8000, 8.0000] |
| H_R | 0.7038 [0.5277, 0.8691] | 0.7883 [0.6075, 0.9381] | 1.0000 [1.0000, 1.0000] | 1.1000 [1.0000, 1.3000] | 5.8000 [3.8000, 8.0000] |

RCR, |R0| and |P| do not depend on the harnesses, so the three arms report the same numbers whenever every leg was scored.

Per-bug set sizes (H_R legs; the other arms read the same files):

| Bug | \|R0\| | \|P\| | RCR |
|---|---:|---:|---:|
| Chart-1 | 1.0 | 13.0 | 1.0000 |
| Chart-8 | 1.0 | 1.0 | 1.0000 |
| Lang-24 | 1.0 | 2.0 | 1.0000 |
| Math-25 | 1.0 | 6.0 | 1.0000 |
| Math-33 | 1.0 | 8.0 | 1.0000 |
| Math-50 | 1.0 | 7.0 | 1.0000 |
| Math-75 | 1.0 | 7.0 | 1.0000 |
| Math-80 | 1.0 | 2.0 | 1.0000 |
| Math-99 | 2.0 | 6.0 | 1.0000 |
| Time-15 | 1.0 | 6.0 | 1.0000 |

Raw counts (pooled counts; table rates above are bug-weighted):

- HN: 231/300 accepted; 236/300 compiled; 231 compiled and crashed before later acceptance checks.
- HN_C: 217/300 accepted; 246/300 compiled; 217 compiled and crashed before later acceptance checks.
- HR: 217/300 accepted; 242/300 compiled; 217 compiled and crashed before later acceptance checks.

