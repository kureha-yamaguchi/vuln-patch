package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final long oneL = 1L;
        final long twoL = 2L;
        final long threeL = 3L;
        final int oneI = 1;
        final int twoI = 2;
        final int threeI = 3;

        Frequency f = new Frequency();

        f.addValue(oneL);
        f.addValue(twoL);
        f.addValue(oneI);
        f.addValue(twoI);
        f.addValue(threeL);
        f.addValue(threeL);
        f.addValue(3);
        f.addValue(threeI);

        assertApprox("lifted-one-pct", "one pct", 0.25d, f.getPct(1));
        assertApprox("lifted-two-pct", "two pct", 0.25d, f.getPct(Long.valueOf(2)));
        assertApprox("lifted-three-pct", "three pct", 0.5d, f.getPct(threeL));
        assertApprox("lifted-three-object-pct", "three (Object) pct", 0.5d, f.getPct((Object) (Integer.valueOf(3))));
        assertApprox("lifted-five-pct", "five pct", 0.0d, f.getPct(5));
        assertApprox("lifted-foo-pct", "foo pct", 0.0d, f.getPct("foo"));
        assertApprox("lifted-one-cum-pct", "one cum pct", 0.25d, f.getCumPct(1));
        assertApprox("lifted-two-cum-pct", "two cum pct", 0.50d, f.getCumPct(Long.valueOf(2)));
        assertApprox("lifted-integer-arg-cum-pct", "Integer argument", 0.50d, f.getCumPct(Integer.valueOf(2)));
        assertApprox("lifted-three-cum-pct", "three cum pct", 1.0d, f.getCumPct(threeL));
        assertApprox("lifted-five-cum-pct", "five cum pct", 1.0d, f.getCumPct(5));
        assertApprox("lifted-zero-cum-pct", "zero cum pct", 0.0d, f.getCumPct(0));
        assertApprox("lifted-foo-cum-pct", "foo cum pct", 0.0d, f.getCumPct("foo"));

        int maxSeen = 3;
        int extraAdds = data.consumeInt(0, 16);
        for (int i = 0; i < extraAdds; i++) {
            int v = data.consumeInt(-1000, 1000);
            f.addValue(v);
            if (v > maxSeen) {
                maxSeen = v;
            }

            try {
                Integer probePresent = Integer.valueOf(v);
                Integer probeAbsent = Integer.valueOf(maxSeen == 1000 ? -1000 : maxSeen + 1);

                long beforeSum = f.getSumFreq();
                int beforeHash = f.hashCode();
                String beforeString = f.toString();

                double objectPctPresent = f.getPct((Object) probePresent);
                double comparablePctPresent = f.getPct(probePresent);

                long afterSum = f.getSumFreq();
                int afterHash = f.hashCode();
                String afterString = f.toString();

                if (!approxEquals(objectPctPresent, comparablePctPresent)) {
                    throw new RuntimeException("[oracle:object-vs-comparable-present] metamorphic violation: deprecated getPct(Object) is documented as replaced by getPct(Comparable), so equivalent Integer inputs must yield the same percentage input=" + probePresent + " lhs=" + objectPctPresent + " rhs=" + comparablePctPresent);
                }

                if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
                    throw new RuntimeException("[oracle:read-only-query] metamorphic violation: getPct is a read-only getter and must not change observable state input=" + probePresent + " beforeSum=" + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeToString=" + escape(beforeString) + " afterToString=" + escape(afterString));
                }

                double objectPctAbsent = f.getPct((Object) probeAbsent);
                double comparablePctAbsent = f.getPct(probeAbsent);

                if (!approxEquals(objectPctAbsent, comparablePctAbsent)) {
                    throw new RuntimeException("[oracle:object-vs-comparable-absent] metamorphic violation: deprecated getPct(Object) is documented as replaced by getPct(Comparable), so equivalent absent Integer inputs must yield the same percentage input=" + probeAbsent + " lhs=" + objectPctAbsent + " rhs=" + comparablePctAbsent);
                }

                /* Contract justification:
                 * getCount(v) returns 0 for absent values; getPct(Comparable) returns getCount(v)/getSumFreq when non-empty.
                 * Therefore, for a non-empty Frequency and an absent integral probe, the percentage must be exactly 0.
                 * A patch that redirects getPct(Object) to cumulative percentage breaks this and can return 1.0 for values above the maximum.
                 */
                if (!approxEquals(0.0d, comparablePctAbsent) || !approxEquals(0.0d, objectPctAbsent)) {
                    throw new RuntimeException("[oracle:absent-value-zero-pct] metamorphic violation: absent value must have zero percentage in a non-empty frequency input=" + probeAbsent + " objectPct=" + objectPctAbsent + " comparablePct=" + comparablePctAbsent + " sumFreq=" + f.getSumFreq());
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                return;
            }
        }
    }

    private static void assertApprox(String oracleId, String label, double expected, double actual) {
        if (!approxEquals(expected, actual)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double a, double b) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Math.abs(a - b) <= TOLERANCE;
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