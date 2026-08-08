#!/usr/bin/env python3
"""Re-walk #8: replay roll 10's RECORDED artifacts through every remaining
unproven step, with the real javac/JVM. VM-only; zero generation cost.

Why this exists (the roll-11 gate): sixteen defects in, every one a seam,
and every walk before this one verified up to the frontier instead of
THROUGH it — so each roll bought exactly one seam. This script drives the
whole post-generation path end to end with recorded material:

  [1] the twin recompiles and reruns (proven twice; rerun for true state)
  [2] the reference compiles              <- first-time event
  [3] every literal reconstructs (incl. the 2-D jacobian)
  [4] the driver compiles WITH the reference's class dir on -cp  <- roll-10 fix
  [5] the driver runs; observables parse  <- first-time event
  [6] THE SCREEN, at true test state      <- the substantive question
  [7] the pin check at the disputed point

Inputs are verbatim from ladder1k's trace/checkout: the reference the model
actually wrote, the twin the chain actually built, the merged signature and
matched-observable dict the chain actually recorded. Nothing synthesized.

Run on the VM:
  cd /home/code/experiments-vuln-patch && python3 scripts/rewalk8_replay.py
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

from java.harness.build import HarnessBuilder                      # noqa: E402
from java.relations import reference_run as rr                     # noqa: E402
from java.relations.reference_impl import (                        # noqa: E402
    pin_check, pins_for_disputed, screen_reference, test_corroboration_pins)

CO = ('/home/code/scratch/co/stage2b_20260808_063833/'
      '01_patch1-Math-65-CapGen_c/Math_65_buggy')

# Recorded by the chain in stage2b's (stage-2 roll 2's) trace — verbatim.
SIG = ('double[][] jacobian, double[] residuals, double[] residualsWeights, '
       'double cost, int rows, int cols')
MATCHED = {'getChiSquare': 'getChiSquare', 'getCovariances': 'getCovariances',
           'getRMS': 'getRMS', 'guessParametersErrors': 'guessParametersErrors'}
SIBLINGS = ['getCovariances', 'getEvaluations', 'getIterations',
            'getJacobianEvaluations', 'getRMS', 'guessParametersErrors']
# The failing test never asserts getChiSquare directly, so the production
# attribution (pins_for_disputed) must yield NO pin and the check ABSTAINS.


def step(n, title, ok, why):
    mark = 'OK ' if ok else 'FAIL'
    print(f'[{n}] {mark} {title}: {why}')
    if not ok:
        print('\n== WALK STOPS HERE — this is the defect to fix ==')
        sys.exit(1)


def main():
    ref_src = open(f'{CO}/fuzz/reference/ReferenceImpl.java').read()
    twin_src = open(f'{CO}/fuzz/reference_twin/StateTwinDriver.java').read()
    builder = HarnessBuilder(jazzer_api_jar='/nonexistent-jazzer.jar')

    vals, why = rr.run_twin(builder, CO, twin_src,
                            work_subdir='rewalk8_twin')
    step(1, 'twin recompiles and reruns', bool(vals), why)

    params = rr.parse_parameters(SIG)
    lits = []
    for typ, name in params:
        printed = (vals.get(f'__param_{name}') or ['ABSENT'])[0]
        lit = rr.java_literal(typ, printed)
        print(f'    literal {name:18s} ({typ:10s}): '
              + ('ok' if lit else f'FAILED from {printed[:70]!r}'))
        lits.append((lit, typ))
    step(3, 'every literal reconstructs', all(l for l, _ in lits),
         f'{len([l for l, _ in lits if l])}/{len(lits)}')

    driver_src = rr.build_reference_call_driver(
        'ReferenceImpl', list(MATCHED.items()), lits)
    obs, why = rr.run_reference(builder, CO, ref_src, driver_src,
                                work_subdir='rewalk8_ref')
    step(5, 'reference + driver compile and run', bool(obs), why)

    buggy_obs = {k: v for k, v in vals.items() if not k.startswith('__')}
    off = [k for k in SIBLINGS if k in obs and k in buggy_obs]
    # OPTION B, verbatim recorded material: the failure message and the
    # test's assertion line, attributed through the production function.
    failure_msg = ('junit.framework.AssertionFailedError: '
                   'expected:<0.004> but was:<0.0019737107108948474>')
    assert_src = 'assertEquals(0.004, errors[0], 0.001);'
    pins = test_corroboration_pins([failure_msg], [assert_src],
                                   buggy_obs, SIBLINGS)
    print(f'    corroboration pins: {pins}')
    ok, why = screen_reference(obs, buggy_obs, off_defect_keys=set(off),
                               test_corroboration=pins)
    print(f'    shared off-defect: {off}')
    for k in sorted(set(obs) | set(buggy_obs)):
        print(f'    {k:24s} ref={str(obs.get(k, "-"))[:52]:54s} '
              f'buggy={str(buggy_obs.get(k, "-"))[:52]}')
    step(6, 'THE SCREEN (admit = doc-derived reference reproduces buggy '
            'on siblings)', True, ('ADMITTED — ' if ok else 'DISCARDED — ')
         + why)

    # Roll 11's exact material: the RMS assertion whose literal was
    # misattributed to getChiSquare, plus the real failure message. The
    # production attribution must yield NO pin for getChiSquare -> ABSTAIN.
    rms_assert = ('assertEquals(1.768262623567235,  '
                  'Math.sqrt(circle.getN()) * rms,  1.0e-10);')
    pins_d = pins_for_disputed('getChiSquare', [failure_msg],
                               [rms_assert + '\n' + assert_src], buggy_obs)
    print(f'    disputed pins: {pins_d}')
    ok, why = pin_check(obs, pins_d, ['getChiSquare'])
    step(7, 'pin check at the disputed point', True,
         ('pass/abstain — ' if ok else 'DISCARD (bug-copy) — ') + why)

    print('\n== WALK COMPLETE: every remaining mechanical step ran. ==')


if __name__ == '__main__':
    main()
