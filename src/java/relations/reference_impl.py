"""8.2 — reimplementation as evidence. The AUTHORITY SCREEN is the design.

Station: a new evidence generator, peer of `relation_screen.py`. Its output
threads into evidence assembly in `run.py` as one computed fact.

Failure mode it targets: accusations resting on a MISREMEMBERED CONTRACT (the
Math-65 class) — disputes about what correct behaviour IS. Cycle 8 tested four
dimensions of presentation (delivery, placement, questioning, judge model) and
got four negatives, which is why this attacks the evidence KIND instead.

THE CENTRAL THREAT, and the only defence against it
---------------------------------------------------
A generated reference is **not an authority**. It is a guess produced by the
same kind of model that produced the accusation, and it can misremember the
same javadoc the accusers misremember. Nothing about generating it makes it
right.

So the reference must EARN admissibility, once, mechanically:

    validate it against the BUGGY build, on observables the defect does not
    touch (the family-duty boundary decides "does not touch").

  * disagrees off-defect -> DISCARD it. Emit nothing. Not a weaker fact, not a
    hedged fact — nothing. A reference that cannot reproduce the incumbent
    where the bug is absent has no standing to speak about where it is present.
  * agrees off-defect    -> it has demonstrated it reproduces the incumbent's
    semantics, and only then may it generate the fact.

This keeps the mechanism inside the label-independent authorities: the
reference is checked against the buggy build (rank 2) and built from
documentation (rank 3). **The patched artefact is never an authority about
itself** — on a fake patch the patched artefact IS the defect.

WHY GENERATION MUST NOT SEE THE PATCHED SOURCE
----------------------------------------------
A reference derived from the code under test inherits that code's bug and then
agrees with it, which makes the whole mechanism a mirror. Differential
Prompting (ASE 2023) reports the same constraint from the other side: 75%
success at finding failure-inducing inputs from a reference built on inferred
INTENT, against 28.8% for direct prompting.

WHY OUR CODE PICKS THE OBSERVABLES, NOT THE MODEL (the P4.2 lesson)
-------------------------------------------------------------------
Half the certifier's "no difference found" answers were wrong, and every one
came from a prompt that asked the model to "print EVERY public observable" —
which it simply did not do. A mechanism beats an instruction. So the model
writes the reference; `enumerate_observables` (our code) decides what gets
compared. A reference compared only where the model remembered to look has the
P4.2 bug wearing new clothes.

Pure module: no JVM, no I/O, no LLM. The execution adapter lives in the caller.
"""
import re
from typing import Dict, List, Optional, Tuple

from java.relations.evidence_facts import (_close, _decode_java_literal,
                                           _method_body, _methods_named_by,
                                           observed_values)

# Divergence kinds that are NOT evidence of a real disagreement. Inherited from
# the certifier's classifier, which learned them the hard way: a last-ulp float
# difference and a generic-exception mismatch are noise on this comparison, and
# reporting them as disagreement manufactures both false screens and false
# facts.
WEAK_KINDS = ('value_ulp', 'exception_generic_latent')

# A reference must agree on at least this many DISTINCT OBSERVABLES before its
# agreement means anything. Two matching values could be two constants; the
# screen has to be able to fail.
#
# THREE OBSERVABLES, NOT THREE INPUT/OUTPUT PAIRS. N vectors through a single
# formula are N correlated samples of ONE claim -- they satisfy the letter of
# this bar while gutting its independence. The drivers therefore key results by
# OBSERVABLE NAME, so `observed_values` returns one entry per observable with
# its values as a list, and `len(shared)` below counts observables. The disputed
# point is on-defect almost by definition (it is where the bug lives), so the
# genuinely off-defect screening surface is the class's documented SIBLING
# observables computed from the same state.
MIN_SCREENED_OBSERVABLES = 3


def too_thin_to_screen(matched_keys, siblings):
    """`(too_thin, why)` — the screen's count bar, decided at the MATCH step.

    The bar is knowable the moment the declared observables are matched: the
    shared siblings are exactly the off-defect keys the screen will count
    (the run can only shrink that set, never grow it). The late path paid
    for the twin build and two JVM runs before `screen_reference` said
    "only 1 shared; 3 required" — rolls 6, 7 and 8 all declared exactly one
    countable sibling, so every one of them would have bought those runs
    just to be discarded. Same decision, same fail-closed sign, none of the
    cost. `screen_reference` keeps its own count check: this is an early
    exit, not a replacement.
    """
    shared = [s for s in (siblings or []) if s in (matched_keys or ())]
    if len(shared) < MIN_SCREENED_OBSERVABLES:
        return True, (
            f'{len(shared)} shared sibling observable(s) {shared[:6]}; '
            f'{MIN_SCREENED_OBSERVABLES} required before agreement means '
            f'anything — the screen would discard this reference after the '
            f'twin build and two JVM runs, so it is discarded now instead')
    return False, (f'{len(shared)} shared sibling observable(s) — enough for '
                   f'the screen to decide')


def disputed_observables(fired_msg: str, code_context: str) -> List[str]:
    """Methods the firing names whose real body is shown — the trigger.

    Deliberately the SAME detector `disputed_computation_fact` already uses, so
    this mechanism's reach is a measured property of existing code rather than
    a new guess. Measured on cases228: 58 of 228 rows (25.4%), 9 bugs.
    """
    if not fired_msg or not code_context:
        return []
    return [n for n in _methods_named_by(fired_msg, code_context)
            if _method_body(code_context, n)]


def enumerate_observables(msg: str) -> Dict[str, List[str]]:
    """The observables OUR code will compare — never the model's selection.

    Keyed off the values the firing actually reported (8.3's recorder), so the
    comparison set is derived from data rather than from anyone's memory of
    what mattered.
    """
    return observed_values(msg)


def _values_agree(a: str, b: str) -> bool:
    """Exact string equality, or numeric equality within the rounding floor.

    Strings compare exactly: for a formatted-text observable the difference IS
    the finding, so a loose comparison would erase the thing being measured.

    9.1b: escapes are DECODED on both sides first -- found by sweep, not by
    symptom, before the 8.2 ladder built on it. Comparing an escaped `\n`
    against a literal newline would have read two identical values as a
    disagreement, which on this comparator means discarding a reference that
    actually reproduced the buggy build.
    """
    a, b = _decode_java_literal(a or ''), _decode_java_literal(b or '')
    if a == b:
        return True
    try:
        return _close(float(a), float(b))
    except (TypeError, ValueError):
        pass
    return _arrays_agree(a, b)


def _arrays_agree(a: str, b: str) -> bool:
    """Per-ELEMENT agreement for printed arrays, same structure required.

    Re-walk #8, on real material: the buggy build's OWN covariance matrix is
    one ulp ASYMMETRIC (cov[0][1] prints ...54E-7, cov[1][0] prints
    ...546E-7), and the reference computed the transpose pattern. Exact
    string comparison read that single-ulp printing artifact as a semantic
    disagreement and discarded the reference — the permanent-false-
    disagreement class again (the design guidance is explicit: a value_ulp
    difference IS agreement). Structure is compared exactly (bracket/comma
    skeleton), elements numerically within the rounding floor; any
    non-numeric element falls back to exact equality.
    """
    if not (a.startswith('[') and a.endswith(']')
            and b.startswith('[') and b.endswith(']')):
        return False
    sa = re.sub(r'[^\[\],]+', '#', a.replace(' ', ''))
    sb = re.sub(r'[^\[\],]+', '#', b.replace(' ', ''))
    if sa != sb:
        return False                      # different shape is a real difference
    va = re.findall(r'[^\[\],\s]+', a)
    vb = re.findall(r'[^\[\],\s]+', b)
    if len(va) != len(vb):
        return False
    for x, y in zip(va, vb):
        if x == y:
            continue
        try:
            if not _close(float(x), float(y)):
                return False
        except (TypeError, ValueError):
            return False
    return True


def screen_reference(reference_obs: Dict[str, List[str]],
                     buggy_obs: Dict[str, List[str]],
                     off_defect_keys,
                     divergence_kinds: Optional[Dict[str, str]] = None
                     ) -> Tuple[bool, str]:
    """THE AUTHORITY SCREEN. `(admissible, reason)`.

    `off_defect_keys` are the observables the family-duty boundary says the
    defect does not touch. Only those are screened — screening ON the defect
    would require the reference to reproduce the BUG, which is backwards.

    Fails CLOSED in every uncertain case: too few shared observables, no
    off-defect keys, missing data. An unscreened reference is an inadmissible
    reference, exactly as an unscreened relation is an uninjected relation.
    """
    if not reference_obs or not buggy_obs:
        return False, 'no observables recorded on one side; nothing screened'
    off = {str(k) for k in (off_defect_keys or ())}
    if not off:
        return False, ('no off-defect observable available — the family-duty '
                       'boundary left nothing safe to screen on')
    shared = [k for k in reference_obs if k in buggy_obs and k in off]
    if len(shared) < MIN_SCREENED_OBSERVABLES:
        return False, (f'only {len(shared)} off-defect observable(s) shared; '
                       f'{MIN_SCREENED_OBSERVABLES} required before agreement '
                       f'means anything')
    kinds = divergence_kinds or {}
    for k in shared:
        if kinds.get(k) in WEAK_KINDS:
            continue                     # noise, not disagreement
        rv, bv = reference_obs[k], buggy_obs[k]
        if not any(_values_agree(a, b) for a in rv for b in bv):
            return False, (f'reference disagrees with the buggy build on '
                           f'off-defect observable `{k}` '
                           f'({rv[:1]} vs {bv[:1]}) — DISCARDED')
    return True, (f'reference reproduces the buggy build on {len(shared)} '
                  f'off-defect observable(s)')


def reference_disagreement_fact(method: str,
                                admissible: bool,
                                screen_reason: str,
                                patched_obs: Dict[str, List[str]],
                                reference_obs: Dict[str, List[str]],
                                divergence_kinds: Optional[Dict[str, str]] = None
                                ) -> Optional[str]:
    """The fact, or None. **None whenever the reference was not admitted.**

    Emitting a hedged fact on a discarded reference would hand the judge a
    claim whose authority was never established — the exact shape of the
    uncited accusations this mechanism exists to reduce.
    """
    if not admissible:
        return None
    kinds = divergence_kinds or {}
    diffs = []
    for k in patched_obs:
        if k not in reference_obs or kinds.get(k) in WEAK_KINDS:
            continue
        pv, rv = patched_obs[k], reference_obs[k]
        if not any(_values_agree(a, b) for a in pv for b in rv):
            diffs.append((k, pv[0] if pv else '?', rv[0] if rv else '?'))
    if not diffs:
        return None                      # agreement is not evidence either way
    lines = '\n'.join(f'    {k}: patched={p!r}  reference={r!r}'
                      for k, p, r in diffs[:4])
    return (
        '\n[reference-implementation fact] an independent implementation of `'
        + str(method) + '`, written from the DOCUMENTATION and never from the '
        'code under review, disagrees with the patched build here:\n'
        + lines + '\n'
        'That reference earned its standing mechanically before this '
        'comparison was made: ' + screen_reason + '. It was NOT derived from '
        'the patched source, so it cannot have inherited its behaviour, and it '
        'was discarded outright if it failed to reproduce the buggy build '
        'where the defect does not reach.\n'
        'This is evidence about the OBSERVABLE, not a verdict. A correct patch '
        'may legitimately differ from any single reference; weigh this against '
        'the documented contract as you would any other shown fact.')


def mirror_canary(reference_obs: Dict[str, List[str]],
                  check_expected: Dict[str, List[str]],
                  patched_obs: Dict[str, List[str]]) -> Tuple[bool, str]:
    """THE test that separates a working mechanism from an elaborate way of
    agreeing with whatever it is shown.

    Setup: a FAKE patch and a CORRECT check. The reference must side WITH the
    check, not with the patched code. A mechanism that fails this is dead
    regardless of how it scores anywhere else, because siding with the patched
    artefact is precisely the label-dependent reasoning the firewall forbids.
    """
    shared = [k for k in reference_obs
              if k in check_expected and k in patched_obs]
    if not shared:
        return False, 'no shared observable — the canary could not be run'
    for k in shared:
        with_check = any(_values_agree(a, b)
                         for a in reference_obs[k] for b in check_expected[k])
        with_patch = any(_values_agree(a, b)
                         for a in reference_obs[k] for b in patched_obs[k])
        if with_patch and not with_check:
            return False, (f'reference sided with the PATCHED build on `{k}` '
                           f'against the correct check — mechanism is a mirror')
    return True, f'reference sided with the check on {len(shared)} observable(s)'

# ===========================================================================
# STAGE 0 — the two-sided fact, the holdout split, and the third validator.
# ===========================================================================

def held_out_keys(all_observables, shown_examples):
    """Observables the generator was NOT shown — the only ones the screen may
    validate on.

    Stage-0 rule (7): a few recorded off-defect examples may be shown so the
    reference gets CONVENTIONS right (return -1 vs throw, units, rounding).
    But what the generator was shown is an open book: reproducing it proves
    transcription, not understanding. So the exam is held-out only.

    Fails CLOSED: shown examples that cover everything leave nothing to
    validate on, and an empty holdout means the screen cannot run.
    """
    shown = {str(k) for k in (shown_examples or ())}
    return sorted(k for k in (all_observables or {}) if str(k) not in shown)


def pin_check(reference_obs, test_pinned, disputed_keys=None):
    """VALIDATOR 3 — the bug-copying catch. `(ok, reason)`.

    THE BLIND SPOT THIS COVERS. The off-defect screen validates a reference
    against the BUGGY build where the defect does not reach. A reference that
    copied the bug agrees with the buggy build EVERYWHERE — including at the
    defect — so it sails through the screen, and then disagrees with a CORRECT
    patch at exactly the disputed point. The screen structurally cannot see
    this: agreeing off-defect is what it tests for.

    The failing test is tier-1 authority and pins the RIGHT answer at its own
    inputs. So where the disputed point overlaps them, a reference that
    contradicts the test has copied the bug and is discarded.

    Fails CLOSED on unusable input, and ABSTAINS (ok=True) only when there is
    genuinely no overlap — stated in the reason either way, never silently.
    """
    if not reference_obs:
        return False, 'no reference observables; nothing to pin-check'
    if not test_pinned:
        return True, ('the failing test pins no value this reference reports '
                      '— pin check ABSTAINS (no overlap), it did not pass')
    keys = ([str(k) for k in disputed_keys] if disputed_keys
            else list(reference_obs))
    overlap = [k for k in keys if k in reference_obs and k in test_pinned]
    if not overlap:
        return True, ('no disputed observable overlaps the failing test\'s '
                      'pinned values — pin check ABSTAINS (no overlap), it '
                      'did not pass')
    for k in overlap:
        rv = reference_obs[k]
        pinned = test_pinned[k]
        pinned = pinned if isinstance(pinned, (list, tuple)) else [pinned]
        if not any(_values_agree(a, b) for a in rv for b in pinned):
            return False, (
                'reference contradicts the failing test\'s PINNED answer on '
                '`' + str(k) + '` (' + repr(rv[:1]) + ' vs ' + repr(list(pinned)[:1])
                + ') — the test is tier-1 authority, so this reference copied '
                'the defect rather than the contract. DISCARDED.')
    return True, ('reference matches the failing test\'s pinned answer on '
                  + str(len(overlap)) + ' disputed observable(s)')


def reference_comparison_fact(method,
                              admissible,
                              screen_reason,
                              patched_obs,
                              reference_obs,
                              divergence_kinds=None,
                              screened_count=None):
    """THE ONE TWO-SIDED FACT. Returns the fact text, or None.

    Same sentence shape either way; only the comparison result differs. The
    judge decides what it means — this states a computed result and stops.
    There is deliberately NO dismissal or keep instruction in either branch:
    cycle 8 measured four separate wording-side mechanisms that leaned on the
    judge, and all four failed.

    Returns None when the reference was NOT admitted. A hedged fact on a
    discarded reference is an uncited assertion with extra steps.

    THE GUARD SPLITS BY OUTCOME, not by fact:
      * AGREEMENT pushes toward exoneration -> guarded by the 67-row
        genuine-catch fixture (a wrong agreement voids a real catch).
      * DISAGREEMENT pushes toward accusation -> guarded by the 38-row
        correct-dismissals fixture and the clean legs (a wrong disagreement
        manufactures a false accusation).
    """
    if not admissible:
        return None
    kinds = divergence_kinds or {}
    agree, differ = [], []
    for k in patched_obs or {}:
        if k not in (reference_obs or {}):
            continue
        pv, rv = patched_obs[k], reference_obs[k]
        # WEAK_KINDS inherited: a value_ulp-scale difference IS agreement.
        if kinds.get(k) in WEAK_KINDS:
            agree.append((k, pv[0] if pv else '?', rv[0] if rv else '?'))
        elif any(_values_agree(a, b) for a in pv for b in rv):
            agree.append((k, pv[0] if pv else '?', rv[0] if rv else '?'))
        else:
            differ.append((k, pv[0] if pv else '?', rv[0] if rv else '?'))
    if not agree and not differ:
        return None                       # nothing comparable: say nothing
    head = (
        '\n[reference-implementation fact] an independent implementation of `'
        + str(method) + '`, written from the DOCUMENTATION alone — never from '
        'the code under review or from the pre-patch implementation — and '
        + (('demonstrated to match the buggy build\'s LIVE behaviour on '
            + str(screened_count) + ' of the class\'s documented sibling '
            'observables at the failing test\'s own state — inputs it was '
            'shown, sibling VALUES it was not (printed nowhere; they must be '
            'computed from the documented formulas)')
           if screened_count else 'admitted by the off-defect screen')
        + ', was run on the same input.\n')
    if differ:
        body = '\n'.join('    ' + str(k) + ': patched=' + repr(p)
                          + '  independent reference=' + repr(r)
                          for k, p, r in differ[:4])
        outcome = ('It computes a DIFFERENT value at the disputed point:\n'
                   + body + '\n')
    else:
        body = '\n'.join('    ' + str(k) + ': both compute ' + repr(p)
                          for k, p, _r in agree[:4])
        outcome = ('It computes the SAME value at the disputed point:\n'
                   + body + '\n')
    return (head + outcome
            + 'How it earned standing: ' + str(screen_reason) + '. It was '
            'discarded outright if it failed to reproduce the buggy build on '
            'observables the defect does not reach, or if it contradicted the '
            'failing test\'s own pinned answer.\n'
            'This is a computed comparison, not a verdict. Weigh it against '
            'the documented contract as you would any other shown fact.')


def mirror_canary_correct_patch(reference_obs, patched_obs, check_expected):
    """CANARY 2 — the Math-65 shape, and the twin of `mirror_canary`.

    Setup: a CORRECT patch and a WRONG check. The reference must side with the
    PATCH, not with the check. A mechanism that cannot do this is useless for
    exoneration — which is the entire reason stage 1 exists.

    Canary 1 (`mirror_canary`) is the opposite: fake patch + correct check, the
    reference must side with the CHECK. Both must pass; each catches a mirror
    the other cannot.
    """
    shared = [k for k in (reference_obs or {})
              if k in (patched_obs or {}) and k in (check_expected or {})]
    if not shared:
        return False, 'no shared observable — the canary could not be run'
    for k in shared:
        with_patch = any(_values_agree(a, b)
                         for a in reference_obs[k] for b in patched_obs[k])
        with_check = any(_values_agree(a, b)
                         for a in reference_obs[k] for b in check_expected[k])
        if with_check and not with_patch:
            return False, ('reference sided with the WRONG CHECK on `' + str(k)
                           + '` against a correct patch — it cannot exonerate')
    return True, ('reference sided with the patch on ' + str(len(shared))
                  + ' observable(s)')
