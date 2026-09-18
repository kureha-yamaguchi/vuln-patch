package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            int mode = data.consumeInt(0, 3);

            double root = boundedDouble(data);
            int offset = data.consumeInt(-16, 16);
            double initial = root + offset;

            double lowerBound;
            double upperBound;

            if (mode == 0) {
                lowerBound = root - (Math.abs(offset) + 1 + data.consumeInt(0, 32));
                upperBound = root + (Math.abs(offset) + 1 + data.consumeInt(0, 32));
                if (initial < lowerBound) {
                    initial = lowerBound;
                } else if (initial > upperBound) {
                    initial = upperBound;
                }
            } else if (mode == 1) {
                lowerBound = initial;
                upperBound = initial + 1.0 + data.consumeInt(0, 8);
            } else if (mode == 2) {
                upperBound = initial;
                lowerBound = initial - (1.0 + data.consumeInt(0, 8));
            } else {
                lowerBound = initial + data.consumeInt(-4, 4);
                upperBound = lowerBound + data.consumeInt(-2, 2);
            }

            int maximumIterations;
            if (data.consumeBoolean()) {
                maximumIterations = data.consumeInt(-3, 3);
            } else {
                maximumIterations = 1 + data.consumeInt(0, 64);
            }

            PolynomialFunction function;
            switch (data.consumeInt(0, 4)) {
                case 0: {
                    double slope = nonZeroSmallDouble(data);
                    function = new PolynomialFunction(new double[] { -slope * root, slope });
                    break;
                }
                case 1: {
                    double scale = nonZeroSmallDouble(data);
                    function = new PolynomialFunction(new double[] { -scale * root, 0.0, scale });
                    break;
                }
                case 2: {
                    double scale = nonZeroSmallDouble(data);
                    double r2 = root * root;
                    function = new PolynomialFunction(new double[] { -scale * root, scale * (1.0 - r2), -scale * root, scale });
                    break;
                }
                case 3: {
                    double c = boundedDouble(data);
                    function = new PolynomialFunction(new double[] { c });
                    break;
                }
                default: {
                    int degree = data.consumeInt(0, 6);
                    double[] coeffs = new double[degree + 1];
                    for (int i = 0; i < coeffs.length; i++) {
                        coeffs[i] = boundedDouble(data);
                    }
                    if (data.consumeBoolean()) {
                        coeffs[0] = -root;
                        if (coeffs.length > 1) {
                            coeffs[1] = coeffs[1] == 0.0 ? 1.0 : coeffs[1];
                        }
                    }
                    function = new PolynomialFunction(coeffs);
                    break;
                }
            }

            UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);

            if (data.remainingBytes() > 0 && data.consumeBoolean()) {
                double root2 = boundedDouble(data);
                double slope2 = nonZeroSmallDouble(data);
                PolynomialFunction linear2 = new PolynomialFunction(new double[] { -slope2 * root2, slope2 });

                double lower2 = root2 - (1 + data.consumeInt(0, 16));
                double upper2 = root2 + (1 + data.consumeInt(0, 16));
                double initial2 = root2 + data.consumeInt(-8, 8);
                if (initial2 < lower2) {
                    initial2 = lower2;
                } else if (initial2 > upper2) {
                    initial2 = upper2;
                }

                UnivariateRealSolverUtils.bracket(
                    linear2,
                    initial2,
                    lower2,
                    upper2,
                    1 + data.consumeInt(0, 32)
                );
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static double boundedDouble(FuzzedDataProvider data) {
        int hi = data.consumeInt();
        int lo = data.consumeInt();
        long bits = (((long) hi) << 32) | (lo & 0xffffffffL);
        double value = Double.longBitsToDouble(bits);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return data.consumeInt(-1024, 1024);
        }
        if (value > 1.0e6) {
            return 1.0e6;
        }
        if (value < -1.0e6) {
            return -1.0e6;
        }
        return value;
    }

    private static double nonZeroSmallDouble(FuzzedDataProvider data) {
        double v = boundedDouble(data);
        if (v == 0.0) {
            return data.consumeBoolean() ? 1.0 : -1.0;
        }
        if (v > 1024.0) {
            return 1024.0;
        }
        if (v < -1024.0) {
            return -1024.0;
        }
        return v;
    }
}