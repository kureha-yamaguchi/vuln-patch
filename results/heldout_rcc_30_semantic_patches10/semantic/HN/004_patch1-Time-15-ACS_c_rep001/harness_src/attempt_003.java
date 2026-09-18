package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOraclePairs();
        checkLiftedOracleExpectedRejections();
        checkMetamorphicOverloadAgreementLongIntLongLong(data);
        checkMetamorphicOverloadAgreementLongIntIntInt(data);
        checkMetamorphicNegationRelation(data);
    }

    private static void checkLiftedOraclePairs() {
        assertLongEquals("lifted-0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertLongEquals("lifted-1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertLongEquals("lifted-2", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertLongEquals("lifted-3", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertLongEquals("lifted-4", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertLongEquals("lifted-5", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertLongEquals("lifted-6", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertLongEquals("lifted-7", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertLongEquals("lifted-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        assertLongEquals("lifted-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertLongEquals("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertLongEquals("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");
    }

    private static void checkLiftedOracleExpectedRejections() {
        assertArithmeticException("lifted-ex-0", Long.MIN_VALUE, -1);
        assertArithmeticException("lifted-ex-1", Long.MIN_VALUE, 100);
        assertArithmeticException("lifted-ex-2", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertArithmeticException("lifted-ex-3", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkMetamorphicOverloadAgreementLongIntLongLong(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1000000, 1000000);
            val2 = data.consumeInt(-1000000, 1000000);
        } catch (Throwable t) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Throwable t) {
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Throwable t) {
            return;
        }

        // Contract justification: safeMultiply(long,int) and safeMultiply(long,long) both compute the same mathematical
        // product with overflow checking; on valid small inputs the observable result must agree. A patch that merely
        // deletes the throw or returns a wrong value in one overload violates this post-condition without crashing.
        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement-longInt-longLong] metamorphic violation: FieldUtils.safeMultiply(long,int) != FieldUtils.safeMultiply(long,long) inputVal1="
                            + val1 + " inputVal2=" + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkMetamorphicOverloadAgreementLongIntIntInt(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Throwable t) {
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Throwable t) {
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Throwable t) {
            return;
        }

        // Contract justification: the int/int and long/int overloads both implement safe multiplication; when both inputs
        // and result fit in int, widening the int result to long must equal the long/int result. This detects silent
        // wrong-value fixes even when no exception is involved.
        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:overload-agreement-longInt-intInt] metamorphic violation: FieldUtils.safeMultiply(long,int) != widened FieldUtils.safeMultiply(int,int) inputA="
                            + a + " inputB=" + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkMetamorphicNegationRelation(FuzzedDataProvider data) {
        int v1;
        int v2;
        try {
            v1 = data.consumeInt(-1000000, 1000000);
            v2 = data.consumeInt(-1000000, 1000000);
        } catch (Throwable t) {
            return;
        }

        long product;
        try {
            product = FieldUtils.safeMultiply((long) v1, v2);
        } catch (Throwable t) {
            return;
        }

        long negatedProduct;
        try {
            negatedProduct = FieldUtils.safeMultiply(product, -1);
        } catch (Throwable t) {
            return;
        }

        long productWithNegatedFactor;
        try {
            productWithNegatedFactor = FieldUtils.safeMultiply((long) v1, -v2);
        } catch (Throwable t) {
            return;
        }

        // Contract justification: multiplication by -1 is negation, so for any non-overflowing calls
        // safeMultiply(safeMultiply(x,y), -1) must equal safeMultiply(x, -y). This is a real-library metamorphic
        // post-condition that a throw-deleting or wrong-value patch in the val2 == -1 branch would break observably.
        if (negatedProduct != productWithNegatedFactor) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:negation-relation] metamorphic violation: safeMultiply(safeMultiply(x,y),-1) != safeMultiply(x,-y) x="
                            + v1 + " y=" + v2 + " lhs=" + negatedProduct + " rhs=" + productWithNegatedFactor);
        }
    }

    private static void assertArithmeticException(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply("
                            + val1 + ", " + val2 + ") but returned " + actual);
        } catch (ArithmeticException expected) {
            return;
        }
    }

    private static void assertLongEquals(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }
}