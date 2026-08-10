"""Shadow isolation: read the buggy-side value a sibling oracle hid.

Pre-registered in ``docs/math65-formula-read-2026-08-10.md``. The full-harness
buggy replay runs every oracle; when a DIFFERENT one throws first the JVM dies
there, the firing check's own message is never printed, and the value verdict
comes back ``unknown`` — the exact state in which the judge is told to fall
back on the check's stated contract, which is how six consecutive Math-65 legs
convicted a correct patch on relations that fire identically on both builds.

Three pieces, all testable without a JVM:

  * ``oracle_mute.instrument_for_counting(src, id, record_firing=True)``
    — the isolation transform. Every sibling alarm muted, the target's own
    throw kept but printed instead of fatal.
  * ``FuzzRunner.replay_input_isolated`` — the composition (read source,
    transform, compile, replay ONE input, harvest the message). Stubbed
    builder, stubbed Jazzer.
  * ``evidence_facts.isolated_value_reading`` / ``isolation_dismisses`` /
    ``isolation_reading_fact`` — the arithmetic and the wording. Two numbers
    and a subtraction; anything they cannot decide changes nothing.

G-S2 (the pre-registered no-catch-killed gate) lives at the bottom: neither
dismissal condition may hold on any row of the 67-row genuine-catch guard
population, nor on the archived keep-finding firings that carry real values.
"""
import json
import os
import re

import pytest

from java.execution.oracle_mute import instrument_for_counting
from java.relations.evidence_facts import (
    ISOLATION_FACT_TAG, isolated_value_reading, isolation_dismisses,
    isolation_reading_fact, terminal_profile)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
_FIXTURES = os.path.join(ROOT, 'tests', 'fixtures', 'harness_sources.json')
_GUARD = os.path.join(ROOT, 'docs', 'replay', 'backtrack',
                      'guard_population.json')
_CASES228 = os.path.join(ROOT, 'tests', 'fixtures', 'cases228.jsonl')


def _harness(name):
    with open(_FIXTURES) as fh:
        return json.load(fh)[name]['source']


# ---------------------------------------------------------------------------
# 1 — the arithmetic. Identity, strictly-closer, and every ambiguous shape.
# ---------------------------------------------------------------------------

def test_identical_values_on_both_builds_dismiss():
    patched = ('[oracle:chi] relation chiSquare_matches_doc violated: '
               'chi=2.2222 expected=3.5556 tol=1.0E-9')
    buggy = ('[oracle:chi] relation chiSquare_matches_doc violated: '
             'chi=2.2222 expected=3.5556 tol=1.0E-9')
    read = isolated_value_reading(patched, buggy)
    assert read['reading'] == 'identical'
    assert isolation_dismisses(read)


def test_identical_tolerates_the_rounding_floor_but_not_a_real_difference():
    base = '[oracle:c] violated: actual=%s expected=3.5556'
    near = isolated_value_reading(base % '2.2222000000000001', base % '2.2222')
    assert near['reading'] == 'identical'
    far = isolated_value_reading(base % '2.2300', base % '2.2222')
    assert far['reading'] != 'identical'


def test_patched_strictly_closer_to_the_checks_own_expected_dismisses():
    # The check's own yardstick: expected 3.9375. Patched 4.5636 is 0.63 away,
    # buggy 0.0564 is 3.88 away — the patch moved this observable TOWARD the
    # value the check demands.
    patched = '[oracle:chi] violated: chi=4.5636 expected=3.9375'
    buggy = '[oracle:chi] violated: chi=0.0564 expected=3.9375'
    read = isolated_value_reading(patched, buggy)
    assert read['reading'] == 'patched-closer'
    assert isolation_dismisses(read)
    assert read['key'] == 'chi'
    assert read['expected'] == pytest.approx(3.9375)


def test_buggy_closer_is_corroborating_and_never_dismisses():
    # The genuine-catch shape: the buggy build satisfies the check and the
    # patched build does not. Stated as a fact; dismisses nothing.
    patched = '[oracle:m] violated: actual=99.0 expected=1.0'
    buggy = '[oracle:m] violated: actual=1.5 expected=1.0'
    read = isolated_value_reading(patched, buggy)
    assert read['reading'] == 'buggy-closer'
    assert not isolation_dismisses(read)


@pytest.mark.parametrize('patched,buggy', [
    # No isolated reading at all — the commonest failure, and the whole
    # fail-closed rule in one case.
    ('[oracle:c] violated: actual=4.0 expected=1.0', None),
    ('[oracle:c] violated: actual=4.0 expected=1.0', ''),
    # No expected key: nothing to measure closeness against.
    ('[oracle:c] violated: actual=4.0', '[oracle:c] violated: actual=9.0'),
    # The two builds disagree about the expected value, so there is no shared
    # yardstick (this is the check reading its own inputs differently).
    ('[oracle:c] violated: actual=4.0 expected=1.0',
     '[oracle:c] violated: actual=9.0 expected=2.0'),
    # More than one observable differs — which one is the check about?
    ('[oracle:c] violated: a=4.0 b=7.0 expected=1.0',
     '[oracle:c] violated: a=9.0 b=2.0 expected=1.0'),
    # Equidistant: neither build is closer.
    ('[oracle:c] violated: actual=3.0 expected=2.0',
     '[oracle:c] violated: actual=1.0 expected=2.0'),
    # Non-finite: distances are not comparable.
    ('[oracle:c] violated: actual=NaN expected=2.0',
     '[oracle:c] violated: actual=1.0 expected=2.0'),
    ('[oracle:c] violated: actual=Infinity expected=2.0',
     '[oracle:c] violated: actual=1.0 expected=2.0'),
    # No numbers at all on the buggy side.
    ('[oracle:c] violated: actual=4.0 expected=1.0',
     '[oracle:c] violated: the optimizer refused the input'),
])
def test_every_unresolved_shape_stays_ambiguous_and_dismisses_nothing(
        patched, buggy):
    read = isolated_value_reading(patched, buggy)
    assert read['reading'] == 'ambiguous'
    assert not isolation_dismisses(read)
    assert read['detail']


def test_reference_keys_alone_can_never_read_identical():
    """The one precondition on top of ``compare_fired_values``.

    ``expected=``/``tol=`` are the check's own constants: equal on both builds
    by construction. A pair of messages that share ONLY those would compare
    "identical" and dismiss a firing whose observed values were never compared
    at all. Requiring one shared OBSERVED key closes that."""
    patched = '[oracle:c] violated: patchedSide=4.0 expected=1.0 tol=1.0E-9'
    buggy = '[oracle:c] violated: buggySide=9.0 expected=1.0 tol=1.0E-9'
    from java.relations.evidence_facts import compare_fired_values
    assert compare_fired_values(patched, buggy) == 'identical'
    read = isolated_value_reading(patched, buggy)
    assert read['reading'] == 'ambiguous'
    assert 'reference keys' in read['detail']


def test_textually_identical_messages_read_identical_without_any_numbers():
    msg = '[oracle:c] violated: the value is not a number'
    read = isolated_value_reading(msg, msg)
    assert read['reading'] == 'identical'
    assert isolation_dismisses(read)


def test_isolation_dismisses_accepts_the_bare_reading_name():
    assert isolation_dismisses('identical')
    assert isolation_dismisses('patched-closer')
    assert not isolation_dismisses('buggy-closer')
    assert not isolation_dismisses('ambiguous')
    assert not isolation_dismisses(None)


# ---------------------------------------------------------------------------
# 2 — the fact wording.
# ---------------------------------------------------------------------------

def test_the_identical_fact_names_both_values_under_the_tag():
    read = isolated_value_reading(
        '[oracle:chi] violated: chi=2.2222 expected=3.5556',
        '[oracle:chi] violated: chi=2.2222 expected=3.5556')
    fact = isolation_reading_fact(read, {'chi'})
    assert ISOLATION_FACT_TAG in fact
    assert 'SHADOWED' in fact and 'ISOLATION' in fact
    assert '2.2222' in fact
    assert 'chi' in fact


def test_the_patched_closer_fact_names_expected_and_both_values():
    read = isolated_value_reading(
        '[oracle:chi] violated: chi=4.5636 expected=3.9375',
        '[oracle:chi] violated: chi=0.0564 expected=3.9375')
    fact = isolation_reading_fact(read, {'chi'})
    assert ISOLATION_FACT_TAG in fact
    for number in ('3.9375', '4.5636', '0.0564'):
        assert number in fact
    assert 'strictly CLOSER' in fact


def test_the_buggy_closer_fact_is_stated_but_claims_no_conviction():
    read = isolated_value_reading(
        '[oracle:m] violated: actual=99.0 expected=1.0',
        '[oracle:m] violated: actual=1.5 expected=1.0')
    fact = isolation_reading_fact(read, {'m'})
    assert ISOLATION_FACT_TAG in fact
    assert 'AWAY' in fact
    assert 'decides nothing by itself' in fact
    # It must not read as a terminal identical-on-both fact: that would drop
    # the very firing it corroborates.
    assert terminal_profile(fact) is None


def test_an_unresolved_reading_states_nothing():
    """Fail-closed in the evidence too: an ambiguous measurement leaves the
    judge's input byte-for-byte as it was."""
    assert isolation_reading_fact(
        isolated_value_reading('[oracle:c] violated: a=1.0', None)) is None
    assert isolation_reading_fact(None) is None
    assert isolation_reading_fact('identical') is None


def test_the_fact_names_the_checks_that_fired_when_it_knows_them():
    read = isolated_value_reading('[oracle:c] v: a=1.0 expected=2.0',
                                  '[oracle:c] v: a=1.0 expected=2.0')
    assert 'alpha, beta' in isolation_reading_fact(read, {'beta', 'alpha'})
    assert 'this check' in isolation_reading_fact(read, None)


# ---------------------------------------------------------------------------
# 3 — the isolation transform, on the two real archived harness sources.
# ---------------------------------------------------------------------------

def test_record_firing_keeps_the_targets_message_and_mutes_every_sibling():
    src = _harness('math30_multi_oracle')
    out = instrument_for_counting(src, 'swap-symmetry', record_firing=True)
    assert out is not None
    # The target still constructs its alarm (that is where the value lives),
    # but the throw is now caught and printed instead of ending the run.
    assert '[oracle:swap-symmetry]' in out
    assert '__vpViolated++;' in out
    assert 'System.err.println("[relfire] "' in out
    assert 'catch (Throwable __vpAlarm)' in out
    # Every sibling is silenced, so nothing can speak before the target.
    for sib in ('lifted-big-dataset', 'midpoint-pvalue', 'u-sum'):
        assert '; /* muted:%s */' % sib in out
        assert '[oracle:%s]' % sib not in out
    assert out.count('static long __vpChecked = 0, __vpViolated = 0;') == 1
    assert out.count('{') == out.count('}')


def test_record_firing_works_when_the_oracle_throws_in_a_helper_method():
    src = _harness('closure70_oracle_and_escape')
    out = instrument_for_counting(src, 'warning-array-consistency',
                                  record_firing=True)
    assert out is not None
    assert '[oracle:warning-array-consistency]' in out
    assert 'System.err.println("[relfire] "' in out
    for sib in ('duplicate-local-var-count', 'duplicate-local-var-redef',
                'duplicate-local-var-init', 'fresh-compiler-agreement'):
        assert '; /* muted:%s */' % sib in out
    assert out.count('{') == out.count('}')


def test_record_firing_is_off_by_default_so_the_counting_path_is_unchanged():
    src = _harness('math30_multi_oracle')
    assert (instrument_for_counting(src, 'u-sum')
            == instrument_for_counting(src, 'u-sum', record_firing=False))
    plain = instrument_for_counting(src, 'u-sum')
    assert '{ __vpViolated++; }' in plain
    assert '[relfire]' not in plain
    assert '[oracle:u-sum]' not in plain


def test_the_printed_line_is_the_marker_the_existing_harvester_reads():
    from java.relations.relation_screen import harvest_relfire_lines
    out = instrument_for_counting(_harness('math30_multi_oracle'), 'u-sum',
                                  record_firing=True)
    assert 'System.err.println("[relfire] "' in out
    sample = '[relfire] [oracle:u-sum] violated: actual=1.0 expected=2.0'
    assert harvest_relfire_lines('noise\n' + sample + '\nmore') == [sample]


def test_the_transform_still_fails_open_on_every_bad_input():
    src = _harness('math30_multi_oracle')
    assert instrument_for_counting(src, 'no-such-oracle',
                                   record_firing=True) is None
    assert instrument_for_counting('', 'u-sum', record_firing=True) is None
    assert instrument_for_counting(None, 'u-sum', record_firing=True) is None
    assert instrument_for_counting(src, '', record_firing=True) is None


# ---------------------------------------------------------------------------
# 4 — the composition path: read, transform, compile, replay ONE input.
#     Stub builder, stub Jazzer; no javac, no JVM.
# ---------------------------------------------------------------------------
_ONE_ORACLE = (
    'public class HarnessX {\n'
    '  public static void fuzzerTestOneInput('
    'com.code_intelligence.jazzer.api.FuzzedDataProvider data) {\n'
    '    int x = data.consumeInt();\n'
    '    if (x != x) {\n'
    '      throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow('
    '"[oracle:only] violated: actual=1.0 expected=2.0");\n'
    '    }\n'
    '  }\n'
    '}\n')


class _StubBuilder:
    """Compiles (or refuses to) without touching javac."""

    def __init__(self, compiles=True):
        self.compiles = compiles
        self.sources = []

    def build(self, source, project_dir, output_subdir):
        self.sources.append(source)

        class _BR:
            compiled = self.compiles
            class_name = 'HarnessX'
            harness_path = '/tmp/does-not-exist/HarnessX.java'
        return _BR()


def _runner():
    from java.execution.fuzz_runner import FuzzRunner
    r = FuzzRunner.__new__(FuzzRunner)
    r.jazzer_standalone_jar = r.jazzer_api_jar = None
    r.expected_exceptions = []
    return r


def _stub_jazzer(monkeypatch, output='', triggered=False, returncode=0,
                 raises=False):
    import java.execution.fuzz_runner as fr

    class _Outcome:
        combined_output = output
        timed_out = False

    _Outcome.triggered = triggered
    _Outcome.returncode = returncode

    def _run(**kwargs):
        if raises:
            raise RuntimeError('jazzer exploded')
        return _Outcome()

    monkeypatch.setattr(fr, 'run_jazzer', _run)


def _isolated(monkeypatch, tmp_path, builder=None, source=_ONE_ORACLE,
              **jazzer):
    import java.execution.fuzz_runner as fr
    _stub_jazzer(monkeypatch, **jazzer)
    src = tmp_path / 'HarnessX.java'
    src.write_text(source)
    return fr.FuzzRunner.replay_input_isolated(
        _runner(), str(src), 'HarnessX', 'cp', str(tmp_path / 'input'),
        'only', builder=builder if builder is not None else _StubBuilder(),
        buggy_dir=str(tmp_path))


def test_the_isolated_replay_returns_the_targets_own_message(monkeypatch,
                                                             tmp_path):
    status, msg, _out = _isolated(
        monkeypatch, tmp_path,
        output='[relfire] [oracle:only] violated: actual=1.0 expected=2.0\n')
    assert status == 'fired'
    assert msg == '[oracle:only] violated: actual=1.0 expected=2.0'


def test_the_isolated_variant_is_what_actually_gets_compiled(monkeypatch,
                                                             tmp_path):
    builder = _StubBuilder()
    _isolated(monkeypatch, tmp_path, builder=builder,
              output='[relfire] [oracle:only] v: actual=1.0 expected=2.0')
    assert len(builder.sources) == 1
    compiled = builder.sources[0]
    assert 'System.err.println("[relfire] "' in compiled
    assert '__vpViolated++;' in compiled


@pytest.mark.parametrize('kwargs,expected', [
    # Ran to completion, the target never fired: evidence, but not a value.
    (dict(output='[relscreen] checked=1 violated=0'), 'silent'),
    # Non-zero exit with no message: never read as a clean reading.
    (dict(output='boom', returncode=1), 'error'),
    # The run itself failed.
    (dict(raises=True), 'error'),
])
def test_every_non_reading_outcome_carries_no_message(monkeypatch, tmp_path,
                                                      kwargs, expected):
    status, msg, _out = _isolated(monkeypatch, tmp_path, **kwargs)
    assert status == expected
    assert msg is None


@pytest.mark.parametrize('kind', ['no-builder', 'no-dir', 'no-target',
                                  'unreadable', 'no-such-oracle',
                                  'does-not-compile'])
def test_the_isolation_fails_closed_on_every_setup_failure(monkeypatch,
                                                           tmp_path, kind):
    """G4's offline half: a failed measurement can reach no dismissal path.

    Every one of these returns ``isolate_failed`` with no message, and a
    message-less reading is ambiguous, which dismisses nothing."""
    import java.execution.fuzz_runner as fr
    _stub_jazzer(monkeypatch, output='[relfire] [oracle:only] v: a=1.0')
    src = tmp_path / 'HarnessX.java'
    src.write_text(_ONE_ORACLE)
    kw = dict(builder=_StubBuilder(), buggy_dir=str(tmp_path))
    path, target = str(src), 'only'
    if kind == 'no-builder':
        kw['builder'] = None
    elif kind == 'no-dir':
        kw['buggy_dir'] = None
    elif kind == 'no-target':
        target = ''
    elif kind == 'unreadable':
        path = str(tmp_path / 'absent' / 'HarnessX.java')
    elif kind == 'no-such-oracle':
        target = 'not-in-this-harness'
    elif kind == 'does-not-compile':
        kw['builder'] = _StubBuilder(compiles=False)
    status, msg, _out = fr.FuzzRunner.replay_input_isolated(
        _runner(), path, 'HarnessX', 'cp', str(tmp_path / 'input'), target,
        **kw)
    assert status == 'isolate_failed'
    assert msg is None
    read = isolated_value_reading('[oracle:only] v: actual=1.0 expected=2.0',
                                  msg)
    assert read['reading'] == 'ambiguous'
    assert not isolation_dismisses(read)


def test_a_transform_that_raises_is_caught(monkeypatch, tmp_path):
    import java.execution.fuzz_runner as fr
    from java.execution import oracle_mute as om
    _stub_jazzer(monkeypatch)
    monkeypatch.setattr(
        om, 'instrument_for_counting',
        lambda *a, **k: (_ for _ in ()).throw(ValueError('nope')))
    src = tmp_path / 'HarnessX.java'
    src.write_text(_ONE_ORACLE)
    status, msg, _out = fr.FuzzRunner.replay_input_isolated(
        _runner(), str(src), 'HarnessX', 'cp', str(tmp_path / 'input'),
        'only', builder=_StubBuilder(), buggy_dir=str(tmp_path))
    assert status == 'isolate_failed'
    assert msg is None


# ---------------------------------------------------------------------------
# 5 — the run.py hook: armed only on a still-unknown verdict whose buggy-side
#     reading was PREVENTED. Pinned by reading the source, since the enclosing
#     loop needs a whole live run to execute.
# ---------------------------------------------------------------------------

def test_the_hook_is_gated_on_an_unknown_verdict_and_a_non_clean_replay():
    with open(os.path.join(ROOT, 'src', 'java', 'run.py')) as fh:
        body = fh.read()
    assert 'replay_input_isolated' in body
    gate = re.search(
        r'if \(_fired_ids and _iso_vv == "unknown"\s*\n\s*'
        r'and _breplay_status != "clean"\):', body)
    assert gate, 'the shadow-isolation hook lost its arming condition'
    # It sits AFTER the muted ladder, so a muted re-replay that already
    # resolved the value question is not re-measured.
    assert body.index('_mvv_seen = _mvv') < gate.start()


def test_the_hook_has_no_flag_and_no_new_conviction_rule():
    """The prereg asks for no flag: the path activates only on a verdict that
    is ALREADY the degraded one, and only the two affirmative readings act."""
    with open(os.path.join(ROOT, 'src', 'java', 'run.py')) as fh:
        body = fh.read()
    block = body[body.index('SHADOW-ISOLATION READING'):]
    block = block[:block.index('# The firing INPUT itself')]
    assert 'args.' not in block            # no flag consulted
    assert block.count('drop_reasons.append') == 1
    assert 'kept_reason' not in block      # nothing here can convict


# ---------------------------------------------------------------------------
# 6 — G-S2, the pre-registered no-catch-killed gate.
#
# "For every row of the 67-row guard population that carries actual/expected
# values, neither arithmetic dismissal condition holds." The population records
# each genuine catch's PATCHED-side firing only; no buggy-side reading was ever
# taken for these rows. So arm 1 measures exactly the property that matters
# when a reading is unavailable: no catch can be dismissed without one. Arm
# 2 supplies the missing side from the rows' own numbers, in the shape a
# genuine catch has (the buggy build satisfying the check the patch violates),
# and checks the arithmetic does not invert on real data.
# ---------------------------------------------------------------------------

def _guard_rows():
    if not os.path.exists(_GUARD):
        pytest.skip('genuine-catch guard population not present')
    with open(_GUARD) as fh:
        return json.load(fh)


def _row_message(row):
    """Everything a guard row records about its firing, as one blob."""
    return ' '.join(str(row.get(k) or '')
                    for k in ('check', 'claim', 'fired', 'fired_assertion'))


def test_the_guard_population_is_still_the_pinned_67_genuine_catches():
    rows = _guard_rows()
    assert len(rows) == 67
    assert all(r['true_label'].startswith('genuine catch') for r in rows)


def test_gs2_no_guard_row_can_be_dismissed_without_a_buggy_side_reading():
    """Arm 1 — the fail-closed property, on all 67 rows.

    This is not a formality: it is the state the mechanism is in whenever the
    isolated harness fails to compile, the input does not fire it, or the
    values do not parse, which the desk read expects to be the common case."""
    dismissed = [r for r in _guard_rows()
                 if isolation_dismisses(
                     isolated_value_reading(_row_message(r), None))]
    assert dismissed == [], (
        'G-S2 FAILED: %d genuine catch(es) dismissed with no buggy-side '
        'reading — redesign, do not ship: %s'
        % (len(dismissed), [r['check'] for r in dismissed]))


def test_gs2_no_guard_row_is_dismissed_when_the_buggy_build_is_correct():
    """Arm 2 — the genuine-catch shape, from each row's own numbers.

    A genuine catch means the patched build violates a property the buggy
    build honours. Supplying that buggy-side reading (its observed value
    sitting exactly on the check's own expected value) must read
    ``buggy-closer`` — corroboration — and never a dismissal.

    The count is PINNED at zero and that is the finding, not a pass by
    accident: the guard population stores each row's judge CLAIM, truncated
    to ~150 characters, so no row carries an expected/observed value pair the
    arithmetic can read. Arm 2 therefore has no reach here, which is exactly
    why the archived keep-finding alarms — which store the alarm text
    verbatim — are exercised below."""
    read_rows = 0
    dismissed = []
    for row in _guard_rows():
        msg = _row_message(row)
        counterfactual = _buggy_satisfies(msg)
        if counterfactual is None:
            continue
        read_rows += 1
        if isolation_dismisses(isolated_value_reading(msg, counterfactual)):
            dismissed.append(row['check'])
    assert dismissed == [], (
        'G-S2 FAILED on %d of %d rows carrying values: %s'
        % (len(dismissed), read_rows, dismissed))
    assert read_rows == 0, (
        'guard rows now carry readable value pairs (%d) — arm 2 has reach '
        'here for the first time; re-run G-S2 and restate its counts'
        % read_rows)


_OBS_KEY_RE = re.compile(r'([A-Za-z_]\w*)\s*=\s*'
                         r'(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)')


def _buggy_satisfies(msg):
    """The same firing message with ONE observed value moved onto the check's
    own expected value — what a build that honours the property would print.

    Every other key is copied verbatim, so exactly one observable differs
    between the two messages: the shape a genuine catch has. Returns None when
    the message carries no single expected key, or no observed key that
    differs from it. The choice of which observed key to move is arbitrary and
    only used to BUILD the fixture; the reading under test picks the differing
    key itself."""
    from java.relations.evidence_facts import _reference_key
    pairs = _OBS_KEY_RE.findall(msg or '')
    expected = [(k, v) for k, v in pairs if re.search('expect', k, re.I)]
    if len(expected) != 1:
        return None
    exp_val = expected[0][1]
    for key, val in pairs:
        if _reference_key(key) or val == exp_val:
            continue
        return re.sub(r'\b%s\s*=\s*%s' % (re.escape(key), re.escape(val)),
                      '%s=%s' % (key, exp_val), msg, count=1)
    return None


def _cases228_keeps():
    if not os.path.exists(_CASES228):
        pytest.skip('cases228 fixture not present')
    rows = [json.loads(line) for line in open(_CASES228) if line.strip()]
    return [r for r in rows if r.get('gold') == 'keep-finding']


def test_gs2_extends_to_the_archived_keep_findings_that_carry_real_values():
    """The guard population records judges' claims, not raw alarms. The
    archived keep-finding rows record the alarm text VERBATIM, values
    included, so they are where the arithmetic can actually be exercised on
    real catches — few of them, but real."""
    read_rows, dismissed = 0, []
    for row in _cases228_keeps():
        msg = row.get('fired_assertion') or ''
        counterfactual = _buggy_satisfies(msg)
        if counterfactual is None:
            continue
        read_rows += 1
        reading = isolated_value_reading(msg, counterfactual)
        if isolation_dismisses(reading):
            dismissed.append((row.get('id'), reading['reading']))
        # A build that satisfies the check must read as the CORROBORATING
        # direction, never as ambiguous-by-accident.
        assert reading['reading'] == 'buggy-closer', (row.get('id'), reading)
    # Pinned, not a floor: if the fixture changes, the count reported in
    # docs/shadow-isolation-build-2026-08-10.md expires with it.
    assert read_rows == 2, (
        'the number of archived keep-findings carrying a movable '
        'expected/observed pair moved (%d != 2) — re-run G-S2 and restate '
        'its counts' % read_rows)
    assert dismissed == [], (
        'G-S2 FAILED on archived genuine catches: %s' % dismissed)


_SCALED_RE = re.compile(r'([A-Za-z_]\w*)\s*=\s*'
                        r'(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)')


def _buggy_reads_differently(msg):
    """The same firing message with its first observed value doubled and
    shifted — a buggy-side reading that is genuinely DIFFERENT from the
    patched one. None when there is no observed numeric key to move."""
    from java.relations.evidence_facts import _reference_key
    for key, val in _SCALED_RE.findall(msg or ''):
        if _reference_key(key):
            continue
        try:
            moved = float(val) * 2.0 + 1.0
        except ValueError:
            continue
        return re.sub(r'\b%s\s*=\s*%s' % (re.escape(key), re.escape(val)),
                      '%s=%r' % (key, moved), msg, count=1)
    return None


def test_gs2_a_different_buggy_reading_is_never_read_as_identical():
    """The identity condition's own failure mode, on every archived catch that
    prints numbers: two messages that differ only in what the build OBSERVED
    must never compare identical. This is what the shared-observed-key
    precondition exists to guarantee, checked on real alarm text rather than
    on constructed strings."""
    checked, wrong = 0, []
    for row in _cases228_keeps():
        msg = row.get('fired_assertion') or ''
        counterfactual = _buggy_reads_differently(msg)
        if counterfactual is None:
            continue
        checked += 1
        if isolated_value_reading(msg, counterfactual)['reading'] \
                == 'identical':
            wrong.append(row.get('id'))
    assert checked == 19, (
        'the archived keep-findings carrying numeric values moved '
        '(%d != 19) — re-run G-S2 and restate its counts' % checked)
    assert wrong == [], (
        'G-S2 FAILED: a genuinely different buggy reading compared identical '
        'on %s' % wrong)


def test_gs2_no_archived_keep_finding_is_dismissed_without_a_reading():
    dismissed = [r.get('id') for r in _cases228_keeps()
                 if isolation_dismisses(isolated_value_reading(
                     r.get('fired_assertion') or '', None))]
    assert dismissed == []
