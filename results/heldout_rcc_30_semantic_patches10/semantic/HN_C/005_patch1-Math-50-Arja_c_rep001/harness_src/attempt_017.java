package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int coeffLen = data.consumeInt(0, 8);
        double[] coeffs = new double[coeffLen];
        for (int i = 0; i < coeffLen; i++) {
            int raw = data.consumeInt();
            switch (raw & 7) {
                case 0:
                    coeffs[i] = 0.0;
                    break;
                case 1:
                    coeffs[i] = -0.0;
                    break;
                case 2:
                    coeffs[i] = Double.NaN;
                    break;
                case 3:
                    coeffs[i] = Double.POSITIVE_INFINITY;
                    break;
                case 4:
                    coeffs[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 5:
                    coeffs[i] = (double) Integer.MIN_VALUE;
                    break;
                case 6:
                    coeffs[i] = (double) Integer.MAX_VALUE;
                    break;
                default:
                    coeffs[i] = (double) raw / (double) ((Math.abs(raw % 31)) + 1);
                    break;
            }
        }

        org.apache.commons.math.analysis.polynomials.PolynomialFunction function =
                new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);

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

        double min;
        switch (data.consumeInt(0, 7)) {
            case 0:
                min = data.consumeInt();
                break;
            case 1:
                min = Double.NaN;
                break;
            case 2:
                min = Double.POSITIVE_INFINITY;
                break;
            case 3:
                min = Double.NEGATIVE_INFINITY;
                break;
            case 4:
                min = 0.0;
                break;
            case 5:
                min = -0.0;
                break;
            case 6:
                min = Double.MIN_VALUE;
                break;
            default:
                min = -Double.MIN_VALUE;
                break;
        }

        double max;
        switch (data.consumeInt(0, 7)) {
            case 0:
                max = data.consumeInt();
                break;
            case 1:
                max = Double.NaN;
                break;
            case 2:
                max = Double.POSITIVE_INFINITY;
                break;
            case 3:
                max = Double.NEGATIVE_INFINITY;
                break;
            case 4:
                max = 0.0;
                break;
            case 5:
                max = -0.0;
                break;
            case 6:
                max = Double.MAX_VALUE;
                break;
            default:
                max = -Double.MAX_VALUE;
                break;
        }

        double start;
        switch (data.consumeInt(0, 7)) {
            case 0:
                start = data.consumeInt();
                break;
            case 1:
                start = min;
                break;
            case 2:
                start = max;
                break;
            case 3:
                start = 0.5 * (min + max);
                break;
            case 4:
                start = Double.NaN;
                break;
            case 5:
                start = Double.POSITIVE_INFINITY;
                break;
            case 6:
                start = Double.NEGATIVE_INFINITY;
                break;
            default:
                start = 0.0;
                break;
        }

        int maxEval = data.consumeInt(0, 10000);

        AllowedSolution allowed;
        switch (data.consumeInt(0, 4)) {
            case 0:
                allowed = AllowedSolution.ANY_SIDE;
                break;
            case 1:
                allowed = AllowedSolution.LEFT_SIDE;
                break;
            case 2:
                allowed = AllowedSolution.RIGHT_SIDE;
                break;
            case 3:
                allowed = AllowedSolution.BELOW_SIDE;
                break;
            default:
                allowed = AllowedSolution.ABOVE_SIDE;
                break;
        }

        switch (data.consumeInt(0, 2)) {
            case 0:
                solver.solve(maxEval, function, min, max);
                break;
            case 1:
                solver.solve(maxEval, function, min, max, start);
                break;
            default:
                solver.solve(maxEval, function, min, max, allowed);
                break;
        }

        if (data.consumeBoolean()) {
            BaseSecantSolver solver2;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    solver2 = new RegulaFalsiSolver();
                    break;
                case 1:
                    solver2 = new IllinoisSolver();
                    break;
                default:
                    solver2 = new PegasusSolver();
                    break;
            }

            double min2 = data.consumeBoolean() ? max : min;
            double max2 = data.consumeBoolean() ? min : max;
            AllowedSolution allowed2;
            switch (data.consumeInt(0, 4)) {
                case 0:
                    allowed2 = AllowedSolution.ANY_SIDE;
                    break;
                case 1:
                    allowed2 = AllowedSolution.LEFT_SIDE;
                    break;
                case 2:
                    allowed2 = AllowedSolution.RIGHT_SIDE;
                    break;
                case 3:
                    allowed2 = AllowedSolution.BELOW_SIDE;
                    break;
                default:
                    allowed2 = AllowedSolution.ABOVE_SIDE;
                    break;
            }
            solver2.solve(data.consumeInt(0, 10000), function, min2, max2, allowed2);
        }
    }
}