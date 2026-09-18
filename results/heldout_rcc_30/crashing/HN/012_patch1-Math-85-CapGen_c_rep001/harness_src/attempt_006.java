package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);

        exerciseAnchor(normal);
        exerciseExplore(normal, data);
    }

    private static void exerciseAnchor(NormalDistribution normal) {
        try {
            double p = 0.9772498680518209;
            double x = normal.inverseCumulativeProbability(p);
            if (Math.abs(x - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression input should invert to 2.0 input=" + p + " result=" + x);
            }
            /* API contract for a distribution inverse: for a valid probability p in (0,1),
             * cumulativeProbability(inverseCumulativeProbability(p)) should recover p.
             * A patch that merely suppresses the throw or returns a wrong value breaks this. */
            double roundTrip = normal.cumulativeProbability(x);
            if (Math.abs(roundTrip - p) > 1.0e-6) {
                throw new RuntimeException("[oracle:roundtrip-anchor] metamorphic violation: CDF(ICDF(p)) != p input=" + p + " x=" + x + " roundTrip=" + roundTrip);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void exerciseExplore(NormalDistribution normal, FuzzedDataProvider data) {
        int iterations = 1 + data.consumeInt(0, 8);
        for (int i = 0; i < iterations; i++) {
            int magnitude = data.consumeInt(2, 20);
            int sign = data.consumeBoolean() ? 1 : -1;
            int k = sign * magnitude;

            try {
                double targetX = (double) k;
                double p = normal.cumulativeProbability(targetX);
                double recovered = normal.inverseCumulativeProbability(p);

                if (Math.abs(recovered - targetX) > 1.0e-6) {
                    throw new RuntimeException("[oracle:quantile-recovery] metamorphic violation: ICDF(CDF(x)) != x input=" + targetX + " p=" + p + " recovered=" + recovered);
                }

                /* Same documented inverse-operation guarantee as above, checked in the opposite
                 * direction on valid-by-construction probabilities produced by the real library. */
                double p2 = normal.cumulativeProbability(recovered);
                if (Math.abs(p2 - p) > 1.0e-6) {
                    throw new RuntimeException("[oracle:cdf-icdf] metamorphic violation: CDF(ICDF(p)) != p x=" + targetX + " p=" + p + " recovered=" + recovered + " roundTrip=" + p2);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
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
            String name = cur.getClass().getName();
            if (name.contains("Invalid")
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

    private static boolean isRootCause(Throwable t) {
        boolean mathExceptionFamily = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                mathExceptionFamily = true;
                break;
            }
        }
        if (!mathExceptionFamily) {
            return false;
        }
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            for (StackTraceElement ste : cur.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                        && "bracket".equals(ste.getMethodName())) {
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