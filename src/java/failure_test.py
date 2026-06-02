"""Extract the bug-triggering test(s) shipped with a Defects4J bug.

For every Defects4J bug the framework records the test method(s) that
expose the fault (`defects4j export -p tests.trigger`). Slicing those
methods out of the checkout's test sources gives a worked example of an
input that already reaches the root cause — strong steering material
for the LLM, which can then ask itself: "can my FuzzedDataProvider calls
produce values equivalent to the ones this test constructs?".

The output of this module is consumed by `PromptBuilder`. If anything
fails — no triggering tests, source file missing, brace counting goes
sideways on an unusual layout — we degrade gracefully and emit a
FailureTest with `method_source=None`, so the prompt at least cites the
test by name.
"""
import os
import re
import subprocess
from dataclasses import dataclass
from typing import List, Optional, Tuple


@dataclass
class FailureTest:
    """A single bug-triggering JUnit test."""
    test_class: str               # e.g. 'org.apache.commons.lang3.StringUtilsTest'
    test_method: str              # e.g. 'testCapitalize'
    source_path: Optional[str]    # absolute path to the .java file, if found
    method_source: Optional[str]  # the method body, brace-balanced
    # Fully-qualified throwable the trigger test fails with on the buggy
    # checkout (e.g. 'java.lang.ArrayIndexOutOfBoundsException'), or None
    # if it couldn't be determined. This is the crashing-vs-semantic
    # discriminator: an assertion-failure type means the bug is semantic.
    exception_type: Optional[str] = None

    @property
    def has_source(self) -> bool:
        return bool(self.method_source)


# JUnit assertion failures mean the trigger test failed on a *comparison*,
# not on an uncaught application exception — i.e. the bug is semantic
# (non-crashing) rather than a crash Jazzer can catch directly.
_ASSERTION_THROWABLES = frozenset({
    'junit.framework.AssertionFailedError',
    'junit.framework.ComparisonFailure',
    'org.junit.ComparisonFailure',
    'org.junit.internal.ArrayComparisonFailure',
    'java.lang.AssertionError',
})


def is_crashing_bug(failure_tests: List['FailureTest']) -> bool:
    """A bug is 'crashing' (in scope for the current pipeline) if at
    least one of its trigger tests fails with a thrown application
    exception rather than a JUnit assertion comparison.

    Semantic bugs, whose trigger tests fail only an assertEquals-style
    check, throw an assertion type and are out of scope until the
    differential oracle lands. If no exception type could be determined
    for any test we conservatively treat the bug as non-crashing, since
    the crash gate would just burn the whole attempt budget on it."""
    for ft in failure_tests:
        if ft.exception_type and ft.exception_type not in _ASSERTION_THROWABLES:
            return True
    return False


class FailureTestExtractor:
    """Locate and read the source of each test that triggers the bug.

    Robust to the usual oddities: missing `dir.src.tests`, methods with
    annotations on their own line, strings containing braces, line and
    block comments. If extraction fails for any test we still return a
    FailureTest with empty `method_source`.
    """

    # Anchored to start-of-line / after `;` so we don't match
    # invocations like `foo.methodName(`. Allows leading annotations,
    # modifiers, and a return type (void or otherwise). The actual
    # declaration is wrapped in capture group 1 so callers can locate
    # the first real character of the method (the regex's overall
    # match.start() points at the leading anchor character, which
    # isn't where we want the slice to begin).
    _METHOD_DECL_TEMPLATE = (
        r'(?:^|[\n\r;])[ \t]*'
        r'('                                # group 1: method declaration
        r'(?:@\w+(?:\s*\([^()]*(?:\([^()]*\)[^()]*)*\))?\s+)*'  # annotations on the same line
        r'(?:(?:public|protected|private|static|final|synchronized|'
        r'abstract|default)\s+)*'
        r'(?:void|[A-Za-z_][\w<>,\[\]\s.?]*?)\s+'
        r'{name}\s*\([^)]*\)\s*(?:throws[^{{]+)?\{{'
        r')'
    )

    def extract(self, buggy_dir: str,
                project_name: Optional[str] = None,
                bug_id: Optional[str] = None) -> List[FailureTest]:
        try:
            triggers = self._list_triggers(buggy_dir)
        except subprocess.CalledProcessError:
            return []

        # Map `class::method` -> thrown throwable, used to classify the
        # bug as crashing vs semantic. Read from `defects4j info`, which
        # records the root cause as static metadata (no checkout, compile,
        # or test run needed). Best-effort: an empty map just leaves
        # exception_type None everywhere, which is_crashing_bug treats as
        # non-crashing.
        exc_by_test = (self._trigger_exceptions(project_name, bug_id)
                       if project_name and bug_id else {})

        test_src_dir = self._test_source_dir(buggy_dir)
        out: List[FailureTest] = []
        for cls, mtd in triggers:
            path = self._find_source(buggy_dir, test_src_dir, cls)
            body = self._extract_method(path, mtd) if path else None
            out.append(FailureTest(
                test_class=cls,
                test_method=mtd,
                source_path=path,
                method_source=body,
                exception_type=exc_by_test.get(f'{cls}::{mtd}'),
            ))
        return out

    # --- defects4j queries -----------------------------------------------

    # The `defects4j info` block we parse looks like:
    #     Root cause in triggering tests:
    #      - org.apache.commons.lang.math.NumberUtilsTest::testLang300
    #        --> java.lang.NumberFormatException: 1l is not a valid number
    # i.e. a `- class::method` line followed by a `--> throwable[: msg]`
    # line, one pair per triggering test.
    _ROOT_CAUSE_HEADER = 'Root cause in triggering tests:'
    _TRIGGER_NAME_RE = re.compile(r'^-\s+([\w.$]+::[\w$]+)\s*$')
    _ROOT_CAUSE_RE = re.compile(r'^-->\s*([\w.$]+)')

    def _trigger_exceptions(self, project_name: str, bug_id: str) -> dict:
        """Map each `class::method` to the throwable its root cause names,
        read from `defects4j info -p <project> -b <bug_id>`. This is
        static metadata, so it needs neither a checkout nor a test run.
        Best-effort: any failure returns {} and the bug is then treated
        as non-crashing by `is_crashing_bug`."""
        try:
            result = subprocess.run(
                ['defects4j', 'info', '-p', project_name, '-b', bug_id],
                capture_output=True, text=True,
            )
        except (OSError, subprocess.SubprocessError):
            return {}
        if result.returncode != 0:
            return {}

        out: dict = {}
        in_section = False
        current: Optional[str] = None
        for line in result.stdout.splitlines():
            stripped = line.strip()
            if stripped.startswith(self._ROOT_CAUSE_HEADER):
                in_section = True
                continue
            if not in_section:
                continue
            name_m = self._TRIGGER_NAME_RE.match(stripped)
            if name_m:
                current = name_m.group(1)
                continue
            cause_m = self._ROOT_CAUSE_RE.match(stripped)
            if cause_m and current:
                out[current] = cause_m.group(1)
                current = None
                continue
            # A divider line (`----...`) or any non-blank line that is
            # neither a trigger name nor a `-->` cause marks the end of
            # the root-cause block. Dividers start with `-`, so test for
            # them explicitly rather than relying on the leading char.
            if set(stripped) == {'-'} and len(stripped) > 2:
                break
            if stripped and not stripped.startswith('-'):
                break
        return out

    def _list_triggers(self, buggy_dir: str) -> List[Tuple[str, str]]:
        """Return `[(class, method), ...]` from `tests.trigger`. The
        property is static metadata, so it doesn't require the project
        to be compiled first."""
        result = subprocess.run(
            ['defects4j', 'export', '-p', 'tests.trigger'],
            cwd=buggy_dir, check=True, capture_output=True, text=True,
        )
        triggers: List[Tuple[str, str]] = []
        for line in result.stdout.splitlines():
            line = line.strip()
            if not line or '::' not in line:
                continue
            cls, _, mtd = line.partition('::')
            triggers.append((cls.strip(), mtd.strip()))
        return triggers

    def _test_source_dir(self, buggy_dir: str) -> Optional[str]:
        """Resolve the project-relative test source directory via
        `defects4j export -p dir.src.tests`. May not exist on every
        project layout — callers must cope with None."""
        try:
            raw = subprocess.run(
                ['defects4j', 'export', '-p', 'dir.src.tests'],
                cwd=buggy_dir, check=True, capture_output=True, text=True,
            ).stdout.strip()
            for part in raw.split(':'):
                part = part.strip()
                if not part:
                    continue
                candidate = os.path.join(buggy_dir, part)
                if os.path.isdir(candidate):
                    return candidate
            return None
        except subprocess.CalledProcessError:
            return None

    # --- file lookup -----------------------------------------------------

    def _find_source(self, buggy_dir: str,
                     test_src_dir: Optional[str],
                     test_class: str) -> Optional[str]:
        rel = test_class.replace('.', os.sep) + '.java'
        if test_src_dir:
            candidate = os.path.join(test_src_dir, rel)
            if os.path.isfile(candidate):
                return candidate
        # Some d4j projects have multi-module / generated layouts where
        # dir.src.tests isn't a single canonical place. Fall back to a
        # walk of the checkout for the matching file.
        basename = os.path.basename(rel)
        for root, _, files in os.walk(buggy_dir):
            if basename in files:
                full = os.path.join(root, basename)
                if full.endswith(os.sep + rel):
                    return full
        return None

    # --- method extraction ----------------------------------------------

    def _extract_method(self, source_path: str,
                        method_name: str) -> Optional[str]:
        """Slice the named method (and preceding annotations) out of
        the file at `source_path` using brace counting that respects
        strings, char literals, and comments."""
        try:
            with open(source_path, 'r',
                      encoding='utf-8', errors='replace') as fh:
                source = fh.read()
        except OSError:
            return None

        pattern = re.compile(
            self._METHOD_DECL_TEMPLATE.format(name=re.escape(method_name))
        )
        match = pattern.search(source)
        if not match:
            return None

        open_brace = match.end() - 1
        # Capture group 1 starts at the first real character of the
        # declaration (annotation, modifier, or return type). Without
        # this the slice would start at the regex's leading-newline
        # anchor, pulling in the enclosing class declaration.
        start = source.rfind('\n', 0, match.start(1)) + 1
        # Walk back through any annotation lines preceding the
        # declaration — the regex only matches same-line annotations.
        while True:
            prev_end = start - 1
            if prev_end < 0:
                break
            prev_start = source.rfind('\n', 0, prev_end) + 1
            prev_line = source[prev_start:prev_end].strip()
            if prev_line.startswith('@'):
                start = prev_start
            else:
                break

        return self._slice_balanced(source, start, open_brace)

    @staticmethod
    def _slice_balanced(source: str, start: int,
                        open_brace: int) -> Optional[str]:
        """Brace-count from `open_brace`, skipping `{`/`}` that appear
        inside string literals, char literals, line comments, or block
        comments. Returns `source[start:end]` or None on imbalance."""
        depth = 0
        i = open_brace
        n = len(source)
        while i < n:
            c = source[i]
            if c == '"':
                i += 1
                while i < n and source[i] != '"':
                    if source[i] == '\\' and i + 1 < n:
                        i += 2
                    else:
                        i += 1
                i += 1
            elif c == "'":
                i += 1
                while i < n and source[i] != "'":
                    if source[i] == '\\' and i + 1 < n:
                        i += 2
                    else:
                        i += 1
                i += 1
            elif c == '/' and i + 1 < n and source[i + 1] == '/':
                nl = source.find('\n', i)
                i = n if nl == -1 else nl + 1
            elif c == '/' and i + 1 < n and source[i + 1] == '*':
                end = source.find('*/', i + 2)
                i = n if end == -1 else end + 2
            elif c == '{':
                depth += 1
                i += 1
            elif c == '}':
                depth -= 1
                i += 1
                if depth == 0:
                    return source[start:i]
            else:
                i += 1
        return None