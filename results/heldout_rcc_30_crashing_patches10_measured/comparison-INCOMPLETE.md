# Heldout harness comparison

Method-level RCC against developer-changed methods (R0), on the buggy build.
Means weight bugs equally, then patches/repetitions equally within each bug.
95% percentile CIs resample paired bugs; they do not treat adaptive candidates as independent.
Noncompiled candidates and empty accepted sets score zero when R0 is defined.
Missing coverage or failed region gates make the report incomplete.

HN = level B (no neighbourhood); HN_C = level C (function-only; no patch, failing tests, or neighbourhood); HR = full context.
Semantic level C has no lifted-test oracle; its acceptance/crash rate measures escaping findings without that oracle.

**INCOMPLETE — some selected measurements are missing.**

- crashing/HN/004_patch1-Lang-43-Arja_c_rep001: FileNotFoundError: [Errno 2] No such file or directory: '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_001_compiled.xml'
- crashing/HN_C/004_patch1-Lang-43-Arja_c_rep001: FileNotFoundError: [Errno 2] No such file or directory: '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_002_compiled.xml'
- crashing/HR/004_patch1-Lang-43-Arja_c_rep001: FileNotFoundError: [Errno 2] No such file or directory: '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/004_patch1-Lang-43-Arja_c_rep001/cov/attempt_001_compiled.xml'
- crashing/HN_C/008_patch1-Lang-43-CapGen_c_rep001: FileNotFoundError: [Errno 2] No such file or directory: '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN_C/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_001_compiled.xml'
- crashing/HR/008_patch1-Lang-43-CapGen_c_rep001: FileNotFoundError: [Errno 2] No such file or directory: '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_001_compiled.xml'
- crashing/HN/008_patch1-Lang-43-CapGen_c_rep001: FileNotFoundError: [Errno 2] No such file or directory: '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HN/008_patch1-Lang-43-CapGen_c_rep001/cov/attempt_001_compiled.xml'
- crashing/HR/010_patch1-Math-58-CapGen_c_rep001: FileNotFoundError: [Errno 2] No such file or directory: '/datadrive/vuln-patch/results/heldout_rcc_30/crashing/HR/010_patch1-Math-58-CapGen_c_rep001/cov/attempt_008_compiled.xml'
Measured existing artifacts from `/datadrive/vuln-patch/results/heldout_rcc_30`.
Excluded 41 failed or unfinished patch groups; details are in manifest.json.

