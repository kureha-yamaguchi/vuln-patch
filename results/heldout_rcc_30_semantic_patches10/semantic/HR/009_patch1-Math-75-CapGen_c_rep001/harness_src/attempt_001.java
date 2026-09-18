package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Iterator;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency f = new Frequency();

        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        try {
            f.addValue(oneL);
            f.addValue(twoL);
            f.addValue(oneI);
            f.addValue(twoI);
            f.addValue(threeL);
            f.addValue(threeL);
            f.addValue(3);
            f.addValue(threeI);
        } catch (Throwable t) {
            return;
        }

        double onePct;
        double twoPct;
        double threePct;
        double threeObjectPct;
        double fivePct;
        double fooPct;
        double oneCumPct;
        double twoCumPct;
        double integerArgumentCumPct;
        double threeCumPct;
        double fiveCumPct;
        double zeroCumPct;
        double fooCumPct;
        try {
            onePct = f.getPct(1);
            twoPct = f.getPct(Long.valueOf(2));
            threePct = f.getPct(threeL);
            threeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
            fivePct = f.getPct(5);
            fooPct = f.getPct("foo");
            oneCumPct = f.getCumPct(1);
            twoCumPct = f.getCumPct(Long.valueOf(2));
            integerArgumentCumPct = f.getCumPct(Integer.valueOf(2));
            threeCumPct = f.getCumPct(threeL);
            fiveCumPct = f.getCumPct(5);
            zeroCumPct = f.getCumPct(0);
            fooCumPct = f.getCumPct("foo");
        } catch (Throwable t) {
            return;
        }

        assertDouble("[oracle:testPcts-one-pct]", "one pct", 0.25, onePct, TOLERANCE);
        assertDouble("[oracle:testPcts-two-pct]", "two pct", 0.25, twoPct, TOLERANCE);
        assertDouble("[oracle:testPcts-three-pct]", "three pct", 0.5, threePct, TOLERANCE);
        assertDouble("[oracle:testPcts-three-object-pct]", "three (Object) pct", 0.5, threeObjectPct, TOLERANCE);
        assertDouble("[oracle:testPcts-five-pct]", "five pct", 0.0, fivePct, TOLERANCE);
        assertDouble("[oracle:testPcts-foo-pct]", "foo pct", 0.0, fooPct, TOLERANCE);
        assertDouble("[oracle:testPcts-one-cum-pct]", "one cum pct", 0.25, oneCumPct, TOLERANCE);
        assertDouble("[oracle:testPcts-two-cum-pct]", "two cum pct", 0.50, twoCumPct, TOLERANCE);
        assertDouble("[oracle:testPcts-integer-argument]", "Integer argument", 0.50, integerArgumentCumPct, TOLERANCE);
        assertDouble("[oracle:testPcts-three-cum-pct]", "three cum pct", 1.0, threeCumPct, TOLERANCE);
        assertDouble("[oracle:testPcts-five-cum-pct]", "five cum pct", 1.0, fiveCumPct, TOLERANCE);
        assertDouble("[oracle:testPcts-zero-cum-pct]", "zero cum pct", 0.0, zeroCumPct, TOLERANCE);
        assertDouble("[oracle:testPcts-foo-cum-pct]", "foo cum pct", 0.0, fooCumPct, TOLERANCE);

        Frequency fuzzFreq;
        int m;
        int n;
        try {
            fuzzFreq = new Frequency();
            m = data.consumeInt(1, 8);
            for (int i = 0; i < m; i++) {
                int v = data.consumeInt(-1_000_000, 1_000_000);
                if (data.consumeBoolean()) {
                    fuzzFreq.addValue(v);
                } else {
                    fuzzFreq.addValue((long) v);
                }
            }
            n = data.consumeInt(-1_000_000, 1_000_000);
        } catch (Throwable t) {
            return;
        }

        long sum;
        long count;
        double pctObject;
        try {
            sum = fuzzFreq.getSumFreq();
            count = fuzzFreq.getCount(n);
            pctObject = fuzzFreq.getPct((Object) Integer.valueOf(n));
        } catch (Throwable t) {
            return;
        }

        if (sum > 0) {
            double expectedPct = (double) count / (double) sum;
            double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(expectedPct), Math.abs(pctObject)));
            if (Math.abs(expectedPct - pctObject) > tol) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:getPctObject_matches_count_over_sum_for_integer_object] consistency violation: " +
                    "getPct((Object)Integer.valueOf(n)) must equal getCount(n)/getSumFreq() for integral values; " +
                    "n=" + n + " count=" + count + " sum=" + sum +
                    " expected=" + expectedPct + " actual=" + pctObject);
            }
        }

        long sumBefore;
        int hashBefore;
        String strBefore;
        long sumAfter;
        int hashAfter;
        String strAfter;
        try {
            sumBefore = fuzzFreq.getSumFreq();
            hashBefore = fuzzFreq.hashCode();
            strBefore = fuzzFreq.toString();
            fuzzFreq.getPct((Object) Integer.valueOf(n));
            sumAfter = fuzzFreq.getSumFreq();
            hashAfter = fuzzFreq.hashCode();
            strAfter = fuzzFreq.toString();
        } catch (Throwable t) {
            return;
        }

        /* getPct is documented as a getter returning a proportion, so it must be read-only.
           A patch that merely dodges the bad path by mutating or caching incorrect state would violate this. */
        if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(strBefore, strAfter)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:getPct_object_is_read_only] metamorphic violation: getPct(Object) changed observable state; " +
                "sumBefore=" + sumBefore + " sumAfter=" + sumAfter +
                " hashBefore=" + hashBefore + " hashAfter=" + hashAfter +
                " strBefore=" + escape(strBefore) + " strAfter=" + escape(strAfter));
        }

        long reportedSum;
        long recomputedSum = 0;
        try {
            reportedSum = fuzzFreq.getSumFreq();
            Iterator<?> it = fuzzFreq.valuesIterator();
            while (it.hasNext()) {
                Comparable<?> v = (Comparable<?>) it.next();
                recomputedSum += fuzzFreq.getCount(v);
            }
        } catch (Throwable t) {
            return;
        }

        /* Consistency cross-check: getSumFreq() is the total frequency, and valuesIterator() exposes each distinct
           value present. Summing getCount(v) over all distinct values must therefore reproduce getSumFreq(). */
        if (reportedSum != recomputedSum) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:getSumFreq_recomputed_from_valuesIterator] consistency violation: " +
                "reportedSum=" + reportedSum + " recomputedSum=" + recomputedSum);
        }
    }

    private static void assertDouble(String oracleId, String label, double expected, double actual, double tolerance) {
        if (Double.isNaN(expected) ? !Double.isNaN(actual) : Math.abs(expected - actual) > tolerance) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracleId + " semantic mismatch: " + label +
                " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}