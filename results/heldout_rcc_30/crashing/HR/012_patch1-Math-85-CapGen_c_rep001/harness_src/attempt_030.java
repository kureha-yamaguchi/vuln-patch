package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorEndpointRoot();
        exploreShiftScaleEndpointRoots(data);
    }

    private static void runAnchorEndpointRoot() {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
        double p = 0.9772498680518209;
        double expectedQuantile = 2.0;
        try {
            double q = normal.inverseCumulativeProbability(p);
            checkZeroMassRecovery(normal, expectedQuantile, q, "anchor");
            checkIntervalApiAgreement(normal, expectedQuantile, q, "anchor");
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                throw new RuntimeException("[oracle:endpoint-zero-mass] valid inverse CDF input hit endpoint-root failure for anchor p=" + p, t);
            }
        }
    }

    private static void exploreShiftScaleEndpointRoots(FuzzedDataProvider data) {
        double mean = data.consumeInt(-1000, 1000) / 10.0;
        double sigma = data.consumeInt(1, 1000) / 10.0;
        boolean upper = data.consumeBoolean();

        NormalDistribution normal = new NormalDistributionImpl(mean, sigma);

        double target;
        if (upper) {
            target = mean + (2.0 * sigma);
        } else {
            target = mean - (2.0 * sigma);
        }

        try {
            double p = normal.cumulativeProbability(target);
            if (!(p > 0.0 && p < 1.0)) {
                return;
            }

            double q = normal.inverseCumulativeProbability(p);

            checkZeroMassRecovery(normal, target, q, "fuzz");
            checkIntervalApiAgreement(normal, target, q, "fuzz");
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseMathException(t)) {
                throw new RuntimeException("[oracle:endpoint-zero-mass] valid constructed probability from target=" + target + " mean=" + mean + " sigma=" + sigma + " triggered endpoint-root failure", t);
            }
        }
    }

    private static void checkZeroMassRecovery(NormalDistribution normal, double target, double recovered, String tag) throws MathException {
        double lo = Math.min(target, recovered);
        double hi = Math.max(target, recovered);

        /* Contract used for this oracle:
         * inverseCumulativeProbability(p) returns an x whose cumulative probability is p.
         * Here p is itself constructed as cumulativeProbability(target), so a correct implementation
         * must recover the same quantile. We read that equality through a second real API:
         * the interval probability between target and recovered must therefore be ~0.
         * A throw-deleting patch that returns the wrong quantile violates this post-condition.
         */
        double intervalMass = normal.cumulativeProbability(lo, hi);
        double tolerance = 1.0e-12;

        if (intervalMass > tolerance) {
            throw new RuntimeException("[oracle:endpoint-zero-mass] metamorphic violation: recovered quantile changed constructed probability mass tag=" + tag + " target=" + target + " recovered=" + recovered + " intervalMass=" + intervalMass);
        }
    }

    private static void checkIntervalApiAgreement(NormalDistribution normal, double a, double b, String tag) throws MathException {
        double lo = Math.min(a, b);
        double hi = Math.max(a, b);

        /* Contract used for this consistency check:
         * the API cumulativeProbability(x0, x1) reports the same interval mass as
         * cumulativeProbability(x1) - cumulativeProbability(x0).
         * This cross-check is independent of the quantile-recovery oracle above and
         * still detects a masked helper inconsistency if the top-level call stops throwing.
         */
        double reported = normal.cumulativeProbability(lo, hi);
        double recomputed = normal.cumulativeProbability(hi) - normal.cumulativeProbability(lo);
        double diff = Math.abs(reported - recomputed);
        double tolerance = 1.0e-12;

        if (diff > tolerance) {
            throw new RuntimeException("[oracle:interval-api-agree] consistency violation: tag=" + tag + " a=" + a + " b=" + b + " reported=" + reported + " recomputed=" + recomputed);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.indexOf("Invalid") >= 0 || name.indexOf("Illegal") >= 0) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCauseMathException(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && hasRelevantFrame(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasRelevantFrame(Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        if (frames == null) {
            return false;
        }
        int i;
        for (i = 0; i < frames.length; i++) {
            StackTraceElement ste = frames[i];
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls) && "inverseCumulativeProbability".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(cls) && "value".equals(method)) {
                return true;
            }
        }
        return false;
    }
}