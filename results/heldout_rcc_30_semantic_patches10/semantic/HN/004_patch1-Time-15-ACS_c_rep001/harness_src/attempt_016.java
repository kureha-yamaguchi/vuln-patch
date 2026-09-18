package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static void assertEqualsLong(String oracleId, String expr, long expected, long actual) {
        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmeticException(String oracleId, String expr, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected ArithmeticException actual=" + actual);
        } catch (ArithmeticException expected) {
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertEqualsLong("lifted-eq-0", "FieldUtils.safeMultiply(0L, 0)", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("lifted-eq-1", "FieldUtils.safeMultiply(1L, 1)", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("lifted-eq-2", "FieldUtils.safeMultiply(1L, 3)", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("lifted-eq-3", "FieldUtils.safeMultiply(3L, 1)", 3L, FieldUtils.safeMultiply(3L, 1));
        assertEqualsLong("lifted-eq-4", "FieldUtils.safeMultiply(2L, 3)", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("lifted-eq-5", "FieldUtils.safeMultiply(2L, -3)", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("lifted-eq-6", "FieldUtils.safeMultiply(-2L, 3)", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("lifted-eq-7", "FieldUtils.safeMultiply(-2L, -3)", 6L, FieldUtils.safeMultiply(-2L, -3));
        assertEqualsLong(
            "lifted-eq-8",
            "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)",
            -1L * Integer.MIN_VALUE,
            FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        assertEqualsLong(
            "lifted-eq-9",
            "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)",
            Long.MAX_VALUE,
            FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong(
            "lifted-eq-10",
            "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)",
            Long.MIN_VALUE,
            FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong(
            "lifted-eq-11",
            "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)",
            -Long.MAX_VALUE,
            FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        expectArithmeticException("lifted-throw-0", "FieldUtils.safeMultiply(Long.MIN_VALUE, -1)", Long.MIN_VALUE, -1);
        expectArithmeticException("lifted-throw-1", "FieldUtils.safeMultiply(Long.MIN_VALUE, 100)", Long.MIN_VALUE, 100);
        expectArithmeticException(
            "lifted-throw-2",
            "FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)",
            Long.MIN_VALUE,
            Integer.MAX_VALUE);
        expectArithmeticException(
            "lifted-throw-3",
            "FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)",
            Long.MAX_VALUE,
            Integer.MIN_VALUE);

        /*
         * Documented guarantee: same-name safeMultiply overloads all perform overflow-checked multiplication.
         * For the same mathematical operation Long.MIN_VALUE * -1, the long,int and long,long overloads must
         * agree on rejecting overflow. A patch that removes the throw in one overload breaks this observable
         * sibling-agreement post-condition.
         */
        Throwable exLongInt = null;
        Throwable exLongLong = null;
        long resLongInt = 0L;
        long resLongLong = 0L;
        try {
            resLongInt = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
        } catch (Throwable e) {
            exLongInt = e;
        }
        try {
            resLongLong = FieldUtils.safeMultiply(Long.MIN_VALUE, -1L);
        } catch (Throwable e) {
            exLongLong = e;
        }

        if (exLongInt == null && exLongLong == null) {
            if (resLongInt != resLongLong) {
                throw new RuntimeException(
                    "[oracle:overload-overflow] metamorphic violation: safeMultiply(long,int) and safeMultiply(long,long) disagree on result input="
                        + Long.MIN_VALUE + ",-1 lhs=" + resLongInt + " rhs=" + resLongLong);
            }
        } else if (exLongInt instanceof ArithmeticException && exLongLong instanceof ArithmeticException) {
        } else if (exLongInt == null || exLongLong == null
                || !exLongInt.getClass().equals(exLongLong.getClass())) {
            throw new RuntimeException(
                "[oracle:overload-overflow] metamorphic violation: safeMultiply(long,int) and safeMultiply(long,long) disagree on overflow handling input="
                    + Long.MIN_VALUE + ",-1 longIntException="
                    + (exLongInt == null ? "none" : exLongInt.getClass().getName())
                    + " longLongException="
                    + (exLongLong == null ? "none" : exLongLong.getClass().getName()));
        }

        int fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000, 1_000);
        long fuzzLong = fuzzVal1;

        /*
         * Documented guarantee: these overloads describe the same checked multiplication on equivalent inputs.
         * With fenced magnitudes both calls are valid, so a correct implementation must return the same long.
         */
        Throwable fuzzExLongInt = null;
        Throwable fuzzExLongLong = null;
        long lhs = 0L;
        long rhs = 0L;
        try {
            lhs = FieldUtils.safeMultiply(fuzzLong, fuzzVal2);
        } catch (Throwable e) {
            fuzzExLongInt = e;
        }
        try {
            rhs = FieldUtils.safeMultiply(fuzzLong, (long) fuzzVal2);
        } catch (Throwable e) {
            fuzzExLongLong = e;
        }

        if (fuzzExLongInt != null || fuzzExLongLong != null) {
            return;
        }

        if (lhs != rhs) {
            throw new RuntimeException(
                "[oracle:overload-eq] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long) input="
                    + fuzzLong + "," + fuzzVal2 + " lhs=" + lhs + " rhs=" + rhs);
        }
    }
}