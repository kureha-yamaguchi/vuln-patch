package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        Frequency f = new Frequency();

        final long oneL = 1L;
        final long twoL = 2L;
        final long threeL = 3L;
        final int oneI = 1;
        final int twoI = 2;
        final int threeI = 3;
        final double tolerance = 10E-15;

        try {
            f.addValue(oneL);
            f.addValue(twoL);
            f.addValue(oneI);
            f.addValue(twoI);
            f.addValue(threeL);
            f.addValue(threeL);
            f.addValue(3);
            f.addValue(threeI);
        } catch (RuntimeException e) {
            return;
        }

        checkDouble("lifted-one-pct", "one pct", 0.25d, safeGetPctInt(f, 1), tolerance);
        checkDouble("lifted-two-pct", "two pct", 0.25d, safeGetPctComparable(f, Long.valueOf(2)), tolerance);
        checkDouble("lifted-three-pct", "three pct", 0.5d, safeGetPctComparable(f, Long.valueOf(3)), tolerance);
        checkDouble("lifted-three-object-pct", "three (Object) pct", 0.5d, safeGetPctObject(f, Integer.valueOf(3)), tolerance);
        checkDouble("lifted-five-pct", "five pct", 0.0d, safeGetPctInt(f, 5), tolerance);
        checkDouble("lifted-foo-pct", "foo pct", 0.0d, safeGetPctObject(f, "foo"), tolerance);
        checkDouble("lifted-one-cum-pct", "one cum pct", 0.25d, safeGetCumPctInt(f, 1), tolerance);
        checkDouble("lifted-two-cum-pct", "two cum pct", 0.50d, safeGetCumPctComparable(f, Long.valueOf(2)), tolerance);
        checkDouble("lifted-int-arg-cum-pct", "Integer argument", 0.50d, safeGetCumPctObject(f, Integer.valueOf(2)), tolerance);
        checkDouble("lifted-three-cum-pct", "three cum pct", 1.0d, safeGetCumPctComparable(f, Long.valueOf(3)), tolerance);
        checkDouble("lifted-five-cum-pct", "five cum pct", 1.0d, safeGetCumPctInt(f, 5), tolerance);
        checkDouble("lifted-zero-cum-pct", "zero cum pct", 0.0d, safeGetCumPctInt(f, 0), tolerance);
        checkDouble("lifted-foo-cum-pct", "foo cum pct", 0.0d, safeGetCumPctObject(f, "foo"), tolerance);

        int extraMutations = data.consumeInt(0, 16);
        for (int i = 0; i < extraMutations; i++) {
            int kind = data.consumeInt(0, 4);
            try {
                if (kind == 0) {
                    f.addValue(data.consumeInt(-1000000, 1000000));
                } else if (kind == 1) {
                    f.addValue((long) data.consumeInt(-1000000, 1000000));
                } else if (kind == 2) {
                    f.addValue(Integer.valueOf(data.consumeInt(-1000000, 1000000)));
                } else if (kind == 3) {
                    f.addValue(Long.valueOf((long) data.consumeInt(-1000000, 1000000)));
                } else {
                    f.addValue((Object) Integer.valueOf(data.consumeInt(-1000000, 1000000)));
                }
            } catch (RuntimeException e) {
                return;
            }

            // Documented contract: integral values are not distinguished by type, and the deprecated
            // getPct(Object) is replaced by getPct(Comparable). Therefore for any integral probe v,
            // getPct((Object) Integer.valueOf(v)), getPct(Integer.valueOf(v)), and getPct(v) must agree.
            // A patch that silently routes the Object overload to cumulative percentage violates this.
            int probe = data.consumeInt(-1000000, 1000000);
            try {
                long sumBefore = f.getSumFreq();
                int hashBefore = f.hashCode();
                String stringBefore = f.toString();

                double pctObject = f.getPct((Object) Integer.valueOf(probe));
                double pctComparable = f.getPct(Integer.valueOf(probe));
                double pctPrimitive = f.getPct(probe);

                long sumAfter = f.getSumFreq();
                int hashAfter = f.hashCode();
                String stringAfter = f.toString();

                if (sumBefore != sumAfter) {
                    throw new RuntimeException("[oracle:readonly-sumfreq] metamorphic violation: getPct changed getSumFreq probe=" + probe + " before=" + sumBefore + " after=" + sumAfter);
                }
                if (hashBefore != hashAfter) {
                    throw new RuntimeException("[oracle:readonly-hash] metamorphic violation: getPct changed hashCode probe=" + probe + " before=" + hashBefore + " after=" + hashAfter);
                }
                if (!safeEquals(stringBefore, stringAfter)) {
                    throw new RuntimeException("[oracle:readonly-tostring] metamorphic violation: getPct changed toString probe=" + probe + " before=" + escape(stringBefore) + " after=" + escape(stringAfter));
                }

                if (!sameDouble(pctObject, pctComparable, tolerance)) {
                    throw new RuntimeException("[oracle:object-comparable-pct] metamorphic violation: getPct((Object)Integer) must equal getPct(Integer) for integral values probe=" + probe + " lhs=" + pctObject + " rhs=" + pctComparable);
                }
                if (!sameDouble(pctObject, pctPrimitive, tolerance)) {
                    throw new RuntimeException("[oracle:object-primitive-pct] metamorphic violation: getPct((Object)Integer) must equal getPct(int) for integral values probe=" + probe + " lhs=" + pctObject + " rhs=" + pctPrimitive);
                }

                double cumObject = f.getCumPct((Object) Integer.valueOf(probe));
                double cumComparable = f.getCumPct(Integer.valueOf(probe));
                double cumPrimitive = f.getCumPct(probe);

                if (!sameDouble(cumObject, cumComparable, tolerance)) {
                    throw new RuntimeException("[oracle:object-comparable-cumpct] metamorphic violation: getCumPct((Object)Integer) must equal getCumPct(Integer) for integral values probe=" + probe + " lhs=" + cumObject + " rhs=" + cumComparable);
                }
                if (!sameDouble(cumObject, cumPrimitive, tolerance)) {
                    throw new RuntimeException("[oracle:object-primitive-cumpct] metamorphic violation: getCumPct((Object)Integer) must equal getCumPct(int) for integral values probe=" + probe + " lhs=" + cumObject + " rhs=" + cumPrimitive);
                }
            } catch (RuntimeException e) {
                if (e instanceof FuzzerSecurityIssueLow) {
                    throw e;
                }
                if (hasOraclePrefix(e)) {
                    throw e;
                }
                return;
            }
        }

        try {
            int known = data.consumeInt(-1000000, 1000000);
            Frequency g = new Frequency();
            int copies = data.consumeInt(1, 8);
            int distractors = data.consumeInt(0, 8);
            for (int i = 0; i < copies; i++) {
                g.addValue(known);
            }
            for (int i = 0; i < distractors; i++) {
                int other = known;
                while (other == known) {
                    other = data.consumeInt(-1000000, 1000000);
                }
                g.addValue(other);
            }

            // Oracle from construction: we chose exactly how many copies of `known` were inserted,
            // so getCount/getSumFreq fixes the expected percentage without any guessed reference implementation.
            double expectedPct = (double) g.getCount(known) / (double) g.getSumFreq();
            double actualObjPct = g.getPct((Object) Integer.valueOf(known));
            double actualCmpPct = g.getPct(Integer.valueOf(known));

            if (!sameDouble(actualCmpPct, expectedPct, tolerance)) {
                throw new RuntimeException("[oracle:constructed-comparable-pct] metamorphic violation: constructed frequency percentage mismatch input=" + known + " expected=" + expectedPct + " actual=" + actualCmpPct);
            }
            if (!sameDouble(actualObjPct, expectedPct, tolerance)) {
                throw new RuntimeException("[oracle:constructed-object-pct] metamorphic violation: constructed frequency percentage mismatch input=" + known + " expected=" + expectedPct + " actual=" + actualObjPct);
            }
        } catch (RuntimeException e) {
            if (e instanceof FuzzerSecurityIssueLow) {
                throw e;
            }
            if (hasOraclePrefix(e)) {
                throw e;
            }
        }
    }

    private static double safeGetPctObject(Frequency f, Object v) {
        try {
            return f.getPct(v);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private static double safeGetPctComparable(Frequency f, Comparable<?> v) {
        try {
            return f.getPct(v);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private static double safeGetPctInt(Frequency f, int v) {
        try {
            return f.getPct(v);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctObject(Frequency f, Object v) {
        try {
            return f.getCumPct(v);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctComparable(Frequency f, Comparable<?> v) {
        try {
            return f.getCumPct(v);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private static double safeGetCumPctInt(Frequency f, int v) {
        try {
            return f.getCumPct(v);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }

    private static void checkDouble(String oracleId, String label, double expected, double actual, double tolerance) {
        if (!sameDouble(expected, actual, tolerance)) {
            throw new FuzzerSecurityIssueLow("[oracle:" + oracleId + "] semantic mismatch: " + label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean sameDouble(double a, double b, double tolerance) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        if (a == b) {
            return true;
        }
        return Math.abs(a - b) <= tolerance;
    }

    private static boolean hasOraclePrefix(RuntimeException e) {
        String msg = e.getMessage();
        return msg != null && msg.startsWith("[oracle:");
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