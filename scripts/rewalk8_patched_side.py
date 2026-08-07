#!/usr/bin/env python3
"""Re-walk #8 addendum: the PATCHED side of the would-be fact.

The chain's final step (never yet reached in a roll: the screen discards
first) is to run the twin on the PATCHED build and compare the reference's
disputed-point value against the patch's. This shows what that fact would
say for Math-65, using roll 10's recorded twin and reference values.
"""
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

from java.harness.build import HarnessBuilder                      # noqa: E402
from java.relations import reference_run as rr                     # noqa: E402

BASE = ('/home/code/scratch/co/ladder1k_20260807_164149/'
        '01_patch1-Math-65-CapGen_c')
BUGGY = BASE + '/Math_65_buggy'
PATCHED = BASE + '/Math_65_buggy_patched_patch1-Math-65-CapGen'

# The reference's outputs from the re-walk #8 run (recorded).
REF = {'getChiSquare': '6.253505411815327',
       'getRMS': '0.09931552348327041',
       'guessParametersErrors': '[0.003947421421789695, 0.003953773486615504]'}
BUGGY_VALS = {'getChiSquare': '1.5633763529538318',
              'getRMS': '0.09931552348327041',
              'guessParametersErrors':
                  '[0.0019737107108948474, 0.001976886743307752]'}


def main():
    twin_src = open(BUGGY + '/fuzz/reference_twin/StateTwinDriver.java').read()
    builder = HarnessBuilder(jazzer_api_jar='/nonexistent-jazzer.jar')
    vals, why = rr.run_twin(builder, PATCHED, twin_src,
                            work_subdir='rewalk8_twin_p')
    print('patched twin:', why)
    if not vals:
        sys.exit(1)
    from java.relations.reference_impl import _values_agree
    for k in ('getChiSquare', 'getRMS', 'guessParametersErrors'):
        p = (vals.get(k) or ['-'])[0]
        agree = _values_agree(p, REF[k])
        print(f'  {k}:')
        print(f'      buggy     = {BUGGY_VALS[k]}')
        print(f'      patched   = {p}')
        print(f'      reference = {REF[k]}'
              f'   -> {"AGREES with patched" if agree else "DISAGREES"}')


if __name__ == '__main__':
    main()
