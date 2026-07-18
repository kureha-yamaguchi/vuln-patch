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
import tempfile
from typing import List, Optional

from build import HarnessBuilder
from fuzz_runner import run_jazzer
from java_source import (library_subclass, negative_modulo_index,
                         violation_swallowed)

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


def _measure_on_corpus(build, builder, buggy_dir, jazzer_standalone_jar,
                       jazzer_api_jar, literals, timeout_seconds=25):
    """Run one already-compiled counting harness over EXACTLY the given
    input literals (libFuzzer `-runs=0` replays the corpus and exits —
    no mutation), and return (checked, violated) or None on failure.

    P2.2: seeding with the failing test's own trigger literals asks the
    direction question — does the relation FIRE on the very inputs the
    bug is about? A relation that fires there disagrees with the buggy
    behaviour where the test pins the truth (direction-confirmed); one
    that stays silent there while the general run fires is suspect."""
    if not literals:
        return None
    corpus = tempfile.mkdtemp(prefix='reldir_')
    try:
        for i, lit in enumerate(literals):
            with open(os.path.join(corpus, f'seed_{i:03d}'), 'w',
                      encoding='utf-8', errors='replace') as fh:
                fh.write(lit)
        outcome = run_jazzer(
            jazzer_standalone_jar=jazzer_standalone_jar,
            target_class=build.class_name,
            harness_dir=os.path.dirname(build.harness_path),
            project_cp=builder.test_classpath(buggy_dir),
            timeout_seconds=timeout_seconds,
            jazzer_api_jar=jazzer_api_jar,
            corpus_dir=corpus,
            extra_libfuzzer_args=['-runs=0'],
        )
    except Exception:
        return None
    finally:
        import shutil
        shutil.rmtree(corpus, ignore_errors=True)
    m = _STATS_RE.search(outcome.stdout + '\n' + outcome.stderr)
    if not m:
        return None
    return int(m.group(1)), int(m.group(2))


def screen_relations(candidates: List,
                     builder: HarnessBuilder,
                     buggy_dir: str,
                     jazzer_standalone_jar: str,
                     package: Optional[str] = None,
                     imports: Optional[List[str]] = None,
                     jazzer_api_jar: Optional[str] = None,
                     trigger_literals: Optional[List[str]] = None,
                     runs: int = 20000,
                     timeout_seconds: int = 45,
                     max_keep: int = 3,
                     repair_fn=None) -> List:
    """Screen each candidate on the buggy build; return the survivors
    (ranked: selective-firing first, silent second), capped at `max_keep`
    so the prompt is never flooded. Each survivor's `screen_note` records
    what the screen observed. Fails soft per candidate — a candidate that
    cannot be compiled/run is dropped with a printed reason, never injected
    unscreened."""
    confirmed, confirmed_flaky, selective, silent, high_ratio = (
        [], [], [], [], [])
    for i, rel in enumerate(candidates):
        name = getattr(rel, 'name', f'relation{i}')
        # Format gate: the counting harness recognises a violation ONLY by
        # the mandated `throw new RuntimeException("... violated ...")`
        # (synthesis instructions require that literal). A check that
        # throws in any other shape would fuzz as "silent" and evade the
        # fire-ratio rule entirely — reject it here, before spending a
        # compile, instead of counting what it can't see.
        if 'violated' not in getattr(rel, 'check', ''):
            print(f"  [screen] {name}: check never throws the mandated "
                  f"'violated' message — format-rejected, dropped")
            continue
        # P0.2 self-swallow lint: an alarm thrown inside the check's own
        # catch-everything block is caught and discarded before the
        # counting wrapper can see it — the check then measures 0/20,000
        # on buggy and gets promoted as "well-behaved" (every Lang-7-run
        # candidate had this). Reject BEFORE spending a compile.
        swallow_reason = violation_swallowed(getattr(rel, 'check', ''))
        if swallow_reason is not None:
            print(f"  [screen] {name}: SELF-SWALLOWED ALARM — "
                  f"{swallow_reason} — dropped")
            continue
        # P2.3 constraint parity: the harness may not subclass a library
        # class to force behaviour. A relation that needs one compiles
        # here but the harness can't implement it — the model then
        # silently drops it (Math-2's MIN_VALUE-support relation). Reject
        # at the screen so the miss is visible, not silent.
        subclassed = library_subclass(getattr(rel, 'check', ''))
        if subclassed is not None:
            print(f"  [screen] {name}: needs an anonymous/local subclass of "
                  f"`{subclassed}` — forbidden in harnesses (use a real "
                  f"library subclass instead) — dropped")
            continue
        # Harness-bug lint (same as campaign gate 0d): an index computed as
        # Math.abs(consume…()) % n goes negative on Integer.MIN_VALUE and
        # the check crashes on its own input handling.
        negmod = negative_modulo_index(getattr(rel, 'check', ''))
        if negmod is not None:
            print(f"  [screen] {name}: NEGATIVE-MODULO INDEX — {negmod} — "
                  f"dropped")
            continue
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
            # R1: one repair attempt before giving up — a compile error is
            # otherwise death, and ~22% of candidates die here, many on a
            # fixable typo (wrong import/type/method) rather than a bad idea.
            repaired = None
            if repair_fn is not None:
                repaired = repair_fn(rel, build.stderr or '')
            if repaired:
                try:
                    rel.check = repaired
                except Exception:
                    pass
                src = _screen_harness_source(package, imports or [], cls,
                                             repaired)
                try:
                    build = builder.build(
                        src, buggy_dir, output_subdir=f'relscreen_{i}r')
                except Exception:
                    build = None
            if not build or not build.compiled:
                print(f"  [screen] {name}: does not compile — dropped"
                      + (" (repair failed)" if repair_fn is not None else "")
                      + (f" ({reason[0]})" if reason else ""))
                continue
            print(f"  [screen] {name}: compile-repaired (R1)")
        # P0.2 canary: compile+run a variant FORCED to raise its alarm and
        # require the counting wrapper to register it. The lint above
        # proves the alarm escapes the check's own catches statically;
        # this proves the whole counting plumbing can hear it end to end.
        # (`if (data != null)` — always true, but the compiler can't
        # prove it, so no unreachable-code error.)
        canary_body = ('if (data != null) throw new RuntimeException('
                       '"canary relation violated");\n'
                       + getattr(rel, 'check', ''))
        canary_src = _screen_harness_source(package, imports or [],
                                            f'{cls}Canary', canary_body)
        try:
            canary_build = builder.build(canary_src, buggy_dir,
                                         output_subdir=f'relscreen_{i}c')
            canary_heard = False
            if canary_build.compiled:
                canary_out = run_jazzer(
                    jazzer_standalone_jar=jazzer_standalone_jar,
                    target_class=canary_build.class_name,
                    harness_dir=os.path.dirname(canary_build.harness_path),
                    project_cp=builder.test_classpath(buggy_dir),
                    timeout_seconds=20,
                    jazzer_api_jar=jazzer_api_jar,
                    extra_libfuzzer_args=['-runs=50'],
                )
                cm = _STATS_RE.search(canary_out.stdout + '\n'
                                      + canary_out.stderr)
                canary_heard = bool(cm and int(cm.group(2)) >= 1)
        except Exception as exc:
            print(f"  [screen] {name}: canary error ({exc}) — dropped")
            continue
        if not canary_heard:
            print(f"  [screen] {name}: CANARY FAILED — a forced alarm was "
                  f"not registered by the counting wrapper; this "
                  f"candidate's violations cannot be measured — dropped")
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

        # --- P2.2: direction + determinism on the trigger inputs --------
        # Two focused replays over EXACTLY the failing test's literals.
        # (a) does the relation fire there? Firing = it disagrees with the
        #     buggy behaviour where the test pins the truth =
        #     DIRECTION-CONFIRMED. (b) do the two replays AGREE? A relation
        #     built on a nondeterministic call (Math-2's sample()) fires a
        #     different number of times each run — FLAKY, deprioritise it
        #     in favour of a deterministic discriminator.
        direction = 'uncovered'   # uncovered | confirmed | inverted
        deterministic = True
        if trigger_literals:
            r1 = _measure_on_corpus(build, builder, buggy_dir,
                                    jazzer_standalone_jar, jazzer_api_jar,
                                    trigger_literals)
            r2 = _measure_on_corpus(build, builder, buggy_dir,
                                    jazzer_standalone_jar, jazzer_api_jar,
                                    trigger_literals)
            if r1 and r2:
                v1, v2 = r1[1], r2[1]
                deterministic = (v1 == v2)
                if v1 > 0 and v2 > 0:
                    direction = 'confirmed'
                elif v1 == 0 and v2 == 0 and ratio > MAX_FIRE_RATIO:
                    # Silent on the trigger inputs yet firing on most
                    # random inputs → it is asserting the WRONG direction
                    # exactly where the test pins the right one.
                    direction = 'inverted'

        if direction == 'inverted':
            # DEMOTED, not dropped (changed after minfix_w2c): "silent on
            # the trigger corpus, loud on random inputs" has two readings —
            # a backwards rule (Lang-7) OR a sound rule whose violation
            # domain the tiny byte-corpus simply never reaches (Math-2's
            # mean-formula fires only at overflow-scale parameters; the
            # hard drop deleted the single best discriminator we have).
            # The two are mechanically indistinguishable here, so: keep it
            # OUT of the harness prompt (run.py filters on this marker)
            # but keep it for pool/replay, where the patched-side replay
            # plus the verifier — who is told about the inversion suspicion
            # via this note — decide.
            note = (f"INVERTED-SUSPECT: silent on the failing test's own "
                    f"input corpus while firing on {ratio:.0%} of random "
                    f"inputs on buggy. EITHER a backwards rule (asserting "
                    f"the buggy direction) OR a sound rule whose violation "
                    f"domain the trigger corpus never reaches — replay-only,"
                    f" never prompt-injected; treat a patched-build firing "
                    f"with scepticism about direction")
            try:
                rel.screen_note = note
            except Exception:
                pass
            print(f"  [screen] {name}: {note}")
            high_ratio.append(rel)
            continue

        flag = '' if deterministic else ' [FLAKY — nondeterministic check]'
        if direction == 'confirmed':
            # The reliable, correctly-aimed case: fires exactly where the
            # failing test says the buggy code is wrong. Rank first and
            # EXEMPT from the ratio cap (a correct check aimed at the bug
            # legitimately fires on almost every input once P0.2 stopped
            # swallowing its alarm).
            note = (f"DIRECTION-CONFIRMED: fires on the failing test's own "
                    f"inputs (buggy violates the pinned behaviour there); "
                    f"{violated}/{checked} on random inputs{flag}")
            try:
                rel.screen_note = note
            except Exception:
                pass
            print(f"  [screen] {name}: KEPT (direction-confirmed){flag} — "
                  f"{note}")
            (confirmed_flaky if not deterministic
             else confirmed).append(rel)
            continue

        if ratio > MAX_FIRE_RATIO:
            # Above the cap but direction NOT confirmed on the trigger
            # inputs — keep provisionally, ranked behind confirmed and
            # selective, and say so loudly.
            note = (f"fired on {violated}/{checked} ({ratio:.0%}) buggy "
                    f"inputs — ABOVE the {MAX_FIRE_RATIO:.0%} cap and NOT "
                    f"direction-confirmed on the trigger inputs; kept "
                    f"provisionally, treat with suspicion{flag}")
            try:
                rel.screen_note = note
            except Exception:
                pass
            print(f"  [screen] {name}: RATIO-CAP — {note}")
            high_ratio.append(rel)
            continue
        note = (f"fired on {violated}/{checked} fuzzed inputs on the "
                f"buggy build" + (" (selective — consistent with the "
                                  "defect region)" if violated else
                                  " (silent on the buggy build)") + flag)
        try:
            rel.screen_note = note
        except Exception:
            pass
        print(f"  [screen] {name}: KEPT — {note}")
        (selective if violated else silent).append(rel)
    # Ranking (best first): direction-confirmed & deterministic, then
    # direction-confirmed but flaky (a lucky discriminator beats none),
    # then selective firers, then above-cap provisional, then ONE silent
    # tripwire. Silent relations fired on nothing the buggy build does —
    # past the first, each adds prompt mass without new evidence and
    # measurably distracts the generator (mined54: Lang-7 TP->FN under 36
    # mined assertions).
    kept = (confirmed + confirmed_flaky + selective
            + high_ratio + silent[:1])[:max_keep]
    dropped_by_cap = [r for r in confirmed + confirmed_flaky + selective
                      + high_ratio + silent if r not in kept]
    if dropped_by_cap:
        # Not a screening failure — the prompt-size/distraction cap. Say
        # so, or "4 KEPT" followed by "3 survived" reads as an off-by-one.
        print(f"  [screen] cap: injecting {len(kept)} "
              f"(direction-confirmed first, at most 1 silent); "
              + ", ".join(getattr(r, 'name', '?') for r in dropped_by_cap)
              + " kept-but-cut")
    return kept


def replay_on_patched(relations: List,
                      builder: HarnessBuilder,
                      patched_dir: str,
                      jazzer_standalone_jar: str,
                      package: Optional[str] = None,
                      imports: Optional[List[str]] = None,
                      jazzer_api_jar: Optional[str] = None,
                      trigger_literals: Optional[List[str]] = None,
                      runs: int = 20000,
                      timeout_seconds: int = 45) -> List[dict]:
    """P3.2 replay: execute already-screened relations DIRECTLY against the
    patched build — no harness generation, no fuzzing luck in between.

    Why: a screened relation used to matter only if the harness writer
    chose to implement it AND the patched-build fuzz found the right
    inputs. Math-2's probe-validated mean-formula discriminator was lost
    to exactly those two coin flips. Here each relation gets the same
    counting wrapper the screen used, compiled against the PATCHED
    checkout, and measured two ways:

    - trigger tier: replay EXACTLY the failing test's own input literals
      (-runs=0, twice — determinism check). The failing test is the most
      trusted evidence source; an overfit only guarantees the test's own
      ASSERTIONS pass, not that other contract relations hold at those
      inputs (Math-2/Arja: getNumericalMean is still -49.76 at the
      trigger params although sample() passes).
    - fuzzed tier: the same `runs` budget the buggy-side screen used.

    Returns finding dicts (relation, tier, counts, note) for every
    relation that fires; the CALLER must pass each through the LLM
    verifier before it may influence any verdict — replay never convicts
    on its own."""
    findings = []
    for i, rel in enumerate(relations):
        name = getattr(rel, 'name', f'relation{i}')
        check = getattr(rel, 'check', '')
        if not check:
            continue
        cls = f'RelReplay{i}'
        src = _screen_harness_source(package, imports or [], cls, check)
        try:
            build = builder.build(src, patched_dir,
                                  output_subdir=f'relreplay_{i}')
        except Exception as exc:
            print(f"  [replay] {name}: build error ({exc}) — skipped")
            continue
        if not build.compiled:
            print(f"  [replay] {name}: does not compile against the "
                  f"patched build — skipped")
            continue

        trig_v = None                     # violated count on trigger inputs
        trig_deterministic = False
        if trigger_literals:
            r1 = _measure_on_corpus(build, builder, patched_dir,
                                    jazzer_standalone_jar, jazzer_api_jar,
                                    trigger_literals)
            r2 = _measure_on_corpus(build, builder, patched_dir,
                                    jazzer_standalone_jar, jazzer_api_jar,
                                    trigger_literals)
            if r1 and r2:
                trig_v = max(r1[1], r2[1])
                trig_deterministic = (r1[1] == r2[1])

        checked = violated = None
        try:
            outcome = run_jazzer(
                jazzer_standalone_jar=jazzer_standalone_jar,
                target_class=build.class_name,
                harness_dir=os.path.dirname(build.harness_path),
                project_cp=builder.test_classpath(patched_dir),
                timeout_seconds=timeout_seconds,
                jazzer_api_jar=jazzer_api_jar,
                extra_libfuzzer_args=[f'-runs={runs}'],
            )
            m = _STATS_RE.search(outcome.stdout + '\n' + outcome.stderr)
            if m:
                checked, violated = int(m.group(1)), int(m.group(2))
        except Exception as exc:
            print(f"  [replay] {name}: fuzzed replay failed ({exc})")

        if trig_v:
            tier = 'trigger'
            note = (f"fires on the failing test's OWN input literals on "
                    f"the patched build"
                    + (" (deterministic, 2/2 replays)" if trig_deterministic
                       else " (NONDETERMINISTIC across replays — flaky)")
                    + (f"; fuzzed: {violated}/{checked}"
                       if checked is not None else ""))
        elif violated:
            tier = 'fuzzed'
            note = (f"fires on {violated}/{checked} fuzzed inputs on the "
                    f"patched build (silent on the trigger literals)")
        else:
            if checked is not None or trig_v is not None:
                print(f"  [replay] {name}: quiet on the patched build"
                      + (f" ({checked} fuzzed inputs)" if checked else ""))
            continue
        print(f"  [replay] {name}: FIRED [{tier}] — {note}")
        findings.append({
            'relation': rel, 'name': name, 'tier': tier, 'note': note,
            'trigger_violations': trig_v,
            'trigger_deterministic': trig_deterministic,
            'fuzz_checked': checked, 'fuzz_violated': violated,
        })
    return findings
