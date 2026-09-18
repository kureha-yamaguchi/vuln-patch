package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int len = data.consumeInt(3, 40);
        double amplitude = positiveFromInt(data.consumeInt(1, 1_000_000)) / 10.0;
        double sigma = 0.5 + (positiveFromInt(data.consumeInt(1, 1_000_000)) % 1000) / 100.0;
        double offset = data.consumeInt(-1000, 1000);
        double centerBeyondRight = offset + (len - 1) + 1.0 + (positiveFromInt(data.consumeInt(1, 1_000_000)) % 3000) / 100.0;
        double baseline = (positiveFromInt(data.consumeInt(0, 100_000)) % 1000) / 1.0e12;

        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());

        for (int i = 0; i < len; i++) {
            double x = offset + i;
            double delta = x - centerBeyondRight;
            double y = amplitude * Math.exp(-(delta * delta) / (2.0 * sigma * sigma)) + baseline;
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return;
            }
            fitterA.addObservedPoint(x, y);
            fitterB.addObservedPoint(x, y);
        }

        double[] lhs;
        try {
            lhs = fitterA.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] rhs;
        try {
            rhs = fitterB.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        // Contract/oracle: the no-arg fit() is defined by computing ParameterGuesser(...).guess()
        // and then delegating to the sibling overload fit(double[] initialGuess). Therefore, whenever
        // both calls return normally on the same observations, they must agree. A patch that merely
        // suppresses the crashing path or changes behavior instead of delegating correctly will break this.
        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() must agree with fit(ParameterGuesser.guess()) "
                    + "input=len=" + len
                    + ",offset=" + offset
                    + ",center=" + centerBeyondRight
                    + ",sigma=" + sigma
                    + ",amplitude=" + amplitude
                    + ",lhs=" + arrayToString(lhs)
                    + ",rhs=" + arrayToString(rhs));
        }
    }

    private static void runAnchor() {
        final double[] anchorData = {
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

        GaussianFitter fitterA = new GaussianFitter(new LevenbergMarquardtOptimizer());
        GaussianFitter fitterB = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < anchorData.length; i++) {
            fitterA.addObservedPoint(i, anchorData[i]);
            fitterB.addObservedPoint(i, anchorData[i]);
        }

        double[] lhs;
        try {
            lhs = fitterA.fit();
        } catch (RuntimeException t) {
            if (isRootCause(t)) {
                throw t;
            }
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitterB.getObservations())).guess();
        } catch (RuntimeException t) {
            return;
        }

        double[] rhs;
        try {
            rhs = fitterB.fit(guess);
        } catch (RuntimeException t) {
            return;
        }

        if (!sameArray(lhs, rhs)) {
            throw new RuntimeException(
                "[oracle:fit-overload] metamorphic violation: fit() must agree with fit(ParameterGuesser.guess()) "
                    + "input=anchor"
                    + ",lhs=" + arrayToString(lhs)
                    + ",rhs=" + arrayToString(rhs));
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (t == null) {
            return false;
        }
        if (!t.getClass().getName().equals("org.apache.commons.math.exception.NotStrictlyPositiveException")) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        Package p = t.getClass().getPackage();
        if (p != null) {
            String n = p.getName();
            if (n != null && n.startsWith("org.apache.commons.math.exception")) {
                return true;
            }
        }
        return false;
    }

    private static double positiveFromInt(int v) {
        return v < 0 ? -(double) v : (double) v;
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
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            if (Double.isNaN(x) && Double.isNaN(y)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1.0e-7 * scale) {
                return false;
            }
        }
        return true;
    }

    private static String arrayToString(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}