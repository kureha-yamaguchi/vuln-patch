package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double ANCHOR_X = 2.0d;
    private static final double STRICT_TOLERANCE = 1.0e-12;
    private static final double ROUNDTRIP_TOLERANCE = 1.0e-9;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution standard = new NormalDistributionImpl(0.0d, 1.0d);

        try {
            double anchor = standard.inverseCumulativeProbability(ANCHOR_P);
            if (Math.abs(anchor - ANCHOR_X) > STRICT_TOLERANCE) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability(testSeed) must recover 2.0 input=" + ANCHOR_P + " lhs=" + anchor + " rhs=" + ANCHOR_X);
            }
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                throwUnchecked(t);
            }
            return;
        }

        int trials = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < trials; i++) {
            int endpoint = chooseEndpoint(data);
            tryRoundTrip(standard, endpoint);
        }

        int extraTrials = data.consumeInt(0, 3);
        for (int i = 0; i < extraTrials; i++) {
            double mean = data.consumeInt(-50, 50);
            double sigma = 1.0d + data.consumeInt(0, 20);
            int k = chooseEndpoint(data);
            NormalDistribution shifted = new NormalDistributionImpl(mean, sigma);
            double x = mean + (sigma * k);
            tryRoundTrip(shifted, x);
        }
    }

    private static int chooseEndpoint(FuzzedDataProvider data) {
        int k = data.consumeInt(-20, 20);
        if (k == 0 || k == 1) {
            k = 2;
        } else if (k == -1) {
            k = -2;
        }
        return k;
    }

    private static void tryRoundTrip(NormalDistribution dist, double x) {
        try {
            double p = dist.cumulativeProbability(x);
            if (!(p > 0.0d && p < 1.0d) || Double.isNaN(p)) {
                return;
            }

            double recovered = dist.inverseCumulativeProbability(p);

            /*
             * Contract/oracle: inverseCumulativeProbability is the inverse of cumulativeProbability
             * on valid probabilities. We construct p from the real library's cumulativeProbability(x),
             * so a correct implementation must recover x (within solver tolerance). A patch that merely
             * deletes or bypasses the throw in bracket but returns the wrong endpoint/value breaks this.
             */
            if (Math.abs(recovered - x) > ROUNDTRIP_TOLERANCE) {
                throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverse(cumulativeProbability(x)) must recover x input=" + x + " lhs=" + recovered + " rhs=" + x);
            }
        } catch (Throwable t) {
            if (shouldPropagate(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean shouldPropagate(Throwable t) {
        if (t instanceof RuntimeException && !(t instanceof IllegalArgumentException) && !(t instanceof NumberFormatException)) {
            String msg = t.getMessage();
            return msg != null && msg.startsWith("[oracle:");
        }

        if (isValidationFamily(t)) {
            return false;
        }

        return isMathExceptionFamily(t) && hasBracketInChain(t);
    }

    private static boolean isValidationFamily(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.endsWith("IllegalArgumentException")
                    || name.endsWith("InvalidRepresentationException")
                    || name.endsWith("InvalidMatrixException")
                    || name.endsWith("NotPositiveException")
                    || name.endsWith("NotStrictlyPositiveException")
                    || name.endsWith("OutOfRangeException")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMathExceptionFamily(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                return true;
            }
            String name = cur.getClass().getName();
            if ("org.apache.commons.math.MathException".equals(name)
                    || "org.apache.commons.math.ConvergenceException".equals(name)
                    || name.endsWith(".MathException")
                    || name.endsWith(".ConvergenceException")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBracketInChain(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] trace = cur.getStackTrace();
            if (trace == null) {
                continue;
            }
            for (int i = 0; i < trace.length; i++) {
                StackTraceElement ste = trace[i];
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                        && "bracket".equals(ste.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
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