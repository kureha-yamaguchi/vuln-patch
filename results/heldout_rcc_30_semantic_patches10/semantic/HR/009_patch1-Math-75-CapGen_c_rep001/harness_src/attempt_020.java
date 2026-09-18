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

        double actual;

        actual = f.getPct(1);
        if (Math.abs(actual - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-one-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(2));
        if (Math.abs(actual - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-two-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(threeL);
        if (Math.abs(actual - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-three-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct((Object) (Integer.valueOf(3)));
        if (Math.abs(actual - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-three-object-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct(5);
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-five-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getPct("foo");
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-foo-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct(1);
        if (Math.abs(actual - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-one-cumpct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actual - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-two-cumpct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actual - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-integer-argument-cumpct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(threeL);
        if (Math.abs(actual - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-three-cumpct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(5);
        if (Math.abs(actual - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-five-cumpct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(0);
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-zero-cumpct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct("foo");
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-copy-foo-cumpct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        int base = data.consumeInt(-1_000_000, 999_997);
        int mid = base + 1;
        int high = base + 2;

        Frequency shifted = new Frequency();
        shifted.addValue((long) base);
        shifted.addValue((long) mid);
        shifted.addValue(base);
        shifted.addValue(mid);
        shifted.addValue((long) high);
        shifted.addValue((long) high);
        shifted.addValue(high);
        shifted.addValue(Integer.valueOf(high));

        try {
            double shiftedPct = shifted.getPct((Object) Integer.valueOf(mid));
            double shiftedCumPct = shifted.getCumPct(Integer.valueOf(mid));
            long shiftedCount = shifted.getCount(Integer.valueOf(mid));
            long shiftedSum = shifted.getSumFreq();

            if (shiftedCount != 2L || shiftedSum != 8L) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:translated-shape-sanity] semantic mismatch: expectedCount=2 expectedSum=8 actualCount="
                        + shiftedCount + " actualSum=" + shiftedSum);
            }

            if (Math.abs(shiftedPct - 0.25d) > tolerance) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:translated-middle-object-pct] metamorphic violation: constructed distribution fixes the answer for the middle bucket to 2/8=0.25, but getPct((Object)Integer) returned "
                        + shiftedPct + " for base=" + base + " mid=" + mid + " high=" + high);
            }

            if (Math.abs(shiftedCumPct - 0.50d) > tolerance) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:translated-middle-cumpct] semantic mismatch: expected=0.5 actual=" + shiftedCumPct
                        + " base=" + base + " mid=" + mid + " high=" + high);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            // Frequency documents that getPct returns the proportion equal to v, and integral
            // values are not distinguished by type. A throw-deleting patch that still computes a
            // cumulative proportion would break this equality for the translated middle bucket.
            double lhs = shifted.getPct((Object) Integer.valueOf(mid));
            double rhs = shifted.getPct((long) mid);
            if (Math.abs(lhs - rhs) > tolerance) {
                throw new RuntimeException(
                    "[oracle:translated-object-long-agree-mid] metamorphic violation: equivalent integral inputs must have the same percentage input="
                        + mid + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}