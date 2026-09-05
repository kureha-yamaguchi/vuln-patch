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

from metrics import rcc, reached, region as region_mod   # noqa: E402
from metrics.keys import (MethodKey, key_from_mangled,   # noqa: E402
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
    result = rcc.root_cause_coverage(region,
                                     reached.reached_from_report(report))
    assert result.value == 1.0
    assert not result.by_arity_only, 'this must match exactly, not by arity'


def test_a_the_other_overload_is_not_counted(tmp_path, tree_dir):
    """`indexOf(Object)` is a different method. Parameter types are part of
    the key precisely so a same-named overload cannot be credited."""
    region = _region('overload', tree_dir)
    report = _report(tmp_path, [('indexOf', '(Ljava.lang.Object;)I', True)])
    result = rcc.root_cause_coverage(region,
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
    result = rcc.root_cause_coverage(region,
                                     reached.reached_from_report(report))
    assert result.value == 1.0


def test_a_an_uncovered_method_is_missed_not_absent(tmp_path, tree_dir):
    """A method the report KNOWS about but never ran is a miss. A zero here
    is the reading the metric exists to produce."""
    region = _region('overload', tree_dir)
    report = _report(tmp_path, [('indexOf', '(Ljava.lang.String;I)I', False)])
    result = rcc.root_cause_coverage(region,
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
    gate = rcc.trigger_gate(region, reached.reached_from_report(report))
    assert gate.passed


def test_c_the_gate_fails_when_the_trigger_test_misses_the_region(
        tmp_path, tree_dir):
    """The triggering test fails BECAUSE of the changed code, so it must run
    it. A miss here means R-hat or the plumbing is wrong — and without this
    gate that fault would read as RCC = 0 on every bug."""
    region = _region('overload', tree_dir)
    report = _report(tmp_path, [('indexOf', '(Ljava.lang.String;I)I', False)])
    gate = rcc.trigger_gate(region, reached.reached_from_report(report))
    assert not gate.passed
    assert 'indexOf' in gate.detail


def test_c_a_fields_only_fix_leaves_the_population(tree_dir):
    """A fix that changes no method body gives an empty R-hat. RCC is then
    undefined, not zero."""
    region = _region('fields_only', tree_dir)
    assert region.is_empty
    assert region.unmapped
    assert rcc.root_cause_coverage(region, set()).value is None
    assert not rcc.trigger_gate(region, set()).passed


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
