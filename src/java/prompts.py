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
from java_source import highlight_trigger_calls



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
              variant_strategy: Optional[str] = None,
              verifier_enabled: bool = False,
              mined_oracles: Optional[List] = None,
              synthesized_relations: Optional[List] = None,
              oracle_mechanism: Optional[str] = None,
              touched_javadocs: Optional[List[str]] = None,
              class_context: Optional[List[str]] = None,
              ) -> List[Dict[str, str]]:
        """Assemble the chat-completion messages.

        ``touched_javadocs`` are the javadoc texts of the touched methods
        (extracted once by the caller from the buggy sources). Only their
        @param/@throws lines are injected — the documented preconditions /
        rejection contract the valid-by-construction rule tells the model
        to satisfy, which it previously had to GUESS from test source.

        ``oracle_mechanism`` (semantic bugs) assigns THIS harness one extra
        oracle mechanism instead of stacking all of them into every prompt:
        'pairs' injects the mined sibling-test pairs, 'relations' injects the
        screened synthesized relations, 'consistency' injects neither (the
        variant-strategy consistency guidance carries the load). The lifted
        trigger-test block — the foundation every semantic TP so far came
        through — is always present. Rationale: stacking every block dilutes
        attention (a TP regressed to an FN under 36 injected mined
        assertions) and blocks contradicted each other; the campaign already
        rotates strategies across the harness SET, so rotating mechanisms
        the same way keeps every mechanism tried without any single prompt
        carrying all of them. None (the default) preserves the old
        stack-everything behaviour for A/B comparison.

        ``verifier_enabled`` says a post-hoc soundness reviewer (the
        relation verifier) will screen every fired oracle before it can
        count as a finding. When True, the guardrail wording shifts from
        "when in doubt, don't assert" to "prefer the strongest assertion
        you can justify" — the FP risk the cautious wording defends
        against is now handled downstream, and the cautious wording
        demonstrably causes vacuous assertions (e.g. asserting only that
        a mean is finite, which a wrong-but-finite value passes).

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
                variant_strategy=variant_strategy,
                verifier_enabled=verifier_enabled,
                mined_oracles=mined_oracles or [],
                synthesized_relations=synthesized_relations or [],
                oracle_mechanism=oracle_mechanism,
                touched_javadocs=touched_javadocs,
                class_context=class_context,
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
        precond = self._preconditions_block(touched_javadocs)
        if precond:
            sections.append(precond)
        if context.root_cause_reachable:
            sections.append(self._variant_analysis_block(
                context.root_cause_reachable,
                covered_functions or [],
                found_signatures or [],
                strategy=variant_strategy,
                verifier_enabled=verifier_enabled,
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
                        variant_strategy: Optional[str] = None,
                        verifier_enabled: bool = False,
                        mined_oracles: Optional[List] = None,
                        synthesized_relations: Optional[List] = None,
                        oracle_mechanism: Optional[str] = None,
                        touched_javadocs: Optional[List[str]] = None,
                        class_context: Optional[List[str]] = None,
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
        # The 'consistency' mechanism slot is the one harness per set that
        # gets the class skeleton: its oracle is a cross-member agreement
        # check, and the ctxstudy showed exactly that relation class
        # (sibling agreement, overload equivalence, round-trip) only
        # appears when the model can see the class-level contracts. The
        # other mechanism slots stay lean — every prompt paying the
        # skeleton cost is the bloat that once distracted the generator.
        if oracle_mechanism == 'consistency' and class_context:
            sections.append(self._class_context_block(class_context))
        precond = self._preconditions_block(touched_javadocs)
        if precond:
            sections.append(precond)
        sections.append(self._lifted_assertion_block(
            chosen, all_failure_tests,
            verifier_enabled=verifier_enabled))
        # Mechanism gating: only the assigned extra-oracle block is
        # injected (None = stack everything, the pre-rotation behaviour).
        # The lifted block above is unconditional — it is the foundation.
        if oracle_mechanism in (None, 'pairs'):
            mined_block = self._mined_oracle_block(mined_oracles or [],
                                                   chosen)
            if mined_block:
                sections.append(mined_block)
        if oracle_mechanism in (None, 'relations'):
            rel_block = self._synthesized_relations_block(
                synthesized_relations or [])
            if rel_block:
                sections.append(rel_block)
        if context.root_cause_reachable:
            sections.append(self._variant_analysis_block(
                context.root_cause_reachable,
                covered_functions or [],
                found_signatures or [],
                strategy=variant_strategy,
                verifier_enabled=verifier_enabled,
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
                                all_failure_tests: List[FailureTest],
                                verifier_enabled: bool = False) -> str:
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
            "1. LIFT EVERY ASSERTION (not just one): the failing test almost"
            " always checks SEVERAL input/output pairs (e.g. many assertEquals"
            " lines). Each pair is a SEPARATE trusted oracle — the correct"
            " code is known to produce that exact expected value for that"
            " exact input. Find ALL of them (assertEquals / assertTrue /"
            " assertSame / assertNull ...), and for each identify (a) the call"
            " it makes on the real API and (b) the EXPECTED value it checks"
            " against. Copy both VERBATIM — do not infer or recompute; use the"
            " literals the test hard-codes.",
            "",
            "2. RECONSTRUCT: in fuzzerTestOneInput, make EACH of those calls"
            " through the real public API, exactly as the test does, to get"
            " each ACTUAL value. EXACTLY-AS-THE-TEST-DOES includes the"
            " test's SETUP: if the expected value depends on environment"
            " the test arranges (a registered source file, a Locale or"
            " TimeZone, a formatting mode, a fixture object), replicate"
            " that setup faithfully — and if you cannot replicate it,"
            " DROP that pair rather than assert it, because a"
            " half-reconstructed scenario fires on correct code too (that"
            " exact mistake caused two false accusations of correct"
            " patches).",
            "",
            "3. ASSERT ALL OF THEM: if ANY actual value does not equal its"
            " expected value, `throw new com.code_intelligence.jazzer.api."
            "FuzzerSecurityIssueLow(\"[oracle:<short-id>] semantic mismatch:"
            " <what differed>\")`. Every DISTINCT check gets its own"
            " [oracle:<short-id>] prefix (e.g. [oracle:lifted-pairs],"
            " [oracle:mean-formula]) — alarms without an ID are rejected"
            " mechanically; the ID is how the pipeline tells which check"
            " fired."
            " Check every lifted pair — an overfitting patch that fixed only"
            " the one reported input still fails the others, so the more pairs"
            " you assert the more likely it is caught. Jazzer reports the throw"
            " as a finding, exactly like a crash, so scoring is unchanged. If"
            " all match, continue."
            " WHEN THE EXPECTED VALUE IS A STRING OF CODE OR FORMATTED TEXT"
            " (compiler/printer output, rendered source, messages with"
            " layout): never compare raw strings — normalize BOTH sides"
            " first (collapse all whitespace runs, e.g."
            " s.replaceAll(\"\\\\s+\", \"\")) so the check fires only on a"
            " CONTENT difference. A correct implementation may legitimately"
            " differ in spaces, newlines or statement-separator placement,"
            " and a raw-string firing over formatting will be dismissed in"
            " review — burying the real content difference the same"
            " comparison would have caught.",
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
        if verifier_enabled:
            # The relation verifier screens every fired oracle post hoc,
            # which shifts (but does not remove) the cost of an unsound
            # assertion: the reviewer is itself imperfect — it has both
            # passed unsound oracles and killed a sound one — so the prompt
            # must NOT promise that wrong assertions are free. What the
            # reviewer does buy is licence to stop hedging: without this
            # paragraph the guardrail above makes the model so FP-averse it
            # asserts only trivia — observed concretely on a distribution
            # bug where it checked a mean was *finite* (−49.76 passes)
            # instead of cross-checking it against the sampled average.
            parts.append(
                "  CALIBRATION: your assertions will be screened by an"
                " automated soundness reviewer before any firing counts, so"
                " do NOT hedge to trivially-weak checks to be safe. Write"
                " the strongest check you can JUSTIFY from the documented"
                " contract, and state that justification in a comment next"
                " to the check (the reviewer reads it). The reviewer is a"
                " safety net, not a licence to guess — it is imperfect, so"
                " an assertion still needs a real justification — but a"
                " vacuous assertion (isFinite / not-null / no-throw) can"
                " never catch a wrong value and guarantees a miss, which"
                " nothing downstream can recover."
            )
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
                parts.extend(self._test_context_blocks(chosen))
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
        sibling_block = self._field_siblings_block(fn)
        if sibling_block:
            parts.append(sibling_block)
        return '\n'.join(parts)

    @staticmethod
    def _field_siblings_block(fn: TouchedFunction) -> str:
        """Render the members coupled to this function through a SHARED
        FIELD (instance or static) with no call edge
        (analysis.FieldSibling). The
        call-graph context above shows what the function calls; this shows
        where the STATE it reads/writes is established and reported —
        the divergence surface a masked-symptom defect stays visible on."""
        siblings = getattr(fn, 'field_siblings', None)
        if not siblings:
            return ''
        parts: List[str] = [
            f"STATE COUPLING: these members of the same class share the"
            f" listed field(s) with `{fn.func_name}` but neither"
            " calls the other — state one of them writes is what the other"
            " reports. A defect the patch leaves (or introduces) in that"
            " shared state stays observable through these members even when"
            f" `{fn.func_name}`'s own output looks right, so a strong"
            " harness CHECKS THEY AGREE: compare what a reader reports"
            " against what the writer/constructor established.",
        ]
        for s in siblings:
            parts.append(f"  - `{s.signature}` "
                         f"(shared field(s): {', '.join(s.shared_fields)})")
            if s.javadoc:
                parts.append(f"      doc: {s.javadoc}")
            if s.is_constructor and s.source:
                parts.extend([
                    "    <constructor_body>", s.source,
                    "    </constructor_body>",
                ])
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

    @staticmethod
    def _class_context_block(class_context: List[str]) -> str:
        """Class-level skeletons for the consistency-mechanism harness —
        the contracts a cross-member agreement oracle needs (sibling
        accessors, constructor invariants, class javadoc). Same view the
        synthesizer and verifier get; label-free (buggy checkout only)."""
        return (
            "CLASS-LEVEL CONTEXT — partial skeletons of the touched"
            " class(es): signatures, fields, javadoc; non-touched method"
            " bodies elided. Use them to find CROSS-MEMBER consistency"
            " oracles: sibling accessors that must agree with each other or"
            " with constructor arguments, overloads that must be"
            " equivalent, round-trips that must restore state. Assert only"
            " what a shown contract or invariant guarantees.\n"
            "<class_skeletons>\n"
            + '\n\n'.join(class_context)
            + "\n</class_skeletons>")

    @staticmethod
    def _preconditions_block(javadocs: Optional[List[str]],
                             max_chars: int = 900) -> str:
        """Compact documented-preconditions / rejection-contract block: the
        @param/@throws lines from the touched methods' javadoc.

        The valid-by-construction rule tells the generator to 'build inputs
        satisfying the documented preconditions' — but the generator was
        never FED that documentation (only test source and bodies), so it
        had to guess the preconditions. This block closes the gap. It
        AUGMENTS the rule; when javadoc is absent (undocumented code,
        OSS-Fuzz targets) it renders empty and the rule stands alone —
        never replaced."""
        if not javadocs:
            return ''
        lines: List[str] = []
        for doc in javadocs:
            for ln in (doc or '').splitlines():
                s = ln.strip()
                if (s.startswith('@param') or s.startswith('@throws')
                        or s.startswith('@exception')):
                    if s not in lines:
                        lines.append(s)
        if not lines:
            return ''
        parts = [
            "DOCUMENTED PRECONDITIONS / REJECTION CONTRACT of the touched"
            " method(s), from their javadoc. An input violating an @param"
            " constraint is INVALID — a documented @throws on it is CORRECT"
            " behaviour, never a finding. Build inputs that satisfy these"
            " constraints BY CONSTRUCTION (order/clamp/force them valid"
            " before the call) and assert only on those:",
        ]
        total = 0
        for s in lines:
            if total + len(s) > max_chars:
                parts.append("  - ... (further documented constraints"
                             " elided)")
                break
            parts.append(f"  - {s}")
            total += len(s)
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
                "is either (i) a validation/rejection exception "
                "(IllegalArgumentException or a subclass, "
                "NumberFormatException, a library 'invalid input' exception, "
                "...) OR (ii) a GENERIC JDK runtime exception that a method "
                "commonly leaks on malformed / out-of-domain input "
                "(StringIndexOutOfBoundsException, IndexOutOfBoundsException, "
                "ArrayIndexOutOfBoundsException, NullPointerException, "
                "ClassCastException, ArithmeticException), then its signature "
                "CANNOT distinguish the bug from "
                "correct rejection — a correctly fixed version legitimately "
                "throws the same exception, possibly at the same line, when "
                "the input really is invalid. This is the #1 false-positive "
                "source: the SAME exception class leaks from the SAME method "
                "on OTHER malformed inputs that even the correct fix does not "
                "handle (e.g. a lone type-suffix like \"L\" makes "
                "createNumber throw StringIndexOutOfBounds on buggy AND fixed "
                "alike) — so matching the ground-truth class and location is "
                "NOT enough. In that case, let it propagate "
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
                hint, lines = highlight_trigger_calls(
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
                parts.extend(self._test_context_blocks(ft))
            else:
                parts.append(
                    f'<failing_test class="{ft.test_class}"'
                    f' method="{ft.test_method}" />'
                )
        return '\n'.join(parts)

    @staticmethod
    def _test_context_blocks(ft: FailureTest) -> List[str]:
        """H1/H2 companion blocks for a rendered <failing_test>: the REAL
        failure message the test produced on the buggy build (names the
        diverging observable and the wrong value), and the test-class
        support the method depends on (setUp/helpers/constants/fixtures)
        so the harness replicates the scenario instead of improvising the
        setup — the root of every setup-divergence false alarm."""
        parts: List[str] = []
        if getattr(ft, 'failure_message', None):
            parts.extend([
                "On the BUGGY build this exact test FAILS with:",
                "<real_failure_message>",
                ft.failure_message,
                "</real_failure_message>",
                "This is ground truth: it names the observable that"
                " diverges and the wrong value the buggy build produces."
                " A faithful copy of this test MUST observe exactly this"
                " wrong value on the buggy build — if your check observes"
                " a different value, your setup diverges from the test's"
                " and the harness will be rejected.",
            ])
        if getattr(ft, 'support_source', None):
            parts.extend([
                "The test method depends on the following from its test"
                " class — setUp, helper methods, constants, fixture"
                " files. Replicate this environment EXACTLY (register the"
                " same files, wire the same providers, use the same"
                " constants); do NOT improvise setup the test performs"
                " differently, and DROP any check whose setup you cannot"
                " replicate:",
                f'<test_support class="{ft.test_class}">',
                ft.support_source,
                "</test_support>",
            ])
        return parts

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

    def _mined_oracle_block(self, mined: List, chosen) -> str:
        """Inject SIBLING test methods from the bug's own test class as extra
        trusted oracles to lift input->expected pairs from.

        `_lifted_assertion_block` already lifts the chosen trigger test. This
        widens the net to OTHER test methods that exercise the same patched
        class/method with DIFFERENT inputs. Except for the reported failure,
        the buggy code already passes these, so a correct patch must too —
        and an overfit patch that special-cases only the reported input
        fails one of these siblings. Sound by construction: same provenance
        as the lifted seed, no invention. We hand over whole method bodies
        (not individual assert lines) because a test commonly stores the call
        result in a local and asserts on it across several lines — the model
        lifts the pairs, exactly as it does for the trigger test."""
        if not mined:
            return ''
        lines = [
            "ADDITIONAL TRUSTED ORACLES — sibling tests from THIS bug's own"
            " test class that exercise the patched code with DIFFERENT inputs"
            " than the reported one. Except for the single reported failure,"
            " the buggy code already passes these, so a CORRECT patch must"
            " too; an overfitting patch that special-cases only the reported"
            " input still returns the wrong value for one of them.",
            "From each method below, LIFT the input->expected pairs (the call"
            " it makes on the real API and the value it asserts), reconstruct"
            " the call in your harness, and throw"
            " FuzzerSecurityIssueLow(\"[oracle:sibling-pairs] semantic"
            " mismatch: <which>\") when the"
            " patched code disagrees. For lifted pairs, both the input AND"
            " its expected value must be copied VERBATIM from the test"
            " source. Do NOT invent a NEW input->expected-value pair (e.g."
            " Integer.MIN_VALUE, empty, huge values) and guess its answer —"
            " an expected value you compute yourself fires on CORRECT"
            " patches too and is the top false-positive source. (This"
            " restriction is about value-equality pairs only: the"
            " consistency, metamorphic, and screened-relation checks that"
            " other sections of this prompt ask for are DIFFERENT oracle"
            " kinds — they need no guessed expected value — and remain"
            " required alongside these pairs.) If a pair needs a helper/type"
            " you cannot reconstruct, skip just that pair.",
            "<mined_sibling_tests>",
        ]
        for t in mined:
            lines.append(f"// {t.name} ({t.num_asserts} assertions)")
            lines.append(t.source)
        lines.append("</mined_sibling_tests>")
        return '\n'.join(lines)

    def _synthesized_relations_block(self, relations: List) -> str:
        """Inject mechanically screened synthesized relations as candidate
        oracles to implement.

        Mined oracles cover only inputs the developers tested; an overfit can
        pass every existing test yet stay wrong on an untested input whose
        oracle exists in no test. These relations generalise to a whole input
        family. The caller passes only relations that SURVIVED the buggy-build
        screen (relation_screen): each was compiled and exercised over many
        fuzzed inputs on the buggy checkout, and candidates that fired
        indiscriminately (out-of-domain — they would flag almost any
        implementation) were dropped. That screen establishes consistency
        with the buggy build's mostly-correct behaviour — NOT soundness
        against a known-good reference (no such reference exists without
        cheating) — so the wording below presents them as strong candidates
        with a stated justification, never as validated ground truth. The
        model implements each check against the real API, fencing to
        valid-by-construction inputs."""
        if not relations:
            return ''
        lines = [
            "SYNTHESIZED RELATION CANDIDATES (mechanically pre-screened) —"
            " invariants/metamorphic relations derived from the documented"
            " contract of the patched code. Each was compiled and exercised"
            " over many fuzzed inputs on the current build; candidates that"
            " fired indiscriminately were dropped as out-of-domain. They are"
            " STRONG CANDIDATES, not verified ground truth: implement each"
            " one, keep its justification comment next to the check, and"
            " fence its inputs exactly as instructed below. These generalise"
            " BEYOND the tested inputs, so they can catch an overfit that"
            " only special-cased the reported input. Implement each one in"
            " your harness in addition to the lifted/mined oracles — but"
            " these are EXTRAS, not your whole job: KEEP INVENTING YOUR OWN"
            " checks beyond them. Two self-invented shapes have repeatedly"
            " been the ones that catch what nothing else does:"
            " (a) HIDDEN-STATE checks — after any call documented as a"
            " question (get*/is*/contains/indexOf/size, no mention of"
            " modifying), re-read the object's cheap observable properties"
            " (capacity, size, length, later lookups) and assert they did"
            " not change; a 'read-only' call that silently mutates internal"
            " state is a classic defect no provided relation will mention."
            " (b) SIBLING-AGREEMENT checks — two methods documented to do"
            " the same job (overloads taking char vs String, Class vs class"
            " name, etc.) must agree on the same logical input:",
        ]
        for i, r in enumerate(relations, 1):
            name = getattr(r, 'name', f'relation{i}')
            kind = getattr(r, 'kind', '')
            contract = getattr(r, 'contract', '')
            check = getattr(r, 'check', '')
            input_spec = getattr(r, 'input_spec', '')
            screen_note = getattr(r, 'screen_note', '')
            lines.append(f"<relation name=\"{name}\" kind=\"{kind}\">")
            if contract:
                lines.append(f"  // holds because: {contract}")
            if input_spec:
                lines.append(f"  // valid input: {input_spec}")
            if screen_note:
                lines.append(f"  // screen: {screen_note}")
            lines.append(check)
            lines.append("</relation>")
        lines.append(
            "Build each relation's input with the FuzzedDataProvider so it is"
            " VALID BY CONSTRUCTION, wrap the API calls in try/catch and SKIP"
            " (return) on any caught exception — an exception is a rejection,"
            " never a violation. Throw"
            " com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("
            "\"relation <name> violated: ...\") only on a genuine"
            " disagreement, and include the observed values in the message."
            " Two structural rules (checked mechanically, violations are"
            " rejected): (1) the violation throw goes OUTSIDE/AFTER the"
            " try/catch that wraps the API calls — thrown inside your own"
            " catch-everything block it is caught and discarded, dead code;"
            " (2) if you ever convert a CAUGHT exception into your own alarm,"
            " pass it as the cause: new FuzzerSecurityIssueLow(\"...\", e) —"
            " never bury it in the message text.")
        return '\n'.join(lines)

    def _variant_analysis_block(self,
                                reachable: List[str],
                                covered: List[str],
                                signatures: List[str],
                                strategy: Optional[str] = None,
                                verifier_enabled: bool = False) -> str:
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
        if signatures:
            # Masked-symptom pressure. Every signature the set has found so
            # far fired via SOME check — and a band-aid patch, by
            # definition, silences exactly the checks that fired on the
            # reported symptom. A harness whose ONLY oracle is one of those
            # already-found triggers goes silent on such a patch and the
            # overfit is missed. So once the set has any trigger, every new
            # harness must carry at least one oracle that does not depend
            # on the known symptom re-appearing.
            parts.append(
                "INDEPENDENT ORACLE REQUIRED: the set has already found"
                " trigger(s): " + '; '.join(signatures[:5]) + ". A band-aid"
                " patch silences exactly these known symptoms, so a harness"
                " that only re-checks them cannot tell a real fix from a"
                " cover-up. Include at least ONE additional check that"
                " would still fire if the known symptom disappeared but a"
                " related quantity stayed wrong (see the consistency checks"
                " below)."
            )
        parts.append(self._variant_strategy_menu(
            assigned=strategy, verifier_enabled=verifier_enabled))
        parts.append(self._consistency_hint_block())
        return '\n'.join(parts)

    # NOTE: an earlier version prefixed this block with a name-matched
    # whitelist (_STAT_PATTERNS: get*Mean, size, hashCode, ...) that told
    # the model WHICH reachable sibling to cross-check and HOW. Retired:
    # the patterns were shaped by one dataset's vocabulary (the exact
    # overfitting this pipeline hunts), and relation synthesis subsumes
    # them — it reproduces the mean/variance/bounds/monotonicity contracts
    # from the class's own javadoc, per codebase, with a mechanical screen
    # behind it. What remains below is the dataset-neutral schema.
    @staticmethod
    def _consistency_hint_block() -> str:
        parts = [
            "CONSISTENCY CHECKS (these directly support strategy (b) — a"
            " summary a defect can leave wrong while the top-level output is"
            " masked). Identify from the API IN FRONT OF YOU which exposed"
            " values can be cross-checked against an independent"
            " computation — a reported aggregate vs a recomputation from"
            " the object's own output, a value vs the object's own stated"
            " bounds — and for at least one, throw on a mismatch beyond"
            " tolerance. These are SOUND (a summary must match the data it"
            " summarises), so they will not fire on a correct"
            " implementation.",
        ]
        # The observed hedge: pointed at the right sibling, the model wrote
        # `if (!Double.isFinite(mean)) throw` — satisfied the letter of the
        # hint with the weakest member of the sound set, and a wrong-but-
        # finite value sailed through. Close that loophole explicitly, and
        # name the two comparisons that are always available so "strong"
        # is not left to the model's imagination.
        parts.append(
            "WHAT DOES NOT COUNT: asserting the reported value is finite,"
            " non-NaN, non-null, or that no exception was thrown is NOT a"
            " consistency check — every wrong-but-finite value passes those,"
            " so they can never catch this class of bug. A valid check"
            " COMPARES the reported value against a second, independently"
            " obtained quantity: the object's own stated bounds, or a"
            " recomputation from the object's own output.")
        # A SCHEMA, deliberately not a worked instance: a concrete example
        # (say, a mean cross-checked against samples) would hand the model
        # the answer whenever the bug under evaluation happens to match it
        # — overfitting to this dataset and proving nothing about the
        # method. Placeholders force the model to instantiate the pattern
        # from the API actually in front of it, which is the capability we
        # are trying to elicit and measure.
        parts.append('\n'.join([
            "SHAPE OF A VALID CHECK (a schema — instantiate every"
            " <placeholder> with real calls from the API above):",
            "",
            "    var reported = <the value the object claims>;",
            "    var independent = <the SAME quantity obtained a second,"
            " independent way>;",
            "    if (<reported disagrees with independent beyond a generous"
            " tolerance>)",
            "        throw new com.code_intelligence.jazzer.api."
            "FuzzerSecurityIssueLow(\"[oracle:<short-id>] consistency"
            " violation: \" + <both values>);",
            "",
            "Ways to obtain the independent value — each sound for ANY"
            " correct implementation of ANY library:",
            "  1. LIMITS THE OBJECT ITSELF STATES: a summary of data must"
            " lie within that data's own reported extremes (lower/upper,"
            " min/max, first/last, 0..capacity).",
            "  2. RECOMPUTATION FROM THE OBJECT'S OWN OUTPUT: aggregate"
            " many values/elements the same object produces and compare"
            " the aggregate to the reported summary.",
            "  3. A SECOND, IDENTICALLY-CONSTRUCTED OBJECT (or a fresh"
            " recomputation of a cached value): both must report the same"
            " thing.",
            "",
            "Use enough items and a generous tolerance (a few percent, or"
            " several standard errors) so noise on a CORRECT implementation"
            " never fires; a genuinely wrong value is typically off by far"
            " more than any sane tolerance.",
        ]))
        return '\n'.join(parts)

    def _variant_strategy_menu(self, assigned: Optional[str] = None,
                               verifier_enabled: bool = False) -> str:
        """A menu of variant strategies for the next harness.

        Reaching a different function is only ONE way the SAME root cause,
        left unfixed by a narrow (band-aid) patch, still manifests. This
        lists the situations we can justify — a neighbouring reachable
        function, a callee masked by a self-correcting downstream step, or
        inputs just past the seed the overfit special-cased — each of which
        stays WITHIN the root-cause region and stems from the SAME root
        cause, matching this block's definition of a valid sibling bug.
        (Deliberately omitted: parallel sister methods not in the call
        graph — they violate the in-region definition — and stateful call
        sequences, for which we have no evidence in this setting.) Examples
        are generic (not from any evaluated project) so the model learns
        the pattern, not a specific answer. Every option keeps the same
        guardrail: only assert what holds for ANY correct implementation,
        else a correct patch false-positives."""
        header = (
            "VARIANT STRATEGY — the patch may be a band-aid that fixed the"
            " reported instance while the SAME root cause still bites"
            " elsewhere in this region. Pick the ONE probe that best fits:")
        if assigned in ('a', 'b', 'c'):
            # The campaign rotates strategies across the harness SET so each
            # is tried by at least one harness (rather than the model always
            # picking the same one). This harness is ASSIGNED one.
            header = (
                "VARIANT STRATEGY — this harness is one of a set that divides"
                " the strategies below so each is tried. USE STRATEGY"
                f" ({assigned}) for THIS harness (fall back to another ONLY if"
                " it is clearly inapplicable to this patch). The patch may be"
                " a band-aid that fixed the reported instance while the SAME"
                " root cause still bites elsewhere in this region:")
        # With the post-hoc soundness reviewer active the model may stop
        # hedging to vacuous finiteness checks (observed failure mode) —
        # but the reviewer is imperfect (it has both passed unsound
        # oracles and killed a sound one), so the prompt must not promise
        # that a wrong assertion is free.
        guardrail = (
            "GUARDRAIL (all options): only assert a property that holds for"
            " EVERY correct implementation. If you cannot state one, exercise"
            " the path WITHOUT asserting — an assertion you can't justify"
            " false-positives on a correct patch."
            if not verifier_enabled else
            "GUARDRAIL (all options): prefer a property that holds for EVERY"
            " correct implementation, and state the documented guarantee"
            " behind each check in a comment (an automated soundness"
            " reviewer screens every fired assertion and reads it). Do NOT"
            " retreat to vacuous checks (finiteness, non-null, no-throw) to"
            " stay safe — a vacuous check guarantees a missed bug, which"
            " nothing downstream can recover; the reviewer backstops a"
            " justified-but-borderline check, though it is not infallible.")
        return '\n'.join([
            header,
            "",
            "  (a) DIFFERENT REACHABLE FUNCTION (coverage spread). Shape the"
            " input so a different function in the region above runs / a"
            " different failure signature appears. Good when the same"
            " root-cause pattern recurs across several functions and the patch"
            " only fixed one.",
            "",
            "  (b) CONSISTENCY CROSS-CHECK on a masked helper. A defect can"
            " leave the patched function's top-level output CORRECT because a"
            " downstream step (an iterative solve, a convergence loop, a clamp,"
            " a re-normalisation) absorbs the error — while a reachable HELPER"
            " that produces a RELATED quantity stays wrong (the unfixed root"
            " cause). Rather than guess that helper's exact value, compute the"
            " SAME quantity TWO independent ways and assert they AGREE — a"
            " check that is sound without knowing the answer. Generic"
            " patterns: a stated summary/aggregate must match the EMPIRICAL"
            " value recomputed over many items the same object produces; a"
            " reported size/count must match a manual count of the elements;"
            " a cached/precomputed value must equal a fresh recomputation;"
            " equal objects must have equal hashCodes. Use a tolerance for"
            " floating-point/statistical estimates and require enough items,"
            " so a CORRECT implementation never fires.",
            "",
            "  (c) FLIP THE PATCHED CONDITION (overfit on the seed). The patch"
            " changed a specific line/condition. An overfitting fix often"
            " special-cases the seed input and breaks just past it. Construct"
            " inputs that make the OLD and NEW behaviour DIFFER — values at and"
            " around the boundary the changed condition tests — and check the"
            " result is still correct there.",
            "",
            guardrail,
        ])

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
            "- FENCE DEGENERATE INPUTS: construct the inputs you assert on"
            " to be non-degenerate BY CONSTRUCTION — non-empty strings and"
            " collections, visible (non-blank) labels/names, at least one"
            " element where elements are iterated. Contracts are routinely"
            " silent about the empty/blank case (a correct implementation"
            " may legitimately do nothing for an empty label, return"
            " nothing for an empty collection), so a relation whose ONLY"
            " violations occur on degenerate inputs is testing unspecified"
            " behaviour and will be rejected in review. Assert on the"
            " degenerate case ONLY when the documented contract explicitly"
            " covers it — and cite that sentence.",
            "- FENCE EXTREME MAGNITUDES the same way: cap fuzzed numeric"
            " parameters to moderate ranges by construction (as a rule of"
            " thumb, |value| <= 1_000_000 for integers that get multiplied"
            " together or fed to combinatorial/statistical formulas) unless"
            " the documented contract explicitly covers larger values. At"
            " billion-scale parameters a CORRECT implementation's double"
            " arithmetic legitimately degrades — internal overflow to NaN,"
            " rounding beyond any tolerance, log-gamma saturation — so an"
            " assertion whose only violations occur at such magnitudes"
            " accuses correct code (this exact pattern produced repeated"
            " false accusations: a probability check that sees NaN only at"
            " N near 2^31, a validation exception from a parameter range"
            " that overflowed). The bug you hunt fires at REASONABLE"
            " magnitudes too whenever the contract really is broken.",
            "- CONDITIONAL SIDE EFFECTS ARE NOT UNCONDITIONAL. If the"
            " method performs an observable side effect (adds to a"
            " collection, increments a counter, populates a cache, sets a"
            " field) only INSIDE guards — nested `if`s on collaborator or"
            " field state, not just on the input — you may assert that side"
            " effect happened ONLY after your harness has established EVERY"
            " one of those guard conditions. A correct implementation"
            " legitimately does nothing when any guard is unmet (a null"
            " owner/collector/context, a disabled flag), so 'the effect"
            " always happens after the call' is unsound and fires on correct"
            " patches. Read the shown method body: satisfy each guard on the"
            " path to the effect, or do not assert the effect.",
            "- Use only real library calls for BOTH sides (no hand-rolled"
            " reference implementation).",
            "- On violation, `throw new RuntimeException(\"[oracle:"
            "<short-id>] metamorphic violation: <which relation>"
            " input=<...> lhs=<...> rhs=<...>\")` with the concrete values,"
            " so a reviewer can replay the disagreement. The"
            " [oracle:<short-id>] prefix is MANDATORY (checked"
            " mechanically) — an un-named alarm is invisible to the"
            " per-check acceptance machinery. Jazzer reports the throw as"
            " a finding, the same as a crash.",
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