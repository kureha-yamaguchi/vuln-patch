# Heldout harness comparison

Method-level RCC against developer-changed methods (R0), on the buggy build.
Means weight bugs equally, then patches/repetitions equally within each bug.
95% percentile CIs resample paired bugs; they do not treat adaptive candidates as independent.
Noncompiled candidates and empty accepted sets score zero when R0 is defined.
Missing coverage or failed region gates make the report incomplete.

HN = level B (no neighbourhood); HN_C = level C (function-only; no patch, failing tests, or neighbourhood); HR = full context.
Semantic level C has no lifted-test oracle; its acceptance/crash rate measures escaping findings without that oracle.

Semantic — **Subset**: 10/69 certified patches; 10/27 frozen bugs.
Selection: first patches in certified queue order. Subset intervals describe only the selected bugs.

## Semantic

10 bugs; 10 patch/repetition legs per arm.

| Arm | Accepted / tries | Compiled / tries | Compiled + crashed / tries | Mean candidate RCC | Accepted-set RCC |
|---|---:|---:|---:|---:|---:|
| HN | 0.7700 [0.6600, 0.8733] | 0.7867 [0.6767, 0.8867] | 0.7700 [0.6600, 0.8733] | 0.7667 [0.6567, 0.8733] | 1.0000 [1.0000, 1.0000] |
| HN_C | 0.7233 [0.6067, 0.8300] | 0.8200 [0.7333, 0.8967] | 0.7233 [0.6067, 0.8300] | 0.6517 [0.4633, 0.8100] | 1.0000 [1.0000, 1.0000] |
| HR | 0.7233 [0.6067, 0.8400] | 0.8067 [0.6733, 0.9233] | 0.7233 [0.6067, 0.8400] | 0.7950 [0.6617, 0.9133] | 1.0000 [1.0000, 1.0000] |
| HR-HN | -0.0467 [-0.0833, -0.0133] | 0.0200 [-0.0233, 0.0633] | -0.0467 [-0.0833, -0.0133] | 0.0283 [-0.0167, 0.0733] | 0.0000 [0.0000, 0.0000] |
| HR-HN_C | -0.0000 [-0.1667, 0.1800] | -0.0133 [-0.1867, 0.1567] | -0.0000 [-0.1667, 0.1800] | 0.1433 [-0.0567, 0.3500] | 0.0000 [0.0000, 0.0000] |

Raw counts (pooled counts; table rates above are bug-weighted):

- HN: 231/300 accepted; 236/300 compiled; 231 compiled and crashed before later acceptance checks.
- HN_C: 217/300 accepted; 246/300 compiled; 217 compiled and crashed before later acceptance checks.
- HR: 217/300 accepted; 242/300 compiled; 217 compiled and crashed before later acceptance checks.

