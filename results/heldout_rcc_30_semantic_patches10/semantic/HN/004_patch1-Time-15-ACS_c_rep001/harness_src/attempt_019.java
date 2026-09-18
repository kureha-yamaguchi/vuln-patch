package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-0x0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x3] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-3x1] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2x3] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2x-3] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed--2x3] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed--2x-3] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed--1xminint] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxx1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx-1-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx100-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minxmaxint-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxminint-throws] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        int fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzMul = data.consumeInt(-1_000, 1_000);
        long lhs;
        long rhs;
        try {
            lhs = FieldUtils.safeMultiply((long) fuzzVal, fuzzMul);
            rhs = FieldUtils.safeMultiply((long) fuzzVal, (long) fuzzMul);
        } catch (RuntimeException e) {
            return;
        }

        // Contract justification: the same-name overloads safeMultiply(long, int) and
        // safeMultiply(long, long) are both documented as "Multiply two values throwing
        // an exception if overflow occurs." For any non-overflowing int multiplier,
        // both real library calls must therefore produce the same product. A patch that
        // merely deletes the throw or returns a wrapped value in one overload breaks this.
        if (lhs != rhs) {
            throw new RuntimeException(
                "[oracle:overload-equivalence] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) inputVal="
                    + fuzzVal + " inputMul=" + fuzzMul + " lhs=" + lhs + " rhs=" + rhs);
        }

        int identityVal = data.consumeInt(-1_000_000, 1_000_000);
        long identityActual;
        try {
            identityActual = FieldUtils.safeMultiply((long) identityVal, 1);
        } catch (RuntimeException e) {
            return;
        }

        // Contract justification: the method's own implementation and documented purpose
        // guarantee multiplication by 1 returns the original value when no overflow occurs;
        // this is a direct observable post-condition on the API result.
        if (identityActual != (long) identityVal) {
            throw new RuntimeException(
                "[oracle:identity-by-one] metamorphic violation: safeMultiply(x,1) must equal x input="
                    + identityVal + " lhs=" + identityActual + " rhs=" + identityVal);
        }
    }
}