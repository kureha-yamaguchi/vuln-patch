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
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Dict, Optional, Callable

from build import HarnessBuilder, BuildResult
sys.path.insert(0, str(Path(__file__).parent.parent))
from llm import HarnessGenerator
from fuzz_runner import HarnessVerifier, VerificationResult


# A prompt factory takes the current set-coverage state (the functions
# already exercised by accepted harnesses, and the crash signatures
# already found) and returns fresh chat-completion messages. This is how
# variant-analysis context is injected without the campaign needing to
# know how prompts are assembled.
PromptFactory = Callable[[List[str], List[str]], List[Dict[str, str]]]


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
                 require_trigger: bool = True):
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

        def fresh_prompt() -> List[Dict[str, str]]:
            if prompt_factory is None:
                return list(messages)
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

        while (result.achieved_successes < self.target_successes
               and result.attempts < self.max_attempts
               and invalid_responses < self.max_invalid_responses):
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

            # --- accepted ---------------------------------------------
            result.successful_results.append(build)
            result.achieved_successes += 1
            signature = verification.signature if verification else None
            result.accepted_signatures.append(signature or '')

            # Fold this harness's coverage into the set state so the next
            # fresh prompt steers elsewhere.
            if verification:
                for fn in verification.reached_functions:
                    if fn not in covered_seen:
                        covered_seen.add(fn)
                        covered_functions.append(fn)
                if signature and signature not in found_signatures:
                    found_signatures.append(signature)

            self._print_success(result, build, signature)

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
                       signature: Optional[str] = None) -> None:
        gate = "compiled + triggered" if self.require_trigger else "compiled"
        sig = f"  [crash: {signature}]" if signature else ""
        print(f"✓ {gate} at {build.harness_path}  "
              f"({result.achieved_successes}/"
              f"{self.target_successes} successes, "
              f"{result.attempts} attempts so far){sig}")

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