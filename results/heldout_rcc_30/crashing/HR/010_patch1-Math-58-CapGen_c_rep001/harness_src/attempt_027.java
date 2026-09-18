package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.exception.MathIllegalArgumentException;
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
        exerciseAnchorDelegation();
        exerciseTailDelegation(data);
        exerciseInsufficientSampleRejection(data);
    }

    private static void exerciseAnchorDelegation() {
        checkPublicFitDelegatesSafely(ANCHOR, "[anchor]");
    }

    private static void exerciseTailDelegation(FuzzedDataProvider data) {
        int len = data.consumeInt(3, 32);
        boolean rightTail = data.consumeBoolean();
        double amplitude = boundedDouble(data.consumeInt(), 0.1, 1.0e4);
        double sigma = boundedDouble(data.consumeInt(), 0.2, 25.0);
        double spacing = boundedDouble(data.consumeInt(), 0.2, 3.0);
        double start = boundedDouble(data.consumeInt(), -50.0, 50.0);
        double offset = boundedDouble(data.consumeInt(), 0.5, 6.0) * sigma + spacing * len;

        double[] ys = new double[len];
        double mean = rightTail ? (start - offset) : (start + spacing * (len - 1) + offset);
        for (int i = 0; i < len; i++) {
            double x = start + i * spacing;
            double z = (x - mean) / sigma;
            ys[i] = amplitude * Math.exp(-0.5 * z * z);
        }

        checkPublicFitDelegatesSafely(ys, "[tail len=" + len + " side=" + (rightTail ? "R" : "L") + "]");
    }

    private static void exerciseInsufficientSampleRejection(FuzzedDataProvider data) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());

        probeMustReject(fitter, 0, "empty");

        int mutations = data.consumeInt(0, 2);
        for (int i = 0; i < mutations; i++) {
            double x = boundedDouble(data.consumeInt(), -100.0, 100.0);
            double y = boundedDouble(data.consumeInt(), 0.0, 1.0e4);
            fitter.addObservedPoint(x, y);

            WeightedObservedPoint[] snapshot = fitter.getObservations();
            if (snapshot.length != i + 1) {
                throw new RuntimeException("[oracle:insufficient-reprobe] snapshot size mismatch after add: expected=" + (i + 1) + " actual=" + snapshot.length);
            }

            probeMustReject(fitter, snapshot.length, "afterAdd" + i);
        }
    }

    private static void probeMustReject(GaussianFitter fitter, int expectedSize, String label) {
        try {
            fitter.fit();
            throw new RuntimeException("[oracle:insufficient-reprobe] fit() accepted fewer than three observations at state=" + label + " size=" + expectedSize);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseThrowable(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void checkPublicFitDelegatesSafely(double[] ys, String label) {
        GaussianFitter fitForGuess = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitForGuess.addObservedPoint(i, ys[i]);
        }

        final double[] guessed;
        try {
            guessed = new GaussianFitter.ParameterGuesser(fitForGuess.getObservations()).guess();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseThrowable(t)) {
                throwUnchecked(t);
            }
            return;
        }

        double[] explicitResult = null;
        boolean explicitOk = false;
        try {
            GaussianFitter explicit = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                explicit.addObservedPoint(i, ys[i]);
            }
            explicitResult = explicit.fit(guessed);
            explicitOk = true;
        } catch (Throwable t) {
            if (!isCleanRejection(t) && isRootCauseThrowable(t)) {
                throwUnchecked(t);
            }
            return;
        }

        try {
            GaussianFitter pub = new GaussianFitter(new LevenbergMarquardtOptimizer());
            for (int i = 0; i < ys.length; i++) {
                pub.addObservedPoint(i, ys[i]);
            }
            double[] publicResult = pub.fit();
            if (explicitOk && !closeVec(publicResult, explicitResult)) {
                throw new RuntimeException(
                    "[oracle:delegate-tail] metamorphic violation: fit() must delegate consistently to fit(guess) " +
                    label + " public=" + vec(publicResult) + " explicit=" + vec(explicitResult) + " guess=" + vec(guessed));
            }
        } catch (Throwable t) {
            if (explicitOk && isRootCauseThrowable(t)) {
                throw new RuntimeException(
                    "[oracle:delegate-tail] metamorphic violation: public fit() leaked root-cause failure while fit(guess) succeeded " +
                    label + " guess=" + vec(guessed) + " explicit=" + vec(explicitResult), t);
            }
            if (!isCleanRejection(t) && isRootCauseThrowable(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException || t instanceof MathIllegalArgumentException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.startsWith("org.apache.commons.math.exception.");
    }

    private static boolean isRootCauseThrowable(Throwable t) {
        if (!t.getClass().getName().endsWith("NotStrictlyPositiveException")) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String cls = st[i].getClassName();
            String m = st[i].getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cls) && "fit".equals(m)) ||
                ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cls) && "getObservations".equals(m)) ||
                ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(cls) && "guess".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static boolean closeVec(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double av = a[i];
            double bv = b[i];
            double scale = Math.max(1.0, Math.max(Math.abs(av), Math.abs(bv)));
            if (Double.isNaN(av) || Double.isNaN(bv) || Math.abs(av - bv) > 1.0e-5 * scale) {
                return false;
            }
        }
        return true;
    }

    private static double boundedDouble(int raw, double min, double max) {
        long positive = raw & 0x7fffffffL;
        double fraction = positive / (double) Integer.MAX_VALUE;
        return min + (max - min) * fraction;
    }

    private static String vec(double[] v) {
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

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}