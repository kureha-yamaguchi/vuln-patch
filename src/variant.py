"""Language-agnostic variant-analysis steering.

The core research heuristic of this project — "given a set of harnesses
already probing a vulnerability's root cause, produce the next harness as a
*sibling* aimed at the still-uncovered part of that region" — is identical
whether the target is Jazzer/Java or libFuzzer/C. It was originally embedded
in the Java ``PromptBuilder``; it now lives here so the OSS-Fuzz/libFuzzer
pipeline reuses the exact same steering instead of forking a second copy that
could silently drift.

This module deliberately has no third-party imports (no javalang, no clang)
so either front-end can import it without dragging in the other's toolchain.
"""
from typing import List, Optional

import config


def variant_analysis_directive(reachable: List[str],
                               covered: List[str],
                               signatures: List[str],
                               cap: Optional[int] = None) -> str:
    """Steer successive harnesses across the root-cause neighbourhood.

    ``reachable`` are the functions in/around the patched region (the head of
    the reachable set). ``covered`` are functions earlier accepted harnesses
    already exercised, and ``signatures`` the crashes already found; both are
    used to push the next harness at something new. The wording is neutral
    ("function", "call graph") so it reads correctly for methods and free
    functions alike.
    """
    if cap is None:
        cap = config.MAX_REACHABLE_IN_PROMPT
    shown = reachable[:cap]
    covered_set = set(covered)
    remaining = [r for r in shown if r not in covered_set]
    parts: List[str] = [
        "This harness is ONE of a set probing the root cause of"
        " the vulnerability the patch under analysis is meant to fix."
        " The patched lines sit at the head of the reachable region"
        " below. A valid sibling bug is one that:\n"
        "  (a) lives in this region (same function or call graph), AND\n"
        "  (b) stems from the SAME root cause\n"
        "<root_cause_reachable>",
        *(f"- {name}" for name in shown),
        "</root_cause_reachable>",
    ]
    if len(reachable) > cap:
        parts.append(
            f"(+{len(reachable) - cap} more reachable functions omitted.)"
        )

    if covered or signatures:
        parts.append(
            "Already covered by earlier harnesses — target something"
            " different:"
        )
        if covered:
            parts.append("Functions covered:")
            parts.extend(f"- {c}" for c in sorted(covered_set))
        if signatures:
            parts.append("Crashes already found:")
            parts.extend(f"- {s}" for s in signatures)
        if remaining:
            parts.append("Uncovered functions to steer toward:")
            parts.extend(f"- {r}" for r in remaining)
    else:
        parts.append(
            "First harness: establish the most direct path from the"
            " fuzz entrypoint through the patched code."
        )
    return '\n'.join(parts)
