"""Run the R-hat construction and the triggering-test gate over one split.

This is the half of the RCC measurement that needs no fuzzer, no model and
no harness. It answers two questions for every bug in a split:

  1. How big is R-hat? These denominators are small, and the metric's
     meaning depends on knowing how small.
  2. Does the bug's own triggering test run all of R-hat? A bug that fails
     that gate leaves the population, because its RCC would be unreadable.

Run it before spending anything on harness generation. A bug that fails here
would fail no matter how good its harness set is.

    python src/metrics/sweep.py \
        --split suites/splits/crashing_split.jsonl --side holdout \
        --out results/rcc_crashing_holdout

MEASUREMENT ONLY.
"""
import argparse
import json
import os
import sys
import traceback

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from metrics import collect, rcc, reached, region as region_mod  # noqa: E402


def sweep_bug(project: str, bug_id, out_root: str) -> dict:
    """R-hat and the gate for one bug. Never raises: a broken bug is a
    recorded status, not a crashed sweep."""
    record = {'project': project, 'bug_id': bug_id, 'status': 'ok'}
    try:
        buggy_dir = collect.ensure_buggy_build(project, bug_id)
        region = region_mod.region_from_defects4j(project, bug_id, buggy_dir)
        record['region_size'] = region.size
        record['region'] = [str(key) for key in sorted(region.keys, key=str)]
        record['unmapped'] = region.unmapped
        record['trigger_tests'] = collect.trigger_tests(buggy_dir)
        record['instrumentation_includes'] = collect.package_prefix(
            buggy_dir, region.methods)

        if region.is_empty:
            record['status'] = 'excluded_empty_region'
            record['gate'] = False
            return record

        out_dir = os.path.join(out_root, f'{project}_{bug_id}', 'trigger')
        run = collect.trigger_coverage(buggy_dir, out_dir)
        # Probes give the lower bound. Frames add what the probes provably
        # missed: JaCoCo's probe sits after a method's exit, so a method
        # that throws through it reads as missed. The two stay apart in the
        # record, so a frame-only hit is never mistaken for a probe hit.
        by_probe = reached.reached_from_report(run.report)
        by_frame = reached.reached_from_stack(run.report, run.trace)
        trigger_reached = by_probe | by_frame
        gate = rcc.trigger_gate(region, trigger_reached)
        record['trigger_reached_size'] = len(trigger_reached)
        record['trigger_probe_size'] = len(by_probe)
        record['trigger_frame_added'] = sorted(
            str(key) for key in by_frame - by_probe)
        record['gate'] = gate.passed
        record['gate_detail'] = gate.detail
        record['gate_missed'] = [str(key) for key in gate.missed]
        if not gate.passed:
            record['status'] = 'excluded_gate_failed'
    except Exception as exc:                       # noqa: BLE001
        record['status'] = 'infra_error'
        record['error'] = f'{type(exc).__name__}: {exc}'
        record['traceback'] = traceback.format_exc()
    return record


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--split', required=True, help='a split .jsonl')
    parser.add_argument('--side', required=True,
                        choices=['dev', 'holdout'])
    parser.add_argument('--out', required=True, help='output directory')
    args = parser.parse_args(argv)

    with open(args.split) as handle:
        bugs = [json.loads(line) for line in handle if line.strip()]
    bugs = [b for b in bugs if b.get('side') == args.side]

    os.makedirs(args.out, exist_ok=True)
    records_path = os.path.join(args.out, 'region_gate.jsonl')
    records = []
    with open(records_path, 'w') as out:
        for index, bug in enumerate(bugs, 1):
            project, bug_id = bug['project'], bug['bug_id']
            print(f'[{index}/{len(bugs)}] {project}-{bug_id}', flush=True)
            record = sweep_bug(project, bug_id, args.out)
            records.append(record)
            out.write(json.dumps(record) + '\n')
            out.flush()
            print(f'    {record["status"]}  '
                  f'|R-hat|={record.get("region_size", "?")}  '
                  f'gate={record.get("gate", "?")}', flush=True)

    print(f'\nwrote {records_path}')
    _summary(records)
    return 0


def _summary(records) -> None:
    usable = [r for r in records if r['status'] == 'ok']
    print(f'\n{"bug":<12} {"|R-hat|":>8}  {"gate":<6} status')
    for record in records:
        name = f'{record["project"]}-{record["bug_id"]}'
        print(f'{name:<12} {str(record.get("region_size", "-")):>8}  '
              f'{str(record.get("gate", "-")):<6} {record["status"]}')
    print(f'\nusable bugs: {len(usable)}/{len(records)}')
    if usable:
        sizes = sorted(r['region_size'] for r in usable)
        print(f'|R-hat| min/median/max: {sizes[0]}/'
              f'{sizes[len(sizes) // 2]}/{sizes[-1]}')


if __name__ == '__main__':
    raise SystemExit(main())
