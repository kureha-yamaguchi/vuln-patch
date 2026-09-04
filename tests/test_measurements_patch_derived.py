"""Fences for `java.measurements.patch_derived` — reading P back out of a
finished run.

An archived run has no fuzz-introspector project left, so the patch-derived
set cannot be recomputed; it has to be read out of the `PatchContext` the
run recorded at the time. That context was written for a PROMPT, not for a
measurement, and the three ways it is lossy are what most of these tests
pin down:

  * callers are stored as the calling method's SOURCE TEXT, so the name has
    to be parsed back out of the declaration line and the declaring class
    is simply not there;
  * `root_cause_reachable` holds shortened display labels of methods the
    per-function `reachable` lists already named, so it must not add the
    same method a second time under its short spelling;
  * callee depth is not recorded at all.

The fixture is the analysis section of a real archived run (Closure-18),
copied verbatim, so the parser is held against the format that actually
exists rather than one invented here.

`lines_for` is exercised on a tiny checkout built in tmp_path, because what
it has to get right — nested classes sharing their outer file's identity,
constructors spelled `<init>`, a class-less caller ref — is easier to see
in six methods than in a real project.
"""
import json
import os

import pytest

from java.measurements import patch_derived as pd
from java.measurements.locations import (CALLEE, CALLER, OUTSIDE, SEED,
                                         LineRef, MethodRef, MethodSet)

FIXTURE = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                       'fixtures', 'measurements',
                       'trace_excerpt_closure18.md')

# The one method the Closure-18 patch touched.
PARSE_INPUTS = MethodRef('com.google.javascript.jscomp.Compiler',
                         'parseInputs', ())


@pytest.fixture(scope='module')
def closure18() -> MethodSet:
    return pd.from_trace(FIXTURE)


# ---------------------------------------------------------------------------
# Finding the context dict inside a trace.md
# ---------------------------------------------------------------------------

def test_context_dict_is_found_in_the_trace():
    ctx = pd.context_dict_from_trace(FIXTURE)
    assert ctx['package'] == 'com.google.javascript.jscomp'
    assert [f['func_name'] for f in ctx['functions']] == ['parseInputs']
    assert ctx['modified_files'] == [
        'src/com/google/javascript/jscomp/Compiler.java']


def test_a_trace_without_the_analysis_section_is_an_error(tmp_path):
    p = tmp_path / 'trace.md'
    p.write_text('# Pipeline trace\n\n## [0] failing-tests-found\nnothing\n')
    with pytest.raises(ValueError):
        pd.context_dict_from_trace(str(p))


def test_the_json_block_is_read_from_the_right_section(tmp_path):
    """A later step's JSON block must not be picked up instead."""
    p = tmp_path / 'trace.md'
    p.write_text(
        '# Pipeline trace\n\n'
        '## [2] analysis (TargetAnalyzer)\n'
        '**output:**\n```json\n{"package": "a.b"}\n```\n\n'
        '## [3] synth\n```json\n{"package": "WRONG"}\n```\n')
    assert pd.context_dict_from_trace(str(p)) == {'package': 'a.b'}


# ---------------------------------------------------------------------------
# The archived Closure-18 set
# ---------------------------------------------------------------------------

def test_the_touched_method_is_the_only_seed(closure18):
    assert closure18.refs(SEED) == [PARSE_INPUTS]
    assert closure18.items[PARSE_INPUTS].depth == 0


def test_ring_counts(closure18):
    """1 touched method, its 5 recorded callers, 152 distinct callees.

    154 names are listed as reachable; one is introspector reporting a
    whole expression as the receiver
    (`(moduleGraph == null ? ... ).manageDependencies(...)`) and cannot be
    resolved, and one more is a duplicate of another entry."""
    assert len(closure18.refs(SEED)) == 1
    assert len(closure18.refs(CALLER)) == 5
    assert len(closure18.refs(CALLEE)) == 152
    assert len(closure18) == 158


def test_a_sample_callee_keeps_its_qualified_class(closure18):
    ref = MethodRef('com.google.javascript.jscomp.DependencyOptions',
                    'needsManagement', ())
    assert closure18.ring_of(ref) == CALLEE
    assert closure18.items[ref].depth == 1        # archives record no depth


def test_a_method_outside_the_set_reads_as_outside(closure18):
    assert closure18.ring_of(MethodRef('com.example.Nope', 'nope', ())) \
        == OUTSIDE


def test_display_labels_do_not_double_count(closure18):
    """`root_cause_reachable` is the reachable list re-spelled, so it must
    add no new methods: every callee is qualified, none is a bare
    `Compiler.hasErrors` shadow of one already there."""
    ctx = pd.context_dict_from_trace(FIXTURE)
    assert ctx['root_cause_reachable']            # the labels really are there
    without = pd.method_set_from_context(dict(ctx, root_cause_reachable=[]))
    assert len(without) == len(closure18)


def test_unresolvable_names_are_reported_not_dropped(closure18):
    assert any('manageDependencies' in u for u in closure18.unmatched)
    # broken labels for a generic receiver: 'List<...CodeChangeHandler>.add'
    # is shortened to 'CodeChangeHandler>.add' by the prompt renderer.
    assert 'CodeChangeHandler>.add' in closure18.unmatched


# ---------------------------------------------------------------------------
# Callers recovered from source text
# ---------------------------------------------------------------------------

def test_callers_come_back_with_a_name_and_an_arity_but_no_class(closure18):
    callers = {(r.name, r.params) for r in closure18.refs(CALLER)}
    assert callers == {
        ('parse', ()),
        ('parse', ('String[]', 'CompilerOptions')),
        ('helperInlineReferenceToFunction',
         ('String', 'String', 'String', 'InliningMode', 'boolean')),
        ('testSets', ('boolean', 'String', 'String')),
        ('checkSynthesizedExtern', ('String', 'String', 'String')),
    }
    assert all(r.class_fq == '' for r in closure18.refs(CALLER))


@pytest.mark.parametrize('source,expected', [
    ('public void parse() {\n    parseInputs();\n  }',
     ('parse', ())),
    ('protected Node parse(String[] original, CompilerOptions options) {\n}',
     ('parse', ('String[]', 'CompilerOptions'))),
    ('@SuppressWarnings("unchecked")\n  private void testSets(boolean a,'
     ' String b) {\n}',
     ('testSets', ('boolean', 'String'))),
    ('public void wrap(\n      final String a,\n      final int b) {\n}',
     ('wrap', ('String', 'int'))),
    ('static <T> List<T> pick(Map<String, T> m, int n) {\n}',
     ('pick', ('Map', 'int'))),
    ('  Widget(int size) {\n    this.size = size;\n  }',
     ('Widget', ('int',))),
    ('public void old(String args[]) {\n}',
     ('old', ('String[]',))),
])
def test_declaration_line_parsing(source, expected):
    ref = pd.caller_ref_from_source(source)
    assert (ref.name, ref.params) == expected
    assert ref.class_fq == ''


def test_a_body_that_starts_with_a_keyword_is_not_read_as_a_declaration():
    """A fragment whose first parenthesised word is `if` must not become a
    method called `if`."""
    src = 'if (a) {\n  b();\n}\nvoid real(int x) {\n}'
    assert pd.caller_ref_from_source(src).name == 'real'


def test_source_that_declares_nothing_is_reported():
    ms = pd.method_set_from_context(
        {'functions': [{'fi_name': '[a.B].c()', 'xrefs': ['x = y + 1;']}]})
    assert ms.refs(CALLER) == []
    assert 'xref-source-unparsed' in ms.unmatched


# ---------------------------------------------------------------------------
# from_context_json — the same thing from a plain JSON file
# ---------------------------------------------------------------------------

def _write(tmp_path, ctx) -> str:
    p = tmp_path / 'context.json'
    p.write_text(json.dumps(ctx))
    return str(p)


def test_context_json_matches_the_trace(tmp_path, closure18):
    ctx = pd.context_dict_from_trace(FIXTURE)
    ms = pd.from_context_json(_write(tmp_path, ctx))
    assert set(ms.items) == set(closure18.items)


def test_xref_names_are_preferred_over_source_text(tmp_path):
    """The future `context.json` records caller NAMES; when it does, the
    class comes back too and nothing has to be guessed from text."""
    ctx = {'functions': [{
        'fi_name': '[com.example.A].target()',
        'xrefs': ['public void guessed() {\n  target();\n}'],
        'xref_names': ['[com.example.Top].calls(int)'],
    }]}
    ms = pd.from_context_json(_write(tmp_path, ctx))
    assert ms.refs(CALLER) == [MethodRef('com.example.Top', 'calls', ('int',))]


def test_a_seed_with_no_mangled_name_falls_back_to_the_ast(tmp_path):
    ctx = {'functions': [{'fi_name': None, 'func_name': 'area',
                          'func_class_fq': 'com.example.Widget',
                          'func_param_types': ['int', 'int']}]}
    ms = pd.from_context_json(_write(tmp_path, ctx))
    assert ms.refs(SEED) == [MethodRef('com.example.Widget', 'area',
                                       ('int', 'int'))]


def test_an_empty_context_is_an_empty_set(tmp_path):
    ms = pd.from_context_json(_write(tmp_path, {}))
    assert len(ms) == 0 and ms.unmatched == []


# ---------------------------------------------------------------------------
# lines_for — methods to source lines
# ---------------------------------------------------------------------------

WIDGET = '''package com.example;

public class Widget {
  private int size;

  public Widget(int size) {
    this.size = size;
  }

  public int area(int w, int h) {
    return w * h;
  }

  static class Inner {
    void helper() {
      int q = 1;
    }
  }
}
'''

GADGET = '''package com.example;

public class Gadget {
  public int area(int w, int h) {
    return w + h;
  }

  void solo() {
    int z = 0;
  }
}
'''


@pytest.fixture
def checkout(tmp_path):
    root = tmp_path / 'src' / 'com' / 'example'
    root.mkdir(parents=True)
    (root / 'Widget.java').write_text(WIDGET)
    (root / 'Gadget.java').write_text(GADGET)
    (root / 'notes.txt').write_text('not java')
    return str(tmp_path / 'src')


def _lines(line_set, class_top):
    return sorted(r.line for r in line_set.refs() if r.class_top_fq == class_top)


def test_lines_for_covers_a_method_body(checkout):
    ms = MethodSet()
    ms.add(MethodRef('com.example.Widget', 'area', ('int', 'int')), SEED, 0)
    ls = pd.lines_for(ms, checkout)
    # 'public int area(...)' is line 10, its closing brace line 12.
    assert _lines(ls, 'com.example.Widget') == [10, 11, 12]
    assert ls.ring_of(LineRef('com.example.Widget', 11)) == SEED


def test_parameter_types_need_not_agree_only_the_arity(checkout):
    """The call graph mis-types arguments often enough that comparing
    types loses real matches, so only the count is compared."""
    ms = MethodSet()
    ms.add(MethodRef('com.example.Widget', 'area', ('Object', 'Object')),
           CALLEE, 1)
    assert _lines(pd.lines_for(ms, checkout), 'com.example.Widget') \
        == [10, 11, 12]


def test_a_nested_class_is_keyed_by_the_file_it_lives_in(checkout):
    ms = MethodSet()
    ms.add(MethodRef('com.example.Widget.Inner', 'helper', ()), CALLEE, 1)
    ls = pd.lines_for(ms, checkout)
    assert _lines(ls, 'com.example.Widget') == [15, 16, 17]


def test_a_constructor_is_found_under_init(checkout):
    ms = MethodSet()
    ms.add(MethodRef('com.example.Widget', '<init>', ('int',)), SEED, 0)
    assert _lines(pd.lines_for(ms, checkout), 'com.example.Widget') \
        == [6, 7, 8]


def test_an_unqualified_class_still_matches(checkout):
    """introspector writes bare class names for some receivers."""
    ms = MethodSet()
    ms.add(MethodRef('Widget', 'area', ('int', 'int')), CALLEE, 1)
    assert _lines(pd.lines_for(ms, checkout), 'com.example.Widget') \
        == [10, 11, 12]


def test_a_classless_caller_matches_only_when_the_name_is_unique(checkout):
    """A caller parsed out of source text has no class. `solo/0` names one
    method in the whole checkout, so it can be placed; `area/2` names two,
    so it is left out rather than guessed at."""
    ms = MethodSet()
    ms.add(MethodRef('', 'solo', ()), CALLER, 1)
    ms.add(MethodRef('', 'area', ('int', 'int')), CALLER, 1)
    ls = pd.lines_for(ms, checkout)
    assert _lines(ls, 'com.example.Gadget') == [8, 9, 10]
    assert _lines(ls, 'com.example.Widget') == []


def test_a_method_not_in_the_checkout_contributes_nothing(checkout):
    ms = MethodSet()
    ms.add(MethodRef('com.example.Widget', 'vanished', ()), SEED, 0)
    assert len(pd.lines_for(ms, checkout)) == 0


def test_the_nearest_ring_wins_on_a_shared_line(checkout):
    """`helper` sits inside `Inner`, which sits inside `Widget`'s file; a
    line claimed by a caller and a callee keeps the caller tag."""
    ms = MethodSet()
    ms.add(MethodRef('com.example.Widget', 'area', ('int', 'int')), CALLEE, 1)
    ms.add(MethodRef('com.example.Gadget', 'area', ('int', 'int')), CALLER, 1)
    ls = pd.lines_for(ms, checkout)
    assert ls.ring_of(LineRef('com.example.Widget', 11)) == CALLEE
    assert ls.ring_of(LineRef('com.example.Gadget', 5)) == CALLER


def test_an_unparsable_file_costs_only_its_own_methods(tmp_path, checkout):
    broken = os.path.join(checkout, 'com', 'example', 'Broken.java')
    with open(broken, 'w') as fh:
        fh.write('package com.example; class Broken { void x( }')
    ms = MethodSet()
    ms.add(MethodRef('com.example.Widget', 'area', ('int', 'int')), SEED, 0)
    assert _lines(pd.lines_for(ms, checkout), 'com.example.Widget') \
        == [10, 11, 12]


def test_lines_for_is_deterministic(checkout, closure18):
    ms = MethodSet()
    ms.add(MethodRef('com.example.Widget', 'area', ('int', 'int')), SEED, 0)
    ms.add(MethodRef('com.example.Gadget', 'solo', ()), CALLEE, 1)
    first = pd.lines_for(ms, checkout).to_dict()
    assert first == pd.lines_for(ms, checkout).to_dict()
