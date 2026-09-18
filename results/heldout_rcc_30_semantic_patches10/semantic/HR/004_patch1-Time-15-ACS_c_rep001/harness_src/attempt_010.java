package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();

        long val1 = data.consumeLong();
        int val2 = data.consumeInt();
        checkSafeMultiplyLongIntAgreesWithLongLong(val1, val2);

        int a = data.consumeInt();
        int b = data.consumeInt();
        checkSafeMultiplyIntIntAgreesWithLongInt(a, b);

        int x = data.consumeInt(-1_000_000, 1_000_000);
        int y = data.consumeInt(-1_000_000, 1_000_000);
        checkCommutativityForModerateInputs(x, y);
    }

    private static void checkLiftedOracles() {
        assertSafeMultiplyEquals("lifted-0x0", 0L, 0L, 0);
        assertSafeMultiplyEquals("lifted-1x1", 1L, 1L, 1);
        assertSafeMultiplyEquals("lifted-1x3", 3L, 1L, 3);
        assertSafeMultiplyEquals("lifted-3x1", 3L, 3L, 1);

        assertSafeMultiplyEquals("lifted-2x3", 6L, 2L, 3);
        assertSafeMultiplyEquals("lifted-2xneg3", -6L, 2L, -3);
        assertSafeMultiplyEquals("lifted-neg2x3", -6L, -2L, 3);
        assertSafeMultiplyEquals("lifted-neg2xneg3", 6L, -2L, -3);

        assertSafeMultiplyEquals(
                "lifted-neg1xminint",
                -1L * Integer.MIN_VALUE,
                -1L,
                Integer.MIN_VALUE);

        assertSafeMultiplyEquals("lifted-maxx1", Long.MAX_VALUE, Long.MAX_VALUE, 1);
        assertSafeMultiplyEquals("lifted-minx1", Long.MIN_VALUE, Long.MIN_VALUE, 1);
        assertSafeMultiplyEquals("lifted-maxxneg1", -Long.MAX_VALUE, Long.MAX_VALUE, -1);

        assertSafeMultiplyThrowsArithmetic("lifted-minxneg1-throws", Long.MIN_VALUE, -1);
        assertSafeMultiplyThrowsArithmetic("lifted-minx100-throws", Long.MIN_VALUE, 100);
        assertSafeMultiplyThrowsArithmetic("lifted-minxintmax-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertSafeMultiplyThrowsArithmetic("lifted-maxxintmin-throws", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void assertSafeMultiplyEquals(String oracleId, long expected, long left, int right) {
        final long actual;
        try {
            actual = FieldUtils.safeMultiply(left, right);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected return " + expected
                            + " from FieldUtils.safeMultiply(" + left + ", " + right + ") but threw "
                            + t.getClass().getName(),
                    t);
        }
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply("
                            + left + ", " + right + ") expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertSafeMultiplyThrowsArithmetic(String oracleId, long left, int right) {
        try {
            long actual = FieldUtils.safeMultiply(left, right);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from "
                            + "FieldUtils.safeMultiply(" + left + ", " + right + ") but returned " + actual);
        } catch (ArithmeticException expected) {
            return;
        }
    }

    private static void checkSafeMultiplyLongIntAgreesWithLongLong(long val1, int val2) {
        final long expected;
        try {
            expected = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Throwable t) {
            return;
        }

        final long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            return;
        }

        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:rel-longint-longlong] metamorphic violation: "
                            + "safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs "
                            + "input=(" + val1 + "," + val2 + ") lhs=" + actual + " rhs=" + expected);
        }
    }

    private static void checkSafeMultiplyIntIntAgreesWithLongInt(int a, int b) {
        final int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Throwable t) {
            return;
        }

        final long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Throwable t) {
            return;
        }

        if (longResult != (long) intResult) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:rel-intint-longint] metamorphic violation: "
                            + "safeMultiply(int,int) must agree with widened safeMultiply(long,int) "
                            + "input=(" + a + "," + b + ") lhs=" + longResult + " rhs=" + intResult);
        }
    }

    private static void checkCommutativityForModerateInputs(int x, int y) {
        final long lhs;
        try {
            lhs = FieldUtils.safeMultiply((long) x, y);
        } catch (Throwable t) {
            return;
        }

        final long rhs;
        try {
            rhs = FieldUtils.safeMultiply((long) y, x);
        } catch (Throwable t) {
            return;
        }

        // Contract justification: these overloads implement multiplication with overflow checking;
        // for moderate inputs where both calls succeed, integer multiplication is commutative, so
        // both observable results must match. A patch that merely suppresses a throw or returns a
        // silent wrong value in one branch would violate this relation without needing any crash.
        if (lhs != rhs) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:rel-commutative] metamorphic violation: "
                            + "safeMultiply should be commutative for successful moderate inputs "
                            + "input=(" + x + "," + y + ") lhs=" + lhs + " rhs=" + rhs);
        }
    }
}