"""Diff-hit instrumentation — does a generated input REACH the code the
patch changed?

Station: patched-build materialisation (`PatchedProjectBuilder.build_patched_dir`
in fuzz_runner.py), between "patch applied" and `defects4j compile`.

Failure mode it measures: a harness compiles, its oracles are sound, it runs
for the full fuzz budget on the patched build — and stays quiet because no
generated input ever entered the changed method at all. From the outside that
is indistinguishable from "the patch fixed it". Counting entries into each
patch-changed method separates the two.

Three mechanical steps, none of them bug-specific:

  1. `changed_methods` maps the patch's POST-patch line numbers to the
     enclosing method/constructor declarations (javalang, same AST route as
     bug_context/analysis.py). Hunks that only touch fields, imports or
     class-level declarations map to no method and are recorded as unmapped
     rather than silently dropped.
  2. `instrument_source` inserts one `vulnpatch.DiffCov.hit("<id>");` call at
     the entry of each such method, in the patched WORKING COPY only.
  3. `helper_source` generates `vulnpatch/DiffCov.java`, which owns the
     counters and emits `[diffcov] method=<id> hits=<N>` — one line per
     changed method, zeros included (a zero is the whole point).

MEASUREMENT ONLY. Nothing here feeds a prompt, the verifier, a gate or a
verdict; see the collection site in fuzz_runner.py.
"""
import os
import re
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple

import javalang

from java.parsing.java_source import skip_literal

# Where the instrumented JVM writes its periodic counter dump. Read by
# `run_jazzer`; set as an environment variable on the Jazzer subprocess so
# nothing about the java command line changes when the flag is off.
OUT_ENV_VAR = 'VULNPATCH_DIFFCOV_OUT'

# Generated helper class, and the package directory it lives in.
HELPER_PACKAGE = 'vulnpatch'
HELPER_CLASS = 'DiffCov'
HELPER_FQN = f'{HELPER_PACKAGE}.{HELPER_CLASS}'

_DIFFCOV_LINE_RE = re.compile(r'^\[diffcov\] method=(\S+) hits=(\d+)\s*$',
                              re.MULTILINE)
_HUNK_HEADER_RE = re.compile(r'^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@')


# --- 1. diff -> (class, method) ------------------------------------------

@dataclass
class ChangedMethod:
    """One method or constructor whose BODY the patch touches."""
    rel_path: str          # project-relative path of the post-patch file
    class_name: str        # fully qualified, nested types joined with '.'
    method_name: str       # simple name; a constructor uses its class's
    param_types: Tuple[str, ...]
    decl_line: int         # 1-indexed line of the declaration
    insert_offset: int     # char offset in the post-patch file text

    @property
    def method_id(self) -> str:
        return (f'{self.class_name}#{self.method_name}'
                f'({",".join(self.param_types)})')


@dataclass
class DiffCovPlan:
    methods: List[ChangedMethod] = field(default_factory=list)
    # Changed lines that mapped to no instrumentable method — field/import
    # only hunks, abstract declarations, unparseable files. Recorded, not
    # dropped: "the patch changed nothing we can count" is itself a reading
    # of a run whose diffcov output is empty.
    unmapped: List[dict] = field(default_factory=list)

    @property
    def method_ids(self) -> List[str]:
        return [m.method_id for m in self.methods]

    def as_dict(self) -> dict:
        return {
            'methods': [
                {'method_id': m.method_id, 'file': m.rel_path,
                 'line': m.decl_line}
                for m in self.methods
            ],
            'unmapped': list(self.unmapped),
        }


def changed_lines_by_file(patch_text: str) -> Dict[str, List[int]]:
    """`{post-patch relative path: [changed line numbers]}`.

    Line numbers are in the NEW (post-patch) file, because that is the tree
    we instrument. Added lines are recorded at their new-file position; a
    hunk that only DELETES records the new-file line the deletion collapsed
    onto, so a pure-deletion patch still maps to its enclosing method.
    """
    # Local import: fuzz_runner owns the counted unified-diff parser (it is
    # the safety net that rejects truncated patches). Importing it lazily
    # keeps the module graph one-way — fuzz_runner imports diffcov, never
    # the reverse at module scope.
    from java.execution.fuzz_runner import _file_sections

    out: Dict[str, List[int]] = {}
    for header, hunks in _file_sections(patch_text):
        rel_path = _new_path(header)
        if not rel_path:
            continue
        lines_for_file = out.setdefault(rel_path, [])
        for _old_start, body in hunks:
            m = _HUNK_HEADER_RE.match(body[0])
            if not m:
                continue
            new_ln = int(m.group(3))
            recorded = False
            for raw in body[1:]:
                if raw.startswith('\\'):
                    continue
                if raw.startswith('+'):
                    lines_for_file.append(new_ln)
                    recorded = True
                    new_ln += 1
                elif raw.startswith('-'):
                    if not recorded:
                        lines_for_file.append(max(new_ln - 1, 1))
                        recorded = True
                else:  # context line (' ' or an empty line)
                    new_ln += 1
    return {p: list(dict.fromkeys(lns)) for p, lns in out.items() if lns}


def _new_path(header_lines: List[str]) -> Optional[str]:
    """Project-relative path of the POST-patch file from a diff header.

    drr patches carry `/src/...` prefixes with no a/ b/ (see
    PatchedProjectBuilder), so both forms are stripped. A `+++ /dev/null`
    (file deleted) has nothing to instrument.
    """
    for line in header_lines:
        m = re.match(r'^\+\+\+\s+(\S+)', line)
        if not m:
            continue
        path = m.group(1)
        if path == '/dev/null':
            return None
        if path.startswith('b/'):
            path = path[2:]
        return path.lstrip('/')
    # No +++ line: fall back to the --- side, which names the same file for
    # every modification hunk.
    for line in header_lines:
        m = re.match(r'^---\s+(\S+)', line)
        if m and m.group(1) != '/dev/null':
            path = m.group(1)
            if path.startswith('a/'):
                path = path[2:]
            return path.lstrip('/')
    return None


def changed_methods(patch_text: str, root_dir: str) -> DiffCovPlan:
    """Map every changed line to the smallest method/constructor whose body
    contains it, reading the POST-patch sources under `root_dir`."""
    plan = DiffCovPlan()
    seen = set()
    for rel_path, line_numbers in sorted(changed_lines_by_file(patch_text).items()):
        if not rel_path.endswith('.java'):
            plan.unmapped.append({'file': rel_path, 'line': None,
                                  'reason': 'not a java source file'})
            continue
        full = os.path.join(root_dir, rel_path)
        try:
            with open(full, encoding='utf-8', errors='replace') as fh:
                source = fh.read()
        except OSError:
            plan.unmapped.append({'file': rel_path, 'line': None,
                                  'reason': 'post-patch file not readable'})
            continue
        try:
            declarations = method_declarations(source)
        except _ParseFailure as exc:
            plan.unmapped.append({'file': rel_path, 'line': None,
                                  'reason': f'javalang: {exc}'})
            continue
        offsets = _line_offsets(source)
        for line_no in line_numbers:
            decl = _smallest_containing(declarations, offsets, line_no)
            if decl is None:
                plan.unmapped.append({
                    'file': rel_path, 'line': line_no,
                    'reason': 'no enclosing method (field/import/class level)'})
                continue
            if decl['insert_offset'] is None:
                plan.unmapped.append({
                    'file': rel_path, 'line': line_no,
                    'reason': f"{decl['name']} has no body (abstract/native)"})
                continue
            key = (rel_path, decl['start'])
            if key in seen:
                continue
            seen.add(key)
            plan.methods.append(ChangedMethod(
                rel_path=rel_path,
                class_name=decl['class_name'],
                method_name=decl['name'],
                param_types=tuple(decl['param_types']),
                decl_line=decl['decl_line'],
                insert_offset=decl['insert_offset'],
            ))
    plan.methods.sort(key=lambda m: (m.rel_path, m.decl_line))
    return plan


class _ParseFailure(RuntimeError):
    """javalang could not read the compilation unit."""


def method_declarations(source: str) -> List[dict]:
    """Every method/constructor in one compilation unit, with the character
    range it spans and the offset a counter call may be inserted at.

    Ranges are character offsets rather than line numbers so an inner
    (anonymous- or nested-class) declaration is strictly contained in its
    outer one and `_smallest_containing` picks the deepest — the same
    granularity argument as analysis.py's brace-matching collector.
    """
    try:
        tree = javalang.parse.parse(source)
    except (javalang.parser.JavaSyntaxError,
            javalang.tokenizer.LexerError,
            IndexError, TypeError) as exc:
        raise _ParseFailure(str(exc)) from None

    package = tree.package.name if tree.package else ''
    offsets = _line_offsets(source)
    out: List[dict] = []
    for path, node in tree:
        if not isinstance(node, (javalang.tree.MethodDeclaration,
                                 javalang.tree.ConstructorDeclaration)):
            continue
        if not node.position:
            continue
        decl_off = _offset_of(offsets, node.position.line,
                              node.position.column)
        if decl_off is None:
            continue
        open_idx, end_idx = _body_span(source, decl_off, node.name)
        if end_idx < 0:
            continue
        type_chain = _enclosing_types(path)
        class_name = '.'.join(([package] if package else []) + type_chain)
        out.append({
            'name': node.name,
            'class_name': class_name or node.name,
            'param_types': [_type_name(p) for p in (node.parameters or [])],
            'decl_line': node.position.line,
            'start': decl_off,
            'end': end_idx,
            'insert_offset': (None if open_idx < 0
                              else _entry_offset(source, open_idx)),
        })
    return out


def _enclosing_types(path) -> List[str]:
    """Simple names of the type declarations enclosing a node, outermost
    first. Anonymous classes contribute no name (they have none)."""
    names = []
    for item in path:
        if isinstance(item, (list, tuple)):
            continue
        if isinstance(item, (javalang.tree.ClassDeclaration,
                             javalang.tree.InterfaceDeclaration,
                             javalang.tree.EnumDeclaration,
                             javalang.tree.AnnotationDeclaration)):
            names.append(item.name)
    return names


def _type_name(param) -> str:
    """Erased, simple-name rendering of a formal parameter's type —
    `Object`, `int`, `double[]`, `Map`. Enough to tell overloads apart,
    stable enough to survive as a string key."""
    t = param.type
    chain = [getattr(t, 'name', None) or 'Object']
    dims = len(getattr(t, 'dimensions', None) or [])
    sub = getattr(t, 'sub_type', None)
    while sub is not None:
        chain.append(getattr(sub, 'name', ''))
        dims = len(getattr(sub, 'dimensions', None) or []) or dims
        sub = getattr(sub, 'sub_type', None)
    name = '.'.join(p for p in chain if p) + '[]' * dims
    if getattr(param, 'varargs', False) or 'varargs' in (param.modifiers or set()):
        name += '...'
    return name


def _line_offsets(source: str) -> List[int]:
    """Character offset of the start of each 1-indexed line (index 0 is a
    filler so `offsets[n]` is line n)."""
    offsets = [0, 0]
    for line in source.split('\n')[:-1]:
        offsets.append(offsets[-1] + len(line) + 1)
    return offsets


def _offset_of(offsets: List[int], line: int, column: int) -> Optional[int]:
    if line <= 0 or line >= len(offsets):
        return None
    return offsets[line] + max(column - 1, 0)


def _skip_trivia(src: str, i: int) -> int:
    """Advance past whitespace and comments."""
    n = len(src)
    while i < n:
        c = src[i]
        if c.isspace():
            i += 1
        elif src.startswith('//', i):
            j = src.find('\n', i)
            i = n if j < 0 else j + 1
        elif src.startswith('/*', i):
            j = src.find('*/', i + 2)
            i = n if j < 0 else j + 2
        else:
            return i
    return i


def _body_span(src: str, decl_off: int, name: str) -> Tuple[int, int]:
    """`(body_open_brace_index, declaration_end_index)` for the declaration
    starting at `decl_off`.

    The parameter list is located from the declaration's NAME rather than
    from `decl_off` directly, so an annotation with arguments or a generic
    return type (`Map<String,List<Integer>> f()`) can't be mistaken for it.
    An abstract/interface/native declaration ends at its `;` and reports
    `-1` for the open brace.
    """
    m = re.compile(r'\b' + re.escape(name) + r'\s*\(').search(src, decl_off)
    if not m:
        return -1, -1
    close = _match_paren(src, m.end() - 1)
    if close < 0:
        return -1, -1
    i = _skip_trivia(src, close + 1)
    # Skip a `throws A, B` clause and any array-return `[]` suffix.
    while i < len(src) and src[i] not in '{;':
        i += 1
        i = _skip_trivia(src, i)
    if i >= len(src):
        return -1, -1
    if src[i] == ';':
        return -1, i
    end = _match_brace(src, i)
    return (i, end) if end >= 0 else (-1, -1)


def _entry_offset(src: str, open_idx: int) -> int:
    """Offset at which a statement may be inserted as the method's FIRST
    executable statement.

    Normally just past the body's `{`. A constructor whose body begins with
    an explicit `this(...)`/`super(...)` invocation is the exception: Java
    requires that call to be the first statement, so the counter goes after
    its terminating `;`.
    """
    i = _skip_trivia(src, open_idx + 1)
    m = re.compile(r'(this|super)\s*\(').match(src, i)
    if not m:
        return open_idx + 1
    close = _match_paren(src, m.end() - 1)
    if close < 0:
        return open_idx + 1
    j = _skip_trivia(src, close + 1)
    return j + 1 if j < len(src) and src[j] == ';' else open_idx + 1


def _match_paren(src: str, open_idx: int) -> int:
    return _match_delim(src, open_idx, '(', ')')


def _match_brace(src: str, open_idx: int) -> int:
    return _match_delim(src, open_idx, '{', '}')


def _match_delim(src: str, open_idx: int, opener: str, closer: str) -> int:
    """Comment-aware delimiter matcher. `java_source.match_brace` skips
    string/char literals but not comments, and a commented-out brace inside
    a method body is common enough in these projects to matter here."""
    if open_idx < 0 or open_idx >= len(src) or src[open_idx] != opener:
        return -1
    depth, i, n = 0, open_idx, len(src)
    while i < n:
        c = src[i]
        # Comments before literals: an apostrophe in a `// don't` comment
        # otherwise starts a char-literal scan that swallows real braces.
        if src.startswith('//', i) or src.startswith('/*', i):
            i = _skip_trivia(src, i)
            continue
        if c in '"\'':
            i = skip_literal(src, i)
            continue
        if c == opener:
            depth += 1
        elif c == closer:
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def _smallest_containing(declarations: List[dict], offsets: List[int],
                         line_no: int) -> Optional[dict]:
    """The deepest declaration whose character range contains `line_no`."""
    if line_no <= 0 or line_no >= len(offsets):
        return None
    start_off = offsets[line_no]
    end_off = (offsets[line_no + 1] - 1 if line_no + 1 < len(offsets)
               else start_off)
    candidates = [d for d in declarations
                  if d['start'] <= end_off and start_off <= d['end']]
    if not candidates:
        return None
    return min(candidates, key=lambda d: d['end'] - d['start'])


# --- 2. source instrumentation -------------------------------------------

def instrument_source(source: str,
                      targets: List[Tuple[int, str]]) -> str:
    """Insert `vulnpatch.DiffCov.hit("<id>");` at each `(offset, method_id)`.

    Inserted inline, with no newline: every line in the file keeps its
    number, so stack traces, the trigger-test net and any later reading of
    the patched tree still line up with the diff.
    """
    out = source
    for offset, method_id in sorted(targets, reverse=True):
        call = f' {HELPER_FQN}.hit("{method_id}");'
        out = out[:offset] + call + out[offset:]
    return out


def helper_source(method_ids: List[str], flush_seconds: float = 2.0) -> str:
    """Source of the generated `vulnpatch/DiffCov.java`.

    Written in a deliberately old Java dialect — raw collections, no
    for-each, no autoboxing, no annotations, no diamond. Defects4J projects
    compile with their own historical `-source` level (1.3/1.4 for several
    of them), under which generics are a syntax error; the JDK that runs
    the build is modern, so the Java 8 CLASSES still link. See
    docs/diffcov-instrumentation-2026-08-09.md.
    """
    unique = list(dict.fromkeys(method_ids))
    ids = ',\n'.join(f'        "{mid}"' for mid in unique)
    return f'''package {HELPER_PACKAGE};

/* GENERATED by the vuln-patch pipeline (src/java/execution/diffcov.py) for
   ONE patched build. Counts entries into the methods that build's patch
   changed. Measurement only: never read by a prompt, oracle, verifier,
   gate or verdict.
   ASCII ONLY, and in a deliberately old Java dialect (raw collections, no
   for-each, no autoboxing, no annotations) - `defects4j compile` runs the
   project's own historical -source level with the platform charset. */
public final class {HELPER_CLASS} {{

    private static final long FLUSH_MILLIS = {max(int(flush_seconds * 1000), 250)}L;

    private static final String[] METHODS = {{
{ids}
    }};

    private static final java.util.concurrent.ConcurrentHashMap COUNTS =
        new java.util.concurrent.ConcurrentHashMap();

    private static String outPath = null;

    static {{
        /* Every step is optional: a static initializer that throws would
           turn each instrumented class into a NoClassDefFoundError and
           change what the fuzz run measures. */
        try {{
            int i = 0;
            while (i < METHODS.length) {{
                COUNTS.put(METHODS[i],
                           new java.util.concurrent.atomic.LongAdder());
                i++;
            }}
            outPath = System.getenv("{OUT_ENV_VAR}");
            Runtime.getRuntime().addShutdownHook(new Thread() {{
                public void run() {{
                    flushFile();
                    report();
                }}
            }});
            if (outPath != null) {{
                Thread flusher = new Thread() {{
                    public void run() {{
                        for (;;) {{
                            try {{
                                Thread.sleep(FLUSH_MILLIS);
                            }} catch (Throwable stop) {{
                                return;
                            }}
                            flushFile();
                        }}
                    }}
                }};
                flusher.setDaemon(true);
                flusher.start();
            }}
        }} catch (Throwable ignored) {{
        }}
    }}

    private {HELPER_CLASS}() {{
    }}

    /* Hot path: one map lookup and one counter increment. No allocation, no
       I/O, no throw. Every id is pre-registered above, so a miss is a no-op
       rather than an insert. */
    public static void hit(String id) {{
        try {{
            Object counter = COUNTS.get(id);
            if (counter != null) {{
                ((java.util.concurrent.atomic.LongAdder) counter).increment();
            }}
        }} catch (Throwable ignored) {{
        }}
    }}

    private static String render() {{
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (i < METHODS.length) {{
            Object counter = COUNTS.get(METHODS[i]);
            long n = 0L;
            if (counter != null) {{
                n = ((java.util.concurrent.atomic.LongAdder) counter).sum();
            }}
            sb.append("[diffcov] method=").append(METHODS[i])
              .append(" hits=").append(n).append('\\n');
            i++;
        }}
        return sb.toString();
    }}

    private static void report() {{
        try {{
            System.err.print(render());
            System.err.flush();
        }} catch (Throwable ignored) {{
        }}
    }}

    /* The file, not stderr, is the channel the collector trusts: the fuzz
       runner SIGKILLs the JVM on its subprocess timeout and libFuzzer ends a
       finding run from native code, and neither path runs shutdown hooks. */
    private static synchronized void flushFile() {{
        if (outPath == null) {{
            return;
        }}
        try {{
            java.io.File tmp = new java.io.File(outPath + ".tmp");
            java.io.FileOutputStream out = new java.io.FileOutputStream(tmp);
            try {{
                out.write(render().getBytes("UTF-8"));
                out.flush();
            }} finally {{
                out.close();
            }}
            java.io.File dst = new java.io.File(outPath);
            dst.delete();
            tmp.renameTo(dst);
        }} catch (Throwable ignored) {{
        }}
    }}
}}
'''


def source_root_of(rel_path: str, source: str) -> str:
    """Project-relative source root of a .java file, derived by stripping
    its package path off its own path (`source/org/jfree/X.java` with
    `package org.jfree;` -> `source`). Avoids a `defects4j export` call and
    works for every project layout in the dataset."""
    m = re.search(r'(?m)^\s*package\s+([\w.]+)\s*;', source)
    directory = os.path.dirname(rel_path)
    if not m:
        return directory
    pkg_path = m.group(1).replace('.', os.sep)
    if directory == pkg_path:
        return ''
    suffix = os.sep + pkg_path
    if directory.endswith(suffix):
        return directory[:-len(suffix)]
    return directory


def instrument_patched_dir(patched_dir: str, patch_path: str,
                           flush_seconds: float = 2.0) -> DiffCovPlan:
    """Rewrite the patched WORKING COPY in place: a counter call at the
    entry of each patch-changed method, plus the generated helper class.
    Returns the plan (also written to `.diffcov_methods.json` beside the
    build so a later reader doesn't re-derive it)."""
    import json

    with open(patch_path, encoding='utf-8', errors='replace') as fh:
        patch_text = fh.read()
    plan = changed_methods(patch_text, patched_dir)

    by_file: Dict[str, List[Tuple[int, str]]] = {}
    for m in plan.methods:
        by_file.setdefault(m.rel_path, []).append(
            (m.insert_offset, m.method_id))

    helper_root = None
    for rel_path, targets in sorted(by_file.items()):
        full = os.path.join(patched_dir, rel_path)
        with open(full, encoding='utf-8', errors='replace') as fh:
            source = fh.read()
        if helper_root is None:
            helper_root = source_root_of(rel_path, source)
        with open(full, 'w', encoding='utf-8') as fh:
            fh.write(instrument_source(source, targets))

    if plan.methods:
        # ONE copy of the helper, in the first changed file's source root: a
        # second copy under another root that the same javac invocation also
        # compiles is a duplicate-class error.
        helper_dir = os.path.join(patched_dir, helper_root or '',
                                  HELPER_PACKAGE)
        os.makedirs(helper_dir, exist_ok=True)
        with open(os.path.join(helper_dir, f'{HELPER_CLASS}.java'),
                  'w', encoding='utf-8') as fh:
            fh.write(helper_source(plan.method_ids, flush_seconds))

    try:
        with open(os.path.join(patched_dir, '.diffcov_methods.json'),
                  'w', encoding='utf-8') as fh:
            json.dump(plan.as_dict(), fh, indent=1)
    except OSError:
        pass   # the plan is also returned; the file is a convenience
    return plan


# --- 3. collection --------------------------------------------------------

def parse_diffcov(text: str) -> Dict[str, int]:
    """`{method-id: hits}` from `[diffcov] method=<id> hits=<N>` lines.

    Repeated ids take the LARGEST count: the periodic flush and the shutdown
    hook can both land in one blob, and counters only ever grow.
    """
    counts: Dict[str, int] = {}
    for method_id, hits in _DIFFCOV_LINE_RE.findall(text or ''):
        n = int(hits)
        if n >= counts.get(method_id, -1):
            counts[method_id] = n
    return counts


def read_diffcov_file(path: str) -> Dict[str, int]:
    """Counts from the JVM's periodic dump; `{}` if it was never written."""
    try:
        with open(path, encoding='utf-8', errors='replace') as fh:
            return parse_diffcov(fh.read())
    except OSError:
        return {}
