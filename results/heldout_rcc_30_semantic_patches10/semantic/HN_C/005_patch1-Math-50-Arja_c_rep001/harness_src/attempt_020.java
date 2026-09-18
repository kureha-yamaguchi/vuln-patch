package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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

        org.apache.commons.math.analysis.UnivariateRealFunction function;
        double min;
        double max;

        int mode = data.consumeInt(0, 5);
        if (mode == 0) {
            long rootBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            double root = Double.longBitsToDouble(rootBits);
            if (Double.isNaN(root) || Double.isInfinite(root)) {
                root = data.consumeInt() / 1024.0;
            }

            double width;
            if (data.consumeBoolean()) {
                width = data.consumeInt(0, 16);
            } else {
                long widthBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                width = Math.abs(Double.longBitsToDouble(widthBits));
                if (Double.isNaN(width) || Double.isInfinite(width)) {
                    width = Math.abs(data.consumeInt()) / 1024.0;
                }
            }

            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                    new double[] { -root, 1.0 });

            if (data.consumeBoolean()) {
                min = root - width;
                max = root + width;
            } else {
                min = root + width;
                max = root - width;
            }
        } else if (mode == 1) {
            int degree = data.consumeInt(1, 8);
            double[] coeffs = new double[degree + 1];
            for (int i = 0; i < coeffs.length; i++) {
                if (data.consumeBoolean()) {
                    coeffs[i] = data.consumeInt() / 128.0;
                } else {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    coeffs[i] = Double.longBitsToDouble(bits);
                }
            }
            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);

            long minBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            long maxBits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
            min = Double.longBitsToDouble(minBits);
            max = Double.longBitsToDouble(maxBits);
            if ((Double.isNaN(min) || Double.isInfinite(min)) && data.consumeBoolean()) {
                min = data.consumeInt() / 256.0;
            }
            if ((Double.isNaN(max) || Double.isInfinite(max)) && data.consumeBoolean()) {
                max = data.consumeInt() / 256.0;
            }
        } else if (mode == 2) {
            double c0 = data.consumeInt() / 64.0;
            double c1 = data.consumeInt() / 64.0;
            double c2 = data.consumeInt() / 64.0;
            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                    new double[] { c0, c1, c2 });

            double center = data.consumeInt() / 64.0;
            double radius = Math.abs(data.consumeInt()) / 64.0;
            min = center - radius;
            max = center + radius;
            if (data.consumeBoolean()) {
                double t = min;
                min = max;
                max = t;
            }
        } else if (mode == 3) {
            double root1 = data.consumeInt() / 512.0;
            double root2 = data.consumeInt() / 512.0;
            double a = data.consumeBoolean() ? 1.0 : -1.0;
            double[] coeffs = new double[] { a * root1 * root2, -a * (root1 + root2), a };
            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);

            min = root1;
            max = root2;
            if (data.consumeBoolean()) {
                min -= Math.abs(data.consumeInt()) / 512.0;
            }
            if (data.consumeBoolean()) {
                max += Math.abs(data.consumeInt()) / 512.0;
            }
            if (data.consumeBoolean()) {
                double t = min;
                min = max;
                max = t;
            }
        } else if (mode == 4) {
            double root = data.consumeInt() / 1024.0;
            double scale = data.consumeBoolean() ? 1.0 : (data.consumeInt() / 1024.0);
            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                    new double[] { -scale * root, scale });

            min = data.consumeBoolean() ? root : (root - Math.abs(data.consumeInt()) / 1024.0);
            max = data.consumeBoolean() ? root : (root + Math.abs(data.consumeInt()) / 1024.0);
            if (data.consumeBoolean()) {
                double t = min;
                min = max;
                max = t;
            }
        } else {
            double constant;
            if (data.consumeBoolean()) {
                constant = 0.0;
            } else {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                constant = Double.longBitsToDouble(bits);
            }
            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                    new double[] { constant });

            min = data.consumeInt() / 128.0;
            max = data.consumeInt() / 128.0;
        }

        solver.solve(maxEval, function, min, max, allowed);
    }
}