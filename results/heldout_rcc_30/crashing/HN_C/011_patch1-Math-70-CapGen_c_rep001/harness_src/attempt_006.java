package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        int coeffCount = data.consumeInt(1, 8);
        double[] coefficients = new double[coeffCount];

        for (int i = 0; i < coeffCount; i++) {
            int kind = data.consumeInt(0, 10);
            switch (kind) {
                case 0:
                    coefficients[i] = 0.0d;
                    break;
                case 1:
                    coefficients[i] = -0.0d;
                    break;
                case 2:
                    coefficients[i] = 1.0d;
                    break;
                case 3:
                    coefficients[i] = -1.0d;
                    break;
                case 4:
                    coefficients[i] = Double.MIN_VALUE;
                    break;
                case 5:
                    coefficients[i] = -Double.MIN_VALUE;
                    break;
                case 6:
                    coefficients[i] = Double.MAX_VALUE;
                    break;
                case 7:
                    coefficients[i] = -Double.MAX_VALUE;
                    break;
                case 8: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    coefficients[i] = Double.longBitsToDouble(bits);
                    break;
                }
                case 9: {
                    int numerator = data.consumeInt();
                    int denominator = data.consumeInt();
                    if (denominator == 0) {
                        denominator = 1;
                    }
                    coefficients[i] = ((double) numerator) / ((double) denominator);
                    break;
                }
                default:
                    coefficients[i] = (double) data.consumeByte();
                    break;
            }
        }

        PolynomialFunction f = new PolynomialFunction(coefficients);
        BisectionSolver solver = new BisectionSolver();

        double[] values = new double[3];
        for (int i = 0; i < values.length; i++) {
            int kind = data.consumeInt(0, 12);
            switch (kind) {
                case 0:
                    values[i] = 0.0d;
                    break;
                case 1:
                    values[i] = -0.0d;
                    break;
                case 2:
                    values[i] = 1.0d;
                    break;
                case 3:
                    values[i] = -1.0d;
                    break;
                case 4:
                    values[i] = Double.MIN_VALUE;
                    break;
                case 5:
                    values[i] = -Double.MIN_VALUE;
                    break;
                case 6:
                    values[i] = Double.MAX_VALUE;
                    break;
                case 7:
                    values[i] = -Double.MAX_VALUE;
                    break;
                case 8:
                    values[i] = Double.POSITIVE_INFINITY;
                    break;
                case 9:
                    values[i] = Double.NEGATIVE_INFINITY;
                    break;
                case 10:
                    values[i] = Double.NaN;
                    break;
                case 11: {
                    long bits = (((long) data.consumeInt()) << 32) ^ (data.consumeInt() & 0xffffffffL);
                    values[i] = Double.longBitsToDouble(bits);
                    break;
                }
                default:
                    values[i] = (double) data.consumeInt();
                    break;
            }
        }

        double min = values[0];
        double max = values[1];
        double initial = values[2];

        switch (data.consumeInt(0, 7)) {
            case 0:
                break;
            case 1: {
                double tmp = min;
                min = max;
                max = tmp;
                break;
            }
            case 2:
                max = min;
                break;
            case 3:
                initial = min;
                break;
            case 4:
                initial = max;
                break;
            case 5:
                min = -Math.abs(min);
                max = Math.abs(max);
                break;
            case 6:
                min = initial;
                break;
            default:
                max = initial;
                break;
        }

        try {
            solver.solve(f, min, max, initial);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}