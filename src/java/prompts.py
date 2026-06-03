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
import re
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

        # Hard constraints lead: the entrypoint signature, class name and
        # no-fences rule are what actually gate compilation, and a 20B
        # model weights the head of the prompt most. Context (patch,
        # sources, worked example) follows; the reasoning directive comes
        # last so it's the final thing before the model writes.
        sections: List[str] = [
            self._hard_constraints(context.package),
            self._intro(codebase),
            self._patch_block(context.patch_text),
        ]
        if context.source_imports:
            sections.append(self._imports_block(context.source_imports))
        for fn in context.functions:
            sections.append(self._function_block(fn))
        if failure_tests:
            sections.append(self._failure_test_block(
                failure_tests,
                signatures=[fn.func_signature for fn in context.functions],
            ))
        if context.root_cause_reachable:
            sections.append(self._variant_analysis_block(
                context.root_cause_reachable,
                covered_functions or [],
                found_signatures or [],
            ))
        sections.append(self._fdp_reference())
        sections.append(self._skeleton_block(context.package))
        sections.append(self._chain_of_thought())

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

    def _imports_block(self, imports: List[str]) -> str:
        """Verbatim import statements from the modified source file(s).
        Gives the model the correct package paths for types it otherwise
        guesses wrong (Range, RectangleEdge, Size2D, etc.)."""
        return '\n'.join([
            "The modified source file uses these imports — copy them "
            "exactly when you need these types:",
            "<source_imports>",
            *imports,
            "</source_imports>",
        ])

    def _intro(self, codebase: str) -> str:
        return '\n'.join([
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

    def _failure_test_block(self, failure_tests: List[FailureTest],
                            signatures: Optional[List[str]] = None,
                            max_test_chars: int = 1500) -> str:
        """Seed the prompt with the bug-triggering test(s) shipped by
        Defects4J. The LLM should treat the inputs they construct as a
        worked example of values that already reach the root cause. When
        we know the throwable the test fails with, we name it so the
        harness aims to reproduce that specific crash.

        `signatures` are the touched-function signatures, used to pick
        which seed lines to highlight (array-null lines for an NPE on an
        array target; out-of-bounds integer-argument lines for an
        index/bounds exception). Without them we fall back to the full
        body only.

        Long test bodies are truncated to `max_test_chars`: a handful of
        cases shows the construction pattern, and dumping a large
        combinatorial test (e.g. Chart-13's 31-case arrangement test)
        starves gpt-oss-20b of output budget and produces empty
        completions."""
        sigs = signatures or []
        crash_types = sorted({ft.exception_type for ft in failure_tests
                              if ft.exception_type})
        parts: List[str] = [
            "A known failing test in the project already triggers this "
            "bug. Reconstruct the object setup it builds — the harness "
            "does not have to take everything from FuzzedDataProvider. "
            "Use FuzzedDataProvider to vary the parameters the test "
            "hard-codes (dimensions, which edges/branches are taken, "
            "constraint values), reconstructing the surrounding objects "
            "the way the test does so the touched code path is reached.",
        ]
        if crash_types:
            joined = ', '.join(crash_types)
            parts.append(
                "On the buggy version this fault surfaces as an uncaught "
                f"{joined}. Your harness should drive the touched code "
                "into that same failure so Jazzer reports it as a "
                "finding. Do NOT catch and swallow that throwable — let "
                "it propagate out of fuzzerTestOneInput. Only catch the "
                "checked exceptions the target method declares (returning "
                "or rethrowing as RuntimeException), so the genuine fault "
                "is the only thing that surfaces."
            )
        for ft in failure_tests:
            if ft.method_source:
                hint, lines = self._highlight_trigger_calls(
                    ft.method_source, crash_types, sigs)
                if lines:
                    parts.extend([
                        hint,
                        f'<key_calls class="{ft.test_class}" '
                        f'method="{ft.test_method}">',
                        lines,
                        "</key_calls>",
                    ])
                body = ft.method_source
                if len(body) > max_test_chars:
                    body = (body[:max_test_chars]
                            + "\n        // ... (test truncated; the cases "
                              "above show the construction pattern)")
                parts.extend([
                    f'<failing_test class="{ft.test_class}" '
                    f'method="{ft.test_method}">',
                    body,
                    "</failing_test>",
                ])
            else:
                parts.append(
                    f'<failing_test class="{ft.test_class}" '
                    f'method="{ft.test_method}" />'
                )
        return '\n'.join(parts)

    # Crash classes that come from indexing past the end of a string or
    # array — the seed lines worth surfacing are the ones whose numeric
    # arguments are large relative to the accompanying string/collection.
    _BOUNDS_EXCEPTIONS = frozenset({
        'java.lang.StringIndexOutOfBoundsException',
        'java.lang.ArrayIndexOutOfBoundsException',
        'java.lang.IndexOutOfBoundsException',
    })

    @classmethod
    def _highlight_trigger_calls(cls, method_source: str,
                                 crash_types: List[str],
                                 signatures: List[str],
                                 max_lines: int = 8):
        """Pick the seed lines most likely to BE the trigger, given the
        crash class and the target signature, and return (hint, lines).

        Two dispatch arms, because 'which assertion is load-bearing'
        depends entirely on the bug class — keying on the token `null`
        alone (the previous behaviour) mislabels an integer-bounds bug
        whose only `null` is an incidental argument, which is exactly how
        Lang-45 got steered wrong. Returns ('', '') when no arm applies,
        so the full body is the only seed."""
        has_array_param = any('[]' in s for s in signatures)
        is_bounds = any(c in cls._BOUNDS_EXCEPTIONS for c in crash_types)

        # Arm 1: index/bounds crash. Surface calls whose integer literals
        # exceed the length of the string literal in the same call — those
        # are the out-of-bounds drivers (e.g. abbreviate("0123456789",15,20)
        # where 15/20 > 10). This is the Lang-45 case.
        if is_bounds:
            lines = cls._lines_with_oversized_ints(method_source, max_lines)
            if lines:
                hint = (
                    "This is an index/bounds crash. The trigger lines are "
                    "the ones where a numeric argument is LARGER than the "
                    "length of the string passed in the same call — that "
                    "out-of-range index is what overruns the buffer. The "
                    "other lines keep the indices in range and only show "
                    "the happy path. Mirror these specific calls, and use "
                    "FuzzedDataProvider to push the numeric arguments at "
                    "or beyond the string length:"
                )
                return hint, lines

        # Arm 2: NPE against an array-taking target. Surface the calls
        # that put a null *inside* an otherwise-populated array. This is
        # the Lang-39 case.
        if has_array_param:
            lines = cls._lines_with_null(method_source, max_lines)
            if lines:
                hint = (
                    "The trigger lines are the ones that pass a `null` "
                    "array element (or a null array) while the companion "
                    "array is non-null — for this NPE that null is almost "
                    "always the cause, and the rest is the happy path. "
                    "Mirror these specific calls:"
                )
                return hint, lines

        return '', ''

    @staticmethod
    def _lines_with_null(method_source: str, max_lines: int):
        hits = [ln.strip() for ln in method_source.splitlines()
                if 'null' in ln and '(' in ln]
        if not hits:
            return ''
        if len(hits) > max_lines:
            hits = hits[:max_lines] + ["// ... (further null-bearing "
                                       "calls omitted)"]
        return '\n'.join(hits)

    @staticmethod
    def _lines_with_oversized_ints(method_source: str, max_lines: int):
        """Lines containing a call where an integer ARGUMENT exceeds the
        length of a double-quoted string literal on the same line. Best-
        effort and string-literal-based on purpose: it needs no Java
        parse, and the trigger seeds in these tests are hard-coded
        literals (the whole reason they're a usable worked example).

        Critically, string literals are removed from the line BEFORE the
        integer scan — otherwise the digits inside a test string like
        "0123456789" get read as the integer 123456789 and every line
        looks oversized. We compare real numeric arguments against the
        longest removed string's length."""
        str_lit = re.compile(r'"((?:[^"\\]|\\.)*)"')
        int_lit = re.compile(r'(?<![\w.])(-?\d+)(?![\w.])')
        hits = []
        for ln in method_source.splitlines():
            if '(' not in ln:
                continue
            strings = str_lit.findall(ln)
            if not strings:
                continue
            longest = max((len(s) for s in strings), default=0)
            # A line whose only strings are empty (e.g. abbreviate(null,
            # 1, -1, "")) has nothing to index into — skip it; the int
            # being "larger" than length 0 is meaningless there.
            if longest == 0:
                continue
            # Blank out the string literals so their digits aren't scanned
            # as integer arguments.
            without_strings = str_lit.sub('""', ln)
            ints = [int(m) for m in int_lit.findall(without_strings)]
            # An argument past the string's end is the overrun driver; -1
            # is the method's documented "no limit" sentinel, so exclude
            # it to avoid flagging happy-path lines.
            if any(n > longest for n in ints if n != -1):
                hits.append(ln.strip())
        if not hits:
            return ''
        if len(hits) > max_lines:
            hits = hits[:max_lines] + ["// ... (further out-of-range "
                                       "calls omitted)"]
        return '\n'.join(hits)

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
            "Before writing the input-construction code, concisely "
            "identify (a) the bug class and (b) the minimal input shape "
            "that drives the touched functions into the buggy behaviour. "
            "Put this as a `/* ... */` comment on the first line INSIDE "
            "the entrypoint body, where the skeleton marks "
            "`// >>> YOUR CODE HERE <<<`, then write the construction "
            "code below it. Do not put anything above the package line.",
        ])

    def _skeleton_block(self, package: Optional[str]) -> str:
        """Hand the model a complete, compilable file and ask it to fill
        in ONLY the body. The scaffolding the 20B model repeatedly gets
        wrong (package line, the jazzer import, the class name, the
        entrypoint signature, stray `main` methods, invented FDP-provider
        classes) is given to it, so the only thing left to reason about is
        input construction — which is the thing that actually determines
        whether the bug is reached. This collapses the prose-not-code,
        wrong-package, and hallucinated-import failure classes seen across
        attempts 1-44 into a single fill-in-the-blank task."""
        pkg_line = (f"package {package};" if package
                    else "package <copy the package line from the patch>;")
        return '\n'.join([
            "Produce your harness by completing the skeleton below. "
            "Reproduce it EXACTLY, character for character, except for "
            "the single region marked `// >>> YOUR CODE HERE <<<`, which "
            "you replace with your input-construction code. Do NOT change "
            "the package line, the import, the class name, or the "
            "entrypoint signature. Do NOT add a `main` method. Do NOT add "
            "other imports unless javac would need them, and if so add "
            "them only in the import region directly below the existing "
            "import. Output the completed file as raw Java — no fences.",
            "<skeleton>",
            pkg_line,
            "",
            "import com.code_intelligence.jazzer.api.FuzzedDataProvider;",
            "",
            "public class FuzzHarness {",
            "    public static void fuzzerTestOneInput("
            "com.code_intelligence.jazzer.api.FuzzedDataProvider data) {",
            "        // >>> YOUR CODE HERE <<<",
            "        // Construct the inputs that drive the touched code",
            "        // into the documented crash, then call the target",
            "        // method directly (same package — no reflection).",
            "        // Let the genuine fault propagate out of this method;",
            "        // catch only the checked exceptions the target",
            "        // declares (rethrow as RuntimeException).",
            "    }",
            "}",
            "</skeleton>",
        ])

    def _hard_constraints(self, package: Optional[str]) -> str:
        if package:
            package_line = (
                f"- Declare it in package `{package}` "
                f"(`package {package};` at the top)."
            )
        else:
            package_line = (
                "- Declare it in the same package as the touched code "
                "(copy the `package X.Y.Z;` line from the modified file "
                "shown in the patch below)."
            )
        return '\n'.join([
            "Write a Jazzer (JVM libFuzzer port) harness. Non-negotiable:",
            "",
            "- Public class named exactly `FuzzHarness`.",
            "- Entrypoint exactly:",
            "    public static void fuzzerTestOneInput"
            "(com.code_intelligence.jazzer.api.FuzzedDataProvider data)",
            package_line,
            "  Same-package placement reaches package-private members "
            "directly — do NOT use reflection.",
            "- Return ONLY raw Java source for FuzzHarness.java: no "
            "markdown fences, no prose outside the file (a leading "
            "`/* ... */` comment is allowed). It must compile with "
            "`javac` against the project classpath plus jazzer-api.jar.",
            "- CRITICAL: If your response contains ANY text that is not "
            "valid Java source — prose, markdown, backticks, numbered "
            "steps, explanations — the entire response is rejected. "
            "Output the .java file and nothing else.",
        ])

    def _fdp_reference(self) -> str:
        return '\n'.join([
            "Derive all inputs from FuzzedDataProvider. Use ONLY these "
            "methods — do not invent overloads:",
            "",
            "    int     consumeInt()                       // any int",
            "    int     consumeInt(int min, int max)       // inclusive bounds",
            "    byte    consumeByte()                       // any byte",
            "    boolean consumeBoolean()",
            "    String  consumeString(int maxLength)       // ONE arg, not (min, max)",
            "    String  consumeAsciiString(int maxLength)",
            "    String  consumeRemainingAsString()",
            "    byte[]  consumeBytes(int maxLength)",
            "    byte[]  consumeRemainingAsBytes()",
            "    int     remainingBytes()                   // bytes left in the buffer",
            "",
            "Catch the checked exceptions the target declares (rethrow "
            "as RuntimeException or return) so only unexpected "
            "exceptions surface as findings.",
        ])