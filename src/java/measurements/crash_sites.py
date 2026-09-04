"""Crash sites — where a Jazzer finding actually landed.

MEASUREMENT ONLY. Nothing in the pipeline imports this module.

CSM (crash-site match) is the share of crashes whose SITE lies inside
the root-cause region. The site is the deepest frame that is still
inside the library under test — deliberately NOT the harness's own
assertion frame. A harness that asserts on a value and throws does so
from `FuzzHarness.fuzzerTestOneInput`, which is the same location for
every finding it ever produces; scoring that frame would say nothing
about where the defect is. So:

  * frames from Jazzer, the JDK and JUnit are dropped outright — they
    are infrastructure, never "the library";
  * frames in the generated harness are KEPT (they are part of the
    story) but flagged, and are never chosen as the site;
  * the first remaining frame, reading the trace top-down, is the site.

When the headline exception has no library frame at all — the classic
case is a harness that catches a library crash and rethrows it as its
own alarm type — the site is taken from the `Caused by:` chain, from
its deepest entry outward. That is the same rule
`java.execution.fuzz_runner.cause_signature` uses to recover the
identity of a laundered crash, and it is why a finding like
"FuzzerSecurityIssueLow at FuzzHarness.fuzzerTestOneInput, caused by
StringIndexOutOfBoundsException at G2TextMeasurer.getStringWidth"
scores against `G2TextMeasurer.getStringWidth` and not against the
harness.

`from_jazzer_output` reads a live run's stdout+stderr.
`from_trace` reads an archived leg, which only kept `trace.md`; see its
docstring for what parts of a trace carry crashes and how each one is
attributed to the buggy or the patched build.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import List, Optional, Tuple

from java.measurements.locations import MethodRef, from_stack_frame

# --------------------------------------------------------------------------
# What counts as library / harness / infrastructure
# --------------------------------------------------------------------------

HARNESS_CLASS_PREFIX = 'FuzzHarness'
HARNESS_ENTRY_METHOD = 'fuzzerTestOneInput'

#: Frames from these package roots are never part of the library under
#: test. Same list `fuzz_runner.crash_signature` uses, plus junit (test
#: scaffolding lifted into a harness can drag JUnit frames in).
EXCLUDED_FRAME_PREFIXES = (
    'com.code_intelligence.jazzer',
    'java.',
    'jdk.',
    'sun.',
    'junit.',
)

SITE_LIBRARY = 'library'
SITE_HARNESS_ONLY = 'harness_only'

BUILD_BUGGY = 'buggy'
BUILD_PATCHED = 'patched'


def _is_excluded(class_fq: str) -> bool:
    return any(class_fq == p.rstrip('.') or class_fq.startswith(p)
               for p in EXCLUDED_FRAME_PREFIXES)


def _is_harness(ref: MethodRef) -> bool:
    """The generated harness: its class, or the Jazzer entry point that
    only a harness can define."""
    return (ref.class_simple.startswith(HARNESS_CLASS_PREFIX)
            or ref.name == HARNESS_ENTRY_METHOD)


# --------------------------------------------------------------------------
# The crash site object
# --------------------------------------------------------------------------

@dataclass
class CrashSite:
    """One crash, reduced to the location it should be scored against.

    exception        throwable type of the headline (`== Java Exception:`)
    message          the rest of the headline line, if any
    causes           throwable types of the `Caused by:` chain, outermost
                     cause first
    frames           every project frame in trace order, harness frames
                     included, as (method, source line or None)
    harness_frames   parallel to `frames`: True where that frame is the
                     generated harness rather than the library
    top_library      the site: first frame that is neither infrastructure
                     nor harness; None when the crash never left the
                     harness
    top_library_line the site's source line, when the frame carried one
    site_kind        'library' when a site was found, else 'harness_only'
    build            'buggy', 'patched', or '' when not attributable
    harness          harness name, when known
    source           where in the input this crash was read from
    """
    exception: str = ''
    message: str = ''
    causes: List[str] = field(default_factory=list)
    frames: List[Tuple[MethodRef, Optional[int]]] = field(default_factory=list)
    harness_frames: List[bool] = field(default_factory=list)
    top_library: Optional[MethodRef] = None
    top_library_line: Optional[int] = None
    site_kind: str = SITE_HARNESS_ONLY
    build: str = ''
    harness: str = ''
    source: str = ''

    @property
    def dedupe_key(self) -> tuple:
        """Identity used to collapse repeats: exception plus the frame
        list. The message is left out on purpose — the same crash
        reported twice may carry different fuzzed values in its text."""
        return (self.exception,
                tuple((r.strict_key, ln) for r, ln in self.frames))

    def to_dict(self) -> dict:
        return {
            'exception': self.exception,
            'message': self.message,
            'causes': list(self.causes),
            'frames': [{'method': r.to_dict(), 'line': ln,
                        'harness': h}
                       for (r, ln), h in zip(self.frames,
                                             self.harness_frames)],
            'top_library': (self.top_library.to_dict()
                            if self.top_library else None),
            'top_library_line': self.top_library_line,
            'site_kind': self.site_kind,
            'build': self.build,
            'harness': self.harness,
            'source': self.source,
        }

    @classmethod
    def from_dict(cls, d: dict) -> 'CrashSite':
        frames, flags = [], []
        for f in d.get('frames', []):
            frames.append((MethodRef.from_dict(f['method']), f.get('line')))
            flags.append(bool(f.get('harness')))
        tl = d.get('top_library')
        return cls(
            exception=d.get('exception', ''),
            message=d.get('message', ''),
            causes=list(d.get('causes') or []),
            frames=frames,
            harness_frames=flags,
            top_library=MethodRef.from_dict(tl) if tl else None,
            top_library_line=d.get('top_library_line'),
            site_kind=d.get('site_kind', SITE_HARNESS_ONLY),
            build=d.get('build', ''),
            harness=d.get('harness', ''),
            source=d.get('source', ''),
        )


# --------------------------------------------------------------------------
# Parsing Jazzer output
# --------------------------------------------------------------------------

_EXC_RE = re.compile(r'^==\s*Java Exception:\s*([\w.$]+)\s*(?::\s*(.*))?$')
_CAUSE_RE = re.compile(r'^\s*Caused by:\s*([\w.$]+)\s*(?::\s*(.*))?$')
_FRAME_RE = re.compile(r'^\s*at\s+\S')
_SUPPRESSED_RE = re.compile(r'^\s*(\.\.\.\s*\d+\s+more|Suppressed:)')
#: JDK 9+ prints frames as `at java.base/java.lang.String.substring(...)`
#: and some JUnit runners as `at app//org.jfree.Foo.bar(...)`. The part
#: before the '/' is the module (or class-loader) name, not the package,
#: and `locations.from_stack_frame` does not accept it, so it is
#: stripped before the frame is handed over.
_MODULE_RE = re.compile(r'^(\s*at\s+)[\w.$@]*/{1,2}')


def _parse_frame(line: str) -> Optional[Tuple[MethodRef, Optional[int]]]:
    return from_stack_frame(_MODULE_RE.sub(r'\1', line))


def _build_site(exception: str, message: str,
                segments: List[Tuple[str, List]],
                build: str, harness: str, source: str) -> CrashSite:
    """Assemble one CrashSite from parsed segments.

    `segments` is [(throwable, [(ref, line), ...]), ...] with the
    headline first and each `Caused by:` after it. The site is the first
    library frame of the headline; failing that, the first library frame
    of the DEEPEST cause, then of each shallower cause in turn.
    """
    frames: List[Tuple[MethodRef, Optional[int]]] = []
    flags: List[bool] = []
    for _, seg_frames in segments:
        for ref, line in seg_frames:
            frames.append((ref, line))
            flags.append(_is_harness(ref))

    order = [0] + list(range(len(segments) - 1, 0, -1))
    site, site_line = None, None
    for idx in order:
        for ref, line in segments[idx][1]:
            if not _is_harness(ref):
                site, site_line = ref, line
                break
        if site is not None:
            break

    return CrashSite(
        exception=exception,
        message=message,
        causes=[t for t, _ in segments[1:]],
        frames=frames,
        harness_frames=flags,
        top_library=site,
        top_library_line=site_line,
        site_kind=SITE_LIBRARY if site is not None else SITE_HARNESS_ONLY,
        build=build,
        harness=harness,
        source=source,
    )


def from_jazzer_output(text: str, build: str = '', harness: str = '',
                       source: str = 'jazzer-output') -> List[CrashSite]:
    """Every `== Java Exception:` block in a Jazzer run's output.

    A block is the headline line, the `\\tat ...` frames under it, and
    any `Caused by:` chain, and it ends at the first line that is none
    of those — in practice `== libFuzzer crashing input ==`, a blank
    run-statistics line, or the next headline. Frames whose class is
    infrastructure (Jazzer, JDK, JUnit) are dropped as they are read, so
    a chain like

        Caused by: java.lang.StringIndexOutOfBoundsException
            at java.base/java.lang.String.substring(String.java:1874)
            at org.jfree.chart.text.G2TextMeasurer.getStringWidth(...:78)

    contributes only the `G2TextMeasurer` frame, which then becomes the
    site.

    No deduplication happens here: the caller sees exactly the crashes
    the output contained, in order.
    """
    sites: List[CrashSite] = []
    exception = message = ''
    segments: List[Tuple[str, List]] = []
    open_block = False

    def close():
        nonlocal open_block, segments, exception, message
        if open_block:
            sites.append(_build_site(exception, message, segments,
                                     build, harness, source))
        open_block = False
        segments = []
        exception = message = ''

    for raw in (text or '').splitlines():
        m = _EXC_RE.match(raw.strip())
        if m:
            close()
            open_block = True
            exception = m.group(1)
            message = (m.group(2) or '').strip()
            segments = [(exception, [])]
            continue
        if not open_block:
            continue
        m = _CAUSE_RE.match(raw)
        if m:
            segments.append((m.group(1), []))
            continue
        if _SUPPRESSED_RE.match(raw):
            # '... 12 more' and 'Suppressed:' keep the block open but
            # carry no location of their own.
            continue
        if _FRAME_RE.match(raw):
            parsed = _parse_frame(raw)
            if parsed is not None and not _is_excluded(parsed[0].class_fq):
                segments[-1][1].append(parsed)
            continue
        if not raw.strip():
            # A blank line inside a trace is tolerated; Jazzer prints one
            # between the frames and the libFuzzer footer.
            continue
        close()
    close()
    return sites


# --------------------------------------------------------------------------
# Parsing an archived trace.md
# --------------------------------------------------------------------------

_HEADER_RE = re.compile(r'^##\s+(.*\S)\s*$')
_EVIDENCE_OPEN = '<evidence>'
_EVIDENCE_CLOSE = '</evidence>'

#: A one-line crash identity the pipeline writes into its own notes:
#: `<throwable>@<Class>.<method>`. `fuzz_runner.crash_signature` and
#: `cause_signature` produce exactly this shape.
_SIG_RE = re.compile(r'([\w.$]+)@([\w.$]+)\.([\w$<>]+)')

#: The two annotation shapes that carry a BUGGY-build crash identity.
#: Both are written next to a patched-build firing to say what the same
#: input does on the unpatched code.
_BUGGY_REPLAY_RE = re.compile(
    r'on the buggy replay:\s*(?P<head>[^;]*?)'
    r'(?:\s*;\s*root cause:\s*(?P<cause>[^;]*?))?'
    r'\s*;\s*under the patched firing:\s*(?P<patched>\S+)')
_DIFF_REPLAY_RE = re.compile(
    r'alarm wraps generic cause\s*(?P<alarm>[^;\s]+)\s*;\s*'
    r'buggy-build replay gives headline=(?P<head>[^,]*)'
    r'(?:,\s*cause=(?P<cause>\S*))?')

_BUGGY_WORDS = ('buggy build', 'buggy-build', 'on the buggy', 'buggy side',
                'buggy-side', 'known-buggy', 'screen-fuzz-buggy',
                'the buggy code', 'buggy replay', 'buggy version')
_PATCHED_WORDS = ('patched build', 'patched-build', 'on the patched',
                  'patched code', 'patched side', 'patched-side',
                  'patched-fuzz', 'patched version', 'patched firing')

_PATHY = re.compile(r'\S*/\S*')


def _strip_paths(text: str) -> str:
    """Remove filesystem paths before looking for build words.

    Jazzer prints `artifact_prefix='/home/.../Chart_26_buggy/fuzz/...'`
    in every finding, and that directory is named after the CHECKOUT the
    harness was compiled in, not the build it was run against. Reading
    the word 'buggy' out of it would mislabel every patched-side crash,
    so any whitespace-delimited token containing a '/' is dropped."""
    return _PATHY.sub(' ', text)


def _nearest_build(lines: List[str], upto: int, floor: int) -> str:
    """Scan backwards from `upto` to `floor` for the closest build word.

    Traces name the build in prose right before the evidence they are
    about ("The assertion that ACTUALLY fired on the patched code is:",
    "it did NOT trigger the bug on the known-buggy version"). Taking the
    nearest such phrase above the crash is the whole heuristic; when a
    line mentions both builds, the one that appears later in the line
    wins, which matches how these sentences are written ("... fired on
    the buggy build ... on this patched build")."""
    for i in range(upto, max(floor, 0) - 1, -1):
        text = _strip_paths(lines[i]).lower()
        pos_b = max((text.rfind(w) for w in _BUGGY_WORDS), default=-1)
        pos_p = max((text.rfind(w) for w in _PATCHED_WORDS), default=-1)
        if pos_b < 0 and pos_p < 0:
            continue
        return BUILD_BUGGY if pos_b > pos_p else BUILD_PATCHED
    return ''


def _sig_site(sig: str, build: str, harness: str, source: str,
              default_exception: str = '') -> Optional[CrashSite]:
    """Turn a `<throwable>@<Class>.<method>` note into a CrashSite.

    These notes carry no line numbers and only one frame, so the site is
    that frame (or None when it is the harness). A note that is only a
    throwable name, with no '@frame' — the traces write bare
    'StringIndexOutOfBoundsException' and 'none recorded' — yields
    nothing, because a crash with no location cannot be scored for CSM
    and counting it as a miss would understate the metric."""
    if not sig:
        return None
    m = _SIG_RE.search(sig)
    if not m:
        return None
    exception, cls, name = m.group(1), m.group(2), m.group(3)
    if _is_excluded(cls):
        return None
    ref = MethodRef(cls.replace('$', '.'), name)
    harness_frame = _is_harness(ref)
    return CrashSite(
        exception=exception or default_exception,
        frames=[(ref, None)],
        harness_frames=[harness_frame],
        top_library=None if harness_frame else ref,
        top_library_line=None,
        site_kind=SITE_HARNESS_ONLY if harness_frame else SITE_LIBRARY,
        build=build,
        harness=harness,
        source=source,
    )


def from_trace(trace_md_path: str) -> List[CrashSite]:
    """Every crash an archived leg's `trace.md` still records.

    An archived leg keeps only `trace.md`, so the crashes have to be
    recovered from the text the run happened to write down. Three places
    carry them, and each is attributed to a build differently:

    1. **`<evidence>` blocks inside a `verifier / judge` LLM call.**
       These are the concrete evidence
       `relations.relation_verifier` attaches to a judging prompt
       (relation_verifier.py:395 and :413), and the sentence directly
       above them is always "The assertion that ACTUALLY fired on the
       **patched** code is:". They contain the raw Jazzer output —
       headline, frames, `Caused by:` chain, the libFuzzer footer — so
       they are parsed with `from_jazzer_output`. Build: whatever the
       nearest build word above says, defaulting to 'patched' when the
       block sits in a verifier section and says nothing.
       *(example: runs-archive/runs/final30B_20260729_145001/
       11_patch1-Chart-26-Jaid_c/trace.md, section `## [87]` at line
       10936; evidence at lines 11145-11209, with a harness-only crash
       at 11146 and a laundered one at 11158 whose `Caused by:` chain
       at 11160-11166 lands on
       `org.jfree.chart.text.G2TextMeasurer.getStringWidth`.)*

    2. **`== Java Exception:` blocks anywhere else** — for instance the
       CITATION quoted into an `outcome-drop` detail. Parsed the same
       way; the build comes from the nearest build word above, and is
       '' when there is none.

    3. **One-line crash identities in the run's own annotations.** The
       buggy build's output is not archived, but two notes written
       beside a patched firing quote its identity in
       `fuzz_runner`'s `<throwable>@<Class>.<method>` form:

         `[differential replay] laundering check: alarm wraps generic
          cause <sig>; buggy-build replay gives headline=<sig>,
          cause=<sig>` (same file, line 11168)

         `Underlying exception identity — on the buggy replay: <sig>;
          root cause: <sig>; under the patched firing: <sig>` (same
          file, line 12726)

       The `buggy-build replay`/`buggy replay` parts become 'buggy'
       crashes, the `alarm wraps generic cause` part is the patched
       firing's own root cause. Parts that name only a throwable
       ('none', 'none recorded', a bare type name) are skipped: with no
       frame there is no site to score.

    Two things are deliberately NOT read. `- trigger: <exception>` lines
    under an accepted `harness-attempt` prove the harness crashed the
    buggy build, but carry no frame at all, so they would add crashes
    with no site. And filesystem paths are stripped before any build
    word is looked for, because the working directory is always named
    after the buggy checkout (`.../Chart_26_buggy/fuzz/...`) whichever
    build was actually run.

    Crashes are deduplicated per build on (exception, frames); the first
    occurrence keeps its `source`, which is the section header it was
    found under.
    """
    with open(trace_md_path, errors='replace') as fh:
        lines = fh.read().split('\n')

    header = ''
    header_line = 0
    in_evidence = False
    evidence_start = 0
    evidence: List[str] = []
    found: List[CrashSite] = []

    def flush_evidence() -> None:
        build = _nearest_build(lines, evidence_start - 1, header_line)
        if not build and 'verifier' in header.lower():
            build = BUILD_PATCHED
        text = '\n'.join(evidence)
        found.extend(from_jazzer_output(
            text, build=build, source=f"{header} · evidence"))
        # Annotations live inside the evidence block too.
        for note in evidence:
            found.extend(_signature_sites(note, header, 'evidence'))

    i = 0
    while i < len(lines):
        line = lines[i]
        m = _HEADER_RE.match(line)
        if m:
            if in_evidence:
                # A truncated trace can start a new section before the
                # block closed; take what the block held so far.
                flush_evidence()
                in_evidence = False
            header, header_line = m.group(1), i
            i += 1
            continue

        if line.strip() == _EVIDENCE_OPEN:
            in_evidence, evidence, evidence_start = True, [], i + 1
            i += 1
            continue
        if in_evidence:
            if line.strip() == _EVIDENCE_CLOSE:
                flush_evidence()
                in_evidence = False
            else:
                evidence.append(line)
            i += 1
            continue

        if _EXC_RE.match(line.strip()):
            # A crash block outside any evidence block: take it and the
            # lines under it until the block parser stops.
            j = i + 1
            while j < len(lines) and not _HEADER_RE.match(lines[j]):
                nxt = lines[j].strip()
                if (_FRAME_RE.match(lines[j]) or _CAUSE_RE.match(lines[j])
                        or _SUPPRESSED_RE.match(lines[j]) or not nxt):
                    j += 1
                    continue
                break
            build = _nearest_build(lines, i - 1, header_line)
            found.extend(from_jazzer_output(
                '\n'.join(lines[i:j]), build=build,
                source=f"{header} · jazzer-output"))
            i = j
            continue

        found.extend(_signature_sites(line, header, 'annotation'))
        i += 1

    return _dedupe(found)


def _signature_sites(line: str, header: str, where: str) -> List[CrashSite]:
    """Crash identities quoted in one line of prose (see `from_trace`
    point 3)."""
    out: List[CrashSite] = []
    m = _DIFF_REPLAY_RE.search(line)
    if m:
        s = _sig_site(m.group('alarm') or '', BUILD_PATCHED, '',
                      f"{header} · {where}:differential-replay-cause")
        if s:
            out.append(s)
        for part, tag in ((m.group('head'), 'headline'),
                          (m.group('cause'), 'cause')):
            s = _sig_site(part or '', BUILD_BUGGY, '',
                          f"{header} · {where}:buggy-replay-{tag}")
            if s:
                out.append(s)
    m = _BUGGY_REPLAY_RE.search(line)
    if m:
        for part, tag in ((m.group('head'), 'headline'),
                          (m.group('cause'), 'cause')):
            s = _sig_site(part or '', BUILD_BUGGY, '',
                          f"{header} · {where}:buggy-replay-{tag}")
            if s:
                out.append(s)
        s = _sig_site(m.group('patched') or '', BUILD_PATCHED, '',
                      f"{header} · {where}:patched-firing")
        if s:
            out.append(s)
    return out


def _dedupe(sites: List[CrashSite]) -> List[CrashSite]:
    """Collapse identical (build, exception, frames), keeping the first
    — and with it the first `source` the crash was seen under."""
    seen = set()
    out = []
    for s in sites:
        key = (s.build,) + s.dedupe_key
        if key in seen:
            continue
        seen.add(key)
        out.append(s)
    return out
