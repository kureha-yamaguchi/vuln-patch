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
    class executed on the SAME constructed states. That is admissible: the buggy
    build is authority rank 2 whether its values are archived or produced now.
    Bounded cost -- one class, no fuzzing loop.

    `construct` is a Java expression building the object from a vector, with
    `{vec}` substituted; observables are read as no-arg getters, keyed the same
    way as the reference driver so the two dictionaries compare key-for-key.
    """
    pkg = f'package {package};\n\n' if package else ''
    calls = []
    for obs in observables:
        for vec in vectors:
            calls.append(
                '    try {\n'
                f'      {fq_class} o = {construct.replace("{vec}", vec)};\n'
                f'      Object r = o.{obs}();\n'
                f'      System.out.println("{obs}=" + String.valueOf(r));\n'
                '    } catch (Throwable t) {\n'
                f'      System.out.println("{obs}=EX:" '
                '+ t.getClass().getSimpleName());\n'
                '    }')
    return (pkg + 'public class BuggyTwinDriver {\n'
            '  public static void main(String[] args) {\n'
            + '\n'.join(calls) + '\n'
            f'    System.out.println("{END_MARKER}");\n'
            '  }\n}\n')


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
    if not obs:
        return None, 'reference produced no parseable observables — DISCARDED'
    return obs, f'reference ran and produced {len(obs)} observable(s)'
