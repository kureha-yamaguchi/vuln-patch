package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Iterator;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        if (data.remainingBytes() < 8) {
            return;
        }

        Frequency f = new Frequency();

        int center = data.consumeInt(-1000000, 1000000);
        int lower = center - 1;
        int upper = center + 1;
        int lowerCount = data.consumeInt(1, 8);
        int upperCount = data.consumeInt(1, 8);

        try {
            for (int i = 0; i < lowerCount; i++) {
                if (data.consumeBoolean()) {
                    f.addValue(lower);
                } else {
                    f.addValue((long) lower);
                }
            }
            for (int i = 0; i < upperCount; i++) {
                if (data.consumeBoolean()) {
                    f.addValue(upper);
                } else {
                    f.addValue((long) upper);
                }
            }
        } catch (RuntimeException e) {
            return;
        }

        Integer absentMiddle = Integer.valueOf(center);

        double pctObject;
        long countAbsent;
        long sumFreq;
        double cumPctObject;
        long cumFreqObject;
        long iterCumFreq;
        try {
            pctObject = f.getPct((Object) absentMiddle);
            countAbsent = f.getCount(absentMiddle);
            sumFreq = f.getSumFreq();
            cumPctObject = f.getCumPct((Object) absentMiddle);
            cumFreqObject = f.getCumFreq((Object) absentMiddle);
            iterCumFreq = recomputeCumFreqFromIterator(f, center);
        } catch (RuntimeException e) {
            return;
        }

        if (countAbsent != 0L) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:constructed-gap-count-zero] semantic mismatch: constructed absent middle unexpectedly has count=" + countAbsent
                    + " center=" + center + " lower=" + lower + " upper=" + upper
                    + " lowerCount=" + lowerCount + " upperCount=" + upperCount);
        }

        if (sumFreq != (long) lowerCount + (long) upperCount) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:constructed-gap-sumfreq] consistency violation: reported sumFreq=" + sumFreq
                    + " expected=" + ((long) lowerCount + (long) upperCount)
                    + " center=" + center + " lowerCount=" + lowerCount + " upperCount=" + upperCount);
        }

        if (iterCumFreq != cumFreqObject) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:iterator-recomputed-cumfreq-gap] consistency violation: getCumFreq(Object)=" + cumFreqObject
                    + " recomputedFromIterator=" + iterCumFreq
                    + " center=" + center + " lower=" + lower + " upper=" + upper
                    + " lowerCount=" + lowerCount + " upperCount=" + upperCount);
        }

        // Contract-based exact oracle: getPct returns "the percentage of values that are equal to v".
        // Here v=center is absent by construction because we add only center-1 and center+1, so the correct result is exactly 0.
        if (Math.abs(pctObject - 0.0d) > TOLERANCE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:interior-gap-object-pct-zero] semantic mismatch: getPct((Object) absentMiddle)=" + pctObject
                    + " expected=0.0 countAbsent=" + countAbsent + " sumFreq=" + sumFreq
                    + " cumPct=" + cumPctObject + " cumFreq=" + cumFreqObject
                    + " center=" + center + " lower=" + lower + " upper=" + upper
                    + " lowerCount=" + lowerCount + " upperCount=" + upperCount);
        }

        // Mandatory metamorphic/post-condition:
        // For an absent interior value in a non-empty distribution with lower mass present, getPct(v) must differ from getCumPct(v):
        // pct(v)=0 because no values equal v, while cumPct(v)>0 because all lower-bucket values are <= v.
        // A band-aid that routes getPct(Object) to cumulative logic violates this observable relation without throwing.
        double expectedCumPct = ((double) lowerCount) / ((double) lowerCount + (double) upperCount);
        if (cumPctObject <= 0.0d || cumPctObject >= 1.0d || Math.abs(cumPctObject - expectedCumPct) > 1e-12) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:interior-gap-cumpct-shape] consistency violation: getCumPct((Object) absentMiddle)=" + cumPctObject
                    + " expected=" + expectedCumPct
                    + " center=" + center + " lowerCount=" + lowerCount + " upperCount=" + upperCount);
        }
        if (!(Math.abs(pctObject - cumPctObject) > TOLERANCE)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:interior-gap-pct-vs-cumpct] metamorphic violation: absent interior value must satisfy getPct(Object)!=getCumPct(Object)"
                    + " pct=" + pctObject + " cumPct=" + cumPctObject
                    + " center=" + center + " lower=" + lower + " upper=" + upper
                    + " lowerCount=" + lowerCount + " upperCount=" + upperCount);
        }

        // Reach the exact patched path on the historic fixture as well, but do not reuse the already-covered direct alarm signature.
        tryHistoricFixtureWithoutOracleReuse(data);
    }

    private static long recomputeCumFreqFromIterator(Frequency f, int query) {
        long total = 0L;
        Iterator<?> it = f.valuesIterator();
        while (it.hasNext()) {
            Object next = it.next();
            if (!(next instanceof Comparable<?>)) {
                continue;
            }
            Comparable<?> key = (Comparable<?>) next;
            if (compareIntegralComparableToInt(key, query) <= 0) {
                total += f.getCount(key);
            }
        }
        return total;
    }

    private static int compareIntegralComparableToInt(Comparable<?> key, int query) {
        if (key instanceof Long) {
            long v = ((Long) key).longValue();
            return v < query ? -1 : (v == query ? 0 : 1);
        }
        if (key instanceof Integer) {
            int v = ((Integer) key).intValue();
            return v < query ? -1 : (v == query ? 0 : 1);
        }
        return 0;
    }

    private static void tryHistoricFixtureWithoutOracleReuse(FuzzedDataProvider data) {
        Frequency f = new Frequency();
        try {
            long oneL = 1L;
            long twoL = 2L;
            long threeL = 3L;
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

            f.getPct(1);
            f.getPct(Long.valueOf(2));
            f.getPct(threeL);
            f.getPct((Object) Integer.valueOf(3));
            f.getPct(5);
            f.getPct("foo");
            f.getCumPct(1);
            f.getCumPct(Long.valueOf(2));
            f.getCumPct(Integer.valueOf(2));
            f.getCumPct(threeL);
            f.getCumPct(5);
            f.getCumPct(0);
            f.getCumPct("foo");

            int probe = data.consumeInt(-10, 10);
            f.getPct((Object) Integer.valueOf(probe));
        } catch (RuntimeException e) {
            return;
        }
    }
}