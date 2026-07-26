"""Regenerate a harness until a target number of attempts compile AND
trigger the bug on the buggy checkout.

A campaign reuses the same buggy checkout and a prompt that is rebuilt
each fresh attempt to carry variant-analysis context (what the rest of
the set already covers). Each accepted attempt is written to its own
subdirectory under `<buggy_dir>/fuzz/attempt_NNN/` so they don't clobber
each other and can be evaluated downstream.

Convergence criterion. A harness is accepted into the set only if it
(1) compiles and (2) crashes the *buggy* version within a short Jazzer
budget. Compiling is necessary but not sufficient: a harness that builds
yet never reaches the root cause tells us nothing about whether a patch
fixed it, so it is treated as a failed attempt (and fed back as a repair
turn) rather than a win. The buggy-version run is delegated to
`HarnessVerifier`.

Variant analysis. Because we are assembling a *set* whose job is to
interrogate the root cause from many angles (and thereby expose sibling
bugs), each fresh attempt's prompt is rebuilt to tell the model which
reachable functions and crash signatures the set already covers, and to
push toward the uncovered remainder. Crash signatures and reached
functions come from the verifier's run on the buggy code.

After a failed attempt we feed the diagnostics back to the LLM as a
repair turn. The diagnostic is the javac error for a compile failure, or
a "compiled but did not trigger" note for a verify failure. Chains of
consecutive failed repair turns are capped at `max_repair_failures` so
the campaign can recover from a bad lead by starting over.
"""
import inspect
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Dict, Optional, Callable

from java.harness.build import HarnessBuilder, BuildResult
sys.path.insert(0, str(Path(__file__).parent.parent))
from java.parsing.java_source import (alarm_ids_missing, boolean_swallow,
                         negative_modulo_index, oracle_families,
                         rethrow_without_cause, violation_swallowed)
from llm import HarnessGenerator, record_event
from java.execution.fuzz_runner import HarnessVerifier, VerificationResult
from java.execution.oracle_strength import exception_headline, lifted_observed_mismatch


# A prompt factory takes the current set-coverage state (the functions
# already exercised by accepted harnesses, and the crash signatures
# already found) and returns fresh chat-completion messages. This is how
# variant-analysis context is injected without the campaign needing to
# know how prompts are assembled.
#
# A factory MAY additionally accept a third positional argument — the sorted
# list of check-FAMILY stems already covered by accepted harnesses — so the
# later-attempt prompt can steer toward an UNCOVERED family (see
# `novelty_verdict` and the family-novelty gate in `run`). The campaign
# detects the factory's arity and passes families only when it is accepted, so
# a legacy two-argument factory keeps working unchanged.
PromptFactory = Callable[..., List[Dict[str, str]]]


def _factory_accepts_families(factory: Optional[PromptFactory]) -> bool:
    """True iff `factory` can take the accepted-families third argument (has
    >=3 positional parameters, or a *args catch-all). Fails safe to False so a
    two-argument factory is never called with an extra positional it would
    reject."""
    if factory is None:
        return False
    try:
        params = list(inspect.signature(factory).parameters.values())
    except (TypeError, ValueError):
        return False
    if any(p.kind == p.VAR_POSITIONAL for p in params):
        return True
    positional = [p for p in params
                  if p.kind in (p.POSITIONAL_ONLY, p.POSITIONAL_OR_KEYWORD)]
    return len(positional) >= 3


def novelty_verdict(new_families,
                    accepted_families,
                    rejections_so_far: int,
                    max_rejections: int = 2) -> str:
    """Family-novelty decision for a harness that has ALREADY compiled and
    fired on the buggy build. Pure predicate — no I/O, no mutation.

    Returns 'reject' when the harness adds NO check family beyond those an
    accepted harness already covers AND the per-leg novelty-rejection budget
    is not yet spent; 'accept' otherwise. Accepting despite zero new families
    is the deliberate fail-open once the budget (`max_rejections`, default 2)
    is exhausted — the campaign must still be able to finish rather than loop
    forever demanding a family the model cannot invent.

    FAIL-OPEN ON EMPTY EXTRACTION (belt and braces): an EMPTY `new_families`
    set is never redundancy — it means the family extractor could not read the
    candidate's checks (e.g. a genuinely dynamic oracle id it cannot resolve
    statically), so there is no evidence of overlap to reject on. Such a
    candidate is always 'accept'. The campaign gate independently short-circuits
    empty extraction to a `family-extract-failed` fail-open before ever calling
    this predicate; this guard makes a vacuous rejection impossible even if a
    future caller forgets to.

      new_families      — family stems of the candidate harness.
      accepted_families — union of family stems over accepted harnesses.
      rejections_so_far — novelty-rejections already spent this leg.
    """
    new_families = set(new_families)
    if not new_families:
        return 'accept'
    families_added = new_families - set(accepted_families)
    if families_added:
        return 'accept'
    if rejections_so_far < max_rejections:
        return 'reject'
    return 'accept'


# --- Spec N (cycle 3b): generation-side convergence gate --------------------
# The relation arsenal must not be declared finished while its ONLY buggy-side
# detections come from the failing test's own literals — a "seed" detection. A
# seed-only arsenal has proven nothing beyond re-deriving the one input the bug
# report already handed us (the benchmark-farming shape); NON-seed power means
# at least one kept relation fires on the buggy build under inputs OTHER than
# those seeds.
#
# Where the seed / non-seed split lives in the screen data (relation_screen):
#   * rel.screen_stats = (checked, violated) is the FUZZED tier. The main
#     buggy-build screen runs Jazzer from an EMPTY corpus (no trigger seeds
#     are planted — _measure_on_corpus, which replays exactly the seeds, is a
#     SEPARATE call that does not touch screen_stats). Every `violated` here is
#     therefore a firing on a mutated, non-seed input: violated > 0 is non-seed
#     detection power by construction.
#   * the seed/trigger tier feeds ONLY rel.screen_direction
#     ('confirmed'|'inverted'|'uncovered'). A relation "confirmed" on the seeds
#     but silent on the fuzzed tier has violated == 0 → it does NOT count
#     (seed-only confirmation).
#   * rel.screen_direction == 'inverted' (INVERTED-SUSPECT) fires on random
#     inputs yet is silent on the very seeds the bug is about — its firing may
#     be the BACKWARDS direction, so it is not trustworthy non-seed catching
#     and is excluded conservatively.
# A silent tripwire (violated == 0) never counts. Fail-open: a relation whose
# screen data is missing or unparseable is treated as carrying no non-seed
# power (it cannot let the campaign finish on evidence we never measured).

def arsenal_has_nonseed_power(relations) -> bool:
    """True iff ANY kept relation shows a buggy-side detection attributable to
    a NON-seed input: it fired on the fuzzed (random, un-seeded) screen tier
    (screen_stats violated > 0, equivalently buggy_fire_ratio > 0) AND is not
    flagged as an inverted-direction suspect. Seed-only confirmations and
    silent tripwires do not count. Pure predicate — no I/O, no mutation."""
    for rel in relations or []:
        checked = violated = 0
        stats = getattr(rel, 'screen_stats', None)
        if stats is not None and len(stats) == 2:
            try:
                checked, violated = int(stats[0]), int(stats[1])
            except (TypeError, ValueError):
                checked = violated = 0
        else:
            # No raw counts recorded — fall back to the rounded ratio, which
            # is > 0 exactly when the fuzzed tier fired at least once.
            try:
                if float(getattr(rel, 'buggy_fire_ratio', 0) or 0) > 0:
                    checked, violated = 1, 1
            except (TypeError, ValueError):
                pass
        if checked <= 0 or violated <= 0:
            # Silent tripwire OR seed-only confirmation (fuzzed-silent).
            continue
        if getattr(rel, 'screen_direction', '') == 'inverted':
            # INVERTED-SUSPECT: fired on random inputs but backwards on seeds.
            continue
        return True
    return False


def converge_nonseed_arsenal(kept_relations,
                             synth_round: Callable[[], List],
                             screen_round: Callable[[List], List],
                             max_extra_rounds: int = 2,
                             min_extra_rounds: int = 0) -> List:
    """Spec N convergence loop. While the kept arsenal has ZERO non-seed
    detection power and we are under the extra-round bound, request another
    synthesis round (`synth_round()` returns fresh candidates — reuse the
    caller's existing synthesis call verbatim), re-screen it
    (`screen_round(candidates)` returns survivors — reuse the caller's existing
    screen call verbatim), and merge the new survivors into the kept set
    (de-duplicated by name). Returns the possibly-extended kept list.

    This owns only the loop and the merge; `synth_round`/`screen_round` are the
    caller's own closures over the existing machinery, so no synthesis or
    screening policy is duplicated here. Fails soft: a synthesis or screening
    exception ends the loop with whatever was kept so far, never a drop."""
    kept = list(kept_relations or [])
    seen = {getattr(r, 'name', None) or id(r) for r in kept}
    extra_rounds = 0
    # Cycle 4a-i: `min_extra_rounds` forces generation WIDTH regardless of
    # power — the paired pool measurement (retro #3) showed catches ride on
    # whether a roll happens to invent the right check, and extra measured
    # rounds are the direct lever (safe: survivors arrive screened).
    while ((extra_rounds < min_extra_rounds
            or not arsenal_has_nonseed_power(kept))
           and extra_rounds < max_extra_rounds):
        extra_rounds += 1
        if extra_rounds <= min_extra_rounds:
            print(f"[convergence] width round {extra_rounds} "
                  f"(min_extra_rounds={min_extra_rounds})")
        else:
            print(f"[convergence] arsenal has zero non-seed detection power "
                  f"— requesting round {extra_rounds}")
        try:
            candidates = synth_round() or []
        except Exception as exc:
            print(f"[convergence] synthesis round {extra_rounds} failed "
                  f"({exc}) — stopping convergence loop")
            break
        if not candidates:
            continue
        try:
            survivors = screen_round(candidates) or []
        except Exception as exc:
            print(f"[convergence] screening round {extra_rounds} failed "
                  f"({exc}) — stopping convergence loop")
            break
        for r in survivors:
            key = getattr(r, 'name', None) or id(r)
            if key not in seen:
                seen.add(key)
                kept.append(r)
    return kept


@dataclass
class CampaignResult:
    target_successes: int
    achieved_successes: int
    attempts: int
    results: List[BuildResult] = field(default_factory=list)
    successful_results: List[BuildResult] = field(default_factory=list)
    raw_responses: List[str] = field(default_factory=list)
    # Crash signature on the buggy version for each accepted harness,
    # aligned with successful_results. Lets downstream reporting show the
    # diversity of faults the set covers.
    accepted_signatures: List[str] = field(default_factory=list)
    # Exception headline of the throwable that fired on the buggy version
    # for each accepted harness, aligned with successful_results ('' when
    # unavailable). Finer-grained than the signature: the message text
    # says WHICH oracle fired (e.g. 'RuntimeException: consistency
    # violation: reported mean != empirical' vs the lifted symptom
    # assertion), which is what post-run analysis needs to judge whether
    # the set carries symptom-independent oracles.
    accepted_trigger_details: List[str] = field(default_factory=list)

    @property
    def converged(self) -> bool:
        """True iff we hit the target before exhausting max_attempts."""
        return self.achieved_successes >= self.target_successes

    @property
    def success_rate(self) -> float:
        return self.achieved_successes / max(1, self.attempts)

    @property
    def distinct_signatures(self) -> int:
        """How many distinct buggy-version crashes the set covers — the
        headline variant-analysis number."""
        return len({s for s in self.accepted_signatures if s})


class HarnessCampaign:
    """Generate harnesses repeatedly until `target_successes` build, or
    until `max_attempts` is exhausted. Failed attempts spawn a repair
    turn that carries the javac diagnostics back to the LLM; chains of
    consecutive failed repair turns are capped at `max_repair_failures`
    so the campaign can recover from a bad lead by starting over from
    the original prompt."""

    def __init__(self,
                 generator: HarnessGenerator,
                 builder: HarnessBuilder,
                 target_successes: int = 5,
                 max_attempts: int = 50,
                 max_repair_failures: int = 2,
                 stderr_truncate: int = 4000,
                 min_source_chars: int = 20,
                 max_invalid_responses: int = 100,
                 verifier: Optional[HarnessVerifier] = None,
                 require_trigger: bool = True,
                 escalation_generator: Optional[HarnessGenerator] = None,
                 escalate_after: int = 0,
                 trigger_wrong_values: Optional[List[str]] = None):
        if target_successes < 1:
            raise ValueError("target_successes must be at least 1")
        if max_attempts < target_successes:
            raise ValueError(
                "max_attempts must be >= target_successes"
            )
        if max_repair_failures < 1:
            raise ValueError("max_repair_failures must be at least 1")
        if require_trigger and verifier is None:
            raise ValueError(
                "require_trigger=True needs a verifier to run the harness "
                "against the buggy checkout"
            )
        self.generator = generator
        self.builder = builder
        self.target_successes = target_successes
        self.max_attempts = max_attempts
        self.max_repair_failures = max_repair_failures
        self.stderr_truncate = stderr_truncate
        # Below this many non-whitespace chars, treat the extracted source
        # as an empty response rather than sending it to javac.
        self.min_source_chars = min_source_chars
        # A structurally-invalid response (prose, markdown, JUnit test,
        # main() demo — anything that isn't a FuzzHarness) means the model
        # didn't really answer, so it does NOT consume a `max_attempts`
        # slot: spending the generation budget on non-answers is exactly
        # the waste this gate exists to remove. But an unbounded free
        # retry would let a model that only ever emits prose loop forever,
        # so the total count of such rejects across the campaign is capped
        # here; hitting the cap ends the campaign like exhausting attempts.
        self.max_invalid_responses = max_invalid_responses
        self.verifier = verifier
        # When True, a harness must crash the buggy version to be
        # accepted (the new "compiles AND triggers" criterion). When
        # False, the campaign falls back to the old compile-only gate —
        # useful for ablation experiments.
        self.require_trigger = require_trigger
        # H3: the ACTUAL (wrong) values the real trigger tests observe on
        # the buggy build. When set, a lifted/test-copy check that fires
        # on buggy with a DIFFERENT observed value is rejected — its
        # scenario is not the test's scenario (setup divergence), and its
        # firing would measure the divergence, not the patch.
        self.trigger_wrong_values = trigger_wrong_values or []
        # Two-tier generation: if `escalation_generator` is set and the
        # primary produces no ACCEPTED harness after `escalate_after`
        # attempts, we swap to it for the rest of this bug (see run()).
        self.escalation_generator = escalation_generator
        self.escalate_after = escalate_after

    def run(self, messages: List[Dict[str, str]],
            buggy_dir: str,
            prompt_factory: Optional[PromptFactory] = None,
            patch_text: str = '') -> CampaignResult:
        """Run the generate → build → verify loop until `target_successes`
        harnesses are accepted or `max_attempts` is exhausted.

        `messages` is the initial (set-empty) prompt. If `prompt_factory`
        is given, every *fresh* attempt rebuilds the prompt from it using
        the current set-coverage state, so later harnesses are told what
        the set already covers. Without a factory the original prompt is
        reused unchanged (back-compatible behaviour).

        `patch_text` is included verbatim in the no-trigger repair message
        so the model can re-read exactly what changed when its harness
        compiles but doesn't crash the buggy version."""
        self._patch_text = patch_text
        result = CampaignResult(
            target_successes=self.target_successes,
            achieved_successes=0,
            attempts=0,
        )

        # Evolving set-coverage state, fed to prompt_factory on each
        # fresh attempt to drive variant analysis.
        covered_functions: List[str] = []
        covered_seen: set = set()
        found_signatures: List[str] = []
        # Union of the check-FAMILY stems of every accepted harness this leg
        # (see java_source.oracle_families). Steers later attempts toward an
        # UNCOVERED family and backs the family-novelty gate below.
        accepted_families: set = set()
        # Per-leg count of harnesses rejected for adding no new family. Bounds
        # the gate so it fails open rather than looping forever.
        novelty_rejections = 0
        pass_families = _factory_accepts_families(prompt_factory)

        def fresh_prompt() -> List[Dict[str, str]]:
            if prompt_factory is None:
                return list(messages)
            if pass_families:
                return prompt_factory(list(covered_functions),
                                      list(found_signatures),
                                      sorted(accepted_families))
            return prompt_factory(list(covered_functions),
                                  list(found_signatures))

        # The pristine prompt for the *current* set state. Rebuilt
        # whenever we reset (on acceptance or a cold repair chain) so the
        # fallback always reflects the latest coverage context.
        original_messages = fresh_prompt()
        current_messages = list(original_messages)
        # Consecutive failed repair turns since the last reset. The
        # fresh-prompt attempt itself isn't counted — only attempts that
        # already carried repair context.
        repair_failures = 0
        # Total structurally-invalid responses seen this campaign. Bounds
        # the "free retry" so a prose-only model can't loop forever.
        invalid_responses = 0

        # Two-tier escalation: once the primary model has burned
        # `escalate_after` consecutive real attempts without a NEW accepted
        # harness, switch to the stronger model for the rest of this bug.
        # nano matches the flagship's judgment when it converges but fails
        # to BUILD on hard bugs, so this recovers those cases at nano prices
        # for everything else. STALL-based, not zero-accepted-based: the
        # earlier "escalate only while 0 accepted" rule meant a single early
        # weak nano accept blocked escalation for the rest of the bug, and
        # the set's quality stayed capped by nano even as it stopped
        # producing. Already-escalated when there is no escalation model
        # configured.
        escalated = self.escalation_generator is None or self.escalate_after <= 0
        attempts_at_last_accept = 0

        while (result.achieved_successes < self.target_successes
               and result.attempts < self.max_attempts
               and invalid_responses < self.max_invalid_responses):
            if (not escalated
                    and result.attempts - attempts_at_last_accept
                        >= self.escalate_after):
                print(f"\n  [escalate] {result.attempts} attempts, "
                      f"{result.achieved_successes} accepted, none in the "
                      f"last {self.escalate_after} — switching to the "
                      "stronger model for the rest of this bug.")
                self.generator = self.escalation_generator
                escalated = True
                # Fresh start on the stronger model: don't make it inherit the
                # primary's failed repair chain.
                original_messages = fresh_prompt()
                current_messages = list(original_messages)
                repair_failures = 0
            is_repair_attempt = len(current_messages) > len(original_messages)

            raw = self.generator.generate(current_messages)

            # --- gate -1: structurally a harness at all? ----------------
            # Reject prose / markdown / JUnit-test / main()-demo responses
            # BEFORE counting an attempt or invoking javac. This is the
            # dominant 20B failure mode (a third of attempts 1-44), and
            # each one previously burned an attempt slot AND a javac run.
            # We still feed it back as a repair turn so the model is told
            # to return just the file, and after a cold chain we reset.
            invalid_reason = self.builder.looks_like_harness(raw)
            if invalid_reason is not None:
                invalid_responses += 1
                self._print_invalid_response(invalid_reason,
                                             invalid_responses)
                result.raw_responses.append(raw)
                repair_failures, current_messages, original_messages = (
                    self._handle_failure(
                        diagnostic=self._build_empty_response_message(),
                        raw=raw,
                        is_repair_attempt=is_repair_attempt,
                        repair_failures=repair_failures,
                        current_messages=current_messages,
                        original_messages=original_messages,
                        fresh_prompt=fresh_prompt,
                    )
                )
                continue

            result.attempts += 1
            attempt_label = f'attempt_{result.attempts:03d}'
            self._print_attempt_header(result.attempts, is_repair_attempt)
            self._print_raw(raw)

            source = self.builder.extract_source(raw)

            result.raw_responses.append(raw)

            # NB: the empty/near-empty-response case is now caught by the
            # structural gate (-1) above, before the attempt is counted,
            # via looks_like_harness() returning "empty response". The old
            # min_source_chars gate that used to live here is therefore
            # unreachable and has been removed; min_source_chars is kept on
            # the instance only for backward compatibility with callers
            # that read it.

            # --- gate 0: alarms must be able to escape (P0.2) -----------
            # An oracle throw inside the harness's own catch-everything
            # block is caught and discarded before Jazzer can see it —
            # the harness then looks "silent on buggy" and the acceptance
            # gate promotes a check that can never fire. Reject with a
            # targeted repair turn instead of compiling a deaf harness.
            swallow_reason = violation_swallowed(source)
            if swallow_reason is not None:
                print(f"✗ self-swallowed alarm: {swallow_reason}")
                repair_failures, current_messages, original_messages = (
                    self._handle_failure(
                        diagnostic=self._build_swallow_message(
                            swallow_reason),
                        raw=raw,
                        is_repair_attempt=is_repair_attempt,
                        repair_failures=repair_failures,
                        current_messages=current_messages,
                        original_messages=original_messages,
                        fresh_prompt=fresh_prompt,
                    )
                )
                continue

            # --- gate 0b: caught-crash re-throws must carry a cause (P0.3)
            # An alarm that re-labels a caught crash without attaching it
            # as the cause erases the crash's identity — attribution can
            # then no longer check it against the unpatched build (the
            # Chart-26 launder).
            cause_reason = rethrow_without_cause(source)
            if cause_reason is not None:
                print(f"✗ re-throw without cause: {cause_reason}")
                repair_failures, current_messages, original_messages = (
                    self._handle_failure(
                        diagnostic=(
                            "Your harness re-throws a caught exception as "
                            "its own alarm without preserving the "
                            "original: " + cause_reason + "\n"
                            "Return the full corrected FuzzHarness.java. "
                            "Raw Java source only. No markdown fences."),
                        raw=raw,
                        is_repair_attempt=is_repair_attempt,
                        repair_failures=repair_failures,
                        current_messages=current_messages,
                        original_messages=original_messages,
                        fresh_prompt=fresh_prompt,
                    )
                )
                continue

            # --- gate 0c: every alarm must carry an oracle ID (P0.4) ----
            # Un-named alarms cannot be told apart at acceptance, so a
            # check that never ran on buggy cannot be flagged before its
            # first-ever execution lands on the patched build.
            id_reason = alarm_ids_missing(source)
            if id_reason is not None:
                print(f"✗ unnamed oracle: {id_reason}")
                repair_failures, current_messages, original_messages = (
                    self._handle_failure(
                        diagnostic=(
                            "Every oracle alarm in your harness needs a "
                            "name: " + id_reason + "\n"
                            "Start each distinct check's message with "
                            "[oracle:<short-id>], e.g. `throw new "
                            "FuzzerSecurityIssueLow(\"[oracle:round-"
                            "trip] semantic mismatch: ...\")`. Use a "
                            "DIFFERENT id per check. Return the full "
                            "corrected FuzzHarness.java. Raw Java source "
                            "only. No markdown fences."),
                        raw=raw,
                        is_repair_attempt=is_repair_attempt,
                        repair_failures=repair_failures,
                        current_messages=current_messages,
                        original_messages=original_messages,
                        fresh_prompt=fresh_prompt,
                    )
                )
                continue

            # --- gate 0d: no negative-modulo index (harness bug) --------
            # Math.abs(Integer.MIN_VALUE) is negative; a harness that
            # indexes with Math.abs(consume…()) % n eventually crashes on
            # its own and the junk firing costs the leg its verdict.
            negmod_reason = negative_modulo_index(source)
            if negmod_reason is not None:
                print(f"✗ negative-modulo index: {negmod_reason}")
                repair_failures, current_messages, original_messages = (
                    self._handle_failure(
                        diagnostic=(
                            "Your harness computes an array/list index "
                            "that can go NEGATIVE: " + negmod_reason + "\n"
                            "Return the full corrected FuzzHarness.java. "
                            "Raw Java source only. No markdown fences."),
                        raw=raw,
                        is_repair_attempt=is_repair_attempt,
                        repair_failures=repair_failures,
                        current_messages=current_messages,
                        original_messages=original_messages,
                        fresh_prompt=fresh_prompt,
                    )
                )
                continue

            # --- gate 0e: no evidence-destroying boolean-swallow (L) -----
            # A harness that catches a crash into a bare flag
            # (catch { success = false; }) and alarms on the flag
            # (if (!success) throw ...) has flattened the caught exception's
            # identity into `false` — attribution can no longer tell which
            # exception fired or whether the unpatched build crashes the same
            # way (the Chart-26 launder in a different disguise). Reject and
            # ask for a regenerate rather than compile a deaf-to-identity
            # harness.
            boolswallow_reason = boolean_swallow(source)
            if boolswallow_reason is not None:
                print(f"✗ evidence-destroying alarm: {boolswallow_reason}")
                repair_failures, current_messages, original_messages = (
                    self._handle_failure(
                        diagnostic=(
                            "Your harness catches an exception into a bare "
                            "boolean/flag and then raises its alarm on that "
                            "flag: " + boolswallow_reason + "\n"
                            "A caught exception is a REJECTION, never a "
                            "violation, and flattening it into a flag "
                            "destroys the identity attribution needs. Either "
                            "assert on the real observable the test pins "
                            "(compute the actual value and compare it), or "
                            "if you must re-report a caught crash, re-throw "
                            "it with the original as the cause: `new "
                            "FuzzerSecurityIssueLow(\"[oracle:<id>] ...\", "
                            "e)`. Return the full corrected FuzzHarness.java. "
                            "Raw Java source only. No markdown fences."),
                        raw=raw,
                        is_repair_attempt=is_repair_attempt,
                        repair_failures=repair_failures,
                        current_messages=current_messages,
                        original_messages=original_messages,
                        fresh_prompt=fresh_prompt,
                    )
                )
                continue

            build = self.builder.build(
                source, buggy_dir,
                output_subdir=attempt_label,
            )

            result.results.append(build)

            # --- gate 1: must compile ---------------------------------
            if not build.compiled:
                self._print_failure(build)
                repair_failures, current_messages, original_messages = (
                    self._handle_failure(
                        diagnostic=self._build_repair_message(build.stderr),
                        raw=raw,
                        is_repair_attempt=is_repair_attempt,
                        repair_failures=repair_failures,
                        current_messages=current_messages,
                        original_messages=original_messages,
                        fresh_prompt=fresh_prompt,
                    )
                )
                continue

            # --- gate 2: must trigger on the buggy version ------------
            if self.require_trigger:
                verification = self.verifier.verify(build)
                if not verification.crashed:
                    self._print_no_trigger(verification)
                    repair_failures, current_messages, original_messages = (
                        self._handle_failure(
                            diagnostic=self._build_no_trigger_message(
                                verification,
                                patch_text=getattr(self, '_patch_text', '')),
                            raw=raw,
                            is_repair_attempt=is_repair_attempt,
                            repair_failures=repair_failures,
                            current_messages=current_messages,
                            original_messages=original_messages,
                            fresh_prompt=fresh_prompt,
                        )
                    )
                    continue
            else:
                verification = None

            # --- gate 3 (H3): a test-copy check must observe the REAL
            #     wrong value. The trigger tests' failure messages name
            #     the exact value the buggy build produces; a lifted
            #     check firing with a DIFFERENT observed value has
            #     rebuilt the scenario wrong, and that difference — not
            #     the patch — is what it measures (the Closure-62-c /
            #     Closure-73-c false-alarm class). Abstains unless both
            #     values are extractable — a reject here feeds the
            #     normal repair loop with the exact divergence.
            if verification is not None and self.trigger_wrong_values:
                # max_len=4000: the default 200-char headline truncates
                # BEFORE the observed value in long lifted-check messages
                # (Closure-62's multi-line expected/actual) — the H3 gate
                # then silently abstains on exactly the firings it exists
                # to test.
                _headline = exception_headline(
                    verification.stdout + '\n' + verification.stderr,
                    max_len=4000) or ''
                _observed = lifted_observed_mismatch(
                    _headline, self.trigger_wrong_values)
                if _observed is not None:
                    _reals = '; '.join(self.trigger_wrong_values)
                    print(f"✗ H3 setup-fidelity gate: lifted check observed "
                          f"{_observed!r} on the buggy build, but the real "
                          f"test observes {_reals!r}")
                    record_event(
                        'deterministic', method='harness-attempt',
                        target=attempt_label,
                        output='REJECTED (H3: lifted check observed a '
                               'different wrong value than the real test '
                               '— setup divergence)',
                        detail={'observed': _observed,
                                'real_wrong_values':
                                    self.trigger_wrong_values,
                                'headline': _headline})
                    repair_failures, current_messages, original_messages = (
                        self._handle_failure(
                            diagnostic=(
                                "Your test-copy check fired on the buggy "
                                "build, but it observed a DIFFERENT wrong "
                                f"value ({_observed}) than the real test "
                                f"observes there ({_reals}). That means "
                                "your harness rebuilds the test's scenario "
                                "differently from the test itself — the "
                                "firing measures your setup divergence, "
                                "not the bug. Replicate the test's setup "
                                "EXACTLY (helpers, registered files, "
                                "constants — see <test_support>), or drop "
                                "the check you cannot set up faithfully. "
                                "Return the full corrected "
                                "FuzzHarness.java. Raw Java source only. "
                                "No markdown fences."),
                            raw=raw,
                            is_repair_attempt=is_repair_attempt,
                            repair_failures=repair_failures,
                            current_messages=current_messages,
                            original_messages=original_messages,
                            fresh_prompt=fresh_prompt,
                        )
                    )
                    continue

            # --- gate 4 (family novelty): a later harness must add a NEW
            #     check FAMILY. The width5 20260725 run showed later harnesses
            #     re-skinning an earlier check under a new id (same observable,
            #     new name) — no new evidence, and a set that only probes one
            #     observable is blind to a patch that leaves a DIFFERENT
            #     quantity wrong. Once at least one harness is accepted, a
            #     compiled-and-fired attempt whose every check family is
            #     already covered is bounced back with a targeted repair turn.
            #     Bounded: after two such rejections this leg the gate fails
            #     open and accepts the redundant harness, so the campaign can
            #     always finish.
            harness_families = oracle_families(source)
            families_added = harness_families - accepted_families
            if accepted_families and not harness_families:
                # FAIL-OPEN BY CONSTRUCTION: an EMPTY extracted family set is a
                # parse failure, not redundancy — the gate cannot judge what it
                # could not read, so it must never reject on it (this is exactly
                # what produced night20's six vacuous rejections). Log WHY, then
                # fall through accept-eligible. novelty_verdict independently
                # returns 'accept' for an empty set; this branch never reaches
                # it, so a vacuous rejection is impossible on two counts.
                print("✓ [novelty] family extraction empty — fail-open "
                      "accept (family-extract-failed)")
                record_event(
                    'deterministic', method='harness-attempt',
                    target=attempt_label,
                    output='family-extract-failed (empty family set — '
                           'accepted fail-open; the novelty gate cannot judge '
                           'checks it could not parse, e.g. a dynamic oracle '
                           'id)',
                    detail={'harness_families': [],
                            'accepted_families': sorted(accepted_families)})
            elif accepted_families:
                if (novelty_verdict(harness_families, accepted_families,
                                    novelty_rejections) == 'reject'):
                    novelty_rejections += 1
                    mine = ', '.join(sorted(harness_families)) or '(none)'
                    print(f"✗ [novelty] no new check family (covers "
                          f"{{{mine}}}, all already accepted) — rejecting "
                          f"({novelty_rejections}/2)")
                    record_event(
                        'deterministic', method='harness-attempt',
                        target=attempt_label,
                        output='REJECTED (family-novelty: adds no check '
                               'family beyond the accepted harnesses)',
                        detail={'harness_families': sorted(harness_families),
                                'accepted_families': sorted(accepted_families),
                                'novelty_rejections': novelty_rejections})
                    repair_failures, current_messages, original_messages = (
                        self._handle_failure(
                            diagnostic=self._build_novelty_message(
                                harness_families, accepted_families),
                            raw=raw,
                            is_repair_attempt=is_repair_attempt,
                            repair_failures=repair_failures,
                            current_messages=current_messages,
                            original_messages=original_messages,
                            fresh_prompt=fresh_prompt,
                        )
                    )
                    continue
                if not families_added:
                    # verdict == 'accept' with no new family: the budget is
                    # spent, so we accept a redundant harness rather than loop.
                    print("[novelty] cap reached — accepting redundant "
                          "harness")
                    record_event(
                        'deterministic', method='harness-attempt',
                        target=attempt_label,
                        output='novelty-cap fail-open (budget spent — '
                               'redundant harness admitted so the campaign '
                               'can finish)',
                        detail={'harness_families': sorted(harness_families),
                                'accepted_families': sorted(accepted_families),
                                'novelty_rejections': novelty_rejections})
                else:
                    # passed the novelty gate on real new evidence: record the
                    # compact pass so every non-trivial gate decision survives
                    # in the trace, not only the rejections.
                    print(f"✓ [novelty] adds {len(families_added)} new check "
                          f"family(ies)")
                    record_event(
                        'deterministic', method='harness-attempt',
                        target=attempt_label,
                        output='novelty-gate PASS (adds new check families)',
                        detail={'families_added': sorted(families_added),
                                'families_added_count': len(families_added)})

            # --- accepted ---------------------------------------------
            result.successful_results.append(build)
            result.achieved_successes += 1
            attempts_at_last_accept = result.attempts
            record_event('deterministic', method='harness-attempt',
                         target=attempt_label,
                         output='ACCEPTED (compiles + crashes the buggy build)',
                         trigger=(exception_headline(
                             verification.stdout + '\n' + verification.stderr)
                             if verification else ''))
            signature = verification.signature if verification else None
            result.accepted_signatures.append(signature or '')
            # Attribute the trigger: WHICH throwable fired on the buggy
            # version. Downstream analysis reads this to tell "accepted
            # via the reported symptom" (a band-aid patch will silence
            # it) from "accepted via an independent consistency check"
            # (survives one) — the distinction that decides whether the
            # set can catch masked-symptom overfits.
            detail = ''
            if verification is not None:
                detail = exception_headline(
                    verification.stdout + '\n' + verification.stderr) or ''
            result.accepted_trigger_details.append(detail)

            # Fold this harness's coverage into the set state so the next
            # fresh prompt steers elsewhere.
            if verification:
                for fn in verification.reached_functions:
                    if fn not in covered_seen:
                        covered_seen.add(fn)
                        covered_functions.append(fn)
                if signature and signature not in found_signatures:
                    found_signatures.append(signature)
            # Fold the accepted harness's check FAMILIES so the next attempt
            # is steered off them and the novelty gate measures against them.
            accepted_families |= harness_families

            self._print_success(result, build, signature, detail)

            # Reset to a fresh prompt that now reflects the updated set
            # coverage — this is what makes attempt N+1 a *variant* of
            # the accepted set rather than a blind resample.
            original_messages = fresh_prompt()
            current_messages = list(original_messages)
            repair_failures = 0

        return result

    def _handle_failure(self, diagnostic, raw, is_repair_attempt,
                        repair_failures, current_messages,
                        original_messages, fresh_prompt):
        """Shared failure bookkeeping for both gates (compile, trigger).
        Returns the updated (repair_failures, current_messages,
        original_messages) triple."""
        # Every gate rejection funnels through here — record it so the trace
        # shows what happened to each generated harness.
        record_event('deterministic', method='harness-attempt',
                     output='REJECTED',
                     reason=str(diagnostic).strip().splitlines()[0][:200]
                     if diagnostic else '')
        if is_repair_attempt:
            repair_failures += 1

        if repair_failures >= self.max_repair_failures:
            # Chain has gone cold. Start over from a fresh prompt (which
            # reflects current set coverage) rather than the wrong path
            # the LLM committed to.
            self._print_reset(repair_failures)
            original_messages = fresh_prompt()
            current_messages = list(original_messages)
            repair_failures = 0
        else:
            # Feed the failure back as a repair turn: the model sees its
            # own previous attempt followed by the diagnostic, so it
            # fixes rather than blindly resamples.
            current_messages = current_messages + [
                {'role': 'assistant', 'content': raw},
                {'role': 'user', 'content': diagnostic},
            ]
        return repair_failures, current_messages, original_messages

    # --- repair turn -----------------------------------------------------

    def _build_empty_response_message(self) -> str:
        """The user message that follows an empty/near-empty completion.
        Asks for just the file, in case the previous turn ran out of
        output budget on reasoning."""
        return (
            "Your previous response contained no usable Java source. "
            "Return the complete FuzzHarness.java now and nothing else: "
            "raw Java only, no markdown fences, no commentary, public "
            "class named FuzzHarness, entrypoint exactly\n"
            "    public static void fuzzerTestOneInput("
            "com.code_intelligence.jazzer.api.FuzzedDataProvider data)"
        )

    def _build_repair_message(self, stderr: str) -> str:
        """The user message that follows a failed compile. We restate
        the hard constraints because the LLM has been known to drop
        the no-fences rule, the FuzzHarness class name, or the exact
        entrypoint signature once it starts iterating on a fix."""
        truncated = stderr[:self.stderr_truncate]
        if len(stderr) > self.stderr_truncate:
            truncated += '\n... (truncated)'
        return (
            "That did not compile. javac reported:\n"
            f"{truncated}\n\n"
            "Return the full corrected FuzzHarness.java. Rules:\n"
            "- Raw Java source only. No markdown fences. No prose. "
            "No explanations. The file starts with a comment or "
            "package statement and ends with a closing brace.\n"
            "- Public class named exactly `FuzzHarness`.\n"
            "- Entrypoint exactly:\n"
            "    public static void fuzzerTestOneInput("
            "com.code_intelligence.jazzer.api.FuzzedDataProvider data)\n"
            "- Use only the FuzzedDataProvider methods listed in the "
            "original instructions. Do NOT invent methods like "
            "getInt(), consumeDouble(), getRemainingSize(), or "
            "consumeIntInRange() — they do not exist.\n"
            "- Do NOT use classes or methods that are not on the "
            "project classpath. If javac says 'cannot find symbol', "
            "remove that import and use only classes visible in the "
            "source_imports block or java.* / java.awt.*."
        )

    def _build_novelty_message(self, harness_families,
                               accepted_families) -> str:
        """Repair turn for a harness that compiled and fired but whose every
        check family is already covered by an accepted harness (gate 4)."""
        covered = ', '.join(sorted(accepted_families)) or '(none)'
        mine = ', '.join(sorted(harness_families)) or '(none)'
        return (
            "That compiled and fired on the buggy build, but every check "
            f"FAMILY in this harness ({{{mine}}}) is already covered by an "
            f"accepted harness (covered families: {{{covered}}}). Re-skinning "
            "an already-covered family under a new name adds no new evidence: "
            "a set of harnesses that all probe the SAME observable is blind "
            "to a patch that leaves a DIFFERENT quantity wrong. Invent checks "
            "in an UNCOVERED family — cover a different observable, a sibling "
            "method, or a different receiver-state axis than the families "
            "listed above. Return the full corrected FuzzHarness.java. Raw "
            "Java source only. No markdown fences."
        )

    def _build_swallow_message(self, reason: str) -> str:
        """Repair turn for a harness whose alarm throw can never escape
        its own error handling (P0.2)."""
        return (
            "Your harness has a fatal structural problem: "
            f"{reason}\n\n"
            "The violation/oracle throw is your harness's ONLY way to "
            "report a finding. If it is thrown inside a try whose catch "
            "clause catches Throwable/Exception/RuntimeException and "
            "returns, the finding is silently discarded and the harness "
            "can never fire at all.\n\n"
            "Return the full corrected FuzzHarness.java. Keep try/catch "
            "ONLY around the code that CALLS the library (to skip invalid "
            "inputs), and either:\n"
            "- move the violation check and its throw AFTER/OUTSIDE that "
            "try block, or\n"
            "- add `catch (RuntimeException e) { if "
            "(String.valueOf(e.getMessage()).contains(\"violated\")) "
            "throw e; }` BEFORE any broader catch, so the alarm is "
            "re-thrown while input-rejection noise is still skipped.\n"
            "Raw Java source only. No markdown fences. No prose."
        )

    def _build_no_trigger_message(self,
                                  verification: VerificationResult,
                                  patch_text: str = '') -> str:
        """The user message that follows a harness that compiled but did
        NOT crash the buggy version.

        The goal of this turn is narrow: get the *next* harness to crash
        the known-buggy checkout. We give the model three things, in
        decreasing order of reliability:

          1. The patch diff, as ground truth for what behaviour differs
             between buggy and fixed code (no editorial claim about what
             *kind* of bug it is — that varies per bug and a wrong guess
             here actively misleads the model).
          2. What its last harness actually reached on the buggy code
             (from the verifier), so it knows whether it was close to the
             fault or in the wrong region entirely.
          3. A short, bug-agnostic checklist for turning "reached the
             code" into "crashed the code".

        Signature is unchanged so the call site needs no edits."""
        # Why no crash: a timeout means the harness ran the full budget
        # without Jazzer finding anything (often: it never drove input
        # into the changed code, or always took a safe branch); a clean
        # exit means Jazzer returned with no finding (often: every input
        # was handled normally, or an exception was caught and swallowed
        # inside the harness).
        if verification.timed_out:
            why = ("It ran for the entire time budget on the buggy code "
                   "and Jazzer reported no crash. Most often this means "
                   "the inputs you generated never drove execution into "
                   "the changed code, or always took a path that does not "
                   "fault.")
        else:
            why = ("Jazzer exited cleanly with no finding on the buggy "
                   "code. Most often this means every input was handled "
                   "normally, or the harness itself caught and swallowed "
                   "the exception that should have propagated.")

        # The single most actionable signal we have: which project
        # functions the last harness demonstrably entered on the buggy
        # code. Empty => it never reached the changed region, so the fix
        # is to construct inputs that get there at all. Non-empty => it is
        # in the right area and needs an input that pushes the fault.
        reached = getattr(verification, 'reached_functions', None) or []
        if reached:
            shown = ', '.join(reached[:8])
            coverage_note = (
                "Your last harness DID reach project code on the buggy "
                f"version — it entered: {shown}. So you are close: the "
                "fault is near here. Adjust the input so it pushes one of "
                "these calls past the boundary the patch changed, rather "
                "than relocating to a different API.\n\n"
            )
        else:
            coverage_note = (
                "Your last harness did NOT reach any project code on the "
                "buggy version that we could observe — it likely threw or "
                "returned before getting into the changed method. First "
                "priority: construct an input that actually calls the "
                "patched method with arguments that get past its initial "
                "validation, so execution reaches the changed lines at "
                "all.\n\n"
            )

        patch_reminder = (
            f"The patch under analysis is below. The buggy version is the "
            f"code BEFORE this patch is applied (the '+' lines are the "
            f"FIXED code; the buggy code has the '-' lines instead):\n"
            f"{patch_text}\n\n"
            if patch_text else ""
        )

        return (
            "That compiled, but it did NOT trigger the bug on the known-"
            f"buggy version. {why}\n\n"
            f"{patch_reminder}"
            f"{coverage_note}"
            "To make the next harness crash the BUGGY version, work "
            "through this:\n\n"
            "  1. From the diff, identify the exact behavioural difference "
            "between buggy and fixed code — which inputs are handled "
            "differently. The crash you want exists only on the buggy "
            "side, so target inputs that hit that difference. Do not "
            "assume the bug is any particular shape (missing bounds check, "
            "wrong branch, off-by-one, null handling, etc.) — read the "
            "diff and let it tell you.\n"
            "  2. Choose input that reaches the changed code AND drives it "
            "into the faulting state. Use the FuzzedDataProvider to "
            "produce values in the range that exercises the difference, "
            "not arbitrary values that are likely rejected early.\n"
            "  3. Do NOT rely on an exception that BOTH versions throw "
            "(e.g. validation that exists in buggy and fixed alike). That "
            "fires on the patched code too and so cannot distinguish them. "
            "You want a fault that the buggy code reaches and the fixed "
            "code prevents.\n"
            "  4. CRITICAL — let the crash escape. Do NOT wrap the call in "
            "a try/catch that swallows the throwable you want Jazzer to "
            "report. Catch ONLY checked exceptions the signature forces "
            "you to handle, and rethrow them as RuntimeException; let every "
            "unchecked exception propagate out of fuzzerTestOneInput so "
            "Jazzer can see it.\n\n"
            "Return the full corrected FuzzHarness.java — raw Java only, "
            "no fences, public class FuzzHarness, entrypoint exactly\n"
            "    public static void fuzzerTestOneInput("
            "com.code_intelligence.jazzer.api.FuzzedDataProvider data)"
        )

    # --- logging ---------------------------------------------------------

    def _print_attempt_header(self, n: int, is_repair: bool) -> None:
        tag = ' (repair turn)' if is_repair else ''
        print(f"\n{'=' * 20} attempt {n:03d}/{self.max_attempts}"
              f"{tag} {'=' * 20}")

    def _print_raw(self, raw: str) -> None:
        print("--- raw LLM response ---")
        print(raw)

    def _print_success(self, result: CampaignResult,
                       build: BuildResult,
                       signature: Optional[str] = None,
                       detail: str = '') -> None:
        gate = "compiled + triggered" if self.require_trigger else "compiled"
        sig = f"  [crash: {signature}]" if signature else ""
        print(f"✓ {gate} at {build.harness_path}  "
              f"({result.achieved_successes}/"
              f"{self.target_successes} successes, "
              f"{result.attempts} attempts so far){sig}")
        if detail:
            print(f"  [trigger: {detail}]")

    def _print_failure(self, build: BuildResult) -> None:
        print(f"✗ javac failed (rc={build.returncode})")
        if build.stderr:
            print("--- javac stderr ---")
            print(build.stderr)

    def _print_empty_response(self, source: str) -> None:
        n = len(source.strip())
        print(f"✗ empty LLM response ({n} non-whitespace chars) — "
              "no usable source, rejecting")

    def _print_invalid_response(self, reason: str, total: int) -> None:
        print(f"\n{'=' * 20} invalid response "
              f"({total}/{self.max_invalid_responses}) {'=' * 20}")
        print(f"✗ not a harness ({reason}) — regenerating "
              "without spending an attempt")

    def _print_no_trigger(self, verification: VerificationResult) -> None:
        if verification.timed_out:
            print("✗ compiled but did NOT trigger on the buggy version "
                  "(timed out) — rejecting")
        else:
            print(f"✗ compiled but did NOT trigger on the buggy version "
                  f"(rc={verification.returncode}) — rejecting")

    def _print_reset(self, failures: int) -> None:
        print(f"⟲ {failures} repair turns failed in a row — "
              "resetting to a fresh prompt")