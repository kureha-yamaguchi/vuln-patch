"""Cycle-7: repair mechanically-diagnosed harness defects instead of discarding
the attempt.

32% of all harness build attempts were rejected, and 71% of those rejections were
a small family of structural mistakes the gate already detects and locates. That
is where Chart-19's winning rule family died — proposed in both rolls, never
reaching the reviewer, rejected at construction rather than starved by the rule
budget.

The invariant these tests exist to protect: a repair may only ever REMOVE the
defect it targets. It must never weaken a check, never introduce a new defect, and
must leave the source untouched when it cannot do that.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'src'))

from java.harness.repair import (  # noqa: E402
    repair_harness, repair_missing_alarm_id, repair_rethrow_without_cause,
    repair_swallowed_alarm)
from java.parsing.java_source import (  # noqa: E402
    alarm_ids_missing, boolean_swallow, rethrow_without_cause,
    violation_swallowed)

ALARM = 'com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow'

SWALLOWED = f"""
public class FuzzHarness {{
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {{
        try {{
            int x = data.consumeInt();
            if (x != x) throw new {ALARM}("[oracle:id] relation violated");
        }} catch (Throwable t) {{
            return;
        }}
    }}
}}
"""

UNNAMED_LITERAL = f"""
public class FuzzHarness {{
    // relation: my_check_name
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {{
        if (1 != 1) throw new {ALARM}("semantic mismatch: bad");
    }}
}}
"""

UNNAMED_VARIABLE = f"""
public class FuzzHarness {{
    // relation: my_check_name
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {{
        String violation = "semantic mismatch: " + 1;
        if (1 != 1) throw new {ALARM}(violation);
    }}
}}
"""


# --- swallowed alarm (65 of 65 cleared in the archive) --------------------

def test_swallowed_alarm_is_detected_then_repaired():
    assert violation_swallowed(SWALLOWED)
    assert violation_swallowed(repair_swallowed_alarm(SWALLOWED)) is None


def test_the_repair_preserves_the_catch_all_intent():
    """The harness still swallows everything it swallowed before — only OUR
    alarm is now allowed out. Silently widening what escapes would change the
    check's meaning."""
    fixed = repair_swallowed_alarm(SWALLOWED)
    assert 'catch (Throwable t)' in fixed
    assert 'return;' in fixed
    assert 'instanceof' in fixed


def test_swallow_repair_is_idempotent():
    once = repair_swallowed_alarm(SWALLOWED)
    assert repair_swallowed_alarm(once) == once


def test_narrow_catches_are_left_alone():
    """Only a BROAD catch can absorb our alarm; a narrow one must not be
    touched."""
    src = SWALLOWED.replace('Throwable t', 'NumberFormatException t')
    assert repair_swallowed_alarm(src) == src


# --- missing alarm id (29 of 39 cleared) ----------------------------------

def test_unnamed_literal_alarm_gets_the_relation_name():
    assert alarm_ids_missing(UNNAMED_LITERAL)
    fixed = repair_missing_alarm_id(UNNAMED_LITERAL)
    assert alarm_ids_missing(fixed) is None
    assert '[oracle:my_check_name]' in fixed


def test_unnamed_variable_alarm_is_tagged_at_the_construction_site():
    """25 of the 39 unnamed alarms pass a VARIABLE, not a literal — the first
    implementation missed all of them."""
    assert alarm_ids_missing(UNNAMED_VARIABLE)
    fixed = repair_missing_alarm_id(UNNAMED_VARIABLE)
    assert alarm_ids_missing(fixed) is None
    assert '[oracle:my_check_name]' in fixed
    assert 'violation' in fixed          # original message preserved


def test_the_id_comes_from_the_harness_not_invented():
    """The ID must describe the check, never assert something about behaviour."""
    fixed = repair_missing_alarm_id(UNNAMED_LITERAL)
    assert 'my_check_name' in fixed


def test_id_repair_is_idempotent():
    once = repair_missing_alarm_id(UNNAMED_LITERAL)
    assert repair_missing_alarm_id(once) == once


def test_an_already_named_alarm_is_untouched():
    src = UNNAMED_LITERAL.replace('"semantic mismatch: bad"',
                                  '"[oracle:already] semantic mismatch: bad"')
    assert repair_missing_alarm_id(src) == src


# --- the whole-source entrypoint ------------------------------------------

def test_repair_harness_reports_what_it_applied_and_what_remains():
    fixed, applied, remaining = repair_harness(SWALLOWED)
    assert 'swallowed-alarm' in applied
    assert 'swallowed-alarm' not in remaining


def test_a_clean_harness_is_returned_unchanged():
    clean = f"""
    public class FuzzHarness {{
        // relation: fine
        public static void fuzzerTestOneInput(FuzzedDataProvider d) {{
            if (1 != 1) throw new {ALARM}("[oracle:fine] relation violated");
        }}
    }}
    """
    fixed, applied, remaining = repair_harness(clean)
    assert fixed == clean and applied == [] and remaining == []


def test_repair_never_introduces_a_defect_the_source_did_not_have():
    """THE safety invariant. A repair that trips a previously-passing detector
    is backed out, so the result can never fail something the original passed."""
    for src in (SWALLOWED, UNNAMED_LITERAL, UNNAMED_VARIABLE):
        before = {n for n, d in (('swallowed-alarm', violation_swallowed),
                                 ('boolean-swallow', boolean_swallow),
                                 ('missing-alarm-id', alarm_ids_missing),
                                 ('rethrow-without-cause',
                                  rethrow_without_cause)) if d(src)}
        fixed, _applied, remaining = repair_harness(src)
        assert not (set(remaining) - before), \
            'repair introduced a defect the original did not have'


def test_boolean_swallow_is_repaired_by_a_two_site_transform():
    """The largest rejection bucket (77 of 240). Deferred until a compiler was
    available to verify the output, then built and compile-validated on the VM.

    The transform is two coordinated edits: declare a holder before the try,
    capture the exception in the catch, and attach it as the alarm's cause.

    HONESTY GUARD: capturing alone would clear the detector, because a
    non-literal assignment stops the catch matching the bare-flag shape. That
    would game the acceptance test while preserving nothing. So the repair only
    applies when the cause actually REACHES an alarm — which is the value the
    detector protects."""
    src = f"""public class FuzzHarness {{
      // relation: flagged
      public static void f(FuzzedDataProvider d) {{
        boolean threw = false;
        try {{ d.consumeInt(); }} catch (Exception e) {{ threw = true; }}
        if (threw) throw new {ALARM}("[oracle:flagged] relation violated");
      }}
    }}"""
    assert boolean_swallow(src)
    fixed, applied, remaining = repair_harness(src)
    assert 'boolean-swallow' in applied
    assert 'boolean-swallow' not in remaining
    # the exception is captured AND delivered to the alarm
    assert '__vpCause = e;' in fixed
    assert '__vpCause)' in fixed


def test_boolean_swallow_repair_is_skipped_when_no_alarm_can_take_the_cause():
    """If the cause cannot reach an alarm, the repair must decline rather than
    clear the detector by capturing into a variable nobody reads."""
    src = f"""public class FuzzHarness {{
      public static void f(FuzzedDataProvider d) {{
        boolean threw = false;
        try {{ d.consumeInt(); }} catch (Exception e) {{ threw = true; }}
        if (threw) throw new {ALARM}("[oracle:x] relation violated", other);
      }}
    }}"""
    fixed, applied, _ = repair_harness(src)
    assert 'boolean-swallow' not in applied
    assert fixed == src


# --- integration: the campaign applies it, and marks it -------------------

def test_campaign_applies_repair_before_the_structural_gates():
    """The repair must run BEFORE the gates, so a repairable harness is
    accepted instead of spending an LLM repair turn on it."""
    import inspect
    from java.harness import campaign
    src = inspect.getsource(campaign.HarnessCampaign.run)
    repair_at = src.find('repair_harness')
    gate0_at = src.find('violation_swallowed(source)')
    assert repair_at != -1, 'repair is not wired into the campaign'
    assert gate0_at != -1
    assert repair_at < gate0_at, 'repair must run before gate 0'


def test_every_repaired_harness_is_marked_in_the_trace():
    """Required so an accusation on a previously-silent leg is attributable to
    a repaired harness with one grep — the observability that makes bundling
    this into a precision batch defensible."""
    import inspect
    from java.harness import campaign
    src = inspect.getsource(campaign.HarnessCampaign.run)
    seg = src[src.find('repair_harness'):src.find('violation_swallowed(source)')]
    assert "method='harness-repair'" in seg
    assert 'REPAIRED' in seg


def test_a_repair_failure_never_blocks_the_build():
    """Fail-open: an exception inside the repair must leave the original source
    untouched and let the gates run, exactly as before."""
    import inspect
    from java.harness import campaign
    src = inspect.getsource(campaign.HarnessCampaign.run)
    seg = src[src.find('from java.harness.repair'):]
    assert 'except Exception' in seg[:400]
    assert '_repaired, _applied, _remaining = source, [], []' in seg[:600]


def test_an_alarm_named_through_a_variable_is_left_alone():
    """Cycle-7 smoke finding: a THIRD already-named form. The tag lives in a
    variable, so neither the literal check nor the dynamic-ID regex sees it, and
    the repair prepended a redundant fallback — producing
    "[oracle:unnamed-check] [oracle:circle-err2-0]" and polluting the IDs that
    screening keys on."""
    src = f"""public class FuzzHarness {{
      static void check(boolean bad) {{
        String oracleId = "[oracle:circle-err2-0]";
        if (bad) throw new {ALARM}(oracleId + " semantic mismatch: errors[0]");
      }}
    }}"""
    assert repair_missing_alarm_id(src) == src
    assert 'unnamed-check' not in repair_missing_alarm_id(src)


# --- 8.7: acceptance records whether the harness came from a repair --------

def test_acceptance_event_records_repair_provenance():
    """Turns three greps into one lookup.

    Attributing Chart-19's catch in the cycle-7 pricing pair required
    cross-referencing repair events against patched-fuzz attempt tags by hand.
    The acceptance record now carries it directly."""
    import inspect
    from java.harness import campaign
    src = inspect.getsource(campaign.HarnessCampaign.run)
    acc = src[src.find("# --- accepted ---"):]
    assert "'from_repaired_attempt'" in acc
    assert "'repairs_applied'" in acc
    assert 'FROM REPAIRED ATTEMPT' in acc


def test_repaired_attempts_map_is_run_local():
    """Nothing crosses runs — the standing no-pooling rule."""
    import inspect
    from java.harness import campaign
    src = inspect.getsource(campaign.HarnessCampaign.run)
    assert '_repaired_attempts: dict = {}' in src, \
        'the map must be initialised inside run(), not on the instance'


def test_acceptance_notes_the_repaired_source_is_reconstructable():
    """The transform is deterministic over the recorded pre-repair output, so
    the trace need not store the repaired source twice."""
    import inspect
    from java.harness import campaign
    src = inspect.getsource(campaign.HarnessCampaign.run)
    assert "'repaired_source_reconstructable': True" in src
