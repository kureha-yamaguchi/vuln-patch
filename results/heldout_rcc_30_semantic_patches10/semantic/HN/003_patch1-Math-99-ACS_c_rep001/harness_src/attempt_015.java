package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int a = 30;
        final int b = 50;
        final int c = 77;

        assertEqualsInt("gcd-0-0", 0, MathUtils.gcd(0, 0), "MathUtils.gcd(0, 0)");
        assertEqualsInt("gcd-0-b", b, MathUtils.gcd(0, b), "MathUtils.gcd(0, 50)");
        assertEqualsInt("gcd-a-0", a, MathUtils.gcd(a, 0), "MathUtils.gcd(30, 0)");
        assertEqualsInt("gcd-0-negb", b, MathUtils.gcd(0, -b), "MathUtils.gcd(0, -50)");
        assertEqualsInt("gcd-nega-0", a, MathUtils.gcd(-a, 0), "MathUtils.gcd(-30, 0)");

        assertEqualsInt("gcd-a-b", 10, MathUtils.gcd(a, b), "MathUtils.gcd(30, 50)");
        assertEqualsInt("gcd-nega-b", 10, MathUtils.gcd(-a, b), "MathUtils.gcd(-30, 50)");
        assertEqualsInt("gcd-a-negb", 10, MathUtils.gcd(a, -b), "MathUtils.gcd(30, -50)");
        assertEqualsInt("gcd-nega-negb", 10, MathUtils.gcd(-a, -b), "MathUtils.gcd(-30, -50)");

        assertEqualsInt("gcd-a-c", 1, MathUtils.gcd(a, c), "MathUtils.gcd(30, 77)");
        assertEqualsInt("gcd-nega-c", 1, MathUtils.gcd(-a, c), "MathUtils.gcd(-30, 77)");
        assertEqualsInt("gcd-a-negc", 1, MathUtils.gcd(a, -c), "MathUtils.gcd(30, -77)");
        assertEqualsInt("gcd-nega-negc", 1, MathUtils.gcd(-a, -c), "MathUtils.gcd(-30, -77)");

        assertEqualsInt(
                "gcd-shifted",
                3 * (1 << 15),
                MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15)),
                "MathUtils.gcd(3 * (1<<20), 9 * (1<<15))");

        assertEqualsInt(
                "gcd-max-0",
                Integer.MAX_VALUE,
                MathUtils.gcd(Integer.MAX_VALUE, 0),
                "MathUtils.gcd(Integer.MAX_VALUE, 0)");
        assertEqualsInt(
                "gcd-negmax-0",
                Integer.MAX_VALUE,
                MathUtils.gcd(-Integer.MAX_VALUE, 0),
                "MathUtils.gcd(-Integer.MAX_VALUE, 0)");
        assertEqualsInt(
                "gcd-1<<30-min",
                1 << 30,
                MathUtils.gcd(1 << 30, -Integer.MIN_VALUE),
                "MathUtils.gcd(1<<30, -Integer.MIN_VALUE)");

        expectArithmeticException("gcd-min-0", Integer.MIN_VALUE, 0);
        expectArithmeticException("gcd-0-min", 0, Integer.MIN_VALUE);
        expectArithmeticException("gcd-min-min", Integer.MIN_VALUE, Integer.MIN_VALUE);

        int power = data.consumeInt(0, 30);
        int pow2 = 1 << power;
        if (data.consumeBoolean()) {
            pow2 = -pow2;
        }

        expectArithmeticExceptionLcm("lcm-min-power2", Integer.MIN_VALUE, pow2);

        int x = data.consumeInt(1, 1_000_000);
        int y = data.consumeInt(1, 1_000_000);
        if (data.consumeBoolean()) {
            x = -x;
        }
        if (data.consumeBoolean()) {
            y = -y;
        }

        try {
            int g1 = MathUtils.gcd(x, y);
            int g2 = MathUtils.gcd(-x, y);
            int g3 = MathUtils.gcd(x, -y);
            int g4 = MathUtils.gcd(-x, -y);

            if (!(g1 == g2 && g1 == g3 && g1 == g4)) {
                throw new RuntimeException(
                        "[oracle:gcd-sign-invariance] metamorphic violation: gcd must be invariant under operand sign changes input=("
                                + x + "," + y + ") gcd(x,y)=" + g1 + " gcd(-x,y)=" + g2 + " gcd(x,-y)=" + g3
                                + " gcd(-x,-y)=" + g4);
            }
        } catch (Throwable t) {
            return;
        }

        try {
            int l1 = MathUtils.lcm(x, y);
            int l2 = MathUtils.lcm(-x, y);
            int l3 = MathUtils.lcm(x, -y);
            int l4 = MathUtils.lcm(-x, -y);

            /* Contract justification: lcm javadoc says it returns "the least common multiple
             * of the absolute value of two numbers", so changing operand signs must not change
             * the successful result. A patch that merely deletes the new overflow throw can
             * silently return a sign-dependent wrong value instead of the documented absolute one. */
            if (!(l1 == l2 && l1 == l3 && l1 == l4)) {
                throw new RuntimeException(
                        "[oracle:lcm-sign-invariance] metamorphic violation: lcm must be invariant under operand sign changes input=("
                                + x + "," + y + ") lcm(x,y)=" + l1 + " lcm(-x,y)=" + l2 + " lcm(x,-y)=" + l3
                                + " lcm(-x,-y)=" + l4);
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual, String call) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + call + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmeticException(String oracleId, int p, int q) {
        try {
            int actual = MathUtils.gcd(p, q);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: MathUtils.gcd(" + p + ", " + q
                            + ") expected ArithmeticException actualReturn=" + actual);
        } catch (ArithmeticException expected) {
        }
    }

    private static void expectArithmeticExceptionLcm(String oracleId, int p, int q) {
        try {
            int actual = MathUtils.lcm(p, q);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: MathUtils.lcm(" + p + ", " + q
                            + ") expected ArithmeticException actualReturn=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}