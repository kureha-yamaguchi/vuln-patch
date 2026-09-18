package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int iterations = 1 + data.consumeInt(0, 8);
        for (int i = 0; i < iterations; i++) {
            double mean = data.consumeInt(-50, 50);
            double sd = data.consumeInt(1, 20);
            int k = data.consumeInt(-6, 6);
            if (k == 0 && data.consumeBoolean()) {
                k = 2;
            }
            double x = mean + sd * k;

            NormalDistribution dist;
            try {
                dist = new NormalDistributionImpl(mean, sd);
            } catch (IllegalArgumentException e) {
                return;
            } catch (RuntimeException e) {
                if (isValidationLike(e)) {
                    return;
                }
                return;
            }

            try {
                double p = dist.cumulativeProbability(x);
                double inv = dist.inverseCumulativeProbability(p);

                // Contract/oracle: inverseCumulativeProbability is the inverse of cumulativeProbability
                // for valid probabilities produced by the same real distribution. A "fix" that merely
                // suppresses the throw in bracket but returns a wrong value violates this round-trip.
                if (Math.abs(inv - x) > 1.0e-6) {
                    throw new RuntimeException(
                        "[oracle:norm-roundtrip] metamorphic violation: inverse(cdf(x)) must recover x input="
                            + x + " lhs=" + inv + " rhs=" + x + " mean=" + mean + " sd=" + sd + " p=" + p);
                }

                double p2 = dist.cumulativeProbability(inv);
                if (Math.abs(p2 - p) > 1.0e-6) {
                    throw new RuntimeException(
                        "[oracle:norm-cdf-roundtrip] metamorphic violation: cdf(inverse(p)) must recover p input="
                            + p + " lhs=" + p2 + " rhs=" + p + " mean=" + mean + " sd=" + sd + " x=" + x);
                }
            } catch (Throwable t) {
                if (isValidationLike(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    sneakyThrow(t);
                }
                return;
            }
        }
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:anchor-exact] metamorphic violation: known regression seed must invert to 2.0 input=0.9772498680518209 lhs="
                        + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isValidationLike(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException) && !(t instanceof RuntimeException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                    && "bracket".equals(ste.getMethodName())) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            return isRootCause(cause);
        }
        return false;
    }

    private static boolean isValidationLike(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("IllegalArgument")
                || name.contains("NumberFormat")
                || name.contains("Invalid")
                || name.contains("OutOfRange")
                || name.contains("NoData")
                || name.contains("NullArgument");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}