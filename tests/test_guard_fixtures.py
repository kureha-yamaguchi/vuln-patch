"""Population pins for the two GUARD fixtures.

Standing rule 2 — *build the guard population before the mechanism* — is only
mechanically enforceable if the populations are frozen assets rather than
scratch tables rebuilt per study. These tests pin both.

**The two guards are complements, and a mechanism needs whichever one it can
damage:**

* `genuine catches` (67 rows) — kept alarms on legs that ended as real catches.
  Guards any DISMISS-pushing mechanism: if it voids these, it destroys recall.
  This is the population the engagement experiment failed against (10 wrong
  voids, 22% of the answered set).
* `correct dismissals` (38 rows, this fixture) — dismissed alarms on CORRECT
  patch legs. On a correct patch the alarm IS a false alarm, so dismissing it is
  unambiguously right. Guards any KEEP-pushing mechanism: if it overturns these,
  it manufactures false accusations.

The second guard exists because 8.14 measured the judge as BOTH the largest
recall class (over-dismissal, ~10 of 14 misses) and the capped precision
component (under-dismissal). Any fix aimed at one direction is presumed to
damage the other until measured against the opposite guard.
"""
import json
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

DISMISSALS = ROOT / 'tests' / 'fixtures' / 'correct_dismissals.jsonl'


def rows():
    return [json.loads(l) for l in DISMISSALS.open()]


def test_correct_dismissals_fixture_exists_and_is_pinned():
    """Population size is pinned. If it moves, a study built on it must be
    re-run rather than silently comparing against a different denominator —
    the lesson from fix (ii), whose numbers expired when its population changed
    from 10 rows on one leg to 27 across three."""
    r = rows()
    assert len(r) == 38, (
        f'correct-dismissal guard population changed ({len(r)} != 38); '
        're-run any study that used it rather than inheriting its numbers')


def test_every_row_is_a_dismissal_on_a_correct_patch():
    """The definition that makes these unambiguously right: the patch is
    correct, so the alarm was a false alarm, so dismissing it was correct."""
    for x in rows():
        assert x['leg'].endswith('_c'), f'{x["id"]}: not a correct-patch leg'
        assert x['gold'] == 'dismiss-finding'


def test_rows_carry_what_a_guard_check_needs():
    for x in rows():
        assert x['fired_assertion'], f'{x["id"]}: no fired assertion recorded'
        assert x['dismissal_why'], f'{x["id"]}: no dismissal reasoning recorded'


def test_ids_are_unique():
    ids = [x['id'] for x in rows()]
    assert len(ids) == len(set(ids))


def test_the_population_spans_more_than_one_leg_and_roll():
    """A guard drawn from a single leg guards nothing general — the failure that
    got fix (ii) deferred (all 10 of its rows were one fake patch)."""
    r = rows()
    assert len({x['bug'] for x in r}) >= 8
    assert len({x['roll'] for x in r}) == 2


def test_the_two_guards_are_disjoint_in_direction():
    """Sanity: the genuine-catch guard holds KEPT alarms, this one holds
    DISMISSED alarms. A mechanism cannot be safe by construction on both — it
    must be measured against each."""
    genuine = ROOT / 'docs' / 'replay' / 'backtrack' / 'guard_population.json'
    if not genuine.exists():
        pytest.skip('genuine-catch guard not present')
    g = json.load(genuine.open())
    assert all(x['answer_key'] == 'NONE' for x in g)
    assert all(x['gold'] == 'dismiss-finding' for x in rows())
