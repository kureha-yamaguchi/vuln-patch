package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Iterator;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        /*
         * Independent metamorphic oracle, outside the already-covered lifted checks:
         * By contract, getPct(v) is the proportion equal to v, while getCumPct(v) is
         * the proportion <= v. For an all-integral Frequency and any integer k,
         * proportion(equal k) == proportion(<= k) - proportion(<= k-1).
         * A "fix" that merely hides one top-level symptom but leaves the same root
         * cause in getPct(Object) will still violate this relation.
         */
        try {
            Frequency g = new Frequency();
            int base = data.consumeInt(-1000, 1000);
            int lowerCopies = data.consumeInt(1, 4);
            int baseCopies = data.consumeInt(1, 4);
            int upperCopies = data.consumeInt(1, 4);

            for (int i = 0; i < lowerCopies; i++) {
                g.addValue(base - 1);
            }
            for (int i = 0; i < baseCopies; i++) {
                g.addValue(base);
            }
            for (int i = 0; i < upperCopies; i++) {
                g.addValue(base + 1);
            }

            double pctObject = g.getPct((Object) Integer.valueOf(base));
            double cumHere = g.getCumPct(base);
            double cumPrev = g.getCumPct(base - 1);
            double discreteMass = cumHere - cumPrev;

            if (Math.abs(pctObject - discreteMass) > tolerance) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:object-pct-equals-cum-delta] metamorphic violation: getPct((Object)Integer.valueOf(k)) must equal getCumPct(k)-getCumPct(k-1) for integral data"
                    + " k=" + base
                    + " pctObject=" + pctObject
                    + " cumHere=" + cumHere
                    + " cumPrev=" + cumPrev
                    + " discreteMass=" + discreteMass
                );
            }

            /*
             * Consistency cross-check on a reachable helper:
             * getCumFreq(k) must equal the cumulative count recomputed from the
             * object's own sorted valuesIterator() output and getCount(value).
             */
            long manualCumFreq = 0L;
            Iterator<Comparable<?>> it = g.valuesIterator();
            while (it.hasNext()) {
                Comparable<?> value = it.next();
                @SuppressWarnings("unchecked")
                Comparable<Object> cmp = (Comparable<Object>) value;
                if (cmp.compareTo(Long.valueOf(base)) <= 0) {
                    manualCumFreq += g.getCount(value);
                }
            }
            long reportedCumFreq = g.getCumFreq(base);
            if (reportedCumFreq != manualCumFreq) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:cumfreq-from-iterator-recompute] consistency violation: reported cumulative frequency disagrees with recomputation"
                    + " k=" + base
                    + " reportedCumFreq=" + reportedCumFreq
                    + " manualCumFreq=" + manualCumFreq
                );
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        /*
         * Reconstruct the exact failing test fixture and lifted assertions.
         * This reaches the patched getPct(Object) through the real public API.
         */
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

        if (Math.abs(f.getPct(1) - 0.25d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-one-pct-again] semantic mismatch: expected=0.25 actual=" + f.getPct(1));
        }
        if (Math.abs(f.getPct(Long.valueOf(2)) - 0.25d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-two-pct-again] semantic mismatch: expected=0.25 actual=" + f.getPct(Long.valueOf(2)));
        }
        if (Math.abs(f.getPct(threeL) - 0.5d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-three-pct-again] semantic mismatch: expected=0.5 actual=" + f.getPct(threeL));
        }
        if (Math.abs(f.getPct((Object) (Integer.valueOf(3))) - 0.5d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-three-object-pct-again] semantic mismatch: expected=0.5 actual=" + f.getPct((Object) (Integer.valueOf(3))));
        }
        if (Math.abs(f.getPct(5) - 0.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-five-pct-again] semantic mismatch: expected=0.0 actual=" + f.getPct(5));
        }
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-foo-pct-again] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct(1) - 0.25d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-one-cumpct-again] semantic mismatch: expected=0.25 actual=" + f.getCumPct(1));
        }
        if (Math.abs(f.getCumPct(Long.valueOf(2)) - 0.50d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-two-cumpct-again] semantic mismatch: expected=0.5 actual=" + f.getCumPct(Long.valueOf(2)));
        }
        if (Math.abs(f.getCumPct(Integer.valueOf(2)) - 0.50d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-integer-arg-cumpct-again] semantic mismatch: expected=0.5 actual=" + f.getCumPct(Integer.valueOf(2)));
        }
        if (Math.abs(f.getCumPct(threeL) - 1.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-three-cumpct-again] semantic mismatch: expected=1.0 actual=" + f.getCumPct(threeL));
        }
        if (Math.abs(f.getCumPct(5) - 1.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-five-cumpct-again] semantic mismatch: expected=1.0 actual=" + f.getCumPct(5));
        }
        if (Math.abs(f.getCumPct(0) - 0.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-zero-cumpct-again] semantic mismatch: expected=0.0 actual=" + f.getCumPct(0));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-foo-cumpct-again] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }
    }
}