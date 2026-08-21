"""Extract the evidence for one candidate patch, then cache it on disk.

The extraction repeats the pipeline's own steps, in the pipeline's own order
(java/run.py, steps 3 to 4c). The first four steps are the same for both bug
kinds; the rest differ, because the two kinds report themselves differently.

  1. select the patch and check out the buggy Defects4J version
  2. extract the bug-triggering test(s)
  3. classify the bug, and refuse a bug of the other kind
  4. run the safety-net gate: every trigger test must fail on the buggy
     checkout (cached in the checkout by a marker file, so it is usually free)
  5. parse the patch into its touched functions and their reachable region

A crashing bug then gets one more step:

  6c. capture the runtime crash by running the trigger test on the buggy code

A semantic bug gets four more instead. It throws nothing, so there is no
crashing value to read back, and the evidence that replaces it is the reported
wrong value plus the local contract of the touched code:

  6s. read the failure message the safety-net run already recorded, which
      names the diverging observable and the wrong value
  7s. resolve what each trigger test uses from its own test class
  8s. assemble the class skeletons of the touched and collaborating classes
  9s. read the javadoc of the touched methods, and the class's overload
      groups, method families and no-argument readers

Steps 1, 4 and 6c need the checkout, so the baseline is not free of execution.
What it never does is build the patched code, compile a harness, or fuzz. The
README's budget section states that split.

The result is cached as rendered text, not as raw objects. Two reasons:

  * Every prompt version and every sample then reads one byte-identical
    evidence string, so a score difference between versions can only come from
    the prompt wording.
  * The costly half (checkout, the Defects4J test runs, fuzz-introspector) runs
    once per patch instead of once per model call.

The cache entry records the bug kind and the renderer version that produced it.
A request for the other kind, or for a newer renderer, misses the cache rather
than reading a stale rendering.
"""
import json
import os
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional

from java.bug_context.analysis import TargetAnalyzer
from java.bug_context.code_context import assemble_class_context
from java.bug_context.crash_input import CrashInputExtractor
from java.bug_context.failure_test import (FailureTestExtractor,
                                           is_crashing_bug,
                                           resolve_test_support)
from java.bug_context.patches import PatchSelector
from java.execution.fuzz_runner import (PatchedProjectBuilder,
                                        TriggerVerificationError)
from java.parsing.java_source import (candidate_anchor_literals,
                                      sibling_and_state_hints)
from java.relations.relation_synth import javadoc_for

from baseline_llmjudge.defects4j import evidence, evidence_semantic

# Bump when the EXTRACTION changes, not only when the rendering does.
# A cached entry is rendered text, so an extraction fix reaches the
# baseline only when the cache that predates it is invalidated.
#   2: class-aware and type-aware resolution of a touched function, so the
#      neighbourhood belongs to the patched method (see
#      java/bug_context/analysis.py, _match_fi_name).
#   3: the semantic path landed. A crashing entry written at version 2 is
#      still current, so the crashing cache is keyed separately below.
CACHE_VERSION = 3

# The crashing extraction did not change when the semantic path landed, so its
# cached entries stay valid. Keying each kind to its own version is what makes
# that possible: one shared number would have invalidated 82 crashing entries
# and orphaned every published crashing evidence_sha256.
CACHE_VERSION_BY_KIND = {'crashing': 2, 'semantic': 3}

#: The renderer each kind reads its blocks from.
RENDERER_BY_KIND = {'crashing': evidence, 'semantic': evidence_semantic}


class ContextUnavailable(Exception):
    """This patch cannot be given the pipeline's evidence.

    `status` uses the pipeline's own vocabulary ('non_crashing',
    'semantic_skip', 'crashing_skip', 'bug_not_reproduced', 'error') so a
    record from either side of the comparison can be filtered the same way."""

    def __init__(self, status: str, detail: str):
        super().__init__(detail)
        self.status = status
        self.detail = detail


@dataclass
class PatchEvidence:
    """The cached evidence for one candidate patch."""
    project: str
    bug_id: str
    apr_tool: str
    patch: str
    bug_kind: str
    context_degraded: bool
    text: str
    manifest: dict
    facts: dict          # small audit summary, never sent to the model

    def as_dict(self) -> dict:
        return {
            'cache_version': CACHE_VERSION_BY_KIND[self.bug_kind],
            'renderer_version': RENDERER_BY_KIND[
                self.bug_kind].RENDERER_VERSION,
            'project': self.project,
            'bug_id': self.bug_id,
            'apr_tool': self.apr_tool,
            'patch': self.patch,
            'bug_kind': self.bug_kind,
            'context_degraded': self.context_degraded,
            'text': self.text,
            'manifest': self.manifest,
            'facts': self.facts,
        }

    @classmethod
    def from_dict(cls, d: dict) -> 'PatchEvidence':
        return cls(project=d['project'], bug_id=d['bug_id'],
                   apr_tool=d['apr_tool'], patch=d['patch'],
                   bug_kind=d['bug_kind'],
                   context_degraded=d['context_degraded'],
                   text=d['text'], manifest=d['manifest'], facts=d['facts'])


def cache_path(cache_dir, patch_path: str) -> Path:
    """One cache file per candidate patch, named after the patch file."""
    return Path(cache_dir) / (Path(patch_path).stem + '.json')


def load_or_build(patch_path: str, label: str,
                  cache_dir: Optional[str] = None,
                  refresh: bool = False,
                  kind: str = 'crashing') -> PatchEvidence:
    """The evidence for `patch_path`, from cache when it is current.

    `label` ('correct' or 'overfitting') only tells the selector which drr
    directory the patch lives under. It never reaches the rendered text; see
    tests/test_llmjudge_baseline.py for the guard that enforces this.

    `kind` selects the pool. A patch whose bug belongs to the other kind
    raises `ContextUnavailable`, because the two kinds are two populations
    with two frozen splits and two families of prompt version."""
    if kind not in RENDERER_BY_KIND:
        raise ValueError(f'unknown bug kind {kind!r}; expected one of '
                         f'{sorted(RENDERER_BY_KIND)}')
    path = cache_path(cache_dir, patch_path) if cache_dir else None
    if path is not None and path.exists() and not refresh:
        cached = json.loads(path.read_text())
        # The kind is checked first. An entry of the other kind is a different
        # rendering of a different question, so reading it would silently mix
        # the two pools inside one run.
        if (cached.get('bug_kind') == kind
                and cached.get('cache_version') == CACHE_VERSION_BY_KIND[kind]
                and cached.get('renderer_version')
                == RENDERER_BY_KIND[kind].RENDERER_VERSION):
            return PatchEvidence.from_dict(cached)

    built = _build(patch_path, label, kind)
    if path is not None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(built.as_dict(), indent=1))
    return built


def _build(patch_path: str, label: str, kind: str) -> PatchEvidence:
    """Select the patch, gate it, then hand off to the path for its kind."""
    selector = PatchSelector(correct=(label == 'correct'),
                             overfitting=(label == 'overfitting'),
                             patch_file=patch_path)
    selection = selector.select()

    failure_tests = FailureTestExtractor().extract(
        selection.buggy_dir,
        project_name=selection.project_name,
        bug_id=selection.bug_id,
    )
    if not failure_tests:
        raise ContextUnavailable(
            'non_crashing',
            'no bug-triggering test, so the bug has no reported symptom')
    crashing = is_crashing_bug(failure_tests)
    if kind == 'crashing' and not crashing:
        raise ContextUnavailable(
            'semantic_skip',
            'semantic bug (trigger test asserts rather than throws)')
    if kind == 'semantic' and crashing:
        raise ContextUnavailable(
            'crashing_skip',
            'crashing bug (trigger test throws rather than asserts)')

    # Same gate the pipeline applies before it spends a token. Cached in the
    # checkout, so on a checkout the pipeline already used it costs nothing.
    try:
        PatchedProjectBuilder().verify_bug_reproduces(selection.buggy_dir)
    except TriggerVerificationError as exc:
        raise ContextUnavailable(exc.status, str(exc))

    context = TargetAnalyzer().analyze(patch_path=selection.patch_path,
                                       buggy_dir=selection.buggy_dir)

    if kind == 'crashing':
        blocks, extra_facts = _crashing_blocks(selection, context,
                                               failure_tests)
    else:
        blocks, extra_facts = _semantic_blocks(selection, context,
                                               failure_tests)

    renderer = RENDERER_BY_KIND[kind]
    facts = {
        'modified_files': context.modified_files,
        'package': context.package,
        'touched_functions': [fn.func_name for fn in context.functions],
        'reachable_count': len(context.root_cause_reachable or []),
        # Empty notes plus an empty neighbourhood means a real leaf.
        # Notes plus an empty neighbourhood means the lookup failed.
        'neighbourhood_notes': list(context.neighbourhood_notes or []),
        'trigger_tests': [f'{ft.test_class}::{ft.test_method}'
                          for ft in failure_tests],
    }
    facts.update(extra_facts)
    return PatchEvidence(
        project=selection.project_name,
        bug_id=selection.bug_id,
        apr_tool=selection.apr_tool,
        patch=os.path.basename(selection.patch_path),
        bug_kind=kind,
        # An empty touched-function set means the prompt carries no function
        # bodies. The pipeline stamps its record and carries on; so do we, so
        # the aggregator can tell a degraded input from a normal one.
        context_degraded=not context.functions,
        text=renderer.evidence_text(blocks),
        manifest=renderer.manifest(blocks),
        facts=facts,
    )


# --- the crashing path -------------------------------------------------------

def _crashing_blocks(selection, context, failure_tests):
    """Step 6c: the runtime crash, captured on the buggy build."""
    crash_input = None
    primary = next((ft for ft in failure_tests if ft.has_source),
                   failure_tests[0])
    if primary is not None:
        literals = []
        if primary.method_source:
            literals = candidate_anchor_literals(
                primary.method_source,
                [fn.func_name for fn in context.functions])
        crash_input = CrashInputExtractor().extract(
            buggy_dir=selection.buggy_dir,
            test_class=primary.test_class,
            test_method=primary.test_method,
            candidate_literals=literals,
        )
    blocks = evidence.render(context, failure_tests, crash_input)
    return blocks, {
        'crash_evidence_captured': bool(
            crash_input is not None and crash_input.has_evidence),
    }


# --- the semantic path -------------------------------------------------------

def _semantic_blocks(selection, context, failure_tests):
    """Steps 6s to 9s: the reported wrong value, and the local contract.

    Each of the four steps is best-effort, exactly as it is in the pipeline. A
    step that yields nothing leaves its block out, and the fact summary records
    the miss so a thin prompt is visible in the artifact."""
    # 6s) The failure message the safety net already recorded. It names the
    #     diverging observable and the wrong value, so it is the semantic
    #     counterpart of the crashing path's observed throwable.
    messages = PatchedProjectBuilder.trigger_failure_messages(
        selection.buggy_dir)
    for ft in failure_tests:
        ft.failure_message = messages.get(
            f'{ft.test_class}::{ft.test_method}')

    # 7s) What each trigger test uses from its own test class.
    for ft in failure_tests:
        try:
            ft.support_source = resolve_test_support(
                ft, checkout_dir=selection.buggy_dir)
        except Exception:            # context is best-effort, never fatal
            ft.support_source = None

    chosen = next((ft for ft in failure_tests if ft.has_source),
                  failure_tests[0])

    # 8s) The class skeletons. The pipeline skips them when no touched
    #     function was resolved, because the assembler keys on one.
    class_context: List[str] = []
    if context.functions:
        try:
            class_context = assemble_class_context(
                selection.buggy_dir,
                context.modified_files or [],
                [fn.func_name for fn in context.functions],
                test_sources=[ft.method_source or ''
                              for ft in failure_tests])
        except Exception:
            class_context = []

    # 9s) The documented contract of the touched methods, and the class's
    #     overload groups, method families and no-argument readers.
    javadocs: List[str] = []
    for rel in (context.modified_files or []):
        try:
            src_text = (Path(selection.buggy_dir) / rel).read_text(
                encoding='utf-8', errors='replace')
        except OSError:
            continue
        for fn in context.functions:
            doc = javadoc_for(src_text, fn.func_name)
            if doc and doc not in javadocs:
                javadocs.append(doc)

    sibling_hints = ''
    for rel in (context.modified_files or [])[:1]:
        full = os.path.join(selection.buggy_dir, rel.lstrip('/'))
        if not os.path.isfile(full):
            continue
        try:
            with open(full, encoding='utf-8', errors='replace') as fh:
                sibling_hints = sibling_and_state_hints(fh.read())
        except OSError:
            sibling_hints = ''

    blocks = evidence_semantic.render(
        context, failure_tests, chosen,
        class_context=class_context,
        javadocs=javadocs,
        sibling_hints=sibling_hints)
    return blocks, {
        'lifted_from': f'{chosen.test_class}::{chosen.test_method}',
        'failure_message_captured': bool(chosen.failure_message),
        'test_support_captured': bool(chosen.support_source),
        'class_skeleton_count': len(class_context),
        'javadoc_count': len(javadocs),
        'sibling_hint_chars': len(sibling_hints or ''),
    }
