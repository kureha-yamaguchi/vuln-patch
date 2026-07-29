"""Cycle-7: fail-loud field access, and the self-describing gold vocabulary.

Both exist because of specific errors, and each test names the error it prevents.
See src/java/relations/fixture_fields.py for the full account.
"""
import json
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from java.relations.fixture_fields import (  # noqa: E402
    DISMISS, GOLD_VERDICTS, KEEP, UNRESOLVED, MissingField, field,
    gold_verdict, is_scored, must_dismiss, must_keep)

FIXTURES = ['cases228.jsonl', 'cases_subset150.jsonl',
            'cases_close25.jsonl', 'cases_compliance20.jsonl']


# --- the tripwire ---------------------------------------------------------

def test_missing_field_raises_instead_of_returning_none():
    """THE bug this module exists for. `case.get('trusted_values')` returned
    None for all 228 rows because the field does not exist; the Nones were read
    as "empty", and a plausible 89% statistic was computed from nothing."""
    case = {'gold': KEEP, 'fired_assertion': 'x'}
    with pytest.raises(MissingField):
        field(case, 'trusted_values')


def test_the_error_names_the_fields_that_do_exist():
    """A typo must be diagnosable from the message alone."""
    case = {'gold': KEEP, 'failing_test': 'x'}
    with pytest.raises(MissingField) as e:
        field(case, 'failing_tests')      # note the typo
    msg = str(e.value)
    assert 'failing_tests' in msg and 'failing_test' in msg


def test_the_error_warns_against_switching_to_get():
    """The tempting "fix" is .get(), which reintroduces the bug."""
    with pytest.raises(MissingField) as e:
        field({'a': 1}, 'b')
    assert '.get()' in str(e.value)


def test_an_explicit_default_is_honoured():
    assert field({'a': 1}, 'b', default='fallback') == 'fallback'


def test_default_none_must_be_deliberate_and_is_honoured():
    """default=None is a real choice, distinct from the no-default sentinel."""
    assert field({'a': 1}, 'b', default=None) is None


def test_present_field_is_returned_untouched():
    assert field({'gold': DISMISS}, 'gold') == DISMISS


def test_non_mapping_input_raises_rather_than_crashing_obscurely():
    with pytest.raises(MissingField):
        field(None, 'gold')


# --- the vocabulary -------------------------------------------------------

def test_canonical_values_are_self_describing():
    """The point of the rename: the value says what to DO, so it cannot be read
    as a statement about the patch."""
    assert KEEP == 'keep-finding'
    assert DISMISS == 'dismiss-finding'
    assert UNRESOLVED == 'unresolved'


def test_legacy_spellings_still_read_correctly():
    """Old recorded artefacts must stay readable — and must map the RIGHT way.
    SOUND means keep, which is the mapping that got inverted in prose."""
    assert gold_verdict({'gold': 'SOUND'}) == KEEP
    assert gold_verdict({'gold': 'UNSOUND'}) == DISMISS
    assert gold_verdict({'gold': 'UNRESOLVED'}) == UNRESOLVED


def test_sound_means_keep_not_patch_is_good():
    """Pins the semantics that were read backwards. gold=SOUND describes the
    CHECK: the finding is legitimate and must not be dropped."""
    assert must_keep({'gold': 'SOUND'}) is True
    assert must_dismiss({'gold': 'SOUND'}) is False


def test_unrecognised_verdict_raises_rather_than_being_guessed():
    with pytest.raises(ValueError):
        gold_verdict({'gold': 'PROBABLY_FINE'})


def test_missing_gold_raises():
    with pytest.raises(MissingField):
        gold_verdict({'fired_assertion': 'x'})


def test_unresolved_rows_are_not_scored():
    assert is_scored({'gold': UNRESOLVED}) is False
    assert is_scored({'gold': KEEP}) is True


# --- the shipped fixtures ------------------------------------------------

@pytest.mark.parametrize('name', FIXTURES)
def test_every_shipped_fixture_row_uses_the_canonical_vocabulary(name):
    """No legacy spellings left in the data — the migration is complete, so a
    reader that only understands canonical values is safe."""
    path = ROOT / 'tests' / 'fixtures' / name
    rows = [json.loads(l) for l in path.open()]
    assert rows, f'{name} is empty'
    for r in rows:
        assert r['gold'] in GOLD_VERDICTS, \
            f"{name}: non-canonical gold {r['gold']!r}"


def test_cases228_verdict_counts_survived_the_migration():
    """Verbatim counts, so a botched rewrite cannot pass silently."""
    rows = [json.loads(l) for l in
            (ROOT / 'tests/fixtures/cases228.jsonl').open()]
    assert len(rows) == 228
    assert sum(1 for r in rows if must_keep(r)) == 71
    assert sum(1 for r in rows if must_dismiss(r)) == 137
    assert sum(1 for r in rows if not is_scored(r)) == 20


def test_non_numeric_population_is_pinned_with_its_verdict_split():
    """Guards the twice-corrected population behind the rejected fix (ii).

    History, because both earlier numbers are quoted in committed documents:

    * Pre-fix-(i) it was **10 rows, all one FAKE patch** (Lang-41), all
      keep-finding. It had been described as "all correct patches, so fixing can
      only help precision" — false, and it licensed a fix that measurement then
      rejected.
    * Fix (i) enlarged it to **27 rows across three legs**, because recognising
      project assertion helpers extracts values from more tests. 21 keep-finding
      and 6 on a correct patch — so a precision opportunity does exist here now,
      which the pre-fix-(i) population genuinely did not contain.

    If this count moves again, fix (ii)'s park decision must be re-measured
    rather than inherited — that is the whole point of pinning it.
    """
    from java.relations.evidence_facts import _trusted_numbers
    from java.parsing.java_source import expected_assert_literals as EAL
    rows = [json.loads(l) for l in
            (ROOT / 'tests/fixtures/cases228.jsonl').open()]
    pop = [r for r in rows
           if (tv := list(dict.fromkeys(EAL(r.get('failing_test') or ''))))
           and not _trusted_numbers(tv)]
    assert len(pop) == 27, (
        'the non-numeric population moved; re-measure fix (ii) before '
        'inheriting its park decision (see evidence_facts.py NOT SHIPPED note)')
    assert sum(1 for r in pop if must_keep(r)) == 21
    assert sum(1 for r in pop if must_dismiss(r)) == 6
    legs = {'Lang-41' if 'Lang-41' in json.dumps(r['provenance']) else 'Lang-60'
            for r in pop}
    assert legs == {'Lang-41', 'Lang-60'}


def test_the_original_ten_were_all_one_fake_patch():
    """The specific false claim, pinned against the ORIGINAL JUnit-only
    extraction so the correction stays checkable after later extractor work."""
    from java.parsing.java_source import (ASSERT_EQ_RE, LITERAL_ARG_RE,
                                          split_top_level_args)
    from java.relations.evidence_facts import _trusted_numbers

    def junit_only(src):
        out = []
        for m in ASSERT_EQ_RE.finditer(src or ''):
            args = split_top_level_args(m.group(1))
            if len(args) < 2:
                continue
            cand = args[0]
            if (len(args) >= 3 and args[0].startswith('"')
                    and not args[1].startswith('"')):
                cand = args[1]
            if not LITERAL_ARG_RE.match(cand):
                continue
            lit = cand[1:-1] if cand.startswith(('"', "'")) else cand
            if lit and lit[-1] in 'fFdDlL' and any(c.isdigit() for c in lit):
                lit = lit[:-1]
            if len(lit) >= 3 and lit not in out:
                out.append(lit)
        return out

    rows = [json.loads(l) for l in
            (ROOT / 'tests/fixtures/cases228.jsonl').open()]
    ten = [r for r in rows
           if (tv := list(dict.fromkeys(junit_only(r.get('failing_test') or ''))))
           and not _trusted_numbers(tv)]
    assert len(ten) == 10
    assert all(must_keep(r) for r in ten)          # NOT "correct patches"
    assert all(r['label'] == 'overfitting' for r in ten)
    assert all('Lang-41' in json.dumps(r['provenance']) for r in ten)
