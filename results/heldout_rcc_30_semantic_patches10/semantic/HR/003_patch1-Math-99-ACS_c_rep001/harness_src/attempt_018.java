package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int safeA = data.consumeInt(1, 16);
        int safeB = data.consumeInt(1, 16);

        MathUtils.gcd(safeA, 0);
        MathUtils.lcm(safeA, safeB);

        if (data.consumeBoolean()) {
            checkGcdMustReject(Integer.MIN_VALUE, 0, "gcd-min-left-zero");
            checkGcdMustReject(0, Integer.MIN_VALUE, "gcd-zero-right-min");
            checkLcmMustReject(Integer.MIN_VALUE, 1, "lcm-min-left-one");
            checkLcmMustReject(1, Integer.MIN_VALUE, "lcm-one-right-min");
        } else {
            checkLcmMustReject(Integer.MIN_VALUE, 1, "lcm-min-left-one");
            checkLcmMustReject(1, Integer.MIN_VALUE, "lcm-one-right-min");
            checkGcdMustReject(Integer.MIN_VALUE, 0, "gcd-min-left-zero");
            checkGcdMustReject(0, Integer.MIN_VALUE, "gcd-zero-right-min");
        }
    }

    private static void checkGcdMustReject(int a, int b, String id) {
        try {
            int actual = MathUtils.gcd(a, b);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: lifted test requires ArithmeticException for gcd("
                    + a + "," + b + "), but buggy code returned " + actual
                    + "; post-condition violated because gcd is mathematically non-negative and this input's true magnitude is 2^31, which cannot be represented as int");
        } catch (ArithmeticException expectedOnFixedBuild) {
            return;
        }
    }

    private static void checkLcmMustReject(int a, int b, String id) {
        try {
            int actual = MathUtils.lcm(a, b);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: documented special case requires ArithmeticException for lcm("
                    + a + "," + b + "), but buggy code returned " + actual
                    + "; post-condition violated because lcm is defined as a non-negative least common multiple, yet the returned value is negative");
        } catch (ArithmeticException expectedOnFixedBuild) {
            return;
        }
    }
}