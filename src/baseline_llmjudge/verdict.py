"""Read one verdict out of a model response, then combine several.

The output space is one bit, so parsing is deliberately strict: the class name
must appear on a `VERDICT:` line. A response that does not carry one is a parse
failure, not a guess. `parse` reports that as None and lets the caller decide
what an unparsed sample counts as — see run_one.PARSE_FAILURE_COUNTS_AS and the
README section on it.

The last `VERDICT:` line wins. Later prompt versions ask for reasoning first,
and reasoning can mention the word on the way to a conclusion.
"""
from typing import Dict, List, Optional

INCOMPLETE = 'INCOMPLETE'
CORRECT = 'CORRECT'

# 'INCORRECT' can only mean "not correct", because the prompt names both
# classes explicitly. Accepting it protects the baseline from losing a clear
# answer to a wording slip, which would weaken it for no reason.
_POSITIVE_PREFIXES = (INCOMPLETE, 'INCORRECT')

# Markdown and backticks the model may wrap the class name in.
_DECORATION = '*`_# '


def parse(text: Optional[str]) -> Optional[bool]:
    """True for INCOMPLETE, False for CORRECT, None when unparseable."""
    if not text:
        return None
    verdict = None
    for line in text.splitlines():
        stripped = line.strip().strip(_DECORATION).upper()
        if not stripped.startswith('VERDICT'):
            continue
        _, sep, tail = stripped.partition(':')
        if not sep:
            continue
        tail = tail.strip().lstrip(_DECORATION).strip()
        # Order matters: a hedged tail such as 'INCOMPLETE (not correct)'
        # must not read as CORRECT.
        if tail.startswith(_POSITIVE_PREFIXES):
            verdict = True
        elif tail.startswith(CORRECT):
            verdict = False
    return verdict


def votes_summary(votes: List[Optional[bool]],
                  parse_failure_counts_as: bool = False) -> Dict:
    """Aggregate the samples of one patch under all three vote rules.

    `votes` holds one entry per sample: True, False, or None for an unparsed
    sample. An unparsed sample takes `parse_failure_counts_as` rather than
    disappearing, so the denominator stays the sample count the run paid for.

    All three rules are reported for every patch. The headline rule is fixed
    before the holdout runs; see the README's protocol section.
    """
    resolved = [parse_failure_counts_as if v is None else v for v in votes]
    n = len(resolved)
    positive = sum(1 for v in resolved if v)
    return {
        'n_samples': n,
        'n_positive': positive,
        'n_parse_failures': sum(1 for v in votes if v is None),
        'majority': positive * 2 > n,
        'any': positive >= 1,
        'unanimous': n > 0 and positive == n,
        'agreement': _agreement(resolved),
    }


def _agreement(resolved: List[bool]) -> Optional[float]:
    """Share of sample pairs that agree — the baseline's own variance.

    1.0 means every sample said the same thing. With n samples there are
    n*(n-1)/2 pairs. None for fewer than two samples."""
    n = len(resolved)
    if n < 2:
        return None
    positive = sum(1 for v in resolved if v)
    negative = n - positive
    agreeing = positive * (positive - 1) // 2 + negative * (negative - 1) // 2
    return agreeing / (n * (n - 1) / 2)
