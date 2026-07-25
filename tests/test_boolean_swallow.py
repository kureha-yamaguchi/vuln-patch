"""Spec L: the evidence-destroying alarm lint (boolean-swallow).

boolean_swallow(source) -> reason-string | None sits beside the existing
violation_swallowed lint. It fires when a catch block's body only assigns
literals to variables (no rethrow, no reference to the caught exception
escaping the alarm), and a later throw/alarm conditions on that variable --
because that pattern destroys the exception identity every downstream
pre-existing / laundering guard needs.

Fixtures (see tests/fixtures/boolean_swallow.json for provenance):
  * chart26_success_flag  -- verbatim archived pool30 Chart-26-c harness block
    (trace lines 2676-2691): catch { success = false; } then if (!success)
    throw ... => lint fires, reason names 'success'.
  * rethrow_with_cause    -- catch rethrows carrying the cause => quiet.
  * catch_return_skip     -- legitimate input-rejection skip => quiet.
  * flag_not_used_in_alarm-- swallowed flag never gates an alarm => quiet.

The module under test already exists (java.parsing.java_source) but the
boolean_swallow function may not be landed yet while a parallel agent
implements it; in that case these tests fail with an AttributeError only,
which is the expected transient state per the cycle plan.
"""
import json
import os

from java.parsing import java_source

_FIX = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fixtures")


def _load(name):
    with open(os.path.join(_FIX, name + ".json"), "r", encoding="utf-8") as fh:
        return json.load(fh)


_BS = _load("boolean_swallow")


# --------------------------------------------------------------------------
# Positive: the archived Chart-26 success=false block. Lint must fire and the
# reason must name the swallowed variable 'success'.
# --------------------------------------------------------------------------
def test_chart26_success_flag_swallow_fires():
    case = _BS["chart26_success_flag"]
    reason = java_source.boolean_swallow(case["source"])
    assert reason is not None
    assert case["expected_variable"] in reason  # names the variable 'success'


# --------------------------------------------------------------------------
# Negatives: legitimate patterns the lint must leave alone (None).
# --------------------------------------------------------------------------
def test_rethrow_with_cause_is_quiet():
    # The caught exception is rethrown as the cause -> identity survives.
    assert java_source.boolean_swallow(_BS["rethrow_with_cause"]["source"]) is None


def test_catch_return_skip_is_quiet():
    # Mandated input-rejection skip: catch { return; } -> not a bare-flag swallow.
    assert java_source.boolean_swallow(_BS["catch_return_skip"]["source"]) is None


def test_flag_not_used_in_alarm_is_quiet():
    # Catch assigns a literal to a flag that no later alarm conditions on.
    assert java_source.boolean_swallow(_BS["flag_not_used_in_alarm"]["source"]) is None
