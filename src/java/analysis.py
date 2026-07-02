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
class RelatedCallee:
    """A method that a touched function *calls on or near the changed
    lines*, whose behaviour the patched code depends on.

    The motivating case: a patched method consumes the return value of a
    call it makes (e.g. `consumed = translate(input, pos, out)`), and the
    fault lives in how that returned value is used. The enclosing body
    alone never shows the callee's contract, so the model cannot reason
    about it. Surfacing the callee's declaration — and, when it is
    abstract/overridden, the concrete implementations visible in the same
    package — closes that gap. This is bug-type-agnostic: it is the same
    'give the model the context it provably needs' move as `xrefs` and
    `source_imports`, not logic specific to any one fault.
    """
    name: str
    # Where the declaration was found, for prompt attribution / debugging
    # (e.g. 'CharSequenceTranslator.java' or 'LookupTranslator.java').
    source_file: Optional[str] = None
    signature: Optional[str] = None
    # The declaration body if one was resolvable. For an abstract callee
    # this is the abstract declaration; concrete overrides go in `impls`.
    source: Optional[str] = None
    # True when the resolved declaration has no body (abstract/interface),
    # signalling that `impls` carries the behaviour that actually runs.
    is_abstract: bool = False
    # Concrete implementations of this callee found in the same package —
    # the bodies that actually execute at runtime. Each entry is
    # (source_file, body). Only populated for abstract/overridden callees.
    impls: List[Tuple[str, str]] = field(default_factory=list)


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
    # Methods called on/near the changed lines whose behaviour the patch
    # depends on, with their declarations and concrete implementations
    # resolved from the same package. See RelatedCallee. Best-effort and
    # purely additive: empty when nothing resolves.
    related_callees: List[RelatedCallee] = field(default_factory=list)


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
        proj_prefix = self._project_prefix(package)
        root_cause_reachable: List[str] = []
        seen_reach: set = set()
        for fn in functions:
            for name in fn.reachable:
                # Keep project functions; drop JDK noise (Double.isNaN,
                # Math.exp, Object.<init>, ...) that the call graph pulls
                # in but that isn't part of the root-cause neighbourhood.
                if proj_prefix and not self._is_project_fn(name, proj_prefix):
                    continue
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
                tf = TouchedFunction(
                    func_name=m['name'],
                    func_signature=m['signature'],
                    func_source=body,
                )
                # Additive enrichment: surface the methods this function
                # calls within its body whose contract the patch depends
                # on. Best-effort — any failure leaves related_callees [].
                try:
                    tf.related_callees = self._resolve_related_callees(
                        node=m.get('node'),
                        own_method_name=m['name'],
                        cu_tree=tree,
                        cu_source=source,
                        cu_rel_path=rel_path,
                        buggy_dir=buggy_dir,
                    )
                except Exception as e:  # never let enrichment break analysis
                    print(f"related-callee resolution failed for "
                          f"{rel_path}:{m['name']} ({e})")
                out.append(tf)
        return out

    # --- related callees (caller -> callee context) ----------------------

    # Cap how many distinct called methods we resolve per touched
    # function. A method that calls dozens of helpers should not flood
    # the prompt; the ones whose contract the patch depends on are few.
    _MAX_RELATED_CALLEES = 6
    # Cap concrete implementations surfaced per abstract callee, so a
    # widely-overridden interface method doesn't dump the whole hierarchy.
    _MAX_IMPLS_PER_CALLEE = 4

    def _resolve_related_callees(
        self,
        node,
        own_method_name: str,
        cu_tree,
        cu_source: str,
        cu_rel_path: str,
        buggy_dir: str,
    ) -> List[RelatedCallee]:
        """Find the methods called inside `node`'s body whose declaration
        exists in the same package, and return their signatures, bodies,
        and (for abstract/overridden ones) concrete implementations.

        Universal rationale: a patch's fault frequently depends on the
        contract of something the method *calls* — most sharply when the
        return value flows into the changed expression (the Lang_6 loop
        trusts the codepoint count returned by `translate(...)`). The
        enclosing body never shows that contract. We resolve only names
        that actually have a declaration in the project package, which
        naturally filters out java.lang/library calls without hard-coding
        any allowlist, keeping this agnostic to bug type and project.
        """
        if node is None:
            return []

        # 1) Distinct method names invoked inside this method's subtree,
        #    in first-seen order. Skip self-recursion — the body is
        #    already shown in full.
        called: List[str] = []
        seen: set = set()
        try:
            for _, inv in node.filter(javalang.tree.MethodInvocation):
                name = getattr(inv, 'member', None)
                if not name:
                    continue
                # NOTE: do NOT skip names equal to the enclosing method.
                # A method calling a *different overload* of itself is a
                # central case here (the final translate(CharSequence,
                # Writer) calls the abstract translate(CharSequence, int,
                # Writer)). We instead drop the enclosing method's own
                # declaration from the resolved set below, by signature.
                if name not in seen:
                    seen.add(name)
                    called.append(name)
        except Exception:
            return []

        # 2) Index every method declaration in the package directory on
        #    disk (plus the current compilation unit), by name. Built once
        #    per touched function; cheap relative to the rest of analysis.
        decl_index = self._package_method_index(
            cu_tree, cu_source, cu_rel_path, buggy_dir,
        )

        out: List[RelatedCallee] = []
        own_sig = self._enclosing_signature(node)
        for name in called:
            decls = decl_index.get(name)
            if not decls:
                # No declaration in the package → library/JDK call, not
                # useful root-cause context. Skip without an allowlist.
                continue
            # Drop the enclosing method's own declaration: it is already
            # shown in full as func_source. A same-name call then keeps
            # only the *other* overloads (e.g. the abstract translate),
            # which is exactly the context the body lacks.
            if own_sig is not None:
                decls = [d for d in decls
                         if self._sig_key(d['signature']) != own_sig]
            if not decls:
                continue
            # Prefer the abstract declaration as the 'primary' (its
            # Javadoc carries the contract); fall back to a concrete one.
            # Concrete impls — including overrides of the abstract — are
            # surfaced separately as the bodies that actually run.
            concrete = [d for d in decls if not d['is_abstract']]
            abstract = [d for d in decls if d['is_abstract']]
            primary = (abstract[0] if abstract else concrete[0])

            impls: List[Tuple[str, str]] = []
            if primary['is_abstract'] or len(concrete) > 1:
                for d in concrete[:self._MAX_IMPLS_PER_CALLEE]:
                    impls.append((d['source_file'], d['source']))

            out.append(RelatedCallee(
                name=name,
                source_file=primary['source_file'],
                signature=primary['signature'],
                source=primary['source'],
                is_abstract=primary['is_abstract'],
                impls=impls,
            ))
            if len(out) >= self._MAX_RELATED_CALLEES:
                break
        return out

    @staticmethod
    def _sig_key(signature: str) -> str:
        """Normalise a signature to name+param-arity-ish key for matching
        the enclosing method against the package index. We compare the
        method name and parenthesised parameter list textually, which is
        enough to distinguish overloads without resolving types."""
        if signature is None:
            return ""
        # Keep from the method name's '(' onward, plus the bare name.
        m = re.search(r'(\w+)\s*\(([^)]*)\)', signature)
        if not m:
            return signature.strip()
        name = m.group(1)
        # Param arity by counting top-level commas (0 params => arity 0).
        params = m.group(2).strip()
        arity = 0 if not params else params.count(',') + 1
        return f"{name}/{arity}"

    def _enclosing_signature(self, node) -> Optional[str]:
        """Signature key for the touched method itself, used to exclude
        its own declaration from its resolved callees."""
        try:
            return self._sig_key(self._format_method_sig(node))
        except Exception:
            return None

    def _package_method_index(
        self,
        cu_tree,
        cu_source: str,
        cu_rel_path: str,
        buggy_dir: str,
    ) -> Dict[str, List[dict]]:
        """Map method-name -> list of declarations found in the modified
        file's package directory (non-recursive: same-package siblings).

        Each declaration dict has source_file, signature, source (body),
        and is_abstract. Same-package scope is the right granularity: it
        is where overrides of an abstract callee live (LookupTranslator,
        CsvEscaper, AggregateTranslator all sit beside
        CharSequenceTranslator), and it bounds the work without a
        whole-project parse.
        """
        index: Dict[str, List[dict]] = {}

        pkg_dir = os.path.dirname(os.path.join(buggy_dir, cu_rel_path))
        if not os.path.isdir(pkg_dir):
            # Still index the current compilation unit so intra-file
            # callees resolve even when the dir walk is unavailable.
            self._index_unit(index, cu_tree, cu_source,
                             os.path.basename(cu_rel_path))
            return index

        for fname in sorted(os.listdir(pkg_dir)):
            if not fname.endswith('.java'):
                continue
            fpath = os.path.join(pkg_dir, fname)
            try:
                with open(fpath) as fh:
                    fsrc = fh.read()
                ftree = javalang.parse.parse(fsrc)
            except (OSError, javalang.parser.JavaSyntaxError,
                    javalang.tokenizer.LexerError):
                continue
            self._index_unit(index, ftree, fsrc, fname)
        return index

    def _index_unit(self, index: Dict[str, List[dict]],
                    tree, source: str, source_file: str) -> None:
        """Add every method declaration in one compilation unit to
        `index`, recording whether it is abstract (no body)."""
        lines = source.splitlines()
        for _, n in tree:
            if not isinstance(n, javalang.tree.MethodDeclaration):
                continue
            if not n.position:
                continue
            start = n.position.line
            end = self._method_end_line(lines, start)
            body = "\n".join(lines[start - 1:end])
            is_abstract = (getattr(n, 'body', None) is None) or \
                          ('abstract' in (n.modifiers or set()))
            index.setdefault(n.name, []).append({
                'source_file': source_file,
                'signature': self._format_method_sig(n),
                'source': body,
                'is_abstract': is_abstract,
            })

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
                'node': node,
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
        """Look up each AST-resolved enclosing method in the
        fuzz-introspector project and attach its statically reachable
        set (the root-cause neighbourhood the variant block steers
        across). Failures here are non-fatal — the harness body alone is
        still useful.

        Resolution is done ourselves against ``project.all_functions``
        rather than via ``find_function_by_name``: in the current JVM
        frontend that helper returns None (it expects a mangled name, not
        a bare method name), which previously left every reachable set
        empty. We map our AST method name to introspector's mangled name
        ``[pkg.Class].method(argtypes)`` — disambiguating overloads by
        argument count — and call the real reachability API."""
        if project is None:
            return functions
        index = self._build_fi_index(project)
        for fn in functions:
            mangled = self._match_fi_name(fn, index)
            if mangled is None:
                # Legacy fallback for older introspector versions whose
                # find_function_by_name accepted a bare name.
                try:
                    resolved = project.find_function_by_name(
                        fn.func_name, True)
                    mangled = getattr(resolved, 'name', None)
                except Exception:
                    mangled = None
            if not mangled:
                continue
            try:
                xrefs = project.get_cross_references_by_name(mangled)
                fn.xrefs = [x.function_source_code_as_text() for x in xrefs]
            except Exception:
                pass
            fn.reachable = self._reachable_of(project, mangled)
        return functions

    def _build_fi_index(self, project) -> Dict[str, List[str]]:
        """Map bare method name -> [introspector mangled names].

        introspector keys functions by mangled name
        (``[pkg.Class].method(argtypes)``); we index by the trailing
        method name so an AST-resolved ``func_name`` can find its
        overload(s)."""
        funcs = getattr(project, 'all_functions', None)
        if not funcs:
            return {}
        keys = (funcs.keys() if isinstance(funcs, dict)
                else [getattr(f, 'name', '') for f in funcs])
        index: Dict[str, List[str]] = {}
        for k in keys:
            if not k:
                continue
            index.setdefault(self._fi_method_name(k), []).append(k)
        return index

    def _match_fi_name(self, fn: TouchedFunction,
                       index: Dict[str, List[str]]) -> Optional[str]:
        """Resolve a touched function to its introspector mangled name,
        picking the overload whose argument count matches the AST
        signature when there is more than one."""
        cands = index.get(fn.func_name)
        if not cands:
            return None
        if len(cands) == 1:
            return cands[0]
        want = self._arg_count(fn.func_signature or '')
        for c in cands:
            if self._arg_count(c) == want:
                return c
        return cands[0]

    @staticmethod
    def _fi_method_name(mangled: str) -> str:
        """Bare method name from an introspector JVM name
        (``[pkg.Class].method(args)`` / ``Class.method(args)``)."""
        name = re.sub(r'^\[[^\]]*\]\.?', '', mangled).strip()
        name = name.split('(')[0]
        return name.split('.')[-1] if '.' in name else name

    @staticmethod
    def _arg_count(paren: str) -> int:
        """Number of comma-separated args inside the first (...) group,
        or -1 if there is none. Good enough to disambiguate overloads
        (generics with top-level commas are rare in these signatures)."""
        m = re.search(r'\(([^)]*)\)', paren)
        if not m:
            return -1
        inner = m.group(1).strip()
        return 0 if not inner else len([a for a in inner.split(',')
                                        if a.strip()])

    def _reachable_of(self, project, mangled: str) -> List[str]:
        """Transitive reachable-function set for a mangled name.

        Primary path is the current JVM API
        ``project.get_reachable_functions(function=<mangled>)``. We keep
        the legacy per-profile attribute probe as a fallback so an API
        drift degrades to [] rather than breaking the analysis."""
        getter = getattr(project, 'get_reachable_functions', None)
        if callable(getter):
            try:
                val = getter(function=mangled)
                if val:
                    return TargetAnalyzer._as_name_list(val)
            except Exception:
                pass
        try:
            resolved = project.find_function_by_name(mangled, True)
        except Exception:
            resolved = None
        if resolved is not None:
            for attr in ('functions_reached', 'reached_functions',
                         'functions_reached_by_function'):
                val = getattr(resolved, attr, None)
                if val:
                    return TargetAnalyzer._as_name_list(val)
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
    def _is_project_fn(mangled: str, proj_prefix: str) -> bool:
        """True if a reachable function belongs to the project, judged by
        its RECEIVER class (the ``[pkg.Class]`` bracket) rather than the
        whole mangled name. The JVM frontend sometimes mis-types call
        arguments with the project package, so a substring check over the
        full name lets JDK statics (``Math.abs(...)``) leak through; the
        receiver bracket is the reliable signal — JDK/static calls have
        none."""
        m = re.match(r'^\[([^\]]*)\]', mangled)
        receiver = m.group(1) if m else ''
        return proj_prefix in receiver

    @staticmethod
    def _project_prefix(package: Optional[str]) -> str:
        """Reverse-domain prefix used to keep reachable functions that
        belong to the project and drop JDK/library noise. Derived from
        the touched file's package: first three components (e.g.
        ``org.apache.commons`` from ``org.apache.commons.math.special``),
        which is specific enough to exclude java.* / third-party calls
        without dropping the project's own neighbouring code."""
        if not package:
            return ''
        parts = package.split('.')
        return '.'.join(parts[:3]) if len(parts) >= 3 else package

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