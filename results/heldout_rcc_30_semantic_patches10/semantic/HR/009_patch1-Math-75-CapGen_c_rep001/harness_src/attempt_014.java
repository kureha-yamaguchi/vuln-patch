package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Collections;
import java.util.Iterator;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        liftedSeedOracle();
        reverseComparatorMiddleBucketOracle(data);
    }

    private static void liftedSeedOracle() {
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

        requireExact("[oracle:lifted-seed-one-pct]", "one pct", 0.25, f.getPct(1));
        requireExact("[oracle:lifted-seed-two-pct]", "two pct", 0.25, f.getPct(Long.valueOf(2)));
        requireExact("[oracle:lifted-seed-three-pct]", "three pct", 0.5, f.getPct(Long.valueOf(3)));
        requireExact("[oracle:lifted-seed-three-object-pct]", "three (Object) pct", 0.5, f.getPct((Object) Integer.valueOf(3)));
        requireExact("[oracle:lifted-seed-five-pct]", "five pct", 0.0, f.getPct(5));
        requireExact("[oracle:lifted-seed-foo-pct]", "foo pct", 0.0, f.getPct("foo"));
        requireExact("[oracle:lifted-seed-one-cumpct]", "one cum pct", 0.25, f.getCumPct(1));
        requireExact("[oracle:lifted-seed-two-cumpct]", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        requireExact("[oracle:lifted-seed-integer-argument-cumpct]", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        requireExact("[oracle:lifted-seed-three-cumpct]", "three cum pct", 1.0, f.getCumPct(threeL));
        requireExact("[oracle:lifted-seed-five-cumpct]", "five cum pct", 1.0, f.getCumPct(5));
        requireExact("[oracle:lifted-seed-zero-cumpct]", "zero cum pct", 0.0, f.getCumPct(0));
        requireExact("[oracle:lifted-seed-foo-cumpct]", "foo cum pct", 0.0, f.getCumPct("foo"));
    }

    private static void reverseComparatorMiddleBucketOracle(FuzzedDataProvider data) {
        try {
            int base = data.consumeInt(-1000, 1000);
            int lowCount = data.consumeInt(1, 5);
            int midCount = data.consumeInt(1, 5);
            int highCount = data.consumeInt(1, 5);

            Frequency f = new Frequency(Collections.reverseOrder());

            for (int i = 0; i < lowCount; i++) {
                f.addValue(base);
            }
            for (int i = 0; i < midCount; i++) {
                f.addValue(base + 1);
            }
            for (int i = 0; i < highCount; i++) {
                f.addValue(base + 2);
            }

            long sumBefore = f.getSumFreq();
            int hashBefore = f.hashCode();
            String stringBefore = f.toString();

            double actual = f.getPct((Object) Integer.valueOf(base + 1));
            double expected = ((double) midCount) / ((double) (lowCount + midCount + highCount));

            if (!sameDouble(expected, actual)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:reverse-comparator-middle-bucket] semantic mismatch: " +
                    "getPct((Object) middle) must equal the exact middle-bucket proportion even when a Comparator orders values differently; " +
                    "inputBase=" + base +
                    " lowCount=" + lowCount +
                    " midCount=" + midCount +
                    " highCount=" + highCount +
                    " expected=" + expected +
                    " actual=" + actual
                );
            }

            // Documented guarantee: getPct is a read-only query ("Returns the percentage of values...").
            // A throw-deleting / branch-skipping patch must not mutate the receiver while answering.
            if (sumBefore != f.getSumFreq() || hashBefore != f.hashCode() || !stringBefore.equals(f.toString())) {
                throw new RuntimeException(
                    "[oracle:reverse-comparator-readonly] metamorphic violation: getPct(Object) changed observable receiver state " +
                    "base=" + base +
                    " sumBefore=" + sumBefore +
                    " sumAfter=" + f.getSumFreq() +
                    " hashBefore=" + hashBefore +
                    " hashAfter=" + f.hashCode() +
                    " toStringBefore=" + escape(stringBefore) +
                    " toStringAfter=" + escape(f.toString())
                );
            }

            // Independent cross-check on the same state but via the object's own output:
            // valuesIterator() returns the set of values that have been added; with integral inputs they are returned as Longs.
            // Under the supplied reverse comparator, iteration must expose the comparator order, so the middle bucket is the second element.
            Iterator<Comparable<?>> it = f.valuesIterator();
            Comparable<?> first = it.hasNext() ? it.next() : null;
            Comparable<?> second = it.hasNext() ? it.next() : null;
            Comparable<?> third = it.hasNext() ? it.next() : null;
            if (first instanceof Long && second instanceof Long && third instanceof Long) {
                long firstV = ((Long) first).longValue();
                long secondV = ((Long) second).longValue();
                long thirdV = ((Long) third).longValue();
                if (!(firstV == (long) base + 2 && secondV == (long) base + 1 && thirdV == (long) base)) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:reverse-comparator-iterator-order] consistency violation: valuesIterator order disagrees with the installed reverse comparator " +
                        "base=" + base +
                        " first=" + firstV +
                        " second=" + secondV +
                        " third=" + thirdV
                    );
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void requireExact(String oracle, String label, double expected, double actual) {
        if (!sameDouble(expected, actual)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:unnamed-check] " + oracle + " semantic mismatch: " + label + " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static boolean sameDouble(double a, double b) {
        if (Double.isNaN(a)) {
            return Double.isNaN(b);
        }
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}