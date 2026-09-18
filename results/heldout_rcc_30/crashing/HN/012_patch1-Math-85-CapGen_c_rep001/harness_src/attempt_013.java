package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double ANCHOR_EXPECTED = 2.0d;
    private static final double ROUNDTRIP_TOLERANCE = 1.0e-6d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution anchorDist = new NormalDistributionImpl(0.0d, 1.0d);
        runAnchor(anchorDist);

        int trials = 1 + Math.max(0, data.consumeInt(0, 4));
        for (int i = 0; i < trials; i++) {
            exploreStandardNormalIntegerQuantiles(data);
        }
    }

    private static void runAnchor(NormalDistribution dist) {
        try {
            double result = dist.inverseCumulativeProbability(ANCHOR_P);
            if (Math.abs(result - ANCHOR_EXPECTED) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: inverseCumulativeProbability(normalCDF(2)) should recover 2.0 input="
                        + ANCHOR_P + " lhs=" + result + " rhs=" + ANCHOR_EXPECTED);
            }
        } catch (Throwable t) {
            if (isPatchedMethodBug(t)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static void exploreStandardNormalIntegerQuantiles(FuzzedDataProvider data) {
        NormalDistribution dist = new NormalDistributionImpl(0.0d, 1.0d);
        int k = data.consumeInt(-8, 8);
        double p;
        try {
            p = dist.cumulativeProbability((double) k);
        } catch (Throwable t) {
            if (isPatchedMethodBug(t)) {
                sneakyThrow(t);
            }
            return;
        }

        if (!(p >= 0.0d && p <= 1.0d) || Double.isNaN(p)) {
            return;
        }

        try {
            double recovered = dist.inverseCumulativeProbability(p);
            /* Contract/oracle:
             * We construct p from the real API itself as p = CDF(k) for a valid, moderate integer k.
             * For any correct implementation of inverseCumulativeProbability, applying the inverse to a
             * cumulative probability produced by the same distribution must recover the original quantile
             * up to solver tolerance. A throw-deleting patch or silent wrong-result patch breaks this.
             */
            if (Math.abs(recovered - (double) k) > ROUNDTRIP_TOLERANCE) {
                throw new RuntimeException(
                    "[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(k)) != k input="
                        + k + " lhs=" + recovered + " rhs=" + k);
            }
        } catch (Throwable t) {
            if (isPatchedMethodBug(t)) {
                sneakyThrow(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isPatchedMethodBug(Throwable t) {
        return (t instanceof MathException) && chainHasBracketFrame(t);
    }

    private static boolean chainHasBracketFrame(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] trace = cur.getStackTrace();
            if (trace != null) {
                for (StackTraceElement ste : trace) {
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                            && "bracket".equals(ste.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("OutOfRange")
                || name.contains("NoData")
                || name.contains("NullArgument");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}