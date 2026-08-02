"""The repaired replay substrate, and the trap that nearly poisoned it.

34% of cases228 (78 of 228 rows) carry a `fired_assertion` truncated at 201
characters with a trailing ellipsis -- the fingerprint of the 200-char cap the
batch-8 smoke found. Every offline measurement over message CONTENT has been
running on those prefixes.

This fixture repairs what the archive can still supply. It is a NEW file rather
than a mutation of cases228, so 8.1's recorded numbers and every prior offline
measurement stay comparable against the substrate they were computed on.

THE TRAP, recorded because the first build fell into it: extracting by searching
the trace for `[oracle:<id>]` matches the tag inside PROMPTS and HARNESS SOURCE
as well as inside fired alarms. That build produced 76 rows of exactly 3000
characters whose tails were Java source and prompt prose. Rule 8's inflation
direction, and the first time it nearly reached a versioned asset. Extraction is
now alarm-scoped: the text must follow a thrown FuzzerSecurityIssue or a
`== Java Exception:` record.
"""
import json
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

BASE = ROOT / 'tests' / 'fixtures' / 'cases228.jsonl'
FIXED = ROOT / 'tests' / 'fixtures' / 'cases228_untruncated.jsonl'


def _rows(p):
    return [json.loads(l) for l in p.open()]


def test_the_repaired_fixture_matches_the_original_row_for_row():
    base, fixed = _rows(BASE), _rows(FIXED)
    assert len(base) == len(fixed) == 228
    assert [r['id'] for r in base] == [r['id'] for r in fixed]


def test_only_the_fired_assertion_ever_differs():
    """Labels, gold and provenance are the answer key -- repairing message text
    must not touch them."""
    for b, f in zip(_rows(BASE), _rows(FIXED)):
        for k in ('label', 'gold', 'note', 'provenance', 'harness_source'):
            assert b.get(k) == f.get(k), f'{k} changed on {b["id"]}'


def test_repaired_rows_are_strictly_longer_and_no_longer_elided():
    n = 0
    for b, f in zip(_rows(BASE), _rows(FIXED)):
        if not f.get('_repaired_from_archive'):
            continue
        n += 1
        assert len(f['fired_assertion']) > len(b['fired_assertion'])
        assert not f['fired_assertion'].rstrip().endswith(('…', '...'))
    assert n == 70, f'expected 70 repaired rows, found {n}'


def test_no_row_contains_prompt_prose_or_harness_source():
    """THE TRAP. A tag-scoped search matched prompts and Java source and
    produced 3000-character rows of prose. Alarm-scoped extraction cannot."""
    markers = ('jazzer-api.jar', 'against the project classpath',
               'public static void fuzzerTestOneInput', '\n\n')
    for f in _rows(FIXED):
        fa = f['fired_assertion']
        for m in markers:
            assert m not in fa, f'{m!r} leaked into {f["id"]}'


def test_every_row_is_still_a_recognisable_firing():
    """FOUR legitimate shapes, enumerated from the data rather than guessed:
    an `[oracle:<id>]` alarm, the same with a Jazzer class prefix, a
    `relation <name> violated` replay firing (with or without that prefix), and
    a bare JVM exception from a crashing leg. Two earlier drafts of this test
    asserted a narrower shape and failed -- the data was right both times."""
    for f in _rows(FIXED):
        fa = f['fired_assertion']
        assert ('oracle:' in fa[:120]
                or 'relation ' in fa[:120]
                or 'Exception' in fa[:120]
                or 'Error' in fa[:120]), f['id'] + ': ' + fa[:80]


def test_repair_never_changed_a_rows_shape():
    """A repaired row must be a longer version of the SAME firing, not a
    different one -- the first 60 characters are the identity check."""
    for b, f in zip(_rows(BASE), _rows(FIXED)):
        if f.get('_repaired_from_archive'):
            assert f['fired_assertion'][:60] == b['fired_assertion'][:60], \
                f'repair swapped the firing on {b["id"]}'


def test_the_unrepairable_rows_are_declared_not_silently_kept():
    """8 rows cannot be repaired: the archive stored the capped form for them
    too, which is exactly the cost 8.21(a) exists to stop accruing. They are
    FLAGGED, so a consumer can exclude them rather than treat a prefix as a
    whole message."""
    still = [f for f in _rows(FIXED) if f.get('_still_truncated')]
    assert len(still) == 8
    for f in still:
        assert f['fired_assertion'].rstrip().endswith(('…', '...'))


def test_the_new_field_does_not_exist_in_either_fixture():
    """Sanity on the 8.4 claim: `actualRaw=` still cannot appear in ANY
    archived row, repaired or not -- the field postdates every one of these
    runs. Repairing the truncation does not change that."""
    for p in (BASE, FIXED):
        for r in _rows(p):
            assert 'actualRaw=' not in (r.get('fired_assertion') or '')
