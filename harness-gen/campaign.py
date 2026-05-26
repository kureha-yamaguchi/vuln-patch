"""Regenerate a harness until a target number of attempts compile.

A campaign reuses the same prompt and the same buggy checkout, so the
only thing varying across attempts is the LLM's sampling. Each
successful attempt is written to its own subdirectory under
`<buggy_dir>/fuzz/attempt_NNN/` so they don't clobber each other and
can be evaluated downstream (coverage of the patched lines, ability
to crash on the buggy version, etc.).

After a failed compile we feed the javac diagnostics back to the LLM
as a repair turn — in practice most "almost right" outputs (wrong
constructor arity, missing import, uncaught checked exception)
compile on the second try. To stop a poisoned chain from dominating
the campaign we reset back to the original prompt after a successful
compile or after `max_repair_failures` failed repair turns in a row.
"""
from dataclasses import dataclass, field
from typing import List, Dict

from build import HarnessBuilder, BuildResult
from llm import HarnessGenerator


@dataclass
class CampaignResult:
    target_successes: int
    achieved_successes: int
    attempts: int
    results: List[BuildResult] = field(default_factory=list)
    successful_results: List[BuildResult] = field(default_factory=list)
    raw_responses: List[str] = field(default_factory=list)

    @property
    def converged(self) -> bool:
        """True iff we hit the target before exhausting max_attempts."""
        return self.achieved_successes >= self.target_successes

    @property
    def success_rate(self) -> float:
        return self.achieved_successes / max(1, self.attempts)


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
                 stderr_truncate: int = 4000):
        if target_successes < 1:
            raise ValueError("target_successes must be at least 1")
        if max_attempts < target_successes:
            raise ValueError(
                "max_attempts must be >= target_successes"
            )
        if max_repair_failures < 1:
            raise ValueError("max_repair_failures must be at least 1")
        self.generator = generator
        self.builder = builder
        self.target_successes = target_successes
        self.max_attempts = max_attempts
        self.max_repair_failures = max_repair_failures
        self.stderr_truncate = stderr_truncate

    def run(self, messages: List[Dict[str, str]],
            buggy_dir: str) -> CampaignResult:
        result = CampaignResult(
            target_successes=self.target_successes,
            achieved_successes=0,
            attempts=0,
        )

        # The pristine prompt we always fall back to. Never mutated.
        original_messages = list(messages)
        # The working copy that grows with repair turns and is reset on
        # success or when max_repair_failures is hit.
        current_messages = list(original_messages)
        # How many consecutive repair turns have failed since the last
        # reset. The original-prompt attempt itself isn't counted here
        # — only attempts that already carried repair context into the
        # LLM. So a fresh prompt failing once, then 3 repair turns
        # failing, triggers the reset on the 3rd repair failure.
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

            if build.compiled:
                result.successful_results.append(build)
                result.achieved_successes += 1
                self._print_success(result, build)
                # Reset on success so the next attempt is anchored to
                # the prompt that just worked, not a stale repair
                # chain we happen to have accumulated.
                current_messages = list(original_messages)
                repair_failures = 0
                continue

            self._print_failure(build)

            if is_repair_attempt:
                repair_failures += 1

            if repair_failures >= self.max_repair_failures:
                # The chain has gone cold. Start over from the
                # original prompt so the next sample isn't anchored to
                # whatever wrong path the LLM committed to earlier.
                self._print_reset(repair_failures)
                current_messages = list(original_messages)
                repair_failures = 0
            else:
                # Feed the failure back as a repair turn for the next
                # iteration. We append rather than replace so the LLM
                # sees its own previous attempt followed by the
                # diagnostic — this is what lets it "fix" rather than
                # blindly resample.
                current_messages = current_messages + [
                    {'role': 'assistant', 'content': raw},
                    {'role': 'user', 'content':
                        self._build_repair_message(build.stderr)},
                ]

        return result

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

    # --- logging ---------------------------------------------------------

    def _print_attempt_header(self, n: int, is_repair: bool) -> None:
        tag = ' (repair turn)' if is_repair else ''
        print(f"\n{'=' * 20} attempt {n:03d}/{self.max_attempts}"
              f"{tag} {'=' * 20}")

    def _print_raw(self, raw: str) -> None:
        print("--- raw LLM response ---")
        print(raw)

    def _print_success(self, result: CampaignResult,
                       build: BuildResult) -> None:
        print(f"✓ compiled at {build.harness_path}  "
              f"({result.achieved_successes}/"
              f"{self.target_successes} successes, "
              f"{result.attempts} attempts so far)")

    def _print_failure(self, build: BuildResult) -> None:
        print(f"✗ javac failed (rc={build.returncode})")
        if build.stderr:
            print("--- javac stderr ---")
            print(build.stderr)

    def _print_reset(self, failures: int) -> None:
        print(f"⟲ {failures} repair turns failed in a row — "
              "resetting to the original prompt")