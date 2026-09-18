package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLcmFrontierDoubling(data);
        checkGcdFrontierDoubling();
        runLiftedTestLcmOraclesUnique();
    }

    private static void checkLcmFrontierDoubling(FuzzedDataProvider data) {
        int k;
        boolean neg;
        try {
            k = data.consumeInt(0, 30);
            neg = data.consumeBoolean();
        } catch (Throwable t) {
            return;
        }

        int n = 1 << k;
        if (neg) {
            n = -n;
        }

        final int frontier = -(1 << 30);
        final int expectedSafe = 1 << 30;

        int safe;
        try {
            safe = MathUtils.lcm(frontier, n);
        } catch (Throwable t) {
            return;
        }

        if (safe != expectedSafe) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:lcm-frontier-safe] semantic mismatch: MathUtils.lcm(" + frontier + "," + n + ") expected=" + expectedSafe + " actual=" + safe
            );
        }

        int risky;
        try {
            risky = MathUtils.lcm(Integer.MIN_VALUE, n);
        } catch (ArithmeticException e) {
            return;
        } catch (Throwable t) {
            return;
        }

        long exactDoubled = 2L * safe;
        throw new FuzzerSecurityIssueLow(
            "[oracle:lcm-frontier-doubling] metamorphic violation: for power-of-two partner n=" + n
                + ", doubling the first argument from " + frontier + " to " + Integer.MIN_VALUE
                + " doubles the exact mathematical lcm from " + safe + " to " + exactDoubled
                + "; because " + exactDoubled + " is not representable as int, MathUtils.lcm must reject with ArithmeticException instead of returning "
                + risky
        );
    }

    private static void checkGcdFrontierDoubling() {
        final int safeInput = -(1 << 30);
        final int safeExpected = 1 << 30;

        int safe;
        try {
            safe = MathUtils.gcd(safeInput, 0);
        } catch (Throwable t) {
            return;
        }

        if (safe != safeExpected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:gcd-frontier-safe] semantic mismatch: MathUtils.gcd(" + safeInput + ",0) expected=" + safeExpected + " actual=" + safe
            );
        }

        int risky;
        try {
            risky = MathUtils.gcd(Integer.MIN_VALUE, 0);
        } catch (ArithmeticException e) {
            return;
        } catch (Throwable t) {
            return;
        }

        long exactDoubled = 2L * safe;
        throw new FuzzerSecurityIssueLow(
            "[oracle:gcd-frontier-doubling] metamorphic violation: doubling the first argument from " + safeInput + " to "
                + Integer.MIN_VALUE + " with second argument 0 doubles the exact gcd from " + safe + " to " + exactDoubled
                + "; since " + exactDoubled + " is not representable as int, MathUtils.gcd must reject with ArithmeticException instead of returning "
                + risky
        );
    }

    private static void runLiftedTestLcmOraclesUnique() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEquals("lifted-lcm-0b", 0, MathUtils.lcm(0, b));
        assertEquals("lifted-lcm-a0", 0, MathUtils.lcm(a, 0));
        assertEquals("lifted-lcm-1b", b, MathUtils.lcm(1, b));
        assertEquals("lifted-lcm-a1", a, MathUtils.lcm(a, 1));
        assertEquals("lifted-lcm-ab", 150, MathUtils.lcm(a, b));
        assertEquals("lifted-lcm-negab", 150, MathUtils.lcm(-a, b));
        assertEquals("lifted-lcm-anegb", 150, MathUtils.lcm(a, -b));
        assertEquals("lifted-lcm-neganeg", 150, MathUtils.lcm(-a, -b));
        assertEquals("lifted-lcm-ac", 2310, MathUtils.lcm(a, c));
        assertEquals("lifted-lcm-scale", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5));
        assertEquals("lifted-lcm-00", 0, MathUtils.lcm(0, 0));

        expectArithmeticUnique("lifted-lcm-min-1-throws", Integer.MIN_VALUE, 1);
        expectArithmeticUnique("lifted-lcm-min-pow-throws", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticUnique("lifted-lcm-max-maxminus-throws", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void expectArithmeticUnique(String oracleId, int x, int y) {
        try {
            MathUtils.lcm(x, y);
        } catch (ArithmeticException e) {
            return;
        } catch (Throwable t) {
            return;
        }

        throw new FuzzerSecurityIssueLow(
            "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from MathUtils.lcm(" + x + "," + y + ")"
        );
    }

    private static void assertEquals(String oracleId, int expected, int actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual
            );
        }
    }
}