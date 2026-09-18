package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double STRICT_TOL = 1.0e-12d;
    private static final double ROUNDTRIP_TOL = 1.0e-9d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistributionImpl anchorDist = new NormalDistributionImpl(0.0, 1.0);

        try {
            double result = anchorDist.inverseCumulativeProbability(ANCHOR_P);
            if (Math.abs(result - 2.0d) > STRICT_TOL) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability(cdf(2)) must recover 2.0 input="
                        + ANCHOR_P + " lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            handleLibraryThrowable(t);
            return;
        }

        int trials = data.consumeInt(1, 4);
        for (int i = 0; i < trials; i++) {
            NormalDistributionImpl dist = new NormalDistributionImpl(0.0, 1.0);
            int target = 2 + data.consumeInt(0, 32);

            double p;
            try {
                p = dist.cumulativeProbability((double) target);
            } catch (Throwable t) {
                handleLibraryThrowable(t);
                return;
            }

            try {
                double recovered = dist.inverseCumulativeProbability(p);
                if (Math.abs(recovered - (double) target) > ROUNDTRIP_TOL) {
                    /*
                     * Contract asserted: inverseCumulativeProbability is the inverse CDF.
                     * Since p is built by a real call to cumulativeProbability(target), a correct
                     * implementation must recover target (within floating tolerance). A patch that
                     * merely deletes the throw or bypasses the bracketing logic can silently return
                     * the wrong quantile and violate this observable round-trip relation.
                     */
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x input="
                            + target + " lhs=" + recovered + " rhs=" + target);
                }
            } catch (Throwable t) {
                handleLibraryThrowable(t);
                return;
            }
        }
    }

    private static void handleLibraryThrowable(Throwable t) {
        if (t instanceof RuntimeException && t.getClass() == RuntimeException.class
                && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            throw (RuntimeException) t;
        }
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
            if (name.startsWith("org.apache.commons.math")
                    && (name.contains("Invalid") || name.contains("Illegal") || name.contains("Argument"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException && stackPassesThroughBracket(cur)) {
                return true;
            }
        }
        return false;
    }

    private static boolean stackPassesThroughBracket(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                    && "bracket".equals(ste.getMethodName())) {
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