package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.joda.time.Duration;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkEquals("lifted-0x0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        checkEquals("lifted-1x1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        checkEquals("lifted-1x3", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        checkEquals("lifted-3x1", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");

        checkEquals("lifted-2x3", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        checkEquals("lifted-2x-3", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        checkEquals("lifted--2x3", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        checkEquals("lifted--2x-3", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");

        checkEquals(
                "lifted--1xIntegerMin",
                -1L * Integer.MIN_VALUE,
                FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");

        checkEquals(
                "lifted-longMaxx1",
                Long.MAX_VALUE,
                FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        checkEquals(
                "lifted-longMinx1",
                Long.MIN_VALUE,
                FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        checkEquals(
                "lifted-longMaxx-1",
                -Long.MAX_VALUE,
                FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        checkThrowsArithmetic(
                "lifted-longMinx-1-throws",
                Long.MIN_VALUE,
                -1,
                "FieldUtils.safeMultiply(Long.MIN_VALUE, -1)");
        checkThrowsArithmetic(
                "lifted-longMinx100-throws",
                Long.MIN_VALUE,
                100,
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 100)");
        checkThrowsArithmetic(
                "lifted-longMinxIntegerMax-throws",
                Long.MIN_VALUE,
                Integer.MAX_VALUE,
                "FieldUtils.safeMultiply(Long.MIN_VALUE, Integer.MAX_VALUE)");
        checkThrowsArithmetic(
                "lifted-longMaxxIntegerMin-throws",
                Long.MAX_VALUE,
                Integer.MIN_VALUE,
                "FieldUtils.safeMultiply(Long.MAX_VALUE, Integer.MIN_VALUE)");

        long moderateVal = data.consumeInt(-1_000_000, 1_000_000);
        int moderateFactor = data.consumeInt(-1_000, 1_000);

        try {
            long lhs = FieldUtils.safeMultiply(moderateVal, moderateFactor);
            long rhs = FieldUtils.safeMultiply(moderateVal, (long) moderateFactor);
            if (lhs != rhs) {
                throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: safeMultiply(long,int) must agree with safeMultiply(long,long) on the same mathematical inputs "
                                + "inputVal=" + moderateVal
                                + " inputFactor=" + moderateFactor
                                + " lhs=" + lhs
                                + " rhs=" + rhs);
            }
        } catch (Throwable ignored) {
        }

        long baseMillis = data.consumeInt(-1_000_000, 1_000_000);
        long durationToAdd = data.consumeInt(-1_000_000, 1_000_000);
        int scalar = data.consumeInt(-1_000, 1_000);

        try {
            Duration base = new Duration(baseMillis);
            Duration actual = base.withDurationAdded(durationToAdd, scalar);
            long expectedMillis = FieldUtils.safeAdd(base.getMillis(), FieldUtils.safeMultiply(durationToAdd, scalar));
            long actualMillis = actual.getMillis();
            if (actualMillis != expectedMillis) {
                throw new RuntimeException(
                        "[oracle:duration-postcondition] metamorphic violation: Duration.withDurationAdded(long,int) is documented by its implementation to return new Duration(safeAdd(getMillis(), safeMultiply(durationToAdd, scalar))); a throw-deleting or wrong-value multiply patch breaks this observable result "
                                + "baseMillis=" + baseMillis
                                + " durationToAdd=" + durationToAdd
                                + " scalar=" + scalar
                                + " expectedMillis=" + expectedMillis
                                + " actualMillis=" + actualMillis);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void checkEquals(String oracleId, long expected, long actual, String expr) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr
                            + " expected=" + expected
                            + " actual=" + actual);
        }
    }

    private static void checkThrowsArithmetic(String oracleId, long val1, int val2, String expr) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from "
                            + expr + " but returned=" + actual);
        } catch (ArithmeticException expected) {
        }
    }
}