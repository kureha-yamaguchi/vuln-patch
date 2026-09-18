package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertEqualsLong("seed-0x0", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("seed-1x1", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("seed-1x3", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("seed-3x1", 3L, FieldUtils.safeMultiply(3L, 1));
        assertEqualsLong("seed-2x3", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("seed-2x-3", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("seed--2x3", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("seed--2x-3", 6L, FieldUtils.safeMultiply(-2L, -3));
        assertEqualsLong("seed--1x-intmin", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));
        assertEqualsLong("seed-longmaxx1", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("seed-longminx1", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("seed-longmaxx-1", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        expectArithmeticException("seed-longminx-1-throws", Long.MIN_VALUE, -1);
        expectArithmeticException("seed-longminx100-throws", Long.MIN_VALUE, 100);
        expectArithmeticException("seed-longminx-intmax-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        expectArithmeticException("seed-longmaxx-intmin-throws", Long.MAX_VALUE, Integer.MIN_VALUE);

        long fuzzLong;
        int selector = data.consumeInt(0, 4);
        if (selector == 0) {
            fuzzLong = data.consumeInt(-1_000_000, 1_000_000);
        } else if (selector == 1) {
            fuzzLong = Long.MIN_VALUE;
        } else if (selector == 2) {
            fuzzLong = Long.MAX_VALUE;
        } else if (selector == 3) {
            fuzzLong = -1L;
        } else {
            fuzzLong = data.consumeInt();
        }

        int fuzzInt;
        int selector2 = data.consumeInt(0, 6);
        if (selector2 == 0) {
            fuzzInt = -1;
        } else if (selector2 == 1) {
            fuzzInt = 0;
        } else if (selector2 == 2) {
            fuzzInt = 1;
        } else if (selector2 == 3) {
            fuzzInt = data.consumeInt(-1_000_000, 1_000_000);
        } else if (selector2 == 4) {
            fuzzInt = Integer.MIN_VALUE;
        } else if (selector2 == 5) {
            fuzzInt = Integer.MAX_VALUE;
        } else {
            fuzzInt = data.consumeInt();
        }

        try {
            FieldUtils.safeMultiply(fuzzLong, fuzzInt);
        } catch (ArithmeticException ignored) {
        }

        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);
        try {
            int intResult = FieldUtils.safeMultiply(a, b);
            long longIntResult = FieldUtils.safeMultiply((long) a, b);
            long longLongResult = FieldUtils.safeMultiply((long) a, (long) b);

            /* Contract used: the three safeMultiply overloads are documented as the same operation
               on the same mathematical input space, differing only by parameter types/return width.
               For operands that fit in int-result range, all real overloads must agree exactly.
               A "fix" that merely suppresses/changes one branch in long,int would violate this. */
            if ((long) intResult != longIntResult) {
                throw new RuntimeException("[oracle:overload-int-vs-longint] metamorphic violation: safeMultiply(int,int) and safeMultiply(long,int) disagree inputA=" + a + " inputB=" + b + " lhs=" + intResult + " rhs=" + longIntResult);
            }
            if (longIntResult != longLongResult) {
                throw new RuntimeException("[oracle:overload-longint-vs-longlong] metamorphic violation: safeMultiply(long,int) and safeMultiply(long,long) disagree inputA=" + a + " inputB=" + b + " lhs=" + longIntResult + " rhs=" + longLongResult);
            }
        } catch (ArithmeticException ignored) {
            return;
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void expectArithmeticException(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException actualReturn=" + actual + " inputVal1=" + val1 + " inputVal2=" + val2
            );
        } catch (ArithmeticException expected) {
        }
    }
}