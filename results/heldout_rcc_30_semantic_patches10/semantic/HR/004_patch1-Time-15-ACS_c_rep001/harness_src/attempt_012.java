package org.joda.time.field;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.joda.time.Duration;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkLiftedTestOracles();
        checkPublicApiCallSiteOracle();
        relationSafeMultiplyLongIntAgreesWithLongLongSibling(data);
        relationSafeMultiplyIntIntAgreesWithLongIntSibling(data);
        relationDurationWithDurationAddedMatchesSeededMultiplication(data);
    }

    private static void checkLiftedTestOracles() {
        assertLongEquals("lifted-0x0", 0L, FieldUtils.safeMultiply(0L, 0), "FieldUtils.safeMultiply(0L, 0)");
        assertLongEquals("lifted-1x1", 1L, FieldUtils.safeMultiply(1L, 1), "FieldUtils.safeMultiply(1L, 1)");
        assertLongEquals("lifted-1x3", 3L, FieldUtils.safeMultiply(1L, 3), "FieldUtils.safeMultiply(1L, 3)");
        assertLongEquals("lifted-3x1", 3L, FieldUtils.safeMultiply(3L, 1), "FieldUtils.safeMultiply(3L, 1)");

        assertLongEquals("lifted-2x3", 6L, FieldUtils.safeMultiply(2L, 3), "FieldUtils.safeMultiply(2L, 3)");
        assertLongEquals("lifted-2x-3", -6L, FieldUtils.safeMultiply(2L, -3), "FieldUtils.safeMultiply(2L, -3)");
        assertLongEquals("lifted--2x3", -6L, FieldUtils.safeMultiply(-2L, 3), "FieldUtils.safeMultiply(-2L, 3)");
        assertLongEquals("lifted--2x-3", 6L, FieldUtils.safeMultiply(-2L, -3), "FieldUtils.safeMultiply(-2L, -3)");

        assertLongEquals(
                "lifted--1xIntMin",
                -1L * Integer.MIN_VALUE,
                FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE),
                "FieldUtils.safeMultiply(-1L, Integer.MIN_VALUE)");

        assertLongEquals(
                "lifted-LongMaxx1",
                Long.MAX_VALUE,
                FieldUtils.safeMultiply(Long.MAX_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, 1)");
        assertLongEquals(
                "lifted-LongMinx1",
                Long.MIN_VALUE,
                FieldUtils.safeMultiply(Long.MIN_VALUE, 1),
                "FieldUtils.safeMultiply(Long.MIN_VALUE, 1)");
        assertLongEquals(
                "lifted-LongMaxx-1",
                -Long.MAX_VALUE,
                FieldUtils.safeMultiply(Long.MAX_VALUE, -1),
                "FieldUtils.safeMultiply(Long.MAX_VALUE, -1)");

        expectArithmeticException("lifted-LongMinx-1", Long.MIN_VALUE, -1);
        expectArithmeticException("lifted-LongMinx100", Long.MIN_VALUE, 100);
        expectArithmeticException("lifted-LongMinxIntMax", Long.MIN_VALUE, Integer.MAX_VALUE);
        expectArithmeticException("lifted-LongMaxxIntMin", Long.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static void checkPublicApiCallSiteOracle() {
        try {
            Duration.ZERO.withDurationAdded(Long.MIN_VALUE, -1);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:public-api-withDurationAdded] semantic mismatch: Duration.ZERO.withDurationAdded(Long.MIN_VALUE, -1) should throw ArithmeticException because withDurationAdded delegates to FieldUtils.safeMultiply(durationToAdd, scalar) and a throw-deleting patch would silently return a wrong Duration");
        } catch (ArithmeticException expected) {
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Exception e) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:public-api-withDurationAdded] semantic mismatch: unexpected exception type "
                            + e.getClass().getName()
                            + " from Duration.ZERO.withDurationAdded(Long.MIN_VALUE, -1)",
                    e);
        }
    }

    private static void relationSafeMultiplyLongIntAgreesWithLongLongSibling(FuzzedDataProvider data) {
        long val1 = data.consumeLong();
        int val2 = data.consumeInt();
        long expected;
        try {
            expected = FieldUtils.safeMultiply(val1, (long) val2);
        } catch (Exception e) {
            return;
        }
        long actual;
        try {
            actual = FieldUtils.safeMultiply(val1, val2);
        } catch (Exception e) {
            return;
        }
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:safeMultiplyLongInt_agreesWithLongLongSibling] metamorphic violation: safeMultiply(long,int) != safeMultiply(long,long) for equivalent inputs val1="
                            + val1
                            + " val2="
                            + val2
                            + " actual="
                            + actual
                            + " expected="
                            + expected);
        }
    }

    private static void relationSafeMultiplyIntIntAgreesWithLongIntSibling(FuzzedDataProvider data) {
        int a = data.consumeInt();
        int b = data.consumeInt();
        int intResult;
        try {
            intResult = FieldUtils.safeMultiply(a, b);
        } catch (Exception e) {
            return;
        }
        long longResult;
        try {
            longResult = FieldUtils.safeMultiply((long) a, b);
        } catch (Exception e) {
            return;
        }
        if (longResult != (long) intResult) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:safeMultiplyIntInt_agreesWithLongIntSibling] metamorphic violation: safeMultiply(int,int) != safeMultiply((long)int,int) for a="
                            + a
                            + " b="
                            + b
                            + " intResult="
                            + intResult
                            + " longResult="
                            + longResult);
        }
    }

    private static void relationDurationWithDurationAddedMatchesSeededMultiplication(FuzzedDataProvider data) {
        long base = data.consumeInt(-1_000_000, 1_000_000);
        long durationToAdd = data.consumeInt(-1_000_000, 1_000_000);
        int scalar = data.consumeInt(-1_000, 1_000);

        long expectedAdd;
        try {
            expectedAdd = FieldUtils.safeMultiply(durationToAdd, scalar);
        } catch (Exception e) {
            return;
        }

        Duration baseDuration = new Duration(base);
        Duration actualDuration;
        try {
            actualDuration = baseDuration.withDurationAdded(durationToAdd, scalar);
        } catch (Exception e) {
            return;
        }

        long expectedMillis;
        try {
            expectedMillis = FieldUtils.safeAdd(base, expectedAdd);
        } catch (Exception e) {
            return;
        }

        long actualMillis = actualDuration.getMillis();
        if (actualMillis != expectedMillis) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:duration-withDurationAdded-consistency] metamorphic violation: Duration.withDurationAdded(durationToAdd, scalar) must equal baseMillis + safeMultiply(durationToAdd, scalar) for base="
                            + base
                            + " durationToAdd="
                            + durationToAdd
                            + " scalar="
                            + scalar
                            + " actualMillis="
                            + actualMillis
                            + " expectedMillis="
                            + expectedMillis);
        }
    }

    private static void expectArithmeticException(String oracleId, long val1, int val2) {
        try {
            long actual = FieldUtils.safeMultiply(val1, val2);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(" + val1 + ", " + val2 + ") but got value " + actual);
        } catch (ArithmeticException expected) {
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (Exception e) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: expected ArithmeticException from FieldUtils.safeMultiply(" + val1 + ", " + val2 + ") but got unexpected exception " + e.getClass().getName(),
                    e);
        }
    }

    private static void assertLongEquals(String oracleId, long expected, long actual, String expr) {
        if (actual != expected) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual);
        }
    }
}