"""Regression pins for the seven measurement fixes made after the first
real measurement run.

Each test encodes the concrete shape that failed on real data, not a
generic property: the Lang_39 checkout whose patched source root also
contains the word "buggy", the `String.charAt` callee the introspector
listed inside P, the overloaded `StringUtils.replaceEach` a crash frame
landed in, the phantom trailing line a patch's final newline produced,
the pure-deletion hunk with no line of its own on the buggy tree, the
`fuzz_out/` directory a `--coverage` run left behind, and the
`java.base/`-prefixed frames of a JDK 9+ stack trace.

Helpers are reused from the sibling measurement tests (plain functions
only): `_mset`/`_lset`/`_write_leg` build a leg the way `cli.py` writes
one, `_write_tree`/`_unified` build a tiny Java tree and diff it.
"""
import json
import os
import re

import pytest

from java.measurements import cli
from java.measurements import coverage as cov_mod
from java.measurements import crash_sites as cs_mod
from java.measurements import locations as loc
from java.measurements import metrics as M
from java.measurements import root_cause as rc
from java.execution import diffcov

from test_measurements_metrics import _lset, _mset, _write_leg
from test_measurements_root_cause import _unified, _write_tree

FIXTURES = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        'fixtures', 'measurements')
JACOCO_XML = os.path.join(FIXTURES, 'jacoco_example.xml')

RESULT = {'label': 'overfitting', 'status': 'evaluated',
          'bug_kind': 'crashing', 'project': 'Lang', 'bug_id': '39',
          'apr_tool': 'SimFix', 'crashed_on_patch': True}


# ===========================================================================
# 1. coverage._dirs_for_build — one build's directories only
# ===========================================================================

def test_dirs_for_build_splits_the_two_class_dirs():
    """A JaCoCo report handed both builds' classes reported every class
    twice; classpath.json lists `classes_buggy` and `classes_patched`
    side by side, so each report must get only its own."""
    dirs = ['/x/cov/classes_buggy', '/x/cov/classes_patched']
    assert cov_mod._dirs_for_build(dirs, 'buggy') == ['/x/cov/classes_buggy']
    assert cov_mod._dirs_for_build(dirs, 'patched') == \
        ['/x/cov/classes_patched']


def test_dirs_for_build_source_roots_where_both_paths_say_buggy():
    """The real Lang-39 layout: the patched checkout is a COPY of the
    buggy one, so its path contains 'buggy' too
    (`Lang_39_buggy_patched_patch1-Lang-39-SimFix`). A substring test on
    the whole path put both roots in the buggy build; the segment test
    splits them."""
    dirs = ['/co/Lang_39_buggy/src/java',
            '/co/Lang_39_buggy_patched_patch1-Lang-39-SimFix/src/java']
    assert cov_mod._dirs_for_build(dirs, 'buggy') == [dirs[0]]
    assert cov_mod._dirs_for_build(dirs, 'patched') == [dirs[1]]


def test_dirs_for_build_falls_back_to_everything_when_nothing_matches():
    """An older leg layout has a single, unlabelled classes directory;
    filtering it away would leave JaCoCo with no classfiles at all."""
    dirs = ['/build/classes', '/build/more']
    assert cov_mod._dirs_for_build(dirs, 'patched') == dirs
    assert cov_mod._dirs_for_build([], 'buggy') == []


def test_collect_leg_reports_each_exec_against_its_own_build_dirs(tmp_path,
                                                                  monkeypatch):
    """End to end: the arguments `collect_leg` hands `report` for the
    buggy `.exec` must name no patched directory, and vice versa."""
    calls = []

    def _fake_report(exec_paths, class_dirs, source_dirs, out_xml, jar=None):
        calls.append({'exec': list(exec_paths), 'classes': list(class_dirs),
                      'sources': list(source_dirs), 'xml': out_xml})
        with open(JACOCO_XML) as src, open(out_xml, 'w') as dst:
            dst.write(src.read())
        return out_xml

    monkeypatch.setattr(cov_mod, 'report', _fake_report)

    leg = tmp_path / 'leg'
    cov_dir = leg / 'cov'
    cov_dir.mkdir(parents=True)
    (cov_dir / 'attempt_001_buggy.exec').write_text('')
    (cov_dir / 'attempt_001_patched.exec').write_text('')
    (cov_dir / 'classpath.json').write_text(json.dumps({
        'class_dirs': ['/x/cov/classes_buggy', '/x/cov/classes_patched'],
        'source_dirs': ['/co/Lang_39_buggy/src/java',
                        '/co/Lang_39_buggy_patched_patch1-Lang-39-SimFix/'
                        'src/java'],
        'include_glob': 'org.jfree.**',
    }))

    out = cov_mod.collect_leg(str(leg))
    assert sorted(out) == ['buggy', 'patched']

    by_build = {('patched' if 'patched' in os.path.basename(c['xml'])
                 else 'buggy'): c for c in calls}
    assert len(calls) == 2 and sorted(by_build) == ['buggy', 'patched']
    assert by_build['buggy']['classes'] == ['/x/cov/classes_buggy']
    assert by_build['buggy']['sources'] == ['/co/Lang_39_buggy/src/java']
    assert by_build['patched']['classes'] == ['/x/cov/classes_patched']
    assert by_build['patched']['sources'] == [
        '/co/Lang_39_buggy_patched_patch1-Lang-39-SimFix/src/java']


# ===========================================================================
# 2. metrics._strip_jdk — JDK callees are not part of P or R
# ===========================================================================

JDK_QUALIFIED = loc.MethodRef('java.lang.String', 'charAt', ('int',))
JDK_UNQUALIFIED = loc.MethodRef('String', 'charAt', ('StringUtils',))
JDK_EXCEPTION = loc.MethodRef('IllegalArgumentException', '<init>',
                              ('String',))
LIB_QUALIFIED = loc.MethodRef('org.apache.commons.lang3.StringUtils',
                              'replaceEach', ('String', 'String[]',
                                              'String[]'))
LIB_UNQUALIFIED = loc.MethodRef('MatchNameNode', '<init>', ('String',))


def test_is_jdk_drops_jdk_refs_and_keeps_library_refs():
    """The introspector lists JDK receivers unqualified and with garbage
    parameter labels ('String.charAt(StringUtils)'), so a package test
    alone let them into P; the simple-name list catches those, and must
    not swallow an unqualified LIBRARY class."""
    assert M._is_jdk(JDK_QUALIFIED) is True
    assert M._is_jdk(JDK_UNQUALIFIED) is True
    assert M._is_jdk(JDK_EXCEPTION) is True
    assert M._is_jdk(LIB_QUALIFIED) is False
    assert M._is_jdk(LIB_UNQUALIFIED) is False


def test_strip_jdk_counts_drops_and_removes_their_edges():
    """A dropped ref must take its call-graph edges with it, or the
    remaining edge list points at methods no longer in the set."""
    ms = _mset([(LIB_QUALIFIED, loc.SEED), (LIB_UNQUALIFIED, loc.CALLER),
                (JDK_QUALIFIED, loc.CALLEE), (JDK_UNQUALIFIED, loc.CALLEE),
                (JDK_EXCEPTION, loc.CALLEE)])
    ms.edges = [(LIB_QUALIFIED, JDK_QUALIFIED),
                (JDK_QUALIFIED, LIB_UNQUALIFIED),
                (LIB_QUALIFIED, LIB_UNQUALIFIED)]
    ms.unmatched = ['some.unresolved.name']

    kept, dropped = M._strip_jdk(ms)

    assert dropped == 3
    assert set(kept.refs()) == {LIB_QUALIFIED, LIB_UNQUALIFIED}
    assert kept.edges == [(LIB_QUALIFIED, LIB_UNQUALIFIED)]
    assert kept.unmatched == ['some.unresolved.name']
    # the ring tags of the survivors are untouched
    assert kept.ring_of(LIB_QUALIFIED) == loc.SEED
    assert kept.ring_of(LIB_UNQUALIFIED) == loc.CALLER
    assert M._strip_jdk(None) == (None, 0)


def test_compute_leg_reports_jdk_dropped_for_p_and_r(tmp_path):
    """The count has to reach the metrics row: a leg whose P was mostly
    JDK callees looked like a wide P with a terrible PSC."""
    run = str(tmp_path / 'run')
    os.makedirs(run, exist_ok=True)
    leg = _write_leg(
        run, '01_patch1-Lang-39-SimFix_o', RESULT,
        patch_derived=_mset([(LIB_QUALIFIED, loc.SEED),
                             (JDK_QUALIFIED, loc.CALLEE),
                             (JDK_UNQUALIFIED, loc.CALLEE)]),
        root_cause={'methods': _mset([(LIB_QUALIFIED, loc.SEED),
                                      (JDK_EXCEPTION, loc.CALLEE)]).to_dict(),
                    'lines': _lset([]).to_dict(),
                    'manifest': _mset([]).to_dict()})

    row = M.compute_leg(leg)
    assert row['jdk_dropped'] == {'P_method': 2, 'R_method': 1}
    assert row['sizes']['P_method'] == 1
    assert row['sizes']['R_method']['full'] == 1
    # P is now exactly R, so RCR is 1.0 rather than diluted by JDK members
    assert row['rcr__method__full__na']['value'] == 1.0


# ===========================================================================
# 3. metrics._site_in — a frame inside an overloaded method
# ===========================================================================

RE_CALLER = loc.MethodRef('org.apache.commons.lang3.StringUtils',
                          'replaceEach', ('String', 'String[]', 'String[]'))
RE_SEED = loc.MethodRef('org.apache.commons.lang3.StringUtils', 'replaceEach',
                        ('String', 'String[]', 'String[]', 'boolean', 'int'))
RE_FRAME = loc.MethodRef('org.apache.commons.lang3.StringUtils', 'replaceEach')


def test_site_in_picks_the_nearest_ring_of_an_overloaded_name():
    """The real crash frame `at ...StringUtils.replaceEach(StringUtils
    .java:3620)` carries no parameter types, and R held BOTH arities (the
    5-arg seed the developer fixed and the 3-arg caller). The by-name
    lookup was ambiguous and scored the crash 'outside R'; it must
    resolve to the nearer ring instead."""
    rset = _mset([(RE_SEED, loc.SEED), (RE_CALLER, loc.CALLER)])
    index = loc.MethodIndex(rset.refs())
    # the ambiguity is real: the plain index lookup still gives up
    assert index.lookup(RE_FRAME, frame=True) is None
    assert M._site_in(index, RE_FRAME, rset) == RE_SEED


def test_site_in_is_none_when_the_name_is_not_in_the_set():
    """Disambiguating must not turn into 'always find something'."""
    other = loc.MethodRef('org.apache.commons.lang3.StringUtils', 'join',
                          ('String[]',))
    rset = _mset([(other, loc.SEED)])
    index = loc.MethodIndex(rset.refs())
    assert M._site_in(index, RE_FRAME, rset) is None
    assert M._site_in(index, None, rset) is None


def test_site_in_with_params_takes_the_ordinary_lookup():
    """A ref that DOES carry parameters is not a frame; it must match its
    own overload exactly, not the nearest ring."""
    rset = _mset([(RE_SEED, loc.SEED), (RE_CALLER, loc.CALLER)])
    index = loc.MethodIndex(rset.refs())
    assert M._site_in(index, RE_CALLER, rset) == RE_CALLER
    assert M._site_in(index, RE_SEED, rset) == RE_SEED


def test_csm_counts_the_overloaded_frame_as_inside_the_seed_ring(tmp_path):
    """Same shape through `compute_leg`: the crash landed on the method
    the developer fixed, so CSM is 1.0 and the seed ring owns it."""
    run = str(tmp_path / 'run')
    os.makedirs(run, exist_ok=True)
    leg = _write_leg(
        run, '01_patch1-Lang-39-SimFix_o', RESULT,
        patch_derived=_mset([(RE_SEED, loc.SEED)]),
        root_cause={'methods': _mset([(RE_SEED, loc.SEED),
                                      (RE_CALLER, loc.CALLER)]).to_dict(),
                    'lines': _lset([]).to_dict(),
                    'manifest': _mset([]).to_dict()},
        crash_sites=[{'build': 'buggy', 'site_kind': 'library',
                      'exception': 'java.lang.NullPointerException',
                      'top_library': RE_FRAME.to_dict(),
                      'top_library_line': 3620}])

    row = M.compute_leg(leg)
    csm = row['csm__method__full__na']
    assert (csm['value'], csm['num'], csm['den']) == (1.0, 1, 1)
    assert csm['by_ring']['seed'] == {'value': 1.0, 'num': 1, 'den': 1}
    assert csm['by_ring']['outside']['num'] == 0


# ===========================================================================
# 4. root_cause.parse_hunks / orient_patch — the phantom line and the
#    pure-deletion orientation check
# ===========================================================================

THING_REL = 'src/org/example/Thing.java'

THING_FIXED = """package org.example;

public class Thing {

    public int f(int x) {
        if (x < 0) {
            throw new IllegalArgumentException();
        }
        return x;
    }
}
"""

# The buggy revision: the developer's guard is simply absent, so the
# fixed -> buggy patch is a PURE DELETION with no '+' line anywhere.
THING_BUGGY = THING_FIXED \
    .replace('        if (x < 0) {\n'
             '            throw new IllegalArgumentException();\n'
             '        }\n', '')

_HDR_RE = re.compile(r'^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@')


def _deletion_patch():
    """fixed -> buggy: the Defects4J direction."""
    return _unified(THING_FIXED, THING_BUGGY, THING_REL)


@pytest.fixture
def thing_dir(tmp_path):
    return _write_tree(tmp_path / 'buggy', {THING_REL: THING_BUGGY})


def test_parse_hunks_drops_the_phantom_final_empty_body_line():
    """A patch text ends with a newline, so splitting it leaves a final
    '' that is not a context line. Counting it put a context line one
    past the end of the hunk, and every orientation check then failed on
    a file that actually matched."""
    patch = _deletion_patch()
    assert patch.endswith('\n')
    hunks = rc.parse_hunks(patch)
    assert len(hunks) == 1
    h = hunks[0]
    assert h.body[-1] != ''
    m = _HDR_RE.match([l for l in patch.split('\n')
                       if l.startswith('@@')][0])
    old_count = int(m.group(2)) if m.group(2) is not None else 1
    new_count = int(m.group(4)) if m.group(4) is not None else 1
    # the header implies (new lines - added lines) context lines
    assert len(h.context_new) == new_count - len(h.added)
    assert len(h.context_old) == old_count - len(h.removed)
    assert h.added == []


def test_orient_patch_verifies_a_pure_deletion_against_the_buggy_tree(
        thing_dir):
    """A pure deletion has an empty '+' side, so the old check ('are the
    added lines in the buggy tree?') had nothing to test and fell back to
    DIR_ASSUMED. Its context lines still pin the orientation."""
    patch = _deletion_patch()
    oriented, direction = rc.orient_patch(patch, thing_dir)
    assert direction == rc.DIR_FIXED_TO_BUGGY
    assert oriented == patch


def test_orient_patch_reverses_a_pure_insertion(thing_dir):
    """The same patch the other way round (buggy -> fixed, a pure
    insertion) must be recognised and swapped."""
    reversed_text = rc.reverse_patch(_deletion_patch())
    oriented, direction = rc.orient_patch(reversed_text, thing_dir)
    assert direction == rc.DIR_BUGGY_TO_FIXED
    assert oriented == rc.reverse_patch(reversed_text)
    # after the swap the patch deletes again, and deletes the guard
    assert [t for _n, t in rc.parse_hunks(oriented)[0].removed] == [
        '        if (x < 0) {',
        '            throw new IllegalArgumentException();',
        '        }']


# ===========================================================================
# 5. changed_lines / diffcov.changed_lines_by_file — deletion boundaries
#    and change-group order
# ===========================================================================

MOD_HUNK_MINUS_FIRST = """--- a/src/org/example/Thing.java
+++ b/src/org/example/Thing.java
@@ -5,3 +5,3 @@
     public int f(int x) {
-        return x;
+        return x + 1;
     }
"""

MOD_HUNK_PLUS_FIRST = """--- a/src/org/example/Thing.java
+++ b/src/org/example/Thing.java
@@ -5,3 +5,3 @@
     public int f(int x) {
+        return x + 1;
-        return x;
     }
"""

DEL_HUNK = """--- a/src/org/example/Thing.java
+++ b/src/org/example/Thing.java
@@ -5,4 +5,3 @@
     public int f(int x) {
-        check(x);
         return x;
     }
"""


def test_changed_lines_of_a_deletion_takes_both_boundary_lines(thing_dir):
    """The deleted guard has no line of its own on the buggy tree. The
    mapper recorded only the line BEFORE the gap; a crash caused by the
    missing check lands on the line AFTER it just as often, so both
    boundary lines belong to the developer's region."""
    lines = rc.changed_lines(_deletion_patch(), thing_dir)
    body = THING_BUGGY.split('\n')
    before = body.index('    public int f(int x) {') + 1
    after = body.index('        return x;') + 1
    assert after == before + 1
    assert loc.LineRef('org.example.Thing', before) in lines
    assert loc.LineRef('org.example.Thing', after) in lines
    assert all(lines.ring_of(r) == loc.SEED for r in lines.refs())
    # nothing outside the gap's two boundary lines is claimed
    assert {r.line for r in lines.refs()} == {before, after}


def test_changed_lines_of_a_plus_first_modification_has_no_boundary_line(
        thing_dir):
    """`reverse_patch` lists a modification's '+' side before its '-'
    side. Deciding per hunk instead of per change group made the mapper
    treat the trailing '-' as a pure deletion and add the untouched line
    above it to R."""
    lines = rc.changed_lines(MOD_HUNK_PLUS_FIRST, thing_dir)
    assert {r.line for r in lines.refs()} == {6}
    minus_first = rc.changed_lines(MOD_HUNK_MINUS_FIRST, thing_dir)
    assert set(minus_first.refs()) == set(lines.refs())


def test_diffcov_change_group_order_does_not_change_the_lines():
    """Same rule one layer down, where the pipeline's own line mapper
    lives: a collapsed line is recorded only for a group with no '+' at
    all, whichever order the hunk lists its sides in."""
    minus_first = diffcov.changed_lines_by_file(MOD_HUNK_MINUS_FIRST)
    plus_first = diffcov.changed_lines_by_file(MOD_HUNK_PLUS_FIRST)
    assert minus_first == plus_first == {THING_REL: [6]}
    # a group with no '+' does record the line the deletion collapsed onto
    assert diffcov.changed_lines_by_file(DEL_HUNK) == {THING_REL: [5]}


# ===========================================================================
# 6. cli.measure_leg — fuzz_out/ wins over trace.md for crash sites
# ===========================================================================

JAZZER_BUGGY = """INFO: Instrumented org.example.Thing (took 8 ms)
#2\tpulse  ft: 12 exec/s: 0 rss: 512Mb

== Java Exception: java.lang.NullPointerException
\tat org.example.Thing.f(Thing.java:21)
\tat org.example.FuzzHarness.fuzzerTestOneInput(FuzzHarness.java:12)
\tat java.base/java.lang.reflect.Method.invoke(Method.java:568)
== libFuzzer crashing input ==
"""

JAZZER_PATCHED = """== Java Exception: java.lang.IllegalStateException: boom
\tat org.example.Thing.g(Thing.java:30)
\tat org.example.FuzzHarness.fuzzerTestOneInput(FuzzHarness.java:12)
== libFuzzer crashing input ==
"""


def _crash_leg(tmp_path, with_fuzz_out):
    leg = tmp_path / '01_patch1-Lang-39-SimFix_o'
    leg.mkdir(parents=True)
    (leg / 'result.jsonl').write_text(json.dumps(RESULT) + '\n')
    (leg / 'trace.md').write_text('# trace\n')
    if with_fuzz_out:
        fo = leg / 'fuzz_out'
        fo.mkdir()
        (fo / 'attempt_001_buggy.txt').write_text(JAZZER_BUGGY)
        (fo / 'attempt_002_patched.txt').write_text(JAZZER_PATCHED)
    return leg


def _isolate_other_stages(monkeypatch):
    """Only the crash-site stage is under test here."""
    monkeypatch.setattr(cli, 'ensure_buggy_checkout', lambda *a, **k: (
        (_ for _ in ()).throw(RuntimeError('no defects4j here'))))
    for name in ('_mod_patch_derived', '_mod_root_cause', '_mod_coverage'):
        monkeypatch.setattr(cli, name, lambda: (_ for _ in ()).throw(
            ImportError('not needed here')))


def test_measure_leg_reads_crash_sites_from_fuzz_out_not_the_trace(
        tmp_path, monkeypatch):
    """A `--coverage` run saves every fuzz run's raw Jazzer output under
    fuzz_out/, for BOTH builds; trace.md only carries the patched build's
    evidence blocks, so reading the trace lost every buggy-build crash
    and left `build` unattributable."""
    leg = _crash_leg(tmp_path, with_fuzz_out=True)
    _isolate_other_stages(monkeypatch)
    monkeypatch.setattr(cs_mod, 'from_trace', lambda p: (
        (_ for _ in ()).throw(AssertionError('from_trace must not run'))))

    status = cli.measure_leg(str(leg))
    assert 'crash_sites' in status['done'], status['errors']

    sites = json.loads(
        (leg / 'measurements' / 'crash_sites.json').read_text())
    assert len(sites) == 2
    by_build = {s['build']: s for s in sites}
    assert sorted(by_build) == ['buggy', 'patched']
    assert by_build['buggy']['source'] == 'fuzz_out/attempt_001_buggy.txt'
    assert by_build['buggy']['harness'] == 'attempt_001'
    assert by_build['buggy']['exception'] == 'java.lang.NullPointerException'
    assert by_build['buggy']['top_library']['name'] == 'f'
    assert by_build['buggy']['site_kind'] == 'library'
    assert by_build['patched']['source'] == 'fuzz_out/attempt_002_patched.txt'
    assert by_build['patched']['harness'] == 'attempt_002'


def test_measure_leg_falls_back_to_the_trace_without_fuzz_out(tmp_path,
                                                              monkeypatch):
    """Legs archived before the fuzz_out/ dump exists must still be
    measurable from trace.md alone."""
    leg = _crash_leg(tmp_path, with_fuzz_out=False)
    _isolate_other_stages(monkeypatch)
    seen = {}

    def _from_trace(path):
        seen['path'] = path
        return [cs_mod.CrashSite(exception='java.lang.AssertionError',
                                 site_kind=cs_mod.SITE_HARNESS_ONLY,
                                 build='patched', source='trace')]

    monkeypatch.setattr(cs_mod, 'from_trace', _from_trace)

    status = cli.measure_leg(str(leg))
    assert 'crash_sites' in status['done'], status['errors']
    assert seen['path'] == str(leg / 'trace.md')
    sites = json.loads(
        (leg / 'measurements' / 'crash_sites.json').read_text())
    assert [s['source'] for s in sites] == ['trace']


# ===========================================================================
# 7. locations.from_stack_frame — module and classloader prefixes
# ===========================================================================

def test_from_stack_frame_accepts_a_module_prefixed_frame():
    """JDK 9+ prints `java.base/java.lang.String.substring(...)`. The
    module prefix was read as part of the class name, so the frame parsed
    to a class nothing could match."""
    ref, line = loc.from_stack_frame(
        '\tat java.base/java.lang.String.substring(String.java:1874)')
    assert ref == loc.MethodRef('java.lang.String', 'substring')
    assert line == 1874


def test_from_stack_frame_accepts_a_classloader_prefixed_frame():
    """Some runners print the class loader instead: `app//org.jfree...`."""
    ref, line = loc.from_stack_frame('\tat app//org.jfree.Foo.bar(Foo.java:12)')
    assert ref == loc.MethodRef('org.jfree.Foo', 'bar')
    assert line == 12
    # the plain form still works, and a non-frame line is still rejected
    assert loc.from_stack_frame('at a.B.c(B.java:1)')[0] == \
        loc.MethodRef('a.B', 'c')
    assert loc.from_stack_frame('Caused by: java.lang.Error') is None


def test_is_synthetic_needs_the_lambda_dollar_not_the_word_lambda():
    """`lambdaWeight` is an ordinary library method; a prefix test on the
    bare word 'lambda' dropped it from the covered set."""
    assert loc.MethodRef('a.B', 'lambdaWeight').is_synthetic is False
    assert loc.MethodRef('a.B', 'lambda$x$0').is_synthetic is True
    assert loc.MethodRef('a.B', 'access$100').is_synthetic is True
