"""Tests for `java.measurements.root_cause` — the developer-fix reader.

Everything here runs on a synthetic two-class Java tree built in tmp_path
and on diffs generated from it with difflib, so no Defects4J installation is
needed. The three `defects4j` shell-outs the module makes (`export`,
`checkout`, and the test-source-directory lookup) are monkeypatched.
"""
import difflib
import os
import shutil
import subprocess
import sys

import pytest

from java.measurements import locations, root_cause
from java.measurements.locations import CALLER, LineRef, MethodRef, SEED

FIXTURES = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        'fixtures', 'measurements')

WIDGET_REL = 'src/org/example/Widget.java'
GADGET_REL = 'src/org/example/Gadget.java'

WIDGET_FIXED = """package org.example;

import java.util.List;
import java.util.Map;

/** A widget with a size. */
public class Widget {

    private int size;

    private final List<String> tags;

    public Widget(int size, List<String> tags) {
        this.size = size;
        this.tags = tags;
    }

    public int grow(int by) {
        int result = size + by;
        return result;
    }

    public int shrink(int by) {
        return size - by;
    }

    static class Inner {
        int compute(int x) {
            return x * 2;
        }
    }
}
"""

# The buggy revision: the Map import is gone (an import-only hunk that maps
# to no method), the constructor and grow() are off by one, and the nested
# class multiplies by the wrong constant.
WIDGET_BUGGY = WIDGET_FIXED \
    .replace('import java.util.Map;\n', '') \
    .replace('        this.size = size;', '        this.size = size + 1;') \
    .replace('        int result = size + by;',
             '        int result = size + by + 1;') \
    .replace('            return x * 2;', '            return x * 3;')

GADGET_FIXED = """package org.example;

public final class Gadget {

    private final int[] data;

    public Gadget(int[] data) {
        this.data = data;
    }

    public int scale(int factor) {
        return data[factor] * factor;
    }
}
"""

GADGET_BUGGY = GADGET_FIXED.replace('        return data[factor] * factor;',
                                    '        return data[factor - 1] * factor;')


# --- helpers --------------------------------------------------------------

def _write_tree(root, files):
    for rel, text in files.items():
        path = os.path.join(str(root), rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w') as fh:
            fh.write(text)
    return str(root)


def _unified(from_text, to_text, rel):
    """One file's unified diff, with the `a/`,`b/` prefixes Defects4J's
    src.patch files carry."""
    return '\n'.join(difflib.unified_diff(
        from_text.split('\n'), to_text.split('\n'),
        fromfile='a/' + rel, tofile='b/' + rel, lineterm='')) + '\n'


def _forward_patch():
    """The Defects4J direction: fixed -> buggy, so the '+' side is buggy."""
    return (_unified(WIDGET_FIXED, WIDGET_BUGGY, WIDGET_REL)
            + _unified(GADGET_FIXED, GADGET_BUGGY, GADGET_REL))


def _reverse_patch_text():
    """The other direction: buggy -> fixed."""
    return (_unified(WIDGET_BUGGY, WIDGET_FIXED, WIDGET_REL)
            + _unified(GADGET_BUGGY, GADGET_FIXED, GADGET_REL))


@pytest.fixture
def buggy_dir(tmp_path):
    return _write_tree(tmp_path / 'buggy',
                       {WIDGET_REL: WIDGET_BUGGY, GADGET_REL: GADGET_BUGGY})


@pytest.fixture
def fixed_dir(tmp_path):
    return _write_tree(tmp_path / 'fixed',
                       {WIDGET_REL: WIDGET_FIXED, GADGET_REL: GADGET_FIXED})


@pytest.fixture(autouse=True)
def no_defects4j(monkeypatch):
    """No test may shell out to `defects4j`; each test that needs one of the
    shell-outs monkeypatches that helper explicitly."""
    def _boom(*a, **kw):
        raise AssertionError(f'unexpected subprocess call: {a}')
    monkeypatch.setattr(root_cause, '_run', _boom)
    monkeypatch.setattr(root_cause, 'test_source_dirs', lambda d: [])
    monkeypatch.delenv('D4J_HOME', raising=False)


def _capturing_run(cmd, cwd=None, timeout=None):
    """A real subprocess call that captures output — the stand-in for
    `root_cause._run` in the tests that exercise the `diff` fallback."""
    return subprocess.run(list(cmd), cwd=cwd, capture_output=True, text=True)


def _line_of(text, needle):
    for i, line in enumerate(text.split('\n'), start=1):
        if needle in line:
            return i
    raise AssertionError(f'{needle!r} not in the synthetic source')


WIDGET_INIT = MethodRef('org.example.Widget', '<init>', ('int', 'List'))
WIDGET_GROW = MethodRef('org.example.Widget', 'grow', ('int',))
INNER_COMPUTE = MethodRef('org.example.Widget.Inner', 'compute', ('int',))
GADGET_SCALE = MethodRef('org.example.Gadget', 'scale', ('int',))
EXPECTED_SEEDS = {WIDGET_INIT, WIDGET_GROW, INNER_COMPUTE, GADGET_SCALE}


# --- seeds ----------------------------------------------------------------

def test_changed_methods_as_refs_finds_every_changed_method(buggy_dir):
    refs, unmapped = root_cause.changed_methods_as_refs(_forward_patch(),
                                                        buggy_dir)
    assert set(refs) == EXPECTED_SEEDS
    # The constructor is normalised to '<init>', never left as 'Widget'.
    assert all(r.name != 'Widget' for r in refs)
    assert unmapped, 'the import-only hunk should be recorded as unmapped'


def test_import_only_hunk_is_unmapped_not_dropped(buggy_dir):
    _refs, unmapped = root_cause.changed_methods_as_refs(_forward_patch(),
                                                         buggy_dir)
    reasons = {u.get('reason') for u in unmapped}
    assert any('no enclosing method' in (r or '') for r in reasons), reasons
    files = {u.get('file') for u in unmapped}
    assert files == {WIDGET_REL}


def test_seeds_are_deterministic(buggy_dir):
    a, _ = root_cause.changed_methods_as_refs(_forward_patch(), buggy_dir)
    b, _ = root_cause.changed_methods_as_refs(_forward_patch(), buggy_dir)
    assert a == b


# --- direction detection --------------------------------------------------

def test_forward_patch_is_recognised_as_fixed_to_buggy(buggy_dir):
    patch = _forward_patch()
    oriented, direction = root_cause.orient_patch(patch, buggy_dir)
    assert direction == root_cause.DIR_FIXED_TO_BUGGY
    assert oriented == patch


def test_reversed_patch_is_detected_and_swapped(buggy_dir):
    oriented, direction = root_cause.orient_patch(_reverse_patch_text(),
                                                  buggy_dir)
    assert direction == root_cause.DIR_BUGGY_TO_FIXED
    # After the swap the seeds are exactly the forward-patch seeds.
    refs, _ = root_cause.changed_methods_as_refs(oriented, buggy_dir)
    assert set(refs) == EXPECTED_SEEDS


def test_orientation_is_unverified_when_the_tree_does_not_match(tmp_path):
    empty = _write_tree(tmp_path / 'other', {'src/org/example/Nope.java': ''})
    oriented, direction = root_cause.orient_patch(_forward_patch(), empty)
    assert direction == root_cause.DIR_ASSUMED
    assert oriented == _forward_patch()   # kept as-is, per the D4J convention


def test_reverse_patch_round_trips(buggy_dir):
    once = root_cause.reverse_patch(_forward_patch())
    twice = root_cause.reverse_patch(once)
    assert twice == _forward_patch()
    # And the reversed text keeps `a/` on the pre side, `b/` on the post side.
    heads = [l for l in once.split('\n') if l.startswith(('--- ', '+++ '))]
    assert all(l[4:].startswith('a/') for l in heads if l.startswith('--- '))
    assert all(l[4:].startswith('b/') for l in heads if l.startswith('+++ '))


# --- lines ----------------------------------------------------------------

def test_changed_lines_are_buggy_tree_lines_keyed_by_top_level_class(buggy_dir):
    lines = root_cause.changed_lines(_forward_patch(), buggy_dir)
    grow_line = _line_of(WIDGET_BUGGY, 'int result = size + by + 1;')
    init_line = _line_of(WIDGET_BUGGY, 'this.size = size + 1;')
    inner_line = _line_of(WIDGET_BUGGY, 'return x * 3;')
    scale_line = _line_of(GADGET_BUGGY, 'return data[factor - 1] * factor;')
    for ref in (LineRef('org.example.Widget', grow_line),
                LineRef('org.example.Widget', init_line),
                LineRef('org.example.Widget', inner_line),
                LineRef('org.example.Gadget', scale_line)):
        assert ref in lines, ref
        assert lines.ring_of(ref) == SEED
    # Nested-class lines are keyed by the FILE's top-level class.
    assert not any(r.class_top_fq.endswith('Inner') for r in lines.refs())


def test_changed_lines_from_reversed_patch_cover_the_same_edits(buggy_dir):
    """Orienting a reversed patch lands on exactly the same lines.

    Both line mappers decide per CHANGE GROUP (the '+'/'-' run between two
    context lines) whether a deletion is pure, so the order in which a hunk
    lists its '-' and '+' sides — which reversal flips — cannot change the
    result."""
    forward = set(root_cause.changed_lines(_forward_patch(), buggy_dir).refs())
    oriented, _ = root_cause.orient_patch(_reverse_patch_text(), buggy_dir)
    backward = set(root_cause.changed_lines(oriented, buggy_dir).refs())
    edited = {
        LineRef('org.example.Widget',
                _line_of(WIDGET_BUGGY, 'this.size = size + 1;')),
        LineRef('org.example.Widget',
                _line_of(WIDGET_BUGGY, 'int result = size + by + 1;')),
        LineRef('org.example.Widget',
                _line_of(WIDGET_BUGGY, 'return x * 3;')),
        LineRef('org.example.Gadget',
                _line_of(GADGET_BUGGY, 'return data[factor - 1] * factor;')),
    }
    assert edited <= forward
    assert edited <= backward
    assert backward == forward


# --- trigger frames (the manifest set) ------------------------------------

def _install_failing_tests(buggy_dir):
    shutil.copyfile(os.path.join(FIXTURES, 'failing_tests_example.txt'),
                    os.path.join(buggy_dir, 'failing_tests'))


def test_trigger_frames_keeps_project_frames_only(buggy_dir):
    _install_failing_tests(buggy_dir)
    manifest = root_cause.trigger_frames(buggy_dir)
    refs = set(manifest.refs())
    assert refs == {
        MethodRef('org.example.Widget', 'grow'),
        MethodRef('org.example.Widget.Inner', 'compute'),
        MethodRef('org.example.Gadget', 'scale'),
    }
    assert all(manifest.ring_of(r) == SEED for r in refs)
    # JDK / JUnit frames are gone, and so are the test classes themselves.
    assert not any(r.class_fq.startswith(('java.', 'jdk.', 'sun.', 'junit.',
                                          'org.junit.'))
                   for r in refs)
    assert not any(r.class_fq.endswith(('Test', 'Tests')) for r in refs)


def test_trigger_frames_records_dropped_test_classes(buggy_dir):
    _install_failing_tests(buggy_dir)
    manifest = root_cause.trigger_frames(buggy_dir)
    dropped = ' '.join(manifest.unmatched)
    assert 'WidgetTest' in dropped and 'GadgetTests' in dropped


def test_trigger_frames_excludes_classes_under_a_test_source_dir(buggy_dir,
                                                                 tmp_path,
                                                                 monkeypatch):
    """A helper class in the test tree whose name does NOT end in Test is
    still excluded, because `defects4j export -p dir.src.tests` places it."""
    test_root = _write_tree(tmp_path / 'buggy' / 'test',
                            {'org/example/Harness.java': 'package org.example;\n'})
    monkeypatch.setattr(root_cause, 'test_source_dirs', lambda d: [test_root])
    with open(os.path.join(buggy_dir, 'failing_tests'), 'w') as fh:
        fh.write('--- org.example.WidgetTest::testX\n'
                 'java.lang.IllegalStateException: boom\n'
                 '\tat org.example.Harness.drive(Harness.java:7)\n'
                 '\tat org.example.Widget.grow(Widget.java:20)\n')
    refs = set(root_cause.trigger_frames(buggy_dir).refs())
    assert refs == {MethodRef('org.example.Widget', 'grow')}


def test_trigger_frames_absent_file_is_an_empty_set(buggy_dir):
    assert len(root_cause.trigger_frames(buggy_dir)) == 0
    assert root_cause._manifest_source(buggy_dir) == root_cause.MANIFEST_ABSENT


# --- developer_patch routes ----------------------------------------------

def _install_src_patch(tmp_path, project, bug_id, text):
    home = tmp_path / 'd4j'
    patches = home / 'framework' / 'projects' / project / 'patches'
    os.makedirs(str(patches), exist_ok=True)
    with open(os.path.join(str(patches), f'{bug_id}.src.patch'), 'w') as fh:
        fh.write(text)
    return str(home)


def test_developer_patch_route_1_reads_the_src_patch(tmp_path, buggy_dir):
    home = _install_src_patch(tmp_path, 'Widgets', 3, _forward_patch())
    text, route = root_cause.developer_patch('Widgets', 3, buggy_dir,
                                             d4j_home=home)
    assert route == root_cause.ROUTE_SRC_PATCH
    assert text == _forward_patch()


def test_developer_patch_route_1_uses_the_D4J_HOME_env(tmp_path, buggy_dir,
                                                       monkeypatch):
    home = _install_src_patch(tmp_path, 'Widgets', 3, _forward_patch())
    monkeypatch.setenv('D4J_HOME', home)
    _text, route = root_cause.developer_patch('Widgets', 3, buggy_dir)
    assert route == root_cause.ROUTE_SRC_PATCH


def test_developer_patch_route_2_diffs_the_fixed_checkout(buggy_dir, fixed_dir,
                                                          monkeypatch):
    monkeypatch.setattr(root_cause, 'defects4j_home', lambda explicit=None: None)
    monkeypatch.setattr(root_cause, 'export_property',
                        lambda project_dir, prop: 'src')
    monkeypatch.setattr(root_cause, '_run', _capturing_run)
    text, route = root_cause.developer_patch('Widgets', 3, buggy_dir,
                                             fixed_dir=fixed_dir)
    assert route == root_cause.ROUTE_FIXED_DIFF
    # Paths came back project-relative, so the hunks resolve against the
    # buggy checkout exactly like a src.patch does.
    assert '+++ ' + WIDGET_REL in text
    refs, _unmapped = root_cause.changed_methods_as_refs(text, buggy_dir)
    assert set(refs) == EXPECTED_SEEDS


def test_developer_patch_route_2_checks_out_the_fixed_version(buggy_dir,
                                                              fixed_dir,
                                                              monkeypatch):
    calls = []

    def _fake_checkout(project, bug_id, dest):
        calls.append((project, bug_id, dest))
        return fixed_dir

    monkeypatch.setattr(root_cause, 'defects4j_home', lambda explicit=None: None)
    monkeypatch.setattr(root_cause, 'checkout_fixed', _fake_checkout)
    monkeypatch.setattr(root_cause, 'export_property',
                        lambda project_dir, prop: 'src')
    monkeypatch.setattr(root_cause, '_run', _capturing_run)
    _text, route = root_cause.developer_patch('Widgets', 3, buggy_dir)
    assert route == root_cause.ROUTE_FIXED_DIFF
    assert calls and calls[0][0] == 'Widgets' and calls[0][1] == 3


def test_developer_patch_raises_when_no_route_works(buggy_dir, monkeypatch):
    monkeypatch.setattr(root_cause, 'defects4j_home', lambda explicit=None: None)
    monkeypatch.setattr(root_cause, 'checkout_fixed',
                        lambda project, bug_id, dest: None)
    with pytest.raises(RuntimeError):
        root_cause.developer_patch('Widgets', 3, buggy_dir)


# --- compute() ------------------------------------------------------------

class _FakeFunction:
    def __init__(self, name, callees=()):
        self.name = name
        self.base_callsites = [[c, 0] for c in callees]


class _FakeProject:
    """The shape `neighbourhood.build` reads: `all_functions` profiles with
    `.name` (introspector's mangled JVM form) and `.base_callsites`."""

    def __init__(self):
        self.all_functions = [
            _FakeFunction('[org.example.Widget].grow(int)',
                          ['[org.example.Gadget].scale(int)']),
            _FakeFunction('[org.example.Widget].shrink(int)',
                          ['[org.example.Widget].grow(int)']),
            _FakeFunction('[org.example.Gadget].scale(int)', []),
            _FakeFunction('[org.example.Widget].<init>(int,java.util.List)',
                          ['[org.example.Widget].grow(int)']),
            _FakeFunction('[org.example.Widget$Inner].compute(int)', []),
        ]


def _compute(tmp_path, buggy_dir, **kw):
    home = _install_src_patch(tmp_path, 'Widgets', 3, _forward_patch())
    return root_cause.compute('Widgets', 3, buggy_dir, d4j_home=home, **kw)


def test_compute_ties_the_three_sets_together(tmp_path, buggy_dir):
    _install_failing_tests(buggy_dir)
    rc = _compute(tmp_path, buggy_dir)
    assert set(rc.seeds) == EXPECTED_SEEDS
    assert rc.route == root_cause.ROUTE_SRC_PATCH
    assert rc.direction_assumed == root_cause.DIR_FIXED_TO_BUGGY
    assert rc.unmapped_hunks
    assert rc.manifest_source == root_cause.MANIFEST_FILE
    assert MethodRef('org.example.Widget', 'grow') in rc.manifest
    assert len(rc.lines) > 0
    assert rc.patch_text == _forward_patch()


def test_compute_round_trips_through_a_dict(tmp_path, buggy_dir):
    _install_failing_tests(buggy_dir)
    rc = _compute(tmp_path, buggy_dir)
    back = root_cause.RootCause.from_dict(rc.to_dict())
    assert set(back.seeds) == set(rc.seeds)
    assert set(back.lines.refs()) == set(rc.lines.refs())
    assert set(back.manifest.refs()) == set(rc.manifest.refs())
    assert (back.route, back.direction_assumed, back.manifest_source) == \
           (rc.route, rc.direction_assumed, rc.manifest_source)
    assert back.unmapped_hunks == rc.unmapped_hunks
    assert back.patch_text == rc.patch_text


def test_compute_on_a_reversed_patch_still_lands_on_the_buggy_tree(tmp_path,
                                                                   buggy_dir):
    home = _install_src_patch(tmp_path, 'Widgets', 4, _reverse_patch_text())
    rc = root_cause.compute('Widgets', 4, buggy_dir, d4j_home=home)
    assert rc.direction_assumed == root_cause.DIR_BUGGY_TO_FIXED
    assert set(rc.seeds) == EXPECTED_SEEDS


def test_compute_without_a_project_is_seeds_only(tmp_path, buggy_dir):
    rc = _compute(tmp_path, buggy_dir, introspector_project=None)
    assert set(rc.methods.refs()) == set(rc.seeds) == EXPECTED_SEEDS
    assert not rc.methods.refs(CALLER)


def test_compute_falls_back_to_seeds_when_neighbourhood_is_missing(
        tmp_path, buggy_dir, monkeypatch):
    import java.measurements as pkg
    # Both the cached submodule attribute and the sys.modules entry have to
    # go, or `from java.measurements import neighbourhood` is served from
    # the package attribute a previous test set.
    monkeypatch.delattr(pkg, 'neighbourhood', raising=False)
    monkeypatch.setitem(sys.modules, 'java.measurements.neighbourhood', None)
    rc = _compute(tmp_path, buggy_dir, introspector_project=_FakeProject())
    assert set(rc.methods.refs()) == EXPECTED_SEEDS
    assert any('neighbourhood' in n for n in rc.notes)


def test_compute_grows_rings_when_a_project_is_supplied(tmp_path, buggy_dir):
    try:
        from java.measurements import neighbourhood      # noqa: F401
    except ImportError:
        pytest.skip('java.measurements.neighbourhood is not written yet — '
                    'ring assertions cannot run; the seeds-only fallback is '
                    'covered by the test above')
    rc = _compute(tmp_path, buggy_dir, introspector_project=_FakeProject(),
                  caller_cap=20, callee_cap=20, callee_depth=2)
    assert EXPECTED_SEEDS.issubset(set(rc.methods.refs()))
    assert not rc.notes, rc.notes
    assert len(rc.methods) > len(EXPECTED_SEEDS), \
        'a caller ring should have added shrink()/the constructor'
    assert rc.methods.refs(CALLER), 'no method was tagged as a caller'


# --- the locations.py contract this module relies on ----------------------

def test_locations_types_are_used_as_given():
    assert locations.SEED == 'seed' and locations.CALLER == 'caller'
    assert locations.from_javalang('org.example.Widget', 'Widget',
                                   ['int']).name == '<init>'
    ref, line = locations.from_stack_frame(
        '\tat org.example.Widget$Inner.compute(Widget.java:29)')
    assert ref == MethodRef('org.example.Widget.Inner', 'compute')
    assert line == 29


# --- the triggering-test gate ---------------------------------------------

MEASUREMENT_FIXTURES = FIXTURES
JACOCO_XML = os.path.join(FIXTURES, 'jacoco_example.xml')

# The fixture report's library methods: `draw` and the constructor ran,
# `unusedHelper` did not (its probe never fired).
DRAW = MethodRef('org.jfree.demo.Widget', 'draw', ('Graphics2D', 'int'))
HELPER = MethodRef('org.jfree.demo.Widget', 'unusedHelper',
                   ('int[]', 'boolean'))


class _FakeTriggerRun:
    def __init__(self, report, trace):
        self.report = report
        self.trace = trace


@pytest.fixture
def fake_collect(monkeypatch, tmp_path):
    """Stand in for `metrics.collect`, the sibling package's runner.

    `trigger_gate` imports it inside the function, so replacing the two
    functions on the real module is enough: no Defects4J, no JVM."""
    from metrics import collect

    seen = {'trace': ''}

    def _trigger_tests(buggy_dir):
        seen['tests_for'] = buggy_dir
        return ['org.jfree.demo.WidgetTest::testDraw']

    def _trigger_coverage(buggy_dir, out_dir, tests=None):
        seen['out_dir'] = out_dir
        seen['tests'] = tests
        return _FakeTriggerRun(JACOCO_XML, seen['trace'])

    monkeypatch.setattr(collect, 'trigger_tests', _trigger_tests)
    monkeypatch.setattr(collect, 'trigger_coverage', _trigger_coverage)
    return seen


def _rc_with_seeds(*refs):
    rc = root_cause.RootCause()
    for ref in refs:
        rc.methods.add(ref, SEED, 0)
    return rc


def test_gate_passes_when_the_triggering_tests_run_every_seed(fake_collect,
                                                              buggy_dir):
    gate = root_cause.trigger_gate(buggy_dir, _rc_with_seeds(DRAW),
                                   os.path.join(buggy_dir, 'trigger'))
    assert gate['passed'] is True
    assert gate['missed'] == []
    assert gate['tests'] == ['org.jfree.demo.WidgetTest::testDraw']
    assert gate['detail'] == 'all 1 method(s) reached'
    assert gate['reached_size'] == gate['probe_size'] == 4


def test_gate_fails_when_a_seed_was_never_reached(fake_collect, buggy_dir):
    """Without this check a name mismatch or a bad extraction would give
    RCC = 0 on every bug, which reads exactly like a real finding."""
    gate = root_cause.trigger_gate(buggy_dir, _rc_with_seeds(DRAW, HELPER),
                                   os.path.join(buggy_dir, 'trigger'))
    assert gate['passed'] is False
    assert gate['missed'] == [str(HELPER)]
    assert 'did not run' in gate['detail']


def test_gate_uses_the_frames_too(fake_collect, buggy_dir):
    """The probe limitation applies to the triggering test as much as to a
    harness: the test fails BY throwing, so the method it throws out of can
    read as missed from probes alone."""
    fake_collect['trace'] = (
        '--- org.jfree.demo.WidgetTest::testDraw\n'
        'java.lang.IllegalStateException: boom\n'
        '\tat org.jfree.demo.Widget.unusedHelper(Widget.java:41)\n')
    gate = root_cause.trigger_gate(buggy_dir, _rc_with_seeds(DRAW, HELPER),
                                   os.path.join(buggy_dir, 'trigger'))
    assert gate['passed'] is True
    assert gate['frame_added'] == [str(HELPER)]
    assert gate['probe_size'] == 4 and gate['reached_size'] == 5


def test_gate_writes_the_trace_where_the_manifest_reads_it(fake_collect,
                                                           buggy_dir):
    """R̂₁ comes from `<checkout>/failing_tests`, which Defects4J writes only
    once a test has been run — so a fresh checkout has an empty manifest for
    want of a trace.  The gate runs the tests, so it leaves the trace."""
    fake_collect['trace'] = (
        '--- org.jfree.demo.WidgetTest::testDraw\n'
        'java.lang.IllegalStateException: boom\n'
        '\tat org.example.Widget.grow(Widget.java:20)\n')
    rc = _rc_with_seeds(DRAW)
    assert len(rc.manifest) == 0
    rc.gate = root_cause.trigger_gate(buggy_dir, rc,
                                      os.path.join(buggy_dir, 'trigger'))
    written = os.path.join(buggy_dir, root_cause.MANIFEST_FILE)
    assert os.path.isfile(written)
    root_cause.refresh_manifest(rc, buggy_dir)
    assert set(rc.manifest.refs()) == {MethodRef('org.example.Widget', 'grow')}
    assert rc.manifest_source == root_cause.MANIFEST_FILE


def test_gate_on_an_empty_region_fails_without_running_anything(buggy_dir):
    """No seeds, no denominator: the bug leaves the population, and there is
    nothing to spend a `defects4j test` on."""
    gate = root_cause.trigger_gate(buggy_dir, root_cause.RootCause(),
                                   os.path.join(buggy_dir, 'trigger'))
    assert gate['passed'] is False
    assert 'R-hat is empty' in gate['detail']


def test_the_gate_result_round_trips_through_root_cause_json(fake_collect,
                                                             buggy_dir):
    """`metrics.py` reads the gate out of `root_cause.json` as plain JSON,
    so it has to survive the dict on the way in and out."""
    rc = _rc_with_seeds(DRAW)
    rc.gate = root_cause.trigger_gate(buggy_dir, rc,
                                      os.path.join(buggy_dir, 'trigger'))
    d = rc.to_dict()
    assert d['trigger_gate']['passed'] is True
    assert root_cause.RootCause.from_dict(d).gate == rc.gate
    # a file written before the gate existed, and a run that did not ask
    # for it, both read back as "not run" — which is not "failed"
    assert root_cause.RootCause.from_dict({}).gate is None


# --- the population summary ------------------------------------------------

def test_population_check_counts_every_exclusion():
    """A mean is only honest beside the count of what left the population,
    and an unavailable measurement is never a zero."""
    records = [
        {'project': 'Chart', 'bug_id': '5', 'status': 'ok', 'rcc': 1.0},
        {'project': 'Math', 'bug_id': '70', 'status': 'ok', 'rcc': 0.5},
        {'project': 'Lang', 'bug_id': '43', 'status': 'no_harnesses'},
        {'project': 'Lang', 'bug_id': '1', 'status': 'excluded_empty_region'},
        {'project': 'Lang', 'bug_id': '2', 'status': 'excluded_gate_failed'},
        {'project': 'Lang', 'bug_id': '3', 'status': 'infra_error'},
    ]
    summary = root_cause.population_check(records)
    assert summary['n_records'] == 6 and summary['n_scored'] == 2
    assert summary['mean'] == 0.75
    assert list(summary['counts']) == list(root_cause.POPULATION_STATUSES)
    assert summary['counts'] == {'ok': 2, 'excluded_empty_region': 1,
                                 'excluded_gate_failed': 1,
                                 'no_harnesses': 1, 'infra_error': 1}
    assert summary['scored'] == ['Chart-5', 'Math-70']
    assert summary['bugs']['no_harnesses'] == ['Lang-43']


def test_population_check_of_nothing_has_no_mean():
    summary = root_cause.population_check([])
    assert summary == {'n_records': 0, 'n_scored': 0, 'scored': [],
                       'mean': None, 'counts': {}, 'bugs': {}}


def test_gate_separates_a_naming_fault_from_a_coverage_fault(fake_collect,
                                                             buggy_dir):
    """A seed the report holds no method for at all is a naming or
    extraction fault — the fault the gate exists to catch — and it reads
    differently from a method the tests simply did not run."""
    ghost = MethodRef('org.jfree.demo.Widget', 'noSuchMethod', ())
    gate = root_cause.trigger_gate(buggy_dir, _rc_with_seeds(DRAW, ghost),
                                   os.path.join(buggy_dir, 'trigger'))
    assert gate['passed'] is False
    assert gate['unresolved'] == [str(ghost)]
    assert gate['missed'] == [str(ghost)]
    assert 'no method in the coverage report matches' in gate['detail']
