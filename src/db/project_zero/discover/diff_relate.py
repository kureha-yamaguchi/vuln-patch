"""Deep diff-relatability check via gpt-5-mini.

Sits one layer below the two rough verifiers (prose-LLM + URL/file-overlap):
takes each candidate seed that survived the rough phase and, when patches
for both sides can be fetched, sends the actual unified diffs to the LLM
to judge whether the two patches are causally related (the later being
necessitated by the prior being incomplete).

Patch diffs can be tens of kilobytes; we truncate per-side to a budget so
the prompt stays under ~8k tokens. The truncation keeps file headers and
the first N lines of each hunk so the LLM sees what files changed and
roughly which lines.

Output schema:
  {
    diff_related: bool,
    diff_kind: 'incomplete_fix_confirmed' | 'same_root_cause_confirmed'
             | 'one_extends_other' | 'unrelated' | 'insufficient_data',
    confidence: 0-1,
    shared_files: [str, ...],
    cited_change: str,             # short quote from one or both diffs
    reasoning: str,                # 1-3 sentences
  }
"""
from __future__ import annotations

import json
import os
import re
from dataclasses import asdict, dataclass, field
from typing import Any, Dict, List, Optional

from . import config
from .classifier import BudgetExceededError, Classifier
from .code_overlap import (
    CodeOverlap, CodeOverlapChecker, Patch, _patches_collide,
)


# --------------------------------------------------------------------------
# Truncation

# Per-side soft cap on characters of patch text to send to the LLM. With a
# rough 4-char-per-token estimate, 12000 chars = ~3000 tokens per patch,
# leaving headroom for the system prompt + structured output.
PER_SIDE_CHAR_BUDGET = 12_000


def truncate_patch(patch_text: str, budget: int = PER_SIDE_CHAR_BUDGET) -> str:
    """Keep all file headers (`diff --git`, `+++`, `---`, `@@`) and as many
    hunk-body lines as fit. When a hunk is too long, drop its middle and
    insert a `... [N lines elided] ...` marker."""
    if len(patch_text) <= budget:
        return patch_text

    # Stage 1: cap each hunk to the first 80 lines + a marker.
    out_lines: List[str] = []
    current_hunk: List[str] = []

    def flush_current_hunk():
        nonlocal current_hunk
        if not current_hunk:
            return
        keep = 80
        if len(current_hunk) > keep + 5:
            out_lines.extend(current_hunk[:keep])
            out_lines.append(
                f'... [{len(current_hunk) - keep} hunk lines elided] ...'
            )
        else:
            out_lines.extend(current_hunk)
        current_hunk = []

    for line in patch_text.splitlines():
        if line.startswith(('diff --git ', '--- ', '+++ ', 'index ',
                            'new file mode', 'deleted file mode',
                            'rename ', 'similarity index ')):
            flush_current_hunk()
            out_lines.append(line)
        elif line.startswith('@@'):
            flush_current_hunk()
            out_lines.append(line)
        else:
            current_hunk.append(line)
    flush_current_hunk()

    truncated = '\n'.join(out_lines)

    # Stage 2: if we're still over budget, hard-truncate by file.
    if len(truncated) > budget:
        truncated = (truncated[:budget]
                     + '\n... [patch text truncated to fit token budget] ...')
    return truncated


# --------------------------------------------------------------------------
# Dataclasses

@dataclass
class DiffRelateDecision:
    later_cve: str
    prior_cve: str
    # availability
    later_patch_available: bool
    prior_patch_available: bool
    later_patch_url: Optional[str]
    prior_patch_url: Optional[str]
    later_files: List[str] = field(default_factory=list)
    prior_files: List[str] = field(default_factory=list)
    # LLM verdict
    diff_related: bool = False
    diff_kind: str = 'insufficient_data'
    confidence: float = 0.0
    shared_files: List[str] = field(default_factory=list)
    cited_change: str = ''
    reasoning: str = ''
    # plumbing
    skip_reason: str = ''
    llm_cached: bool = False
    llm_cost_usd: float = 0.0

    def as_dict(self) -> dict:
        return asdict(self)


# --------------------------------------------------------------------------
# LLM prompt + schema

DIFF_SYSTEM_PROMPT = (
    "You are a security-patch analyst. You will be shown the unified-diff "
    "fix commits for two bugs that an upstream document (a Project Zero "
    "RCA, the P0 0-day tracker, or a Project Zero blog post) flagged as "
    "potentially related. Decide whether the two patches are CODE-LEVEL "
    "related at the file/function level, and whether the later patch's "
    "existence is best explained by the prior patch being incomplete.\n\n"
    "Categories:\n"
    "  - incomplete_fix_confirmed: the later patch COMPLETES the prior "
    "patch's OWN fix. Either (a) it re-touches the same function/file the "
    "prior changed, patches a call site/branch the prior missed, or "
    "bypasses a check the prior added; OR (b) it applies the SAME corrective "
    "change the prior made (same logic/guard/refactor) to a sibling site the "
    "prior should have covered but didn't — even in a different file. The "
    "test is continuity of the FIX: the later finishes the exact job the "
    "prior started. If instead the later patch is a DIFFERENT fix for a "
    "DIFFERENT bug — even one in the same subsystem and same bug class — and "
    "it neither re-touches the prior's code nor replicates the prior's "
    "specific change, it is NOT an incomplete fix; use "
    "same_root_cause_confirmed.\n"
    "  - same_root_cause_confirmed: the two patches address the same "
    "underlying flaw or bug class but in SEPARATE, independent locations, "
    "each with its OWN distinct fix — e.g. variant bugs found by auditing "
    "the same subsystem, the same use-after-free / type-confusion pattern "
    "surfacing in a different handler via different code. Each patch fully "
    "fixes its own bug; the later is not finishing or replicating the "
    "prior's fix. This is the right label for sibling/variant bugs in the "
    "same component that share no file and no common corrective change.\n"
    "  - one_extends_other: one patch is a strict extension or refinement "
    "of the other (e.g. adding test coverage, hardening); useful but not "
    "causally a fix-of-a-fix.\n"
    "  - unrelated: the two patches touch different code with no shared "
    "function/file/data structure of meaningful overlap.\n"
    "  - insufficient_data: the patches are too small, too obfuscated, "
    "or otherwise don't support a confident judgment.\n\n"
    "Be conservative. Quote one short sentence/line from the diff(s) "
    "supporting the verdict in `cited_change`. Return JSON only."
)

DIFF_SCHEMA: Dict[str, Any] = {
    'type': 'object',
    'additionalProperties': False,
    'required': [
        'diff_related', 'diff_kind', 'confidence',
        'shared_files', 'cited_change', 'reasoning',
    ],
    'properties': {
        'diff_related': {'type': 'boolean'},
        'diff_kind': {
            'type': 'string',
            'enum': ['incomplete_fix_confirmed',
                     'same_root_cause_confirmed',
                     'one_extends_other',
                     'unrelated',
                     'insufficient_data'],
        },
        'confidence': {'type': 'number', 'minimum': 0, 'maximum': 1},
        'shared_files': {
            'type': 'array',
            'items': {'type': 'string'},
            'description': 'File paths touched by BOTH patches.',
        },
        'cited_change': {
            'type': 'string',
            'description': 'Short literal quote from one of the diffs '
                           'supporting the verdict, or empty if none.',
        },
        'reasoning': {'type': 'string'},
    },
}


def _build_user_prompt(later_cve: str, prior_cve: str,
                       later: Patch, prior: Patch,
                       later_text: str, prior_text: str,
                       seed_context: Optional[str] = None) -> str:
    parts: List[str] = []
    if seed_context:
        parts.append('CONTEXT FROM UPSTREAM DOCUMENT:')
        parts.append(seed_context)
        parts.append('')

    parts.append(f'LATER CVE: {later_cve}')
    parts.append(f'  patch commit: {later.commit_url}')
    parts.append(f'  files touched: {", ".join(later.files[:20])}'
                 + ('' if len(later.files) <= 20 else ' ...'))
    parts.append('')
    parts.append(f'PRIOR CVE / BUG: {prior_cve}')
    parts.append(f'  patch commit: {prior.commit_url}')
    parts.append(f'  files touched: {", ".join(prior.files[:20])}'
                 + ('' if len(prior.files) <= 20 else ' ...'))
    parts.append('')
    parts.append('--- LATER PATCH (unified diff, possibly truncated) ---')
    parts.append(later_text)
    parts.append('')
    parts.append('--- PRIOR PATCH (unified diff, possibly truncated) ---')
    parts.append(prior_text)
    parts.append('')
    parts.append(
        'Judge the code-level relationship per the system prompt rules. '
        'Quote a short literal change in `cited_change`. JSON only.'
    )
    return '\n'.join(parts)


# --------------------------------------------------------------------------
# Orchestrator

@dataclass
class DiffRelateRun:
    seeds_in: int
    confirmed_in: int
    analyzed: int                # had patches for both sides
    related_count: int
    decisions: List[DiffRelateDecision]
    spend_usd: float
    calls_live: int
    calls_cached: int


class DiffRelateAnalyzer:
    """Iterates the confirmed seeds (or any input seed list), fetches both
    sides' patches via :class:`CodeOverlapChecker`, sends the diffs to the
    LLM, and emits :class:`DiffRelateDecision`."""

    def __init__(self,
                 classifier: Classifier,
                 overlap_checker: CodeOverlapChecker,
                 only_confirmed: bool = True):
        self.classifier = classifier
        self.overlap_checker = overlap_checker
        self.only_confirmed = only_confirmed

    def run(self, seeds: List[Dict[str, Any]]) -> DiffRelateRun:
        decisions: List[DiffRelateDecision] = []
        total = len(seeds)
        confirmed = [s for s in seeds if s.get('confirmed')]
        target = confirmed if self.only_confirmed else seeds
        analyzed = 0
        related = 0
        budget_done = False
        for idx, s in enumerate(target, 1):
            later = s['later_cve']
            prior = s['prior_cve']
            ov: CodeOverlap = self.overlap_checker.check_pair(later, prior)
            later_patch = ov.later_patches[0] if ov.later_patches else None
            prior_patch = ov.prior_patches[0] if ov.prior_patches else None

            dec = DiffRelateDecision(
                later_cve=later, prior_cve=prior,
                later_patch_available=bool(later_patch),
                prior_patch_available=bool(prior_patch),
                later_patch_url=(later_patch.commit_url
                                 if later_patch else None),
                prior_patch_url=(prior_patch.commit_url
                                 if prior_patch else None),
                later_files=list(later_patch.files) if later_patch else [],
                prior_files=list(prior_patch.files) if prior_patch else [],
            )

            if not (later_patch and prior_patch):
                dec.skip_reason = (
                    'no patches for both sides ('
                    + ('later=ok ' if later_patch else 'later=missing ')
                    + ('prior=ok' if prior_patch else 'prior=missing')
                    + ')'
                )
                decisions.append(dec)
                self._print_progress(idx, len(target), dec, related)
                continue

            # Reject self-pairs: when the resolver picked up the same
            # upstream fix for both sides — happens for github (a
            # github:owner/repo@sha "prior" extracted from the later's
            # own RCA), Gerrit (two CVEs link to the same chromium-review
            # change), Bugzilla (one CVE === one bug), and any cross-host
            # case where commit URLs canonicalise to the same string.
            # See :func:`code_overlap._patches_collide` for the full rule.
            if _patches_collide([later_patch], [prior_patch]):
                dec.skip_reason = 'self-pair (later and prior resolved to same fix)'
                decisions.append(dec)
                self._print_progress(idx, len(target), dec, related)
                continue

            # Read raw patches from cache.
            later_text = self._read_patch(later_patch)
            prior_text = self._read_patch(prior_patch)
            if not later_text or not prior_text:
                dec.skip_reason = 'patch cache read failed'
                decisions.append(dec)
                self._print_progress(idx, len(target), dec, related)
                continue

            later_text_t = truncate_patch(later_text)
            prior_text_t = truncate_patch(prior_text)
            seed_context = _seed_context_for_prompt(s)

            if budget_done:
                dec.skip_reason = 'LLM budget exhausted'
                decisions.append(dec)
                self._print_progress(idx, len(target), dec, related)
                continue

            try:
                user_prompt = _build_user_prompt(
                    later, prior, later_patch, prior_patch,
                    later_text_t, prior_text_t,
                    seed_context=seed_context,
                )
                decision = self.classifier.classify(
                    system_prompt=DIFF_SYSTEM_PROMPT,
                    user_prompt=user_prompt,
                    schema=DIFF_SCHEMA,
                    schema_name='diff_relate',
                )
                parsed = decision.parsed
                dec.diff_related = bool(parsed.get('diff_related'))
                dec.diff_kind = str(parsed.get('diff_kind', 'insufficient_data'))
                dec.confidence = float(parsed.get('confidence', 0.0))
                dec.shared_files = list(parsed.get('shared_files') or [])
                dec.cited_change = str(parsed.get('cited_change', ''))
                dec.reasoning = str(parsed.get('reasoning', ''))
                dec.llm_cached = decision.cached
                dec.llm_cost_usd = decision.cost_usd
                analyzed += 1
                if dec.diff_related:
                    related += 1
            except BudgetExceededError:
                print(f"  ! LLM budget exhausted after "
                      f"{self.classifier.calls['live']} live calls",
                      flush=True)
                budget_done = True
                dec.skip_reason = 'LLM budget exhausted mid-flight'

            decisions.append(dec)
            self._print_progress(idx, len(target), dec, related)

        return DiffRelateRun(
            seeds_in=total,
            confirmed_in=len(confirmed),
            analyzed=analyzed,
            related_count=related,
            decisions=decisions,
            spend_usd=self.classifier.spend_usd,
            calls_live=self.classifier.calls['live'],
            calls_cached=self.classifier.calls['cached'],
        )

    # ------------------------------------------------------------------

    @staticmethod
    def _read_patch(patch: Patch) -> str:
        if not patch.local_path or not os.path.isfile(patch.local_path):
            return ''
        try:
            with open(patch.local_path, 'r',
                      encoding='utf-8', errors='replace') as f:
                return f.read()
        except OSError:
            return ''

    @staticmethod
    def _print_progress(idx: int, total: int,
                        dec: DiffRelateDecision, related: int) -> None:
        # `skip_reason` may be set even when both patches are available
        # (self-pair, budget-exhausted) — prefer it over the default
        # `diff_kind` so the line accurately reflects what happened.
        if dec.skip_reason:
            mark = '·'
            status = dec.skip_reason
        elif not (dec.later_patch_available and dec.prior_patch_available):
            mark = '·'
            status = 'skipped'
        else:
            mark = '✓' if dec.diff_related else '×'
            status = (f"kind={dec.diff_kind:30s} "
                      f"conf={dec.confidence:.2f} "
                      f"shared={len(dec.shared_files)}f")
        print(
            f"  [{idx:3d}/{total}] {mark} "
            f"{dec.later_cve} -> {dec.prior_cve}  {status}  "
            f"related={related}",
            flush=True,
        )


def _seed_context_for_prompt(seed: Dict[str, Any]) -> Optional[str]:
    """Compact the prose evidence from the seed into a small block to give
    the LLM the upstream-document context alongside the patch diffs."""
    if seed.get('llm_cited_sentence'):
        cite = seed['llm_cited_sentence']
        url = seed.get('llm_best_evidence_url') or ''
        return f'"{cite}"\n  -- {url}'
    # Fallback: first evidence quote.
    ev = seed.get('evidence') or []
    if ev:
        first = ev[0]
        return f'"{first.get("quote", "")}"\n  -- {first.get("url", "")}'
    return None


# --------------------------------------------------------------------------
# I/O

def load_seeds(path: str) -> List[Dict[str, Any]]:
    with open(path, 'r', encoding='utf-8') as f:
        return json.load(f)


def write_run(out_dir: str, run: DiffRelateRun) -> None:
    os.makedirs(out_dir, exist_ok=True)
    json_path = os.path.join(out_dir, 'deep_relate.json')
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump([d.as_dict() for d in run.decisions], f, indent=2)
    print(f"  wrote {json_path} ({len(run.decisions)} decisions)")

    csv_path = os.path.join(out_dir, 'deep_relate.csv')
    related = [d for d in run.decisions if d.diff_related]
    import csv as _csv
    with open(csv_path, 'w', encoding='utf-8', newline='') as f:
        w = _csv.writer(f)
        w.writerow([
            'later_cve', 'prior_cve', 'diff_kind', 'confidence',
            'shared_files', 'cited_change',
            'later_patch_url', 'prior_patch_url',
        ])
        for d in related:
            w.writerow([
                d.later_cve, d.prior_cve, d.diff_kind,
                f'{d.confidence:.2f}',
                '|'.join(d.shared_files[:5]),
                d.cited_change,
                d.later_patch_url or '', d.prior_patch_url or '',
            ])
    print(f"  wrote {csv_path} ({len(related)} related rows)")
