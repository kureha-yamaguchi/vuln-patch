package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int maxEval = Math.max(1, data.consumeInt(1, 256));

        double root = data.consumeInt(-10000, 10000) / 16.0;
        double deltaA = data.consumeInt(-10000, 10000) / 16.0;
        double deltaB = data.consumeInt(-10000, 10000) / 16.0;
        double scale = data.consumeInt(-1000, 1000) / 32.0;
        if (scale == 0.0) {
            scale = data.consumeBoolean() ? 1.0 : -1.0;
        }

        double min = root + deltaA;
        double max = root + deltaB;
        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }

        double start = data.consumeBoolean()
                ? root
                : (data.consumeInt(-10000, 10000) / 16.0);

        AllowedSolution[] allowedValues = AllowedSolution.values();
        AllowedSolution allowed = allowedValues[Math.floorMod(data.consumeInt(), allowedValues.length)];

        int functionKind = data.consumeInt(0, 4);
        org.apache.commons.math.analysis.UnivariateRealFunction function;
        switch (functionKind) {
            case 0:
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] { -scale * root, scale });
                break;
            case 1:
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] { scale * root * root, -2.0 * scale * root, scale });
                break;
            case 2:
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] {
                                -scale * root * root * root,
                                3.0 * scale * root * root,
                                -3.0 * scale * root,
                                scale
                        });
                break;
            case 3:
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] { 0.0 });
                break;
            default:
                int extra = data.consumeInt(0, 5);
                byte[] raw = data.consumeBytes(extra + 1);
                double[] coeffs = new double[raw.length + 1];
                coeffs[0] = -scale * root;
                for (int i = 0; i < raw.length; i++) {
                    coeffs[i + 1] = raw[i];
                }
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);
                break;
        }

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

        if (data.consumeBoolean()) {
            solver.solve(maxEval, function, min, max, allowed);
        } else {
            solver.solve(maxEval, function, min, max, start, allowed);
        }
    }
}