package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

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

        String beforeToString = f.toString();
        long beforeSumFreq = f.getSumFreq();
        int beforeHash = f.hashCode();

        double actualOnePct = f.getPct(1);
        if (!closeEnough(actualOnePct, 0.25, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-one-pct] semantic mismatch: expected=0.25 actual=" + actualOnePct);
        }

        double actualTwoPct = f.getPct(Long.valueOf(2));
        if (!closeEnough(actualTwoPct, 0.25, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-two-pct] semantic mismatch: expected=0.25 actual=" + actualTwoPct);
        }

        double actualThreePct = f.getPct(threeL);
        if (!closeEnough(actualThreePct, 0.5, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-three-pct] semantic mismatch: expected=0.5 actual=" + actualThreePct);
        }

        double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        if (!closeEnough(actualThreeObjectPct, 0.5, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-three-object-pct] semantic mismatch: expected=0.5 actual=" + actualThreeObjectPct);
        }

        double actualFivePct = f.getPct(5);
        if (!closeEnough(actualFivePct, 0.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-five-pct] semantic mismatch: expected=0.0 actual=" + actualFivePct);
        }

        double actualFooPct = f.getPct("foo");
        if (!closeEnough(actualFooPct, 0.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-foo-pct] semantic mismatch: expected=0.0 actual=" + actualFooPct);
        }

        double actualOneCumPct = f.getCumPct(1);
        if (!closeEnough(actualOneCumPct, 0.25, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-one-cum-pct] semantic mismatch: expected=0.25 actual=" + actualOneCumPct);
        }

        double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
        if (!closeEnough(actualTwoCumPct, 0.50, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-two-cum-pct] semantic mismatch: expected=0.5 actual=" + actualTwoCumPct);
        }

        double actualIntegerArgumentCumPct = f.getCumPct(Integer.valueOf(2));
        if (!closeEnough(actualIntegerArgumentCumPct, 0.50, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-integer-argument] semantic mismatch: expected=0.5 actual=" + actualIntegerArgumentCumPct);
        }

        double actualThreeCumPct = f.getCumPct(threeL);
        if (!closeEnough(actualThreeCumPct, 1.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-three-cum-pct] semantic mismatch: expected=1.0 actual=" + actualThreeCumPct);
        }

        double actualFiveCumPct = f.getCumPct(5);
        if (!closeEnough(actualFiveCumPct, 1.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-five-cum-pct] semantic mismatch: expected=1.0 actual=" + actualFiveCumPct);
        }

        double actualZeroCumPct = f.getCumPct(0);
        if (!closeEnough(actualZeroCumPct, 0.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-zero-cum-pct] semantic mismatch: expected=0.0 actual=" + actualZeroCumPct);
        }

        double actualFooCumPct = f.getCumPct("foo");
        if (!closeEnough(actualFooCumPct, 0.0, TOLERANCE)) {
            throw new FuzzerSecurityIssueLow("[oracle:testPcts-foo-cum-pct] semantic mismatch: expected=0.0 actual=" + actualFooCumPct);
        }

        long afterSumFreq = f.getSumFreq();
        int afterHash = f.hashCode();
        String afterToString = f.toString();
        if (beforeSumFreq != afterSumFreq || beforeHash != afterHash || !safeEquals(beforeToString, afterToString)) {
            throw new FuzzerSecurityIssueLow("[oracle:hidden-state-read-only] semantic mismatch: getPct/getCumPct are read-only queries, so getSumFreq/hashCode/toString must not change; beforeSumFreq=" + beforeSumFreq + " afterSumFreq=" + afterSumFreq + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeToString=" + escapeOneLine(beforeToString) + " afterToString=" + escapeOneLine(afterToString));
        }

        {
            Frequency relFreq;
            int n;
            int probe;
            long count;
            long sum;
            double actual;
            double expected;
            try {
                relFreq = new Frequency();
                n = data.consumeInt(1, 8);
                for (int i = 0; i < n; i++) {
                    int v = data.consumeInt(-5, 5);
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            relFreq.addValue(v);
                            break;
                        case 1:
                            relFreq.addValue((long) v);
                            break;
                        case 2:
                            relFreq.addValue(Integer.valueOf(v));
                            break;
                        default:
                            relFreq.addValue(Long.valueOf(v));
                            break;
                    }
                }
                probe = data.consumeInt(-5, 5);
                count = relFreq.getCount((Object) Integer.valueOf(probe));
                sum = relFreq.getSumFreq();
                expected = (double) count / (double) sum;
                actual = relFreq.getPct((Object) Integer.valueOf(probe));
            } catch (Exception e) {
                return;
            }
            if (!closeEnough(actual, expected, 1e-9 * Math.max(1.0, Math.max(Math.abs(actual), Math.abs(expected))))) {
                throw new FuzzerSecurityIssueLow("[oracle:count-over-sum] relation getPctObject_matches_count_over_sum_for_integer_object violated: probe=" + probe + " count=" + count + " sum=" + sum + " expected=" + expected + " actual=" + actual);
            }
        }

        {
            Frequency relFreq;
            int n;
            Integer probe;
            double a;
            double b;
            try {
                relFreq = new Frequency();
                n = data.consumeInt(1, 8);
                for (int i = 0; i < n; i++) {
                    int v = data.consumeInt(-6, 6);
                    if (data.consumeBoolean()) {
                        relFreq.addValue(v);
                    } else {
                        relFreq.addValue((long) v);
                    }
                }
                probe = Integer.valueOf(data.consumeInt(-6, 6));
                a = relFreq.getPct((Object) probe);
                b = relFreq.getPct((Comparable<?>) probe);
            } catch (Exception e) {
                return;
            }
            if (!closeEnough(a, b, 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b))))) {
                throw new FuzzerSecurityIssueLow("[oracle:object-vs-comparable] relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe=" + probe + " object=" + a + " comparable=" + b);
            }
        }

        {
            Frequency relFreq;
            int n;
            int probe;
            double pct;
            long count;
            long sum;
            try {
                relFreq = new Frequency();
                n = data.consumeInt(1, 8);
                for (int i = 0; i < n; i++) {
                    int v = data.consumeInt(-4, 4);
                    if (data.consumeBoolean()) {
                        relFreq.addValue(Integer.valueOf(v));
                    } else {
                        relFreq.addValue(Long.valueOf(v));
                    }
                }
                probe = data.consumeInt(-4, 4);
                pct = relFreq.getPct(probe);
                count = relFreq.getCount(probe);
                sum = relFreq.getSumFreq();
            } catch (Exception e) {
                return;
            }
            double expected = (double) count / (double) sum;
            if (!closeEnough(pct, expected, 1e-9 * Math.max(1.0, Math.max(Math.abs(pct), Math.abs(expected))))) {
                throw new FuzzerSecurityIssueLow("[oracle:int-overload-count-over-sum] metamorphic violation: getPct(int) must equal getCount(int)/getSumFreq on a non-empty distribution; probe=" + probe + " count=" + count + " sum=" + sum + " expected=" + expected + " actual=" + pct);
            }
        }
    }

    private static boolean closeEnough(double a, double b, double tolerance) {
        return Math.abs(a - b) <= tolerance;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escapeOneLine(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}