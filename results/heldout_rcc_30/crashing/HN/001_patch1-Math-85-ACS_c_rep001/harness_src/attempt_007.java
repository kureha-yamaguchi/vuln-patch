package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        double mean = data.consumeInt(-1000, 1000);
        double sd = data.consumeInt(1, 1000);
        int target = data.consumeInt(-8, 8);
        boolean useAnchorDistribution = data.consumeBoolean();

        NormalDistribution dist = useAnchorDistribution
                ? new NormalDistributionImpl(0.0, 1.0)
                : new NormalDistributionImpl(mean, sd);

        tryExploreRoundTrip(dist, target);

        int extraTargets = data.consumeInt(0, 4);
        for (int i = 0; i < extraTargets; i++) {
            int t = data.consumeInt(-8, 8);
            tryExploreRoundTrip(dist, t);
        }
    }

    private static void runAnchor() {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
        try {
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: faithful regression check input=0.9772498680518209 result=" + result + " expected=2.0");
            }
            double roundTrip = normal.cumulativeProbability(result);
            if (Math.abs(roundTrip - 0.9772498680518209d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-rt] metamorphic violation: inverse/cdf round-trip input=0.9772498680518209 inv=" + result + " cdf(inv)=" + roundTrip);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void tryExploreRoundTrip(NormalDistribution dist, int target) {
        try {
            double p = dist.cumulativeProbability((double) target);
            if (!(p >= 0.0d && p <= 1.0d) || Double.isNaN(p)) {
                return;
            }

            double inv = dist.inverseCumulativeProbability(p);

            /* Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * We construct p from the real cumulativeProbability(x), so p is valid by construction.
             * A patch that only deletes/suppresses the throw in bracket but returns a wrong value
             * will violate this round-trip relation.
             */
            double tolerance = 1.0e-9d;
            if (Math.abs(inv - (double) target) > tolerance) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverse(cdf(x))==x target=" + target + " p=" + p + " inv=" + inv);
            }

            double p2 = dist.cumulativeProbability(inv);
            if (Math.abs(p2 - p) > 1.0e-12d) {
                throw new RuntimeException("[oracle:cdfinv] metamorphic violation: cdf(inv(p))==p target=" + target + " p=" + p + " inv=" + inv + " cdf(inv)=" + p2);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] trace = cur.getStackTrace();
            for (int i = 0; i < trace.length; i++) {
                StackTraceElement e = trace[i];
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                        && "bracket".equals(e.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}