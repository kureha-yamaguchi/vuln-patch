"""8.23 -- summary.md must refuse to exist for an incomplete run.

pairA8's official table covered 29 of 30 legs. Lang-22-c died on an uncaught
network error and wrote no result.jsonl; the summary was generated anyway. Every
rate in that table had the wrong denominator, and nothing said so.

Rule 15's family again: a report that looks complete and is not. The fix refuses
the FILENAME, not just the content -- an incomplete table that is merely
annotated still gets read as the result three weeks later.
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def _extract_summary_block():
    """The python heredoc out of run_suite.sh, so the test exercises the
    shipped code rather than a copy of it."""
    sh = (ROOT / 'run_suite.sh').read_text()
    start = sh.index("import json, math, sys, collections, os")
    end = sh.index('PY\n', start)
    return sh[start:end]


def _run(tmp_path, n_legs, n_results):
    root = tmp_path / 'run'
    root.mkdir()
    worklist = tmp_path / 'worklist'
    lines = []
    for i in range(n_legs):
        tag = f'{i:02d}_leg'
        lines.append(f'{i}\t{tag}\t-c\tpatch{i}')
        d = root / tag
        d.mkdir()
        if i < n_results:
            (d / 'result.jsonl').write_text(json.dumps({
                'project': 'P', 'bug_id': str(i), 'label': 'correct',
                'bug_kind': 'semantic', 'status': 'evaluated',
                'crashed_on_patch': False, 'tokens_total': {'total_tokens': 1},
            }) + '\n')
    worklist.write_text('\n'.join(lines) + '\n')
    script = tmp_path / 'sum.py'
    script.write_text(_extract_summary_block())
    r = subprocess.run(
        [sys.executable, str(script), str(root), str(worklist),
         str(root / 'summary.md'), 'testsuite', '20260101_000000'],
        capture_output=True, text=True)
    return r, root


def test_a_complete_run_still_writes_summary(tmp_path):
    r, root = _run(tmp_path, 3, 3)
    assert r.returncode == 0, r.stderr
    assert (root / 'summary.md').exists()
    assert '3 runs' in (root / 'summary.md').read_text()


def test_an_incomplete_run_writes_NO_summary(tmp_path):
    """The whole point: the real filename must not appear."""
    r, root = _run(tmp_path, 3, 2)
    assert r.returncode != 0
    assert not (root / 'summary.md').exists(), \
        'summary.md must not exist for an incomplete run'


def test_it_says_loudly_which_legs_are_missing(tmp_path):
    r, root = _run(tmp_path, 3, 2)
    assert 'REFUSING' in r.stderr
    bad = root / 'summary-INCOMPLETE.md'
    assert bad.exists()
    txt = bad.read_text()
    assert 'INCOMPLETE, NOT SCORED' in txt
    assert '02_leg' in txt, 'the missing leg must be named'
    assert '2 of 3' in txt


def test_a_present_but_EMPTY_result_counts_as_missing(tmp_path):
    """An empty result.jsonl is the shape a killed leg actually leaves -- the
    file can exist and hold nothing. Counting FILES rather than CONTENT would
    pass this and produce the same silently-wrong table."""
    r, root = _run(tmp_path, 2, 2)
    assert r.returncode == 0 and (root / 'summary.md').exists()

    (root / 'summary.md').unlink()
    (root / '01_leg' / 'result.jsonl').write_text('   \n')   # present, empty
    r2 = subprocess.run(
        [sys.executable, str(tmp_path / 'sum.py'), str(root),
         str(tmp_path / 'worklist'), str(root / 'summary.md'), 's', 't'],
        capture_output=True, text=True)
    assert r2.returncode != 0, 'an empty result file must count as missing'
    assert not (root / 'summary.md').exists()
    assert '01_leg' in (root / 'summary-INCOMPLETE.md').read_text()
