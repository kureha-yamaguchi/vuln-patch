"""Run the frozen heldout comparison and write its RCC report."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import shlex
import subprocess
import sys
import time
from datetime import datetime, timezone

from . import ARMS, report

REPO = Path(__file__).resolve().parents[4]
KINDS = ('crashing', 'semantic')
COMMON = ['--verify_relations', '--synthesize_relations',
          '--replay_relations_on_patched', '--rule_compile_repair', '--reference_impl']


def sha256(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def code_hash():
    digest = hashlib.sha256()
    for path in sorted((REPO / 'src').rglob('*.py')):
        digest.update(str(path.relative_to(REPO)).encode())
        digest.update(path.read_bytes())
    return digest.hexdigest()


def jsonl(path):
    return [json.loads(line) for line in Path(path).read_text().splitlines() if line.strip()]


def select_existing(args):
    """Select complete patch groups in archive order, before looking at RCC."""
    source = args.measure_existing.resolve()
    original = report.read_json(source / 'manifest.json')
    if original['options']['attempts'] != args.attempts:
        raise ValueError('-N must match the existing run; candidate budgets are not truncated')
    if set(original['arms']) != set(ARMS):
        raise ValueError('existing run must contain all three comparison arms')
    jobs, populations, excluded = [], {}, []
    for kind in selected_kinds(args.kind):
        groups = {}
        for job in original['jobs']:
            if job['kind'] == kind:
                groups.setdefault(job['patch_id'], []).append(job)
        expected = {(arm, rep) for arm in ARMS
                    for rep in range(1, original['options']['repetitions'] + 1)}
        complete = []
        for patch, group in groups.items():
            try:
                if ({(j['arm'], j['repetition']) for j in group} != expected
                        or len(group) != len(expected)):
                    raise ValueError('missing or duplicate arm/repetition')
                for job in group:
                    leg = source / job['leg']
                    if report.read_json(leg / 'execution.json')['returncode'] != 0:
                        raise ValueError(f"{job['arm']} generation failed")
                    validate_record(leg, job, args.attempts)
                complete.append(group)
            except (ValueError, OSError, KeyError) as exc:
                excluded.append({'kind': kind, 'patch_id': patch, 'reason': str(exc)})
        wanted = min(args.max_patches, len(groups)) if args.max_patches else len(complete)
        if not wanted or len(complete) < wanted:
            raise ValueError(f'{kind}: only {len(complete)} complete patches; need {wanted or 1}')
        chosen = [j for group in complete[:wanted] for j in group]
        jobs.extend({**j, 'source_leg': str(source / j['leg'])} for j in chosen)
        pop = original['populations'][kind]
        populations[kind] = {
            'bugs': sorted({(j['project'], j['bug_id']) for j in chosen}),
            'patches': wanted, 'full_bugs': pop.get('full_bugs', pop['bugs']),
            'full_patches': pop.get('full_patches', pop['patches']),
            'selection': 'first complete patch groups in source manifest order'}
    return source, original, jobs, populations, excluded


def selected_kinds(kind):
    return KINDS if kind == 'both' else (kind,)


def artifact_dir(job, out):
    return Path(job['source_leg']) if 'source_leg' in job else out / job['leg']


def prepare_run(args):
    out = args.out.resolve()
    if out.exists() and any(out.iterdir()):
        raise ValueError(f'{out} is not empty; move the previous run before starting another')
    out.mkdir(parents=True, exist_ok=True)
    opts = {k: v for k, v in vars(args).items()
            if k not in ('measure_job', 'manifest')}
    opts = {k: str(v.resolve()) if isinstance(v, Path) else v for k, v in opts.items()}
    opts['checkout_root'] = str((args.checkout_root or out / 'checkouts').resolve())
    manifest = {'schema': 2, 'arms': list(ARMS), 'created_utc': datetime.now(timezone.utc).isoformat(),
                'code_hash': code_hash(), 'options': opts, 'populations': {}, 'jobs': []}
    git = subprocess.run(['git', 'rev-parse', 'HEAD'], cwd=REPO,
                         capture_output=True, text=True)
    manifest['git_sha'] = git.stdout.strip() or 'unknown'
    if args.measure_existing:
        source, original, jobs, populations, excluded = select_existing(args)
        # Reuse the builds and generation settings that produced these artifacts.
        for key in ('checkout_root', 'd4j_home', 'model', 'verify_timeout', 'repetitions'):
            opts[key] = original['options'][key]
        manifest.update(source_run=str(source), source_code_hash=original.get('code_hash'),
                        source_manifest_sha256=sha256(source / 'manifest.json'),
                        jobs=jobs, populations=populations, excluded_patches=excluded)
        for kind in populations:
            shutil.copytree(source / 'provenance' / kind, out / 'provenance' / kind)
        report.write_json(out / 'manifest.json', manifest)
        return manifest
    for kind in selected_kinds(args.kind):
        prov = out / 'provenance' / kind
        prov.mkdir(parents=True)
        split = REPO / 'suites/splits' / f'{kind}_split.jsonl'
        labels = REPO / 'suites/labels' / ('crashing' if kind == 'crashing' else '')
        inputs = [split] + [labels / f'{name}.jsonl' for name in
                            ('verified_correct', 'verified_incorrect', 'excluded')]
        hashes = {}
        for source in inputs:
            shutil.copyfile(source, prov / source.name)
            hashes[str(source.relative_to(REPO))] = sha256(source)
        queue = prov / 'queue.txt'
        subprocess.run([sys.executable, '-m', 'java.dataset.build_split_queue',
                        '--kind', kind, '--side', 'holdout', '--patches', opts['patches'],
                        '--out', str(queue)], cwd=REPO, check=True)
        expected = {(r['project'], str(r['bug_id'])) for r in jsonl(split)
                    if r['side'] == 'holdout'}
        actual, patches = set(), []
        for index, line in enumerate(queue.read_text().splitlines(), 1):
            flag, path = line.split(' ', 1)
            path = Path(path).resolve()
            match = re.fullmatch(r'patch\d+-([^-]+)-(\d+)-.+\.patch', path.name)
            if not match:
                raise ValueError(f'unrecognised patch filename: {path}')
            project, bug = match.groups()
            actual.add((project, bug))
            patch_id = f'{index:03d}_{path.stem}_{flag[1:]}'
            patch = {'patch_id': patch_id, 'project': project, 'bug_id': bug,
                     'patch': str(path), 'patch_sha256': sha256(path), 'flag': flag}
            patches.append(patch)
        if actual != expected:
            raise ValueError(f'{kind}: frozen bug population mismatch; '
                             f'missing={sorted(expected - actual)}, extra={sorted(actual - expected)}')
        chosen = patches[:args.max_patches] if args.max_patches else patches
        for index, patch in enumerate(chosen):
            for repetition in range(1, args.repetitions + 1):
                arms = list(ARMS)
                offset = (index + repetition - 1) % len(arms)
                for arm in arms[offset:] + arms[:offset]:
                    leg = f"{kind}/{arm}/{patch['patch_id']}_rep{repetition:03d}"
                    manifest['jobs'].append({**patch, 'kind': kind, 'arm': arm,
                                             'repetition': repetition, 'leg': leg})
        manifest['populations'][kind] = {
            'bugs': sorted({(p['project'], p['bug_id']) for p in chosen}),
            'patches': len(chosen), 'full_bugs': sorted(expected),
            'full_patches': len(patches), 'input_sha256': hashes,
            'selection': 'first patches in certified queue order'}
    report.write_json(out / 'manifest.json', manifest)
    return manifest


def command(job, manifest, out):
    opts = manifest['options']
    cmd = [sys.executable, str(REPO / 'src/java/run.py'), job['flag'],
           '--patch_file', job['patch'], '--model', opts['model'],
           '--candidate_budget', str(opts['attempts']), '--coverage',
           '--verify_timeout', str(opts['verify_timeout']),
           '--fuzz_timeout', str(opts['verify_timeout']),
           '--results_json', str(out / job['leg'] / 'result.jsonl'), *COMMON]
    level = ARMS[job['arm']]
    if level is not None:
        cmd += ['--naive', level]
    return cmd


def environment(job, manifest):
    opts = manifest['options']
    env = dict(os.environ)
    env['D4J_HOME'] = opts['d4j_home']
    env['D4J_CHECKOUT_ROOT'] = str(Path(opts['checkout_root']) / job['leg'])
    env['PATH'] = str(Path(opts['d4j_home']) / 'framework/bin') + os.pathsep + env.get('PATH', '')
    env['PYTHONPATH'] = str(REPO / 'src') + os.pathsep + env.get('PYTHONPATH', '')
    env['PYTHONUNBUFFERED'] = '1'
    env['GITSHA'] = manifest['git_sha']
    return env


def validate_record(leg, job, budget):
    result = report.read_record(leg, budget)
    if (result.get('project'), str(result.get('bug_id')), result.get('bug_kind')) != (
            job['project'], job['bug_id'], job['kind']):
        raise ValueError('result identity/kind does not match frozen job')
    level = ARMS[job['arm']]
    if level is not None:
        if not result.get('naive') or result.get('naive_level') != level:
            raise ValueError(f"{job['arm']} result is not the {level} ablation")
        if level == 'function' and job['kind'] == 'semantic':
            if not result.get('naive_level_c_semantic'):
                raise ValueError('semantic level-C result lacks its oracle limitation marker')
    elif result.get('naive') or result.get('naive_level'):
        raise ValueError('HR result unexpectedly used naive prompts')
    return result


def run_logged(cmd, log_path, env):
    """Stream merged child stdout/stderr to the terminal and its log file."""
    started = time.monotonic()
    stamp = datetime.now(timezone.utc).isoformat(timespec='seconds')
    print(f'[{stamp}] Running: {shlex.join(cmd)}', flush=True)
    print(f'Log: {log_path}', flush=True)
    with Path(log_path).open('w', encoding='utf-8', buffering=1) as log:
        with subprocess.Popen(cmd, cwd=REPO / 'src', env=env,
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              text=True, encoding='utf-8', errors='replace',
                              bufsize=1) as proc:
            try:
                for line in proc.stdout:
                    log.write(line)
                    sys.stdout.write(line)
                    sys.stdout.flush()
                returncode = proc.wait()
            except BaseException:
                proc.terminate()
                try:
                    proc.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    proc.kill()
                    proc.wait()
                raise
    print(f'Finished: exit={returncode}, elapsed={time.monotonic() - started:.1f}s, '
          f'log={log_path}', flush=True)
    return subprocess.CompletedProcess(cmd, returncode)


def run_jobs(manifest, out):
    opts = manifest['options']
    d4j = Path(opts['d4j_home']) / 'framework/bin/defects4j'
    if not d4j.is_file() or not shutil.which('java'):
        raise ValueError('Defects4J executable or Java missing; check --d4j-home and PATH')
    # Fail before spending tokens if the source parser or call-graph frontend
    # used by the existing pipeline is unavailable.
    import javalang  # noqa: F401
    if not manifest.get('source_run'):
        from fuzz_introspector import commands  # noqa: F401
    failures = []
    generation_jobs = [] if manifest.get('source_run') else manifest['jobs']
    for index, job in enumerate(generation_jobs, 1):
        leg = out / job['leg']
        leg.mkdir(parents=True, exist_ok=True)
        print(f"[{index}/{len(manifest['jobs'])}] {job['leg']}", flush=True)
        try:
            cmd = command(job, manifest, out)
            report.write_json(leg / 'invocation.json', {'argv': cmd})
            proc = run_logged(cmd, leg / 'run.log', environment(job, manifest))
            report.write_json(leg / 'execution.json', {'returncode': proc.returncode})
            if proc.returncode:
                raise ValueError(f'pipeline exited {proc.returncode}; see run.log')
            validate_record(leg, job, opts['attempts'])
        except Exception as exc:
            failures.append({'leg': job['leg'], 'error': str(exc)})
            print(f'  FAILED: {exc}', flush=True)
    # Quarantined measurements only after generation for ALL arms finishes.
    for index, job in enumerate(manifest['jobs'], 1):
        leg = out / job['leg']
        leg.mkdir(parents=True, exist_ok=True)
        if any(e['leg'] == job['leg'] for e in failures):
            continue
        print(f"[measure {index}/{len(manifest['jobs'])}] {job['leg']}", flush=True)
        try:
            # Keep per-job Defects4J configuration process-local, as in the
            # existing suite runner. The measurement command never calls an LLM.
            env = environment(job, manifest)
            proc = run_logged(
                [sys.executable, '-m', 'java.measurements.heldout_comparison.cli',
                 '--manifest', str(out / 'manifest.json'), '--measure-job', str(index - 1)],
                leg / 'measurement.log', env)
            if proc.returncode:
                raise ValueError('measurement failed; see measurement.log and measurements/errors.json')
        except Exception as exc:
            failures.append({'leg': job['leg'], 'error': str(exc)})
            print(f'  FAILED: {exc}', flush=True)
    report.write_json(out / 'execution_errors.json', failures)


def measure_job(manifest, job, out):
    """Run in a child with this job's Defects4J environment; no LLM calls."""
    from java.measurements.cli import measure_leg

    opts = manifest['options']
    leg = artifact_dir(job, out)
    rec = validate_record(leg, job, opts['attempts'])
    status = measure_leg(
        str(leg), checkout_root=str(Path(opts['checkout_root']) / job['leg']),
        d4j_home=opts['d4j_home'], trigger_gate=True,
        coverage=any(a['compiled'] for a in rec['candidate_attempts']))
    report.write_json(out / job['leg'] / 'measurement_status.json', status)
    required = {'checkout', 'root_cause', 'trigger_gate', 'coverage'}
    errors = {k: v for k, v in status['errors'].items() if k in required}
    if errors:
        raise ValueError(f'measurement stages failed: {errors}')
    return 0


def summarize(manifest, out):
    opts = manifest['options']
    rows, errors = [], []
    execution_errors = out / 'execution_errors.json'
    failed = {e['leg']: e['error'] for e in report.read_json(execution_errors)} if execution_errors.exists() else {}
    for job in manifest['jobs']:
        leg = artifact_dir(job, out)
        try:
            if job['leg'] in failed:
                raise ValueError(failed[job['leg']])
            validate_record(leg, job, opts['attempts'])
            scores = report.score_leg(leg, opts['attempts'], replay_dir=job.get('replay_dir'))
            report.write_json(out / job['leg'] / 'candidate_metrics.json', scores)
            rows.append({**job, **scores})
        except Exception as exc:
            errors.append({'leg': job['leg'], 'error': f'{type(exc).__name__}: {exc}'})
    result = {'complete': not errors, 'errors': errors, 'groups': {},
              'bootstrap': opts['bootstrap'], 'seed': opts['seed'],
              'arms': manifest.get('arms', ['HN', 'HR']),
              'populations': manifest.get('populations', {}),
              'excluded_patches': manifest.get('excluded_patches', []),
              'source_run': manifest.get('source_run'),
              'replay': manifest.get('replay'),
              'expected_legs': len(manifest['jobs']), 'scored_legs': len(rows)}
    # A complete kind can be shown even if the other failed, but never show
    # a partial kind under the name of the full frozen population.
    for kind in manifest.get('populations', {}):
        if any(e['leg'].startswith(kind + '/') for e in errors):
            continue
        result['groups'][kind] = report.paired_summary(
            [r for r in rows if r['kind'] == kind], manifest['populations'][kind]['bugs'],
            opts['bootstrap'], opts['seed'], arms=result['arms'])
    with (out / 'candidate_metrics.jsonl').open('w') as handle:
        for row in rows:
            handle.write(json.dumps(row, sort_keys=True, allow_nan=False) + '\n')
    report.write_json(out / 'comparison.json', result)
    filename = 'comparison.md' if result['complete'] else 'comparison-INCOMPLETE.md'
    other = out / ('comparison-INCOMPLETE.md' if result['complete'] else 'comparison.md')
    other.unlink(missing_ok=True)
    (out / filename).write_text(report.render(result))
    print(f"Report: {out / filename} ({len(rows)}/{len(manifest['jobs'])} legs)")
    return 0 if result['complete'] else 2


def output_dir(attempts, kind='both', max_patches=None, measure_existing=False):
    name = f'heldout_rcc_{attempts}'
    if kind != 'both' or max_patches is not None or measure_existing:
        name += f'_{kind}'
    if max_patches is not None:
        name += f'_patches{max_patches}'
    if measure_existing:
        name += '_measured'
    return REPO / 'results' / name


def parser():
    p = argparse.ArgumentParser(description=__doc__,
                                epilog='Output: results/heldout_rcc_N, with kind/cap/measurement suffixes for subsets.')
    p.add_argument('--kind', choices=('both', *KINDS), default='both')
    p.add_argument('--max-patches', type=int, default=None, metavar='K',
                   help='maximum patches per selected kind, keeping all arms')
    p.add_argument('--measure-existing', type=Path, metavar='RUN_DIR',
                   help='measure completed patch groups in an existing run; no generation')
    p.add_argument('--manifest', type=Path, help=argparse.SUPPRESS)
    p.add_argument('--measure-job', type=int, default=None, help=argparse.SUPPRESS)
    p.add_argument('--report-only', type=Path, metavar='RUN_DIR',
                   help='rescore an existing run from its own artifacts and rewrite its '
                        'report; no generation, no measurement, no model calls')
    p.add_argument('-N', '--attempts', type=int, default=30, metavar='N', help='candidate responses per patch/arm (default: 30)')
    p.add_argument('--repetitions', type=int, default=1, help='independent campaigns per patch/arm (default: 1)')
    p.add_argument('--model', default='gpt-5.4')
    p.add_argument('--verify-timeout', type=int, default=20, help='same buggy acceptance budget in seconds for all arms')
    p.add_argument('--patches', type=Path, default=REPO / 'drr/Patches')
    p.add_argument('--d4j-home', type=Path, default=Path(os.getenv('D4J_HOME', str(REPO / 'defects4j'))))
    p.add_argument('--checkout-root', type=Path, default=None, help='default: OUT/checkouts; retained for measurement')
    p.add_argument('--bootstrap', type=int, default=10000)
    p.add_argument('--seed', type=int, default=20260916, help='bootstrap seed, not an LLM determinism guarantee')
    return p


def main(argv=None):
    args = parser().parse_args(argv)
    try:
        if min(args.attempts, args.repetitions, args.verify_timeout, args.bootstrap) < 1:
            raise ValueError('attempts, repetitions, timeout and bootstrap must be positive')
        if args.max_patches is not None and args.max_patches < 1:
            raise ValueError('--max-patches must be positive')
        if args.measure_job is not None:
            if args.manifest is None:
                raise ValueError('measurement worker requires a manifest')
            manifest = report.read_json(args.manifest)
            return measure_job(manifest, manifest['jobs'][args.measure_job], args.manifest.parent)
        if args.report_only is not None:
            # Rescore in place. Every input is already on disk, so this
            # rewrites comparison.{md,json} and candidate_metrics.jsonl
            # without rerunning a build, a campaign or a measurement.
            out = args.report_only.resolve()
            return summarize(report.read_json(out / 'manifest.json'), out)
        args.out = output_dir(args.attempts, args.kind, args.max_patches, args.measure_existing)
        manifest = prepare_run(args)
        for kind, pop in manifest['populations'].items():
            print(f"{kind}: {len(pop['bugs'])} bugs, {pop['patches']} certified patches")
        count = len(manifest['jobs']) * manifest['options']['attempts']
        action = 'existing campaigns to measure' if args.measure_existing else 'campaigns to generate'
        print(f"{len(manifest['jobs'])} {action}; {count} candidate responses across all arms", flush=True)
        run_jobs(manifest, args.out)
        return summarize(manifest, args.out)
    except (ValueError, OSError, ImportError, subprocess.CalledProcessError) as exc:
        print(f'ERROR: {exc}', file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
