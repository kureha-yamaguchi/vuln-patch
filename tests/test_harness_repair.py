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


def test_boolean_swallow_is_deliberately_not_repaired_yet():
    """77 of the corpus carry it — the largest single bucket — but repairing it
    needs a two-site transform (capture the exception, attach it as the cause),
    and there is no compiler available offline to verify the result. Deferred
    until the live smoke confirms the three shipped repairs compile.

    Pinned so the gap is a recorded decision rather than an oversight."""
    src = f"""
    public class FuzzHarness {{
        // relation: flagged
        public static void fuzzerTestOneInput(FuzzedDataProvider d) {{
            boolean threw = false;
            try {{ d.consumeInt(); }} catch (Exception e) {{ threw = true; }}
            if (threw) throw new {ALARM}("[oracle:flagged] relation violated");
        }}
    }}
    """
    assert boolean_swallow(src)
    _fixed, applied, remaining = repair_harness(src)
    assert 'boolean-swallow' not in applied
    assert 'boolean-swallow' in remaining
