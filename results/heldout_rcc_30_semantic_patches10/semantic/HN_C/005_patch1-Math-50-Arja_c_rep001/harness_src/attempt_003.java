package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    private static double boundedDoubleFromInt(int v, double scale) {
        return ((double) v) / scale;
    }

    private static org.apache.commons.math.analysis.polynomials.PolynomialFunction linearWithRoot(double root, double slope) {
        return new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                new double[] { -slope * root, slope });
    }

    private static org.apache.commons.math.analysis.polynomials.PolynomialFunction cubicWithRoot(double root) {
        double a0 = -root * root * root;
        double a1 = 3.0 * root * root;
        double a2 = -3.0 * root;
        double a3 = 1.0;
        return new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                new double[] { a0, a1, a2, a3 });
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double absAcc = Math.max(1.0e-12, Math.abs(boundedDoubleFromInt(data.consumeInt(), 1000000000.0)));
        double relAcc = Math.max(1.0e-14, Math.abs(boundedDoubleFromInt(data.consumeInt(), 1000000000.0)));

        BaseSecantSolver solver;
        switch (data.consumeInt(0, 2)) {
            case 0:
                solver = data.consumeBoolean()
                        ? new RegulaFalsiSolver(absAcc)
                        : new RegulaFalsiSolver(relAcc, absAcc);
                break;
            case 1:
                solver = data.consumeBoolean()
                        ? new IllinoisSolver(absAcc)
                        : new IllinoisSolver(relAcc, absAcc);
                break;
            default:
                solver = data.consumeBoolean()
                        ? new PegasusSolver(absAcc)
                        : new PegasusSolver(relAcc, absAcc);
                break;
        }

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

        org.apache.commons.math.analysis.UnivariateRealFunction function;
        double root = boundedDoubleFromInt(data.consumeInt(), 1024.0);
        int functionKind = data.consumeInt(0, 3);
        if (functionKind == 0) {
            double slope = boundedDoubleFromInt(data.consumeInt(), 1024.0);
            if (slope == 0.0) {
                slope = data.consumeBoolean() ? 1.0 : -1.0;
            }
            function = linearWithRoot(root, slope);
        } else if (functionKind == 1) {
            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                    new double[] { -root, 1.0, 0.0, 0.0 });
        } else if (functionKind == 2) {
            function = cubicWithRoot(root);
        } else {
            double root2 = root + Math.max(1.0e-6, Math.abs(boundedDoubleFromInt(data.consumeInt(), 4096.0)));
            double c0 = root * root2;
            double c1 = -(root + root2);
            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                    new double[] { c0, c1, 1.0 });
        }

        double delta = Math.max(1.0e-12, Math.abs(boundedDoubleFromInt(data.consumeInt(), 4096.0)));
        double min;
        double max;

        switch (data.consumeInt(0, 4)) {
            case 0:
                min = root - delta;
                max = root + delta;
                break;
            case 1:
                min = root;
                max = root + delta;
                break;
            case 2:
                min = root - delta;
                max = root;
                break;
            case 3:
                min = root - (2.0 * delta);
                max = root + delta;
                break;
            default:
                min = root - delta;
                max = root + (2.0 * delta);
                break;
        }

        if (!(min < max)) {
            double t = min;
            min = max;
            max = t;
            if (!(min < max)) {
                max = min + 1.0e-12;
            }
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
                start = min + (max - min) * 0.5;
                break;
            default:
                start = min + (max - min) * (Math.abs(data.consumeInt(0, 1000000)) / 1000000.0);
                break;
        }

        if (start < min) {
            start = min;
        } else if (start > max) {
            start = max;
        }

        int maxEval = data.consumeInt(1, 100000);

        if (data.consumeBoolean()) {
            solver.solve(maxEval, function, min, max, allowed);
        } else {
            solver.solve(maxEval, function, min, max, start, allowed);
        }
    }
}