"""Re-score a finished run: all five region metrics from what it left on disk.

`sweep_full.py` now records all five, but the run this was written for
recorded RCC only, and RCC needs just R-hat and F(H). The
other four metrics need two more things, and both can be rebuilt after the
fact from what the run left on disk:

  * P — the patch-derived set. Rebuilt by re-running the pipeline's OWN
    static analysis on the same APR patch and the same buggy checkout. It
    is deterministic under the same budget, so it reproduces what the
    pipeline saw. It costs no model call and no fuzz time.
  * C — the crashes. Read from the measurement pass's Jazzer output, which
    the run already saved as `harness/jazzer_output.txt`.

So this script adds nothing to the experiment. It re-reads one finished run
and reports RCC, RCR, RCP, PSC and CSM for it.

    python src/metrics/rescore.py \\
        --run results/rcc_hr_crashing_holdout_20260904_001615 --drr drr

It also recomputes RCC. A recomputed RCC that differs from the recorded one
means the run directory and the checkout no longer agree, so the number is
printed next to the original rather than replacing it.

MEASUREMENT ONLY.
"""
import argparse
import json
import os
import sys
import traceback

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from metrics import (                                     # noqa: E402
    collect, crashes, patchset, reached, region as region_mod, scores,
)

REPO = os.path.dirname(os.path.dirname(os.path.dirname(
    os.path.abspath(__file__))))


def harness_classes(bug_dir: str):
    """The class names of the accepted harnesses, from the leg's record."""
    path = os.path.join(bug_dir, 'leg', 'result.jsonl')
    if not os.path.isfile(path):
        return []
    with open(path) as handle:
        leg = json.loads(handle.readline())
    return [entry.get('class_name', '')
            for entry in leg.get('accepted_harnesses', [])]


def measure_bug(record: dict, run_dir: str, drr_root: str) -> dict:
    """The five metrics for one finished bug. Never raises."""
    out = {'project': record['project'], 'bug_id': record['bug_id'],
           'status': record['status'], 'rcc_recorded': record.get('rcc')}
    if record['status'] != 'ok':
        return out

    bug_dir = os.path.join(run_dir, f'{record["project"]}_{record["bug_id"]}')
    report = os.path.join(bug_dir, 'harness', reached.REPORT_NAME)
    trace_path = os.path.join(bug_dir, 'harness', 'jazzer_output.txt')
    try:
        buggy_dir = collect.ensure_buggy_build(record['project'],
                                               record['bug_id'])
        region = region_mod.region_from_defects4j(
            record['project'], record['bug_id'], buggy_dir)
        with open(trace_path, encoding='utf-8', errors='replace') as handle:
            trace = handle.read()

        # F(H), exactly as the sweep built it: probes, plus the methods the
        # probes provably missed because the path threw through them.
        fuzzer = (reached.reached_from_report(report)
                  | reached.reached_from_stack(report, trace))
        patch = patchset.patch_set_from_patch(
            os.path.join(drr_root, record['leg']), buggy_dir)
        found = crashes.crashes(report, trace, harness_classes(bug_dir))

        sets = scores.set_metrics(region, patch.keys, fuzzer)
        match = scores.crash_site_match(region, found)

        out.update({
            'region_size': sets.region_size,
            'patch_size': sets.patch_size,
            'patch_touched_size': len(patch.touched),
            'patch_notes': patch.notes,
            'fuzzer_reached_size': sets.reached_size,
            'rcc': sets.rcc, 'rcr': sets.rcr,
            'rcp': sets.rcp, 'psc': sets.psc,
            'csm': match.value,
            'crashes_total': match.total,
            'crashes_in_region': match.matched,
            'crashes_off_region': match.off_region,
            'crashes_no_frame': match.no_frame,
            'crashes_unresolved': match.unresolved,
            'region_in_patch': [str(k) for k in sets.region_in_patch],
            'region_in_reached': [str(k) for k in sets.region_in_reached],
            'crash_sites': [
                str(crash.site) if crash.site
                else ('no_frame' if crash.frame is None
                      else f'unresolved:{crash.frame}')
                for crash in found],
        })
    except Exception as exc:                       # noqa: BLE001
        out['status'] = 'infra_error'
        out['error'] = f'{type(exc).__name__}: {exc}'
        out['traceback'] = traceback.format_exc()
    return out


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True,
                        help='a directory `sweep_full.py` wrote')
    parser.add_argument('--drr', default=os.path.join(REPO, 'drr'))
    parser.add_argument('--out', default=None,
                        help='output jsonl (default: <run>/metrics.jsonl)')
    args = parser.parse_args(argv)

    with open(os.path.join(args.run, 'rcc.jsonl')) as handle:
        records = [json.loads(line) for line in handle if line.strip()]

    path = args.out or os.path.join(args.run, 'metrics.jsonl')
    rows = []
    with open(path, 'w') as out:
        for index, record in enumerate(records, 1):
            name = f'{record["project"]}-{record["bug_id"]}'
            print(f'[{index}/{len(records)}] {name}', flush=True)
            row = measure_bug(record, args.run, args.drr)
            rows.append(row)
            out.write(json.dumps(row) + '\n')
            out.flush()
            print(f'    {row["status"]}  RCC={_fmt(row.get("rcc"))} '
                  f'RCR={_fmt(row.get("rcr"))} RCP={_fmt(row.get("rcp"))} '
                  f'PSC={_fmt(row.get("psc"))} CSM={_fmt(row.get("csm"))}',
                  flush=True)

    print(f'\nwrote {path}')
    _summary(rows)
    return 0


def _fmt(value) -> str:
    return f'{value:.3f}' if isinstance(value, float) else '-'


def _summary(rows) -> None:
    names = ['rcc', 'rcr', 'rcp', 'psc', 'csm']
    header = (f'\n{"bug":<10} {"|R|":>4} {"|P|":>5} {"|F|":>5} {"|C|":>4}  '
              + '  '.join(f'{n.upper():>6}' for n in names) + '  status')
    print(header)
    for row in rows:
        print(f'{row["project"] + "-" + str(row["bug_id"]):<10} '
              f'{str(row.get("region_size", "-")):>4} '
              f'{str(row.get("patch_size", "-")):>5} '
              f'{str(row.get("fuzzer_reached_size", "-")):>5} '
              f'{str(row.get("crashes_total", "-")):>4}  '
              + '  '.join(f'{_fmt(row.get(n)):>6}' for n in names)
              + f'  {row["status"]}')
    print()
    for name in names:
        scored = [row[name] for row in rows
                  if isinstance(row.get(name), float)]
        if scored:
            print(f'mean {name.upper()} = {sum(scored) / len(scored):.3f}  '
                  f'over {len(scored)} scored bug(s)')
        else:
            print(f'mean {name.upper()} = undefined (no scored bug)')


if __name__ == '__main__':
    raise SystemExit(main())
