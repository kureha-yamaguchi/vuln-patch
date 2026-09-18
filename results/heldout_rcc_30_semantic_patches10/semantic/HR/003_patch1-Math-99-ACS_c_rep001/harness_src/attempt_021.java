package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkLiftedTestLcmOracles();
        checkHelperExactMinProductImpliesLcmRejects(data);
    }

    private static void checkLiftedTestLcmOracles() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEqualsOracle("lifted-lcm-0b", 0, MathUtils.lcm(0, b), "MathUtils.lcm(0, 50)");
        assertEqualsOracle("lifted-lcm-a0", 0, MathUtils.lcm(a, 0), "MathUtils.lcm(30, 0)");
        assertEqualsOracle("lifted-lcm-1b", b, MathUtils.lcm(1, b), "MathUtils.lcm(1, 50)");
        assertEqualsOracle("lifted-lcm-a1", a, MathUtils.lcm(a, 1), "MathUtils.lcm(30, 1)");
        assertEqualsOracle("lifted-lcm-ab", 150, MathUtils.lcm(a, b), "MathUtils.lcm(30, 50)");
        assertEqualsOracle("lifted-lcm-negab1", 150, MathUtils.lcm(-a, b), "MathUtils.lcm(-30, 50)");
        assertEqualsOracle("lifted-lcm-negab2", 150, MathUtils.lcm(a, -b), "MathUtils.lcm(30, -50)");
        assertEqualsOracle("lifted-lcm-negab3", 150, MathUtils.lcm(-a, -b), "MathUtils.lcm(-30, -50)");
        assertEqualsOracle("lifted-lcm-ac", 2310, MathUtils.lcm(a, c), "MathUtils.lcm(30, 77)");
        assertEqualsOracle("lifted-lcm-scale", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5),
                "MathUtils.lcm((1<<20)*3, (1<<20)*5)");
        assertEqualsOracle("lifted-lcm-00", 0, MathUtils.lcm(0, 0), "MathUtils.lcm(0, 0)");

        expectArithmeticFromLcm("lifted-lcm-min-one-throws", Integer.MIN_VALUE, 1);
        expectArithmeticFromLcm("lifted-lcm-min-pow-throws", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticFromLcm("lifted-lcm-max-maxminus-throws", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void checkHelperExactMinProductImpliesLcmRejects(FuzzedDataProvider data) {
        int k = data.consumeInt(0, 30);
        int magnitude = 1 << k;
        int n = data.consumeBoolean() ? -magnitude : magnitude;

        int g;
        try {
            g = MathUtils.gcd(Integer.MIN_VALUE, n);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:helper-exact-minproduct] semantic mismatch: gcd(Integer.MIN_VALUE," + n
                            + ") threw for valid power-of-two input",
                    t);
        }

        int expectedG = magnitude;
        if (g != expectedG) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:helper-exact-minproduct] semantic mismatch: gcd(Integer.MIN_VALUE," + n
                            + ") expected=" + expectedG + " actual=" + g);
        }

        int reduced = Integer.MIN_VALUE / g;

        int intProduct;
        try {
            intProduct = MathUtils.mulAndCheck(reduced, n);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:helper-exact-minproduct] semantic mismatch: mulAndCheck(" + reduced + "," + n
                            + ") threw after exact reduction",
                    t);
        }

        long longProduct;
        try {
            longProduct = MathUtils.mulAndCheck((long) reduced, (long) n);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:helper-exact-minproduct] semantic mismatch: long mulAndCheck(" + reduced + "," + n
                            + ") threw after exact reduction",
                    t);
        }

        if (intProduct != Integer.MIN_VALUE || longProduct != Integer.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:helper-exact-minproduct] consistency violation: expected exact intermediate -2^31"
                            + " reduced=" + reduced + " n=" + n + " intProduct=" + intProduct + " longProduct="
                            + longProduct);
        }

        boolean violated = false;
        String detail = null;
        Throwable cause = null;
        try {
            int lcm = MathUtils.lcm(Integer.MIN_VALUE, n);
            violated = true;
            detail = "completed normally with lcm=" + lcm + " for n=" + n + " after helper established exact intermediate Integer.MIN_VALUE";
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            violated = true;
            detail = "wrong exception type " + t.getClass().getName() + " for n=" + n;
            cause = t;
        }
        if (violated) {
            if (cause != null) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:helper-exact-minproduct] post-condition violation: lcm must reject because the real helper path computes an exact intermediate of Integer.MIN_VALUE, whose absolute value is not representable as a nonnegative int; "
                                + detail,
                        cause);
            }
            throw new FuzzerSecurityIssueLow(
                    "[oracle:helper-exact-minproduct] post-condition violation: lcm must reject because the real helper path computes an exact intermediate of Integer.MIN_VALUE, whose absolute value is not representable as a nonnegative int; "
                            + detail);
        }
    }

    private static void expectArithmeticFromLcm(String oracleId, int a, int b) {
        boolean violated = false;
        String detail = null;
        Throwable cause = null;
        try {
            int actual = MathUtils.lcm(a, b);
            violated = true;
            detail = "expected ArithmeticException but returned " + actual + " for MathUtils.lcm(" + a + ", " + b + ")";
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            violated = true;
            detail = "expected ArithmeticException but got " + t.getClass().getName() + " for MathUtils.lcm(" + a + ", " + b + ")";
            cause = t;
        }
        if (violated) {
            if (cause != null) {
                throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + detail, cause);
            }
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + detail);
        }
    }

    private static void assertEqualsOracle(String oracleId, int expected, int actual, String call) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + call + " expected=" + expected + " actual=" + actual);
        }
    }
}