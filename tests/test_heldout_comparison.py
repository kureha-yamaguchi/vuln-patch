"""Budget, measurement missingness, population, and paired inference checks."""
import json
from pathlib import Path
from types import SimpleNamespace

import pytest

from java.harness.campaign import HarnessCampaign
from java.harness.build import BuildResult
from java.measurements.heldout_comparison import cli, report
from metrics.core import locations as loc


SOURCE = ('public class FuzzHarness { public static void fuzzerTestOneInput('
          'com.code_intelligence.jazzer.api.FuzzedDataProvider data) {} }')


def test_fixed_budget_counts_invalid_compile_failure_silent_and_accepted(monkeypatch, tmp_path):
    import java.harness.campaign as campaign
    monkeypatch.setattr(campaign, 'record_event', lambda *a, **kw: None)
    generated = iter(['invalid', SOURCE, SOURCE, SOURCE])
    labels = []

    def build(source, buggy_dir, output_subdir):
        labels.append(output_subdir)
        return BuildResult(str(tmp_path / output_subdir / 'FuzzHarness.java'),
                           'FuzzHarness', '', output_subdir != 'attempt_002',
                           0, '', '', attempt_label=output_subdir)

    def verify(build):
        return SimpleNamespace(crashed=build.attempt_label == 'attempt_004',
                               signature='Crash', reached_functions=[], stdout='', stderr='',
                               timed_out=False)

    builder = SimpleNamespace(looks_like_harness=lambda raw: 'invalid' if raw == 'invalid' else None,
                              extract_source=lambda raw: raw, build=build)
    runner = HarnessCampaign(SimpleNamespace(generate=lambda msgs: next(generated)), builder,
                             target_successes=4, max_attempts=4,
                             verifier=SimpleNamespace(verify=verify), count_invalid_attempts=True)
    # Presentation reads diagnostics that are irrelevant to these fake builds.
    for name in ('_print_failure', '_print_no_trigger', '_print_success'):
        monkeypatch.setattr(runner, name, lambda *a: None)
    result = runner.run([{'role': 'user', 'content': 'generate'}], str(tmp_path))
    assert result.attempts == 4
    assert labels == ['attempt_002', 'attempt_003', 'attempt_004']
    assert [a['compiled'] for a in result.candidate_attempts] == [False, False, True, True]
    assert [a['accepted'] for a in result.candidate_attempts] == [False, False, False, True]
    assert result.achieved_successes == 1


def make_leg(tmp_path, outcomes):
    leg = tmp_path / 'leg'
    (leg / 'cov').mkdir(parents=True)
    (leg / 'fuzz_out').mkdir()
    (leg / 'measurements').mkdir()
    attempts = [dict(attempt_label=f'attempt_{i:03d}', compiled=c, crashed_buggy=a, accepted=a)
                for i, (c, a) in enumerate(outcomes, 1)]
    rec = dict(status='candidate_experiment', candidate_budget=len(attempts), candidate_attempts=attempts)
    (leg / 'result.jsonl').write_text(json.dumps(rec) + '\n')
    ms = loc.MethodSet()
    for name in ('a', 'b'):
        ms.add(loc.MethodRef('p.C', name, ()), loc.SEED)
    report.write_json(leg / 'measurements/root_cause.json',
                      {'methods': ms.to_dict(), 'trigger_gate': {'passed': True}})
    # P holds one root method, one method outside R0, and one JDK callee the
    # project's coverage can never contain.
    ps = loc.MethodSet()
    ps.add(loc.MethodRef('p.C', 'a', ()), loc.SEED)
    ps.add(loc.MethodRef('p.C', 'c', ()), loc.CALLEE, 1)
    ps.add(loc.MethodRef('java.lang.StringBuilder', 'append', ()), loc.CALLEE, 1)
    report.write_json(leg / 'measurements/patch_derived.json', ps.to_dict())
    report.write_json(leg / 'cov/classpath.json', {'include_glob': 'p.**'})
    for i, attempt in enumerate(attempts):
        if not attempt['compiled']:
            continue
        # Each compiled candidate covers a different seed. Neither candidate
        # covers all of R0, but their accepted union does. Method 'c' is
        # outside R0 and always covered, so |F| exceeds |R0 n F|.
        xml = '<report><package name="p"><class name="p/C" sourcefilename="C.java">'
        for j, method in enumerate(('a', 'b')):
            hit = int(i % 2 == j)
            xml += (f'<method name="{method}" desc="()V" line="{j + 1}">'
                    f'<counter type="METHOD" missed="{1-hit}" covered="{hit}"/></method>')
        xml += ('<method name="c" desc="()V" line="3">'
                '<counter type="METHOD" missed="0" covered="1"/></method>')
        xml += '</class></package></report>'
        name = attempt['attempt_label']
        (leg / 'cov' / f'{name}_compiled.xml').write_text(xml)
        (leg / 'fuzz_out' / f'{name}_compiled.txt').write_text('no exception\n')
    return leg


def test_candidate_mean_differs_from_accepted_union(tmp_path):
    leg = make_leg(tmp_path, [(True, True), (True, True), (False, False)])
    scores = report.score_leg(leg, 3)
    assert scores['candidate_rcc'] == pytest.approx(1 / 3)
    assert scores['accepted_set_rcc'] == 1
    assert scores['compile_rate'] == pytest.approx(2 / 3)
    assert [a['rcc'] for a in scores['candidates']] == [.5, .5, 0]


def test_precision_patch_set_and_recovery(tmp_path):
    """RCP, PSC, |F(H)| and RCR, per candidate and per accepted set."""
    leg = make_leg(tmp_path, [(True, True), (True, True), (False, False)])
    scores = report.score_leg(leg, 3)
    # F is {a, c} then {b, c}; the reject ran nothing.
    assert [c['f_size'] for c in scores['candidates']] == [2, 2, 0]
    assert [c['rcp'] for c in scores['candidates']] == [.5, .5, None]
    assert [c['psc'] for c in scores['candidates']] == [1, .5, 0]
    # Means over compiled candidates only; the reject leaves RCP undefined.
    assert scores['candidate_f_size'] == 2
    assert scores['candidate_rcp'] == .5
    assert scores['candidate_psc'] == pytest.approx(.75)
    assert scores['candidate_rcc_compiled'] == .5
    # The accepted union is {a, b, c}: all of R0 plus one method outside it.
    assert scores['accepted_set_f_size'] == 3
    assert scores['accepted_set_rcp'] == pytest.approx(2 / 3)
    assert scores['accepted_set_psc'] == 1
    # RCR reads no coverage: P recovers 'a' but not 'b'.
    assert scores['rcr'] == .5
    assert (scores['r0_size'], scores['p_size']) == (2, 2)


def test_jdk_callees_never_enter_the_patch_derived_denominator(tmp_path):
    leg = make_leg(tmp_path, [(True, True)])
    written = loc.MethodSet.from_dict(report.read_json(leg / 'measurements/patch_derived.json'))
    assert len(written.refs()) == 3
    # |P| is 2: java.lang.StringBuilder.append is not a project method.
    assert report.score_leg(leg, 1)['p_size'] == 2


def test_precision_is_undefined_not_zero_without_an_accepted_set(tmp_path):
    scores = report.score_leg(make_leg(tmp_path, [(True, False)]), 1)
    assert scores['accepted_set_rcc'] == 0
    assert scores['accepted_set_f_size'] == 0
    assert scores['accepted_set_psc'] == 0
    assert scores['accepted_set_rcp'] is None
    # The compiled candidate still spent its budget somewhere.
    assert scores['candidate_rcp'] == .5


def test_undefined_legs_shrink_the_bug_count_not_the_mean():
    """A bug with no defined value leaves the metric, and is counted out."""
    rows = []
    for bug, value in [('1', .4), ('2', None)]:
        for arm in ('HN', 'HR'):
            rows.append(dict(project='P', bug_id=bug, patch_id='0', repetition=1, arm=arm,
                             attempts=30, compiled=15, accepted=10, crashed_buggy=10,
                             **{m: 1.0 for m in report.METRICS}))
            rows[-1]['accepted_set_rcp'] = value
    summary = report.paired_summary(rows, [('P', '1'), ('P', '2')], bootstrap=200)
    stat = summary['estimates']['HR']['accepted_set_rcp']
    assert stat['mean'] == .4 and stat['defined_bugs'] == 1
    assert summary['estimates']['HR']['candidate_rcp']['defined_bugs'] == 2


def test_empty_harness_set_is_zero_not_missing(tmp_path):
    scores = report.score_leg(make_leg(tmp_path, [(False, False)]), 1)
    assert scores['candidate_rcc'] == scores['accepted_set_rcc'] == 0


def test_missing_compiled_coverage_is_not_zero(tmp_path):
    leg = make_leg(tmp_path, [(True, False)])
    (leg / 'cov/attempt_001_compiled.xml').unlink()
    with pytest.raises(FileNotFoundError):
        report.score_leg(leg, 1)


def test_failed_gate_is_not_low_rcc(tmp_path):
    leg = make_leg(tmp_path, [(True, True)])
    path = leg / 'measurements/root_cause.json'
    root = report.read_json(path)
    root['trigger_gate']['passed'] = False
    report.write_json(path, root)
    with pytest.raises(ValueError, match='gate'):
        report.score_leg(leg, 1)


def test_missing_candidate_is_not_a_smaller_denominator(tmp_path):
    leg = make_leg(tmp_path, [(False, False)])
    with pytest.raises(ValueError, match='budget'):
        report.read_record(leg, 30)


def test_bug_weighting_and_paired_bootstrap():
    rows = []
    for bug, count, base in [('1', 1, 0), ('2', 9, .5)]:
        for patch in range(count):
            for arm, delta in [('HN', 0), ('HR', .25)]:
                rows.append(dict(project='P', bug_id=bug, patch_id=str(patch), repetition=1,
                                 arm=arm, attempts=30, compiled=15, accepted=10, crashed_buggy=10,
                                 **{m: base + delta for m in report.METRICS}))
    summary = report.paired_summary(rows, [('P', '1'), ('P', '2')], bootstrap=200)
    assert summary['estimates']['HN']['candidate_rcc']['mean'] == .25  # not .45
    assert summary['estimates']['HR-HN']['candidate_rcc'] == {
        'mean': .25, 'ci95': [.25, .25], 'defined_bugs': 2}
    with pytest.raises(ValueError, match='unpaired'):
        report.paired_summary(rows[:-1], [('P', '1'), ('P', '2')], bootstrap=20)


def test_duplicate_candidate_ids_rejected(tmp_path):
    leg = make_leg(tmp_path, [(False, False), (False, False)])
    rec = json.loads((leg / 'result.jsonl').read_text())
    rec['candidate_attempts'][1]['attempt_label'] = 'attempt_001'
    (leg / 'result.jsonl').write_text(json.dumps(rec))
    with pytest.raises(ValueError, match='candidate IDs'):
        report.read_record(leg, 2)


def test_missing_manifest_jobs_prevents_complete_report(tmp_path):
    manifest = {'options': {'attempts': 30, 'bootstrap': 20, 'seed': 1},
                'jobs': [{'leg': 'crashing/HN/missing'}, {'leg': 'semantic/HR/missing'}]}
    assert cli.summarize(manifest, tmp_path) == 2
    assert not (tmp_path / 'comparison.md').exists()
    assert (tmp_path / 'comparison-INCOMPLETE.md').exists()
    assert report.read_json(tmp_path / 'comparison.json')['scored_legs'] == 0


def test_command_arms_share_budget_and_protocol(tmp_path):
    opts = {'model': 'test-model', 'attempts': 7, 'verify_timeout': 20}
    job = {'flag': '-c', 'patch': '/some patch.patch', 'leg': 'leg', 'arm': 'HR'}
    hr = cli.command(job, {'options': opts}, tmp_path)
    hn = cli.command({**job, 'arm': 'HN'}, {'options': opts}, tmp_path)
    assert hn == hr + ['--naive', 'neighbourhood']
    hc = cli.command({**job, 'arm': 'HN_C'}, {'options': opts}, tmp_path)
    assert hc == hr + ['--naive', 'function']
    assert hr[hr.index('--candidate_budget') + 1] == '7'
    assert '/some patch.patch' in hr  # passed as one argv element


@pytest.mark.parametrize('arms', [('HN', 'HR'), tuple(cli.ARMS)])
def test_offline_report_complete_then_missing_artifact_invalidates_it(tmp_path, arms):
    jobs = []
    populations = {}
    for kind in cli.KINDS:
        populations[kind] = {'bugs': [('P', '1'), ('P', '2')]}
        for bug in ('1', '2'):
            for arm in arms:
                relative = f'{kind}/{arm}/P-{bug}'
                scratch = tmp_path / relative
                leg = make_leg(scratch, [(True, True), (True, True)])
                rec = json.loads((leg / 'result.jsonl').read_text())
                rec.update(project='P', bug_id=bug, bug_kind=kind,
                           naive=arm != 'HR', naive_level=cli.ARMS[arm],
                           naive_level_c_semantic=arm == 'HN_C' and kind == 'semantic')
                (leg / 'result.jsonl').write_text(json.dumps(rec) + '\n')
                jobs.append(dict(leg=relative + '/leg', project='P', bug_id=bug,
                                 kind=kind, arm=arm, patch_id=f'patch-{bug}', repetition=1))
    manifest = {'options': {'attempts': 2, 'bootstrap': 50, 'seed': 1},
                'jobs': jobs, 'populations': populations, 'arms': list(arms)}
    assert cli.summarize(manifest, tmp_path) == 0
    full = report.read_json(tmp_path / 'comparison.json')
    assert full['complete']
    assert set(full['groups']['semantic']['totals']) == set(arms)
    if 'HN_C' in arms:
        assert 'HR-HN_C' in full['groups']['semantic']['estimates']
    assert set(full['groups']) == {'crashing', 'semantic'}
    assert full['groups']['semantic']['estimates']['HR']['accepted_set_rcc']['mean'] == 1
    # An existing summary cannot conceal a later missing raw coverage artifact.
    (tmp_path / jobs[-1]['leg'] / 'cov/attempt_001_compiled.xml').unlink()
    assert cli.summarize(manifest, tmp_path) == 2
    partial = report.read_json(tmp_path / 'comparison.json')
    assert not partial['complete']
    assert set(partial['groups']) == {'crashing'}
    assert not (tmp_path / 'comparison.md').exists()


def test_fixed_budget_does_not_change_default_free_invalid_retry(monkeypatch, tmp_path):
    import java.harness.campaign as campaign
    monkeypatch.setattr(campaign, 'record_event', lambda *a, **kw: None)
    responses = iter(['invalid', SOURCE])
    builder = SimpleNamespace(
        looks_like_harness=lambda raw: 'invalid' if raw == 'invalid' else None,
        extract_source=lambda raw: raw,
        build=lambda source, buggy_dir, output_subdir: BuildResult(
            str(tmp_path / 'FuzzHarness.java'), 'FuzzHarness', '', True,
            0, '', '', attempt_label=output_subdir))
    runner = HarnessCampaign(SimpleNamespace(generate=lambda msgs: next(responses)), builder,
                             target_successes=1, max_attempts=1, require_trigger=False)
    monkeypatch.setattr(runner, '_print_success', lambda *a: None)
    result = runner.run([{'role': 'user', 'content': 'generate'}], str(tmp_path))
    assert result.attempts == 1
    assert len(result.raw_responses) == 2
    assert result.achieved_successes == 1


def test_compiled_crash_can_be_rejected_by_later_gate(tmp_path):
    leg = make_leg(tmp_path, [(True, False)])
    rec = json.loads((leg / 'result.jsonl').read_text())
    rec['candidate_attempts'][0]['crashed_buggy'] = True
    (leg / 'result.jsonl').write_text(json.dumps(rec))
    scores = report.score_leg(leg, 1)
    assert scores['buggy_crash_rate'] == 1
    assert scores['acceptance_rate'] == scores['accepted_set_rcc'] == 0
    assert scores['candidate_rcc'] == .5


def test_budget_cli_and_automatic_output(monkeypatch, tmp_path):
    monkeypatch.setattr(cli, 'REPO', tmp_path)
    assert cli.parser().parse_args([]).attempts == 30
    for flag in ('-N', '--attempts'):
        args = cli.parser().parse_args([flag, '7'])
        assert args.attempts == 7
        assert not hasattr(args, 'out')
        assert cli.output_dir(args.attempts) == tmp_path / 'results/heldout_rcc_7'
    assert cli.main(['-N', '0']) == 2
    assert not (tmp_path / 'results').exists()
    for removed in ('--dry-run', '--resume', '--report-only', '--out'):
        with pytest.raises(SystemExit):
            cli.parser().parse_args([removed])


def test_three_arm_bootstrap_keeps_level_c_paired():
    rows = []
    for bug, base in [('1', 0), ('2', .5)]:
        for arm, value in [('HN', base), ('HN_C', base / 2), ('HR', base + .25)]:
            rows.append(dict(project='P', bug_id=bug, patch_id=bug, repetition=1,
                             arm=arm, attempts=30, compiled=15, accepted=10, crashed_buggy=10,
                             **{m: value for m in report.METRICS}))
    summary = report.paired_summary(rows, [('P', '1'), ('P', '2')],
                                    bootstrap=200, arms=tuple(cli.ARMS))
    assert summary['n_legs_per_arm'] == 2
    assert summary['estimates']['HR-HN']['candidate_rcc'] == {
        'mean': .25, 'ci95': [.25, .25], 'defined_bugs': 2}
    assert summary['estimates']['HR-HN_C']['candidate_rcc'] == {
        'mean': .375, 'ci95': [.25, .5], 'defined_bugs': 2}
    missing_c = [r for r in rows if r['arm'] != 'HN_C']
    with pytest.raises(ValueError, match='unpaired'):
        report.paired_summary(missing_c, [('P', '1'), ('P', '2')], arms=tuple(cli.ARMS))


@pytest.mark.parametrize('function_only', [False, True])
def test_level_c_repair_turns_do_not_reintroduce_withheld_context(monkeypatch, tmp_path, function_only):
    import java.harness.campaign as campaign
    monkeypatch.setattr(campaign, 'record_event', lambda *a, **kw: None)
    monkeypatch.setattr(campaign, 'lifted_observed_mismatch', lambda *a: 'observed')
    prompts = []

    def generate(messages):
        prompts.append(json.dumps(messages))
        return SOURCE

    def verify(build):
        return SimpleNamespace(crashed=build.attempt_label != 'attempt_001',
                               signature='Crash', reached_functions=['NEIGHBOURHOOD_SECRET'],
                               stdout='', stderr='', timed_out=False)

    builder = SimpleNamespace(
        looks_like_harness=lambda raw: None, extract_source=lambda raw: raw,
        build=lambda source, buggy_dir, output_subdir: BuildResult(
            str(tmp_path / 'FuzzHarness.java'), 'FuzzHarness', '', True,
            0, '', '', attempt_label=output_subdir))
    runner = HarnessCampaign(SimpleNamespace(generate=generate), builder,
                             target_successes=3, max_attempts=3,
                             verifier=SimpleNamespace(verify=verify),
                             trigger_wrong_values=['WITHHELD_TEST_VALUE'],
                             count_invalid_attempts=True, function_only=function_only)
    monkeypatch.setattr(runner, '_print_no_trigger', lambda *a: None)
    result = runner.run([{'role': 'user', 'content': 'function source only'}],
                        str(tmp_path), patch_text='WITHHELD_PATCH')
    assert result.attempts == 3
    assert not any(a['accepted'] for a in result.candidate_attempts)  # gate unchanged
    for secret in ('WITHHELD_PATCH', 'NEIGHBOURHOOD_SECRET', 'WITHHELD_TEST_VALUE'):
        assert (secret in prompts[-1]) is (not function_only)


def test_validate_level_c_identity_and_semantic_marker(tmp_path):
    leg = make_leg(tmp_path, [(False, False)])
    rec = json.loads((leg / 'result.jsonl').read_text())
    rec.update(project='P', bug_id='1', bug_kind='semantic', naive=True,
               naive_level='function', naive_level_c_semantic=True)
    path = leg / 'result.jsonl'
    path.write_text(json.dumps(rec))
    job = dict(project='P', bug_id='1', kind='semantic', arm='HN_C')
    assert cli.validate_record(leg, job, 1)['naive_level'] == 'function'
    with pytest.raises(ValueError, match='ablation'):
        cli.validate_record(leg, {**job, 'arm': 'HN'}, 1)
    rec['naive_level_c_semantic'] = False
    path.write_text(json.dumps(rec))
    with pytest.raises(ValueError, match='marker'):
        cli.validate_record(leg, job, 1)


def test_single_command_runs_then_reports(monkeypatch, tmp_path):
    monkeypatch.setattr(cli, 'REPO', tmp_path)
    calls = []
    manifest = {'options': {'attempts': 7}, 'populations': {}, 'jobs': []}

    def prepare(args):
        calls.append('prepare')
        assert args.attempts == 7
        assert args.out == tmp_path / 'results/heldout_rcc_7'
        return manifest

    monkeypatch.setattr(cli, 'prepare_run', prepare)
    monkeypatch.setattr(cli, 'run_jobs', lambda *a: calls.append('run'))
    monkeypatch.setattr(cli, 'summarize', lambda *a: calls.append('report') or 0)
    assert cli.main(['-N', '7']) == 0
    assert calls == ['prepare', 'run', 'report']


def test_existing_output_is_preserved(tmp_path):
    out = tmp_path / 'results/heldout_rcc_30'
    out.mkdir(parents=True)
    previous = out / 'comparison.md'
    previous.write_text('previous results')
    with pytest.raises(ValueError, match='not empty'):
        cli.prepare_run(SimpleNamespace(out=out))
    assert previous.read_text() == 'previous results'


def test_verbose_logging_streams_both_outputs_and_preserves_failure(tmp_path, capsys):
    import os
    import sys

    log = tmp_path / 'run.log'
    cmd = [sys.executable, '-u', '-c',
           'import sys; print("generation output"); '
           'print("diagnostic output", file=sys.stderr); sys.exit(7)']
    result = cli.run_logged(cmd, log, dict(os.environ))
    console = capsys.readouterr().out
    assert result.returncode == 7
    for text in ('generation output', 'diagnostic output'):
        assert text in console
        assert text in log.read_text()
    assert 'exit=7' in console
    assert 'elapsed=' in console
    assert str(log) in console


def existing_run(tmp_path):
    source = tmp_path / 'source'
    jobs = []
    for bug in ('1', '2', '3'):
        for arm in cli.ARMS:
            leg = make_leg(source / 'crashing' / arm / bug, [(True, True)])
            rec = json.loads((leg / 'result.jsonl').read_text())
            rec.update(project='P', bug_id=bug, bug_kind='crashing',
                       naive=arm != 'HR', naive_level=cli.ARMS[arm])
            (leg / 'result.jsonl').write_text(json.dumps(rec) + '\n')
            report.write_json(leg / 'execution.json',
                              {'returncode': 1 if bug == '1' and arm == 'HR' else 0})
            jobs.append(dict(leg=str(leg.relative_to(source)), project='P', bug_id=bug,
                             kind='crashing', arm=arm, patch_id=bug, repetition=1))
    report.write_json(source / 'manifest.json', {
        'options': dict(attempts=1, repetitions=1, checkout_root=str(source / 'checkouts'),
                        d4j_home='/d4j', model='original-model', verify_timeout=20),
        'arms': list(cli.ARMS), 'jobs': jobs,
        'populations': {'crashing': {'patches': 3, 'bugs': [('P', b) for b in ('1', '2', '3')]}}})
    (source / 'provenance/crashing').mkdir(parents=True)
    (source / 'provenance/crashing/queue.txt').write_text('original queue\n')
    return source


def test_measure_existing_selects_complete_groups_and_reports_subset(tmp_path):
    source = existing_run(tmp_path)
    args = cli.parser().parse_args(['-N', '1', '--kind', 'crashing', '--max-patches', '2',
                                  '--measure-existing', str(source), '--bootstrap', '20'])
    args.out = tmp_path / 'measured'
    before = (source / 'manifest.json').read_bytes()
    manifest = cli.prepare_run(args)
    assert {j['patch_id'] for j in manifest['jobs']} == {'2', '3'}
    assert len(manifest['jobs']) == 6
    assert manifest['excluded_patches'][0]['patch_id'] == '1'
    assert manifest['options']['model'] == 'original-model'
    assert manifest['options']['checkout_root'] == str(source / 'checkouts')
    assert cli.summarize(manifest, args.out) == 0
    result = report.read_json(args.out / 'comparison.json')
    assert set(result['groups']) == {'crashing'}
    assert result['groups']['crashing']['n_bugs'] == 2
    assert '2/3 certified patches' in (args.out / 'comparison.md').read_text()
    assert '**Subset**' in (args.out / 'comparison.md').read_text()
    assert (source / 'manifest.json').read_bytes() == before
    assert not list(source.rglob('candidate_metrics.json'))
    # A failed new measurement cannot be masked by older valid source coverage.
    report.write_json(args.out / 'execution_errors.json',
                      [{'leg': manifest['jobs'][0]['leg'], 'error': 'measurement failed'}])
    assert cli.summarize(manifest, args.out) == 2
    assert not (args.out / 'comparison.md').exists()


def test_existing_selection_requires_budget_and_enough_complete_patches(tmp_path):
    source = existing_run(tmp_path)
    args = cli.parser().parse_args(['--kind', 'crashing', '--max-patches', '2',
                                  '--measure-existing', str(source)])
    with pytest.raises(ValueError, match='must match'):
        cli.select_existing(args)
    args.attempts = 1
    args.max_patches = 3
    with pytest.raises(ValueError, match='only 2 complete patches'):
        cli.select_existing(args)
    args.max_patches = None
    assert len(cli.select_existing(args)[2]) == 6


def test_semantic_cap_selects_only_requested_kind(monkeypatch, tmp_path):
    repo = tmp_path / 'repo'
    (repo / 'suites/splits').mkdir(parents=True)
    (repo / 'suites/labels').mkdir(parents=True)
    (repo / 'suites/splits/semantic_split.jsonl').write_text('\n'.join(
        json.dumps(dict(project='P', bug_id=str(b), side='holdout')) for b in (1, 2)))
    for name in ('verified_correct', 'verified_incorrect', 'excluded'):
        (repo / f'suites/labels/{name}.jsonl').write_text('')
    paths = [repo / f'patch{i}-P-{i}-tool.patch' for i in (1, 2)]
    for path in paths:
        path.write_text('patch contents')

    def run(cmd, **kwargs):
        if cmd[0] == 'git':
            return SimpleNamespace(stdout='test-sha')
        assert cmd[cmd.index('--kind') + 1] == 'semantic'
        Path(cmd[cmd.index('--out') + 1]).write_text(''.join(f'-c {p}\n' for p in paths))

    monkeypatch.setattr(cli, 'REPO', repo)
    monkeypatch.setattr(cli.subprocess, 'run', run)
    args = cli.parser().parse_args(['--kind', 'semantic', '--max-patches', '1'])
    args.out = cli.output_dir(args.attempts, args.kind, args.max_patches)
    manifest = cli.prepare_run(args)
    assert args.out.name == 'heldout_rcc_30_semantic_patches1'
    assert set(manifest['populations']) == {'semantic'}
    assert manifest['populations']['semantic']['patches'] == 1
    assert manifest['populations']['semantic']['full_patches'] == 2
    assert {j['arm'] for j in manifest['jobs']} == set(cli.ARMS)
    assert {j['bug_id'] for j in manifest['jobs']} == {'1'}
    assert cli.output_dir(30, 'crashing', 10, True).name == 'heldout_rcc_30_crashing_patches10_measured'
    assert cli.main(['--max-patches', '0']) == 2


def test_existing_mode_launches_only_measurement_workers(monkeypatch, tmp_path):
    source = existing_run(tmp_path)
    args = cli.parser().parse_args(['-N', '1', '--kind', 'crashing', '--max-patches', '2',
                                  '--measure-existing', str(source)])
    args.out = tmp_path / 'measured'
    manifest = cli.prepare_run(args)
    d4j = tmp_path / 'd4j/framework/bin/defects4j'
    d4j.parent.mkdir(parents=True)
    d4j.touch()
    manifest['options']['d4j_home'] = str(tmp_path / 'd4j')
    monkeypatch.setattr(cli.shutil, 'which', lambda name: '/bin/java')
    commands = []

    def logged(cmd, log, env):
        commands.append(cmd)
        assert '--measure-job' in cmd
        assert cmd[cmd.index('--manifest') + 1] == str(args.out / 'manifest.json')
        assert env['D4J_CHECKOUT_ROOT'].startswith(str(source / 'checkouts'))
        return SimpleNamespace(returncode=0)

    monkeypatch.setattr(cli, 'run_logged', logged)
    monkeypatch.setattr(cli, 'command', lambda *a: pytest.fail('must not generate'))
    cli.run_jobs(manifest, args.out)
    assert len(commands) == 6


def test_existing_worker_measures_source_and_writes_status_to_new_output(monkeypatch, tmp_path):
    import java.measurements.cli as measurements

    source = existing_run(tmp_path)
    args = cli.parser().parse_args(['-N', '1', '--kind', 'crashing', '--max-patches', '2',
                                  '--measure-existing', str(source)])
    args.out = tmp_path / 'measured'
    manifest = cli.prepare_run(args)
    job = manifest['jobs'][0]
    calls = []

    def measure(leg, **kwargs):
        calls.append((leg, kwargs))
        return {'errors': {}}

    monkeypatch.setattr(measurements, 'measure_leg', measure)
    assert cli.main(['--manifest', str(args.out / 'manifest.json'), '--measure-job', '0']) == 0
    assert calls[0][0] == str(source / job['leg'])
    assert calls[0][1]['coverage'] is True
    assert (args.out / job['leg'] / 'measurement_status.json').is_file()
    assert not (source / job['leg'] / 'measurement_status.json').exists()


def test_replay_plan_only_missing_rejected_candidates(tmp_path):
    from java.measurements.heldout_comparison import replay
    source = existing_run(tmp_path)
    manifest = report.read_json(source / 'manifest.json')
    leg = source / manifest['jobs'][0]['leg']
    (leg / 'cov/attempt_001_compiled.xml').unlink()
    with pytest.raises(ValueError, match='accepted candidate'):
        replay.plan(manifest, source)
    rec = json.loads((leg / 'result.jsonl').read_text())
    rec['candidate_attempts'][0].update(accepted=False, crashed_buggy=False)
    (leg / 'result.jsonl').write_text(json.dumps(rec))
    (leg / 'harness_src').mkdir()
    (leg / 'harness_src/attempt_001.java').write_text(SOURCE)
    (leg / 'fuzz_out/attempt_001_compiled.txt').write_text('INFO: Seed: 12345\n')
    tasks = replay.plan(manifest, source)
    assert len(tasks) == 1
    assert tasks[0]['seed'] == 12345
    (leg / 'cov/attempt_001_compiled.exec').touch()
    with pytest.raises(ValueError, match='convert it instead'):
        replay.plan(manifest, source)


def test_replay_coverage_keeps_original_outcomes_and_requires_provenance(tmp_path):
    import shutil
    leg = make_leg(tmp_path, [(True, False), (True, True)])
    (leg / 'harness_src').mkdir()
    src = leg / 'harness_src/attempt_001.java'
    src.write_text(SOURCE)
    replay = tmp_path / 'replay/attempt_001'
    replay.mkdir(parents=True)
    shutil.move(leg / 'cov/attempt_001_compiled.xml', replay / 'coverage.xml')
    (replay / 'fuzzer.log').write_text('replayed output')
    with pytest.raises(FileNotFoundError):
        report.score_leg(leg, 2, replay_dir=replay.parent)
    status = {'attempt_label': 'attempt_001', 'source_sha256': cli.sha256(src)}
    report.write_json(replay / 'status.json', status)
    scores = report.score_leg(leg, 2, replay_dir=replay.parent)
    assert scores['candidate_rcc'] == .5
    assert scores['accepted_set_rcc'] == .5
    assert scores['accepted'] == 1
    assert scores['replayed_candidates'] == 1
    assert scores['candidates'][0]['coverage_origin'] == 'replay'
    assert scores['candidates'][1]['coverage_origin'] == 'original'
    src.write_text('changed candidate')
    with pytest.raises(ValueError, match='does not match'):
        report.score_leg(leg, 2, replay_dir=replay.parent)
