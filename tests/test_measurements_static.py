"""F_stat — the STATIC fuzzer-reachable set, end to end.

Station: `java.measurements.static_reach` (the set), `metrics.py` (the
`__stat` keys and the stat-versus-dyn sizes), `aggregate.py` (the extra
Table 3 block), and the `--coverage` hook in
`execution.fuzz_runner.HarnessVerifier` that saves the harness sources the
set is read from.

Failure mode this targets: a static set that quietly means something other
than what its name says. Three ways that happens, and one test each:

  * the entry extraction picks up calls that are not library calls (the
    JDK, the Jazzer API, the harness's own helpers) or misses ones that
    are (a call inside a lambda body, a constructor), so F_stat is a set
    of something else;
  * a `stat` number is put in a `dyn` column, or vice versa — the fifth
    key slot exists to make that impossible and is asserted directly;
  * the hook that saves the sources changes the run it is measuring. The
    OFF path is pinned here as it is for every other hook.
"""
import json
import os
import sys

import pytest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, 'src'))

from java.execution import fuzz_runner                       # noqa: E402
from java.measurements import aggregate as A                 # noqa: E402
from java.measurements import locations as loc               # noqa: E402
from java.measurements import metrics as M                   # noqa: E402
from java.measurements import static_reach as SR             # noqa: E402
import run as run_mod                                        # noqa: E402


# ---------------------------------------------------------------------------
# A tiny library, as a call graph and as a method list
# ---------------------------------------------------------------------------

class _Profile:
    def __init__(self, name, callees):
        self.name = name
        self.base_callsites = [[c, 0] for c in callees]


class _Project:
    def __init__(self, graph):
        self.all_functions = [_Profile(n, cs) for n, cs in graph.items()]


#: Alpha.a -> Beta.e ; Beta.c -> Gamma.h -> Gamma.deep
GRAPH = {
    '[org.ex.Alpha].<init>()': [],
    '[org.ex.Alpha].a()': ['[org.ex.Beta].e()'],
    '[org.ex.Beta].<init>(int)': [],
    '[org.ex.Beta].c()': ['[org.ex.Gamma].h()'],
    '[org.ex.Beta].d(int)': [],
    '[org.ex.Beta].e()': [],
    '[org.ex.Gamma].h()': ['[org.ex.Gamma].deep()'],
    '[org.ex.Gamma].deep()': [],
    '[org.ex.Delta].only(int)': [],
}

A_new = loc.MethodRef('org.ex.Alpha', '<init>', ())
A_a = loc.MethodRef('org.ex.Alpha', 'a', ())
B_new = loc.MethodRef('org.ex.Beta', '<init>', ('int',))
B_c = loc.MethodRef('org.ex.Beta', 'c', ())
B_d = loc.MethodRef('org.ex.Beta', 'd', ('int',))
B_e = loc.MethodRef('org.ex.Beta', 'e', ())
G_h = loc.MethodRef('org.ex.Gamma', 'h', ())
G_deep = loc.MethodRef('org.ex.Gamma', 'deep', ())
D_only = loc.MethodRef('org.ex.Delta', 'only', ('int',))


HARNESS = '''
package org.ex;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.ArrayList;
import java.util.List;

public class FuzzHarness {

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int n = data.consumeInt(0, 10);          // Jazzer API: not library
        Alpha al = new Alpha();                  // constructor
        al.a();                                  // receiver of declared type
        Beta b = new Beta(n);                    // constructor with an arg
        Math.abs(n);                             // JDK: must be ignored
        List<String> xs = new ArrayList<String>();
        xs.forEach(s -> b.d(n));                 // call inside a lambda body
        helper(b);                               // the harness's own method
    }

    private static void helper(Beta b) {
        b.c();
    }
}
'''


@pytest.fixture
def project():
    return _Project(GRAPH)


@pytest.fixture
def index(project):
    return SR.project_index(project)[0]


# ===========================================================================
# entry extraction
# ===========================================================================

def test_entries_are_the_library_calls_the_source_writes(index):
    """What the harness calls DIRECTLY, and only that.

    A receiver whose declared type the file shows resolves through that
    type; `new Beta(n)` is a constructor; a call written inside a lambda
    body is a call like any other. The JDK call, the Jazzer API call and
    the harness's own helper are not library methods and must not appear —
    the last one especially, because the index's receiver rescue would
    otherwise be free to attribute `helper/1` to any one-argument library
    method that happens to share the name."""
    entries, unmatched = SR.harness_entries(HARNESS, index)
    assert set(entries) == {A_new, A_a, B_new, B_d, B_c}
    # b.c() sits in the helper's body: the helper is not an entry, the call
    # it makes is.
    assert B_c in entries
    # nothing from the JDK, from Jazzer, or from the harness itself
    assert not [e for e in entries if e.class_fq.startswith(('java.', 'Fuzz'))]
    assert 'helper/1' not in ' '.join(unmatched)
    # `s.length()` is not written here, so nothing failed to resolve
    assert unmatched == []


def test_unresolved_calls_are_counted_not_dropped(index):
    """A call the project's method list cannot place is recorded as it was
    written. A number built on a set that silently lost members cannot be
    told apart from a number built on a set that genuinely had none."""
    src = '''
package org.ex;
public class FuzzHarness {
    public static void fuzzerTestOneInput(Object data) {
        Unknown u = new Unknown();
        u.nothingLikeThis(1, 2, 3);
    }
}
'''
    entries, unmatched = SR.harness_entries(src, index)
    assert entries == []
    assert unmatched == ['Unknown.<init>/0', 'Unknown.nothingLikeThis/3']


def test_a_receiverless_call_is_rescued_by_name_and_arity(index):
    """`locations.MethodIndex`'s rule 4: when the source does not say what
    the receiver is, the unique (name, arity) match in the project is
    taken. `only/1` is unique, so it resolves; a name several classes
    declare would stay unmatched rather than be guessed at."""
    src = '''
package org.ex;
public class FuzzHarness {
    public static void fuzzerTestOneInput(Object data) {
        make().only(3);
    }
    private static Delta make() { return null; }
}
'''
    entries, unmatched = SR.harness_entries(src, index)
    assert entries == [D_only]
    assert unmatched == []


# ===========================================================================
# the walk down
# ===========================================================================

def test_reach_walks_down_from_the_entries(project, index):
    """F_stat is the entries plus everything the call graph reaches from
    them, with the edges the walk crossed. `Gamma.deep` is two steps below
    an entry and is in; `Delta.only` is in the project and is not, because
    nothing the harness calls leads there."""
    entries, _ = SR.harness_entries(HARNESS, index)
    reach = SR.static_reach(entries, project)
    assert reach.methods == {A_new, A_a, B_new, B_c, B_d,   # entries
                             B_e, G_h, G_deep}              # walked to
    assert D_only not in reach.methods
    assert set(reach.entries) == set(entries)
    assert (A_a, B_e) in reach.edges and (G_h, G_deep) in reach.edges
    assert reach.unmatched == []


def test_the_walk_takes_the_same_caps_as_the_patch_derived_set(project, index):
    """P's callee ring and F_stat use one traversal under one pair of caps,
    so the two sets are comparable. A depth of 1 stops before `Gamma.deep`;
    a node cap of 0 admits no callee at all, and the entries survive both
    because they are not walked to."""
    entries, _ = SR.harness_entries(HARNESS, index)
    shallow = SR.static_reach(entries, project, cap=200, depth=1)
    assert G_h in shallow.methods and G_deep not in shallow.methods
    starved = SR.static_reach(entries, project, cap=0, depth=3)
    assert starved.methods == set(entries)

    import config
    default = SR.static_reach(entries, project)
    assert (config.REACHABLE_NODE_CAP, config.REACHABLE_MAX_DEPTH) == (200, 3)
    assert default.methods == SR.static_reach(
        entries, project, cap=config.REACHABLE_NODE_CAP,
        depth=config.REACHABLE_MAX_DEPTH).methods


def test_static_reach_round_trips_through_json(project, index):
    entries, _ = SR.harness_entries(HARNESS, index)
    reach = SR.static_reach(entries, project)
    reach.set_name, reach.source = 'kept', 'harness_src'
    reach.harnesses = ['attempt_001']
    back = SR.StaticReach.from_dict(json.loads(json.dumps(reach.to_dict())))
    assert back.methods == reach.methods
    assert set(back.entries) == set(reach.entries)
    assert back.edges == reach.edges
    assert (back.set_name, back.source, back.harnesses) == \
        ('kept', 'harness_src', ['attempt_001'])


# ===========================================================================
# one leg
# ===========================================================================

def _leg(tmp_path, *, sources=None, accepted=None, trace=None):
    leg = tmp_path / 'run' / '01_patch1-Ex-1-Tool_o'
    leg.mkdir(parents=True, exist_ok=True)
    rec = {'label': 'overfitting', 'status': 'evaluated',
           'bug_kind': 'crashing', 'project': 'Ex', 'bug_id': 1,
           'crashed_on_patch': True}
    if accepted is not None:
        rec['coverage'] = {'accepted_attempts': list(accepted),
                           'compiled_attempts': sorted(sources or {})}
    (leg / 'result.jsonl').write_text(json.dumps(rec) + '\n')
    if sources:
        sdir = leg / 'harness_src'
        sdir.mkdir(exist_ok=True)
        for label, text in sources.items():
            (sdir / f'{label}.java').write_text(text)
    (leg / 'trace.md').write_text(trace if trace is not None else '# trace\n')
    return str(leg)


ONLY_DELTA = '''
package org.ex;
public class FuzzHarness {
    public static void fuzzerTestOneInput(Object data) {
        Delta d = new Delta();
        d.only(1);
    }
}
'''


def test_collect_leg_splits_kept_from_all_compiled(tmp_path, project):
    """The two harness sets of the README's "kept versus all compiled",
    for the static side. `kept` is what the acceptance gate admitted, named
    by `result.jsonl`; `compiled` is every saved source. Reading only the
    kept set would let the gate, not the prompt, decide the number."""
    leg = _leg(tmp_path,
               sources={'attempt_001': HARNESS, 'attempt_002': ONLY_DELTA},
               accepted=['attempt_001'])
    sets = SR.collect_leg(leg, project)

    assert sorted(sets) == ['compiled', 'kept']
    assert sets['kept'].harnesses == ['attempt_001']
    assert sets['compiled'].harnesses == ['attempt_001', 'attempt_002']
    assert D_only not in sets['kept'].methods
    assert D_only in sets['compiled'].methods
    assert sets['kept'].source == sets['compiled'].source == 'harness_src'

    mdir = os.path.join(leg, 'measurements')
    on_disk = json.load(open(os.path.join(mdir, 'static_kept.json')))
    assert set(on_disk) >= {'methods', 'entries', 'edges', 'unmatched',
                            'harnesses'}
    assert len(on_disk['methods']) == len(sets['kept'].methods)
    assert os.path.isfile(os.path.join(mdir, 'static_compiled.json'))


def test_an_unparsable_harness_costs_only_itself(tmp_path, project):
    """One file that javalang cannot read must not lose the set. It is
    recorded in `unmatched` under its attempt id, and the other harness's
    entries are still there."""
    leg = _leg(tmp_path,
               sources={'attempt_001': HARNESS,
                        'attempt_002': 'this is not java {{{'},
               accepted=['attempt_001', 'attempt_002'])
    sets = SR.collect_leg(leg, project)
    assert A_a in sets['kept'].methods
    assert any('attempt_002' in u and 'unparsed' in u
               for u in sets['kept'].unmatched)


# --- the archived-leg fallback --------------------------------------------

TRACE = '''# trace

## [17] 🧠 LLM call — **harness generation** — model `m`
<details open><summary>▸ Output (~100 chars)</summary>

```
%(good)s
```

</details>

---
## [18] ⚙️ harness-attempt
**output:** **REJECTED**
- reason: no oracle id

---
## [19] 🧠 LLM call — **harness generation** — model `m`
<details open><summary>▸ Output (~100 chars)</summary>

```
%(kept)s
```

</details>

---
## [20] ⚙️ harness-attempt · `attempt_003`
**output:** **ACCEPTED (compiles + crashes the buggy build)**
- trigger: java.lang.IllegalStateException

---
''' % {'good': ONLY_DELTA.strip(), 'kept': HARNESS.strip()}


def test_trace_fallback_recovers_the_accepted_harnesses(tmp_path, project):
    """An archived leg has no `harness_src/`. The accepted harnesses are
    then read out of `trace.md`: an ACCEPTED `harness-attempt` names the
    attempt, and the harness it accepted is the generation output just
    above it. The REJECTED attempt's source sits in the same trace and is
    deliberately not taken — nothing ties it to an attempt id, so a
    `compiled` set built from it could not say what it was over."""
    leg = _leg(tmp_path, trace=TRACE)
    sets = SR.collect_leg(leg, project)

    assert sorted(sets) == ['kept']
    assert sets['kept'].source == 'trace'
    assert sets['kept'].harnesses == ['attempt_003']
    assert A_a in sets['kept'].methods       # from the accepted harness
    assert D_only not in sets['kept'].methods   # the rejected one is not in
    assert not os.path.exists(
        os.path.join(leg, 'measurements', 'static_compiled.json'))


def test_trace_fallback_also_reads_the_verifier_harness_blocks(tmp_path,
                                                               project):
    """A verifier prompt quotes the harness it is judging inside
    `<harness>` tags, and a verifier only ever sees an accepted harness. It
    is the backstop for a trace whose generation output was not printed."""
    trace = ('# trace\n\n## [30] 🧠 LLM call — **verifier / judge**\n'
             '<harness>\n' + HARNESS.strip() + '\n</harness>\n')
    leg = _leg(tmp_path, trace=trace)
    sets = SR.collect_leg(leg, project)
    assert sets['kept'].harnesses == ['harness_1']
    assert A_a in sets['kept'].methods


def test_the_same_harness_is_never_counted_twice(tmp_path, project):
    """The trace prints one accepted harness in both places. Dedup is on
    the source text, so it is one harness, not two."""
    trace = TRACE + ('\n## [30] 🧠 LLM call — **verifier / judge**\n'
                     '<harness>\n' + HARNESS.strip() + '\n</harness>\n')
    leg = _leg(tmp_path, trace=trace)
    sets = SR.collect_leg(leg, project)
    assert sets['kept'].harnesses == ['attempt_003']


def test_a_leg_with_no_harness_source_at_all_writes_nothing(tmp_path,
                                                            project):
    leg = _leg(tmp_path)
    assert SR.collect_leg(leg, project) == {}
    assert not os.path.isdir(os.path.join(leg, 'measurements')) or \
        not [f for f in os.listdir(os.path.join(leg, 'measurements'))
             if f.startswith('static_')]


# ===========================================================================
# metrics: the __stat keys
# ===========================================================================

def _write_measurements(leg, *, root_cause=None, patch_derived=None,
                        coverage=None, static=None):
    mdir = os.path.join(leg, 'measurements')
    os.makedirs(mdir, exist_ok=True)
    if patch_derived is not None:
        loc.dump(patch_derived, os.path.join(mdir, 'patch_derived.json'))
    if root_cause is not None:
        with open(os.path.join(mdir, 'root_cause.json'), 'w') as fh:
            json.dump(root_cause, fh)
    for build, cov in (coverage or {}).items():
        with open(os.path.join(mdir, f'coverage_{build}.json'), 'w') as fh:
            json.dump(cov, fh)
    for name, d in (static or {}).items():
        with open(os.path.join(mdir, f'static_{name}.json'), 'w') as fh:
            json.dump(d, fh)


def _mset(pairs):
    ms = loc.MethodSet()
    for ref, ring in pairs:
        ms.add(ref, ring)
    return ms


def _stat_leg(tmp_path, project):
    """A leg carrying both kinds of F: JaCoCo coverage that ran `Alpha.a`
    only, and a static set that reaches `Alpha.a` and `Beta.e`.

    R0 is {Alpha.a, Beta.e}, so exactly one of its two methods was reached
    in principle but never executed — the diagnostic `R_stat_only` counts.
    """
    leg = _leg(tmp_path, sources={'attempt_001': HARNESS,
                                  'attempt_002': ONLY_DELTA},
               accepted=['attempt_001'])
    SR.collect_leg(leg, project)
    _write_measurements(
        leg,
        patch_derived=_mset([(A_a, loc.SEED), (B_c, loc.CALLEE)]),
        root_cause={'project': 'Ex', 'bug_id': '1',
                    'methods': _mset([(A_a, loc.SEED), (B_e, loc.SEED),
                                      (G_h, loc.CALLEE)]).to_dict()},
        coverage={'buggy': {
            'build': 'buggy',
            'methods': [A_a.to_dict()],
            'lines': [],
            'all_methods': [m.to_dict() for m in
                            (A_new, A_a, B_new, B_c, B_d, B_e, G_h, G_deep,
                             D_only)],
            'branches_covered': 0, 'branches_total': 0}})
    return leg


def test_stat_keys_are_emitted_and_carry_the_kind(tmp_path, project):
    """The reserved fifth slot, now filled. Every static number ends in
    `__stat`, every dynamic one in `__dyn`, and no metric that does not
    read F(H) carries a kind at all — which is what stops a table from
    putting "could reach" and "did reach" in one column."""
    leg = _stat_leg(tmp_path, project)
    row = M.compute_leg(leg)

    stat_keys = sorted(k for k in row if k.endswith('__stat'))
    assert stat_keys == sorted(
        [f'{metric}__method__{rvar}__{build}__stat'
         for metric in ('rcc', 'rcp')
         for rvar in ('R0', 'R1', 'full')
         for build in ('buggy', 'compiled')]
        + [f'psc__method__na__{build}__stat'
           for build in ('buggy', 'compiled')])
    # method granularity only: a call graph names no lines
    assert not [k for k in stat_keys if '__line__' in k]
    for key in stat_keys:
        assert len(key.split('__')) == 5, key
        assert key.split('__')[0] in M.F_METRICS
    assert row['available']['static_kept'] is True
    assert row['available']['static_compiled'] is True


def test_stat_values_are_the_static_set_not_the_dynamic_one(tmp_path,
                                                            project):
    """R0 = {Alpha.a, Beta.e}. The kept harness executed `Alpha.a` alone,
    but statically it also reaches `Beta.e` through it, so RCC doubles when
    read from F_stat. That gap is the whole point of measuring both."""
    leg = _stat_leg(tmp_path, project)
    row = M.compute_leg(leg)
    assert row['rcc__method__R0__buggy__dyn']['value'] == 0.5
    assert row['rcc__method__R0__buggy__stat']['value'] == 1.0
    assert row['rcc__method__R0__buggy__stat']['num'] == 2
    # PSC: P = {Alpha.a, Beta.c}, both statically reachable, one executed
    assert row['psc__method__na__buggy__dyn']['value'] == 0.5
    assert row['psc__method__na__buggy__stat']['value'] == 1.0
    # RCP's denominator is the whole static set, not the part that matched
    assert row['rcp__method__R0__buggy__stat']['den'] == \
        row['sizes']['Fstat_method']['kept']


def test_sizes_record_the_static_set_and_the_two_gaps(tmp_path, project):
    """`R_stat_only` is "pointed at but never reached" — the methods of the
    root-cause region a harness could get to and the fuzzer never drove it
    to, which is a fuzzing failure and not a harness-writing one.
    `R_dyn_only` is its opposite, and counts the static analysis's blind
    spots."""
    leg = _stat_leg(tmp_path, project)
    row = M.compute_leg(leg)
    s = row['sizes']

    assert s['F_kind'] == {'buggy': ['dyn', 'stat'], 'compiled': ['stat']}
    assert s['Fstat_method']['kept'] == 8      # 5 entries + 3 walked to
    assert s['Fstat_entries']['kept'] == 5
    assert s['Fstat_harnesses'] == {'kept': 1, 'compiled': 2}
    assert s['Fstat_source'] == {'kept': 'harness_src',
                                 'compiled': 'harness_src'}
    # Beta.e is in R0, in F_stat and not in F_dyn: reachable, never run.
    assert s['R_stat_only']['buggy']['R0'] == 1
    assert s['R_dyn_only']['buggy']['R0'] == 0
    # the compiled slot has no dynamic side here, so no comparison is made
    assert 'compiled' not in s['R_stat_only']


def test_a_run_without_static_files_emits_no_stat_key(tmp_path, project):
    """Every run made before F_stat existed must come out byte-identical.
    Absent files mean absent keys, not zeros."""
    leg = _leg(tmp_path)
    _write_measurements(
        leg, patch_derived=_mset([(A_a, loc.SEED)]),
        root_cause={'methods': _mset([(A_a, loc.SEED)]).to_dict()})
    row = M.compute_leg(leg)
    assert not [k for k in row if k.endswith('__stat')]
    assert row['available']['static_kept'] is False
    assert row['sizes']['Fstat_method'] == {}
    assert row['sizes']['R_stat_only'] == {}


# ===========================================================================
# aggregate: the extra Table 3 block
# ===========================================================================

def test_render_markdown_adds_a_static_reach_block(tmp_path, project):
    """A run that measured F_stat renders a second table, labelled, never
    extra columns in the first: "could reach" and "did reach" side by side
    in one row is exactly the reading the key's fifth slot exists to
    prevent."""
    _stat_leg(tmp_path, project)
    run = str(tmp_path / 'run')
    M.write_metrics(run)
    agg = A.aggregate(run)

    assert A.has_fkind(agg, 'stat') is True
    text = A.render_markdown(agg)
    assert '(kept harnesses, build buggy)' in text            # the dyn block
    assert '(kept harnesses, build buggy, static reach)' in text
    assert 'RCC (stat)' in text and 'RCC (dyn)' in text
    blocks = text.split('Table 3')
    assert len(blocks) - 1 >= 2
    static_block = [b for b in blocks if 'static reach' in b][0]
    # RCR 0.500 (P holds one of R0's two methods), RCC(stat) 1.000
    assert '| crashing | method | H_R | 0.500 | 1.000 |' in static_block


def test_no_static_block_without_a_static_set(tmp_path):
    """Unchanged for every archived run: one block, and it is the dynamic
    one."""
    from test_measurements_metrics import build_run
    run = str(tmp_path / 'run')
    build_run(run)
    M.write_metrics(run)
    agg = A.aggregate(run)
    assert A.has_fkind(agg, 'stat') is False
    text = A.render_markdown(agg)
    assert 'static reach' not in text


# ===========================================================================
# the hook: harness sources saved beside the coverage dumps
# ===========================================================================

class _FakeProc:
    returncode = 0
    stdout = ''
    stderr = ''


def _build_result(tmp_path, label='attempt_002', body='class FuzzHarness {}'):
    from java.harness.build import BuildResult
    hdir = tmp_path / 'build' / label
    hdir.mkdir(parents=True, exist_ok=True)
    (hdir / 'FuzzHarness.java').write_text(body)
    return BuildResult(harness_path=str(hdir / 'FuzzHarness.java'),
                       class_name='FuzzHarness', classpath='/cp',
                       compiled=True, returncode=0, stdout='', stderr='',
                       attempt_label=label)


def _verifier(**extra):
    return fuzz_runner.HarnessVerifier(
        jazzer_standalone_jar='/jars/jazzer.jar',
        buggy_classpath='/cp/classes', timeout_seconds=20,
        jazzer_api_jar='/jars/jazzer-api.jar', **extra)


@pytest.fixture
def quiet_jazzer(monkeypatch):
    monkeypatch.setattr(fuzz_runner.subprocess, 'run',
                        lambda cmd, **kw: _FakeProc())


def test_no_harness_source_is_saved_without_the_flag(quiet_jazzer, tmp_path):
    """The OFF path. This gate decides which harnesses the whole run has,
    so it must not gain a file write, a directory, or anything else when
    the measurement flag is off."""
    _verifier().verify(_build_result(tmp_path))
    # a source directory without the coverage flag is not coverage ON
    v = _verifier(coverage_src_dir=str(tmp_path / 'harness_src'))
    v.verify(_build_result(tmp_path))
    assert not (tmp_path / 'harness_src').exists()
    assert v.coverage_sources == []


def test_every_compiled_candidate_s_source_is_saved(quiet_jazzer, tmp_path):
    """With the flag: one `.java` per candidate the gate ran, under the
    SAME attempt id its `.exec` dump carries, so the two artifacts of one
    candidate line up by name. The rejected candidates are here too — this
    run is the only time they are ever seen."""
    src_dir = tmp_path / 'leg' / 'harness_src'
    v = _verifier(coverage_dir=str(tmp_path / 'leg' / 'cov'),
                  coverage_include='org.ex.**',
                  coverage_out_dir=str(tmp_path / 'leg' / 'fuzz_out'),
                  coverage_src_dir=str(src_dir))
    v.verify(_build_result(tmp_path, 'attempt_001', body=HARNESS))
    v.verify(_build_result(tmp_path, 'attempt_002', body=ONLY_DELTA))

    assert sorted(os.listdir(src_dir)) == ['attempt_001.java',
                                           'attempt_002.java']
    assert (src_dir / 'attempt_001.java').read_text() == HARNESS
    assert [os.path.basename(p) for p in v.coverage_sources] == \
        ['attempt_001.java', 'attempt_002.java']
    # the same ids the dumps carry
    assert [d['harness'] for d in v.coverage_dumps] == ['attempt_001',
                                                        'attempt_002']


def test_a_source_that_cannot_be_copied_is_not_fatal(quiet_jazzer, tmp_path):
    """Fail-soft: a copy that does not happen costs one harness in a later
    static-reach set and nothing else about the run."""
    v = _verifier(coverage_dir=str(tmp_path / 'leg' / 'cov'),
                  coverage_include='org.ex.**',
                  coverage_src_dir=str(tmp_path / 'leg' / 'harness_src'))
    br = _build_result(tmp_path, 'attempt_001')
    os.remove(br.harness_path)
    out = v.verify(br)
    assert out.crashed is False
    assert v.coverage_sources == []


def test_record_coverage_lists_the_saved_sources(tmp_path):
    """The leg record names the files, next to the dumps, so the
    measurement side never has to guess where they are."""
    v = _verifier(coverage_dir=str(tmp_path / 'cov'),
                  coverage_include='org.ex.**',
                  coverage_src_dir=str(tmp_path / 'harness_src'))
    v.coverage_sources.append(str(tmp_path / 'harness_src' /
                                  'attempt_001.java'))
    extras = {}
    run_mod._record_coverage([v], str(tmp_path / 'cov'), extras,
                             accepted=['attempt_001'])
    assert extras['coverage']['sources'] == [
        str(tmp_path / 'harness_src' / 'attempt_001.java')]

    # with the flag off there is nothing to list, and nothing is recorded
    empty = {}
    run_mod._record_coverage([_verifier()], None, empty)
    assert empty == {}


def test_the_source_directory_sits_beside_the_coverage_directory():
    """`<leg>/cov` and `<leg>/harness_src`: one flag turns both on, and the
    measurement side finds the sources from the leg directory alone."""
    assert run_mod._coverage_src_dir('/runs/leg01/cov') == \
        '/runs/leg01/harness_src'
    assert run_mod._coverage_src_dir(None) is None
