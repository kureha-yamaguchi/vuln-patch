package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOracles();
        checkOverloadAgreementLongIntLongLong(data);
        checkOverloadAgreementLongIntIntInt(data);
        checkMetamorphicSignRelation(data);
    }

    private static void checkLiftedOracles() {
        assertEqualsLong("lifted-0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertEqualsLong("lifted-1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertEqualsLong("lifted-2", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertEqualsLong("lifted-3", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertEqualsLong("lifted-4", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertEqualsLong("lifted-5", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertEqualsLong("lifted-6", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertEqualsLong("lifted-7", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertEqualsLong("lifted-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        assertEqualsLong("lifted-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertEqualsLong("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertEqualsLong("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        assertArithmeticException("lifted-12", Long.MIN_VALUE, -1);
        assertArithmeticException("lifted-13", Long.MIN_VALUE, 100);
        assertArithmeticException("lifted-14", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertArithmeticException("lifted-15", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkOverloadAgreementLongIntLongLong(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement-longInt-longLong] semantic mismatch: safeMultiply(long,int) != safeMultiply(long,long) for equivalent inputs val1="
                            + val1 + " val2=" + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkOverloadAgreementLongIntIntInt(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement-longInt-intInt] semantic mismatch: safeMultiply(long,int) != widened safeMultiply(int,int) for inputs a="
                            + a + " b=" + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkMetamorphicSignRelation(FuzzedDataProvider data) {
        long x;
        int y;
        try {
            x = (long) data.consumeInt(-1000000, 1000000);
            y = data.consumeInt(-1000, 1000);
        } catch (Exception e) {
            return;
        }

        long lhs;
        try {
            lhs = FieldUtils.safeMultiply(-x, y);
        } catch (Exception e) {
            return;
        }

        long rhs;
        try {
            rhs = FieldUtils.safeMultiply(FieldUtils.safeMultiply(x, y), -1);
        } catch (Exception e) {
            return;
        }

        /* Contract justification:
         * safeMultiply computes the exact mathematical product or throws ArithmeticException on overflow.
         * For moderate valid-by-construction inputs where both sides succeed, (-x) * y must equal (x * y) * (-1).
         * This is an observable post-condition over only real library calls; a patch that suppresses/changes the
         * special-case handling for multiplier -1 or returns a wrong value instead of the exact product breaks it.
         */
        if (lhs != rhs) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:sign-relation] metamorphic violation: safeMultiply(-x, y) must equal safeMultiply(safeMultiply(x, y), -1) for x="
                            + x + " y=" + y + " lhs=" + lhs + " rhs=" + rhs);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertArithmeticException(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                            + ") was required by the documented contract and lifted test to throw ArithmeticException, but returned "
                            + actual);
        } catch (ArithmeticException expected) {
        }
    }
}