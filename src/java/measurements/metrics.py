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
   ``coverage_patched.json``.
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
build        which coverage the F-side came from: ``buggy`` or ``patched``.
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
MEASUREMENTS_DIR = 'measurements'
RESULT_FILE = 'result.jsonl'
METRICS_FILE = 'metrics.jsonl'

BUILDS = ('buggy', 'patched')
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
#:           source calls.  Reserved for the planned static variant; nothing
#:           emits it yet.
F_KINDS = ('dyn', 'stat')

#: The only kind currently computed; every F-using key ends in it for now.
DEFAULT_F_KIND = 'dyn'

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


def _strip_jdk(ms: Optional[loc.MethodSet]):
    """(copy of `ms` without JDK methods, number dropped). Edges whose
    endpoint was dropped go with it; `unmatched` and `multi` are kept."""
    if ms is None:
        return None, 0
    keep = loc.MethodSet()
    dropped = 0
    for ref, tag in ms.items.items():
        if _is_jdk(ref):
            dropped += 1
            continue
        keep.items[ref] = tag
    keep.multi = {r: v for r, v in ms.multi.items() if r in keep.items}
    keep.edges = [(a, b) for a, b in ms.edges
                  if a in keep.items and b in keep.items]
    keep.unmatched = list(ms.unmatched)
    return keep, dropped


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
    out['jdk_dropped'] = {'P_method': p_jdk, 'R_method': r_jdk}
    covs = _load_coverage(mdir)
    sites = _load_crash_sites(mdir)

    out['available'] = {
        'patch_derived': p_methods is not None,
        'patch_derived_lines': p_lines is not None,
        'root_cause': r_methods is not None or r_lines is not None,
        'root_cause_manifest': manifest is not None,
        'coverage_buggy': 'buggy' in covs,
        'coverage_patched': 'patched' in covs,
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
        # How each build's F was obtained, next to how big it is.  Only
        # 'dyn' is produced today; see F_KINDS.
        'F_kind': {b: DEFAULT_F_KIND for b in covs},
        'F_method': {b: len(c.methods) for b, c in covs.items()},
        'F_line': {b: len(c.lines) for b, c in covs.items()},
        'F_all_methods': {b: len(c.all_methods) for b, c in covs.items()},
        'branches': {b: {'covered': c.branches_covered,
                         'total': c.branches_total} for b, c in covs.items()},
        'manifest_method': len(manifest) if manifest is not None else None,
    }
    if sites is not None:
        lib = [s for s in sites if s.site_kind == 'library']
        sizes['crash_sites_total'] = len(sites)
        sizes['crash_sites_library'] = len(lib)
        sizes['crash_sites_harness_only'] = len(sites) - len(lib)
    else:
        sizes['crash_sites_total'] = None
        sizes['crash_sites_library'] = None
        sizes['crash_sites_harness_only'] = None
    out['sizes'] = sizes

    matching: dict = {}

    # -- method granularity ------------------------------------------------
    # RCR is coverage-free, but when coverage exists its all_methods list is
    # the better identity space, so the primary build's list is used for it.
    primary = next((b for b in BUILDS if b in covs), None)
    primary_pop = (covs[primary].all_methods or None) if primary else None

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
                'missing': idx.missing, 'build': build}
            pset = set(pc)
            out[metric_key('psc', 'method', None, build)] = dict(
                _ratio(len(pset & fset), len(pset)),
                by_ring=_by_ring_ratio(
                    pset, lambda x: pc.get(x, loc.OUTSIDE), fset))

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
                harness_only=len(sites) - len(lib))
        for rvar, rlset in rvars_line.items():
            rings = [rlset.ring_of(s.line) if s.line is not None
                     else loc.OUTSIDE for s in lib]
            num = sum(1 for g in rings if g != loc.OUTSIDE)
            out[metric_key('csm', 'line', rvar, None)] = dict(
                _ratio(num, len(lib)),
                by_ring=_decompose(list(range(len(rings))),
                                   lambda i: rings[i], len(lib)),
                harness_only=len(sites) - len(lib))

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
