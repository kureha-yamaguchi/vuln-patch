package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double STRICT_TOL = 1.0e-12d;
    private static final double ROUNDTRIP_TOL = 1.0e-6d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0.0d, 1.0d);

        try {
            double result = normal.inverseCumulativeProbability(ANCHOR_P);
            if (Math.abs(result - 2.0d) > STRICT_TOL) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability(cdf(2)) should recover 2.0 input=" + ANCHOR_P + " lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }

        int junkCount = data.consumeInt(0, 4);
        for (int i = 0; i < junkCount; i++) {
            if (data.consumeBoolean()) {
                data.consumeAsciiString(data.consumeInt(0, 32));
            } else {
                data.consumeBytes(data.consumeInt(0, 32));
            }
        }

        int k = data.consumeInt(2, 16);

        try {
            double p = normal.cumulativeProbability((double) k);
            double recovered = normal.inverseCumulativeProbability(p);

            /* Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability for valid probabilities.
             * We build p by construction as cumulativeProbability(k) from the same real NormalDistribution,
             * so p is valid and a correct implementation must recover k (within solver tolerance).
             * A patch that merely suppresses the throw but returns a wrong value violates this round-trip.
             */
            if (Math.abs(recovered - (double) k) > ROUNDTRIP_TOL) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) ~= x input=" + k + " lhs=" + recovered + " rhs=" + k);
            }

            if (data.consumeBoolean()) {
                double p2 = normal.cumulativeProbability((double) k);
                double recovered2 = normal.inverseCumulativeProbability(p2);
                if (Math.abs(recovered2 - recovered) > ROUNDTRIP_TOL) {
                    throw new RuntimeException("[oracle:idempotent-roundtrip] metamorphic violation: repeated round-trip on same distribution/input must agree input=" + k + " lhs=" + recovered + " rhs=" + recovered2);
                }
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
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Illegal") || name.contains("Invalid")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            return msg != null && msg.startsWith("[oracle:");
        }
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasFrameInChain(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket");
    }

    private static boolean hasFrameInChain(Throwable t, String className, String methodName) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] stack = cur.getStackTrace();
            if (stack != null) {
                for (StackTraceElement e : stack) {
                    if (className.equals(e.getClassName()) && methodName.equals(e.getMethodName())) {
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