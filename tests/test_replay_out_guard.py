"""verifier_replay must refuse an occupied output directory, not truncate it.

Written after 8.1, where a duplicate launch destroyed a 33-minute incumbent run:
`results.jsonl` is opened 'w', so the second process truncated the file to zero
while the first went on writing at its old offset, leaving a sparse file
(188,628 bytes, 11,615 non-null, 20 of ~440 rows parseable). Nothing errored.

This is deliberately NOT part of the cycle-8 batch. The batch is closed and
measured, and verifier_replay is an offline tool that nothing in the pipeline
imports -- so this cannot perturb the batch smoke, which is exactly why it can
land separately.
"""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPLAY = ROOT / 'src' / 'java' / 'verifier_replay.py'


def test_the_tool_is_not_on_the_pipeline_path():
    """The claim that licenses landing this outside the batch, checked rather
    than asserted: no shipped pipeline module imports verifier_replay."""
    hits = []
    for p in (ROOT / 'src').rglob('*.py'):
        if p.name == 'verifier_replay.py':
            continue
        txt = p.read_text(errors='ignore')
        for line in txt.splitlines():
            s = line.strip()
            if s.startswith(('import ', 'from ')) and 'verifier_replay' in s:
                hits.append(f'{p}: {s}')
    assert not hits, f'verifier_replay is imported by shipped code: {hits}'


def test_refuses_when_results_already_exist(tmp_path):
    out = tmp_path / 'run1'
    out.mkdir()
    (out / 'results.jsonl').write_text('{"id": "prior work"}\n')
    cases = tmp_path / 'c.jsonl'
    cases.write_text('{"id":"x","harness_source":"class A{}",'
                     '"fired_assertion":"boom","label":"correct"}\n')
    r = subprocess.run(
        [sys.executable, str(REPLAY), '--cases', str(cases), '--out', str(out)],
        capture_output=True, text=True)
    assert r.returncode != 0, 'must refuse, not proceed'
    assert 'refusing to run' in (r.stdout + r.stderr)
    # and the prior work must still be there
    assert (out / 'results.jsonl').read_text() == '{"id": "prior work"}\n'


def test_a_fresh_directory_is_still_accepted(tmp_path):
    """The guard must not block the normal case. Uses a bad cases file so the
    run exits before any LLM call -- what is checked is that it gets PAST the
    directory guard, not that it completes."""
    out = tmp_path / 'fresh'
    cases = tmp_path / 'empty.jsonl'
    cases.write_text('')
    r = subprocess.run(
        [sys.executable, str(REPLAY), '--cases', str(cases), '--out', str(out)],
        capture_output=True, text=True)
    assert 'refusing to run' not in (r.stdout + r.stderr)
