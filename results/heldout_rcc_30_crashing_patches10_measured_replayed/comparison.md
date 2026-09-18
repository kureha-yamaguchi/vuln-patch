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

**Remeasured coverage:** Original acceptance outcomes; original coverage where available, saved rejected candidates replayed only where coverage is missing. Same time/run limits and original seed; copy of final archived corpus, not the original per-attempt corpus. Live probes saved before timeout kill.
133 rejected candidates selected for replay. Candidate metrics combine original and replay executions; acceptance outcomes remain original.

Measured existing artifacts from `/datadrive/vuln-patch/results/heldout_rcc_30`.
Excluded 41 failed or unfinished patch groups; details are in manifest.json.

Crashing — **Subset**: 10/51 certified patches; 8/10 frozen bugs.
Selection: first complete patch groups in source manifest order. Subset intervals describe only the selected bugs.

## Crashing

8 bugs; 10 patch/repetition legs per arm.

### Outcomes and root-cause coverage

| Arm | Compiled / tries | Accepted / tries | Mean candidate RCC | Mean RCC, compiled only | Accepted-set RCC |
|---|---|---|---|---|---|
| H_N | 0.9042 [0.8542, 0.9542] | 0.8021 [0.5646, 0.9500] | 0.8250 [0.7083, 0.9333] | 0.9110 [0.7914, 1.0000] | 0.8750 [0.6250, 1.0000] |
| H_N_C | 0.8396 [0.6958, 0.9625] | 0.7542 [0.5792, 0.9083] | 0.7583 [0.5958, 0.9042] | 0.9089 [0.7767, 0.9958] | 1.0000 [1.0000, 1.0000] |
| H_R | 0.9000 [0.8521, 0.9479] | 0.7375 [0.5083, 0.8958] | 0.8167 [0.7312, 0.9042] | 0.9102 [0.8154, 0.9850] | 0.8750 [0.6250, 1.0000] |

### Fuzzer-reachable set and precision

| Arm | Mean candidate \|F(H)\| | Accepted-set \|F(H)\| | Mean candidate RCP | Accepted-set RCP |
|---|---|---|---|---|
| H_N | 19.7063 [4.9935, 41.6880] | 22.3125 [6.8750, 44.1250] | 0.1921 [0.0884, 0.3080] | 0.1424 [0.0443, 0.2627] |
| H_N_C | 10.9675 [6.7667, 15.8867] | 21.0625 [13.2500, 29.6875] | 0.1577 [0.0743, 0.2592] | 0.0946 [0.0398, 0.1692] |
| H_R | 19.7467 [5.4606, 40.9328] | 28.5000 [10.3750, 52.8750] | 0.1845 [0.0864, 0.2939] | 0.0768 [0.0334, 0.1255] |

Accepted-set RCP is undefined for an arm that accepted nothing; bugs with a defined value: H_R 7/8.

### Patch-derived set

| Arm | Mean candidate PSC | Accepted-set PSC | RCR | Mean \|R0\| | Mean \|P\| |
|---|---|---|---|---|---|
| H_N | 0.5126 [0.3624, 0.6843] | 0.5000 [0.3208, 0.6896] | 1.0000 [1.0000, 1.0000] | 1.1250 [1.0000, 1.3750] | 5.0000 [3.6250, 6.6250] |
| H_N_C | 0.6068 [0.4479, 0.7783] | 0.7271 [0.5646, 0.8875] | 1.0000 [1.0000, 1.0000] | 1.1250 [1.0000, 1.3750] | 5.0000 [3.6250, 6.6250] |
| H_R | 0.5272 [0.3946, 0.6726] | 0.6687 [0.4062, 0.8875] | 1.0000 [1.0000, 1.0000] | 1.1250 [1.0000, 1.3750] | 5.0000 [3.6250, 6.6250] |

RCR, |R0| and |P| do not depend on the harnesses, so the three arms report the same numbers whenever every leg was scored.

Per-bug set sizes (H_R legs; the other arms read the same files):

| Bug | \|R0\| | \|P\| | RCR |
|---|---:|---:|---:|
| Chart-5 | 1.0 | 10.0 | 1.0000 |
| Lang-20 | 2.0 | 5.0 | 1.0000 |
| Lang-43 | 1.0 | 6.0 | 1.0000 |
| Lang-45 | 1.0 | 3.0 | 1.0000 |
| Lang-59 | 1.0 | 5.0 | 1.0000 |
| Math-58 | 1.0 | 5.0 | 1.0000 |
| Math-70 | 1.0 | 2.0 | 1.0000 |
| Math-85 | 1.0 | 4.0 | 1.0000 |

Raw counts (pooled counts; table rates above are bug-weighted):

- HN: 219/300 accepted; 268/300 compiled; 219 compiled and crashed before later acceptance checks.
- HN_C: 206/300 accepted; 242/300 compiled; 206 compiled and crashed before later acceptance checks.
- HR: 199/300 accepted; 269/300 compiled; 199 compiled and crashed before later acceptance checks.

