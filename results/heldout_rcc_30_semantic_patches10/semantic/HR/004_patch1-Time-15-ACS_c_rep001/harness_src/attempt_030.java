package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();

        long fuzzVal = (((long) data.consumeInt(-1_000_000, 1_000_000)) * 1_000_001L)
                + data.consumeInt(-1_000_000, 1_000_000);
        int fuzzInt = data.consumeInt(-1_000_000, 1_000_000);

        try {
            long lhs = FieldUtils.safeMultiply(fuzzVal, fuzzInt);
            long rhs = FieldUtils.safeMultiply(fuzzVal, (long) fuzzInt);
            /* Documented guarantee: the same-name safeMultiply overloads "Multiply two values
             * throwing an exception if overflow occurs." For equivalent mathematical inputs that
             * do not overflow, both real library calls must compute the same product. A patch
             * that merely deletes/avoids the overflow throw or silently returns a wrong value in
             * one overload breaks this sibling-agreement post-condition. */
            if (lhs != rhs) {
                throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long)"
                                + " inputVal=" + fuzzVal
                                + " inputInt=" + fuzzInt
                                + " lhs=" + lhs
                                + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkLiftedTestOracles() {
        checkEqualsLong("eq-0x0", 0L, FieldUtils.safeMultiply(0L, 0));
        checkEqualsLong("eq-1x1", 1L, FieldUtils.safeMultiply(1L, 1));
        checkEqualsLong("eq-1x3", 3L, FieldUtils.safeMultiply(1L, 3));
        checkEqualsLong("eq-3x1", 3L, FieldUtils.safeMultiply(3L, 1));

        checkEqualsLong("eq-2x3", 6L, FieldUtils.safeMultiply(2L, 3));
        checkEqualsLong("eq-2x-3", -6L, FieldUtils.safeMultiply(2L, -3));
        checkEqualsLong("eq--2x3", -6L, FieldUtils.safeMultiply(-2L, 3));
        checkEqualsLong("eq--2x-3", 6L, FieldUtils.safeMultiply(-2L, -3));

        checkEqualsLong("eq--1xminint", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        checkEqualsLong("eq-maxx1", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        checkEqualsLong("eq-minx1", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        checkEqualsLong("eq-maxx-1", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        expectArithmetic("throw-minx-1", Long.MIN_VALUE, -1);
        expectArithmetic("throw-minx100", Long.MIN_VALUE, 100);
        expectArithmetic("throw-minxmaxint", Long.MIN_VALUE, Integer.MAX_VALUE);
        expectArithmetic("throw-maxxminint", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply(long,int)"
                            + " expected=" + expected
                            + " actual=" + actual);
        }
    }

    private static void expectArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException"
                            + " but returned=" + actual
                            + " for val1=" + val1
                            + " val2=" + val2);
        } catch (ArithmeticException expected) {
        }
    }
}