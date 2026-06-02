# Confirmed P0 variant-pair findings

**Total: 123 pairs** (strong 21, medium 32, unsure 70)

_Column legend:_

- **scb&audit** — `✓`/`✗` is the LLM `same_codebase` flag; the second character is the codebase-audit verdict: `=` agrees, `⚠` disagrees, `?` one side unknown.
- **deep** — verdict from the deep diff-relate LLM pass: `incomplete_fix_confirmed | same_root_cause_confirmed | one_extends_other | unrelated | insufficient_data`, or `skip: <reason>` when no LLM call was made (no patches, self-pair, budget).

_Group definitions:_

- **STRONG** — deep diff-relate verifier confirmed code-level relatedness (`incomplete_fix_confirmed` or `same_root_cause_confirmed`) and the codebase audit did not flag a disagreement.
- **MEDIUM** — high-confidence LLM-prose verdict but the deep verifier could not run (e.g. no patches fetchable for both sides).
- **UNSURE** — lower-confidence LLM-prose verdict, or deep verifier returned `unrelated`/`insufficient_data`, or the codebase audit disagrees with the LLM's `same_codebase` claim.

## STRONG — 21 pairs

| later | prior | llm_kind | scb&audit | conf | deep | cited_sentence |
|---|---|---|---|---|---|---|
| CVE-2022-4135 | chromium:1392715 | see_also | ✓= | 0.80 | same_root_cause_confirmed c=0.90 | Issue/Bug Report: https://bugs.chromium.org/p/chromium/issues/detail?id=1392715 |
| CVE-2022-4262 | chromium:1394403 | see_also | ✓= | 0.80 | same_root_cause_confirmed c=0.90 | Issue/Bug Report: https://bugs.chromium.org/p/chromium/issues/detail?id=1394403 |
| CVE-2022-1096 | CVE-2021-30551 | incomplete_fix | ✓= | 0.95 | incomplete_fix_confirmed c=0.80 | CVE-2022-1096 was incompletely fixed. |
| CVE-2022-1364 | chromium:1315901 | same_root_cause | ✓= | 0.80 | incomplete_fix_confirmed c=0.80 | Found variants: See https://bugs.chromium.org/p/chromium/issues/detail?id=1315901#c65 |
| CVE-2019-11707 | mozilla:1607670 | same_root_cause | ✓= | 0.80 | same_root_cause_confirmed c=0.78 | which found a non-security variant of this bug pattern: https://bugzilla.mozilla.org/show_bug.cgi?id=1607670 |
| CVE-2019-13720 | chromium:977107 | see_also | ✓= | 0.78 | same_root_cause_confirmed c=0.75 | The [bug report](https://bugs.chromium.org/p/chromium/issues/detail?id=977107) was made public in October 2019, 10 days before Kaspersky discovered and reported |
| CVE-2021-44828 | CVE-2021-39793 | same_root_cause | ✓? | 0.75 | incomplete_fix_confirmed c=0.75 | "Looking through the list of public Mali bugs for issues described as _\"Mali GPU Kernel Driver elevates CPU RO pages to writable\"_, there is a third bug CVE-2 |
| CVE-2021-21206 | chromium:1045874 | same_root_cause | ✓= | 0.85 | same_root_cause_confirmed c=0.78 | unexpected JavaScript callback triggered by a `thennable` object' is a fairly well-known bug class within Chrome that commonly causes use-after-frees and [itera |
| CVE-2021-37975 | chromium:1252918 | see_also | ✓= | 0.90 | same_root_cause_confirmed c=0.85 | Issue/Bug Report: https://bugs.chromium.org/p/chromium/issues/detail?id=1252918 |
| CVE-2021-4102 | chromium:1278387 | see_also | ✓= | 0.90 | same_root_cause_confirmed c=0.90 | Issue/Bug Report: [crbug.com/1278387](https://crbug.com/1278387) |
| CVE-2021-4102 | chromium:791245 | same_root_cause | ✓= | 0.80 | same_root_cause_confirmed c=0.75 | There was a bug in Turbofan's handling of WriteBarriers in the past during `SimplfiedLoweringPhase` [crbug.com/791245]. |
| CVE-2021-4102 | chromium:1307610 | same_root_cause | ✓= | 0.85 | same_root_cause_confirmed c=0.70 | * [crbug.com/1307610](https://crbug.com/1307610) discovered by Brendon Tiszka is not a direct variant of this vulnerability, however it is another WriteBarrier  |
| CVE-2021-21166 | chromium:1174582 | see_also | ✓= | 0.85 | same_root_cause_confirmed c=0.86 | **Issue/Bug Reports:** * https://bugs.chromium.org/p/chromium/issues/detail?id=1174582 * https://bugs.chromium.org/p/chromium/issues/detail?id=1181341 * https:/ |
| CVE-2021-21166 | chromium:1177465 | see_also | ✓= | 0.90 | incomplete_fix_confirmed c=0.78 | Issue/Bug Reports: * https://bugs.chromium.org/p/chromium/issues/detail?id=1177465 |
| CVE-2020-6572 | chromium:1066893 | see_also | ✓= | 0.85 | same_root_cause_confirmed c=0.82 | **Issue/Bug Report:** https://bugs.chromium.org/p/chromium/issues/detail?id=1066893 |
| CVE-2020-6572 | chromium:1004730 | same_root_cause | ✓= | 0.85 | same_root_cause_confirmed c=0.75 | This vulnerability is essentially the same bug as CVE-2020-6572, it's just triggered by an error path after initialize `MojoAudioDecoderService` twice rather th |
| CVE-2020-6572 | chromium:999311 | same_root_cause | ✓= | 0.90 | same_root_cause_confirmed c=0.80 | This vulnerability is essentially the same bug as CVE-2020-6572, it's just triggered by an error path after initialize `MojoAudioDecoderService` twice rather th |
| CVE-2020-16009 | chromium:1143772 | see_also | ✓= | 0.90 | same_root_cause_confirmed c=0.86 | Chromium: https://bugs.chromium.org/p/chromium/issues/detail?id=1143772 |
| CVE-2020-6820 | mozilla:1626728 | see_also | ✓= | 0.86 | same_root_cause_confirmed c=0.84 | **Issue/Bug Report:** https://bugzilla.mozilla.org/show_bug.cgi?id=1626728 |
| CVE-2020-6820 | mozilla:1507180 | same_root_cause | ✓= | 0.78 | same_root_cause_confirmed c=0.80 | Manual code auditing/variant analysis after seeing the patch for [Bug 1507180](https://bugzilla.mozilla.org/show_bug.cgi?id=1507180) which is a similar UaF in t |
| CVE-2020-6820 | mozilla:1627892 | same_root_cause | ✓= | 0.70 | incomplete_fix_confirmed c=0.78 | Since the vulnerability was disclosed, Firefox has already begun reducing the use of raw pointers in the Cache module. |

## MEDIUM — 32 pairs

| later | prior | llm_kind | scb&audit | conf | deep | cited_sentence |
|---|---|---|---|---|---|---|
| CVE-2022-41073 | CVE-2022-29104 | same_root_cause | ✓? | 0.85 | skip: no patches for both sides (later=m | CVE-2022-41073 is the same bug as [CVE-2022-29104](https://msrc.microsoft.com/update-guide/en-us/vulnerability/CVE-2022-29104), which was fixed in May 2022 and  |
| CVE-2022-21882 | CVE-2021-1732 | same_root_cause | ✓= | 0.86 | skip: no patches for both sides (later=o | Known cases of the same exploit flow: It is the same as the previous CVE-2021-1732 exploit, and is a common way of exploiting privilege escalation vulnerabiliti |
| CVE-2022-1232 | CVE-2022-1096 | incomplete_fix | ✓? | 0.95 | skip: no patches for both sides (later=m | CVE-2022-1096 was incompletely fixed. |
| CVE-2022-1096 | chromium-p0:2280 | incomplete_fix | ✓? | 0.95 | unrelated c=0.85 | CVE-2022-1096 was incompletely fixed. |
| CVE-2022-4906 | CVE-2022-3723 | same_root_cause | ✓? | 0.85 | skip: no patches for both sides (later=m | it wouldn't have prevented CVE-2022-4906 (see above) which is otherwise very similar. |
| CVE-2023-28252 | CVE-2022-37969 | same_root_cause | ✓? | 0.85 | skip: no patches for both sides (later=m | **Known cases of the same exploit flow:** The gadgets are same as the ITW exploit of `CVE-2023-23376`, the code layout has overlaps with the ITW exploit of `CVE |
| CVE-2023-6345 | CVE-2023-2136 | same_root_cause | ✓= | 0.85 | skip: no patches for both sides (later=m | Exploits for [CVE-2023-2136] and CVE-2023-6345 used the same technique to reach Skia from within the Chrome renderer (i.e. by using a `DrawSlugOp` command). |
| CVE-2023-33107 | CVE-2020-11261 | same_root_cause | ✓= | 0.85 | skip: no patches for both sides (later=o | CVE-2023-33107 could have been found by analyzing the patch for CVE-2020-11261 and auditing the same code area for variants. |
| CVE-2020-0030 | CVE-2019-2215 | incomplete_fix | ✓? | 0.95 | unrelated c=0.90 | The patch for the in-the-wild 0-day (CVE-2019-2215) actually introduced another use-after-free condition. |
| CVE-2019-1367 | CVE-2018-8653 | same_root_cause | ✓= | 0.86 | skip: no patches for both sides (later=m | Therefore it’s likely that the actor found this bug by performing variant analysis on CVE-2018-8653, looking for vulnerabilities in the same bug class. |
| CVE-2020-1429 | CVE-2019-1367 | incomplete_fix | ✓? | 0.90 | skip: no patches for both sides (later=m | The incomplete patch and the variant are patched as CVE-2020-1429. |
| CVE-2020-0674 | CVE-2019-1367 | incomplete_fix | ✓= | 0.90 | skip: no patches for both sides (later=m | * **Nov 2019** - The incomplete patch and the variant are patched as CVE-2020-1429. * **Jan 2020** - TAG discovers another in-the-wild exploit sample ([CVE-2020 |
| CVE-2019-13720 | chromium-p0:1963 | incomplete_fix | ✓? | 0.90 | unrelated c=0.86 | First patch was incomplete and thus the 2nd CVE was issued to fully fix the issue. |
| CVE-2020-6427 | CVE-2019-13720 | incomplete_fix | ✓? | 0.90 | skip: no patches for both sides (later=m | First patch was incomplete and thus the 2nd CVE was issued to fully fix the issue. |
| CVE-2020-6428 | CVE-2019-13720 | incomplete_fix | ✓? | 0.96 | skip: no patches for both sides (later=m | First patch was incomplete and thus the 2nd CVE was issued to fully fix the issue. |
| CVE-2020-6429 | CVE-2019-13720 | incomplete_fix | ✓? | 0.95 | skip: no patches for both sides (later=m | First patch was incomplete and thus the 2nd CVE was issued to fully fix the issue. |
| CVE-2020-6449 | CVE-2019-13720 | incomplete_fix | ✓? | 0.95 | insufficient_data c=0.55 | First patch was incomplete and thus the 2nd CVE was issued to fully fix the issue. |
| CVE-2020-6451 | CVE-2019-13720 | incomplete_fix | ✓? | 0.90 | skip: no patches for both sides (later=m | First patch was incomplete and thus the 2nd CVE was issued to fully fix the issue. |
| CVE-2020-6450 | CVE-2019-13720 | incomplete_fix | ✓? | 0.95 | skip: no patches for both sides (later=m | First patch was incomplete and thus the 2nd CVE was issued to fully fix the issue. |
| CVE-2021-39793 | CVE-2021-28664 | incomplete_fix | ✓= | 0.90 | skip: no patches for both sides (later=o | The patch addresses the remaining site that was missed in the CVE-2021-28664 fix (see below). |
| CVE-2021-4102 | chromium:1382434 | same_root_cause | ✓= | 0.88 | unrelated c=0.86 | crbug.com/1382434 discovered by Sergei Glazunov of Google Project Zero is not a variant of this vulnerability, however it uses WriteBarrier elision to exploit a |
| CVE-2020-6572 | CVE-2019-13695 | same_root_cause | ✓? | 0.85 | skip: no patches for both sides (later=o | This vulnerability is essentially the same bug as CVE-2020-6572, it's just triggered by an error path after initialize `MojoAudioDecoderService` twice rather th |
| CVE-2020-6572 | CVE-2019-5870 | same_root_cause | ✓? | 0.86 | skip: no patches for both sides (later=o | This vulnerability is essentially the same bug as CVE-2020-6572, it's just triggered by an error path after initialize `MojoAudioDecoderService` twice rather th |
| CVE-2020-0674 | CVE-2018-8653 | same_root_cause | ✓= | 0.86 | skip: no patches for both sides (later=m | There are now 4 JScript vulnerabilities (CVE-2018-8653, CVE-2019-1367, CVE-2019-1429, and CVE-2020-0674) of the same bug class, using the same exploitation meth |
| CVE-2020-0674 | CVE-2019-1429 | same_root_cause | ✓= | 0.90 | skip: no patches for both sides (later=m | This vulnerability is a trivial variant of [CVE-2019-1367]/CVE-2019-1429 and thus shares the long history with that bug. |
| CVE-2020-27946 | CVE-2020-27930 | same_root_cause | ✓? | 0.87 | skip: no patches for both sides (later=m | * [CVE-2020-27946](https://bugs.chromium.org/p/project-zero/issues/detail?id=2113): Apple CoreText libType1Scaler.dylib memory disclosure via uninitialized arra |
| CVE-2020-29624 | CVE-2020-27930 | same_root_cause | ✓? | 0.85 | skip: no patches for both sides (later=m | **Found variants:** * [CVE-2020-29624](https://bugs.chromium.org/p/project-zero/issues/detail?id=2115): Apple CoreText libFontParser.dylib stack corruption in t |
| CVE-2020-27944 | CVE-2020-27930 | same_root_cause | ✓? | 0.85 | skip: no patches for both sides (later=m | Found variants: * [CVE-2020-27943] ... * [CVE-2020-27944](https://bugs.chromium.org/p/project-zero/issues/detail?id=2116): Apple CoreText libType1Scaler.dylib h |
| CVE-2020-27930 | chromium-p0:2114 | same_root_cause | ✓? | 0.86 | skip: no patches for both sides (later=m | Found variants: * [CVE-2020-27943](https://bugs.chromium.org/p/project-zero/issues/detail?id=2114): Apple CoreText libType1Scaler.dylib heap buffer overflow in  |
| CVE-2020-0986 | chromium-p0:2096 | incomplete_fix | ✓? | 0.90 | skip: no patches for both sides (later=m | Not really a variant, but instead identifying that the original fix was bad. |
| CVE-2020-16011 | CVE-2020-16010 | same_root_cause | ✓? | 0.90 | skip: no patches for both sides (later=m | Found variants: * [CVE-2020-16011](https://bugs.chromium.org/p/project-zero/issues/detail?id=2112#c3): An identical bug existing in Chrome for Windows |
| CVE-2022-26925 | CVE-2021-36942 | regression | ✓? | 0.90 | skip: no patches for both sides (later=m | CVE-2021-36942 (Patch regressed) |

## UNSURE — 70 pairs

| later | prior | llm_kind | scb&audit | conf | deep | cited_sentence |
|---|---|---|---|---|---|---|
| CVE-2019-1429 | CVE-2019-1367 | incomplete_fix | ✓= | 0.88 | skip: no patches for both sides (later=m | The incomplete patch and the variant are patched as CVE-2020-1429. |
| CVE-2022-22265 | CVE-2020-28343 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | The bug was likely discovered while performing variant analysis on [CVE-2020-28343]. |
| CVE-2022-22265 | chromium-p0:2073 | same_root_cause | ✓? | 0.84 | skip: no patches for both sides (later=m | The bug was likely discovered while performing variant analysis on [CVE-2020-28343](https://bugs.chromium.org/p/project-zero/issues/detail?id=2073). |
| CVE-2022-4135 | chromium:1335422 | same_root_cause | ✓= | 0.75 | unrelated c=0.92 | **Ideas to kill the bug class:** The specific subclass (out-of-bounds access on an `std::vector`) has been eliminated in Chrome by ["safe C++ mode"](https://bug |
| CVE-2022-4262 | chromium:1425616 | same_root_cause | ✓= | 0.80 | one_extends_other c=0.78 | not all such issues had security impact, such as [this one](https://bugs.chromium.org/p/chromium/issues/detail?id=1425616) where due to lucky circumstances, the |
| CVE-2022-41128 | CVE-2021-34480 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | This vulnerability is very similar to [CVE-2021-34480](https://bugs.chromium.org/p/project-zero/issues/detail?id=2188) discovered by Ivan Fratric through fuzzin |
| CVE-2022-41128 | chromium-p0:2188 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | This vulnerability is very similar to [CVE-2021-34480](https://bugs.chromium.org/p/project-zero/issues/detail?id=2188) discovered by Ivan Fratric through fuzzin |
| CVE-2022-1096 | chromium:1309225 | see_also | ✓= | 0.75 | unrelated c=0.62 | Issue/Bug Report: https://bugs.chromium.org/p/chromium/issues/detail?id=1309225 |
| CVE-2022-1096 | CVE-2016-5128 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=o | So it seems reasonable to search to see if the same vulnerability could be exploited via the other "if/else" branch, which is what happened here. |
| CVE-2022-1364 | chromium:1263462 | same_root_cause | ✓= | 0.80 | unrelated c=0.86 | Known cases of the same exploit flow: [CVE-2021-38003](https://bugs.chromium.org/p/chromium/issues/detail?id=1263462) had exploited access to the "hole" in the  |
| CVE-2022-1364 | CVE-2021-21195 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=o | Could've been found as a variant of [CVE-2021-21195](https://bugs.chromium.org/p/chromium/issues/detail?id=1182647) |
| CVE-2022-1364 | chromium:1182647 | same_root_cause | ✓= | 0.65 | unrelated c=0.75 | Could've been found as a variant of [CVE-2021-21195](https://bugs.chromium.org/p/chromium/issues/detail?id=1182647) or through (differential?) fuzzing. |
| CVE-2022-1364 | CVE-2021-38003 | same_root_cause | ✓= | 0.80 | skip: no patches for both sides (later=o | CVE-2021-38003 had exploited access to the "hole" in the same way. |
| CVE-2022-3723 | chromium:1378239 | see_also | ✓= | 0.88 | unrelated c=0.86 | Issue/Bug Report: https://bugs.chromium.org/p/chromium/issues/detail?id=1378239 (Embargoed) |
| CVE-2022-3723 | chromium:1382434 | same_root_cause | ✓= | 0.78 | unrelated c=0.85 | Found variants: Sergei Glazunov from Project Zero found a similar bug that involves copy-on-write arrays: [CVE-2022-4906](https://bugs.chromium.org/p/chromium/i |
| CVE-2023-38831 | CVE-2023-3883 | same_root_cause | ✓? | 0.78 | skip: no patches for both sides (later=m | Note that while most samples exploiting CVE-2023-3883 use an archive entry with a trailing space, it is not a requirement, and a space in any position in the fi |
| CVE-2023-28252 | CVE-2023-23376 | same_root_cause | ✓= | 0.75 | skip: no patches for both sides (later=m | The ITW exploitation strategies and code flows are close to the exploit of [CVE-2023-23376] |
| CVE-2023-36802 | CVE-2022-37969 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | Known cases of the same exploit flow: The in the wild exploited CVE-2022-37969 CLFS vulnerability followed a very similar exploit flow, as descibed by [Zscaler' |
| CVE-2023-26369 | CVE-2011-3402 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | The vulnerability is very similar to CVE-2011-3402 affecting the TrueType font parsing engine in win32k and exploited in the wild by Duqu. |
| CVE-2023-36033 | CVE-2022-21902 | same_root_cause | ✓? | 0.78 | skip: no patches for both sides (later=m | `CVE-2022-21902` is an OOB read in `CKeyframeAnimation::AddKeyframeData` dealing with *DCOMPOSITION_EXPRESSION_TYPE_PATH* type of `CPathData` |
| CVE-2019-11707 | mozilla:1544386 | see_also | ✓= | 0.85 | unrelated c=0.80 | Firefox issue: https://bugzilla.mozilla.org/show_bug.cgi?id=1544386 |
| CVE-2019-11707 | CVE-2019-9810 | same_root_cause | ✓? | 0.80 | unrelated c=0.90 | The result is a classic incorrect side-effect modelling bug, similar to for example [CVE-2019-9810](https://www.mozilla.org/en-US/security/advisories/mfsa2019-0 |
| CVE-2019-1367 | chromium-p0:1504 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | Finds numerous bugs ([P0 1504](https://bugs.chromium.org/p/project-zero/issues/detail?id=1504), [P0 1505]..., [P0 1506]...) through fuzzer |
| CVE-2019-1367 | chromium-p0:1505 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | Finds numerous bugs ([P0 1504](https://bugs.chromium.org/p/project-zero/issues/detail?id=1504), [P0 1505](https://bugs.chromium.org/p/project-zero/issues/detail |
| CVE-2019-1367 | chromium-p0:1947 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | Found variants: * [P0 1947](https://bugs.chromium.org/p/project-zero/issues/detail?id=1947) (CVE-2019-1429): Use-after-free where members of the arguments objec |
| CVE-2019-13720 | chromium:1019226 | see_also | ✓= | 0.85 | one_extends_other c=0.90 | **Issue/Bug Report:** https://bugs.chromium.org/p/chromium/issues/detail?id=1019226 |
| CVE-2019-13732 | CVE-2019-13720 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | Found variants: P0 1963 (CVE-2019-13732, CVE-2020-6406): Heap use-after-free in PannerHandler::TailTime. |
| CVE-2020-6406 | CVE-2019-13720 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | Found variants: * [P0 1963] (CVE-2019-13732, CVE-2020-6406): Heap use-after-free in PannerHandler::TailTime. |
| CVE-2021-21206 | chromium:663476 | same_root_cause | ✓= | 0.77 | skip: no patches for both sides (later=o | Promise.then publicly documented in https://bugs.chromium.org/p/chromium/issues/detail?id=663476#c10 |
| CVE-2021-21206 | chromium:678706 | same_root_cause | ✓= | 0.80 | skip: no patches for both sides (later=o | A vulnerability researcher could have also come across crbug.com/678706 and crbug.com/708887 dug deeper into this area of code. (Historical/present/future) cont |
| CVE-2021-21206 | chromium:708887 | same_root_cause | ✓= | 0.75 | skip: no patches for both sides (later=o | A vulnerability researcher could have also come across crbug.com/678706 and crbug.com/708887 dug deeper into this area of code. |
| CVE-2021-38000 | chromium:1249962 | see_also | ✓= | 0.80 | one_extends_other c=0.90 | Issue/Bug Report: https://bugs.chromium.org/p/chromium/issues/detail?id=1249962 |
| CVE-2021-37975 | CVE-2019-13720 | same_root_cause | ✓= | 0.80 | unrelated c=0.92 | **Known cases of the same exploit flow:** The `TypedArray/ArrayBuffer` exploit flow follows the exploit of [CVE-2019-13720](https://googleprojectzero.github.io/ |
| CVE-2021-37975 | chromium:1252878 | same_root_cause | ✓= | 0.80 | one_extends_other c=0.90 | The bug [1252878] ... appears to be the same ephemeron handling problem in oilpan. |
| CVE-2021-37975 | github:v8/v8@e677a6f6b257 | same_root_cause | ✓= | 0.80 | skip: self-pair (later and prior resolve | Found variants: The bug [1252878] (https://bugs.chromium.org/p/chromium/issues/detail?id=1252878) (patch (https://github.com/v8/v8/commit/e677a6f6b257e992094b91 |
| CVE-2021-30551 | chromium:1216437 | see_also | ✓= | 0.90 | unrelated c=0.80 | Issue/Bug Report: https://bugs.chromium.org/p/chromium/issues/detail?id=1216437 |
| CVE-2021-30551 | chromium:619166 | same_root_cause | ✓= | 0.75 | skip: no patches for both sides (later=o | It's possible, however, that the researcher used https://crbug.com/619166, a bug where unexpected JS execution inside interceptors during a JS property assignme |
| CVE-2021-30632 | CVE-2020-16009 | same_root_cause | ✓= | 0.80 | unrelated c=0.72 | The ingredients required (optimization of multiple functions and having multiple objects in different stages of the transition tree) for generating this type of |
| CVE-2021-30632 | chromium-p0:2106 | same_root_cause | ✓? | 0.82 | skip: no patches for both sides (later=o | The ingredients required (optimization of multiple functions and having multiple objects in different stages of the transition tree) for generating this type of |
| CVE-2021-30632 | chromium:1209558 | same_root_cause | ✓= | 0.75 | unrelated c=0.80 | Some vulnerabilities in property access: * https://bugs.chromium.org/p/chromium/issues/detail?id=1209558 |
| CVE-2021-30632 | chromium:1216437 | same_root_cause | ✓= | 0.70 | unrelated c=0.80 | Some vulnerabilities in property access: * https://bugs.chromium.org/p/chromium/issues/detail?id=1209558 * https://bugs.chromium.org/p/chromium/issues/detail?id |
| CVE-2021-30632 | chromium:1203122 | same_root_cause | ✓= | 0.80 | unrelated c=0.90 | Some vulnerabilities in property access: * https://bugs.chromium.org/p/chromium/issues/detail?id=1209558 * https://bugs.chromium.org/p/chromium/issues/detail?id |
| CVE-2021-30632 | chromium:746946 | same_root_cause | ✓= | 0.67 | skip: no patches for both sides (later=o | Some vulnerabilities in map transition/deprecation: * https://bugs.chromium.org/p/chromium/issues/detail?id=746946 |
| CVE-2021-1905 | CVE-2020-11261 | same_root_cause | ✓= | 0.80 | skip: no patches for both sides (later=m | This change is believed to be related to CVE-2020-11261 (also marked as exploited in-the-wild), and is not directly useful by itself. |
| CVE-2021-4102 | chromium:1423610 | same_root_cause | ✓= | 0.80 | unrelated c=0.80 | * [crbug.com/1423610](https://crbug.com/1423610) discovered by Nan Wang and Zhenghang Xiao of Qihoo360 is another WriteBarrier elision bug in V8's Maglev JIT co |
| CVE-2021-4083 | CVE-2021-0920 | same_root_cause | ✓? | 0.80 | unrelated c=0.83 | Found variants: [CVE-2021-4083 by Jann Horn] |
| CVE-2021-30858 | chromium:1032890 | see_also | ✗= | 0.75 | unrelated c=0.82 | It is also possible though that the attackers found the vulnerability after seeing the similar Chrome & WebKit bugs. |
| CVE-2021-26411 | CVE-2019-1208 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | Getting arbitrary read-write via changing the length of an Array is very common, but [this blog post](https://www.trendmicro.com/en_us/research/19/i/from-bindif |
| CVE-2019-17026 | CVE-2019-9810 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | Similar bugs in Spidermonkey: + https://doar-e.github.io/blog/2019/06/17/a-journey-into-ionmonkey-root-causing-cve-2019-9810/ + https://bugs.chromium.org/p/proj |
| CVE-2019-17026 | chromium-p0:1820 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | is a similar type of bug in Spidermonkey that was also exploited in the wild [P0 1820]. |
| CVE-2020-1429 | CVE-2020-0674 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | There are now 4 JScript vulnerabilities (CVE-2018-8653, CVE-2019-1367, CVE-2019-1429, and CVE-2020-0674) of the same bug class, using the same exploitation meth |
| CVE-2020-0968 | CVE-2020-0674 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | Found variants: * [CVE-2020-0968](https://msrc.microsoft.com/update-guide/en-us/vulnerability/CVE-2020-0968): During string concatenation, one of the two string |
| CVE-2020-17053 | CVE-2020-1380 | same_root_cause | ✓? | 0.75 | skip: no patches for both sides (later=m | Found variants: * [CVE-2020-17053](https://msrc.microsoft.com/update-guide/en-US/vulnerability/CVE-2020-17053): Discovered by Elliot Cao (@iamelli0t). |
| CVE-2020-16010 | CVE-2020-16009 | same_root_cause | ✓= | 0.70 | unrelated c=0.90 | This vulnerability was used by the same actors during the same operation as [CVE-2020-15999](CVE-2020-15999.md), [CVE-2020-17087](CVE-2020-17087.md), [CVE-2020- |
| CVE-2020-27930 | CVE-2020-16009 | same_root_cause | ✓⚠ | 0.75 | skip: no patches for both sides (later=m | This vulnerability was used by the same actors during the same operation as [CVE-2020-15999](CVE-2020-15999.md), [CVE-2020-17087](CVE-2020-17087.md), [CVE-2020- |
| CVE-2020-27932 | CVE-2020-16009 | same_root_cause | ✓⚠ | 0.80 | skip: no patches for both sides (later=m | Fuzzing or auditing the Map transition/deprecation logic. **Found variants:** |
| CVE-2020-15999 | chromium-p0:168 | same_root_cause | ✓? | 0.80 | unrelated c=0.95 | It's also quite possible that it has been discovered as a result of variant analysis of https://bugs.chromium.org/p/project-zero/issues/detail?id=168. |
| CVE-2020-6418 | chromium:1053604 | see_also | ✓= | 0.90 | unrelated c=0.90 | Issue/Bug Report: https://bugs.chromium.org/p/chromium/issues/detail?id=1053604 |
| CVE-2020-27943 | CVE-2020-27930 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | **Found variants:** * [CVE-2020-27943](https://bugs.chromium.org/p/project-zero/issues/detail?id=2114): Apple CoreText libType1Scaler.dylib heap buffer overflow |
| CVE-2020-27930 | chromium-p0:2113 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | Perform a complete audit of the CharString interpreter implemented in `libType1Scaler.dylib`. **Found variants:** * [CVE-2020-27946](https://bugs.chromium.org/p |
| CVE-2020-27930 | chromium-p0:2115 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | Perform a complete audit of the CharString interpreter implemented in `libType1Scaler.dylib`. **Found variants:** * [CVE-2020-27943] ... * [CVE-2020-29624] ...  |
| CVE-2020-27930 | chromium-p0:2116 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | Perform a complete audit of the CharString interpreter implemented in `libType1Scaler.dylib`. **Found variants:** * [CVE-2020-27944](https://bugs.chromium.org/p |
| CVE-2020-27930 | CVE-2015-3052 | same_root_cause | ✓? | 0.78 | skip: no patches for both sides (later=m | July 2015: A [series of blog posts] about the exploitation of the BLEND Type 1 operator vulnerabilities (CVE-2015-0093, CVE-2015-3052), similar in nature to the |
| CVE-2020-0986 | CVE-2019-0880 | incomplete_fix | ✓= | 0.85 | skip: no patches for both sides (later=m | Found variants: * [P0 2096](https://bugs.chromium.org/p/project-zero/issues/detail?id=2096): Not really a variant, but instead identifying that the original fix |
| CVE-2020-17008 | CVE-2020-0986 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=m | This fix was assigned CVE-2020-17008/CVE-2021-1648. |
| CVE-2021-1648 | CVE-2020-0986 | same_root_cause | ✓? | 0.80 | skip: no patches for both sides (later=o | (CVE-2020-17008/CVE-2021-1648) ### Structural improvements * Verifying any pointers that are passed in a LPC message in ProcessRequest prior to passing to GdiPr |
| CVE-2020-6820 | mozilla:1368273 | same_root_cause | ✓= | 0.80 | skip: no patches for both sides (later=o | It seems that others, including Firefox engineers, also haven’t found a way to trigger this bug and similar bugs that require winning this race condition that d |
| CVE-2020-6820 | mozilla:1655115 | same_root_cause | ✓= | 0.78 | one_extends_other c=0.70 | Found variants: * [Bug 165115](https://bugzilla.mozilla.org/show_bug.cgi?id=1655115): UAF in `StreamControl::CloseReadStreams`, but the code is dead code. |
| CVE-2020-16010 | chromium-p0:2112 | same_root_cause | ✓? | 0.80 | unrelated c=0.90 | Found variants: * [CVE-2020-16011](https://bugs.chromium.org/p/project-zero/issues/detail?id=2112#c3): An identical bug existing in Chrome for Windows |
| CVE-2020-16010 | chromium:1144368 | see_also | ✓= | 0.85 | one_extends_other c=0.90 | Chromium: https://bugs.chromium.org/p/chromium/issues/detail?id=1144368 |
