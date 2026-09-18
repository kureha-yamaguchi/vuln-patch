package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
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
        runCase(ANCHOR_DATA, true);

        int len = data.consumeInt(3, 40);
        double[] ys = new double[len];

        double amplitude = 1.0 + data.consumeInt(0, 10000) / 100.0;
        double sigma = 0.2 + data.consumeInt(0, 5000) / 1000.0;
        double center = (len - 1) + 0.5 + data.consumeInt(0, 4000) / 200.0;
        double floor = data.consumeBoolean() ? 0.0 : data.consumeInt(0, 100) * 1e-18;

        for (int i = 0; i < len; i++) {
            double dx = i - center;
            double exponent = -(dx * dx) / (2.0 * sigma * sigma);
            double y = floor + amplitude * Math.exp(exponent);
            if (data.consumeBoolean()) {
                y *= 1.0 + (data.consumeInt(0, 20) / 1000.0);
            }
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                y = 1e-300;
            }
            ys[i] = y;
        }

        if (!isStrictlyIncreasing(ys)) {
            for (int i = 1; i < ys.length; i++) {
                if (!(ys[i] > ys[i - 1])) {
                    ys[i] = Math.nextUp(ys[i - 1]);
                }
            }
        }

        runCase(ys, true);
    }

    private static void runCase(double[] ys, boolean validByConstruction) {
        GaussianFitter fitNoArg = buildFitter(ys);
        try {
            fitNoArg.fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCauseFromFit(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        /* Oracle: the documented same-name overloads fit() and fit(double[] initialGuess)
         * must agree on equivalent inputs. GaussianFitter.fit() computes a guess from the
         * observations and delegates to the overload taking that guess; a patch that merely
         * deletes the throw or changes behavior without preserving delegation will break this.
         * If either side throws, the relation does not apply and we skip it.
         */
        GaussianFitter fitWithGuess = buildFitter(ys);
        double[] guess;
        try {
            guess = new GaussianFitter.ParameterGuesser(fitWithGuess.getObservations()).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] lhs;
        double[] rhs;
        try {
            lhs = buildFitter(ys).fit();
        } catch (RuntimeException t) {
            if (validByConstruction && isRootCauseFromFit(t)) {
                throw t;
            }
            return;
        }
        try {
            rhs = fitWithGuess.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() must agree with fit(initialGuess) " +
                "inputLen=" + ys.length +
                " lhs=" + arrayToString(lhs) +
                " rhs=" + arrayToString(rhs)
            );
        }
    }

    private static GaussianFitter buildFitter(double[] ys) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(i, ys[i]);
        }
        return fitter;
    }

    private static boolean isRootCauseFromFit(RuntimeException t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(RuntimeException t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || t instanceof MathIllegalArgumentException;
    }

    private static boolean isStrictlyIncreasing(double[] ys) {
        for (int i = 1; i < ys.length; i++) {
            if (!(ys[i] > ys[i - 1])) {
                return false;
            }
        }
        return true;
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
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1e-8 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String arrayToString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}