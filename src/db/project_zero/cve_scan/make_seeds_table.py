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
    """Bucket a confirmed seed pair by URL completeness + relatedness.

    Primary axis is patch-URL availability (do we have the source code
    on both sides?). Secondary axis is the relatedness verdict (does
    the deep verifier confirm or reject the sibling claim?).

    Returns ``(bucket, why)``.
    """
    later_url = s.get('later_patch_url')
    prior_url = s.get('prior_patch_url')
    both_urls = bool(later_url and prior_url)
    one_url   = bool(later_url) != bool(prior_url)
    no_urls   = not later_url and not prior_url

    deep_kind   = (d or {}).get('diff_kind')
    skip_reason = (d or {}).get('skip_reason') or ''
    prose_kind  = s['llm_relationship_kind']
    prose_conf  = s['llm_confidence']

    # ---------- TIER 4: DROPPED ----------------------------------------
    # Explicit exclusions: same fix on both sides, or LATER and PRIOR
    # come from different vendors / products (typically same-attack
    # same-actor, not same-code-level).
    if skip_reason.startswith('self-pair'):
        return ('DROPPED', 'self-pair: both sides resolved to the same fix')
    if a and a.get('verdict') == 'disagrees':
        lc = a.get('later_codebase', '?')
        pc = a.get('prior_codebase', '?')
        return ('DROPPED', f'cross-vendor ({lc} vs {pc}) — same-actor or '
                          f'same-operation, not a code-level sibling')

    # ---------- TIER 1: READY ------------------------------------------
    # Both fix patches fetched AND the deep diff verifier confirmed the
    # sibling claim at the code level.  These are ready to ingest into
    # the fuzzing pipeline immediately.
    if both_urls and deep_kind == 'incomplete_fix_confirmed':
        return ('READY (incomplete_fix)',
                'both patches fetched; deep verifier confirmed '
                'incomplete-fix at code level')
    if both_urls and deep_kind == 'same_root_cause_confirmed':
        return ('READY (same_root_cause)',
                'both patches fetched; deep verifier confirmed shared '
                'root cause at code level')
    if both_urls and deep_kind == 'one_extends_other':
        return ('READY (one_extends_other)',
                'both patches fetched; deep verifier says one extends '
                'the other (refinement / hardening)')

    # ---------- TIER 3: REJECTED ---------------------------------------
    # Both patches fetched, deep verifier read the diffs, said unrelated.
    # These are the most reliably-rejected pairs.
    if both_urls and deep_kind in ('unrelated', 'insufficient_data'):
        return (f'REJECTED ({deep_kind})',
                f'both patches fetched; deep verifier rejected as '
                f'{deep_kind} after reading the actual diffs')

    # ---------- TIER 2: SUSPECTED — prose verified, URL gap -----------
    # Prose-LLM verified the sibling claim from the upstream document
    # (Project Zero RCA, P0 sheet, narrative blog post) but the
    # resolver couldn't fetch all the patches we'd need to verify at
    # the code level.  Sub-split by relationship kind + URL status so
    # the most actionable rows surface first.

    # 2a: one patch already resolved — closest to actionable
    if one_url and prose_kind in ('incomplete_fix', 'regression', 'same_root_cause'):
        side = 'later' if later_url else 'prior'
        missing = 'prior' if later_url else 'later'
        return (f'SUSPECTED (1 URL, need {missing})',
                f'prose-LLM said {prose_kind} (conf {prose_conf:.2f}); '
                f'{side} patch resolved, {missing} side missing')

    # 2b: no URLs at all — prose evidence only
    if no_urls and prose_kind in ('incomplete_fix', 'regression') \
            and prose_conf >= 0.85:
        return ('SUSPECTED (no URLs)',
                f'prose-LLM said {prose_kind} (conf {prose_conf:.2f}); '
                f'no patches fetchable on either side')
    if no_urls and prose_kind == 'same_root_cause' \
            and s['llm_same_codebase'] and prose_conf >= 0.80:
        return ('SUSPECTED (no URLs)',
                f'prose-LLM said same_root_cause + same_codebase '
                f'(conf {prose_conf:.2f}); no patches fetchable')

    # ---------- UNSURE -------------------------------------------------
    return ('UNSURE',
            f'low-confidence prose ({prose_kind} conf {prose_conf:.2f}) '
            f'and no deep verification')


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

    # Buckets are produced dynamically by _categorize.  We keep a stable
    # ordering for the report (TIER 1 → TIER 4) by ordering keys via the
    # `_BUCKET_ORDER` list below; any bucket not in the list lands at
    # the end.
    from collections import defaultdict
    buckets: Dict[str, list] = defaultdict(list)
    bucket_reasons: Dict[Tuple[str, str], str] = {}
    for s in confirmed:
        d = dx.get((s['later_cve'], s['prior_cve']))
        a = ax.get((s['later_cve'], s['prior_cve']))
        bucket, why = _categorize(s, d, a)
        buckets[bucket].append((s, d, a))
        bucket_reasons[(s['later_cve'], s['prior_cve'])] = why

    # Stable display order (TIER 1 → TIER 4 → UNSURE).
    _BUCKET_ORDER = [
        'READY (incomplete_fix)',
        'READY (same_root_cause)',
        'READY (one_extends_other)',
        'SUSPECTED (1 URL, need prior)',
        'SUSPECTED (1 URL, need later)',
        'SUSPECTED (no URLs)',
        'REJECTED (unrelated)',
        'REJECTED (insufficient_data)',
        'DROPPED',
        'UNSURE',
    ]
    # Anything dynamically generated (e.g. an extra 1-URL flavour) goes
    # at the end, sorted, so the report is deterministic.
    ordered_keys = [k for k in _BUCKET_ORDER if k in buckets]
    extras = sorted(k for k in buckets if k not in _BUCKET_ORDER)
    ordered_keys += extras

    def _count(key_prefix: str) -> int:
        return sum(len(v) for k, v in buckets.items()
                   if k.startswith(key_prefix))

    n_ready     = _count('READY')
    n_suspected = _count('SUSPECTED')
    n_rejected  = _count('REJECTED')
    n_dropped   = _count('DROPPED')
    n_unsure    = _count('UNSURE')

    out_md = [
        '# Confirmed P0 variant-pair findings',
        '',
        f'**Total: {len(confirmed)} pairs** — bucketed by '
        f'**patch-URL completeness × relatedness verdict**.',
        '',
        f'### TIER 1 — READY ({n_ready}) ✓ both patches + deep verifier confirmed',
        '',
        '> Both fix-commit URLs resolved AND the deep diff verifier read '
        'both diffs and confirmed the sibling claim at the code level. '
        'These are ready to ingest into a downstream pipeline '
        '(e.g. fuzzing harness generator) immediately. Sub-buckets by '
        'deep verdict — `incomplete_fix` is the gold target, '
        '`same_root_cause` is broader, `one_extends_other` is the '
        'weakest (refinement / hardening).',
        '',
        f'### TIER 2 — SUSPECTED ({n_suspected}) ⚠ prose verified, URL gap',
        '',
        '> Prose-LLM verified the sibling claim from the upstream '
        'document (Project Zero RCA, sheet, narrative blog post) but '
        'the resolver couldn\'t fetch all the patches needed for the '
        'deep code-level check. The relationship is most likely real; '
        'they just need a human to grab the missing patch URL. '
        '`SUSPECTED (1 URL, need <side>)` rows are closest to '
        'actionable; `SUSPECTED (no URLs)` rows need both URLs.',
        '',
        f'### TIER 3 — REJECTED ({n_rejected}) × both patches + deep verifier rejected',
        '',
        '> Both patches were fetched and the deep verifier judged the '
        'diffs unrelated or insufficient. After the trusted-owner '
        'filter on github-search results, these are mostly correct '
        'rejections; before the filter they were dominated by wrong-repo '
        'resolutions.',
        '',
        f'### TIER 4 — DROPPED ({n_dropped}) ✗ explicit exclusions',
        '',
        '> Two reasons: codebase audit says cross-vendor (e.g. Apple iOS '
        'bug paired with a Chrome bug because they were used in the '
        'same attack campaign, not because they share code), or both '
        'sides resolve to the same fix commit (self-pair).',
        '',
        f'### UNSURE ({n_unsure}) low-confidence prose, no clear fit',
        '',
        '> Edge cases that don\'t fit any tier above. Should be empty in '
        'practice — review individually if any land here.',
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
    ]
    for k in ordered_keys:
        out_md.append(render_table(k, buckets[k], bucket_reasons))
    md = '\n'.join(out_md)

    out_path = os.path.join(out_dir, 'seeds_table.md')
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(md)
    print(f"wrote {out_path}")
    for k in ordered_keys:
        print(f"  {k:34s} {len(buckets[k])}")
    return 0


if __name__ == '__main__':
    sys.exit(main())
