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

So classification here answers one question — **what would notice this bug?** —
with three answers, which is what the rest of the pipeline branches on:

``sanitizer``
    ASan/MSan/UBSan/LSan, a fatal signal, or libFuzzer's own timeout/OOM
    limits. Memory-safety and UB bugs. This is the case the pipeline already
    handled: reach the fault and the runtime does the rest.

``project-assert``
    The project's own invariant check (``assert``, ``CHECK``, a fatal-error
    path). Still detected at run time — the abort is caught like any crash —
    but it is a *logic* defect, and a sibling has to violate an invariant
    rather than corrupt memory. The prompt has to say so, and the build must
    keep asserts enabled or the bug is invisible.

``harness``
    Nothing observes it. The bug is a wrong value that the program returns
    happily. The harness itself must compute the expectation and abort on a
    mismatch, which is the C analogue of the Java semantic path's lifted
    assertion. Harness-supplied alarms are *claims*, not sanitizer findings,
    so they stay distinguishable all the way into the results JSON.

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

    @property
    def is_semantic(self) -> bool:
        return self.kind == SEMANTIC

    @property
    def is_crashing(self) -> bool:
        return self.kind == CRASHING

    @property
    def needs_harness_oracle(self) -> bool:
        """True when the harness must carry its own check. This is the one
        property that changes what a *valid* harness looks like: without an
        oracle the harness cannot fail, however well it reaches the code."""
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
                   "sanitizer-detected bug (override with --bug-kind)")
    low = raw.lower()

    if any(h in low for h in _WRONG_RESULT_TYPES):
        return BugClass(
            kind=SEMANTIC, oracle=ORACLE_HARNESS, crash_type=raw,
            reason=f"'{raw}' is a wrong-value bug — no sanitizer reports it, "
                   "so the harness must carry its own oracle")

    if any(h in low for h in _ASSERT_TYPES):
        return BugClass(
            kind=SEMANTIC, oracle=ORACLE_PROJECT_ASSERT, crash_type=raw,
            reason=f"'{raw}' is a violated invariant the project checks "
                   "itself; the sibling must break the invariant, not memory")

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
