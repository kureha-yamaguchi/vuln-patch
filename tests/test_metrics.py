"""RCC at function level — the three things that can silently break it.

RCC is a set intersection, so it fails in exactly three ways, and each one
produces a plausible-looking number rather than an error:

  (a) R-hat names a method one way and the JaCoCo report names it another.
      Every bug then reads RCC = 0, which looks like a real finding.
  (b) A missing or empty coverage report is read as "reached nothing"
      instead of as an infrastructure error.
  (c) The triggering-test gate lets a broken bug into the population.

One test group each. The Java fixtures are the diffcov ones, so the region
side is exercised against real javalang output (constructors, overloads,
arrays, a fields-only patch).
"""
import os
import shutil
import sys

import pytest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, 'src'))

from java.measurements.d4j_rcc_sweep import crashes, reached, scores             # noqa: E402
from java.measurements.d4j_rcc_sweep import region as region_mod                 # noqa: E402
from java.measurements.d4j_rcc_sweep.keys import (MethodKey, key_from_mangled,   # noqa: E402
                          normalise_type)

FIXTURES = os.path.join(ROOT, 'tests', 'fixtures')
WIDGET_REL = 'source/org/example/Widget.java'
GADGET_REL = 'source/org/example/Gadget.java'

# One class element, as jacococli writes it. `hit` decides whether the
# method's only line is covered, which is what F(H) membership means.
_REPORT = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<report name="fixture">
  <package name="org/example">
    <class name="org/example/Widget" sourcefilename="Widget.java">
      {methods}
    </class>
    <sourcefile name="Widget.java">
      {lines}
    </sourcefile>
  </package>
</report>
"""


@pytest.fixture
def tree_dir(tmp_path):
    """A post-patch working copy laid out the way a real checkout is."""
    for rel, fixture in ((WIDGET_REL, 'diffcov_widget.java'),
                         (GADGET_REL, 'diffcov_gadget.java')):
        dst = tmp_path / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(os.path.join(FIXTURES, fixture), dst)
    return str(tmp_path)


def _patch(name: str) -> str:
    with open(os.path.join(FIXTURES, f'diffcov_{name}.patch')) as fh:
        return fh.read()


def _region(name: str, tree_dir: str):
    return region_mod.region_from_patch(_patch(name), tree_dir)


def _report(tmp_path, methods, name='cov'):
    """Write a jacoco.xml holding `methods` — (name, desc, line, hit)."""
    out_dir = tmp_path / name
    out_dir.mkdir()
    method_xml, line_xml, line_no = [], [], 100
    for mname, desc, hit in methods:
        # `<init>` is a legal JVM method name but not a legal XML attribute
        # value; jacococli escapes it, so the fixture must too.
        escaped = mname.replace('<', '&lt;').replace('>', '&gt;')
        method_xml.append(
            f'<method name="{escaped}" desc="{desc}" line="{line_no}">'
            f'<counter type="LINE" missed="0" covered="1"/></method>')
        line_xml.append(f'<line nr="{line_no}" ci="{1 if hit else 0}"/>')
        line_no += 10
    path = out_dir / 'jacoco.xml'
    path.write_text(_REPORT.format(methods='\n'.join(method_xml),
                                   lines='\n'.join(line_xml)))
    return str(path)


# --- (a) the two naming schemes must meet --------------------------------

def test_a_source_and_descriptor_spell_the_same_method(tmp_path, tree_dir):
    """`indexOf(String, int)` from the AST and `(Ljava/lang/String;I)I` from
    the descriptor are one method. If they are not, every RCC reads 0."""
    region = _region('overload', tree_dir)
    report = _report(tmp_path, [
        ('indexOf', '(Ljava.lang.String;I)I', True),
    ])
    result = scores.root_cause_coverage(region,
                                     reached.reached_from_report(report))
    assert result.value == 1.0
    assert not result.by_arity_only, 'this must match exactly, not by arity'


def test_a_the_other_overload_is_not_counted(tmp_path, tree_dir):
    """`indexOf(Object)` is a different method. Parameter types are part of
    the key precisely so a same-named overload cannot be credited."""
    region = _region('overload', tree_dir)
    report = _report(tmp_path, [('indexOf', '(Ljava.lang.Object;)I', True)])
    result = scores.root_cause_coverage(region,
                                     reached.reached_from_report(report))
    assert result.value == 0.0


def test_a_a_constructor_is_init_on_the_jacoco_side(tmp_path, tree_dir):
    """javalang names a constructor after its class; the JVM calls it
    `<init>`. Both Widget constructors are in this patch."""
    region = _region('constructor', tree_dir)
    assert region.size == 2
    report = _report(tmp_path, [
        ('<init>', '()V', True),
        ('<init>', '(I)V', True),
    ])
    result = scores.root_cause_coverage(region,
                                     reached.reached_from_report(report))
    assert result.value == 1.0


def test_a_an_uncovered_method_is_missed_not_absent(tmp_path, tree_dir):
    """A method the report KNOWS about but never ran is a miss. A zero here
    is the reading the metric exists to produce."""
    region = _region('overload', tree_dir)
    report = _report(tmp_path, [('indexOf', '(Ljava.lang.String;I)I', False)])
    result = scores.root_cause_coverage(region,
                                     reached.reached_from_report(report))
    assert result.value == 0.0
    assert [str(k) for k in result.missed] == ['Widget.indexOf(String, int)']


def test_a_varargs_and_nested_types_normalise_to_one_spelling():
    """The two sides disagree on three spellings, and only these three."""
    assert normalise_type('int...') == 'int[]'
    assert normalise_type('java.lang.String') == 'String'
    assert normalise_type('java.util.Map$Entry') == 'Entry'


def test_a_a_name_without_a_receiver_is_dropped():
    """A JDK static carries no `[pkg.Class]` bracket and can never be in
    R-hat, so it must not become a key."""
    assert key_from_mangled('Math.abs(int)') is None
    assert key_from_mangled('[org.example.Widget].indexOf(int)') == MethodKey(
        'org.example.Widget', 'indexOf', ('int',))


# --- (b) no coverage is an error, never a zero ---------------------------

def test_b_a_missing_report_raises_rather_than_reading_as_zero(tmp_path):
    with pytest.raises(reached.CoverageUnavailable):
        reached.reached_from_report(str(tmp_path / 'jacoco.xml'))


def test_b_a_report_that_decodes_to_nothing_raises(tmp_path):
    """An empty report means the classes carried no debug information, or
    Jazzer instrumented nothing. Both are broken plumbing, not a result."""
    report = _report(tmp_path, [])
    with pytest.raises(reached.CoverageUnavailable):
        reached.reached_from_report(report)


def test_b_a_missing_exec_dump_raises(tmp_path):
    """Jazzer writes the dump from a shutdown hook, so a hard kill leaves
    none. That is an infrastructure error."""
    with pytest.raises(reached.CoverageUnavailable):
        reached.exec_to_xml([str(tmp_path / 'none.exec')],
                            classfiles=str(tmp_path),
                            out_dir=str(tmp_path / 'out'))


# --- (c) the gate keeps broken bugs out of the population ----------------

def test_c_the_gate_passes_when_the_trigger_test_runs_the_region(
        tmp_path, tree_dir):
    region = _region('overload', tree_dir)
    report = _report(tmp_path, [('indexOf', '(Ljava.lang.String;I)I', True)])
    gate = scores.trigger_gate(region, reached.reached_from_report(report))
    assert gate.passed


def test_c_the_gate_fails_when_the_trigger_test_misses_the_region(
        tmp_path, tree_dir):
    """The triggering test fails BECAUSE of the changed code, so it must run
    it. A miss here means R-hat or the plumbing is wrong — and without this
    gate that fault would read as RCC = 0 on every bug."""
    region = _region('overload', tree_dir)
    report = _report(tmp_path, [('indexOf', '(Ljava.lang.String;I)I', False)])
    gate = scores.trigger_gate(region, reached.reached_from_report(report))
    assert not gate.passed
    assert 'indexOf' in gate.detail


def test_c_a_fields_only_fix_leaves_the_population(tree_dir):
    """A fix that changes no method body gives an empty R-hat. RCC is then
    undefined, not zero."""
    region = _region('fields_only', tree_dir)
    assert region.is_empty
    assert region.unmapped
    assert scores.root_cause_coverage(region, set()).value is None
    assert not scores.trigger_gate(region, set()).passed


# --- (d) the probe limitation, and the frames that repair it -------------

def test_d_a_throwing_method_reads_as_missed_from_probes_alone(tmp_path):
    """JaCoCo puts a method's probe after its exit, so a method whose body
    is `return other(x);` reads as MISSED when `other` throws. Math-70 is
    the recorded case: the stack trace names line 72 and JaCoCo reports
    line 72 as never covered."""
    report = _report(tmp_path, [('solve', '(Ljava.lang.Object;DDD)D', False)])
    assert reached.reached_from_report(report) == set()


def test_d_a_stack_frame_recovers_that_method(tmp_path):
    """A frame is proof the method was entered. It is unioned with the
    probes, never substituted for them."""
    report = _report(tmp_path, [('solve', '(Ljava.lang.Object;DDD)D', False)])
    trace = ('java.lang.NullPointerException\n'
             '\tat org.example.Widget.solve(Widget.java:100)\n')
    found = reached.reached_from_stack(report, trace)
    assert [str(k) for k in found] == ['Widget.solve(Object, double, double, '
                                       'double)']


def test_d_the_frame_line_tells_two_overloads_apart(tmp_path):
    """A frame carries no parameter types, so the LINE resolves the
    overload. Matching on the name alone would credit both."""
    report = _report(tmp_path, [
        ('solve', '(Ljava.lang.Object;DDD)D', False),   # line 100
        ('solve', '(DD)D', False),                      # line 110
    ])
    trace = '\tat org.example.Widget.solve(Widget.java:110)\n'
    found = reached.reached_from_stack(report, trace)
    assert [str(k) for k in found] == ['Widget.solve(double, double)']


def test_d_library_and_engine_frames_are_dropped(tmp_path):
    report = _report(tmp_path, [('solve', '(DD)D', False)])
    trace = ('\tat java.lang.String.charAt(String.java:100)\n'
             '\tat org.junit.runners.Suite.run(Suite.java:100)\n'
             '\tat com.code_intelligence.jazzer.Bits.go(Bits.java:100)\n')
    assert reached.stack_frames(trace) == set()
    assert reached.reached_from_stack(report, trace) == set()


def test_d_no_trace_adds_nothing(tmp_path):
    report = _report(tmp_path, [('solve', '(DD)D', True)])
    assert reached.reached_from_stack(report, '') == set()


# --- (e) the other three set metrics, and their denominators -------------

def _keys(*specs):
    """A set of MethodKeys from `Class.method(T1, T2)` style specs."""
    made = set()
    for spec in specs:
        head, _, tail = spec.partition('(')
        cls, _, name = head.rpartition('.')
        params = [p.strip() for p in tail.rstrip(')').split(',') if p.strip()]
        made.add(MethodKey(cls, name, tuple(params)))
    return made


def test_e_each_metric_uses_its_own_denominator(tree_dir):
    """RCC and RCP share a numerator. Only the denominator separates them,
    and mixing the two is the easiest error to make here."""
    region = _region('overload', tree_dir)          # Widget.indexOf(String, int)
    patch = _keys('org.example.Widget.indexOf(String, int)',
                  'org.example.Widget.resize(int)')
    fuzzer = _keys('org.example.Widget.indexOf(String, int)',
                   'org.example.Widget.resize(int)',
                   'org.example.Widget.paint()',
                   'org.example.Widget.clear()')
    sets = scores.set_metrics(region, patch, fuzzer)
    assert sets.rcc == 1.0          # 1 of 1 method in R-hat
    assert sets.rcr == 1.0          # P recovers that method
    assert sets.rcp == 0.25         # 1 of the 4 methods F(H) ran
    assert sets.psc == 1.0          # both members of P were run


def test_e_recovery_is_static_and_ignores_the_fuzzer(tree_dir):
    """RCR scores the EXTRACTION. A harness set that ran everything cannot
    raise it, and one that ran nothing cannot lower it."""
    region = _region('overload', tree_dir)
    patch = _keys('org.example.Widget.resize(int)')
    assert scores.set_metrics(region, patch, set()).rcr == 0.0
    assert scores.set_metrics(region, patch, region.keys).rcr == 0.0


def test_e_an_empty_denominator_is_undefined_not_zero(tree_dir):
    """An empty P leaves PSC's population. Reading it as 0.0 would put a
    failed static analysis into the mean as a bad harness set."""
    region = _region('overload', tree_dir)
    sets = scores.set_metrics(region, set(), set())
    assert sets.psc is None
    assert sets.rcp is None
    assert sets.rcr == 0.0          # R-hat is not empty, so RCR is defined


# --- (f) the crashes, and where they happened ----------------------------

_HARNESS = 'org.example.FuzzHarness'


def _crash_output(*blocks) -> str:
    return '\n'.join(blocks)


def test_f_one_banner_is_one_crash():
    text = _crash_output(
        '== Java Exception: java.lang.IllegalStateException: a\n'
        '\tat org.example.Widget.solve(Widget.java:100)\n',
        '== Java Exception: java.lang.IllegalStateException: b\n'
        '\tat org.example.Widget.solve(Widget.java:110)\n')
    assert len(crashes.crash_blocks(text)) == 2


def test_f_the_site_is_the_deepest_cause_not_the_harness_alarm():
    """A harness that catches a library throwable and rethrows its own
    alarm puts ITSELF at the top. The deepest `Caused by:` is the fault."""
    block = ('== Java Exception: java.lang.RuntimeException: [oracle:x]\n'
             '\tat org.example.FuzzHarness.check(FuzzHarness.java:85)\n'
             'Caused by: java.lang.IndexOutOfBoundsException: -1\n'
             '\tat java.util.ArrayList.add(ArrayList.java:479)\n'
             '\tat org.example.Widget.solve(Widget.java:100)\n'
             '\tat org.example.FuzzHarness.check(FuzzHarness.java:78)\n')
    assert crashes.site_frame(block, [_HARNESS]) == ('org.example.Widget',
                                                     'solve', 100)


def test_f_a_harness_only_crash_has_no_site():
    """An oracle that fires on a wrong VALUE names no library method. That
    is a real outcome, and it must not be confused with a lookup failure."""
    block = ('== Java Exception: java.lang.RuntimeException: [oracle:y]\n'
             '\tat org.example.FuzzHarness.fuzzerTestOneInput'
             '(FuzzHarness.java:64)\n')
    assert crashes.site_frame(block, [_HARNESS]) is None


def test_f_csm_counts_only_crashes_inside_the_region(tmp_path, tree_dir):
    """CSM's denominator is every crash, including the ones with no site."""
    region = _region('overload', tree_dir)     # Widget.indexOf(String, int)
    report = _report(tmp_path, [
        ('indexOf', '(Ljava.lang.String;I)I', True),    # line 100
        ('resize', '(I)V', True),                       # line 110
    ])
    text = _crash_output(
        '== Java Exception: java.lang.IllegalStateException: in region\n'
        '\tat org.example.Widget.indexOf(Widget.java:100)\n',
        '== Java Exception: java.lang.IllegalStateException: elsewhere\n'
        '\tat org.example.Widget.resize(Widget.java:110)\n',
        '== Java Exception: java.lang.RuntimeException: [oracle:z]\n'
        '\tat org.example.FuzzHarness.fuzzerTestOneInput'
        '(FuzzHarness.java:64)\n')
    found = crashes.crashes(report, text, [_HARNESS])
    match = scores.crash_site_match(region, found)
    assert (match.total, match.matched) == (3, 1)
    assert (match.off_region, match.no_frame, match.unresolved) == (1, 1, 0)
    assert match.value == pytest.approx(1 / 3)


def test_f_no_crash_leaves_csm_undefined(tmp_path, tree_dir):
    region = _region('overload', tree_dir)
    report = _report(tmp_path, [('indexOf', '(Ljava.lang.String;I)I', True)])
    assert crashes.crashes(report, 'no findings here') == []
    assert scores.crash_site_match(region, []).value is None
