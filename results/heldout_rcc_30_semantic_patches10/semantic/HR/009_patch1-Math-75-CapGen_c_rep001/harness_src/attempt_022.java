package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.util.Iterator;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        checkSeedGroundTruth();
        auditCloneRecomputedObjectPct(data);
    }

    private static void checkSeedGroundTruth() {
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

        double actual = f.getPct((Object) (Integer.valueOf(3)));
        if (Double.doubleToLongBits(actual) != Double.doubleToLongBits(0.5d)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:seed-ground-truth] semantic mismatch: f.getPct((Object)(Integer.valueOf(3))) expected=0.5 actual=" + actual
            );
        }
    }

    private static void auditCloneRecomputedObjectPct(FuzzedDataProvider data) {
        try {
            int base = data.consumeInt(-1000, 1000);
            int gap1 = data.consumeInt(1, 20);
            int gap2 = data.consumeInt(1, 20);
            long a = base;
            long b = base + gap1;
            long c = base + gap1 + gap2;

            int countA = data.consumeInt(1, 4);
            int countB = data.consumeInt(1, 4);
            int countC = data.consumeInt(1, 4);

            Frequency original = new Frequency();

            addRepeatedMixedIntegral(original, a, countA, data.consumeBoolean());
            addRepeatedMixedIntegral(original, b, countB, data.consumeBoolean());
            addRepeatedMixedIntegral(original, c, countC, data.consumeBoolean());

            long sumBefore = original.getSumFreq();
            int hashBefore = original.hashCode();
            String textBefore = original.toString();

            double buggyPathValue = original.getPct((Object) Integer.valueOf((int) b));

            long sumAfter = original.getSumFreq();
            int hashAfter = original.hashCode();
            String textAfter = original.toString();

            if (sumBefore != sumAfter || hashBefore != hashAfter || !textBefore.equals(textAfter)) {
                throw new RuntimeException(
                    "[oracle:object-query-readonly-clone-axis] metamorphic violation: getPct(Object) is documented as a read-only query, but public reader state changed sumBefore="
                        + sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                        + " textBefore=" + escapeOneLine(textBefore) + " textAfter=" + escapeOneLine(textAfter)
                );
            }

            Frequency rebuilt = new Frequency();
            Iterator<Comparable<?>> it = original.valuesIterator();
            while (it.hasNext()) {
                Comparable<?> value = it.next();
                long freq = original.getCount(value);
                for (long i = 0; i < freq; i++) {
                    rebuilt.addValue(value);
                }
            }

            long rebuiltSum = rebuilt.getSumFreq();
            long rebuiltCountAtB = rebuilt.getCount(Long.valueOf(b));
            double independentMass = (double) rebuiltCountAtB / (double) rebuiltSum;

            /* Contract justification:
             * - valuesIterator() returns the set of values that have been added; for integral inputs they are normalized to Longs.
             * - getCount(v) returns the frequency of v.
             * - getSumFreq() returns the total frequency count.
             * Replaying each iterator value exactly getCount(value) times into a fresh Frequency reconstructs the same distribution.
             * Therefore rebuiltCountAtB / rebuiltSum is an independent recomputation of the proportion of values equal to b.
             * getPct(Object) is documented to return "the percentage of values that are equal to v", so it must match this recomputed mass.
             * A band-aid that only masks one symptom but leaves the object-overload routed to cumulative percentage will still violate this equality.
             */
            if (Double.doubleToLongBits(buggyPathValue) != Double.doubleToLongBits(independentMass)) {
                throw new RuntimeException(
                    "[oracle:clone-recomputed-middle-object-mass] metamorphic violation: deprecated getPct(Object) disagrees with independently rebuilt empirical mass query="
                        + b + " objectPct=" + buggyPathValue + " rebuiltCount=" + rebuiltCountAtB + " rebuiltSum=" + rebuiltSum
                        + " rebuiltMass=" + independentMass + " originalSum=" + original.getSumFreq()
                );
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void addRepeatedMixedIntegral(Frequency f, long value, int times, boolean preferObject) {
        for (int i = 0; i < times; i++) {
            if (preferObject) {
                f.addValue((Object) Integer.valueOf((int) value));
            } else {
                switch (i % 3) {
                    case 0:
                        f.addValue((int) value);
                        break;
                    case 1:
                        f.addValue(Long.valueOf(value));
                        break;
                    default:
                        f.addValue(value);
                        break;
                }
            }
        }
    }

    private static String escapeOneLine(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}