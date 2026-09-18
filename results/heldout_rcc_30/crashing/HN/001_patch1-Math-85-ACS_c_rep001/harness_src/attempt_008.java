package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);

        // ANCHOR: exact failing test input first.
        try {
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-eq2] metamorphic violation: exact failing test should recover x=2.0 input=0.9772498680518209 result=" + result);
            }
            // Contract/oracle: inverseCumulativeProbability(p) returns x such that cumulativeProbability(x) ~= p.
            // A patch that only deletes the throw but returns the wrong endpoint/value would violate this.
            try {
                double roundTrip = normal.cumulativeProbability(result);
                if (Math.abs(roundTrip - 0.9772498680518209d) > 1.0e-12d) {
                    throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: cdf(inv(p)) == p input=0.9772498680518209 inv=" + result + " roundTrip=" + roundTrip);
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
                return;
            }
        }

        // EXPLORE: construct valid probabilities by p = CDF(x) for moderate integer x.
        // Property: when x lands exactly on a bracket endpoint, fa*fb == 0 in bracket(), which the buggy code mishandles.
        int magnitude = data.consumeInt(2, 32);
        boolean negative = data.consumeBoolean();
        int offset = data.consumeInt(0, 3);
        double x = negative ? -(magnitude + offset) : (magnitude + offset);

        try {
            double p = normal.cumulativeProbability(x);
            if (!(p > 0.0d && p < 1.0d)) {
                return;
            }

            double inv;
            try {
                inv = normal.inverseCumulativeProbability(p);
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    sneakyThrow(t);
                    return;
                }
                return;
            }

            // Oracle from the input itself: for p constructed as CDF(x), inverseCumulativeProbability(p) should recover x.
            if (Math.abs(inv - x) > 1.0e-9d) {
                throw new RuntimeException("[oracle:inv-cdf] metamorphic violation: inv(cdf(x)) == x inputX=" + x + " p=" + p + " inv=" + inv);
            }

            // Equivalent contract phrasing: CDF(inverseCumulativeProbability(p)) ~= p for valid p in (0,1).
            double p2;
            try {
                p2 = normal.cumulativeProbability(inv);
            } catch (Throwable ignored) {
                return;
            }
            if (Math.abs(p2 - p) > 1.0e-12d) {
                throw new RuntimeException("[oracle:cdf-inv] metamorphic violation: cdf(inv(p)) == p inputX=" + x + " p=" + p + " inv=" + inv + " roundTrip=" + p2);
            }
        } catch (MathException ignored) {
            return;
        } catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.indexOf("Illegal") >= 0 || name.indexOf("Invalid") >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException && hasBracketFrame(cur)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBracketFrame(Throwable t) {
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
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