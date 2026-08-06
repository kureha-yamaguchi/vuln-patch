"""Single shared judge-decision entrypoint (cycle-5 guards).

The full shipped soundness decision — base verify, the 5B recall-side
dismissal lint (void-and-re-ask), and the 5C precision-side terminal
identical gate — used to live inline in ``run.py`` as two helpers called at
each of the two judge sites, while the offline ``verifier_replay.py`` harness
called ``verifier.verify()`` RAW, past every guard. The replay therefore
measured a decision the pipeline no longer makes.

This module makes ``adjudicate`` the ONE decision both callers route through:

    run.py  judge sites ─┐
                         ├─► adjudicate(...) ─► verify → 5B → 5C ─► (ok, why)
    verifier_replay.py ──┘

``run.py`` never calls ``verifier.verify()`` again (grep-provable); the two
guard bodies moved here verbatim as internal helpers (``_guarded_verify``,
``_terminal_identical_gate``) so run.py's verdicts are byte-identical and the
fail-open behaviour is preserved unchanged.
"""
import re


# ---------------------------------------------------------------------------
# Permanent audit trail for the cycle-6 decisions.
#
# `run_suite.sh` DELETES `run.log` on a successful leg, so a `print` diagnostic
# cannot survive a green run: `trace.md` is built ONLY from `record_event`
# output (llm.get_events -> run.py::_write_trace_md). night20c therefore could
# not tell "6B ran and found nothing" from "6B never ran" — see
# docs/replay/night20c_analysis.md. Every cycle-6 decision below emits TWO
# events: one where it is CONSIDERED (proving the code path executed at all)
# and one where it DECIDES. The prints are kept: they are still useful on a
# FAILED leg, where run.log survives.
#
# Never raises into the pipeline: `record_event` already swallows its own
# errors, and the import + call are wrapped again here so a stubbed or broken
# recorder can never turn a judge decision into an exception.
# ---------------------------------------------------------------------------

def _ev(method, target=None, output=None, reason=None):
    """Record one cycle-6 audit event. Fail-silent by construction."""
    try:
        from llm import record_event
        record_event('deterministic', method=method,
                     target=('' if target is None else str(target)),
                     output=('' if output is None else str(output)),
                     reason=('' if reason is None else str(reason)))
    except Exception:  # pragma: no cover - defensive
        pass


def _target_of(fired):
    """A short, stable label for the firing under judgement: its oracle id when
    the fired message carries one, else the message's first line."""
    try:
        text = str(fired or '')
        m = re.search(r"\[oracle:([^\]]+)\]", text)
        if m:
            return m.group(1)
        first = next((ln.strip() for ln in text.splitlines() if ln.strip()), '')
        return first[:60] or 'firing'
    except Exception:  # pragma: no cover - defensive
        return 'firing'


def _guarded_verify(verifier, verify_kwargs, pinned=None,
                    evidence_profile=None):
    """Cycle-5B recall-side dismissal lint. Run the soundness judge, and when
    it returns UNSOUND on a VOID ground, re-ask it ONCE with the void made
    explicit:

      (i) 5B(i) — the dismissal varies a parameter the check's own source
          PINS (pinned_parameters); re-ask stating the pin.
      (ii) 5B(ii) — under the drift-kill signature (`evidence_profile`) the
          dismissal is an uncited "a correct implementation could..."
          hypothetical; re-ask demanding a citation.

    Cycle-5D completes 5B(ii)'s STATED semantics. The rule says an uncited
    hypothetical is INADMISSIBLE under the drift-kill signature; the code used
    to re-ask once and then accept whatever came back, including a SECOND
    uncited hypothetical — so the inadmissible verdict still killed the
    finding. Now: under the FULL signature, when the re-asked dismissal is
    citation-void AGAIN, the dismissal does not stand and the finding is KEPT
    with an explicit `5B: dismissal inadmissible` why. This applies ONLY on the
    citation-void path (which by construction requires all three signature
    flags); the pin-void path is untouched.

    Cycle-5 ITERATION 3 replaces 5B(ii)'s keyword-based citation detector with
    a STRUCTURAL one: the answer format now demands a third line,
    ``CITATION: "<verbatim quote>" | NONE``, and the quote is checked
    MECHANICALLY for literal presence in the material the judge was shown
    (harness source, code context, concrete evidence, trusted values). UNSOUND
    with ``NONE``, or with a quote that is not actually there, is
    citation-void. Keyword matching survives ONLY as the fallback for a
    missing/garbled CITATION line (answer-format noncompliance).

    FAILS OPEN: the re-ask is a fresh verify call, which itself fails open to
    KEEP on an LLM error; `reask_verdict_usable` detects that sentinel and, on
    it, we return the ORIGINAL verdict — so an LLM error can never manufacture
    a drop OR a keep. Only a genuine re-ask verdict replaces the original.
    Format noncompliance likewise never voids anything by itself, and an
    exception inside the citation check falls back to the keyword path."""
    ok, why = verifier.verify(**verify_kwargs)
    if ok:
        return ok, why
    # 5B (void-and-re-ask) was deleted 2026-08-06. It never fired: zero
    # occurrences across the 143-row fixture iterations and every archived live
    # run including both closing pairs, after its matcher bug was found and
    # fixed. Its reach was structurally capped at 10 of 143 rows. The base
    # verify stands on its own; a dismissal is a dismissal.
    return ok, why


def _family_duty_escape(verifier, fired, failing_block, check_source,
                        fd_state):
    """The ONE Spec-J family-duty escape, shared by every terminal gate and
    asked AT MOST ONCE per firing.

    `fd_state` is a mutable dict ``{'value': True|False|None, 'why': str}``
    carrying a family-duty answer already known for this firing — from run.py's
    Spec-J ladder (``fd_prior``) or from an earlier gate in the same
    ``adjudicate`` call. YES means the violated property IS the failing test's
    own observable, i.e. the patch-failed-to-fix pattern, and the keep stands.

    FAILS OPEN in both directions: ``verifier.family_duty`` already returns
    ``(True, ...)`` on an LLM error, and an exception escaping it is caught here
    and read as YES. A transport failure can therefore never manufacture a drop;
    and because the caller only ever turns a KEEP into a DROP, it can never
    manufacture a keep either. Returns ``(escape_granted, why)``.

    AUDIT: emits ``cycle6_family_duty_considered`` (was the judge asked, or was
    a prior answer reused) and ``cycle6_family_duty_decided`` (YES/NO and where
    the answer came from) so a green leg's trace.md shows how many times the
    family-duty question was actually put to the judge."""
    tgt = _target_of(fired)
    prior = fd_state.get('value')
    if prior is True:
        _ev('cycle6_family_duty_considered', target=tgt, output='prior=True',
            reason='skipped: fd_prior already known (not asking the judge)')
        _ev('cycle6_family_duty_decided', target=tgt, output='YES',
            reason='source=prior')
        return True, (fd_state.get('why')
                      or "family duty applies (prior review)")
    if prior is False:
        _ev('cycle6_family_duty_considered', target=tgt, output='prior=False',
            reason='skipped: fd_prior already known (not asking the judge)')
        _ev('cycle6_family_duty_decided', target=tgt, output='NO',
            reason='source=prior')
        return False, (fd_state.get('why')
                       or "family duty does not apply (prior review)")
    _ev('cycle6_family_duty_considered', target=tgt, output='prior=None',
        reason='asking verifier.family_duty (no prior answer for this firing)')
    try:
        fd_ok, fd_why = verifier.family_duty(fired, failing_block, check_source)
    except Exception as e:  # pragma: no cover - defensive
        print(f"      [family-duty-error] {e} — escape granted (fail-open)")
        fd_state['value'] = True
        fd_state['why'] = "family-duty check unavailable (error)"
        _ev('cycle6_family_duty_decided', target=tgt, output='YES',
            reason=f'source=error fail-open ({type(e).__name__}: {e})')
        return True, fd_state['why']
    fd_state['value'] = bool(fd_ok)
    fd_state['why'] = fd_why
    _ev('cycle6_family_duty_decided', target=tgt,
        output=('YES' if fd_ok else 'NO'),
        reason='source=asked; ' + str(fd_why)[:300])
    return bool(fd_ok), fd_why


def _indiscriminate_rate_gate(ok, why, evidence_text, verifier, fired,
                              failing_block, check_source, fd_state):
    """Cycle-6 PART 2 — MECHANICAL drop on the indiscriminate profile.

    When a firing's own evidence carries a MEASURED buggy-side fire rate at or
    above ``INTRINSIC_FIRE_RATIO`` (0.95), the check condemns the KNOWN-BROKEN
    build on essentially every input. Whatever it is measuring, it is not this
    patch: it was already true before the patch existed. The SOUND keep is void
    UNLESS the family-duty question answers YES.

    This is enforcement, not persuasion. night20b (docs/replay/
    night20b_analysis.md, "Chronic-FP classification") delivered exactly this
    fact, in the evidence block the judge was shown, on:
      * Math-73-c ``lifted-seed`` / Closure-62 ``null-source-eol-caret`` —
        "[fire-rate fact] buggy build 999/1000 = 100% ... intrinsic to the
        check/setup construction, not a detection of the defect", kept SOUND
        with ``CITATION: NONE``;
      * Math-30 ``overflow-boundary-monotone`` — buggy 20000/20000 = 100%,
        kept SOUND with ``CITATION: NONE`` on a from-first-principles assertion.
    Five of the eight bad keeps had the clearing fact in hand. So the CODE
    decides it now.

    Narrower than the reverted 5D rate path in ``terminal_profile``: that one
    also condemned the two-sided ``TERMINAL_BOTH_FIRE_RATIO`` band and measured
    net-negative (docs/replay/v5d_iter2_analysis.md). This gate fires ONLY on
    the intrinsic bar, where the note's own wording already says the firing is
    structural.

    GUARD: Chart-19's convicting relation also measures buggy 20000/20000 =
    100%. It must and does survive — via family-duty YES, because it asserts
    the failing test's OWN observable. The escape is not weakened to force any
    drop; if a case needs the escape loosened, the rule is wrong.

    FAILS OPEN: no measurement, an unparseable note, or any exception leaves
    the verdict untouched; the family-duty escape fails open to YES.

    AUDIT: emits ``cycle6_6B_indiscriminate_considered`` (with the parsed
    buggy-side rate, or ``rate=None`` when the evidence carried no usable
    measurement) and ``cycle6_6B_indiscriminate_decided`` (dropped /
    escaped-by-family-duty-YES / not-applicable). Both survive a green leg, so
    "6B ran and found no rate" is now distinguishable from "6B never ran"."""
    tgt = _target_of(fired)
    if not ok:
        # LABEL FIX (item 1, the 7th instance of this defect shape). These two
        # events used to read "verdict already UNSOUND before this gate" and
        # "nothing to drop (already UNSOUND)". In this file `ok` is the status
        # of the FIRING, not of the patch: ok=True means the alarm STANDS as
        # evidence against the patch, ok=False means it was already explained
        # away. So "already UNSOUND" meant "the alarm is already discarded" —
        # but every reader parses it as "the patch is unsound", the opposite
        # sense, which is how it got misread twice in one day. Verified against
        # 16 legs of final30A/B with unambiguous outcomes: 16 agree, 0 disagree.
        _ev('cycle6_6B_indiscriminate_considered', target=tgt,
            output='alarm-already-discarded',
            reason='this alarm was already explained away upstream of the '
                   'gate; 6B only ever discards a STANDING alarm, so there is '
                   'nothing here for it to act on')
        _ev('cycle6_6B_indiscriminate_decided', target=tgt,
            output='not-applicable · alarm-already-discarded',
            reason='no standing alarm to discard — verdict unchanged')
        return ok, why
    try:
        from java.relations.evidence_facts import indiscriminate_rate_diagnosis
        diag = indiscriminate_rate_diagnosis(evidence_text)
        rate = diag['drop_rate']
    except Exception as e:  # pragma: no cover - defensive
        print(f"      [6B-rate-parse-error] {e} — verdict unchanged")
        _ev('cycle6_6B_indiscriminate_considered', target=tgt,
            output='rate=parse-error',
            reason=f'{type(e).__name__}: {e}')
        _ev('cycle6_6B_indiscriminate_decided', target=tgt,
            output='not-applicable',
            reason='rate parse raised — verdict unchanged (fail-open)')
        return ok, why
    # The state is reported on BOTH events, and the measured rate is reported
    # even when no drop follows. "not-applicable" alone used to cover five
    # different situations — including the healthy one where the check works.
    seen = ('rate=None' if diag['rate'] is None
            else f"rate={diag['rate']:.4f}")
    _ev('cycle6_6B_indiscriminate_considered', target=tgt,
        output=f"{diag['state']} · {seen}", reason=diag['detail'])
    if rate is None:
        _ev('cycle6_6B_indiscriminate_decided', target=tgt,
            output=f"not-applicable · {diag['state']}",
            reason=f"{diag['detail']} — verdict unchanged")
        return ok, why
    fd_ok, fd_why = _family_duty_escape(
        verifier, fired, failing_block, check_source, fd_state)
    if fd_ok:
        _ev('cycle6_6B_indiscriminate_decided', target=tgt,
            output='escaped', reason='family-duty YES: ' + str(fd_why)[:300])
        return ok, why
    print(f"      [6B-INDISCRIMINATE-DROP] buggy-side fire rate {rate:.0%} "
          f">= intrinsic bar, family-duty NO")
    _ev('cycle6_6B_indiscriminate_decided', target=tgt, output='dropped',
        reason=f'6B-INDISCRIMINATE-DROP: buggy rate {rate:.0%} >= intrinsic '
               f'bar and family-duty NO')
    return False, ("INDISCRIMINATE-RATE TERMINAL [6B-INDISCRIMINATE-DROP] "
                   "(family-duty NO): this check condemns the KNOWN-BROKEN "
                   "build on {:.0%} of random valid inputs, so the behaviour "
                   "it reports pre-dates the patch, and a focused review found "
                   "the violated property is NOT the failing test's own "
                   "observable. ".format(rate) + str(fd_why))


def _confirmed_fires_on_both_gate(ok, why, evidence_text, verifier, fired,
                                  failing_block, check_source, fd_state):
    """Cycle-6 PART 3 — confirmed fires-on-both, resolved by the VALUE
    comparison before anything is dropped.

    "Fires on both builds" is ambiguous on its own. A replay (the direct
    same-check buggy replay, or the muted re-replay once the shadowing check is
    silenced) CONFIRMS the check fires on both; run.py then hands both fired
    messages to ``compare_fired_values`` (cycle-2b) and the note stamps the
    answer. This gate reads that answer — never prose:

      * ``different``    -> the PARTIAL-FIX pattern: the patch changed the
                            behaviour at this input without restoring the
                            expected value. It is the strongest conviction
                            evidence the pipeline has (it is what caught
                            Lang-63) and is NEVER dropped by this rule.
      * ``identical``    -> genuinely pre-existing: the unpatched build does
                            the same thing at the same input. Mechanical drop,
                            with the family-duty escape.
      * ``not-compared`` -> UNKNOWN. Not a drop. Dropping on unknown is exactly
                            how the marker bug happened; it is not relocated
                            into this rule. ``[fact:not-compared]`` therefore
                            stays in ``_NON_TERMINAL_FACT_TAGS``.

    In practice the ``identical`` case is usually already dropped upstream by
    the 5C gate; this gate still owns it because 5C resolves its tags
    deny-first, so a blob carrying a confirmed-identical fact from one site AND
    an unconfirmed ``not-compared`` from another reads as non-terminal there.
    The CONFIRMED measurement is the stronger fact and decides here.

    FAILS OPEN: an unreadable blob, a missing confirmation or any exception
    leaves the verdict untouched; the family-duty escape fails open to YES.

    AUDIT: emits ``cycle6_6C_fires_on_both_considered`` carrying the RESOLVED
    value verdict (``different`` / ``identical`` / ``not-compared`` / ``none``)
    and ``cycle6_6C_fires_on_both_decided`` (dropped / escaped / kept-partial-
    fix / not-applicable), so a green leg records which of the four inputs this
    gate actually saw."""
    tgt = _target_of(fired)
    if not ok:
        # Same label fix as 6B — see the note there. `ok` is the firing's
        # status, not the patch's.
        _ev('cycle6_6C_fires_on_both_considered', target=tgt,
            output='alarm-already-discarded',
            reason='this alarm was already explained away upstream of the '
                   'gate; 6C only ever acts on a STANDING alarm')
        _ev('cycle6_6C_fires_on_both_decided', target=tgt,
            output='not-applicable · alarm-already-discarded',
            reason='no standing alarm to discard — verdict unchanged')
        return ok, why
    try:
        from java.relations.evidence_facts import (
            confirmed_fires_on_both_verdict)
        verdict = confirmed_fires_on_both_verdict(evidence_text)
    except Exception as e:  # pragma: no cover - defensive
        print(f"      [6C-tag-parse-error] {e} — verdict unchanged")
        _ev('cycle6_6C_fires_on_both_considered', target=tgt,
            output='verdict=parse-error', reason=f'{type(e).__name__}: {e}')
        _ev('cycle6_6C_fires_on_both_decided', target=tgt,
            output='not-applicable',
            reason='tag parse raised — verdict unchanged (fail-open)')
        return ok, why
    _ev('cycle6_6C_fires_on_both_considered', target=tgt,
        output='verdict=' + ('none' if verdict is None else str(verdict)),
        reason='resolved value comparison for the confirmed fires-on-both tag')
    if verdict == 'different':
        print("      [6C-partial-fix-keep] confirmed on both builds with "
              "DIFFERENT observed values — conviction evidence, never dropped "
              "here")
        _ev('cycle6_6C_fires_on_both_decided', target=tgt,
            output='kept', reason='6C-partial-fix-keep: DIFFERENT observed '
                                  'values — conviction evidence, never dropped')
        return ok, why
    if verdict != 'identical':
        # LABEL FIX (item 1): these are two different situations and the old
        # single 'not-applicable' hid which one happened. 'not-compared' means a
        # fires-on-both confirmation exists but the observed values could not be
        # compared; None means there was no confirmation to compare at all. Only
        # the second means "this gate had nothing to look at".
        state = ('values-not-comparable' if verdict == 'not-compared'
                 else 'no-fires-on-both-confirmation')
        _ev('cycle6_6C_fires_on_both_decided', target=tgt,
            output=f'not-applicable · {state}',
            reason=('a fires-on-both confirmation exists but its observed '
                    'values could not be compared, and unknown is never a '
                    'drop — verdict unchanged'
                    if verdict == 'not-compared' else
                    'no fires-on-both confirmation appears in this evidence, '
                    'so there was nothing to compare — verdict unchanged'))
        return ok, why
    fd_ok, fd_why = _family_duty_escape(
        verifier, fired, failing_block, check_source, fd_state)
    if fd_ok:
        _ev('cycle6_6C_fires_on_both_decided', target=tgt, output='escaped',
            reason='family-duty YES: ' + str(fd_why)[:300])
        return ok, why
    print("      [6C-FIRES-ON-BOTH-DROP] confirmed on both builds with "
          "IDENTICAL observed values, family-duty NO")
    _ev('cycle6_6C_fires_on_both_decided', target=tgt, output='dropped',
        reason='6C-FIRES-ON-BOTH-DROP: confirmed on both builds with IDENTICAL '
               'observed values and family-duty NO')
    return False, ("CONFIRMED-FIRES-ON-BOTH TERMINAL [6C-FIRES-ON-BOTH-DROP] "
                   "(family-duty NO): a replay confirmed this same check fires "
                   "on the BUGGY build at this exact input and the two fired "
                   "messages compare IDENTICAL, so the behaviour is "
                   "pre-existing, and a focused review found the violated "
                   "property is NOT the failing test's own observable. "
                   + str(fd_why))


def _terminal_identical_gate(ok, why, evidence_text, verifier, fired,
                             failing_block, check_source,
                             fd_prior=None, fd_state=None):
    """Cycle-5C precision-side mirror. IDENTICAL-ON-BOTH / fires-on-buggy is
    TERMINAL: a discretionary SOUND keep on a firing carrying that mechanical
    fact is void UNLESS the Spec-J family-duty question answers YES.
    Provenance ('lifts the trusted test') alone cannot override it.

    Cycle-5D adds the MEASURED form of the same fact: a [fire-rate fact]
    whose buggy-side rate is genuinely high (see ``fire_rate_is_terminal``) is
    the same pre-existing-behaviour signal expressed as rates, and is terminal
    on the same terms — family-duty escape included. The 5A asymmetric CATCH
    profile (buggy LOW / patched high) is never terminal.

    `fd_prior` carries a family-duty result already computed for this firing
    (True=YES, False=NO, None=not consulted) so we never double-ask.
    `fd_state` is the cycle-6 shared cache of that same answer; when the caller
    passes one, the answer this gate learns is reused by the cycle-6 gates
    instead of asking the judge a second time. `fd_prior` seeds it.

    FAILS OPEN: family_duty returns (True, ...) on any LLM error, so an error
    can never manufacture a drop; only an explicit DUTY:NO voids the keep."""
    if fd_state is None:
        fd_state = {'value': fd_prior, 'why': None}
    if not ok:
        return ok, why
    if fd_state.get('value') is True:
        return ok, why
    from java.relations.evidence_facts import terminal_profile
    profile = terminal_profile(evidence_text)
    if not profile:
        return ok, why
    fd_ok, fd_why = _family_duty_escape(
        verifier, fired, failing_block, check_source, fd_state)
    if fd_ok:
        return ok, why
    label = ("IDENTICAL/FIRES-ON-BUGGY TERMINAL"
             if profile == 'identical-on-both'
             else "FIRES-ON-BOTH RATE TERMINAL [5D-rate]")
    return False, (label + " (family-duty NO): " + fd_why)


def adjudicate(verifier, *, harness_source, fired_assertion, trusted_values,
               concrete_evidence, code_context, pinned_source, evidence_profile,
               failing_block, check_source, fd_prior=None,
               is_direction_confirmed=False):
    """The ONE shipped judge decision, for both run.py and verifier_replay.py.

    Order (exactly as shipped inline in run.py):
      1. base ``verifier.verify(...)`` on the verify kwargs;
      2. 5B ``_guarded_verify`` void-and-re-ask (pin-void / citation-void),
         keyed by ``pinned_source`` and ``evidence_profile``;
      3. 5C ``_terminal_identical_gate``, then the two cycle-6 mechanical
         gates — 6B ``_indiscriminate_rate_gate`` (measured buggy-side fire
         rate at/above the intrinsic bar) and 6C
         ``_confirmed_fires_on_both_gate`` (a confirmed fires-on-both whose
         VALUE comparison came back identical). All three are skipped when the
         firing is direction-confirmed — a mechanical buggy-build catch — and
         all three share ONE family-duty answer via ``fd_state``, seeded from
         ``fd_prior``, so ``verifier.family_duty`` is asked at most once per
         firing.

    Returns ``(ok, why)``. Fails open throughout: an LLM error in any step can
    never manufacture a drop or a keep (see the helpers' docstrings). Every
    mechanical drop is logged with its own greppable tag
    (``[6B-INDISCRIMINATE-DROP]``, ``[6C-FIRES-ON-BOTH-DROP]``) and carries
    that tag in the returned ``why``.

    ``pinned_source`` is passed straight through as the ``pinned`` argument of
    the 5B lint — run.py supplies the ``pinned_parameters()`` dict it already
    computed; a caller with no reconstructable pin (e.g. offline replay) passes
    any non-dict value, which ``dismissal_invokes_pinned`` treats
    conservatively (it never fires a pin-void), so no void is manufactured.
    """
    ok, why = _guarded_verify(
        verifier,
        dict(harness_source=harness_source, fired_assertion=fired_assertion,
             trusted_values=trusted_values, concrete_evidence=concrete_evidence,
             code_context=code_context),
        pinned=pinned_source, evidence_profile=evidence_profile)
    # Always-on entry event: the ONE place that says whether the terminal gate
    # ladder ran at all for this firing. Without it a trace with no 6B/6C event
    # is ambiguous between "direction-confirmed, gates deliberately skipped"
    # and "this code never executed".
    _ev('cycle6_gates_entry', target=_target_of(fired_assertion),
        output=('skipped' if is_direction_confirmed else 'running'),
        reason=('direction-confirmed firing (mechanical buggy-build catch) — '
                '5C/6B/6C all skipped by design'
                if is_direction_confirmed else
                f'running 5C -> 6B -> 6C; base verdict ok={bool(ok)}, '
                f'fd_prior={fd_prior}'))
    if not is_direction_confirmed:
        fd_state = {'value': fd_prior, 'why': None}
        for _gate in (_terminal_identical_gate,
                      _indiscriminate_rate_gate,
                      _confirmed_fires_on_both_gate):
            ok, why = _gate(ok, why, concrete_evidence, verifier,
                            fired_assertion, failing_block, check_source,
                            fd_state=fd_state)
    return ok, why
