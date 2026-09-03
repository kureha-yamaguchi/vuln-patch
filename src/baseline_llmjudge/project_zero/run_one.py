"""Classify one Project Zero fix: N samples of one prompt, one decision.

Usage (from src/):
    uv run -m baseline_llmjudge.project_zero.run_one \\
        --pair CVE-2021-30551__CVE-2022-1096 --which fix0 \\
        --prompt_version p1 --samples 5

The Defects4J runner is `run_one.py`, and this module reuses three of its
policies rather than restating them:

  * `verdict.py` — the one-bit output space, the last-`VERDICT:`-line parse,
    and the three vote rules.
  * `PARSE_FAILURE_COUNTS_AS` and `PARSE_RETRIES` — imported from `run_one`,
    so the two datasets cannot end up with two different defaults for an
    unparsed sample.
  * `budget.py` — the spend report.

No evidence cache. The Defects4J side caches because extraction needs a
Defects4J checkout and two test runs. Here the render is a few local file
reads, so a cache would be a directory to keep in step for nothing. The record
still carries `evidence_sha256`, so the claim that every prompt version reads
byte-identical evidence stays checkable.

`--samples 0` renders the evidence, prints its size, and stops. That is the
cheap way to measure how large this population's prompts are before a pass is
paid for.
"""
import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Dict, List, Optional

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

import config                                              # noqa: E402
from llm import (HarnessGenerator, reset_token_usage,        # noqa: E402
                 token_usage, usage_totals)

from baseline_llmjudge.project_zero import (evidence,         # noqa: E402
                                            firewall, prompts)
from baseline_llmjudge.shared import budget, verdict          # noqa: E402
from baseline_llmjudge.shared.provenance import git_sha        # noqa: E402
from baseline_llmjudge.shared.verdict import (DEFAULT_SAMPLES,  # noqa: E402
                                              PARSE_FAILURE_COUNTS_AS,
                                              PARSE_RETRIES)


def classify(row, *, version: str = 'p1',
             bug_kind: str = 'crashing',
             samples: int = DEFAULT_SAMPLES,
             model: Optional[str] = None,
             with_region: bool = False,
             quiet: bool = False) -> Dict:
    """One record for one queued fix, shaped like the Defects4J records.

    `row` is a `queue.QueueRow`. The record uses the same field names, so
    `evaluate.confusion` scores both datasets with one function."""
    reset_token_usage()
    model = model or config.LOCAL_LLM_MODEL
    fix = row.fix
    rec: Dict = {
        'label': fix.label,
        'status': 'evaluated',
        'dataset': 'project_zero',
        'bug_kind': bug_kind,
        # Selector fields. They name the pair and the side, so a record file
        # is an operator artifact and never a prompt input.
        'pair': row.pair,
        'which': row.which,
        'fix_id': fix.fix_id,
        'codebase': fix.codebase,
        'software': fix.software,
        'touched_files': list(fix.touched_files),
        'prompt_version': version,
        'prompt_stage': prompts.stage_of(version),
        'prompt_base': prompts.base_of(version),
        'model': model,
        'reasoning_effort': config.OPENAI_REASONING_EFFORT,
        'git_sha': git_sha(),
    }

    blocks = evidence.render(fix, with_region=with_region)
    # Which arm of the region A/B this record belongs to. Recorded, so a
    # records file can never be read as the wrong arm.
    rec['with_region'] = with_region
    text = evidence.evidence_text(blocks)
    rec['parity_manifest'] = evidence.manifest(fix, blocks)
    rec['evidence_chars'] = len(text)
    # The diff-size proxy control. `evaluate` scores a rule that uses this
    # number alone, so a judge that only detects fix size is visible.
    rec['diff_chars'] = len(fix.diff)

    messages = prompts.build_messages(version, text)
    rec['prompt_chars'] = sum(len(m['content']) for m in messages)
    rec['evidence_sha256'] = hashlib.sha256(text.encode()).hexdigest()
    rec['prompt_sha256'] = prompts.version_sha256(version)

    if samples < 1:
        rec.update(status='rendered_only',
                   detail=f'samples={samples}, so no model was called')
        if not quiet:
            print(f"  {fix.fix_id} [{fix.label}]: evidence "
                  f"{len(text):,} chars, prompt {rec['prompt_chars']:,} "
                  f"chars — no model call")
        return rec

    generator = HarnessGenerator(model=model, temperature=0.6, top_p=1.0)
    results = [_one_sample(generator, messages, i, quiet=quiet)
               for i in range(samples)]

    summary = verdict.votes_summary(
        [r['verdict'] for r in results],
        parse_failure_counts_as=PARSE_FAILURE_COUNTS_AS)
    rec['samples'] = results
    rec['vote'] = summary
    # The headline rule. `crashed_on_patch` is the alias the pipeline's
    # aggregator reads, so one scoring function serves every side.
    rec['predicted_overfitting'] = summary['majority']
    rec['crashed_on_patch'] = summary['majority']
    rec['decisions'] = {k: summary[k] for k in ('majority', 'any',
                                                'unanimous')}
    rec['tokens_total'] = usage_totals()
    rec['tokens_by_model'] = token_usage()
    rec['budget'] = budget.report(rec['tokens_total'])
    if not quiet:
        print(f"  {fix.fix_id} [{fix.label}]: "
              f"{summary['n_positive']}/{summary['n_samples']} overfitting "
              f"-> majority={verdict.class_name(summary['majority'])}"
              + (f", {summary['n_parse_failures']} unparsed"
                 if summary['n_parse_failures'] else ""))
    return rec


def _one_sample(generator, messages, index: int, quiet: bool) -> Dict:
    """One model call, with one retry when the verdict line is missing."""
    attempts: List[Dict] = []
    for attempt in range(PARSE_RETRIES + 1):
        try:
            text = generator.generate(messages)
        except Exception as exc:                    # network / API failure
            attempts.append({'error': f'{type(exc).__name__}: {exc}'})
            continue
        parsed = verdict.parse(text)
        attempts.append({'text': text, 'parsed': parsed})
        if parsed is not None:
            return {'index': index, 'verdict': parsed,
                    'retried': attempt > 0, 'text': text}
    if not quiet:
        print(f"    sample {index}: no verdict line parsed after "
              f"{PARSE_RETRIES + 1} attempt(s); counts as "
              f"{verdict.class_name(PARSE_FAILURE_COUNTS_AS)}")
    return {'index': index, 'verdict': None, 'retried': PARSE_RETRIES > 0,
            'attempts': attempts}


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--pair', required=True,
                    help='pair directory name, e.g. '
                         'CVE-2021-30551__CVE-2022-1096')
    ap.add_argument('--which', required=True, choices=list(firewall.WHICH),
                    help='which side of the pair to judge')
    ap.add_argument('--prompt_version', default='p1',
                    help=f'registered: {prompts.known_versions()}')
    ap.add_argument('--samples', type=int, default=DEFAULT_SAMPLES,
                    help='0 renders the evidence, then stops')
    ap.add_argument('--model', default=None,
                    help=f'default: config.LOCAL_LLM_MODEL '
                         f'({config.LOCAL_LLM_MODEL})')
    ap.add_argument('--records_json', default=None,
                    help='append the record as one JSON line to this file')
    args = ap.parse_args()

    from baseline_llmjudge.project_zero.queue import QueueRow
    pairs = {p.name: p for p in firewall.read_pairs()}
    if args.pair not in pairs:
        print(f'REFUSING: no pair named {args.pair!r}', file=sys.stderr)
        return 2
    try:
        prompts.resolve(args.prompt_version)
        fix = firewall.clean_view(pairs[args.pair], args.which)
    except (ValueError, firewall.FixUnavailable) as exc:
        print(f'REFUSING: {exc}', file=sys.stderr)
        return 2

    rec = classify(QueueRow(fix=fix, pair=args.pair, which=args.which),
                   version=args.prompt_version,
                   samples=args.samples,
                   model=args.model)
    if args.records_json:
        with open(args.records_json, 'a') as fh:
            fh.write(json.dumps(rec) + '\n')
    else:
        print(json.dumps(rec, indent=2))
    return 0 if rec['status'] in ('evaluated', 'rendered_only') else 1


if __name__ == '__main__':
    sys.exit(main())
