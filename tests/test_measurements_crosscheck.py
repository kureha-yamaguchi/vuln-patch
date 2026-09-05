"""Two implementations of root-cause coverage, on the same real reports.

`java.measurements.d4j_rcc_sweep` (Kureha's earlier sweep) and the rest of
`src/java/measurements` both compute RCC = |R̂ ∩ F(H)| / |R̂|, and they share
nothing: different region extractors, different method identities,
different readers of the JaCoCo XML. The sweep reads the report through
fuzz-introspector; the general layer parses the XML itself. Two
independent paths to one number are only worth having if somebody checks
that they agree, which is what this file does — on the real reports of

    results/rcc_hr_crashing_holdout_20260904_001615/

the crashing-split holdout sweep `d4j_rcc_sweep/rcc_sweep.py` produced, with
its per-bug records in `rcc.jsonl` and the merged harness-set report of each
bug in `<bug>/harness/jacoco.xml`.

The whole file skips when that directory is not on the machine. It reads it
and writes nothing.

WHERE THEY AGREE, AND WHERE THEY DO NOT
=======================================
RCC agrees on every scored bug, and so does the frame repair (Math-70, the
one bug that needs it). |F(H)| does NOT agree everywhere, and the difference
is real rather than a rounding of ours: fuzz-introspector decides which
lines belong to a method by taking, from the method's declaration line, as
many entries of the source file's line list as the method's LINE counter
says it has. That window runs past the method whenever the counter and the
line list disagree, and a covered line belonging to a LATER method is then
credited to the earlier one. `EXPECTED_F` records the four bugs where this
happens and names the method each time; `test_f_sizes_match_within_the_known_
window_artefact` is the test that would fail if the gap ever moved.

MEASUREMENT ONLY.
"""
import json
import os
import re

import pytest

from java.measurements import coverage as cov_mod
from java.measurements import root_cause as rc_mod
from java.measurements.locations import MethodIndex, MethodRef

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RUN_DIR = os.path.join(REPO, 'results',
                       'rcc_hr_crashing_holdout_20260904_001615')
RECORDS = os.path.join(RUN_DIR, 'rcc.jsonl')

pytestmark = pytest.mark.skipif(
    not os.path.isfile(RECORDS),
    reason=f'the measured holdout run is not on this machine ({RUN_DIR})')


# ---------------------------------------------------------------------------
# reading her records
# ---------------------------------------------------------------------------

def _records():
    with open(RECORDS, encoding='utf-8') as fh:
        return [json.loads(line) for line in fh if line.strip()]


def _bug_dir(record):
    return os.path.join(RUN_DIR, f"{record['project']}_{record['bug_id']}")


def _scored():
    """Her records that carry a number AND the report it was read from."""
    out = []
    for record in _records():
        report = os.path.join(_bug_dir(record), 'harness', 'jacoco.xml')
        if os.path.isfile(report) and isinstance(record.get('rcc'),
                                                 (int, float)):
            out.append((record, report))
    return out


# `d4j_rcc_sweep.keys.MethodKey.__str__`: the SIMPLE class name, the method
# name,
# and the simple parameter types, comma-space separated —
# `StringUtils.join(Object[], String, int, int)`. A constructor is
# `Widget.<init>(int)`.
_KEY_RE = re.compile(r'^(?P<cls>[\w$.]+)\.(?P<name>[\w$]+|<init>|<clinit>)'
                     r'\((?P<args>.*)\)$')


def region_ref(text):
    """One of her `region` strings as a `MethodRef`.

    The class is her SIMPLE name, so the ref is unqualified and
    `MethodIndex` matches it on (simple class, name, arity) — which is
    exactly her own `MethodKey.loose` fallback."""
    m = _KEY_RE.match(text.strip())
    assert m, f'unparsable region key: {text!r}'
    params = tuple(p.strip() for p in m.group('args').split(',') if p.strip())
    return MethodRef(m.group('cls'), m.group('name'), params)


# ---------------------------------------------------------------------------
# our side of the number
# ---------------------------------------------------------------------------

def our_coverage(record, report):
    """F(H) our way: parse the merged report, then repair the probe miss
    from the run's own Jazzer output."""
    cov = cov_mod.parse_jacoco_xml(report)
    trace_path = os.path.join(os.path.dirname(report), 'jazzer_output.txt')
    if os.path.isfile(trace_path):
        with open(trace_path, encoding='utf-8', errors='replace') as fh:
            cov_mod.repair_from_frames(cov, report, fh.read())
    return cov


def candidates(ref, population, index):
    """Every method of the report one of her region strings can mean.

    Her `MethodKey` holds the FULLY QUALIFIED class, but the string she
    writes into `rcc.jsonl` prints only the simple one
    (`NumberUtils.createNumber(String)`), so the package is gone by the
    time we read it. Where a project has two classes of that simple name —
    Lang has `org.apache.commons.lang.NumberUtils` and
    `org.apache.commons.lang.math.NumberUtils`, and Lang-58's fix is in the
    second — the string names both, and no reader of the string can tell
    which. So this returns all of them and the caller counts the key as
    covered when ANY of them was; `test_region_strings_that_name_two_
    classes` says where that happens.

    Exact parameter types first, then `MethodIndex`'s fallback for an
    unqualified ref, which is (simple class, name, arity). That is her
    order too: `d4j_rcc_sweep.rcc.ReachedSet` compares types and falls back
    to
    the argument count."""
    exact = [m for m in sorted(population)
             if m.class_simple == ref.class_simple and m.name == ref.name
             and m.params == ref.params]
    if exact:
        return exact
    # The fallback cannot separate two overloads of equal arity and returns
    # nothing rather than guessing, which is also what she reports.
    hit = index.lookup(ref)
    return [hit] if hit is not None else []


def our_rcc(record, cov):
    """RCC our way: her region strings, our identities, our matching."""
    population = cov.all_methods or cov.methods
    index = MethodIndex(population)
    covered, missed = [], []
    for text in record.get('region') or []:
        found = candidates(region_ref(text), population, index)
        (covered if any(m in cov.methods for m in found) else missed
         ).append(text)
    if not (covered or missed):
        return None, covered, missed
    return len(covered) / (len(covered) + len(missed)), covered, missed


# ---------------------------------------------------------------------------
# 1. the number itself
# ---------------------------------------------------------------------------

def test_rcc_agrees_on_every_scored_bug():
    """The headline claim: two implementations, one number per bug."""
    scored = _scored()
    assert scored, 'no scored bug had a report to re-read'
    ours = {}
    for record, report in scored:
        bug = f"{record['project']}-{record['bug_id']}"
        value, covered, missed = our_rcc(record, our_coverage(record, report))
        ours[bug] = value
        assert value == record['rcc'], (
            f'{bug}: the sweep says RCC={record["rcc"]}, we say {value} '
            f'(covered {covered}, missed {missed})')
    assert ours == {'Chart-5': 1.0, 'Lang-16': 1.0, 'Lang-20': 1.0,
                    'Lang-45': 1.0, 'Lang-58': 1.0, 'Lang-59': 1.0,
                    'Math-58': 1.0, 'Math-70': 1.0, 'Math-85': 1.0}


def test_every_region_key_resolves_to_a_real_method():
    """A region key that matched nothing would read as RCC = 0 and look
    like a finding. Every one of hers lands on a method of the report."""
    for record, report in _scored():
        cov = our_coverage(record, report)
        index = MethodIndex(cov.all_methods)
        for text in record['region']:
            assert candidates(region_ref(text), cov.all_methods, index), (
                f"{record['project']}-{record['bug_id']}: {text} matched no "
                'method in the report')


def test_region_strings_that_name_two_classes():
    """Where her printed region key is ambiguous, and by how much.

    This is a property of the record format, not of either implementation:
    the key prints the simple class name, and Lang has two `NumberUtils`.
    Lang-58's fix is in `org.apache.commons.lang.math.NumberUtils` and only
    that one ran, so counting "any candidate covered" gives the same answer
    her fully-qualified key does — but the ambiguity is named here rather
    than left implicit. Lang-16 has the same method in its region and is
    NOT ambiguous: its report holds only the one class."""
    ambiguous = {}
    for record, report in _scored():
        cov = our_coverage(record, report)
        index = MethodIndex(cov.all_methods)
        for text in record['region']:
            found = candidates(region_ref(text), cov.all_methods, index)
            if len(found) > 1:
                ambiguous[f"{record['project']}-{record['bug_id']}"] = sorted(
                    m.strict_key for m in found)
    assert ambiguous == {
        'Lang-58': ['org.apache.commons.lang.NumberUtils.createNumber(String)',
                    'org.apache.commons.lang.math.NumberUtils.'
                    'createNumber(String)'],
    }
    # In both, exactly one of the two ran, so the ambiguity costs nothing.
    for record, report in _scored():
        if f"{record['project']}-{record['bug_id']}" not in ambiguous:
            continue
        cov = our_coverage(record, report)
        index = MethodIndex(cov.all_methods)
        found = candidates(region_ref(record['region'][0]), cov.all_methods,
                           index)
        assert sum(1 for m in found if m in cov.methods) == 1


# ---------------------------------------------------------------------------
# 2. the frame repair
# ---------------------------------------------------------------------------

def test_the_frame_repair_finds_the_same_methods_on_math_70():
    """Math-70 is the recorded probe miss: `BisectionSolver.solve` throws
    through its only call, so JaCoCo's probe never runs and the method
    reads as missed. Both implementations recover it from the stack frame,
    and both recover the same two methods."""
    record = next(r for r in _records()
                  if (r['project'], r['bug_id']) == ('Math', '70'))
    cov = our_coverage(record, os.path.join(_bug_dir(record), 'harness',
                                            'jacoco.xml'))
    ours = sorted(f"{m.class_simple}.{m.name}({', '.join(m.params)})"
                  for m in cov.frame_added)
    assert ours == record['fuzzer_frame_added'] == [
        'BisectionSolver.solve(UnivariateRealFunction, double, double, '
        'double)',
        'BisectionSolver.solve(double, double)']
    # The repair is a UNION, never a substitution: the probe set is intact.
    assert cov.methods_from_probes <= cov.methods
    assert len(cov.methods) == len(cov.methods_from_probes) + 2


def test_no_other_bug_needed_the_frame_repair():
    """Everywhere else the probes already had every method the frames name,
    which is what makes Math-70 worth naming."""
    added = {}
    for record, report in _scored():
        cov = our_coverage(record, report)
        added[f"{record['project']}-{record['bug_id']}"] = len(cov.frame_added)
    assert added == {'Chart-5': 0, 'Lang-16': 0, 'Lang-20': 0, 'Lang-45': 0,
                     'Lang-58': 0, 'Lang-59': 0, 'Math-58': 0, 'Math-70': 2,
                     'Math-85': 0}


# ---------------------------------------------------------------------------
# 3. |F(H)|, where the two DISAGREE
# ---------------------------------------------------------------------------

#: Per bug: (our |F| after the frame repair, her `fuzzer_reached_size`), and
#: the method she counts that we do not. Ours is JaCoCo's own METHOD counter;
#: hers is fuzz-introspector's line window (module docstring), which credits
#: a method with a covered line that belongs to a later one.
EXPECTED_F = {
    'Chart-5': (17, 17, None),
    'Lang-16': (6, 7, 'StringUtils.<clinit>, whose 33-line window from line '
                      '147 reaches isBlank\'s covered lines at 223-228'),
    'Lang-20': (13, 14, 'StringUtils.<clinit>, same window, reaching the '
                        'covered lines from 361 on'),
    'Lang-45': (2, 2, None),
    'Lang-58': (7, 7, None),
    'Lang-59': (3, 3, None),
    'Math-58': (82, 82, None),
    'Math-70': (9, 10, 'UnivariateRealSolverImpl.<init>(UnivariateRealFunction'
                       ',int,double), which shares its declaration line 41 '
                       'with the covered <init>(int,double)'),
    'Math-85': (47, 48, 'UnivariateRealSolverImpl.<init>(UnivariateRealFunction'
                        ',int,double), the same shared line 41'),
}


def test_f_sizes_match_within_the_known_window_artefact():
    """|F(H)| agrees on five of the nine scored bugs and is one smaller on
    the other four. Ours counts a method when JaCoCo's own METHOD counter
    says it was executed; hers counts it when a line inside a WINDOW
    starting at the method's declaration line was executed, and the window
    over-runs. On all four bugs the method that separates the two is one
    JaCoCo reports as never executed, so the difference is hers to explain,
    not a method we lost."""
    seen = {}
    for record, report in _scored():
        bug = f"{record['project']}-{record['bug_id']}"
        cov = our_coverage(record, report)
        seen[bug] = (len(cov.methods), record['fuzzer_reached_size'])
    assert seen == {bug: (ours, hers)
                    for bug, (ours, hers, _why) in EXPECTED_F.items()}
    # Never the other way round: everything we count, she counts too.
    for bug, (ours, hers) in seen.items():
        assert ours <= hers, f'{bug}: we count more than the sweep does'


def test_probe_halves_agree_wherever_the_window_does_not_over_run():
    """The probe halves are the same measurement on both sides, so on the
    five bugs where her window stays inside its method they agree
    exactly."""
    for record, report in _scored():
        bug = f"{record['project']}-{record['bug_id']}"
        _ours, _hers, why = EXPECTED_F[bug]
        if why is not None:
            continue
        cov = our_coverage(record, report)
        assert len(cov.methods_from_probes) == record['fuzzer_probe_size'], bug


# ---------------------------------------------------------------------------
# 4. the population
# ---------------------------------------------------------------------------

def test_population_check_reproduces_her_summary_table():
    """`population_check` states what a mean is only honest beside: how many
    bugs were scored, and why each of the others left. Her summary.md says
    nine scored at 1.000 and Lang-43 excluded for accepting no harness."""
    summary = rc_mod.population_check(_records())
    assert summary['n_records'] == 10
    assert summary['n_scored'] == 9
    assert summary['mean'] == 1.0
    assert summary['counts'] == {'ok': 9, 'no_harnesses': 1}
    assert summary['bugs']['no_harnesses'] == ['Lang-43']
    assert 'Lang-43' not in summary['scored']


def test_an_unavailable_measurement_is_never_a_zero():
    """Lang-43's leg accepted no harness, so it has no F(H) at all. It
    leaves the population; it does not score zero and drag the mean."""
    lang43 = next(r for r in _records()
                  if (r['project'], r['bug_id']) == ('Lang', '43'))
    assert lang43['status'] == rc_mod.STATUS_NO_HARNESSES
    assert lang43.get('rcc') is None
    assert lang43['gate'] is True, 'the gate passed: the region is fine'


def test_population_statuses_are_the_ones_the_sweep_can_write():
    """Every status `d4j_rcc_sweep/sweep.py` and `rcc_sweep.py` can record
    has
    a slot here, so a population summary can never silently drop a class of
    exclusion."""
    for status in ('ok', 'excluded_empty_region', 'excluded_gate_failed',
                   'no_harnesses', 'infra_error'):
        assert status in rc_mod.POPULATION_STATUSES
    assert set(r['status'] for r in _records()) <= set(
        rc_mod.POPULATION_STATUSES)


# ---------------------------------------------------------------------------
# 5. the direction of the dependency
# ---------------------------------------------------------------------------

def test_the_import_between_the_two_implementations_is_one_way():
    """The general layer may read `d4j_rcc_sweep`; the sweep may never read
    the general layer.

    Both now live in `src/java/measurements/`, so the direction is no longer
    a package boundary and has to be named. The reason is the firewall:
    `root_cause.py` is the only module of the general layer allowed to read
    a developer fix, and nothing the pipeline can reach may import it. The
    sweep starts the pipeline's own runner, so an edge from the sweep into
    the general layer would put the quarantined module one import closer to
    the pipeline. `tests/test_measurements_firewall.py` is the general
    enforcement; this names the specific direction."""
    import ast

    sweep_dir = os.path.join(REPO, 'src', 'java', 'measurements',
                             'd4j_rcc_sweep')
    own = {'d4j_rcc_sweep'}
    offenders = []
    for name in sorted(os.listdir(sweep_dir)):
        if not name.endswith('.py'):
            continue
        with open(os.path.join(sweep_dir, name), encoding='utf-8') as fh:
            tree = ast.parse(fh.read())
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                names = [a.name for a in node.names]
            elif isinstance(node, ast.ImportFrom):
                names = [node.module or '']
            else:
                continue
            for imported in names:
                parts = imported.lstrip('.').split('.')
                # its own modules are `java.measurements.d4j_rcc_sweep.*`
                if 'measurements' in parts and not own & set(parts):
                    offenders.append((name, imported))
    assert not offenders, (
        'd4j_rcc_sweep must not import the general measurement layer: '
        f'{offenders}')
