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

from oss_fuzz.bugclass import BugClass, ORACLE_HARNESS
from oss_fuzz.ossfuzz import OssFuzz, Checkout, HarnessPlacement, RunOutcome

_FENCE_RE = re.compile(r"```(?:c|cc|cpp|c\+\+)?\s*\n(.*?)```", re.DOTALL)

# A tagged alarm and a way to stop. Both are required of a semantic harness:
# the tag makes the finding attributable, the abort makes it observable.
_ORACLE_TAG_RE = re.compile(r"\[oracle:\s*([A-Za-z0-9_.\-]{1,48})\s*\]")
_ABORT_RE = re.compile(r"\b(?:std::)?(?:abort|__builtin_trap|_Exit)\s*\(")


def oracle_tag_missing(source: str) -> Optional[str]:
    """Why this harness cannot report a semantic finding, or None if it can.

    Only applied to bugs whose oracle must come from the harness. Checking the
    source is worth doing *before* the build because the alternative is a full
    Docker compile plus a verify run to discover the same thing — and the
    verdict is indistinguishable from an honest miss ("compiled but did not
    trigger"), so the campaign would keep re-steering a harness that was
    structurally incapable of ever firing.

    Deliberately shallow: it asks whether an alarm exists and can stop the
    process, not whether the relation behind it is true. Nothing here can
    establish that — which is why harness-oracle findings are reported as
    claims needing triage rather than as confirmed siblings.
    """
    if not _ORACLE_TAG_RE.search(source):
        return ("no tagged oracle alarm — this bug does not crash, so a "
                "harness with no check of its own can never fail")
    if not _ABORT_RE.search(source):
        return ("the oracle alarm never stops the process — printing a "
                "mismatch and returning 0 leaves libFuzzer thinking the input "
                "was fine")
    return None


@dataclass
class GenResult:
    harness_name: str
    source: str
    ext: str
    signature: Optional[str] = None
    covered: List[str] = field(default_factory=list)
    # Which oracle actually fired on the vulnerable build (bugclass.ORACLE_*).
    # Not always the one the bug class predicted: a harness aimed at a
    # wrong-value bug that trips ASan found a memory bug instead — a real
    # finding, but not evidence about this fix's completeness.
    found_by: Optional[str] = None


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
                 verify_seconds: int = 60,
                 bug_class: Optional[BugClass] = None):
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
        # None keeps the pre-split behaviour: assume a sanitizer is the oracle,
        # gate on "it crashed", require nothing of the harness's own checks.
        self.bug_class = bug_class

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

            # Semantic gate, before the build: a harness for a non-crashing bug
            # must carry an alarm that can fire, or the whole attempt is spent
            # proving nothing. Costs a string search; saves a Docker build and
            # a verify run per bad harness.
            if self.bug_class and self.bug_class.needs_harness_oracle:
                why = oracle_tag_missing(source)
                if why is not None:
                    print(f"  no usable oracle: {why}")
                    repair_context = (
                        "Your harness cannot fail: " + why + ".\n"
                        "This bug does not crash — no sanitizer will report a "
                        "sibling of it. Add a check comparing two values you "
                        "obtained from real library calls, report it as "
                        '`fprintf(stderr, "[oracle:<short-id>] <what '
                        'disagreed>\\n"); abort();`, and only compare after '
                        "confirming the library accepted the input. Re-output "
                        "the complete file as one code block.")
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
                self.project, run_name, self.verify_seconds, self.sanitizer,
                bug_class=self.bug_class)
            if outcome.triggered:
                print(f"  ACCEPTED — triggers on vulnerable build "
                      f"[{outcome.signature or outcome.crash_reason}] "
                      f"found by {outcome.found_by}")
                # Worth saying out loud when the finding is not of the class we
                # aimed at: it is still a real crash on the vulnerable build,
                # but it is not evidence about *this* bug's kind.
                if self.bug_class and outcome.found_by and \
                        outcome.found_by != self.bug_class.oracle:
                    print(f"  note: expected a {self.bug_class.oracle} "
                          f"finding for this {self.bug_class.kind} bug")
                result.successful.append(GenResult(
                    harness_name=name, source=source, ext=self.ext,
                    signature=outcome.signature, found_by=outcome.found_by))
            else:
                print("  compiled but did not trigger on the vulnerable "
                      "build; re-steering")
                # Next iteration's prompt_factory call will steer via covered/
                # signatures; nothing to repair.
        return result
