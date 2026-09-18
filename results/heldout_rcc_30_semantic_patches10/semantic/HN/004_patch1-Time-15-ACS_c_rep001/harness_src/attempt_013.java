package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        int actualInt;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        expectArithmetic(Long.MIN_VALUE, -1, "seed-exc-0");
        expectArithmetic(Long.MIN_VALUE, 100, "seed-exc-1");
        expectArithmetic(Long.MIN_VALUE, Integer.MAX_VALUE, "seed-exc-2");
        expectArithmetic(Long.MAX_VALUE, Integer.MIN_VALUE, "seed-exc-3");

        int a = data.consumeInt(-46340, 46340);
        int b = data.consumeInt(-46340, 46340);

        try {
            long li = FieldUtils.safeMultiply((long) a, b);
            long ll = FieldUtils.safeMultiply((long) a, (long) b);
            actualInt = FieldUtils.safeMultiply(a, b);

            // Contract justification: all safeMultiply overloads document the same mathematical
            // multiplication with overflow checking. For bounded inputs where no overflow occurs,
            // the real overloads must agree exactly on the result. A patch that suppresses a throw
            // by returning a wrong value, or that breaks a fast path like val2 == -1/0/1, violates
            // this observable post-condition even if no exception is thrown.
            if (li != ll) {
                throw new RuntimeException("[oracle:overload-long] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long) inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + ll);
            }
            if (li != (long) actualInt) {
                throw new RuntimeException("[oracle:overload-int] metamorphic violation: safeMultiply(long,int) != safeMultiply(int,int) inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + actualInt);
            }

            if (b == 1 && li != a) {
                throw new RuntimeException("[oracle:identity-one] metamorphic violation: multiplication by one changed value inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + a);
            }
            if (b == 0 && li != 0L) {
                throw new RuntimeException("[oracle:identity-zero] metamorphic violation: multiplication by zero was non-zero inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=0");
            }
            if (b == -1) {
                long expectedNeg = -((long) a);
                if (li != expectedNeg) {
                    throw new RuntimeException("[oracle:identity-negone] metamorphic violation: multiplication by minus one did not negate inputA=" + a + " inputB=" + b + " lhs=" + li + " rhs=" + expectedNeg);
                }
            }
        } catch (ArithmeticException ignored) {
            return;
        }
    }

    private static void expectArithmetic(long val1, int val2, String oracleId) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2 + ") was expected to throw ArithmeticException but returned=" + actual);
        } catch (ArithmeticException expected) {
            return;
        }
    }
}