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


def load_all():
    """All entries (menu + optional + documented), for studies/coverage."""
    return list(_load().get('entries', []))


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


def _relevance(entry: dict, context_text: str) -> int:
    """How many of an entry's applicability keywords appear in the touched
    method's text (name + javadoc + class name). Higher = more likely the
    entry's CONDITION actually holds here, so it should survive the cap."""
    if not context_text:
        return 0
    ctx = context_text.lower()
    return sum(1 for k in entry.get('keywords', []) if k.lower() in ctx)


def entries_for_kinds(kinds: List[str],
                      cap: int = 3,
                      include_optional: bool = True,
                      context_text: str = '') -> List[dict]:
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
    # Context relevance dominates when a context is given: an entry whose
    # applicability keywords match the touched method outranks a globally
    # "high-priority" entry that does not fit (trig injected for a
    # distribution was the bug this fixes). With no context, fall back to
    # status + static priority.
    ranked = sorted(
        seen.values(),
        key=lambda e: (-_relevance(e, context_text),
                       _status_rank.get(e.get('status'), 3),
                       e.get('priority', 2)))
    return ranked[:cap]


def render_entry(entry: dict) -> str:
    """One entry as prompt text: the variation WITH its condition and
    known exceptions — never the statement alone."""
    return (f"HARMLESS VARIATION ({entry['id']}): {entry['statement']}\n"
            f"  APPLIES ONLY IF: {entry['condition']}\n"
            f"  DO NOT APPLY TO: {entry['exceptions']}")


# ---- content-aware selection: nano ranks, keyword scoring is the fallback ----
_RANK_SYS = (
    "You pick the metamorphic-testing relations most relevant to a Java "
    "method, to check a candidate patch. Given the method context and a "
    "numbered list of candidate relations (all already sound for this kind "
    "of input), reply with ONLY a JSON array of the N most relevant relation "
    "ids, exact strings from the list, most relevant first. No other text. "
    "Prefer relations whose CONDITION plausibly holds for this specific "
    "method over generic ones."
)


def select_relevant(kinds: List[str], context_text: str, cap: int = 3,
                    generator=None, include_optional: bool = True):
    """The content-aware selection R4 injects. Deterministic detection picks
    the kinds (free); this picks which of the kind's candidates make the cap.

    Evidence (study/rank_eval.py, 25 tasks): the keyword score alone falls
    back to bad static defaults when the javadoc lacks the matching word
    (trig injected for KMeans/Base64/digest). A cheap model (gpt-5.4-nano)
    is robust to that. So: nano ranks the candidate list when a generator is
    given; keyword scoring is the offline fallback. Either way the result is
    3 SOUND candidates — ranking only affects relevance, never soundness
    (condition-check + screening + judge still gate)."""
    candidates = entries_for_kinds(kinds, cap=999, context_text='',
                                   include_optional=include_optional)
    if not candidates:
        return []
    if len(candidates) <= cap:
        return candidates
    if generator is not None:
        try:
            picked = _nano_rank_ids(candidates, context_text, cap, generator)
            chosen = [c for pid in picked for c in candidates if c['id'] == pid]
            # top up from keyword order if nano returned fewer than cap
            if len(chosen) < cap:
                for c in entries_for_kinds(kinds, cap=cap,
                                           context_text=context_text):
                    if c not in chosen:
                        chosen.append(c)
                    if len(chosen) >= cap:
                        break
            return chosen[:cap]
        except Exception:
            pass  # fall through to keyword ranking
    return entries_for_kinds(kinds, cap=cap, context_text=context_text,
                             include_optional=include_optional)


def _nano_rank_ids(candidates, context_text, cap, generator):
    import re
    listing = "\n".join(f'- {c["id"]}: {c["statement"]}' for c in candidates)
    prompt = (f"Method context:\n{context_text}\n\nCandidate relations:\n"
              f"{listing}\n\nReturn the {cap} most relevant ids.")
    reply = generator.generate([
        {"role": "system", "content": _RANK_SYS.replace('N most', f'{cap} most')},
        {"role": "user", "content": prompt}])
    m = re.search(r'\[([^\]]*)\]', reply or '')
    ids = []
    valid = {c['id'] for c in candidates}
    if m:
        for raw in re.findall(r'"([^"]+)"|\'([^\']+)\'|([\w-]+)', m.group(1)):
            v = next(x for x in raw if x)
            if v in valid and v not in ids:
                ids.append(v)
    return ids[:cap]
