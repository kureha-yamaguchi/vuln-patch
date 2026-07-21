"""Class-level codebase context for the relation synthesizer and verifier.

Both LLM stages currently see only the PATCHED METHOD, but on inspection of
real tasks the discriminating invariant routinely lives elsewhere in the

class or type hierarchy:
  * a CONSTRUCTOR-established invariant (a size field computed as a product
    of dimension sizes — the touched accessor merely returns it);
  * a SIBLING method that must agree (a complementary function documented
    as summing to 1 with the touched one; an accessor pair that must
    round-trip);
  * the METHOD/CLASS JAVADOC contract (never returns null for a valid id;
    denominator always positive after reduction).
None of that is visible from the touched method's body alone, so the
synthesizer couldn't propose those relations and the verifier couldn't use
them to judge soundness (e.g. it kept an oracle comparing a fixed literal
against a STOCHASTIC sampler's output because nothing told it the method
was stochastic).

This module renders a CLASS SKELETON — class javadoc, declaration, fields,
constructors, and every method's javadoc + signature (full bodies only for
the touched methods) — plus the same skeletons for supertypes and the
most-referenced collaborator types. Pure regex/brace-matching, best-effort,
capped, fails soft to fewer blocks. Reads only the buggy checkout: no
developer fix, no labels.
"""
import os
import re
from typing import Dict, List, Optional, Set
from java.parsing.java_source import match_brace

_CLASS_DECL_RE = re.compile(
    r'(?:public\s+|abstract\s+|final\s+|strictfp\s+)*'
    r'(?:class|interface|enum)\s+(\w+)'
    r'(?:\s+extends\s+([\w.,\s<>]+?))?'
    r'(?:\s+implements\s+([\w.,\s<>]+?))?\s*\{', re.DOTALL)

# A member declaration: javadoc (optional) + modifiers + signature, ending
# in '{' (method/ctor with body) or ';' (field / abstract method).
_MEMBER_RE = re.compile(
    r'(?P<doc>/\*\*.*?\*/\s*)?'
    r'(?P<decl>(?:@\w+(?:\([^)]*\))?\s*)*'
    r'(?:public|protected|private)\s'
    r'[^;{}]*?)'
    r'(?P<end>[;{])', re.DOTALL)


def _compact_doc(doc: str, max_lines: int = 12) -> str:
    # Strip the enclosing /** … */ from the WHOLE comment first, so a
    # single-line javadoc (`/** text */`) keeps its text instead of being
    # discarded for starting with '/**' (the old bug dropped every
    # one-line contract — e.g. Lang-7's createNumber doc).
    body = (doc or '').strip()
    if body.startswith('/**'):
        body = body[3:]
    elif body.startswith('/*'):
        body = body[2:]
    if body.endswith('*/'):
        body = body[:-2]
    lines = [ln.strip().lstrip('*').strip() for ln in body.splitlines()]
    lines = [ln for ln in lines if ln]
    if len(lines) > max_lines:
        lines = lines[:max_lines] + ['…']
    return '\n'.join('     * ' + ln for ln in lines)


def class_skeleton(source: str,
                   full_body_methods: Set[str] = frozenset(),
                   max_chars: int = 9000,
                   max_ctor_body_chars: int = 1200) -> str:
    """Render `source` as a skeleton: class javadoc + declaration, then per
    member its (compacted) javadoc and signature — with the full body kept
    only for methods named in `full_body_methods` and for CONSTRUCTORS.
    Constructor bodies are kept because they are where class invariants get
    ESTABLISHED (a size field computed as a product of dimensions, a sign
    normalisation, a canonical ordering) — the exact facts the relation
    stages need and that no signature or javadoc reliably states. The
    output is what a maintainer would skim to learn the class's CONTRACTS,
    at a fraction of the tokens of the raw file."""
    if not source:
        return ''
    m = _CLASS_DECL_RE.search(source)
    if not m:
        return source[:max_chars]
    class_name = m.group(1)
    out: List[str] = []
    # Class javadoc: the /** ... */ immediately before the declaration.
    pre = source[:m.start()]
    dm = re.search(r'/\*\*(?:[^*]|\*(?!/))*\*/\s*$', pre, re.DOTALL)
    if dm:
        out.append(_compact_doc(dm.group(0)))
    decl_line = source[m.start():source.find('{', m.start())].strip()
    out.append(decl_line + ' {')

    body_open = source.find('{', m.start())
    body_close = match_brace(source, body_open)
    body = source[body_open + 1:body_close if body_close > 0 else len(source)]

    # Emit members as (is_touched, piece) so truncation can PROTECT the
    # touched methods' javadoc+body. A big class whose patched method sits
    # near the bottom used to lose exactly that method to the raw
    # character cut — and its javadoc states the contract the relations
    # need most (Lang-7's createNumber doc was truncated away). P2.1.
    members: List[tuple] = []
    pos = 0
    while pos < len(body):
        mm = _MEMBER_RE.search(body, pos)
        if not mm:
            break
        decl = ' '.join(mm.group('decl').split())
        doc = mm.group('doc') or ''
        if mm.group('end') == ';':
            piece = (_compact_doc(doc) + '\n' if doc.strip() else '')
            piece += f'    {decl};'
            members.append((False, piece))
            pos = mm.end()
            continue
        open_idx = mm.end() - 1
        close_idx = match_brace(body, open_idx)
        if close_idx < 0:
            pos = mm.end()
            continue
        name = None
        if '(' in decl:
            head = decl[:decl.index('(')].strip()
            name = head.split()[-1] if head.split() else None
        piece = (_compact_doc(doc) + '\n' if doc.strip() else '')
        is_ctor = name == class_name
        is_touched = bool(name and name in full_body_methods)
        if is_touched or (is_ctor and close_idx - open_idx
                          <= max_ctor_body_chars):
            piece += '    ' + decl + ' ' + body[open_idx:close_idx + 1]
        else:
            piece += f'    {decl} {{ … }}'
        members.append((is_touched, piece))
        pos = close_idx + 1

    header = '\n'.join(out)
    footer = '\n}'
    budget = max_chars - len(header) - len(footer)
    kept, running = [], 0
    # First pass: reserve budget for every touched-method piece (their
    # javadoc + body), in source order.
    for is_touched, piece in members:
        if is_touched:
            kept.append(piece)
            running += len(piece) + 1
    truncated = False
    # Second pass: fill the remainder with non-touched skeleton pieces,
    # keeping source order by re-walking and inserting where they fit.
    result_pieces = []
    for is_touched, piece in members:
        if is_touched:
            result_pieces.append(piece)
        elif running + len(piece) + 1 <= budget:
            result_pieces.append(piece)
            running += len(piece) + 1
        else:
            truncated = True
    text = header + '\n' + '\n'.join(result_pieces) + footer
    if truncated:
        text += '\n// … (non-patched members truncated; patched methods kept)'
    return text


def _find_class_file(buggy_dir: str, simple_name: str,
                     _cache: Dict[str, Dict[str, str]] = {}) -> Optional[str]:
    """Locate `<simple_name>.java` under the checkout's main source tree
    (tests excluded). Walk once per checkout, then O(1)."""
    idx = _cache.get(buggy_dir)
    if idx is None:
        idx = {}
        for root, _dirs, files in os.walk(buggy_dir):
            if ('/test' in root or root.endswith('/tests')
                    or '/fuzz' in root):
                continue
            for f in files:
                if f.endswith('.java'):
                    # Prefer main-source paths on name collisions.
                    key = f[:-5]
                    path = os.path.join(root, f)
                    if key not in idx or '/main/' in path:
                        idx[key] = path
        _cache[buggy_dir] = idx
    return idx.get(simple_name)


_TYPE_REF_RE = re.compile(r'\b([A-Z]\w{2,})\b')
_JDKISH = frozenset({
    'String', 'Object', 'Integer', 'Long', 'Double', 'Float', 'Boolean',
    'Character', 'Byte', 'Short', 'Math', 'System', 'Override',
    'Exception', 'RuntimeException', 'IllegalArgumentException',
    'IllegalStateException', 'NullPointerException', 'Throwable',
    'StringBuilder', 'StringBuffer', 'Number', 'Comparable', 'Serializable',
    'List', 'ArrayList', 'Map', 'HashMap', 'Set', 'HashSet', 'Iterator',
    'Collection', 'Arrays', 'Collections', 'Deprecated',
    'SuppressWarnings', 'FIXME', 'TODO',
})


def assemble_class_context(buggy_dir: str,
                           modified_files: List[str],
                           touched_method_names: List[str],
                           max_supertypes: int = 3,
                           max_collaborators: int = 5,
                           per_class_chars: int = 12000,
                           total_chars: int = 48000,
                           test_sources: Optional[List[str]] = None
                           ) -> List[str]:
    """Labeled skeleton blocks for (1) each patched class — full bodies for
    the touched methods, (2) its supertypes/interfaces resolvable in the
    source tree, (3) the classes the failing TEST itself constructs/reads
    (role="test-subject"), (4) the most-referenced collaborator types in
    the touched method bodies. Ordered by relevance; truncated by the
    total budget.

    (3) exists because the documented contract that convicts a patch
    routinely lives in the test's SUBJECT class rather than the patched
    one: a patch to an abstract/parent method leaves the subject class's
    documented accessor formula wrong, and neither the supertype walk
    (wrong direction) nor the collaborator scan (patched file only) ever
    surfaces it — the synthesizer then cannot propose the one relation
    that matters (batch4 Math-2-o: six relations, all anchored on the
    patched method's class, none on the subject's documented formula)."""
    blocks: List[str] = []
    seen_classes: Set[str] = set()
    touched = set(touched_method_names or [])
    supertype_names: List[str] = []
    collab_counts: Dict[str, int] = {}
    test_subject_counts: Dict[str, int] = {}
    for ts in test_sources or []:
        # Constructed types outrank merely-mentioned ones.
        for t in re.findall(r'\bnew\s+([A-Z]\w+)\s*\(', ts or ''):
            if t not in _JDKISH:
                test_subject_counts[t] = test_subject_counts.get(t, 0) + 3
        for t in _TYPE_REF_RE.findall(ts or ''):
            if t not in _JDKISH:
                test_subject_counts[t] = test_subject_counts.get(t, 0) + 1

    for rel in modified_files or []:
        path = os.path.join(buggy_dir, rel)
        try:
            src = open(path, encoding='utf-8', errors='replace').read()
        except OSError:
            continue
        cls = os.path.basename(rel)[:-5] if rel.endswith('.java') else rel
        seen_classes.add(cls)
        skel = class_skeleton(src, full_body_methods=touched,
                              max_chars=per_class_chars)
        blocks.append(f'<class name="{cls}" role="patched">\n{skel}\n</class>')
        dm = _CLASS_DECL_RE.search(src)
        if dm:
            for grp in (dm.group(2), dm.group(3)):
                for name in re.findall(r'[\w.]+', grp or ''):
                    simple = name.split('.')[-1]
                    if simple not in seen_classes:
                        supertype_names.append(simple)
        # Collaborator candidates: types referenced inside the file's
        # touched-method region (approximated by the whole file when the
        # region isn't isolatable — counts still rank same-class helpers
        # low because they aren't capitalized type refs).
        for t in _TYPE_REF_RE.findall(src):
            if t not in _JDKISH and t not in seen_classes:
                collab_counts[t] = collab_counts.get(t, 0) + 1

    for name in supertype_names[:max_supertypes]:
        path = _find_class_file(buggy_dir, name)
        if not path or name in seen_classes:
            continue
        try:
            src = open(path, encoding='utf-8', errors='replace').read()
        except OSError:
            continue
        seen_classes.add(name)
        skel = class_skeleton(src, max_chars=per_class_chars // 2)
        blocks.append(
            f'<class name="{name}" role="supertype-contract">\n{skel}\n'
            f'</class>')

    # Test-subject classes BEFORE collaborators: the failing test's own
    # subjects are the highest-relevance contract source after the patched
    # class itself.
    ranked_subjects = sorted(test_subject_counts.items(),
                             key=lambda kv: -kv[1])
    added = 0
    for name, _n in ranked_subjects:
        if added >= 3:
            break
        if name in seen_classes:
            continue
        path = _find_class_file(buggy_dir, name)
        if not path:
            continue
        try:
            src = open(path, encoding='utf-8', errors='replace').read()
        except OSError:
            continue
        seen_classes.add(name)
        skel = class_skeleton(src, max_chars=per_class_chars // 2)
        blocks.append(
            f'<class name="{name}" role="test-subject">\n{skel}\n</class>')
        added += 1

    ranked = sorted(collab_counts.items(), key=lambda kv: -kv[1])
    added = 0
    for name, _n in ranked:
        if added >= max_collaborators:
            break
        if name in seen_classes:
            continue
        path = _find_class_file(buggy_dir, name)
        if not path:
            continue
        try:
            src = open(path, encoding='utf-8', errors='replace').read()
        except OSError:
            continue
        seen_classes.add(name)
        skel = class_skeleton(src, max_chars=per_class_chars // 2)
        blocks.append(
            f'<class name="{name}" role="collaborator">\n{skel}\n</class>')
        added += 1

    # Enforce the total budget, dropping from the tail (least relevant).
    out, used = [], 0
    for b in blocks:
        if used + len(b) > total_chars and out:
            break
        out.append(b)
        used += len(b)
    return out
