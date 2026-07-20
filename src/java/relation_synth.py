"""Synthesize codebase-specific relations that must hold for any correct
implementation of the patched code — the general replacement for the old
hand-coded statistical-summary whitelist.

Mining (test_oracle_miner) recovers TRUSTED input->output pairs the
developers already wrote, but only for the inputs they happened to test. An
overfit patch can pass every existing test yet stay wrong on an input nobody
tested (e.g. a fraction reduced through a unit operand). This module asks an
LLM to propose METAMORPHIC relations and OUTPUT INVARIANTS — properties that
hold for ANY correct implementation across a whole family of inputs, not just
the tested ones — grounded in the patched source, the reachable API, and the
mined sibling tests (which show the real API usage).

A proposed relation is only a HYPOTHESIS; it is NOT trusted until screened
against the buggy build over the mined/passing inputs (relation_screen): a
relation the buggy code already violates on a passing input is unsound and
dropped. This module only PROPOSES; soundness is decided mechanically
downstream. Nothing here reads the developer fix or the dataset label.

Fails soft: any parse/LLM error yields an empty list, so synthesis can only
ADD candidate oracles, never break a run.
"""
from dataclasses import dataclass
from typing import List, Optional
import json
import re

from llm import HarnessGenerator

_SYSTEM = (
    "You are a software-verification expert. Given a patched Java method and"
    " its API, you propose RELATIONS that must hold for EVERY correct"
    " implementation — never merely for the reference or for the tested"
    " inputs. Two kinds only:\n"
    "  * METAMORPHIC: two real API calls whose results must agree (or relate"
    " in a fixed way) for any valid input — e.g. parse(format(x)) == x;"
    " f(a,b) == f(b,a) when documented commutative; reduce(reduce(x)) =="
    " reduce(x).\n"
    "  * INVARIANT: a property the output must satisfy for every valid input,"
    " read straight off the documented contract — e.g. a reduced fraction has"
    " gcd(numerator,denominator)==1; a returned index is within bounds.\n"
    "Each relation must be SOUND: if any correct implementation could violate"
    " it, do not propose it. Avoid comparisons to a different library, and"
    " avoid boundary-fragile claims (division by zero, NaN, overflow,"
    " empty/null) unless you explicitly exclude those inputs."
)

_INSTRUCTIONS = (
    "ANCHOR REQUIREMENT — READ FIRST. The patch changed a SPECIFIC"
    " expression in a SPECIFIC method. An overfit usually differs from a"
    " correct implementation WHERE the code was changed (the one exception:"
    " a method the FAILING TEST reads can stay wrong when the patch edited"
    " elsewhere — that is what the secondary list below is for). Your FIRST relation"
    " MUST directly constrain the OUTPUT of a changed method, exercising the"
    " exact input the change is about — the prefix / sign / boundary value /"
    " edge case named in the changed condition — and asserting only what that"
    " method's OWN documented contract guarantees there. (E.g. if the change"
    " adds a guard on inputs starting with a token, your relation must feed"
    " such inputs and assert the documented result; if the change rewrites a"
    " formula the javadoc states, assert the method's output equals that"
    " formula recomputed independently.) Do NOT fill the slots with generic"
    " round-trip or bound properties of untouched sibling methods.\n"
    "If the changed expression is a NUMERIC formula, a correct fix may"
    " reassociate it (a*(b/c) vs (a*b)/c) and differ by rounding, so"
    " assert APPROXIMATE equality with a GENEROUS relative tolerance —"
    " never exact ==, or you will falsely flag a correct reassociation."
    " Use `Math.abs(a-b) <= tol * Math.max(1.0, Math.max(Math.abs(a),"
    " Math.abs(b)))` with tol AT LEAST 1e-9; and when the computation"
    " multiplies or sums large integer/long intermediates (products near"
    " or above 2^31, or billion-scale inputs), use tol = 1e-6 — at those"
    " magnitudes the library's OWN double rounding exceeds 1e-9, so a"
    " tighter tolerance false-fires on a correct patch. A too-tight"
    " tolerance is as unsound as exact ==; the real bug you are hunting"
    " diverges by a LARGE amount (e.g. a sign flip or a value 100x off),"
    " not by rounding.\n"
    "STANDING STRATEGY — DOCUMENTED FORMULAS (checked first, before"
    " anything else): scan the javadoc of the touched class's numeric"
    " getters AND of every class shown with role=\"test-subject\" (the"
    " classes the failing test itself constructs and reads — the defect's"
    " observable surface often lives there, not in the patched class) for"
    " a stated closed-form formula ('the value equals a * b / c of the"
    " constructor parameters', 'returns x * (1 - x)', ...). If ANY such"
    " formula exists, your FIRST relation MUST be that"
    " formula: recompute it independently from the object's own parameters"
    " and compare with the generous magnitude-scaled tolerance above. A"
    " documented formula is the strongest relation class there is — it is"
    " deterministic, it holds for every correct implementation by"
    " definition, and a patch that leaves the value wrong anywhere in the"
    " domain cannot pass it. Only when no documented formula exists does"
    " the anchor requirement below decide your first relation.\n"
    "STANDING STRATEGY — DOCUMENTED @THROWS (checked second): scan the"
    " touched method's javadoc (and its parent declaration's, if shown)"
    " for declared exceptions — '@throws X if/when <condition>'. For EACH"
    " such declaration whose triggering input you can construct (a null"
    " argument, a malformed string, an out-of-range value), one relation"
    " MUST construct exactly that input and assert the documented throw:"
    " call the method, and if it COMPLETES NORMALLY (or throws a"
    " different, non-subclass exception), throw the violation."
    " MANDATORY check shape for these documented-throw relations —"
    " catching broadly and returning DEFEATS the relation (a patch"
    " whose reordered guard throws the WRONG exception class then"
    " silently skips instead of firing):\n"
    "    try { obj.method(rejectedInput); violated = true; /* completed"
    " normally */ }\n"
    "    catch (TheDocumentedException ok) { /* contract honoured */ }\n"
    "    catch (Throwable t) { violated = true; /* wrong exception"
    " class — also a contract violation */ }\n"
    "  then throw the violation OUTSIDE the try when `violated` is set,"
    " naming what actually happened (completed vs wrong class"
    " <type>). This is a"
    " tripwire: it stays silent on any build that honours the contract"
    " and fires on a patch whose added or reordered guard swallows the"
    " documented rejection — a classic overfit shape (a guard inserted"
    " BEFORE an argument check silently returns where the contract says"
    " throw). Use ONLY declared @throws with their stated conditions —"
    " never invent an undocumented rejection; a subclass of the declared"
    " exception counts as honouring the contract. IMPORTANT — vary the"
    " RECEIVER and the OTHER arguments across their special states while"
    " holding the rejected input fixed: an overfit often reorders the"
    " check behind a guard that only triggers for a special receiver"
    " state (a NaN/empty/zero receiver), so a plain rejected input alone"
    " misses it — assert the documented throw on an ORDINARY receiver"
    " AND on each special-state receiver the class admits (empty,"
    " extreme, identity, NaN), since a patch may return early only in"
    " the special-state case.\n"
    "Propose up to 6 relations. COVERAGE REQUIREMENT (R2): the set must"
    " cover DISTINCT documented observables — after the documented-formula"
    " and @throws slots above, spread the remaining slots across different"
    " contract sources (a formula, a throws clause, a state/read-only"
    " guarantee, a sibling-agreement rule) rather than proposing several"
    " variants of one observable. Do NOT anchor every relation on the"
    " patched method alone: a patch to one method can leave a DOCUMENTED"
    " observable of the test-subject class wrong away from the trigger,"
    " and a set of six relations about the patched method's own edge"
    " cases will all stay quiet while that documented observable"
    " diverges. At least ONE relation must assert a documented guarantee"
    " of a test-subject class when any is shown. And when the context"
    " lists SAME-NAME OVERLOADS or a METHOD FAMILY that contains the"
    " patched method, write one agreement relation PER SIBLING PAIR"
    " (patched method, sibling) for up to two siblings — prioritising"
    " the siblings that share the most behaviour with the patched"
    " method (same output shape, documented shared semantics), NOT just"
    " any family member: a patch that changes one member while a"
    " close sibling keeps the shared behaviour breaks exactly that"
    " pairwise agreement, and an agreement relation aimed at a distant"
    " family member can hold on a patch that a close-sibling relation"
    " convicts. Each pair relation: the two members given equivalent"
    " inputs must produce the documented consistent outcome — identical"
    " results up to the documented difference between them.\n"
    "DEEP-DIVE PROTOCOL — do this enumeration BEFORE proposing:"
    " (1) LIST every documented observable in scope — each stated"
    " formula, each declared @throws, each documented range/format,"
    " each documented family agreement, each read-only/state guarantee"
    " — from the patched class AND every test-subject class shown, each"
    " with the doc line that states it; (2) MARK which of these the"
    " patch text could plausibly affect, directly or through shared"
    " state; (3) SPEND your slots on the marked ones per the coverage"
    " requirement above. Relations aimed at observables no"
    " documentation states are last-resort fills, never first choices."
    " For each relation,"
    " give:\n"
    "  - name: a short slug\n"
    "  - kind: metamorphic | invariant\n"
    "  - contract: the ONE documented guarantee that makes it hold for every"
    " correct implementation (javadoc sentence or a visible code invariant)\n"
    "  - input: how to build a VALID input by construction (magnitudes,"
    " signs, shapes) that a correct implementation MUST accept — so a"
    " violation is a real defect, not a rejection of bad input.\n"
    "    From the changed condition in the patch, the documented ranges"
    " (@param/@throws), and any bounds/size accessors this class exposes,"
    " identify the input-domain BOUNDARIES this code defines, and construct"
    " each check's inputs to PROBE at and just past them — but assert only"
    " what the contract guarantees there. Probing a boundary is free;"
    " asserting an unspecified boundary value is the classic unsound"
    " oracle.\n"
    "  - check: a self-contained Java snippet that builds the input from an"
    " `int`/`long`/`String` chosen with the FuzzedDataProvider `data`,"
    " computes both sides (or the property) via the REAL public API, and"
    " `throw new RuntimeException(\"relation <name> violated: \"+...)` on"
    " disagreement. It MUST wrap the API calls in try/catch and RETURN"
    " (skip) on ANY caught exception — an exception is a rejection, never a"
    " violation.\n"
    "    STRUCTURE RULE (checked mechanically; violations are rejected):"
    " the try/catch goes ONLY around the API calls that build/compute the"
    " values. The comparison and its `throw new RuntimeException(...)"
    " violated ...` must sit AFTER/OUTSIDE that try block — a violation"
    " thrown inside your own catch-everything block is caught by it and"
    " silently discarded, making the whole check dead. Pattern:\n"
    "      T a, b;\n"
    "      try { a = api(...); b = api2(...); } catch (Exception e)"
    " { return; }\n"
    "      if (!close(a, b)) throw new RuntimeException(\"relation <name>"
    " violated: \" + a + \" vs \" + b);\n"
    "Draw inputs ONLY with the Jazzer FuzzedDataProvider API on `data`:"
    " data.consumeInt(), data.consumeInt(lo,hi), data.consumeLong(),"
    " data.consumeBoolean(), data.consumeAsciiString(n),"
    " data.consumeRemainingAsString() — there is NO drawInt/nextInt. The"
    " check body runs INSIDE fuzzerTestOneInput, so use the real library"
    " types directly (no wrapper class, no imports block).\n"
    "IMPLEMENTABILITY (checked mechanically; violations are dropped): the"
    " check may NOT define an anonymous or local SUBCLASS of a library"
    " class to reach a code path (e.g. `new AbstractIntegerDistribution("
    "...){...}`) — a hand-built stand-in is forbidden and the relation"
    " will be discarded. If the behaviour you want lives on an abstract"
    " base, reach it through a REAL concrete library subclass that already"
    " exists (e.g. use `new UniformIntegerDistribution(min, max)` to get an"
    " integer distribution over a chosen support, rather than subclassing"
    " the abstract base yourself). If no real subclass can construct the"
    " input you need, choose a different relation.\n"
    "Return ONLY a JSON array of objects with keys name, kind, contract,"
    " input, check. No prose outside the JSON."
)

def _unflatten_check(check: str) -> str:
    """Recover a check the model double-escaped in its JSON. Sometimes the
    whole snippet arrives as ONE physical line with literal `\\n`/`\\t`
    sequences instead of real newlines (stochastic model output); javac
    then dies on 'illegal character \\'. Only transform when there is NO
    real newline yet literal `\\n` markers are present — a well-formed
    multi-line check is left untouched, and a legitimate `"\\n"` string
    literal (which sits inside otherwise multi-line code) is never
    reached."""
    if '\n' not in check and '\\n' in check:
        return check.replace('\\n', '\n').replace('\\t', '\t')
    return check


def javadoc_for(source: str, method_name: str, max_chars: int = 1500) -> str:
    """The /** ... */ javadoc immediately preceding `method_name`'s
    declaration in `source`, or ''. The documented contract is the ONLY
    honest source of ground truth for inputs no test covers, so it is fed
    to synthesis verbatim rather than paraphrased. Pure regex — fails soft
    to '' on anything unusual."""
    if not source or not method_name:
        return ''
    pat = re.compile(
        r'/\*\*(.*?)\*/'                      # the javadoc body
        r'\s*(?:@\w+(?:\([^)]*\))?\s*)*'      # annotations between doc & decl
        r'(?:public|protected|private|static|final|abstract|synchronized'
        r'|native|\s)*[\w\[\]<>,.\s]+?'       # modifiers + return type
        r'\b' + re.escape(method_name) + r'\s*\(',
        re.DOTALL)
    m = pat.search(source)
    if not m:
        return ''
    doc = m.group(1)
    # Strip the leading ' * ' gutter for compactness.
    doc = '\n'.join(ln.strip().lstrip('*').strip()
                    for ln in doc.splitlines()).strip()
    return doc[:max_chars]


@dataclass
class Relation:
    name: str
    kind: str
    contract: str
    input_spec: str
    check: str
    # Filled by relation_screen for survivors: a one-line summary of what
    # the mechanical screen observed (execs, fire ratio). Rendered into the
    # prompt so the model (and the log reader) can see the evidence level.
    screen_note: str = ''
    # True when this relation came from the cross-leg pool rather than this
    # leg's own synthesis. Pooled relations are screening/replay material
    # only — they are never injected into the harness prompt (injected
    # sibling-leg relation mass displaced the generator's own checks in the
    # p23gate run: Lang-60-o lost its convicting capacity oracle).
    from_pool: bool = False


class RelationSynthesizer:
    """Proposes candidate relations (unscreened) for a semantic bug."""

    def __init__(self, generator: Optional[HarnessGenerator] = None):
        self._gen = generator or HarnessGenerator(temperature=0.3, top_p=1.0)
        # Full record of every LLM call this synthesizer makes (synthesis,
        # compile-repair, soundness-harden) — prompt messages + raw output —
        # so a run can dump a complete, auditable pipeline trace.
        self.llm_calls: List[dict] = []

    def _record_generate(self, messages, phase: str) -> str:
        """generate() + record the (phase, prompt, output) for the trace."""
        out = self._gen.generate(messages) or ""
        try:
            self.llm_calls.append(
                {'phase': phase, 'messages': messages, 'output': out})
        except Exception:
            pass
        return out

    def synthesize(self, patched_sources: List[str],
                   class_name: str,
                   reachable: List[str],
                   mined_tests: List,
                   trigger_summary: str = '',
                   patch_text: str = '',
                   javadocs: Optional[List[str]] = None,
                   class_context: Optional[List[str]] = None,
                   source_imports: Optional[List[str]] = None,
                   trigger_test_block: str = '',
                   trigger_methods: Optional[List[str]] = None,
                   max_rules: int = 4,
                   ) -> List[Relation]:
        """Propose candidate relations for the patched method(s).

        ``patch_text`` (the APR diff under analysis) and ``javadocs`` (the
        documented contracts of the touched methods) ground the proposals in
        the changed behaviour and its specification. Grounding proposals in
        the diff is safe ONLY because every candidate is mechanically
        screened downstream (relation_screen) before it can reach a prompt —
        un-screened diff-targeted oracles are the exact failure mode that
        produced false positives when tried as prompt exhortation.

        ``class_context`` (code_context.assemble_class_context) is the
        CLASS-LEVEL view: skeletons of the whole patched class, its
        supertypes, and key collaborators. Inspection of real tasks showed
        the strongest relation routinely lives OUTSIDE the patched method —
        a constructor-established invariant, a complementary sibling
        function, a class-javadoc contract — none of which is visible from
        the method body alone."""
        if not patched_sources:
            return []
        ctx = []
        # P2.1: the bug's own failing test is the ONE trusted source of the
        # correct DIRECTION (does createNumber("--1") return null or throw?).
        # It goes FIRST and is framed as authoritative — the patch may be
        # the overfit, so where the test pins a behaviour the test wins.
        # Withholding it (trigger_test_block was empty for the whole
        # project's history) is what let synthesis read the buggy body and
        # write relations backwards (Lang-7).
        if trigger_test_block:
            ctx += [
                "THE BUG'S OWN FAILING TEST (most trusted — this is ground"
                " truth for the correct behaviour on its inputs). The patch"
                " under analysis MAY be an overfit; where this test pins a"
                " direction, the TEST is right and the patched code is not"
                " evidence. Read the exact call it makes and the value it"
                " expects, and make your relations agree with it:",
                "<failing_test>", trigger_test_block, "</failing_test>",
            ]
        # NB: this is the BUGGY (pre-patch) source — extracted from the
        # buggy checkout. It is NOT the fixed behaviour; do not read a
        # correct direction off it (that inverted Lang-7's relation).
        ctx.append(
            "BUGGY method(s) (PRE-PATCH source — this is the code WITH the"
            " bug; the failing test above shows where it is wrong). Use it"
            " to see the code shape and API, NEVER as a model of correct"
            " behaviour:")
        for s in patched_sources:
            ctx += ["<code>", s, "</code>"]
        if class_context:
            ctx.append(
                "CLASS-LEVEL CONTEXT — the whole patched class (skeleton:"
                " contracts + signatures; full bodies only for patched"
                " methods), its supertypes, and key collaborators. The"
                " STRONGEST relations usually tie the patched method to"
                " something HERE rather than to itself: a sibling method"
                " documented to agree or complement it (two accessors that"
                " must round-trip; a pair documented to sum to a constant),"
                " a constructor-established field invariant the method must"
                " respect, or a class/method javadoc guarantee (never null,"
                " always positive, canonical order). Prefer such"
                " cross-member relations over properties of the patched"
                " method in isolation.")
            ctx.extend(class_context)
        if patch_text:
            ctx += [
                "The patch under analysis (propose relations that the"
                " CHANGED expressions/conditions could violate — the"
                " boundary values the changed condition tests are where an"
                " overfit breaks):",
                "<patch>", patch_text, "</patch>",
            ]
            # Distil the added/removed code lines so the model can't miss what
            # actually changed. The discriminating oracle always lives at one
            # of these expressions; surfacing them defeats the observed drift
            # to generic properties of untouched methods.
            # Keep the +/- markers: which lines the patch ADDED vs REMOVED
            # is exactly the direction signal. Stripping them (the old
            # behaviour) made added and deleted code indistinguishable, so
            # the model could not tell what the patch was trying to do.
            changed = []
            for ln in patch_text.splitlines():
                if ln[:1] in '+-' and not ln.startswith(('+++', '---')):
                    body = ln[1:].strip()
                    if body and not body.startswith(('*', '//', '/*')):
                        tag = 'ADDED  ' if ln[0] == '+' else 'REMOVED'
                        changed.append(f"{tag}: {body}")
            changed = list(dict.fromkeys(changed))[:20]
            if changed:
                ctx.append(
                    "THE EXACT LINES THE PATCH ADDED/REMOVED (your first"
                    " relation must target the behaviour THESE govern — the"
                    " condition, boundary token, or formula here is where an"
                    " overfit and a correct fix diverge; ADDED lines are what"
                    " the patch introduced, REMOVED what it deleted — the"
                    " overfit may have changed these WRONGLY):\n"
                    + "\n".join("    " + c for c in changed))
        for jd in (javadocs or []):
            if jd:
                ctx += ["Documented contract of a touched method (relations"
                        " must follow from contracts like this):",
                        "<javadoc>", jd, "</javadoc>"]
        if source_imports:
            # The class's own import list pins every referenced type to its
            # TRUE package IN THIS CODEBASE VERSION. Package layouts move
            # between library versions (measured: a whole candidate set died
            # at the screen's compile because the model placed a type in the
            # package it occupies in a more famous version of the library).
            ctx.append(
                "IMPORTS OF THE PATCHED CLASS — the authoritative package"
                " for every non-JDK type in this codebase VERSION. When a"
                " check references a type, use the package EXACTLY as"
                " imported here (or as shown in the skeletons); NEVER guess"
                " a package name from library conventions — this version's"
                " layout may differ, and a wrong package means the relation"
                " cannot compile and is discarded unread:\n"
                + "\n".join(source_imports[:60]))
        if reachable:
            ctx.append("Reachable API (call these, do not reimplement): "
                       + ", ".join(reachable[:30]))
        if trigger_methods:
            # P3.2b, demoted to ADVISORY after p23gate: as a mandate this
            # block re-aimed synthesis at low-level internals and lost the
            # winning contract-level relation (Closure-33-o regressed from
            # catch to miss). The documented contract of the touched code
            # stays the PRIMARY anchor; these names are secondary targets.
            ctx.append(
                "ALSO WORTH CONSTRAINING (secondary): methods/types the"
                " failing test exercises — the bug's symptom is observed"
                " through these, so when the patch edited a DIFFERENT"
                " method than the one whose output is wrong, one relation"
                " constraining one of these can catch it. This is an"
                " addition to, never a replacement for, relations grounded"
                " in the documented contract of the touched code — prefer"
                " the documented-contract relation when choosing:\n    "
                + ", ".join(trigger_methods))
        if mined_tests:
            ctx.append("Real API usage from the project's own tests (mirror"
                       " these call shapes; they are trusted):")
            for t in mined_tests[:3]:
                ctx += ["<test>", getattr(t, 'source', str(t)), "</test>"]
        if trigger_summary:
            ctx.append("The reported failure on the buggy code: "
                       + trigger_summary)
        instr = _INSTRUCTIONS
        if max_rules != 4:
            # Only the candidate COUNT changes — the strong-shape guidance is
            # untouched, so a 4-vs-N comparison isolates whether more draws
            # raise the odds the discriminating relation appears (vs generation
            # variance) rather than confounding it with a prompt reword.
            instr = instr.replace("Propose up to 4 relations",
                                  f"Propose up to {max_rules} relations", 1)
        user = "\n".join(ctx) + "\n\n" + instr
        messages = [
            {'role': 'system', 'content': _SYSTEM},
            {'role': 'user', 'content': user},
        ]
        # Stash the exact prompt so callers can dump a full, inspectable trace
        # (what context the model saw + the instructions it was given).
        self.last_prompt = {'system': _SYSTEM, 'context': "\n".join(ctx),
                            'instructions': instr}
        # One retry on an unparseable/empty response: a JSON-shaped ask can
        # still come back wrapped in prose, and silently returning zero
        # candidates hides the failure from the whole run (observed: a leg
        # "tested" synthesis that never proposed anything). The retry
        # re-states the format constraint; a second failure is logged by
        # the caller via the empty return.
        for attempt in range(2):
            try:
                out = self._record_generate(messages, 'synthesis') or ""
            except Exception:
                return []
            rels = self._parse(out)
            if rels:
                return rels
            messages = messages + [
                {'role': 'assistant', 'content': out[:2000]},
                {'role': 'user', 'content':
                    "That response could not be parsed. Return ONLY the JSON"
                    " array of relation objects (keys: name, kind, contract,"
                    " input, check) — no prose, no fences."},
            ]
        return []

    def repair_check(self, rel: 'Relation', javac_error: str,
                     imports: Optional[List[str]] = None) -> Optional[str]:
        """R1: given a candidate relation whose Java `check` failed to
        compile, ask the model ONCE for a corrected check body. Returns the
        corrected snippet or None. ~22% of candidates die at compile with no
        second chance today; this recovers the ones that are a fixable typo
        (wrong import, undeclared type, bad method name) rather than a
        genuinely unimplementable idea."""
        try:
            avail = "\n".join((imports or [])[:40])
            msg = [
                {'role': 'system', 'content':
                 "You fix a Java snippet that failed to compile. Return ONLY "
                 "the corrected snippet (the body that runs inside "
                 "fuzzerTestOneInput, drawing inputs from `data`), no prose, "
                 "no fences. Keep the SAME property being checked; change only "
                 "what is needed to compile. Throw the violation with "
                 "`throw new RuntimeException(\"relation <name> violated: \"+"
                 "...)` OUTSIDE any catch. Use only real API on the classpath."},
                {'role': 'user', 'content':
                 f"Property (keep it): {getattr(rel,'contract','')}\n"
                 f"Available imports:\n{avail}\n\n"
                 f"Snippet that failed:\n{getattr(rel,'check','')}\n\n"
                 f"javac error:\n{javac_error[:600]}\n\nCorrected snippet:"},
            ]
            out = self._record_generate(msg, 'compile_repair') or ""
            # strip fences if the model added them despite instructions
            out = re.sub(r'^```[a-z]*\n?|```$', '', out.strip(), flags=re.M)
            return out.strip() or None
        except Exception:
            return None

    def harden_for_soundness(self, rel: 'Relation', extremes_text: str,
                             n_fired: int, n_ordinary: int = 0,
                             imports: Optional[List[str]] = None
                             ) -> Optional[str]:
        """Soundness repair via deep domain reasoning. The check fired on
        `n_fired` extreme inputs AND `n_ordinary` benign/ordinary inputs during
        soundness testing. The model must think through WHY the input caused
        firing and, crucially, WHETHER that input is one the method is
        contractually required to handle (in-domain) or garbage it never
        promised anything about (out-of-domain), then either KEEP it or fix it.
        Firing on ORDINARY inputs is strong evidence of unsoundness — those are
        valid values a correct implementation must handle. Reasons ONLY from the
        documented contract (we have no correct implementation to run). Returns
        the corrected snippet, or None to KEEP the original unchanged."""
        try:
            avail = "\n".join((imports or [])[:40])
            ordinary_note = (
                f" It ALSO fired on {n_ordinary} ORDINARY, benign inputs "
                f"(small finite numbers, simple non-empty strings) — those are "
                f"plainly in-domain values a correct implementation MUST handle, "
                f"so firing there is very strong evidence the rule is unsound."
                if n_ordinary > 0 else
                " It stayed quiet on ordinary benign inputs, so if it is unsound "
                "it is only at an extreme.")
            msg = [
                {'role': 'system', 'content':
                 "You are a SKEPTICAL reviewer whose job is to PROVE a Java "
                 "relation check is UNSOUND. The check asserts a property that "
                 "must hold for EVERY correct implementation and throws a "
                 "'...violated...' RuntimeException otherwise. It fired during "
                 "soundness testing, and you must think DEEPLY before deciding "
                 "its fate. Do NOT rush to KEEP and do NOT rush to rewrite — "
                 "reason it out in these explicit steps:\n"
                 "STEP 1 — WHICH INPUT fired it? Read the check line by line and "
                 "identify the specific input value(s) that make it throw "
                 "'violated' (a NaN, an Inf+(-Inf), a negative index, an empty "
                 "string, Integer.MIN_VALUE, ...). Name the concrete triggering "
                 "input.\n"
                 "STEP 2 — IS THAT INPUT ONE THE METHOD MUST HANDLE? Consult the "
                 "documented contract (@param ranges, @throws, prose). Decide "
                 "which case this is:\n"
                 "  (a) IN-DOMAIN — the contract requires the method to accept "
                 "this input and return a defined result. Then ask: would a "
                 "CORRECT implementation ALSO produce a result that trips this "
                 "check on that input? If yes, the check is UNSOUND (it demands "
                 "more than the contract guarantees) — you MUST fix it.\n"
                 "  (b) OUT-OF-DOMAIN — the contract says the input is illegal / "
                 "unspecified / the method may throw or do anything. Then the "
                 "firing is meaningless and the check must simply NOT run on it: "
                 "fix it by GUARDING (skip/return when the drawn input is out of "
                 "domain) so it can never false-fire there.\n"
                 "  (c) IN-DOMAIN and the correct result does NOT trip the check "
                 "— then this firing is the genuine DEFECT the buggy code "
                 "exhibits, and the check is sound: KEEP.\n"
                 "STEP 3 — Watch the classic soundness traps in your STEP-2 "
                 "reasoning: comparing doubles with == / equals when the value "
                 "can be NaN (NaN==NaN is FALSE, so min==max on two NaNs fails "
                 "for correct code); assuming a NaN result implies a NaN operand "
                 "(Inf+(-Inf)=NaN, 0.0/0.0=NaN with no NaN operand); assuming a "
                 "total order where NaN makes it partial; integer overflow "
                 "(Math.abs(MIN_VALUE)<0, -MIN_VALUE==MIN_VALUE); empty/very-"
                 "long strings; empty collections.\n"
                 "DECIDE: if case (c) for every firing, reply with the single "
                 "token KEEP. Otherwise (case a or b) rewrite the check so it "
                 "STILL throws '<name> violated' on the original defect for "
                 "ordinary inputs, but no longer fires on the input you found "
                 "unsound/out-of-domain (handle it in the expected value, or "
                 "guard/skip that drawn input). A rewrite that stops catching "
                 "the defect is wrong. Return ONLY the corrected Java snippet "
                 "(body inside fuzzerTestOneInput, draws from `data`, throws "
                 "OUTSIDE any catch) — no prose, no fences — or the token KEEP."},
                {'role': 'user', 'content':
                 f"Documented contract (the ONLY source of truth for what a "
                 f"correct implementation guarantees, and for what is in-domain "
                 f"vs illegal input): {getattr(rel,'contract','')}\n"
                 f"Available imports:\n{avail}\n\n"
                 f"Soundness-test result: the check fired on {n_fired} extreme "
                 f"inputs.{ordinary_note}\n\n"
                 f"The extreme/boundary inputs fed (identify which one fires "
                 f"it):\n{extremes_text}\n\n"
                 f"Check that fired:\n{getattr(rel,'check','')}\n\n"
                 f"Work through STEP 1, 2, 3, then answer KEEP or a corrected "
                 f"snippet:"},
            ]
            out = (self._record_generate(msg, 'soundness_harden') or "").strip()
            if not out or out.upper().startswith('KEEP'):
                return None
            out = re.sub(r'^```[a-z]*\n?|```$', '', out, flags=re.M).strip()
            # A repaired check must still throw the mandated violation token,
            # or the counting wrapper can never see it — reject a degenerate
            # "repair" that removed the alarm.
            if 'violated' not in out:
                return None
            return out or None
        except Exception:
            return None

    @staticmethod
    def _parse(out: str) -> List[Relation]:
        m = re.search(r'\[.*\]', out, re.DOTALL)
        if not m:
            return []
        try:
            items = json.loads(m.group(0))
        except (ValueError, TypeError):
            return []
        rels: List[Relation] = []
        for it in items if isinstance(items, list) else []:
            if not isinstance(it, dict) or 'check' not in it:
                continue
            rels.append(Relation(
                name=str(it.get('name', 'relation'))[:60],
                kind=str(it.get('kind', 'invariant')),
                contract=str(it.get('contract', '')),
                input_spec=str(it.get('input', '')),
                check=_unflatten_check(str(it['check'])),
            ))
        return rels
