"""How a crash site is resolved to a method, and CSM's two denominators.

Everything is built from hand-written measurement JSON in the layout
`cli.py` writes, so no checkout, build or JaCoCo run is needed.

Two things are tested here that `test_measurements_metrics.py` does not:

* **Resolution by line.** A stack frame carries a class, a method name and
  a source line, but no parameter types. A build's coverage now carries a
  line-ownership map (`method_lines`), so the line picks out ONE overload —
  and a crash in `solve/3` then scores outside an R̂ that holds `solve/4`,
  where the old name-only rule scored it inside. The name rule is still
  there for a leg whose coverage predates the map.
* **The strict denominator.** `csm__*` counts only crashes with a library
  site; `csm_strict__*` counts every crash the kept set reported, which is
  what `d4j_rcc_sweep.crashes` does.
"""
import json
import os

from java.measurements import locations as loc
from java.measurements import metrics as M

# --- the Math-70 shape ------------------------------------------------------
# BisectionSolver.solve(f, min, max) delegates to solve(f, min, max, initial).
# The developer fixed the four-argument one, so R̂ holds solve/4; the crash
# happens in solve/3, on a line only the ownership map can attribute.
SOLVER = 'org.apache.commons.math.analysis.solvers.BisectionSolver'
SOLVE3 = loc.MethodRef(SOLVER, 'solve', ('UnivariateRealFunction', 'double',
                                         'double'))
SOLVE4 = loc.MethodRef(SOLVER, 'solve', ('UnivariateRealFunction', 'double',
                                         'double', 'double'))
#: what a stack frame gives: no parameter types at all
SOLVE_FRAME = loc.MethodRef(SOLVER, 'solve', ())

A_f_str = loc.MethodRef('org.ex.Alpha', 'f', ('String',))
A_f_int = loc.MethodRef('org.ex.Alpha', 'f', ('int',))
A_g = loc.MethodRef('org.ex.Alpha', 'g', ())
B_h = loc.MethodRef('org.ex.Beta', 'h', ())


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


def _cov(build, methods, all_methods, method_lines=None, lines=()):
    """One coverage_<build>.json.  `method_lines` is the line-ownership
    map; a leg written before it existed simply has no such key, which is
    what `method_lines=None` produces here."""
    d = {'build': build,
         'methods': [m.to_dict() for m in methods],
         'lines': [x.to_dict() for x in lines],
         'all_methods': [m.to_dict() for m in all_methods],
         'branches_covered': 0, 'branches_total': 0}
    if method_lines is not None:
        d['method_lines'] = [dict(m.to_dict(), start=a, end=b)
                             for m, (a, b) in method_lines.items()]
    return d


def _site(build, kind, method=None, line=None,
          exception='java.lang.NullPointerException'):
    return {'build': build, 'site_kind': kind, 'exception': exception,
            'top_library': method.to_dict() if method else None,
            'top_library_line': (line.to_dict() if hasattr(line, 'to_dict')
                                 else line)}


def _leg(tmp_path, name, *, root_cause, coverage=None, crash_sites=None):
    leg = os.path.join(str(tmp_path), name)
    mdir = os.path.join(leg, 'measurements')
    os.makedirs(mdir, exist_ok=True)
    with open(os.path.join(leg, 'result.jsonl'), 'w') as fh:
        fh.write(json.dumps(
            {'label': 'overfitting', 'status': 'evaluated',
             'bug_kind': 'crashing', 'project': 'Math', 'bug_id': '70',
             'apr_tool': 'Arja', 'crashed_on_patch': True}) + '\n')
    with open(os.path.join(mdir, 'root_cause.json'), 'w') as fh:
        json.dump(root_cause, fh)
    for build, cov in (coverage or {}).items():
        with open(os.path.join(mdir, f'coverage_{build}.json'), 'w') as fh:
            json.dump(cov, fh)
    if crash_sites is not None:
        with open(os.path.join(mdir, 'crash_sites.json'), 'w') as fh:
            json.dump(crash_sites, fh)
    return leg


def _solver_root_cause():
    """R̂ = the four-argument solve alone, the method the developer fixed."""
    return {'project': 'Math', 'bug_id': '70',
            'methods': _mset([(SOLVE4, loc.SEED)]).to_dict(),
            'lines': _lset([]).to_dict(),
            'manifest': _mset([]).to_dict()}


#: solve/3 is declared on line 66 and solve/4 on line 72, so 66..71 belong
#: to the three-argument overload and 72 onwards to the four-argument one.
SOLVER_OWNERSHIP = {SOLVE3: (66, 72), SOLVE4: (72, 95)}


def _solver_leg(tmp_path, name, *, line, ownership):
    return _leg(
        tmp_path, name,
        root_cause=_solver_root_cause(),
        coverage={'buggy': _cov('buggy', [SOLVE3, SOLVE4], [SOLVE3, SOLVE4],
                                method_lines=ownership)},
        crash_sites=[_site('buggy', 'library', SOLVE_FRAME, line)])


# ---------------------------------------------------------------------------
# resolution by line
# ---------------------------------------------------------------------------

def test_a_crash_in_another_overload_scores_outside_r_hat(tmp_path):
    """THE MATH-70 SHAPE. The crash is in `solve/3`; R̂ holds `solve/4`.
    The line says which overload it was, so the site is outside R̂ — where
    matching by name alone would have called it a hit."""
    leg = _solver_leg(tmp_path, '01_patch1-Math-70-Arja_o',
                      line=68, ownership=SOLVER_OWNERSHIP)
    row = M.compute_leg(leg)
    csm = row['csm__method__R0__na']
    assert (csm['num'], csm['den'], csm['value']) == (0, 1, 0.0)
    assert csm['by_ring']['outside']['value'] == 1.0
    assert csm['resolution'] == {'line': 1, 'name': 0,
                                 'name-ambiguous-nearest-ring': 0}
    assert row['sizes']['csm_resolution'] == csm['resolution']


def test_a_crash_in_the_fixed_overload_scores_inside(tmp_path):
    """The same leg with the crash one overload further down the file:
    line 80 is owned by `solve/4`, which IS R̂."""
    leg = _solver_leg(tmp_path, '01_patch1-Math-70-Arja_o',
                      line=80, ownership=SOLVER_OWNERSHIP)
    row = M.compute_leg(leg)
    csm = row['csm__method__R0__na']
    assert (csm['num'], csm['den'], csm['value']) == (1, 1, 1.0)
    assert csm['by_ring']['seed']['value'] == 1.0
    assert csm['resolution']['line'] == 1


def test_without_an_ownership_map_the_name_rule_decides(tmp_path):
    """A leg whose coverage predates `method_lines` has nothing to resolve
    the line against, so the frame is matched by class and name — and the
    same crash in `solve/3` is then credited to `solve/4`. This is the
    answer the line rule replaces, and it is kept as the fallback because
    a name match is better than no match at all."""
    leg = _solver_leg(tmp_path, '01_patch1-Math-70-Arja_o',
                      line=68, ownership=None)
    row = M.compute_leg(leg)
    csm = row['csm__method__R0__na']
    assert (csm['num'], csm['den'], csm['value']) == (1, 1, 1.0)
    assert csm['resolution'] == {'line': 0, 'name': 1,
                                 'name-ambiguous-nearest-ring': 0}


def test_without_any_coverage_the_name_rule_decides(tmp_path):
    """No coverage file at all: same fallback, no crash."""
    leg = _leg(tmp_path, '01_patch1-Math-70-Arja_o',
               root_cause=_solver_root_cause(),
               crash_sites=[_site('buggy', 'library', SOLVE_FRAME, 68)])
    row = M.compute_leg(leg)
    assert row['csm__method__R0__na']['resolution']['name'] == 1


def test_a_line_no_method_owns_falls_back_to_the_name_rule(tmp_path):
    """Line 400 is past every range in the map (an inlined frame, a report
    from a different revision): the map cannot place it, so the name rule
    does."""
    leg = _solver_leg(tmp_path, '01_patch1-Math-70-Arja_o',
                      line=400, ownership=SOLVER_OWNERSHIP)
    row = M.compute_leg(leg)
    csm = row['csm__method__R0__na']
    assert csm['resolution'] == {'line': 0, 'name': 1,
                                 'name-ambiguous-nearest-ring': 0}
    assert csm['value'] == 1.0


def test_a_crash_with_no_line_falls_back_to_the_name_rule(tmp_path):
    """A crash identity recovered from a one-line note in a trace carries
    no line number at all."""
    leg = _solver_leg(tmp_path, '01_patch1-Math-70-Arja_o',
                      line=None, ownership=SOLVER_OWNERSHIP)
    row = M.compute_leg(leg)
    assert row['csm__method__R0__na']['resolution']['name'] == 1


def test_the_nearest_ring_tie_break_is_recorded_as_its_own_rule(tmp_path):
    """Two overloads of one name are both in R̂ and there is no ownership
    map, so the name alone cannot say which was hit. The site is inside R̂
    either way, and the rule says the ring was a tie-break."""
    leg = _leg(
        tmp_path, '01_patch1-Chart-1-Arja_o',
        root_cause={'project': 'Chart', 'bug_id': '1',
                    'methods': _mset([(A_f_str, loc.SEED),
                                      (A_f_int, loc.CALLEE)]).to_dict(),
                    'lines': _lset([]).to_dict(),
                    'manifest': _mset([]).to_dict()},
        crash_sites=[_site('buggy', 'library',
                           loc.MethodRef('org.ex.Alpha', 'f', ()), 12)])
    row = M.compute_leg(leg)
    csm = row['csm__method__full__na']
    assert csm['value'] == 1.0
    assert csm['by_ring']['seed']['value'] == 1.0      # the nearest ring won
    assert csm['resolution'] == {'line': 0, 'name': 0,
                                 'name-ambiguous-nearest-ring': 1}


# ---------------------------------------------------------------------------
# the two denominators
# ---------------------------------------------------------------------------

def _mixed_leg(tmp_path, name='01_patch1-Chart-1-Arja_o', extra=()):
    """Six crashes: three library sites inside R̂, one library site outside
    it, two that never left the harness."""
    sites = [
        _site('buggy', 'library', A_f_str, 12),
        _site('buggy', 'library', A_g, 30, 'java.lang.IllegalStateException'),
        _site('patched', 'library', A_f_str, 13, 'java.lang.ArithmeticException'),
        _site('buggy', 'library', B_h, 90, 'java.lang.ArrayIndexOutOfBoundsException'),
        _site('buggy', 'harness_only', None, None, 'java.lang.AssertionError'),
        _site('patched', 'harness_only', None, None, 'java.lang.AssertionError'),
    ] + list(extra)
    return _leg(
        tmp_path, name,
        root_cause={'project': 'Chart', 'bug_id': '1',
                    'methods': _mset([(A_f_str, loc.SEED),
                                      (A_g, loc.CALLER)]).to_dict(),
                    'lines': _lset([(loc.LineRef('org.ex.Alpha', 12),
                                     loc.SEED)]).to_dict(),
                    'manifest': _mset([]).to_dict()},
        crash_sites=sites)


def test_the_two_denominators_on_one_leg(tmp_path):
    """Three of the four library sites are in R̂, so CSM is 3/4; two more
    crashes never left the harness, so the strict CSM is 3/6."""
    row = M.compute_leg(_mixed_leg(tmp_path))
    csm = row['csm__method__full__na']
    strict = row['csm_strict__method__full__na']
    assert (csm['num'], csm['den'], csm['value']) == (3, 4, 0.75)
    assert (strict['num'], strict['den'], strict['value']) == (3, 6, 0.5)
    # the same numerator, so the strict number is never the larger one
    assert strict['num'] == csm['num']
    assert strict['value'] <= csm['value']


def test_the_strict_row_says_what_its_extra_denominator_holds(tmp_path):
    row = M.compute_leg(_mixed_leg(tmp_path))
    strict = row['csm_strict__method__full__na']
    assert strict['harness_only'] == 2
    assert strict['library'] == 4
    # harness-only crashes decompose into 'outside': 1 library site outside
    # R̂ plus the 2 that named no library code at all
    assert strict['by_ring']['outside'] == {'value': 0.5, 'num': 3, 'den': 6}
    assert strict['by_ring']['seed']['num'] == 2
    assert strict['by_ring']['caller']['num'] == 1
    assert sum(strict['by_ring'][r]['num']
               for r in ('seed', 'caller', 'callee', 'outside')) == 6


def test_the_strict_denominator_exists_at_line_granularity_too(tmp_path):
    row = M.compute_leg(_mixed_leg(tmp_path))
    # one crash sits on org.ex.Alpha:12, the only line in R̂
    assert row['csm__line__R0__na']['den'] == 4
    assert row['csm__line__R0__na']['num'] == 1
    strict = row['csm_strict__line__R0__na']
    assert (strict['num'], strict['den']) == (1, 6)
    assert strict['harness_only'] == 2


def test_both_denominators_ignore_the_acceptance_gate_s_own_crashes(tmp_path):
    """`compiled` crashes come from candidates the gate then threw away;
    they are counted in `crash_by_build` and in neither denominator."""
    row = M.compute_leg(_mixed_leg(tmp_path, extra=[
        _site('compiled', 'library', B_h, 91),
        _site('compiled', 'harness_only', None, None)]))
    assert row['crash_total'] == 8
    assert row['crash_compiled'] == 2
    assert row['csm__method__full__na']['den'] == 4
    assert row['csm_strict__method__full__na']['den'] == 6


def test_a_leg_with_no_crash_sites_file_has_no_strict_key(tmp_path):
    leg = _leg(tmp_path, '01_patch1-Math-70-Arja_o',
               root_cause=_solver_root_cause())
    row = M.compute_leg(leg)
    assert not [k for k in row if k.startswith('csm_strict__')]
    assert row['sizes']['csm_resolution'] is None


def test_the_strict_key_has_the_four_slot_shape(tmp_path):
    assert M.metric_key('csm_strict', 'method', 'R0', None) == \
        'csm_strict__method__R0__na'
    row = M.compute_leg(_mixed_leg(tmp_path))
    assert 'csm_strict__method__R0__na' in row


# ---------------------------------------------------------------------------
# the README says all of this
# ---------------------------------------------------------------------------

README = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(
    __file__))), 'src', 'java', 'measurements', 'README.md')


def test_readme_documents_both_denominators_and_the_resolution_rules():
    with open(README) as fh:
        text = fh.read()
    for phrase in (
            'csm_strict__<gran>__<R̂>__na',        # the new key
            'Two denominators, both reported',
            'method_lines',                        # the ownership map
            'name-ambiguous-nearest-ring',         # the recorded rules
            'sizes.csm_resolution',
            'headline_site',                       # the cause-chain rule
            'Follow the cause chain to its end',
            '`javax.`',                            # the added exclusions
            '`org.junit.`',
            'accepted_harnesses[].class_name',     # harness by record name
            'distinct crashes',                    # the dedupe difference
    ):
        assert phrase in text, phrase
