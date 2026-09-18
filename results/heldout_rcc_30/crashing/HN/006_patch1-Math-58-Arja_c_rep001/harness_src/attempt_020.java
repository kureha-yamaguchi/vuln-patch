package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /* Contract/oracle used below:
         * GaussianFitter.fit() computes an initial guess from the observations and then performs fitting.
         * The overload fit(double[] initialGuess) is the same operation when given that very guess.
         * Therefore, for the same observations, fit() and fit(new ParameterGuesser(getObservations()).guess())
         * must agree whenever both calls return normally. A patch that merely deletes/avoids the failing call path
         * can violate this sibling-overload agreement without throwing.
         */

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

        // ANCHOR: exact failing test inputs first.
        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }
            try {
                fitter.fit();
            } catch (Throwable t) {
                boolean throughPatchedFit = false;
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                                && "fit".equals(st[i].getMethodName())) {
                            throughPatchedFit = true;
                            break;
                        }
                    }
                }
                boolean validationFamily =
                        (t instanceof IllegalArgumentException)
                        || (t instanceof NumberFormatException)
                        || t.getClass().getName().startsWith("org.apache.commons.math.exception.");
                boolean rootCause =
                        (t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)
                        && throughPatchedFit;
                if (rootCause) {
                    throw (RuntimeException) t;
                }
                if (validationFamily) {
                    return;
                }
                return;
            }
        }

        // ANCHOR metamorphic sibling-agreement check: fit() must agree with fit(guessedInitialParameters).
        {
            GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                f1.addObservedPoint(i, anchor[i]);
                f2.addObservedPoint(i, anchor[i]);
            }

            double[] guess;
            try {
                guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
            } catch (Throwable t) {
                return;
            }

            double[] r1;
            double[] r2;
            try {
                r1 = f1.fit();
                r2 = f2.fit(guess);
            } catch (Throwable t) {
                boolean throughPatchedFit = false;
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                                && "fit".equals(st[i].getMethodName())) {
                            throughPatchedFit = true;
                            break;
                        }
                    }
                }
                boolean validationFamily =
                        (t instanceof IllegalArgumentException)
                        || (t instanceof NumberFormatException)
                        || t.getClass().getName().startsWith("org.apache.commons.math.exception.");
                boolean rootCause =
                        (t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)
                        && throughPatchedFit;
                if (rootCause) {
                    throw (RuntimeException) t;
                }
                if (validationFamily) {
                    return;
                }
                return;
            }

            if (r1 != null && r2 != null && r1.length == r2.length) {
                for (int i = 0; i < r1.length; i++) {
                    double a = r1[i];
                    double b = r2[i];
                    boolean equal;
                    if (Double.isNaN(a) && Double.isNaN(b)) {
                        equal = true;
                    } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                        equal = (a == b);
                    } else {
                        double diff = Math.abs(a - b);
                        double tol = 1.0e-8 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                        equal = diff <= tol;
                    }
                    if (!equal) {
                        throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() != fit(guess()) input=anchor lhs=" + a + " rhs=" + b + " index=" + i);
                    }
                }
            }
        }

        // EXPLORE: varied, valid-by-construction, non-negative bell-shaped observations.
        int n = data.consumeInt(3, 40);
        double[] ys = new double[n];
        int xOffset = data.consumeInt(-20, 20);
        int step = data.consumeInt(1, 3);
        int centerIndex = data.consumeInt(0, n - 1);
        double center = xOffset + centerIndex * step + (data.consumeInt(-100, 100) / 100.0);
        double sigma = data.consumeInt(10, 800) / 100.0;
        double amplitude = Math.pow(10.0, data.consumeInt(-20, 3));
        double baseline = data.consumeBoolean() ? 0.0 : Math.pow(10.0, data.consumeInt(-25, -5));

        for (int i = 0; i < n; i++) {
            double x = xOffset + i * step;
            double z = (x - center) / sigma;
            double y = baseline + amplitude * Math.exp(-0.5 * z * z);

            // Small multiplicative noise while preserving non-negativity and moderate magnitudes.
            int noiseBucket = data.consumeInt(70, 130);
            y *= (noiseBucket / 100.0);

            // Optional zero padding / sparse tails to vary surrounding content.
            if ((i < 3 || i >= n - 3) && data.consumeBoolean()) {
                y *= data.consumeInt(0, 10) / 1000.0;
            }

            if (!(y >= 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 0.0;
            }
            ys[i] = y;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(xOffset + i * step, ys[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            boolean throughPatchedFit = false;
            StackTraceElement[] st = t.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                            && "fit".equals(st[i].getMethodName())) {
                        throughPatchedFit = true;
                        break;
                    }
                }
            }
            boolean validationFamily =
                    (t instanceof IllegalArgumentException)
                    || (t instanceof NumberFormatException)
                    || t.getClass().getName().startsWith("org.apache.commons.math.exception.");
            boolean rootCause =
                    (t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)
                    && throughPatchedFit;
            if (rootCause) {
                throw (RuntimeException) t;
            }
            if (validationFamily) {
                return;
            }
            return;
        }

        // EXPLORE sibling-agreement oracle.
        {
            GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                double x = xOffset + i * step;
                f1.addObservedPoint(x, ys[i]);
                f2.addObservedPoint(x, ys[i]);
            }

            double[] guess;
            try {
                guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
            } catch (Throwable t) {
                return;
            }

            double[] r1;
            double[] r2;
            try {
                r1 = f1.fit();
                r2 = f2.fit(guess);
            } catch (Throwable t) {
                boolean throughPatchedFit = false;
                StackTraceElement[] st = t.getStackTrace();
                if (st != null) {
                    for (int i = 0; i < st.length; i++) {
                        if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(st[i].getClassName())
                                && "fit".equals(st[i].getMethodName())) {
                            throughPatchedFit = true;
                            break;
                        }
                    }
                }
                boolean validationFamily =
                        (t instanceof IllegalArgumentException)
                        || (t instanceof NumberFormatException)
                        || t.getClass().getName().startsWith("org.apache.commons.math.exception.");
                boolean rootCause =
                        (t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)
                        && throughPatchedFit;
                if (rootCause) {
                    throw (RuntimeException) t;
                }
                if (validationFamily) {
                    return;
                }
                return;
            }

            if (r1 == null || r2 == null || r1.length != r2.length) {
                return;
            }

            for (int i = 0; i < r1.length; i++) {
                double a = r1[i];
                double b = r2[i];
                boolean equal;
                if (Double.isNaN(a) && Double.isNaN(b)) {
                    equal = true;
                } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                    equal = (a == b);
                } else {
                    double diff = Math.abs(a - b);
                    double tol = 1.0e-8 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                    equal = diff <= tol;
                }
                if (!equal) {
                    throw new RuntimeException("[oracle:sibling-fit] metamorphic violation: fit() != fit(guess()) input=n=" + ys.length + ",xOffset=" + xOffset + ",step=" + step + " lhs=" + a + " rhs=" + b + " index=" + i);
                }
            }
        }
    }
}