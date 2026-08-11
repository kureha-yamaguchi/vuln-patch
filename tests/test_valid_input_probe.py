"""The valid-by-construction probe: is the tier-2 alarm's own premise true?

Pre-registered in ``docs/reportable-exception-prereg-2026-08-09.md``
(2026-08-11 section). A tier-2/unexpected-exception firing convicts on the
premise that its input was valid-by-construction; 8.41's Chart-7-c and
Chart-26-c false positives are firings on inputs the relation wrongly
DECLARED valid. Before such a firing may convict, the probe replays the
exact firing input through the SAME check compiled against the BUGGY build:
same exception type there means the input was never valid — the firing is
DEMOTED via ``[fact:input-invalid-on-both]`` (judge-visible, never a
terminal dismissal); a different or absent exception is a discriminating
fact and the conviction stands; a failed measurement states nothing.

Four pieces, all testable without a JVM:

  * ``evidence_facts.tier2_exception_type`` — the message parser.
  * ``evidence_facts.valid_input_probe_reading`` / ``_demotes`` / ``_fact``
    — the classification and the wording. Fail-closed everywhere.
  * the ``run.py`` hook — pinned by reading the source, same as the
    shadow-isolation hook it sits beside.
  * the gates' offline halves: G-V2 (the Chart-7-c/Chart-26-c shapes read
    invalid-on-both under a same-type buggy reading — the archived crash
    artifacts were pruned with co/, so the unit form is the offline half
    and the live canary carries the rest) and G-V3 (the 11 archived
    Chart-19 tier-2 firings from the rex replay study must all read
    discriminating — buggy returns -1, no throw — with ZERO demotions).
"""
import json
import os
import re

import pytest

from java.execution.oracle_mute import instrument_for_counting
from java.relations.evidence_facts import (
    INVALID_ON_BOTH_FACT_TAG, TIER2_MARK, terminal_profile,
    tier2_exception_type, valid_input_probe_demotes, valid_input_probe_fact,
    valid_input_probe_reading)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
_REX_PHASE2 = os.path.join(ROOT, 'runs-archive', 'runs',
                           'rex_replay_20260809_074539', 'phase2_cases.jsonl')

# The draw-05 exemplar, verbatim from docs/rex-replay-study-2026-08-09.md.
_CHART19_EXEMPLAR = (
    '[relfire] relation objectlist-indexof-null-absent-is-minus-one '
    'violated: unexpected java.lang.IllegalArgumentException on '
    "valid-by-construction input: Null 'object' argument. "
    '__consumed=i:0 __rcvstate list:ObjectList size=0 increment=8')

# The two 8.41 regressions, verbatim fragments from docs/plan.md 8.41.
_CHART7_FIRING = (
    '[relfire] relation clone_preserves_series_observables violated: '
    'unexpected java.lang.IndexOutOfBoundsException from clone on '
    'valid-by-construction input')
_CHART26_FIRING = (
    '[relfire] relation area-chart-draw-null-info-no-exception violated: '
    'unexpected java.lang.StringIndexOutOfBoundsException on '
    'valid-by-construction input')


# ---------------------------------------------------------------------------
# 1 — the parser.
# ---------------------------------------------------------------------------

def test_the_exemplar_parses_to_its_exception_type():
    assert tier2_exception_type(_CHART19_EXEMPLAR) == \
        'java.lang.IllegalArgumentException'


def test_prose_between_type_and_on_does_not_break_the_parse():
    # Chart-7-c's live message interposes "from clone" before "on".
    assert tier2_exception_type(_CHART7_FIRING) == \
        'java.lang.IndexOutOfBoundsException'


@pytest.mark.parametrize('msg', [
    None, '',
    # The relation's own value comparison — not a tier-2 alarm.
    '[relfire] relation r violated: actual=1.0 expected=2.0',
    # An escaped exception with no tier-2 wording.
    'java.lang.IllegalArgumentException: bad input',
])
def test_everything_that_is_not_a_tier2_alarm_parses_to_none(msg):
    assert tier2_exception_type(msg) is None


def test_the_mark_is_the_shape_the_synthesis_prompt_mandates():
    """The probe keys on the exact literal relation_synth mandates and the
    rex study greps for; if either moves, this pin moves with it."""
    assert TIER2_MARK == 'violated: unexpected '
    from java.studies.rex_replay import TIER2_MARK as _STUDY_MARK
    assert _STUDY_MARK == TIER2_MARK


# ---------------------------------------------------------------------------
# 2 — the classification. Only a same-type buggy throw demotes; everything
#     unmeasured resolves to nothing.
# ---------------------------------------------------------------------------

def test_same_exception_type_on_buggy_reads_invalid_on_both():
    buggy = (_CHART19_EXEMPLAR
             .replace(' size=0 increment=8', ' size=0 increment=4'))
    read = valid_input_probe_reading(_CHART19_EXEMPLAR, 'fired', buggy)
    assert read['reading'] == 'invalid-on-both'
    assert read['patched_type'] == 'java.lang.IllegalArgumentException'
    assert read['buggy_type'] == 'java.lang.IllegalArgumentException'
    assert valid_input_probe_demotes(read)


def test_a_different_exception_type_on_buggy_is_discriminating():
    buggy = _CHART19_EXEMPLAR.replace('java.lang.IllegalArgumentException',
                                      'java.lang.NullPointerException')
    read = valid_input_probe_reading(_CHART19_EXEMPLAR, 'fired', buggy)
    assert read['reading'] == 'discriminating'
    assert not valid_input_probe_demotes(read)
    assert 'DIFFERENT exception type' in read['detail']


def test_a_buggy_value_alarm_without_an_exception_is_discriminating():
    # The check fired on buggy — but its own value comparison, not the
    # tier-2 catch. No unexpected exception was raised there.
    buggy = '[relfire] relation r violated: actual=1.0 expected=-1.0'
    read = valid_input_probe_reading(_CHART19_EXEMPLAR, 'fired', buggy)
    assert read['reading'] == 'discriminating'
    assert read['buggy_type'] is None
    assert not valid_input_probe_demotes(read)


def test_a_silent_buggy_replay_is_discriminating():
    # The genuine-catch shape: buggy runs the probe to completion.
    read = valid_input_probe_reading(_CHART19_EXEMPLAR, 'silent', None)
    assert read['reading'] == 'discriminating'
    assert 'no exception' in read['detail']
    assert not valid_input_probe_demotes(read)


@pytest.mark.parametrize('status,msg', [
    ('error', None),
    ('isolate_failed', None),
    (None, None),
])
def test_a_failed_measurement_resolves_nothing(status, msg):
    read = valid_input_probe_reading(_CHART19_EXEMPLAR, status, msg)
    assert read['reading'] == 'unresolved'
    assert not valid_input_probe_demotes(read)
    assert valid_input_probe_fact(read, {'r'}) is None


def test_a_non_tier2_patched_firing_is_outside_the_probe():
    read = valid_input_probe_reading(
        '[relfire] relation r violated: actual=1.0 expected=2.0',
        'fired', _CHART19_EXEMPLAR)
    assert read['reading'] == 'unresolved'
    assert valid_input_probe_fact(read) is None


def test_demotes_accepts_the_bare_reading_name():
    assert valid_input_probe_demotes('invalid-on-both')
    assert not valid_input_probe_demotes('discriminating')
    assert not valid_input_probe_demotes('unresolved')
    assert not valid_input_probe_demotes(None)


# ---------------------------------------------------------------------------
# 3 — the fact wording. The demotion is judge-visible and never terminal.
# ---------------------------------------------------------------------------

def _invalid_on_both_fact():
    read = valid_input_probe_reading(_CHART19_EXEMPLAR, 'fired',
                                     _CHART19_EXEMPLAR)
    return valid_input_probe_fact(
        read, {'objectlist-indexof-null-absent-is-minus-one'})


def test_the_demoting_fact_carries_the_tag_and_names_the_mechanics():
    fact = _invalid_on_both_fact()
    assert INVALID_ON_BOTH_FACT_TAG in fact
    assert 'BUGGY build' in fact
    assert 'java.lang.IllegalArgumentException' in fact
    assert 'DEMOTED' in fact and 'REJECTION' in fact
    assert 'objectlist-indexof-null-absent-is-minus-one' in fact


def test_the_demoting_fact_is_never_a_terminal_dismissal():
    """The prereg's parenthesis, mechanically: DEMOTE means the judge sees
    the fact — it must not read as either terminal profile, which would
    drop the firing before any judge did."""
    assert terminal_profile(_invalid_on_both_fact()) is None


def test_the_discriminating_fact_corroborates_and_claims_no_dismissal():
    read = valid_input_probe_reading(_CHART19_EXEMPLAR, 'silent', None)
    fact = valid_input_probe_fact(read, {'r'})
    assert INVALID_ON_BOTH_FACT_TAG not in fact
    assert 'DIFFERENTLY' in fact
    assert 'dismisses nothing' in fact
    assert terminal_profile(fact) is None


def test_the_fact_names_the_checks_when_it_knows_them():
    read = valid_input_probe_reading(_CHART19_EXEMPLAR, 'silent', None)
    assert 'alpha, beta' in valid_input_probe_fact(read, {'beta', 'alpha'})
    assert 'this check' in valid_input_probe_fact(read, None)


def test_a_non_dict_reading_states_nothing():
    assert valid_input_probe_fact(None) is None
    assert valid_input_probe_fact('invalid-on-both') is None


# ---------------------------------------------------------------------------
# 4 — the isolation transform recognises a tier-2 throw as the target's own
#     alarm (the probe's load-bearing assumption: the buggy-side replay can
#     only report the exception type if the tier-2 rethrow survives as the
#     recorded firing).
# ---------------------------------------------------------------------------

_TIER2_HARNESS = (
    'public class HarnessT2 {\n'
    '  public static void fuzzerTestOneInput('
    'com.code_intelligence.jazzer.api.FuzzedDataProvider data) {\n'
    '    org.jfree.chart.util.ObjectList list;\n'
    '    try { list = new org.jfree.chart.util.ObjectList(); }\n'
    '    catch (Exception e) { return; }\n'
    '    try {\n'
    '      int observed = list.indexOf(null);\n'
    '      if (observed != -1) {\n'
    '        throw new RuntimeException("relation '
    'objectlist-indexof-null-absent-is-minus-one violated: observed=" '
    '+ observed);\n'
    '      }\n'
    '    } catch (RuntimeException e) {\n'
    '      throw new RuntimeException("relation '
    'objectlist-indexof-null-absent-is-minus-one violated: unexpected " '
    '+ e.getClass().getName() + " on valid-by-construction input: " '
    '+ e.getMessage());\n'
    '    }\n'
    '  }\n'
    '}\n')


def test_the_tier2_rethrow_is_recorded_not_muted_when_it_is_the_target():
    out = instrument_for_counting(
        _TIER2_HARNESS, 'objectlist-indexof-null-absent-is-minus-one',
        record_firing=True)
    assert out is not None
    # Both throws carry the target id: the tally-plus-print replacement, so
    # the buggy-side run PRINTS the tier-2 message instead of dying on it.
    assert 'System.err.println("[relfire] "' in out
    assert 'violated: unexpected' in out
    assert out.count('{') == out.count('}')


# ---------------------------------------------------------------------------
# 5 — the run.py hook: same station as the shadow-isolation hook, demote is
#     a fact delivery, never a drop. Pinned by reading the source, since the
#     enclosing loop needs a whole live run to execute.
# ---------------------------------------------------------------------------

def _probe_block():
    with open(os.path.join(ROOT, 'src', 'java', 'run.py')) as fh:
        body = fh.read()
    start = body.index('VALID-BY-CONSTRUCTION PROBE')
    return body, body[start:body.index('# The firing INPUT itself')]


def test_the_probe_sits_at_the_shadow_isolation_station():
    body, block = _probe_block()
    assert body.index('SHADOW-ISOLATION READING') \
        < body.index('VALID-BY-CONSTRUCTION PROBE')
    assert 'replay_input_isolated' in block
    assert 'valid_input_probe_reading' in block
    # It reuses a reading the isolation loop already paid for.
    assert '_iso_results.get' in block


def test_the_probe_arms_only_on_a_tier2_firing_with_a_named_check():
    _body, block = _probe_block()
    assert re.search(r'if _fired_ids and _t2x\(fired\):', block)


def test_the_probe_demotes_but_never_drops_and_never_convicts():
    _body, block = _probe_block()
    assert 'drop_reasons.append' not in block
    assert 'kept_reason' not in block
    assert 'args.' not in block            # no flag consulted
    # The fact reaches both the note list and the concrete evidence.
    assert '_fact_notes.append(_vp_fact)' in block
    assert re.search(r'evid = \(\(evid \+ "\\n" \+ _vp_fact\)', block)


def test_one_deterministic_event_per_probe():
    _body, block = _probe_block()
    assert block.count("method='valid-input-probe'") == 1
    assert "'deterministic'" in block


def test_the_existing_isolation_hook_still_has_its_single_drop():
    """The probe was inserted inside the slice the shadow-isolation tests
    pin; re-assert their invariant here so a regression names this file."""
    with open(os.path.join(ROOT, 'src', 'java', 'run.py')) as fh:
        body = fh.read()
    block = body[body.index('SHADOW-ISOLATION READING'):]
    block = block[:block.index('# The firing INPUT itself')]
    assert block.count('drop_reasons.append') == 1


# ---------------------------------------------------------------------------
# 6 — G-V2, the offline (unit) half: the exact 8.41 Chart-7-c/Chart-26-c
#     firing shapes demote under a same-type buggy reading. The archived
#     legs' crash artifacts were pruned with co/ (docs/plan.md 8.41), so no
#     archived input exists to replay; the live canary carries the rest.
# ---------------------------------------------------------------------------

@pytest.mark.parametrize('firing,exc', [
    (_CHART7_FIRING, 'java.lang.IndexOutOfBoundsException'),
    (_CHART26_FIRING, 'java.lang.StringIndexOutOfBoundsException'),
])
def test_gv2_the_841_fp_shapes_demote_when_buggy_throws_the_same_type(
        firing, exc):
    assert tier2_exception_type(firing) == exc
    read = valid_input_probe_reading(firing, 'fired', firing)
    assert read['reading'] == 'invalid-on-both'
    assert valid_input_probe_demotes(read)
    fact = valid_input_probe_fact(read, None)
    assert INVALID_ON_BOTH_FACT_TAG in fact
    assert terminal_profile(fact) is None


def test_gv2_the_841_fp_shapes_do_not_demote_on_a_failed_measurement():
    """The same two shapes, unmeasured: unchanged, per the fail-closed rule
    — the probe must never manufacture the demotion it exists to earn."""
    for firing in (_CHART7_FIRING, _CHART26_FIRING):
        read = valid_input_probe_reading(firing, 'error', None)
        assert read['reading'] == 'unresolved'
        assert valid_input_probe_fact(read) is None


# ---------------------------------------------------------------------------
# 7 — G-V3, the offline half: zero demotions of any archived genuine tier-2
#     catch. The 11 Chart-19 firings of the rex replay study (the study's
#     own phase-2 fixture, messages verbatim) were all SILENT on the buggy
#     build (0/20000 — buggy returns -1, no throw), so every one must read
#     discriminating and none may demote.
# ---------------------------------------------------------------------------

def _rex_phase2_rows():
    if not os.path.exists(_REX_PHASE2):
        pytest.skip('rex replay study phase-2 fixture not present')
    return [json.loads(line) for line in open(_REX_PHASE2) if line.strip()]


def _tier2_message(row):
    fa = row['fired_assertion']
    i = fa.find('[relfire]')
    assert i >= 0, row['id']
    return fa[i:]


def test_gv3_the_fixture_is_still_the_pinned_11_chart19_firings():
    rows = _rex_phase2_rows()
    assert len(rows) == 11
    assert all('Chart-19' in r['note'] for r in rows)
    # The buggy-silent basis is the study's own screen fact, recorded per
    # case — not an assumption made here.
    assert all('silent on the buggy build' in r['concrete_evidence']
               for r in rows)


def test_gv3_every_archived_chart19_tier2_firing_reads_discriminating():
    demoted = []
    for row in _rex_phase2_rows():
        msg = _tier2_message(row)
        assert tier2_exception_type(msg) == \
            'java.lang.IllegalArgumentException', row['id']
        # Buggy returns -1, no throw: the study screened each relation on
        # the buggy build and it was silent — the probe's 'silent' outcome.
        read = valid_input_probe_reading(msg, 'silent', None)
        if read['reading'] != 'discriminating' \
                or valid_input_probe_demotes(read):
            demoted.append(row['id'])
        fact = valid_input_probe_fact(read, None)
        assert fact and INVALID_ON_BOTH_FACT_TAG not in fact
        assert terminal_profile(fact) is None
    assert demoted == [], (
        'G-V3 FAILED: %d archived genuine tier-2 catch(es) would be '
        'demoted — redesign, do not ship: %s' % (len(demoted), demoted))
