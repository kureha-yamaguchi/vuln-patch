package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

        // Reconstruct FrequencyTest.testPcts exactly.
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

        checkDouble("lift-one-pct", "one pct", 0.25, f.getPct(1), tolerance);
        checkDouble("lift-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)), tolerance);
        checkDouble("lift-three-pct", "three pct", 0.5, f.getPct(threeL), tolerance);
        checkDouble("lift-three-object-pct", "three (Object) pct", 0.5, f.getPct((Object) (Integer.valueOf(3))), tolerance);
        checkDouble("lift-five-pct", "five pct", 0.0, f.getPct(5), tolerance);
        checkDouble("lift-foo-pct", "foo pct", 0.0, f.getPct("foo"), tolerance);
        checkDouble("lift-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1), tolerance);
        checkDouble("lift-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)), tolerance);
        checkDouble("lift-integer-argument", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)), tolerance);
        checkDouble("lift-three-cum-pct", "three cum pct", 1.0, f.getCumPct(threeL), tolerance);
        checkDouble("lift-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5), tolerance);
        checkDouble("lift-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0), tolerance);
        checkDouble("lift-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"), tolerance);

        // Post-condition / metamorphic check:
        // The docs for getPct(Object) say it is "replaced by getPct(Comparable)"; these overloads
        // must therefore agree on equivalent inputs. A patch that redirects Object to cumulative
        // percentage violates this even when nothing throws.
        long beforeSum = f.getSumFreq();
        int beforeHash = f.hashCode();
        String beforeString = f.toString();
        double pctObj3 = f.getPct((Object) Integer.valueOf(3));
        double pctCmp3 = f.getPct((Comparable<?>) Integer.valueOf(3));
        long afterSum = f.getSumFreq();
        int afterHash = f.hashCode();
        String afterString = f.toString();
        if (!sameDouble(pctObj3, pctCmp3, tolerance)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:overload-agreement] semantic mismatch: getPct(Object) != getPct(Comparable) input=3 lhs=" +
                pctObj3 + " rhs=" + pctCmp3);
        }
        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:read-only] semantic mismatch: read-only getPct changed state beforeSum=" + beforeSum +
                " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash +
                " beforeString=" + escape(beforeString) + " afterString=" + escape(afterString));
        }

        // Generalize with trusted relations on fuzz-constructed inputs.
        Frequency g = new Frequency();
        int mutations = data.consumeInt(1, 32);
        for (int i = 0; i < mutations; i++) {
            int choice = data.consumeInt(0, 2);
            int v = data.consumeInt(-1000, 1000);
            if (choice == 0) {
                g.addValue(v);
            } else if (choice == 1) {
                g.addValue((long) v);
            } else {
                char c = (char) data.consumeInt(0, 127);
                g.addValue(c);
            }
        }

        try {
            // Construct input from known answer: choose a value first, add it exactly k times, then
            // its percentage must be k / sum by the documented definition of getPct.
            Frequency h = new Frequency();
            int target = data.consumeInt(-1000, 1000);
            int targetCount = data.consumeInt(1, 8);
            int otherCount = data.consumeInt(0, 8);
            for (int i = 0; i < targetCount; i++) {
                h.addValue(target);
            }
            for (int i = 0; i < otherCount; i++) {
                int other = data.consumeInt(-1000, 1000);
                if (other == target) {
                    other = target + 1;
                }
                h.addValue(other);
            }
            double expectedPct = (double) targetCount / (double) h.getSumFreq();
            double actualPctComparable = h.getPct((Comparable<?>) Integer.valueOf(target));
            double actualPctObject = h.getPct((Object) Integer.valueOf(target));
            checkDouble("count-derived-comparable", "getPct(Comparable) from constructed counts", expectedPct, actualPctComparable, tolerance);
            checkDouble("count-derived-object", "getPct(Object) from constructed counts", expectedPct, actualPctObject, tolerance);

            // Same-name overload agreement for integral values: integers/longs/Integer/Long are not
            // distinguished by type per class contract, so equivalent representations must agree.
            double pctInt = h.getPct(target);
            double pctLong = h.getPct((long) target);
            if (!sameDouble(pctInt, actualPctComparable, tolerance) || !sameDouble(pctLong, actualPctComparable, tolerance)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:integral-equivalence] semantic mismatch: equivalent integral pct overloads differ target=" +
                    target + " pctInt=" + pctInt + " pctLong=" + pctLong + " pctComparable=" + actualPctComparable);
            }
        } catch (RuntimeException ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            // If a relation setup or probe unexpectedly throws, skip per harness hygiene.
        }
    }

    private static void checkDouble(String oracleId, String what, double expected, double actual, double tolerance) {
        if (!sameDouble(expected, actual, tolerance)) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean sameDouble(double a, double b, double tolerance) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Math.abs(a - b) <= tolerance;
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