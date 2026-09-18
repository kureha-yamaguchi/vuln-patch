package org.apache.commons.math.stat;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final double tolerance = 10E-15;

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

        double actual;

        actual = f.getPct(1);
        if (!(Math.abs(actual - 0.25d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-one-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(2));
        if (!(Math.abs(actual - 0.25d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-two-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(3));
        if (!(Math.abs(actual - 0.5d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-three-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct((Object) Integer.valueOf(3));
        if (!(Math.abs(actual - 0.5d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-three-object-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct(5);
        if (!(Math.abs(actual - 0.0d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-five-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getPct("foo");
        if (!(Math.abs(actual - 0.0d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-foo-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct(1);
        if (!(Math.abs(actual - 0.25d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-one-cumpct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(2));
        if (!(Math.abs(actual - 0.50d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-two-cumpct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Integer.valueOf(2));
        if (!(Math.abs(actual - 0.50d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-integer-cumpct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(3));
        if (!(Math.abs(actual - 1.0d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-three-cumpct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(5);
        if (!(Math.abs(actual - 1.0d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-five-cumpct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(0);
        if (!(Math.abs(actual - 0.0d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-zero-cumpct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct("foo");
        if (!(Math.abs(actual - 0.0d) <= tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:seed-foo-cumpct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        Frequency g = new Frequency();
        int mutations = data.consumeInt(1, 8);
        for (int i = 0; i < mutations; i++) {
            int v = data.consumeInt(-1000000, 1000000);

            switch (data.consumeInt(0, 3)) {
                case 0:
                    g.addValue(v);
                    break;
                case 1:
                    g.addValue((long) v);
                    break;
                case 2:
                    g.addValue(Integer.valueOf(v));
                    break;
                default:
                    g.addValue((Comparable<?>) Long.valueOf(v));
                    break;
            }

            try {
                long beforeSum = g.getSumFreq();
                int beforeHash = g.hashCode();
                String beforeString = g.toString();

                Object probe = Integer.valueOf(v);
                double lhs = g.getPct(probe);

                long afterSum = g.getSumFreq();
                int afterHash = g.hashCode();
                String afterString = g.toString();

                if (beforeSum != afterSum || beforeHash != afterHash || !beforeString.equals(afterString)) {
                    throw new RuntimeException(
                        "[oracle:readonly-getpct-object] metamorphic violation: getPct(Object) is documented as a read-only query, but observable state changed input="
                            + v + " sumBefore=" + beforeSum + " sumAfter=" + afterSum
                            + " hashBefore=" + beforeHash + " hashAfter=" + afterHash
                            + " toStringBefore=" + beforeString.replace("\n", "\\n").replace("\t", "\\t")
                            + " toStringAfter=" + afterString.replace("\n", "\\n").replace("\t", "\\t"));
                }

                double rhsOverload = g.getPct((Comparable<?>) Long.valueOf(v));
                if (Double.isNaN(lhs) != Double.isNaN(rhsOverload)
                        || (!Double.isNaN(lhs) && Math.abs(lhs - rhsOverload) > tolerance)) {
                    throw new RuntimeException(
                        "[oracle:object-vs-comparable-overload] metamorphic violation: getPct(Object) and getPct(Comparable) have the same documented meaning for comparable inputs input="
                            + v + " lhs=" + lhs + " rhs=" + rhsOverload);
                }

                long sum = g.getSumFreq();
                if (sum > 0) {
                    double rhsFormula = (double) g.getCount((Comparable<?>) Long.valueOf(v)) / (double) sum;
                    if (Double.isNaN(lhs) != Double.isNaN(rhsFormula)
                            || (!Double.isNaN(lhs) && Math.abs(lhs - rhsFormula) > tolerance)) {
                        throw new RuntimeException(
                            "[oracle:pct-formula] metamorphic violation: getPct is documented as the proportion of values equal to v, so it must equal getCount(v)/getSumFreq() input="
                                + v + " lhs=" + lhs + " rhs=" + rhsFormula + " count="
                                + g.getCount((Comparable<?>) Long.valueOf(v)) + " sum=" + sum);
                    }
                }
            } catch (Throwable t) {
                return;
            }
        }
    }
}