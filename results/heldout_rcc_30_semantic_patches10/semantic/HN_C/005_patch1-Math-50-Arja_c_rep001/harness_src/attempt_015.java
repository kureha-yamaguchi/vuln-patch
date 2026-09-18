package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BaseSecantSolver solver;
        switch (data.consumeInt(0, 2)) {
            case 0:
                solver = new RegulaFalsiSolver();
                break;
            case 1:
                solver = new IllinoisSolver();
                break;
            default:
                solver = new PegasusSolver();
                break;
        }

        AllowedSolution[] allowedValues = new AllowedSolution[] {
            AllowedSolution.ANY_SIDE,
            AllowedSolution.LEFT_SIDE,
            AllowedSolution.RIGHT_SIDE,
            AllowedSolution.BELOW_SIDE,
            AllowedSolution.ABOVE_SIDE
        };
        AllowedSolution allowed = allowedValues[data.consumeInt(0, allowedValues.length - 1)];

        int maxEval = data.consumeInt(1, 10000);

        double root = data.consumeInt(-100000, 100000) / 256.0;
        double delta = (data.consumeInt(0, 4096) + 1) / 256.0;
        double start = data.consumeInt(-100000, 100000) / 256.0;

        UnivariateRealFunction linear = new PolynomialFunction(new double[] { -root, 1.0 });
        UnivariateRealFunction quadratic = new PolynomialFunction(new double[] { -2.0, 0.0, 1.0 });

        int coeffLen = data.consumeInt(1, 8);
        double[] coeffs = new double[coeffLen];
        boolean anyNonZero = false;
        for (int i = 0; i < coeffLen; i++) {
            coeffs[i] = data.consumeInt(-50, 50);
            if (coeffs[i] != 0.0) {
                anyNonZero = true;
            }
        }
        if (!anyNonZero) {
            coeffs[coeffLen - 1] = 1.0;
        }
        UnivariateRealFunction fuzzPoly = new PolynomialFunction(coeffs);

        double a = data.consumeInt(-100000, 100000) / 256.0;
        double b = data.consumeInt(-100000, 100000) / 256.0;
        double min = Math.min(a, b);
        double max = Math.max(a, b);

        switch (data.consumeInt(0, 8)) {
            case 0:
                solver.solve(maxEval, linear, root - delta, root + delta, allowed);
                break;
            case 1:
                solver.solve(maxEval, linear, root, root + delta, allowed);
                break;
            case 2:
                solver.solve(maxEval, linear, root - delta, root, allowed);
                break;
            case 3:
                solver.solve(maxEval, linear, root - delta, root + delta, start, allowed);
                break;
            case 4:
                solver.solve(maxEval, quadratic, 0.0, 2.0, allowed);
                break;
            case 5:
                solver.solve(maxEval, quadratic, -2.0, 0.0, allowed);
                break;
            case 6:
                solver.solve(maxEval, fuzzPoly, min, max, allowed);
                break;
            case 7:
                solver.solve(maxEval, fuzzPoly, a, b, start, allowed);
                break;
            default:
                if (data.consumeBoolean()) {
                    solver.solve(maxEval, linear, root - delta, root + delta);
                } else {
                    solver.solve(maxEval, fuzzPoly, min, max);
                }
                break;
        }
    }
}