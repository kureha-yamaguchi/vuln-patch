package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /*
         * Oracle asserted below:
         * GaussianFitter.fit() and GaussianFitter.fit(double[] initialGuess) are same-name overloads
         * documented to perform the fit on the same observations, and the patched implementation of fit()
         * is exactly "return fit((new ParameterGuesser(getObservations())).guess());".
         * Therefore, for any observation set where both calls return normally, fit() must produce the same
         * result as fit(new ParameterGuesser(getObservations()).guess()).
         * A patch that merely deletes/avoids the throwing path but returns a different result would violate this.
         */

        final double[] anchor = new double[] {
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

        // ANCHOR: exact failing test input first.
        {
            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitter.addObservedPoint(i, anchor[i]);
            }
            try {
                fitter.fit();
            } catch (RuntimeException t) {
                boolean inFit = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        inFit = true;
                        break;
                    }
                }
                String cn = t.getClass().getName();
                boolean rootCause = cn.endsWith("NotStrictlyPositiveException");
                boolean cleanRejection =
                        t instanceof IllegalArgumentException
                        || cn.contains(".exception.")
                        || cn.contains("NumberFormat");
                if (rootCause && inFit) {
                    throw t;
                }
                if (cleanRejection) {
                    return;
                }
                // Swallow out-of-scope failures.
            }
        }

        // Post-condition / sibling-overload agreement on the exact anchor input.
        {
            GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                fitterA.addObservedPoint(i, anchor[i]);
                fitterB.addObservedPoint(i, anchor[i]);
            }
            try {
                double[] lhs = fitterA.fit();
                double[] guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
                double[] rhs = fitterB.fit(guess);
                if (lhs == null || rhs == null || lhs.length != rhs.length) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() vs fit(guess()) length/null mismatch");
                }
                for (int i = 0; i < lhs.length; i++) {
                    double a = lhs[i];
                    double b = rhs[i];
                    boolean same;
                    if (Double.isNaN(a) && Double.isNaN(b)) {
                        same = true;
                    } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                        same = a == b;
                    } else {
                        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                        same = Math.abs(a - b) <= 1.0e-8 * scale;
                    }
                    if (!same) {
                        throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) input=anchor idx=" + i + " lhs=" + a + " rhs=" + b);
                    }
                }
            } catch (RuntimeException t) {
                String cn = t.getClass().getName();
                if (cn.startsWith("java.lang.RuntimeException") && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
                // If either side rejects/throws, the relation does not apply for this input.
            } catch (Exception t) {
                // The relation does not apply if either side throws.
            }
        }

        // EXPLORE: generate many valid positive observation sets with the same root property:
        // real observations with increasing x and strictly positive y values, including
        // anchor-derived tails and synthetic Gaussian/tail shapes at moderate magnitudes.
        int variants = 1 + data.consumeInt(1, 4);
        for (int v = 0; v < variants; v++) {
            int mode = data.consumeInt(0, 2);
            double[] y;
            double xStart;
            double xStep;

            if (mode == 0) {
                int start = data.consumeInt(0, anchor.length - 3);
                int len = data.consumeInt(3, anchor.length - start);
                y = new double[len];
                double scale = Math.pow(10.0, data.consumeInt(-3, 3));
                for (int i = 0; i < len; i++) {
                    double val = anchor[start + i] * scale;
                    if (val <= 0.0 || Double.isNaN(val) || Double.isInfinite(val)) {
                        val = Math.abs(anchor[start + i]) + 1.0e-300;
                    }
                    y[i] = val;
                }
                xStart = data.consumeInt(-20, 20);
                xStep = data.consumeBoolean() ? 1.0 : data.consumeInt(1, 5);
            } else if (mode == 1) {
                int len = data.consumeInt(3, 40);
                y = new double[len];
                double amp = Math.pow(10.0, data.consumeInt(-6, 6));
                double sigma = data.consumeInt(1, 50);
                double center = data.consumeInt(-20, len + 20);
                for (int i = 0; i < len; i++) {
                    double d = i - center;
                    double val = amp * Math.exp(-(d * d) / (2.0 * sigma * sigma));
                    if (val <= 0.0 || Double.isNaN(val) || Double.isInfinite(val)) {
                        val = 1.0e-300;
                    }
                    y[i] = val;
                }
                xStart = data.consumeInt(-50, 50);
                xStep = data.consumeBoolean() ? 1.0 : data.consumeInt(1, 3);
            } else {
                int len = data.consumeInt(3, 35);
                y = new double[len];
                double scale = Math.pow(10.0, data.consumeInt(-10, 2));
                double growth = 1.05 + (data.consumeInt(0, 50) / 100.0);
                double cur = scale;
                for (int i = 0; i < len; i++) {
                    cur *= growth;
                    if (cur <= 0.0 || Double.isNaN(cur) || Double.isInfinite(cur)) {
                        cur = 1.0e-300 * (i + 1);
                    }
                    y[i] = cur;
                }
                xStart = data.consumeInt(-30, 30);
                xStep = data.consumeBoolean() ? 1.0 : data.consumeInt(1, 4);
            }

            GaussianFitter fitNoGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter fitWithGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < y.length; i++) {
                double x = xStart + i * xStep;
                if (Double.isNaN(x) || Double.isInfinite(x)) {
                    x = i;
                }
                double obs = y[i];
                if (!(obs > 0.0) || Double.isNaN(obs) || Double.isInfinite(obs)) {
                    obs = 1.0e-300;
                }
                fitNoGuess.addObservedPoint(x, obs);
                fitWithGuess.addObservedPoint(x, obs);
            }

            try {
                fitNoGuess.fit();
            } catch (RuntimeException t) {
                boolean inFit = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        inFit = true;
                        break;
                    }
                }
                String cn = t.getClass().getName();
                boolean rootCause = cn.endsWith("NotStrictlyPositiveException");
                boolean cleanRejection =
                        t instanceof IllegalArgumentException
                        || cn.contains(".exception.")
                        || cn.contains("NumberFormat");
                if (rootCause && inFit) {
                    throw t;
                }
                if (cleanRejection) {
                    continue;
                }
                continue;
            }

            try {
                double[] lhs = fitNoGuess.fit();
                double[] guess = (new GaussianFitter.ParameterGuesser(fitWithGuess.getObservations())).guess();
                double[] rhs = fitWithGuess.fit(guess);
                if (lhs == null || rhs == null || lhs.length != rhs.length) {
                    throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() vs fit(guess()) length/null mismatch variant=" + v);
                }
                for (int i = 0; i < lhs.length; i++) {
                    double a = lhs[i];
                    double b = rhs[i];
                    boolean same;
                    if (Double.isNaN(a) && Double.isNaN(b)) {
                        same = true;
                    } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                        same = a == b;
                    } else {
                        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                        same = Math.abs(a - b) <= 1.0e-8 * scale;
                    }
                    if (!same) {
                        throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) variant=" + v + " idx=" + i + " lhs=" + a + " rhs=" + b + " len=" + y.length);
                    }
                }
            } catch (RuntimeException t) {
                String cn = t.getClass().getName();
                if (cn.startsWith("java.lang.RuntimeException") && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw t;
                }
                // If either side rejects/throws, the relation does not apply for this input.
            } catch (Exception t) {
                // The relation does not apply if either side throws.
            }
        }
    }
}