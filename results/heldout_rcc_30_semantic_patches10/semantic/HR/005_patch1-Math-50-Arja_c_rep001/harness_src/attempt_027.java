package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.exception.TooManyEvaluationsException;

public class FuzzHarness {
    private static final UnivariateRealFunction ISSUE631_FUNCTION = new UnivariateRealFunction() {
        public double value(double x) {
            return Math.exp(x) - Math.pow(Math.PI, 3.0);
        }
    };

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            UnivariateRealSolver solver = new RegulaFalsiSolver();
            double root = solver.solve(3624, ISSUE631_FUNCTION, 1.0, 10.0);

            if (Math.abs(root - 3.4341896575482003) <= 1e-15) {
                throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                    "[oracle:issue631-missing-throw] semantic mismatch: expected TooManyEvaluationsException but solver returned root=" + root);
            }

            throw new com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow(
                "[oracle:issue631-wrong-return] semantic mismatch: expected TooManyEvaluationsException but solver returned unexpected value=" + root);
        } catch (TooManyEvaluationsException expectedOnFixedBuild) {
        }

        double root = data.consumeInt(2000, 4000) / 1000.0;
        double min = root - data.consumeInt(1, 1000) / 1000.0;
        double max = root + data.consumeInt(1, 1000) / 1000.0;
        if (!(min < root && root < max)) {
            return;
        }

        final double chosenRoot = root;
        final double target = Math.exp(chosenRoot);
        final double scale = data.consumeBoolean() ? 2.0 : 0.5;
        final int maxEval = data.consumeInt(50, 2000);
        final double absAcc = 1.0e-6;

        UnivariateRealFunction f1 = new UnivariateRealFunction() {
            public double value(double x) {
                return Math.exp(x) - target;
            }
        };
        UnivariateRealFunction f2 = new UnivariateRealFunction() {
            public double value(double x) {
                return scale * (Math.exp(x) - target);
            }
        };

        RegulaFalsiSolver s1 = new RegulaFalsiSolver(absAcc);
        RegulaFalsiSolver s2 = new RegulaFalsiSolver(absAcc);

        final double r1;
        final double r2;
        try {
            r1 = s1.solve(maxEval, f1, min, max);
            r2 = s2.solve(maxEval, f2, min, max);
        } catch (RuntimeException ex) {
            return;
        }

        double tol = 64.0 * absAcc;
        if (Math.abs(r1 - r2) > tol) {
            throw new RuntimeException(
                "[oracle:scale-invariance] metamorphic violation: positive scaling changed root inputRoot="
                    + chosenRoot + " min=" + min + " max=" + max + " lhs=" + r1 + " rhs=" + r2 + " tol=" + tol);
        }
    }
}