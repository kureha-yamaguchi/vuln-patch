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
import sys
from typing import Dict, List, Optional, Tuple

from . import config


def deep_label(d: Optional[dict]) -> str:
    if d is None:
        return 'n/a'
    if d.get('skip_reason'):
        return f"skip: {d['skip_reason'][:34]}"
    return f"{d['diff_kind']} c={d['confidence']:.2f}"


def render_row(s: dict, d: Optional[dict], audit: Optional[dict]) -> str:
    cite = (s.get('llm_cited_sentence') or '').replace('|', '\\|').replace('\n', ' ')[:160]
    audit_verdict = audit['verdict'] if audit else 'n/a'
    audit_mark = {
        'agrees': '=',
        'disagrees': '⚠',
        'unknown_codebase': '?',
    }.get(audit_verdict, ' ')
    return (
        f"| {s['later_cve']} | {s['prior_cve']} | "
        f"{s['llm_relationship_kind']} | "
        f"{'✓' if s['llm_same_codebase'] else '✗'}{audit_mark} | "
        f"{s['llm_confidence']:.2f} | "
        f"{deep_label(d)} | "
        f"{cite} |"
    )


HEADER = (
    "| later | prior | llm_kind | scb&audit | conf | deep | cited_sentence |"
)
SEP = "|---|---|---|---|---|---|---|"


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
        '- **scb&audit** — `✓`/`✗` is the LLM `same_codebase` flag; '
        'the second character is the codebase-audit verdict: '
        '`=` agrees, `⚠` disagrees, `?` one side unknown.',
        '- **deep** — verdict from the deep diff-relate LLM pass: '
        '`incomplete_fix_confirmed | same_root_cause_confirmed | '
        'one_extends_other | unrelated | insufficient_data`, or '
        '`skip: <reason>` when no LLM call was made (no patches, '
        'self-pair, budget).',
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
