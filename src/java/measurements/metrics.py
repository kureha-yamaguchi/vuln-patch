"""The five Table 2 metrics for one leg of a run.

A "leg" is one (bug, patch, arm) run directory, e.g.
``<run_dir>/07_patch1-Chart-8-CapGen_c/``.  This module reads ONLY the JSON
files that `cli.py` has already written into ``<leg>/measurements/`` — it
never runs a checkout, a build, or a coverage collection — so every number
here can be reproduced (and tested) from files on disk.

The four sets the metrics are built from
----------------------------------------
P  patch-derived set.  What the harness generator could learn about the
   fix from the material it was given (its context / its trace).
   File: ``patch_derived.json`` (methods), ``patch_derived_lines.json``.
R  root-cause region.  Where the bug actually lives, taken from the
   developer fix plus its caller/callee neighbourhood.  Every element
   carries a ring: ``seed`` (a method the developer changed), ``caller``,
   ``callee``.  File: ``root_cause.json``.
F  fuzzer-reachable set.  What the generated harness actually executed,
   measured on one build.  File: ``coverage_buggy.json`` /
   ``coverage_patched.json`` / ``coverage_compiled.json`` (the last one is
   the ALL-COMPILED harness set, not the kept one; see `BUILDS`).
C  crash set.  Where the campaign's crashes landed.
   File: ``crash_sites.json``.

The metrics (each is a plain ratio; the plain-English reading follows)
---------------------------------------------------------------------
RCR = |R n P| / |R|   how much of the real root cause the harness generator
                      was even told about.
RCC = |R n F| / |R|   how much of the real root cause the harness actually
                      ran.
RCP = |R n F| / |F|   how much of what the harness ran was root cause, i.e.
                      how well the fuzzing budget was spent.
PSC = |P n F| / |P|   how much of what the generator was told about it
                      managed to reach.
CSM = |{c in C : site(c) in R}| / |C|   how often a crash landed inside the
                      root cause.  Only crashes whose deepest frame is in
                      library code count; ``harness_only`` crashes are
                      reported separately, not in the denominator.

Three axes multiply every metric
--------------------------------
granularity  ``method`` or ``line``.
R-variant    ``R0``   = the seed ring alone (the methods the developer
                        actually changed);
             ``R1``   = R0 plus the manifest (the frames of the failing
                        trigger test) — METHOD GRANULARITY ONLY, because a
                        manifest is a set of stack frames stored as methods
                        and carries no line information;
             ``full`` = the whole ringed region (seed + caller + callee).
build        which coverage the F-side came from: ``buggy``, ``patched``
             or ``compiled``.  The first two are the KEPT harness set on
             the two builds; ``compiled`` is every compiled candidate on
             the buggy build (see `BUILDS`).
             RCR and CSM do not read coverage, so their build slot is
             ``na``; PSC has no R-side, so its R-variant slot is ``na``.
F kind       how F(H) was obtained — ``dyn`` (run-time coverage, the only
             one produced today) or ``stat`` (static call-graph
             reachability, planned).  Only the three metrics that read F
             (RCC, RCP, PSC) carry this slot, and they always carry it, so
             a dynamic number can never share a column with a static one.
             See `F_KINDS` and `metric_key`.

Every ratio is reported as ``{'value', 'num', 'den'}`` with ``value`` None
when the denominator is 0, and carries a ``by_ring`` sub-dict:

* RCR / RCC / PSC — one ratio per ring of the set in the denominator, so
  the rings' denominators add up to the aggregate denominator.
* RCP / CSM — a decomposition of the SINGLE denominator (|F|, |C|) across
  the ring of R that each element fell in, plus ``outside`` for the
  elements that fell outside R.  Those four values sum to exactly 1.

RCR additionally gets a cross-table, ``rcr_cross__<gran>__<rvar>__na``:
counts of the recovered elements by (ring in R, ring in P).  It is counts,
not ratios, so it is not averaged by `aggregate`.

Matching methods across tools
-----------------------------
P, R and F are named by three different tools, so the sets are intersected
through `locations.MethodIndex` rather than by raw equality: when coverage
is present its ``all_methods`` list is the identity space and both P and R
are mapped into it; without coverage the index is built over R and P is
mapped into it.  Refs that resolve to nothing, or to more than one
candidate, are counted (``matching`` in the output) instead of being
silently dropped.
"""
from __future__ import annotations

import json
import os
import re
from typing import Dict, Iterable, List, Optional, Sequence, Set, Tuple

from . import locations as loc

# --- file names this module reads ------------------------------------------
F_PATCH_DERIVED = 'patch_derived.json'
F_PATCH_DERIVED_LINES = 'patch_derived_lines.json'
F_ROOT_CAUSE = 'root_cause.json'
F_CRASH_SITES = 'crash_sites.json'
F_COVERAGE = 'coverage_{build}.json'
F_STATIC = 'static_{set}.json'
MEASUREMENTS_DIR = 'measurements'
RESULT_FILE = 'result.jsonl'
METRICS_FILE = 'metrics.jsonl'

#: The harness-set/build combinations a leg can carry coverage for.
#: ``buggy`` and ``patched`` are the KEPT harnesses (the ones the acceptance
#: gate admitted) on the two builds; ``compiled`` is EVERY compiled
#: candidate, kept or rejected, on the buggy build — the acceptance gate
#: runs each one once, and that run is the only chance to see what the
#: rejected ones reached.  Reading only the kept set would let the gate,
#: not the prompt, decide RCC; see the README, "Kept versus all compiled
#: harnesses".  ``buggy`` stays first: it is the primary build.
BUILD_COMPILED = 'compiled'
BUILDS = ('buggy', 'patched', BUILD_COMPILED)

#: The harness sets the STATIC reachable set F_stat is computed for, and
#: the build slot each one's metric keys use.  ``kept`` is the harnesses the
#: acceptance gate admitted and ``compiled`` is every candidate that
#: compiled — the same two sets `BUILDS` covers dynamically.
#:
#: Static reach is read off the source, so it is the SAME on the buggy and
#: the patched build: the build slot carries no information for a ``stat``
#: key.  ``buggy`` is used for the kept set by convention, because ``buggy``
#: is the primary build in every dynamic key, and it keeps the kept set's
#: static and dynamic numbers in the same column of a table.
STATIC_SETS = ('kept', BUILD_COMPILED)
STATIC_BUILD_SLOT = {'kept': 'buggy', BUILD_COMPILED: BUILD_COMPILED}

GRANULARITIES = ('method', 'line')
R_VARIANTS = ('R0', 'R1', 'full')
R_VARIANTS_LINE = ('R0', 'full')      # R1 has no line-level counterpart
RING_ORDER = (loc.SEED, loc.CALLER, loc.CALLEE)
RING_ORDER_OUT = RING_ORDER + (loc.OUTSIDE,)

#: How the fuzzer-reachable set F(H) was obtained.  Two kinds exist, and a
#: metric computed from one must never be averaged together with the same
#: metric computed from the other, so the kind is written into the key of
#: every metric that reads F:
#:
#: ``dyn``   dynamic.  What the harnesses ACTUALLY executed, observed at run
#:           time from the JaCoCo coverage a ``--coverage`` run leaves
#:           behind.  This is the only kind produced today.
#: ``stat``  static.  What the harnesses COULD reach: the callees reachable
#:           on the static call graph from the library methods the harness
#:           source calls, computed by `java.measurements.static_reach` and
#:           read here out of ``static_kept.json`` / ``static_compiled.json``.
#:           METHOD GRANULARITY ONLY — a call graph names methods, not
#:           lines — so no ``stat`` key is ever emitted at line granularity.
F_KINDS = ('dyn', 'stat')

#: The kind a key carries when the caller does not say, and the kind the
#: rendered tables default to.
DEFAULT_F_KIND = 'dyn'
F_KIND_STATIC = 'stat'

#: The metrics whose value depends on F(H), so their keys carry the F kind.
#: ``rcr``, ``csm`` and ``rcr_cross`` never read F and keep 4-slot keys.
F_METRICS = ('rcc', 'rcp', 'psc')

LEG_RE = re.compile(r'^(?P<index>\d+)_(?P<patch>.+)_(?P<arm>[oc])$')


# ---------------------------------------------------------------------------
# small helpers
# ---------------------------------------------------------------------------

def metric_key(metric: str, gran: str, rvar: Optional[str],
               build: Optional[str], fkind: Optional[str] = None) -> str:
    """The flat field name a metric lands under.

    Metrics that do not read the fuzzer-reachable set F(H) — ``rcr``,
    ``csm``, ``rcr_cross`` — have four slots,
    ``<metric>__<granularity>__<R-variant>__<build>``, with the literal
    ``na`` in a slot the metric does not use, e.g. ``rcr__method__R0__na``,
    ``csm__line__full__na``.

    Metrics that do read F — ``rcc``, ``rcp``, ``psc`` — add a fifth slot
    naming the KIND of F (see `F_KINDS`), so a dynamic number can never be
    put in the same column as a static one:
    ``rcc__method__R0__buggy__dyn``, ``rcp__line__full__patched__dyn``,
    ``psc__method__na__buggy__dyn``.

    For an F-using metric `fkind` defaults to `DEFAULT_F_KIND` and must be
    one of `F_KINDS`; passing one for a metric that does not read F is an
    error, because such a key would claim a dependence that is not there."""
    base = f"{metric}__{gran}__{rvar or 'na'}__{build or 'na'}"
    if metric not in F_METRICS:
        if fkind is not None:
            raise ValueError(
                f"metric {metric!r} does not use F(H), so it takes no F kind "
                f"(got {fkind!r}); F-using metrics are {F_METRICS}")
        return base
    kind = DEFAULT_F_KIND if fkind is None else fkind
    if kind not in F_KINDS:
        raise ValueError(f"unknown F kind {kind!r}; expected one of {F_KINDS}")
    return f"{base}__{kind}"


def _ratio(num: int, den: int) -> dict:
    """One reported number: the ratio plus the two counts it came from.
    ``value`` is None when the denominator is 0 (nothing to measure)."""
    return {'value': (num / den) if den else None, 'num': int(num),
            'den': int(den)}


def _read_json(path: str):
    if not os.path.isfile(path):
        return None
    try:
        with open(path) as fh:
            return json.load(fh)
    except (OSError, ValueError):
        return None


def read_result(leg_dir: str) -> dict:
    """First JSON line of ``<leg>/result.jsonl`` ({} when missing/unreadable)."""
    path = os.path.join(leg_dir, RESULT_FILE)
    if not os.path.isfile(path):
        return {}
    try:
        with open(path) as fh:
            for line in fh:
                line = line.strip()
                if line:
                    return json.loads(line)
    except (OSError, ValueError):
        return {}
    return {}


def leg_dirs(run_dir: str) -> List[str]:
    """Every leg directory of a run, in name order (deterministic).

    A directory counts as a leg when its name looks like
    ``NN_<patch>_<o|c>`` or when it simply contains a ``result.jsonl``."""
    out = []
    for name in sorted(os.listdir(run_dir)):
        path = os.path.join(run_dir, name)
        if not os.path.isdir(path):
            continue
        if LEG_RE.match(name) or os.path.isfile(os.path.join(path, RESULT_FILE)):
            out.append(path)
    return out


# ---------------------------------------------------------------------------
# loading the measured objects out of their JSON files
# ---------------------------------------------------------------------------

def _load_method_set(path: str) -> Optional[loc.MethodSet]:
    d = _read_json(path)
    return loc.MethodSet.from_dict(d) if isinstance(d, dict) else None


def _load_line_set(path: str) -> Optional[loc.LineSet]:
    d = _read_json(path)
    return loc.LineSet.from_dict(d) if isinstance(d, dict) else None


def _root_cause_parts(path: str):
    """``root_cause.json`` -> (methods MethodSet, lines LineSet, manifest
    MethodSet).  Any part that is absent comes back None; the file is read
    as plain JSON so this module never imports `root_cause`."""
    d = _read_json(path)
    if not isinstance(d, dict):
        return None, None, None
    methods = loc.MethodSet.from_dict(d['methods']) \
        if isinstance(d.get('methods'), dict) else None
    lines = loc.LineSet.from_dict(d['lines']) \
        if isinstance(d.get('lines'), dict) else None
    manifest = loc.MethodSet.from_dict(d['manifest']) \
        if isinstance(d.get('manifest'), dict) else None
    return methods, lines, manifest


class _Coverage:
    """The parts of a `coverage.Coverage` dict the metrics need.

    Shape read (all keys optional except the two set lists):
    ``{'build': 'buggy', 'methods': [MethodRef dicts],
       'lines': [LineRef dicts], 'all_methods': [MethodRef dicts],
       'branches_covered': int, 'branches_total': int}``"""

    def __init__(self, d: dict, build: str):
        self.build = d.get('build') or build
        self.methods: Set[loc.MethodRef] = {
            loc.MethodRef.from_dict(x) for x in (d.get('methods') or [])}
        self.lines: Set[loc.LineRef] = {
            loc.LineRef.from_dict(x) for x in (d.get('lines') or [])}
        self.all_methods: Set[loc.MethodRef] = {
            loc.MethodRef.from_dict(x) for x in (d.get('all_methods') or [])}
        self.branches_covered = int(d.get('branches_covered') or 0)
        self.branches_total = int(d.get('branches_total') or 0)


def _load_coverage(mdir: str) -> Dict[str, _Coverage]:
    out = {}
    for build in BUILDS:
        d = _read_json(os.path.join(mdir, F_COVERAGE.format(build=build)))
        if isinstance(d, dict):
            out[build] = _Coverage(d, build)
    return out


class _Static:
    """The parts of a `static_reach.StaticReach` dict the metrics need.

    Shape read: ``{'methods': [MethodRef dicts], 'entries': [MethodRef
    dicts], 'harnesses': [str], 'unmatched': [str], 'source': str}``.
    ``methods`` is F_stat itself (the entries plus everything the bounded
    call-graph walk reached from them); ``entries`` is the smaller claim,
    the methods the harness sources call directly."""

    def __init__(self, d: dict, set_name: str):
        self.set_name = d.get('set') or set_name
        self.source = d.get('source') or ''
        self.methods: Set[loc.MethodRef] = {
            loc.MethodRef.from_dict(x) for x in (d.get('methods') or [])}
        self.entries: Set[loc.MethodRef] = {
            loc.MethodRef.from_dict(x) for x in (d.get('entries') or [])}
        self.harnesses: List[str] = list(d.get('harnesses') or [])
        self.unmatched: List[str] = list(d.get('unmatched') or [])


def _load_static(mdir: str) -> Dict[str, _Static]:
    """``static_kept.json`` / ``static_compiled.json``, keyed by harness
    set.  Absent files are simply not in the result, exactly as for
    coverage, so a run made before F_stat existed emits no ``stat`` key."""
    out = {}
    for set_name in STATIC_SETS:
        d = _read_json(os.path.join(mdir, F_STATIC.format(set=set_name)))
        if isinstance(d, dict):
            out[set_name] = _Static(d, set_name)
    return out


class _CrashSite:
    """The parts of a `crash_sites.CrashSite` dict the metrics need.

    Shape read: ``{'build': 'buggy', 'exception': str,
    'site_kind': 'library'|'harness_only',
    'top_library': MethodRef dict or null,
    'top_library_line': LineRef dict, or a bare int line number, or null}``
    A bare int is paired with the top-level class of ``top_library``."""

    def __init__(self, d: dict):
        self.build = d.get('build')
        self.exception = d.get('exception')
        self.site_kind = d.get('site_kind') or (
            'library' if d.get('top_library') else 'harness_only')
        tl = d.get('top_library')
        self.method: Optional[loc.MethodRef] = \
            loc.MethodRef.from_dict(tl) if isinstance(tl, dict) else None
        line = d.get('top_library_line')
        self.line: Optional[loc.LineRef] = None
        if isinstance(line, dict):
            self.line = loc.LineRef.from_dict(line)
        elif isinstance(line, int) and self.method is not None:
            self.line = loc.LineRef(loc.top_level_of(self.method.class_fq),
                                    line)


def _load_crash_sites(mdir: str) -> Optional[List[_CrashSite]]:
    d = _read_json(os.path.join(mdir, F_CRASH_SITES))
    if d is None:
        return None
    if isinstance(d, dict):                       # tolerate {'sites': [...]}
        d = d.get('sites') or []
    return [_CrashSite(x) for x in d if isinstance(x, dict)]


# ---------------------------------------------------------------------------
# the R-variants
# ---------------------------------------------------------------------------

def _r0(methods: loc.MethodSet) -> loc.MethodSet:
    """R0 — the seed ring alone: the methods the developer's fix changed."""
    ms = loc.MethodSet()
    for ref in methods.refs(loc.SEED):
        ms.add(ref, loc.SEED, 0)
    return ms


def _r1(methods: loc.MethodSet,
        manifest: Optional[loc.MethodSet]) -> loc.MethodSet:
    """R1 — R0 widened with the manifest (the failing trigger test's stack
    frames).  Ring tags come from whichever side claims a method first;
    `MethodSet.add` keeps the nearest ring, so a seed stays a seed."""
    ms = _r0(methods)
    for ref in (manifest.refs() if manifest else []):
        t = manifest.items[ref]
        ms.add(ref, t.ring, t.depth)
    return ms


def _r_variants(methods: Optional[loc.MethodSet],
                manifest: Optional[loc.MethodSet]) -> Dict[str, loc.MethodSet]:
    if methods is None:
        return {}
    return {'R0': _r0(methods), 'R1': _r1(methods, manifest), 'full': methods}


def _r_variants_line(lines: Optional[loc.LineSet]) -> Dict[str, loc.LineSet]:
    """Line-granularity R comes straight out of ``root_cause.lines``: R0 is
    the seed-ring lines, ``full`` is all of them.  There is no R1 — see the
    module docstring."""
    if lines is None:
        return {}
    r0 = loc.LineSet()
    for ref in lines.refs(loc.SEED):
        r0.add(ref, loc.SEED)
    return {'R0': r0, 'full': lines}


# ---------------------------------------------------------------------------
# cross-tool method matching
# ---------------------------------------------------------------------------

class _Projection:
    """P and R mapped into one identity space, plus the bookkeeping.

    ``r`` / ``p`` are the canonical ref sets; ``r_ring`` gives the ring of
    each canonical R member (nearest ring wins when two R refs collapse
    onto one canonical ref); ``stats`` counts what could not be mapped."""

    def __init__(self, r: Set[loc.MethodRef], r_ring: Dict[loc.MethodRef, str],
                 p: Set[loc.MethodRef], p_ring: Dict[loc.MethodRef, str],
                 stats: dict, index: loc.MethodIndex):
        self.r, self.r_ring = r, r_ring
        self.p, self.p_ring = p, p_ring
        self.stats = stats
        self.index = index


_RANK = getattr(loc.MethodSet, '_RANK',
                {loc.SEED: 0, loc.CALLER: 1, loc.CALLEE: 2})


def _rank(ring: str) -> int:
    """Ring nearness: seed 0 < caller 1 < callee 2, anything else last."""
    return _RANK.get(ring, 9)


def _map_refs(refs: Iterable[loc.MethodRef],
              index: loc.MethodIndex) -> Set[loc.MethodRef]:
    """A plain ref set mapped into `index`'s identity space.

    A ref the index cannot resolve is KEPT AS ITSELF rather than dropped:
    this set is a denominator (|F_stat| in RCP), and silently losing its
    unresolvable members would make the fuzzing budget look smaller than it
    was.  Two refs that resolve to one canonical method collapse into one,
    which is the same thing `_map_set` does for P and R."""
    out: Set[loc.MethodRef] = set()
    for ref in refs:
        out.add(index.lookup(ref) or ref)
    return out


def _map_set(ms: loc.MethodSet, index: loc.MethodIndex, identity: bool):
    canon: Dict[loc.MethodRef, str] = {}
    unmatched = 0
    for ref in ms.refs():
        hit = ref if identity else index.lookup(ref)
        if hit is None:
            unmatched += 1
            continue
        ring = ms.ring_of(ref)
        if hit not in canon or _rank(ring) < _rank(canon[hit]):
            canon[hit] = ring
    return canon, unmatched


def _project(rset: loc.MethodSet, pset: Optional[loc.MethodSet],
             population: Optional[Iterable[loc.MethodRef]]) -> _Projection:
    """Put R and P in one space.

    With a ``population`` (a build's ``all_methods``) both sides are looked
    up in it.  Without one the population IS R, so R maps to itself and only
    P is looked up — the fallback the module docstring describes."""
    if population is None:
        index = loc.MethodIndex(rset.refs())
        r_canon, r_missed = _map_set(rset, index, identity=True)
    else:
        index = loc.MethodIndex(population)
        r_canon, r_missed = _map_set(rset, index, identity=False)
    if pset is None:
        p_canon, p_missed = {}, 0
    else:
        p_canon, p_missed = _map_set(pset, index, identity=False)
    stats = {
        'space': 'coverage_all_methods' if population is not None else 'R',
        'r_unmatched': r_missed,
        'p_unmatched': p_missed,
        'ambiguous': index.ambiguous,
        'missing': index.missing,
        # Refs whose labelled class had no such method at all and that the
        # index resolved by the unique (name, arity) match instead
        # (MethodIndex.lookup rule 4).
        'reclassified': index.reclassified,
        'r_caller_provenance': _caller_provenance(rset),
        'p_caller_provenance': _caller_provenance(pset),
    }
    return _Projection(set(r_canon), r_canon, set(p_canon), p_canon,
                       stats, index)


def _site_in(index: loc.MethodIndex, ref: Optional[loc.MethodRef],
             rset: Optional[loc.MethodSet] = None):
    """Resolve a crash frame against an index.  Frames carry no parameter
    types, so a ref with no params is matched with ``frame=True`` (arity
    ignored); anything else goes through the normal strict/loose path.

    A frame inside an OVERLOADED method (two `replaceEach` arities both in
    the set) is ambiguous by name alone. The site is then still inside the
    set — the question CSM asks — so among the same-name candidates the
    one in the nearest ring is taken rather than reporting 'outside'."""
    if ref is None:
        return None
    hit = index.lookup(ref, frame=not ref.params)
    if hit is not None or ref.params or rset is None:
        return hit
    cands = index.by_name.get(f"{ref.class_simple}.{ref.name}", [])
    if ref.is_qualified:
        cands = [c for c in cands
                 if not c.is_qualified or c.class_fq == ref.class_fq]
    cands = [c for c in cands if c in rset]
    if not cands:
        return None
    return min(cands, key=lambda c: (_rank(rset.ring_of(c)), c.strict_key))


# ---------------------------------------------------------------------------
# the metric computations
# ---------------------------------------------------------------------------

def _by_ring_ratio(members: Set, ring_of, other: Set,
                   rings: Sequence[str] = RING_ORDER) -> dict:
    """One ratio per ring of the DENOMINATOR set: for each ring, how much
    of that ring is also in `other`.  Used by RCR, RCC and PSC."""
    out = {}
    for ring in rings:
        part = {m for m in members if ring_of(m) == ring}
        out[ring] = _ratio(len(part & other), len(part))
    return out


def _decompose(members: Set, ring_of, den: int) -> dict:
    """Split ONE denominator across the ring each of its elements fell in,
    ``outside`` for the ones that fell outside R.  Used by RCP and CSM: the
    four values sum to exactly 1 when the denominator is non-zero."""
    out = {}
    for ring in RING_ORDER_OUT:
        out[ring] = _ratio(sum(1 for m in members if ring_of(m) == ring), den)
    return out


def _cross(members, ring_a, ring_b) -> dict:
    """Counts of the elements of `members` by (ring in R, ring in P).

    The RCR cross-table: it shows WHERE a recovered method sits on each
    side — e.g. developer seeds that land in our callee ring mean the
    repair tool patched upstream of where the developer fixed."""
    out = {a: {b: 0 for b in RING_ORDER} for a in RING_ORDER}
    for m in members:
        a, b = ring_a(m), ring_b(m)
        if a in out and b in out[a]:
            out[a][b] += 1
    return out


def _line_ring(lset: loc.LineSet):
    return lambda ref: lset.ring_of(ref)


# ---------------------------------------------------------------------------
# per-leg entry point
# ---------------------------------------------------------------------------

# Methods the introspector lists as callees but that belong to the JDK, not
# to the library under test ('String.charAt', 'StringBuilder.append',
# 'IllegalArgumentException.<init>'). They are never in the developer's
# region and never instrumented for coverage, so leaving them in P or R only
# inflates denominators. Qualified JDK packages are recognised by prefix;
# unqualified names by a short list of ubiquitous JDK classes (the
# introspector prints JDK receivers without a package).
_JDK_PREFIXES = ('java.', 'javax.', 'jdk.', 'sun.', 'com.sun.')
_JDK_SIMPLE = {
    'String', 'StringBuilder', 'StringBuffer', 'Object', 'Math', 'System',
    'Integer', 'Long', 'Double', 'Float', 'Short', 'Byte', 'Boolean',
    'Character', 'Number', 'Arrays', 'Collections', 'List', 'ArrayList',
    'LinkedList', 'Map', 'HashMap', 'TreeMap', 'LinkedHashMap', 'Set',
    'HashSet', 'TreeSet', 'Iterator', 'Collection', 'Objects', 'Class',
    'Thread', 'Throwable', 'Exception', 'RuntimeException', 'Error',
    'IllegalArgumentException', 'IllegalStateException',
    'NullPointerException', 'IndexOutOfBoundsException',
    'ArrayIndexOutOfBoundsException', 'StringIndexOutOfBoundsException',
    'UnsupportedOperationException', 'ArithmeticException',
    'NumberFormatException', 'ClassCastException', 'Locale', 'Calendar',
    'Date', 'TimeZone', 'BigDecimal', 'BigInteger', 'Enum', 'Comparable',
    'CharSequence', 'Iterable', 'Optional', 'Random', 'Pattern', 'Matcher',
}


def _is_jdk(ref: loc.MethodRef) -> bool:
    if ref.class_fq.startswith(_JDK_PREFIXES):
        return True
    return (not ref.is_qualified) and ref.class_fq.split('.')[0] in _JDK_SIMPLE


#: Public name for the same test.  `static_reach` drops calls to the JDK
#: before it tries to resolve them against the project's method list, and
#: the two must agree on what "the JDK" is, so there is one list.
is_jdk = _is_jdk


#: JDK methods the introspector labels with the SEED's class instead of the
#: receiver's, so `_is_jdk` cannot see them: a call to
#: `Rectangle2D.getCenterX()` inside `Axis.drawLabel` is written
#: `[org.jfree.chart.axis.Axis].getCenterX()`, which looks like a project
#: method of a project class.  These are the java.awt.geom / java.lang
#: accessor names that mislabelling actually produced on real runs.  A name
#: here is dropped ONLY when the class it is labelled with declares no
#: method of that name in the coverage population, so a project class that
#: really does have a `getWidth()` keeps it.
_MISLABELLED_NAMES = {
    'getCenterX', 'getCenterY', 'getBounds2D', 'getWidth', 'getHeight',
    'getX', 'getY', 'createTransformedShape',
    'getMinX', 'getMaxX', 'getMinY', 'getMaxY',
}


def _strip_jdk(ms: Optional[loc.MethodSet]):
    """(copy of `ms` without JDK methods, number dropped). Edges whose
    endpoint was dropped go with it; `unmatched`, `multi` and `provenance`
    are kept."""
    return _strip(ms, _is_jdk)


def _strip_mislabelled(ms: Optional[loc.MethodSet], population):
    """(copy of `ms` without mislabelled-receiver JDK methods, number).

    The second pass over `_strip_jdk`'s output, and the one that needs the
    coverage population: a ref survives when the class it is labelled with
    declares a method of that name somewhere in the population.  With no
    population nothing is dropped — the check cannot be made, and guessing
    would remove real project methods."""
    if ms is None:
        return None, 0
    pop = list(population or [])
    if not pop:
        return ms, 0
    declared = {f'{r.class_simple}.{r.name}' for r in pop}

    def drop(ref: loc.MethodRef) -> bool:
        return (ref.name in _MISLABELLED_NAMES
                and f'{ref.class_simple}.{ref.name}' not in declared)
    return _strip(ms, drop)


def _strip(ms: Optional[loc.MethodSet], drop):
    """(copy of `ms` without the members `drop` says to remove, number)."""
    if ms is None:
        return None, 0
    keep = loc.MethodSet()
    dropped = 0
    for ref, tag in ms.items.items():
        if drop(ref):
            dropped += 1
            continue
        keep.items[ref] = tag
    keep.multi = {r: v for r, v in ms.multi.items() if r in keep.items}
    keep.edges = [(a, b) for a, b in ms.edges
                  if a in keep.items and b in keep.items]
    keep.unmatched = list(ms.unmatched)
    keep.provenance = {r: v for r, v in ms.provenance.items()
                       if r in keep.items}
    return keep, dropped


def _caller_provenance(ms: Optional[loc.MethodSet]) -> Optional[dict]:
    """How many members of the CALLER ring each way of finding a caller
    produced.  `introspector` is the static call graph; `source-scan` is
    the source-text fallback `neighbourhood.SourceScan` runs for a seed the
    graph found no caller for.  A set written before provenance existed
    counts entirely as `introspector` (see `MethodSet.provenance_of`)."""
    if ms is None:
        return None
    out = {'introspector': 0, 'source-scan': 0}
    for ref in ms.refs(loc.CALLER):
        prov = ms.provenance_of(ref)
        out[prov] = out.get(prov, 0) + 1
    return out


def _f_kinds(covs: dict, statics: dict) -> Dict[str, List[str]]:
    """Per build slot, which KINDS of F(H) that slot carries.

    ``dyn`` comes from a build's JaCoCo coverage; ``stat`` from a harness
    set's static reach, filed under the build slot `STATIC_BUILD_SLOT`
    gives it.  A slot with both is a slot where the same harness set can be
    read two ways, which is the comparison `R_stat_only` / `R_dyn_only`
    below reports."""
    out: Dict[str, List[str]] = {}
    for build in covs:
        out.setdefault(build, []).append(DEFAULT_F_KIND)
    for set_name in statics:
        slot = STATIC_BUILD_SLOT.get(set_name, set_name)
        out.setdefault(slot, []).append(F_KIND_STATIC)
    return {b: sorted(set(k)) for b, k in out.items()}


def compute_leg(leg_dir: str) -> dict:
    """All Table 2 numbers for one leg, as a flat JSON-serialisable dict.

    Reads only ``<leg>/result.jsonl`` and ``<leg>/measurements/*.json``.
    Metrics whose inputs are missing are simply not emitted; the
    ``available`` sub-dict says which inputs were there."""
    leg_dir = os.path.abspath(leg_dir)
    mdir = os.path.join(leg_dir, MEASUREMENTS_DIR)
    result = read_result(leg_dir)
    name = os.path.basename(leg_dir)
    m = LEG_RE.match(name)

    out: dict = {
        'leg': name,
        'leg_dir': leg_dir,
        'run': os.path.basename(os.path.dirname(leg_dir)),
        'leg_index': int(m.group('index')) if m else None,
        'patch_name': m.group('patch') if m else None,
        'arm': m.group('arm') if m else None,
        'label': result.get('label'),
        'status': result.get('status'),
        'bug_kind': result.get('bug_kind'),
        'project': result.get('project'),
        'bug_id': (str(result.get('bug_id'))
                   if result.get('bug_id') is not None else None),
        'apr_tool': result.get('apr_tool'),
        'crashed_on_patch': bool(result.get('crashed_on_patch')),
    }
    out['bug'] = (f"{out['project']}-{out['bug_id']}"
                  if out['project'] and out['bug_id'] else None)
    # 'caught' is the classifier's positive signal on an overfitting patch:
    # some harness still crashed the patched build (run.py's crashed_on_patch).
    out['caught'] = bool(out['label'] == 'overfitting'
                         and out['crashed_on_patch'])
    out['outcome'] = {
        ('overfitting', True): 'caught', ('overfitting', False): 'missed',
        ('correct', True): 'false_alarm', ('correct', False): 'clean',
    }.get((out['label'], out['crashed_on_patch']))

    # -- inputs ------------------------------------------------------------
    p_methods = _load_method_set(os.path.join(mdir, F_PATCH_DERIVED))
    p_lines = _load_line_set(os.path.join(mdir, F_PATCH_DERIVED_LINES))
    r_methods, r_lines, manifest = _root_cause_parts(
        os.path.join(mdir, F_ROOT_CAUSE))
    p_methods, p_jdk = _strip_jdk(p_methods)
    r_methods, r_jdk = _strip_jdk(r_methods)
    manifest, _ = _strip_jdk(manifest)
    covs = _load_coverage(mdir)
    statics = _load_static(mdir)
    sites = _load_crash_sites(mdir)

    # The primary build's method list is the identity space for everything
    # below, and the population the mislabelled-receiver pass needs: a JDK
    # accessor wearing a project class's name can only be recognised by
    # asking whether that class has such a method at all.
    primary = next((b for b in BUILDS if b in covs), None)
    primary_pop = (covs[primary].all_methods or None) if primary else None
    p_methods, p_mis = _strip_mislabelled(p_methods, primary_pop)
    r_methods, r_mis = _strip_mislabelled(r_methods, primary_pop)
    manifest, _ = _strip_mislabelled(manifest, primary_pop)
    out['jdk_dropped'] = {'P_method': p_jdk, 'R_method': r_jdk,
                          'P_mislabelled': p_mis, 'R_mislabelled': r_mis}

    out['available'] = {
        'patch_derived': p_methods is not None,
        'patch_derived_lines': p_lines is not None,
        'root_cause': r_methods is not None or r_lines is not None,
        'root_cause_manifest': manifest is not None,
        'coverage_buggy': 'buggy' in covs,
        'coverage_patched': 'patched' in covs,
        'coverage_compiled': BUILD_COMPILED in covs,
        'static_kept': 'kept' in statics,
        'static_compiled': BUILD_COMPILED in statics,
        'crash_sites': sites is not None,
    }
    out['builds'] = [b for b in BUILDS if b in covs]

    rvars = _r_variants(r_methods, manifest)
    rvars_line = _r_variants_line(r_lines)

    # -- raw sizes ---------------------------------------------------------
    sizes: dict = {
        'P_method': len(p_methods) if p_methods is not None else None,
        'P_line': len(p_lines) if p_lines is not None else None,
        'P_method_by_ring': ({r: len(p_methods.refs(r)) for r in RING_ORDER}
                             if p_methods is not None else None),
        'P_line_by_ring': ({r: len(p_lines.refs(r)) for r in RING_ORDER}
                           if p_lines is not None else None),
        'R_method': {k: len(v) for k, v in rvars.items()},
        'R_method_by_ring': {
            k: {r: len(v.refs(r)) for r in RING_ORDER}
            for k, v in rvars.items()},
        'R_line': {k: len(v) for k, v in rvars_line.items()},
        'R_line_by_ring': {
            k: {r: len(v.refs(r)) for r in RING_ORDER}
            for k, v in rvars_line.items()},
        # How each build slot's F was obtained, next to how big it is: the
        # KINDS present for that slot, sorted.  A slot can now carry both —
        # JaCoCo coverage for 'dyn' and call-graph reachability for 'stat' —
        # so this is a list, and it is a list even when only one kind is
        # there.  See F_KINDS.
        'F_kind': _f_kinds(covs, statics),
        'F_method': {b: len(c.methods) for b, c in covs.items()},
        'F_line': {b: len(c.lines) for b, c in covs.items()},
        'F_all_methods': {b: len(c.all_methods) for b, c in covs.items()},
        # The static reachable set, per HARNESS SET (not per build: static
        # reach is the same on both builds).  `Fstat_entries` is the
        # narrower claim inside it — the library methods the harness
        # sources call directly, before the walk down.
        'Fstat_method': {k: len(v.methods) for k, v in statics.items()},
        'Fstat_entries': {k: len(v.entries) for k, v in statics.items()},
        'Fstat_harnesses': {k: len(v.harnesses) for k, v in statics.items()},
        'Fstat_source': {k: v.source for k, v in statics.items()},
        'Fstat_unmatched': {k: len(v.unmatched) for k, v in statics.items()},
        'branches': {b: {'covered': c.branches_covered,
                         'total': c.branches_total} for b, c in covs.items()},
        'manifest_method': len(manifest) if manifest is not None else None,
        # Where the CALLER ring came from: the call graph, or the source
        # scan that fills in for it when the graph resolved no caller.
        'P_caller_provenance': _caller_provenance(p_methods),
        'R_caller_provenance': _caller_provenance(r_methods),
    }
    if sites is not None:
        lib = [s for s in sites if s.site_kind == 'library']
        sizes['crash_sites_total'] = len(sites)
        sizes['crash_sites_library'] = len(lib)
        sizes['crash_sites_harness_only'] = len(sites) - len(lib)
        # Of those, the ones the acceptance gate itself produced, over all
        # compiled candidates.  Counted, and excluded from CSM below.
        sizes['crash_sites_compiled'] = sum(
            1 for s in sites if s.build == BUILD_COMPILED)
    else:
        sizes['crash_sites_total'] = None
        sizes['crash_sites_library'] = None
        sizes['crash_sites_harness_only'] = None
        sizes['crash_sites_compiled'] = None
    out['sizes'] = sizes

    matching: dict = {}

    # -- method granularity ------------------------------------------------
    # RCR is coverage-free, but when coverage exists its all_methods list is
    # the better identity space, so the primary build's list (computed
    # above, with the mislabelled-receiver pass) is used for it.
    for rvar, rset in rvars.items():
        proj = _project(rset, p_methods, primary_pop)
        matching[f'method__{rvar}__primary'] = dict(
            proj.stats, build=primary)
        if p_methods is not None:
            out[metric_key('rcr', 'method', rvar, None)] = dict(
                _ratio(len(proj.r & proj.p), len(proj.r)),
                by_ring=_by_ring_ratio(proj.r, lambda x: proj.r_ring.get(
                    x, loc.OUTSIDE), proj.p))
            out[f"rcr_cross__method__{rvar}__na"] = _cross(
                proj.r & proj.p,
                lambda x: proj.r_ring.get(x, loc.OUTSIDE),
                lambda x: proj.p_ring.get(x, loc.OUTSIDE))

        for build, cov in covs.items():
            bproj = _project(rset, p_methods, cov.all_methods or None)
            matching[f'method__{rvar}__{build}'] = dict(bproj.stats,
                                                        build=build)
            fset = set(cov.methods)
            ring_of = lambda x, _m=bproj: _m.r_ring.get(x, loc.OUTSIDE)
            inter = bproj.r & fset
            out[metric_key('rcc', 'method', rvar, build)] = dict(
                _ratio(len(inter), len(bproj.r)),
                by_ring=_by_ring_ratio(bproj.r, ring_of, fset))
            out[metric_key('rcp', 'method', rvar, build)] = dict(
                _ratio(len(inter), len(fset)),
                by_ring=_decompose(fset, ring_of, len(fset)))

    # PSC needs no R at all.
    if p_methods is not None:
        for build, cov in covs.items():
            fset = set(cov.methods)
            idx = loc.MethodIndex(cov.all_methods or p_methods.refs())
            pc, p_missed = _map_set(p_methods, idx,
                                    identity=not cov.all_methods)
            matching[f'psc__method__{build}'] = {
                'space': ('coverage_all_methods' if cov.all_methods else 'P'),
                'p_unmatched': p_missed, 'ambiguous': idx.ambiguous,
                'missing': idx.missing, 'reclassified': idx.reclassified,
                'p_caller_provenance': _caller_provenance(p_methods),
                'build': build}
            pset = set(pc)
            out[metric_key('psc', 'method', None, build)] = dict(
                _ratio(len(pset & fset), len(pset)),
                by_ring=_by_ring_ratio(
                    pset, lambda x: pc.get(x, loc.OUTSIDE), fset))

    # -- static reach (F_stat) ---------------------------------------------
    # METHOD GRANULARITY ONLY: a call graph names methods, so there is no
    # line-level static set and no `stat` key at line granularity.  The
    # build slot is a convention, not an observation — static reach is read
    # off the harness source and is identical on the buggy and the patched
    # build — so the kept set goes in the `buggy` slot and the all-compiled
    # set in `compiled`, which puts each one's static number in the same
    # table column as its dynamic number.  The fifth key slot says `stat`,
    # so the two can never be mistaken for each other.
    stat_only: Dict[str, dict] = {}
    dyn_only: Dict[str, dict] = {}
    for set_name, st in sorted(statics.items()):
        build = STATIC_BUILD_SLOT.get(set_name, set_name)
        for rvar, rset in rvars.items():
            sproj = _project(rset, p_methods, primary_pop)
            matching[f'method__{rvar}__{build}__stat'] = dict(
                sproj.stats, build=build, f_kind=F_KIND_STATIC,
                harness_set=set_name)
            fset = _map_refs(st.methods, sproj.index)
            ring_of = lambda x, _m=sproj: _m.r_ring.get(x, loc.OUTSIDE)
            inter = sproj.r & fset
            out[metric_key('rcc', 'method', rvar, build, F_KIND_STATIC)] = dict(
                _ratio(len(inter), len(sproj.r)),
                by_ring=_by_ring_ratio(sproj.r, ring_of, fset))
            out[metric_key('rcp', 'method', rvar, build, F_KIND_STATIC)] = dict(
                _ratio(len(inter), len(fset)),
                by_ring=_decompose(fset, ring_of, len(fset)))
            # "Pointed but not reached", and its opposite.  A method in
            # R that the harnesses could statically get to but never ran is
            # a FUZZING failure (the inputs never drove it there); one they
            # ran but no call in the source leads to is the static
            # analysis's blind spot (reflection, a lambda, a virtual call
            # the graph does not resolve).  Only computable where the same
            # slot has both kinds.
            if build in covs:
                fdyn = _map_refs(covs[build].methods, sproj.index)
                stat_only.setdefault(build, {})[rvar] = len(
                    (sproj.r & fset) - fdyn)
                dyn_only.setdefault(build, {})[rvar] = len(
                    (sproj.r & fdyn) - fset)
        if p_methods is not None:
            idx = loc.MethodIndex(primary_pop or p_methods.refs())
            pc, p_missed = _map_set(p_methods, idx,
                                    identity=primary_pop is None)
            fset = _map_refs(st.methods, idx)
            matching[f'psc__method__{build}__stat'] = {
                'space': ('coverage_all_methods' if primary_pop else 'P'),
                'p_unmatched': p_missed, 'ambiguous': idx.ambiguous,
                'missing': idx.missing, 'reclassified': idx.reclassified,
                'p_caller_provenance': _caller_provenance(p_methods),
                'build': build, 'f_kind': F_KIND_STATIC,
                'harness_set': set_name}
            pset = set(pc)
            out[metric_key('psc', 'method', None, build, F_KIND_STATIC)] = dict(
                _ratio(len(pset & fset), len(pset)),
                by_ring=_by_ring_ratio(
                    pset, lambda x: pc.get(x, loc.OUTSIDE), fset))
    sizes['R_stat_only'] = stat_only
    sizes['R_dyn_only'] = dyn_only

    # -- line granularity --------------------------------------------------
    for rvar, rlset in rvars_line.items():
        rl = set(rlset.refs())
        ring_of = _line_ring(rlset)
        if p_lines is not None:
            pl = set(p_lines.refs())
            out[metric_key('rcr', 'line', rvar, None)] = dict(
                _ratio(len(rl & pl), len(rl)),
                by_ring=_by_ring_ratio(rl, ring_of, pl))
            out[f"rcr_cross__line__{rvar}__na"] = _cross(
                rl & pl, ring_of, _line_ring(p_lines))
        for build, cov in covs.items():
            fl = set(cov.lines)
            inter = rl & fl
            out[metric_key('rcc', 'line', rvar, build)] = dict(
                _ratio(len(inter), len(rl)),
                by_ring=_by_ring_ratio(rl, ring_of, fl))
            out[metric_key('rcp', 'line', rvar, build)] = dict(
                _ratio(len(inter), len(fl)),
                by_ring=_decompose(fl, ring_of, len(fl)))
    if p_lines is not None:
        pl = set(p_lines.refs())
        for build, cov in covs.items():
            fl = set(cov.lines)
            out[metric_key('psc', 'line', None, build)] = dict(
                _ratio(len(pl & fl), len(pl)),
                by_ring=_by_ring_ratio(pl, _line_ring(p_lines), fl))

    # -- crash-site match --------------------------------------------------
    if sites is not None:
        lib = [s for s in sites if s.site_kind == 'library']
        out['crash_harness_only'] = len(sites) - len(lib)
        out['crash_library'] = len(lib)
        out['crash_total'] = len(sites)
        out['crash_by_build'] = {
            b: sum(1 for s in sites if s.build == b) for b in BUILDS}
        # CSM asks whether the crashes the KEPT harness set produced landed
        # in the root-cause region, so the acceptance gate's own crashes —
        # the ``compiled`` build, which includes the candidates that were
        # then thrown away — are counted above but never enter CSM's
        # denominator.  They are still visible as ``crash_by_build``
        # ['compiled'] and as ``crash_sites_compiled``.
        kept = [s for s in sites if s.build != BUILD_COMPILED]
        lib_kept = [s for s in kept if s.site_kind == 'library']
        out['crash_compiled'] = len(sites) - len(kept)
        lib = lib_kept
        sites_csm = kept
        for rvar, rset in rvars.items():
            index = loc.MethodIndex(rset.refs())
            resolved = [_site_in(index, s.method, rset) for s in lib]
            ring_of_site = [rset.ring_of(r) if r is not None else loc.OUTSIDE
                            for r in resolved]
            num = sum(1 for g in ring_of_site if g != loc.OUTSIDE)
            out[metric_key('csm', 'method', rvar, None)] = dict(
                _ratio(num, len(lib)),
                by_ring=_decompose(list(range(len(ring_of_site))),
                                   lambda i: ring_of_site[i], len(lib)),
                harness_only=len(sites_csm) - len(lib))
        for rvar, rlset in rvars_line.items():
            rings = [rlset.ring_of(s.line) if s.line is not None
                     else loc.OUTSIDE for s in lib]
            num = sum(1 for g in rings if g != loc.OUTSIDE)
            out[metric_key('csm', 'line', rvar, None)] = dict(
                _ratio(num, len(lib)),
                by_ring=_decompose(list(range(len(rings))),
                                   lambda i: rings[i], len(lib)),
                harness_only=len(sites_csm) - len(lib))

    out['matching'] = matching
    return out


# ---------------------------------------------------------------------------
# per-run entry point
# ---------------------------------------------------------------------------

def write_metrics(run_dir: str) -> List[dict]:
    """Compute every leg of `run_dir` and (re)write ``metrics.jsonl``.

    One JSON object per line, in leg-name order.  The file is overwritten,
    never appended to across runs, so it always matches the run on disk.
    A leg that raises records ``{'leg': ..., 'error': ...}`` and the sweep
    continues."""
    rows = []
    for leg in leg_dirs(run_dir):
        try:
            rows.append(compute_leg(leg))
        except Exception as exc:                       # never lose the sweep
            rows.append({'leg': os.path.basename(leg),
                         'leg_dir': os.path.abspath(leg),
                         'error': f'{type(exc).__name__}: {exc}'})
    path = os.path.join(run_dir, METRICS_FILE)
    with open(path, 'w') as fh:
        for row in rows:
            fh.write(json.dumps(row, sort_keys=True) + '\n')
    return rows


def read_metrics(run_dir: str) -> List[dict]:
    """Read back a run's ``metrics.jsonl`` (empty list when absent)."""
    path = os.path.join(run_dir, METRICS_FILE)
    if not os.path.isfile(path):
        return []
    rows = []
    with open(path) as fh:
        for line in fh:
            line = line.strip()
            if line:
                rows.append(json.loads(line))
    return rows
