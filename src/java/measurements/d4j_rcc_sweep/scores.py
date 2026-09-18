"""The five root-cause region metrics, as set algebra.

Three sets and one list of crashes go in, and five numbers come out.

  * R-hat comes from `d4j_rcc_sweep.region` — the developer fix. It is the
    approximated root-cause region.
  * P comes from `d4j_rcc_sweep.patchset` — the APR patch under analysis, plus its
    static neighbourhood. It is the patch-derived set.
  * F(H) comes from `d4j_rcc_sweep.reached` — a JaCoCo report of the fuzz run. It
    is the set of methods the harness set ran.
  * C comes from `d4j_rcc_sweep.crashes` — one entry per Jazzer finding, with the
    method the crash happened in.

R-hat, P and F(H) are all sets of `d4j_rcc_sweep.keys.MethodKey`, so the four
reach metrics are set algebra and nothing else:

    RCC(H) = |R-hat & F(H)| / |R-hat|   how much of R-hat was covered
    RCR    = |R-hat & P|    / |R-hat|   how much of R-hat P recovers
    RCP(H) = |R-hat & F(H)| / |F(H)|    how much of the budget lands in R-hat
    PSC(H) = |P & F(H)|     / |P|       how much of the budget lands in P
    CSM(H) = |{c in C : site(c) in R-hat}| / |C|   are the crashes right

RCC and RCP share a numerator and differ only in the denominator. RCR uses
no runtime evidence at all: it scores the EXTRACTION, not the harness set.

`trigger_gate` is the check that must pass first. Every Defects4J bug has a
triggering test, and that test fails BECAUSE of the code the developer fix
changed. So the triggering test must run every method in R-hat. When it does
not, either R-hat is wrong or the coverage plumbing is wrong, and this bug's
RCC means nothing. Without this gate a name mismatch gives RCC = 0 on every
bug, which reads exactly like a real finding.

MEASUREMENT ONLY.
"""
from dataclasses import dataclass, field
from typing import Iterable, List, Optional, Set

from java.measurements.d4j_rcc_sweep.keys import MethodKey
from java.measurements.d4j_rcc_sweep.region import Region

# How a method in R-hat matched the reached set.
EXACT = 'exact'    # same class, name and parameter types
ARITY = 'arity'    # same class, name and argument COUNT — a fallback
MISSED = 'missed'  # not reached


class KeySet:
    """A set of methods to test membership against, with one fallback.

    Used for F(H), for P and for R-hat, because all three are compared the
    same way.

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
    found = KeySet(reached)
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


def overlap(subject: Iterable[MethodKey], other: KeySet) -> List[MethodKey]:
    """The members of `subject` that `other` holds.

    Exact first, argument-count fallback second — the same rule
    `root_cause_coverage` applies, so every intersection in this file is
    counted one way."""
    return [key for key in sorted(subject, key=str)
            if other.classify(key) != MISSED]


def _ratio(numerator: int, denominator: int) -> Optional[float]:
    """A fraction, or None when its denominator is empty.

    An empty denominator means the measurement is UNDEFINED. It never means
    zero, and it never enters a mean."""
    return numerator / denominator if denominator else None


@dataclass
class SetMetrics:
    """RCC, RCR, RCP and PSC for one bug and one harness set."""
    region_size: int
    patch_size: int
    reached_size: int
    region_in_reached: List[MethodKey] = field(default_factory=list)
    region_in_patch: List[MethodKey] = field(default_factory=list)
    patch_in_reached: List[MethodKey] = field(default_factory=list)

    @property
    def rcc(self) -> Optional[float]:
        """Root-cause coverage. None when R-hat is empty."""
        return _ratio(len(self.region_in_reached), self.region_size)

    @property
    def rcr(self) -> Optional[float]:
        """Root-cause recovery. None when R-hat is empty."""
        return _ratio(len(self.region_in_patch), self.region_size)

    @property
    def rcp(self) -> Optional[float]:
        """Root-cause precision. None when the harness set reached nothing,
        which `d4j_rcc_sweep.reached` already treats as an error."""
        return _ratio(len(self.region_in_reached), self.reached_size)

    @property
    def psc(self) -> Optional[float]:
        """Patch-derived set coverage. None when P is empty — a patch whose
        neighbourhood could not be built leaves this metric's population."""
        return _ratio(len(self.patch_in_reached), self.patch_size)


def set_metrics(region: Region, patch_set: Set[MethodKey],
                reached: Set[MethodKey]) -> SetMetrics:
    """RCC, RCR, RCP and PSC for one bug, from the three sets."""
    in_reached = KeySet(reached)
    in_patch = KeySet(patch_set)
    return SetMetrics(
        region_size=region.size,
        patch_size=len(patch_set),
        reached_size=len(reached),
        region_in_reached=overlap(region.keys, in_reached),
        region_in_patch=overlap(region.keys, in_patch),
        patch_in_reached=overlap(patch_set, in_reached),
    )


@dataclass
class CrashMatch:
    """CSM for one bug: did the crashes happen in R-hat?

    The three counts below never overlap, and with `matched` they add up to
    `total`. See `d4j_rcc_sweep.crashes` for what separates the last two."""
    total: int
    matched: int = 0       # the site is in R-hat
    off_region: int = 0    # the site is a real method, outside R-hat
    no_frame: int = 0      # an oracle fired on a value; nothing threw
    unresolved: int = 0    # a library frame that no method owns

    @property
    def value(self) -> Optional[float]:
        """CSM. None when the harness set produced no crash at all."""
        return _ratio(self.matched, self.total)


def crash_site_match(region: Region, found: List) -> CrashMatch:
    """CSM(H) for one bug. `found` is a list of `d4j_rcc_sweep.crashes.Crash`."""
    in_region = KeySet(region.keys)
    match = CrashMatch(total=len(found))
    for crash in found:
        if crash.site is not None:
            if in_region.classify(crash.site) != MISSED:
                match.matched += 1
            else:
                match.off_region += 1
        elif crash.frame is None:
            match.no_frame += 1
        else:
            match.unresolved += 1
    return match
