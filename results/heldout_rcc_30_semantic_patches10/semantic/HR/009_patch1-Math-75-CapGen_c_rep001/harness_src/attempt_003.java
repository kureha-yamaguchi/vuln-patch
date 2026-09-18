package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Iterator;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    private static void requireDoubleEquals(String oracleId, String what, double expected, double actual) {
        if (Double.isNaN(expected) ? !Double.isNaN(actual) : Math.abs(expected - actual) > TOLERANCE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual
            );
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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

        requireDoubleEquals("lifted-one-pct", "f.getPct(1)", 0.25, f.getPct(1));
        requireDoubleEquals("lifted-two-pct", "f.getPct(Long.valueOf(2))", 0.25, f.getPct(Long.valueOf(2)));
        requireDoubleEquals("lifted-three-pct", "f.getPct(threeL)", 0.5, f.getPct(threeL));
        requireDoubleEquals("lifted-three-object-pct-copy", "f.getPct((Object) Integer.valueOf(3))", 0.5, f.getPct((Object) Integer.valueOf(3)));
        requireDoubleEquals("lifted-five-pct-copy", "f.getPct(5)", 0.0, f.getPct(5));
        requireDoubleEquals("lifted-foo-pct-copy", "f.getPct(\"foo\")", 0.0, f.getPct("foo"));
        requireDoubleEquals("lifted-one-cum-pct-copy", "f.getCumPct(1)", 0.25, f.getCumPct(1));
        requireDoubleEquals("lifted-two-cum-pct-copy", "f.getCumPct(Long.valueOf(2))", 0.50, f.getCumPct(Long.valueOf(2)));
        requireDoubleEquals("lifted-integer-argument-copy", "f.getCumPct(Integer.valueOf(2))", 0.50, f.getCumPct(Integer.valueOf(2)));
        requireDoubleEquals("lifted-three-cum-pct-copy", "f.getCumPct(threeL)", 1.0, f.getCumPct(threeL));
        requireDoubleEquals("lifted-five-cum-pct-copy", "f.getCumPct(5)", 1.0, f.getCumPct(5));
        requireDoubleEquals("lifted-zero-cum-pct-copy", "f.getCumPct(0)", 0.0, f.getCumPct(0));
        requireDoubleEquals("lifted-foo-cum-pct-copy", "f.getCumPct(\"foo\")", 0.0, f.getCumPct("foo"));

        Frequency fuzzFreq;
        try {
            fuzzFreq = new Frequency();
            int m = data.consumeInt(1, 8);
            for (int i = 0; i < m; i++) {
                int v = data.consumeInt(-1000, 1000);
                if (data.consumeBoolean()) {
                    fuzzFreq.addValue(v);
                } else {
                    fuzzFreq.addValue((long) v);
                }
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        int chosen;
        try {
            if (data.consumeBoolean()) {
                Iterator<?> it = fuzzFreq.valuesIterator();
                if (!it.hasNext()) {
                    return;
                }
                Object first = it.next();
                if (!(first instanceof Number)) {
                    return;
                }
                chosen = ((Number) first).intValue();
            } else {
                chosen = data.consumeInt(-1000, 1000);
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        double pctObject;
        double pctInt;
        double pctComparable;
        try {
            long sumBefore = fuzzFreq.getSumFreq();
            int hashBefore = fuzzFreq.hashCode();
            String stringBefore = fuzzFreq.toString();

            pctObject = fuzzFreq.getPct((Object) Integer.valueOf(chosen));
            pctInt = fuzzFreq.getPct(chosen);
            pctComparable = fuzzFreq.getPct((Comparable<?>) Integer.valueOf(chosen));

            long sumAfter = fuzzFreq.getSumFreq();
            int hashAfter = fuzzFreq.hashCode();
            String stringAfter = fuzzFreq.toString();

            if (sumBefore != sumAfter || hashBefore != hashAfter || !stringBefore.equals(stringAfter)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:getpct-object-hidden-state] metamorphic violation: getPct(Object) is a read-only accessor and must not change observable state; sumBefore="
                        + sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                        + " stringBefore=" + stringBefore + " stringAfter=" + stringAfter
                );
            }
        } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow finding) {
            throw finding;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }

        if (Math.abs(pctObject - pctInt) > TOLERANCE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:getpct-object-vs-int-overload] metamorphic violation: overloads for the same logical integer input must agree because integer values are not distinguished by type; n="
                    + chosen + " objectPct=" + pctObject + " intPct=" + pctInt
            );
        }

        if (Math.abs(pctObject - pctComparable) > TOLERANCE) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:getpct-object-vs-comparable-overload] metamorphic violation: getPct(Object) delegates the same query space as getPct(Comparable) for Integer input; n="
                    + chosen + " objectPct=" + pctObject + " comparablePct=" + pctComparable
            );
        }

        long cumAtN;
        long cumAtPrev;
        long countAtN;
        try {
            int prev = chosen == Integer.MIN_VALUE ? chosen : chosen - 1;
            cumAtN = fuzzFreq.getCumFreq(chosen);
            cumAtPrev = fuzzFreq.getCumFreq(prev);
            countAtN = fuzzFreq.getCount(chosen);

            if (chosen != Integer.MIN_VALUE && cumAtN - cumAtPrev != countAtN) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:cumfreq-delta-equals-count] consistency violation: for integral data, getCumFreq(n)-getCumFreq(n-1) must equal getCount(n); n="
                        + chosen + " cumAtN=" + cumAtN + " cumAtPrev=" + cumAtPrev + " countAtN=" + countAtN
                );
            }
        } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow finding) {
            throw finding;
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            return;
        }
    }
}