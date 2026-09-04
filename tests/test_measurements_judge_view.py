"""The judge's view: RCR for the one-shot LLM baseline.

Everything here is synthetic. The record is written in the shape a real
`baseline_llmjudge` record has (`evidence_facts`, `parity_manifest`,
`evidence_sha256`, `patch`, `label`, `project`, `bug_id`, `apr_tool`), and
the rendered evidence is the excerpt in
`tests/fixtures/measurements/judge_evidence_excerpt.txt`, which carries one
of each block the parser reads: a function block with its signature, a
call-site example, a callee declaration, and the capped reachable list.

The root-cause file is built from `locations` types and written in the
layout `cli.py` produces, so no checkout, build or model call is needed.

The synthetic bug:

  R (root_cause.json)   seed    org.ex.Alpha.transform(String,int)
                        callee  org.ex.Beta.helper(String)
                        callee  org.ex.Gamma.unseen()
                        caller  org.ex.Caller.drive(String)
  manifest              caller  org.ex.Manifest.run()

  J (what the judge saw, from the excerpt)
                        seed    org.ex.Alpha.transform(String,int)
                        callee  Beta.helper          (in R)
                        callee  Gamma.absent         (not in R)
                        caller  drive(String)        (class-less, in R)

so RCR full = 3/4, RCR R0 = 1/1, RCR R1 = 1/2 (the manifest frame was
never shown).
"""
import json
import os

import pytest

from java.measurements import judge_view as JV
from java.measurements import locations as loc

FIXTURES = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        'fixtures', 'measurements')
EVIDENCE_FIXTURE = os.path.join(FIXTURES, 'judge_evidence_excerpt.txt')

# --- the population the synthetic bug talks about --------------------------
SEED = loc.MethodRef('org.ex.Alpha', 'transform', ('String', 'int'))
B_helper = loc.MethodRef('org.ex.Beta', 'helper', ('String',))
G_unseen = loc.MethodRef('org.ex.Gamma', 'unseen', ())
C_drive = loc.MethodRef('org.ex.Caller', 'drive', ('String',))
M_run = loc.MethodRef('org.ex.Manifest', 'run', ())


@pytest.fixture(scope='module')
def evidence_text():
    with open(EVIDENCE_FIXTURE, encoding='utf-8') as fh:
        return fh.read()


# ---------------------------------------------------------------------------
# building the synthetic inputs
# ---------------------------------------------------------------------------

def _record(**over):
    """One baseline record, in the shape `run_one.classify` writes."""
    rec = {
        'label': 'correct',
        'status': 'evaluated',
        'bug_kind': 'semantic',
        'project': 'Ex',
        'bug_id': '1',
        'apr_tool': 'Tool',
        'patch': 'patchA-Ex-1-Tool.patch',
        'prompt_version': 's3.2',
        'context_degraded': False,
        'parity_manifest': {
            'renderer_version': 1,
            'blocks': [{'name': 'patch', 'origin': 'reused', 'chars': 120},
                       {'name': 'touched_function:transform',
                        'origin': 'reused', 'chars': 300},
                       {'name': 'root_cause_reachable', 'origin': 'rendered',
                        'chars': 60}],
            'total_chars': 480,
        },
        'evidence_facts': {
            'modified_files': ['src/main/java/org/ex/Alpha.java'],
            'package': 'org.ex',
            'touched_functions': ['transform'],
            'reachable_count': 10,
            'neighbourhood_notes': [],
            'trigger_tests': ['org.ex.AlphaTest::testTransform'],
        },
        'evidence_sha256': 'deadbeef' * 8,
        'prompt_chars': 900,
        'predicted_overfitting': False,
    }
    rec.update(over)
    return rec


def _root_cause(path, *, with_manifest=True):
    """A `root_cause.json` in the layout the measurement CLI writes."""
    methods = loc.MethodSet()
    methods.add(SEED, loc.SEED, 0)
    methods.add(B_helper, loc.CALLEE, 1)
    methods.add(G_unseen, loc.CALLEE, 1)
    methods.add(C_drive, loc.CALLER, 1)
    manifest = loc.MethodSet()
    if with_manifest:
        manifest.add(M_run, loc.CALLER, 1)
    lines = loc.LineSet()
    lines.add(loc.LineRef('org.ex.Alpha', 12), loc.SEED)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as fh:
        json.dump({'methods': methods.to_dict(),
                   'lines': lines.to_dict(),
                   'manifest': manifest.to_dict(),
                   'route': 'test',
                   'direction_assumed': 'fixed_to_buggy'}, fh)
    return path


def _leg(run_dir, name, result):
    leg = os.path.join(run_dir, name)
    os.makedirs(leg, exist_ok=True)
    with open(os.path.join(leg, 'result.jsonl'), 'w') as fh:
        fh.write(json.dumps(result) + '\n')
    return leg


# ---------------------------------------------------------------------------
# J — the set the judge was shown
# ---------------------------------------------------------------------------

def test_facts_only_gives_the_seed_ring_and_nothing_else():
    """With no evidence text the record yields seeds only: it stores the
    SIZE of the reachable list, never its members."""
    ms = JV.judge_shown_set(_record())
    assert [r for r in ms.refs()] == [loc.MethodRef('org.ex.Alpha',
                                                    'transform', ())]
    assert ms.refs(loc.CALLEE) == [] and ms.refs(loc.CALLER) == []
    sources = JV.shown_sources(_record())
    assert sources['seed_functions'] == 1
    assert sources['reachable_labels'] == 0
    # the count the record does keep, reported next to the empty ring
    assert sources['reachable_count'] == 10


def test_seed_names_fall_back_to_the_parity_manifest():
    """A record with no `evidence_facts` still has the block names."""
    rec = _record()
    rec.pop('evidence_facts')
    ms = JV.judge_shown_set(rec)
    # no package and no modified file, so the class is unknown
    assert ms.refs() == [loc.MethodRef('', 'transform', ())]


def test_evidence_text_gives_all_three_rings(evidence_text):
    ms = JV.judge_shown_set(_record(), evidence_text)
    assert ms.ring_of(SEED) == loc.SEED, 'signature gives the real arity'
    assert ms.ring_of(loc.MethodRef('Beta', 'helper', ())) == loc.CALLEE
    assert ms.ring_of(loc.MethodRef('Gamma', 'absent', ())) == loc.CALLEE
    # a call-site example is source text: the declaring class is not in it
    assert ms.ring_of(loc.MethodRef('', 'drive', ('String',))) == loc.CALLER
    # the junk label is kept as a name that resolved to nothing
    assert any('not a label' in u for u in ms.unmatched)
    assert len(ms) == 4


def test_every_member_is_tagged_as_the_judges(evidence_text):
    ms = JV.judge_shown_set(_record(), evidence_text)
    assert {ms.provenance_of(r) for r in ms.refs()} == {JV.PROVENANCE}


def test_sources_break_the_set_down_by_block(evidence_text):
    s = JV.shown_sources(_record(), evidence_text)
    assert s['seed_functions'] == 1
    assert s['reachable_labels'] == 3          # two names plus the junk one
    assert s['callee_declarations'] == 1       # <callee name="helper" ...>
    assert s['xrefs'] == 1
    assert s['reachable_omitted'] == 7         # the '(+7 more ...)' line


# ---------------------------------------------------------------------------
# RCR
# ---------------------------------------------------------------------------

def test_rcr_values_and_ring_breakdown(tmp_path, evidence_text):
    rc = _root_cause(str(tmp_path / 'measurements' / 'root_cause.json'))
    out = JV.rcr_for_judge(_record(), rc, evidence_text=evidence_text,
                           evidence_source='text')
    assert out['available'] is True
    assert out['evidence_source'] == 'text'

    full = out['rcr__method__full__na']
    assert (full['num'], full['den']) == (3, 4)
    assert full['value'] == pytest.approx(0.75)
    by_ring = full['by_ring']
    assert (by_ring['seed']['num'], by_ring['seed']['den']) == (1, 1)
    assert (by_ring['caller']['num'], by_ring['caller']['den']) == (1, 1)
    # Gamma.unseen is in R and was never shown; Gamma.absent was shown and
    # is not in R, so it cannot rescue the ring
    assert (by_ring['callee']['num'], by_ring['callee']['den']) == (1, 2)
    assert by_ring['callee']['value'] == pytest.approx(0.5)

    r0 = out['rcr__method__R0__na']
    assert (r0['num'], r0['den'], r0['value']) == (1, 1, 1.0)

    # R1 adds the manifestation frame, which the evidence never named
    r1 = out['rcr__method__R1__na']
    assert (r1['num'], r1['den']) == (1, 2)
    assert r1['by_ring']['caller']['num'] == 0
    assert r1['by_ring']['caller']['den'] == 1


def test_rcr_cross_table_says_which_ring_each_hit_sat_in(tmp_path,
                                                         evidence_text):
    rc = _root_cause(str(tmp_path / 'measurements' / 'root_cause.json'))
    out = JV.rcr_for_judge(_record(), rc, evidence_text=evidence_text)
    cross = out['rcr_cross__method__full__na']
    assert cross['seed']['seed'] == 1
    assert cross['caller']['caller'] == 1
    assert cross['callee']['callee'] == 1
    assert sum(sum(row.values()) for row in cross.values()) == 3


def test_sizes_and_unmatched_counts_are_reported(tmp_path, evidence_text):
    rc = _root_cause(str(tmp_path / 'measurements' / 'root_cause.json'))
    out = JV.rcr_for_judge(_record(), rc, evidence_text=evidence_text)
    assert out['sizes']['J_method'] == 4
    assert out['sizes']['J_method_by_ring'] == {'seed': 1, 'caller': 1,
                                               'callee': 2}
    assert out['sizes']['R_method'] == {'R0': 1, 'R1': 2, 'full': 4}
    assert out['sizes']['J_unresolved_names'] == 1      # the junk label
    m = out['matching']['method__full__primary']
    assert m['space'] == 'R', 'no coverage in the leg, so R is the space'
    assert m['r_unmatched'] == 0
    # Gamma.absent is the one shown name R has nothing for
    assert m['j_unmatched'] == 1


def test_facts_only_rcr_measures_the_seed_ring_alone(tmp_path):
    rc = _root_cause(str(tmp_path / 'measurements' / 'root_cause.json'))
    out = JV.rcr_for_judge(_record(), rc)
    assert out['available'] is True
    assert out['evidence_source'] == 'facts'
    # the seed still matches: no parameter types, so arity is ignored
    assert out['rcr__method__R0__na']['num'] == 1
    full = out['rcr__method__full__na']
    assert (full['num'], full['den']) == (1, 4)


def test_a_record_without_evidence_is_unavailable_not_an_error(tmp_path):
    rc = _root_cause(str(tmp_path / 'measurements' / 'root_cause.json'))
    rec = _record(status='semantic_skip',
                  detail='semantic bug (trigger test asserts)')
    rec.pop('evidence_facts')
    rec.pop('parity_manifest')
    out = JV.rcr_for_judge(rec, rc)
    assert out['available'] is False
    assert 'no evidence' in out['reason']
    assert out['project'] == 'Ex' and out['patch_stem'] == 'patchA-Ex-1-Tool'


def test_a_missing_root_cause_file_is_unavailable_not_an_error(tmp_path):
    out = JV.rcr_for_judge(_record(), str(tmp_path / 'nope.json'))
    assert out['available'] is False
    assert 'root_cause' in out['reason']


# ---------------------------------------------------------------------------
# recovering the evidence text, under the digest gate
# ---------------------------------------------------------------------------

def _cache_entry(cache_dir, stem, text):
    os.makedirs(cache_dir, exist_ok=True)
    with open(os.path.join(cache_dir, stem + '.json'), 'w') as fh:
        json.dump({'project': 'Ex', 'bug_id': '1', 'text': text}, fh)


def test_cache_entry_is_used_only_when_its_digest_matches(tmp_path,
                                                          evidence_text):
    import hashlib
    cache = str(tmp_path / 'cache')
    _cache_entry(cache, 'patchA-Ex-1-Tool', evidence_text)

    digest = hashlib.sha256(evidence_text.encode()).hexdigest()
    text, why = JV.evidence_text_for(_record(evidence_sha256=digest), [cache])
    assert why == 'verified' and text == evidence_text

    # the cache was rebuilt since the run: the text no longer belongs to
    # this record, so it is refused rather than attributed
    text, why = JV.evidence_text_for(_record(), [cache])
    assert text is None and why == 'digest_mismatch'

    text, why = JV.evidence_text_for(_record(patch='other.patch'), [cache])
    assert text is None and why == 'not_cached'


def test_an_injected_evidence_text_wins(evidence_text):
    rec = _record(evidence_text=evidence_text)
    text, why = JV.evidence_text_for(rec, [])
    assert why == 'injected' and text == evidence_text


# ---------------------------------------------------------------------------
# pairing records with the legs of a measured run
# ---------------------------------------------------------------------------

def _run_with_legs(tmp_path):
    run = str(tmp_path / 'run')
    os.makedirs(run, exist_ok=True)
    _leg(run, '01_patchA-Ex-1-Tool_c',
         {'project': 'Ex', 'bug_id': '1', 'apr_tool': 'Tool',
          'label': 'correct', 'status': 'evaluated'})
    _leg(run, '02_patchB-Ex-1-Tool_c',
         {'project': 'Ex', 'bug_id': 1, 'apr_tool': 'Tool',
          'label': 'correct', 'status': 'evaluated'})
    _leg(run, '03_patchA-Ex-2-Other_o',
         {'project': 'Ex', 'bug_id': '2', 'apr_tool': 'Other',
          'label': 'overfitting', 'status': 'evaluated'})
    return run


def _records_file(tmp_path, records):
    path = str(tmp_path / 'records.jsonl')
    with open(path, 'w') as fh:
        for r in records:
            fh.write(json.dumps(r) + '\n')
    return path


def test_pairing_by_identity_and_by_patch_stem(tmp_path):
    run = _run_with_legs(tmp_path)
    records = _records_file(tmp_path, [
        # two legs share (Ex, 1, Tool, correct); the patch stem breaks it
        _record(),
        # the only leg with this identity
        _record(project='Ex', bug_id='2', apr_tool='Other',
                label='overfitting', patch='patchA-Ex-2-Other.patch'),
        # no leg at all
        _record(project='Ex', bug_id='9', apr_tool='Nobody',
                patch='patchA-Ex-9-Nobody.patch'),
        # identity ties and the stem names neither leg
        _record(patch='patchZ-Ex-1-Tool.patch'),
    ])
    rows = JV.pair_records_with_legs(records, run)
    assert [r['match'] for r in rows] == ['patch_stem', 'identity', 'none',
                                          'ambiguous']
    assert rows[0]['leg'] == '01_patchA-Ex-1-Tool_c'
    assert rows[0]['n_candidates'] == 2
    assert rows[1]['leg'] == '03_patchA-Ex-2-Other_o'
    assert rows[2]['leg_dir'] is None
    assert rows[3]['leg_dir'] is None, 'a tie is reported, never guessed'


def test_bug_id_is_compared_as_a_string_on_both_sides(tmp_path):
    """A leg writes `bug_id` as an int in one run and a string in another;
    the pairing must not care."""
    run = _run_with_legs(tmp_path)
    records = _records_file(tmp_path, [_record(bug_id=1,
                                               patch='patchB-Ex-1-Tool.patch')])
    rows = JV.pair_records_with_legs(records, run)
    assert rows[0]['leg'] == '02_patchB-Ex-1-Tool_c'


# ---------------------------------------------------------------------------
# the run-level entry point and the CLI
# ---------------------------------------------------------------------------

def test_judge_rows_carry_the_pipelines_rcr_beside_the_judges(tmp_path,
                                                              evidence_text):
    run = _run_with_legs(tmp_path)
    _root_cause(os.path.join(run, '01_patchA-Ex-1-Tool_c', 'measurements',
                             'root_cause.json'))
    # the pipeline's own row for that leg, as `metrics.write_metrics` wrote it
    with open(os.path.join(run, 'metrics.jsonl'), 'w') as fh:
        fh.write(json.dumps({
            'leg': '01_patchA-Ex-1-Tool_c',
            'rcr__method__full__na': {'value': 1.0, 'num': 4, 'den': 4},
            'rcr__method__R0__na': {'value': 1.0, 'num': 1, 'den': 1},
            'rcr__method__R1__na': {'value': 0.5, 'num': 1, 'den': 2},
            'sizes': {'P_method': 9,
                      'P_method_by_ring': {'seed': 1, 'caller': 2,
                                           'callee': 6}},
        }) + '\n')
    records = _records_file(tmp_path,
                            [_record(evidence_text=evidence_text)])

    rows = JV.judge_rows(records, run)
    assert len(rows) == 1
    row = rows[0]
    assert row['available'] is True
    assert row['evidence_recovery'] == 'injected'
    assert row['rcr__method__full__na']['num'] == 3
    assert row['pipeline']['rcr__method__full__na']['num'] == 4
    assert row['pipeline_sizes']['P_method'] == 9

    table = JV.render_table(rows)
    assert 'patchA-Ex-1-Tool' in table
    assert '0.75/1.00' in table, 'judge next to pipeline, same R'


def test_a_leg_with_no_root_cause_file_still_gets_a_row(tmp_path):
    run = _run_with_legs(tmp_path)
    records = _records_file(tmp_path, [_record()])
    rows = JV.judge_rows(records, run)
    assert rows[0]['available'] is False
    assert 'root_cause' in rows[0]['reason']
    # and the table renders it instead of raising
    assert 'patchA-Ex-1-Tool' in JV.render_table(rows)


def test_cli_writes_one_json_row_per_record(tmp_path, capsys):
    run = _run_with_legs(tmp_path)
    _root_cause(os.path.join(run, '01_patchA-Ex-1-Tool_c', 'measurements',
                             'root_cause.json'))
    records = _records_file(tmp_path, [_record()])
    out = str(tmp_path / 'judge_rcr.jsonl')
    assert JV.main([records, run, '--out', out]) == 0
    printed = capsys.readouterr().out
    assert 'Evaluation only' in printed
    lines = [json.loads(l) for l in open(out) if l.strip()]
    assert len(lines) == 1
    assert lines[0]['available'] is True
    assert 'record' not in lines[0], 'the row is the measurement, not a copy'
    assert lines[0]['rcr__method__R0__na']['den'] == 1


# ---------------------------------------------------------------------------
# the overload retry
# ---------------------------------------------------------------------------

OVERLOAD = loc.MethodRef('org.ex.Alpha', 'transform', ('String',))


def _coverage(mdir, all_methods):
    """A `coverage_buggy.json`, so the identity space is the build's whole
    method list — the space `metrics.compute_leg` uses when a leg has one."""
    os.makedirs(mdir, exist_ok=True)
    with open(os.path.join(mdir, 'coverage_buggy.json'), 'w') as fh:
        json.dump({'build': 'buggy', 'methods': [],
                   'lines': [], 'branches_covered': 0, 'branches_total': 0,
                   'all_methods': [m.to_dict() for m in all_methods]}, fh)


def test_an_overloaded_name_is_retried_against_r_alone(tmp_path):
    """The evidence spells a method without its parameter types. When the
    build's whole method list holds two `transform`s the name is ambiguous
    there, but R holds exactly one, so the seed still counts — and the
    retry is counted so the row can be re-read without it."""
    mdir = str(tmp_path / 'measurements')
    rc = _root_cause(os.path.join(mdir, 'root_cause.json'))
    _coverage(mdir, [SEED, OVERLOAD, B_helper, G_unseen, C_drive])

    out = JV.rcr_for_judge(_record(), rc)
    m = out['matching']['method__R0__primary']
    assert m['space'] == 'coverage_all_methods'
    assert m['j_by_name_in_r'] == 1
    assert out['rcr__method__R0__na']['num'] == 1


def test_a_name_ambiguous_inside_r_is_left_unmatched(tmp_path):
    """The retry fails closed: when R itself holds both overloads the name
    identifies no single member, so nothing is credited."""
    mdir = str(tmp_path / 'measurements')
    methods = loc.MethodSet()
    methods.add(SEED, loc.SEED, 0)
    methods.add(OVERLOAD, loc.CALLEE, 1)
    os.makedirs(mdir, exist_ok=True)
    with open(os.path.join(mdir, 'root_cause.json'), 'w') as fh:
        json.dump({'methods': methods.to_dict(),
                   'manifest': loc.MethodSet().to_dict()}, fh)
    _coverage(mdir, [SEED, OVERLOAD])

    out = JV.rcr_for_judge(_record(), os.path.join(mdir, 'root_cause.json'))
    m = out['matching']['method__full__primary']
    assert m['j_by_name_in_r'] == 0
    assert m['j_unmatched'] == 1
    assert out['rcr__method__full__na']['num'] == 0


def test_the_signature_removes_the_need_for_the_retry(tmp_path,
                                                      evidence_text):
    """With the evidence text the seed carries its real parameter types, so
    it matches exactly and no retry is involved."""
    mdir = str(tmp_path / 'measurements')
    rc = _root_cause(os.path.join(mdir, 'root_cause.json'))
    _coverage(mdir, [SEED, OVERLOAD, B_helper, G_unseen, C_drive])
    out = JV.rcr_for_judge(_record(), rc, evidence_text=evidence_text,
                           evidence_source='text')
    assert out['matching']['method__R0__primary']['j_by_name_in_r'] == 0
    assert out['rcr__method__R0__na']['num'] == 1
