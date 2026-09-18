package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.Arrays;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    private static final double[] ANCHOR_Y = new double[] {
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
        runScenario(ANCHOR_Y, 0.0, 1.0, true);

        int mode = data.consumeInt(0, 2);
        double[] ys;
        double x0 = data.consumeInt(-50, 50);
        double step = 1.0 + data.consumeInt(0, 4);

        if (mode == 0) {
            int prefixDrop = data.consumeInt(0, Math.max(0, ANCHOR_Y.length - 3));
            int suffixDrop = data.consumeInt(0, Math.max(0, ANCHOR_Y.length - prefixDrop - 3));
            int len = ANCHOR_Y.length - prefixDrop - suffixDrop;
            ys = new double[len];
            System.arraycopy(ANCHOR_Y, prefixDrop, ys, 0, len);
            if (data.consumeBoolean()) {
                int idx = data.consumeInt(0, len - 1);
                double factor = 1.0 + (data.consumeInt(-20, 20) / 200.0);
                if (factor <= 0.0) {
                    factor = 0.05;
                }
                ys[idx] *= factor;
            }
        } else if (mode == 1) {
            int len = data.consumeInt(3, 40);
            ys = new double[len];
            double v = Math.pow(10.0, -data.consumeInt(5, 40));
            for (int i = 0; i < len; i++) {
                double grow = 1.05 + (data.consumeInt(0, 400) / 100.0);
                v *= grow;
                if (!(v > 0.0) || Double.isInfinite(v) || Double.isNaN(v)) {
                    v = Math.abs(v);
                    if (!(v > 0.0) || Double.isInfinite(v) || Double.isNaN(v)) {
                        v = 1e-300;
                    }
                }
                ys[i] = v;
            }
        } else {
            int left = data.consumeInt(1, 15);
            int right = data.consumeInt(1, 15);
            ys = new double[left + ANCHOR_Y.length + right];
            double tiny = 1e-320;
            for (int i = 0; i < left; i++) {
                ys[i] = tiny;
                tiny *= 10.0;
                if (!(tiny > 0.0) || Double.isInfinite(tiny) || Double.isNaN(tiny)) {
                    tiny = 1e-300;
                }
            }
            System.arraycopy(ANCHOR_Y, 0, ys, left, ANCHOR_Y.length);
            tiny = ANCHOR_Y[ANCHOR_Y.length - 1];
            for (int i = 0; i < right; i++) {
                tiny *= 1.2 + (data.consumeInt(0, 30) / 100.0);
                if (!(tiny > 0.0) || Double.isInfinite(tiny) || Double.isNaN(tiny)) {
                    tiny = ANCHOR_Y[ANCHOR_Y.length - 1];
                }
                ys[left + ANCHOR_Y.length + i] = tiny;
            }
        }

        runScenario(ys, x0, step, true);
    }

    private static void runScenario(double[] ys, double x0, double step, boolean validByConstruction) {
        if (!isValidDataset(ys, step)) {
            return;
        }

        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            fitter.addObservedPoint(x0 + i * step, ys[i]);
        }

        WeightedObservedPoint[] before = fitter.getObservations();
        if (before.length != ys.length) {
            throw new RuntimeException("[oracle:getobs-size] metamorphic violation: snapshot size mismatch input=" + ys.length + " got=" + before.length);
        }

        GaussianFitter.ParameterGuesser guesser = new GaussianFitter.ParameterGuesser(before);
        double[] guess1;
        double[] guess2;
        try {
            guess1 = guesser.guess();
            guess2 = guesser.guess();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (guess1 == guess2 || !Arrays.equals(guess1, guess2)) {
            throw new RuntimeException("[oracle:guess-clone] metamorphic violation: repeated guess() must be stable and returned arrays must be independent");
        }

        WeightedObservedPoint[] beforeSecondRead = fitter.getObservations();
        if (!sameObservations(before, beforeSecondRead)) {
            throw new RuntimeException("[oracle:getobs-stable] metamorphic violation: repeated getObservations() changed without mutation");
        }

        double[] fitAuto = null;
        double[] fitExplicit = null;
        boolean autoOk = false;
        boolean explicitOk = false;

        try {
            fitAuto = fitter.fit();
            autoOk = true;
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            if (!isCleanRejection(t)) {
                return;
            }
            return;
        }

        WeightedObservedPoint[] afterAuto = fitter.getObservations();
        if (!sameObservations(before, afterAuto)) {
            throw new RuntimeException("[oracle:fit-readonly] metamorphic violation: fit() changed stored observations");
        }

        GaussianFitter fitter2 = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (WeightedObservedPoint p : before) {
            fitter2.addObservedPoint(p.getWeight(), p.getX(), p.getY());
        }

        try {
            fitExplicit = fitter2.fit(guess1.clone());
            explicitOk = true;
        } catch (Throwable t) {
            if (validByConstruction && isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        if (autoOk && explicitOk) {
            if (fitAuto == null || fitExplicit == null || fitAuto.length != fitExplicit.length) {
                throw new RuntimeException("[oracle:fit-eq-shape] metamorphic violation: fit result shapes differ");
            }
            for (int i = 0; i < fitAuto.length; i++) {
                double a = fitAuto[i];
                double b = fitExplicit[i];
                double diff = Math.abs(a - b);
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                if (Double.isNaN(a) != Double.isNaN(b) || diff > 1e-6 * scale) {
                    throw new RuntimeException("[oracle:fit-eq] metamorphic violation: fit() and fit(guess()) disagree at index=" + i + " lhs=" + a + " rhs=" + b);
                }
            }
        }
    }

    private static boolean sameObservations(WeightedObservedPoint[] a, WeightedObservedPoint[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            WeightedObservedPoint p = a[i];
            WeightedObservedPoint q = b[i];
            if (Double.doubleToLongBits(p.getWeight()) != Double.doubleToLongBits(q.getWeight())
                || Double.doubleToLongBits(p.getX()) != Double.doubleToLongBits(q.getX())
                || Double.doubleToLongBits(p.getY()) != Double.doubleToLongBits(q.getY())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidDataset(double[] ys, double step) {
        if (ys == null || ys.length < 3 || !(step > 0.0) || Double.isNaN(step) || Double.isInfinite(step)) {
            return false;
        }
        for (double y : ys) {
            if (!(y > 0.0) || Double.isNaN(y) || Double.isInfinite(y)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || t instanceof MathIllegalArgumentException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            String m = e.getMethodName();
            if (("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(c) && "fit".equals(m))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(c) && "fit".equals(m))
                || ("org.apache.commons.math.optimization.fitting.CurveFitter".equals(c) && "getObservations".equals(m))
                || ("org.apache.commons.math.optimization.fitting.GaussianFitter$ParameterGuesser".equals(c) && "guess".equals(m))) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}