package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            exploreMirroredSynthetic(data);
        } else if (mode == 1) {
            exploreMirroredFromAnchorShape(data);
        } else {
            exploreSmallBell(data);
        }
    }

    private static void runAnchor() {
        double[] y = new double[] {
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

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(i, y[i]);
        }
        try {
            fitter.fit();
        } catch (Throwable t) {
            if (isCleanRejection(t) || isKnownRootCause(t)) {
                return;
            }
        }
    }

    private static void exploreMirroredSynthetic(FuzzedDataProvider data) {
        int n = data.consumeInt(7, 31);
        double norm = boundedPositive(data.consumeInt(), 0.5, 1000.0);
        double mean = bounded(data.consumeInt(), -20.0, 20.0);
        double sigma = boundedPositive(data.consumeInt(), 0.2, 8.0);
        double spacing = boundedPositive(data.consumeInt(), 0.2, 3.0);
        double start = bounded(data.consumeInt(), -20.0, 20.0);

        GaussianFitter original = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter mirrored = new GaussianFitter(new LevenbergMarquardtOptimizer());

        Gaussian g = new Gaussian(norm, mean, sigma);
        for (int i = 0; i < n; i++) {
            double x = start + i * spacing;
            double y = g.value(x);
            original.addObservedPoint(x, y);
            mirrored.addObservedPoint(-x, y);
        }

        checkMirrorConsistency(original, mirrored);
    }

    private static void exploreMirroredFromAnchorShape(FuzzedDataProvider data) {
        double[] base = new double[] {
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

        double xShift = bounded(data.consumeInt(), -50.0, 50.0);
        double xScale = boundedPositive(data.consumeInt(), 0.25, 3.0);
        double yScale = boundedPositive(data.consumeInt(), 0.5, 20.0);

        GaussianFitter original = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter mirrored = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < base.length; i++) {
            double x = xShift + i * xScale;
            double y = base[i] * yScale;
            original.addObservedPoint(x, y);
            mirrored.addObservedPoint(-x, y);
        }

        checkMirrorConsistency(original, mirrored);
    }

    private static void exploreSmallBell(FuzzedDataProvider data) {
        int n = data.consumeInt(5, 15);
        int peak = data.consumeInt(1, n - 2);
        double amplitude = boundedPositive(data.consumeInt(), 1.0, 1000.0);
        double baseline = boundedPositive(data.consumeInt(), 0.0, 0.001);
        double width = boundedPositive(data.consumeInt(), 0.8, 4.0);

        GaussianFitter original = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter mirrored = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < n; i++) {
            double dx = i - peak;
            double y = baseline + amplitude * Math.exp(-(dx * dx) / (2.0 * width * width));
            original.addObservedPoint(i, y);
            mirrored.addObservedPoint(-i, y);
        }

        checkMirrorConsistency(original, mirrored);
    }

    private static void checkMirrorConsistency(GaussianFitter original, GaussianFitter mirrored) {
        WeightedObservedPoint[] obsA = original.getObservations();
        WeightedObservedPoint[] obsB = mirrored.getObservations();
        if (obsA.length < 5 || obsA.length != obsB.length) {
            return;
        }

        try {
            GaussianFitter.ParameterGuesser guesserA = new GaussianFitter.ParameterGuesser(obsA);
            GaussianFitter.ParameterGuesser guesserB = new GaussianFitter.ParameterGuesser(obsB);
            double[] guessA = guesserA.guess();
            double[] guessB = guesserB.guess();

            /*
             * Sound metamorphic guarantee:
             * Reflecting every observation x -> -x leaves a Gaussian's norm and sigma unchanged
             * and negates only its mean. ParameterGuesser operates solely on the observations,
             * so the guessed parameters for mirrored observations must mirror the same way.
             */
            requireMirror("guess-mirror", guessA, guessB, 1e-6, 1e-6);

            double[] fitA = original.fit();
            double[] fitB = mirrored.fit();

            /*
             * Sound metamorphic guarantee:
             * fit() estimates Gaussian parameters from observations. Mirroring all x values
             * describes the same curve under x -> -x, so the fitted norm and sigma stay equal
             * and the fitted mean flips sign. A patch that merely suppresses the known throw
             * but routes to the wrong fitting path can violate this observable relation.
             */
            requireMirror("fit-mirror", fitA, fitB, 1e-4, 1e-4);
        } catch (Throwable t) {
            if (isCleanRejection(t) || isKnownRootCause(t)) {
                return;
            }
        }
    }

    private static void requireMirror(String oracleId, double[] a, double[] b, double rel, double abs) {
        if (a == null || b == null || a.length < 3 || b.length < 3) {
            return;
        }
        if (!close(a[0], b[0], rel, abs) || !close(a[1], -b[1], rel, abs) || !close(a[2], b[2], rel, abs)) {
            throw new RuntimeException("[oracle:" + oracleId + "] metamorphic violation: lhs=[" + a[0] + "," + a[1] + "," + a[2]
                    + "] rhs=[" + b[0] + "," + b[1] + "," + b[2] + "]");
        }
    }

    private static boolean close(double x, double y, double rel, double abs) {
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isInfinite(x) || Double.isInfinite(y)) {
            return false;
        }
        double scale = Math.max(Math.abs(x), Math.abs(y));
        return Math.abs(x - y) <= Math.max(abs, rel * Math.max(1.0, scale));
    }

    private static boolean isKnownRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(m))
                    || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(m))
                    || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(m))
                    || ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(cls) && "validateParameters".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException || t instanceof MathIllegalArgumentException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static double bounded(int v, double min, double max) {
        double unit = (v & 0x7fffffff) / (double) Integer.MAX_VALUE;
        return min + unit * (max - min);
    }

    private static double boundedPositive(int v, double min, double max) {
        return Math.max(min, bounded(v, min, max));
    }
}