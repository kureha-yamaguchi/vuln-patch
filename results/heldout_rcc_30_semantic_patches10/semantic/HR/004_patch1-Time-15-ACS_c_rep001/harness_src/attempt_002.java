package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertEqualsLong("lifted-00", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertEqualsLong("lifted-01", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertEqualsLong("lifted-02", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertEqualsLong("lifted-03", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertEqualsLong("lifted-04", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertEqualsLong("lifted-05", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertEqualsLong("lifted-06", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertEqualsLong("lifted-07", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertEqualsLong("lifted-08", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        assertEqualsLong("lifted-09", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertEqualsLong("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertEqualsLong("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        assertThrowsArithmetic("lifted-12", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("lifted-13", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("lifted-14", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("lifted-15", Long.MAX_VALUE, Integer.MIN_VALUE);

        long val1 = ((long) data.consumeInt() << 32) ^ (data.consumeInt() & 0xffffffffL);
        int val2 = data.consumeInt();

        Long siblingExpected;
        try {
            siblingExpected = Long.valueOf(FieldUtils.safeMultiply(val1, (long) val2));
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        Long siblingActual;
        try {
            siblingActual = Long.valueOf(FieldUtils.safeMultiply(val1, val2));
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (!siblingActual.equals(siblingExpected)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:relation-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                            + val1 + " inputVal2=" + val2 + " lhs=" + siblingActual.longValue() + " rhs="
                            + siblingExpected.longValue());
        }

        int a = data.consumeInt();
        int b = data.consumeInt();

        Integer intResult;
        try {
            intResult = Integer.valueOf(FieldUtils.safeMultiply(a, b));
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        Long longResult;
        try {
            longResult = Long.valueOf(FieldUtils.safeMultiply((long) a, b));
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (longResult.longValue() != (long) intResult.intValue()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:relation-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int product is valid inputA="
                            + a + " inputB=" + b + " lhs=" + longResult.longValue() + " rhs="
                            + intResult.intValue());
        }

        long n = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long viaOne = FieldUtils.safeMultiply(n, 1);
            if (viaOne != n) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:identity-times-one] semantic mismatch: multiplication by one must preserve the value input="
                                + n + " actual=" + viaOne + " expected=" + n);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        long m = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long viaZero = FieldUtils.safeMultiply(m, 0);
            if (viaZero != 0L) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:annihilator-times-zero] semantic mismatch: multiplication by zero must yield zero input="
                                + m + " actual=" + viaZero + " expected=0");
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        int moderate = data.consumeInt(-1_000_000, 1_000_000);
        int sign = data.consumeBoolean() ? 1 : -1;
        try {
            long product = FieldUtils.safeMultiply((long) moderate, sign);
            int negated = FieldUtils.safeNegate(moderate);
            long expected = sign == 1 ? (long) moderate : (long) negated;
            /*
             * Contract justification: safeMultiply performs exact multiplication with overflow checks,
             * and safeNegate performs exact negation with overflow checks. For moderate values and sign
             * restricted to {-1,1}, multiplying by 1 must return the same value and multiplying by -1
             * must equal safeNegate(value). A patch that only suppresses exceptions or returns a wrong
             * fallback value would violate this observable post-condition without needing any throw.
             */
            if (product != expected) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:sign-sibling] metamorphic violation: multiply by +/-1 must match identity/negation input="
                                + moderate + " sign=" + sign + " actual=" + product + " expected=" + expected);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: FieldUtils.safeMultiply(" + val1 + ", " + val2
                            + ") was expected to throw ArithmeticException but returned " + actual);
        } catch (ArithmeticException expected) {
        }
    }
}