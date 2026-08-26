"""READ 2's offline gates, discharged against the archived runs.

`docs/p1b-aiming-and-rms-read-2026-08-11.md` prereg two gates that need no
model call and no VM:

  * **G-R1** — replay `observables_probed_by` + `widening_targets` under Fix 1
    on all five `p1b_live` legs' archived contexts and checks. Pinned
    prediction, verbatim: `rms` in the target list for legs 01, 02, 03;
    `optimize` / `add` / `Axis` / `RectangleEdge` absent from all five.
  * **G-R2** — replay the screen under Fix 2 on the archived `getRMS`
    discards. Prediction: all of them stop being discarded *for
    `getChiSquare`*. "Any that then fail on a different sibling is a separate
    finding and must be written down before the live roll."

Both replays read the archive itself — the legs' own `code_context` out of
`result.jsonl`, their own synthesised checks and recorded screen details out
of `trace.md`, and the patch-touched method names out of the same
`TargetAnalyzer` event the pipeline records at step [2]. Nothing here is
invented, and nothing is pooled: the archive is read, never written.

The archive is not part of a fresh checkout, so every test skips when it is
absent.
"""
import glob
import json
import re
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'src'))

from java.relations.reference_impl import (                     # noqa: E402
    MIN_SCREENED_OBSERVABLES, admission_key, exempt_patch_touched,
    observables_probed_by, screen_reference, widening_targets)

P1B_LIVE = ROOT / 'runs-archive' / 'runs' / 'p1b_live_20260811_012407'


def _legs():
    return sorted(p for p in glob.glob(str(P1B_LIVE / '[0-9]*'))
                  if Path(p, 'result.jsonl').is_file())


def _leg_inputs(leg_dir):
    """`(code_context, [check sources])` exactly as the live widening saw
    them: the context the leg recorded, and every synthesised relation's
    check body in synthesis order (which IS the arrival order the enumeration
    ranks on)."""
    rec = json.load(open(Path(leg_dir, 'result.jsonl')))
    trace = open(Path(leg_dir, 'trace.md'), errors='ignore').read()
    checks = []
    for m in re.finditer(r'^\s*"check": (".*?")\s*$', trace, re.M):
        try:
            checks.append(json.loads(m.group(1)))
        except ValueError:                        # pragma: no cover
            pass
    return rec.get('code_context') or '', checks


def _patched_methods(trace_text):
    """The diff-derived touched functions, off the leg's own recorded
    `analysis (TargetAnalyzer)` event — the same list `_patched_method_names`
    hands the chain in production."""
    return list(dict.fromkeys(re.findall(r'"func_name": "([^"]+)"',
                                         trace_text)))


# ---------------------------------------------------------------------------
# G-R1 — the enumeration, replayed on the five live legs
# ---------------------------------------------------------------------------

def test_GR1_the_replay_still_reproduces_the_LIVE_target_lists_unscoped():
    """The replay is only evidence if its inputs are the live inputs. Under
    the UNSCOPED rule (Fix 1 disabled by using a context with no patched
    block) the reconstruction must return exactly what each leg requested."""
    legs = _legs()
    if not legs:
        pytest.skip('p1b_live archive not present')
    live = {'01': ['optimize', 'chisquare', 'valueref'],
            '02': ['optimize', 'chisquare', 'valueref'],
            '03': ['optimize', 'chisquare', 'pointref'],
            '04': ['add', 'maxmiddleindex', 'start'],
            '05': ['axis', 'drawlabel', 'rectangleedge']}
    seen = {}
    for leg in legs:
        ctx, checks = _leg_inputs(leg)
        # Strip the role markers: same context, scope unresolvable, so the
        # enumeration falls back to the pre-READ-2 list.
        unscoped = ctx.replace('role="patched"', 'role="was-patched"')
        targets, _why = widening_targets({}, checks, unscoped, 3)
        seen[Path(leg).name[:2]] = [admission_key(t) for t in targets]
    assert seen == live


def test_GR1_rms_enters_on_all_three_math_legs_with_the_cap_unchanged():
    legs = _legs()
    if not legs:
        pytest.skip('p1b_live archive not present')
    import config
    got = {}
    for leg in legs:
        ctx, checks = _leg_inputs(leg)
        targets, _why = widening_targets({}, checks, ctx,
                                         config.P1B_MAX_REFERENCES)
        got[Path(leg).name[:2]] = [admission_key(t) for t in targets]
    assert config.P1B_MAX_REFERENCES == 3
    # The doc's pinned table, verbatim.
    assert got['01'] == ['chisquare', 'rms', 'evaluations']
    assert got['02'] == ['chisquare', 'rms', 'covariances']
    assert got['03'] == ['chisquare', 'rms', 'covariances']
    assert got['04'] == ['maxmiddleindex', 'itemcount', 'minmiddleindex']
    assert got['05'] == ['visible']
    # No leg exceeds the cap (G-R6's request half, offline).
    assert all(len(v) <= config.P1B_MAX_REFERENCES for v in got.values())


def test_GR1_every_wasted_live_request_disappears():
    """`optimize` ×3, `add`, `Axis`, `RectangleEdge` — the five requests the
    live draw spent and the chain then refused, plus the type names."""
    legs = _legs()
    if not legs:
        pytest.skip('p1b_live archive not present')
    wasted = {'optimize', 'add', 'start', 'axis', 'drawlabel', 'rectangleedge',
              'levenbergmarquardtoptimizer', 'vectorialpointvaluepair',
              'functionevaluationexception', 'optimizationexception'}
    for leg in legs:
        ctx, checks = _leg_inputs(leg)
        excluded = []
        probed = observables_probed_by(checks, ctx, excluded=excluded)
        keys = {admission_key(p) for p in probed}
        assert not (keys & wasted), Path(leg).name
        # Nothing is dropped without a recorded reason.
        assert excluded and all(e['why'] for e in excluded)


# ---------------------------------------------------------------------------
# G-R2 — the screen, replayed on the archived `getRMS` discards
# ---------------------------------------------------------------------------

_ATTEMPT_RE = re.compile(
    r'## \[\d+\] ⚙️ reference-impl · `([^`]+)`\n'
    r'\*\*output:\*\* \*\*([^*]+)\*\*\n'
    r'(?:- reason: ([^\n]*)\n)?'
    r'(?:- detail: (\{[^\n]*\})\n)?')


def _rms_discards():
    """Every archived `getRMS` candidate the screen discarded for
    disagreeing with the buggy build on `getChiSquare`, with the surface it
    was screened on."""
    rows = []
    for trace in glob.glob(str(ROOT / 'runs-archive' / 'runs' / '*' / '*'
                               / 'trace.md')):
        text = open(trace, errors='ignore').read()
        touched = _patched_methods(text)
        surface = {}
        for target, output, reason, detail in _ATTEMPT_RE.findall(text):
            if output == 'screening surface resolved':
                m = re.search(r"'siblings': \[([^\]]*)\]", detail or '')
                surface[target] = re.findall(r"'([^']+)'", m.group(1)) if m \
                    else []
            if (target != 'getRMS' or output != 'screen DISCARDED'
                    or 'off-defect observable `getChiSquare`' not in
                    (reason or '')):
                continue
            shared = re.search(r"'off_defect_shared': (\d+)", detail or '')
            rows.append({'trace': trace, 'touched': touched,
                         'siblings': surface.get('getRMS', []),
                         'shared': int(shared.group(1)) if shared else -1})
    return rows


def test_GR2_the_defect_observable_leaves_the_screening_surface():
    """The prediction: not one of these candidates can be discarded for
    `getChiSquare` again, because `getChiSquare` is no longer screened."""
    rows = _rms_discards()
    if not rows:
        pytest.skip('runs archive not present')
    assert len(rows) == 11, len(rows)             # the doc's count, verbatim
    for row in rows:
        assert row['touched'] == ['getChiSquare'], row['trace']
        kept, exempted = exempt_patch_touched(row['siblings'], row['touched'])
        assert exempted == ['getChiSquare'], row['trace']
        assert 'getChiSquare' not in kept


def test_GR2_the_separate_finding_all_eleven_then_fail_the_COUNT_bar():
    """The other half of G-R2, written down before the live roll: every one
    of the eleven shared exactly 3 off-defect observables with the buggy
    build, so exempting the patch-touched one leaves 2 and the screen refuses
    on the count bar. Fix 2 alone admits NO rms reference on any archived
    leg. §2.3 predicted otherwise from the 7 declared siblings; the binding
    quantity is what the generated reference SHARES, not what the class
    declares."""
    rows = _rms_discards()
    if not rows:
        pytest.skip('runs archive not present')
    assert {r['shared'] for r in rows} == {3}
    remaining = [r['shared'] - 1 for r in rows]
    assert set(remaining) == {2}
    # What the screen says at that count, for the trace to carry.
    ok, why = screen_reference({'a': ['1'], 'b': ['2']},
                               {'a': ['1'], 'b': ['2']},
                               off_defect_keys={'a', 'b'})
    assert ok is False
    assert f'2 off-defect observable(s) shared; ' \
           f'{MIN_SCREENED_OBSERVABLES} required' in why


# The claim below is about the archive AS IT STOOD when the read-2 fix was
# written (commit e95a3ed): runs made BEFORE the fix went live. Runs from
# p1b_live2_20260811_023425 onward ran WITH the exemption live, so their
# admissions legitimately carry `patch_touched_exempted` surfaces (verified:
# p1b_live2 leg 02's `getJacobianEvaluations` admission — the exemption
# engaged and the reference was still admitted on 5 remaining siblings).
# Pin the test to the pre-fix set: every run_suite.sh run dir ends in
# `_YYYYMMDD_HHMMSS`; undated dirs all predate the cutoff and are kept.
_PRE_FIX_CUTOFF = '20260811_023425'


def _pre_fix_traces():
    for trace in sorted(glob.glob(str(ROOT / 'runs-archive' / 'runs' / '*'
                                      / '*' / 'trace.md'))):
        stamp = re.search(r'(\d{8}_\d{6})$', Path(trace).parents[1].name)
        if stamp and stamp.group(1) >= _PRE_FIX_CUTOFF:
            continue
        yield trace


def test_GR2_the_exemption_touches_no_other_archived_admission():
    """The safety half: 43 of the archive's 49 admissions sat exactly ON the
    count bar, so an exemption that reached them would delete them. It does
    not — every archived admission is either FOR the patched observable
    (already excluded from its own off-defect set) or on a different class,
    whose siblings the patch never touched. Pinned to the pre-fix archive
    (see `_PRE_FIX_CUTOFF`): the claim is about admissions made WITHOUT the
    exemption, and post-fix runs carry exempted surfaces by design."""
    rows = []
    for trace in _pre_fix_traces():
        text = open(trace, errors='ignore').read()
        touched = _patched_methods(text)
        surface = {}
        for target, output, _reason, detail in _ATTEMPT_RE.findall(text):
            if output == 'screening surface resolved':
                m = re.search(r"'siblings': \[([^\]]*)\]", detail or '')
                surface[target] = re.findall(r"'([^']+)'", m.group(1)) if m \
                    else []
            if output != 'screen ADMITTED':
                continue
            rows.append((trace, target, surface.get(target, []), touched))
    if not rows:
        pytest.skip('runs archive not present')
    for trace, target, siblings, touched in rows:
        _kept, exempted = exempt_patch_touched(siblings, touched)
        assert exempted == [], (trace, target, exempted)
