"""Fences for `java.measurements.neighbourhood` — the call-graph walk.

The patch-derived set P and the developer-fix set R-hat are the SAME
construction over different seeds, so everything the two comparisons rest
on lives in `build`: which methods land in which ring, how far the walk
goes, and what it does when a seed does not exist in the call graph.

What is pinned here:

  * the three rings — seeds at depth 0, callers at depth 1, callees at
    their BFS depth;
  * both bounds actually bind: the node cap truncates the walk and the
    depth bound stops it going deeper, so a hub method can never hang it;
  * the caller cap keeps at most N callers per seed, chosen in sorted
    order, so two runs over the same project pick the same N;
  * every edge the walk crossed is recorded, and both ends of an edge are
    always members of the set;
  * a seed that matches no project function is still a seed, and its name
    is listed in `unmatched` — a failed lookup must not read as "this
    method has no neighbourhood";
  * a seed spelled loosely (simple class name, or types the JVM frontend
    mis-typed) still finds its project function.

The project is a stub: introspector's real object is a bag of function
profiles with a mangled `.name` and a `.base_callsites` list, and that is
all `function_map` and the walk ever touch.
"""
import config
import pytest

from java.measurements.locations import (CALLEE, CALLER, SEED, MethodRef,
                                         from_introspector)
from java.measurements import neighbourhood


# ---------------------------------------------------------------------------
# A stand-in for a fuzz-introspector project
# ---------------------------------------------------------------------------

class FakeFunction:
    """One introspector function profile: its mangled name and the names
    it calls. `base_callsites` entries are tuples here; the real object
    also uses bare strings and objects with `dst_function_name`, and
    `callsite_dst` covers all three."""

    def __init__(self, name, callees=()):
        self.name = name
        self.base_callsites = [(c, 0) for c in callees]


class FakeProject:
    def __init__(self, functions):
        self.all_functions = list(functions)


def m(cls, name, *params):
    """'[pkg.Cls].name(A,B)' the way introspector spells it."""
    return f"[{cls}].{name}({','.join(params)})"


PKG = 'com.example'
A_ROOT = m(f'{PKG}.A', 'root')
A_MID = m(f'{PKG}.A', 'mid', 'int')
B_LEAF = m(f'{PKG}.B', 'leaf')
C_DEEP = m(f'{PKG}.C', 'deep')
D_DEEPER = m(f'{PKG}.D', 'deeper')
CALLER1 = m(f'{PKG}.Top', 'one')
CALLER2 = m(f'{PKG}.Top', 'two')
CALLER3 = m(f'{PKG}.Top', 'three')


def chain_project():
    """root -> mid -> leaf -> deep -> deeper, with three callers of root."""
    return FakeProject([
        FakeFunction(A_ROOT, [A_MID]),
        FakeFunction(A_MID, [B_LEAF]),
        FakeFunction(B_LEAF, [C_DEEP]),
        FakeFunction(C_DEEP, [D_DEEPER]),
        FakeFunction(D_DEEPER, []),
        FakeFunction(CALLER2, [A_ROOT]),
        FakeFunction(CALLER3, [A_ROOT]),
        FakeFunction(CALLER1, [A_ROOT]),
    ])


def ref(mangled):
    r = from_introspector(mangled)
    assert r is not None, mangled
    return r


# ---------------------------------------------------------------------------
# Rings
# ---------------------------------------------------------------------------

def test_seed_is_ring_seed_at_depth_zero():
    ms = neighbourhood.build([ref(A_ROOT)], chain_project())
    assert ms.ring_of(ref(A_ROOT)) == SEED
    assert ms.items[ref(A_ROOT)].depth == 0
    assert ms.unmatched == []


def test_callees_carry_their_bfs_depth():
    ms = neighbourhood.build([ref(A_ROOT)], chain_project(),
                             callee_depth=4, callee_cap=50)
    assert ms.items[ref(A_MID)].depth == 1
    assert ms.items[ref(B_LEAF)].depth == 2
    assert ms.items[ref(C_DEEP)].depth == 3
    assert ms.ring_of(ref(C_DEEP)) == CALLEE


def test_callers_are_ring_caller_at_depth_one():
    ms = neighbourhood.build([ref(A_ROOT)], chain_project())
    callers = set(ms.refs(CALLER))
    assert callers == {ref(CALLER1), ref(CALLER2), ref(CALLER3)}
    assert all(ms.items[c].depth == 1 for c in callers)


# ---------------------------------------------------------------------------
# Bounds
# ---------------------------------------------------------------------------

def test_depth_bound_stops_the_walk():
    """depth 2 reaches mid and leaf; deep is one step too far."""
    ms = neighbourhood.build([ref(A_ROOT)], chain_project(),
                             callee_depth=2, callee_cap=50)
    assert ref(A_MID) in ms
    assert ref(B_LEAF) in ms
    assert ref(C_DEEP) not in ms
    assert ref(D_DEEPER) not in ms


def test_node_cap_truncates_the_walk():
    ms = neighbourhood.build([ref(A_ROOT)], chain_project(),
                             callee_depth=9, callee_cap=2)
    assert len(ms.refs(CALLEE)) == 2
    assert ref(A_MID) in ms and ref(B_LEAF) in ms


def test_a_cycle_terminates():
    """Two methods that call each other: the walk must stop, not spin.

    A.mid is both A.root's callee and its caller, so it qualifies for two
    rings; the nearer one (caller) is the tag it keeps and the other is
    recorded in `multi`."""
    proj = FakeProject([FakeFunction(A_ROOT, [A_MID]),
                        FakeFunction(A_MID, [A_ROOT])])
    ms = neighbourhood.build([ref(A_ROOT)], proj,
                             callee_depth=9, callee_cap=100)
    assert set(ms.items) == {ref(A_ROOT), ref(A_MID)}
    assert ms.ring_of(ref(A_ROOT)) == SEED
    assert ms.ring_of(ref(A_MID)) == CALLER
    assert CALLEE in ms.multi[ref(A_MID)]


def test_caller_cap_keeps_the_first_n_in_sorted_order():
    ms = neighbourhood.build([ref(A_ROOT)], chain_project(), caller_cap=2)
    # sorted by mangled name: '...one', '...three', '...two'
    assert set(ms.refs(CALLER)) == {ref(CALLER1), ref(CALLER3)}


def test_caller_cap_defaults_to_config():
    assert config.MAX_XREFS_PER_FUNCTION >= 3
    ms = neighbourhood.build([ref(A_ROOT)], chain_project())
    assert len(ms.refs(CALLER)) == 3


def test_callee_defaults_come_from_config():
    """No explicit bounds -> the pipeline's own caps, not something local."""
    ms = neighbourhood.build([ref(A_ROOT)], chain_project())
    reached = len(ms.refs(CALLEE))
    assert reached == min(config.REACHABLE_NODE_CAP,
                          config.REACHABLE_MAX_DEPTH)


# ---------------------------------------------------------------------------
# Edges
# ---------------------------------------------------------------------------

def test_edges_record_the_walk_and_the_callers():
    ms = neighbourhood.build([ref(A_ROOT)], chain_project(),
                             callee_depth=2, callee_cap=50, caller_cap=5)
    assert (ref(A_ROOT), ref(A_MID)) in ms.edges
    assert (ref(A_MID), ref(B_LEAF)) in ms.edges
    assert (ref(CALLER1), ref(A_ROOT)) in ms.edges


def test_every_edge_endpoint_is_in_the_set():
    ms = neighbourhood.build([ref(A_ROOT)], chain_project(),
                             callee_depth=3, callee_cap=50)
    for a, b in ms.edges:
        assert a in ms and b in ms


def test_edges_are_deduplicated():
    """The same call made twice is one edge."""
    proj = FakeProject([FakeFunction(A_ROOT, [A_MID, A_MID]),
                        FakeFunction(A_MID, [])])
    ms = neighbourhood.build([ref(A_ROOT)], proj)
    assert ms.edges.count((ref(A_ROOT), ref(A_MID))) == 1


# ---------------------------------------------------------------------------
# Matching seeds against the project
# ---------------------------------------------------------------------------

def test_unmatched_seed_is_still_a_seed_and_is_reported():
    ghost = MethodRef('com.example.Nowhere', 'gone', ())
    ms = neighbourhood.build([ghost], chain_project())
    assert ms.ring_of(ghost) == SEED
    assert ms.unmatched == [ghost.strict_key]
    # It has no neighbourhood, and that is different from having none found.
    assert ms.refs(CALLEE) == []


def test_loose_seed_finds_the_project_function():
    """A seed with only a simple class name still resolves, and is
    recorded under the project's fully-qualified spelling."""
    loose = MethodRef('A', 'mid', ('int',))
    ms = neighbourhood.build([loose], chain_project())
    assert ms.ring_of(ref(A_MID)) == SEED
    assert loose not in ms
    assert ms.unmatched == []


def test_two_seeds_share_one_set():
    ms = neighbourhood.build([ref(A_ROOT), ref(B_LEAF)], chain_project(),
                             callee_depth=1, callee_cap=50)
    assert ms.ring_of(ref(A_ROOT)) == SEED
    assert ms.ring_of(ref(B_LEAF)) == SEED
    # B.leaf is a seed even though A.root's walk would also reach it, and
    # the seed ring wins.
    assert ms.ring_of(ref(C_DEEP)) == CALLEE


def test_seed_reached_as_a_callee_keeps_seed_and_records_the_other_ring():
    ms = neighbourhood.build([ref(A_ROOT), ref(A_MID)], chain_project(),
                             callee_depth=2, callee_cap=50)
    assert ms.ring_of(ref(A_MID)) == SEED
    assert CALLEE in ms.multi.get(ref(A_MID), [])


# ---------------------------------------------------------------------------
# Callsite shapes and seeds_from_context
# ---------------------------------------------------------------------------

class DstObject:
    def __init__(self, name):
        self.dst_function_name = name


@pytest.mark.parametrize('callsite', [
    B_LEAF,                     # bare string
    (B_LEAF, 12),               # tuple
    [B_LEAF, 12],               # list
    DstObject(B_LEAF),          # object with dst_function_name
])
def test_all_three_callsite_shapes_are_understood(callsite):
    fn = FakeFunction(A_ROOT)
    fn.base_callsites = [callsite]
    proj = FakeProject([fn, FakeFunction(B_LEAF, [])])
    ms = neighbourhood.build([ref(A_ROOT)], proj)
    assert ms.ring_of(ref(B_LEAF)) == CALLEE


def test_seeds_from_context_prefers_the_mangled_name():
    ctx = {'functions': [{'fi_name': A_MID, 'func_name': 'mid',
                          'func_class_fq': 'com.example.A',
                          'func_param_types': ['int']}]}
    assert neighbourhood.seeds_from_context(ctx) == [ref(A_MID)]


def test_seeds_from_context_falls_back_to_the_ast_spelling():
    ctx = {'functions': [{'fi_name': None, 'func_name': 'mid',
                          'func_class_fq': 'com.example.A',
                          'func_param_types': ['int']}]}
    assert neighbourhood.seeds_from_context(ctx) == [
        MethodRef('com.example.A', 'mid', ('int',))]


def test_seeds_from_context_names_a_constructor_init():
    ctx = {'functions': [{'func_name': 'A', 'func_class_fq': 'com.example.A',
                          'func_param_types': []}]}
    assert neighbourhood.seeds_from_context(ctx) == [
        MethodRef('com.example.A', '<init>', ())]


def test_seeds_from_context_deduplicates():
    entry = {'fi_name': A_MID}
    assert len(neighbourhood.seeds_from_context(
        {'functions': [entry, dict(entry)]})) == 1


def test_empty_project_yields_only_unmatched_seeds():
    ms = neighbourhood.build([ref(A_ROOT)], FakeProject([]))
    assert ms.refs(SEED) == [ref(A_ROOT)]
    assert ms.unmatched == [ref(A_ROOT).strict_key]
    assert ms.edges == []
