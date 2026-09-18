package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BisectionSolver solver = new BisectionSolver();

        long bitsRoot = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        double root = Double.longBitsToDouble(bitsRoot);
        if (Double.isNaN(root) || Double.isInfinite(root)) {
            root = data.consumeInt(-32, 32);
        }

        long bitsScale = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        double scale = Double.longBitsToDouble(bitsScale);
        if (Double.isNaN(scale) || Double.isInfinite(scale) || scale == 0.0d) {
            scale = data.consumeBoolean() ? 1.0d : -1.0d;
        }

        long bitsSpan = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        double span = Double.longBitsToDouble(bitsSpan);
        if (Double.isNaN(span) || Double.isInfinite(span) || !(span > 0.0d)) {
            span = Math.abs((double) data.consumeInt(-32, 32)) + (data.consumeBoolean() ? 0.0d : 1.0d);
        }

        PolynomialFunction f;
        if (data.consumeBoolean()) {
            int degreePlusOne = data.consumeInt(1, 8);
            double[] coeffs = new double[degreePlusOne];
            for (int i = 0; i < coeffs.length; i++) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                double v = Double.longBitsToDouble(bits);
                if (Double.isNaN(v) || Double.isInfinite(v)) {
                    v = data.consumeInt(-16, 16);
                }
                coeffs[i] = v;
            }
            f = new PolynomialFunction(coeffs);
        } else {
            f = new PolynomialFunction(new double[] { -scale * root, scale });
        }

        double min = root - span;
        double max = root + span;

        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }
        if (data.consumeBoolean()) {
            min = root;
        }
        if (data.consumeBoolean()) {
            max = root;
        }

        long bitsInitial = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        double initial = Double.longBitsToDouble(bitsInitial);
        if (data.consumeBoolean()) {
            initial = root;
        } else if (Double.isNaN(initial) || Double.isInfinite(initial)) {
            initial = data.consumeInt(-64, 64);
        }

        try {
            if (data.consumeBoolean()) {
                double primeRoot = data.consumeInt(-8, 8);
                double primeSpan = Math.abs(data.consumeInt(-8, 8)) + 1.0d;
                PolynomialFunction prime = new PolynomialFunction(new double[] { -primeRoot, 1.0d });
                solver.solve(prime, primeRoot - primeSpan, primeRoot + primeSpan);
            }

            solver.solve(f, min, max, initial);
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}