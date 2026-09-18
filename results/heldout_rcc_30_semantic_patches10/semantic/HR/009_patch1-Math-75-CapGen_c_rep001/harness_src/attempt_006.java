package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.HashSet;
import java.util.Iterator;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency f = new Frequency();

        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
        int oneI = 1;
        int twoI = 2;
        int threeI = 3;
        double tolerance = 10E-15;

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        checkDouble("lifted-one-pct-v3", 0.25d, f.getPct(1), tolerance, "f.getPct(1)");
        checkDouble("lifted-two-pct-v3", 0.25d, f.getPct(Long.valueOf(2)), tolerance, "f.getPct(Long.valueOf(2))");
        checkDouble("lifted-three-pct-v3", 0.5d, f.getPct(threeL), tolerance, "f.getPct(threeL)");
        checkDouble("lifted-three-object-pct-v3", 0.5d, f.getPct((Object) (Integer.valueOf(3))), tolerance, "f.getPct((Object) Integer.valueOf(3))");
        checkDouble("lifted-five-pct-v3", 0.0d, f.getPct(5), tolerance, "f.getPct(5)");
        checkDouble("lifted-foo-pct-v3", 0.0d, f.getPct("foo"), tolerance, "f.getPct(\"foo\")");
        checkDouble("lifted-one-cumpct-v3", 0.25d, f.getCumPct(1), tolerance, "f.getCumPct(1)");
        checkDouble("lifted-two-cumpct-v3", 0.50d, f.getCumPct(Long.valueOf(2)), tolerance, "f.getCumPct(Long.valueOf(2))");
        checkDouble("lifted-integer-argument-v3", 0.50d, f.getCumPct(Integer.valueOf(2)), tolerance, "f.getCumPct(Integer.valueOf(2))");
        checkDouble("lifted-three-cumpct-v3", 1.0d, f.getCumPct(threeL), tolerance, "f.getCumPct(threeL)");
        checkDouble("lifted-five-cumpct-v3", 1.0d, f.getCumPct(5), tolerance, "f.getCumPct(5)");
        checkDouble("lifted-zero-cumpct-v3", 0.0d, f.getCumPct(0), tolerance, "f.getCumPct(0)");
        checkDouble("lifted-foo-cumpct-v3", 0.0d, f.getCumPct("foo"), tolerance, "f.getCumPct(\"foo\")");

        Frequency g = new Frequency();
        HashSet<Long> expectedDistinct = new HashSet<Long>();
        int mutations = data.consumeInt(1, 20);
        for (int i = 0; i < mutations; i++) {
            long v = data.consumeInt(-1000000, 1000000);
            expectedDistinct.add(Long.valueOf(v));
            switch (data.consumeInt(0, 4)) {
                case 0:
                    g.addValue((int) v);
                    break;
                case 1:
                    g.addValue(v);
                    break;
                case 2:
                    g.addValue(Integer.valueOf((int) v));
                    break;
                case 3:
                    g.addValue((Object) Integer.valueOf((int) v));
                    break;
                default:
                    g.addValue(Long.valueOf(v));
                    break;
            }
        }

        Object pctProbe = Integer.valueOf(data.consumeInt(-1000000, 1000000));
        g.getPct(pctProbe);

        String before = g.toString();
        long beforeSum = g.getSumFreq();

        Iterator<Comparable<?>> it = g.valuesIterator();
        Long previous = null;
        int seen = 0;
        while (it.hasNext()) {
            Comparable<?> key = it.next();
            seen++;

            if (!(key instanceof Long)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:iterator-integral-normalization] semantic mismatch: valuesIterator returned non-Long key after integral-only adds keyClass="
                        + (key == null ? "null" : key.getClass().getName()) + " key=" + key);
            }

            Long current = (Long) key;
            if (previous != null && previous.longValue() >= current.longValue()) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:iterator-sorted-order] semantic mismatch: valuesIterator not in strict ascending order previous="
                        + previous + " current=" + current);
            }

            if (!expectedDistinct.contains(current)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:iterator-membership] semantic mismatch: iterator reported unseen normalized value current="
                        + current + " expectedDistinct=" + expectedDistinct);
            }

            // Documented guarantees used here:
            // - valuesIterator() returns the set of values that have been added.
            // - Integral values are converted to Longs when added.
            // Therefore, for integral-only inputs, the iterator must enumerate exactly the distinct normalized Long keys.
            if (g.getCount(current) <= 0) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:iterator-positive-count] consistency violation: iterated key has non-positive count key="
                        + current + " count=" + g.getCount(current));
            }
            previous = current;
        }

        if (seen != expectedDistinct.size()) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:iterator-distinct-cardinality] consistency violation: iterator distinct count disagrees with normalized inserted set seen="
                    + seen + " expected=" + expectedDistinct.size());
        }

        // Read-only post-condition: getPct(Object) is a query over the existing frequency distribution.
        // A patch that deletes bookkeeping or mutates receiver state while answering would violate this.
        String after = g.toString();
        long afterSum = g.getSumFreq();
        if (beforeSum != afterSum || !before.equals(after)) {
            throw new RuntimeException(
                "[oracle:getpct-object-readonly-via-tostring] metamorphic violation: read-only query changed observable state input="
                    + pctProbe + " beforeSum=" + beforeSum + " afterSum=" + afterSum
                    + " beforeToString=" + escape(before) + " afterToString=" + escape(after));
        }
    }

    private static void checkDouble(String oracleId, double expected, double actual, double tolerance, String expr) {
        if (Double.isNaN(expected)) {
            if (!Double.isNaN(actual)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=NaN actual=" + actual);
            }
            return;
        }
        if (Double.isNaN(actual) || Math.abs(expected - actual) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + expr + " expected=" + expected + " actual=" + actual
                    + " tolerance=" + tolerance);
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}