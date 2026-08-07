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
    pkg = f'package {package};\n\n' if package else ''
    calls = []
    for obs in observables:
        for j, vec in enumerate(vectors):
            fn = call.format(obs=obs)
            calls.append(
                '    try {\n'
                f'      Object r = {reference_class}.{fn}({vec});\n'
                f'      System.out.println("{obs}=" + String.valueOf(r));\n'
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
            f'        Object r = o.{obs}();\n'
            f'        System.out.println("{obs}=" + String.valueOf(r));\n'
            '      } catch (Throwable t) {\n'
            f'        System.out.println("{obs}=EX:" '
            '+ t.getClass().getSimpleName());\n'
            '      }'
            for obs in observables)
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
