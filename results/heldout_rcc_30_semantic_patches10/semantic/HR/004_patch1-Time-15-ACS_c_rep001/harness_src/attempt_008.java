package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    /*__vpCause*/ private static Throwable __vpCause = null;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Lifted exact oracles from TestFieldUtils.testSafeMultiplyLongInt.
        checkEquals("lift-0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        checkEquals("lift-1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        checkEquals("lift-2", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        checkEquals("lift-3", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        checkEquals("lift-4", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        checkEquals("lift-5", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        checkEquals("lift-6", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        checkEquals("lift-7", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        checkEquals("lift-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        checkEquals("lift-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        checkEquals("lift-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        checkEquals("lift-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        checkArithmeticException("lift-throw-0", Long.MIN_VALUE, -1);
        checkArithmeticException("lift-throw-1", Long.MIN_VALUE, 100);
        checkArithmeticException("lift-throw-2", Long.MIN_VALUE, Integer.MAX_VALUE);
        checkArithmeticException("lift-throw-3", Long.MAX_VALUE, Integer.MIN_VALUE);

        // Generalisation from the trusted failing pair:
        // For any x, safeMultiply(x, 1) is documented multiplication and must preserve x.
        // This catches throw-deleting or wrong-value patches because the observable result must equal the multiplicative identity.
        long identityInput = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long identityActual = FieldUtils.safeMultiply(identityInput, 1);
            if (identityActual != identityInput) {
                throw new FuzzerSecurityIssueLow(
                        "[oracle:identity-one] semantic mismatch: safeMultiply(" + identityInput + ", 1) expected="
                                + identityInput + " actual=" + identityActual);
            }
        } catch (ArithmeticException e) {
            return;
        }

        // Equivalent-input sibling agreement:
        // The overloads safeMultiply(long,int) and safeMultiply(long,long) document the same overflow-checked multiplication.
        // On the same logical inputs, when the long-long call succeeds, the long-int overload must return the same value.
        long val1 = data.consumeInt(-1_000_000, 1_000_000);
        int val2 = data.consumeInt(-1_000_000, 1_000_000);
        long expectedLongLong;
        try {
            expectedLongLong = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (ArithmeticException e) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        long actualLongInt;
        try {
            actualLongInt = FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException e) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (actualLongInt != expectedLongLong) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-longint-longlong] metamorphic violation: safeMultiply(long,int) vs safeMultiply(long,long) input=("
                            + val1 + "," + val2 + ") lhs=" + actualLongInt + " rhs=" + expectedLongLong);
        }

        // Sibling agreement across int/int and long/int:
        // Both overloads document the same checked multiplication; widening an int operand to long must preserve a valid product.
        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (ArithmeticException e) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        long widenedResult;
        try {
            widenedResult = FieldUtils.safeMultiply((long) a, b);
        } catch (ArithmeticException e) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (widenedResult != (long) intResult) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:sibling-intint-longint] metamorphic violation: safeMultiply(int,int) vs safeMultiply(long,int) input=("
                            + a + "," + b + ") lhs=" + widenedResult + " rhs=" + intResult);
        }
    }

    private static void checkEquals(String oracleId, long expected, long actual, String call) {
        if (expected != actual) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + call + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void checkArithmeticException(String oracleId, long val1, int val2) {
        boolean threw = false;
        try {
            FieldUtils.safeMultiply(val1, val2);
        } catch (ArithmeticException expected) { __vpCause = expected;
            threw = true;
        }
        if (!threw) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply("
                            + val1 + ", " + val2 + ")", __vpCause);
        }
    }
}