package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double EPS = 1.0e-12d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        // ANCHOR: exact failing test first. On the buggy version this reaches
        // AbstractContinuousDistribution.inverseCumulativeProbability -> UnivariateRealSolverUtils.bracket
        // and throws MathException wrapping the faulty ConvergenceException.
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0d, 1.0d);
            double result = normal.inverseCumulativeProbability(ANCHOR_P);

            // Contract/oracle: the failing test itself establishes that this exact input
            // must return 2.0 for a correct implementation. A patch that merely suppresses
            // the throw or returns a wrong value is caught here.
            if (Math.abs(result - 2.0d) > EPS) {
                throw new RuntimeException(
                    "[oracle:anchor-val] metamorphic violation: exact regression input must recover x=2.0 input="
                        + ANCHOR_P + " lhs=" + result + " rhs=2.0");
            }

            // Contract/oracle: inverse CDF is the inverse of CDF on valid probabilities.
            // For p in (0,1), cumulativeProbability(inverseCumulativeProbability(p)) must recover p.
            double back = normal.cumulativeProbability(result);
            if (Math.abs(back - ANCHOR_P) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:cdf-inv-anchor] metamorphic violation: cdf(invCDF(p)) must equal p input="
                        + ANCHOR_P + " lhs=" + back + " rhs=" + ANCHOR_P);
            }
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

        // EXPLORE:
        // Root-cause property: choose p exactly as cumulativeProbability(x) for a standard
        // normal x that lies on the solver's integer-stepping bracket boundary. Then the real
        // root-finding path reaches bracket with fa * fb == 0, the condition changed by the patch.
        int magnitude = data.consumeInt(2, 20);
        boolean negative = data.consumeBoolean();
        int xInt = negative ? -magnitude : magnitude;
        double x = (double) xInt;

        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0d, 1.0d);
            double p = normal.cumulativeProbability(x);

            // Input is valid by construction: p comes from the real CDF of the same distribution,
            // so p is in (0,1) and inverseCumulativeProbability is obliged to accept it.
            double inv = normal.inverseCumulativeProbability(p);

            // Oracle from the input itself: because p was constructed as CDF(x), a correct inverse
            // must recover x. A throw-deleting patch that returns a nearby wrong endpoint breaks this.
            if (Math.abs(inv - x) > 1.0e-9d) {
                throw new RuntimeException(
                    "[oracle:recover-x] metamorphic violation: invCDF(CDF(x)) must recover x input="
                        + x + " lhs=" + inv + " rhs=" + x);
            }

            // Equivalent observable contract using only real library calls:
            // for valid p, cdf(invCDF(p)) must return p.
            double back = normal.cumulativeProbability(inv);
            if (Math.abs(back - p) > 1.0e-12d) {
                throw new RuntimeException(
                    "[oracle:cdf-inv] metamorphic violation: cdf(invCDF(p)) must equal p input="
                        + p + " lhs=" + back + " rhs=" + p);
            }
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
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
            || t instanceof NumberFormatException
            || containsClassNameFragment(t, "Illegal")
            || containsClassNameFragment(t, "Invalid");
    }

    private static boolean isRootCause(Throwable t) {
        if (t instanceof RuntimeException && t.getMessage() != null && t.getMessage().startsWith("[oracle:")) {
            return true;
        }
        if (!(t instanceof MathException)) {
            return false;
        }
        return stackContainsBracket(t);
    }

    private static boolean stackContainsBracket(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] trace = cur.getStackTrace();
            if (trace != null) {
                for (StackTraceElement e : trace) {
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

    private static boolean containsClassNameFragment(Throwable t, String fragment) {
        Throwable cur = t;
        while (cur != null) {
            String name = cur.getClass().getName();
            if (name != null && name.indexOf(fragment) >= 0) {
                return true;
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