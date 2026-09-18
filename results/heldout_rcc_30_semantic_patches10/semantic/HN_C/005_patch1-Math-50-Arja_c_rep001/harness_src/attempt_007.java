package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] v = new double[10];
        for (int i = 0; i < v.length; i++) {
            int kind = data.consumeInt(0, 11);
            int raw = data.consumeInt();
            switch (kind) {
                case 0:
                    v[i] = 0.0d;
                    break;
                case 1:
                    v[i] = 1.0d;
                    break;
                case 2:
                    v[i] = -1.0d;
                    break;
                case 3:
                    v[i] = raw;
                    break;
                case 4:
                    v[i] = raw / 16.0d;
                    break;
                case 5:
                    v[i] = raw / 1024.0d;
                    break;
                case 6:
                    v[i] = (raw % 1000);
                    break;
                case 7:
                    v[i] = (raw % 1000) / 1000.0d;
                    break;
                case 8:
                    v[i] = Double.NaN;
                    break;
                case 9:
                    v[i] = Double.POSITIVE_INFINITY;
                    break;
                case 10:
                    v[i] = Double.NEGATIVE_INFINITY;
                    break;
                default:
                    v[i] = Math.copySign(Double.MIN_VALUE, raw == 0 ? 1.0d : (double) raw);
                    break;
            }
        }

        double a = v[0];
        double b = v[1];
        double c = v[2];
        double r0 = v[3];
        double r1 = v[4];
        double span = v[5];
        double extra = v[6];

        double[] coefficients;
        switch (data.consumeInt(0, 6)) {
            case 0:
                coefficients = new double[] { a };
                break;
            case 1:
                coefficients = new double[] { -r0, 1.0d };
                break;
            case 2: {
                double scale = a == 0.0d ? 1.0d : a;
                coefficients = new double[] { -scale * r0, scale };
                break;
            }
            case 3:
                coefficients = new double[] { r0 * r1, -(r0 + r1), 1.0d };
                break;
            case 4:
                coefficients = new double[] { c, b, a };
                break;
            case 5:
                coefficients = new double[] { extra, c, b, a };
                break;
            default:
                coefficients = new double[] { 0.0d, -r0 * r1, (r0 + r1), 1.0d };
                break;
        }

        PolynomialFunction function = new PolynomialFunction(coefficients);

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

        AllowedSolution allowed;
        switch (data.consumeInt(0, 4)) {
            case 0:
                allowed = AllowedSolution.ANY_SIDE;
                break;
            case 1:
                allowed = AllowedSolution.LEFT_SIDE;
                break;
            case 2:
                allowed = AllowedSolution.RIGHT_SIDE;
                break;
            case 3:
                allowed = AllowedSolution.BELOW_SIDE;
                break;
            default:
                allowed = AllowedSolution.ABOVE_SIDE;
                break;
        }

        double min;
        double max;
        double start;

        switch (data.consumeInt(0, 6)) {
            case 0:
                min = r0 - 1.0d;
                max = r0 + 1.0d;
                start = (min + max) / 2.0d;
                break;
            case 1:
                min = r0;
                max = r0 + Math.abs(span) + 1.0d;
                start = min;
                break;
            case 2:
                min = r0 - Math.abs(span) - 1.0d;
                max = r0;
                start = max;
                break;
            case 3:
                min = r0 + Math.abs(span) + 1.0d;
                max = r0 - Math.abs(span) - 1.0d;
                start = (min + max) / 2.0d;
                break;
            case 4:
                min = a;
                max = b;
                start = c;
                break;
            case 5:
                min = -Math.abs(r0) - Math.abs(span) - 1.0d;
                max = Math.abs(r1) + Math.abs(span) + 1.0d;
                start = extra;
                break;
            default:
                min = 0.0d;
                max = 0.0d;
                start = 0.0d;
                break;
        }

        int maxEval = data.consumeInt(1, 256);

        switch (data.consumeInt(0, 3)) {
            case 0:
                solver.solve(maxEval, function, min, max);
                break;
            case 1:
                solver.solve(maxEval, function, min, max, start);
                break;
            case 2:
                solver.solve(maxEval, function, min, max, allowed);
                break;
            default:
                solver.solve(maxEval, function, min, max, start, allowed);
                break;
        }
    }
}