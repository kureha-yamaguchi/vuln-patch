package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Iterator;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency f = new Frequency();

        char lower = pickPrintableChar(data.consumeInt());
        char higher = pickDistinctHigherPrintableChar(lower, data.consumeInt());

        int lowerCount = 1 + Math.abs(data.consumeInt() % 8);
        int higherCount = 1 + Math.abs(data.consumeInt() % 8);

        try {
            for (int i = 0; i < lowerCount; i++) {
                f.addValue(lower);
            }
            for (int i = 0; i < higherCount; i++) {
                f.addValue(higher);
            }
        } catch (RuntimeException e) {
            return;
        }

        try {
            Object higherAsObject = Character.valueOf(higher);

            double objectPct = f.getPct(higherAsObject);
            double charPct = f.getPct(higher);
            long count = f.getCount(higher);
            long sum = f.getSumFreq();
            double recomputedPct = (double) count / (double) sum;

            /* The overload docs match: getPct(Object) and getPct(char)/getPct(Comparable)
               all return "the percentage of values that are equal to v".
               For any correct implementation, passing the same Character through Object
               or char must agree. A band-aid that special-cases only the Integer seed or
               silently keeps delegating to cumulative percentage will violate this. */
            if (!sameDouble(objectPct, charPct)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:char-object-overload-agreement] semantic mismatch: getPct((Object)Character) != getPct(char)"
                        + " lower=" + (int) lower
                        + " higher=" + (int) higher
                        + " lowerCount=" + lowerCount
                        + " higherCount=" + higherCount
                        + " objectPct=" + objectPct
                        + " charPct=" + charPct);
            }

            /* Independent recomputation from exposed state: getPct(v) is documented as
               getCount(v) / getSumFreq(). This cross-check is outside the known seed-only
               symptom and still fails if getPct(Object) stays wrong while other APIs are right. */
            if (!sameDouble(objectPct, recomputedPct)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:char-object-count-over-sum] consistency violation: getPct((Object)Character) != getCount(char)/getSumFreq()"
                        + " lower=" + (int) lower
                        + " higher=" + (int) higher
                        + " lowerCount=" + lowerCount
                        + " higherCount=" + higherCount
                        + " objectPct=" + objectPct
                        + " recomputedPct=" + recomputedPct
                        + " count=" + count
                        + " sum=" + sum);
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
            return;
        }

        try {
            /* Recompute the summary from the object's own output: valuesIterator() yields the
               distinct added values and summing getCount(value) over them must equal getSumFreq(). */
            long iteratedSum = 0L;
            Iterator<Comparable<?>> it = f.valuesIterator();
            while (it.hasNext()) {
                Comparable<?> v = it.next();
                iteratedSum += f.getCount(v);
            }
            long reportedSum = f.getSumFreq();
            if (iteratedSum != reportedSum) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:sumfreq-from-iterator] consistency violation: recomputed sum from valuesIterator/getCount differs from getSumFreq()"
                        + " lower=" + (int) lower
                        + " higher=" + (int) higher
                        + " lowerCount=" + lowerCount
                        + " higherCount=" + higherCount
                        + " iteratedSum=" + iteratedSum
                        + " reportedSum=" + reportedSum);
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
        }
    }

    private static boolean sameDouble(double a, double b) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Math.abs(a - b) <= 1.0e-15;
    }

    private static char pickPrintableChar(int x) {
        return (char) ('!' + Math.abs(x % 93));
    }

    private static char pickDistinctHigherPrintableChar(char base, int x) {
        int baseIdx = base - '!';
        int spanAbove = 92 - baseIdx;
        if (spanAbove <= 0) {
            return '~';
        }
        int offset = 1 + Math.abs(x % spanAbove);
        return (char) (base + offset);
    }
}