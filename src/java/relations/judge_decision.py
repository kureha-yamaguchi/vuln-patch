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
    from java.relations.evidence_facts import (
        dismissal_invokes_pinned, verdict_needs_citation,
        pinned_reask_statement, citation_reask_statement,
        reask_verdict_usable, strip_citation_line, citation_void_decision)

    # The material the judge was actually shown, for the literal grounding
    # check. `concrete_evidence` is taken from the ORIGINAL kwargs, so the
    # re-ask statement we ourselves append can never ground a citation.
    _tv = verify_kwargs.get('trusted_values')
    contexts = (verify_kwargs.get('harness_source'),
                verify_kwargs.get('code_context'),
                verify_kwargs.get('concrete_evidence'),
                _tv)

    def _citation_void(verdict_text):
        """Structural citation check, wrapped fail-safe: any failure degrades
        to the pre-existing keyword decision, never to a void."""
        try:
            void, event = citation_void_decision(
                evidence_profile, verdict_text, contexts)
        except Exception as e:  # pragma: no cover - defensive
            print(f"      [citation-check-error] {e} — falling back to the"
                  f" keyword path")
            try:
                return bool(verdict_needs_citation(
                    evidence_profile, strip_citation_line(verdict_text)))
            except Exception:
                return False
        if event != 'not-signature':
            print(f"      [citation-check] {event}")
        return void

    reask_stmt, tag = None, None
    if pinned and dismissal_invokes_pinned(strip_citation_line(why), pinned):
        reask_stmt, tag = pinned_reask_statement(pinned), "pin-void"
    elif evidence_profile and _citation_void(why):
        reask_stmt, tag = citation_reask_statement(), "citation-void"
    if not reask_stmt:
        return ok, why
    kw2 = dict(verify_kwargs)
    kw2['concrete_evidence'] = (
        (kw2.get('concrete_evidence') or '') + "\n" + reask_stmt)
    print(f"      [{tag}] verdict void — re-asking once")
    ok2, why2 = verifier.verify(**kw2)
    if not reask_verdict_usable(strip_citation_line(why2)):
        # Re-ask unavailable (LLM error / unparseable) -> keep the ORIGINAL
        # verdict. Never a manufactured flip.
        return ok, why
    if (tag == "citation-void" and not ok2
            and _citation_void(why2)):
        # 5D: TWICE citation-void under the full drift-kill signature. The
        # rule already declares such a dismissal inadmissible; enforce it.
        print("      [5B-INADMISSIBLE] dismissal twice uncited under "
              "drift-kill signature — KEEPING the finding")
        return True, ("[5B-INADMISSIBLE keep] 5B: dismissal inadmissible — "
                      "twice uncited under drift-kill signature; the "
                      "re-asked dismissal was again an uncited hypothetical: "
                      + str(why2))
    return ok2, (f"[{tag} re-ask] " + why2)


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
    manufacture a keep either. Returns ``(escape_granted, why)``."""
    prior = fd_state.get('value')
    if prior is True:
        return True, (fd_state.get('why')
                      or "family duty applies (prior review)")
    if prior is False:
        return False, (fd_state.get('why')
                       or "family duty does not apply (prior review)")
    try:
        fd_ok, fd_why = verifier.family_duty(fired, failing_block, check_source)
    except Exception as e:  # pragma: no cover - defensive
        print(f"      [family-duty-error] {e} — escape granted (fail-open)")
        fd_state['value'] = True
        fd_state['why'] = "family-duty check unavailable (error)"
        return True, fd_state['why']
    fd_state['value'] = bool(fd_ok)
    fd_state['why'] = fd_why
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
    the verdict untouched; the family-duty escape fails open to YES."""
    if not ok:
        return ok, why
    try:
        from java.relations.evidence_facts import indiscriminate_buggy_rate
        rate = indiscriminate_buggy_rate(evidence_text)
    except Exception as e:  # pragma: no cover - defensive
        print(f"      [6B-rate-parse-error] {e} — verdict unchanged")
        return ok, why
    if rate is None:
        return ok, why
    fd_ok, fd_why = _family_duty_escape(
        verifier, fired, failing_block, check_source, fd_state)
    if fd_ok:
        return ok, why
    print(f"      [6B-INDISCRIMINATE-DROP] buggy-side fire rate {rate:.0%} "
          f">= intrinsic bar, family-duty NO")
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
    leaves the verdict untouched; the family-duty escape fails open to YES."""
    if not ok:
        return ok, why
    try:
        from java.relations.evidence_facts import (
            confirmed_fires_on_both_verdict)
        verdict = confirmed_fires_on_both_verdict(evidence_text)
    except Exception as e:  # pragma: no cover - defensive
        print(f"      [6C-tag-parse-error] {e} — verdict unchanged")
        return ok, why
    if verdict == 'different':
        print("      [6C-partial-fix-keep] confirmed on both builds with "
              "DIFFERENT observed values — conviction evidence, never dropped "
              "here")
        return ok, why
    if verdict != 'identical':
        # 'not-compared' (unknown) or no confirmation at all.
        return ok, why
    fd_ok, fd_why = _family_duty_escape(
        verifier, fired, failing_block, check_source, fd_state)
    if fd_ok:
        return ok, why
    print("      [6C-FIRES-ON-BOTH-DROP] confirmed on both builds with "
          "IDENTICAL observed values, family-duty NO")
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
    if not is_direction_confirmed:
        fd_state = {'value': fd_prior, 'why': None}
        for _gate in (_terminal_identical_gate,
                      _indiscriminate_rate_gate,
                      _confirmed_fires_on_both_gate):
            ok, why = _gate(ok, why, concrete_evidence, verifier,
                            fired_assertion, failing_block, check_source,
                            fd_state=fd_state)
    return ok, why
