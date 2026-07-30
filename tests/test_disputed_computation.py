"""Cycle-7 (Math-65): the disputed-computation fact.

Failure class: evidence that is DELIVERED but LOST. The decisive line sat once at
character 27,051 of a 59,830-character prompt. Every reviewer that quoted it
dismissed correctly (3/3); every reviewer that accused cited nothing and asserted
the inverse from a remembered javadoc (4/4).

So the fix duplicates the method's own source next to the firing. It adds no new
evidence — the placement audit over all 230 archived prompts confirmed facts are
already adjacent to the firing, so this is a targeted addition, not a repair of
the assembly order.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'src'))

from java.relations.evidence_facts import (  # noqa: E402
    disputed_computation_fact, terminal_profile)

# A patched-class skeleton in the shape the code-context builder emits: bodies
# the patch did not touch are elided to `{ … }`.
SKELETON = """
public class Estimator {
    /** Get the RMS value. */
    public double getRMS() { … }

    /** Get a Chi-Square-like value. */
    public double getChiSquare() {
        double chiSquare = 0;
        for (int i = 0; i < rows; ++i) {
            final double residual = residuals[i];
            chiSquare += residual * residual / residualsWeights[i];
        }
        return chiSquare;
    }

    public String toString() { return "Estimator"; }
}
"""


def test_fires_when_the_firing_calls_the_method():
    fact = disputed_computation_fact(
        'metamorphic violation: expected 4*getChiSquare()==n*getRMS()^2',
        SKELETON)
    assert fact is not None
    assert 'chiSquare += residual * residual / residualsWeights[i];' in fact


def test_fires_when_the_firing_only_NAMES_the_quantity():
    """The Math-65 shape that the first implementation missed entirely: the
    message names the relation, not a call. `chiSquare_matches_...` references
    getChiSquare() but contains no parenthesis."""
    fact = disputed_computation_fact(
        'relation chiSquare_matches_weighted_residual_sum violated', SKELETON)
    assert fact is not None
    assert 'residualsWeights[i]' in fact


def test_elided_bodies_are_never_quoted():
    """`getRMS() { … }` shows no computation, so there is nothing to repeat."""
    fact = disputed_computation_fact('getRMS() disagreed', SKELETON)
    assert fact is None


def test_uninteresting_methods_are_skipped():
    fact = disputed_computation_fact('toString() mismatch', SKELETON)
    assert fact is None


def test_returns_none_without_a_firing_or_without_source():
    assert disputed_computation_fact('', SKELETON) is None
    assert disputed_computation_fact('getChiSquare() bad', '') is None
    assert disputed_computation_fact(None, None) is None


def test_body_is_verbatim_not_paraphrased():
    """The whole value is that it is copyable and checkable."""
    fact = disputed_computation_fact('getChiSquare() disagreed', SKELETON)
    for line in ('double chiSquare = 0;',
                 'final double residual = residuals[i];',
                 'return chiSquare;'):
        assert line in fact


def test_nested_braces_do_not_truncate_the_body():
    """The for-loop's braces must not end the extraction early."""
    fact = disputed_computation_fact('getChiSquare() disagreed', SKELETON)
    assert 'return chiSquare;' in fact


def test_a_runaway_body_is_not_quoted():
    """A huge method would push the rest of the evidence down — the exact
    failure this fact exists to fix."""
    big = 'public class C { public double compute() {\n' \
          + '    x += 1;\n' * 400 + '} }'
    assert disputed_computation_fact('compute() disagreed', big) is None


def test_at_most_three_methods_are_quoted():
    src = 'public class C {\n' + '\n'.join(
        f'  public double alpha{i}() {{ return {i}; }}' for i in range(8)
    ) + '\n}'
    firing = ' '.join(f'alpha{i}()' for i in range(8))
    fact = disputed_computation_fact(firing, src)
    assert fact.count('(as shown in the patched class)') == 3


# --- the neutrality guard -------------------------------------------------

def test_the_block_carries_no_dismiss_lean():
    """Cycle-5A neutralised the trigger-tier fact because a dismiss lean
    'coached the judge to discard its cleanest drift-kill catches'. This block
    fires on 34 keep-finding rows, where a dismissal would be WRONG, so it must
    state the fact symmetrically and say so explicitly."""
    fact = disputed_computation_fact('getChiSquare() disagreed', SKELETON)
    low = fact.lower()
    assert 'cuts both ways' in low
    assert 'not, by itself, grounds either way' in low
    # It must explicitly protect the generalisation case.
    assert 'generalises beyond this method' in low
    assert 'legitimate catch' in low
    # And it must not tell the reviewer what verdict to reach.
    assert 'is unsound' not in low
    assert 'must be dismissed' not in low


def test_the_block_does_not_trip_the_terminal_detector():
    """5C reads certain phrasings as terminal. A merely informational block must
    not be mistaken for one."""
    fact = disputed_computation_fact('getChiSquare() disagreed', SKELETON)
    assert terminal_profile(fact) is None


def test_the_block_says_it_is_not_new_evidence():
    """It duplicates material the reviewer already has; claiming otherwise would
    be a false fact."""
    fact = disputed_computation_fact('getChiSquare() disagreed', SKELETON)
    assert 'nothing here is new evidence' in fact.lower()
