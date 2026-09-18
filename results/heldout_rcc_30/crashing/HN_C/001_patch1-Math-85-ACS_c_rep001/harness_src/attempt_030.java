package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int functionKind = data.consumeInt(0, 4);
        UnivariateRealFunction function;
        double preferredCenter = data.consumeInt(-1000, 1000);

        switch (functionKind) {
            case 0:
                function = null;
                break;
            case 1: {
                int degreePlusOne = data.consumeInt(0, 8);
                double[] coefficients = new double[degreePlusOne];
                for (int i = 0; i < degreePlusOne; i++) {
                    coefficients[i] = data.consumeInt(-1000, 1000);
                }
                function = new PolynomialFunction(coefficients);
                break;
            }
            case 2: {
                double root = data.consumeInt(-1000, 1000);
                preferredCenter = root;
                function = new PolynomialFunction(new double[] { -root, 1.0 });
                break;
            }
            case 3: {
                double r1 = data.consumeInt(-100, 100);
                double r2 = data.consumeInt(-100, 100);
                preferredCenter = data.consumeBoolean() ? r1 : r2;
                function = new PolynomialFunction(new double[] { r1 * r2, -(r1 + r2), 1.0 });
                break;
            }
            default:
                function = new PolynomialFunction(new double[] { data.consumeInt(-10, 10) });
                break;
        }

        double lowerBound;
        double upperBound;
        double initial;
        int maximumIterations;

        int argMode = data.consumeInt(0, 5);
        switch (argMode) {
            case 0: {
                long lbBits = (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL);
                long ubBits = (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL);
                long inBits = (((long) data.consumeInt()) << 32) | (((long) data.consumeInt()) & 0xffffffffL);
                lowerBound = Double.longBitsToDouble(lbBits);
                upperBound = Double.longBitsToDouble(ubBits);
                initial = Double.longBitsToDouble(inBits);
                maximumIterations = data.consumeInt();
                break;
            }
            case 1: {
                double a = data.consumeInt(-1000, 1000);
                double b = data.consumeInt(-1000, 1000);
                lowerBound = Math.min(a, b);
                upperBound = Math.max(a, b);
                if (data.consumeBoolean()) {
                    initial = lowerBound + (upperBound - lowerBound) / 2.0;
                } else {
                    initial = data.consumeBoolean() ? lowerBound : upperBound;
                }
                maximumIterations = data.consumeInt(-5, 20);
                break;
            }
            case 2: {
                lowerBound = data.consumeInt(-1000, 1000);
                upperBound = lowerBound;
                initial = lowerBound;
                maximumIterations = data.consumeInt(-5, 5);
                break;
            }
            case 3: {
                lowerBound = data.consumeInt(-1000, 0);
                upperBound = data.consumeInt(0, 1000);
                if (lowerBound > upperBound) {
                    double t = lowerBound;
                    lowerBound = upperBound;
                    upperBound = t;
                }
                initial = data.consumeBoolean() ? lowerBound - data.consumeInt(1, 10) : upperBound + data.consumeInt(1, 10);
                maximumIterations = data.consumeInt(1, 20);
                break;
            }
            case 4: {
                double leftSpan = data.consumeInt(1, 20);
                double rightSpan = data.consumeInt(1, 20);
                lowerBound = preferredCenter - leftSpan;
                upperBound = preferredCenter + rightSpan;
                initial = preferredCenter + data.consumeInt(-1, 1);
                if (initial < lowerBound) {
                    initial = lowerBound;
                }
                if (initial > upperBound) {
                    initial = upperBound;
                }
                maximumIterations = data.consumeInt(1, 50);
                break;
            }
            default: {
                double x = data.consumeInt(-50, 50);
                double y = data.consumeInt(-50, 50);
                lowerBound = x;
                upperBound = y;
                initial = data.consumeInt(-60, 60);
                maximumIterations = data.consumeInt(-2, 3);
                break;
            }
        }

        try {
            UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}