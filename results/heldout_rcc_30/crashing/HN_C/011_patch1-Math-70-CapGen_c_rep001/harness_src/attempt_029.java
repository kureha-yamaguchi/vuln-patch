package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double root = data.consumeInt(-10000, 10000) / 32.0;
        double width = data.consumeInt(0, 10000) / 32.0 + 1.0e-9;

        double min = root - width;
        double max = root + width;

        double initial;
        switch (data.consumeInt(0, 5)) {
            case 0:
                initial = root;
                break;
            case 1:
                initial = min;
                break;
            case 2:
                initial = max;
                break;
            case 3:
                initial = root + width * 2.0;
                break;
            case 4:
                initial = Double.NaN;
                break;
            default:
                initial = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                break;
        }

        UnivariateRealFunction f;
        switch (data.consumeInt(0, 4)) {
            case 0:
                f = new PolynomialFunction(new double[] { -root, 1.0 });
                break;
            case 1:
                f = new PolynomialFunction(new double[] { -(root * root * root), 0.0, 0.0, 1.0 });
                break;
            case 2:
                double r2 = root * root;
                double r4 = r2 * r2;
                f = new PolynomialFunction(new double[] { -(r4 * root), 0.0, 0.0, 0.0, 0.0, 1.0 });
                break;
            case 3:
                f = new PolynomialFunction(new double[] { 0.0, 0.0, 1.0 });
                break;
            default:
                f = new PolynomialFunction(new double[] { data.consumeInt(-100, 100), data.consumeInt(-100, 100), data.consumeInt(-100, 100) });
                break;
        }

        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(f, min, max, initial);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}