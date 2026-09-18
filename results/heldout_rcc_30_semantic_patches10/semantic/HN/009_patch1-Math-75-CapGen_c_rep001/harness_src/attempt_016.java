package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        Frequency f = new Frequency();
        long oneL = 1L;
        long twoL = 2L;
        long threeL = 3L;
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

        double actualOnePct = f.getPct(1);
        if (Math.abs(actualOnePct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-one-pct] semantic mismatch: expected=0.25 actual=" + actualOnePct
            );
        }

        double actualTwoPct = f.getPct(Long.valueOf(2));
        if (Math.abs(actualTwoPct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-two-pct] semantic mismatch: expected=0.25 actual=" + actualTwoPct
            );
        }

        double actualThreePct = f.getPct(threeL);
        if (Math.abs(actualThreePct - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-three-pct] semantic mismatch: expected=0.5 actual=" + actualThreePct
            );
        }

        long sumBeforeObjectPct = f.getSumFreq();
        int hashBeforeObjectPct = f.hashCode();
        String stringBeforeObjectPct = f.toString();
        double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        long sumAfterObjectPct = f.getSumFreq();
        int hashAfterObjectPct = f.hashCode();
        String stringAfterObjectPct = f.toString();

        if (Math.abs(actualThreeObjectPct - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-three-object-pct] semantic mismatch: expected=0.5 actual=" + actualThreeObjectPct
            );
        }

        // getPct is a read-only query ("Returns the percentage..."), so cheap observable state must not change.
        // A patch that redirects to a mutating path or corrupts bookkeeping would violate this post-condition.
        if (sumBeforeObjectPct != sumAfterObjectPct
                || hashBeforeObjectPct != hashAfterObjectPct
                || (stringBeforeObjectPct == null ? stringAfterObjectPct != null : !stringBeforeObjectPct.equals(stringAfterObjectPct))) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:hidden-state-getPctObject] semantic mismatch: getPct(Object) changed state"
                    + " sumBefore=" + sumBeforeObjectPct
                    + " sumAfter=" + sumAfterObjectPct
                    + " hashBefore=" + hashBeforeObjectPct
                    + " hashAfter=" + hashAfterObjectPct
                    + " toStringBefore=" + String.valueOf(stringBeforeObjectPct)
                    + " toStringAfter=" + String.valueOf(stringAfterObjectPct)
            );
        }

        double actualFivePct = f.getPct(5);
        if (Math.abs(actualFivePct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-five-pct] semantic mismatch: expected=0.0 actual=" + actualFivePct
            );
        }

        double actualFooPct = f.getPct("foo");
        if (Math.abs(actualFooPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-foo-pct] semantic mismatch: expected=0.0 actual=" + actualFooPct
            );
        }

        double actualOneCumPct = f.getCumPct(1);
        if (Math.abs(actualOneCumPct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-one-cum-pct] semantic mismatch: expected=0.25 actual=" + actualOneCumPct
            );
        }

        double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actualTwoCumPct - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-two-cum-pct] semantic mismatch: expected=0.5 actual=" + actualTwoCumPct
            );
        }

        double actualIntegerArgumentCumPct = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actualIntegerArgumentCumPct - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-integer-argument] semantic mismatch: expected=0.5 actual=" + actualIntegerArgumentCumPct
            );
        }

        double actualThreeCumPct = f.getCumPct(threeL);
        if (Math.abs(actualThreeCumPct - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-three-cum-pct] semantic mismatch: expected=1.0 actual=" + actualThreeCumPct
            );
        }

        double actualFiveCumPct = f.getCumPct(5);
        if (Math.abs(actualFiveCumPct - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-five-cum-pct] semantic mismatch: expected=1.0 actual=" + actualFiveCumPct
            );
        }

        double actualZeroCumPct = f.getCumPct(0);
        if (Math.abs(actualZeroCumPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-zero-cum-pct] semantic mismatch: expected=0.0 actual=" + actualZeroCumPct
            );
        }

        double actualFooCumPct = f.getCumPct("foo");
        if (Math.abs(actualFooCumPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:testPcts-foo-cum-pct] semantic mismatch: expected=0.0 actual=" + actualFooCumPct
            );
        }

        {
            Frequency fuzzFreq = null;
            Integer probe = null;
            Double lhs = null;
            Double rhs = null;
            boolean skip = false;
            Exception caught = null;
            try {
                fuzzFreq = new Frequency();
                int n = data.consumeInt(1, 8);
                for (int i = 0; i < n; i++) {
                    int v = data.consumeInt(-6, 6);
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            fuzzFreq.addValue(v);
                            break;
                        case 1:
                            fuzzFreq.addValue((long) v);
                            break;
                        case 2:
                            fuzzFreq.addValue(Integer.valueOf(v));
                            break;
                        default:
                            fuzzFreq.addValue(Long.valueOf(v));
                            break;
                    }
                }
                probe = Integer.valueOf(data.consumeInt(-6, 6));
                lhs = Double.valueOf(fuzzFreq.getPct((Object) probe));
                rhs = Double.valueOf(fuzzFreq.getPct((Comparable<?>) probe));
            } catch (Exception e) {
                skip = true;
                caught = e;
            }
            if (!skip) {
                // Deprecated getPct(Object) is documented as the same logical query as getPct(Comparable),
                // and integer values are not distinguished by type in Frequency; equivalent Integer inputs must agree.
                double a = lhs.doubleValue();
                double b = rhs.doubleValue();
                double relTol = 1e-9 * Math.max(1.0d, Math.max(Math.abs(a), Math.abs(b)));
                if (Math.abs(a - b) > relTol) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "relation getPct_object_and_comparable_agree_on_same_integer_value violated: object=" + a + " comparable=" + b + " probe=" + probe
                    );
                }
            } else if (caught != null) {
                return;
            }
        }

        {
            Frequency fuzzFreq = null;
            Integer probe = null;
            Long count = null;
            Long sum = null;
            Double actual = null;
            Double expected = null;
            boolean skip = false;
            Exception caught = null;
            try {
                fuzzFreq = new Frequency();
                int n = data.consumeInt(1, 8);
                for (int i = 0; i < n; i++) {
                    int v = data.consumeInt(-5, 5);
                    switch (data.consumeInt(0, 3)) {
                        case 0:
                            fuzzFreq.addValue(v);
                            break;
                        case 1:
                            fuzzFreq.addValue((long) v);
                            break;
                        case 2:
                            fuzzFreq.addValue(Integer.valueOf(v));
                            break;
                        default:
                            fuzzFreq.addValue(Long.valueOf(v));
                            break;
                    }
                }
                probe = Integer.valueOf(data.consumeInt(-5, 5));
                count = Long.valueOf(fuzzFreq.getCount((Object) probe));
                sum = Long.valueOf(fuzzFreq.getSumFreq());
                expected = Double.valueOf((double) count.longValue() / (double) sum.longValue());
                actual = Double.valueOf(fuzzFreq.getPct((Object) probe));
            } catch (Exception e) {
                skip = true;
                caught = e;
            }
            if (!skip) {
                // Contract: getPct(v) returns the percentage of values equal to v; on a non-empty Frequency this is getCount(v)/getSumFreq().
                // This catches silent wrong-answer patches even when no exception is thrown.
                double a = actual.doubleValue();
                double e = expected.doubleValue();
                double relTol = 1e-9 * Math.max(1.0d, Math.max(Math.abs(a), Math.abs(e)));
                if (Math.abs(a - e) > relTol) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "relation getPctObject_matches_count_over_sum_for_integer_object violated: actual=" + a
                            + " expected=" + e
                            + " count=" + count.longValue()
                            + " sum=" + sum.longValue()
                            + " probe=" + probe
                    );
                }
            } else if (caught != null) {
                return;
            }
        }
    }
}