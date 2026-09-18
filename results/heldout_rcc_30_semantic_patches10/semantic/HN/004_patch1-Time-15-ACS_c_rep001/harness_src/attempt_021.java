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
                "[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected="
                    + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected="
                    + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected="
                    + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected="
                    + (-Long.MAX_VALUE) + " actual=" + actualLong);
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

        // Contract justification: safeMultiply(long,int) and safeMultiply(int,int) both implement safe multiplication;
        // for non-overflowing int inputs they must agree up to widening. A patch that silently returns a wrong product
        // instead of throwing is observable through this sibling-agreement post-condition.
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }
        long rel1Left;
        int rel1Right;
        try {
            rel1Left = FieldUtils.safeMultiply((long) a, b);
            rel1Right = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        if (rel1Left != (long) rel1Right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:overload-longint-intint] metamorphic violation: safeMultiply((long)a,b) must equal widened safeMultiply(a,b) for non-overflowing int inputs inputA="
                    + a + " inputB=" + b + " lhs=" + rel1Left + " rhs=" + rel1Right);
        }

        // Contract justification: safeMultiply(long,int) and safeMultiply(long,long) are documented identically
        // apart from the second parameter type; on equivalent small non-overflowing inputs they must return the same result.
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long rel2Left;
        long rel2Right;
        try {
            rel2Left = FieldUtils.safeMultiply(val1, val2);
            rel2Right = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        if (rel2Left != rel2Right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:overload-longint-longlong] metamorphic violation: safeMultiply(long,int) must equal safeMultiply(long,long) on equivalent small inputs val1="
                    + val1 + " val2=" + val2 + " lhs=" + rel2Left + " rhs=" + rel2Right);
        }

        // Contract justification: for any int x except Integer.MIN_VALUE, safeMultiply((long)x, -1) computes the same
        // mathematical negation as safeNegate(x), widened to long. This directly exercises the patched -1 branch.
        int x;
        try {
            x = data.consumeInt(Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
        } catch (Exception e) {
            return;
        }
        long mulNeg;
        int neg;
        try {
            mulNeg = FieldUtils.safeMultiply((long) x, -1);
            neg = FieldUtils.safeNegate(x);
        } catch (Exception e) {
            return;
        }
        if (mulNeg != (long) neg) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:negation-sibling] metamorphic violation: safeMultiply((long)x,-1) must equal widened safeNegate(x) for x != Integer.MIN_VALUE x="
                    + x + " lhs=" + mulNeg + " rhs=" + neg);
        }

        // Contract justification: multiplying by 1 is an identity case explicitly implemented by the public API.
        long moderate;
        try {
            moderate = (long) data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long identity;
        try {
            identity = FieldUtils.safeMultiply(moderate, 1);
        } catch (Exception e) {
            return;
        }
        if (identity != moderate) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:identity-times-one] metamorphic violation: safeMultiply(v,1) must equal v v="
                    + moderate + " actual=" + identity);
        }
    }
}