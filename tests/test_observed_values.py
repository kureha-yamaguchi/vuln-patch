"""8.3 — the recording half: buggy-side observed VALUES reach the record.

The gap this closes: 0 of 1,452 recorded buggy-side steps carried an observed
value, only fired/counts. The values were computed for the cross-build
comparison and then dropped on the floor. That absence is what made 8.2
untestable and what forces 6C's values-not-compared abstentions.

Recording only -- no verdict reads these yet. 8.2's authority screen and 8.20's
scope fact are the consumers, and both come after.
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from java.relations.evidence_facts import (                     # noqa: E402
    _kv_values, observed_values)


def test_it_keeps_the_values_the_numeric_extractor_must_drop():
    """The reason this is a separate function. `_kv_values` feeds a numeric
    comparison, so a value it cannot parse is correctly invisible to it. The
    RECORD must keep those -- they are exactly the formatted-text values 8.2
    and 8.20 consume."""
    msg = ('[oracle:fmt] semantic mismatch: expectedRaw=assert (1; '
           'actualRaw=null count=3')
    numeric = _kv_values(msg)
    recorded = observed_values(msg)
    assert 'count' in numeric and 'expectedRaw' not in numeric
    assert recorded['expectedRaw'] == ['assert (1;']
    assert recorded['actualRaw'] == ['null']
    assert recorded['count'] == ['3']


def test_values_containing_spaces_survive():
    """`x- -0.0` is the Closure-38 shape: the space IS the defect."""
    assert observed_values('m expectedRaw=x- -0.0 actualRaw=x--0.0') == {
        'expectedRaw': ['x- -0.0'], 'actualRaw': ['x--0.0']}


def test_a_repeated_key_keeps_every_occurrence():
    assert observed_values('a=1 b=2 a=3')['a'] == ['1', '3']


def test_it_is_total_and_never_raises():
    for bad in (None, '', 123, [], {'x': 1}):
        assert observed_values(bad) == {} or isinstance(
            observed_values(bad), dict)


def test_no_pairs_reads_as_no_values_recorded():
    """`{}` must mean 'nothing recorded', never 'nothing existed' -- the
    distinction the whole 6C abstention rests on."""
    assert observed_values('[oracle:x] something went wrong') == {}


def test_silent_on_buggy_stays_valueless_by_construction():
    """The fail-safe the plan requires: values only exist when a check FIRES on
    the buggy side. A silent buggy replay yields no message, hence no values,
    hence abstention -- not a manufactured comparison."""
    assert observed_values(None) == {}


def test_the_run_records_the_event():
    """Wiring pin -- a recorder nothing calls records nothing (rule 15)."""
    src = (ROOT / 'src' / 'java' / 'run.py').read_text()
    assert "method='buggy-side-observed-values'" in src
    assert 'observed_values as _ov' in src
    seg = src[src.index("method='buggy-side-observed-values'") - 2000:
              src.index("method='buggy-side-observed-values'") + 1200]
    assert 'buggy_values' in seg and 'patched_values' in seg


def test_recording_is_wrapped_so_it_can_never_break_a_run():
    src = (ROOT / 'src' / 'java' / 'run.py').read_text()
    i = src.index("method='buggy-side-observed-values'")
    seg = src[i - 2000:i + 1600]
    assert 'except Exception:' in seg
    assert 'recording must never break a run' in seg
