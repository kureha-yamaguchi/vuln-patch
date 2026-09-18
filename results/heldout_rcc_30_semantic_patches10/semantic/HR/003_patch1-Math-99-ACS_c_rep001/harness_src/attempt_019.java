package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedLcmTestOracles();
        reachPatchedGcdBoundary();
        runMulDistributiveOracle(data);
    }

    private static void runLiftedLcmTestOracles() {
        int a = 30;
        int b = 50;
        int c = 77;

        assertEqualsOracle("lifted-lcm-0b", 0, MathUtils.lcm(0, b), "MathUtils.lcm(0, " + b + ")");
        assertEqualsOracle("lifted-lcm-a0", 0, MathUtils.lcm(a, 0), "MathUtils.lcm(" + a + ", 0)");
        assertEqualsOracle("lifted-lcm-1b", b, MathUtils.lcm(1, b), "MathUtils.lcm(1, " + b + ")");
        assertEqualsOracle("lifted-lcm-a1", a, MathUtils.lcm(a, 1), "MathUtils.lcm(" + a + ", 1)");
        assertEqualsOracle("lifted-lcm-ab", 150, MathUtils.lcm(a, b), "MathUtils.lcm(" + a + ", " + b + ")");
        assertEqualsOracle("lifted-lcm-negab", 150, MathUtils.lcm(-a, b), "MathUtils.lcm(" + (-a) + ", " + b + ")");
        assertEqualsOracle("lifted-lcm-anegb", 150, MathUtils.lcm(a, -b), "MathUtils.lcm(" + a + ", " + (-b) + ")");
        assertEqualsOracle("lifted-lcm-neganegb", 150, MathUtils.lcm(-a, -b), "MathUtils.lcm(" + (-a) + ", " + (-b) + ")");
        assertEqualsOracle("lifted-lcm-ac", 2310, MathUtils.lcm(a, c), "MathUtils.lcm(" + a + ", " + c + ")");
        assertEqualsOracle("lifted-lcm-scale", (1 << 20) * 15, MathUtils.lcm((1 << 20) * 3, (1 << 20) * 5),
                "MathUtils.lcm(" + ((1 << 20) * 3) + ", " + ((1 << 20) * 5) + ")");
        assertEqualsOracle("lifted-lcm-00", 0, MathUtils.lcm(0, 0), "MathUtils.lcm(0, 0)");

        expectArithmeticOracle("lifted-lcm-min-1-throws", Integer.MIN_VALUE, 1);
        expectArithmeticOracle("lifted-lcm-min-pow-throws", Integer.MIN_VALUE, 1 << 20);
        expectArithmeticOracle("lifted-lcm-max-maxminus1-throws", Integer.MAX_VALUE, Integer.MAX_VALUE - 1);
    }

    private static void reachPatchedGcdBoundary() {
        expectArithmeticFromGcd("gcd-min-left-zero", Integer.MIN_VALUE, 0);
        expectArithmeticFromGcd("gcd-zero-right-min", 0, Integer.MIN_VALUE);
        expectArithmeticFromGcd("gcd-min-both", Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    private static void runMulDistributiveOracle(FuzzedDataProvider data) {
        int x = data.consumeInt(-1000, 1000);
        int y = data.consumeInt(-1000, 1000);
        int z = data.consumeInt(-1000, 1000);

        int lhs;
        int rhs;
        try {
            int yPlusZ = MathUtils.addAndCheck(y, z);
            lhs = MathUtils.mulAndCheck(x, yPlusZ);
            int xy = MathUtils.mulAndCheck(x, y);
            int xz = MathUtils.mulAndCheck(x, z);
            rhs = MathUtils.addAndCheck(xy, xz);
        } catch (Throwable t) {
            return;
        }

        if (lhs != rhs) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:mul-distributive-safe] metamorphic violation: "
                            + "for overflow-free int arithmetic, multiplication distributes over addition; "
                            + "mulAndCheck(x, addAndCheck(y, z)) must equal addAndCheck(mulAndCheck(x, y), mulAndCheck(x, z)) "
                            + "input={x=" + x + ",y=" + y + ",z=" + z + "} lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static void assertEqualsOracle(String id, int expected, int actual, String call) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + id + "] semantic mismatch: " + call + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmeticOracle(String id, int a, int b) {
        boolean violated = false;
        try {
            int actual = MathUtils.lcm(a, b);
            violated = true;
            if (violated) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:" + id + "] semantic mismatch: expected ArithmeticException from MathUtils.lcm(" + a + ", " + b
                                + ") but returned " + actual);
            }
        } catch (ArithmeticException expected) {
            return;
        }
    }

    private static void expectArithmeticFromGcd(String id, int a, int b) {
        boolean violated = false;
        try {
            int actual = MathUtils.gcd(a, b);
            violated = true;
            if (violated) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:" + id + "] semantic mismatch: expected ArithmeticException from MathUtils.gcd(" + a + ", " + b
                                + ") but returned " + actual);
            }
        } catch (ArithmeticException expected) {
            return;
        }
    }
}