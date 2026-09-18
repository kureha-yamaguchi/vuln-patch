package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        /* Post-condition asserted below:
         * For a real NormalDistribution, inverseCumulativeProbability(cumulativeProbability(x)) == x
         * for values x we construct ourselves. This round-trip uses only real library calls and
         * exercises the public API that reaches bracket(). A patch that merely suppresses the throw
         * but returns the wrong quantile violates this observable contract.
         */
        NormalDistribution normal = new NormalDistributionImpl(0, 1);

        // ANCHOR: exact failing test input first.
        try {
            double anchorP = 0.9772498680518209d;
            double anchorResult = normal.inverseCumulativeProbability(anchorP);
            if (Math.abs(anchorResult - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-roundtrip] metamorphic violation: inverseCumulativeProbability returned wrong value input=" + anchorP + " lhs=" + anchorResult + " rhs=2.0");
            }
        } catch (Throwable t) {
            boolean rootCause = false;
            if (t instanceof org.apache.commons.math.MathException) {
                Throwable c = t;
                while (c != null && !rootCause) {
                    StackTraceElement[] st = c.getStackTrace();
                    if (st != null) {
                        for (int i = 0; i < st.length; i++) {
                            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(st[i].getClassName())
                                    && "bracket".equals(st[i].getMethodName())) {
                                rootCause = true;
                                break;
                            }
                        }
                    }
                    c = c.getCause();
                }
            }
            if (rootCause || t instanceof RuntimeException) {
                throw (RuntimeException) (t instanceof RuntimeException ? t : new RuntimeException(t));
            }
            return;
        }

        // EXPLORE: construct many valid probabilities p = CDF(x) for integer x with |x| >= 2.
        // This targets the root cause property: the true root can land exactly on a bracket endpoint,
        // where buggy code treats fa*fb == 0 as failure instead of success.
        int trials = data.consumeInt(1, 8);
        for (int i = 0; i < trials; i++) {
            int sign = data.consumeBoolean() ? 1 : -1;
            int magnitude = data.consumeInt(2, 6);
            double x = sign * magnitude;

            double p;
            try {
                p = normal.cumulativeProbability(x);
            } catch (Throwable t) {
                // Any exception here means this input is not usable for the round-trip oracle.
                return;
            }

            try {
                double inv = normal.inverseCumulativeProbability(p);
                if (Math.abs(inv - x) > 1.0e-12d) {
                    throw new RuntimeException("[oracle:norm-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input=" + x + " lhs=" + inv + " rhs=" + x);
                }
            } catch (Throwable t) {
                boolean cleanRejection =
                        t instanceof IllegalArgumentException ||
                        t instanceof NumberFormatException;

                if (cleanRejection) {
                    return;
                }

                boolean rootCause = false;
                if (t instanceof org.apache.commons.math.MathException) {
                    Throwable c = t;
                    while (c != null && !rootCause) {
                        StackTraceElement[] st = c.getStackTrace();
                        if (st != null) {
                            for (int j = 0; j < st.length; j++) {
                                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(st[j].getClassName())
                                        && "bracket".equals(st[j].getMethodName())) {
                                    rootCause = true;
                                    break;
                                }
                            }
                        }
                        c = c.getCause();
                    }
                }

                if (rootCause || t instanceof RuntimeException) {
                    throw (RuntimeException) (t instanceof RuntimeException ? t : new RuntimeException(t));
                }
                return;
            }
        }
    }
}