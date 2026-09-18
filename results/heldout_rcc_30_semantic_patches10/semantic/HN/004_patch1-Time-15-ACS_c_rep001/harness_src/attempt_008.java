package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        try {
            actualLong = FieldUtils.safeMultiply(0L, 0);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-0x0] semantic mismatch: expected=0 actual=ArithmeticException", e);
        }
        if (actualLong != 0L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-0x0] semantic mismatch: expected=0 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x1] semantic mismatch: expected=1 actual=ArithmeticException", e);
        }
        if (actualLong != 1L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x1] semantic mismatch: expected=1 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x3] semantic mismatch: expected=3 actual=ArithmeticException", e);
        }
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-1x3] semantic mismatch: expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(3L, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-3x1] semantic mismatch: expected=3 actual=ArithmeticException", e);
        }
        if (actualLong != 3L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-3x1] semantic mismatch: expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, 3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2x3] semantic mismatch: expected=6 actual=ArithmeticException", e);
        }
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2x3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, -3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2xneg3] semantic mismatch: expected=-6 actual=ArithmeticException", e);
        }
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-2xneg3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, 3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2x3] semantic mismatch: expected=-6 actual=ArithmeticException", e);
        }
        if (actualLong != -6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2x3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, -3);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2xneg3] semantic mismatch: expected=6 actual=ArithmeticException", e);
        }
        if (actualLong != 6L) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg2xneg3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg1xintmin] semantic mismatch: expected=" + (-1L * Integer.MIN_VALUE) + " actual=ArithmeticException", e);
        }
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-neg1xintmin] semantic mismatch: expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxx1] semantic mismatch: expected=" + Long.MAX_VALUE + " actual=ArithmeticException", e);
        }
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxx1] semantic mismatch: expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx1] semantic mismatch: expected=" + Long.MIN_VALUE + " actual=ArithmeticException", e);
        }
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx1] semantic mismatch: expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        } catch (ArithmeticException e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxneg1] semantic mismatch: expected=" + (-Long.MAX_VALUE) + " actual=ArithmeticException", e);
        }
        if (actualLong != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxneg1] semantic mismatch: expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            long unexpected = FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minxneg1-throws] semantic mismatch: expected=ArithmeticException actual=" + unexpected);
        } catch (ArithmeticException expected) {
        }

        try {
            long unexpected = FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minx100-throws] semantic mismatch: expected=ArithmeticException actual=" + unexpected);
        } catch (ArithmeticException expected) {
        }

        try {
            long unexpected = FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-minxintmax-throws] semantic mismatch: expected=ArithmeticException actual=" + unexpected);
        } catch (ArithmeticException expected) {
        }

        try {
            long unexpected = FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-maxxintmin-throws] semantic mismatch: expected=ArithmeticException actual=" + unexpected);
        } catch (ArithmeticException expected) {
        }

        int fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000_000, 1_000_000);

        try {
            long lhs = FieldUtils.safeMultiply((long) fuzzVal1, fuzzVal2);
            long rhs = FieldUtils.safeMultiply((long) fuzzVal1, (long) fuzzVal2);
            // Same-name overloads share the same documented contract ("Multiply two values throwing
            // an exception if overflow occurs"), so on the same mathematical inputs and successful
            // completion they must return the same product. A patch that removes the exceptional
            // behavior on a special case and instead returns a wrapped value violates this relation.
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) input=(" +
                    fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException e) {
            return;
        }

        int canonical = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long viaOne = FieldUtils.safeMultiply((long) canonical, 1);
            // The documented contract for multiplication plus the visible special-case branch for
            // multiplier 1 guarantee that multiplying by 1 preserves the input exactly. This is an
            // observable post-condition on the returned value, so a throw-deleting/wrong-value patch
            // is caught even if no exception fires.
            if (viaOne != (long) canonical) {
                throw new RuntimeException(
                    "[oracle:identity-by-one] metamorphic violation: safeMultiply(x,1)==x input=" +
                    canonical + " lhs=" + viaOne + " rhs=" + canonical);
            }
        } catch (ArithmeticException e) {
            return;
        }
    }
}