"""Post-hoc audit of the `same_codebase` claim on every confirmed seed.

For each confirmed seed pair, infers the codebase of both sides from the
P0 sheet's Vendor/Product columns (for CVEs) or from the bug-id prefix
(for `chromium`, `mozilla`, `github:` identifiers) and compares against
the LLM-prose verdict. Flags pairs where the heuristic disagrees with the
LLM's claim so they can be reviewed manually.

Example usage (from src/):

    uv run -m cve_scan.run_inspect_unsure
"""
import sys

from .inspect_unsure import main


if __name__ == '__main__':
    sys.exit(main())
