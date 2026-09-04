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
"""
from __future__ import annotations

from typing import Dict, List, Optional, Tuple

import config
from java.bug_context.call_graph import bfs_callees_with_edges, function_map
from java.measurements.locations import (CALLEE, CALLER, SEED, MethodIndex,
                                         MethodRef, MethodSet,
                                         from_introspector, from_javalang)

__all__ = ['build', 'seeds_from_context', 'callsite_dst']


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
# The construction
# ---------------------------------------------------------------------------

def build(seeds: List[MethodRef], project, *, caller_cap: Optional[int] = None,
          callee_cap: Optional[int] = None,
          callee_depth: Optional[int] = None) -> MethodSet:
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
    recorded in `unmatched`."""
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
            ms.add(seed, SEED, 0)
            if str(seed) not in ms.unmatched:
                ms.unmatched.append(str(seed))
            resolved.append((seed, None))
        else:
            ms.add(hit, SEED, 0)
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
    for seed_ref, mangled in resolved:
        if mangled is None:
            continue
        for caller in sorted(set(reverse.get(mangled, [])))[:caller_cap]:
            cref = _ref(caller)
            if cref is None:
                continue
            ms.add(cref, CALLER, 1)
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
            ms.add(cref, CALLEE, depths.get(name, 1))
        for src, dst in edges:
            a, b = _ref(src), _ref(dst)
            if a is None or b is None:
                continue
            _edge(a, b)

    for name in unresolvable:
        if name not in ms.unmatched:
            ms.unmatched.append(name)
    return ms
