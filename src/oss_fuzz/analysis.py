"""Turn a fix diff into the root-cause context the prompt needs.

The Java pipeline runs fuzz-introspector for a real call graph. For a first
libFuzzer version we stay dependency-light: parse the unified diff for the
files and line ranges the fix touched, then pull the *enclosing* C/C++
function bodies out of the vulnerable checkout by brace-matching. The names
of those functions plus the callees mentioned in their bodies form a
best-effort "reachable set" that feeds the shared variant-analysis steering.

This is intentionally a heuristic (no full parse). It is good enough to point
the model at the right functions; the trigger gate — not the analysis — is
what actually decides whether a generated harness is valid.
"""
from __future__ import annotations

import os
import re
from dataclasses import dataclass, field
from typing import List, Optional

# Lines that end in ')' or '){' at column 0-ish and are NOT control flow are
# treated as function headers. Crude but effective for typical C/C++ style.
_CONTROL = {"if", "for", "while", "switch", "do", "else", "return",
            "sizeof", "case"}
_CALL_RE = re.compile(r"\b([A-Za-z_][A-Za-z0-9_]*)\s*\(")
_HUNK_RE = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@")


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

    def as_dict(self) -> dict:
        return {
            "language": self.language,
            "functions": [f.name for f in self.functions],
            "files": sorted({f.file for f in self.functions}),
            "headers": self.headers,
            "reachable": self.root_cause_reachable,
        }


class DiffAnalyzer:
    def __init__(self, language: str = "c++"):
        self.language = language

    def analyze(self, patch_text: str, vuln_dir: str) -> PatchContext:
        touched: List[TouchedFunction] = []
        headers: set = set()
        for path, changed_lines in self._changed_lines(patch_text).items():
            if not self._is_source(path):
                if path.endswith((".h", ".hpp", ".hh")):
                    headers.add(os.path.basename(path))
                continue
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

        reachable = self._reachable(touched)
        return PatchContext(
            patch_text=patch_text, functions=touched,
            root_cause_reachable=reachable, language=self.language,
            headers=sorted(headers),
        )

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

    def _reachable(self, touched: List[TouchedFunction]) -> List[str]:
        names: List[str] = []
        seen = set()
        for fn in touched:
            if fn.name not in seen:
                seen.add(fn.name)
                names.append(fn.name)
        for fn in touched:
            for m in _CALL_RE.finditer(fn.source):
                callee = m.group(1)
                if callee in _CONTROL or callee in seen:
                    continue
                seen.add(callee)
                names.append(callee)
        return names

    # -- io ----------------------------------------------------------------
    def _is_source(self, path: str) -> bool:
        return path.endswith((".c", ".cc", ".cpp", ".cxx", ".c++"))

    def _read(self, path: str) -> List[str]:
        try:
            with open(path, errors="replace") as fh:
                return fh.read().splitlines()
        except (FileNotFoundError, IsADirectoryError):
            return []
