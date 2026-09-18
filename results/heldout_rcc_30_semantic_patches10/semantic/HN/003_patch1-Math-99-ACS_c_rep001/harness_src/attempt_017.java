package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkGcdLiftedPairs();
        checkGcdLiftedExceptions();

        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);

        // MathUtils.gcd keeps sign-insensitive semantics as shown by the lifted test
        // (all four sign variants of the same magnitudes return the same gcd).
        try {
            int g1 = MathUtils.gcd(a, b);
            int g2 = MathUtils.gcd(-a, b);
            int g3 = MathUtils.gcd(a, -b);
            int g4 = MathUtils.gcd(-a, -b);
            if (g1 != g2) {
                throw new RuntimeException("[oracle:gcd-sign-1] metamorphic violation: gcd(a,b)==gcd(-a,b) inputA=" + a + " inputB=" + b + " lhs=" + g1 + " rhs=" + g2);
            }
            if (g1 != g3) {
                throw new RuntimeException("[oracle:gcd-sign-2] metamorphic violation: gcd(a,b)==gcd(a,-b) inputA=" + a + " inputB=" + b + " lhs=" + g1 + " rhs=" + g3);
            }
            if (g1 != g4) {
                throw new RuntimeException("[oracle:gcd-sign-3] metamorphic violation: gcd(a,b)==gcd(-a,-b) inputA=" + a + " inputB=" + b + " lhs=" + g1 + " rhs=" + g4);
            }
        } catch (Throwable ignored) {
            return;
        }

        // MathUtils.lcm is documented as the least common multiple of the absolute
        // values, so flipping signs must not change the answer. A patch that deletes
        // the overflow check and leaks Math.abs(Integer.MIN_VALUE) as a negative value
        // violates this observable post-condition.
        try {
            int nonZeroA = data.consumeBoolean() ? a : (a == 0 ? 1 : a);
            int nonZeroB = data.consumeBoolean() ? b : (b == 0 ? -1 : b);
            int l1 = MathUtils.lcm(nonZeroA, nonZeroB);
            int l2 = MathUtils.lcm(-nonZeroA, nonZeroB);
            int l3 = MathUtils.lcm(nonZeroA, -nonZeroB);
            int l4 = MathUtils.lcm(-nonZeroA, -nonZeroB);
            if (l1 != l2) {
                throw new RuntimeException("[oracle:lcm-sign-1] metamorphic violation: lcm(a,b)==lcm(-a,b) inputA=" + nonZeroA + " inputB=" + nonZeroB + " lhs=" + l1 + " rhs=" + l2);
            }
            if (l1 != l3) {
                throw new RuntimeException("[oracle:lcm-sign-2] metamorphic violation: lcm(a,b)==lcm(a,-b) inputA=" + nonZeroA + " inputB=" + nonZeroB + " lhs=" + l1 + " rhs=" + l3);
            }
            if (l1 != l4) {
                throw new RuntimeException("[oracle:lcm-sign-3] metamorphic violation: lcm(a,b)==lcm(-a,-b) inputA=" + nonZeroA + " inputB=" + nonZeroB + " lhs=" + l1 + " rhs=" + l4);
            }
        } catch (Throwable ignored) {
            return;
        }

        // Directly exercise the patched lcm overflow path on a documented special case:
        // lcm(Integer.MIN_VALUE, 1) would be 2^31 and must throw ArithmeticException.
        checkExpectedArithmeticForLcm(Integer.MIN_VALUE, 1, "lcm-min-power2-a");
        checkExpectedArithmeticForLcm(1, Integer.MIN_VALUE, "lcm-min-power2-b");
    }

    private static void checkGcdLiftedPairs() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEqualsInt("gcd-00", 0, MathUtils.gcd(0, 0));
        assertEqualsInt("gcd-0b", b, MathUtils.gcd(0, b));
        assertEqualsInt("gcd-a0", a, MathUtils.gcd(a, 0));
        assertEqualsInt("gcd-0negb", b, MathUtils.gcd(0, -b));
        assertEqualsInt("gcd-nega0", a, MathUtils.gcd(-a, 0));

        assertEqualsInt("gcd-ab", 10, MathUtils.gcd(a, b));
        assertEqualsInt("gcd-negab", 10, MathUtils.gcd(-a, b));
        assertEqualsInt("gcd-anegb", 10, MathUtils.gcd(a, -b));
        assertEqualsInt("gcd-neganegb", 10, MathUtils.gcd(-a, -b));

        assertEqualsInt("gcd-ac", 1, MathUtils.gcd(a, c));
        assertEqualsInt("gcd-negac", 1, MathUtils.gcd(-a, c));
        assertEqualsInt("gcd-anegc", 1, MathUtils.gcd(a, -c));
        assertEqualsInt("gcd-neganegc", 1, MathUtils.gcd(-a, -c));

        assertEqualsInt("gcd-shifted", 3 * (1 << 15), MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15)));

        assertEqualsInt("gcd-max-0", Integer.MAX_VALUE, MathUtils.gcd(Integer.MAX_VALUE, 0));
        assertEqualsInt("gcd-negmax-0", Integer.MAX_VALUE, MathUtils.gcd(-Integer.MAX_VALUE, 0));
        assertEqualsInt("gcd-2pow30-min", 1 << 30, MathUtils.gcd(1 << 30, -Integer.MIN_VALUE));
    }

    private static void checkGcdLiftedExceptions() {
        checkExpectedArithmeticForGcd(Integer.MIN_VALUE, 0, "gcd-min-0");
        checkExpectedArithmeticForGcd(0, Integer.MIN_VALUE, "gcd-0-min");
        checkExpectedArithmeticForGcd(Integer.MIN_VALUE, Integer.MIN_VALUE, "gcd-min-min");
    }

    private static void checkExpectedArithmeticForGcd(int x, int y, String id) {
        try {
            int actual = MathUtils.gcd(x, y);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException but got value " + actual + " for MathUtils.gcd(" + x + "," + y + ")"
            );
        } catch (ArithmeticException expected) {
        }
    }

    private static void checkExpectedArithmeticForLcm(int x, int y, String id) {
        try {
            int actual = MathUtils.lcm(x, y);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException but got value " + actual + " for MathUtils.lcm(" + x + "," + y + ")"
            );
        } catch (ArithmeticException expected) {
        }
    }

    private static void assertEqualsInt(String id, int expected, int actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual
            );
        }
    }
}