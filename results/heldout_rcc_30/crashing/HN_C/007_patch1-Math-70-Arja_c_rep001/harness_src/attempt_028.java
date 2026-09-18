package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        BisectionSolver solver = new BisectionSolver();

        try {
            if (data.consumeBoolean()) {
                solver.solve(new SinFunction(), -3.0, 3.0);
            }

            UnivariateRealFunction f;
            int functionKind = data.consumeInt(0, 1);
            if (functionKind == 0) {
                f = new SinFunction();
            } else {
                int len = data.consumeInt(0, 8);
                double[] coefficients = new double[len];
                for (int i = 0; i < len; i++) {
                    int mode = data.consumeInt(0, 9);
                    switch (mode) {
                        case 0:
                            coefficients[i] = 0.0;
                            break;
                        case 1:
                            coefficients[i] = -0.0;
                            break;
                        case 2:
                            coefficients[i] = 1.0;
                            break;
                        case 3:
                            coefficients[i] = -1.0;
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
                        case 7:
                            coefficients[i] = (double) data.consumeInt();
                            break;
                        case 8:
                            coefficients[i] = ((double) data.consumeInt()) / ((double) data.consumeInt(1, 1024));
                            break;
                        default:
                            coefficients[i] = ((double) data.consumeByte()) * Math.pow(2.0, data.consumeInt(-16, 16));
                            break;
                    }
                }
                f = new PolynomialFunction(coefficients);
            }

            double min;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    min = 0.0;
                    break;
                case 1:
                    min = -0.0;
                    break;
                case 2:
                    min = 1.0;
                    break;
                case 3:
                    min = -1.0;
                    break;
                case 4:
                    min = Double.NaN;
                    break;
                case 5:
                    min = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    min = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    min = (double) data.consumeInt();
                    break;
                case 8:
                    min = ((double) data.consumeInt()) / ((double) data.consumeInt(1, 1024));
                    break;
                default:
                    min = ((double) data.consumeByte()) * Math.pow(2.0, data.consumeInt(-16, 16));
                    break;
            }

            double max;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    max = 0.0;
                    break;
                case 1:
                    max = -0.0;
                    break;
                case 2:
                    max = 1.0;
                    break;
                case 3:
                    max = -1.0;
                    break;
                case 4:
                    max = Double.NaN;
                    break;
                case 5:
                    max = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    max = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    max = (double) data.consumeInt();
                    break;
                case 8:
                    max = ((double) data.consumeInt()) / ((double) data.consumeInt(1, 1024));
                    break;
                default:
                    max = ((double) data.consumeByte()) * Math.pow(2.0, data.consumeInt(-16, 16));
                    break;
            }

            double initial;
            switch (data.consumeInt(0, 9)) {
                case 0:
                    initial = 0.0;
                    break;
                case 1:
                    initial = -0.0;
                    break;
                case 2:
                    initial = 1.0;
                    break;
                case 3:
                    initial = -1.0;
                    break;
                case 4:
                    initial = Double.NaN;
                    break;
                case 5:
                    initial = Double.POSITIVE_INFINITY;
                    break;
                case 6:
                    initial = Double.NEGATIVE_INFINITY;
                    break;
                case 7:
                    initial = (double) data.consumeInt();
                    break;
                case 8:
                    initial = ((double) data.consumeInt()) / ((double) data.consumeInt(1, 1024));
                    break;
                default:
                    initial = ((double) data.consumeByte()) * Math.pow(2.0, data.consumeInt(-16, 16));
                    break;
            }

            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            FuzzHarness.<RuntimeException>sneakyThrow(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}