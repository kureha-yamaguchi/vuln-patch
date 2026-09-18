package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        Frequency f = seededFrequency();

        liftedTestPctsOracles(f);
        equivalentObjectIntegerOracles(f);
        insertionOrderInvarianceCheck(data);
    }

    private static Frequency seededFrequency() {
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
        return f;
    }

    private static void liftedTestPctsOracles(Frequency f) {
        double tolerance = 10E-15;
        long threeL = 3;
        int threeI = 3;

        requireDouble("lifted-one-pct-seed-b", 0.25, f.getPct(1), tolerance, "f.getPct(1)");
        requireDouble("lifted-two-pct-seed-b", 0.25, f.getPct(Long.valueOf(2)), tolerance, "f.getPct(Long.valueOf(2))");
        requireDouble("lifted-three-pct-seed-b", 0.5, f.getPct(threeL), tolerance, "f.getPct(threeL)");
        requireDouble("lifted-three-object-pct-seed-b", 0.5, f.getPct((Object) (Integer.valueOf(3))), tolerance, "f.getPct((Object) Integer.valueOf(3))");
        requireDouble("lifted-five-pct-seed-b", 0.0, f.getPct(5), tolerance, "f.getPct(5)");
        requireDouble("lifted-foo-pct-seed-b", 0.0, f.getPct("foo"), tolerance, "f.getPct(\"foo\")");
        requireDouble("lifted-one-cum-pct-seed-b", 0.25, f.getCumPct(1), tolerance, "f.getCumPct(1)");
        requireDouble("lifted-two-cum-pct-seed-b", 0.50, f.getCumPct(Long.valueOf(2)), tolerance, "f.getCumPct(Long.valueOf(2))");
        requireDouble("lifted-integer-argument-cum-pct-seed-b", 0.50, f.getCumPct(Integer.valueOf(2)), tolerance, "f.getCumPct(Integer.valueOf(2))");
        requireDouble("lifted-three-cum-pct-seed-b", 1.0, f.getCumPct(threeL), tolerance, "f.getCumPct(threeL)");
        requireDouble("lifted-five-cum-pct-seed-b", 1.0, f.getCumPct(5), tolerance, "f.getCumPct(5)");
        requireDouble("lifted-zero-cum-pct-seed-b", 0.0, f.getCumPct(0), tolerance, "f.getCumPct(0)");
        requireDouble("lifted-foo-cum-pct-seed-b", 0.0, f.getCumPct("foo"), tolerance, "f.getCumPct(\"foo\")");

        // Same seeded fixture as the test. This is an extra trusted equivalence oracle:
        // integer values are documented not to be distinguished by type, so an explicit Object
        // Integer query for logical value 2 must match the lifted exact value already pinned for 2.
        requireDouble("variant-object-two-exact", 0.25, f.getPct((Object) Integer.valueOf(2)), tolerance,
                "f.getPct((Object) Integer.valueOf(2))");

        // Another exact equivalent-input oracle on the same seeded fixture:
        // absent logical value 5 must still have percentage 0 through the Object overload.
        requireDouble("variant-object-five-exact", 0.0, f.getPct((Object) Integer.valueOf(5)), tolerance,
                "f.getPct((Object) Integer.valueOf(5))");

        // A second distinct present-value check via the Object overload.
        requireDouble("variant-object-threei-exact", 0.5, f.getPct((Object) Integer.valueOf(threeI)), tolerance,
                "f.getPct((Object) Integer.valueOf(threeI))");
    }

    private static void equivalentObjectIntegerOracles(Frequency f) {
        double tolerance = 10E-15;

        // Contract guarantee used: integer values are not distinguished by type.
        // Therefore all overload routes representing logical value 2 must agree.
        double objectTwo = f.getPct((Object) Integer.valueOf(2));
        double comparableTwo = f.getPct((Comparable<?>) Integer.valueOf(2));
        requireDouble("equiv-object-vs-comparable-two", comparableTwo, objectTwo, tolerance,
                "getPct((Object)2) vs getPct((Comparable<?>)2)");

        // Contract guarantee used: percentage of equal values is count / total when non-empty.
        // This is an independently obtained quantity for the same logical value 2 on the seeded object.
        long countTwo = f.getCount(2);
        long sum = f.getSumFreq();
        double recomputed = (double) countTwo / (double) sum;
        requireDouble("equiv-object-vs-recomputed-two", recomputed, objectTwo, tolerance,
                "getPct((Object)2) vs getCount(2)/getSumFreq()");
    }

    private static void insertionOrderInvarianceCheck(FuzzedDataProvider data) {
        int m = data.consumeInt(1, 8);
        int[] values = new int[m];
        boolean[] firstAsInt = new boolean[m];
        for (int i = 0; i < m; i++) {
            values[i] = data.consumeInt(-1000, 1000);
            firstAsInt[i] = data.consumeBoolean();
        }
        int query = data.consumeInt(-1000, 1000);

        Frequency f1 = new Frequency();
        Frequency f2 = new Frequency();

        try {
            for (int i = 0; i < m; i++) {
                if (firstAsInt[i]) {
                    f1.addValue(values[i]);
                } else {
                    f1.addValue((long) values[i]);
                }
            }
            for (int i = m - 1; i >= 0; i--) {
                if (firstAsInt[i]) {
                    f2.addValue((long) values[i]);
                } else {
                    f2.addValue(values[i]);
                }
            }
        } catch (RuntimeException e) {
            return;
        }

        double pct1;
        double pct2;
        try {
            // Contract guarantee used: Frequency summarizes a multiset of values, so insertion
            // order and primitive/wrapper route used to add the same integral multiset cannot
            // change the reported percentage for the same logical query value.
            pct1 = f1.getPct((Object) Integer.valueOf(query));
            pct2 = f2.getPct((Object) Integer.valueOf(query));
        } catch (RuntimeException e) {
            return;
        }

        double tolerance = 10E-15;
        if (!closeEnough(pct1, pct2, tolerance)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:order-invariance-object-pct] consistency violation: query=" + query
                            + " pct1=" + pct1 + " pct2=" + pct2 + " sum1=" + f1.getSumFreq()
                            + " sum2=" + f2.getSumFreq());
        }

        long sumBefore = f1.getSumFreq();
        int hashBefore = f1.hashCode();
        String strBefore = f1.toString();
        try {
            // Contract guarantee used: getPct is a getter, so a throw-deleting or stateful
            // band-aid would violate read-only behaviour observable through public readers.
            f1.getPct((Object) Integer.valueOf(query));
        } catch (RuntimeException e) {
            return;
        }
        long sumAfter = f1.getSumFreq();
        int hashAfter = f1.hashCode();
        String strAfter = f1.toString();
        if (sumBefore != sumAfter || hashBefore != hashAfter || !strBefore.equals(strAfter)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:readonly-axis-public-readers] consistency violation: query=" + query
                            + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter
                            + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                            + " strChanged=" + (!strBefore.equals(strAfter)));
        }
    }

    private static void requireDouble(String oracleId, double expected, double actual, double tolerance, String what) {
        if (!closeEnough(expected, actual, tolerance)) {
            throw new FuzzerSecurityIssueLow(
                    "[oracle:" + oracleId + "] semantic mismatch: " + what
                            + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean closeEnough(double expected, double actual, double tolerance) {
        if (Double.doubleToLongBits(expected) == Double.doubleToLongBits(actual)) {
            return true;
        }
        return Math.abs(expected - actual) <= tolerance;
    }
}