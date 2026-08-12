"""Generate → build → verify loop for libFuzzer harnesses.

Compact C-side analogue of the Java HarnessCampaign: keep asking the LLM for a
harness, build it against the *vulnerable* checkout, and accept it only if it
actually crashes there (the trigger gate). On a build failure we feed the
compiler stderr back for repair; on a compile-but-no-crash we re-steer via the
prompt factory toward an uncovered part of the root-cause region. Acceptance =
"compiles AND triggers on the vulnerable build", exactly as in the Java flow,
AND finds a crash the set does not already have — see the distinct-finding gate
in ``run``, without which ``target_successes`` counts harnesses rather than
findings.
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Callable, Dict, List, Optional

from oss_fuzz.bugclass import BugClass, ORACLE_HARNESS
from oss_fuzz.ossfuzz import (OssFuzz, Checkout, HarnessPlacement, RunOutcome,
                              included_paths, missing_includes)

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
        """The distinct crashes the set has found, in acceptance order.

        Distinct by construction — the campaign's distinct-finding gate refuses
        a harness whose signature is already in here — which is what lets
        ``achieved`` be read as a count of evidence rather than of harnesses.
        It is also the steering input, so duplicates here would tell the model
        the same ground twice and inflate what looks covered.
        """
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
                 bug_class: Optional[BugClass] = None,
                 artifacts=None):
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
        # artifacts.RunArtifacts, or None. Records the prompt and the harness
        # for EVERY attempt, not just accepted ones: a campaign that spends 30
        # attempts and accepts nothing is the case most in need of explaining,
        # and it is exactly the case that leaves nothing behind otherwise.
        self.artifacts = artifacts
        # The stock-build question is asked at most once per campaign; see run().
        self._stock_checked = False
        # Header paths the compiler has already failed to find in this project.
        # Facts, not guesses — and they do not become true on a later attempt.
        self.missing_headers: List[str] = []

    def _missing_header_note(self) -> str:
        """Every header this project has been shown not to have, restated.

        Accumulated across the campaign rather than left to the last compiler
        error, because that error is all the model was ever shown: the 20260812
        run watched libxaac re-include the same three headers across 30
        attempts, each time being told about only the most recent one.
        """
        if not self.missing_headers:
            return ""
        return ("\n\nThese header paths do NOT exist in this project's include "
                "path — the compiler has already looked for each one. Do not "
                "include any of them again:\n"
                + "\n".join(f"  {h}" for h in self.missing_headers))

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

            if self.artifacts is not None:
                self.artifacts.record_prompt(n, messages)

            raw = self.generator.generate(messages)
            source = extract_source(raw)
            if not source:
                print("  no harness in response; retrying")
                repair_context = ("Your reply had no code block. Output ONLY a "
                                  "single fenced code block with the complete "
                                  "LLVMFuzzerTestOneInput translation unit.")
                continue

            # Kept before the pre-build gates below, so a harness they reject
            # still leaves a file behind: those rejections print a one-line
            # reason, and the harness is what the reason is about.
            name = f"vp_harness_{n}"
            if self.artifacts is not None:
                self.artifacts.record_harness(name, self.ext, source)

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

            # A header the compiler has already failed to find will not be
            # found on a later attempt either, so this is a build we know the
            # answer to. Worth checking because the model does come back with
            # the same path: libxaac's campaign spent four separate Docker
            # builds rediscovering that 'ixheaace.h' is not there.
            reused = [h for h in included_paths(source)
                      if h in self.missing_headers]
            if reused:
                print(f"  includes headers already known missing: "
                      f"{', '.join(reused)}")
                repair_context = (
                    "Your harness includes a header this project does not have, "
                    "which the compiler already told you on an earlier attempt. "
                    "Re-output the complete file as one code block, reaching "
                    "what you need through the known-good includes above."
                    + self._missing_header_note())
                continue

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
                # Read before the stock build below, which overwrites it.
                stderr = getattr(self.of, "last_build_stderr", "") or ""
                # The diagnostics can look like a harness bug and still not be
                # one: the tree may not build at all. Ask the project's own
                # build, once, on the first failure — the answer cannot change
                # across attempts, and getting it wrong costs every remaining
                # attempt (the 20260811 run: 15/15 on two projects).
                if not self._stock_checked:
                    self._stock_checked = True
                    stock = self.of.stock_build_error(
                        self.project, self.vuln, self.sanitizer)
                    if stock:
                        result.infra_error = stock
                        print(f"  ABORTING: this is an infrastructure failure, "
                              f"not a harness problem:\n    {stock}")
                        break
                for h in missing_includes(stderr):
                    if h not in self.missing_headers:
                        self.missing_headers.append(h)
                print("  build failed; feeding compiler errors back")
                repair_context = (
                    "Your harness did not compile. Fix it and re-output the "
                    "complete file as one code block. Compiler errors:\n"
                    + stderr[-1500:] + self._missing_header_note())
                continue

            # Trigger gate: must crash the vulnerable build. Under the overwrite
            # placement the binary carries the replaced harness's name, not ours.
            run_name = (self.placement.runtime_name(name) if self.placement
                        else name)
            outcome = self.of.run_fuzzer(
                self.project, run_name, self.verify_seconds, self.sanitizer,
                bug_class=self.bug_class, log_tag=f"verify_{name}")
            if outcome.triggered and outcome.signature in result.signatures:
                # Distinct-finding gate. Five harnesses that all re-find one
                # crash satisfy `target_successes=5` while carrying one piece
                # of evidence, and each one then costs a HEAD build and gets
                # counted again in the sibling total. The signature is already
                # computed, so this is a set lookup — cheap enough to be worth
                # keeping even if it turns out to reject nothing, which is what
                # happened to the Java front-end's analogous family gate
                # (deleted 2026-08-06 after 458 inert evaluations).
                #
                # Only ever fires on a signature we could actually read: an
                # unreadable one is None, never `in` the list, so it is
                # accepted. Failing closed there would let one unparseable
                # crash report stall the campaign to max_attempts.
                print(f"  triggers, but re-finds a crash the set already has "
                      f"[{outcome.signature}]; steering off it")
                repair_context = (
                    f"That harness works, but it reproduces "
                    f"`{outcome.signature}` — a crash this set has already "
                    "found, so it adds no new evidence about what the fix "
                    "missed. Win a DIFFERENT way: either reach a different "
                    "fault in the region (different crash type, or a different "
                    "innermost frame), or keep this path and add a tagged "
                    '`[oracle:<id>]` check that fires where no sanitizer does. '
                    "Re-output the complete file as one code block.")
                continue

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
