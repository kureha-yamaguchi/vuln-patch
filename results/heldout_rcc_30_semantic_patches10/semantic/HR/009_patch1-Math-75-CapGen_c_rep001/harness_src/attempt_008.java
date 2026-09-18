package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Iterator;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Frequency f = new Frequency();

        long oneL = 1;
        long twoL = 2;
        long threeL = 3;
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

        double actualOnePct = f.getPct(1);
        if (Math.abs(actualOnePct - 0.25d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-one-pct-inline] semantic mismatch: expected=0.25 actual=" + actualOnePct);
        }

        double actualTwoPct = f.getPct(Long.valueOf(2));
        if (Math.abs(actualTwoPct - 0.25d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-two-pct-inline] semantic mismatch: expected=0.25 actual=" + actualTwoPct);
        }

        double actualThreePct = f.getPct(threeL);
        if (Math.abs(actualThreePct - 0.5d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-three-pct-inline] semantic mismatch: expected=0.5 actual=" + actualThreePct);
        }

        double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        if (Math.abs(actualThreeObjectPct - 0.5d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-three-object-pct-inline] semantic mismatch: expected=0.5 actual=" + actualThreeObjectPct);
        }

        double actualFivePct = f.getPct(5);
        if (Math.abs(actualFivePct - 0.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-five-pct-inline] semantic mismatch: expected=0.0 actual=" + actualFivePct);
        }

        double actualFooPct = f.getPct("foo");
        if (Math.abs(actualFooPct - 0.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-foo-pct-inline] semantic mismatch: expected=0.0 actual=" + actualFooPct);
        }

        double actualOneCumPct = f.getCumPct(1);
        if (Math.abs(actualOneCumPct - 0.25d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-one-cumpct-inline] semantic mismatch: expected=0.25 actual=" + actualOneCumPct);
        }

        double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actualTwoCumPct - 0.50d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-two-cumpct-inline] semantic mismatch: expected=0.50 actual=" + actualTwoCumPct);
        }

        double actualIntegerArgCumPct = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actualIntegerArgCumPct - 0.50d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-integer-argument-inline] semantic mismatch: expected=0.50 actual=" + actualIntegerArgCumPct);
        }

        double actualThreeCumPct = f.getCumPct(threeL);
        if (Math.abs(actualThreeCumPct - 1.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-three-cumpct-inline] semantic mismatch: expected=1.0 actual=" + actualThreeCumPct);
        }

        double actualFiveCumPct = f.getCumPct(5);
        if (Math.abs(actualFiveCumPct - 1.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-five-cumpct-inline] semantic mismatch: expected=1.0 actual=" + actualFiveCumPct);
        }

        double actualZeroCumPct = f.getCumPct(0);
        if (Math.abs(actualZeroCumPct - 0.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-zero-cumpct-inline] semantic mismatch: expected=0.0 actual=" + actualZeroCumPct);
        }

        double actualFooCumPct = f.getCumPct("foo");
        if (Math.abs(actualFooCumPct - 0.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-foo-cumpct-inline] semantic mismatch: expected=0.0 actual=" + actualFooCumPct);
        }

        Frequency partition = new Frequency();
        int base = data.consumeInt(-1000, 1000);
        int gap = data.consumeInt(1, 20);
        int lowCount = data.consumeInt(1, 5);
        int highCount = data.consumeInt(1, 5);
        boolean putHighFirst = data.consumeBoolean();

        int low = base;
        int high = base + gap;

        if (putHighFirst) {
            for (int i = 0; i < highCount; i++) {
                partition.addValue(high);
            }
            for (int i = 0; i < lowCount; i++) {
                partition.addValue(low);
            }
        } else {
            for (int i = 0; i < lowCount; i++) {
                partition.addValue(low);
            }
            for (int i = 0; i < highCount; i++) {
                partition.addValue(high);
            }
        }

        if (data.consumeBoolean()) {
            int extra = high + data.consumeInt(1, 20);
            int extraCount = data.consumeInt(1, 5);
            for (int i = 0; i < extraCount; i++) {
                partition.addValue(extra);
            }
        }

        try {
            long sumFreq = partition.getSumFreq();
            if (sumFreq > 0) {
                double sumOfExactPctsQueriedViaObjectInteger = 0.0d;
                Iterator<Comparable<?>> it = partition.valuesIterator();
                while (it.hasNext()) {
                    Comparable<?> key = it.next();
                    if (!(key instanceof Long)) {
                        return;
                    }
                    long longKey = ((Long) key).longValue();
                    if (longKey < Integer.MIN_VALUE || longKey > Integer.MAX_VALUE) {
                        return;
                    }
                    sumOfExactPctsQueriedViaObjectInteger += partition.getPct((Object) Integer.valueOf((int) longKey));
                }

                double expectedPartitionSum = 1.0d;
                if (Math.abs(sumOfExactPctsQueriedViaObjectInteger - expectedPartitionSum) > 1.0E-12d) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:object-pct-partition-sum] consistency violation: " +
                        "sum of exact percentages over all distinct values must equal 1.0 because getPct is documented as the proportion of values equal to v, and the distinct-value buckets returned by valuesIterator partition the population; " +
                        "sum=" + sumOfExactPctsQueriedViaObjectInteger +
                        " expected=1.0 sumFreq=" + sumFreq +
                        " low=" + low + " high=" + high +
                        " lowCount=" + lowCount + " highCount=" + highCount +
                        " toString=" + partition.toString().replace("\n", "\\n").replace("\t", "\\t")
                    );
                }
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
            return;
        }
    }
}