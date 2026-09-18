package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;

        actualLong = FieldUtils.safeMultiply(0L, 0);
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0x0] semantic mismatch: expected=0 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 1);
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x1] semantic mismatch: expected=1 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(1L, 3);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1x3] semantic mismatch: expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(3L, 1);
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3x1] semantic mismatch: expected=3 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, 3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(2L, -3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2x-3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, 3);
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x3] semantic mismatch: expected=-6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-2L, -3);
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--2x-3] semantic mismatch: expected=6 actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted--1xintmin] semantic mismatch: expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx1] semantic mismatch: expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx1] semantic mismatch: expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        if (actualLong != -Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxx-1] semantic mismatch: expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, -1)");
        } catch (ArithmeticException expected) {
        } catch (Throwable e) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx-1-throws] semantic mismatch: expected ArithmeticException but got " + e.getClass().getName(), e);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, 100)");
        } catch (ArithmeticException expected) {
        } catch (Throwable e) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minx100-throws] semantic mismatch: expected ArithmeticException but got " + e.getClass().getName(), e);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)");
        } catch (ArithmeticException expected) {
        } catch (Throwable e) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-minxintmax-throws] semantic mismatch: expected ArithmeticException but got " + e.getClass().getName(), e);
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)");
        } catch (ArithmeticException expected) {
        } catch (Throwable e) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-maxxintmin-throws] semantic mismatch: expected ArithmeticException but got " + e.getClass().getName(), e);
        }

        int fuzzA;
        int fuzzB;
        try {
            fuzzA = data.consumeInt(-46340, 46340);
            fuzzB = data.consumeInt(-46340, 46340);
        } catch (Exception e) {
            return;
        }
        long rel1Left;
        int rel1Right;
        try {
            rel1Left = FieldUtils.safeMultiply((long) fuzzA, fuzzB);
            rel1Right = FieldUtils.safeMultiply(fuzzA, fuzzB);
        } catch (Exception e) {
            return;
        }
        if (rel1Left != (long) rel1Right) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_intInt_small_inputs violated: lhs=" + rel1Left + " rhs=" + rel1Right + " a=" + fuzzA + " b=" + fuzzB);
        }

        long fuzzVal1;
        int fuzzVal2;
        try {
            fuzzVal1 = (long) data.consumeInt(-1000000, 1000000);
            fuzzVal2 = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long rel2Left;
        long rel2Right;
        try {
            rel2Left = FieldUtils.safeMultiply(fuzzVal1, fuzzVal2);
            rel2Right = FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2);
        } catch (Exception e) {
            return;
        }
        if (rel2Left != rel2Right) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_longLong_small_inputs violated: lhs=" + rel2Left + " rhs=" + rel2Right + " val1=" + fuzzVal1 + " val2=" + fuzzVal2);
        }

        int identitySeed;
        try {
            identitySeed = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long identityValue = (long) identitySeed;
        long timesOne;
        long timesZero;
        try {
            timesOne = FieldUtils.safeMultiply(identityValue, 1);
            timesZero = FieldUtils.safeMultiply(identityValue, 0);
        } catch (Exception e) {
            return;
        }
        /* Contract/post-condition check: safeMultiply(x, 1) returns x and safeMultiply(x, 0) returns 0 by the method's documented behavior and implementation.
           A patch that avoids the throw path by deleting or bypassing the special-case logic could still silently return the wrong long; these observable results must hold. */
        if (timesOne != identityValue) {
            throw new FuzzerSecurityIssueLow("[oracle:identity-x1] metamorphic violation: safeMultiply(x,1)==x input=" + identityValue + " lhs=" + timesOne + " rhs=" + identityValue);
        }
        if (timesZero != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:annihilator-x0] metamorphic violation: safeMultiply(x,0)==0 input=" + identityValue + " lhs=" + timesZero + " rhs=0");
        }

        int signSeed;
        try {
            signSeed = data.consumeInt(-1000000, 1000000);
        } catch (Exception e) {
            return;
        }
        long signValue = (long) signSeed;
        long pos;
        long neg;
        try {
            pos = FieldUtils.safeMultiply(signValue, 1);
            neg = FieldUtils.safeMultiply(signValue, -1);
        } catch (Exception e) {
            return;
        }
        /* Metamorphic relation: for any non-overflowing input, multiplying by -1 must negate the result of multiplying by 1.
           This uses two real library calls and catches silent wrong-value patches that do not throw. */
        if (neg != -pos) {
            throw new FuzzerSecurityIssueLow("[oracle:sign-flip] metamorphic violation: safeMultiply(x,-1)==-safeMultiply(x,1) input=" + signValue + " lhs=" + neg + " rhs=" + (-pos));
        }
    }
}