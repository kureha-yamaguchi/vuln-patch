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
        if (actual != -Long.MAX_VALUE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actual);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-12] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-13] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-14] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:seed-15] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) expected ArithmeticException but returned normally");
        } catch (ArithmeticException expected) {
        }

        long fuzzVal1 = data.consumeInt(-1000000, 1000000);
        int fuzzVal2 = data.consumeInt(-1000000, 1000000);

        try {
            long lhs = FieldUtils.safeMultiply(fuzzVal1, fuzzVal2);
            long rhs = FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2);
            if (lhs != rhs) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on the same mathematical inputs; both overloads are documented as multiplying two values with overflow checks. input=(" + fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + rhs);
            }

            if (fuzzVal2 == 1 && lhs != fuzzVal1) {
                throw new RuntimeException("[oracle:identity-one] metamorphic violation: multiplying by 1 must preserve the input per the documented multiplication contract. input=(" + fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + fuzzVal1);
            }
            if (fuzzVal2 == 0 && lhs != 0L) {
                throw new RuntimeException("[oracle:identity-zero] metamorphic violation: multiplying by 0 must yield 0 per the documented multiplication contract. input=(" + fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=0");
            }
            if (fuzzVal2 == -1) {
                long negated = FieldUtils.safeMultiply(-1L, fuzzVal1);
                if (lhs != negated) {
                    throw new RuntimeException("[oracle:commute-neg1] metamorphic violation: multiplication is the same real library operation under operand reordering for the shared long,long overload, so x*(-1) must equal (-1)*x when both calls succeed. input=(" + fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + negated);
                }
            }
        } catch (ArithmeticException ignored) {
            return;
        }

        int smallA = data.consumeInt(-46340, 46340);
        int smallB = data.consumeInt(-46340, 46340);
        try {
            long viaLongInt = FieldUtils.safeMultiply((long) smallA, smallB);
            int viaIntInt = FieldUtils.safeMultiply(smallA, smallB);
            if (viaLongInt != (long) viaIntInt) {
                throw new RuntimeException("[oracle:int-long-agreement] metamorphic violation: safeMultiply(int,int) and safeMultiply(long,int) are same-name overloads with the same documented semantics, so for moderate inputs where int multiplication cannot overflow they must agree. input=(" + smallA + "," + smallB + ") lhs=" + viaLongInt + " rhs=" + viaIntInt);
            }
        } catch (ArithmeticException ignored) {
            return;
        }
    }
}