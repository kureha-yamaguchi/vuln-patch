package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        class Utils {
            boolean stackHasGaussianFitterFit(Throwable t) {
                for (StackTraceElement ste : t.getStackTrace()) {
                    if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                            && "fit".equals(ste.getMethodName())) {
                        return true;
                    }
                }
                return false;
            }

            boolean isMathValidation(Throwable t) {
                if (t == null) {
                    return false;
                }
                if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
                    return true;
                }
                Package p = t.getClass().getPackage();
                String pkg = p == null ? "" : p.getName();
                return pkg.startsWith("org.apache.commons.math.exception");
            }

            boolean isGroundTruth(Throwable t) {
                return t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException;
            }

            void maybePropagate(Throwable t, boolean validByConstruction) {
                if (t == null) {
                    return;
                }
                if (isGroundTruth(t) && stackHasGaussianFitterFit(t) && validByConstruction) {
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    throw new RuntimeException(t);
                }
            }

            GaussianFitter buildFitter(double[] ys, double startX, double step) {
                GaussianFitter fitter =
                        new GaussianFitter(new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());
                for (int i = 0; i < ys.length; i++) {
                    fitter.addObservedPoint(startX + i * step, ys[i]);
                }
                return fitter;
            }

            void assertOverloadAgreement(GaussianFitter fitter, String inputDesc) {
                /*
                 * Contract/oracle: fit() and fit(double[] initialGuess) are same-name overloads whose
                 * documented behavior agrees when initialGuess is the guess that fit() itself derives.
                 * A correct implementation of fit() is therefore equivalent to:
                 *   guess = new ParameterGuesser(getObservations()).guess();
                 *   fit(guess)
                 * The patch changes exactly that delegation. If a patch merely deletes the throw or
                 * changes control flow to avoid the crashing branch while returning a different result,
                 * this overload-agreement oracle detects it.
                 *
                 * Hygiene: if either side throws, this relation does not apply for that input and we skip.
                 */
                final double[] guess;
                try {
                    guess = (new GaussianFitter.ParameterGuesser(fitter.getObservations())).guess();
                } catch (RuntimeException e) {
                    if (!isMathValidation(e)) {
                        maybePropagate(e, false);
                    }
                    return;
                }

                final double[] lhs;
                try {
                    lhs = fitter.fit();
                } catch (RuntimeException e) {
                    if (isMathValidation(e)) {
                        maybePropagate(e, true);
                    }
                    return;
                }

                final double[] rhs;
                try {
                    rhs = fitter.fit(guess);
                } catch (RuntimeException e) {
                    if (isMathValidation(e)) {
                        maybePropagate(e, true);
                    }
                    return;
                }

                if (lhs == null || rhs == null || lhs.length != rhs.length) {
                    throw new RuntimeException(
                            "[oracle:fit-overload] metamorphic violation: fit() and fit(guess) returned different shapes input="
                                    + inputDesc
                                    + " lhsLen=" + (lhs == null ? -1 : lhs.length)
                                    + " rhsLen=" + (rhs == null ? -1 : rhs.length));
                }

                for (int i = 0; i < lhs.length; i++) {
                    double a = lhs[i];
                    double b = rhs[i];
                    boolean sameNaN = Double.isNaN(a) && Double.isNaN(b);
                    boolean sameInf = Double.isInfinite(a) || Double.isInfinite(b);
                    if (sameNaN) {
                        continue;
                    }
                    if (sameInf) {
                        if (a != b) {
                            throw new RuntimeException(
                                    "[oracle:fit-overload] metamorphic violation: fit() and fit(guess) disagree input="
                                            + inputDesc + " idx=" + i + " lhs=" + a + " rhs=" + b);
                        }
                        continue;
                    }
                    double tol = 1.0e-10 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                    if (Math.abs(a - b) > tol) {
                        throw new RuntimeException(
                                "[oracle:fit-overload] metamorphic violation: fit() and fit(guess) differ input="
                                        + inputDesc + " idx=" + i + " lhs=" + a + " rhs=" + b + " tol=" + tol);
                    }
                }
            }
        }

        Utils u = new Utils();

        // ANCHOR: exact regression input from GaussianFitterTest.testMath519.
        {
            double[] anchorY = new double[] {
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
            GaussianFitter anchorFitter = u.buildFitter(anchorY, 0.0, 1.0);
            try {
                anchorFitter.fit();
            } catch (RuntimeException e) {
                if (u.isMathValidation(e)) {
                    u.maybePropagate(e, true);
                    return;
                }
                return;
            }
            u.assertOverloadAgreement(anchorFitter, "anchor");
        }

        // EXPLORE: valid-by-construction positive bell-shaped datasets with varying length/position/scale.
        int len = data.consumeInt(3, 40);
        int centerTicks = data.consumeInt(-20, len * 20 + 20);
        int sigmaTicks = data.consumeInt(5, 200);
        int ampTicks = data.consumeInt(1, 10000);
        int baseTicks = data.consumeInt(0, 1000);
        int stepTicks = data.consumeInt(1, 30);
        int skewTicks = data.consumeInt(-200, 200);
        int rippleTicks = data.consumeInt(0, 500);

        double center = centerTicks / 10.0;
        double sigma = sigmaTicks / 10.0;
        double amplitude = ampTicks / 10.0;
        double baseline = baseTicks / 1000.0;
        double step = stepTicks / 10.0;
        double skew = skewTicks / 1000.0;
        double ripple = rippleTicks / 10000.0;

        double[] ys = new double[len];
        for (int i = 0; i < len; i++) {
            double x = i * step;
            double dx = x - center;
            double gaussian = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma));
            double asymmetric = 1.0 + skew * dx;
            if (asymmetric < 0.1) {
                asymmetric = 0.1;
            }
            double wave = 1.0 + ripple * Math.cos(i);
            if (wave < 0.1) {
                wave = 0.1;
            }
            double y = baseline + gaussian * asymmetric * wave;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = baseline + Math.abs(gaussian) + 1.0e-9;
            }
            ys[i] = y;
        }

        GaussianFitter fuzzFitter = u.buildFitter(ys, 0.0, step);
        try {
            fuzzFitter.fit();
        } catch (RuntimeException e) {
            if (u.isMathValidation(e)) {
                u.maybePropagate(e, true);
                return;
            }
            return;
        }

        u.assertOverloadAgreement(
                fuzzFitter,
                "len=" + len
                        + ",center=" + center
                        + ",sigma=" + sigma
                        + ",amp=" + amplitude
                        + ",base=" + baseline
                        + ",step=" + step
                        + ",skew=" + skew
                        + ",ripple=" + ripple);
    }
}