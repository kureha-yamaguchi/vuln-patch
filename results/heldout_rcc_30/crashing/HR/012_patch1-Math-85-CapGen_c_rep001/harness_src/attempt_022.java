package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int steps = data.consumeInt(1, 8);
        int extraChecks = data.consumeInt(1, 4);
        exploreBoundaryWindow(steps, extraChecks, data.consumeBoolean());
    }

    private static void runAnchor() {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        try {
            double result = normal.inverseCumulativeProbability(0.9772498680518209);
            if (Math.abs(result - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:seed-contract] metamorphic violation: failing-test seed must recover x=2.0 input=0.9772498680518209 result=" + result);
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

    private static void exploreBoundaryWindow(int steps, int extraChecks, boolean useNextUpFirst) {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        double x = 2.0;
        double p0;
        try {
            p0 = normal.cumulativeProbability(x);
        } catch (Throwable t) {
            return;
        }

        double pLo = p0;
        double pHi = p0;
        int i;
        for (i = 0; i < steps; i++) {
            pLo = Math.nextAfter(pLo, 0.0);
            pHi = Math.nextAfter(pHi, 1.0);
        }

        double q0;
        double qLo;
        double qHi;
        try {
            if (useNextUpFirst) {
                qHi = normal.inverseCumulativeProbability(pHi);
                q0 = normal.inverseCumulativeProbability(p0);
                qLo = normal.inverseCumulativeProbability(pLo);
            } else {
                qLo = normal.inverseCumulativeProbability(pLo);
                q0 = normal.inverseCumulativeProbability(p0);
                qHi = normal.inverseCumulativeProbability(pHi);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException("[oracle:ulp-window] metamorphic violation: valid probabilities around cdf(2.0) must not fail p0=" + p0 + " pLo=" + pLo + " pHi=" + pHi, t);
            }
            return;
        }

        /*
         * Soundness:
         * inverseCumulativeProbability is the inverse of cumulativeProbability on a continuous
         * distribution, so for pLo < p0 < pHi a correct implementation must preserve order:
         * qLo <= q0 <= qHi. Since p0 was constructed as cumulativeProbability(2.0), 2.0 must lie
         * between the quantiles for neighboring probabilities, and q0 must recover 2.0.
         * A throw-deleting or overfit patch that merely suppresses the old exception can still
         * return a wrong quantile here, so these post-conditions catch silent misbehavior.
         */
        if (!(qLo <= q0 && q0 <= qHi)) {
            throw new RuntimeException("[oracle:ulp-window] metamorphic violation: inverse CDF must be monotone pLo=" + pLo + " p0=" + p0 + " pHi=" + pHi + " qLo=" + qLo + " q0=" + q0 + " qHi=" + qHi);
        }
        if (!(qLo <= x && x <= qHi)) {
            throw new RuntimeException("[oracle:ulp-window] metamorphic violation: cdf-neighbor quantiles must bracket constructed x p0=" + p0 + " qLo=" + qLo + " x=" + x + " qHi=" + qHi);
        }
        if (Math.abs(q0 - x) > 1.0e-12) {
            throw new RuntimeException("[oracle:ulp-window] metamorphic violation: inverse(cdf(x)) must recover x x=" + x + " p0=" + p0 + " q0=" + q0);
        }

        for (i = 0; i < extraChecks; i++) {
            double probeP = (i % 2 == 0) ? pLo : pHi;
            try {
                double q = normal.inverseCumulativeProbability(probeP);
                double back = normal.cumulativeProbability(q);
                if (Math.abs(back - probeP) > 1.0e-12) {
                    throw new RuntimeException("[oracle:ulp-backproj] metamorphic violation: cdf(inverse(p)) must recover p p=" + probeP + " q=" + q + " back=" + back);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw new RuntimeException("[oracle:ulp-backproj] metamorphic violation: valid neighboring probability must be accepted p=" + probeP, t);
                }
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        int i;
        for (i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            String cls = e.getClassName();
            String method = e.getMethodName();
            if (("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls) && "bracket".equals(method))
                    || ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls) && "inverseCumulativeProbability".equals(method))
                    || ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(cls) && "value".equals(method))
                    || ("org.apache.commons.math.ConvergenceException".equals(cls) && "<init>".equals(method))
                    || ("org.apache.commons.math.MathRuntimeException".equals(cls) && "buildMessage".equals(method))
                    || ("org.apache.commons.math.MathRuntimeException".equals(cls) && "createIllegalArgumentException".equals(method))) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        return cause != null && cause != t && isRootCause(cause);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }
}