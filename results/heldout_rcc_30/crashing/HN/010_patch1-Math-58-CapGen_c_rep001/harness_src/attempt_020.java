package org.apache.commons.math.optimization.fitting;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.function.Gaussian;
import org.apache.commons.math.exception.MathIllegalArgumentException;
import org.apache.commons.math.exception.NotStrictlyPositiveException;
import org.apache.commons.math.optimization.general.LevenbergMarquardtOptimizer;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        double[] anchor = new double[] {
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

        runCase(anchor, true);

        int n = data.consumeInt(3, 40);
        double[] ys = new double[n];

        boolean useMonotoneIncreasing = data.consumeBoolean();
        if (useMonotoneIncreasing) {
            double current = Math.abs((double) data.consumeInt(-1000, 1000)) / 1000.0 + 1e-12;
            for (int i = 0; i < n; i++) {
                int stepRaw = data.consumeInt(0, 1000);
                current += (stepRaw + 1) * 1e-6;
                ys[i] = current;
            }
        } else {
            int center = data.consumeInt(0, n - 1);
            double amplitude = Math.abs((double) data.consumeInt(-100000, 100000)) / 1000.0 + 1e-6;
            double sigma = Math.abs((double) data.consumeInt(1, 2000)) / 200.0 + 0.1;
            double baseline = Math.abs((double) data.consumeInt(-1000, 1000)) / 1000.0;
            Gaussian.Parametric g = new Gaussian.Parametric();
            for (int i = 0; i < n; i++) {
                double x = i;
                double y;
                try {
                    y = g.value(x, new double[] { amplitude, center, sigma });
                } catch (RuntimeException e) {
                    return;
                }
                double noise = ((double) data.consumeInt(-1000, 1000)) / 1_000_000.0;
                ys[i] = Math.max(1e-12, baseline + y + noise);
            }
        }

        runCase(ys, false);
    }

    private static void runCase(double[] ys, boolean propagateRootCause) {
        GaussianFitter fitter = new GaussianFitter(new LevenbergMarquardtOptimizer());
        for (int i = 0; i < ys.length; i++) {
            double y = ys[i];
            if (Double.isNaN(y) || Double.isInfinite(y) || y <= 0.0) {
                y = Math.abs(y);
                if (Double.isNaN(y) || Double.isInfinite(y) || y == 0.0) {
                    y = 1e-12;
                }
            }
            fitter.addObservedPoint(i, y);
        }

        try {
            fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagate(t, propagateRootCause)) {
                sneakyThrow(t);
            }
            return;
        }

        double[] guess;
        try {
            guess = (new GaussianFitter.ParameterGuesser(fitter.getObservations())).guess();
        } catch (Throwable t) {
            return;
        }

        double[] viaOverload;
        try {
            viaOverload = fitter.fit(guess);
        } catch (Throwable t) {
            if (shouldPropagate(t, false)) {
                sneakyThrow(t);
            }
            return;
        }

        double[] viaNoArg;
        try {
            viaNoArg = fitter.fit();
        } catch (Throwable t) {
            if (shouldPropagate(t, propagateRootCause)) {
                sneakyThrow(t);
            }
            return;
        }

        /* Contract/oracle: GaussianFitter.fit() computes a ParameterGuesser guess and then delegates to
           the same fitting operation as fit(double[] initialGuess). Therefore, for the same observations,
           fit() and fit(guess()) must agree whenever both calls succeed. A patch that merely suppresses
           the exception path or changes the delegation logic can violate this observable equivalence. */
        if (viaNoArg == null || viaOverload == null || viaNoArg.length != viaOverload.length) {
            throw new RuntimeException("[oracle:fit-overload] metamorphic violation: fit() and fit(guess()) returned incompatible shapes");
        }
        for (int i = 0; i < viaNoArg.length; i++) {
            double a = viaNoArg[i];
            double b = viaOverload[i];
            boolean equal;
            if (Double.isNaN(a) && Double.isNaN(b)) {
                equal = true;
            } else if (Double.isInfinite(a) || Double.isInfinite(b)) {
                equal = a == b;
            } else {
                double scale = Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)));
                equal = Math.abs(a - b) <= 1e-8 * scale;
            }
            if (!equal) {
                throw new RuntimeException(
                    "[oracle:fit-overload] metamorphic violation: fit() != fit(guess()) inputLen=" + ys.length +
                    " index=" + i + " lhs=" + a + " rhs=" + b
                );
            }
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean allowRootCause) {
        if (!allowRootCause) {
            return false;
        }
        if (!(t instanceof NotStrictlyPositiveException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement e : trace) {
            if ("org.apache.commons.math.optimization.fitting.GaussianFitter".equals(e.getClassName())
                    && "fit".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}