package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Collections;
import java.util.Comparator;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int a = data.consumeInt(-1000, 1000);
        int b = data.consumeInt(-1000, 1000);
        int c = data.consumeInt(-1000, 1000);

        if (a == b || a == c || b == c) {
            return;
        }

        int low = a;
        int mid = b;
        int high = c;
        if (low > mid) {
            int t = low;
            low = mid;
            mid = t;
        }
        if (mid > high) {
            int t = mid;
            mid = high;
            high = t;
        }
        if (low > mid) {
            int t = low;
            low = mid;
            mid = t;
        }

        int lowCount = data.consumeInt(1, 8);
        int midCount = data.consumeInt(1, 8);
        int highCount = data.consumeInt(1, 8);
        int sum = lowCount + midCount + highCount;

        Comparator<?> reverse = Collections.reverseOrder();
        Frequency f = new Frequency(reverse);

        try {
            for (int i = 0; i < lowCount; i++) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        f.addValue(low);
                        break;
                    case 1:
                        f.addValue((long) low);
                        break;
                    default:
                        f.addValue((Comparable<?>) Long.valueOf(low));
                        break;
                }
            }
            for (int i = 0; i < midCount; i++) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        f.addValue(mid);
                        break;
                    case 1:
                        f.addValue((long) mid);
                        break;
                    default:
                        f.addValue((Comparable<?>) Long.valueOf(mid));
                        break;
                }
            }
            for (int i = 0; i < highCount; i++) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        f.addValue(high);
                        break;
                    case 1:
                        f.addValue((long) high);
                        break;
                    default:
                        f.addValue((Comparable<?>) Long.valueOf(high));
                        break;
                }
            }

            Object lowAsObject = Integer.valueOf(low);
            double actualObjectPct = f.getPct(lowAsObject);
            double expectedPct = ((double) lowCount) / ((double) sum);

            if (Double.doubleToLongBits(actualObjectPct) != Double.doubleToLongBits(expectedPct)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:reverse-lowest-object-pct-constructed] semantic mismatch: getPct(Object) must return the proportion of values equal to v; comparator only changes ordering for cumulative queries. low="
                        + low + " lowCount=" + lowCount + " mid=" + mid + " midCount=" + midCount
                        + " high=" + high + " highCount=" + highCount + " sum=" + sum
                        + " expected=" + expectedPct + " actual=" + actualObjectPct);
            }

            long reportedSum = f.getSumFreq();
            long recomputedSum = 0L;
            java.util.Iterator<Comparable<?>> it = f.valuesIterator();
            while (it.hasNext()) {
                Comparable<?> value = it.next();
                recomputedSum += f.getCount(value);
            }

            if (reportedSum != recomputedSum) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:iterator-count-sum-consistency-reverse] consistency violation: getSumFreq must equal the sum of counts over the valuesIterator output. reported="
                        + reportedSum + " recomputed=" + recomputedSum);
            }
        } catch (FuzzerSecurityIssueLow issue) {
            throw issue;
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            return;
        }
    }
}