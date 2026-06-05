#!/usr/bin/env python3
"""
Resolve remaining fix CLs for CVE sibling pairs.

For Chromium bugs: uses the Chromium Gerrit REST API (bug:NNN search)
to find merged CLs.  For Mozilla bugs: tries the mfsa pages + Bugzilla.
For CVEs: queries NVD for Patch-tagged references.

Usage:
    python resolve_remaining_cls.py

Writes resolved_cls.json next to this script.
"""

import json
import re
import sys
import time

import requests

CHROMIUM_BUGS = {
    663476:  ("CVE-2021-21206 prior",       "Promise.then thennable callback pattern"),
    678706:  ("CVE-2021-21206 prior",       "Related callback bug"),
    708887:  ("CVE-2021-21206 prior",       "Related callback bug"),
    619166:  ("CVE-2021-30551 prior",       "JS execution inside interceptors"),
    746946:  ("CVE-2021-30632 prior",       "Map transition element kinds"),
    1382434: ("CVE-2022-4906 later",        "Glazunov COW array bug"),
    1055788: ("CVE-2020-6427 later",        "GHSL-2020-035 IIRFilterHandler UaP"),
    1311641: ("CVE-2022-1232 later",        "Fix for DefineOwnPropertyIgnoreAttributes"),
    1182647: ("CVE-2021-21195 prior",       "V8 escape analysis / CVE-2021-21195"),
    1263462: ("CVE-2021-38003 prior",       "V8 hole access / CVE-2021-38003"),
}

P0_BUGS = {
    2106: ("CVE-2021-30632 prior",                 "Map transition/deprecation"),
    1963: ("CVE-2019-13732/CVE-2020-6406 later",   "WebAudio PannerHandler UAF"),
    2280: ("CVE-2022-1232 later",                  "DefineOwnPropertyIgnoreAttributes"),
    1820: ("CVE-2019-11707/CVE-2019-17026 prior",  "SpiderMonkey similar ITW bug"),
}

CVES_NEEDING_CL = {
    "CVE-2020-6427": "GHSL-2020-035",
    "CVE-2020-6428": "GHSL-2020-037",
    "CVE-2020-6429": "GHSL-2020-038",
    "CVE-2020-6449": "GHSL-2020-040",
    "CVE-2020-6450": "GHSL-2020-053 (incomplete-fix of 035/038)",
    "CVE-2020-6451": "GHSL-2020-041",
    "CVE-2020-6406": "Second fix after incomplete P0 1963 patch",
    "CVE-2019-13732": "P0 1963 (PannerHandler::TailTime UAF, first fix)",
    "CVE-2016-5128": "Historical property-access interceptor bug",
    "CVE-2021-21195": "V8 escape analysis variant",
    "CVE-2021-38003": "V8 hole access",
    "CVE-2020-16011": "Identical JavaBitmap bug on Windows",
    "CVE-2019-9810":  "Mozilla IonMonkey alias analysis (mfsa2019-09)",
    "CVE-2021-1905":  "Qualcomm Adreno GPU UaF",
}


def _gerrit_query(query):
    """Raw Gerrit REST query — returns parsed JSON list of changes."""
    import urllib.parse as _up
    url = (f'https://chromium-review.googlesource.com/changes/'
           f'?q={_up.quote(query)}&n=5')
    try:
        resp = requests.get(url, timeout=15,
                            headers={'User-Agent': 'Mozilla/5.0',
                                     'Accept': 'application/json'})
        if resp.status_code != 200:
            return []
        text = resp.text.lstrip(")]}'\n")
        return json.loads(text)
    except (requests.RequestException, json.JSONDecodeError, ValueError) as e:
        print(f'    Gerrit error ({query!r}): {e}')
        return []


def try_gerrit_api(bug_id):
    """Query Gerrit REST for merged CLs that reference the bug.

    Tries multiple query forms because Chromium's commit-message bug
    trailer has evolved over time:
      - `bug:NNN`                     — modern `Bug: NNN` trailer
      - `tr:chromium:NNN`             — tracker-ref form
      - `message:"BUG=NNN"`           — pre-2015 BUG= syntax
      - `message:"crbug.com/NNN"`     — when commit cites a crbug URL
      - `message:"project-zero/NNN"`  — when commit cites a P0 issue
    """
    seen = set()
    out = []
    queries = [
        f'bug:{bug_id} status:merged',
        f'tr:chromium:{bug_id} status:merged',
        f'message:"BUG={bug_id}" status:merged',
        f'message:"crbug.com/{bug_id}" status:merged',
        f'message:"crbug/{bug_id}" status:merged',
        f'message:"issues/detail?id={bug_id}" status:merged',
    ]
    for q in queries:
        data = _gerrit_query(q)
        for change in data:
            num = change.get('_number')
            if not num or num in seen:
                continue
            seen.add(num)
            out.append({
                'url': f'https://chromium-review.googlesource.com/c/'
                       f'{change.get("project", "chromium/src")}/+/{num}',
                'subject': (change.get('subject') or '')[:80],
            })
        if len(out) >= 3:
            break
    return out


def try_gerrit_p0(p0_id):
    """For a P0 issue number, search Gerrit for commits whose message
    cites the issue (`project-zero/NNN`, `b/NNN` buganizer ref, etc.)."""
    seen = set()
    out = []
    queries = [
        f'message:"project-zero/{p0_id}" status:merged',
        f'message:"project-zero:{p0_id}" status:merged',
        f'message:"project-zero/issues/detail?id={p0_id}" status:merged',
        f'message:"bugs.chromium.org/p/project-zero/issues/detail?id={p0_id}" status:merged',
        f'message:"P0 {p0_id}" status:merged',
    ]
    for q in queries:
        data = _gerrit_query(q)
        for change in data:
            num = change.get('_number')
            if not num or num in seen:
                continue
            seen.add(num)
            out.append({
                'url': f'https://chromium-review.googlesource.com/c/'
                       f'{change.get("project", "chromium/src")}/+/{num}',
                'subject': (change.get('subject') or '')[:80],
            })
        if len(out) >= 3:
            break
    return out


def try_ghsl(advisory):
    """Fetch a GitHub Security Lab advisory page (GHSL-YYYY-NNN) and
    extract any crbug URLs or chromium-review CL URLs from it."""
    url = f'https://securitylab.github.com/advisories/{advisory}/'
    try:
        resp = requests.get(url, timeout=10,
                            headers={'User-Agent': 'Mozilla/5.0'})
        if resp.status_code != 200:
            return []
        bugs = re.findall(r'(?:https?://)?(?:bugs\.chromium\.org/[^\s"\'<>)]*?id=(\d+)|crbug\.com/(\d+))',
                          resp.text)
        flat = [b[0] or b[1] for b in bugs if (b[0] or b[1])]
        cls = re.findall(r'https://chromium-review\.googlesource\.com/[^\s"\'<>)]+',
                         resp.text)
        return list(dict.fromkeys([f'crbug/{b}' for b in flat] + cls))
    except requests.RequestException:
        return []


def try_monorail(bug_id, project='chromium'):
    """Try the old bugs.chromium.org Monorail (may be a JS redirect)."""
    url = f'https://bugs.chromium.org/p/{project}/issues/detail?id={bug_id}'
    try:
        resp = requests.get(url, timeout=10,
                            headers={'User-Agent': 'Mozilla/5.0'})
        if resp.status_code != 200:
            return []
        cls = re.findall(
            r'https://chromium-review\.googlesource\.com/c/[^\s"\'<>)]+',
            resp.text,
        )
        commits = re.findall(
            r'https://chromium\.googlesource\.com/[^\s"\'<>)]+/\+/[a-f0-9]+',
            resp.text,
        )
        return list(dict.fromkeys(cls + commits))
    except requests.RequestException:
        return []


def try_nvd(cve_id):
    """Pull NVD references and keep ones that look like source-host URLs."""
    url = f'https://services.nvd.nist.gov/rest/json/cves/2.0?cveId={cve_id}'
    keep = ('googlesource', 'hg.mozilla.org', 'bugzilla.mozilla.org',
            'git.kernel.org', 'codelinaro', 'mozilla.org/en-US/security')
    try:
        resp = requests.get(url, timeout=15,
                            headers={'User-Agent': 'Mozilla/5.0'})
        if resp.status_code != 200:
            return []
        data = resp.json()
        out = []
        for vuln in data.get('vulnerabilities', []):
            for ref in (vuln.get('cve', {}) or {}).get('references', []) or []:
                u = ref.get('url') or ''
                tags = [t.lower() for t in (ref.get('tags') or [])]
                if any(k in u for k in keep) or 'patch' in tags:
                    out.append({'url': u, 'tags': tags})
        return out
    except (requests.RequestException, json.JSONDecodeError, ValueError):
        return []


def try_mfsa(cve_id):
    """Search Mozilla's CVE-indexed advisory page."""
    url = (f'https://www.mozilla.org/en-US/security/advisories/?cve={cve_id}')
    try:
        resp = requests.get(url, timeout=10,
                            headers={'User-Agent': 'Mozilla/5.0'})
        hg = re.findall(r'https://hg\.mozilla\.org/[^\s"\'<>)]+', resp.text)
        bz = re.findall(r'https://bugzilla\.mozilla\.org/show_bug\.cgi\?id=\d+',
                        resp.text)
        return list(dict.fromkeys(hg + bz))
    except requests.RequestException:
        return []


def main():
    print('=' * 70)
    print('Resolving remaining fix CLs for CVE sibling pairs')
    print('=' * 70)

    results = []

    print('\n--- Chromium issues (Gerrit bug:NNN search) ---')
    for bug_id, (ctx, desc) in CHROMIUM_BUGS.items():
        print(f'\n  [{bug_id}] {ctx}: {desc}')
        cls = try_gerrit_api(bug_id)
        if not cls:
            mono = try_monorail(bug_id, 'chromium')
            cls = [{'url': u, 'subject': ''} for u in mono]
        if cls:
            for c in cls[:3]:
                print(f'    FOUND: {c["url"]}  {c.get("subject", "")[:60]}')
            results.append(('chromium', str(bug_id), ctx, desc,
                            [c['url'] for c in cls]))
        else:
            print('    NOT FOUND via Gerrit/Monorail')
            results.append(('chromium', str(bug_id), ctx, desc, []))
        time.sleep(0.4)

    print('\n--- Project Zero bugs (Gerrit message-search) ---')
    for p0, (ctx, desc) in P0_BUGS.items():
        print(f'\n  [P0#{p0}] {ctx}: {desc}')
        cls = try_gerrit_p0(p0)
        if not cls:
            # As a fallback try the JS-redirect monorail page.
            mono = try_monorail(p0, 'project-zero')
            cls = [{'url': u, 'subject': ''} for u in mono]
        if cls:
            for c in cls[:3]:
                print(f'    FOUND: {c["url"]}  {c.get("subject", "")[:60]}')
            results.append(('p0', str(p0), ctx, desc,
                            [c['url'] for c in cls]))
        else:
            print('    NOT FOUND (P0 tracker is JS-rendered; manual lookup needed)')
            results.append(('p0', str(p0), ctx, desc, []))
        time.sleep(0.4)

    # GHSL advisories — extract crbug ids, then use Gerrit to find CLs.
    GHSL = {
        'CVE-2020-6427': 'GHSL-2020-035-chrome',
        'CVE-2020-6428': 'GHSL-2020-037-chrome',
        'CVE-2020-6429': 'GHSL-2020-038-chrome',
        'CVE-2020-6449': 'GHSL-2020-040-chrome',
        'CVE-2020-6450': 'GHSL-2020-053-chrome',
        'CVE-2020-6451': 'GHSL-2020-041-chrome',
    }
    print('\n--- GHSL advisories (fetch + Gerrit follow-up) ---')
    for cve, adv in GHSL.items():
        print(f'\n  [{cve}] {adv}')
        items = try_ghsl(adv)
        crbug_ids = [s.split('/', 1)[1] for s in items if s.startswith('crbug/')]
        urls = [s for s in items if s.startswith('http')]
        if crbug_ids:
            for cb in crbug_ids[:2]:
                print(f'    GHSL → crbug/{cb}')
                cls = try_gerrit_api(cb)
                for c in cls[:2]:
                    print(f'      Gerrit FOUND: {c["url"]}')
                    urls.append(c['url'])
        if urls:
            results.append(('ghsl', cve, adv, '', urls))
        else:
            print('    NOT FOUND (GHSL page or Gerrit had nothing)')
            results.append(('ghsl', cve, adv, '', []))
        time.sleep(0.4)

    print('\n--- CVEs via NVD + Gerrit CVE-search fallback ---')
    for cve, desc in CVES_NEEDING_CL.items():
        print(f'\n  [{cve}] {desc}')
        refs = try_nvd(cve)
        if 'mozilla' in desc.lower() or cve == 'CVE-2019-9810':
            refs.extend({'url': u, 'tags': ['mfsa']} for u in try_mfsa(cve))
        # Always also try Gerrit's free-text search for the CVE id —
        # many Chromium commits cite the CVE in the subject or body.
        cls = _gerrit_query(f'message:"{cve}" status:merged')[:3]
        for c in cls:
            num = c.get('_number')
            if num:
                url = (f'https://chromium-review.googlesource.com/c/'
                       f'{c.get("project", "chromium/src")}/+/{num}')
                refs.append({'url': url, 'tags': ['gerrit-msg']})
        if refs:
            for r in refs[:5]:
                print(f'    FOUND: {r["url"]}  tags={r.get("tags", [])}')
            results.append(('cve', cve, '', desc,
                            [r['url'] for r in refs]))
        else:
            print('    NOT FOUND')
            results.append(('cve', cve, '', desc, []))
        time.sleep(0.6)

    found = sum(1 for r in results if r[4])
    print('\n' + '=' * 70)
    print(f'SUMMARY: {found}/{len(results)} resolved')
    print('=' * 70)

    # Write next to this script; gitignored. The human-readable
    # summary lives in findings_urls.md (manually curated).
    import os as _os
    out_path = _os.path.join(_os.path.dirname(_os.path.abspath(__file__)),
                              'resolved_cls.json')
    with open(out_path, 'w') as f:
        json.dump(results, f, indent=2)
    print(f'wrote {out_path}')


if __name__ == '__main__':
    sys.exit(main())
