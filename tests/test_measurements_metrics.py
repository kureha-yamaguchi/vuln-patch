"""Per-leg Table 2 metrics, computed from hand-written measurement JSON.

Everything here is built from `java.measurements.locations` types and
written to disk in the layout `cli.py` produces, so the tests never need a
checkout, a build, or the sibling modules that normally produce the files.
The synthetic run is:

  01_patch1-Chart-1-Arja_o        Chart-1, crashing, overfitting, CAUGHT
  02_patch1-Chart-1-Nopol2015_o   Chart-1, crashing, overfitting, MISSED
  03_patch1-Lang-24-ACS_c         Lang-24, semantic, correct

Legs 01 and 02 are the same bug, so a macro-average over bugs must count
them once between them (see test_measurements_aggregate.py).
"""
import json
import os

import pytest

from java.measurements import locations as loc
from java.measurements import metrics as M

# --- the method population the synthetic run talks about -------------------
A_a = loc.MethodRef('org.ex.Alpha', 'a', ())
A_b = loc.MethodRef('org.ex.Alpha', 'b', ('String',))
B_c = loc.MethodRef('org.ex.Beta', 'c', ())
B_d = loc.MethodRef('org.ex.Beta', 'd', ('int',))
B_e = loc.MethodRef('org.ex.Beta', 'e', ())
B_f = loc.MethodRef('org.ex.Beta', 'f', ())
B_g = loc.MethodRef('org.ex.Beta', 'g', ())
G_g = loc.MethodRef('org.ex.Gamma', 'g', ())
G_h = loc.MethodRef('org.ex.Gamma', 'h', ())

A10, A11, A12 = (loc.LineRef('org.ex.Alpha', n) for n in (10, 11, 12))
B20, B21, B40, B99 = (loc.LineRef('org.ex.Beta', n) for n in (20, 21, 40, 99))
G5, G9 = loc.LineRef('org.ex.Gamma', 5), loc.LineRef('org.ex.Gamma', 9)


# ---------------------------------------------------------------------------
# writing the synthetic run
# ---------------------------------------------------------------------------

def _mset(pairs):
    ms = loc.MethodSet()
    for ref, ring in pairs:
        ms.add(ref, ring)
    return ms


def _lset(pairs):
    ls = loc.LineSet()
    for ref, ring in pairs:
        ls.add(ref, ring)
    return ls


def _cov(build, methods, lines, all_methods, covered=0, total=0):
    return {'build': build,
            'methods': [m.to_dict() for m in methods],
            'lines': [l.to_dict() for l in lines],
            'all_methods': [m.to_dict() for m in all_methods],
            'branches_covered': covered, 'branches_total': total}


def _site(build, kind, method=None, line=None, exception='java.lang.Error'):
    """A crash_sites.CrashSite dict.  `line` may be a LineRef or the bare
    int a stack frame carries — crash_sites.py emits the int, and metrics
    must pair it with the top-level class of the site method."""
    return {'build': build, 'site_kind': kind, 'exception': exception,
            'top_library': method.to_dict() if method else None,
            'top_library_line': (line.to_dict() if hasattr(line, 'to_dict')
                                 else line)}


def _write_leg(run_dir, name, result, *, patch_derived=None,
               patch_derived_lines=None, root_cause=None, coverage=None,
               crash_sites=None):
    leg = os.path.join(run_dir, name)
    mdir = os.path.join(leg, 'measurements')
    os.makedirs(mdir, exist_ok=True)
    with open(os.path.join(leg, 'result.jsonl'), 'w') as fh:
        fh.write(json.dumps(result) + '\n')
    with open(os.path.join(leg, 'trace.md'), 'w') as fh:
        fh.write('# trace\n')
    if patch_derived is not None:
        loc.dump(patch_derived, os.path.join(mdir, 'patch_derived.json'))
    if patch_derived_lines is not None:
        loc.dump(patch_derived_lines,
                 os.path.join(mdir, 'patch_derived_lines.json'))
    if root_cause is not None:
        with open(os.path.join(mdir, 'root_cause.json'), 'w') as fh:
            json.dump(root_cause, fh)
    for build, cov in (coverage or {}).items():
        with open(os.path.join(mdir, f'coverage_{build}.json'), 'w') as fh:
            json.dump(cov, fh)
    if crash_sites is not None:
        with open(os.path.join(mdir, 'crash_sites.json'), 'w') as fh:
            json.dump(crash_sites, fh)
    return leg


def chart1_root_cause():
    """R for Chart-1: 2 seeds, 1 caller, 1 callee; the manifest adds one
    frame (Beta.e) that the ringed region never reached."""
    return {
        'project': 'Chart', 'bug_id': '1',
        'methods': _mset([(A_a, loc.SEED), (A_b, loc.SEED),
                          (B_c, loc.CALLER), (B_d, loc.CALLEE)]).to_dict(),
        'lines': _lset([(A10, loc.SEED), (A11, loc.SEED),
                        (B20, loc.CALLER), (B40, loc.CALLEE)]).to_dict(),
        'manifest': _mset([(A_a, loc.SEED), (B_e, loc.CALLER)]).to_dict(),
    }


def build_run(run_dir, leg1_patch_derived=None, extra_leg=False):
    """The three-leg synthetic run.  `leg1_patch_derived` swaps leg 01's P
    so the same builder can make a weaker "naive" run to diff against."""
    os.makedirs(run_dir, exist_ok=True)
    p1 = leg1_patch_derived if leg1_patch_derived is not None else \
        _mset([(A_a, loc.SEED), (B_c, loc.SEED), (B_f, loc.CALLER)])
    p1_lines = _lset([(A10, loc.SEED), (A12, loc.SEED), (B20, loc.CALLER)]) \
        if leg1_patch_derived is None else _lset([(B21, loc.SEED)])

    _write_leg(
        run_dir, '01_patch1-Chart-1-Arja_o',
        {'label': 'overfitting', 'status': 'evaluated', 'bug_kind': 'crashing',
         'project': 'Chart', 'bug_id': '1', 'apr_tool': 'Arja',
         'crashed_on_patch': True},
        patch_derived=p1, patch_derived_lines=p1_lines,
        root_cause=chart1_root_cause(),
        coverage={'buggy': _cov('buggy', [A_a, B_c, B_d, B_g],
                                [A10, B20, B40, B99],
                                [A_a, A_b, B_c, B_d, B_e, B_f, B_g], 5, 10)},
        crash_sites=[
            # deepest library frame: a stack frame carries no parameter
            # types, so this must still match Beta.d(int) in R.
            _site('buggy', 'library', loc.MethodRef('org.ex.Beta', 'd', ()),
                  B40, 'java.lang.NullPointerException'),
            # bare int line, the shape crash_sites.py actually writes
            _site('buggy', 'library', A_a, 10, 'java.lang.IllegalStateException'),
            _site('buggy', 'harness_only', None, None, 'java.lang.AssertionError'),
        ])

    _write_leg(
        run_dir, '02_patch1-Chart-1-Nopol2015_o',
        {'label': 'overfitting', 'status': 'evaluated', 'bug_kind': 'crashing',
         'project': 'Chart', 'bug_id': '1', 'apr_tool': 'Nopol2015',
         'crashed_on_patch': False},
        patch_derived=_mset([(B_f, loc.SEED)]),
        patch_derived_lines=_lset([(B21, loc.SEED)]),
        root_cause=chart1_root_cause(),
        coverage=None, crash_sites=[])

    _write_leg(
        run_dir, '03_patch1-Lang-24-ACS_c',
        {'label': 'correct', 'status': 'evaluated', 'bug_kind': 'semantic',
         'project': 'Lang', 'bug_id': '24', 'apr_tool': 'ACS',
         'crashed_on_patch': False},
        patch_derived=_mset([(G_g, loc.SEED)]),
        patch_derived_lines=_lset([(G5, loc.SEED)]),
        root_cause={'project': 'Lang', 'bug_id': '24',
                    'methods': _mset([(G_g, loc.SEED)]).to_dict(),
                    'lines': _lset([]).to_dict(),
                    'manifest': _mset([]).to_dict()},
        coverage={'buggy': _cov('buggy', [G_h], [G9], [G_g, G_h], 1, 4)},
        crash_sites=[_site('buggy', 'harness_only')])

    if extra_leg:
        _write_leg(
            run_dir, '04_patch1-Math-5-Jaid_c',
            {'label': 'correct', 'status': 'evaluated', 'bug_kind': 'semantic',
             'project': 'Math', 'bug_id': '5', 'apr_tool': 'Jaid',
             'crashed_on_patch': False},
            patch_derived=_mset([(G_g, loc.SEED)]),
            root_cause={'methods': _mset([(G_g, loc.SEED)]).to_dict(),
                        'lines': _lset([]).to_dict(),
                        'manifest': _mset([]).to_dict()})
    return run_dir


@pytest.fixture()
def run_dir(tmp_path):
    return build_run(str(tmp_path / 'run_conditioned'))


def _leg(run_dir, n):
    return os.path.join(run_dir, sorted(os.listdir(run_dir))[n])


def _v(row, key):
    return row[key]['value']


# ---------------------------------------------------------------------------
# tests
# ---------------------------------------------------------------------------

def test_metric_keys_name_the_kind_of_F(run_dir):
    """Every metric built from F(H) says which kind of F it used; the ones
    that never look at F do not, and an unknown kind is refused."""
    row = M.compute_leg(_leg(run_dir, 0))
    ratios = [k for k, v in row.items()
              if isinstance(v, dict) and 'value' in v and 'den' in v]
    f_using = [k for k in ratios if k.split('__')[0] in ('rcc', 'rcp', 'psc')]
    other = [k for k in ratios if k.split('__')[0] not in ('rcc', 'rcp', 'psc')]
    assert f_using and other
    for key in f_using:
        assert len(key.split('__')) == 5, key
        assert key.endswith('__dyn'), key
    for key in other:
        assert len(key.split('__')) == 4, key
        assert not key.endswith(('__dyn', '__stat')), key
    # the cross-table is not a ratio but is keyed the same 4-slot way
    assert len('rcr_cross__method__R0__na'.split('__')) == 4
    assert 'rcr_cross__method__R0__na' in row

    # this leg has no static reach, so every build slot carries 'dyn' alone
    # (F_kind is a LIST per slot: a slot can carry both kinds)
    assert set(map(tuple, row['sizes']['F_kind'].values())) == {('dyn',)}

    assert M.F_KINDS == ('dyn', 'stat')
    assert M.metric_key('rcc', 'method', 'R0', 'buggy') == \
        'rcc__method__R0__buggy__dyn'
    assert M.metric_key('psc', 'line', None, 'patched', 'stat') == \
        'psc__line__na__patched__stat'
    assert M.metric_key('rcr', 'method', 'R0', None) == 'rcr__method__R0__na'
    with pytest.raises(ValueError):
        M.metric_key('rcc', 'method', 'R0', 'buggy', 'guessed')
    with pytest.raises(ValueError):
        M.metric_key('rcr', 'method', 'R0', None, 'dyn')


def test_identity_fields(run_dir):
    row = M.compute_leg(_leg(run_dir, 0))
    assert row['leg'] == '01_patch1-Chart-1-Arja_o'
    assert (row['project'], row['bug_id'], row['apr_tool']) == \
        ('Chart', '1', 'Arja')
    assert row['bug'] == 'Chart-1'
    assert row['label'] == 'overfitting' and row['bug_kind'] == 'crashing'
    assert row['caught'] is True and row['outcome'] == 'caught'
    assert row['arm'] == 'o' and row['leg_index'] == 1
    assert M.compute_leg(_leg(run_dir, 1))['outcome'] == 'missed'
    assert M.compute_leg(_leg(run_dir, 2))['outcome'] == 'clean'


def test_rcr_method_all_variants(run_dir):
    row = M.compute_leg(_leg(run_dir, 0))
    # R0 = {Alpha.a, Alpha.b}; P = {Alpha.a, Beta.c, Beta.f} -> 1 of 2
    r0 = row['rcr__method__R0__na']
    assert (r0['value'], r0['num'], r0['den']) == (0.5, 1, 2)
    # R1 = R0 + manifest frame Beta.e -> 1 of 3
    assert _v(row, 'rcr__method__R1__na') == pytest.approx(1 / 3)
    assert row['rcr__method__R1__na']['den'] == 3
    # full = seed+caller+callee -> {Alpha.a, Beta.c} of 4
    assert _v(row, 'rcr__method__full__na') == 0.5
    assert row['rcr__method__full__na']['num'] == 2


def test_rcr_by_ring(run_dir):
    rings = M.compute_leg(_leg(run_dir, 0))['rcr__method__full__na']['by_ring']
    assert rings['seed'] == {'value': 0.5, 'num': 1, 'den': 2}
    assert rings['caller'] == {'value': 1.0, 'num': 1, 'den': 1}
    assert rings['callee'] == {'value': 0.0, 'num': 0, 'den': 1}


def test_rcc_and_psc_method(run_dir):
    row = M.compute_leg(_leg(run_dir, 0))
    # F = {Alpha.a, Beta.c, Beta.d, Beta.g}
    assert _v(row, 'rcc__method__full__buggy__dyn') == 0.75
    assert row['rcc__method__full__buggy__dyn']['by_ring']['callee']['value'] == 1.0
    assert _v(row, 'rcc__method__R0__buggy__dyn') == 0.5
    assert _v(row, 'rcc__method__R1__buggy__dyn') == pytest.approx(1 / 3)
    # P = 3 methods, 2 of them covered
    assert _v(row, 'psc__method__na__buggy__dyn') == pytest.approx(2 / 3)
    psc_rings = row['psc__method__na__buggy__dyn']['by_ring']
    assert psc_rings['seed']['value'] == 1.0      # Alpha.a + Beta.c
    assert psc_rings['caller']['value'] == 0.0    # Beta.f never covered


def test_rcp_is_a_budget_decomposition_summing_to_one(run_dir):
    row = M.compute_leg(_leg(run_dir, 0))
    rcp = row['rcp__method__full__buggy__dyn']
    assert _v(row, 'rcp__method__full__buggy__dyn') == 0.75
    assert {k: v['value'] for k, v in rcp['by_ring'].items()} == {
        'seed': 0.25, 'caller': 0.25, 'callee': 0.25, 'outside': 0.25}
    # every RCP entry in the run decomposes |F| exactly
    for leg in M.leg_dirs(run_dir):
        r = M.compute_leg(leg)
        for key, val in r.items():
            if key.startswith('rcp__') and val['den']:
                total = sum(v['value'] for v in val['by_ring'].values())
                assert total == pytest.approx(1.0), key


def test_line_granularity(run_dir):
    row = M.compute_leg(_leg(run_dir, 0))
    assert _v(row, 'rcr__line__full__na') == 0.5          # {A10, B20} of 4
    assert _v(row, 'rcr__line__R0__na') == 0.5            # {A10} of {A10,A11}
    assert _v(row, 'rcc__line__full__buggy__dyn') == 0.75
    assert _v(row, 'rcp__line__full__buggy__dyn') == 0.75
    assert _v(row, 'psc__line__na__buggy__dyn') == pytest.approx(2 / 3)
    assert _v(row, 'csm__line__full__na') == 1.0
    assert _v(row, 'csm__line__R0__na') == 0.5


def test_r1_is_method_granularity_only(run_dir):
    row = M.compute_leg(_leg(run_dir, 0))
    assert 'rcr__method__R1__na' in row
    assert 'rcr__line__R1__na' not in row
    assert 'rcc__line__R1__buggy__dyn' not in row


def test_csm_counts_library_sites_only(run_dir):
    row = M.compute_leg(_leg(run_dir, 0))
    # 3 sites, one of them harness-only: the denominator is 2.
    assert row['csm__method__full__na']['den'] == 2
    assert _v(row, 'csm__method__full__na') == 1.0
    assert row['csm__method__full__na']['harness_only'] == 1
    assert row['crash_harness_only'] == 1 and row['crash_total'] == 3
    # Beta.d landed in the callee ring, Alpha.a in the seed ring.
    rings = {k: v['value'] for k, v in
             row['csm__method__full__na']['by_ring'].items()}
    assert rings == {'seed': 0.5, 'caller': 0.0, 'callee': 0.5, 'outside': 0.0}
    # against R0 only Alpha.a counts
    assert _v(row, 'csm__method__R0__na') == 0.5
    assert row['csm__method__R0__na']['by_ring']['outside']['value'] == 0.5


def test_none_when_denominator_is_zero(run_dir):
    no_crashes = M.compute_leg(_leg(run_dir, 1))
    assert no_crashes['csm__method__full__na'] == {
        'value': None, 'num': 0, 'den': 0,
        'by_ring': no_crashes['csm__method__full__na']['by_ring'],
        'harness_only': 0}
    only_harness = M.compute_leg(_leg(run_dir, 2))
    assert _v(only_harness, 'csm__method__R0__na') is None      # 0 library sites
    assert only_harness['crash_harness_only'] == 1
    assert _v(only_harness, 'rcr__line__R0__na') is None         # empty R lines
    assert only_harness['rcr__line__R0__na']['den'] == 0


def test_correct_leg_ratios(run_dir):
    row = M.compute_leg(_leg(run_dir, 2))
    assert _v(row, 'rcr__method__R0__na') == 1.0     # P is exactly the seed
    assert _v(row, 'rcc__method__R0__buggy__dyn') == 0.0  # but nothing ran it
    assert _v(row, 'rcp__method__R0__buggy__dyn') == 0.0
    assert row['rcp__method__R0__buggy__dyn']['by_ring']['outside']['value'] == 1.0
    assert _v(row, 'psc__method__na__buggy__dyn') == 0.0


def test_sizes_and_available_flags(run_dir):
    row = M.compute_leg(_leg(run_dir, 0))
    assert row['available'] == {
        'patch_derived': True, 'patch_derived_lines': True,
        'root_cause': True, 'root_cause_body_lines': False,
        'root_cause_manifest': True,
        'coverage_buggy': True, 'coverage_patched': False,
        'coverage_compiled': False, 'static_kept': False,
        'static_compiled': False, 'crash_sites': True}
    assert row['builds'] == ['buggy']
    s = row['sizes']
    assert s['P_method'] == 3 and s['P_line'] == 3
    assert s['R_method'] == {'R0': 2, 'R1': 3, 'full': 4}
    assert s['R_method_by_ring']['full'] == {'seed': 2, 'caller': 1,
                                             'callee': 1}
    assert s['R_line'] == {'R0': 2, 'full': 4}
    assert s['F_method'] == {'buggy': 4} and s['F_line'] == {'buggy': 4}
    assert s['F_kind'] == {'buggy': ['dyn']}
    assert s['F_all_methods'] == {'buggy': 7}
    assert s['branches'] == {'buggy': {'covered': 5, 'total': 10}}
    assert (s['crash_sites_total'], s['crash_sites_library'],
            s['crash_sites_harness_only']) == (3, 2, 1)

    bare = M.compute_leg(_leg(run_dir, 1))
    assert bare['available']['coverage_buggy'] is False
    assert bare['builds'] == []
    assert not [k for k in bare if k.startswith(('rcc__', 'rcp__', 'psc__'))]


def test_unmatched_methods_are_counted(run_dir):
    # leg 02's P is Beta.f, which is in no ring of R and (no coverage here)
    # in no identity space either — it must be counted, not silently dropped.
    row = M.compute_leg(_leg(run_dir, 1))
    stats = row['matching']['method__full__primary']
    assert stats['space'] == 'R'
    assert stats['p_unmatched'] == 1
    assert _v(row, 'rcr__method__full__na') == 0.0
    # with coverage present the space is the build's all_methods list
    covered = M.compute_leg(_leg(run_dir, 0))
    assert covered['matching']['method__full__buggy']['space'] == \
        'coverage_all_methods'
    assert covered['matching']['method__full__buggy']['p_unmatched'] == 0


def test_write_metrics_one_line_per_leg_and_overwrites(run_dir):
    M.write_metrics(run_dir)
    rows = M.write_metrics(run_dir)          # twice: file must not grow
    path = os.path.join(run_dir, 'metrics.jsonl')
    with open(path) as fh:
        lines = [l for l in fh if l.strip()]
    assert len(lines) == len(rows) == 3
    assert [json.loads(l)['leg'] for l in lines] == [
        '01_patch1-Chart-1-Arja_o', '02_patch1-Chart-1-Nopol2015_o',
        '03_patch1-Lang-24-ACS_c']
    assert M.read_metrics(run_dir)[0]['bug'] == 'Chart-1'


def test_missing_measurements_dir_is_not_an_error(tmp_path):
    leg = os.path.join(str(tmp_path), '01_patch1-Chart-1-Arja_o')
    os.makedirs(leg)
    with open(os.path.join(leg, 'result.jsonl'), 'w') as fh:
        fh.write(json.dumps({'label': 'correct', 'project': 'Chart',
                             'bug_id': '1', 'apr_tool': 'Arja',
                             'bug_kind': 'crashing'}) + '\n')
    row = M.compute_leg(leg)
    assert not any(row['available'].values())
    assert not [k for k in row if '__' in k and k.startswith(
        ('rcr__', 'rcc__', 'rcp__', 'psc__', 'csm__'))]


# ---------------------------------------------------------------------------
# cli smoke test: nothing available, everything still recorded
# ---------------------------------------------------------------------------

def test_cli_main_without_checkouts_still_writes_metrics(tmp_path, monkeypatch,
                                                         capsys):
    from java.measurements import cli

    run = tmp_path / 'raw_run'
    leg = run / '01_patch1-Chart-1-Arja_o'
    leg.mkdir(parents=True)
    (leg / 'trace.md').write_text('# trace\n')
    (leg / 'result.jsonl').write_text(json.dumps(
        {'label': 'overfitting', 'status': 'evaluated', 'bug_kind': 'crashing',
         'project': 'Chart', 'bug_id': '1', 'apr_tool': 'Arja',
         'crashed_on_patch': True}) + '\n')

    def _no_checkout(*a, **k):
        raise RuntimeError('no defects4j here')
    monkeypatch.setattr(cli, 'ensure_buggy_checkout', _no_checkout)
    for name in ('_mod_patch_derived', '_mod_root_cause', '_mod_coverage',
                 '_mod_crash_sites'):
        monkeypatch.setattr(cli, name, lambda: (_ for _ in ()).throw(
            ImportError('module not built yet')))

    assert cli.main([str(run), '--coverage']) == 0
    rows = M.read_metrics(str(run))
    assert len(rows) == 1
    assert rows[0]['available'] == {
        'patch_derived': False, 'patch_derived_lines': False,
        'root_cause': False, 'root_cause_body_lines': False,
        'root_cause_manifest': False,
        'coverage_buggy': False, 'coverage_patched': False,
        'coverage_compiled': False, 'static_kept': False,
        'static_compiled': False, 'crash_sites': False}
    errors = json.loads((leg / 'measurements' / 'errors.json').read_text())
    assert set(errors) == {'checkout', 'patch_derived', 'patch_derived_lines',
                           'root_cause', 'coverage', 'crash_sites'}
    out = capsys.readouterr().out
    assert 'metrics.jsonl: 1 row(s)' in out and 'n/a' in out


# ---------------------------------------------------------------------------
# the third harness set: every candidate that compiled
# ---------------------------------------------------------------------------

def _compiled_leg(tmp_path):
    """One leg measured for both harness sets.

    The KEPT set (build `buggy`) ran only Alpha.a.  The ALL-COMPILED set
    (build `compiled`, the acceptance gate's own run of every candidate)
    additionally ran Alpha.b, Beta.c and Beta.d — the candidates the gate
    then threw away.  Crash sites carry both builds."""
    run = str(tmp_path / 'run')
    return _write_leg(
        run, '01_patch1-Chart-1-Arja_o',
        {'label': 'overfitting', 'status': 'evaluated', 'bug_kind': 'crashing',
         'project': 'Chart', 'bug_id': '1', 'apr_tool': 'Arja',
         'crashed_on_patch': True},
        patch_derived=_mset([(A_a, loc.SEED), (B_c, loc.CALLER)]),
        patch_derived_lines=_lset([(A10, loc.SEED)]),
        root_cause=chart1_root_cause(),
        coverage={
            'buggy': _cov('buggy', [A_a], [A10],
                          [A_a, A_b, B_c, B_d], 2, 10),
            'compiled': _cov('compiled', [A_a, A_b, B_c, B_d],
                             [A10, A11, B20, B40],
                             [A_a, A_b, B_c, B_d], 7, 10),
        },
        crash_sites=[
            # the kept set's crash: inside R0
            _site('buggy', 'library', A_a, 10),
            # the acceptance gate's own crashes, over all compiled
            # candidates: one inside R0, one outside R entirely
            _site('compiled', 'library', A_b, 11),
            _site('compiled', 'library', B_g, 99),
        ])


def test_compiled_build_gets_its_own_f_metrics(tmp_path):
    """RCC/RCP/PSC are emitted for `compiled` exactly as for the other two
    builds — same keys, same fifth slot — so the kept set's number and the
    all-compiled number can never be mistaken for each other."""
    row = M.compute_leg(_compiled_leg(tmp_path))
    assert row['builds'] == ['buggy', 'compiled']
    assert row['available']['coverage_compiled'] is True
    # R0 = {Alpha.a, Alpha.b}: the kept set ran half of it, the full set of
    # compiled candidates ran all of it.  Measuring only the kept set would
    # have credited the acceptance gate's filter with that difference.
    assert _v(row, 'rcc__method__R0__buggy__dyn') == 0.5
    assert _v(row, 'rcc__method__R0__compiled__dyn') == 1.0
    assert _v(row, 'rcp__method__R0__compiled__dyn') == 0.5    # 2 of 4
    assert _v(row, 'psc__method__na__compiled__dyn') == 1.0
    assert _v(row, 'psc__method__na__buggy__dyn') == 0.5
    assert _v(row, 'rcc__line__full__buggy__dyn') == 0.25
    assert _v(row, 'rcc__line__full__compiled__dyn') == 1.0
    for key in ('rcc__method__R0__compiled__dyn',
                'rcp__line__full__compiled__dyn',
                'psc__method__na__compiled__dyn'):
        assert key in row and len(key.split('__')) == 5
    # sizes carry the third build the same way
    s = row['sizes']
    assert s['F_method'] == {'buggy': 1, 'compiled': 4}
    assert s['F_line'] == {'buggy': 1, 'compiled': 4}
    assert s['F_all_methods'] == {'buggy': 4, 'compiled': 4}
    assert s['F_kind'] == {'buggy': ['dyn'], 'compiled': ['dyn']}
    assert s['branches']['compiled'] == {'covered': 7, 'total': 10}
    # RCR and CSM never read F, so they are unchanged and build-free
    assert 'rcr__method__R0__compiled' not in row
    assert _v(row, 'rcr__method__R0__na') == 0.5


def test_csm_ignores_compiled_build_crash_sites(tmp_path):
    """CSM asks where the KEPT harnesses' crashes landed.  The acceptance
    gate crashes every candidate it runs — that is what the gate is — so
    counting those would measure the gate, not the harness set.  They are
    counted where they belong instead."""
    row = M.compute_leg(_compiled_leg(tmp_path))
    csm = row['csm__method__R0__na']
    # denominator is the ONE kept-side library site, not all three
    assert (csm['den'], csm['num'], csm['value']) == (1, 1, 1.0)
    assert csm['harness_only'] == 0
    # counting all three would have given 2/3
    assert row['crash_by_build'] == {'buggy': 1, 'patched': 0, 'compiled': 2}
    assert row['crash_total'] == 3 and row['crash_library'] == 3
    assert row['crash_compiled'] == 2
    assert row['sizes']['crash_sites_compiled'] == 2
    assert row['sizes']['crash_sites_library'] == 3
    assert row['csm__line__R0__na']['den'] == 1


def test_rcr_cross_table(run_dir):
    row = M.compute_leg(_leg(run_dir, 0))
    # recovered: Alpha.a (seed in R, seed in P) and Beta.c (caller in R,
    # seed in P) — the second says our neighbourhood filed a caller of the
    # developer's fix as a patch seed.
    cross = row['rcr_cross__method__full__na']
    assert cross['seed'] == {'seed': 1, 'caller': 0, 'callee': 0}
    assert cross['caller'] == {'seed': 1, 'caller': 0, 'callee': 0}
    assert cross['callee'] == {'seed': 0, 'caller': 0, 'callee': 0}
    assert row['rcr_cross__line__full__na']['caller']['caller'] == 1  # B20
    # counts, not ratios: the aggregator must not try to average them
    from java.measurements import aggregate as A
    assert not [k for k in A.leg_values(row) if k.startswith('rcr_cross')]
