package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double root = (double) data.consumeInt(-1000000, 1000000);
        double width = (double) data.consumeInt(1, 1000000);

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
                initial = (min + max) / 2.0;
                break;
            case 4:
                initial = root + (double) data.consumeInt(-(int) width, (int) width);
                break;
            default:
                initial = root + (double) data.consumeInt();
                break;
        }

        UnivariateRealFunction f;
        switch (data.consumeInt(0, 4)) {
            case 0:
                f = new PolynomialFunction(new double[] { -root, 1.0 });
                break;
            case 1:
                f = new PolynomialFunction(new double[] {
                        -root * root * root,
                        3.0 * root * root,
                        -3.0 * root,
                        1.0
                });
                break;
            case 2:
                double scale = (double) data.consumeInt(-1000, 1000);
                if (scale == 0.0) {
                    scale = 1.0;
                }
                f = new PolynomialFunction(new double[] { -scale * root, scale });
                break;
            case 3:
                f = new PolynomialFunction(new double[] {
                        -(root - 1.0) * root * (root + 1.0),
                        -1.0,
                        1.0,
                        1.0
                });
                break;
            default:
                f = new PolynomialFunction(new double[] { 0.0, 1.0 });
                break;
        }

        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}