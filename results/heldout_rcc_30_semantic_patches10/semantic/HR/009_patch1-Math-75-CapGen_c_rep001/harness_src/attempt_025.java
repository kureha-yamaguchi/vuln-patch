package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Iterator;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        checkStringMiddleBucketConsistency(data);
        checkLiftedSeedAssertions();
    }

    private static void checkStringMiddleBucketConsistency(FuzzedDataProvider data) {
        Frequency f;
        String low;
        String mid;
        String high;
        try {
            f = new Frequency();

            int base = data.consumeInt(0, 23);
            low = String.valueOf((char) ('a' + base));
            mid = String.valueOf((char) ('a' + base + 1));
            high = String.valueOf((char) ('a' + base + 2));

            int lowCount = data.consumeInt(1, 4);
            int midCount = data.consumeInt(1, 4);
            int highCount = data.consumeInt(1, 4);

            int totalAdds = lowCount + midCount + highCount;
            int lowRemaining = lowCount;
            int midRemaining = midCount;
            int highRemaining = highCount;

            for (int i = 0; i < totalAdds; i++) {
                int pick = data.consumeInt(0, 2);
                if ((pick == 0 && lowRemaining > 0) || (midRemaining == 0 && highRemaining == 0)) {
                    f.addValue(low);
                    lowRemaining--;
                } else if ((pick == 1 && midRemaining > 0) || (lowRemaining == 0 && highRemaining == 0)) {
                    f.addValue(mid);
                    midRemaining--;
                } else if (highRemaining > 0) {
                    f.addValue(high);
                    highRemaining--;
                } else if (midRemaining > 0) {
                    f.addValue(mid);
                    midRemaining--;
                } else {
                    f.addValue(low);
                    lowRemaining--;
                }
            }
        } catch (Exception e) {
            return;
        }

        double actualPct;
        long sumFreq;
        long countMid;
        long cumFreqMid;
        long iteratedCumMid;
        try {
            actualPct = f.getPct((Object) mid);
            sumFreq = f.getSumFreq();
            countMid = f.getCount(mid);
            cumFreqMid = f.getCumFreq(mid);

            iteratedCumMid = 0L;
            Iterator<?> it = f.valuesIterator();
            while (it.hasNext()) {
                Object v = it.next();
                if (!(v instanceof Comparable)) {
                    return;
                }
                @SuppressWarnings("unchecked")
                Comparable<Object> comparable = (Comparable<Object>) v;
                if (comparable.compareTo(mid) <= 0) {
                    iteratedCumMid += f.getCount(v);
                }
            }
        } catch (Exception e) {
            return;
        }

        double expectedPct = (double) countMid / (double) sumFreq;
        if (Math.abs(expectedPct - actualPct) > TOLERANCE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:string-middle-pct-count-over-sum] semantic mismatch: value=" + mid
                    + " expectedPct=" + expectedPct
                    + " actualPct=" + actualPct
                    + " count=" + countMid
                    + " sumFreq=" + sumFreq
            );
        }

        /* Contract justification: getCumFreq reports the cumulative frequency, and valuesIterator exposes
           the object's distinct values. Recomputing the cumulative count by summing getCount(v) over all
           iterated values <= mid must match getCumFreq(mid) for every correct implementation. This is an
           independent helper cross-check: a band-aid patch that only special-cases getPct(Object) can leave
           the helper path inconsistent. */
        if (iteratedCumMid != cumFreqMid) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:string-cumfreq-recomputed-from-iterator] consistency violation: value=" + mid
                    + " reportedCumFreq=" + cumFreqMid
                    + " recomputedCumFreq=" + iteratedCumMid
                    + " sumFreq=" + sumFreq
            );
        }
    }

    private static void checkLiftedSeedAssertions() {
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

        demandEq("seed-one-pct-v2", 0.25, f.getPct(1));
        demandEq("seed-two-pct-v2", 0.25, f.getPct(Long.valueOf(2)));
        demandEq("seed-three-pct-v2", 0.5, f.getPct(threeL));
        demandEq("seed-three-object-pct-v2", 0.5, f.getPct((Object) (Integer.valueOf(3))));
        demandEq("seed-five-pct-v2", 0.0, f.getPct(5));
        demandEq("seed-foo-pct-v2", 0.0, f.getPct("foo"));
        demandEq("seed-one-cumpct-v2", 0.25, f.getCumPct(1));
        demandEq("seed-two-cumpct-v2", 0.50, f.getCumPct(Long.valueOf(2)));
        demandEq("seed-integer-argument-cumpct-v2", 0.50, f.getCumPct(Integer.valueOf(2)));
        demandEq("seed-three-cumpct-v2", 1.0, f.getCumPct(threeL));
        demandEq("seed-five-cumpct-v2", 1.0, f.getCumPct(5));
        demandEq("seed-zero-cumpct-v2", 0.0, f.getCumPct(0));
        demandEq("seed-foo-cumpct-v2", 0.0, f.getCumPct("foo"));
    }

    private static void demandEq(String oracleId, double expected, double actual) {
        if (Math.abs(expected - actual) > TOLERANCE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual
            );
        }
    }
}