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

    @property
    def has_source(self) -> bool:
        return bool(self.method_source)


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
        r'(?:@\w+(?:\s*\([^)]*\))?\s+)*'    # annotations on the same line
        r'(?:(?:public|protected|private|static|final|synchronized|'
        r'abstract|default)\s+)*'
        r'(?:void|[A-Za-z_][\w<>,\[\]\s.?]*?)\s+'
        r'{name}\s*\([^)]*\)\s*(?:throws[^{{]+)?\{{'
        r')'
    )

    def extract(self, buggy_dir: str) -> List[FailureTest]:
        try:
            triggers = self._list_triggers(buggy_dir)
        except subprocess.CalledProcessError:
            return []

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
            ))
        return out

    # --- defects4j queries -----------------------------------------------

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
            rel = subprocess.run(
                ['defects4j', 'export', '-p', 'dir.src.tests'],
                cwd=buggy_dir, check=True, capture_output=True, text=True,
            ).stdout.strip()
            return os.path.join(buggy_dir, rel) if rel else None
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