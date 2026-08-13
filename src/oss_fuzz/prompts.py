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

import config
from variant import variant_analysis_directive
from oss_fuzz.analysis import PatchContext, TouchedFunction, covered_keys
from oss_fuzz.bugclass import (BugClass, ORACLE_HARNESS, ORACLE_PROJECT_ASSERT,
                               ORACLE_SANITIZER)
from oss_fuzz.crash_evidence import CrashEvidence

# How much of the reference harness to quote. Generous: it is the single most
# useful thing in the prompt (a complete, compiling example of driving this
# project's API from raw bytes) and most OSS-Fuzz targets are well under this.
MAX_REFERENCE_HARNESS_CHARS = 4000


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
              base_includes: Optional[List[str]] = None,
              base_harness_source: Optional[str] = None,
              crash_evidence: Optional[CrashEvidence] = None
              ) -> List[Dict[str, str]]:
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
        # Evidence about the original firing, strongest form available: a
        # verified replay when run.py managed one, the OSV prose otherwise. The
        # loose crash_type/crash_state arguments still work on their own so
        # callers predating the evidence record are unchanged.
        evidence = crash_evidence or CrashEvidence.from_osv(crash_type,
                                                            crash_state)
        if evidence.has_evidence:
            sections.append(self._original_crash_block(evidence, bug_class))
        for fn in context.functions:
            sections.append(self._function_block(fn))
        # The crash stack's own functions, when the fix did not touch them. For a
        # fix in a caller or a helper this is where the fault actually is, and it
        # used to reach the model as three bare names in a prose line.
        for fn in context.frame_functions:
            sections.append(self._function_block(fn))
        if context.headers:
            sections.append(
                "Public headers touched by the fix (include what you need):\n"
                + "\n".join(f"  #include \"{h}\"" for h in context.headers))
        if base_includes:
            sections.append(self._known_includes_block(base_harness,
                                                       base_includes))
        # The project's own fuzz target: this corpus's nearest thing to Java's
        # trigger test. See _reference_harness_block.
        if base_harness_source:
            sections.append(self._reference_harness_block(base_harness,
                                                         base_harness_source,
                                                         is_c))

        if context.root_cause_reachable:
            # Labels carry the signature, location and reachability of each
            # entry; the keys stay plain names so the covered/uncovered split
            # still matches. covered_keys reconciles the sanitizer's spelling of
            # a function with the static index's.
            sections.append(variant_analysis_directive(
                context.root_cause_reachable,
                covered_keys(covered_functions, context.root_cause_reachable),
                found_signatures,
                labels={r.name: r.label() for r in context.reachable}))
            routes = self._routes_block(context)
            if routes:
                sections.append(routes)

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
            # Superseded by the PoC lines inside the crash-evidence block, which
            # get the size and the preview from the file itself instead of from
            # a string the caller had to build. Kept because it is a public
            # parameter and it costs one branch; run.py no longer passes it, and
            # never in fact did — it was wired to a hard-coded None.
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

    def _original_crash_block(self, ev: CrashEvidence,
                              bug_class: Optional[BugClass] = None) -> str:
        """What the original bug did, from the strongest source we have.

        The Java front-end runs the trigger test on the buggy checkout and
        labels the result "trust it over anything inferred from the test body".
        This is that block. When ``ev.observed`` the frames come with files and
        lines from a real sanitizer report and the faulting access is quoted;
        otherwise it is the OSV record's prose, and the block says so rather
        than presenting stale names as measurements.

        The stack is the call path a variant harness has to re-enter, which is
        more direct evidence than the diff: the diff says what changed, this
        says where it blew up.
        """
        if ev.observed:
            head = ("The ORIGINAL bug, OBSERVED: we replayed the recorded PoC "
                    "against the vulnerable checkout with the project's own "
                    "fuzz target and this is what the sanitizer printed. It is "
                    "ground truth — trust it over anything inferred from the "
                    "diff.")
        else:
            head = ("The ORIGINAL bug this fix addressed, as reported by "
                    "OSS-Fuzz. This is the bug report's own text, not a run we "
                    "reproduced: the frame names are reliable, the absence of "
                    "anything else is not evidence.")
        lines = [head]
        if ev.crash_type:
            lines.append(f"  crash type : {ev.crash_type}")
        if ev.access:
            lines.append(f"  access     : {ev.access}")
        # Keyed on the oracle, not the kind: a project-assert bug is crashing,
        # but "no sanitizer saw this" is still the load-bearing fact about it —
        # it tells the model not to go hunting for memory corruption.
        if bug_class and bug_class.oracle != ORACLE_SANITIZER:
            detail = ("a sanitizer will NOT report a sibling of this bug"
                      if bug_class.needs_harness_oracle else
                      "the library's own check reports it, not a sanitizer")
            lines.append(f"  detected by: {bug_class.oracle} — {detail}")
        if ev.frames:
            lines.append("  crash stack (innermost first):")
            lines.extend(f"    #{i} {fr.describe()}"
                         for i, fr in enumerate(ev.frames[:12]))
        elif ev.frame_names:
            lines.append("  crash stack (innermost first): "
                         + " <- ".join(ev.frame_names))
        if ev.alloc_frames:
            # For a heap bug the block's history is the other half of the
            # story: the size it was given is the invariant the fix restored.
            lines.append("  the memory involved was allocated/freed at:")
            lines.extend(f"    #{i} {fr.describe()}"
                         for i, fr in enumerate(ev.alloc_frames[:6]))
        lines.extend(self._poc_lines(ev))

        target = ev.names[0] if ev.names else None
        if target:
            reach = (f"Drive execution into `{target}` via the public API. "
                     "Reaching that frame is necessary but NOT sufficient — the "
                     "point is to reach it along a path the fix did not harden.")
            if bug_class and bug_class.oracle == ORACLE_HARNESS:
                reach += (" Your oracle must observe a value that this frame "
                          "computes; a check on something it cannot influence "
                          "proves nothing about this fix.")
            lines.append(reach)
        return "\n".join(lines)

    def _poc_lines(self, ev: CrashEvidence) -> List[str]:
        """The PoC's shape — size and a short hex/ASCII preview.

        A preview and never the whole input, on purpose. A harness that embeds
        the recorded testcase as a constant passes the trigger gate and proves
        nothing about the fix, which is the failure the Java front-end's "anchor
        THEN fuzz" wording exists to prevent. What transfers is the shape: the
        magic bytes, the field order, the length it had to reach.
        """
        out: List[str] = []
        if ev.poc_size is not None:
            out.append(f"  the recorded PoC is {ev.poc_size} bytes")
        if ev.poc_preview:
            out.append(f"  its first bytes: {ev.poc_preview}")
            out.append(
                "  Build inputs of THIS SHAPE from (data, size) — same magic "
                "bytes, same field order, comparable length — and let the "
                "fuzzer vary the rest. Do NOT hard-code these bytes: a harness "
                "that replays a constant crashes the vulnerable build and says "
                "nothing about whether the fix generalises, which is the whole "
                "question.")
        if ev.poc_did_not_reproduce:
            out.append(
                "  NOTE: replaying that PoC on the vulnerable checkout did NOT "
                "crash, so the code path may have moved or the build differs "
                "from the one that found it. Treat the report above as a lead, "
                "not as a confirmed reproduction.")
        return out

    def _reference_harness_block(self, path: Optional[str], source: str,
                                 is_c: bool) -> str:
        """The project's existing fuzz target, in full.

        This corpus's closest thing to the Java pipeline's trigger test. The
        Java prompt carries the failing test's body, its setup and its fixtures
        precisely because a harness that improvises the setup diverges from the
        scenario that actually fails. On the C side the equivalent artefact is
        the target that found this bug — and unlike a test it is *known to
        compile in this project's OSS-Fuzz build*, which makes it simultaneously
        the answer to "how is this API driven from bytes", "which headers
        resolve", and "what does this build already link".

        Only the include block of it used to reach the prompt.
        """
        where = f" ({path})" if path else ""
        body = source.strip()
        if len(body) > MAX_REFERENCE_HARNESS_CHARS:
            body = body[:MAX_REFERENCE_HARNESS_CHARS] + "\n/* ...truncated */"
        return "\n".join([
            f"REFERENCE HARNESS{where} — this project's own libFuzzer target, "
            "the one the original bug was found with. It compiles and links in "
            "this project's OSS-Fuzz build today, so every include, type and "
            "initialisation call in it is known to work:",
            f"```{'c' if is_c else 'cpp'}",
            body,
            "```",
            "Follow its setup (the same headers, the same init/teardown, the "
            "same way it turns bytes into whatever the API wants) and then "
            "diverge where it matters: it drives the API generically, and your "
            "job is to drive it into the region the fix touched. Do not copy it "
            "unchanged — an identical harness re-finds whatever it already "
            "found.",
        ])

    def _routes_block(self, context: PatchContext) -> str:
        """How to GET to the root cause: entry paths and callers.

        The direction the old reachable set never computed. A list of nearby
        function names says where a sibling might live; it does not say how a
        harness could ever execute one of them, and for a `static` helper or an
        internal function with no public caller the honest answer is "not
        directly, only through here". The Java front-end has carried this as
        ``xrefs`` from the start.

        Only the seed tiers get routes — the functions the fix touched and the
        ones that faulted. Routing every callee too would triple the block for
        targets that are already covered by their parent's path.
        """
        seeds = [r for r in context.reachable
                 if r.tier.startswith(("crash-frame", "touched"))]
        if not seeds:
            return ""
        lines = [
            "HOW TO REACH IT. Static call graph of the vulnerable checkout "
            "(tree-sitter, no build), so it is sound-ish but not complete: "
            "calls made through function pointers, vtables or macros may be "
            "missing, and a route that is absent below may still exist.",
        ]
        unreached: List[str] = []
        for r in seeds[:config.MAX_CALLERS_IN_PROMPT * 2]:
            lines.append(f"- {r.name}:")
            if r.entry_path:
                lines.append("    an existing fuzz target reaches it: "
                             + " -> ".join(r.entry_path))
            elif r.entry_unknown:
                lines.append("    whether an existing target reaches it was "
                             "not determined (the call-graph search was "
                             "truncated) — treat it as unknown, not as "
                             "unreachable")
            else:
                unreached.append(r.name)
            if r.callers:
                lines.append("    called by: " + ", ".join(r.callers))
            elif not r.entry_path:
                lines.append("    no caller in the indexed sources — it is "
                             "either a public entry point itself or only "
                             "reached indirectly")
        if unreached:
            lines.append(
                "No existing target reaches " + ", ".join(unreached[:6])
                + ". That is the interesting case, not a dead end: it means "
                "this code is under-fuzzed today, and your harness has to open "
                "a route in through the public API rather than follow one. Work "
                "up the 'called by' chain until you find something declared in "
                "a public header.")
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
        # Two provenances, and conflating them would be a false statement about
        # the fix: a crash-stack function is where the fault SURFACED, which is
        # often a caller or a helper the commit never edited.
        why = ("the ORIGINAL CRASH REPORT named this frame; the fix did not "
               "change it, so whatever it does wrong it still does"
               if fn.origin == "crash-frame"
               else "the fix sits inside it")
        return (f"Function `{fn.name}` in {fn.file} (vulnerable version, "
                f"{why}):\n```{self._fence()}\n{body}\n```")

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
