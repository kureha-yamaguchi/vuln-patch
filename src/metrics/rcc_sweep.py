"""Measure RCC(H_R) over one split, end to end.

For each bug in the split this script:

  1. picks one candidate patch (one "leg") for the bug, deterministically;
  2. runs the harness pipeline on that leg, which produces the accepted
     harness set H_R and records where each harness lives;
  3. builds R-hat from the developer fix, and runs the triggering-test gate;
  4. re-runs the accepted harnesses for coverage only, and computes RCC.

Step 2 costs model calls and fuzz time. Steps 3 and 4 cost neither, so
`sweep.py` can run them alone first to check the population.

WHY ONE LEG PER BUG. The harness set is conditioned on the patch under
analysis, so H_R differs from leg to leg. R-hat, however, comes from the
developer fix and is a property of the BUG. One leg per bug therefore gives
one RCC per bug, which is the unit the split is written in. An overfitting
patch is preferred when the bug has one, because that is the case where a
sibling bug matters; otherwise the first correct patch is used. Within a
class the choice is the lexicographically first path, so the selection is
reproducible.

MEASUREMENT ONLY.
"""
import argparse
import glob
import json
import os
import re
import subprocess
import sys
import traceback

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import config                                                    # noqa: E402
from metrics import collect, rcc, reached, sweep                 # noqa: E402
from metrics import region as region_mod                         # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.dirname(
    os.path.abspath(__file__))))
SRC = os.path.join(REPO, 'src')


def select_leg(project: str, bug_id, drr_root: str):
    """One (patch_file, kind) for a bug, or None. See the module docstring."""
    for kind, folder in (('overfitting', 'Doverfitting'),
                         ('correct', 'Dcorrect')):
        pattern = os.path.join(drr_root, 'Patches', folder, '*', project,
                               '*.patch')
        # `-Lang-16-` and not `-Lang-1-`: the bug id must be a whole field.
        wanted = re.compile(rf'-{re.escape(project)}-{re.escape(str(bug_id))}-')
        found = sorted(p for p in glob.glob(pattern) if wanted.search(p))
        if found:
            return found[0], kind
    return None, None


def run_leg(patch_file: str, kind: str, out_dir: str, model: str,
            targets: int, attempts: int, fuzz_timeout: int) -> dict:
    """Run the harness pipeline on one leg. Returns its record."""
    # ABSOLUTE, all of them. `java/run.py` runs with cwd=src, so a relative
    # path there resolves under src/ and the record lands nowhere. Same
    # class of fault as the trigger dump in `collect.trigger_coverage`.
    out_dir = os.path.abspath(out_dir)
    patch_file = os.path.abspath(patch_file)
    os.makedirs(out_dir, exist_ok=True)
    results_json = os.path.join(out_dir, 'result.jsonl')
    env = dict(os.environ)
    env['PATH'] = (env.get('PATH', '') + os.pathsep +
                   os.path.join(config.D4J_HOME, 'framework', 'bin'))
    env['PYTHONUNBUFFERED'] = '1'
    cmd = [sys.executable, '-u', 'java/run.py',
           f'--{kind}', '--patch_file', patch_file, '--model', model,
           '-n', str(targets), '-m', str(attempts),
           '--fuzz_timeout', str(fuzz_timeout),
           '--verify_timeout', str(fuzz_timeout),
           '--results_json', results_json]
    with open(os.path.join(out_dir, 'run.log'), 'w') as log:
        subprocess.run(cmd, cwd=SRC, env=env, stdout=log,
                       stderr=subprocess.STDOUT)
    if not os.path.isfile(results_json):
        raise collect.CollectionError('the leg produced no record')
    with open(results_json) as handle:
        return json.loads(handle.readline())


def measure_bug(project: str, bug_id, out_root: str, args) -> dict:
    """R-hat, the gate, H_R and RCC for one bug. Never raises."""
    record = sweep.sweep_bug(project, bug_id, out_root)
    if record['status'] != 'ok':
        return record

    bug_dir = os.path.join(out_root, f'{project}_{bug_id}')
    try:
        patch_file, kind = select_leg(project, bug_id, args.drr)
        if not patch_file:
            record['status'] = 'no_patch'
            return record
        record['leg'] = os.path.relpath(patch_file, args.drr)
        record['leg_kind'] = kind

        leg = run_leg(patch_file, kind, os.path.join(bug_dir, 'leg'),
                      args.model, args.targets, args.attempts,
                      args.fuzz_timeout)
        accepted = leg.get('accepted_harnesses', [])
        record['leg_status'] = leg.get('status')
        record['harness_set_size'] = len(accepted)
        if not accepted:
            record['status'] = 'no_harnesses'
            return record

        buggy_dir = os.path.join(config.D4J_CHECKOUT_ROOT,
                                 f'{project}_{bug_id}_buggy')
        region = region_mod.region_from_defects4j(project, bug_id, buggy_dir)
        run = collect.harness_coverage(
            buggy_dir, accepted, os.path.join(bug_dir, 'harness'),
            includes=record.get('instrumentation_includes', ''),
            runs=args.runs)
        record['per_harness'] = run.per_harness

        probe = reached.reached_from_report(run.report)
        frame = reached.reached_from_stack(run.report, run.trace)
        record['fuzzer_reached_size'] = len(probe | frame)
        record['fuzzer_probe_size'] = len(probe)
        record['fuzzer_frame_added'] = sorted(str(k) for k in frame - probe)

        result = rcc.root_cause_coverage(region, probe | frame)
        record['rcc'] = result.value
        record['rcc_covered'] = [str(k) for k in result.covered]
        record['rcc_missed'] = [str(k) for k in result.missed]
        record['rcc_by_arity_only'] = [str(k) for k in result.by_arity_only]
    except Exception as exc:                       # noqa: BLE001
        record['status'] = 'infra_error'
        record['error'] = f'{type(exc).__name__}: {exc}'
        record['traceback'] = traceback.format_exc()
    return record


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--split', required=True)
    parser.add_argument('--side', required=True, choices=['dev', 'holdout'])
    parser.add_argument('--out', required=True)
    parser.add_argument('--drr', default=os.path.join(REPO, 'drr'))
    parser.add_argument('--model', default='gpt-5.4')
    parser.add_argument('--targets', type=int, default=3,
                        help='harnesses to accept per leg')
    parser.add_argument('--attempts', type=int, default=8)
    parser.add_argument('--fuzz_timeout', type=int, default=20)
    parser.add_argument('--runs', type=int, default=20000,
                        help='libFuzzer runs per harness in the '
                             'measurement pass')
    args = parser.parse_args(argv)

    with open(args.split) as handle:
        bugs = [json.loads(line) for line in handle if line.strip()]
    bugs = [b for b in bugs if b.get('side') == args.side]

    os.makedirs(args.out, exist_ok=True)
    path = os.path.join(args.out, 'rcc.jsonl')
    records = []
    with open(path, 'w') as out:
        for index, bug in enumerate(bugs, 1):
            project, bug_id = bug['project'], bug['bug_id']
            print(f'[{index}/{len(bugs)}] {project}-{bug_id}', flush=True)
            record = measure_bug(project, bug_id, args.out, args)
            records.append(record)
            out.write(json.dumps(record) + '\n')
            out.flush()
            print(f'    {record["status"]}  |R-hat|='
                  f'{record.get("region_size", "?")}  '
                  f'|H|={record.get("harness_set_size", "?")}  '
                  f'RCC={record.get("rcc", "?")}', flush=True)

    print(f'\nwrote {path}')
    _summary(records)
    return 0


def _summary(records) -> None:
    print(f'\n{"bug":<10} {"leg":<12} {"|R-hat|":>7} {"|H|":>4} '
          f'{"|F(H)|":>7} {"RCC":>6}  status')
    scored = []
    for record in records:
        name = f'{record["project"]}-{record["bug_id"]}'
        value = record.get('rcc')
        if value is not None:
            scored.append(value)
        print(f'{name:<10} {str(record.get("leg_kind", "-")):<12} '
              f'{str(record.get("region_size", "-")):>7} '
              f'{str(record.get("harness_set_size", "-")):>4} '
              f'{str(record.get("fuzzer_reached_size", "-")):>7} '
              f'{("%.3f" % value) if value is not None else "-":>6}  '
              f'{record["status"]}')
    if scored:
        print(f'\nscored bugs: {len(scored)}/{len(records)}')
        print(f'mean RCC(H_R) = {sum(scored) / len(scored):.3f}')
        print(f'bugs with RCC = 1.0: '
              f'{sum(1 for v in scored if v == 1.0)}/{len(scored)}')


if __name__ == '__main__':
    raise SystemExit(main())
