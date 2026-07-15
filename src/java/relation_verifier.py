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
    "OBSERVED EVIDENCE BEATS HYPOTHETICALS: when the fired message or the"
    " evidence section shows the CONCRETE values/object state the run"
    " produced, judge that observation. If the observed output state is one"
    " no correct implementation could produce (an object holding mutually"
    " contradictory fields, a value outside the documented range, a"
    " collection whose reported size disagrees with its contents), the"
    " assertion that caught it is SOUND regardless of how speculative its"
    " general form sounds.\n\n"
    "Answer on two lines EXACTLY:\n"
    "VERDICT: SOUND | UNSOUND\n"
    "WHY: <one sentence>"
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
            # the lifted-seed oracle. Keep it without review. Trivial
            # literals are excluded upstream (len >= 3), so a spurious
            # substring hit ("1" in "input=21") can't trigger this.
            for v in trusted_values:
                sv = str(v)
                if sv and len(sv) >= 3 and sv in fired_assertion:
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

    @staticmethod
    def _parse(out: str) -> Tuple[bool, str]:
        text = out.strip()
        why = ""
        for line in text.splitlines():
            s = line.strip()
            if s.upper().startswith("WHY:"):
                why = s[4:].strip()
        upper = text.upper()
        # Only drop on an explicit UNSOUND verdict; anything else keeps it.
        if "VERDICT:" in upper:
            verdict = upper.split("VERDICT:", 1)[1].lstrip()
            if verdict.startswith("UNSOUND"):
                return False, why or "oracle judged unsound"
            return True, why or "oracle judged sound"
        # No parseable verdict -> keep (fail open).
        return True, why or "no verdict parsed; keeping finding"
