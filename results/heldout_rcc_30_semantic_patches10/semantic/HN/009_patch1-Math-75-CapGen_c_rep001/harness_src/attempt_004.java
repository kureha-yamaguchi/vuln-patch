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

        long sumBeforeReads = f.getSumFreq();
        String toStringBeforeReads = f.toString();
        int hashBeforeReads = f.hashCode();

        assertApprox("one pct", 0.25, f.getPct(1), tolerance, "lifted-one-pct");
        assertApprox("two pct", 0.25, f.getPct(Long.valueOf(2)), tolerance, "lifted-two-pct");
        assertApprox("three pct", 0.5, f.getPct(threeL), tolerance, "lifted-three-pct");
        assertApprox("three (Object) pct", 0.5, f.getPct((Object) (Integer.valueOf(3))), tolerance, "lifted-three-object-pct");
        assertApprox("five pct", 0.0, f.getPct(5), tolerance, "lifted-five-pct");
        assertApprox("foo pct", 0.0, f.getPct("foo"), tolerance, "lifted-foo-pct");
        assertApprox("one cum pct", 0.25, f.getCumPct(1), tolerance, "lifted-one-cum-pct");
        assertApprox("two cum pct", 0.50, f.getCumPct(Long.valueOf(2)), tolerance, "lifted-two-cum-pct");
        assertApprox("Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)), tolerance, "lifted-integer-cum-pct");
        assertApprox("three cum pct", 1.0, f.getCumPct(threeL), tolerance, "lifted-three-cum-pct");
        assertApprox("five cum pct", 1.0, f.getCumPct(5), tolerance, "lifted-five-cum-pct");
        assertApprox("zero cum pct", 0.0, f.getCumPct(0), tolerance, "lifted-zero-cum-pct");
        assertApprox("foo cum pct", 0.0, f.getCumPct("foo"), tolerance, "lifted-foo-cum-pct");

        assertLongEquals("read-only get* methods must not change sumFreq", sumBeforeReads, f.getSumFreq(), "read-only-sum");
        assertStringEquals("read-only get* methods must not change toString", toStringBeforeReads, f.toString(), "read-only-string");
        assertIntEquals("read-only get* methods must not change hashCode", hashBeforeReads, f.hashCode(), "read-only-hash");

        try {
            int low = data.consumeInt(-1000, 999);
            int gap = data.consumeInt(1, 1000);
            int high = low + gap;
            int lowCount = data.consumeInt(1, 20);
            int highCount = data.consumeInt(1, 20);

            Frequency g = new Frequency();

            for (int i = 0; i < lowCount; i++) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        g.addValue(low);
                        break;
                    case 1:
                        g.addValue((long) low);
                        break;
                    default:
                        g.addValue(Integer.valueOf(low));
                        break;
                }
                // Contract: absent incomparable String query returns 0 for getPct/getCumPct when values are present and String is not comparable to numeric values.
                assertApprox("reprobe absent foo pct after mutation", 0.0, g.getPct("foo"), tolerance, "reprobe-foo-pct");
                assertApprox("reprobe absent foo cum pct after mutation", 0.0, g.getCumPct("foo"), tolerance, "reprobe-foo-cum-pct");
            }

            for (int i = 0; i < highCount; i++) {
                switch (data.consumeInt(0, 2)) {
                    case 0:
                        g.addValue(high);
                        break;
                    case 1:
                        g.addValue((long) high);
                        break;
                    default:
                        g.addValue(Integer.valueOf(high));
                        break;
                }
                // Same documented rejection as above must remain 0 after every state change, independent of container contents.
                assertApprox("reprobe absent foo pct after mutation", 0.0, g.getPct("foo"), tolerance, "reprobe-foo-pct");
                assertApprox("reprobe absent foo cum pct after mutation", 0.0, g.getCumPct("foo"), tolerance, "reprobe-foo-cum-pct");
            }

            long sumBeforeMeta = g.getSumFreq();
            int hashBeforeMeta = g.hashCode();
            String stringBeforeMeta = g.toString();

            double pctObject = g.getPct((Object) Integer.valueOf(low));
            double pctComparable = g.getPct((Comparable<?>) Integer.valueOf(low));
            double expectedPct = ((double) lowCount) / ((double) (lowCount + highCount));

            // Contract: deprecated getPct(Object) is replaced by getPct(Comparable), so equivalent Integer inputs must produce the same percentage.
            if (!approxEqual(pctObject, pctComparable, tolerance)) {
                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: getPct(Object)==getPct(Comparable) input=" + low + " lhs=" + pctObject + " rhs=" + pctComparable);
            }

            // Contract: getPct returns the proportion of values equal to v; here we constructed exactly lowCount copies of low out of lowCount+highCount total.
            if (!approxEqual(pctObject, expectedPct, tolerance)) {
                throw new RuntimeException("[oracle:constructed-answer] metamorphic violation: constructed percentage mismatch input=" + low + " lhs=" + pctObject + " rhs=" + expectedPct);
            }

            // Contract: get* methods are read-only accessors; they must not mutate observable state.
            if (sumBeforeMeta != g.getSumFreq()) {
                throw new RuntimeException("[oracle:meta-read-only-sum] metamorphic violation: sum changed after reads input=" + low + " lhs=" + sumBeforeMeta + " rhs=" + g.getSumFreq());
            }
            if (hashBeforeMeta != g.hashCode()) {
                throw new RuntimeException("[oracle:meta-read-only-hash] metamorphic violation: hash changed after reads input=" + low + " lhs=" + hashBeforeMeta + " rhs=" + g.hashCode());
            }
            if (!stringBeforeMeta.equals(g.toString())) {
                throw new RuntimeException("[oracle:meta-read-only-string] metamorphic violation: string changed after reads input=" + low + " lhs=" + escape(stringBeforeMeta) + " rhs=" + escape(g.toString()));
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static boolean approxEqual(double expected, double actual, double tolerance) {
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            return Double.isNaN(expected) && Double.isNaN(actual);
        }
        return Math.abs(expected - actual) <= tolerance;
    }

    private static void assertApprox(String what, double expected, double actual, double tolerance, String oracleId) {
        if (!approxEqual(expected, actual, tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void assertLongEquals(String what, long expected, long actual, String oracleId) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void assertIntEquals(String what, int expected, int actual, String oracleId) {
        if (expected != actual) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual
            );
        }
    }

    private static void assertStringEquals(String what, String expected, String actual, String oracleId) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + escape(expected) + " actual=" + escape(actual)
            );
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}