package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int functionMode = data.consumeInt(0, 5);
        UnivariateRealFunction function;

        if (functionMode == 0) {
            function = null;
        } else if (functionMode == 1) {
            double c0 = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
            );
            function = new PolynomialFunction(new double[] { c0 });
        } else if (functionMode == 2) {
            double c0 = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
            );
            double c1 = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
            );
            function = new PolynomialFunction(new double[] { c0, c1 });
        } else if (functionMode == 3) {
            double root = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
            );
            double scale = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
            );
            function = new PolynomialFunction(new double[] { -root * scale, scale });
        } else if (functionMode == 4) {
            double c0 = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
            );
            double c1 = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
            );
            double c2 = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
            );
            function = new PolynomialFunction(new double[] { c0, c1, c2 });
        } else {
            int len = data.consumeInt(1, 8);
            double[] coeffs = new double[len];
            for (int i = 0; i < len; i++) {
                coeffs[i] = Double.longBitsToDouble(
                    (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
                );
            }
            function = new PolynomialFunction(coeffs);
        }

        double x = Double.longBitsToDouble(
            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
        );
        double y = Double.longBitsToDouble(
            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
        );
        double z = Double.longBitsToDouble(
            (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)
        );

        double lowerBound;
        double upperBound;
        double initial;

        int boundsMode = data.consumeInt(0, 6);
        if (boundsMode == 0) {
            lowerBound = x;
            upperBound = y;
            initial = z;
        } else if (boundsMode == 1) {
            lowerBound = Math.min(x, y);
            upperBound = Math.max(x, y);
            initial = lowerBound + (upperBound - lowerBound) / 2.0;
        } else if (boundsMode == 2) {
            lowerBound = Math.min(x, y);
            upperBound = Math.max(x, y);
            initial = data.consumeBoolean() ? lowerBound : upperBound;
        } else if (boundsMode == 3) {
            lowerBound = Math.max(x, y);
            upperBound = Math.min(x, y);
            initial = z;
        } else if (boundsMode == 4) {
            lowerBound = Math.min(x, y);
            upperBound = Math.max(x, y);
            initial = upperBound + z;
        } else if (boundsMode == 5) {
            lowerBound = Math.min(x, y);
            upperBound = lowerBound;
            initial = z;
        } else {
            lowerBound = data.consumeBoolean() ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            upperBound = data.consumeBoolean() ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            initial = data.consumeBoolean() ? Double.NaN : z;
        }

        int maxIterations;
        int iterMode = data.consumeInt(0, 3);
        if (iterMode == 0) {
            maxIterations = data.consumeInt();
        } else if (iterMode == 1) {
            maxIterations = data.consumeInt(-2, 2);
        } else if (iterMode == 2) {
            maxIterations = data.consumeInt(1, 32);
        } else {
            maxIterations = Integer.MAX_VALUE;
        }

        try {
            UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maxIterations);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}