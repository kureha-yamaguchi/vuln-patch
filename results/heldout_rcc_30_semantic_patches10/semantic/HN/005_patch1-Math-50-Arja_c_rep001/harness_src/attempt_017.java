package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final boolean runIssue631 = data.consumeBoolean();

        if (runIssue631) {
            final UnivariateRealFunction f = new UnivariateRealFunction() {
                public double value(double x) {
                    return Math.exp(x) - Math.pow(Math.PI, 3.0);
                }
            };

            final UnivariateRealSolver solver = new RegulaFalsiSolver();
            try {
                final double root = solver.solve(3624, f, 1.0, 10.0);
                if (Math.abs(root - 3.4341896575482003) <= 1e-15) {
                    throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                        "[oracle:issue631-no-exception] semantic mismatch: expected TooManyEvaluationsException but solve returned root=" + root);
                }
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:issue631-wrong-return] semantic mismatch: expected TooManyEvaluationsException but solve returned unexpected root=" + root);
            } catch (TooManyEvaluationsException expected) {
            } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
                return;
            }
        }

        final double target = data.consumeInt(-1000, 1000) / 100.0;
        final double min = target - 1.0;
        final double max = target + 1.0;

        final UnivariateRealFunction linear = new UnivariateRealFunction() {
            public double value(double x) {
                return x - target;
            }
        };

        try {
            final RegulaFalsiSolver defaultSolver = new RegulaFalsiSolver();
            final double implicitAny = defaultSolver.solve(32, linear, min, max);

            final RegulaFalsiSolver explicitSolver = new RegulaFalsiSolver();
            final double explicitAny = explicitSolver.solve(32, linear, min, max, AllowedSolution.ANY_SIDE);

            if (Double.doubleToLongBits(implicitAny) != Double.doubleToLongBits(explicitAny)) {
                throw new RuntimeException(
                    "[oracle:default-any-side] metamorphic violation: solve(maxEval,f,min,max) must equal solve(maxEval,f,min,max,AllowedSolution.ANY_SIDE) because bracketed solvers document ANY_SIDE as the default allowed solution; inputTarget="
                        + target + " lhs=" + implicitAny + " rhs=" + explicitAny);
            }
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
            return;
        }

        try {
            final RegulaFalsiSolver statefulSolver = new RegulaFalsiSolver();
            final UnivariateRealFunction quadratic = new UnivariateRealFunction() {
                public double value(double x) {
                    return x * x - 2.0;
                }
            };

            statefulSolver.solve(64, quadratic, 0.0, 2.0, AllowedSolution.LEFT_SIDE);

            final double afterMutationImplicit = statefulSolver.solve(64, quadratic, 0.0, 2.0);
            final double freshExplicitAny = new RegulaFalsiSolver().solve(64, quadratic, 0.0, 2.0, AllowedSolution.ANY_SIDE);

            if (Double.doubleToLongBits(afterMutationImplicit) != Double.doubleToLongBits(freshExplicitAny)) {
                throw new RuntimeException(
                    "[oracle:allowed-state-reset] metamorphic violation: implicit solve must use the documented default ANY_SIDE regardless of prior solve(...) calls that changed allowed-solution state; lhs="
                        + afterMutationImplicit + " rhs=" + freshExplicitAny);
            }
        } catch (RuntimeException e) { /*__vpRepair*/ if (e instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) e;
            if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                throw e;
            }
        }
    }
}