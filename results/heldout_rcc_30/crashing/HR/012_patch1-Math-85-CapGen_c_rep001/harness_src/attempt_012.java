package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }

        try {
            queryHistoryIndependenceOracle(data);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runAnchor() throws MathException {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        normal.inverseCumulativeProbability(0.9772498680518209);
    }

    private static void queryHistoryIndependenceOracle(FuzzedDataProvider data) {
        NormalDistribution fresh = new NormalDistributionImpl(0, 1);
        NormalDistribution warmed = new NormalDistributionImpl(0, 1);

        int z = data.consumeInt(-6, 6);
        double p;
        try {
            p = fresh.cumulativeProbability((double) z);
        } catch (Throwable t) {
            return;
        }

        int warmups = data.consumeInt(0, 8);
        for (int i = 0; i < warmups; i++) {
            try {
                int mode = data.consumeInt(0, 2);
                if (mode == 0) {
                    int numerator = data.consumeInt(1, 999999);
                    double q = numerator / 1000000.0;
                    warmed.inverseCumulativeProbability(q);
                } else if (mode == 1) {
                    int x = data.consumeInt(-6, 6);
                    warmed.cumulativeProbability((double) x);
                } else {
                    int numerator = data.consumeInt(1, 999999);
                    double q = numerator / 1000000.0;
                    warmed.inverseCumulativeProbability(q);
                    int x = data.consumeInt(-6, 6);
                    warmed.cumulativeProbability((double) x);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                return;
            }
        }

        double xFresh;
        double xWarm;
        try {
            xFresh = fresh.inverseCumulativeProbability(p);
            xWarm = warmed.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
            return;
        }

        /*
         * Contract basis: NormalDistributionImpl is immutable with fixed mean/stddev,
         * and inverseCumulativeProbability is a pure query. Prior unrelated queries
         * must not change a later answer for the same valid probability.
         * This catches a throw-deleting or stateful band-aid that masks the known crash
         * while leaving the helper path inconsistent.
         */
        if (Math.abs(xFresh - xWarm) > 1.0e-12) {
            throw new RuntimeException(
                "[oracle:history-indep] metamorphic violation: inverseCumulativeProbability depends on prior query history"
                    + " z=" + z
                    + " p=" + p
                    + " fresh=" + xFresh
                    + " warmed=" + xWarm
                    + " warmups=" + warmups);
        }

        /*
         * Independent cross-check using a second, identically-constructed object:
         * p was produced by cumulativeProbability(z) on the same standard normal, so
         * both identical objects must recover the same quantile for that p.
         * Use a generous tolerance to avoid floating-point noise.
         */
        if (Math.abs(xFresh - z) > 1.0e-9 || Math.abs(xWarm - z) > 1.0e-9) {
            throw new RuntimeException(
                "[oracle:built-quantile] metamorphic violation: quantile built from cumulativeProbability(z) was not recovered"
                    + " z=" + z
                    + " p=" + p
                    + " fresh=" + xFresh
                    + " warmed=" + xWarm);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || classNameContains(t, "Illegal")
            || classNameContains(t, "Invalid");
    }

    private static boolean classNameContains(Throwable t, String needle) {
        return t != null && t.getClass().getName().contains(needle);
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            String c = st[i].getClassName();
            String m = st[i].getMethodName();
            if (("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(c)
                    && "inverseCumulativeProbability".equals(m))
                || ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(c)
                    && "bracket".equals(m))) {
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