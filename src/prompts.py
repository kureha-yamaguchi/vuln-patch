"""Build the chat-completion prompt that asks the LLM to write a Jazzer
harness for the patched code.

Sections are constructed as lists of clean lines and joined with '\\n'
rather than as triple-quoted strings, so the indentation of the Python
source never leaks into the prompt. Multi-line substitutions
(patch_text, function source, failing-test bodies) are spliced in
verbatim — no `textwrap.dedent` on substituted content, which would
break when the substituted text has its own indentation, and no
`str.format`, which would choke on the literal `{` / `}` in Java code.
"""
import os
from typing import List, Dict, Optional

import config
from analysis import PatchContext, TouchedFunction
from failure_test import FailureTest


class PromptBuilder:
    """Builds the chat-completion messages from the extracted patch
    context. Targets Jazzer (JVM libFuzzer port) so the result can be
    compiled against the Java project."""

    def __init__(self, language: str = 'Java'):
        self.language = language

    def build(self, buggy_dir: str,
              context: PatchContext,
              failure_tests: Optional[List[FailureTest]] = None,
              covered_functions: Optional[List[str]] = None,
              found_signatures: Optional[List[str]] = None,
              ) -> List[Dict[str, str]]:
        """Assemble the chat-completion messages.

        `covered_functions` and `found_signatures` describe the harnesses
        already accepted into the set this campaign. When present, a
        variant-analysis block asks the model to drive a *different*
        slice of the root-cause neighbourhood and find a *different*
        crash — the mechanism by which the set interrogates the root
        cause broadly and exposes sibling bugs rather than re-finding the
        same fault."""
        codebase = os.path.basename(buggy_dir.rstrip('/'))

        sections: List[str] = [
            self._intro(codebase),
            self._patch_block(context.patch_text),
        ]
        for fn in context.functions:
            sections.append(self._function_block(fn))
        if failure_tests:
            sections.append(self._failure_test_block(failure_tests))
        if context.root_cause_reachable:
            sections.append(self._variant_analysis_block(
                context.root_cause_reachable,
                covered_functions or [],
                found_signatures or [],
            ))
        sections.append(self._chain_of_thought())
        sections.append(self._guidelines(context.package))

        prompt = '\n\n'.join(sections)

        print("#" * 20 + " prompt " + "#" * 20)
        print(prompt)
        print("#" * 48)

        # The bulk of the instructions live in the user message; the
        # system message stays short and policy-shaped, which is what
        # gpt-oss-20b's harmony format prefers.
        return [
            {'role': 'system', 'content':
                f'You are an expert {self.language} security engineer '
                'who writes Jazzer fuzzing harnesses. Return a single '
                'compilable .java file — no markdown fences, no prose '
                'outside the file.'},
            {'role': 'user', 'content': prompt},
        ]

    # --- sections --------------------------------------------------------

    def _intro(self, codebase: str) -> str:
        return '\n'.join([
            f"You are a {self.language} security engineer writing a "
            "fuzzing harness for a codebase you are analysing.",
            "",
            f"The codebase is `{codebase}`. A patch has been applied "
            "that touches the functions listed below. Your harness "
            "must exercise those functions so the behaviour changed "
            "by the patch is reachable from the fuzz entrypoint.",
        ])

    def _patch_block(self, patch_text: str) -> str:
        return '\n'.join([
            "The patch under analysis is:",
            "<patch>",
            patch_text,
            "</patch>",
        ])

    def _function_block(self, fn: TouchedFunction) -> str:
        parts: List[str] = [
            f"Function: `{fn.func_name}` has the following function "
            "signature:",
            "<signature>",
            fn.func_signature,
            "</signature>",
            "",
            "and the following source code:",
            "",
            "<code>",
            fn.func_source,
            "</code>",
        ]
        if fn.xrefs:
            parts.extend([
                "",
                "The function is used in other places of the code. "
                "Use these cross-references as examples of how to "
                "call the target function in the fuzzing harness you "
                "write:",
            ])
            for xref in fn.xrefs:
                parts.extend(["<xref>", xref, "</xref>"])
        return '\n'.join(parts)

    def _failure_test_block(self, failure_tests: List[FailureTest]) -> str:
        """Seed the prompt with the bug-triggering test(s) shipped by
        Defects4J. The LLM should treat the inputs they construct as a
        worked example of values that already reach the root cause."""
        parts: List[str] = [
            "A known failing test in the project already triggers "
            "this bug. Treat the inputs it constructs as a worked "
            "example of values that drive the touched functions "
            "through the buggy code path. The harness must take its "
            "inputs from FuzzedDataProvider rather than hard-coding "
            "them — but the FuzzedDataProvider calls should be able "
            "to produce values similar in shape and content to "
            "the ones the test uses.",
        ]
        for ft in failure_tests:
            if ft.method_source:
                parts.extend([
                    f'<failing_test class="{ft.test_class}" '
                    f'method="{ft.test_method}">',
                    ft.method_source,
                    "</failing_test>",
                ])
            else:
                parts.append(
                    f'<failing_test class="{ft.test_class}" '
                    f'method="{ft.test_method}" />'
                )
        return '\n'.join(parts)

    def _variant_analysis_block(self,
                                reachable: List[str],
                                covered: List[str],
                                signatures: List[str]) -> str:
        """Steer the harness set across the root-cause neighbourhood.

        We are building a *set* of harnesses, not one. To detect
        overfitting we want them to collectively interrogate the whole
        region of code downstream of the patched lines — that is where
        sibling bugs of the same fault hide. So we show the model:

          * the statically reachable region from the root cause
            (fuzz-introspector's call graph), capped for context budget;
          * which of those functions harnesses already in the set have
            exercised, and which crashes they already found;

        and ask it to push into the *uncovered* part / a *different*
        crash. This is what turns N independent samples into a
        coverage-spreading variant-analysis suite."""
        cap = config.MAX_REACHABLE_IN_PROMPT
        shown = reachable[:cap]
        covered_set = set(covered)
        remaining = [r for r in shown if r not in covered_set]

        parts: List[str] = [
            "Variant analysis — you are contributing ONE harness to a "
            "SET of harnesses that together must interrogate the root "
            "cause from as many angles as possible. The patched lines "
            "sit at the head of the following region of statically "
            "reachable functions (from the project call graph). Sibling "
            "bugs of this fault are most likely to live inside this "
            "region:",
            "<root_cause_reachable>",
            *(f"- {name}" for name in shown),
            "</root_cause_reachable>",
        ]
        if len(reachable) > cap:
            parts.append(
                f"(+{len(reachable) - cap} more reachable functions "
                "omitted for brevity.)"
            )

        if covered or signatures:
            parts.append("")
            parts.append(
                "Harnesses already accepted into the set have exercised "
                "the functions and produced the crashes below. Do NOT "
                "simply reproduce these — your harness is most valuable "
                "if it drives a DIFFERENT reachable function and/or "
                "surfaces a DIFFERENT crash, while still funnelling "
                "through the patched code so it remains a test of THIS "
                "root cause:"
            )
            if covered:
                parts.append("Already-covered functions:")
                parts.extend(f"- {c}" for c in sorted(covered_set))
            if signatures:
                parts.append("Crash signatures already found:")
                parts.extend(f"- {s}" for s in signatures)
            if remaining:
                parts.append("")
                parts.append(
                    "Still-uncovered reachable functions worth steering "
                    "toward (pick one or more as your target downstream "
                    "of the patched code):"
                )
                parts.extend(f"- {r}" for r in remaining)
        else:
            parts.append("")
            parts.append(
                "This is the first harness in the set: establish the "
                "most direct path from the fuzz entrypoint through the "
                "patched code into this reachable region."
            )
        return '\n'.join(parts)

    def _chain_of_thought(self) -> str:
        return '\n'.join([
            "Before writing the harness, in 2–3 sentences identify:",
            "(a) the bug class — what category of fault the patch "
            "addresses;",
            "(b) the minimal input shape that would drive the "
            "touched functions into the buggy behaviour.",
            "Put this reasoning inside a `/* ... */` block at the "
            "very top of the .java file (above the package "
            "statement) so the output remains a single compilable "
            "file. Then write the harness.",
        ])

    def _guidelines(self, package: Optional[str]) -> str:
        if package:
            package_line = (
                f"- Declare the harness in package `{package}` "
                f"(`package {package};` at the top of the file). "
                "Putting it in the same package as the touched code "
                "lets it access package-private types, fields, and "
                "methods directly — do NOT use reflection to reach "
                "private state."
            )
        else:
            package_line = (
                "- Declare the harness in the same package as the "
                "touched code (read the `package X.Y.Z;` line at "
                "the top of the modified file shown in the patch "
                "above and copy it). Same-package access lets you "
                "call package-private methods and constructors "
                "directly — do NOT use reflection to reach private "
                "state."
            )

        return '\n'.join([
            "Use the experience you have writing Jazzer harnesses "
            "to compose this one. Hard constraints:",
            "",
            "- The harness must be a Jazzer (JVM libFuzzer port) "
            "harness. The entrypoint must be exactly:",
            "",
            "    public static void fuzzerTestOneInput"
            "(com.code_intelligence.jazzer.api.FuzzedDataProvider data)",
            "",
            "- Name the public class `FuzzHarness`.",
            "",
            package_line,
            "",
            "- FuzzedDataProvider has the following method "
            "signatures. Use ONLY these — do not invent overloads:",
            "",
            "    int     consumeInt()                       // any int",
            "    int     consumeInt(int min, int max)       // inclusive bounds",
            "    boolean consumeBoolean()",
            "    String  consumeString(int maxLength)       // ONE arg, not (min, max)",
            "    String  consumeAsciiString(int maxLength)",
            "    String  consumeRemainingAsString()",
            "    byte[]  consumeBytes(int maxLength)",
            "    byte[]  consumeRemainingAsBytes()",
            "    int     remainingBytes()                   // bytes left in the buffer",
            "",
            "- Use those to derive the inputs the target functions "
            "need. Catch checked exceptions the target declares and "
            "either rethrow them as RuntimeException or simply "
            "return, so only unexpected exceptions surface as "
            "findings.",
            "",
            "- Make sure the harness explores the code path changed "
            "by the patch so the root cause of the bug is reachable "
            "from the fuzz entrypoint.",
            "",
            "- Return ONLY the raw Java source for FuzzHarness.java. "
            "A leading `/* ... */` comment block containing your "
            "reasoning is allowed (and expected, per the previous "
            "section); no markdown code fences, no prose outside "
            "the .java file. The file must compile as-is with "
            "`javac` against the project's classpath plus "
            "jazzer-api.jar.",
        ])