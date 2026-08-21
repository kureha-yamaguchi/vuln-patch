"""Render the evidence one semantic-bug patch carries.

A semantic bug is one that nothing at run time reports: its trigger test fails
a JUnit assertion because the code returns a wrong value, and no throwable
escapes. The pipeline's harness prompt for such a bug is the semantic branch of
`PromptBuilder.build` — `_build_semantic` in java/harness/prompts.py. It joins
up to eleven sections. This module rebuilds the factual ones so the baseline
reasons over the same evidence, and it drops the ones that only teach Jazzer
harness authorship.

`evidence.py` is the crashing counterpart. The two modules stay separate for
one reason: each carries its own renderer version, and the context cache keys
on that number. One shared constant would mean a change here invalidated every
cached crashing entry, and every published crashing `evidence_sha256` would
then point at a cache that no longer exists.

Every block records where its text came from, so the parity claim is auditable
from the run artifact instead of from a prose promise:

  block                  | pipeline section            | origin here
  -----------------------+-----------------------------+-----------------
  patch                  | _patch_block                | reused verbatim
  source_imports         | _imports_block              | reused verbatim
  touched_function       | _function_block             | reused verbatim
  class_skeletons        | _class_context_block        | re-rendered
  documented_contract    | _preconditions_block        | re-rendered
  sibling_and_state      | sibling_and_state_hints     | re-rendered
  trigger_tests          | _lifted_assertion_block     | re-rendered
  root_cause_reachable   | _variant_analysis_block     | shared renderer

The five re-rendered blocks exist because the pipeline fuses facts with
instructions inside one section:

  * `_class_context_block` wraps the class skeletons in a request to find
    cross-member consistency oracles. The skeletons are the fact and they
    stay, under a neutral heading.
  * `_preconditions_block` lists the @param and @throws lines of the touched
    methods, then states the rejection-oracle ordering rule. The javadoc lines
    are the fact and they stay, under the same 900-character cap. The ordering
    rule is harness authorship, so it goes.
  * `sibling_and_state_hints` lists the overload groups, the method families
    and the no-argument readers of the touched class. Each of its three
    headings ends with advice about the check to write. The lists are the fact
    and they stay. The headings are trimmed at their first bracket.
  * `_lifted_assertion_block` is mostly instruction: it tells the model to
    lift every assertion, reconstruct each call, and throw on a mismatch. It
    also calls `_metamorphic_block` on its last line. So the method cannot be
    reused, and the facts inside it are re-rendered here in the same order,
    from the same fields: the reported wrong value, the API class the test
    drives, the chosen test's body under the same 1500-character cap, that
    test's support source, and the names of the bug's other trigger tests.
  * `_variant_analysis_block` lists the root-cause reachable set, then tells
    the harness which part of it earlier harnesses already covered. The list
    is evidence and is kept under the same `MAX_REACHABLE_IN_PROMPT` cap. This
    module calls `evidence.reachable_block`, because the pipeline builds this
    section from one method on both of its paths.

Dropped outright, because each states a rule about the .java file the model
must emit: `_hard_constraints`, `_intro`, `_metamorphic_block`,
`_fdp_reference`, `_skeleton_block`.

Dropped for a second reason, and disclosed in the manifest: two kinds of
evidence the pipeline gets from work the baseline never does.

  * `_synthesized_relations_block`. A relation candidate comes from a separate
    model call, and only a candidate that survives a compile and a run on the
    buggy build ever reaches a prompt.
  * The `--divcap` divergence facts. Collecting one needs the PATCHED build.

Both sit beside execution evidence on the list of things the pipeline observes
and the baseline does not. That gap is what the experiment measures.
"""
from typing import List, Optional

from java.harness.prompts import PromptBuilder

# `Block` and `evidence_text` are shared with the crashing renderer, so the two
# baselines cannot disagree about what a block is or how blocks are joined.
from baseline_llmjudge.defects4j.evidence import reachable_block
from baseline_llmjudge.shared.blocks import Block, evidence_text

# `evidence_text` is re-exported, not called here: a caller reads the renderer
# of its own pool and asks that module for everything.
__all__ = ['RENDERER_VERSION', 'DROPPED_SECTIONS', 'WITHHELD_EVIDENCE',
           'Block', 'evidence_text', 'render', 'manifest']

# Bump when a block's rendering changes. The context cache keys on it, so an
# old cache can never be silently mixed with a new rendering.
RENDERER_VERSION = 1

# Same caps the pipeline applies.
MAX_TEST_CHARS = 1500          # _lifted_assertion_block's trigger-test body
MAX_CONTRACT_CHARS = 900       # _preconditions_block's javadoc lines

# Sections of the semantic prompt this module deliberately does not carry.
DROPPED_SECTIONS = (
    '_hard_constraints', '_intro', '_synthesized_relations_block',
    '_metamorphic_block', '_fdp_reference', '_skeleton_block',
)

# Evidence the pipeline has and the baseline does not, with the reason. This
# is a disclosure, not a rendering choice: each entry needs work the baseline
# never does, so no wording here could close the gap.
WITHHELD_EVIDENCE = (
    'synthesized_relations: a relation candidate comes from a separate model '
    'call, and only a candidate that survives a compile and a run on the '
    'buggy build reaches a prompt',
    'divcap_divergences: collecting one needs the patched build',
)

# Stateless, so one shared instance is enough. Calling the pipeline's own
# methods is the point: a copy of their text would drift the moment the
# harness prompt changes, and the parity claim would quietly become false.
_PB = PromptBuilder()

# Javadoc tags `_preconditions_block` keeps. Same three, in the same order.
_CONTRACT_TAGS = ('@param', '@throws', '@exception')


def render(context, failure_tests, chosen=None, *,
           class_context: Optional[List[str]] = None,
           javadocs: Optional[List[str]] = None,
           sibling_hints: Optional[str] = None) -> List[Block]:
    """The evidence blocks for one candidate patch, in prompt order.

    `chosen` is the trigger test whose assertion the pipeline lifts on this
    attempt. The baseline decides once, so it renders the first test with a
    source body when the caller names none."""
    blocks: List[Block] = [
        Block('patch', 'reused', _PB._patch_block(context.patch_text)),
    ]
    if context.source_imports:
        blocks.append(Block('source_imports', 'reused',
                            _PB._imports_block(context.source_imports)))
    for fn in context.functions:
        blocks.append(Block(f'touched_function:{fn.func_name}', 'reused',
                            _PB._function_block(fn)))
    skeletons = _class_skeletons_block(class_context)
    if skeletons:
        blocks.append(Block('class_skeletons', 'rendered', skeletons))
    contract = _documented_contract_block(javadocs)
    if contract:
        blocks.append(Block('documented_contract', 'rendered', contract))
    siblings = _sibling_and_state_block(sibling_hints)
    if siblings:
        blocks.append(Block('sibling_and_state', 'rendered', siblings))
    trigger = _trigger_tests_block(failure_tests, chosen)
    if trigger:
        blocks.append(Block('trigger_tests', 'rendered', trigger))
    reachable = reachable_block(context.root_cause_reachable)
    if reachable:
        blocks.append(Block('root_cause_reachable', 'rendered', reachable))
    return blocks


def manifest(blocks: List[Block]) -> dict:
    """The input parity manifest: what the baseline saw, and where from."""
    return {
        'renderer_version': RENDERER_VERSION,
        'blocks': [{'name': b.name, 'origin': b.origin, 'chars': b.chars}
                   for b in blocks],
        'total_chars': sum(b.chars for b in blocks),
        'dropped_pipeline_sections': list(DROPPED_SECTIONS),
        'withheld_pipeline_evidence': list(WITHHELD_EVIDENCE),
    }


# --- re-rendered blocks ------------------------------------------------------

def _class_skeletons_block(class_context: Optional[List[str]]) -> str:
    """The touched class(es), their supertypes and their collaborators.

    Same source list and same tag name as `_class_context_block`, without its
    request to hunt for a cross-member oracle."""
    if not class_context:
        return ''
    return '\n'.join([
        "Partial skeletons of the class(es) this patch touches, of their"
        " supertypes, and of the classes the failing test itself uses."
        " Signatures, fields and javadoc are shown. A method body is shown"
        " only for a touched method.",
        "<class_skeletons>",
        '\n\n'.join(class_context),
        "</class_skeletons>",
    ])


def _documented_contract_block(javadocs: Optional[List[str]]) -> str:
    """The documented contract of the touched method(s).

    Same tag selection, same de-duplication and same 900-character cap as
    `_preconditions_block`. Its rejection-oracle ordering rule is harness
    authorship, so it goes."""
    if not javadocs:
        return ''
    lines: List[str] = []
    for doc in javadocs:
        for raw in (doc or '').splitlines():
            line = raw.strip()
            if line.startswith(_CONTRACT_TAGS) and line not in lines:
                lines.append(line)
    if not lines:
        return ''
    parts = [
        "Documented contract of the touched method(s), from their javadoc."
        " An input that violates an @param constraint is invalid, and a"
        " documented @throws on such an input is correct behaviour.",
    ]
    total = 0
    for line in lines:
        if total + len(line) > MAX_CONTRACT_CHARS:
            parts.append("  - ... (further documented constraints elided)")
            break
        parts.append(f"  - {line}")
        total += len(line)
    return '\n'.join(parts)


def _sibling_and_state_block(sibling_hints: Optional[str]) -> str:
    """The overload groups, method families and readable state of the class.

    `sibling_and_state_hints` renders three parts, and each heading ends with
    advice about the check to write. The item lines are the fact. Each heading
    is therefore trimmed at its first bracket, which leaves the neutral name
    of the list and drops the advice with it."""
    if not sibling_hints or not sibling_hints.strip():
        return ''
    parts = [_neutral_heading(p) for p in sibling_hints.split('\n\n')]
    parts = [p for p in parts if p.strip()]
    if not parts:
        return ''
    return '\n'.join([
        "Mechanically listed members of the touched class. They are raw"
        " material for a judgement about a sibling path, not a claim about"
        " one.",
        *parts,
    ])


def _neutral_heading(part: str) -> str:
    """One part of the hint block, with the advice cut out of its heading.

    The heading runs to the first colon. Trimming it at the first bracket is
    safe under truncation too: the hint block is capped, so a part can end
    inside its own bracket and leave no colon behind at all."""
    head, colon, tail = part.partition(':')
    head = head.split('(')[0].strip().rstrip(' -—')
    if not colon:
        return head
    return f'{head}:{tail}'


def _trigger_tests_block(failure_tests, chosen) -> str:
    """The reported symptom: the failing test, and the wrong value it saw.

    Mirrors the facts of `_lifted_assertion_block`, in its order, from its
    fields. The reported wrong value is the semantic counterpart of the
    crashing path's observed throwable: it names the observable that diverges
    and the value the buggy build produced for it."""
    tests = list(failure_tests or [])
    if not tests:
        return ''
    target = chosen or next((ft for ft in tests if ft.has_source), tests[0])

    parts: List[str] = [
        "The project's own test below FAILS on the buggy code. It is the"
        " reported symptom of this bug — the failure the patch was written to"
        " remove. Nothing throws: the test fails a JUnit assertion, because"
        " the code returns a wrong value.",
    ]
    message = getattr(target, 'failure_message', None)
    if message:
        parts.extend([
            "The failure this exact test produced on the buggy code. This is"
            " the verified failure; trust it over anything inferred from the"
            " test body. It names the observable that diverges and the wrong"
            " value the buggy code returns for it.",
            "<observed_failure>",
            message,
            "</observed_failure>",
        ])
    entry = _api_classes(tests)
    if entry:
        parts.append("Public API class(es) the failing test drives: "
                     + ', '.join(entry))
    if target.method_source:
        body = target.method_source
        if len(body) > MAX_TEST_CHARS:
            body = body[:MAX_TEST_CHARS] + "\n        // ... (truncated)"
        parts.extend([
            f'<failing_test class="{target.test_class}"'
            f' method="{target.test_method}">',
            body,
            "</failing_test>",
        ])
    else:
        parts.append(f'<failing_test class="{target.test_class}"'
                     f' method="{target.test_method}" />')
    support = getattr(target, 'support_source', None)
    if support:
        parts.extend([
            "What the test method uses from its own test class: the setup,"
            " the helper methods, the constants, and the fixture files.",
            f'<test_support class="{target.test_class}">',
            support,
            "</test_support>",
        ])
    others = [f'{ft.test_class}::{ft.test_method}'
              for ft in tests if ft is not target]
    if others:
        parts.append("Other trigger test(s) for this same bug, which probe the"
                     " same root cause: " + ', '.join(others) + '.')
    return '\n'.join(parts)


def _api_classes(failure_tests) -> List[str]:
    """The production class(es) the trigger test(s) drive.

    Same derivation as `_entry_point_hint`: the test's enclosing class, minus
    its package and a trailing 'Test'. That method returns the name fused
    with an instruction to drive fuzzed input through it, so only the
    derivation is shared, not the text."""
    names: List[str] = []
    for ft in failure_tests:
        simple = ft.test_class.rsplit('.', 1)[-1]
        if simple.endswith('Test') and len(simple) > 4:
            simple = simple[:-4]
        if simple and simple not in names:
            names.append(simple)
    return names
