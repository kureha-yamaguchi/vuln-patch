package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        try {
            double root = solver.solve(3624, f, 1.0, 10.0);
            throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-pinned] semantic mismatch: expected TooManyEvaluationsException but solve returned root=" + root);
        } catch (TooManyEvaluationsException expectedOnFixed) {
            return;
        }
    }
}