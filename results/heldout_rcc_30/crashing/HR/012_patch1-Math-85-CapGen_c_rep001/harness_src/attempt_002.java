package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);

        runAnchor(normal);

        double anchorX = data.consumeBoolean() ? 2.0 : -2.0;
        double pEq;
        try {
            pEq = normal.cumulativeProbability(anchorX);
        } catch (Throwable t) {
            return;
        }

        int steps = data.consumeInt(0, 32);
        double pVar = pEq;
        boolean goUp = data.consumeBoolean();
        int i;
        for (i = 0; i < steps; i++) {
            double next = goUp ? Math.nextUp(pVar) : Math.nextAfter(pVar, 0.0);
            if (!(next > 0.0 && next < 1.0)) {
                break;
            }
            pVar = next;
            if (data.consumeBoolean()) {
                goUp = !goUp;
            }
        }

        checkBoundaryMonotonicity(normal, pEq, "mono-eq");
        checkBoundaryMonotonicity(normal, pVar, "mono-var");
        checkSymmetry(normal, pEq, "sym-eq");
        checkSymmetry(normal, pVar, "sym-var");
    }

    private static void runAnchor(NormalDistributionImpl normal) {
        final double p = 0.9772498680518209d;

        double x = inverseOrOracle(normal, p, "anchor");
        if (Double.isNaN(x)) {
            return;
        }

        double q = 1.0d - p;
        double y = inverseOrOracle(normal, q, "anchor-complement");
        if (Double.isNaN(y)) {
            return;
        }

        /* For the standard normal distribution, quantiles are antisymmetric:
         * inverseCumulativeProbability(1 - p) == -inverseCumulativeProbability(p).
         * A throw-deleting or overfit patch can avoid the old exception yet still return
         * an inconsistent quantile; this observable catches that. */
        if (Math.abs(x + y) > 1.0e-12) {
            throw new RuntimeException("[oracle:anchor-sym] metamorphic violation: inv(p) + inv(1-p) != 0 p=" + p + " x=" + x + " y=" + y);
        }
    }

    private static void checkBoundaryMonotonicity(NormalDistributionImpl normal, double p, String id) {
        if (!(p > 0.0 && p < 1.0)) {
            return;
        }

        double pLo = Math.nextAfter(p, 0.0);
        double pHi = Math.nextUp(p);
        if (!(pLo > 0.0 && pHi < 1.0 && pLo < p && p < pHi)) {
            return;
        }

        double xLo = inverseOrNaN(normal, pLo);
        double xHi = inverseOrNaN(normal, pHi);
        if (Double.isNaN(xLo) || Double.isNaN(xHi)) {
            return;
        }

        double xEq = inverseOrOracle(normal, p, id);
        if (Double.isNaN(xEq)) {
            return;
        }

        /* inverseCumulativeProbability is monotone in p for a continuous distribution:
         * if pLo < p < pHi then inv(pLo) <= inv(p) <= inv(pHi).
         * This specifically probes the patched fa*fb boundary by checking the exact
         * probability and its immediate floating-point neighbors. */
        if (xEq < xLo || xEq > xHi) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: monotonicity around boundary pLo=" + pLo + " p=" + p + " pHi=" + pHi + " xLo=" + xLo + " x=" + xEq + " xHi=" + xHi);
        }
    }

    private static void checkSymmetry(NormalDistributionImpl normal, double p, String id) {
        if (!(p > 0.0 && p < 1.0)) {
            return;
        }

        double q = 1.0d - p;
        if (!(q > 0.0 && q < 1.0)) {
            return;
        }

        double x = inverseOrOracle(normal, p, id + "-p");
        double y = inverseOrOracle(normal, q, id + "-q");
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return;
        }

        /* For NormalDistributionImpl(0,1), the distribution is symmetric about 0, so
         * inv(1-p) == -inv(p) for every valid probability. This is independent of the
         * old crash signature and still fires if a patch merely suppresses the throw. */
        if (Math.abs(x + y) > 1.0e-10) {
            throw new RuntimeException("[oracle:" + id + "] metamorphic violation: symmetry broken p=" + p + " q=" + q + " x=" + x + " y=" + y);
        }
    }

    private static double inverseOrNaN(NormalDistributionImpl normal, double p) {
        try {
            return normal.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static double inverseOrOracle(NormalDistributionImpl normal, double p, String id) {
        try {
            return normal.inverseCumulativeProbability(p);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:" + id + "] metamorphic violation: valid inverse rejected p=" + p, t);
            }
            return Double.NaN;
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        int i;
        for (i = 0; i < stack.length; i++) {
            String cls = stack[i].getClassName();
            String method = stack[i].getMethodName();
            if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(method))
                    || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls) && "inverseCumulativeProbability".equals(method))
                    || ("org.apache.commons.math.distribution.NormalDistributionImpl".equals(cls) && "inverseCumulativeProbability".equals(method))) {
                return true;
            }
        }
        return false;
    }
}