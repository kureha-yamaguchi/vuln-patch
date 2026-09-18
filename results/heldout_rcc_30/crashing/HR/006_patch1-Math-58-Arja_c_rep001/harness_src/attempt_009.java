package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR = {
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
        exerciseStatefulDelegateAgreement(ANCHOR);
        exerciseGuessOrderInvariance(ANCHOR);

        double[] synthetic = buildPositiveGaussianLikeData(data);
        if (synthetic != null) {
            exerciseGuessOrderInvariance(synthetic);
            exerciseStatefulDelegateAgreement(synthetic);
        }
    }

    private static double[] buildPositiveGaussianLikeData(FuzzedDataProvider data) {
        if (data.remainingBytes() <= 0) {
            return null;
        }

        int n = data.consumeInt(5, 40);
        double norm = 1.0 + data.consumeInt(0, 5000);
        double sigma = 0.5 + (data.consumeInt(0, 2000) / 200.0);
        double center = data.consumeInt(-20, 20) + (data.consumeInt(0, 1000) / 1000.0);
        double spacing = 0.25 + (data.consumeInt(0, 40) / 20.0);

        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            double x = (i * spacing) - ((n - 1) * spacing / 2.0);
            double d = x - center;
            double v = norm * Math.exp(-(d * d) / (2.0 * sigma * sigma));
            if (!(v > 0.0) || Double.isNaN(v) || Double.isInfinite(v)) {
                return null;
            }
            y[i] = v;
        }
        return y;
    }

    private static void exerciseStatefulDelegateAgreement(double[] y) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < y.length; i++) {
            fitter.addObservedPoint(i, y[i]);
        }

        final double[] guess;
        try {
            // Contract visible in GaussianFitter.fit(): it computes a ParameterGuesser guess
            // from getObservations() and then fits using that guess. A prior read-only fit(double[])
            // must not change the later fit() result on the same receiver.
            guess = new GaussianFitter.ParameterGuesser(fitter.getObservations()).guess();
        } catch (Throwable t) {
            return;
        }

        final double[] explicit;
        try {
            explicit = fitter.fit(guess);
        } catch (Throwable t) {
            return;
        }

        try {
            double[] implicit = fitter.fit();
            if (!sameParams(explicit, implicit)) {
                throw new RuntimeException(
                    "[oracle:readonly-delegate] metamorphic violation: fit(double[] guess) and later fit() disagree"
                    + " explicit=" + format(explicit)
                    + " implicit=" + format(implicit)
                    + " n=" + y.length);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:readonly-delegate] metamorphic violation: fit(double[] guess) succeeded, but subsequent fit() failed on same observations"
                    + " guess=" + format(guess)
                    + " explicit=" + format(explicit)
                    + " n=" + y.length, t);
            }
        }
    }

    private static void exerciseGuessOrderInvariance(double[] y) {
        WeightedObservedPoint[] a = new WeightedObservedPoint[y.length];
        WeightedObservedPoint[] b = new WeightedObservedPoint[y.length];
        for (int i = 0; i < y.length; i++) {
            a[i] = new WeightedObservedPoint(1.0, i, y[i]);
            b[y.length - 1 - i] = new WeightedObservedPoint(1.0, i, y[i]);
        }

        try {
            // ParameterGuesser.basicGuess operates on the observation content, not insertion order.
            // Reversing the same observation set must therefore produce the same guessed parameters.
            double[] g1 = new GaussianFitter.ParameterGuesser(a).guess();
            double[] g2 = new GaussianFitter.ParameterGuesser(b).guess();
            if (!sameParams(g1, g2)) {
                throw new RuntimeException(
                    "[oracle:guess-order] metamorphic violation: guess changed after reversing equivalent observations"
                    + " forward=" + format(g1)
                    + " reverse=" + format(g2)
                    + " n=" + y.length);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:guess-order] unexpected root-cause failure while guessing from valid-by-construction positive Gaussian-shaped observations"
                    + " n=" + y.length, t);
            }
        }
    }

    private static boolean sameParams(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            if (Double.doubleToLongBits(x) == Double.doubleToLongBits(y)) {
                continue;
            }
            double scale = Math.max(1.0, Math.max(Math.abs(x), Math.abs(y)));
            if (Math.abs(x - y) > 1e-7 * scale) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRootCause(Throwable t) {
        if (t == null) {
            return false;
        }
        String name = t.getClass().getName();
        if (name == null || !name.contains("NotStrictlyPositive")) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cn = ste.getClassName();
            String mn = ste.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(cn) && "fit".equals(mn))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(cn) && "getObservations".equals(mn))
                || (cn != null && cn.contains("GaussianFitter$ParameterGuesser") && "guess".equals(mn))
                || ("org.apache.commons.math.analysis.function.Gaussian$Parametric".equals(cn))) {
                return true;
            }
        }
        return false;
    }

    private static String format(double[] v) {
        if (v == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < v.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}