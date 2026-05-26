"""Build the chat-completion prompt that asks the LLM to write a Jazzer
harness for the patched code."""
import os
from typing import List, Dict, Optional

from analysis import PatchContext, TouchedFunction


class PromptBuilder:
    """Builds the chat-completion messages from the extracted patch
    context. Targets Jazzer (JVM libFuzzer port) so the result can be
    compiled against the Java project."""

    def __init__(self, language: str = 'Java'):
        self.language = language

    def build(self, buggy_dir: str,
              context: PatchContext) -> List[Dict[str, str]]:
        codebase = os.path.basename(buggy_dir.rstrip('/'))

        prompt = self._intro(codebase)
        prompt += self._patch_block(context.patch_text)
        for fn in context.functions:
            prompt += self._function_block(fn)
        prompt += self._guidelines(context.package)

        print("#" * 20 + " prompt " + "#" * 20)
        print(prompt)
        print("#" * 48)

        # gpt-oss models follow the OpenAI "harmony" chat format. The
        # local servers (Ollama / LM Studio) apply that template
        # automatically when you use the standard chat-completions API,
        # so we don't have to do anything special here. Putting the
        # long instructions as a user message (rather than system)
        # tends to give better results with gpt-oss-20b because its
        # system channel is intended for short, stable instructions
        # like a persona / policy.
        return [
            {'role': 'system', 'content':
                f'You are an expert {self.language} security engineer '
                'who writes Jazzer harnesses. Return only compilable '
                'Java code, no commentary.'},
            {'role': 'user', 'content': prompt},
        ]

    # --- sections --------------------------------------------------------

    def _intro(self, codebase: str) -> str:
        return ("""Hello. You are a %s security engineer and you need to write a fuzzing harness for a codebase you are analysing.

                The codebase is called %s. A patch has been applied that touches the functions listed below. Your harness should exercise those functions so the behaviour changed by the patch is reachable from the fuzz entrypoint.\n""" % (self.language, codebase))

    def _patch_block(self, patch_text: str) -> str:
        return ("""The patch under analysis is:
                <patch>
                %s
                </patch>
                """ % patch_text)

    def _function_block(self, fn: TouchedFunction) -> str:
        block = ("""Function: %s has the following function signature:
                <signature>
                %s
                </signature>

                and the following source code:

                <code>
                %s
                </code>
                """ % (fn.func_name, fn.func_signature, fn.func_source))

        if fn.xrefs:
            block += ("""The function is used in other places of the code. Use these cross-references as examples of how to call the target function in the fuzzing harness you write:\n""")
            for xref in fn.xrefs:
                block += '<xref>\n%s\n</xref>\n' % xref
        return block

    def _guidelines(self, package: Optional[str]) -> str:
        if package:
            package_line = (
                f"    - Declare the harness in package `{package}` "
                f"(`package {package};` at the top of the file). "
                "Putting it in the same package as the touched code lets "
                "it access package-private types, fields, and methods "
                "directly — do NOT use reflection to reach private state."
            )
        else:
            package_line = (
                "    - Declare the harness in the same package as the "
                "touched code (read the `package X.Y.Z;` line at the top "
                "of the modified file shown in the patch above and copy "
                "it). Same-package access lets you call package-private "
                "methods and constructors directly — do NOT use "
                "reflection to reach private state."
            )

        return """I expect you to be great at writing fuzz harnesses and already have a lot of experience writing Jazzer harnesses for Java. You should use the knowledge you have to compose the harness for me. Here are a few more guidelines:

            - The harness must be a Jazzer (JVM libFuzzer port) harness. The entrypoint must be exactly:

            public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)

            - Name the public class `FuzzHarness`.

            %s

            - FuzzedDataProvider has the following method signatures. Use ONLY these — do not invent overloads:

                int     consumeInt()                       // any int
                int     consumeInt(int min, int max)       // inclusive bounds
                boolean consumeBoolean()
                String  consumeString(int maxLength)       // ONE arg, not (min, max)
                String  consumeAsciiString(int maxLength)
                String  consumeRemainingAsString()
                byte[]  consumeBytes(int maxLength)
                byte[]  consumeRemainingAsBytes()
                int     remainingBytes()                   // bytes left in the buffer

            - Use those to derive the inputs the target functions need. Catch checked exceptions the target declares and either rethrow them as RuntimeException or simply return, so only unexpected exceptions surface as findings.

            - Make sure the harness explores the code path changed by the patch so the root cause of the bug is reachable from the fuzz entrypoint. Prefer driving the touched functions through the same call sites shown in the cross-references above.

            - Return ONLY the raw Java source for FuzzHarness.java. No markdown code fences, no commentary, no surrounding explanation. The file must compile as-is with javac against the project's classpath plus jazzer-api.jar.
            """ % package_line