"""Every outcome mutation must go through the evented helpers.

Why this test exists (2026-07-31). A finding's outcome was changed in nine
places in run.py, all `print`-only. `run_suite.sh` deletes run.log on success,
so trace.md — the archived record — contained no reason for any drop. On
SEMANTIC legs that was survivable: the drop usually follows a judge call whose
VERDICT/WHY/CITATION text the recorder captures automatically. On CRASHING legs
the deciding step is purely mechanical, so the print was the only record: the
crashtrace1 run (Lang-27, 2026-07-31) shows two patched-build firings,
`crashed_on_patch: false`, zero judge calls and no recorded reason anywhere.

The fix is structural rather than "add nine record_event calls", because the
tenth site would be forgotten the same way: `drop_finding()` and
`flag_overfitting()` set the outcome AND emit the event, and this test fails
the build if any code outside them mutates the outcome directly. Same pattern
as `judge_decision.adjudicate()`, which stopped production and offline replay
from drifting apart.
"""
import re
from pathlib import Path

RUN_PY = Path(__file__).resolve().parents[1] / 'src' / 'java' / 'run.py'
SRC = RUN_PY.read_text()
LINES = SRC.splitlines()

# The two helper bodies are the ONLY places allowed to mutate the outcome.
_HELPERS = ('def drop_finding(', 'def flag_overfitting(')


def _helper_line_ranges():
    """(start, end) line numbers, 1-indexed, of each helper's body."""
    out = []
    for name in _HELPERS:
        i = SRC[:SRC.index(name)].count('\n')          # 0-indexed def line
        j = i + 1
        while j < len(LINES) and (not LINES[j].strip()
                                  or LINES[j].startswith((' ', '\t'))):
            j += 1
        out.append((i + 1, j))                          # 1-indexed inclusive
    return out


def _outside_helpers(pattern):
    """Line numbers matching `pattern` that are not inside a helper body."""
    ranges = _helper_line_ranges()
    hits = []
    for n, line in enumerate(LINES, start=1):
        if line.lstrip().startswith('#'):
            continue                                    # comments may mention it
        if re.search(pattern, line) and not any(a <= n <= b for a, b in ranges):
            hits.append(f'{n}: {line.strip()[:90]}')
    return hits


def test_no_direct_triggered_mutation_outside_helper():
    hits = _outside_helpers(r'\.triggered\s*=\s*(?!=)')
    assert not hits, (
        'Outcome mutated without an event. Use drop_finding(r, site, reason) '
        'so the decision reaches trace.md:\n  ' + '\n  '.join(hits))


def test_no_direct_crashed_on_patch_write_outside_helper():
    # The single legitimate read/derive in the record builder assigns from the
    # triggered count; only *extras* writes are the verdict-setting path.
    hits = _outside_helpers(r"record_extras\[.crashed_on_patch.\]\s*=")
    assert not hits, (
        'Verdict set without an event. Use flag_overfitting(record_extras, '
        'site, reason):\n  ' + '\n  '.join(hits))


def test_helpers_emit_events():
    """Both helpers must call record_event — the point of routing through them."""
    for (start, end), name in zip(_helper_line_ranges(), _HELPERS):
        body = '\n'.join(LINES[start - 1:end])
        assert 'record_event(' in body, f'{name} does not emit an event'


def test_helpers_use_distinct_method_names():
    """outcome-drop vs outcome-flag: greppable, and distinguishable in traces."""
    body = SRC[SRC.index('def drop_finding('):]
    assert "method='outcome-drop'" in body
    assert "method='outcome-flag'" in body


def test_every_drop_site_names_a_site_tag():
    """Each call passes a non-empty site tag as the 2nd positional argument, so
    a trace says WHICH rule dropped the finding, not merely that one did."""
    for m in re.finditer(r'drop_finding\(\s*([^)]*?)\)', SRC, re.S):
        args = m.group(1)
        if args.strip().startswith('r, site'):
            continue                                    # the def line itself
        assert re.search(r"'[a-z0-9-]{4,}'", args), (
            f'drop_finding call without a site tag: {args[:80]}')
