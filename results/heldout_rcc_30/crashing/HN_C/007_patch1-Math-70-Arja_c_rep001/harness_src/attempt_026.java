package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int len1 = data.consumeInt(0, 8);
        double[] c1 = new double[len1 == 0 ? 1 : len1];
        for (int i = 0; i < c1.length; i++) {
            int kind = data.consumeInt(0, 7);
            if (kind == 0) {
                c1[i] = 0.0d;
            } else if (kind == 1) {
                c1[i] = -0.0d;
            } else if (kind == 2) {
                c1[i] = Double.NaN;
            } else if (kind == 3) {
                c1[i] = Double.POSITIVE_INFINITY;
            } else if (kind == 4) {
                c1[i] = Double.NEGATIVE_INFINITY;
            } else if (kind == 5) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                c1[i] = Double.longBitsToDouble(bits);
            } else if (kind == 6) {
                c1[i] = data.consumeInt() / 1024.0d;
            } else {
                c1[i] = data.consumeByte();
            }
        }

        int len2 = data.consumeInt(0, 8);
        double[] c2 = new double[len2 == 0 ? 1 : len2];
        for (int i = 0; i < c2.length; i++) {
            int kind = data.consumeInt(0, 7);
            if (kind == 0) {
                c2[i] = 0.0d;
            } else if (kind == 1) {
                c2[i] = -0.0d;
            } else if (kind == 2) {
                c2[i] = Double.NaN;
            } else if (kind == 3) {
                c2[i] = Double.POSITIVE_INFINITY;
            } else if (kind == 4) {
                c2[i] = Double.NEGATIVE_INFINITY;
            } else if (kind == 5) {
                long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                c2[i] = Double.longBitsToDouble(bits);
            } else if (kind == 6) {
                c2[i] = data.consumeInt() / 1024.0d;
            } else {
                c2[i] = data.consumeByte();
            }
        }

        UnivariateRealFunction f1 = new PolynomialFunction(c1);
        UnivariateRealFunction f2 = new PolynomialFunction(c2);

        double min;
        switch (data.consumeInt(0, 8)) {
            case 0:
                min = 0.0d;
                break;
            case 1:
                min = -0.0d;
                break;
            case 2:
                min = Double.NaN;
                break;
            case 3:
                min = Double.POSITIVE_INFINITY;
                break;
            case 4:
                min = Double.NEGATIVE_INFINITY;
                break;
            case 5:
                min = data.consumeInt() / 4096.0d;
                break;
            case 6:
                min = data.consumeByte();
                break;
            case 7:
                min = data.consumeBoolean() ? Double.MIN_VALUE : -Double.MIN_VALUE;
                break;
            default:
                min = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                break;
        }

        double max;
        switch (data.consumeInt(0, 8)) {
            case 0:
                max = 0.0d;
                break;
            case 1:
                max = -0.0d;
                break;
            case 2:
                max = Double.NaN;
                break;
            case 3:
                max = Double.POSITIVE_INFINITY;
                break;
            case 4:
                max = Double.NEGATIVE_INFINITY;
                break;
            case 5:
                max = data.consumeInt() / 4096.0d;
                break;
            case 6:
                max = data.consumeByte();
                break;
            case 7:
                max = data.consumeBoolean() ? Double.MAX_VALUE : -Double.MAX_VALUE;
                break;
            default:
                max = Double.longBitsToDouble((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL));
                break;
        }

        double initial;
        switch (data.consumeInt(0, 8)) {
            case 0:
                initial = 0.0d;
                break;
            case 1:
                initial = -0.0d;
                break;
            case 2:
                initial = Double.NaN;
                break;
            case 3:
                initial = Double.POSITIVE_INFINITY;
                break;
            case 4:
                initial = Double.NEGATIVE_INFINITY;
                break;
            case 5:
                initial = data.consumeInt() / 4096.0d;
                break;
            case 6:
                initial = data.consumeByte();
                break;
            case 7:
                initial = min;
                break;
            default:
                initial = max;
                break;
        }

        try {
            int scenario = data.consumeInt(0, 4);
            if (scenario == 0) {
                BisectionSolver solver = new BisectionSolver();
                solver.solve(f1, min, max, initial);
            } else if (scenario == 1) {
                BisectionSolver solver = new BisectionSolver(f1);
                solver.solve(f1, min, max, initial);
            } else if (scenario == 2) {
                BisectionSolver solver = new BisectionSolver(f1);
                solver.solve(f2, min, max, initial);
            } else if (scenario == 3) {
                BisectionSolver solver = new BisectionSolver(f2);
                solver.solve(f1, max, min, initial);
            } else {
                BisectionSolver solver = new BisectionSolver(data.consumeBoolean() ? f1 : f2);
                solver.solve(data.consumeBoolean() ? f1 : f2, min, max, data.consumeBoolean() ? min : max);
            }
        } catch (MaxIterationsExceededException e) {
            throw new RuntimeException(e);
        } catch (FunctionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}