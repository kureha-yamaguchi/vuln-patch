"""Extract the patch + touched functions + cross-references from a
buggy Defects4J checkout using fuzz-introspector."""
import os
import re
from dataclasses import dataclass, field, asdict
from typing import List, Optional, Set, Tuple

from fuzz_introspector import commands as fi_commands


@dataclass
class TouchedFunction:
    """One project function that the patch touches, with its source and
    every call site fuzz-introspector found."""
    func_name: str
    func_signature: str
    func_source: str
    xrefs: List[str] = field(default_factory=list)


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

    def as_dict(self) -> dict:
        return asdict(self)


class TargetAnalyzer:
    """Parse a patch, run fuzz-introspector on the buggy checkout, and
    resolve the source + cross-references of every project function the
    patch touches."""

    # `identifier(` — any Java call or declaration on a changed line.
    _JAVA_CALL_RE = re.compile(r'\b([A-Za-z_]\w*)\s*\(')

    def __init__(self, language: str = 'Java'):
        self.language = language

    def analyze(self, patch_path: str, buggy_dir: str) -> PatchContext:
        modified_files, candidate_names, patch_text = self._parse_patch(
            patch_path,
        )
        project = self._light_project(buggy_dir)
        functions = self._resolve_functions(project, candidate_names)
        package = self._resolve_package(modified_files, buggy_dir)
        return PatchContext(
            modified_files=modified_files,
            patch_text=patch_text,
            functions=functions,
            package=package,
        )

    # --- internals -------------------------------------------------------

    def _parse_patch(self, patch_path: str) -> Tuple[List[str], Set[str], str]:
        """Collect modified file paths and candidate function names from
        the patch. We look in two places:
          - the trailing context of `@@ ... @@` hunk headers (often the
            enclosing Java method signature when the patch was produced
            by git with `*.java diff=java`),
          - any `identifier(` on +/- changed lines (called or declared
            methods inside the change).
        """
        modified_files: List[str] = []
        candidate_names: Set[str] = set()

        with open(patch_path) as fh:
            for line in fh:
                m = re.match(r'^---\s+(?:a/)?(\S+)', line)
                if m and m.group(1) != '/dev/null':
                    modified_files.append(m.group(1))
                    continue
                if line.startswith('@@'):
                    tail = line.split('@@', 2)[-1]
                    candidate_names.update(self._JAVA_CALL_RE.findall(tail))
                    continue
                if line.startswith(('+', '-')) and not line.startswith(('+++', '---')):
                    candidate_names.update(self._JAVA_CALL_RE.findall(line[1:]))

        with open(patch_path) as fh:
            patch_text = fh.read()

        return modified_files, candidate_names, patch_text

    def _light_project(self, buggy_dir: str):
        """Analyse the buggy checkout once and return the light-project
        handle fuzz-introspector exposes for queries."""
        _, report = fi_commands.analyse_end_to_end(
            arg_language=self.language,
            target_dir=buggy_dir,
            module_only=True,
            dump_files=False,
        )
        return report['light-project']

    def _resolve_functions(self, project,
                           candidate_names: Set[str]) -> List[TouchedFunction]:
        """For each candidate the project actually knows, collect source
        + xrefs. find_function_by_name returns None for unknown names,
        which filters out language keywords and non-project identifiers."""
        functions: List[TouchedFunction] = []
        seen: Set[str] = set()
        for name in candidate_names:
            if name in seen:
                continue
            seen.add(name)
            fn = project.find_function_by_name(name, True)
            if not fn:
                continue
            xrefs = project.get_cross_references_by_name(fn.name)
            functions.append(TouchedFunction(
                func_name=fn.name,
                func_signature=fn.sig,
                func_source=fn.function_source_code_as_text(),
                xrefs=[x.function_source_code_as_text() for x in xrefs],
            ))
        return functions

    _PACKAGE_RE = re.compile(r'^\s*package\s+([\w.]+)\s*;')

    def _resolve_package(self, modified_files: List[str],
                         buggy_dir: str) -> Optional[str]:
        """Read the `package X.Y.Z;` declaration from the first modified
        Java source file we can find on disk. This is the ground truth
        for where the harness should live so it can access package-
        private members (e.g. Compiler.getOptions(),
        CompilerOptions.dependencyOptions in Closure) without reflection.

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