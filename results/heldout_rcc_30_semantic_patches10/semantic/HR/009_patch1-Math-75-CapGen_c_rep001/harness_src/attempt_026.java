package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        oracleGroundTruthSeedReplica();
        oracleLowerInsertionDecreasesExactObjectPct(data);
    }

    private static void oracleGroundTruthSeedReplica() {
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

        requireDouble("[oracle:seed-one-pct]", 0.25, f.getPct(1));
        requireDouble("[oracle:seed-two-pct]", 0.25, f.getPct(Long.valueOf(2)));
        requireDouble("[oracle:seed-three-pct]", 0.5, f.getPct(threeL));
        requireDouble("[oracle:seed-three-object-pct-replica]", 0.5, f.getPct((Object) (Integer.valueOf(3))));
        requireDouble("[oracle:seed-five-pct]", 0.0, f.getPct(5));
        requireDouble("[oracle:seed-foo-pct]", 0.0, f.getPct("foo"));
        requireDouble("[oracle:seed-one-cum-pct]", 0.25, f.getCumPct(1));
        requireDouble("[oracle:seed-two-cum-pct]", 0.50, f.getCumPct(Long.valueOf(2)));
        requireDouble("[oracle:seed-integer-arg-cumpct]", 0.50, f.getCumPct(Integer.valueOf(2)));
        requireDouble("[oracle:seed-three-cum-pct]", 1.0, f.getCumPct(threeL));
        requireDouble("[oracle:seed-five-cum-pct]", 1.0, f.getCumPct(5));
        requireDouble("[oracle:seed-zero-cum-pct]", 0.0, f.getCumPct(0));
        requireDouble("[oracle:seed-foo-cum-pct]", 0.0, f.getCumPct("foo"));
    }

    private static void oracleLowerInsertionDecreasesExactObjectPct(FuzzedDataProvider data) {
        try {
            int base = data.consumeInt(-1000, 1000);
            int lowerCount = data.consumeInt(1, 20);
            int middleCount = data.consumeInt(1, 20);
            int upperCount = data.consumeInt(1, 20);

            int low = base;
            int mid = base + 1;
            int high = base + 2;

            Frequency f = new Frequency();

            addRepeated(f, low, lowerCount);
            addRepeated(f, mid, middleCount);
            addRepeated(f, high, upperCount);

            Object midAsObject = Integer.valueOf(mid);

            long sumBefore = f.getSumFreq();
            long countBefore = f.getCount(mid);
            double pctBefore = f.getPct(midAsObject);

            double expectedBefore = ((double) countBefore) / ((double) sumBefore);
            requireDouble("[oracle:constructed-before-exact]", expectedBefore, pctBefore);

            f.addValue(low);

            long sumAfter = f.getSumFreq();
            long countAfter = f.getCount(mid);
            double pctAfter = f.getPct(midAsObject);

            double expectedAfter = ((double) countAfter) / ((double) sumAfter);
            requireDouble("[oracle:lower-insert-exact]", expectedAfter, pctAfter);

            if (!(pctAfter < pctBefore)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:lower-insert-monotonic] metamorphic violation: adding one non-target value strictly below the queried bucket must decrease getPct(Object) because getCount(mid) is unchanged while getSumFreq increases; mid="
                        + mid + " low=" + low + " high=" + high
                        + " lowerCount=" + lowerCount + " middleCount=" + middleCount + " upperCount=" + upperCount
                        + " before=" + pctBefore + " after=" + pctAfter
                        + " countBefore=" + countBefore + " countAfter=" + countAfter
                        + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter);
            }
        } catch (RuntimeException e) {
            if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) {
                throw e;
            }
            return;
        }
    }

    private static void addRepeated(Frequency f, int value, int count) {
        for (int i = 0; i < count; i++) {
            f.addValue(value);
        }
    }

    private static void requireDouble(String oracle, double expected, double actual) {
        if (Double.doubleToLongBits(expected) == Double.doubleToLongBits(actual)) {
            return;
        }
        double diff = Math.abs(expected - actual);
        if (diff <= 1.0E-14) {
            return;
        }
        throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
            "[oracle:unnamed-check] " + oracle + " semantic mismatch: expected=" + expected + " actual=" + actual);
    }
}