package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        data.consumeRemainingAsBytes();

        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        try {
            final double root = solver.solve(3624, f, 1.0, 10.0);

            // Lifted oracle from RegulaFalsiSolverTest.testIssue631:
            // for this exact valid input, a correct implementation must throw
            // TooManyEvaluationsException instead of returning normally.
            // The buggy implementation reaches BaseSecantSolver.doSolve(),
            // takes the REGULA_FALSI x == x1 path repeatedly, and returns the
            // pinned wrong value below instead of exhausting evaluations.
            final double expectedBuggyRoot = 3.4341896575482003d;
            final double diff = Math.abs(root - expectedBuggyRoot);

            if (diff <= 1e-15d) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-pinned] semantic mismatch: expected TooManyEvaluationsException but solve returned pinned buggy root="
                        + root);
            }

            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-nonthrow] semantic mismatch: expected TooManyEvaluationsException but solve returned root="
                    + root);
        } catch (TooManyEvaluationsException expectedOnFixed) {
            return;
        }

    }
}