package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.QuinticFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction function;
        int functionChoice = data.consumeInt(0, 2);
        if (functionChoice == 0) {
            function = new SinFunction();
        } else if (functionChoice == 1) {
            function = new QuinticFunction();
        } else {
            int degree = data.consumeInt(0, 7);
            double[] coefficients = new double[degree + 1];
            for (int i = 0; i < coefficients.length; i++) {
                int mode = data.consumeInt(0, 7);
                double v;
                switch (mode) {
                    case 0:
                        v = 0.0;
                        break;
                    case 1:
                        v = data.consumeByte();
                        break;
                    case 2:
                        v = data.consumeInt(-16, 16);
                        break;
                    case 3:
                        v = data.consumeInt() / (double) (data.consumeInt(1, 1024));
                        break;
                    case 4:
                        v = (double) data.consumeInt() * (double) data.consumeInt(-8, 8);
                        break;
                    case 5:
                        v = ((double) data.consumeInt()) / 2147483647.0;
                        break;
                    case 6:
                        v = data.consumeBoolean() ? Math.PI : -Math.PI;
                        break;
                    default:
                        long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                        v = Double.longBitsToDouble(bits);
                        if (Double.isNaN(v) || Double.isInfinite(v)) {
                            v = data.consumeInt(-32, 32);
                        }
                        break;
                }
                coefficients[i] = v;
            }
            if (data.consumeBoolean()) {
                coefficients[0] = 0.0;
            }
            if (data.consumeBoolean() && coefficients.length > 1) {
                coefficients[1] = data.consumeBoolean() ? 1.0 : -1.0;
            }
            function = new PolynomialFunction(coefficients);
        }

        double[] xs = new double[3];
        for (int i = 0; i < xs.length; i++) {
            int mode = data.consumeInt(0, 11);
            double v;
            switch (mode) {
                case 0:
                    v = 0.0;
                    break;
                case 1:
                    v = 1.0;
                    break;
                case 2:
                    v = -1.0;
                    break;
                case 3:
                    v = Math.PI;
                    break;
                case 4:
                    v = -Math.PI;
                    break;
                case 5:
                    v = data.consumeByte();
                    break;
                case 6:
                    v = data.consumeInt(-32, 32);
                    break;
                case 7:
                    v = data.consumeInt() / (double) data.consumeInt(1, 4096);
                    break;
                case 8:
                    v = ((double) data.consumeInt(-1024, 1024)) * Math.PI;
                    break;
                case 9:
                    v = (double) data.consumeInt() * (double) data.consumeInt(-4, 4);
                    break;
                case 10:
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    v = Double.longBitsToDouble(bits);
                    break;
                default:
                    v = data.consumeBoolean() ? Double.MIN_VALUE : -Double.MIN_VALUE;
                    break;
            }
            xs[i] = v;
        }

        if (function instanceof SinFunction && data.consumeBoolean()) {
            int k0 = data.consumeInt(-8, 8);
            int k1 = data.consumeInt(-8, 8);
            xs[0] = k0 * Math.PI;
            xs[1] = k1 * Math.PI;
            if (data.consumeBoolean()) {
                xs[2] = data.consumeInt(-8, 8) * Math.PI;
            }
        } else if (function instanceof QuinticFunction && data.consumeBoolean()) {
            double[] roots = new double[] { -1.0, 0.0, 1.0 };
            xs[0] = roots[data.consumeInt(0, roots.length - 1)];
            xs[1] = roots[data.consumeInt(0, roots.length - 1)];
            if (data.consumeBoolean()) {
                xs[2] = roots[data.consumeInt(0, roots.length - 1)];
            }
        } else if (function instanceof PolynomialFunction && data.consumeBoolean()) {
            xs[0] = 0.0;
            if (data.consumeBoolean()) {
                xs[1] = 0.0;
            }
            if (data.consumeBoolean()) {
                xs[2] = 0.0;
            }
        }

        double min = xs[0];
        double max = xs[1];
        double start = xs[2];

        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }
        if (data.consumeBoolean()) {
            max = min;
        }
        if (data.consumeBoolean()) {
            start = min;
        } else if (data.consumeBoolean()) {
            start = max;
        }

        AllowedSolution[] allowedValues = AllowedSolution.values();
        AllowedSolution allowed = allowedValues[data.consumeInt(0, allowedValues.length - 1)];

        BaseSecantSolver solver;
        int solverChoice = data.consumeInt(0, 2);
        if (solverChoice == 0) {
            solver = new RegulaFalsiSolver();
        } else if (solverChoice == 1) {
            solver = new IllinoisSolver();
        } else {
            solver = new PegasusSolver();
        }

        int maxEval;
        if (data.consumeBoolean()) {
            maxEval = data.consumeInt(0, 8);
        } else {
            maxEval = data.consumeInt(1, 512);
        }

        int callStyle = data.consumeInt(0, 2);
        if (callStyle == 0) {
            solver.solve(maxEval, function, min, max);
        } else if (callStyle == 1) {
            solver.solve(maxEval, function, min, max, allowed);
        } else {
            solver.solve(maxEval, function, min, max, start);
        }
    }
}