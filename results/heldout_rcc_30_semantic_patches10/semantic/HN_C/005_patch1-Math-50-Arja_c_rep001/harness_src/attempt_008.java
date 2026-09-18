package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.function.Sin;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] generated = new double[8];
        for (int i = 0; i < generated.length; i++) {
            int raw = data.consumeInt();
            switch (data.consumeInt(0, 9)) {
                case 0:
                    generated[i] = 0.0;
                    break;
                case 1:
                    generated[i] = -0.0;
                    break;
                case 2:
                    generated[i] = raw;
                    break;
                case 3:
                    generated[i] = raw / 1024.0;
                    break;
                case 4:
                    generated[i] = raw / 1048576.0;
                    break;
                case 5:
                    generated[i] = (double) (byte) raw;
                    break;
                case 6:
                    generated[i] = Double.NaN;
                    break;
                case 7:
                    generated[i] = Double.POSITIVE_INFINITY;
                    break;
                case 8:
                    generated[i] = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    generated[i] = (raw % 2 == 0 ? 1.0 : -1.0) * Math.abs(raw % 1000);
                    break;
            }
        }

        double min = generated[0];
        double max = generated[1];
        double start = generated[2];
        double r1 = generated[3];
        double r2 = generated[4];
        double c = generated[5];

        double absoluteAccuracy = Math.abs(generated[6]);
        double relativeAccuracy = Math.abs(generated[7]);

        if (!(absoluteAccuracy > 0.0) || Double.isNaN(absoluteAccuracy)) {
            absoluteAccuracy = 1.0e-6;
        }
        if (!(relativeAccuracy >= 0.0) || Double.isNaN(relativeAccuracy)) {
            relativeAccuracy = 1.0e-14;
        }

        BaseSecantSolver solver;
        switch (data.consumeInt(0, 5)) {
            case 0:
                solver = new RegulaFalsiSolver();
                break;
            case 1:
                solver = new RegulaFalsiSolver(absoluteAccuracy);
                break;
            case 2:
                solver = new IllinoisSolver();
                break;
            case 3:
                solver = new IllinoisSolver(absoluteAccuracy);
                break;
            case 4:
                solver = new PegasusSolver();
                break;
            default:
                solver = new PegasusSolver(absoluteAccuracy);
                break;
        }

        UnivariateRealFunction function;
        switch (data.consumeInt(0, 5)) {
            case 0:
                function = new PolynomialFunction(new double[] { c });
                break;
            case 1:
                function = new PolynomialFunction(new double[] { -r1, 1.0 });
                break;
            case 2:
                function = new PolynomialFunction(new double[] { r1 * r2, -(r1 + r2), 1.0 });
                break;
            case 3:
                byte[] bytes = data.consumeBytes(data.consumeInt(0, 16));
                int len = bytes.length == 0 ? 1 : bytes.length;
                double[] coeffs = new double[len];
                for (int i = 0; i < len; i++) {
                    byte b = bytes.length == 0 ? 0 : bytes[i];
                    switch (data.consumeInt(0, 4)) {
                        case 0:
                            coeffs[i] = b;
                            break;
                        case 1:
                            coeffs[i] = b / 16.0;
                            break;
                        case 2:
                            coeffs[i] = (i == 0) ? -r1 : (i == 1 ? 1.0 : b / 32.0);
                            break;
                        case 3:
                            coeffs[i] = (b == 0) ? 0.0 : 1.0 / b;
                            break;
                        default:
                            coeffs[i] = ((b & 1) == 0 ? 1.0 : -1.0) * (b & 0x7f);
                            break;
                    }
                }
                function = new PolynomialFunction(coeffs);
                break;
            case 4:
                function = new Sin();
                break;
            default:
                function = new PolynomialFunction(new double[] { -min, 1.0 });
                break;
        }

        int maxEval = data.consumeInt(1, 10000);
        AllowedSolution[] allowedValues = AllowedSolution.values();
        AllowedSolution allowed = allowedValues[data.consumeInt(0, allowedValues.length - 1)];

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