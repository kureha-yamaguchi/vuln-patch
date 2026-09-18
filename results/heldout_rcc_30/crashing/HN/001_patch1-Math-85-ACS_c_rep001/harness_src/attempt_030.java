package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double TOL = 1.0e-12;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int mean = data.consumeInt(-100, 100);
        int mode = data.consumeInt(0, 3);

        double x;
        if (mode == 0) {
            x = mean + 2.0;
        } else if (mode == 1) {
            x = mean - 2.0;
        } else if (mode == 2) {
            x = mean + 2.0 + data.consumeInt(-3, 3);
        } else {
            x = mean - 2.0 + data.consumeInt(-3, 3);
        }

        NormalDistributionImpl dist = new NormalDistributionImpl(mean, 1.0);

        double p;
        try {
            p = dist.cumulativeProbability(x);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        try {
            double inv = dist.inverseCumulativeProbability(p);

            /*
             * Metamorphic contract: inverse cumulative probability is the inverse of
             * cumulative probability on valid probabilities. We construct p by calling
             * cumulativeProbability(x) on a real NormalDistributionImpl with positive
             * standard deviation, so p is valid by construction. A correct implementation
             * should therefore recover x (within tolerance). A "fix" that merely deletes
             * the throw or returns an arbitrary nearby value would violate this.
             */
            if (Double.isNaN(inv) || Math.abs(inv - x) > TOL) {
                throw new RuntimeException(
                    "[oracle:normal-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) == x input="
                        + x + " mean=" + mean + " p=" + p + " lhs=" + inv + " rhs=" + x);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static void runAnchor() {
        try {
            NormalDistributionImpl normal = new NormalDistributionImpl(0, 1);
            double result = normal.inverseCumulativeProbability(0.9772498680518209d);
            if (Double.isNaN(result) || Math.abs(result - 2.0d) > TOL) {
                throw new RuntimeException(
                    "[oracle:anchor-result] metamorphic violation: failing test must return 2.0 input=0.9772498680518209 lhs="
                        + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasBracketFrame(t);
    }

    private static boolean hasBracketFrame(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                for (StackTraceElement e : st) {
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