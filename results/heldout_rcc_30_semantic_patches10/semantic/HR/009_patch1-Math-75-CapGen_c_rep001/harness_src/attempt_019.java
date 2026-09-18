package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedFrequencyTest();
        runOrderInvariantCrossCheck(data);
    }

    private static void runLiftedFrequencyTest() {
        Frequency f = new Frequency();
        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        checkDoubleOracle("oracle-lift-one-pct", 0.25, f.getPct(1), "f.getPct(1)");
        checkDoubleOracle("oracle-lift-two-pct", 0.25, f.getPct(Long.valueOf(2)), "f.getPct(Long.valueOf(2))");
        checkDoubleOracle("oracle-lift-three-pct", 0.5, f.getPct(threeL), "f.getPct(threeL)");
        checkDoubleOracle("oracle-lift-three-object-pct", 0.5, f.getPct((Object) (Integer.valueOf(3))), "f.getPct((Object) Integer.valueOf(3))");
        checkDoubleOracle("oracle-lift-five-pct", 0.0, f.getPct(5), "f.getPct(5)");
        checkDoubleOracle("oracle-lift-foo-pct", 0.0, f.getPct("foo"), "f.getPct(\"foo\")");
        checkDoubleOracle("oracle-lift-one-cum-pct", 0.25, f.getCumPct(1), "f.getCumPct(1)");
        checkDoubleOracle("oracle-lift-two-cum-pct", 0.50, f.getCumPct(Long.valueOf(2)), "f.getCumPct(Long.valueOf(2))");
        checkDoubleOracle("oracle-lift-integer-arg-cum-pct", 0.50, f.getCumPct(Integer.valueOf(2)), "f.getCumPct(Integer.valueOf(2))");
        checkDoubleOracle("oracle-lift-three-cum-pct", 1.0, f.getCumPct(threeL), "f.getCumPct(threeL)");
        checkDoubleOracle("oracle-lift-five-cum-pct", 1.0, f.getCumPct(5), "f.getCumPct(5)");
        checkDoubleOracle("oracle-lift-zero-cum-pct", 0.0, f.getCumPct(0), "f.getCumPct(0)");
        checkDoubleOracle("oracle-lift-foo-cum-pct", 0.0, f.getCumPct("foo"), "f.getCumPct(\"foo\")");
    }

    private static void runOrderInvariantCrossCheck(FuzzedDataProvider data) {
        int m = data.consumeInt(1, 8);
        int[] values = new int[m];
        for (int i = 0; i < m; i++) {
            values[i] = data.consumeInt(-1000000, 1000000);
        }
        int query = data.consumeInt(-1000000, 1000000);

        Frequency forward = new Frequency();
        Frequency reverse = new Frequency();

        try {
            for (int i = 0; i < m; i++) {
                if ((i & 1) == 0) {
                    forward.addValue(values[i]);
                } else {
                    forward.addValue((long) values[i]);
                }
            }
            for (int i = m - 1; i >= 0; i--) {
                if ((i & 1) == 0) {
                    reverse.addValue((long) values[i]);
                } else {
                    reverse.addValue(values[i]);
                }
            }
        } catch (RuntimeException e) {
            return;
        }

        long sumA;
        long sumB;
        long countA;
        long countB;
        long cumA;
        long cumB;
        double cumPctA;
        double cumPctB;
        String iterA;
        String iterB;
        try {
            sumA = forward.getSumFreq();
            sumB = reverse.getSumFreq();
            countA = forward.getCount(query);
            countB = reverse.getCount(query);
            cumA = forward.getCumFreq(query);
            cumB = reverse.getCumFreq(query);
            cumPctA = forward.getCumPct(Integer.valueOf(query));
            cumPctB = reverse.getCumPct(Integer.valueOf(query));
            iterA = snapshotValues(forward);
            iterB = snapshotValues(reverse);
        } catch (RuntimeException e) {
            return;
        }

        // Contract basis: Frequency stores frequencies of values, ordered by value, not insertion order.
        // Two objects built from the same multiset must therefore report the same counts, cumulative
        // counts/percentages, total frequency, and distinct-value iteration order. A band-aid that
        // only special-cases one top-level output can still leave these related summaries inconsistent.
        if (sumA != sumB || countA != countB || cumA != cumB || !sameDouble(cumPctA, cumPctB) || !iterA.equals(iterB)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:order-invariant-cum-summary] consistency violation: "
                    + "query=" + query
                    + " sumA=" + sumA
                    + " sumB=" + sumB
                    + " countA=" + countA
                    + " countB=" + countB
                    + " cumA=" + cumA
                    + " cumB=" + cumB
                    + " cumPctA=" + cumPctA
                    + " cumPctB=" + cumPctB
                    + " iterA=" + escape(iterA)
                    + " iterB=" + escape(iterB));
        }

        List<Long> distinctCounts = new ArrayList<Long>();
        try {
            Iterator<?> it = forward.valuesIterator();
            while (it.hasNext()) {
                Object v = it.next();
                distinctCounts.add(Long.valueOf(forward.getCount((Comparable<?>) v)));
            }
        } catch (RuntimeException e) {
            return;
        }

        long recomputed = 0L;
        for (int i = 0; i < distinctCounts.size(); i++) {
            recomputed += distinctCounts.get(i).longValue();
        }

        // Contract basis: getSumFreq() is the total frequency; summing getCount over every distinct
        // value yielded by valuesIterator() independently recomputes that same total from the object's
        // own output. This cross-check targets helper consistency in getSumFreq/getCount/iterator.
        if (recomputed != sumA) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:sum-via-distinct-counts] consistency violation: "
                    + "reportedSum=" + sumA
                    + " recomputedSum=" + recomputed
                    + " distinctCardinality=" + distinctCounts.size()
                    + " values=" + escape(iterA));
        }
    }

    private static String snapshotValues(Frequency f) {
        StringBuilder sb = new StringBuilder();
        Iterator<?> it = f.valuesIterator();
        while (it.hasNext()) {
            Object v = it.next();
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(String.valueOf(v));
        }
        return sb.toString();
    }

    private static void checkDoubleOracle(String id, double expected, double actual, String expr) {
        if (!sameDouble(expected, actual)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] [" + id + "] semantic mismatch: expr=" + expr + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean sameDouble(double expected, double actual) {
        if (Double.doubleToLongBits(expected) == Double.doubleToLongBits(actual)) {
            return true;
        }
        double diff = Math.abs(expected - actual);
        return diff <= TOLERANCE;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}