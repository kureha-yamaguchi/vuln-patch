package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // Reach the patched RegulaFalsi path through the exact failing-test setup.
        // We do not alarm on this already-known symptom here; we use it to drive execution
        // through the real public API and the patched loop/branch.
        try {
            final UnivariateRealFunction issue631 = new UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };
            final RegulaFalsiSolver seeded = new RegulaFalsiSolver();
            seeded.solve(3624, issue631, 1.0, 10.0);
        } catch (Throwable ignored) {
            // For this harness, any outcome here is only for reachability.
        }

        final int ai = data.consumeInt(-1000, 1000);
        final int left = data.consumeInt(1, 1000);
        final int right = data.consumeInt(1, 1000);
        final double a = ai;
        final double min = a - left;
        final double max = a + right;

        double rPos;
        double rNeg;
        double rLeft;
        double rAbove;

        try {
            final UnivariateRealFunction positive = new UnivariateRealFunction() {
                public double value(double x) {
                    return x - a;
                }
            };
            final UnivariateRealFunction negative = new UnivariateRealFunction() {
                public double value(double x) {
                    return a - x;
                }
            };

            final RegulaFalsiSolver s1 = new RegulaFalsiSolver();
            final RegulaFalsiSolver s2 = new RegulaFalsiSolver();
            final RegulaFalsiSolver s3 = new RegulaFalsiSolver();
            final RegulaFalsiSolver s4 = new RegulaFalsiSolver();

            // Contract justification:
            // In BaseSecantSolver.doSolve(), after computing the secant approximation x,
            // the solver returns immediately if fx == 0.0, regardless of AllowedSolution.
            // For the affine function f(x)=x-a over a bracket [a-left, a+right], the secant
            // formula yields x == a exactly, so any correct implementation must return the
            // exact root a. Negating the function preserves the same root, so both calls
            // must agree. A band-aid that only hides the known throw bug but perturbs the
            // iteration/update logic would violate this exact-root symmetry.
            rPos = s1.solve(64, positive, min, max, AllowedSolution.ANY_SIDE);
            rNeg = s2.solve(64, negative, min, max, AllowedSolution.ANY_SIDE);

            // Same documented reason: exact-root return happens before allowed-side filtering,
            // so different AllowedSolution choices must not change the returned exact root.
            rLeft = s3.solve(64, positive, min, max, AllowedSolution.LEFT_SIDE);
            rAbove = s4.solve(64, positive, min, max, AllowedSolution.ABOVE_SIDE);
        } catch (Throwable ignored) {
            // Exceptions are rejections for this input; do not treat them as violations.
            return;
        }

        if (rPos != a || rNeg != a) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:linear-sign-symmetry] metamorphic violation: exact affine root not preserved under function negation"
                    + " a=" + a
                    + " min=" + min
                    + " max=" + max
                    + " positive=" + rPos
                    + " negative=" + rNeg);
        }

        if (rLeft != a || rAbove != a || rLeft != rAbove || rLeft != rPos) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:exact-root-allowed-invariance] metamorphic violation: exact root should be returned independent of AllowedSolution"
                    + " a=" + a
                    + " min=" + min
                    + " max=" + max
                    + " any=" + rPos
                    + " left=" + rLeft
                    + " above=" + rAbove);
        }
    }
}