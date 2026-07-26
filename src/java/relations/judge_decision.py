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

    FAILS OPEN: the re-ask is a fresh verify call, which itself fails open to
    KEEP on an LLM error; `reask_verdict_usable` detects that sentinel and, on
    it, we return the ORIGINAL verdict — so an LLM error can never manufacture
    a drop OR a keep. Only a genuine re-ask verdict replaces the original."""
    ok, why = verifier.verify(**verify_kwargs)
    if ok:
        return ok, why
    from java.relations.evidence_facts import (
        dismissal_invokes_pinned, verdict_needs_citation,
        pinned_reask_statement, citation_reask_statement,
        reask_verdict_usable)
    reask_stmt, tag = None, None
    if pinned and dismissal_invokes_pinned(why, pinned):
        reask_stmt, tag = pinned_reask_statement(pinned), "pin-void"
    elif evidence_profile and verdict_needs_citation(evidence_profile, why):
        reask_stmt, tag = citation_reask_statement(), "citation-void"
    if not reask_stmt:
        return ok, why
    kw2 = dict(verify_kwargs)
    kw2['concrete_evidence'] = (
        (kw2.get('concrete_evidence') or '') + "\n" + reask_stmt)
    print(f"      [{tag}] verdict void — re-asking once")
    ok2, why2 = verifier.verify(**kw2)
    if not reask_verdict_usable(why2):
        # Re-ask unavailable (LLM error / unparseable) -> keep the ORIGINAL
        # verdict. Never a manufactured flip.
        return ok, why
    return ok2, (f"[{tag} re-ask] " + why2)


def _terminal_identical_gate(ok, why, evidence_text, verifier, fired,
                             failing_block, check_source,
                             fd_prior=None):
    """Cycle-5C precision-side mirror. IDENTICAL-ON-BOTH / fires-on-buggy is
    TERMINAL: a discretionary SOUND keep on a firing carrying that mechanical
    fact is void UNLESS the Spec-J family-duty question answers YES.
    Provenance ('lifts the trusted test') alone cannot override it.

    `fd_prior` carries a family-duty result already computed for this firing
    (True=YES, False=NO, None=not consulted) so we never double-ask.

    FAILS OPEN: family_duty returns (True, ...) on any LLM error, so an error
    can never manufacture a drop; only an explicit DUTY:NO voids the keep."""
    if not ok:
        return ok, why
    if fd_prior is True:
        return ok, why
    from java.relations.evidence_facts import carries_terminal_identical_fact
    if not carries_terminal_identical_fact(evidence_text):
        return ok, why
    if fd_prior is False:
        fd_ok, fd_why = False, "family duty does not apply (prior review)"
    else:
        fd_ok, fd_why = verifier.family_duty(fired, failing_block,
                                             check_source)
    if fd_ok:
        return ok, why
    return False, ("IDENTICAL/FIRES-ON-BUGGY TERMINAL (family-duty NO): "
                   + fd_why)


def adjudicate(verifier, *, harness_source, fired_assertion, trusted_values,
               concrete_evidence, code_context, pinned_source, evidence_profile,
               failing_block, check_source, fd_prior=None,
               is_direction_confirmed=False):
    """The ONE shipped judge decision, for both run.py and verifier_replay.py.

    Order (exactly as shipped inline in run.py):
      1. base ``verifier.verify(...)`` on the verify kwargs;
      2. 5B ``_guarded_verify`` void-and-re-ask (pin-void / citation-void),
         keyed by ``pinned_source`` and ``evidence_profile``;
      3. 5C ``_terminal_identical_gate`` (skipped when the firing is
         direction-confirmed — a mechanical buggy-build catch), consulting
         ``fd_prior`` and, only when needed, ``verifier.family_duty``.

    Returns ``(ok, why)``. Fails open throughout: an LLM error in any step can
    never manufacture a drop or a keep (see the two helpers' docstrings).

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
        ok, why = _terminal_identical_gate(
            ok, why, concrete_evidence, verifier, fired_assertion,
            failing_block, check_source, fd_prior=fd_prior)
    return ok, why
