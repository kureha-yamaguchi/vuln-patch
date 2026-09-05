"""The paper's two result tables, filled straight from a run's aggregates.

The draft paper (``docs/Fuzzing_Harness_Generator_2027_Paper.pdf``) has two
result tables whose cells are still ``XX.X``.  This module fills them from
the files `cli.py` already writes — ``metrics.jsonl`` and ``aggregate.json``
— so a table in the paper can always be traced back to a run directory and
a key set.  It computes no new measurement of its own: every number here is
either a mean/standard deviation `aggregate.py` produced, a difference of
two such means, or a count of leg outcomes.

    Table 3  the main result.  Rows are harness set (H_N, H_R, their
             difference) x bug class (All, Crashing, Semantic) x
             granularity; columns are RCR, RCC, RCP, PSC, CSM and F1.
    Table 4  panel (a), coverage of the root-cause region per bug kind,
             with the harness sets as columns and "mean +/- std" cells;
             panel (b), the classification counts and P / R / F1.

Both come in two renderings, chosen with ``fmt``: ``md`` for the writeup
and ``latex`` for the paper (a booktabs tabular, cells like
``0.71 $\\pm$ 0.12``).

Function, LINE and BRANCH, never edge
-------------------------------------
The paper's fine granularity is the control-flow edge.  We do not measure
control-flow edges: a full CFG-edge version would need bytecode-level
analysis (ASM plus JaCoCo probes) nobody has built (see the README,
section 2).  Three rows are rendered instead:

* ``Function`` — the Java method or constructor;
* ``Line``     — the source line;
* ``Branch``   — the branch OUTCOMES on those same lines, from JaCoCo's
  per-line ``mb``/``cb``.  That counts the outgoing edges of decision
  points only: the fallthrough edge of a straight-line statement is not a
  JaCoCo branch and is not represented.  It is the closest observable
  stand-in for the paper's edge level, and it is labelled ``Branch``, not
  ``Edge``.

The string "Edge" is never emitted.  CSM has no Branch cell — a crash site
is a stack frame, not a branch outcome — so it prints an en dash there.

What is undefined, and why
--------------------------
Three kinds of cell print an en dash instead of a number, and the reason
is different in each case:

* **RCR in the difference block.**  RCR judges the patch-derived set P,
  which is built before any harness exists, so it is the same for the
  naive and the conditioned arm and no H_R - H_N difference is defined.
* **CSM for semantic bugs.**  A semantic bug does not crash: the harness
  itself supplies the verdict, so there is no library crash site to match
  against the root-cause region.
* **A missing arm.**  With no naive run given, every H_N cell — and so
  every difference cell — is a dash rather than a zero.

An ordinary undefined ratio (an empty denominator, or a key a run never
emitted, such as the R1 variant at line granularity) prints the same dash.

Pairing
-------
When both arms are given, the two runs are joined leg by leg on
(project, bug id, APR tool, label) exactly as `aggregate.delta` does, and
BOTH arms are then aggregated over the shared legs only, so the difference
column subtracts two numbers computed over the same patches.  The note
under each table says how many legs that left.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from typing import Dict, List, Optional, Sequence, Tuple

from . import aggregate as A
from . import ratios as M

AGGREGATE_FILE = 'aggregate.json'

#: Bug classes, in the paper's row order.  ``all`` is computed over the
#: complete set of legs, not as the mean of the other two.
BUG_CLASSES = ('all', 'crashing', 'semantic')
CLASS_LABELS = {'all': 'All', 'crashing': 'Crashing', 'semantic': 'Semantic'}

#: Our granularities and the names the tables give them.  The paper calls
#: its fine granularity "Edge" and we do not measure control-flow edges, so
#: the labels are ``Line`` and ``Branch`` and never ``Edge``.
GRANULARITIES = M.GRANULARITIES
GRAN_LABELS = {'method': 'Function', 'line': 'Line', 'branch': 'Branch'}

#: The five set metrics, in the paper's column / row order, with the flag
#: that says whether the metric reads the fuzzer-reachable set F(H) (those
#: three carry the kind of F in their key and in the tables' key note).
SET_METRICS = (
    ('RCR', 'Root-cause recovery, RCR', False),
    ('RCC', 'Root-cause coverage, RCC', True),
    ('RCP', 'Root-cause precision, RCP', True),
    ('PSC', 'Patch-derived set coverage, PSC', True),
    ('CSM', 'Crash-site match, CSM', False),
)

#: Defaults for the three axes a table has to fix before it can name a
#: single key: the kind of F(H), the build (which harness set), and the
#: R-variant.  ``R0`` is the narrowest region — the methods the developer
#: changed — and is the paper's R̂.
DEFAULT_F_KIND = M.DEFAULT_F_KIND
DEFAULT_BUILD = A.DEFAULT_BUILD
DEFAULT_RVAR = 'R0'

#: What ``--rvar`` accepts: the method-level variants plus the line-only
#: ones (`metrics.R_VARIANTS_LINE_ONLY`, today just ``Rbody``).  A table
#: has one R-variant for both of its granularity rows, so a line-only
#: variant is honoured on the Line row and the Function row falls back to
#: `DEFAULT_RVAR` — see `metric_field`.
R_VARIANTS = M.R_VARIANTS + M.R_VARIANTS_LINE_ONLY

#: What a leg's `outcome` field means for the classifier, with an
#: overfitting patch as the positive case.
OUTCOME_TO_CELL = {'caught': 'tp', 'missed': 'fn',
                   'false_alarm': 'fp', 'clean': 'tn'}

#: What each table is called, in both renderings — a pasted LaTeX body
#: has to say which of the paper's tables it belongs in.
TABLE3_TITLE = ('Table 3 — root-cause conditioning, by bug class and '
                'granularity')
TABLE4_TITLE = ('Table 4 — coverage of the root-cause region, and '
                'classification')

#: Decimal places in a rendered cell.  Two is what the paper's draft
#: shows; RCP is often a few thousandths, so `--dp 3` exists.
DEFAULT_DP = 2

_DASH = {'md': '–', 'latex': '--'}
FORMATS = ('md', 'latex')


def _dash(fmt: str) -> str:
    return _DASH[fmt]


def _plural(n: int, word: str) -> str:
    return f'{n} {word}' if n == 1 else f'{n} {word}s'


# ---------------------------------------------------------------------------
# one arm of the experiment
# ---------------------------------------------------------------------------

class Arm:
    """One run directory, read as one harness-generation method.

    `name` is ``HN`` (the naive arm) or ``HR`` (the root-cause-conditioned
    arm); `legs` are the ``metrics.jsonl`` rows and `agg` the
    `aggregate.aggregate` result for them.  Nothing is recomputed that the
    run directory already holds."""

    def __init__(self, name: str, run_dir: str, legs: Sequence[dict],
                 agg: dict, aggregate_source: str = 'computed'):
        self.name = name
        self.run_dir = run_dir
        self.legs = list(legs)
        self.agg = agg
        self.aggregate_source = aggregate_source

    # -- identity ---------------------------------------------------------
    def __repr__(self) -> str:                          # pragma: no cover
        return f'<Arm {self.name} {self.run_dir} {len(self.legs)} legs>'

    @property
    def label(self) -> str:
        """``H_N`` / ``H_R``, the way the paper writes the arm."""
        return f'H_{self.name[-1]}' if self.name.upper().startswith('H') \
            else self.name

    # -- numbers ----------------------------------------------------------
    def by_kind(self, bug_class: str) -> dict:
        return (self.agg.get('by_kind') or {}).get(bug_class, {})

    def n(self, bug_class: str = 'all') -> Tuple[int, int]:
        """(bugs, legs) in one bug class."""
        entry = self.by_kind(bug_class)
        return int(entry.get('n_bugs') or 0), int(entry.get('n_legs') or 0)

    def stat(self, key: str, bug_class: str) -> Optional[dict]:
        """The ``{mean, std, n_bugs}`` of one flat metric field, or None
        when this run never emitted that field."""
        return (self.by_kind(bug_class).get('metrics') or {}).get(key)

    def legs_of(self, bug_class: str) -> List[dict]:
        return [leg for leg in self.legs
                if 'error' not in leg
                and (bug_class == 'all' or leg.get('bug_kind') == bug_class)]

    def f1(self, bug_class: str = 'all') -> dict:
        """Binary classification from the legs' `outcome` field.

        A leg's outcome is one of ``caught`` / ``missed`` / ``false_alarm``
        / ``clean``, written by `metrics.compute_leg` from the patch's
        ground-truth label and whether a harness crashed the patched build.
        With an overfitting patch as the positive case those are TP / FN /
        FP / TN, and precision, recall and F1 follow.  A leg with no
        outcome (an unevaluated or errored leg) is counted in `n_unlabelled`
        and takes part in nothing else."""
        return f1_from_legs(self.legs_of(bug_class))


def f1_from_legs(legs: Sequence[dict]) -> dict:
    """Precision / recall / F1 over a list of leg rows (see `Arm.f1`)."""
    cells = {'tp': 0, 'fp': 0, 'fn': 0, 'tn': 0}
    unlabelled = 0
    for leg in legs:
        cell = OUTCOME_TO_CELL.get(leg.get('outcome'))
        if cell is None:
            unlabelled += 1
        else:
            cells[cell] += 1
    tp, fp, fn, tn = cells['tp'], cells['fp'], cells['fn'], cells['tn']
    precision = (tp / (tp + fp)) if (tp + fp) else None
    recall = (tp / (tp + fn)) if (tp + fn) else None
    if precision and recall:
        f1 = 2 * precision * recall / (precision + recall)
    elif precision is None or recall is None:
        f1 = None
    else:                                   # both defined, at least one 0
        f1 = 0.0
    return {'tp': tp, 'fp': fp, 'fn': fn, 'tn': tn,
            'n_unlabelled': unlabelled,
            'n_overfitting': tp + fn, 'n_correct': fp + tn,
            'n_legs': tp + fp + fn + tn + unlabelled,
            'precision': precision, 'recall': recall, 'f1': f1}


def load_arm(run_dir: str, name: str = 'HR',
             fkind: str = DEFAULT_F_KIND) -> Arm:
    """Read one run directory as an `Arm`.

    ``metrics.jsonl`` is read if it is there and recomputed in memory from
    the legs' ``measurements/`` JSON if it is not — nothing is written back,
    so an archived run stays exactly as it was archived.  ``aggregate.json``
    is used as it stands when it holds a ``by_kind`` block, and otherwise
    `aggregate.aggregate_legs` recomputes it.  The stored aggregate is
    reused whatever `fkind` says, because every metric key carries its own
    kind of F(H) — `fkind` selects a column, it does not change one."""
    run_dir = os.path.abspath(run_dir)
    if not os.path.isdir(run_dir):
        raise ValueError(f'not a run directory: {run_dir}')
    legs = M.read_metrics(run_dir)
    if not legs:
        legs = []
        for leg_dir in M.leg_dirs(run_dir):
            try:
                legs.append(M.compute_leg(leg_dir))
            except Exception as exc:                    # never lose the arm
                legs.append({'leg': os.path.basename(leg_dir),
                             'error': f'{type(exc).__name__}: {exc}'})
    agg = None
    source = 'recomputed (no aggregate.json)'
    path = os.path.join(run_dir, AGGREGATE_FILE)
    if os.path.isfile(path):
        try:
            with open(path) as fh:
                stored = json.load(fh)
        except (OSError, ValueError):
            stored = None
        if isinstance(stored, dict) and stored.get('by_kind'):
            missing = stale_keys(stored, legs)
            if missing:
                source = (f'recomputed ({AGGREGATE_FILE} is stale: '
                          f'{len(missing)} key(s) the legs carry are not in '
                          f'it, e.g. {sorted(missing)[0]})')
            else:
                agg, source = stored, AGGREGATE_FILE
        elif stored is not None:
            source = f'recomputed ({AGGREGATE_FILE} unreadable)'
    if agg is None:
        agg = A.aggregate_legs(legs, run_dir=run_dir, fkind=fkind)
    return Arm(name, run_dir, legs, agg, aggregate_source=source)


def stale_keys(stored: dict, legs: Sequence[dict]) -> set:
    """Metric fields the legs carry that a stored ``aggregate.json`` does not.

    An archived aggregate can predate the current key format — the runs
    measured before the kind of F(H) moved into a fifth slot hold
    ``rcc__method__R0__buggy`` where their own ``metrics.jsonl`` now holds
    ``rcc__method__R0__buggy__dyn``.  Reusing such a file would fill the
    whole RCC/RCP/PSC side of both tables with dashes while the numbers sat
    in ``metrics.jsonl`` all along, so any key the legs have and the stored
    aggregate lacks makes the file stale and the aggregate is recomputed."""
    have = set()
    for entry in (stored.get('by_kind') or {}).values():
        have |= set(entry.get('metrics') or {})
    want = set()
    for leg in legs:
        if 'error' not in leg:
            want |= set(A.leg_values(leg))
    return want - have


# ---------------------------------------------------------------------------
# pairing the two arms
# ---------------------------------------------------------------------------

def pair(hr: Arm, hn: Optional[Arm]) -> Tuple[Arm, Optional[Arm], dict]:
    """Restrict both arms to the legs they share, as `aggregate.delta` does.

    Returns the two restricted arms and a small dict describing the join.
    With no naive arm nothing is restricted."""
    if hn is None:
        bugs, legs = hr.n('all')
        return hr, None, {'paired': False, 'n_legs': legs, 'n_bugs': bugs,
                          'dropped_hr': 0, 'dropped_hn': 0}

    def by_id(arm: Arm) -> Dict[tuple, dict]:
        out: Dict[tuple, dict] = {}
        for row in sorted(arm.legs, key=lambda r: r.get('leg') or ''):
            if 'error' in row:
                continue
            out.setdefault(A.leg_key(row), row)
        return out

    r_by, n_by = by_id(hr), by_id(hn)
    shared = sorted(set(r_by) & set(n_by))
    r_legs = [r_by[k] for k in shared]
    n_legs = [n_by[k] for k in shared]
    hr2 = Arm(hr.name, hr.run_dir, r_legs,
              A.aggregate_legs(r_legs, run_dir=hr.run_dir),
              'recomputed over the shared legs')
    hn2 = Arm(hn.name, hn.run_dir, n_legs,
              A.aggregate_legs(n_legs, run_dir=hn.run_dir),
              'recomputed over the shared legs')
    info = {'paired': True, 'n_legs': len(shared),
            'n_bugs': hr2.n('all')[0],
            'dropped_hr': len(set(r_by) - set(n_by)),
            'dropped_hn': len(set(n_by) - set(r_by))}
    return hr2, hn2, info


# ---------------------------------------------------------------------------
# which key a cell reads
# ---------------------------------------------------------------------------

def rvar_for(gran: str, rvar: str) -> str:
    """The R-variant a cell of granularity `gran` can actually read.

    ``Rbody`` (and any other line-only variant, see
    `metrics.R_VARIANTS_LINE_ONLY`) exists for the LINE-SHAPED
    granularities only — ``line`` and ``branch``, which count the same
    line sets two ways.  The body of a developer-changed method IS that
    method, so at method granularity the variant would be R0 under another
    name and no such key is emitted.  A table fixes one R-variant for all
    of its rows, so the Function row falls back to `DEFAULT_RVAR` and the
    Line and Branch rows keep what was asked for.  The fallback is visible in each row's ``keys`` entry
    and in the table's key note."""
    if (gran not in M.GRANULARITIES_LINE_LIKE
            and rvar in M.R_VARIANTS_LINE_ONLY):
        return DEFAULT_RVAR
    return rvar


def metric_field(metric: str, gran: str, rvar: str = DEFAULT_RVAR,
                 build: str = DEFAULT_BUILD,
                 fkind: str = DEFAULT_F_KIND) -> str:
    """The flat metric field one table cell reads.

    Built with `metrics.metric_key`, so the four/five-slot shapes and the
    ``na`` placeholders are the module that defines them, not a format
    string repeated here.  RCR and CSM take no build and no kind of F; PSC
    has no R-side and so no R-variant.  A line-only R-variant asked for on
    a non-line row falls back to `DEFAULT_RVAR` — see `rvar_for`."""
    m = metric.lower()
    rvar = rvar_for(gran, rvar)
    if m in ('rcr', 'csm'):
        return M.metric_key(m, gran, rvar, None)
    if m == 'psc':
        return M.metric_key(m, gran, None, build, fkind)
    return M.metric_key(m, gran, rvar, build, fkind)


def is_undefined(metric: str, bug_class: str, is_delta: bool = False,
                 gran: Optional[str] = None) -> bool:
    """The cells that are undefined before any data is read: CSM for
    semantic bugs (nothing crashes, so there is no site to match), CSM at
    branch granularity (a crash site is a stack frame, not a branch
    outcome, so `metrics.py` emits no such key), and the RCR difference
    (RCR does not depend on the harness method)."""
    m = metric.lower()
    if m == 'csm' and bug_class == 'semantic':
        return True
    if m == 'csm' and gran == 'branch':
        return True
    if m == 'rcr' and is_delta:
        return True
    return False


# ---------------------------------------------------------------------------
# formatting
# ---------------------------------------------------------------------------

def _num(v: Optional[float], signed: bool = False,
         dp: int = DEFAULT_DP) -> Optional[str]:
    if v is None:
        return None
    return f'{v:+.{dp}f}' if signed else f'{v:.{dp}f}'


def _cell(v: Optional[float], fmt: str, signed: bool = False,
          dp: int = DEFAULT_DP) -> str:
    return _num(v, signed, dp) or _dash(fmt)


def _pm(stat: Optional[dict], fmt: str, signed: bool = False,
        dp: int = DEFAULT_DP) -> str:
    """``mean +/- std`` for one cell, in the rendering `fmt` asks for."""
    if not stat or stat.get('mean') is None:
        return _dash(fmt)
    mean = _num(stat['mean'], signed, dp)
    std = _num(stat.get('std'), dp=dp)
    if std is None:
        return mean
    sep = ' $\\pm$ ' if fmt == 'latex' else ' ± '
    return f'{mean}{sep}{std}'


def _mean_of(arm: Optional[Arm], key: str, bug_class: str) -> Optional[float]:
    if arm is None:
        return None
    stat = arm.stat(key, bug_class)
    return stat.get('mean') if stat else None


def _arm_tex(label: str) -> str:
    """``H_N`` -> ``$H_N$`` for LaTeX."""
    return f'${label}$' if label.startswith('H_') else label


# ---------------------------------------------------------------------------
# the notes every table carries
# ---------------------------------------------------------------------------

def _key_note(fkind: str, build: str, rvar: str) -> str:
    # A line-only variant has no method-level key at all, so its examples
    # are drawn from the Line row instead of the Function row.
    line_only = rvar in M.R_VARIANTS_LINE_ONLY
    gran = 'line' if line_only else 'method'
    example = metric_field('rcc', gran, rvar, build, fkind)
    note = (f'Keys: R-variant {rvar}, build {build} '
            f'({A.BUILD_LABELS.get(build, build)}), F(H) kind {fkind} — '
            f'e.g. {example}. RCR and CSM read no coverage '
            f'({metric_field("rcr", gran, rvar)}); PSC has no R-side '
            f'({metric_field("psc", gran, rvar, build, fkind)}).')
    if line_only:
        note += (f' {rvar} is a line-only region (the whole body of each '
                 f'developer-changed method), so the Line rows read it and '
                 f'the Function rows fall back to {DEFAULT_RVAR} '
                 f'({metric_field("rcc", "method", rvar, build, fkind)}).')
    return note


GRAN_NOTE = ('Granularity: "Function" is the Java method or constructor, '
             '"Line" the source line, "Branch" the branch outcomes on '
             'those same lines (JaCoCo\'s per-line mb/cb). "Branch" '
             'counts the outgoing edges of DECISION POINTS only — a '
             'straight-line statement\'s fallthrough edge is not a JaCoCo '
             'branch and is not represented — so it is the nearest '
             'observable stand-in for the paper\'s edge level, not that '
             'level itself; a full control-flow edge version (ASM plus '
             'JaCoCo probes) is future work.')

BRANCH_CSM_NOTE = ('CSM has no Branch cell: a crash site is a stack frame, '
                   'so it is matched against the root-cause region at '
                   'function and line granularity and there is no branch '
                   'outcome to match.')

RCR_NOTE = ('RCR judges the patch-derived set P, which is built before any '
            'harness exists, so it is identical for the two harness methods '
            'and no H_R - H_N difference is defined.')

CSM_NOTE = ('CSM is defined over observed crash sites, so it applies to '
            'crashing bugs only; a semantic bug is reported by the harness '
            'itself and has no library crash site.')

F1_NOTE = ('F1 is reported once per (harness method, bug class): the '
           'overfitting/correct decision is made from crashes and does not '
           'depend on the granularity coverage is measured at. Positive '
           'case = overfitting patch; the counts come from each leg\'s '
           'outcome (caught/missed/false_alarm/clean).')

ALL_NOTE = ('"All" is computed over the complete set of legs, not as the '
            'mean of the Crashing and Semantic rows. Set metrics are '
            'macro-averaged: the mean over a bug\'s patches first, then the '
            'mean (and standard deviation) over bugs.')


def _n_note(hr: Arm, hn: Optional[Arm], info: dict) -> str:
    parts = []
    for arm in (hn, hr):
        if arm is None:
            continue
        bugs, legs = arm.n('all')
        parts.append(f'{arm.label} = {os.path.basename(arm.run_dir)} '
                     f'({_plural(legs, "leg")} over {_plural(bugs, "bug")}, '
                     f'aggregate {arm.aggregate_source})')
    if hn is None:
        parts.append('H_N = not given (its cells and every difference are '
                     'undefined)')
    elif info.get('paired'):
        parts.append(f'paired on {info["n_legs"]} shared legs over '
                     f'{info["n_bugs"]} bugs; dropped {info["dropped_hr"]} '
                     f'H_R-only and {info["dropped_hn"]} H_N-only legs')
    return 'Runs: ' + '; '.join(parts) + '.'


def _class_n_note(hr: Arm) -> str:
    bits = []
    for cls in BUG_CLASSES:
        bugs, legs = hr.n(cls)
        bits.append(f'{CLASS_LABELS[cls]} n = {_plural(bugs, "bug")} / '
                    f'{_plural(legs, "leg")}')
    return 'Sizes (H_R): ' + '; '.join(bits) + '.'


def _render_notes(notes: Sequence[str], fmt: str) -> str:
    if fmt == 'latex':
        return '\n'.join(f'% {n}' for n in notes)
    return '\n'.join(f'- {n}' for n in notes)


# ---------------------------------------------------------------------------
# Table 3
# ---------------------------------------------------------------------------

def table3_rows(hr: Arm, hn: Optional[Arm] = None,
                fkind: str = DEFAULT_F_KIND, build: str = DEFAULT_BUILD,
                rvar: str = DEFAULT_RVAR) -> List[dict]:
    """Table 3 as data: one dict per (harness block, bug class, granularity).

    ``cells`` holds the five set metrics as floats or None, ``f1`` the
    classification F1 of that (block, bug class) — repeated on both
    granularity rows, which the renderers print once — and ``undefined``
    names the cells the paper defines as undefined whatever the data says."""
    hr_p, hn_p, info = pair(hr, hn)
    blocks = [('H_N', hn_p, False), ('H_R', hr_p, False),
              ('delta', None, True)]
    rows: List[dict] = []
    for block, arm, is_delta in blocks:
        for cls in BUG_CLASSES:
            if is_delta:
                f1_val = None
                if hn_p is not None:
                    a = hr_p.f1(cls)['f1']
                    b = hn_p.f1(cls)['f1']
                    f1_val = (a - b) if (a is not None and b is not None) \
                        else None
            else:
                f1_val = arm.f1(cls)['f1'] if arm is not None else None
            for gran in GRANULARITIES:
                cells = {}
                undefined = {}
                for metric, _label, _uses_f in SET_METRICS:
                    key = metric_field(metric, gran, rvar, build, fkind)
                    if is_undefined(metric, cls, is_delta, gran):
                        cells[metric] = None
                        undefined[metric] = True
                        continue
                    undefined[metric] = False
                    if is_delta:
                        a = _mean_of(hr_p, key, cls)
                        b = _mean_of(hn_p, key, cls)
                        cells[metric] = (a - b) if (a is not None
                                                    and b is not None) else None
                    else:
                        cells[metric] = _mean_of(arm, key, cls)
                rows.append({'block': block, 'bug_class': cls,
                             'granularity': gran,
                             'gran_label': GRAN_LABELS[gran],
                             'is_delta': is_delta, 'cells': cells,
                             'undefined': undefined, 'f1': f1_val,
                             'keys': {m: metric_field(m, gran, rvar, build,
                                                      fkind)
                                      for m, _l, _u in SET_METRICS}})
    rows_info = info
    for row in rows:
        row['pairing'] = rows_info
    return rows


def table3(hr: Arm, hn: Optional[Arm] = None, fmt: str = 'md',
           fkind: str = DEFAULT_F_KIND, build: str = DEFAULT_BUILD,
           rvar: str = DEFAULT_RVAR, dp: int = DEFAULT_DP) -> str:
    """The paper's Table 3, as markdown or as a LaTeX tabular.

    Rows: harness set (H_N, H_R, the difference) x bug class x granularity.
    Columns: RCR, RCC, RCP, PSC, CSM and F1.  F1 belongs to a (harness set,
    bug class), not to a granularity, so it is printed on the Function row
    of each pair and spans the two rows in LaTeX."""
    if fmt not in FORMATS:
        raise ValueError(f'unknown format {fmt!r}; expected one of {FORMATS}')
    rows = table3_rows(hr, hn, fkind=fkind, build=build, rvar=rvar)
    notes = [_n_note(hr, hn, rows[0]['pairing']), _key_note(fkind, build, rvar), GRAN_NOTE,
             BRANCH_CSM_NOTE, ALL_NOTE, RCR_NOTE + ' (a)',
             CSM_NOTE + ' (b)', F1_NOTE]
    render = _table3_latex if fmt == 'latex' else _table3_md
    return render(rows, notes, fkind, dp)


def _t3_cell(row: dict, metric: str, fmt: str, dp: int) -> str:
    if row['undefined'][metric]:
        return _dash(fmt)
    return _cell(row['cells'][metric], fmt, signed=row['is_delta'], dp=dp)


def _table3_md(rows: Sequence[dict], notes: Sequence[str], fkind: str,
               dp: int = DEFAULT_DP) -> str:
    head = ['Harness', 'Bug class', 'Granularity']
    head += [f'{m} ({fkind})' if uses_f else m for m, _l, uses_f in SET_METRICS]
    head += ['F1']
    out = [f'### {TABLE3_TITLE}', '',
           '| ' + ' | '.join(head) + ' |',
           '| ' + ' | '.join('---' for _ in head) + ' |']
    for row in rows:
        cells = [_t3_cell(row, m, 'md', dp) for m, _l, _u in SET_METRICS]
        # F1 is per (harness, bug class): printed once, on the Function row.
        if row['granularity'] == GRANULARITIES[0]:
            f1 = _cell(row['f1'], 'md', signed=row['is_delta'], dp=dp)
        else:
            f1 = ''
        block = row['block'] if row['block'] != 'delta' else 'Δ(H_R − H_N)'
        out.append('| ' + ' | '.join(
            [block, CLASS_LABELS[row['bug_class']], row['gran_label']]
            + cells + [f1]) + ' |')
    out += ['', _render_notes(notes, 'md')]
    return '\n'.join(out) + '\n'


def _table3_latex(rows: Sequence[dict], notes: Sequence[str], fkind: str,
                  dp: int = DEFAULT_DP) -> str:
    cols = 'lll' + 'c' * (len(SET_METRICS) + 1)
    head = ['Harness', 'Bug class', 'Granularity']
    # Every metric but RCR is a property of the harness set H, so it is
    # written as METRIC_g(H); RCR judges P and takes no H.
    head += [f'{m}$_g$' if m == 'RCR' else f'{m}$_g$(H)'
             for m, _l, _uses_f in SET_METRICS]
    head += ['F1 (H)']
    out = [f'% {TABLE3_TITLE}',
           _render_notes(notes, 'latex'),
           '% needs \\usepackage{booktabs} and \\usepackage{multirow}',
           f'\\begin{{tabular}}{{{cols}}}',
           '\\toprule',
           ' & '.join(head) + ' \\\\',
           '\\midrule']
    per_block = len(BUG_CLASSES) * len(GRANULARITIES)
    for i, row in enumerate(rows):
        first_of_block = (i % per_block == 0)
        first_of_class = (row['granularity'] == GRANULARITIES[0])
        if first_of_block and i:
            out.append('\\midrule')
        block = ('\\Delta(H_R - H_N)' if row['block'] == 'delta'
                 else row['block'])
        block_cell = (f'\\multirow{{{per_block}}}{{*}}{{${block}$}}'
                      if first_of_block else '')
        class_cell = (f'\\multirow{{{len(GRANULARITIES)}}}{{*}}{{'
                      f'{CLASS_LABELS[row["bug_class"]]}}}'
                      if first_of_class else '')
        cells = [_t3_cell(row, m, 'latex', dp) for m, _l, _u in SET_METRICS]
        f1 = (f'\\multirow{{{len(GRANULARITIES)}}}{{*}}{{'
              f'{_cell(row["f1"], "latex", signed=row["is_delta"], dp=dp)}}}'
              if first_of_class else '')
        out.append(' & '.join([block_cell, class_cell, row['gran_label']]
                              + cells + [f1]) + ' \\\\')
    out += ['\\bottomrule', '\\end{tabular}']
    return '\n'.join(out) + '\n'


# ---------------------------------------------------------------------------
# Table 4
# ---------------------------------------------------------------------------

#: Table 4's bug kinds — it has no "All" panel for the coverage metrics,
#: only for the classification counts.
TABLE4_KINDS = ('crashing', 'semantic')


def table4_rows(hr: Arm, hn: Optional[Arm] = None,
                fkind: str = DEFAULT_F_KIND, build: str = DEFAULT_BUILD,
                rvar: str = DEFAULT_RVAR) -> dict:
    """Table 4 as data: ``coverage`` (panel a) and ``classification``
    (panel b), plus the pairing info the notes report."""
    hr_p, hn_p, info = pair(hr, hn)
    coverage = []
    for cls in TABLE4_KINDS:
        bugs, legs = hr_p.n(cls)
        block = {'bug_class': cls, 'n_bugs': bugs, 'n_legs': legs,
                 'metrics': []}
        for metric, label, _uses_f in SET_METRICS:
            entry = {'metric': metric, 'label': label,
                     'harness_independent': metric == 'RCR',
                     'has_hn': hn_p is not None,
                     'undefined': is_undefined(metric, cls),
                     # CSM is undefined at branch granularity but defined
                     # at the other two, so the flag is also kept per
                     # granularity and the cell renderer reads that one.
                     'undefined_by_gran': {}, 'by_gran': {}}
            for gran in GRANULARITIES:
                key = metric_field(metric, gran, rvar, build, fkind)
                entry['undefined_by_gran'][gran] = is_undefined(
                    metric, cls, gran=gran)
                entry['by_gran'][gran] = {
                    'key': key,
                    'HN': (hn_p.stat(key, cls) if hn_p else None),
                    'HR': hr_p.stat(key, cls),
                }
            block['metrics'].append(entry)
        coverage.append(block)

    classification = []
    for cls in BUG_CLASSES:
        hr_f1 = hr_p.f1(cls)
        classification.append({
            'bug_class': cls,
            'D_ovf': hr_f1['n_overfitting'], 'D_cor': hr_f1['n_correct'],
            'HR': hr_f1, 'HN': (hn_p.f1(cls) if hn_p else None)})
    return {'coverage': coverage, 'classification': classification,
            'pairing': info}


def table4(hr: Arm, hn: Optional[Arm] = None, fmt: str = 'md',
           fkind: str = DEFAULT_F_KIND, build: str = DEFAULT_BUILD,
           rvar: str = DEFAULT_RVAR, dp: int = DEFAULT_DP) -> str:
    """The paper's Table 4, as markdown or as LaTeX tabulars.

    Panel (a) is the coverage of the root-cause region per bug kind, with
    Function-level and Line-level blocks each split into H_N and H_R and
    cells ``mean +/- std``; RCR is harness-independent and spans the two
    harness columns.  Panel (b) is the classification: how many overfitting
    and correct patches each bug kind holds, then precision, recall and F1
    for each harness set."""
    if fmt not in FORMATS:
        raise ValueError(f'unknown format {fmt!r}; expected one of {FORMATS}')
    data = table4_rows(hr, hn, fkind=fkind, build=build, rvar=rvar)
    notes = [_n_note(hr, hn, data['pairing']), _class_n_note(hr), _key_note(fkind, build,
                                                                rvar),
             GRAN_NOTE, BRANCH_CSM_NOTE, ALL_NOTE,
             'RCR spans the two harness columns: it judges the '
             'patch-derived set, not the harnesses, so one number holds '
             'for both. (a)',
             CSM_NOTE + ' (b)', F1_NOTE]
    split = _split_rcr(data)
    if split:
        notes.append('RCR is shown per arm, not spanned, at '
                     + ', '.join(split) + ': the two runs\' patch-derived '
                     'sets differ there, so there is no single number.')
    render = _table4_latex if fmt == 'latex' else _table4_md
    return render(data, notes, dp)


def _t4_spans(entry: dict, gran: str) -> bool:
    """Does this cell span the two harness columns?

    Only RCR does, and only when it really is one number: it judges the
    patch-derived set P, so the two arms normally agree exactly.  They can
    still disagree — the two runs build P from their own ``context.json``
    — and a spanned cell would then hide one arm's value behind the
    other's, so a disagreement is shown as two cells instead."""
    if not entry['harness_independent'] or entry['undefined']:
        return False
    if entry.get('undefined_by_gran', {}).get(gran):
        return False
    if not entry['has_hn']:
        # No naive run was given at all: its column is a dash, not a copy
        # of the conditioned arm's number.
        return False
    slot = entry['by_gran'][gran]
    hn, hr = slot['HN'], slot['HR']
    if not hn or not hr or hn.get('mean') is None or hr.get('mean') is None:
        return True
    return abs(hn['mean'] - hr['mean']) < 1e-9


def _t4_cells(entry: dict, gran: str, fmt: str,
              dp: int = DEFAULT_DP) -> List[str]:
    """The two harness cells of one metric at one granularity."""
    if entry['undefined'] or entry.get(
            'undefined_by_gran', {}).get(gran):
        return [_dash(fmt), _dash(fmt)]
    slot = entry['by_gran'][gran]
    if _t4_spans(entry, gran):
        # One number, reported for both harness methods.
        value = _pm(slot['HR'] or slot['HN'], fmt, dp=dp)
        return [value, value]
    return [_pm(slot['HN'], fmt, dp=dp), _pm(slot['HR'], fmt, dp=dp)]


def _split_rcr(data: dict) -> List[str]:
    """The (bug class, granularity) pairs where RCR did NOT span."""
    out = []
    for block in data['coverage']:
        for entry in block['metrics']:
            if (not entry['harness_independent'] or entry['undefined']
                    or not entry['has_hn']):
                continue
            for gran in GRANULARITIES:
                if not _t4_spans(entry, gran):
                    out.append(f'{CLASS_LABELS[block["bug_class"]]}/'
                               f'{GRAN_LABELS[gran]}')
    return out


def _table4_md(data: dict, notes: Sequence[str],
               dp: int = DEFAULT_DP) -> str:
    head = ['Metric', 'Function-level H_N', 'Function-level H_R',
            'Line-level H_N', 'Line-level H_R',
            'Branch-level H_N', 'Branch-level H_R']
    out = [f'### {TABLE4_TITLE}', '',
           '**(a) Coverage of the root-cause region**', '',
           '| ' + ' | '.join(head) + ' |',
           '| ' + ' | '.join('---' for _ in head) + ' |']
    for block in data['coverage']:
        label = (f'**{CLASS_LABELS[block["bug_class"]]} bugs '
                 f'(n = {_plural(block["n_bugs"], "bug")}, '
                 f'{_plural(block["n_legs"], "leg")})**')
        out.append('| ' + ' | '.join([label] + [''] * (len(head) - 1)) + ' |')
        for entry in block['metrics']:
            cells = []
            for gran in GRANULARITIES:
                cells += _t4_cells(entry, gran, 'md', dp)
            out.append('| ' + ' | '.join([entry['label']] + cells) + ' |')
    head_b = ['Bug kind', 'D_ovf', 'D_cor', 'H_N P', 'H_N R', 'H_N F1',
              'H_R P', 'H_R R', 'H_R F1']
    out += ['', '**(b) Classification of patches (all granularities)**', '',
            '| ' + ' | '.join(head_b) + ' |',
            '| ' + ' | '.join('---' for _ in head_b) + ' |']
    for row in data['classification']:
        cells = [CLASS_LABELS[row['bug_class']], str(row['D_ovf']),
                 str(row['D_cor'])]
        for arm_key in ('HN', 'HR'):
            stats = row[arm_key]
            for field in ('precision', 'recall', 'f1'):
                cells.append(_cell(stats[field], 'md', dp=dp) if stats
                             else _dash('md'))
        out.append('| ' + ' | '.join(cells) + ' |')
    out += ['', _render_notes(notes, 'md')]
    return '\n'.join(out) + '\n'


def _table4_latex(data: dict, notes: Sequence[str],
                  dp: int = DEFAULT_DP) -> str:
    out = [f'% {TABLE4_TITLE}',
           _render_notes(notes, 'latex'),
           '% needs \\usepackage{booktabs}',
           '\\begin{tabular}{l' + 'c' * (2 * len(GRANULARITIES)) + '}',
           '\\toprule',
           ' & ' + ' & '.join(
               f'\\multicolumn{{2}}{{c}}{{{GRAN_LABELS[g]}-level}}'
               for g in GRANULARITIES) + ' \\\\',
           ' '.join(f'\\cmidrule(lr){{{2 + 2 * i}-{3 + 2 * i}}}'
                    for i in range(len(GRANULARITIES))),
           'Metric & ' + ' & '.join(
               '$H_N$ & $H_R$' for _ in GRANULARITIES) + ' \\\\',
           '\\midrule',
           f'\\multicolumn{{{1 + 2 * len(GRANULARITIES)}}}{{l}}'
           '{\\textit{(a) Coverage of the root-cause region}} \\\\']
    for i, block in enumerate(data['coverage']):
        if i:
            out.append('\\addlinespace')
        out.append(f'\\multicolumn{{{1 + 2 * len(GRANULARITIES)}}}{{l}}{{'
                   f'{CLASS_LABELS[block["bug_class"]]} bugs '
                   f'(n = {_plural(block["n_bugs"], "bug")}, '
                   f'{_plural(block["n_legs"], "leg")})'
                   f'}} \\\\')
        for entry in block['metrics']:
            cells = []
            for gran in GRANULARITIES:
                pair_cells = _t4_cells(entry, gran, 'latex', dp)
                if _t4_spans(entry, gran):
                    cells.append(f'\\multicolumn{{2}}{{c}}{{{pair_cells[0]}}}')
                else:
                    cells.extend(pair_cells)
            out.append('\\quad ' + ' & '.join([entry['label']] + cells)
                       + ' \\\\')
    out += ['\\bottomrule', '\\end{tabular}', '',
            '\\begin{tabular}{lcccccccc}',
            '\\toprule',
            ' & \\multicolumn{2}{c}{Patches} & \\multicolumn{3}{c}{$H_N$} '
            '& \\multicolumn{3}{c}{$H_R$} \\\\',
            '\\cmidrule(lr){2-3} \\cmidrule(lr){4-6} \\cmidrule(lr){7-9}',
            'Bug kind & $D_{ovf}$ & $D_{cor}$ & P & R & F1 & P & R & F1 \\\\',
            '\\midrule',
            '\\multicolumn{9}{l}{\\textit{(b) Classification of patches '
            '(all granularities)}} \\\\']
    for row in data['classification']:
        cells = [CLASS_LABELS[row['bug_class']], str(row['D_ovf']),
                 str(row['D_cor'])]
        for arm_key in ('HN', 'HR'):
            stats = row[arm_key]
            for field in ('precision', 'recall', 'f1'):
                cells.append(_cell(stats[field], 'latex', dp=dp) if stats
                             else _dash('latex'))
        out.append(' & '.join(cells) + ' \\\\')
    out += ['\\bottomrule', '\\end{tabular}']
    return '\n'.join(out) + '\n'


# ---------------------------------------------------------------------------
# command line
# ---------------------------------------------------------------------------

def render_both(hr: Arm, hn: Optional[Arm] = None, fmt: str = 'md',
                fkind: str = DEFAULT_F_KIND, build: str = DEFAULT_BUILD,
                rvar: str = DEFAULT_RVAR, dp: int = DEFAULT_DP) -> str:
    """Table 3 and Table 4, one after the other, in one rendering."""
    opts = dict(fmt=fmt, fkind=fkind, build=build, rvar=rvar, dp=dp)
    return (table3(hr, hn, **opts) + '\n' + table4(hr, hn, **opts))


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog='python -m java.measurements.paper_tables',
        description="Fill the paper's Table 3 and Table 4 from measured "
                    "run directories.")
    p.add_argument('--hr', required=True,
                   help='the root-cause-conditioned run (H_R)')
    p.add_argument('--hn', default=None,
                   help='the naive run (H_N); without it every H_N cell and '
                        'every difference prints an en dash')
    p.add_argument('--fkind', default=DEFAULT_F_KIND, choices=M.F_KINDS,
                   help='which kind of fuzzer-reachable set the RCC/RCP/PSC '
                        'columns are read for (default: %(default)s)')
    p.add_argument('--build', default=DEFAULT_BUILD, choices=M.BUILDS,
                   help='whose coverage those columns read, i.e. which '
                        'harness set (default: %(default)s, the harnesses '
                        'the acceptance gate kept)')
    p.add_argument('--rvar', default=DEFAULT_RVAR, choices=R_VARIANTS,
                   help='the root-cause region variant (default: '
                        '%(default)s, the methods the developer changed). '
                        'Rbody is line-only — every line of the body of '
                        'each changed method — so the Line and Branch rows '
                        'read it and the Function rows fall back to R0')
    p.add_argument('--fmt', default='md', choices=FORMATS,
                   help='markdown for the writeup, latex for the paper')
    p.add_argument('--dp', type=int, default=DEFAULT_DP,
                   help='decimal places in every cell (default: %(default)s; '
                        'RCP can be a few thousandths, so --dp 3 is worth '
                        'having for that column)')
    p.add_argument('--out', default=None,
                   help='write to this file instead of standard output')
    return p


def main(argv: Optional[List[str]] = None) -> int:
    args = build_parser().parse_args(argv)
    hr = load_arm(args.hr, 'HR', fkind=args.fkind)
    hn = load_arm(args.hn, 'HN', fkind=args.fkind) if args.hn else None
    text = render_both(hr, hn, fmt=args.fmt, fkind=args.fkind,
                       build=args.build, rvar=args.rvar, dp=args.dp)
    if args.out:
        with open(args.out, 'w') as fh:
            fh.write(text)
        print(f'wrote {args.out}')
    else:
        sys.stdout.write(text)
    return 0


if __name__ == '__main__':                              # pragma: no cover
    sys.exit(main())
