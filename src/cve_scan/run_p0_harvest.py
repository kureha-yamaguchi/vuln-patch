"""Phase 1 CLI — harvest variant-CVE seed pairs from Project Zero's
public surfaces (sheet, RCA repo, narrative blog posts) and verify each
candidate via two rough signals running in parallel:

  * prose-LLM verification via gpt-5-mini;
  * URL-based file-level patch-overlap check.

A pair is `confirmed` if EITHER signal is positive.

Example usage (from the project's src/ directory):

    uv run -m cve_scan.run_p0_harvest                # full harvest + verify
    uv run -m cve_scan.run_p0_harvest --no-llm       # overlap-only verify
    uv run -m cve_scan.run_p0_harvest --no-overlap   # LLM-only verify
    uv run -m cve_scan.run_p0_harvest --refresh      # re-pull all sources
    uv run -m cve_scan.run_p0_harvest --budget-usd 3 --out ./out

Set GITHUB_TOKEN in the environment to lift GitHub API + .patch fetch
rate limits (60 → 5000 per hour).
"""
import argparse
import os
import sys

from . import config
from .classifier import Classifier
from .p0_harvest import P0Harvester, write_outputs


def parse_args():
    parser = argparse.ArgumentParser(
        description="Harvest variant-CVE seed pairs from Project Zero "
                    "sources; verify with LLM prose + URL overlap.",
    )
    parser.add_argument('--no-llm', action='store_true',
                        help="Skip the prose-LLM verification step.")
    parser.add_argument('--no-overlap', action='store_true',
                        help="Skip the URL/file-overlap verification step.")
    parser.add_argument('--refresh', action='store_true',
                        help="Force re-fetch of the P0 sheet CSVs, the RCA "
                             "repo, and the narrative posts.")
    parser.add_argument('--budget-usd', type=float,
                        default=config.LLM_MAX_BUDGET_USD,
                        help=f"Cap LLM spend in USD "
                             f"(default: ${config.LLM_MAX_BUDGET_USD:.2f}).")
    parser.add_argument('--model', type=str,
                        default=config.LLM_CLASSIFIER_MODEL,
                        help=f"OpenAI model to use "
                             f"(default: {config.LLM_CLASSIFIER_MODEL}).")
    parser.add_argument('--github-token', type=str, default=None,
                        help="GitHub token for higher API rate limits "
                             "(falls back to GITHUB_TOKEN env var).")
    parser.add_argument('--out', type=str,
                        default=config.CVE_SCAN_OUTPUT_DIR,
                        help=f"Directory for output JSON/CSV "
                             f"(default: {config.CVE_SCAN_OUTPUT_DIR}).")
    return parser.parse_args()


def main():
    args = parse_args()

    classifier = None
    if not args.no_llm:
        classifier = Classifier(model=args.model,
                                max_budget_usd=args.budget_usd)

    harvester = P0Harvester(
        classifier=classifier,
        refresh=args.refresh,
        use_overlap=not args.no_overlap,
        github_token=args.github_token or os.getenv('GITHUB_TOKEN'),
    )
    result = harvester.run()

    write_outputs(result, args.out)

    _print_summary(result)
    sys.exit(0)


def _print_summary(result) -> None:
    print("\n" + "#" * 20 + " p0 harvest " + "#" * 20)
    print(f"candidates    : {len(result.candidates)}")
    print(f"verified      : {len(result.seeds)}")
    confirmed = sum(1 for s in result.seeds if s.confirmed)
    llm_confirmed = sum(1 for s in result.seeds if s.llm_confirmed)
    overlap_confirmed = sum(1 for s in result.seeds
                            if s.overlap_status == 'overlap')
    both = sum(1 for s in result.seeds
               if s.llm_confirmed and s.overlap_status == 'overlap')
    print(f"confirmed     : {confirmed}  "
          f"(llm={llm_confirmed}, overlap={overlap_confirmed}, both={both})")
    print(f"llm spend     : ${result.spend_usd:.4f}")
    print(f"llm calls     : {result.calls_live} live + "
          f"{result.calls_cached} cached")
    print("#" * 52)


if __name__ == '__main__':
    main()
