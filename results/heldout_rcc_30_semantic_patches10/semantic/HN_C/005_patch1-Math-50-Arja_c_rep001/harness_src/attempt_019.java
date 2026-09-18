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

        int functionKind = data.consumeInt(0, 4);
        double[] coeffs;
        double root = 0.0;

        switch (functionKind) {
            case 0: {
                int aInt = data.consumeInt(-1000, 1000);
                if (aInt == 0) {
                    aInt = data.consumeBoolean() ? 1 : -1;
                }
                int bInt = data.consumeInt(-1000, 1000);
                double a = aInt;
                double b = bInt;
                coeffs = new double[] { b, a };
                root = -b / a;
                break;
            }
            case 1: {
                int rInt = data.consumeInt(-1000, 1000);
                root = rInt;
                coeffs = new double[] { -root, 1.0, -root, 1.0 };
                break;
            }
            case 2: {
                int rInt = data.consumeInt(-1000, 1000);
                root = rInt;
                coeffs = new double[] { root * root, -2.0 * root, 1.0 };
                break;
            }
            case 3: {
                if (data.consumeBoolean()) {
                    coeffs = new double[] { 0.0 };
                    root = 0.0;
                } else {
                    coeffs = new double[] { data.consumeInt(-1000, 1000) };
                    root = 0.0;
                }
                break;
            }
            default: {
                int degree = data.consumeInt(0, 6);
                coeffs = new double[degree + 1];
                for (int i = 0; i < coeffs.length; i++) {
                    coeffs[i] = data.consumeInt(-50, 50);
                }
                if (coeffs.length == 0) {
                    coeffs = new double[] { 0.0 };
                }
                root = data.consumeInt(-100, 100);
                break;
            }
        }

        UnivariateRealFunction f = new PolynomialFunction(coeffs);

        double leftWidth = data.consumeInt(0, 1000);
        double rightWidth = data.consumeInt(0, 1000);
        double min = root - leftWidth;
        double max = root + rightWidth;

        switch (data.consumeInt(0, 5)) {
            case 0:
                min = root;
                break;
            case 1:
                max = root;
                break;
            case 2:
                min = root;
                max = root;
                break;
            case 3:
                min = root - 1.0;
                max = root + 1.0;
                break;
            case 4:
                min = root + 1.0;
                max = root - 1.0;
                break;
            default:
                break;
        }

        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }

        double start;
        switch (data.consumeInt(0, 4)) {
            case 0:
                start = min;
                break;
            case 1:
                start = max;
                break;
            case 2:
                start = root;
                break;
            case 3:
                start = 0.5 * (min + max);
                break;
            default:
                start = data.consumeInt(-2000, 2000);
                break;
        }

        int maxEval = data.consumeInt(0, 200);

        switch (data.consumeInt(0, 5)) {
            case 0:
                solver.solve(maxEval, f, min, max);
                break;
            case 1:
                solver.solve(maxEval, f, min, max, start);
                break;
            case 2:
                solver.solve(maxEval, f, min, max, AllowedSolution.ANY_SIDE);
                break;
            case 3:
                solver.solve(maxEval, f, min, max, AllowedSolution.LEFT_SIDE);
                break;
            case 4:
                solver.solve(maxEval, f, min, max, AllowedSolution.RIGHT_SIDE);
                break;
            default:
                solver.solve(
                    maxEval,
                    f,
                    min,
                    max,
                    data.consumeBoolean() ? AllowedSolution.BELOW_SIDE : AllowedSolution.ABOVE_SIDE
                );
                break;
        }
    }
}