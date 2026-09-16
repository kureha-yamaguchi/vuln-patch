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


def prepare_run(args):
    out = args.out.resolve()
    if out.exists() and any(out.iterdir()):
        raise ValueError(f'{out} is not empty; move the previous run before starting another')
    out.mkdir(parents=True, exist_ok=True)
    opts = {k: v for k, v in vars(args).items()
            if k != 'measure_job'}
    opts = {k: str(v.resolve()) if isinstance(v, Path) else v for k, v in opts.items()}
    opts['checkout_root'] = str((args.checkout_root or out / 'checkouts').resolve())
    manifest = {'schema': 2, 'arms': list(ARMS), 'created_utc': datetime.now(timezone.utc).isoformat(),
                'code_hash': code_hash(), 'options': opts, 'populations': {}, 'jobs': []}
    git = subprocess.run(['git', 'rev-parse', 'HEAD'], cwd=REPO,
                         capture_output=True, text=True)
    manifest['git_sha'] = git.stdout.strip() or 'unknown'
    for kind in KINDS:
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
            for repetition in range(1, args.repetitions + 1):
                # Rotate all three arms across first/middle/last positions.
                arms = list(ARMS)
                offset = (index + repetition - 2) % len(arms)
                arms = arms[offset:] + arms[:offset]
                for arm in arms:
                    leg = f'{kind}/{arm}/{patch_id}_rep{repetition:03d}'
                    manifest['jobs'].append({**patch, 'kind': kind, 'arm': arm,
                                             'repetition': repetition, 'leg': leg})
        if actual != expected:
            raise ValueError(f'{kind}: frozen bug population mismatch; '
                             f'missing={sorted(expected - actual)}, extra={sorted(actual - expected)}')
        manifest['populations'][kind] = {'bugs': sorted(expected), 'patches': len(patches),
                                          'input_sha256': hashes}
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
    from fuzz_introspector import commands  # noqa: F401
    failures = []
    for index, job in enumerate(manifest['jobs'], 1):
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
        if any(e['leg'] == job['leg'] for e in failures):
            continue
        print(f"[measure {index}/{len(manifest['jobs'])}] {job['leg']}", flush=True)
        try:
            # Keep per-job Defects4J configuration process-local, as in the
            # existing suite runner. The measurement command never calls an LLM.
            env = environment(job, manifest)
            proc = run_logged(
                [sys.executable, '-m', 'java.measurements.heldout_comparison.cli',
                 '-N', str(opts['attempts']), '--measure-job', str(index - 1)],
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
    leg = out / job['leg']
    rec = validate_record(leg, job, opts['attempts'])
    status = measure_leg(
        str(leg), checkout_root=str(Path(opts['checkout_root']) / job['leg']),
        d4j_home=opts['d4j_home'], trigger_gate=True,
        coverage=any(a['compiled'] for a in rec['candidate_attempts']))
    report.write_json(leg / 'measurement_status.json', status)
    required = {'checkout', 'root_cause', 'trigger_gate', 'coverage'}
    errors = {k: v for k, v in status['errors'].items() if k in required}
    if errors:
        raise ValueError(f'measurement stages failed: {errors}')
    return 0


def summarize(manifest, out):
    opts = manifest['options']
    rows, errors = [], []
    for job in manifest['jobs']:
        leg = out / job['leg']
        try:
            validate_record(leg, job, opts['attempts'])
            scores = report.score_leg(leg, opts['attempts'])
            report.write_json(leg / 'candidate_metrics.json', scores)
            rows.append({**job, **scores})
        except Exception as exc:
            errors.append({'leg': job['leg'], 'error': f'{type(exc).__name__}: {exc}'})
    result = {'complete': not errors, 'errors': errors, 'groups': {},
              'bootstrap': opts['bootstrap'], 'seed': opts['seed'],
              'arms': manifest.get('arms', ['HN', 'HR']),
              'expected_legs': len(manifest['jobs']), 'scored_legs': len(rows)}
    # A complete kind can be shown even if the other failed, but never show
    # a partial kind under the name of the full frozen population.
    for kind in KINDS:
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


def output_dir(attempts):
    """Store the complete experiment under its candidate budget."""
    return REPO / 'results' / f'heldout_rcc_{attempts}'


def parser():
    p = argparse.ArgumentParser(description=__doc__,
                                epilog='Output: results/heldout_rcc_N under the repository root.')
    p.add_argument('--measure-job', type=int, default=None, help=argparse.SUPPRESS)
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
        args.out = output_dir(args.attempts)
        if args.measure_job is not None:
            manifest = report.read_json(args.out / 'manifest.json')
            return measure_job(manifest, manifest['jobs'][args.measure_job], args.out)
        manifest = prepare_run(args)
        for kind, pop in manifest['populations'].items():
            print(f"{kind}: {len(pop['bugs'])} bugs, {pop['patches']} certified patches")
        count = len(manifest['jobs']) * manifest['options']['attempts']
        print(f"{len(manifest['jobs'])} campaigns; {count} candidate responses total across all planned arms", flush=True)
        run_jobs(manifest, args.out)
        return summarize(manifest, args.out)
    except (ValueError, OSError, ImportError, subprocess.CalledProcessError) as exc:
        print(f'ERROR: {exc}', file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
