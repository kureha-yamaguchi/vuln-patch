"""A queryable call graph of a C/C++ checkout: callees, CALLERS, and the route
in from an existing fuzz target.

Why this module exists at all — two findings about the previous arrangement.

**1. The reachable set was silently dead for every pure-C project.** The old
code read ``project.all_functions`` from ``fuzz_introspector.frontends.oss_fuzz
.analyse_folder``. For ``language='c++'`` that list is populated (as a side
effect of ``CppProject.generate_report``), but ``CProject`` never assigns it —
its report is built from ``no_fuzz_function_list`` instead, and ``all_functions``
stays ``[]`` for the object's whole life. So on a C project the function map came
back empty, every touched name failed to resolve, and ``_reachable_set`` took its
"introspector added nothing" branch and fell back to the brace-match heuristic —
after paying for a full tree-sitter parse of the checkout (budget: 30 minutes).
Indexing ``source_code_files[*].func_defs`` directly, as below, works for both
frontends because that is where both of them actually keep their functions.

**2. It only ever looked downstream.** A bounded BFS over callees describes the
blast radius *below* the patched code. But the question a libFuzzer harness has
to answer first is the opposite one — *how do I get from
``LLVMFuzzerTestOneInput`` to here?* — and that is the caller direction, which
the Java front-end has carried as ``xrefs`` from the start. This module supplies
both, plus a concrete shortest path from a real fuzz target where one exists.

Going to the frontends directly also drops three costs ``analyse_folder``
imposes: ``generate_report``'s per-function ``calculate_function_uses`` /
``calculate_function_depth`` (the same O(N^2) pathology the Java path has to
monkeypatch), ``extract_calltree`` per in-tree harness, and — because
``process_c_project`` passes no ``dump_output`` to its ``module_only`` branch and
joins its paths against ``out=''`` — a stray ``report.yaml`` and
``fuzzerLogFile-0.data`` written into the current working directory.

Everything is bounded and fail-soft: file count, function count and BFS budget
are all capped, every accessor tolerates a shape it does not recognise, and
``build_index`` returns ``None`` rather than raising. A missing index costs
steering quality; the trigger gate is still what decides whether a harness is
valid.
"""
from __future__ import annotations

import os
import re
from dataclasses import dataclass, field
from typing import Dict, Iterable, List, Optional, Sequence, Set, Tuple

import config

try:
    from fuzz_introspector.frontends import frontend_c as _fe_c
    from fuzz_introspector.frontends import frontend_cpp as _fe_cpp
    FRONTENDS_AVAILABLE = True
except ImportError:          # pragma: no cover - exercised by the extra-less env
    _fe_c = None             # type: ignore[assignment]
    _fe_cpp = None           # type: ignore[assignment]
    FRONTENDS_AVAILABLE = False

# The libFuzzer entry point. Every OSS-Fuzz C/C++ target defines exactly one, so
# it is both the marker for "this file is a harness" and the root of the
# route-in search.
FUZZ_ENTRY = "LLVMFuzzerTestOneInput"

_C_EXTS = (".c", ".h")
_CPP_EXTS = (".c", ".cc", ".cpp", ".cxx", ".c++", ".C", ".h", ".hpp", ".hh",
             ".hxx", ".inl", ".ipp")
_HEADER_EXTS = (".h", ".hpp", ".hh", ".hxx", ".inl", ".ipp")

# Directories not worth indexing. Deliberately NOT the analyser's
# ``_NON_LIBRARY_DIRS``: that list excludes ``fuzz``/``fuzzers`` because a fix
# touching them yields no root-cause functions, whereas here those directories
# hold the harnesses whose reachability is the most useful thing we compute.
_SKIP_DIRS = {
    ".git", ".github", "node_modules", "build", "out", "cmake-build",
    "test", "tests", "testing", "unittest", "unittests", "testdata",
    "example", "examples", "demo", "demos", "sample", "samples",
    "doc", "docs", "man", "benchmark", "benchmarks",
    "third_party", "thirdparty", "vendor", "external", "deps", "subprojects",
    "bindings", "binding", "contrib",
}

_STATIC_RE = re.compile(r"(?:^|\s)static(?:\s|$)")


@dataclass
class FuncNode:
    """One function definition, normalised across the two frontends."""
    name: str                                  # index key ('ns::C::m' or 'f')
    file: str = ""
    start_line: int = 0
    end_line: int = 0
    # The declaration exactly as written, taken off the AST node and cut at the
    # body — ground truth, and the only form that is right for both frontends.
    # Reconstructing it from arg_types/return_type is not good enough: the C++
    # frontend often leaves arg_types empty, which renders `Reader::Read()` for a
    # two-argument method, and a signature that is confidently wrong about arity
    # is worse for the model than no signature at all.
    decl: str = ""
    arg_types: List[str] = field(default_factory=list)
    return_type: str = ""
    callees: List[str] = field(default_factory=list)
    in_harness_file: bool = False
    in_header: bool = False
    internal: bool = False                     # `static`/anonymous namespace

    @property
    def short_name(self) -> str:
        """Trailing segment — what a C++ name matches on when the caller only
        knows the method name."""
        return self.name.rsplit("::", 1)[-1]

    def signature(self) -> Optional[str]:
        """``int parse_item(const char *p, unsigned long n)``, or None.

        The region used to be bare names, which left the model to invent every
        call it made — and here a wrong guess is not a wrong line, it is a full
        Docker build spent finding out. None when we could not read the
        declaration, so the prompt shows the name alone rather than a guess.
        """
        if self.decl:
            return self.decl
        if self.arg_types:
            ret = f"{self.return_type} " if self.return_type else ""
            return f"{ret}{self.name}({', '.join(self.arg_types)})"
        return None

    def where(self) -> str:
        return f"{self.file}:{self.start_line}" if self.file else ""


@dataclass
class IndexStats:
    """What the index cost and how complete it is.

    Reported rather than kept private: a cap that silently truncates coverage
    reads as "we looked everywhere and found nothing".
    """
    files: int = 0
    files_skipped_cap: int = 0
    functions: int = 0
    functions_skipped_cap: int = 0
    harness_files: int = 0
    entry_points: int = 0
    parse_failures: int = 0

    def describe(self) -> str:
        out = (f"{self.functions} functions in {self.files} files, "
               f"{self.entry_points} fuzz entry point(s)")
        if self.files_skipped_cap or self.functions_skipped_cap:
            out += (f" [CAPPED: {self.files_skipped_cap} files and "
                    f"{self.functions_skipped_cap} functions not indexed]")
        if self.parse_failures:
            out += f" ({self.parse_failures} files failed to parse)"
        return out


class CodeIndex:
    """Name -> function, plus both edge directions over the whole checkout."""

    def __init__(self, nodes: Sequence[FuncNode],
                 stats: Optional[IndexStats] = None):
        self.stats = stats or IndexStats()
        self.by_name: Dict[str, FuncNode] = {}
        # Trailing-segment index for C++ ('parse' -> 'xml::Reader::parse').
        # A tail shared by several definitions is ambiguous, so it is dropped
        # rather than resolved to whichever happened to be indexed first —
        # steering toward the wrong overload is worse than steering by name.
        tails: Dict[str, List[str]] = {}
        for node in nodes:
            if not node.name:
                continue
            # First definition wins. A header's inline definition and a .cc's
            # out-of-line one are the same function to us, and re-registering
            # would flip the recorded location for no gain.
            self.by_name.setdefault(node.name, node)
            tails.setdefault(node.short_name, []).append(node.name)
        self.by_tail: Dict[str, str] = {
            tail: names[0] for tail, names in tails.items()
            if len(set(names)) == 1}
        self.ambiguous_tails: Set[str] = {
            tail for tail, names in tails.items() if len(set(names)) > 1}

        # Reverse edges. One pass, because the forward lists are already here
        # and a per-query scan of every function (which is what introspector's
        # own get_cross_references_by_name does) is O(N) per lookup.
        self.callers: Dict[str, List[str]] = {}
        for node in self.by_name.values():
            for callee in node.callees:
                key = self._key_of(callee)
                if key is None:
                    continue
                bucket = self.callers.setdefault(key, [])
                if node.name not in bucket:
                    bucket.append(node.name)

        self.entry_points: List[str] = [
            n.name for n in self.by_name.values()
            if n.short_name == FUZZ_ENTRY or n.name == FUZZ_ENTRY]
        self.stats.entry_points = len(self.entry_points)
        self._entry_parent: Optional[Dict[str, Optional[str]]] = None
        # False once the entry-point search has run and hit its node budget.
        # Load-bearing: with a truncated search, "no route found" is not the
        # same claim as "no route exists", and the prompt says one of those out
        # loud. See entry_search_complete.
        self._entry_complete = True

    # -- resolution --------------------------------------------------------
    def _key_of(self, name: str) -> Optional[str]:
        """A callee name as it appears at a call site, mapped to an index key.

        Only names the index actually defines resolve, which is how libc and
        third-party noise is dropped without a denylist: ``strlen`` has no
        definition in the checkout, so it is not a project function. This is the
        same rule the Java path applies via its package prefix.
        """
        if not name:
            return None
        if name in self.by_name:
            return name
        tail = name.rsplit("::", 1)[-1]
        return self.by_tail.get(tail)

    def resolve(self, name: str) -> Optional[FuncNode]:
        key = self._key_of(name)
        return self.by_name.get(key) if key else None

    def resolve_at(self, file_hint: Optional[str],
                   line: Optional[int]) -> Optional[FuncNode]:
        """The function whose body spans ``line`` in a file ending
        ``file_hint`` — how a sanitizer frame's location is turned into a
        function when its symbol does not match the index (inlined, mangled, or
        a static in a translation unit we indexed under a different name)."""
        if not file_hint or not line:
            return None
        base = os.path.basename(file_hint)
        for node in self.by_name.values():
            if node.file and os.path.basename(node.file) == base \
                    and node.start_line <= line <= max(node.end_line,
                                                       node.start_line):
                return node
        return None

    # -- edges -------------------------------------------------------------
    def callees_bfs(self, seeds: Iterable[str], cap: int,
                    max_depth: int) -> List[Tuple[str, int]]:
        """(name, depth) pairs downstream of ``seeds``, breadth first.

        Bounded by ``cap`` nodes and ``max_depth`` levels, so cost is O(cap)
        whatever the graph looks like — the same guarantee the Java BFS gives,
        and the reason neither uses introspector's unbounded transitive walk.
        Depth is kept because it is the ranking signal: a direct callee of the
        patched line is a far better sibling candidate than something four hops
        away, and the old flat list threw that away.
        """
        seen: Set[str] = set()
        queue: List[Tuple[str, int]] = []
        for seed in seeds:
            key = self._key_of(seed)
            if key and key not in seen:
                seen.add(key)
                queue.append((key, 0))
        out: List[Tuple[str, int]] = []
        while queue and len(out) < cap:
            name, depth = queue.pop(0)
            node = self.by_name.get(name)
            if node is None:
                continue
            for callee in node.callees:
                key = self._key_of(callee)
                if key is None or key in seen:
                    continue
                seen.add(key)
                out.append((key, depth + 1))
                if len(out) >= cap:
                    break
                if depth + 1 < max_depth:
                    queue.append((key, depth + 1))
        return out

    def callers_of(self, name: str, limit: int = 8) -> List[str]:
        key = self._key_of(name)
        return list(self.callers.get(key, []))[:limit] if key else []

    # -- the route in ------------------------------------------------------
    def _build_entry_parents(self) -> Dict[str, Optional[str]]:
        """Forward BFS from every fuzz entry point, remembering each node's
        discoverer, so a shortest route from a real harness to any reachable
        function is one dictionary walk away.

        One traversal for the whole checkout rather than a search per target:
        the answer is the same for every function we later ask about, and the
        set of nodes it visits is the honest definition of "the existing
        harnesses can get here at all".
        """
        parent: Dict[str, Optional[str]] = {}
        queue: List[str] = []
        for entry in self.entry_points:
            parent[entry] = None
            queue.append(entry)
        # Generous, because this is a single O(edges) pass over an in-memory
        # graph — 36k functions traverse in well under a second — and because
        # the cost of setting it too low is not a slower answer but a WRONG
        # one: an unfinished search reports functions as unreachable, and the
        # prompt tells the model to go and open its own route in on the
        # strength of that. The cap is a runaway backstop, not a budget.
        budget = max(config.ENTRY_BFS_NODE_CAP, 1)
        visited = 0
        while queue:
            if visited >= budget:
                self._entry_complete = False
                print(f"  WARNING: the entry-point reachability search stopped "
                      f"at {budget} nodes with {len(queue)} still queued; "
                      "'no existing target reaches it' is downgraded to "
                      "'not determined' (raise ENTRY_BFS_NODE_CAP)")
                break
            name = queue.pop(0)
            visited += 1
            node = self.by_name.get(name)
            if node is None:
                continue
            for callee in node.callees:
                key = self._key_of(callee)
                if key is None or key in parent:
                    continue
                parent[key] = name
                queue.append(key)
        return parent

    @property
    def entry_search_complete(self) -> bool:
        """True when the route-in search ran to exhaustion, so a missing path
        really does mean no existing target reaches that function."""
        if self._entry_parent is None:
            self._entry_parent = self._build_entry_parents()
        return self._entry_complete

    @property
    def entry_reachable(self) -> Set[str]:
        if self._entry_parent is None:
            self._entry_parent = self._build_entry_parents()
        return set(self._entry_parent)

    def path_from_entry(self, name: str,
                        max_len: int = 8) -> Optional[List[str]]:
        """``[LLVMFuzzerTestOneInput, ..., name]``, or None if no existing
        target reaches it.

        None is informative, not a failure: it says the model must find its own
        way in, and the callers list is then the only lead. Long paths are
        truncated in the middle — the two ends (the entry and the target) are
        the part that steers.
        """
        if self._entry_parent is None:
            self._entry_parent = self._build_entry_parents()
        key = self._key_of(name)
        if key is None or key not in self._entry_parent:
            return None
        chain: List[str] = []
        cur: Optional[str] = key
        while cur is not None and len(chain) <= max_len * 2:
            chain.append(cur)
            cur = self._entry_parent.get(cur)
        chain.reverse()
        if len(chain) > max_len:
            head = chain[:max_len - 2]
            return head + [f"...({len(chain) - max_len + 1} frames)",
                           chain[-1]]
        return chain


# -- construction ----------------------------------------------------------
def build_index(root: str, language: str,
                file_cap: Optional[int] = None,
                function_cap: Optional[int] = None) -> Optional[CodeIndex]:
    """Index ``root`` with the tree-sitter frontend for ``language``.

    Returns None when the introspector extra is absent or the parse produced
    nothing usable, which is the caller's signal to fall back to the heuristic.
    """
    if not FRONTENDS_AVAILABLE:
        return None
    file_cap = file_cap if file_cap is not None else config.INDEX_FILE_CAP
    function_cap = (function_cap if function_cap is not None
                    else config.INDEX_FUNCTION_CAP)
    is_c = language.lower() == "c"
    paths, skipped = _source_files(root, _C_EXTS if is_c else _CPP_EXTS,
                                   file_cap)
    if not paths:
        return None
    stats = IndexStats(files=len(paths), files_skipped_cap=skipped)

    frontend = _fe_c if is_c else _fe_cpp
    try:
        project = frontend.load_treesitter_trees(paths, is_log=False)
    except Exception:
        return None

    nodes: List[FuncNode] = []
    for source in getattr(project, "source_code_files", None) or []:
        try:
            is_harness = bool(source.has_libfuzzer_harness())
        except Exception:
            is_harness = False
        if is_harness:
            stats.harness_files += 1
        src_file = getattr(source, "source_file", "") or ""
        in_header = src_file.endswith(_HEADER_EXTS)
        defs = getattr(source, "func_defs", None) or []
        for fd in defs:
            if len(nodes) >= function_cap:
                stats.functions_skipped_cap += 1
                continue
            node = _normalise(fd, project, src_file, is_harness, in_header,
                              is_c)
            if node is not None:
                nodes.append(node)
    stats.functions = len(nodes)
    if not nodes:
        return None
    index = CodeIndex(nodes, stats)
    # Relative paths throughout: a container path is meaningless to whoever
    # reads the prompt, and the model was shown project-relative files.
    for node in index.by_name.values():
        node.file = _relative(node.file, root)
    return index


def _normalise(fd, project, src_file: str, is_harness: bool,
               in_header: bool, is_c: bool) -> Optional[FuncNode]:
    """One frontend function object -> ``FuncNode``.

    The two frontends do not share an interface: ``frontend_c`` exposes
    ``name()``/``callsites()`` as methods computed on demand and keeps line
    numbers on the tree-sitter node, while ``frontend_cpp`` exposes ``name``/
    ``base_callsites`` as attributes that only exist once
    ``extract_callsites`` has run. Each accessor is attempted independently so
    one unparseable declaration costs its own fields, not the function.
    """
    name = _try(lambda: fd.name() if is_c else fd.name, "")
    if not isinstance(name, str) or not name:
        return None
    decl = _decl_text(fd)
    if is_c:
        callees = [c[0] for c in _try(fd.callsites, []) or []
                   if isinstance(c, (list, tuple)) and c]
        start = _try(lambda: fd.root.start_point.row + 1, 0)
        end = _try(lambda: fd.root.end_point.row + 1, 0)
        args = _try(fd.get_function_arg_types, []) or []
        ret = _try(fd.get_function_return_type, "") or ""
        sig_text = decl or _try(fd.function_signature, "") or ""
    else:
        # Idempotent, and cheap when generate_report already ran.
        _try(lambda: fd.extract_callsites(project), None)
        callees = [c[0] for c in (getattr(fd, "base_callsites", None) or [])
                   if isinstance(c, (list, tuple)) and c]
        start = getattr(fd, "start_line", 0) or 0
        end = getattr(fd, "end_line", 0) or 0
        args = list(getattr(fd, "arg_types", None) or [])
        ret = getattr(fd, "return_type", "") or ""
        sig_text = decl or getattr(fd, "sig", "") or ""
    return FuncNode(
        name=name, file=src_file, start_line=start, end_line=end,
        decl=decl,
        arg_types=[a for a in args if isinstance(a, str)],
        return_type=ret if isinstance(ret, str) else "",
        callees=[c for c in callees if isinstance(c, str)],
        in_harness_file=is_harness, in_header=in_header,
        # A `static` function has internal linkage: a harness compiled as its
        # own translation unit cannot call it at all, so telling the model to
        # "steer toward" it is telling it to do something impossible. The
        # callers list is the usable form of that target.
        internal=bool(_STATIC_RE.search(sig_text))
        or "(anonymous namespace)" in name,
    )


def _try(fn, default):
    try:
        return fn()
    except Exception:
        return default


def _decl_text(fd, limit: int = 300) -> str:
    """The function's declaration as written, cut at the opening of its body.

    Straight off the tree-sitter node, so it carries the qualifiers both
    frontends drop when they rebuild a signature from parts (``const``, ``*``,
    default arguments, ``static``). Whitespace is collapsed because a
    declaration routinely spans several lines and the prompt wants one.
    """
    text = _try(lambda: fd.root.text.decode("utf-8", "replace"), "") or ""
    if not text:
        return ""
    cut = text.find("{")
    head = text[:cut] if cut > 0 else text
    # A constructor's member-init list is not part of the signature and can be
    # long; the parameter list ends at the matching ')'.
    colon = head.find("\n:")
    if colon > 0:
        head = head[:colon]
    head = " ".join(head.split())
    return head[:limit] + ("..." if len(head) > limit else "")


def _source_files(root: str, exts: Tuple[str, ...],
                  cap: int) -> Tuple[List[str], int]:
    """Project sources worth indexing, deterministically ordered.

    Sorted before the cap is applied so a truncated index is reproducible
    rather than dependent on directory iteration order, and shallow paths come
    first because a project's own library code sits nearer the root than the
    trees we did not manage to exclude by name.
    """
    found: List[str] = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames
                       if d.lower() not in _SKIP_DIRS
                       and not d.startswith(".")]
        for fn in filenames:
            if fn.endswith(exts):
                found.append(os.path.join(dirpath, fn))
    found.sort(key=lambda p: (p.count(os.sep), p))
    if len(found) <= cap:
        return found, 0
    return found[:cap], len(found) - cap


def _relative(path: str, root: str) -> str:
    try:
        return os.path.relpath(path, root)
    except ValueError:
        return path
