"""Classify each Project Zero fix as a crashing bug or a semantic bug.

`.claude/CONTEXT.md` fixes the two words. A **crashing bug** is one that
something at run time reports by itself: a sanitizer, a throwable, or the
project's own assert. A **semantic bug** is one that nothing reports, so a
harness must supply the verdict itself.

This step is a GATE, not a formality. The Defects4J baseline runs two pools,
because a Defects4J trigger test either throws or fails an assertion. This
dataset is browser and kernel memory-safety work, so the expectation is that
nearly every fix is crashing. Read `bug_kind.jsonl` before you build anything
per pool. A pool of three fixes does not support a pool of its own.

Two passes, and they are deliberately asymmetric:

  1. A rule pass over the ADDED lines of the diff. A fix that adds a bounds
     check, a null guard, a lifetime or refcount change, a type-cast guard or
     an overflow guard is repairing a fault the run time reports. That is a
     crashing bug.
  2. A model pass on everything the rule pass leaves unsure.

There is no semantic rule set, and that is not an omission. "Nothing reports
this at run time" is the absence of a marker, so no regex can assert it. The
rule pass can therefore say `crashing` or say nothing. The model decides the
rest, and `decided_by` records which pass ruled on each fix.

The classifier reads the clean view from `firewall`, so it never sees the
label either. A classifier that knew which fix was the prior one could sort the
two classes by that instead of by the code.
"""
import argparse
import json
import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

import config                                              # noqa: E402
import re                                                  # noqa: E402
from llm import HarnessGenerator                            # noqa: E402

from baseline_llmjudge.project_zero import firewall        # noqa: E402

CRASHING = 'crashing'
SEMANTIC = 'semantic'

DEFAULT_OUT = (firewall.REPO / 'src' / 'db' / 'project_zero'
               / 'bug_kind.jsonl')

#: Markers of a fault the run time reports by itself. Each entry names the
#: fault class it stands for, so a decision is readable rather than a bare
#: regex hit. A fix that adds any of these repairs a crashing bug.
RULE_MARKERS: Tuple[Tuple[str, str], ...] = (
    ('assert',
     r'\b(?:CHECK|DCHECK|CHECK_[A-Z]{2}|DCHECK_[A-Z]{2}|BUG_ON|WARN_ON'
     r'|static_assert|SLOW_DCHECK)\b'),
    ('bounds_check',
     r'\b(?:size|len|length|count|capacity|index|offset|num_\w+)\b'
     r'\s*(?:<=?|>=?)'),
    ('null_guard',
     r'(?:==|!=)\s*(?:nullptr|NULL)|\bIS_ERR\b|\bis_null\b'),
    ('lifetime_or_refcount',
     r'\b(?:refcount|ref_count|AddRef|RefPtr|scoped_refptr|unique_ptr'
     r'|WeakPtr|Persistent|HandleScope|Handle<|kfree|kzalloc|free\('
     r'|put_page|get_page|synchronize_rcu|rcu_read_lock|spin_lock'
     r'|mutex_lock|use-after-free)\b'),
    ('type_or_cast_guard',
     r'\b(?:Is[A-Z]\w*\(\)|IsJS\w+|InstanceType|DynamicCast|dynamic_cast'
     r'|CheckedCast|MaybeCast|Cast<|IsHeapObject|IsSmi)\b'),
    ('overflow_guard',
     r'\b(?:overflow|Overflow|INT_MAX|SIZE_MAX|UINT32_MAX|CheckedNumeric'
     r'|base::Checked|saturated_cast|checked_cast)\b'),
    ('alias_or_side_effect_model',
     r'\b(?:AliasSet|hasDefaultAliasSet|getAliasSet|hasSideEffects'
     r'|WriteBarrier|write_barrier|disallowArbitraryCode)\b'),
)

_COMPILED = tuple((name, re.compile(pattern))
                  for name, pattern in RULE_MARKERS)

SYSTEM = ('You are an expert security engineer who classifies bug fixes by '
          'how the bug reports itself at run time.')

TASK = (
    "Below is the diff of an upstream security fix. Classify the bug it"
    " repairs by how that bug reports ITSELF at run time.\n\n"
    "- CRASHING: something at run time reports the bug on its own. A"
    " sanitizer aborts, the process crashes, or the project's own assert"
    " fires. Memory-safety faults, type confusion, integer overflow that"
    " leads to a bad access, and use-after-free are all crashing.\n"
    "- SEMANTIC: nothing at run time reports the bug. The code runs to"
    " completion and returns a wrong value, grants wrong access, or takes a"
    " wrong branch. A checker would have to know the right answer to see it.\n\n"
    "Judge the BUG, not the fix. Answer from the diff alone."
)

CONTRACT = ("End your answer with a final line in exactly one of these two"
            " forms, and write nothing after it:\n"
            "KIND: CRASHING\n"
            "KIND: SEMANTIC")

_KIND_LINE = re.compile(r'^\s*[*`_# ]*KIND\s*:\s*([A-Z]+)', re.M | re.I)


def added_lines(diff: str) -> str:
    """The lines the diff adds, without the `+` marker or the file headers."""
    return '\n'.join(ln[1:] for ln in diff.splitlines()
                     if ln.startswith('+') and not ln.startswith('+++'))


def classify_by_rule(fix: firewall.Fix) -> Tuple[Optional[str], str]:
    """`(kind, evidence)`. The kind is None when no marker matches."""
    added = added_lines(fix.diff)
    found = [name for name, pattern in _COMPILED if pattern.search(added)]
    if found:
        return CRASHING, 'markers: ' + ', '.join(found)
    return None, 'no crashing marker in the added lines'


def classify_by_model(fix: firewall.Fix,
                      generator: HarnessGenerator) -> Tuple[str, str]:
    """`(kind, evidence)` from one model call, with one retry.

    An unparsed answer falls back to `crashing`, the majority class of this
    dataset. The fallback is recorded, so its cost stays visible."""
    messages = [
        {'role': 'system', 'content': SYSTEM},
        {'role': 'user', 'content': '\n\n'.join(
            [TASK, _diff_for_prompt(fix), CONTRACT])},
    ]
    for attempt in range(2):
        try:
            text = generator.generate(messages)
        except Exception as exc:
            return CRASHING, f'model error, defaulted: {type(exc).__name__}'
        matches = _KIND_LINE.findall(text or '')
        if matches:
            word = matches[-1].upper()
            if word.startswith('CRASH'):
                return CRASHING, 'model'
            if word.startswith('SEMANT'):
                return SEMANTIC, 'model'
    return CRASHING, 'no KIND line parsed after 2 attempts, defaulted'


def _diff_for_prompt(fix: firewall.Fix, cap: int = 12000) -> str:
    diff = fix.diff if len(fix.diff) <= cap else (
        fix.diff[:cap] + '\n... (truncated)')
    return f'<diff>\n{diff}\n</diff>'


def classify_all(*, rules_only: bool = False, model: Optional[str] = None,
                 quiet: bool = False) -> List[Dict]:
    """One row per DISTINCT fix commit, over both sides of every pair.

    The unit is the commit, not the pair. Six pairs of this dataset share one
    `fix0`, so a per-pair pass would classify that fix six times."""
    generator = None
    rows: List[Dict] = []
    seen = set()
    for pair in firewall.read_pairs():
        for which in firewall.WHICH:
            commit = pair.commit(which)
            if not commit or firewall.fix_id(commit) in seen:
                continue
            try:
                fix = firewall.clean_view(pair, which)
            except firewall.FixUnavailable as exc:
                if not quiet:
                    print(f'  skip {pair.name}/{which}: {exc.status}')
                continue
            seen.add(fix.fix_id)
            kind, evidence = classify_by_rule(fix)
            decided_by = 'rule'
            if kind is None:
                if rules_only:
                    kind, decided_by = None, 'unsure'
                else:
                    if generator is None:
                        generator = HarnessGenerator(
                            model=model or config.LOCAL_LLM_MODEL,
                            temperature=0.6, top_p=1.0)
                    kind, evidence = classify_by_model(fix, generator)
                    decided_by = 'model'
            rows.append({
                'fix_id': fix.fix_id,
                # Selector fields. This file is an operator artifact, and it
                # is never rendered, so it may name the pair and the side.
                'pair': pair.name,
                'which': which,
                'commit': commit,
                'codebase': fix.codebase,
                'bug_kind': kind,
                'decided_by': decided_by,
                'evidence': evidence,
                'diff_chars': len(fix.diff),
            })
            if not quiet:
                print(f'  {fix.fix_id} {pair.name}/{which}: '
                      f'{kind or "unsure"} ({decided_by})')
    return rows


def _print_mix(rows: List[Dict]) -> None:
    counts: Dict[str, int] = {}
    by_pass: Dict[str, int] = {}
    for r in rows:
        key = r['bug_kind'] or 'unsure'
        counts[key] = counts.get(key, 0) + 1
        by_pass[r['decided_by']] = by_pass.get(r['decided_by'], 0) + 1
    print('\n' + '=' * 60)
    print(f'distinct fixes classified: {len(rows)}')
    for kind in sorted(counts):
        print(f'  {kind:<10} {counts[kind]}')
    print('decided by:')
    for name in sorted(by_pass):
        print(f'  {name:<10} {by_pass[name]}')
    semantic = counts.get(SEMANTIC, 0)
    if semantic < 5:
        print(f'\nThe semantic pool holds {semantic} fixes. That is too few '
              f'for a pool of its own.\nReport the crashing pool alone, and '
              f'state this count as the reason.')
    print('=' * 60)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--out', default=str(DEFAULT_OUT),
                    help=f'default: {DEFAULT_OUT}')
    ap.add_argument('--rules_only', action='store_true',
                    help='no model calls; an unsure fix stays unsure')
    ap.add_argument('--model', default=None,
                    help=f'default: config.LOCAL_LLM_MODEL '
                         f'({config.LOCAL_LLM_MODEL})')
    args = ap.parse_args()

    rows = classify_all(rules_only=args.rules_only, model=args.model)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open('w') as fh:
        for row in rows:
            fh.write(json.dumps(row) + '\n')
    _print_mix(rows)
    print(f'Wrote {out}')
    return 0


def load(path: Path = DEFAULT_OUT) -> Dict[str, str]:
    """`{fix_id: bug_kind}` from a previous run, or `{}` when absent."""
    if not path.exists():
        return {}
    out: Dict[str, str] = {}
    for line in path.read_text().splitlines():
        if line.strip():
            row = json.loads(line)
            if row.get('bug_kind'):
                out[row['fix_id']] = row['bug_kind']
    return out


if __name__ == '__main__':
    sys.exit(main())
