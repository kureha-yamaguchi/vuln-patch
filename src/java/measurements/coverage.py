"""F(H) — what the harness set actually executed.

MEASUREMENT ONLY. Nothing in the pipeline imports this module.

The paper's F(H) is "the set of code locations the fuzz harness set H
executed". We get that set from JaCoCo, which is the only tool in the
stack that reports coverage at *method* and *line* granularity for the
library under test. The route is:

  1. Jazzer runs the harness with `--coverage_dump=<file>.exec`, which
     writes a JaCoCo execution-data file (`jazzer_coverage_args`).
  2. The JaCoCo command-line tool turns one or more `.exec` files plus
     the compiled classes and sources into an XML report (`report`,
     using the jar `ensure_jacoco_cli` downloads).
  3. `parse_jacoco_xml` reads that XML into a `Coverage` object whose
     members are `locations.MethodRef` / `locations.LineRef` — the same
     identities every other measurement in this package uses.
  4. `collect_leg` does all three for one archived run leg and unions
     the per-harness coverage into one `Coverage` per build.

Two conventions matter for the numbers to mean what the paper says:

  * The harness's own class is NOT part of the library, so every class
    whose simple name starts with `FuzzHarness` is dropped. Counting it
    would make every harness look like it covered "one more method".
  * Synthetic methods (JaCoCo reports `lambda$foo$0`, `access$100`, and
    similar compiler-generated members) are dropped, because no other
    source in the package — a diff hunk, a stack frame, an
    introspector name — can name them, so they could never be matched
    and would only inflate denominators.

`Coverage.methods` is the covered set; `Coverage.all_methods` is every
method JaCoCo saw after those two filters. `all_methods` is the
reference population you hand to `locations.MethodIndex` when you need
to resolve a name from some other tool onto a real method.

Branches, and why a MERGED report exists
----------------------------------------
JaCoCo also reports, on every source line, how many branch outcomes that
line has and how many were taken (`<line nr=".." mi=".." ci=".." mb=".."
cb=".."/>`: `mb` missed, `cb` covered). `Coverage.line_branches` keeps
that pair per line, which is what the `branch` granularity of
`metrics.py` counts.

Those are COUNTS, not identities. The XML never says WHICH outcome of a
line was taken, so the union of two harnesses' branch coverage cannot be
computed from two per-harness reports: harness A taking outcome 1 and
harness B taking outcome 1 of the same line looks exactly like the two of
them taking outcome 1 and outcome 2. Method and line sets do not have
this problem — they are sets of identities and union exactly — so only
the branch numbers need help.

The help is JaCoCo's own merge: all of a build's `.exec` files are handed
to ONE `report` call, which merges the execution data before it counts
anything, so the branch counts in the resulting `merged_<build>.xml` are
the harness SET's, exactly. `collect_leg` writes that report and reads
the whole per-build `Coverage` from it, recording `branches_from =
'merged'`. When it cannot (a leg archived with its XML reports but
without its `.exec` files, or a JaCoCo CLI that will not run) it falls
back to `union` of the per-harness reports and records `branches_from =
'union-upper-bound'` — see `union` for what that bound is.
"""
from __future__ import annotations

import json
import os
import subprocess
import urllib.request
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from typing import Dict, Iterable, List, Optional, Set, Tuple

from java.measurements.locations import (LineRef, MethodRef, from_jacoco,
                                         from_stack_frame, top_level_of)

# --------------------------------------------------------------------------
# The coverage object
# --------------------------------------------------------------------------

HARNESS_CLASS_PREFIX = 'FuzzHarness'

BUILD_BUGGY = 'buggy'
BUILD_PATCHED = 'patched'
#: The acceptance-check run of EVERY compiled candidate, kept or not. It is
#: the BUGGY build — the gate runs each freshly compiled harness against the
#: unfixed code — so it is reported against the buggy classes and sources,
#: and it is a separate build token only because it is a different HARNESS
#: SET: all compiled candidates, versus the kept ones the other two tokens
#: cover. See the README, "Kept versus all compiled harnesses".
BUILD_COMPILED = 'compiled'
#: The FIXED-INPUT-BUDGET re-run of the kept harness set on the buggy build
#: (`remeasure_leg`). `buggy` is the coverage of the run the verdict rests
#: on, and that run had a wall-clock budget, so its numbers move with the
#: load on the machine that produced them. `remeasure` re-runs exactly the
#: kept harnesses with `-runs=N` instead, so the same archive measured on
#: two machines gives the same set. Same build, same harnesses, different
#: budget — which is why it is a build token of its own and not a second
#: reading of `buggy`. See the README, "3.3.3 As-run and re-measured".
BUILD_REMEASURE = 'remeasure'

#: Every build token a `.exec` dump can carry. No token may contain an
#: underscore: `_split_exec_name` splits a dump's name on the LAST one.
BUILDS = (BUILD_BUGGY, BUILD_PATCHED, BUILD_COMPILED, BUILD_REMEASURE)

#: How a `Coverage`'s branch numbers were obtained. `merged` means one
#: JaCoCo report over ALL of that build's `.exec` files, so the counts are
#: the harness set's own and are exact. `union-upper-bound` means the
#: per-harness reports were added up because no merged report could be
#: produced, which can only over-count (see `union`).
BRANCHES_MERGED = 'merged'
BRANCHES_UNION = 'union-upper-bound'

#: The file a build's merged report is written to, inside the leg's `cov/`.
MERGED_XML = 'merged_{build}.xml'


def _is_harness_class(class_fq: str) -> bool:
    """True for the generated harness itself.

    JaCoCo names it `org/jfree/chart/axis/FuzzHarness`; repaired
    attempts sometimes add a suffix (`FuzzHarness2`), and its nested
    helpers show up as `FuzzHarness$1`. All of them are harness code,
    not library code, so we test the simple name's prefix."""
    simple = class_fq.replace('/', '.').replace('$', '.').rsplit('.', 1)[-1]
    return simple.startswith(HARNESS_CLASS_PREFIX)


def _is_synthetic_method(name: str) -> bool:
    """Compiler-generated members JaCoCo reports but no other source can
    name: `lambda$draw$0`, `access$100`, `$deserializeLambda$`."""
    return '$' in name or name.startswith('lambda$')


def _normalise_prefix(prefix: str) -> str:
    """'org.jfree.**' / 'org/jfree/' / 'org.jfree.' -> 'org.jfree'."""
    p = prefix.replace('/', '.').strip()
    while p.endswith('*'):
        p = p[:-1]
    return p.rstrip('.')


def _in_prefix(dotted: str, prefix: Optional[str]) -> bool:
    """Package-boundary prefix test: 'org.jfree' accepts
    'org.jfree.chart.Foo' but not 'org.jfreex.Foo'."""
    if not prefix:
        return True
    return dotted == prefix or dotted.startswith(prefix + '.')


@dataclass
class Coverage:
    """One build's executed locations, plus the population they live in.

    methods           the methods this build ran: the JaCoCo METHOD
                      counter's covered set, UNIONED with `frame_methods`
                      once `collect_leg` has repaired the probe miss (see
                      `frame_methods` below and `methods_from_frames`)
    methods_from_probes
                      the probe half of `methods` on its own: methods with
                      JaCoCo METHOD counter covered > 0. `parse_jacoco_xml`
                      fills it with the same set it puts in `methods`, and
                      the frame repair never touches it, so the two
                      provenances stay countable
    frame_methods     methods a STACK FRAME in the raw fuzzer output proves
                      were entered, resolved against `all_methods` by class,
                      name and a line the report shows the method owns.
                      JaCoCo places a method's probe after the method's
                      exit, so a method that throws through its only call
                      reads as missed from probes alone; a frame is proof it
                      ran. Not a subset of `methods_from_probes` and not
                      disjoint from it either — a method can be seen both
                      ways, and `frame_added` is the part probes missed
    lines             source lines with covered instructions (ci > 0)
    branches_covered  branch outcomes taken, summed over the same methods
    branches_total    branch outcomes present, summed over the same methods
    line_branches     per source line, (taken, present) branch outcomes —
                      JaCoCo's `cb` and `mb + cb`. Only lines that HAVE a
                      decision point appear; a line with no branch is
                      absent rather than stored as (0, 0).
    branches_from     how the branch numbers were obtained:
                      `BRANCHES_MERGED` (one JaCoCo report over all the
                      build's .exec files — exact for a harness SET) or
                      `BRANCHES_UNION` (per-harness reports added up, an
                      upper bound; see `union`). Empty when no branch data
                      was read at all.
    all_methods       every method JaCoCo saw (covered or not), after the
                      harness / synthetic / package filters — the
                      denominator, and the population for MethodIndex
    build             'buggy', 'patched' or '' when not attributed
    harness           the harness (or '+'-joined harnesses) this came from
    """
    methods: Set[MethodRef] = field(default_factory=set)
    methods_from_probes: Set[MethodRef] = field(default_factory=set)
    frame_methods: Set[MethodRef] = field(default_factory=set)
    lines: Set[LineRef] = field(default_factory=set)
    branches_covered: int = 0
    branches_total: int = 0
    line_branches: Dict[LineRef, Tuple[int, int]] = field(default_factory=dict)
    branches_from: str = ''
    all_methods: Set[MethodRef] = field(default_factory=set)
    build: str = ''
    harness: str = ''

    @property
    def methods_from_frames(self) -> Set[MethodRef]:
        """The other name for `frame_methods`, so a reader who has the
        probe half in hand (`methods_from_probes`) can ask for the frame
        half by the matching name."""
        return self.frame_methods

    @property
    def frame_added(self) -> Set[MethodRef]:
        """What the frames added that the probes did not have. This is the
        size of the probe limitation on this build, and it is what
        `metrics.py` reports as `F_frame_added`."""
        return self.frame_methods - self.methods_from_probes

    def to_dict(self) -> dict:
        return {
            'methods': [m.to_dict() for m in sorted(self.methods)],
            'methods_from_probes': [m.to_dict()
                                    for m in sorted(self.methods_from_probes)],
            'frame_methods': [m.to_dict() for m in sorted(self.frame_methods)],
            'lines': [l.to_dict() for l in sorted(self.lines)],
            'branches_covered': int(self.branches_covered),
            'branches_total': int(self.branches_total),
            'line_branches': [dict(ref.to_dict(), covered=int(c),
                                   total=int(t))
                              for ref, (c, t) in sorted(
                                  self.line_branches.items())],
            'branches_from': self.branches_from,
            'all_methods': [m.to_dict() for m in sorted(self.all_methods)],
            'build': self.build,
            'harness': self.harness,
        }

    @classmethod
    def from_dict(cls, d: dict) -> 'Coverage':
        # `line_branches` and `branches_from` are younger than the rest of
        # the file, so a coverage JSON written before the branch
        # granularity existed simply has neither and reads back as an
        # empty map and an empty flag.
        lb: Dict[LineRef, Tuple[int, int]] = {}
        for x in d.get('line_branches') or []:
            lb[LineRef.from_dict(x)] = (int(x.get('covered', 0)),
                                        int(x.get('total', 0)))
        methods = {MethodRef.from_dict(m) for m in d.get('methods', [])}
        # `methods_from_probes` and `frame_methods` are younger than
        # `methods`. A coverage JSON written before the frame repair existed
        # has neither, and every method in it came from a probe, so the
        # probe half reads back as the whole set and the frame half as
        # empty — which is exactly what that file recorded.
        probes = ({MethodRef.from_dict(m) for m in d['methods_from_probes']}
                  if isinstance(d.get('methods_from_probes'), list)
                  else set(methods))
        return cls(
            methods=methods,
            methods_from_probes=probes,
            frame_methods={MethodRef.from_dict(m)
                           for m in d.get('frame_methods', [])},
            lines={LineRef.from_dict(l) for l in d.get('lines', [])},
            branches_covered=int(d.get('branches_covered', 0)),
            branches_total=int(d.get('branches_total', 0)),
            line_branches=lb,
            branches_from=d.get('branches_from', ''),
            all_methods={MethodRef.from_dict(m)
                         for m in d.get('all_methods', [])},
            build=d.get('build', ''),
            harness=d.get('harness', ''),
        )


def union(covs: Iterable[Coverage]) -> Coverage:
    """F(H) for a whole harness SET: the union of what each harness ran.

    Method and line sets are sets of IDENTITIES, so they union exactly.
    Branch data is counts, not identities (see the module docstring), so
    it cannot be unioned exactly here at all, and the two ways it is
    reported bracket the truth from opposite sides:

    * `branches_covered` / `branches_total` are the whole-build scalars
      and keep the largest value seen, a LOWER bound on the set's branch
      coverage (the harnesses may between them have taken outcomes that
      no single one of them took).
    * `line_branches` keeps, per line, the SUM of the per-harness taken
      counts, capped at that line's total, an UPPER bound (two harnesses
      taking the SAME outcome are counted twice). `branches_from` is set
      to `BRANCHES_UNION` to say so.

    The exact answer for a harness set comes from a merged JaCoCo report,
    which is what `collect_leg` produces whenever the `.exec` files are
    there; this function is the fallback for when they are not.

    `build` survives only if every input agrees on it; `harness` becomes
    the '+'-joined sorted list of the harness names.
    """
    covs = list(covs)
    out = Coverage()
    builds, harnesses = set(), set()
    for c in covs:
        out.methods |= set(c.methods)
        out.methods_from_probes |= set(c.methods_from_probes)
        out.frame_methods |= set(c.frame_methods)
        out.lines |= set(c.lines)
        out.all_methods |= set(c.all_methods)
        out.branches_covered = max(out.branches_covered, c.branches_covered)
        out.branches_total = max(out.branches_total, c.branches_total)
        for ref, (cov_n, tot_n) in c.line_branches.items():
            have_c, have_t = out.line_branches.get(ref, (0, 0))
            # The total is a property of the compiled code, so every
            # report agrees on it; max() rather than a sum.
            out.line_branches[ref] = (have_c + cov_n, max(have_t, tot_n))
        if c.build:
            builds.add(c.build)
        if c.harness:
            harnesses.add(c.harness)
    # Cap each line's taken count at the outcomes that line actually has:
    # summing per-harness counts can otherwise exceed the total.
    out.line_branches = {ref: (min(c, t), t)
                         for ref, (c, t) in out.line_branches.items()}
    if out.line_branches:
        out.branches_from = BRANCHES_UNION
    out.build = builds.pop() if len(builds) == 1 else ''
    out.harness = '+'.join(sorted(harnesses))
    return out


# --------------------------------------------------------------------------
# Parsing a JaCoCo XML report
# --------------------------------------------------------------------------

def parse_jacoco_xml(path: str,
                     include_prefix: Optional[str] = None) -> Coverage:
    """Read a JaCoCo XML report into a `Coverage`.

    The report nests like this::

        <report>
          <package name="org/jfree/chart/plot">
            <class name="org/jfree/chart/plot/PiePlot$1"
                   sourcefilename="PiePlot.java">
              <method name="draw" desc="(Ljava/awt/Graphics2D;)V" line="42">
                <counter type="INSTRUCTION" missed="0" covered="9"/>
                <counter type="BRANCH"      missed="1" covered="3"/>
                <counter type="METHOD"      missed="0" covered="1"/>
              </method>
            </class>
            <sourcefile name="PiePlot.java">
              <line nr="42" mi="0" ci="9" mb="1" cb="3"/>
            </sourcefile>
          </package>
        </report>

    A method counts as covered when its METHOD counter has covered > 0.
    A line counts as covered when it has at least one covered
    instruction (ci > 0).

    `mb`/`cb` are the branch halves of the same line: how many of that
    line's branch outcomes were missed and how many were covered. They
    go into `line_branches` as (cb, mb + cb) for every line that has any
    — that is the per-line data the `branch` granularity is counted from.
    The whole-build `branches_covered` / `branches_total` scalars are
    read from the method-level BRANCH counters instead and are
    unchanged; the two agree on a report, because they are the same
    numbers grouped two ways.

    Lines are keyed by package + sourcefilename, which is exactly what
    `LineRef` wants: nested classes share their outer class's file, so
    `PiePlot$1` and `PiePlot` both land on `org.jfree.chart.plot.PiePlot`.

    `include_prefix` ('org.jfree', or the glob 'org.jfree.**') keeps only
    that package subtree; without it the whole report is read. Harness
    classes and synthetic methods are always dropped (see module
    docstring), and the branch counters are summed over exactly the
    methods that survive those filters, so every number in the returned
    object describes the same set of methods.

    `iterparse` is used and elements are cleared as they close: a report
    for a project the size of Closure is tens of megabytes and does not
    need to be resident all at once.
    """
    prefix = _normalise_prefix(include_prefix) if include_prefix else None
    cov = Coverage()

    pkg_dotted = ''           # current <package>, dotted
    class_fq = ''             # current <class>, dotted, '$' -> '.'
    class_skipped = True      # is the current <class> filtered out?
    src_skipped = True        # is the current <sourcefile> filtered out?
    src_class_top = ''        # LineRef key for the current <sourcefile>
    method: Optional[MethodRef] = None
    method_skipped = True

    for event, el in ET.iterparse(path, events=('start', 'end')):
        tag = el.tag

        if event == 'start':
            if tag == 'package':
                pkg_dotted = (el.get('name') or '').replace('/', '.')
            elif tag == 'class':
                raw = el.get('name') or ''
                class_fq = raw.replace('/', '.').replace('$', '.')
                class_skipped = (_is_harness_class(raw)
                                 or not _in_prefix(class_fq, prefix))
            elif tag == 'sourcefile':
                fname = el.get('name') or ''
                stem = fname[:-5] if fname.endswith('.java') else fname
                src_class_top = f"{pkg_dotted}.{stem}" if pkg_dotted else stem
                src_skipped = (stem.startswith(HARNESS_CLASS_PREFIX)
                               or not _in_prefix(src_class_top, prefix))
            elif tag == 'method':
                method, method_skipped = None, True
                name = el.get('name') or ''
                if class_skipped or _is_synthetic_method(name):
                    continue
                try:
                    method = from_jacoco(class_fq, name, el.get('desc') or '()V')
                except (AssertionError, KeyError, ValueError):
                    # An unparsable descriptor is a JaCoCo/ASM oddity, not
                    # something a measurement should crash on; skip it.
                    method = None
                method_skipped = method is None
            elif tag == 'line':
                if src_skipped:
                    continue
                ref = LineRef(top_level_of(src_class_top),
                              int(el.get('nr') or 0))
                if int(el.get('ci') or 0) > 0:
                    cov.lines.add(ref)
                # The branch halves of the same element. A line with no
                # decision point has mb = cb = 0 and is left out, so
                # `line_branches` holds exactly the lines that CAN branch.
                mb = int(el.get('mb') or 0)
                cb = int(el.get('cb') or 0)
                if mb or cb:
                    have_c, have_t = cov.line_branches.get(ref, (0, 0))
                    cov.line_branches[ref] = (have_c + cb, have_t + mb + cb)
            elif tag == 'counter':
                # Counters appear at method, class, package and report
                # level. Only the method-level ones are read, so nothing
                # is counted twice and the totals cover exactly the
                # methods that survived the filters.
                if method_skipped or method is None:
                    continue
                ctype = el.get('type')
                covered = int(el.get('covered') or 0)
                missed = int(el.get('missed') or 0)
                if ctype == 'METHOD' and covered > 0:
                    cov.methods.add(method)
                elif ctype == 'BRANCH':
                    cov.branches_covered += covered
                    cov.branches_total += covered + missed
            continue

        # event == 'end'
        if tag == 'method':
            if method is not None and not method_skipped:
                cov.all_methods.add(method)
            method, method_skipped = None, True
        elif tag == 'class':
            class_fq, class_skipped = '', True
        elif tag == 'sourcefile':
            src_class_top, src_skipped = '', True
        elif tag == 'package':
            pkg_dotted = ''
        el.clear()

    # Everything in `methods` came from a METHOD counter, i.e. from a
    # probe. The frame repair (`repair_from_frames`) adds to `methods`
    # later and leaves this half alone, which is how the two provenances
    # stay countable.
    cov.methods_from_probes = set(cov.methods)
    return cov


# --------------------------------------------------------------------------
# The probe limitation, and the stack frames that repair it
# --------------------------------------------------------------------------

def method_line_owners(path: str,
                       include_prefix: Optional[str] = None
                       ) -> Dict[MethodRef, Tuple[int, int]]:
    """Which lines of its source file each method owns: `{ref: [start, end)}`.

    The JaCoCo report says where every method STARTS (`<method line="72">`)
    and, separately, which lines of each source file were executed
    (`<sourcefile><line nr="72" .../>`). It never says which method a line
    belongs to. So ownership is reconstructed the only way the report
    allows: within one source file, the methods are sorted by their
    declaration line and each one owns from its own line up to the next
    method's, the last one to the end of the file.

    A source file, not a class: a nested class's methods sit inside the
    outer class's file, so `Widget` and `Widget$Inner` are sorted together
    or the ranges would overlap. Two methods declared on the SAME line
    (JaCoCo does this for a constructor pair that shares a signature line)
    get the same range and are therefore indistinguishable by line — the
    one case where a frame credits both.

    Only used by `frame_methods`. The same harness / synthetic / package
    filters as `parse_jacoco_xml` are applied, so every ref this returns is
    one `all_methods` can also contain.
    """
    prefix = _normalise_prefix(include_prefix) if include_prefix else None
    per_file: Dict[Tuple[str, str], List[Tuple[int, MethodRef]]] = {}

    pkg_dotted = ''
    class_fq = ''
    class_skipped = True
    src_name = ''
    for event, el in ET.iterparse(path, events=('start', 'end')):
        if event == 'start':
            if el.tag == 'package':
                pkg_dotted = (el.get('name') or '').replace('/', '.')
            elif el.tag == 'class':
                raw = el.get('name') or ''
                class_fq = raw.replace('/', '.').replace('$', '.')
                src_name = el.get('sourcefilename') or ''
                class_skipped = (_is_harness_class(raw)
                                 or not _in_prefix(class_fq, prefix)
                                 or not src_name)
            elif el.tag == 'method':
                name = el.get('name') or ''
                start = int(el.get('line') or 0)
                if class_skipped or _is_synthetic_method(name) or start <= 0:
                    continue
                try:
                    ref = from_jacoco(class_fq, name, el.get('desc') or '()V')
                except (AssertionError, KeyError, ValueError):
                    continue
                per_file.setdefault((pkg_dotted, src_name), []).append(
                    (start, ref))
            continue
        if el.tag == 'class':
            class_fq, src_name, class_skipped = '', '', True
        elif el.tag == 'package':
            pkg_dotted = ''
        el.clear()

    owners: Dict[MethodRef, Tuple[int, int]] = {}
    for entries in per_file.values():
        entries.sort(key=lambda e: (e[0], e[1]))
        starts = sorted({start for start, _ in entries})
        after = {s: n for s, n in zip(starts, starts[1:])}
        for start, ref in entries:
            end = after.get(start, 1 << 30)
            have = owners.get(ref)
            # One ref, two declarations (a report merged over builds): keep
            # the widest range rather than whichever came last.
            owners[ref] = ((min(have[0], start), max(have[1], end))
                           if have else (start, end))
    return owners


def stack_frames(text: str) -> Set[Tuple[str, str, int]]:
    """Every stack frame in some fuzzer output, as (class, method, line).

    Frames are read with `locations.from_stack_frame`, the same parser the
    manifest set and the crash sites use, so a module prefix
    (`java.base/java.lang.String.charAt`) or a class-loader prefix
    (`app//org.jfree.Foo.bar`) does not become part of the class name. A
    frame with no line number is dropped: the line is what tells two
    overloads apart, and without it a frame could credit either.

    Nothing is filtered by package here. A JDK, JUnit or harness frame
    simply resolves against no method of the library's report, because
    `all_methods` holds the library's methods and nothing else.
    """
    out: Set[Tuple[str, str, int]] = set()
    for raw in (text or '').split('\n'):
        parsed = from_stack_frame(raw)
        if parsed is None:
            continue
        ref, lineno = parsed
        if lineno is None or not ref.class_fq:
            continue
        out.add((ref.class_fq, ref.name, int(lineno)))
    return out


def frame_methods(report_xml: str, text: str,
                  include_prefix: Optional[str] = None) -> Set[MethodRef]:
    """The methods a run's stack traces PROVE were entered.

    THE PROBE LIMITATION. JaCoCo marks a line covered when a probe on it
    executes, and it places a method's probe after the method's exit. A
    method whose body is a single `return other(x);` therefore reads as
    MISSED when `other` throws, because the probe after the call never
    runs. Every bug in the crashing split ends in a throw, so this
    under-reports exactly the path the measurement is about. Math-70 is the
    recorded case: the trace names `BisectionSolver.solve` at line 72 and
    JaCoCo reports line 72 as never covered.

    A stack frame is proof the method was entered, so the two sources
    union: probes are the lower bound, frames add what the probes provably
    missed. `Coverage.methods_from_probes` and `Coverage.frame_methods`
    keep them apart afterwards, so a frame-only hit is never mistaken for a
    probe hit.

    A frame gives a class, a method name and a LINE, but no parameter
    types. `method_line_owners` gives each method the lines it owns, so
    matching on the line is what separates two overloads of one name — a
    name-only match would credit both.
    """
    frames = stack_frames(text)
    if not frames:
        return set()
    owners = method_line_owners(report_xml, include_prefix)
    found: Set[MethodRef] = set()
    for ref, (start, end) in owners.items():
        for cls, name, line in frames:
            if cls == ref.class_fq and name == ref.name and start <= line < end:
                found.add(ref)
                break
    return found


def repair_from_frames(cov: Coverage, report_xml: str, text: str,
                       include_prefix: Optional[str] = None) -> Coverage:
    """Union a run's frame-proven methods into `cov`, in place.

    `cov.methods_from_probes` is left as it was, so the two provenances
    stay countable; `cov.frame_methods` records what the frames found, and
    `cov.frame_added` is the part the probes had missed."""
    cov.frame_methods = frame_methods(report_xml, text, include_prefix)
    # Only methods the report knows about: a frame that resolves to nothing
    # in this build's population is not evidence about this build.
    if cov.all_methods:
        cov.frame_methods &= cov.all_methods
    cov.methods |= cov.frame_methods
    return cov


def fuzzer_output(leg_dir: str, build: str) -> str:
    """Every raw Jazzer log a leg kept for ONE build, concatenated.

    A `--coverage` run saves each Jazzer run's output to
    `<leg>/fuzz_out/<harness>_<build>.txt`; the build token in the name is
    the same one the `.exec` dumps carry, so the frames repaired into a
    build's coverage are the frames of that build's own runs and no
    other's. Missing directory, missing files: the empty string, which
    adds no frames.
    """
    fo_dir = os.path.join(leg_dir, 'fuzz_out')
    if not os.path.isdir(fo_dir):
        return ''
    parts = []
    for fname in sorted(os.listdir(fo_dir)):
        if not fname.endswith('.txt'):
            continue
        split = _split_exec_name(fname[:-len('.txt')])
        if split is None or split[1] != build:
            continue
        try:
            with open(os.path.join(fo_dir, fname), encoding='utf-8',
                      errors='replace') as fh:
                parts.append(fh.read())
        except OSError:
            continue
    return '\n'.join(parts)


# --------------------------------------------------------------------------
# Running Jazzer with coverage on
# --------------------------------------------------------------------------

# Defined pipeline-side (see execution/coverage_flags.py) and re-exported here
# so measurement code has one place to look.
from java.execution.coverage_flags import jazzer_coverage_args  # noqa: E402,F401

# --------------------------------------------------------------------------
# The JaCoCo command-line tool
# --------------------------------------------------------------------------

#: The jar, its download URL and its version all come from `src/config.py`,
#: which is also where `src/metrics` reads them (`metrics.reached
#: .ensure_cli_jar`). One jar, one cache, one version, whichever package
#: asks for it. The fallbacks below are used only where `config` cannot be
#: imported at all — a measurement copied out of the repo — and they name
#: the same artifact.
try:                                                # pragma: no cover
    import config as _config
    JACOCO_VERSION = _config.JACOCO_VERSION
    JACOCO_CLI_JAR = _config.JACOCO_CLI_JAR
    JACOCO_CLI_URL = _config.JACOCO_CLI_URL
except Exception:                                   # pragma: no cover
    JACOCO_VERSION = os.getenv('JACOCO_VERSION', '0.8.12')
    JACOCO_CLI_JAR = os.path.expanduser(
        f'~/.cache/jacoco/jacococli-{JACOCO_VERSION}.jar')
    JACOCO_CLI_URL = (
        'https://repo.maven.apache.org/maven2/org/jacoco/org.jacoco.cli/'
        f'{JACOCO_VERSION}/org.jacoco.cli-{JACOCO_VERSION}-nodeps.jar'
    )
JACOCO_CACHE_DIR = os.path.dirname(JACOCO_CLI_JAR)


def ensure_jacoco_cli(cache_dir: Optional[str] = None) -> str:
    """Path to the JaCoCo command-line jar, downloading it once.

    Mirrors `java.execution.jazzer.JazzerEnvironment._ensure_jar`: if the
    jar is already on disk return its path, otherwise fetch it from
    Maven Central. The jar's name, its version and the URL are
    `config.JACOCO_*`, so this package and `src/metrics` download the same
    file to the same place and neither can be on a different JaCoCo.

    `JACOCO_CLI_JAR` in the environment overrides the location entirely
    (point it at a jar you already have, or at the path you want the
    download to land in); it is read live rather than at import, so a test
    can set it. `cache_dir` moves the download into another directory
    under the same file name, and the environment override wins over it.
    """
    override = os.getenv('JACOCO_CLI_JAR')
    if override:
        jar = override
        cache_dir = os.path.dirname(jar) or '.'
    elif cache_dir:
        jar = os.path.join(cache_dir, os.path.basename(JACOCO_CLI_JAR))
    else:
        jar = JACOCO_CLI_JAR
        cache_dir = os.path.dirname(jar) or '.'
    if os.path.isfile(jar):
        return jar
    os.makedirs(cache_dir, exist_ok=True)
    print(f"Downloading JaCoCo CLI from {JACOCO_CLI_URL}")
    urllib.request.urlretrieve(JACOCO_CLI_URL, jar)
    return jar


def _run(cmd: List[str]) -> subprocess.CompletedProcess:
    """The one place this module starts a process. Tests replace it."""
    return subprocess.run(cmd, capture_output=True, text=True)


def report(exec_paths: List[str],
           class_dirs: List[str],
           source_dirs: List[str],
           out_xml: str,
           jar: Optional[str] = None) -> str:
    """Turn `.exec` execution data into an XML report; return `out_xml`.

    Runs

        java -jar org.jacoco.cli.jar report <exec>... \
             --classfiles <dir> ... --sourcefiles <dir> ... --xml <out>

    Several `.exec` files may be passed at once; JaCoCo merges them,
    which is how the coverage of a multi-harness set is combined at the
    tool level rather than in Python. `--classfiles` must point at the
    compiled classes of the SAME build the harness ran against — a
    report built against the other build's classes silently mis-maps
    line numbers.

    Raises `RuntimeError` if the tool exits non-zero or writes nothing.
    """
    jar = jar or ensure_jacoco_cli()
    out_dir = os.path.dirname(os.path.abspath(out_xml))
    if out_dir:
        os.makedirs(out_dir, exist_ok=True)

    cmd = ['java', '-jar', jar, 'report']
    cmd += list(exec_paths)
    for d in class_dirs:
        cmd += ['--classfiles', d]
    for d in source_dirs:
        cmd += ['--sourcefiles', d]
    cmd += ['--xml', out_xml]

    proc = _run(cmd)
    if getattr(proc, 'returncode', 0) != 0:
        raise RuntimeError(
            f"jacoco report failed ({proc.returncode}): "
            f"{(getattr(proc, 'stderr', '') or '')[:2000]}")
    if not os.path.isfile(out_xml):
        raise RuntimeError(f"jacoco report produced no XML at {out_xml}")
    return out_xml


# --------------------------------------------------------------------------
# One archived run leg
# --------------------------------------------------------------------------

def _split_exec_name(stem: str) -> Optional[tuple]:
    """'attempt_003_patched' -> ('attempt_003', 'patched').

    The build is the LAST underscore-separated field, because harness
    names contain underscores themselves (`attempt_003`), which is also
    why no build token may contain one."""
    if '_' not in stem:
        return None
    harness, build = stem.rsplit('_', 1)
    if build not in BUILDS or not harness:
        return None
    return harness, build


def _dirs_for_build(dirs: List[str], build: str) -> List[str]:
    """The class/source directories belonging to ONE build.

    `classpath.json` lists both builds' directories (`classes_buggy`,
    `classes_patched`, and the two checkouts' source roots). A JaCoCo
    report must see only the classes that were actually instrumented for
    that dump: the same class from the other build has a different class
    id, so its data would not match and every class would appear twice.
    A directory belongs to the patched build when any path segment
    mentions 'patched'; otherwise to the buggy build. When nothing
    matches (an older layout) all directories are used, as before.

    `compiled` is the acceptance gate's run of every compiled candidate,
    and `remeasure` is the fixed-budget re-run of the kept set; both happen
    on the BUGGY build, so both take the buggy build's directories —
    anything that is not `patched` does.
    """
    def is_patched(d: str) -> bool:
        return any('patched' in seg for seg in d.replace('\\', '/').split('/'))
    want_patched = (build == BUILD_PATCHED)
    picked = [d for d in dirs if is_patched(d) == want_patched]
    return picked or list(dirs)


def _merged_xml_path(cov_dir: str, build: str) -> str:
    """Where one build's merged report lives: `cov/merged_<build>.xml`."""
    return os.path.join(cov_dir, MERGED_XML.format(build=build))


def collect_leg(leg_dir: str) -> Dict[str, Coverage]:
    """Coverage for one run leg, one `Coverage` per build.

    Expects the leg to have been fuzzed with `jazzer_coverage_args`, so
    that it looks like::

        <leg_dir>/cov/<harness>_<build>.exec     one per harness+build
        <leg_dir>/cov/classpath.json             where the classes live

    Three build tokens appear here (`BUILDS`): `buggy` and `patched` are
    the KEPT harnesses on the two builds, and `compiled` is every compiled
    candidate on the buggy build, from the acceptance gate's own run. They
    are grouped and unioned separately, so a leg with a `compiled` dump
    gets a third output file, `coverage_compiled.json`. The fourth token,
    `remeasure`, is not produced here: it is a re-run rather than a reading
    of what the leg already left behind (`remeasure_leg`).

    Each build's methods are then repaired against that build's own raw
    fuzzer output, `fuzz_out/<harness>_<build>.txt`: a method that throws
    through its only call reads as missed from JaCoCo's probes alone, and a
    stack frame is proof it ran (`frame_methods`). The frames are unioned
    into `methods`, and `methods_from_probes` / `frame_methods` keep the
    two provenances apart.

    `classpath.json` holds ``{"class_dirs": [...], "source_dirs": [...],
    "include_glob": "org.jfree.**"}``. Both directory lists are the
    arguments handed to the JaCoCo CLI; `include_glob` is reused as the
    package filter when the XML is parsed, so the report and the parse
    agree on what "the library" is.

    For each `.exec` an XML report is written NEXT TO IT
    (`<harness>_<build>.xml`) and reused if it already exists — running
    the CLI is the slow part and the archived `.exec` never changes.

    Per BUILD a second, MERGED report is then produced: every `.exec` of
    that build in ONE `jacoco report` call, written to
    `cov/merged_<build>.xml`. That report is where the build's `Coverage`
    is read from, because it is the only place the harness SET's BRANCH
    counts are correct — the per-harness XMLs carry counts, not branch
    identities, so they cannot be unioned exactly (module docstring).
    Everything else in the object is taken from the same report too, so
    one build's numbers all come from one place; methods and lines are
    unaffected by the choice, since JaCoCo's merge and a set union give
    the same answer for them. A build with a single `.exec` needs no
    second call: its per-harness report already IS the merged one.

    When no merged report can be produced — a leg archived with its XML
    reports but WITHOUT its `.exec` files, or a JaCoCo CLI that will not
    run — the per-harness reports are unioned instead and the result
    records `branches_from = BRANCHES_UNION`, an upper bound on the
    branch counts. A leg with no `.exec` files at all still yields
    coverage: the `<harness>_<build>.xml` reports left in `cov/` are read
    directly.

    The per-build results are written to
    `<leg_dir>/measurements/coverage_<build>.json` and returned.

    Builds with no coverage of either kind are simply absent from the
    result.
    """
    cov_dir = os.path.join(leg_dir, 'cov')
    with open(os.path.join(cov_dir, 'classpath.json')) as fh:
        cp = json.load(fh)
    class_dirs = list(cp.get('class_dirs') or [])
    source_dirs = list(cp.get('source_dirs') or [])
    include_glob = cp.get('include_glob') or ''
    prefix = _normalise_prefix(include_glob) if include_glob else None

    names = sorted(os.listdir(cov_dir))
    per_build: Dict[str, List[Coverage]] = {}
    execs: Dict[str, List[str]] = {}
    xmls: Dict[str, List[str]] = {}

    for fname in names:
        if not fname.endswith('.exec'):
            continue
        split = _split_exec_name(fname[:-len('.exec')])
        if split is None:
            continue
        harness, build = split
        exec_path = os.path.join(cov_dir, fname)
        xml_path = os.path.join(cov_dir, fname[:-len('.exec')] + '.xml')
        if not os.path.isfile(xml_path):
            report([exec_path], _dirs_for_build(class_dirs, build),
                   _dirs_for_build(source_dirs, build), xml_path)
        cov = parse_jacoco_xml(xml_path, include_prefix=prefix)
        cov.build, cov.harness = build, harness
        per_build.setdefault(build, []).append(cov)
        execs.setdefault(build, []).append(exec_path)
        xmls.setdefault(build, []).append(xml_path)

    # An archive that kept the reports but not the execution data: read
    # the per-harness XMLs straight off disk. `merged_<build>.xml` is not
    # one of them — it is this function's own output — and is skipped.
    if not execs:
        for fname in names:
            if not fname.endswith('.xml') or fname.startswith('merged_'):
                continue
            split = _split_exec_name(fname[:-len('.xml')])
            if split is None:
                continue
            harness, build = split
            xml_path = os.path.join(cov_dir, fname)
            cov = parse_jacoco_xml(xml_path, include_prefix=prefix)
            cov.build, cov.harness = build, harness
            per_build.setdefault(build, []).append(cov)
            xmls.setdefault(build, []).append(xml_path)

    out_dir = os.path.join(leg_dir, 'measurements')
    os.makedirs(out_dir, exist_ok=True)
    result: Dict[str, Coverage] = {}
    for build, covs in sorted(per_build.items()):
        harness = '+'.join(sorted(c.harness for c in covs if c.harness))
        merged_xml = _merged_report(cov_dir, build, execs.get(build) or [],
                                    xmls.get(build) or [],
                                    class_dirs, source_dirs)
        if merged_xml is not None:
            merged = parse_jacoco_xml(merged_xml, include_prefix=prefix)
            merged.branches_from = BRANCHES_MERGED
        else:
            merged = union(covs)
        merged.build, merged.harness = build, harness
        # The probe repair, on the build's OWN raw fuzzer output. A method
        # that throws through its only call reads as missed from probes
        # alone (`frame_methods`), and every bug in the crashing split ends
        # in a throw. Without a merged report there is no line-ownership
        # map to resolve a frame against, so that leg keeps the probe set
        # and records no frames.
        if merged_xml is not None:
            text = fuzzer_output(leg_dir, build)
            if text:
                repair_from_frames(merged, merged_xml, text, prefix)
        result[build] = merged
        with open(os.path.join(out_dir, f'coverage_{build}.json'), 'w') as fh:
            json.dump(merged.to_dict(), fh, indent=1)
    return result


def _merged_report(cov_dir: str, build: str, exec_paths: List[str],
                   xml_paths: List[str], class_dirs: List[str],
                   source_dirs: List[str]) -> Optional[str]:
    """The XML holding ONE build's whole harness set, or None.

    Three cases, in order:

    * one `.exec` — its own per-harness report already covers the whole
      set, so it is returned as-is and no second CLI call is made;
    * several `.exec` — they go into one `jacoco report` call whose output
      is `cov/merged_<build>.xml`, reused when it is already on disk;
    * no `.exec` — nothing can be merged, so None comes back and the
      caller falls back to `union`.

    A CLI failure is also None rather than an exception: a merged report
    is an improvement on the union, not a precondition for measuring a
    leg at all.
    """
    if not exec_paths:
        return None
    if len(exec_paths) == 1:
        return xml_paths[0] if xml_paths else None
    out_xml = _merged_xml_path(cov_dir, build)
    if os.path.isfile(out_xml):
        return out_xml
    try:
        return report(sorted(exec_paths), _dirs_for_build(class_dirs, build),
                      _dirs_for_build(source_dirs, build), out_xml)
    except (RuntimeError, OSError):
        return None


# --------------------------------------------------------------------------
# Re-measuring a leg on a fixed input budget
# --------------------------------------------------------------------------

def remeasure_leg(leg_dir: str, runs: int = 20000, keep_going: int = 1000,
                  buggy_dir: Optional[str] = None,
                  includes: Optional[str] = None,
                  timeout_seconds: int = 300) -> Optional[Coverage]:
    """Re-run a leg's KEPT harnesses on the buggy build with `-runs=N`.

    Two different questions, two build tokens:

    * `buggy` is the AS-RUN coverage — the harnesses as the pipeline
      actually ran them, under the wall-clock budget the verdict rests on.
      It is the honest record of the run, and it is also machine-dependent:
      a loaded machine gets through fewer inputs in 20 seconds, so the same
      archive re-measured elsewhere would not give the same set.
    * `remeasure` is the same harnesses on the same build with a FIXED
      INPUT budget (`-runs`, `--keep_going`) instead of a clock. The number
      of inputs is the experiment's parameter, so two machines agree.

    Which harnesses: `result.jsonl`'s `accepted_harnesses`, the field
    `java.run` records for exactly this purpose (harness path, class name,
    classpath, attempt label). A leg without it — an archive older than
    that field — returns None rather than a coverage of nothing.

    The run itself is `metrics.collect.harness_coverage`, imported from the
    sibling package: it already knows the three rules a measurement pass
    has to follow (a large `--keep_going`, because an accepted harness
    crashes the buggy build by design; `-runs` rather than a clock; one
    dump per harness, merged by JaCoCo). The dumps land in
    `<leg>/cov/remeasure/<attempt>.exec` and the merged report beside them
    as `jacoco.xml`, and the parsed result is written to
    `<leg>/measurements/coverage_remeasure.json`.

    The import goes one way only: this package may read `metrics`, and
    `metrics` never reads this one (see the README, "Relation to
    src/metrics").
    """
    from metrics import collect                      # one-way: see docstring

    result = {}
    result_path = os.path.join(leg_dir, 'result.jsonl')
    if os.path.isfile(result_path):
        with open(result_path, encoding='utf-8', errors='replace') as fh:
            for line in fh:
                line = line.strip()
                if line:
                    result = json.loads(line)
                    break
    accepted = list(result.get('accepted_harnesses') or [])
    if not accepted:
        return None

    if buggy_dir is None:
        project = result.get('project')
        bug_id = result.get('bug_id')
        if not project or bug_id is None:
            raise RuntimeError('no project/bug_id in result.jsonl, so the '
                               'buggy checkout cannot be located')
        buggy_dir = collect.ensure_buggy_build(project, bug_id)

    if includes is None:
        cp_path = os.path.join(leg_dir, 'cov', 'classpath.json')
        includes = ''
        if os.path.isfile(cp_path):
            with open(cp_path) as fh:
                includes = json.load(fh).get('include_glob') or ''

    out_dir = os.path.join(leg_dir, 'cov', BUILD_REMEASURE)
    run = collect.harness_coverage(buggy_dir, accepted, out_dir,
                                   includes=includes, runs=runs,
                                   keep_going=keep_going,
                                   timeout_seconds=timeout_seconds)
    prefix = _normalise_prefix(includes) if includes else None
    cov = parse_jacoco_xml(run.report, include_prefix=prefix)
    cov.branches_from = BRANCHES_MERGED
    cov.build = BUILD_REMEASURE
    cov.harness = '+'.join(sorted(
        str(e.get('attempt_label') or '') for e in accepted
        if e.get('attempt_label')))
    # The same probe repair the as-run coverage gets, from this pass's own
    # Jazzer output rather than the leg's archived one.
    if run.trace:
        repair_from_frames(cov, run.report, run.trace, prefix)

    mdir = os.path.join(leg_dir, 'measurements')
    os.makedirs(mdir, exist_ok=True)
    with open(os.path.join(mdir, f'coverage_{BUILD_REMEASURE}.json'),
              'w') as fh:
        json.dump(cov.to_dict(), fh, indent=1)
    return cov
