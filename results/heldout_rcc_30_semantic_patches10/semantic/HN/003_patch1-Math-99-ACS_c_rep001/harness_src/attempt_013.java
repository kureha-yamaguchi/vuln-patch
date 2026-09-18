package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = 30;
        int b = 50;
        int c = 77;

        checkEquals("gcd-00", 0, MathUtils.gcd(0, 0), "MathUtils.gcd(0, 0)");
        checkEquals("gcd-0b", b, MathUtils.gcd(0, b), "MathUtils.gcd(0, 50)");
        checkEquals("gcd-a0", a, MathUtils.gcd(a, 0), "MathUtils.gcd(30, 0)");
        checkEquals("gcd-0neg-b", b, MathUtils.gcd(0, -b), "MathUtils.gcd(0, -50)");
        checkEquals("gcd-neg-a0", a, MathUtils.gcd(-a, 0), "MathUtils.gcd(-30, 0)");

        checkEquals("gcd-ab", 10, MathUtils.gcd(a, b), "MathUtils.gcd(30, 50)");
        checkEquals("gcd-negab", 10, MathUtils.gcd(-a, b), "MathUtils.gcd(-30, 50)");
        checkEquals("gcd-anegb", 10, MathUtils.gcd(a, -b), "MathUtils.gcd(30, -50)");
        checkEquals("gcd-neganegb", 10, MathUtils.gcd(-a, -b), "MathUtils.gcd(-30, -50)");

        checkEquals("gcd-ac", 1, MathUtils.gcd(a, c), "MathUtils.gcd(30, 77)");
        checkEquals("gcd-negac", 1, MathUtils.gcd(-a, c), "MathUtils.gcd(-30, 77)");
        checkEquals("gcd-anegc", 1, MathUtils.gcd(a, -c), "MathUtils.gcd(30, -77)");
        checkEquals("gcd-neganegc", 1, MathUtils.gcd(-a, -c), "MathUtils.gcd(-30, -77)");

        checkEquals("gcd-shift", 3 * (1 << 15), MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15)),
                "MathUtils.gcd(3 * (1<<20), 9 * (1<<15))");

        checkEquals("gcd-max-0", Integer.MAX_VALUE, MathUtils.gcd(Integer.MAX_VALUE, 0),
                "MathUtils.gcd(Integer.MAX_VALUE, 0)");
        checkEquals("gcd-negmax-0", Integer.MAX_VALUE, MathUtils.gcd(-Integer.MAX_VALUE, 0),
                "MathUtils.gcd(-Integer.MAX_VALUE, 0)");
        checkEquals("gcd-1<<30-min", 1 << 30, MathUtils.gcd(1 << 30, -Integer.MIN_VALUE),
                "MathUtils.gcd(1<<30, -Integer.MIN_VALUE)");

        expectArithmetic("gcd-min-0-throws", Integer.MIN_VALUE, 0);
        expectArithmetic("gcd-0-min-throws", 0, Integer.MIN_VALUE);
        expectArithmetic("gcd-min-min-throws", Integer.MIN_VALUE, Integer.MIN_VALUE);

        int x = data.consumeInt(-1_000_000, 1_000_000);
        int y = data.consumeInt(-1_000_000, 1_000_000);
        int scale = data.consumeInt(1, 32);

        try {
            int gx = MathUtils.gcd(x, y);
            int gy = MathUtils.gcd(y, x);
            if (gx != gy) {
                throw new RuntimeException("[oracle:gcd-symmetry] metamorphic violation: gcd must be symmetric for any correct greatest-common-divisor implementation input=(" + x + "," + y + ") lhs=" + gx + " rhs=" + gy);
            }
        } catch (Throwable ignored) {
        }

        try {
            int sx = MathUtils.gcd(x, y);
            int sy = MathUtils.gcd(-x, y);
            int sz = MathUtils.gcd(x, -y);
            int sw = MathUtils.gcd(-x, -y);
            if (sx != sy || sx != sz || sx != sw) {
                throw new RuntimeException("[oracle:gcd-sign-invariance] metamorphic violation: gcd is over common divisors of the absolute values, so changing operand signs must not change the result input=(" + x + "," + y + ") base=" + sx + " negX=" + sy + " negY=" + sz + " negBoth=" + sw);
            }
        } catch (Throwable ignored) {
        }

        try {
            int mx = x * scale;
            int my = y * scale;
            int base = MathUtils.gcd(x, y);
            int scaled = MathUtils.gcd(mx, my);
            long expectedScaled = Math.abs((long) base) * (long) scale;
            if (scaled != (int) expectedScaled) {
                throw new RuntimeException("[oracle:gcd-scaling] metamorphic violation: scaling both inputs by a positive factor must scale gcd by that factor input=(" + x + "," + y + ") factor=" + scale + " base=" + base + " scaled=" + scaled + " expected=" + expectedScaled);
            }
        } catch (Throwable ignored) {
        }

        try {
            int l1 = MathUtils.lcm(x, y);
            int l2 = MathUtils.lcm(y, x);
            if (l1 != l2) {
                throw new RuntimeException("[oracle:lcm-symmetry] metamorphic violation: lcm is defined on the absolute values of two numbers, so swapping arguments must not change the result input=(" + x + "," + y + ") lhs=" + l1 + " rhs=" + l2);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void checkEquals(String oracleId, int expected, int actual, String call) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + call + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmetic(String oracleId, int p, int q) {
        try {
            int actual = MathUtils.gcd(p, q);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from MathUtils.gcd(" + p + ", " + q + ") actualReturn=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}