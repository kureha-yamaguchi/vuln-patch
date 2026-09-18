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
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-1] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-1] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(twoL);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-2] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-2] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(oneI);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-3] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-3] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(twoI);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-4] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-4] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(threeL);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-5] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-5] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(threeL);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-6] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-6] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(3);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-7] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-7] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        f.addValue(threeI);
        if (Math.abs(f.getPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct-after-add-8] semantic mismatch: expected=0.0 actual=" + f.getPct("foo"));
        }
        if (Math.abs(f.getCumPct("foo") - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct-after-add-8] semantic mismatch: expected=0.0 actual=" + f.getCumPct("foo"));
        }

        double actualOnePct = f.getPct(1);
        if (Math.abs(actualOnePct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:one-pct] semantic mismatch: expected=0.25 actual=" + actualOnePct);
        }

        double actualTwoPct = f.getPct(Long.valueOf(2));
        if (Math.abs(actualTwoPct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:two-pct] semantic mismatch: expected=0.25 actual=" + actualTwoPct);
        }

        double actualThreePct = f.getPct(threeL);
        if (Math.abs(actualThreePct - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:three-pct] semantic mismatch: expected=0.5 actual=" + actualThreePct);
        }

        long sumBefore = f.getSumFreq();
        int hashBefore = f.hashCode();
        String stringBefore = f.toString();
        double actualThreeObjectPct = f.getPct((Object) (Integer.valueOf(3)));
        long sumAfter = f.getSumFreq();
        int hashAfter = f.hashCode();
        String stringAfter = f.toString();

        if (Math.abs(actualThreeObjectPct - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:three-object-pct] semantic mismatch: expected=0.5 actual=" + actualThreeObjectPct);
        }

        if (sumBefore != sumAfter || hashBefore != hashAfter || !stringBefore.equals(stringAfter)) {
            throw new RuntimeException(
                "[oracle:read-only-getpct-object] metamorphic violation: getPct(Object) is a reader and must not mutate observable state input=3 sumBefore="
                    + sumBefore + " sumAfter=" + sumAfter + " hashBefore=" + hashBefore + " hashAfter=" + hashAfter
                    + " toStringBefore=" + stringBefore.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")
                    + " toStringAfter=" + stringAfter.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t"));
        }

        double actualFivePct = f.getPct(5);
        if (Math.abs(actualFivePct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:five-pct] semantic mismatch: expected=0.0 actual=" + actualFivePct);
        }

        double actualFooPct = f.getPct("foo");
        if (Math.abs(actualFooPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-pct] semantic mismatch: expected=0.0 actual=" + actualFooPct);
        }

        double actualOneCumPct = f.getCumPct(1);
        if (Math.abs(actualOneCumPct - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:one-cumpct] semantic mismatch: expected=0.25 actual=" + actualOneCumPct);
        }

        double actualTwoCumPct = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actualTwoCumPct - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:two-cumpct] semantic mismatch: expected=0.5 actual=" + actualTwoCumPct);
        }

        double actualIntegerArgumentCumPct = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actualIntegerArgumentCumPct - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:integer-argument-cumpct] semantic mismatch: expected=0.5 actual=" + actualIntegerArgumentCumPct);
        }

        double actualThreeCumPct = f.getCumPct(threeL);
        if (Math.abs(actualThreeCumPct - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:three-cumpct] semantic mismatch: expected=1.0 actual=" + actualThreeCumPct);
        }

        double actualFiveCumPct = f.getCumPct(5);
        if (Math.abs(actualFiveCumPct - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:five-cumpct] semantic mismatch: expected=1.0 actual=" + actualFiveCumPct);
        }

        double actualZeroCumPct = f.getCumPct(0);
        if (Math.abs(actualZeroCumPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:zero-cumpct] semantic mismatch: expected=0.0 actual=" + actualZeroCumPct);
        }

        double actualFooCumPct = f.getCumPct("foo");
        if (Math.abs(actualFooCumPct - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:foo-cumpct] semantic mismatch: expected=0.0 actual=" + actualFooCumPct);
        }

        try {
            // Documented sibling agreement: getPct(Object) is deprecated in favor of getPct(Comparable),
            // so for a Comparable input they must return the same percentage; a wrong delegation to getCumPct breaks this.
            double lhs = f.getPct((Object) Integer.valueOf(3));
            double rhs = f.getPct(Integer.valueOf(3));
            if (Math.abs(lhs - rhs) > tolerance) {
                throw new RuntimeException(
                    "[oracle:object-vs-comparable-getpct] metamorphic violation: equivalent overloads disagree input=3 lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        int extraAdds = data.consumeInt(0, 8);
        for (int i = 0; i < extraAdds; i++) {
            int op = data.consumeInt(0, 2);
            if (op == 0) {
                f.addValue(data.consumeInt(-1000000, 1000000));
            } else if (op == 1) {
                f.addValue((long) data.consumeInt(-1000000, 1000000));
            } else {
                f.addValue((char) data.consumeInt(0, 255));
            }

            if (f.getSumFreq() > 0) {
                double probePct = f.getPct("foo");
                if (Math.abs(probePct - 0.0d) > tolerance) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:foo-pct-reprobe] semantic mismatch: expected=0.0 actual=" + probePct);
                }
                double probeCumPct = f.getCumPct("foo");
                if (Math.abs(probeCumPct - 0.0d) > tolerance) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:foo-cumpct-reprobe] semantic mismatch: expected=0.0 actual=" + probeCumPct);
                }
            }
        }

        try {
            int chosen = data.consumeInt(-1000000, 1000000);
            Frequency g = new Frequency();
            g.addValue(chosen);
            g.addValue(chosen);
            g.addValue(chosen + 1);
            // Constructed-from-known-answer oracle: with counts {chosen:2, chosen+1:1}, pct(chosen) must be 2/3.
            double lhs = g.getPct((Object) Integer.valueOf(chosen));
            double rhs = g.getPct(Integer.valueOf(chosen));
            if (Math.abs(lhs - rhs) > tolerance) {
                throw new RuntimeException(
                    "[oracle:fuzzed-object-vs-comparable-getpct] metamorphic violation: equivalent overloads disagree input="
                        + chosen + " lhs=" + lhs + " rhs=" + rhs);
            }
            double expected = 2.0d / 3.0d;
            if (Math.abs(rhs - expected) > tolerance) {
                throw new RuntimeException(
                    "[oracle:fuzzed-known-answer-getpct] metamorphic violation: constructed distribution has known answer input="
                        + chosen + " lhs=" + rhs + " rhs=" + expected);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}