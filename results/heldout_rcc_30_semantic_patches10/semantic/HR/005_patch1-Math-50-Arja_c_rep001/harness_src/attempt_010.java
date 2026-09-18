package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int tweak = data.consumeInt(-2, 2);
        final int maxEval = 3624 + tweak;

        final UnivariateRealFunction f = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - Math.pow(Math.PI, 3.0);
            }
        };

        final RegulaFalsiSolver solver = new RegulaFalsiSolver();

        final double root;
        try {
            root = solver.solve(maxEval, f, 1.0, 10.0);
        } catch (TooManyEvaluationsException expectedOnFixed) {
            if (maxEval == 3624) {
                return;
            }
            throw expectedOnFixed;
        }

        if (maxEval == 3624) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:issue631-exact] semantic mismatch: expected TooManyEvaluationsException for solve(3624, f, 1, 10) but got root=" + root);
        }

        final double expectedRoot = Math.log(Math.pow(Math.PI, 3.0));
        if (Math.abs(root - expectedRoot) > 1e-6) {
            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:known-root-nearby] semantic mismatch: maxEval=" + maxEval +
                " expectedApprox=" + expectedRoot + " actual=" + root);
        }
    }
}