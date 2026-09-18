package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int shape = data.consumeInt(0, 7);

        double a = data.consumeInt();
        double b = data.consumeInt();
        double c = data.consumeInt();
        double d = data.consumeInt();

        a = (data.consumeBoolean() ? a : a / 1024.0);
        b = (data.consumeBoolean() ? b : b / 1024.0);
        c = (data.consumeBoolean() ? c : c / 1024.0);
        d = (data.consumeBoolean() ? d : d / 1024.0);

        double[] coefficients;
        switch (shape) {
            case 0: {
                int len = data.consumeInt(1, 8);
                coefficients = new double[len];
                for (int i = 0; i < len; i++) {
                    int kind = data.consumeInt(0, 7);
                    switch (kind) {
                        case 0:
                            coefficients[i] = 0.0;
                            break;
                        case 1:
                            coefficients[i] = data.consumeByte();
                            break;
                        case 2:
                            coefficients[i] = data.consumeInt(-16, 16);
                            break;
                        case 3:
                            coefficients[i] = data.consumeInt() / 4096.0;
                            break;
                        case 4:
                            coefficients[i] = Double.NaN;
                            break;
                        case 5:
                            coefficients[i] = Double.POSITIVE_INFINITY;
                            break;
                        case 6:
                            coefficients[i] = Double.NEGATIVE_INFINITY;
                            break;
                        default:
                            coefficients[i] = data.consumeBoolean() ? a : b;
                            break;
                    }
                }
                break;
            }
            case 1:
                coefficients = new double[] { -a, 1.0 };
                break;
            case 2:
                coefficients = new double[] { a * b, -(a + b), 1.0 };
                break;
            case 3:
                coefficients = new double[] { a * a, -2.0 * a, 1.0 };
                break;
            case 4:
                coefficients = new double[] { 0.0 };
                break;
            case 5:
                coefficients = new double[] { a };
                break;
            case 6:
                coefficients = new double[] { c, b, a };
                break;
            default:
                coefficients = new double[] { d, c, b, a };
                break;
        }

        UnivariateRealFunction function = new PolynomialFunction(coefficients);

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

        AllowedSolution[] allowedValues = AllowedSolution.values();
        AllowedSolution allowed = allowedValues[data.consumeInt(0, allowedValues.length - 1)];

        double min;
        double max;
        switch (data.consumeInt(0, 7)) {
            case 0:
                min = a;
                max = b;
                break;
            case 1:
                min = b;
                max = a;
                break;
            case 2:
                min = a;
                max = a;
                break;
            case 3:
                min = a - 1.0;
                max = a + 1.0;
                break;
            case 4:
                min = b - 1.0;
                max = b + 1.0;
                break;
            case 5:
                min = -1.0;
                max = 1.0;
                break;
            case 6:
                min = -Math.abs(a);
                max = Math.abs(b);
                break;
            default:
                min = data.consumeBoolean() ? Double.NEGATIVE_INFINITY : c;
                max = data.consumeBoolean() ? Double.POSITIVE_INFINITY : d;
                break;
        }

        int maxEval = data.consumeInt(0, 10000);

        if (data.consumeBoolean()) {
            solver.solve(maxEval, function, min, max, allowed);
        } else {
            double startValue;
            switch (data.consumeInt(0, 5)) {
                case 0:
                    startValue = a;
                    break;
                case 1:
                    startValue = b;
                    break;
                case 2:
                    startValue = 0.5 * (min + max);
                    break;
                case 3:
                    startValue = min;
                    break;
                case 4:
                    startValue = max;
                    break;
                default:
                    startValue = c;
                    break;
            }
            solver.solve(maxEval, function, min, max, startValue, allowed);
        }
    }
}