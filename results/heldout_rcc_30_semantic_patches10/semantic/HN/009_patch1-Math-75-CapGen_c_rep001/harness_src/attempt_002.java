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

        long beforeSum = f.getSumFreq();
        int beforeHash = f.hashCode();
        String beforeString = f.toString();

        f.addValue(oneL);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-1] metamorphic violation: addValue must increase total frequency by 1 input=1 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(twoL);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-2] metamorphic violation: addValue must increase total frequency by 1 input=2 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(oneI);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-3] metamorphic violation: addValue must increase total frequency by 1 input=1 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(twoI);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-4] metamorphic violation: addValue must increase total frequency by 1 input=2 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(threeL);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-5] metamorphic violation: addValue must increase total frequency by 1 input=3 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(threeL);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-6] metamorphic violation: addValue must increase total frequency by 1 input=3 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(3);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-7] metamorphic violation: addValue must increase total frequency by 1 input=3 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }
        beforeSum = f.getSumFreq();
        beforeHash = f.hashCode();
        beforeString = f.toString();

        f.addValue(threeI);
        if (f.getSumFreq() != beforeSum + 1) {
            throw new RuntimeException("[oracle:sum-after-add-8] metamorphic violation: addValue must increase total frequency by 1 input=3 lhs=" + f.getSumFreq() + " rhs=" + (beforeSum + 1));
        }

        double actual;

        actual = f.getPct(1);
        if (Math.abs(actual - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-one-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(2));
        if (Math.abs(actual - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-two-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getPct(Long.valueOf(3));
        if (Math.abs(actual - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-three-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct((Object) (Integer.valueOf(3)));
        if (Math.abs(actual - 0.5d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-three-object-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getPct(5);
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-five-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getPct("foo");
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-foo-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct(1);
        if (Math.abs(actual - 0.25d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-one-cum-pct] semantic mismatch: expected=0.25 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(2));
        if (Math.abs(actual - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-two-cum-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Integer.valueOf(2));
        if (Math.abs(actual - 0.50d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-integer-arg-cum-pct] semantic mismatch: expected=0.5 actual=" + actual);
        }

        actual = f.getCumPct(Long.valueOf(3));
        if (Math.abs(actual - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-three-cum-pct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(5);
        if (Math.abs(actual - 1.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-five-cum-pct] semantic mismatch: expected=1.0 actual=" + actual);
        }

        actual = f.getCumPct(0);
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-zero-cum-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        actual = f.getCumPct("foo");
        if (Math.abs(actual - 0.0d) > tolerance) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow("[oracle:test-foo-cum-pct] semantic mismatch: expected=0.0 actual=" + actual);
        }

        try {
            Frequency g = new Frequency();

            int base = data.consumeInt(-1000, 1000);
            int mutationCount = data.consumeInt(1, 16);
            int probe = base + 1000000;

            for (int i = 0; i < mutationCount; i++) {
                int delta = data.consumeInt(-5, 5);
                int added = base + delta;
                if (added == probe) {
                    added = base;
                }

                long sumBefore = g.getSumFreq();
                g.addValue(added);

                if (g.getSumFreq() != sumBefore + 1) {
                    throw new RuntimeException("[oracle:fuzz-sum-increment] metamorphic violation: addValue must increase total frequency by 1 input=" + added + " lhs=" + g.getSumFreq() + " rhs=" + (sumBefore + 1));
                }

                /* Contract used:
                 * - getPct(Object) is deprecated in favor of getPct(Comparable); they document the same result:
                 *   "Returns the percentage of values that are equal to v".
                 * - We choose probe first, then only add values from base-5..base+5, so probe is absent by construction.
                 * Therefore for every non-empty state reached here, both overloads must return the same documented value, 0.0.
                 * A patch that silently routes Object to cumulative percentage instead of exact percentage breaks this without throwing.
                 */
                double objectPct = g.getPct((Object) Integer.valueOf(probe));
                double comparablePct = g.getPct(Integer.valueOf(probe));
                if (Math.abs(objectPct - comparablePct) > tolerance) {
                    throw new RuntimeException("[oracle:overload-equivalence-absent] metamorphic violation: getPct(Object) must agree with getPct(Comparable) for the same Integer input=" + probe + " lhs=" + objectPct + " rhs=" + comparablePct);
                }
                if (Math.abs(objectPct - 0.0d) > tolerance) {
                    throw new RuntimeException("[oracle:absent-probe-zero] metamorphic violation: absent value must have percentage 0.0 input=" + probe + " lhs=" + objectPct + " rhs=0.0");
                }
            }

            if (g.getSumFreq() > 0) {
                int target = base + data.consumeInt(-5, 5);

                /* Contract used:
                 * getPct(Comparable) is documented as the proportion of values equal to v.
                 * The Object overload is deprecated-replaced by that overload, so they must agree on the same Integer value.
                 * This post-condition is directly observable through the public API and catches wrong silent behavior.
                 */
                double pctObj = g.getPct((Object) Integer.valueOf(target));
                double pctCmp = g.getPct(Integer.valueOf(target));
                if (Math.abs(pctObj - pctCmp) > tolerance) {
                    throw new RuntimeException("[oracle:overload-equivalence-present-or-absent] metamorphic violation: equivalent overloads disagree input=" + target + " lhs=" + pctObj + " rhs=" + pctCmp);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }
    }
}