"""8.2 stage 0 — the information rule, enforced by a CHECK not an instruction.

BLIND TO IMPLEMENTATIONS, MAXIMAL ON SPECIFICATION.

The subtle half is the buggy body, not the patched source. A reference that
copies the buggy code agrees with the buggy build EVERYWHERE -- including at the
defect -- so the off-defect screen structurally cannot catch it, and it then
disagrees with a CORRECT patch at exactly the disputed point. That is the false
accusation this mechanism exists to prevent, produced by the mechanism itself.

P4.2's lesson is why this is a refusal rather than a sentence in the prompt: a
mechanism beats an instruction, and "do not look at the implementation" is
exactly the kind of instruction models were measured ignoring.
"""
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from java.relations.reference_gen import (                      # noqa: E402
    ImplementationLeak, assert_no_implementation, build_reference_prompt,
    looks_like_implementation)

SKELETON = ('public class Chi {\n'
            '  private final double[] w;\n'
            '  public double chiSquare(double[] a, double[] b);\n}')
DOCS = ['/** Computes the chi-square statistic. @param a observed counts */']
TEST = 'public void testChi() { assertEquals(3.3, chiSquare(x, y), 1e-9); }'


def test_a_body_is_detected():
    assert looks_like_implementation('public int f() { return a + b; }')
    assert looks_like_implementation('if (x > 0) { y = 1; }')
    assert looks_like_implementation('throw new IllegalArgumentException();')


def test_a_skeleton_is_not_a_body():
    assert looks_like_implementation(SKELETON) is None
    assert looks_like_implementation(DOCS[0]) is None


def test_the_prompt_REFUSES_material_carrying_an_implementation():
    """The whole rule, as a refusal."""
    with pytest.raises(ImplementationLeak) as e:
        build_reference_prompt(
            'chiSquare',
            skeleton='public double chiSquare(double[] a) { return sum / n; }',
            docs=DOCS, failing_test=TEST)
    assert 'bug-copying reference' in str(e.value)


def test_docs_carrying_a_body_are_refused_too():
    with pytest.raises(ImplementationLeak):
        build_reference_prompt('chiSquare', skeleton=SKELETON,
                               docs=['/** ... */ double f(){ return 1; }'],
                               failing_test=TEST)


def test_TESTS_are_exempt_because_they_are_specification():
    """The failing test and sibling tests contain code by nature. They are the
    project's executable spec and tier-1 authority -- leaking them leaks only
    truth. This is the deleted mined-oracles MATERIAL in a legitimate use."""
    assert_no_implementation({'failing_test': 'void t(){ assertEquals(1, f()); }',
                              'other_tests': 'void u(){ if (x) { y(); } }'})


def test_a_clean_prompt_is_built_and_carries_the_spec_surface():
    msgs = build_reference_prompt('chiSquare', SKELETON, DOCS, TEST,
                                  other_tests=['void t2(){ assertTrue(x); }'],
                                  shown_examples={'f(1)': '2'},
                                  package='org.x')
    body = msgs[1]['content']
    assert 'DOCUMENTATION' in body and 'chi-square statistic' in body
    assert 'THE FAILING TEST' in body and 'assertEquals(3.3' in body
    assert "executable specification" in body
    assert 'CONVENTIONS' in body and 'f(1) -> 2' in body
    assert 'SKELETON' in body
    assert 'package org.x' in body


def test_the_prompt_never_asks_the_model_to_avoid_looking():
    """If the material is clean there is nothing to avoid, and an instruction
    would imply the check is optional."""
    body = build_reference_prompt('chiSquare', SKELETON, DOCS, TEST)[1]['content']
    assert 'do not look at the patch' not in body.lower()
    assert 'not shown any existing implementation' in body.lower()


def test_the_system_prompt_states_the_information_position():
    msgs = build_reference_prompt('chiSquare', SKELETON, DOCS, TEST)
    assert 'never an existing implementation' in msgs[0]['content']
