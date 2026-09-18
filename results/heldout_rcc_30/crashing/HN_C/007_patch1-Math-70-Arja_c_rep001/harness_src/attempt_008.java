package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int mode = data.consumeInt(0, 3);

        PolynomialFunction f;
        double min;
        double max;
        double initial;

        if (mode == 0) {
            double root = data.consumeInt();
            double leftWidth = data.consumeInt(0, 1024);
            double rightWidth = data.consumeInt(0, 1024);

            f = new PolynomialFunction(new double[] { -root, 1.0 });
            min = root - leftWidth;
            max = root + rightWidth;

            if (data.consumeBoolean()) {
                double t = min;
                min = max;
                max = t;
            }
            if (data.consumeBoolean()) {
                min = max;
            }

            switch (data.consumeInt(0, 4)) {
                case 0:
                    initial = root;
                    break;
                case 1:
                    initial = min;
                    break;
                case 2:
                    initial = max;
                    break;
                case 3:
                    initial = root + data.consumeInt();
                    break;
                default:
                    initial = Double.longBitsToDouble(
                        (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
                    break;
            }
        } else if (mode == 1) {
            int degree = data.consumeInt(0, 8);
            double[] coeffs = new double[degree + 1];
            for (int i = 0; i < coeffs.length; i++) {
                coeffs[i] = data.consumeInt();
            }
            if (data.consumeBoolean()) {
                coeffs[0] = 0.0;
            }
            if (coeffs.length == 1 && coeffs[0] == 0.0) {
                coeffs[0] = 1.0;
            }

            f = new PolynomialFunction(coeffs);
            min = data.consumeInt();
            max = data.consumeInt();
            initial = data.consumeBoolean()
                ? data.consumeInt()
                : Double.longBitsToDouble(
                    (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
        } else if (mode == 2) {
            double root = data.consumeInt();
            double scale = data.consumeInt(0, 16) + 1.0;

            f = new PolynomialFunction(new double[] { -root * root, 0.0, 1.0 });
            min = -Math.abs(root) - scale;
            max = Math.abs(root) + scale;

            if (data.consumeBoolean()) {
                min = 0.0;
            }
            if (data.consumeBoolean()) {
                max = 0.0;
            }

            initial = data.consumeBoolean() ? root : -root;
        } else {
            byte[] raw = data.consumeBytes(data.consumeInt(0, 32));
            int coeffCount = (raw.length % 8) + 1;
            double[] coeffs = new double[coeffCount];
            for (int i = 0; i < coeffCount; i++) {
                int v = (i < raw.length) ? raw[i] : i;
                coeffs[i] = v;
            }
            if (coeffCount > 1 && data.consumeBoolean()) {
                coeffs[coeffCount - 1] = 1.0;
            } else if (coeffCount == 1 && coeffs[0] == 0.0) {
                coeffs[0] = -1.0;
            }

            f = new PolynomialFunction(coeffs);
            min = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
            max = Double.longBitsToDouble(
                (((long) data.consumeInt()) << 32) | (data.consumeInt() & 0xffffffffL));
            initial = data.consumeBoolean() ? min : max;
        }

        BisectionSolver solver = new BisectionSolver();
        try {
            solver.solve(f, min, max, initial);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}