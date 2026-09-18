package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        Frequency seed = new Frequency();
        seed.addValue(1L);
        seed.addValue(2L);
        seed.addValue(1);
        seed.addValue(2);
        seed.addValue(3L);
        seed.addValue(3L);
        seed.addValue(3);
        seed.addValue(Integer.valueOf(3));

        seed.getPct((Object) Integer.valueOf(3));

        double seedPrefix = 0.0;
        java.util.Iterator<Comparable<?>> seedIt = seed.valuesIterator();
        while (seedIt.hasNext()) {
            Comparable<?> key = seedIt.next();
            if (!(key instanceof Long)) {
                return;
            }
            long k = ((Long) key).longValue();
            double bucketPct;
            double cumPct;
            try {
                bucketPct = seed.getPct((Object) Integer.valueOf((int) k));
                cumPct = seed.getCumPct(k);
            } catch (Throwable t) {
                return;
            }
            seedPrefix += bucketPct;
            // Documented guarantee: getPct(v) is the proportion equal to v and getCumPct(v) is the
            // proportion less than or equal to v, so along the sorted valuesIterator the cumulative
            // percentage at each key must equal the prefix sum of exact bucket percentages.
            // A band-aid patch that merely masks one top-level symptom while leaving getPct(Object)
            // wrong still breaks this per-prefix consistency.
            if (Math.abs(seedPrefix - cumPct) > tolerance) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:prefix-object-cumpct-seed] consistency violation: key=" + k
                        + " prefixPct=" + seedPrefix
                        + " cumPct=" + cumPct
                        + " sumFreq=" + seed.getSumFreq());
            }
        }

        int a = data.consumeInt(-1000, 1000);
        int gap1 = data.consumeInt(1, 20);
        int gap2 = data.consumeInt(1, 20);
        int b = a + gap1;
        int c = b + gap2;

        int countA = data.consumeInt(1, 5);
        int countB = data.consumeInt(1, 5);
        int countC = data.consumeInt(1, 5);

        Frequency fuzzed = new Frequency();

        for (int i = 0; i < countA; i++) {
            if (data.consumeBoolean()) {
                fuzzed.addValue((long) a);
            } else if (data.consumeBoolean()) {
                fuzzed.addValue(a);
            } else {
                fuzzed.addValue(Integer.valueOf(a));
            }
        }
        for (int i = 0; i < countB; i++) {
            if (data.consumeBoolean()) {
                fuzzed.addValue((long) b);
            } else if (data.consumeBoolean()) {
                fuzzed.addValue(b);
            } else {
                fuzzed.addValue(Integer.valueOf(b));
            }
        }
        for (int i = 0; i < countC; i++) {
            if (data.consumeBoolean()) {
                fuzzed.addValue((long) c);
            } else if (data.consumeBoolean()) {
                fuzzed.addValue(c);
            } else {
                fuzzed.addValue(Integer.valueOf(c));
            }
        }

        fuzzed.getPct((Object) Integer.valueOf(b));

        double prefix = 0.0;
        java.util.Iterator<Comparable<?>> it = fuzzed.valuesIterator();
        while (it.hasNext()) {
            Comparable<?> key = it.next();
            if (!(key instanceof Long)) {
                return;
            }
            long k = ((Long) key).longValue();
            double bucketPct;
            double cumPct;
            try {
                bucketPct = fuzzed.getPct((Object) Integer.valueOf((int) k));
                cumPct = fuzzed.getCumPct(k);
            } catch (Throwable t) {
                return;
            }
            prefix += bucketPct;
            // Same documented guarantee as above, now generalized to fuzzed but valid-by-construction
            // integral inputs. This cross-check uses two independent public observations:
            // valuesIterator order + repeated getPct(Object) prefix sum versus getCumPct(long).
            if (Math.abs(prefix - cumPct) > tolerance) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:prefix-object-cumpct-fuzzed] consistency violation: key=" + k
                        + " prefixPct=" + prefix
                        + " cumPct=" + cumPct
                        + " a=" + a
                        + " b=" + b
                        + " c=" + c
                        + " countA=" + countA
                        + " countB=" + countB
                        + " countC=" + countC
                        + " sumFreq=" + fuzzed.getSumFreq());
            }
        }
    }
}