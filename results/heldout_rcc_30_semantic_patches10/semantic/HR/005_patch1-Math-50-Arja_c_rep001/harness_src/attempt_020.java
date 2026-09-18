package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    private static final UnivariateRealFunction ISSUE631_FUNCTION = new UnivariateRealFunction() {
        public double value(double x) {
            return Math.exp(x) - Math.pow(Math.PI, 3.0);
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        final int maxEval;
        if (data.remainingBytes() > 0 && data.consumeBoolean()) {
            maxEval = 3624;
        } else {
            maxEval = 3624;
        }

        try {
            final double root = solver.solve(maxEval, ISSUE631_FUNCTION, 1.0, 10.0);

            /*
             * Lifted from RegulaFalsiSolverTest.testIssue631:
             * for this exact function/interval/budget, correct behavior is to throw
             * TooManyEvaluationsException instead of returning a root. The buggy build
             * reaches BaseSecantSolver.doSolve's REGULA_FALSI x == x1 path and returns
             * 3.4341896575482003 instead.
             */
            if (Math.abs(root - 3.4341896575482003) <= 1e-15) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-exact] semantic mismatch: expected TooManyEvaluationsException but solve returned pinned buggy root "
                        + root + " for maxEval=" + maxEval);
            }

            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-return] semantic mismatch: expected TooManyEvaluationsException but solve returned "
                    + root + " for maxEval=" + maxEval);
        } catch (TooManyEvaluationsException expectedOnFixedBuild) {
            /*
             * Mandatory post-condition/metamorphic check:
             * A solver with a smaller evaluation budget cannot succeed if the same
             * deterministic call with budget 3624 already requires more evaluations
             * and throws. This boundary-neighbor check flips the patched condition's
             * context and still reaches the same real code path.
             */
            final int smallerBudget = 3623;
            final double root2 = solver.solve(smallerBudget, ISSUE631_FUNCTION, 1.0, 10.0);
            throw new FuzzerSecurityIssueLow(
                "[oracle:budget-neighbor] metamorphic violation: solve returned for smaller budget after larger budget threw "
                    + "maxEval1=" + maxEval + " maxEval2=" + smallerBudget + " returnedRoot=" + root2, expectedOnFixedBuild);
        }
    }
}