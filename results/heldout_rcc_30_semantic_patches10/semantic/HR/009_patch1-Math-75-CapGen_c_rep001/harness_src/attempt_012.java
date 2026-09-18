package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.ArrayList;
import java.util.List;

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

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        requireDouble("seed-one-pct", 0.25, f.getPct(1));
        requireDouble("seed-two-pct", 0.25, f.getPct(Long.valueOf(2)));
        requireDouble("seed-three-pct", 0.5, f.getPct(Long.valueOf(3)));
        requireDouble("seed-three-object-pct", 0.5, f.getPct((Object) (Integer.valueOf(3))));
        requireDouble("seed-five-pct", 0.0, f.getPct(5));
        requireDouble("seed-foo-pct", 0.0, f.getPct("foo"));
        requireDouble("seed-one-cum-pct", 0.25, f.getCumPct(1));
        requireDouble("seed-two-cum-pct", 0.50, f.getCumPct(Long.valueOf(2)));
        requireDouble("seed-integer-argument-cum-pct", 0.50, f.getCumPct(Integer.valueOf(2)));
        requireDouble("seed-three-cum-pct", 1.0, f.getCumPct(threeL));
        requireDouble("seed-five-cum-pct", 1.0, f.getCumPct(5));
        requireDouble("seed-zero-cum-pct", 0.0, f.getCumPct(0));
        requireDouble("seed-foo-cum-pct", 0.0, f.getCumPct("foo"));

        try {
            Frequency g = new Frequency();
            Frequency twin = new Frequency();
            List<Integer> added = new ArrayList<Integer>();

            int additions = data.consumeInt(1, 8);
            for (int i = 0; i < additions; i++) {
                int v = data.consumeInt(-1000, 1000);
                added.add(Integer.valueOf(v));
                addIntegralBoth(g, twin, v, data.consumeInt(0, 2));
            }

            if (added.isEmpty()) {
                return;
            }

            int probe = added.get(data.consumeInt(0, added.size() - 1)).intValue();
            long sumBefore = g.getSumFreq();
            int hashBefore = g.hashCode();
            boolean equalsBefore = g.equals(twin);

            g.getPct((Object) Integer.valueOf(probe));

            long sumAfter = g.getSumFreq();
            int hashAfter = g.hashCode();
            boolean equalsAfter = g.equals(twin);
            int twinHashAfter = twin.hashCode();

            if (sumBefore != sumAfter) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:getpct-object-readonly-sum-hash-equals] post-condition violation: getPct(Object) is documented as a read-only query over the frequency distribution, so calling it must not change getSumFreq; sumBefore="
                        + sumBefore + " sumAfter=" + sumAfter + " probe=" + probe);
            }

            if (!equalsBefore) {
                return;
            }

            if (!equalsAfter) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:getpct-object-readonly-sum-hash-equals] post-condition violation: getPct(Object) is documented as a read-only query, so two identically constructed Frequency instances must remain equal after querying one of them; probe="
                        + probe + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter + " twinHashAfter=" + twinHashAfter);
            }

            if (hashBefore != hashAfter || hashAfter != twinHashAfter) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:getpct-object-readonly-sum-hash-equals] post-condition violation: getPct(Object) is documented as a read-only query, so hashCode must remain stable and continue to match an equal twin; probe="
                        + probe + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter + " twinHashAfter=" + twinHashAfter);
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
            return;
        }
    }

    private static void addIntegralBoth(Frequency a, Frequency b, int v, int mode) {
        switch (mode) {
            case 0:
                a.addValue(v);
                b.addValue(v);
                break;
            case 1:
                a.addValue((long) v);
                b.addValue((long) v);
                break;
            default:
                a.addValue(Long.valueOf(v));
                b.addValue(Long.valueOf(v));
                break;
        }
    }

    private static void requireDouble(String oracleId, double expected, double actual) {
        if (Double.isNaN(expected)) {
            if (!Double.isNaN(actual)) {
                throw issue(oracleId, expected, actual);
            }
            return;
        }
        if (Math.abs(expected - actual) > TOLERANCE) {
            throw issue(oracleId, expected, actual);
        }
    }

    private static FuzzerSecurityIssueLow issue(String oracleId, double expected, double actual) {
        return new FuzzerSecurityIssueLow(
            "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
    }
}