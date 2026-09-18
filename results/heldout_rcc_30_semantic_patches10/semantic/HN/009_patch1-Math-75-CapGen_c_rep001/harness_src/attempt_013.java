package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static final double TOLERANCE = 10E-15;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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

        checkApprox("lifted-one-pct", "one pct", 0.25, f.getPct(1));
        checkApprox("lifted-two-pct", "two pct", 0.25, f.getPct(Long.valueOf(2)));
        checkApprox("lifted-three-pct", "three pct", 0.5, f.getPct(Long.valueOf(3)));
        checkApprox("lifted-three-object-pct", "three (Object) pct", 0.5, f.getPct((Object) Integer.valueOf(3)));
        checkApprox("lifted-five-pct", "five pct", 0.0, f.getPct(5));
        checkApprox("lifted-foo-pct", "foo pct", 0.0, f.getPct("foo"));
        checkApprox("lifted-one-cum-pct", "one cum pct", 0.25, f.getCumPct(1));
        checkApprox("lifted-two-cum-pct", "two cum pct", 0.50, f.getCumPct(Long.valueOf(2)));
        checkApprox("lifted-integer-argument", "Integer argument", 0.50, f.getCumPct(Integer.valueOf(2)));
        checkApprox("lifted-three-cum-pct", "three cum pct", 1.0, f.getCumPct(Long.valueOf(3)));
        checkApprox("lifted-five-cum-pct", "five cum pct", 1.0, f.getCumPct(5));
        checkApprox("lifted-zero-cum-pct", "zero cum pct", 0.0, f.getCumPct(0));
        checkApprox("lifted-foo-cum-pct", "foo cum pct", 0.0, f.getCumPct("foo"));

        int probe = data.consumeInt(-1000, 1000);
        int mutations = data.consumeInt(0, 16);

        for (int i = 0; i < mutations; i++) {
            int v = data.consumeInt(-1000, 1000);
            switch (data.consumeInt(0, 4)) {
                case 0:
                    f.addValue(v);
                    break;
                case 1:
                    f.addValue((long) v);
                    break;
                case 2:
                    f.addValue(Integer.valueOf(v));
                    break;
                case 3:
                    f.addValue((Object) Integer.valueOf(v));
                    break;
                default:
                    f.addValue(Long.valueOf(v));
                    break;
            }

            verifyDeprecatedObjectOverloadAgreementAndReadOnly(f, probe, "after-mutation-" + i);
            verifyDeprecatedCumPctAgreementAndReadOnly(f, probe, "after-mutation-" + i);
            verifyAbsentComparableRejectionInvariant(f, data.consumeAsciiString(8), "after-mutation-" + i);
        }

        verifyDeprecatedObjectOverloadAgreementAndReadOnly(f, probe, "final");
        verifyDeprecatedCumPctAgreementAndReadOnly(f, probe, "final");
        verifyAbsentComparableRejectionInvariant(f, data.consumeAsciiString(8), "final");

        Frequency g = new Frequency();
        int a = data.consumeInt(-1000, 1000);
        int b = data.consumeInt(-1000, 1000);
        int extra = data.consumeInt(0, 8);

        g.addValue(a);
        g.addValue(b);
        for (int i = 0; i < extra; i++) {
            g.addValue(data.consumeInt(-1000, 1000));
        }

        try {
            double lhs = g.getPct((Object) Integer.valueOf(a));
            double rhs = g.getPct(Integer.valueOf(a));
            if (!sameDouble(lhs, rhs)) {
                throw new RuntimeException("[oracle:equiv-object-comparable] metamorphic violation: deprecated getPct(Object) must agree with getPct(Comparable) for the same Comparable input input=" + a + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            return;
        }

        try {
            double lhs = g.getCumPct((Object) Integer.valueOf(b));
            double rhs = g.getCumPct(Integer.valueOf(b));
            if (!sameDouble(lhs, rhs)) {
                throw new RuntimeException("[oracle:equiv-cumpct-object-comparable] metamorphic violation: deprecated getCumPct(Object) must agree with getCumPct(Comparable) for the same Comparable input input=" + b + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            return;
        }
    }

    private static void verifyDeprecatedObjectOverloadAgreementAndReadOnly(Frequency f, int probe, String stage) {
        try {
            long beforeSum = f.getSumFreq();
            int beforeHash = f.hashCode();
            String beforeString = f.toString();

            double lhs = f.getPct((Object) Integer.valueOf(probe));
            double rhs = f.getPct(Integer.valueOf(probe));

            long afterSum = f.getSumFreq();
            int afterHash = f.hashCode();
            String afterString = f.toString();

            if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
                throw new RuntimeException("[oracle:readonly-getpct] metamorphic violation: getPct is a read-only query and must not change observable state stage=" + stage + " beforeSum=" + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeString=" + quote(beforeString) + " afterString=" + quote(afterString));
            }

            /* Contract basis:
             * - getPct(Object) is deprecated and replaced by getPct(Comparable), so for a Comparable input they must agree.
             * - getPct is documented as a query ("Returns the percentage..."), so it must not mutate observable state.
             * A patch that redirects to the cumulative method, or deletes/changes bookkeeping incorrectly, violates one or both checks.
             */
            if (!sameDouble(lhs, rhs)) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:getpct-object-vs-comparable] semantic mismatch: deprecated getPct(Object) must equal getPct(Comparable) for the same Comparable input stage=" + stage + " input=" + probe + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) {
                throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            }
        }
    }

    private static void verifyDeprecatedCumPctAgreementAndReadOnly(Frequency f, int probe, String stage) {
        try {
            long beforeSum = f.getSumFreq();
            int beforeHash = f.hashCode();
            String beforeString = f.toString();

            double lhs = f.getCumPct((Object) Integer.valueOf(probe));
            double rhs = f.getCumPct(Integer.valueOf(probe));

            long afterSum = f.getSumFreq();
            int afterHash = f.hashCode();
            String afterString = f.toString();

            if (beforeSum != afterSum || beforeHash != afterHash || !safeEquals(beforeString, afterString)) {
                throw new RuntimeException("[oracle:readonly-getcumpct] metamorphic violation: getCumPct is a read-only query and must not change observable state stage=" + stage + " beforeSum=" + beforeSum + " afterSum=" + afterSum + " beforeHash=" + beforeHash + " afterHash=" + afterHash + " beforeString=" + quote(beforeString) + " afterString=" + quote(afterString));
            }

            if (!sameDouble(lhs, rhs)) {
                throw new RuntimeException("[oracle:getcumpct-object-vs-comparable] metamorphic violation: deprecated getCumPct(Object) must equal getCumPct(Comparable) for the same Comparable input stage=" + stage + " input=" + probe + " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                String m = t.getMessage();
                if (m != null && m.startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
            }
        }
    }

    private static void verifyAbsentComparableRejectionInvariant(Frequency f, String s, String stage) {
        try {
            String probe = s == null ? "" : s;
            double pct = f.getPct(probe);
            double cumPct = f.getCumPct(probe);

            /* Contract basis:
             * For a non-comparable-to-set query like String against an integral-valued Frequency:
             * - getPct returns 0 because getCount returns 0.
             * - getCumPct returns 0 if at least one value has been added but v is not comparable to the values set.
             * This must stay true after every state change because the set remains integral-only.
             */
            if (f.getSumFreq() > 0) {
                if (!sameDouble(pct, 0.0)) {
                    throw new RuntimeException("[oracle:absent-string-pct] metamorphic violation: non-comparable String probe against integral-only Frequency must have pct 0 stage=" + stage + " input=" + quote(probe) + " actual=" + pct);
                }
                if (!sameDouble(cumPct, 0.0)) {
                    throw new RuntimeException("[oracle:absent-string-cumpct] metamorphic violation: non-comparable String probe against integral-only Frequency must have cumPct 0 stage=" + stage + " input=" + quote(probe) + " actual=" + cumPct);
                }
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                String m = t.getMessage();
                if (m != null && m.startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
            }
        }
    }

    private static void checkApprox(String oracleId, String what, double expected, double actual) {
        if (!approxEquals(expected, actual)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double expected, double actual) {
        if (Double.isNaN(expected) && Double.isNaN(actual)) {
            return true;
        }
        return Math.abs(expected - actual) <= TOLERANCE;
    }

    private static boolean sameDouble(double a, double b) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b) || Math.abs(a - b) <= TOLERANCE;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}