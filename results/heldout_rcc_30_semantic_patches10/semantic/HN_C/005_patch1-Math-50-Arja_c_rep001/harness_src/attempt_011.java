package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int family = data.consumeInt(0, 4);
        int allowedIdx = data.consumeInt(0, AllowedSolution.values().length - 1);
        AllowedSolution allowed = AllowedSolution.values()[allowedIdx];
        int solverKind = data.consumeInt(0, 2);
        int callKind = data.consumeInt(0, 2);
        int maxEval = data.consumeInt();

        long bitsA = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long bitsB = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long bitsC = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long bitsD = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
        long bitsE = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);

        double a = Double.longBitsToDouble(bitsA);
        double b = Double.longBitsToDouble(bitsB);
        double c = Double.longBitsToDouble(bitsC);
        double d = Double.longBitsToDouble(bitsD);
        double e = Double.longBitsToDouble(bitsE);

        if (!Double.isFinite(a) || a == 0.0) {
            a = (data.consumeInt(-16, 16) == 0) ? 1.0 : data.consumeInt(-16, 16);
        }
        if (!Double.isFinite(b)) {
            b = data.consumeInt(-32, 32);
        }
        if (!Double.isFinite(c)) {
            c = data.consumeInt(-32, 32);
        }
        if (!Double.isFinite(d) || d == 0.0) {
            d = data.consumeInt(1, 16);
        }
        if (!Double.isFinite(e)) {
            e = data.consumeInt(-32, 32);
        }

        double[] coeffs;
        double root = b;
        double root2 = c;

        switch (family) {
            case 0:
                coeffs = new double[] { -a * root, a };
                break;
            case 1:
                coeffs = new double[] { a * root * root2, -a * (root + root2), a };
                break;
            case 2:
                coeffs = new double[] { -a * root * root * root, 3.0 * a * root * root, -3.0 * a * root, a };
                break;
            case 3:
                coeffs = new double[] { e };
                break;
            default:
                int n = data.consumeInt(1, 8);
                coeffs = new double[n];
                for (int i = 0; i < n; i++) {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    double v = Double.longBitsToDouble(bits);
                    if (!Double.isFinite(v)) {
                        v = data.consumeInt(-64, 64);
                    }
                    coeffs[i] = v;
                }
                boolean allZero = true;
                for (int i = 0; i < coeffs.length; i++) {
                    if (coeffs[i] != 0.0) {
                        allZero = false;
                        break;
                    }
                }
                if (allZero) {
                    coeffs[0] = 1.0;
                }
                break;
        }

        PolynomialFunction f = new PolynomialFunction(coeffs);

        double min;
        double max;
        double start;

        if (data.consumeBoolean()) {
            min = root - Math.abs(d);
            max = root + Math.abs(d);
        } else {
            min = b;
            max = c;
        }

        if (data.consumeBoolean()) {
            min = max;
        }
        if (data.consumeBoolean()) {
            double t = min;
            min = max;
            max = t;
        }

        if (data.consumeBoolean()) {
            min = Double.NEGATIVE_INFINITY;
        }
        if (data.consumeBoolean()) {
            max = Double.POSITIVE_INFINITY;
        }
        if (data.consumeBoolean()) {
            min = Double.NaN;
        }
        if (data.consumeBoolean()) {
            max = Double.NaN;
        }

        start = data.consumeBoolean() ? root : (min + max) * 0.5;
        if (data.consumeBoolean()) {
            start = Double.longBitsToDouble(((((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL)));
        }

        switch (solverKind) {
            case 0: {
                RegulaFalsiSolver solver = data.consumeBoolean()
                        ? new RegulaFalsiSolver()
                        : new RegulaFalsiSolver(Math.abs(a));
                if (callKind == 0) {
                    solver.solve(maxEval, f, min, max);
                } else if (callKind == 1) {
                    solver.solve(maxEval, f, min, max, start);
                } else {
                    solver.solve(maxEval, f, min, max, allowed);
                }
                break;
            }
            case 1: {
                IllinoisSolver solver = data.consumeBoolean()
                        ? new IllinoisSolver()
                        : new IllinoisSolver(Math.abs(a));
                if (callKind == 0) {
                    solver.solve(maxEval, f, min, max);
                } else if (callKind == 1) {
                    solver.solve(maxEval, f, min, max, start);
                } else {
                    solver.solve(maxEval, f, min, max, allowed);
                }
                break;
            }
            default: {
                PegasusSolver solver = data.consumeBoolean()
                        ? new PegasusSolver()
                        : new PegasusSolver(Math.abs(a));
                if (callKind == 0) {
                    solver.solve(maxEval, f, min, max);
                } else if (callKind == 1) {
                    solver.solve(maxEval, f, min, max, start);
                } else {
                    solver.solve(maxEval, f, min, max, allowed);
                }
                break;
            }
        }
    }
}