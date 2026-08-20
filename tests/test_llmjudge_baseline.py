"""Guards for the one-shot LLM baseline in src/baseline_llmjudge/.

Three properties are asserted here, and each one is a claim the README makes:

  1. INPUT PARITY. Every block marked 'reused' really is the pipeline's own
     text, not a copy of it. A copy would drift the moment the harness prompt
     changed, and the parity claim would quietly become false.
  2. NO LABEL LEAKAGE. The ground truth sits in the patch path
     (drr/Patches/Dcorrect/... against .../Doverfitting/...) and in the
     certification rows. It must reach the patch selector only. No rendered
     block and no built prompt may carry it.
  3. NO HARNESS AUTHORSHIP. The baseline writes no .java file, so a rendered
     block that tells the model how to write one is evidence that the wrong
     pipeline section was carried over.

Every test here is offline. It builds the context objects by hand, so no
Defects4J checkout, no test run and no model call is needed.
"""
import pytest

from java.bug_context.analysis import PatchContext, TouchedFunction
from java.bug_context.failure_test import FailureTest
from java.harness.prompts import PromptBuilder

from baseline_llmjudge import evidence, evidence_semantic, prompts

PB = PromptBuilder()

# The two words the drr dataset labels a patch with, plus the directory names
# they appear as in a patch path. A rendered block carrying any of these has
# leaked the answer into the question.
LABEL_TOKENS = ('Dcorrect', 'Doverfitting', 'drr_label', 'verified_correct',
                'verified_incorrect', 'plausible')

# Phrases that only make sense to something that writes a Jazzer harness.
AUTHORSHIP_TOKENS = ('FuzzerSecurityIssue', 'FuzzedDataProvider',
                     'fuzzerTestOneInput', 'com.code_intelligence',
                     'hard-code', 'your harness', 'Your harness',
                     'no markdown fences', 'metamorphic relation')


@pytest.fixture
def fn():
    return TouchedFunction(
        func_name='substringBetween',
        func_signature='String substringBetween(String, String)',
        func_source=('public static String substringBetween(String s, '
                     'String tag) {\n    return s.substring(1);\n}'),
        func_class='StringUtils',
        func_class_fq='org.apache.commons.lang3.StringUtils',
        func_param_types=['String', 'String'],
    )


@pytest.fixture
def context(fn):
    return PatchContext(
        modified_files=['src/main/java/org/apache/commons/lang3/'
                        'StringUtils.java'],
        patch_text=('--- a/StringUtils.java\n+++ b/StringUtils.java\n'
                    '-    return s.substring(1);\n'
                    '+    if (s.isEmpty()) return null;\n'
                    '+    return s.substring(1);\n'),
        functions=[fn],
        package='org.apache.commons.lang3',
        root_cause_reachable=['StringUtils.substring',
                              'StringUtils.indexOf'],
        source_imports=['import java.util.List;'],
    )


@pytest.fixture
def crashing_test():
    return FailureTest(
        test_class='org.apache.commons.lang3.StringUtilsTest',
        test_method='testSubstringBetween',
        source_path='/checkout/StringUtilsTest.java',
        method_source=('public void testSubstringBetween() {\n'
                       '    StringUtils.substringBetween("", "x");\n}'),
        exception_type='java.lang.StringIndexOutOfBoundsException',
    )


@pytest.fixture
def semantic_test():
    return FailureTest(
        test_class='org.apache.commons.lang3.StringUtilsTest',
        test_method='testSubstringBetween',
        source_path='/checkout/StringUtilsTest.java',
        method_source=('public void testSubstringBetween() {\n'
                       '    assertEquals("b", StringUtils.'
                       'substringBetween("abc", "a"));\n}'),
        exception_type='junit.framework.AssertionFailedError',
        failure_message=('junit.framework.AssertionFailedError: '
                         'expected:<b> but was:<bc>'),
        support_source=('// --- setUp() (test setup) ---\n'
                        'public void setUp() { this.tag = "a"; }'),
    )


@pytest.fixture
def semantic_facts():
    """The extra evidence the pipeline's semantic branch carries."""
    return {
        'class_context': ['// class org.apache.commons.lang3.StringUtils\n'
                          'public static String substring(String, int)'],
        # `javadoc_for` strips the ' * ' gutter before it returns, so the
        # tag lines reach the renderer bare. `_preconditions_block` selects on
        # exactly that shape, and this renderer must select the same lines.
        'javadocs': ['Return the substring between two tags.\n'
                     '@param s the string\n'
                     '@throws IllegalArgumentException on a null tag'],
        'sibling_hints': 'SAME-NAME OVERLOADS:\n  substringBetween(String)',
    }


class FakeCrashInput:
    """Shaped like java.bug_context.crash_input.CrashInput."""
    exception_type = 'java.lang.StringIndexOutOfBoundsException'
    message = 'String index out of range: 1'
    throw_site = 'java.lang.String.substring(String.java:1963)'
    literals = ['', 'x']
    best_anchor = ''
    has_evidence = True


def _crashing_blocks(context, crashing_test):
    return evidence.render(context, [crashing_test], FakeCrashInput())


def _semantic_blocks(context, semantic_test, semantic_facts):
    return evidence_semantic.render(context, [semantic_test], semantic_test,
                                    **semantic_facts)


# --- 1. input parity ---------------------------------------------------------

def test_reused_blocks_are_the_pipelines_own_text(context, crashing_test, fn):
    """Each 'reused' block equals the pipeline method's own output."""
    blocks = {b.name: b for b in _crashing_blocks(context, crashing_test)}

    assert blocks['patch'].origin == 'reused'
    assert blocks['patch'].text == PB._patch_block(context.patch_text)

    assert blocks['source_imports'].origin == 'reused'
    assert (blocks['source_imports'].text
            == PB._imports_block(context.source_imports))

    key = f'touched_function:{fn.func_name}'
    assert blocks[key].origin == 'reused'
    assert blocks[key].text == PB._function_block(fn)


def test_semantic_reused_blocks_are_the_pipelines_own_text(
        context, semantic_test, semantic_facts, fn):
    blocks = {b.name: b for b in
              _semantic_blocks(context, semantic_test, semantic_facts)}

    assert blocks['patch'].text == PB._patch_block(context.patch_text)
    assert (blocks['source_imports'].text
            == PB._imports_block(context.source_imports))
    assert (blocks[f'touched_function:{fn.func_name}'].text
            == PB._function_block(fn))
    for name in ('patch', 'source_imports',
                 f'touched_function:{fn.func_name}'):
        assert blocks[name].origin == 'reused'


def test_the_two_renderers_share_their_reused_blocks(
        context, crashing_test, semantic_test, semantic_facts):
    """A block reused by both renderers must be the same string in both."""
    crashing = {b.name: b.text
                for b in _crashing_blocks(context, crashing_test)}
    semantic = {b.name: b.text
                for b in _semantic_blocks(context, semantic_test,
                                          semantic_facts)}
    for name in ('patch', 'source_imports',
                 'touched_function:substringBetween'):
        assert crashing[name] == semantic[name]


@pytest.mark.parametrize('module', [evidence, evidence_semantic])
def test_manifest_names_every_block_and_the_dropped_sections(
        module, context, crashing_test, semantic_test, semantic_facts):
    if module is evidence:
        blocks = _crashing_blocks(context, crashing_test)
    else:
        blocks = _semantic_blocks(context, semantic_test, semantic_facts)
    manifest = module.manifest(blocks)

    assert manifest['renderer_version'] == module.RENDERER_VERSION
    assert [b['name'] for b in manifest['blocks']] == [b.name for b in blocks]
    assert manifest['total_chars'] == sum(b.chars for b in blocks)
    assert (manifest['dropped_pipeline_sections']
            == list(module.DROPPED_SECTIONS))


@pytest.mark.parametrize('module', [evidence, evidence_semantic])
def test_evidence_text_joins_blocks_as_the_pipeline_joins_sections(
        module, context, crashing_test, semantic_test, semantic_facts):
    if module is evidence:
        blocks = _crashing_blocks(context, crashing_test)
    else:
        blocks = _semantic_blocks(context, semantic_test, semantic_facts)
    assert (module.evidence_text(blocks)
            == '\n\n'.join(b.text for b in blocks))


def test_the_two_renderers_carry_separate_version_numbers():
    """The context cache keys on the renderer version of its own kind.

    One shared constant would mean a semantic rendering change invalidated
    every cached crashing entry, and every published crashing
    evidence_sha256 would then point at a cache that no longer exists."""
    assert evidence.RENDERER_VERSION is not None
    assert evidence_semantic.RENDERER_VERSION is not None
    assert (evidence_semantic.__name__ != evidence.__name__)


# --- 2. no label leakage -----------------------------------------------------

@pytest.mark.parametrize('version', prompts.known_versions('crashing'))
def test_no_label_reaches_a_crashing_prompt(context, crashing_test, version):
    text = evidence.evidence_text(_crashing_blocks(context, crashing_test))
    built = '\n'.join(m['content']
                      for m in prompts.build_messages(version, text))
    for token in LABEL_TOKENS:
        assert token not in built, f'{version} leaks {token!r}'


@pytest.mark.parametrize('version', prompts.known_versions('semantic'))
def test_no_label_reaches_a_semantic_prompt(context, semantic_test,
                                            semantic_facts, version):
    text = evidence_semantic.evidence_text(
        _semantic_blocks(context, semantic_test, semantic_facts))
    built = '\n'.join(m['content']
                      for m in prompts.build_messages(version, text))
    for token in LABEL_TOKENS:
        assert token not in built, f'{version} leaks {token!r}'


def test_no_label_reaches_a_rendered_block(context, crashing_test,
                                           semantic_test, semantic_facts):
    blocks = (_crashing_blocks(context, crashing_test)
              + _semantic_blocks(context, semantic_test, semantic_facts))
    for block in blocks:
        for token in LABEL_TOKENS:
            assert token not in block.text, f'{block.name} leaks {token!r}'


# --- 3. no harness authorship ------------------------------------------------

def test_no_rendered_block_teaches_harness_authorship(
        context, crashing_test, semantic_test, semantic_facts):
    blocks = (_crashing_blocks(context, crashing_test)
              + _semantic_blocks(context, semantic_test, semantic_facts))
    for block in blocks:
        for token in AUTHORSHIP_TOKENS:
            assert token not in block.text, f'{block.name} carries {token!r}'


def test_the_semantic_renderer_carries_the_reported_wrong_value(
        context, semantic_test, semantic_facts):
    """The failure message is the semantic counterpart of the throwable.

    It names the observable that diverges and the wrong value the buggy
    build produces, so a renderer that dropped it would hand the baseline a
    weaker question than the pipeline asks."""
    blocks = {b.name: b.text
              for b in _semantic_blocks(context, semantic_test,
                                        semantic_facts)}
    assert 'expected:<b> but was:<bc>' in blocks['trigger_tests']
    assert semantic_test.support_source in blocks['trigger_tests']
    assert semantic_test.method_source in blocks['trigger_tests']


def test_the_semantic_renderer_drops_the_relation_and_divergence_evidence():
    """Both need something the baseline never does.

    A screened relation needs a compile and a run on the buggy build. A
    divergence needs the PATCHED build. Section 2 of the README lists them
    as evidence the pipeline gets and the baseline does not."""
    dropped = evidence_semantic.DROPPED_SECTIONS
    assert '_synthesized_relations_block' in dropped
    assert '_metamorphic_block' in dropped
    assert '_fdp_reference' in dropped
    assert '_skeleton_block' in dropped


def test_the_semantic_renderer_keeps_the_class_and_contract_facts(
        context, semantic_test, semantic_facts):
    blocks = {b.name: b.text
              for b in _semantic_blocks(context, semantic_test,
                                        semantic_facts)}
    assert 'public static String substring(String, int)' \
        in blocks['class_skeletons']
    assert '@param s the string' in blocks['documented_contract']
    assert '@throws IllegalArgumentException on a null tag' \
        in blocks['documented_contract']
    assert 'substringBetween(String)' in blocks['sibling_and_state']


def test_the_contract_block_selects_the_pipelines_own_javadoc_lines(
        semantic_facts):
    """Same tag selection as `_preconditions_block`, line for line.

    The pipeline drops the rejection-ordering rule and the valid-by-
    construction instruction, and this renderer drops them too. What must not
    differ is WHICH javadoc lines survive: a renderer that kept one more line
    than the pipeline would give the baseline evidence the pipeline withholds,
    and one that kept fewer would weaken the baseline for no reason."""
    javadocs = semantic_facts['javadocs']
    mine = evidence_semantic._documented_contract_block(javadocs)
    theirs = PB._preconditions_block(javadocs)

    def tag_lines(text):
        return [ln.strip().lstrip('- ').strip() for ln in text.splitlines()
                if ln.strip().lstrip('- ').startswith(('@param', '@throws',
                                                       '@exception'))]

    assert tag_lines(mine) == tag_lines(theirs)
    assert tag_lines(mine)


def test_both_contract_blocks_are_empty_without_a_documented_tag():
    undocumented = ['Return the substring between two tags.']
    assert evidence_semantic._documented_contract_block(undocumented) == ''
    assert PB._preconditions_block(undocumented) == ''
    assert evidence_semantic._documented_contract_block([]) == ''


# --- the prompt-version registry --------------------------------------------

def test_every_registered_version_builds_two_messages():
    for name in prompts.known_versions():
        messages = prompts.build_messages(name, 'EVIDENCE')
        assert [m['role'] for m in messages] == ['system', 'user']
        assert messages[0]['content'] == prompts.SYSTEM
        assert 'EVIDENCE' in messages[1]['content']


def test_every_registered_version_demands_the_verdict_contract():
    for name in prompts.known_versions():
        assert 'VERDICT: OVERFITTING' in prompts.version_text(name)
        assert 'VERDICT: CORRECT' in prompts.version_text(name)


def test_every_registered_version_belongs_to_exactly_one_kind():
    for kind in ('crashing', 'semantic'):
        for name in prompts.known_versions(kind):
            assert prompts.kind_of(name) == kind


def test_an_iteration_inherits_its_bases_kind():
    for name in prompts.known_versions():
        if prompts.is_iteration(name):
            assert (prompts.kind_of(name)
                    == prompts.kind_of(prompts.base_of(name)))


def test_the_two_kinds_share_no_version_name():
    crashing = set(prompts.known_versions('crashing'))
    semantic = set(prompts.known_versions('semantic'))
    assert not crashing & semantic
    assert crashing | semantic == set(prompts.known_versions())


def test_register_refuses_a_duplicate_name():
    iterations = [n for n in prompts.known_versions()
                  if prompts.is_iteration(n)]
    if not iterations:
        pytest.skip('no iteration registered yet')
    with pytest.raises(ValueError, match='already registered'):
        prompts.register(prompts.PromptVersion(
            name=iterations[-1], hypothesis='x', task='x', instruction='x'))


def test_register_refuses_an_unknown_base():
    with pytest.raises(ValueError):
        prompts.register(prompts.PromptVersion(
            name='v99.1', hypothesis='x', task='x', instruction='x'))


def test_register_refuses_a_name_that_is_not_an_iteration():
    with pytest.raises(ValueError, match='not an iteration name'):
        prompts.register(prompts.PromptVersion(
            name='v4', hypothesis='x', task='x', instruction='x'))


def test_resolve_refuses_an_unregistered_version():
    with pytest.raises(ValueError, match='unknown prompt version'):
        prompts.resolve('nope')
