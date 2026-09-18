package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_DATA = new double[] {
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
        runScenario(ANCHOR_DATA, 0.0, 1.0, true, true);

        int start = data.consumeInt(0, ANCHOR_DATA.length - 3);
        int len = data.consumeInt(3, ANCHOR_DATA.length - start);
        double[] variant = new double[len];

        double scalePow = data.consumeInt(-6, 6);
        double scale = Math.pow(10.0, scalePow);
        double tilt = 1.0 + (data.consumeInt(-5, 5) * 0.005);
        if (!(tilt > 0.0) || Double.isNaN(tilt) || Double.isInfinite(tilt)) {
            tilt = 1.0;
        }

        double previous = 0.0;
        for (int i = 0; i < len; i++) {
            double base = ANCHOR_DATA[start + i] * scale;
            double adjusted = base * Math.pow(tilt, i);
            if (!(adjusted > 0.0) || Double.isNaN(adjusted) || Double.isInfinite(adjusted)) {
                adjusted = Math.abs(base);
                if (!(adjusted > 0.0) || Double.isNaN(adjusted) || Double.isInfinite(adjusted)) {
                    adjusted = Math.pow(10.0, -12 + i);
                }
            }
            if (i > 0 && adjusted <= previous) {
                adjusted = Math.nextUp(previous);
            }
            variant[i] = adjusted;
            previous = adjusted;
        }

        double xOffset = data.consumeInt(-1000, 1000);
        double xStep = data.consumeInt(1, 10);
        runScenario(variant, xOffset, xStep, true, true);

        int synthLen = data.consumeInt(3, 40);
        double[] synthetic = new double[synthLen];
        double mean = synthLen + data.consumeInt(1, 40);
        double sigma = data.consumeInt(1, 50) / 10.0;
        double norm = Math.pow(10.0, data.consumeInt(-8, 4));
        previous = 0.0;
        for (int i = 0; i < synthLen; i++) {
            double x = i;
            double exponent = -((x - mean) * (x - mean)) / (2.0 * sigma * sigma);
            double y = norm * Math.exp(exponent);
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = Math.pow(10.0, -12 + Math.min(i, 12));
            }
            if (i > 0 && y <= previous) {
                y = Math.nextUp(previous);
            }
            synthetic[i] = y;
            previous = y;
        }
        runScenario(synthetic, data.consumeInt(-1000, 1000), data.consumeInt(1, 5), true, true);
    }

    private static void runScenario(double[] y, double xOffset, double xStep, boolean doOracle, boolean validByConstruction) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(xOffset + (i * xStep), y[i]);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t) || t instanceof RuntimeException || t instanceof Error) {
                return;
            }
            return;
        }

        if (!doOracle) {
            return;
        }

        /* Contract/oracle:
         * GaussianFitter.fit() computes a guess from the observations and then fits using that guess.
         * Therefore fit() and fit(guessFromSameObservations) must agree on the same data.
         * A patch that merely suppresses/circumvents the failing path in fit() but changes behavior
         * will violate this sibling-overload agreement.
         */
        GaussianFitter f1 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter f2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            double x = xOffset + (i * xStep);
            f1.addObservedPoint(x, y[i]);
            f2.addObservedPoint(x, y[i]);
        }

        double[] lhs;
        double[] rhs;
        try {
            lhs = f1.fit();
        } catch (Throwable t) {
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(f2.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        try {
            rhs = f2.fit(guess);
        } catch (Throwable t) {
            return;
        }

        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) input="
                    + Arrays.toString(y)
                    + " xOffset=" + xOffset
                    + " xStep=" + xStep
                    + " lhs=" + Arrays.toString(lhs)
                    + " rhs=" + Arrays.toString(rhs));
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof org.apache.commons.math.exception.NotStrictlyPositiveException)) {
            return false;
        }
        return hasFitFrame(t);
    }

    private static boolean hasFitFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(ste.getClassName())
                    && "fit".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean sameArray(double[] a, double[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            if (Double.isInfinite(x) || Double.isInfinite(y)) {
                if (x != y) {
                    return false;
                }
                continue;
            }
            double tol = 1.0e-8 * Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > tol) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}