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
    "Propose up to 4 relations for the patched method. For each, give:\n"
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
    "Draw inputs ONLY with the Jazzer FuzzedDataProvider API on `data`:"
    " data.consumeInt(), data.consumeInt(lo,hi), data.consumeLong(),"
    " data.consumeBoolean(), data.consumeAsciiString(n),"
    " data.consumeRemainingAsString() — there is NO drawInt/nextInt. The"
    " check body runs INSIDE fuzzerTestOneInput, so use the real library"
    " types directly (no wrapper class, no imports block).\n"
    "Return ONLY a JSON array of objects with keys name, kind, contract,"
    " input, check. No prose outside the JSON."
)


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


class RelationSynthesizer:
    """Proposes candidate relations (unscreened) for a semantic bug."""

    def __init__(self, generator: Optional[HarnessGenerator] = None):
        self._gen = generator or HarnessGenerator(temperature=0.3, top_p=1.0)

    def synthesize(self, patched_sources: List[str],
                   class_name: str,
                   reachable: List[str],
                   mined_tests: List,
                   trigger_summary: str = '',
                   patch_text: str = '',
                   javadocs: Optional[List[str]] = None,
                   class_context: Optional[List[str]] = None
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
        ctx = ["Patched method(s):"]
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
        for jd in (javadocs or []):
            if jd:
                ctx += ["Documented contract of a touched method (relations"
                        " must follow from contracts like this):",
                        "<javadoc>", jd, "</javadoc>"]
        if reachable:
            ctx.append("Reachable API (call these, do not reimplement): "
                       + ", ".join(reachable[:30]))
        if mined_tests:
            ctx.append("Real API usage from the project's own tests (mirror"
                       " these call shapes; they are trusted):")
            for t in mined_tests[:3]:
                ctx += ["<test>", getattr(t, 'source', str(t)), "</test>"]
        if trigger_summary:
            ctx.append("The reported failure on the buggy code: "
                       + trigger_summary)
        user = "\n".join(ctx) + "\n\n" + _INSTRUCTIONS
        messages = [
            {'role': 'system', 'content': _SYSTEM},
            {'role': 'user', 'content': user},
        ]
        # One retry on an unparseable/empty response: a JSON-shaped ask can
        # still come back wrapped in prose, and silently returning zero
        # candidates hides the failure from the whole run (observed: a leg
        # "tested" synthesis that never proposed anything). The retry
        # re-states the format constraint; a second failure is logged by
        # the caller via the empty return.
        for attempt in range(2):
            try:
                out = self._gen.generate(messages) or ""
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
                check=str(it['check']),
            ))
        return rels
