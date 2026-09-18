package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();

        long fuzzVal1 = data.consumeInt(-1_000_000, 1_000_000);
        int fuzzVal2 = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply(fuzzVal1, fuzzVal2);
            long rhs = FieldUtils.safeMultiply(fuzzVal1, (long) fuzzVal2);
            // Contract: same-name overloads safeMultiply(long,int) and safeMultiply(long,long)
            // both "Multiply two values throwing an exception if overflow occurs"; for the same
            // mathematical operands on inputs where both return normally, they must produce the
            // same product. A throw-deleting or wrong-value patch in the long,int overload breaks this.
            if (lhs != rhs) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long)"
                        + " input=(" + fuzzVal1 + "," + fuzzVal2 + ") lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (ArithmeticException e) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        int a = data.consumeInt(-1_000_000, 1_000_000);
        int b = data.consumeInt(-1_000_000, 1_000_000);
        try {
            long product = FieldUtils.safeMultiply((long) a, b);
            if (b != 0) {
                long recovered = product / b;
                // Oracle from the input itself: we first choose moderate a and b; for non-zero b,
                // if safeMultiply returns normally then the returned product must satisfy exact
                // integer division back to the original multiplicand.
                if (recovered != a) {
                    throw new RuntimeException(
                        "[oracle:division-inverse] metamorphic violation: product/b != original"
                            + " input=(" + a + "," + b + ") product=" + product + " recovered=" + recovered);
                }
            }
        } catch (ArithmeticException e) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkLiftedTestOracles() {
        assertEqualsLong("seed-0", 0L, FieldUtils.safeMultiply(0L, 0));
        assertEqualsLong("seed-1", 1L, FieldUtils.safeMultiply(1L, 1));
        assertEqualsLong("seed-2", 3L, FieldUtils.safeMultiply(1L, 3));
        assertEqualsLong("seed-3", 3L, FieldUtils.safeMultiply(3L, 1));

        assertEqualsLong("seed-4", 6L, FieldUtils.safeMultiply(2L, 3));
        assertEqualsLong("seed-5", -6L, FieldUtils.safeMultiply(2L, -3));
        assertEqualsLong("seed-6", -6L, FieldUtils.safeMultiply(-2L, 3));
        assertEqualsLong("seed-7", 6L, FieldUtils.safeMultiply(-2L, -3));

        assertEqualsLong("seed-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE));

        assertEqualsLong("seed-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1));
        assertEqualsLong("seed-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1));
        assertEqualsLong("seed-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1));

        assertThrowsArithmetic("seed-12", Long.MIN_VALUE, -1);
        assertThrowsArithmetic("seed-13", Long.MIN_VALUE, 100);
        assertThrowsArithmetic("seed-14", Long.MIN_VALUE, Integer.MAX_VALUE);
        assertThrowsArithmetic("seed-15", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void assertEqualsLong(String id, long expected, long actual) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertThrowsArithmetic(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected ArithmeticException for input=("
                    + val1 + "," + val2 + ") actualReturn=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}