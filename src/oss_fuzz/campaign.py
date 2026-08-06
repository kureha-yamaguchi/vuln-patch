"""Generate → build → verify loop for libFuzzer harnesses.

Compact C-side analogue of the Java HarnessCampaign: keep asking the LLM for a
harness, build it against the *vulnerable* checkout, and accept it only if it
actually crashes there (the trigger gate). On a build failure we feed the
compiler stderr back for repair; on a compile-but-no-crash we re-steer via the
prompt factory toward an uncovered part of the root-cause region. Acceptance =
"compiles AND triggers on the vulnerable build", exactly as in the Java flow.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Callable, Dict, List, Optional

from oss_fuzz.ossfuzz import OssFuzz, Checkout, HarnessPlacement, RunOutcome

_FENCE_RE = re.compile(r"```(?:c|cc|cpp|c\+\+)?\s*\n(.*?)```", re.DOTALL)


@dataclass
class GenResult:
    harness_name: str
    source: str
    ext: str
    signature: Optional[str] = None
    covered: List[str] = field(default_factory=list)


@dataclass
class CampaignResult:
    successful: List[GenResult] = field(default_factory=list)
    attempts: int = 0
    target_successes: int = 5
    # Set when the campaign stopped because the build environment could not
    # build ANY harness (see ossfuzz._infra_error). Distinguishes "the method
    # found nothing" from "the run never got off the ground".
    infra_error: Optional[str] = None

    @property
    def achieved(self) -> int:
        return len(self.successful)

    @property
    def converged(self) -> bool:
        return self.achieved >= self.target_successes

    @property
    def signatures(self) -> List[str]:
        return [g.signature for g in self.successful if g.signature]

    @property
    def covered(self) -> List[str]:
        out: List[str] = []
        for g in self.successful:
            out.extend(g.covered)
        return out


def extract_source(llm_response: str) -> Optional[str]:
    """Pull the single fenced code block out of the model reply."""
    m = _FENCE_RE.search(llm_response)
    if m:
        return m.group(1).strip()
    # No fence: accept the raw reply if it looks like a harness.
    if "LLVMFuzzerTestOneInput" in llm_response:
        return llm_response.strip()
    return None


class HarnessCampaign:
    def __init__(self, generator, oss_fuzz: OssFuzz, project: str,
                 vuln_checkout: Checkout, sanitizer: str, ext: str,
                 placement: Optional[HarnessPlacement] = None,
                 target_successes: int = 5, max_attempts: int = 30,
                 verify_seconds: int = 60):
        self.generator = generator
        self.of = oss_fuzz
        self.project = project
        self.vuln = vuln_checkout
        self.sanitizer = sanitizer
        self.ext = ext
        # How the harness gets compiled (crib / overwrite). None keeps the crib
        # default so a caller that predates placements still works.
        self.placement = placement
        self.target_successes = target_successes
        self.max_attempts = max_attempts
        self.verify_seconds = verify_seconds

    def run(self, prompt_factory: Callable[[List[str], List[str]],
                                           List[Dict[str, str]]]) -> CampaignResult:
        result = CampaignResult(target_successes=self.target_successes)
        messages = prompt_factory([], [])
        repair_context: Optional[str] = None

        while result.achieved < self.target_successes and \
                result.attempts < self.max_attempts:
            result.attempts += 1
            n = result.attempts
            print(f"\n===== attempt {n}/{self.max_attempts} "
                  f"({result.achieved}/{self.target_successes} accepted) =====")

            if repair_context:
                messages = messages + [{"role": "user", "content": repair_context}]
                repair_context = None
            else:
                messages = prompt_factory(result.covered, result.signatures)

            raw = self.generator.generate(messages)
            source = extract_source(raw)
            if not source:
                print("  no harness in response; retrying")
                repair_context = ("Your reply had no code block. Output ONLY a "
                                  "single fenced code block with the complete "
                                  "LLVMFuzzerTestOneInput translation unit.")
                continue

            name = f"vp_harness_{n}"
            out_bin = self.of.build_harness(
                self.project, self.vuln, name, source, self.ext, self.sanitizer,
                placement=self.placement)
            if out_bin is None:
                infra = getattr(self.of, "last_build_infra_error", None)
                if infra:
                    # No compiler ran, so there is nothing to repair. Retrying
                    # would just spend the remaining attempts identically.
                    result.infra_error = infra
                    print(f"  ABORTING: this is an infrastructure failure, not "
                          f"a harness problem:\n    {infra}")
                    break
                stderr = getattr(self.of, "last_build_stderr", "") or ""
                print("  build failed; feeding compiler errors back")
                repair_context = (
                    "Your harness did not compile. Fix it and re-output the "
                    "complete file as one code block. Compiler errors:\n"
                    + stderr[-1500:])
                continue

            # Trigger gate: must crash the vulnerable build. Under the overwrite
            # placement the binary carries the replaced harness's name, not ours.
            run_name = (self.placement.runtime_name(name) if self.placement
                        else name)
            outcome = self.of.run_fuzzer(
                self.project, run_name, self.verify_seconds, self.sanitizer)
            if outcome.triggered:
                print(f"  ACCEPTED — triggers on vulnerable build "
                      f"[{outcome.signature or outcome.crash_reason}]")
                result.successful.append(GenResult(
                    harness_name=name, source=source, ext=self.ext,
                    signature=outcome.signature))
            else:
                print("  compiled but did not trigger on the vulnerable "
                      "build; re-steering")
                # Next iteration's prompt_factory call will steer via covered/
                # signatures; nothing to repair.
        return result
