"""Run-level aggregation: macro-average over bugs, the caught table, the
naive-vs-conditioned delta, and the Table 3 markdown.

The synthetic run is the one built in test_measurements_metrics.py:

  Chart-1 (crashing) has TWO legs — RCR(full, method) 0.5 and 0.0
  Lang-24 (semantic) has ONE leg — RCR(full, method) 1.0

so the per-bug means are 0.25 and 1.0, and the mean over ALL bugs is 0.625.
Averaging the three legs directly would give 0.5, which is the mistake these
tests exist to catch.
"""
import json
import os
import sys

import pytest

_TESTS = os.path.dirname(os.path.abspath(__file__))
if _TESTS not in sys.path:
    sys.path.insert(0, _TESTS)

from test_measurements_metrics import build_run, _mset, A_a, B_c, B_f   # noqa: E402

from java.measurements import aggregate as A                            # noqa: E402
from java.measurements import locations as loc                          # noqa: E402
from java.measurements import metrics as M                              # noqa: E402


@pytest.fixture()
def conditioned(tmp_path):
    """H_R: the run whose harnesses were conditioned on the root cause."""
    run = build_run(str(tmp_path / 'run_conditioned'))
    M.write_metrics(run)
    return run


@pytest.fixture()
def naive(tmp_path):
    """H_N: same bugs and legs, but leg 01's patch-derived set is a single
    method that is in no ring of R.  Plus one leg the conditioned run does
    not have, which the join must drop."""
    run = build_run(str(tmp_path / 'run_naive'),
                    leg1_patch_derived=_mset([(B_f, loc.SEED)]),
                    extra_leg=True)
    M.write_metrics(run)
    return run


# ---------------------------------------------------------------------------
# macro-averaging
# ---------------------------------------------------------------------------

def test_macro_average_is_per_bug_then_per_class(conditioned):
    agg = A.aggregate(conditioned)
    assert agg['n_legs'] == 3 and agg['n_bugs'] == 2

    per_bug = agg['per_bug']
    assert set(per_bug) == {'Chart-1', 'Lang-24'}
    assert per_bug['Chart-1']['n_legs'] == 2
    assert per_bug['Chart-1']['metrics']['rcr__method__full__na'] == 0.25
    assert per_bug['Lang-24']['metrics']['rcr__method__full__na'] == 1.0

    all_kind = agg['by_kind']['all']['metrics']['rcr__method__full__na']
    # 0.625 = mean(0.25, 1.0): the two-leg bug counted ONCE.
    assert all_kind['mean'] == 0.625
    assert all_kind['std'] == pytest.approx(0.375)
    assert all_kind['n_bugs'] == 2


def test_per_bug_kind_counts(conditioned):
    by_kind = A.aggregate(conditioned)['by_kind']
    assert (by_kind['crashing']['n_bugs'], by_kind['crashing']['n_legs']) == (1, 2)
    assert (by_kind['semantic']['n_bugs'], by_kind['semantic']['n_legs']) == (1, 1)
    assert (by_kind['all']['n_bugs'], by_kind['all']['n_legs']) == (2, 3)
    crashing = by_kind['crashing']['metrics']
    assert crashing['rcr__method__full__na']['mean'] == 0.25
    assert crashing['rcr__method__full__na']['std'] == 0.0   # one bug
    # leg 02 has no coverage, so the bug mean uses the one leg that does
    assert crashing['rcc__method__R0__buggy__dyn']['mean'] == 0.5
    assert crashing['psc__method__na__buggy__dyn']['mean'] == pytest.approx(2 / 3)


def test_per_ring_values_are_aggregated_too(conditioned):
    metrics = A.aggregate(conditioned)['by_kind']['crashing']['metrics']
    # leg 01 seed ring 0.5, leg 02 seed ring 0.0 -> bug mean 0.25
    assert metrics['rcr__method__full__na#seed']['mean'] == 0.25
    assert metrics['rcp__method__full__buggy__dyn#outside']['mean'] == 0.25


# ---------------------------------------------------------------------------
# RCC vs caught
# ---------------------------------------------------------------------------

def test_rcc_vs_caught_table(conditioned):
    table = A.aggregate(conditioned)['rcc_vs_caught']
    assert table['rcc_key'] == 'rcc__method__R0__buggy__dyn'
    # only the two overfitting legs; the correct leg is not in this table
    assert table['n_overfitting_legs'] == 2
    assert (table['n_caught'], table['n_missed']) == (1, 1)
    assert table['mean_rcc_caught'] == 0.5
    assert table['mean_rcc_missed'] is None      # that leg has no coverage
    bins = {(b['lo'], b['hi']): b for b in table['bins']}
    assert bins[(0.5, 0.75)]['n'] == 1
    assert bins[(0.5, 0.75)]['caught_rate'] == 1.0
    assert bins[(0.0, 0.25)]['n'] == 0 and bins[(0.0, 0.25)]['caught_rate'] is None
    undefined = [b for b in table['bins'] if b['lo'] is None][0]
    assert undefined['n'] == 1 and undefined['caught'] == 0
    assert 'RCC vs caught' in A.render_rcc_vs_caught(A.aggregate(conditioned))


# ---------------------------------------------------------------------------
# naive vs conditioned
# ---------------------------------------------------------------------------

def test_delta_joins_on_shared_legs(naive, conditioned):
    d = A.delta(naive, conditioned)
    assert d['n_common_legs'] == 3 and d['n_common_bugs'] == 2
    # the naive-only leg is not scored on either side
    assert d['dropped_naive'] == [['Math', '5', 'Jaid', 'correct']]
    assert d['dropped_conditioned'] == []
    assert sorted(d['joined']) == [
        ['Chart', '1', 'Arja', 'overfitting'],
        ['Chart', '1', 'Nopol2015', 'overfitting'],
        ['Lang', '24', 'ACS', 'correct']]
    assert d['naive']['n_legs'] == 3 and d['conditioned']['n_legs'] == 3


def test_delta_is_conditioned_minus_naive(naive, conditioned):
    d = A.delta(naive, conditioned)
    # crashing bug: H_R 0.25, H_N 0.0
    assert d['naive']['by_kind']['crashing']['metrics'][
        'rcr__method__full__na']['mean'] == 0.0
    assert d['delta']['crashing']['rcr__method__full__na'] == 0.25
    # all bugs: H_R 0.625, H_N 0.5
    assert d['delta']['all']['rcr__method__full__na'] == pytest.approx(0.125)
    # coverage-side metrics are untouched by the swap
    assert d['delta']['crashing']['rcc__method__R0__buggy__dyn'] == 0.0
    # undefined on both sides stays undefined
    assert d['delta']['semantic']['rcr__line__R0__na'] is None


# ---------------------------------------------------------------------------
# rendering
# ---------------------------------------------------------------------------

def test_render_markdown_table3_cells(naive, conditioned):
    text = A.render_markdown(A.delta(naive, conditioned))
    # the three F-using columns name the kind of F they were read for
    assert ('| bug class | granularity | run | RCR | RCC (dyn) | RCP (dyn) '
            '| PSC (dyn) | CSM |') in text
    assert '| crashing | method | H_N | 0.000 | 0.500 | 0.250 | 0.000 | 0.500 |' \
        in text
    assert '| crashing | method | H_R | 0.250 | 0.500 | 0.250 | 0.667 | 0.500 |' \
        in text
    assert '| crashing | method | delta | +0.250 | +0.000 | +0.000 | +0.667 ' \
           '| +0.000 |' in text
    # the semantic bug has no root-cause lines and no library crash sites
    assert '| semantic | line | H_R | n/a | n/a | 0.000 | 0.000 | n/a |' in text
    for kind in ('crashing', 'semantic', 'all'):
        for gran in ('method', 'line'):
            for row in ('H_N', 'H_R', 'delta'):
                assert f'| {kind} | {gran} | {row} |' in text
    assert '3 paired legs over 2 bugs' in text


def test_render_markdown_of_a_single_run(conditioned):
    text = A.render_markdown(A.aggregate(conditioned))
    assert '| crashing | method | H_R | 0.250 | 0.500 | 0.250 | 0.667 | 0.500 |' \
        in text
    assert 'H_N' not in text
    assert '3 legs over 2 bugs' in text


def test_render_markdown_adds_a_block_for_the_compiled_set(tmp_path):
    """A run that measured both harness sets renders two tables: the kept
    harnesses (the default, buggy build) and every candidate that
    compiled. Each says which set it is about, and the compiled table
    reads ONLY compiled keys — falling back to another build would put a
    different harness set in the same column."""
    from test_measurements_metrics import _compiled_leg
    _compiled_leg(tmp_path)
    run = str(tmp_path / 'run')
    M.write_metrics(run)
    agg = A.aggregate(run)

    assert A.has_build(agg, 'compiled') is True
    text = A.render_markdown(agg)
    assert text.count('Table 3') == 2
    assert '(kept harnesses, build buggy)' in text
    assert '(all compiled harnesses, build compiled)' in text
    kept, compiled = text.split('Table 3')[1], text.split('Table 3')[2]
    # RCC: half the seed ring for the kept set, all of it for the compiled
    assert '| crashing | method | H_R | 0.500 | 0.500 |' in kept
    assert '| crashing | method | H_R | 0.500 | 1.000 |' in compiled

    # asked for one build, only that block is rendered
    only = A.render_markdown(agg, build='compiled')
    assert only.count('Table 3') == 1
    assert '(all compiled harnesses, build compiled)' in only


def test_render_markdown_stays_one_block_without_a_compiled_set(conditioned):
    """The default is unchanged for every run made before the compiled set
    was collected: one table, and it is the kept harnesses'."""
    agg = A.aggregate(conditioned)
    assert A.has_build(agg, 'compiled') is False
    text = A.render_markdown(agg)
    assert text.count('Table 3') == 1
    assert 'all compiled harnesses' not in text
    assert '| crashing | method | H_R | 0.250 | 0.500 | 0.250 | 0.667 | 0.500 |' \
        in text


def test_table3_columns_for_another_build_does_not_fall_back(conditioned):
    """The buggy column list may fall back to the patched build (a run can
    have only that side). No other build may: its whole point is that it is
    a DIFFERENT harness set."""
    assert A.table3_columns() == A.TABLE3_COLUMNS
    cols = dict((name, tmpl) for name, tmpl, _f in A.table3_columns('compiled'))
    assert cols['RCC'] == ('rcc__{g}__R0__compiled__{f}',)
    assert cols['RCP'] == ('rcp__{g}__R0__compiled__{f}',)
    assert cols['PSC'] == ('psc__{g}__na__compiled__{f}',)
    # RCR and CSM never read F(H), so they are the same in every table
    assert cols['RCR'] == ('rcr__{g}__R0__na',)
    assert cols['CSM'] == ('csm__{g}__R0__na',)


def test_aggregate_survives_error_rows(tmp_path, conditioned):
    with open(os.path.join(conditioned, 'metrics.jsonl'), 'a') as fh:
        fh.write(json.dumps({'leg': '99_broken_c', 'error': 'boom'}) + '\n')
    agg = A.aggregate(conditioned)
    assert agg['n_errors'] == 1 and agg['n_legs'] == 3 and agg['n_bugs'] == 2


# --- the triggering-test gate ---------------------------------------------

def _set_gate(run_dir, leg_name, passed):
    """Rewrite one leg's metrics row with a gate verdict on it."""
    path = os.path.join(run_dir, 'metrics.jsonl')
    rows = [json.loads(line) for line in open(path) if line.strip()]
    for row in rows:
        if row.get('leg') == leg_name:
            row.setdefault('sizes', {})['trigger_gate_passed'] = passed
    with open(path, 'w') as fh:
        for row in rows:
            fh.write(json.dumps(row) + '\n')


def test_gated_only_drops_the_legs_whose_bug_failed_the_gate(conditioned):
    """A bug whose own triggering tests do not reach every method the
    developer fix changed has an R̂ that cannot be trusted, so its RCC
    describes our extraction rather than the harness set."""
    _set_gate(conditioned, '03_patch1-Lang-24-ACS_c', False)
    full = A.aggregate(conditioned)
    gated = A.aggregate(conditioned, gated_only=True)

    assert full['n_legs'] == 3 and full['n_bugs'] == 2
    assert gated['n_legs'] == 2 and gated['n_bugs'] == 1
    assert 'Lang-24' not in gated['per_bug']
    # the count that left is reported either way, so a mean is never
    # printed without it
    assert full['n_gate_failed'] == gated['n_gate_failed'] == 1
    assert full['gated_only'] is False and gated['gated_only'] is True


def test_a_gate_that_was_not_run_is_not_a_failed_gate(conditioned):
    """The gate is slow and off by default, so most runs have no answer.
    Treating "not asked" as "failed" would empty them."""
    _set_gate(conditioned, '01_patch1-Chart-1-Arja_o', True)
    agg = A.aggregate(conditioned, gated_only=True)
    assert agg['n_legs'] == 3 and agg['n_gate_failed'] == 0
    rows = M.read_metrics(conditioned)
    passed = {row['leg']: A.gate_passed(row) for row in rows}
    assert passed['01_patch1-Chart-1-Arja_o'] is True
    assert passed['03_patch1-Lang-24-ACS_c'] is None
