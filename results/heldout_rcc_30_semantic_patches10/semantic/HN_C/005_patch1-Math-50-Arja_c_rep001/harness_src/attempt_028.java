package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int maxEval = data.consumeInt(-32, 512);

        double aRaw;
        switch (data.consumeInt(0, 9)) {
            case 0: aRaw = 0.0; break;
            case 1: aRaw = -0.0; break;
            case 2: aRaw = 1.0; break;
            case 3: aRaw = -1.0; break;
            case 4: aRaw = Double.NaN; break;
            case 5: aRaw = Double.POSITIVE_INFINITY; break;
            case 6: aRaw = Double.NEGATIVE_INFINITY; break;
            case 7: aRaw = data.consumeInt() / 16.0; break;
            case 8: aRaw = data.consumeInt() / 1024.0; break;
            default: aRaw = data.consumeInt();
        }

        double r;
        switch (data.consumeInt(0, 9)) {
            case 0: r = 0.0; break;
            case 1: r = -0.0; break;
            case 2: r = 1.0; break;
            case 3: r = -1.0; break;
            case 4: r = Double.NaN; break;
            case 5: r = Double.POSITIVE_INFINITY; break;
            case 6: r = Double.NEGATIVE_INFINITY; break;
            case 7: r = data.consumeInt() / 16.0; break;
            case 8: r = data.consumeInt() / 1024.0; break;
            default: r = data.consumeInt();
        }

        double d0;
        switch (data.consumeInt(0, 9)) {
            case 0: d0 = 0.0; break;
            case 1: d0 = -0.0; break;
            case 2: d0 = 1.0; break;
            case 3: d0 = -1.0; break;
            case 4: d0 = Double.NaN; break;
            case 5: d0 = Double.POSITIVE_INFINITY; break;
            case 6: d0 = Double.NEGATIVE_INFINITY; break;
            case 7: d0 = data.consumeInt() / 16.0; break;
            case 8: d0 = data.consumeInt() / 1024.0; break;
            default: d0 = data.consumeInt();
        }

        double d1;
        switch (data.consumeInt(0, 9)) {
            case 0: d1 = 0.0; break;
            case 1: d1 = -0.0; break;
            case 2: d1 = 1.0; break;
            case 3: d1 = -1.0; break;
            case 4: d1 = Double.NaN; break;
            case 5: d1 = Double.POSITIVE_INFINITY; break;
            case 6: d1 = Double.NEGATIVE_INFINITY; break;
            case 7: d1 = data.consumeInt() / 16.0; break;
            case 8: d1 = data.consumeInt() / 1024.0; break;
            default: d1 = data.consumeInt();
        }

        double start;
        switch (data.consumeInt(0, 9)) {
            case 0: start = 0.0; break;
            case 1: start = -0.0; break;
            case 2: start = 1.0; break;
            case 3: start = -1.0; break;
            case 4: start = Double.NaN; break;
            case 5: start = Double.POSITIVE_INFINITY; break;
            case 6: start = Double.NEGATIVE_INFINITY; break;
            case 7: start = data.consumeInt() / 16.0; break;
            case 8: start = data.consumeInt() / 1024.0; break;
            default: start = data.consumeInt();
        }

        org.apache.commons.math.analysis.UnivariateRealFunction function;

        int functionMode = data.consumeInt(0, 3);
        if (functionMode == 0) {
            double a = aRaw;
            if (a == 0.0 || Double.isNaN(a)) {
                a = 1.0;
            }
            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                    new double[] { -a * r, a });
        } else if (functionMode == 1) {
            double a = aRaw;
            if (a == 0.0 || Double.isNaN(a)) {
                a = -1.0;
            }
            double rr = r * r;
            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                    new double[] { a * rr, -2.0 * a * r, a });
        } else if (functionMode == 2) {
            int len = data.consumeInt(0, 8);
            double[] coeffs = new double[len];
            for (int i = 0; i < len; i++) {
                switch (data.consumeInt(0, 9)) {
                    case 0: coeffs[i] = 0.0; break;
                    case 1: coeffs[i] = -0.0; break;
                    case 2: coeffs[i] = 1.0; break;
                    case 3: coeffs[i] = -1.0; break;
                    case 4: coeffs[i] = Double.NaN; break;
                    case 5: coeffs[i] = Double.POSITIVE_INFINITY; break;
                    case 6: coeffs[i] = Double.NEGATIVE_INFINITY; break;
                    case 7: coeffs[i] = data.consumeInt() / 16.0; break;
                    case 8: coeffs[i] = data.consumeInt() / 1024.0; break;
                    default: coeffs[i] = data.consumeInt();
                }
            }
            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);
        } else {
            double a = aRaw;
            if (a == 0.0 || Double.isNaN(a)) {
                a = 1.0;
            }
            double s;
            switch (data.consumeInt(0, 6)) {
                case 0: s = 0.0; break;
                case 1: s = 1.0; break;
                case 2: s = -1.0; break;
                case 3: s = data.consumeInt() / 8.0; break;
                case 4: s = data.consumeInt() / 256.0; break;
                case 5: s = Double.POSITIVE_INFINITY; break;
                default: s = Double.NEGATIVE_INFINITY;
            }
            double c0 = a * r * s;
            double c1 = -a * (r + s);
            double c2 = a;
            function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                    new double[] { c0, c1, c2 });
        }

        double min;
        double max;
        if (data.consumeBoolean()) {
            min = r - Math.abs(d0);
            max = r + Math.abs(d1);
        } else {
            min = d0;
            max = d1;
        }
        if (data.consumeBoolean()) {
            double tmp = min;
            min = max;
            max = tmp;
        }

        BaseAbstractUnivariateRealSolver<org.apache.commons.math.analysis.UnivariateRealFunction> solver;
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

        if (data.consumeBoolean()) {
            solver.solve(maxEval, function, min, max);
        } else {
            solver.solve(maxEval, function, min, max, start);
        }
    }
}