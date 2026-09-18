package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);

        // ANCHOR: exact failing test input from NormalDistributionTest.testMath280.
        try {
            double anchored = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Math.abs(anchored - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: inverseCumulativeProbability(CDF(2)) must be 2 input=0.9772498680518209 lhs=" + anchored + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }

        // EXPLORE:
        // Valid-by-construction inputs only: build p from the real library's CDF at an integer x.
        // Contract/oracle: inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
        // A patch that merely deletes the throw or returns a wrong endpoint would violate this round-trip.
        int x = data.consumeInt(-8, 8);
        if (data.consumeBoolean()) {
            x = 2;
        }

        try {
            double p = normal.cumulativeProbability((double) x);
            double inv = normal.inverseCumulativeProbability(p);
            if (Math.abs(inv - (double) x) > 1.0e-12d) {
                throw new RuntimeException("[oracle:normal-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x input=" + x + " p=" + p + " lhs=" + inv + " rhs=" + x);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }

        // Additional varied but still valid construction: use a second library-derived probability.
        try {
            int y = data.consumeInt(-8, 8);
            double p2 = normal.cumulativeProbability((double) y);
            double inv2 = normal.inverseCumulativeProbability(p2);
            if (Math.abs(inv2 - (double) y) > 1.0e-12d) {
                throw new RuntimeException("[oracle:normal-roundtrip-2] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(y)) must recover y input=" + y + " p=" + p2 + " lhs=" + inv2 + " rhs=" + y);
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
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (int i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
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
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        if (name != null) {
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