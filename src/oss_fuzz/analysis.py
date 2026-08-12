"""Turn a fix diff into the root-cause context the prompt needs.

Two complementary signals, mirroring ``src/java/analysis.py``:

  1. A unified-diff parse + brace-match finds the C/C++ function that
     physically contains each changed line (the inverse of a call graph:
     "which function is line N in").

  2. fuzz-introspector's *light* static analysis (tree-sitter, no build)
     supplies the call graph. For each touched function we do a bounded BFS
     over its immediate callees (``base_callsites``), capped by node count and
     depth, unioned with the callees its source actually names — resolved to
     project functions only, so libc/stdlib noise is dropped. This is the
     root-cause neighbourhood the shared variant block steers across.

If fuzz-introspector is unavailable or fails/times out, we fall back to the
original brace-match heuristic (touched names + every ``ident(`` in their
bodies). Either way the trigger gate — not the analysis — is what ultimately
decides whether a generated harness is valid, so a degraded reachable set
costs steering quality, not correctness.

Install the introspector extra the same way the Java path does:
``uv sync --extra introspector``.
"""
from __future__ import annotations

import os
import re
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple

import config

try:
    from fuzz_introspector.frontends import oss_fuzz as _fi_oss_fuzz
    _FI_AVAILABLE = True
except ImportError:
    _fi_oss_fuzz = None  # type: ignore[assignment]
    _FI_AVAILABLE = False

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


@dataclass
class PatchContext:
    patch_text: str
    functions: List[TouchedFunction] = field(default_factory=list)
    root_cause_reachable: List[str] = field(default_factory=list)
    language: str = "c++"
    headers: List[str] = field(default_factory=list)
    reachable_source: str = "heuristic"   # 'fuzz-introspector' | 'heuristic'
    # Source files the fix touched that were deliberately not mined for
    # root-cause functions (tests/harnesses/tools) — reported so a surprising
    # empty context is explainable rather than mysterious.
    skipped_paths: List[str] = field(default_factory=list)

    def as_dict(self) -> dict:
        return {
            "language": self.language,
            "functions": [f.name for f in self.functions],
            "files": sorted({f.file for f in self.functions}),
            "headers": self.headers,
            "reachable": self.root_cause_reachable,
            "reachable_source": self.reachable_source,
            "skipped_non_library": self.skipped_paths,
        }


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

    def analyze(self, patch_text: str, vuln_dir: str) -> PatchContext:
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
        reachable, source = self._reachable_set(touched, vuln_dir)
        return PatchContext(
            patch_text=patch_text, functions=touched,
            root_cause_reachable=reachable, language=self.language,
            headers=sorted(headers), reachable_source=source,
            skipped_paths=sorted(skipped),
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

    # -- reachable set (fuzz-introspector, with heuristic fallback) --------
    def _reachable_set(self, touched: List[TouchedFunction],
                       vuln_dir: str) -> Tuple[List[str], str]:
        """Root-cause neighbourhood + which analysis produced it.

        Seed with the touched functions, then expand via fuzz-introspector's
        static call graph (bounded BFS over ``base_callsites``) unioned with
        project-resolved source callees. Fall back to the brace-match
        heuristic when introspector is unavailable or adds nothing.
        """
        seeds: List[str] = []
        seen = set()
        for fn in touched:
            if fn.name and fn.name not in seen:
                seen.add(fn.name)
                seeds.append(fn.name)

        project = self._light_project_safe(vuln_dir)
        if project is None:
            return self._reachable_heuristic(touched), "heuristic"

        fmap = self._function_map(project)
        proj_names = set(fmap)
        names = list(seeds)
        for fn in touched:
            mangled = self._match_fi(fn.name, fmap)
            if mangled is None:
                continue
            bfs = self._bfs_callees(fmap, mangled,
                                    self.reachable_node_cap,
                                    self.reachable_max_depth)
            src = self._source_callees(fn.source, proj_names)
            for name in bfs + src:
                if name and name not in seen:
                    seen.add(name)
                    names.append(name)

        # If introspector resolved nothing beyond the seeds (e.g. name-match
        # miss on a heavily-namespaced C++ target), the heuristic is strictly
        # more informative — use it rather than ship a bare seed list.
        if len(names) <= len(seeds):
            return self._reachable_heuristic(touched), "heuristic"
        return names, "fuzz-introspector"

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

    # -- fuzz-introspector plumbing (mirrors src/java/analysis.py) ----------
    def _light_project_safe(self, vuln_dir: str):
        """Run introspector's light (tree-sitter, no-build) frontend on the
        vulnerable checkout, wrapped so a failure/timeout degrades to the
        heuristic rather than taking down the run."""
        if not _FI_AVAILABLE:
            print("fuzz-introspector not installed; using heuristic reachable "
                  "set (install with: uv sync --extra introspector)")
            return None
        lang = "c" if self.language.lower() == "c" else "c++"
        try:
            project, _ = self._with_timeout(
                lambda: _fi_oss_fuzz.analyse_folder(
                    language=lang, directory=vuln_dir,
                    module_only=True, dump_output=False),
                config.INTROSPECTOR_TIMEOUT_SECONDS,
            )
            return project
        except Exception as e:
            print(f"fuzz-introspector failed/timed out ({e}); "
                  "using heuristic reachable set")
            return None

    @staticmethod
    def _function_map(project) -> Dict[str, object]:
        """``project.all_functions`` normalised to name -> function object."""
        funcs = getattr(project, "all_functions", None) or []
        return {getattr(f, "name", ""): f
                for f in funcs if getattr(f, "name", "")}

    def _match_fi(self, name: str, fmap: Dict[str, object]) -> Optional[str]:
        """Resolve a touched function name to an introspector key. C keys are
        the bare name (exact hit); C++ keys may be ``ns::Class::method`` so we
        also accept a trailing-segment match."""
        if not name:
            return None
        if name in fmap:
            return name
        for k in fmap:
            if k.split("::")[-1] == name or k.endswith("::" + name):
                return k
        return None

    def _bfs_callees(self, fmap: Dict[str, object], start: str,
                     cap: int, max_depth: int) -> List[str]:
        """BFS the call graph from ``start`` via each function's
        ``base_callsites`` (immediate callees), bounded by ``cap`` nodes and
        ``max_depth`` levels — O(cap) regardless of graph size, so it cannot
        hang on hub functions. ``base_callsites`` entries are ``(name, line)``
        tuples in the C/C++ frontend; other shapes are handled defensively."""
        seen = {start}
        out: List[str] = []
        queue: List[Tuple[str, int]] = [(start, 0)]
        while queue and len(out) < cap:
            node, depth = queue.pop(0)
            prof = fmap.get(node)
            if prof is None:
                continue
            for cs in (getattr(prof, "base_callsites", None) or []):
                if isinstance(cs, (list, tuple)) and cs:
                    dst = cs[0]
                elif isinstance(cs, str):
                    dst = cs
                else:
                    dst = getattr(cs, "dst_function_name", None)
                # Keep project functions only: callees introspector knows as
                # defined functions (in fmap). libc/externals have no profile
                # and are not useful steering targets — drop them, matching
                # how the Java path drops JDK calls.
                if not dst or dst in seen or dst not in fmap:
                    continue
                seen.add(dst)
                out.append(dst)
                if len(out) >= cap:
                    break
                if depth + 1 < max_depth:
                    queue.append((dst, depth + 1))
        return out

    def _source_callees(self, source: Optional[str],
                        proj_names: set) -> List[str]:
        """Callees the function's source names, kept only if they are project
        functions (present in the introspector index). Recovers calls the
        static graph can miss (e.g. through function pointers/macros) while
        dropping libc/stdlib noise the way the Java path drops JDK calls."""
        if not source or not proj_names:
            return []
        suffix = {n.split("::")[-1]: n for n in proj_names}
        out: List[str] = []
        seen = set()
        for m in _CALL_RE.finditer(source):
            ident = m.group(1)
            if ident in _CONTROL:
                continue
            resolved = ident if ident in proj_names else suffix.get(ident)
            if resolved and resolved not in seen:
                seen.add(resolved)
                out.append(resolved)
        return out

    @staticmethod
    def _with_timeout(fn, seconds):
        """Run ``fn()`` but abort after ``seconds`` via SIGALRM (main thread
        only; off the main thread signal.signal raises and the caller's
        try/except degrades to the heuristic)."""
        if not seconds or seconds <= 0:
            return fn()
        import signal

        def _handler(signum, frame):
            raise TimeoutError("fuzz-introspector exceeded budget")
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