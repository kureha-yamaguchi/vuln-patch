package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double root1 = data.consumeInt() / 16.0;
        double root2 = data.consumeInt() / 16.0;

        UnivariateRealFunction passedFunction;
        if (data.consumeBoolean()) {
            passedFunction = new PolynomialFunction(new double[] { -root1, 1.0 });
        } else {
            int degree = data.consumeInt(0, 3);
            double[] coeffs = new double[degree + 1];
            for (int i = 0; i < coeffs.length; i++) {
                coeffs[i] = data.consumeInt() / 32.0;
            }
            if (coeffs[coeffs.length - 1] == 0.0) {
                coeffs[coeffs.length - 1] = 1.0;
            }
            passedFunction = new PolynomialFunction(coeffs);
        }

        UnivariateRealFunction ctorFunction;
        if (data.consumeBoolean()) {
            ctorFunction = new PolynomialFunction(new double[] { -root2, 1.0 });
        } else {
            int degree = data.consumeInt(0, 3);
            double[] coeffs = new double[degree + 1];
            for (int i = 0; i < coeffs.length; i++) {
                coeffs[i] = data.consumeInt() / 32.0;
            }
            if (coeffs[coeffs.length - 1] == 0.0) {
                coeffs[coeffs.length - 1] = 1.0;
            }
            ctorFunction = new PolynomialFunction(coeffs);
        }

        BisectionSolver solver = data.consumeBoolean() ? new BisectionSolver(ctorFunction) : new BisectionSolver();

        double a;
        switch (data.consumeInt(0, 7)) {
            case 0:
                a = Double.NaN;
                break;
            case 1:
                a = Double.POSITIVE_INFINITY;
                break;
            case 2:
                a = Double.NEGATIVE_INFINITY;
                break;
            default:
                a = data.consumeInt() / 16.0;
                break;
        }

        double b;
        switch (data.consumeInt(0, 7)) {
            case 0:
                b = Double.NaN;
                break;
            case 1:
                b = Double.POSITIVE_INFINITY;
                break;
            case 2:
                b = Double.NEGATIVE_INFINITY;
                break;
            default:
                b = data.consumeInt() / 16.0;
                break;
        }

        double min = a;
        double max = b;
        if (data.consumeBoolean() && a > b) {
            min = b;
            max = a;
        }

        double initial;
        switch (data.consumeInt(0, 9)) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = (min + max) / 2.0;
                break;
            case 3:
                initial = Double.NaN;
                break;
            case 4:
                initial = Double.POSITIVE_INFINITY;
                break;
            case 5:
                initial = Double.NEGATIVE_INFINITY;
                break;
            default:
                initial = data.consumeInt() / 16.0;
                break;
        }

        try {
            solver.solve(passedFunction, min, max, initial);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}