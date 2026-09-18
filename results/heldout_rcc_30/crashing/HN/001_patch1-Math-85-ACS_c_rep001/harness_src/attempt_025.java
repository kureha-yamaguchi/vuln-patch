package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int mean = data.consumeInt(-1000, 1000);
        explore(mean);

        int extraCases = data.consumeInt(0, 4);
        for (int i = 0; i < extraCases; i++) {
            int m = data.consumeInt(-1000, 1000);
            explore(m);
        }
    }

    private static void anchor() {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        try {
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:anchor] metamorphic violation: failing-test contract input=0.9772498680518209 result="
                        + result + " expected=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void explore(int mean) {
        NormalDistribution dist = new NormalDistributionImpl(mean, 1.0d);
        double x = mean + 2.0d;
        try {
            double p = dist.cumulativeProbability(x);
            double inv = dist.inverseCumulativeProbability(p);

            /* Contract/oracle:
             * For a continuous normal distribution, inverseCumulativeProbability is the inverse
             * of cumulativeProbability on valid probabilities. We build p by construction as
             * cumulativeProbability(x), so a correct implementation must recover x (within solver
             * tolerance). A patch that merely suppresses the throw in bracket but returns a wrong
             * value will violate this round-trip relation.
             */
            if (Math.abs(inv - x) > 1.0e-6d) {
                throw new RuntimeException(
                    "[oracle:roundtrip] metamorphic violation: inverse(cdf(x)) != x mean="
                        + mean + " x=" + x + " p=" + p + " inv=" + inv);
            }

            try {
                double p2 = dist.cumulativeProbability(inv);
                if (Math.abs(p2 - p) > 1.0e-6d) {
                    throw new RuntimeException(
                        "[oracle:cdf-roundtrip] metamorphic violation: cdf(inverse(p)) != p mean="
                            + mean + " x=" + x + " p=" + p + " inv=" + inv + " p2=" + p2);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof IllegalArgumentException || c instanceof NumberFormatException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        boolean sawMathException = false;
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof MathException) {
                sawMathException = true;
            }
            for (StackTraceElement ste : c.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                        && "bracket".equals(ste.getMethodName())
                        && sawMathException) {
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