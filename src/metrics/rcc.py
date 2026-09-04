"""Root-cause coverage — RCC(H) = |R-hat & F(H)| / |R-hat|.

Plain English: of the methods the maintainer had to change to fix the bug,
what share did the harness set actually run?

  * R-hat comes from `metrics.region` — the developer fix.
  * F(H) comes from `metrics.reached` — a JaCoCo report of the fuzz run.
  * Both are sets of `metrics.keys.MethodKey`, so this file is set algebra
    and nothing else.

`trigger_gate` is the check that must pass first. Every Defects4J bug has a
triggering test, and that test fails BECAUSE of the code the developer fix
changed. So the triggering test must run every method in R-hat. When it does
not, either R-hat is wrong or the coverage plumbing is wrong, and this bug's
RCC means nothing. Without this gate a name mismatch gives RCC = 0 on every
bug, which reads exactly like a real finding.

MEASUREMENT ONLY.
"""
from dataclasses import dataclass, field
from typing import List, Optional, Set

from metrics.keys import MethodKey
from metrics.region import Region

# How a method in R-hat matched the reached set.
EXACT = 'exact'    # same class, name and parameter types
ARITY = 'arity'    # same class, name and argument COUNT — a fallback
MISSED = 'missed'  # not reached


class ReachedSet:
    """F(H), with the one fallback the two naming schemes need.

    An exact match compares the parameter types. A rare type spelling — an
    unusual generic, a synthetic bridge method — can make one side write a
    type the other does not. The fallback then compares the argument count
    instead, so a spelling difference cannot turn a covered method into a
    missed one. It cannot separate two overloads of equal arity, so every
    fallback match is reported separately and never hidden.
    """

    def __init__(self, keys: Set[MethodKey]):
        self.exact = set(keys)
        self.by_arity = {key.loose for key in keys}

    def __len__(self) -> int:
        return len(self.exact)

    def classify(self, key: MethodKey) -> str:
        if key in self.exact:
            return EXACT
        if key.loose in self.by_arity:
            return ARITY
        return MISSED


@dataclass
class RccResult:
    """RCC for one bug and one harness set."""
    region_size: int
    covered: List[MethodKey] = field(default_factory=list)
    missed: List[MethodKey] = field(default_factory=list)
    # Covered by the argument-count fallback only. A subset of `covered`.
    by_arity_only: List[MethodKey] = field(default_factory=list)
    reached_size: int = 0

    @property
    def value(self) -> Optional[float]:
        """RCC. None when R-hat is empty — that bug leaves the population,
        it does not score zero."""
        if self.region_size == 0:
            return None
        return len(self.covered) / self.region_size


def root_cause_coverage(region: Region,
                        reached: Set[MethodKey]) -> RccResult:
    """RCC(H) for one bug."""
    found = ReachedSet(reached)
    result = RccResult(region_size=region.size, reached_size=len(found))
    for key in sorted(region.keys, key=str):
        kind = found.classify(key)
        if kind == MISSED:
            result.missed.append(key)
            continue
        result.covered.append(key)
        if kind == ARITY:
            result.by_arity_only.append(key)
    return result


@dataclass
class GateResult:
    """Did the bug's own triggering test run every method in R-hat?"""
    passed: bool
    missed: List[MethodKey] = field(default_factory=list)
    detail: str = ''


def trigger_gate(region: Region, trigger_reached: Set[MethodKey]) -> GateResult:
    """Check R-hat against the triggering test's own coverage.

    Run this before any RCC number is recorded. A bug that fails the gate
    leaves the population, and the failure is counted."""
    if region.is_empty:
        return GateResult(False, [], 'R-hat is empty: the developer fix '
                                     'changed no method body')
    result = root_cause_coverage(region, trigger_reached)
    if result.missed:
        names = ', '.join(str(key) for key in result.missed)
        return GateResult(False, result.missed,
                          f'the triggering test did not run {names}')
    return GateResult(True, [], f'all {region.size} method(s) reached')
