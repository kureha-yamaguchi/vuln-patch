"""Mechanically screen synthesized relations on the BUGGY build before any
of them may enter a harness prompt.

Why this screen, and why the buggy build: an LLM-proposed relation is a
HYPOTHESIS. Injecting unscreened hypotheses labelled "trusted" reproduced
the diff-targeting failure mode (invented out-of-domain oracles firing on
correct patches). A sound reference implementation to validate against does
not exist without cheating (the developer fix is off-limits; sibling APR
patches are unlabelled and their overfit modes cluster, so cross-patch
consensus preferentially kills the most discriminating oracles). What we DO
have, for free, is the buggy checkout — which is correct almost everywhere:
it passes its whole test suite except the trigger tests, and bugs are
localized. That yields a cheap mechanical signal:

  * A relation that fires on a LARGE fraction of random valid inputs on the
    buggy build is out-of-domain — it contradicts behaviour we know is
    mostly correct, so it would flag almost any implementation. DROP.
  * A relation that fires on a SMALL fraction is consistent with detecting
    the actual defect region (the buggy build IS wrong somewhere). KEEP,
    ranked first.
  * A relation that never fires is consistent with everything we can check;
    it can still catch a patch-introduced regression. KEEP, ranked last.

This is a necessary filter, not a soundness proof — survivors are injected
as screened CANDIDATES (see prompts._synthesized_relations_block), never as
validated ground truth.

Mechanics: each candidate's check body is wrapped in a COUNTING harness (the
body's violation throw is caught and tallied, never surfaced to Jazzer), the
harness is compiled with the existing HarnessBuilder against the buggy
classpath, and run for a fixed `-runs` budget. The shutdown hook prints
`[relscreen] checked=N violated=M`, which we parse. Anything that fails to
compile or run is dropped — a relation we cannot execute is a relation we
cannot screen, and unscreened means uninjected.
"""
import os
import re
from typing import List, Optional

from build import HarnessBuilder
from fuzz_runner import run_jazzer

_STATS_RE = re.compile(
    r'\[relscreen\]\s+checked=(\d+)\s+violated=(\d+)')

# Fire ratio above which a relation is out-of-domain: the buggy build is
# known-correct on the overwhelming majority of inputs (it passes its whole
# suite bar the triggers), so a relation violated this often contradicts
# known-good behaviour rather than detecting the localized defect.
MAX_FIRE_RATIO = 0.20
# Below this many executed checks the run tells us nothing (e.g. every
# input was rejected by the body's own try/catch fences).
MIN_CHECKED = 100


def _screen_harness_source(package: Optional[str],
                           imports: List[str],
                           class_name: str,
                           check_body: str) -> str:
    """A counting wrapper around one relation check.

    The check body is written (per relation_synth's instructions) to run
    inside fuzzerTestOneInput, draw inputs from `data`, catch-and-return on
    API exceptions, and `throw new RuntimeException("relation ... violated
    ...")` on disagreement. Here that throw is CAUGHT and counted instead of
    surfacing, so Jazzer never stops on it and one `-runs` budget measures
    the fire RATIO. `return` inside the body is legal (it returns from
    runCheck, and the counting already happened)."""
    lines = []
    if package:
        lines.append(f'package {package};')
    lines.append(
        'import com.code_intelligence.jazzer.api.FuzzedDataProvider;')
    for imp in imports or []:
        imp = imp.strip()
        if imp and imp not in lines:
            lines.append(imp if imp.endswith(';') else imp + ';')
    lines += [
        '',
        f'public class {class_name} ' + '{',
        '    static long checked = 0, violated = 0;',
        '    static {',
        '        Runtime.getRuntime().addShutdownHook(new Thread(() ->',
        '            System.err.println("[relscreen] checked=" + checked',
        '                + " violated=" + violated)));',
        '    }',
        '',
        '    private static void runCheck(FuzzedDataProvider data)'
        ' throws Exception {',
        check_body,
        '    }',
        '',
        '    public static void fuzzerTestOneInput(FuzzedDataProvider data)'
        ' {',
        '        checked++;',
        '        try {',
        '            runCheck(data);',
        '        } catch (RuntimeException e) {',
        '            String m = String.valueOf(e.getMessage());',
        '            if (m.contains("violated")) { violated++; }',
        '            // other runtime exceptions: input rejection the body',
        '            // failed to fence — not a violation, not counted',
        '        } catch (Throwable t) {',
        '            // rejection/linkage noise — not a violation',
        '        }',
        '    }',
        '}',
    ]
    return '\n'.join(lines)


def screen_relations(candidates: List,
                     builder: HarnessBuilder,
                     buggy_dir: str,
                     jazzer_standalone_jar: str,
                     package: Optional[str] = None,
                     imports: Optional[List[str]] = None,
                     jazzer_api_jar: Optional[str] = None,
                     runs: int = 20000,
                     timeout_seconds: int = 45,
                     max_keep: int = 3) -> List:
    """Screen each candidate on the buggy build; return the survivors
    (ranked: selective-firing first, silent second), capped at `max_keep`
    so the prompt is never flooded. Each survivor's `screen_note` records
    what the screen observed. Fails soft per candidate — a candidate that
    cannot be compiled/run is dropped with a printed reason, never injected
    unscreened."""
    selective, silent = [], []
    for i, rel in enumerate(candidates):
        name = getattr(rel, 'name', f'relation{i}')
        cls = f'RelScreen{i}'
        src = _screen_harness_source(package, imports or [], cls,
                                     getattr(rel, 'check', ''))
        try:
            build = builder.build(src, buggy_dir,
                                  output_subdir=f'relscreen_{i}')
        except Exception as exc:
            print(f"  [screen] {name}: build error ({exc}) — dropped")
            continue
        if not build.compiled:
            reason = (build.stderr or '').strip().splitlines()
            print(f"  [screen] {name}: does not compile — dropped"
                  + (f" ({reason[0]})" if reason else ""))
            continue
        try:
            outcome = run_jazzer(
                jazzer_standalone_jar=jazzer_standalone_jar,
                target_class=build.class_name,
                harness_dir=os.path.dirname(build.harness_path),
                project_cp=builder.test_classpath(buggy_dir),
                timeout_seconds=timeout_seconds,
                jazzer_api_jar=jazzer_api_jar,
                # Fixed execution budget (last flag wins in libFuzzer), so
                # the ratio denominator is comparable across candidates.
                extra_libfuzzer_args=[f'-runs={runs}'],
            )
        except Exception as exc:
            print(f"  [screen] {name}: run error ({exc}) — dropped")
            continue
        m = _STATS_RE.search(outcome.stdout + '\n' + outcome.stderr)
        if not m:
            print(f"  [screen] {name}: no stats line (harness crashed the "
                  f"JVM or never ran) — dropped")
            continue
        checked, violated = int(m.group(1)), int(m.group(2))
        if checked < MIN_CHECKED:
            print(f"  [screen] {name}: only {checked} checks executed — "
                  f"dropped (cannot screen)")
            continue
        ratio = violated / checked
        if ratio > MAX_FIRE_RATIO:
            print(f"  [screen] {name}: fired on {violated}/{checked} "
                  f"({ratio:.0%}) buggy-build inputs — out-of-domain, "
                  f"dropped")
            continue
        note = (f"fired on {violated}/{checked} fuzzed inputs on the "
                f"buggy build" + (" (selective — consistent with the "
                                  "defect region)" if violated else
                                  " (silent on the buggy build)"))
        try:
            rel.screen_note = note
        except Exception:
            pass
        print(f"  [screen] {name}: KEPT — {note}")
        (selective if violated else silent).append(rel)
    survivors = (selective + silent)[:max_keep]
    dropped_by_cap = (selective + silent)[max_keep:]
    if dropped_by_cap:
        # Not a screening failure — just the prompt-size cap. Say so, or
        # "4 KEPT" followed by "3 survived" reads as an off-by-one.
        print(f"  [screen] cap: injecting the top {max_keep}; "
              + ", ".join(getattr(r, 'name', '?') for r in dropped_by_cap)
              + " kept-but-cut (selective firers rank first)")
    return survivors
