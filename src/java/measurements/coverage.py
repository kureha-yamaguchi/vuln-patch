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
                                         top_level_of)

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

#: Every build token a `.exec` dump can carry. No token may contain an
#: underscore: `_split_exec_name` splits a dump's name on the LAST one.
BUILDS = (BUILD_BUGGY, BUILD_PATCHED, BUILD_COMPILED)

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

    methods           methods with JaCoCo METHOD counter covered > 0
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
    lines: Set[LineRef] = field(default_factory=set)
    branches_covered: int = 0
    branches_total: int = 0
    line_branches: Dict[LineRef, Tuple[int, int]] = field(default_factory=dict)
    branches_from: str = ''
    all_methods: Set[MethodRef] = field(default_factory=set)
    build: str = ''
    harness: str = ''

    def to_dict(self) -> dict:
        return {
            'methods': [m.to_dict() for m in sorted(self.methods)],
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
        return cls(
            methods={MethodRef.from_dict(m) for m in d.get('methods', [])},
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

    return cov


# --------------------------------------------------------------------------
# Running Jazzer with coverage on
# --------------------------------------------------------------------------

# Defined pipeline-side (see execution/coverage_flags.py) and re-exported here
# so measurement code has one place to look.
from java.execution.coverage_flags import jazzer_coverage_args  # noqa: E402,F401

# --------------------------------------------------------------------------
# The JaCoCo command-line tool
# --------------------------------------------------------------------------

JACOCO_VERSION = os.getenv('JACOCO_VERSION', '0.8.12')
JACOCO_CLI_URL = (
    'https://repo1.maven.org/maven2/org/jacoco/org.jacoco.cli/'
    f'{JACOCO_VERSION}/org.jacoco.cli-{JACOCO_VERSION}-nodeps.jar'
)
JACOCO_CACHE_DIR = os.path.expanduser('~/.cache/jacoco')


def ensure_jacoco_cli(cache_dir: Optional[str] = None) -> str:
    """Path to the JaCoCo command-line jar, downloading it once.

    Mirrors `java.execution.jazzer.JazzerEnvironment._ensure_jar`: if the
    jar is already on disk return its path, otherwise fetch it from
    Maven Central into the cache directory. The 'nodeps' classifier is
    the shaded build that runs with plain `java -jar`.

    `JACOCO_CLI_JAR` overrides the location entirely (point it at a jar
    you already have, or at the path you want the download to land in).
    """
    override = os.getenv('JACOCO_CLI_JAR')
    if override:
        jar = override
        cache_dir = os.path.dirname(jar) or '.'
    else:
        cache_dir = cache_dir or JACOCO_CACHE_DIR
        jar = os.path.join(
            cache_dir, f'org.jacoco.cli-{JACOCO_VERSION}-nodeps.jar')
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
    and that gate runs on the BUGGY build, so it takes the buggy build's
    directories — anything that is not `patched` does.
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

    Three build tokens appear (`BUILDS`): `buggy` and `patched` are the
    KEPT harnesses on the two builds, and `compiled` is every compiled
    candidate on the buggy build, from the acceptance gate's own run. They
    are grouped and unioned separately, so a leg with a `compiled` dump
    gets a third output file, `coverage_compiled.json`.

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
