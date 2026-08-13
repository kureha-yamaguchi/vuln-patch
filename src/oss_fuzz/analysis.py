"""Turn a fix diff (plus the original crash) into the root-cause context the
prompt needs.

Three signals, mirroring ``src/java/bug_context/analysis.py``:

  1. A unified-diff parse + brace-match finds the C/C++ function that
     physically contains each changed line (the inverse of a call graph:
     "which function is line N in").

  2. The ORIGINAL CRASH STACK seeds the neighbourhood alongside the diff. The
     diff says what changed; the stack says where it blew up, and the two are
     routinely not the same function — a fix in a bounds helper leaves the
     innermost frame outside the diff entirely. Seeding from both is what makes
     the region a description of the fault rather than of the commit.

  3. ``callgraph.CodeIndex`` (tree-sitter, no build) supplies the graph, in both
     directions. Downstream we do a bounded BFS over callees, as before. Upstream
     we now also list each root-cause function's CALLERS and, when an existing
     fuzz target reaches it, a concrete path from ``LLVMFuzzerTestOneInput`` —
     because "how do I get in" is the question a harness has to answer before
     "what is nearby", and it was the half this front-end never computed.

Every entry in the resulting region carries its file, line, signature, depth and
whether an existing target can reach it (``ReachableFn``), so the prompt can
show a map instead of a list of bare names. ``PatchContext.root_cause_reachable``
is still the plain list of names, for the shared variant block and the artifacts
record.

If the index cannot be built (introspector extra absent, parse failure, timeout)
we fall back to the original brace-match heuristic (touched names + every
``ident(`` in their bodies). Either way the trigger gate — not the analysis — is
what ultimately decides whether a generated harness is valid, so a degraded
region costs steering quality, not correctness.

Install the introspector extra the same way the Java path does:
``uv sync --extra introspector``.
"""
from __future__ import annotations

import os
import re
from dataclasses import dataclass, field
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

import config
from oss_fuzz.callgraph import (FRONTENDS_AVAILABLE, FUZZ_ENTRY, CodeIndex,
                                FuncNode, build_index)

# Lines that end in ')' or '){' at column 0-ish and are NOT control flow are
# treated as function headers. Crude but effective for typical C/C++ style.
_CONTROL = {"if", "for", "while", "switch", "do", "else", "return",
            "sizeof", "case"}

# Common libc/stdlib entry points. Only used by the heuristic reachable set,
# which has no call graph to tell project functions from external ones — see
# DiffAnalyzer._reachable_heuristic. Not exhaustive by design: it covers the
# names that actually crowd out real targets in C fix diffs.
_LIBC_NAMES = {
    # memory
    "malloc", "calloc", "realloc", "free", "memcpy", "memmove", "memset",
    "memcmp", "memchr", "alloca", "bzero",
    # strings
    "strlen", "strnlen", "strcpy", "strncpy", "strcat", "strncat", "strcmp",
    "strncmp", "strcasecmp", "strncasecmp", "strchr", "strrchr", "strstr",
    "strdup", "strndup", "strtok", "strspn", "strcspn", "strpbrk", "strerror",
    # conversion / formatting
    "atoi", "atol", "atoll", "atof", "strtol", "strtoul", "strtoll",
    "strtoull", "strtod", "sprintf", "snprintf", "vsnprintf", "sscanf",
    "printf", "fprintf", "puts", "putchar",
    # ctype
    "isalpha", "isdigit", "isalnum", "isspace", "isupper", "islower",
    "isprint", "ispunct", "isxdigit", "tolower", "toupper",
    # stdio / os
    "fopen", "fclose", "fread", "fwrite", "fseek", "ftell", "fflush", "fgets",
    "open", "close", "read", "write", "lseek", "stat", "fstat",
    "abort", "exit", "assert", "qsort", "bsearch", "time", "getenv",
}
_CALL_RE = re.compile(r"\b([A-Za-z_][A-Za-z0-9_]*)\s*\(")
_HUNK_RE = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@")

# Directory names whose contents are not library code: tests, the project's own
# fuzz harnesses, CLI tools, examples and language bindings. A fix that also
# updates these (very common in a broad upstream sync) must not contribute
# root-cause functions — see DiffAnalyzer._is_non_library.
_NON_LIBRARY_DIRS = {
    "test", "tests", "testing", "unittest", "unittests", "suite",
    "fuzz", "fuzzing", "fuzzer", "fuzzers", "oss-fuzz",
    "example", "examples", "demo", "demos", "sample", "samples",
    "bindings", "binding", "tools", "tool", "benchmark", "benchmarks",
    "docs", "doc", "contrib", "third_party", "thirdparty", "vendor",
}


@dataclass
class TouchedFunction:
    file: str
    name: str
    source: str
    start_line: int
    # Why this function is in the context: 'diff' (the fix changed lines inside
    # it) or 'crash-frame' (the original crash report named it). Both are
    # root-cause evidence and the prompt shows both, but it must not claim the
    # fix sits inside a function it does not.
    origin: str = "diff"


@dataclass
class ReachableFn:
    """One function in the root-cause neighbourhood, with directions on it.

    The old region was a list of bare names, which asked the model to guess
    three separate things it could not: what the function's signature is, where
    it lives, and how execution could ever get there. Each of those guesses
    costs a Docker build to disprove. Every field below is something the static
    index already knew and was throwing away.
    """
    name: str
    # 'crash-frame+touched' | 'crash-frame' | 'touched' | 'route-in' | 'callee',
    # in that order of priority — see DiffAnalyzer._region.
    tier: str = "callee"
    depth: int = 0                       # hops from a seed; 0 for seeds
    file: Optional[str] = None
    line: Optional[int] = None
    signature: Optional[str] = None
    callers: List[str] = field(default_factory=list)
    # Shortest static path from an existing fuzz target's entry point, or None
    # when no target in the tree reaches this function. None is a real finding:
    # it means the harness has to open its own route in.
    entry_path: List[str] = field(default_factory=list)
    entry_reachable: bool = False
    # True when the route-in search was truncated, so the absence of a path is
    # not evidence of its absence. Without this the prompt would tell the model
    # "no existing target reaches it — open your own route in" on the strength
    # of a search that simply stopped early.
    entry_unknown: bool = False
    # `static` / anonymous-namespace: not callable from a separate translation
    # unit at all, so it is a place to REACH, never a function to call.
    internal: bool = False

    def label(self) -> str:
        """One line for the prompt's region list."""
        head = self.name
        if self.signature:
            # The declaration is the local text, so for a C++ member it reads
            # `int Read(...)` while the index key is `fmt::Reader::Read`. Show
            # both when they differ: the qualified name is what the region and
            # the coverage report key on, the declaration is what to call.
            head = (self.signature if self.name in self.signature
                    else f"{self.name} — {self.signature}")
        notes: List[str] = [self.tier if self.tier != "callee"
                            else f"depth {self.depth}"]
        if self.file and self.line:
            notes.append(f"{self.file}:{self.line}")
        if self.internal:
            notes.append("internal linkage — reach it, do not call it")
        if not self.entry_reachable:
            notes.append("reachability from an existing target not determined"
                         if self.entry_unknown
                         else "no existing target reaches it")
        return f"{head}  [{', '.join(notes)}]"


@dataclass
class PatchContext:
    patch_text: str
    functions: List[TouchedFunction] = field(default_factory=list)
    root_cause_reachable: List[str] = field(default_factory=list)
    language: str = "c++"
    headers: List[str] = field(default_factory=list)
    reachable_source: str = "heuristic"   # 'code-index' | 'heuristic'
    # Source files the fix touched that were deliberately not mined for
    # root-cause functions (tests/harnesses/tools) — reported so a surprising
    # empty context is explainable rather than mysterious.
    skipped_paths: List[str] = field(default_factory=list)
    # The annotated region. Same membership and order as
    # ``root_cause_reachable``, which stays a plain name list so the shared
    # variant block and the artifacts record keep working unchanged.
    reachable: List[ReachableFn] = field(default_factory=list)
    # Functions the ORIGINAL crash stack named that the fix did not touch,
    # resolved to source. The most direct evidence of where the fault lives, and
    # previously present in the prompt as three bare names.
    frame_functions: List[TouchedFunction] = field(default_factory=list)
    # One line about what the index cost and where it was capped, or None when
    # no index was built. Recorded, not just printed: a capped index is the one
    # way an empty-looking neighbourhood can be an artefact of the budget.
    index_stats: Optional[str] = None
    # How many touched/crash-frame seeds the index could actually resolve, out
    # of how many we had. 'code-index' with 0/5 resolved is not a working
    # analysis, and the old single-word label could not say so.
    seeds_resolved: Tuple[int, int] = (0, 0)

    @property
    def entry_points(self) -> List[str]:
        """Names of root-cause functions an existing fuzz target already
        reaches — the cheap places to start."""
        return [r.name for r in self.reachable if r.entry_reachable]

    def as_dict(self) -> dict:
        return {
            "language": self.language,
            "functions": [f.name for f in self.functions],
            "files": sorted({f.file for f in self.functions}),
            "headers": self.headers,
            "reachable": self.root_cause_reachable,
            "reachable_source": self.reachable_source,
            "skipped_non_library": self.skipped_paths,
            "crash_frame_functions": [f.name for f in self.frame_functions],
            "index_stats": self.index_stats,
            "seeds_resolved": list(self.seeds_resolved),
            "reachable_detail": [
                {"name": r.name, "tier": r.tier, "depth": r.depth,
                 "file": r.file, "line": r.line, "signature": r.signature,
                 "callers": r.callers, "entry_path": r.entry_path,
                 "entry_reachable": r.entry_reachable,
                 "entry_unknown": r.entry_unknown, "internal": r.internal}
                for r in self.reachable],
        }


def covered_keys(covered: Iterable[str],
                 reachable: Iterable[str]) -> List[str]:
    """Which region entries a set of *observed* function names actually hits.

    The names come from a sanitizer stack (``crash_evidence.reached_functions``)
    and the region keys come from the static index, and the two spellings do not
    always agree: ASan prints a demangled ``ns::Foo::bar``, tree-sitter may have
    indexed ``Foo::bar``, and C static functions match bare either way. Matching
    on the trailing segment as well as the whole name is what stops the coverage
    half of the steering from reading as "nothing covered" on every C++ target.
    """
    region = list(reachable)
    tails: Dict[str, List[str]] = {}
    for name in region:
        tails.setdefault(name.rsplit("::", 1)[-1], []).append(name)
    exact = set(region)
    hit: List[str] = []
    for name in covered:
        if not name:
            continue
        if name in exact and name not in hit:
            hit.append(name)
            continue
        for candidate in tails.get(name.rsplit("::", 1)[-1], []):
            if candidate not in hit:
                hit.append(candidate)
    return hit


class DiffAnalyzer:
    def __init__(self, language: str = "c++",
                 reachable_node_cap: Optional[int] = None,
                 reachable_max_depth: Optional[int] = None):
        self.language = language
        self.reachable_node_cap = (reachable_node_cap
                                   if reachable_node_cap is not None
                                   else config.REACHABLE_NODE_CAP)
        self.reachable_max_depth = (reachable_max_depth
                                    if reachable_max_depth is not None
                                    else config.REACHABLE_MAX_DEPTH)
        # The index is built once per checkout and kept, so a caller that learns
        # something new about the crash later can re-run the analysis without
        # paying for the parse again. run.py does exactly that: the diff analysis
        # has to happen BEFORE the Docker image is pulled (a target we cannot
        # steer should not cost gigabytes), but the PoC can only be replayed
        # AFTER, so the verified crash stack arrives one step too late to have
        # seeded the region on the first pass.
        self._index_cache: Dict[str, Optional[CodeIndex]] = {}

    def analyze(self, patch_text: str, vuln_dir: str,
                crash_frames: Optional[Sequence[str]] = None,
                crash_locations: Optional[Dict[str, Tuple[str, int]]] = None
                ) -> PatchContext:
        """``crash_frames`` are the original crash's function names, innermost
        first (from ``crash_evidence.CrashEvidence.names``). They seed the
        neighbourhood next to the diff and are resolved to source, because the
        frame that faulted is not reliably a function the fix touched.

        ``crash_locations`` maps a frame name to the ``(file, line)`` the
        sanitizer printed for it, used only when the symbol fails to resolve —
        a `static` helper the index recorded under a name the linker mangled, or
        a frame reported inside a macro. Only available for an *observed* crash;
        the OSV prose carries names alone.
        """
        touched: List[TouchedFunction] = []
        headers: set = set()
        skipped: List[str] = []
        for path, changed_lines in self._changed_lines(patch_text).items():
            is_hdr = self._is_header_file(path)
            if not self._is_source(path) and not is_hdr:
                continue
            if self._is_non_library(path):
                skipped.append(path)
                continue
            if is_hdr:
                headers.add(os.path.basename(path))
                # ...and fall through: headers are mined for functions too. In
                # C++ a great deal of real code is inline in headers (templates,
                # small methods), so skipping them loses genuine fixes — assimp
                # OSV-2026-505 fixes only include/assimp/StreamReader.h, and
                # treating headers as declaration-only rejected it as "touches
                # no C/C++ function". Prototypes are not mistaken for
                # definitions: _is_header rejects lines ending in ';'.
            abs_path = os.path.join(vuln_dir, path)
            lines = self._read(abs_path)
            if not lines:
                continue
            seen_starts = set()
            for ln in changed_lines:
                fn = self._enclosing_function(lines, ln)
                if fn and fn.start_line not in seen_starts:
                    seen_starts.add(fn.start_line)
                    touched.append(TouchedFunction(
                        file=path, name=fn.name,
                        source=fn.source, start_line=fn.start_line))

        touched = self._dedupe(touched)
        index = self._index_safe(vuln_dir)

        # The crash stack, resolved to real functions with real source. Only the
        # frames the fix did NOT touch: the touched ones are already carried in
        # full, and repeating them would spend context saying the same thing.
        frames = [f for f in (crash_frames or []) if f]
        touched_names = {fn.name for fn in touched}
        locs = crash_locations or {}
        frame_fns = self._frame_functions(frames, touched_names, index,
                                         vuln_dir, locs)

        region = self._region(touched, frames, index, locs)
        if region:
            source = "code-index"
            resolved = sum(
                1 for n in list(touched_names) + frames
                if index is not None and index.resolve(n) is not None)
            seeds = (resolved, len(touched_names) + len(frames))
        else:
            # No index, or an index that resolved none of our seeds. The
            # heuristic is strictly more informative than a bare seed list.
            region = [ReachableFn(name=n, tier="touched")
                      for n in self._reachable_heuristic(touched)]
            source = "heuristic"
            seeds = (0, len(touched_names) + len(frames))

        return PatchContext(
            patch_text=patch_text, functions=touched,
            root_cause_reachable=[r.name for r in region],
            language=self.language,
            headers=sorted(headers), reachable_source=source,
            skipped_paths=sorted(skipped),
            reachable=region, frame_functions=frame_fns,
            index_stats=(index.stats.describe() if index else None),
            seeds_resolved=seeds,
        )

    @staticmethod
    def _dedupe(touched: List[TouchedFunction]) -> List[TouchedFunction]:
        """Collapse same-named functions to one entry.

        A broad upstream sync can touch several files that each define e.g.
        ``main``; without this the prompt splices four near-identical function
        blocks and the steering list repeats the name. Keeps the first (largest
        body wins ties by arriving first) and preserves order.
        """
        out: List[TouchedFunction] = []
        seen = set()
        for fn in touched:
            # A CLI entry point is not a reachability goal: a libFuzzer harness
            # cannot call main(), and OSS-Fuzz forbids it defining one.
            if fn.name == "main":
                continue
            # '?' is _enclosing_function's placeholder for a header it could not
            # name (macro-generated definitions, K&R style). An unnameable
            # function cannot be a steering target — "steer toward ?" is noise,
            # and the changed lines are in the diff regardless.
            if not fn.name or fn.name == "?":
                continue
            key = fn.name or f"?@{fn.file}:{fn.start_line}"
            if key in seen:
                continue
            seen.add(key)
            out.append(fn)
        return out

    # -- diff parsing ------------------------------------------------------
    def _changed_lines(self, patch_text: str) -> dict:
        """Map new-file path -> set of changed (added/context-at-change) line
        numbers in the *new* (post-fix) file. We map these onto the vulnerable
        checkout too; line drift is usually small and the brace-match tolerates
        it because we search outward from the hit."""
        out: dict = {}
        cur_file = None
        new_ln = 0
        for line in patch_text.splitlines():
            if line.startswith("+++ "):
                p = line[4:].strip()
                p = p[2:] if p.startswith("b/") else p
                cur_file = None if p == "/dev/null" else p
                out.setdefault(cur_file, set())
                continue
            if line.startswith("@@"):
                m = _HUNK_RE.match(line)
                new_ln = int(m.group(1)) if m else 0
                continue
            if cur_file is None:
                continue
            if line.startswith("+") and not line.startswith("+++"):
                out[cur_file].add(new_ln)
                new_ln += 1
            elif line.startswith("-") and not line.startswith("---"):
                out[cur_file].add(new_ln)  # anchor: changed region
            elif not line.startswith("\\"):
                new_ln += 1
        out.pop(None, None)
        return out

    # -- function extraction ----------------------------------------------
    def _enclosing_function(self, lines: List[str], target_ln: int):
        """Find the function header at/above target_ln and brace-match its body."""
        idx = min(max(target_ln - 1, 0), len(lines) - 1)
        header = None
        for i in range(idx, max(idx - 400, -1), -1):
            if self._is_header(lines, i):
                header = i
                break
        if header is None:
            return None
        name = self._func_name(lines[header])
        # find the '{' then match braces
        depth = 0
        started = False
        end = header
        for j in range(header, min(header + 2000, len(lines))):
            depth += lines[j].count("{") - lines[j].count("}")
            if "{" in lines[j]:
                started = True
            if started and depth <= 0:
                end = j
                break
        src = "\n".join(lines[header:end + 1])

        class _F:  # tiny holder
            pass
        f = _F()
        f.name = name or "?"
        f.source = src
        f.start_line = header + 1
        return f

    def _is_header(self, lines: List[str], i: int) -> bool:
        line = lines[i]
        stripped = line.strip()
        if not stripped or stripped.startswith(("#", "//", "*", "/*", "}")):
            return False
        # Header heuristics: a name'(' with the line (or the next) opening a
        # brace, and the leading token not a control keyword.
        m = _CALL_RE.search(stripped)
        if not m or m.group(1) in _CONTROL:
            return False
        opens_here = stripped.endswith("{") or stripped.endswith(")")
        next_opens = (i + 1 < len(lines) and lines[i + 1].strip().startswith("{"))
        # Reject obvious statements (call ending in ';').
        if stripped.endswith(";"):
            return False
        return opens_here or next_opens

    def _func_name(self, header_line: str) -> Optional[str]:
        # The function name is the identifier immediately before the '('.
        before_paren = header_line.split("(", 1)[0]
        toks = re.findall(r"[A-Za-z_][A-Za-z0-9_:]*", before_paren)
        return toks[-1] if toks else None

    # -- the root-cause region (code index, with heuristic fallback) --------
    def _resolve_frame(self, index: CodeIndex, name: str,
                       locs: Dict[str, Tuple[str, int]]) -> Optional[FuncNode]:
        """A crash frame's function: by symbol, else by the location printed
        beside it. See ``analyze``'s ``crash_locations``."""
        node = index.resolve(name)
        if node is not None:
            return node
        where = locs.get(name)
        return index.resolve_at(where[0], where[1]) if where else None

    def _region(self, touched: List[TouchedFunction],
                frames: Sequence[str],
                index: Optional[CodeIndex],
                locs: Optional[Dict[str, Tuple[str, int]]] = None
                ) -> List[ReachableFn]:
        """The root-cause neighbourhood, ranked and annotated.

        Four tiers, in the order a harness author would want them:

          ``crash-frame``  functions the original report's stack named. The most
                           specific statement available about where the fault is,
                           and the reason the diff alone is not enough.
          ``touched``      functions the fix changed. Kept IN the region — unlike
                           the Java path, which drops them because their bodies
                           are already in the prompt — because "has any accepted
                           harness actually entered the patched function?" is the
                           first question the coverage map has to answer, and a
                           region that excludes them can never answer it.
          ``route-in``     callers of the above: the ways execution gets there.
          ``callee``       what the root cause reaches, by BFS depth.

        Ranking matters because the list is truncated at
        ``MAX_REACHABLE_IN_PROMPT``. Unranked, what survived the cut was
        whichever functions the BFS happened to reach first; ranked, the cut
        falls at the far end of the neighbourhood where it belongs. Within a
        tier, functions an existing fuzz target already reaches come first
        (they are cheap to hit), then externally callable ones.
        """
        if index is None:
            return []
        locs = locs or {}
        out: List[ReachableFn] = []
        seen: set = set()
        touched_keys = set()
        for fn in touched:
            node = index.resolve(fn.name)
            touched_keys.add(node.name if node else fn.name)

        def add(name: str, tier: str, depth: int) -> None:
            node = (self._resolve_frame(index, name, locs)
                    if tier == "crash-frame" else index.resolve(name))
            key = node.name if node else name
            if not key or key in seen:
                return
            # Harness code is not a steering target. It is the thing we are
            # writing: "drive execution into LLVMFuzzerTestOneInput" is not a
            # reachability goal, and the entry point turns up here naturally as
            # the caller of anything an existing target reaches. Its useful form
            # is the route (entry_path), not a region entry.
            if node is not None and (node.in_harness_file
                                     or node.short_name == FUZZ_ENTRY):
                return
            seen.add(key)
            # Both at once is the highest-value entry in the region: the fix
            # changed this function AND it is where the original bug faulted.
            # Collapsing that to one tier throws away half of why it is here.
            if tier == "crash-frame" and key in touched_keys:
                tier = "crash-frame+touched"
            out.append(self._annotate(key, node, tier, depth, index))

        for name in frames:
            add(name, "crash-frame", 0)
        for fn in touched:
            add(fn.name, "touched", 0)
        if not seen:
            # Nothing resolved: the index exists but knows none of our seeds
            # (heavily macro-generated code, or a checkout whose sources we
            # capped away). Saying so beats shipping callee noise from nothing.
            return []

        # Ordered, NOT `list(seen)`: set iteration order varies between
        # interpreter runs, which would make the route-in ordering and the BFS
        # seed order — and therefore the prompt, and therefore the run —
        # irreproducible for no reason.
        seeds = [r.name for r in out]
        for name in seeds:
            for caller in index.callers_of(name,
                                           config.MAX_CALLERS_IN_PROMPT):
                add(caller, "route-in", 0)
        for name, depth in index.callees_bfs(seeds, self.reachable_node_cap,
                                             self.reachable_max_depth):
            add(name, "callee", depth)

        # Callees the static graph missed but the touched source plainly names
        # (through a macro or a function pointer). Same recovery the previous
        # implementation did with _source_callees, kept because it is cheap and
        # the tree-sitter graph is not complete.
        for fn in touched:
            for m in _CALL_RE.finditer(fn.source or ""):
                ident = m.group(1)
                if ident in _CONTROL or ident in _LIBC_NAMES:
                    continue
                if index.resolve(ident) is not None:
                    add(ident, "callee", 1)

        tier_rank = {"crash-frame+touched": 0, "crash-frame": 1, "touched": 2,
                     "route-in": 3, "callee": 4}
        out.sort(key=lambda r: (tier_rank.get(r.tier, 9), r.depth,
                                not r.entry_reachable, r.internal))
        return out

    def _annotate(self, key: str, node: Optional[FuncNode], tier: str,
                  depth: int, index: CodeIndex) -> ReachableFn:
        path = index.path_from_entry(key, config.MAX_ENTRY_PATH_FRAMES) or []
        return ReachableFn(
            name=key, tier=tier, depth=depth,
            file=(node.file or None) if node else None,
            line=(node.start_line or None) if node else None,
            signature=node.signature() if node else None,
            callers=index.callers_of(key, config.MAX_CALLERS_IN_PROMPT),
            entry_path=path, entry_reachable=bool(path),
            entry_unknown=not path and not index.entry_search_complete,
            internal=bool(node.internal) if node else False,
        )

    def _frame_functions(self, frames: Sequence[str], touched_names: set,
                         index: Optional[CodeIndex], vuln_dir: str,
                         locs: Optional[Dict[str, Tuple[str, int]]] = None
                         ) -> List[TouchedFunction]:
        """The crash stack's frames as source, innermost first.

        The OSV record gives three-odd bare names; the prompt used to print them
        as prose and stop there. With the index we know where each one is, so we
        can show the model the code that faulted — which for a fix in a caller is
        the only place the actual fault is visible.
        """
        if index is None:
            return []
        out: List[TouchedFunction] = []
        seen: set = set()
        for name in frames:
            node = self._resolve_frame(index, name, locs or {})
            if node is None or node.name in seen:
                continue
            if node.name in touched_names or node.short_name in touched_names:
                continue          # already in the prompt in full
            seen.add(node.name)
            src = self._read_span(os.path.join(vuln_dir, node.file),
                                  node.start_line, node.end_line)
            if not src:
                continue
            out.append(TouchedFunction(file=node.file, name=node.name,
                                       source=src, start_line=node.start_line,
                                       origin="crash-frame"))
        return out

    def _read_span(self, path: str, start: int, end: int,
                   max_lines: int = 200) -> str:
        lines = self._read(path)
        if not lines or start <= 0:
            return ""
        stop = min(max(end, start), start + max_lines, len(lines))
        return "\n".join(lines[start - 1:stop])

    def _index_safe(self, vuln_dir: str) -> Optional[CodeIndex]:
        """Build the code index, degrading to None on anything at all.

        The timeout is SIGALRM-based and therefore main-thread only; off the main
        thread ``signal.signal`` raises and we land in the same except branch, so
        the failure mode is "no index" rather than "no run".
        """
        if vuln_dir in self._index_cache:
            return self._index_cache[vuln_dir]
        index = self._build_index_safe(vuln_dir)
        self._index_cache[vuln_dir] = index
        return index

    def _build_index_safe(self, vuln_dir: str) -> Optional[CodeIndex]:
        if not FRONTENDS_AVAILABLE:
            print("  fuzz-introspector not installed; using the heuristic "
                  "root-cause region (install with: uv sync --extra "
                  "introspector)")
            return None
        lang = "c" if self.language.lower() == "c" else "c++"
        try:
            index = self._with_timeout(
                lambda: build_index(vuln_dir, lang),
                config.INTROSPECTOR_TIMEOUT_SECONDS)
        except Exception as exc:
            print(f"  code index failed/timed out ({exc}); using the "
                  "heuristic root-cause region")
            return None
        if index is None:
            print("  code index produced no functions; using the heuristic "
                  "root-cause region")
            return None
        print(f"  code index: {index.stats.describe()}")
        return index

    def _reachable_heuristic(self, touched: List[TouchedFunction]) -> List[str]:
        """Original brace-match heuristic: touched names + every ``ident(``
        in their bodies. No project/libc distinction (that needs the call
        graph), so it is noisier than the introspector path but never fails."""
        names: List[str] = []
        seen = set()
        for fn in touched:
            if fn.name not in seen:
                seen.add(fn.name)
                names.append(fn.name)
        for fn in touched:
            for m in _CALL_RE.finditer(fn.source):
                callee = m.group(1)
                # libc/stdlib calls are not steering targets: "drive execution
                # into strlen" says nothing about the project. The introspector
                # path drops these via the call graph (a name absent from the
                # project index); the heuristic has no graph, so it needs an
                # explicit denylist to avoid the same noise.
                if (callee in _CONTROL or callee in seen
                        or callee in _LIBC_NAMES):
                    continue
                seen.add(callee)
                names.append(callee)
        return names

    @staticmethod
    def _with_timeout(fn, seconds):
        """Run ``fn()`` but abort after ``seconds`` via SIGALRM (main thread
        only; off the main thread signal.signal raises and the caller's
        try/except degrades to the heuristic)."""
        if not seconds or seconds <= 0:
            return fn()
        import signal

        def _handler(signum, frame):
            raise TimeoutError("the code index exceeded its budget")
        old = signal.signal(signal.SIGALRM, _handler)
        signal.alarm(int(seconds))
        try:
            return fn()
        finally:
            signal.alarm(0)
            signal.signal(signal.SIGALRM, old)

    # -- io ----------------------------------------------------------------
    def _is_source(self, path: str) -> bool:
        # '.C' is upper-case C++, not a case-insensitive spelling of '.c': it is
        # what Wt names every one of its sources, so without it that project's
        # only usable record was rejected as "touches no C/C++ function" over a
        # diff of 773 lines of C++ (20260812).
        return path.endswith((".c", ".cc", ".cpp", ".cxx", ".c++", ".C"))

    def _is_header_file(self, path: str) -> bool:
        return path.endswith((".h", ".hpp", ".hh", ".hxx", ".inl", ".ipp"))

    def _is_non_library(self, path: str) -> bool:
        """True for tests, existing fuzz harnesses, CLI tools and bindings.

        These are not root-cause material even when a fix touches them, and
        including them actively degrades steering: capstone's newest record
        touches four different ``main`` functions plus the project's own
        ``LLVMFuzzerTestOneInput``, and "steer toward LLVMFuzzerTestOneInput"
        is not a reachability goal — it is the thing we are writing. The
        library code the fix touched is what we want to reach.
        """
        parts = {p.lower() for p in path.replace("\\", "/").split("/")[:-1]}
        return bool(parts & _NON_LIBRARY_DIRS)

    def _read(self, path: str) -> List[str]:
        try:
            with open(path, errors="replace") as fh:
                return fh.read().splitlines()
        except (FileNotFoundError, IsADirectoryError):
            return []