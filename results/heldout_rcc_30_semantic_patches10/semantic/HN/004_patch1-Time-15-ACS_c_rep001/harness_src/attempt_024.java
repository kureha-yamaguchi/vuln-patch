package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long actualLong;
        try {
            actualLong = FieldUtils.safeMultiply(0L, 0);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) threw " + t.getClass().getName(), t);
        }
        if (actualLong != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-0] semantic mismatch: FieldUtils.safeMultiply(0L, 0) expected=0 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) threw " + t.getClass().getName(), t);
        }
        if (actualLong != 1L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-1] semantic mismatch: FieldUtils.safeMultiply(1L, 1) expected=1 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(1L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) threw " + t.getClass().getName(), t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-2] semantic mismatch: FieldUtils.safeMultiply(1L, 3) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(3L, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) threw " + t.getClass().getName(), t);
        }
        if (actualLong != 3L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-3] semantic mismatch: FieldUtils.safeMultiply(3L, 1) expected=3 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) threw " + t.getClass().getName(), t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-4] semantic mismatch: FieldUtils.safeMultiply(2L, 3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) threw " + t.getClass().getName(), t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-5] semantic mismatch: FieldUtils.safeMultiply(2L, -3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, 3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) threw " + t.getClass().getName(), t);
        }
        if (actualLong != -6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-6] semantic mismatch: FieldUtils.safeMultiply(-2L, 3) expected=-6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-2L, -3);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) threw " + t.getClass().getName(), t);
        }
        if (actualLong != 6L) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-7] semantic mismatch: FieldUtils.safeMultiply(-2L, -3) expected=6 actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) threw " + t.getClass().getName(), t);
        }
        if (actualLong != (-1L * Integer.MIN_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-8] semantic mismatch: FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE) expected=" + (-1L * Integer.MIN_VALUE) + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) threw " + t.getClass().getName(), t);
        }
        if (actualLong != Long.MAX_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-9] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, 1) expected=" + Long.MAX_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MIN_VALUE, 1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) threw " + t.getClass().getName(), t);
        }
        if (actualLong != Long.MIN_VALUE) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-10] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 1) expected=" + Long.MIN_VALUE + " actual=" + actualLong);
        }

        try {
            actualLong = FieldUtils.safeMultiply(Long.MAX_VALUE, -1);
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) threw " + t.getClass().getName(), t);
        }
        if (actualLong != (-Long.MAX_VALUE)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-11] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, -1) expected=" + (-Long.MAX_VALUE) + " actual=" + actualLong);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, -1);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-0] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-0] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, -1) threw " + t.getClass().getName() + " instead of ArithmeticException", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, 100);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-1] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, 100) threw " + t.getClass().getName() + " instead of ArithmeticException", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-2] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-2] semantic mismatch: FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE) threw " + t.getClass().getName() + " instead of ArithmeticException", t);
        }

        try {
            FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE);
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-3] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) was expected to throw ArithmeticException");
        } catch (ArithmeticException expected) {
        } catch (Throwable t) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-reject-3] semantic mismatch: FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE) threw " + t.getClass().getName() + " instead of ArithmeticException", t);
        }

        long val1Relation1;
        int val2Relation1;
        try {
            val1Relation1 = (long) data.consumeInt(-1000000, 1000000);
            val2Relation1 = data.consumeInt(-1000000, 1000000);
        } catch (Throwable t) {
            return;
        }
        long relation1Left;
        long relation1Right;
        try {
            relation1Left = FieldUtils.safeMultiply(val1Relation1, val2Relation1);
            relation1Right = FieldUtils.safeMultiply(val1Relation1, (long) val2Relation1);
        } catch (Throwable t) {
            return;
        }
        if (relation1Left != relation1Right) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_longLong_small_inputs violated: lhs=" + relation1Left + " rhs=" + relation1Right + " val1=" + val1Relation1 + " val2=" + val2Relation1);
        }

        int aRelation2;
        int bRelation2;
        try {
            aRelation2 = data.consumeInt(-46340, 46340);
            bRelation2 = data.consumeInt(-46340, 46340);
        } catch (Throwable t) {
            return;
        }
        long relation2Left;
        int relation2Right;
        try {
            relation2Left = FieldUtils.safeMultiply((long) aRelation2, bRelation2);
            relation2Right = FieldUtils.safeMultiply(aRelation2, bRelation2);
        } catch (Throwable t) {
            return;
        }
        if (relation2Left != (long) relation2Right) {
            throw new FuzzerSecurityIssueLow("relation overload_agreement_longInt_intInt_small_inputs violated: lhs=" + relation2Left + " rhs=" + relation2Right + " a=" + aRelation2 + " b=" + bRelation2);
        }

        long metamorphicValue;
        try {
            metamorphicValue = (long) data.consumeInt(-1000000, 1000000);
        } catch (Throwable t) {
            return;
        }
        long negResult;
        long posResult;
        long sumResult;
        try {
            /* Contract asserted: for valid non-overflow inputs, multiplying by -1 computes the additive inverse,
               and multiplying by 1 returns the original value. A patch that merely deletes the documented
               ArithmeticException branch or silently returns the wrong value in the -1 case violates this relation
               observably because safeMultiply(x, -1) + safeMultiply(x, 1) must equal 0 for every moderate x. */
            negResult = FieldUtils.safeMultiply(metamorphicValue, -1);
            posResult = FieldUtils.safeMultiply(metamorphicValue, 1);
            sumResult = FieldUtils.safeAdd(negResult, posResult);
        } catch (Throwable t) {
            return;
        }
        if (sumResult != 0L) {
            throw new FuzzerSecurityIssueLow("[oracle:negation-additive-inverse] metamorphic violation: safeMultiply(x,-1) and safeMultiply(x,1) are not additive inverses input=" + metamorphicValue + " lhs=" + negResult + " rhs=" + posResult + " sum=" + sumResult);
        }
    }
}