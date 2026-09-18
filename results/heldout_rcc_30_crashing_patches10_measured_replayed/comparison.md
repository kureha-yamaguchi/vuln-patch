# Heldout harness comparison

Method-level RCC against developer-changed methods (R0), on the buggy build.
Means weight bugs equally, then patches/repetitions equally within each bug.
95% percentile CIs resample paired bugs; they do not treat adaptive candidates as independent.
Noncompiled candidates and empty accepted sets score zero when R0 is defined.
Missing coverage or failed region gates make the report incomplete.

HN = level B (no neighbourhood); HN_C = level C (function-only; no patch, failing tests, or neighbourhood); HR = full context.
Semantic level C has no lifted-test oracle; its acceptance/crash rate measures escaping findings without that oracle.

**Remeasured coverage:** Original acceptance outcomes; original coverage where available, saved rejected candidates replayed only where coverage is missing. Same time/run limits and original seed; copy of final archived corpus, not the original per-attempt corpus. Live probes saved before timeout kill.
133 rejected candidates selected for replay. Candidate RCC combines original and replay executions; acceptance outcomes remain original.

Measured existing artifacts from `/datadrive/vuln-patch/results/heldout_rcc_30`.
Excluded 41 failed or unfinished patch groups; details are in manifest.json.

Crashing — **Subset**: 10/51 certified patches; 8/10 frozen bugs.
Selection: first complete patch groups in source manifest order. Subset intervals describe only the selected bugs.

## Crashing

8 bugs; 10 patch/repetition legs per arm.

| Arm | Compiled / tries | Accepted / tries | Mean candidate RCC | Accepted-set RCC |
|---|---:|---:|---:|---:|
| H_N | 0.9042 [0.8542, 0.9542] | 0.8021 [0.5646, 0.9500] | 0.8250 [0.7083, 0.9333] | 0.8750 [0.6250, 1.0000] |
| H_N_C | 0.8396 [0.6958, 0.9625] | 0.7542 [0.5792, 0.9083] | 0.7583 [0.5958, 0.9042] | 1.0000 [1.0000, 1.0000] |
| H_R | 0.9000 [0.8521, 0.9479] | 0.7375 [0.5083, 0.8958] | 0.8167 [0.7312, 0.9042] | 0.8750 [0.6250, 1.0000] |

Raw counts (pooled counts; table rates above are bug-weighted):

- HN: 219/300 accepted; 268/300 compiled; 219 compiled and crashed before later acceptance checks.
- HN_C: 206/300 accepted; 242/300 compiled; 206 compiled and crashed before later acceptance checks.
- HR: 199/300 accepted; 269/300 compiled; 199 compiled and crashed before later acceptance checks.

