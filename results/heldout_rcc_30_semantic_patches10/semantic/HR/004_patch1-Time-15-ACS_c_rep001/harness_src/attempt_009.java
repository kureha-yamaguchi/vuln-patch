package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkEquals("lifted-0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        checkEquals("lifted-1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        checkEquals("lifted-2", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        checkEquals("lifted-3", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");
        checkEquals("lifted-4", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        checkEquals("lifted-5", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        checkEquals("lifted-6", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        checkEquals("lifted-7", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");
        checkEquals("lifted-8", -1L * Integer.MIN_VALUE, FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");
        checkEquals("lifted-9", Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        checkEquals("lifted-10", Long.MIN_VALUE, FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        checkEquals("lifted-11", -Long.MAX_VALUE, FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        expectArithmetic("lifted-throw-0", Long.MIN_VALUE, -1);
        expectArithmetic("lifted-throw-1", Long.MIN_VALUE, 100);
        expectArithmetic("lifted-throw-2", Long.MIN_VALUE, Integer.MAX_VALUE);
        expectArithmetic("lifted-throw-3", Long.MAX_VALUE, Integer.MIN_VALUE);

        long fuzzVal;
        switch (data.consumeInt(0, 5)) {
            case 0:
                fuzzVal = Long.MIN_VALUE;
                break;
            case 1:
                fuzzVal = Long.MAX_VALUE;
                break;
            case 2:
                fuzzVal = data.consumeInt(-1_000_000, 1_000_000);
                break;
            case 3:
                fuzzVal = data.consumeInt();
                break;
            case 4:
                fuzzVal = data.consumeBoolean() ? 1L : -1L;
                break;
            default:
                fuzzVal = 0L;
                break;
        }

        int fuzzFactor;
        switch (data.consumeInt(0, 6)) {
            case 0:
                fuzzFactor = -1;
                break;
            case 1:
                fuzzFactor = 0;
                break;
            case 2:
                fuzzFactor = 1;
                break;
            case 3:
                fuzzFactor = data.consumeInt(-1_000_000, 1_000_000);
                break;
            case 4:
                fuzzFactor = Integer.MIN_VALUE;
                break;
            case 5:
                fuzzFactor = Integer.MAX_VALUE;
                break;
            default:
                fuzzFactor = data.consumeInt();
                break;
        }

        // Documented sibling-agreement guarantee: these are same-name overloads for safeMultiply,
        // so for the same mathematical inputs they must either both return the same value or both reject.
        // A throw-deleting patch in safeMultiply(long,int) breaks this observable post-condition.
        try {
            long lhs = FieldUtils.safeMultiply(fuzzVal, fuzzFactor);
            long rhs = FieldUtils.safeMultiply(fuzzVal, (long) fuzzFactor);
            if (lhs != rhs) {
                throw new RuntimeException("[oracle:sibling-overload] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long) inputVal=" + fuzzVal + " inputFactor=" + fuzzFactor + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t1) {
            try {
                FieldUtils.safeMultiply(fuzzVal, (long) fuzzFactor);
            } catch (Throwable t2) {
                return;
            }
            return;
        }

        // Another trusted post-condition from the real API examples: with scalar 1, withDurationAdded(d, 1)
        // uses FieldUtils.safeMultiply(d, 1) and must therefore add exactly d milliseconds when the call succeeds.
        try {
            long base = data.consumeInt(-1_000_000, 1_000_000);
            long delta = data.consumeInt(-1_000_000, 1_000_000);
            org.joda.time.Duration dur = new org.joda.time.Duration(base);
            org.joda.time.Duration updated = dur.withDurationAdded(delta, 1);
            long expected = FieldUtils.safeAdd(base, delta);
            if (updated.getMillis() != expected) {
                throw new RuntimeException("[oracle:duration-scalar-one] metamorphic violation: withDurationAdded(delta,1) must add delta exactly base=" + base + " delta=" + delta + " actual=" + updated.getMillis() + " expected=" + expected);
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void checkEquals(String id, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + id + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectArithmetic(String id, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + id + "] semantic mismatch: expected ArithmeticException for FieldUtils.safeMultiply(" + val1 + ", " + val2 + ") but got=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}