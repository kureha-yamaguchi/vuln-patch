package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedLcmAssertions();
        runIndependentLcmNonNegativeOracle(data);
        runMulAndCheckExactProductOracle(data);
        reachCreateArithmeticExceptionPath();
    }

    private static void runLiftedLcmAssertions() {
        final int a = 30;
        final int b = 50;
        final int c = 77;

        assertEqualsInt("[oracle:lifted-lcm-0-b]", 0, MathUtils.lcm(0, b), "MathUtils.lcm(0, 50)");
        assertEqualsInt("[oracle:lifted-lcm-a-0]", 0, MathUtils.lcm(a, 0), "MathUtils.lcm(30, 0)");
        assertEqualsInt("[oracle:lifted-lcm-1-b]", b, MathUtils.lcm(1, b), "MathUtils.lcm(1, 50)");
        assertEqualsInt("[oracle:lifted-lcm-a-1]", a, MathUtils.lcm(a, 1), "MathUtils.lcm(30, 1)");
        assertEqualsInt("[oracle:lifted-lcm-a-b]", 150, MathUtils.lcm(a, b), "MathUtils.lcm(30, 50)");
        assertEqualsInt("[oracle:lifted-lcm-neg-a-b]", 150, MathUtils.lcm(-a, b), "MathUtils.lcm(-30, 50)");
        assertEqualsInt("[oracle:lifted-lcm-a-neg-b]", 150, MathUtils.lcm(a, -b), "MathUtils.lcm(30, -50)");
        assertEqualsInt("[oracle:lifted-lcm-neg-a-neg-b]", 150, MathUtils.lcm(-a, -b), "MathUtils.lcm(-30, -50)");
        assertEqualsInt("[oracle:lifted-lcm-a-c]", 2310, MathUtils.lcm(a, c), "MathUtils.lcm(30, 77)");
        assertEqualsInt("[oracle:lifted-lcm-pow-scale]", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5), "MathUtils.lcm((1<<20)*3, (1<<20)*5)");
        assertEqualsInt("[oracle:lifted-lcm-0-0]", 0, MathUtils.lcm(0, 0), "MathUtils.lcm(0, 0)");

        expectArithmeticExceptionFromLcm("[oracle:lifted-lcm-min-1-throws]", Integer.MIN_VALUE, 1);
        expectArithmeticExceptionFromLcm("[oracle:lifted-lcm-min-pow-throws]", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticExceptionFromLcm("[oracle:lifted-lcm-max-maxminus1-throws]", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void runIndependentLcmNonNegativeOracle(FuzzedDataProvider data) {
        /*
         * Contract justification:
         * lcm is the least common multiple and the implementation computes Math.abs(...),
         * so whenever MathUtils.lcm(a, b) returns normally, the result must be non-negative.
         * A band-aid patch that simply removes the documented ArithmeticException for
         * lcm(Integer.MIN_VALUE, powerOfTwo) would still violate this post-condition by
         * returning Integer.MIN_VALUE, which is negative.
         */
        checkReturnedLcmIsNonNegative(Integer.MIN_VALUE, 1, "[oracle:lcm-nonnegative-min-1]");
        checkReturnedLcmIsNonNegative(Integer.MIN_VALUE, 1 << 20, "[oracle:lcm-nonnegative-min-pow20]");

        int k = data.consumeInt(0, 30);
        int n = 1 << k;
        if (data.consumeBoolean()) {
            n = -n;
        }
        checkReturnedLcmIsNonNegative(Integer.MIN_VALUE, n, "[oracle:lcm-nonnegative-fuzz-left]");
        checkReturnedLcmIsNonNegative(n, Integer.MIN_VALUE, "[oracle:lcm-nonnegative-fuzz-right]");
    }

    private static void runMulAndCheckExactProductOracle(FuzzedDataProvider data) {
        /*
         * Contract justification:
         * MathUtils.mulAndCheck(int, int) returns the exact mathematical product when that
         * product is representable as an int, and throws only on overflow. We construct
         * x and y in [-46340, 46340], so x*y is always representable in int by construction.
         * The expected value is therefore trusted exact arithmetic from the chosen input.
         * This is an independent check on a different reachable function in the same region.
         */
        int x = data.consumeInt(-46340, 46340);
        int y = data.consumeInt(-46340, 46340);
        long exact = (long) x * (long) y;
        int actual;
        try {
            actual = MathUtils.mulAndCheck(x, y);
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:mul-exact-safe-range] semantic mismatch: mulAndCheck unexpectedly rejected safe product for x="
                            + x + " y=" + y + " exact=" + exact,
                    t);
        }
        if (actual != (int) exact) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:mul-exact-safe-range] semantic mismatch: mulAndCheck(" + x + ", " + y
                            + ") expected=" + (int) exact + " actual=" + actual);
        }
    }

    private static void reachCreateArithmeticExceptionPath() {
        try {
            MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        } catch (Throwable ignored) {
            // Reach the real overflow path that uses MathRuntimeException.createArithmeticException.
        }
    }

    private static void checkReturnedLcmIsNonNegative(int left, int right, String oracleId) {
        int actual;
        try {
            actual = MathUtils.lcm(left, right);
        } catch (Throwable ignored) {
            return;
        }
        if (actual < 0) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " post-condition violation: lcm returned a negative value for left="
                            + left + " right=" + right + " actual=" + actual);
        }
    }

    private static void expectArithmeticExceptionFromLcm(String oracleId, int left, int right) {
        try {
            MathUtils.lcm(left, right);
        } catch (ArithmeticException expected) {
            return;
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " semantic mismatch: wrong exception type from MathUtils.lcm("
                            + left + ", " + right + ")",
                    t);
        }
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected ArithmeticException from MathUtils.lcm("
                        + left + ", " + right + ")");
    }

    private static void assertEqualsInt(String oracleId, int expected, int actual, String what) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:unnamed-check] " + oracleId + " semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }
}