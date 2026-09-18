package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * The no-arg fit() overload and fit(double[] initialGuess) must agree when
         * initialGuess is exactly the ParameterGuesser-produced guess for the same
         * observations, because the patched implementation is literally:
         *   return fit((new ParameterGuesser(getObservations())).guess());
         * A patch that merely suppresses the throw or returns a wrong result from
         * fit() would violate this sibling-overload agreement.
         */

        final double[] anchorData = new double[] {
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
            for (int i = 0; i < anchorData.length; i++) {
                fitter.addObservedPoint(i, anchorData[i]);
            }

            try {
                fitter.fit();
            } catch (RuntimeException t) {
                boolean throughFit = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if (t instanceof NotStrictlyPositiveException && throughFit) {
                    throw t;
                }
            }

            try {
                double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
                double[] lhs = fitter.fit();
                double[] rhs = fitter.fit(guess);
                if (lhs != null && rhs != null) {
                    if (lhs.length != rhs.length) {
                        throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() and fit(guess) length mismatch lhs="
                                + lhs.length + " rhs=" + rhs.length);
                    }
                    for (int i = 0; i < lhs.length; i++) {
                        double a = lhs[i];
                        double b = rhs[i];
                        boolean equal;
                        if (Double.isNaN(a) || Double.isNaN(b)) {
                            equal = Double.isNaN(a) && Double.isNaN(b);
                        } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                            equal = a == b;
                        } else {
                            double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                            equal = Math.abs(a - b) <= 1.0e-7 * scale;
                        }
                        if (!equal) {
                            throw new RuntimeException("[oracle:fit-overload-anchor] metamorphic violation: fit() != fit(guess) index="
                                    + i + " lhs=" + a + " rhs=" + b);
                        }
                    }
                }
            } catch (RuntimeException t) {
                boolean throughFit = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if (t instanceof RuntimeException
                        && t.getMessage() != null
                        && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
                if (t instanceof NotStrictlyPositiveException && throughFit) {
                    throw t;
                }
            }
        }

        int cases = 1 + data.consumeInt(1, 3);
        Gaussian.Parametric generator = new Gaussian.Parametric();

        for (int c = 0; c < cases; c++) {
            int n = data.consumeInt(3, 60);
            double norm = 1.0e-6 + data.consumeInt(1, 1_000_000) / 10_000.0;
            double sigma = 0.2 + data.consumeInt(0, 2000) / 100.0;
            double mean = data.consumeInt(0, n - 1) + data.consumeInt(0, 1000) / 1000.0;
            double xStart = data.consumeInt(-1000, 1000) / 10.0;
            double step = 0.1 + data.consumeInt(0, 1000) / 100.0;
            double baseline = data.consumeInt(0, 1000) / 1_000_000.0;
            double jitterScale = data.consumeInt(0, 1000) / 1_000_000.0;

            double[] params = new double[] { norm, mean, sigma };
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

            for (int i = 0; i < n; i++) {
                double x = xStart + i * step;
                double mappedX = (x - xStart) / step;
                double y;
                try {
                    y = generator.value(mappedX, params);
                } catch (RuntimeException impossibleForValidParams) {
                    return;
                }
                y += baseline;
                if (jitterScale > 0.0) {
                    int signed = data.consumeInt(-1000, 1000);
                    double factor = 1.0 + signed * jitterScale / 1000.0;
                    if (factor <= 0.01) {
                        factor = 0.01;
                    }
                    y *= factor;
                }
                if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                    y = baseline + 1.0e-12;
                }
                fitter.addObservedPoint(x, y);
            }

            try {
                fitter.fit();
            } catch (RuntimeException t) {
                boolean throughFit = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if (t instanceof NotStrictlyPositiveException && throughFit) {
                    throw t;
                }
                continue;
            }

            try {
                double[] guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
                double[] lhs = fitter.fit();
                double[] rhs = fitter.fit(guess);

                if (lhs == null || rhs == null) {
                    continue;
                }
                if (lhs.length != rhs.length) {
                    throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() and fit(guess) length mismatch n="
                            + n + " lhs=" + lhs.length + " rhs=" + rhs.length);
                }

                for (int i = 0; i < lhs.length; i++) {
                    double a = lhs[i];
                    double b = rhs[i];
                    boolean equal;
                    if (Double.isNaN(a) || Double.isNaN(b)) {
                        equal = Double.isNaN(a) && Double.isNaN(b);
                    } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                        equal = a == b;
                    } else {
                        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                        equal = Math.abs(a - b) <= 1.0e-6 * scale;
                    }
                    if (!equal) {
                        throw new RuntimeException("[oracle:fit-overload-fuzz] metamorphic violation: fit() != fit(guess) n="
                                + n + " index=" + i + " lhs=" + a + " rhs=" + b);
                    }
                }
            } catch (RuntimeException t) {
                boolean throughFit = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        throughFit = true;
                        break;
                    }
                }
                if (t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
                if (t instanceof NotStrictlyPositiveException && throughFit) {
                    throw t;
                }
            }
        }
    }
}