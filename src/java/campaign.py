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
        self.verifier = verifier
        # When True, a harness must crash the buggy version to be
        # accepted (the new "compiles AND triggers" criterion). When
        # False, the campaign falls back to the old compile-only gate —
        # useful for ablation experiments.
        self.require_trigger = require_trigger

    def run(self, messages: List[Dict[str, str]],
            buggy_dir: str,
            prompt_factory: Optional[PromptFactory] = None) -> CampaignResult:
        """Run the generate → build → verify loop until `target_successes`
        harnesses are accepted or `max_attempts` is exhausted.

        `messages` is the initial (set-empty) prompt. If `prompt_factory`
        is given, every *fresh* attempt rebuilds the prompt from it using
        the current set-coverage state, so later harnesses are told what
        the set already covers. Without a factory the original prompt is
        reused unchanged (back-compatible behaviour)."""
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

        while (result.achieved_successes < self.target_successes
               and result.attempts < self.max_attempts):
            result.attempts += 1
            attempt_label = f'attempt_{result.attempts:03d}'
            is_repair_attempt = len(current_messages) > len(original_messages)
            self._print_attempt_header(result.attempts, is_repair_attempt)

            raw = self.generator.generate(current_messages)
            self._print_raw(raw)

            source = self.builder.extract_source(raw)
            build = self.builder.build(
                source, buggy_dir,
                output_subdir=attempt_label,
            )

            result.raw_responses.append(raw)
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
                                verification),
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
            "Return the full corrected FuzzHarness.java. Same rules as "
            "before: raw Java source only, no markdown fences, no "
            "commentary, public class named FuzzHarness, entrypoint "
            "exactly\n"
            "    public static void fuzzerTestOneInput("
            "com.code_intelligence.jazzer.api.FuzzedDataProvider data)\n"
            "and only the FuzzedDataProvider methods listed in the "
            "original instructions."
        )

    def _build_no_trigger_message(self,
                                  verification: VerificationResult) -> str:
        """The user message that follows a harness that compiled but did
        NOT crash the buggy version. The harness is syntactically fine
        but isn't reaching the root cause, so we tell the model exactly
        that and point it back at the patch + failing-test inputs."""
        if verification.timed_out:
            why = ("It ran for the full time budget on the buggy code "
                   "without finding any crash.")
        else:
            why = ("Jazzer exited cleanly (no finding) on the buggy "
                   "code.")
        return (
            "That compiled, but it did NOT trigger the bug on the known-"
            f"buggy version. {why}\n\n"
            "A harness is only useful here if it actually drives the "
            "patched code path into the faulty behaviour — otherwise it "
            "cannot test whether a patch fixes the root cause. Revise "
            "the harness so the values from FuzzedDataProvider reach the "
            "touched function(s) along the buggy path. Re-read the patch "
            "and the failing-test inputs shown earlier: feed shapes of "
            "input equivalent to what that test constructs. Return the "
            "full corrected FuzzHarness.java — raw Java only, no fences, "
            "public class FuzzHarness, entrypoint exactly\n"
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