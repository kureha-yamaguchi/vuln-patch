# verifier replay (v5d_compliance_1446)

20 cases x 1 repeats, votes=1, model=config-default

scored: 20 cases (0 excluded — unresolved ladder, below)

| case | label | kept | note |
|---|---|---|---|
| night20_L04_oraclecontainsreadonlyseed_r10 | overfitting | 0/1 | [night20/04_patch1-Lang-60-Arja-plausible_o row 10] gold=UNSOUND class=lazy-state legoutcome=FN why=contains could legitimately compact buffer before searching |
| night20_L04_oraclecontainscapacitystableseed_r12 | overfitting | 0/1 | [night20/04_patch1-Lang-60-Arja-plausible_o row 12] gold=UNSOUND class=lazy-state legoutcome=FN why=contains could lazily minimizeCapacity then search |
| night20_L08_relationminuspositivezerohasnofo_r21 | overfitting | 0/1 | [night20/08_patch1-Closure-38-SequenceR_o row 21] gold=SOUND class=format-freedom legoutcome=FN why=correct printer could emit x-0.0 vs hard-coded x-0 |
| pool30_L04_oraclecontainsreadonly_r67 | overfitting | 0/1 | [pool30/04_patch1-Lang-60-Arja-plausible_o row 67] gold=SOUND class=contract-backed legoutcome=TP why=capacity 32→0 on same builder impossible for correct contains |
| pool30_L04_oraclecontainsreadonlycapacity_r68 | overfitting | 0/1 | [pool30/04_patch1-Lang-60-Arja-plausible_o row 68] gold=UNSOUND class=lazy-state legoutcome=TP why=contains could compact storage as optimization |
| pool30_L16_relationsubtractionpositiveinteg_r80 | overfitting | 0/1 | [pool30/16_patch1-Closure-38-SequenceR_o row 80] gold=SOUND class=format-freedom legoutcome=FN why=printer "could" emit optional separator `1- 2` |
| poolB_L04_oraclecontainsreadonly_r136 | overfitting | 0/1 | [poolB/04_patch1-Lang-60-Arja-plausible_o row 136] gold=SOUND class=contract-backed legoutcome=TP why=completed query changing capacity while text same impossible |
| width5_L04_oraclecontainsreadonlycapacity_r197 | overfitting | 1/1 | [width5/04_patch1-Lang-60-Arja-plausible_o row 197] gold=SOUND class=observed-impossible legoutcome=FN why=capacity is explicit observable state w/ separate minimizeCapacity mutator (later neutralized by universal screen; leg still FN) |
| width5_L04_relationcontainsdoesnotchangecap_r200 | overfitting | 1/1 | [width5/04_patch1-Lang-60-Arja-plausible_o row 200] gold=SOUND class=lazy-state legoutcome=FN why=contains "could legally" call minimizeCapacity first |
| pool30_L02_relationpackagenameclassoverload_r64 | overfitting | 1/1 | [pool30/02_patch1-Lang-41-Arja-plausible_o row 64] gold=SOUND class=trusted-lift legoutcome=TP why=overload delegation shown in implementation + trusted array tests |
| width5_L02_oraclepkgclasscanonicalagreement_r190 | overfitting | 1/1 | [width5/02_patch1-Lang-41-Arja-plausible_o row 190] gold=SOUND class=observed-impossible legoutcome=TP why=for observed java.util.Map.Entry input the values are determined |
| poolB_L26_relationintegratereturnsrequeste_r177 | overfitting | 1/1 | [poolB/26_patch1-Math-74-Arja-plausible_o row 177] gold=SOUND class=trusted-lift legoutcome=TP why=no-events integrate must stop at requested t |
| night20_L18_oraclecanonicalseparatedu_r54 | correct | 1/1 | [night20/18_patch1-Math-30-CapGen_c row 54] gold=UNSOUND class=preexisting/identical legoutcome=FP why=same mismatch fires on buggy; unrelated to trusted bug |
| pool30_L05_relationmaxmiddleindexpointstoam_r69 | overfitting | 1/1 | [pool30/05_patch1-Chart-7-Arja-plausible_o row 69] gold=UNSOUND class=other (alt-definition) legoutcome=FN why=integer-average middle could differ from check's recomputation |
| width5_L01_relationhypergeommeanformula_r188 | overfitting | 1/1 | [width5/01_patch1-Math-2-Arja-plausible_o row 188] gold=SOUND class=contract-backed legoutcome=TP why=documented n*m/N mean |
| width5_L02_relationpackagenameclassoverload_r191 | overfitting | 1/1 | [width5/02_patch1-Lang-41-Arja-plausible_o row 191] gold=SOUND class=trusted-lift legoutcome=TP why=two views of same documented package name |
| poolB_L05_oraclemaxmiddlemaximum_r137 | overfitting | 1/1 | [poolB/05_patch1-Chart-7-Arja-plausible_o row 137] gold=SOUND class=consistency legoutcome=TP why=recomputes max middle from object's own stored periods |
| night20_L18_relationlargesamplepvalueisproba_r56 | correct | 1/1 | [night20/18_patch1-Math-30-CapGen_c row 56] gold=UNSOUND class=contract-backed legoutcome=FP why=p-value must be finite in [0,1] |
| poolB_L18_relationreducedhalfdenominatorco_r156 | correct | 1/1 | [poolB/18_patch1-Lang-22-DeepRepair_c row 156] gold=UNSOUND class=trusted-lift legoutcome=FP why=getReducedFraction(2w,2) must equal whole-number fraction |
| night20_L11_oraclemirrorednegativenonbracket_r32 | overfitting | 1/1 | [night20/11_patch1-Math-73-ACS-plausible_o row 32] gold=SOUND class=contract-backed legoutcome=TP why=javadoc pins IAE for same-sign triples (DUTY pattern beats IDENT) |

**OVER-KILL rate (true findings dropped): 7/17 = 41%**
**LEAK rate (false findings kept): 3/3 = 100%**

Tokens: 223,199 total (214,366 in + 8,833 out, 20 calls)
By model: {"gpt-5.4": {"prompt_tokens": 214366, "completion_tokens": 8833, "total_tokens": 223199, "calls": 20}}

## unresolved-ladder (0 cases, excluded from the rates above)

The original run's trace shows the Spec-J ladder was ARMED for these firings (the buggy-replay value comparison returned identical) but records no family-duty event, so the rung it took — trigger-input exemption (fd_prior=True) or setup-divergence (fd_prior left None) — is not recoverable. They are replayed on fd_prior=None and reported here rather than scored on a guess.

(none)

Drift-kill signals not reconstructable from logged evidence (defaulted to conservative False) in 17/20 cases:
  - night20_L04_oraclecontainsreadonlyseed_r10: buggy_silent, deterministic_trigger, patched_firing
  - night20_L04_oraclecontainscapacitystableseed_r12: buggy_silent, deterministic_trigger, patched_firing
  - pool30_L04_oraclecontainsreadonly_r67: buggy_silent, deterministic_trigger, patched_firing
  - pool30_L04_oraclecontainsreadonlycapacity_r68: buggy_silent, deterministic_trigger, patched_firing
  - poolB_L04_oraclecontainsreadonly_r136: buggy_silent, deterministic_trigger, patched_firing
  - width5_L04_oraclecontainsreadonlycapacity_r197: buggy_silent, deterministic_trigger, patched_firing
  - pool30_L02_relationpackagenameclassoverload_r64: deterministic_trigger
  - width5_L02_oraclepkgclasscanonicalagreement_r190: buggy_silent, deterministic_trigger, patched_firing
  - poolB_L26_relationintegratereturnsrequeste_r177: deterministic_trigger
  - night20_L18_oraclecanonicalseparatedu_r54: buggy_silent, deterministic_trigger, patched_firing
  - pool30_L05_relationmaxmiddleindexpointstoam_r69: buggy_silent, deterministic_trigger
  - width5_L01_relationhypergeommeanformula_r188: buggy_silent
  - width5_L02_relationpackagenameclassoverload_r191: buggy_silent
  - poolB_L05_oraclemaxmiddlemaximum_r137: deterministic_trigger, patched_firing
  - night20_L18_relationlargesamplepvalueisproba_r56: buggy_silent
  - poolB_L18_relationreducedhalfdenominatorco_r156: buggy_silent, deterministic_trigger
  - night20_L11_oraclemirrorednegativenonbracket_r32: buggy_silent, deterministic_trigger, patched_firing
