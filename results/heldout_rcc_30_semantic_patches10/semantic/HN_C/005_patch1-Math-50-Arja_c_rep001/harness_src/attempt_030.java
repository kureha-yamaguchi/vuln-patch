package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int coeffLen = data.consumeInt(1, 8);
        double[] coeffs = new double[coeffLen];
        for (int i = 0; i < coeffLen; i++) {
            coeffs[i] = data.consumeInt(-1000, 1000);
        }

        if (data.consumeBoolean()) {
            coeffs[0] = data.consumeInt(-10, 10);
        }
        if (coeffLen > 1 && data.consumeBoolean()) {
            coeffs[1] = data.consumeInt(-10, 10);
        }
        if (coeffLen > 2 && data.consumeBoolean()) {
            coeffs[coeffLen - 1] = data.consumeInt(-3, 3);
        }

        UnivariateRealFunction function = new PolynomialFunction(coeffs);

        double min = data.consumeInt(-1000, 1000);
        double max = data.consumeInt(-1000, 1000);
        double start = data.consumeInt(-1000, 1000);

        if (data.consumeBoolean()) {
            max = min;
        }
        if (data.consumeBoolean()) {
            start = min;
        }
        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }

        AllowedSolution[] allowedValues = AllowedSolution.values();
        AllowedSolution allowed = allowedValues[data.consumeInt(0, allowedValues.length - 1)];

        double absoluteAccuracy = Math.abs((double) data.consumeInt(-1000, 1000));
        double relativeAccuracy = Math.abs((double) data.consumeInt(-1000, 1000));
        double functionValueAccuracy = Math.abs((double) data.consumeInt(-1000, 1000));

        BaseSecantSolver solver;
        switch (data.consumeInt(0, 2)) {
            case 0:
                if (data.consumeBoolean()) {
                    solver = new RegulaFalsiSolver();
                } else if (data.consumeBoolean()) {
                    solver = new RegulaFalsiSolver(absoluteAccuracy);
                } else if (data.consumeBoolean()) {
                    solver = new RegulaFalsiSolver(relativeAccuracy, absoluteAccuracy);
                } else {
                    solver = new RegulaFalsiSolver(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
                }
                break;
            case 1:
                if (data.consumeBoolean()) {
                    solver = new IllinoisSolver();
                } else if (data.consumeBoolean()) {
                    solver = new IllinoisSolver(absoluteAccuracy);
                } else if (data.consumeBoolean()) {
                    solver = new IllinoisSolver(relativeAccuracy, absoluteAccuracy);
                } else {
                    solver = new IllinoisSolver(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
                }
                break;
            default:
                if (data.consumeBoolean()) {
                    solver = new PegasusSolver();
                } else if (data.consumeBoolean()) {
                    solver = new PegasusSolver(absoluteAccuracy);
                } else if (data.consumeBoolean()) {
                    solver = new PegasusSolver(relativeAccuracy, absoluteAccuracy);
                } else {
                    solver = new PegasusSolver(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
                }
                break;
        }

        int maxEval = data.consumeInt(1, 1000);

        switch (data.consumeInt(0, 3)) {
            case 0:
                solver.solve(maxEval, function, min, max);
                break;
            case 1:
                solver.solve(maxEval, function, min, max, start);
                break;
            case 2:
                solver.solve(maxEval, function, min, max, allowed);
                break;
            default:
                solver.solve(maxEval, function, min, max, start, allowed);
                break;
        }
    }
}