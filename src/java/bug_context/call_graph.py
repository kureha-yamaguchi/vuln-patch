"""Call-reachability primitives over a fuzz-introspector project graph.

Given an introspector `project` (its `all_functions` profiles with
`base_callsites`), compute a bounded reachable-callee set for a mangled JVM
function name, plus the small name-normalisation helpers that support it
(mangled-name → bare/short name, project-membership test, overload arg
count). These were `@staticmethod`s on `TargetAnalyzer`; they carry no patch
or diff state — only the project graph — so they live here as plain
functions. `TargetAnalyzer` still owns the orchestration (which touched
function to resolve, how to fold the result into the prompt context).

All bounded and fail-soft: the BFS is capped by node count and depth so it
can never hang on a hub function, and every accessor degrades to [] / '' on
a shape it doesn't recognise.
"""
import re
from typing import Dict, List, Optional, Tuple

import config


def fi_method_name(mangled: str) -> str:
    """Bare method name from an introspector JVM name
    (``[pkg.Class].method(args)`` / ``Class.method(args)``)."""
    name = re.sub(r'^\[[^\]]*\]\.?', '', mangled).strip()
    name = name.split('(')[0]
    return name.split('.')[-1] if '.' in name else name


def arg_count(paren: str) -> int:
    """Number of comma-separated args inside the first (...) group, or -1 if
    there is none. Good enough to disambiguate overloads (generics with
    top-level commas are rare in these signatures)."""
    m = re.search(r'\(([^)]*)\)', paren)
    if not m:
        return -1
    inner = m.group(1).strip()
    return 0 if not inner else len([a for a in inner.split(',') if a.strip()])


def reachable_of(project, mangled: str, node_cap: int,
                 max_depth: int) -> List[str]:
    """Bounded reachable-function set for a mangled name.

    Budget-bounded BFS over immediate call-sites (``base_callsites``), NOT
    introspector's ``get_reachable_functions`` — that does an unbounded
    transitive walk that blows up to minutes of CPU on hub functions (e.g.
    ``inverseCumulativeProbability``). Direct callees are always included
    (BFS visits them first), then we expand breadth-first until a node cap,
    so cost is O(cap) irrespective of call-graph size and depth floats up to
    REACHABLE_MAX_DEPTH within that budget. Evidence from the Defects4J
    bugs: every downstream manifest-site / sibling sits at depth 1 (e.g.
    Math-2's ``getNumericalMean``), so a shallow capped walk suffices while
    the cap guarantees it can never hang.

    Falls back to the project-level getter (under a hard timeout) only for
    introspector versions that don't expose ``base_callsites``."""
    fmap = function_map(project)
    if fmap and mangled in fmap:
        names = bfs_callees(fmap, mangled, node_cap, max_depth)
        if names:
            return names
    getter = getattr(project, 'get_reachable_functions', None)
    if callable(getter):
        try:
            val = with_timeout(lambda: getter(function=mangled),
                               config.REACHABLE_TIMEOUT_SECONDS)
            if val:
                return as_name_list(val)
        except Exception:
            pass
    return []


def function_map(project) -> Dict[str, object]:
    """`project.all_functions` normalised to a name -> profile dict."""
    funcs = getattr(project, 'all_functions', None)
    if not funcs:
        return {}
    if isinstance(funcs, dict):
        return funcs
    return {getattr(f, 'name', ''): f
            for f in funcs if getattr(f, 'name', '')}


def bfs_callees(fmap: Dict[str, object], start: str,
                cap: int, max_depth: int) -> List[str]:
    """BFS the call graph from `start` via each profile's `base_callsites`
    (immediate callees), bounded by `cap` nodes and `max_depth` levels."""
    seen = {start}
    out: List[str] = []
    queue: List[Tuple[str, int]] = [(start, 0)]
    while queue and len(out) < cap:
        name, depth = queue.pop(0)
        prof = fmap.get(name)
        if prof is None:
            continue
        for cs in (getattr(prof, 'base_callsites', None) or []):
            if isinstance(cs, (list, tuple)) and cs:
                dst = cs[0]
            elif isinstance(cs, str):
                dst = cs
            else:
                dst = getattr(cs, 'dst_function_name', None)
            if not dst or dst in seen:
                continue
            seen.add(dst)
            out.append(dst)
            if len(out) >= cap:
                break
            if depth + 1 < max_depth:
                queue.append((dst, depth + 1))
    return out


def with_timeout(fn, seconds):
    """Run fn() but abort after `seconds` via SIGALRM (main thread only;
    degrades gracefully off-main-thread since signal.signal raises, caught
    by the caller)."""
    if not seconds or seconds <= 0:
        return fn()
    import signal

    def _handler(signum, frame):
        raise TimeoutError("reachable fallback exceeded budget")
    old = signal.signal(signal.SIGALRM, _handler)
    signal.alarm(int(seconds))
    try:
        return fn()
    finally:
        signal.alarm(0)
        signal.signal(signal.SIGALRM, old)


def as_name_list(val) -> List[str]:
    """Normalise whatever the reachable-set accessor returned (list of str,
    list of function objects, or dict) into a list of names."""
    if isinstance(val, dict):
        val = list(val.keys())
    out: List[str] = []
    for item in val:
        if isinstance(item, str):
            out.append(item)
        else:
            name = getattr(item, 'name', None)
            if name:
                out.append(name)
    return out


def receiver_of(mangled: str) -> str:
    """The ``[pkg.Class]`` receiver of an introspector JVM name, or ''.

    Every caller that needs the declaring class goes through this, so the
    bracket is parsed in exactly one place."""
    m = re.match(r'^\[([^\]]*)\]', mangled)
    return m.group(1) if m else ''


def is_project_fn(mangled: str, proj_prefix: str) -> bool:
    """True if a reachable function belongs to the project, judged by its
    RECEIVER class (the ``[pkg.Class]`` bracket) rather than the whole
    mangled name. The JVM frontend sometimes mis-types call arguments with
    the project package, so a substring check over the full name lets JDK
    statics (``Math.abs(...)``) leak through; the receiver bracket is the
    reliable signal — JDK/static calls have none."""
    return proj_prefix in receiver_of(mangled)


def simple_type(name: str) -> str:
    """A parameter type reduced to its simple name, for comparison.

    introspector writes a mangled parameter list in mixed spellings
    (``CharSequence``, ``java.io.Writer``), and the AST writes the simple
    name the source used. Comparing simple names is what lets the two
    agree. Generic arguments are dropped, and array brackets are kept,
    because ``byte[]`` and ``byte`` are different overloads."""
    name = name.strip()
    name = re.sub(r'<[^>]*>', '', name)
    dims = '[]' * name.count('[')
    name = name.split('[')[0].strip()
    return name.split('.')[-1] + dims


def mangled_param_types(mangled: str) -> List[str]:
    """Simple parameter type names of an introspector JVM name.

    Returns [] for a no-argument method and for a name with no argument
    list at all. Use `arg_count` to tell those two apart."""
    m = re.search(r'\(([^)]*)\)', mangled)
    if not m:
        return []
    inner = m.group(1).strip()
    if not inner:
        return []
    return [simple_type(a) for a in inner.split(',') if a.strip()]


def project_prefix(package: Optional[str]) -> str:
    """Reverse-domain prefix used to keep reachable functions that belong to
    the project and drop JDK/library noise. Derived from the touched file's
    package: first three components (e.g. ``org.apache.commons`` from
    ``org.apache.commons.math.special``), which is specific enough to
    exclude java.* / third-party calls without dropping the project's own
    neighbouring code."""
    if not package:
        return ''
    parts = package.split('.')
    return '.'.join(parts[:3]) if len(parts) >= 3 else package


def short_name(name: str) -> str:
    """Reduce a fully-qualified / mangled fuzz-introspector function name to
    something readable for the prompt: the `Class.method` tail.

    JVM names look like `[pkg.Class].method(args)` or `pkg.Class.method`.
    The receiver bracket carries the declaring class, so it is FOLDED IN
    rather than stripped. Dropping it used to reduce every overload of one
    method to the same bare word, which made two different functions
    indistinguishable in the prompt and made a same-name callee look like
    the touched function itself."""
    receiver = receiver_of(name)
    name = re.sub(r'^\[[^\]]*\]\.?', '', name).strip()
    # Drop argument lists.
    name = name.split('(')[0]
    parts = [p for p in name.split('.') if p]
    if receiver and len(parts) == 1:
        return f"{receiver.split('.')[-1]}.{parts[0]}"
    return '.'.join(parts[-2:]) if len(parts) >= 2 else name


def label_of(mangled: str) -> str:
    """`Class.method` for the prompt. See `short_name`."""
    return short_name(mangled)


def qualified_label(mangled: str) -> str:
    """`Class.method(TypeA, TypeB)` — used only to separate two kept
    functions whose `Class.method` labels are identical."""
    params = ', '.join(mangled_param_types(mangled))
    return f"{short_name(mangled)}({params})"
