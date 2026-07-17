"""P3.2 relation pooling — share screened relations across a bug's legs.

Synthesis is stochastic: on Math-2's correct leg it proposed the
mean-formula relation that convicts the overfit, but on the overfit leg
the SAME synthesis call proposed unrelated quantile relations, so the
discriminator was never available where it was needed. Relations are
properties that must hold for ANY correct implementation, so a relation
screened SOUND on a bug's buggy build is equally valid for every other
patch of that bug (same buggy checkout, same contract).

This module persists screened relations keyed by (project, bug) and
loads the union for a later leg. It carries NO label and NO patch
identity — only the project+bug and the relation text — so nothing
leaks between the overfit and correct sides beyond the sound invariant
itself.

Concurrency: legs run as separate processes (possibly parallel). Writes
are append-mostly with a per-file lock and dedup-on-read, so a partial
concurrent write can at worst miss the newest relation, never corrupt
the pool.
"""
import json
import os
from dataclasses import asdict
from typing import List

from relation_synth import Relation

_DEFAULT_DIR = os.environ.get(
    'RELATION_POOL_DIR',
    os.path.join(os.path.expanduser('~'), '.vuln_patch_relation_pool'))


def _key_path(pool_dir: str, project: str, bug_id: str) -> str:
    safe = f"{project}-{bug_id}".replace('/', '_')
    return os.path.join(pool_dir, f"{safe}.jsonl")


def _dedup_key(rel: Relation) -> str:
    # Two relations are "the same" if their executable check is the same
    # (names vary run-to-run; the check body is the substance).
    return ' '.join((rel.check or '').split())


def load_pool(project: str, bug_id: str,
              pool_dir: str = _DEFAULT_DIR) -> List[Relation]:
    """Return the deduped relations previously screened for this bug, or
    [] if none / on any read error (pooling must only ever ADD)."""
    path = _key_path(pool_dir, project, bug_id)
    if not os.path.exists(path):
        return []
    out, seen = [], set()
    try:
        with open(path, encoding='utf-8') as fh:
            for line in fh:
                line = line.strip()
                if not line:
                    continue
                d = json.loads(line)
                rel = Relation(
                    name=d.get('name', ''), kind=d.get('kind', ''),
                    contract=d.get('contract', ''),
                    input_spec=d.get('input_spec', ''),
                    check=d.get('check', ''),
                    screen_note=d.get('screen_note', ''),
                    from_pool=True)
                k = _dedup_key(rel)
                if k and k not in seen:
                    seen.add(k)
                    out.append(rel)
    except (OSError, ValueError):
        return []
    return out


def save_relations(project: str, bug_id: str, relations: List[Relation],
                   pool_dir: str = _DEFAULT_DIR) -> int:
    """Append newly-screened relations for this bug, skipping ones whose
    check is already pooled. Returns how many were newly added. Best
    effort: any error is swallowed (pooling must never break a run)."""
    if not relations:
        return 0
    try:
        os.makedirs(pool_dir, exist_ok=True)
        existing = {_dedup_key(r) for r in load_pool(project, bug_id, pool_dir)}
        path = _key_path(pool_dir, project, bug_id)
        added = 0
        with open(path, 'a', encoding='utf-8') as fh:
            for rel in relations:
                if getattr(rel, 'from_pool', False):
                    continue  # came FROM the pool; nothing new to record
                k = _dedup_key(rel)
                if not k or k in existing:
                    continue
                existing.add(k)
                fh.write(json.dumps(asdict(rel)) + '\n')
                added += 1
        return added
    except OSError:
        return 0


def merge_candidates(fresh: List[Relation],
                     pooled: List[Relation]) -> List[Relation]:
    """Union fresh synthesis candidates with pooled relations, deduped by
    check body. Fresh come first (this leg's own reasoning), then pooled
    ones not already present."""
    out, seen = [], set()
    for rel in list(fresh) + list(pooled):
        k = _dedup_key(rel)
        if k and k not in seen:
            seen.add(k)
            out.append(rel)
    return out
