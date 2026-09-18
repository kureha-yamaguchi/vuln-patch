package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

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

        double actual;

        actual = f.getPct(1);
        if (Math.abs(actual - 0.25) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:one-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(2));
        if (Math.abs(actual - 0.25) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:two-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(3));
        if (Math.abs(actual - 0.5) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:three-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct((Object) (Integer.valueOf(3)));
        if (Math.abs(actual - 0.5) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:three-object-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct(5);
        if (Math.abs(actual - 0.0) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:five-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getPct("foo");
        if (Math.abs(actual - 0.0) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct(1);
        if (Math.abs(actual - 0.25) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:one-cum-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actual - 0.50) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:two-cum-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actual - 0.50) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:integer-argument-cum-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(3));
        if (Math.abs(actual - 1.0) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:three-cum-pct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(5);
        if (Math.abs(actual - 1.0) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:five-cum-pct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(0);
        if (Math.abs(actual - 0.0) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:zero-cum-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct("foo");
        if (Math.abs(actual - 0.0) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cum-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        try {
            Frequency g = new Frequency();
            int pivot = data.consumeInt(-1000000, 1000000);
            int mutations = data.consumeInt(1, 20);

            g.addValue(pivot);

            for (int i = 0; i < mutations; i++) {
                int v = data.consumeInt(-1000000, 1000000);
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        g.addValue(v);
                        break;
                    case 1:
                        g.addValue((long) v);
                        break;
                    case 2:
                        g.addValue(Integer.valueOf(v));
                        break;
                    case 3:
                        g.addValue((Comparable<?>) Long.valueOf(v));
                        break;
                    default:
                        g.addValue((Object) Integer.valueOf(v));
                        break;
                }
            }

            long sumBefore = g.getSumFreq();
            int hashBefore = g.hashCode();
            String stringBefore = g.toString();

            double lhs = g.getPct((Object) Integer.valueOf(pivot));
            long sumAfter = g.getSumFreq();
            int hashAfter = g.hashCode();
            String stringAfter = g.toString();
            double rhs = g.getPct(Long.valueOf(pivot));

            if (sumBefore != sumAfter || hashBefore != hashAfter || !stringBefore.equals(stringAfter)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:read-only-getPct-object] metamorphic violation: getPct(Object) is a read-only query but observable state changed sumBefore=" +
                    sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter +
                    " toStringBefore=" + stringBefore + " toStringAfter=" + stringAfter);
            }

            if (Math.abs(lhs - rhs) > tolerance) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:object-vs-comparable-pct] metamorphic violation: deprecated getPct(Object) and getPct(Comparable) must agree for the same integral value because the class contract says integers/longs/Integer/Long are not distinguished by type and getPct(Object) is replaced by getPct(Comparable); input=" +
                    pivot + " lhs=" + lhs + " rhs=" + rhs + " sumFreq=" + g.getSumFreq() +
                    " countAsInt=" + g.getCount(Integer.valueOf(pivot)) +
                    " countAsLong=" + g.getCount(Long.valueOf(pivot)));
            }
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            return;
        }
    }
}