package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BisectionSolver solver = new BisectionSolver();

        double min;
        switch (data.consumeInt(0, 9)) {
            case 0:
                min = 0.0d;
                break;
            case 1:
                min = -0.0d;
                break;
            case 2:
                min = 1.0d;
                break;
            case 3:
                min = -1.0d;
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
                min = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                break;
            case 8:
                min = data.consumeInt(-1000000, 1000000);
                break;
            default:
                min = ((double) data.consumeInt()) / 1024.0d;
                break;
        }

        double max;
        switch (data.consumeInt(0, 9)) {
            case 0:
                max = 0.0d;
                break;
            case 1:
                max = -0.0d;
                break;
            case 2:
                max = 1.0d;
                break;
            case 3:
                max = -1.0d;
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
                max = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                break;
            case 8:
                max = data.consumeInt(-1000000, 1000000);
                break;
            default:
                max = ((double) data.consumeInt()) / 1024.0d;
                break;
        }

        double initial;
        switch (data.consumeInt(0, 9)) {
            case 0:
                initial = 0.0d;
                break;
            case 1:
                initial = -0.0d;
                break;
            case 2:
                initial = 1.0d;
                break;
            case 3:
                initial = -1.0d;
                break;
            case 4:
                initial = Double.NaN;
                break;
            case 5:
                initial = Double.POSITIVE_INFINITY;
                break;
            case 6:
                initial = Double.NEGATIVE_INFINITY;
                break;
            case 7:
                initial = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                break;
            case 8:
                initial = data.consumeInt(-1000000, 1000000);
                break;
            default:
                initial = ((double) data.consumeInt()) / 1024.0d;
                break;
        }

        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }
        if (data.consumeBoolean()) {
            max = min;
        }
        if (data.consumeBoolean()) {
            initial = min;
        } else if (data.consumeBoolean()) {
            initial = max;
        } else if (data.consumeBoolean()) {
            initial = (min + max) / 2.0d;
        }

        UnivariateRealFunction targetFunction;
        int functionChoice = data.consumeInt(0, 3);
        if (functionChoice == 0) {
            targetFunction = new SinFunction();
        } else if (functionChoice == 1) {
            double root = ((double) data.consumeInt()) / 256.0d;
            targetFunction = new PolynomialFunction(new double[] { -root, 1.0d });
            if (data.consumeBoolean()) {
                min = root - Math.abs(((double) data.consumeInt()) / 16.0d) - 1.0d;
                max = root + Math.abs(((double) data.consumeInt()) / 16.0d) + 1.0d;
            }
        } else if (functionChoice == 2) {
            int coeffMax = data.remainingBytes();
            if (coeffMax > 8) {
                coeffMax = 8;
            }
            if (coeffMax < 0) {
                coeffMax = 0;
            }
            byte[] coeffBytes = data.consumeBytes(coeffMax);
            double[] coeffs;
            if (coeffBytes.length == 0) {
                coeffs = new double[] { 0.0d };
            } else {
                coeffs = new double[coeffBytes.length];
                for (int i = 0; i < coeffBytes.length; i++) {
                    coeffs[i] = coeffBytes[i];
                }
            }
            targetFunction = new PolynomialFunction(coeffs);
        } else {
            double c0 = data.consumeByte();
            double c1 = data.consumeByte();
            double c2 = data.consumeByte();
            targetFunction = new PolynomialFunction(new double[] { c0, c1, c2 });
        }

        try {
            if (data.consumeBoolean()) {
                UnivariateRealFunction seedFunction;
                int seedChoice = data.consumeInt(0, 2);
                if (seedChoice == 0) {
                    seedFunction = new SinFunction();
                } else if (seedChoice == 1) {
                    double seedRoot = ((double) data.consumeInt()) / 128.0d;
                    seedFunction = new PolynomialFunction(new double[] { -seedRoot, 1.0d });
                } else {
                    seedFunction = new PolynomialFunction(new double[] { data.consumeByte(), data.consumeByte(), data.consumeByte() });
                }

                double seedMin = ((double) data.consumeInt()) / 64.0d;
                double seedMax = ((double) data.consumeInt()) / 64.0d;
                if (data.consumeBoolean()) {
                    double t = seedMin;
                    seedMin = seedMax;
                    seedMax = t;
                }
                solver.solve(seedFunction, seedMin, seedMax);
            }

            solver.solve(targetFunction, min, max, initial);
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}