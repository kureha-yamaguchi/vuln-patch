"""What the original crash actually WAS, read off a real run instead of prose.

The Java front-end does not ask the model to infer the failure from the diff:
``bug_context/crash_input.py`` runs the trigger test on the buggy checkout and
captures what really happened — the throwable, its message, the throw site, the
literal input that caused it — and ``prompts.py`` labels that block "trust it
over anything inferred from the test body". This module is that station for
C/C++.

OSS-Fuzz publishes no test case, so the corpus's stand-in for a trigger test is
two things:

  * the OSV record's prose (``crash_type`` plus three-odd frame *names*), which
    is what the prompt used to carry on its own, and
  * the crash the *existing* fuzz target produces when the PoC is replayed on
    the vulnerable build — a full sanitizer report: the access, every frame with
    file and line, and the allocation stack for a heap bug.

``run.py`` already replayed the PoC as a sanity check and printed one line of
the result; everything below is about keeping it. When there is no PoC the OSV
prose is all there is and ``from_osv`` wraps it in the same shape, so the prompt
builder has one type to render either way and says which it got.

Nothing here imports the rest of the package (``ossfuzz`` imports *this* way
round), and every parser degrades to "no evidence" rather than raising: a report
we cannot read costs prompt quality, never the run.
"""
from __future__ import annotations

import os
import re
from dataclasses import dataclass, field
from typing import List, Optional

# One sanitizer stack frame. ASan prints C++ symbols demangled and with their
# argument lists — ``#1 0x5 in ns::Foo::bar(char const*, unsigned long)
# /src/p/x.cc:10:5`` — so the symbol cannot be scraped as one token; the
# location is peeled off the end instead (see _split_frame).
_FRAME_RE = re.compile(r"^\s*#(\d+)\s+0x[0-9a-fA-F]+\s+in\s+(.+?)\s*$")
# ``/src/p/x.cc:10:5`` or ``x.cc:10``. The column is optional and dropped.
_LOC_RE = re.compile(r"^(?P<file>[^\s:]+):(?P<line>\d+)(?::\d+)?$")

# The sanitizer's own name for what went wrong. Both spellings appear: ERROR
# opens the report, SUMMARY closes it, and a leak report only has the latter.
_TYPE_RES = (
    re.compile(r"ERROR:\s*(?:Address|Memory|Leak|UndefinedBehavior)Sanitizer:"
               r"\s*([A-Za-z0-9 _\-]+)"),
    re.compile(r"SUMMARY:\s*(?:Address|Memory|Leak|UndefinedBehavior)Sanitizer:"
               r"\s*([A-Za-z0-9 _\-]+)"),
    re.compile(r"ERROR:\s*libFuzzer:\s*([A-Za-z0-9 _\-]+)"),
    re.compile(r"runtime error:\s*([^\n]{1,120})"),
)
# Same trailing-address problem crash_signature has: "heap-buffer-overflow on
# address 0x7b9c..." is a different string every run.
_ADDRESSES_RE = re.compile(r"\s+(?:on (?:unknown )?address|at pc)\b.*$")

# ASan's one-line description of the faulting access, which says more about the
# bug than its type does: a 1-byte read past the end is a different fix from a
# 64-byte write.
_ACCESS_RE = re.compile(
    r"((?:READ|WRITE) of size \d+ at 0x[0-9a-fA-F]+[^\n]*)")

# Where the heap block came from. For a use-after-free or an overflow this is
# the other half of the story — the size the allocation was given is the
# invariant the fix had to restore.
_ALLOC_HEADERS = (
    "allocated by thread",
    "freed by thread",
    "previously allocated by",
)

# Frames that are not project code. A stack trace is mostly this, and none of
# it is a steering target or evidence of coverage.
_NOISE_PREFIXES = (
    "__asan", "__msan", "__ubsan", "__lsan", "__sanitizer", "__interceptor",
    "__libc", "__GI_", "__pthread", "__clone", "__cxa_", "__gxx_",
    "_start", "_init", "_fini",
    "fuzzer::", "LLVMFuzzerTestOneInput", "LLVMFuzzerRunDriver",
    "main", "start_thread", "clone", "abort", "raise", "operator new",
    "operator delete", "malloc", "calloc", "realloc", "free",
    "<null>", "(", "?",
)


@dataclass
class Frame:
    """One stack frame: the symbol, and where it is if the build had symbols."""
    index: int
    function: str
    file: Optional[str] = None
    line: Optional[int] = None

    @property
    def bare_name(self) -> str:
        """The symbol without its argument list — the shape the static index
        keys on (``ns::Foo::bar``, not ``ns::Foo::bar(char const*)``)."""
        return strip_args(self.function)

    def describe(self) -> str:
        where = _project_relative(self.file or "")
        if where and self.line:
            where = f"{where}:{self.line}"
        return f"{self.function}{f'  [{where}]' if where else ''}"


@dataclass
class CrashEvidence:
    """The original bug's failure, from the strongest source available.

    ``source`` is the honest label the prompt repeats: ``'observed'`` means we
    replayed a PoC on the vulnerable build and this is what the sanitizer
    printed; ``'osv'`` means it is the bug report's prose, which names frames
    but not lines and can be years stale.
    """
    source: str = "osv"                      # 'observed' | 'osv'
    crash_type: Optional[str] = None
    access: Optional[str] = None
    frames: List[Frame] = field(default_factory=list)
    alloc_frames: List[Frame] = field(default_factory=list)
    # Names only, for the case where that is all the record gave us.
    frame_names: List[str] = field(default_factory=list)
    # The PoC, when one was supplied locally. Bytes are not quoted verbatim into
    # the prompt beyond a short preview: the point is the input's SHAPE (magic
    # bytes, field order, how long it had to be), not a constant to hard-code.
    poc_path: Optional[str] = None
    poc_size: Optional[int] = None
    poc_preview: Optional[str] = None
    # Set when a PoC was replayed and did NOT crash the vulnerable build. That
    # is a fact about the corpus record, not about the harness, and it must not
    # be presented to the model as though the crash were confirmed.
    poc_did_not_reproduce: bool = False

    @property
    def observed(self) -> bool:
        return self.source == "observed"

    @property
    def has_evidence(self) -> bool:
        return bool(self.crash_type or self.frames or self.frame_names
                    or self.poc_preview)

    @property
    def names(self) -> List[str]:
        """Frame symbols innermost-first, project frames only.

        The steering seeds. Prefers the observed stack (real, ordered, with
        locations) and falls back to the record's prose names.
        """
        if self.frames:
            out: List[str] = []
            for fr in self.frames:
                name = fr.bare_name
                if name and name not in out:
                    out.append(name)
            return out
        return [strip_args(n) for n in self.frame_names if n]

    @property
    def locations(self) -> dict:
        """``{frame name: (file, line)}`` for the frames we have locations for.

        Handed to ``DiffAnalyzer.analyze`` as ``crash_locations``: when a frame's
        symbol does not match anything in the static index, the file and line the
        sanitizer printed still identify the function. Empty unless the crash was
        observed — the OSV prose gives names alone.
        """
        return {fr.bare_name: (fr.file, fr.line)
                for fr in self.frames if fr.file and fr.line}

    @classmethod
    def from_osv(cls, crash_type: Optional[str],
                 crash_state: Optional[List[str]]) -> "CrashEvidence":
        """The record's prose, in the same shape as a real report."""
        return cls(source="osv", crash_type=crash_type,
                   frame_names=[s for s in (crash_state or []) if s])

    @classmethod
    def from_run(cls, output: str, *, poc_path: Optional[str] = None,
                 triggered: bool = True,
                 fallback: Optional["CrashEvidence"] = None
                 ) -> "CrashEvidence":
        """Parse a replay of the PoC on the vulnerable build.

        ``fallback`` (normally the OSV-derived evidence) supplies anything the
        report did not say, so a partially-parsed report is never worse than the
        prose it replaces. A replay that did not crash returns the fallback
        flagged ``poc_did_not_reproduce`` — the PoC and the report still
        describe the input's shape, but nothing here was confirmed.
        """
        base = fallback or cls()
        poc = _poc_facts(poc_path)
        if not triggered:
            return cls(source=base.source, crash_type=base.crash_type,
                       access=base.access, frames=list(base.frames),
                       alloc_frames=list(base.alloc_frames),
                       frame_names=list(base.frame_names),
                       poc_did_not_reproduce=True, **poc)

        frames, alloc = _parse_frames(output)
        return cls(
            source="observed",
            crash_type=crash_type_of(output) or base.crash_type,
            access=access_of(output) or base.access,
            frames=frames or list(base.frames),
            alloc_frames=alloc,
            frame_names=list(base.frame_names),
            **poc,
        )


def strip_args(symbol: str) -> str:
    """``ns::Foo::bar(char const*, unsigned long)`` -> ``ns::Foo::bar``.

    Cuts at the first '(' rather than balancing parentheses: a demangled name
    can contain nested ones ("(anonymous namespace)::f(int)"), and for matching
    against the static index the leading segment is what counts.
    """
    name = (symbol or "").strip()
    cut = name.find("(")
    if cut > 0:
        name = name[:cut]
    return name.strip().rstrip(":")


def crash_type_of(output: str) -> Optional[str]:
    for rex in _TYPE_RES:
        m = rex.search(output)
        if m:
            return _ADDRESSES_RE.sub("", m.group(1)).strip()
    return None


def access_of(output: str) -> Optional[str]:
    m = _ACCESS_RE.search(output)
    return m.group(1).strip() if m else None


def reached_functions(output: str, limit: int = 60) -> List[str]:
    """Project functions this run demonstrably entered, innermost first.

    The C analogue of ``java/execution/fuzz_runner.py::covered_functions``, and
    it exists for the same reason: the campaign's steering is only a coverage
    map of the root-cause neighbourhood if something measures what the accepted
    harnesses actually reached. A sanitizer stack is not full coverage — it is
    the path that was live at the moment of the crash — but it is the one
    ground-truth reach signal a run produces for free, and it is exactly what
    the Java front-end steers on.
    """
    out: List[str] = []
    seen = set()
    for fr in _iter_frames(output):
        name = fr.bare_name
        if not name or _is_noise(name) or name in seen:
            continue
        seen.add(name)
        out.append(name)
        if len(out) >= limit:
            break
    return out


# -- internals -------------------------------------------------------------
def _iter_frames(output: str):
    for line in output.splitlines():
        m = _FRAME_RE.match(line)
        if not m:
            continue
        symbol, path, line_no = _split_frame(m.group(2))
        if not symbol:
            continue
        yield Frame(index=int(m.group(1)), function=symbol,
                    file=path, line=line_no)


def _split_frame(rest: str):
    """Peel ``<file>:<line>[:<col>]`` off the end of a frame's tail.

    Split from the right because the symbol may contain spaces (demangled C++)
    while the location never does. A frame with no location — ``(<unknown
    module>)``, or a stripped build — keeps the whole tail as the symbol.
    """
    parts = rest.rsplit(" ", 1)
    if len(parts) == 2:
        m = _LOC_RE.match(parts[1])
        if m:
            return parts[0].strip(), m.group("file"), int(m.group("line"))
        # '(/lib/x86_64-linux-gnu/libc.so.6+0x2a1c9)' — a module offset, not a
        # source location, and not worth showing.
        if parts[1].startswith("(") and parts[1].endswith(")"):
            return parts[0].strip(), None, None
    return rest.strip(), None, None


def _parse_frames(output: str):
    """The faulting stack and the allocation stack, kept apart.

    A heap report contains several ``#0 ...`` stacks in a row; the first belongs
    to the access, the ones under an "allocated by"/"freed by" header belong to
    the block's history. Merging them puts the allocator on the path the harness
    is told to re-enter.
    """
    fault: List[Frame] = []
    alloc: List[Frame] = []
    in_alloc = False
    for line in output.splitlines():
        low = line.lower()
        if any(h in low for h in _ALLOC_HEADERS):
            in_alloc = True
            continue
        m = _FRAME_RE.match(line)
        if not m:
            # A blank line ends a stack; the next '#0' after one starts a new
            # one, so only reset on the boundary rather than on any prose.
            if not line.strip():
                continue
            continue
        symbol, path, line_no = _split_frame(m.group(2))
        if not symbol:
            continue
        idx = int(m.group(1))
        if idx == 0 and fault and not in_alloc:
            # A second faulting stack (LeakSanitizer prints one per leak).
            # Keep the first; it is the one the summary refers to.
            in_alloc = True
        frame = Frame(index=idx, function=symbol, file=path, line=line_no)
        (alloc if in_alloc else fault).append(frame)
    return _project_only(fault), _project_only(alloc)


def _project_only(frames: List[Frame]) -> List[Frame]:
    """Drop sanitizer/libFuzzer/libc frames but keep the order and the count.

    Kept as *frames* rather than names because the file and line are the part
    the OSV prose cannot give, and they are what makes the block worth splicing
    into a prompt at all.
    """
    return [f for f in frames if not _is_noise(f.bare_name)]


def _is_noise(name: str) -> bool:
    if not name:
        return True
    return name.startswith(_NOISE_PREFIXES)


_CONTAINER_SRC_RE = re.compile(r"^/src/[^/]+/")


def _project_relative(path: str) -> str:
    """``/src/libxml2/parser.c`` -> ``parser.c``.

    ``helper.py build_fuzzers <project> <path>`` always mounts the checkout at
    ``/src/<project>``, so that prefix is a container detail with no meaning to
    whoever reads the prompt or the artifacts — and the model was shown these
    files under their project-relative names, which is what the region list and
    the function blocks use. Leaving both spellings in one prompt invites the
    model to treat them as two different files.
    """
    return _CONTAINER_SRC_RE.sub("", path) if path else path


def _poc_facts(poc_path: Optional[str]) -> dict:
    """Size and a short preview of the PoC, or empty when there is none.

    Deliberately a *preview*: the prompt tells the model to reproduce the
    input's shape, not to hard-code the bytes. A harness that embeds a constant
    testcase passes the trigger gate and proves nothing about the fix, which is
    the failure mode the Java front-end's "anchor THEN fuzz" wording is written
    against.
    """
    if not poc_path or not os.path.isfile(poc_path):
        return {}
    try:
        size = os.path.getsize(poc_path)
        with open(poc_path, "rb") as fh:
            head = fh.read(64)
    except OSError:
        return {}
    return {"poc_path": poc_path, "poc_size": size,
            "poc_preview": hexpreview(head)}


def hexpreview(data: bytes) -> str:
    """``48 65 6c 6c 6f  |Hello|`` — hex plus the printable rendering.

    Both, because either alone hides half of what an input format looks like:
    the hex shows magic bytes and lengths, the ASCII shows tags and keywords.
    """
    if not data:
        return ""
    hexed = " ".join(f"{b:02x}" for b in data)
    text = "".join(chr(b) if 32 <= b < 127 else "." for b in data)
    return f"{hexed}  |{text}|"
