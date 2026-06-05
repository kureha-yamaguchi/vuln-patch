# Needs manual page-read — 18 open-source bug-ids missing a patch URL

These are the open-source pairs still missing **one** fix-commit URL
after the automated resolver + override pass. I could not resolve them
automatically because the relevant pages are either JavaScript-rendered
(`issues.chromium.org`) or sign-in gated, and neither NVD nor Gerrit
`bug:NNN` search returns a trustworthy commit for them.

**How to use this file:** open the "best URL to read" for an item, find
the merged fix commit (look for a `chromium-review.googlesource.com/c/.../+/NNN`
link, a `Fixed:`/`Closed` commit hash, or an `hg.mozilla.org/.../rev/HASH`
changeset), and either paste the page content back to me OR add the URL
straight into `patch_url_overrides.json` and re-run the harvester.

Grouped by how likely a real, dedicated fix commit exists.

---

## A. Real fix exists upstream, but the bug page is access-restricted

The fix commits for these are referenced only on Chromium bug pages that
remain **security-view-restricted even to signed-in Google accounts**
(verified 2026-06 — both `issues.chromium.org/issues/1144489` and
`/issues/1023817` returned "access denied"). The fix CLs are therefore
not obtainable without Chromium-security-team access. The exact bug
number to request / re-check later is recorded below.

| bug-id | what it is | fix lives on (restricted) | exact bug to re-check |
|---|---|---|---|
| `CVE-2020-6406` | Chrome WebAudio UAF in `PannerHandler::TailTime` (incomplete-fix follow-up of CVE-2019-13720) | crbug/1023817 (PannerHandler), tracked as the "remaining work" bug | issues.chromium.org/issues/1023817 |
| `CVE-2019-13732` | same PannerHandler bug family (P0 1963 == CVE-2019-13732 / CVE-2020-6406) | crbug/1023817 | issues.chromium.org/issues/1023817 |
| `CVE-2020-16011` | Chrome `ConvertToJavaBitmap` heap overflow on **Windows** — split from the Android bug (CVE-2020-16010, crbug/1144368, fix `e598fc59…`) into its own bug in `os_exchange_data_provider_win.cc` | crbug/1144489 | issues.chromium.org/issues/1144489 |
| `CVE-2021-1905` | Qualcomm Adreno GPU UAF (`kgsl` memory mapping) | May-2021 Qualcomm bulletin → CodeLinaro — **the published commit URL no longer resolves** (CodeAurora retired; no working CodeLinaro mirror found) | — dead |
| `CVE-2021-44828` | Mali GPU "CPU RO pages → writable" (third bug in the CVE-2021-28664 → CVE-2021-39793 chain) | **no public git commit** — ARM ships the Mali driver as *releases* (fixed in Bifrost/Valhall r35p0, Midgard r32p0), not as a browsable commit. Dead end. | — dead |
| `chromium-p0:1963` | == CVE-2019-13732 / CVE-2020-6406 (PannerHandler WebAudio). The CVE-2019-13720 RCA names it as a found variant but does not link its fix CL. | crbug/1023817 (restricted) | — restricted |
| `chromium-p0:2112` | **RESOLVED as a self-pair.** The CVE-2020-16010 RCA shows P0 2112 is that bug's *own* Project Zero issue (not a variant), so its fix is the same commit `e598fc59…`. Added to the overrides; the `CVE-2020-16010 → chromium-p0:2112` pair is now correctly DROPPED as a self-reference. | done | — |

## B. "Similar bug" references — fix may exist, lower confidence it's a true sibling

_None remaining — every readable open-source target has been resolved._

(`chromium-p0:1820` was the last; it turned out to **be** CVE-2019-11707
itself (bugzilla 1544386), so its fix is the same gecko-dev commit
`4ca7a9d3…`. The `CVE-2019-11707 → chromium-p0:1820` pair correctly
falls out as a self-reference, and `CVE-2019-17026 → chromium-p0:1820`
is now READY (incomplete_fix).)

> **Resolved during this pass** (moved out of this list):
> `CVE-2016-5128` + `chromium:619166` (V8 interceptor `objects.cc`,
> commit `2c8ca9ad…`), `CVE-2019-13695` (MojoAudioDecoder, `d496219f…`),
> `CVE-2019-5870` (MojoCdmService, `b7b305f3…`), and
> `chromium-p0:168` + `CVE-2014-9665` (FreeType `Load_SBit_Png` sbix PNG
> integer overflow on `git.savannah.gnu.org`, commit `54abd228…` — the
> 2014 fix whose bug class resurfaced as the 2020 ITW 0-day
> CVE-2020-15999). All now READY. The pattern that worked: read the
> *full* upstream bug page — the "The following revision refers to this
> bug" / "Fixed in …" comment carries the exact commit URL (chromium,
> savannah, hg.mozilla, codelinaro — the resolver now fetches all of
> these).

## C. Likely NO dedicated fix commit — background references (low priority)

These appear in Project Zero RCAs as *historical context* ("a researcher
could have come across crbugN"), not as sibling CVEs with their own fix.

> **Correction:** `chromium:619166` was *initially* mis-classified here
> as an unrelated "Add HasOwnProperty with array indexes" change — that
> was only a supporting CL on the bug. Reading the full bug page
> (= CVE-2016-5128) showed the real fix is commit `2c8ca9ad…`
> *"Make sure api interceptors don't change the store target w/o
> storing"* in `src/objects.cc` — the same V8 property-interceptor
> lineage as CVE-2021-30551 / CVE-2022-1096. It is now RESOLVED and
> READY (`CVE-2021-30551 → chromium:619166`, same_root_cause_confirmed).
> Lesson: don't judge an old crbug from its version-tag cherry-pick;
> read the full bug page.

| bug-id | cited in RCA of | note |
|---|---|---|
| `chromium:663476` | CVE-2021-21206 | Promise.then thennable historical doc |
| `chromium:678706` | CVE-2021-21206 | related-callback historical bug |
| `chromium:708887` | CVE-2021-21206 | related-callback historical bug |
| `chromium:746946` | CVE-2021-30632 | map transition/deprecation historical bug |

## D. Dead — no fix exists upstream

| bug-id | status |
|---|---|
| `mozilla:1368273` | Bugzilla marks it **INCOMPLETE / wontfix** (cold trail, no reproducible test case). The CVE-2020-6820 RCA cites it as a similar-but-never-fixed Cache-module UAF. No commit to fetch. |

---

## To add a resolved URL

Edit `patch_url_overrides.json`:

```json
"overrides": {
  ...
  "CVE-2020-6406": "https://chromium-review.googlesource.com/c/chromium/src/+/NNNNNNN",
  ...
}
```

then re-run from `src/db/project_zero`:

```bash
uv run --no-project --with openai --python 3.12 -m discover.run_p0_harvest --budget-usd 1
uv run --no-project --with openai --python 3.12 -m discover.run_diff_relate --budget-usd 2
uv run --no-project --with openai --python 3.12 -m discover.run_inspect_unsure
uv run --no-project --with openai --python 3.12 -m discover.make_seeds_table
```

The resolver auto-normalises `source.chromium.org/.../+/sha`,
`git.kernel.org/linus/sha`, `source.codeaurora.org/.../commit/?id=`,
and `hg.mozilla.org/.../rev/sha` forms, so paste whichever URL the page
shows.
