"""Command line for one bug's RCC number.

Everything it needs already exists on disk: the developer fix, the buggy
checkout, and one or more JaCoCo reports from the fuzz run. It runs no
fuzzer and no build. See `src/metrics/README.md` for how to produce the
reports.

    python src/metrics/cli.py --project Lang --bug 1 \
        --buggy-dir /tmp/d4j/Lang_1_buggy \
        --report runs/lang1/set/jacoco.xml \
        --trigger-report runs/lang1/trigger/jacoco.xml
"""
import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from metrics import rcc, reached, region as region_mod   # noqa: E402


def _build_region(args):
    if args.patch:
        with open(args.patch, encoding='utf-8', errors='replace') as handle:
            return region_mod.region_from_patch(handle.read(), args.buggy_dir)
    return region_mod.region_from_defects4j(args.project, args.bug,
                                            args.buggy_dir)


def _in_patch_order(result):
    """Every method in R-hat, hit and missed together, in one stable order."""
    return sorted(result.covered + result.missed, key=str)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', help='Defects4J project, e.g. Lang')
    parser.add_argument('--bug', help='Defects4J bug id, e.g. 1')
    parser.add_argument('--patch', help='developer fix file, instead of '
                                        '--project/--bug')
    parser.add_argument('--buggy-dir', required=True,
                        help='the buggy checkout the fix applies to')
    parser.add_argument('--report', required=True, action='append',
                        help='a jacoco.xml from the harness set; repeatable')
    parser.add_argument('--trigger-report',
                        help='a jacoco.xml from the bug triggering test')
    args = parser.parse_args(argv)

    if not args.patch and not (args.project and args.bug):
        parser.error('give either --patch, or both --project and --bug')

    region = _build_region(args)
    print(f'R-hat: {region.size} method(s)')
    for method in region.methods:
        print(f'  {method.method_id}  [{method.rel_path}:{method.decl_line}]')
    for entry in region.unmapped:
        print(f'  unmapped: {entry}')
    if region.is_empty:
        print('\nEXCLUDED: the developer fix changed no method body.')
        return 2

    if args.trigger_report:
        gate = rcc.trigger_gate(
            region, reached.reached_from_report(args.trigger_report))
        print(f'\ntrigger gate: {"PASS" if gate.passed else "FAIL"} '
              f'— {gate.detail}')
        if not gate.passed:
            print('EXCLUDED: R-hat or the coverage plumbing is wrong.')
            return 2
    else:
        print('\ntrigger gate: NOT RUN (no --trigger-report). The number '
              'below is unverified.')

    result = rcc.root_cause_coverage(
        region, reached.reached_from_reports(args.report))
    print(f'\nF(H): {result.reached_size} method(s) reached')
    for key in _in_patch_order(result):
        mark = 'HIT ' if key in result.covered else 'MISS'
        note = '  (arity fallback)' if key in result.by_arity_only else ''
        print(f'  {mark} {key}{note}')
    print(f'\nRCC = {len(result.covered)}/{result.region_size} '
          f'= {result.value:.3f}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
