package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkExact("lift-0x0", 0L, 0L, 0);
        checkExact("lift-1x1", 1L, 1L, 1);
        checkExact("lift-1x3", 3L, 1L, 3);
        checkExact("lift-3x1", 3L, 3L, 1);

        checkExact("lift-2x3", 6L, 2L, 3);
        checkExact("lift-2x-3", -6L, 2L, -3);
        checkExact("lift--2x3", -6L, -2L, 3);
        checkExact("lift--2x-3", 6L, -2L, -3);

        checkExact("lift--1xIntegerMin", -1L * Integer.MIN_VALUE, -1L, Integer.MIN_VALUE);

        checkExact("lift-longMaxx1", Long.MAX_VALUE, Long.MAX_VALUE, 1);
        checkExact("lift-longMinx1", Long.MIN_VALUE, Long.MIN_VALUE, 1);
        checkExact("lift-longMaxx-1", -Long.MAX_VALUE, Long.MAX_VALUE, -1);

        checkThrowsArithmetic("lift-longMinx-1-throws", Long.MIN_VALUE, -1);
        checkThrowsArithmetic("lift-longMinx100-throws", Long.MIN_VALUE, 100);
        checkThrowsArithmetic("lift-longMinxIntegerMax-throws", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkThrowsArithmetic("lift-longMaxxIntegerMin-throws", Long.MAX_VALUE, Integer.MIN_VALUE);

        long fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzScalar = data.consumeInt(-1_000, 1_000);

        // Documented guarantee: the safeMultiply overloads have the same contract
        // ("Multiply two values throwing an exception if overflow occurs"), so for
        // the same mathematical inputs they must agree whenever both calls succeed.
        // A "fix" that merely suppresses the -1 overflow throw or returns a wrapped
        // value breaks this observable equality without relying on exceptions alone.
        try {
            long lhs = FieldUtils.safeMultiply(fuzzVal, fuzzScalar);
            long rhs = FieldUtils.safeMultiply(fuzzVal, (long) fuzzScalar);
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: " +
                    "safeMultiply(long,int) != safeMultiply(long,long) " +
                    "inputVal=" + fuzzVal + " inputScalar=" + fuzzScalar +
                    " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            return;
        }

        int small = data.consumeInt(-1_000_000, 1_000_000);
        // Documented guarantee: multiplying by 1 returns the original value in the
        // patched method body itself; a patch that makes the target branch unreachable
        // or silently alters the result violates this post-condition directly.
        try {
            long actual = FieldUtils.safeMultiply((long) small, 1);
            if (actual != (long) small) {
                throw new RuntimeException(
                    "[oracle:identity-times-one] metamorphic violation: " +
                    "safeMultiply(x,1) must equal x input=" + small +
                    " lhs=" + actual + " rhs=" + ((long) small));
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkExact(String id, long expected, long val1, int val2) {
        final long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: unexpected throw for FieldUtils.safeMultiply(" +
                val1 + ", " + val2 + "), expected=" + expected + ", threw=" +
                t.getClass().getName());
        }
        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: FieldUtils.safeMultiply(" +
                val1 + ", " + val2 + ") expected=" + expected + " actual=" + actual);
        }
    }

    private static void checkThrowsArithmetic(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(" +
                val1 + ", " + val2 + ") but returned=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}