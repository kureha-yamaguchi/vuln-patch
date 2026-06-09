"""Capture the exact input that triggers a Defects4J crash bug.

Motivation
----------
The harness generator was being seeded with the *source of the trigger
test*, then left to infer which value actually crashes. For tests with a
single string-literal call that inference is easy, but real trigger tests
(e.g. Lang_27's ``testCreateNumber``) contain dozens of calls and the one
that throws is often exactly the line lost to prompt truncation. The model
then guesses, and — as the Lang_27 campaign showed — guesses wrong for 50
attempts straight.

This module removes the guessing. It runs the bug-triggering test against
the already-checked-out *buggy* code and reads the crashing value back out
of the failure output (stack trace + exception message). That value is the
ground-truth anchor: feeding it verbatim into the prompt guarantees the
trigger gate can pass.

Generality
----------
The approach is bug-type-agnostic because it observes the *runtime*
failure rather than pattern-matching test syntax:

* It does not assume the crash is a string-index bug, an NPE, or anything
  else — it just records what the JVM reported.
* It extracts, in priority order, (a) any quoted literal echoed in the
  exception message, (b) the exception's own detail message, and
  (c) the throwing location from the stack trace. Each is useful context
  for a different class of bug; callers decide how much to surface.

Everything here is *additive*. If capture fails for any reason (D4J layout
differences, an exotic test runner, a sandbox with no JVM) the public
entry point returns ``None`` and the caller falls back to the previous
test-source-only prompt. No bug type can regress relative to today.
"""
from __future__ import annotations

import os
import re
import shutil
import subprocess
from dataclasses import dataclass
from typing import List, Optional

import config


# How long to let the single trigger test run. A crashing unit test fails
# fast; this is a guard against a hang, not a real budget.
_DEFAULT_TEST_TIMEOUT = getattr(config, "TRIGGER_TEST_TIMEOUT_SECONDS", 300)


@dataclass
class CrashInput:
    """Ground-truth crash evidence captured from the buggy version.

    Attributes
    ----------
    test_class, test_method:
        The D4J trigger test this evidence came from.
    exception_type:
        Fully-qualified throwable observed at runtime (may refine, or
        fill in, the static ``FailureTest.exception_type``).
    message:
        The throwable's detail message, verbatim. Often contains the
        offending value (e.g. ``For input string: "1eE"``).
    literals:
        Distinct quoted string literals recovered from the message and
        the immediate stack frames — the most directly reusable anchors.
    throw_site:
        ``ClassName.method(File.java:NN)`` of the top application frame,
        for prompts that want to point at *where* it broke.
    raw_trace:
        Truncated raw failure text, kept for debugging / fallback.
    """
    test_class: str
    test_method: str
    exception_type: Optional[str] = None
    message: Optional[str] = None
    literals: Optional[List[str]] = None
    throw_site: Optional[str] = None
    raw_trace: Optional[str] = None

    @property
    def best_anchor(self) -> Optional[str]:
        """The single most reusable crashing literal, if any.

        Prefers a unique literal; when several appear, the first is still
        a far better anchor than nothing, because it came from the actual
        failure rather than the test's happy-path assertions."""
        if self.literals:
            return self.literals[0]
        return None

    @property
    def has_evidence(self) -> bool:
        return bool(self.message or self.literals or self.throw_site)


class CrashInputExtractor:
    """Run a D4J trigger test on the buggy checkout and read the crash.

    The extractor is deliberately tolerant: any failure to invoke D4J or
    parse its output yields ``None`` rather than raising, so a capture
    problem degrades the prompt gracefully instead of aborting the run.
    """

    # `Exception in thread "main" <fqcn>: <message>` and the more common
    # JUnit form `<fqcn>: <message>` that D4J echoes in failing_tests.
    _EXC_LINE_RE = re.compile(
        r'(?:^|\s)'
        r'(?P<type>(?:[A-Za-z_$][\w$]*\.)+[A-Za-z_$][\w$]*'
        r'(?:Exception|Error|Throwable))'
        r'(?::\s*(?P<msg>.*))?$'
    )
    # Top application stack frame: `at pkg.Class.method(File.java:42)`.
    # We skip JDK-internal frames (java.*, javax.*, sun.*, jdk.*) so the
    # reported site is where the *project* code failed, not String.java.
    _FRAME_RE = re.compile(
        r'\bat\s+(?P<loc>(?P<fqcn>(?:[\w$]+\.)+[\w$]+)'
        r'\((?P<file>[\w$]+\.java):(?P<lineno>\d+)\))'
    )
    _JDK_PREFIXES = ('java.', 'javax.', 'sun.', 'jdk.', 'com.sun.')
    # Quoted literal inside a detail message, e.g. For input string: "1eE".
    _QUOTED_RE = re.compile(r'"((?:[^"\\]|\\.)*)"')

    def __init__(self, test_timeout: int = _DEFAULT_TEST_TIMEOUT):
        self.test_timeout = test_timeout

    # --- public API ------------------------------------------------------

    def extract(self, buggy_dir: str, test_class: str,
                test_method: str,
                candidate_literals: Optional[List[str]] = None
                ) -> Optional[CrashInput]:
        """Capture crash evidence for ``test_class::test_method``.

        ``candidate_literals`` is an optional, ordered list of string
        literals mined from the trigger test by the caller. Java often
        omits the offending value from an exception message (e.g.
        ``StringIndexOutOfBoundsException: ... index out of range: -1``);
        when the runtime trace yields no quotable input, these candidates
        are surfaced as the fallback anchor. This keeps the capture
        general: value-echoing exceptions use the message, value-free ones
        fall back to the test's own inputs.

        Returns ``None`` if D4J is unavailable or the test produced no
        parseable failure (e.g. it is not actually a crashing test on this
        checkout)."""
        trace = self._failure_text(buggy_dir, test_class, test_method)
        if not trace:
            return None
        return self._parse(trace, test_class, test_method,
                           candidate_literals or [])

    # --- D4J invocation --------------------------------------------------

    def _failure_text(self, buggy_dir: str, test_class: str,
                      test_method: str) -> Optional[str]:
        """Return the raw failure output for the single trigger test.

        Strategy, cheapest first:

        1. Read D4J's recorded ``failing_tests`` file if the checkout
           already has one — it holds the exact stack trace and costs no
           JVM time.
        2. Otherwise run ``defects4j test -t <class::method>`` and read
           the freshly written ``failing_tests``.
        """
        recorded = self._read_recorded_failure(buggy_dir, test_method,
                                               test_class)
        if recorded:
            return recorded

        if shutil.which("defects4j") is None:
            return None

        target = f"{test_class}::{test_method}"
        try:
            subprocess.run(
                ["defects4j", "test", "-t", target],
                cwd=buggy_dir,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                timeout=self.test_timeout,
                check=False,
            )
        except (subprocess.TimeoutExpired, OSError):
            return None

        return self._read_recorded_failure(buggy_dir, test_method,
                                           test_class)

    @staticmethod
    def _read_recorded_failure(buggy_dir: str, test_method: str,
                               test_class: str) -> Optional[str]:
        """Pull the block for this test out of D4J's ``failing_tests``.

        The file concatenates one record per failing test, each beginning
        with a ``--- <class>::<method>`` header followed by the trace.
        We return just the matching record so unrelated failures never
        pollute the captured input."""
        path = os.path.join(buggy_dir, "failing_tests")
        if not os.path.isfile(path):
            return None
        try:
            with open(path, "r", errors="replace") as fh:
                content = fh.read()
        except OSError:
            return None

        header = f"{test_class}::{test_method}"
        records = re.split(r'(?m)^---\s+', content)
        for rec in records:
            if rec.startswith(header):
                return rec
        # No per-test header matched; if the file is about this one test
        # only, return the whole thing rather than nothing.
        return content if header.split("::")[0] in content else None

    # --- parsing ---------------------------------------------------------

    def _parse(self, trace: str, test_class: str,
               test_method: str,
               candidate_literals: List[str]) -> Optional[CrashInput]:
        exc_type: Optional[str] = None
        message: Optional[str] = None

        for line in trace.splitlines():
            m = self._EXC_LINE_RE.search(line.strip())
            if m:
                exc_type = m.group("type")
                message = (m.group("msg") or "").strip() or None
                break  # first throwable line is the proximate cause

        # First *application* stack frame — skip JDK-internal frames so we
        # point at the project code, not String.substring.
        throw_site = None
        for fm in self._FRAME_RE.finditer(trace):
            fqcn = fm.group("fqcn")
            if not fqcn.startswith(self._JDK_PREFIXES):
                throw_site = fm.group("loc")
                break
        if throw_site is None:
            first = self._FRAME_RE.search(trace)
            throw_site = first.group("loc") if first else None

        # Anchor literals: prefer values echoed in the runtime message
        # (most reliable), then fall back to the test-source candidates.
        literals: List[str] = []
        if message:
            literals.extend(self._QUOTED_RE.findall(message))
        literals.extend(candidate_literals)
        seen = set()
        literals = [s for s in literals
                    if s and not (s in seen or seen.add(s))]

        result = CrashInput(
            test_class=test_class,
            test_method=test_method,
            exception_type=exc_type,
            message=message,
            literals=literals or None,
            throw_site=throw_site,
            raw_trace=trace[:2000] if trace else None,
        )
        return result if result.has_evidence or exc_type else None