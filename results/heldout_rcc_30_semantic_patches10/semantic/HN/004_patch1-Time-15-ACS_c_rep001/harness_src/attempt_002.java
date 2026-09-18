package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkEquals("t0", 0L, 0, 0L);
        checkEquals("t1", 1L, 1, 1L);
        checkEquals("t2", 1L, 3, 3L);
        checkEquals("t3", 3L, 1, 3L);

        checkEquals("t4", 2L, 3, 6L);
        checkEquals("t5", 2L, -3, -6L);
        checkEquals("t6", -2L, 3, -6L);
        checkEquals("t7", -2L, -3, 6L);

        checkEquals("t8", -1L, Integer.MIN_VALUE, -1L * Integer.MIN_VALUE);

        checkEquals("t9", Long.MAX_VALUE, 1, Long.MAX_VALUE);
        checkEquals("t10", Long.MIN_VALUE, 1, Long.MIN_VALUE);
        checkEquals("t11", Long.MAX_VALUE, -1, -Long.MAX_VALUE);

        checkThrowsArithmetic("t12", Long.MIN_VALUE, -1);
        checkThrowsArithmetic("t13", Long.MIN_VALUE, 100);
        checkThrowsArithmetic("t14", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkThrowsArithmetic("t15", Long.MAX_VALUE, Integer.MIN_VALUE);

        long fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzMul = data.consumeInt(-1_000_000, 1_000_000);

        try {
            long lhs = FieldUtils.safeMultiply(fuzzVal, fuzzMul);
            long rhs = FieldUtils.safeMultiply(fuzzVal, (long) fuzzMul);
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-long-long] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs per their shared documented contract input=("
                        + fuzzVal + "," + fuzzMul + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }

        int fuzzIntVal = data.consumeInt(-46340, 46340);
        int fuzzIntMul = data.consumeInt(-46340, 46340);

        try {
            long lhs = FieldUtils.safeMultiply((long) fuzzIntVal, fuzzIntMul);
            int rhsInt = FieldUtils.safeMultiply(fuzzIntVal, fuzzIntMul);
            long rhs = (long) rhsInt;
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-int-longint] metamorphic violation: safeMultiply(int,int) and safeMultiply(long,int) must agree when the long input is exactly the same int-valued number input=("
                        + fuzzIntVal + "," + fuzzIntMul + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void checkEquals(String id, long val1, int val2, long expected) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            if (actual != expected) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + id + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                        + ") expected=" + expected + " actual=" + actual);
            }
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                    + ") unexpectedly threw ArithmeticException");
        }
    }

    private static void checkThrowsArithmetic(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                    + ") expected ArithmeticException but returned=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}