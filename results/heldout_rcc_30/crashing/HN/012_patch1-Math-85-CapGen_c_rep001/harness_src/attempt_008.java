package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                sneakyThrow(t);
            }
            return;
        }

        double mean = boundedDouble(data.consumeInt(), -1000.0, 1000.0);
        double sd = boundedPositiveDouble(data.consumeInt(), 1.0e-6, 1000.0);

        NormalDistributionImpl dist;
        try {
            dist = new NormalDistributionImpl(mean, sd);
        } catch (Throwable t) {
            return;
        }

        double targetX;
        if (data.consumeBoolean()) {
            targetX = mean + 2.0 * sd;
        } else {
            int k = data.consumeInt(-8, 8);
            targetX = mean + ((double) k) * sd;
        }

        try {
            double p = dist.cumulativeProbability(targetX);
            if (!(p > 0.0 && p < 1.0) || Double.isNaN(p)) {
                return;
            }

            double recovered = dist.inverseCumulativeProbability(p);

            /* Contract/metamorphic check:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * We construct p from a real NormalDistributionImpl.cumulativeProbability(targetX), so this input is valid by construction.
             * A patch that merely suppresses the throw in bracket or skips correct bracketing can silently return the wrong quantile,
             * violating inverse(cdf(x)) ~= x even when no exception is thrown.
             */
            if (!approximatelyEqual(recovered, targetX, 1.0e-6)) {
                throw new RuntimeException(
                    "[oracle:norm-inverse-roundtrip] metamorphic violation: inverse(cdf(x)) != x"
                    + " mean=" + mean
                    + " sd=" + sd
                    + " x=" + targetX
                    + " p=" + p
                    + " recovered=" + recovered
                );
            }

            double p2 = dist.cumulativeProbability(recovered);
            if (!(Double.isNaN(p2)) && !approximatelyEqual(p2, p, 1.0e-6)) {
                throw new RuntimeException(
                    "[oracle:norm-cdf-roundtrip] metamorphic violation: cdf(inverse(p)) != p"
                    + " mean=" + mean
                    + " sd=" + sd
                    + " x=" + targetX
                    + " p=" + p
                    + " recovered=" + recovered
                    + " p2=" + p2
                );
            }
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runAnchor() throws MathException {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
        double result = normal.inverseCumulativeProbability(0.9772498680518209d);
        if (!approximatelyEqual(result, 2.0d, 1.0e-12)) {
            throw new RuntimeException(
                "[oracle:anchor-exact] metamorphic violation: failing-test seed should recover quantile 2.0"
                + " inputP=0.9772498680518209 result=" + result
            );
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            return true;
        }
        if (!(t instanceof MathException)) {
            return false;
        }
        return stackHasBracket(t);
    }

    private static boolean stackHasBracket(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                        && "bracket".equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean approximatelyEqual(double a, double b, double tol) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }
        if (a == b) {
            return true;
        }
        double scale = Math.max(1.0d, Math.max(Math.abs(a), Math.abs(b)));
        return Math.abs(a - b) <= tol * scale;
    }

    private static double boundedDouble(int raw, double min, double max) {
        long unsigned = ((long) raw) - (long) Integer.MIN_VALUE;
        double unit = unsigned / 4294967295.0d;
        return min + (max - min) * unit;
    }

    private static double boundedPositiveDouble(int raw, double min, double max) {
        double v = boundedDouble(raw, min, max);
        if (v <= 0.0d || Double.isNaN(v) || Double.isInfinite(v)) {
            return min;
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}