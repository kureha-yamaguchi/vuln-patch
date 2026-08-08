"""The state-twin architecture (post-roll-4): parsing, matching, setup
extraction, input recovery, and the production-path walkthrough.

Roll 4's lesson pinned here: the walkthrough must drive the SAME code path
run.py drives. `test_production_chain_*` calls `_reference_impl_fact` itself
with a stubbed generator/JVM — a call-site lag now fails a test instead of a
roll.
"""
import os
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
    names, why = rr.match_parameters(rr.parse_parameters(ROLL4_SIGS[1]), CANON, [])
    assert names == ['residuals', 'residualsWeights', 'cost']


def test_match_bare_ambiguous_types_discards():
    # Two double[] fields exist; a bare double[] cannot pick one.
    names, why = rr.match_parameters(rr.parse_parameters(ROLL4_SIGS[0]), CANON, [])
    assert names is None
    assert 'unmappable' in why


def test_match_unmappable_roll4_sig4_discards():
    names, why = rr.match_parameters(rr.parse_parameters(ROLL4_SIGS[3]), CANON, [])
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


def _run_chain(monkeypatch, twin_outputs, ref_output, generated=REFERENCE_REPLY,
               ctx=None):
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
              'actual=9.99', class_ctx=[ctx if ctx is not None else CTX],
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


# ---------------------------------------------------------------------------
# VM re-walk #4 (2026-08-07): unique read variables, and the screening
# surface scoped to the receiver's own type with free passes excluded.
# ---------------------------------------------------------------------------

def test_read_variables_are_unique_across_observables():
    # `variable r is already defined` with 9 real siblings.
    src = rr.build_state_twin_driver(
        'X o = new X();', 'o',
        ['getA', 'getB', 'getC', 'getD'], ['f'])
    names = [l.split('=')[0].strip().split()[-1]
             for l in src.splitlines() if 'Object r' in l]
    assert len(names) == len(set(names)) == 4, names


def test_reference_driver_read_variables_are_unique():
    d = rr.build_driver('R', ['getA', 'getB', 'getC'], ['1'])
    names = [l.split('=')[0].strip().split()[-1]
             for l in d.splitlines() if 'Object r' in l]
    assert len(names) == len(set(names)) == 3, names


def test_siblings_scoped_to_the_receivers_type():
    # The context holds collaborators; their observables are NOT callable on
    # the receiver, so an unscoped scan guarantees a compile error.
    ctx = XML_CTX + '''
<class name="Other" role="collaborator">
public class Other {
    public double getSomethingElse() { }
}
</class>'''
    decl = rr.types_declaring(ctx, 'getChiSquare')
    sibs = rg.sibling_observables(ctx, 'getChiSquare', declaring_types=decl)
    assert 'getRMS' in sibs
    assert 'getSomethingElse' not in sibs
    assert 'getPointRef' not in sibs


def test_stored_settings_are_excluded_not_just_sorted_last():
    ctx = ('<class name="Opt" role="patched">\npublic class Opt {\n'
           '  public double getChiSquare() { }\n'
           '  public double getRMS() { }\n'
           '  public int getMaxIterations() { }\n'
           '  public int getMaxEvaluations() { }\n'
           '  public double getDefaultTolerance() { }\n}\n</class>')
    decl = rr.types_declaring(ctx, 'getChiSquare')
    sibs = rg.sibling_observables(ctx, 'getChiSquare', declaring_types=decl)
    assert sibs == ['getRMS'], sibs      # free passes gone


# ---------------------------------------------------------------------------
# VM re-walk #5 (2026-08-07): array observables must print by VALUE, and
# BOTH sides must format identically. Real symptom: getCovariances printed
# `[D@19469ea2` -- a per-invocation identity hash, so equal arrays always
# "disagreed", and 2 of Math-65's 6 siblings are arrays.
# ---------------------------------------------------------------------------

ALL_EMITTERS = ('twin', 'reference', 'buggy_twin')


def _emit(kind):
    if kind == 'twin':
        return rr.build_state_twin_driver('X o = new X();', 'o',
                                          ['getCovariances'], ['residuals'])
    if kind == 'reference':
        return rr.build_driver('R', ['getCovariances'], ['a'])
    return rr.build_buggy_twin_driver('C', 'new C()', ['getCovariances'],
                                      ['a'])


@pytest.mark.parametrize('kind', ALL_EMITTERS)
def test_every_emitter_carries_the_shared_formatter(kind):
    src = _emit(kind)
    assert 'static String vpFmt' in src, kind


@pytest.mark.parametrize('kind', ALL_EMITTERS)
def test_no_observable_is_printed_with_raw_valueOf(kind):
    src = _emit(kind)
    # OBSERVABLE prints only: the `__state`/`__construct` echoes are
    # bookkeeping literals, not values under comparison.
    reads = [l for l in src.splitlines()
             if 'System.out.println' in l and '=" +' in l
             and '__state' not in l and '__construct' not in l]
    assert reads, kind
    for line in reads:
        assert 'String.valueOf(' not in line, (kind, line)
        assert 'vpFmt(' in line, (kind, line)


@pytest.mark.parametrize('kind', ALL_EMITTERS)
def test_formatter_is_identical_across_emitters(kind):
    # Identical formatting on BOTH sides is the whole point: a difference
    # here manufactures disagreement out of representation.
    a = rr._FMT_HELPER
    assert a in _emit(kind)


def test_formatter_covers_every_array_arity_and_nesting():
    h = rr._FMT_HELPER
    for t in ('double[]', 'int[]', 'long[]', 'float[]', 'boolean[]',
              'byte[]', 'short[]', 'char[]'):
        assert f'instanceof {t}' in h, t
    # double[][] is an Object[] -> deepToString, which prints nested values.
    assert 'instanceof Object[]' in h and 'deepToString' in h
    # The Object[] branch must come AFTER the primitive-array branches,
    # or a double[] would match Object[]... (it does not, but order still
    # matters for nested primitive arrays).
    assert h.index('instanceof double[]') < h.index('instanceof Object[]')


def test_reflection_and_observable_paths_share_one_formatter():
    # printField and the observable reads must not format differently:
    # __param_* feeds the reference's INPUTS, the reads feed the COMPARISON.
    src = rr.build_state_twin_driver('X o = new X();', 'o', ['getCov'],
                                     ['residuals'])
    assert 'vpFmt(v)' in src            # printField path
    assert 'vpFmt(r0)' in src           # observable path
    assert src.count('static String vpFmt') == 1


# ---------------------------------------------------------------------------
# VM re-walk #6 (2026-08-07): the model spells observables without the
# accessor prefix. Real declared list: ['rms', 'chiSquare',
# 'guessParametersErrors'] against wanted getChiSquare/getRMS/... .
# ---------------------------------------------------------------------------

ROLL5_DECLARED = ['rms', 'chiSquare', 'guessParametersErrors']
ROLL5_WANTED = ['getChiSquare', 'getRMS', 'getCovariances',
                'guessParametersErrors']


def test_observable_key_matches_the_codebase_convention():
    assert rr.observable_key('getChiSquare') == rr.observable_key('chiSquare')
    assert rr.observable_key('isEmpty') == rr.observable_key('empty')
    # No prefix to drop: unchanged, which is why it matched in the roll.
    assert rr.observable_key('guessParametersErrors') == \
        'guessparameterserrors'
    # Too short after stripping -> not treated as a prefix.
    assert rr.observable_key('getX') == 'getx'


def test_the_roll5_discard_now_matches():
    m = rr.match_observable_names(ROLL5_DECLARED, ROLL5_WANTED)
    assert m['getChiSquare'] == 'chiSquare'      # the disputed observable
    assert m['getRMS'] == 'rms'
    assert m['guessParametersErrors'] == 'guessParametersErrors'
    assert 'getCovariances' not in m             # genuinely not implemented


def test_driver_calls_declared_name_but_keys_canonical():
    m = rr.match_observable_names(ROLL5_DECLARED, ROLL5_WANTED)
    d = rr.build_driver('ReferenceImpl', list(m.items()), ['a, b'])
    assert 'ReferenceImpl.compute_chiSquare(a, b)' in d   # what it wrote
    assert '"getChiSquare="' in d                         # what we compare
    assert 'compute_getChiSquare' not in d
    # The exception path must carry the canonical key too, or a throw would
    # land under a key nothing compares.
    assert '"getChiSquare=EX:"' in d


def test_unmatched_declaration_is_not_called():
    m = rr.match_observable_names(['somethingElse'], ['getChiSquare'])
    assert m == {}
    d = rr.build_driver('R', list(m.items()), ['a'])
    assert 'compute_' not in d


def test_plain_names_still_work_unchanged():
    d = rr.build_driver('R', ['getA', 'getB'], ['x'])
    assert 'R.compute_getA(x)' in d and '"getA="' in d


# ---------------------------------------------------------------------------
# Roll 6 (2026-08-07): the twin compiled, then died exit 1 / no end marker.
# HarnessBuilder compiles with `-d <fuzz_dir>` but BuildResult.classpath
# carries only project cp + jazzer api jar -- so `java -cp` could not find
# the class it had just built. A seam defect: the pipeline's own Jazzer
# runner has always appended the harness dir.
# ---------------------------------------------------------------------------

def _fake_drv(cp='/a.jar', out='/proj/fuzz/reference_twin'):
    return types.SimpleNamespace(
        classpath=cp, harness_path=f'{out}/StateTwinDriver.java',
        class_name='StateTwinDriver', compiled=True)


def test_runtime_classpath_includes_the_compiled_output_dir():
    cp = rr._runtime_classpath(_fake_drv(), '/proj')
    assert '/proj/fuzz/reference_twin' in cp.split(':')
    assert '/a.jar' in cp.split(':')


def test_runtime_classpath_falls_back_to_project_dir():
    drv = types.SimpleNamespace(classpath='/a.jar', harness_path='',
                                class_name='X')
    assert '/proj' in rr._runtime_classpath(drv, '/proj').split(':')


def test_jvm_failure_reason_carries_the_jvms_own_words():
    # The attribution gap roll 6 cost: "no end marker" alone is
    # indistinguishable between a missing class, a thrown exception and a
    # silent exit.
    why = rr._jvm_failure_reason(
        'twin run',
        'Error: Could not find or load main class StateTwinDriver\n', 1)
    assert 'Could not find or load main class' in why
    assert 'exit 1' in why


def test_jvm_failure_reason_says_so_when_nothing_was_printed():
    why = rr._jvm_failure_reason('twin run', '', 137)
    assert 'printed\nnothing' in why or 'printed nothing' in why
    assert '137' in why


def test_run_twin_reports_jvm_output_on_failure(monkeypatch):
    class B:
        def build(self, src, d, output_subdir=''):
            return _fake_drv()
    monkeypatch.setattr(
        rr, '_run_java',
        lambda cls, cp, cwd, t: ('Exception in thread "main" '
                                 'java.lang.NoClassDefFoundError: Circle', 1,
                                 None))
    vals, why = rr.run_twin(B(), '/proj', 'class StateTwinDriver {}')
    assert vals is None
    assert 'NoClassDefFoundError' in why


def test_run_twin_passes_the_output_dir_on_the_classpath(monkeypatch):
    seen = {}

    class B:
        def build(self, src, d, output_subdir=''):
            return _fake_drv()

    def spy(cls, cp, cwd, t):
        seen['cp'], seen['cwd'] = cp, cwd
        return 'x=1\n__construct0=OK\n' + rr.END_MARKER, 0, None
    monkeypatch.setattr(rr, '_run_java', spy)
    vals, why = rr.run_twin(B(), '/proj', 'class StateTwinDriver {}')
    assert vals is not None, why
    assert '/proj/fuzz/reference_twin' in seen['cp']
    assert seen['cwd'] == '/proj/fuzz/reference_twin'


# ---------------------------------------------------------------------------
# Roll 7 (2026-08-07): the model declared BARE types -- `double[], double[],
# double` -- so nominal matching had nothing to work with and two double[]
# fields made the unique-type fallback ambiguous. Real Math-65 field set.
# ---------------------------------------------------------------------------

M65_CANON = [('int', 'DEFAULT_MAX_ITERATIONS'), ('Checker', 'checker'),
             ('double[][]', 'jacobian'), ('int', 'cols'), ('int', 'rows'),
             ('double[]', 'targetValues'), ('double[]', 'residualsWeights'),
             ('double[]', 'point'), ('Objective', 'objective'),
             ('double[]', 'residuals'), ('double', 'cost')]

M65_CTX_BODY = ('public class A {\n'
                '  private double[] residuals;\n'
                '  private double[] residualsWeights;\n'
                '  private int rows;\n'
                '  private double[] targetValues;\n'
                '  private double cost;\n'
                '  public double getChiSquare() {\n'
                '    double chiSquare = 0;\n'
                '    for (int i = 0; i < rows; ++i) {\n'
                '      final double residual = residuals[i];\n'
                '      chiSquare += residual * residual / residualsWeights[i];\n'
                '    }\n    return chiSquare;\n  }\n}')


def test_fields_read_by_is_code_order_not_declaration_order():
    canon = rr.canonical_state(M65_CTX_BODY)
    reads = rr.fields_read_by(M65_CTX_BODY, 'getChiSquare', canon)
    assert [n for _t, n in reads] == ['rows', 'residuals', 'residualsWeights']
    # targetValues and cost are fields but this method does not read them.
    assert 'targetValues' not in [n for _t, n in reads]


def test_bare_signature_maps_by_read_order():
    canon = rr.canonical_state(M65_CTX_BODY)
    reads = rr.fields_read_by(M65_CTX_BODY, 'getChiSquare', canon)
    names, why = rr.match_parameters(
        rr.parse_parameters('double[], double[]'), canon, reads)
    assert names == ['residuals', 'residualsWeights'], why


def test_bare_signature_without_read_order_still_discards():
    # No body visible -> no ordering evidence -> ambiguous -> discard.
    names, why = rr.match_parameters(
        rr.parse_parameters('double[], double[]'), M65_CANON, [])
    assert names is None and 'unmappable' in why


def test_unsupplyable_parameter_discards_rather_than_guessing():
    # Roll 7's real signature: the third `double` corresponds to no field
    # the method reads. Guessing would feed silent wrong input.
    canon = rr.canonical_state(M65_CTX_BODY)
    reads = rr.fields_read_by(M65_CTX_BODY, 'getChiSquare', canon)
    names, why = rr.match_parameters(
        rr.parse_parameters('double[], double[], double'), canon, reads)
    assert names is None
    assert 'read by the method' in why and 'residuals' in why


def test_named_parameters_still_win_over_read_order():
    canon = rr.canonical_state(M65_CTX_BODY)
    reads = rr.fields_read_by(M65_CTX_BODY, 'getChiSquare', canon)
    names, why = rr.match_parameters(
        rr.parse_parameters('double[] residualsWeights, double[] residuals'),
        canon, reads)
    assert names == ['residualsWeights', 'residuals'], why   # order honoured


def test_substring_match_cannot_cross_to_an_unread_field():
    # `residuals` is a substring of `residualsWeights`: mapping one to the
    # other compiles, runs, and feeds the reference the WRONG array.
    canon = [('double[]', 'residualsWeights'), ('int', 'rows')]
    names, why = rr.match_parameters(
        rr.parse_parameters('double[] residuals'), canon, [('int', 'rows')])
    assert names is None, (names, why)


def test_error_names_read_fields_and_total_count():
    canon = rr.canonical_state(M65_CTX_BODY)
    names, why = rr.match_parameters(
        rr.parse_parameters('Widget gadget'), canon, [])
    assert names is None
    assert 'all fields (' in why          # count, not a truncated list alone


# ---------------------------------------------------------------------------
# Roll 8 (2026-08-07): fields_read_by was built, tested, and wired at ZERO
# call sites -- the same shape as roll 2's Spec K (one door of two). Unit
# tests call helpers directly with correct arguments, so they cannot see a
# missing or degraded call. These tests look at the SEAM.
# ---------------------------------------------------------------------------

import inspect


def _chain_source():
    from java import run as runmod
    return inspect.getsource(runmod._reference_impl_fact)


def test_match_parameters_requires_read_order_positionally():
    # The default that let production run with None for a whole roll is
    # gone: omitting it is now a TypeError, not a silent degradation.
    sig = inspect.signature(rr.match_parameters)
    p = sig.parameters['read_order']
    assert p.default is inspect.Parameter.empty


def test_production_passes_read_fields_to_the_mapper():
    src = _chain_source()
    assert 'fields_read_by(' in src, 'derivation never invoked in production'
    i = src.index('match_parameters(')
    call = src[i:i + 220]
    assert 'fields_read_by' in call, (
        'match_parameters is called without the read-order argument: '
        + call[:160])


def test_every_helper_the_chain_imports_is_actually_used():
    # The generalized Spec-K guard: a mechanism imported but never called
    # is a mechanism that does not exist. Roll 2 (one door of two) and
    # roll 8 (zero call sites) were both invisible to unit tests.
    src = _chain_source()
    i = src.index('from java.relations.reference_run import')
    block = src[i:src.index(')', i)]
    imported = [n.strip() for n in
                block.split('import', 1)[1].replace('(', '').split(',')]
    unused = [n for n in imported if n and f'{n}(' not in src]
    assert not unused, f'imported into the chain but never called: {unused}'


def test_chain_calls_the_reference_impl_helpers_it_imports():
    src = _chain_source()
    i = src.index('from java.relations.reference_impl import')
    block = src[i:src.index(')', i)]
    imported = [n.strip() for n in
                block.split('import', 1)[1].replace('(', '').split(',')]
    unused = [n for n in imported if n and f'{n}(' not in src]
    assert not unused, f'imported into the chain but never called: {unused}'


# ---------------------------------------------------------------------------
# Roll 8 pre-walk (2026-08-07): three seams found by replaying roll 8's
# RECORDED reference through the code roll 9 would run -- before spending the
# roll. (1) The comment line was bare types but the model's own declarations
# named both parameters, in the OPPOSITE order from the buggy body's read
# order: positional mapping would have fed the reference swapped arrays --
# same type, same length, runs cleanly, computes garbage, and the screen's
# discard would then read as "a doc-derived reference cannot reproduce the
# buggy build". (2) Rolls 6/7/8 all declared ONE countable sibling against a
# screen bar of three, because the prompt never named the siblings that
# count. (3) The bar was enforced only inside screen_reference, after the
# twin build and two JVM runs, though it is knowable at the match step.
# ---------------------------------------------------------------------------

ROLL8_REFERENCE = '''// compute(double[], double[]) : getRMS, getChiSquare
public class ReferenceImpl {

    public static double compute_getRMS(double[] residualsWeights, double[] residuals) {
        double chiSquare = compute_getChiSquare(residualsWeights, residuals);
        return Math.sqrt(chiSquare / residuals.length);
    }

    public static double compute_getChiSquare(double[] residualsWeights, double[] residuals) {
        double chiSquare = 0.0;
        for (int i = 0; i < residuals.length; ++i) {
            final double r = residuals[i];
            chiSquare += residualsWeights[i] * r * r;
        }
        return chiSquare;
    }
}'''


def test_roll8_reference_maps_by_declaration_names_not_read_order():
    # The verbatim roll-8 material end to end: merge, then map. The
    # declaration order (residualsWeights, residuals) must win over the
    # read order (residuals, residualsWeights) -- the swap IS the bug.
    sig = rr.declared_signature(ROLL8_REFERENCE)
    assert sig == 'double[], double[]'
    canon = rr.canonical_state(M65_CTX_BODY)
    merged = rr.merge_declared_parameter_names(ROLL8_REFERENCE, sig, canon)
    assert merged == 'double[] residualsWeights, double[] residuals'
    reads = rr.fields_read_by(M65_CTX_BODY, 'getChiSquare', canon)
    names, why = rr.match_parameters(
        rr.parse_parameters(merged), canon, reads)
    assert names == ['residualsWeights', 'residuals'], why
    assert names != ['residuals', 'residualsWeights']


def test_merge_declines_when_declarations_disagree():
    src = ('// compute(double[], double[]) : a, b\n'
           'class ReferenceImpl {\n'
           '  public static double compute_a(double[] x, double[] y) '
           '{ return 0; }\n'
           '  public static double compute_b(double[] y, double[] x) '
           '{ return 0; }\n}')
    assert rr.merge_declared_parameter_names(src, 'double[], double[]') == \
        'double[], double[]'


def test_merge_never_overwrites_comment_line_names():
    merged = rr.merge_declared_parameter_names(
        ROLL8_REFERENCE, 'double[] weights, double[] residuals')
    assert merged == 'double[] weights, double[] residuals'


def test_merge_declines_on_arity_or_type_mismatch():
    # Comment says two params, declarations carry two but a different type.
    src = ('// compute(double[], double) : a\n'
           'class ReferenceImpl {\n'
           '  public static double compute_a(double[] x, double[] y) '
           '{ return 0; }\n}')
    assert rr.merge_declared_parameter_names(src, 'double[], double') == \
        'double[], double'
    assert rr.merge_declared_parameter_names(src, 'double[]') == 'double[]'


def test_merge_declines_names_that_answer_to_no_field():
    # A model that names its parameters `r, w` has named nothing a field
    # answers to. Adopting those would turn a read-order-mappable
    # signature into a discard, so the merge declines and the positional
    # fallback stays in play.
    src = ('// compute(double[], double[]) : a\n'
           'class ReferenceImpl {\n'
           '  public static double compute_a(double[] r, double[] w) '
           '{ return 0; }\n}')
    canon = rr.canonical_state(M65_CTX_BODY)
    assert rr.merge_declared_parameter_names(
        src, 'double[], double[]', canon) == 'double[], double[]'
    # Without canonical evidence available the merge stays permissive —
    # a name is still better than a bare type when nothing contradicts it.
    assert 'double[] r, double[] w' == rr.merge_declared_parameter_names(
        src, 'double[], double[]')


def test_merge_strips_final_modifier_from_declarations():
    src = ('// compute(double[], int) : a\n'
           'class ReferenceImpl {\n'
           '  public static double compute_a(final double[] residuals, '
           'final int rows) { return 0; }\n}')
    assert rr.merge_declared_parameter_names(src, 'double[], int') == \
        'double[] residuals, int rows'


def test_prompt_names_the_siblings_and_the_bar():
    from java.relations.reference_impl import MIN_SCREENED_OBSERVABLES
    sibs = ['getCovariances', 'getRMS', 'guessParametersErrors']
    msgs = rg.build_reference_prompt(
        method='getChiSquare', skeleton='class A { /* body withheld */ }',
        docs=[], failing_test='', siblings=sibs)
    user = msgs[1]['content']
    for s in sibs:
        assert f'`{s}`' in user
    assert f'At least {MIN_SCREENED_OBSERVABLES} of them' in user
    # Counters are named as underivable-from-state, so the model skips
    # rather than fakes them.
    assert 'bookkeeping' in user


def test_prompt_without_siblings_omits_the_section():
    msgs = rg.build_reference_prompt(
        method='getChiSquare', skeleton='class A { /* body withheld */ }',
        docs=[], failing_test='')
    assert 'siblings that count' not in msgs[1]['content']


def test_too_thin_is_decided_at_the_match_step():
    from java.relations.reference_impl import too_thin_to_screen
    sibs = ['getRMS', 'getCovariances', 'getEvaluations', 'getIterations']
    # Roll 8's real shape: disputed + one sibling matched.
    thin, why = too_thin_to_screen(
        {'getChiSquare': 'getChiSquare', 'getRMS': 'getRMS'}, sibs)
    assert thin and 'JVM runs' in why
    thin, why = too_thin_to_screen(
        {'getChiSquare': 'x', 'getRMS': 'x', 'getCovariances': 'x',
         'getEvaluations': 'x'}, sibs)
    assert not thin, why


def test_chain_resolves_siblings_before_generating():
    # Seam: the prompt can only NAME the siblings if the surface is
    # resolved before the generation -- and a broken declaring-type parse
    # must cost zero model calls.
    src = _chain_source()
    assert src.index('sibling_observables(') < src.index(
        'build_reference_prompt(')
    call = src[src.index('build_reference_prompt('):]
    call = call[:call.index('except')]
    assert 'siblings=siblings' in call, (
        'the prompt is built without the sibling list: ' + call[:200])


def test_chain_merges_declaration_names_before_mapping():
    # Seam: merging after the mapper would be the roll-8 shape again --
    # a helper that exists but cannot affect the decision.
    src = _chain_source()
    assert src.index('merge_declared_parameter_names(') < src.index(
        'match_parameters(')


def test_chain_decides_the_thin_bar_before_the_twin_runs():
    src = _chain_source()
    assert src.index('too_thin_to_screen(') < src.index('run_twin(')


M65_SURFACE_CTX = '''<class name="AbstractLeastSquaresOptimizer" role="patched">
     * Get a Chi-Square-like value assuming the N residuals follow N
     * distinct normal distributions centered on 0 and whose variances are
     * the reciprocal of the weights. @return chi-square value
public abstract class AbstractLeastSquaresOptimizer {
    protected double[] residuals;
    protected double[] residualsWeights;
    protected double cost;
    protected int rows;
    protected int cols;
    public double getChiSquare() {
        double chiSquare = 0;
        for (int i = 0; i < rows; ++i) {
            final double residual = residuals[i];
            chiSquare += residual * residual / residualsWeights[i];
        }
        return chiSquare;
    }
    public double getRMS() { }
    public double[][] getCovariances() { }
    public double[] guessParametersErrors() { }
    public int getEvaluations() { }
    public int getIterations() { }
    public int getJacobianEvaluations() { }
}
</class>'''


def test_roll8_reference_through_the_chain_discards_thin_before_any_jvm(
        monkeypatch):
    # The whole roll-9-if-nothing-changed scenario, on the production
    # function: roll 8's verbatim reply against a real-shaped M65 surface.
    # The chain must (a) recover the declaration names — the swap is the
    # bug — and (b) discard at the thin bar BEFORE building or running any
    # twin, with the count in the reason.
    fact, events, calls = _run_chain(
        monkeypatch, [], None, generated=ROLL8_REFERENCE,
        ctx=M65_SURFACE_CTX)
    assert fact is None
    outs = [(o or '') for o, _ in events]
    assert any('parameter names recovered' in o for o in outs)
    assert any('too thin to screen' in o for o in outs)
    assert calls['twin_dirs'] == []          # decided before any JVM run
    thin_reason = next(r for o, r in events
                       if o and 'too thin' in o)
    assert '1 shared sibling' in thin_reason


# ---------------------------------------------------------------------------
# Roll 9 (2026-08-07): died at `driver did not compile`, reason carrying no
# javac output. Desk diagnosis, confirmed by replay: java_literal stripped
# EVERY `[]` from a multi-dimensional type and passed deepToString's inner
# text through -- `new double[]{[...], [...]}` -- non-None, so the chain
# proceeded and javac refused the driver. Roll 9's reference was the first
# to take a `double[][] jacobian` (the prompt fix worked; the literal
# builder had never been asked for 2-D). Second seam, same read: all three
# COMPILE-failure branches returned bare messages while BuildResult.stderr
# held javac's words -- the roll-6 attribution treatment covered only the
# RUN phase.
# ---------------------------------------------------------------------------

ROLL9_SIG = ('double[][] jacobian, double[] residuals, '
             'double[] residualsWeights, double cost, int rows, int cols')


def test_java_literal_two_dimensional_arrays():
    # Real-shaped: the twin's deepToString of getCovariances in re-walk #7.
    printed = ('[[0.0015747823386087533, 3.199542770565854E-7], '
               '[3.199542770565854E-7, 0.0016461547716898509]]')
    lit = rr.java_literal('double[][]', printed)
    assert lit == ('new double[][]{{0.0015747823386087533, '
                   '3.199542770565854E-7}, {3.199542770565854E-7, '
                   '0.0016461547716898509}}')
    assert rr.java_literal('double[][]', '[]') == 'new double[0][]'
    assert rr.java_literal('int[][]', '[[1, 2], [3]]') == \
        'new int[][]{{1, 2}, {3}}'
    # Element types whose printed form could itself contain a bracket
    # fail closed rather than guessing.
    assert rr.java_literal('String[][]', '[[a], [b]]') is None
    # 1-D behaviour unchanged.
    assert rr.java_literal('double[]', '[1.0, 2.5]') == \
        'new double[]{1.0, 2.5}'


def test_roll9_signature_builds_a_bracket_free_driver():
    # The verbatim roll-9 parameter list, end to end: every literal
    # reconstructs, and the driver text contains no `{[` -- the exact
    # character pair javac refused.
    params = rr.parse_parameters(ROLL9_SIG)
    printed = {
        'jacobian': '[[1.5, 2.5], [3.5, 4.5]]',
        'residuals': '[1.0, 2.0]',
        'residualsWeights': '[1.0, 1.0]',
        'cost': '1.25', 'rows': '5', 'cols': '2',
    }
    lits = [(rr.java_literal(t, printed[n]), t) for t, n in params]
    assert all(l for l, _ in lits), lits
    driver = rr.build_reference_call_driver(
        'ReferenceImpl',
        [('getChiSquare', 'getChiSquare'), ('getRMS', 'getRMS')],
        lits)
    assert '{[' not in driver
    assert 'new double[][]{{1.5, 2.5}, {3.5, 4.5}}' in driver


def test_driver_hoists_each_array_literal_exactly_once():
    # Re-walk #8 (roll 10's recorded artifacts, replayed): ~630 points ->
    # ~15KB per array literal, inlined once per observable call -> main()
    # blew the JVM's 64KB bytecode-per-method cap (`code too large`). One
    # static field + one initializer per array bounds every method
    # regardless of how many observables share the state.
    big = '[' + ', '.join(f'{i}.5' for i in range(630)) + ']'
    lits = [(rr.java_literal('double[]', big), 'double[]'),
            ('5', 'int')]
    driver = rr.build_reference_call_driver(
        'ReferenceImpl',
        [('getChiSquare', 'getChiSquare'), ('getRMS', 'getRMS'),
         ('getCovariances', 'getCovariances'),
         ('guessParametersErrors', 'guessParametersErrors')],
        lits)
    # The big literal appears ONCE (in its initializer), not per call.
    assert driver.count('629.5') == 1
    # Call sites reference the field; scalars stay inline.
    assert driver.count('.compute_getChiSquare(A0, 5)') == 1
    body = driver[driver.index('void main'):]
    assert '629.5' not in body


def test_driver_refuses_a_single_oversized_literal():
    monster = '[' + ', '.join('1.0' for _ in range(20000)) + ']'
    with pytest.raises(ValueError, match='64KB'):
        rr.build_reference_call_driver(
            'ReferenceImpl', [('a', 'a')],
            [(rr.java_literal('double[]', monster), 'double[]')])


class _FailedBuild:
    compiled = False
    stderr = ('ReferenceDriver.java:12: error: illegal start of expression\n'
              '      Object r1 = ReferenceImpl.compute_getChiSquare('
              'new double[]{[1.5, 2.5], ...\n'
              '1 error\n')


class _FailingBuilder:
    def build(self, source, project_dir, output_subdir=''):
        return _FailedBuild()


def test_compile_discards_carry_javacs_words():
    obs, why = rr.run_reference(_FailingBuilder(), '/b', 'class R {}', 'drv')
    assert obs is None
    assert 'javac:' in why and 'illegal start of expression' in why
    vals, why = rr.run_twin(_FailingBuilder(), '/b', 'class T {}')
    assert vals is None
    assert 'javac:' in why and 'illegal start of expression' in why


def test_compile_discard_says_when_javac_printed_nothing():
    class _Silent:
        compiled = False
        stderr = ''
    class _B:
        def build(self, *a, **k): return _Silent()
    obs, why = rr.run_reference(_B(), '/b', 'class R {}', 'drv')
    assert obs is None and 'printed nothing' in why


def test_all_compile_branches_use_the_attribution_helper():
    # Seam: a fourth compile branch added without the helper would
    # reopen the roll-9 gap.
    for fn in (rr.run_reference, rr.run_twin):
        src = inspect.getsource(fn)
        assert "not compile" not in src.replace(
            '_compile_failure_reason', ''), (
            f'{fn.__name__} has a compile branch bypassing attribution')
        assert '_compile_failure_reason(' in src


# ---------------------------------------------------------------------------
# Roll 10 (2026-08-07): `cannot find symbol` at the driver's first call to
# ReferenceImpl. Decided by reading build(), not by re-rolling: javac's -d
# output dir was never on the COMPILE classpath, so the driver could not see
# the class the previous build() call had just produced -- roll 6's seam,
# one phase over (runtime was fixed, compile was not). And the compile
# reason's raw character-count head spent itself on the source-line echo
# (the hundreds-of-characters literal argument) and cut off `symbol:` and
# `location:` -- the two lines that decide between the two candidate causes.
# ---------------------------------------------------------------------------

ROLL10_STDERR = (
    'ReferenceDriver.java:18: error: cannot find symbol\n'
    '      Object r1 = ReferenceImpl.compute_getChiSquare(new double[]{'
    '0.9627965464677999, -1.128551577257383, 0.4038559788434105, '
    '1.7357155278727387, -0.9737928643591186, 0.1101013265334561, '
    '0.6511469699382675, -0.4113178459772596, 0.0997307251152219}, '
    'new double[]{1.0, 1.0, 1.0, 1.0, 1.0});\n'
    '                  ^\n'
    '  symbol:   class ReferenceImpl\n'
    '  location: class ReferenceDriver\n'
    '1 error\n')


def test_compile_reason_keeps_symbol_and_location_lines():
    class _B:
        compiled = False
        stderr = ROLL10_STDERR
    why = rr._compile_failure_reason('driver', _B())
    assert 'cannot find symbol' in why
    assert 'symbol:   class ReferenceImpl' in why
    assert 'location: class ReferenceDriver' in why
    # The source-line echo (the long literal) must NOT eat the budget.
    assert '0.9627965464677999' not in why


def test_driver_compiles_with_the_references_class_dir_on_the_classpath(
        monkeypatch):
    seen = []

    class _Ref:
        compiled = True
        harness_path = '/b/fuzz/reference/ReferenceImpl.java'

    class _Drv:
        compiled = True
        harness_path = '/b/fuzz/reference/ReferenceDriver.java'
        class_name = 'ReferenceDriver'
        classpath = 'TESTCP'

    class _Builder:
        def build(self, source, project_dir, output_subdir='',
                  extra_classpath=()):
            seen.append(list(extra_classpath))
            return _Ref() if len(seen) == 1 else _Drv()

    monkeypatch.setattr(rr, '_run_java',
                        lambda *a, **k: (f'getRMS=1.0\n{rr.END_MARKER}\n',
                                        0, None))
    obs, why = rr.run_reference(_Builder(), '/b', 'class R {}', 'class D {}')
    assert obs == {'getRMS': ['1.0']}, why
    # First build (the reference) gets no extras; the second (the driver)
    # must see the dir the reference's .class landed in.
    assert seen[0] == []
    assert seen[1] == ['/b/fuzz/reference']


def test_build_prepends_extra_classpath_to_javac(monkeypatch, tmp_path):
    from java.harness import build as bmod
    cmds = []

    def fake_run(cmd, **kw):
        cmds.append(cmd)
        return types.SimpleNamespace(returncode=0, stdout='', stderr='')
    monkeypatch.setattr(bmod.subprocess, 'run', fake_run)
    builder = bmod.HarnessBuilder(jazzer_api_jar='jz.jar')
    builder._test_classpath = lambda d: 'TESTCP'
    builder.build('public class D {}', str(tmp_path),
                  output_subdir='reference', extra_classpath=['/refdir'])
    cp = cmds[0][cmds[0].index('-cp') + 1]
    assert cp.split(os.pathsep) == ['/refdir', 'TESTCP', 'jz.jar']


# ---------------------------------------------------------------------------
# Re-walk #8, screen findings (2026-08-07): the buggy build's own covariance
# matrix is one ulp ASYMMETRIC in its printed form, and exact string
# comparison read that as a semantic disagreement -- the permanent-false-
# disagreement class again. Arrays now agree per element within the
# rounding floor. The REAL divergences (chiSquare 4x, the errors vector)
# must keep disagreeing.
# ---------------------------------------------------------------------------

REWALK8_COV_REF = ('[[0.0015747823386087533, 3.1995427705658546E-7], '
                   '[3.199542770565854E-7, 0.0015798545955493326]]')
REWALK8_COV_BUGGY = ('[[0.0015747823386087533, 3.199542770565854E-7], '
                     '[3.1995427705658546E-7, 0.0015798545955493326]]')


def test_array_agreement_is_per_element_not_string():
    from java.relations.reference_impl import _values_agree
    assert _values_agree(REWALK8_COV_REF, REWALK8_COV_BUGGY)
    # Real divergence stays a divergence.
    assert not _values_agree('[0.003947421421789695, 0.003953773486615504]',
                             '[0.0019737107108948474, 0.001976886743307752]')
    # Shape is structure, not noise.
    assert not _values_agree('[[1.0], [2.0]]', '[1.0, 2.0]')
    assert not _values_agree('[1.0, 2.0]', '[1.0]')
    # Non-numeric elements: exact only.
    assert not _values_agree('[a, b]', '[a, c]')


def test_screen_admits_on_ulp_noise_discards_on_real_divergence():
    from java.relations.reference_impl import screen_reference
    ref = {'getRMS': ['0.09931552348327041'],
           'getCovariances': [REWALK8_COV_REF],
           'guessParametersErrors': ['[0.0039, 0.0039]']}
    buggy = {'getRMS': ['0.09931552348327041'],
             'getCovariances': [REWALK8_COV_BUGGY],
             'guessParametersErrors': ['[0.0039, 0.0039]']}
    ok, why = screen_reference(ref, buggy, off_defect_keys=set(ref))
    assert ok, why
    buggy['guessParametersErrors'] = ['[0.0019737, 0.0019768]']
    ok, why = screen_reference(ref, buggy, off_defect_keys=set(ref))
    assert not ok and 'guessParametersErrors' in why


# ---------------------------------------------------------------------------
# OPTION B (user decision 2026-08-07): re-grade defect-reached siblings
# against the failing test's own asserted literal. All fixtures verbatim
# from the Math-65 record: the failure message, the assertion line, the
# twin's buggy print, the reference's computed values.
# ---------------------------------------------------------------------------

M65_FAILURE_MSG = ('--- org.apache.commons.math.optimization.general.'
                   'LevenbergMarquardtOptimizerTest::testCircleFitting\n'
                   'junit.framework.AssertionFailedError: '
                   'expected:<0.004> but was:<0.0019737107108948474>')
M65_ASSERT_SRC = ('double[] errors = optimizer.guessParametersErrors();\n'
                  '        assertEquals(0.004, errors[0], 0.001);\n'
                  '        assertEquals(0.004, errors[1], 0.001);\n')
M65_BUGGY_ERRORS = '[0.0019737107108948474, 0.001976886743307752]'
M65_REF_ERRORS = '[0.003947421421789695, 0.003953773486615504]'
M65_SIBS = ['getCovariances', 'getEvaluations', 'getIterations',
            'getJacobianEvaluations', 'getRMS', 'guessParametersErrors']


def test_corroboration_pin_attributed_by_verbatim_state_match():
    from java.relations.reference_impl import test_corroboration_pins
    buggy = {'guessParametersErrors': [M65_BUGGY_ERRORS],
             'getRMS': ['0.09931552348327041']}
    pins = test_corroboration_pins([M65_FAILURE_MSG], [M65_ASSERT_SRC],
                                   buggy, M65_SIBS)
    assert pins == {'guessParametersErrors': ('0.004', '0.001')}
    # No verbatim match of the observed value -> no pin, no guessing.
    buggy['guessParametersErrors'] = ['[0.9, 0.9]']
    assert test_corroboration_pins([M65_FAILURE_MSG], [M65_ASSERT_SRC],
                                   buggy, M65_SIBS) == {}
    # Assertion literal not found -> pin survives with tolerance None.
    buggy['guessParametersErrors'] = [M65_BUGGY_ERRORS]
    pins = test_corroboration_pins([M65_FAILURE_MSG], ['no asserts here'],
                                   buggy, M65_SIBS)
    assert pins == {'guessParametersErrors': ('0.004', None)}


def test_screen_option_b_regrades_the_defect_reached_sibling():
    from java.relations.reference_impl import screen_reference
    ref = {'getRMS': ['0.09931552348327041'],
           'getCovariances': [REWALK8_COV_REF],
           'guessParametersErrors': [M65_REF_ERRORS]}
    buggy = {'getRMS': ['0.09931552348327041'],
             'getCovariances': [REWALK8_COV_BUGGY],
             'guessParametersErrors': [M65_BUGGY_ERRORS]}
    pins = {'guessParametersErrors': ('0.004', '0.001')}
    # Without the pin: discarded (the pre-decision behaviour).
    ok, why = screen_reference(ref, buggy, off_defect_keys=set(ref))
    assert not ok and 'guessParametersErrors' in why
    # With the pin: admitted, and the reason says what happened.
    ok, why = screen_reference(ref, buggy, off_defect_keys=set(ref),
                               test_corroboration=pins)
    assert ok, why
    assert 're-graded' in why and 'guessParametersErrors' in why


def test_screen_option_b_requires_buggy_to_fail_the_pin():
    # Containment of the open-book concern: if the buggy build ALSO matches
    # the test's value, the answer key is not provably wrong there, and the
    # disagreement stands.
    from java.relations.reference_impl import screen_reference
    ref = {'a': ['0.0039'], 'b': ['1.0'], 'c': ['2.0']}
    buggy = {'a': ['0.0041'], 'b': ['1.0'], 'c': ['2.0']}
    pins = {'a': ('0.004', '0.001')}
    ok, why = screen_reference(ref, buggy, off_defect_keys=set(ref),
                               test_corroboration=pins)
    assert not ok and '`a`' in why


def test_screen_option_b_requires_reference_to_match_the_pin():
    from java.relations.reference_impl import screen_reference
    ref = {'a': ['0.9'], 'b': ['1.0'], 'c': ['2.0']}
    buggy = {'a': ['0.0019'], 'b': ['1.0'], 'c': ['2.0']}
    pins = {'a': ('0.004', '0.001')}
    ok, why = screen_reference(ref, buggy, off_defect_keys=set(ref),
                               test_corroboration=pins)
    assert not ok


def test_chain_resolves_corroboration_before_the_screen():
    src = _chain_source()
    assert src.index('test_corroboration_pins(') < src.index(
        'screen_reference(')
    call = src[src.index('screen_reference('):][:220]
    assert 'test_corroboration=corro' in call


# ---------------------------------------------------------------------------
# Roll 11 (defect 19, the first in the mechanism's judgement): the pin check
# discarded a correctly-diverging reference against a literal from a
# NEIGHBOURING assertion. 1.768262623567235 is asserted against
# `Math.sqrt(circle.getN()) * rms` — an RMS line, not a getChiSquare value —
# and reached the disputed observable only because the chain mapped every
# trusted literal onto it by construction. Validator 3 now attributes pins
# exactly as corroboration does.
# ---------------------------------------------------------------------------

M65_RMS_ASSERT_SRC = (
    '        double rms = optimizer.getRMS();\n'
    '        assertEquals(1.768262623567235,  '
    'Math.sqrt(circle.getN()) * rms,  1.0e-10);\n')


def test_no_pin_borrowed_from_a_neighbouring_assertion():
    from java.relations.reference_impl import pins_for_disputed, pin_check
    buggy = {'getChiSquare': ['1.5633763529538318'],
             'guessParametersErrors': [M65_BUGGY_ERRORS]}
    pins = pins_for_disputed('getChiSquare', [M65_FAILURE_MSG],
                             [M65_RMS_ASSERT_SRC + M65_ASSERT_SRC], buggy)
    # The failure message attributes to errors (not chiSquare), and no
    # assertion calls getChiSquare directly -> NO pin, whatever literals
    # the test contains.
    assert pins == {}
    # And the roll-11 reference (correctly diverging on-defect) ABSTAINS
    # instead of being discarded against the RMS literal.
    ok, why = pin_check({'getChiSquare': ['6.253505411815327']}, pins,
                        ['getChiSquare'])
    assert ok and 'ABSTAIN' in why


def test_pin_attaches_by_direct_assertion_and_keeps_the_bug_copy_catch():
    from java.relations.reference_impl import pins_for_disputed, pin_check
    src = ('  Opt optimizer = new Opt();\n'
           '  Assert.assertEquals(3.25, optimizer.getChiSquare(), 1e-9);\n')
    pins = pins_for_disputed('getChiSquare', [], [src],
                             {'getChiSquare': ['9.99']})
    assert pins == {'getChiSquare': [('3.25', '1e-9')]}
    # A bug-copying reference (echoes buggy 9.99) is still discarded.
    ok, why = pin_check({'getChiSquare': ['9.99']}, pins, ['getChiSquare'])
    assert not ok and 'copied the defect' in why
    # A faithful reference passes within the test's own tolerance.
    ok, why = pin_check({'getChiSquare': ['3.2500000004']}, pins,
                        ['getChiSquare'])
    assert ok, why


def test_pin_attaches_by_state_identity_when_failure_is_on_disputed():
    from java.relations.reference_impl import pins_for_disputed
    # If the failing assertion's observed value appears verbatim in the
    # twin's buggy print for the DISPUTED observable, the expected value
    # pins it — with the test's own tolerance recovered.
    msg = 'junit...AssertionFailedError: expected:<0.004> but was:<0.00197>'
    buggy = {'getX': ['0.00197']}
    pins = pins_for_disputed('getX', [msg],
                             ['assertEquals(0.004, thing.getX(), 0.001);'],
                             buggy)
    assert ('0.004', '0.001') in pins['getX']


def test_chain_attributes_pins_instead_of_blanket_trusted_values():
    src = _chain_source()
    assert 'pins_for_disputed(' in src
    i = src.index('pin_check(')
    assert 'trusted_values' not in src[i:i + 120], (
        'pin_check is still fed blanket trusted_values')


# ---------------------------------------------------------------------------
# 8.25 phase 1: THE REFERENCE VERDICT GATE (user-approved 2026-08-07).
# Deterministic, judge-independent, runs only on KEPT convictions. All
# fixtures verbatim from roll 12's record.
# ---------------------------------------------------------------------------

ROLL12_FIRING = ('[oracle:constant-weight-rms-chi] metamorphic violation: '
                 'n*RMS^2 must equal weight^2*chiSquare for uniform weights '
                 'weight=2.0 n=634 lhs=25.01402164726131 '
                 'rhs=6.253505411815327 chiSquare=6.253505411815327 '
                 'rms=0.09931552348327041')
ADMITTED_M65 = {
    'method': 'getChiSquare',
    'obs': {'getChiSquare': ['6.253505411815327'],
            'getRMS': ['0.09931552348327041'],
            'getCovariances': [REWALK8_COV_REF],
            'guessParametersErrors': [M65_REF_ERRORS]},
    'buggy': {'getChiSquare': ['1.5633763529538318'],
              'getRMS': ['0.09931552348327041'],
              'getCovariances': [REWALK8_COV_BUGGY],
              'guessParametersErrors': [M65_BUGGY_ERRORS]},
}


def test_gate_voids_the_roll12_conviction():
    from java.relations.reference_impl import reference_verdict_gate
    v, why = reference_verdict_gate(ROLL12_FIRING, ADMITTED_M65)
    assert v == 'void', why
    # The reason names the discriminating observable — the one where
    # reference and buggy DIFFER, so legacy coincidence cannot explain it.
    assert 'chiSquare' in why and 'VOID' in why


def test_gate_never_claims_corroboration_across_states():
    # Dry-run finding, fixed BEFORE first live run: a firing at a different
    # input legitimately produces different values, so disagreement with
    # the reference's test-state values cannot distinguish "patch wrong"
    # from "state different". The conviction stands, but the gate says
    # why it cannot say more.
    from java.relations.reference_impl import reference_verdict_gate
    fired = ROLL12_FIRING.replace('chiSquare=6.253505411815327',
                                  'chiSquare=42.0')
    v, why = reference_verdict_gate(fired, ADMITTED_M65)
    assert v == 'abstain'
    assert 'same-state cannot be established' in why
    # And the real roll-12 kept firing at a fuzz input abstains too.
    fuzz_fired = ('[oracle:chiSquare_matches_weighted_residual_sum] relation '
                  'violated: chi=0.055934191393670646 expected=0.0279')
    v, why = reference_verdict_gate(fuzz_fired, ADMITTED_M65)
    assert v == 'abstain'


def test_gate_abstains_on_shared_legacy_behaviour():
    # The design note's trap one level down: rms is identical on buggy,
    # patched and reference — matching it says nothing about the dispute.
    from java.relations.reference_impl import reference_verdict_gate
    fired = '[oracle:x] violation: rms=0.09931552348327041 threshold=0.5'
    v, why = reference_verdict_gate(fired, ADMITTED_M65)
    assert v == 'abstain'
    assert 'vouches for nothing' in why


def test_gate_abstains_without_an_admitted_reference():
    from java.relations.reference_impl import reference_verdict_gate
    assert reference_verdict_gate(ROLL12_FIRING, None)[0] == 'abstain'
    assert reference_verdict_gate(ROLL12_FIRING, {})[0] == 'abstain'
    assert reference_verdict_gate('', ADMITTED_M65)[0] == 'abstain'
    fired = '[oracle:x] violation: lhs=1.0 rhs=2.0 count=3'
    v, why = reference_verdict_gate(fired, ADMITTED_M65)
    assert v == 'abstain' and 'no observable' in why


def test_gate_voids_nothing_on_the_genuine_catch_population():
    # Pre-registered gate (a). The 67 rows carry no admitted reference —
    # the gate abstains on every one BY REACH, disclosed as such: this is
    # an abstention result, not a values-level exoneration. The live
    # guard remains the ladder's clean-leg hard stop.
    import json
    from pathlib import Path
    from java.relations.reference_impl import reference_verdict_gate
    pop = Path(__file__).resolve().parents[1] / 'docs' / 'replay' / \
        'backtrack' / 'guard_population.json'
    if not pop.exists():
        pytest.skip('genuine-catch guard not present')
    rows = json.load(pop.open())
    assert len(rows) == 67
    verdicts = [reference_verdict_gate(r.get('claim', ''), None)[0]
                for r in rows]
    assert verdicts.count('void') == 0
    assert set(verdicts) == {'abstain'}


def test_gate_runs_only_on_kept_convictions_and_before_the_keep():
    # Structural guards: (i) the gate is called inside the kept branch
    # (it can void or corroborate a keep, never manufacture one — the
    # 38-row population is untouched by construction); (ii) a void skips
    # the append.
    src = open(Path(__file__).resolve().parents[1] / 'src' / 'java'
               / 'run.py').read()
    i = src.index('reference_verdict_gate(\n')
    keep = src.index('_kept_replays.append', i)
    void = src.index("if _gv == 'void':", i)
    assert void < keep, 'the void decision must precede the keep'


from pathlib import Path  # noqa: E402  (used by the structural guards)


# ---------------------------------------------------------------------------
# Door parity + parse reach (post-roll-13 corpus measurement). The verbatim
# ladder1g firing: the harness echoes the method CALL (`getChiSquare()=`)
# and recomputes the buggy formula inline (`sum(...)=1.56...`), both of
# which defeated the k=v parser -- the reference's exact value sat behind
# them, unread.
# ---------------------------------------------------------------------------

LADDER1G_FIRING = ('[oracle:unnamed-check] [oracle:seed-dense-chi-formula] '
                   'metamorphic violation: getChiSquare()=6.253505411815327 '
                   'sum((target[i]-value[i])^2 / weights[i])='
                   '1.5633763529538318 relTol=1.0e-10')


def test_gate_reads_method_call_echo_keys_and_formula_echo_values():
    from java.relations.reference_impl import reference_verdict_gate
    v, why = reference_verdict_gate(LADDER1G_FIRING, ADMITTED_M65)
    assert v == 'void', why
    assert 'getChiSquare' in why


def test_gate_is_wired_on_both_doors():
    # Spec-K parity, measured before wired: the corpus scan found the
    # gate's only reach on the HARNESS door. Both adjudication sites must
    # gate their keeps; a void must precede the keep on each.
    src = open(Path(__file__).resolve().parents[1] / 'src' / 'java'
               / 'run.py').read()
    sites = [m for m in range(len(src))
             if src.startswith('reference_verdict_gate(\n', m)
             or src.startswith('reference_verdict_gate(', m)
             and src[m - 1] != '.']
    calls = src.count('_gv, _gwhy = reference_verdict_gate(')
    assert calls == 2, f'expected the gate on both doors, found {calls}'
    # Harness door: the void path records a drop reason, never a keep.
    i = src.index('fd_prior=_fd_consult_result')
    harness = src[i:i + 1800]
    assert "reference-verdict-gate: " in harness
    assert harness.index("if _gv == 'void':") < harness.index('kept_reason =')


# ---------------------------------------------------------------------------
# Stage 2 (Math-2, the first held-out bug): reference-chain reach was ZERO.
# The kept relation's check CALLS dist.getNumericalMean() and recomputes the
# documented mean n*m/N — the exact dispute the leg exists for, body shown —
# but its fired MESSAGE prints only actual=/expected= and the detector read
# only the message. Detection now also scans the check SOURCE (the same
# artifact the judge receives) for called methods, exact call syntax only.
# ---------------------------------------------------------------------------

MATH2_CHECK_SRC = '''int N = data.consumeInt(1, 1000000);
int m = data.consumeInt(0, N);
int n = data.consumeInt(0, N);
org.apache.commons.math3.distribution.HypergeometricDistribution dist;
double actual;
try {
  dist = new org.apache.commons.math3.distribution.HypergeometricDistribution(N, m, n);
  actual = dist.getNumericalMean();
} catch (Exception e) { return; }
double expected = ((double) n) * ((double) m) / ((double) N);
if (!(Math.abs(actual - expected) <= tol)) {
  throw new RuntimeException("relation hypergeometricMeanFormula violated: actual=" + actual + " expected=" + expected);
}'''

MATH2_FIRED = ('relation hypergeometricMeanFormula violated: '
               'actual=-49.5 expected=27.5')

MATH2_CTX = ('public class HypergeometricDistribution {\n'
             '  private int numberOfSuccesses;\n'
             '  public double getNumericalMean() {\n'
             '    return (double) (getSampleSize() * getNumberOfSuccesses()) '
             '/ (double) getPopulationSize();\n  }\n'
             '  public int sample() { … }\n'
             '}')


def test_detection_reads_the_check_source_not_only_the_message():
    from java.relations.reference_impl import disputed_observables
    # The fired message alone names nothing — the stage-2 zero-reach shape.
    assert disputed_observables(MATH2_FIRED, MATH2_CTX) == []
    # With the check source, the called method with a shown body is found.
    got = disputed_observables(MATH2_FIRED, MATH2_CTX,
                               check_source=MATH2_CHECK_SRC)
    assert 'getNumericalMean' in got


def test_option_a_detects_declared_methods_and_declines_absent_ones():
    from java.relations.reference_impl import disputed_observables
    # OPTION A (user decision 2026-08-08): an elided declaration is enough
    # — Math-2's `sample` firings now trigger (the chain's own validators
    # decide admission), and the disputed != patched shape is reachable.
    fired = '[oracle:seed-sample-lower] semantic mismatch: sample=-50'
    assert 'sample' in disputed_observables(fired, MATH2_CTX)
    # A name the context does not declare still declines.
    assert disputed_observables(
        '[oracle:x] mismatch: fooBarBaz=3', MATH2_CTX,
        check_source='int q = obj.fooBarBaz();') == []


def test_chain_passes_the_check_source_at_both_doors():
    src = open(Path(__file__).resolve().parents[1] / 'src' / 'java'
               / 'run.py').read()
    assert src.count('check_source=src)') == 1     # harness door
    assert src.count('check_source=_src)') == 1    # replay door
    i = src.index('def _reference_impl_fact')
    body = src[i:i + 3000]
    assert 'check_source=check_source' in body


# ---------------------------------------------------------------------------
# Stage-2 roll 2: the patched method's full body was in the context and
# _method_body returned None for TWO independent reasons — the throws
# clause between the parameter list and the brace (976 throws-clause
# definitions in the untruncated fixture corpus, 913 invisible: 93.5%),
# and the 900-char QUOTING cap applied to an EXISTENCE check (this body is
# 1,518 chars). Two locks on one door; fixing either alone changed nothing.
# ---------------------------------------------------------------------------

THROWS_BODY = ('    public int inverseCumulativeProbability(final double p) '
               'throws OutOfRangeException {\n'
               + '        cumulative += probability(x);   // padding line\n'
               * 28
               + '        return x;\n    }')
THROWS_CTX = ('public class HypergeometricDistribution {\n'
              '  private int numberOfSuccesses;\n'
              + THROWS_BODY + '\n'
              '  public double getNumericalMean() { … }\n'
              '}')


def test_throws_clause_methods_are_visible_to_every_matcher():
    from java.relations.evidence_facts import _method_body, _defined_methods
    assert len(THROWS_BODY) > 900          # both locks genuinely engaged
    # Quoting path: still capped (this body must NOT ride into a prompt).
    assert _method_body(THROWS_CTX, 'inverseCumulativeProbability') is None
    # Existence path: visible.
    body = _method_body(THROWS_CTX, 'inverseCumulativeProbability',
                        max_chars=None)
    assert body is not None and 'cumulative += probability' in body
    assert 'inverseCumulativeProbability' in _defined_methods(THROWS_CTX)


def test_detector_sees_the_throws_declaring_patched_method():
    from java.relations.reference_impl import disputed_observables
    fired = ('[oracle:x] semantic mismatch: inverseCumulativeProbability '
             'returned -50')
    got = disputed_observables(fired, THROWS_CTX)
    assert 'inverseCumulativeProbability' in got
    # OPTION A: the elided getNumericalMean is DECLARED, so it detects too
    # — this is Math-2's disputed != patched shape, the decision's point.
    got = disputed_observables(
        '[oracle:x] mismatch: getNumericalMean wrong', THROWS_CTX)
    assert 'getNumericalMean' in got


def test_fields_read_by_reads_through_a_throws_clause():
    canon = rr.canonical_state(THROWS_CTX)
    reads = rr.fields_read_by(THROWS_CTX, 'inverseCumulativeProbability',
                              canon)
    # The body references no canonical field by name here; the point is
    # that the HEADER matched (empty is fine, None/crash is not).
    assert isinstance(reads, list)
    src_ctx = THROWS_CTX.replace('probability(x)', 'numberOfSuccesses')
    reads = rr.fields_read_by(src_ctx, 'inverseCumulativeProbability',
                              rr.canonical_state(src_ctx))
    assert [n for _t, n in reads] == ['numberOfSuccesses']


# ---------------------------------------------------------------------------
# Stage 4 (Math-30): a CALL is not a declaration. The `[{;]` tail accepted
# `return 2 * standardNormal.cumulativeProbability(z);` as a declaration
# because `);` ends a call statement too -- one wasted generation on a
# method the context never declares (20.9% of fixture matches were calls).
# ---------------------------------------------------------------------------

def test_a_call_is_not_a_declaration():
    from java.relations.reference_impl import _method_declared
    ctx = ('public class MannWhitneyUTest {\n'
           '  public double mannWhitneyU(double[] x, double[] y) {\n'
           '    return 2 * standardNormal.cumulativeProbability(z);\n'
           '  }\n}')
    # The verbatim Math-30 false trigger: a receiver-dotted call.
    assert not _method_declared(ctx, 'cumulativeProbability')
    # An unqualified call is not a declaration either.
    assert not _method_declared('  return cumulativeProbability(z);\n',
                                'cumulativeProbability')
    # Real declarations of every stripe still pass.
    assert _method_declared(
        'public double cumulativeProbability(double x) {', 'cumulativeProbability')
    assert _method_declared(
        '  double cumulativeProbability(double x);', 'cumulativeProbability')
    assert _method_declared(
        'double[] cumulativeProbability(int n) { … }', 'cumulativeProbability')
    assert _method_declared(
        'public int inverseCumulativeProbability(final double p) '
        'throws OutOfRangeException {', 'inverseCumulativeProbability')
    # And detection composes: the call in the check source triggers the
    # candidate, the context's lack of a DECLARATION declines it.
    from java.relations.reference_impl import disputed_observables
    got = disputed_observables(
        '[oracle:x] mismatch: p=0.9', ctx,
        check_source='double p = standardNormal.cumulativeProbability(z);')
    assert 'cumulativeProbability' not in got
