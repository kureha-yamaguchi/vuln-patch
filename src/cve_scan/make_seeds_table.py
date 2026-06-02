"""Render `seeds_table.md` — a human-readable Markdown table of every
confirmed seed pair, grouped by evidence strength.

Combines `seeds.json` (rough verifier output), `deep_relate.json` (deep
diff verdict), and `codebase_audit.json` (heuristic same-codebase check)
into one ordered table.

Three groups:
  STRONG — deep diff-relate verifier confirmed `incomplete_fix_confirmed`
           or `same_root_cause_confirmed`.
  MEDIUM — high-confidence prose-LLM verdict (incomplete_fix ≥0.9, any
           regression, or same_root_cause+same_codebase ≥0.85), but the
           deep verifier could not run (no patches for both sides).
  UNSURE — everything else, including pairs the deep verifier judged
           `unrelated` and pairs where the codebase audit disagrees with
           the LLM's same_codebase claim.
"""
import json
import os
import re
import sys
from typing import Dict, List, Optional, Tuple

from . import config


def deep_label(d: Optional[dict]) -> str:
    """Full deep-verdict text. Includes the LLM's reasoning when the
    verdict is `unrelated` or `insufficient_data` so the reader can see
    why the deep pass rejected what the prose-LLM had flagged."""
    if d is None:
        return 'n/a'
    if d.get('skip_reason'):
        return f"skip — {d['skip_reason']}"
    base = f"{d['diff_kind']} (conf {d['confidence']:.2f})"
    if d['diff_kind'] in ('unrelated', 'insufficient_data'):
        reason = (d.get('reasoning') or '').replace('|', '\\|').replace('\n', ' ').strip()
        if reason:
            return f"{base} — {reason[:260]}"
    return base


# Per-codebase product + vendor labels for the `software` column. Falls
# back to a humanised version of the codebase key if not in the map.
_CODEBASE_TO_LABEL = {
    'chrome':                'Google Chrome (V8/Blink)',
    'mozilla-gecko':         'Mozilla Firefox (Gecko/SpiderMonkey)',
    'apple-webkit':          'Apple WebKit (Safari engine, open source)',
    'apple-ios':             'Apple iOS (closed source)',
    'apple-os':              'Apple macOS (closed source)',
    'apple-macos':           'Apple macOS (closed source)',
    'microsoft-windows':     'Microsoft Windows (closed source)',
    'ie-jscript':            'Microsoft IE / JScript (closed source, EOL)',
    'windows-kernel':        'Microsoft Windows kernel (closed source)',
    'windows-clfs':          'Microsoft Windows CLFS driver (closed source)',
    'edge':                  'Microsoft Edge legacy / Chakra (closed source, EOL)',
    'microsoft-office':      'Microsoft Office (closed source)',
    'mali-gpu-driver':       'ARM Mali GPU kernel driver (AOSP, open source)',
    'android-kernel':        'Android (AOSP, open source)',
    'qualcomm-android':      'Qualcomm Adreno GPU (Android, partial open source)',
    'linux-kernel':          'Linux kernel (open source)',
    'samsung-android':       'Samsung Android device driver (closed source)',
    'winrar':                'RARLAB WinRAR (closed source)',
    'adobe-reader':          'Adobe Acrobat Reader (closed source)',
    'fuzzilli':              'Project Zero Fuzzilli (open source, fuzzer)',
    'unknown':               'unknown',
}


def _label_for(cb: str) -> str:
    return _CODEBASE_TO_LABEL.get(cb, cb)


def software_label(audit: Optional[dict]) -> str:
    """Human-readable `later_codebase / prior_codebase` with full
    product + vendor names. Collapses to a single value when both
    sides agree; otherwise shows both with a comparator."""
    if not audit:
        return _label_for('unknown')
    later_cb = audit.get('later_codebase') or 'unknown'
    prior_cb = audit.get('prior_codebase') or 'unknown'
    later_label = _label_for(later_cb)
    prior_label = _label_for(prior_cb)
    if later_cb == prior_cb and later_cb != 'unknown':
        return later_label
    if 'unknown' in (later_cb, prior_cb):
        return f'{later_label} / {prior_label}'
    return f'{later_label} ≠ {prior_label}'


def audit_label(audit: Optional[dict]) -> str:
    """Codebase-audit verdict with a short rationale."""
    if not audit:
        return 'n/a'
    verdict = audit.get('verdict', 'n/a')
    later_cb = audit.get('later_codebase') or 'unknown'
    prior_cb = audit.get('prior_codebase') or 'unknown'
    later_src = audit.get('later_source') or ''
    prior_src = audit.get('prior_source') or ''
    if verdict == 'agrees':
        return f'agrees (both {later_cb})'
    if verdict == 'disagrees':
        # `note` already encodes the human-readable reason.
        note = audit.get('note') or f'{later_cb} vs {prior_cb}'
        return f'disagrees — {note}'
    if verdict == 'unknown_codebase':
        which = []
        if later_cb == 'unknown':
            which.append(f'later ({later_src})')
        if prior_cb == 'unknown':
            which.append(f'prior ({prior_src})')
        return f"unknown — couldn't infer codebase for {', '.join(which)}"
    return verdict


def _short_host(url: str) -> str:
    """Display label for a URL — pick a tight, host-aware shortening
    so the table cell stays readable while the link is clickable."""
    if not url:
        return ''
    m = re.match(r'https?://([^/]+)(/.*)?$', url)
    if not m:
        return url
    host = m.group(1)
    path = m.group(2) or ''
    if 'github.com' in host:
        gm = re.match(r'/([^/]+)/([^/]+)/(commit|blob/[^/]+)/(.+)$', path)
        if gm:
            owner, repo, _, rest = gm.groups()
            tail = rest[:14] if 'commit' in gm.group(3) else rest
            return f'gh:{owner}/{repo}@{tail}'
    if 'chromium.googlesource.com' in host:
        gm = re.match(r'.*?\+/([0-9a-f]+)', path)
        if gm:
            return f'cr-source@{gm.group(1)[:12]}'
    if 'android.googlesource.com' in host:
        gm = re.match(r'.*?\+/([0-9a-f]+)', path)
        if gm:
            return f'aosp@{gm.group(1)[:12]}'
    if 'chromium-review.googlesource.com' in host:
        gm = re.search(r'\+/(\d+)', path)
        if gm:
            return f'cr-review+/{gm.group(1)}'
    if 'git.kernel.org' in host:
        gm = re.search(r'id=([0-9a-f]+)', path)
        if gm:
            return f'k.org@{gm.group(1)[:12]}'
    if 'git.codelinaro.org' in host:
        gm = re.search(r'/-/commit/([0-9a-f]+)', path)
        if gm:
            return f'codelinaro@{gm.group(1)[:12]}'
    if 'bugzilla.mozilla.org' in host:
        gm = re.search(r'id=(\d+)', path)
        if gm:
            return f'bz.moz#{gm.group(1)}'
    if 'bugs.chromium.org' in host:
        gm = re.search(r'id=(\d+)', path)
        if gm:
            project = 'cr' if '/p/chromium/' in path else 'p0'
            return f'bugs.cr/{project}#{gm.group(1)}'
    if 'docs.google.com' in host:
        return 'p0-sheet'
    if 'projectzero.google' in host or 'googleprojectzero' in host:
        # RCA or blog post: take the year/cve from the path if present.
        gm = re.search(r'(CVE-\d{4}-\d+)\.(md|html)', path)
        if gm:
            return f'rca:{gm.group(1)}'
        if 'mind-the-gap' in path:
            return 'mind-the-gap'
        if 'deja-vu' in path:
            return 'deja-vu-lnerability'
        return 'p0-blog'
    return host[:24]


def _link(label_url: Tuple[str, str]) -> str:
    label, url = label_url
    if not url:
        return ''
    return f'[{label}]({url})'


def _cve_nvd_link(bug_id: str) -> str:
    """Return a clickable NVD link for a CVE-id, or a bug-tracker link
    for a non-CVE identifier."""
    if bug_id.startswith('CVE-'):
        return f'[{bug_id}](https://nvd.nist.gov/vuln/detail/{bug_id})'
    if bug_id.startswith('chromium-p0:'):
        n = bug_id.split(':', 1)[1]
        return f'[{bug_id}](https://bugs.chromium.org/p/project-zero/issues/detail?id={n})'
    if bug_id.startswith('chromium:'):
        n = bug_id.split(':', 1)[1]
        return f'[{bug_id}](https://bugs.chromium.org/p/chromium/issues/detail?id={n})'
    if bug_id.startswith('mozilla:'):
        n = bug_id.split(':', 1)[1]
        return f'[{bug_id}](https://bugzilla.mozilla.org/show_bug.cgi?id={n})'
    if bug_id.startswith('github:'):
        rest = bug_id.split(':', 1)[1]
        m = re.match(r'([^/]+)/([^@]+)@([0-9a-f]+)', rest)
        if m:
            owner, repo, sha = m.group(1), m.group(2), m.group(3)
            return f'[{bug_id}](https://github.com/{owner}/{repo}/commit/{sha})'
    return bug_id


def links_label(s: dict) -> str:
    """Compact column showing where the pair was found and any patch
    URLs we resolved. Links are rendered as markdown so they're
    clickable. `nvd:` is the NVD entry for the LATER CVE (or the
    bug-tracker entry for a non-CVE identifier). `tried:` lists
    upstream-commit URLs from the LATER bug's RCA prose — i.e.
    patch candidates the resolver knew about (it then dedupes +
    filters via the denylist/source-files rule before fetching, so
    not every one was actually fetched)."""
    parts: List[str] = []
    # NVD links for both sides — useful starting point for a human
    # following up on a pair.
    parts.append(f'nvd-L:{_cve_nvd_link(s["later_cve"])}')
    parts.append(f'nvd-P:{_cve_nvd_link(s["prior_cve"])}')
    ev = (s.get('evidence') or [{}])[0]
    ev_url = ev.get('url') or ''
    if ev_url:
        parts.append(_link((f'ev:{_short_host(ev_url)}', ev_url)))
    if s.get('later_patch_url'):
        parts.append(_link((f'L→{_short_host(s["later_patch_url"])}',
                            s['later_patch_url'])))
    if s.get('prior_patch_url'):
        parts.append(_link((f'P→{_short_host(s["prior_patch_url"])}',
                            s['prior_patch_url'])))
    # Up to 3 additional commit URLs that the LATER's RCA pointed at
    # (resolver candidate pool). Skip duplicates of the already-shown
    # resolved patch URL.
    upstream = s.get('upstream_commits') or []
    later_resolved = s.get('later_patch_url') or ''
    extras: List[str] = []
    for u in upstream:
        if u == later_resolved:
            continue
        if u in extras:
            continue
        extras.append(u)
        if len(extras) >= 3:
            break
    for u in extras:
        parts.append(_link((f'tried:{_short_host(u)}', u)))
    return ' '.join(parts)


def render_row(s: dict, d: Optional[dict], audit: Optional[dict],
               why: str = '') -> str:
    cite = (s.get('llm_cited_sentence') or '').replace('|', '\\|').replace('\n', ' ')[:120]
    why_cell = (why or '').replace('|', '\\|').replace('\n', ' ')
    return (
        f"| {s['later_cve']} | {s['prior_cve']} | "
        f"{software_label(audit)} | "
        f"{s['llm_relationship_kind']} | "
        f"{s['llm_confidence']:.2f} | "
        f"{deep_label(d)} | "
        f"{audit_label(audit)} | "
        f"{why_cell} | "
        f"{links_label(s)} | "
        f"{cite} |"
    )


HEADER = (
    "| later | prior | software | llm_kind | conf | deep | audit | why | links | cited_sentence |"
)
SEP = "|---|---|---|---|---|---|---|---|---|---|"


def render_table(title: str, rows: list,
                 bucket_reasons: Optional[Dict[Tuple[str, str], str]] = None
                 ) -> str:
    out: List[str] = []
    out.append(f"## {title} — {len(rows)} pairs\n")
    if not rows:
        out.append('_None._\n')
        return '\n'.join(out)
    out.append(HEADER)
    out.append(SEP)
    for s, d, a in rows:
        why = ''
        if bucket_reasons is not None:
            why = bucket_reasons.get((s['later_cve'], s['prior_cve']), '')
        out.append(render_row(s, d, a, why))
    out.append('')
    return '\n'.join(out)


def _categorize(s: dict, d: Optional[dict],
                a: Optional[dict]) -> Tuple[str, str]:
    """Bucket a confirmed seed pair. Returns (bucket, why)."""
    deep_kind = d.get('diff_kind') if d else None
    skip_reason = (d or {}).get('skip_reason') or ''

    # DROPPED: self-pair OR codebase-audit disagrees (cross-vendor).
    if skip_reason.startswith('self-pair'):
        return ('DROPPED', 'self-pair: both sides resolved to the same fix')
    if a and a.get('verdict') == 'disagrees':
        lc = a.get('later_codebase', '?')
        pc = a.get('prior_codebase', '?')
        return ('DROPPED', f'cross-codebase ({lc} vs {pc}) — likely '
                          f'same-actor / same-operation, not a code-level sibling')

    # INCOMPLETE_FIX: deep verifier confirmed at the diff level, OR
    # LLM-prose says incomplete_fix/regression with high confidence
    # and deep skipped (couldn't run).
    if deep_kind == 'incomplete_fix_confirmed':
        return ('INCOMPLETE_FIX', 'deep verifier confirmed incomplete-fix at code level')
    if s['llm_relationship_kind'] in ('incomplete_fix', 'regression') \
            and s['llm_confidence'] >= 0.85 \
            and skip_reason.startswith('no patches'):
        return ('INCOMPLETE_FIX',
                f"prose-LLM said {s['llm_relationship_kind']} (conf "
                f"{s['llm_confidence']:.2f}); patches not fetchable")

    # SAME_ROOT_CAUSE: deep verifier ratified same-root-cause OR
    # one-extends-other, OR LLM-prose says same_root_cause with
    # same_codebase and deep skipped.
    if deep_kind in ('same_root_cause_confirmed', 'one_extends_other'):
        return ('SAME_ROOT_CAUSE',
                f'deep verifier ratified ({deep_kind})')
    if s['llm_relationship_kind'] == 'same_root_cause' \
            and s['llm_same_codebase'] \
            and s['llm_confidence'] >= 0.80 \
            and skip_reason.startswith('no patches'):
        return ('SAME_ROOT_CAUSE',
                f"prose-LLM said same_root_cause (conf "
                f"{s['llm_confidence']:.2f}); patches not fetchable")

    # UNRELATED: deep verifier said unrelated/insufficient_data
    # — the pair was probably a false positive from the prose pass.
    if deep_kind in ('unrelated', 'insufficient_data'):
        return ('UNRELATED',
                f'deep verifier rejected as {deep_kind} after reading '
                f'both diffs')

    # Anything else: low confidence, no deep info, no fit above.
    return ('UNSURE', 'low-confidence prose, no deep verification, no clear fit')


def main() -> int:
    # Read pipeline JSON from pipeline/, write the human-readable
    # seeds_table.md to the parent output dir so a reader hits the
    # report first when they ls the directory.
    pipeline_dir = config.CVE_SCAN_PIPELINE_DIR
    out_dir = config.CVE_SCAN_OUTPUT_DIR
    seeds_path = os.path.join(pipeline_dir, 'seeds.json')
    deep_path = os.path.join(pipeline_dir, 'deep_relate.json')
    audit_path = os.path.join(pipeline_dir, 'codebase_audit.json')

    if not os.path.isfile(seeds_path):
        print(f"error: {seeds_path} not found", file=sys.stderr)
        return 2
    with open(seeds_path, 'r', encoding='utf-8') as f:
        seeds = json.load(f)
    deeps: List[dict] = []
    if os.path.isfile(deep_path):
        with open(deep_path, 'r', encoding='utf-8') as f:
            deeps = json.load(f)
    audits: List[dict] = []
    if os.path.isfile(audit_path):
        with open(audit_path, 'r', encoding='utf-8') as f:
            audits = json.load(f)
    dx = {(d['later_cve'], d['prior_cve']): d for d in deeps}
    ax = {(a['later'], a['prior']): a for a in audits}

    confirmed = [s for s in seeds if s.get('confirmed')]
    buckets: Dict[str, list] = {
        'INCOMPLETE_FIX': [],
        'SAME_ROOT_CAUSE': [],
        'UNRELATED': [],
        'DROPPED': [],
        'UNSURE': [],
    }
    bucket_reasons: Dict[Tuple[str, str], str] = {}
    for s in confirmed:
        d = dx.get((s['later_cve'], s['prior_cve']))
        a = ax.get((s['later_cve'], s['prior_cve']))
        bucket, why = _categorize(s, d, a)
        buckets[bucket].append((s, d, a))
        bucket_reasons[(s['later_cve'], s['prior_cve'])] = why

    out_md = [
        '# Confirmed P0 variant-pair findings',
        '',
        f'**Total: {len(confirmed)} pairs** — bucketed for the '
        f'incomplete-fix-database use case:',
        '',
        f'- **INCOMPLETE_FIX** ({len(buckets["INCOMPLETE_FIX"])}) — '
        f'the gold target: patches that were incomplete and caused a '
        f'follow-up CVE. Either the deep diff verifier confirmed this '
        f'at the code level, or the prose-LLM said incomplete_fix / '
        f'regression with high confidence and we just couldn\'t fetch '
        f'patches to verify.',
        f'- **SAME_ROOT_CAUSE** ({len(buckets["SAME_ROOT_CAUSE"])}) — '
        f'real sibling pairs (shared root cause / same bug class) but '
        f'NOT necessarily incomplete-fix relationships. Useful for the '
        f'broader variant-CVE database, less central to the '
        f'patches-that-failed thesis.',
        f'- **UNRELATED** ({len(buckets["UNRELATED"])}) — the prose-LLM '
        f'flagged these as variants but the deep verifier read the '
        f'actual diffs and rejected them. After the trusted-owner '
        f'filter on github-search results, the remaining `unrelated` '
        f'verdicts are mostly correct rejections; before the filter '
        f'they were dominated by wrong-repo resolutions.',
        f'- **DROPPED** ({len(buckets["DROPPED"])}) — pairs we exclude '
        f'from the dataset: codebase audit says cross-vendor (e.g. '
        f'Apple iOS bug paired with a Chrome bug because they were '
        f'used in the same attack), or both sides resolve to the same '
        f'commit (self-pair).',
        f'- **UNSURE** ({len(buckets["UNSURE"])}) — low-confidence '
        f'prose verdicts that didn\'t fit any of the above.',
        '',
        '_Column legend:_',
        '',
        '- **software** — the codebases of the two sides, inferred from '
        'the P0 sheet (Vendor / Product) for CVEs or from the bug-id '
        'prefix for chromium / mozilla / github identifiers. Format: '
        'single value when both sides agree, `a ≠ b` when known but '
        'different, `a / b` when one side is `unknown`.',
        '- **llm_kind** — the prose-LLM verdict: '
        '`incomplete_fix | regression | same_root_cause | exploit_chain '
        '| see_also | unrelated`.',
        '- **conf** — the prose-LLM verdict confidence (0–1).',
        '- **deep** — verdict from the deep diff-relate LLM pass: '
        '`incomplete_fix_confirmed | same_root_cause_confirmed | '
        'one_extends_other | unrelated | insufficient_data`, with the '
        'deep-LLM confidence. For `unrelated` and `insufficient_data`, '
        'the LLM\'s reasoning is appended after an em-dash so the '
        'reader can see WHY the deep pass rejected the pair (e.g. '
        '*"prior touches drivers/android/binder.c; later touches '
        'drivers/hid/hid-input.c — disjoint subsystems"*). When the '
        'deep verifier could NOT be called, this column says '
        '`skip — <reason>` (e.g. `no patches for both sides '
        '(later=missing prior=ok)` when one or both patches could not '
        'be fetched, `self-pair (later and prior resolved to same '
        'commit)`, or `LLM budget exhausted`).',
        '- **audit** — the codebase-audit verdict: '
        '`agrees (both <codebase>)` when the heuristic confirms the '
        'LLM\'s `same_codebase` claim; '
        '`disagrees — <reason>` when the inferred codebases differ '
        '(usually means the LLM mistook same-actor / same-operation '
        'context for code-level sharing); '
        '`unknown — couldn\'t infer codebase for ...` when one side '
        'isn\'t in the P0 sheet and has no RCA we can mine for vendor '
        'info (typical for non-ITW prior CVEs).',
        '- **links** — clickable evidence and patch URLs. '
        '`ev:<host>` is the upstream document where the pair was '
        'first extracted (an RCA file, a P0 narrative post, or the '
        'P0 sheet). `L→<host>` is the fix-commit URL we resolved for '
        'the LATER bug; `P→<host>` is the same for the PRIOR bug — '
        'both are absent when the resolver could not fetch a patch '
        '(see the `deep` column for `later=missing` / `prior=missing` '
        'in that case). `tried:<host>` lists up to 3 other commit '
        'URLs the LATER bug\'s RCA mentioned, so you can see what '
        'patch candidates the resolver had to choose from.',
        '- **why** — short note explaining why this pair landed in '
        'this bucket (e.g. *"deep verifier confirmed incomplete-fix '
        'at code level"*, *"prose-LLM said same_root_cause; patches '
        'not fetchable"*).',
        '',
        render_table('INCOMPLETE_FIX', buckets['INCOMPLETE_FIX'], bucket_reasons),
        render_table('SAME_ROOT_CAUSE', buckets['SAME_ROOT_CAUSE'], bucket_reasons),
        render_table('UNRELATED', buckets['UNRELATED'], bucket_reasons),
        render_table('DROPPED', buckets['DROPPED'], bucket_reasons),
        render_table('UNSURE', buckets['UNSURE'], bucket_reasons),
    ]
    md = '\n'.join(out_md)

    out_path = os.path.join(out_dir, 'seeds_table.md')
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(md)
    print(f"wrote {out_path}")
    for k, v in buckets.items():
        print(f"  {k:18s} {len(v)}")
    return 0


if __name__ == '__main__':
    sys.exit(main())
