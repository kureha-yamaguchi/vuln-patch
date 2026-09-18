package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiftedTestOracles();
        runStateIndependentAbsentProbeChecks(data);
        runRelationCountOverSum(data);
        runRelationObjectComparableAgreement(data);
    }

    private static void runLiftedTestOracles() {
        Frequency f = new Frequency();
        String violation = null;
        try {
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

            long sumBefore = f.getSumFreq();
            int hashBefore = f.hashCode();
            String stringBefore = f.toString();

            double onePct = f.getPct(1);
            double twoPct = f.getPct(Long.valueOf(2));
            double threePct = f.getPct(threeL);
            double threeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
            double fivePct = f.getPct(5);
            double fooPct = f.getPct("foo");
            double oneCumPct = f.getCumPct(1);
            double twoCumPct = f.getCumPct(Long.valueOf(2));
            double integerArgumentCumPct = f.getCumPct(Integer.valueOf(2));
            double threeCumPct = f.getCumPct(threeL);
            double fiveCumPct = f.getCumPct(5);
            double zeroCumPct = f.getCumPct(0);
            double fooCumPct = f.getCumPct("foo");

            long sumAfter = f.getSumFreq();
            int hashAfter = f.hashCode();
            String stringAfter = f.toString();

            // Read-only post-condition: get* methods are queries; they must not mutate observable state.
            // A "fix" that changes bookkeeping or caches incorrectly would break these cheap readers.
            if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
                violation = "[oracle:hidden-state-readonly] semantic mismatch: get* query mutated state sumBefore=" + sumBefore
                        + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                        + " toStringBefore=" + escape(stringBefore) + " toStringAfter=" + escape(stringAfter);
            } else if (!approxEq(onePct, 0.25, TOLERANCE)) {
                violation = "[oracle:one-pct] semantic mismatch: expected=0.25 actual=" + onePct;
            } else if (!approxEq(twoPct, 0.25, TOLERANCE)) {
                violation = "[oracle:two-pct] semantic mismatch: expected=0.25 actual=" + twoPct;
            } else if (!approxEq(threePct, 0.5, TOLERANCE)) {
                violation = "[oracle:three-pct] semantic mismatch: expected=0.5 actual=" + threePct;
            } else if (!approxEq(threeObjectPct, 0.5, TOLERANCE)) {
                violation = "[oracle:three-object-pct] semantic mismatch: expected=0.5 actual=" + threeObjectPct;
            } else if (!approxEq(fivePct, 0.0, TOLERANCE)) {
                violation = "[oracle:five-pct] semantic mismatch: expected=0.0 actual=" + fivePct;
            } else if (!approxEq(fooPct, 0.0, TOLERANCE)) {
                violation = "[oracle:foo-pct] semantic mismatch: expected=0.0 actual=" + fooPct;
            } else if (!approxEq(oneCumPct, 0.25, TOLERANCE)) {
                violation = "[oracle:one-cum-pct] semantic mismatch: expected=0.25 actual=" + oneCumPct;
            } else if (!approxEq(twoCumPct, 0.50, TOLERANCE)) {
                violation = "[oracle:two-cum-pct] semantic mismatch: expected=0.5 actual=" + twoCumPct;
            } else if (!approxEq(integerArgumentCumPct, 0.50, TOLERANCE)) {
                violation = "[oracle:integer-argument-cum-pct] semantic mismatch: expected=0.5 actual=" + integerArgumentCumPct;
            } else if (!approxEq(threeCumPct, 1.0, TOLERANCE)) {
                violation = "[oracle:three-cum-pct] semantic mismatch: expected=1.0 actual=" + threeCumPct;
            } else if (!approxEq(fiveCumPct, 1.0, TOLERANCE)) {
                violation = "[oracle:five-cum-pct] semantic mismatch: expected=1.0 actual=" + fiveCumPct;
            } else if (!approxEq(zeroCumPct, 0.0, TOLERANCE)) {
                violation = "[oracle:zero-cum-pct] semantic mismatch: expected=0.0 actual=" + zeroCumPct;
            } else if (!approxEq(fooCumPct, 0.0, TOLERANCE)) {
                violation = "[oracle:foo-cum-pct] semantic mismatch: expected=0.0 actual=" + fooCumPct;
            }
        } catch (Throwable t) {
            return;
        }
        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static void runStateIndependentAbsentProbeChecks(FuzzedDataProvider data) {
        Frequency f = new Frequency();
        String violation = null;
        try {
            int n = data.consumeInt(1, 8);
            int absentLow = data.consumeInt(-20, -1);
            int base = data.consumeInt(1, 10);

            for (int i = 0; i < n; i++) {
                int v = base + data.consumeInt(0, 10);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }

                // Re-probed after every state change as required for absent-value rejection checks.
                // With only positive integral values inserted, a smaller absent integral probe must have pct 0 and cumPct 0,
                // and an unrelated String probe must have pct 0; these documented query outcomes must not depend on container state.
                double pctAbsentLow = f.getPct(absentLow);
                double cumPctAbsentLow = f.getCumPct(absentLow);
                double pctFoo = f.getPct("foo");

                if (!approxEq(pctAbsentLow, 0.0, TOLERANCE)) {
                    violation = "[oracle:reprobe-absent-pct] semantic mismatch: expected=0.0 actual=" + pctAbsentLow
                            + " afterMutationIndex=" + i + " probe=" + absentLow;
                    break;
                }
                if (!approxEq(cumPctAbsentLow, 0.0, TOLERANCE)) {
                    violation = "[oracle:reprobe-absent-cum-pct] semantic mismatch: expected=0.0 actual=" + cumPctAbsentLow
                            + " afterMutationIndex=" + i + " probe=" + absentLow;
                    break;
                }
                if (!approxEq(pctFoo, 0.0, TOLERANCE)) {
                    violation = "[oracle:reprobe-foo-pct] semantic mismatch: expected=0.0 actual=" + pctFoo
                            + " afterMutationIndex=" + i;
                    break;
                }
            }
        } catch (Throwable t) {
            return;
        }
        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static void runRelationCountOverSum(FuzzedDataProvider data) {
        Frequency f;
        int n;
        int probe;
        long count;
        long sum;
        double expected;
        double actual;
        String violation = null;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-5, 5);
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        f.addValue(v);
                        break;
                    case 1:
                        f.addValue((long) v);
                        break;
                    case 2:
                        f.addValue(Integer.valueOf(v));
                        break;
                    default:
                        f.addValue(Long.valueOf(v));
                        break;
                }
            }
            probe = data.consumeInt(-5, 5);
            count = f.getCount((Object) Integer.valueOf(probe));
            sum = f.getSumFreq();
            expected = (double) count / (double) sum;
            actual = f.getPct((Object) Integer.valueOf(probe));
        } catch (Throwable t) {
            return;
        }

        // Contract guarantee: getPct(Object) returns the percentage of values equal to v; with non-empty Frequency,
        // that is exactly getCount(v) / getSumFreq(). A patch that redirects to cumulative percentage violates this.
        if (!approxEq(actual, expected, 1e-9 * Math.max(1.0, Math.max(Math.abs(actual), Math.abs(expected))))) {
            violation = "relation getPctObject_matches_count_over_sum_for_integer_object violated: actual=" + actual
                    + " expected=" + expected + " count=" + count + " sum=" + sum + " probe=" + probe;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static void runRelationObjectComparableAgreement(FuzzedDataProvider data) {
        Frequency f;
        int n;
        Integer probe;
        double a;
        double b;
        String violation = null;

        try {
            f = new Frequency();
            n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f.addValue(v);
                } else {
                    f.addValue((long) v);
                }
            }
            probe = Integer.valueOf(data.consumeInt(-6, 6));
            a = f.getPct((Object) probe);
            b = f.getPct((Comparable<?>) probe);
        } catch (Throwable t) {
            return;
        }

        // Contract guarantee: deprecated getPct(Object) is the sibling overload for getPct(Comparable);
        // on the same Integer value they must agree. The buggy implementation returns cumulative pct from the Object overload.
        if (!approxEq(a, b, 1e-9 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b))))) {
            violation = "relation getPct_object_and_comparable_agree_on_same_integer_value violated: object=" + a
                    + " comparable=" + b + " probe=" + probe;
        }

        if (violation != null) {
            throw new FuzzerSecurityIssueLow(violation);
        }
    }

    private static boolean approxEq(double actual, double expected, double tolerance) {
        return Math.abs(actual - expected) <= tolerance;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}