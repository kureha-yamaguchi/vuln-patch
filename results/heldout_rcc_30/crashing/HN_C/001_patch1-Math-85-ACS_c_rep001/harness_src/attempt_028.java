package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int coeffCount = data.consumeInt(1, 8);
        double[] coefficients = new double[coeffCount];
        for (int i = 0; i < coeffCount; i++) {
            int mode = data.consumeInt(0, 5);
            if (mode == 0) {
                coefficients[i] = data.consumeInt(-1000, 1000);
            } else if (mode == 1) {
                coefficients[i] = data.consumeByte();
            } else if (mode == 2) {
                coefficients[i] = data.consumeInt() / 1024.0;
            } else if (mode == 3) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                coefficients[i] = Double.longBitsToDouble(bits);
            } else if (mode == 4) {
                coefficients[i] = data.consumeBoolean() ? 0.0 : -0.0;
            } else {
                coefficients[i] = data.consumeBoolean() ? 1.0 : -1.0;
            }
        }

        UnivariateRealFunction function = new PolynomialFunction(coefficients);

        long initialBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long lowerBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long upperBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

        double initial = Double.longBitsToDouble(initialBits);
        double lowerBound = Double.longBitsToDouble(lowerBits);
        double upperBound = Double.longBitsToDouble(upperBits);

        int boundsMode = data.consumeInt(0, 6);
        if (boundsMode == 0) {
            if (lowerBound > upperBound) {
                double t = lowerBound;
                lowerBound = upperBound;
                upperBound = t;
            }
        } else if (boundsMode == 1) {
            int a = data.consumeInt(-100, 100);
            int b = data.consumeInt(-100, 100);
            lowerBound = Math.min(a, b);
            upperBound = Math.max(a, b);
            initial = data.consumeInt(-100, 100);
        } else if (boundsMode == 2) {
            int center = data.consumeInt(-50, 50);
            int radius = data.consumeInt(0, 50);
            lowerBound = center - radius;
            upperBound = center + radius;
            initial = center;
        } else if (boundsMode == 3) {
            lowerBound = data.consumeInt(-10, 10);
            upperBound = lowerBound;
            initial = lowerBound;
        } else if (boundsMode == 4) {
            lowerBound = data.consumeInt(-1000, 999);
            upperBound = lowerBound + 1.0;
            initial = data.consumeBoolean() ? lowerBound : upperBound;
        } else if (boundsMode == 5) {
            lowerBound = data.consumeInt(-100, 0);
            upperBound = data.consumeInt(0, 100);
            if (lowerBound > upperBound) {
                double t = lowerBound;
                lowerBound = upperBound;
                upperBound = t;
            }
            initial = 0.0;
        }

        int initialMode = data.consumeInt(0, 4);
        if (initialMode == 0) {
            initial = lowerBound;
        } else if (initialMode == 1) {
            initial = upperBound;
        } else if (initialMode == 2) {
            initial = (lowerBound + upperBound) / 2.0;
        } else if (initialMode == 3) {
            initial = lowerBound - 1.0;
        }

        int maximumIterations;
        int iterMode = data.consumeInt(0, 4);
        if (iterMode == 0) {
            maximumIterations = data.consumeInt();
        } else if (iterMode == 1) {
            maximumIterations = data.consumeInt(-10, 10);
        } else if (iterMode == 2) {
            maximumIterations = data.consumeInt(1, 100);
        } else if (iterMode == 3) {
            maximumIterations = 1;
        } else {
            maximumIterations = Integer.MAX_VALUE;
        }

        try {
            UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
        } catch (Throwable t) {
            FuzzHarness.<RuntimeException>sneakyThrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}