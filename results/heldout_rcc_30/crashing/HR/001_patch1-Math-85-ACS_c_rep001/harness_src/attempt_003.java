package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double SEED_P = 0.9772498680518209d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistributionImpl standard = new NormalDistributionImpl(0.0, 1.0);
        checkMustReturnQuantile(standard, SEED_P, 2.0, 1.0e-12, "anchor-seed");

        double[] probs = new double[3];
        probs[0] = SEED_P;
        probs[1] = nextDown(SEED_P);
        probs[2] = nextUp(SEED_P);

        int trials = 1 + data.consumeInt(1, 6);
        for (int i = 0; i < trials; i++) {
            int meanInt = data.consumeInt(-20, 20);
            double mean = (double) meanInt;
            NormalDistributionImpl shifted = new NormalDistributionImpl(mean, 1.0);

            checkMustReturnQuantile(shifted, SEED_P, mean + 2.0, 1.0e-10, "shifted-seed");

            for (int j = 0; j < probs.length; j++) {
                checkLocationEquivariance(mean, probs[j]);
            }

            int offset = data.consumeInt(1, 6);
            double x = mean + offset;
            try {
                double pFromApi = shifted.cumulativeProbability(x);
                if (pFromApi > 0.0 && pFromApi < 1.0) {
                    checkLocationEquivariance(mean, pFromApi);
                }
            } catch (Throwable t) {
            }
        }
    }

    private static void checkMustReturnQuantile(NormalDistributionImpl dist, double p, double expected,
                                                double tol, String tag) {
        if (!(p > 0.0 && p < 1.0)) {
            return;
        }
        try {
            double q = dist.inverseCumulativeProbability(p);
            if (Math.abs(q - expected) > tol) {
                throw new RuntimeException(
                    "[oracle:" + tag + "] metamorphic violation: expected quantile mismatch p=" + p +
                    " expected=" + expected + " actual=" + q +
                    " mean=" + dist.getMean() + " sd=" + dist.getStandardDeviation());
            }
        } catch (MathException e) {
            if (isInPatchedScope(e)) {
                throw new RuntimeException(
                    "[oracle:" + tag + "] metamorphic violation: valid probability rejected p=" + p +
                    " expected=" + expected + " mean=" + dist.getMean() +
                    " sd=" + dist.getStandardDeviation(), e);
            }
        } catch (IllegalArgumentException e) {
        } catch (RuntimeException e) {
            if (isOracle(e)) {
                throw e;
            }
        }
    }

    private static void checkLocationEquivariance(double mean, double p) {
        if (!(p > 0.0 && p < 1.0)) {
            return;
        }
        NormalDistributionImpl standard = new NormalDistributionImpl(0.0, 1.0);
        NormalDistributionImpl shifted = new NormalDistributionImpl(mean, 1.0);
        try {
            double q0 = standard.inverseCumulativeProbability(p);
            double qm = shifted.inverseCumulativeProbability(p);
            double lhs = qm - q0;
            double rhs = mean;
            if (Math.abs(lhs - rhs) > 1.0e-10) {
                throw new RuntimeException(
                    "[oracle:location-shift] metamorphic violation: quantile shift mismatch p=" + p +
                    " mean=" + mean + " shifted=" + qm + " standard=" + q0 +
                    " lhs=" + lhs + " rhs=" + rhs);
            }
        } catch (Throwable t) {
            if (isOracle(t)) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isOracle(Throwable t) {
        return t instanceof RuntimeException
            && t.getMessage() != null
            && t.getMessage().startsWith("[oracle:");
    }

    private static boolean isInPatchedScope(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] stack = cur.getStackTrace();
            for (int i = 0; i < stack.length; i++) {
                String cls = stack[i].getClassName();
                String method = stack[i].getMethodName();
                if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                        && "bracket".equals(method))
                    || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                        && "inverseCumulativeProbability".equals(method))
                    || ("org.apache.commons.math.ConvergenceException".equals(cls)
                        && "<init>".equals(method))
                    || ("org.apache.commons.math.MathRuntimeException".equals(cls)
                        && ("createIllegalArgumentException".equals(method)
                            || "buildMessage".equals(method)))) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static double nextUp(double x) {
        if (Double.isNaN(x) || x == Double.POSITIVE_INFINITY) {
            return x;
        }
        if (x == 0.0d) {
            return Double.longBitsToDouble(1L);
        }
        long bits = Double.doubleToLongBits(x);
        return Double.longBitsToDouble(x > 0.0d ? bits + 1L : bits - 1L);
    }

    private static double nextDown(double x) {
        if (Double.isNaN(x) || x == Double.NEGATIVE_INFINITY) {
            return x;
        }
        if (x == 0.0d) {
            return -Double.longBitsToDouble(1L);
        }
        long bits = Double.doubleToLongBits(x);
        return Double.longBitsToDouble(x > 0.0d ? bits - 1L : bits + 1L);
    }
}