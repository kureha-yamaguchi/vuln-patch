"""Guards for the Project Zero baseline in src/baseline_llmjudge/project_zero/.

Five properties are asserted here, and each one is a claim the README makes:

  1. THE FIREWALL HOLDS. No clean view of any fix in the dataset carries a CVE
     id, a tracker id, a date, a commit message or a metadata header. The
     sweep runs over every pair, so a new pair cannot enter the dataset with a
     leak the tests never saw.
  2. NO LABEL LEAKAGE. No rendered block and no built prompt names the label,
     the pair, the side or the relationship verdict.
  3. THE TWO CLASSES ARE RENDERED THE SAME WAY. A positive fix and a negative
     fix draw their blocks from one list, in one order, under one set of
     headings. Only the diff and the source differ. This is the test that
     matters most: the negative class is the later fix of the same pair, so a
     systematic surface difference would be scored instead of the code.
  4. INPUT PARITY. The `patch` block really is the C/C++ front-end's own text.
  5. THE TWO PROMPT REGISTRIES ARE SEPARATE. Nothing in prompts_pz can move a
     recorded Defects4J digest.

Every test here is offline. No model call, no network request, and no fetched
context is required.
"""
import re

import pytest

from oss_fuzz.prompts import LibFuzzerPromptBuilder

from baseline_llmjudge.defects4j import prompts as prompts_d4j
from baseline_llmjudge.project_zero import (compare, evidence, firewall,
                                            prompts, queue, split)
from baseline_llmjudge.project_zero.evaluate import _baselines
from baseline_llmjudge.shared.version import PromptVersion

PAIRS = firewall.read_pairs()

# One pair with both sides present, used wherever a test needs a concrete fix.
FIXTURE_PAIR = next(p for p in PAIRS
                    if p.name == 'CVE-2021-30551__CVE-2022-1096')

#: Every token that would tell the model which side of a pair it is reading.
#: `fix0` and `fix1` are the file names, and `incomplete_fix` and its two
#: siblings are the `relationship_kind` values.
LABEL_TOKENS = ('fix0', 'fix1', 'prior_cve', 'later_cve', 'incomplete_fix',
                'same_root_cause', 'one_extends_other', 'deep_reasoning',
                'deep_confidence', 'overfitting_label')

#: Patterns that must not survive the firewall, anywhere in a clean view.
LEAK_PATTERNS = {
    'a CVE id': re.compile(r'CVE-\d{4}-\d'),
    'an ISO date': re.compile(r'\b(?:19|20)\d\d-\d\d-\d\d\b'),
    'a crbug reference': re.compile(r'(?i)crbug'),
    'a bug number': re.compile(r'(?i)\bbug\s*[:#-]?\s*\d{4,}'),
    'a Fixes: tag': re.compile(r'(?mi)^\s*Fixes:'),
    'Gerrit metadata': re.compile(
        r'(?i)Change-Id|Reviewed-on|Cr-Commit-Position'),
    'an upstream commit id': re.compile(r'(?i)commit [0-9a-f]{8,}'),
    'a Phabricator link': re.compile(r'(?i)Differential Revision'),
    'a Mercurial header': re.compile(r'(?m)^# (?:Node ID|Parent|Date|User|HG)'),
    'a mail Date header': re.compile(r'(?m)^Date:'),
    'a mail Subject header': re.compile(r'(?m)^Subject:'),
    'a tracker id in a path': re.compile(
        r'(?m)^(?:diff --git|---|\+\+\+).*\d{6,}'),
}

#: Phrases that only make sense to something that writes a libFuzzer harness.
#: The baseline emits no .c or .cc file, so a prompt version carrying one of
#: these would prove that a harness-authorship section was carried over.
AUTHORSHIP_TOKENS = ('LLVMFuzzerTestOneInput', 'FuzzedDataProvider',
                     'your harness', 'Your harness', 'no markdown fences',
                     'metamorphic relation', 'byte-carving',
                     'const uint8_t *data')


def _fix(pair, which):
    return firewall.clean_view(pair, which)


def _blob(fix):
    """Every character of a clean view that could reach a prompt."""
    return '\n'.join([fix.diff, *(t for _, t in fix.sources),
                      *fix.touched_files, fix.codebase, fix.software])


# --- 1. the firewall holds ---------------------------------------------------

@pytest.mark.parametrize('pair', PAIRS, ids=lambda p: p.name)
def test_no_clean_view_carries_a_leak(pair):
    """Every clean view of every pair, swept for every leak pattern."""
    for which in firewall.WHICH:
        fix = _fix(pair, which)
        blob = _blob(fix)
        for description, pattern in LEAK_PATTERNS.items():
            found = pattern.search(blob)
            assert found is None, (
                f'{pair.name}/{which} leaks {description}: '
                f'{found.group(0)!r}')


def test_scrub_masks_a_cve_id_and_a_date():
    text, masked = firewall.scrub('This is CVE-2020-15999 on 2020-10-19.')
    assert 'CVE-2020-15999' not in text
    assert '2020-10-19' not in text
    assert masked == 2


def test_scrub_masks_a_tracker_id_inside_a_file_path():
    """v8 names a regression test after the bug it covers."""
    line = ('diff --git a/test/mjsunit/regress-crbug-1228407.js '
            'b/test/mjsunit/regress-crbug-1228407.js')
    text, masked = firewall.scrub(line)
    assert '1228407' not in text
    assert masked >= 1


def test_scrub_leaves_a_numeric_constant_in_code_alone():
    """The path rule is scoped to diff header lines, not to code."""
    text, _ = firewall.scrub('+  size_t limit = 1048576;')
    assert '1048576' in text


def test_drop_commit_message_starts_at_the_first_diff_line():
    raw = ('Subject: fix the thing\n\nBody text.\n'
           'diff --git a/x.c b/x.c\n+int y;\n')
    body, dropped = firewall.drop_commit_message(raw)
    assert body.startswith('diff --git')
    assert dropped == raw.index('diff --git')


def test_drop_metadata_lines_removes_an_embedded_header():
    """Four Bugzilla attachments hold a second changeset header mid-diff."""
    raw = ('diff --git a/x.c b/x.c\n+int y;\n'
           '# HG changeset patch\n'
           '# Node ID deadbeefdeadbeefdeadbeefdeadbeefdeadbeef\n'
           '# Parent  cafebabecafebabecafebabecafebabecafebabe\n'
           'diff --git a/z.c b/z.c\n+int w;\n')
    body, dropped = firewall.drop_metadata_lines(raw)
    assert '# Node ID' not in body
    assert '# Parent' not in body
    assert dropped == 3
    # The diff content survives.
    assert '+int y;' in body and '+int w;' in body


def test_a_shared_fix_gets_one_id():
    """The id comes from the commit, so two pairs of one fix share it."""
    same = [p for p in PAIRS if p.prior_cve == 'CVE-2019-13720']
    assert len(same) > 1
    ids = {firewall.fix_id(p.fix0_commit) for p in same}
    assert len(ids) == 1


def test_the_clean_view_carries_no_pair_name_and_no_side():
    fix = _fix(FIXTURE_PAIR, 'fix0')
    for field in (fix.fix_id, fix.codebase, fix.software):
        assert 'CVE' not in field
        assert 'fix0' not in field and 'fix1' not in field


# --- 2. no label leakage -----------------------------------------------------

@pytest.mark.parametrize('version', prompts.known_versions())
@pytest.mark.parametrize('which', firewall.WHICH)
def test_no_label_reaches_a_pz_prompt(version, which):
    fix = _fix(FIXTURE_PAIR, which)
    blocks = evidence.render(fix)
    built = '\n'.join(
        m['content'] for m in prompts.build_messages(
            version, evidence.evidence_text(blocks)))
    for token in LABEL_TOKENS:
        assert token not in built, f'{version} leaks {token!r}'


@pytest.mark.parametrize('which', firewall.WHICH)
def test_no_label_reaches_a_rendered_block(which):
    for block in evidence.render(_fix(FIXTURE_PAIR, which)):
        for token in LABEL_TOKENS:
            assert token not in block.text, f'{block.name} leaks {token!r}'


@pytest.mark.parametrize('version', prompts.known_versions())
def test_no_prompt_version_teaches_harness_authorship(version):
    """The baseline emits no harness, so no version may explain how to."""
    text = prompts.version_text(version)
    for token in AUTHORSHIP_TOKENS:
        assert token not in text, f'{version} carries {token!r}'


# --- 3. the two classes are rendered the same way ----------------------------

@pytest.mark.parametrize('pair', PAIRS, ids=lambda p: p.name)
def test_both_sides_render_the_same_blocks(pair):
    """The negative class must not be identifiable by its block set.

    Three blocks are unconditional. `touched_source` follows the fetch, so its
    presence tracks `fix.sources` and never the label."""
    for which in firewall.WHICH:
        fix = _fix(pair, which)
        blocks = {b.name: b for b in evidence.render(fix)}
        assert set(blocks) <= set(evidence.RENDERED_BLOCK_NAMES)
        assert {'patch', 'touched_files', 'codebase'} <= set(blocks)
        assert ('touched_source' in blocks) == bool(fix.sources)


@pytest.mark.parametrize('pair', PAIRS, ids=lambda p: p.name)
def test_both_sides_render_the_same_headings(pair):
    """One heading per block, and the same one on both sides."""
    def headings(which):
        return {b.name: b.text.splitlines()[0]
                for b in evidence.render(_fix(pair, which))}
    a, b = headings('fix0'), headings('fix1')
    for name in set(a) & set(b):
        assert a[name] == b[name], f'{name} heading differs by side'


def test_blocks_are_rendered_in_the_declared_order():
    names = [b.name for b in evidence.render(_fix(FIXTURE_PAIR, 'fix0'))]
    declared = [n for n in evidence.RENDERED_BLOCK_NAMES if n in names]
    assert names == declared


# --- 4. input parity ---------------------------------------------------------

def test_the_patch_block_is_the_pipelines_own_text():
    """The one reused block equals the C/C++ front-end method's output."""
    fix = _fix(FIXTURE_PAIR, 'fix0')
    blocks = {b.name: b for b in evidence.render(fix)}
    expected = LibFuzzerPromptBuilder(language='c++')._patch_block(fix.diff)
    assert blocks['patch'].text == expected
    assert blocks['patch'].origin == 'reused'


def test_manifest_names_every_block_and_the_withheld_evidence():
    fix = _fix(FIXTURE_PAIR, 'fix0')
    blocks = evidence.render(fix)
    m = evidence.manifest(fix, blocks)
    assert [b['name'] for b in m['blocks']] == [b.name for b in blocks]
    assert m['total_chars'] == sum(b.chars for b in blocks)
    assert m['parity_target'].startswith('oss_fuzz.prompts')
    assert list(evidence.WITHHELD_PIPELINE_EVIDENCE) == \
        m['withheld_pipeline_evidence']
    # The commit message is withheld by the firewall, not absent from the
    # data. The README says so, and the manifest must say so too.
    assert 'commit_message' in m['withheld_pipeline_evidence']
    assert m['scrub_report'] == fix.scrub_report


# --- 5. the two prompt registries are separate -------------------------------

def test_importing_prompts_pz_adds_no_defects4j_version():
    """A Project Zero version must not appear in the Defects4J registry."""
    for name in prompts.known_versions():
        assert name not in prompts_d4j.VERSIONS


def test_the_two_modules_carry_separate_system_messages():
    """One shared SYSTEM would tie every recorded digest to both datasets."""
    assert prompts.SYSTEM != prompts_d4j.SYSTEM


def test_a_pz_iteration_name_is_invalid_in_the_defects4j_grammar():
    assert prompts.is_iteration('p2.1')
    assert not prompts_d4j.is_iteration('p2.1')


@pytest.mark.parametrize('version', prompts.known_versions())
def test_every_registered_version_demands_the_verdict_contract(version):
    text = prompts.version_text(version)
    assert 'VERDICT: OVERFITTING' in text
    assert 'VERDICT: CORRECT' in text


@pytest.mark.parametrize('version', prompts.known_versions())
def test_every_registered_version_builds_two_messages(version):
    messages = prompts.build_messages(version, 'EVIDENCE')
    assert [m['role'] for m in messages] == ['system', 'user']
    assert 'EVIDENCE' in messages[1]['content']


def test_register_refuses_a_duplicate_name():
    """A scored version is frozen, so a second registration is refused."""
    version = PromptVersion(name='p3.9', hypothesis='x', task='x',
                                    instruction='x')
    prompts.register(version)
    try:
        with pytest.raises(ValueError, match='already registered'):
            prompts.register(version)
    finally:
        prompts.VERSIONS.pop('p3.9', None)


def test_register_refuses_a_name_that_is_not_an_iteration():
    with pytest.raises(ValueError, match='not an iteration name'):
        prompts.register(PromptVersion(
            name='p4', hypothesis='x', task='x', instruction='x'))


def test_register_refuses_an_unknown_base():
    with pytest.raises(ValueError, match='unknown base'):
        prompts.register(PromptVersion(
            name='p9.1', hypothesis='x', task='x', instruction='x'))


def test_resolve_refuses_an_unregistered_version():
    with pytest.raises(ValueError, match='unknown prompt version'):
        prompts.resolve('p2.7')


# --- the queue ---------------------------------------------------------------

QUEUE_ROWS, QUEUE_STATS = queue.build_queue()


def test_the_queue_holds_one_row_per_distinct_commit():
    ids = [r.fix.fix_id for r in QUEUE_ROWS]
    assert len(ids) == len(set(ids))


def test_a_positive_row_is_always_a_prior_fix():
    for row in QUEUE_ROWS:
        expected = 'fix0' if row.label == 'overfitting' else 'fix1'
        assert row.which == expected


def test_no_negative_row_is_a_prior_fix_elsewhere():
    """A fix known to have left a sibling bug cannot be a negative."""
    ever_fix0 = {p.fix0_commit for p in PAIRS if p.fix0_commit}
    ever_prior = {p.prior_cve for p in PAIRS if p.prior_cve}
    by_name = {p.name: p for p in PAIRS}
    for row in QUEUE_ROWS:
        if row.label != 'correct':
            continue
        pair = by_name[row.pair]
        assert pair.fix1_commit not in ever_fix0
        assert pair.later_cve not in ever_prior


def test_every_queued_row_carries_fetched_source():
    """`require_source` is the default, so the population is uniform."""
    for row in QUEUE_ROWS:
        assert row.fix.sources, f'{row.fix.fix_id} has no fetched source'


def test_the_population_stats_add_up():
    assert QUEUE_STATS['rows'] == len(QUEUE_ROWS)
    assert (QUEUE_STATS['overfitting'] + QUEUE_STATS['correct']
            == QUEUE_STATS['rows'])
    assert not QUEUE_STATS['duplicate_commits_with_differing_diffs']


# --- the baselines that read no code -----------------------------------------

def test_the_size_rule_finds_a_population_separable_by_size():
    rows = [{'label': 'overfitting', 'diff_chars': 100,
             'decisions': {'majority': False}} for _ in range(5)]
    rows += [{'label': 'correct', 'diff_chars': 9000,
              'decisions': {'majority': True}} for _ in range(5)]
    size = _baselines(rows)['size_rule']
    assert size['f1'] == 1.0
    assert size['threshold_diff_chars'] == 100


def test_the_size_rule_excludes_the_degenerate_threshold():
    """The largest diff length predicts every row positive.

    That rule is `always_positive` under another name, so it must not be
    reported as evidence of a size confound."""
    rows = [{'label': 'overfitting', 'diff_chars': n,
             'decisions': {'majority': False}} for n in (100, 9000)]
    rows += [{'label': 'correct', 'diff_chars': n,
              'decisions': {'majority': False}} for n in (200, 8000)]
    out = _baselines(rows)
    assert out['size_rule']['threshold_diff_chars'] != 9000
    assert out['always_positive']['recall'] == 1.0
    assert out['always_positive']['precision'] == 0.5


def test_the_size_rule_is_none_when_only_one_size_exists():
    rows = [{'label': 'overfitting', 'diff_chars': 100, 'decisions': {}},
            {'label': 'correct', 'diff_chars': 100, 'decisions': {}}]
    assert _baselines(rows)['size_rule'] is None


# --- the frozen split --------------------------------------------------------

SPLIT_ROWS, SPLIT_STATS = split.build_split()
GROUPS = split.root_cause_groups(PAIRS)


def test_every_pair_belongs_to_exactly_one_group():
    seen = [name for names in GROUPS.values() for name in names]
    assert sorted(seen) == sorted(p.name for p in PAIRS)
    assert len(seen) == len(set(seen))


def test_a_shared_prior_fix_makes_one_group():
    """Six pairs share the prior fix of CVE-2019-13720."""
    same = [p.name for p in PAIRS if p.prior_cve == 'CVE-2019-13720']
    assert len(same) == 6
    gids = {gid for gid, names in GROUPS.items()
            if set(names) & set(same)}
    assert len(gids) == 1


def test_a_dual_role_cve_chains_two_pairs_into_one_group():
    """CVE-2022-1096 is a later CVE in one pair and a prior in another."""
    chained = [p.name for p in PAIRS
               if 'CVE-2022-1096' in (p.prior_cve, p.later_cve)]
    assert len(chained) > 1
    gids = {gid for gid, names in GROUPS.items() if set(names) & set(chained)}
    assert len(gids) == 1


def test_both_fixes_of_a_pair_land_on_one_side():
    """The contamination guard, and the reason the group is the split unit.

    The two fixes of a pair repair one root cause and often touch one file. A
    judge tuned on one of them and scored on the other would be scored on code
    it was tuned against."""
    side_of_pair = {name: row['side']
                    for row in SPLIT_ROWS for name in row['pairs']}
    for pair in PAIRS:
        assert pair.name in side_of_pair
    for row in SPLIT_ROWS:
        sides = {side_of_pair[name] for name in row['pairs']}
        assert len(sides) == 1, f"group {row['group']} straddles two sides"


def test_a_group_is_never_split_across_sides():
    by_group = {}
    for row in SPLIT_ROWS:
        assert row['group'] not in by_group
        by_group[row['group']] = row['side']
    assert set(by_group.values()) <= set(split.SIDES)


def test_the_split_is_deterministic():
    again, again_stats = split.build_split()
    assert again == SPLIT_ROWS
    assert again_stats == SPLIT_STATS


def test_both_sides_hold_rows():
    for side in split.SIDES:
        assert SPLIT_STATS['sides'][side]['rows_at_freeze'] > 0


def test_the_two_sides_are_roughly_balanced():
    """Groups vary in size, so the balance is approximate by construction."""
    counts = [SPLIT_STATS['sides'][s]['rows_at_freeze'] for s in split.SIDES]
    assert abs(counts[0] - counts[1]) <= max(counts) * 0.35


def test_the_stats_add_up():
    assert SPLIT_STATS['groups'] == len(SPLIT_ROWS)
    assert SPLIT_STATS['pairs'] == len(PAIRS)
    for side in split.SIDES:
        s = SPLIT_STATS['sides'][side]
        assert s['overfitting_at_freeze'] + s['correct_at_freeze'] == \
            s['rows_at_freeze']


def test_the_queue_honours_a_side():
    sides = {name: row['side']
             for row in SPLIT_ROWS for name in row['pairs']}
    whole, _ = queue.build_queue()
    parts = []
    for side in split.SIDES:
        rows, _ = queue.build_queue(side=side, sides=sides)
        for row in rows:
            assert sides[row.pair] == side
        parts.extend(row.fix.fix_id for row in rows)
    # The two sides partition the population: no row is lost, none is doubled.
    assert sorted(parts) == sorted(r.fix.fix_id for r in whole)


# --- the comparison ----------------------------------------------------------

def test_floor_of_takes_the_higher_baseline():
    run = {'baselines': {'always_positive': {'f1': 0.72},
                         'size_rule': {'f1': 0.69}}}
    assert compare.floor_of(run) == 0.72


def test_floor_of_survives_a_missing_size_rule():
    run = {'baselines': {'always_positive': {'f1': 0.72},
                         'size_rule': None}}
    assert compare.floor_of(run) == 0.72


def test_floor_of_is_none_without_baselines():
    assert compare.floor_of({}) is None


def test_select_prefers_the_higher_f1():
    runs = [{'prompt_version': 'p1', 'headline': {'f1': 0.4, 'FP': 1}},
            {'prompt_version': 'p2', 'headline': {'f1': 0.6, 'FP': 9}}]
    assert compare.select(runs)['prompt_version'] == 'p2'


def test_select_breaks_a_tie_on_fewer_false_positives():
    runs = [{'prompt_version': 'p1', 'headline': {'f1': 0.5, 'FP': 7}},
            {'prompt_version': 'p2', 'headline': {'f1': 0.5, 'FP': 2}}]
    assert compare.select(runs)['prompt_version'] == 'p2'


def test_select_ignores_an_unscored_run():
    runs = [{'prompt_version': 'p1', 'headline': {'f1': None, 'FP': 0}}]
    assert compare.select(runs) is None
