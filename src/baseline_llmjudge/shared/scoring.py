"""The confusion matrix, the headline rule, and the printed summary form.

One matrix serves both datasets. The arithmetic cannot drift between them,
because there is only one copy of it.

The positive class is always ground-truth `overfitting`. `.claude/CONTEXT.md`
defines an overfitting patch as one that makes the reported failure go away
without a fix of the root cause. So a positive prediction is a claim that a
sibling bug survives the patch.
"""
from typing import Dict, Iterable, List

#: Every vote rule, reported for every patch as a sensitivity curve.
RULES = ('majority', 'any', 'unanimous')

#: The pre-registered headline. One place holds this choice, so a summary from
#: either dataset names the same rule.
HEADLINE_RULE = 'majority'


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
        'precision': div(tp, tp + fp),
        'recall': div(tp, tp + fn),
        'specificity': div(tn, tn + fp),
        'accuracy': div(tp + tn, tp + fn + fp + tn),
        'f1': div(2 * tp, 2 * tp + fp + fn),
    }


def div(num, den):
    """Guarded division. None rather than zero, so an empty cell reads as
    'not measured' and never as 'measured zero'."""
    return (num / den) if den else None


def counts(values: Iterable[str]) -> Dict[str, int]:
    out: Dict[str, int] = {}
    for v in values:
        out[v] = out.get(v, 0) + 1
    return out


def pct(v) -> str:
    return 'n/a' if v is None else f'{v:.3f}'


def print_summary(s: Dict) -> None:
    """The summary block both evaluators print.

    It reads only the keys both summaries carry. A dataset-specific breakdown
    is printed by that dataset's own evaluator, after this."""
    h = s['headline']
    print("\n" + "=" * 60)
    print(f"kind={s['bug_kind']} side={s['side']} "
          f"version={s['prompt_version']} "
          f"model={s['model']} samples={s['samples_per_patch']}")
    print(f"scored {s['scored']} of {s['queued']}  "
          f"(positive prior {pct(s['positive_class_prior'])})")
    print(f"headline rule: {s['headline_rule']}")
    print(f"  TP={h['TP']} FN={h['FN']} FP={h['FP']} TN={h['TN']}")
    print(f"  precision={pct(h['precision'])} recall={pct(h['recall'])} "
          f"specificity={pct(h['specificity'])} f1={pct(h['f1'])}")
    print("vote-rule sensitivity:")
    for m in s['vote_rule_sensitivity']:
        print(f"  {m['rule']:<10} P={pct(m['precision'])} "
              f"R={pct(m['recall'])} F1={pct(m['f1'])}")
    print(f"mean sample agreement: {pct(s['mean_sample_agreement'])}"
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
    print("=" * 60)
