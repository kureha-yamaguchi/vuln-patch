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


class LibFuzzerPromptBuilder:
    def __init__(self, language: str = "c++"):
        self.language = language

    def build(self, context: PatchContext,
              covered_functions: List[str],
              found_signatures: List[str],
              harness_name: str,
              reproducer_hint: Optional[str] = None) -> List[Dict[str, str]]:
        is_c = context.language.lower() == "c"
        lang_label = "C" if is_c else "C++"
        sections: List[str] = [self._intro(lang_label)]

        sections.append(self._patch_block(context.patch_text))
        for fn in context.functions:
            sections.append(self._function_block(fn))
        if context.headers:
            sections.append(
                "Public headers touched by the fix (include what you need):\n"
                + "\n".join(f"  #include \"{h}\"" for h in context.headers))

        if context.root_cause_reachable:
            sections.append(variant_analysis_directive(
                context.root_cause_reachable,
                covered_functions, found_signatures))

        sections.append(self._metamorphic_block())
        sections.append(self._byte_carving_reference())
        if reproducer_hint:
            sections.append(
                "A known crashing input for the ORIGINAL bug (bytes shown as "
                f"context, not to be hardcoded):\n{reproducer_hint}")
        sections.append(self._skeleton(is_c, harness_name))

        system = (
            f"You are an expert {lang_label} security engineer writing "
            "libFuzzer harnesses for OSS-Fuzz. You output exactly one "
            "self-contained fuzz target and nothing else.")
        return [
            {"role": "system", "content": system},
            {"role": "user", "content": "\n\n".join(sections)},
        ]

    def _intro(self, lang_label: str) -> str:
        return (
            f"Write a single {lang_label} libFuzzer harness that exercises the "
            "code region a security fix touched, to discover a SIBLING bug the "
            "fix may have missed. The harness must define "
            "`int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size)` and "
            "call the project's real public API — never re-implement it.")

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

    def _metamorphic_block(self) -> str:
        # Language-neutral version of the Java metamorphic hint.
        return "\n".join([
            "METAMORPHIC CHECK (optional, catches wrong-output bugs that never "
            "crash): if the patched function has a natural relation — "
            "round-trip f(g(x))==x (decode/encode), idempotence f(f(x))==f(x) "
            "(normalise/canonicalise), or equivalent-inputs mapping to equal "
            "results — compute BOTH sides from real library calls on the fuzzed "
            "input and `abort()` (or `__builtin_trap()`) on a genuine "
            "violation. Only assert relations true for ANY correct "
            "implementation; guard inputs the library rejected so you don't "
            "report false positives.",
        ])

    def _skeleton(self, is_c: bool, harness_name: str) -> str:
        if is_c:
            head = ("#include <stdint.h>\n#include <stddef.h>\n"
                    "#include <stdlib.h>\n#include <string.h>\n"
                    "/* #include the project headers you call */\n")
        else:
            head = ("#include <cstdint>\n#include <cstddef>\n#include <cstring>\n"
                    "// #include the project headers you call\n")
        sig = ('extern "C" ' if not is_c else "") + \
            "int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {"
        return "\n".join([
            "Output ONLY a single fenced code block containing the complete "
            f"translation unit (it will be saved as {harness_name}"
            f"{'.c' if is_c else '.cc'} and compiled with the project's own "
            "OSS-Fuzz flags). Do not include a main(). Skeleton:",
            f"```{self._fence()}",
            head + sig,
            "    // 1) carve inputs from (data, size)",
            "    // 2) call the real API in the touched region",
            "    // 3) (optional) metamorphic check",
            "    return 0;",
            "}",
            "```",
        ])
