package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actual;

        actual = FieldUtils.safeMultiply(0L, 0);
        if (actual != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-0x0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 1);
        if (actual != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1x1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(1L, 3);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-1x3] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(3L, 1);
        if (actual != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-3x1] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, 3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2x3] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(2L, -3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-2x-3] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, 3);
        if (actual != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted--2x3] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-2L, -3);
        if (actual != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted--2x-3] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actual != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted--1xminint] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actual != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-maxx1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actual != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-minx1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actual);
        }

        actual = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-maxx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-minx-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-minx100] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-minxmaxint] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-throw-maxxminint] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        int fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzMul = data.consumeInt(-1_000_000, 1_000_000);

        try {
            long lhs = FieldUtils.safeMultiply((long) fuzzVal, fuzzMul);
            long rhs = FieldUtils.safeMultiply((long) fuzzVal, (long) fuzzMul);
            // Contract justification: these are same-name overloads of safeMultiply with the same documented behavior
            // ("Multiply two values throwing an exception if overflow occurs"), so for any non-overflowing inputs they must agree.
            // A patch that only suppresses the exceptional -1 fast-path or otherwise returns a wrong value breaks this equality observably.
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) inputVal="
                        + fuzzVal + " inputMul=" + fuzzMul + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        long identityInput = (long) data.consumeInt(-1_000_000, 1_000_000);
        try {
            long identityResult = FieldUtils.safeMultiply(identityInput, 1);
            // Contract justification: the real API's own test suite lifts exact identity pairs such as (1,1), (3,1), (Long.MAX_VALUE,1), (Long.MIN_VALUE,1),
            // establishing that multiplying by 1 must preserve the value for valid inputs.
            // A throw-deleting or wrong-value patch can make the fast-path reachable but return an incorrect post-state/result.
            if (identityResult != identityInput) {
                throw new RuntimeException(
                    "[oracle:identity-by-one] metamorphic violation: safeMultiply(x,1)==x input="
                        + identityInput + " lhs=" + identityResult + " rhs=" + identityInput);
            }
        } catch (ArithmeticException ignored) {
            return;
        }
    }
}