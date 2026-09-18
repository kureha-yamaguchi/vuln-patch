package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runLiteralRegressionProbe();
        runConstructedQuantileDualTailCheck(data);
    }

    private static void runLiteralRegressionProbe() {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        try {
            normal.inverseCumulativeProbability(0.9772498680518209);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedRegionMathFailure(t)) {
                return;
            }
        }
    }

    private static void runConstructedQuantileDualTailCheck(FuzzedDataProvider data) {
        final NormalDistribution maker = new NormalDistributionImpl(0, 1);
        final int k = data.consumeInt(1, 8);

        final double p;
        try {
            p = maker.cumulativeProbability((double) k);
        } catch (Throwable t) {
            return;
        }

        final NormalDistribution solver = new NormalDistributionImpl(0, 1);
        final double q;
        try {
            q = solver.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedRegionMathFailure(t)) {
                throw new RuntimeException(
                    "[oracle:dual-tail-cdf] inverse rejected valid probability constructed from cumulativeProbability("
                        + k + ") p=" + p,
                    t);
            }
            return;
        }

        try {
            /*
             * Sound contract:
             * q was obtained from inverseCumulativeProbability(p), so a correct implementation
             * must satisfy both lower-tail and upper-tail recovery on the same distribution:
             *   cumulativeProbability(q) == p
             *   cumulativeProbability(-q) == 1 - p   (standard normal symmetry)
             * This catches throw-deleting or wrong-value patches: if inverse returns an incorrect
             * q instead of throwing, the recovered tails disagree with the original probability.
             */
            double lower = solver.cumulativeProbability(q);
            double upperComplement = 1.0 - solver.cumulativeProbability(-q);

            if (Math.abs(lower - p) > 1.0e-12 || Math.abs(upperComplement - p) > 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:dual-tail-cdf] metamorphic violation: p must be recovered from both tails"
                        + " k=" + k + " p=" + p + " q=" + q
                        + " lower=" + lower + " upperComplement=" + upperComplement);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("IllegalArgument")
            || name.contains("Invalid")
            || name.contains("OutOfRange")
            || name.contains("NotPositive")
            || name.contains("NotStrictlyPositive");
    }

    private static boolean isPatchedRegionMathFailure(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String method = e.getMethodName();
            if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                    && "bracket".equals(method))
                || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                    && "inverseCumulativeProbability".equals(method))
                || ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(cls)
                    && "value".equals(method))
                || ("org.apache.commons.math.MathRuntimeException".equals(cls)
                    && ("createIllegalArgumentException".equals(method)
                        || "buildMessage".equals(method)))
                || ("org.apache.commons.math.ConvergenceException".equals(cls)
                    && "<init>".equals(method))) {
                return true;
            }
        }
        return false;
    }
}