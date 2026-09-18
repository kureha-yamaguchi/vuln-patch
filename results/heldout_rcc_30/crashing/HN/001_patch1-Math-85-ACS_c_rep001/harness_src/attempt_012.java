package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);

        // ANCHOR: exact regression input from NormalDistributionTest.testMath280.
        // Contract/oracle: inverseCumulativeProbability is the inverse of cumulativeProbability
        // on valid probabilities; the regression test requires this exact input to return 2.0.
        exerciseExactProbability(normal, 0.9772498680518209d, 2.0d, 1.0e-12d, true);

        // EXPLORE: generate many valid probabilities with the triggering property:
        // p = CDF(k) for an integer k whose exact root can land on a bracketing endpoint.
        // These are valid-by-construction inputs because p comes from the real distribution API.
        int magnitude = data.consumeInt(2, 6);
        int sign = data.consumeBoolean() ? 1 : -1;
        int xInt = sign * magnitude;
        double x = (double) xInt;

        try {
            double p = normal.cumulativeProbability(x);
            exerciseExactProbability(normal, p, x, 1.0e-9d, false);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
            return;
        }

        // Additional varied but still real and valid-by-construction cases:
        // perturb around integer roots using a second real-library round-trip seed.
        int deltaNum = data.consumeInt(-3, 3);
        double shiftedX = x + (deltaNum * 0.25d);
        if (shiftedX > -8.0d && shiftedX < 8.0d) {
            try {
                double p2 = normal.cumulativeProbability(shiftedX);
                double inv2 = normal.inverseCumulativeProbability(p2);
                double back2 = normal.cumulativeProbability(inv2);
                if (Math.abs(back2 - p2) > 1.0e-9d) {
                    throw new RuntimeException("[oracle:norm-roundtrip] metamorphic violation: inverse/cdf round-trip input=" + shiftedX + " p=" + p2 + " inv=" + inv2 + " back=" + back2);
                }
            } catch (Throwable t) {
                if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
            }
        }
    }

    private static void exerciseExactProbability(NormalDistribution normal, double p, double expectedX, double tol, boolean anchor) {
        try {
            double inv = normal.inverseCumulativeProbability(p);

            if (Math.abs(inv - expectedX) > tol) {
                throw new RuntimeException("[oracle:exact-inverse] metamorphic violation: expected inverseCumulativeProbability to recover known x input=" + p + " expected=" + expectedX + " actual=" + inv);
            }

            // Contract/oracle: for valid probabilities, cumulativeProbability(inverseCumulativeProbability(p)) == p.
            // If a patch merely suppresses the throw and returns the wrong value, this observable relation breaks.
            try {
                double back = normal.cumulativeProbability(inv);
                if (Math.abs(back - p) > Math.max(1.0e-12d, tol)) {
                    throw new RuntimeException("[oracle:cdf-inverse-roundtrip] metamorphic violation: cdf(inverseCumulativeProbability(p)) != p input=" + p + " inv=" + inv + " back=" + back);
                }
            } catch (Throwable t) {
                if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                    throw (RuntimeException) t;
                }
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                return;
            }

            if (anchor && Math.abs(inv - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-value] metamorphic violation: regression seed must return 2.0 input=" + p + " actual=" + inv);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && stackHasBracket(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean stackHasBracket(Throwable t) {
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (StackTraceElement e : st) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        if (name.startsWith("org.apache.commons.math.")) {
            String simple = t.getClass().getSimpleName();
            if (simple.contains("Illegal") || simple.contains("Invalid")) {
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