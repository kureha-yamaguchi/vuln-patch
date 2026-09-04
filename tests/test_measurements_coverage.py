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
    monkeypatch.delenv('JACOCO_CLI_JAR', raising=False)
    seen = {}

    def _fetch(url, dest):
        seen['url'], seen['dest'] = url, dest
        open(dest, 'w').write('jar')

    monkeypatch.setattr(cov_mod.urllib.request, 'urlretrieve', _fetch)
    jar = cov_mod.ensure_jacoco_cli(cache_dir=str(tmp_path / 'cache'))
    assert jar.endswith('-nodeps.jar')
    assert seen['url'].startswith('https://repo1.maven.org/maven2/org/jacoco/')
    assert seen['dest'] == jar


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
    # one report per .exec, and an XML cached next to it
    assert len(fake_java) == 3
    assert (cov_dir / 'attempt_002_buggy.xml').is_file()
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


def test_the_real_runner_is_a_plain_subprocess_call():
    """Guard the seam the tests monkeypatch: `_run` must stay the only
    place a process is started."""
    assert cov_mod._run.__module__ == cov_mod.__name__
    assert 'subprocess' in cov_mod.__dict__
    assert cov_mod.subprocess is subprocess
