package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        AllowedSolution[] allowedValues = AllowedSolution.values();

        int allowedIdx1 = data.consumeInt();
        if (allowedIdx1 == Integer.MIN_VALUE) {
            allowedIdx1 = 0;
        }
        AllowedSolution allowed1 = allowedValues[Math.abs(allowedIdx1) % allowedValues.length];

        BaseSecantSolver solver1;
        switch (data.consumeInt(0, 2)) {
            case 0:
                solver1 = new RegulaFalsiSolver();
                break;
            case 1:
                solver1 = new IllinoisSolver();
                break;
            default:
                solver1 = new PegasusSolver();
                break;
        }

        double left1 = -1.0 - Math.abs((double) data.consumeInt());
        double right1 = 1.0 + Math.abs((double) data.consumeInt());
        int maxEval1 = data.consumeInt(1, 2048);

        org.apache.commons.math.analysis.polynomials.PolynomialFunction identity =
                new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] {0.0, 1.0});
        solver1.solve(maxEval1, identity, left1, right1, allowed1);

        int coeffCount = data.consumeInt(1, 8);
        double[] coeffs = new double[coeffCount];
        for (int i = 0; i < coeffCount; i++) {
            int raw = data.consumeInt();
            switch (data.consumeInt(0, 9)) {
                case 0:
                    coeffs[i] = 0.0;
                    break;
                case 1:
                    coeffs[i] = -0.0;
                    break;
                case 2:
                    coeffs[i] = 1.0;
                    break;
                case 3:
                    coeffs[i] = -1.0;
                    break;
                case 4:
                    coeffs[i] = raw;
                    break;
                case 5:
                    coeffs[i] = raw / 2.0;
                    break;
                case 6:
                    coeffs[i] = raw / 1024.0;
                    break;
                case 7:
                    coeffs[i] = raw * 1024.0;
                    break;
                case 8:
                    coeffs[i] = (raw == 0) ? Double.NaN : (raw > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
                    break;
                default:
                    coeffs[i] = (raw == Integer.MIN_VALUE) ? Double.MIN_VALUE : -raw;
                    break;
            }
        }

        org.apache.commons.math.analysis.polynomials.PolynomialFunction poly =
                new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);

        int allowedIdx2 = data.consumeInt();
        if (allowedIdx2 == Integer.MIN_VALUE) {
            allowedIdx2 = 0;
        }
        AllowedSolution allowed2 = allowedValues[Math.abs(allowedIdx2) % allowedValues.length];

        BaseSecantSolver solver2;
        switch (data.consumeInt(0, 2)) {
            case 0:
                solver2 = new RegulaFalsiSolver();
                break;
            case 1:
                solver2 = new IllinoisSolver();
                break;
            default:
                solver2 = new PegasusSolver();
                break;
        }

        double a = data.consumeInt();
        double b = data.consumeInt();
        if (data.consumeBoolean()) {
            double t = a;
            a = b;
            b = t;
        }
        int maxEval2 = data.consumeInt(1, 4096);
        solver2.solve(maxEval2, poly, a, b, allowed2);

        int allowedIdx3 = data.consumeInt();
        if (allowedIdx3 == Integer.MIN_VALUE) {
            allowedIdx3 = 0;
        }
        AllowedSolution allowed3 = allowedValues[Math.abs(allowedIdx3) % allowedValues.length];

        BaseSecantSolver solver3;
        switch (data.consumeInt(0, 2)) {
            case 0:
                solver3 = new RegulaFalsiSolver();
                break;
            case 1:
                solver3 = new IllinoisSolver();
                break;
            default:
                solver3 = new PegasusSolver();
                break;
        }

        org.apache.commons.math.analysis.polynomials.PolynomialFunction quadratic =
                new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] {-1.0, 0.0, 1.0});

        double base = Math.abs((double) data.consumeInt());
        double left3 = data.consumeBoolean() ? -base - 1.0 : -1.0;
        double right3 = data.consumeBoolean() ? base + 1.0 : 2.0;
        int maxEval3 = data.consumeInt(1, 4096);
        solver3.solve(maxEval3, quadratic, left3, right3, allowed3);
    }
}