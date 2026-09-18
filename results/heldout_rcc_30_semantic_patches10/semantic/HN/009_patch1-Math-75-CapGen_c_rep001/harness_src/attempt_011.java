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
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-1", "after first mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-1", "after first mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(twoL);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-2", "after second mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-2", "after second mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(oneI);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-3", "after third mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-3", "after third mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(twoI);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-4", "after fourth mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-4", "after fourth mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(threeL);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-5", "after fifth mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-5", "after fifth mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(threeL);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-6", "after sixth mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-6", "after sixth mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(3);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-7", "after seventh mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-7", "after seventh mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        f.addValue(threeI);
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "reprobe-foo-pct-8", "after eighth mutation, non-comparable probe should still have 0 pct on a non-empty integral frequency");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "reprobe-foo-cumpct-8", "after eighth mutation, non-comparable probe should still have 0 cumulative pct on a non-empty integral frequency");

        assertApprox(0.25, f.getPct(1), TOLERANCE, "testPcts-one-pct", "one pct");
        assertApprox(0.25, f.getPct(Long.valueOf(2)), TOLERANCE, "testPcts-two-pct", "two pct");
        assertApprox(0.5, f.getPct(Long.valueOf(threeL)), TOLERANCE, "testPcts-three-pct", "three pct");
        assertApprox(0.5, f.getPct((Object) (Integer.valueOf(3))), TOLERANCE, "testPcts-three-object-pct", "three (Object) pct");
        assertApprox(0.0, f.getPct(5), TOLERANCE, "testPcts-five-pct", "five pct");
        assertApprox(0.0, f.getPct("foo"), TOLERANCE, "testPcts-foo-pct", "foo pct");
        assertApprox(0.25, f.getCumPct(1), TOLERANCE, "testPcts-one-cumpct", "one cum pct");
        assertApprox(0.50, f.getCumPct(Long.valueOf(2)), TOLERANCE, "testPcts-two-cumpct", "two cum pct");
        assertApprox(0.50, f.getCumPct(Integer.valueOf(2)), TOLERANCE, "testPcts-integer-argument", "Integer argument");
        assertApprox(1.0, f.getCumPct(Long.valueOf(threeL)), TOLERANCE, "testPcts-three-cumpct", "three cum pct");
        assertApprox(1.0, f.getCumPct(5), TOLERANCE, "testPcts-five-cumpct", "five cum pct");
        assertApprox(0.0, f.getCumPct(0), TOLERANCE, "testPcts-zero-cumpct", "zero cum pct");
        assertApprox(0.0, f.getCumPct("foo"), TOLERANCE, "testPcts-foo-cumpct", "foo cum pct");

        try {
            Frequency g = new Frequency();
            int target = data.consumeInt(-1000, 1000);
            int other = data.consumeInt(-1000, 1000);
            if (other == target) {
                other = target + 1;
            }
            int targetCopies = data.consumeInt(1, 8);
            int otherCopies = data.consumeInt(1, 8);

            for (int i = 0; i < targetCopies; i++) {
                g.addValue(target);
            }
            for (int i = 0; i < otherCopies; i++) {
                g.addValue(other);
            }

            long sumBefore = g.getSumFreq();
            int hashBefore = g.hashCode();
            String stringBefore = g.toString();

            double objectPct = g.getPct((Object) Integer.valueOf(target));
            long sumAfter = g.getSumFreq();
            int hashAfter = g.hashCode();
            String stringAfter = g.toString();

            // getPct is documented as a read-only query ("Returns the percentage..."), so it must not mutate observable state.
            if (sumBefore != sumAfter || hashBefore != hashAfter || !safeEquals(stringBefore, stringAfter)) {
                throw new RuntimeException("[oracle:read-only-getPct-object] metamorphic violation: read-only accessor changed receiver state target=" + target + " sumBefore=" + sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter + " toStringBefore=" + escape(stringBefore) + " toStringAfter=" + escape(stringAfter));
            }

            double comparablePct = g.getPct(Integer.valueOf(target));
            // Deprecated getPct(Object) is documented as replaced by getPct(Comparable); equivalent Integer inputs must agree.
            if (!approxEquals(objectPct, comparablePct, TOLERANCE)) {
                throw new RuntimeException("[oracle:object-vs-comparable-getPct] metamorphic violation: equivalent overloads disagreed target=" + target + " lhs=" + objectPct + " rhs=" + comparablePct + " sumFreq=" + g.getSumFreq() + " count=" + g.getCount(Integer.valueOf(target)));
            }

            double constructedExpected = ((double) g.getCount(Integer.valueOf(target))) / ((double) g.getSumFreq());
            // The javadoc for getPct says it returns "the proportion of values equal to v"; for a non-empty frequency built from known counts, that proportion is count/sumFreq.
            if (!approxEquals(comparablePct, constructedExpected, TOLERANCE)) {
                throw new RuntimeException("[oracle:pct-count-over-sum] metamorphic violation: getPct disagreed with getCount/sumFreq target=" + target + " lhs=" + comparablePct + " rhs=" + constructedExpected + " sumFreq=" + g.getSumFreq() + " count=" + g.getCount(Integer.valueOf(target)));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }

    private static void assertApprox(double expected, double actual, double tolerance, String id, String what) {
        if (!approxEquals(expected, actual, tolerance)) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:" + id + "] semantic mismatch: " + what + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean approxEquals(double a, double b, double tolerance) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return Double.isNaN(a) && Double.isNaN(b);
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