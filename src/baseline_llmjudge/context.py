"""Extract the evidence for one candidate patch, then cache it on disk.

The extraction repeats the pipeline's own steps, in the pipeline's own order
(java/run.py, steps 3 to 4c):

  1. select the patch and check out the buggy Defects4J version
  2. extract the bug-triggering test(s)
  3. classify the bug; only a crashing bug is in scope here
  4. run the safety-net gate: every trigger test must fail on the buggy
     checkout (cached in the checkout by a marker file, so it is usually free)
  5. parse the patch into its touched functions and their reachable region
  6. capture the runtime crash by running the trigger test on the buggy code

Steps 1, 4 and 6 need the checkout, so the baseline is not free of execution.
What it never does is build the patched code, compile a harness, or fuzz. The
README's budget section states that split.

The result is cached as rendered text, not as raw objects. Two reasons:

  * All four prompt versions and all samples then read one byte-identical
    evidence string, so a score difference between versions can only come from
    the prompt wording.
  * The costly half (checkout, two Defects4J test runs, fuzz-introspector) runs
    once per patch instead of once per model call.
"""
import json
import os
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional

from java.bug_context.analysis import TargetAnalyzer
from java.bug_context.crash_input import CrashInputExtractor
from java.bug_context.failure_test import FailureTestExtractor, is_crashing_bug
from java.bug_context.patches import PatchSelector
from java.execution.fuzz_runner import (PatchedProjectBuilder,
                                        TriggerVerificationError)
from java.parsing.java_source import candidate_anchor_literals

from baseline_llmjudge import evidence

CACHE_VERSION = 1


class ContextUnavailable(Exception):
    """This patch cannot be given the pipeline's evidence.

    `status` uses the pipeline's own vocabulary ('non_crashing',
    'semantic_skip', 'bug_not_reproduced', 'error') so a record from either
    side of the comparison can be filtered the same way."""

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
            'cache_version': CACHE_VERSION,
            'renderer_version': evidence.RENDERER_VERSION,
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
                  refresh: bool = False) -> PatchEvidence:
    """The evidence for `patch_path`, from cache when it is current.

    `label` ('correct' or 'overfitting') only tells the selector which drr
    directory the patch lives under. It never reaches the rendered text; see
    tests/test_llmjudge_baseline.py for the guard that enforces this."""
    path = cache_path(cache_dir, patch_path) if cache_dir else None
    if path is not None and path.exists() and not refresh:
        cached = json.loads(path.read_text())
        if (cached.get('cache_version') == CACHE_VERSION
                and cached.get('renderer_version')
                == evidence.RENDERER_VERSION):
            return PatchEvidence.from_dict(cached)

    built = _build(patch_path, label)
    if path is not None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(built.as_dict(), indent=1))
    return built


def _build(patch_path: str, label: str) -> PatchEvidence:
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
    if not is_crashing_bug(failure_tests):
        raise ContextUnavailable(
            'semantic_skip',
            'semantic bug (trigger test asserts rather than throws)')

    # Same gate the pipeline applies before it spends a token. Cached in the
    # checkout, so on a checkout the pipeline already used it costs nothing.
    try:
        PatchedProjectBuilder().verify_bug_reproduces(selection.buggy_dir)
    except TriggerVerificationError as exc:
        raise ContextUnavailable(exc.status, str(exc))

    context = TargetAnalyzer().analyze(patch_path=selection.patch_path,
                                       buggy_dir=selection.buggy_dir)

    # An empty touched-function set means the prompt carries no function
    # bodies. The pipeline stamps its record and carries on; so do we, so the
    # aggregator can tell a degraded input from a normal one.
    context_degraded = not context.functions

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
    return PatchEvidence(
        project=selection.project_name,
        bug_id=selection.bug_id,
        apr_tool=selection.apr_tool,
        patch=os.path.basename(selection.patch_path),
        bug_kind='crashing',
        context_degraded=context_degraded,
        text=evidence.evidence_text(blocks),
        manifest=evidence.manifest(blocks),
        facts={
            'modified_files': context.modified_files,
            'package': context.package,
            'touched_functions': [fn.func_name for fn in context.functions],
            'reachable_count': len(context.root_cause_reachable or []),
            'trigger_tests': [f'{ft.test_class}::{ft.test_method}'
                              for ft in failure_tests],
            'crash_evidence_captured': bool(
                crash_input is not None and crash_input.has_evidence),
        },
    )
