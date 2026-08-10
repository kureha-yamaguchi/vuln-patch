"""Divergence capture at the diff boundary — WHICH observable does the patch
actually move, and on what shape of input?

Station: evidence assembly for relation synthesis (run.py, immediately before
`RelationSynthesizer.synthesize`), plus a build-time instrumentation pass that
is a sibling of diffcov's.

Failure mode it targets: a leg where reach is saturated and invention is
absent. 8.36 measured millions of entries into the patch-changed method with
zero firings; the relations probe the class's documented observables but never
the one the patch distorts, because nothing in the pipeline's evidence says
WHICH observable the patch touches. Invention is then guessing.

Three mechanical steps, none of them bug-specific:

  1. `wanted_from_patch` reuses diffcov's `changed_methods` VERBATIM (imported,
     not copied) to decide which methods are in scope — the same post-patch
     line -> declaration mapping, the same overload-separating method ids.
  2. `instrument_dir` rewrites a working copy so each of those methods logs,
     for the first N DISTINCT argument tuples it sees, one line
     `[divobs] method=<id> args=<typed> ret=<typed> count=<N> stable=<0|1>`
     (or `state=<typed fields>` for a method with no return value). Whole
     values, typed, capped COUNT — the 8.31 truncation lesson.
  3. `diff_observations` pairs the two builds' logs on (method, argument
     tuple) and reports the pairs whose observed value MOVED.

THE SOUNDNESS RULE, and it is the whole design: a divergence steers
ATTENTION (which observable, which input region). It is NEVER an oracle. A
correct patch diverges from the buggy build too — that is what fixing means —
so neither recorded value may become an expected value. The synthesis prompt
says so explicitly, and `java_source.anchors_buggy_value` demotes any relation
that anchors on a buggy-side value anyway.

Boundary: divergence records reach the relation-SYNTHESIS prompt and the run
artifacts. They do NOT reach the relation verifier, the judge, or any gate —
see the collection site in run.py.
"""
import json
import os
import re
import subprocess
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Sequence, Set, Tuple

import javalang

from java.parsing.java_source import skip_literal
# The diff -> method mapping and the source-geometry helpers are diffcov's,
# imported rather than re-derived: one parser for "which methods did the patch
# change", one brace matcher, one entry-offset rule (the constructor
# this()/super() case in particular). A second copy would drift.
from java.execution.diffcov import (  # noqa: F401
    changed_methods, method_declarations, source_root_of,
    _body_span, _entry_offset, _enclosing_types, _line_offsets, _match_brace,
    _offset_of, _skip_trivia, _type_name,
)

# Where the instrumented JVM writes its observation dump. Passed as an
# environment variable so no command line anywhere changes when the flag is
# off.
OUT_ENV_VAR = 'VULNPATCH_DIVCAP_OUT'

HELPER_PACKAGE = 'vulnpatch'
HELPER_CLASS = 'DivObs'
HELPER_FQN = f'{HELPER_PACKAGE}.{HELPER_CLASS}'

# Name of the local the entry injection declares. Reserved by construction:
# no project source uses a leading double underscore with this suffix.
ARGS_VAR = '__divcap_args'

PLAN_FILE = '.divcap_methods.json'
OUT_FILE = 'divobs.out'

_PRIMITIVES = frozenset({'boolean', 'byte', 'short', 'char', 'int', 'long',
                         'float', 'double'})
_BOXES = {'boolean': 'Boolean', 'byte': 'Byte', 'short': 'Short',
          'char': 'Character', 'int': 'Integer', 'long': 'Long',
          'float': 'Float', 'double': 'Double'}

# args= is non-greedy and the trailing fields are fixed-width-ish, so a value
# containing spaces or '=' still parses; the Java side escapes newlines so a
# value can never span lines.
_DIVOBS_LINE_RE = re.compile(
    r'^\[divobs\] method=(\S+) args=(.*?) (ret|state)=(.*) '
    r'count=(\d+) stable=([01])\s*$', re.MULTILINE)

_IDENT = re.compile(r'[A-Za-z0-9_$]')


# --- 1. which methods, and everything the injection needs to know ---------

@dataclass
class ObsTarget:
    """One method/constructor to instrument, in ONE tree."""
    rel_path: str
    class_name: str
    method_name: str
    param_types: Tuple[str, ...]
    param_names: Tuple[str, ...]
    return_type: Optional[str]      # erased, raw; None for void/constructor
    is_constructor: bool
    is_static: bool
    decl_line: int
    entry_offset: int
    body_open: int
    body_end: int
    return_sites: Tuple[Tuple[int, int], ...]

    @property
    def method_id(self) -> str:
        return (f'{self.class_name}#{self.method_name}'
                f'({",".join(self.param_types)})')

    @property
    def signature(self) -> Tuple[str, str, Tuple[str, ...]]:
        return (self.class_name, self.method_name, self.param_types)

    @property
    def observable(self) -> str:
        """Which of the two capture shapes this target uses."""
        return 'ret' if self.return_type is not None else 'state'


@dataclass
class DivCapPlan:
    targets: List[ObsTarget] = field(default_factory=list)
    # Methods the patch changed that carry NO capturable observable in this
    # tree, with the reason. Recorded rather than dropped: an empty capture
    # otherwise reads as "the patch moves nothing" when it means "there was
    # nothing here to watch".
    skipped: List[dict] = field(default_factory=list)

    @property
    def method_ids(self) -> List[str]:
        return [t.method_id for t in self.targets]

    def as_dict(self) -> dict:
        return {
            'methods': [{'method_id': t.method_id, 'file': t.rel_path,
                         'line': t.decl_line, 'observable': t.observable}
                        for t in self.targets],
            'skipped': list(self.skipped),
        }


def wanted_from_patch(patch_text: str,
                      patched_dir: str) -> Dict[str, List[tuple]]:
    """`{post-patch relative path: [(class, method, param_types)]}` — the
    methods the patch changed, straight off diffcov's mapping.

    The signature triple, not a file offset, is what identifies a method
    across the two trees: the same declaration sits at a different offset in
    the buggy sources, and matching on the signature is what pairs them.
    """
    plan = changed_methods(patch_text, patched_dir)
    out: Dict[str, List[tuple]] = {}
    for m in plan.methods:
        out.setdefault(m.rel_path, []).append(
            (m.class_name, m.method_name, m.param_types))
    return out


def obs_targets(source: str,
                wanted: Sequence[tuple]) -> Tuple[List[ObsTarget], List[dict]]:
    """Locate each wanted `(class, method, param_types)` in one compilation
    unit and work out how to instrument it.

    Fail-closed everywhere: a declaration this module cannot rewrite with
    confidence is SKIPPED with a reason, never rewritten on a guess. A
    mis-rewrite costs the whole leg its patched build.
    """
    want: Set[tuple] = set(wanted)
    skipped: List[dict] = []
    try:
        tree = javalang.parse.parse(source)
    except (javalang.parser.JavaSyntaxError, javalang.tokenizer.LexerError,
            IndexError, TypeError) as exc:
        return [], [{'reason': f'javalang: {exc}'}]

    package = tree.package.name if tree.package else ''
    offsets = _line_offsets(source)
    # Every declaration's character range, so a `return` belonging to a
    # nested or anonymous class is attributed to that class and left alone.
    all_spans = [(d['start'], d['end']) for d in method_declarations(source)]

    targets: List[ObsTarget] = []
    seen: Set[tuple] = set()
    for path, node in tree:
        is_ctor = isinstance(node, javalang.tree.ConstructorDeclaration)
        if not is_ctor and not isinstance(node,
                                          javalang.tree.MethodDeclaration):
            continue
        if not node.position:
            continue
        type_chain = _enclosing_types(path)
        class_name = '.'.join(([package] if package else []) + type_chain)
        class_name = class_name or node.name
        params = node.parameters or []
        sig = (class_name, node.name, tuple(_type_name(p) for p in params))
        if sig not in want or sig in seen:
            continue
        decl_off = _offset_of(offsets, node.position.line,
                              node.position.column)
        if decl_off is None:
            continue
        open_idx, end_idx = _body_span(source, decl_off, node.name)
        if open_idx < 0 or end_idx < 0:
            skipped.append({'method': _sig_id(sig),
                            'reason': 'no body (abstract/native/interface)'})
            seen.add(sig)
            continue
        modifiers = node.modifiers or set()
        is_static = 'static' in modifiers
        ret = None if is_ctor else getattr(node, 'return_type', None)
        return_type = _raw_type(ret) if ret is not None else None

        body = source[open_idx:end_idx]
        if return_type is None:
            if is_static:
                # No receiver to photograph and no return value: nothing at
                # this frame is observable without argument-mutation
                # tracking, which v1 does not do.
                skipped.append({'method': _sig_id(sig),
                                'reason': 'static void (no observable)'})
                seen.add(sig)
                continue
            sites: Tuple[Tuple[int, int], ...] = ()
        else:
            if '->' in body:
                # A lambda body's `return` is not inside any declaration
                # javalang reports, so the exclusion below cannot see it and
                # the rewrite would type-clash with the lambda's own return
                # type. Skip rather than risk the build.
                skipped.append({'method': _sig_id(sig),
                                'reason': 'lambda in body (return rewrite '
                                          'unsafe)'})
                seen.add(sig)
                continue
            inner = [(a, b) for a, b in all_spans
                     if a > open_idx and b < end_idx]
            sites = tuple(_return_sites(source, open_idx, end_idx, inner))
            if not sites:
                skipped.append({'method': _sig_id(sig),
                                'reason': 'no value-returning statement'})
                seen.add(sig)
                continue
        seen.add(sig)
        targets.append(ObsTarget(
            rel_path='', class_name=class_name, method_name=node.name,
            param_types=sig[2],
            param_names=tuple(p.name for p in params),
            return_type=return_type,
            is_constructor=is_ctor, is_static=is_static,
            decl_line=node.position.line,
            entry_offset=_entry_offset(source, open_idx),
            body_open=open_idx, body_end=end_idx,
            return_sites=sites,
        ))
    for sig in sorted(want - seen):
        skipped.append({'method': _sig_id(sig),
                        'reason': 'declaration not found in this tree'})
    targets.sort(key=lambda t: t.decl_line)
    return targets, skipped


def _sig_id(sig: tuple) -> str:
    return f'{sig[0]}#{sig[1]}({",".join(sig[2])})'


def _raw_type(node) -> str:
    """Erased, raw rendering of a return type — `int`, `double[]`,
    `Map`, `Map.Entry`. Generic arguments are dropped: the injected cast is
    a raw cast (an unchecked-conversion warning, never an error), and
    several projects in this dataset compile at a `-source` level where
    generics are a syntax error."""
    chain = [getattr(node, 'name', None) or 'Object']
    dims = len(getattr(node, 'dimensions', None) or [])
    sub = getattr(node, 'sub_type', None)
    while sub is not None:
        chain.append(getattr(sub, 'name', ''))
        dims = len(getattr(sub, 'dimensions', None) or []) or dims
        sub = getattr(sub, 'sub_type', None)
    return '.'.join(p for p in chain if p) + '[]' * dims


def _return_sites(src: str, open_idx: int, end_idx: int,
                  exclude: Sequence[Tuple[int, int]]) -> List[Tuple[int, int]]:
    """`[(offset of `return`, offset of its `;`)]` for the value-returning
    statements that belong to THIS declaration.

    Comment- and literal-aware, and any `return` inside a strictly smaller
    declaration (a method of an anonymous or nested class) is excluded — it
    belongs to that method, not this one."""
    sites: List[Tuple[int, int]] = []
    i = open_idx
    while i < end_idx:
        if src.startswith('//', i) or src.startswith('/*', i):
            i = _skip_trivia(src, i)
            continue
        if src[i] in '"\'':
            i = skip_literal(src, i)
            continue
        if src.startswith('return', i):
            before = src[i - 1] if i else ' '
            after = src[i + 6] if i + 6 < len(src) else ' '
            if not _IDENT.match(before) and not _IDENT.match(after):
                if any(a <= i < b for a, b in exclude):
                    i += 6
                    continue
                end = _statement_end(src, i + 6, end_idx)
                if end < 0:
                    return []          # unparseable body: capture nothing
                if src[i + 6:end].strip():
                    sites.append((i, end))
                i = end + 1
                continue
        i += 1
    return sites


def _statement_end(src: str, i: int, limit: int) -> int:
    """Offset of the `;` closing the statement that starts at `i`, or -1."""
    depth = 0
    while i < limit:
        if src.startswith('//', i) or src.startswith('/*', i):
            i = _skip_trivia(src, i)
            continue
        c = src[i]
        if c in '"\'':
            i = skip_literal(src, i)
            continue
        if c in '([{':
            depth += 1
        elif c in ')]}':
            depth -= 1
            if depth < 0:
                return -1
        elif c == ';' and depth == 0:
            return i
        i += 1
    return -1


# --- 2. source instrumentation -------------------------------------------

def instrument_source(source: str, targets: Sequence[ObsTarget]) -> str:
    """Rewrite one compilation unit so each target logs its observable.

    Every edit is INLINE — no newline is ever inserted — so each line keeps
    its number and stack traces, the trigger-test net and any later read of
    the tree still line up with the diff (diffcov's rule, same reason).

    Two shapes:
      * a method with a return value declares `String __divcap_args = ...`
        at its entry and each of its own `return <expr>;` becomes
        `return <cast> DivObs.ret(id, __divcap_args, <expr>);`
      * a void method or constructor wraps its body in `try { ... } finally
        { DivObs.state(id, __divcap_args, this); }` — the observable is the
        receiver's own primitive/array state AFTER the call, which is the
        only thing that frame produces.
    """
    edits: List[Tuple[int, int, str]] = []
    for t in targets:
        mid = t.method_id
        args = (f' String {ARGS_VAR} = {HELPER_FQN}.args("{mid}", '
                f'{_args_array(t)});')
        if t.return_type is None:
            edits.append((t.entry_offset, t.entry_offset, args + ' try {'))
            edits.append((t.body_end, t.body_end,
                          f' }} finally {{ {HELPER_FQN}.state("{mid}", '
                          f'{ARGS_VAR}, this); }} '))
            continue
        edits.append((t.entry_offset, t.entry_offset, args))
        cast = ('' if t.return_type in _PRIMITIVES
                else f'({t.return_type}) ')
        for start, semi in t.return_sites:
            expr = source[start + 6:semi]
            edits.append((start, semi + 1,
                          f'return {cast}{HELPER_FQN}.ret("{mid}", '
                          f'{ARGS_VAR},{expr});'))
    out = source
    for start, stop, text in sorted(edits, key=lambda e: e[0], reverse=True):
        out = out[:start] + text + out[stop:]
    return out


def _args_array(t: ObsTarget) -> str:
    """`new Object[]{...}` for the target's parameters, with EXPLICIT boxing.

    Autoboxing is a syntax error at the `-source` levels several projects in
    this dataset compile at (the same constraint DiffCov.java is written
    under), so a primitive parameter is wrapped by hand.
    """
    if not t.param_names:
        return 'new Object[0]'
    items = []
    for name, ptype in zip(t.param_names, t.param_types):
        box = _BOXES.get(ptype)
        items.append(f'new {box}({name})' if box else name)
    return 'new Object[]{' + ', '.join(items) + '}'


def helper_source(method_ids: Sequence[str], flush_seconds: float = 2.0,
                  max_shapes: int = 64, max_obs: int = 200000) -> str:
    """Source of the generated `vulnpatch/DivObs.java`.

    Same old-Java dialect as DiffCov.java and for the same reason: raw
    collections, no generics, no for-each, no autoboxing, no annotations, no
    diamond, ASCII only. See docs/diffcov-instrumentation-2026-08-09.md.

    Bounding follows the 8.31 lesson — values are whole and typed, what is
    capped is the COUNT of distinct argument tuples per method (`max_shapes`)
    and the number of calls watched before the method goes inert
    (`max_obs`). The only width bound is a per-LINE safety valve, and when it
    bites it says so in the line.
    """
    unique = list(dict.fromkeys(method_ids))
    ids = ',\n'.join(f'        "{mid}"' for mid in unique)
    return f'''package {HELPER_PACKAGE};

/* GENERATED by the vuln-patch pipeline (src/java/execution/divcap.py) for ONE
   build. Records, per patch-changed method, the observable at the diff
   boundary for the first {max_shapes} distinct argument tuples it sees.
   The two builds' logs are paired and diffed OUTSIDE the JVM.
   ASCII ONLY, and in a deliberately old Java dialect (raw collections, no
   for-each, no autoboxing, no annotations, no generics) - `defects4j compile`
   runs the project's own historical -source level with the platform charset. */
public final class {HELPER_CLASS} {{

    private static final int MAX_SHAPES = {max_shapes};
    private static final int MAX_OBS = {max_obs};
    private static final int MAX_LINE = 20000;
    private static final long FLUSH_MILLIS = {max(int(flush_seconds * 1000), 250)}L;

    private static final String[] METHODS = {{
{ids}
    }};

    /* One record per DISTINCT argument tuple. `stable` goes false when the
       same tuple produced two different values inside ONE build - a value
       that is not stable within a build cannot evidence a divergence
       between builds, and the reader drops it. */
    static final class Obs {{
        String kind;
        String value;
        long count;
        boolean stable = true;
    }}

    private static final java.util.HashMap SHAPES = new java.util.HashMap();
    private static final java.util.HashMap TOTALS = new java.util.HashMap();

    private static String outPath = null;

    static {{
        /* Every step is optional: a static initializer that throws would
           turn each instrumented class into a NoClassDefFoundError and change
           what the run observes. */
        try {{
            int i = 0;
            while (i < METHODS.length) {{
                SHAPES.put(METHODS[i], new java.util.LinkedHashMap());
                TOTALS.put(METHODS[i], new long[1]);
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

    /* Method entry. Returns the rendered argument tuple, or null once this
       method has gone inert - null makes every ret()/state() below a no-op,
       so a saturated method costs one map lookup per call and nothing else. */
    public static String args(String id, Object[] a) {{
        try {{
            long[] total = (long[]) TOTALS.get(id);
            if (total == null || total[0] >= MAX_OBS) {{
                return null;
            }}
            total[0]++;
            StringBuffer sb = new StringBuffer();
            int i = 0;
            while (i < a.length) {{
                if (i > 0) {{
                    sb.append('|');
                }}
                sb.append(val(a[i]));
                i++;
            }}
            return sb.toString();
        }} catch (Throwable ignored) {{
            return null;
        }}
    }}

    public static boolean ret(String id, String a, boolean v) {{ record(id, a, "ret", "z:" + v); return v; }}
    public static byte ret(String id, String a, byte v) {{ record(id, a, "ret", "b:" + v); return v; }}
    public static short ret(String id, String a, short v) {{ record(id, a, "ret", "h:" + v); return v; }}
    public static char ret(String id, String a, char v) {{ record(id, a, "ret", "c:" + v); return v; }}
    public static int ret(String id, String a, int v) {{ record(id, a, "ret", "i:" + v); return v; }}
    public static long ret(String id, String a, long v) {{ record(id, a, "ret", "l:" + v); return v; }}
    public static float ret(String id, String a, float v) {{ record(id, a, "ret", "f:" + v); return v; }}
    public static double ret(String id, String a, double v) {{ record(id, a, "ret", "d:" + v); return v; }}
    public static Object ret(String id, String a, Object v) {{ record(id, a, "ret", val(v)); return v; }}

    public static void state(String id, String a, Object receiver) {{
        record(id, a, "state", fields(receiver));
    }}

    private static synchronized void record(String id, String a, String kind,
                                            String value) {{
        if (a == null) {{
            return;
        }}
        try {{
            java.util.LinkedHashMap m =
                (java.util.LinkedHashMap) SHAPES.get(id);
            if (m == null) {{
                return;
            }}
            Obs o = (Obs) m.get(a);
            if (o != null) {{
                o.count++;
                if (!o.value.equals(value)) {{
                    o.stable = false;
                }}
                return;
            }}
            if (m.size() >= MAX_SHAPES) {{
                return;
            }}
            o = new Obs();
            o.kind = kind;
            o.value = value;
            o.count = 1L;
            m.put(a, o);
        }} catch (Throwable ignored) {{
        }}
    }}

    /* Typed, WHOLE value. The tags match the pipeline's existing consumed-
       value conventions (z/b/h/i/l/f/d/c scalars, q for strings, *a for
       arrays, o for anything else). */
    private static String val(Object v) {{
        if (v == null) {{
            return "null";
        }}
        try {{
            if (v instanceof Boolean) return "z:" + v;
            if (v instanceof Byte) return "b:" + v;
            if (v instanceof Short) return "h:" + v;
            if (v instanceof Character) return "c:" + v;
            if (v instanceof Integer) return "i:" + v;
            if (v instanceof Long) return "l:" + v;
            if (v instanceof Float) return "f:" + v;
            if (v instanceof Double) return "d:" + v;
            if (v instanceof String) return "q:\\"" + esc((String) v) + "\\"";
            if (v instanceof boolean[]) return "za:" + java.util.Arrays.toString((boolean[]) v);
            if (v instanceof byte[]) return "ba:" + java.util.Arrays.toString((byte[]) v);
            if (v instanceof short[]) return "ha:" + java.util.Arrays.toString((short[]) v);
            if (v instanceof char[]) return "ca:" + java.util.Arrays.toString((char[]) v);
            if (v instanceof int[]) return "ia:" + java.util.Arrays.toString((int[]) v);
            if (v instanceof long[]) return "la:" + java.util.Arrays.toString((long[]) v);
            if (v instanceof float[]) return "fa:" + java.util.Arrays.toString((float[]) v);
            if (v instanceof double[]) return "da:" + java.util.Arrays.toString((double[]) v);
            if (v instanceof Object[]) return "oa:" + esc(java.util.Arrays.deepToString((Object[]) v));
            return "o:" + v.getClass().getName() + "("
                   + esc(String.valueOf(v)) + ")";
        }} catch (Throwable ignored) {{
            return "o:<unrenderable>";
        }}
    }}

    /* The receiver's own primitive and primitive-array fields, in NAME order
       so the two builds render the same object the same way. Sorted, not
       declaration-ordered: getDeclaredFields() order is unspecified, and an
       ordering difference would read as a divergence. */
    private static String fields(Object o) {{
        if (o == null) {{
            return "null";
        }}
        StringBuffer sb = new StringBuffer();
        sb.append(o.getClass().getName());
        try {{
            java.util.TreeMap seen = new java.util.TreeMap();
            Class c = o.getClass();
            while (c != null && c != Object.class) {{
                java.lang.reflect.Field[] fs = c.getDeclaredFields();
                int i = 0;
                while (i < fs.length) {{
                    java.lang.reflect.Field f = fs[i];
                    i++;
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {{
                        continue;
                    }}
                    Class t = f.getType();
                    if (!t.isPrimitive()
                        && !(t.isArray() && t.getComponentType().isPrimitive())) {{
                        continue;
                    }}
                    if (seen.containsKey(f.getName())) {{
                        continue;
                    }}
                    try {{
                        f.setAccessible(true);
                        seen.put(f.getName(), val(f.get(o)));
                    }} catch (Throwable ignore) {{
                    }}
                }}
                c = c.getSuperclass();
            }}
            java.util.Iterator it = seen.entrySet().iterator();
            while (it.hasNext()) {{
                java.util.Map.Entry e = (java.util.Map.Entry) it.next();
                sb.append(' ').append((String) e.getKey()).append('=')
                  .append((String) e.getValue());
            }}
        }} catch (Throwable ignored) {{
        }}
        return sb.toString();
    }}

    /* Newlines and carriage returns become two-character escapes: one
       observation is one line, and the reader's line regex depends on it.
       An identity hash (`Foo@1a2b3c`) is collapsed to `Foo@` - it differs
       between two JVMs for the same logical value and would read as a
       divergence on every object without a toString(). */
    private static String esc(String s) {{
        if (s == null) {{
            return "null";
        }}
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (i < s.length()) {{
            char ch = s.charAt(i);
            i++;
            if (ch == '\\n') {{
                sb.append("\\\\n");
            }} else if (ch == '\\r') {{
                sb.append("\\\\r");
            }} else if (ch == '@') {{
                sb.append('@');
                while (i < s.length() && isHex(s.charAt(i))) {{
                    i++;
                }}
            }} else {{
                sb.append(ch);
            }}
        }}
        return sb.toString();
    }}

    private static boolean isHex(char c) {{
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
               || (c >= 'A' && c <= 'F');
    }}

    private static synchronized String render() {{
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (i < METHODS.length) {{
            java.util.LinkedHashMap m =
                (java.util.LinkedHashMap) SHAPES.get(METHODS[i]);
            i++;
            if (m == null) {{
                continue;
            }}
            java.util.Iterator it = m.entrySet().iterator();
            while (it.hasNext()) {{
                java.util.Map.Entry e = (java.util.Map.Entry) it.next();
                Obs o = (Obs) e.getValue();
                String line = "[divobs] method=" + METHODS[i - 1]
                    + " args=" + e.getKey()
                    + " " + o.kind + "=" + o.value
                    + " count=" + o.count
                    + " stable=" + (o.stable ? "1" : "0");
                if (line.length() > MAX_LINE) {{
                    line = line.substring(0, MAX_LINE)
                        + " [divcap-line-truncated] count=" + o.count
                        + " stable=0";
                }}
                sb.append(line).append('\\n');
            }}
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


def instrument_dir(root_dir: str, wanted_by_file: Dict[str, List[tuple]],
                   flush_seconds: float = 2.0,
                   max_shapes: int = 64) -> DivCapPlan:
    """Rewrite a WORKING COPY in place — observation calls in each wanted
    method, plus the generated helper class. Works on either tree: the
    patched one (where `wanted_by_file` came from the diff) and the buggy
    one (same signatures, different offsets)."""
    plan = DivCapPlan()
    helper_root = None
    for rel_path, wanted in sorted(wanted_by_file.items()):
        full = os.path.join(root_dir, rel_path)
        try:
            with open(full, encoding='utf-8', errors='replace') as fh:
                source = fh.read()
        except OSError:
            plan.skipped.append({'file': rel_path,
                                 'reason': 'source not readable in this tree'})
            continue
        targets, skipped = obs_targets(source, wanted)
        for s in skipped:
            plan.skipped.append(dict(s, file=rel_path))
        if not targets:
            continue
        for t in targets:
            t.rel_path = rel_path
        if helper_root is None:
            helper_root = source_root_of(rel_path, source)
        with open(full, 'w', encoding='utf-8') as fh:
            fh.write(instrument_source(source, targets))
        plan.targets.extend(targets)

    if plan.targets:
        # ONE copy of the helper, in the first instrumented file's source
        # root: a second copy under another root the same javac invocation
        # compiles is a duplicate-class error.
        helper_dir = os.path.join(root_dir, helper_root or '', HELPER_PACKAGE)
        os.makedirs(helper_dir, exist_ok=True)
        with open(os.path.join(helper_dir, f'{HELPER_CLASS}.java'), 'w',
                  encoding='utf-8') as fh:
            fh.write(helper_source(plan.method_ids, flush_seconds, max_shapes))
    try:
        with open(os.path.join(root_dir, PLAN_FILE), 'w',
                  encoding='utf-8') as fh:
            json.dump(plan.as_dict(), fh, indent=1)
    except OSError:
        pass    # the plan is also returned; the file is a convenience
    return plan


def instrument_patched_dir(patched_dir: str, patch_path: str,
                           flush_seconds: float = 2.0,
                           max_shapes: int = 64) -> DivCapPlan:
    """Instrument the PATCHED working copy and remember which signatures it
    covered — `.divcap_wanted.json`, so the buggy twin can be instrumented
    from the same list without re-reading the diff against the wrong tree."""
    with open(patch_path, encoding='utf-8', errors='replace') as fh:
        patch_text = fh.read()
    wanted = wanted_from_patch(patch_text, patched_dir)
    try:
        with open(os.path.join(patched_dir, '.divcap_wanted.json'), 'w',
                  encoding='utf-8') as fh:
            json.dump({k: [list(s) for s in v] for k, v in wanted.items()},
                      fh, indent=1)
    except OSError:
        pass
    return instrument_dir(patched_dir, wanted, flush_seconds, max_shapes)


def read_wanted(patched_dir: str) -> Dict[str, List[tuple]]:
    """The signature list a previous `instrument_patched_dir` wrote."""
    try:
        with open(os.path.join(patched_dir, '.divcap_wanted.json'),
                  encoding='utf-8') as fh:
            raw = json.load(fh)
    except (OSError, ValueError):
        return {}
    return {k: [(s[0], s[1], tuple(s[2])) for s in v]
            for k, v in (raw or {}).items()}


# --- 3. collection, pairing, ranking --------------------------------------

@dataclass(frozen=True)
class Observation:
    method_id: str
    shape: str          # the rendered argument tuple; the pairing key
    kind: str           # 'ret' | 'state'
    value: str
    count: int
    stable: bool


@dataclass(frozen=True)
class Divergence:
    method_id: str
    observable: str     # 'return value' | 'receiver state after the call'
    input_shape: str
    buggy_value: str
    patched_value: str
    count: int

    def as_dict(self) -> dict:
        return {'method': self.method_id, 'observable': self.observable,
                'input_shape': self.input_shape,
                'buggy_value': self.buggy_value,
                'patched_value': self.patched_value, 'count': self.count}


_OBSERVABLE = {'ret': 'return value',
               'state': 'receiver state after the call'}


def parse_divobs(text: str) -> List[Observation]:
    """Observations from `[divobs] ...` lines.

    The periodic flush and the shutdown hook can both land in one blob, so a
    repeated (method, shape) takes the LARGEST count; if two copies disagree
    on the VALUE the observation is marked unstable and the reader drops it.
    """
    by_key: Dict[Tuple[str, str], Observation] = {}
    for method_id, shape, kind, value, count, stable in _DIVOBS_LINE_RE.findall(
            text or ''):
        key = (method_id, shape)
        obs = Observation(method_id=method_id, shape=shape, kind=kind,
                          value=value, count=int(count), stable=stable == '1')
        prev = by_key.get(key)
        if prev is None:
            by_key[key] = obs
            continue
        by_key[key] = Observation(
            method_id=method_id, shape=shape, kind=kind,
            value=obs.value if obs.count >= prev.count else prev.value,
            count=max(obs.count, prev.count),
            stable=prev.stable and obs.stable and prev.value == obs.value)
    return list(by_key.values())


def merge_observations(*groups: Sequence[Observation]) -> List[Observation]:
    """Fold several runs of the SAME build into one observation set (one
    `defects4j test -t` per trigger test, one JVM each)."""
    merged: Dict[Tuple[str, str], Observation] = {}
    for group in groups:
        for obs in group:
            key = (obs.method_id, obs.shape)
            prev = merged.get(key)
            if prev is None:
                merged[key] = obs
                continue
            merged[key] = Observation(
                method_id=obs.method_id, shape=obs.shape, kind=obs.kind,
                value=prev.value, count=prev.count + obs.count,
                stable=prev.stable and obs.stable
                and prev.value == obs.value and prev.kind == obs.kind)
    return list(merged.values())


def diff_observations(buggy: Sequence[Observation],
                      patched: Sequence[Observation]) -> List[Divergence]:
    """Pair on (method, argument tuple) and report the pairs whose observed
    value MOVED.

    Fail-closed on everything ambiguous: an observation that was not stable
    within its own build, or whose two sides watched different observables,
    is not a divergence — it is an unknown, and an unknown must not steer
    invention.
    """
    b_index = {(o.method_id, o.shape): o for o in buggy if o.stable}
    p_index = {(o.method_id, o.shape): o for o in patched if o.stable}
    out: List[Divergence] = []
    for key in sorted(set(b_index) & set(p_index)):
        b, p = b_index[key], p_index[key]
        if b.kind != p.kind or b.value == p.value:
            continue
        out.append(Divergence(
            method_id=b.method_id,
            observable=_OBSERVABLE.get(b.kind, b.kind),
            input_shape=b.shape,
            buggy_value=b.value,
            patched_value=p.value,
            # The paired count: how many times BOTH builds were observed on
            # this tuple. Claiming the larger side would overstate it.
            count=min(b.count, p.count)))
    return out


def rank_divergences(divergences: Sequence[Divergence],
                     k: int = 8) -> List[Divergence]:
    """Top `k`, DIVERSITY first and frequency second (prereg decision 3).

    Diversity is applied twice. Methods are ordered by how many distinct
    argument tuples diverge under them, and then the slots are filled
    ROUND-ROBIN across methods — so eight slots cannot all be eaten by eight
    near-identical tuples of one method while a second changed method goes
    unmentioned. Within a method, the most frequently observed tuple first.
    """
    groups: Dict[str, List[Divergence]] = {}
    for d in divergences:
        groups.setdefault(d.method_id, []).append(d)
    for items in groups.values():
        items.sort(key=lambda d: (-d.count, d.input_shape))
    order = sorted(groups, key=lambda m: (-len(groups[m]), m))
    out: List[Divergence] = []
    row = 0
    while len(out) < k:
        added = False
        for method_id in order:
            items = groups[method_id]
            if row < len(items):
                out.append(items[row])
                added = True
                if len(out) >= k:
                    break
        if not added:
            break
        row += 1
    return out


def untag(value: str) -> str:
    """The bare literal behind a typed value — `i:-2` -> `-2`,
    `q:"09"` -> `09`, `d:0.5` -> `0.5`. What a relation's expected literal
    would have to equal for the anti-anchoring lint to match."""
    v = (value or '').strip()
    m = re.match(r'^[a-z]{1,2}:(.*)$', v, re.DOTALL)
    if m:
        v = m.group(1)
    if len(v) >= 2 and v.startswith('"') and v.endswith('"'):
        v = v[1:-1]
    return v


_FIELD_RE = re.compile(r'(\w+)=(\S+)')


def buggy_side_values(divergences: Sequence[Divergence]) -> List[str]:
    """Bare buggy-side literals of the divergences, for the anti-anchoring
    lint. Only values that MOVED are here by construction, which is exactly
    the lint's precondition.

    A receiver-state observation is a whole field dump, which no relation
    would ever compare against as one string — so it is split back into its
    fields and only the fields that actually MOVED contribute a value.
    """
    out: List[str] = []

    def _add(text: str) -> None:
        if text and text not in out:
            out.append(text)

    for d in divergences:
        if d.observable == _OBSERVABLE['ret']:
            _add(untag(d.buggy_value))
            continue
        after = dict(_FIELD_RE.findall(d.patched_value))
        for name, value in _FIELD_RE.findall(d.buggy_value):
            if after.get(name) != value:
                _add(untag(value))
    return out


# --- 4. the run-time collection pass (VM only) ----------------------------

def collect_divergences(buggy_dir: str, patch_path: str,
                        top_k: int = 8) -> dict:
    """Run the SAME inputs through both instrumented builds and return the
    ranked divergences.

    The driver is the bug's own trigger tests, which is the one execution
    this pipeline already sends through both builds with byte-identical
    inputs and no fuzzing luck in between. Its limits are documented in
    docs/divcap-build-2026-08-10.md §2.

    Fail-soft in every direction: any build, compile or run failure returns
    a result with `status` set and no divergences. The flag must never be
    able to cost a leg its run.

    The instrumentation's own tuning (`DIVCAP_FLUSH_SECONDS`,
    `DIVCAP_MAX_SHAPES`) is read by the builder from `config`, so the
    environment sets it once for both trees.
    """
    from java.execution.fuzz_runner import PatchedProjectBuilder

    result = {'status': 'ok', 'divergences': [], 'plan': None,
              'buggy_observations': 0, 'patched_observations': 0}
    try:
        builder = PatchedProjectBuilder(divcap=True)
        # verify_trigger=False: the trigger net is the main pipeline's job on
        # its own tree, and this pass RUNS those tests itself two lines down.
        patched_dir = builder.build_patched_dir(buggy_dir, patch_path,
                                                verify_trigger=False)
        result['plan'] = builder.divcap_plan
        if not (builder.divcap_plan or {}).get('methods'):
            result['status'] = 'no instrumentable changed method'
            return result
        buggy_inst = builder.build_divcap_buggy_dir(buggy_dir, patched_dir)
        triggers = PatchedProjectBuilder._trigger_tests(buggy_dir)
        patched_obs = run_and_read(patched_dir, triggers)
        buggy_obs = run_and_read(buggy_inst, triggers)
    except Exception as exc:
        result['status'] = f'divcap unavailable: {exc}'
        return result
    result['buggy_observations'] = len(buggy_obs)
    result['patched_observations'] = len(patched_obs)
    result['divergences'] = rank_divergences(
        diff_observations(buggy_obs, patched_obs), top_k)
    return result


def run_and_read(project_dir: str,
                 tests: Sequence[str]) -> List[Observation]:
    """Run each trigger test in `project_dir` and return what the
    instrumented methods observed.

    The dump FILE is the primary channel and stderr the fallback, matching
    diffcov's collector. Unlike the fuzz runner's SIGKILL path a
    `defects4j test` JVM does exit normally, so the shutdown hook is
    reliable here — but the file survives a d4j harness that swallows
    stderr, so both are read."""
    out_path = os.path.join(project_dir, OUT_FILE)
    groups: List[List[Observation]] = []
    for test in tests:
        try:
            os.unlink(out_path)     # never read a previous test's dump
        except OSError:
            pass
        env = dict(os.environ)
        env[OUT_ENV_VAR] = out_path
        try:
            proc = subprocess.run(['defects4j', 'test', '-t', test],
                                  cwd=project_dir, env=env,
                                  capture_output=True, text=True)
        except Exception:
            continue
        text = ''
        try:
            with open(out_path, encoding='utf-8', errors='replace') as fh:
                text = fh.read()
        except OSError:
            pass
        if not text:
            text = (proc.stdout or '') + '\n' + (proc.stderr or '')
        groups.append(parse_divobs(text))
    return merge_observations(*groups)
