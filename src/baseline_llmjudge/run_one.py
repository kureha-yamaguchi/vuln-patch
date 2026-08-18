"""Classify one candidate patch: N samples of one prompt, one decision.

Usage (from src/):
    uv run -m baseline_llmjudge.run_one -o \\
        --patch_file ../drr/Patches/Doverfitting/Jaid/Chart/patch1-Chart-9-Jaid-plausible.patch \\
        --prompt_version v1 --samples 5

The model, the client, the timeout policy and the usage recorder all come from
`llm.HarnessGenerator`, the same wrapper the harness campaign uses, constructed
with the same sampling arguments (`temperature=0.6, top_p=1.0`). A reasoning
model ignores those two — the OpenAI API rejects them — so the samples of one
patch vary through the provider's own nondeterminism. The README says so in the
"Same model" section, because it changes what the five samples measure.
"""
import argparse
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path
from typing import Dict, List, Optional

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

import config                                            # noqa: E402
from llm import (HarnessGenerator, reset_token_usage,     # noqa: E402
                 token_usage, usage_totals)

from baseline_llmjudge import budget, context, prompts, verdict  # noqa: E402

# An unparsed sample counts as the NEGATIVE class. It is not dropped: dropping
# it would hand the baseline a filter the pipeline never gets, because a
# pipeline run that produces no usable harness is scored, not excluded.
PARSE_FAILURE_COUNTS_AS = False

# One retry per sample, then the sample is a parse failure. More retries would
# quietly buy the baseline extra attempts the pipeline does not get per harness.
PARSE_RETRIES = 1

DEFAULT_SAMPLES = 5


def classify(patch_path: str, label: str, *,
             version: str = 'v0',
             samples: int = DEFAULT_SAMPLES,
             cache_dir: Optional[str] = None,
             model: Optional[str] = None,
             refresh_context: bool = False,
             quiet: bool = False) -> Dict:
    """One record for one candidate patch, shaped like the pipeline's."""
    reset_token_usage()
    model = model or config.LOCAL_LLM_MODEL
    rec: Dict = {
        'label': label,
        'status': 'evaluated',
        'bug_kind': 'crashing',
        'project': None, 'bug_id': None, 'apr_tool': None,
        'patch': os.path.basename(patch_path),
        'prompt_version': version,
        'prompt_stage': prompts.stage_of(version),
        'prompt_base': prompts.base_of(version),
        'model': model,
        'reasoning_effort': config.OPENAI_REASONING_EFFORT,
        'git_sha': _git_sha(),
    }

    try:
        ev = context.load_or_build(patch_path, label, cache_dir=cache_dir,
                                   refresh=refresh_context)
    except context.ContextUnavailable as exc:
        rec.update(status=exc.status, detail=exc.detail)
        return rec
    except Exception as exc:                       # extraction is fragile
        rec.update(status='error', detail=f'{type(exc).__name__}: {exc}')
        return rec

    rec.update(project=ev.project, bug_id=ev.bug_id, apr_tool=ev.apr_tool,
               patch=ev.patch, context_degraded=ev.context_degraded,
               parity_manifest=ev.manifest, evidence_facts=ev.facts)

    messages = prompts.build_messages(version, ev.text)
    rec['prompt_chars'] = sum(len(m['content']) for m in messages)
    # The evidence is meant to be byte-identical across prompt versions, so
    # that a dev score difference can only come from the wording. Recording
    # its digest makes that a CHECK rather than an assumption: two version
    # runs of the same patch must carry the same value here.
    rec['evidence_sha256'] = hashlib.sha256(ev.text.encode()).hexdigest()
    # Which prompt TEXT produced this score. A version is a draft until it is
    # run, then it is frozen; this digest is what makes that rule checkable
    # after the fact instead of a promise about editing discipline.
    rec['prompt_sha256'] = prompts.version_sha256(version)
    generator = HarnessGenerator(model=model, temperature=0.6, top_p=1.0)

    results: List[Dict] = []
    for i in range(samples):
        results.append(_one_sample(generator, messages, i, quiet=quiet))
    votes = [r['verdict'] for r in results]

    summary = verdict.votes_summary(
        votes, parse_failure_counts_as=PARSE_FAILURE_COUNTS_AS)
    rec['samples'] = results
    rec['vote'] = summary
    # The headline rule. `crashed_on_patch` is the alias the pipeline's
    # aggregator reads, so one scoring function serves both sides.
    rec['predicted_incomplete'] = summary['majority']
    rec['crashed_on_patch'] = summary['majority']
    rec['decisions'] = {k: summary[k] for k in ('majority', 'any',
                                                'unanimous')}
    rec['tokens_total'] = usage_totals()
    rec['tokens_by_model'] = token_usage()
    rec['budget'] = budget.report(rec['tokens_total'])
    if not quiet:
        print(f"  {ev.project}-{ev.bug_id} ({ev.apr_tool}) [{label}]: "
              f"{summary['n_positive']}/{summary['n_samples']} incomplete "
              f"-> majority={summary['majority']}"
              + (f", {summary['n_parse_failures']} unparsed"
                 if summary['n_parse_failures'] else ""))
    return rec


def _one_sample(generator, messages, index: int, quiet: bool) -> Dict:
    """One model call, with one retry when the verdict line is missing."""
    attempts = []
    for attempt in range(PARSE_RETRIES + 1):
        try:
            text = generator.generate(messages)
        except Exception as exc:                   # network / API failure
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
              f"{'INCOMPLETE' if PARSE_FAILURE_COUNTS_AS else 'CORRECT'}")
    return {'index': index, 'verdict': None, 'retried': PARSE_RETRIES > 0,
            'attempts': attempts}


def _git_sha() -> str:
    env = os.environ.get('GITSHA')
    if env:
        return env
    try:
        return subprocess.run(['git', 'rev-parse', 'HEAD'],
                              capture_output=True, text=True,
                              timeout=10).stdout.strip() or 'unknown'
    except Exception:
        return 'unknown'


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--patch_file', required=True,
                    help='path to the candidate .patch file')
    group = ap.add_mutually_exclusive_group(required=True)
    group.add_argument('-c', '--correct', action='store_true',
                       help='ground truth: this patch is correct')
    group.add_argument('-o', '--overfitting', action='store_true',
                       help='ground truth: this patch is overfitting')
    ap.add_argument('--prompt_version', default='v0',
                    help=f'stage-A design or stage-B iteration; registered: '
                         f'{prompts.known_versions()}')
    ap.add_argument('--samples', type=int, default=DEFAULT_SAMPLES)
    ap.add_argument('--model', default=None,
                    help=f'default: config.LOCAL_LLM_MODEL '
                         f'({config.LOCAL_LLM_MODEL})')
    ap.add_argument('--cache_dir', default=None,
                    help='reuse/store the extracted evidence here')
    ap.add_argument('--refresh_context', action='store_true',
                    help='re-extract the evidence even when cached')
    ap.add_argument('--records_json', default=None,
                    help='append the record as one JSON line to this file')
    args = ap.parse_args()

    rec = classify(args.patch_file,
                   'correct' if args.correct else 'overfitting',
                   version=args.prompt_version,
                   samples=args.samples,
                   cache_dir=args.cache_dir,
                   model=args.model,
                   refresh_context=args.refresh_context)
    if args.records_json:
        with open(args.records_json, 'a') as fh:
            fh.write(json.dumps(rec) + '\n')
    else:
        print(json.dumps(rec, indent=2))
    if rec['status'] == 'evaluated':
        return 0
    return 1 if rec['status'] == 'error' else 3


if __name__ == '__main__':
    sys.exit(main())
