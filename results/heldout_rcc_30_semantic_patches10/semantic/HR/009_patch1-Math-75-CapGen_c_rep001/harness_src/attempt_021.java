package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Iterator;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        String violation = null;
        Throwable cause = null;

        try {
            Frequency seeded = new Frequency();
            seeded.addValue(Long.valueOf(1));
            seeded.addValue(Long.valueOf(2));
            seeded.addValue(Integer.valueOf(1));
            seeded.addValue(Integer.valueOf(2));
            seeded.addValue(Long.valueOf(3));
            seeded.addValue(Long.valueOf(3));
            seeded.addValue(3);
            seeded.addValue(Integer.valueOf(3));

            long sumFreq = seeded.getSumFreq();
            double totalPct = 0.0;
            Iterator<?> it = seeded.valuesIterator();
            while (it.hasNext()) {
                Object key = it.next();
                totalPct += seeded.getPct(key);
            }

            if (Math.abs(totalPct - 1.0d) > 1.0e-15d) {
                violation =
                    "[oracle:object-pct-distinct-partition-total] consistency violation: " +
                    "sum of getPct(Object) over all distinct observed values must equal 1.0 " +
                    "because getPct is documented as the percentage of values equal to v and the distinct values partition the sample; " +
                    "sumFreq=" + sumFreq + " totalPct=" + totalPct + " toString=" + seeded.toString();
            }
        } catch (Throwable t) {
            return;
        }

        if (violation != null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(violation, cause);
        }

        Frequency fuzzed;
        long sumBefore;
        int hashBefore;
        String strBefore;
        Double totalPct = null;
        Double queryPct = null;
        Long fuzzedSum = null;
        Integer distinctCount = null;
        try {
            fuzzed = new Frequency();

            int base = data.consumeInt(-100000, 100000);
            int c1 = data.consumeInt(1, 4);
            int c2 = data.consumeInt(1, 4);
            int c3 = data.consumeInt(1, 4);

            for (int i = 0; i < c1; i++) {
                if (data.consumeBoolean()) {
                    fuzzed.addValue(base);
                } else {
                    fuzzed.addValue((long) base);
                }
            }
            for (int i = 0; i < c2; i++) {
                if (data.consumeBoolean()) {
                    fuzzed.addValue(base + 1);
                } else {
                    fuzzed.addValue((long) (base + 1));
                }
            }
            for (int i = 0; i < c3; i++) {
                if (data.consumeBoolean()) {
                    fuzzed.addValue(base + 2);
                } else {
                    fuzzed.addValue((long) (base + 2));
                }
            }

            sumBefore = fuzzed.getSumFreq();
            hashBefore = fuzzed.hashCode();
            strBefore = fuzzed.toString();

            double running = 0.0d;
            int seen = 0;
            Iterator<?> it = fuzzed.valuesIterator();
            while (it.hasNext()) {
                Object key = it.next();
                running += fuzzed.getPct(key);
                seen++;
            }

            totalPct = Double.valueOf(running);
            queryPct = Double.valueOf(fuzzed.getPct((Object) Integer.valueOf(base + 1)));
            fuzzedSum = Long.valueOf(fuzzed.getSumFreq());
            distinctCount = Integer.valueOf(seen);

            long sumAfter = fuzzed.getSumFreq();
            int hashAfter = fuzzed.hashCode();
            String strAfter = fuzzed.toString();

            if (sumBefore != sumAfter || hashBefore != hashAfter || !strBefore.equals(strAfter)) {
                violation =
                    "[oracle:getpct-object-readonly-axis-iter-partition] metamorphic violation: " +
                    "getPct(Object) is a read-only accessor and must not change observable state; " +
                    "sum " + sumBefore + "->" + sumAfter +
                    " hash " + hashBefore + "->" + hashAfter +
                    " strChanged=" + (!strBefore.equals(strAfter));
            }
        } catch (Throwable t) {
            return;
        }

        if (violation != null) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(violation, cause);
        }

        if (distinctCount != null && distinctCount.intValue() >= 3 && totalPct != null) {
            if (Math.abs(totalPct.doubleValue() - 1.0d) > 1.0e-12d) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:fuzzed-object-pct-distinct-partition-total] consistency violation: " +
                    "sum of getPct(Object) over all distinct values from valuesIterator must equal 1.0 " +
                    "because those distinct values partition the dataset; " +
                    "distinctCount=" + distinctCount +
                    " sumFreq=" + fuzzedSum +
                    " middlePct=" + queryPct +
                    " totalPct=" + totalPct
                );
            }
        }
    }
}