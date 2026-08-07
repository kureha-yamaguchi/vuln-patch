"""8.2 stage 0 — the compile-and-run adapter for a generated reference.

Reuses `HarnessBuilder` for compilation (the project classpath is already
resolved and cached there) and runs the reference through a small driver that
prints its observables as `key=value` lines — the same shape `observed_values`
already parses, so nothing new has to learn a format.

OUR CODE PICKS THE OBSERVABLES (the P4.2 lesson). The generated class exposes an
entry point; the DRIVER decides which inputs to feed it and prints every result.
A reference compared only where the model remembered to print would carry P4.2's
bug in new clothes: half the certifier's "no difference found" answers were
wrong, every one from a prompt that asked the model to print everything.

FAILS CLOSED throughout. Compile failure, run failure, timeout, empty output,
unparseable output — all return `(None, reason)`. A reference we could not run
is a reference with no standing, exactly as an unscreened relation is an
uninjected relation.
"""
import os
import re
import subprocess
from typing import Dict, List, Optional, Tuple

from java.relations.evidence_facts import observed_values


def _java_string(expr: str) -> str:
    """A Java string literal holding `expr` verbatim."""
    return '"' + expr.replace(chr(92), chr(92)*2).replace('"', chr(92)+'"') + '"'


#: The driver prints one `key=value` per observable, then this marker. Its
#: absence means the run did not complete, however the process exited.
END_MARKER = '[[reference-run-complete]]'


def declared_signature(reference_source: str) -> Optional[str]:
    """The `// compute(<types>)` line the prompt requires, or None.

    The generator declares the signature it chose; our code reads it rather
    than guessing. A reference whose signature we cannot read is a reference we
    cannot drive, which is a discard -- not an assumption.
    """
    if not reference_source:
        return None
    m = re.search(r'//\s*compute\(([^)\n]*)\)', reference_source)
    return m.group(1).strip() if m else None


def build_driver(reference_class: str,
                 observables: List[str],
                 vectors: List[str],
                 package: Optional[str] = None,
                 call: str = 'compute_{obs}') -> str:
    """Call EVERY observable on EVERY vector, keyed by OBSERVABLE NAME.

    KEYING IS THE DESIGN. `observed_values` returns {key: [values]}, and the
    screen counts KEYS -- so keying by observable name makes
    MIN_SCREENED_OBSERVABLES mean "three DISTINCT observables", not "three
    input/output pairs through one formula". N vectors through a single formula
    are N correlated samples of one claim; they would satisfy the letter of the
    screen while gutting its independence.

    A throw is RECORDED, not skipped: a documented rejection contract is an
    observable. Matching throws are agreement (shared semantics); a one-sided
    throw is a disagreement, and exactly the misunderstanding the screen exists
    to catch.
    """
    # Roll 4 died on `build_driver('ReferenceImpl', 'compute', [''])` -- a str
    # iterates as characters (compute_c, compute_o, ...). The type guard makes
    # that CLASS of call-site lag impossible rather than merely fixed.
    if isinstance(observables, str) or isinstance(vectors, str):
        raise TypeError('observables and vectors must be lists, not str '
                        '(a str iterates as characters -- the roll-4 bug)')
    pkg = f'package {package};\n\n' if package else ''
    calls = []
    _k = 0
    for obs in observables:
        for j, vec in enumerate(vectors):
            fn = call.format(obs=obs)
            _k += 1
            calls.append(
                '    try {\n'
                f'      Object r{_k} = {reference_class}.{fn}({vec});\n'
                f'      System.out.println("{obs}=" + String.valueOf(r{_k}));\n'
                '    } catch (Throwable t) {\n'
                f'      System.out.println("{obs}=EX:" '
                '+ t.getClass().getSimpleName());\n'
                '    }')
    return (pkg + 'public class ReferenceDriver {\n'
            '  public static void main(String[] args) {\n'
            + '\n'.join(calls) + '\n'
            f'    System.out.println("{END_MARKER}");\n'
            '  }\n}\n')


def build_buggy_twin_driver(fq_class: str,
                            construct: str,
                            observables: List[str],
                            vectors: List[str],
                            package: Optional[str] = None) -> str:
    """The OTHER side of the comparison, run LIVE on the buggy build.

    Fuzzed vectors have no recorded buggy values, so the screen needs the buggy
    class executed on the SAME constructed states. Admissible: the buggy build
    is authority rank 2 whether its values are archived or produced now.
    Bounded cost -- one class, no fuzzing loop.

    CONSTRUCTION IS ATTEMPTED ONCE PER VECTOR AND ECHOED. Mis-construction is a
    REACH risk rather than a safety one -- a wrong state produces systematic
    disagreement across every observable, so the reference is discarded, and
    wrong-state AGREEMENT across many independent observables is implausibility
    stacked on implausibility. But a discard then reads as "bad reference" when
    it was "bad twin", and that misattribution costs a roll. So each vector
    emits `__construct<j>` (OK or the exception) and `__state<j>` (the
    expression used), making state-mismatch distinguishable from semantic
    disagreement in one read.

    Constructing once per vector rather than once per (observable, vector) also
    means a single failure is reported once, not N times.
    """
    pkg = f'package {package};\n\n' if package else ''
    blocks = []
    for j, vec in enumerate(vectors):
        reads = '\n'.join(
            '      try {\n'
            f'        Object r{_i} = o.{obs}();\n'
            f'        System.out.println("{obs}=" + String.valueOf(r{_i}));\n'
            '      } catch (Throwable t) {\n'
            f'        System.out.println("{obs}=EX:" '
            '+ t.getClass().getSimpleName());\n'
            '      }'
            for _i, obs in enumerate(observables))
        blocks.append(
            f'    System.out.println("__state{j}=" + {_java_string(vec)});\n'
            '    try {\n'
            f'      {fq_class} o = {construct.replace("{vec}", vec)};\n'
            f'      System.out.println("__construct{j}=OK");\n'
            + reads + '\n'
            '    } catch (Throwable t) {\n'
            f'      System.out.println("__construct{j}=EX:" '
            '+ t.getClass().getSimpleName());\n'
            '    }')
    return (pkg + 'public class BuggyTwinDriver {\n'
            '  public static void main(String[] args) {\n'
            + '\n'.join(blocks) + '\n'
            f'    System.out.println("{END_MARKER}");\n'
            '  }\n}\n')


def construction_report(output: str) -> Dict[str, str]:
    """`{__construct<j>: OK|EX:Type}` from a twin run -- the attribution read.

    A discard with every `__construct` OK is a SEMANTIC disagreement; one with a
    failed construct is a bad twin, and the difference is one grep rather than
    one roll.
    """
    return {k: v[0] for k, v in observed_values(output).items()
            if k.startswith('__construct') or k.startswith('__state')}


def run_reference(builder,
                  buggy_dir: str,
                  reference_source: str,
                  driver_source: str,
                  timeout_seconds: int = 60,
                  work_subdir: str = 'reference') -> Tuple[
                      Optional[Dict[str, List[str]]], str]:
    """Compile the reference + driver and run them. `(observables, reason)`.

    `observables` is None on ANY failure — the caller must treat that as "no
    standing", never as "no difference found".
    """
    try:
        ref = builder.build(reference_source, buggy_dir,
                            output_subdir=work_subdir)
    except Exception as e:                       # pragma: no cover - defensive
        return None, f'reference compile raised: {type(e).__name__}: {e}'
    if not getattr(ref, 'compiled', False):
        return None, 'reference did not compile — DISCARDED'
    try:
        drv = builder.build(driver_source, buggy_dir,
                            output_subdir=work_subdir)
    except Exception as e:                       # pragma: no cover - defensive
        return None, f'driver compile raised: {type(e).__name__}: {e}'
    if not getattr(drv, 'compiled', False):
        return None, 'driver did not compile — DISCARDED'
    cp = getattr(drv, 'classpath', '') or ''
    cls = getattr(drv, 'class_name', 'ReferenceDriver')
    try:
        p = subprocess.run(['java', '-cp', cp, cls],
                           capture_output=True, text=True,
                           timeout=timeout_seconds,
                           cwd=os.path.dirname(getattr(drv, 'harness_path', '')
                                               or buggy_dir))
    except subprocess.TimeoutExpired:
        return None, f'reference run timed out after {timeout_seconds}s'
    except Exception as e:                       # pragma: no cover - defensive
        return None, f'reference run raised: {type(e).__name__}: {e}'
    out = (p.stdout or '') + '\n' + (p.stderr or '')
    if END_MARKER not in out:
        return None, ('reference run did not complete (no end marker; exit '
                      f'{p.returncode}) — DISCARDED')
    obs = observed_values(out.split(END_MARKER)[0])
    # Bookkeeping keys are NOT observables. `__state`/`__construct` exist for
    # attribution, and counting them would let the twin's own diagnostics
    # satisfy MIN_SCREENED_OBSERVABLES -- volume meeting the letter of the bar
    # while contributing nothing to independence, the exact failure the
    # distinct-observable rule was written against.
    obs = {k: v for k, v in obs.items() if not k.startswith('__')}
    if not obs:
        return None, 'reference produced no parseable observables — DISCARDED'
    return obs, f'reference ran and produced {len(obs)} observable(s)'


# ===========================================================================
# THE STATE-TWIN ARCHITECTURE (post-roll-4 redesign, 2026-08-07).
#
# Roll 4 exposed the real design gap: the chain never settled WHAT STATE the
# reference, the buggy build and the patched build are compared at. Synthesized
# vectors cannot construct arbitrary object state (Math-65's optimizer state is
# not constructible from a signature), and a reference compared at one state
# against a build observed at another measures nothing.
#
# The answer: EVERYTHING RUNS AT THE FAILING TEST'S OWN STATE.
#   * the twin driver replays the test's SETUP (assertions stripped) on the
#     buggy build AND on the patched build -- tier-1 material, shown to the
#     generator by design, so no holdout is violated by using its state;
#   * the twin prints the receiver's observables AND (via reflection) the
#     state fields the declared signature needs, so the reference's inputs
#     come from the twin itself;
#   * the SCREEN compares reference-vs-buggy on sibling observables;
#   * the FACT compares reference-vs-patched at the same state, where the
#     disputed observable's difference is meaningful;
#   * the PIN CHECK finally has real overlap: at test state, the failing
#     test's pinned answer applies to the disputed observable.
#
# The screen's exam is open-INPUT, closed-OUTPUT: the generator saw the test's
# inputs, but the sibling VALUES are printed nowhere -- it must compute them
# from documented formulas. Transcription cannot pass it; understanding can.
# The fact's standing sentence must therefore say "at the failing test's own
# state", never "states it was never shown".
# ===========================================================================

_PRIMITIVE_DEFAULTS = {
    'double': '0.0', 'float': '0.0f', 'int': '0', 'long': '0L',
    'boolean': 'false', 'String': '""',
}


def declared_observable_names(reference_source: str) -> List[str]:
    """The `: name1, name2` tail of the declared-signature line, or []."""
    if not reference_source:
        return []
    m = re.search(r'//\s*compute\([^)\n]*\)\s*:\s*([^\n]+)', reference_source)
    if not m:
        return []
    return [n.strip() for n in m.group(1).split(',') if n.strip()]


def parse_parameters(sig: str) -> List[Tuple[str, str]]:
    """`(type, name)` pairs from a declared signature; name may be ''.

    Accepts both bare-type lists (`double[], double[], double`) and named
    lists (`double[] residuals, double[] residualsWeights, double cost`).
    """
    out = []
    for part in (sig or '').split(','):
        toks = part.strip().split()
        if not toks:
            continue
        if len(toks) == 1:
            out.append((toks[0], ''))
        else:
            out.append((' '.join(toks[:-1]), toks[-1]))
    return out


def canonical_state(class_context: str) -> List[Tuple[str, str]]:
    """The class's declared FIELDS -- the canonical state vocabulary.

    The frame everything anchors on: parameters are matched to canonical
    variables by NAME, and the twin recovers their values by reflection.
    """
    if not class_context:
        return []
    found = re.findall(
        r'(?:private|protected|public)\s+(?:static\s+|final\s+|transient\s+)*'
        r'([\w\[\]<>.]+)\s+(\w+)\s*[;=]', class_context)
    seen, out = set(), []
    for typ, name in found:
        if typ in ('return', 'new', 'class') or name in seen:
            continue
        seen.add(name)
        out.append((typ, name))
    return out


def match_parameters(params: List[Tuple[str, str]],
                     canonical: List[Tuple[str, str]]
                     ) -> Tuple[Optional[List[str]], str]:
    """Each declared parameter matched to a canonical field. `(names, why)`.

    Nominal first (exact, then case-insensitive, then unique
    substring-either-way); unique-type fallback only when exactly one unused
    canonical field has that type. Anything unmatchable -> (None, reason):
    an unmappable signature is a DISCARD with its reason, never a guessed
    call (roll 4's five-signatures-in-five-attempts finding).
    """
    if not params:
        return None, 'declared signature has no parameters'
    unused = list(canonical)
    resolved = []
    for typ, name in params:
        pick = None
        if name:
            for c in unused:
                if c[1] == name:
                    pick = c
                    break
            if pick is None:
                for c in unused:
                    if c[1].lower() == name.lower():
                        pick = c
                        break
            if pick is None:
                subs = [c for c in unused
                        if name.lower() in c[1].lower()
                        or c[1].lower() in name.lower()]
                if len(subs) == 1:
                    pick = subs[0]
        if pick is None:
            same_t = [c for c in unused if c[0] == typ]
            if len(same_t) == 1:
                pick = same_t[0]
        if pick is None:
            label = f'{typ} {name}'.strip()
            return None, (f'parameter `{label}` matches no canonical state '
                          f'field (fields: '
                          f'{[n for _t, n in canonical][:8]}) — signature '
                          f'unmappable, DISCARDED')
        unused.remove(pick)
        resolved.append(pick[1])
    return resolved, f'matched {len(resolved)} parameter(s) to state fields'


_ASSERT_STMT = re.compile(
    r'(?:org\.junit\.)?(?:Assert\.)?(?:assert\w+|fail)\s*\(')


def _strip_assert_statements(body: str) -> str:
    """Remove assertion STATEMENTS, preserving everything else.

    Statement-aware, not line-based: the VM re-walk showed line-dropping
    eats closing braces (`assertTrue(x); }` lost its `}`) and leaves `try`
    blocks without their tails. Walks each assert call to its matching `)`
    and the following `;`, removes exactly that span.
    """
    out, i, n = [], 0, len(body)
    while i < n:
        m = _ASSERT_STMT.search(body, i)
        if not m:
            out.append(body[i:])
            break
        out.append(body[i:m.start()])
        j = body.index('(', m.start())
        depth = 0
        while j < n:
            if body[j] == '(':
                depth += 1
            elif body[j] == ')':
                depth -= 1
                if depth == 0:
                    break
            j += 1
        j += 1
        while j < n and body[j] in ' \t':
            j += 1
        if j < n and body[j] == ';':
            j += 1
        i = j
    return ''.join(out)


def _match_brace(text: str, open_idx: int) -> int:
    """Index of the `}` matching `{` at open_idx, or -1."""
    depth = 0
    for j in range(open_idx, len(text)):
        if text[j] == '{':
            depth += 1
        elif text[j] == '}':
            depth -= 1
            if depth == 0:
                return j
    return -1


def isolate_test_method(source, disputed, siblings):
    """The ONE test method from a possibly-annotated blob, or None.

    The VM re-walk found the chain's input is not always a clean method: the
    recorded form carries advisory comments, helper classes and field lists.
    Stripping assertions from THAT produced structurally invalid Java and a
    wrong receiver. So: find every `void name(...) {...}` method, pick the
    one that calls the disputed observable, else the one with the most
    sibling calls; extract exactly its brace-matched span.
    """
    import re as _re
    if not source:
        return None
    best, best_score = None, -1
    for m in _re.finditer(r'(?:public\s+)?void\s+\w+\s*\([^)]*\)'
                          r'(?:\s*throws\s+[\w.,\s]+)?\s*\{', source):
        end = _match_brace(source, source.index('{', m.start()))
        if end < 0:
            continue
        meth = source[m.start():end + 1]
        score = 0
        if _re.search(r'\.' + _re.escape(disputed) + r'\s*\(', meth):
            score += 100
        score += sum(
            len(_re.findall(r'\.' + _re.escape(s) + r'\s*\(', meth))
            for s in (siblings or []))
        if score > best_score:
            best, best_score = meth, score
    return best


def types_declaring(class_context, method):
    """Class names whose body declares `method`, plus `extends` closure.

    VM re-walk #2: the receiver must be selected by DECLARING TYPE, not by
    call frequency -- the most-called variable was the optimizer's RESULT
    object, and a same-named method on another type would have produced a
    compilable twin reading the wrong object (the silent-wrong-state case).

    VM re-walk #3: the context arrives XML-WRAPPED
    (a <class name="X" role="..."> ... </class> wrapper), and a bare
    class-name pattern matched the ATTRIBUTE, returning {'name'} six
    times over -- so the one class that actually declares getChiSquare was
    invisible and every candidate was rejected. Both shapes are parsed now.
    This is the sibling-extractor lesson again (16 observables read as 2):
    check a matcher's output against ground truth before a decision rests
    on it, which is why `plausible_class_names` exists below.
    """
    import re as _re
    if not class_context or not method:
        return set()
    decl = _re.compile(r'\b' + _re.escape(method) + r'\s*\(')
    out, extends = set(), {}

    # (a) XML-wrapped blocks: name from the attribute, body to </class>.
    for m in _re.finditer(r'<class\s+name="([^"]+)"[^>]*>', class_context):
        name = m.group(1).split('.')[-1]
        close = class_context.find('</class>', m.end())
        body = class_context[m.end():close if close > 0 else len(class_context)]
        if decl.search(body):
            out.add(name)
        e = _re.search(r'\bclass\s+' + _re.escape(name)
                       + r'\b[^{]*?\bextends\s+([\w.]+)', body)
        if e:
            extends[name] = e.group(1).split('.')[-1]

    # (b) Bare `class X ... { ... }` declarations (unwrapped context).
    for m in _re.finditer(r'\bclass\s+([A-Z]\w*)\b([^{;]*)\{', class_context):
        name = m.group(1)
        # Not an XML tag (<class name=...>) and not javadoc prose: a real
        # declaration is never preceded by '<'.
        if class_context[max(0, m.start() - 1):m.start()] == '<':
            continue
        e = _re.search(r'\bextends\s+([\w.]+)', m.group(2))
        if e:
            extends.setdefault(name, e.group(1).split('.')[-1])
        idx = class_context.index('{', m.start())
        close = _match_brace(class_context, idx)
        body = (class_context[idx:close + 1] if close > 0
                else class_context[idx:])
        if decl.search(body):
            out.add(name)

    # Transitive `extends` closure: a subclass of a declaring type declares
    # it too (getChiSquare lives on AbstractLeastSquaresOptimizer; the test
    # declares a LevenbergMarquardtOptimizer).
    for _ in range(4):
        grew = False
        for child, parent in extends.items():
            if parent in out and child not in out:
                out.add(child)
                grew = True
        if not grew:
            break
    return out


def plausible_class_names(names):
    """True if a parsed type set looks like Java class names, not artefacts.

    VM re-walk #3's tell: a declaring-type set of {'name'} was self-evidently
    wrong to a reader and invisible to the code. A parse that yields only
    lowercase or XML-attribute-ish tokens is a BROKEN PARSE, and the caller
    must discard loudly rather than treat it as "no declaring type found" --
    the two are different failures and only one is about the leg.
    """
    bad = {'name', 'role', 'class', 'value', 'type'}
    real = [n for n in (names or ()) if n and n not in bad and n[:1].isupper()]
    return bool(real)


def extract_test_setup(test_source, disputed, siblings=None,
                       declaring_types=None):
    """`(setup_code, receiver_var, reason)` from a failing-test method OR an
    annotated blob (the VM re-walk found both shapes arrive here).

    Receiver = the variable the DISPUTED observable is invoked on; when the
    test never calls it (Math-65's testCircleFitting never calls
    getChiSquare), the variable with the most SIBLING-observable calls --
    the object whose state the test actually inspects.
    """
    import re as _re
    if not test_source:
        return None, None, 'no failing-test source available'
    meth = isolate_test_method(test_source, disputed, siblings or [])
    if meth is None:
        return None, None, ('no test method isolatable from the source '
                            '(annotated blob without a void method?)')
    i = meth.find('{')
    body = meth[i + 1:-1]
    setup = _strip_assert_statements(body).strip()
    if not setup:
        return None, None, 'failing-test body is all assertions'
    if setup.count('{') != setup.count('}'):
        return None, None, ('setup braces unbalanced after assertion '
                            'stripping -- refusing to emit invalid Java')
    # RECEIVER BY DECLARING TYPE (VM re-walk #2). Usage-pattern guesses
    # (most-called, last-constructed) both picked wrong objects; worse, a
    # same-named method on another type compiles and reads the WRONG object.
    # A candidate needs type evidence: its declared type must be one that
    # declares the disputed observable -- and a VISIBLE declaration of the
    # wrong type VETOES a by-call candidate. No type evidence -> discard.
    declaring = {str(t) for t in (declaring_types or ())}
    decls = {v: t for t, v in _re.findall(
        r'\b([A-Z]\w*)(?:<[^>]*>)?\s+(\w+)\s*=', body)}
    by_call = [v for v in _re.findall(
        r'(\w+)\s*\.\s*' + _re.escape(disputed) + r'\s*\(', body)]
    receiver = None
    for v in by_call:
        t = decls.get(v)
        if t is None or not declaring or t in declaring:
            receiver = v            # call evidence, type unknown or confirmed
            break
    if receiver is None and declaring:
        typed = [v for v in decls if decls[v] in declaring]
        if len(set(typed)) == 1:
            receiver = typed[0]
    if receiver is None:
        return None, None, (
            'no receiver with type evidence: no variable both reaches the '
            'disputed observable and is declared with a type that declares '
            'it (declaring types: ' + str(sorted(declaring)[:4]) + ') — '
            'DISCARDED rather than guessed')
    return setup, receiver, f'setup extracted; receiver `{receiver}`'


def extract_test_dependencies(test_file_source, setup_code):
    """`(imports, helper_class_sources)` the setup needs, from the test FILE.

    The VM re-walk's deepest finding: a test's setup may construct FIXTURE
    classes that exist only in the test file (`new Circle()`), so an
    isolated method cannot compile without them. Helpers referenced by
    `new <Name>(` are extracted brace-matched and emitted as top-level
    package-private classes beside the driver. Missing file or missing
    helper -> the caller discards with a reason; nothing is guessed.
    """
    import re as _re
    if not test_file_source or not setup_code:
        return [], []
    imports = _re.findall(r'^import\s+[\w.*]+\s*;', test_file_source,
                          _re.M)
    imports = [i for i in imports if 'junit' not in i]
    needed = set(_re.findall(r'new\s+([A-Z]\w*)\s*\(', setup_code))
    helpers = []
    for name in sorted(needed):
        m = _re.search(r'(?:private|public|protected)?\s*(?:static\s+)?'
                       r'(?:final\s+)?class\s+' + _re.escape(name)
                       + r'\b[^{]*\{', test_file_source)
        if not m:
            continue
        end = _match_brace(test_file_source,
                           test_file_source.index('{', m.start()))
        if end < 0:
            continue
        cls = test_file_source[m.start():end + 1]
        cls = _re.sub(r'^\s*(?:private|public|protected)\s+', '', cls)
        cls = _re.sub(r'^\s*static\s+', '', cls)
        helpers.append(cls)
    return imports, helpers


def test_package(test_file_source):
    """The test file's own `package` declaration, or None.

    VM re-walk #2: the test refers to the class under test by SIMPLE NAME
    because they share a package. A twin emitted package-less loses that
    implicit resolution ("cannot find symbol
    LevenbergMarquardtOptimizer"). Emitting the twin INTO the test's package
    restores exactly the resolution the original test method had -- the
    honest fix, since the twin is that method.
    """
    import re as _re
    if not test_file_source:
        return None
    m = _re.search(r'^\s*package\s+([\w.]+)\s*;', test_file_source, _re.M)
    return m.group(1) if m else None


def ascii_safe(java_source):
    """Every non-ASCII char as a backslash-uXXXX escape -- semantically identical.

    Java processes unicode escapes BEFORE lexing, so this is a total fix for
    the VM's `unmappable character for encoding US-ASCII` failure: an
    em-dash in a copied comment compiles identically as backslash-u2014,
    everywhere, including inside comments and string literals.
    """
    return ''.join(c if ord(c) < 128 else '\\u%04x' % ord(c)
                   for c in java_source)


#: Reflection helper injected into the twin: walks the class hierarchy for a
#: named field and prints it (arrays via Arrays.toString), so the reference's
#: inputs are recoverable regardless of getter availability or visibility.
_REFLECT_HELPER = '''
  static void printField(Object o, String name) {
    try {
      Class<?> c = o.getClass();
      java.lang.reflect.Field f = null;
      while (c != null && f == null) {
        try { f = c.getDeclaredField(name); }
        catch (NoSuchFieldException e) { c = c.getSuperclass(); }
      }
      if (f == null) { System.out.println("__param_" + name + "=ABSENT"); return; }
      f.setAccessible(true);
      Object v = f.get(o);
      String s;
      if (v instanceof double[]) s = java.util.Arrays.toString((double[]) v);
      else if (v instanceof int[]) s = java.util.Arrays.toString((int[]) v);
      else if (v instanceof Object[]) s = java.util.Arrays.deepToString((Object[]) v);
      else s = String.valueOf(v);
      System.out.println("__param_" + name + "=" + s);
    } catch (Throwable t) {
      System.out.println("__param_" + name + "=EX:" + t.getClass().getSimpleName());
    }
  }
'''


def build_state_twin_driver(setup_code: str,
                            receiver: str,
                            observables: List[str],
                            param_fields: List[str],
                            package: Optional[str] = None,
                            imports: Optional[List[str]] = None,
                            helper_classes: Optional[List[str]] = None) -> str:
    """The twin: replay the failing test's setup, then print everything.

    Runs UNMODIFIED on the buggy build and on the patched build -- one source,
    two classpaths, which is what makes its two outputs comparable. Prints
    `__construct0=OK` after setup (the attribution read), each observable as
    `name=value`, and each needed state field as `__param_<name>=...` via
    reflection.
    """
    if isinstance(observables, str) or isinstance(param_fields, str):
        raise TypeError('observables and param_fields must be lists, not str')
    pkg = f'package {package};\n\n' if package else ''
    imp = ''.join((i if i.rstrip().endswith(';') else f'import {i};') + '\n'
                  for i in (imports or []))
    helpers = '\n\n'.join(helper_classes or [])
    # UNIQUE read variables (VM re-walk #4: `variable r is already defined`
    # with 9 real siblings). Each read is already try-scoped, so this is
    # belt-and-braces -- but a name that cannot collide removes the failure
    # mode from every caller and every future refactor of the block shape.
    reads = '\n'.join(
        '      try {\n'
        f'        Object r{_i} = {receiver}.{obs}();\n'
        f'        System.out.println("{obs}=" + String.valueOf(r{_i}));\n'
        '      } catch (Throwable t) {\n'
        f'        System.out.println("{obs}=EX:" '
        '+ t.getClass().getSimpleName());\n'
        '      }'
        for _i, obs in enumerate(observables))
    params = '\n'.join(
        f'      printField({receiver}, "{name}");' for name in param_fields)
    out = (pkg + imp + 'public class StateTwinDriver {\n'
            + _REFLECT_HELPER +
            '  public static void main(String[] args) throws Exception {\n'
            '    try {\n'
            + '\n'.join('      ' + l for l in setup_code.splitlines()) + '\n'
            '      System.out.println("__construct0=OK");\n'
            + reads + '\n' + params + '\n'
            '    } catch (Throwable t) {\n'
            '      System.out.println("__construct0=EX:" '
            '+ t.getClass().getSimpleName());\n'
            '    }\n'
            f'    System.out.println("{END_MARKER}");\n'
            '  }\n}\n'
            + ('\n' + helpers + '\n' if helpers else ''))
    out = ascii_safe(out)
    if out.count('{') != out.count('}'):
        raise ValueError('twin driver braces unbalanced -- refusing to emit '
                         'invalid Java (caller discards with this reason)')
    return out


def java_literal(typ: str, printed: str) -> Optional[str]:
    """A Java literal reconstructing a twin-printed value, or None.

    `[1.0, 2.0]` (Arrays.toString) becomes `new double[]{1.0, 2.0}` etc.
    None for ABSENT/EX/unsupported -- the caller discards with a reason,
    never fabricates an input.
    """
    p = (printed or '').strip()
    if not p or p == 'ABSENT' or p.startswith('EX:') or p == 'null':
        return None
    base = typ.replace('[]', '').strip()
    if typ.endswith('[]'):
        if not (p.startswith('[') and p.endswith(']')):
            return None
        inner = p[1:-1].strip()
        return f'new {base}[]{{{inner}}}' if inner else f'new {base}[0]'
    if base == 'String':
        return _java_string(p)
    if base in ('double', 'float', 'int', 'long', 'boolean'):
        suffix = {'float': 'f', 'long': 'L'}.get(base, '')
        return p + suffix
    return None


def build_reference_call_driver(reference_class: str,
                                observable_names: List[str],
                                args_expr: str,
                                package: Optional[str] = None) -> str:
    """Call each `compute_<name>` ONCE with the shared resolved arguments.

    One state (the test's), many observables -- keyed by observable name so
    the screen's count stays an observable count.
    """
    if isinstance(observable_names, str):
        raise TypeError('observable_names must be a list, not str')
    return build_driver(reference_class, observable_names, [args_expr],
                        package=package)


def run_twin(builder, project_dir: str, twin_source: str,
             timeout_seconds: int = 60,
             work_subdir: str = 'reference_twin') -> Tuple[
                 Optional[Dict[str, List[str]]], str]:
    """Compile and run the state twin on ONE build. `(all_values, reason)`.

    Returns every printed key INCLUDING the `__` bookkeeping (the caller
    needs `__construct0` and `__param_*`); observables for screening must be
    filtered by the caller. None on any failure -- fails closed.
    """
    try:
        drv = builder.build(twin_source, project_dir,
                            output_subdir=work_subdir)
    except Exception as e:                       # pragma: no cover - defensive
        return None, f'twin compile raised: {type(e).__name__}: {e}'
    if not getattr(drv, 'compiled', False):
        return None, 'twin did not compile — DISCARDED'
    cp = getattr(drv, 'classpath', '') or ''
    cls = getattr(drv, 'class_name', 'StateTwinDriver')
    try:
        p = subprocess.run(['java', '-cp', cp, cls],
                           capture_output=True, text=True,
                           timeout=timeout_seconds,
                           cwd=os.path.dirname(getattr(drv, 'harness_path', '')
                                               or project_dir))
    except subprocess.TimeoutExpired:
        return None, f'twin run timed out after {timeout_seconds}s'
    except Exception as e:                       # pragma: no cover - defensive
        return None, f'twin run raised: {type(e).__name__}: {e}'
    out = (p.stdout or '') + '\n' + (p.stderr or '')
    if END_MARKER not in out:
        return None, (f'twin run did not complete (no end marker; exit '
                      f'{p.returncode}) — DISCARDED')
    vals = observed_values(out.split(END_MARKER)[0])
    if vals.get('__construct0', ['?'])[0] != 'OK':
        return None, ('twin setup failed: __construct0='
                      + vals.get('__construct0', ['absent'])[0]
                      + ' — bad twin, not semantic disagreement')
    return vals, f'twin ran; {len(vals)} key(s)'
