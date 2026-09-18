package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction function;
        int functionMode = data.consumeInt(0, 4);

        if (functionMode == 0) {
            function = null;
        } else if (functionMode == 1) {
            function = new SinFunction();
        } else if (functionMode == 2) {
            int rawRoot = data.consumeInt();
            double root;
            switch (Math.abs(rawRoot % 8)) {
                case 0:
                    root = rawRoot;
                    break;
                case 1:
                    root = rawRoot / 10.0;
                    break;
                case 2:
                    root = rawRoot / 1000.0;
                    break;
                case 3:
                    root = 0.0;
                    break;
                case 4:
                    root = -0.0;
                    break;
                case 5:
                    root = Double.NaN;
                    break;
                case 6:
                    root = Double.POSITIVE_INFINITY;
                    break;
                default:
                    root = Double.NEGATIVE_INFINITY;
                    break;
            }
            function = new PolynomialFunction(new double[] { -root, 1.0 });
        } else if (functionMode == 3) {
            int len = data.consumeInt(1, 8);
            double[] coeffs = new double[len];
            for (int i = 0; i < len; i++) {
                int raw = data.consumeInt();
                switch (Math.abs(raw % 10)) {
                    case 0:
                        coeffs[i] = raw;
                        break;
                    case 1:
                        coeffs[i] = raw / 3.0;
                        break;
                    case 2:
                        coeffs[i] = raw / 1000.0;
                        break;
                    case 3:
                        coeffs[i] = 0.0;
                        break;
                    case 4:
                        coeffs[i] = -0.0;
                        break;
                    case 5:
                        coeffs[i] = Double.NaN;
                        break;
                    case 6:
                        coeffs[i] = Double.POSITIVE_INFINITY;
                        break;
                    case 7:
                        coeffs[i] = Double.NEGATIVE_INFINITY;
                        break;
                    case 8:
                        coeffs[i] = data.consumeBoolean() ? 1.0 : -1.0;
                        break;
                    default:
                        coeffs[i] = data.consumeByte();
                        break;
                }
            }
            function = new PolynomialFunction(coeffs);
        } else {
            function = new PolynomialFunction(new double[] { 0.0 });
        }

        int rawLower = data.consumeInt();
        int rawUpper = data.consumeInt();
        int rawInitial = data.consumeInt();

        double lowerBound;
        switch (Math.abs(rawLower % 8)) {
            case 0:
                lowerBound = rawLower;
                break;
            case 1:
                lowerBound = rawLower / 10.0;
                break;
            case 2:
                lowerBound = rawLower / 1000000.0;
                break;
            case 3:
                lowerBound = Double.NaN;
                break;
            case 4:
                lowerBound = Double.POSITIVE_INFINITY;
                break;
            case 5:
                lowerBound = Double.NEGATIVE_INFINITY;
                break;
            case 6:
                lowerBound = 0.0;
                break;
            default:
                lowerBound = data.consumeByte();
                break;
        }

        double upperBound;
        switch (Math.abs(rawUpper % 8)) {
            case 0:
                upperBound = rawUpper;
                break;
            case 1:
                upperBound = rawUpper / 10.0;
                break;
            case 2:
                upperBound = rawUpper / 1000000.0;
                break;
            case 3:
                upperBound = Double.NaN;
                break;
            case 4:
                upperBound = Double.POSITIVE_INFINITY;
                break;
            case 5:
                upperBound = Double.NEGATIVE_INFINITY;
                break;
            case 6:
                upperBound = 0.0;
                break;
            default:
                upperBound = data.consumeByte();
                break;
        }

        double initial;
        switch (Math.abs(rawInitial % 8)) {
            case 0:
                initial = rawInitial;
                break;
            case 1:
                initial = rawInitial / 10.0;
                break;
            case 2:
                initial = rawInitial / 1000000.0;
                break;
            case 3:
                initial = Double.NaN;
                break;
            case 4:
                initial = Double.POSITIVE_INFINITY;
                break;
            case 5:
                initial = Double.NEGATIVE_INFINITY;
                break;
            case 6:
                initial = 0.0;
                break;
            default:
                initial = data.consumeByte();
                break;
        }

        int maximumIterations;
        int iterMode = data.consumeInt(0, 5);
        if (iterMode == 0) {
            maximumIterations = data.consumeInt(-10, 10);
        } else if (iterMode == 1) {
            maximumIterations = 1;
        } else if (iterMode == 2) {
            maximumIterations = Integer.MAX_VALUE;
        } else if (iterMode == 3) {
            maximumIterations = Integer.MIN_VALUE;
        } else if (iterMode == 4) {
            maximumIterations = 100;
        } else {
            maximumIterations = data.consumeInt();
        }

        int scenario = data.consumeInt(0, 6);
        if (scenario == 0) {
            if (!(Double.isNaN(lowerBound) || Double.isNaN(upperBound) || Double.isInfinite(lowerBound) || Double.isInfinite(upperBound))) {
                if (lowerBound > upperBound) {
                    double t = lowerBound;
                    lowerBound = upperBound;
                    upperBound = t;
                }
                if (lowerBound == upperBound) {
                    upperBound = lowerBound + 1.0;
                }
                initial = lowerBound + (upperBound - lowerBound) / 2.0;
            }
            if (maximumIterations <= 0) {
                maximumIterations = 1;
            }
        } else if (scenario == 1) {
            initial = lowerBound - 1.0;
        } else if (scenario == 2) {
            upperBound = lowerBound;
        } else if (scenario == 3) {
            if (!(Double.isNaN(initial) || Double.isNaN(lowerBound) || Double.isNaN(upperBound))) {
                lowerBound = initial - 1.0;
                upperBound = initial + 1.0;
            }
            maximumIterations = data.consumeInt(-5, 0);
        } else if (scenario == 4) {
            if (!(Double.isNaN(initial) || Double.isInfinite(initial))) {
                lowerBound = initial;
                upperBound = initial + 1.0;
            }
            maximumIterations = 1;
        } else if (scenario == 5) {
            lowerBound = Double.NEGATIVE_INFINITY;
            upperBound = Double.POSITIVE_INFINITY;
            maximumIterations = data.consumeInt(1, 5);
        }

        try {
            UnivariateRealSolverUtils.bracket(function, initial, lowerBound, upperBound, maximumIterations);
        } catch (Throwable t) {
            throwUnchecked(t);
        }
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>throwAny(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwAny(Throwable t) throws T {
        throw (T) t;
    }
}