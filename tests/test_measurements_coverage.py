"""Tests for java.measurements.coverage — F(H) from JaCoCo reports.

The fixture `fixtures/measurements/jacoco_example.xml` is a hand-written
JaCoCo report with everything the parser has to get right in one file:
a top-level class, one of its nested classes (sharing the source file),
a lambda, a `FuzzHarness` class that must not be counted, a method that
was never executed, branch counters, and a second package used to test
the `include_prefix` filter.
"""
import json
import os
import subprocess

import pytest

from java.measurements import coverage as cov_mod
from java.measurements.coverage import Coverage
from java.measurements.locations import LineRef, MethodRef

FIXTURES = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        'fixtures', 'measurements')
JACOCO_XML = os.path.join(FIXTURES, 'jacoco_example.xml')

# The library methods in the fixture, by strict key.
W_INIT = 'org.jfree.demo.Widget.<init>()'
W_DRAW = 'org.jfree.demo.Widget.draw(Graphics2D,int)'
W_HELPER = 'org.jfree.demo.Widget.unusedHelper(int[],boolean)'
W_INNER = 'org.jfree.demo.Widget.Inner.compute(int)'
OTHER_GO = 'org.other.Thing.go()'


def keys(refs):
    return {r.strict_key for r in refs}


# ---------------------------------------------------------------- parsing

def test_parse_covered_methods():
    """Covered = JaCoCo METHOD counter with covered > 0. The lambda is
    dropped as synthetic; the harness class is dropped as harness; the
    never-executed method is present in the population but not covered."""
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    assert keys(cov.methods) == {W_INIT, W_DRAW, W_INNER, OTHER_GO}


def test_parse_all_methods_is_the_population():
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    assert keys(cov.all_methods) == {W_INIT, W_DRAW, W_HELPER, W_INNER,
                                     OTHER_GO}
    # every covered method is in the population it is measured against
    assert cov.methods <= cov.all_methods


def test_parse_descriptors_become_simple_param_names():
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    draw = next(m for m in cov.all_methods if m.name == 'draw')
    assert draw.params == ('Graphics2D', 'int')
    helper = next(m for m in cov.all_methods if m.name == 'unusedHelper')
    assert helper.params == ('int[]', 'boolean')


def test_parse_nested_class_keeps_its_own_identity():
    """Widget$Inner is its own class for method identity ('$' becomes
    '.'), even though it shares Widget.java for line identity."""
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    inner = next(m for m in cov.all_methods if m.name == 'compute')
    assert inner.class_fq == 'org.jfree.demo.Widget.Inner'


def test_parse_lines_key_on_the_top_level_class():
    """Lines come from <sourcefile>, keyed by package + file name, so a
    nested class's line lands on the outer class. ci=0 is not covered."""
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    assert cov.lines == {
        LineRef('org.jfree.demo.Widget', 10),
        LineRef('org.jfree.demo.Widget', 20),
        LineRef('org.jfree.demo.Widget', 60),   # the nested class's line
        LineRef('org.other.Thing', 7),
    }
    assert LineRef('org.jfree.demo.Widget', 40) not in cov.lines


def test_parse_excludes_the_harness():
    """FuzzHarness is the thing doing the measuring, not the thing being
    measured: neither its method, nor its line, nor its branches count."""
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    assert not [m for m in cov.all_methods
                if m.class_simple.startswith('FuzzHarness')]
    assert not [l for l in cov.lines if 'FuzzHarness' in l.class_top_fq]
    # the harness's 1-of-2 branches are not in the totals either
    assert cov.branches_covered == 3 + 0 + 2      # draw, unusedHelper, compute
    assert cov.branches_total == 4 + 2 + 2


def test_parse_include_prefix_filters_on_package_boundaries():
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML, include_prefix='org.jfree')
    assert keys(cov.methods) == {W_INIT, W_DRAW, W_INNER}
    assert OTHER_GO not in keys(cov.all_methods)
    assert LineRef('org.other.Thing', 7) not in cov.lines


def test_parse_include_prefix_accepts_a_jazzer_glob():
    """The same string the run passed to --instrumentation_includes can
    be reused as the parse filter."""
    a = cov_mod.parse_jacoco_xml(JACOCO_XML, include_prefix='org.jfree.**')
    b = cov_mod.parse_jacoco_xml(JACOCO_XML, include_prefix='org.jfree')
    assert keys(a.methods) == keys(b.methods)


def test_parse_prefix_does_not_match_a_partial_segment():
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML, include_prefix='org.jfr')
    assert cov.methods == set()


# ---------------------------------------------------------- serialisation

def test_round_trip_through_dict():
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    cov.build, cov.harness = 'patched', 'attempt_003'
    back = Coverage.from_dict(json.loads(json.dumps(cov.to_dict())))
    assert back.methods == cov.methods
    assert back.lines == cov.lines
    assert back.all_methods == cov.all_methods
    assert (back.branches_covered, back.branches_total) == (
        cov.branches_covered, cov.branches_total)
    assert (back.build, back.harness) == ('patched', 'attempt_003')


def test_to_dict_is_deterministic():
    a = cov_mod.parse_jacoco_xml(JACOCO_XML).to_dict()
    b = cov_mod.parse_jacoco_xml(JACOCO_XML).to_dict()
    assert json.dumps(a) == json.dumps(b)


# ----------------------------------------------------------------- union

def test_union_merges_sets_and_keeps_the_common_build():
    m1 = MethodRef('org.jfree.demo.Widget', 'draw', ('int',))
    m2 = MethodRef('org.jfree.demo.Widget', 'paint', ())
    a = Coverage(methods={m1}, lines={LineRef('org.jfree.demo.Widget', 1)},
                 branches_covered=3, branches_total=8, all_methods={m1, m2},
                 build='buggy', harness='attempt_001')
    b = Coverage(methods={m2}, lines={LineRef('org.jfree.demo.Widget', 2)},
                 branches_covered=5, branches_total=8, all_methods={m1, m2},
                 build='buggy', harness='attempt_002')
    u = cov_mod.union([a, b])
    assert u.methods == {m1, m2}
    assert len(u.lines) == 2
    assert u.all_methods == {m1, m2}
    # branch coverage cannot be added without double counting, so the
    # union keeps the largest single observation as a lower bound
    assert (u.branches_covered, u.branches_total) == (5, 8)
    assert u.build == 'buggy'
    assert u.harness == 'attempt_001+attempt_002'


def test_union_drops_the_build_when_inputs_disagree():
    a = Coverage(build='buggy')
    b = Coverage(build='patched')
    assert cov_mod.union([a, b]).build == ''


def test_union_of_nothing_is_empty():
    u = cov_mod.union([])
    assert u.methods == set() and u.lines == set() and u.build == ''


# ------------------------------------------------------------ jazzer args

def test_jazzer_coverage_args():
    args = cov_mod.jazzer_coverage_args('/tmp/run.exec', 'org.jfree.**')
    assert args == ['--coverage_dump=/tmp/run.exec',
                    '--instrumentation_includes=org.jfree.**']


def test_jazzer_coverage_args_is_pure():
    """It only builds strings — no jar lookup, no process, no file."""
    before = cov_mod.jazzer_coverage_args('/tmp/a.exec', 'x.**')
    after = cov_mod.jazzer_coverage_args('/tmp/a.exec', 'x.**')
    assert before == after
    assert not os.path.exists('/tmp/a.exec')


# ---------------------------------------------------------------- report

class _FakeProc:
    def __init__(self, returncode=0, stderr=''):
        self.returncode = returncode
        self.stdout = ''
        self.stderr = stderr


@pytest.fixture
def fake_java(monkeypatch, tmp_path):
    """Replace the one subprocess call with a recorder that writes an XML
    where the real tool would."""
    calls = []

    def _run(cmd):
        calls.append(list(cmd))
        out = cmd[cmd.index('--xml') + 1]
        with open(JACOCO_XML) as src, open(out, 'w') as dst:
            dst.write(src.read())
        return _FakeProc()

    monkeypatch.setattr(cov_mod, '_run', _run)
    return calls


def test_report_builds_the_expected_command(fake_java, tmp_path):
    out = str(tmp_path / 'report.xml')
    got = cov_mod.report(['/x/a.exec', '/x/b.exec'],
                         ['/build/classes', '/build/more'],
                         ['/src/main/java'], out, jar='/jars/jacoco.jar')
    assert got == out
    cmd = fake_java[0]
    assert cmd[:5] == ['java', '-jar', '/jars/jacoco.jar', 'report',
                       '/x/a.exec']
    assert cmd[5] == '/x/b.exec'
    assert cmd[6:10] == ['--classfiles', '/build/classes',
                         '--classfiles', '/build/more']
    assert cmd[10:12] == ['--sourcefiles', '/src/main/java']
    assert cmd[12:] == ['--xml', out]
    assert os.path.isfile(out)


def test_report_raises_when_the_tool_fails(monkeypatch, tmp_path):
    monkeypatch.setattr(cov_mod, '_run',
                        lambda cmd: _FakeProc(1, 'boom'))
    with pytest.raises(RuntimeError, match='boom'):
        cov_mod.report(['/x/a.exec'], ['/c'], ['/s'],
                       str(tmp_path / 'r.xml'), jar='/jars/jacoco.jar')


def test_report_raises_when_no_xml_appears(monkeypatch, tmp_path):
    monkeypatch.setattr(cov_mod, '_run', lambda cmd: _FakeProc(0))
    with pytest.raises(RuntimeError, match='no XML'):
        cov_mod.report(['/x/a.exec'], ['/c'], ['/s'],
                       str(tmp_path / 'r.xml'), jar='/jars/jacoco.jar')


# ------------------------------------------------------------- jacoco jar

def test_ensure_jacoco_cli_returns_an_existing_jar(monkeypatch, tmp_path):
    jar = tmp_path / 'org.jacoco.cli.jar'
    jar.write_text('not really a jar')
    monkeypatch.setenv('JACOCO_CLI_JAR', str(jar))

    def _no_download(*a, **k):
        raise AssertionError('should not download an existing jar')

    monkeypatch.setattr(cov_mod.urllib.request, 'urlretrieve', _no_download)
    assert cov_mod.ensure_jacoco_cli() == str(jar)


def test_ensure_jacoco_cli_downloads_into_the_cache(monkeypatch, tmp_path):
    """The jar's name and its URL come from `config`, which is where
    `d4j_rcc_sweep.reached` reads them too: one JaCoCo for both."""
    import config

    monkeypatch.delenv('JACOCO_CLI_JAR', raising=False)
    seen = {}

    def _fetch(url, dest):
        seen['url'], seen['dest'] = url, dest
        open(dest, 'w').write('jar')

    monkeypatch.setattr(cov_mod.urllib.request, 'urlretrieve', _fetch)
    jar = cov_mod.ensure_jacoco_cli(cache_dir=str(tmp_path / 'cache'))
    assert os.path.basename(jar) == os.path.basename(config.JACOCO_CLI_JAR)
    assert os.path.dirname(jar) == str(tmp_path / 'cache')
    assert seen['url'] == config.JACOCO_CLI_URL == cov_mod.JACOCO_CLI_URL
    assert seen['dest'] == jar


def test_ensure_jacoco_cli_defaults_to_the_config_path(monkeypatch):
    """With no cache directory and no override, the jar is exactly the one
    `config.JACOCO_CLI_JAR` names — the same file
    `d4j_rcc_sweep.reached.ensure_cli_jar` would use."""
    import config

    monkeypatch.delenv('JACOCO_CLI_JAR', raising=False)

    def _fetch(url, dest):
        raise AssertionError(f'should not download in this test: {dest}')

    monkeypatch.setattr(cov_mod.urllib.request, 'urlretrieve', _fetch)
    monkeypatch.setattr(cov_mod.os.path, 'isfile', lambda p: True)
    assert cov_mod.ensure_jacoco_cli() == config.JACOCO_CLI_JAR


# ------------------------------------------------------------ collect_leg

def test_collect_leg_unions_per_build_and_writes_json(fake_java, tmp_path):
    leg = tmp_path / 'leg'
    cov_dir = leg / 'cov'
    cov_dir.mkdir(parents=True)
    for name in ('attempt_002_buggy.exec', 'attempt_002_patched.exec',
                 'attempt_003_patched.exec', 'notes.txt'):
        (cov_dir / name).write_text('')
    (cov_dir / 'classpath.json').write_text(json.dumps({
        'class_dirs': ['/build/classes'],
        'source_dirs': ['/src/main/java'],
        'include_glob': 'org.jfree.**',
    }))

    out = cov_mod.collect_leg(str(leg))

    assert sorted(out) == ['buggy', 'patched']
    assert out['patched'].harness == 'attempt_002+attempt_003'
    assert out['buggy'].harness == 'attempt_002'
    # include_glob from classpath.json was applied at parse time
    assert OTHER_GO not in keys(out['patched'].all_methods)
    # one report per .exec, and an XML cached next to it, plus ONE merged
    # report for the only build that has more than one .exec — `buggy` has
    # a single dump, so its per-harness report already is the merged one.
    assert len(fake_java) == 4
    assert (cov_dir / 'attempt_002_buggy.xml').is_file()
    assert (cov_dir / 'merged_patched.xml').is_file()
    assert not (cov_dir / 'merged_buggy.xml').exists()
    merged_cmd = [c for c in fake_java
                  if c[c.index('--xml') + 1].endswith('merged_patched.xml')]
    assert len(merged_cmd) == 1
    assert sorted(a for a in merged_cmd[0] if a.endswith('.exec')) == [
        str(cov_dir / 'attempt_002_patched.exec'),
        str(cov_dir / 'attempt_003_patched.exec')]
    assert out['patched'].branches_from == cov_mod.BRANCHES_MERGED
    assert out['buggy'].branches_from == cov_mod.BRANCHES_MERGED
    # the per-build unions are written where the CLI expects them
    for build in ('buggy', 'patched'):
        path = leg / 'measurements' / f'coverage_{build}.json'
        loaded = Coverage.from_dict(json.loads(path.read_text()))
        assert loaded.build == build
        assert loaded.methods == out[build].methods


def test_collect_leg_reuses_a_cached_xml(fake_java, tmp_path):
    leg = tmp_path / 'leg'
    cov_dir = leg / 'cov'
    cov_dir.mkdir(parents=True)
    (cov_dir / 'attempt_001_buggy.exec').write_text('')
    with open(JACOCO_XML) as src:
        (cov_dir / 'attempt_001_buggy.xml').write_text(src.read())
    (cov_dir / 'classpath.json').write_text(json.dumps(
        {'class_dirs': [], 'source_dirs': [], 'include_glob': ''}))

    cov_mod.collect_leg(str(leg))
    assert fake_java == []          # the CLI was never run


def test_collect_leg_ignores_exec_files_with_no_build_suffix(fake_java,
                                                             tmp_path):
    leg = tmp_path / 'leg'
    cov_dir = leg / 'cov'
    cov_dir.mkdir(parents=True)
    (cov_dir / 'stray.exec').write_text('')
    (cov_dir / 'attempt_001_unknownbuild.exec').write_text('')
    (cov_dir / 'classpath.json').write_text(json.dumps(
        {'class_dirs': [], 'source_dirs': [], 'include_glob': ''}))

    assert cov_mod.collect_leg(str(leg)) == {}
    assert fake_java == []


def test_collect_leg_collects_the_compiled_candidate_set(fake_java, tmp_path):
    """`compiled` is a third build token: the acceptance gate's run of
    EVERY candidate that compiled, kept or not.  It is grouped, unioned
    and written exactly like the other two, so a leg gets a third file,
    and the harness names it lists are the compiled ones — including the
    candidates that never made it into the kept set."""
    leg = tmp_path / 'leg'
    cov_dir = leg / 'cov'
    cov_dir.mkdir(parents=True)
    for name in ('attempt_001_compiled.exec', 'attempt_002_compiled.exec',
                 'attempt_002_buggy.exec', 'attempt_002_patched.exec'):
        (cov_dir / name).write_text('')
    (cov_dir / 'classpath.json').write_text(json.dumps({
        'class_dirs': ['/cov/classes_buggy', '/cov/classes_patched'],
        'source_dirs': ['/buggy/source', '/patched/source'],
        'include_glob': 'org.jfree.**',
    }))

    out = cov_mod.collect_leg(str(leg))

    assert sorted(out) == ['buggy', 'compiled', 'patched']
    # attempt_001 was rejected by the gate, so it appears ONLY here
    assert out['compiled'].harness == 'attempt_001+attempt_002'
    assert out['buggy'].harness == 'attempt_002'
    path = leg / 'measurements' / 'coverage_compiled.json'
    loaded = Coverage.from_dict(json.loads(path.read_text()))
    assert loaded.build == 'compiled'
    assert loaded.methods == out['compiled'].methods
    # the report for a `compiled` dump was built against the BUGGY classes:
    # that is the build the acceptance gate runs on.
    cmd = [c for c in fake_java
           if any(a.endswith('attempt_001_compiled.exec') for a in c)][0]
    assert '/cov/classes_buggy' in cmd and '/cov/classes_patched' not in cmd
    assert '/buggy/source' in cmd and '/patched/source' not in cmd


def test_dirs_for_build_maps_compiled_to_the_buggy_side():
    """`compiled` names a harness SET and `remeasure` a budget, not a
    different build of the code — the acceptance gate and the fixed-budget
    re-run both run on the buggy build — so both must resolve to the same
    directories `buggy` does."""
    dirs = ['/cov/classes_buggy', '/cov/classes_patched']
    assert cov_mod._dirs_for_build(dirs, 'compiled') == \
        cov_mod._dirs_for_build(dirs, 'remeasure') == \
        cov_mod._dirs_for_build(dirs, 'buggy') == ['/cov/classes_buggy']
    assert cov_mod._dirs_for_build(dirs, 'patched') == ['/cov/classes_patched']
    # an older layout that names neither build still uses everything
    assert cov_mod._dirs_for_build(['/one', '/two'], 'compiled') == \
        ['/one', '/two']


def test_split_exec_name_knows_the_build_tokens():
    assert cov_mod._split_exec_name('attempt_003_compiled') == \
        ('attempt_003', 'compiled')
    assert cov_mod.BUILDS == ('buggy', 'patched', 'compiled', 'remeasure')
    # the split is on the LAST underscore, so no token may contain one
    for build in cov_mod.BUILDS:
        assert '_' not in build
    assert cov_mod._split_exec_name('attempt_003_kept') is None


def test_the_real_runner_is_a_plain_subprocess_call():
    """Guard the seam the tests monkeypatch: `_run` must stay the only
    place a process is started."""
    assert cov_mod._run.__module__ == cov_mod.__name__
    assert 'subprocess' in cov_mod.__dict__
    assert cov_mod.subprocess is subprocess


# --------------------------------------------------- the probe limitation

# Two overloads of one name, so a frame's LINE has to do the work its
# missing parameter types cannot.  Written inline rather than added to the
# shared fixture, because nothing else needs an overload pair.
_OVERLOAD_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<report name="demo">
  <package name="org/jfree/demo">
    <class name="org/jfree/demo/Solver" sourcefilename="Solver.java">
      <method name="solve" desc="(DD)D" line="66">
        <counter type="LINE" missed="1" covered="0"/>
        <counter type="METHOD" missed="1" covered="0"/>
      </method>
      <method name="solve" desc="(Ljava/lang/Object;DDD)D" line="72">
        <counter type="LINE" missed="1" covered="0"/>
        <counter type="METHOD" missed="1" covered="0"/>
      </method>
    </class>
    <sourcefile name="Solver.java">
      <line nr="66" mi="1" ci="0"/>
      <line nr="72" mi="1" ci="0"/>
    </sourcefile>
  </package>
</report>
"""


def _overload_report(tmp_path):
    path = tmp_path / 'overloads.xml'
    path.write_text(_OVERLOAD_XML)
    return str(path)


def test_method_line_owners_splits_a_file_at_the_next_declaration():
    """The report says where a method starts and, separately, which lines
    ran.  Ownership is the gap to the next declaration in the same SOURCE
    FILE — nested classes included, or the ranges would overlap."""
    owners = cov_mod.method_line_owners(JACOCO_XML)
    by_key = {r.strict_key: rng for r, rng in owners.items()}
    assert by_key[W_INIT] == (10, 20)
    assert by_key[W_DRAW] == (20, 40)
    assert by_key[W_HELPER] == (40, 60)
    # the nested class's method is last in Widget.java, so it owns the rest
    assert by_key[W_INNER][0] == 60 and by_key[W_INNER][1] > 10000
    # harness classes and synthetic members never get a range
    assert not any(k.startswith('org.jfree.demo.FuzzHarness') for k in by_key)
    assert not any('lambda$' in k for k in by_key)


def test_a_throwing_method_reads_as_missed_from_probes_alone():
    """JaCoCo puts a method's probe after its exit, so a method that throws
    through its only call is reported as never executed.  `unusedHelper`
    is that method in the fixture."""
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    assert W_HELPER not in keys(cov.methods)


def test_a_stack_frame_recovers_that_method():
    """A frame is proof the method was entered.  The two sources union;
    the frame set never replaces the probe set."""
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    trace = ('java.lang.IllegalStateException: boom\n'
             '\tat org.jfree.demo.Widget.unusedHelper(Widget.java:41)\n'
             '\tat org.jfree.demo.FuzzHarness.fuzzerTestOneInput'
             '(FuzzHarness.java:5)\n')
    cov_mod.repair_from_frames(cov, JACOCO_XML, trace)
    assert keys(cov.frame_added) == {W_HELPER}
    assert W_HELPER in keys(cov.methods)
    assert W_HELPER not in keys(cov.methods_from_probes)
    assert cov.methods_from_probes < cov.methods
    # the harness's own frame resolves to nothing: it is not library code
    assert not any('FuzzHarness' in m.class_fq for m in cov.frame_methods)


def test_the_frame_line_tells_two_overloads_apart(tmp_path):
    """A frame carries no parameter types, so the LINE picks the overload.
    Matching on the name alone would credit both."""
    report = _overload_report(tmp_path)
    cov = cov_mod.parse_jacoco_xml(report)
    cov_mod.repair_from_frames(
        cov, report, '\tat org.jfree.demo.Solver.solve(Solver.java:73)\n')
    assert {m.strict_key for m in cov.frame_methods} == {
        'org.jfree.demo.Solver.solve(Object,double,double,double)'}


def test_a_frame_without_a_line_number_adds_nothing(tmp_path):
    """Without a line there is nothing to tell the overloads apart, so the
    frame is dropped rather than credited to a guess."""
    report = _overload_report(tmp_path)
    cov = cov_mod.parse_jacoco_xml(report)
    cov_mod.repair_from_frames(
        cov, report, '\tat org.jfree.demo.Solver.solve(Unknown Source)\n')
    assert cov.frame_methods == set()


def test_frames_outside_the_report_add_nothing():
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    before = set(cov.methods)
    cov_mod.repair_from_frames(cov, JACOCO_XML, (
        '\tat java.lang.String.charAt(String.java:100)\n'
        '\tat org.junit.runners.Suite.run(Suite.java:100)\n'
        '\tat com.code_intelligence.jazzer.Bits.go(Bits.java:12)\n'))
    assert cov.frame_methods == set() and cov.methods == before


def test_frame_methods_survive_a_round_trip_through_dict():
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    cov_mod.repair_from_frames(
        cov, JACOCO_XML,
        '\tat org.jfree.demo.Widget.unusedHelper(Widget.java:41)\n')
    back = Coverage.from_dict(json.loads(json.dumps(cov.to_dict())))
    assert back.methods == cov.methods
    assert back.frame_methods == cov.frame_methods
    assert back.methods_from_probes == cov.methods_from_probes
    assert back.frame_added == cov.frame_added


def test_a_coverage_file_written_before_the_repair_reads_as_all_probes():
    """An older `coverage_<build>.json` has neither key, and every method
    in it came from a probe.  It must not read back as "no probes"."""
    old = {'methods': [MethodRef('org.ex.A', 'a', ()).to_dict()],
           'lines': [], 'all_methods': []}
    back = Coverage.from_dict(old)
    assert back.methods == back.methods_from_probes
    assert back.frame_methods == set() and back.frame_added == set()


def test_collect_leg_repairs_each_build_from_its_own_fuzzer_output(fake_java,
                                                                   tmp_path):
    """The frames unioned into a build's coverage are the frames of THAT
    build's runs: `fuzz_out/<harness>_<build>.txt` carries the token."""
    leg = tmp_path / 'leg'
    cov_dir = leg / 'cov'
    cov_dir.mkdir(parents=True)
    for name in ('attempt_001_buggy.exec', 'attempt_001_patched.exec'):
        (cov_dir / name).write_text('')
    (cov_dir / 'classpath.json').write_text(json.dumps(
        {'class_dirs': [], 'source_dirs': [], 'include_glob': ''}))
    fo = leg / 'fuzz_out'
    fo.mkdir()
    (fo / 'attempt_001_buggy.txt').write_text(
        '\tat org.jfree.demo.Widget.unusedHelper(Widget.java:41)\n')
    (fo / 'attempt_001_patched.txt').write_text('no frames here\n')

    out = cov_mod.collect_leg(str(leg))

    assert keys(out['buggy'].frame_added) == {W_HELPER}
    assert W_HELPER in keys(out['buggy'].methods)
    assert out['patched'].frame_methods == set()
    assert W_HELPER not in keys(out['patched'].methods)
    # and it is in the file the metrics read
    written = Coverage.from_dict(json.loads(
        (leg / 'measurements' / 'coverage_buggy.json').read_text()))
    assert keys(written.frame_added) == {W_HELPER}


# --------------------------------------------------------- remeasure_leg

class _FakeHarnessRun:
    def __init__(self, report, trace=''):
        self.report = report
        self.trace = trace
        self.per_harness = []


@pytest.fixture
def fake_collect(monkeypatch, tmp_path):
    """Stand in for `d4j_rcc_sweep.collect`, the sweep package's runner.

    `remeasure_leg` imports it inside the function, so replacing the two
    functions on the real module is enough and nothing has to run
    Defects4J or Jazzer."""
    from java.measurements.d4j_rcc_sweep import collect

    seen = {}

    def _harness_coverage(buggy_dir, accepted, out_dir, includes='',
                          runs=20000, keep_going=1000, timeout_seconds=300):
        seen.update(buggy_dir=buggy_dir, accepted=accepted, out_dir=out_dir,
                    includes=includes, runs=runs, keep_going=keep_going)
        os.makedirs(out_dir, exist_ok=True)
        report = os.path.join(out_dir, 'jacoco.xml')
        with open(JACOCO_XML) as src, open(report, 'w') as dst:
            dst.write(src.read())
        return _FakeHarnessRun(report, seen.get('trace', ''))

    def _ensure_buggy_build(project, bug_id):
        seen['built'] = (project, bug_id)
        return str(tmp_path / f'{project}_{bug_id}_buggy')

    monkeypatch.setattr(collect, 'harness_coverage', _harness_coverage)
    monkeypatch.setattr(collect, 'ensure_buggy_build', _ensure_buggy_build)
    return seen


def _remeasure_leg(tmp_path, accepted=True, include_glob='org.jfree.**'):
    leg = tmp_path / 'leg'
    (leg / 'cov').mkdir(parents=True)
    (leg / 'cov' / 'classpath.json').write_text(json.dumps(
        {'class_dirs': [], 'source_dirs': [], 'include_glob': include_glob}))
    record = {'project': 'Chart', 'bug_id': '5', 'label': 'overfitting'}
    if accepted:
        record['accepted_harnesses'] = [
            {'harness_path': '/h/FuzzHarness.java', 'class_name': 'FuzzHarness',
             'classpath': '/cp', 'attempt_label': 'attempt_002'},
            {'harness_path': '/h/FuzzHarness.java', 'class_name': 'FuzzHarness',
             'classpath': '/cp', 'attempt_label': 'attempt_001'}]
    (leg / 'result.jsonl').write_text(json.dumps(record) + '\n')
    return leg


def test_remeasure_leg_runs_the_kept_set_on_a_fixed_input_budget(
        fake_collect, tmp_path):
    """`buggy` is the as-run coverage, under the pipeline's wall clock;
    `remeasure` is the same harnesses re-run with `-runs=N`, so the number
    does not move with the load on the machine."""
    leg = _remeasure_leg(tmp_path)
    cov = cov_mod.remeasure_leg(str(leg), runs=20000, keep_going=1000)

    assert fake_collect['runs'] == 20000
    assert fake_collect['keep_going'] == 1000
    assert fake_collect['includes'] == 'org.jfree.**'
    assert [e['attempt_label'] for e in fake_collect['accepted']] == [
        'attempt_002', 'attempt_001']
    assert fake_collect['out_dir'] == str(leg / 'cov' / 'remeasure')
    assert fake_collect['built'] == ('Chart', '5')

    assert cov.build == 'remeasure'
    assert cov.harness == 'attempt_001+attempt_002'
    # the include glob from classpath.json was applied at parse time
    assert OTHER_GO not in keys(cov.all_methods)
    written = Coverage.from_dict(json.loads(
        (leg / 'measurements' / 'coverage_remeasure.json').read_text()))
    assert written.build == 'remeasure' and written.methods == cov.methods


def test_remeasure_leg_repairs_its_own_run_from_its_own_frames(fake_collect,
                                                               tmp_path):
    leg = _remeasure_leg(tmp_path, include_glob='')
    fake_collect['trace'] = (
        '\tat org.jfree.demo.Widget.unusedHelper(Widget.java:41)\n')
    cov = cov_mod.remeasure_leg(str(leg))
    assert keys(cov.frame_added) == {W_HELPER}


def test_remeasure_leg_without_accepted_harnesses_returns_none(fake_collect,
                                                               tmp_path):
    """A leg archived before `accepted_harnesses` existed, or one that
    accepted none: there is no set to re-run, which is not a set of
    nothing."""
    leg = _remeasure_leg(tmp_path, accepted=False)
    assert cov_mod.remeasure_leg(str(leg)) is None
    assert not (leg / 'measurements').exists()


def test_remeasure_is_a_build_token_collect_leg_never_produces(fake_java,
                                                               tmp_path):
    """`remeasure` is a re-run, not a reading of what the leg left behind,
    so scanning a leg's `cov/` must not invent one."""
    leg = tmp_path / 'leg'
    (leg / 'cov' / 'remeasure').mkdir(parents=True)
    (leg / 'cov' / 'remeasure' / 'attempt_001.exec').write_text('')
    (leg / 'cov' / 'classpath.json').write_text(json.dumps(
        {'class_dirs': [], 'source_dirs': [], 'include_glob': ''}))
    assert cov_mod.collect_leg(str(leg)) == {}
