package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;
import org.apache.commons.math.exception.NotStrictlyPositiveException;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] anchor = new double[] {
            1.1143831578403364E-29,
            4.95281403484594E-28,
            1.1171347211930288E-26,
            1.7044813962636277E-25,
            1.9784716574832164E-24,
            1.8630236407866774E-23,
            1.4820532905097742E-22,
            1.0241963854632831E-21,
            6.275077366673128E-21,
            3.461808994532493E-20,
            1.7407124684715706E-19,
            8.056687953553974E-19,
            3.460193945992071E-18,
            1.3883326374011525E-17,
            5.233894983671116E-17,
            1.8630791465263745E-16,
            6.288759227922111E-16,
            2.0204433920597856E-15,
            6.198768938576155E-15,
            1.821419346860626E-14,
            5.139176445538471E-14,
            1.3956427429045787E-13,
            3.655705706448139E-13,
            9.253753324779779E-13,
            2.267636001476696E-12,
            5.3880460095836855E-12,
            1.2431632654852931E-11
        };

        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint((double) i, anchor[i]);
            }
            try {
                fitter.fit();
            } catch (RuntimeException t) {
                boolean inFit = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                            && "fit".equals(st[i].getMethodName())) {
                        inFit = true;
                        break;
                    }
                }
                if (inFit && t instanceof NotStrictlyPositiveException) {
                    throw t;
                }
            }
        }

        {
            GaussianFitter left = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter right = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                left.addObservedPoint((double) i, anchor[i]);
                right.addObservedPoint((double) i, anchor[i]);
            }
            try {
                double[] lhs = left.fit();
                double[] guess = (new GaussianFitter.ParameterGuesser(right.getObservations())).guess();
                double[] rhs = right.fit(guess);
                if (lhs != null && rhs != null && lhs.length == rhs.length && lhs.length > 0) {
                    for (int i = 0; i < lhs.length; i++) {
                        double a = lhs[i];
                        double b = rhs[i];
                        if (Double.isNaN(a) != Double.isNaN(b)) {
                            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagree on NaN state input=anchor lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                        }
                        if (!Double.isNaN(a) && !Double.isNaN(b)) {
                            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                            if (Math.abs(a - b) > 1.0e-6 * scale) {
                                throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagree input=anchor lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                            }
                        }
                    }
                }
            } catch (RuntimeException t) {
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
            } catch (Error t) {
            }
        }

        int len = data.consumeInt(8, 40);
        int xStart = data.consumeInt(-50, 50);
        int xStep = data.consumeInt(1, 4);

        double amplitudeBase = (double) data.consumeInt(1, 1000000);
        double amplitudeScale = Math.pow(10.0, -data.consumeInt(0, 12));
        double amplitude = amplitudeBase * amplitudeScale;
        if (!(amplitude > 0.0) || Double.isInfinite(amplitude) || Double.isNaN(amplitude)) {
            amplitude = 1.0;
        }

        double sigma = ((double) data.consumeInt(1, 5000)) / 100.0;
        if (!(sigma > 0.0) || Double.isInfinite(sigma) || Double.isNaN(sigma)) {
            sigma = 1.0;
        }

        int peakBeyondLast = data.consumeInt(1, 60);
        double center = xStart + (len - 1) * xStep + peakBeyondLast;

        double[] ys = new double[len];
        double[] xs = new double[len];
        for (int i = 0; i < len; i++) {
            double x = xStart + i * xStep;
            xs[i] = x;
            double dx = x - center;
            double exponent = -(dx * dx) / (2.0 * sigma * sigma);
            double y = amplitude * Math.exp(exponent);
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Double.MIN_VALUE;
            }
            ys[i] = y;
        }

        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitter.addObservedPoint(xs[i], ys[i]);
            }
            try {
                fitter.fit();
            } catch (RuntimeException t) {
                boolean inFit = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                            && "fit".equals(st[i].getMethodName())) {
                        inFit = true;
                        break;
                    }
                }
                if (inFit && t instanceof NotStrictlyPositiveException) {
                    throw t;
                }
            }
        }

        {
            GaussianFitter left = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter right = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                left.addObservedPoint(xs[i], ys[i]);
                right.addObservedPoint(xs[i], ys[i]);
            }

            try {
                /*
                 * Contract/oracle: the documented same-name overloads fit() / fit(double[] initialGuess)
                 * must agree when fit() itself computes that same initial guess. The patched code changes
                 * fit() exactly to delegate to fit(guess), so deleting/short-circuiting that behavior would
                 * make these two real-library calls disagree observably.
                 */
                double[] lhs = left.fit();
                double[] guess = (new GaussianFitter.ParameterGuesser(right.getObservations())).guess();
                double[] rhs = right.fit(guess);

                if (lhs == null || rhs == null || lhs.length != rhs.length || lhs.length == 0) {
                    throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(initialGuess) shape mismatch inputLen=" + len + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                }

                for (int i = 0; i < lhs.length; i++) {
                    double a = lhs[i];
                    double b = rhs[i];
                    if (Double.isNaN(a) != Double.isNaN(b)) {
                        throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagree on NaN state inputLen=" + len + " xs=" + java.util.Arrays.toString(xs) + " ys=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                    }
                    if (Double.isInfinite(a) != Double.isInfinite(b)) {
                        throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagree on infinity state inputLen=" + len + " xs=" + java.util.Arrays.toString(xs) + " ys=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                    }
                    if (!Double.isNaN(a) && !Double.isNaN(b) && !Double.isInfinite(a) && !Double.isInfinite(b)) {
                        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                        if (Math.abs(a - b) > 1.0e-6 * scale) {
                            throw new RuntimeException("[oracle:overload-agreement] metamorphic violation: fit() and fit(initialGuess) disagree inputLen=" + len + " xs=" + java.util.Arrays.toString(xs) + " ys=" + java.util.Arrays.toString(ys) + " lhs=" + java.util.Arrays.toString(lhs) + " rhs=" + java.util.Arrays.toString(rhs));
                        }
                    }
                }
            } catch (RuntimeException t) {
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
                boolean inFit = false;
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                            && "fit".equals(st[i].getMethodName())) {
                        inFit = true;
                        break;
                    }
                }
                if (inFit && t instanceof NotStrictlyPositiveException) {
                    throw t;
                }
            } catch (Error t) {
            }
        }
    }
}