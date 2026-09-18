package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Collections;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Frequency seed = new Frequency();
        seed.addValue(1L);
        seed.addValue(2L);
        seed.addValue(Integer.valueOf(1));
        seed.addValue(Integer.valueOf(2));
        seed.addValue(3L);
        seed.addValue(3L);
        seed.addValue(3);
        seed.addValue(Integer.valueOf(3));

        double actualSeedObjectPct;
        try {
            actualSeedObjectPct = seed.getPct((Object) Integer.valueOf(3));
        } catch (RuntimeException e) {
            return;
        }

        if (!sameDouble(actualSeedObjectPct, 0.5d, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:seed-object-three-direct] semantic mismatch: f.getPct((Object)(Integer.valueOf(3))) expected=0.5 actual=" + actualSeedObjectPct);
        }

        int a = data.consumeInt(-1000, 1000);
        int b = data.consumeInt(-1000, 1000);
        int c = data.consumeInt(-1000, 1000);

        int lo = a;
        int mid = b;
        int hi = c;
        if (lo > mid) {
            int t = lo;
            lo = mid;
            mid = t;
        }
        if (mid > hi) {
            int t = mid;
            mid = hi;
            hi = t;
        }
        if (lo > mid) {
            int t = lo;
            lo = mid;
            mid = t;
        }

        if (lo == mid) {
            mid = lo + 1;
        }
        if (mid == hi) {
            hi = mid + 1;
        }
        if (lo == mid || mid == hi) {
            return;
        }

        int lowCount = data.consumeInt(1, 5);
        int midCount = data.consumeInt(1, 5);
        int highCount = lowCount + data.consumeInt(1, 5);

        Frequency natural = new Frequency();
        Frequency reverse = new Frequency(Collections.reverseOrder());

        addCopies(natural, lo, lowCount);
        addCopies(natural, mid, midCount);
        addCopies(natural, hi, highCount);

        addCopies(reverse, lo, lowCount);
        addCopies(reverse, mid, midCount);
        addCopies(reverse, hi, highCount);

        double naturalObjectPct;
        double reverseObjectPct;
        try {
            naturalObjectPct = natural.getPct((Object) Integer.valueOf(mid));
            reverseObjectPct = reverse.getPct((Object) Integer.valueOf(mid));
        } catch (RuntimeException e) {
            return;
        }

        // Contract: getPct returns "the percentage of values that are equal to v".
        // Equality count for a value is independent of ordering, so two Frequency
        // objects built from the same multiset but different comparators must report
        // the same getPct((Object) value). A band-aid that keeps routing Object overloads
        // through cumulative logic would make the result depend on comparator order.
        if (!sameDouble(naturalObjectPct, reverseObjectPct, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:comparator-invariant-object-pct] metamorphic violation: same multiset different comparator changed getPct((Object)mid) mid=" + mid + " natural=" + naturalObjectPct + " reverse=" + reverseObjectPct + " counts=[" + lowCount + "," + midCount + "," + highCount + "] values=[" + lo + "," + mid + "," + hi + "]");
        }

        double expectedMidPct = ((double) midCount) / ((double) lowCount + (double) midCount + (double) highCount);
        if (!sameDouble(naturalObjectPct, expectedMidPct, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:constructed-middle-object-pct] semantic mismatch: expected constructed middle pct=" + expectedMidPct + " actual=" + naturalObjectPct + " counts=[" + lowCount + "," + midCount + "," + highCount + "] values=[" + lo + "," + mid + "," + hi + "]");
        }
    }

    private static void addCopies(Frequency f, int value, int count) {
        for (int i = 0; i < count; i++) {
            f.addValue(value);
        }
    }

    private static boolean sameDouble(double a, double b, double tol) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return Double.isNaN(a) && Double.isNaN(b);
        }
        return Math.abs(a - b) <= tol;
    }
}