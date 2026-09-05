"""Turning per-leg metrics into the paper's tables.

`metrics.write_metrics` leaves one line per leg in ``metrics.jsonl``.  This
module rolls those lines up:

`aggregate(run_dir, gated_only=...)`
    Macro-averages, in this order (the order matters):
      1. per BUG — the mean over that bug's legs, so a bug that several APR
         tools patched counts exactly once;
      2. per bug class — the mean and standard deviation over those bug
         means, for ``crashing``, ``semantic`` and ``all``.
    Also builds the RCC-versus-caught table: for the overfitting legs, the
    rate at which the pipeline caught the patch inside each RCC bin, and
    the mean RCC of the caught legs against the missed ones.  That table is
    per leg, not per bug — it is asking about individual decisions.
    With `gated_only`, legs whose bug failed the triggering-test gate are
    dropped first: their own triggering tests do not reach every method the
    developer fix changed, so their R̂ or their coverage plumbing is wrong
    and their RCC is a number about the tooling. The count that left is
    always reported (`n_gate_failed`), gated or not.

`delta(naive_run, conditioned_run)`
    Joins the two runs' legs on (project, bug_id, apr_tool, label), keeps
    the legs both runs have, aggregates each side over exactly that shared
    set, and reports conditioned minus naive (H_R - H_N).

`render_markdown(agg)`
    The Table 3 layout: rows are (bug class x granularity x
    {H_N, H_R, delta}), columns are RCR RCC RCP PSC CSM, ``n/a`` wherever a
    number is undefined.  It accepts either a `delta()` result (three rows
    per block) or a plain `aggregate()` result (one H_R row per block).
    Its `build` argument says which harness set the F-using columns are
    about — the default ``buggy`` is the harnesses the acceptance gate
    KEPT — and when the run also measured the ``compiled`` set (every
    candidate that compiled, kept or not) a second table for it is printed
    below the first.

The kind of fuzzer-reachable set
--------------------------------
RCC, RCP and PSC are computed from F(H), and F(H) has two possible kinds —
``dyn`` (run-time coverage, the only one produced today) and ``stat`` (the
planned static call-graph variant).  Their metric keys carry the kind in a
fifth slot (``rcc__method__R0__buggy__dyn``), so every function here that
picks an F-using column takes an `fkind` argument, defaulting to ``dyn``,
and the rendered table names the kind in those three column headers
("RCC (dyn)").  Nothing mixes the two kinds in one column.

"Standard deviation" here is the population standard deviation over the bug
means (`statistics.pstdev`): one bug gives 0.0, no bugs gives None.
"""
from __future__ import annotations

import json
import os
import statistics
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

from . import metrics as M

BUG_KINDS = ('crashing', 'semantic', 'all')

#: The three granularity rows every Table 3 block carries.  ``branch``
#: counts branch OUTCOMES on the same line sets ``line`` counts lines of
#: (see `metrics.GRANULARITIES`); it has no CSM column — a crash site is a
#: stack frame, not a branch outcome — so that cell prints ``n/a``.
GRANULARITIES = M.GRANULARITIES

#: RCC bins for the RCC-versus-caught table (lower bound inclusive, upper
#: exclusive; the last bin includes 1.0).
RCC_BINS = ((0.0, 0.25), (0.25, 0.5), (0.5, 0.75), (0.75, 1.0001))

#: The kind of fuzzer-reachable set the F-using columns are read for, when
#: the caller does not say.  Only ``dyn`` is produced today; see
#: `metrics.F_KINDS`.
DEFAULT_F_KIND = M.DEFAULT_F_KIND

#: Which flat metric field each Table 3 column reads, most-wanted first.
#: The first key present in the aggregate wins; nothing present prints n/a.
#: ``{g}`` is the granularity; ``{f}`` is the kind of F(H) and appears only
#: in the three columns whose metric reads F, which is also why those three
#: print their kind in the header ("RCC (dyn)").
#:
#: This is the default table: the KEPT harness set on the buggy build,
#: falling back to the patched build when a run has only that.  A table for
#: another build comes from `table3_columns`.
TABLE3_COLUMNS = (
    ('RCR', ('rcr__{g}__R0__na',), False),
    ('RCC', ('rcc__{g}__R0__buggy__{f}', 'rcc__{g}__R0__patched__{f}'), True),
    ('RCP', ('rcp__{g}__R0__buggy__{f}', 'rcp__{g}__R0__patched__{f}'), True),
    ('PSC', ('psc__{g}__na__buggy__{f}', 'psc__{g}__na__patched__{f}'), True),
    ('CSM', ('csm__{g}__R0__na',), False),
)

#: The build Table 3 is about unless the caller says otherwise.
DEFAULT_BUILD = 'buggy'

#: The harness set each build token's coverage is over, in words, for the
#: rendered table's block labels.  ``buggy`` and ``patched`` are the
#: harnesses the acceptance gate KEPT; ``compiled`` is every candidate that
#: compiled, whether the gate kept it or not (see `metrics.BUILDS`).
BUILD_LABELS = {
    'buggy': 'kept harnesses',
    'patched': 'kept harnesses',
    'compiled': 'all compiled harnesses',
    'remeasure': 'kept harnesses, fixed input budget',
}


#: What each kind of fuzzer-reachable set is, in words, for a table header.
#: Only the non-default kind is printed, so a ``dyn`` table's header is
#: exactly what it always was.
F_KIND_LABELS = {'dyn': 'dynamic coverage', 'stat': 'static reach'}


def table3_columns(build: str = DEFAULT_BUILD) -> tuple:
    """Table 3's columns, reading the F-using ones for ONE build.

    `DEFAULT_BUILD` gives exactly `TABLE3_COLUMNS` — buggy first, patched
    as the fallback for a run that has only the patched side.  Any other
    build (``compiled``) reads that build and nothing else: falling back
    to another build would silently put a different HARNESS SET in the
    same column, which is the whole distinction this parameter exists to
    keep.  RCR and CSM never read F(H), so they are the same in every
    table."""
    if build == DEFAULT_BUILD:
        return TABLE3_COLUMNS
    out = []
    for name, templates, uses_f in TABLE3_COLUMNS:
        if uses_f:
            first = templates[0]
            head, _, tail = first.rpartition('__{f}')
            base = head.rsplit('__', 1)[0]
            templates = (f'{base}__{build}__{{f}}',)
        out.append((name, templates, uses_f))
    return tuple(out)


def column_label(name: str, uses_f: bool, fkind: str) -> str:
    """The Table 3 header for one column: the F-using ones name their kind,
    e.g. ``RCC (dyn)``, so two tables built from different kinds of F cannot
    be mistaken for each other."""
    return f'{name} ({fkind})' if uses_f else name


def split_key(key: str) -> dict:
    """Take a flat metric field name apart.

    Handles both shapes — the 4-slot keys of the metrics that do not read
    F(H) (``rcr__method__R0__na``) and the 5-slot keys of the ones that do
    (``rcc__method__R0__buggy__dyn``) — and the ``#<ring>`` suffix
    `leg_values` appends to a per-ring ratio.  ``f_kind`` is None for a
    4-slot key; ``ring`` is None when there is no suffix."""
    base, _, ring = key.partition('#')
    parts = base.split('__')
    while len(parts) < 4:
        parts.append('na')
    metric, gran, rvar, build = parts[:4]
    fkind = parts[4] if len(parts) > 4 else None
    return {'metric': metric, 'granularity': gran, 'rvar': rvar,
            'build': build, 'f_kind': fkind, 'ring': (ring or None)}


# ---------------------------------------------------------------------------
# flattening one leg's metric fields
# ---------------------------------------------------------------------------

def _is_ratio(v) -> bool:
    return isinstance(v, dict) and 'value' in v and 'num' in v and 'den' in v


def leg_values(leg: dict) -> Dict[str, Optional[float]]:
    """Every ratio in one leg row, flattened to ``key -> value``.

    Per-ring ratios keep their parent's name with ``#<ring>`` appended, so
    the suffix lands after the whole key whichever shape it has, e.g.
    ``rcr__method__full__na#seed`` and
    ``rcp__method__full__buggy__dyn#outside``.  A ratio with a zero
    denominator contributes None and is skipped by every mean."""
    out: Dict[str, Optional[float]] = {}
    for key, val in leg.items():
        if not _is_ratio(val):
            continue
        out[key] = val['value']
        for ring, sub in (val.get('by_ring') or {}).items():
            if _is_ratio(sub):
                out[f'{key}#{ring}'] = sub['value']
    return out


def _mean(vals: Sequence[float]) -> Optional[float]:
    vals = [v for v in vals if v is not None]
    return (sum(vals) / len(vals)) if vals else None


def _std(vals: Sequence[float]) -> Optional[float]:
    vals = [v for v in vals if v is not None]
    if not vals:
        return None
    return statistics.pstdev(vals)


def bug_key(leg: dict) -> str:
    """A bug is a (project, bug_id) pair — NOT a patch and not a tool."""
    return leg.get('bug') or f"{leg.get('project')}-{leg.get('bug_id')}"


def leg_key(leg: dict) -> Tuple:
    """The identity two runs are joined on for `delta`."""
    return (leg.get('project'), str(leg.get('bug_id')), leg.get('apr_tool'),
            leg.get('label'))


# ---------------------------------------------------------------------------
# aggregation
# ---------------------------------------------------------------------------

def _per_bug(legs: Sequence[dict]) -> Dict[str, dict]:
    """Step 1 of the macro-average: collapse each bug's legs to one row."""
    bugs: Dict[str, dict] = {}
    for leg in legs:
        key = bug_key(leg)
        entry = bugs.setdefault(key, {
            'bug': key, 'bug_kind': leg.get('bug_kind'),
            'legs': [], 'values': {}, 'metrics': {}})
        entry['legs'].append(leg.get('leg'))
        if entry['bug_kind'] is None:
            entry['bug_kind'] = leg.get('bug_kind')
        for name, val in leg_values(leg).items():
            entry['values'].setdefault(name, []).append(val)
    for entry in bugs.values():
        entry['n_legs'] = len(entry['legs'])
        entry['metrics'] = {name: _mean(vals)
                            for name, vals in entry['values'].items()}
        entry.pop('values')
    return dict(sorted(bugs.items()))


def _per_kind(bugs: Dict[str, dict]) -> Dict[str, dict]:
    """Step 2: mean and spread over the bug means, per bug class."""
    out: Dict[str, dict] = {}
    for kind in BUG_KINDS:
        chosen = [b for b in bugs.values()
                  if kind == 'all' or b['bug_kind'] == kind]
        names = sorted({n for b in chosen for n in b['metrics']})
        stats = {}
        for name in names:
            vals = [b['metrics'].get(name) for b in chosen]
            defined = [v for v in vals if v is not None]
            stats[name] = {'mean': _mean(vals), 'std': _std(vals),
                           'n_bugs': len(defined)}
        out[kind] = {
            'n_bugs': len(chosen),
            'n_legs': sum(b['n_legs'] for b in chosen),
            'metrics': stats,
        }
    return out


def _rcc_key(legs: Sequence[dict],
             fkind: str = DEFAULT_F_KIND) -> Optional[str]:
    """The RCC field the caught table reads: method granularity, seed-ring
    R0, buggy build; patched build if that is all the run has.  RCC reads
    F(H), so the key carries the kind of F and only that kind is looked
    for."""
    present = {k for leg in legs for k in leg_values(leg)}
    for cand in (f'rcc__method__R0__buggy__{fkind}',
                 f'rcc__method__R0__patched__{fkind}',
                 f'rcc__method__full__buggy__{fkind}',
                 f'rcc__method__full__patched__{fkind}'):
        if cand in present:
            return cand
    return None


def rcc_vs_caught(legs: Sequence[dict], rcc_key: Optional[str] = None,
                  fkind: str = DEFAULT_F_KIND) -> dict:
    """Does a harness that ran more of the root cause catch more overfitting
    patches?  Per overfitting leg: its RCC, and whether the pipeline caught
    it (``caught`` = some harness still crashed the patched build).

    `fkind` picks which kind of fuzzer-reachable set the RCC column is read
    for; an explicit `rcc_key` overrides it."""
    over = [leg for leg in legs if leg.get('label') == 'overfitting']
    key = rcc_key or _rcc_key(over, fkind)
    rows = []
    for leg in over:
        rows.append({'leg': leg.get('leg'), 'bug': bug_key(leg),
                     'rcc': (leg_values(leg).get(key) if key else None),
                     'caught': bool(leg.get('caught'))})
    bins = []
    for lo, hi in RCC_BINS:
        inside = [r for r in rows
                  if r['rcc'] is not None and lo <= r['rcc'] < hi]
        caught = sum(1 for r in inside if r['caught'])
        bins.append({'lo': lo, 'hi': min(hi, 1.0), 'n': len(inside),
                     'caught': caught,
                     'caught_rate': (caught / len(inside)) if inside else None})
    undefined = [r for r in rows if r['rcc'] is None]
    bins.append({'lo': None, 'hi': None, 'n': len(undefined),
                 'caught': sum(1 for r in undefined if r['caught']),
                 'caught_rate': (sum(1 for r in undefined if r['caught'])
                                 / len(undefined)) if undefined else None})
    return {
        'rcc_key': key,
        'f_kind': (split_key(key)['f_kind'] if key else fkind),
        'n_overfitting_legs': len(rows),
        'n_caught': sum(1 for r in rows if r['caught']),
        'n_missed': sum(1 for r in rows if not r['caught']),
        'mean_rcc_caught': _mean([r['rcc'] for r in rows if r['caught']]),
        'mean_rcc_missed': _mean([r['rcc'] for r in rows if not r['caught']]),
        'bins': bins,
        'legs': rows,
    }


def gate_passed(leg: dict) -> Optional[bool]:
    """Did this leg's bug pass the triggering-test gate?

    None when the gate was not run — it is slow and off by default, so most
    runs have no answer, and "not asked" is not "failed"."""
    sizes = leg.get('sizes') or {}
    value = sizes.get('trigger_gate_passed')
    return bool(value) if value is not None else None


def aggregate_legs(legs: Sequence[dict], run_dir: Optional[str] = None,
                   fkind: str = DEFAULT_F_KIND,
                   gated_only: bool = False) -> dict:
    """Aggregate an explicit list of leg rows (used by `aggregate` and by
    `delta`, which needs the same maths over a restricted set of legs).

    Every metric present is averaged whatever its key, F-using or not;
    `fkind` only picks the RCC column of the RCC-versus-caught table.

    `gated_only` drops the legs whose bug FAILED the triggering-test gate:
    the bug's own triggering tests did not reach every method the developer
    fix changed, so its R̂ or the coverage plumbing is wrong and its RCC
    describes our tooling rather than the harness set. Legs whose gate was
    not run are KEPT — the gate is off by default, and treating "not asked"
    as "failed" would empty most runs. The count that left is reported as
    `n_gate_failed` either way, so a mean is never printed without it."""
    failed = [leg for leg in legs if gate_passed(leg) is False]
    if gated_only:
        legs = [leg for leg in legs if gate_passed(leg) is not False]
    scored = [leg for leg in legs if 'error' not in leg]
    bugs = _per_bug(scored)
    return {
        'run_dir': run_dir,
        'n_legs': len(scored),
        'n_bugs': len(bugs),
        'n_errors': len(legs) - len(scored),
        'n_gate_failed': len(failed),
        'gated_only': bool(gated_only),
        'f_kind': fkind,
        'by_kind': _per_kind(bugs),
        'per_bug': bugs,
        'rcc_vs_caught': rcc_vs_caught(scored, fkind=fkind),
    }


def aggregate(run_dir: str, fkind: str = DEFAULT_F_KIND,
              gated_only: bool = False) -> dict:
    """Read ``<run_dir>/metrics.jsonl`` and roll it up (see module docstring)."""
    return aggregate_legs(M.read_metrics(run_dir), run_dir=run_dir,
                          fkind=fkind, gated_only=gated_only)


# ---------------------------------------------------------------------------
# naive vs conditioned
# ---------------------------------------------------------------------------

def delta(run_dir_naive: str, run_dir_conditioned: str,
          fkind: str = DEFAULT_F_KIND) -> dict:
    """H_R - H_N on the legs the two runs share.

    H_N is the naive-harness run, H_R the root-cause-conditioned one.  Legs
    are matched on (project, bug_id, apr_tool, label); a leg only one run
    has is dropped so the comparison is paired.  When a run repeats an
    identity, the first leg in name order is used."""
    naive = M.read_metrics(run_dir_naive)
    cond = M.read_metrics(run_dir_conditioned)

    def by_id(rows):
        out = {}
        for row in sorted(rows, key=lambda r: r.get('leg') or ''):
            if 'error' in row:
                continue
            out.setdefault(leg_key(row), row)
        return out

    n_by, c_by = by_id(naive), by_id(cond)
    shared = sorted(set(n_by) & set(c_by))
    agg_n = aggregate_legs([n_by[k] for k in shared], run_dir_naive,
                           fkind=fkind)
    agg_c = aggregate_legs([c_by[k] for k in shared], run_dir_conditioned,
                           fkind=fkind)

    diff: Dict[str, Dict[str, Optional[float]]] = {}
    for kind in BUG_KINDS:
        mn = agg_n['by_kind'][kind]['metrics']
        mc = agg_c['by_kind'][kind]['metrics']
        row = {}
        for name in sorted(set(mn) | set(mc)):
            a = mc.get(name, {}).get('mean')
            b = mn.get(name, {}).get('mean')
            row[name] = (a - b) if (a is not None and b is not None) else None
        diff[kind] = row
    return {
        'naive_run': run_dir_naive,
        'conditioned_run': run_dir_conditioned,
        'n_common_legs': len(shared),
        'n_common_bugs': agg_c['n_bugs'],
        'f_kind': fkind,
        'joined': [list(k) for k in shared],
        'dropped_naive': [list(k) for k in sorted(set(n_by) - set(c_by))],
        'dropped_conditioned': [list(k) for k in sorted(set(c_by) - set(n_by))],
        'naive': agg_n,
        'conditioned': agg_c,
        'delta': diff,
    }


# ---------------------------------------------------------------------------
# rendering
# ---------------------------------------------------------------------------

def _fmt(v: Optional[float], signed: bool = False) -> str:
    if v is None:
        return 'n/a'
    return f'{v:+.3f}' if signed else f'{v:.3f}'


def _pick(metrics_map: dict, candidates: Sequence[str]) -> Optional[str]:
    for cand in candidates:
        if cand in metrics_map:
            return cand
    return None


def metric_names(agg: dict) -> set:
    """Every flat metric field name anywhere in an `aggregate()` or
    `delta()` result.  Used to ask whether a build was measured at all."""
    names = set()
    for source in (agg.get('by_kind'), agg.get('delta'),
                   (agg.get('naive') or {}).get('by_kind'),
                   (agg.get('conditioned') or {}).get('by_kind')):
        for entry in (source or {}).values():
            if isinstance(entry, dict) and 'metrics' in entry:
                names |= set(entry['metrics'])
            elif isinstance(entry, dict):
                names |= set(entry)
    return names


def has_build(agg: dict, build: str) -> bool:
    """True when any F-using metric in `agg` was computed for `build`."""
    return any(split_key(name)['build'] == build for name in metric_names(agg))


def has_fkind(agg: dict, fkind: str) -> bool:
    """True when any metric in `agg` was computed from that KIND of F(H).

    What says whether a run measured the static reachable set at all: only
    the three F-using metrics carry a kind, and only they get a ``stat``
    variant."""
    return any(split_key(name)['f_kind'] == fkind
               for name in metric_names(agg))


def _table3_blocks(agg: dict, fkind: str, build: str) -> str:
    """One Table 3 table, for one build's harness set."""
    paired = 'delta' in agg and 'naive' in agg and 'conditioned' in agg
    label = BUILD_LABELS.get(build, build)
    # The default kind is not named in the header — that is what every
    # table said before a second kind existed — so only a `stat` table
    # announces itself.
    kind_note = ('' if fkind == DEFAULT_F_KIND
                 else f', {F_KIND_LABELS.get(fkind, fkind)}')
    if paired:
        blocks = [('H_N', agg['naive']['by_kind'], False),
                  ('H_R', agg['conditioned']['by_kind'], False),
                  ('delta', agg['delta'], True)]
        header = (f"Table 3 — root-cause conditioning, "
                  f"{agg.get('n_common_legs')} paired legs "
                  f"over {agg.get('n_common_bugs')} bugs "
                  f"({label}, build {build}{kind_note})")
    else:
        blocks = [('H_R', agg['by_kind'], False)]
        header = (f"Table 3 — {agg.get('n_legs')} legs over "
                  f"{agg.get('n_bugs')} bugs "
                  f"({label}, build {build}{kind_note})")

    columns = table3_columns(build)
    cols = [column_label(name, uses_f, fkind)
            for name, _, uses_f in columns]
    lines = [header, '',
             '| bug class | granularity | run | ' + ' | '.join(cols) + ' |',
             '| --- | --- | --- | ' + ' | '.join('---' for _ in cols) + ' |']
    for kind in BUG_KINDS:
        for gran in GRANULARITIES:
            for row_name, source, is_delta in blocks:
                if is_delta:
                    table = source.get(kind, {})

                    def get(key, _t=table):
                        return _t.get(key)
                else:
                    entry = source.get(kind, {})
                    table = entry.get('metrics', {})

                    def get(key, _t=table):
                        return (_t.get(key) or {}).get('mean')
                cells = []
                for col, templates, _uses_f in columns:
                    key = _pick(table, [t.format(g=gran, f=fkind)
                                        for t in templates])
                    cells.append(_fmt(get(key) if key else None,
                                      signed=is_delta))
                lines.append(f'| {kind} | {gran} | {row_name} | '
                             + ' | '.join(cells) + ' |')
    return '\n'.join(lines) + '\n'


def render_markdown(agg: dict, fkind: Optional[str] = None,
                    build: str = DEFAULT_BUILD) -> str:
    """The Table 3 markdown block.

    Accepts a `delta()` result — rows H_N, H_R and delta — or a plain
    `aggregate()` result, which has only the one run to show (rendered as
    H_R).

    `fkind` picks which kind of fuzzer-reachable set the RCC, RCP and PSC
    columns are read for, and those three columns name it in their header
    ("RCC (dyn)").  It defaults to the kind the aggregate was built with,
    and to `DEFAULT_F_KIND` for an aggregate that does not record one.

    `build` picks whose coverage those three columns are read from, and
    with it which HARNESS SET the table is about: the default `buggy` is
    the harnesses the acceptance gate KEPT, and `compiled` is every
    candidate that compiled.  Each table says which in its header.  When
    the default table is asked for and the run also measured the compiled
    set, a SECOND table for it follows, because a kept-only number is
    partly the gate's doing — see the measurements README, "Kept versus
    all compiled harnesses"."""
    if fkind is None:
        fkind = agg.get('f_kind') or DEFAULT_F_KIND
    out = _table3_blocks(agg, fkind, build)
    if build == DEFAULT_BUILD and has_build(agg, 'compiled'):
        out += '\n' + _table3_blocks(agg, fkind, 'compiled')
    # The same table read from the STATIC reachable set, when the run
    # measured one. It is a separate block, never extra columns: RCC over
    # what a harness could reach and RCC over what it did reach are two
    # different numbers, and putting them side by side in one row invites
    # exactly the comparison the fifth key slot exists to prevent.
    if fkind == DEFAULT_F_KIND and has_fkind(agg, M.F_KIND_STATIC):
        out += '\n' + _table3_blocks(agg, M.F_KIND_STATIC, build)
    return out


def render_rcc_vs_caught(agg: dict) -> str:
    """The RCC-versus-caught table as markdown (a diagnostic, not Table 3)."""
    t = agg.get('rcc_vs_caught') or {}
    lines = [f"RCC vs caught ({t.get('rcc_key') or 'no RCC available'}) — "
             f"{t.get('n_overfitting_legs', 0)} overfitting legs, "
             f"{t.get('n_caught', 0)} caught", '',
             '| RCC bin | legs | caught | caught rate |',
             '| --- | --- | --- | --- |']
    for b in t.get('bins', []):
        label = ('undefined' if b['lo'] is None
                 else f"{b['lo']:.2f}-{b['hi']:.2f}")
        lines.append(f"| {label} | {b['n']} | {b['caught']} | "
                     f"{_fmt(b['caught_rate'])} |")
    lines += ['', f"mean RCC caught: {_fmt(t.get('mean_rcc_caught'))}   "
                  f"mean RCC missed: {_fmt(t.get('mean_rcc_missed'))}"]
    return '\n'.join(lines) + '\n'
