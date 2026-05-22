"""Build the chat-completion prompt that asks the LLM to write a Jazzer
harness for the patched code."""
import os
from typing import List, Dict

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
        prompt += self._guidelines()

        print("#" * 20 + " prompt " + "#" * 20)
        print(prompt)
        print("#" * 48)

        # gpt-oss models follow the OpenAI "harmony" chat format. The
        # local servers (Ollama / LM Studio) apply that template
        # automatically when you use the standard chat-completions API,
        # so we don't have to do anything special here.
        return [
            {'role': 'system', 'content':
                f'You are an expert {self.language} security engineer '
                'who writes Jazzer harnesses. Return only compilable '
                'Java code, no commentary.'},
            {'role': 'user', 'content': prompt},
        ]

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

    def _guidelines(self) -> str:
        guidleine = """I expect you to be great at writing fuzz harnesses and already have a lot of experience writing Jazzer harnesses for Java. You should use the knowledge you have to compose the harness for me. Here are a few more guidelines:

        - The harness must be a Jazzer (JVM libFuzzer port) harness. The entrypoint must be exactly:

        public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data)

        - Wrap the entrypoint in a public class named `FuzzHarness` in the default package (no `package` statement) so it can be compiled directly with javac.

        - Use `data.consumeString`, `data.consumeInt`, `data.consumeBytes`, etc. to derive the inputs your target functions need from the fuzzer-supplied bytes. Catch checked exceptions the target declares and either rethrow them as RuntimeException or simply return, so only unexpected exceptions surface as findings.

        - Make sure the harness explores the code path changed by the patch so the root cause of the bug is reachable from the fuzz entrypoint. Prefer driving the touched functions through the same call sites shown in the cross-references above.

        - Return ONLY the raw Java source for FuzzHarness.java. No markdown code fences, no commentary, no surrounding explanation. The file must compile as-is with javac against the project's classpath plus jazzer-api.jar.
        """

        return guidleine