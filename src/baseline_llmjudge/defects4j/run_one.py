"""Classify one candidate patch: N samples of one prompt, one decision.

Usage (from src/):
    uv run -m baseline_llmjudge.defects4j.run_one -o \\
        --patch_file ../drr/Patches/Doverfitting/Jaid/Chart/patch1-Chart-9-Jaid-plausible.patch \\
        --prompt_version v1 --samples 5

`--kind` selects the pool, and it defaults to `crashing`. A patch whose bug
belongs to the other kind is not scored: the record carries `semantic_skip` or
`crashing_skip` and no model is called.

`--samples 0` extracts the evidence, caches it, and stops. That is the cheap
way to measure how large a pool's prompts are before a pass is paid for.

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
import sys
from pathlib import Path
from typing import Dict, List, Optional

sys.path.insert(0, str(next(p for p in Path(__file__).resolve().parents
                            if (p / 'config.py').exists())))

import config                                            # noqa: E402
from llm import (HarnessGenerator, reset_token_usage,     # noqa: E402
                 token_usage, usage_totals)

from baseline_llmjudge.defects4j import context, prompts       # noqa: E402
from baseline_llmjudge.shared import budget, verdict           # noqa: E402
from baseline_llmjudge.shared.provenance import git_sha        # noqa: E402
from baseline_llmjudge.shared.verdict import (DEFAULT_SAMPLES,  # noqa: E402
                                              PARSE_FAILURE_COUNTS_AS,
                                              PARSE_RETRIES)


def classify(patch_path: str, label: str, *,
             version: str = 'v1',
             kind: str = 'crashing',
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
        # The kind that was ASKED for. A patch of the other kind is refused
        # below, so a scored record's kind is also the kind it really is.
        'bug_kind': kind,
        'project': None, 'bug_id': None, 'apr_tool': None,
        'patch': os.path.basename(patch_path),
        'prompt_version': version,
        'prompt_stage': prompts.stage_of(version),
        'prompt_base': prompts.base_of(version),
        'prompt_kind': prompts.kind_of(version),
        'model': model,
        'reasoning_effort': config.OPENAI_REASONING_EFFORT,
        'git_sha': git_sha(),
    }

    try:
        ev = context.load_or_build(patch_path, label, cache_dir=cache_dir,
                                   refresh=refresh_context, kind=kind)
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
    if samples < 1:
        # The measurement path. The evidence is now extracted and cached, so
        # the prompt size of this pool can be read without paying for a pass.
        rec.update(status='extracted_only',
                   detail=f'samples={samples}, so no model was called')
        if not quiet:
            print(f"  {ev.project}-{ev.bug_id} ({ev.apr_tool}) [{label}]: "
                  f"evidence {len(ev.text):,} chars, prompt "
                  f"{rec['prompt_chars']:,} chars — no model call")
        return rec

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
    rec['predicted_overfitting'] = summary['majority']
    rec['crashed_on_patch'] = summary['majority']
    rec['decisions'] = {k: summary[k] for k in ('majority', 'any',
                                                'unanimous')}
    rec['tokens_total'] = usage_totals()
    rec['tokens_by_model'] = token_usage()
    rec['budget'] = budget.report(rec['tokens_total'])
    if not quiet:
        print(f"  {ev.project}-{ev.bug_id} ({ev.apr_tool}) [{label}]: "
              f"{summary['n_positive']}/{summary['n_samples']} overfitting "
              f"-> majority={verdict.class_name(summary['majority'])}"
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
              f"{verdict.class_name(PARSE_FAILURE_COUNTS_AS)}")
    return {'index': index, 'verdict': None, 'retried': PARSE_RETRIES > 0,
            'attempts': attempts}


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--patch_file', required=True,
                    help='path to the candidate .patch file')
    group = ap.add_mutually_exclusive_group(required=True)
    group.add_argument('-c', '--correct', action='store_true',
                       help='ground truth: this patch is correct')
    group.add_argument('-o', '--overfitting', action='store_true',
                       help='ground truth: this patch is overfitting')
    ap.add_argument('--prompt_version', default='v1',
                    help=f'stage-A design or stage-B iteration; registered: '
                         f'{prompts.known_versions()}')
    ap.add_argument('--kind', default=None,
                    choices=['crashing', 'semantic'],
                    help='bug pool; default: the pool the prompt version '
                         'belongs to')
    ap.add_argument('--samples', type=int, default=DEFAULT_SAMPLES,
                    help='0 extracts and caches the evidence, then stops')
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

    try:
        kind = args.kind or prompts.kind_of(args.prompt_version)
    except ValueError as exc:
        print(f"REFUSING: {exc}", file=sys.stderr)
        return 2

    rec = classify(args.patch_file,
                   'correct' if args.correct else 'overfitting',
                   version=args.prompt_version,
                   kind=kind,
                   samples=args.samples,
                   cache_dir=args.cache_dir,
                   model=args.model,
                   refresh_context=args.refresh_context)
    if args.records_json:
        with open(args.records_json, 'a') as fh:
            fh.write(json.dumps(rec) + '\n')
    else:
        print(json.dumps(rec, indent=2))
    if rec['status'] in ('evaluated', 'extracted_only'):
        return 0
    return 1 if rec['status'] == 'error' else 3


if __name__ == '__main__':
    sys.exit(main())
