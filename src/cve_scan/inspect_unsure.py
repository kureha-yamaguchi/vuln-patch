"""Cross-reference the P0 sheet + RCA files + bug-id conventions to
verify the `same_codebase` claim on every confirmed seed pair, especially
the UNSURE ones where the deep diff verifier couldn't run.

For each pair:
- Resolve the LATER CVE's vendor/product/codebase from the sheet.
- Resolve the PRIOR identifier's codebase by the same lookup (if CVE) or
  from the identifier prefix (chromium/chromium-p0 → Chromium; mozilla
  → Mozilla/Firefox; github:owner/repo@sha → owner/repo).
- Compare and emit a verdict: agrees, disagrees, or unknown.
"""
from __future__ import annotations

import csv
import json
import os
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple

from . import config


# Cached sheet CSVs live here.
SHEET_DIR = os.path.join(config.CVE_SCAN_CACHE_DIR, 'p0')

# Heuristic vendor/product → codebase key mapping.
# Two CVEs map to the same `codebase` string iff they are in the same
# upstream codebase. This is the comparison key.
def codebase_key(vendor: str, product: str) -> str:
    v = (vendor or '').strip().lower()
    p = (product or '').strip().lower()

    # Known buckets, biggest first.
    if v == 'apple' and 'os x' not in p and 'mac' not in p:
        if 'safari' in p or 'webkit' in p:
            return 'apple-webkit'
        return f'apple-{p}' if p else 'apple'
    if v == 'apple':
        return f'apple-{p}' if p else 'apple-os'
    if v == 'google' or v == 'chrome' or 'chrome' in p or v == 'chromium' \
            or v == 'v8' or v == 'blink':
        # V8 and Blink are subcomponents of Chrome; for "same codebase"
        # purposes, all roll up to the chrome bucket.
        return 'chrome'
    if v == 'microsoft':
        if 'jscript' in p or 'internet explorer' in p or v == 'ie':
            return 'ie-jscript'
        if 'win32k' in p or 'windows' in p and 'kernel' in p:
            return 'windows-kernel'
        if 'edge' in p or 'chakra' in p:
            return 'edge'
        if 'office' in p:
            return 'microsoft-office'
        if 'clfs' in p:
            return 'windows-clfs'
        return f'microsoft-{p}' if p else 'microsoft'
    if v == 'mozilla' or 'firefox' in p:
        return 'mozilla-gecko'
    if 'samsung' in v:
        return f'samsung-{p}' if p else 'samsung'
    if 'arm' in v.lower() or 'mali' in p.lower() or 'mali' in v.lower():
        return 'mali-gpu-driver'
    if 'qualcomm' in v:
        return f'qualcomm-{p}' if p else 'qualcomm'
    if 'rar' in p.lower() or 'winrar' in p.lower():
        return 'winrar'
    if 'android' in p.lower():
        return 'android-kernel'
    return f'{v}-{p}' if (v or p) else 'unknown'


def _product_from_rca(cve_id: str) -> Optional[str]:
    """Fallback for CVEs missing from the sheet — look up the RCA file in
    the cloned 0days-in-the-wild repo and pull out its `**Product:**`
    line."""
    m = re.match(r'CVE-(\d{4})-\d{4,}', cve_id, re.IGNORECASE)
    if not m:
        return None
    year = m.group(1)
    rca_root = os.path.join(config.CVE_SCAN_CACHE_DIR, 'p0_repo',
                            '0day-RCAs', year)
    for fn in (cve_id + '.md', cve_id + '.html'):
        path = os.path.join(rca_root, fn)
        if not os.path.isfile(path):
            continue
        try:
            with open(path, 'r', encoding='utf-8', errors='replace') as f:
                # Frontmatter only — first 40 lines is plenty for the
                # **Product:** line.
                head = '\n'.join(f.read().splitlines()[:40])
        except OSError:
            continue
        m = re.search(r'\*\*Product:\*\*\s*(.+)', head)
        if m:
            return m.group(1).strip()
    return None


def codebase_from_bug_id(bug_id: str,
                         sheet_index: Dict[str, Tuple[str, str]]) -> Tuple[str, str]:
    """Return (codebase_key, source_str) for a bug identifier."""
    if bug_id.startswith('CVE-'):
        vp = sheet_index.get(bug_id)
        if vp is not None:
            v, p = vp
            return codebase_key(v, p), f'sheet ({v}/{p})'
        # Sheet miss — fall back to the CVE's own RCA file.
        rca_product = _product_from_rca(bug_id)
        if rca_product:
            return codebase_key('', rca_product), f'rca product ({rca_product})'
        return 'unknown', 'no sheet entry or RCA'
    if bug_id.startswith('chromium-p0:'):
        # P0's project-zero tracker hosts issues across many vendors
        # (Windows, Chrome, Apple, Mali, etc.) — the prefix alone tells
        # us nothing about which codebase the underlying bug is in.
        return 'unknown', 'P0 tracker spans vendors'
    if bug_id.startswith('chromium:'):
        return 'chrome', 'chromium project tracker'
    if bug_id.startswith('mozilla:'):
        return 'mozilla-gecko', 'mozilla bug prefix'
    if bug_id.startswith('github:'):
        rest = bug_id.split(':', 1)[1]
        m = re.match(r'([^/]+)/([^@]+)@', rest)
        if m:
            owner = m.group(1).lower()
            repo = m.group(2).lower()
            slug = f'{owner}/{repo}'
            mapping = {
                # V8 / Blink / Chromium all roll up to `chrome` for
                # same-codebase comparison purposes.
                'v8/v8': 'chrome',
                'chromium/chromium': 'chrome',
                'chromium/chromium-src': 'chrome',
                'mozilla/gecko-dev': 'mozilla-gecko',
                'webkit/webkit': 'apple-webkit',
                'torvalds/linux': 'linux-kernel',
                'googleprojectzero/fuzzilli': 'fuzzilli',
            }
            return mapping.get(slug, slug), f'github repo'
    return 'unknown', f'unrecognised prefix'


def build_sheet_index() -> Dict[str, Tuple[str, str]]:
    """Walk the cached sheet CSVs and return {cve: (vendor, product)}."""
    out: Dict[str, Tuple[str, str]] = {}
    if not os.path.isdir(SHEET_DIR):
        return out
    for fn in os.listdir(SHEET_DIR):
        if not (fn.startswith('sheet_') and fn.endswith('.csv')):
            continue
        path = os.path.join(SHEET_DIR, fn)
        try:
            with open(path, 'r', encoding='utf-8', errors='replace') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    cve_cell = row.get('CVE') or ''
                    cves = re.findall(r'CVE-\d{4}-\d{4,}', cve_cell)
                    vendor = (row.get('Vendor') or '').strip()
                    product = (row.get('Product') or '').strip()
                    for cid in cves:
                        out.setdefault(cid, (vendor, product))
        except OSError:
            continue
    return out


@dataclass
class PairAssessment:
    later: str
    prior: str
    llm_relationship_kind: str
    llm_same_codebase_claim: bool
    llm_confidence: float
    deep_kind: Optional[str]
    later_codebase: str
    later_source: str
    prior_codebase: str
    prior_source: str
    same_codebase_observed: bool   # both unknown → False
    agrees_with_llm: bool
    verdict: str                   # 'agrees' | 'disagrees' | 'unknown_codebase'
    note: str = ''

    def as_dict(self) -> dict:
        from dataclasses import asdict
        return asdict(self)


def assess_pair(seed: dict, deep: Optional[dict],
                sheet_index: Dict[str, Tuple[str, str]]) -> PairAssessment:
    later = seed['later_cve']
    prior = seed['prior_cve']
    later_cb, later_src = codebase_from_bug_id(later, sheet_index)
    prior_cb, prior_src = codebase_from_bug_id(prior, sheet_index)

    if later_cb == 'unknown' or prior_cb == 'unknown':
        verdict = 'unknown_codebase'
        same_observed = False
    else:
        same_observed = (later_cb == prior_cb)
        if seed['llm_same_codebase'] == same_observed:
            verdict = 'agrees'
        else:
            verdict = 'disagrees'

    note = ''
    if verdict == 'disagrees' and seed['llm_same_codebase']:
        note = (f'LLM said same_codebase=True but we infer different '
                f'codebases ({later_cb} vs {prior_cb}).')
    elif verdict == 'disagrees' and not seed['llm_same_codebase']:
        note = (f'LLM said same_codebase=False but we infer same '
                f'codebase ({later_cb}).')

    return PairAssessment(
        later=later, prior=prior,
        llm_relationship_kind=seed['llm_relationship_kind'],
        llm_same_codebase_claim=seed['llm_same_codebase'],
        llm_confidence=seed['llm_confidence'],
        deep_kind=(deep['diff_kind'] if deep else None),
        later_codebase=later_cb, later_source=later_src,
        prior_codebase=prior_cb, prior_source=prior_src,
        same_codebase_observed=same_observed,
        agrees_with_llm=(verdict == 'agrees'),
        verdict=verdict, note=note,
    )


def run(seeds_path: str, diffs_path: str) -> List[PairAssessment]:
    with open(seeds_path, 'r', encoding='utf-8') as f:
        seeds = json.load(f)
    diffs = []
    if os.path.isfile(diffs_path):
        with open(diffs_path, 'r', encoding='utf-8') as f:
            diffs = json.load(f)
    dx = {(d['later_cve'], d['prior_cve']): d for d in diffs}
    sheet_index = build_sheet_index()
    confirmed = [s for s in seeds if s['confirmed']]
    out = []
    for s in confirmed:
        d = dx.get((s['later_cve'], s['prior_cve']))
        out.append(assess_pair(s, d, sheet_index))
    return out


def main():
    seeds_path = os.path.join(config.CVE_SCAN_OUTPUT_DIR, 'seeds.json')
    diffs_path = os.path.join(config.CVE_SCAN_OUTPUT_DIR, 'deep_relate.json')
    out_dir = config.CVE_SCAN_OUTPUT_DIR
    assessments = run(seeds_path, diffs_path)

    os.makedirs(out_dir, exist_ok=True)
    json_path = os.path.join(out_dir, 'codebase_audit.json')
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump([a.as_dict() for a in assessments], f, indent=2)
    print(f"wrote {json_path}")

    # Summary
    agrees    = [a for a in assessments if a.verdict == 'agrees']
    disagrees = [a for a in assessments if a.verdict == 'disagrees']
    unknown   = [a for a in assessments if a.verdict == 'unknown_codebase']
    print(f"\nAssessment summary for {len(assessments)} confirmed pairs:")
    print(f"  same_codebase claim AGREES with sheet+heuristic: {len(agrees)}")
    print(f"  same_codebase claim DISAGREES:                   {len(disagrees)}")
    print(f"  one or both codebases UNKNOWN:                   {len(unknown)}")

    if disagrees:
        print("\nPairs where the LLM's same_codebase claim disagrees with the sheet:")
        for a in disagrees:
            print(f"  {a.later} -> {a.prior}")
            print(f"    LLM said same_codebase={a.llm_same_codebase_claim}")
            print(f"    later codebase: {a.later_codebase}  [{a.later_source}]")
            print(f"    prior codebase: {a.prior_codebase}  [{a.prior_source}]")
            print(f"    note: {a.note}")

    if unknown:
        print(f"\nPairs where codebase could not be inferred ({len(unknown)} pairs):")
        for a in unknown[:20]:
            print(f"  {a.later} -> {a.prior}  "
                  f"(later: {a.later_codebase}, prior: {a.prior_codebase})")
        if len(unknown) > 20:
            print(f"  ... and {len(unknown) - 20} more")

    return 0


if __name__ == '__main__':
    sys.exit(main())
