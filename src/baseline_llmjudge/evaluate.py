"""Score one side of the frozen crashing split with the one-shot baseline.

Usage (from src/):
    uv run -m baseline_llmjudge.evaluate --side dev --prompt_version v1
    uv run -m baseline_llmjudge.evaluate --side holdout --prompt_version v2 \\
        --confirm_holdout

The queue comes from `java/dataset/build_split_queue.py`, invoked as a
subprocess. That is deliberate: the pipeline's own evaluator builds its queue
with the same script, so both sides score the same certified patches, honour
the same exclusions, and inherit the same `-c` / `-o` ground truth. A second
implementation of the queue would be a place for the two populations to drift
apart.

`--side holdout` refuses to run without `--confirm_holdout`.

Stage B selects on holdout F1, so every stage-B iteration gets a holdout pass.
A holdout pass is a SCORING pass and nothing else: it returns the numbers in
`summary.json`, and its `records.jsonl` is never read for refinement.
`errors.py` refuses holdout records, so that rule is enforced rather than
remembered.
"""
import argparse
import json
import math
import shutil
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

import config                                              # noqa: E402
from baseline_llmjudge import (budget, prompts, run_one,     # noqa: E402
                               verdict)

REPO = Path(__file__).resolve().parents[2]
QUEUE_BUILDER = REPO / 'src' / 'java' / 'dataset' / 'build_split_queue.py'
SPLIT_FILE = REPO / 'suites' / 'splits' / 'crashing_split.jsonl'
RULES = ('majority', 'any', 'unanimous')
HEADLINE_RULE = 'majority'


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--side', required=True, choices=['dev', 'holdout'])
    ap.add_argument('--prompt_version', default='v1',
                    help=f'stage-A design or stage-B iteration; registered: '
                         f'{prompts.known_versions()}')
    ap.add_argument('--samples', type=int, default=run_one.DEFAULT_SAMPLES)
    ap.add_argument('--projects', default='',
                    help='space-separated allow-list; default = every '
                         'project on this side')
    ap.add_argument('--model', default=None,
                    help=f'default: config.LOCAL_LLM_MODEL '
                         f'({config.LOCAL_LLM_MODEL})')
    ap.add_argument('--out_dir', default=None,
                    help='default: results/llmjudge_<side>_<version>_<ts>')
    ap.add_argument('--cache_dir', default=str(REPO / 'results'
                                               / 'llmjudge_cache'),
                    help='extracted evidence is cached here and reused '
                         'across prompt versions')
    ap.add_argument('--refresh_context', action='store_true')
    ap.add_argument('--tool_records', default=None,
                    help="a pipeline run's records.jsonl; adds the paired "
                         "head-to-head matrix and the McNemar counts")
    ap.add_argument('--confirm_holdout', action='store_true',
                    help='required for --side holdout')
    ap.add_argument('--dry_run', action='store_true',
                    help='build the queue and stop')
    args = ap.parse_args()

    # Resolve the version first: a typo, or a stage-B iteration that has not
    # been registered yet, must fail before any directory is created.
    try:
        version = prompts.resolve(args.prompt_version)
    except ValueError as exc:
        print(f"REFUSING: {exc}", file=sys.stderr)
        return 2

    if args.side == 'holdout' and not args.confirm_holdout:
        print("REFUSING: --side holdout needs --confirm_holdout. A holdout "
              "pass scores a registered version and nothing else — never "
              "read its errors. See the README protocol.",
              file=sys.stderr)
        return 2

    ts = datetime.now().strftime('%Y%m%d_%H%M%S')
    out_dir = Path(args.out_dir) if args.out_dir else (
        REPO / 'results'
        / f'llmjudge_{args.side}_{args.prompt_version}_{ts}')
    out_dir.mkdir(parents=True, exist_ok=True)

    queue_path = out_dir / 'queue.txt'
    queue = _build_queue(args.side, args.projects, queue_path)
    print(f"Output dir     : {out_dir}")
    print(f"Prompt version : {version.name}  "
          f"[stage {prompts.stage_of(version.name)}]")
    print(f"Hypothesis     : {version.hypothesis}")
    print(f"Patches queued : {len(queue)}")
    print(f"Samples each   : {args.samples}  "
          f"({len(queue) * args.samples} model calls minimum)")
    if args.dry_run:
        print("--dry_run — stopping before any model call.")
        return 0

    # Pin the result to the split and the code it was produced with.
    shutil.copy(SPLIT_FILE, out_dir / 'crashing_split.jsonl')
    (out_dir / 'split_provenance.txt').write_text(
        _git_log_for(SPLIT_FILE))

    records_path = out_dir / 'records.jsonl'
    records: List[Dict] = []
    spend: Dict[str, int] = {}
    with open(records_path, 'w') as fh:
        for i, (label, patch_path) in enumerate(queue, start=1):
            print(f"[{i}/{len(queue)}] {label} "
                  f"{Path(patch_path).name}")
            rec = run_one.classify(patch_path, label,
                                   version=args.prompt_version,
                                   samples=args.samples,
                                   cache_dir=args.cache_dir,
                                   model=args.model,
                                   refresh_context=args.refresh_context)
            fh.write(json.dumps(rec) + '\n')
            fh.flush()
            records.append(rec)
            spend = budget.add(spend, rec.get('tokens_total') or {})
            if rec['status'] != 'evaluated':
                print(f"  not scored: {rec['status']} "
                      f"({rec.get('detail', '')})")

    summary = summarise(records, spend,
                        side=args.side,
                        version=args.prompt_version,
                        samples=args.samples,
                        model=args.model or config.LOCAL_LLM_MODEL,
                        tool_records=args.tool_records)
    (out_dir / 'summary.json').write_text(json.dumps(summary, indent=2))
    _print_summary(summary)
    print(f"\nWrote {records_path}")
    print(f"Wrote {out_dir / 'summary.json'}")
    return 0


# --- queue -------------------------------------------------------------------

def _build_queue(side: str, projects: str, out: Path):
    """`[(label, patch_path)]` for one side, from the pipeline's own builder."""
    cmd = [sys.executable, str(QUEUE_BUILDER), '--side', side,
           '--out', str(out)]
    if projects.strip():
        cmd += ['--projects', projects]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    sys.stderr.write(proc.stderr)
    if proc.returncode != 0:
        raise SystemExit(f"queue build failed (exit {proc.returncode})")
    queue = []
    for line in out.read_text().splitlines():
        if not line.strip():
            continue
        marker, path = line.split(' ', 1)
        queue.append(('correct' if marker == '-c' else 'overfitting',
                      path.strip()))
    return queue


def _git_log_for(path: Path) -> str:
    try:
        proc = subprocess.run(
            ['git', 'log', '-1', '--format=%H %ad', '--', str(path)],
            capture_output=True, text=True, cwd=str(REPO), timeout=15)
        return proc.stdout.strip() or '(not under git)'
    except Exception:
        return '(not under git)'


# --- scoring -----------------------------------------------------------------

def confusion(rows: List[Dict], rule: str) -> Dict:
    """The pipeline's matrix, computed on the same field it reads.

    Positive class = ground-truth 'overfitting'. Identical arithmetic to the
    jq block in scripts/evaluate_crashing.sh."""
    ovf = [r for r in rows if r['label'] == 'overfitting']
    cor = [r for r in rows if r['label'] == 'correct']

    def positive(r):
        return bool((r.get('decisions') or {}).get(rule))

    tp = sum(1 for r in ovf if positive(r))
    fn = len(ovf) - tp
    fp = sum(1 for r in cor if positive(r))
    tn = len(cor) - fp
    return {
        'rule': rule,
        'overfitting_evaluated': len(ovf),
        'correct_evaluated': len(cor),
        'TP': tp, 'FN': fn, 'FP': fp, 'TN': tn,
        'precision': _div(tp, tp + fp),
        'recall': _div(tp, tp + fn),
        'specificity': _div(tn, tn + fp),
        'accuracy': _div(tp + tn, tp + fn + fp + tn),
        'f1': _div(2 * tp, 2 * tp + fp + fn),
    }


def summarise(records: List[Dict], spend: Dict, *, side: str, version: str,
              samples: int, model: str,
              tool_records: Optional[str] = None) -> Dict:
    scored = [r for r in records if r['status'] == 'evaluated']
    n_pos = sum(1 for r in scored if r['label'] == 'overfitting')
    agreements = [(r.get('vote') or {}).get('agreement')
                  for r in scored]
    agreements = [a for a in agreements if a is not None]
    parse_failures = sum((r.get('vote') or {}).get('n_parse_failures', 0)
                         for r in scored)

    summary: Dict = {
        'side': side,
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
        'git_sha': run_one._git_sha(),
        'queued': len(records),
        'scored': len(scored),
        'not_scored': _counts(r['status'] for r in records
                              if r['status'] != 'evaluated'),
        'positive_class_prior': _div(n_pos, len(scored)),
        'mean_sample_agreement': (sum(agreements) / len(agreements)
                                  if agreements else None),
        'parse_failures': parse_failures,
        'headline': confusion(scored, HEADLINE_RULE),
        'vote_rule_sensitivity': [confusion(scored, r) for r in RULES],
        'by_bug': _by_bug(scored),
        'budget': budget.report(spend),
    }
    # Same matrix with parse failures excluded, so a reader can see what the
    # negative-class default is worth.
    clean = [r for r in scored
             if not (r.get('vote') or {}).get('n_parse_failures')]
    if len(clean) != len(scored):
        summary['headline_excluding_parse_failures'] = confusion(
            clean, HEADLINE_RULE)

    if tool_records:
        summary['head_to_head'] = _head_to_head(scored, Path(tool_records))
    return summary


def _by_bug(scored: List[Dict]) -> List[Dict]:
    """Per-bug breakdown. Patches of one bug share every evidence block
    except the diff, so a per-patch total hides how few bugs it rests on."""
    bugs: Dict = {}
    for r in scored:
        key = f"{r['project']}-{r['bug_id']}"
        slot = bugs.setdefault(key, {'bug': key, 'patches': 0,
                                     'overfitting': 0, 'correct': 0,
                                     'right': 0})
        slot['patches'] += 1
        slot[r['label']] += 1
        want = r['label'] == 'overfitting'
        if bool((r.get('decisions') or {}).get(HEADLINE_RULE)) == want:
            slot['right'] += 1
    return [bugs[k] for k in sorted(bugs)]


def _head_to_head(scored: List[Dict], tool_path: Path) -> Dict:
    """Paired comparison against a pipeline run, on the patches both scored."""
    if not tool_path.exists():
        return {'error': f'{tool_path} not found'}
    tool: Dict = {}
    for line in tool_path.read_text().splitlines():
        if not line.strip():
            continue
        row = json.loads(line)
        if row.get('status') != 'evaluated':
            continue
        tool[_pair_key(row)] = bool(row.get('crashed_on_patch'))

    both, b, c = [], 0, 0
    for r in scored:
        key = _pair_key(r)
        if key not in tool:
            continue
        want = r['label'] == 'overfitting'
        tool_right = tool[key] == want
        base_right = bool((r.get('decisions') or {})
                          .get(HEADLINE_RULE)) == want
        both.append(r)
        if tool_right and not base_right:
            b += 1
        elif base_right and not tool_right:
            c += 1
    return {
        'paired_patches': len(both),
        'baseline_on_paired': confusion(both, HEADLINE_RULE),
        'tool_only_right': b,
        'baseline_only_right': c,
        'mcnemar_exact_two_sided_p': _mcnemar_p(b, c),
        'note': ('b = the pipeline is right and the baseline is wrong; '
                 'c = the reverse. Patches the pipeline did not score are '
                 'excluded from this block only.'),
    }


def _pair_key(row: Dict) -> str:
    return (f"{row.get('project')}|{row.get('bug_id')}|"
            f"{row.get('apr_tool')}|{row.get('label')}")


def _mcnemar_p(b: int, c: int):
    """Exact two-sided McNemar p-value over the discordant pairs."""
    n = b + c
    if n == 0:
        return None
    tail = sum(math.comb(n, i) for i in range(min(b, c) + 1))
    return min(1.0, 2.0 * tail / (2 ** n))


def _div(num, den):
    return (num / den) if den else None


def _counts(values):
    out: Dict[str, int] = {}
    for v in values:
        out[v] = out.get(v, 0) + 1
    return out


def _print_summary(s: Dict) -> None:
    h = s['headline']
    print("\n" + "=" * 60)
    print(f"side={s['side']} version={s['prompt_version']} "
          f"model={s['model']} samples={s['samples_per_patch']}")
    print(f"scored {s['scored']} of {s['queued']}  "
          f"(positive prior {_pct(s['positive_class_prior'])})")
    print(f"headline rule: {s['headline_rule']}")
    print(f"  TP={h['TP']} FN={h['FN']} FP={h['FP']} TN={h['TN']}")
    print(f"  precision={_pct(h['precision'])} recall={_pct(h['recall'])} "
          f"specificity={_pct(h['specificity'])} f1={_pct(h['f1'])}")
    print("vote-rule sensitivity:")
    for m in s['vote_rule_sensitivity']:
        print(f"  {m['rule']:<10} P={_pct(m['precision'])} "
              f"R={_pct(m['recall'])} F1={_pct(m['f1'])}")
    print(f"mean sample agreement: {_pct(s['mean_sample_agreement'])}"
          f"   parse failures: {s['parse_failures']}")
    bd = s['budget']
    print(f"budget: {bd['calls']} calls, {bd['prompt_tokens']:,} in "
          f"({bd['cached_prompt_tokens']:,} cached), "
          f"{bd['completion_tokens']:,} out")
    if bd['cost_usd_full_rate'] is not None:
        print(f"  cost (full rate)  : ${bd['cost_usd_full_rate']:.2f}")
    if bd['cost_usd_with_cache_rate'] is not None:
        print(f"  cost (cache rate) : "
              f"${bd['cost_usd_with_cache_rate']:.2f}")
    if bd['note']:
        print(f"  {bd['note']}")
    hh = s.get('head_to_head')
    if hh and 'paired_patches' in hh:
        print(f"head-to-head on {hh['paired_patches']} paired patches: "
              f"pipeline-only-right={hh['tool_only_right']}, "
              f"baseline-only-right={hh['baseline_only_right']}, "
              f"McNemar p={_pct_raw(hh['mcnemar_exact_two_sided_p'])}")
    print("=" * 60)


def _pct(v):
    return 'n/a' if v is None else f'{v:.3f}'


def _pct_raw(v):
    return 'n/a' if v is None else f'{v:.4f}'


if __name__ == '__main__':
    sys.exit(main())
