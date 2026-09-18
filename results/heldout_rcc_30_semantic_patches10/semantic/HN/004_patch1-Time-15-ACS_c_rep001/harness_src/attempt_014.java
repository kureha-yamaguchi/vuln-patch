package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    /*__vpCause*/ private static Throwable __vpCause = null;

    private static void assertLongEquals(String oracleId, long expected, long actual, String detail) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + detail +
                " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void assertArithmeticException(String oracleId, long val1, int val2) {
        boolean threw = false;
        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException expected) { __vpCause = expected;
            threw = true;
        }
        if (!threw) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(" +
                val1 + ", " + val2 + ")"
            , __vpCause);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertLongEquals("fixed-0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertLongEquals("fixed-1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertLongEquals("fixed-2", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertLongEquals("fixed-3", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertLongEquals("fixed-4", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertLongEquals("fixed-5", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertLongEquals("fixed-6", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertLongEquals("fixed-7", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertLongEquals(
            "fixed-8",
            -1L * Integer.MIN_VALUE,
            FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
            "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)"
        );
        assertLongEquals(
            "fixed-9",
            Long.MAX_VALUE,
            FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
            "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)"
        );
        assertLongEquals(
            "fixed-10",
            Long.MIN_VALUE,
            FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
            "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)"
        );
        assertLongEquals(
            "fixed-11",
            -Long.MAX_VALUE,
            FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
            "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)"
        );

        assertArithmeticException("throws-min-neg1", Long.MIN_VALUE, -1);
        assertArithmeticException("throws-min-100", Long.MIN_VALUE, 100);
        assertArithmeticException("throws-min-intmax", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertArithmeticException("throws-max-intmin", Long.MAX_VALUE, Integer.MIN_VALUE);

        int rel1Val1Int = data.consumeInt(-1000000, 1000000);
        int rel1Val2 = data.consumeInt(-1000000, 1000000);
        long rel1Left;
        long rel1Right;
        try {
            rel1Left = FieldUtils.safeMultiply((long) rel1Val1Int, rel1Val2);
            rel1Right = FieldUtils.safeMultiply((long) rel1Val1Int, (long) rel1Val2);
        } catch (Exception e) {
            rel1Left = 0L;
            rel1Right = 0L;
            return;
        }
        if (rel1Left != rel1Right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:overload-agreement-longint-longlong] metamorphic violation: " +
                "safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent small non-overflow inputs " +
                "input1=" + rel1Val1Int + " input2=" + rel1Val2 +
                " lhs=" + rel1Left + " rhs=" + rel1Right
            );
        }

        int rel2A = data.consumeInt(-46340, 46340);
        int rel2B = data.consumeInt(-46340, 46340);
        long rel2Left;
        int rel2Right;
        try {
            rel2Left = FieldUtils.safeMultiply((long) rel2A, rel2B);
            rel2Right = FieldUtils.safeMultiply(rel2A, rel2B);
        } catch (Exception e) {
            rel2Left = 0L;
            rel2Right = 0;
            return;
        }
        if (rel2Left != (long) rel2Right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:overload-agreement-longint-intint] metamorphic violation: " +
                "safeMultiply(long,int) must agree with safeMultiply(int,int) when both inputs and product fit in int " +
                "input1=" + rel2A + " input2=" + rel2B +
                " lhs=" + rel2Left + " rhs=" + rel2Right
            );
        }

        int rel3N = data.consumeInt(-1000000, 1000000);
        long rel3Mul;
        int rel3Neg;
        try {
            rel3Mul = FieldUtils.safeMultiply((long) rel3N, -1);
            rel3Neg = FieldUtils.safeNegate(rel3N);
        } catch (Exception e) {
            rel3Mul = 0L;
            rel3Neg = 0;
            return;
        }
        if (rel3Mul != (long) rel3Neg) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negate-agreement] metamorphic violation: " +
                "the safe* family documents safe arithmetic with the same mathematical result, so multiplying by -1 must match safeNegate for int-range inputs " +
                "input=" + rel3N + " lhs=" + rel3Mul + " rhs=" + rel3Neg
            );
        }

        int rel4N = data.consumeInt(-1000000, 1000000);
        long rel4Once;
        long rel4Twice;
        try {
            rel4Once = FieldUtils.safeMultiply((long) rel4N, -1);
            rel4Twice = FieldUtils.safeMultiply(rel4Once, -1);
        } catch (Exception e) {
            rel4Once = 0L;
            rel4Twice = 0L;
            return;
        }
        if (rel4Twice != (long) rel4N) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:double-negation] metamorphic violation: " +
                "for any non-overflowing value, multiplying by -1 twice must recover the original value; a patch that merely deletes the throw or returns the wrong sign breaks this post-condition " +
                "input=" + rel4N + " intermediate=" + rel4Once + " result=" + rel4Twice
            );
        }
    }
}