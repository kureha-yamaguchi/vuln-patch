"""8.3 step 1 — the buggy-side message extractor must not cut the values off.

The batch-8 smoke found two cutters between an alarm and its mechanical reader:
a 200-char cap and a line-oriented capture. Both were fixed on the PATCHED side.
`_extract_oracle_msg` -- the BUGGY-side equivalent, and the function 8.3 exists
to make useful -- had both, unlooked-at, because until 8.3 nothing downstream
consumed its values.

That is the selection effect again: the function was only ever asked for a short
prefix, so a bug that only shows up when you need the tail stayed invisible.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))


def _load():
    """Import just the helper from run.py without executing its main()."""
    src = (ROOT / 'src' / 'java' / 'run.py').read_text()
    start = src.index('_MSG_END_RE')
    end = src.index('def parse_args')
    ns = {'re': re}
    exec(src[start:end], ns)
    return ns['_extract_oracle_msg']


extract = _load()

STACK = '\n\tat com.google.Foo.bar(Foo.java:91)\n'


def test_a_long_message_keeps_its_trailing_values():
    """Cutter 1: the 200-char cap. The values live at the END."""
    msg = ('[oracle:fmt] semantic mismatch: ' + 'x' * 220
           + ' expectedRaw=a actualRaw=b')
    got = extract(msg + STACK, 'fmt')
    assert 'actualRaw=b' in got


def test_a_message_with_a_real_newline_keeps_its_trailing_values():
    """Cutter 2: the line-oriented capture. 8.4's escaping should prevent real
    newlines, but this function must not DEPEND on another component's
    compliance for its own correctness."""
    msg = ('[oracle:fmt] semantic mismatch: expectedRaw=line one\nline two'
           ' actualRaw=b')
    got = extract(msg + STACK, 'fmt')
    assert 'actualRaw=b' in got


def test_it_still_stops_at_the_stack_trace():
    """The message must not swallow the stack — that would be over-capture,
    and over-capture pollutes every value comparison downstream."""
    got = extract('[oracle:fmt] semantic mismatch: a=1' + STACK, 'fmt')
    assert got == '[oracle:fmt] semantic mismatch: a=1'
    assert 'Foo.java' not in got


def test_it_stops_at_the_next_exception_record():
    out = ('[oracle:one] mismatch: a=1\n'
           '== Java Exception: [oracle:two] mismatch: a=2\n')
    assert extract(out, 'one') == '[oracle:one] mismatch: a=1'


def test_it_stops_at_a_caused_by_chain():
    out = '[oracle:x] mismatch: a=1\nCaused by: java.lang.NullPointerException\n'
    assert extract(out, 'x') == '[oracle:x] mismatch: a=1'


def test_it_stops_at_the_libfuzzer_banner():
    out = '[oracle:x] mismatch: a=1\n#12\tINITED cov: 1337\n'
    assert extract(out, 'x') == '[oracle:x] mismatch: a=1'


def test_absent_id_is_still_None():
    """Unchanged: absence must read as UNKNOWN, never as 'ran clean'."""
    assert extract('nothing here', 'x') is None
    assert extract('', 'x') is None
    assert extract('[oracle:y] m', None) is None


def test_the_recovered_values_are_actually_comparable():
    """The point of the fix: `compare_fired_values` can now see the shared
    keys it needs. With the old extractor both sides truncated to a prefix
    carrying no key at all."""
    from java.relations.evidence_facts import compare_fired_values
    pad = 'y' * 220
    patched = f'[oracle:fmt] semantic mismatch: {pad} actual=4.94 expected=6.99'
    buggy_out = (f'[oracle:fmt] semantic mismatch: {pad} '
                 f'actual=1.11 expected=6.99' + STACK)
    bmsg = extract(buggy_out, 'fmt')
    assert compare_fired_values(patched, bmsg) == 'different'
