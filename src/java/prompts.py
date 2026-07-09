"""Build chat-completion prompts for Jazzer harness generation.

Sections are assembled as lists of lines joined with '\n' so Python
indentation never leaks into the prompt and Java literals with '{' / '}'
are spliced verbatim without str.format choking on them.
"""
import os
import re
from typing import List, Dict, Optional

import config
from analysis import PatchContext, TouchedFunction
from crash_input import CrashInput
from failure_test import FailureTest


class PromptBuilder:
    """Builds chat-completion messages from a PatchContext."""

    def __init__(self, language: str = 'Java'):
        self.language = language

    def build(self, buggy_dir: str,
              context: PatchContext,
              failure_tests: Optional[List[FailureTest]] = None,
              covered_functions: Optional[List[str]] = None,
              found_signatures: Optional[List[str]] = None,
              crash_input: Optional["CrashInput"] = None,
              bug_kind: str = "crashing",
              semantic_test: Optional[FailureTest] = None,
              ) -> List[Dict[str, str]]:
        """Assemble the chat-completion messages.

        covered_functions / found_signatures describe harnesses already
        accepted this campaign; when present, the variant-analysis block
        steers the new harness toward uncovered code and a different crash.

        ``bug_kind`` selects the oracle the harness is built around:

          * ``"crashing"`` (default) — the bug's trigger test fails with a
            thrown application exception. The harness reaches the fault and
            lets that throwable escape; nothing about this path changes.
          * ``"semantic"`` — the trigger test fails a JUnit assertion, so
            the code returns a WRONG value without throwing. There is no
            crash to catch, so instead we lift the expected value out of one
            trigger test's ``assertEquals`` and have the harness throw when
            the patched code disagrees. ``semantic_test`` is the single
            trigger test to lift from this attempt (the caller round-robins
            across the bug's trigger tests so each harness checks a
            different one)."""
        if bug_kind == "semantic":
            return self._build_semantic(
                buggy_dir, context,
                semantic_test=semantic_test,
                all_failure_tests=failure_tests or [],
                covered_functions=covered_functions,
                found_signatures=found_signatures,
            )
        codebase = os.path.basename(buggy_dir.rstrip('/'))

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
                method_names=[fn.func_name for fn in context.functions],
                crash_input=crash_input,
            ))
        if context.root_cause_reachable:
            sections.append(self._variant_analysis_block(
                context.root_cause_reachable,
                covered_functions or [],
                found_signatures or [],
            ))
        sections.append(self._metamorphic_block())
        sections.append(self._fdp_reference())
        sections.append(self._skeleton_block(context.package))

        prompt = '\n\n'.join(sections)

        print("#" * 20 + " prompt " + "#" * 20)
        print(prompt)
        print("#" * 48)

        return [
            {'role': 'system', 'content':
                f'You are an expert {self.language} security engineer '
                'who writes Jazzer fuzzing harnesses. Return a single '
                'compilable .java file — no markdown fences, no prose '
                'outside the file.'},
            {'role': 'user', 'content': prompt},
        ]

    # --- semantic (non-crashing) path ------------------------------------

    def _build_semantic(self, buggy_dir: str,
                        context: PatchContext,
                        semantic_test: Optional[FailureTest],
                        all_failure_tests: List[FailureTest],
                        covered_functions: Optional[List[str]] = None,
                        found_signatures: Optional[List[str]] = None,
                        ) -> List[Dict[str, str]]:
        """Build the prompt for a semantic (assertion-failing) bug.

        The harness reaches the patched code through the real API exactly
        as in the crashing path, but its oracle is an assertion lifted from
        ``semantic_test`` rather than an escaping throwable: it reconstructs
        the call the test makes, compares the result against the expected
        value baked into the test's ``assertEquals``, and throws when they
        differ. On the buggy/overfitting code the value is wrong and the
        harness throws (Jazzer reports a finding); on a correctly patched
        version it matches and the harness returns cleanly."""
        codebase = os.path.basename(buggy_dir.rstrip('/'))
        chosen = semantic_test or (all_failure_tests[0]
                                   if all_failure_tests else None)

        sections: List[str] = [
            self._hard_constraints(context.package),
            self._intro(codebase),
            self._patch_block(context.patch_text),
        ]
        if context.source_imports:
            sections.append(self._imports_block(context.source_imports))
        for fn in context.functions:
            sections.append(self._function_block(fn))
        sections.append(self._lifted_assertion_block(
            chosen, all_failure_tests))
        if context.root_cause_reachable:
            sections.append(self._variant_analysis_block(
                context.root_cause_reachable,
                covered_functions or [],
                found_signatures or [],
            ))
        sections.append(self._fdp_reference())
        sections.append(self._skeleton_block(context.package))

        prompt = '\n\n'.join(sections)

        print("#" * 20 + " prompt (semantic) " + "#" * 20)
        print(prompt)
        print("#" * 48)

        return [
            {'role': 'system', 'content':
                f'You are an expert {self.language} security engineer '
                'who writes Jazzer fuzzing harnesses. Return a single '
                'compilable .java file — no markdown fences, no prose '
                'outside the file.'},
            {'role': 'user', 'content': prompt},
        ]

    def _lifted_assertion_block(self, chosen: Optional[FailureTest],
                                all_failure_tests: List[FailureTest]) -> str:
        """Instruct the model to lift the expected value from the trigger
        test's assertion and throw on mismatch.

        This is the semantic-bug oracle. Unlike the crashing path — where
        the harness lets a real throwable escape — here the *harness itself*
        is the thing that throws, by comparing the patched code's actual
        output against the expected value the failing test hard-codes in its
        assertEquals. We surface that test's full source so the model can
        copy both the call and the expected literal verbatim; copying beats
        inferring, exactly as with crash anchors."""
        parts: List[str] = [
            "THIS IS A NON-CRASHING (SEMANTIC) BUG. The trigger test does"
            " not throw — it fails a JUnit assertion because the code returns"
            " a WRONG value. There is therefore no exception to catch. Your"
            " harness must SUPPLY the oracle:",
            "",
            "1. LIFT: read the failing test below and find its assertion"
            " (assertEquals / assertTrue / assertSame ...). Identify (a) the"
            " call(s) it makes on the real API and (b) the EXPECTED value it"
            " checks against. Copy both VERBATIM — do not infer or recompute"
            " the expected value; use the literal the test hard-codes.",
            "",
            "2. RECONSTRUCT: in fuzzerTestOneInput, make that same call"
            " through the real public API, exactly as the test does, to get"
            " the ACTUAL value.",
            "",
            "3. ASSERT: if the actual value does not equal the expected"
            " value, `throw new com.code_intelligence.jazzer.api."
            "FuzzerSecurityIssueLow(\"semantic mismatch: <what differed>\")`."
            " Jazzer reports that throw as a finding, exactly like a crash,"
            " so the rest of the pipeline scores it unchanged. If they match,"
            " return normally.",
            "",
            "4. THEN GENERALISE — but keep every assertion TRUSTED. The"
            " lifted pair tells you the answer for ONE input only. Do not"
            " invent an expected value for an arbitrary fuzzed input: an"
            " assertion you cannot justify will fire on CORRECT patches too"
            " (a false positive, the worst outcome). Extend coverage only in"
            " ways where the correct answer is independently known:",
            "",
            "  (a) CONSTRUCT THE INPUT FROM A KNOWN ANSWER. Pick a value with"
            " FuzzedDataProvider, build the canonical input that must map to"
            " it, and assert you recover it — the answer is trusted because"
            " you chose it first. E.g. fuzz an int n, format it, parse it"
            " back, assert == n.",
            "",
            "  (b) EQUIVALENCE TO THE SEED. The seed input has properties"
            " that fix its answer. Generate other inputs that SHARE those"
            " properties and must therefore share the seed's expected value,"
            " and assert they equal it. E.g. if the contract is"
            " case-insensitive and foo(\"abc\")==X, then foo(\"ABC\") and"
            " foo(\"Abc\") must also ==X. This turns the one lifted value into"
            " an oracle for a whole family of inputs.",
            "",
            "  (c) METAMORPHIC RELATION between two real calls (see below) —"
            " trusted because it holds for ANY correct implementation.",
            "",
            "  GUARDRAIL: for any fuzzed input where you cannot justify the"
            " expected value by (a), (b), or (c), still CALL the API on it"
            " (this exercises the patched path), but do NOT assert on its"
            " result — just return. Assert only trusted answers; explore"
            " everything else without asserting.",
        ]
        if chosen is not None:
            entry = self._entry_point_hint([chosen])
            if entry:
                parts.append(entry)
            if chosen.method_source:
                parts.extend([
                    "Lift the call and the expected value from THIS test"
                    " (this attempt's target):",
                    f'<failing_test class="{chosen.test_class}"'
                    f' method="{chosen.test_method}">',
                    chosen.method_source,
                    "</failing_test>",
                ])
            else:
                parts.append(
                    "Target trigger test (source unavailable — reconstruct"
                    f" from its name): {chosen.test_class}::"
                    f"{chosen.test_method}"
                )
        if len(all_failure_tests) > 1:
            others = ', '.join(
                f'{ft.test_class}::{ft.test_method}'
                for ft in all_failure_tests if ft is not chosen)
            parts.append(
                "Other trigger tests for this same bug (siblings probing the"
                f" same root cause; other harnesses target these): {others}.")
        # Reuse the existing metamorphic guidance as the principled way to
        # extend coverage beyond the single lifted input.
        parts.append(self._metamorphic_block())
        return '\n'.join(parts)

    # --- sections --------------------------------------------------------

    def _hard_constraints(self, package: Optional[str]) -> str:
        if package:
            package_line = (
                f"- Package: `{package}` "
                f"(`package {package};` at the top)."
            )
        else:
            package_line = (
                "- Package: same as the touched code "
                "(copy the `package X.Y.Z;` line from the patch)."
            )
        return '\n'.join([
            "Write a Jazzer harness. Rules:",
            "",
            "- Class named exactly `FuzzHarness`.",
            "- Entrypoint exactly:",
            "    public static void fuzzerTestOneInput"
            "(com.code_intelligence.jazzer.api.FuzzedDataProvider data)",
            package_line,
            "  Same-package placement gives direct access to package-private"
            " members — no reflection.",
            "- Output raw Java only: no markdown fences, no prose. A leading"
            " `/* ... */` comment is allowed. Must compile with `javac`"
            " against the project classpath plus jazzer-api.jar.",
            "- Reach the fault through the library's REAL code, not a"
            " hand-built stand-in. You may construct and use classes that"
            " already exist in the library, but do NOT write your own"
            " subclass, anonymous class, mock, or stub of the patched class"
            " or any of its callees to force the crash. A harness that"
            " manufactures the crash with a custom implementation proves"
            " nothing about real usage and is rejected.",
        ])

    def _intro(self, codebase: str) -> str:
        return (
            f"Codebase: `{codebase}`. The patch below touches the functions"
            " listed. Your harness must call those functions so the patched"
            " behaviour is reachable from the fuzz entrypoint."
        )

    def _patch_block(self, patch_text: str) -> str:
        return '\n'.join([
            "Patch under analysis:",
            "<patch>",
            patch_text,
            "</patch>",
        ])

    def _imports_block(self, imports: List[str]) -> str:
        return '\n'.join([
            "Available imports from the modified file (copy exactly"
            " when you need these types):",
            "<source_imports>",
            *imports,
            "</source_imports>",
        ])

    def _function_block(self, fn: TouchedFunction) -> str:
        parts: List[str] = [
            f"Function `{fn.func_name}`:",
            "<signature>",
            fn.func_signature,
            "</signature>",
            "<code>",
            fn.func_source,
            "</code>",
        ]
        if fn.xrefs:
            parts.append(
                "Call-site examples (use these as a guide for constructing"
                " the target call):"
            )
            for xref in fn.xrefs:
                parts.extend(["<xref>", xref, "</xref>"])
        callee_block = self._related_callees_block(fn)
        if callee_block:
            parts.append(callee_block)
        return '\n'.join(parts)
    
    def _related_callees_block(self, fn: TouchedFunction) -> str:
        """Render the methods this function calls whose behaviour the
        patched code depends on. The body shows how their results are
        USED; the declarations below show what they return and how real
        implementations behave — the context needed to reason about a
        fault that spans a caller and a callee."""
        callees = getattr(fn, 'related_callees', None)
        if not callees:
            return ''

        parts: List[str] = [
            f"Methods called by `{fn.func_name}` whose behaviour the patched"
            " code depends on. The body above shows how their results are"
            " USED; the declarations below show what they return and how"
            " real implementations behave. To reach the fault you usually"
            " need to drive the target through one of these implementations"
            " with an input that makes its return value exercise the"
            " patched path.",
        ]
        for rc in callees:
            attrs = f' name="{rc.name}"'
            if rc.source_file:
                attrs += f' from="{rc.source_file}"'
            if rc.is_abstract:
                attrs += ' abstract="true"'
            parts.append(f"<callee{attrs}>")
            if rc.signature:
                parts.extend(["<signature>", rc.signature, "</signature>"])
            if rc.source:
                tag = "contract" if rc.is_abstract else "code"
                parts.extend([f"<{tag}>", rc.source, f"</{tag}>"])
            for impl_file, impl_src in rc.impls:
                parts.append(f'<implementation in="{impl_file}">')
                parts.append(impl_src)
                parts.append("</implementation>")
            parts.append("</callee>")
        return '\n'.join(parts)

    def _failure_test_block(self, failure_tests: List[FailureTest],
                            signatures: Optional[List[str]] = None,
                            method_names: Optional[List[str]] = None,
                            crash_input: Optional[CrashInput] = None,
                            max_test_chars: int = 1500) -> str:
        """Seed the prompt with the D4J trigger test(s).

        Two-step strategy: anchor on the known-crashing input first
        (guarantees the trigger gate passes), then fuzz the neighbourhood
        (catches overfitting patches that only special-cased the seed).

        When ``crash_input`` is supplied it carries the *runtime* crash
        evidence captured from the buggy checkout — the exception type,
        detail message, throw site, and anchor literal observed at the
        actual failure. That is strictly more reliable than re-inferring
        the crashing value from the test source, so it is surfaced as the
        primary anchor and its observed exception type takes precedence
        over the statically-declared one."""
        sigs = signatures or []
        crash_types = sorted({ft.exception_type for ft in failure_tests
                              if ft.exception_type})
        # Prefer the runtime-observed throwable when we captured one.
        if crash_input and crash_input.exception_type:
            crash_types = [crash_input.exception_type] + [
                t for t in crash_types if t != crash_input.exception_type]
        parts: List[str] = [
            "The failing test below shows how to reach the bug."
            " Use it with TWO strategies:",
            "",
            "1. ANCHOR: call the target with the exact input(s) from the"
            " test first. This is your guaranteed crash on the buggy version.",
            "",
            "2. EXPLORE: identify the input PROPERTY that triggers the"
            " patched line (the root cause), then use FuzzedDataProvider to"
            " generate many varied REAL inputs that satisfy that property —"
            " different lengths, positions, and surrounding content — and"
            " drive them all through the real entry point. You are testing"
            " whether the patch fixes the root cause for ALL such inputs,"
            " not just the seed; overfitting patches special-case the seed.",
        ]
        entry_point = self._entry_point_hint(failure_tests)
        if entry_point:
            parts.append(entry_point)
        if crash_types:
            joined = ', '.join(crash_types)
            target_methods = ', '.join(
                f'`{m}`' for m in (method_names or [])
            ) or 'the patched method(s)'
            parts.append(
                "On the buggy version the root cause surfaces as: "
                f"{joined} (or a sibling failure with a different signature "
                "stemming from the same root cause). Your harness must "
                "distinguish a genuine defect from the patch doing its job:\n"
                "  - The fixed code is SUPPOSED to reject invalid input cleanly. "
                "Any throwable that is a deliberate rejection of bad input is "
                "CORRECT post-fix behaviour — CATCH it inside fuzzerTestOneInput "
                "and return normally. Recognize clean rejection by exception "
                "FAMILY and context, NEVER by exact class identity or message "
                "text: a correct patch may reject the same invalid input with a "
                "DIFFERENT exception class or message than the buggy version "
                "(e.g. a specific IllegalArgumentException subclass instead of "
                "a generic one, or a null/reworded message). Any throwable in "
                "the IllegalArgumentException / NumberFormatException family — "
                "or any library-specific validation exception — raised while "
                "the code is checking its arguments counts as clean rejection.\n"
                "  - PROPAGATE a throwable only when BOTH hold: (1) it signals "
                "the root cause — its class matches the ground-truth throwable "
                "below, or it is your own assertion/metamorphic "
                "RuntimeException — AND (2) its stack trace passes through "
                f"{target_methods} or a function listed in "
                "<root_cause_reachable>. Any other throwable — INCLUDING the "
                "same exception class thrown from a different location — must "
                "be swallowed: it is a pre-existing defect outside this "
                "patch's scope, it will crash every version including "
                "correctly patched ones, and it produces a false positive. "
                "Enforce this in code, e.g.:\n"
                "    try { /* library call */ }\n"
                "    catch (RuntimeException t) {\n"
                "        if (isRootCause(t)) throw t;  // else swallow\n"
                "    }\n"
                "  where isRootCause checks instanceof against the "
                "ground-truth throwable class AND loops over t.getStackTrace() "
                "requiring a frame whose class/method matches the patched or "
                "reachable region."
            )
            parts.append(
                "VALID-BY-CONSTRUCTION INPUTS: if the ground-truth throwable "
                "is itself a validation/rejection exception "
                "(IllegalArgumentException or a subclass, "
                "NumberFormatException, a library 'invalid input' exception, "
                "...), then its signature CANNOT distinguish the bug from "
                "correct rejection — a correctly fixed version legitimately "
                "throws the same exception, possibly at the same line, when "
                "the input really is invalid. In that case, let it propagate "
                "ONLY for inputs that are VALID BY CONSTRUCTION: inputs a "
                "correct implementation is obligated to accept because you "
                "built them to satisfy the documented preconditions yourself "
                "(e.g. if the API requires start <= end, generate two values "
                "and order them BEFORE the call; if a parameter must be "
                "positive, force it positive; if elements must be non-null, "
                "supply non-null elements). For any input whose validity you "
                "cannot guarantee, catch the rejection and return normally — "
                "a rejection of a possibly-invalid input proves nothing.\n"
                "Rule of thumb: if a careful, correct version of this method "
                "would still throw that exception for that input, it is NOT a "
                "bug — swallow it. Only when the method throws on an input it "
                "was obliged to handle has it lost control of its own "
                "invariants — let that propagate."
            )
        ground_truth = self._crash_input_block(crash_input)
        if ground_truth:
            parts.append(ground_truth)
        for ft in failure_tests:
            if ft.method_source:
                hint, lines = self._highlight_trigger_calls(
                    ft.method_source, crash_types, sigs,
                    method_names=method_names or [])
                if lines:
                    parts.extend([
                        hint,
                        f'<key_calls class="{ft.test_class}"'
                        f' method="{ft.test_method}">',
                        lines,
                        "</key_calls>",
                    ])
                body = ft.method_source
                if len(body) > max_test_chars:
                    body = (body[:max_test_chars]
                            + "\n        // ... (truncated)")
                parts.extend([
                    f'<failing_test class="{ft.test_class}"'
                    f' method="{ft.test_method}">',
                    body,
                    "</failing_test>",
                ])
            else:
                parts.append(
                    f'<failing_test class="{ft.test_class}"'
                    f' method="{ft.test_method}" />'
                )
        return '\n'.join(parts)

    @staticmethod
    def _entry_point_hint(failure_tests: List[FailureTest]) -> str:
        """Name the public API the trigger test drives, so the harness
        reaches the fault through the real call chain rather than poking
        the patched method directly with a hand-built helper.

        The trigger test's enclosing class (minus a trailing 'Test') is
        the production entry point that exercised the bug — e.g. a test in
        StringUtilsTest means the real path is StringUtils. Surfacing it
        steers the model to call that, not to construct the internal type
        itself."""
        classes = []
        seen = set()
        for ft in failure_tests:
            cls = ft.test_class
            # Strip package and a trailing 'Test' to recover the API class.
            simple = cls.rsplit('.', 1)[-1]
            if simple.endswith('Test') and len(simple) > 4:
                simple = simple[:-4]
            if simple and simple not in seen:
                seen.add(simple)
                classes.append(simple)
        if not classes:
            return ''
        named = ', '.join(f'`{c}`' for c in classes)
        return (
            "REAL ENTRY POINT: in production the bug is reached through the"
            f" public API the failing test drives (here: {named}). Drive your"
            " fuzzed input through that public API so it flows along the real"
            " call chain to the patched line. Prefer this over calling the"
            " patched method directly, and never reach it via a custom"
            " implementation of a library type."
        )


    def _crash_input_block(self, crash_input: Optional[CrashInput]) -> str:
        """Render the ground-truth crash evidence as a prompt section.

        Returns '' when no evidence was captured, so the caller falls
        back to test-source anchoring exactly as before."""
        if crash_input is None or not crash_input.has_evidence:
            return ''
        parts: List[str] = [
            "GROUND-TRUTH CRASH (captured by running the trigger test on"
            " the buggy version — this is the verified failure, trust it"
            " over anything inferred from the test body):",
            "<ground_truth_crash>",
        ]
        if crash_input.exception_type:
            parts.append(f"throwable: {crash_input.exception_type}")
        if crash_input.message:
            parts.append(f"message: {crash_input.message}")
        if crash_input.throw_site:
            parts.append(f"thrown_at: {crash_input.throw_site}")
        anchor = crash_input.best_anchor
        if anchor is not None:
            parts.append(
                f'anchor_input: "{anchor}"  '
                "// hard-code this verbatim as your first call, then fuzz"
                " inputs of the same shape"
            )
        if crash_input.literals and len(crash_input.literals) > 1:
            others = ', '.join(f'"{lit}"'
                               for lit in crash_input.literals[1:])
            parts.append(f"other_observed_literals: {others}")
        parts.append("</ground_truth_crash>")
        return '\n'.join(parts)

    _BOUNDS_EXCEPTIONS = frozenset({
        'java.lang.StringIndexOutOfBoundsException',
        'java.lang.ArrayIndexOutOfBoundsException',
        'java.lang.IndexOutOfBoundsException',
    })
    _INT_PARAM_RE = re.compile(r'\b(?:int|long)\b')

    @classmethod
    def _highlight_trigger_calls(cls, method_source: str,
                                 crash_types: List[str],
                                 signatures: List[str],
                                 method_names: Optional[List[str]] = None,
                                 max_lines: int = 8):
        """Return (hint, lines) for the most likely trigger lines.

        Arm 0 (ground truth): single distinct string-literal call to a
          target method — use verbatim as anchor, fuzz the shape.
        Arm 1 (bounds): integer argument exceeds string-literal length.
        Arm 2 (NPE): null passed where an array is expected.
        Returns ('', '') when no arm fires; caller falls back to full body."""
        names = method_names or []

        literal_calls = cls._literal_arg_calls(method_source, names)
        distinct = sorted(set(literal_calls))
        if len(distinct) == 1:
            hint = (
                "Anchor call — hard-code this literal verbatim as the first"
                " call in your harness, then use FuzzedDataProvider to"
                " generate additional inputs of the same shape:"
            )
            return hint, '\n'.join(distinct)

        has_int_param = any(cls._INT_PARAM_RE.search(s) for s in signatures)
        is_bounds = any(c in cls._BOUNDS_EXCEPTIONS for c in crash_types)

        if is_bounds and has_int_param:
            lines = cls._lines_with_oversized_ints(method_source, max_lines)
            if lines:
                hint = (
                    "Trigger lines: numeric argument exceeds the string"
                    " length in the same call. Mirror these calls and use"
                    " FuzzedDataProvider to vary the numeric arguments"
                    " at or beyond the string length:"
                )
                return hint, lines

        has_array_param = any('[]' in s for s in signatures)
        if has_array_param:
            lines = cls._lines_with_null(method_source, max_lines)
            if lines:
                hint = (
                    "Trigger lines: null passed as an array element."
                    " Mirror these calls:"
                )
                return hint, lines

        return '', ''

    @classmethod
    def candidate_anchor_literals(cls, method_source: str,
                                  method_names: List[str]) -> List[str]:
        """Unquoted string literals passed to target methods in the test source.

        Used as fallback anchor candidates for CrashInputExtractor when the
        runtime trace yields no quotable value from the exception message."""
        quoted = cls._literal_arg_calls(method_source, method_names)
        return [q[1:-1] for q in quoted if len(q) >= 2]

    @staticmethod
    def _literal_arg_calls(method_source: str,
                           method_names: List[str]) -> List[str]:
        """Find `targetMethod("literal")` calls and return the literals.

        Conservative: matches only a single string-literal argument with
        no concatenation or extra args, so it stays silent on ambiguous
        inputs rather than guessing wrong."""
        if not method_names:
            return []
        name_alt = '|'.join(re.escape(n) for n in method_names)
        call_re = re.compile(
            r'\b(?:' + name_alt + r')\s*\(\s*'
            r'("(?:[^"\\]|\\.)*")'
            r'\s*\)'
        )
        return call_re.findall(method_source)

    @staticmethod
    def _lines_with_null(method_source: str, max_lines: int):
        hits = [ln.strip() for ln in method_source.splitlines()
                if 'null' in ln and '(' in ln]
        if not hits:
            return ''
        if len(hits) > max_lines:
            hits = hits[:max_lines] + ["// ... (further null-bearing calls omitted)"]
        return '\n'.join(hits)

    @staticmethod
    def _lines_with_oversized_ints(method_source: str, max_lines: int):
        """Lines where an integer argument exceeds the longest string
        literal on the same line (proxy for an out-of-range index).
        String literals are stripped before the int scan so their digits
        are not mistaken for numeric arguments."""
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
            if longest == 0:
                continue
            without_strings = str_lit.sub('""', ln)
            ints = [int(m) for m in int_lit.findall(without_strings)]
            if any(n > longest for n in ints if n != -1):
                hits.append(ln.strip())
        if not hits:
            return ''
        if len(hits) > max_lines:
            hits = hits[:max_lines] + ["// ... (further out-of-range calls omitted)"]
        return '\n'.join(hits)

    def _variant_analysis_block(self,
                                reachable: List[str],
                                covered: List[str],
                                signatures: List[str]) -> str:
        """Steer successive harnesses across the root-cause neighbourhood."""
        cap = config.MAX_REACHABLE_IN_PROMPT
        shown = reachable[:cap]
        covered_set = set(covered)
        remaining = [r for r in shown if r not in covered_set]
        parts: List[str] = [
            "This harness is ONE of a set probing the root cause of"
            " the vulnerability the patch under analysis is meant to fix."
            " The patched lines sit at the head of the reachable region"
            " below. A valid sibling bug is one that:\n"
            "  (a) lives in this region (same method or call graph), AND\n"
            "  (b) stems from the SAME root cause\n"
            "<root_cause_reachable>",
            *(f"- {name}" for name in shown),
            "</root_cause_reachable>",
        ]
        if len(reachable) > cap:
            parts.append(
                f"(+{len(reachable) - cap} more reachable functions omitted.)"
            )

        if covered or signatures:
            parts.append(
                "Already covered by earlier harnesses — target something"
                " different:"
            )
            if covered:
                parts.append("Functions covered:")
                parts.extend(f"- {c}" for c in sorted(covered_set))
            if signatures:
                parts.append("Crashes already found:")
                parts.extend(f"- {s}" for s in signatures)
                parts.append(
                    "If the crash you plan to reproduce has the SAME"
                    " signature as one already listed above, this harness"
                    " must instead win through a post-condition /"
                    " metamorphic assertion (see the MANDATORY check"
                    " below) — re-triggering an already-found signature"
                    " adds no new evidence, and a campaign of identical"
                    " crash reproducers is blind to patches that merely"
                    " delete the throw."
                )
            if remaining:
                parts.append("Uncovered functions to steer toward:")
                parts.extend(f"- {r}" for r in remaining)
        else:
            parts.append(
                "First harness: establish the most direct path from the"
                " fuzz entrypoint through the patched code."
            )
        return '\n'.join(parts)

    def _metamorphic_block(self) -> str:
        """Require a post-condition / metamorphic oracle alongside the crash.

        Crash-only harnesses miss overfitting patches that SUPPRESS the
        crash without restoring correct behaviour: delete the throw, guard
        the crashing branch into unreachability, or swap the failing
        operation for one that silently does the wrong thing. In all three
        cases no exception ever fires, so every crash-keyed harness passes
        the patch (the dominant false-negative mode observed in evaluation).
        The check is therefore MANDATORY, framed as a thought experiment
        against those adversarial patches.

        The block also carries hygiene rules learned from the false-positive
        side: a relation a correct implementation can legally break, or a
        caught exception converted into a 'violation', fires on correct
        patches too. Requiring the model to cite the documented guarantee
        behind each assertion — and to skip, never report, inputs where
        either side throws — keeps the added oracle from buying recall with
        precision."""
        return '\n'.join([
            "POST-CONDITION / METAMORPHIC CHECK (MANDATORY — catches"
            " wrong-output bugs that never throw):",
            "ASSUME THE ADVERSARY. The patch under analysis may 'fix' the"
            " bug by simply (a) deleting the throw / bookkeeping statement,"
            " (b) adding a guard that makes the crashing branch unreachable"
            " (and with it the branch's intended behaviour), or (c)"
            " replacing the failing operation with one that silently does"
            " the wrong thing (e.g. appending instead of sorted-inserting,"
            " skipping a modification-counter update). In all three worlds"
            " NO exception ever fires and a crash-only harness passes the"
            " patch. Therefore, in addition to reproducing the ground-truth"
            " failure, your harness MUST assert at least ONE observable"
            " post-condition"
            " from the documented contract of the patched method that such"
            " a patch would violate — e.g. after inserting into an"
            " auto-sorted collection, the collection is still sorted; after"
            " a removal, an iterator over the container either throws or"
            " reflects the removal (never silently yields stale state); a"
            " branch that is supposed to set a size/flag/result observably"
            " set it. State in a comment WHICH contract guarantee you"
            " assert and WHY a throw-deleting patch would break it. A"
            " harness that only reproduces the crash is incomplete.",
            "",
            "Prefer a post-condition you can read directly off the API"
            " after the call. Where none is observable, use ONE metamorphic"
            " relation — a relation between two related calls of the target"
            " that must hold for ANY correct implementation: compute both"
            " sides from REAL library calls on the fuzzed input, and throw"
            " if they disagree.",
            "- Round-trip / inverse: f(g(x)) == x  (e.g. decode(encode(x)),"
            " parse(format(v)), unescape(escape(s))).",
            "- Idempotence: f(f(x)) == f(x)  (e.g. normalise, trim, strip,"
            " canonicalise).",
            "- Equivalent inputs: two inputs that must map to the same"
            " result do  (e.g. case-insensitive parse: f(s) == f("
            "s.toUpperCase()); leading-zero / whitespace variants of the"
            " same number).",
            "- Composition / split: f(a + b) relates to f(a) and f(b)"
            " consistently  (e.g. a translator/encoder applied to a"
            " concatenation equals the concatenation of the parts).",
            "- Oracle from the input itself: when the fuzzed input is"
            " CONSTRUCTED from a known value, the result must recover it"
            " (e.g. build the canonical string for a random int n, parse"
            " it, and assert it equals n).",
            "",
            "HYGIENE RULES — violating these fires on CORRECT patches (a"
            " false positive, the worst outcome):",
            "- If EITHER side of a relation, or the call preceding a"
            " post-condition read, throws anything at all, the check does"
            " not apply to that input: catch it, skip the check, return"
            " normally. NEVER convert a caught exception into a violation —"
            " an exception is a rejection, not a wrong answer.",
            "- Before asserting a relation or post-condition, cite in a"
            " comment the documented guarantee (javadoc sentence, class"
            " contract, or invariant visible in the code shown above) that"
            " makes it hold for EVERY correct implementation, including"
            " edge cases: null elements, empty inputs, duplicates,"
            " no-solution inputs. If you cannot cite one, do not assert it.",
            "- Use only real library calls for BOTH sides (no hand-rolled"
            " reference implementation).",
            "- On violation, `throw new RuntimeException(\"metamorphic"
            " violation: <which relation> input=<...> lhs=<...>"
            " rhs=<...>\")` with the concrete values, so a reviewer can"
            " replay the disagreement. Jazzer reports that as a finding,"
            " the same as a crash.",
        ])

    def _fdp_reference(self) -> str:
        return '\n'.join([
            "Use ONLY these FuzzedDataProvider methods (no invented"
            " overloads):",
            "",
            "    int     consumeInt()                    // any int",
            "    int     consumeInt(int min, int max)    // inclusive",
            "    byte    consumeByte()",
            "    boolean consumeBoolean()",
            "    String  consumeString(int maxLength)    // ONE arg",
            "    String  consumeAsciiString(int maxLength)",
            "    String  consumeRemainingAsString()",
            "    byte[]  consumeBytes(int maxLength)",
            "    byte[]  consumeRemainingAsBytes()",
            "    int     remainingBytes()",
            "",
        ])

    def _skeleton_block(self, package: Optional[str]) -> str:
        pkg_line = (f"package {package};" if package
                    else "package <copy from patch>;")
        return '\n'.join([
            "Complete the skeleton below. Fill in ONLY the"
            " `// >>> YOUR CODE HERE <<<` region. Do NOT change the"
            " package, import, class name, or entrypoint. Do NOT add a"
            " `main` method. Add extra imports only if javac requires them,"
            " directly below the existing import.",
            "<skeleton>",
            pkg_line,
            "",
            "import com.code_intelligence.jazzer.api.FuzzedDataProvider;",
            "",
            "public class FuzzHarness {",
            "    public static void fuzzerTestOneInput("
            "com.code_intelligence.jazzer.api.FuzzedDataProvider data) {",
            "        // >>> YOUR CODE HERE <<<",
            "    }",
            "}",
            "</skeleton>",
        ])