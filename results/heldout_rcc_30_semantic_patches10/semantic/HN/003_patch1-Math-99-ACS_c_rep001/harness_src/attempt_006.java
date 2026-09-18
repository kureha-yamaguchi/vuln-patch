package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEqualsInt("gcd-0-0", 0, MathUtils.gcd(0, 0));
        assertEqualsInt("gcd-0-b", b, MathUtils.gcd(0, b));
        assertEqualsInt("gcd-a-0", a, MathUtils.gcd(a, 0));
        assertEqualsInt("gcd-0-negb", b, MathUtils.gcd(0, -b));
        assertEqualsInt("gcd-nega-0", a, MathUtils.gcd(-a, 0));

        assertEqualsInt("gcd-a-b", 10, MathUtils.gcd(a, b));
        assertEqualsInt("gcd-nega-b", 10, MathUtils.gcd(-a, b));
        assertEqualsInt("gcd-a-negb", 10, MathUtils.gcd(a, -b));
        assertEqualsInt("gcd-nega-negb", 10, MathUtils.gcd(-a, -b));

        assertEqualsInt("gcd-a-c", 1, MathUtils.gcd(a, c));
        assertEqualsInt("gcd-nega-c", 1, MathUtils.gcd(-a, c));
        assertEqualsInt("gcd-a-negc", 1, MathUtils.gcd(a, -c));
        assertEqualsInt("gcd-nega-negc", 1, MathUtils.gcd(-a, -c));

        assertEqualsInt("gcd-shifted", 3 * (1 << 15), MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15)));

        assertEqualsInt("gcd-max-0", Integer.MAX_VALUE, MathUtils.gcd(Integer.MAX_VALUE, 0));
        assertEqualsInt("gcd-negmax-0", Integer.MAX_VALUE, MathUtils.gcd(-Integer.MAX_VALUE, 0));
        assertEqualsInt("gcd-1<<30-negmin", 1 << 30, MathUtils.gcd(1 << 30, -Integer.MIN_VALUE));

        expectArithmeticExceptionForGcd("gcd-min-0", Integer.MIN_VALUE, 0);
        expectArithmeticExceptionForGcd("gcd-0-min", 0, Integer.MIN_VALUE);
        expectArithmeticExceptionForGcd("gcd-min-min", Integer.MIN_VALUE, Integer.MIN_VALUE);

        int x = data.consumeInt(-1_000_000, 1_000_000);
        int y = data.consumeInt(-1_000_000, 1_000_000);
        if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) {
            try {
                int g1 = MathUtils.gcd(x, y);
                int g2 = MathUtils.gcd(-x, y);
                int g3 = MathUtils.gcd(x, -y);
                int g4 = MathUtils.gcd(-x, -y);
                if (g1 != g2 || g1 != g3 || g1 != g4) {
                    throw new RuntimeException("[oracle:gcd-sign-invariance] metamorphic violation: gcd should be invariant under operand sign flips for finite int inputs; input=(" + x + "," + y + ") g=" + g1 + " gNegX=" + g2 + " gNegY=" + g3 + " gNegBoth=" + g4);
                }
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("[oracle:gcd-sign-invariance]")) {
                    throw e;
                }
                return;
            }
        }

        int shift = data.consumeInt(0, 30);
        int n = 1 << shift;
        if (data.consumeBoolean()) {
            n = -n;
        }

        assertEqualsInt("lcm-zero-left", 0, MathUtils.lcm(0, x));
        assertEqualsInt("lcm-zero-right", 0, MathUtils.lcm(y, 0));

        expectArithmeticExceptionForLcm("lcm-min-power2-left", Integer.MIN_VALUE, n);
        expectArithmeticExceptionForLcm("lcm-min-power2-right", n, Integer.MIN_VALUE);
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmeticExceptionForGcd(String oracleId, int p, int q) {
        try {
            int actual = MathUtils.gcd(p, q);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException actualReturn=" + actual);
        } catch (ArithmeticException expected) {
        }
    }

    private static void expectArithmeticExceptionForLcm(String oracleId, int a, int b) {
        try {
            int actual = MathUtils.lcm(a, b);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException actualReturn=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}