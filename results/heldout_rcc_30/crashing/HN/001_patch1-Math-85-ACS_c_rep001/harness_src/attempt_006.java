package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
            double p = 0.9772498680518209d;
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: inverseCumulativeProbability(cdf(2)) must return 2 for the standard normal input from the regression test input=0.9772498680518209 lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        double mean = boundedDouble(data.consumeInt(), -100.0d, 100.0d);
        double sd = boundedPositiveScale(data.consumeInt());
        int sign = data.consumeBoolean() ? 1 : -1;
        int multiplier = data.consumeInt(1, 6);

        double target = mean + sign * 2.0d * sd;
        if (multiplier > 2) {
            target = mean + sign * ((double) multiplier) * sd;
        }

        try {
            NormalDistribution dist = new NormalDistributionImpl(mean, sd);

            double p = dist.cumulativeProbability(target);

            if (!(p >= 0.0d && p <= 1.0d) || Double.isNaN(p)) {
                return;
            }

            double inv = dist.inverseCumulativeProbability(p);

            /* Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * We construct p from the same real distribution as p = CDF(target), so a correct implementation
             * is obliged to accept it and recover target (up to numerical tolerance). A patch that merely
             * deletes the throw or skips the bracketing logic can silently return a wrong value, which this
             * round-trip oracle catches.
             */
            double tol = Math.max(1.0e-12d, Math.abs(target) * 1.0e-9d);
            if (Math.abs(inv - target) > tol) {
                throw new RuntimeException("[oracle:cdf-icdf-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x for a valid NormalDistribution input=x mean=" + mean + " sd=" + sd + " p=" + p + " lhs=" + inv + " rhs=" + target);
            }

            double p2 = dist.cumulativeProbability(inv);
            double probTol = 1.0e-10d;
            if (Math.abs(p2 - p) > probTol) {
                throw new RuntimeException("[oracle:probability-roundtrip] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) must recover p for valid p mean=" + mean + " sd=" + sd + " p=" + p + " lhs=" + p2 + " rhs=" + p);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasFrameInChain(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket");
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("IllegalArgument")
                    || name.contains("Invalid")
                    || name.contains("OutOfRange")
                    || name.contains("NoData")
                    || name.contains("NotPositive")
                    || name.contains("NotStrictlyPositive")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasFrameInChain(Throwable t, String className, String methodName) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if (className.equals(e.getClassName()) && methodName.equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static double boundedDouble(int raw, double min, double max) {
        long unsigned = raw & 0xffffffffL;
        double fraction = unsigned / (double) 0xffffffffL;
        return min + (max - min) * fraction;
    }

    private static double boundedPositiveScale(int raw) {
        long unsigned = raw & 0xffffffffL;
        double fraction = unsigned / (double) 0xffffffffL;
        return 0.1d + 49.9d * fraction;
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}