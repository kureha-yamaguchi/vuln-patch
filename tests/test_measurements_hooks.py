"""The three measurement hooks: context.json, --coverage, --naive.

Station: three different ones, tested together because they share one
rule and it is the rule that can silently break.

  * HOOK 1 — analysis (TargetAnalyzer), run.py. The context dict the trace
    already records is also written to `<leg_dir>/context.json`, carrying
    two new measurement-only fields: `xref_names` (the mangled names of
    the callers whose SOURCE TEXT `xrefs` holds) and the shape of the
    callee walk (`reachable_edges` / `reachable_depth`).
  * HOOK 2 — the Jazzer invocation, execution/fuzz_runner.py. `--coverage`
    adds exactly two Jazzer flags and copies the class files a post-hoc
    JaCoCo report will need. Both classes that invoke Jazzer carry it:
    `FuzzRunner` (the KEPT harnesses, builds `patched` and `buggy`) and
    `HarnessVerifier`, whose acceptance run is the only time every
    COMPILED candidate is executed (build `compiled`).
  * HOOK 3 — prompt assembly, harness/prompts.py AND
    relations/relation_synth.py. `--naive` builds the paper's
    unconditioned H_N leg by dropping every root-cause-NEIGHBOURHOOD
    insertion from BOTH model-facing prompt builders: the harness prompt
    (variant-analysis / <root_cause_reachable> block with its coverage
    steering, <xref> caller call-sites, <callee> declarations, and the
    reachable-region clause of the propagation rule) and the
    relation-synthesis prompt (the "Reachable API" line). A leg whose
    harness prompt is unconditioned while its rule-synthesis prompt still
    lists the reachable set is not an H_N leg, and nothing but a test
    says so.

Failure mode they target: a measurement that changes the thing it
measures. Every hook is flag-gated (hook 1 is a file write and nothing
else), and the tests below are mostly about the OFF path — that the
Jazzer command line, the prompt text, and the reachable set are what they
were. A hook that quietly perturbs the run makes every number collected
with it uncomparable to every number collected without it, and nothing
downstream would notice.
"""
import json
import os
import sys

import pytest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, 'src'))

from java.bug_context import call_graph                    # noqa: E402
from java.bug_context.analysis import (                     # noqa: E402
    PatchContext, RelatedCallee, TargetAnalyzer, TouchedFunction)
from java.bug_context.failure_test import FailureTest       # noqa: E402
from java.execution import fuzz_runner                      # noqa: E402
from java.harness.prompts import PromptBuilder              # noqa: E402
from java.relations.relation_synth import RelationSynthesizer  # noqa: E402
import run as run_mod                                       # noqa: E402


# ---------------------------------------------------------------------------
# A tiny fake introspector project
# ---------------------------------------------------------------------------

class _Profile:
    def __init__(self, name, callees):
        self.name = name
        self.base_callsites = [[c, 0] for c in callees]


class _Project:
    """Just enough of a fuzz-introspector project for the call-graph and
    xref passes: `all_functions` plus the two accessors."""

    def __init__(self, graph, xrefs=None):
        self.all_functions = [_Profile(n, cs) for n, cs in graph.items()]
        self._xrefs = xrefs or {}

    def get_cross_references_by_name(self, mangled):
        return self._xrefs.get(mangled, [])

    def find_function_by_name(self, name, exact):   # pragma: no cover
        return None


class _Xref:
    def __init__(self, name, source):
        self.name = name
        self._source = source

    def function_source_code_as_text(self):
        return self._source


GRAPH = {
    '[org.example.A].root(int)': ['[org.example.A].mid(int)',
                                  '[org.example.B].leaf()'],
    '[org.example.A].mid(int)': ['[org.example.B].leaf()',
                                 '[org.example.C].deep()'],
    '[org.example.B].leaf()': [],
    '[org.example.C].deep()': ['[org.example.A].root(int)'],
}


# ===========================================================================
# HOOK 1a — bfs_callees_with_edges is the same walk as bfs_callees
# ===========================================================================

def _fmap():
    return call_graph.function_map(_Project(GRAPH))


@pytest.mark.parametrize('cap,depth', [
    (200, 3), (200, 1), (200, 2), (1, 3), (2, 3), (3, 1), (0, 3), (200, 0),
])
def test_with_edges_returns_exactly_the_same_names(cap, depth):
    """The names half must be indistinguishable from the old function —
    same members AND same order — at every cap/depth the wrapper can be
    called with. `bfs_callees` is on the prompt path (it feeds
    `root_cause_reachable`), so a reordering here is a silent prompt
    change wearing a measurement's clothes."""
    fmap = _fmap()
    start = '[org.example.A].root(int)'
    names, _edges, _depths = call_graph.bfs_callees_with_edges(
        fmap, start, cap, depth)
    assert names == call_graph.bfs_callees(fmap, start, cap, depth)


def test_reachable_of_still_returns_the_same_set():
    """The public entry point the analyzer uses is unchanged by the
    rewrite of its BFS."""
    project = _Project(GRAPH)
    assert call_graph.reachable_of(project, '[org.example.A].root(int)',
                                   200, 3) == [
        '[org.example.A].mid(int)',
        '[org.example.B].leaf()',
        '[org.example.C].deep()',
    ]


def test_edges_and_depths_describe_the_walk():
    names, edges, depths = call_graph.bfs_callees_with_edges(
        _fmap(), '[org.example.A].root(int)', 200, 3)
    # Every edge is (a node the walk expanded, a callee of it).
    assert ('[org.example.A].root(int)', '[org.example.A].mid(int)') in edges
    # An edge to an ALREADY-SEEN node is still a real edge: mid calls leaf,
    # which root reached first. The name appears once; the edge twice.
    assert edges.count(('[org.example.A].mid(int)',
                        '[org.example.B].leaf()')) == 1
    assert names.count('[org.example.B].leaf()') == 1
    # The start is depth 0 and every returned name has a depth.
    assert depths['[org.example.A].root(int)'] == 0
    assert depths['[org.example.A].mid(int)'] == 1
    assert depths['[org.example.C].deep()'] == 2
    assert set(names) <= set(depths)


def test_depth_bound_is_respected():
    names, _edges, depths = call_graph.bfs_callees_with_edges(
        _fmap(), '[org.example.A].root(int)', 200, 1)
    assert '[org.example.C].deep()' not in names
    assert max(depths.values()) <= 1


def test_node_cap_is_respected():
    names, _edges, _depths = call_graph.bfs_callees_with_edges(
        _fmap(), '[org.example.A].root(int)', 2, 3)
    assert len(names) == 2


# ===========================================================================
# HOOK 1b — xref_names and the walk shape land on the context
# ===========================================================================

def _touched():
    return TouchedFunction(
        func_name='root',
        func_signature='int root(int)',
        func_source='public int root(int x) { return mid(x); }',
        func_class='A',
        func_class_fq='org.example.A',
        func_param_types=['int'],
    )


def test_enrich_records_xref_names_beside_the_xref_text():
    """`xrefs` (what the prompt renders) and `xref_names` (what a
    measurement matches on) must line up index for index."""
    fn = _touched()
    project = _Project(GRAPH, xrefs={
        '[org.example.A].root(int)': [
            _Xref('[org.example.D].callerOne()', 'void callerOne() { root(1); }'),
            _Xref('[org.example.D].callerOne()', 'void callerOne() { root(1); }'),
            _Xref('[org.example.E].callerTwo()', 'void callerTwo() { root(2); }'),
        ]})
    out = TargetAnalyzer()._enrich_with_xrefs([fn], project, [])
    got = out[0]
    # The DUPLICATE caller source is still deduped exactly as before.
    assert got.xrefs == ['void callerOne() { root(1); }',
                         'void callerTwo() { root(2); }']
    assert got.xref_names == ['[org.example.D].callerOne()',
                              '[org.example.E].callerTwo()']
    assert len(got.xref_names) == len(got.xrefs)


def test_enrich_records_the_walk_shape_without_touching_reachable():
    fn = _touched()
    project = _Project(GRAPH)
    got = TargetAnalyzer()._enrich_with_xrefs([fn], project, [])[0]
    # The prompt-visible field is exactly what reachable_of produces.
    assert got.reachable[:3] == call_graph.reachable_of(
        project, '[org.example.A].root(int)', 200, 3)
    assert ['[org.example.A].root(int)',
            '[org.example.A].mid(int)'] in got.reachable_edges
    assert got.reachable_depth['[org.example.A].root(int)'] == 0
    # JSON-clean: edges are lists of strings, depths are str -> int.
    json.dumps({'e': got.reachable_edges, 'd': got.reachable_depth})


def test_walk_shape_is_empty_and_harmless_without_an_introspector():
    fn = _touched()
    notes = []
    out = TargetAnalyzer()._enrich_with_xrefs([fn], None, notes)
    assert notes == ['introspector_unavailable']
    assert out[0].reachable_edges == []
    assert out[0].reachable_depth == {}
    assert out[0].xref_names == []


# ---- the file write itself ------------------------------------------------

class _Args:
    def __init__(self, results_json=None, coverage=False, naive=False):
        self.results_json = results_json
        self.coverage = coverage
        self.naive = naive


def _context():
    fn = _touched()
    fn.xrefs = ['void callerOne() { root(1); }']
    fn.xref_names = ['[org.example.D].callerOne()']
    fn.reachable = ['[org.example.A].mid(int)']
    fn.reachable_edges = [['[org.example.A].root(int)',
                           '[org.example.A].mid(int)']]
    fn.reachable_depth = {'[org.example.A].root(int)': 0,
                          '[org.example.A].mid(int)': 1}
    return PatchContext(
        modified_files=['source/org/example/A.java'],
        patch_text='--- a\n+++ b\n',
        functions=[fn],
        package='org.example.deep.pkg',
        root_cause_reachable=['A.mid'],
        reachable_edges=fn.reachable_edges,
        reachable_depth=fn.reachable_depth,
    )


def test_context_json_is_written_beside_result_jsonl(tmp_path):
    leg = tmp_path / 'leg_01'
    leg.mkdir()
    run_mod._write_context_json(
        _Args(results_json=str(leg / 'result.jsonl')), _context())
    written = json.loads((leg / 'context.json').read_text())
    # It IS the context dict — same object the trace records.
    assert written['package'] == 'org.example.deep.pkg'
    assert written['root_cause_reachable'] == ['A.mid']
    fn = written['functions'][0]
    assert fn['xrefs'] == ['void callerOne() { root(1); }']
    assert fn['xref_names'] == ['[org.example.D].callerOne()']
    assert fn['reachable_edges'] == [['[org.example.A].root(int)',
                                      '[org.example.A].mid(int)']]
    assert fn['reachable_depth']['[org.example.A].mid(int)'] == 1
    assert written['reachable_edges'] == fn['reachable_edges']
    assert written['reachable_depth']['[org.example.A].root(int)'] == 0


def test_context_json_is_skipped_without_a_leg_directory(tmp_path):
    """No --results_json means no leg directory; the hook does nothing
    rather than guessing a path."""
    run_mod._write_context_json(_Args(results_json=None), _context())
    assert list(tmp_path.iterdir()) == []


def test_context_json_write_failure_is_not_fatal(tmp_path, capsys):
    bad = tmp_path / 'not_a_dir'
    bad.write_text('x')
    run_mod._write_context_json(
        _Args(results_json=str(bad / 'result.jsonl')), _context())
    assert 'context.json' in capsys.readouterr().out


# ===========================================================================
# HOOK 2 — --coverage adds two Jazzer flags and nothing else
# ===========================================================================

class _FakeProc:
    returncode = 0
    stdout = ''
    stderr = ''


@pytest.fixture
def captured_jazzer(monkeypatch):
    """Record the (cmd, env) every run_jazzer invocation would have run."""
    calls = []

    def _fake_run(cmd, **kwargs):
        calls.append((list(cmd), kwargs.get('env')))
        return _FakeProc()

    monkeypatch.setattr(fuzz_runner.subprocess, 'run', _fake_run)
    return calls


def _invoke(tmp_path, **extra):
    fuzz_runner.run_jazzer(
        jazzer_standalone_jar='/jars/jazzer.jar',
        target_class='FuzzHarness',
        harness_dir=str(tmp_path),
        project_cp='/cp/classes',
        timeout_seconds=20,
        jazzer_api_jar='/jars/jazzer-api.jar',
        keep_going=8,
        **extra,
    )


def test_jazzer_command_is_byte_identical_without_the_flag(
        captured_jazzer, tmp_path):
    """The OFF path. Passing neither coverage argument, passing them as
    None, and passing only one of them must all produce the SAME command
    and the same (absent) environment override."""
    _invoke(tmp_path)
    _invoke(tmp_path, coverage_dump=None, coverage_include=None)
    _invoke(tmp_path, coverage_dump=str(tmp_path / 'x.exec'),
            coverage_include=None)
    _invoke(tmp_path, coverage_dump=None, coverage_include='org.example.**')
    cmds = [c for c, _env in captured_jazzer]
    envs = [e for _c, e in captured_jazzer]
    assert cmds[0] == cmds[1] == cmds[2] == cmds[3]
    assert envs == [None, None, None, None]
    assert not any(a.startswith('--coverage_dump') for a in cmds[0])
    assert not any(a.startswith('--instrumentation_includes')
                   for a in cmds[0])


def test_coverage_adds_exactly_the_two_flags(captured_jazzer, tmp_path):
    from java.execution.coverage_flags import jazzer_coverage_args
    dump = str(tmp_path / 'cov' / 'attempt_002_patched.exec')
    _invoke(tmp_path)
    _invoke(tmp_path, coverage_dump=dump, coverage_include='org.example.**')
    off, on = captured_jazzer[0][0], captured_jazzer[1][0]
    expected = jazzer_coverage_args(dump, 'org.example.**')
    assert len(expected) == 2
    # Exactly those two arguments are added, and every other argument is
    # unchanged and in the same order.
    assert [a for a in on if a not in expected] == off
    assert [a for a in on if a in expected] == expected
    # They are Jazzer's flags, so they come before libFuzzer's separator.
    assert on.index(expected[0]) < on.index('--')
    # The env is still untouched (that channel belongs to --diffcov).
    assert captured_jazzer[1][1] is None


def test_coverage_dump_directory_is_created_and_stale_dumps_removed(
        captured_jazzer, tmp_path):
    dump = tmp_path / 'cov' / 'attempt_001_buggy.exec'
    dump.parent.mkdir()
    dump.write_text('stale')
    _invoke(tmp_path, coverage_dump=str(dump),
            coverage_include='org.example.**')
    assert not dump.exists()      # never merge a previous run's data
    nested = tmp_path / 'deeper' / 'cov' / 'a_patched.exec'
    _invoke(tmp_path, coverage_dump=str(nested),
            coverage_include='org.example.**')
    assert nested.parent.is_dir()


def test_raw_fuzz_output_is_saved_only_with_the_flag(monkeypatch, tmp_path):
    """The `.exec` file says which lines ran; this file is the run's own
    account of itself (which oracles fired, what libFuzzer did, how a
    killed run was spending its budget) and is otherwise discarded once
    the caller has taken the fields it wants."""
    class _Talkative:
        returncode = 0
        stdout = 'INFO: seed corpus\n[oracle:sum] metamorphic violation'
        stderr = '== Java Exception: java.lang.IllegalStateException'

    monkeypatch.setattr(fuzz_runner.subprocess, 'run',
                        lambda cmd, **kw: _Talkative())
    out = tmp_path / 'fuzz_out' / 'attempt_002_patched.txt'

    # Without the flag: nothing is written and no directory is invented.
    _invoke(tmp_path)
    assert not (tmp_path / 'fuzz_out').exists()

    # With it: the RAW combined stream, both halves, verbatim.
    _invoke(tmp_path, output_dump=str(out))
    text = out.read_text()
    assert text == f'{_Talkative.stdout}\n{_Talkative.stderr}'
    assert '[oracle:sum] metamorphic violation' in text
    assert 'java.lang.IllegalStateException' in text


def test_raw_fuzz_output_survives_a_timed_out_run(monkeypatch, tmp_path):
    """A run killed on the wall-clock cap is exactly the one whose log a
    human wants, so it is written after the timeout branch, not before."""
    def _timeout(cmd, **kw):
        raise fuzz_runner.subprocess.TimeoutExpired(
            cmd, 5, output='partial stdout', stderr='partial stderr')

    monkeypatch.setattr(fuzz_runner.subprocess, 'run', _timeout)
    out = tmp_path / 'fuzz_out' / 'attempt_001_buggy.txt'
    outcome = fuzz_runner.run_jazzer(
        jazzer_standalone_jar='/jars/jazzer.jar', target_class='FuzzHarness',
        harness_dir=str(tmp_path), project_cp='/cp', timeout_seconds=5,
        output_dump=str(out))
    assert outcome.timed_out is True
    assert out.read_text() == 'partial stdout\npartial stderr'


def test_saving_the_raw_output_changes_nothing_about_the_outcome(
        captured_jazzer, tmp_path):
    """A plain file write: same command, same env, same returned fields."""
    _invoke(tmp_path)
    _invoke(tmp_path, output_dump=str(tmp_path / 'fuzz_out' / 'a_patched.txt'))
    assert captured_jazzer[0] == captured_jazzer[1]


# ---- the runner's own gating ---------------------------------------------

def test_fuzz_runner_collects_no_dumps_with_the_flag_off():
    runner = fuzz_runner.FuzzRunner(jazzer_standalone_jar='/jars/j.jar')
    assert runner._coverage_on() is False
    assert runner._coverage_dump_path('attempt_001', 'patched') is None
    assert runner._coverage_output_path('attempt_001', 'patched') is None
    assert runner.coverage_dumps == []
    assert runner.coverage_outputs == []
    # The snapshot is a no-op too, so no directory is invented.
    runner.snapshot_coverage_build('/nonexistent/checkout', 'patched')


def test_fuzz_runner_names_dumps_by_harness_and_build(tmp_path):
    runner = fuzz_runner.FuzzRunner(
        jazzer_standalone_jar='/jars/j.jar',
        coverage_dir=str(tmp_path / 'cov'),
        coverage_include='org.example.**',
        coverage_out_dir=str(tmp_path / 'fuzz_out'))
    p1 = runner._coverage_dump_path('attempt_002', 'patched')
    p2 = runner._coverage_dump_path('attempt_002', 'buggy')
    assert os.path.basename(p1) == 'attempt_002_patched.exec'
    assert os.path.basename(p2) == 'attempt_002_buggy.exec'
    # The raw log carries the SAME harness/build name, in fuzz_out/.
    o1 = runner._coverage_output_path('attempt_002', 'patched')
    assert os.path.basename(o1) == 'attempt_002_patched.txt'
    assert os.path.dirname(o1) == str(tmp_path / 'fuzz_out')
    assert [d['exists'] for d in runner.coverage_dumps] == [False, False]
    os.makedirs(os.path.dirname(p1), exist_ok=True)
    with open(p1, 'wb') as fh:
        fh.write(b'exec')
    runner._note_coverage_dump(p1)
    runner._note_coverage_dump(p2)
    assert runner.coverage_dumps[0]['exists'] is True
    assert runner.coverage_dumps[0]['bytes'] == 4
    assert runner.coverage_dumps[1]['exists'] is False


# ---- the acceptance check: every COMPILED candidate ----------------------

def _build_result(tmp_path, label='attempt_002'):
    from java.harness.build import BuildResult
    hdir = tmp_path / label
    hdir.mkdir(parents=True, exist_ok=True)
    (hdir / 'FuzzHarness.java').write_text('class FuzzHarness {}')
    return BuildResult(harness_path=str(hdir / 'FuzzHarness.java'),
                       class_name='FuzzHarness', classpath='/cp',
                       compiled=True, returncode=0, stdout='', stderr='',
                       attempt_label=label)


def _verifier(tmp_path=None, **extra):
    return fuzz_runner.HarnessVerifier(
        jazzer_standalone_jar='/jars/jazzer.jar',
        buggy_classpath='/cp/classes',
        timeout_seconds=20,
        jazzer_api_jar='/jars/jazzer-api.jar',
        **extra)


def test_verifier_command_is_byte_identical_without_the_flag(
        captured_jazzer, tmp_path):
    """The OFF path for the gate that decides which harnesses are KEPT.
    This run is on the pipeline's critical path — a harness is admitted to
    the set only if it crashes here — so an extra flag would change which
    harnesses the whole run has, not merely what is measured."""
    br = _build_result(tmp_path)
    _verifier().verify(br)
    _verifier(coverage_dir=None, coverage_include=None,
              coverage_out_dir=None).verify(br)
    # a directory without a glob, and a glob without a directory: neither
    # is coverage ON, so neither may touch the command
    _verifier(coverage_dir=str(tmp_path / 'cov'),
              coverage_out_dir=str(tmp_path / 'fuzz_out')).verify(br)
    _verifier(coverage_include='org.example.**').verify(br)
    cmds = [c for c, _env in captured_jazzer]
    envs = [e for _c, e in captured_jazzer]
    assert cmds[0] == cmds[1] == cmds[2] == cmds[3]
    assert envs == [None, None, None, None]
    assert not any(a.startswith('--coverage_dump') for a in cmds[0])
    assert not any(a.startswith('--instrumentation_includes') for a in cmds[0])
    assert not (tmp_path / 'cov').exists()
    assert not (tmp_path / 'fuzz_out').exists()


def test_verifier_carries_exactly_the_two_flags(captured_jazzer, tmp_path):
    """With the flag, the acceptance run gets the SAME two Jazzer flags the
    fuzz runs get — and nothing else changes about its command."""
    from java.execution.coverage_flags import jazzer_coverage_args
    br = _build_result(tmp_path)
    _verifier().verify(br)
    v = _verifier(coverage_dir=str(tmp_path / 'cov'),
                  coverage_include='org.example.**',
                  coverage_out_dir=str(tmp_path / 'fuzz_out'))
    v.verify(br)
    off, on = captured_jazzer[0][0], captured_jazzer[1][0]
    expected = jazzer_coverage_args(
        str(tmp_path / 'cov' / 'attempt_002_compiled.exec'),
        'org.example.**')
    assert len(expected) == 2
    assert [a for a in on if a not in expected] == off
    assert [a for a in on if a in expected] == expected
    assert on.index(expected[0]) < on.index('--')
    assert captured_jazzer[1][1] is None


def test_verifier_names_its_dumps_with_the_compiled_build_token(tmp_path):
    """`compiled` = the buggy build, every candidate that compiled. The
    token has no underscore, because the measurement side recovers
    (harness, build) by splitting on the LAST one."""
    v = _verifier(coverage_dir=str(tmp_path / 'cov'),
                  coverage_include='org.example.**',
                  coverage_out_dir=str(tmp_path / 'fuzz_out'))
    dump = v._coverage_dump_path('attempt_002', 'compiled')
    out = v._coverage_output_path('attempt_002', 'compiled')
    assert os.path.basename(dump) == 'attempt_002_compiled.exec'
    assert os.path.dirname(dump) == str(tmp_path / 'cov')
    assert os.path.basename(out) == 'attempt_002_compiled.txt'
    assert os.path.dirname(out) == str(tmp_path / 'fuzz_out')
    assert '_' not in 'compiled'
    from java.measurements.coverage import _split_exec_name
    assert _split_exec_name('attempt_002_compiled') == ('attempt_002',
                                                        'compiled')
    # and with the flag off there is nothing to name
    assert _verifier()._coverage_on() is False
    assert _verifier()._coverage_dump_path('attempt_002', 'compiled') is None
    assert _verifier().coverage_dumps == []


def test_verifier_records_one_dump_and_one_log_per_candidate(
        monkeypatch, tmp_path):
    """Every compiled candidate is run here exactly once — the rejected
    ones are never run again — so this is where the "all compiled
    harnesses" coverage set comes from. The record lists a candidate whose
    dump Jazzer failed to write too, which a silently missing file would
    not."""
    class _Talkative:
        returncode = 0
        stdout = 'INFO: seed corpus'
        stderr = '== Java Exception: java.lang.IllegalStateException'

    def _run(cmd, **kw):
        dump = [a for a in cmd if a.startswith('--coverage_dump=')]
        if dump and 'attempt_001' in dump[0]:
            path = dump[0].split('=', 1)[1]
            with open(path, 'wb') as fh:
                fh.write(b'exec')
        return _Talkative()

    monkeypatch.setattr(fuzz_runner.subprocess, 'run', _run)
    v = _verifier(coverage_dir=str(tmp_path / 'cov'),
                  coverage_include='org.example.**',
                  coverage_out_dir=str(tmp_path / 'fuzz_out'))
    v.verify(_build_result(tmp_path, 'attempt_001'))
    v.verify(_build_result(tmp_path, 'attempt_002'))

    assert [(d['harness'], d['build'], d['exists']) for d in v.coverage_dumps] \
        == [('attempt_001', 'compiled', True),
            ('attempt_002', 'compiled', False)]
    assert [os.path.basename(p) for p in v.coverage_outputs] == [
        'attempt_001_compiled.txt', 'attempt_002_compiled.txt']
    raw = (tmp_path / 'fuzz_out' / 'attempt_001_compiled.txt').read_text()
    assert raw == f'{_Talkative.stdout}\n{_Talkative.stderr}'


def test_verifier_snapshots_the_buggy_classes_once(monkeypatch, tmp_path):
    """The post-hoc report needs the buggy build's class files, and the
    latent-oracle scan only snapshots them when the campaign KEPT a
    harness — a leg that kept none is exactly the leg whose compiled-set
    coverage matters, so the verifier does it itself, once."""
    monkeypatch.setattr(fuzz_runner.subprocess, 'run',
                        lambda cmd, **kw: _FakeProc())
    calls = []
    monkeypatch.setattr(fuzz_runner, 'snapshot_coverage_classpath',
                        lambda *a: calls.append(a))
    v = _verifier(coverage_dir=str(tmp_path / 'cov'),
                  coverage_include='org.example.**',
                  coverage_out_dir=str(tmp_path / 'fuzz_out'),
                  coverage_checkout='/checkout/lang_1_buggy')
    v.verify(_build_result(tmp_path, 'attempt_001'))
    v.verify(_build_result(tmp_path, 'attempt_002'))
    assert calls == [('/checkout/lang_1_buggy', str(tmp_path / 'cov'),
                      'buggy', 'org.example.**')]
    # with the flag off nothing is snapshotted at all
    _verifier(coverage_checkout='/checkout/lang_1_buggy').verify(
        _build_result(tmp_path, 'attempt_003'))
    assert len(calls) == 1


# ---- the classpath snapshot ----------------------------------------------

def test_snapshot_copies_the_class_files_and_merges_both_builds(
        tmp_path, monkeypatch):
    """JaCoCo needs the exact class files that were instrumented, and the
    patched working copy does not survive the run — so they are copied
    out, and one classpath.json describes both sides of the leg."""
    checkout = tmp_path / 'checkout'
    (checkout / 'target' / 'classes' / 'org').mkdir(parents=True)
    (checkout / 'target' / 'classes' / 'org' / 'A.class').write_bytes(b'CAFE')
    (checkout / 'source').mkdir()
    cov = tmp_path / 'cov'

    monkeypatch.setattr(fuzz_runner, 'd4j_export',
                        lambda prop, d: {'dir.bin.classes': 'target/classes',
                                         'dir.src.classes': 'source'}[prop])
    merged = fuzz_runner.snapshot_coverage_classpath(
        str(checkout), str(cov), 'patched', 'org.example.**')
    assert (cov / 'classes_patched' / 'org' / 'A.class').read_bytes() == b'CAFE'
    assert merged['include_glob'] == 'org.example.**'
    assert merged['class_dirs'] == [str(cov / 'classes_patched')]
    assert merged['source_dirs'] == [str(checkout / 'source')]

    merged2 = fuzz_runner.snapshot_coverage_classpath(
        str(checkout), str(cov), 'buggy', 'org.example.**')
    assert merged2['class_dirs'] == [str(cov / 'classes_patched'),
                                     str(cov / 'classes_buggy')]
    on_disk = json.loads((cov / 'classpath.json').read_text())
    assert on_disk == merged2


def test_snapshot_survives_an_unresolvable_checkout(tmp_path, monkeypatch):
    monkeypatch.setattr(fuzz_runner, 'd4j_export', lambda prop, d: '')
    info = fuzz_runner.snapshot_coverage_classpath(
        str(tmp_path / 'nope'), str(tmp_path / 'cov'), 'patched', 'org.**')
    assert info['class_dirs'] == []
    assert info['include_glob'] == 'org.**'


# ---- run.py's own coverage gating ----------------------------------------

def test_coverage_setup_is_off_without_the_flag(tmp_path):
    args = _Args(results_json=str(tmp_path / 'result.jsonl'), coverage=False)
    assert run_mod._coverage_setup(args, _context()) == (None, '', None)
    assert not (tmp_path / 'cov').exists()
    assert not (tmp_path / 'fuzz_out').exists()


def test_coverage_setup_needs_a_leg_directory(capsys):
    args = _Args(results_json=None, coverage=True)
    assert run_mod._coverage_setup(args, _context()) == (None, '', None)
    assert '--results_json' in capsys.readouterr().out


def test_coverage_setup_derives_the_include_glob(tmp_path):
    args = _Args(results_json=str(tmp_path / 'result.jsonl'), coverage=True)
    cov_dir, glob_, out_dir = run_mod._coverage_setup(args, _context())
    # The project PREFIX (first three package components), not the full
    # package — the neighbouring code of the patch is in sibling packages.
    assert glob_ == 'org.example.deep.**'
    assert cov_dir == str(tmp_path / 'cov')
    assert out_dir == str(tmp_path / 'fuzz_out')
    assert os.path.isdir(cov_dir) and os.path.isdir(out_dir)


def test_include_glob_falls_back_to_the_touched_class_package():
    ctx = _context()
    ctx.package = None
    ctx.functions[0].func_class_fq = 'org.example.deep.pkg.A'
    assert run_mod._coverage_include_glob(ctx) == 'org.example.deep.**'


def test_include_glob_is_empty_when_no_package_is_known(capsys):
    ctx = _context()
    ctx.package = None
    ctx.functions[0].func_class_fq = None
    assert run_mod._coverage_include_glob(ctx) == ''


def test_record_coverage_is_a_no_op_with_the_flag_off():
    extras = {}
    run_mod._record_coverage([], None, extras)
    assert extras == {}


def test_record_coverage_lists_every_dump(tmp_path):
    runner = fuzz_runner.FuzzRunner(
        jazzer_standalone_jar='/jars/j.jar',
        coverage_dir=str(tmp_path), coverage_include='org.example.**',
        coverage_out_dir=str(tmp_path / 'out'))
    runner._coverage_dump_path('attempt_001', 'patched')
    runner._coverage_output_path('attempt_001', 'patched')
    extras = {}
    run_mod._record_coverage([runner], str(tmp_path), extras)
    assert extras['coverage']['dumps'][0]['harness'] == 'attempt_001'
    assert extras['coverage']['outputs'] == [
        str(tmp_path / 'out' / 'attempt_001_patched.txt')]
    assert extras['coverage']['classpath'] is None    # none written yet
    assert extras['coverage']['dir'] == str(tmp_path)


def test_record_coverage_separates_compiled_from_accepted(tmp_path):
    """Two harness SETS come out of one leg: every candidate the
    acceptance gate ran (build `compiled`) and the subset it kept. The
    record names both, so a later reader never has to re-derive the gate's
    decision from the trace to know which set a coverage number is over."""
    verifier = fuzz_runner.HarnessVerifier(
        jazzer_standalone_jar='/jars/j.jar', buggy_classpath='/cp',
        coverage_dir=str(tmp_path), coverage_include='org.example.**',
        coverage_out_dir=str(tmp_path / 'out'))
    for label in ('attempt_001', 'attempt_002', 'attempt_003'):
        verifier._coverage_dump_path(label, 'compiled')
    runner = fuzz_runner.FuzzRunner(
        jazzer_standalone_jar='/jars/j.jar',
        coverage_dir=str(tmp_path), coverage_include='org.example.**',
        coverage_out_dir=str(tmp_path / 'out'))
    runner._coverage_dump_path('attempt_002', 'patched')
    extras = {}
    run_mod._record_coverage([runner, verifier], str(tmp_path), extras,
                             accepted=['attempt_002'])
    cov = extras['coverage']
    assert cov['compiled_attempts'] == ['attempt_001', 'attempt_002',
                                        'attempt_003']
    assert cov['accepted_attempts'] == ['attempt_002']
    assert {d['build'] for d in cov['dumps']} == {'compiled', 'patched'}


def test_record_coverage_accepts_no_harnesses_at_all(tmp_path):
    """The leg the compiled set exists for: the gate kept nothing, so the
    kept-side lists are empty and every candidate is still accounted for."""
    verifier = fuzz_runner.HarnessVerifier(
        jazzer_standalone_jar='/jars/j.jar', buggy_classpath='/cp',
        coverage_dir=str(tmp_path), coverage_include='org.example.**',
        coverage_out_dir=str(tmp_path / 'out'))
    verifier._coverage_dump_path('attempt_001', 'compiled')
    extras = {}
    run_mod._record_coverage([verifier], str(tmp_path), extras, accepted=[])
    assert extras['coverage']['compiled_attempts'] == ['attempt_001']
    assert extras['coverage']['accepted_attempts'] == []


# ===========================================================================
# HOOK 3 — --naive drops the three root-cause-conditioning insertions
# ===========================================================================

# The marker strings of the three insertions. If a prompt section is
# renamed, this list is what says so out loud.
VARIANT_MARKERS = ['<root_cause_reachable>',
                   'This harness is ONE of a set probing the root cause']
XREF_MARKERS = ['<xref>', 'Call-site examples']
# The callee half of the same neighbourhood: declarations (and concrete
# implementations) of what the patched method calls.
CALLEE_MARKERS = ['<callee', 'whose behaviour the patched',
                  'indexOfIgnoreCase']
CLAUSE_MARKER = 'or a function listed in'


@pytest.fixture
def prompt_context():
    fn = TouchedFunction(
        func_name='substringBetween',
        func_signature='String substringBetween(String, String)',
        func_source=('public static String substringBetween(String s, '
                     'String tag) {\n    return s.substring(1);\n}'),
        func_class='StringUtils',
        func_class_fq='org.apache.commons.lang3.StringUtils',
        func_param_types=['String', 'String'],
        xrefs=['void caller() { StringUtils.substringBetween("", "x"); }'],
        xref_names=['[org.apache.commons.lang3.Caller].caller()'],
        reachable=['[org.apache.commons.lang3.StringUtils].substring(int)'],
        related_callees=[RelatedCallee(
            name='indexOfIgnoreCase',
            source_file='StringUtils.java',
            signature='static int indexOfIgnoreCase(CharSequence, int)',
            source=('static int indexOfIgnoreCase(CharSequence s, int p) '
                    '{\n    return -1;\n}'),
        )],
    )
    return PatchContext(
        modified_files=['src/main/java/org/apache/commons/lang3/'
                        'StringUtils.java'],
        patch_text=('--- a/StringUtils.java\n+++ b/StringUtils.java\n'
                    '-    return s.substring(1);\n'
                    '+    if (s.isEmpty()) return null;\n'),
        functions=[fn],
        package='org.apache.commons.lang3',
        root_cause_reachable=['StringUtils.substring', 'StringUtils.indexOf'],
        source_imports=['import java.util.List;'],
    )


@pytest.fixture
def prompt_test():
    return FailureTest(
        test_class='org.apache.commons.lang3.StringUtilsTest',
        test_method='testSubstringBetween',
        source_path='/checkout/StringUtilsTest.java',
        method_source=('public void testSubstringBetween() {\n'
                       '    StringUtils.substringBetween("", "x");\n}'),
        exception_type='java.lang.StringIndexOutOfBoundsException',
    )


def _text(builder, context, tests, **kw):
    return builder.build(buggy_dir='/checkout/lang_1_buggy',
                         context=context, failure_tests=tests,
                         covered_functions=['StringUtils.indexOf'],
                         found_signatures=['NullPointerException'],
                         **kw)[1]['content']


@pytest.mark.parametrize('bug_kind', ['crashing', 'semantic'])
def test_default_prompt_is_unchanged_and_the_flag_defaults_to_false(
        prompt_context, prompt_test, bug_kind):
    """(ii) The OFF path, snapshotted the only way that means anything:
    the builder called WITHOUT the new argument must produce exactly what
    the builder called with the argument explicitly False produces. The
    default is the pre-change behaviour, on both bug paths."""
    kw = {'bug_kind': bug_kind}
    if bug_kind == 'semantic':
        kw['semantic_test'] = prompt_test
    before = _text(PromptBuilder(), prompt_context, [prompt_test], **kw)
    after = _text(PromptBuilder(naive=False), prompt_context,
                  [prompt_test], **kw)
    assert before == after
    assert PromptBuilder().naive is False


@pytest.mark.parametrize('bug_kind', ['crashing', 'semantic'])
def test_conditioned_prompt_carries_all_three_insertions(
        prompt_context, prompt_test, bug_kind):
    """The guard that keeps the naive assertions from being vacuous: with
    the flag off, every marker the next test looks for is present."""
    kw = {'bug_kind': bug_kind}
    if bug_kind == 'semantic':
        kw['semantic_test'] = prompt_test
    text = _text(PromptBuilder(), prompt_context, [prompt_test], **kw)
    for marker in VARIANT_MARKERS + CALLEE_MARKERS:
        assert marker in text
    if bug_kind == 'crashing':
        # The xref and propagation-clause insertions live on the crashing
        # path's function/failure-test blocks.
        for marker in XREF_MARKERS:
            assert marker in text
        assert CLAUSE_MARKER in text
    else:
        for marker in XREF_MARKERS:
            assert marker in text


@pytest.mark.parametrize('bug_kind', ['crashing', 'semantic'])
def test_naive_prompt_drops_all_three_insertions(
        prompt_context, prompt_test, bug_kind):
    """(i) With the flag, none of the three blocks' marker strings appear
    — including the dangling reference to a section that is no longer
    there."""
    kw = {'bug_kind': bug_kind}
    if bug_kind == 'semantic':
        kw['semantic_test'] = prompt_test
    text = _text(PromptBuilder(naive=True), prompt_context,
                 [prompt_test], **kw)
    for marker in (VARIANT_MARKERS + XREF_MARKERS + CALLEE_MARKERS
                   + [CLAUSE_MARKER]):
        assert marker not in text, marker


@pytest.mark.parametrize('bug_kind', ['crashing', 'semantic'])
def test_naive_only_ever_removes_text(prompt_context, prompt_test, bug_kind):
    """"Everything else identical", checked line by line rather than by
    eye: diff the two prompts and require every difference to be a
    DELETION belonging to one of the three insertions. The one exception
    is the propagation rule, whose line is rewritten because a clause was
    cut out of the middle of it — so that rewrite must shorten the line
    and change nothing else about it."""
    import difflib
    kw = {'bug_kind': bug_kind}
    if bug_kind == 'semantic':
        kw['semantic_test'] = prompt_test
    full = _text(PromptBuilder(), prompt_context, [prompt_test], **kw)
    naive = _text(PromptBuilder(naive=True), prompt_context,
                  [prompt_test], **kw)
    a, b = full.splitlines(), naive.splitlines()
    # The callee block is rendered from the fixture, so its exact lines are
    # available rather than guessed at with substrings.
    callee_lines = set(PromptBuilder()._related_callees_block(
        prompt_context.functions[0]).splitlines())
    assert callee_lines
    insertion_markers = VARIANT_MARKERS + XREF_MARKERS + CALLEE_MARKERS + [
        'Already covered by earlier harnesses',
        'Functions covered:', 'Crashes already found:',
        'Uncovered functions to steer toward:',
        '- StringUtils.substring', '- StringUtils.indexOf',
        '- NullPointerException',
        'If the crash you plan to reproduce has the SAME',
        '(a) lives in this region', '(b) stems from the SAME root cause',
        'Prefer a check whose', 'Rotate the', 'STRATEGY',
    ]
    for tag, i1, i2, j1, j2 in difflib.SequenceMatcher(
            None, a, b).get_opcodes():
        if tag == 'equal':
            continue
        assert tag != 'insert', f"naive ADDED text: {b[j1:j2]}"
        if tag == 'replace':
            # Only the propagation-rule line, and only shorter.
            assert i2 - i1 == 1 and j2 - j1 == 1, (a[i1:i2], b[j1:j2])
            assert 'PROPAGATE a throwable only when BOTH hold' in a[i1]
            assert len(b[j1]) < len(a[i1])
            assert b[j1] == a[i1].replace(
                ' or a function listed in <root_cause_reachable>', '')
            continue
        # A deletion must be part of one of the three insertions (or the
        # blank line that separated the block from its neighbour).
        removed = '\n'.join(a[i1:i2])
        assert (not removed.strip()
                or any(m in removed for m in insertion_markers)
                or set(a[i1:i2]) <= callee_lines), removed
    # And the sections that have nothing to do with conditioning survive.
    assert prompt_context.patch_text in naive
    assert prompt_context.functions[0].func_source in naive
    assert 'import java.util.List;' in naive
    assert 'FuzzedDataProvider' in naive


def test_naive_keeps_the_propagation_rule_minus_the_reachable_clause(
        prompt_context, prompt_test):
    naive = _text(PromptBuilder(naive=True), prompt_context,
                  [prompt_test], bug_kind='crashing')
    # The rule itself is still there; only the clause that widened it to a
    # region the naive prompt no longer describes is gone.
    assert 'PROPAGATE a throwable only when BOTH hold' in naive
    assert 'its stack trace passes through `substringBetween`.' in naive


# ===========================================================================
# HOOK 3b — --naive drops the neighbourhood from the RULE-SYNTHESIS prompt
# ===========================================================================
#
# The first --naive pilot ablated only the harness prompt. The synthesis
# prompt kept its "Reachable API" line, so both arms of the comparison were
# handed the callee set and the ablation measured less than it claimed.
# These tests pin the same three properties as the harness-prompt ones: the
# OFF path is byte-identical, the flag defaults to False, and the ON path
# differs by removals only.

REACHABLE_MARKER = 'Reachable API (call these, do not reimplement):'


class _NullGenerator:
    """A generator that returns nothing. `synthesize` stashes the exact
    prompt in `last_prompt` before parsing, so the prompt text is testable
    without an LLM (the empty reply just yields zero candidates)."""

    def __init__(self):
        self.calls = []

    def generate(self, messages):
        self.calls.append(messages)
        return ''


SYNTH_REACHABLE = ['StringUtils.substring', 'StringUtils.indexOf']


def _synth_prompt(**kw):
    """Build the synthesis prompt and return (context, instructions)."""
    synth = RelationSynthesizer(_NullGenerator(), **kw)
    synth.synthesize(
        patched_sources=['public static String substringBetween(String s,'
                         ' String tag) {\n    return s.substring(1);\n}'],
        class_name='StringUtils',
        reachable=SYNTH_REACHABLE,
        mined_tests=[],
        trigger_summary='StringIndexOutOfBoundsException',
        patch_text=('--- a/StringUtils.java\n+++ b/StringUtils.java\n'
                    '-    return s.substring(1);\n'
                    '+    if (s.isEmpty()) return null;\n'),
        javadocs=['/** Returns the substring between the tags. */'],
        class_context=['<class name="StringUtils" role="patched"/>'],
        source_imports=['import java.util.List;'],
        trigger_test_block='// StringUtilsTest::testSubstringBetween',
        trigger_methods=['substringBetween'],
    )
    return synth.last_prompt['context'], synth.last_prompt['instructions']


@pytest.mark.parametrize('focused', [False, True])
def test_default_synthesis_prompt_is_unchanged_and_defaults_to_false(focused):
    """(ii) The OFF path: the synthesizer built WITHOUT the new argument
    must produce exactly the prompt the synthesizer built with it
    explicitly False produces, on both the broad and the focused path."""
    before = _synth_prompt(focused=focused)
    after = _synth_prompt(focused=focused, naive=False)
    assert before == after
    assert RelationSynthesizer(_NullGenerator()).naive is False


@pytest.mark.parametrize('focused', [False, True])
def test_conditioned_synthesis_prompt_carries_the_reachable_line(focused):
    """The guard that keeps the naive assertion from being vacuous."""
    ctx, _instr = _synth_prompt(focused=focused)
    assert REACHABLE_MARKER in ctx
    for name in SYNTH_REACHABLE:
        assert name in ctx


@pytest.mark.parametrize('focused', [False, True])
def test_naive_synthesis_prompt_drops_the_reachable_line(focused):
    """(i) With the flag, the callee set is gone — the line AND the method
    names it listed. This is the leak the pilot shipped with: nine
    occurrences of this line in each arm's trace."""
    ctx, _instr = _synth_prompt(focused=focused, naive=True)
    assert REACHABLE_MARKER not in ctx
    for name in SYNTH_REACHABLE:
        assert name not in ctx


@pytest.mark.parametrize('focused', [False, True])
def test_naive_synthesis_only_ever_removes_text(focused):
    """"Everything else identical", line by line: every difference must be
    a DELETION of the reachable line. The permitted level-B grounding —
    the failing test, the patch, the patched class's own source, javadoc
    and imports — must survive untouched."""
    import difflib
    full_ctx, full_instr = _synth_prompt(focused=focused)
    naive_ctx, naive_instr = _synth_prompt(focused=focused, naive=True)
    assert full_instr == naive_instr
    a, b = full_ctx.splitlines(), naive_ctx.splitlines()
    for tag, i1, i2, j1, j2 in difflib.SequenceMatcher(
            None, a, b).get_opcodes():
        if tag == 'equal':
            continue
        assert tag == 'delete', (tag, a[i1:i2], b[j1:j2])
        removed = '\n'.join(a[i1:i2])
        assert not removed.strip() or REACHABLE_MARKER in removed, removed
    assert '// StringUtilsTest::testSubstringBetween' in naive_ctx
    assert 'return s.substring(1);' in naive_ctx
    assert 'import java.util.List;' in naive_ctx
    assert 'Returns the substring between the tags.' in naive_ctx
    assert 'THE PATCH-CHANGED CLASS: StringUtils' in naive_ctx


def test_naive_scope_is_recorded_and_names_both_prompt_builders():
    """The leg record must say WHICH prompt builders honoured the flag —
    `naive: True` alone cannot distinguish a fully unconditioned leg from
    one whose synthesis prompt still carried the neighbourhood."""
    src = open(os.path.join(ROOT, 'src', 'java', 'run.py'),
               encoding='utf-8').read()
    assert "record_extras['naive_scope']" in src
    i = src.index("record_extras['naive_scope']")
    block = src[i:i + 900]
    assert 'harness_prompt' in block
    assert 'relation_synth' in block
    # And the synthesizer is actually constructed with the flag.
    assert "naive=getattr(args, 'naive', False))" in src
