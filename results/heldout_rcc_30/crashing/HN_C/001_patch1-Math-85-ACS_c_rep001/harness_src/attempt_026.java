package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int slopeSeed = data.consumeInt();
        if (slopeSeed == 0) {
            slopeSeed = 1;
        }
        int interceptSeed = data.consumeInt();
        int widthLeftSeed = data.consumeInt(1, 32);
        int widthRightSeed = data.consumeInt(1, 32);
        int maxIterations = data.consumeInt(1, 64);

        double slope = slopeSeed;
        double intercept = interceptSeed;
        double root = -intercept / slope;

        if (Double.isNaN(root) || Double.isInfinite(root)) {
            root = 0.0;
            intercept = 0.0;
            slope = slope >= 0.0 ? 1.0 : -1.0;
        }

        PolynomialFunction linear = new PolynomialFunction(new double[] { intercept, slope });

        double lower = root - widthLeftSeed;
        double upper = root + widthRightSeed;

        double initial;
        switch (data.consumeInt(0, 4)) {
            case 0:
                initial = root;
                break;
            case 1:
                initial = lower;
                break;
            case 2:
                initial = upper;
                break;
            case 3:
                initial = root - Math.min(0.5, widthLeftSeed / 2.0);
                break;
            default:
                initial = root + Math.min(0.5, widthRightSeed / 2.0);
                break;
        }

        if (initial < lower) {
            initial = lower;
        } else if (initial > upper) {
            initial = upper;
        }

        try {
            UnivariateRealSolverUtils.bracket(linear, initial, lower, upper, maxIterations);

            double expandedLower = lower - data.consumeInt(0, 8);
            double expandedUpper = upper + data.consumeInt(0, 8);
            double boundaryInitial = data.consumeBoolean() ? expandedLower : expandedUpper;
            UnivariateRealSolverUtils.bracket(
                    linear,
                    boundaryInitial,
                    expandedLower,
                    expandedUpper,
                    data.consumeInt(1, 64));

            int degree = data.consumeInt(1, 6);
            double[] coeffs = new double[degree + 1];
            for (int i = 0; i < coeffs.length; i++) {
                coeffs[i] = data.consumeInt(-1000, 1000);
            }

            int chosenRoot = data.consumeInt(-16, 16);
            double valueAtRoot = 0.0;
            double pow = 1.0;
            for (int i = 0; i < coeffs.length; i++) {
                valueAtRoot += coeffs[i] * pow;
                pow *= chosenRoot;
            }
            coeffs[0] -= valueAtRoot;

            PolynomialFunction rootedPoly = new PolynomialFunction(coeffs);

            double polyLower = chosenRoot - data.consumeInt(1, 16);
            double polyUpper = chosenRoot + data.consumeInt(1, 16);
            double polyInitial;
            switch (data.consumeInt(0, 2)) {
                case 0:
                    polyInitial = chosenRoot;
                    break;
                case 1:
                    polyInitial = polyLower;
                    break;
                default:
                    polyInitial = polyUpper;
                    break;
            }

            UnivariateRealSolverUtils.bracket(
                    rootedPoly,
                    polyInitial,
                    polyLower,
                    polyUpper,
                    data.consumeInt(1, 64));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}