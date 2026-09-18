package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.analysis.UnivariateRealFunction function;
        if (data.consumeBoolean()) {
            function = new org.apache.commons.math.analysis.SinFunction();
        } else {
            function = new org.apache.commons.math.analysis.QuinticFunction();
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

        AllowedSolution allowed =
                AllowedSolution.values()[data.consumeInt(0, AllowedSolution.values().length - 1)];

        double root;
        if (function instanceof org.apache.commons.math.analysis.SinFunction) {
            switch (data.consumeInt(0, 4)) {
                case 0:
                    root = 0.0;
                    break;
                case 1:
                    root = Math.PI;
                    break;
                case 2:
                    root = -Math.PI;
                    break;
                case 3:
                    root = 2.0 * Math.PI;
                    break;
                default:
                    root = -2.0 * Math.PI;
                    break;
            }
        } else {
            switch (data.consumeInt(0, 4)) {
                case 0:
                    root = -1.0;
                    break;
                case 1:
                    root = -0.5;
                    break;
                case 2:
                    root = 0.0;
                    break;
                case 3:
                    root = 0.5;
                    break;
                default:
                    root = 1.0;
                    break;
            }
        }

        double spanA;
        double spanB;
        if (function instanceof org.apache.commons.math.analysis.SinFunction) {
            spanA = data.consumeInt(0, 100000) / 10000.0;
            spanB = data.consumeInt(0, 100000) / 10000.0;
        } else {
            spanA = data.consumeInt(0, 200000) / 100000.0;
            spanB = data.consumeInt(0, 200000) / 100000.0;
        }

        double min;
        double max;
        switch (data.consumeInt(0, 7)) {
            case 0:
                min = root - spanA;
                max = root + spanB;
                break;
            case 1:
                min = root;
                max = root + spanB;
                break;
            case 2:
                min = root - spanA;
                max = root;
                break;
            case 3:
                min = root;
                max = root;
                break;
            case 4:
                min = root + spanA;
                max = root - spanB;
                break;
            case 5:
                min = root + spanA + 0.01;
                max = root + spanA + spanB + 0.02;
                break;
            case 6:
                min = root - spanA - spanB - 0.02;
                max = root - spanA - 0.01;
                break;
            default:
                min = -spanA;
                max = spanB;
                break;
        }

        int maxEval = data.consumeInt(1, 10000);

        if (data.consumeBoolean()) {
            solver.solve(maxEval, function, min, max);
        } else {
            solver.solve(maxEval, function, min, max, allowed);
        }
    }
}