"""Rebuild the patch-derived set P from what a finished run left behind.

MEASUREMENT ONLY — nothing in the pipeline imports this.

`neighbourhood.build` needs a live fuzz-introspector project, which an
archived run no longer has. What it does have is the analysis step's own
output: the `PatchContext` the pipeline built at the time, which already
lists the touched methods, their callers and their reachable callees. So
for archived runs P is *read back* rather than recomputed, and this module
is the reader.

Two entry points for the same object in two containers:

  `from_trace(path)`        the `trace.md` an archived run wrote, where the
                            context sits in a fenced JSON block;
  `from_context_json(path)` a plain JSON file holding the same dict — the
                            per-leg `context.json` future runs will write.

Plus `lines_for`, which turns a set of methods into the set of source lines
those methods occupy, so a method-level set can be compared against
line-level coverage.

WHAT AN ARCHIVE CANNOT TELL US.  Three losses, all of them because the
context dict was built for a prompt and not for a measurement:

  1. Callee DEPTH is not recorded. The context stores a flat list of
     reachable names with no distance, so every callee read back from an
     archive is tagged depth 1. Depth-sensitive numbers must not be
     computed from `from_trace` output.
  2. Callers are stored as SOURCE TEXT (the whole calling method's body),
     not as names, because that is what the prompt showed the model. The
     method NAME is recovered from the declaration line with a regex and
     the declaring CLASS is simply unknown, so a caller ref has
     `class_fq == ''`. A future `context.json` carrying an `xref_names`
     list removes this guess, and `from_context_json` prefers it when it
     is there.
  3. `root_cause_reachable` holds display LABELS (`Compiler.hasErrors`,
     `IR.block(Node, Node)`), not mangled names — the prompt renderer
     shortened them. A label with no argument list carries no arity, so it
     is matched loosely against what the per-function `reachable` lists
     already produced and only added when it is genuinely new.
"""
from __future__ import annotations

import json
import os
import re
from typing import Dict, List, Optional, Tuple

from java.execution import diffcov
from java.measurements.locations import (CALLEE, CALLER, SEED, LineRef,
                                         LineSet, MethodIndex, MethodRef,
                                         MethodSet, class_top_from_source,
                                         from_introspector, from_javalang,
                                         simple_type, top_level_of,
                                         _split_top_level)

__all__ = ['from_trace', 'from_context_json', 'lines_for',
           'context_dict_from_trace', 'method_set_from_context']


# ---------------------------------------------------------------------------
# Reading the context dict out of a trace.md
# ---------------------------------------------------------------------------

# The analysis step's heading, e.g. '## [2] ⚙️ analysis (TargetAnalyzer)'.
_ANALYSIS_HEADING = re.compile(r'^##\s*\[\d+\]\s*\S*\s*analysis\s*\(TargetAnalyzer\)',
                               re.MULTILINE)


def context_dict_from_trace(trace_md_path: str) -> dict:
    """The `PatchContext.as_dict()` dict recorded in a run's `trace.md`.

    A trace is a flat markdown log: a `## [N] ... ` heading per pipeline
    step, then that step's output. The analysis step's output is one
    ```json fenced block. We find the heading, then take the first fenced
    JSON block after it and stop at the fence that closes it.

    Raises ValueError when the section or its JSON block is missing."""
    with open(trace_md_path, encoding='utf-8') as fh:
        text = fh.read()
    m = _ANALYSIS_HEADING.search(text)
    if m is None:
        raise ValueError(f"no 'analysis (TargetAnalyzer)' section in "
                         f"{trace_md_path}")
    rest = text[m.end():]
    # Stop at the next step heading so we cannot wander into another step.
    nxt = re.search(r'^##\s*\[\d+\]', rest, re.MULTILINE)
    if nxt:
        rest = rest[:nxt.start()]
    fence = re.search(r'^```json\s*$', rest, re.MULTILINE)
    if fence is None:
        raise ValueError(f"analysis section of {trace_md_path} has no "
                         f"```json block")
    body = rest[fence.end():]
    close = re.search(r'^```\s*$', body, re.MULTILINE)
    if close is None:
        raise ValueError(f"unterminated ```json block in {trace_md_path}")
    return json.loads(body[:close.start()])


# ---------------------------------------------------------------------------
# Names: mangled, display label, or caller source text
# ---------------------------------------------------------------------------

# 'Compiler.hasErrors', 'IR.block(Node, Node)', 'PerformanceTracker.<init>'.
_LABEL_RE = re.compile(r'^(?P<cls>[\w.$]+)\.(?P<name>[\w$]+|<init>|<clinit>)'
                       r'(?:\((?P<args>[^)]*)\))?$')


def _ref_from_label(label: str) -> Tuple[Optional[MethodRef], bool]:
    """A display label -> (ref, arity_known).

    `arity_known` is False for a label with no argument list: the renderer
    only appends types when two kept methods would otherwise print the same
    word, so most labels carry no arity at all and must be matched with
    arity ignored."""
    m = _LABEL_RE.match(label.strip())
    if m is None:
        return None, False
    cls = m.group('cls').replace('$', '.')
    args = m.group('args')
    if args is None:
        return MethodRef(cls, m.group('name'), ()), False
    params = tuple(simple_type(a) for a in _split_top_level(args))
    return MethodRef(cls, m.group('name'), params), True


def _ref_from_name(name: str) -> Tuple[Optional[MethodRef], bool]:
    """Whatever spelling the context used -> (ref, arity_known).

    Tries the mangled introspector form first, then the display label."""
    ref = from_introspector(name)
    if ref is not None:
        return ref, True
    return _ref_from_label(name)


# Java keywords that are followed by '(' and would otherwise read as a
# method declaration if the xref body's first line is not the signature.
_NOT_A_DECL = {'if', 'for', 'while', 'switch', 'catch', 'return', 'new',
               'do', 'else', 'try', 'assert', 'throw', 'super', 'this',
               'synchronized', 'case'}

_DECL_RE = re.compile(
    r'^[ \t]*'
    r'(?:(?:public|protected|private|static|final|abstract|synchronized|'
    r'native|strictfp|default|transient|volatile)[ \t]+)*'
    r'(?:<[^>\n]*>[ \t]*)?'                       # generic method: <T>
    r'(?:(?P<ret>[\w.$]+(?:<[^;{}\n]*>)?(?:\s*\[\s*\])*)[ \t]+)?'
    r'(?P<name>[A-Za-z_$][\w$]*)[ \t]*\(',
    re.MULTILINE)


def _balanced_args(text: str, open_paren: int) -> Optional[Tuple[str, int]]:
    """(argument text, index of the closing ')') for the '(' at
    `open_paren`.

    A parameter list can run over several lines, so the closing bracket is
    found by counting depth rather than by a regex."""
    depth = 0
    for i in range(open_paren, len(text)):
        c = text[i]
        if c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                return text[open_paren + 1:i], i
    return None


# What may sit between a signature's ')' and its body: a throws clause.
_AFTER_SIGNATURE_RE = re.compile(r'^\s*(?:throws\s+[\w.$,\s]+)?\{')


def _has_a_body(text: str, close_paren: int) -> bool:
    """True when a '{' follows the closing bracket, allowing a `throws`
    clause in between.

    This is what separates a declaration from a plain CALL: `b();` inside
    the body looks exactly like a no-argument method header until you look
    at what comes next."""
    return bool(_AFTER_SIGNATURE_RE.match(text[close_paren + 1:]))


def _param_type(decl: str) -> Optional[str]:
    """One formal parameter's declaration -> its simple type name.

    'final String expectedResult' -> 'String'; 'String[] original' ->
    'String[]'; 'String original[]' -> 'String[]'; 'Map<String,Integer> m'
    -> 'Map'."""
    d = re.sub(r'@\w+(?:\([^)]*\))?', ' ', decl)
    d = re.sub(r'\bfinal\b', ' ', d).strip()
    if not d:
        return None
    m = re.match(r'^(?P<type>.*?)\s+(?P<var>[A-Za-z_$][\w$]*)\s*'
                 r'(?P<dims>(?:\[\s*\])*)$', d, re.S)
    if m is None:
        return simple_type(d)
    return simple_type(m.group('type') + m.group('dims').replace(' ', ''))


def caller_ref_from_source(source: str) -> Optional[MethodRef]:
    """The method a caller's SOURCE TEXT declares, as a class-less ref.

    Archived contexts store each cross-reference as the whole body of the
    calling method, so the only identity available is the declaration line.
    The first line that parses as `[modifiers] [type] name(...)` followed
    by a body wins; annotations, `if (`/`for (` lines and plain calls such
    as `b();` are skipped. The declaring class
    is not in the text, so the ref carries `class_fq == ''` and can only
    ever be matched by (name, arity)."""
    for m in _DECL_RE.finditer(source or ''):
        name = m.group('name')
        if name in _NOT_A_DECL or (m.group('ret') or '') in _NOT_A_DECL:
            continue
        found = _balanced_args(source, m.end() - 1)
        if found is None:
            continue
        args, close = found
        if not _has_a_body(source, close):
            continue
        params = []
        for part in _split_top_level(args):
            t = _param_type(part)
            if t:
                params.append(t)
        return MethodRef('', name, tuple(params))
    return None


# ---------------------------------------------------------------------------
# Context dict -> MethodSet
# ---------------------------------------------------------------------------

def _already_present(index: MethodIndex, ref: MethodRef,
                     arity_known: bool) -> bool:
    """Does the set already hold the method this label names?

    `MethodIndex.lookup` answers None both when nothing matches and when
    SEVERAL things match, and here those two must be told apart: a label
    that matches two overloads is still a duplicate of one of them, and
    adding it would make the set bigger than the neighbourhood is.

    So the candidate lists are read directly. With an arity, a match on
    (simple class, name, arity) counts; a label with no argument list
    carries no arity, so any method of the same class and name counts."""
    if arity_known and index.loose.get(ref.loose_key):
        return True
    if arity_known:
        return False
    return bool(index.by_name.get(f"{ref.class_simple}.{ref.name}"))

def method_set_from_context(ctx: dict) -> MethodSet:
    """The patch-derived set P as an archived `PatchContext` recorded it.

    Rings:
      SEED    one per entry of `functions` (mangled `fi_name` preferred,
              AST class/name/types otherwise), depth 0;
      CALLEE  each entry of a function's `reachable` list, plus anything in
              the context-level `root_cause_reachable` that the reachable
              lists did not already name. DEPTH IS ALWAYS 1 — the archive
              does not record how far down the walk found it.
      CALLER  one per `xrefs` entry, recovered from source text (see
              `caller_ref_from_source`) unless the context carries an
              `xref_names` list of mangled names, which is used instead.

    Names that parse as neither a mangled name nor a display label are
    listed in `MethodSet.unmatched` rather than silently dropped."""
    ms = MethodSet()
    functions = ctx.get('functions') or []

    # -- seeds ---------------------------------------------------------
    for fn in functions:
        if not isinstance(fn, dict):
            continue
        ref = None
        if fn.get('fi_name'):
            ref = from_introspector(fn['fi_name'])
        if ref is None and fn.get('func_name'):
            ref = from_javalang(fn.get('func_class_fq') or
                                fn.get('func_class') or '',
                                fn['func_name'],
                                fn.get('func_param_types') or [])
        if ref is None:
            ms.unmatched.append(str(fn.get('func_name') or fn.get('fi_name')))
            continue
        ms.add(ref, SEED, 0)

    # -- callees: the per-function reachable lists first ---------------
    for fn in functions:
        if not isinstance(fn, dict):
            continue
        for name in (fn.get('reachable') or []):
            ref, _ = _ref_from_name(name)
            if ref is None:
                if name not in ms.unmatched:
                    ms.unmatched.append(name)
                continue
            ms.add(ref, CALLEE, 1)

    # -- callees: the context-level union, in label form ---------------
    # `root_cause_reachable` is the per-function `reachable` lists filtered
    # and RE-SPELLED as prompt labels, so nearly every entry is a method
    # the loop above already added under its mangled name. Adding it again
    # under the short spelling would count one method twice, so a label is
    # only added when the set holds nothing it could refer to.
    index = MethodIndex(list(ms.items))
    for label in (ctx.get('root_cause_reachable') or []):
        ref, arity_known = _ref_from_label(label)
        if ref is None:
            if label not in ms.unmatched:
                ms.unmatched.append(label)
            continue
        if _already_present(index, ref, arity_known):
            continue
        ms.add(ref, CALLEE, 1)

    # -- callers -------------------------------------------------------
    for fn in functions:
        if not isinstance(fn, dict):
            continue
        names = fn.get('xref_names')
        if names:
            for name in names:
                ref, _ = _ref_from_name(name)
                if ref is None:
                    if name not in ms.unmatched:
                        ms.unmatched.append(name)
                    continue
                ms.add(ref, CALLER, 1)
            continue
        for src in (fn.get('xrefs') or []):
            ref = caller_ref_from_source(src)
            if ref is None:
                ms.unmatched.append('xref-source-unparsed')
                continue
            ms.add(ref, CALLER, 1)
    return ms


def from_trace(trace_md_path: str) -> MethodSet:
    """P for an archived run, read out of its `trace.md`.

    See `method_set_from_context` for the rings and
    `context_dict_from_trace` for where in the file the dict lives. Callee
    depth is not recorded in an archive, so every callee comes back at
    depth 1."""
    return method_set_from_context(context_dict_from_trace(trace_md_path))


def from_context_json(path: str) -> MethodSet:
    """P from a JSON file holding `PatchContext.as_dict()`.

    The per-leg `context.json` future runs will write. Identical to
    `from_trace` except that when a function entry carries `xref_names`
    (mangled names of its callers) those are used and no source text has
    to be parsed."""
    with open(path, encoding='utf-8') as fh:
        return method_set_from_context(json.load(fh))


# ---------------------------------------------------------------------------
# Methods -> lines
# ---------------------------------------------------------------------------

def _line_of(offsets: List[int], pos: int) -> int:
    """1-based line number of a character offset, by binary search over
    the offsets each line starts at."""
    lo, hi = 0, len(offsets) - 1
    while lo < hi:
        mid = (lo + hi + 1) // 2
        if offsets[mid] <= pos:
            lo = mid
        else:
            hi = mid - 1
    return lo + 1


def _line_starts(source: str) -> List[int]:
    offsets = [0]
    for i, ch in enumerate(source):
        if ch == '\n':
            offsets.append(i + 1)
    return offsets


def _index_checkout(source_root: str) -> Tuple[Dict[tuple, list], Dict[tuple, list]]:
    """Every method declared under `source_root`, indexed two ways.

    Returns (by_class, by_name) where

      by_class[(top-level class, method name, arity)] -> [span, ...]
      by_name[(method name, arity)]                   -> [span, ...]

    and a span is (class_top_fq, first line, last line). Files that do not
    parse are skipped — this is a measurement, so a broken file costs the
    methods in it and nothing else. Walk order is sorted so two runs over
    the same checkout produce the same lists."""
    by_class: Dict[tuple, list] = {}
    by_name: Dict[tuple, list] = {}
    for dirpath, dirnames, filenames in os.walk(source_root):
        dirnames.sort()
        for fname in sorted(filenames):
            if not fname.endswith('.java'):
                continue
            full = os.path.join(dirpath, fname)
            try:
                with open(full, encoding='utf-8', errors='replace') as fh:
                    source = fh.read()
            except OSError:
                continue
            rel = os.path.relpath(full, source_root)
            class_top = class_top_from_source(rel, source)
            try:
                decls = diffcov.method_declarations(source)
            except Exception:
                continue
            offsets = _line_starts(source)
            for d in decls:
                span = (class_top, _line_of(offsets, d['start']),
                        _line_of(offsets, d['end']))
                arity = len(d.get('param_types') or [])
                names = [d['name']]
                # A constructor is declared under the class's simple name;
                # a MethodRef spells it '<init>'. Register both.
                if d['name'] == (d.get('class_name') or '').rsplit('.', 1)[-1]:
                    names.append('<init>')
                for nm in names:
                    top = top_level_of(d.get('class_name') or class_top)
                    by_class.setdefault((top, nm, arity), []).append(span)
                    by_class.setdefault((top.rsplit('.', 1)[-1], nm, arity),
                                        []).append(span)
                    by_name.setdefault((nm, arity), []).append(span)
    return by_class, by_name


def lines_for(method_set: MethodSet, source_root: str) -> LineSet:
    """Every source line the methods in `method_set` occupy.

    Walks the `.java` files under `source_root`, asks
    `diffcov.method_declarations` for each declaration's character span,
    and matches a `MethodRef` to a declaration on

        top-level class + method name + number of parameters

    — parameter TYPES are not compared, because the mangled names the
    call graph produces mis-spell them often enough that type equality
    loses real matches. A ref whose class is fully qualified must match
    the qualified class; a ref with only a simple class name (introspector
    writes those for JDK types) matches on the simple name; a ref with no
    class at all — a caller recovered from source text, see
    `caller_ref_from_source` — matches only when (name, arity) is unique
    in the whole checkout, and is skipped otherwise.

    Every line from the declaration's first line to its closing brace is
    added, tagged with the ring the method carries. A line inside a nested
    declaration belongs to both, and `LineSet.add` keeps the nearer ring.
    Methods that cannot be located contribute nothing."""
    by_class, by_name = _index_checkout(source_root)
    out = LineSet()
    for ref, tagged in sorted(method_set.items.items()):
        arity = len(ref.params)
        spans = None
        if ref.class_fq:
            top = top_level_of(ref.class_fq)
            spans = by_class.get((top, ref.name, arity))
            if not spans:
                spans = by_class.get((top.rsplit('.', 1)[-1], ref.name, arity))
        else:
            cands = by_name.get((ref.name, arity)) or []
            # Dedupe: the same declaration is registered under several keys.
            uniq = sorted(set(cands))
            spans = uniq if len(uniq) == 1 else None
        if not spans:
            continue
        for class_top, first, last in sorted(set(spans)):
            for line in range(first, last + 1):
                out.add(LineRef(class_top, line), tagged.ring)
    return out
