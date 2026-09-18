package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Iterator;

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
        if (!(Math.abs(0.25 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-one-pct-exact] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(2));
        if (!(Math.abs(0.25 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-two-pct-exact] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(threeL);
        if (!(Math.abs(0.5 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-three-pct-exact] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct((Object) (Integer.valueOf(3)));
        if (!(Math.abs(0.5 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-three-object-pct-exact] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct(5);
        if (!(Math.abs(0.0 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-five-pct-exact] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getPct("foo");
        if (!(Math.abs(0.0 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-foo-pct-exact] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct(1);
        if (!(Math.abs(0.25 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-one-cumpct-exact] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(2));
        if (!(Math.abs(0.50 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-two-cumpct-exact] semantic mismatch: expected=0.50 actual=" + actual);
        }

        actual = f.getCumPct(Integer.valueOf(2));
        if (!(Math.abs(0.50 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-integer-argument-cumpct-exact] semantic mismatch: expected=0.50 actual=" + actual);
        }

        actual = f.getCumPct(threeL);
        if (!(Math.abs(1.0 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-three-cumpct-exact] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(5);
        if (!(Math.abs(1.0 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-five-cumpct-exact] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(0);
        if (!(Math.abs(0.0 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-zero-cumpct-exact] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct("foo");
        if (!(Math.abs(0.0 - actual) <= tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:lifted-foo-cumpct-exact] semantic mismatch: expected=0.0 actual=" + actual);
        }

        boolean relationMismatch = false;
        String relationMessage = null;
        try {
            Frequency g = new Frequency();

            int lower = data.consumeInt(-1000, 999);
            int upper = data.consumeInt(lower + 1, 1000);

            if (data.consumeBoolean()) {
                g.addValue(lower);
            } else {
                g.addValue((long) lower);
            }

            if (data.consumeBoolean()) {
                g.addValue(upper);
            } else {
                g.addValue((long) upper);
            }

            int extra = data.consumeInt(0, 6);
            for (int i = 0; i < extra; i++) {
                int v = data.consumeInt(lower, upper);
                if (data.consumeBoolean()) {
                    g.addValue(v);
                } else {
                    g.addValue((long) v);
                }
            }

            Comparable<?> prev = null;
            Comparable<?> current = null;
            Iterator<Comparable<?>> it = g.valuesIterator();
            while (it.hasNext()) {
                prev = current;
                current = it.next();
            }

            if (!(current instanceof Comparable) || prev == null) {
                return;
            }

            long sumFreq = g.getSumFreq();
            long cumFreqAtMax = g.getCumFreq(current);
            double cumPctAtPrev = g.getCumPct(prev);
            double pctAtMaxViaObject = g.getPct((Object) Integer.valueOf(upper));
            double expectedPctAtMax = 1.0 - cumPctAtPrev;

            // Contract justification:
            // getPct(v) returns the proportion equal to v, while getCumPct(v) returns the proportion <= v.
            // For the maximum present value max, getCumFreq(max) must equal getSumFreq(), so getCumPct(max) == 1.0.
            // Therefore the percentage of the maximum bucket is exactly the remaining mass after the previous distinct bucket:
            // getPct((Object) max) == 1.0 - getCumPct(previousDistinct).
            // A band-aid that merely redirects getPct(Object) to cumulative percentage violates this post-condition.
            if (sumFreq != cumFreqAtMax) {
                relationMismatch = true;
                relationMessage =
                    "[oracle:max-cumfreq-equals-sumfreq] consistency violation: max=" + current +
                    " sumFreq=" + sumFreq + " cumFreqAtMax=" + cumFreqAtMax;
            } else {
                double relTol = 1e-12 * Math.max(1.0, Math.max(Math.abs(expectedPctAtMax), Math.abs(pctAtMaxViaObject)));
                if (!(Math.abs(expectedPctAtMax - pctAtMaxViaObject) <= relTol)) {
                    relationMismatch = true;
                    relationMessage =
                        "[oracle:max-pct-from-previous-cumpct] consistency violation: max=" + upper +
                        " previousDistinct=" + prev +
                        " expectedPctAtMax=" + expectedPctAtMax +
                        " actualPctAtMax=" + pctAtMaxViaObject +
                        " cumPctAtPrevious=" + cumPctAtPrev +
                        " sumFreq=" + sumFreq +
                        " cumFreqAtMax=" + cumFreqAtMax;
                }
            }
        } catch (Throwable t) {
            return;
        }

        if (relationMismatch) {
            throw new FuzzerSecurityIssueLow(relationMessage);
        }
    }
}