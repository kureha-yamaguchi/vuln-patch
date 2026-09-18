package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] v = new double[8];
        for (int i = 0; i < v.length; i++) {
            int mode = data.consumeInt(0, 9);
            int n = data.consumeInt();
            switch (mode) {
                case 0:
                    v[i] = (double) n;
                    break;
                case 1:
                    v[i] = ((double) n) / 1024.0;
                    break;
                case 2:
                    v[i] = 0.0;
                    break;
                case 3:
                    v[i] = -0.0;
                    break;
                case 4:
                    v[i] = 1.0;
                    break;
                case 5:
                    v[i] = -1.0;
                    break;
                case 6:
                    v[i] = Double.NaN;
                    break;
                case 7:
                    v[i] = Double.POSITIVE_INFINITY;
                    break;
                case 8:
                    v[i] = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    v[i] = ((double) (n % 1000000)) / 17.0;
                    break;
            }
        }

        double[] coefficients;
        int family = data.consumeInt(0, 5);
        switch (family) {
            case 0:
                coefficients = new double[] { v[0] };
                break;
            case 1:
                coefficients = new double[] { v[0], v[1] == 0.0 ? 1.0 : v[1] };
                break;
            case 2:
                coefficients = new double[] { -v[0], 1.0 };
                break;
            case 3:
                coefficients = new double[] { v[0], v[1], v[2] };
                break;
            case 4:
                coefficients = new double[] { v[0] * v[1], -(v[0] + v[1]), 1.0 };
                break;
            default:
                coefficients = new double[] { v[0], v[1], v[2], v[3] };
                break;
        }

        PolynomialFunction function = new PolynomialFunction(coefficients);

        double min = v[4];
        double max = v[5];
        double start = v[6];

        if ((family == 2 || family == 4) && data.consumeBoolean()) {
            double root = v[0];
            double width = (double) (data.consumeInt(0, 1000) + 1);
            min = root - width;
            max = root + width;
            start = root + (data.consumeBoolean() ? 0.0 : width / 2.0);
            if (data.consumeBoolean()) {
                double t = min;
                min = max;
                max = t;
            }
        } else if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }

        int maxEval = data.consumeInt(0, 1000);

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
        int allowedIndex = data.consumeInt();
        if (allowedIndex == Integer.MIN_VALUE) {
            allowedIndex = 0;
        }
        AllowedSolution allowed = allowedValues[Math.abs(allowedIndex) % allowedValues.length];

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