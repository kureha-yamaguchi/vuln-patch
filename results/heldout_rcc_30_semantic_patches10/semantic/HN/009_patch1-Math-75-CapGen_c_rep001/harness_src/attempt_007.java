package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

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

        long sumBeforeRead = f.getSumFreq();
        int hashBeforeRead = f.hashCode();
        String toStringBeforeRead = f.toString();

        double onePct = f.getPct(1);
        if (!approxEq(onePct, 0.25, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-one-pct] semantic mismatch: expected=0.25 actual=" + onePct);
        }

        double twoPct = f.getPct(Long.valueOf(2));
        if (!approxEq(twoPct, 0.25, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-two-pct] semantic mismatch: expected=0.25 actual=" + twoPct);
        }

        double threePct = f.getPct(threeL);
        if (!approxEq(threePct, 0.5, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-three-pct] semantic mismatch: expected=0.5 actual=" + threePct);
        }

        double threeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        if (!approxEq(threeObjectPct, 0.5, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-three-object-pct] semantic mismatch: expected=0.5 actual=" + threeObjectPct);
        }

        double fivePct = f.getPct(5);
        if (!approxEq(fivePct, 0.0, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-five-pct] semantic mismatch: expected=0.0 actual=" + fivePct);
        }

        double fooPct = f.getPct("foo");
        if (!approxEq(fooPct, 0.0, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-foo-pct] semantic mismatch: expected=0.0 actual=" + fooPct);
        }

        double oneCumPct = f.getCumPct(1);
        if (!approxEq(oneCumPct, 0.25, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-one-cum-pct] semantic mismatch: expected=0.25 actual=" + oneCumPct);
        }

        double twoCumPct = f.getCumPct(Long.valueOf(2));
        if (!approxEq(twoCumPct, 0.50, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-two-cum-pct] semantic mismatch: expected=0.5 actual=" + twoCumPct);
        }

        double integerArgCumPct = f.getCumPct(Integer.valueOf(2));
        if (!approxEq(integerArgCumPct, 0.50, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-integer-argument] semantic mismatch: expected=0.5 actual=" + integerArgCumPct);
        }

        double threeCumPct = f.getCumPct(threeL);
        if (!approxEq(threeCumPct, 1.0, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-three-cum-pct] semantic mismatch: expected=1.0 actual=" + threeCumPct);
        }

        double fiveCumPct = f.getCumPct(5);
        if (!approxEq(fiveCumPct, 1.0, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-five-cum-pct] semantic mismatch: expected=1.0 actual=" + fiveCumPct);
        }

        double zeroCumPct = f.getCumPct(0);
        if (!approxEq(zeroCumPct, 0.0, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-zero-cum-pct] semantic mismatch: expected=0.0 actual=" + zeroCumPct);
        }

        double fooCumPct = f.getCumPct("foo");
        if (!approxEq(fooCumPct, 0.0, TOLERANCE)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-foo-cum-pct] semantic mismatch: expected=0.0 actual=" + fooCumPct);
        }

        // Read-only post-condition: getPct/getCumPct are query methods over the frequency table.
        // A throw-deleting or wrong-bookkeeping patch that mutates hidden state would violate these cheap observable readers.
        long sumAfterRead = f.getSumFreq();
        int hashAfterRead = f.hashCode();
        String toStringAfterRead = f.toString();
        if (sumBeforeRead != sumAfterRead || hashBeforeRead != hashAfterRead || !safeEquals(toStringBeforeRead, toStringAfterRead)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:read-only-state] semantic mismatch: get* query mutated state sumBefore=" + sumBeforeRead +
                " sumAfter=" + sumAfterRead +
                " hashBefore=" + hashBeforeRead +
                " hashAfter=" + hashAfterRead +
                " toStringBefore=" + toStringBeforeRead +
                " toStringAfter=" + toStringAfterRead);
        }

        Frequency f1;
        int n1;
        Integer probe1;
        double actual1;
        double expected1;
        long count1;
        long sum1;
        try {
            f1 = new Frequency();
            n1 = data.consumeInt(1, 8);
            for (int i = 0; i < n1; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f1.addValue(v);
                        break;
                    case 1:
                        f1.addValue((long) v);
                        break;
                    case 2:
                        f1.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f1.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe1 = Integer.valueOf(data.consumeInt(-5, 5));
            count1 = f1.getCount((Object) probe1);
            sum1 = f1.getSumFreq();
            expected1 = (double) count1 / (double) sum1;
        } catch (Throwable e) {
            return;
        }
        try {
            actual1 = f1.getPct((Object) probe1);
        } catch (Throwable e) {
            return;
        }
        if (!approxEq(actual1, expected1, 1e-9 * Math.max(1.0, Math.max(Math.abs(actual1), Math.abs(expected1))))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:count-over-sum] relation getPctObject_matches_count_over_sum_for_integer_object violated: actual=" +
                actual1 + " expected=" + expected1 + " count=" + count1 + " sum=" + sum1 + " probe=" + probe1);
        }

        Frequency f2;
        int n2;
        Integer probe2;
        double objectPct;
        double comparablePct;
        try {
            f2 = new Frequency();
            n2 = data.consumeInt(1, 8);
            for (int i = 0; i < n2; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f2.addValue(v);
                } else {
                    f2.addValue((long) v);
                }
            }
            probe2 = Integer.valueOf(data.consumeInt(-6, 6));
        } catch (Throwable e) {
            return;
        }
        try {
            objectPct = f2.getPct((Object) probe2);
            comparablePct = f2.getPct((Comparable<?>) probe2);
        } catch (Throwable e) {
            return;
        }
        if (!approxEq(objectPct, comparablePct, 1e-9 * Math.max(1.0, Math.max(Math.abs(objectPct), Math.abs(comparablePct))))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:object-vs-comparable] relation getPct_object_and_comparable_agree_on_same_integer_value violated: object=" +
                objectPct + " comparable=" + comparablePct + " probe=" + probe2);
        }

        Frequency f3;
        Integer canonical;
        double pctInt;
        double pctLong;
        long countInt;
        long countLong;
        try {
            f3 = new Frequency();
            int n3 = data.consumeInt(1, 8);
            for (int i = 0; i < n3; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f3.addValue(Integer.valueOf(v));
                } else {
                    f3.addValue(Long.valueOf(v));
                }
            }
            canonical = Integer.valueOf(data.consumeInt(-6, 6));
            pctInt = f3.getPct((Object) canonical);
            pctLong = f3.getPct((Object) Long.valueOf(canonical.longValue()));
            countInt = f3.getCount((Object) canonical);
            countLong = f3.getCount((Object) Long.valueOf(canonical.longValue()));
        } catch (Throwable e) {
            return;
        }
        // Equivalent-input post-condition from Frequency's integer-cross-type contract:
        // integer values are not distinguished by type for get* methods, so Integer(x) and Long(x) must agree.
        if (countInt != countLong || !approxEq(pctInt, pctLong, 1e-9 * Math.max(1.0, Math.max(Math.abs(pctInt), Math.abs(pctLong))))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:integer-cross-type] metamorphic violation: Integer/Long equivalent inputs disagree probe=" + canonical +
                " countInt=" + countInt + " countLong=" + countLong +
                " pctInt=" + pctInt + " pctLong=" + pctLong);
        }
    }

    private static boolean approxEq(double a, double b, double tol) {
        return Math.abs(a - b) <= tol;
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}