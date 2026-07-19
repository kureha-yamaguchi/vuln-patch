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
from typing import Iterable, List, Optional, Tuple


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
    # H2: the failure message this test produced on the BUGGY build
    # ('...AssertionFailedError: expected:<NaN> but was:<4.0>') — names
    # the diverging observable and the wrong value. Captured by the P0.1
    # safety net run; None on checkouts that predate the capture.
    failure_message: Optional[str] = None
    # H1: the parts of the test class this method actually uses —
    # setUp/@Before, helper methods, referenced fields/constants, fixture
    # file contents — so the harness writer replicates the real setup
    # instead of improvising it. None when unresolvable.
    support_source: Optional[str] = None

    @property
    def has_source(self) -> bool:
        return bool(self.method_source)


# JUnit assertion failures mean the trigger test failed on a *comparison*,
# not on an uncaught application exception — i.e. the bug is semantic
# (non-crashing) rather than a crash Jazzer can catch directly.
#
# Public (no leading underscore): this is the single source of truth for
# the crashing/semantic distinction, shared with classify_bugs.py, which
# classifies bugs straight from defects4j's static trigger_tests files
# instead of through `defects4j info` — keeping one throwable set here
# means the two can't quietly drift out of agreement with each other.
ASSERTION_THROWABLES = frozenset({
    'junit.framework.AssertionFailedError',
    'junit.framework.ComparisonFailure',
    'org.junit.ComparisonFailure',
    'org.junit.internal.ArrayComparisonFailure',
    'java.lang.AssertionError',
})


def classify_exceptions(exception_types: Iterable[Optional[str]]) -> str:
    """Classify a bug from the throwable name(s) its trigger test(s)
    fail with: 'crashing' if any is a thrown application exception,
    'semantic' if every throwable seen is a JUnit assertion type, or
    'unknown' if no throwable could be determined for any test at all.
    A crash on any trigger test counts as evidence of a crashing defect,
    even if others are assertion failures."""
    seen = [e for e in exception_types if e]
    if not seen:
        return "unknown"
    if any(e not in ASSERTION_THROWABLES for e in seen):
        return "crashing"
    return "semantic"


def is_crashing_bug(failure_tests: List['FailureTest']) -> bool:
    """A bug is 'crashing' (in scope for the current pipeline) if at
    least one of its trigger tests fails with a thrown application
    exception rather than a JUnit assertion comparison.

    Semantic bugs, whose trigger tests fail only an assertEquals-style
    check, throw an assertion type and are out of scope until the
    differential oracle lands. If no exception type could be determined
    for any test we conservatively treat the bug as non-crashing, since
    the crash gate would just burn the whole attempt budget on it."""
    return classify_exceptions(
        ft.exception_type for ft in failure_tests) == "crashing"


# The `defects4j info` block we parse looks like:
#     Root cause in triggering tests:
#      - org.apache.commons.lang.math.NumberUtilsTest::testLang300
#        --> java.lang.NumberFormatException: 1l is not a valid number
# i.e. a `- class::method` line followed by a `--> throwable[: msg]`
# line, one pair per triggering test.
_ROOT_CAUSE_HEADER = 'Root cause in triggering tests:'
_TRIGGER_NAME_RE = re.compile(r'^-\s+([\w.$]+::[\w$]+)\s*$')
_ROOT_CAUSE_RE = re.compile(r'^-->\s*([\w.$]+)')


def _trigger_exceptions_from_info(project_name: str, bug_id: str) -> dict:
    """Map each `class::method` to the throwable its root cause names,
    read from `defects4j info -p <project> -b <bug_id>`. This is static
    metadata, so it needs neither a checkout nor a test run.
    Best-effort: any failure returns {} and the bug is then treated as
    non-crashing by `is_crashing_bug` / `classify_bug_kind`."""
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
        if stripped.startswith(_ROOT_CAUSE_HEADER):
            in_section = True
            continue
        if not in_section:
            continue
        name_m = _TRIGGER_NAME_RE.match(stripped)
        if name_m:
            current = name_m.group(1)
            continue
        cause_m = _ROOT_CAUSE_RE.match(stripped)
        if cause_m and current:
            out[current] = cause_m.group(1)
            current = None
            continue
        # A divider line (`----...`) or any non-blank line that is
        # neither a trigger name nor a `-->` cause marks the end of the
        # root-cause block. Dividers start with `-`, so test for them
        # explicitly rather than relying on the leading char.
        if set(stripped) == {'-'} and len(stripped) > 2:
            break
        if stripped and not stripped.startswith('-'):
            break
    return out


def classify_bug_kind(project_name: str, bug_id: str) -> str:
    """Classify a bug as 'crashing' or 'semantic' from defects4j's static
    root-cause metadata alone — no checkout or test run needed. This is
    the same signal `is_crashing_bug` uses, just read straight from
    `defects4j info` instead of from `FailureTest` objects assembled
    after a checkout: the "Root cause in triggering tests" section
    already enumerates every trigger test defects4j has a throwable for.
    Lets callers reject semantic bugs (--skip_semantic) before paying
    for the checkout they'd otherwise discard the result of."""
    exc_by_test = _trigger_exceptions_from_info(project_name, bug_id)
    verdict = classify_exceptions(exc_by_test.values())
    return "crashing" if verdict == "crashing" else "semantic"


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
            triggers = []

        # Map `class::method` -> thrown throwable, used to classify the
        # bug as crashing vs semantic. Read from `defects4j info`, which
        # records the root cause as static metadata (no checkout, compile,
        # or test run needed). Best-effort: an empty map just leaves
        # exception_type None everywhere, which is_crashing_bug treats as
        # non-crashing.
        exc_by_test = (self._trigger_exceptions(project_name, bug_id)
                       if project_name and bug_id else {})

        # Fallback: `defects4j export -p tests.trigger` needs a valid,
        # correctly-versioned checkout and fails on some (e.g. a stale dir),
        # which would silently drop the bug. `defects4j info` is static and
        # lists the SAME trigger tests, so recover them from there rather
        # than treating the bug as having no trigger test at all.
        if not triggers and exc_by_test:
            triggers = [(k.split('::', 1)[0], k.split('::', 1)[1])
                        for k in exc_by_test if '::' in k]

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

    def _trigger_exceptions(self, project_name: str, bug_id: str) -> dict:
        return _trigger_exceptions_from_info(project_name, bug_id)

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

# --------------------------------------------------------------------------
# H1: resolve what the trigger-test method USES from its test class, so the
# harness prompt can show the whole scenario instead of a bare method body.
# Every setup-divergence failure traced in the 2026-07-18 quality check
# (Closure-62's formatter() helper, Chart-26's entity wiring) came from the
# model improvising setup it was never shown. Selected context only — the
# measured lesson is that BULK context makes the model worse, so each piece
# is included because the method references it, and the total is capped.

# JUnit / language names that look like calls but never live in the class.
_NON_HELPER_CALLS = frozenset({
    'assertEquals', 'assertTrue', 'assertFalse', 'assertNull',
    'assertNotNull', 'assertSame', 'assertNotSame', 'assertArrayEquals',
    'assertThat', 'fail', 'if', 'for', 'while', 'switch', 'catch', 'new',
    'return', 'super', 'this', 'synchronized', 'assertNotEquals',
})
_CALL_RE = re.compile(r'(?<![\w.])([a-zA-Z_]\w*)\s*\(')
_IDENT_RE = re.compile(r'\b([A-Za-z_]\w*)\b')
_BEFORE_RE = re.compile(
    r'@(?:Before|BeforeClass|BeforeEach|Override)\s+(?:public\s+|protected\s+|'
    r'static\s+)*void\s+(setUp|\w*[Ss]etup\w*)\s*\(')
_EXTENDS_RE = re.compile(r'\bclass\s+\w+\s+extends\s+([\w.]+)')
_FIXTURE_RE = re.compile(
    r'"([\w][\w./ -]*\.(?:txt|xml|json|csv|properties|ser|dat|html|js|java|'
    r'bin|gz|zip|properties))"')


def _read(path):
    try:
        with open(path, 'r', encoding='utf-8', errors='replace') as fh:
            return fh.read()
    except OSError:
        return None


def _field_declarations(class_source: str, wanted: set) -> List[str]:
    """Single-line field/constant declarations of the class whose name is
    in `wanted`. Line-based on purpose: multi-line initializers are rare
    in test classes and a missed one costs a context line, not a verdict."""
    out = []
    decl_re = re.compile(
        r'^\s*(?:public|protected|private)\s+(?:static\s+|final\s+)*'
        r'[\w<>\[\],.\s]+?\s+(' + '|'.join(re.escape(w) for w in wanted)
        + r')\s*[=;]')
    for line in class_source.splitlines():
        if decl_re.match(line):
            out.append(line.strip())
            if len(out) >= 12:
                break
    return out


def resolve_test_support(ft: FailureTest,
                         checkout_dir: Optional[str] = None,
                         cap: int = 8000) -> Optional[str]:
    """Assemble the test-class context `ft.method_source` depends on:
    setUp/@Before methods, same-class (and one-superclass-hop) helper
    methods it calls, field/constant declarations it references, and the
    content of fixture files it names. Returns one labeled block, capped
    at `cap` chars, or None if nothing could be resolved."""
    if not (ft.source_path and ft.method_source):
        return None
    class_src = _read(ft.source_path)
    if not class_src:
        return None
    extractor = FailureTestExtractor()
    sections: List[str] = []
    seen_methods = {ft.test_method}

    def _grab(path, name, label):
        if name in seen_methods:
            return None
        seen_methods.add(name)
        body = extractor._extract_method(path, name)
        if body:
            sections.append(f"// --- {label} ---\n{body}")
        return body

    # 1. Lifecycle setup always matters when present.
    for m in _BEFORE_RE.finditer(class_src):
        _grab(ft.source_path, m.group(1), f"{m.group(1)}() (test setup)")
    if 'void setUp' in class_src and 'setUp' not in seen_methods:
        _grab(ft.source_path, 'setUp', 'setUp() (test setup)')

    # 2. Helper methods the test method calls, one recursion level deep
    #    (helpers calling helpers), resolved in the test class first and
    #    then one superclass hop (Closure tests keep helpers on a base
    #    class like CompilerTestCase).
    super_src = super_path = None
    m = _EXTENDS_RE.search(class_src)
    if m and checkout_dir:
        simple = m.group(1).split('.')[-1]
        if simple not in ('TestCase',):    # JUnit's own base has no helpers
            for root, _dirs, files in os.walk(checkout_dir):
                if simple + '.java' in files:
                    super_path = os.path.join(root, simple + '.java')
                    super_src = _read(super_path)
                    break

    frontier = [ft.method_source]
    for _depth in range(2):
        calls: set = set()
        for body in frontier:
            calls.update(_CALL_RE.findall(body))
        calls -= _NON_HELPER_CALLS
        calls -= seen_methods
        frontier = []
        for name in sorted(calls):
            body = None
            if re.search(r'\b' + re.escape(name) + r'\s*\(', class_src):
                body = _grab(ft.source_path, name, f'helper {name}()')
            if body is None and super_src and re.search(
                    r'\b' + re.escape(name) + r'\s*\(', super_src):
                body = _grab(super_path, name,
                             f'helper {name}() (from superclass)')
            if body:
                frontier.append(body)

    # 3. Fields / constants the method (or its helpers) reference.
    used = set(_IDENT_RE.findall(
        ft.method_source + ''.join(sections)))
    fields = _field_declarations(class_src, used)
    if super_src:
        fields += _field_declarations(super_src, used)
    if fields:
        sections.append("// --- class fields/constants the test uses ---\n"
                        + '\n'.join(dict.fromkeys(fields)))

    # 4. Fixture files named by path-like string literals.
    if checkout_dir:
        fixture_names = _FIXTURE_RE.findall(
            ft.method_source + ''.join(sections))[:3]
        included = 0
        for fx in dict.fromkeys(fixture_names):
            base = os.path.basename(fx)
            for root, _dirs, files in os.walk(checkout_dir):
                if base in files:
                    content = _read(os.path.join(root, base))
                    if content:
                        sections.append(
                            f"// --- fixture file {fx} (content) ---\n"
                            + content[:1500])
                        included += 1
                    break
            if included >= 2:
                break

    if not sections:
        return None
    out = '\n\n'.join(sections)
    if len(out) > cap:
        out = out[:cap] + '\n// ... (test support truncated at cap)'
    return out
