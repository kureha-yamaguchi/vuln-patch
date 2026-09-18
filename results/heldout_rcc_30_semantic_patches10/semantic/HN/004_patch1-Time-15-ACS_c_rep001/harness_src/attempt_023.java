package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzMul = data.consumeInt(-1_000_000, 1_000_000);

        try {
            FieldUtils.safeMultiply(fuzzVal, fuzzMul);
        } catch (ArithmeticException ignored) {
        }

        checkEquals("seed-0x0", 0L, 0L, 0);
        checkEquals("seed-1x1", 1L, 1L, 1);
        checkEquals("seed-1x3", 3L, 1L, 3);
        checkEquals("seed-3x1", 3L, 3L, 1);
        checkEquals("seed-2x3", 6L, 2L, 3);
        checkEquals("seed-2x-3", -6L, 2L, -3);
        checkEquals("seed--2x3", -6L, -2L, 3);
        checkEquals("seed--2x-3", 6L, -2L, -3);
        checkEquals("seed--1xIntMin", -1L * Integer.MIN_VALUE, -1L, Integer.MIN_VALUE);
        checkEquals("seed-maxx1", Long.MAX_VALUE, Long.MAX_VALUE, 1);
        checkEquals("seed-minx1", Long.MIN_VALUE, Long.MIN_VALUE, 1);
        checkEquals("seed-maxx-1", -Long.MAX_VALUE, Long.MAX_VALUE, -1);

        checkThrows("seed-minx-1-throws", Long.MIN_VALUE, -1);
        checkThrows("seed-minx100-throws", Long.MIN_VALUE, 100);
        checkThrows("seed-minxIntMax-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkThrows("seed-maxxIntMin-throws", Long.MAX_VALUE, Integer.MIN_VALUE);

        long a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long lhs = FieldUtils.safeMultiply(a, b);
            long rhs = FieldUtils.safeMultiply(a, (long) b);
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on the same mathematical inputs input=("
                        + a + "," + b + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        long chosen = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long actual = FieldUtils.safeMultiply(chosen, 1);
            if (actual != chosen) {
                throw new RuntimeException(
                    "[oracle:identity-times-one] metamorphic violation: multiplying by one must return the original value input="
                        + chosen + " lhs=" + actual + " rhs=" + chosen);
            }
        } catch (ArithmeticException ignored) {
        }
    }

    private static void checkEquals(String id, long expected, long val1, int val2) {
        long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected value " + expected + " but threw "
                    + t.getClass().getName() + " for input=(" + val1 + "," + val2 + ")", t);
        }
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual
                    + " input=(" + val1 + "," + val2 + ")");
        }
    }

    private static void checkThrows(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException but returned "
                    + actual + " for input=(" + val1 + "," + val2 + ")");
        } catch (ArithmeticException expected) {
        }
    }
}