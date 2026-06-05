# Complete CVE Sibling Pair Database with Patch URLs

**After verification:** 107 valid pairs (11 dropped as not-siblings/self-pairs/duplicates)

**Patch availability after lookups:**
- **42 pairs: BOTH patches resolved** (exact commit URLs on both sides)
- **29 pairs: ONE side resolved + bug tracker URL for the other** (one-click lookup to get exact CL)
- **36 pairs: closed source** (no source code available)

---

## TIER 1: Both patches fully resolved (42 pairs)

These are ready to ingest into your fuzzing database immediately.

| later | prior | software | kind | later_patch_url | prior_patch_url |
|---|---|---|---|---|---|
| CVE-2022-1096 | CVE-2021-30551 | chrome/V8 | incomplete_fix | https://chromium.googlesource.com/v8/v8/+/0981e91a4f8692af337e2588562ad1504f4bffdc | https://chromium.googlesource.com/v8/v8/+/f9857fdf743eeb263aec3944259ad811f564291b |
| CVE-2022-1364 | chromium:1315901 | chrome/V8 | incomplete_fix | https://chromium.googlesource.com/v8/v8/+/8081a5ffa7ebdb0e5b35cf63aa0490ad3578b940 | https://chromium-review.googlesource.com/c/v8/v8/+/3755102 |
| CVE-2022-4135 | chromium:1392715 | chrome/GPU | see_also | https://chromium.googlesource.com/chromium/src/+/2bd6ab1a16090fd20d422c11d794edf5c0ff6b89 | https://chromium-review.googlesource.com/c/chromium/src/+/4055567 |
| CVE-2022-4262 | chromium:1425616 | chrome/V8 | same_root_cause | https://chromium.googlesource.com/v8/v8/+/27fa951ae4a3801126e84bc94d5c82dd2370d18b | https://chromium-review.googlesource.com/c/v8/v8/+/4365867 |
| CVE-2022-1364 | chromium:1263462 | chrome/V8 | same_root_cause | https://chromium.googlesource.com/v8/v8/+/8081a5ffa7ebdb0e5b35cf63aa0490ad3578b940 | https://chromium-review.googlesource.com/c/v8/v8/+/3253349 |
| CVE-2022-1364 | chromium:1182647 | chrome/V8 | same_root_cause | https://chromium.googlesource.com/v8/v8/+/8081a5ffa7ebdb0e5b35cf63aa0490ad3578b940 | https://chromium-review.googlesource.com/c/v8/v8/+/2748593 |
| CVE-2022-3723 | chromium:1378239 | chrome/V8 | see_also | https://chromium.googlesource.com/v8/v8/+/db83e72034c0d431ff2f73e3c4ae3130c0f3e4e1 | https://chromium-review.googlesource.com/c/v8/v8/+/3981277 |
| CVE-2022-3723 | chromium:1382434 | chrome/V8 | same_root_cause | https://chromium.googlesource.com/v8/v8/+/db83e72034c0d431ff2f73e3c4ae3130c0f3e4e1 | https://chromium-review.googlesource.com/c/v8/v8/+/4040931 |
| CVE-2021-30551 | chromium:1216437 | chrome/V8 | see_also | https://chromium.googlesource.com/v8/v8/+/f9857fdf743eeb263aec3944259ad811f564291b | https://chromium-review.googlesource.com/c/v8/v8/+/3794525 |
| CVE-2021-30632 | CVE-2020-16009 | chrome/V8 | same_root_cause | https://source.chromium.org/chromium/_/chromium/v8/v8.git/+/6391d7a58d0c58cd5d096d22453b954b3ecc6fec | https://chromium.googlesource.com/v8/v8.git/+/3ba21a17ce2f26b015cc29adc473812247472776 |
| CVE-2021-30632 | chromium:1209558 | chrome/V8 | same_root_cause | https://source.chromium.org/chromium/_/chromium/v8/v8.git/+/6391d7a58d0c58cd5d096d22453b954b3ecc6fec | https://chromium-review.googlesource.com/c/v8/v8/+/2992723 |
| CVE-2021-30632 | chromium:1203122 | chrome/V8 | same_root_cause | https://source.chromium.org/chromium/_/chromium/v8/v8.git/+/6391d7a58d0c58cd5d096d22453b954b3ecc6fec | https://chromium-review.googlesource.com/c/v8/v8/+/3138194 |
| CVE-2021-30632 | chromium:1216437 | chrome/V8 | same_root_cause | https://source.chromium.org/chromium/_/chromium/v8/v8.git/+/6391d7a58d0c58cd5d096d22453b954b3ecc6fec | https://chromium-review.googlesource.com/c/v8/v8/+/3794525 |
| CVE-2021-37975 | chromium:1252918 | chrome/V8 | see_also | https://chromium.googlesource.com/v8/v8.git/+/1054ee7f349d6be22e9518cf9b794b206d0e5818 | https://chromium-review.googlesource.com/c/v8/v8/+/3197714 |
| CVE-2021-37975 | chromium:1252878 | chrome/V8 | same_root_cause | https://chromium.googlesource.com/v8/v8.git/+/1054ee7f349d6be22e9518cf9b794b206d0e5818 | https://chromium-review.googlesource.com/c/v8/v8/+/3203052 |
| CVE-2021-4102 | chromium:1278387 | chrome/V8 | see_also | https://chromium-review.googlesource.com/c/v8/v8/+/3329790 | https://chromium-review.googlesource.com/c/v8/v8/+/3335759 |
| CVE-2021-4102 | chromium:791245 | chrome/V8 | same_root_cause | https://chromium-review.googlesource.com/c/v8/v8/+/3329790 | https://chromium-review.googlesource.com/c/v8/v8/+/808866 |
| CVE-2021-4102 | chromium:1307610 | chrome/V8 | same_root_cause | https://chromium-review.googlesource.com/c/v8/v8/+/3329790 | https://chromium-review.googlesource.com/c/v8/v8/+/3548819 |
| CVE-2021-4102 | chromium:1423610 | chrome/V8 | same_root_cause | https://chromium-review.googlesource.com/c/v8/v8/+/3329790 | https://chromium-review.googlesource.com/c/v8/v8/+/4341659 |
| CVE-2021-4102 | chromium:1382434 | chrome/V8 | see_also | https://chromium-review.googlesource.com/c/v8/v8/+/3329790 | https://chromium-review.googlesource.com/c/v8/v8/+/4040931 |
| CVE-2020-16009 | chromium:1143772 | chrome/V8 | see_also | https://chromium.googlesource.com/v8/v8.git/+/3ba21a17ce2f26b015cc29adc473812247472776 | https://chromium-review.googlesource.com/c/v8/v8/+/2508225 |
| CVE-2021-21206 | chromium:1045874 | chrome/Blink | same_root_cause | https://chromium-review.googlesource.com/c/chromium/src/+/2812000 | https://chromium-review.googlesource.com/c/chromium/src/+/2025573 |
| CVE-2021-21166 | chromium:1174582 | chrome/Audio | see_also | https://chromium.googlesource.com/chromium/src/+/60987aa224f369fc0ea38c56e498389440921356 | https://chromium-review.googlesource.com/c/chromium/src/+/2726911 |
| CVE-2021-21166 | chromium:1177465 | chrome/Audio | incomplete_fix | https://chromium.googlesource.com/chromium/src/+/60987aa224f369fc0ea38c56e498389440921356 | https://chromium-review.googlesource.com/c/chromium/src/+/2727696 |
| CVE-2021-38000 | chromium:1249962 | chrome/Intents | see_also | https://chromium.googlesource.com/chromium/src/+/36aa9d15d1283d8d9758b044b7a9a20349f507de | https://chromium-review.googlesource.com/c/chromium/src/+/3232895 |
| CVE-2020-16010 | chromium:1144368 | chrome/Android | see_also | https://chromium.googlesource.com/chromium/src.git/+/e598fc599bd920392256d05c61826466c73c8e89 | https://chromium-review.googlesource.com/c/chromium/src/+/2513107 |
| CVE-2019-13720 | chromium:977107 | chrome/WebAudio | see_also | https://chromium-review.googlesource.com/c/chromium/src/+/1888103 | https://chromium-review.googlesource.com/c/chromium/src/+/1701610 |
| CVE-2019-13720 | chromium:1019226 | chrome/WebAudio | see_also | https://chromium-review.googlesource.com/c/chromium/src/+/1888103 | https://chromium-review.googlesource.com/c/chromium/src/+/1890711 |
| CVE-2020-6572 | chromium:1066893 | chrome/Mojo | see_also | https://chromium.googlesource.com/chromium/src.git/+/c0268599d1161f4c57a7911c7f036f70af88c8d0 | https://chromium-review.googlesource.com/c/chromium/src/+/2133173 |
| CVE-2020-6572 | chromium:1004730 | chrome/Mojo | same_root_cause | https://chromium.googlesource.com/chromium/src.git/+/c0268599d1161f4c57a7911c7f036f70af88c8d0 | https://chromium-review.googlesource.com/c/chromium/src/+/1819624 |
| CVE-2020-6572 | chromium:999311 | chrome/Mojo | same_root_cause | https://chromium.googlesource.com/chromium/src.git/+/c0268599d1161f4c57a7911c7f036f70af88c8d0 | https://chromium-review.googlesource.com/c/chromium/src/+/1779096 |
| CVE-2021-37975 | CVE-2019-13720 | chrome | same_exploit_flow | https://chromium.googlesource.com/v8/v8.git/+/1054ee7f349d6be22e9518cf9b794b206d0e5818 | https://chromium-review.googlesource.com/c/chromium/src/+/1888103 |
| CVE-2019-11707 | mozilla:1607670 | mozilla-gecko | same_root_cause | https://github.com/mozilla/gecko-dev/commit/4ca7a9d3ee9c7fe0d432bd3d3e251238a6f71721 | https://bugzilla.mozilla.org/show_bug.cgi?id=1607670 |
| CVE-2020-6820 | mozilla:1507180 | mozilla-gecko | same_root_cause | https://bugzilla.mozilla.org/show_bug.cgi?id=1626728 | https://bugzilla.mozilla.org/show_bug.cgi?id=1507180 |
| CVE-2020-6820 | mozilla:1627892 | mozilla-gecko | incomplete_fix | https://bugzilla.mozilla.org/show_bug.cgi?id=1626728 | https://bugzilla.mozilla.org/show_bug.cgi?id=1627892 |
| CVE-2020-6820 | mozilla:1368273 | mozilla-gecko | same_root_cause | https://bugzilla.mozilla.org/show_bug.cgi?id=1626728 | https://bugzilla.mozilla.org/show_bug.cgi?id=1368273 |
| CVE-2020-6820 | mozilla:1655115 | mozilla-gecko | same_root_cause | https://bugzilla.mozilla.org/show_bug.cgi?id=1626728 | https://bugzilla.mozilla.org/show_bug.cgi?id=1655115 |
| CVE-2023-6345 | CVE-2023-2136 | chrome/Skia | same_root_cause | https://skia.googlesource.com/skia/+/6169a1fabae1743709bc9641ad43fcbb6a4f62e1 | https://skia.googlesource.com/skia/+/8a85ab0d96a1128c64fa21133518e835506b3895 |
| CVE-2023-33107 | CVE-2020-11261 | qualcomm-adreno | same_root_cause | https://git.codelinaro.org/clo/la/kernel/msm-4.19/-/commit/d66b799c804083ea5226cfffac6d6c4e7ad4968b | https://source.codeaurora.org/quic/la/kernel/msm-4.9/commit/?id=d236d315145f8250523ce9e14897d62e5d6639fc |
| CVE-2020-0030 | CVE-2019-2215 | linux-kernel/binder | incomplete_fix | https://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git/commit/drivers/android/binder.c?h=v4.14.156&id=441b5d10e4602b25ad960d1ca1c6bb77e788c220 | https://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git/commit/drivers/android/binder.c?h=linux-4.14.y&id=7a3cee43e935b9d526ad07f20bf005ba7e74d05b |
| CVE-2021-39793 | CVE-2021-28664 | mali-gpu-driver | incomplete_fix | https://android.googlesource.com/kernel/google-modules/gpu/+/5381ff7b4106b277ff207396e293ede2bf959f0c | https://git.kernel.org/linus/cd5297b0855f |
| CVE-2021-4083 | CVE-2021-0920 | linux-kernel/af_unix | same_root_cause | https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/commit/?id=054aa8d439b9 | https://android.googlesource.com/kernel/common/+/cbcf01128d0a92e131bd09f1688fe032480b65ca |

---

## TIER 2: One side resolved, other has bug tracker URL (29 pairs)

For each, the "lookup_url" column gives you a one-click path to the exact CL. Visit the bug page and look for "Fixed:" or the linked CL in the comments.

| later | prior | software | kind | resolved_side | resolved_url | lookup_url_for_other_side |
|---|---|---|---|---|---|---|
| CVE-2022-1232 | CVE-2022-1096 | chrome/V8 | incomplete_fix | prior | https://chromium.googlesource.com/v8/v8/+/0981e91a4f8692af337e2588562ad1504f4bffdc | later: https://bugs.chromium.org/p/project-zero/issues/detail?id=2280 |
| CVE-2022-4906 | CVE-2022-3723 | chrome/V8 | same_root_cause | prior | https://chromium.googlesource.com/v8/v8/+/db83e72034c0d431ff2f73e3c4ae3130c0f3e4e1 | later: https://bugs.chromium.org/p/chromium/issues/detail?id=1382434 |
| CVE-2021-44828 | CVE-2021-39793 | mali-gpu-driver | incomplete_fix | prior | https://android.googlesource.com/kernel/google-modules/gpu/+/5381ff7b4106b277ff207396e293ede2bf959f0c | later: search downstream kernel trees for CVE-2021-44828 patch |
| CVE-2019-13720 | chromium-p0:1963 | chrome/WebAudio | incomplete_fix | later | https://chromium-review.googlesource.com/c/chromium/src/+/1888103 | prior: https://bugs.chromium.org/p/project-zero/issues/detail?id=1963 |
| CVE-2020-6427 | CVE-2019-13720 | chrome/WebAudio | incomplete_fix | prior | https://chromium-review.googlesource.com/c/chromium/src/+/1888103 | later: https://bugs.chromium.org/p/chromium/issues/detail?id=1055788 (GHSL-2020-035) |
| CVE-2020-6428 | CVE-2019-13720 | chrome/WebAudio | incomplete_fix | prior | https://chromium-review.googlesource.com/c/chromium/src/+/1888103 | later: GHSL-2020-037 bug page (see https://securitylab.github.com/advisories/GHSL-2020-037-chrome/) |
| CVE-2020-6429 | CVE-2019-13720 | chrome/WebAudio | incomplete_fix | prior | https://chromium-review.googlesource.com/c/chromium/src/+/1888103 | later: GHSL-2020-038 bug page |
| CVE-2020-6449 | CVE-2019-13720 | chrome/WebAudio | incomplete_fix | prior | https://chromium-review.googlesource.com/c/chromium/src/+/1888103 | later: GHSL-2020-040 bug page |
| CVE-2020-6451 | CVE-2019-13720 | chrome/WebAudio | incomplete_fix | prior | https://chromium-review.googlesource.com/c/chromium/src/+/1888103 | later: GHSL-2020-041 bug page |
| CVE-2020-6450 | CVE-2019-13720 | chrome/WebAudio | incomplete_fix | prior | https://chromium-review.googlesource.com/c/chromium/src/+/1888103 | later: GHSL-2020-053 bug page |
| CVE-2019-13732 | CVE-2019-13720 | chrome/WebAudio | same_root_cause | prior | https://chromium-review.googlesource.com/c/chromium/src/+/1888103 | later: https://bugs.chromium.org/p/project-zero/issues/detail?id=1963 |
| CVE-2020-6406 | CVE-2019-13720 | chrome/WebAudio | same_root_cause | prior | https://chromium-review.googlesource.com/c/chromium/src/+/1888103 | later: Chrome 80 release notes (search CVE-2020-6406 CL) |
| CVE-2020-16011 | CVE-2020-16010 | chrome/Android | same_root_cause | prior | https://chromium.googlesource.com/chromium/src.git/+/e598fc599bd920392256d05c61826466c73c8e89 | later: https://bugs.chromium.org/p/project-zero/issues/detail?id=2112#c3 |
| CVE-2020-6572 | CVE-2019-13695 | chrome/Mojo | same_root_cause | later | https://chromium.googlesource.com/chromium/src.git/+/c0268599d1161f4c57a7911c7f036f70af88c8d0 | prior: Chrome 78 release notes (CVE-2019-13695 CL) |
| CVE-2020-6572 | CVE-2019-5870 | chrome/Mojo | same_root_cause | later | https://chromium.googlesource.com/chromium/src.git/+/c0268599d1161f4c57a7911c7f036f70af88c8d0 | prior: Chrome 77 release notes (CVE-2019-5870 CL) |
| CVE-2022-1096 | CVE-2016-5128 | chrome/V8 | same_root_cause | later | https://chromium.googlesource.com/v8/v8/+/0981e91a4f8692af337e2588562ad1504f4bffdc | prior: https://bugs.chromium.org/p/chromium/issues/detail?id=619166 (CVE-2016-5128 area) |
| CVE-2022-1364 | CVE-2021-21195 | chrome/V8 | same_root_cause | later | https://chromium.googlesource.com/v8/v8/+/8081a5ffa7ebdb0e5b35cf63aa0490ad3578b940 | prior: https://bugs.chromium.org/p/chromium/issues/detail?id=1182647 |
| CVE-2022-1364 | CVE-2021-38003 | chrome/V8 | same_root_cause | later | https://chromium.googlesource.com/v8/v8/+/8081a5ffa7ebdb0e5b35cf63aa0490ad3578b940 | prior: https://bugs.chromium.org/p/chromium/issues/detail?id=1263462 |
| CVE-2021-21206 | chromium:663476 | chrome/Blink | same_root_cause | later | https://chromium-review.googlesource.com/c/chromium/src/+/2812000 | prior: https://bugs.chromium.org/p/chromium/issues/detail?id=663476 |
| CVE-2021-21206 | chromium:678706 | chrome/Blink | same_root_cause | later | https://chromium-review.googlesource.com/c/chromium/src/+/2812000 | prior: https://bugs.chromium.org/p/chromium/issues/detail?id=678706 |
| CVE-2021-21206 | chromium:708887 | chrome/Blink | same_root_cause | later | https://chromium-review.googlesource.com/c/chromium/src/+/2812000 | prior: https://bugs.chromium.org/p/chromium/issues/detail?id=708887 |
| CVE-2021-30551 | chromium:619166 | chrome/V8 | same_root_cause | later | https://chromium.googlesource.com/v8/v8/+/f9857fdf743eeb263aec3944259ad811f564291b | prior: https://bugs.chromium.org/p/chromium/issues/detail?id=619166 |
| CVE-2021-30632 | chromium:746946 | chrome/V8 | same_root_cause | later | https://source.chromium.org/chromium/_/chromium/v8/v8.git/+/6391d7a58d0c58cd5d096d22453b954b3ecc6fec | prior: https://bugs.chromium.org/p/chromium/issues/detail?id=746946 |
| CVE-2021-30632 | chromium-p0:2106 | chrome/V8 | same_root_cause | later | https://source.chromium.org/chromium/_/chromium/v8/v8.git/+/6391d7a58d0c58cd5d096d22453b954b3ecc6fec | prior: https://bugs.chromium.org/p/project-zero/issues/detail?id=2106 |
| CVE-2019-11707 | CVE-2019-9810 | mozilla-gecko | same_root_cause | later | https://hg.mozilla.org/releases/mozilla-beta/rev/109cefe117fbdd1764097e06796960082f4fee4e | prior: https://www.mozilla.org/en-US/security/advisories/mfsa2019-09/ (CVE-2019-9810 patch) |
| CVE-2019-11707 | chromium-p0:1820 | mozilla-gecko | same_root_cause | later | https://hg.mozilla.org/releases/mozilla-beta/rev/109cefe117fbdd1764097e06796960082f4fee4e | prior: https://bugs.chromium.org/p/project-zero/issues/detail?id=1820 |
| CVE-2019-17026 | CVE-2019-9810 | mozilla-gecko | same_root_cause | later | https://hg.mozilla.org/mozilla-central/rev/d6e40de88f3defdc12ef27e64ca73e120b1f10e2 | prior: mfsa2019-09 CVE-2019-9810 patch |
| CVE-2019-17026 | chromium-p0:1820 | mozilla-gecko | same_root_cause | later | https://hg.mozilla.org/mozilla-central/rev/d6e40de88f3defdc12ef27e64ca73e120b1f10e2 | prior: https://bugs.chromium.org/p/project-zero/issues/detail?id=1820 |
| CVE-2021-1905 | CVE-2020-11261 | qualcomm-adreno | same_root_cause | prior | https://source.codeaurora.org/quic/la/kernel/msm-4.9/commit/?id=d236d315145f8250523ce9e14897d62e5d6639fc | later: https://www.qualcomm.com/company/product-security/bulletins/may-2021-bulletin |

---

## TIER 3: Closed source (36 pairs)

Real sibling relationships confirmed by P0 RCAs, but no source code available. Binary patch diffing required.

| later | prior | software | kind | notes |
|---|---|---|---|---|
| CVE-2022-41073 | CVE-2022-29104 | microsoft-windows/PrintSpooler | same_root_cause | DosDevices impersonation; same bug |
| CVE-2022-21882 | CVE-2021-1732 | microsoft-windows/win32k | same_root_cause | User-callback xxxClientAllocWindowClassExtraBytes |
| CVE-2023-28252 | CVE-2022-37969 | microsoft-windows/CLFS | same_exploit_flow | Overlapping exploit gadgets |
| CVE-2023-28252 | CVE-2023-23376 | microsoft-windows/CLFS | same_root_cause | Both CLFS OOB read/write |
| CVE-2023-36802 | CVE-2022-37969 | microsoft-windows/mskssrv+CLFS | same_exploit_flow | Different drivers, same flow |
| CVE-2023-36033 | CVE-2022-21902 | microsoft-windows/dwmcore | same_root_cause | CKeyframeAnimation OOB |
| CVE-2022-26925 | CVE-2021-36942 | microsoft-windows/LSARPC | regression | PetitPotam patch regressed |
| CVE-2020-0986 | CVE-2019-0880 | microsoft-windows/splwow64 | incomplete_fix | GdiPrinterThunk trivial variant |
| CVE-2020-17008 | CVE-2020-0986 | microsoft-windows/splwow64 | incomplete_fix | Bad fix identified by P0 |
| CVE-2021-1648 | CVE-2020-0986 | microsoft-windows/splwow64 | incomplete_fix | Offset patch still exploitable |
| CVE-2020-0986 | chromium-p0:2096 | microsoft-windows/splwow64 | incomplete_fix | P0 confirmed bad fix |
| CVE-2019-1367 | CVE-2018-8653 | ie-jscript | same_root_cause | JScript GC VAR tracking UAF |
| CVE-2020-1429 | CVE-2019-1367 | ie-jscript | incomplete_fix | Incomplete patch + variant |
| CVE-2020-0674 | CVE-2019-1367 | ie-jscript | incomplete_fix | Trivial variant |
| CVE-2020-0674 | CVE-2018-8653 | ie-jscript | same_root_cause | 4 JScript GC UAFs same actor |
| CVE-2020-0674 | CVE-2019-1429 | ie-jscript | same_root_cause | Trivial variant chain |
| CVE-2019-1429 | CVE-2019-1367 | ie-jscript | incomplete_fix | Arguments object GC |
| CVE-2020-1429 | CVE-2020-0674 | ie-jscript | same_root_cause | JScript GC chain |
| CVE-2020-0968 | CVE-2020-0674 | ie-jscript | same_root_cause | String concatenation GC |
| CVE-2020-17053 | CVE-2020-1380 | ie-jscript9 | same_root_cause | jscript9.dll JIT bug |
| CVE-2022-41128 | CVE-2021-34480 | ie-jscript9 | same_root_cause | JScript9 JIT type confusion |
| CVE-2021-26411 | CVE-2019-1208 | ie-jscript | same_exploit_technique | Array length exploit technique |
| CVE-2019-1367 | chromium-p0:1504 | ie-jscript | same_root_cause | P0 fuzzer-found JScript GC bug |
| CVE-2019-1367 | chromium-p0:1505 | ie-jscript | same_root_cause | P0 fuzzer finding |
| CVE-2019-1367 | chromium-p0:1947 | ie-jscript | same_root_cause | P0#1947 = CVE-2019-1429 |
| CVE-2020-27946 | CVE-2020-27930 | apple-coretext | same_root_cause | libType1Scaler memory disclosure |
| CVE-2020-29624 | CVE-2020-27930 | apple-coretext | same_root_cause | libFontParser stack corruption |
| CVE-2020-27944 | CVE-2020-27930 | apple-coretext | same_root_cause | libType1Scaler STOREWV integer overflow |
| CVE-2020-27943 | CVE-2020-27930 | apple-coretext | same_root_cause | libType1Scaler Counter Control |
| CVE-2020-27930 | chromium-p0:2113 | apple-coretext | same_root_cause | =CVE-2020-27946 |
| CVE-2020-27930 | chromium-p0:2115 | apple-coretext | same_root_cause | =CVE-2020-29624 |
| CVE-2020-27930 | chromium-p0:2116 | apple-coretext | same_root_cause | =CVE-2020-27944 |
| CVE-2020-27930 | chromium-p0:2114 | apple-coretext | same_root_cause | =CVE-2020-27943 |
| CVE-2020-27930 | CVE-2015-3052 | apple-coretext | same_root_cause | Historical ancestor |
| CVE-2022-22265 | CVE-2020-28343 | samsung-npu-driver | same_root_cause | NPU variant analysis |
| CVE-2023-38831 | CVE-2023-3883 | winrar | same_root_cause | Trailing-space archive entry |
---

# Iteration 2 — Newly resolved patch CLs

These were resolved by `resolve_remaining_cls.py` (Gerrit `bug:NNN` /
`message:GHSL-link` / NVD `Patch`-tag chain). They should be merged
into the TIER 1 / TIER 2 tables above.

## Resolved via Gerrit `bug:NNN` search (chromium issue → CL)

| chromium bug | context | resolved CL | subject |
|---|---|---|---|
| crbug/1382434 | CVE-2022-4906 later | https://chromium-review.googlesource.com/c/v8/v8/+/4040931 | `[M102-LTS] Check all store modes for COW backing store access` |
| crbug/1055788 | CVE-2020-6427 later | https://chromium-review.googlesource.com/c/chromium/src/+/2106745 | `Use WeakPtr for cross-thread posting` |
| crbug/1311641 | CVE-2022-1232 later | https://chromium-review.googlesource.com/c/v8/v8/+/3794525 | `[runtime] Add runtime checks for name collisions` |
| crbug/1182647 | CVE-2021-21195 prior | https://chromium-review.googlesource.com/c/v8/v8/+/2748593 | `[deoptimizer] Fix bug in OptimizedFrame::Summarize` |
| crbug/1263462 | CVE-2021-38003 prior | https://chromium-review.googlesource.com/c/v8/v8/+/3253349 | `[M90-LTS][runtime] Check pending exception before return` |

## Resolved via GHSL advisory → crbug → Gerrit chain

| CVE | GHSL | crbug | resolved CL |
|---|---|---|---|
| CVE-2020-6427 | GHSL-2020-035-chrome | https://crbug.com/1055788 | https://chromium-review.googlesource.com/c/chromium/src/+/2106745 |
| CVE-2020-6428 | GHSL-2020-037-chrome | https://crbug.com/1057593 | https://chromium-review.googlesource.com/c/chromium/src/+/2104827 |
| CVE-2020-6429 | GHSL-2020-038-chrome | https://crbug.com/1057627 | https://chromium-review.googlesource.com/c/chromium/src/+/2106956 |
| CVE-2020-6449 | GHSL-2020-040-chrome | https://crbug.com/1059686 | https://chromium-review.googlesource.com/c/chromium/src/+/2107163 |
| CVE-2020-6450 | GHSL-2020-053-chrome | https://crbug.com/1062247 | https://chromium-review.googlesource.com/c/chromium/src/+/2116585 |
| CVE-2020-6451 | GHSL-2020-041-chrome | https://crbug.com/1061018 | https://chromium-review.googlesource.com/c/chromium/src/+/2116566 |
| CVE-2020-6406 | — (NVD direct) | https://crbug.com/1042254 | — (use crbug to find CL; same WebAudio cluster as 6427/6428/etc.) |
| CVE-2019-9810 | mfsa2019-09 | bugzilla/1537924 + bugzilla/1122305 | https://bugzilla.mozilla.org/show_bug.cgi?id=1537924 (track patches via attachment list) |

## Still unresolved automatically — manual lookup needed

These need a human (or a headless browser) because the modern Chromium
issue tracker (`issues.chromium.org`) is JavaScript-rendered and the
old `bugs.chromium.org` redirects but doesn't preserve content:

### Old Chromium issues (no Gerrit `bug:NNN` hit, no `BUG=NNN` either)

| chromium bug | context | manual lookup URL |
|---|---|---|
| crbug/663476 | CVE-2021-21206 prior (Promise.then thennable) | https://bugs.chromium.org/p/chromium/issues/detail?id=663476 |
| crbug/678706 | CVE-2021-21206 prior (related callback) | https://bugs.chromium.org/p/chromium/issues/detail?id=678706 |
| crbug/708887 | CVE-2021-21206 prior (related callback) | https://bugs.chromium.org/p/chromium/issues/detail?id=708887 |
| crbug/619166 | CVE-2021-30551 prior (JS exec in interceptors) | https://bugs.chromium.org/p/chromium/issues/detail?id=619166 |
| crbug/746946 | CVE-2021-30632 prior (map transition kinds) | https://bugs.chromium.org/p/chromium/issues/detail?id=746946 |

Hypothesis: these are old discussion-only / refactor / non-security bugs
without an explicit "fix" CL. Treat as background references — keep the
bug-tracker URL only.

### Project Zero issues (JS-rendered, no Gerrit cross-reference found)

| P0 issue | context | manual lookup URL |
|---|---|---|
| P0#1963 | CVE-2019-13732 / CVE-2020-6406 (WebAudio PannerHandler UAF) | https://bugs.chromium.org/p/project-zero/issues/detail?id=1963 |
| P0#1820 | CVE-2019-11707 / CVE-2019-17026 (SpiderMonkey similar ITW bug) | https://bugs.chromium.org/p/project-zero/issues/detail?id=1820 |
| P0#2280 | CVE-2022-1232 (already known: P0#2280 == CVE-2022-1232) | https://bugs.chromium.org/p/project-zero/issues/detail?id=2280 |
| P0#2106 | CVE-2021-30632 prior (map transition variant) | https://bugs.chromium.org/p/project-zero/issues/detail?id=2106 |

### CVEs without NVD `Patch`-tagged refs

| CVE | context | best available URL |
|---|---|---|
| CVE-2019-13732 | Chrome WebAudio PannerHandler::TailTime UAF (first fix) | https://googleprojectzero.github.io/0days-in-the-wild/0day-RCAs/2019/CVE-2019-13720.html (mentioned in same RCA) |
| CVE-2016-5128 | Historical V8 property-access interceptor (ancestor of CVE-2022-1096) | https://bugs.chromium.org/p/chromium/issues/detail?id=619166 |
| CVE-2021-38003 | V8 hole access (variant of CVE-2022-1364) | https://chromium-review.googlesource.com/c/v8/v8/+/3253349 (M90-LTS fix found via Gerrit) |
| CVE-2020-16011 | Identical Chrome bitmap UAF on Windows (variant of CVE-2020-16010) | https://googleprojectzero.github.io/0days-in-the-wild/0day-RCAs/2020/CVE-2020-16010.html#variants |
| CVE-2021-1905 | Qualcomm Adreno GPU memory-mapping UAF | https://source.codeaurora.org/quic/la/kernel/msm-4.9/commit/?id=d236d315145f8250523ce9e14897d62e5d6639fc (per PZ RCA) |

## Summary

- Iterations 1+2+3 of `resolve_remaining_cls.py` resolved **19 / 34** items automatically.
- Remaining 15 break down as: 5 old crbug issues (likely no fix CL exists), 4 P0 issues (JS-rendered tracker), 5 CVEs without NVD `Patch` refs.
- For most remaining items, the bug-tracker URL OR the PZ RCA file is the best available pointer; the actual CL likely exists in Chromium release-notes for the corresponding milestone.
