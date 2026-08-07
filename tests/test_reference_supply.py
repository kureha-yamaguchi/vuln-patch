"""8.2 — the screening surface: DISTINCT observables, and a live buggy twin.

Two design decisions, both load-bearing, both pinned here.

1. MIN_SCREENED_OBSERVABLES counts DISTINCT OBSERVABLES, not input/output pairs.
   The disputed point is on-defect almost by definition -- it is where the bug
   lives -- so N vectors through that one formula are N correlated samples of a
   single claim. They would satisfy the letter of the screen while gutting its
   independence. The class's documented SIBLING observables, computed from the
   same state, are the genuinely off-defect surface.

2. Fuzzed vectors have no recorded buggy values, so the buggy build is executed
   LIVE on the same constructed states. Admissible: the buggy build is authority
   rank 2 whether its values are archived or produced now.

Throws are OBSERVABLES, not skips. A documented rejection contract is behaviour:
matching throws are agreement (shared semantics), a one-sided throw is a
disagreement -- exactly the misunderstanding the screen exists to catch.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from java.relations.evidence_facts import observed_values          # noqa: E402
from java.relations.reference_impl import (                        # noqa: E402
    MIN_SCREENED_OBSERVABLES, screen_reference)
from java.relations.reference_run import (                         # noqa: E402
    build_buggy_twin_driver, build_driver)

OBS = ['chiSquare', 'getRMS', 'cost']
VECS = ['a1, w1', 'a2, w2']


def test_results_are_keyed_by_OBSERVABLE_not_by_input():
    out = '\n'.join(['chiSquare=1.0', 'chiSquare=2.0', 'getRMS=3.0',
                     'cost=4.0'])
    vals = observed_values(out)
    assert sorted(vals) == ['chiSquare', 'cost', 'getRMS']
    assert vals['chiSquare'] == ['1.0', '2.0'], 'inputs become VALUES, not keys'


def test_many_vectors_through_ONE_formula_do_NOT_clear_the_screen():
    """THE INTERPRETATION, as a test. Ten samples of one claim is still one
    claim, and the screen must say so."""
    ten = observed_values('\n'.join(f'chiSquare={i}.0' for i in range(10)))
    ok, why = screen_reference(ten, ten, off_defect_keys={'chiSquare'})
    assert ok is False
    assert f'{MIN_SCREENED_OBSERVABLES} required' in why


def test_three_DISTINCT_observables_do_clear_it():
    three = observed_values('chiSquare=1.0\ngetRMS=2.0\ncost=3.0')
    ok, _why = screen_reference(three, three, off_defect_keys=set(three))
    assert ok is True


def test_the_reference_driver_calls_every_observable_on_every_vector():
    d = build_driver('ReferenceImpl', OBS, VECS)
    assert len(re.findall(r'ReferenceImpl\.', d)) == len(OBS) * len(VECS)
    assert sorted(set(re.findall(r'println\("(\w+)=', d))) == sorted(OBS)


def test_the_buggy_twin_reads_the_SAME_keys():
    """The two dictionaries must compare key-for-key, or the screen silently
    shares nothing and discards every reference."""
    r = build_driver('ReferenceImpl', OBS, VECS)
    b = build_buggy_twin_driver('org.X.Opt', 'new org.X.Opt({vec})', OBS, VECS)
    assert (sorted(set(re.findall(r'println\("(\w+)=', r)))
            == sorted(set(re.findall(r'println\("(\w+)=', b))))


def test_the_twin_constructs_the_object_from_each_vector():
    b = build_buggy_twin_driver('org.X.Opt', 'new org.X.Opt({vec})', OBS, VECS)
    assert 'new org.X.Opt(a1, w1)' in b and 'new org.X.Opt(a2, w2)' in b
    assert '{vec}' not in b, 'the placeholder must be substituted'


def test_both_drivers_record_a_throw_rather_than_skipping_it():
    for src in (build_driver('R', OBS, VECS),
                build_buggy_twin_driver('C', 'new C({vec})', OBS, VECS)):
        assert 'catch (Throwable t)' in src
        assert 'EX:' in src


def test_matching_throws_are_AGREEMENT():
    ref = observed_values('chiSquare=EX:IllegalArgumentException\n'
                          'getRMS=1.0\ncost=2.0')
    bug = observed_values('chiSquare=EX:IllegalArgumentException\n'
                          'getRMS=1.0\ncost=2.0')
    ok, _ = screen_reference(ref, bug, off_defect_keys=set(ref))
    assert ok is True, 'a shared documented rejection is shared semantics'


def test_a_ONE_SIDED_throw_is_a_disagreement():
    ref = observed_values('chiSquare=5.0\ngetRMS=1.0\ncost=2.0')
    bug = observed_values('chiSquare=EX:IllegalArgumentException\n'
                          'getRMS=1.0\ncost=2.0')
    ok, why = screen_reference(ref, bug, off_defect_keys=set(ref))
    assert ok is False and 'DISCARDED' in why


# --- the sibling extractor: measured, not guessed --------------------------

def test_the_extractor_sees_the_whole_observable_surface():
    """A first walkthrough extracted 2 observables from Math-65 and concluded
    the class was too thin to screen. The class has 16. The regex matched only
    `double` returns -- measuring a mechanism's reach with a matcher that sees a
    third of the data is how a design gets abandoned for a property it does not
    have."""
    from java.relations.reference_gen import sibling_observables
    ctx = '\n'.join([
        'public class Opt {',
        '  public double getChiSquare() { }',
        '  public double getRMS() { }',
        '  public double[][] getCovariances() { }',
        '  public int getIterations() { }',
        '  public int getMaxIterations() { }',
        '  public double[] getPoint() { }',
        '  public VectorialConvergenceChecker getConvergenceChecker() { }',
        '  public double evaluate(double x) { }',
        '}'])
    sibs = sibling_observables(ctx, 'getChiSquare')
    assert 'getRMS' in sibs and 'getCovariances' in sibs
    assert 'getChiSquare' not in sibs, 'the disputed point cannot screen itself'
    assert 'evaluate' not in sibs, 'takes an argument; not a no-arg observable'
    assert 'getConvergenceChecker' not in sibs, 'object type; not comparable'


def test_stored_settings_sort_AFTER_computed_quantities():
    """A getter echoing a constructor argument agrees trivially and screens
    nothing, so it must not crowd out a real computation under the cap."""
    from java.relations.reference_gen import sibling_observables
    ctx = ('public class C {\n'
           '  public int getMaxIterations() { }\n'
           '  public double getRMS() { }\n'
           '  public double getX() { }\n}')
    sibs = sibling_observables(ctx, 'getChiSquare', cap=2)
    assert 'getMaxIterations' not in sibs
    assert set(sibs) == {'getRMS', 'getX'}
