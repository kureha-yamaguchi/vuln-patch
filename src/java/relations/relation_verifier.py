"""Verify a fired harness's oracle is SOUND before trusting it to flag a
patch — a non-cheating false-positive filter.

The dominant semantic-FP mode is an INVENTED assertion that is not true for
every correct implementation, so it fires on a CORRECT patch:
  * `FastMath.max(a,b) == Math.max(a,b)` — differs on NaN / -0.0
  * `z * z.reciprocal() == 1` — false at z == 0 and under overflow
  * "more iterations must not change the returned root" — false for an
    iterative solver

Before a harness that fired on the patched code is counted as evidence of
overfitting, we ask the LLM to review its assertions and judge whether each
is SOUND (true for ANY correct implementation) or UNSOUND (could fire on a
correct one). Unsound findings are dropped. This uses ONLY the harness
source — never the developer fix — so it is not cheating; it is the
prompt-side analogue of validating a relation against known-correct
behaviour.

Fails OPEN: on any error or ambiguous verdict the finding is KEPT, so the
verifier can only REMOVE false positives, never manufacture false negatives.

KNOWN ERROR MODES (both observed — this filter is imperfect in BOTH
directions, which is why nothing upstream may promise "the reviewer makes
wrong assertions free"):
  * LEAK: passed unsound oracles in several batches (FPs survived).
  * OVER-KILL: dropped a genuine Time-4 detection by reasoning "a correct
    implementation may legitimately throw on this input" — ignoring that
    the harness catches-and-skips exceptions, so a throwing correct
    implementation could never have caused the firing. The firing came from
    the code COMPLETING with an impossible value. The exception-path
    guidance below exists to close exactly that reasoning hole.
  * OVER-KILL (skeleton absence): with class context attached, dropped a
    genuine Chart-19 detection by reading the PARTIAL skeleton as complete
    — "nothing in the provided contracts guarantees getDomainAxisIndex(null)
    must throw" — when the null-check lived in an ELIDED method body. The
    absence-is-not-unsoundness guidance below exists to close that hole:
    the skeleton may only CONFIRM facts, never prove a guarantee absent.
"""
import re
from typing import List, Optional, Tuple

from llm import HarnessGenerator

_SYSTEM = (
    "You are a strict reviewer of fuzzing-harness ORACLES. A harness flagged"
    " a code patch as buggy because an assertion inside it threw. Decide"
    " whether that flag is TRUSTWORTHY. An assertion is SOUND only if it holds"
    " for EVERY correct implementation of the API under test; if a correct"
    " implementation could make it throw, it is UNSOUND and the flag is a"
    " false positive. Judge only oracle soundness, not whether the patch is"
    " actually buggy."
    " CRITICAL: a harness may contain SEVERAL independent assertions. Only"
    " the ONE that actually threw determines whether this flag is a false"
    " positive; the others did not fire and are irrelevant to this finding."
    " When told which assertion fired, judge THAT assertion alone — do NOT"
    " reject the finding because some OTHER, unrelated assertion in the file"
    " looks unsound."
)

_GUIDANCE = (
    # Model-facing text stays dataset-neutral (see the observed FP modes in
    # the module docstring for the concrete cases that motivated each
    # category — they must not leak into the prompt, or the critic tunes
    # to this benchmark's vocabulary instead of the failure categories).
    "BEFORE ANYTHING ELSE, read how the harness handles exceptions around"
    " the fired check. When the harness CATCHES exceptions from the API"
    " calls and returns/skips instead of asserting on them, then 'a correct"
    " implementation might legitimately THROW on this input' can NOT make"
    " the firing a false positive — a throwing implementation would have"
    " been skipped, never flagged. In that case the firing means the code"
    " COMPLETED and produced a value that failed the check, so the only"
    " question is: could a CORRECT implementation COMPLETE with such a"
    " value? Do not answer UNSOUND on the possibility of a throw that the"
    " harness demonstrably swallows.\n\n"
    "Scrutinise especially:\n"
    "- Comparisons to a DIFFERENT library/method as a reference (an"
    " alternative implementation of the same math/parsing/formatting)"
    " that may legitimately differ on edge inputs (NaN, -0.0,"
    " +/-Infinity, overflow, empty input, locale).\n"
    "- Relations that break at a boundary: division/reciprocal at 0, log of"
    " 0, index at length, an identity like f(g(x))==x that fails on special"
    " values.\n"
    "- Iteration/precision-dependent claims (more iterations, tighter"
    " tolerance, or scaling must not change a result) for approximate or"
    " iterative code.\n"
    "- Floating-point equality with a too-tight or zero tolerance.\n"
    "A CONSISTENCY check that computes the same quantity two independent ways"
    " from the SAME implementation (an aggregate recomputed from the"
    " object's own output vs the aggregate it reports; a manual count vs a"
    " reported count) is normally SOUND.\n"
    "ABSENCE OF EVIDENCE IS NOT UNSOUNDNESS: any code/context you are shown"
    " is a PARTIAL view. 'Nothing provided guarantees X' is never by itself"
    " grounds for UNSOUND — the guarantee may live in an elided body,"
    " undocumented behaviour, or a class you were not shown. To answer"
    " UNSOUND you must point at something POSITIVE: a shown contract the"
    " assertion contradicts, or a concrete correct implementation that"
    " would fire it AND survive the harness's catch/skip structure.\n"
    "TRUST HIERARCHY — THE SHOWN CODE MAY BE THE BUG: the code context you"
    " are shown is usually the BUGGY build, and the guard/branch logic you"
    " read there can be the very defect under test. Where a TRUSTED"
    " expected value (a literal the bug's own failing test asserts —"
    " listed under trusted values) pins a behaviour, that test value"
    " OUTRANKS the shown body: do not answer UNSOUND by citing a shown"
    " guard/condition that contradicts a trusted test value — an assertion"
    " demanding the test-pinned behaviour at the test-pinned boundary is"
    " the canonical SOUND oracle, and the shown guard that forbids it is"
    " exactly what the fix is expected to change.\n"
    "OBSERVED EVIDENCE BEATS HYPOTHETICALS: when the fired message or the"
    " evidence section shows the CONCRETE values/object state the run"
    " produced, judge that observation. If the observed output state is one"
    " no correct implementation could produce (an object holding mutually"
    " contradictory fields, a value outside the documented range, a"
    " collection whose reported size disagrees with its contents), the"
    " assertion that caught it is SOUND regardless of how speculative its"
    " general form sounds.\n"
    "MECHANICAL FACTS OUTRANK PROVENANCE: a bracketed computed fact"
    " ([buggy-replay fact], [differential replay], [trigger-test lift])"
    " that states the replay was shadowed/unavailable can be overridden"
    " ONLY by another computed fact - e.g. a shown trusted value or"
    " documented contract that covers the firing input's own regime. That"
    " a check 'encodes' or 'lifts' a trusted test is provenance about the"
    " check's origin, not a fact about THIS input: it never by itself"
    " overrides a shadowed fact.\n"
    "IDENTICAL-ON-BOTH-BUILDS IS TERMINAL: when a computed fact states the"
    " observed behavior is identical on both builds at the exact firing"
    " input, NO contract argument can rescue the finding - if the asserted"
    " contract is real, the UNPATCHED code violates it identically at this"
    " input, which makes the violation pre-existing surface by definition,"
    " not evidence about the patch. Arguing 'no correct implementation"
    " could return this value' concedes the point: the buggy build - the"
    " one whose defect is already known and reported - returns it too."
    " The ONLY ground to keep such a finding: the violated property is the"
    " very behaviour the failing test shows is wrong (the patch's duty to"
    " fix), observed at inputs beyond the test's own - the"
    " patch-failed-to-fix pattern. If that duty does not apply, the"
    " verdict is UNSOUND.\n"
    "BUT — a firing that claims a STRUCTURALLY IMPOSSIBLE violation is"
    " evidence the CHECK's own comparison is broken, NOT a defect, and is"
    " UNSOUND. If the check asserts a property the class GUARANTEES by"
    " construction (an immutable/final-field object's operand 'changed'"
    " after a call; a value that differs from ITSELF; a fresh copy"
    " 'unequal' to its source) and the firing says that guaranteed"
    " property was violated, the cause is the check's comparison, not the"
    " code — overwhelmingly the NaN trap: `NaN == NaN` and"
    " `Double.NaN != Double.NaN` are FALSE, so a check comparing"
    " components that can be NaN with ==/equals reports 'changed' or"
    " 'unequal' on correct code whenever a component is NaN (and this"
    " fuzzer feeds NaN). Unless the check demonstrably uses NaN-safe"
    " comparison (Double.isNaN handling, or Double.compare), a"
    " structurally-impossible firing on a NaN-capable value is UNSOUND.\n\n"
    "REASONING PROTOCOL — work through these five steps, in order,"
    " before answering (they exist because measured failures came from"
    " skipping one of them):\n"
    "1. RESTATE what the fired check asserts, as a contract claim"
    " ('for input class X, the result must satisfy Y').\n"
    "2. NAME the claim's source: a quoted documentation sentence, a"
    " trusted test-lifted value, or invented plausibility. An invented"
    " claim survives only via step 3.\n"
    "3. CONSTRUCT the strongest counterexample: a concrete correct"
    " implementation and a concrete input that would fire this check.\n"
    "4. TEST the counterexample, twice. (a) Against the harness's"
    " catch/skip structure and the trusted values — if it would be"
    " swallowed, is unreachable, or contradicts a trusted value, it"
    " does not count. (b) Against the CONCRETE OBSERVED firing — to"
    " count, the counterexample must actually PRODUCE what this firing"
    " observed, not merely be plausible in the abstract. This is where"
    " most wrong UNSOUND verdicts die: the hypothetical sounds valid in"
    " general and is impossible for the actual firing. Apply it to"
    " every kind of check, not only numeric ones:\n"
    "   - NUMERIC disagreement: a tolerance/rounding counterexample"
    " cannot explain a sign flip or a value orders of magnitude off"
    " (see the ROUNDING FLOOR below).\n"
    "   - BOOLEAN / STATE / EXCEPTION disagreement: a 'a correct"
    " implementation could lazily compute / cache / take a different"
    " branch / throw a different exception' counterexample counts ONLY"
    " if that behaviour would produce THIS observed result — the same"
    " true/false, the same exception type, the same before/after change"
    " — on THIS input. Naming a mechanism a correct implementation"
    " 'could' have ('contains() could evaluate lazily', 'it could throw"
    " here') is NOT enough: you must show that mechanism yields the"
    " EXACT observed firing. If the observed evidence shows a completed"
    " call returning a concrete wrong value/state, a 'could throw'"
    " counterexample is irrelevant (it did not throw — it completed).\n"
    "   - SPECIAL-VALUE / NONDETERMINISM: a special-value counterexample"
    " cannot explain a firing whose observed input is ordinary; a"
    " nondeterminism counterexample cannot explain a deterministic 2/2"
    " replay.\n"
    "5. VERDICT: UNSOUND only if a counterexample survives BOTH tests"
    " of step 4; otherwise SOUND.\n"
    "ROUNDING FLOOR (apply inside step 4b): a NUMERIC disagreement whose"
    " magnitude is within floating-point rounding of the boundary or the"
    " compared value — |a - b| <= ~1e-9 * max(1, |a|, |b|) — is ALWAYS"
    " reproducible by a correct implementation's own double arithmetic,"
    " so a check that fires ONLY by such a margin is UNSOUND (zero or"
    " too-tight tolerance) unless the documented contract guarantees an"
    " exact result. This explicitly includes a 'value lies within its"
    " bounds' check where the value misses a bound by a rounding epsilon"
    " (a mean landing one ulp below an integer support bound at a"
    " degenerate input), and any recomputed-formula check compared with"
    " ==, an absolute tolerance, or a tolerance below 1e-9-relative. The"
    " real defect you are backstopping diverges by a LARGE amount (a sign"
    " flip, a value multiples off), never by a last-digit epsilon.\n\n"
    "CITATION LINE — QUOTE, DO NOT PARAPHRASE: when your verdict is"
    " UNSOUND, the CITATION line must hold a passage copied EXACTLY,"
    " character for character, from the material shown to you above — the"
    " harness source, the code context, the concrete evidence, or the"
    " trusted values — that supports the dismissal: a documented contract"
    " the assertion contradicts, a shown implementation detail, or the"
    " line of the check whose comparison is demonstrably broken. Copy it"
    " verbatim; do not summarise, re-word, or reconstruct it from memory,"
    " and never invent one — a passage that is not present word for word"
    " in what you were shown counts as no citation at all. If your"
    " dismissal rests on a hypothetical (what some correct implementation"
    " could do) rather than on something SHOWN, answer CITATION: NONE."
    " For a SOUND verdict, CITATION: NONE is fine.\n\n"
    "Answer on three lines EXACTLY:\n"
    "VERDICT: SOUND | UNSOUND\n"
    "WHY: <one sentence — for UNSOUND, it must contain the surviving"
    " counterexample>\n"
    "CITATION: \"<verbatim quote from the shown material>\" | NONE"
)

# Ensemble lenses for votes > 1: each vote reviews the same finding from a
# DIFFERENT angle, because identical redundant votes share blind spots
# (reasoning models at fixed effort are near-deterministic, so re-asking
# the same question mostly re-produces the same answer). Majority of
# UNSOUND is required to drop.
_LENSES = [
    # Lens 1: the default, contract-first review (no extra emphasis).
    "",
    # Lens 2: exception-path / reachability — can the claimed correct-impl
    # behaviour actually REACH a throw the harness would report?
    "\nFOCUS FOR THIS REVIEW: trace the exact control flow from the fired"
    " check backwards. Enumerate every way a correct implementation could"
    " cause THIS throw to execute, checking each against the harness's own"
    " catch/skip structure and input construction. If every such path is"
    " swallowed or unreachable, the finding stands.",
    # Lens 3: concrete-witness — does the fired message itself prove or
    # disprove a correct implementation could produce it?
    "\nFOCUS FOR THIS REVIEW: take the concrete values in the fired message"
    " as the observed witness. Ask only: is there ANY correct implementation"
    " of the documented API whose output on some valid input matches this"
    " witness? Construct it explicitly or answer SOUND.",
]


_ATTRIB_SYSTEM = (
    "You are an ATTRIBUTION reviewer. A fired check has ALREADY been"
    " judged SOUND (it holds for every correct implementation) — do not"
    " re-litigate soundness. Your ONE question: is this firing evidence"
    " that THIS patch is an overfit — a patch that made the reported"
    " failing test pass without actually fixing the underlying defect?\n"
    "KEEP THIS FRONT OF MIND — it is the most common way you will go"
    " wrong: an overfit is caught MOST OFTEN by a check on a DIFFERENT"
    " observable than the failing test's exact assertion. The overfit"
    " special-cased the one assertion the test checks; a sound check on"
    " a RELATED documented property of the same code then still fails,"
    " because the real defect is untouched. So 'this check tests a"
    " different observable than the test' is NOT a reason to dismiss —"
    " it is the NORMAL shape of a real catch. You dismiss only when the"
    " facts positively show the firing is pre-existing surface the"
    " patch had no duty to touch.\n"
    "You are shown computed, mechanical facts (from replaying the exact"
    " firing input on the unpatched buggy build, from screening the"
    " check on the buggy build, and from rerunning the real failing"
    " test), a statement of the reported bug, and the check's own"
    " quoted contract. Decide from THESE, not from what an"
    " implementation 'should' do.")

_ATTRIB_GUIDANCE = (
    "DEEP-DIVE — answer these four questions from the facts before you"
    " rule:\n"
    "Q1. Does the BUGGY build violate this check? A screening fact of"
    " 'DIRECTION-CONFIRMED' or a buggy replay that fires the same check"
    " means YES — the check demonstrably detects the reported defect on"
    " the unpatched code. (If the buggy build does NOT violate it and"
    " the patched build does, the patch INTRODUCED the violation:"
    " ATTRIBUTED, done.)\n"
    "Q2. Is the checked property one the patch had a DUTY to preserve?"
    " YES if it is the failing test's own observable, OR a STATED"
    " documented contract of the code under repair (the check's quoted"
    " formula / @throws / documented range). A patch 'fixing' this code"
    " is obliged to honour that code's own documented contract.\n"
    "Q3. Does the PATCHED build STILL violate it (this firing)? Yes by"
    " construction — that is why you are here.\n"
    "Q4. Could a CORRECT fix of the reported defect have legitimately"
    " left this violated? Only if the property is genuinely unrelated"
    " pre-existing surface: a DIFFERENT feature whose failure has a"
    " different underlying identity than the reported defect (e.g. a"
    " crash whose underlying exception type differs from the reported"
    " one), with no documented contract tying it to the repaired code.\n"
    "RULE: if Q1=yes and Q2=yes and Q3=yes, the patch left a"
    " defect-detecting documented property violated -> ATTRIBUTED"
    " (this is the patch-failed-to-fix pattern, and it is the norm for"
    " generalization catches). Answer NOT_ATTRIBUTED only when Q4"
    " gives a concrete pre-existing-surface reason — most strongly when"
    " the firing merely rebuilds the failing test's own scenario while"
    " the real test now passes (a lifted copy diverging from the real"
    " test), or when the underlying failure identity differs from the"
    " reported defect's. Use INCONCLUSIVE only when the facts truly cut"
    " neither way (no buggy-side data at all).\n"
    "Answer on three lines EXACTLY:\n"
    "FACT: <quote the single fact that most decides this — prefer the"
    " screening/direction or buggy-replay fact over any dismissal-"
    " worded note>\n"
    "ATTRIBUTION: ATTRIBUTED | NOT_ATTRIBUTED | INCONCLUSIVE\n"
    "WHY: <one sentence: your Q1-Q4 answers and the decision>")


_FAMILY_DUTY_SYSTEM = (
    "You answer ONE narrow question about a fuzzing-harness check that fired"
    " on a patched build. A computed, key-value-certified fact has ALREADY"
    " established that the observed behaviour at this exact input is IDENTICAL"
    " on the buggy and patched builds — do NOT re-judge that fact, and do NOT"
    " apply any general oracle-soundness rubric. Decide only the single"
    " question put to you, from the failing test and the check shown, and"
    " answer in the two-line format requested.")


class RelationVerifier:
    """LLM-as-critic soundness check for a harness's oracle."""

    def __init__(self, generator: Optional[HarnessGenerator] = None,
                 votes: int = 1):
        # Deterministic-ish: low temperature (ignored by reasoning models,
        # which use reasoning_effort instead — fine either way).
        self._gen = generator or HarnessGenerator(temperature=0.0, top_p=1.0)
        # votes > 1 enables the diverse-lens ensemble: the finding is
        # dropped only when a strict MAJORITY of lenses judge it unsound.
        # Default 1 (single review) until the offline replay harness shows
        # the ensemble is worth its extra calls.
        self.votes = max(1, int(votes))

    def verify(self, harness_source: str,
               fired_assertion: Optional[str] = None,
               trusted_values: Optional[list] = None,
               concrete_evidence: Optional[str] = None,
               code_context: Optional[str] = None,
               ) -> Tuple[bool, str]:
        """Return (trustworthy, reason). trustworthy=False means the oracle
        is judged UNSOUND (likely false positive) and should be dropped.

        ``fired_assertion`` is the message of the throwable that ACTUALLY
        fired on the patched code (from Jazzer's output). When given, the
        verifier judges ONLY that assertion — not every assertion in the
        file. This is essential: a harness bundles several independent
        oracles, and only the one that fired can make this finding a false
        positive. Judging the whole file lets an unsound-but-DORMANT sibling
        oracle veto a legitimate catch by a sound one (observed: a sound
        `sample >= 0` firing was dropped because a tight-tolerance inverse-CDF
        check sat lower in the same file).

        ``trusted_values`` are EXPECTED values lifted from the project's own
        trigger/passing tests (assertEquals first-arg literals — see
        PromptBuilder.expected_assert_literals). An assertion that fires
        because the result disagrees with one of these is GROUND TRUTH (the
        correct code produces these values), not a speculative relation — so
        it must not be rejected as 'too tight'. Passing them lets the critic
        tell a lifted-seed oracle from an invented one.

        ``concrete_evidence`` is raw output captured at the firing (the
        crash block: exception line + stack + any printed state). The critic
        is told to weigh this observed behaviour over hypotheticals — the
        Time-4 over-kill happened precisely because the abstract relation
        sounded refutable while the observed object state was impossible for
        any correct implementation.

        ``code_context`` is the class-level skeleton of the code under test
        (code_context.assemble_class_context, joined) — the same view the
        synthesizer gets. Without it the critic judges soundness from the
        harness text plus generic Java knowledge, and the measured leaks
        were domain-knowledge failures: it kept a fixed-literal comparison
        against a STOCHASTIC sampler (nothing said the method draws random
        numbers), and kept a finding whose exception the class javadoc
        documents as correct behaviour at that boundary. Built from the
        buggy checkout only — label-free, no developer fix."""
        if fired_assertion and trusted_values:
            # Cheap, deterministic short-circuit before spending an LLM call:
            # if the fired assertion quotes a trusted expected value, it is
            # the lifted-seed oracle. Keep it without review. Match as a
            # standalone token (no word/digit char on either side), not a
            # raw substring — "100" must not match inside "1000" and keep
            # an unrelated oracle unreviewed. len >= 3 additionally fences
            # trivial literals.
            for v in trusted_values:
                sv = str(v)
                if (sv and len(sv) >= 3
                        and re.search(r'(?<![\w.])' + re.escape(sv)
                                      + r'(?![\w.])', fired_assertion)):
                    return True, (f"fired assertion checks a trusted "
                                  f"test-lifted value ({v}); kept")
        focus = ""
        if fired_assertion:
            focus = (
                "\nThe assertion that ACTUALLY fired on the patched code is:\n"
                f"    {fired_assertion}\n"
                "Judge ONLY this assertion's soundness. Ignore other"
                " assertions in the file that did not fire — they cannot make"
                " THIS finding a false positive.\n")
        if trusted_values:
            focus += (
                "\nThese expected values were lifted from the project's own"
                " trigger/passing test, so the CORRECT implementation is known"
                " to produce them — an assertion that fires by disagreeing with"
                " one of them is TRUSTED ground truth, NOT a speculative"
                " relation; do not reject it as too tight or"
                " iteration-dependent:\n    "
                + "; ".join(str(v) for v in trusted_values if v) + "\n")
        if concrete_evidence:
            focus += (
                "\nCONCRETE EVIDENCE captured at the firing (observed"
                " behaviour of the actual run — weigh this over"
                " hypotheticals):\n<evidence>\n"
                + concrete_evidence.strip() + "\n</evidence>\n")
        if code_context:
            focus += (
                "\nTHE CODE UNDER TEST — class-level skeletons (contracts,"
                " field invariants, sibling-method signatures and javadoc)"
                " of the patched class and its neighbours. Ground your"
                " soundness judgment in what THIS code documents and does,"
                " not in generic assumptions: a documented throw at a"
                " boundary is CORRECT behaviour (an assertion firing on it"
                " is unsound); a method documented or implemented as"
                " random/stochastic can never soundly be compared to a"
                " fixed expected literal; a documented guarantee (never"
                " null, always reduced, canonical order) makes an assertion"
                " of that guarantee sound.\n"
                "THIS SKELETON IS PARTIAL: method bodies not touched by the"
                " patch are elided to `{ ... }` and javadoc may simply be"
                " absent. Use the skeleton only to CONFIRM a fact it"
                " actually shows (a documented throw, a stated range, a"
                " stochastic method). NEVER treat the ABSENCE of a"
                " guarantee from this partial view as evidence the"
                " guarantee does not exist — the behaviour may live in an"
                " elided body or undocumented code. When the skeleton is"
                " silent on the fired check, fall back to the harness's own"
                " catch/skip structure and the concrete evidence, and keep"
                " the finding unless something SHOWN here refutes it."
                "\n<codebase_context>\n"
                + code_context.strip() + "\n</codebase_context>\n")
        base_user = (
            "Review this Jazzer harness's assertions.\n\n"
            "<harness>\n" + (harness_source or "") + "\n</harness>\n"
            + focus + "\n" + _GUIDANCE)

        verdicts: List[Tuple[bool, str]] = []
        for i in range(self.votes):
            lens = _LENSES[i % len(_LENSES)] if self.votes > 1 else ""
            messages = [
                {'role': 'system', 'content': _SYSTEM},
                {'role': 'user', 'content': base_user + lens},
            ]
            try:
                out = self._gen.generate(messages) or ""
            except Exception as e:  # fail open — never invent a false negative
                verdicts.append(
                    (True, f"verifier error ({e}); keeping finding"))
                continue
            verdicts.append(self._parse(out))
        if len(verdicts) == 1:
            return verdicts[0]
        unsound = [(ok, why) for ok, why in verdicts if not ok]
        # Strict majority of UNSOUND to drop; ties fail open.
        if len(unsound) * 2 > len(verdicts):
            return False, (f"{len(unsound)}/{len(verdicts)} lenses judged"
                           f" unsound: " + unsound[0][1])
        kept = [why for ok, why in verdicts if ok]
        return True, (f"{len(kept)}/{len(verdicts)} lenses judged sound: "
                      + (kept[0] if kept else ""))

    def attribute(self, fired_assertion: str,
                  fact_notes: str,
                  bug_summary: str,
                  check_rationale: Optional[str] = None
                  ) -> Tuple[bool, str]:
        """Second judge of the two-judge split. Returns (convicts, why).

        convicts=False ONLY on an explicit NOT_ATTRIBUTED — INCONCLUSIVE,
        parse failures and errors all fail OPEN (the finding stands on the
        soundness verdict), so this stage can only remove convictions
        whose computed facts positively say 'not this patch', never
        manufacture a miss from ambiguity. Call it only when fact notes
        were actually computed; with no facts there is nothing for this
        judge to decide."""
        user = (
            "THE REPORTED BUG:\n" + (bug_summary or "?") + "\n\n"
            "THE FIRING under review:\n    "
            + (fired_assertion or "?") + "\n"
            + (("The check's own stated rationale/contract:\n    "
                + check_rationale + "\n") if check_rationale else "")
            + "\nCOMPUTED FACTS about this firing:\n<facts>\n"
            + (fact_notes or "").strip() + "\n</facts>\n\n"
            + _ATTRIB_GUIDANCE)
        messages = [
            {'role': 'system', 'content': _ATTRIB_SYSTEM},
            {'role': 'user', 'content': user},
        ]
        try:
            out = self._gen.generate(messages) or ""
        except Exception as e:
            return True, f"attribution error ({e}); finding stands"
        upper = out.upper()
        why = ""
        for line in out.splitlines():
            s = line.strip()
            if s.upper().startswith("WHY:"):
                why = s[4:].strip()
        if "ATTRIBUTION:" in upper:
            v = upper.split("ATTRIBUTION:", 1)[1].lstrip()
            if v.startswith("NOT_ATTRIBUTED"):
                return False, why or "not attributed to the patch"
            return True, why or "attributed or inconclusive"
        return True, why or "no attribution verdict parsed; finding stands"

    def family_duty(self, fired_assertion: str,
                    failing_test_block: str,
                    check_source: str) -> Tuple[bool, str]:
        """Cycle-3 mechanical identical-drop gate (ONE focused LLM call).

        A computed, key-value-certified fact has ALREADY established that the
        observed behaviour at the exact firing input is IDENTICAL on the
        buggy and patched builds. By the identical-on-both-builds rule that
        makes the finding pre-existing surface, it is dropped MECHANICALLY —
        unless the single legitimate exception holds: the property this check
        asserts is THE VERY behaviour the failing test shows is wrong (the
        patch-failed-to-fix pattern), observed at inputs beyond the test's
        own. Three cycle-2 prompt wordings failed to make the general
        soundness rubric honour that identical fact, so cycle 3 asks ONLY
        this single narrow question here — never the full rubric.

        Returns (keep, why). keep=True => the family duty applies; proceed to
        the normal rv.verify path. keep=False => drop the finding
        mechanically (the caller mirrors the DEFECT-FAMILY-DISMISSED block).

        Fails OPEN: on ANY LLM error it returns (True, ...) so an API failure
        can NEVER cause a drop. Parses strictly: anything not affirmatively
        `DUTY: YES` => False."""
        user = (
            "THE REAL FAILING TEST — its name, source, and the message it"
            " fails with on the buggy build:\n<failing_test>\n"
            + (failing_test_block or "?").strip()
            + "\n</failing_test>\n\n"
            "THE CHECK THAT FIRED — its source:\n<check_source>\n"
            + (check_source or "?").strip()
            + "\n</check_source>\n\n"
            "THE ASSERTION MESSAGE it threw on the patched build:\n    "
            + (fired_assertion or "?") + "\n\n"
            "The observed behaviour is identical on the buggy and patched"
            " builds at this input. The ONLY ground to keep this finding is"
            " the patch-failed-to-fix pattern: the property this check"
            " asserts must be THE VERY behaviour the failing test shows is"
            " wrong (the same observable — not merely the same method or"
            " class), observed at inputs beyond the test's own. Is that the"
            " case here? Answer on two lines: DUTY: YES | NO, WHY: <one"
            " sentence>.")
        messages = [
            {'role': 'system', 'content': _FAMILY_DUTY_SYSTEM},
            {'role': 'user', 'content': user},
        ]
        try:
            out = self._gen.generate(messages) or ""
        except Exception:  # fail open — an API error may NEVER cause a drop
            return True, ("family-duty check unavailable — fail open to full"
                          " judging")
        why = ""
        for line in out.splitlines():
            s = line.strip()
            if s.upper().startswith("WHY:"):
                why = s[4:].strip()
        for line in out.splitlines():
            s = line.strip()
            if s.upper().startswith("DUTY:"):
                verdict = s.upper().split("DUTY:", 1)[1].lstrip()
                if verdict.startswith("YES"):
                    return True, why or "family duty applies; proceed"
                return False, why or "family duty does not apply"
        # No parseable DUTY line -> not YES -> mechanical drop.
        return False, why or "no DUTY verdict parsed"

    @staticmethod
    def _parse(out: str) -> Tuple[bool, str]:
        text = out.strip()
        why = ""
        citation = ""
        for line in text.splitlines():
            s = line.strip()
            if s.upper().startswith("WHY:"):
                why = s[4:].strip()
            elif s.upper().startswith("CITATION:"):
                # Carried through VERBATIM, on its own line, so the caller can
                # check the quote against the material actually shown
                # (evidence_facts.citation_void_decision). Only the answer
                # format knows the citation; (ok, why) is the only channel out.
                citation = s

        def _out(ok: bool, reason: str) -> Tuple[bool, str]:
            return ok, (reason + "\n" + citation if citation else reason)

        upper = text.upper()
        # Only drop on an explicit UNSOUND verdict; anything else keeps it.
        if "VERDICT:" in upper:
            verdict = upper.split("VERDICT:", 1)[1].lstrip()
            if verdict.startswith("UNSOUND"):
                return _out(False, why or "oracle judged unsound")
            return _out(True, why or "oracle judged sound")
        # No parseable verdict -> keep (fail open).
        return _out(True, why or "no verdict parsed; keeping finding")
