package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        assertEqualsLong("[oracle:lifted-0]", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertEqualsLong("[oracle:lifted-1]", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertEqualsLong("[oracle:lifted-2]", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertEqualsLong("[oracle:lifted-3]", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        assertEqualsLong("[oracle:lifted-4]", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertEqualsLong("[oracle:lifted-5]", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertEqualsLong("[oracle:lifted-6]", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertEqualsLong("[oracle:lifted-7]", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        assertEqualsLong("[oracle:lifted-8]", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE), "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        assertEqualsLong("[oracle:lifted-9]", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1), "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertEqualsLong("[oracle:lifted-10]", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1), "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertEqualsLong("[oracle:lifted-11]", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1), "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        expectArithmeticException("[oracle:lifted-12]", Long.MIN_VALUE, -1, "FieldUtils.safeMultiply(Long.MIN_VALUE, -1)");
        expectArithmeticException("[oracle:lifted-13]", Long.MIN_VALUE, 100, "FieldUtils.safeMultiply(Long.MIN_VALUE, 100)");
        expectArithmeticException("[oracle:lifted-14]", Long.MIN_VALUE, Integer.MAX_VALUE, "FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)");
        expectArithmeticException("[oracle:lifted-15]", Long.MAX_VALUE, Integer.MIN_VALUE, "FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)");

        long fuzzVal1 = (((long) data.consumeInt()) << 32) ^ (((long) data.consumeInt()) & 0xffffffffL);
        int fuzzVal2 = data.consumeInt();
        long expectedLongLong;
        try {
            expectedLongLong = FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2);
        } catch (Throwable t) {
            return;
        }
        long actualLongInt;
        try {
            actualLongInt = FieldUtils.safeMultiply(fuzzVal1, fuzzVal2);
        } catch (Throwable t) {
            return;
        }
        if (actualLongInt != expectedLongLong) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:relation-longint-longlong] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on equivalent inputs inputVal1="
                    + fuzzVal1 + " inputVal2=" + fuzzVal2 + " lhs=" + actualLongInt + " rhs=" + expectedLongLong);
        }

        int a = data.consumeInt();
        int b = data.consumeInt();
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Throwable t) {
            return;
        }
        long widenedResult;
        try {
            widenedResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Throwable t) {
            return;
        }
        if (widenedResult != (long) intResult) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:relation-intint-longint] metamorphic violation: safeMultiply(int,int) must agree with safeMultiply(long,int) when the int result is valid inputA="
                    + a + " inputB=" + b + " lhs=" + widenedResult + " rhs=" + intResult);
        }

        long moderate = data.consumeInt(-1_000_000, 1_000_000);
        int scalar = data.consumeInt(-1_000_000, 1_000_000);
        long left;
        long rightBase;
        try {
            left = FieldUtils.safeMultiply(moderate, scalar);
            rightBase = FieldUtils.safeMultiply(moderate, 1);
        } catch (Throwable t) {
            return;
        }
        long right;
        try {
            right = FieldUtils.safeMultiply(rightBase, scalar);
        } catch (Throwable t) {
            return;
        }
        if (left != right) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:relation-identity-compose] metamorphic violation: multiplication by 1 is the identity, so safeMultiply(x,s) must equal safeMultiply(safeMultiply(x,1),s) inputX="
                    + moderate + " inputScalar=" + scalar + " lhs=" + left + " rhs=" + right);
        }

        long identityInput = data.consumeInt(-1_000_000, 1_000_000);
        long identityActual;
        try {
            identityActual = FieldUtils.safeMultiply(identityInput, 1);
        } catch (Throwable t) {
            return;
        }
        if (identityActual != identityInput) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:post-identity] semantic mismatch: documented multiplication semantics require multiplying by 1 to preserve the value; a throw-deleting or wrong-value patch would break this observable post-condition input="
                    + identityInput + " actual=" + identityActual);
        }
    }

    private static void assertEqualsLong(String oracleId, long expected, long actual, String call) {
        if (actual != expected) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracleId + " semantic mismatch: " + call + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmeticException(String oracleId, long val1, int val2, String call) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracleId + " semantic mismatch: " + call + " expected ArithmeticException but returned actual=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}