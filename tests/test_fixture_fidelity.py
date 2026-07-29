"""Fixture gate-fidelity — the replay fixture must carry every input
PRODUCTION hands the decision, or the gate measures a crippled pipeline.

The bug this locks down: ``cases228.jsonl`` (and the subset derived from it)
carried NO failing-test field. ``verifier_replay`` therefore passed
``failing_block=''`` into ``adjudicate``, and
``RelationVerifier.family_duty`` -- whose entire question is "does this check
assert the FAILING TEST's own observable?" -- could essentially never answer
YES. The v6 enforcement gate then reported 4 genuine catches (inventory rows
32, 33, 122, 133) as killed by rule 6B, with the killed catches' own reason
reading "the real failing test is not provided here". That is a HARNESS
artifact charged to the rule. Production has the input: run.py passes
``_j3_failing_test_block(failure_tests)``.

Same class as the ``fd_prior=None`` artifact in test_replay_fidelity.py: a
gate is only valid where it is FAITHFUL.

Fully offline (reads the committed fixtures only): ZERO tokens.
"""
import json
import os

import pytest

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FULL = os.path.join(REPO, 'tests', 'fixtures', 'cases228.jsonl')
SUBSET = os.path.join(REPO, 'tests', 'fixtures', 'cases_subset150.jsonl')

MARKER = '[REAL FAILING TEST '

# Legs whose archived trace genuinely renders no failing-test block (the judge
# site that renders it was never reached). Cases from such a leg are allowed to
# carry failing_test == ''. Currently EMPTY: every one of the 228 cases came
# from a leg whose trace has the block. Adding an entry here is a deliberate,
# reviewed concession -- it re-disables the family-duty escape for those rows.
KNOWN_MISSING_LEGS = frozenset()

# The four rows the v6 gate reported as 6B over-kills purely because the escape
# had no input. They must carry a real block for the re-run to mean anything.
V6_ARTIFACT_ROWS = (32, 33, 122, 133)

# Post-reclassification the subset is 141 rows: rows 21 and 80 are
# gold=UNRESOLVED (contested) and must stay OUT of the scored population.
SUBSET_ROWS = 141
EXCLUDED_ROWS = (21, 80)


def _stream(path):
    """Yield cases one at a time -- the full fixture is ~8 MB."""
    with open(path, encoding='utf-8') as fh:
        for line in fh:
            line = line.strip()
            if line:
                yield json.loads(line)


def _leg_key(case):
    prov = case.get('provenance') or {}
    return f"{prov.get('run')}/{prov.get('leg')}"


@pytest.mark.parametrize('path', [FULL, SUBSET])
def test_every_case_carries_a_failing_test_block(path):
    """No scored row may reach adjudicate with an empty failing_block."""
    empty = []
    for case in _stream(path):
        block = case.get('failing_test')
        assert block is not None, (
            f"{case.get('id')}: failing_test field missing entirely -- the "
            f"replay would silently fall back to '' and disable family-duty")
        if not block.strip():
            if _leg_key(case) not in KNOWN_MISSING_LEGS:
                empty.append((case.get('id'), _leg_key(case)))
            continue
        assert block.startswith(MARKER), (
            f"{case.get('id')}: failing_test does not start with {MARKER!r} -- "
            f"it is not the verbatim _j3_failing_test_block rendering")
    assert not empty, (
        f"{len(empty)} case(s) carry no failing-test block and are not listed "
        f"in KNOWN_MISSING_LEGS: {empty[:10]}")


def test_failing_test_is_a_real_test_body():
    """The block must carry the test's own source, not just its name."""
    for case in _stream(FULL):
        block = case['failing_test']
        if not block.strip():
            continue
        head, _, body = block.partition('\n')
        assert 'trust source #1, verbatim]' in head, head[:120]
        assert '::' in head, head[:120]
        assert body.strip(), f"{case['id']}: block has a header but no body"


def test_v6_artifact_rows_now_have_the_escape_input():
    """Rows 32/33/122/133 were killed for lack of exactly this input."""
    seen = {}
    for case in _stream(FULL):
        row = (case.get('provenance') or {}).get('inventory_row')
        if row in V6_ARTIFACT_ROWS:
            seen[row] = case
    missing = [r for r in V6_ARTIFACT_ROWS if r not in seen]
    assert not missing, f"inventory rows absent from the fixture: {missing}"
    for row, case in seen.items():
        assert case['failing_test'].startswith(MARKER), row
        assert len(case['failing_test']) > 200, (
            f"row {row}: block is only {len(case['failing_test'])} chars")


def test_subset_population_is_unchanged_by_the_backfill():
    """Backfilling a field must not move the scored population."""
    rows, ids = [], []
    for case in _stream(SUBSET):
        prov = case['provenance']
        rows.append(prov['inventory_row'])
        ids.append(case['id'])
        assert (case['gold'] in ('keep-finding', 'SOUND')
                or (case['gold'] in ('dismiss-finding', 'UNSOUND')
                    and prov.get('leg_label') == 'c')), (
            f"{case['id']}: does not satisfy the subset filter")
    assert len(rows) == SUBSET_ROWS, f"subset is {len(rows)} rows"
    for excluded in EXCLUDED_ROWS:
        assert excluded not in rows, (
            f"contested row {excluded} (gold=UNRESOLVED) is back in the "
            f"scored subset")
    assert len(set(ids)) == len(ids), "duplicate case ids in the subset"


def test_subset_is_exactly_the_filtered_full_fixture():
    expected = [c['id'] for c in _stream(FULL)
                if c['gold'] in ('keep-finding', 'SOUND')
                or (c['gold'] in ('dismiss-finding', 'UNSOUND')
                    and (c['provenance'] or {}).get('leg_label') == 'c')]
    actual = [c['id'] for c in _stream(SUBSET)]
    assert actual == expected


def test_replay_reads_the_failing_test_field():
    """Plumbing: verifier_replay must prefer the case's failing_test."""
    src = open(os.path.join(REPO, 'src', 'java', 'verifier_replay.py'),
               encoding='utf-8').read()
    assert "c.get('failing_test')" in src
    assert 'failing_block=failing_block' in src
