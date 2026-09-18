package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.QuinticFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        long bits1 = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long bits2 = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long bits3 = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long bits4 = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

        double rawA = Double.longBitsToDouble(bits1);
        double rawB = Double.longBitsToDouble(bits2);
        double rawC = Double.longBitsToDouble(bits3);
        double rawD = Double.longBitsToDouble(bits4);

        double a = (Double.isNaN(rawA) || Double.isInfinite(rawA)) ? 0.0 : rawA;
        double b = (Double.isNaN(rawB) || Double.isInfinite(rawB)) ? 1.0 : rawB;
        double c = (Double.isNaN(rawC) || Double.isInfinite(rawC)) ? -1.0 : rawC;
        double d = (Double.isNaN(rawD) || Double.isInfinite(rawD)) ? 0.5 : rawD;

        double delta = Math.abs(b);
        if (!(delta > 0.0) || Double.isInfinite(delta)) {
            delta = 1.0;
        }
        if (delta > 1.0e6) {
            delta = 1.0e6;
        }

        UnivariateRealFunction function;
        double rootHint;

        switch (data.consumeInt(0, 4)) {
            case 0: {
                int k = data.consumeInt(-32, 32);
                rootHint = k * Math.PI;
                function = new SinFunction();
                break;
            }
            case 1: {
                rootHint = a;
                function = new PolynomialFunction(new double[] { -rootHint, 1.0 });
                break;
            }
            case 2: {
                double r = Math.abs(a);
                rootHint = r;
                function = new PolynomialFunction(new double[] { -(r * r), 0.0, 1.0 });
                break;
            }
            case 3: {
                double[] roots = new double[] { -1.0, -0.5, 0.0, 0.5, 1.0 };
                rootHint = roots[data.consumeInt(0, roots.length - 1)];
                function = new QuinticFunction();
                break;
            }
            default: {
                int len = data.consumeInt(1, 8);
                double[] coeffs = new double[len];
                for (int i = 0; i < len; i++) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    double v = Double.longBitsToDouble(bits);
                    coeffs[i] = (Double.isNaN(v) || Double.isInfinite(v)) ? 0.0 : v;
                }
                if (len == 1) {
                    coeffs[0] = 0.0;
                    rootHint = 0.0;
                } else {
                    coeffs[0] = -a;
                    coeffs[1] = 1.0;
                    rootHint = a;
                }
                function = new PolynomialFunction(coeffs);
                break;
            }
        }

        double min;
        double max;

        switch (data.consumeInt(0, 6)) {
            case 0:
                min = rootHint - delta;
                max = rootHint + delta;
                break;
            case 1:
                min = rootHint;
                max = rootHint + delta;
                break;
            case 2:
                min = rootHint - delta;
                max = rootHint;
                break;
            case 3:
                min = rootHint + delta;
                max = rootHint - delta;
                break;
            case 4:
                min = a;
                max = c;
                break;
            case 5:
                min = c;
                max = a;
                break;
            default:
                min = d - delta;
                max = d + delta;
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

        AllowedSolution allowed = AllowedSolution.values()[data.consumeInt(0, AllowedSolution.values().length - 1)];
        int maxEval = data.consumeInt(1, 10000);

        switch (data.consumeInt(0, 2)) {
            case 0:
                solver.solve(maxEval, function, min, max);
                break;
            case 1:
                solver.solve(maxEval, function, min, max, allowed);
                break;
            default:
                double start;
                switch (data.consumeInt(0, 3)) {
                    case 0:
                        start = rootHint;
                        break;
                    case 1:
                        start = min;
                        break;
                    case 2:
                        start = max;
                        break;
                    default:
                        start = a;
                        break;
                }
                solver.solve(maxEval, function, min, max, start);
                break;
        }
    }
}