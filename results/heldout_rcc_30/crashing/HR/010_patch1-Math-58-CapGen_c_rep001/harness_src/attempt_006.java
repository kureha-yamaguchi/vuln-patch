package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
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

        try {
            org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer optimizer1 =
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer();
            GaussianFitter stepwise = new GaussianFitter(optimizer1);
            for (int i = 0; i < anchor.length; i++) {
                stepwise.addObservedPoint(i, anchor[i]);
            }

            double[] guessed = null;
            boolean fitWithGuessSucceeded = false;
            double[] fitWithGuess = null;
            try {
                guessed = new GaussianFitter.ParameterGuesser(stepwise.getObservations()).guess();
                fitWithGuess = stepwise.fit(guessed.clone());
                fitWithGuessSucceeded = true;
            } catch (IllegalArgumentException e) {
            } catch (RuntimeException e) {
            }

            org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer optimizer2 =
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer();
            GaussianFitter direct = new GaussianFitter(optimizer2);
            for (int i = 0; i < anchor.length; i++) {
                direct.addObservedPoint(i, anchor[i]);
            }

            try {
                double[] directFit = direct.fit();

                if (fitWithGuessSucceeded && fitWithGuess != null && directFit != null &&
                    fitWithGuess.length == directFit.length && directFit.length == 3) {
                    double maxRel = 0.0;
                    for (int i = 0; i < 3; i++) {
                        double a = fitWithGuess[i];
                        double b = directFit[i];
                        double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                        double rel = Math.abs(a - b) / scale;
                        if (rel > maxRel) {
                            maxRel = rel;
                        }
                    }
                    if (maxRel > 1.0e-6) {
                        throw new RuntimeException(
                            "[oracle:anchor-delegation] fit() and stepwise guess+fit(double[]) disagree on the same valid anchor observations: rel=" +
                            maxRel + " direct=" + directFit[0] + "," + directFit[1] + "," + directFit[2] +
                            " stepwise=" + fitWithGuess[0] + "," + fitWithGuess[1] + "," + fitWithGuess[2]);
                    }
                }
            } catch (Throwable t) {
                boolean inRegion = false;
                for (StackTraceElement ste : t.getStackTrace()) {
                    String cn = ste.getClassName();
                    String mn = ste.getMethodName();
                    if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cn) && "fit".equals(mn)) ||
                        ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cn) && "getObservations".equals(mn)) ||
                        ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cn) && "guess".equals(mn)) ||
                        ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(cn) && "validateParameters".equals(mn))) {
                        inRegion = true;
                        break;
                    }
                }

                if (fitWithGuessSucceeded && inRegion) {
                    throw new RuntimeException(
                        "[oracle:anchor-valid-input] fit() rejected anchor observations although ParameterGuesser.guess() and fit(double[]) succeeded on the same data",
                        t);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
        }

        try {
            int n = data.consumeInt(4, 20);
            double amplitude = 1.0 + data.consumeInt(0, 10000) / 10.0;
            double mean = data.consumeInt(-200, 200) / 10.0;
            double sigma = 0.5 + data.consumeInt(0, 200) / 20.0;
            double step = 0.2 + data.consumeInt(0, 100) / 25.0;
            double start = mean - step * (n / 2.0);

            double[] xs = new double[n];
            double[] ys = new double[n];
            for (int i = 0; i < n; i++) {
                double x = start + i * step;
                double z = (x - mean) / sigma;
                double base = amplitude * Math.exp(-0.5 * z * z);
                double noise = data.consumeInt(0, 1000) / 1000000.0;
                xs[i] = x;
                ys[i] = base * (1.0 + noise);
            }

            GaussianFitter original = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());
            GaussianFitter reversed = new GaussianFitter(
                new org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer());

            for (int i = 0; i < n; i++) {
                original.addObservedPoint(xs[i], ys[i]);
                int j = n - 1 - i;
                reversed.addObservedPoint(xs[j], ys[j]);
            }

            try {
                WeightedObservedPoint[] obs1 = original.getObservations();
                WeightedObservedPoint[] obs2 = reversed.getObservations();
                double[] g1 = new GaussianFitter.ParameterGuesser(obs1).guess();
                double[] g2 = new GaussianFitter.ParameterGuesser(obs2).guess();

                if (g1.length == 3 && g2.length == 3) {
                    double maxRelGuess = 0.0;
                    for (int i = 0; i < 3; i++) {
                        double scale = Math.max(1.0, Math.max(Math.abs(g1[i]), Math.abs(g2[i])));
                        double rel = Math.abs(g1[i] - g2[i]) / scale;
                        if (rel > maxRelGuess) {
                            maxRelGuess = rel;
                        }
                    }
                    if (maxRelGuess > 1.0e-12) {
                        throw new RuntimeException(
                            "[oracle:guess-permutation] ParameterGuesser.guess() changed under pure observation reordering: rel=" +
                            maxRelGuess + " g1=" + g1[0] + "," + g1[1] + "," + g1[2] +
                            " g2=" + g2[0] + "," + g2[1] + "," + g2[2]);
                    }
                }
            } catch (IllegalArgumentException e) {
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                    throw e;
                }
            }

            try {
                double[] p1 = original.fit();
                double[] p2 = reversed.fit();

                if (p1 != null && p2 != null && p1.length == 3 && p2.length == 3) {
                    double maxRelFit = 0.0;
                    for (int i = 0; i < 3; i++) {
                        double scale = Math.max(1.0, Math.max(Math.abs(p1[i]), Math.abs(p2[i])));
                        double rel = Math.abs(p1[i] - p2[i]) / scale;
                        if (rel > maxRelFit) {
                            maxRelFit = rel;
                        }
                    }
                    if (maxRelFit > 1.0e-4) {
                        throw new RuntimeException(
                            "[oracle:fit-permutation] GaussianFitter.fit() changed under pure observation reordering: rel=" +
                            maxRelFit + " p1=" + p1[0] + "," + p1[1] + "," + p1[2] +
                            " p2=" + p2[0] + "," + p2[1] + "," + p2[2]);
                    }
                }
            } catch (IllegalArgumentException e) {
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("[oracle:")) {
                    throw e;
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
        }
    }
}