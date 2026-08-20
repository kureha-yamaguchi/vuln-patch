"""Render the evidence one crashing-bug patch carries.

The pipeline's harness prompt is the crashing branch of
`PromptBuilder.build` (java/harness/prompts.py). It joins ten sections. Four
of them state FACTS about the patch and the bug; six of them tell the model
how to write a Jazzer harness. This module rebuilds the factual four so the
baseline reasons over the same evidence, and drops the six that only teach
harness authorship.

Every block records where its text came from, so the parity claim is auditable
from the run artifact instead of from a prose promise:

  block                  | pipeline section        | origin here
  -----------------------+-------------------------+-----------------
  patch                  | _patch_block            | reused verbatim
  source_imports         | _imports_block          | reused verbatim
  touched_function       | _function_block         | reused verbatim
  trigger_tests          | _failure_test_block     | re-rendered
  root_cause_reachable   | _variant_analysis_block | re-rendered

The two re-rendered blocks exist because the pipeline fuses facts with
instructions inside one section:

  * `_failure_test_block` wraps the trigger test in an ANCHOR/EXPLORE
    strategy, and its crash-evidence sub-block ends every anchor with
    "hard-code this verbatim as your first call, then fuzz". The facts kept
    here are the same ones, in the same order, from the same fields: the
    observed throwable, its message, the throw site, the observed literals,
    the highlighted trigger call lines, and the test bodies under the same
    1500-character cap.
  * `_variant_analysis_block` lists the root-cause reachable set, then tells
    the harness which part of it earlier harnesses already covered. The list
    is evidence and is kept under the same `MAX_REACHABLE_IN_PROMPT` cap. The
    coverage steering is coordination between harnesses and has no meaning
    for a one-shot decision, so it goes.

Dropped outright: `_hard_constraints`, `_intro`, `_metamorphic_block`,
`_fdp_reference`, `_skeleton_block`. Each states a rule about the .java file
the model must emit. None states a fact about the patch.
"""
from dataclasses import dataclass
from typing import List, Optional

import config
from java.harness.prompts import PromptBuilder
from java.parsing.java_source import highlight_trigger_calls

# Bump when a block's rendering changes. The context cache keys on it, so an
# old cache can never be silently mixed with a new rendering.
RENDERER_VERSION = 1

# Same cap the pipeline applies to a trigger-test body.
MAX_TEST_CHARS = 1500

# Sections of the crashing prompt this module deliberately does not carry.
DROPPED_SECTIONS = (
    '_hard_constraints', '_intro', '_metamorphic_block',
    '_fdp_reference', '_skeleton_block',
)

# Stateless, so one shared instance is enough. Calling the pipeline's own
# methods is the point: a copy of their text would drift the moment the
# harness prompt changes, and the parity claim would quietly become false.
_PB = PromptBuilder()


@dataclass
class Block:
    """One rendered evidence section."""
    name: str
    origin: str      # 'reused' (pipeline text verbatim) or 'rendered'
    text: str

    @property
    def chars(self) -> int:
        return len(self.text)


def render(context, failure_tests, crash_input) -> List[Block]:
    """The evidence blocks for one candidate patch, in prompt order."""
    blocks: List[Block] = [
        Block('patch', 'reused', _PB._patch_block(context.patch_text)),
    ]
    if context.source_imports:
        blocks.append(Block('source_imports', 'reused',
                            _PB._imports_block(context.source_imports)))
    for fn in context.functions:
        blocks.append(Block(f'touched_function:{fn.func_name}', 'reused',
                            _PB._function_block(fn)))
    trigger = _trigger_tests_block(context, failure_tests, crash_input)
    if trigger:
        blocks.append(Block('trigger_tests', 'rendered', trigger))
    reachable = _reachable_block(context.root_cause_reachable)
    if reachable:
        blocks.append(Block('root_cause_reachable', 'rendered', reachable))
    return blocks


def evidence_text(blocks: List[Block]) -> str:
    """The blocks joined exactly as the pipeline joins its sections."""
    return '\n\n'.join(b.text for b in blocks)


def manifest(blocks: List[Block]) -> dict:
    """The input parity manifest: what the baseline saw, and where from."""
    return {
        'renderer_version': RENDERER_VERSION,
        'blocks': [{'name': b.name, 'origin': b.origin, 'chars': b.chars}
                   for b in blocks],
        'total_chars': sum(b.chars for b in blocks),
        'dropped_pipeline_sections': list(DROPPED_SECTIONS),
    }


# --- re-rendered blocks ------------------------------------------------------

def _trigger_tests_block(context, failure_tests, crash_input) -> str:
    """The reported symptom: the failing test(s) and the observed crash.

    Mirrors `_failure_test_block` fact for fact, including its precedence
    rule that a runtime-observed throwable outranks the statically declared
    one, and its 1500-character body cap."""
    if not failure_tests:
        return ''
    signatures = [fn.func_signature for fn in context.functions]
    method_names = [fn.func_name for fn in context.functions]

    crash_types = sorted({ft.exception_type for ft in failure_tests
                          if ft.exception_type})
    if crash_input is not None and crash_input.exception_type:
        crash_types = [crash_input.exception_type] + [
            t for t in crash_types if t != crash_input.exception_type]

    parts: List[str] = [
        "The project's own test(s) below FAIL on the buggy code. They are"
        " the reported symptom of this bug — the failure the patch was"
        " written to remove.",
    ]
    if crash_types:
        parts.append("Throwable(s) observed, most reliable first: "
                     + ', '.join(crash_types))

    evidence = _crash_evidence(crash_input)
    if evidence:
        parts.append(evidence)

    for ft in failure_tests:
        if not ft.method_source:
            parts.append(f'<failing_test class="{ft.test_class}"'
                         f' method="{ft.test_method}" />')
            continue
        _hint, lines = highlight_trigger_calls(
            ft.method_source, crash_types, signatures,
            method_names=method_names)
        if lines:
            # The pipeline's `hint` is authorship guidance ("hard-code this
            # literal as the first call in your harness"). The LINES are the
            # fact, so they stay under a neutral heading.
            parts.extend([
                "Call(s) in the test most likely to reach the fault:",
                f'<key_calls class="{ft.test_class}"'
                f' method="{ft.test_method}">',
                lines,
                "</key_calls>",
            ])
        body = ft.method_source
        if len(body) > MAX_TEST_CHARS:
            body = body[:MAX_TEST_CHARS] + "\n        // ... (truncated)"
        parts.extend([
            f'<failing_test class="{ft.test_class}"'
            f' method="{ft.test_method}">',
            body,
            "</failing_test>",
        ])
    return '\n'.join(parts)


def _crash_evidence(crash_input) -> str:
    """The captured runtime failure, field for field.

    Same fields and same order as `_crash_input_block`, minus the clause that
    tells the model to hard-code the anchor and fuzz around it."""
    if crash_input is None or not crash_input.has_evidence:
        return ''
    parts = [
        "Runtime failure captured by running the trigger test on the buggy"
        " code. This is the verified failure; trust it over anything"
        " inferred from the test body.",
        "<observed_crash>",
    ]
    if crash_input.exception_type:
        parts.append(f"throwable: {crash_input.exception_type}")
    if crash_input.message:
        parts.append(f"message: {crash_input.message}")
    if crash_input.throw_site:
        parts.append(f"thrown_at: {crash_input.throw_site}")
    anchor = crash_input.best_anchor
    if anchor is not None:
        parts.append(f'crashing_input: "{anchor}"')
    if crash_input.literals and len(crash_input.literals) > 1:
        others = ', '.join(f'"{lit}"' for lit in crash_input.literals[1:])
        parts.append(f"other_observed_literals: {others}")
    parts.append("</observed_crash>")
    return '\n'.join(parts)


def _reachable_block(reachable: Optional[List[str]]) -> str:
    """The root-cause neighbourhood: code a sibling bug could hide in.

    Same source list and same cap as `_variant_analysis_block`, without its
    harness-coverage steering."""
    if not reachable:
        return ''
    cap = config.MAX_REACHABLE_IN_PROMPT
    shown = reachable[:cap]
    parts = [
        "The patched lines sit at the head of the region below. A sibling"
        " bug is one that lives in this region and stems from the SAME root"
        " cause as the reported failure.",
        "<root_cause_reachable>",
        *(f"- {name}" for name in shown),
        "</root_cause_reachable>",
    ]
    if len(reachable) > cap:
        parts.append(
            f"(+{len(reachable) - cap} more reachable functions omitted.)")
    return '\n'.join(parts)


#: The same block under a public name, for the semantic renderer. The pipeline
#: builds this section from `_variant_analysis_block` on both of its paths, so
#: one renderer here keeps the two baselines from drifting apart. This is an
#: alias, not a second rendering, so RENDERER_VERSION does not move.
reachable_block = _reachable_block
