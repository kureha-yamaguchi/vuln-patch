"""Regression fence for root-cause neighbourhood resolution.

A touched function is matched to fuzz-introspector by name, and a bare
method name is ambiguous in every real project: commons-lang declares 14
methods called `translate` and 7 called `toBoolean`. Matching on the name
plus the argument COUNT alone picked a different class's method for 3 of
the 4 Defects4J bugs measured, and the neighbourhood then described that
other function.

Covers:
  * `_declaring_class`  — the class comes from the AST, so a nested class
    is `Outer.Inner` and not the file name.
  * `_match_fi_name`    — class first, then parameter TYPES, then arity;
    None rather than a guess; a note when it stays ambiguous.
  * `_is_touched_self`  — excludes the touched function by signature, so a
    different OVERLOAD of it survives (the Lang-6 shape, where the whole
    neighbourhood is another `translate`).
  * `_dedupe`           — one entry per function when the frontend reports
    it under two spellings, preferring the spelling the source declares.
  * `short_name`        — keeps the declaring class, so two overloads are
    not one word.

Every fixture asserts the NEW behaviour, so each test fails against the
name-and-arity matcher it replaces.
"""
import javalang
import pytest

from java.bug_context.analysis import TargetAnalyzer, TouchedFunction
from java.bug_context.call_graph import (mangled_param_types, qualified_label,
                                         short_name, simple_type)

# The Lang-6 shape: one class, three overloads of one name, one of them
# abstract. The 2-argument overload calls the 3-argument one.
LANG6 = """
package org.apache.commons.lang3.text.translate;
public abstract class CharSequenceTranslator {
    public abstract int translate(CharSequence input, int index, Writer out);
    public final String translate(CharSequence input) { return null; }
    public final void translate(CharSequence input, Writer out) {
        int consumed = translate(input, 0, out);
    }
}
"""

# The nested-class shape: CsvEscaper does not live in a CsvEscaper.java.
NESTED = """
package org.apache.commons.lang3;
public class StringEscapeUtils {
    static class CsvEscaper {
        public int translate(CharSequence input, int index, Writer out) {
            return 0;
        }
    }
}
"""

FI_TRANSLATE_3 = ('[org.apache.commons.lang3.text.translate'
                  '.CharSequenceTranslator].translate'
                  '(CharSequence,int,java.io.Writer)')
FI_TRANSLATE_2 = ('[org.apache.commons.lang3.text.translate'
                  '.CharSequenceTranslator].translate'
                  '(CharSequence,java.io.Writer)')
FI_TRANSLATE_1 = ('[org.apache.commons.lang3.text.translate'
                  '.CharSequenceTranslator].translate(CharSequence)')
FI_CODEPOINT_2 = ('[org.apache.commons.lang3.text.translate'
                  '.CodePointTranslator].translate(int,java.io.Writer)')
FI_CSV_3 = ('[org.apache.commons.lang3.StringEscapeUtils.CsvEscaper]'
            '.translate(CharSequence,int,java.io.Writer)')
# The frontend mis-types call arguments with the enclosing class. This is
# the 3-argument overload again, spelled wrongly.
FI_TRANSLATE_3_MISTYPED = ('[org.apache.commons.lang3.text.translate'
                           '.CharSequenceTranslator].translate'
                           '(CharSequence,CharSequenceTranslator,'
                           'java.io.Writer)')


def _touched(source, method_name, arity, types=None):
    """The TouchedFunction the AST pass would build for one method.

    `types` selects among overloads of equal arity — the case the matcher
    under test exists for, so the fixture must be able to name it too."""
    ta = TargetAnalyzer()
    tree = javalang.parse.parse(source)
    methods = ta._collect_methods(tree, source.splitlines())
    picked = [m for m in methods
              if m['name'] == method_name and len(m['param_types']) == arity
              and (types is None or m['param_types'] == types)]
    assert len(picked) == 1, [m['signature'] for m in methods]
    m = picked[0]
    return TouchedFunction(
        func_name=m['name'],
        func_signature=m['signature'],
        func_source='',
        func_class=m['class'],
        func_class_fq=ta._qualify(tree, m['class']),
        func_param_types=list(m['param_types']),
        overload_types=[list(o['param_types']) for o in methods
                        if o['name'] == m['name']
                        and o['class'] == m['class']],
    )


# --------------------------------------------------------------------------
# the declaring class

def test_declaring_class_is_the_outer_class():
    fn = _touched(LANG6, 'translate', 2)
    assert fn.func_class == 'CharSequenceTranslator'
    assert fn.func_class_fq == ('org.apache.commons.lang3.text.translate'
                                '.CharSequenceTranslator')


def test_declaring_class_keeps_the_nesting():
    """A file-name match would call this StringEscapeUtils."""
    fn = _touched(NESTED, 'translate', 3)
    assert fn.func_class == 'StringEscapeUtils.CsvEscaper'
    assert fn.func_class_fq == ('org.apache.commons.lang3'
                                '.StringEscapeUtils.CsvEscaper')


def test_overload_set_holds_every_same_named_declaration():
    fn = _touched(LANG6, 'translate', 2)
    assert sorted(len(o) for o in fn.overload_types) == [1, 2, 3]


# --------------------------------------------------------------------------
# matching a touched function to introspector

def test_match_prefers_the_declaring_class():
    """CodePointTranslator.translate(int, Writer) also takes 2 arguments."""
    ta = TargetAnalyzer()
    fn = _touched(LANG6, 'translate', 2)
    # The wrong class is listed FIRST, which is what the old matcher took.
    index = {'translate': [FI_CODEPOINT_2, FI_TRANSLATE_2]}
    assert ta._match_fi_name(fn, index) == FI_TRANSLATE_2


def test_match_separates_two_overloads_of_equal_arity():
    """toBoolean(String) and toBoolean(Boolean) both take one argument."""
    ta = TargetAnalyzer()
    source = """
    package org.apache.commons.lang;
    public class BooleanUtils {
        public static boolean toBoolean(Boolean b) { return false; }
        public static boolean toBoolean(String str) { return false; }
    }
    """
    fn = _touched(source, 'toBoolean', 1, ['String'])
    assert fn.func_param_types == ['String']
    index = {'toBoolean': ['[org.apache.commons.lang.BooleanUtils]'
                           '.toBoolean(Boolean)',
                           '[org.apache.commons.lang.BooleanUtils]'
                           '.toBoolean(String)']}
    assert ta._match_fi_name(fn, index).endswith('toBoolean(String)')


def test_match_separates_two_classes_of_the_same_simple_name():
    """commons-lang declares NumberUtils in two packages."""
    ta = TargetAnalyzer()
    source = """
    package org.apache.commons.lang.math;
    public class NumberUtils {
        public static Number createNumber(String str) { return null; }
    }
    """
    fn = _touched(source, 'createNumber', 1)
    index = {'createNumber': [
        '[org.apache.commons.lang.NumberUtils].createNumber(String)',
        '[org.apache.commons.lang.math.NumberUtils].createNumber(String)']}
    assert ta._match_fi_name(fn, index) == (
        '[org.apache.commons.lang.math.NumberUtils].createNumber(String)')


def test_match_refuses_rather_than_guessing_another_class():
    """A guess reports a DIFFERENT function's neighbourhood as this one's."""
    ta = TargetAnalyzer()
    fn = _touched(LANG6, 'translate', 2)
    notes = []
    assert ta._match_fi_name(fn, {'translate': [FI_CODEPOINT_2]},
                             notes) is None
    assert notes == ['class_unresolved:CharSequenceTranslator.translate']


def test_match_notes_an_unknown_name():
    ta = TargetAnalyzer()
    fn = _touched(LANG6, 'translate', 2)
    notes = []
    assert ta._match_fi_name(fn, {}, notes) is None
    assert notes == ['unresolved:translate']


def test_match_records_a_surviving_ambiguity():
    """Two candidates the filters cannot separate must not pass silently."""
    ta = TargetAnalyzer()
    fn = _touched(LANG6, 'translate', 2)
    twin = FI_TRANSLATE_2.replace('java.io.Writer', 'Writer')
    notes = []
    got = ta._match_fi_name(fn, {'translate': [FI_TRANSLATE_2, twin]}, notes)
    assert got in (FI_TRANSLATE_2, twin)
    assert notes == ['ambiguous:translate']


# --------------------------------------------------------------------------
# excluding the touched function without losing its overloads

def test_a_different_overload_is_not_the_touched_function():
    """The Lang-6 regression: every neighbour is another `translate`, and a
    bare-name test discarded all of them."""
    ta = TargetAnalyzer()
    fn = _touched(LANG6, 'translate', 2)
    fn.fi_name = FI_TRANSLATE_2
    assert not ta._is_touched_self(FI_TRANSLATE_3, [fn])
    assert not ta._is_touched_self(FI_TRANSLATE_1, [fn])


def test_the_touched_function_itself_is_excluded():
    ta = TargetAnalyzer()
    fn = _touched(LANG6, 'translate', 2)
    fn.fi_name = FI_TRANSLATE_2
    assert ta._is_touched_self(FI_TRANSLATE_2, [fn])


def test_a_mistyped_spelling_of_the_touched_function_is_excluded():
    """Same class, same name, same arity, types the source never declares."""
    ta = TargetAnalyzer()
    fn = _touched(LANG6, 'translate', 2)
    fn.fi_name = FI_TRANSLATE_2
    mistyped = FI_TRANSLATE_2.replace('java.io.Writer',
                                      'CharSequenceTranslator')
    assert mangled_param_types(mistyped) == ['CharSequence',
                                             'CharSequenceTranslator']
    assert ta._is_touched_self(mistyped, [fn])


def test_another_class_is_never_the_touched_function():
    ta = TargetAnalyzer()
    fn = _touched(LANG6, 'translate', 2)
    fn.fi_name = FI_TRANSLATE_2
    assert not ta._is_touched_self(FI_CSV_3, [fn])
    assert not ta._is_touched_self(FI_CODEPOINT_2, [fn])


# --------------------------------------------------------------------------
# one entry per function, named readably

def test_dedupe_collapses_two_spellings_of_one_function():
    ta = TargetAnalyzer()
    fn = _touched(LANG6, 'translate', 2)
    fn.fi_name = FI_TRANSLATE_2
    got = ta._dedupe([FI_TRANSLATE_3_MISTYPED, FI_TRANSLATE_3], [fn])
    assert got == [FI_TRANSLATE_3], 'the declared spelling must win'


def test_dedupe_keeps_genuinely_different_arities():
    ta = TargetAnalyzer()
    fn = _touched(LANG6, 'translate', 2)
    fn.fi_name = FI_TRANSLATE_2
    got = ta._dedupe([FI_TRANSLATE_3, FI_TRANSLATE_1], [fn])
    assert got == [FI_TRANSLATE_3, FI_TRANSLATE_1]


def test_short_name_keeps_the_declaring_class():
    """Stripping the receiver reduced every overload to one word."""
    assert short_name(FI_TRANSLATE_3) == 'CharSequenceTranslator.translate'
    assert short_name(FI_CSV_3) == 'CsvEscaper.translate'
    assert short_name('[Boolean].booleanValue()') == 'Boolean.booleanValue'
    assert short_name('org.apache.commons.lang.NumberUtils.createInteger') \
        == 'NumberUtils.createInteger'
    assert short_name('createInteger') == 'createInteger'


def test_labels_add_parameters_only_when_two_entries_clash():
    ta = TargetAnalyzer()
    assert ta._labels_for([FI_TRANSLATE_3]) == [
        'CharSequenceTranslator.translate']
    assert ta._labels_for([FI_TRANSLATE_3, FI_TRANSLATE_1]) == [
        'CharSequenceTranslator.translate(CharSequence, int, Writer)',
        'CharSequenceTranslator.translate(CharSequence)']
    assert ta._labels_for([FI_TRANSLATE_3, FI_CSV_3]) == [
        'CharSequenceTranslator.translate', 'CsvEscaper.translate']


# --------------------------------------------------------------------------
# the type helpers

@pytest.mark.parametrize('given, want', [
    ('java.io.Writer', 'Writer'),
    ('java.util.Map<String, Integer>', 'Map'),
    ('byte[]', 'byte[]'),
    ('  CharSequence ', 'CharSequence'),
])
def test_simple_type(given, want):
    assert simple_type(given) == want


def test_mangled_param_types():
    assert mangled_param_types(FI_TRANSLATE_3) == ['CharSequence', 'int',
                                                   'Writer']
    assert mangled_param_types('[C].m()') == []
    assert mangled_param_types('[C].m(byte[],java.util.List<String>)') == [
        'byte[]', 'List']


def test_qualified_label():
    assert qualified_label(FI_TRANSLATE_3) == (
        'CharSequenceTranslator.translate(CharSequence, int, Writer)')
