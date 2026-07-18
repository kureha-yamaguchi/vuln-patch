"""R4 closed menu of harmless input variations — data access.

The menu lives in variation_menu.json (derived from the deduplicated
cross-source catalog suites/menu-candidates.md; governed by the
anti-overfitting contract in semantic-recall-brainstorm.md R4(c)).

The lookup model is deliberately dumb: exact match on the detected
input kind. Detection (when R4 is wired up) is a three-tier hybrid:
type-shaped kinds mechanically from signatures (number, collection,
encode/decode pair); ambiguous text-shaped cases via ONE constrained
classification call whose output is a single label from
input_kind_vocabulary or "unknown" (logged, cached per leg); unknown
injects nothing. The model under prompt never sees the whole menu
and never chooses a category — the pipeline injects at most the
entries whose `input_kinds` contain the detected kind, rendered with
their condition and exceptions attached (an entry without its
condition is exactly the unsound-rule generator the closed menu
exists to prevent).

NOT yet wired into any prompt: that is TO DO item R4 and gets its own
measurement point (ground rule 1). This module only provides the data
so the wiring is a one-line change when its turn comes.
"""
import json
import os
from typing import List, Optional

_MENU_PATH = os.path.join(os.path.dirname(__file__), 'variation_menu.json')
_cache: Optional[dict] = None

# Statuses that may ever be injected into a prompt. Pool and
# anchor-candidate entries are records, not menu items.
_INJECTABLE = ('menu', 'menu_optional', 'menu_documented')


def _load() -> dict:
    global _cache
    if _cache is None:
        with open(_MENU_PATH, encoding='utf-8') as fh:
            _cache = json.load(fh)
    return _cache


def input_kinds() -> List[str]:
    """The closed vocabulary of detectable input kinds."""
    return list(_load().get('input_kind_vocabulary', []))


def entries_for_kind(kind: str,
                     include_optional: bool = True) -> List[dict]:
    """Menu entries applicable to one detected input kind (possibly
    empty — the fail-safe: unknown kind means no entry, never a wrong
    one)."""
    statuses = _INJECTABLE if include_optional else ('menu',)
    return [e for e in _load().get('entries', [])
            if e.get('status') in statuses
            and kind in e.get('input_kinds', [])]


def entries_for_kinds(kinds: List[str],
                      cap: int = 3,
                      include_optional: bool = True) -> List[dict]:
    """Menu entries for a SET of detected kinds — the real detection
    output, since one class legitimately spans kinds (a date formatter
    consumes numbers AND is a format/parse pair). Union over the
    kinds, deduplicated by entry id, capped at `cap` to respect the
    measured injected-mass limit. ORDER MATTERS in `kinds`: pass the
    kinds of the touched method's own parameters first — priority
    follows list order, then status ('menu' before 'menu_optional')."""
    seen: dict = {}
    for kind in kinds:
        for e in entries_for_kind(kind, include_optional):
            seen.setdefault(e['id'], e)
    _status_rank = {'menu': 0, 'menu_optional': 1, 'menu_documented': 2}
    ranked = sorted(
        seen.values(),
        key=lambda e: (_status_rank.get(e.get('status'), 3),
                       e.get('priority', 2)))
    return ranked[:cap]


def render_entry(entry: dict) -> str:
    """One entry as prompt text: the variation WITH its condition and
    known exceptions — never the statement alone."""
    return (f"HARMLESS VARIATION ({entry['id']}): {entry['statement']}\n"
            f"  APPLIES ONLY IF: {entry['condition']}\n"
            f"  DO NOT APPLY TO: {entry['exceptions']}")
