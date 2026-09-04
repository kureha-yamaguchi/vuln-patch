"""The third granularity: BRANCH — branch outcomes on a set of lines.

JaCoCo reports, on every source line, how many branch outcomes that line's
decision point has and how many were taken (`<line ... mb=".." cb=".."/>`).
`coverage.Coverage.line_branches` keeps that pair per line, and `metrics.py`
re-weights the SAME line sets by it:

    total(L) = sum of (mb + cb) over L        taken(L) = sum of cb over L

    RCC_branch = taken(R̂ lines) / total(R̂ lines)
    PSC_branch = taken(P  lines) / total(P  lines)
    RCP_branch = taken(R̂ lines) / taken(all lines)
    RCR_branch = total(R̂ ∩ P lines) / total(R̂ lines)

There is no CSM at branch level: a crash site is a stack frame, not a
branch outcome.

The counts are per line, not per branch identity, so two harnesses' branch
coverage cannot be unioned exactly from two per-harness reports.  These
tests pin both routes: the MERGED JaCoCo report (all of a build's .exec
files in one `report` call) that `collect_leg` prefers, and the
`union-upper-bound` fallback it records when no merged report can be made.
"""
import json
import os
import sys

import pytest

_TESTS = os.path.dirname(os.path.abspath(__file__))
if _TESTS not in sys.path:
    sys.path.insert(0, _TESTS)

from test_measurements_metrics import _lset, _write_leg      # noqa: E402

from java.measurements import aggregate as A                 # noqa: E402
from java.measurements import coverage as cov_mod            # noqa: E402
from java.measurements import locations as loc               # noqa: E402
from java.measurements import metrics as M                   # noqa: E402
from java.measurements import paper_tables as PT             # noqa: E402
from java.measurements.coverage import Coverage              # noqa: E402
from java.measurements.locations import LineRef              # noqa: E402

FIXTURES = os.path.join(_TESTS, 'fixtures', 'measurements')
JACOCO_XML = os.path.join(FIXTURES, 'jacoco_example.xml')

W20 = LineRef('org.jfree.demo.Widget', 20)
W40 = LineRef('org.jfree.demo.Widget', 40)
W60 = LineRef('org.jfree.demo.Widget', 60)


# ---------------------------------------------------------------------------
# parsing mb / cb
# ---------------------------------------------------------------------------

def test_parse_reads_the_per_line_branch_pair():
    """(taken, present) per line, straight off `cb` and `mb + cb`.

    In the fixture: line 20 is 3 of 4, line 40 is 0 of 2 (a method that was
    never executed), line 60 is 2 of 2.  Lines 10 and 7 have mb = cb = 0 —
    no decision point — and are absent rather than stored as (0, 0), so a
    branch denominator is never inflated by straight-line code."""
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    assert cov.line_branches == {W20: (3, 4), W40: (0, 2), W60: (2, 2)}
    assert LineRef('org.jfree.demo.Widget', 10) not in cov.line_branches
    assert LineRef('org.other.Thing', 7) not in cov.line_branches


def test_the_harness_class_contributes_no_branches():
    """FuzzHarness.java's line 5 has mb=1 cb=1 and must not appear: the
    harness is not the library under test."""
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    assert not any(r.class_top_fq.endswith('FuzzHarness')
                   for r in cov.line_branches)


def test_the_per_line_totals_agree_with_the_method_counters():
    """The same outcomes grouped two ways: the whole-build BRANCH counters
    read off the methods, and the per-line pairs read off the sourcefiles."""
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    taken = sum(c for c, _t in cov.line_branches.values())
    total = sum(t for _c, t in cov.line_branches.values())
    assert (taken, total) == (cov.branches_covered, cov.branches_total)


def test_include_prefix_filters_branches_too():
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML, include_prefix='org.other')
    assert cov.line_branches == {}


# ---------------------------------------------------------------------------
# round-tripping
# ---------------------------------------------------------------------------

def test_line_branches_survive_to_dict_and_back():
    cov = cov_mod.parse_jacoco_xml(JACOCO_XML)
    cov.branches_from = cov_mod.BRANCHES_MERGED
    back = Coverage.from_dict(json.loads(json.dumps(cov.to_dict())))
    assert back.line_branches == cov.line_branches
    assert back.branches_from == cov_mod.BRANCHES_MERGED


def test_a_coverage_file_written_before_branches_reads_back_empty():
    """Backward compatibility: no `line_branches`, no `branches_from`."""
    old = {'build': 'buggy', 'methods': [], 'lines': [],
           'all_methods': [], 'branches_covered': 4, 'branches_total': 9}
    back = Coverage.from_dict(old)
    assert back.line_branches == {} and back.branches_from == ''
    assert (back.branches_covered, back.branches_total) == (4, 9)


# ---------------------------------------------------------------------------
# union: the upper bound, and why it is one
# ---------------------------------------------------------------------------

def test_union_of_line_branches_is_a_capped_sum_upper_bound():
    """Two harnesses each taking ONE outcome of a two-way line look exactly
    like the two of them taking both — the XML gives counts, not branch
    identities — so the union sums and caps at the line's total, an upper
    bound, and says so in `branches_from`."""
    a = Coverage(line_branches={W20: (1, 4), W60: (1, 2)})
    b = Coverage(line_branches={W20: (2, 4), W60: (1, 2)})
    u = cov_mod.union([a, b])
    assert u.line_branches[W20] == (3, 4)       # 1 + 2, under the cap
    assert u.line_branches[W60] == (2, 2)       # 1 + 1 capped at 2
    assert u.branches_from == cov_mod.BRANCHES_UNION


def test_union_without_branch_data_records_no_source():
    u = cov_mod.union([Coverage(), Coverage()])
    assert u.line_branches == {} and u.branches_from == ''


# ---------------------------------------------------------------------------
# collect_leg: the merged report, and the fallback
# ---------------------------------------------------------------------------

class _FakeProc:
    def __init__(self, returncode=0, stderr=''):
        self.returncode = returncode
        self.stdout = ''
        self.stderr = stderr


@pytest.fixture
def fake_java(monkeypatch):
    """Replace the one subprocess call with a recorder that writes the
    fixture XML where the real JaCoCo CLI would."""
    calls = []

    def _run(cmd):
        calls.append(list(cmd))
        out = cmd[cmd.index('--xml') + 1]
        with open(JACOCO_XML) as src, open(out, 'w') as dst:
            dst.write(src.read())
        return _FakeProc()

    monkeypatch.setattr(cov_mod, '_run', _run)
    return calls


def _leg_with(tmp_path, names):
    leg = tmp_path / 'leg'
    cov_dir = leg / 'cov'
    cov_dir.mkdir(parents=True)
    for name in names:
        if name.endswith('.xml'):
            with open(JACOCO_XML) as src:
                (cov_dir / name).write_text(src.read())
        else:
            (cov_dir / name).write_text('')
    (cov_dir / 'classpath.json').write_text(json.dumps(
        {'class_dirs': ['/build/classes'],
         'source_dirs': ['/src/main/java'], 'include_glob': 'org.jfree.**'}))
    return leg, cov_dir


def test_several_execs_are_merged_into_one_report(fake_java, tmp_path):
    """The whole per-build Coverage comes from `merged_<build>.xml`, so its
    branch counts are the harness SET's and exact."""
    leg, cov_dir = _leg_with(tmp_path, ['attempt_001_buggy.exec',
                                        'attempt_002_buggy.exec'])
    out = cov_mod.collect_leg(str(leg))

    merged = [c for c in fake_java
              if c[c.index('--xml') + 1].endswith('merged_buggy.xml')]
    assert len(merged) == 1
    assert sorted(a for a in merged[0] if a.endswith('.exec')) == [
        str(cov_dir / 'attempt_001_buggy.exec'),
        str(cov_dir / 'attempt_002_buggy.exec')]
    assert out['buggy'].branches_from == cov_mod.BRANCHES_MERGED
    # the merged report is what the numbers came from, but the object still
    # names the harness set it is over
    assert out['buggy'].harness == 'attempt_001+attempt_002'
    assert out['buggy'].line_branches == {W20: (3, 4), W40: (0, 2),
                                          W60: (2, 2)}


def test_a_merged_report_is_reused_when_it_is_already_on_disk(fake_java,
                                                              tmp_path):
    leg, cov_dir = _leg_with(tmp_path, ['attempt_001_buggy.exec',
                                        'attempt_002_buggy.exec',
                                        'attempt_001_buggy.xml',
                                        'attempt_002_buggy.xml',
                                        'merged_buggy.xml'])
    out = cov_mod.collect_leg(str(leg))
    assert fake_java == []                     # nothing was run again
    assert out['buggy'].branches_from == cov_mod.BRANCHES_MERGED


def test_one_exec_needs_no_second_report(fake_java, tmp_path):
    """A build with a single dump: its per-harness report already IS the
    merged one, so no `merged_<build>.xml` is written."""
    leg, cov_dir = _leg_with(tmp_path, ['attempt_001_buggy.exec'])
    out = cov_mod.collect_leg(str(leg))
    assert len(fake_java) == 1
    assert not (cov_dir / 'merged_buggy.xml').exists()
    assert out['buggy'].branches_from == cov_mod.BRANCHES_MERGED


def test_no_exec_files_falls_back_to_the_per_harness_xmls(fake_java,
                                                          tmp_path):
    """An archive that kept the XML reports but not the execution data.
    The leg is still measured — from the reports — and the branch counts
    are flagged as the union upper bound, because nothing can be merged."""
    leg, cov_dir = _leg_with(tmp_path, ['attempt_001_buggy.xml',
                                        'attempt_002_buggy.xml'])
    out = cov_mod.collect_leg(str(leg))
    assert fake_java == []
    assert sorted(out) == ['buggy']
    assert out['buggy'].harness == 'attempt_001+attempt_002'
    assert out['buggy'].branches_from == cov_mod.BRANCHES_UNION
    # both reports are the same fixture, so every count doubled and was
    # then capped at the line's own total
    assert out['buggy'].line_branches == {W20: (4, 4), W40: (0, 2),
                                          W60: (2, 2)}
    written = json.loads(
        (leg / 'measurements' / 'coverage_buggy.json').read_text())
    assert written['branches_from'] == cov_mod.BRANCHES_UNION


def test_a_failing_merge_falls_back_instead_of_raising(monkeypatch,
                                                       tmp_path):
    """A merged report is an improvement on the union, not a precondition
    for measuring the leg."""
    leg, cov_dir = _leg_with(tmp_path, ['attempt_001_buggy.exec',
                                        'attempt_002_buggy.exec',
                                        'attempt_001_buggy.xml',
                                        'attempt_002_buggy.xml'])
    monkeypatch.setattr(cov_mod, '_run',
                        lambda cmd: _FakeProc(1, 'no such jar'))
    out = cov_mod.collect_leg(str(leg))
    assert out['buggy'].branches_from == cov_mod.BRANCHES_UNION


# ---------------------------------------------------------------------------
# the metrics
# ---------------------------------------------------------------------------

A10, A11, A20, A30, A40, A50 = (
    loc.LineRef('org.ex.Alpha', n) for n in (10, 11, 20, 30, 40, 50))

#: (taken, total) per line of the synthetic leg's coverage.
#:
#:   R̂ (R0)  = A10 (2 of 2) + A20 (2 of 4)          -> 4 of 6
#:   R̂ (full) adds the caller line A30 (1 of 2)      -> 5 of 8
#:   P        = A20 (2 of 4) + A50 (0 of 2)          -> 2 of 6
#:   taken anywhere = 2 + 2 + 1 + 3 + 0              -> 8
LINE_BRANCHES = {A10: (2, 2), A20: (2, 4), A30: (1, 2),
                 A40: (3, 4), A50: (0, 2)}


def _cov_with_branches(build='buggy', source=cov_mod.BRANCHES_MERGED):
    return {'build': build, 'methods': [], 'all_methods': [],
            'lines': [l.to_dict() for l in (A10, A20, A30, A40)],
            'branches_covered': 8, 'branches_total': 14,
            'branches_from': source,
            'line_branches': [dict(r.to_dict(), covered=c, total=t)
                              for r, (c, t) in LINE_BRANCHES.items()]}


def _branch_leg(run_dir, name='01_patch1-Alpha-1-Arja_o', **kw):
    return _write_leg(
        run_dir, name,
        {'label': 'overfitting', 'status': 'evaluated', 'bug_kind': 'crashing',
         'project': 'Alpha', 'bug_id': '1', 'apr_tool': 'Arja',
         'crashed_on_patch': True},
        patch_derived_lines=_lset([(A20, loc.SEED), (A50, loc.SEED)]),
        root_cause={
            'project': 'Alpha', 'bug_id': '1',
            'lines': _lset([(A10, loc.SEED), (A20, loc.SEED),
                            (A30, loc.CALLER)]).to_dict(),
            'body_lines': _lset([(A10, loc.SEED), (A11, loc.SEED),
                                 (A20, loc.SEED)]).to_dict(),
        },
        coverage={'buggy': _cov_with_branches()},
        crash_sites=[{'build': 'buggy', 'site_kind': 'library',
                      'exception': 'java.lang.NullPointerException',
                      'top_library': None, 'top_library_line': 20}],
        **kw)


@pytest.fixture()
def row(tmp_path):
    leg = _branch_leg(str(tmp_path / 'run'))
    return M.compute_leg(leg)


def _v(row, key):
    return row[key]['value']


def test_rcc_branch_is_taken_over_total_on_the_region(row):
    assert _v(row, 'rcc__branch__R0__buggy__dyn') == pytest.approx(4 / 6)
    assert row['rcc__branch__R0__buggy__dyn']['num'] == 4
    assert row['rcc__branch__R0__buggy__dyn']['den'] == 6
    assert _v(row, 'rcc__branch__full__buggy__dyn') == 0.625     # 5 of 8


def test_rcc_branch_by_ring_denominators_add_up(row):
    rings = row['rcc__branch__full__buggy__dyn']['by_ring']
    assert (rings['seed']['num'], rings['seed']['den']) == (4, 6)
    assert (rings['caller']['num'], rings['caller']['den']) == (1, 2)
    # no callee line at all: an empty denominator is null, not 0.0
    assert rings['callee'] == {'value': None, 'num': 0, 'den': 0}
    assert (rings['seed']['den'] + rings['caller']['den']
            + rings['callee']['den']
            == row['rcc__branch__full__buggy__dyn']['den'])


def test_psc_branch_is_taken_over_total_on_p(row):
    assert _v(row, 'psc__branch__na__buggy__dyn') == pytest.approx(2 / 6)


def test_rcp_branch_denominator_is_every_outcome_taken(row):
    assert _v(row, 'rcp__branch__R0__buggy__dyn') == 0.5          # 4 of 8
    assert _v(row, 'rcp__branch__full__buggy__dyn') == 0.625      # 5 of 8


def test_rcp_branch_by_ring_decomposition_sums_to_one(row):
    rings = row['rcp__branch__full__buggy__dyn']['by_ring']
    assert rings['seed']['value'] == 0.5                          # 4 of 8
    assert rings['caller']['value'] == 0.125                      # 1 of 8
    assert rings['callee']['value'] == 0.0
    assert rings['outside']['value'] == 0.375                     # 3 of 8
    assert sum(rings[r]['value'] for r in
               ('seed', 'caller', 'callee', 'outside')) == 1.0


def test_rcr_branch_weighs_the_recovered_region_by_outcomes(row):
    """|R̂ ∩ P| and |R̂| counted in branch outcomes rather than in lines.
    Only A20 is in both, and it carries 4 of R0's 6 outcomes."""
    assert _v(row, 'rcr__branch__R0__na') == pytest.approx(4 / 6)
    assert _v(row, 'rcr__branch__full__na') == 0.5                # 4 of 8
    # RCR reads no coverage, but branch WEIGHTS have to come from a report,
    # and the key says which build's was used.
    assert row['rcr__branch__R0__na']['weights_from'] == 'buggy'


def test_the_line_only_r_variant_exists_at_branch_granularity_too(row):
    """Rbody is a LINE set, so branch counts it the same way: A10 (2 of 2)
    + A11 (no decision point, nothing) + A20 (2 of 4)."""
    assert _v(row, 'rcc__branch__Rbody__buggy__dyn') == pytest.approx(4 / 6)
    assert 'rcc__method__Rbody__buggy__dyn' not in row


def test_there_is_no_csm_at_branch_granularity(row):
    """A crash site is a stack frame, not a branch outcome."""
    assert any(k.startswith('csm__line__') for k in row)
    assert not any(k.startswith('csm__branch__') for k in row)


def test_no_branch_key_carries_the_static_f_kind(row):
    """A call graph names methods, so `stat` is method-only — and so is
    `rcr_cross`, which counts locations, not outcomes."""
    assert not any(k.startswith('rcr_cross__branch__') for k in row)
    for key in row:
        if key.startswith(('rcc__branch__', 'rcp__branch__', 'psc__branch__')):
            assert key.endswith('__dyn')


def test_sizes_report_the_per_set_branch_totals_and_their_source(row):
    b = row['sizes']['branches']['buggy']
    assert b['source'] == cov_mod.BRANCHES_MERGED
    assert b['lines_with_branches'] == 5
    assert b['all_lines'] == {'taken': 8, 'total': 14}
    assert b['R']['R0'] == {'taken': 4, 'total': 6}
    assert b['R']['full'] == {'taken': 5, 'total': 8}
    assert b['P'] == {'taken': 2, 'total': 6}
    # the whole-build counters are untouched next to them
    assert (b['covered'], b['total']) == (8, 14)


def test_a_leg_without_per_line_branch_data_emits_no_branch_keys(tmp_path):
    """The coverage files of a run measured before `line_branches` existed
    simply carry no branch granularity — not a zero-valued one."""
    leg = _write_leg(
        str(tmp_path / 'run'), '01_patch1-Alpha-1-Arja_o',
        {'label': 'correct', 'status': 'evaluated', 'bug_kind': 'crashing',
         'project': 'Alpha', 'bug_id': '1', 'apr_tool': 'Arja'},
        patch_derived_lines=_lset([(A20, loc.SEED)]),
        root_cause={'lines': _lset([(A10, loc.SEED)]).to_dict()},
        coverage={'buggy': {'build': 'buggy', 'methods': [], 'all_methods': [],
                            'lines': [A10.to_dict()],
                            'branches_covered': 3, 'branches_total': 7}})
    out = M.compute_leg(leg)
    assert not any('__branch__' in k for k in out)
    assert 'rcc__line__R0__buggy__dyn' in out
    assert out['sizes']['branches']['buggy']['all_lines'] == {'taken': 0,
                                                              'total': 0}


# ---------------------------------------------------------------------------
# the tables
# ---------------------------------------------------------------------------

@pytest.fixture()
def branch_run(tmp_path):
    run = str(tmp_path / 'run_conditioned')
    _branch_leg(run, '01_patch1-Alpha-1-Arja_o')
    _branch_leg(run, '02_patch1-Beta-2-Jaid_c')
    M.write_metrics(run)
    return run


def test_aggregate_carries_the_branch_keys(branch_run):
    metrics = A.aggregate(branch_run)['by_kind']['all']['metrics']
    assert metrics['rcc__branch__R0__buggy__dyn']['mean'] == \
        pytest.approx(4 / 6)
    assert metrics['psc__branch__na__buggy__dyn']['mean'] == \
        pytest.approx(2 / 6)
    # per-ring values aggregate at branch granularity too
    assert metrics['rcp__branch__full__buggy__dyn#outside']['mean'] == 0.375


def test_table3_markdown_has_a_branch_row_per_bug_class(branch_run):
    text = A.render_markdown(A.aggregate(branch_run))
    for kind in ('crashing', 'semantic', 'all'):
        assert f'| {kind} | branch | H_R |' in text
    # the branch row's RCC is filled and its CSM is n/a (no such key)
    line = next(l for l in text.splitlines()
                if l.startswith('| all | branch | H_R |'))
    assert '0.667' in line and line.rstrip().endswith('n/a |')


def test_paper_table3_renders_a_branch_row(branch_run):
    arm = PT.load_arm(branch_run, 'HR')
    text = PT.table3(arm, fmt='md')
    assert '| H_R | All | Branch |' in text
    row = next(l for l in text.splitlines()
               if l.startswith('| H_R | All | Branch |'))
    cells = [c.strip() for c in row.strip('|').split('|')]
    # RCR RCC RCP PSC CSM, then the (blank) F1 cell
    assert cells[3:8] == ['0.67', '0.67', '0.50', '0.33', '–']
    assert 'Branch' in PT.GRAN_LABELS.values()


def test_paper_table4_gains_a_branch_level_column_pair(branch_run):
    arm = PT.load_arm(branch_run, 'HR')
    text = PT.table4(arm, fmt='md')
    assert 'Branch-level H_N | Branch-level H_R' in text
    row = next(l for l in text.splitlines()
               if l.startswith('| Root-cause coverage, RCC |'))
    cells = [c.strip() for c in row.strip('|').split('|')]
    # Function H_N/H_R, Line H_N/H_R, Branch H_N/H_R — no naive arm here,
    # so every H_N cell is a dash
    assert len(cells) == 7
    assert cells[5] == '–' and cells[6].startswith('0.67')


def test_the_notes_say_what_branch_does_and_does_not_count(branch_run):
    arm = PT.load_arm(branch_run, 'HR')
    for text in (PT.table3(arm, fmt='md'), PT.table4(arm, fmt='md')):
        assert 'decision points' in text.lower()
        assert 'fallthrough' in text
        assert 'future work' in text
        assert 'Edge' not in text
