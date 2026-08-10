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
                                           _METHOD_CALL_RE,
                                           _methods_named_by,
                                           _UNINTERESTING_METHODS,
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


def disputed_observables(fired_msg: str, code_context: str,
                         check_source: Optional[str] = None) -> List[str]:
    """Methods the firing (or its check) names that the context DECLARES.

    STAGE 2 (Math-2, the first held-out bug): reach was ZERO, and the trace
    said why — the kept relation's check CALLS `dist.getNumericalMean()` and
    recomputes the documented mean (the exact dispute the leg exists for),
    but its fired MESSAGE prints only `actual=.. expected=..`, and the
    detector read only the message. The check SOURCE is the same artifact
    the judge already receives, so scanning it adds no new authority:
    methods it CALLS (exact call syntax, the narrow matcher — never
    substrings) join the candidates.

    OPTION A (user decision 2026-08-08): the filter is DECLARED IN THE
    CONTEXT, not body-shown. The body requirement was inherited from the
    quoting feature (`disputed_computation_fact`, which pastes the body
    into a judge prompt and KEEPS its own requirement) — but this chain
    never quotes the body to anyone: the generator is FORBIDDEN it
    (information rule), and the one internal consumer (`fields_read_by`,
    ordering nameless parameters) already degrades to [] without it. The
    old rule made the chain sit out every disputed != patched leg, because
    the context assembler elides non-patched bodies — Math-2's shape.
    The judge's prompts are untouched either way; a wrongly-started chain
    ends in a reasoned discard at the screen, never in wrong evidence.
    """
    if not fired_msg or not code_context:
        return []
    msg_names = list(_methods_named_by(fired_msg, code_context))
    called = []
    for n in _METHOD_CALL_RE.findall(str(check_source or '')):
        if n not in called and n not in _UNINTERESTING_METHODS:
            called.append(n)
    # ORDER IS ATTEMPTED ORDER (stage-4 roll 2: only disputed[0] was ever
    # tried, and on the SOFix leg both firings' position 0 was a stored-
    # field accessor named incidentally by the message — getNumericalMean,
    # in both lists, attempted in neither). Ranked by signal strength,
    # mechanically: (1) named by the MESSAGE and called by the CHECK — two
    # independent routes agreeing (Math-65's getChiSquare); (2) check-
    # called only, in call order — the check calls what it disputes
    # (Math-2's mean relation); (3) message-only last — words a firing
    # happens to print (sampleSize=50 -> getSampleSize) are the weakest
    # signal and produced both wasted attempts.
    both = [n for n in msg_names if n in called]
    check_only = [n for n in called if n not in msg_names]
    msg_only = [n for n in msg_names if n not in called]
    return [n for n in both + check_only + msg_only
            if _method_declared(code_context, n)]


#: Words that can directly precede a method CALL but never a declaration's
#: return type position: `return foo(x);` is a call, `double foo(x) {` is
#: a declaration.
_NOT_A_RETURN_TYPE = frozenset({
    'return', 'new', 'throw', 'else', 'case', 'break', 'continue', 'do',
    'while', 'if', 'assert', 'yield'})


def _method_declared(code_context, name) -> bool:
    """A DECLARATION for `name` exists in the context — signature visible;
    the body may be shown, elided (`{ … }`), or abstract (`;`). Absence
    still declines: a name the context does not declare is not this
    class's dispute.

    A CALL is not a declaration (stage 4, Math-30): `return 2 *
    standardNormal.cumulativeProbability(z);` ends in `);` and satisfied
    the old `[{;]` tail, registering a NormalDistribution method as
    declared by the patched class — one wasted generation per false
    trigger (fixture scope: 3,858 of 18,496 matches, 20.9%, were calls).
    A declaration's name is preceded by its return type — a type-ish
    token and whitespace — never by a receiver dot, and `return foo(x);`
    is excluded by keyword. Fail-closed held throughout (no fact can come
    of a false trigger); this fix is about spend and noise.
    """
    if not code_context or not name:
        return False
    pat = re.compile(
        r'([A-Za-z_$][\w$]*|[\]>])\s+' + re.escape(name)
        + r'\s*\([^)]*\)\s*(?:throws\s+[\w.$\s,]*?)?[{;]')
    for m in pat.finditer(str(code_context)):
        if m.group(1) not in _NOT_A_RETURN_TYPE:
            return True
    return False


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


def _pin_matches(printed: str, expected: str, tol) -> bool:
    """Any numeric element of `printed` within `tol` of `expected`.

    The failing test asserts on ELEMENTS (`errors[0]`), the twin prints the
    whole array, so the pin matches if any element lands inside the test's
    own tolerance. With no tolerance recovered, the rounding floor applies
    (stricter — fails closed toward not-corroborated).
    """
    try:
        e = float(expected)
        t = float(tol) if tol is not None else None
    except (TypeError, ValueError):
        return False
    for tok in re.findall(r'-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?',
                          _decode_java_literal(printed or '')):
        try:
            v = float(tok)
        except ValueError:
            continue
        if (abs(v - e) <= t) if t is not None else _close(v, e):
            return True
    return False


# ---------------------------------------------------------------------------
# THE ADMISSION STORE (p1b step 1 — docs/p1b-design-2026-08-11.md §3).
#
# What it replaces: admission was ONE attribute on the chain function,
# overwritten by every successful attempt, so a leg that admitted three
# references kept only the last. Measured in the archive: `stack_confirm/05`
# admitted `getChiSquare`, `getPointRef` and `getValueRef` and kept
# `getValueRef`; `mechb/09` has the same shape. The gate then compared a
# chi-square firing against a `getPoint` reference and abstained on "no shared
# observable" — 36 of the archive's 221 abstentions carry that reason.
#
# The store is a plain dict keyed by `observable_key(method)`, so `chiSquare`
# and `getChiSquare` are ONE slot — the same normalization the gate, the
# observable matcher and `_methods_named_by` have used since P0.
# ---------------------------------------------------------------------------

SAME_OBSERVABLE_RULE = 'keep-first'
NO_REFERENCE_FOR_THIS_OBSERVABLE = 'no-reference-for-this-observable'


def admission_key(method) -> str:
    """The store slot an observable name belongs to."""
    from java.relations.reference_run import observable_key
    return observable_key(method)


def admit_reference(store: Dict[str, dict], method: str, record: dict
                    ) -> Tuple[bool, str]:
    """Retain `record` under the observable it is a reference FOR.

    `(stored, why)`; `store` is mutated in place. Pure, never raises.

    SAME OBSERVABLE, SECOND REFERENCE -> KEEP-FIRST, and that is the
    conservative rule here for three reasons, none of them taste:

      * **Admission is binary, so "best" would have to be invented.** A
        record is in this store only because it passed the screen (>=3
        shared sibling observables reproduced against the buggy build), the
        corroboration attribution and the pin check. Nothing the chain
        computes ORDERS two references that all passed; picking by screened-
        sibling count or by arrival time would be a preference the screen
        does not express, applied to decide which implementation gets to
        void a conviction.
      * **Keep-first makes the store append-only.** A record never changes
        after it is written. The gate is called at BOTH judge doors,
        interleaved with further admissions, so under keep-last the same
        firing could read a different reference depending on WHEN it looked
        — a wrong answer that depends on door ordering is the hardest kind
        to see in a trace.
      * **The first admission is already the highest-ranked candidate.**
        `disputed_observables` ranks candidates by signal strength (named by
        the message AND called by the check first) and the chain attempts
        them in that order. Overwriting the first admitted reference with a
        later candidate's is the single-slot defect in miniature.

    The set-aside record is not silently dropped: the caller records the
    duplicate as its own event, so the class stays countable.
    """
    key = admission_key(method)
    prior = (store or {}).get(key)
    if prior is not None:
        return False, (
            f'`{key}` already has an admitted reference this leg (from '
            f'`{prior.get("method")}`) — KEEP-FIRST: the incumbent stands and '
            f'this reference from `{method}` is set aside. Admission is a '
            f'binary verdict, not a score, so nothing orders two admitted '
            f'references; keeping the first also keeps the store append-only, '
            f'which is what makes the two judge doors read the same reference '
            f'for the same observable')
    store[key] = record
    return True, (
        f'reference for `{key}` (from `{method}`) admitted and RETAINED under '
        f'its own observable; the leg now holds {sorted(store)}')


def admitted_reference_for(store, fired_msg, code_context,
                           check_source=None) -> Tuple[Optional[dict], str]:
    """The admitted record for the observable THIS firing disputes.

    `(record_or_None, why)`. Pure, never raises. Resolution reuses the
    chain's own detector — `disputed_observables(fired, ctx,
    check_source=...)`, normalized through `admission_key` — so the gate and
    the chain cannot disagree about what a firing is about.

    ONE BACK-COMPAT PASS-THROUGH, recorded rather than hidden. When the
    lookup misses and the leg admitted exactly ONE reference, that record is
    returned anyway, with a reason that says SUBSTITUTED. That is byte-for-
    byte what the single slot did, and it keeps this step a pure repair of
    the RETENTION defect: every archived single-admission leg reads exactly
    as it did before. The honest reading is
    `no-reference-for-this-observable` (design §3: "substituting a reference
    for a DIFFERENT observable is not a fallback; it is the current bug"),
    and the p1b gate build is where it becomes one — the substitution event
    exists so the coverage roll can count, before that, how often the honest
    lookup would have differed.
    """
    store = store or {}
    if not store:
        return None, 'no admitted reference for this leg'
    try:
        disputed = disputed_observables(fired_msg, code_context,
                                        check_source=check_source)
    except Exception:                            # pragma: no cover - defensive
        disputed = []
    for name in disputed:
        rec = store.get(admission_key(name))
        if rec is not None:
            return rec, (
                f'admitted reference for `{admission_key(name)}` — the '
                f'observable this firing itself disputes (from '
                f'`{rec.get("method")}`)')
    _asked = disputed[:4] or ['<none resolvable>']
    if len(store) == 1:
        (only_key, only_rec), = store.items()
        return only_rec, (
            f'SUBSTITUTED: this firing disputes {_asked} and the leg\'s one '
            f'admitted reference is for `{only_key}`. Passed through '
            f'unchanged — a single-admission leg reads byte-for-byte as it '
            f'did before the store was split by observable. The honest '
            f'reading is {NO_REFERENCE_FOR_THIS_OBSERVABLE}')
    return None, (
        f'{NO_REFERENCE_FOR_THIS_OBSERVABLE}: this firing disputes {_asked}; '
        f'this leg admitted {sorted(store)}. Substituting a reference for a '
        f'DIFFERENT observable is the defect, not a fallback')


def reference_verdict_gate(fired_msg, admitted, lookup_why=None
                           ) -> Tuple[str, str]:
    """8.25 phase 1: `('void'|'corroborate'|'abstain', why)` on a KEPT
    conviction. Deterministic — the judge is not consulted (roll 12: nine
    deliveries of the fact, zero engagements; fifth negative on the
    persuasion axis. The user's decision: stop persuading).

    `admitted` is ONE admission record — the caller looks it up by the
    firing's own observable (`admitted_reference_for`) and passes that
    lookup's reason as `lookup_why`, so a no-record abstention says WHICH
    observable went unmatched instead of the flat "no admitted reference for
    this leg". With a record in hand the reading is unchanged.

    THE PREDICATE NEEDS NO STATE RECOVERY. The firing message prints the
    observable values the relation fired ON (the 8.3/8.4 recorders), and an
    ADMITTED reference — screened on 3+ siblings, corroboration-attributed,
    pin-disciplined — computed its own values for the same observables at
    test state. Compare where they overlap (names normalized, chiSquare ==
    getChiSquare):

      * all shared observables agree AND at least one is DISCRIMINATING
        (the reference's value differs from the BUGGY build's at test
        state) -> VOID. The 16-digit coincidence on a discriminating
        value is itself the proof of same-state: a firing at any other
        input reproducing the reference's exact value is vanishingly
        unlikely, so the relation demonstrably fired on the very
        behaviour an independently validated implementation produces.
      * anything else -> ABSTAIN, with the reason split three ways: no
        admitted reference; no shared observable; or values differ /
        agree only on legacy behaviour.

    DISAGREEMENT IS DELIBERATELY NOT 'CORROBORATE' (dry-run finding,
    recorded before first live run): a firing at a DIFFERENT input
    legitimately produces different values, so disagreement with the
    reference's test-state values cannot distinguish "the patch is
    wrong" from "the state is different". The corroboration side of the
    both-signs design needs the reference evaluated AT the firing's own
    input (the 8.4 firing-state extension) — asymmetry is the honest
    phase-1 shape: void is self-proving, corroborate is not.

    Sound against the overfit trap by construction: a genuine catch fires
    at values the correct implementation does NOT produce, so it can only
    ever be abstained on, never voided. No admitted reference -> abstain:
    the gate consumes admission, never manufactures it.
    """
    from java.relations.reference_run import observable_key
    obs = (admitted or {}).get('obs') or {}
    buggy = (admitted or {}).get('buggy') or {}
    if not fired_msg or not obs:
        # The lookup's own reason survives ONLY where there is no record to
        # read; a firing with no message abstains in today's words exactly.
        return 'abstain', ((lookup_why if (lookup_why and not obs) else None)
                           or 'no admitted reference for this leg')
    # Harness firings echo the method CALL (`getChiSquare()=6.25...`), and
    # the k=v parser requires a bare identifier before `=` — ladder1g's
    # second patched firing carried the reference's exact value behind that
    # `()` and read as no-overlap. Normalized HERE, not in the global
    # parser: the 8.3 recorder's semantics stay untouched.
    fired_vals = observed_values(re.sub(r'\(\s*\)\s*=', '=',
                                        str(fired_msg)))
    # Formula echoes (`sum((t-v)^2 / w[i])=...`) are not parseable keys, so
    # the k=v parser swallows them INTO the preceding value ("6.25 sum((...")
    # and a matching number reads as a mismatch. A scalar value is one
    # token; arrays keep their bracketed form.
    fired_vals = {k: [v if v.startswith('[') else v.split()[0]
                      for v in vs]
                  for k, vs in fired_vals.items()}
    ref_by_key = {observable_key(k): (k, v) for k, v in obs.items()}
    shared = [(fk, ref_by_key[observable_key(fk)])
              for fk in fired_vals if observable_key(fk) in ref_by_key]
    if not shared:
        return 'abstain', ('the firing reports no observable the admitted '
                           'reference computes')
    disagree = [fk for fk, (_rk, rv) in shared
                if not any(_values_agree(a, b)
                           for a in fired_vals[fk] for b in rv)]
    if disagree:
        return 'abstain', (
            f'fired values differ from the reference\'s TEST-STATE values '
            f'on {disagree[:4]} — a firing at a different input is expected '
            f'to differ, so same-state cannot be established and this says '
            f'nothing about the patch (corroboration needs the reference '
            f'evaluated at the firing\'s own input — the 8.4 extension)')
    discriminating = [
        fk for fk, (rk, rv) in shared
        if rk in buggy and not any(_values_agree(a, b)
                                   for a in rv for b in buggy[rk])]
    if not discriminating:
        return 'abstain', (
            f'fired values match the reference only on observables where '
            f'buggy and reference coincide ({[fk for fk, _ in shared][:4]}) '
            f'— shared legacy behaviour vouches for nothing about the '
            f'dispute')
    return 'void', (
        f'the relation fired on exactly the values the admitted reference '
        f'computes ({[fk for fk, _ in shared][:4]}), including '
        f'{discriminating[:3]} where reference and buggy DIFFER — an '
        f'implementation derived from the documentation alone, validated '
        f'on {len(obs)} observables, produces the very behaviour this '
        f'relation condemns; the relation\'s contract is wrong, the '
        f'conviction is VOID')


def _assert_tolerance(expected: str, test_sources) -> Optional[str]:
    """The test's own assertEquals tolerance for `expected`, or None."""
    for src in test_sources or []:
        t = re.search(r'assertEquals\(\s*' + re.escape(expected)
                      + r'\s*,[^,()]+,\s*([0-9.eE+-]+)\s*\)', src or '')
        if t:
            return t.group(1)
    return None


def pins_for_disputed(method, failure_messages, test_sources, buggy_obs
                      ) -> Dict[str, List[Tuple[str, Optional[str]]]]:
    """Pin material that GENUINELY attaches to the disputed observable.

    Roll 11 (defect 19, the first inside the mechanism's judgement rather
    than its plumbing): the chain mapped EVERY trusted test literal onto the
    disputed observable by construction, so a reference that correctly
    diverged on-defect was discarded against a literal from a NEIGHBOURING
    assertion — 1.768262623567235 is asserted against
    `Math.sqrt(circle.getN()) * rms`, an RMS line, not a getChiSquare value.
    The silent-wrong-comparison class, one layer in.

    Validator 3 now uses the SAME attribution discipline as corroboration
    pins. A pin attaches to the disputed observable only via:
      (a) the failure message's expected value, when its observed
          (`but was:<...>`) value appears VERBATIM in the twin's buggy
          print for the disputed observable — the state identity rule; or
      (b) an assertion whose actual-value expression calls the disputed
          method DIRECTLY: `assert*(<literal>, <...method(...)...>[, tol])`.
    Each pin carries the test's own tolerance where recoverable. No
    attribution -> no pin -> the pin check ABSTAINS, stated not silent.
    """
    pins: List[Tuple[str, Optional[str]]] = []
    for msg in failure_messages or []:
        m = re.search(r'expected:<([^>]+)> but was:<([^>]+)>', msg or '')
        if not m:
            continue
        exp, was = m.group(1).strip(), m.group(2).strip()
        if any(was in str(v) for v in (buggy_obs or {}).get(method, [])):
            pins.append((exp, _assert_tolerance(exp, test_sources)))
    for src in test_sources or []:
        for m in re.finditer(
                r'assert\w*\(\s*(-?[\d.][\deE.+-]*)\s*,'
                r'\s*[^,;]*\b' + re.escape(method) + r'\s*\([^)]*\)[^,;]*'
                r'(?:,\s*(-?[\d.][\deE.+-]*)\s*)?\)', src or ''):
            pins.append((m.group(1), m.group(2)))
    return {method: pins} if pins else {}


def test_corroboration_pins(failure_messages, test_sources, buggy_obs,
                            siblings) -> Dict[str, Tuple[str, Optional[str]]]:
    """`{sibling: (expected, tolerance)}` — mechanical attribution only.

    OPTION B's evidence source (user decision 2026-08-07). A sibling the
    defect REACHES is a rigged screen question: the buggy build is the wrong
    answer key there, and the failing test's own asserted literal is the
    right one. The pin attaches to a sibling ONLY when the failure message's
    observed value (`but was:<...>`) appears verbatim inside the twin's
    buggy print for that sibling — the same character-level identity that
    proves the twin stands at the failing assertion's state (re-walk #7).
    The tolerance is recovered from the test source's
    `assertEquals(expected, ..., tol)` literal; unrecoverable tolerance
    stays None and the stricter rounding floor applies downstream.
    """
    out = {}
    for msg in failure_messages or []:
        m = re.search(r'expected:<([^>]+)> but was:<([^>]+)>', msg or '')
        if not m:
            continue
        exp, was = m.group(1).strip(), m.group(2).strip()
        key = next((k for k in (siblings or [])
                    if any(was in str(v) for v in buggy_obs.get(k, []))),
                   None)
        if not key:
            continue
        tol = None
        for src in test_sources or []:
            t = re.search(r'assertEquals\(\s*' + re.escape(exp)
                          + r'\s*,[^,()]+,\s*([0-9.eE+-]+)\s*\)', src or '')
            if t:
                tol = t.group(1)
                break
        out[key] = (exp, tol)
    return out


def screen_reference(reference_obs: Dict[str, List[str]],
                     buggy_obs: Dict[str, List[str]],
                     off_defect_keys,
                     divergence_kinds: Optional[Dict[str, str]] = None,
                     test_corroboration: Optional[
                         Dict[str, Tuple[str, Optional[str]]]] = None
                     ) -> Tuple[bool, str]:
    """THE AUTHORITY SCREEN. `(admissible, reason)`.

    `off_defect_keys` are the observables the family-duty boundary says the
    defect does not touch. Only those are screened — screening ON the defect
    would require the reference to reproduce the BUG, which is backwards.

    OPTION B (user decision 2026-08-07, from re-walk #8's read): the sibling
    surface can contain an observable the defect REACHES — Math-65's
    `guessParametersErrors` is the very value the failing test asserts on —
    and there the buggy build is the wrong answer key, so exact-match
    grading fails the reference for being right. `test_corroboration`
    carries the failing test's own asserted literals for such siblings, and
    a disagreement with buggy is RE-GRADED as a pass ONLY when BOTH hold:
    the reference matches the test's expected value within the test's own
    tolerance, AND the buggy build fails that same pin. The second
    condition contains the open-book concern (the generator sees the test):
    the test's answer overrides the buggy build only where the buggy build
    is demonstrably the one that is wrong.

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
    corroborated = []
    for k in shared:
        if kinds.get(k) in WEAK_KINDS:
            continue                     # noise, not disagreement
        rv, bv = reference_obs[k], buggy_obs[k]
        if not any(_values_agree(a, b) for a in rv for b in bv):
            pin = (test_corroboration or {}).get(k)
            if pin:
                exp, tol = pin
                if (any(_pin_matches(a, exp, tol) for a in rv)
                        and not any(_pin_matches(b, exp, tol) for b in bv)):
                    corroborated.append(k)
                    continue
            return False, (f'reference disagrees with the buggy build on '
                           f'off-defect observable `{k}` '
                           f'({rv[:1]} vs {bv[:1]}) — DISCARDED')
    why = (f'reference reproduces the buggy build on {len(shared)} '
           f'off-defect observable(s)')
    if corroborated:
        why += (f' ({len(corroborated)} of them defect-reached and re-graded '
                f'against the failing test\'s own asserted value, which the '
                f'reference matches and the buggy build fails: '
                f'{corroborated})')
    return True, why


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
        pinned = list(pinned) if isinstance(pinned, (list, tuple)) else [pinned]
        # A pin may be a bare value (compared on the rounding floor) or an
        # attributed `(value, tolerance)` pair from `pins_for_disputed` —
        # then the TEST'S OWN tolerance governs, not our floor (roll 11:
        # the test's slack is part of what the test pins).
        def _pin_ok(a, p):
            if isinstance(p, (list, tuple)):
                return _pin_matches(a, p[0], p[1] if len(p) > 1 else None)
            return _values_agree(a, p)
        if not any(_pin_ok(a, p) for a in rv for p in pinned):
            return False, (
                'reference contradicts the failing test\'s PINNED answer on '
                '`' + str(k) + '` (' + repr(rv[:1]) + ' vs ' + repr(pinned[:1])
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
