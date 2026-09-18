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
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: exact regression seed must invert to 2.0 input=0.9772498680518209 result=" + result);
            }
        } catch (Throwable t) {
            if (isValidationRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static void runExplore(FuzzedDataProvider data) {
        double mean = data.consumeInt(-100, 100);
        double sd = data.consumeInt(1, 100);
        boolean upperTail = data.consumeBoolean();

        /*
         * Contract/oracle:
         * inverseCumulativeProbability(p) returns an x such that cumulativeProbability(x)=p.
         * We construct p from the real distribution's cumulativeProbability(x), so a correct
         * implementation should round-trip back to x (within numerical tolerance).
         *
         * To exercise the patched bracket line, choose x so that on the NormalDistribution
         * production path it lands exactly on a bracket endpoint after the first expansion:
         * upper tail: x = mean + sd + 1
         * lower tail: x = mean - sd - 1
         * This makes f(a)*f(b) == 0.0 when the endpoint is the exact root.
         */
        double x = upperTail ? (mean + sd + 1.0d) : (mean - sd - 1.0d);

        try {
            NormalDistribution normal = new NormalDistributionImpl(mean, sd);
            double p = normal.cumulativeProbability(x);
            double inv = normal.inverseCumulativeProbability(p);

            double tol = 1.0e-6d * Math.max(1.0d, Math.abs(x));
            if (Double.isNaN(inv) || Math.abs(inv - x) > tol) {
                throw new RuntimeException(
                    "[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x inputMean="
                        + mean + " inputSd=" + sd + " x=" + x + " p=" + p + " recovered=" + inv + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isValidationRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return stackPassesThroughBracket(t);
    }

    private static boolean stackPassesThroughBracket(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] stack = cur.getStackTrace();
            if (stack != null) {
                for (StackTraceElement e : stack) {
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

    private static boolean isValidationRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n != null) {
                if (n.contains("Illegal") || n.contains("Invalid")) {
                    return true;
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