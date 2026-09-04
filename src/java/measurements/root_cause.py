"""The developer fix, turned into the approximated root-cause region R-hat.

FIREWALL — WHY THIS MODULE IS QUARANTINED
=========================================
This is the ONLY module in the repository that is allowed to read the
developer's fix for a bug. The pipeline (`java.run` and everything it
imports) must never see that fix: if any prompt, oracle, gate or verdict
could reach it, every number the pipeline produces would be measuring a
system that had been told the answer. So this file lives in
`java.measurements`, nothing under `src/` outside that package imports the
package (enforced by `tests/test_measurements_firewall.py`), and inside the
package only the measurement CLI imports this module.

WHAT IT COMPUTES
================
For one Defects4J bug, on the BUGGY checkout:

  methods   the methods the developer's patch changed ("seeds", ring SEED),
            plus the caller ring (depth 1) and the capped callee rings when
            an introspector project is supplied — built by
            `java.measurements.neighbourhood.build`, exactly the way the
            pipeline's own patch-derived set is built, so the two are
            comparable.
  lines     the individual source lines the patch changed, keyed by the
            top-level class of their file (ring SEED).
  body_lines
            every line of the BODY of each developer-changed method, keyed
            the same way (ring SEED). `lines` answers "did the harnesses
            execute the fix's own lines"; `body_lines` answers "how
            thoroughly is the fixed method exercised", which is the fairer
            question to ask of a fuzzer that was never shown the fix.
  manifest  the project methods that appear in the trigger tests' failure
            stack traces — "R-hat-1", where the bug MANIFESTS, which is
            usually not where it is caused.

DEFECTS4J DIRECTION CONVENTION
==============================
Defects4J ships each bug's developer fix at
`<D4J_HOME>/framework/projects/<Project>/patches/<bug_id>.src.patch`, and by
the framework's convention that patch runs FIXED -> BUGGY: applying it to
the fixed revision reproduces the bug. Its `+` side and its post-patch line
numbers therefore describe the BUGGY tree, which is the tree we measure on.
We do not take that on trust: `orient_patch` checks the `+` lines of the
first hunk with removals against the buggy sources, falls back to checking
the `-` lines, reverses the patch when the `-` side is the one that matches,
and records which direction was assumed in `RootCause.direction_assumed`.

Defects4J itself is not installed on every machine that runs these tests, so
every `defects4j` invocation sits behind a tiny function that a test can
monkeypatch (`_run`, `defects4j_home`, `export_property`, `checkout_fixed`,
`run_tree_diff`, `test_source_dirs`).
"""
from __future__ import annotations

import os
import re
import shutil
import subprocess
from dataclasses import dataclass, field
from typing import List, Optional, Sequence, Tuple

from java.measurements.locations import (
    LineRef, LineSet, MethodRef, MethodSet, SEED,
    class_top_from_source, from_javalang, from_stack_frame,
)

# Routes by which the developer fix can be obtained.
ROUTE_SRC_PATCH = 'd4j_src_patch'
ROUTE_FIXED_DIFF = 'fixed_checkout_diff'

# Direction of the patch text we ended up using.
DIR_FIXED_TO_BUGGY = 'fixed_to_buggy'            # verified: '+' side is buggy
DIR_BUGGY_TO_FIXED = 'buggy_to_fixed_reversed'   # verified: we reversed it
DIR_ASSUMED = 'fixed_to_buggy_assumed'           # neither side verified

# Where the manifest set came from.
MANIFEST_FILE = 'failing_tests'
MANIFEST_ABSENT = 'absent'

_HUNK_RE = re.compile(r'^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@(.*)$')

# Stack-frame packages that are never project code.
_NON_PROJECT_PREFIXES = (
    'java.', 'javax.', 'jdk.', 'sun.', 'sunw.', 'com.sun.', 'oracle.',
    'junit.', 'org.junit.', 'org.hamcrest.', 'org.apache.tools.ant.',
    'org.jacoco.', 'net.bytebuddy.', 'org.mockito.',
)
_TEST_CLASS_SUFFIXES = ('Test', 'Tests', 'TestCase', 'TestSuite')


# ---------------------------------------------------------------------------
# The result
# ---------------------------------------------------------------------------

@dataclass
class RootCause:
    """R-hat for one bug, on the buggy tree.

    methods   seeds (developer-changed methods, ring SEED) plus the caller
              and callee rings when an introspector project was supplied.
    lines     developer-changed lines, ring SEED.
    body_lines
              every line of the body of each seed method, ring SEED — the
              method-body counterpart of `lines`, computed with
              `patch_derived.lines_for` over the seeds alone.
    manifest  project frames from the trigger tests' stack traces, ring SEED.
    patch_text        the oriented patch (its '+' side is the buggy tree).
    route             'd4j_src_patch' or 'fixed_checkout_diff'.
    direction_assumed one of the DIR_* constants above.
    unmapped_hunks    changed lines that mapped to no method (imports,
                      fields, class-level declarations, unparseable files) —
                      recorded rather than dropped, because "the fix changed
                      nothing method-shaped" is itself a reading of a bug.
    manifest_source   'failing_tests' or 'absent'.
    notes             anything that degraded (e.g. no neighbourhood module).
    """
    methods: MethodSet = field(default_factory=MethodSet)
    lines: LineSet = field(default_factory=LineSet)
    body_lines: LineSet = field(default_factory=LineSet)
    manifest: MethodSet = field(default_factory=MethodSet)
    patch_text: str = ''
    route: str = ''
    direction_assumed: str = DIR_ASSUMED
    unmapped_hunks: List[dict] = field(default_factory=list)
    manifest_source: str = MANIFEST_ABSENT
    notes: List[str] = field(default_factory=list)

    @property
    def seeds(self) -> List[MethodRef]:
        return self.methods.refs(SEED)

    def to_dict(self) -> dict:
        return {
            'methods': self.methods.to_dict(),
            'lines': self.lines.to_dict(),
            'body_lines': self.body_lines.to_dict(),
            'manifest': self.manifest.to_dict(),
            'patch_text': self.patch_text,
            'route': self.route,
            'direction_assumed': self.direction_assumed,
            'unmapped_hunks': list(self.unmapped_hunks),
            'manifest_source': self.manifest_source,
            'notes': list(self.notes),
        }

    @classmethod
    def from_dict(cls, d: dict) -> 'RootCause':
        return cls(
            methods=MethodSet.from_dict(d.get('methods') or {}),
            lines=LineSet.from_dict(d.get('lines') or {}),
            # Written since the Rbody variant was added; a root_cause.json
            # from before that carries no such key and loads as an empty
            # set rather than failing.
            body_lines=LineSet.from_dict(d.get('body_lines') or {}),
            manifest=MethodSet.from_dict(d.get('manifest') or {}),
            patch_text=d.get('patch_text') or '',
            route=d.get('route') or '',
            direction_assumed=d.get('direction_assumed') or DIR_ASSUMED,
            unmapped_hunks=list(d.get('unmapped_hunks') or []),
            manifest_source=d.get('manifest_source') or MANIFEST_ABSENT,
            notes=list(d.get('notes') or []),
        )


# ---------------------------------------------------------------------------
# Isolated shell-outs — every `defects4j`/`diff` call is one of these
# ---------------------------------------------------------------------------

def _run(cmd: Sequence[str], cwd: Optional[str] = None,
         timeout: Optional[int] = 600) -> subprocess.CompletedProcess:
    """One subprocess call. Tests monkeypatch this (or the helpers below)
    so nothing in this module needs Defects4J to be installed."""
    return subprocess.run(list(cmd), cwd=cwd, capture_output=True,
                          text=True, timeout=timeout)


def defects4j_home(explicit: Optional[str] = None) -> Optional[str]:
    """Root of the Defects4J installation: the directory that contains
    `framework/projects/<Project>/patches/`.

    Order: the explicit argument, then `$D4J_HOME`, then the location of the
    `defects4j` executable on PATH (it lives at `<home>/framework/bin/`)."""
    if explicit:
        return explicit
    env = os.environ.get('D4J_HOME')
    if env:
        return env
    exe = shutil.which('defects4j')
    if not exe:
        return None
    exe = os.path.realpath(exe)
    bin_dir = os.path.dirname(exe)                       # <home>/framework/bin
    framework = os.path.dirname(bin_dir)                 # <home>/framework
    home = os.path.dirname(framework)                    # <home>
    return home or None


def src_patch_path(home: str, project: str, bug_id) -> str:
    """`<home>/framework/projects/<Project>/patches/<bug_id>.src.patch`."""
    return os.path.join(home, 'framework', 'projects', str(project),
                        'patches', f'{bug_id}.src.patch')


def export_property(project_dir: str, prop: str) -> Optional[str]:
    """`defects4j export -p <prop>` inside a checkout (e.g.
    `dir.src.classes` -> the project-relative main source directory).
    None when the call is unavailable or fails."""
    try:
        r = _run(['defects4j', 'export', '-p', prop], cwd=project_dir,
                 timeout=300)
    except (OSError, subprocess.SubprocessError):
        return None
    if r.returncode != 0:
        return None
    return (r.stdout or '').strip() or None


def checkout_fixed(project: str, bug_id, dest: str) -> Optional[str]:
    """`defects4j checkout -p <P> -v <id>f -w <dest>` — the developer-FIXED
    revision. Returns `dest` on success, None on any failure. (No compile:
    the fix is only ever diffed here, never run.)"""
    if os.path.isfile(os.path.join(dest, '.defects4j.config')):
        return dest
    try:
        r = _run(['defects4j', 'checkout', '-p', str(project),
                  '-v', f'{bug_id}f', '-w', dest])
    except (OSError, subprocess.SubprocessError):
        return None
    return dest if r.returncode == 0 and os.path.isdir(dest) else None


def run_tree_diff(fixed_src: str, buggy_src: str) -> str:
    """`diff -ruN <fixed_src> <buggy_src>` — a unified diff in the
    FIXED -> BUGGY direction, matching the src.patch convention. `diff`
    exits 1 when there are differences, which is the normal case."""
    try:
        r = _run(['diff', '-ruN', fixed_src, buggy_src])
    except (OSError, subprocess.SubprocessError):
        return ''
    return r.stdout or ''


def test_source_dirs(buggy_dir: str) -> List[str]:
    """Absolute test source directories of a checkout, from
    `defects4j export -p dir.src.tests`. Empty when unavailable; callers
    then fall back to the name-shape test-class heuristic alone."""
    raw = export_property(buggy_dir, 'dir.src.tests')
    out: List[str] = []
    for part in (raw or '').split(':'):
        part = part.strip()
        if not part:
            continue
        cand = part if os.path.isabs(part) else os.path.join(buggy_dir, part)
        if os.path.isdir(cand):
            out.append(os.path.normpath(cand))
    return out


# ---------------------------------------------------------------------------
# Route 1 / route 2: getting the developer patch
# ---------------------------------------------------------------------------

def developer_patch(project: str, bug_id, buggy_dir: str,
                    d4j_home: Optional[str] = None,
                    fixed_dir: Optional[str] = None) -> Tuple[str, str]:
    """The developer's fix as unified-diff text, plus the route it came by.

    Route 1 (`d4j_src_patch`): read the shipped
    `framework/projects/<P>/patches/<bug_id>.src.patch`. Cheap, exact, and
    the only thing needed on a machine with Defects4J installed.

    Route 2 (`fixed_checkout_diff`): when that file cannot be read, check out
    the fixed revision (or use `fixed_dir` if the caller already has one) and
    diff its source directory against the buggy checkout's, FIXED -> BUGGY so
    the result has the same orientation as route 1.

    Raises RuntimeError when neither route produces any text — silently
    returning an empty patch would look like "the developer changed nothing".
    """
    home = defects4j_home(d4j_home)
    if home:
        path = src_patch_path(home, project, bug_id)
        try:
            with open(path, encoding='utf-8', errors='replace') as fh:
                text = fh.read()
            if text.strip():
                return text, ROUTE_SRC_PATCH
        except OSError:
            pass

    fixed = fixed_dir
    if not fixed or not os.path.isdir(fixed):
        dest = fixed or os.path.join(os.path.dirname(os.path.abspath(buggy_dir)),
                                     f'{project}_{bug_id}_fixed_measure')
        fixed = checkout_fixed(project, bug_id, dest)
    if not fixed or not os.path.isdir(fixed):
        raise RuntimeError(
            f'no developer patch for {project}-{bug_id}: no src.patch under '
            f'{home!r} and no fixed checkout available')

    rel = _source_dir_rel(buggy_dir, fixed)
    fixed_src = os.path.join(fixed, rel) if rel else fixed
    buggy_src = os.path.join(buggy_dir, rel) if rel else buggy_dir
    raw = run_tree_diff(fixed_src, buggy_src)
    if not raw.strip():
        raise RuntimeError(
            f'no developer patch for {project}-{bug_id}: diff of {fixed_src} '
            f'against {buggy_src} is empty')
    return _relativise_diff_paths(raw, fixed_src, buggy_src, rel), ROUTE_FIXED_DIFF


def _source_dir_rel(buggy_dir: str, fixed_dir: str) -> str:
    """Project-relative main source directory (`source`, `src/main/java`,
    ...), from `defects4j export -p dir.src.classes` on either checkout.
    '' means "the whole checkout", which is a correct if slower fallback."""
    for d in (buggy_dir, fixed_dir):
        raw = export_property(d, 'dir.src.classes')
        for part in (raw or '').split(':'):
            part = part.strip().strip('/')
            if not part:
                continue
            if os.path.isdir(os.path.join(buggy_dir, part)) and \
                    os.path.isdir(os.path.join(fixed_dir, part)):
                return part
    return ''


def _relativise_diff_paths(text: str, fixed_src: str, buggy_src: str,
                           rel: str) -> str:
    """Rewrite the absolute paths `diff -ruN` puts in its `---`/`+++` lines
    into project-relative ones (`source/org/jfree/X.java`), so the hunks
    resolve against the buggy checkout exactly like a src.patch does."""
    fixed_src = fixed_src.rstrip('/')
    buggy_src = buggy_src.rstrip('/')
    out = []
    for line in text.split('\n'):
        if line.startswith('--- ') or line.startswith('+++ '):
            marker, rest = line[:4], line[4:]
            path, sep, tail = rest.partition('\t')
            for root in (fixed_src, buggy_src):
                if path == root:
                    path = rel
                    break
                if path.startswith(root + '/'):
                    suffix = path[len(root) + 1:]
                    path = f'{rel}/{suffix}' if rel else suffix
                    break
            out.append(marker + path + sep + tail)
        else:
            out.append(line)
    return '\n'.join(out)


# ---------------------------------------------------------------------------
# Direction detection
# ---------------------------------------------------------------------------

@dataclass
class _Hunk:
    old_path: str
    new_path: str
    old_start: int
    new_start: int
    body: List[str]

    @property
    def added(self) -> List[Tuple[int, str]]:
        """`(new-file line number, text)` for every '+' line."""
        out, n = [], self.new_start
        for raw in self.body:
            if raw.startswith('\\'):
                continue
            if raw.startswith('+'):
                out.append((n, raw[1:]))
                n += 1
            elif raw.startswith('-'):
                continue
            else:
                n += 1
        return out

    def _context(self, start: int, skip: str) -> List[Tuple[int, str]]:
        out, n = [], start
        for raw in self.body:
            if raw.startswith('\\'):
                continue
            if raw.startswith(skip):
                continue
            if raw.startswith(('+', '-')):
                n += 1
                continue
            out.append((n, raw[1:] if raw.startswith(' ') else raw))
            n += 1
        return out

    @property
    def context_new(self) -> List[Tuple[int, str]]:
        """`(new-file line number, text)` for every unchanged context line.
        Present in every hunk, so orientation can be verified even for a
        pure deletion (whose '+' side is empty)."""
        return self._context(self.new_start, '-')

    @property
    def context_old(self) -> List[Tuple[int, str]]:
        """`(old-file line number, text)` for every unchanged context line."""
        return self._context(self.old_start, '+')

    @property
    def removed(self) -> List[Tuple[int, str]]:
        """`(old-file line number, text)` for every '-' line."""
        out, n = [], self.old_start
        for raw in self.body:
            if raw.startswith('\\'):
                continue
            if raw.startswith('-'):
                out.append((n, raw[1:]))
                n += 1
            elif raw.startswith('+'):
                continue
            else:
                n += 1
        return out


def parse_hunks(patch_text: str) -> List[_Hunk]:
    """Every hunk of a unified diff, with both file paths and both start
    line numbers. Deliberately small and local: direction detection needs
    the PRE side too, which the pipeline's own parser discards."""
    hunks: List[_Hunk] = []
    old_path = new_path = ''
    cur: Optional[_Hunk] = None
    for line in (patch_text or '').split('\n'):
        if line.startswith('--- '):
            old_path = _strip_prefix(line[4:])
            cur = None
            continue
        if line.startswith('+++ '):
            new_path = _strip_prefix(line[4:])
            cur = None
            continue
        m = _HUNK_RE.match(line)
        if m:
            cur = _Hunk(old_path=old_path, new_path=new_path,
                        old_start=int(m.group(1)), new_start=int(m.group(3)),
                        body=[])
            hunks.append(cur)
            continue
        if cur is None:
            continue
        if line.startswith(('+', '-', ' ', '\\')) or line == '':
            cur.body.append(line)
        else:
            cur = None      # junk between hunks ends the current one
    # A patch text ends with a newline, so the split leaves a final '' that
    # is NOT a context line; counting it would put a phantom line one past
    # the hunk and break every orientation check.
    for h in hunks:
        while h.body and h.body[-1] == '':
            h.body.pop()
    return hunks


def _strip_prefix(raw: str) -> str:
    """`a/source/org/X.java\t2019-01-01` -> `source/org/X.java`."""
    path = raw.split('\t')[0].strip()
    if path == '/dev/null':
        return ''
    if path.startswith(('a/', 'b/')):
        path = path[2:]
    return path.lstrip('/')


def orient_patch(patch_text: str, buggy_dir: str) -> Tuple[str, str]:
    """Return `(patch whose '+' side is the BUGGY tree, direction assumed)`.

    Defects4J's convention is fixed -> buggy, so normally the patch is used
    as it stands. We verify it anyway on the first hunk that removes
    anything: if that hunk's '+' lines are present in the buggy sources at
    the positions the hunk claims, the patch is fixed -> buggy; if instead
    its '-' lines are the ones present, the patch runs buggy -> fixed and we
    reverse it. When neither side can be checked (file missing, no hunk with
    content) we keep the patch as-is and say so with DIR_ASSUMED, so a later
    reader can tell a verified orientation from a presumed one.
    """
    hunks = parse_hunks(patch_text)
    probe = next((h for h in hunks if h.removed and h.added), None)
    if probe is None:
        probe = next((h for h in hunks if h.added or h.removed), None)
    if probe is None:
        return patch_text, DIR_ASSUMED

    # The unchanged context lines are numbered differently on the two
    # sides whenever the hunk adds or removes lines, so together with the
    # changed lines they pin the orientation — also for a pure deletion,
    # whose '+' side alone would be empty and unverifiable.
    post_ok = _side_matches(buggy_dir, probe.new_path,
                            probe.added + probe.context_new)
    pre_ok = _side_matches(buggy_dir, probe.old_path,
                           probe.removed + probe.context_old)
    if post_ok and not pre_ok:
        return patch_text, DIR_FIXED_TO_BUGGY
    if pre_ok and not post_ok:
        return reverse_patch(patch_text), DIR_BUGGY_TO_FIXED
    return patch_text, DIR_ASSUMED


def _side_matches(root: str, rel_path: str,
                  numbered: List[Tuple[int, str]]) -> bool:
    """True when every `(line number, text)` pair is exactly what the file
    at `root/rel_path` holds. An empty list is never a match — "nothing to
    check" must not read as "verified"."""
    if not rel_path or not numbered:
        return False
    # Defects4J's src.patch files carry git-style 'a/'/'b/' prefixes and
    # drr patches a leading '/', neither of which exists in the checkout.
    candidates = [rel_path]
    if rel_path[:2] in ('a/', 'b/'):
        candidates.append(rel_path[2:])
    candidates.append(rel_path.lstrip('/'))
    file_lines = None
    for cand in candidates:
        try:
            with open(os.path.join(root, cand), encoding='utf-8',
                      errors='replace') as fh:
                file_lines = fh.read().split('\n')
            break
        except OSError:
            continue
    if file_lines is None:
        return False
    for number, text in numbered:
        if number <= 0 or number > len(file_lines):
            return False
        if file_lines[number - 1].rstrip() != text.rstrip():
            return False
    return True


def reverse_patch(patch_text: str) -> str:
    """Swap a unified diff's two sides: file headers, hunk ranges and the
    '+'/'-' prefix of every body line. The `a/`/`b/` prefixes travel with
    the side they belong to, so the reversed text's `+++` line still names
    the post-patch file the way every diff reader expects."""
    lines = (patch_text or '').split('\n')
    out: List[str] = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith('--- ') and i + 1 < len(lines) \
                and lines[i + 1].startswith('+++ '):
            old_side, new_side = line[4:], lines[i + 1][4:]
            out.append('--- ' + _retag(new_side, 'a'))
            out.append('+++ ' + _retag(old_side, 'b'))
            i += 2
            continue
        m = _HUNK_RE.match(line)
        if m:
            old = _range(m.group(1), m.group(2))
            new = _range(m.group(3), m.group(4))
            out.append(f'@@ -{new} +{old} @@{m.group(5)}')
            i += 1
            continue
        if line.startswith('--- ') or line.startswith('+++ '):
            out.append(line)
        elif line.startswith('+'):
            out.append('-' + line[1:])
        elif line.startswith('-'):
            out.append('+' + line[1:])
        else:
            out.append(line)
        i += 1
    return '\n'.join(out)


def _range(start: str, length: Optional[str]) -> str:
    return f'{start},{length}' if length is not None else start


def _retag(raw: str, want: str) -> str:
    """Put the `a/` or `b/` prefix that matches the side this path is now
    on, leaving prefix-less paths (and `/dev/null`) alone."""
    path, sep, tail = raw.partition('\t')
    stripped = path.strip()
    if stripped.startswith(('a/', 'b/')):
        path = want + '/' + stripped[2:]
    return path + sep + tail


# ---------------------------------------------------------------------------
# Patch -> methods and lines
# ---------------------------------------------------------------------------

def changed_methods_as_refs(patch_text: str,
                            buggy_dir: str) -> Tuple[List[MethodRef], List[dict]]:
    """The methods the developer changed, as `MethodRef`s, plus the changed
    lines that mapped to no method.

    Reuses `execution.diffcov.changed_methods`, which is the pipeline's own
    diff -> enclosing-method mapper, so the developer set and the pipeline's
    patch-derived set are built by identical machinery. Constructors come out
    of javalang named after their class; `from_javalang` renames them to
    `<init>`, the form every other source in this package uses.
    """
    from java.execution import diffcov

    plan = diffcov.changed_methods(patch_text or '', buggy_dir)
    refs: List[MethodRef] = []
    for m in plan.methods:
        ref = from_javalang(m.class_name, m.method_name, list(m.param_types))
        if ref not in refs:
            refs.append(ref)
    return refs, list(plan.unmapped)


def changed_lines(patch_text: str, buggy_dir: str) -> LineSet:
    """The individual lines the developer changed, on the buggy tree.

    Line numbers are post-side, i.e. buggy-tree numbers once the patch has
    been oriented. Each file's lines are keyed by the top-level class of the
    file (nested classes share a file, and so share a key)."""
    from java.execution import diffcov

    out = LineSet()
    by_file = diffcov.changed_lines_by_file(patch_text or '')
    # A deletion-only change has no line of its own on the buggy tree; the
    # pipeline's mapper records the line BEFORE the gap. A crash caused by
    # the missing code shows up on the line AFTER it just as often, so both
    # boundary lines are part of the developer's region.
    for h in parse_hunks(patch_text or ''):
        n, pending, has_add = h.new_start, False, False
        for raw in h.body:
            if raw.startswith('\\'):
                continue
            if raw.startswith('-'):
                pending = True
            elif raw.startswith('+'):
                has_add = True
                n += 1
            else:                       # a context line ends the group
                if pending and not has_add:
                    by_file.setdefault(h.new_path, []).extend([max(n - 1, 1), n])
                pending = has_add = False
                n += 1
        if pending and not has_add:
            by_file.setdefault(h.new_path, []).append(max(n - 1, 1))
    for rel_path, numbers in sorted(by_file.items()):
        if not rel_path.endswith('.java'):
            continue
        try:
            with open(os.path.join(buggy_dir, rel_path), encoding='utf-8',
                      errors='replace') as fh:
                source = fh.read()
        except OSError:
            source = ''
        class_top = class_top_from_source(rel_path, source)
        for n in sorted(numbers):
            out.add(LineRef(class_top, n), SEED)
    return out


# ---------------------------------------------------------------------------
# Where the bug manifests: the trigger tests' stack traces
# ---------------------------------------------------------------------------

def trigger_frames(buggy_dir: str) -> MethodSet:
    """Project methods named in the trigger tests' failure stack traces.

    Defects4J writes `<checkout>/failing_tests` whenever `defects4j test`
    runs. Each record starts with `--- <test class>::<test method>`, then the
    throwable line, then `at pkg.Cls.method(File.java:N)` frames. We keep
    only frames in project code: no JDK/JUnit packages, and no test classes
    (name ends in Test/Tests/TestCase/TestSuite, or the file lives under a
    test source directory, or it is the record's own test class).

    An empty set is returned when the file does not exist — see
    `manifest_source` on `RootCause`, which distinguishes "no file" from
    "file with no project frames".
    """
    path = os.path.join(buggy_dir, MANIFEST_FILE)
    try:
        with open(path, encoding='utf-8', errors='replace') as fh:
            text = fh.read()
    except OSError:
        return MethodSet()

    tdirs = test_source_dirs(buggy_dir)
    out = MethodSet()
    record_test_class = ''
    for line in text.split('\n'):
        stripped = line.strip()
        if stripped.startswith('--- '):
            record_test_class = stripped[4:].split('::')[0].strip()
            continue
        parsed = from_stack_frame(line)
        if parsed is None:
            continue
        ref, _lineno = parsed
        if _is_project_frame(ref, record_test_class, buggy_dir, tdirs):
            out.add(MethodRef(ref.class_fq, ref.name), SEED)
        elif ref.class_fq and not ref.class_fq.startswith(_NON_PROJECT_PREFIXES):
            # A test class we deliberately dropped: keep the name so the
            # count of discarded frames is auditable rather than invisible.
            if str(ref) not in out.unmatched:
                out.unmatched.append(str(ref))
    return out


def _is_project_frame(ref: MethodRef, record_test_class: str,
                      buggy_dir: str, test_dirs: Sequence[str]) -> bool:
    cls = ref.class_fq
    if not cls or cls.startswith(_NON_PROJECT_PREFIXES):
        return False
    if record_test_class and cls.split('$')[0] == record_test_class:
        return False
    simple = cls.rsplit('.', 1)[-1].split('$')[0]
    if simple.endswith(_TEST_CLASS_SUFFIXES):
        return False
    if _in_test_dir(cls, buggy_dir, test_dirs):
        return False
    return True


def _in_test_dir(class_fq: str, buggy_dir: str,
                 test_dirs: Sequence[str]) -> bool:
    """True when the top-level class's .java file sits under one of the
    checkout's test source directories."""
    if not test_dirs:
        return False
    top = class_fq.split('$')[0]
    parts = top.split('.')
    # locations.MethodRef joins nested types with '.', so trim trailing
    # segments until one names a file that exists.
    for cut in range(len(parts), 0, -1):
        rel = os.path.join(*parts[:cut]) + '.java'
        for d in test_dirs:
            if os.path.isfile(os.path.join(d, rel)):
                return True
    return False


def _manifest_source(buggy_dir: str) -> str:
    return (MANIFEST_FILE
            if os.path.isfile(os.path.join(buggy_dir, MANIFEST_FILE))
            else MANIFEST_ABSENT)


# ---------------------------------------------------------------------------
# Everything together
# ---------------------------------------------------------------------------

def compute(project: str, bug_id, buggy_dir: str, *,
            d4j_home: Optional[str] = None,
            fixed_dir: Optional[str] = None,
            introspector_project=None,
            caller_cap: Optional[int] = None,
            callee_cap: Optional[int] = None,
            callee_depth: Optional[int] = None,
            source_root: Optional[str] = None) -> RootCause:
    """R-hat for one bug: patch -> oriented patch -> seeds + rings, lines,
    and the manifest set from the trigger tests' stack traces.

    `introspector_project` is a fuzz-introspector project for the buggy
    tree. Without it the method set is the seeds alone (no rings), which is
    still a valid — just smaller — region.

    `source_root` is a directory of `.java` sources for that same tree
    (normally `buggy_dir` itself). Two things read it: the caller ring, for
    a seed the call graph found no caller for (the JVM frontend does not
    resolve virtual or interface calls, so those seeds otherwise get an
    empty caller ring — see `neighbourhood.SourceScan`), and `body_lines`,
    which needs the seed methods' declarations to know where their bodies
    start and end. It defaults to `buggy_dir` for the second.
    """
    patch_text, route = developer_patch(project, bug_id, buggy_dir,
                                        d4j_home=d4j_home,
                                        fixed_dir=fixed_dir)
    oriented, direction = orient_patch(patch_text, buggy_dir)
    seeds, unmapped = changed_methods_as_refs(oriented, buggy_dir)
    lines = changed_lines(oriented, buggy_dir)

    notes: List[str] = []
    methods = _rings(seeds, introspector_project, caller_cap, callee_cap,
                     callee_depth, notes, source_root)
    body_lines = _body_lines(seeds, source_root or buggy_dir, notes)

    return RootCause(
        methods=methods,
        lines=lines,
        body_lines=body_lines,
        manifest=trigger_frames(buggy_dir),
        patch_text=oriented,
        route=route,
        direction_assumed=direction,
        unmapped_hunks=unmapped,
        manifest_source=_manifest_source(buggy_dir),
        notes=notes,
    )


def _rings(seeds: List[MethodRef], introspector_project,
           caller_cap: Optional[int], callee_cap: Optional[int],
           callee_depth: Optional[int], notes: List[str],
           source_root: Optional[str] = None) -> MethodSet:
    """Grow the caller/callee rings around the seeds via
    `java.measurements.neighbourhood`. Imported lazily so this module can be
    read (and the seeds computed) even where that module is absent; the
    fallback is a seeds-only set with the degradation recorded in `notes`."""
    try:
        from java.measurements import neighbourhood
    except ImportError:
        notes.append('neighbourhood module unavailable: seeds only, no rings')
        return _seed_set(seeds)
    try:
        return neighbourhood.build(seeds, introspector_project,
                                   caller_cap=caller_cap,
                                   callee_cap=callee_cap,
                                   callee_depth=callee_depth,
                                   source_root=source_root)
    except Exception as exc:                       # noqa: BLE001 - fail soft
        notes.append(f'neighbourhood.build failed ({exc.__class__.__name__}: '
                     f'{exc}): seeds only, no rings')
        return _seed_set(seeds)


def _body_lines(seeds: List[MethodRef], source_root: Optional[str],
                notes: List[str]) -> LineSet:
    """Every line of the BODY of each developer-changed method.

    The seeds alone are handed to `patch_derived.lines_for`, which finds
    each method's declaration in the buggy sources and returns every line
    from its signature to its closing brace. So this is the same machinery
    the pipeline's own patch-derived line set is built with, applied to the
    developer's methods instead of the repair tool's — the two stay
    comparable.

    The rings are deliberately NOT included: a caller's or callee's body is
    not part of the fix, and folding those in would make the region a
    different thing from the one `RootCause.lines` describes.

    Imported lazily, and every failure is soft: a missing module, an
    unreadable source root or a parse failure costs the body lines and is
    written into `notes`, rather than costing the whole of R-hat."""
    if not source_root:
        notes.append('no source root: body_lines empty')
        return LineSet()
    try:
        from java.measurements import patch_derived
    except ImportError:
        notes.append('patch_derived module unavailable: body_lines empty')
        return LineSet()
    try:
        return patch_derived.lines_for(_seed_set(seeds), source_root)
    except Exception as exc:                       # noqa: BLE001 - fail soft
        notes.append(f'patch_derived.lines_for failed '
                     f'({exc.__class__.__name__}: {exc}): body_lines empty')
        return LineSet()


def _seed_set(seeds: List[MethodRef]) -> MethodSet:
    ms = MethodSet()
    for ref in seeds:
        ms.add(ref, SEED, 0)
    return ms
