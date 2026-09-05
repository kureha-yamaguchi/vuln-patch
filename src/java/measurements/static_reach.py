"""F_stat(H) — what the harness set COULD reach.

MEASUREMENT ONLY. Nothing in the pipeline imports this module.

`coverage.py` measures F(H), the locations the harnesses actually executed
while fuzzing. This module measures the other half of the same question:
the locations they could have executed at all. The difference matters
because a harness that never runs a method may have missed it for two very
different reasons — it does not call it, or it calls it and the fuzzer's
inputs never got there — and only the second is a fuzzing problem.

How one harness's set is built
------------------------------
1. **Entry methods.** The harness's `FuzzHarness.java` is parsed with
   javalang, and every method invocation and constructor call in it is
   read out. A call gives us a name and a number of arguments; where the
   receiver is a variable whose declared type we can see (a local, a
   parameter or a field), or is written as a class name, it gives us a
   receiver class as well. Each call is then looked up in the project's
   own method list (`locations.MethodIndex` over the introspector function
   map): exactly when the receiver class is known, and by the unique
   (name, arity) match when it is not — the index's receiver-rescue rule.
   Calls that resolve to nothing are counted in `unmatched`; calls to the
   JDK, to the Jazzer API, and to the harness's own helper methods are
   dropped before that, since they were never library methods to begin
   with.
2. **The walk down.** From those entry methods the static call graph is
   walked downwards with `call_graph.bfs_callees_with_edges` — the same
   traversal, and the same two caps (`config.REACHABLE_NODE_CAP`,
   `config.REACHABLE_MAX_DEPTH`), the patch-derived set P uses. Using the
   same walk is the point: P and F_stat are then comparable set for set.

F_stat(H) for a harness SET is the union over its harnesses. Entries are
reported separately from the walked set (`entries`), because "the harness
calls this method" and "the harness could get to this method" are
different claims and the second is much weaker.

What this over-approximates
---------------------------
Resolution is by name and arity, with a receiver class only when the
source spells one out, so a same-named, same-arity method on an unrelated
class can be taken for the one that was meant. In the other direction,
anything the call graph cannot see is invisible here too: reflection,
dynamic dispatch through an interface, and the bodies of lambdas passed to
library methods. So F_stat is neither an upper nor a lower bound in the
strict sense; it is "the calls written down, plus what the call graph says
they lead to". Every number it feeds is reported next to `unmatched`, so a
small set can be traced to a resolution gap rather than read as a finding.

The two harness sets
--------------------
Same split as `coverage.py` (see the README, "Kept versus all compiled
harnesses"): `kept` is the harnesses the acceptance gate admitted, and
`compiled` is every candidate that compiled, kept or not. The sources come
from `<leg>/harness_src/<attempt>.java`, which a `--coverage` run saves
for every compiled candidate. A leg archived before that hook existed has
no such directory, and then the accepted harnesses are recovered from the
text of `trace.md` instead — the `kept` set only, marked `source: 'trace'`,
since the trace never records a rejected candidate's source under its own
attempt id.
"""
from __future__ import annotations

import json
import os
import re
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Sequence, Set, Tuple

import config
from java.bug_context.call_graph import bfs_callees_with_edges, function_map
from metrics.core.locations import (MethodIndex, MethodRef,
                                         from_introspector, simple_type)
from metrics.core.ratios import is_jdk

__all__ = ['StaticReach', 'harness_entries', 'static_reach', 'collect_leg',
           'project_index', 'harness_sources_from_trace',
           'SETS', 'BUILD_SLOT', 'STATIC_FILE']

#: The two harness sets a leg can carry, and the build slot each one's
#: metric keys use. Static reach is computed from source, so it is the same
#: on the buggy and the patched build; `buggy` is used for the kept set by
#: convention, because that is the primary build everywhere else.
SETS = ('kept', 'compiled')
BUILD_SLOT = {'kept': 'buggy', 'compiled': 'compiled'}

#: `<leg>/measurements/static_kept.json`, `static_compiled.json`.
STATIC_FILE = 'static_{set}.json'

#: Where a `--coverage` run saves each compiled candidate's harness source.
HARNESS_SRC_DIR = 'harness_src'

MEASUREMENTS_DIR = 'measurements'
RESULT_FILE = 'result.jsonl'
TRACE_FILE = 'trace.md'

#: Receiver types that are never library code: the Jazzer API the harness
#: is written against, and the harness class itself. Calls on them are
#: dropped silently rather than counted as unresolved names.
_HARNESS_PREFIX = 'FuzzHarness'
_JAZZER_TYPES = {
    'FuzzedDataProvider', 'FuzzerSecurityIssueLow',
    'FuzzerSecurityIssueMedium', 'FuzzerSecurityIssueHigh',
    'FuzzerSecurityIssueCritical', 'Jazzer',
}


# ---------------------------------------------------------------------------
# The set
# ---------------------------------------------------------------------------

@dataclass
class StaticReach:
    """One harness set's static reachable set.

    methods    the set itself: the entry methods plus everything the walk
               reached from them (a flat set — static reach has no rings,
               and no lines, since the call graph names methods only)
    entries    the library methods the harness sources call directly
    edges      every call-graph edge the walk crossed, as (from, to)
    unmatched  calls that resolved to no project method, as written
    harnesses  which harnesses this is the union over
    set_name   'kept' or 'compiled' (see `SETS`)
    source     'harness_src' when the sources came from the saved files,
               'trace' when they were recovered from `trace.md`
    """
    methods: Set[MethodRef] = field(default_factory=set)
    entries: List[MethodRef] = field(default_factory=list)
    edges: List[Tuple[MethodRef, MethodRef]] = field(default_factory=list)
    unmatched: List[str] = field(default_factory=list)
    harnesses: List[str] = field(default_factory=list)
    set_name: str = ''
    source: str = ''

    def __len__(self) -> int:
        return len(self.methods)

    def __contains__(self, ref: MethodRef) -> bool:
        return ref in self.methods

    def to_dict(self) -> dict:
        return {
            'granularity': 'method',
            'set': self.set_name,
            'source': self.source,
            'methods': [m.to_dict() for m in sorted(self.methods)],
            'entries': [m.to_dict() for m in sorted(self.entries)],
            'edges': [[a.to_dict(), b.to_dict()] for a, b in self.edges],
            'unmatched': list(self.unmatched),
            'harnesses': list(self.harnesses),
        }

    @classmethod
    def from_dict(cls, d: dict) -> 'StaticReach':
        return cls(
            methods={MethodRef.from_dict(x) for x in (d.get('methods') or [])},
            entries=[MethodRef.from_dict(x) for x in (d.get('entries') or [])],
            edges=[(MethodRef.from_dict(a), MethodRef.from_dict(b))
                   for a, b in (d.get('edges') or [])],
            unmatched=list(d.get('unmatched') or []),
            harnesses=list(d.get('harnesses') or []),
            set_name=d.get('set') or '',
            source=d.get('source') or '',
        )


# ---------------------------------------------------------------------------
# The project's own method list
# ---------------------------------------------------------------------------

def project_index(project) -> Tuple[MethodIndex, Dict[MethodRef, str]]:
    """`(index over every project method, ref -> its mangled name)`.

    The same population `neighbourhood.build` resolves seeds against, so a
    method named by a harness and a method named by a patch come out as the
    one ref. Names the introspector wrote that do not parse are skipped."""
    fmap = function_map(project)
    name_of: Dict[MethodRef, str] = {}
    population: List[MethodRef] = []
    for name in sorted(fmap):
        ref = from_introspector(name)
        if ref is None:
            continue
        population.append(ref)
        name_of.setdefault(ref, name)
    return MethodIndex(population), name_of


# ---------------------------------------------------------------------------
# Entry methods, out of one harness source
# ---------------------------------------------------------------------------

def _declared_types(tree) -> Dict[str, str]:
    """Variable name -> declared type's simple name, over the whole file.

    Locals, parameters and fields, all in one flat map. A harness is one
    small class with no shadowing worth modelling, so one map per file is
    enough; when a name IS declared twice with different types the last
    declaration wins and the call is resolved against that one."""
    import javalang

    out: Dict[str, str] = {}

    def _put(type_node, name):
        if not name or type_node is None:
            return
        tname = getattr(type_node, 'name', None)
        if tname:
            out[name] = simple_type(tname)

    for _path, node in tree:
        if isinstance(node, (javalang.tree.LocalVariableDeclaration,
                             javalang.tree.FieldDeclaration)):
            for decl in (node.declarators or []):
                _put(node.type, getattr(decl, 'name', None))
        elif isinstance(node, (javalang.tree.FormalParameter,
                               javalang.tree.CatchClauseParameter)):
            _put(getattr(node, 'type', None), getattr(node, 'name', None))
        elif isinstance(node, javalang.tree.TryResource):
            _put(getattr(node, 'type', None), getattr(node, 'name', None))
    return out


def _receiver_class(qualifier: Optional[str],
                    declared: Dict[str, str]) -> Optional[str]:
    """The class a call is made on, or None when the source does not say.

    `dist.sample()` with `HypergeometricDistribution dist` above it gives
    the declared type; `Math.abs(x)` gives the class name written in the
    call; `helper()` and `foo().bar()` give nothing, and those are the
    calls the index's receiver rescue has to resolve by name and arity."""
    if not qualifier:
        return None
    last = qualifier.split('.')[-1].strip()
    if not last:
        return None
    if last in declared:
        return declared[last]
    # A qualifier that is not a variable we saw is either a class name
    # (`Math.abs`) or a field of something we cannot see. An initial
    # capital is Java's own convention for the first case.
    if last[0].isupper():
        return last
    return None


def _ignored_receiver(cls: Optional[str]) -> bool:
    """True for a receiver that is never library code: the Jazzer API the
    harness is written against, and the harness class itself."""
    if not cls:
        return False
    return cls in _JAZZER_TYPES or cls.startswith(_HARNESS_PREFIX)


def _declared_methods(tree) -> Set[Tuple[str, int]]:
    """`(name, arity)` of every method the harness file declares itself.

    An unqualified call to one of them is the harness calling itself, which
    Java resolves that way too, so it must not be handed to the index — the
    receiver rescue would happily attribute `helper(x)` to some library
    method that happens to be called `helper` and take one argument."""
    import javalang

    out: Set[Tuple[str, int]] = set()
    for _path, node in tree:
        if isinstance(node, javalang.tree.MethodDeclaration):
            out.add((node.name, len(node.parameters or [])))
    return out


def _calls(java_source: str) -> List[Tuple[Optional[str], str, int]]:
    """Every call written in a harness, as (receiver class, name, arity).

    Method invocations and constructor calls, anywhere in the file — a
    lambda body is walked like any other body, because javalang's tree
    holds it as ordinary statements. `new Foo(...)` becomes
    `('Foo', '<init>', n)`.

    Raises whatever javalang raises on a source it cannot parse; the
    caller decides whether one unreadable harness is fatal."""
    import javalang

    tree = javalang.parse.parse(java_source)
    declared = _declared_types(tree)
    own = _declared_methods(tree)
    out: List[Tuple[Optional[str], str, int]] = []
    for _path, node in tree:
        if isinstance(node, javalang.tree.MethodInvocation):
            cls = _receiver_class(getattr(node, 'qualifier', None), declared)
            arity = len(node.arguments or [])
            if cls is None and (node.member, arity) in own:
                continue          # the harness calling its own helper
            out.append((cls, node.member, arity))
        elif isinstance(node, javalang.tree.SuperMethodInvocation):
            out.append((None, node.member, len(node.arguments or [])))
        elif isinstance(node, javalang.tree.ClassCreator):
            tname = getattr(getattr(node, 'type', None), 'name', None)
            if tname:
                out.append((simple_type(tname), '<init>',
                            len(node.arguments or [])))
    return out


def harness_entries(java_source: str,
                    index: MethodIndex) -> Tuple[List[MethodRef], List[str]]:
    """`(entry methods, unresolved call names)` for one harness source.

    The library methods the harness calls DIRECTLY. Each call is turned
    into a `MethodRef` carrying the receiver class the source spelled out
    (or none) and one placeholder parameter per argument, then looked up in
    `index`: the placeholder types never match strictly, so the match is
    the index's (simple class, name, arity) rule, falling through to the
    receiver rescue — the unique (name, arity) match anywhere in the
    project — when the receiver is unknown or wrong. See the module
    docstring for what that over-approximates.

    Calls on the JDK, on the Jazzer API, and on the harness's own declared
    methods are dropped before the lookup; they are not library methods and
    listing them as unresolved would bury the resolution failures that
    matter. Order is the order the calls
    appear, duplicates removed."""
    entries: List[MethodRef] = []
    unmatched: List[str] = []
    for cls, name, arity in _calls(java_source):
        if _ignored_receiver(cls):
            continue
        ref = MethodRef(cls or '', name, ('?',) * arity)
        if cls and is_jdk(ref):
            continue
        hit = index.lookup(ref)
        if hit is None:
            written = f"{cls or '?'}.{name}/{arity}"
            if written not in unmatched:
                unmatched.append(written)
            continue
        if hit not in entries:
            entries.append(hit)
    return entries, unmatched


# ---------------------------------------------------------------------------
# The walk down
# ---------------------------------------------------------------------------

def static_reach(entries: Sequence[MethodRef], project,
                 cap: Optional[int] = None,
                 depth: Optional[int] = None) -> StaticReach:
    """The entry methods plus everything the call graph reaches from them.

    The walk is `call_graph.bfs_callees_with_edges` under the pipeline's
    own two caps — `config.REACHABLE_NODE_CAP` nodes and
    `config.REACHABLE_MAX_DEPTH` levels — which are the caps the
    patch-derived set P is built with, so P and F_stat are comparable.
    The caps apply PER ENTRY, exactly as they apply per seed in P.

    Entries the call graph does not know (a method matched in the index but
    absent from the function map, which the two disagreeing on a spelling
    can produce) stay in the set as themselves and are listed in
    `unmatched`; they contribute no callees."""
    cap = config.REACHABLE_NODE_CAP if cap is None else cap
    depth = config.REACHABLE_MAX_DEPTH if depth is None else depth

    fmap = function_map(project)
    _index, name_of = project_index(project)

    out = StaticReach()
    edge_seen: Set[Tuple[MethodRef, MethodRef]] = set()
    unresolvable: List[str] = []

    def _ref(name: str) -> Optional[MethodRef]:
        r = from_introspector(name)
        if r is None and name not in unresolvable:
            unresolvable.append(name)
        return r

    for entry in entries:
        if entry not in out.entries:
            out.entries.append(entry)
        out.methods.add(entry)
        mangled = name_of.get(entry)
        if mangled is None or mangled not in fmap:
            if str(entry) not in unresolvable:
                unresolvable.append(str(entry))
            continue
        names, edges, _depths = bfs_callees_with_edges(
            fmap, mangled, cap, depth)
        for name in names:
            ref = _ref(name)
            if ref is not None:
                out.methods.add(ref)
        for src, dst in edges:
            a, b = _ref(src), _ref(dst)
            if a is None or b is None or (a, b) in edge_seen:
                continue
            edge_seen.add((a, b))
            out.edges.append((a, b))
    out.unmatched = list(unresolvable)
    return out


# ---------------------------------------------------------------------------
# Harness sources: the saved files, and the archive fallback
# ---------------------------------------------------------------------------

def _read(path: str) -> str:
    with open(path, encoding='utf-8', errors='replace') as fh:
        return fh.read()


def harness_sources_from_dir(src_dir: str) -> List[Tuple[str, str]]:
    """`[(attempt id, source)]` for every `.java` under `<leg>/harness_src`,
    in name order. The attempt id is the file's stem, which is the same id
    the candidate's `.exec` dump carries."""
    out: List[Tuple[str, str]] = []
    if not os.path.isdir(src_dir):
        return out
    for name in sorted(os.listdir(src_dir)):
        if not name.endswith('.java'):
            continue
        try:
            out.append((name[:-len('.java')],
                        _read(os.path.join(src_dir, name))))
        except OSError:
            continue
    return out


_SECTION_RE = re.compile(r'^## \[(\d+)\] (.*)$', re.MULTILINE)
_OUTPUT_BLOCK_RE = re.compile(
    r'<details[^>]*><summary>▸ Output[^<]*</summary>\s*\n+```[a-zA-Z]*\n'
    r'(.*?)\n```', re.DOTALL)
_HARNESS_TAG_RE = re.compile(r'<harness>\n(.*?)\n</harness>', re.DOTALL)
_ATTEMPT_RE = re.compile(r'`(attempt_\w+)`')


def _looks_like_harness(text: str) -> bool:
    """A Java file that is a Jazzer harness, not prose or a diff."""
    head = text.lstrip()
    return (head.startswith('package ')
            or head.startswith('import com.code_intelligence.jazzer'))


def harness_sources_from_trace(trace_path: str) -> List[Tuple[str, str]]:
    """`[(attempt id, source)]` for the ACCEPTED harnesses of an archived
    leg, recovered from the run's `trace.md`.

    Two places in the trace carry a harness source:

    * the **output of a `harness generation` LLM call** — the model's whole
      reply is the file, so it starts with `package` or with the Jazzer
      import;
    * the **`<harness>` block of a verifier prompt**, which quotes the
      harness being judged.

    Only the first can be tied to an attempt id: a `harness-attempt`
    section says ACCEPTED and names the attempt (``· `attempt_003` ``), and
    the harness it accepted is the generation output just above it. Those
    are taken with their ids. The `<harness>` blocks are added after them,
    under the id `harness_<n>`, and only when their text is not already
    there — a verifier only ever sees an accepted harness, so they add the
    accepted ones a missing generation output would have lost.

    Rejected candidates are deliberately NOT recovered: the trace prints
    their sources too, but nothing in it ties one to an attempt id, so a
    `compiled` set built from the trace could not say what it was over.
    Deduplication is on the source text with whitespace stripped."""
    try:
        text = _read(trace_path)
    except OSError:
        return []

    bounds = [(m.start(), m.group(2)) for m in _SECTION_RE.finditer(text)]
    bounds.append((len(text), ''))

    out: List[Tuple[str, str]] = []
    seen: Set[str] = set()

    def _add(label: str, source: str) -> None:
        key = ''.join(source.split())
        if not key or key in seen:
            return
        seen.add(key)
        out.append((label, source))

    pending: Optional[str] = None
    for i in range(len(bounds) - 1):
        start, header = bounds[i]
        body = text[start:bounds[i + 1][0]]
        if 'harness generation' in header:
            for block in _OUTPUT_BLOCK_RE.findall(body):
                if _looks_like_harness(block):
                    pending = block
        elif 'harness-attempt' in header:
            if '**ACCEPTED' in body and pending is not None:
                label = _ATTEMPT_RE.search(header)
                _add(label.group(1) if label else f'step_{i}', pending)
            pending = None

    for n, block in enumerate(_HARNESS_TAG_RE.findall(text), start=1):
        if _looks_like_harness(block):
            _add(f'harness_{n}', block)
    return out


# ---------------------------------------------------------------------------
# One archived run leg
# ---------------------------------------------------------------------------

def _result_coverage(leg_dir: str) -> dict:
    """The `coverage` extras block of a leg's `result.jsonl` ({} when the
    run predates the hook that writes it)."""
    path = os.path.join(leg_dir, RESULT_FILE)
    if not os.path.isfile(path):
        return {}
    try:
        with open(path) as fh:
            for line in fh:
                line = line.strip()
                if not line:
                    continue
                rec = json.loads(line)
                cov = rec.get('coverage')
                return cov if isinstance(cov, dict) else {}
    except (OSError, ValueError):
        return {}
    return {}


_Sourced = Dict[str, Tuple[str, List[Tuple[str, str]]]]


def _sources_for_sets(leg_dir: str) -> _Sourced:
    """`{set name: (where it came from, [(attempt, source)])}`.

    `compiled` is every saved harness source; `kept` is the subset the
    acceptance gate admitted, which `result.jsonl` names under
    ``coverage.accepted_attempts``. When the leg has no saved sources at
    all — an archived run, made before the hook that saves them — `kept`
    falls back to the accepted harnesses recovered from `trace.md` and
    `compiled` is simply absent, since the trace cannot name a rejected
    candidate's attempt (see `harness_sources_from_trace`)."""
    saved = harness_sources_from_dir(os.path.join(leg_dir, HARNESS_SRC_DIR))
    out: Dict[str, Tuple[str, List[Tuple[str, str]]]] = {}
    if saved:
        cov = _result_coverage(leg_dir)
        out['compiled'] = ('harness_src', saved)
        if isinstance(cov.get('accepted_attempts'), list):
            accepted = set(cov['accepted_attempts'])
            out['kept'] = ('harness_src',
                           [(a, s) for a, s in saved if a in accepted])
        else:
            # The record does not say which the gate kept, so the trace is
            # the only thing that does.
            from_trace = harness_sources_from_trace(
                os.path.join(leg_dir, TRACE_FILE))
            if from_trace:
                out['kept'] = ('trace', from_trace)
        return out
    from_trace = harness_sources_from_trace(os.path.join(leg_dir, TRACE_FILE))
    if from_trace:
        out['kept'] = ('trace', from_trace)
    return out


def collect_leg(leg_dir: str, project) -> Dict[str, StaticReach]:
    """F_stat for one run leg, one `StaticReach` per harness set.

    `project` is a fuzz-introspector project object — the same one
    `neighbourhood.build` takes, and the same one the leg's R was built
    from, so the two sets are named the same way and can be intersected.

    Writes ``<leg>/measurements/static_kept.json`` and
    ``static_compiled.json`` and returns what it wrote. A set with no
    harness sources is absent from both the result and the disk. A harness
    that does not parse is skipped and recorded in `unmatched`, so one bad
    file costs its own entries and nothing else."""
    leg_dir = os.path.abspath(leg_dir)
    index, _name_of = project_index(project)
    sources = _sources_for_sets(leg_dir)

    out_dir = os.path.join(leg_dir, MEASUREMENTS_DIR)
    os.makedirs(out_dir, exist_ok=True)

    result: Dict[str, StaticReach] = {}
    for set_name in SETS:
        if set_name not in sources:
            continue
        origin, items = sources[set_name]
        entries: List[MethodRef] = []
        unmatched: List[str] = []
        harnesses: List[str] = []
        for label, source in items:
            harnesses.append(label)
            try:
                found, missed = harness_entries(source, index)
            except Exception as exc:                   # noqa: BLE001
                missed = [f'{label}: unparsed ({type(exc).__name__})']
                found = []
            for ref in found:
                if ref not in entries:
                    entries.append(ref)
            for name in missed:
                if name not in unmatched:
                    unmatched.append(name)
        reach = static_reach(entries, project)
        for name in unmatched:
            if name not in reach.unmatched:
                reach.unmatched.append(name)
        reach.harnesses = harnesses
        reach.set_name = set_name
        reach.source = origin
        result[set_name] = reach
        with open(os.path.join(out_dir,
                               STATIC_FILE.format(set=set_name)), 'w') as fh:
            json.dump(reach.to_dict(), fh, indent=1)
    return result


def load(path: str) -> StaticReach:
    """Read one `static_<set>.json` back."""
    with open(path) as fh:
        return StaticReach.from_dict(json.load(fh))
