package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException");
        } catch (ArithmeticException expected) {
        }

        int fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000_000, 1_000_000);
        long lhs;
        long rhs;
        try {
            lhs = FieldUtils.safeMultiply((long) fuzzVal1, fuzzVal2);
            rhs = FieldUtils.safeMultiply((long) fuzzVal1, (long) fuzzVal2);
        } catch (Throwable t) {
            return;
        }
        /* Documented guarantee used for this oracle:
         * the safeMultiply overloads are same-name family members over the same input space;
         * widening an int operand to long must not change the mathematical result for any
         * correct implementation when both calls succeed. A throw-deleting or wrong-value
         * patch in the long,int overload would break this observable agreement.
         */
        if (lhs != rhs) {
            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) for equivalent inputs input=(" + ((long) fuzzVal1) + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + rhs);
        }
    }
}