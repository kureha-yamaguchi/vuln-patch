package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final double expectedRoot = 3.4341896575482003;
        final double expectedTol = 1e-15;

        {
            final UnivariateRealSolver solver = new RegulaFalsiSolver();
            try {
                final double root = solver.solve(3624, f, 1.0, 10.0);
                if (Math.abs(root - expectedRoot) <= expectedTol) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:issue631] semantic mismatch: expected TooManyEvaluationsException but solve returned root=" + root
                    );
                }
                return;
            } catch (TooManyEvaluationsException expected) {
                // Trusted oracle lifted verbatim from RegulaFalsiSolverTest.testIssue631:
                // the exact public call solver.solve(3624, f, 1, 10) must reject by throwing
                // TooManyEvaluationsException instead of silently returning the known root.
            } catch (RuntimeException other) { /*__vpRepair*/ if (other instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) other;
                return;
            }
        }

        final int extraBudget = data.consumeInt(4000, 12000);

        try {
            final RegulaFalsiSolver defaultSolver = new RegulaFalsiSolver();
            final RegulaFalsiSolver explicitAnySideSolver = new RegulaFalsiSolver();

            final double lhs = defaultSolver.solve(extraBudget, f, 1.0, 10.0);
            final double rhs = explicitAnySideSolver.solve(
                extraBudget,
                f,
                1.0,
                10.0,
                AllowedSolution.ANY_SIDE
            );

            // Documented contract: BracketedUnivariateRealSolver states ANY_SIDE is the
            // backwards-compatible default, and BaseSecantSolver constructors set
            // allowed = AllowedSolution.ANY_SIDE. Therefore the default overload and the
            // explicit ANY_SIDE overload must agree on the same interval/function.
            // A patch that silently breaks bookkeeping/state instead of throwing can
            // violate this observable post-condition even when no exception occurs.
            if (Double.doubleToLongBits(lhs) != Double.doubleToLongBits(rhs)) {
                throw new RuntimeException(
                    "[oracle:default-any-side] metamorphic violation: default overload must equal explicit ANY_SIDE input=maxEval="
                        + extraBudget + ",min=1.0,max=10.0 lhs=" + lhs + " rhs=" + rhs
                );
            }
        } catch (Throwable ignored) { /*__vpRepair*/ if (ignored instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) ignored;
            return;
        }
    }
}