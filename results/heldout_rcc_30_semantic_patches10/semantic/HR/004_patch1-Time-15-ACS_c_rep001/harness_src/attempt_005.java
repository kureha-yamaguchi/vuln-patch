package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkSeedGeneralization();
        checkSiblingAgreementLongIntVsLongLong(data);
        checkSiblingAgreementIntIntVsLongInt(data);
        checkConstructedInputOracle(data);
    }

    private static void checkLiftedTestOracles() {
        assertEqualsLong("lifted-0x0", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("lifted-1x1", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("lifted-1x3", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("lifted-3x1", 3L, FieldUtils.safeMultiply(3L, 1));

        assertEqualsLong("lifted-2x3", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("lifted-2x-3", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("lifted--2x3", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("lifted--2x-3", 6L, FieldUtils.safeMultiply(-2L, -3));

        assertEqualsLong("lifted--1xIntegerMin", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        assertEqualsLong("lifted-longMaxx1", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("lifted-longMinx1", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("lifted-longMaxx-1", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        assertThrowsArithmetic("lifted-longMinx-1-throws", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("lifted-longMinx100-throws", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("lifted-longMinxIntegerMax-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("lifted-longMaxxIntegerMin-throws", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkSeedGeneralization() {
        // Contract justification: safeMultiply documents multiplication with overflow checking.
        // For multiplier 1, the method body explicitly returns val1; any patch that merely removes
        // the throw or makes a branch unreachable must still preserve this observable identity.
        long[] values = new long[] {
            Long.MIN_VALUE,
            Long.MAX_VALUE,
            -1L,
            0L,
            1L,
            2L,
            -2L,
            123456789L,
            -987654321L
        };
        for (int i = 0; i < values.length; i++) {
            long v = values[i];
            assertEqualsLong("postcond-identity-" + i, v, FieldUtils.safeMultiply(v, 1));
        }

        // Contract justification: multiplying by zero yields zero and is explicitly handled in code.
        for (int i = 0; i < values.length; i++) {
            long v = values[i];
            assertEqualsLong("postcond-zero-" + i, 0L, FieldUtils.safeMultiply(v, 0));
        }

        // Contract justification: for any val1 other than Long.MIN_VALUE, multiplication by -1 is
        // numeric negation. This post-condition would be violated by a silent wrong-value patch.
        for (int i = 0; i < values.length; i++) {
            long v = values[i];
            if (v != Long.MIN_VALUE) {
                assertEqualsLong("postcond-negate-" + i, -v, FieldUtils.safeMultiply(v, -1));
            }
        }
    }

    private static void checkSiblingAgreementLongIntVsLongLong(FuzzedDataProvider data) {
        long val1 = data.consumeInt(-1_000_000, 1_000_000);
        int val2 = data.consumeInt(-1_000_000, 1_000_000);

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
            throw new FuzzerSecurityIssueLow(
                "[oracle:safeMultiplyLongInt_agreesWithLongLongSibling] metamorphic violation: safeMultiply(long,int) disagrees with safeMultiply(long,long) inputVal1="
                    + val1 + " inputVal2=" + val2 + " lhs=" + actual + " rhs=" + expected);
        }
    }

    private static void checkSiblingAgreementIntIntVsLongInt(FuzzedDataProvider data) {
        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);

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
            throw new FuzzerSecurityIssueLow(
                "[oracle:safeMultiplyIntInt_agreesWithLongIntSibling] metamorphic violation: safeMultiply(int,int) disagrees with safeMultiply(long,int) inputA="
                    + a + " inputB=" + b + " lhs=" + longResult + " rhs=" + intResult);
        }
    }

    private static void checkConstructedInputOracle(FuzzedDataProvider data) {
        // Oracle-from-input justification: we choose n first, then build the canonical equivalent
        // expression n * (-1). For every n except Integer.MIN_VALUE, safeMultiply(int,int) must
        // equal safeNegate(n); both are real library calls from the safe* family with the same
        // documented overflow semantics.
        int n = data.consumeInt(-1_000_000, 1_000_000);
        if (n == Integer.MIN_VALUE) {
            return;
        }

        int negated;
        try {
            negated = FieldUtils.safeNegate(n);
        } catch (Throwable t) {
            return;
        }

        int multiplied;
        try {
            multiplied = FieldUtils.safeMultiply(n, -1);
        } catch (Throwable t) {
            return;
        }

        if (multiplied != negated) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:safeMultiply_vs_safeNegate] metamorphic violation: safeMultiply(n,-1) must equal safeNegate(n) inputN="
                    + n + " lhs=" + multiplied + " rhs=" + negated);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException but returned=" + actual
                    + " for val1=" + val1 + " val2=" + val2);
        } catch (ArithmeticException expected) {
        }
    }
}