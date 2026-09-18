package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedTestOracles();

        long fuzzLong = data.consumeLong();
        int fuzzInt = data.consumeInt();
        int fuzzIntA = data.consumeInt();
        int fuzzIntB = data.consumeInt();

        relationSafeMultiplyLongIntAgreesWithLongLongSibling(fuzzLong, fuzzInt);
        relationSafeMultiplyIntIntAgreesWithLongIntSibling(fuzzIntA, fuzzIntB);
        relationIdentityAndZero(fuzzLong);
    }

    private static void liftedTestOracles() {
        assertEqualsLong("lifted-0", 0L, FieldUtils.safeMultiply(0L, 0));

        assertEqualsLong("lifted-1", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("lifted-2", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("lifted-3", 3L, FieldUtils.safeMultiply(3L, 1));

        assertEqualsLong("lifted-4", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("lifted-5", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("lifted-6", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("lifted-7", 6L, FieldUtils.safeMultiply(-2L, -3));

        assertEqualsLong("lifted-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        assertEqualsLong("lifted-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        assertThrowsArithmetic("lifted-12", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("lifted-13", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("lifted-14", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("lifted-15", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void relationSafeMultiplyLongIntAgreesWithLongLongSibling(long val1, int val2) {
        long expected;
        try {
            expected = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Throwable t) {
            return;
        }

        long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            return;
        }

        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:long-int-vs-long-long] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                    + val1 + " inputVal2=" + val2 + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static void relationSafeMultiplyIntIntAgreesWithLongIntSibling(int a, int b) {
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Throwable t) {
            return;
        }

        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Throwable t) {
            return;
        }

        if (longResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:int-int-vs-long-int] metamorphic violation: safeMultiply(int,int) must agree with widened safeMultiply(long,int) when the int result is valid inputA="
                    + a + " inputB=" + b + " lhs=" + longResult + " rhs=" + intResult);
        }
    }

    private static void relationIdentityAndZero(long x) {
        long timesOne;
        try {
            timesOne = FieldUtils.safeMultiply(x, 1);
        } catch (Throwable t) {
            return;
        }
        if (timesOne != x) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:identity-one] metamorphic violation: documented multiplication semantics require multiplying by 1 to preserve the value input="
                    + x + " lhs=" + timesOne + " rhs=" + x);
        }

        long timesZero;
        try {
            timesZero = FieldUtils.safeMultiply(x, 0);
        } catch (Throwable t) {
            return;
        }
        if (timesZero != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:zero-annihilator] metamorphic violation: documented multiplication semantics require multiplying by 0 to produce 0 input="
                    + x + " lhs=" + timesZero + " rhs=0");
        }

        long lhs;
        long rhs;
        try {
            lhs = FieldUtils.safeMultiply(x, -1);
            rhs = -FieldUtils.safeMultiply(x, 1);
        } catch (Throwable t) {
            return;
        }
        if (lhs != rhs) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negation-consistency] metamorphic violation: for non-rejected inputs, multiplying by -1 must equal negating the multiply-by-1 result input="
                    + x + " lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException but returned=" + actual
                    + " for val1=" + val1 + " val2=" + val2);
        } catch (ArithmeticException expected) {
            return;
        }
    }
}