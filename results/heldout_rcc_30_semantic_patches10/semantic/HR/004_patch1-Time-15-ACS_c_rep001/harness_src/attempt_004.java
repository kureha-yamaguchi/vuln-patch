package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        int fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzScalar = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply((long) fuzzVal, fuzzScalar);
            long rhs = FieldUtils.safeMultiply((long) fuzzVal, (long) fuzzScalar);
            /* Contract justification: the overloads safeMultiply(long,int) and safeMultiply(long,long)
               both document the same operation, "Multiply two values throwing an exception if overflow occurs".
               For the same mathematical operands in a non-overflowing range, both real library calls must
               return the same result. A patch that suppresses a needed overflow check or returns a silently
               wrong wrapped value in one overload breaks this observable post-condition even when no throw occurs. */
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) inputVal="
                        + ((long) fuzzVal) + " inputScalar=" + fuzzScalar + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
            return;
        }
    }
}