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


def software_label(audit: Optional[dict]) -> str:
    """Human-readable `later_codebase / prior_codebase` pair. When both
    sides are known and equal, collapse to a single value; otherwise
    show both with a comparator (`=` same, `≠` different, `?` unknown)."""
    if not audit:
        return 'unknown / unknown'
    later_cb = audit.get('later_codebase') or 'unknown'
    prior_cb = audit.get('prior_codebase') or 'unknown'
    if later_cb == prior_cb and later_cb != 'unknown':
        return later_cb
    if 'unknown' in (later_cb, prior_cb):
        return f'{later_cb} / {prior_cb}'
    # Both known but different — use a clear separator.
    return f'{later_cb} ≠ {prior_cb}'


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


def links_label(s: dict) -> str:
    """Compact column showing where the pair was found and any patch
    URLs we resolved. Links are rendered as markdown so they're
    clickable. `tried:` lists upstream-commit URLs from the LATER
    bug's RCA prose — i.e. patch candidates the resolver knew about
    (it then dedupes + filters via the denylist/source-files rule
    before fetching, so not every one was actually fetched)."""
    parts: List[str] = []
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


def render_row(s: dict, d: Optional[dict], audit: Optional[dict]) -> str:
    cite = (s.get('llm_cited_sentence') or '').replace('|', '\\|').replace('\n', ' ')[:120]
    return (
        f"| {s['later_cve']} | {s['prior_cve']} | "
        f"{software_label(audit)} | "
        f"{s['llm_relationship_kind']} | "
        f"{s['llm_confidence']:.2f} | "
        f"{deep_label(d)} | "
        f"{audit_label(audit)} | "
        f"{links_label(s)} | "
        f"{cite} |"
    )


HEADER = (
    "| later | prior | software | llm_kind | conf | deep | audit | links | cited_sentence |"
)
SEP = "|---|---|---|---|---|---|---|---|---|"


def render_table(title: str, rows: list) -> str:
    out: List[str] = []
    out.append(f"## {title} — {len(rows)} pairs\n")
    if not rows:
        out.append('_None._\n')
        return '\n'.join(out)
    out.append(HEADER)
    out.append(SEP)
    for s, d, a in rows:
        out.append(render_row(s, d, a))
    out.append('')
    return '\n'.join(out)


def main() -> int:
    out_dir = config.CVE_SCAN_OUTPUT_DIR
    seeds_path = os.path.join(out_dir, 'seeds.json')
    deep_path = os.path.join(out_dir, 'deep_relate.json')
    audit_path = os.path.join(out_dir, 'codebase_audit.json')

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
    strong, medium, unsure = [], [], []
    for s in confirmed:
        d = dx.get((s['later_cve'], s['prior_cve']))
        a = ax.get((s['later_cve'], s['prior_cve']))
        deep_kind = d['diff_kind'] if d else None
        # Audit disagreement bumps a pair from MEDIUM/STRONG to UNSURE so it
        # gets manual attention.
        audit_disagrees = bool(a and a['verdict'] == 'disagrees')
        if (deep_kind in ('incomplete_fix_confirmed',
                          'same_root_cause_confirmed')
                and not audit_disagrees):
            strong.append((s, d, a))
        elif audit_disagrees:
            unsure.append((s, d, a))
        elif (s['llm_relationship_kind'] == 'incomplete_fix'
              and s['llm_confidence'] >= 0.9) \
             or s['llm_relationship_kind'] == 'regression' \
             or (s['llm_relationship_kind'] == 'same_root_cause'
                 and s['llm_same_codebase']
                 and s['llm_confidence'] >= 0.85):
            medium.append((s, d, a))
        else:
            unsure.append((s, d, a))

    out_md = [
        '# Confirmed P0 variant-pair findings',
        '',
        f'**Total: {len(confirmed)} pairs** '
        f'(strong {len(strong)}, medium {len(medium)}, unsure {len(unsure)})',
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
        '',
        '_Group definitions:_',
        '',
        '- **STRONG** — deep diff-relate verifier confirmed code-level '
        'relatedness (`incomplete_fix_confirmed` or '
        '`same_root_cause_confirmed`) and the codebase audit did not '
        'flag a disagreement.',
        '- **MEDIUM** — high-confidence LLM-prose verdict but the deep '
        'verifier could not run (e.g. no patches fetchable for both '
        'sides).',
        '- **UNSURE** — lower-confidence LLM-prose verdict, or deep '
        'verifier returned `unrelated`/`insufficient_data`, or the '
        'codebase audit disagrees with the LLM\'s `same_codebase` '
        'claim.',
        '',
        render_table('STRONG', strong),
        render_table('MEDIUM', medium),
        render_table('UNSURE', unsure),
    ]
    md = '\n'.join(out_md)

    out_path = os.path.join(out_dir, 'seeds_table.md')
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(md)
    print(f"wrote {out_path}")
    print(f"  strong={len(strong)}, medium={len(medium)}, unsure={len(unsure)}")
    return 0


if __name__ == '__main__':
    sys.exit(main())
