"""The paper's Table 3 and Table 4, filled from measured runs.

The synthetic pair of runs is the one the aggregate tests use: three legs
over two bugs (Chart-1 crashing, patched twice; Lang-24 semantic, patched
once), in a conditioned arm (H_R) and a naive arm (H_N) whose first leg was
given a patch-derived set that lies outside the root-cause region.  The two
arms carry the SAME outcomes, so every classification number is equal on
both sides and the differences in the set metrics are the interesting part.

Outcomes in that run, and so the classification the tables report:

  01 Chart-1 overfitting, crashed on the patch   -> caught      TP
  02 Chart-1 overfitting, did not crash          -> missed      FN
  03 Lang-24 correct, did not crash              -> clean       TN

so precision 1.00, recall 0.50, F1 0.67 for All and for Crashing, and
nothing at all for Semantic (no overfitting patch, so no positive case).
"""
import json
import os
import sys

import pytest

_TESTS = os.path.dirname(os.path.abspath(__file__))
if _TESTS not in sys.path:
    sys.path.insert(0, _TESTS)

from test_measurements_metrics import build_run, _mset, B_f           # noqa: E402

from java.measurements import locations as loc                        # noqa: E402
from java.measurements import metrics as M                            # noqa: E402
from java.measurements import paper_tables as PT                      # noqa: E402

_REPO = os.path.dirname(_TESTS)
REAL_RUN = os.path.join(_REPO, 'runs-archive', 'runs',
                        'measure_smoke_20260904_075108')


@pytest.fixture()
def hr(tmp_path):
    """H_R: the root-cause-conditioned arm."""
    run = build_run(str(tmp_path / 'run_conditioned'))
    M.write_metrics(run)
    return PT.load_arm(run, 'HR')


@pytest.fixture()
def hn(tmp_path):
    """H_N: the naive arm.  Leg 01's patch-derived set is one method that
    is in no ring of R, and there is one extra leg the conditioned arm does
    not have, which the pairing must drop."""
    run = build_run(str(tmp_path / 'run_naive'),
                    leg1_patch_derived=_mset([(B_f, loc.SEED)]),
                    extra_leg=True)
    M.write_metrics(run)
    return PT.load_arm(run, 'HN')


# ---------------------------------------------------------------------------
# loading an arm
# ---------------------------------------------------------------------------

def test_load_arm_reads_the_legs_and_the_aggregate(hr):
    assert len(hr.legs) == 3
    assert hr.n('all') == (2, 3)             # (bugs, legs)
    assert hr.n('crashing') == (1, 2)
    assert hr.n('semantic') == (1, 1)
    assert hr.label == 'H_R'
    # the synthetic run has no aggregate.json, so it was recomputed
    assert 'recomputed' in hr.aggregate_source
    assert hr.stat('rcr__method__R0__na', 'crashing')['mean'] == 0.25


def test_load_arm_reuses_a_current_aggregate_json(hr):
    """A stored aggregate that covers every key the legs carry is used as
    it stands, and the note says so."""
    from java.measurements import aggregate as A
    path = os.path.join(hr.run_dir, 'aggregate.json')
    with open(path, 'w') as fh:
        json.dump(A.aggregate(hr.run_dir), fh)
    arm = PT.load_arm(hr.run_dir, 'HR')
    assert arm.aggregate_source == 'aggregate.json'
    assert PT.stale_keys(arm.agg, arm.legs) == set()
    assert arm.stat('rcr__method__R0__na', 'crashing')['mean'] == 0.25


def test_a_stale_aggregate_json_is_recomputed_not_believed(hr):
    """An archived aggregate can predate the current key format — the F
    metrics used to have no kind slot, so ``rcc__method__R0__buggy`` sat
    where ``rcc__method__R0__buggy__dyn`` sits now.  Believing such a file
    would print a dash in every RCC / RCP / PSC cell while the numbers were
    in metrics.jsonl all along."""
    from java.measurements import aggregate as A
    agg = A.aggregate(hr.run_dir)
    for entry in agg['by_kind'].values():                # strip the F keys
        entry['metrics'] = {k: v for k, v in entry['metrics'].items()
                            if not k.startswith(('rcc', 'rcp', 'psc'))}
    with open(os.path.join(hr.run_dir, 'aggregate.json'), 'w') as fh:
        json.dump(agg, fh)

    arm = PT.load_arm(hr.run_dir, 'HR')
    assert 'stale' in arm.aggregate_source
    assert arm.stat('rcc__method__R0__buggy__dyn', 'crashing')['mean'] == 0.5
    text = PT.table3(arm)
    assert '| H_R | Crashing | Function | 0.25 | 0.50 | 0.25 | 0.67 | 0.50 ' \
           '| 0.67 |' in text


# ---------------------------------------------------------------------------
# F1 from the legs' outcomes
# ---------------------------------------------------------------------------

def test_f1_comes_from_the_leg_outcomes(hr):
    all_legs = hr.f1('all')
    assert (all_legs['tp'], all_legs['fn'], all_legs['fp'],
            all_legs['tn']) == (1, 1, 0, 1)
    assert all_legs['precision'] == 1.0
    assert all_legs['recall'] == 0.5
    assert all_legs['f1'] == pytest.approx(2 / 3)
    assert (all_legs['n_overfitting'], all_legs['n_correct']) == (2, 1)

    crashing = hr.f1('crashing')
    assert (crashing['tp'], crashing['fn']) == (1, 1)
    assert crashing['f1'] == pytest.approx(2 / 3)

    # no overfitting patch among the semantic bugs: no positive case at all
    semantic = hr.f1('semantic')
    assert (semantic['tp'], semantic['fp'], semantic['fn'],
            semantic['tn']) == (0, 0, 0, 1)
    assert semantic['precision'] is None and semantic['f1'] is None


def test_f1_maps_every_outcome_to_its_cell():
    legs = [{'outcome': 'caught'}, {'outcome': 'missed'},
            {'outcome': 'false_alarm'}, {'outcome': 'clean'},
            {'outcome': None}]
    got = PT.f1_from_legs(legs)
    assert (got['tp'], got['fn'], got['fp'], got['tn']) == (1, 1, 1, 1)
    assert got['n_unlabelled'] == 1
    assert got['precision'] == 0.5 and got['recall'] == 0.5
    assert got['f1'] == 0.5


# ---------------------------------------------------------------------------
# which key a cell reads
# ---------------------------------------------------------------------------

def test_metric_field_uses_the_metrics_key_format():
    assert PT.metric_field('rcr', 'method') == 'rcr__method__R0__na'
    assert PT.metric_field('csm', 'line') == 'csm__line__R0__na'
    assert PT.metric_field('rcc', 'method') == 'rcc__method__R0__buggy__dyn'
    assert PT.metric_field('psc', 'line') == 'psc__line__na__buggy__dyn'
    assert PT.metric_field('rcp', 'line', 'full', 'compiled', 'stat') == \
        'rcp__line__full__compiled__stat'


def test_undefined_cells_are_named_before_any_data_is_read():
    assert PT.is_undefined('CSM', 'semantic') is True
    assert PT.is_undefined('CSM', 'crashing') is False
    assert PT.is_undefined('RCR', 'all', is_delta=True) is True
    assert PT.is_undefined('RCR', 'all', is_delta=False) is False
    assert PT.is_undefined('RCC', 'semantic', is_delta=True) is False


# ---------------------------------------------------------------------------
# Table 3, markdown
# ---------------------------------------------------------------------------

def test_table3_cells(hr, hn):
    text = PT.table3(hr, hn)
    assert ('| Harness | Bug class | Granularity | RCR | RCC (dyn) | '
            'RCP (dyn) | PSC (dyn) | CSM | F1 |') in text
    # H_N's leg 01 sees none of the root cause; H_R's sees half of it
    assert '| H_N | Crashing | Function | 0.00 | 0.50 | 0.25 | 0.00 | 0.50 ' \
           '| 0.67 |' in text
    assert '| H_R | Crashing | Function | 0.25 | 0.50 | 0.25 | 0.67 | 0.50 ' \
           '| 0.67 |' in text
    # "All" is the complete set, not the mean of the two class rows
    assert '| H_R | All | Function | 0.62 | 0.25 | 0.12 | 0.33 | 0.50 ' \
           '| 0.67 |' in text


def test_table3_delta_row_and_the_undefined_rcr_difference(hr, hn):
    text = PT.table3(hr, hn)
    # PSC is the column the two arms really differ in: 0.67 - 0.00
    assert '| Δ(H_R − H_N) | Crashing | Function | – | +0.00 | +0.00 ' \
           '| +0.67 | +0.00 | +0.00 |' in text
    # RCR judges P, not the harnesses: no difference is defined
    for gran in ('Function', 'Line'):
        for cls in ('All', 'Crashing', 'Semantic'):
            row = [l for l in text.splitlines()
                   if l.startswith(f'| Δ(H_R − H_N) | {cls} | {gran} |')]
            assert len(row) == 1
            assert row[0].split('|')[4].strip() == '–'


def test_table3_semantic_csm_is_a_dash(hr, hn):
    text = PT.table3(hr, hn)
    for block in ('H_N', 'H_R', 'Δ(H_R − H_N)'):
        for gran in ('Function', 'Line'):
            row = [l for l in text.splitlines()
                   if l.startswith(f'| {block} | Semantic | {gran} |')][0]
            assert row.split('|')[8].strip() == '–'      # the CSM column


def test_table3_f1_is_printed_once_per_harness_and_bug_class(hr, hn):
    text = PT.table3(hr, hn)
    for block in ('H_N', 'H_R'):
        function = [l for l in text.splitlines()
                    if l.startswith(f'| {block} | Crashing | Function |')][0]
        line = [l for l in text.splitlines()
                if l.startswith(f'| {block} | Crashing | Line |')][0]
        assert function.split('|')[9].strip() == '0.67'
        assert line.split('|')[9].strip() == ''          # spans the two rows


def test_table3_without_a_naive_arm_dashes_every_hn_and_delta_cell(hr):
    text = PT.table3(hr)
    for block in ('H_N', 'Δ(H_R − H_N)'):
        for row in [l for l in text.splitlines()
                    if l.startswith(f'| {block} |')]:
            cells = [c.strip() for c in row.split('|')[4:10] if c.strip()]
            assert set(cells) == {'–'}, row
    assert 'H_N = not given' in text
    # the conditioned arm is still fully rendered
    assert '| H_R | Crashing | Function | 0.25 |' in text


def test_the_fine_granularity_is_line_and_never_edge(hr, hn):
    for fmt in ('md', 'latex'):
        for text in (PT.table3(hr, hn, fmt=fmt), PT.table4(hr, hn, fmt=fmt)):
            assert 'Edge' not in text and 'edge' in text  # only the note
            assert 'Line' in text and 'Function' in text
            assert 'control-flow edge' in text


def test_table3_note_names_the_exact_key_set(hr, hn):
    text = PT.table3(hr, hn, fkind='dyn', build='buggy', rvar='R0')
    assert ('Keys: R-variant R0, build buggy (kept harnesses), F(H) kind '
            'dyn — e.g. rcc__method__R0__buggy__dyn.') in text
    assert 'psc__method__na__buggy__dyn' in text
    assert 'H_R = run_conditioned (3 legs over 2 bugs' in text
    assert 'paired on 3 shared legs over 2 bugs' in text
    other = PT.table3(hr, hn, build='compiled', rvar='full')
    assert 'rcc__method__full__compiled__dyn' in other


def test_table3_reads_the_build_and_rvar_it_is_asked_for(hr):
    """Nothing falls back to another build: a run with no compiled-harness
    coverage prints dashes in the three F columns rather than the kept
    set's numbers wearing the compiled label."""
    text = PT.table3(hr, build='compiled')
    row = [l for l in text.splitlines()
           if l.startswith('| H_R | Crashing | Function |')][0]
    assert row.split('|')[4].strip() == '0.25'           # RCR, no build
    assert [c.strip() for c in row.split('|')[5:8]] == ['–', '–', '–']


# ---------------------------------------------------------------------------
# Table 4, markdown
# ---------------------------------------------------------------------------

def test_table4_panel_a_cells_carry_the_standard_deviation(hr, hn):
    text = PT.table4(hr, hn)
    assert '| **Crashing bugs (n = 1 bug, 2 legs)** |' in text
    assert '| **Semantic bugs (n = 1 bug, 1 leg)** |' in text
    assert ('| Root-cause coverage, RCC | 0.50 ± 0.00 | 0.50 ± 0.00 '
            '| 0.50 ± 0.00 | 0.50 ± 0.00 |') in text
    assert ('| Patch-derived set coverage, PSC | 0.00 ± 0.00 | 0.67 ± 0.00 '
            '| 0.00 ± 0.00 | 0.67 ± 0.00 |') in text
    assert ('| Metric | Function-level H_N | Function-level H_R '
            '| Line-level H_N | Line-level H_R |') in text


def test_table4_rcr_spans_the_two_harness_columns(hr):
    """RCR is harness-independent, so one number holds for both columns.
    The Chart-1 legs of the two synthetic arms have different patch-derived
    sets, so this is checked with one arm, where the H_N column is a dash
    and the spanned value would otherwise be copied into it."""
    rows = PT.table4_rows(hr, hr)                 # the same arm twice
    entry = [e for e in rows['coverage'][0]['metrics']
             if e['metric'] == 'RCR'][0]
    assert PT._t4_spans(entry, 'method') is True
    text = PT.table4(hr, hr)
    assert ('| Root-cause recovery, RCR | 0.25 ± 0.00 | 0.25 ± 0.00 '
            '| 0.25 ± 0.00 | 0.25 ± 0.00 |') in text
    assert 'shown per arm, not spanned' not in text


def test_table4_says_so_when_rcr_does_not_agree_between_the_arms(hr, hn):
    text = PT.table4(hr, hn)
    assert ('| Root-cause recovery, RCR | 0.00 ± 0.00 | 0.25 ± 0.00 '
            '| 0.00 ± 0.00 | 0.25 ± 0.00 |') in text
    assert ('RCR is shown per arm, not spanned, at Crashing/Function, '
            'Crashing/Line') in text


def test_table4_semantic_csm_is_a_dash(hr, hn):
    text = PT.table4(hr, hn)
    semantic = text.split('Semantic bugs')[1]
    assert '| Crash-site match, CSM | – | – | – | – |' in semantic
    # the crashing panel has it
    crashing = text.split('Semantic bugs')[0]
    assert '| Crash-site match, CSM | 0.50 ± 0.00' in crashing


def test_table4_panel_b_counts_and_scores(hr, hn):
    text = PT.table4(hr, hn)
    assert ('| Bug kind | D_ovf | D_cor | H_N P | H_N R | H_N F1 | H_R P '
            '| H_R R | H_R F1 |') in text
    assert '| All | 2 | 1 | 1.00 | 0.50 | 0.67 | 1.00 | 0.50 | 0.67 |' in text
    assert '| Crashing | 2 | 0 | 1.00 | 0.50 | 0.67 | 1.00 | 0.50 | 0.67 |' \
        in text
    # no overfitting patch among the semantic bugs: no P, R or F1 exists
    assert '| Semantic | 0 | 1 | – | – | – | – | – | – |' in text


def test_table4_without_a_naive_arm_dashes_its_columns(hr):
    text = PT.table4(hr)
    assert '| Root-cause coverage, RCC | – | 0.50 ± 0.00 | – | 0.50 ± 0.00 |' \
        in text
    # the missing arm wins over the RCR span: an absent run is not a value
    assert '| Root-cause recovery, RCR | – | 0.25 ± 0.00 | – | 0.25 ± 0.00 |' \
        in text
    assert '| All | 2 | 1 | – | – | – | 1.00 | 0.50 | 0.67 |' in text


# ---------------------------------------------------------------------------
# LaTeX
# ---------------------------------------------------------------------------

def test_table3_latex_structure(hr, hn):
    text = PT.table3(hr, hn, fmt='latex')
    assert '\\begin{tabular}{lllcccccc}' in text
    assert '\\toprule' in text and '\\midrule' in text
    assert '\\bottomrule' in text and '\\end{tabular}' in text
    assert ('Harness & Bug class & Granularity & RCR$_g$ & RCC$_g$(H) & '
            'RCP$_g$(H) & PSC$_g$(H) & CSM$_g$(H) & F1 (H) \\\\') in text
    assert ('\\multirow{6}{*}{$H_R$} & \\multirow{2}{*}{All} & Function '
            '& 0.62 & 0.25 & 0.12 & 0.33 & 0.50 & '
            '\\multirow{2}{*}{0.67} \\\\') in text
    # the difference block: RCR undefined, en dash written the LaTeX way
    assert ('\\multirow{6}{*}{$\\Delta(H_R - H_N)$} & \\multirow{2}{*}{All} '
            '& Function & -- & +0.00 & +0.00 & +0.33 & +0.00 & '
            '\\multirow{2}{*}{+0.00} \\\\') in text
    # every body row ends in a row break and separates cells with &
    body = [l for l in text.splitlines() if l.startswith(('\\multirow', ' &'))]
    assert body and all(l.endswith('\\\\') and ' & ' in l for l in body)
    # the notes survive as LaTeX comments
    assert '% Keys: R-variant R0, build buggy' in text
    assert '% needs \\usepackage{booktabs} and \\usepackage{multirow}' in text


def test_table4_latex_structure(hr, hn):
    text = PT.table4(hr, hn, fmt='latex')
    assert '\\begin{tabular}{lcccc}' in text            # panel (a)
    assert '\\begin{tabular}{lcccccccc}' in text        # panel (b)
    assert (' & \\multicolumn{2}{c}{Function-level} & '
            '\\multicolumn{2}{c}{Line-level} \\\\') in text
    assert 'Metric & $H_N$ & $H_R$ & $H_N$ & $H_R$ \\\\' in text
    assert ('\\quad Root-cause coverage, RCC & 0.50 $\\pm$ 0.00 & '
            '0.50 $\\pm$ 0.00 & 0.50 $\\pm$ 0.00 & 0.50 $\\pm$ 0.00 \\\\') \
        in text
    assert ('Bug kind & $D_{ovf}$ & $D_{cor}$ & P & R & F1 & P & R & F1 \\\\') \
        in text
    assert 'Crashing & 2 & 0 & 1.00 & 0.50 & 0.67 & 1.00 & 0.50 & 0.67 \\\\' \
        in text
    assert 'Semantic & 0 & 1 & -- & -- & -- & -- & -- & -- \\\\' in text
    assert '\\quad Crash-site match, CSM & -- & -- & -- & -- \\\\' in text
    assert 'n = 1 bug, 2 legs' in text


def test_table4_latex_spans_rcr_with_multicolumn(hr):
    text = PT.table4(hr, hr, fmt='latex')
    assert ('\\quad Root-cause recovery, RCR & '
            '\\multicolumn{2}{c}{0.25 $\\pm$ 0.00} & '
            '\\multicolumn{2}{c}{0.25 $\\pm$ 0.00} \\\\') in text


def test_an_unknown_format_is_refused(hr):
    for fn in (PT.table3, PT.table4):
        with pytest.raises(ValueError):
            fn(hr, fmt='html')


def test_decimal_places_are_adjustable(hr):
    """RCP can be a few thousandths, which two decimals round to zero."""
    assert '| H_R | Crashing | Function | 0.250 |' in PT.table3(hr, dp=3)


# ---------------------------------------------------------------------------
# the command line, on the real measured run
# ---------------------------------------------------------------------------

@pytest.mark.skipif(not os.path.isdir(REAL_RUN),
                    reason='the archived measure_smoke run is not here')
@pytest.mark.parametrize('fmt', ['md', 'latex'])
def test_cli_on_the_real_run(tmp_path, fmt):
    """A smoke run of the CLI over the archived measurement run: both
    tables, both renderings, and nothing written into the run directory."""
    before = sorted(os.listdir(REAL_RUN))
    out = tmp_path / f'tables.{fmt}'
    assert PT.main(['--hr', REAL_RUN, '--fmt', fmt, '--out', str(out)]) == 0
    text = out.read_text()

    assert 'Table 3' in text and 'Table 4' in text
    assert 'Edge' not in text
    assert 'measure_smoke_20260904_075108 (7 legs over 5 bugs' in text
    assert 'rcc__method__R0__buggy__dyn' in text
    if fmt == 'md':
        assert '| H_R | Crashing | Function | 1.00 | 1.00 |' in text
        assert '| All | 3 | 4 | – | – | – | 0.50 | 0.67 | 0.57 |' in text
    else:
        assert '\\begin{tabular}' in text and '$\\pm$' in text
    assert sorted(os.listdir(REAL_RUN)) == before


@pytest.mark.skipif(not os.path.isdir(REAL_RUN),
                    reason='the archived measure_smoke run is not here')
def test_cli_rejects_a_run_directory_that_is_not_there(tmp_path):
    with pytest.raises(ValueError):
        PT.load_arm(str(tmp_path / 'nope'), 'HR')
