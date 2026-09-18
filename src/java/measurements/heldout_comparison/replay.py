"""Remeasure missing coverage for rejected candidates, without model calls.

Usage: python -m java.measurements.heldout_comparison.replay MEASURED_RUN
Output: a separate MEASURED_RUN_replayed directory. Original outcomes stay fixed.
"""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time

import config
from java.harness.build import HarnessBuilder
from java.measurements import coverage
from . import cli, report


def plan(manifest, source):
    """Replay only rejected, compiled candidates without an original XML."""
    tasks = []
    for job in manifest['jobs']:
        leg = cli.artifact_dir(job, source)
        rec = cli.validate_record(leg, job, manifest['options']['attempts'])
        for attempt in rec['candidate_attempts']:
            label = attempt['attempt_label']
            if not attempt['compiled'] or (leg / 'cov' / f'{label}_compiled.xml').exists():
                continue
            if attempt['accepted']:
                raise ValueError(f'{job["leg"]}/{label}: accepted candidate; outside rejected-only replay')
            if (leg / 'cov' / f'{label}_compiled.exec').exists():
                raise ValueError(f'{job["leg"]}/{label}: original dump exists; convert it instead of replaying')
            src = leg / 'harness_src' / f'{label}.java'
            raw = leg / 'fuzz_out' / f'{label}_compiled.txt'
            seed = re.search(r'^INFO: Seed: (\d+)', raw.read_text(), re.MULTILINE)
            if seed is None:
                raise ValueError(f'{raw}: no original Jazzer seed')
            tasks.append({'leg': job['leg'], 'attempt_label': label,
                          'source': str(src), 'source_sha256': cli.sha256(src),
                          'seed': int(seed.group(1))})
    return tasks


def build_watchdog(out):
    support = out / 'support'
    classes = support / 'classes'
    classes.mkdir(parents=True)
    src = Path(__file__).with_name('replay_support') / 'CoverageDeadline.java'
    shutil.copyfile(src, support / src.name)
    subprocess.run(['javac', '-d', str(classes), str(src)], check=True)
    mf = support / 'agent.mf'
    mf.write_text('Premain-Class: CoverageDeadline\n')
    jar = support / 'deadline.jar'
    subprocess.run(['jar', 'cfm', str(jar), str(mf), '-C', str(classes), '.'], check=True)
    return jar


def run_with_deadline(cmd, out, seconds):
    """Allow the Java watchdog to snapshot live probes, then kill the hung JVM."""
    started = time.monotonic()
    live = out / 'live.exec'
    captured_at_deadline = False
    with (out / 'fuzzer.log').open('w') as log:
        proc = subprocess.Popen(cmd, cwd=out, stdout=log, stderr=subprocess.STDOUT)
        try:
            while proc.poll() is None:
                if live.exists():
                    captured_at_deadline = True
                    proc.kill()
                    break
                if time.monotonic() - started > seconds + 15:
                    proc.kill()
                    raise RuntimeError('coverage watchdog failed to save a live dump')
                time.sleep(0.05)
        finally:
            if proc.poll() is None:
                proc.kill()
            proc.wait()
    dump = live if live.exists() else out / 'normal.exec'
    if not dump.is_file() or dump.stat().st_size == 0:
        raise RuntimeError(f'Jazzer exited {proc.returncode} without a coverage dump')
    return dump, {'returncode': proc.returncode,
                  'deadline_snapshot': captured_at_deadline,
                  'elapsed_seconds': time.monotonic() - started}


def replay_one(task, job, manifest, out, agent, classpath):
    label = task['attempt_label']
    leg = cli.artifact_dir(job, out)
    target = out / job['leg'] / 'replay' / label
    target.mkdir(parents=True)
    source = Path(task['source']).read_text()
    if cli.sha256(task['source']) != task['source_sha256']:
        raise ValueError('saved harness changed after replay selection')
    name = HarnessBuilder.primary_class_name(source)
    if not name:
        raise ValueError('saved harness has no public class')
    java = target / f'{name}.java'
    java.write_text(source)
    classes = target / 'classes'
    classes.mkdir()
    cp = report.read_json(leg / 'cov/classpath.json')
    runtime_cp = os.pathsep.join([*cp['class_dirs'], classpath, config.JAZZER_API_JAR])
    compile_cmd = ['javac', '-encoding', 'UTF-8', '-cp', runtime_cp,
                   '-d', str(classes), str(java)]
    with (target / 'compile.log').open('w') as log:
        subprocess.run(compile_cmd, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=120)
    # Each candidate gets an independent copy; no candidate or live run can
    # alter the input corpus used by another replay.
    corpus = target / 'corpus'
    shutil.copytree(out / job['leg'] / 'replay_corpus', corpus)
    seconds = manifest['replay']['deadline_seconds']
    live = target / 'live.exec'
    cmd = ['java', f'-javaagent:{agent}={seconds * 1000},{live}',
           '-cp', os.pathsep.join([config.JAZZER_STANDALONE_JAR, runtime_cp, str(classes)]),
           'com.code_intelligence.jazzer.Jazzer',
           f'--target_class={HarnessBuilder.fully_qualified_class_name(source)}',
           f'--reproducer_path={target}',
           *coverage.jazzer_coverage_args(str(target / 'normal.exec'), cp['include_glob']),
           '--', f'-max_total_time={manifest["options"]["verify_timeout"]}',
           '-runs=100000', f'-seed={task["seed"]}',
           f'-artifact_prefix={target}{os.sep}', str(corpus)]
    report.write_json(target / 'invocation.json', {'compile': compile_cmd, 'replay': cmd, **task})
    dump, status = run_with_deadline(cmd, target, seconds)
    xml = target / 'coverage.xml'
    coverage.report([str(dump)], cp['class_dirs'], cp['source_dirs'], str(xml))
    # Fail before publishing an override if the XML cannot be read.
    coverage.parse_jacoco_xml(str(xml), include_prefix=coverage._normalise_prefix(cp['include_glob']))
    status.update(task, dump=str(dump), coverage_xml=str(xml), original_outcomes_preserved=True)
    report.write_json(target / 'status.json', status)
    return status


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('run_dir', type=Path)
    parser.add_argument('--workers', type=int, default=4)
    args = parser.parse_args(argv)
    if args.workers < 1:
        parser.error('--workers must be positive')
    source = args.run_dir.resolve()
    original = report.read_json(source / 'manifest.json')
    tasks = plan(original, source)
    if not tasks:
        parser.error('no rejected candidates need replay')
    out = source.with_name(source.name + '_replayed')
    if out.exists():
        parser.error(f'{out} already exists; replay never overwrites a prior run')
    out.mkdir()
    manifest = json.loads(json.dumps(original))
    manifest['options']['out'] = str(out)
    manifest['replay'] = {
        'source_report': str(source), 'source_manifest_sha256': cli.sha256(source / 'manifest.json'),
        'code_hash': cli.code_hash(), 'candidates': len(tasks), 'workers': args.workers,
        'deadline_seconds': original['options']['verify_timeout'] + 15,
        'protocol': 'Original acceptance outcomes; original coverage where available, '
                    'saved rejected candidates replayed only where coverage is missing. '
                    'Same time/run limits and original seed; copy of final archived corpus, '
                    'not the original per-attempt corpus. Live probes saved before timeout kill.',
        'tasks': tasks}
    for job in manifest['jobs']:
        job['source_leg'] = str(cli.artifact_dir(job, source))
        job['replay_dir'] = str(out / job['leg'] / 'replay')
    report.write_json(out / 'manifest.json', manifest)
    shutil.copytree(source / 'provenance', out / 'provenance')
    agent = build_watchdog(out)
    by_leg = {j['leg']: j for j in manifest['jobs']}
    classpaths = {}
    for relative in dict.fromkeys(t['leg'] for t in tasks):
        job = by_leg[relative]
        checkout = Path(manifest['options']['checkout_root']) / relative / f'{job["project"]}_{job["bug_id"]}_buggy'
        cp = subprocess.run([str(Path(manifest['options']['d4j_home']) / 'framework/bin/defects4j'),
                             'export', '-p', 'cp.test'], cwd=checkout,
                            capture_output=True, text=True, check=True).stdout.strip()
        if not cp:
            raise ValueError(f'{relative}: empty project classpath')
        classpaths[relative] = cp
        corpus = out / relative / 'replay_corpus'
        shutil.copytree(checkout / 'fuzz/corpus', corpus)
        report.write_json(out / relative / 'replay_inputs.json', {
            'classpath': cp,
            'corpus_sha256': {str(p.relative_to(corpus)): cli.sha256(p)
                              for p in sorted(corpus.rglob('*')) if p.is_file()}})
    print(f'Replaying {len(tasks)} candidates with {args.workers} workers; output: {out}', flush=True)
    statuses, errors = [], []
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = {pool.submit(replay_one, t, by_leg[t['leg']], manifest, out, agent,
                               classpaths[t['leg']]): t for t in tasks}
        for future in as_completed(futures):
            task = futures[future]
            try:
                statuses.append(future.result())
                print(f'[{len(statuses) + len(errors)}/{len(tasks)}] OK {task["leg"]}/{task["attempt_label"]}', flush=True)
            except Exception as exc:
                errors.append({**task, 'error': f'{type(exc).__name__}: {exc}'})
                print(f'FAILED {task["leg"]}/{task["attempt_label"]}: {exc}', flush=True)
            report.write_json(out / 'replay_progress.json', {'completed': len(statuses), 'errors': errors,
                                                          'expected': len(tasks)})
    report.write_json(out / 'replay_status.json', {'completed': statuses, 'errors': errors})
    return cli.summarize(manifest, out)


if __name__ == '__main__':
    raise SystemExit(main())
