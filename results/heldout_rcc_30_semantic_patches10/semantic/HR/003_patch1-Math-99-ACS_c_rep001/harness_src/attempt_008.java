package org.apache.commons.math.util;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int selector = data.consumeInt(0, 4);
        switch (selector) {
            case 0:
                expectArithmeticExceptionFromGcd(Integer.MIN_VALUE, 0, "gcd-min-zero");
                return;
            case 1:
                expectArithmeticExceptionFromGcd(0, Integer.MIN_VALUE, "gcd-zero-min");
                return;
            case 2:
                expectArithmeticExceptionFromGcd(Integer.MIN_VALUE, Integer.MIN_VALUE, "gcd-min-min");
                return;
            case 3:
                expectArithmeticExceptionFromLcm(Integer.MIN_VALUE, 1, "lcm-min-one");
                return;
            default:
                expectArithmeticExceptionFromLcm(1, Integer.MIN_VALUE, "lcm-one-min");
        }
    }

    private static void expectArithmeticExceptionFromGcd(int a, int b, String oracleId) {
        try {
            int actual = MathUtils.gcd(a, b);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from MathUtils.gcd(" + a + "," + b + ") but got " + actual);
        } catch (ArithmeticException expected) {
        }
    }

    private static void expectArithmeticExceptionFromLcm(int a, int b, String oracleId) {
        try {
            int actual = MathUtils.lcm(a, b);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from MathUtils.lcm(" + a + "," + b + ") but got " + actual);
        } catch (ArithmeticException expected) {
        }
    }
}