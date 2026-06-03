"""Extract the patch + touched functions + cross-references from a
buggy Defects4J checkout.

Two complementary signals are used:

  1. A Java AST (javalang) finds the method declaration that
     physically contains each changed line. This replaces the old
     `identifier(` scrape that mostly picked up java.lang.* calls and
     missed the actually-relevant enclosing function entirely.

  2. fuzz-introspector is then used purely to enrich those enclosing
     methods with cross-references by name. It's good at xrefs and
     bad at "which function contains line N"; the AST is the inverse.
"""
import os
import re
from dataclasses import dataclass, field, asdict
from typing import Dict, List, Optional, Tuple

import javalang
try:
    from fuzz_introspector import commands as fi_commands
    _FI_AVAILABLE = True
except ImportError:
    fi_commands = None  # type: ignore[assignment]
    _FI_AVAILABLE = False


@dataclass
class TouchedFunction:
    """One project function that the patch touches, with its source and
    every call site fuzz-introspector found."""
    func_name: str
    func_signature: str
    func_source: str
    xrefs: List[str] = field(default_factory=list)
    # Statically reachable functions from this touched function, per
    # fuzz-introspector's call graph. This is the slice of the codebase
    # downstream of the root cause — the region where *sibling* bugs of
    # the same fault would live — so it is the coverage map the campaign
    # steers the harness set across.
    reachable: List[str] = field(default_factory=list)


@dataclass
class PatchContext:
    """Everything the prompt builder needs about a single patch."""
    modified_files: List[str]
    patch_text: str
    functions: List[TouchedFunction]
    # JVM package the touched code lives in (e.g.
    # 'com.google.javascript.jscomp'). None if we couldn't resolve it
    # from the modified files on disk. The harness is asked to declare
    # this package so it can reach package-private members without
    # reflection.
    package: Optional[str] = None
    # Deduplicated union of every touched function's statically
    # reachable set: the full root-cause neighbourhood the harness set
    # should collectively cover. Drives the variant-analysis prompt
    # block. Empty if fuzz-introspector produced no reachability data.
    root_cause_reachable: List[str] = field(default_factory=list)

    # Import statements from the modified source files, read verbatim.
    # Gives the LLM the correct package paths for types like Range and
    # RectangleEdge that it otherwise consistently guesses wrong.
    source_imports: List[str] = field(default_factory=list)

    def as_dict(self) -> dict:
        return asdict(self)


class TargetAnalyzer:
    """Parse a patch, find the method(s) that physically contain the
    changed lines via a Java AST, and ask fuzz-introspector for their
    cross-references."""

    # `@@ -old_start[,old_len] +new_start[,new_len] @@`
    _HUNK_RE = re.compile(r'^@@\s+-(\d+)(?:,(\d+))?\s+\+\d+')
    _PACKAGE_RE = re.compile(r'^\s*package\s+([\w.]+)\s*;')

    def __init__(self, language: str = 'jvm'):
        self.language = language

    def analyze(self, patch_path: str, buggy_dir: str) -> PatchContext:
        modified_files, hunks_by_file, patch_text = self._parse_patch(
            patch_path,
        )
        package = self._resolve_package(modified_files, buggy_dir)

        # AST-based enclosing-method resolution. Far more reliable
        # than scraping `identifier(` from diff lines, which mostly
        # picked up java.lang String/Object calls rather than project
        # methods.
        enclosing = self._enclosing_methods(hunks_by_file, buggy_dir)

        # fuzz-introspector still earns its keep for xrefs by name —
        # we run it once and look up each enclosing method.
        project = self._light_project_safe(buggy_dir)
        functions = self._enrich_with_xrefs(enclosing, project)

        # Aggregate the reachable neighbourhood of the root cause across
        # all touched functions, preserving first-seen order and
        # dropping the touched functions themselves (the prompt already
        # shows those in full).
        touched_names = {fn.func_name for fn in functions}
        root_cause_reachable: List[str] = []
        seen_reach: set = set()
        for fn in functions:
            for name in fn.reachable:
                short = self._short_name(name)
                if short in touched_names or short in seen_reach:
                    continue
                seen_reach.add(short)
                root_cause_reachable.append(short)

        return PatchContext(
            modified_files=modified_files,
            patch_text=patch_text,
            functions=functions,
            package=package,
            root_cause_reachable=root_cause_reachable,
            source_imports=self._resolve_imports(modified_files, buggy_dir),
        )

    # --- patch parsing ---------------------------------------------------

    def _parse_patch(
        self, patch_path: str,
    ) -> Tuple[List[str], Dict[str, List[int]], str]:
        """Collect modified file paths and the buggy-side start line of
        every hunk in each. We key off the `-` start because
        `buggy_dir` is the buggy version — that's the file the AST
        will be parsed from, so its line numbers are the ones we need
        to map back to methods."""
        modified_files: List[str] = []
        hunks_by_file: Dict[str, List[int]] = {}
        current_file: Optional[str] = None

        with open(patch_path) as fh:
            for line in fh:
                m = re.match(r'^---\s+(?:a/)?(\S+)', line)
                if m and m.group(1) != '/dev/null':
                    current_file = m.group(1).lstrip('/')
                    modified_files.append(current_file)
                    hunks_by_file.setdefault(current_file, [])
                    continue
                m = self._HUNK_RE.match(line)
                if m and current_file is not None:
                    hunks_by_file[current_file].append(int(m.group(1)))

        with open(patch_path) as fh:
            patch_text = fh.read()

        return modified_files, hunks_by_file, patch_text

    # --- AST: enclosing methods ------------------------------------------

    def _enclosing_methods(
        self,
        hunks_by_file: Dict[str, List[int]],
        buggy_dir: str,
    ) -> List[TouchedFunction]:
        """For every changed line, find the smallest method or
        constructor declaration that physically contains it.
        Deduplicates if multiple hunks fall inside the same method."""
        seen: set = set()
        out: List[TouchedFunction] = []

        for rel_path, starts in hunks_by_file.items():
            if not rel_path.endswith('.java') or not starts:
                continue
            full = os.path.join(buggy_dir, rel_path)
            if not os.path.isfile(full):
                continue
            try:
                with open(full) as fh:
                    source = fh.read()
            except OSError:
                continue
            try:
                tree = javalang.parse.parse(source)
            except (javalang.parser.JavaSyntaxError,
                    javalang.tokenizer.LexerError) as e:
                print(f"javalang could not parse {rel_path}: {e}")
                continue

            lines = source.splitlines()
            methods = self._collect_methods(tree, lines)

            for start_line in starts:
                m = self._smallest_containing(methods, start_line)
                if not m:
                    continue
                key = (rel_path, m['name'], m['start'])
                if key in seen:
                    continue
                seen.add(key)
                body = "\n".join(lines[m['start'] - 1:m['end']])
                out.append(TouchedFunction(
                    func_name=m['name'],
                    func_signature=m['signature'],
                    func_source=body,
                ))
        return out

    def _collect_methods(self, tree, lines: List[str]) -> List[dict]:
        """Every MethodDeclaration / ConstructorDeclaration in the
        compilation unit, with its [start_line, end_line] range
        computed by brace-matching from the declaration position.

        Brace-matching (rather than "next method - 1") is the right
        granularity because it handles nested classes, anonymous
        classes, and lambda bodies correctly: an inner method's range
        is strictly contained in its outer method's range, so
        `_smallest_containing` will prefer the inner one — which is
        what we want when a patch lands inside an anonymous class.
        """
        methods: List[dict] = []
        for _, node in tree:
            if not isinstance(
                node,
                (javalang.tree.MethodDeclaration,
                 javalang.tree.ConstructorDeclaration),
            ):
                continue
            if not node.position:
                continue
            start = node.position.line
            end = self._method_end_line(lines, start)
            methods.append({
                'name': node.name,
                'signature': self._format_method_sig(node),
                'start': start,
                'end': end,
            })
        return methods

    @staticmethod
    def _smallest_containing(methods: List[dict],
                             line: int) -> Optional[dict]:
        """Of every method whose [start, end] range contains `line`,
        return the one with the smallest range — that's the deepest
        (most-nested) declaration."""
        candidates = [m for m in methods if m['start'] <= line <= m['end']]
        if not candidates:
            return None
        return min(candidates, key=lambda m: m['end'] - m['start'])

    # --- AST: signature formatting ---------------------------------------

    def _format_method_sig(self, node) -> str:
        """Render a method or constructor signature in roughly the same
        form a human would write it — readable enough for the LLM to
        treat as authoritative. Not legal Java in every edge case
        (annotations, type-parameter bounds), but accurate for the
        name + parameters + throws clause, which is what we need."""
        is_ctor = isinstance(node, javalang.tree.ConstructorDeclaration)
        mods = " ".join(sorted(node.modifiers)) if node.modifiers else ""
        params = ", ".join(
            self._format_param(p) for p in (node.parameters or [])
        )
        throws = ""
        if getattr(node, 'throws', None):
            throws = " throws " + ", ".join(node.throws)
        if is_ctor:
            head = f"{mods} {node.name}({params})".strip()
        else:
            ret = (self._format_type(node.return_type)
                   if node.return_type else "void")
            head = f"{mods} {ret} {node.name}({params})".strip()
        # Collapse any double-spaces introduced by empty modifiers.
        return re.sub(r'\s+', ' ', f"{head}{throws}")

    def _format_param(self, p) -> str:
        t = self._format_type(p.type)
        is_varargs = bool(getattr(p, 'varargs', False))
        if not is_varargs and 'varargs' in (p.modifiers or set()):
            is_varargs = True
        if is_varargs:
            t = t + "..."
        return f"{t} {p.name}"

    def _format_type(self, t) -> str:
        if t is None:
            return "void"
        s = t.name
        args = getattr(t, 'arguments', None)
        if args:
            s += "<" + ", ".join(self._format_type_arg(a) for a in args) + ">"
        dims = getattr(t, 'dimensions', None) or []
        s += "[]" * len(dims)
        sub = getattr(t, 'sub_type', None)
        if sub:
            s += "." + self._format_type(sub)
        return s

    def _format_type_arg(self, arg) -> str:
        # Wildcards (`?` / `? extends Foo`) have arg.type == None.
        if arg.type is None:
            return "?"
        return self._format_type(arg.type)

    # --- brace-matching --------------------------------------------------

    @staticmethod
    def _method_end_line(lines: List[str], start_line: int) -> int:
        """Given a method declaration starting at 1-indexed
        `start_line`, return the 1-indexed line of its closing brace.

        Skips strings, char literals, line comments, and block comments
        so we don't get confused by braces inside them. Abstract /
        interface methods have no body — if we hit `;` at depth 0
        before the first `{`, we treat the declaration as ending on
        that line."""
        depth = 0
        found_open = False
        in_string = False
        in_char = False
        in_block_comment = False
        i = start_line - 1
        while i < len(lines):
            line = lines[i]
            j = 0
            in_line_comment = False
            while j < len(line):
                c = line[j]
                nxt = line[j + 1] if j + 1 < len(line) else ''
                if in_line_comment:
                    break
                if in_block_comment:
                    if c == '*' and nxt == '/':
                        in_block_comment = False
                        j += 2
                        continue
                    j += 1
                    continue
                if in_string:
                    if c == '\\':
                        j += 2
                        continue
                    if c == '"':
                        in_string = False
                    j += 1
                    continue
                if in_char:
                    if c == '\\':
                        j += 2
                        continue
                    if c == "'":
                        in_char = False
                    j += 1
                    continue
                if c == '/' and nxt == '/':
                    in_line_comment = True
                    break
                if c == '/' and nxt == '*':
                    in_block_comment = True
                    j += 2
                    continue
                if c == '"':
                    in_string = True
                    j += 1
                    continue
                if c == "'":
                    in_char = True
                    j += 1
                    continue
                if c == '{':
                    depth += 1
                    found_open = True
                elif c == '}':
                    depth -= 1
                    if found_open and depth == 0:
                        return i + 1
                elif c == ';' and not found_open and depth == 0:
                    # Abstract method / interface method declaration.
                    return i + 1
                j += 1
            i += 1
        return len(lines)

    # --- fuzz-introspector enrichment ------------------------------------

    def _light_project_safe(self, buggy_dir: str):
        """Wrap fuzz-introspector so a failure there doesn't take down
        the whole analysis — the enclosing-method body is still
        usable without xrefs."""
        if not _FI_AVAILABLE:
            print("fuzz-introspector not installed; continuing without xrefs "
                  "(install with: uv sync --extra introspector)")
            return None
        try:
            _, report = fi_commands.analyse_end_to_end(
                arg_language=self.language,
                target_dir=buggy_dir,
                module_only=True,
                dump_files=False,
            )
            return report['light-project']
        except Exception as e:
            print(f"fuzz-introspector failed ({e}); "
                  "continuing without xrefs")
            return None

    def _enrich_with_xrefs(
        self,
        functions: List[TouchedFunction],
        project,
    ) -> List[TouchedFunction]:
        """Look up each AST-resolved enclosing method by name in the
        fuzz-introspector project, attach its call sites (xrefs) and its
        statically reachable set. Failures here are non-fatal — the
        harness body alone is still useful."""
        if project is None:
            return functions
        for fn in functions:
            try:
                resolved = project.find_function_by_name(fn.func_name, True)
            except Exception:
                continue
            if not resolved:
                continue
            try:
                xrefs = project.get_cross_references_by_name(resolved.name)
                fn.xrefs = [x.function_source_code_as_text() for x in xrefs]
            except Exception:
                pass
            fn.reachable = self._reachable_of(resolved, project)
        return functions

    @staticmethod
    def _reachable_of(resolved, project) -> List[str]:
        """Pull the statically reachable function set for `resolved`.

        fuzz-introspector has exposed this under a few names across
        versions (a `functions_reached` attribute on the function
        profile, a `reached_by_functions`/`all_class_functions` shaped
        structure, or a project-level helper). We probe the common ones
        and degrade to [] rather than letting an API drift break the
        whole analysis — consistent with how the rest of this module
        treats fuzz-introspector as best-effort enrichment."""
        # 1) Most common: attribute directly on the function profile.
        for attr in ('functions_reached', 'reached_functions',
                     'functions_reached_by_function'):
            val = getattr(resolved, attr, None)
            if val:
                return TargetAnalyzer._as_name_list(val)
        # 2) Project-level helper keyed by function name.
        for meth in ('get_reached_functions_by_name',
                     'get_functions_reached_by_name'):
            fn = getattr(project, meth, None)
            if callable(fn):
                try:
                    val = fn(resolved.name)
                    if val:
                        return TargetAnalyzer._as_name_list(val)
                except Exception:
                    continue
        return []

    @staticmethod
    def _as_name_list(val) -> List[str]:
        """Normalise whatever the reachable-set accessor returned (list
        of str, list of function objects, or dict) into a list of
        names."""
        if isinstance(val, dict):
            val = list(val.keys())
        out: List[str] = []
        for item in val:
            if isinstance(item, str):
                out.append(item)
            else:
                name = getattr(item, 'name', None)
                if name:
                    out.append(name)
        return out

    @staticmethod
    def _short_name(name: str) -> str:
        """Reduce a fully-qualified / mangled fuzz-introspector function
        name to something readable for the prompt. JVM names often look
        like `[pkg.Class].method(args)` or `pkg.Class.method`; we keep
        the `Class.method` tail."""
        # Strip any bracketed receiver prefix d4j/JVM mangling adds.
        name = re.sub(r'^\[[^\]]*\]\.?', '', name).strip()
        # Drop argument lists.
        name = name.split('(')[0]
        parts = name.split('.')
        return '.'.join(parts[-2:]) if len(parts) >= 2 else name

    # --- package resolution ----------------------------------------------

    def _resolve_imports(self, modified_files: List[str],
                         buggy_dir: str) -> List[str]:
        """Read the import statements from every modified Java source
        file and return them deduplicated. These are injected into the
        prompt so the LLM uses the correct package paths for types like
        Range and RectangleEdge, which it otherwise guesses wrong
        every time (10/11 failures in Chart-13 came from this alone)."""
        _IMPORT_RE = re.compile(r'^\s*(import\s+[\w.]+\s*;)')
        seen: set = set()
        out: List[str] = []
        for rel_path in modified_files:
            if not rel_path.endswith('.java'):
                continue
            full_path = os.path.join(buggy_dir, rel_path)
            if not os.path.isfile(full_path):
                continue
            try:
                with open(full_path) as fh:
                    for line in fh:
                        m = _IMPORT_RE.match(line)
                        if m:
                            stmt = m.group(1).strip()
                            if stmt not in seen:
                                seen.add(stmt)
                                out.append(stmt)
            except OSError:
                continue
        return out

    def _resolve_package(self, modified_files: List[str],
                         buggy_dir: str) -> Optional[str]:
        """Read the `package X.Y.Z;` declaration from the first
        modified Java source file we can find on disk. This is the
        ground truth for where the harness should live so it can
        access package-private members without reflection.

        Returns None if no modified file is readable or none of them
        declares a package — in which case the prompt falls back to
        instructing the model to discover the package itself.
        """
        for rel_path in modified_files:
            if not rel_path.endswith('.java'):
                continue
            full_path = os.path.join(buggy_dir, rel_path)
            if not os.path.isfile(full_path):
                continue
            try:
                with open(full_path) as fh:
                    for line in fh:
                        m = self._PACKAGE_RE.match(line)
                        if m:
                            return m.group(1)
            except OSError:
                continue
        return None