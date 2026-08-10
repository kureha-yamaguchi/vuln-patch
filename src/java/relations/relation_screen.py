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
import shutil
import tempfile
from typing import List, Optional

from java.harness.build import HarnessBuilder
from llm import record_event
from java.execution.fuzz_runner import run_jazzer
from java.parsing.java_source import (anchors_buggy_value, boolean_swallow,
                         constant_receiver_state,
                         library_subclass, negative_modulo_index,
                         guard_llm_tier2,
                         patched_probe_swallowed, probe_before_last_mutation,
                         rethrow_patched_probe, violation_swallowed)
# Fire ratio above which a relation is out-of-domain: the buggy build is
# known-correct on the overwhelming majority of inputs (it passes its whole
# suite bar the triggers), so a relation violated this often contradicts
# known-good behaviour rather than detecting the localized defect. Defined in
# evidence_facts (the pure fact module) and imported here so the two sites that
# reason about it share one number — see evidence_facts for the direction
# rationale (no import cycle; keeps evidence_facts JVM-free for unit tests).
from java.relations.evidence_facts import MAX_FIRE_RATIO

_STATS_RE = re.compile(
    r'\[relscreen\]\s+checked=(\d+)\s+violated=(\d+)')
# Below this many executed checks the run tells us nothing (e.g. every
# input was rejected by the body's own try/catch fences).
MIN_CHECKED = 100


#: 8.4x p1a — a recording FuzzedDataProvider. Every consumed value is
#: logged in consumption order, so a FIRING can print the exact inputs it
#: fired on. Delegation is total (the full interface), logging is capped,
#: and the log resets per input. Formatting matches the pipeline's value
#: conventions (String.valueOf scalars, Arrays.toString arrays).
_RECORDING_FDP = r'''
    static final class RecFDP implements FuzzedDataProvider {
        private final FuzzedDataProvider d;
        private final StringBuilder log = new StringBuilder();
        RecFDP(FuzzedDataProvider d) { this.d = d; }
        void reset() { log.setLength(0); }
        String consumed() { return log.toString(); }
        private <T> T rec(T v) {
            // invdiv read 817858f: positional unlabelled entries made
            // `||3|5||` unreadable (empty strings invisible). Every entry
            // now carries a type tag, and strings are quoted.
            if (log.length() < 1500) {
                if (log.length() > 0) log.append('|');
                String s;
                if (v instanceof boolean[]) s = "za:" + java.util.Arrays.toString((boolean[]) v);
                else if (v instanceof byte[]) s = "ba:" + java.util.Arrays.toString((byte[]) v);
                else if (v instanceof short[]) s = "ha:" + java.util.Arrays.toString((short[]) v);
                else if (v instanceof int[]) s = "ia:" + java.util.Arrays.toString((int[]) v);
                else if (v instanceof long[]) s = "la:" + java.util.Arrays.toString((long[]) v);
                else if (v instanceof Boolean) s = "z:" + v;
                else if (v instanceof Byte) s = "b:" + v;
                else if (v instanceof Short) s = "h:" + v;
                else if (v instanceof Integer) s = "i:" + v;
                else if (v instanceof Long) s = "l:" + v;
                else if (v instanceof Float) s = "f:" + v;
                else if (v instanceof Double) s = "d:" + v;
                else if (v instanceof Character) s = "c:" + v;
                else if (v instanceof String) s = "q:\"" + v + "\"";
                else s = "o:" + String.valueOf(v);
                log.append(s, 0, Math.min(s.length(), 300));
            }
            return v;
        }
        public boolean consumeBoolean() { return rec(d.consumeBoolean()); }
        public boolean[] consumeBooleans(int n) { return rec(d.consumeBooleans(n)); }
        public byte consumeByte() { return rec(d.consumeByte()); }
        public byte consumeByte(byte lo, byte hi) { return rec(d.consumeByte(lo, hi)); }
        public byte[] consumeBytes(int n) { return rec(d.consumeBytes(n)); }
        public byte[] consumeRemainingAsBytes() { return rec(d.consumeRemainingAsBytes()); }
        public short consumeShort() { return rec(d.consumeShort()); }
        public short consumeShort(short lo, short hi) { return rec(d.consumeShort(lo, hi)); }
        public short[] consumeShorts(int n) { return rec(d.consumeShorts(n)); }
        public int consumeInt() { return rec(d.consumeInt()); }
        public int consumeInt(int lo, int hi) { return rec(d.consumeInt(lo, hi)); }
        public int[] consumeInts(int n) { return rec(d.consumeInts(n)); }
        public long consumeLong() { return rec(d.consumeLong()); }
        public long consumeLong(long lo, long hi) { return rec(d.consumeLong(lo, hi)); }
        public long[] consumeLongs(int n) { return rec(d.consumeLongs(n)); }
        public float consumeFloat() { return rec(d.consumeFloat()); }
        public float consumeRegularFloat() { return rec(d.consumeRegularFloat()); }
        public float consumeRegularFloat(float lo, float hi) { return rec(d.consumeRegularFloat(lo, hi)); }
        public float consumeProbabilityFloat() { return rec(d.consumeProbabilityFloat()); }
        public double consumeDouble() { return rec(d.consumeDouble()); }
        public double consumeRegularDouble() { return rec(d.consumeRegularDouble()); }
        public double consumeRegularDouble(double lo, double hi) { return rec(d.consumeRegularDouble(lo, hi)); }
        public double consumeProbabilityDouble() { return rec(d.consumeProbabilityDouble()); }
        public char consumeChar() { return rec(d.consumeChar()); }
        public char consumeChar(char lo, char hi) { return rec(d.consumeChar(lo, hi)); }
        public char consumeCharNoSurrogates() { return rec(d.consumeCharNoSurrogates()); }
        public String consumeString(int n) { return rec(d.consumeString(n)); }
        public String consumeRemainingAsString() { return rec(d.consumeRemainingAsString()); }
        public String consumeAsciiString(int n) { return rec(d.consumeAsciiString(n)); }
        public String consumeRemainingAsAsciiString() { return rec(d.consumeRemainingAsAsciiString()); }
        public <T> T pickValue(T[] a) { return rec(d.pickValue(a)); }
        public <T> T pickValue(java.util.Collection<T> c) { return rec(d.pickValue(c)); }
        public <T> java.util.List<T> pickValues(T[] a, int n) { return rec(d.pickValues(a, n)); }
        public <T> java.util.List<T> pickValues(java.util.Collection<T> c, int n) { return rec(d.pickValues(c, n)); }
        public int remainingBytes() { return d.remainingBytes(); }
    }
'''


#: 8.4x recording half, part 2: on a firing, print every captured
#: receiver's primitive/array state fields via reflection — the exact
#: inputs a reference implementation needs, formatted by the same
#: conventions as the twin (String.valueOf scalars, deepToString arrays).
_STATE_PRINTER = r'''
    static String __rcvState() {
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Object> en : __rcv.entrySet()) {
            Object o = en.getValue();
            if (o == null) continue;
            sb.append(" __rcvstate ").append(en.getKey()).append(':')
              .append(o.getClass().getSimpleName());
            Class<?> c = o.getClass();
            int printed = 0;
            while (c != null && c != Object.class && printed < 24) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (printed >= 24) break;
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                        continue;
                    Class<?> t = f.getType();
                    boolean ok = t.isPrimitive()
                        || (t.isArray() && (t.getComponentType().isPrimitive()
                            || (t.getComponentType().isArray()
                                && t.getComponentType().getComponentType()
                                     .isPrimitive())));
                    if (!ok) continue;
                    try {
                        f.setAccessible(true);
                        Object v = f.get(o);
                        String sv;
                        if (v == null) sv = "null";
                        else if (v instanceof double[]) sv = java.util.Arrays.toString((double[]) v);
                        else if (v instanceof int[]) sv = java.util.Arrays.toString((int[]) v);
                        else if (v instanceof long[]) sv = java.util.Arrays.toString((long[]) v);
                        else if (v instanceof float[]) sv = java.util.Arrays.toString((float[]) v);
                        else if (v instanceof boolean[]) sv = java.util.Arrays.toString((boolean[]) v);
                        else if (v instanceof short[]) sv = java.util.Arrays.toString((short[]) v);
                        else if (v instanceof byte[]) sv = java.util.Arrays.toString((byte[]) v);
                        else if (v.getClass().isArray()) sv = java.util.Arrays.deepToString((Object[]) v);
                        else sv = String.valueOf(v);
                        sb.append(' ').append(f.getName()).append('=')
                          .append(sv, 0, Math.min(sv.length(), 400));
                        printed++;
                    } catch (Throwable ignore) { }
                }
                c = c.getSuperclass();
            }
        }
        String out = sb.toString();
        return out.substring(0, Math.min(out.length(), 3000));
    }
'''


def capture_receivers(check_body: str) -> str:
    """Insert `__rcv.put("var", var);` after every locally-constructed
    object — mechanical, pattern-scoped, fail-closed (no match, no
    capture, no state print; the firing still carries its message and
    consumed inputs). The captured objects' state is what the reference
    needs to be evaluated AT THE FIRING (8.4x p1b's input).

    THE INVDIV DEFECT (read 817858f): zero `__rcvstate` across nine
    traces. The first pattern required `Type var = new Uppercase(...)`
    in one statement — production checks construct FULLY-QUALIFIED
    (`new org.apache.commons.math3...`, lowercase package first) and
    often SPLIT declaration from assignment (`Foo dist; ... dist = new
    Foo(...);` — the verbatim Math-2 shape). The smoke test used `new
    Demo(...)` and exercised neither. Seventh check-your-matcher
    instance. Now: the assignment form `var = new <qualified>(...)` is
    the anchor (it covers both shapes) and the qualified name may start
    with a package segment."""
    return re.sub(
        r'\b(\w+)(\s*=\s*new\s+[A-Za-z_][\w.]*\s*(?:<[^;()]*>)?\s*\([^;]*\)\s*;)',
        r'\1\2 __rcv.put("\1", \1);',
        check_body or '')


def _screen_harness_source(package: Optional[str],
                           imports: List[str],
                           class_name: str,
                           check_body: str,
                           record_firings: bool = False) -> str:
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
    ] + (['    static int firePrints = 0;',
          '    static final java.util.LinkedHashMap<String, Object> __rcv =',
          '        new java.util.LinkedHashMap<>();',
          _RECORDING_FDP, _STATE_PRINTER]
         if record_firings else []) + [
        '    static {',
        '        Runtime.getRuntime().addShutdownHook(new Thread(() ->',
        '            System.err.println("[relscreen] checked=" + checked',
        '                + " violated=" + violated)));',
        '    }',
        '',
        '    private static void runCheck(FuzzedDataProvider data)'
        ' throws Exception {',
        capture_receivers(check_body) if record_firings else check_body,
        '    }',
        '',
        '    public static void fuzzerTestOneInput(FuzzedDataProvider data)'
        ' {',
        '        checked++;',
    ] + ([
        '        RecFDP rec = new RecFDP(data);',
        '        data = rec;',
        '        __rcv.clear();',
    ] if record_firings else []) + [
        '        try {',
        '            runCheck(data);',
        '        } catch (RuntimeException e) {',
        '            String m = String.valueOf(e.getMessage());',
        '            // A relation check raises its alarm as',
        '            // RuntimeException("...violated..."); a harness oracle',
        '            // body (universal screen, measure_single_check) raises a',
        '            // FuzzerSecurityIssue*. Count either — relation checks',
        '            // never throw the latter, so per-relation screening is',
        '            // byte-for-byte unchanged in what it counts.',
        '            if (m.contains("violated")',
        '                    || e.getClass().getName().contains(',
        '                            "FuzzerSecurityIssue")) { violated++;',
    ] + ([
        '                // 8.4x p1a: a firing prints its own message AND the',
        '                // exact inputs it fired on. Capped: first 5 firings.',
        '                if (firePrints < 5) { firePrints++;',
        '                    System.err.println("[relfire] "',
        '                        + m.substring(0, Math.min(m.length(), 400))',
        '                        + " __consumed=" + ((RecFDP) data).consumed()',
        '                        + __rcvState());',
        '                }',
    ] if record_firings else []) + [
        '            }',
        '            // other runtime exceptions: input rejection the body',
        '            // failed to fence — not a violation, not counted',
        '        } catch (Throwable t) {',
        '            // rejection/linkage noise — not a violation, UNLESS it is',
        '            // a harness oracle alarm (universal screen); relation',
        '            // checks never reach here, so screening is unchanged.',
        '            if (t.getClass().getName().contains(',
        '                    "FuzzerSecurityIssue")) { violated++; }',
        '        }',
        '    }',
        '}',
    ]
    return '\n'.join(lines)


def _measure_on_corpus(build, builder, buggy_dir, jazzer_standalone_jar,
                       jazzer_api_jar, literals, timeout_seconds=25,
                       fired_out=None):
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
        return _measure_on_dir(build, builder, buggy_dir,
                               jazzer_standalone_jar, jazzer_api_jar,
                               corpus, timeout_seconds, fired_out=fired_out)
    finally:
        shutil.rmtree(corpus, ignore_errors=True)


def _measure_on_dir(build, builder, work_dir, jazzer_standalone_jar,
                    jazzer_api_jar, corpus_dir, timeout_seconds=25,
                    fired_out=None):
    """Replay one already-compiled counting harness over EXACTLY the seeds
    in `corpus_dir` (`-runs=0`, no mutation) against `work_dir`'s classpath,
    and return (checked, violated) or None on failure.

    `fired_out` (8.4x p1a): pass a list to collect the harness's
    `[relfire]` lines — a recording wrapper's firing messages carrying the
    thrown text AND the exact consumed inputs. Return shape unchanged."""
    try:
        outcome = run_jazzer(
            jazzer_standalone_jar=jazzer_standalone_jar,
            target_class=build.class_name,
            harness_dir=os.path.dirname(build.harness_path),
            project_cp=builder.test_classpath(work_dir),
            timeout_seconds=timeout_seconds,
            jazzer_api_jar=jazzer_api_jar,
            corpus_dir=corpus_dir,
            extra_libfuzzer_args=['-runs=0'],
        )
    except Exception:
        return None
    text = outcome.stdout + '\n' + outcome.stderr
    if fired_out is not None:
        fired_out.extend(harvest_relfire_lines(text))
    m = _STATS_RE.search(text)
    if not m:
        return None
    return int(m.group(1)), int(m.group(2))


def harvest_relfire_lines(text, cap=3):
    """First `cap` DISTINCT `[relfire] ...` lines from a run's output.

    Per-line width 6000: the mechanism-roll read (corpus 2feb27d) found
    the old 700-char harvest truncating mid-array — and since nothing
    else preserved the line, the capped copy was the ONLY copy. The Java
    side prints up to ~4.9k (400 message + 1.5k consumed + 3k state);
    6000 keeps the whole snapshot."""
    out = []
    for m in re.finditer(r'\[relfire\][^\n]{0,6000}', str(text or '')):
        line = m.group(0).strip()
        if line not in out:
            out.append(line)
        if len(out) >= cap:
            break
    return out


def _run_counting_fuzz(build, builder, work_dir, jazzer_standalone_jar,
                       jazzer_api_jar, runs, timeout_seconds):
    """Run ONE already-compiled counting harness for a fixed `-runs` budget
    (last flag wins in libFuzzer, so the ratio denominator is comparable)
    against `work_dir`'s classpath, and return (checked, violated) parsed
    from the `[relscreen]` shutdown line — or None when no stats line was
    produced (the harness crashed the JVM or never ran).

    Raises on a run failure so a caller can distinguish a run error from a
    missing stats line; the per-relation screen relies on that distinction
    for its two drop reasons, while measure_single_check collapses both to
    None (fail-open). This is the single compile-and-measure primitive shared
    by the per-relation screen path and the universal single-check helper."""
    outcome = run_jazzer(
        jazzer_standalone_jar=jazzer_standalone_jar,
        target_class=build.class_name,
        harness_dir=os.path.dirname(build.harness_path),
        project_cp=builder.test_classpath(work_dir),
        timeout_seconds=timeout_seconds,
        jazzer_api_jar=jazzer_api_jar,
        extra_libfuzzer_args=[f'-runs={runs}'],
    )
    m = _STATS_RE.search(outcome.stdout + '\n' + outcome.stderr)
    if not m:
        return None
    return int(m.group(1)), int(m.group(2))


def measure_single_check(check_source: str,
                         builder: HarnessBuilder,
                         buggy_dir: str,
                         jazzer_standalone_jar: str,
                         jazzer_api_jar: Optional[str] = None,
                         package: Optional[str] = None,
                         imports: Optional[List[str]] = None,
                         runs: int = 20000,
                         timeout_seconds: int = 45,
                         class_name: str = 'UScreen',
                         output_subdir: str = 'uscreen') -> Optional[tuple]:
    """Measure ONE check body on the buggy build with the SAME relscreen
    counting wrapper relations get, and return (checked, violated) — or None
    on any compile/run/parse failure (fail-open).

    Reuses the exact `_screen_harness_source` wrapper, the exact `-runs`
    invocation and the exact `[relscreen]` stats parsing the per-relation
    screen uses (via `_run_counting_fuzz`). `check_source` may be a relation
    check body (raises RuntimeException("...violated...")) OR a harness
    oracle's extracted body (raises a FuzzerSecurityIssue*); the counting
    wrapper recognises both, so a fresh harness invention can be screened on
    the buggy build exactly as a screened relation is. Spec M."""
    if not check_source:
        return None
    src = _screen_harness_source(package, imports or [], class_name,
                                 check_source)
    try:
        build = builder.build(src, buggy_dir, output_subdir=output_subdir)
    except Exception:
        return None
    if not build or not build.compiled:
        return None
    try:
        return _run_counting_fuzz(build, builder, buggy_dir,
                                  jazzer_standalone_jar, jazzer_api_jar,
                                  runs, timeout_seconds)
    except Exception:
        return None


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
                     repair_fn=None,
                     patched_classes: Optional[List[str]] = None,
                     documented_exceptions: Optional[dict] = None,
                     divergence_values: Optional[List[str]] = None,
                     ) -> List:
    """Screen each candidate on the buggy build; return the survivors
    (ranked: selective-firing first, silent second), capped at `max_keep`
    so the prompt is never flooded. Each survivor's `screen_note` records
    what the screen observed. Fails soft per candidate — a candidate that
    cannot be compiled/run is dropped with a printed reason, never injected
    unscreened.

    `patched_classes` (the classes the patch under analysis changed) arms
    the 8.35 Mechanism-A normalisation: a probe on one of those classes
    fenced behind a blanket catch-and-return has its outcome swallowed
    before the counting wrapper can see it, and is rewritten to re-throw.
    Empty/omitted, that step is inert and screening is byte-for-byte what
    it was.

    `divergence_values` (--divcap only, the buggy-side values of this leg's
    divergences) arms the anti-anchoring lint. Empty/omitted — which is
    every run that does not pass --divcap — that step is inert too."""
    confirmed, confirmed_flaky, selective, silent, high_ratio = (
        [], [], [], [], [])

    def _mark(r, outcome, reason):
        try:
            r.screen_decision = {'outcome': outcome, 'reason': reason}
        except Exception:
            pass
        record_event('deterministic', method='screen',
                     target=getattr(r, 'name', '?'), output=outcome,
                     reason=reason)

    def _set_note(r, note):
        """Record the screen note, appending any DEMOTION suffix the static
        lints attached to this candidate (they run before the measurement
        that decides the note, and must survive it)."""
        demotion = getattr(r, 'screen_demotion', '') or ''
        try:
            r.screen_note = note + demotion
        except Exception:
            pass
        return note + demotion

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
            _mark(rel, 'dropped', 'format: never throws the mandated '
                  '"violated" message')
            continue
        # 8.35 Mechanism A — reportable patched-only exceptions. A probe on
        # the PATCH-CHANGED class wrapped in the old blanket
        # `catch (Exception e) { return; }` reports NOTHING when the patched
        # build throws on an input the relation itself declared valid by
        # construction: the check goes quiet on both builds, measures 0/N,
        # and reads as a well-behaved tripwire. Normalise it here, before any
        # other lint sees the body, so every downstream check runs on the
        # shape that will actually be compiled: a tier-2 rethrow that fires
        # only for exceptions raised INSIDE the patch-changed class (its
        # constructors excluded — a constructor refusing its arguments is
        # setup, i.e. a rejection). The rethrow carries the mandated
        # "violated" message, so the counting wrapper, [relfire] recording,
        # attribution facts and judge all see an ordinary firing; nothing
        # downstream changes. FAIL-CLOSED: when the rewrite cannot be
        # produced the candidate is KEPT and DEMOTED with the reason
        # recorded — a blanket catch costs recall, not soundness, so a drop
        # would delete sound checks to punish a shape the prompt only just
        # started asking for.
        if patched_classes:
            _swallow = patched_probe_swallowed(getattr(rel, 'check', ''),
                                               patched_classes)
            if _swallow is not None:
                _rewritten = rethrow_patched_probe(
                    getattr(rel, 'check', ''), patched_classes,
                    relation_name=name,
                    documented_exceptions=documented_exceptions)
                if _rewritten:
                    try:
                        rel.check = _rewritten
                        rel.tier2_rewritten = True
                    except Exception:
                        pass
                    print(f"  [screen] {name}: PATCHED-PROBE SWALLOWED — "
                          f"{_swallow} — REWRITTEN (tier-2 rethrow inserted)")
                    record_event('deterministic', method='screen', target=name,
                                 output='tier2-rewritten', reason=_swallow)
                else:
                    print(f"  [screen] {name}: PATCHED-PROBE SWALLOWED — "
                          f"{_swallow} — DEMOTED (rewrite not applicable, "
                          f"kept as written)")
                    try:
                        rel.screen_demotion = (' | PATCHED-PROBE-SWALLOWED '
                                               '(exception blind spot): '
                                               + _swallow)
                    except Exception:
                        pass
                    record_event('deterministic', method='screen', target=name,
                                 output='demoted',
                                 reason=f'patched probe swallowed: {_swallow}')
            else:
                # Gate correction (prereg 2026-08-10): an LLM-WRITTEN tier-2
                # catch rethrows unconditionally, so a DOCUMENTED exception
                # (a solver's declared non-convergence throw) alarms on a
                # correct patch with only the prompt and the judge in the
                # way. Insert the shared documented check at the head of
                # such catches; documented -> rejection, everything else
                # falls through to the relation's own rethrow unchanged.
                _guarded = guard_llm_tier2(getattr(rel, 'check', ''),
                                           patched_classes,
                                           documented_exceptions)
                if _guarded:
                    try:
                        rel.check = _guarded
                        rel.tier2_doc_guarded = True
                    except Exception:
                        pass
                    print(f"  [screen] {name}: TIER-2 DOC-GUARDED "
                          f"(documented exceptions return as rejections)")
                    record_event('deterministic', method='screen',
                                 target=name, output='tier2-doc-guarded',
                                 reason='documented exceptions of the '
                                        'patched class return as rejections '
                                        'before the tier-2 rethrow')
        # P0.2 self-swallow lint: an alarm thrown inside the check's own
        # catch-everything block is caught and discarded before the
        # counting wrapper can see it — the check then measures 0/20,000
        # on buggy and gets promoted as "well-behaved" (every Lang-7-run
        # candidate had this). Reject BEFORE spending a compile.
        swallow_reason = violation_swallowed(getattr(rel, 'check', ''))
        if swallow_reason is not None:
            print(f"  [screen] {name}: SELF-SWALLOWED ALARM — "
                  f"{swallow_reason} — dropped")
            _mark(rel, 'dropped', f'self-swallowed alarm: {swallow_reason}')
            continue
        # L boolean-swallow lint: a check that catches into a bare flag and
        # alarms on the flag (catch { ok = false; } ... if (!ok) throw ...)
        # has destroyed the caught exception's identity — attribution can no
        # longer tell which exception fired or whether the unpatched build
        # crashes the same way. Reject before spending a compile.
        boolswallow = boolean_swallow(getattr(rel, 'check', ''))
        if boolswallow is not None:
            print(f"  [screen] {name}: EVIDENCE-DESTROYING ALARM — "
                  f"{boolswallow} — dropped")
            _mark(rel, 'dropped', f'boolean-swallow alarm: {boolswallow}')
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
            _mark(rel, 'dropped', f'needs forbidden library subclass of '
                  f'{subclassed}')
            continue
        # Harness-bug lint (same as campaign gate 0d): an index computed as
        # Math.abs(consume…()) % n goes negative on Integer.MIN_VALUE and
        # the check crashes on its own input handling.
        negmod = negative_modulo_index(getattr(rel, 'check', ''))
        if negmod is not None:
            print(f"  [screen] {name}: NEGATIVE-MODULO INDEX — {negmod} — "
                  f"dropped")
            _mark(rel, 'dropped', f'negative-modulo index bug: {negmod}')
            continue
        # Structure-from-data lint: a receiver-state check (rejection /
        # index / lookup / size) whose container is built entirely from
        # compile-time constants can only ever exercise ONE shape, so a
        # patch that misbehaves at a different shape is unreachable for it.
        # DEMOTED, not dropped — the check may still be perfectly sound and
        # may still catch a different defect; a drop would delete
        # correct-but-narrow checks. Recording it in the screen note makes
        # the structural blind spot visible to the convergence gate and to
        # the downstream steering instead of leaving it silent.
        conststate = constant_receiver_state(getattr(rel, 'check', ''))
        if conststate is not None:
            print(f"  [screen] {name}: CONSTANT RECEIVER STATE — "
                  f"{conststate} — DEMOTED (kept, structural blind spot "
                  f"recorded)")
            try:
                rel.screen_demotion = (' | CONSTANT-RECEIVER-STATE '
                                       '(structural blind spot): '
                                       + conststate)
            except Exception:
                pass
            record_event('deterministic', method='screen',
                         target=name, output='demoted',
                         reason=f'constant receiver state: {conststate}')
        # 8.35 Mechanism B — the ORDERING half of the same blind spot. A
        # rejection probe that never re-runs after its receiver's last state
        # change has only ever observed the pre-mutation state, which is the
        # state a state-conditional patch still gets right; the container can
        # be perfectly fuzz-shaped and the probe still never sees the shape.
        # DEMOTED like its shape twin: textual ordering is an approximation
        # (a probe inside a loop that mutates after it does re-run), and the
        # check may be sound and catch something else.
        probeorder = probe_before_last_mutation(getattr(rel, 'check', ''))
        if probeorder is not None:
            print(f"  [screen] {name}: PROBE BEFORE LAST MUTATION — "
                  f"{probeorder} — DEMOTED (kept, ordering blind spot "
                  f"recorded)")
            try:
                rel.screen_demotion = (getattr(rel, 'screen_demotion', '')
                                       + ' | PROBE-BEFORE-LAST-MUTATION '
                                       '(ordering blind spot): ' + probeorder)
            except Exception:
                pass
            record_event('deterministic', method='screen',
                         target=name, output='demoted',
                         reason=f'probe before last mutation: {probeorder}')
        # Anti-anchoring lint (--divcap). Synthesis was shown which observable
        # the patch moves and what it moved FROM; a relation that then expects
        # the FROM value is asserting the pre-patch behaviour. DEMOTED, never
        # a drop: the coincidence case (a documented -1, an empty string) is
        # real, and the demotion text names the matched value so it can be
        # told apart. Inert without --divcap.
        if divergence_values:
            anchored = anchors_buggy_value(getattr(rel, 'check', ''),
                                           divergence_values)
            if anchored is not None:
                print(f"  [screen] {name}: ANCHORED ON A PRE-PATCH VALUE — "
                      f"{anchored} — DEMOTED (kept, anchor recorded)")
                try:
                    rel.screen_demotion = (getattr(rel, 'screen_demotion', '')
                                           + ' | ANCHORED-ON-PRE-PATCH-VALUE: '
                                           + anchored)
                except Exception:
                    pass
                record_event('deterministic', method='screen',
                             target=name, output='demoted',
                             reason=f'anchored on a pre-patch value: '
                                    f'{anchored}')
        cls = f'RelScreen{i}'
        src = _screen_harness_source(package, imports or [], cls,
                                     getattr(rel, 'check', ''))
        try:
            build = builder.build(src, buggy_dir,
                                  output_subdir=f'relscreen_{i}')
        except Exception as exc:
            print(f"  [screen] {name}: build error ({exc}) — dropped")
            _mark(rel, 'dropped', f'build error: {exc}')
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
                _mark(rel, 'dropped', 'does not compile'
                      + (' (R1 repair failed)' if repair_fn is not None else '')
                      + (f': {reason[0]}' if reason else ''))
                continue
            print(f"  [screen] {name}: compile-repaired (R1)")
            try:
                rel.compile_repaired = True
            except Exception:
                pass
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
            _mark(rel, 'dropped', f'canary error: {exc}')
            continue
        if not canary_heard:
            print(f"  [screen] {name}: CANARY FAILED — a forced alarm was "
                  f"not registered by the counting wrapper; this "
                  f"candidate's violations cannot be measured — dropped")
            _mark(rel, 'dropped', 'canary failed: forced alarm not registered '
                  'by the counting wrapper (unmeasurable)')
            continue
        try:
            # Shared compile-and-measure primitive (also used by
            # measure_single_check). Fixed `-runs` budget so the ratio
            # denominator is comparable across candidates.
            _stats = _run_counting_fuzz(build, builder, buggy_dir,
                                        jazzer_standalone_jar, jazzer_api_jar,
                                        runs, timeout_seconds)
        except Exception as exc:
            print(f"  [screen] {name}: run error ({exc}) — dropped")
            _mark(rel, 'dropped', f'run error: {exc}')
            continue
        if _stats is None:
            print(f"  [screen] {name}: no stats line (harness crashed the "
                  f"JVM or never ran) — dropped")
            _mark(rel, 'dropped', 'no stats line (harness crashed the JVM or '
                  'never ran)')
            continue
        checked, violated = _stats
        if checked < MIN_CHECKED:
            print(f"  [screen] {name}: only {checked} checks executed — "
                  f"dropped (cannot screen)")
            _mark(rel, 'dropped', f'only {checked} checks executed (< '
                  f'{MIN_CHECKED}, cannot screen)')
            continue
        ratio = violated / checked
        try:
            # Consumed by _harden_survivor: a rule that fires on the buggy
            # build may be firing BECAUSE of the bug, so probe firings on
            # that same build are not evidence of unsoundness.
            rel.buggy_fire_ratio = ratio
            # Spec H: the raw buggy-side counts, in structured form, so the
            # fire-rate fact can quote them (not just the rounded ratio) when
            # this relation later fires on a patched-build replay.
            rel.screen_stats = (checked, violated)
        except Exception:
            pass
        record_event('deterministic', method='screen-fuzz-buggy', target=name,
                     output={'checked': checked, 'violated': violated,
                             'ratio': round(ratio, 4)})

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
        try:
            # Consumed by the replay verifier (run.py): the fuzzed-tier
            # comparison fact needs the mechanical answer to "does this
            # relation detect the reported defect?" as data, not prose.
            rel.screen_direction = direction
        except Exception:
            pass

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
            note = _set_note(rel, note)
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
            note = _set_note(rel, note)
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
            note = _set_note(rel, note)
            print(f"  [screen] {name}: RATIO-CAP — {note}")
            high_ratio.append(rel)
            continue
        note = (f"fired on {violated}/{checked} fuzzed inputs on the "
                f"buggy build" + (" (selective — consistent with the "
                                  "defect region)" if violated else
                                  " (silent on the buggy build)") + flag)
        note = _set_note(rel, note)
        print(f"  [screen] {name}: KEPT — {note}")
        (selective if violated else silent).append(rel)
    # Ranking (best first): direction-confirmed & deterministic, then
    # direction-confirmed but flaky (a lucky discriminator beats none),
    # then selective firers, then above-cap provisional, then ONE silent
    # tripwire. Silent relations fired on nothing the buggy build does —
    # past the first, each adds prompt mass without new evidence and
    # measurably distracts the generator (mined54: Lang-7 TP->FN under 36
    # mined assertions).
    # Record each candidate's bucket for the trace.
    for _b, _lbl in ((confirmed, 'kept: direction-confirmed'),
                     (confirmed_flaky, 'kept: direction-confirmed (flaky)'),
                     (selective, 'kept: selective firer'),
                     (high_ratio, 'kept: above-ratio-cap / inverted (replay-only)'),
                     (silent, 'silent on buggy (tripwire)')):
        for _r in _b:
            _mark(_r, 'kept', _lbl)
    # ALL silent tripwires are kept (was silent[:1]): the prompt-mass
    # lesson that motivated the [:1] cut is enforced downstream anyway
    # (run.py injects at most 2 rules into the harness prompt), while
    # every survivor here also feeds the patched-build REPLAY, which
    # costs only compute — and a silent-on-buggy tripwire is exactly the
    # shape that catches a contract violation the patch INTRODUCES
    # (e.g. a documented-@throws rule: quiet on buggy, loud on an
    # overfit that swallows the throw). Math-53 had 4 silent tripwires
    # and the old cut kept the wrong one.
    kept = (confirmed + confirmed_flaky + selective
            + high_ratio + silent)[:max_keep]
    dropped_by_cap = [r for r in confirmed + confirmed_flaky + selective
                      + high_ratio + silent if r not in kept]
    for _r in dropped_by_cap:
        _mark(_r, 'dropped', 'cut by the keep-cap (kept the higher-ranked ones)')
    if dropped_by_cap:
        # Not a screening failure — the prompt-size/distraction cap. Say
        # so, or "4 KEPT" followed by "3 survived" reads as an off-by-one.
        print(f"  [screen] cap: injecting {len(kept)} "
              f"(direction-confirmed first); "
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
        # 8.4x p1a: the replay wrapper RECORDS — a firing prints its own
        # message and the exact consumed inputs, so the conviction that
        # reaches the judge and the verdict gate carries values instead of
        # a synthesized nameless line (the 88% problem, replay track).
        src = _screen_harness_source(package, imports or [], cls, check,
                                     record_firings=True)
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
        fired_lines = []
        if trigger_literals:
            r1 = _measure_on_corpus(build, builder, patched_dir,
                                    jazzer_standalone_jar, jazzer_api_jar,
                                    trigger_literals, fired_out=fired_lines)
            r2 = _measure_on_corpus(build, builder, patched_dir,
                                    jazzer_standalone_jar, jazzer_api_jar,
                                    trigger_literals, fired_out=fired_lines)
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
            _text = outcome.stdout + '\n' + outcome.stderr
            fired_lines.extend(harvest_relfire_lines(_text))
            m = _STATS_RE.search(_text)
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
            record_event('deterministic', method='replay-on-patched',
                         target=name, output='quiet (did not fire)')
            continue
        print(f"  [replay] {name}: FIRED [{tier}] — {note}")
        record_event('deterministic', method='replay-on-patched', target=name,
                     output=f'FIRED [{tier}]'
                            + (f' — {fired_lines[0][:300]}' if fired_lines
                               else ''),
                     note=note)
        findings.append({
            'relation': rel, 'name': name, 'tier': tier, 'note': note,
            'fired_lines': fired_lines[:3],
            'trigger_violations': trig_v,
            'trigger_deterministic': trig_deterministic,
            'fuzz_checked': checked, 'fuzz_violated': violated,
            # Spec H aliases: the patched-build fuzz counts under the names the
            # fire-rate fact reads (the fuzzed tier IS the patched-build rate).
            'patched_checked': checked, 'patched_violated': violated,
        })
    return findings
