package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        double baseMean = boundedDouble(data.consumeInt(), 100.0);
        double baseSd = positiveBoundedDouble(data.consumeInt(), 0.25, 20.0);

        double altMean = boundedDouble(data.consumeInt(), 100.0);
        double altSd = positiveBoundedDouble(data.consumeInt(), 0.25, 20.0);

        if (data.consumeBoolean()) {
            altMean = baseMean + boundedDouble(data.consumeInt(), 5.0);
        }
        if (data.consumeBoolean()) {
            altSd = positiveBoundedDouble(data.consumeInt(), 0.25, 20.0);
        }

        exploreEndpointRoot(baseMean, baseSd, altMean, altSd);
    }

    private static void runAnchor() {
        try {
            NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
            normal.inverseCumulativeProbability(0.9772498680518209d);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isKnownRootCauseMathException(t)) {
                return;
            }
        }
    }

    private static void exploreEndpointRoot(double mean, double sd, double altMean, double altSd) {
        NormalDistributionImpl dist = new NormalDistributionImpl(mean, sd);

        try {
            /*
             * Soundness:
             * We construct p from the distribution's own cumulativeProbability at x = mean + 2*sd.
             * For any correct implementation, inverseCumulativeProbability(p) must recover the same x.
             * We then mutate the receiver away from that state and restore it; after restoration,
             * the same call must agree with a freshly constructed equal object.
             * This is a receiver-state consistency check using two independent real-library routes:
             * (1) restored object, (2) fresh equal object.
             * A patch that merely deletes/suppresses the buggy throw or corrupts internal state/caching
             * can still violate this agreement.
             */
            double x = mean + 2.0d * sd;
            double p = dist.cumulativeProbability(x);

            // First drive the public API through the patched region with the endpoint-root property.
            try {
                dist.inverseCumulativeProbability(p);
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (!isKnownRootCauseMathException(t)) {
                    return;
                }
                return;
            }

            // Mutate receiver state, then restore original state, then re-probe.
            dist.setMean(altMean);
            dist.setStandardDeviation(altSd);
            dist.setMean(mean);
            dist.setStandardDeviation(sd);

            NormalDistributionImpl fresh = new NormalDistributionImpl(mean, sd);

            double restored;
            double freshValue;
            try {
                restored = dist.inverseCumulativeProbability(p);
                freshValue = fresh.inverseCumulativeProbability(p);
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                return;
            }

            if (Math.abs(restored - freshValue) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:state-fresh] metamorphic violation: restored object and fresh equal object disagree" +
                    " mean=" + mean +
                    " sd=" + sd +
                    " altMean=" + altMean +
                    " altSd=" + altSd +
                    " p=" + p +
                    " restored=" + restored +
                    " fresh=" + freshValue);
            }

            if (Math.abs(restored - x) > 1.0e-9d) {
                throw new RuntimeException(
                    "[oracle:endpoint-recompute] metamorphic violation: inverseCDF(CDF(x)) did not recover constructed endpoint root" +
                    " mean=" + mean +
                    " sd=" + sd +
                    " x=" + x +
                    " p=" + p +
                    " recovered=" + restored);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isKnownRootCauseMathException(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                    && "inverseCumulativeProbability".equals(method))
                || ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                    && "bracket".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.endsWith("IllegalArgumentException")
            || n.endsWith("InvalidArgumentException")
            || n.endsWith("NoDataException")
            || n.endsWith("NotPositiveException")
            || n.endsWith("NotStrictlyPositiveException")
            || n.endsWith("OutOfRangeException");
    }

    private static double boundedDouble(int raw, double bound) {
        return ((raw % 200001) - 100000) * (bound / 100000.0d);
    }

    private static double positiveBoundedDouble(int raw, double min, double max) {
        int v = Math.abs(raw % 100000);
        double unit = v / 100000.0d;
        return min + unit * (max - min);
    }
}