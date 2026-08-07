"""8.2 stage 0 — the generation prompt for an independent reference
implementation, and the compile-and-run adapter.

THE INFORMATION RULE: BLIND TO IMPLEMENTATIONS, MAXIMAL ON SPECIFICATION.

Never shown, and enforced by a check rather than an instruction:

  * the PATCHED source — it is the artefact under review, and on a fake patch
    it IS the defect (rank-5 trap).
  * the BUGGY implementation body — subtler and worse. A reference that copies
    the buggy code agrees with the buggy build EVERYWHERE, including at the
    defect, so the off-defect screen structurally cannot catch it; it then
    disagrees with a CORRECT patch at exactly the disputed point, i.e. it
    manufactures the false accusation this mechanism exists to prevent.

Shown, as richly as the leg provides:

  * the failing test's FULL source — tier-1 authority; leaking it leaks only
    truth, and the reference is later held to its pinned answers (validator 3).
  * the class's OTHER tests — the project's executable specification. (This is
    the deleted mined-oracles MATERIAL in a different and legitimate use:
    context for what the API is meant to do, never assertions to copy.)
  * the whole documentation surface — class javadoc, sibling-method contracts,
    inherited interface docs, and any cited formula or algorithm name.
    Textbook knowledge of a named algorithm is wanted here, not avoided.
  * the class SKELETON — signatures and fields, no bodies.
  * a few recorded off-defect input->output examples, for CONVENTIONS only
    (return -1 vs throw, units, rounding). What is shown is an open book: the
    screen validates only on observables the generator was never shown.

`build_reference_prompt` refuses to emit a prompt whose material fails
`assert_no_implementation`. That is deliberate: the rule is the mechanism's
foundation, and an instruction that says "do not look at the patch" is exactly
the kind of guidance P4.2 showed models ignore.
"""
import re
from typing import Dict, List, Optional, Sequence

#: Shapes that betray a method BODY rather than a signature or a doc comment.
#: A skeleton line ends in `;` or `{ }`; a body has statements in it.
_BODY_MARKERS = (
    re.compile(r'\breturn\b[^;]*;'),
    re.compile(r'\bif\s*\([^)]*\)\s*\{'),
    re.compile(r'\bfor\s*\([^)]*\)\s*\{'),
    re.compile(r'\bwhile\s*\([^)]*\)\s*\{'),
    re.compile(r'\bthrow\s+new\b'),
    re.compile(r'[-+*/]=\s*'),
)


class ImplementationLeak(ValueError):
    """Raised when prompt material contains an implementation body."""


def strip_comments(text: str) -> str:
    """Java comments removed, so documentation cannot be read as code.

    Stage-1 roll 1 refused every prompt on `@return RMS value` — a JAVADOC tag.
    The `return ...;` marker matched from inside a comment across into the next
    statement's semicolon. Documentation is the one thing this prompt must
    carry MAXIMALLY, so a detector that reads it as an implementation refuses
    exactly the material the design depends on.
    """
    if not text:
        return ''
    text = re.sub(r'/\*.*?\*/', ' ', text, flags=re.S)     # block + javadoc
    text = re.sub(r'//[^\n]*', ' ', text)                   # line comments
    # BARE CONTINUATION LINES. assemble_class_context delivers javadoc with its
    # /** and */ delimiters already removed, leaving 332 lines of ` * ...` in
    # Math-65 alone. With no opener the block pattern above matches nothing, so
    # `@return RMS value` survived and matched the `return ...;` marker across
    # newlines to the next semicolon. Stage-1 rolls 1 AND 3 both died here; the
    # first fix handled only delimited comments and looked complete.
    return re.sub(r'^[ \t]*\*.*$', ' ', text, flags=re.M)


def looks_like_implementation(text: str) -> Optional[str]:
    """The first body marker found in `text`, or None.

    Comments are stripped FIRST (see `strip_comments`): javadoc is
    specification, not implementation, and reading it as code refuses the very
    material the reference needs.

    Still deliberately conservative in the LEAKY direction on actual code: it
    is better to refuse a borderline skeleton than to ship a prompt that
    quietly contained the implementation the reference must be independent of.
    """
    if not text:
        return None
    stripped = strip_comments(text)
    for pat in _BODY_MARKERS:
        m = pat.search(stripped)
        if m:
            return m.group(0)[:60]
    return None


def assert_no_implementation(material: Dict[str, str]) -> None:
    """Refuse any section carrying an implementation body.

    `material` maps section name -> text. Sections whose whole purpose is to
    contain code the reference may see (tests, which are specification) are
    exempt by name; everything else is checked.
    """
    exempt = {'failing_test', 'other_tests'}
    for name, text in (material or {}).items():
        if name in exempt:
            continue
        hit = looks_like_implementation(text)
        if hit:
            raise ImplementationLeak(
                f'section {name!r} contains what looks like an implementation '
                f'body ({hit!r}). The reference must be blind to both the '
                f'patched source and the buggy body — a bug-copying reference '
                f'passes the off-defect screen and then falsely accuses a '
                f'correct patch.')


_SYSTEM = (
    "You implement a single method from its SPECIFICATION. You are given "
    "documentation, tests, and signatures — never an existing implementation "
    "of the method itself. Write the method as the specification says it "
    "should behave, using standard knowledge of any algorithm it names. "
    "Return one compilable Java class and nothing else."
)


def build_reference_prompt(method: str,
                           skeleton: str,
                           docs: Sequence[str],
                           failing_test: str,
                           other_tests: Sequence[str] = (),
                           shown_examples: Optional[Dict[str, str]] = None,
                           package: Optional[str] = None) -> List[Dict[str, str]]:
    """Chat messages that ask for an independent reference implementation.

    Raises ImplementationLeak if any non-test section carries a method body.
    """
    material = {
        'skeleton': skeleton or '',
        'docs': '\n\n'.join(d for d in (docs or []) if d),
        'failing_test': failing_test or '',
        'other_tests': '\n\n'.join(t for t in (other_tests or []) if t),
    }
    assert_no_implementation(material)

    parts = [
        f"Implement `{method}` from its specification.",
        "",
        "You are NOT shown any existing implementation of it, and you must "
        "not try to reconstruct one from memory of this project. Derive the "
        "behaviour from the documentation and the tests below. Where the "
        "documentation names a standard algorithm or formula, use the "
        "standard definition of it.",
    ]
    if material['docs']:
        parts += ["", "=== DOCUMENTATION (class, method, siblings, inherited) "
                      "===", material['docs']]
    if material['failing_test']:
        parts += ["", "=== THE FAILING TEST, in full — it pins correct "
                      "behaviour at its own inputs ===", material['failing_test']]
    if material['other_tests']:
        parts += ["", "=== OTHER TESTS OF THIS CLASS — the project's "
                      "executable specification ===", material['other_tests']]
    if shown_examples:
        parts += ["", "=== OBSERVED INPUT -> OUTPUT EXAMPLES (for CONVENTIONS "
                      "only: return values vs exceptions, units, rounding) ==="]
        for k, v in list(shown_examples.items())[:8]:
            parts.append(f"  {k} -> {v}")
    parts += ["", "=== SKELETON (signatures and fields; bodies are withheld) "
                  "===", material['skeleton']]
    parts += [
        "",
        "Write ONE self-contained Java class named `ReferenceImpl` whose "
        "PUBLIC STATIC methods are named `compute_<observable>`"
        + (f", in package {package}" if package else "")
        + ".",
        "",
        "FUNCTIONALIZE IT. These methods may read object state rather than "
        "take arguments; each `compute_<name>` must take that state as "
        "PARAMETERS and be pure — same inputs, same result, no fields, no I/O. "
        "Declare exactly the data the computation needs, in the SAME parameter "
        "order for every one, and return the documented type. A reference that "
        "reads no input cannot be run on different inputs, and one that cannot "
        "be varied cannot be checked.",
        "",
        "NAME each parameter after the state it represents, using the name "
        "the class uses for it, and repeat those names in the declaration "
        "line below. Take only state the computation actually consumes: a "
        "parameter for something the quantity does not depend on cannot be "
        "supplied and the reference will be discarded unused.",
        "",
        "IMPLEMENT THE SIBLINGS TOO, not only the method named above. The "
        "documentation defines several quantities over the same state; write "
        "`compute_<name>` for each one you can derive from the documentation. "
        "They are how this reference earns its standing: the named method is "
        "where the disagreement under review lives, so it cannot also be the "
        "evidence that the reference is trustworthy. The siblings are.",
        "",
        "First line of your reply must be a comment listing what you wrote, "
        "exactly: `// compute(<shared parameter types>) : <name1>, <name2>, "
        "...`. Then the class. No markdown fences, no prose.",
    ]
    return [{'role': 'system', 'content': _SYSTEM},
            {'role': 'user', 'content': '\n'.join(parts)}]


def strip_bodies(class_context: str) -> str:
    """A body-free skeleton from assembled class context.

    NECESSARY because `assemble_class_context` deliberately KEEPS the patched
    methods' bodies (it elides only non-patched members) — and the patched body
    is the one thing this prompt must never contain. Brace-matched, so a nested
    block does not end a method early.

    The result is passed through `assert_no_implementation` by the caller, so a
    stripping miss fails the build loudly rather than leaking quietly.
    """
    if not class_context:
        return ''
    out, i, n = [], 0, len(class_context)
    while i < n:
        ch = class_context[i]
        if ch != '{':
            out.append(ch)
            i += 1
            continue
        depth, j = 0, i
        while j < n:
            if class_context[j] == '{':
                depth += 1
            elif class_context[j] == '}':
                depth -= 1
                if depth == 0:
                    break
            j += 1
        inner = class_context[i + 1:j]
        # A class/interface body holds members, not statements — keep
        # descending into it; a METHOD body is what we drop.
        if re.search(r'\b(class|interface|enum)\b[^;{]*$',
                     ''.join(out)[-160:]):
            out.append('{')
            out.append(strip_bodies(inner))
            out.append('}')
        else:
            out.append('{ /* body withheld */ }')
        i = j + 1
    return ''.join(out)


#: Return types whose value is a computation rather than a stored setting.
#: Deliberately excludes the object types that would need deep comparison.
_OBSERVABLE_TYPES = ('double', 'float', 'int', 'long', 'boolean',
                     'double[]', 'int[]', 'double[][]', 'String')


def sibling_observables(class_context: str, disputed: str,
                        cap: int = 8, declaring_types=None) -> list:
    """The class's PUBLIC NO-ARG observables, minus the disputed one.

    These are the screening surface. The disputed point is on-defect almost by
    definition -- it is where the bug lives -- so it cannot also be the evidence
    that the reference is trustworthy; its siblings are.

    A first walkthrough of Math-65 extracted 2 observables and concluded the
    class was too thin to screen. That was the EXTRACTOR: it matched only
    `double` returns. The real surface is 16. Measuring a mechanism's reach with
    a regex that sees a third of the data is how a design gets abandoned for a
    property it does not have.

    STORED SETTINGS ARE EXCLUDED, not merely sorted last (VM re-walk #4). A
    getter that echoes a constructor argument -- getMaxIterations,
    getMaxEvaluations -- agrees between reference and buggy build for free,
    because both were handed the same number. Counting it toward
    MIN_SCREENED_OBSERVABLES inflates the screen's strength without adding
    any independence: "8 off-defect observables" that is really "6 plus 2
    free passes". Excluding them can push a leg BELOW the bar, and that is
    the honest outcome -- the screen failing closed on a class with too few
    computed quantities is correct, where passing on padding is not.
    """
    if not class_context:
        return []
    # SCOPE TO THE RECEIVER'S OWN TYPE (VM re-walk #4). The context holds
    # every collaborator class, so an unscoped scan returned getPoint,
    # getPointRef and getArgument -- declared on the optimizer's RESULT
    # object, not on the optimizer. The twin calls these on the receiver,
    # so an out-of-type sibling is a guaranteed compile error, and the
    # receiver is chosen by declaring type precisely so the two agree.
    scope = class_context
    if declaring_types:
        want = {str(t).split('.')[-1] for t in declaring_types}
        blocks = []
        for m in re.finditer(r'<class\s+name="([^"]+)"[^>]*>', class_context):
            if m.group(1).split('.')[-1] in want:
                close = class_context.find('</class>', m.end())
                blocks.append(class_context[m.end():
                                            close if close > 0 else len(class_context)])
        if blocks:
            scope = '\n'.join(blocks)
    found = re.findall(
        r'public\s+(?:final\s+)?([\w\[\]<>.]+)\s+(\w+)\s*\(\s*\)',
        scope)
    stored = re.compile(r'^(?:get|is)(Max|Min|Default|Absolute|Relative)\w*$')
    out = []
    for typ, name in found:
        if name == disputed or typ not in _OBSERVABLE_TYPES:
            continue
        if stored.match(name):
            continue                     # free pass, not evidence
        if name not in [n for _t, n in out]:
            out.append((typ, name))
    out.sort(key=lambda tn: tn[1])
    return [n for _t, n in out][:cap]
