package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

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

        for (int scenario = 0; scenario < 2; scenario++) {
            double[] ys;
            if (scenario == 0) {
                ys = anchor;
            } else {
                int n = data.consumeInt(3, 40);
                ys = new double[n];
                double amplitude = Math.pow(10.0, data.consumeInt(-6, 6));
                double sigma = 0.5 + (data.consumeInt(0, 400) / 20.0);
                double center = data.consumeInt(-20, n + 20);
                boolean addBaseline = data.consumeBoolean();
                double baseline = addBaseline ? Math.pow(10.0, data.consumeInt(-12, -3)) : 0.0;
                for (int i = 0; i < n; i++) {
                    double dx = i - center;
                    double y = amplitude * Math.exp(-(dx * dx) / (2.0 * sigma * sigma)) + baseline;
                    int tweak = data.consumeInt(0, 1000);
                    y *= 1.0 + (tweak / 1000000.0);
                    if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                        y = baseline > 0.0 ? baseline : 1e-12;
                    }
                    ys[i] = y;
                }
            }

            GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                fitter.addObservedPoint((double) i, ys[i]);
            }

            WeightedObservedPoint[] beforeObs = fitter.getObservations();
            double[] guessBefore = null;
            boolean beforeGuessOk = false;
            try {
                guessBefore = (new GaussianFitter.ParameterGuesser(beforeObs)).guess();
                beforeGuessOk = true;
            } catch (RuntimeException e) {
                if (!(e instanceof IllegalArgumentException) && !(e instanceof MathIllegalArgumentException)) {
                    if (scenario == 0) {
                        boolean root = false;
                        StackTraceElement[] st = e.getStackTrace();
                        for (int i = 0; i < st.length; i++) {
                            String cn = st[i].getClassName();
                            String mn = st[i].getMethodName();
                            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cn) && "fit".equals(mn)) ||
                                ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cn) && ("fit".equals(mn) || "getObservations".equals(mn))) ||
                                ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cn) && "guess".equals(mn))) {
                                root = true;
                                break;
                            }
                        }
                        if (root && e instanceof NotStrictlyPositiveException) {
                            throw e;
                        }
                    }
                }
            }

            double[] fitResult = null;
            boolean fitOk = false;
            try {
                fitResult = fitter.fit();
                fitOk = true;
            } catch (RuntimeException e) {
                boolean root = false;
                StackTraceElement[] st = e.getStackTrace();
                for (int i = 0; i < st.length; i++) {
                    String cn = st[i].getClassName();
                    String mn = st[i].getMethodName();
                    if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cn) && "fit".equals(mn)) ||
                        ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cn) && ("fit".equals(mn) || "getObservations".equals(mn))) ||
                        ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cn) && "guess".equals(mn))) {
                        root = true;
                        break;
                    }
                }
                if (scenario == 0 && root && e instanceof NotStrictlyPositiveException) {
                    throw e;
                }
                if (!(e instanceof IllegalArgumentException) && !(e instanceof MathIllegalArgumentException)) {
                    return;
                }
            }

            if (fitOk) {
                WeightedObservedPoint[] afterObs;
                double[] guessAfter;
                try {
                    afterObs = fitter.getObservations();
                    guessAfter = (new GaussianFitter.ParameterGuesser(afterObs)).guess();
                } catch (RuntimeException e) {
                    return;
                }

                if (beforeGuessOk && guessBefore != null && guessAfter != null &&
                    guessBefore.length == guessAfter.length && fitResult != null && fitResult.length == 3) {
                    double maxAbs = 0.0;
                    for (int i = 0; i < guessBefore.length; i++) {
                        double d = Math.abs(guessBefore[i] - guessAfter[i]);
                        if (d > maxAbs) {
                            maxAbs = d;
                        }
                    }
                    if (maxAbs > 1e-12) {
                        throw new RuntimeException("[oracle:guess-postfit] metamorphic violation: fit() is read-only over observations, so ParameterGuesser on getObservations() must be stable before/after fit; diff=" + maxAbs + " before=" + java.util.Arrays.toString(guessBefore) + " after=" + java.util.Arrays.toString(guessAfter));
                    }

                    if (beforeObs.length != afterObs.length) {
                        throw new RuntimeException("[oracle:guess-postfit] metamorphic violation: fit() changed observation count from " + beforeObs.length + " to " + afterObs.length);
                    }
                    for (int i = 0; i < beforeObs.length; i++) {
                        if (beforeObs[i].getWeight() != afterObs[i].getWeight() ||
                            beforeObs[i].getX() != afterObs[i].getX() ||
                            beforeObs[i].getY() != afterObs[i].getY()) {
                            throw new RuntimeException("[oracle:guess-postfit] metamorphic violation: fit() changed observation[" + i + "] from (" + beforeObs[i].getWeight() + "," + beforeObs[i].getX() + "," + beforeObs[i].getY() + ") to (" + afterObs[i].getWeight() + "," + afterObs[i].getX() + "," + afterObs[i].getY() + ")");
                        }
                    }
                }

                if (scenario == 0 && fitResult != null && fitResult.length == 3) {
                    if (Math.abs(fitResult[1] - 53.1572792) > 1e-6) {
                        throw new RuntimeException("[oracle:anchor-mean] metamorphic violation: anchor fit mean drifted; got=" + fitResult[1]);
                    }
                }
            }
        }
    }
}