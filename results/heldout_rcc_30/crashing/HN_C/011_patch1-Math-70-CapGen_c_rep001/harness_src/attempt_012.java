package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int coeffLen1 = data.consumeInt(0, 8);
        double[] coeffs1 = new double[coeffLen1 == 0 ? 1 : coeffLen1];
        for (int i = 0; i < coeffs1.length; i++) {
            int v = data.consumeInt();
            switch (data.consumeInt(0, 7)) {
                case 0:
                    coeffs1[i] = 0.0;
                    break;
                case 1:
                    coeffs1[i] = -0.0;
                    break;
                case 2:
                    coeffs1[i] = v;
                    break;
                case 3:
                    coeffs1[i] = v / 1024.0;
                    break;
                case 4:
                    coeffs1[i] = (double) Integer.MAX_VALUE;
                    break;
                case 5:
                    coeffs1[i] = (double) Integer.MIN_VALUE;
                    break;
                case 6:
                    coeffs1[i] = 1.0;
                    break;
                default:
                    coeffs1[i] = -1.0;
                    break;
            }
        }

        int coeffLen2 = data.consumeInt(0, 8);
        double[] coeffs2 = new double[coeffLen2 == 0 ? 1 : coeffLen2];
        for (int i = 0; i < coeffs2.length; i++) {
            int v = data.consumeInt();
            switch (data.consumeInt(0, 7)) {
                case 0:
                    coeffs2[i] = 0.0;
                    break;
                case 1:
                    coeffs2[i] = -0.0;
                    break;
                case 2:
                    coeffs2[i] = v;
                    break;
                case 3:
                    coeffs2[i] = v / 2048.0;
                    break;
                case 4:
                    coeffs2[i] = 2.0;
                    break;
                case 5:
                    coeffs2[i] = -2.0;
                    break;
                case 6:
                    coeffs2[i] = (double) Integer.MAX_VALUE / 2.0;
                    break;
                default:
                    coeffs2[i] = (double) Integer.MIN_VALUE / 2.0;
                    break;
            }
        }

        PolynomialFunction f1 = new PolynomialFunction(coeffs1);
        PolynomialFunction f2 = new PolynomialFunction(coeffs2);

        double min;
        switch (data.consumeInt(0, 8)) {
            case 0:
                min = data.consumeInt();
                break;
            case 1:
                min = data.consumeInt() / 16.0;
                break;
            case 2:
                min = 0.0;
                break;
            case 3:
                min = -0.0;
                break;
            case 4:
                min = Double.NaN;
                break;
            case 5:
                min = Double.POSITIVE_INFINITY;
                break;
            case 6:
                min = Double.NEGATIVE_INFINITY;
                break;
            case 7:
                min = Double.MAX_VALUE;
                break;
            default:
                min = -Double.MAX_VALUE;
                break;
        }

        double max;
        switch (data.consumeInt(0, 8)) {
            case 0:
                max = data.consumeInt();
                break;
            case 1:
                max = data.consumeInt() / 16.0;
                break;
            case 2:
                max = min;
                break;
            case 3:
                max = -min;
                break;
            case 4:
                max = Double.NaN;
                break;
            case 5:
                max = Double.POSITIVE_INFINITY;
                break;
            case 6:
                max = Double.NEGATIVE_INFINITY;
                break;
            case 7:
                max = Double.MIN_VALUE;
                break;
            default:
                max = -Double.MIN_VALUE;
                break;
        }

        double initial;
        switch (data.consumeInt(0, 10)) {
            case 0:
                initial = data.consumeInt();
                break;
            case 1:
                initial = data.consumeInt() / 16.0;
                break;
            case 2:
                initial = min;
                break;
            case 3:
                initial = max;
                break;
            case 4:
                initial = (min + max) / 2.0;
                break;
            case 5:
                initial = Double.NaN;
                break;
            case 6:
                initial = Double.POSITIVE_INFINITY;
                break;
            case 7:
                initial = Double.NEGATIVE_INFINITY;
                break;
            case 8:
                initial = 0.0;
                break;
            case 9:
                initial = -0.0;
                break;
            default:
                initial = Double.MAX_VALUE;
                break;
        }

        BisectionSolver solver;
        if (data.consumeBoolean()) {
            solver = new BisectionSolver();
        } else {
            solver = new BisectionSolver(data.consumeBoolean() ? f1 : f2);
        }

        try {
            if (data.consumeBoolean()) {
                solver.solve(f1, min, max, initial);
            } else {
                solver.solve(f2, min, max, initial);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}