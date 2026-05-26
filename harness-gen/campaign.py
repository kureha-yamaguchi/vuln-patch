"""Regenerate a harness until a target number of attempts compile.

A campaign reuses the same prompt and the same buggy checkout, so the
only thing varying across attempts is the LLM's sampling. Each
successful attempt is written to its own subdirectory under
`<buggy_dir>/fuzz/attempt_NNN/` so they don't clobber each other and
can be evaluated downstream (coverage of the patched lines, ability
to crash on the buggy version, etc.).
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
    until `max_attempts` is exhausted."""

    def __init__(self,
                 generator: HarnessGenerator,
                 builder: HarnessBuilder,
                 target_successes: int = 5,
                 max_attempts: int = 50):
        if target_successes < 1:
            raise ValueError("target_successes must be at least 1")
        if max_attempts < target_successes:
            raise ValueError(
                "max_attempts must be >= target_successes"
            )
        self.generator = generator
        self.builder = builder
        self.target_successes = target_successes
        self.max_attempts = max_attempts

    def run(self, messages: List[Dict[str, str]],
            buggy_dir: str) -> CampaignResult:
        result = CampaignResult(
            target_successes=self.target_successes,
            achieved_successes=0,
            attempts=0,
        )

        while (result.achieved_successes < self.target_successes
               and result.attempts < self.max_attempts):
            result.attempts += 1
            attempt_label = f'attempt_{result.attempts:03d}'
            self._print_attempt_header(result.attempts)

            raw = self.generator.generate(messages)
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
            else:
                self._print_failure(build)

        return result

    # --- logging ---------------------------------------------------------

    def _print_attempt_header(self, n: int) -> None:
        print(f"\n{'=' * 20} attempt {n:03d}/{self.max_attempts} "
              f"{'=' * 20}")

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