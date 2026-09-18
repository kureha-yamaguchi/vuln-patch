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

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int jitter = data.consumeInt(-2, 2);
        int maxEval = 3624 + jitter;
        if (maxEval < 1) {
            maxEval = 1;
        }

        RegulaFalsiSolver solver = new RegulaFalsiSolver();

        try {
            double root = solver.solve(maxEval, ISSUE631_FUNCTION, 1.0, 10.0);

            if (maxEval == 3624) {
                double expected = 3.4341896575482003d;
                double diff = Math.abs(root - expected);
                if (diff <= 1e-15) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:issue631-nonthrow] semantic mismatch: expected TooManyEvaluationsException but solver returned root=" +
                        root + " for maxEval=" + maxEval);
                }
                throw new FuzzerSecurityIssueLow(
                    "[oracle:issue631-wrong-return] semantic mismatch: expected TooManyEvaluationsException but solver returned root=" +
                    root + " expectedPinnedWrongRoot=" + expected + " diff=" + diff + " maxEval=" + maxEval);
            }
        } catch (TooManyEvaluationsException expectedOnPatchedBuild) {
            if (maxEval == 3624) {
                return;
            }
        }

        if (maxEval != 3624) {
            try {
                double explicitStartRoot = solver.solve(maxEval, ISSUE631_FUNCTION, 1.0, 10.0, 1.0, AllowedSolution.ANY_SIDE);
                double plainRoot = solver.solve(maxEval, ISSUE631_FUNCTION, 1.0, 10.0);

                double tol = 16.0 * Math.max(solver.getAbsoluteAccuracy(),
                        solver.getRelativeAccuracy() * Math.max(Math.abs(explicitStartRoot), Math.abs(plainRoot)));

                if (Math.abs(explicitStartRoot - plainRoot) > tol) {
                    throw new FuzzerSecurityIssueLow(
                        "[oracle:overload-consistency-new] consistency violation: plainRoot=" + plainRoot +
                        " explicitStartRoot=" + explicitStartRoot + " tol=" + tol + " maxEval=" + maxEval);
                }
            } catch (TooManyEvaluationsException ignored) {
                return;
            }
        }
    }
}