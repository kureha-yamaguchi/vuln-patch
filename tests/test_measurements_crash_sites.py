"""Tests for java.measurements.crash_sites — where a finding landed.

Two fixtures:

* `fixtures/measurements/jazzer_output_crashes.txt` — synthetic Jazzer
  output with the three shapes a crash can take: an assertion that never
  left the harness, an assertion that laundered a library crash behind a
  `Caused by:` chain, and a plain library exception.
* `fixtures/measurements/trace_excerpt_crashes.md` — a 90-line excerpt of
  a real archived trace (Chart-26, run final30B_20260729_145001), kept
  verbatim, carrying both a patched-side crash and a buggy-side one.
"""
import json
import os

from java.measurements import crash_sites as cs
from java.measurements.crash_sites import CrashSite
from java.measurements.locations import MethodRef

FIXTURES = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        'fixtures', 'measurements')
JAZZER_TXT = os.path.join(FIXTURES, 'jazzer_output_crashes.txt')
TRACE_MD = os.path.join(FIXTURES, 'trace_excerpt_crashes.md')


def jazzer_text():
    with open(JAZZER_TXT) as fh:
        return fh.read()


def sites():
    return cs.from_jazzer_output(jazzer_text(), build='patched',
                                harness='attempt_002')


# ------------------------------------------------- parsing jazzer output

def test_every_exception_block_becomes_one_crash():
    got = sites()
    assert len(got) == 3
    assert [c.build for c in got] == ['patched'] * 3
    assert [c.harness for c in got] == ['attempt_002'] * 3


def test_harness_only_assertion_has_no_site():
    """An oracle that throws from the harness body never entered the
    library, so there is nothing to score for CSM."""
    c = sites()[0]
    assert c.exception == 'com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow'
    assert c.message.startswith('[oracle:label-metadata]')
    assert c.site_kind == 'harness_only'
    assert c.top_library is None and c.top_library_line is None
    # the harness frame is kept, and flagged
    assert [str(r) for r, _ in c.frames] == [
        'org.jfree.demo.FuzzHarness.fuzzerTestOneInput()']
    assert c.harness_frames == [True]


def test_jazzer_frames_are_dropped():
    """The FuzzTargetRunner frame under the first crash is Jazzer's own
    driver, not the project."""
    c = sites()[0]
    assert not [r for r, _ in c.frames if 'jazzer' in r.class_fq]


def test_caused_by_chain_supplies_the_site():
    """The headline fired in the harness; the site is the first library
    frame of the cause — the same rule fuzz_runner.cause_signature uses
    to recover a laundered crash's identity."""
    c = sites()[1]
    assert c.causes == ['java.lang.StringIndexOutOfBoundsException']
    assert c.site_kind == 'library'
    assert str(c.top_library) == 'org.jfree.demo.TextMeasurer.getStringWidth()'
    assert c.top_library_line == 78


def test_jdk_module_prefixed_frames_are_recognised_and_dropped():
    """JDK 9+ prints `at java.base/java.lang.String.substring(...)`. The
    module prefix must not stop the frame being recognised, or a JDK
    frame would survive into the project frame list."""
    c = sites()[1]
    assert not [r for r, _ in c.frames if r.class_fq.startswith('java.')]
    # everything that survived is project code, in trace order
    assert [str(r) for r, _ in c.frames] == [
        'org.jfree.demo.FuzzHarness.runOracle()',
        'org.jfree.demo.FuzzHarness.fuzzerTestOneInput()',
        'org.jfree.demo.TextMeasurer.getStringWidth()',
        'org.jfree.demo.Widget.Inner.compute()',
        'org.jfree.demo.Widget.draw()',
    ]
    assert c.harness_frames == [True, True, False, False, False]


def test_pure_library_crash_takes_its_own_top_frame():
    c = sites()[2]
    assert c.exception == 'java.lang.NullPointerException'
    assert c.message == ''
    assert c.site_kind == 'library'
    assert str(c.top_library) == 'org.jfree.demo.Widget.draw()'
    assert c.top_library_line == 21


def test_nested_class_frames_normalise_the_dollar():
    c = sites()[1]
    assert 'org.jfree.demo.Widget.Inner' in {r.class_fq for r, _ in c.frames}


def test_empty_input_yields_nothing():
    assert cs.from_jazzer_output('') == []
    assert cs.from_jazzer_output('INFO: nothing happened\n') == []


def test_round_trip_through_dict():
    for c in sites():
        back = CrashSite.from_dict(json.loads(json.dumps(c.to_dict())))
        assert back.exception == c.exception
        assert back.frames == c.frames
        assert back.harness_frames == c.harness_frames
        assert back.top_library == c.top_library
        assert back.top_library_line == c.top_library_line
        assert back.site_kind == c.site_kind
        assert (back.build, back.harness, back.source) == (
            c.build, c.harness, c.source)


# ------------------------------------------------------ reading a trace

def test_trace_finds_both_builds():
    got = cs.from_trace(TRACE_MD)
    assert {c.build for c in got} == {'buggy', 'patched'}
    assert all(c.source.startswith('[87]') or c.source.startswith('[103]')
               for c in got)


def test_trace_patched_crashes_come_from_the_evidence_block():
    """`relation_verifier` attaches the firing's raw Jazzer output as
    <evidence> and says right above it that the assertion fired on the
    patched code, so those blocks are patched-side."""
    got = [c for c in cs.from_trace(TRACE_MD) if c.build == 'patched']
    kinds = sorted(c.site_kind for c in got)
    assert kinds.count('harness_only') == 1
    assert kinds.count('library') == 2
    laundered = next(c for c in got if c.causes)
    assert laundered.causes == ['java.lang.StringIndexOutOfBoundsException']
    assert str(laundered.top_library) == (
        'org.jfree.chart.text.G2TextMeasurer.getStringWidth()')
    assert laundered.top_library_line == 78
    assert 'evidence' in laundered.source


def test_trace_buggy_crashes_come_from_the_replay_annotations():
    """The buggy build's own Jazzer output is not archived; its identity
    survives only in the `<throwable>@<Class>.<method>` notes the run
    wrote beside the patched firing."""
    got = [c for c in cs.from_trace(TRACE_MD) if c.build == 'buggy']
    by_site = {str(c.top_library) if c.top_library else None: c for c in got}
    assert 'org.jfree.chart.axis.Axis.drawLabel()' in by_site
    npe = by_site['org.jfree.chart.axis.Axis.drawLabel()']
    assert npe.exception == 'java.lang.NullPointerException'
    assert npe.site_kind == 'library'
    assert npe.top_library_line is None      # signatures carry no line
    # the buggy headline landed in the harness, so it has no site
    assert None in by_site
    assert by_site[None].site_kind == 'harness_only'


def test_trace_dedupes_repeats_within_a_build():
    """The excerpt holds the same two evidence blocks twice (sections 87
    and 103); identical (exception, frames) collapse to one crash."""
    got = cs.from_trace(TRACE_MD)
    keys = [(c.build,) + c.dedupe_key for c in got]
    assert len(keys) == len(set(keys))
    assert len(got) == 5
    # the first section is the one whose source is kept
    assert all(c.source.startswith('[87]') for c in got)


def test_trace_does_not_read_the_build_from_a_directory_name():
    """Every Jazzer footer in the excerpt prints
    `artifact_prefix='/.../Chart_26_buggy/fuzz/...'` — the CHECKOUT
    directory, which is named after the buggy tree whichever build ran.
    Attribution must ignore it."""
    with open(TRACE_MD) as fh:
        text = fh.read()
    assert 'Chart_26_buggy/fuzz' in text
    got = cs.from_trace(TRACE_MD)
    # crashes read out of a real stack trace are the ones whose frames
    # carry source line numbers; every one of them is patched-side
    from_stack = [c for c in got
                  if any(ln is not None for _, ln in c.frames)]
    assert len(from_stack) == 2
    assert all(c.build == 'patched' for c in from_stack)


def test_trace_of_a_file_with_no_crashes():
    tmp = os.path.join(FIXTURES, 'trace_excerpt_crashes.md')
    # a trace body with headers but no findings
    import tempfile
    with tempfile.NamedTemporaryFile('w', suffix='.md', delete=False) as fh:
        fh.write('# Pipeline trace — Demo-1\n\n'
                 '## [0] screen-fuzz-buggy · `some-relation`\n'
                 '**output:** quiet on the buggy build\n')
        path = fh.name
    try:
        assert cs.from_trace(path) == []
    finally:
        os.unlink(path)
    assert os.path.isfile(tmp)


# --------------------------------------------------------- small helpers

def test_harness_detection():
    assert cs._is_harness(MethodRef('org.jfree.demo.FuzzHarness', 'helper'))
    assert cs._is_harness(MethodRef('org.jfree.demo.FuzzHarness2', 'x'))
    # the Jazzer entry point can only be a harness, whatever it is called
    assert cs._is_harness(MethodRef('org.jfree.demo.Target',
                                    'fuzzerTestOneInput'))
    assert not cs._is_harness(MethodRef('org.jfree.demo.Widget', 'draw'))


def test_infrastructure_detection():
    for c in ('com.code_intelligence.jazzer.driver.FuzzTargetRunner',
              'java.lang.String', 'jdk.internal.X', 'sun.misc.Y',
              'junit.framework.Assert',
              # added to match d4j_rcc_sweep, which filtered them from the
              # start: the JDK's extension half and JUnit 4/5
              'javax.swing.JLabel', 'javax.xml.parsers.SAXParser',
              'org.junit.Assert', 'org.junit.jupiter.api.Assertions'):
        assert cs._is_excluded(c), c
    assert not cs._is_excluded('org.jfree.chart.axis.Axis')
    # a project package that merely STARTS like an excluded one is not
    # excluded: the test is on the package boundary
    assert not cs._is_excluded('javaxtools.compiler.Foo')
    assert not cs._is_excluded('org.junitish.Helper')


def test_signature_without_a_frame_is_not_a_crash_site():
    """'none recorded' and a bare throwable name give no location; a
    crash with no site would be scored as a CSM miss it never earned."""
    assert cs._sig_site('none recorded', 'buggy', '', 'x') is None
    assert cs._sig_site('StringIndexOutOfBoundsException', 'buggy', '',
                        'x') is None
    got = cs._sig_site('java.lang.NullPointerException@org.jfree.A.b',
                       'buggy', '', 'x')
    assert got is not None and str(got.top_library) == 'org.jfree.A.b()'


# ------------------------------------------- the cause chain is the site

#: A headline that has a library frame OF ITS OWN and a `Caused by:` chain
#: under it. The old rule scored the headline frame; the site is the
#: deepest cause's first library frame, because that is where the fault
#: was, and the headline frame is kept beside it for reference.
LAUNDERED_WITH_HEADLINE_FRAME = """\
== Java Exception: java.lang.IllegalStateException: renderer said no
\tat org.jfree.demo.Plot.report(Plot.java:400)
\tat org.jfree.demo.FuzzHarness.fuzzerTestOneInput(FuzzHarness.java:30)
Caused by: java.lang.RuntimeException: wrapped
\tat org.jfree.demo.Middle.relay(Middle.java:120)
Caused by: java.lang.ArithmeticException: / by zero
\tat org.jfree.demo.Deep.divide(Deep.java:88)
\tat org.jfree.demo.Middle.relay(Middle.java:118)
\t... 3 more
== libFuzzer crashing input ==
"""


def test_deepest_cause_is_the_site_and_the_headline_is_kept_beside_it():
    c = cs.from_jazzer_output(LAUNDERED_WITH_HEADLINE_FRAME)[0]
    assert c.causes == ['java.lang.RuntimeException',
                        'java.lang.ArithmeticException']
    # the DEEPEST cause, not the headline and not the middle link
    assert str(c.top_library) == 'org.jfree.demo.Deep.divide()'
    assert c.top_library_line == 88
    # the headline's own first library frame is recorded, never scored
    assert str(c.headline_site) == 'org.jfree.demo.Plot.report()'
    assert c.headline_site_line == 400
    assert c.site_kind == 'library'


def test_headline_stands_in_when_the_chain_has_no_library_frame():
    """A cause whose frames are all JDK leaves nothing to score, so the
    headline's own library frame is the site rather than nothing."""
    text = """\
== Java Exception: java.lang.IllegalStateException: bad state
\tat org.jfree.demo.Plot.report(Plot.java:400)
Caused by: java.lang.NumberFormatException: For input string: "x"
\tat java.base/java.lang.Integer.parseInt(Integer.java:652)
== libFuzzer crashing input ==
"""
    c = cs.from_jazzer_output(text)[0]
    assert str(c.top_library) == 'org.jfree.demo.Plot.report()'
    assert c.top_library_line == 400
    assert c.headline_site == c.top_library


def test_a_crash_with_no_chain_has_headline_site_equal_to_the_site():
    c = sites()[2]
    assert c.headline_site == c.top_library
    assert c.headline_site_line == c.top_library_line


def test_headline_site_survives_the_round_trip():
    c = cs.from_jazzer_output(LAUNDERED_WITH_HEADLINE_FRAME)[0]
    back = CrashSite.from_dict(json.loads(json.dumps(c.to_dict())))
    assert back.headline_site == c.headline_site
    assert back.headline_site_line == c.headline_site_line
    assert back.top_library == c.top_library


def test_a_record_written_before_headline_site_existed_reads_back():
    d = sites()[2].to_dict()
    del d['headline_site']
    del d['headline_site_line']
    back = CrashSite.from_dict(d)
    assert back.headline_site is None
    assert back.top_library == sites()[2].top_library


# --------------------------------------- the harness by its record names

#: A harness the generator did NOT name `FuzzHarness*`: only the run
#: record knows it is a harness.
ODD_NAMED_HARNESS = """\
== Java Exception: java.lang.AssertionError: oracle disagreed
\tat org.jfree.demo.OracleProbe$Body.check(OracleProbe.java:55)
\tat org.jfree.demo.OracleProbe.entry(OracleProbe.java:20)
\tat org.jfree.demo.Widget.draw(Widget.java:21)
== libFuzzer crashing input ==
"""


def test_harness_detection_uses_the_record_class_names():
    """Without the record the probe class looks like library code and
    takes the site; with it, the site is the first frame below it."""
    without = cs.from_jazzer_output(ODD_NAMED_HARNESS)[0]
    assert str(without.top_library) == 'org.jfree.demo.OracleProbe.Body.check()'

    with_record = cs.from_jazzer_output(
        ODD_NAMED_HARNESS,
        harness_classes=['org.jfree.demo.OracleProbe$Body',
                         'org.jfree.demo.OracleProbe'])[0]
    assert str(with_record.top_library) == 'org.jfree.demo.Widget.draw()'
    assert with_record.top_library_line == 21
    # the harness frames are kept and flagged, as before
    assert with_record.harness_frames == [True, True, False]


def test_record_names_normalise_the_dollar_like_frames_do():
    """A nested harness class is written `Outer$Inner` in the record and
    `Outer.Inner` on a frame; both spellings must match."""
    ref = MethodRef('org.jfree.demo.OracleProbe.Body', 'check')
    assert cs._is_harness(ref, ['org.jfree.demo.OracleProbe$Body'])
    assert cs._is_harness(ref, ['org.jfree.demo.OracleProbe.Body'])
    assert not cs._is_harness(ref, ['org.jfree.demo.Other'])


def test_the_shape_rule_still_stands_without_a_record():
    """`from_trace` has no run record beside it, so the FuzzHarness* /
    fuzzerTestOneInput rule has to work alone."""
    assert cs._is_harness(MethodRef('org.jfree.demo.FuzzHarness', 'helper'),
                          [])
    assert cs._is_harness(MethodRef('org.jfree.demo.Target',
                                    'fuzzerTestOneInput'), None)
    got = cs.from_trace(TRACE_MD)
    assert [c for c in got if c.site_kind == 'harness_only']
