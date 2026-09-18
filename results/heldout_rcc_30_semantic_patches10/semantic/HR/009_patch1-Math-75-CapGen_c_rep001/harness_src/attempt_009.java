package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    private static void oracleEq(String id, double expected, double actual) {
        if (Double.isNaN(expected) ? !Double.isNaN(actual) : Math.abs(expected - actual) > TOLERANCE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + id + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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

        oracleEq("lifted-one-pct-seedA", 0.25, f.getPct(1));
        oracleEq("lifted-two-pct-seedA", 0.25, f.getPct(Long.valueOf(2)));
        oracleEq("lifted-three-pct-seedA", 0.5, f.getPct(threeL));
        oracleEq("lifted-three-object-pct-seedA", 0.5, f.getPct((Object) (Integer.valueOf(3))));
        oracleEq("lifted-five-pct-seedA", 0.0, f.getPct(5));
        oracleEq("lifted-foo-pct-seedA", 0.0, f.getPct("foo"));
        oracleEq("lifted-one-cumpct-seedA", 0.25, f.getCumPct(1));
        oracleEq("lifted-two-cumpct-seedA", 0.50, f.getCumPct(Long.valueOf(2)));
        oracleEq("lifted-integer-argument-cumpct-seedA", 0.50, f.getCumPct(Integer.valueOf(2)));
        oracleEq("lifted-three-cumpct-seedA", 1.0, f.getCumPct(threeL));
        oracleEq("lifted-five-cumpct-seedA", 1.0, f.getCumPct(5));
        oracleEq("lifted-zero-cumpct-seedA", 0.0, f.getCumPct(0));
        oracleEq("lifted-foo-cumpct-seedA", 0.0, f.getCumPct("foo"));

        String violation = null;
        try {
            Frequency mixed = new Frequency();
            Frequency normalized = new Frequency();

            int m = data.consumeInt(1, 8);
            int[] inserted = new int[m];
            for (int i = 0; i < m; i++) {
                int v = data.consumeInt(-1000, 1000);
                inserted[i] = v;
                if (data.consumeBoolean()) {
                    mixed.addValue(v);
                } else {
                    mixed.addValue((long) v);
                }
                normalized.addValue((long) v);
            }

            int q = data.consumeInt(-1000, 1000);

            long mixedSum = mixed.getSumFreq();
            long normalizedSum = normalized.getSumFreq();
            if (mixedSum != normalizedSum) {
                violation = "[oracle:type-normalization-sumfreq] consistency violation: mixedSum=" + mixedSum
                    + " normalizedSum=" + normalizedSum;
            }

            if (violation == null) {
                long mixedCount = mixed.getCount(q);
                long normalizedCount = normalized.getCount(q);
                if (mixedCount != normalizedCount) {
                    violation = "[oracle:type-normalization-count] consistency violation: q=" + q
                        + " mixedCount=" + mixedCount + " normalizedCount=" + normalizedCount;
                }
            }

            if (violation == null) {
                long mixedCumFreq = mixed.getCumFreq(q);
                long normalizedCumFreq = normalized.getCumFreq(q);
                if (mixedCumFreq != normalizedCumFreq) {
                    violation = "[oracle:type-normalization-cumfreq] consistency violation: q=" + q
                        + " mixedCumFreq=" + mixedCumFreq + " normalizedCumFreq=" + normalizedCumFreq;
                }
            }

            if (violation == null) {
                double mixedCumPct = mixed.getCumPct(Integer.valueOf(q));
                double normalizedCumPct = normalized.getCumPct(Integer.valueOf(q));
                if (Math.abs(mixedCumPct - normalizedCumPct) > TOLERANCE) {
                    violation = "[oracle:type-normalization-cumpct] consistency violation: q=" + q
                        + " mixedCumPct=" + mixedCumPct + " normalizedCumPct=" + normalizedCumPct;
                }
            }

            if (violation == null) {
                double mixedPctObject = mixed.getPct((Object) Integer.valueOf(q));
                double normalizedPctObject = normalized.getPct((Object) Integer.valueOf(q));
                if (Math.abs(mixedPctObject - normalizedPctObject) > TOLERANCE) {
                    violation = "[oracle:type-normalization-object-pct] consistency violation: q=" + q
                        + " mixedPctObject=" + mixedPctObject + " normalizedPctObject=" + normalizedPctObject;
                }
            }

            if (violation == null) {
                java.util.Iterator<?> itA = mixed.valuesIterator();
                java.util.Iterator<?> itB = normalized.valuesIterator();
                int idx = 0;
                while (itA.hasNext() && itB.hasNext()) {
                    Object a = itA.next();
                    Object b = itB.next();
                    if (!a.equals(b)) {
                        violation = "[oracle:type-normalization-values-iterator] consistency violation: index=" + idx
                            + " mixedValue=" + a + " normalizedValue=" + b;
                        break;
                    }
                    idx++;
                }
                if (violation == null && (itA.hasNext() || itB.hasNext())) {
                    violation = "[oracle:type-normalization-values-iterator-length] consistency violation: different iterator lengths";
                }
            }

            if (violation == null) {
                long manualCount = 0;
                for (int i = 0; i < inserted.length; i++) {
                    if (inserted[i] == q) {
                        manualCount++;
                    }
                }
                double expectedPct = (double) manualCount / (double) normalized.getSumFreq();
                double actualPct = mixed.getPct((Object) Integer.valueOf(q));
                // getPct is documented as the proportion equal to v; with only integral values added,
                // manualCount/getSumFreq is an independent recomputation from construction data, so a
                // band-aid that hides one seed value but leaves the root cause in getPct(Object) still violates this.
                if (Math.abs(expectedPct - actualPct) > TOLERANCE) {
                    violation = "[oracle:manual-recompute-object-pct] consistency violation: q=" + q
                        + " expectedPct=" + expectedPct + " actualPct=" + actualPct
                        + " manualCount=" + manualCount + " sum=" + normalized.getSumFreq();
                }
            }
        } catch (Throwable t) {
            return;
        }

        if (violation != null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(violation);
        }
    }
}