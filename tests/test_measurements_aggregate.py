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


def test_aggregate_survives_error_rows(tmp_path, conditioned):
    with open(os.path.join(conditioned, 'metrics.jsonl'), 'a') as fh:
        fh.write(json.dumps({'leg': '99_broken_c', 'error': 'boom'}) + '\n')
    agg = A.aggregate(conditioned)
    assert agg['n_errors'] == 1 and agg['n_legs'] == 3 and agg['n_bugs'] == 2
