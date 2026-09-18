package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static boolean eqDouble(double a, double b, double tol) {
        return Math.abs(a - b) <= tol;
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        // Faithful reconstruction of FrequencyTest.testPcts setup and all lifted assertions.
        try {
            long oneL = 1;
            long twoL = 2;
            long threeL = 3;
            int oneI = 1;
            int twoI = 2;
            int threeI = 3;
            Frequency f = new Frequency();

            f.addValue(oneL);
            f.addValue(twoL);
            f.addValue(oneI);
            f.addValue(twoI);
            f.addValue(threeL);
            f.addValue(threeL);
            f.addValue(3);
            f.addValue(threeI);

            // Read-only hidden-state check: getPct/getCumPct are query methods; they must not change
            // cheap observable state such as total frequency, hashCode, or string form.
            long sumBefore = f.getSumFreq();
            int hashBefore = f.hashCode();
            String strBefore = f.toString();

            double actualOnePct = f.getPct(1);
            if (!eqDouble(actualOnePct, 0.25, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:one-pct] semantic mismatch: expected=0.25 actual=" + actualOnePct);
            }

            double actualTwoPct = f.getPct(Long.valueOf(2));
            if (!eqDouble(actualTwoPct, 0.25, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:two-pct] semantic mismatch: expected=0.25 actual=" + actualTwoPct);
            }

            double actualThreePct = f.getPct(threeL);
            if (!eqDouble(actualThreePct, 0.5, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:three-pct] semantic mismatch: expected=0.5 actual=" + actualThreePct);
            }

            double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
            if (!eqDouble(actualThreeObjectPct, 0.5, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:three-object-pct] semantic mismatch: expected=0.5 actual=" + actualThreeObjectPct);
            }

            double actualFivePct = f.getPct(5);
            if (!eqDouble(actualFivePct, 0.0, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:five-pct] semantic mismatch: expected=0.0 actual=" + actualFivePct);
            }

            double actualFooPct = f.getPct("foo");
            if (!eqDouble(actualFooPct, 0.0, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:foo-pct] semantic mismatch: expected=0.0 actual=" + actualFooPct);
            }

            double actualOneCumPct = f.getCumPct(1);
            if (!eqDouble(actualOneCumPct, 0.25, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:one-cum-pct] semantic mismatch: expected=0.25 actual=" + actualOneCumPct);
            }

            double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
            if (!eqDouble(actualTwoCumPct, 0.50, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:two-cum-pct] semantic mismatch: expected=0.5 actual=" + actualTwoCumPct);
            }

            double actualIntegerArgCumPct = f.getCumPct(Integer.valueOf(2));
            if (!eqDouble(actualIntegerArgCumPct, 0.50, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:integer-argument-cum-pct] semantic mismatch: expected=0.5 actual=" + actualIntegerArgCumPct);
            }

            double actualThreeCumPct = f.getCumPct(threeL);
            if (!eqDouble(actualThreeCumPct, 1.0, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:three-cum-pct] semantic mismatch: expected=1.0 actual=" + actualThreeCumPct);
            }

            double actualFiveCumPct = f.getCumPct(5);
            if (!eqDouble(actualFiveCumPct, 1.0, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:five-cum-pct] semantic mismatch: expected=1.0 actual=" + actualFiveCumPct);
            }

            double actualZeroCumPct = f.getCumPct(0);
            if (!eqDouble(actualZeroCumPct, 0.0, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:zero-cum-pct] semantic mismatch: expected=0.0 actual=" + actualZeroCumPct);
            }

            double actualFooCumPct = f.getCumPct("foo");
            if (!eqDouble(actualFooCumPct, 0.0, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:foo-cum-pct] semantic mismatch: expected=0.0 actual=" + actualFooCumPct);
            }

            long sumAfter = f.getSumFreq();
            int hashAfter = f.hashCode();
            String strAfter = f.toString();
            if (sumBefore != sumAfter || hashBefore != hashAfter || (strBefore == null ? strAfter != null : !strBefore.equals(strAfter))) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:read-only-state] semantic mismatch: getPct/getCumPct changed observable state sumBefore=" + sumBefore +
                    " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter +
                    " strBefore=" + strBefore + " strAfter=" + strAfter);
            }

            // Sibling-agreement post-condition: deprecated getPct(Object) is documented replacement-compatible
            // with getPct(Comparable); equivalent Integer inputs must agree. A "fix" that merely dodges the buggy
            // branch or returns another lookup would violate this without throwing.
            double viaObject = f.getPct((Object) Integer.valueOf(3));
            double viaComparable = f.getPct((Comparable<?>) Integer.valueOf(3));
            if (!eqDouble(viaObject, viaComparable, tolerance)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:object-vs-comparable-seed] metamorphic violation: getPct(Object)!=getPct(Comparable) value=3 object=" +
                    viaObject + " comparable=" + viaComparable);
            }
        } catch (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow e) {
            throw e;
        } catch (Throwable e) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-setup] unexpected exception on valid reconstructed test input", e);
        }

        // Candidate relation 1: getPct(Object Integer) must equal getCount(Object Integer)/getSumFreq()
        // because getPct returns the proportion of values equal to v, and integer values are not distinguished by type.
        Frequency f1 = null;
        Integer probe1 = null;
        long count1 = 0;
        long sum1 = 0;
        double actual1 = 0.0;
        double expected1 = 0.0;
        boolean relation1Ready = false;
        try {
            f1 = new Frequency();
            int n = data.consumeInt(1, 8);
            for (int i = 0; i < n; i++) {
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
            actual1 = f1.getPct((Object) probe1);
            relation1Ready = true;
        } catch (Throwable e) {
            return;
        }
        if (relation1Ready) {
            double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(actual1), Math.abs(expected1)));
            if (!(Math.abs(actual1 - expected1) <= tol)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:getPctObject_matches_count_over_sum_for_integer_object] relation getPctObject_matches_count_over_sum_for_integer_object violated: probe=" +
                    probe1 + " actual=" + actual1 + " expected=" + expected1 + " count=" + count1 + " sum=" + sum1);
            }
        }

        // Candidate relation 2: same logical Integer queried through getPct(Object) and getPct(Comparable)
        // must agree; both are real API calls over the same value space.
        Frequency f2 = null;
        Integer probe2 = null;
        double a2 = 0.0;
        double b2 = 0.0;
        boolean relation2Ready = false;
        try {
            f2 = new Frequency();
            int n2 = data.consumeInt(1, 8);
            for (int i = 0; i < n2; i++) {
                int v = data.consumeInt(-6, 6);
                if (data.consumeBoolean()) {
                    f2.addValue(v);
                } else {
                    f2.addValue((long) v);
                }
            }
            probe2 = Integer.valueOf(data.consumeInt(-6, 6));
            a2 = f2.getPct((Object) probe2);
            b2 = f2.getPct((Comparable<?>) probe2);
            relation2Ready = true;
        } catch (Throwable e) {
            return;
        }
        if (relation2Ready) {
            double tol = 1e-9 * Math.max(1.0, Math.max(Math.abs(a2), Math.abs(b2)));
            if (!(Math.abs(a2 - b2) <= tol)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:getPct_object_and_comparable_agree_on_same_integer_value] relation getPct_object_and_comparable_agree_on_same_integer_value violated: probe=" +
                    probe2 + " object=" + a2 + " comparable=" + b2);
            }
        }

        // Additional trusted generalisation: construct a non-empty integral distribution from fuzzed values,
        // then for a chosen probe getPct(int), getPct(long), getPct(Integer as Comparable), and getPct(Object Integer)
        // must all agree because the class contract says integer values are not distinguished by type.
        Frequency f3 = null;
        int probe3 = 0;
        double pInt = 0.0;
        double pLong = 0.0;
        double pComp = 0.0;
        double pObj = 0.0;
        long sumBefore3 = 0L;
        long sumAfter3 = 0L;
        boolean relation3Ready = false;
        try {
            f3 = new Frequency();
            int n3 = data.consumeInt(1, 10);
            for (int i = 0; i < n3; i++) {
                int v = data.consumeInt(-8, 8);
                switch (data.consumeInt(0, 1)) {
                    case 0:
                        f3.addValue(v);
                        break;
                    default:
                        f3.addValue((long) v);
                        break;
                }
            }
            probe3 = data.consumeInt(-8, 8);
            sumBefore3 = f3.getSumFreq();
            pInt = f3.getPct(probe3);
            pLong = f3.getPct((long) probe3);
            pComp = f3.getPct((Comparable<?>) Integer.valueOf(probe3));
            pObj = f3.getPct((Object) Integer.valueOf(probe3));
            sumAfter3 = f3.getSumFreq();
            relation3Ready = true;
        } catch (Throwable e) {
            return;
        }
        if (relation3Ready) {
            double base = pInt;
            double tol3 = 1e-9 * Math.max(1.0, Math.max(Math.abs(base), Math.max(Math.abs(pLong), Math.max(Math.abs(pComp), Math.abs(pObj)))));
            if (!(Math.abs(pInt - pLong) <= tol3 && Math.abs(pInt - pComp) <= tol3 && Math.abs(pInt - pObj) <= tol3)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:integer-overloads-agree] metamorphic violation: probe=" + probe3 +
                    " int=" + pInt + " long=" + pLong + " comparable=" + pComp + " object=" + pObj);
            }
            if (sumBefore3 != sumAfter3) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:query-does-not-mutate-sum] metamorphic violation: sum changed across getPct overload queries probe=" +
                    probe3 + " before=" + sumBefore3 + " after=" + sumAfter3);
            }
        }
    }
}