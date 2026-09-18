package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Iterator;

public class FuzzHarness {
    private static final double TEST_TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedTestPctsReplica();
        runPrefixRatioFromIteratorOracle(data);
    }

    private static void runLiftedTestPctsReplica() {
        final Frequency f = new Frequency();

        final long oneL = 1;
        final long twoL = 2;
        final long threeL = 3;
        final int oneI = 1;
        final int twoI = 2;
        final int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        checkClose("replica-one-pct", 0.25, f.getPct(1), TEST_TOLERANCE, "f.getPct(1)");
        checkClose("replica-two-pct", 0.25, f.getPct(Long.valueOf(2)), TEST_TOLERANCE, "f.getPct(Long.valueOf(2))");
        checkClose("replica-three-pct", 0.5, f.getPct(threeL), TEST_TOLERANCE, "f.getPct(threeL)");
        checkClose("replica-three-object-pct", 0.5, f.getPct((Object) (Integer.valueOf(3))), TEST_TOLERANCE, "f.getPct((Object) Integer.valueOf(3))");
        checkClose("replica-five-pct", 0.0, f.getPct(5), TEST_TOLERANCE, "f.getPct(5)");
        checkClose("replica-foo-pct", 0.0, f.getPct("foo"), TEST_TOLERANCE, "f.getPct(\"foo\")");
        checkClose("replica-one-cum-pct", 0.25, f.getCumPct(1), TEST_TOLERANCE, "f.getCumPct(1)");
        checkClose("replica-two-cum-pct", 0.50, f.getCumPct(Long.valueOf(2)), TEST_TOLERANCE, "f.getCumPct(Long.valueOf(2))");
        checkClose("replica-integer-argument-cum-pct", 0.50, f.getCumPct(Integer.valueOf(2)), TEST_TOLERANCE, "f.getCumPct(Integer.valueOf(2))");
        checkClose("replica-three-cum-pct", 1.0, f.getCumPct(threeL), TEST_TOLERANCE, "f.getCumPct(threeL)");
        checkClose("replica-five-cum-pct", 1.0, f.getCumPct(5), TEST_TOLERANCE, "f.getCumPct(5)");
        checkClose("replica-zero-cum-pct", 0.0, f.getCumPct(0), TEST_TOLERANCE, "f.getCumPct(0)");
        checkClose("replica-foo-cum-pct", 0.0, f.getCumPct("foo"), TEST_TOLERANCE, "f.getCumPct(\"foo\")");
    }

    private static void runPrefixRatioFromIteratorOracle(FuzzedDataProvider data) {
        Frequency f;
        Integer query;
        double actualCumPct;
        long reportedSum;
        long recomputedPrefix = 0L;

        try {
            f = new Frequency();
            int m = data.consumeInt(1, 8);
            for (int i = 0; i < m; i++) {
                int v = data.consumeInt(-1000, 1000);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }

            query = Integer.valueOf(data.consumeInt(-1000, 1000));

            reportedSum = f.getSumFreq();

            Iterator values = f.valuesIterator();
            while (values.hasNext()) {
                Object next = values.next();
                if (!(next instanceof Number)) {
                    return;
                }
                long key = ((Number) next).longValue();
                if (key <= query.intValue()) {
                    recomputedPrefix += f.getCount((Comparable<?>) next);
                }
            }

            actualCumPct = f.getCumPct((Object) query);

            // Contract justification:
            // getCumPct(Comparable) returns getCumFreq(v) / getSumFreq().
            // Recomputing the cumulative frequency by iterating the object's own sorted distinct values
            // and summing getCount(key) for every key <= query is an independent derivation of the same
            // quantity from real API outputs. A patch that merely redirects or masks one accessor can leave
            // this summary inconsistent even when no exception is thrown.
        } catch (Throwable e) {
            return;
        }

        if (reportedSum <= 0L) {
            return;
        }

        double expectedCumPct = (double) recomputedPrefix / (double) reportedSum;
        if (!closeEnough(expectedCumPct, actualCumPct, 1e-12)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:iterator-prefix-ratio-cumpct] consistency violation: query=" + query
                    + " expectedCumPct=" + expectedCumPct
                    + " actualCumPct=" + actualCumPct
                    + " recomputedPrefix=" + recomputedPrefix
                    + " reportedSum=" + reportedSum);
        }

        try {
            f.getPct((Object) query);
        } catch (Throwable ignored) {
            return;
        }
    }

    private static void checkClose(String oracleId, double expected, double actual, double tol, String what) {
        if (!closeEnough(expected, actual, tol)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what
                    + " expected=" + expected
                    + " actual=" + actual);
        }
    }

    private static boolean closeEnough(double expected, double actual, double tol) {
        if (Double.doubleToLongBits(expected) == Double.doubleToLongBits(actual)) {
            return true;
        }
        return Math.abs(expected - actual) <= tol;
    }
}