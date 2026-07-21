"""Deterministic boundary-value probe via a canned FuzzedDataProvider.

Why not a byte corpus
---------------------
A relation check draws its inputs through Jazzer's FuzzedDataProvider, which
decodes libFuzzer's raw bytes into typed values with its OWN (opaque, version-
specific) recipe. Seeding a corpus with the raw bytes of NaN/Inf/extremes does
NOT make consumeDouble() return NaN — measured: zero firings across every leg,
even on the buggy build where a NaN relation must fire. The bytes never
survive translation.

The fix is to prepare the special values IN JAVA and bypass the translator:
supply our OWN FuzzedDataProvider whose consumeDouble() simply RETURNS NaN,
consumeString() returns "", etc., from fixed per-type tables. The model's
check is unchanged — it still calls data.consumeDouble() — but now it is
handed a real boundary value, guaranteed and deterministically.

A tiny driver runs the check over SEEDS canned providers (each rotates the
tables to a different starting offset, so successive consume* calls and
successive runs cover different boundary combinations), counting violations
the same way the fuzzing screen does (a RuntimeException whose message
contains "violated"). No jazzer, no fuzzing — just `java`.

The value tables are universal (IEEE specials, integer/char extremes, string
edge cases) — identical for every bug, no per-benchmark shaping. This is the
type-driven boundary set; domain-specific boundaries a method needs (an array
Class, a documented threshold) are a separate, generator-proposed concern.
"""
import os
import re
import subprocess
from typing import List, Optional, Tuple

SEEDS = 24  # number of canned providers the driver runs the check against

# Human-readable description of the extreme values the canned provider feeds,
# handed to the model when a check fires on them so it can reason (from the
# contract) about which one exposes an UNSOUND assertion. Kept in sync with
# the per-type tables in _PROVIDER_SRC.
EXTREMES_CHECKLIST = (
    "- doubles/floats: NaN, +Infinity, -Infinity, +0.0, -0.0, MAX_VALUE, "
    "MIN_VALUE, -MAX_VALUE, and combinations that ARITHMETICALLY produce NaN "
    "or Infinity from NON-special operands (e.g. Inf + -Inf = NaN, 0.0/0.0 = "
    "NaN, MAX*2 = Inf) — a correct result can be NaN/Inf even when no INPUT "
    "was NaN/Inf.\n"
    "- ints/longs/shorts/bytes: MIN_VALUE, MAX_VALUE, 0, 1, -1 (watch "
    "overflow: MIN_VALUE negated or abs() stays negative; MAX+1 wraps).\n"
    "- chars: '\\u0000', Character.MAX_VALUE, surrogate-range values.\n"
    "- strings: empty \"\", whitespace-only, embedded NUL, non-ASCII/"
    "surrogate, very long, numeric-looking (\"NaN\", \"-1\"), path-like.\n"
    "- empty/degenerate: empty input, zero-length collections."
)

_STATS_RE = re.compile(r'\[relscreen\]\s+checked=(\d+)\s+violated=(\d+)')

# The canned FuzzedDataProvider. RAW string: Java escapes (\n, \0, \uXXXX)
# must reach javac verbatim, not be interpreted by Python. No package/import
# here — it shares the harness file's package and its FuzzedDataProvider
# import. Bounded consume*(min,max) return in-range boundary values (min /
# max / midpoint); unbounded consume* rotate the per-type extreme tables.
_PROVIDER_SRC = r'''
final class SVCanned implements com.code_intelligence.jazzer.api.FuzzedDataProvider {
    private static final double[] DALL = {Double.NaN, Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY, 0.0, -0.0, Double.MAX_VALUE, Double.MIN_VALUE,
        -Double.MAX_VALUE, 1.0, -1.0, 2.0};
    private static final double[] DFIN = {0.0, -0.0, Double.MAX_VALUE,
        -Double.MAX_VALUE, Double.MIN_VALUE, 1.0, -1.0, 2.0, -2.0};
    private static final float[] FALL = {Float.NaN, Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY, 0f, -0f, Float.MAX_VALUE, Float.MIN_VALUE,
        -Float.MAX_VALUE, 1f, -1f};
    private static final float[] FFIN = {0f, -0f, Float.MAX_VALUE,
        -Float.MAX_VALUE, Float.MIN_VALUE, 1f, -1f, 2f};
    private static final int[] IALL = {Integer.MIN_VALUE, Integer.MAX_VALUE,
        0, 1, -1, 2, -2, 16, 255};
    private static final long[] LALL = {Long.MIN_VALUE, Long.MAX_VALUE, 0L, 1L,
        -1L, (long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE, 2L, -2L};
    private static final short[] SHALL = {Short.MIN_VALUE, Short.MAX_VALUE,
        0, 1, -1, 2};
    private static final byte[] BALL = {Byte.MIN_VALUE, Byte.MAX_VALUE, 0, 1,
        -1, (byte) 0x7f, (byte) 0x80};
    private static final char[] CALL = {'\u0000', ' ', '\t', '\n', 'A', '0',
        '/', Character.MAX_VALUE, '퟿'};
    private static final String[] SALL = {"", " ", "\t", "\n", "\u0000", "  ",
        "é", "😀", "​", "-1", "0", "NaN", "Infinity",
        "/", "..", "a\u0000b", longStr()};
    private static final String[] AALL = {"", " ", "\n", "-1", "0", "A",
        "abc", "/.."};
    // ORDINARY (benign, in-domain) tables — the control run. If a check fires
    // on THESE too, its extreme-firing is a degenerate-structure artifact, not
    // an unsoundness, so it is not worth a repair.
    private static final double[] DORD = {0.0, 1.0, 2.0, 3.0, -1.0, 0.5, 10.0};
    private static final float[] FORD = {0f, 1f, 2f, 3f, -1f, 0.5f};
    private static final int[] IORD = {0, 1, 2, 3, 5, -1, 10};
    private static final long[] LORD = {0L, 1L, 2L, 3L, 5L, -1L, 10L};
    private static final short[] SHORD = {0, 1, 2, 3, 5};
    private static final char[] CORD = {'a', 'b', 'c', 'x', '1', ' '};
    private static final String[] SORD = {"a", "ab", "abc", "hello", "1",
        "x y", "test"};

    private static String longStr() {
        char[] c = new char[256];
        java.util.Arrays.fill(c, 'A');
        return new String(c);
    }

    private int di, li, si, ci, bi, bo;
    private int budget;
    // Per-instance table refs so ORDINARY mode swaps benign values in while
    // the consume* method bodies stay identical.
    private final double[] D, DR;
    private final float[] FL, FR;
    private final int[] I;
    private final long[] L;
    private final short[] SH;
    private final char[] C;
    private final String[] STR, ASTR;

    SVCanned(int seed) { this(seed, false); }

    SVCanned(int seed, boolean ordinary) {
        this.di = this.li = this.si = this.ci = this.bi = this.bo =
            (seed < 0 ? -seed : seed);
        this.budget = 24;
        this.D    = ordinary ? DORD  : DALL;
        this.DR   = ordinary ? DORD  : DFIN;
        this.FL   = ordinary ? FORD  : FALL;
        this.FR   = ordinary ? FORD  : FFIN;
        this.I    = ordinary ? IORD  : IALL;
        this.L    = ordinary ? LORD  : LALL;
        this.SH   = ordinary ? SHORD : SHALL;
        this.C    = ordinary ? CORD  : CALL;
        this.STR  = ordinary ? SORD  : SALL;
        this.ASTR = ordinary ? SORD  : AALL;
    }

    private void step() { if (budget > 0) budget--; }

    public boolean consumeBoolean() { step(); return ((bo++) & 1) == 0; }
    public boolean[] consumeBooleans(int n) {
        step(); boolean[] a = new boolean[clamp(n, 4)];
        for (int k = 0; k < a.length; k++) a[k] = consumeBoolean(); return a; }
    public byte consumeByte() { step(); return BALL[(bi++) % BALL.length]; }
    public byte consumeByte(byte min, byte max) {
        step(); return (byte) inRange(min, max); }
    public byte[] consumeBytes(int n) {
        step(); byte[] a = new byte[clamp(n, 8)];
        for (int k = 0; k < a.length; k++) a[k] = consumeByte(); return a; }
    public byte[] consumeRemainingAsBytes() {
        budget = 0; return new byte[]{0, -1, Byte.MIN_VALUE, Byte.MAX_VALUE}; }
    public short consumeShort() { step(); return SH[(li++) % SH.length]; }
    public short consumeShort(short min, short max) {
        step(); return (short) inRange(min, max); }
    public short[] consumeShorts(int n) {
        step(); short[] a = new short[clamp(n, 4)];
        for (int k = 0; k < a.length; k++) a[k] = consumeShort(); return a; }
    public int consumeInt() { step(); return I[(li++) % I.length]; }
    public int consumeInt(int min, int max) { step(); return inRange(min, max); }
    public int[] consumeInts(int n) {
        step(); int[] a = new int[clamp(n, 4)];
        for (int k = 0; k < a.length; k++) a[k] = consumeInt(); return a; }
    public long consumeLong() { step(); return L[(li++) % L.length]; }
    public long consumeLong(long min, long max) {
        step(); if (max <= min) return min;
        switch ((li++) % 3) { case 0: return min; case 1: return max;
            default: return min + (max - min) / 2; } }
    public long[] consumeLongs(int n) {
        step(); long[] a = new long[clamp(n, 4)];
        for (int k = 0; k < a.length; k++) a[k] = consumeLong(); return a; }
    public float consumeFloat() { step(); return FL[(di++) % FL.length]; }
    public float consumeRegularFloat() { step(); return FR[(di++) % FR.length]; }
    public float consumeRegularFloat(float min, float max) {
        step(); if (max <= min) return min;
        switch ((di++) % 3) { case 0: return min; case 1: return max;
            default: return min + (max - min) / 2f; } }
    public float consumeProbabilityFloat() {
        step(); float[] P = {0f, 1f, 0.5f}; return P[(di++) % 3]; }
    public double consumeDouble() { step(); return D[(di++) % D.length]; }
    public double consumeRegularDouble() { step(); return DR[(di++) % DR.length]; }
    public double consumeRegularDouble(double min, double max) {
        step(); if (max <= min) return min;
        switch ((di++) % 3) { case 0: return min; case 1: return max;
            default: return min + (max - min) / 2; } }
    public double consumeProbabilityDouble() {
        step(); double[] P = {0.0, 1.0, 0.5}; return P[(di++) % 3]; }
    public char consumeChar() { step(); return C[(ci++) % C.length]; }
    public char consumeChar(char min, char max) {
        step(); if (max <= min) return min;
        return (char) (min + ((ci++) % (max - min + 1))); }
    public char consumeCharNoSurrogates() {
        step(); char c = C[(ci++) % C.length];
        return Character.isSurrogate(c) ? 'A' : c; }
    public String consumeString(int maxLength) {
        step(); return cap(STR[(si++) % STR.length], maxLength); }
    public String consumeRemainingAsString() {
        budget = 0; return STR[(si++) % STR.length]; }
    public String consumeAsciiString(int maxLength) {
        step(); return cap(ASTR[(si++) % ASTR.length], maxLength); }
    public String consumeRemainingAsAsciiString() {
        budget = 0; return "abc"; }
    public int remainingBytes() { return budget; }

    private static int clamp(int n, int hi) {
        if (n < 0) return 0; return n < hi ? n : hi; }
    private int inRange(int min, int max) {
        if (max <= min) return min;
        switch ((li++) % 3) { case 0: return min; case 1: return max;
            default: return min + (max - min) / 2; } }
    private static String cap(String s, int maxLength) {
        if (maxLength >= 0 && s.length() > maxLength)
            return s.substring(0, maxLength);
        return s; }
}
'''


def build_probe_source(package: Optional[str], imports: List[str],
                       class_name: str, check_body: str,
                       ordinary: bool = False) -> str:
    """Full .java file: the driver (public class_name, with main + the same
    counting convention the screen uses) plus the canned provider. The check
    body is inserted verbatim into runCheck and handed an SVCanned each run.
    `ordinary=True` runs the benign control tables (the artifact check)."""
    lines: List[str] = []
    if package:
        lines.append(f'package {package};')
    lines.append('import com.code_intelligence.jazzer.api.FuzzedDataProvider;')
    for imp in imports or []:
        imp = imp.strip()
        if imp and imp not in lines:
            lines.append(imp if imp.endswith(';') else imp + ';')
    lines += [
        '',
        f'public class {class_name} ' + '{',
        '    static long checked = 0, violated = 0;',
        '',
        '    private static void runCheck(FuzzedDataProvider data)'
        ' throws Exception {',
        check_body,
        '    }',
        '',
        '    public static void main(String[] args) {',
        f'        for (int seed = 0; seed < {SEEDS}; seed++) ' + '{',
        '            checked++;',
        '            try {',
        f'                runCheck(new SVCanned(seed, '
        f'{"true" if ordinary else "false"}));',
        '            } catch (RuntimeException e) {',
        '                String m = String.valueOf(e.getMessage());',
        '                if (m.contains("violated")) { violated++; }',
        '            } catch (Throwable t) {',
        '                // incidental crash on an extreme value — not a',
        '                // rule violation, not counted (matches the screen)',
        '            }',
        '        }',
        '        System.err.println("[relscreen] checked=" + checked',
        '            + " violated=" + violated);',
        '    }',
        '}',
        _PROVIDER_SRC,
    ]
    return '\n'.join(lines)


def run_canned_probe(builder, work_dir: str, package: Optional[str],
                     imports: List[str], class_name: str, check_body: str,
                     output_subdir: str = '',
                     timeout_seconds: int = 40,
                     ordinary: bool = False
                     ) -> Optional[Tuple[int, int]]:
    """Compile the driver against `work_dir` and run it under a plain JVM
    (bounded heap so an extreme allocation fails fast rather than thrashing).
    `ordinary=True` runs the benign control tables. Returns (checked,
    violated) or None if it could not be built/run."""
    src = build_probe_source(package, imports, class_name, check_body,
                             ordinary=ordinary)
    try:
        build = builder.build(src, work_dir,
                              output_subdir=output_subdir or f'svprobe_{class_name}')
    except Exception:
        return None
    if not build.compiled:
        return None
    cp = os.pathsep.join([build.classpath,
                          os.path.dirname(build.harness_path)])
    try:
        proc = subprocess.run(
            ['java', '-Xmx512m', '-cp', cp, build.class_name],
            capture_output=True, text=True, timeout=timeout_seconds)
    except Exception:
        return None
    m = _STATS_RE.search(proc.stdout + '\n' + proc.stderr)
    if not m:
        return None
    return int(m.group(1)), int(m.group(2))
