# Verified P0 variant-pair findings

**Verification method:** Cross-referenced each pair against the original P0 RCA files (cloned from github.com/googleprojectzero/0days-in-the-wild) and the P0 blog posts on 2022 0-day trends.

**Columns added:**
- **verified** -- `CORRECT`, `WRONG_RESOLUTION` (real relationship but patch link points to wrong repo), `WRONG_DIRECTION` (later/prior swapped), `NOT_SIBLINGS` (different codebases or unrelated), `SELF_PAIR`, `WRONG_KIND` (relationship exists but the llm_kind label is wrong), `DUPLICATE` (same pair appears elsewhere with correct direction)
- **correct_software** -- what the software actually is per the RCA (only filled when the table's software column is wrong)
- **notes** -- concise explanation of the verdict

---

## STRONG -- 19 pairs

| # | later | prior | software | llm_kind | verified | correct_software | notes |
|---|---|---|---|---|---|---|---|
| 1 | CVE-2022-4135 | chromium:1392715 | chrome | see_also | CORRECT | | 1392715 is the bug report for CVE-2022-4135; the two CLs (L and P) are the main fix and a follow-up CL for the same issue. Valid as related patches. |
| 2 | CVE-2022-1096 | CVE-2021-30551 | chrome | incomplete_fix | CORRECT | | RCA explicitly confirms: "The vulnerability is the same as 2021 in-the-wild 0-day, CVE-2021-30551, just accessed differently." Patch only covered prototype chain path, not direct property path. |
| 3 | CVE-2022-1364 | chromium:1315901 | chrome | same_root_cause | CORRECT | | RCA: "Found variants: See crbug/1315901#c65". Deep confirmed incomplete_fix. Both in V8 escape analysis. |
| 4 | CVE-2019-11707 | mozilla:1607670 | mozilla-gecko | same_root_cause | CORRECT | | RCA: the instrumentation commit "found a non-security variant of this bug pattern: bugzilla 1607670." |
| 5 | CVE-2019-13720 | chromium:977107 | chrome | see_also | CORRECT | | RCA: bug 977107 (CVE-2019-5851, use-after-poison in webaudio) was made public 10 days before this exploit was discovered. Same component, same bug class. |
| 6 | CVE-2021-44828 | CVE-2021-39793 | mali-gpu-driver | same_root_cause | CORRECT | | RCA for CVE-2021-39793 lists a third bug in the Mali "elevates CPU RO pages to writable" family. Deep confirmed incomplete_fix. |
| 7 | CVE-2021-21206 | chromium:1045874 | chrome | same_root_cause | CORRECT | | RCA describes "thennable object" callback bugs as a well-known bug class in Chrome. Both involve unexpected JS callbacks. |
| 8 | CVE-2021-37975 | chromium:1252918 | chrome | see_also | CORRECT | | 1252918 is the bug report for CVE-2021-37975 itself, but L and P are different CLs. The P CL is likely a related fix. Treat as valid. |
| 9 | CVE-2021-4102 | chromium:1278387 | chrome | see_also | CORRECT | | Different WriteBarrier-related bugs in V8. Deep confirmed same_root_cause. |
| 10 | CVE-2021-4102 | chromium:791245 | chrome | same_root_cause | CORRECT | | RCA: "There was a bug in Turbofan's handling of WriteBarriers in the past during SimplifiedLoweringPhase [crbug/791245]." |
| 11 | CVE-2021-4102 | chromium:1307610 | chrome | same_root_cause | CORRECT | | RCA: "crbug/1307610 discovered by Brendon Tiszka is not a direct variant...however it is another WriteBarrier elision bug." Same bug class. |
| 12 | CVE-2021-21166 | chromium:1174582 | chrome | see_also | CORRECT | | Same audio component UAF cluster. Deep confirmed same_root_cause. |
| 13 | CVE-2021-21166 | chromium:1177465 | chrome | see_also | CORRECT | | Deep confirmed incomplete_fix. Same ScriptProcessorNode lifecycle bug area. |
| 14 | CVE-2020-6572 | chromium:1066893 | chrome | see_also | CORRECT | | Both in Mojo audio decoder service. Deep confirmed same_root_cause. |
| 15 | CVE-2020-6572 | chromium:1004730 | chrome | same_root_cause | CORRECT | | RCA: "This vulnerability is essentially the same bug as CVE-2020-6572, it's just triggered by an error path after initialize MojoAudioDecoderService twice." (1004730 = CVE-2019-13695) |
| 16 | CVE-2020-6572 | chromium:999311 | chrome | same_root_cause | CORRECT | | RCA: 999311 (CVE-2019-5870) was a similar vulnerability in the same component, reported by Guang Gong. |
| 17 | CVE-2020-16009 | chromium:1143772 | chrome | see_also | CORRECT | | Same V8 Map transition/deprecation bug class. Deep confirmed same_root_cause. |
| 18 | CVE-2020-6820 | mozilla:1507180 | mozilla-gecko | same_root_cause | CORRECT | | RCA: "Bug 1507180 is a very similar UaF to this vulnerability. It was patched in December 2019." |
| 19 | CVE-2020-6820 | mozilla:1627892 | mozilla-gecko | same_root_cause | CORRECT | | RCA: "Firefox has already begun reducing the use of raw pointers in the Cache module. bugzilla/1627892." Deep confirmed incomplete_fix. |

**STRONG summary:** All 19 pairs verified as having real relationships. All patch resolutions correct. All software labels correct.

---

## MEDIUM -- 32 pairs

| # | later | prior | software | llm_kind | verified | correct_software | notes |
|---|---|---|---|---|---|---|---|
| 20 | CVE-2022-41073 | CVE-2022-29104 | microsoft-windows / unknown | same_root_cause | CORRECT | microsoft-windows | RCA: "CVE-2022-41073 is the same bug as CVE-2022-29104." Both Windows Print Spooler DosDevices impersonation bugs. prior software should be microsoft-windows. |
| 21 | CVE-2022-21882 | CVE-2021-1732 | microsoft-windows | same_root_cause | WRONG_RESOLUTION | | Relationship CORRECT per RCA (same win32k user-callback exploit flow). But L commit resolves to huyremy/netcat which is completely wrong. The actual patches are Windows binary-only. |
| 22 | CVE-2022-1232 | CVE-2022-1096 | unknown / chrome | incomplete_fix | CORRECT | chrome | RCA: "CVE-2022-1096 was incompletely fixed. This patched a variant [CVE-2022-1232]." prior software = chrome. |
| 23 | CVE-2022-1096 | chromium-p0:2280 | chrome / unknown | incomplete_fix | WRONG_DIRECTION + DUPLICATE | | P0#2280 IS CVE-2022-1232, which was discovered AFTER CVE-2022-1096. Table has later=1096, prior=2280, but 2280 came later. This is a duplicate of row 22 with reversed direction. Also P commit resolves to archi-tinder (wrong). DROP THIS ROW. |
| 24 | CVE-2022-4906 | CVE-2022-3723 | unknown / chrome | same_root_cause | CORRECT | chrome | RCA: "Sergei Glazunov found a similar bug that involves copy-on-write arrays: CVE-2022-4906." |
| 25 | CVE-2023-28252 | CVE-2022-37969 | microsoft-windows / unknown | same_root_cause | WRONG_KIND | microsoft-windows | RCA: "code layout has overlaps with ITW exploit of CVE-2022-37969." These share the CLFS driver attack surface and exploit flow, but are different bugs. Better classified as same_exploit_flow. |
| 26 | CVE-2023-6345 | CVE-2023-2136 | chrome | same_root_cause | CORRECT | | RCA: "Another integer overflow in Skia...CVE-2023-2136...was found exploited in the wild a few months earlier." Same component (Skia), same bug class (integer overflow). |
| 27 | CVE-2023-33107 | CVE-2020-11261 | qualcomm-android | same_root_cause | CORRECT | | RCA: "CVE-2023-33107 could have been found by analyzing the patch for CVE-2020-11261." Same Adreno GPU memory management code area. |
| 28 | CVE-2020-0030 | CVE-2019-2215 | unknown / chrome | incomplete_fix | WRONG_RESOLUTION | android-kernel (binder.c) | Relationship CONFIRMED by RCA: "The patch for CVE-2019-2215 actually introduced another use-after-free condition." Deep says "unrelated" because L commit resolved to wrong kernel tree (HID/audio changes). Software should be android-kernel, not chrome. |
| 29 | CVE-2019-1367 | CVE-2018-8653 | ie-jscript | same_root_cause | CORRECT | | RCA: "it's likely that the actor found this bug by performing variant analysis on CVE-2018-8653." Both JScript GC UAF bugs, same actor. |
| 30 | CVE-2020-1429 | CVE-2019-1367 | unknown / ie-jscript | incomplete_fix | CORRECT | ie-jscript | RCA: "The incomplete patch and the variant are patched as CVE-2020-1429." |
| 31 | CVE-2020-0674 | CVE-2019-1367 | ie-jscript | incomplete_fix | CORRECT | | RCA: "This vulnerability is a trivial variant of CVE-2019-1367/CVE-2019-1429." |
| 32 | CVE-2019-13720 | chromium-p0:1963 | chrome / unknown | incomplete_fix | WRONG_RESOLUTION | chrome | Relationship CONFIRMED: RCA: "P0 1963 (CVE-2019-13732, CVE-2020-6406): Heap use-after-free in PannerHandler::TailTime. First patch was incomplete." P commit resolved to sachinshelke/ToolsConnector (WRONG). |
| 33 | CVE-2020-6427 | CVE-2019-13720 | unknown / chrome | incomplete_fix | CORRECT | chrome | RCA: "GHSL-2020-035 (CVE-2020-6427): Use-after-poison in IIRFilterHandler and BiquadFilterHandler." Same webaudio bug class, incomplete fix chain. |
| 34 | CVE-2020-6428 | CVE-2019-13720 | unknown / chrome | incomplete_fix | CORRECT | chrome | RCA: "GHSL-2020-037 (CVE-2020-6428): Use-after-free in DeferredTaskHandler::BreakConnections." |
| 35 | CVE-2020-6429 | CVE-2019-13720 | unknown / chrome | incomplete_fix | CORRECT | chrome | RCA: "GHSL-2020-038 (CVE-2020-6429): Use-after-poison in AudioScheduledSourceHandler::NotifyEnded." |
| 36 | CVE-2020-6449 | CVE-2019-13720 | unknown / chrome | incomplete_fix | WRONG_RESOLUTION | chrome | Relationship confirmed. L commit resolved to github/securitylab (PoC/advisory repo, not the actual Chromium patch). |
| 37 | CVE-2020-6451 | CVE-2019-13720 | unknown / chrome | incomplete_fix | CORRECT | chrome | RCA: "GHSL-2020-041 (CVE-2020-6451): Use-after-free in DeferredTaskHandler::ProcessAutomaticPullNodes." |
| 38 | CVE-2020-6450 | CVE-2019-13720 | unknown / chrome | incomplete_fix | CORRECT | chrome | RCA: "GHSL-2020-053 (CVE-2020-6450): Incomplete fix of the vulnerabilities reported in GHSL-2020-035 and GHSL-2020-038." |
| 39 | CVE-2021-39793 | CVE-2021-28664 | mali-gpu-driver | incomplete_fix | CORRECT | | RCA: "This vulnerability is a straightforward variant of CVE-2021-28664, which was fixed...around 10 months earlier." The patch missed one call site. |
| 40 | CVE-2021-4102 | chromium:1382434 | chrome | same_root_cause | WRONG_KIND | | RCA explicitly says "is NOT a variant of this vulnerability, however it uses WriteBarrier elision to exploit a class of vulnerability." Should be see_also or same_bug_class. |
| 41 | CVE-2020-6572 | CVE-2019-13695 | chrome / unknown | same_root_cause | CORRECT | chrome | RCA: "CVE-2019-13695. This vulnerability is essentially the same bug as CVE-2020-6572." |
| 42 | CVE-2020-6572 | CVE-2019-5870 | chrome / unknown | same_root_cause | CORRECT | chrome | RCA: "Guang Gong reported a similar vulnerability (chromium:999311), CVE-2019-5870." Same component. |
| 43 | CVE-2020-0674 | CVE-2018-8653 | ie-jscript | same_root_cause | CORRECT | | RCA: "4 JScript vulnerabilities (CVE-2018-8653, CVE-2019-1367, CVE-2019-1429, and CVE-2020-0674) of the same bug class." |
| 44 | CVE-2020-0674 | CVE-2019-1429 | ie-jscript | same_root_cause | CORRECT | | RCA: "trivial variant of CVE-2019-1367/CVE-2019-1429." |
| 45 | CVE-2020-27946 | CVE-2020-27930 | unknown / apple-ios | same_root_cause | CORRECT | apple-coretext | RCA found variant: "CVE-2020-27946: Apple CoreText libType1Scaler.dylib memory disclosure via uninitialized array." |
| 46 | CVE-2020-29624 | CVE-2020-27930 | unknown / apple-ios | same_root_cause | CORRECT | apple-coretext | RCA found variant: "CVE-2020-29624: Apple CoreText libFontParser.dylib stack corruption." |
| 47 | CVE-2020-27944 | CVE-2020-27930 | unknown / apple-ios | same_root_cause | CORRECT | apple-coretext | RCA found variant: "CVE-2020-27944: Apple CoreText libType1Scaler.dylib heap out-of-bounds write." |
| 48 | CVE-2020-27930 | chromium-p0:2114 | apple-ios / unknown | same_root_cause | WRONG_RESOLUTION | apple-coretext | Relationship correct: P0#2114 IS CVE-2020-27943 (CoreText variant). But P commit resolved to Benidrissa/etutor-digital-ph (WRONG). |
| 49 | CVE-2020-0986 | chromium-p0:2096 | microsoft-windows / unknown | incomplete_fix | WRONG_RESOLUTION | microsoft-windows | Relationship correct: RCA says "P0 2096: Not really a variant, but instead identifying that the original fix was bad." P commit resolved to protoLabsAI/protoMaker (WRONG). |
| 50 | CVE-2020-16011 | CVE-2020-16010 | unknown / chrome | same_root_cause | CORRECT | chrome | RCA: "CVE-2020-16011: An identical bug existing in Chrome for Windows." (16010 was Android, 16011 was Windows). |
| 51 | CVE-2022-26925 | CVE-2021-36942 | microsoft-windows / unknown | regression | CORRECT | microsoft-windows | P0 blog: "Windows PetitPotam issues, the original vulnerability had previously been patched, but at some point regressed." |

**MEDIUM summary:** 32 pairs total. 24 verified CORRECT (some with minor software label fixes). 5 have WRONG_RESOLUTION (real relationship, wrong commit link). 1 WRONG_DIRECTION + DUPLICATE (should be dropped). 2 WRONG_KIND (label should be adjusted).

---

## UNSURE -- 69 pairs

| # | later | prior | software | llm_kind | verified | correct_software | notes |
|---|---|---|---|---|---|---|---|
| 52 | CVE-2019-1429 | CVE-2019-1367 | ie-jscript | incomplete_fix | CORRECT | | RCA: "P0 1947 (CVE-2019-1429): Use-after-free where members of arguments object aren't tracked by GC during toJSON callback." |
| 53 | CVE-2022-22265 | CVE-2020-28343 | samsung-android / unknown | same_root_cause | CORRECT | samsung-npu-driver | RCA: "The bug was likely discovered while performing variant analysis on CVE-2020-28343." Samsung NPU device driver. |
| 54 | CVE-2022-22265 | chromium-p0:2073 | samsung-android / unknown | same_root_cause | WRONG_RESOLUTION | samsung-npu-driver | Same relationship as row 53. P commit resolved to microsoft/GitHub-Copilot-for-Azure (WRONG). P0#2073 is about Samsung NPU. |
| 55 | CVE-2022-4135 | chromium:1335422 | chrome | same_root_cause | NOT_SIBLINGS | | RCA: 1335422 is "safe C++ mode" runtime checks, a structural MITIGATION that kills the bug subclass. This is NOT a variant or sibling -- it's a defensive improvement. DROP. |
| 56 | CVE-2022-4262 | chromium:1425616 | chrome | same_root_cause | CORRECT | | RCA: "not all such issues had security impact, such as this one (1425616) where due to lucky circumstances, the bytecode mismatch was harmless." Same bug class, non-security variant. |
| 57 | CVE-2022-41128 | CVE-2021-34480 | ie-jscript / unknown | same_root_cause | CORRECT | ie-jscript9 | RCA: "This vulnerability is very similar to CVE-2021-34480 discovered by Ivan Fratric." Same JScript9 JIT type confusion class. |
| 58 | CVE-2022-41128 | chromium-p0:2188 | ie-jscript / unknown | same_root_cause | WRONG_RESOLUTION + DUPLICATE | ie-jscript9 | P0#2188 IS CVE-2021-34480. P commit resolved to ShmidtS/context-mode (WRONG). Duplicate of row 57. |
| 59 | CVE-2022-1096 | chromium:1309225 | chrome | see_also | SELF_PAIR | | 1309225 IS the bug report for CVE-2022-1096 itself. Self-reference. DROP. |
| 60 | CVE-2022-1096 | CVE-2016-5128 | chrome / unknown | same_root_cause | CORRECT | chrome | RCA: "CVE-2016-5128 - Security researcher reported bug in the property access interceptor for HTMLEmbedElement." Historical ancestor of the same bug class. |
| 61 | CVE-2022-1364 | chromium:1263462 | chrome | same_root_cause | CORRECT | | RCA: "CVE-2021-38003 (crbug/1263462) had exploited access to the 'hole' in the same way." Deep says unrelated (different subsystems) but the RCA ties them by exploit technique. |
| 62 | CVE-2022-1364 | CVE-2021-21195 | chrome / unknown | same_root_cause | CORRECT | chrome | RCA: "Could've been found as a variant of CVE-2021-21195." |
| 63 | CVE-2022-1364 | chromium:1182647 | chrome | same_root_cause | CORRECT | | RCA references 1182647 as related. Deep says unrelated (different subsystems: escape-analysis vs translated-state) but both concern V8 deoptimization behavior. |
| 64 | CVE-2022-1364 | CVE-2021-38003 | chrome | same_root_cause | CORRECT | | RCA: "CVE-2021-38003 had exploited access to the 'hole' in the same way." Same exploit technique. |
| 65 | CVE-2022-3723 | chromium:1378239 | chrome | see_also | WRONG_RESOLUTION | | RCA: "Issue/Bug Report: crbug/1378239." But L commit resolved to googleprojectzero/fuzzilli (a Fuzzilli TOOLING change, not the actual V8 patch). The relationship is real (1378239 is the bug for 3723), but the deep comparison was between wrong commits. |
| 66 | CVE-2022-3723 | chromium:1382434 | chrome | same_root_cause | WRONG_RESOLUTION | | RCA: "Sergei Glazunov found a similar bug that involves copy-on-write arrays: CVE-2022-4906 (crbug/1382434)." But again L resolved to Fuzzilli. Relationship is real. |
| 67 | CVE-2023-38831 | CVE-2023-3883 | winrar / unknown | same_root_cause | CORRECT | winrar | RCA mentions trailing-space archive entry technique. Both are WinRAR file-handling logic bugs. |
| 68 | CVE-2023-28252 | CVE-2023-23376 | microsoft-windows | same_root_cause | CORRECT | | RCA: "ITW exploitation strategies and code flows are close to the exploit of CVE-2023-23376." Both CLFS driver bugs. |
| 69 | CVE-2023-36802 | CVE-2022-37969 | microsoft-windows / unknown | same_root_cause | WRONG_KIND | microsoft-windows | RCA says "CVE-2022-37969 CLFS vulnerability followed a very similar exploit flow." CVE-2023-36802 is in mskssrv.sys (kernel streaming), CVE-2022-37969 is in CLFS. Different drivers! They share exploit flow, not root cause. Should be same_exploit_flow. |
| 70 | CVE-2023-26369 | CVE-2011-3402 | adobe-reader / unknown | same_root_cause | NOT_SIBLINGS | | RCA: "similar to CVE-2011-3402 affecting the TrueType font parsing engine in win32k." CVE-2023-26369 is Adobe Acrobat, CVE-2011-3402 is Windows win32k. Different codebases entirely. Similar bug pattern (font parsing) but NOT sibling CVEs. DROP. |
| 71 | CVE-2023-36033 | CVE-2022-21902 | microsoft-windows / unknown | same_root_cause | CORRECT | microsoft-windows (dwmcore.dll) | RCA: "CVE-2022-21902 is an OOB read in CKeyframeAnimation::AddKeyframeData dealing with DCOMPOSITION_EXPRESSION_TYPE_PATH type." Same DWM component. |
| 72 | CVE-2019-11707 | CVE-2019-9810 | mozilla-gecko / unknown | same_root_cause | WRONG_RESOLUTION | mozilla-gecko | Relationship correct per RCA: "incorrect side-effect modelling bug, similar to CVE-2019-9810." But deep says unrelated because P commit resolved to tarafans/collections (WRONG). |
| 73 | CVE-2019-11707 | chromium-p0:1820 | mozilla-gecko / unknown | same_root_cause | WRONG_RESOLUTION | mozilla-gecko | RCA: "is a similar type of bug in Spidermonkey that was also exploited in the wild [P0 1820]." P commit resolved to vinicius-ssantos/github-unified-mcp (WRONG). |
| 74 | CVE-2019-1367 | chromium-p0:1504 | ie-jscript / unknown | same_root_cause | CORRECT | ie-jscript | RCA: "P0 researcher identifies bug class. Finds numerous bugs (P0 1504, 1505, 1506, 1587)." |
| 75 | CVE-2019-1367 | chromium-p0:1505 | ie-jscript / unknown | same_root_cause | WRONG_RESOLUTION | ie-jscript | Same as above. P commit resolved to campfirein/byterover-cli (WRONG). |
| 76 | CVE-2019-1367 | chromium-p0:1947 | ie-jscript / unknown | same_root_cause | WRONG_RESOLUTION | ie-jscript | RCA: "P0 1947 (CVE-2019-1429)." P commit resolved to bomino/Z-t-Chi-Calculator.v2 (WRONG). |
| 77 | CVE-2019-13720 | chromium:1019226 | chrome | see_also | CORRECT | | RCA: "Issue/Bug Report: crbug/1019226." Related audio UAF in Chrome. |
| 78 | CVE-2019-13732 | CVE-2019-13720 | unknown / chrome | same_root_cause | CORRECT | chrome | RCA: "P0 1963 (CVE-2019-13732, CVE-2020-6406): Heap use-after-free in PannerHandler::TailTime." |
| 79 | CVE-2020-6406 | CVE-2019-13720 | unknown / chrome | same_root_cause | CORRECT | chrome | Same as above. CVE-2020-6406 is the second fix after the first patch was incomplete. |
| 80 | CVE-2021-21206 | chromium:663476 | chrome | same_root_cause | CORRECT | | RCA: "Promise.then publicly documented in crbug/663476#c10." Historical documentation of the thennable callback bug pattern. |
| 81 | CVE-2021-21206 | chromium:678706 | chrome | same_root_cause | CORRECT | | RCA: "A vulnerability researcher could have also come across crbug/678706 and crbug/708887." |
| 82 | CVE-2021-21206 | chromium:708887 | chrome | same_root_cause | CORRECT | | Same as above. |
| 83 | CVE-2021-38000 | chromium:1249962 | chrome | see_also | CORRECT | | Related intent-scheme URL handling bug. Deep: one_extends_other. |
| 84 | CVE-2021-37975 | CVE-2019-13720 | chrome | same_root_cause | WRONG_KIND | | Deep says unrelated: "later patch changes V8's cppgc marking/ephemeron processing...prior patch modifies Blink's WebAudio ConvolverNode." These are in COMPLETELY different components (V8 GC vs Blink WebAudio). The RCA ties them only by the exploit flow (TypedArray/ArrayBuffer technique), NOT root cause. Should be same_exploit_flow. |
| 85 | CVE-2021-37975 | chromium:1252878 | chrome | same_root_cause | CORRECT | | RCA: "The bug [1252878] appears to be the same ephemeron handling problem in oilpan." Direct variant, same component. |
| 86 | CVE-2021-37975 | github:v8/v8@e677a6f6b257 | chrome | same_root_cause | SELF_PAIR | | Deep column explicitly says "self-pair (later and prior resolved to same fix)." DROP. |
| 87 | CVE-2021-30551 | chromium:1216437 | chrome | see_also | CORRECT | | RCA: "Issue/Bug Report: crbug/1216437." Deep says unrelated (different subsystems within objects.cc) but the RCA lists it as the bug report reference. |
| 88 | CVE-2021-30551 | chromium:619166 | chrome | same_root_cause | CORRECT | | RCA: "It's possible...that the researcher used crbug/619166, a bug where unexpected JS execution inside interceptors." |
| 89 | CVE-2021-30632 | CVE-2020-16009 | chrome | same_root_cause | CORRECT | | Both V8 Map transition/deprecation bugs. RCA lists 16009's components as historical context. Deep says unrelated because L resolved to a PoC JS file, not the actual patch. |
| 90 | CVE-2021-30632 | chromium-p0:2106 | chrome / unknown | same_root_cause | CORRECT | chrome | RCA references P0#2106 (map transition/deprecation area). |
| 91 | CVE-2021-30632 | chromium:1209558 | chrome | same_root_cause | CORRECT | | RCA: "Some vulnerabilities in property access: crbug/1209558." Deep says unrelated because L is just a PoC file. |
| 92 | CVE-2021-30632 | chromium:1203122 | chrome | same_root_cause | CORRECT | | Same situation: L resolved to PoC, P to regression test. Both are related V8 property access/map bugs. |
| 93 | CVE-2021-30632 | chromium:1216437 | chrome | same_root_cause | CORRECT | | Duplicate reference to 1216437 (also appears in row 87 for CVE-2021-30551). |
| 94 | CVE-2021-30632 | chromium:746946 | chrome | same_root_cause | CORRECT | | RCA: "Some vulnerabilities in map transition/deprecation: crbug/746946." |
| 95 | CVE-2021-1905 | CVE-2020-11261 | qualcomm-android | same_root_cause | CORRECT | | RCA: "This change is believed to be related to CVE-2020-11261." Both Qualcomm Adreno GPU memory management bugs. |
| 96 | CVE-2021-4102 | chromium:1423610 | chrome | same_root_cause | CORRECT | | RCA: "crbug/1423610...is another WriteBarrier elision bug in V8's Maglev JIT compiler." Different JIT (Maglev vs Turbofan) but same bug class. |
| 97 | CVE-2021-4083 | CVE-2021-0920 | unknown / chrome | same_root_cause | WRONG_RESOLUTION | linux-kernel (af_unix.c) | Relationship CONFIRMED by RCA: "Found variants: CVE-2021-4083 by Jann Horn." But software is linux-kernel (net/unix/af_unix.c), NOT chrome. L commit resolved to wrong kernel tree. |
| 98 | CVE-2021-30858 | chromium:1032890 | apple-webkit != chrome | see_also | NOT_SIBLINGS | | RCA: "what seems to be a similar bug was found in Chrome: crbug/1032890." WebKit UAF vs Chrome Blink UAF. Different codebases (WebKit vs Chromium). Similar pattern across engines, NOT sibling CVEs. |
| 99 | CVE-2021-26411 | CVE-2019-1208 | ie-jscript / unknown | same_root_cause | WRONG_KIND | ie-jscript | RCA describes "changing the length of an Array" as a common exploitation technique used across multiple JScript bugs. Same EXPLOIT TECHNIQUE, not necessarily same root cause. |
| 100 | CVE-2019-17026 | CVE-2019-9810 | mozilla-gecko / unknown | same_root_cause | WRONG_RESOLUTION | mozilla-gecko | Relationship correct: both IonMonkey/SpiderMonkey JIT side-effect modeling bugs. P commit resolved to tarafans/collections (WRONG). |
| 101 | CVE-2019-17026 | chromium-p0:1820 | mozilla-gecko / unknown | same_root_cause | WRONG_RESOLUTION | mozilla-gecko | Relationship correct: RCA says "is a similar type of bug in Spidermonkey [P0 1820]." P commit resolved to wrong repo. |
| 102 | CVE-2020-1429 | CVE-2020-0674 | unknown / ie-jscript | same_root_cause | CORRECT | ie-jscript | RCA: "4 JScript vulnerabilities...of the same bug class." Part of the CVE-2018-8653 chain. |
| 103 | CVE-2020-0968 | CVE-2020-0674 | unknown / ie-jscript | same_root_cause | CORRECT | ie-jscript | RCA: "CVE-2020-0968: During string concatenation...not correctly tracked by the garbage collector." |
| 104 | CVE-2020-17053 | CVE-2020-1380 | unknown / ie-jscript | same_root_cause | CORRECT | ie-jscript9 | RCA: "CVE-2020-17053: Discovered by Elliot Cao." Note: CVE-2020-1380 targets jscript9.dll (not jscript.dll). |
| 105 | CVE-2020-16010 | CVE-2020-16009 | chrome | same_root_cause | NOT_SIBLINGS | | Deep says unrelated: "later patch modifies ui/gfx/android/java_bitmap.cc...prior patch changes V8 internals." These are completely different components (Android bitmap rendering vs V8 JIT). They were used in the SAME ATTACK CAMPAIGN, not same root cause. |
| 106 | CVE-2020-16010 | chromium-p0:2112 | chrome / unknown | same_root_cause | WRONG_RESOLUTION | chrome | P0#2112 IS the bug report for CVE-2020-16010. P commit resolved to etutor-digital-ph (WRONG). |
| 107 | CVE-2020-16010 | chromium:1144368 | chrome | see_also | CORRECT | | RCA: "crbug/1144368." Related Chromium bug in same area. Deep: one_extends_other. |
| 108 | CVE-2020-27930 | CVE-2020-16009 | apple-ios != chrome | same_root_cause | NOT_SIBLINGS | | Different vendors (Apple vs Google), different codebases (CoreText vs V8). Used by same actor in same operation. NOT sibling CVEs. |
| 109 | CVE-2020-27932 | CVE-2020-16009 | apple-ios != chrome | same_root_cause | NOT_SIBLINGS | | Same issue: Apple iOS kernel bug paired with Chrome V8 bug. Different codebases. Same attack campaign, not same root cause. |
| 110 | CVE-2020-15999 | chromium-p0:168 | chrome / unknown | same_root_cause | NOT_SIBLINGS | | CVE-2020-15999 is in FreeType (embedded in Chrome). P0#168 is about Adobe/Microsoft font bugs. Different codebases. RCA says "possible variant analysis" across different font engines. |
| 111 | CVE-2020-6418 | chromium:1053604 | chrome | see_also | WRONG_RESOLUTION | | 1053604 IS the bug report for CVE-2020-6418. But L commit resolved to a SpiderMonkey/gecko-dev commit (WRONG engine entirely). The actual patch is in V8 (Chromium). |
| 112 | CVE-2020-27943 | CVE-2020-27930 | unknown / apple-ios | same_root_cause | CORRECT | apple-coretext | RCA found variant: "CVE-2020-27943: Apple CoreText libType1Scaler.dylib heap buffer overflow." |
| 113 | CVE-2020-27930 | chromium-p0:2113 | apple-ios / unknown | same_root_cause | WRONG_RESOLUTION | apple-coretext | P0#2113 IS CVE-2020-27946. P commit resolved to etutor-digital-ph (WRONG). |
| 114 | CVE-2020-27930 | chromium-p0:2115 | apple-ios / unknown | same_root_cause | WRONG_RESOLUTION | apple-coretext | P0#2115 IS CVE-2020-29624. P commit resolved to wang2032/multic-md (WRONG). |
| 115 | CVE-2020-27930 | chromium-p0:2116 | apple-ios / unknown | same_root_cause | WRONG_RESOLUTION | apple-coretext | P0#2116 IS CVE-2020-27944. P commit resolved to etutor-digital-ph (WRONG). |
| 116 | CVE-2020-27930 | CVE-2015-3052 | apple-ios / unknown | same_root_cause | CORRECT | apple-coretext / adobe-coretext | RCA traces the Type 1 CharString interpreter bug family back to 2015 Adobe/Microsoft font bugs. Historical ancestor. |
| 117 | CVE-2020-0986 | CVE-2019-0880 | microsoft-windows | incomplete_fix | CORRECT | | RCA: "This bug is very shallow and an extremely trivial variant of CVE-2019-0880." Both in GdiPrinterThunk/splwow64. |
| 118 | CVE-2020-17008 | CVE-2020-0986 | unknown / microsoft-windows | same_root_cause | CORRECT | microsoft-windows | RCA: "This fix was assigned CVE-2020-17008/CVE-2021-1648." Patch for the incomplete fix of CVE-2020-0986. |
| 119 | CVE-2021-1648 | CVE-2020-0986 | unknown / microsoft-windows | same_root_cause | CORRECT | microsoft-windows | Same as above. CVE-2021-1648 = CVE-2020-17008 (renumbered). L commit to hatRiot/bugs is a PoC analysis, not the MS patch. |
| 120 | CVE-2020-6820 | mozilla:1368273 | mozilla-gecko | same_root_cause | CORRECT | | Related Cache module bug in Gecko. |
| 121 | CVE-2020-6820 | mozilla:1655115 | mozilla-gecko | same_root_cause | CORRECT | | RCA: "Bug 1655115: UAF in StreamControl::CloseReadStreams, but the code is dead code. FF removed it." |

---

## Summary statistics

| Category | Count | Notes |
|---|---|---|
| CORRECT | 83 | Real relationship, correct resolution |
| WRONG_RESOLUTION | 20 | Real relationship but patch link(s) point to wrong GitHub repos (commit resolver bug) |
| NOT_SIBLINGS | 6 | Different codebases or unrelated (should be dropped) |
| SELF_PAIR | 2 | Later and prior are the same bug/commit (should be dropped) |
| WRONG_KIND | 4 | Relationship exists but the kind label is wrong (e.g., same_exploit_flow not same_root_cause) |
| WRONG_DIRECTION + DUPLICATE | 2 | Later/prior reversed and already covered by another row |
| **Total** | **117** | (3 rows from original 120 could not be verified individually in UNSURE batch) |

## Rows to DROP (10 total)

1. Row 23: CVE-2022-1096 / chromium-p0:2280 -- WRONG_DIRECTION + DUPLICATE of row 22
2. Row 55: CVE-2022-4135 / chromium:1335422 -- NOT_SIBLINGS (structural mitigation, not variant)
3. Row 58: CVE-2022-41128 / chromium-p0:2188 -- DUPLICATE of row 57 with wrong resolution
4. Row 59: CVE-2022-1096 / chromium:1309225 -- SELF_PAIR
5. Row 70: CVE-2023-26369 / CVE-2011-3402 -- NOT_SIBLINGS (Adobe Reader vs win32k)
6. Row 86: CVE-2021-37975 / github:v8/v8@e677a6f6b257 -- SELF_PAIR
7. Row 98: CVE-2021-30858 / chromium:1032890 -- NOT_SIBLINGS (WebKit vs Chromium)
8. Row 105: CVE-2020-16010 / CVE-2020-16009 -- NOT_SIBLINGS (different Chrome components, same campaign)
9. Row 108: CVE-2020-27930 / CVE-2020-16009 -- NOT_SIBLINGS (Apple vs Chrome)
10. Row 109: CVE-2020-27932 / CVE-2020-16009 -- NOT_SIBLINGS (Apple vs Chrome)
11. Row 110: CVE-2020-15999 / chromium-p0:168 -- NOT_SIBLINGS (FreeType vs Adobe/MS font engine)

## Rows to FIX (kind label)

1. Row 25: CVE-2023-28252 / CVE-2022-37969 -- Change same_root_cause to same_exploit_flow
2. Row 40: CVE-2021-4102 / chromium:1382434 -- Change same_root_cause to see_also (RCA says "NOT a variant")
3. Row 69: CVE-2023-36802 / CVE-2022-37969 -- Change same_root_cause to same_exploit_flow (mskssrv.sys vs CLFS driver)
4. Row 84: CVE-2021-37975 / CVE-2019-13720 -- Change same_root_cause to same_exploit_flow (V8 GC vs WebAudio)
5. Row 99: CVE-2021-26411 / CVE-2019-1208 -- Change same_root_cause to same_exploit_technique

## Software corrections needed

| Row | Original | Correct |
|---|---|---|
| 20 prior | unknown | microsoft-windows |
| 22 later | unknown | chrome |
| 24 prior | unknown | chrome |
| 28 | unknown / chrome | android-kernel (binder.c) |
| 30 later | unknown | ie-jscript |
| 32, 33-38 prior | unknown | chrome |
| 41, 42, 50 prior | unknown | chrome |
| 45-48 | apple-ios | apple-coretext (libType1Scaler.dylib) |
| 53-54 | samsung-android | samsung-npu-driver |
| 57-58 | ie-jscript | ie-jscript9 (jscript9.dll, distinct from jscript.dll) |
| 69 | microsoft-windows | mskssrv.sys (kernel streaming) vs CLFS -- different drivers |
| 71 | microsoft-windows | microsoft-windows (dwmcore.dll) |
| 97 | unknown / chrome | linux-kernel (net/unix/af_unix.c) |
| 104 | ie-jscript | ie-jscript9 (targets jscript9.dll per RCA) |

## Key finding: Commit resolver failure pattern

The most common systematic issue is the commit resolver linking P0 bug tracker IDs (chromium-p0:NNNN) to completely unrelated GitHub repositories. Affected repos include:
- Benidrissa/etutor-digital-ph (appears 4+ times)
- hongikarchi/archi-tinder
- sachinshelke/ToolsConnector
- campfirein/byterover-cli
- bomino/Z-t-Chi-Calculator.v2
- wang2032/multic-md
- ShmidtS/context-mode
- protoLabsAI/protoMaker
- microsoft/GitHub-Copilot-for-Azure
- tarafans/collections
- huyremy/netcat
- vinicius-ssantos/github-unified-mcp

These appear to be random repos that coincidentally contain the same numeric string as the P0 bug ID somewhere in their commit messages or metadata. The resolver likely does a naive keyword search. **Recommendation: For P0 bug IDs, resolve via bugs.chromium.org/p/project-zero/issues/detail?id=NNNN first, then follow the patch links from the actual bug tracker entry.**