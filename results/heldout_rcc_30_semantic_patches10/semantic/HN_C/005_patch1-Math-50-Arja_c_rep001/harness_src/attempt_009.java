package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int functionKind = data.consumeInt(0, 4);

        org.apache.commons.math.analysis.polynomials.PolynomialFunction function;
        double knownRoot = 0.0;

        switch (functionKind) {
            case 0: {
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(new double[] {0.0});
                knownRoot = 0.0;
                break;
            }
            case 1: {
                double root = (double) data.consumeInt();
                double slope = data.consumeBoolean() ? 1.0 : -1.0;
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] {-slope * root, slope});
                knownRoot = root;
                break;
            }
            case 2: {
                double root = (double) data.consumeInt();
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] {root * root, -2.0 * root, 1.0});
                knownRoot = root;
                break;
            }
            case 3: {
                int degree = data.consumeInt(0, 8);
                double[] coeffs = new double[Math.max(1, degree + 1)];
                for (int i = 0; i < coeffs.length; i++) {
                    int raw = data.consumeInt();
                    switch (Math.abs(raw % 8)) {
                        case 0:
                            coeffs[i] = 0.0;
                            break;
                        case 1:
                            coeffs[i] = -0.0;
                            break;
                        case 2:
                            coeffs[i] = raw;
                            break;
                        case 3:
                            coeffs[i] = raw / 1024.0;
                            break;
                        case 4:
                            coeffs[i] = raw * 1024.0;
                            break;
                        case 5:
                            coeffs[i] = Double.NaN;
                            break;
                        case 6:
                            coeffs[i] = raw >= 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                            break;
                        default:
                            coeffs[i] = raw > 0 ? Double.MAX_VALUE : -Double.MAX_VALUE;
                            break;
                    }
                }
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(coeffs);
                knownRoot = data.consumeInt();
                break;
            }
            default: {
                double root = (double) data.consumeInt();
                double a = data.consumeBoolean() ? 1.0 : -1.0;
                function = new org.apache.commons.math.analysis.polynomials.PolynomialFunction(
                        new double[] {-a * root, a});
                function = function.polynomialDerivative();
                knownRoot = 0.0;
                break;
            }
        }

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

        double spanBase;
        switch (data.consumeInt(0, 7)) {
            case 0:
                spanBase = 0.0;
                break;
            case 1:
                spanBase = 1.0;
                break;
            case 2:
                spanBase = 2.0;
                break;
            case 3:
                spanBase = 1e-12;
                break;
            case 4:
                spanBase = 1e6;
                break;
            case 5:
                spanBase = Math.abs((double) data.consumeInt());
                break;
            case 6:
                spanBase = Double.MIN_VALUE;
                break;
            default:
                spanBase = Double.POSITIVE_INFINITY;
                break;
        }

        double min;
        double max;
        switch (data.consumeInt(0, 6)) {
            case 0:
                min = knownRoot - spanBase;
                max = knownRoot + spanBase;
                break;
            case 1:
                min = knownRoot;
                max = knownRoot + spanBase;
                break;
            case 2:
                min = knownRoot - spanBase;
                max = knownRoot;
                break;
            case 3:
                min = knownRoot + spanBase;
                max = knownRoot - spanBase;
                break;
            case 4:
                min = (double) data.consumeInt();
                max = (double) data.consumeInt();
                break;
            case 5:
                min = data.consumeBoolean() ? Double.NaN : Double.NEGATIVE_INFINITY;
                max = data.consumeBoolean() ? Double.NaN : Double.POSITIVE_INFINITY;
                break;
            default:
                min = 0.0;
                max = 0.0;
                break;
        }

        int maxEval;
        switch (data.consumeInt(0, 5)) {
            case 0:
                maxEval = 0;
                break;
            case 1:
                maxEval = 1;
                break;
            case 2:
                maxEval = 2;
                break;
            case 3:
                maxEval = 100;
                break;
            case 4:
                maxEval = data.consumeInt(-10, 1000);
                break;
            default:
                maxEval = data.consumeInt();
                break;
        }

        if (data.consumeBoolean()) {
            solver.solve(maxEval, function, min, max, allowed);
        } else {
            solver.solve(maxEval, function, min, max);
        }
    }
}