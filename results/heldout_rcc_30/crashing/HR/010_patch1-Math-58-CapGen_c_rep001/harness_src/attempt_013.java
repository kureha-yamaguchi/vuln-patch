package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR = new double[] {
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

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int n = data.consumeInt(8, 48);
        double amplitude = positive(data.consumeInt(-5000, 5000), 1.0, 5001.0);
        double mean = data.consumeInt(-200, 200) / 4.0;
        double sigma = positive(data.consumeInt(-200, 200), 0.25, 50.25);
        double step = positive(data.consumeInt(-20, 20), 0.25, 5.25);
        double pivot = data.consumeInt(-200, 200) / 4.0;
        double x0 = pivot - step * (n - 1) / 2.0;

        if (Math.abs(x0) > 1_000_000 || Math.abs(pivot) > 1_000_000 || Math.abs(mean) > 1_000_000) {
            return;
        }

        double[] ys = new double[n];
        Gaussian.Parametric gp = new Gaussian.Parametric();
        double[] params = new double[] { amplitude, mean, sigma };
        for (int i = 0; i < n; i++) {
            double x = x0 + i * step;
            try {
                ys[i] = gp.value(x, params);
            } catch (RuntimeException e) {
                return;
            }
        }

        try {
            double[] fitA = fitFromSeries(x0, step, ys);
            double[] fitB = fitMirroredSeries(x0, step, ys, pivot);

            if (fitA == null || fitB == null || fitA.length < 3 || fitB.length < 3) {
                return;
            }

            /*
             * Metamorphic guarantee: reflecting every x-coordinate around pivot c maps a Gaussian
             * (norm, mean, sigma) to (same norm, 2c-mean, same sigma). Both sides are computed
             * by real library calls through GaussianFitter.fit(), so a patch that only suppresses
             * the throw but leaves the fitted parameters inconsistent still violates this relation.
             */
            double expectedMirroredMean = 2.0 * pivot - fitA[1];
            assertClose("mirror-norm", fitA[0], fitB[0], relTol(fitA[0], fitB[0], 0.20) + 1e-6);
            assertClose("mirror-sigma", Math.abs(fitA[2]), Math.abs(fitB[2]),
                    relTol(Math.abs(fitA[2]), Math.abs(fitB[2]), 0.20) + 1e-6);
            assertClose("mirror-mean", expectedMirroredMean, fitB[1],
                    relTol(expectedMirroredMean, fitB[1], 0.05) + 1e-6);

            GaussianFitter original = buildFitter(x0, step, ys);
            GaussianFitter mirrored = buildMirroredFitter(x0, step, ys, pivot);

            /*
             * Consistency cross-check on a reachable helper: ParameterGuesser.guess() should be
             * equivariant under the same x-reflection because it derives an initial Gaussian guess
             * from the same observations. This checks a related helper quantity independently of
             * the final optimized result.
             */
            double[] guessA = new GaussianFitter.ParameterGuesser(original.getObservations()).guess();
            double[] guessB = new GaussianFitter.ParameterGuesser(mirrored.getObservations()).guess();
            if (guessA.length >= 3 && guessB.length >= 3) {
                assertClose("guess-mirror-norm", guessA[0], guessB[0],
                        relTol(guessA[0], guessB[0], 0.25) + 1e-6);
                assertClose("guess-mirror-sigma", Math.abs(guessA[2]), Math.abs(guessB[2]),
                        relTol(Math.abs(guessA[2]), Math.abs(guessB[2]), 0.25) + 1e-6);
                assertClose("guess-mirror-mean", 2.0 * pivot - guessA[1], guessB[1],
                        relTol(2.0 * pivot - guessA[1], guessB[1], 0.08) + 1e-6);
            }
        } catch (RuntimeException t) {
            if (isOracleFailure(t) || isRootCause(t)) {
                throw t;
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runAnchor() {
        try {
            fitFromSeries(0.0, 1.0, ANCHOR);
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static GaussianFitter buildFitter(double x0, double step, double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(x0 + i * step, ys[i]);
        }
        return fitter;
    }

    private static GaussianFitter buildMirroredFitter(double x0, double step, double[] ys, double pivot) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double x = x0 + i * step;
            fitter.addObservedPoint(2.0 * pivot - x, ys[i]);
        }
        return fitter;
    }

    private static double[] fitFromSeries(double x0, double step, double[] ys) {
        GaussianFitter fitter = buildFitter(x0, step, ys);
        return runFit(fitter);
    }

    private static double[] fitMirroredSeries(double x0, double step, double[] ys, double pivot) {
        GaussianFitter fitter = buildMirroredFitter(x0, step, ys, pivot);
        return runFit(fitter);
    }

    private static double[] runFit(GaussianFitter fitter) {
        try {
            return fitter.fit();
        } catch (RuntimeException t) {
            if (isCleanRejection(t)) {
                return null;
            }
            throw t;
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException || t instanceof MathIllegalArgumentException) {
            return !isRootCause(t);
        }
        String cn = t.getClass().getName();
        return cn.startsWith("org.apache.commons.math.exception.") && !isRootCause(t);
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String c = ste.getClassName();
            String m = ste.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(c) && ("fit".equals(m)))
                    || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(c) && ("getObservations".equals(m) || "fit".equals(m)))
                    || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(c) && "guess".equals(m))
                    || ("org.apache.commons.math.optimization.fitting.GaussianFitter$Parametric".equals(c) && "<init>".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static void assertClose(String tag, double a, double b, double tol) {
        if (Double.isNaN(a) || Double.isNaN(b) || Double.isInfinite(a) || Double.isInfinite(b) || Math.abs(a - b) > tol) {
            throw new RuntimeException("[oracle:" + tag + "] metamorphic violation: lhs=" + a + " rhs=" + b + " tol=" + tol);
        }
    }

    private static double relTol(double a, double b, double frac) {
        double scale = Math.max(Math.abs(a), Math.abs(b));
        return Math.max(1.0, scale) * frac;
    }

    private static double positive(int raw, double min, double span) {
        return min + (Math.abs((double) raw) % span);
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}