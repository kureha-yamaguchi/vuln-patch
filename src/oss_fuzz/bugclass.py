"""Crashing vs semantic bugs: who provides the oracle?

The Java front-end splits Defects4J bugs into ``crashing`` (the trigger test
dies on an escaping throwable) and ``semantic`` (it fails an assertEquals, so
the program ran to completion and merely returned the wrong value) — see
``java/bug_context/failure_test.classify_exceptions``. The distinction decides
the whole downstream shape of a run: a crashing bug needs a harness that
*reaches* the fault, a semantic bug needs a harness that reaches it **and
carries an oracle**, because nothing else will notice.

The same split exists on the OSS-Fuzz side and was previously ignored: every
record was treated as if a sanitizer would report it. That is wrong for a real
slice of the corpus, and wrong in an expensive way — a harness for an
``Incorrect-result`` bug compiles, runs, returns 0 for every input, and is
rejected by the trigger gate every single attempt until the budget is gone. The
run then reports "0 siblings" as though the method had been tested.

Two fields, answering two different questions. Keeping them apart is the whole
design, because conflating them is what put runtime-detected bugs on the
"nothing will notice this" path:

``oracle`` — **what would notice this bug?** Three answers (below). This is
    the fine-grained fact, and it is what the prompt fork and the reporting
    read.

``kind`` — **must the harness supply the verdict?** ``semantic`` iff the
    oracle is the harness itself; ``crashing`` whenever the runtime reports it,
    by whatever means. This is the Java-parity axis: Java calls a bug crashing
    when *anything* escapes the trigger test, including a project's own
    invariant check throwing, and semantic only when the program completes and
    merely returns the wrong value. ``kind`` exists so cross-language
    aggregation compares like with like, and it is deliberately a coarsening
    of ``oracle`` — never an independent judgement (enforced in
    ``BugClass.__post_init__``).

The three oracles:

``sanitizer``
    ASan/MSan/UBSan/LSan, a fatal signal, or libFuzzer's own timeout/OOM
    limits. Memory-safety and UB bugs. This is the case the pipeline already
    handled: reach the fault and the runtime does the rest. → ``crashing``

``project-assert``
    The project's own invariant check (``assert``, ``CHECK``, a fatal-error
    path). Detected at run time — the abort is caught like any crash, and the
    trigger gate needs no help — but it is a *logic* defect, and a sibling has
    to violate an invariant rather than corrupt memory, which is what the
    prompt fork is for. The build must also keep asserts enabled or the bug is
    invisible. → ``crashing``, exactly as Java classifies an escaping
    ``IllegalStateException`` from a project's own check.

``harness``
    Nothing observes it. The bug is a wrong value that the program returns
    happily. The harness itself must compute the expectation and abort on a
    mismatch, which is the C analogue of the Java semantic path's lifted
    assertion. Harness-supplied alarms are *claims*, not sanitizer findings,
    so they stay distinguishable all the way into the results JSON.
    → ``semantic``

A record we cannot read is ``unknown``, which behaves as ``crashing`` (the
sanitizer prior). That is the OPPOSITE of the Java front-end, which treats an
undeterminable bug as semantic — and the divergence is deliberate, because the
two corpora have opposite base rates. Defects4J is dominated by wrong-value
bugs; the OSS-Fuzz corpus is overwhelmingly memory-safety and UB. Guessing
"semantic" here would open the prompt with "THIS BUG DOES NOT CRASH" for the
large majority of records and make ``campaign.oracle_tag_missing`` bounce
sanitizer harnesses that need no oracle, burning the budget on repairs. The
guess is hedged rather than bet on: ``uncertain`` makes the prompt ask for an
optional wrong-value check as insurance, which costs nothing if the bug does
crash. Override with ``--bug-kind`` when the record is known.

``crash_type`` strings come from the OSS-Fuzz OSV record's ``details`` blob
(``osv._parse_details``) and are ClusterFuzz's own vocabulary, e.g.
``Heap-buffer-overflow READ 4``, ``Undefined-shift``, ``ASSERT: idx < len``,
``Incorrect-result``, ``Timeout``. Matching is substring-on-lowercase because
the type is usually decorated with the offending expression or access size.

Pure string work, no I/O, no third-party imports: unit-testable offline and
importable from anywhere in the front-end.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import List, Optional

# --- bug kinds (the headline split, same vocabulary as the Java front-end) ---
CRASHING = "crashing"
SEMANTIC = "semantic"
UNKNOWN = "unknown"

# --- who would notice the bug ------------------------------------------------
ORACLE_SANITIZER = "sanitizer"
ORACLE_PROJECT_ASSERT = "project-assert"
ORACLE_HARNESS = "harness"

# Crash types whose oracle is the project's own invariant check. ClusterFuzz
# reports these as the assert expression itself ("ASSERT: n > 0"), so match on
# the prefix. 'Abrt' is a bare abort() with no sanitizer report, which in
# practice is a project check that called abort() directly.
_ASSERT_TYPES = (
    "assert",
    "check failure",
    "check failed",
    "fatal error",
    "unreachable code",
    "security check failure",
    "user-defined crash",
    "abrt",
)

# Crash types that no runtime detector reports: the fuzz target compared two
# values and disagreed. Only differential/round-trip targets produce these, and
# only because *their own code* called abort().
_WRONG_RESULT_TYPES = (
    "incorrect-result",
    "incorrect result",
    "wrong result",
    "mismatch",
)

# Reported by LeakSanitizer when the process exits, not at a point of access.
# Kept apart from the memory-safety types because a *harness* leaks so easily by
# accident: forgetting one destructor produces the same report on a fixed
# library, so "a leak fired" is only evidence when a leak is what we came for.
# See ossfuzz.incidental_finding.
_LEAK_TYPES = (
    "direct-leak",
    "indirect-leak",
    "memory-leak",
    "memory leak",
)

# Detected by the runtime, but by libFuzzer's resource limits rather than a
# sanitizer — and only if those limits are actually passed to the binary, which
# is why these carry extra flags rather than being just another crash type.
_RESOURCE_TYPES = (
    "timeout",
    "out-of-memory",
    "out of memory",
    "oom",
)

# ClusterFuzz's per-input limits, which is what these bugs were found under.
# libFuzzer's own defaults are far looser (-timeout=1200), so a timeout bug
# simply never reproduces without them.
_RESOURCE_FLAGS = ("-timeout=25", "-rss_limit_mb=2560")


@dataclass(frozen=True)
class BugClass:
    """How a bug manifests, and therefore how the run has to be shaped."""
    kind: str                        # CRASHING | SEMANTIC | UNKNOWN
    oracle: str                      # ORACLE_SANITIZER | _PROJECT_ASSERT | _HARNESS
    reason: str                      # why, for the run log
    crash_type: Optional[str] = None
    resource: bool = False           # timeout/OOM: needs the limits below
    leak: bool = False               # LeakSanitizer reports it at exit

    def __post_init__(self) -> None:
        """``kind`` is a coarsening of ``oracle``, not a second opinion.

        SEMANTIC means exactly "the harness must supply the verdict". Any other
        oracle is reported by the runtime and therefore not semantic, however
        logic-shaped the defect is. This check exists because the reverse was
        the original bug: ``project-assert`` was filed as SEMANTIC on the
        reasonable-sounding grounds that a violated invariant is a logic error,
        which silently made ``--skip-semantic`` discard bugs the trigger gate
        handles unmodified.
        """
        if (self.kind == SEMANTIC) != (self.oracle == ORACLE_HARNESS):
            raise ValueError(
                f"inconsistent bug class: kind={self.kind!r} with "
                f"oracle={self.oracle!r}. kind must be {SEMANTIC!r} exactly "
                f"when oracle is {ORACLE_HARNESS!r} — a runtime-detected bug "
                f"is crashing whatever the shape of the defect")

    @property
    def is_semantic(self) -> bool:
        return self.kind == SEMANTIC

    @property
    def is_crashing(self) -> bool:
        return self.kind == CRASHING

    @property
    def uncertain(self) -> bool:
        """True when the class is a prior rather than a reading of the record.
        Behaves as crashing, but the prompt says so and asks for an optional
        wrong-value check, so an UNKNOWN that is really semantic can still be
        caught instead of burning the budget."""
        return self.kind == UNKNOWN

    @property
    def needs_harness_oracle(self) -> bool:
        """True when the harness must carry its own check. This is the one
        property that changes what a *valid* harness looks like: without an
        oracle the harness cannot fail, however well it reaches the code.

        Equivalent to ``is_semantic`` by the invariant above, and the name to
        prefer at call sites that care about harness shape or run scope — it
        says which fact is being relied on.
        """
        return self.oracle == ORACLE_HARNESS

    def libfuzzer_flags(self) -> List[str]:
        """Extra ``-flag=value`` args the trigger gate needs for this class."""
        return list(_RESOURCE_FLAGS) if self.resource else []

    def describe(self) -> str:
        return f"{self.kind} (oracle: {self.oracle}) — {self.reason}"


def classify(crash_type: Optional[str]) -> BugClass:
    """Classify an OSV record's crash type. Never raises; an unrecognised or
    absent type yields UNKNOWN, whose policy is identical to CRASHING — that
    keeps a record we cannot read behaving exactly as it did before this split
    existed, while still printing as 'unknown' so it can be overridden."""
    raw = (crash_type or "").strip()
    if not raw:
        return BugClass(
            kind=UNKNOWN, oracle=ORACLE_SANITIZER, crash_type=None,
            reason="the OSV record carries no crash type; assuming a "
                   "sanitizer-detected bug, which is the right prior for this "
                   "corpus but only a prior (override with --bug-kind)")
    low = raw.lower()

    if any(h in low for h in _WRONG_RESULT_TYPES):
        return BugClass(
            kind=SEMANTIC, oracle=ORACLE_HARNESS, crash_type=raw,
            reason=f"'{raw}' is a wrong-value bug — no sanitizer reports it, "
                   "so the harness must carry its own oracle")

    if any(h in low for h in _ASSERT_TYPES):
        # CRASHING, not SEMANTIC: the library aborts by itself, so the trigger
        # gate works unmodified and no oracle has to be written. The logic
        # nature of the defect is carried by the oracle field, which is what
        # steers the prompt — see the module docstring.
        return BugClass(
            kind=CRASHING, oracle=ORACLE_PROJECT_ASSERT, crash_type=raw,
            reason=f"'{raw}' is a violated invariant the project checks "
                   "itself, so the runtime still reports it; the sibling must "
                   "break the invariant, not memory")

    if any(h in low for h in _LEAK_TYPES):
        return BugClass(
            kind=CRASHING, oracle=ORACLE_SANITIZER, crash_type=raw,
            leak=True,
            reason=f"'{raw}' is reported by LeakSanitizer when the process "
                   "exits, so a leak in the harness itself looks identical")

    if any(h in low for h in _RESOURCE_TYPES):
        return BugClass(
            kind=CRASHING, oracle=ORACLE_SANITIZER, crash_type=raw,
            resource=True,
            reason=f"'{raw}' is reported by libFuzzer's resource limits, which "
                   f"only apply if passed ({' '.join(_RESOURCE_FLAGS)})")

    return BugClass(
        kind=CRASHING, oracle=ORACLE_SANITIZER, crash_type=raw,
        reason=f"'{raw}' is reported by a sanitizer at the point of failure")


def classify_forced(kind: str, crash_type: Optional[str] = None) -> BugClass:
    """Build a BugClass from an explicit ``--bug-kind`` override.

    ``semantic`` deliberately resolves to the *harness* oracle rather than the
    project-assert one: someone overriding the classification is saying the
    runtime will not report this bug, and demanding an oracle is the safe read
    of that. If the project's own assert is the oracle, the record's crash type
    says so and ``classify`` finds it without an override.
    """
    if kind == SEMANTIC:
        return BugClass(
            kind=SEMANTIC, oracle=ORACLE_HARNESS, crash_type=crash_type,
            reason="forced by --bug-kind semantic: the harness must carry its "
                   "own oracle")
    if kind == CRASHING:
        return BugClass(
            kind=CRASHING, oracle=ORACLE_SANITIZER, crash_type=crash_type,
            reason="forced by --bug-kind crashing: the runtime is the oracle")
    raise ValueError(f"unknown bug kind: {kind!r}")
