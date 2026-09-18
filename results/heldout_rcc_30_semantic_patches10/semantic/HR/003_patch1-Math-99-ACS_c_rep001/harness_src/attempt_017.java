package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        observeLiftedGcdTestWithoutReporting();
        checkZeroAbsAgreementAcrossApis(data);
        drivePatchedBoundaryWithoutAssertingOnRejections(data);
    }

    private static void observeLiftedGcdTestWithoutReporting() {
        int a = 30;
        int b = 50;
        int c = 77;

        requireEquals("lifted-gcd-0-0", 0, MathUtils.gcd(0, 0));
        requireEquals("lifted-gcd-0-b", b, MathUtils.gcd(0, b));
        requireEquals("lifted-gcd-a-0", a, MathUtils.gcd(a, 0));
        requireEquals("lifted-gcd-0-negb", b, MathUtils.gcd(0, -b));
        requireEquals("lifted-gcd-nega-0", a, MathUtils.gcd(-a, 0));

        requireEquals("lifted-gcd-a-b", 10, MathUtils.gcd(a, b));
        requireEquals("lifted-gcd-nega-b", 10, MathUtils.gcd(-a, b));
        requireEquals("lifted-gcd-a-negb", 10, MathUtils.gcd(a, -b));
        requireEquals("lifted-gcd-nega-negb", 10, MathUtils.gcd(-a, -b));

        requireEquals("lifted-gcd-a-c", 1, MathUtils.gcd(a, c));
        requireEquals("lifted-gcd-nega-c", 1, MathUtils.gcd(-a, c));
        requireEquals("lifted-gcd-a-negc", 1, MathUtils.gcd(a, -c));
        requireEquals("lifted-gcd-nega-negc", 1, MathUtils.gcd(-a, -c));

        requireEquals("lifted-gcd-scale", 3 * (1 << 15), MathUtils.gcd(3 * (1 << 20), 9 * (1 << 15)));
        requireEquals("lifted-gcd-max-0", Integer.MAX_VALUE, MathUtils.gcd(Integer.MAX_VALUE, 0));
        requireEquals("lifted-gcd-negmax-0", Integer.MAX_VALUE, MathUtils.gcd(-Integer.MAX_VALUE, 0));
        requireEquals("lifted-gcd-minpower", 1 << 30, MathUtils.gcd(1 << 30, -Integer.MIN_VALUE));

        try {
            MathUtils.gcd(Integer.MIN_VALUE, 0);
        } catch (ArithmeticException expected) {
        }
        try {
            MathUtils.gcd(0, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
        }
        try {
            MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
        } catch (ArithmeticException expected) {
        }
    }

    private static void checkZeroAbsAgreementAcrossApis(FuzzedDataProvider data) {
        int neighborDelta = data.consumeInt(1, 4);
        int fuzzSafe = data.consumeInt(-1_000_000, 1_000_000);

        int[] xs = new int[] {
            Integer.MIN_VALUE + neighborDelta,
            -neighborDelta,
            fuzzSafe
        };

        for (int x : xs) {
            // Contract used: gcd(x,0) returns Math.abs(x)+0 for all representable cases in the zero branch,
            // and lcm(x,1) returns the least common multiple of |x| and 1, which is |x|.
            // For every x != Integer.MIN_VALUE, abs(x) is representable as an int, so both real API calls must
            // succeed and agree on exactly the same value. A throw-deleting or overfit boundary patch near MIN_VALUE
            // can leave one path wrong while the other remains correct.
            int gcdValue;
            int lcmValue;
            try {
                gcdValue = MathUtils.gcd(x, 0);
                lcmValue = MathUtils.lcm(x, 1);
            } catch (ArithmeticException rejected) {
                return;
            }

            int expectedAbs = (int) Math.abs((long) x);
            if (gcdValue != expectedAbs || lcmValue != expectedAbs || gcdValue != lcmValue) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:zero-abs-crosscheck] consistency violation: x=" + x
                        + " expectedAbs=" + expectedAbs
                        + " gcd(x,0)=" + gcdValue
                        + " lcm(x,1)=" + lcmValue);
            }
        }
    }

    private static void drivePatchedBoundaryWithoutAssertingOnRejections(FuzzedDataProvider data) {
        int choose = data.consumeInt(0, 3);
        int near = Integer.MIN_VALUE + data.consumeInt(0, 8);

        try {
            if (choose == 0) {
                MathUtils.gcd(Integer.MIN_VALUE, 0);
            } else if (choose == 1) {
                MathUtils.gcd(0, Integer.MIN_VALUE);
            } else if (choose == 2) {
                MathUtils.gcd(Integer.MIN_VALUE, Integer.MIN_VALUE);
            } else {
                MathUtils.gcd(near, 0);
                MathUtils.lcm(near, 1);
            }
        } catch (ArithmeticException expected) {
        }
    }

    private static void requireEquals(String id, int expected, int actual) {
        if (expected != actual) {
            throw new RuntimeException(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }
}