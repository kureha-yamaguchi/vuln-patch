"""Deep diff-relatability CLI — second-pass verification of the seed pairs.

Reads a `p0_seeds.json` file produced by `cve_scan.run_p0_harvest`, fetches
both sides' fix patches (reusing the rough overlap module's patch
fetcher), and asks gpt-5-mini to judge whether the two diffs are
code-level related.

Example usage (from src/):

    uv run -m cve_scan.run_diff_relate                 # use defaults
    uv run -m cve_scan.run_diff_relate --all           # not just confirmed
    uv run -m cve_scan.run_diff_relate --budget-usd 5
    uv run -m cve_scan.run_diff_relate \\
        --seeds ./findings/pipeline/seeds.json \\
        --out   ./findings/pipeline

Set GITHUB_TOKEN in the environment to lift GitHub API rate limits.
"""
import argparse
import os
import sys

from . import config
from .classifier import Classifier
from .code_overlap import CodeOverlapChecker
from .diff_relate import DiffRelateAnalyzer, load_seeds, write_run
from .p0_harvest import RcaRepo


def parse_args():
    p = argparse.ArgumentParser(
        description="Deep diff-relatability verifier — second LLM pass "
                    "over patch unified diffs.",
    )
    p.add_argument('--seeds', type=str,
                   default=os.path.join(config.CVE_SCAN_PIPELINE_DIR,
                                        'seeds.json'),
                   help=f'Path to seeds.json from the harvester '
                        f'(default: '
                        f'{config.CVE_SCAN_PIPELINE_DIR}/seeds.json).')
    p.add_argument('--all', action='store_true',
                   help='Analyze every seed in the file, not just the '
                        '`confirmed=True` ones.')
    p.add_argument('--budget-usd', type=float,
                   default=config.LLM_MAX_BUDGET_USD,
                   help=f'Cap LLM spend in USD '
                        f'(default: ${config.LLM_MAX_BUDGET_USD:.2f}).')
    p.add_argument('--model', type=str,
                   default=config.LLM_CLASSIFIER_MODEL,
                   help=f'OpenAI model to use '
                        f'(default: {config.LLM_CLASSIFIER_MODEL}).')
    p.add_argument('--github-token', type=str, default=None,
                   help='GitHub token for higher API rate limits '
                        '(falls back to GITHUB_TOKEN env var).')
    p.add_argument('--out', type=str,
                   default=config.CVE_SCAN_PIPELINE_DIR,
                   help=f'Directory for output JSON/CSV '
                        f'(default: {config.CVE_SCAN_PIPELINE_DIR}).')
    return p.parse_args()


def main():
    args = parse_args()

    if not os.path.isfile(args.seeds):
        print(f"error: seeds file not found: {args.seeds}", file=sys.stderr)
        sys.exit(2)

    seeds = load_seeds(args.seeds)
    print(f"Loaded {len(seeds)} seeds from {args.seeds}")
    confirmed_count = sum(1 for s in seeds if s.get('confirmed'))
    print(f"  {confirmed_count} confirmed")

    # The overlap checker reuses its disk cache, so re-running here is
    # cheap as long as we share the cache root with the harvester.
    rca_dir = RcaRepo().ensure(refresh=False)
    overlap = CodeOverlapChecker(
        rca_dir=rca_dir,
        cache_dir=config.CVE_SCAN_CACHE_DIR,
        github_token=args.github_token or os.getenv('GITHUB_TOKEN'),
    )
    classifier = Classifier(model=args.model,
                            max_budget_usd=args.budget_usd)
    analyzer = DiffRelateAnalyzer(
        classifier=classifier,
        overlap_checker=overlap,
        only_confirmed=not args.all,
    )

    print(f"Analyzing {'all' if args.all else 'confirmed-only'} pairs with "
          f"{args.model} (budget ${args.budget_usd:.2f}) ...")
    run = analyzer.run(seeds)

    write_run(args.out, run)
    _print_summary(run)
    sys.exit(0)


def _print_summary(run) -> None:
    print("\n" + "#" * 18 + " diff-relate run " + "#" * 18)
    print(f"seeds total      : {run.seeds_in}")
    print(f"seeds confirmed  : {run.confirmed_in}")
    print(f"analyzed (both)  : {run.analyzed}")
    print(f"related verdicts : {run.related_count}")
    print(f"llm spend        : ${run.spend_usd:.4f}")
    print(f"llm calls        : {run.calls_live} live + {run.calls_cached} cached")
    print("#" * 53)


if __name__ == '__main__':
    main()
