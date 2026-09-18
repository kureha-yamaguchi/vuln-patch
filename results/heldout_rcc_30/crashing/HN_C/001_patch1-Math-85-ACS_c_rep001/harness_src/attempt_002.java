package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        org.apache.commons.math.analysis.UnivariateRealFunction function;

        int functionMode = data.consumeInt(0, 5);
        if (functionMode == 0) {
            function = null;
        } else {
            int coeffLen = data.consumeInt(1, 8);
            double[] coeffs = new double[coeffLen];
            for (int i = 0; i < coeffLen; i++) {
                int coeffMode = data.consumeInt(0, 4);
                if (coeffMode == 0) {
                    coeffs[i] = (double) data.consumeInt();
                } else if (coeffMode == 1) {
                    coeffs[i] = (double) data.consumeByte();
                } else if (coeffMode == 2) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    coeffs[i] = Double.longBitsToDouble(bits);
                } else if (coeffMode == 3) {
                    coeffs[i] = data.consumeBoolean() ? 0.0d : -0.0d;
                } else {
                    coeffs[i] = data.consumeBoolean() ? 1.0d : -1.0d;
                }
            }

            switch (functionMode) {
                case 1:
                    if (coeffs.length > 0) {
                        coeffs[0] = (double) data.consumeInt();
                    }
                    function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);
                    break;
                case 2:
                    coeffs = new double[] {
                        (double) data.consumeInt(),
                        1.0d
                    };
                    function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);
                    break;
                case 3:
                    coeffs = new double[] {
                        (double) data.consumeInt(),
                        0.0d,
                        1.0d
                    };
                    function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);
                    break;
                case 4:
                    coeffs = new double[] {
                        (double) data.consumeInt()
                    };
                    function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);
                    break;
                default:
                    coeffs = new double[] {
                        (double) data.consumeInt(),
                        (double) data.consumeInt(),
                        (double) data.consumeInt(),
                        (double) data.consumeInt()
                    };
                    function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);
                    break;
            }
        }

        double lowerBound;
        double upperBound;
        double initial;

        int argsMode = data.consumeInt(0, 7);
        if (argsMode == 0) {
            double a = (double) data.consumeInt();
            double b = (double) data.consumeInt();
            lowerBound = Math.min(a, b);
            upperBound = Math.max(a, b);
            if (lowerBound == upperBound) {
                upperBound = lowerBound + 1.0d;
            }
            int selector = Math.abs(data.consumeInt());
            initial = lowerBound + (selector % 1001) * (upperBound - lowerBound) / 1000.0d;
        } else if (argsMode == 1) {
            double a = (double) data.consumeInt();
            double b = (double) data.consumeInt();
            lowerBound = Math.max(a, b);
            upperBound = Math.min(a, b);
            initial = (double) data.consumeInt();
        } else if (argsMode == 2) {
            double a = (double) data.consumeInt();
            double b = (double) data.consumeInt();
            lowerBound = Math.min(a, b);
            upperBound = Math.max(a, b);
            if (lowerBound == upperBound) {
                upperBound = lowerBound + 1.0d;
            }
            initial = lowerBound - Math.abs((double) data.consumeInt(0, 1000)) - 1.0d;
        } else if (argsMode == 3) {
            double a = (double) data.consumeInt();
            double b = (double) data.consumeInt();
            lowerBound = Math.min(a, b);
            upperBound = Math.max(a, b);
            if (lowerBound == upperBound) {
                upperBound = lowerBound + 1.0d;
            }
            initial = upperBound + Math.abs((double) data.consumeInt(0, 1000)) + 1.0d;
        } else if (argsMode == 4) {
            long lbBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long ubBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long inBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            lowerBound = Double.longBitsToDouble(lbBits);
            upperBound = Double.longBitsToDouble(ubBits);
            initial = Double.longBitsToDouble(inBits);
        } else if (argsMode == 5) {
            lowerBound = 0.0d;
            upperBound = 0.0d;
            initial = 0.0d;
        } else if (argsMode == 6) {
            lowerBound = -1.0d;
            upperBound = 1.0d;
            initial = data.consumeBoolean() ? -1.0d : 1.0d;
        } else {
            lowerBound = (double) data.consumeByte();
            upperBound = lowerBound + (double) data.consumeInt(0, 3);
            initial = (double) data.consumeByte();
        }

        int maximumIterations;
        int iterMode = data.consumeInt(0, 4);
        if (iterMode == 0) {
            maximumIterations = data.consumeInt();
        } else if (iterMode == 1) {
            maximumIterations = data.consumeInt(-5, 5);
        } else if (iterMode == 2) {
            maximumIterations = 1;
        } else if (iterMode == 3) {
            maximumIterations = Integer.MAX_VALUE;
        } else {
            maximumIterations = Integer.MIN_VALUE;
        }

        try {
            UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}