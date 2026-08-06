"""8.21(a) + 8.21(c) -- the ARCHIVE gets the whole thing.

Both fixes are the same principle arriving at two places: a record is read
months later by someone reconstructing what happened, and neither a truncated
alarm nor an absent field can be recovered afterwards.

The costs already paid, which is why these are not hygiene:
  * 8.4's compliance was read as 38% when it was 100% -- counted inside
    ellipsis-truncated records.
  * The batch-8 defect was DIAGNOSED as the 200-char cap when the cause was an
    embedded newline -- the evidence was a truncated trace record.
  * 8 rows of cases228 can never be repaired: the archive kept only prefixes.
  * The closing pair could not answer 8.2's build/no-build question, because its
    rule named "of trigger rows" and result.jsonl carried no code_context. A
    paid-for measurement was lost for want of a field.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))


# --- 8.21(a): the alarm reaches the archive in full ------------------------

def test_the_record_uses_a_record_sized_cap_not_the_prompt_cap():
    from java.harness.campaign import RECORD_MAX_LEN
    assert RECORD_MAX_LEN >= 100_000
    src = (ROOT / 'src' / 'java' / 'harness' / 'campaign.py').read_text()
    i = src.index('result.accepted_trigger_details.append(detail)')
    seg = src[i - 1200:i]
    assert 'max_len=RECORD_MAX_LEN' in seg, \
        'the accepted-trigger detail must not use the default 200-char cap'


def test_prompt_and_display_consumers_keep_their_cap():
    """The split is the point: raising the cap everywhere would grow every
    prompt and change de-duplication."""
    from java.execution.oracle_strength import (exception_headline,
                                                exception_headlines)
    out = '== Java Exception: ' + 'x' * 400 + '\n'
    assert len(exception_headline(out)) == 201
    assert len(exception_headlines(out)[0]) == 201


def test_a_long_alarm_survives_at_record_size():
    from java.execution.oracle_strength import exception_headline
    from java.harness.campaign import RECORD_MAX_LEN
    msg = '[oracle:x] semantic mismatch: ' + 'y' * 800 + ' actualRaw=z'
    got = exception_headline('== Java Exception: ' + msg + '\n',
                             max_len=RECORD_MAX_LEN)
    assert got.endswith('actualRaw=z')
    assert not got.endswith('…')


# --- 8.21(c): code_context reaches the archive -----------------------------

def test_the_run_records_code_context():
    src = (ROOT / 'src' / 'java' / 'run.py').read_text()
    assert "record_extras['code_context']" in src


def test_it_is_recorded_RAW_not_as_a_precomputed_flag():
    """A flag would freeze today's detector into the archive, and the detector
    is the thing most likely to change. Same rule that made observed_values
    string-preserving."""
    src = (ROOT / 'src' / 'java' / 'run.py').read_text()
    i = src.index("record_extras['code_context']")
    seg = src[i - 900:i + 300]
    assert 'RAW, not a precomputed flag' in seg
    assert "'\\n\\n'.join(class_ctx)" in seg or "join(class_ctx)" in seg


def test_recording_it_cannot_break_a_run():
    src = (ROOT / 'src' / 'java' / 'run.py').read_text()
    i = src.index("record_extras['code_context']")
    assert 'except Exception:' in src[i:i + 260]


def test_extras_actually_reach_the_record():
    """Wiring pin: _emit_record must merge extras, or the field is written
    nowhere and the test above would pass while nothing was recorded."""
    src = (ROOT / 'src' / 'java' / 'run.py').read_text()
    assert 'rec.update(extras or {})' in src
