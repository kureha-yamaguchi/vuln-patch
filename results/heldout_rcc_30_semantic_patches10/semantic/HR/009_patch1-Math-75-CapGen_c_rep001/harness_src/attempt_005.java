package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Frequency f;
        int low;
        int high;
        int lowCount;
        int highCount;

        try {
            f = new Frequency();

            int base = data.consumeInt(-1_000_000, 999_999);
            int gap = data.consumeInt(1, 32);
            low = base;
            high = base + gap;

            lowCount = data.consumeInt(1, 8);
            highCount = data.consumeInt(1, 8);

            for (int i = 0; i < lowCount; i++) {
                if (data.consumeBoolean()) {
                    f.addValue(low);
                } else {
                    f.addValue((long) low);
                }
            }
            for (int i = 0; i < highCount; i++) {
                if (data.consumeBoolean()) {
                    f.addValue(high);
                } else {
                    f.addValue((long) high);
                }
            }
        } catch (Throwable t) {
            return;
        }

        String violation = null;

        try {
            long sum = f.getSumFreq();
            long cumLow = f.getCumFreq(low);
            double actualPctHighObject = f.getPct((Object) Integer.valueOf(high));
            double expectedPctHighObject = (double) (sum - cumLow) / (double) sum;

            /*
             * Contract justification:
             * getPct returns "the percentage of values that are equal to v".
             * In a distribution containing exactly two distinct ordered values low < high,
             * getCumFreq(low) counts all low values and no high values, so
             * sum - getCumFreq(low) is exactly the number of high values.
             * Therefore getPct((Object) Integer.valueOf(high)) must equal
             * (sum - getCumFreq(low)) / sum for every correct implementation.
             * This flips the patched condition at the top boundary: the buggy code returns
             * getCumPct(high) == 1.0 instead of the proper high-bucket percentage.
             */
            if (sum > 0) {
                double diff = Math.abs(actualPctHighObject - expectedPctHighObject);
                double tol = 1e-12 * Math.max(1.0,
                        Math.max(Math.abs(actualPctHighObject), Math.abs(expectedPctHighObject)));
                if (diff > tol) {
                    violation =
                            "[oracle:max-bucket-from-cumfreq-gap] metamorphic violation: "
                                    + "getPct((Object) high) must equal (getSumFreq()-getCumFreq(low))/getSumFreq() "
                                    + "low=" + low
                                    + " high=" + high
                                    + " sum=" + sum
                                    + " cumLow=" + cumLow
                                    + " expected=" + expectedPctHighObject
                                    + " actual=" + actualPctHighObject;
                }
            }
        } catch (Throwable t) {
            return;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }

        violation = null;
        try {
            long sum = f.getSumFreq();
            if (sum <= 0) {
                return;
            }

            long cumLow = f.getCumFreq((Object) Integer.valueOf(low));
            long cumHigh = f.getCumFreq((Object) Integer.valueOf(high));
            double cumPctLow = f.getCumPct((Object) Integer.valueOf(low));
            double cumPctHigh = f.getCumPct((Object) Integer.valueOf(high));

            /*
             * Contract justification:
             * getCumPct(Comparable/Object) is defined in terms of getCumFreq(v)/getSumFreq().
             * Cross-checking the reported cumulative percentage against the independently
             * reported cumulative frequency and total frequency is a sound consistency check.
             * A patch that only special-cases getPct but leaves helper bookkeeping wrong
             * would still be caught here.
             */
            double expectedLow = (double) cumLow / (double) sum;
            double expectedHigh = (double) cumHigh / (double) sum;

            double tolLow = 1e-12 * Math.max(1.0, Math.max(Math.abs(expectedLow), Math.abs(cumPctLow)));
            if (Math.abs(expectedLow - cumPctLow) > tolLow) {
                violation =
                        "[oracle:cumpct-self-consistency-low] consistency violation: "
                                + "getCumPct(low) must equal getCumFreq(low)/getSumFreq() "
                                + "low=" + low
                                + " sum=" + sum
                                + " cumFreq=" + cumLow
                                + " expected=" + expectedLow
                                + " actual=" + cumPctLow;
            }

            double tolHigh = 1e-12 * Math.max(1.0, Math.max(Math.abs(expectedHigh), Math.abs(cumPctHigh)));
            if (violation == null && Math.abs(expectedHigh - cumPctHigh) > tolHigh) {
                violation =
                        "[oracle:cumpct-self-consistency-high] consistency violation: "
                                + "getCumPct(high) must equal getCumFreq(high)/getSumFreq() "
                                + "high=" + high
                                + " sum=" + sum
                                + " cumFreq=" + cumHigh
                                + " expected=" + expectedHigh
                                + " actual=" + cumPctHigh;
            }
        } catch (Throwable t) {
            return;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }

        violation = null;
        try {
            long sumBefore = f.getSumFreq();
            int hashBefore = f.hashCode();
            String strBefore = f.toString();

            f.getCumPct((Object) Integer.valueOf(low));
            f.getCumPct((Object) Integer.valueOf(high));

            long sumAfter = f.getSumFreq();
            int hashAfter = f.hashCode();
            String strAfter = f.toString();

            /*
             * Contract justification:
             * getCumPct(Object) is a getter with no documented mutation.
             * Re-reading cheap observable state after the call is a hidden-state check:
             * a throw-deleting or bookkeeping-corrupting patch must not silently mutate
             * getSumFreq(), hashCode(), or toString() as a side effect of a read-only query.
             */
            if (sumBefore != sumAfter || hashBefore != hashAfter || !strBefore.equals(strAfter)) {
                violation =
                        "[oracle:cumpct-object-read-only] hidden-state violation: "
                                + "state changed across getCumPct(Object) calls "
                                + "sumBefore=" + sumBefore
                                + " sumAfter=" + sumAfter
                                + " hashBefore=" + hashBefore
                                + " hashAfter=" + hashAfter
                                + " toStringChanged=" + (!strBefore.equals(strAfter));
            }
        } catch (Throwable t) {
            return;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }
}