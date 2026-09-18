package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);

        // ANCHOR: exact regression input from NormalDistributionTest.testMath280.
        try {
            double p = 0.9772498680518209d;
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: regression input must invert to x=2.0 input=" + p + " lhs=" + result + " rhs=2.0");
            }

            // Contract/oracle: inverseCumulativeProbability is the inverse of cumulativeProbability
            // on valid probabilities; deleting the buggy throw must still return a value whose CDF
            // maps back to the original probability.
            double roundTrip = normal.cumulativeProbability(result);
            if (Math.abs(roundTrip - p) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: CDF(ICDF(p)) == p for valid p input=" + p + " lhs=" + roundTrip + " rhs=" + p);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // EXPLORE: build valid probabilities by construction using the real CDF on moderate integer x.
        // This targets the root cause property: probabilities that correspond exactly to a root at an
        // endpoint during bracketing (fa * fb == 0). Using p = CDF(x) ensures p is valid and that a
        // correct implementation must accept it.
        int cases = 1 + data.consumeInt(1, 8);
        for (int i = 0; i < cases; i++) {
            int xInt = data.consumeInt(-6, 6);
            double x = (double) xInt;

            try {
                double p = normal.cumulativeProbability(x);
                double inv = normal.inverseCumulativeProbability(p);

                if (Math.abs(inv - x) > 1.0e-6d) {
                    throw new RuntimeException("[oracle:icdf-cdf-int] metamorphic violation: ICDF(CDF(x)) == x for valid x input=" + x + " lhs=" + inv + " rhs=" + x);
                }

                double p2 = normal.cumulativeProbability(inv);
                if (Math.abs(p2 - p) > 1.0e-12d) {
                    throw new RuntimeException("[oracle:cdf-icdf-prob] metamorphic violation: CDF(ICDF(p)) == p for valid p input=" + p + " lhs=" + p2 + " rhs=" + p);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwUnchecked(t);
                }
                return;
            }
        }

        // Additional varied valid-by-construction inputs near the anchor, still moderate magnitude.
        int numerator = data.consumeInt(-1000, 1000);
        double x = 2.0d + (numerator / 1000.0d);
        try {
            double p = normal.cumulativeProbability(x);
            double inv = normal.inverseCumulativeProbability(p);

            if (Math.abs(inv - x) > 1.0e-6d) {
                throw new RuntimeException("[oracle:near-anchor] metamorphic violation: ICDF(CDF(x)) == x for valid x near anchor input=" + x + " lhs=" + inv + " rhs=" + x);
            }

            double p2 = normal.cumulativeProbability(inv);
            if (Math.abs(p2 - p) > 1.0e-12d) {
                throw new RuntimeException("[oracle:near-anchor-prob] metamorphic violation: CDF(ICDF(p)) == p for valid p near anchor input=" + p + " lhs=" + p2 + " rhs=" + p);
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

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("IllegalArgument")
                    || name.contains("Invalid")
                    || name.contains("OutOfRange")
                    || name.contains("NoData")
                    || name.contains("NullArgument")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        boolean mathFamily = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                mathFamily = true;
                break;
            }
        }
        return mathFamily && hasBracketInChain(t);
    }

    private static boolean hasBracketInChain(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st == null) {
                continue;
            }
            for (int i = 0; i < st.length; i++) {
                StackTraceElement e = st[i];
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                        && "bracket".equals(e.getMethodName())) {
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