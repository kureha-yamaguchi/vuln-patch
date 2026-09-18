package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        {
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

            GaussianFitter anchorFitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < anchor.length; i++) {
                anchorFitter.addObservedPoint(i, anchor[i]);
            }

            try {
                anchorFitter.fit();
            } catch (Throwable t) {
                boolean throughPatchedFit = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                            && "fit".equals(e.getMethodName())) {
                        throughPatchedFit = true;
                        break;
                    }
                }
                if (t instanceof NotStrictlyPositiveException && throughPatchedFit) {
                    throw (RuntimeException) t;
                }
                if (t instanceof IllegalArgumentException || t instanceof RuntimeException) {
                    return;
                }
                return;
            }
        }

        int cases = 1;
        if (data.remainingBytes() > 16) {
            cases += data.consumeInt(0, 2);
        }

        for (int c = 0; c < cases; c++) {
            int len = data.consumeInt(3, 40);
            double amplitude = Math.pow(10.0, data.consumeInt(-20, 3));
            if (!(amplitude > 0.0) || Double.isInfinite(amplitude) || Double.isNaN(amplitude)) {
                amplitude = 1.0;
            }

            double sigma = data.consumeInt(1, 200) / 10.0;
            double center = len - 1 + data.consumeInt(1, len + 20);

            double[] ys = new double[len];
            for (int i = 0; i < len; i++) {
                double dx = i - center;
                double base = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
                double tweak = 1.0 + (data.consumeInt(-30, 30) / 100.0);
                if (tweak <= 0.0) {
                    tweak = 0.1;
                }
                double y = base * tweak;
                if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                    y = Math.max(base, Double.MIN_NORMAL);
                }
                ys[i] = y;
            }

            GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
            GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < len; i++) {
                fitterA.addObservedPoint(i, ys[i]);
                fitterB.addObservedPoint(i, ys[i]);
            }

            double[] lhs;
            try {
                lhs = fitterA.fit();
            } catch (Throwable t) {
                boolean throughPatchedFit = false;
                for (StackTraceElement e : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                            && "fit".equals(e.getMethodName())) {
                        throughPatchedFit = true;
                        break;
                    }
                }
                if (t instanceof NotStrictlyPositiveException && throughPatchedFit) {
                    throw (RuntimeException) t;
                }
                continue;
            }

            double[] guess;
            try {
                guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
            } catch (Throwable t) {
                continue;
            }

            double[] rhs;
            try {
                rhs = fitterB.fit(guess);
            } catch (Throwable t) {
                continue;
            }

            /* Contract/oracle: GaussianFitter.fit() is documented as the no-arg sibling of fit(double[]),
               and the patched body itself computes ParameterGuesser(...).guess() then delegates to fit(guess).
               Therefore, for the same observations, fit() must produce the same result as fit(guess) built from
               those observations. A throw-deleting or branch-skipping patch that changes the delegation path
               can violate this observable equivalence without crashing. */
            if (lhs != null && rhs != null && lhs.length == rhs.length) {
                for (int i = 0; i < lhs.length; i++) {
                    long a = Double.doubleToLongBits(lhs[i]);
                    long b = Double.doubleToLongBits(rhs[i]);
                    if (a != b) {
                        throw new RuntimeException(
                            "[oracle:fit-overload] metamorphic violation: fit() != fit(guess) input=len=" + len
                                + ", amp=" + amplitude + ", sigma=" + sigma + ", center=" + center
                                + " lhs[" + i + "]=" + lhs[i] + " rhs[" + i + "]=" + rhs[i]);
                    }
                }
            }
        }
    }
}