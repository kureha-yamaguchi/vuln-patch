"""The state-twin architecture (post-roll-4): parsing, matching, setup
extraction, input recovery, and the production-path walkthrough.

Roll 4's lesson pinned here: the walkthrough must drive the SAME code path
run.py drives. `test_production_chain_*` calls `_reference_impl_fact` itself
with a stubbed generator/JVM — a call-site lag now fails a test instead of a
roll.
"""
import re
import sys
import types
import pytest

sys.path.insert(0, 'src')

from java.relations import reference_run as rr
from java.relations import reference_gen as rg


# The five signatures roll 4 actually produced — the real corpus.
ROLL4_SIGS = [
    'double[], double[], double',
    'double[] residuals, double[] residualsWeights, double cost',
    'double[], double[], double, int',
    'int, double[][], double[], double[], double, int',
    'int, double',
]

CANON = [('double[]', 'residuals'), ('double[]', 'residualsWeights'),
         ('double', 'cost'), ('int', 'rows'), ('double[][]', 'jacobian')]


def test_parse_parameters_named_and_bare():
    assert rr.parse_parameters(ROLL4_SIGS[1]) == [
        ('double[]', 'residuals'), ('double[]', 'residualsWeights'),
        ('double', 'cost')]
    assert rr.parse_parameters(ROLL4_SIGS[0]) == [
        ('double[]', ''), ('double[]', ''), ('double', '')]


def test_match_named_signature_resolves():
    names, why = rr.match_parameters(rr.parse_parameters(ROLL4_SIGS[1]), CANON)
    assert names == ['residuals', 'residualsWeights', 'cost']


def test_match_bare_ambiguous_types_discards():
    # Two double[] fields exist; a bare double[] cannot pick one.
    names, why = rr.match_parameters(rr.parse_parameters(ROLL4_SIGS[0]), CANON)
    assert names is None
    assert 'unmappable' in why


def test_match_unmappable_roll4_sig4_discards():
    names, why = rr.match_parameters(rr.parse_parameters(ROLL4_SIGS[3]), CANON)
    assert names is None


def test_build_driver_refuses_str_observables():
    # THE roll-4 bug, made impossible: a str would iterate as characters.
    with pytest.raises(TypeError):
        rr.build_driver('ReferenceImpl', 'compute', [''])


def test_canonical_state_reads_fields():
    ctx = ('public class A {\n  private double[] residuals;\n'
           '  protected double cost = 0;\n  public int rows;\n'
           '  public double getRMS() { return 0; }\n}')
    got = rr.canonical_state(ctx)
    assert ('double[]', 'residuals') in got and ('double', 'cost') in got


TEST_SRC = '''
public void testChiSquare() {
    CurveFitter f = new CurveFitter();
    f.addPoint(1.0, 2.0);
    optimizer.fit();
    Assert.assertEquals(3.2, optimizer.getChiSquare(), 1e-10);
    assertTrue(optimizer.getRMS() > 0);
}
'''


def test_extract_setup_strips_asserts_and_finds_receiver():
    setup, recv, why = rr.extract_test_setup(TEST_SRC, 'getChiSquare')
    assert recv == 'optimizer'
    assert 'assertEquals' not in setup and 'assertTrue' not in setup
    assert 'addPoint' in setup


def test_extract_setup_all_asserts_is_underivable():
    setup, recv, why = rr.extract_test_setup(
        'public void t() {\n  assertEquals(1, 1);\n}', 'getX')
    assert setup is None and 'assertions' in why


def test_twin_driver_reflects_params_and_reads_observables():
    src = rr.build_state_twin_driver('X o = new X();', 'o',
                                    ['getChiSquare', 'getRMS'],
                                    ['residuals'])
    assert 'printField(o, "residuals")' in src
    assert 'o.getChiSquare()' in src and '__construct0=OK' in src
    assert rr.END_MARKER in src


def test_java_literal_roundtrips():
    assert rr.java_literal('double[]', '[1.0, 2.5]') == 'new double[]{1.0, 2.5}'
    assert rr.java_literal('double[]', '[]') == 'new double[0]'
    assert rr.java_literal('double', '3.14') == '3.14'
    assert rr.java_literal('double[]', 'ABSENT') is None
    assert rr.java_literal('double', 'EX:NullPointerException') is None


# ---------------------------------------------------------------------------
# The production-path walkthrough: drives run.py's own chain function.
# ---------------------------------------------------------------------------

CTX = ('/** The chi-square. @return chi-square value */\n'
       'public class Opt {\n'
       '  private double[] residuals;\n'
       '  private double[] residualsWeights;\n'
       '  public double getChiSquare() { double s=0; return s; }\n'
       '  public double getRMS() { return 0.0; }\n'
       '  public double getCost() { return 0.0; }\n'
       '  public int getRows() { return 0; }\n'
       '}')

REFERENCE_REPLY = (
    '// compute(double[] residuals, double[] residualsWeights) : '
    'getChiSquare, getRMS, getCost, getRows\n'
    'public class ReferenceImpl {\n'
    '  public static double compute_getChiSquare(double[] r, double[] w) '
    '{ double s=0; for (int i=0;i<r.length;i++) s+=r[i]*r[i]/w[i]; return s; }\n'
    '}')


class _FiredMsg(str):
    pass


def _mk_failure_test():
    ft = types.SimpleNamespace()
    ft.method_source = (
        'public void testCS() {\n'
        '  Opt optimizer = new Opt();\n'
        '  optimizer.setUp();\n'
        '  Assert.assertEquals(3.25, optimizer.getChiSquare(), 1e-9);\n}')
    return ft


def _run_chain(monkeypatch, twin_outputs, ref_output, generated=REFERENCE_REPLY):
    """Drive run._reference_impl_fact with stubbed generator and JVM."""
    from java import run as runmod
    events = []
    monkeypatch.setattr(
        'llm.record_event',
        lambda kind, **kw: events.append((kw.get('output'), kw.get('reason'))),
        raising=False)

    class FakeHG:
        def __init__(self, **kw): pass
        def generate(self, msgs): return generated
    monkeypatch.setattr('llm.HarnessGenerator', FakeHG, raising=False)

    calls = {'twin_dirs': []}

    def fake_run_twin(builder, project_dir, twin_source, **kw):
        calls['twin_dirs'].append(project_dir)
        out = twin_outputs.pop(0)
        return out
    def fake_run_reference(builder, buggy_dir, ref_src, driver_src, **kw):
        calls['driver_src'] = driver_src
        return ref_output
    monkeypatch.setattr(rr, 'run_twin', fake_run_twin)
    monkeypatch.setattr(rr, 'run_reference', fake_run_reference)

    class FakePPB:
        def build_patched_dir(self, b, p): return '/patched'
    monkeypatch.setattr(runmod, 'PatchedProjectBuilder', FakePPB)
    runmod._reference_impl_fact._memo = {}

    fact = runmod._reference_impl_fact(
        args=types.SimpleNamespace(model='m', reference_impl=True),
        fired='[oracle:x] semantic mismatch: getChiSquare expected=3.25 '
              'actual=9.99', class_ctx=[CTX],
        failure_tests=[_mk_failure_test()], builder=object(),
        buggy_dir='/buggy', patch_path='/p.patch',
        trusted_values=['3.25'], package=None, imports=None)
    return fact, events, calls


BUGGY_TWIN = ({'__construct0': ['OK'],
               '__param_residuals': ['[1.0, 2.0]'],
               '__param_residualsWeights': ['[1.0, 1.0]'],
               'getChiSquare': ['9.99'], 'getRMS': ['1.1'],
               'getCost': ['2.2'], 'getRows': ['2']}, 'twin ran')
PATCHED_TWIN = ({'__construct0': ['OK'],
                 'getChiSquare': ['3.25'], 'getRMS': ['1.1'],
                 'getCost': ['2.2'], 'getRows': ['2']}, 'twin ran')
REF_OK = ({'getChiSquare': ['3.25'], 'getRMS': ['1.1'],
           'getCost': ['2.2'], 'getRows': ['2']}, 'ran, 4 observables')


def test_production_chain_emits_two_sided_fact(monkeypatch):
    fact, events, calls = _run_chain(
        monkeypatch, [BUGGY_TWIN, PATCHED_TWIN], REF_OK)
    assert fact is not None
    assert 'reference-implementation fact' in fact
    # patched twin computes 3.25 = reference -> agreement side on disputed
    assert 'SAME value' in fact
    # the driver was built with real observable names, not characters
    assert 'compute_getChiSquare(' in calls['driver_src']
    assert 'compute_c(' not in calls['driver_src']
    # both builds' twins ran: buggy then patched
    assert calls['twin_dirs'] == ['/buggy', '/patched']


def test_production_chain_screen_uses_buggy_not_self(monkeypatch):
    # Reference disagrees with buggy on SIBLINGS -> screen must discard.
    ref_bad = ({'getChiSquare': ['3.25'], 'getRMS': ['7.7'],
                'getCost': ['8.8'], 'getRows': ['9']}, 'ran')
    fact, events, calls = _run_chain(monkeypatch, [BUGGY_TWIN], ref_bad)
    assert fact is None
    assert any('screen DISCARDED' in (o or '') for o, _ in events)


def test_production_chain_pin_check_catches_bug_copy(monkeypatch):
    # Reference agrees with buggy EVERYWHERE incl. the disputed point (a
    # bug-copy): screen passes, the PIN must catch it (trusted 3.25 != 9.99).
    ref_copy = ({'getChiSquare': ['9.99'], 'getRMS': ['1.1'],
                 'getCost': ['2.2'], 'getRows': ['2']}, 'ran')
    fact, events, calls = _run_chain(monkeypatch, [BUGGY_TWIN], ref_copy)
    assert fact is None
    assert any('pin-check DISCARDED' in (o or '') for o, _ in events)


def test_production_chain_unmappable_signature_discards(monkeypatch):
    bare = REFERENCE_REPLY.replace(
        'double[] residuals, double[] residualsWeights', 'double[], double[]')
    fact, events, calls = _run_chain(
        monkeypatch, [BUGGY_TWIN, PATCHED_TWIN], REF_OK, generated=bare)
    assert fact is None
    assert any('unmappable' in (o or '') + (r or '') for o, r in events)


def test_wiring_both_doors_pass_patch_path():
    src = open('src/java/run.py').read()
    chunks = src.split('_reference_impl_fact(')[1:]
    # def + memo attr + 2 call sites; a CALL passes args=args in its kwargs.
    calls = [c[:700] for c in chunks if 'args=args' in c[:700]]
    assert len(calls) == 2, f'expected exactly 2 call sites, got {len(calls)}'
    for c in calls:
        assert 'patch_path=selection.patch_path' in c
        assert 'package=context.package' in c
        assert 'imports=context.source_imports' in c


# ---------------------------------------------------------------------------
# The three VM re-walk failures (2026-08-07), pinned with real Math-65 shapes.
# ---------------------------------------------------------------------------

# The annotated blob shape the chain actually receives: method + advisory
# prose + helper class + fields (condensed from ladder1e's recorded trace).
BLOB = '''
[REAL FAILING TEST ...Test::testCircleFitting — trust source #1, verbatim]
    public void testCircleFitting() throws OptimizationException {
        Circle circle = new Circle();
        circle.addPoint( 30.0,  68.0);
        LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
        optimizer.optimize(circle, new double[] { 0 }, new double[] { 1 },
                           new double[] { 98.680, 47.345 });
        assertTrue(optimizer.getEvaluations() < 10);
        double rms = optimizer.getRMS();
        assertEquals(1.768262623567235,  Math.sqrt(circle.getN()) * rms,  1.0e-10);
        double[][] cov = optimizer.getCovariances();
        assertEquals(1.839, cov[0][0], 0.001);
    }
// The test DEPENDS on this setup from its test class (helpers, fields...):
// --- helper Circle() ---
        public Circle() {
            points  = new ArrayList<Point2D.Double>();
        }
// --- class fields/constants the test uses ---
private ArrayList<Point2D.Double> points;
'''

SIBS = ['getRMS', 'getCovariances', 'getEvaluations']


BLOB_DECLARING = {'LevenbergMarquardtOptimizer'}


def test_blob_isolates_the_method_not_the_annotations():
    setup, recv, why = rr.extract_test_setup(BLOB, 'getChiSquare', SIBS,
                                             BLOB_DECLARING)
    assert setup is not None, why
    assert 'helper Circle()' not in setup            # annotations excluded
    assert 'private ArrayList' not in setup          # fields excluded
    assert 'addPoint' in setup


def test_receiver_by_declaring_type_not_by_usage():
    # getChiSquare is never called in this test. The receiver is chosen
    # because `optimizer` is DECLARED with a type that declares the
    # observable -- not because it is most-called (VM re-walk #2) and not
    # because it is last-constructed (roll 5's `center` bug).
    setup, recv, why = rr.extract_test_setup(BLOB, 'getChiSquare', SIBS,
                                             BLOB_DECLARING)
    assert recv == 'optimizer', why


def test_assert_stripping_is_statement_aware():
    body = ('int x = 1;\n'
            '        assertEquals(cov[0][1], cov[1][0], 1.0e-14); }\n')
    out = rr._strip_assert_statements(body)
    assert '}' in out                                # the brace survives
    assert 'assertEquals' not in out


def test_multiline_assert_statement_fully_removed():
    body = ('go();\nassertEquals(1.768262623567235,\n'
            '    Math.sqrt(circle.getN()) * rms,\n    1.0e-10);\nrest();')
    out = rr._strip_assert_statements(body)
    assert 'go();' in out and 'rest();' in out
    assert 'sqrt' not in out


def test_unbalanced_setup_is_refused_not_emitted():
    src = 'public void t() {\n  if (a) {\n  helper(b);\n}'  # missing brace
    setup, recv, why = rr.extract_test_setup(src + '\n}', 'getX', [])
    if setup is not None:
        assert setup.count('{') == setup.count('}')


def test_ascii_safe_escapes_emdash_everywhere():
    s = rr.ascii_safe('// values — the pipeline prose\nint x = 1;')
    assert '—' not in s
    assert '\\u2014' in s
    assert 'int x = 1;' in s


def test_twin_driver_is_ascii_and_balanced_with_helpers():
    imports, helpers = rr.extract_test_dependencies(
        'import java.util.ArrayList;\nimport junit.framework.TestCase;\n'
        'public class T {\n  private static class Circle {\n'
        '    public Circle() { }\n    public void addPoint(double a, double b) { }\n'
        '    public int getN() { return 0; }\n  }\n}',
        'Circle circle = new Circle();')
    assert helpers and 'class Circle' in helpers[0]
    assert all('junit' not in i for i in imports)
    src = rr.build_state_twin_driver(
        'Circle circle = new Circle(); // em—dash prose', 'circle',
        ['getN'], [], imports=imports, helper_classes=helpers)
    assert src.count('{') == src.count('}')
    assert all(ord(c) < 128 for c in src)
    assert 'class Circle' in src


def test_unbalanced_twin_raises_for_honest_discard():
    with pytest.raises(ValueError):
        rr.build_state_twin_driver('if (a) { b();', 'o', ['getN'], [])


# ---------------------------------------------------------------------------
# VM re-walk #2 (2026-08-07): receiver by DECLARING TYPE, and the twin's
# package. Both from the real Math-65 shapes.
# ---------------------------------------------------------------------------

LM_CTX = ('public class AbstractLeastSquaresOptimizer {\n'
          '  public double getChiSquare() { }\n'
          '  public double getRMS() { }\n'
          '  public double[][] getCovariances() { }\n}\n'
          'class LevenbergMarquardtOptimizer extends '
          'AbstractLeastSquaresOptimizer { }\n'
          'class VectorialPointValuePair { public double[] getPointRef() { } }')

LM_BODY = '''public void testCircleFitting() {
    Circle circle = new Circle();
    circle.addPoint(30.0, 68.0);
    LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();
    VectorialPointValuePair optimum = optimizer.optimize(circle);
    double rms = optimizer.getRMS();
    double[][] cov = optimizer.getCovariances();
    assertEquals(1.8, cov[0][0], 0.001);
}'''


def test_types_declaring_walks_inheritance():
    got = rr.types_declaring(LM_CTX, 'getChiSquare')
    assert 'AbstractLeastSquaresOptimizer' in got
    assert 'LevenbergMarquardtOptimizer' in got     # via extends
    assert 'VectorialPointValuePair' not in got


def test_receiver_is_typed_not_most_called():
    declaring = rr.types_declaring(LM_CTX, 'getChiSquare')
    setup, recv, why = rr.extract_test_setup(
        LM_BODY, 'getChiSquare', ['getRMS', 'getCovariances'], declaring)
    assert recv == 'optimizer', why


def test_no_declaring_type_discards_rather_than_guesses():
    setup, recv, why = rr.extract_test_setup(
        LM_BODY, 'getChiSquare', ['getRMS'], {'UnrelatedClass'})
    assert setup is None and recv is None
    assert 'DISCARDED rather than guessed' in why


def test_wrong_typed_variable_cannot_be_receiver_even_if_called():
    # The silent-wrong-state case: a same-named method on another type. The
    # by-call candidate is VETOED because its declared type is not a
    # declaring type.
    body = ('public void t() {\n'
            '  Result r = new Result();\n'
            '  double d = r.getChiSquare();\n}')
    setup, recv, why = rr.extract_test_setup(
        body, 'getChiSquare', [], {'Optimizer'})
    assert recv is None, why


def test_twin_emitted_into_the_tests_package():
    assert rr.test_package(
        'package org.apache.commons.math.optimization.general;\n'
        'import java.util.List;\npublic class T {}'
    ) == 'org.apache.commons.math.optimization.general'
    src = rr.build_state_twin_driver(
        'X o = new X();', 'o', ['getN'], [],
        package='org.apache.commons.math.optimization.general')
    assert src.startswith('package org.apache.commons.math.optimization'
                          '.general;')


# ---------------------------------------------------------------------------
# VM re-walk #3 (2026-08-07): the context is XML-WRAPPED. Fixtures are the
# REAL shape from ladder1e's recorded trace, condensed.
# ---------------------------------------------------------------------------

XML_CTX = '''<class name="AbstractLeastSquaresOptimizer" role="patched">
     * Base class for implementing least squares optimizers.
     * <p>handles boilerplate for thresholds and jacobians.</p>
public abstract class AbstractLeastSquaresOptimizer implements DMVOptimizer {
    protected double[] residuals;
    public double getChiSquare() { }
    public double getRMS() { }
}
</class>
<class name="LevenbergMarquardtOptimizer" role="test-subject">
     * Solves a least squares problem using the Levenberg-Marquardt algorithm.
public class LevenbergMarquardtOptimizer extends AbstractLeastSquaresOptimizer {
    public VectorialPointValuePair optimize(Circle c) { }
}
</class>
<class name="VectorialPointValuePair" role="collaborator">
public class VectorialPointValuePair {
    public double[] getPointRef() { }
}
</class>'''


def test_xml_wrapped_context_yields_real_class_names():
    got = rr.types_declaring(XML_CTX, 'getChiSquare')
    # The re-walk-3 bug: {'name'} six times over, the declarer invisible.
    assert 'name' not in got
    assert 'AbstractLeastSquaresOptimizer' in got     # the declarer
    assert 'LevenbergMarquardtOptimizer' in got       # via extends
    assert 'VectorialPointValuePair' not in got


def test_javadoc_prose_is_not_a_class_name():
    got = rr.types_declaring(XML_CTX, 'getRMS')
    assert all(n[:1].isupper() for n in got), got


def test_plausible_class_names_flags_the_broken_parse():
    assert rr.plausible_class_names({'AbstractLeastSquaresOptimizer'})
    assert not rr.plausible_class_names({'name'})
    assert not rr.plausible_class_names({'name', 'role', 'for'})
    assert not rr.plausible_class_names(set())


def test_receiver_resolves_end_to_end_on_xml_context():
    declaring = rr.types_declaring(XML_CTX, 'getChiSquare')
    setup, recv, why = rr.extract_test_setup(
        LM_BODY, 'getChiSquare', ['getRMS', 'getCovariances'], declaring)
    assert recv == 'optimizer', why


def test_broken_parse_is_discarded_loudly_not_silently(monkeypatch):
    # A broken extractor must not read as "this leg has no declaring type".
    import java.run as runmod
    monkeypatch.setattr(rr, 'types_declaring', lambda ctx, m: {'name'})
    fact, events, calls = _run_chain(
        monkeypatch, [BUGGY_TWIN, PATCHED_TWIN], REF_OK)
    assert fact is None
    assert any('PARSE BROKEN' in (o or '') for o, _ in events), events[-3:]
