"""Score the Project Zero population with the one-shot baseline.

Usage (from src/):
    uv run -m baseline_llmjudge.project_zero.evaluate --dry_run
    uv run -m baseline_llmjudge.project_zero.evaluate --prompt_version p1
    uv run -m baseline_llmjudge.project_zero.evaluate --prompt_version p1 --samples 1

WHAT IS IMPORTED RATHER THAN COPIED. `confusion`, `RULES`, `HEADLINE_RULE` and
`print_summary` all come from `baseline_llmjudge.shared.scoring`. So the two
datasets share one confusion matrix, one headline rule and one printed form,
and the arithmetic cannot drift between them.

`summarise` is local, and so is the Defects4J one. The two differ in what they
group by and in which prompt registry they read, so one shared function would
need a flag for each difference.

TWO DIFFERENCES FROM THE DEFECTS4J EVALUATOR, AND BOTH ARE PROPERTIES OF THIS
POPULATION.

  1. `by_codebase` replaces `by_bug`. The cluster here is the project. 14 of
     the 21 rows are Chrome or v8, and rows of one project share a coding
     style, a review culture and a fault vocabulary. A number computed as
     though 21 rows were independent is too narrow.
  2. `baselines` scores two rules that read no code at all. `always_positive`
     calls every fix overfitting, and the 0.57 prior alone gives it F1 0.73.
     `size_rule` calls a fix overfitting when its diff is short, which is the
     proxy control for the negative-class confound. Both take their best form
     on these very rows, so both are floors and neither is a competitor. A
     judge that does not clearly beat the higher floor is unproven on this
     population, whatever its own F1.

NO HOLDOUT YET. 21 rows do not support two sides: ten rows per side give an
interval about 0.3 wide on F1, and no prompt comparison survives that. So this
evaluator runs one dev side over the whole population, and refuses `--side
holdout`. Freeze a split after `resolve_gerrit.py` raises the population to
about 40 rows, and split by codebase so that v8 does not sit on both sides.
"""
import argparse
import json
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

import config                                              # noqa: E402
from baseline_llmjudge.project_zero import (bugkind,          # noqa: E402
                                            prompts, queue, run_one)
from baseline_llmjudge.shared import budget, verdict          # noqa: E402
from baseline_llmjudge.shared.provenance import git_sha        # noqa: E402
from baseline_llmjudge.shared.scoring import (HEADLINE_RULE,   # noqa: E402
                                              RULES, confusion, counts, div,
                                              pct, print_summary)

REPO = Path(__file__).resolve().parents[3]


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--side', default='dev', choices=['dev', 'holdout'])
    ap.add_argument('--prompt_version', default='p1',
                    help=f'registered: {prompts.known_versions()}')
    ap.add_argument('--samples', type=int,
                    default=run_one.DEFAULT_SAMPLES)
    ap.add_argument('--bug_kind', default=None,
                    choices=[bugkind.CRASHING, bugkind.SEMANTIC],
                    help='score one pool only; needs bug_kind.jsonl')
    ap.add_argument('--model', default=None,
                    help=f'default: config.LOCAL_LLM_MODEL '
                         f'({config.LOCAL_LLM_MODEL})')
    ap.add_argument('--out_dir', default=None,
                    help='default: results/llmjudge_pz_<version>_<ts>')
    ap.add_argument('--allow_missing_source', action='store_true',
                    help='keep a fix whose context fetch produced no file')
    ap.add_argument('--dry_run', action='store_true',
                    help='build the population and stop')
    args = ap.parse_args()

    if args.side == 'holdout':
        print("REFUSING: no Project Zero holdout split is frozen. The "
              "population is 21 rows, and two sides of ten cannot separate "
              "two prompt versions. Raise the population with "
              "tools/resolve_gerrit.py first. See the README.",
              file=sys.stderr)
        return 2

    try:
        version = prompts.resolve(args.prompt_version)
    except ValueError as exc:
        print(f'REFUSING: {exc}', file=sys.stderr)
        return 2

    # Loaded whether or not a pool is selected. `--bug_kind` FILTERS the
    # population; the map itself records each row's kind either way, so the
    # summary can break the score down by pool without splitting the run.
    kinds = bugkind.load()
    if args.bug_kind and not kinds:
        print(f'REFUSING: --bug_kind needs {bugkind.DEFAULT_OUT}. Run '
              f'`uv run -m baseline_llmjudge.project_zero.bugkind` first.',
              file=sys.stderr)
        return 2

    rows, stats = queue.build_queue(
        require_source=not args.allow_missing_source,
        bug_kind=args.bug_kind, kinds=kinds)

    ts = datetime.now().strftime('%Y%m%d_%H%M%S')
    out_dir = Path(args.out_dir) if args.out_dir else (
        REPO / 'results' / f'llmjudge_pz_{version.name}_{ts}')
    out_dir.mkdir(parents=True, exist_ok=True)

    print(f'Output dir     : {out_dir}')
    print('Dataset        : project_zero')
    print(f'Prompt version : {version.name}  '
          f'[stage {prompts.stage_of(version.name)}]')
    print(f'Hypothesis     : {version.hypothesis}')
    print(f'Fixes queued   : {len(rows)}')
    print(f'Samples each   : {args.samples}  '
          f'({len(rows) * args.samples} model calls minimum)')
    queue.print_stats(stats, out=sys.stdout)

    (out_dir / 'population.json').write_text(json.dumps(stats, indent=2))
    (out_dir / 'queue.txt').write_text('\n'.join(
        f"{'-o' if r.label == 'overfitting' else '-c'} {r.pair}|{r.which}"
        for r in rows) + '\n')
    (out_dir / 'dataset_provenance.txt').write_text(
        _git_log_for(REPO / 'src' / 'db' / 'project_zero' / 'pairs'))

    if args.dry_run:
        print('\n--dry_run — stopping before any model call.')
        return 0

    records_path = out_dir / 'records.jsonl'
    records: List[Dict] = []
    spend: Dict[str, int] = {}
    with open(records_path, 'w') as fh:
        for i, row in enumerate(rows, start=1):
            print(f'[{i}/{len(rows)}] {row.label} {row.fix.fix_id}')
            rec = run_one.classify(
                row,
                version=version.name,
                bug_kind=kinds.get(row.fix.fix_id, 'unclassified'),
                samples=args.samples,
                model=args.model)
            fh.write(json.dumps(rec) + '\n')
            fh.flush()
            records.append(rec)
            spend = budget.add(spend, rec.get('tokens_total') or {})
            if rec['status'] != 'evaluated':
                print(f"  not scored: {rec['status']} "
                      f"({rec.get('detail', '')})")

    summary = summarise(records, spend, stats,
                           side=args.side,
                           bug_kind=args.bug_kind or 'both',
                           version=version.name,
                           samples=args.samples,
                           model=args.model or config.LOCAL_LLM_MODEL)
    (out_dir / 'summary.json').write_text(json.dumps(summary, indent=2))
    print_summary(summary)
    _print_pz_extras(summary)
    print(f'\nWrote {records_path}')
    print(f"Wrote {out_dir / 'summary.json'}")
    return 0


# --- scoring -----------------------------------------------------------------

def summarise(records: List[Dict], spend: Dict, population: Dict, *,
                 side: str, bug_kind: str, version: str, samples: int,
                 model: str) -> Dict:
    """The Defects4J summary shape, with two Project Zero additions."""
    scored = [r for r in records if r['status'] == 'evaluated']
    n_pos = sum(1 for r in scored if r['label'] == 'overfitting')
    agreements = [a for a in ((r.get('vote') or {}).get('agreement')
                              for r in scored) if a is not None]

    summary: Dict = {
        'dataset': 'project_zero',
        'side': side,
        'bug_kind': bug_kind,
        'prompt_version': version,
        'prompt_stage': prompts.stage_of(version),
        'prompt_base': prompts.base_of(version),
        'prompt_hypothesis': prompts.resolve(version).hypothesis,
        'prompt_sha256': prompts.version_sha256(version),
        'prompt_text': prompts.version_text(version),
        'samples_per_patch': samples,
        'model': model,
        'reasoning_effort': config.OPENAI_REASONING_EFFORT,
        'headline_rule': HEADLINE_RULE,
        'parse_failure_counts_as': verdict.class_name(
            run_one.PARSE_FAILURE_COUNTS_AS),
        'git_sha': git_sha(),
        'population': population,
        'queued': len(records),
        'scored': len(scored),
        'not_scored': counts(r['status'] for r in records
                              if r['status'] != 'evaluated'),
        'positive_class_prior': div(n_pos, len(scored)),
        'mean_sample_agreement': (sum(agreements) / len(agreements)
                                  if agreements else None),
        'parse_failures': sum((r.get('vote') or {}).get('n_parse_failures', 0)
                              for r in scored),
        'headline': confusion(scored, HEADLINE_RULE),
        'vote_rule_sensitivity': [confusion(scored, r) for r in RULES],
        'by_codebase': _by_codebase(scored),
        'by_bug_kind': _by_bug_kind(scored),
        'baselines': _baselines(scored),
        'budget': budget.report(spend),
    }
    clean = [r for r in scored
             if not (r.get('vote') or {}).get('n_parse_failures')]
    if len(clean) != len(scored):
        summary['headline_excluding_parse_failures'] = confusion(
            clean, HEADLINE_RULE)
    return summary


def _by_codebase(scored: List[Dict]) -> List[Dict]:
    """Per-project breakdown. The project is the cluster in this population.

    14 of the 21 rows are Chrome or v8. Rows of one project share a coding
    style and a fault vocabulary, so a per-row total hides how few projects
    it rests on."""
    groups: Dict[str, Dict] = {}
    for r in scored:
        key = r.get('codebase') or 'unknown'
        slot = groups.setdefault(key, {'codebase': key, 'fixes': 0,
                                       'overfitting': 0, 'correct': 0,
                                       'right': 0})
        slot['fixes'] += 1
        slot[r['label']] += 1
        want = r['label'] == 'overfitting'
        if bool((r.get('decisions') or {}).get(HEADLINE_RULE)) == want:
            slot['right'] += 1
    return [groups[k] for k in sorted(groups)]


def _by_bug_kind(scored: List[Dict]) -> List[Dict]:
    """One confusion matrix per pool, without splitting the run.

    The gate in `bugkind` found 5 semantic rows in this population, which
    is too few for a pool of its own. So one run scores every row, and this
    breakdown shows the two pools separately. Read the semantic row as a
    count, never as an F1."""
    kinds = sorted({r.get('bug_kind') or 'unclassified' for r in scored})
    out = []
    for kind in kinds:
        rows = [r for r in scored if (r.get('bug_kind') or 'unclassified')
                == kind]
        out.append({'bug_kind': kind, 'fixes': len(rows),
                    **confusion(rows, HEADLINE_RULE)})
    return out


def _baselines(scored: List[Dict]) -> Dict:
    """Two floors the model must clear. Neither one reads the code.

    `always_positive` calls every fix overfitting. With a positive prior of
    0.57 that alone scores F1 0.73, so it is the number any result on this
    population has to beat first.

    `size_rule` calls a fix overfitting when its diff is shorter than T. It is
    the proxy control for the negative-class confound: the negative class is
    the later fix of the same pair, and a later fix had more scrutiny.
    `firewall` removes every date, identifier and commit message, so no
    token states the order — but it cannot remove a difference in coding
    style, and fix size is the readable half of that.

    T maximises F1 on the scored rows themselves, so this is the BEST case for
    the proxy and not a fair held-out estimate. That is the point: it is a
    floor, not a competitor.

    A DEGENERATE THRESHOLD IS EXCLUDED. The largest diff length makes the rule
    predict every row positive, which is `always_positive` under another name.
    Both figures are reported, so a reader can see which floor is which."""
    always = confusion([{'label': r['label'], 'decisions': {HEADLINE_RULE: True}}
                        for r in scored], HEADLINE_RULE)
    out: Dict = {
        'always_positive': {'rule': 'every fix is overfitting', **always},
        'size_rule': None,
    }
    best = None
    for threshold in sorted({r.get('diff_chars', 0) for r in scored}):
        flags = [r.get('diff_chars', 0) <= threshold for r in scored]
        if all(flags) or not any(flags):
            continue                      # degenerate: see the docstring
        m = confusion([{'label': r['label'], 'decisions': {HEADLINE_RULE: f}}
                       for r, f in zip(scored, flags)], HEADLINE_RULE)
        if m['f1'] is not None and (best is None or m['f1'] > best['f1']):
            best = {**m, 'threshold_diff_chars': threshold}
    if best is not None:
        out['size_rule'] = {
            'rule': 'diff_chars <= threshold means overfitting',
            'threshold_selected_on': 'the scored rows themselves (best case)',
            **best,
        }
    return out


def _print_pz_extras(s: Dict) -> None:
    print('by bug kind:')
    for g in s['by_bug_kind']:
        print(f"  {g['bug_kind']:<12} {g['fixes']:2d} fixes  "
              f"P={pct(g['precision'])} R={pct(g['recall'])} "
              f"F1={pct(g['f1'])}")
    print('by codebase:')
    for g in s['by_codebase']:
        print(f"  {g['codebase']:<20} {g['right']}/{g['fixes']} right "
              f"(-o {g['overfitting']}, -c {g['correct']})")
    floors = []
    print('baselines that read no code:')
    always = s['baselines']['always_positive']
    floors.append(('always-positive', always['f1']))
    print(f"  always-positive        P={pct(always['precision'])} "
          f"R={pct(always['recall'])} F1={pct(always['f1'])}")
    size = s['baselines']['size_rule']
    if size:
        floors.append(('size rule', size['f1']))
        print(f"  size rule (<= {size['threshold_diff_chars']:,} chars) "
              f"P={pct(size['precision'])} R={pct(size['recall'])} "
              f"F1={pct(size['f1'])}")
    else:
        print('  size rule              no non-degenerate threshold exists')

    model_f1 = s['headline']['f1']
    floors = [(n, f) for n, f in floors if f is not None]
    if model_f1 is not None and floors:
        name, best = max(floors, key=lambda nf: nf[1])
        print(f"  highest floor is the {name}, F1={best:.3f}")
        print(f"  model F1 minus that floor = {model_f1 - best:+.3f}")
        if model_f1 - best <= 0.05:
            print('  The model does not clearly beat a baseline that reads no '
                  'code.\n  Report it as unproven on this population.')
    print('=' * 60)


def _git_log_for(path: Path) -> str:
    try:
        proc = subprocess.run(
            ['git', 'log', '-1', '--format=%H %ad', '--', str(path)],
            capture_output=True, text=True, cwd=str(REPO), timeout=15)
        return proc.stdout.strip() or '(not under git)'
    except Exception:
        return '(not under git)'


if __name__ == '__main__':
    sys.exit(main())
