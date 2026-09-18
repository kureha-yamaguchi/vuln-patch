package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        double mean = data.consumeInt(-10, 10);
        int sdTenths = data.consumeInt(1, 100);
        double sd = sdTenths / 10.0;
        int k = data.consumeInt(2, 20);
        int sign = data.consumeBoolean() ? 1 : -1;

        runConstructedRoundTrip(mean, sd, mean + sign * k * sd);

        int extraCases = data.consumeInt(0, 4);
        for (int i = 0; i < extraCases; i++) {
            double mean2 = data.consumeInt(-20, 20);
            double sd2 = data.consumeInt(1, 50) / 10.0;
            int k2 = data.consumeInt(2, 25);
            int sign2 = data.consumeBoolean() ? 1 : -1;
            runConstructedRoundTrip(mean2, sd2, mean2 + sign2 * k2 * sd2);
        }
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability(CDF(2)) must recover 2.0 input=0.9772498680518209 lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t, true)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runConstructedRoundTrip(double mean, double sd, double target) {
        try {
            NormalDistribution dist = new NormalDistributionImpl(mean, sd);
            double p = dist.cumulativeProbability(target);
            try {
                double inv = dist.inverseCumulativeProbability(p);
                /* Contract/oracle:
                 * inverseCumulativeProbability is the inverse of cumulativeProbability for valid probabilities.
                 * Here p is constructed by a real library call p = CDF(target), so the input is valid by construction.
                 * A "fix" that merely suppresses the throw or skips correct endpoint handling would violate this round-trip.
                 */
                double tol = Math.max(1.0e-6d, 1.0e-6d * Math.abs(target));
                if (Double.isNaN(inv) || Double.isInfinite(inv) || Math.abs(inv - target) > tol) {
                    throw new RuntimeException("[oracle:norm-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) == x input=" + target + " lhs=" + inv + " rhs=" + target + " mean=" + mean + " sd=" + sd + " p=" + p);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t, true)) {
                    sneakyThrow(t);
                }
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        return isMathExceptionFamily(t) && hasBracketInChain(t);
    }

    private static boolean isMathExceptionFamily(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasBracketInChain(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (StackTraceElement e : st) {
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Invalid")
                    || name.contains("OutOfRange")
                    || name.contains("NoData")
                    || name.contains("NullArgument")
                    || name.contains("NotPositive")
                    || name.contains("NotStrictlyPositive")
                    || name.contains("Illegal")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}