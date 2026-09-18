package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            int calls = data.consumeInt(1, 4);
            for (int call = 0; call < calls; call++) {
                UnivariateRealFunction function;
                int functionKind = data.consumeInt(0, 4);

                if (functionKind == 0) {
                    double root;
                    {
                        int mode = data.consumeInt(0, 7);
                        int hi = data.consumeInt();
                        int lo = data.consumeInt();
                        long bits = (((long) hi) << 32) ^ (lo & 0xffffffffL);
                        switch (mode) {
                            case 0:
                                root = 0.0;
                                break;
                            case 1:
                                root = 1.0;
                                break;
                            case 2:
                                root = -1.0;
                                break;
                            case 3:
                                root = data.consumeInt(-16, 16);
                                break;
                            case 4:
                                root = bits % 1024;
                                break;
                            case 5:
                                root = Double.longBitsToDouble(bits);
                                break;
                            case 6:
                                root = Double.POSITIVE_INFINITY;
                                break;
                            default:
                                root = Double.NaN;
                                break;
                        }
                    }
                    function = new PolynomialFunction(new double[] { -root, 1.0 });
                } else if (functionKind == 1) {
                    double c;
                    {
                        int mode = data.consumeInt(0, 6);
                        int hi = data.consumeInt();
                        int lo = data.consumeInt();
                        long bits = (((long) hi) << 32) ^ (lo & 0xffffffffL);
                        switch (mode) {
                            case 0:
                                c = 0.0;
                                break;
                            case 1:
                                c = 1.0;
                                break;
                            case 2:
                                c = -1.0;
                                break;
                            case 3:
                                c = data.consumeInt(-64, 64);
                                break;
                            case 4:
                                c = (double) (bits % 4096L);
                                break;
                            case 5:
                                c = Double.longBitsToDouble(bits);
                                break;
                            default:
                                c = Double.NEGATIVE_INFINITY;
                                break;
                        }
                    }
                    function = new PolynomialFunction(new double[] { c });
                } else if (functionKind == 2) {
                    double c;
                    {
                        int mode = data.consumeInt(0, 6);
                        int hi = data.consumeInt();
                        int lo = data.consumeInt();
                        long bits = (((long) hi) << 32) ^ (lo & 0xffffffffL);
                        switch (mode) {
                            case 0:
                                c = 0.0;
                                break;
                            case 1:
                                c = 1.0;
                                break;
                            case 2:
                                c = -1.0;
                                break;
                            case 3:
                                c = data.consumeInt(-25, 25);
                                break;
                            case 4:
                                c = (double) (bits % 256L);
                                break;
                            case 5:
                                c = Double.longBitsToDouble(bits);
                                break;
                            default:
                                c = Double.NaN;
                                break;
                        }
                    }
                    function = new PolynomialFunction(new double[] { -c, 0.0, 1.0 });
                } else {
                    int len = data.consumeInt(1, 8);
                    double[] coeffs = new double[len];
                    for (int i = 0; i < len; i++) {
                        int mode = data.consumeInt(0, 8);
                        int hi = data.consumeInt();
                        int lo = data.consumeInt();
                        long bits = (((long) hi) << 32) ^ (lo & 0xffffffffL);
                        switch (mode) {
                            case 0:
                                coeffs[i] = 0.0;
                                break;
                            case 1:
                                coeffs[i] = 1.0;
                                break;
                            case 2:
                                coeffs[i] = -1.0;
                                break;
                            case 3:
                                coeffs[i] = data.consumeInt(-1000, 1000);
                                break;
                            case 4:
                                coeffs[i] = ((double) data.consumeInt()) / 1024.0;
                                break;
                            case 5:
                                coeffs[i] = Double.longBitsToDouble(bits);
                                break;
                            case 6:
                                coeffs[i] = Double.POSITIVE_INFINITY;
                                break;
                            case 7:
                                coeffs[i] = Double.NEGATIVE_INFINITY;
                                break;
                            default:
                                coeffs[i] = Double.NaN;
                                break;
                        }
                    }
                    function = new PolynomialFunction(coeffs);
                }

                double lower;
                double initial;
                double upper;

                int boundsMode = data.consumeInt(0, 5);
                if (boundsMode == 0) {
                    double center = data.consumeInt(-20, 20);
                    double width = data.consumeInt(0, 20);
                    lower = center - width;
                    upper = center + width;
                    initial = center;
                } else if (boundsMode == 1) {
                    lower = data.consumeInt(-50, 50);
                    upper = data.consumeInt(-50, 50);
                    initial = data.consumeInt(-50, 50);
                } else if (boundsMode == 2) {
                    double v;
                    {
                        int hi = data.consumeInt();
                        int lo = data.consumeInt();
                        long bits = (((long) hi) << 32) ^ (lo & 0xffffffffL);
                        v = Double.longBitsToDouble(bits);
                    }
                    lower = v;
                    initial = v;
                    upper = v;
                } else if (boundsMode == 3) {
                    lower = Double.NEGATIVE_INFINITY;
                    upper = Double.POSITIVE_INFINITY;
                    initial = data.consumeBoolean() ? 0.0 : Double.NaN;
                } else if (boundsMode == 4) {
                    double a = data.consumeInt(-10, 10);
                    double b = data.consumeInt(-10, 10);
                    if (a <= b) {
                        lower = a;
                        upper = b;
                    } else {
                        lower = b;
                        upper = a;
                    }
                    initial = data.consumeBoolean() ? lower : upper;
                } else {
                    int hi1 = data.consumeInt();
                    int lo1 = data.consumeInt();
                    int hi2 = data.consumeInt();
                    int lo2 = data.consumeInt();
                    int hi3 = data.consumeInt();
                    int lo3 = data.consumeInt();
                    lower = Double.longBitsToDouble((((long) hi1) << 32) ^ (lo1 & 0xffffffffL));
                    initial = Double.longBitsToDouble((((long) hi2) << 32) ^ (lo2 & 0xffffffffL));
                    upper = Double.longBitsToDouble((((long) hi3) << 32) ^ (lo3 & 0xffffffffL));
                }

                int maxIterations;
                switch (data.consumeInt(0, 5)) {
                    case 0:
                        maxIterations = 0;
                        break;
                    case 1:
                        maxIterations = -1;
                        break;
                    case 2:
                        maxIterations = 1;
                        break;
                    case 3:
                        maxIterations = data.consumeInt(1, 8);
                        break;
                    case 4:
                        maxIterations = data.consumeInt(-100, 100);
                        break;
                    default:
                        maxIterations = data.consumeInt();
                        break;
                }

                UnivariateRealSolverUtils.bracket(function, initial, lower, upper, maxIterations);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}