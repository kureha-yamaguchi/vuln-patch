package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int rounds = 1 + Math.max(0, Math.min(8, data.remainingBytes()));
        for (int i = 0; i < rounds; i++) {
            exploreOne(data);
        }
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double p = 0.9772498680518209d;
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: failing-test seed should recover x=2.0 input=" + p + " lhs=" + result + " rhs=2.0");
            }

            try {
                double roundTrip = normal.cumulativeProbability(result);
                if (Math.abs(roundTrip - p) > 1.0e-12d) {
                    throw new RuntimeException("[oracle:anchor-rt] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) must recover p for the known valid seed input=" + p + " lhs=" + roundTrip + " rhs=" + p);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void exploreOne(FuzzedDataProvider data) {
        int z = data.consumeInt(-8, 8);

        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        double p;
        try {
            p = normal.cumulativeProbability((double) z);
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        if (!(p > 0.0d && p < 1.0d)) {
            return;
        }

        double x;
        try {
            x = normal.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            handleThrowable(t);
            return;
        }

        try {
            double p2 = normal.cumulativeProbability(x);

            /* Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * Here p is built by construction as cumulativeProbability(z) for a real NormalDistribution,
             * so a correct implementation is obliged to accept it and recover the original quantile.
             * A patch that only suppresses the throw in bracket but returns a wrong value would violate
             * either x ~= z or cumulativeProbability(x) ~= p.
             */
            if (Math.abs(x - (double) z) > 1.0e-9d) {
                throw new RuntimeException("[oracle:quantile-recover] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(z)) must recover z input=" + z + " lhs=" + x + " rhs=" + z);
            }
            if (Math.abs(p2 - p) > 1.0e-9d) {
                throw new RuntimeException("[oracle:round-trip] metamorphic violation: cumulativeProbability(inverseCumulativeProbability(p)) must recover p input=" + p + " lhs=" + p2 + " rhs=" + p);
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
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
        return hasBracketInChain(t);
    }

    private static boolean hasBracketInChain(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] stack = cur.getStackTrace();
            if (stack != null) {
                for (StackTraceElement ste : stack) {
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                            && "bracket".equals(ste.getMethodName())) {
                        return true;
                    }
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