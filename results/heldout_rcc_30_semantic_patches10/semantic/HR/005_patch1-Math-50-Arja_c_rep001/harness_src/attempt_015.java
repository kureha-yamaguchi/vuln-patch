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

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        try {
            final double root = solver.solve(3624, f, 1.0, 10.0);

            /*
             * Exact lifted oracle from RegulaFalsiSolverTest.testIssue631:
             * for this real API call, a correct implementation must throw
             * TooManyEvaluationsException instead of returning normally.
             * The buggy build returns 3.4341896575482003 here because the
             * REGULA_FALSI x == x1 stagnation case is not repaired.
             */
            throw new FuzzerSecurityIssueLow(
                "[oracle:issue631-exact] semantic mismatch: expected TooManyEvaluationsException, but solve returned root=" + root);
        } catch (TooManyEvaluationsException expected) {
            return;
        }
    }
}