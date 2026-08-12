"""Build chat-completion prompts for libFuzzer (C/C++) harness generation.

Mirrors the Java PromptBuilder's shape but for an ``LLVMFuzzerTestOneInput``
target: instead of a FuzzedDataProvider reference we give a byte-carving
reference over ``(const uint8_t *data, size_t size)``, and the skeleton is a
self-contained translation unit that OSS-Fuzz's builder can compile with the
project's own flags. The variant-analysis steering — the actual research
heuristic — is imported from ``variant.py`` so both front-ends stay identical.
"""
from __future__ import annotations

from typing import Dict, List, Optional

from variant import variant_analysis_directive
from oss_fuzz.analysis import PatchContext, TouchedFunction
from oss_fuzz.bugclass import (BugClass, ORACLE_HARNESS, ORACLE_PROJECT_ASSERT,
                               ORACLE_SANITIZER)


class LibFuzzerPromptBuilder:
    def __init__(self, language: str = "c++"):
        self.language = language

    def build(self, context: PatchContext,
              covered_functions: List[str],
              found_signatures: List[str],
              harness_name: str,
              reproducer_hint: Optional[str] = None,
              crash_type: Optional[str] = None,
              crash_state: Optional[List[str]] = None,
              harness_ext: Optional[str] = None,
              bug_class: Optional[BugClass] = None,
              base_harness: Optional[str] = None,
              base_includes: Optional[List[str]] = None) -> List[Dict[str, str]]:
        """``bug_class`` decides what a *failing* harness even looks like.

        The fork is on ``bug_class.oracle``, not on its kind: a project-assert
        bug is *crashing* (the library aborts by itself) yet still needs its own
        wording, because the sibling has to falsify an invariant rather than
        corrupt memory. Three cases:

        * ``sanitizer`` — reach the fault and the runtime supplies the verdict,
          so the metamorphic block stays the optional extra it has always been.
        * ``project-assert`` — also no verdict to write, but aim at state
          rather than memory.
        * ``harness`` — there is no verdict unless the harness provides one. A
          harness written to the crashing template would run clean forever,
          compiling, reaching the code, and being rejected by the trigger gate
          on every attempt.

        None keeps the crashing wording, which is what every caller predating
        the split expects.
        """
        # The extension the harness will be SAVED as decides which language the
        # model must write, and that is not always the project's language: the
        # overwrite placement replaces an existing harness file in place and
        # cannot change its extension, so a C++ body in a .c file will not
        # compile. When no extension is forced, follow the project.
        is_c = ((harness_ext == ".c") if harness_ext
                else context.language.lower() == "c")
        lang_label = "C" if is_c else "C++"
        sections: List[str] = [self._intro(lang_label, bug_class)]

        sections.append(self._patch_block(context.patch_text))
        if crash_type or crash_state:
            sections.append(self._original_crash_block(crash_type, crash_state,
                                                       bug_class))
        for fn in context.functions:
            sections.append(self._function_block(fn))
        if context.headers:
            sections.append(
                "Public headers touched by the fix (include what you need):\n"
                + "\n".join(f"  #include \"{h}\"" for h in context.headers))
        if base_includes:
            sections.append(self._known_includes_block(base_harness,
                                                       base_includes))

        if context.root_cause_reachable:
            sections.append(variant_analysis_directive(
                context.root_cause_reachable,
                covered_functions, found_signatures))

        # The campaign refuses a harness that re-finds a listed crash, so say
        # so: a gate the model cannot see spends attempts teaching it nothing.
        # Only once the set HAS a finding — as the first harness's instruction
        # it would read as "avoid the bug you were sent here to reach".
        if found_signatures:
            sections.append(self._distinct_finding_block(is_c))

        # The oracle section is the fork. Crashing bugs keep the optional
        # metamorphic nudge; semantic bugs get a mandatory contract instead,
        # because for them the oracle IS the harness's reason to exist.
        oracle = bug_class.oracle if bug_class else None
        if oracle == ORACLE_HARNESS:
            sections.append(self._required_oracle_block(is_c))
        elif oracle == ORACLE_PROJECT_ASSERT:
            sections.append(self._project_invariant_block(crash_type))
        else:
            # Sanitizer or unknown. An unknown class is a prior, not a reading,
            # so say so and lean on the optional relation: it is the only thing
            # that can save the run if the record we could not read was in fact
            # a wrong-value bug, and it costs nothing if it was not.
            sections.append(self._metamorphic_block(
                uncertain=bool(bug_class and bug_class.uncertain)))

        sections.append(self._byte_carving_reference())
        if reproducer_hint:
            sections.append(
                "A known crashing input for the ORIGINAL bug (bytes shown as "
                f"context, not to be hardcoded):\n{reproducer_hint}")
        sections.append(self._skeleton(is_c, harness_name, harness_ext,
                                       bug_class))

        system = (
            f"You are an expert {lang_label} security engineer writing "
            "libFuzzer harnesses for OSS-Fuzz. You output exactly one "
            "self-contained fuzz target and nothing else.")
        return [
            {"role": "system", "content": system},
            {"role": "user", "content": "\n\n".join(sections)},
        ]

    def _intro(self, lang_label: str,
               bug_class: Optional[BugClass] = None) -> str:
        base = (
            f"Write a single {lang_label} libFuzzer harness that exercises the "
            "code region a security fix touched, to discover a SIBLING bug the "
            "fix may have missed. The harness must define "
            "`int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size)` and "
            "call the project's real public API — never re-implement it.")
        if bug_class and bug_class.oracle == ORACLE_HARNESS:
            return base + (
                "\n\nTHIS BUG DOES NOT CRASH. It is a wrong-value bug: the "
                "library returns successfully and returns the wrong answer, so "
                "AddressSanitizer, UBSan and libFuzzer will all report a clean "
                "run no matter how well you reach the code. A harness that "
                "only calls the API is worthless here — the check you write IS "
                "the harness.")
        if bug_class and bug_class.oracle == ORACLE_PROJECT_ASSERT:
            return base + (
                "\n\nThis bug is not memory corruption: it is an INVARIANT the "
                "library checks itself and that a bad input can falsify. The "
                "library aborts when that happens, so you do not need to write "
                "a check — you need to construct a state the invariant does "
                "not hold in, reached along a path the fix did not harden.")
        return base

    def _original_crash_block(self, crash_type: Optional[str],
                              crash_state: Optional[List[str]],
                              bug_class: Optional[BugClass] = None) -> str:
        """The original bug's crash type and crashing frames, straight from the
        OSV record. The frames are the call path the harness has to re-enter,
        which is more direct evidence than the diff alone: the diff says what
        changed, this says where it blew up."""
        lines = ["The ORIGINAL bug this fix addressed, as reported by OSS-Fuzz:"]
        if crash_type:
            lines.append(f"  crash type : {crash_type}")
        # Keyed on the oracle, not the kind: a project-assert bug is crashing,
        # but "no sanitizer saw this" is still the load-bearing fact about it —
        # it tells the model not to go hunting for memory corruption.
        if bug_class and bug_class.oracle != ORACLE_SANITIZER:
            detail = ("a sanitizer will NOT report a sibling of this bug"
                      if bug_class.needs_harness_oracle else
                      "the library's own check reports it, not a sanitizer")
            lines.append(f"  detected by: {bug_class.oracle} — {detail}")
        if crash_state:
            lines.append("  crash stack (innermost first): "
                         + " <- ".join(crash_state))
            # For a semantic bug the frames are where the wrong value surfaced,
            # not where memory was corrupted, so "reach it" is only half the
            # instruction — the oracle has to be able to see the result there.
            reach = (f"Drive execution into `{crash_state[0]}` via the public "
                     "API. Reaching that frame is necessary but NOT sufficient "
                     "— the point is to reach it along a path the fix did not "
                     "harden.")
            if bug_class and bug_class.oracle == ORACLE_HARNESS:
                reach += (" Your oracle must observe a value that this frame "
                          "computes; a check on something it cannot influence "
                          "proves nothing about this fix.")
            lines.append(reach)
        return "\n".join(lines)

    def _known_includes_block(self, base_harness: Optional[str],
                              includes: List[str]) -> str:
        """The include block of the harness file this one replaces.

        The only statement in the prompt about the include path the compiler
        will actually use. The file it comes from builds today in this project's
        OSS-Fuzz build, so every line here is known to resolve — where an
        invented path costs a full Docker build to disprove, and then tends to
        be invented again.
        """
        where = f" of {base_harness}" if base_harness else ""
        return "\n".join([
            f"The include block{where} — the file you are replacing, which "
            "compiles today with this project's own OSS-Fuzz include path. "
            "Every line below is known to resolve; prefer them, and copy the "
            "form (quoted vs angled, prefixed vs bare) for any other header of "
            "this project you need:",
            "```",
            "\n".join(includes),
            "```",
            "A header path you invent costs a full build to disprove. If you "
            "need something not listed, reach it through one of these instead.",
        ])

    def _patch_block(self, patch_text: str) -> str:
        # Cap to keep the prompt bounded; the touched-function bodies below
        # carry the detail.
        text = patch_text if len(patch_text) < 6000 else patch_text[:6000] + "\n...(truncated)"
        return "The security fix under analysis (unified diff):\n```diff\n" + text + "\n```"

    def _function_block(self, fn: TouchedFunction) -> str:
        body = fn.source if len(fn.source) < 4000 else fn.source[:4000] + "\n/*...*/"
        return (f"Function `{fn.name}` in {fn.file} (vulnerable version, the "
                f"fix sits inside it):\n```{self._fence()}\n{body}\n```")

    def _fence(self) -> str:
        return "c" if self.language.lower() == "c" else "cpp"

    def _byte_carving_reference(self) -> str:
        return "\n".join([
            "Carve the fuzz input yourself from (data, size) — there is no "
            "FuzzedDataProvider in plain C. Suggested pattern:",
            "  - bail out early on `size < N` for whatever minimum you need;",
            "  - take integers/lengths from the first bytes, then treat the "
            "remainder as the payload;",
            "  - if the API wants a NUL-terminated string, copy into a "
            "malloc(size+1) buffer and terminate it (never assume `data` is "
            "NUL-terminated).",
            "For C++ targets you MAY use "
            "`FuzzedDataProvider` from <fuzzer/FuzzedDataProvider.h> instead.",
        ])

    def _distinct_finding_block(self, is_c: bool) -> str:
        """Two ways to be a new harness rather than another copy of an old one.

        Stated as the two moves that are actually available, because "be
        different" on its own tends to produce a re-skin: the same call sequence
        with renamed locals, which reaches the same fault and is rejected. The
        second move matters most on a set that has already found its crash —
        it is the only way to add evidence without a second reachable fault,
        and it is what the Java front-end's steering asks for in the same spot.
        """
        trap = "abort()" if is_c else "std::abort()"
        return "\n".join([
            "DISTINCT FINDING REQUIRED. The crashes listed above are already "
            "in this set. A harness that reproduces one of them is REJECTED "
            "however different its code, because it adds no evidence about "
            "what the fix missed. Two ways to win:",
            "  1. reach a DIFFERENT fault in the region — another crash type, "
            "or the same type at a different innermost frame (a sibling "
            "function, a different entry path into the touched code, a "
            "different API-call order);",
            "  2. keep this path and add a check no sanitizer performs: a "
            "relation true of any correct implementation, reported as "
            '`fprintf(stderr, "[oracle:<id>] <what disagreed>\\n"); '
            f"{trap};`. This is the only way a set whose one reachable crash "
            "is already found can still add evidence — and it is what catches "
            "a fix that merely suppressed the reported symptom.",
        ])

    def _metamorphic_block(self, uncertain: bool = False) -> str:
        # Language-neutral version of the Java metamorphic hint. Stays OPTIONAL
        # for crashing bugs: the sanitizer is already a complete oracle there,
        # and a mandatory relation would only add false-alarm surface.
        lines = [
            "METAMORPHIC CHECK (optional, catches wrong-output bugs that never "
            "crash): if the patched function has a natural relation — "
            "round-trip f(g(x))==x (decode/encode), idempotence f(f(x))==f(x) "
            "(normalise/canonicalise), or equivalent-inputs mapping to equal "
            "results — compute BOTH sides from real library calls on the fuzzed "
            "input and `abort()` (or `__builtin_trap()`) on a genuine "
            "violation. Only assert relations true for ANY correct "
            "implementation; guard inputs the library rejected so you don't "
            "report false positives.",
        ]
        if uncertain:
            # The record did not say how this bug manifests. Assuming a
            # sanitizer is the right bet for this corpus, but if the bet is
            # wrong the harness can never fail and the whole budget is spent
            # proving nothing — so ask for the relation rather than merely
            # offering it, and tag it so a firing is attributable.
            lines.append(
                "STRONGLY RECOMMENDED HERE: the bug report does not say how "
                "this bug manifests, so a sanitizer may well report nothing "
                "however precisely you reach the code. If you can see any such "
                "relation, add it — and tag its alarm "
                '`fprintf(stderr, "[oracle:<short-id>] <what disagreed>\\n"); '
                "abort();` so a firing is attributable to your check rather "
                "than mistaken for a sanitizer report.")
        return "\n".join(lines)

    def _required_oracle_block(self, is_c: bool) -> str:
        """The mandatory contract for a wrong-value bug.

        Two requirements beyond the optional metamorphic hint. First, the
        oracle is not optional — without it the harness cannot fail. Second,
        every alarm must be tagged ``[oracle:<id>]``, mirroring the Java
        pipeline's named-alarm gate: an untagged alarm reaches the runner as
        the same "deadly signal" as every other harness's, and the campaign
        steers on those signatures, so untagged alarms would tell the model it
        had covered ground it had not. ``campaign.oracle_tag_missing`` enforces
        the tag before the harness is ever built.
        """
        trap = "abort()" if is_c else "std::abort()"
        return "\n".join([
            "REQUIRED ORACLE — the harness must be able to fail.",
            "Pick a relation that holds for ANY correct implementation of the "
            "patched code, compute both sides from real library calls on the "
            "fuzzed input, and report a violation. Usable relations, best "
            "first:",
            "  - round-trip:  decode(encode(x)) == x, parse(print(v)) == v;",
            "  - idempotence: normalise(normalise(x)) == normalise(x);",
            "  - equivalence: two API paths that must agree (a convenience "
            "wrapper vs the primitive it wraps, streaming vs one-shot, "
            "different-but-equivalent inputs);",
            "  - invariants:  a documented postcondition of the touched "
            "function (length/ordering/range/consistency of an out-parameter).",
            "",
            "Report EXACTLY like this, so the runner can attribute the alarm:",
            '  fprintf(stderr, "[oracle:round-trip] decode(encode(x)) != x: '
            'got %zu want %zu\\n", got, want);',
            f"  {trap};",
            "Rules:",
            "  - every distinct check gets its OWN short id: [oracle:<id>]. "
            "Reuse of one id for two different checks makes them "
            "indistinguishable in the results;",
            "  - only compare after checking the library ACCEPTED the input "
            "(error return, NULL, negative status → `return 0;`). An alarm on "
            "a rejected input is a false positive, and false positives are "
            "worse than no harness: they are reported as unfixed bugs;",
            "  - never assert something the documentation does not promise "
            "(iteration order, exact error codes, padding bytes);",
            "  - do not compare against a value you computed by "
            "re-implementing the library. Both sides must come from real "
            "library calls.",
        ])

    def _project_invariant_block(self, crash_type: Optional[str]) -> str:
        """For bugs whose oracle is the project's own assert/CHECK.

        Nothing has to be written — the library already aborts — so the job is
        purely reachability, with the invariant itself as the steering target.
        ClusterFuzz puts the failing expression in the crash type ("ASSERT:
        idx < len"), which is the most specific instruction available.
        """
        lines = [
            "ORACLE — the library checks this itself.",
            "The original bug was a violated internal invariant: the library "
            "aborts when it does not hold, and libFuzzer records that abort. "
            "You do NOT need to write a check.",
        ]
        if crash_type:
            lines.append(f"The invariant that failed was reported as: "
                         f"`{crash_type}`.")
        lines += [
            "So aim at STATE, not at memory: build inputs that put the touched "
            "code into a configuration its preconditions do not cover — "
            "empty/degenerate values, sizes at and just past internal limits, "
            "a second call that reuses state left by the first, an "
            "out-of-order API sequence the fix did not consider.",
            "Two cautions specific to this class:",
            "  - if the build defines NDEBUG the asserts are gone and nothing "
            "will fire, however good the input; prefer API paths whose checks "
            "are unconditional (explicit `if (...) abort()` / CHECK / fatal "
            "error paths) when the touched code offers both;",
            "  - you MAY additionally add your own tagged check "
            "(`[oracle:<id>]` + abort()) if you can see a postcondition the "
            "library does not verify — but the project's own invariant is the "
            "primary target.",
        ]
        return "\n".join(lines)

    def _skeleton(self, is_c: bool, harness_name: str,
                  harness_ext: Optional[str] = None,
                  bug_class: Optional[BugClass] = None) -> str:
        if is_c:
            head = ("#include <stdint.h>\n#include <stddef.h>\n"
                    "#include <stdlib.h>\n#include <stdio.h>\n"
                    "#include <string.h>\n"
                    "/* #include the project headers you call */\n")
        else:
            head = ("#include <cstdint>\n#include <cstddef>\n#include <cstdio>\n"
                    "#include <cstdlib>\n#include <cstring>\n"
                    "// #include the project headers you call\n")
        sig = ('extern "C" ' if not is_c else "") + \
            "int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {"
        ext = harness_ext or (".c" if is_c else ".cc")
        fence = "c" if is_c else "cpp"
        oracle = bug_class.oracle if bug_class else None
        if oracle == ORACLE_HARNESS:
            step3 = ("    // 3) REQUIRED: compare both sides, "
                     "[oracle:<id>] + abort() on violation")
        elif oracle == ORACLE_PROJECT_ASSERT:
            step3 = ("    // 3) no check needed — the library's own invariant "
                     "aborts")
        else:
            step3 = "    // 3) (optional) metamorphic check"
        return "\n".join([
            "Output ONLY a single fenced code block containing the complete "
            f"translation unit (it will be saved as {harness_name}{ext} and "
            f"compiled with the project's own OSS-Fuzz flags, so it must be "
            f"valid {'C' if is_c else 'C++'}). Do not include a main(). "
            "Skeleton:",
            f"```{fence}",
            head + sig,
            "    // 1) carve inputs from (data, size)",
            "    // 2) call the real API in the touched region",
            step3,
            "    return 0;",
            "}",
            "```",
        ])
