package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final int degree = data.consumeInt(0, 8);
        final double[] coefficients = new double[degree + 1];
        for (int i = 0; i < coefficients.length; i++) {
            int selector = data.consumeInt(0, 11);
            int raw = data.consumeInt();
            switch (selector) {
                case 0:
                    coefficients[i] = 0.0;
                    break;
                case 1:
                    coefficients[i] = -0.0;
                    break;
                case 2:
                    coefficients[i] = raw;
                    break;
                case 3:
                    coefficients[i] = raw / 1024.0;
                    break;
                case 4:
                    coefficients[i] = raw * 1024.0;
                    break;
                case 5:
                    coefficients[i] = raw / 2147483648.0;
                    break;
                case 6:
                    coefficients[i] = Double.NaN;
                    break;
                case 7:
                    coefficients[i] = Double.POSITIVE_INFINITY;
                    break;
                case 8:
                    coefficients[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 9:
                    coefficients[i] = (raw & 1) == 0 ? 1.0 : -1.0;
                    break;
                case 10:
                    coefficients[i] = (double) data.consumeByte();
                    break;
                default:
                    coefficients[i] = raw == Integer.MIN_VALUE ? Integer.MAX_VALUE : -raw;
                    break;
            }
        }

        PolynomialFunction function = new PolynomialFunction(coefficients);

        BaseSecantSolver solver;
        switch (data.consumeInt(0, 2)) {
            case 0:
                solver = new IllinoisSolver();
                break;
            case 1:
                solver = new PegasusSolver();
                break;
            default:
                solver = new RegulaFalsiSolver();
                break;
        }

        double x0;
        switch (data.consumeInt(0, 9)) {
            case 0:
                x0 = 0.0;
                break;
            case 1:
                x0 = -0.0;
                break;
            case 2:
                x0 = data.consumeInt();
                break;
            case 3:
                x0 = data.consumeInt() / 1024.0;
                break;
            case 4:
                x0 = data.consumeByte();
                break;
            case 5:
                x0 = Double.NaN;
                break;
            case 6:
                x0 = Double.POSITIVE_INFINITY;
                break;
            case 7:
                x0 = Double.NEGATIVE_INFINITY;
                break;
            case 8:
                x0 = Double.MIN_VALUE;
                break;
            default:
                x0 = Double.MAX_VALUE;
                break;
        }

        double x1;
        switch (data.consumeInt(0, 9)) {
            case 0:
                x1 = 0.0;
                break;
            case 1:
                x1 = -0.0;
                break;
            case 2:
                x1 = data.consumeInt();
                break;
            case 3:
                x1 = data.consumeInt() / 1024.0;
                break;
            case 4:
                x1 = data.consumeByte();
                break;
            case 5:
                x1 = Double.NaN;
                break;
            case 6:
                x1 = Double.POSITIVE_INFINITY;
                break;
            case 7:
                x1 = Double.NEGATIVE_INFINITY;
                break;
            case 8:
                x1 = Double.MIN_VALUE;
                break;
            default:
                x1 = Double.MAX_VALUE;
                break;
        }

        int shape = data.consumeInt(0, 5);
        if (shape == 0) {
            x1 = x0;
        } else if (shape == 1) {
            double t = x0;
            x0 = x1;
            x1 = t;
        } else if (shape == 2) {
            x1 = x0 + 1.0;
        } else if (shape == 3) {
            x1 = x0 - 1.0;
        } else if (shape == 4) {
            x0 = -1.0;
            x1 = 1.0;
        }

        int maxEval = data.consumeInt(1, 1000);
        AllowedSolution[] allowedSolutions = AllowedSolution.values();
        AllowedSolution allowed = allowedSolutions[data.consumeInt(0, allowedSolutions.length - 1)];

        if (data.consumeBoolean()) {
            solver.solve(maxEval, function, x0, x1, allowed);
        } else {
            solver.solve(maxEval, function, x0, x1);
        }
    }
}