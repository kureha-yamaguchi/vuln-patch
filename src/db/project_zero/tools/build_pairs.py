#!/usr/bin/env python3
"""Materialise the READY P0 variant-pairs into a ``pairs/`` tree, mirroring
the ``linux_kernel/pairs/`` layout.

For every READY pair (both fix-commit patches resolved AND the deep diff
verifier confirmed the relationship at code level, after alias-dedup) this
writes::

    pairs/<PRIOR>__<LATER>/
        fix0.patch      diff of the PRIOR fix (the incomplete one, Fix-0)
        fix1.patch      diff of the LATER fix (the corrective one, Fix-1)
        metadata.json   CVEs, commits, relationship verdicts, files, evidence

Convention follows linux_kernel: **fix0 = prior** (the bug whose patch was
incomplete) and **fix1 = later** (the CVE that incompleteness caused). The
patches themselves are already in the resolver's disk cache; this just copies
them into the per-pair layout and derives metadata. Idempotent.

The actual codebases are deliberately NOT pulled (chromium/src alone is 61 GB
— see findings/ and the memory note); per-pair source-file context is left to
a future ``fetch_context.py`` step, exactly as linux_kernel splits
fetch_patches from fetch_context.

Usage:
    cd src/db/project_zero
    uv run --no-project --with openai --python 3.12 -m tools.build_pairs
    uv run --no-project --with openai --python 3.12 -m tools.build_pairs --force
"""
from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple

# Allow `-m tools.build_pairs` from the project_zero dir.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from discover import config
from discover.code_overlap import CodeOverlapChecker
from discover.make_seeds_table import (
    BUG_ALIASES, _canon_bug, _CODEBASE_TO_LABEL, _label_for,
)
from discover.p0_harvest import RcaRepo

READY_KINDS = {
    'incomplete_fix_confirmed': 'incomplete_fix',
    'same_root_cause_confirmed': 'same_root_cause',
    'one_extends_other': 'one_extends_other',
}

PAIRS_DIR = Path(__file__).resolve().parent.parent / 'pairs'
PIPELINE_DIR = Path(config.CVE_SCAN_PIPELINE_DIR)


def _safe(bug_id: str) -> str:
    """Filesystem-safe form of a bug id (chromium:1234 -> chromium-1234)."""
    return bug_id.replace(':', '-')


def _repo_url_for(commit_url: str) -> Optional[str]:
    """Canonical repository URL for a resolved fix-commit URL."""
    if not commit_url:
        return None
    m = re.match(r'https?://([^/]+)/(.+)', commit_url)
    if not m:
        return commit_url
    host, path = m.group(1), m.group(2)
    if 'github.com' in host:
        mm = re.match(r'([^/]+/[^/]+)', path)
        return f'https://github.com/{mm.group(1)}' if mm else commit_url
    if host.endswith('googlesource.com'):
        mm = re.search(r'/c/(.+?)/\+/', path) or re.match(r'(.+?)(?:\.git)?/\+/', path)
        repo = mm.group(1) if mm else ''
        base = 'chromium-review' if host.startswith('chromium-review') else host.split('.')[0]
        if host.startswith('chromium-review'):
            return f'https://chromium.googlesource.com/{repo}'
        return f'https://{host}/{repo}'
    if 'kernel.org' in host:
        return ('https://git.kernel.org/pub/scm/linux/kernel/git/'
                'torvalds/linux.git')
    if 'codelinaro.org' in host:
        mm = re.match(r'(.+?)/-/commit/', path)
        return f'https://{host}/{mm.group(1)}' if mm else commit_url
    if 'hg.mozilla.org' in host:
        return 'https://hg.mozilla.org/mozilla-central'
    if 'savannah.gnu.org' in host:
        return 'https://git.savannah.gnu.org/git/freetype/freetype2.git'
    return f'https://{host}'


def _commit_id(commit_url: str) -> Optional[str]:
    """Bare commit identifier from a resolved fix-commit URL — a git SHA, or
    `CL/<n>` for a Gerrit change. Mirrors linux_kernel's bare-SHA
    `fix0_commit`; the full URL is kept separately in `*_patch_url`."""
    if not commit_url:
        return None
    m = re.search(r'chromium-review\.googlesource\.com/.*?\+/(\d+)', commit_url)
    if m:
        return f'CL/{m.group(1)}'
    for pat in (r'/\+/([0-9a-f]{7,40})',          # gitiles
                r'/commit/([0-9a-f]{7,40})',      # github / codelinaro
                r'[?&]id=([0-9a-f]{7,40})',       # cgit (kernel.org, savannah)
                r'@([0-9a-f]{7,40})',             # github:owner/repo@sha
                r'/rev/([0-9a-f]{7,40})'):        # hg.mozilla
        m = re.search(pat, commit_url)
        if m:
            return m.group(1)
    return None


_DATE_RE = re.compile(r'^Date:\s+(.+)$', re.MULTILINE)


def _extract_date(patch_text: str) -> Optional[str]:
    """Best-effort commit date from a `Date:` header (github format-patch,
    cgit). Many gitiles/gerrit patches lack it — returns None then."""
    m = _DATE_RE.search(patch_text[:2000])
    return m.group(1).strip() if m else None


def _changed_files(patch_text: str) -> List[str]:
    files = re.findall(r'^diff --git a/(.*?) b/', patch_text, re.MULTILINE)
    if files:
        return files
    # gitiles/gerrit base64-decoded diffs sometimes use `+++ b/<path>`.
    return [f for f in re.findall(r'^\+\+\+ b/(.*)$', patch_text, re.MULTILINE)]


def _load_ready() -> List[dict]:
    """Recompute the READY set with the same alias-dedup as the table."""
    deeps = json.load(open(PIPELINE_DIR / 'deep_relate.json'))
    seeds = {(s['later_cve'], s['prior_cve']): s
             for s in json.load(open(PIPELINE_DIR / 'seeds.json'))
             if s.get('confirmed')}
    audits = {(a['later'], a['prior']): a
              for a in json.load(open(PIPELINE_DIR / 'codebase_audit.json'))}

    seen: set = set()
    ready: List[dict] = []
    for d in deeps:
        L, P = d['later_cve'], d['prior_cve']
        if not (d.get('later_patch_url') and d.get('prior_patch_url')):
            continue
        if d['diff_kind'] not in READY_KINDS:
            continue
        cL, cP = _canon_bug(L), _canon_bug(P)
        if cL == cP or (cL, cP) in seen:      # alias self-pair / duplicate
            continue
        seen.add((cL, cP))
        d['_seed'] = seeds.get((L, P), {})
        d['_audit'] = audits.get((L, P), {})
        ready.append(d)
    return ready


def _codebase(audit: dict) -> str:
    for cb in (audit.get('later_codebase'), audit.get('prior_codebase')):
        if cb and cb != 'unknown':
            return cb
    return 'unknown'


def build(force: bool = False) -> int:
    rca_dir = RcaRepo().ensure(refresh=False)
    chk = CodeOverlapChecker(rca_dir=rca_dir,
                             cache_dir=config.CVE_SCAN_CACHE_DIR,
                             github_token=os.getenv('GITHUB_TOKEN'))
    ready = _load_ready()
    PAIRS_DIR.mkdir(exist_ok=True)
    written, skipped = 0, 0
    for d in ready:
        L, P = d['later_cve'], d['prior_cve']
        ov = chk.check_pair(L, P)
        later_patch = ov.later_patches[0] if ov.later_patches else None
        prior_patch = ov.prior_patches[0] if ov.prior_patches else None
        if not (later_patch and prior_patch
                and later_patch.local_path and prior_patch.local_path):
            print(f"  SKIP {P} -> {L}: patch cache miss")
            skipped += 1
            continue

        name = f'{_safe(P)}__{_safe(L)}'
        pair_dir = PAIRS_DIR / name
        if pair_dir.exists() and not force:
            skipped += 1
            continue
        pair_dir.mkdir(parents=True, exist_ok=True)

        # fix0 = prior (incomplete), fix1 = later (corrective)
        prior_text = Path(prior_patch.local_path).read_text(
            encoding='utf-8', errors='replace')
        later_text = Path(later_patch.local_path).read_text(
            encoding='utf-8', errors='replace')
        (pair_dir / 'fix0.patch').write_text(prior_text)
        (pair_dir / 'fix1.patch').write_text(later_text)

        seed, audit = d['_seed'], d['_audit']
        cb = _codebase(audit)
        meta = {
            'prior_cve': P,
            'later_cve': L,
            'source': 'project_zero_0day_itw',
            'confirmed': True,
            # relationship verdicts
            'relationship_kind': READY_KINDS[d['diff_kind']],
            'deep_diff_kind': d['diff_kind'],
            'deep_confidence': round(float(d.get('confidence', 0.0)), 2),
            'deep_cited_change': d.get('cited_change', ''),
            'deep_reasoning': d.get('reasoning', ''),
            'llm_relationship_kind': seed.get('llm_relationship_kind'),
            'llm_confidence': seed.get('llm_confidence'),
            # codebase
            'codebase': cb,
            'software': _label_for(cb),
            'repo_url': _repo_url_for(prior_patch.commit_url),
            'later_repo_url': _repo_url_for(later_patch.commit_url),
            # commits + patch urls (fix0 = prior, fix1 = later)
            'fix0_commit': _commit_id(prior_patch.commit_url),
            'fix1_commit': _commit_id(later_patch.commit_url),
            'prior_patch_url': prior_patch.commit_url,
            'later_patch_url': later_patch.commit_url,
            'fix0_date': _extract_date(prior_text),
            'fix1_date': _extract_date(later_text),
            # touched code
            'affected_files_fix0': _changed_files(prior_text),
            'affected_files_fix1': _changed_files(later_text),
            'shared_files': d.get('shared_files', []),
            # provenance
            'evidence_url': seed.get('llm_best_evidence_url')
                            or ((seed.get('evidence') or [{}])[0]).get('url'),
            'cited_sentence': seed.get('llm_cited_sentence', ''),
            # pipeline plumbing
            'fuzzing_excluded': cb in {
                'microsoft-windows', 'ie-jscript', 'windows-kernel',
                'windows-clfs', 'edge', 'microsoft-office', 'apple-ios',
                'apple-os', 'apple-macos', 'apple-coretext', 'adobe-reader',
                'winrar', 'samsung-android', 'samsung-npu-driver',
            },
            'notes': '',
        }
        with open(pair_dir / 'metadata.json', 'w') as f:
            json.dump(meta, f, indent=2)
        written += 1
        print(f"  OK   {name}  [{meta['relationship_kind']}, {cb}]")

    print(f"\n{written} written, {skipped} skipped -> {PAIRS_DIR}")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--force', action='store_true',
                    help='Rewrite pairs that already exist')
    args = ap.parse_args()
    return build(force=args.force)


if __name__ == '__main__':
    sys.exit(main())
