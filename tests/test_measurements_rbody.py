"""The third line-level R-variant, ``Rbody``.

``R0`` at line granularity is the lines the developer's fix touched, and it
answers "did the harnesses execute the fix's own lines".  ``Rbody`` is every
line of the BODY of each method the fix touched, and it answers "how
thoroughly is the fixed method exercised" — the fairer question to ask of a
fuzzer that was never shown the fix.  Both are reported.

Three things are checked here:

  1. `root_cause.compute` fills `RootCause.body_lines` with exactly the span
     of the seed methods and nothing else, and the field survives a
     dict round trip while a `root_cause.json` written before the field
     existed still loads;
  2. `metrics.compute_leg` emits the ``Rbody`` keys with the right ratios on
     a hand-written leg, and emits none of them for a leg whose
     `root_cause.json` predates the field;
  3. `aggregate` and `paper_tables` will take ``Rbody`` — for the Line rows,
     with the Function rows falling back to ``R0``.

Everything runs on synthetic sources and hand-written measurement JSON, so
no Defects4J installation and no checkout is needed.
"""
import difflib
import json
import os

import pytest

from java.measurements import aggregate as A
from java.measurements import locations as loc
from java.measurements import metrics as M
from java.measurements import paper_tables as PT
from java.measurements import root_cause
from java.measurements.locations import LineRef, MethodRef, SEED

WIDGET_REL = 'src/org/example/Widget.java'

# Line numbers matter in this file, so it is written out as it reads:
#   1 package, 3 class, 5 field, 7-10 grow(), 12-15 shrink(), 16 close.
WIDGET_FIXED = """package org.example;

public class Widget {

    private int size;

    public int grow(int by) {
        int result = size + by;
        return result;
    }

    public int shrink(int by) {
        return size - by;
    }
}
"""

# The buggy revision differs on ONE line, inside grow().
WIDGET_BUGGY = WIDGET_FIXED.replace('        int result = size + by;',
                                    '        int result = size + by + 1;')

WIDGET_GROW = MethodRef('org.example.Widget', 'grow', ('int',))

# grow()'s declaration line through its closing brace.
GROW_SPAN = list(range(7, 11))
CHANGED_LINE = 8


# ---------------------------------------------------------------------------
# fixtures: a one-file buggy tree and a src.patch that produces it
# ---------------------------------------------------------------------------

def _unified(from_text, to_text, rel):
    return '\n'.join(difflib.unified_diff(
        from_text.split('\n'), to_text.split('\n'),
        fromfile='a/' + rel, tofile='b/' + rel, lineterm='')) + '\n'


@pytest.fixture
def buggy_dir(tmp_path):
    root = tmp_path / 'buggy'
    path = root / WIDGET_REL
    os.makedirs(str(path.parent), exist_ok=True)
    with open(str(path), 'w') as fh:
        fh.write(WIDGET_BUGGY)
    return str(root)


@pytest.fixture
def d4j_home(tmp_path):
    home = tmp_path / 'd4j'
    patches = home / 'framework' / 'projects' / 'Widgets' / 'patches'
    os.makedirs(str(patches), exist_ok=True)
    with open(os.path.join(str(patches), '7.src.patch'), 'w') as fh:
        fh.write(_unified(WIDGET_FIXED, WIDGET_BUGGY, WIDGET_REL))
    return str(home)


@pytest.fixture(autouse=True)
def no_defects4j(monkeypatch):
    """No test here may shell out to `defects4j`."""
    def _boom(*a, **kw):
        raise AssertionError(f'unexpected subprocess call: {a}')
    monkeypatch.setattr(root_cause, '_run', _boom)
    monkeypatch.setattr(root_cause, 'test_source_dirs', lambda d: [])
    monkeypatch.delenv('D4J_HOME', raising=False)


def _compute(buggy_dir, d4j_home, **kw):
    return root_cause.compute('Widgets', 7, buggy_dir, d4j_home=d4j_home,
                              **kw)


def _lines(line_set):
    return {(r.class_top_fq, r.line) for r in line_set.refs()}


# ---------------------------------------------------------------------------
# 1. root_cause.compute
# ---------------------------------------------------------------------------

def test_body_lines_are_the_seed_method_span_and_nothing_else(buggy_dir,
                                                              d4j_home):
    rc = _compute(buggy_dir, d4j_home)
    assert set(rc.seeds) == {WIDGET_GROW}
    assert _lines(rc.body_lines) == {('org.example.Widget', n)
                                     for n in GROW_SPAN}


def test_body_lines_contain_the_changed_lines_they_are_the_body_of(buggy_dir,
                                                                   d4j_home):
    """R0's lines sit inside Rbody: the fix's own lines are lines of the
    fixed method.  This is what makes the two comparable — a wider
    denominator over the same region, not a different region."""
    rc = _compute(buggy_dir, d4j_home)
    assert _lines(rc.lines) == {('org.example.Widget', CHANGED_LINE)}
    assert _lines(rc.lines) <= _lines(rc.body_lines)
    assert len(rc.body_lines) > len(rc.lines)


def test_body_lines_exclude_the_untouched_method_and_the_class_header(
        buggy_dir, d4j_home):
    rc = _compute(buggy_dir, d4j_home)
    got = _lines(rc.body_lines)
    for n in (1, 3, 5, 12, 13, 14, 15, 16):        # shrink() and the frame
        assert ('org.example.Widget', n) not in got


def test_body_lines_are_tagged_seed(buggy_dir, d4j_home):
    """Rbody is grown from the seeds alone, so every line is a seed line —
    no caller's or callee's body is folded in."""
    rc = _compute(buggy_dir, d4j_home)
    assert set(rc.body_lines.items.values()) == {SEED}


def test_body_lines_survive_a_dict_round_trip(buggy_dir, d4j_home):
    rc = _compute(buggy_dir, d4j_home)
    back = root_cause.RootCause.from_dict(rc.to_dict())
    assert _lines(back.body_lines) == _lines(rc.body_lines)
    assert 'body_lines' in rc.to_dict()


def test_an_old_root_cause_json_without_body_lines_still_loads():
    """A run measured before the variant existed wrote no `body_lines` key.
    Reading it must give an empty set, not an exception."""
    old = {
        'methods': loc.MethodSet().to_dict(),
        'lines': loc.LineSet().to_dict(),
        'manifest': loc.MethodSet().to_dict(),
        'patch_text': '', 'route': 'd4j_src_patch',
    }
    assert 'body_lines' not in old
    back = root_cause.RootCause.from_dict(old)
    assert len(back.body_lines) == 0
    assert back.body_lines.refs() == []


def test_body_lines_are_empty_and_noted_when_the_sources_cannot_be_read(
        buggy_dir, d4j_home, monkeypatch):
    """A soft failure costs the body lines and says so; it does not cost
    R-hat."""
    def _boom(*a, **kw):
        raise RuntimeError('no sources')
    monkeypatch.setattr('java.measurements.patch_derived.lines_for', _boom)
    rc = _compute(buggy_dir, d4j_home)
    assert len(rc.body_lines) == 0
    assert any('body_lines' in n for n in rc.notes)
    assert set(rc.seeds) == {WIDGET_GROW}          # R-hat itself is intact


# ---------------------------------------------------------------------------
# 2. metrics: the Rbody keys on a synthetic leg
# ---------------------------------------------------------------------------

ALPHA = 'org.ex.Alpha'
BETA = 'org.ex.Beta'

#: The fixed method's body: 10 lines, of which the fix changed two.
BODY = [LineRef(ALPHA, n) for n in range(10, 20)]
CHANGED = [LineRef(ALPHA, 12), LineRef(ALPHA, 13)]
#: Four of the ten body lines are executed, plus one line outside it.
COVERED = [LineRef(ALPHA, 10), LineRef(ALPHA, 12), LineRef(ALPHA, 13),
           LineRef(ALPHA, 19), LineRef(BETA, 40)]
#: Two of the ten are in the patch-derived set.
P_LINES = [LineRef(ALPHA, 10), LineRef(ALPHA, 11), LineRef(ALPHA, 99)]

A_a = MethodRef(ALPHA, 'a', ())


def _lset(refs, ring=SEED):
    ls = loc.LineSet()
    for ref in refs:
        ls.add(ref, ring)
    return ls


def _mset(refs, ring=SEED):
    ms = loc.MethodSet()
    for ref in refs:
        ms.add(ref, ring)
    return ms


def _write_leg(run_dir, name, *, body_lines=True):
    leg = os.path.join(run_dir, name)
    mdir = os.path.join(leg, 'measurements')
    os.makedirs(mdir, exist_ok=True)
    with open(os.path.join(leg, 'result.jsonl'), 'w') as fh:
        fh.write(json.dumps(
            {'label': 'overfitting', 'status': 'evaluated',
             'bug_kind': 'crashing', 'project': 'Chart', 'bug_id': '1',
             'apr_tool': 'Arja', 'crashed_on_patch': True}) + '\n')

    rc = {'methods': _mset([A_a]).to_dict(),
          'lines': _lset(CHANGED).to_dict(),
          'manifest': loc.MethodSet().to_dict()}
    if body_lines:
        rc['body_lines'] = _lset(BODY).to_dict()
    with open(os.path.join(mdir, 'root_cause.json'), 'w') as fh:
        json.dump(rc, fh)

    loc.dump(_mset([A_a]), os.path.join(mdir, 'patch_derived.json'))
    loc.dump(_lset(P_LINES), os.path.join(mdir, 'patch_derived_lines.json'))
    with open(os.path.join(mdir, 'coverage_buggy.json'), 'w') as fh:
        json.dump({'build': 'buggy',
                   'methods': [A_a.to_dict()],
                   'lines': [r.to_dict() for r in COVERED],
                   'all_methods': [A_a.to_dict()],
                   'branches_covered': 1, 'branches_total': 2}, fh)
    with open(os.path.join(mdir, 'crash_sites.json'), 'w') as fh:
        json.dump([
            # inside the body of the fixed method
            {'build': 'buggy', 'site_kind': 'library',
             'exception': 'java.lang.NullPointerException',
             'top_library': A_a.to_dict(),
             'top_library_line': LineRef(ALPHA, 15).to_dict()},
            # outside it
            {'build': 'buggy', 'site_kind': 'library',
             'exception': 'java.lang.IllegalStateException',
             'top_library': A_a.to_dict(),
             'top_library_line': LineRef(BETA, 40).to_dict()},
        ], fh)
    return leg


@pytest.fixture
def leg(tmp_path):
    return _write_leg(str(tmp_path / 'run'), '01_patch1-Chart-1-Arja_o')


@pytest.fixture
def old_leg(tmp_path):
    return _write_leg(str(tmp_path / 'old'), '01_patch1-Chart-1-Arja_o',
                      body_lines=False)


def test_rcc_line_rbody_is_the_covered_share_of_the_body(leg):
    row = M.compute_leg(leg)
    rcc = row['rcc__line__Rbody__buggy__dyn']
    assert (rcc['num'], rcc['den']) == (4, 10)
    assert rcc['value'] == pytest.approx(0.4)


def test_rcp_line_rbody_is_the_body_share_of_what_ran(leg):
    row = M.compute_leg(leg)
    rcp = row['rcp__line__Rbody__buggy__dyn']
    assert (rcp['num'], rcp['den']) == (4, 5)
    assert rcp['value'] == pytest.approx(0.8)


def test_rcr_line_rbody_is_measured_against_the_patch_derived_lines(leg):
    row = M.compute_leg(leg)
    rcr = row['rcr__line__Rbody__na']
    assert (rcr['num'], rcr['den']) == (2, 10)
    assert rcr['value'] == pytest.approx(0.2)


def test_csm_line_rbody_counts_a_site_inside_the_body(leg):
    """A crash on a body line the fix did not itself touch still lands in
    the fixed method, so Rbody counts it where R0 would not."""
    row = M.compute_leg(leg)
    csm = row['csm__line__Rbody__na']
    assert (csm['num'], csm['den']) == (1, 2)
    assert csm['value'] == pytest.approx(0.5)
    assert row['csm__line__R0__na']['num'] == 0


def test_rbody_size_is_reported_next_to_the_other_two(leg):
    row = M.compute_leg(leg)
    assert row['sizes']['R_line']['Rbody'] == 10
    assert row['sizes']['R_line']['R0'] == 2
    assert row['sizes']['R_line_by_ring']['Rbody'][loc.SEED] == 10
    assert row['available']['root_cause_body_lines'] is True


def test_rbody_is_line_granularity_only(leg):
    row = M.compute_leg(leg)
    assert not [k for k in row if k.startswith('rcc__method__Rbody')]
    assert 'Rbody' not in row['sizes']['R_method']
    assert 'Rbody' in M.R_VARIANTS_LINE
    assert 'Rbody' not in M.R_VARIANTS
    assert M.R_VARIANTS_LINE_ONLY == ('Rbody',)


def test_a_leg_written_before_the_variant_emits_no_rbody_keys(old_leg):
    """An absent `body_lines` is "this run predates the variant", which must
    not be reported as a region of size zero."""
    row = M.compute_leg(old_leg)
    assert not [k for k in row if '__Rbody__' in k]
    assert 'Rbody' not in row['sizes']['R_line']
    assert row['available']['root_cause_body_lines'] is False
    # The other two variants are untouched.
    assert row['rcc__line__R0__buggy__dyn']['den'] == 2


# ---------------------------------------------------------------------------
# 3. aggregate and paper_tables
# ---------------------------------------------------------------------------

def test_aggregate_takes_an_rbody_key_apart_like_any_other(leg):
    row = M.compute_leg(leg)
    vals = A.leg_values(row)
    assert vals['rcc__line__Rbody__buggy__dyn'] == pytest.approx(0.4)
    parts = A.split_key('rcc__line__Rbody__buggy__dyn')
    assert parts['granularity'] == 'line' and parts['rvar'] == 'Rbody'
    assert parts['f_kind'] == 'dyn'


def test_aggregate_averages_the_rbody_keys(leg):
    agg = A.aggregate_legs([M.compute_leg(leg)])
    crashing = agg['by_kind']['crashing']['metrics']
    assert crashing['rcc__line__Rbody__buggy__dyn']['mean'] == \
        pytest.approx(0.4)


def test_paper_tables_reads_rbody_on_the_line_rows(leg):
    assert PT.metric_field('rcc', 'line', 'Rbody') == \
        'rcc__line__Rbody__buggy__dyn'
    assert PT.metric_field('csm', 'line', 'Rbody') == 'csm__line__Rbody__na'


def test_paper_tables_falls_back_to_r0_on_the_function_rows():
    """One table fixes one R-variant for both its rows, and no method-level
    Rbody key exists, so the Function row reads R0."""
    assert PT.metric_field('rcc', 'method', 'Rbody') == \
        'rcc__method__R0__buggy__dyn'
    assert PT.rvar_for('method', 'Rbody') == 'R0'
    assert PT.rvar_for('line', 'Rbody') == 'Rbody'
    assert PT.rvar_for('method', 'full') == 'full'


def test_rvar_option_accepts_rbody():
    assert 'Rbody' in PT.R_VARIANTS
    args = PT.build_parser().parse_args(['--hr', 'x', '--rvar', 'Rbody'])
    assert args.rvar == 'Rbody'


def test_rendered_tables_say_which_rows_read_rbody(tmp_path, leg):
    run_dir = os.path.dirname(leg)
    arm = PT.load_arm(run_dir, 'HR')
    text = PT.table3(arm, rvar='Rbody')
    assert 'rcc__line__Rbody__buggy__dyn' in text
    assert 'Function rows fall back to R0' in text


# ---------------------------------------------------------------------------
# the README has to carry the rationale, not just the code
# ---------------------------------------------------------------------------

def test_readme_documents_the_variant():
    path = os.path.join(os.path.dirname(os.path.dirname(
        os.path.abspath(__file__))), 'src', 'java', 'measurements',
        'README.md')
    with open(path) as fh:
        text = fh.read()
    assert 'Rbody' in text
    assert 'body_lines' in text
    assert 'rcc__line__Rbody__buggy__dyn' in text
