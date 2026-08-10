"""Divergence capture at the diff boundary — one test group per mechanical
step, plus the two boundaries the design rests on.

Station: evidence assembly for relation synthesis (run.py, before
`RelationSynthesizer.synthesize`), with a build-time instrumentation pass that
is a sibling of diffcov's.

Failure mode it targets: reach is saturated, invention is absent — the
relations probe documented observables but never the one the patch moves,
because nothing in the evidence says which one that is.

So what is tested here is exactly what can silently break it: the injection
landing somewhere that changes what the program does (or does not compile),
the pairing/diff/ranking reading a divergence where there is none (or missing
one there is), the prompt block appearing when it should not, and the
anti-anchoring lint — both signs.

Fixtures are the diffcov ones: the same post-patch trees and the same real
`diff -u` output, because the diff -> method mapping is literally the same
code.
"""
import json
import os
import shutil
import sys

import javalang
import pytest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, 'src'))

from java.execution import divcap   # noqa: E402
from java.parsing.java_source import (            # noqa: E402
    anchors_buggy_value, comparison_literals)

FIXTURES = os.path.join(ROOT, 'tests', 'fixtures')
WIDGET_REL = 'source/org/example/Widget.java'
GADGET_REL = 'source/org/example/Gadget.java'

WIDGET_SIGS = [
    ('org.example.Widget', 'Widget', ()),
    ('org.example.Widget', 'Widget', ('int',)),
    ('org.example.Widget', 'indexOf', ('Object',)),
    ('org.example.Widget', 'scale', ('double[]', 'int')),
]


@pytest.fixture
def tree_dir(tmp_path):
    """A working copy laid out the way a real checkout is."""
    for rel, fixture in ((WIDGET_REL, 'diffcov_widget.java'),
                         (GADGET_REL, 'diffcov_gadget.java')):
        dst = tmp_path / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(os.path.join(FIXTURES, fixture), dst)
    return str(tmp_path)


def _widget() -> str:
    with open(os.path.join(FIXTURES, 'diffcov_widget.java')) as fh:
        return fh.read()


def _instrumented_widget(sigs=None) -> str:
    src = _widget()
    targets, _skipped = divcap.obs_targets(src, sigs or WIDGET_SIGS)
    for t in targets:
        t.rel_path = WIDGET_REL
    return divcap.instrument_source(src, targets)


# --- (a) which methods, and what is observable about them -----------------

def test_a_the_diff_mapping_is_diffcovs_own():
    """The scope is 'the methods the patch changed' and it is decided by
    diffcov's mapping, not a second parser: a divergence attributed to a
    method the patch did not touch would be evidence about nothing."""
    from java.execution import diffcov
    assert divcap.changed_methods is diffcov.changed_methods


def test_a_wanted_from_patch_yields_signature_triples(tree_dir):
    """Signatures, not offsets: the same declaration sits somewhere else in
    the buggy tree, and matching on the signature is what pairs the two."""
    with open(os.path.join(FIXTURES, 'diffcov_multi_hunk.patch')) as fh:
        wanted = divcap.wanted_from_patch(fh.read(), tree_dir)
    assert WIDGET_REL in wanted
    for sig in wanted[WIDGET_REL]:
        assert len(sig) == 3 and isinstance(sig[2], tuple)


def test_a_value_returning_methods_capture_their_return():
    targets, _ = divcap.obs_targets(_widget(), WIDGET_SIGS)
    by_id = {t.method_id: t for t in targets}
    scale = by_id['org.example.Widget#scale(double[],int)']
    assert scale.observable == 'ret'
    assert scale.return_type == 'double[]'
    # both `return` statements, not just the first
    assert len(scale.return_sites) == 2


def test_a_constructors_capture_receiver_state_instead():
    """A frame with no return value still produces something: the receiver
    it just built. That is the only observable there is at that frame."""
    targets, _ = divcap.obs_targets(_widget(), WIDGET_SIGS)
    ctor = {t.method_id: t
            for t in targets}['org.example.Widget#Widget(int)']
    assert ctor.observable == 'state'
    assert ctor.return_type is None
    assert ctor.is_constructor


def test_a_overloads_are_kept_apart():
    """`indexOf(Object)` is asked for; `indexOf(String,int)` is not, and
    must not be instrumented — a divergence attributed to the wrong overload
    would point invention at the wrong method."""
    targets, _ = divcap.obs_targets(
        _widget(), [('org.example.Widget', 'indexOf', ('Object',))])
    assert [t.method_id for t in targets] == [
        'org.example.Widget#indexOf(Object)']


def test_a_unobservable_declarations_are_recorded_not_dropped():
    """An empty capture must be readable as 'there was nothing to watch'
    rather than 'the patch moves nothing'."""
    _targets, skipped = divcap.obs_targets(
        _widget(), [('org.example.Widget.Base', 'go', ()),
                    ('org.example.Widget', 'nosuch', ())])
    reasons = {s['method']: s['reason'] for s in skipped}
    assert 'no body' in reasons['org.example.Widget.Base#go()']
    assert 'not found' in reasons['org.example.Widget#nosuch()']


def test_a_a_method_the_patch_added_is_simply_unfound_in_the_buggy_tree():
    """The buggy twin is instrumented from the PATCHED tree's signature
    list, so a method the patch introduced has no counterpart. That is a
    recorded gap, never a guess at a similar method."""
    _targets, skipped = divcap.obs_targets(
        _widget(), [('org.example.Widget', 'brandNew', ('int',))])
    assert skipped and 'not found' in skipped[0]['reason']


# --- (b) the injection -----------------------------------------------------

def test_b_the_instrumented_source_still_parses():
    javalang.parse.parse(_instrumented_widget())


def test_b_every_line_keeps_its_number():
    """Inline edits only. Stack traces, the trigger-test net and any later
    read of the tree still line up with the diff (diffcov's rule)."""
    assert _instrumented_widget().count('\n') == _widget().count('\n')


def test_b_primitive_arguments_are_boxed_by_hand():
    """Autoboxing is a syntax error at the historical `-source` levels
    several projects in this dataset compile at."""
    out = _instrumented_widget()
    assert 'new Object[]{new Integer(size)}' in out
    assert 'new Object[]{xs, new Integer(n)}' in out
    assert 'new Object[0]' in out       # the no-arg constructor


def test_b_a_reference_return_is_cast_back_to_its_declared_type():
    out = _instrumented_widget()
    assert '(double[]) vulnpatch.DivObs.ret(' in out
    # a primitive return uses the typed overload and needs no cast
    assert 'return vulnpatch.DivObs.ret("org.example.Widget#indexOf' in out


def test_b_a_constructor_counter_goes_after_the_explicit_this_call():
    """Java requires `this(...)`/`super(...)` to be the first statement; a
    declaration in front of it does not compile."""
    out = _instrumented_widget()
    head = out.index('public Widget() {')
    body = out[head:out.index('public Widget(int size)')]
    assert body.index('this(7);') < body.index('__divcap_args')


def test_b_a_void_frame_is_wrapped_so_the_state_is_read_after_the_call():
    out = _instrumented_widget()
    assert 'try {' in out
    assert ('} finally { vulnpatch.DivObs.state('
            '"org.example.Widget#Widget(int)", __divcap_args, this); }') in out


def test_b_returns_of_a_nested_declaration_are_left_alone():
    """A `return` inside an anonymous or nested class belongs to that
    class's method, not to the one being instrumented."""
    src = '''package p;
public class Outer {
    public int run(int n) {
        Runnable r = new Runnable() {
            public void go() { return; }
            public int inner() { return 5; }
        };
        return n;
    }
}
'''
    targets, _ = divcap.obs_targets(src, [('p.Outer', 'run', ('int',))])
    out = divcap.instrument_source(src, targets)
    javalang.parse.parse(out)
    assert out.count('DivObs.ret(') == 1
    assert 'return 5;' in out            # the inner one untouched


def test_b_a_lambda_body_makes_the_rewrite_fail_closed():
    """A lambda's `return` is not inside any declaration javalang reports,
    so the exclusion cannot see it and the rewrite would type-clash. No
    capture beats a build that does not compile."""
    src = ('package p;\npublic class L {\n'
           '  public int f(int n) { java.util.function.IntSupplier s = '
           '() -> { return n; }; return s.getAsInt(); }\n}\n')
    targets, skipped = divcap.obs_targets(src, [('p.L', 'f', ('int',))])
    assert targets == []
    assert 'lambda' in skipped[0]['reason']


def test_b_the_helper_obeys_the_old_java_constraints(tmp_path):
    """Same dialect as DiffCov.java, same reason: `defects4j compile` runs
    the project's own historical `-source` level with the platform
    charset."""
    src = divcap.helper_source(['org.example.Widget#indexOf(Object)'], 2.0, 64)
    src.encode('ascii')                      # no non-ASCII byte anywhere
    javalang.parse.parse(src)
    import re
    assert not re.search(r'for\s*\([^;)]+:', src)          # no for-each
    assert '@Override' not in src and '@Deprecated' not in src
    assert 'StringBuilder' not in src
    assert 'HashMap<' not in src and 'Map<' not in src
    assert 'LinkedHashMap<' not in src and 'TreeMap<' not in src


def test_b_instrument_dir_writes_one_helper_and_a_plan(tree_dir):
    plan = divcap.instrument_dir(tree_dir, {WIDGET_REL: WIDGET_SIGS})
    helpers = [os.path.join(root, f)
               for root, _d, files in os.walk(tree_dir)
               for f in files if f == 'DivObs.java']
    assert len(helpers) == 1                 # a second copy is a dup-class error
    assert helpers[0].endswith(
        os.path.join('source', 'vulnpatch', 'DivObs.java'))
    with open(os.path.join(tree_dir, divcap.PLAN_FILE)) as fh:
        on_disk = json.load(fh)
    assert on_disk == plan.as_dict()
    assert len(plan.targets) == 4


def test_b_the_patched_pass_leaves_the_signature_list_for_the_buggy_twin(
        tree_dir):
    """The buggy tree is instrumented from the patched tree's list, so the
    two builds watch the same methods by construction."""
    divcap.instrument_patched_dir(
        tree_dir, os.path.join(FIXTURES, 'diffcov_multi_hunk.patch'))
    wanted = divcap.read_wanted(tree_dir)
    assert wanted and WIDGET_REL in wanted
    assert all(isinstance(s[2], tuple) for s in wanted[WIDGET_REL])


# --- (c) capture lines -> pairing -> divergences ---------------------------

BUGGY_BLOB = (
    'irrelevant jazzer noise\n'
    '[divobs] method=p.W#f(int) args=i:1 ret=q:"09" count=3 stable=1\n'
    '[divobs] method=p.W#f(int) args=i:2 ret=i:5 count=1 stable=1\n'
    '[divobs] method=p.W#f(int) args=i:9 ret=i:0 count=7 stable=1\n'
    '[divobs] method=p.W#W(int) args=i:1 state=p.W a=i:1 b=i:2 '
    'count=9 stable=1\n')
PATCHED_BLOB = (
    '[divobs] method=p.W#f(int) args=i:1 ret=q:"-2" count=4 stable=1\n'
    '[divobs] method=p.W#f(int) args=i:2 ret=i:5 count=1 stable=1\n'
    '[divobs] method=p.W#f(int) args=i:9 ret=i:4 count=7 stable=1\n'
    '[divobs] method=p.W#W(int) args=i:1 state=p.W a=i:1 b=i:3 '
    'count=9 stable=1\n')


def test_c_capture_lines_are_parsed_out_of_the_run_output():
    obs = {(o.method_id, o.shape): o
           for o in divcap.parse_divobs(BUGGY_BLOB)}
    assert obs[('p.W#f(int)', 'i:1')].value == 'q:"09"'
    assert obs[('p.W#f(int)', 'i:1')].count == 3
    assert obs[('p.W#W(int)', 'i:1')].kind == 'state'
    assert obs[('p.W#W(int)', 'i:1')].value == 'p.W a=i:1 b=i:2'


def test_c_a_value_containing_spaces_and_equals_still_parses():
    line = ('[divobs] method=p.W#g() args=q:"a=b c" '
            'ret=q:"x = y count=1" count=2 stable=1\n')
    obs = divcap.parse_divobs(line)
    assert len(obs) == 1
    assert obs[0].shape == 'q:"a=b c"'
    assert obs[0].value == 'q:"x = y count=1"'


def test_c_a_repeated_dump_takes_the_largest_count():
    """The periodic flush and the shutdown hook can both land in one blob."""
    text = ('[divobs] method=a#b() args=i:1 ret=i:2 count=3 stable=1\n'
            '[divobs] method=a#b() args=i:1 ret=i:2 count=8 stable=1\n')
    assert [o.count for o in divcap.parse_divobs(text)] == [8]


def test_c_pairing_is_on_the_argument_tuple_and_only_moved_values_count():
    divs = divcap.diff_observations(divcap.parse_divobs(BUGGY_BLOB),
                                    divcap.parse_divobs(PATCHED_BLOB))
    moved = {(d.method_id, d.input_shape) for d in divs}
    assert ('p.W#f(int)', 'i:1') in moved
    assert ('p.W#f(int)', 'i:9') in moved
    assert ('p.W#W(int)', 'i:1') in moved
    assert ('p.W#f(int)', 'i:2') not in moved     # same value on both builds


def test_c_an_argument_tuple_only_one_build_saw_is_not_a_divergence():
    """Unpaired is unknown, and an unknown must not steer invention."""
    b = divcap.parse_divobs(
        '[divobs] method=a#b() args=i:1 ret=i:1 count=1 stable=1\n')
    p = divcap.parse_divobs(
        '[divobs] method=a#b() args=i:2 ret=i:9 count=1 stable=1\n')
    assert divcap.diff_observations(b, p) == []


def test_c_a_value_unstable_within_one_build_is_dropped_fail_closed():
    """A value that is not even stable inside one build cannot evidence a
    difference BETWEEN builds."""
    b = divcap.parse_divobs(
        '[divobs] method=a#b() args=i:1 ret=i:1 count=4 stable=0\n')
    p = divcap.parse_divobs(
        '[divobs] method=a#b() args=i:1 ret=i:9 count=4 stable=1\n')
    assert divcap.diff_observations(b, p) == []


def test_c_the_paired_count_never_overstates_what_was_seen():
    b = divcap.parse_divobs(
        '[divobs] method=a#b() args=i:1 ret=i:1 count=2 stable=1\n')
    p = divcap.parse_divobs(
        '[divobs] method=a#b() args=i:1 ret=i:9 count=50 stable=1\n')
    assert divcap.diff_observations(b, p)[0].count == 2


def test_c_merging_several_runs_of_one_build_sums_the_counts():
    a = divcap.parse_divobs(
        '[divobs] method=a#b() args=i:1 ret=i:1 count=2 stable=1\n')
    b = divcap.parse_divobs(
        '[divobs] method=a#b() args=i:1 ret=i:1 count=5 stable=1\n')
    merged = divcap.merge_observations(a, b)
    assert [(o.count, o.stable) for o in merged] == [(7, True)]


def test_c_merging_disagreeing_runs_marks_the_value_unstable():
    a = divcap.parse_divobs(
        '[divobs] method=a#b() args=i:1 ret=i:1 count=2 stable=1\n')
    b = divcap.parse_divobs(
        '[divobs] method=a#b() args=i:1 ret=i:7 count=2 stable=1\n')
    assert divcap.merge_observations(a, b)[0].stable is False


def _div(method, shape, count):
    return divcap.Divergence(method_id=method, observable='return value',
                             input_shape=shape, buggy_value='i:0',
                             patched_value='i:1', count=count)


def test_c_ranking_is_diversity_first_then_frequency():
    """Eight slots must not all be eaten by one method's near-identical
    tuples while a second changed method goes unmentioned."""
    divs = ([_div('a#x()', f's{i}', 100 - i) for i in range(10)]
            + [_div('b#y()', 't0', 1)])
    ranked = divcap.rank_divergences(divs, k=8)
    assert ranked[0].method_id == 'a#x()'      # more diverging shapes first
    assert ranked[1].method_id == 'b#y()'      # then round-robin
    assert [d.input_shape for d in ranked if d.method_id == 'a#x()'] == [
        's0', 's1', 's2', 's3', 's4', 's5', 's6']
    assert len(ranked) == 8


def test_c_ranking_falls_back_to_frequency_within_one_method():
    divs = [_div('a#x()', 'lo', 1), _div('a#x()', 'hi', 99)]
    assert [d.input_shape
            for d in divcap.rank_divergences(divs, k=8)] == ['hi', 'lo']


def test_c_the_artifact_record_is_result_jsonl_safe():
    divs = divcap.diff_observations(divcap.parse_divobs(BUGGY_BLOB),
                                    divcap.parse_divobs(PATCHED_BLOB))
    payload = {'divcap': {'divergences': [d.as_dict() for d in divs]}}
    assert json.loads(json.dumps(payload)) == payload
    one = divs[0].as_dict()
    assert set(one) == {'method', 'observable', 'input_shape',
                        'buggy_value', 'patched_value', 'count'}


def test_c_untag_recovers_the_bare_literal():
    assert divcap.untag('i:-2') == '-2'
    assert divcap.untag('q:"09"') == '09'
    assert divcap.untag('d:0.5') == '0.5'
    assert divcap.untag('null') == 'null'


def test_c_state_divergences_contribute_only_the_fields_that_moved():
    """A whole field dump is not something any relation compares against;
    the field that actually moved is."""
    divs = divcap.diff_observations(divcap.parse_divobs(BUGGY_BLOB),
                                    divcap.parse_divobs(PATCHED_BLOB))
    values = divcap.buggy_side_values(divs)
    assert '09' in values          # the moved return value
    assert '2' in values           # field b moved 2 -> 3
    assert '1' not in values       # field a is identical on both builds


# --- (d) the synthesis prompt block ---------------------------------------

def test_d_the_block_is_absent_when_there_is_nothing_to_show():
    from java.relations.relation_synth import divergence_block
    assert divergence_block([]) == ''
    assert divergence_block(None) == ''


def test_d_the_block_names_method_observable_and_input_shape():
    from java.relations.relation_synth import divergence_block
    divs = divcap.diff_observations(divcap.parse_divobs(BUGGY_BLOB),
                                    divcap.parse_divobs(PATCHED_BLOB))
    block = divergence_block(divs)
    assert 'method=p.W#f(int)' in block
    assert 'observable=return value' in block
    assert 'input=i:1' in block


def test_d_the_block_forbids_using_the_values_as_expectations():
    """The whole soundness argument in one instruction: divergence steers
    ATTENTION, the contract still comes only from documentation."""
    from java.relations.relation_synth import divergence_block
    block = divergence_block([{'method': 'a#b()', 'observable': 'return value',
                               'input_shape': 'i:1', 'buggy_value': 'i:0',
                               'patched_value': 'i:1', 'count': 2}]).lower()
    assert 'do not use either recorded value as an expected value' in block
    assert 'documented contract' in block
    assert 'not evidence of a defect' in block


def test_d_the_block_names_no_bug_and_no_dataset(tmp_path):
    """General method only: nothing in the prompt text may name a project,
    a bug id or an APR tool."""
    from java.relations import relation_synth
    text = (relation_synth._DIVERGENCE_HEADER
            + relation_synth._DIVERGENCE_RULES).lower()
    for token in ('lang-', 'math-', 'chart-', 'closure-', 'time-',
                  'defects4j', 'commons', 'jfree'):
        assert token not in text


def test_d_synthesis_renders_the_block_into_the_prompt_context():
    """End of the wire: the block has to land in the context the model is
    actually shown, in the focused passes as well as the single call."""
    from java.relations.relation_synth import RelationSynthesizer

    class _Gen:
        def generate(self, messages):
            return '[]'

    synth = RelationSynthesizer(_Gen())
    synth.synthesize(['class X {}'], 'X', [], [],
                     divergences=[{'method': 'a#b()',
                                   'observable': 'return value',
                                   'input_shape': 'i:1', 'buggy_value': 'i:0',
                                   'patched_value': 'i:1', 'count': 2}])
    assert 'OBSERVED DIVERGENCES' in synth.last_prompt['context']
    synth2 = RelationSynthesizer(_Gen())
    synth2.synthesize(['class X {}'], 'X', [], [])
    assert 'OBSERVED DIVERGENCES' not in synth2.last_prompt['context']


# --- (e) the anti-anchoring lint (both signs) ------------------------------

def test_e_comparison_literals_are_found_in_the_shapes_models_write():
    body = ('if (!"09".equals(r)) throw new RuntimeException("v violated");\n'
            'if (n != -2) throw new RuntimeException("v violated");\n'
            'if (s.equals("zz")) throw new RuntimeException("v violated");')
    found = comparison_literals(body)
    assert '09' in found and '-2' in found and 'zz' in found


def test_e_the_lint_demotes_a_check_that_expects_a_pre_patch_value():
    check = 'if (!"09".equals(r)) throw new RuntimeException("v violated");'
    reason = anchors_buggy_value(check, ['09'])
    assert reason is not None
    assert '`09`' in reason              # the demotion names the match


def test_e_the_lint_matches_a_numeric_anchor_however_it_is_written():
    assert anchors_buggy_value('if (n != -2L) throw x;', ['-2'])
    assert anchors_buggy_value('if (n != -2.0) throw x;', ['-2'])


def test_e_the_lint_is_silent_when_the_value_did_not_move():
    """The other sign. A value that is identical on both builds is not a
    divergence, so it never enters the lint's list — and a literal the check
    happens to use that no divergence recorded is not an anchor."""
    divs = divcap.diff_observations(divcap.parse_divobs(BUGGY_BLOB),
                                    divcap.parse_divobs(PATCHED_BLOB))
    values = divcap.buggy_side_values(divs)
    # `i:5` on args i:2 is identical on both builds, so 5 is not in the list
    assert anchors_buggy_value('if (n != 5) throw x;', values) is None
    assert anchors_buggy_value('if (n != 5) throw x;', []) is None


def test_e_the_lint_ignores_a_literal_that_is_not_compared_against():
    assert anchors_buggy_value('String m = "09" + n; foo(m);', ['09']) is None


def test_e_the_screen_demotes_rather_than_drops(monkeypatch):
    """Pre-registration decision 4: an expected literal can coincide with a
    pre-patch value legitimately (a documented -1), so a drop would delete
    sound checks to punish a coincidence."""
    from java.relations import relation_screen

    class _Rel:
        name = 'r'
        check = ('if (!"09".equals(r)) throw new RuntimeException('
                 '"relation r violated");')

    rel = _Rel()
    monkeypatch.setattr(relation_screen, 'record_event',
                        lambda *a, **k: None)
    # Stop right after the lints: the build step is where the JVM would be
    # needed, and this test is about the lint's effect on the candidate.
    monkeypatch.setattr(
        relation_screen.HarnessBuilder, 'build',
        lambda self, *a, **k: (_ for _ in ()).throw(RuntimeError('no jvm')))
    relation_screen.screen_relations(
        [rel], builder=relation_screen.HarnessBuilder(jazzer_api_jar=None), buggy_dir='/x',
        jazzer_standalone_jar='/x.jar', divergence_values=['09'])
    assert 'ANCHORED-ON-PRE-PATCH-VALUE' in rel.screen_demotion
    assert '`09`' in rel.screen_demotion


def test_e_the_screen_is_byte_for_byte_unchanged_without_the_flag(monkeypatch):
    from java.relations import relation_screen

    class _Rel:
        name = 'r'
        check = ('if (!"09".equals(r)) throw new RuntimeException('
                 '"relation r violated");')

    rel = _Rel()
    monkeypatch.setattr(relation_screen, 'record_event', lambda *a, **k: None)
    monkeypatch.setattr(
        relation_screen.HarnessBuilder, 'build',
        lambda self, *a, **k: (_ for _ in ()).throw(RuntimeError('no jvm')))
    relation_screen.screen_relations(
        [rel], builder=relation_screen.HarnessBuilder(jazzer_api_jar=None), buggy_dir='/x',
        jazzer_standalone_jar='/x.jar')
    assert not hasattr(rel, 'screen_demotion')


# --- (f) the two boundaries ------------------------------------------------

def test_f_nothing_is_recorded_when_the_capture_produced_nothing():
    """Flag off is zero code path: no key in the record, so the frozen guard
    fixtures and every downstream aggregator see the file they always saw."""
    from java.run import _record_divcap
    extras = {}
    _record_divcap(None, extras)
    assert extras == {}


def test_f_the_run_record_carries_the_status_and_the_divergences():
    from java.run import _record_divcap
    divs = divcap.diff_observations(divcap.parse_divobs(BUGGY_BLOB),
                                    divcap.parse_divobs(PATCHED_BLOB))
    extras = {}
    _record_divcap({'status': 'ok', 'divergences': divs,
                    'plan': {'methods': [], 'skipped': []},
                    'buggy_observations': 4, 'patched_observations': 4},
                   extras)
    assert extras['divcap']['status'] == 'ok'
    assert len(extras['divcap']['divergences']) == len(divs)
    assert extras['divcap_methods'] == {'methods': [], 'skipped': []}
    assert json.loads(json.dumps(extras)) == extras


def test_f_divcap_reaches_synthesis_and_nothing_downstream_of_it():
    """The boundary the design rests on. Divergence facts steer INVENTION;
    they are not evidence about the patch, because a correct fix diverges
    too. If the verifier, the judge or a gate ever starts reading them, this
    test is the thing that should have to be deleted first."""
    for module in ('java.harness.prompts', 'java.relations.relation_verifier',
                   'java.relations.judge_decision'):
        path = os.path.join(ROOT, 'src', *module.split('.')) + '.py'
        with open(path) as fh:
            assert 'divcap' not in fh.read().lower(), module
            assert 'divobs' not in fh.read().lower(), module
