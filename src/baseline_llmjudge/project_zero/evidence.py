"""Render the evidence one Project Zero fix carries.

THE PARITY TARGET IS THE C/C++ FRONT-END. The Defects4J baseline claims
block-level parity with the Java harness prompt, and rebuilds four of its five
factual sections. That claim does not transfer here. This dataset is C and C++,
so the counterpart is `LibFuzzerPromptBuilder.build` (oss_fuzz/prompts.py).

One block is reused verbatim, and three are the baseline's own:

  block            | pipeline section | origin here
  -----------------+------------------+---------------
  patch            | _patch_block     | reused verbatim
  touched_files    | (none)           | baseline_only
  touched_source   | _function_block  | baseline_only
  codebase         | (none)           | baseline_only

`patch` calls the pipeline's own method, so its wording and its 6000-character
cap cannot drift. `test_the_patch_block_is_the_pipelines_own_text` asserts it.

`touched_source` is marked `baseline_only`, not `rendered`, and the distinction
matters. The pipeline's `_function_block` carries ONE function, extracted by a
parser, and it states whether the fix touched that function or whether the
crash report named the frame. No C or C++ function extraction runs on this
path, so this block carries whole files instead. It is a different block that
happens to serve a similar purpose. To call it `rendered` would claim a parity
that does not exist.

THE EVIDENCE GAP IS WIDER HERE THAN ON THE DEFECTS4J SIDE, AND EVERY RECORD
SAYS SO. The Defects4J baseline is missing execution evidence alone. This one
is missing the root-cause region as well, because no tree is checked out and no
call graph is built. It is also missing the commit message, which
`firewall` drops on purpose. `WITHHELD_PIPELINE_EVIDENCE` lists all of it,
and the README repeats the list. Do not compare an F1 from this population
with an F1 from the Defects4J population as though the two judges saw
comparable evidence.
"""
from typing import Dict, List

from oss_fuzz.prompts import LibFuzzerPromptBuilder

from baseline_llmjudge.project_zero.firewall import Fix
from baseline_llmjudge.shared.blocks import Block, evidence_text

# Bump when a block's rendering changes. Separate from the Java renderers'
# numbers on purpose: a change here must not invalidate their cached entries.
RENDERER_VERSION = 1

#: Sections of the C/C++ harness prompt this module deliberately does not
#: carry. Each one states a rule about the .c or .cc file the model must emit.
DROPPED_SECTIONS = (
    '_intro', '_byte_carving_reference', '_skeleton',
    '_distinct_finding_block', '_required_oracle_block',
    '_project_invariant_block', '_metamorphic_block',
)

#: Evidence the pipeline gets and this baseline cannot get. Every record
#: carries this list, so the gap is read from the artifact and not from prose.
WITHHELD_PIPELINE_EVIDENCE = (
    # Needs the original crash report and a reproducer input. This dataset
    # stores neither.
    '_original_crash_block',
    # Needs a checked-out tree and a call graph. Only the touched files are
    # fetched here, so no root-cause region is computed.
    '_routes_block',
    # Needs the project's own OSS-Fuzz harness.
    '_reference_harness_block',
    '_known_includes_block',
    # Needs a build of the patched code.
    'compile_result',
    'fuzz_result',
    # Dropped by the firewall, not missing from the dataset. It is the richest
    # leak channel in the data: real examples name the later CVE outright, and
    # only 49 of the 86 patch files carry a message at all, so its presence
    # alone would separate the two classes.
    'commit_message',
)

# Stateless, so one shared instance is enough. Calling the pipeline's own
# method is the point: a copy of its text would drift the moment the harness
# prompt changed.
_PB = LibFuzzerPromptBuilder(language='c++')


def render(fix: Fix) -> List[Block]:
    """The evidence blocks for one fix, in prompt order."""
    blocks: List[Block] = [
        Block('patch', 'reused', _PB._patch_block(fix.diff)),
        Block('touched_files', 'baseline_only', _touched_files_block(fix)),
    ]
    source = _touched_source_block(fix)
    if source:
        blocks.append(Block('touched_source', 'baseline_only', source))
    blocks.append(Block('codebase', 'baseline_only', _codebase_block(fix)))
    return blocks


def manifest(fix: Fix, blocks: List[Block]) -> Dict:
    """The input parity manifest: what the baseline saw, and where from."""
    return {
        'renderer_version': RENDERER_VERSION,
        'parity_target': 'oss_fuzz.prompts.LibFuzzerPromptBuilder.build',
        'blocks': [{'name': b.name, 'origin': b.origin, 'chars': b.chars}
                   for b in blocks],
        'total_chars': sum(b.chars for b in blocks),
        'dropped_pipeline_sections': list(DROPPED_SECTIONS),
        'withheld_pipeline_evidence': list(WITHHELD_PIPELINE_EVIDENCE),
        'scrub_report': fix.scrub_report,
    }


# --- the blocks --------------------------------------------------------------

def _touched_files_block(fix: Fix) -> str:
    """The files the fix changes, read from the diff's own headers."""
    return '\n'.join([
        "Files this fix changes:",
        "<touched_files>",
        *(f'- {path}' for path in fix.touched_files),
        "</touched_files>",
    ])


def _touched_source_block(fix: Fix) -> str:
    """The source of each touched file, at this fix's own commit.

    The state BEFORE the fix for `fix0`, and before the later fix for `fix1`.
    Either way it is the code the diff above applies to."""
    if not fix.sources:
        return ''
    parts = [
        "The source of those files, as it stood when this fix was written."
        " The diff above applies to this code.",
    ]
    for path, text in fix.sources:
        parts.extend([f'<source path="{path}">', text, '</source>'])
    return '\n'.join(parts)


def _codebase_block(fix: Fix) -> str:
    """Which project this is. It sets what a fault in this code can mean."""
    return (f'Project: {fix.software}\n'
            f'Codebase key: {fix.codebase}')


__all__ = ['RENDERED_BLOCK_NAMES', 'RENDERER_VERSION', 'DROPPED_SECTIONS',
           'WITHHELD_PIPELINE_EVIDENCE', 'render', 'manifest',
           'evidence_text']

#: The block names this renderer can emit, in order. A guard test asserts that
#: a positive fix and a negative fix draw from this one list, so the two
#: classes cannot differ by which blocks they carry.
RENDERED_BLOCK_NAMES = ('patch', 'touched_files', 'touched_source',
                        'codebase')
