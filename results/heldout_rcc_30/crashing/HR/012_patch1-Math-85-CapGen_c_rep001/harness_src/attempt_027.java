package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runConstructedIntervalDeltaOracle(data);
    }

    private static void runAnchor() {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
        try {
            normal.inverseCumulativeProbability(0.9772498680518209);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                return;
            }
        }
    }

    private static void runConstructedIntervalDeltaOracle(FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);

        int a = data.consumeInt(-6, 5);
        int gap = data.consumeInt(1, 6);
        int b = a + gap;
        if (b > 6) {
            b = 6;
            if (a >= b) {
                a = b - 1;
            }
        }

        double pA;
        double pB;
        try {
            pA = normal.cumulativeProbability((double) a);
            pB = normal.cumulativeProbability((double) b);
        } catch (Throwable t) {
            return;
        }

        if (!(pA > 0.0 && pA < 1.0 && pB > 0.0 && pB < 1.0 && pA < pB)) {
            return;
        }

        double xA;
        try {
            xA = normal.inverseCumulativeProbability(pA);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                throw new RuntimeException("[oracle:constructed-interval-delta] valid constructed quantile rejected for lower endpoint a=" + a + " pA=" + pA, t);
            }
            return;
        }

        double xB;
        try {
            xB = normal.inverseCumulativeProbability(pB);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                throw new RuntimeException("[oracle:constructed-interval-delta] valid constructed quantile rejected for upper endpoint b=" + b + " pB=" + pB, t);
            }
            return;
        }

        if (!(xA <= xB)) {
            return;
        }

        try {
            /*
             * Contract used: for a continuous distribution, cumulativeProbability(x0, x1)
             * is the probability mass over the interval and must equal
             * cumulativeProbability(x1) - cumulativeProbability(x0).
             * This cross-check is independent of the internal bracketing used by inverse.
             * A throw-deleting or silently-wrong patch can let inverse return a bad endpoint;
             * then the interval mass recomputed from the returned quantiles disagrees with
             * the mass implied by the probabilities used to construct them.
             */
            double reported = normal.cumulativeProbability(xA, xB);
            double independent = pB - pA;

            double tol = 1e-12 + 1e-9 * Math.abs(independent);
            if (Math.abs(reported - independent) > tol) {
                throw new RuntimeException(
                    "[oracle:constructed-interval-delta] metamorphic violation: interval mass from inverse-constructed bounds disagrees with probability delta"
                    + " a=" + a
                    + " b=" + b
                    + " pA=" + pA
                    + " pB=" + pB
                    + " xA=" + xA
                    + " xB=" + xB
                    + " reported=" + reported
                    + " independent=" + independent);
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Throwable t) {
            return;
        }
    }

    private static boolean isRootCauseMathException(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String method = e.getMethodName();
            if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(method))
                || ("org.apache.commons.math.analysis.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(method))
                || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls) && "inverseCumulativeProbability".equals(method))
                || ("org.apache.commons.math.ConvergenceException".equals(cls) && "<init>".equals(method))
                || ("org.apache.commons.math.MathRuntimeException".equals(cls) && method != null && method.startsWith("createIllegalArgumentException"))
                || ("org.apache.commons.math.MathRuntimeException".equals(cls) && "buildMessage".equals(method))) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            return isRootCauseMathException(cause);
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable c = t; c != null && c != c.getCause(); c = c.getCause()) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
            String name = c.getClass().getName();
            if (name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("OutOfRange")
                || name.contains("NoData")
                || name.contains("NotPositive")
                || name.contains("NotStrictlyPositive")
                || name.contains("NullArgument")) {
                return true;
            }
        }
        return false;
    }
}