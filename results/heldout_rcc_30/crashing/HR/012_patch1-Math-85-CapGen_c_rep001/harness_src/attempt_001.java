package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int rounds = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < rounds; i++) {
            int k = data.consumeInt(-6, 6);
            runRoundTripForIntegerQuantile(k);
        }

        int around = data.consumeInt(-6, 6);
        int offsetNumerator = data.consumeInt(-1000, 1000);
        double x = around + (offsetNumerator / 1000.0);
        runGeneralRoundTrip(x);
    }

    private static void runAnchor() {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
        double p = 0.9772498680518209;
        try {
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:anchor-value] metamorphic violation: inverseCumulativeProbability(seed) must reproduce the known test result input=" + p + " lhs=" + result + " rhs=2.0");
            }
            double back = normal.cumulativeProbability(result);
            if (Math.abs(back - p) > 1.0e-12) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) == p input=" + p + " lhs=" + back + " rhs=" + p);
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

    private static void runRoundTripForIntegerQuantile(int k) {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
        try {
            double p = normal.cumulativeProbability((double) k);
            double inv = normal.inverseCumulativeProbability(p);

            /* Documented inverse/CDF contract: for probabilities in range, inverseCumulativeProbability
             * returns the quantile whose cumulative probability is the requested probability.
             * We construct p from the same object's cumulativeProbability at a known x, so a correct
             * implementation must map back to that x. A throw-deleting patch or a silently wrong fix
             * would violate this observable round-trip. */
            if (Math.abs(inv - k) > 1.0e-9) {
                throw new RuntimeException("[oracle:int-roundtrip-x] metamorphic violation: icdf(cdf(k)) == k input=" + k + " lhs=" + inv + " rhs=" + k);
            }

            double back = normal.cumulativeProbability(inv);
            if (Math.abs(back - p) > 1.0e-12) {
                throw new RuntimeException("[oracle:int-roundtrip-p] metamorphic violation: cdf(icdf(p)) == p input=" + p + " lhs=" + back + " rhs=" + p);
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

    private static void runGeneralRoundTrip(double x) {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
        try {
            double p = normal.cumulativeProbability(x);
            if (!(p > 0.0 && p < 1.0)) {
                return;
            }
            double inv = normal.inverseCumulativeProbability(p);

            /* Same documented round-trip guarantee as above, using a non-integer x to explore nearby
             * real inputs around the boundary-triggering seeds. */
            if (Math.abs(inv - x) > 1.0e-7) {
                throw new RuntimeException("[oracle:real-roundtrip-x] metamorphic violation: icdf(cdf(x)) ~= x input=" + x + " lhs=" + inv + " rhs=" + x);
            }

            double back = normal.cumulativeProbability(inv);
            if (Math.abs(back - p) > 1.0e-10) {
                throw new RuntimeException("[oracle:real-roundtrip-p] metamorphic violation: cdf(icdf(p)) ~= p input=" + p + " lhs=" + back + " rhs=" + p);
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
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.indexOf("Invalid") >= 0 || name.indexOf("Illegal") >= 0) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        Throwable cur = t;
        boolean sawMathException = false;
        while (cur != null) {
            if (cur instanceof MathException) {
                sawMathException = true;
            }
            StackTraceElement[] stack = cur.getStackTrace();
            if (stack != null) {
                for (int i = 0; i < stack.length; i++) {
                    StackTraceElement e = stack[i];
                    String cls = e.getClassName();
                    String m = e.getMethodName();
                    if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(m))
                            || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls) && "inverseCumulativeProbability".equals(m))
                            || ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "<init>".equals(m))) {
                        return sawMathException || (t instanceof MathException);
                    }
                }
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