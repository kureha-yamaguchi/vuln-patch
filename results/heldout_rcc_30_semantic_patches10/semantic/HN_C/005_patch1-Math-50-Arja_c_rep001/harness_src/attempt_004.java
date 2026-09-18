package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

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

        AllowedSolution[] allowedValues = AllowedSolution.values();
        AllowedSolution allowed = allowedValues[data.consumeInt(0, allowedValues.length - 1)];

        org.apache.commons.math.analysis.UnivariateRealFunction function;
        double min;
        double max;

        switch (data.consumeInt(0, 3)) {
            case 0: {
                function = new org.apache.commons.math.analysis.SinFunction();
                int k = data.consumeInt(-3, 3);
                double center = k * Math.PI;
                double leftSpan = data.consumeInt(0, 1000) / 100.0;
                double rightSpan = data.consumeInt(0, 1000) / 100.0;
                min = center - leftSpan;
                max = center + rightSpan;
                break;
            }
            case 1: {
                function = new org.apache.commons.math.analysis.SincFunction();
                double leftSpan = data.consumeInt(0, 1000) / 100.0;
                double rightSpan = data.consumeInt(0, 1000) / 100.0;
                min = -leftSpan;
                max = rightSpan;
                break;
            }
            case 2: {
                function = new org.apache.commons.math.analysis.QuinticFunction();
                double center;
                switch (data.consumeInt(0, 4)) {
                    case 0:
                        center = -1.0;
                        break;
                    case 1:
                        center = -0.5;
                        break;
                    case 2:
                        center = 0.0;
                        break;
                    case 3:
                        center = 0.5;
                        break;
                    default:
                        center = 1.0;
                        break;
                }
                double leftSpan = data.consumeInt(0, 1000) / 1000.0;
                double rightSpan = data.consumeInt(0, 1000) / 1000.0;
                min = center - leftSpan;
                max = center + rightSpan;
                break;
            }
            default: {
                int len = data.consumeInt(1, 6);
                double[] coeffs = new double[len];
                for (int i = 0; i < len; i++) {
                    coeffs[i] = data.consumeInt(-1000, 1000) / 100.0;
                }
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);
                min = data.consumeInt(-1000, 1000) / 100.0;
                max = data.consumeInt(-1000, 1000) / 100.0;
                break;
            }
        }

        if (data.consumeBoolean()) {
            if (min > max) {
                double t = min;
                min = max;
                max = t;
            }
        } else {
            if (min < max) {
                double t = min;
                min = max;
                max = t;
            }
        }

        int maxEval = data.consumeInt(1, 10000);
        solver.solve(maxEval, function, min, max, allowed);

        BaseSecantSolver targetedSolver;
        switch (data.consumeInt(0, 2)) {
            case 0:
                targetedSolver = new RegulaFalsiSolver();
                break;
            case 1:
                targetedSolver = new IllinoisSolver();
                break;
            default:
                targetedSolver = new PegasusSolver();
                break;
        }

        org.apache.commons.math.analysis.UnivariateRealFunction targetedFunction;
        double tMin;
        double tMax;
        switch (data.consumeInt(0, 2)) {
            case 0:
                targetedFunction = new org.apache.commons.math.analysis.QuinticFunction();
                tMin = 0.2 + (data.consumeInt(-200, 200) / 1000.0);
                tMax = 0.8 + (data.consumeInt(-200, 200) / 1000.0);
                break;
            case 1:
                targetedFunction = new org.apache.commons.math.analysis.SinFunction();
                tMin = -1.0 - (data.consumeInt(0, 1000) / 1000.0);
                tMax = 1.0 + (data.consumeInt(0, 1000) / 1000.0);
                break;
            default:
                targetedFunction = new org.apache.commons.math.analysis.SincFunction();
                tMin = -1.0 - (data.consumeInt(0, 1000) / 1000.0);
                tMax = 1.0 + (data.consumeInt(0, 1000) / 1000.0);
                break;
        }

        if (data.consumeBoolean() && tMin > tMax) {
            double t = tMin;
            tMin = tMax;
            tMax = t;
        }

        targetedSolver.solve(data.consumeInt(1, 10000), targetedFunction, tMin, tMax,
                allowedValues[data.consumeInt(0, allowedValues.length - 1)]);
    }
}