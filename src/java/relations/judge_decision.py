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


def _terminal_identical_gate(ok, why, evidence_text, verifier, fired,
                             failing_block, check_source,
                             fd_prior=None):
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

    FAILS OPEN: family_duty returns (True, ...) on any LLM error, so an error
    can never manufacture a drop; only an explicit DUTY:NO voids the keep."""
    if not ok:
        return ok, why
    if fd_prior is True:
        return ok, why
    from java.relations.evidence_facts import terminal_profile
    profile = terminal_profile(evidence_text)
    if not profile:
        return ok, why
    if fd_prior is False:
        fd_ok, fd_why = False, "family duty does not apply (prior review)"
    else:
        fd_ok, fd_why = verifier.family_duty(fired, failing_block,
                                             check_source)
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
