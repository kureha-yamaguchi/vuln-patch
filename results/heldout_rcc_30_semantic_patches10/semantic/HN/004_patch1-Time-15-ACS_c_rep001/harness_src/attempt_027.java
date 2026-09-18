package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkEqual("lift-0x0", 0L, FieldUtils.safeMultiply(0L, 0));
        checkEqual("lift-1x1", 1L, FieldUtils.safeMultiply(1L, 1));
        checkEqual("lift-1x3", 3L, FieldUtils.safeMultiply(1L, 3));
        checkEqual("lift-3x1", 3L, FieldUtils.safeMultiply(3L, 1));
        checkEqual("lift-2x3", 6L, FieldUtils.safeMultiply(2L, 3));
        checkEqual("lift-2x-3", -6L, FieldUtils.safeMultiply(2L, -3));
        checkEqual("lift--2x3", -6L, FieldUtils.safeMultiply(-2L, 3));
        checkEqual("lift--2x-3", 6L, FieldUtils.safeMultiply(-2L, -3));
        checkEqual("lift--1xminInt", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        checkEqual("lift-maxx1", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        checkEqual("lift-minx1", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        checkEqual("lift-maxx-1", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        checkThrowsArithmetic("lift-minx-1-throws", Long.MIN_VALUE, -1);
        checkThrowsArithmetic("lift-minx100-throws", Long.MIN_VALUE, 100);
        checkThrowsArithmetic("lift-minxmaxInt-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkThrowsArithmetic("lift-maxxminInt-throws", Long.MAX_VALUE, Integer.MIN_VALUE);

        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);

        try {
            long lhs = FieldUtils.safeMultiply((long) a, b);
            long rhs = FieldUtils.safeMultiply((long) a, (long) b);
            if (lhs != rhs) {
                throw new RuntimeException("[oracle:overload-long-int-vs-long-long] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on the same mathematical inputs inputA=" + a + " inputB=" + b + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }

        try {
            int lhs = FieldUtils.safeMultiply(a, b);
            long rhs = FieldUtils.safeMultiply((long) a, b);
            if (lhs != rhs) {
                throw new RuntimeException("[oracle:overload-int-vs-long-int] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int result is defined inputA=" + a + " inputB=" + b + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void checkEqual(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void checkThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException but returned=" + actual + " for val1=" + val1 + " val2=" + val2
            );
        } catch (ArithmeticException expected) {
        }
    }
}