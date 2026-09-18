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

        assertClose("seed-one-pct", 0.25, f.getPct(1), tolerance, "one pct");
        assertClose("seed-two-pct", 0.25, f.getPct(Long.valueOf(2)), tolerance, "two pct");
        assertClose("seed-three-pct", 0.5, f.getPct(Long.valueOf(3)), tolerance, "three pct");
        assertClose("seed-three-object-pct", 0.5, f.getPct((Object) (Integer.valueOf(3))), tolerance, "three (Object) pct");
        assertClose("seed-five-pct", 0.0, f.getPct(5), tolerance, "five pct");
        assertClose("seed-foo-pct", 0.0, f.getPct("foo"), tolerance, "foo pct");
        assertClose("seed-one-cum-pct", 0.25, f.getCumPct(1), tolerance, "one cum pct");
        assertClose("seed-two-cum-pct", 0.50, f.getCumPct(Long.valueOf(2)), tolerance, "two cum pct");
        assertClose("seed-integer-argument-cum-pct", 0.50, f.getCumPct(Integer.valueOf(2)), tolerance, "Integer argument");
        assertClose("seed-three-cum-pct", 1.0, f.getCumPct(Long.valueOf(3)), tolerance, "three cum pct");
        assertClose("seed-five-cum-pct", 1.0, f.getCumPct(5), tolerance, "five cum pct");
        assertClose("seed-zero-cum-pct", 0.0, f.getCumPct(0), tolerance, "zero cum pct");
        assertClose("seed-foo-cum-pct", 0.0, f.getCumPct("foo"), tolerance, "foo cum pct");

        // Documented guarantee: getPct(Object) is deprecated and replaced by getPct(Comparable),
        // so for Comparable inputs both overloads must agree. Also get* methods are read-only
        // accessors, so calling getPct(Object) must not change observable state.
        probePctOverloadAgreementAndReadOnlyState(f, Integer.valueOf(3), "seed-overload");

        Frequency g = new Frequency();
        boolean useStrings = data.consumeBoolean();
        int steps = data.consumeInt(1, 8);

        for (int i = 0; i < steps; i++) {
            if (useStrings) {
                String v = data.consumeAsciiString(8);
                if (v.length() == 0) {
                    v = "a";
                }
                try {
                    g.addValue(v);
                } catch (Throwable t) {
                    return;
                }

                String q = data.consumeAsciiString(8);
                if (q.length() == 0) {
                    q = "b";
                }
                try {
                    probePctOverloadAgreementAndReadOnlyState(g, q, "fuzz-string-step-" + i);
                } catch (Throwable t) {
                    return;
                }
            } else {
                Integer v = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                try {
                    g.addValue(v);
                } catch (Throwable t) {
                    return;
                }

                Integer q = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                try {
                    probePctOverloadAgreementAndReadOnlyState(g, q, "fuzz-int-step-" + i);
                } catch (Throwable t) {
                    return;
                }
            }
        }

        if (data.consumeBoolean()) {
            g.clear();
            try {
                if (useStrings) {
                    String q = data.consumeAsciiString(8);
                    if (q.length() == 0) {
                        q = "c";
                    }
                    probePctOverloadAgreementAndReadOnlyState(g, q, "fuzz-after-clear");
                } else {
                    Integer q = Integer.valueOf(data.consumeInt(-1000000, 1000000));
                    probePctOverloadAgreementAndReadOnlyState(g, q, "fuzz-after-clear");
                }
            } catch (Throwable t) {
                return;
            }
        }
    }

    private static void probePctOverloadAgreementAndReadOnlyState(Frequency freq, Comparable<?> query, String oracleBase) {
        long beforeSum = freq.getSumFreq();
        int beforeHash = freq.hashCode();
        String beforeString = freq.toString();

        double lhs = freq.getPct((Object) query);

        long afterSum = freq.getSumFreq();
        int afterHash = freq.hashCode();
        String afterString = freq.toString();

        if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
            throw new RuntimeException(
                "[oracle:" + oracleBase + "-readonly] metamorphic violation: getPct(Object) changed read-only state"
                    + " query=" + String.valueOf(query)
                    + " beforeSum=" + beforeSum
                    + " afterSum=" + afterSum
                    + " beforeHash=" + beforeHash
                    + " afterHash=" + afterHash
                    + " beforeToString=" + escape(beforeString)
                    + " afterToString=" + escape(afterString));
        }

        double rhs = freq.getPct(query);
        if (!sameDouble(lhs, rhs, 10E-15)) {
            throw new RuntimeException(
                "[oracle:" + oracleBase + "-agreement] metamorphic violation: getPct(Object) != getPct(Comparable)"
                    + " query=" + String.valueOf(query)
                    + " lhs=" + lhs
                    + " rhs=" + rhs);
        }
    }

    private static void assertClose(String oracleId, double expected, double actual, double tolerance, String label) {
        if (!sameDouble(expected, actual, tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:" + oracleId + "] semantic mismatch: " + label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean sameDouble(double a, double b, double tolerance) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Math.abs(a - b) <= tolerance;
    }

    private static boolean safeEquals(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String escape(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}