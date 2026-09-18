package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkEquals("seed-0", 0L, FieldUtils.safeMultiply(0L, 0));
        checkEquals("seed-1", 1L, FieldUtils.safeMultiply(1L, 1));
        checkEquals("seed-2", 3L, FieldUtils.safeMultiply(1L, 3));
        checkEquals("seed-3", 3L, FieldUtils.safeMultiply(3L, 1));
        checkEquals("seed-4", 6L, FieldUtils.safeMultiply(2L, 3));
        checkEquals("seed-5", -6L, FieldUtils.safeMultiply(2L, -3));
        checkEquals("seed-6", -6L, FieldUtils.safeMultiply(-2L, 3));
        checkEquals("seed-7", 6L, FieldUtils.safeMultiply(-2L, -3));
        checkEquals("seed-8", -1L * (long) Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        checkEquals("seed-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        checkEquals("seed-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        checkEquals("seed-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        checkThrowsArithmetic("seed-throw-0", Long.MIN_VALUE, -1);
        checkThrowsArithmetic("seed-throw-1", Long.MIN_VALUE, 100);
        checkThrowsArithmetic("seed-throw-2", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkThrowsArithmetic("seed-throw-3", Long.MAX_VALUE, Integer.MIN_VALUE);

        long moderateVal = data.consumeInt(-1_000_000, 1_000_000);
        int moderateInt = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply(moderateVal, moderateInt);
            long rhs = FieldUtils.safeMultiply(moderateVal, (long) moderateInt);
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on the same mathematical inputs inputVal="
                        + moderateVal + " inputInt=" + moderateInt + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException e) {
            return;
        }

        long composedBase = data.consumeInt(-1_000_000, 1_000_000);
        int a = data.consumeInt(-1000, 1000);
        int b = data.consumeInt(-1000, 1000);

        try {
            long left = FieldUtils.safeMultiply(composedBase, a + b);
            long right = FieldUtils.safeAdd(FieldUtils.safeMultiply(composedBase, a), FieldUtils.safeMultiply(composedBase, b));
            if (left != right) {
                throw new RuntimeException(
                    "[oracle:distributive-compose] metamorphic violation: safeMultiply should distribute over safeAdd for exact non-overflowing long arithmetic inputVal="
                        + composedBase + " a=" + a + " b=" + b + " lhs=" + left + " rhs=" + right);
            }
        } catch (ArithmeticException e) {
            return;
        }
    }

    private static void checkEquals(String id, long expected, long actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void checkThrowsArithmetic(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException actualReturn=" + actual
                    + " inputVal1=" + val1 + " inputVal2=" + val2);
        } catch (ArithmeticException expected) {
        }
    }
}