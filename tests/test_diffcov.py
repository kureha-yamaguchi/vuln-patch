"""Diff-hit instrumentation — the three mechanical steps, one test group each.

Station: patched-build materialisation (`PatchedProjectBuilder.build_patched_dir`).
Failure mode it measures: a harness runs the full budget on the patched build
and stays quiet because no generated input ever ENTERED the changed method —
indistinguishable, from the outside, from "the patch fixed it".

So the three things that can silently break the measurement are exactly what is
tested here: the diff mapping pointing at the wrong method (or none), the
counter call landing somewhere that changes what the program does, and the
counts never reaching the run artifacts.

The fixture patches are real `diff -u` output over `fixtures/diffcov_widget.java`
and `fixtures/diffcov_gadget.java`, in the drr `/source/...` path style.
"""
import json
import os
import shutil
import sys

import javalang
import pytest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, 'src'))

from java.execution import diffcov   # noqa: E402

FIXTURES = os.path.join(ROOT, 'tests', 'fixtures')
WIDGET_REL = 'source/org/example/Widget.java'
GADGET_REL = 'source/org/example/Gadget.java'


@pytest.fixture
def tree_dir(tmp_path):
    """A post-patch working copy laid out the way a real checkout is."""
    for rel, fixture in ((WIDGET_REL, 'diffcov_widget.java'),
                         (GADGET_REL, 'diffcov_gadget.java')):
        dst = tmp_path / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(os.path.join(FIXTURES, fixture), dst)
    return str(tmp_path)


def _patch(name: str) -> str:
    with open(os.path.join(FIXTURES, f'diffcov_{name}.patch')) as fh:
        return fh.read()


def _plan(name: str, tree_dir: str):
    return diffcov.changed_methods(_patch(name), tree_dir)


# --- (a) diff -> changed methods -----------------------------------------

def test_a_multi_hunk_patch_maps_every_hunk_to_its_own_method(tree_dir):
    """Two hunks, two different methods in one file. Mapping only the first
    would leave the second method uninstrumented and its zero unreadable."""
    plan = _plan('multi_hunk', tree_dir)
    assert plan.method_ids == [
        'org.example.Widget#indexOf(Object)',
        'org.example.Widget#scale(double[],int)',
    ]


def test_a_constructor_hunk_maps_to_the_constructor(tree_dir):
    """Constructors are where object state is established, and a patch that
    only changes one must still be counted."""
    plan = _plan('constructor', tree_dir)
    assert plan.method_ids == [
        'org.example.Widget#Widget()',
        'org.example.Widget#Widget(int)',
    ]


def test_an_overload_hunk_maps_to_the_RIGHT_overload(tree_dir):
    """`indexOf(Object)` and `indexOf(String, int)` share a name. Keying on
    the name alone would attribute hits to whichever came first, and the
    measurement would read as reach when it was reach of the other method."""
    plan = _plan('overload', tree_dir)
    assert plan.method_ids == ['org.example.Widget#indexOf(String,int)']


def test_a_multi_file_patch_covers_both_files(tree_dir):
    """Generics and varargs in the second file's signature must not break
    the parameter rendering the overload key depends on."""
    plan = _plan('multi_file', tree_dir)
    assert plan.method_ids == [
        'org.example.Gadget#render(Map,Object...)',
        'org.example.Widget#indexOf(Object)',
    ]


def test_a_field_and_import_only_patch_records_the_miss(tree_dir):
    """No method to instrument is a legitimate answer — but it must be
    RECORDED, or an empty diffcov reads as "nothing was reached" when it
    actually means "there was nothing to count"."""
    plan = _plan('fields_only', tree_dir)
    assert plan.method_ids == []
    assert len(plan.unmapped) == 2
    assert all(u['file'] == WIDGET_REL for u in plan.unmapped)
    assert all('no enclosing method' in u['reason'] for u in plan.unmapped)


def test_changed_lines_are_POST_patch_numbers(tree_dir):
    """The tree being instrumented is the patched one, so a hunk's line
    numbers must be read off its `+` side."""
    lines = diffcov.changed_lines_by_file(_patch('overload'))
    assert lines == {WIDGET_REL: [29, 30, 31]}


def test_an_unreadable_post_patch_file_is_recorded_not_dropped(tmp_path):
    plan = diffcov.changed_methods(_patch('overload'), str(tmp_path))
    assert plan.methods == []
    assert plan.unmapped == [{'file': WIDGET_REL, 'line': None,
                              'reason': 'post-patch file not readable'}]


# --- (b) instrumentation insertion ---------------------------------------

def _instrumented(name: str, tree_dir: str, rel: str = WIDGET_REL) -> str:
    plan = _plan(name, tree_dir)
    with open(os.path.join(tree_dir, rel)) as fh:
        source = fh.read()
    targets = [(m.insert_offset, m.method_id) for m in plan.methods
               if m.rel_path == rel]
    return diffcov.instrument_source(source, targets)


def test_b_the_counter_lands_at_the_entry_of_the_right_methods(tree_dir):
    out = _instrumented('multi_hunk', tree_dir)
    assert ('public int indexOf(Object o) { '
            'vulnpatch.DiffCov.hit("org.example.Widget#indexOf(Object)");'
            in out)
    assert ('public double[] scale(double[] xs, int n) { '
            'vulnpatch.DiffCov.hit("org.example.Widget#scale(double[],int)");'
            in out)
    # Exactly one call per instrumented method, and none anywhere else.
    assert out.count('vulnpatch.DiffCov.hit(') == 2


def test_b_a_constructor_counter_goes_AFTER_an_explicit_this_call(tree_dir):
    """Java requires `this(...)`/`super(...)` to be the first statement, so a
    counter inserted before it does not compile."""
    out = _instrumented('constructor', tree_dir)
    assert ('this(7); '
            'vulnpatch.DiffCov.hit("org.example.Widget#Widget()");' in out)
    assert ('public Widget(int size) { '
            'vulnpatch.DiffCov.hit("org.example.Widget#Widget(int)");' in out)


def test_b_instrumented_source_still_parses(tree_dir):
    for name in ('multi_hunk', 'constructor', 'overload'):
        javalang.parse.parse(_instrumented(name, tree_dir))


def test_b_instrumentation_preserves_every_line_number(tree_dir):
    """Inserted inline, without a newline: stack traces, the trigger-test
    net and any later read of the patched tree still line up with the diff."""
    with open(os.path.join(tree_dir, WIDGET_REL)) as fh:
        before = fh.read().splitlines()
    after = _instrumented('multi_hunk', tree_dir).splitlines()
    assert len(before) == len(after)
    changed = [i for i, (a, b) in enumerate(zip(before, after)) if a != b]
    assert len(changed) == 2


def test_b_an_abstract_method_is_never_instrumented(tree_dir):
    """`abstract void go();` has no body to enter."""
    with open(os.path.join(tree_dir, WIDGET_REL)) as fh:
        declarations = diffcov.method_declarations(fh.read())
    go = [d for d in declarations if d['name'] == 'go']
    assert go and go[0]['insert_offset'] is None


def test_b_instrument_patched_dir_writes_the_helper_and_the_plan(tree_dir):
    patch_path = os.path.join(FIXTURES, 'diffcov_multi_hunk.patch')
    plan = diffcov.instrument_patched_dir(tree_dir, patch_path)

    helper = os.path.join(tree_dir, 'source', 'vulnpatch', 'DiffCov.java')
    assert os.path.isfile(helper)   # source root derived from the package
    helper_src = open(helper).read()
    for method_id in plan.method_ids:
        assert f'"{method_id}"' in helper_src
    # The counters are pre-registered, so a method that is never entered
    # still prints its zero — the zeros are the signal.
    assert 'hits=' in helper_src
    # Semantics-neutral by construction: no throw, no I/O on the hot path.
    hot = helper_src.split('public static void hit(String id)')[1] \
                    .split('private static String render')[0]
    assert 'catch (Throwable ignored)' in hot
    assert 'FileOutputStream' not in hot

    with open(os.path.join(tree_dir, WIDGET_REL)) as fh:
        javalang.parse.parse(fh.read())

    with open(os.path.join(tree_dir, '.diffcov_methods.json')) as fh:
        on_disk = json.load(fh)
    assert [m['method_id'] for m in on_disk['methods']] == plan.method_ids


def test_b_the_generated_helper_is_parseable_ascii_java():
    """`defects4j compile` runs ant with the project's own historical
    -source level and the platform charset — a non-ASCII byte in a comment
    is a compile error on an ASCII locale, and the whole patched build then
    fails to compile because of instrumentation."""
    src = diffcov.helper_source(['org.example.Widget#indexOf(Object)'], 2.0)
    javalang.parse.parse(src)
    src.encode('ascii')
    assert 'ConcurrentHashMap COUNTS' in src   # raw type, not generic
    assert '<' not in src.split('public final class')[1].split('static {')[0]


def test_b_no_helper_is_written_when_nothing_maps(tree_dir):
    diffcov.instrument_patched_dir(
        tree_dir, os.path.join(FIXTURES, 'diffcov_fields_only.patch'))
    assert not os.path.exists(
        os.path.join(tree_dir, 'source', 'vulnpatch', 'DiffCov.java'))


# --- (c) [diffcov] line parsing -> artifact record ------------------------

BLOB = (
    'INFO: Seed: 1234\n'
    '[diffcov] method=org.example.Widget#indexOf(Object) hits=0\n'
    '[diffcov] method=org.example.Widget#scale(double[],int) hits=41\n'
    '== Java Exception: java.lang.IllegalStateException\n'
)


def test_c_diffcov_lines_are_parsed_out_of_jazzer_output():
    assert diffcov.parse_diffcov(BLOB) == {
        'org.example.Widget#indexOf(Object)': 0,
        'org.example.Widget#scale(double[],int)': 41,
    }


def test_c_a_zero_is_kept_not_treated_as_absent():
    """A zero is the finding: the input never reached the changed code."""
    counts = diffcov.parse_diffcov(BLOB)
    assert 'org.example.Widget#indexOf(Object)' in counts
    assert counts['org.example.Widget#indexOf(Object)'] == 0


def test_c_repeated_dumps_take_the_largest_count():
    """The periodic flush and the shutdown hook can both land in one blob;
    counters only ever grow."""
    text = ('[diffcov] method=a#b() hits=3\n'
            '[diffcov] method=a#b() hits=9\n')
    assert diffcov.parse_diffcov(text) == {'a#b()': 9}


def test_c_the_file_dump_is_read_back(tmp_path):
    out = tmp_path / 'diffcov.out'
    out.write_text('[diffcov] method=a#b(int) hits=5\n')
    assert diffcov.read_diffcov_file(str(out)) == {'a#b(int)': 5}
    assert diffcov.read_diffcov_file(str(tmp_path / 'missing')) == {}


def test_c_collection_prefers_the_file_over_stderr(tmp_path):
    """The runner SIGKILLs the JVM on its subprocess timeout and libFuzzer
    ends a finding run from native code — neither runs shutdown hooks, so
    the timer-flushed file is the channel that survives."""
    from java.execution.fuzz_runner import _collect_diffcov
    out = tmp_path / 'diffcov.out'
    out.write_text('[diffcov] method=a#b() hits=7\n')
    assert _collect_diffcov(str(out), BLOB) == {'a#b()': 7}
    # …and stderr is still the fallback when the file never appeared.
    assert _collect_diffcov(str(tmp_path / 'gone'), BLOB) == \
        diffcov.parse_diffcov(BLOB)


def test_c_the_run_record_carries_one_entry_per_harness_execution():
    """Schema check on the artifact the analysis actually reads."""
    import types
    from java.run import _record_diffcov

    runner = types.SimpleNamespace(
        diffcov=True,
        diffcov_plan={'methods': [{'method_id': 'a#b()',
                                   'file': 'X.java', 'line': 3}],
                      'unmapped': []})
    results = [types.SimpleNamespace(diffcov={'a#b()': 0},
                                     attempt_label='attempt_001',
                                     harness_path='/x/FuzzHarness.java')]
    extras = {}
    _record_diffcov(runner, results, extras)
    assert extras['diffcov'] == [{'diffcov': {'a#b()': 0},
                                  'phase': 'patched-fuzz',
                                  'harness': 'attempt_001'}]
    assert extras['diffcov_methods']['methods'][0]['method_id'] == 'a#b()'
    assert json.loads(json.dumps(extras)) == extras   # result.jsonl-safe


def test_c_nothing_is_recorded_when_the_flag_is_off():
    """Flag off must be zero code path: no key in the record, so the frozen
    baselines and every downstream aggregator see the file they always saw."""
    import types
    from java.run import _record_diffcov

    runner = types.SimpleNamespace(diffcov=False, diffcov_plan=None)
    extras = {}
    _record_diffcov(runner, [types.SimpleNamespace(diffcov={'a#b()': 1},
                                                   attempt_label='a')],
                    extras)
    assert extras == {}


def test_c_diffcov_is_measurement_only():
    """The boundary this iteration rests on: the counts are written to the
    run artifacts and read by humans. If a prompt, the verifier's evidence,
    or a gate ever starts reading them, this test is the thing that should
    have to be deleted first."""
    for module in ('java.harness.prompts', 'java.relations.relation_verifier',
                   'java.relations.judge_decision'):
        path = os.path.join(ROOT, 'src', *module.split('.')) + '.py'
        with open(path) as fh:
            assert 'diffcov' not in fh.read().lower(), module
