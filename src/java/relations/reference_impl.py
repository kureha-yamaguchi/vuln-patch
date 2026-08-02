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

from java.relations.evidence_facts import (_close, _method_body,
                                           _methods_named_by, observed_values)

# Divergence kinds that are NOT evidence of a real disagreement. Inherited from
# the certifier's classifier, which learned them the hard way: a last-ulp float
# difference and a generic-exception mismatch are noise on this comparison, and
# reporting them as disagreement manufactures both false screens and false
# facts.
WEAK_KINDS = ('value_ulp', 'exception_generic_latent')

# A reference must agree on at least this many observables before its agreement
# means anything. Two matching values could be two constants; the screen has to
# be able to fail.
MIN_SCREENED_OBSERVABLES = 3


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
    """
    if a == b:
        return True
    try:
        return _close(float(a), float(b))
    except (TypeError, ValueError):
        return False


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
