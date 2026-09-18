package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        exploreExactEndpointCases(data);
        midpointInteriorOracle(data);
    }

    private static void runAnchor() {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
        double p = 0.9772498680518209d;
        try {
            double x = normal.inverseCumulativeProbability(p);
            if (Math.abs(x - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-int-exact] metamorphic violation: inverse(CDF(2)) must recover 2.0 for the documented continuous inverse on this valid probability input; p=" + p + " x=" + x);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void exploreExactEndpointCases(FuzzedDataProvider data) {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
        int trials = 1 + data.consumeInt(1, 6);
        for (int i = 0; i < trials; i++) {
            int z = data.consumeInt(-8, 8);
            try {
                double p = normal.cumulativeProbability((double) z);
                double x = normal.inverseCumulativeProbability(p);
                if (Math.abs(x - z) > 1.0e-9d) {
                    throw new RuntimeException("[oracle:lattice-recovery] metamorphic violation: probability built from the distribution's own CDF at an integer lattice point must invert back to that same point for a correct continuous normal distribution; z=" + z + " p=" + p + " x=" + x);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    continue;
                }
                if (isGroundTruthRootCause(t)) {
                    throwUnchecked(t);
                }
            }
        }
    }

    private static void midpointInteriorOracle(FuzzedDataProvider data) {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
        int z = data.consumeInt(-6, 5);
        try {
            double leftP = normal.cumulativeProbability((double) z);
            double rightP = normal.cumulativeProbability((double) (z + 1));
            double midP = 0.5d * (leftP + rightP);
            double x = normal.inverseCumulativeProbability(midP);

            /*
             * Contract basis: the normal CDF is continuous and strictly increasing, so its inverse on a
             * probability strictly between CDF(z) and CDF(z+1) must return a value strictly between z and z+1.
             * This catches a throw-deleting / endpoint-clamping patch that masks the original crash by returning
             * one bracket endpoint instead of the true interior quantile.
             */
            if (!(x > z && x < (z + 1))) {
                throw new RuntimeException("[oracle:midpoint-interior] metamorphic violation: inverse of a probability strictly between adjacent endpoint probabilities must lie strictly inside that adjacent x-interval; z=" + z + " leftP=" + leftP + " rightP=" + rightP + " midP=" + midP + " x=" + x);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && hasReachableFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasReachableFrame(Throwable t) {
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement e : stack) {
            String cls = e.getClassName();
            String m = e.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(m)) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                    && "inverseCumulativeProbability".equals(m)) {
                return true;
            }
            if ("org.apache.commons.math.MathRuntimeException".equals(cls)
                    && ("createIllegalArgumentException".equals(m) || "buildMessage".equals(m))) {
                return true;
            }
            if ("org.apache.commons.math.ConvergenceException".equals(cls) && "<init>".equals(m)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("IllegalArgument") || name.contains("Invalid") || name.contains("OutOfRange")
                    || name.contains("NoData") || name.contains("NotPositive")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}