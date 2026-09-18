package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedOraclePairs();
        checkLiftedOracleRejections();
        checkMetamorphicNegationAgreement(data);
        checkOverloadAgreementLongIntLongLong(data);
        checkOverloadAgreementLongIntIntInt(data);
    }

    private static void checkLiftedOraclePairs() {
        assertEqualsLong("[oracle:lifted-0x0]", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertEqualsLong("[oracle:lifted-1x1]", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertEqualsLong("[oracle:lifted-1x3]", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertEqualsLong("[oracle:lifted-3x1]", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertEqualsLong("[oracle:lifted-2x3]", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertEqualsLong("[oracle:lifted-2x-3]", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertEqualsLong("[oracle:lifted--2x3]", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertEqualsLong("[oracle:lifted--2x-3]", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertEqualsLong("[oracle:lifted--1xminint]", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE), "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        assertEqualsLong("[oracle:lifted-maxx1]", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1), "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertEqualsLong("[oracle:lifted-minx1]", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1), "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertEqualsLong("[oracle:lifted-maxx-1]", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1), "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");
    }

    private static void checkLiftedOracleRejections() {
        assertThrowsArithmetic("[oracle:lifted-minx-1-throws]", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("[oracle:lifted-minx100-throws]", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("[oracle:lifted-minxmaxint-throws]", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("[oracle:lifted-maxxminint-throws]", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkMetamorphicNegationAgreement(FuzzedDataProvider data) {
        long val = (long) data.consumeInt(-1_000_000, 1_000_000);
        try {
            long lhs = FieldUtils.safeMultiply(val, -1);
            long rhs = (long) FieldUtils.safeNegate((int) val);
            if (lhs != rhs) {
                throw new FuzzerSecurityIssueLow("[oracle:negation-agreement] metamorphic violation: safeMultiply(v,-1) must agree with safeNegate(v) on int-range non-overflow input; input=" + val + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }
    }

    private static void checkOverloadAgreementLongIntLongLong(FuzzedDataProvider data) {
        long val1;
        int val2;
        try {
            val1 = (long) data.consumeInt(-1_000_000, 1_000_000);
            val2 = data.consumeInt(-1_000_000, 1_000_000);
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        long r2;
        try {
            r2 = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        if (r1 != r2) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] relation overload_agreement_longInt_longLong_small_inputs violated: inputs=" + val1 + "," + val2 + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void checkOverloadAgreementLongIntIntInt(FuzzedDataProvider data) {
        int a;
        int b;
        try {
            a = data.consumeInt(-46340, 46340);
            b = data.consumeInt(-46340, 46340);
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        long r1;
        try {
            r1 = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        int r2;
        try {
            r2 = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }

        if (r1 != (long) r2) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] relation overload_agreement_longInt_intInt_small_inputs violated: inputs=" + a + "," + b + " lhs=" + r1 + " rhs=" + r2);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + oracleId + " semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow("[oracle:unnamed-check] " + oracleId + " semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(" + val1 + ", " + val2 + ") but returned " + actual);
        } catch (ArithmeticException expected) {
        }
    }
}