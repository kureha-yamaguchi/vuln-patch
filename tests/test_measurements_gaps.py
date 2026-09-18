"""The two measurement-side gaps found on the first real measurement run.

Both are about NAMES, not about the pipeline: fuzz-introspector's JVM
frontend gets the caller ring and the receiver class wrong on real code,
and the measurement layer has to compensate without ever changing what the
pipeline did.

GAP 1 — the empty caller ring.  The frontend records a call site only when
it resolves the callee statically, so a virtual or interface call is not
recorded at all.  On the smoke run every seed came back with an empty
caller ring (`PolygonsSet.computeGeometricalProperties()`,
`Axis.drawLabel(...)`, `SimplexSolver.getPivotRow(...)`), which reads as
"nothing in the project calls this method" and is simply false.  The
fallback reads the caller ring out of the SOURCE TEXT instead, and every
member records which of the two found it (`MethodSet.provenance`).

GAP 2 — the mislabelled receiver.  The frontend labels a call with the
class it was reading rather than the class that declares the callee, so
`Rectangle2D.getCenterX()` inside `Axis.drawLabel` is written
`[org.jfree.chart.axis.Axis].getCenterX()`.  That looks like a project
method of a project class, so the JDK filter cannot see it.  Two answers,
both population-aware: `MethodIndex.lookup` rescues the ones that ARE real
library methods under another class, and `metrics._strip_mislabelled`
drops the ones that are JDK accessors the labelled class does not have.

The refs in section 2 are the ones the Chart-26 leg of
`runs-archive/runs/measure_smoke_20260904_075108` actually produced.
"""
import os

from java.measurements import locations as loc
from java.measurements import metrics as M
from java.measurements import neighbourhood as N

from test_measurements_metrics import _cov, _lset, _mset, _write_leg
from test_measurements_neighbourhood import FakeFunction, FakeProject, m


# ===========================================================================
# 1. GAP 1 — the source-scan caller ring
# ===========================================================================

SEED = loc.MethodRef('org.ex.Target', 'boom', ('String', 'int'))

TARGET_JAVA = """package org.ex;

public class Target {

    public void boom(String s, int n) {
        // recursive: the seed's OWN body is not a caller of the seed
        boom(s, n);
    }

    public void near() {
        boom("x", 1);
    }

    public void wrongArity() {
        boom("x");
    }
}
"""

OTHER_JAVA = """package org.ex;

public class Other {

    public void far() {
        new Target().boom("y", 2);
    }
}
"""

TEST_DIR_JAVA = """package org.ex;

public class InATestDirectory {

    public void exercise() {
        new Target().boom("z", 3);
    }
}
"""

TEST_CLASS_JAVA = """package org.ex;

public class TargetTest {

    public void testBoom() {
        new Target().boom("w", 4);
    }
}
"""

NEAR = loc.MethodRef('org.ex.Target', 'near', ())
FAR = loc.MethodRef('org.ex.Other', 'far', ())


def _checkout(tmp_path):
    """A tiny source tree: two callers of the seed in the library, one
    caller under a `test/` directory and one in a `*Test` class."""
    root = tmp_path / 'checkout'
    main = root / 'src' / 'org' / 'ex'
    tests = root / 'src' / 'test' / 'org' / 'ex'
    main.mkdir(parents=True)
    tests.mkdir(parents=True)
    (main / 'Target.java').write_text(TARGET_JAVA)
    (main / 'Other.java').write_text(OTHER_JAVA)
    (main / 'TargetTest.java').write_text(TEST_CLASS_JAVA)
    (tests / 'InATestDirectory.java').write_text(TEST_DIR_JAVA)
    return str(root)


def test_source_scan_finds_the_callers_the_call_graph_missed(tmp_path):
    """Two callers, and only two: the seed's own recursive call, the
    same-name call with the wrong arity, the caller under `test/` and the
    caller in a `*Test` class are all left out."""
    scan = N.SourceScan(_checkout(tmp_path))
    assert scan.callers_of(SEED) == sorted([FAR, NEAR])


def test_source_scan_respects_the_caller_cap(tmp_path):
    """The cap is the same `MAX_XREFS_PER_FUNCTION` the graph callers use,
    and the order is sorted, so two runs keep the same one."""
    scan = N.SourceScan(_checkout(tmp_path))
    assert scan.callers_of(SEED, 1) == [FAR]
    assert scan.callers_of(SEED, 0) == []


def test_source_scan_arity_is_counted_from_the_call_text():
    """Nested calls, literals and comments must not be read as argument
    separators; a generic comma is the known over-count."""
    assert N.call_arity('f()', 1) == 0
    assert N.call_arity('f(a)', 1) == 1
    assert N.call_arity('f(g(a, b), "x,y")', 1) == 2
    assert N.call_arity('f(a /* , */ , b)', 1) == 2
    assert N.call_arity('f(a', 1) is None


def test_build_uses_the_source_scan_only_when_the_graph_found_nothing(
        tmp_path):
    """The whole point of GAP 1: with a project whose `base_callsites`
    resolve nothing, `build` still produces a caller ring, and says the
    source scan produced it."""
    project = FakeProject([FakeFunction(m('org.ex.Target', 'boom',
                                          'String', 'int')),
                           FakeFunction(m('org.ex.Other', 'far'))])
    ms = N.build([SEED], project, source_root=_checkout(tmp_path))

    assert sorted(ms.refs(loc.CALLER)) == sorted([FAR, NEAR])
    assert ms.provenance_of(NEAR) == N.PROV_SOURCE_SCAN
    assert ms.provenance_of(FAR) == N.PROV_SOURCE_SCAN
    assert ms.provenance_of(SEED) == N.PROV_INTROSPECTOR
    # both ends of every recovered edge are members of the set
    for a, b in ms.edges:
        assert a in ms and b in ms


def test_build_without_a_source_root_keeps_the_empty_ring(tmp_path):
    """No `source_root`, no fallback — the behaviour every archived run
    was measured with."""
    project = FakeProject([FakeFunction(m('org.ex.Target', 'boom',
                                          'String', 'int'))])
    assert N.build([SEED], project).refs(loc.CALLER) == []


def test_build_leaves_a_resolved_caller_ring_alone(tmp_path):
    """A seed the graph DID resolve keeps the graph's callers and is never
    scanned, so the scan can never widen a ring that already exists."""
    caller = m('org.ex.Graph', 'callsBoom')
    project = FakeProject([
        FakeFunction(m('org.ex.Target', 'boom', 'String', 'int')),
        FakeFunction(caller, [m('org.ex.Target', 'boom', 'String', 'int')]),
    ])
    ms = N.build([SEED], project, source_root=_checkout(tmp_path))

    graph_caller = loc.MethodRef('org.ex.Graph', 'callsBoom', ())
    assert ms.refs(loc.CALLER) == [graph_caller]
    assert ms.provenance_of(graph_caller) == N.PROV_INTROSPECTOR
    assert NEAR not in ms and FAR not in ms


def test_build_caps_the_scanned_ring_like_the_graph_ring(tmp_path):
    project = FakeProject([FakeFunction(m('org.ex.Target', 'boom',
                                          'String', 'int'))])
    ms = N.build([SEED], project, source_root=_checkout(tmp_path),
                 caller_cap=1)
    assert ms.refs(loc.CALLER) == [FAR]


# ---------------------------------------------------------------------------
# provenance survives the trip through JSON
# ---------------------------------------------------------------------------

def test_provenance_round_trips_through_the_json_file(tmp_path):
    ms = loc.MethodSet()
    ms.add(SEED, loc.SEED, 0, provenance=N.PROV_INTROSPECTOR)
    ms.add(NEAR, loc.CALLER, 1, provenance=N.PROV_SOURCE_SCAN)
    ms.add(FAR, loc.CALLER, 1, provenance=N.PROV_INTROSPECTOR)

    path = str(tmp_path / 'patch_derived.json')
    loc.dump(ms, path)
    back = loc.load_method_set(path)

    assert back.provenance == {SEED: N.PROV_INTROSPECTOR,
                               NEAR: N.PROV_SOURCE_SCAN,
                               FAR: N.PROV_INTROSPECTOR}
    assert back.provenance_of(NEAR) == N.PROV_SOURCE_SCAN


def test_a_file_written_before_provenance_existed_reads_as_introspector():
    """Backward compatibility: the archived runs have no such key, and
    their caller rings are all graph-made by definition."""
    old = _mset([(SEED, loc.SEED), (NEAR, loc.CALLER)]).to_dict()
    del old['provenance']

    back = loc.MethodSet.from_dict(old)
    assert back.provenance == {}
    assert back.provenance_of(NEAR) == 'introspector'


def test_the_ring_a_member_is_filed_under_owns_its_provenance():
    """A method found twice keeps the provenance of the nearer ring, so a
    caller count split by provenance can never double-count."""
    ms = loc.MethodSet()
    ms.add(NEAR, loc.CALLEE, 2, provenance=N.PROV_INTROSPECTOR)
    ms.add(NEAR, loc.CALLER, 1, provenance=N.PROV_SOURCE_SCAN)
    assert ms.ring_of(NEAR) == loc.CALLER
    assert ms.provenance_of(NEAR) == N.PROV_SOURCE_SCAN
    # and the losing add does not overwrite the winner
    ms.add(NEAR, loc.CALLEE, 3, provenance=N.PROV_INTROSPECTOR)
    assert ms.provenance_of(NEAR) == N.PROV_SOURCE_SCAN


# ===========================================================================
# 2. GAP 2 — the mislabelled receiver
# ===========================================================================

# The population a coverage report gives: what the library really declares.
STD_ADD = loc.MethodRef('org.jfree.chart.entity.StandardEntityCollection',
                        'add', ('ChartEntity',))
AXIS_DRAW = loc.MethodRef('org.jfree.chart.axis.Axis', 'drawLabel',
                          ('String', 'Graphics2D', 'Rectangle2D',
                           'Rectangle2D', 'RectangleEdge', 'AxisState',
                           'PlotRenderingInfo'))
AXIS_FONT = loc.MethodRef('org.jfree.chart.axis.Axis', 'getLabelFont', ())
BLOCK_WIDTH = loc.MethodRef('org.jfree.chart.block.AbstractBlock',
                            'getWidth', ())
SIZE_WIDTH = loc.MethodRef('org.jfree.chart.util.Size2D', 'getWidth', ())

POPULATION = [STD_ADD, AXIS_DRAW, AXIS_FONT, BLOCK_WIDTH, SIZE_WIDTH]

# What the introspector wrote, with the seed's class stuck on the receiver.
AXIS_ADD = loc.MethodRef('org.jfree.chart.entity.EntityCollection', 'add',
                         ('AxisLabelEntity',))
AXIS_CENTER_X = loc.MethodRef('org.jfree.chart.axis.Axis', 'getCenterX', ())
AXIS_SHAPE = loc.MethodRef('org.jfree.chart.axis.Axis',
                           'createTransformedShape', ('Rectangle2D',))
AXIS_WIDTH = loc.MethodRef('org.jfree.chart.axis.Axis', 'getWidth', ())
RECT_FLOAT = loc.MethodRef('Rectangle2D.Float', '<init>',
                           ('Axis', 'Axis', 'Axis', 'Axis'))


def test_lookup_rescues_a_real_method_hiding_under_the_wrong_class():
    """`EntityCollection.add(x)` is a real library call implemented by
    `StandardEntityCollection.add(ChartEntity)`. No class named
    EntityCollection declares an `add` in the population, and `add/1` is
    unique there, so the ref resolves — and the rescue is counted."""
    index = loc.MethodIndex(POPULATION)
    assert index.lookup(AXIS_ADD) == STD_ADD
    assert index.reclassified == 1
    assert index.missing == 0


def test_lookup_leaves_a_non_unique_name_and_arity_unmatched():
    """`getWidth/0` is declared by two classes, so attributing it to
    either would be a guess; it stays unmatched and counts as missing."""
    index = loc.MethodIndex(POPULATION)
    assert index.lookup(AXIS_WIDTH) is None
    assert index.reclassified == 0
    assert index.missing == 1


def test_lookup_does_not_rescue_when_the_labelled_class_has_that_name():
    """The guard. `Axis` really does declare `drawLabel`, so a drawLabel
    ref with a different arity is a mis-parse of Axis's own method, not a
    mislabelled receiver, and must not be re-attributed to another class
    that happens to have the only drawLabel of that arity."""
    other = loc.MethodRef('org.jfree.chart.axis.ValueAxis', 'drawLabel',
                          ('String',))
    index = loc.MethodIndex(POPULATION + [other])
    ref = loc.MethodRef('org.jfree.chart.axis.Axis', 'drawLabel', ('String',))
    assert index.lookup(ref) is None
    assert index.reclassified == 0
    assert index.missing == 1
    # without the guard the unique drawLabel/1 would have been returned
    assert index.by_name_arity['drawLabel/1'] == [other]


def test_lookup_does_not_rescue_a_stack_frame():
    """A frame carries no parameter types, so its arity is 0 by accident:
    rescuing on (name, 0) would attribute a frame to any no-argument
    method of that name anywhere in the project."""
    index = loc.MethodIndex(POPULATION)
    frame = loc.MethodRef('org.jfree.chart.entity.EntityCollection', 'add')
    assert index.lookup(frame, frame=True) is None
    assert index.reclassified == 0


# ---------------------------------------------------------------------------
# metrics._strip_mislabelled
# ---------------------------------------------------------------------------

def test_strip_mislabelled_drops_the_chart26_shapes():
    """The exact members the Chart-26 leg carried in R: three JDK
    accessors wearing `org.jfree.chart.axis.Axis`, next to two members
    that must survive."""
    ms = _mset([(AXIS_DRAW, loc.SEED),
                (AXIS_CENTER_X, loc.CALLEE),
                (AXIS_SHAPE, loc.CALLEE),
                (AXIS_WIDTH, loc.CALLEE),
                (BLOCK_WIDTH, loc.CALLEE),
                (AXIS_FONT, loc.CALLEE)])

    kept, dropped = M._strip_mislabelled(ms, POPULATION)

    assert dropped == 3
    assert set(kept.refs()) == {AXIS_DRAW, BLOCK_WIDTH, AXIS_FONT}
    # `getWidth` is in the name list, but AbstractBlock really declares it
    assert BLOCK_WIDTH in kept
    # and a name outside the list is never touched, population or not
    assert AXIS_FONT in kept


def test_strip_mislabelled_needs_a_population():
    """With no coverage there is no population, so the question cannot be
    asked and nothing is dropped — guessing would delete real methods."""
    ms = _mset([(AXIS_CENTER_X, loc.CALLEE)])
    kept, dropped = M._strip_mislabelled(ms, None)
    assert dropped == 0 and AXIS_CENTER_X in kept
    assert M._strip_mislabelled(None, POPULATION) == (None, 0)


def test_a_mislabelled_constructor_is_left_unmatched_not_reattributed():
    """`Rectangle2D.Float.<init>(Axis,Axis,Axis,Axis)` — the same leg's
    other mislabel — is not an accessor name, so neither pass drops it and
    it keeps doing what it did before: failing to match, and so staying
    out of every denominator. The rescue must NOT take it: '<init>/4'
    names no method, so a unique four-argument constructor elsewhere in
    the project is not evidence of anything."""
    ms = _mset([(AXIS_DRAW, loc.SEED), (RECT_FLOAT, loc.CALLEE)])
    after_jdk, jdk = M._strip_jdk(ms)
    kept, mis = M._strip_mislabelled(after_jdk, POPULATION)
    assert (jdk, mis) == (0, 0)
    assert RECT_FLOAT in kept

    only_ctor = loc.MethodRef('org.jfree.chart.axis.AxisState', '<init>',
                              ('a', 'b', 'c', 'd'))
    index = loc.MethodIndex(POPULATION + [only_ctor])
    assert index.lookup(RECT_FLOAT) is None
    assert index.reclassified == 0


def test_the_strip_passes_keep_provenance():
    """A dropped member takes its provenance entry with it; a survivor
    keeps its own, so the caller-provenance counts stay right."""
    ms = loc.MethodSet()
    ms.add(AXIS_DRAW, loc.SEED, 0, provenance=N.PROV_INTROSPECTOR)
    ms.add(BLOCK_WIDTH, loc.CALLER, 1, provenance=N.PROV_SOURCE_SCAN)
    ms.add(AXIS_CENTER_X, loc.CALLER, 1, provenance=N.PROV_INTROSPECTOR)

    kept, _ = M._strip_mislabelled(ms, POPULATION)
    assert kept.provenance == {AXIS_DRAW: N.PROV_INTROSPECTOR,
                               BLOCK_WIDTH: N.PROV_SOURCE_SCAN}


# ===========================================================================
# 3. What reaches the metrics row
# ===========================================================================

RESULT = {'label': 'overfitting', 'status': 'evaluated',
          'bug_kind': 'crashing', 'project': 'Chart', 'bug_id': '26',
          'apr_tool': 'Jaid', 'crashed_on_patch': True}


def _chart26_leg(run):
    """A leg shaped like the Chart-26 one: R holds the seed plus the three
    mislabelled accessors, and P holds the seed plus a source-scanned
    caller."""
    p = loc.MethodSet()
    p.add(AXIS_DRAW, loc.SEED, 0, provenance=N.PROV_INTROSPECTOR)
    p.add(BLOCK_WIDTH, loc.CALLER, 1, provenance=N.PROV_SOURCE_SCAN)
    p.add(AXIS_FONT, loc.CALLER, 1, provenance=N.PROV_INTROSPECTOR)
    r = _mset([(AXIS_DRAW, loc.SEED), (AXIS_CENTER_X, loc.CALLEE),
               (AXIS_SHAPE, loc.CALLEE), (AXIS_WIDTH, loc.CALLEE)])
    return _write_leg(
        run, '06_patch1-Chart-26-Jaid-plausible_o', RESULT,
        patch_derived=p,
        root_cause={'methods': r.to_dict(), 'lines': _lset([]).to_dict(),
                    'manifest': _mset([]).to_dict()},
        coverage={'buggy': _cov('buggy', [AXIS_DRAW], [], POPULATION)})


def test_compute_leg_counts_the_mislabelled_drops_and_the_provenance(
        tmp_path):
    run = str(tmp_path / 'run')
    os.makedirs(run, exist_ok=True)
    leg = _chart26_leg(run)

    row = M.compute_leg(leg)

    assert row['jdk_dropped']['R_mislabelled'] == 3
    assert row['jdk_dropped']['P_mislabelled'] == 0
    # R is now the seed alone, and the harness covered it
    assert row['sizes']['R_method']['full'] == 1
    assert row['sizes']['P_caller_provenance'] == {'introspector': 1,
                                                   'source-scan': 1}
    assert row['sizes']['R_caller_provenance'] == {'introspector': 0,
                                                   'source-scan': 0}
    assert row['matching']['method__full__buggy']['reclassified'] == 0
    assert row['matching']['method__full__buggy']['p_caller_provenance'] == \
        {'introspector': 1, 'source-scan': 1}
    assert row['rcc__method__full__buggy__dyn']['value'] == 1.0
