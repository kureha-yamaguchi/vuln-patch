"""Code locations — the one identity every measurement shares.

Three tools name the same Java method three ways:

  fuzz-introspector  "[org.jfree.chart.plot.PiePlot3D].draw(java.awt.Graphics2D,int)"
                     (class part may be short: "[Arc2D.Double].<init>(...)";
                     parameter labels are unreliable in the JVM frontend)
  JaCoCo XML         class="org/jfree/chart/plot/PiePlot3D"  name="draw"
                     desc="(Ljava/awt/Graphics2D;I)V"
  javalang / diffs   class "org.jfree.chart.plot.PiePlot3D", method "draw",
                     parameter type names ["Graphics2D", "int"]

`MethodRef` is the normalised form all three convert into, and `MethodIndex`
matches refs across sources: exact when both sides are fully qualified,
otherwise by (simple class name, method name, arity). Every set the package
builds is a set of these refs (method granularity) or `LineRef`s (line
granularity), optionally tagged with the ring it came from.
"""
from __future__ import annotations

import json
import re
from dataclasses import dataclass, field
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

# ---------------------------------------------------------------------------
# Method identity
# ---------------------------------------------------------------------------

_PRIMS = {'B': 'byte', 'C': 'char', 'D': 'double', 'F': 'float', 'I': 'int',
          'J': 'long', 'S': 'short', 'Z': 'boolean', 'V': 'void'}


def simple_type(name: str) -> str:
    """'java.util.List<String>' -> 'List'; 'int[]' -> 'int[]';
    'org.jfree.Outer$Inner' -> 'Inner'; 'String...' -> 'String[]'."""
    t = name.strip()
    t = re.sub(r'<.*>', '', t)            # drop generics (greedy: nested too)
    t = t.replace('...', '[]')
    dims = ''
    while t.endswith('[]'):
        dims += '[]'
        t = t[:-2]
    t = t.replace('$', '.')
    t = t.rsplit('.', 1)[-1]
    return t + dims


def _split_top_level(s: str) -> List[str]:
    """Split on commas that are not inside <...>."""
    out, depth, cur = [], 0, ''
    for ch in s:
        if ch == '<':
            depth += 1
        elif ch == '>':
            depth -= 1
        if ch == ',' and depth == 0:
            out.append(cur)
            cur = ''
        else:
            cur += ch
    if cur.strip():
        out.append(cur)
    return [p.strip() for p in out if p.strip()]


@dataclass(frozen=True, order=True)
class MethodRef:
    """One Java method or constructor.

    class_fq : dotted class name, nested types joined with '.', '$' never
               appears. May be unqualified when the source only had a
               simple name (introspector does this for JDK types).
    name     : method name; constructors are '<init>', static init '<clinit>'.
    params   : simple, erased parameter type names, in order.
    """
    class_fq: str
    name: str
    params: Tuple[str, ...] = ()

    # -- keys ----------------------------------------------------------
    @property
    def class_simple(self) -> str:
        return self.class_fq.rsplit('.', 1)[-1]

    @property
    def is_qualified(self) -> bool:
        """True when class_fq carries a package (has a lowercase-leading
        segment before the class). 'Arc2D.Double' is NOT qualified."""
        head = self.class_fq.split('.', 1)[0]
        return bool(head) and head[0].islower()

    @property
    def is_synthetic(self) -> bool:
        return '$' in self.name or self.name.startswith('lambda$')

    @property
    def strict_key(self) -> str:
        return f"{self.class_fq}.{self.name}({','.join(self.params)})"

    @property
    def loose_key(self) -> str:
        return f"{self.class_simple}.{self.name}/{len(self.params)}"

    def __str__(self) -> str:
        return self.strict_key

    # -- serialisation -------------------------------------------------
    def to_dict(self) -> dict:
        return {'class_fq': self.class_fq, 'name': self.name,
                'params': list(self.params)}

    @classmethod
    def from_dict(cls, d: dict) -> 'MethodRef':
        return cls(d['class_fq'], d['name'], tuple(d.get('params') or ()))


# -- converters --------------------------------------------------------

_INTROSPECTOR_RE = re.compile(r'^\[(?P<cls>[^\]]+)\]\.(?P<name>[^(]+)\((?P<args>.*)\)$')


def from_introspector(mangled: str) -> Optional[MethodRef]:
    """'[org.jfree.Outer$Inner].draw(java.awt.Graphics2D,int)' ->
    MethodRef('org.jfree.Outer.Inner', 'draw', ('Graphics2D','int'))."""
    m = _INTROSPECTOR_RE.match(mangled.strip())
    if not m:
        return None
    # generic receivers ('[java.util.List<Foo>].add(..)') must not leak the
    # type argument into the class name
    cls = re.sub(r'<.*>', '', m.group('cls')).replace('$', '.')
    params = tuple(simple_type(p) for p in _split_top_level(m.group('args')))
    return MethodRef(cls, m.group('name').strip(), params)


def _decode_desc(desc: str) -> List[str]:
    """JVM method descriptor parameter list -> simple type names.
    '(Ljava/lang/String;[IZ)V' -> ['String', 'int[]', 'boolean']."""
    assert desc.startswith('('), desc
    i, out, dims = 1, [], 0
    while i < len(desc) and desc[i] != ')':
        c = desc[i]
        if c == '[':
            dims += 1
            i += 1
            continue
        if c == 'L':
            j = desc.index(';', i)
            t = desc[i + 1:j].replace('/', '.')
            i = j + 1
        else:
            t = _PRIMS[c]
            i += 1
        out.append(simple_type(t) + '[]' * dims)
        dims = 0
    return out


def from_jacoco(class_name: str, method_name: str, desc: str) -> MethodRef:
    """JaCoCo XML <class name="org/jfree/Outer$Inner"> <method name desc>."""
    cls = class_name.replace('/', '.').replace('$', '.')
    return MethodRef(cls, method_name, tuple(_decode_desc(desc)))


def from_javalang(class_fq: str, name: str,
                  param_types: Sequence[str]) -> MethodRef:
    """AST route (bug_context.analysis / execution.diffcov). Constructors
    must already be named '<init>' or carry the class's simple name."""
    cls = class_fq.replace('$', '.')
    if name == cls.rsplit('.', 1)[-1]:
        name = '<init>'
    return MethodRef(cls, name, tuple(simple_type(p) for p in param_types))


def from_stack_frame(frame: str) -> Optional[Tuple[MethodRef, Optional[int]]]:
    """'at org.jfree.Outer$Inner.draw(Outer.java:123)' -> (ref, 123).
    Params are unknown from a frame, so the ref has params=() and must be
    matched loosely (arity ignored: see MethodIndex.lookup(frame=True))."""
    # JDK 9+ prefixes frames with the module ('java.base/java.lang.String')
    # and some runners with the class loader ('app//org.jfree.Foo'); the
    # part before the slash is not part of the class name.
    m = re.match(r'^\s*at\s+(?:[\w.$@]*/{1,2})?([\w.$]+)\.([\w$<>]+)'
                 r'\(([^:)]*)(?::(\d+))?\)', frame)
    if not m:
        return None
    cls = m.group(1).replace('$', '.')
    line = int(m.group(4)) if m.group(4) else None
    return MethodRef(cls, m.group(2)), line


# ---------------------------------------------------------------------------
# Line identity
# ---------------------------------------------------------------------------

@dataclass(frozen=True, order=True)
class LineRef:
    """One source line, keyed by the TOP-LEVEL class of its file (nested
    classes share the file, so 'org.jfree.Outer' covers 'Outer.Inner').
    This is what JaCoCo's <sourcefile> and a diff hunk both resolve to."""
    class_top_fq: str
    line: int

    @property
    def key(self) -> str:
        return f"{self.class_top_fq}:{self.line}"

    def to_dict(self) -> dict:
        return {'class_top_fq': self.class_top_fq, 'line': self.line}

    @classmethod
    def from_dict(cls, d: dict) -> 'LineRef':
        return cls(d['class_top_fq'], int(d['line']))


_PACKAGE_RE = re.compile(r'^\s*package\s+([\w.]+)\s*;', re.MULTILINE)


def class_top_from_source(rel_path: str, source: str) -> str:
    """'src/org/jfree/Outer.java' + its text -> 'org.jfree.Outer'. Uses the
    file's package declaration; falls back to the path segments."""
    fname = rel_path.replace('\\', '/').rsplit('/', 1)[-1]
    simple = fname[:-5] if fname.endswith('.java') else fname
    m = _PACKAGE_RE.search(source or '')
    if m:
        return f"{m.group(1)}.{simple}"
    parts = rel_path.replace('\\', '/').split('/')
    # last resort: everything after a segment that looks like a package root
    pkg = [p for p in parts[:-1] if p and p[0].islower() and p not in
           ('src', 'main', 'java', 'source', 'test', 'tests')]
    return '.'.join(pkg + [simple]) if pkg else simple


def top_level_of(class_fq: str) -> str:
    """'org.jfree.Outer.Inner' -> 'org.jfree.Outer' (first Uppercase
    segment closes the top-level class)."""
    parts = class_fq.split('.')
    for i, p in enumerate(parts):
        if p and p[0].isupper():
            return '.'.join(parts[:i + 1])
    return class_fq


# ---------------------------------------------------------------------------
# Rings and tagged sets
# ---------------------------------------------------------------------------

SEED, CALLER, CALLEE, OUTSIDE = 'seed', 'caller', 'callee', 'outside'
RINGS = (SEED, CALLER, CALLEE)


@dataclass(frozen=True)
class Tagged:
    """A method with the ring it was reached through. depth is 0 for a
    seed, 1 for a caller, 1..N for a callee at that BFS depth."""
    ref: MethodRef
    ring: str
    depth: int = 0

    def to_dict(self) -> dict:
        d = self.ref.to_dict()
        d.update(ring=self.ring, depth=self.depth)
        return d

    @classmethod
    def from_dict(cls, d: dict) -> 'Tagged':
        return cls(MethodRef.from_dict(d), d['ring'], int(d.get('depth', 0)))


@dataclass
class MethodSet:
    """A set of methods, each carrying at most one ring tag (nearest ring
    wins: seed < caller < callee, then lower depth). `multi` records the
    other rings a method ALSO qualified for, so multi-membership stays
    countable without inflating the set."""
    items: Dict[MethodRef, Tagged] = field(default_factory=dict)
    multi: Dict[MethodRef, List[str]] = field(default_factory=dict)
    edges: List[Tuple[MethodRef, MethodRef]] = field(default_factory=list)
    unmatched: List[str] = field(default_factory=list)   # names no source resolved

    _RANK = {SEED: 0, CALLER: 1, CALLEE: 2}

    def add(self, ref: MethodRef, ring: str = SEED, depth: int = 0) -> None:
        cur = self.items.get(ref)
        if cur is None:
            self.items[ref] = Tagged(ref, ring, depth)
            return
        rank = self._RANK.get(ring, 9)
        if (rank, depth) < (self._RANK.get(cur.ring, 9), cur.depth):
            self.items[ref] = Tagged(ref, ring, depth)
            loser = cur.ring
        else:
            loser = ring
        # `multi` holds only the rings the method ALSO qualified for, never
        # the one it is filed under.
        if loser != self.items[ref].ring:
            other = self.multi.setdefault(ref, [])
            if loser not in other:
                other.append(loser)

    def refs(self, ring: Optional[str] = None) -> List[MethodRef]:
        if ring is None:
            return list(self.items)
        return [r for r, t in self.items.items() if t.ring == ring]

    def ring_of(self, ref: MethodRef) -> str:
        t = self.items.get(ref)
        return t.ring if t else OUTSIDE

    def __len__(self) -> int:
        return len(self.items)

    def __contains__(self, ref: MethodRef) -> bool:
        return ref in self.items

    def to_dict(self) -> dict:
        return {
            'granularity': 'method',
            'items': [t.to_dict() for t in sorted(self.items.values(),
                                                  key=lambda t: t.ref)],
            'multi': {r.strict_key: rings for r, rings in self.multi.items()},
            'edges': [[a.to_dict(), b.to_dict()] for a, b in self.edges],
            'unmatched': list(self.unmatched),
        }

    @classmethod
    def from_dict(cls, d: dict) -> 'MethodSet':
        ms = cls()
        for it in d.get('items', []):
            t = Tagged.from_dict(it)
            ms.items[t.ref] = t
        keyed = {r.strict_key: r for r in ms.items}
        for k, rings in (d.get('multi') or {}).items():
            if k in keyed:
                ms.multi[keyed[k]] = list(rings)
        ms.edges = [(MethodRef.from_dict(a), MethodRef.from_dict(b))
                    for a, b in d.get('edges', [])]
        ms.unmatched = list(d.get('unmatched', []))
        return ms


@dataclass
class LineSet:
    """Lines, each tagged with the ring of the method that owns it (or
    SEED for lines a diff changed directly)."""
    items: Dict[LineRef, str] = field(default_factory=dict)

    def add(self, ref: LineRef, ring: str = SEED) -> None:
        cur = self.items.get(ref)
        if cur is None or MethodSet._RANK.get(ring, 9) < MethodSet._RANK.get(cur, 9):
            self.items[ref] = ring

    def refs(self, ring: Optional[str] = None) -> List[LineRef]:
        if ring is None:
            return list(self.items)
        return [r for r, g in self.items.items() if g == ring]

    def ring_of(self, ref: LineRef) -> str:
        return self.items.get(ref, OUTSIDE)

    def __len__(self) -> int:
        return len(self.items)

    def __contains__(self, ref: LineRef) -> bool:
        return ref in self.items

    def to_dict(self) -> dict:
        return {'granularity': 'line',
                'items': [dict(r.to_dict(), ring=g)
                          for r, g in sorted(self.items.items())]}

    @classmethod
    def from_dict(cls, d: dict) -> 'LineSet':
        ls = cls()
        for it in d.get('items', []):
            ls.items[LineRef.from_dict(it)] = it.get('ring', SEED)
        return ls


# ---------------------------------------------------------------------------
# Cross-source matching
# ---------------------------------------------------------------------------

class MethodIndex:
    """Match refs from one source against a reference population from
    another (typically JaCoCo's fully-qualified method list).

    lookup(ref):
      1. exact strict key;
      2. if `ref` is unqualified or its params look unreliable, the unique
         (simple class, name, arity) match;
      3. with frame=True (stack frames carry no params), the unique
         (simple class, name) match regardless of arity.
    Returns None when nothing or more than one candidate matches; the
    ambiguity is counted so a caller can report it."""

    def __init__(self, population: Iterable[MethodRef]):
        self.strict: Dict[str, MethodRef] = {}
        self.loose: Dict[str, List[MethodRef]] = {}
        self.by_name: Dict[str, List[MethodRef]] = {}
        for r in population:
            self.strict[r.strict_key] = r
            self.loose.setdefault(r.loose_key, []).append(r)
            self.by_name.setdefault(f"{r.class_simple}.{r.name}", []).append(r)
        self.ambiguous = 0
        self.missing = 0

    def lookup(self, ref: MethodRef, frame: bool = False) -> Optional[MethodRef]:
        hit = self.strict.get(ref.strict_key)
        if hit is not None:
            return hit
        cands = self.by_name.get(f"{ref.class_simple}.{ref.name}", []) if frame \
            else self.loose.get(ref.loose_key, [])
        if ref.is_qualified:
            # a qualified ref must at least agree on the package
            cands = [c for c in cands if not c.is_qualified
                     or c.class_fq == ref.class_fq]
        if len(cands) == 1:
            return cands[0]
        if not cands:
            self.missing += 1
        else:
            self.ambiguous += 1
        return None


# ---------------------------------------------------------------------------
# File helpers
# ---------------------------------------------------------------------------

def dump(obj, path: str) -> None:
    with open(path, 'w') as fh:
        json.dump(obj.to_dict() if hasattr(obj, 'to_dict') else obj, fh, indent=1)


def load_method_set(path: str) -> MethodSet:
    with open(path) as fh:
        return MethodSet.from_dict(json.load(fh))


def load_line_set(path: str) -> LineSet:
    with open(path) as fh:
        return LineSet.from_dict(json.load(fh))
