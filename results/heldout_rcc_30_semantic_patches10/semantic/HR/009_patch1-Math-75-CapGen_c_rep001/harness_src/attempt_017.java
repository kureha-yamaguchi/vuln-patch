package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Iterator;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency seed = exactTestFixture();

        checkExact("lifted-one-pct-bundle2", 0.25d, seed.getPct(1));
        checkExact("lifted-two-pct-bundle2", 0.25d, seed.getPct(Long.valueOf(2)));
        checkExact("lifted-three-pct-bundle2", 0.5d, seed.getPct(Long.valueOf(3)));
        checkExact("lifted-three-object-pct-bundle2", 0.5d, seed.getPct((Object) (Integer.valueOf(3))));
        checkExact("lifted-five-pct-bundle2", 0.0d, seed.getPct(5));
        checkExact("lifted-foo-pct-bundle2", 0.0d, seed.getPct("foo"));
        checkExact("lifted-one-cumpct-bundle2", 0.25d, seed.getCumPct(1));
        checkExact("lifted-two-cumpct-bundle2", 0.50d, seed.getCumPct(Long.valueOf(2)));
        checkExact("lifted-integer-argument-cumpct-bundle2", 0.50d, seed.getCumPct(Integer.valueOf(2)));
        checkExact("lifted-three-cumpct-bundle2", 1.0d, seed.getCumPct(Long.valueOf(3)));
        checkExact("lifted-five-cumpct-bundle2", 1.0d, seed.getCumPct(5));
        checkExact("lifted-zero-cumpct-bundle2", 0.0d, seed.getCumPct(0));
        checkExact("lifted-foo-cumpct-bundle2", 0.0d, seed.getCumPct("foo"));

        /* Contract basis: getPct "Returns the percentage of values that are equal to v".
           Therefore, over all distinct stored values, the weighted average reconstructed from
           value * getPct(value) must equal the same weighted average reconstructed independently
           from value * getCount(value) / getSumFreq(). This uses only real API output and still
           fails if a band-aid patch masks one specific seed assertion while leaving point-mass
           percentages wrong elsewhere. */
        try {
            double meanViaPct = 0.0d;
            double meanViaCounts = 0.0d;
            long sum = seed.getSumFreq();
            Iterator it = seed.valuesIterator();
            while (it.hasNext()) {
                Object key = it.next();
                if (!(key instanceof Number)) {
                    return;
                }
                long v = ((Number) key).longValue();
                meanViaPct += v * seed.getPct((Object) key);
                meanViaCounts += v * ((double) seed.getCount((Comparable<?>) key) / (double) sum);
            }
            if (Math.abs(meanViaPct - meanViaCounts) > 1e-12) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:weighted-mean-via-object-pct] consistency violation: meanViaPct=" + meanViaPct
                        + " meanViaCounts=" + meanViaCounts + " sum=" + sum);
            }
        } catch (Throwable t) {
            if (t instanceof FuzzerSecurityIssueLow) {
                throw (FuzzerSecurityIssueLow) t;
            }
            return;
        }

        int base = data.consumeInt(-1000000, 1000000);
        Frequency shifted = shiftedFixture(base);

        /* Oracle from construction: this fixture always has counts 2,2,4 on consecutive values
           base, base+1, base+2, so the exact percentage at the top bucket is 4/8 = 0.5.
           The buggy Object overload returns the cumulative percentage 1.0 there. This flips the
           patched condition away from the literal seed value 3 and checks just past the same
           boundary pattern on a fuzz-chosen distribution. */
        try {
            double topPctObject = shifted.getPct((Object) Integer.valueOf(base + 2));
            if (Math.abs(topPctObject - 0.5d) > TOLERANCE) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:shifted-top-bucket-object-pct] semantic mismatch: base=" + base
                        + " expected=0.5 actual=" + topPctObject);
            }
        } catch (Throwable t) {
            if (t instanceof FuzzerSecurityIssueLow) {
                throw (FuzzerSecurityIssueLow) t;
            }
            return;
        }

        /* Getter post-condition: getPct(Object) is documented as a query returning a proportion.
           Calling it must not mutate observable state. This specifically guards against a patch
           that deletes or reroutes logic in a way that changes caches/bookkeeping instead of
           computing the requested percentage. */
        try {
            long sumBefore = shifted.getSumFreq();
            int hashBefore = shifted.hashCode();
            String textBefore = shifted.toString();
            shifted.getPct((Object) Integer.valueOf(base + 1));
            long sumAfter = shifted.getSumFreq();
            int hashAfter = shifted.hashCode();
            String textAfter = shifted.toString();
            if (sumBefore != sumAfter || hashBefore != hashAfter || !textBefore.equals(textAfter)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:shifted-object-pct-readonly-axis2] state mismatch: sumBefore=" + sumBefore
                        + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                        + " textBefore=" + escape(textBefore) + " textAfter=" + escape(textAfter));
            }
        } catch (Throwable t) {
            if (t instanceof FuzzerSecurityIssueLow) {
                throw (FuzzerSecurityIssueLow) t;
            }
        }
    }

    private static Frequency exactTestFixture() {
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
        return f;
    }

    private static Frequency shiftedFixture(int base) {
        Frequency f = new Frequency();
        f.addValue((long) base);
        f.addValue(base);
        f.addValue((long) (base + 1));
        f.addValue(base + 1);
        f.addValue((long) (base + 2));
        f.addValue((long) (base + 2));
        f.addValue(base + 2);
        f.addValue(base + 2);
        return f;
    }

    private static void checkExact(String oracleId, double expected, double actual) {
        if (Double.isNaN(actual) || Math.abs(expected - actual) > TOLERANCE) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: expected=" + expected + " actual=" + actual);
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}