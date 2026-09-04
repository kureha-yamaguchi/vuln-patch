"""Walk the call graph outwards from a set of seed methods.

MEASUREMENT ONLY — nothing in the pipeline imports this.

The paper's patch-derived set P is "the code a patch plausibly touches the
behaviour of": the methods the patch physically changed (the SEEDS), the
methods that call them (the CALLERS, one level up), and the methods they
call (the CALLEES, a bounded walk downwards). This module is the one place
that construction lives, so P (built from an APR patch) and R-hat (the same
construction built from the developer's fix) are produced by identical
code and are therefore comparable.

Everything is bounded, so it can never hang on a hub method:

  callers  at most `caller_cap` per seed (config.MAX_XREFS_PER_FUNCTION),
           taken in sorted order so two runs pick the same ones;
  callees  a breadth-first walk stopped by `callee_cap` nodes
           (config.REACHABLE_NODE_CAP) and `callee_depth` levels
           (config.REACHABLE_MAX_DEPTH). The walk itself is
           `call_graph.bfs_callees_with_edges` — the same traversal the
           pipeline uses, asked to also report the edges it crossed and
           the depth it first reached each node at.

The graph is a fuzz-introspector project: a list of function profiles, each
with a mangled `.name` (``[pkg.Class].method(argtypes)``) and a
`.base_callsites` list of the functions it calls. Introspector has no
"who calls me" index, so callers are found by inverting `base_callsites`
over the whole project once.

THE EMPTY CALLER RING, AND THE SOURCE SCAN
==========================================
The JVM frontend records a call site only when it can resolve the callee
statically. A call through an interface or to an overridden method is not
resolved, so on real bugs the inverted index returns NOTHING for the seed:
`PolygonsSet.computeGeometricalProperties()`, `Axis.drawLabel(...)` and
`SimplexSolver.getPivotRow(...)` all come back with an empty caller ring
even though the project plainly calls them.

So when `source_root` is given and the graph yields no caller at all for a
seed, the caller ring for that seed is recovered from the SOURCE TEXT
instead: every `.java` file under `source_root` is parsed, and a method
whose body contains a call written `<seed name>(` with the seed's number
of arguments becomes a caller. This is a TEXTUAL match — it has no types,
so a call to a same-named, same-arity method on an unrelated class counts
too (over-approximation, described in the package README). Members found
this way are tagged 'source-scan' in `MethodSet.provenance`, the ones the
graph produced are tagged 'introspector', and every count that uses the
caller ring can be split by the two.
"""
from __future__ import annotations

import os
import re
from typing import Dict, List, Optional, Tuple

import config
from java.bug_context.call_graph import bfs_callees_with_edges, function_map
from java.measurements.locations import (CALLEE, CALLER, SEED, MethodIndex,
                                         MethodRef, MethodSet,
                                         from_introspector, from_javalang)

#: Values of `MethodSet.provenance`.
PROV_INTROSPECTOR = 'introspector'
PROV_SOURCE_SCAN = 'source-scan'

#: Directory names that hold tests, pruned from the source scan.
TEST_DIRS = ('test', 'tests')

__all__ = ['build', 'seeds_from_context', 'callsite_dst', 'SourceScan',
           'PROV_INTROSPECTOR', 'PROV_SOURCE_SCAN']


# ---------------------------------------------------------------------------
# Graph accessors
# ---------------------------------------------------------------------------

def callsite_dst(cs) -> Optional[str]:
    """The callee's mangled name out of one `base_callsites` entry.

    Introspector versions disagree on the shape: a tuple whose first slot
    is the name, a bare string, or an object with `dst_function_name`.
    Same three cases `call_graph.bfs_callees` handles."""
    if isinstance(cs, (list, tuple)) and cs:
        return cs[0] if isinstance(cs[0], str) else None
    if isinstance(cs, str):
        return cs
    dst = getattr(cs, 'dst_function_name', None)
    return dst if isinstance(dst, str) else None


def _reverse_callsites(fmap: Dict[str, object]) -> Dict[str, List[str]]:
    """callee mangled name -> the mangled names that call it.

    One pass over the whole project. Callers of one method are then a
    dictionary lookup rather than another sweep per seed."""
    rev: Dict[str, List[str]] = {}
    for caller in fmap:
        prof = fmap[caller]
        for cs in (getattr(prof, 'base_callsites', None) or []):
            dst = callsite_dst(cs)
            if not dst:
                continue
            bucket = rev.setdefault(dst, [])
            if caller not in bucket:
                bucket.append(caller)
    return rev


# ---------------------------------------------------------------------------
# Seeds
# ---------------------------------------------------------------------------

def seeds_from_context(ctx_dict: dict) -> List[MethodRef]:
    """The methods a patch touched, out of a `PatchContext.as_dict()` dict.

    Each entry of `functions` carries two spellings of the same method:
    `fi_name`, the mangled name introspector resolved it to (absent when
    the lookup failed), and the AST's own class / name / parameter types.
    The mangled name is preferred because that is the identity the call
    graph is keyed by; the AST spelling is the fallback.

    Duplicates are dropped, order is the order the context lists them."""
    out: List[MethodRef] = []
    for fn in (ctx_dict.get('functions') or []):
        if not isinstance(fn, dict):
            continue
        ref = None
        fi_name = fn.get('fi_name')
        if fi_name:
            ref = from_introspector(fi_name)
        if ref is None:
            name = fn.get('func_name') or ''
            if not name:
                continue
            cls = fn.get('func_class_fq') or fn.get('func_class') or ''
            ref = from_javalang(cls, name, fn.get('func_param_types') or [])
        if ref is not None and ref not in out:
            out.append(ref)
    return out



# ---------------------------------------------------------------------------
# The source-text caller scan (the fallback for an empty caller ring)
# ---------------------------------------------------------------------------

def _skip_literal(source: str, i: int) -> int:
    """Index just past the string or character literal starting at `i`."""
    quote = source[i]
    i += 1
    n = len(source)
    while i < n:
        if source[i] == '\\':
            i += 2
            continue
        if source[i] == quote:
            return i + 1
        i += 1
    return i


def call_arity(source: str, open_idx: int) -> Optional[int]:
    """Number of top-level arguments of the call whose '(' is at `open_idx`.

    Counts the commas that are not nested inside another bracket, skipping
    string and character literals and comments. `None` when the bracket
    never closes. A generic type argument written out in an argument
    (``f(new HashMap<String, Integer>())``) has a comma of its own and
    inflates the count — one more reason the scan is an approximation."""
    depth = 0
    args = 0
    content = False
    i, n = open_idx, len(source)
    while i < n:
        ch = source[i]
        if ch == '/' and i + 1 < n and source[i + 1] == '/':
            j = source.find('\n', i)
            i = n if j < 0 else j + 1
            continue
        if ch == '/' and i + 1 < n and source[i + 1] == '*':
            j = source.find('*/', i + 2)
            i = n if j < 0 else j + 2
            continue
        if ch in '"\'':
            i = _skip_literal(source, i)
            content = True
            continue
        if ch in '([{':
            depth += 1
        elif ch in ')]}':
            depth -= 1
            if depth == 0:
                return args + (1 if content else 0)
        elif ch == ',' and depth == 1:
            args += 1
        elif not ch.isspace():
            content = True
        i += 1
    return None


def _same_method(ref: MethodRef, seed: MethodRef) -> bool:
    """Is `ref` the seed itself? Classes are compared on the simple name
    too, because a seed may carry only a simple class name."""
    if ref.name != seed.name or len(ref.params) != len(seed.params):
        return False
    return (ref.class_fq == seed.class_fq
            or ref.class_simple == seed.class_simple)


class SourceScan:
    """Every method declared under a source root, ready to be asked "which
    of you contains a call to X?".

    The checkout is parsed ONCE (with `execution.diffcov.method_declarations`,
    the same declaration spans `patch_derived.lines_for` matches against) and
    reused for every seed. Directories named `test`/`tests` are pruned and
    classes whose simple name ends in `Test` are skipped: a test calling the
    seed is not part of the library's caller ring.

    Files that do not parse are skipped; a broken file costs the methods in
    it and nothing else."""

    def __init__(self, source_root: str):
        from java.execution import diffcov
        self.source_root = source_root
        # (source text, [(body_start, body_end, MethodRef), ...])
        self.files: List[Tuple[str, List[Tuple[int, int, MethodRef]]]] = []
        for dirpath, dirnames, filenames in os.walk(source_root):
            dirnames[:] = sorted(d for d in dirnames
                                 if d.lower() not in TEST_DIRS)
            for fname in sorted(filenames):
                if not fname.endswith('.java'):
                    continue
                if fname[:-5].endswith('Test'):
                    continue
                full = os.path.join(dirpath, fname)
                try:
                    with open(full, encoding='utf-8', errors='replace') as fh:
                        source = fh.read()
                except OSError:
                    continue
                try:
                    decls = diffcov.method_declarations(source)
                except Exception:                      # noqa: BLE001
                    continue
                spans: List[Tuple[int, int, MethodRef]] = []
                for d in decls:
                    cls = d.get('class_name') or ''
                    if cls.rsplit('.', 1)[-1].endswith('Test'):
                        continue
                    body = d.get('insert_offset')
                    if body is None:
                        # No body (abstract or interface method): it can
                        # contain no call.
                        continue
                    spans.append((body, d['end'],
                                  from_javalang(cls, d['name'],
                                                d.get('param_types') or [])))
                if spans:
                    self.files.append((source, spans))

    @staticmethod
    def _pattern(seed: MethodRef):
        """The call as it is written in source. A constructor seed is
        called as `new Class(`; anything else as `name(`, with any
        receiver in front of it."""
        if seed.name == '<init>':
            token = seed.class_simple
            prefix = r'(?<![\w$])new\s+(?:[\w$]+\s*\.\s*)*'
        else:
            token = seed.name
            prefix = r'(?<![\w$])'
        if not token or not token.isidentifier():
            return None
        return re.compile(prefix + re.escape(token) + r'\s*\(')

    def _enclosing(self, spans, offset: int) -> Optional[MethodRef]:
        """The innermost method body containing `offset` (an anonymous
        class's method sits inside its enclosing method, so the smallest
        span wins)."""
        best = None
        for start, end, ref in spans:
            if start <= offset <= end:
                if best is None or (end - start) < (best[1] - best[0]):
                    best = (start, end, ref)
        return best[2] if best else None

    def callers_of(self, seed: MethodRef, cap: Optional[int] = None
                   ) -> List[MethodRef]:
        """The methods whose body writes a call to `seed`, sorted, capped.

        Matching is on the call's NAME and its number of arguments only:
        the text does not say what the receiver's type is, so a same-named,
        same-arity method on another class is counted too. The seed's own
        body never counts, so a recursive call adds nothing."""
        pat = self._pattern(seed)
        if pat is None:
            return []
        arity = len(seed.params)
        found: set = set()
        for source, spans in self.files:
            for m in pat.finditer(source):
                open_idx = source.index('(', m.end() - 1)
                if call_arity(source, open_idx) != arity:
                    continue
                ref = self._enclosing(spans, m.start())
                if ref is None or _same_method(ref, seed):
                    continue
                found.add(ref)
        out = sorted(found)
        return out if cap is None else out[:cap]


# ---------------------------------------------------------------------------
# The construction
# ---------------------------------------------------------------------------

def build(seeds: List[MethodRef], project, *, caller_cap: Optional[int] = None,
          callee_cap: Optional[int] = None,
          callee_depth: Optional[int] = None,
          source_root: Optional[str] = None) -> MethodSet:
    """Seeds + their callers + their bounded callee walk, as one MethodSet.

    `seeds` are refs from wherever the patch was parsed (a mangled name, a
    Java AST, a diff). `project` is a fuzz-introspector project object, the
    same one `call_graph.function_map` takes.

    Matching: every function name in the project is converted to a
    `MethodRef` once, and each seed is looked up against that population
    with `MethodIndex` — exact when both sides are fully qualified,
    otherwise by (simple class, method name, arity). A seed that matches is
    recorded under the PROJECT's spelling of it, so the seed and the edges
    around it share one identity. A seed that matches nothing is still
    added (ring SEED) and its name is listed in `MethodSet.unmatched`, so a
    failed lookup can never be read as "this method has no neighbourhood".

    Rings: seeds are depth 0; callers are ring CALLER at depth 1; a callee
    is ring CALLEE at its BFS depth. A method reached two ways keeps the
    nearest ring and records the other in `MethodSet.multi` (see
    locations.MethodSet.add).

    Edges: every call site the walk looked at, plus one edge per caller
    into the seed it calls, as (caller_ref, callee_ref) pairs. Names that
    do not parse as introspector names are dropped from the edge list and
    recorded in `unmatched`.

    `source_root`: a checkout of the code the graph was built from. When it
    is given and the graph produced NO caller for a seed, that seed's
    caller ring is scanned out of the source text instead (see the module
    docstring and `SourceScan`); those members are tagged 'source-scan' in
    `MethodSet.provenance` and are capped exactly as graph callers are.
    Without it a seed the frontend could not resolve simply keeps an empty
    caller ring, which is what happened before this fallback existed."""
    caller_cap = config.MAX_XREFS_PER_FUNCTION if caller_cap is None else caller_cap
    callee_cap = config.REACHABLE_NODE_CAP if callee_cap is None else callee_cap
    callee_depth = (config.REACHABLE_MAX_DEPTH if callee_depth is None
                    else callee_depth)

    fmap = function_map(project)
    ms = MethodSet()

    # Project population, and the way back from a ref to its mangled name.
    name_of: Dict[MethodRef, str] = {}
    population: List[MethodRef] = []
    for name in sorted(fmap):
        ref = from_introspector(name)
        if ref is None:
            continue
        population.append(ref)
        name_of.setdefault(ref, name)
    index = MethodIndex(population)

    # -- seeds ---------------------------------------------------------
    resolved: List[Tuple[MethodRef, Optional[str]]] = []
    for seed in seeds:
        hit = index.lookup(seed)
        if hit is None:
            ms.add(seed, SEED, 0, provenance=PROV_INTROSPECTOR)
            if str(seed) not in ms.unmatched:
                ms.unmatched.append(str(seed))
            resolved.append((seed, None))
        else:
            ms.add(hit, SEED, 0, provenance=PROV_INTROSPECTOR)
            resolved.append((hit, name_of.get(hit)))

    unresolvable: List[str] = []
    edge_seen = set()

    def _edge(a: MethodRef, b: MethodRef) -> None:
        if (a, b) not in edge_seen:
            edge_seen.add((a, b))
            ms.edges.append((a, b))

    def _ref(name: str) -> Optional[MethodRef]:
        """Mangled name -> ref, remembering the ones that do not parse."""
        r = from_introspector(name)
        if r is None and name not in unresolvable:
            unresolvable.append(name)
        return r

    # -- callers (one level up, capped, sorted for determinism) --------
    reverse = _reverse_callsites(fmap)
    no_callers: List[MethodRef] = []
    for seed_ref, mangled in resolved:
        found = 0
        names = ([] if mangled is None
                 else sorted(set(reverse.get(mangled, [])))[:caller_cap])
        for caller in names:
            cref = _ref(caller)
            if cref is None:
                continue
            ms.add(cref, CALLER, 1, provenance=PROV_INTROSPECTOR)
            _edge(cref, seed_ref)
            found += 1
        if not found:
            no_callers.append(seed_ref)

    # -- callers the frontend could not resolve, read out of the source --
    if source_root and no_callers:
        try:
            scan = SourceScan(source_root)
        except Exception:                              # noqa: BLE001
            scan = None                                # measurement: fail soft
        if scan is not None:
            for seed_ref in no_callers:
                for cref in scan.callers_of(seed_ref, caller_cap):
                    ms.add(cref, CALLER, 1, provenance=PROV_SOURCE_SCAN)
                    _edge(cref, seed_ref)

    # -- callees (bounded BFS down) ------------------------------------
    for seed_ref, mangled in resolved:
        if mangled is None:
            continue
        names, edges, depths = bfs_callees_with_edges(
            fmap, mangled, callee_cap, callee_depth)
        for name in names:
            cref = _ref(name)
            if cref is None:
                continue
            ms.add(cref, CALLEE, depths.get(name, 1),
                   provenance=PROV_INTROSPECTOR)
        for src, dst in edges:
            a, b = _ref(src), _ref(dst)
            if a is None or b is None:
                continue
            _edge(a, b)

    for name in unresolvable:
        if name not in ms.unmatched:
            ms.unmatched.append(name)
    return ms
