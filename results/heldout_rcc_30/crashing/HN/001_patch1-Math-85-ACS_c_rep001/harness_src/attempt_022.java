package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double ANCHOR_X = 2.0d;
    private static final double TOL = 1.0e-12;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0.0d, 1.0d);

        try {
            double anchor = normal.inverseCumulativeProbability(ANCHOR_P);
            if (!approximatelyEqual(anchor, ANCHOR_X, TOL)) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability must recover the test's documented result input="
                        + ANCHOR_P + " lhs=" + anchor + " rhs=" + ANCHOR_X);
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
            return;
        }

        int mode = data.consumeInt(0, 2);
        if (mode == 0) {
            int x = data.consumeInt(-8, 8);
            try {
                double p = normal.cumulativeProbability((double) x);
                double inv = normal.inverseCumulativeProbability(p);
                /* Contract asserted:
                 * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
                 * We construct p by calling the real cumulativeProbability on a known finite x, so p is valid by construction.
                 * For a correct implementation, inverseCumulativeProbability(cumulativeProbability(x)) must recover x.
                 * A patch that merely deletes/suppresses the throw in bracket but returns a wrong value would violate this.
                 */
                if (!approximatelyEqual(inv, (double) x, TOL)) {
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) == x input="
                            + x + " lhs=" + inv + " rhs=" + x);
                }
            } catch (Throwable t) {
                handleThrowable(t, true);
            }
            return;
        }

        if (mode == 1) {
            int x1 = data.consumeInt(-8, 8);
            int x2 = data.consumeInt(-8, 8);
            int xLo = Math.min(x1, x2);
            int xHi = Math.max(x1, x2);
            try {
                double pLo = normal.cumulativeProbability((double) xLo);
                double pHi = normal.cumulativeProbability((double) xHi);
                double qLo = normal.inverseCumulativeProbability(pLo);
                double qHi = normal.inverseCumulativeProbability(pHi);
                /* Contract asserted:
                 * cumulativeProbability is monotone, and inverseCumulativeProbability is its inverse on valid probabilities.
                 * For xLo <= xHi, the recovered quantiles must satisfy qLo <= qHi.
                 * This uses only real library calls on valid probabilities constructed by cumulativeProbability itself.
                 */
                if (qLo > qHi + TOL) {
                    throw new RuntimeException("[oracle:monotone] metamorphic violation: inverse results must preserve order input=("
                            + xLo + "," + xHi + ") lhs=(" + qLo + "," + qHi + ") rhs=ordered");
                }
            } catch (Throwable t) {
                handleThrowable(t, true);
            }
            return;
        }

        int x = data.consumeInt(-8, 8);
        boolean useAnchor = data.consumeBoolean();
        try {
            double p = useAnchor ? ANCHOR_P : normal.cumulativeProbability((double) x);
            double inv1 = normal.inverseCumulativeProbability(p);
            double inv2 = normal.inverseCumulativeProbability(p);
            /* Contract asserted:
             * inverseCumulativeProbability is a pure query on the distribution; repeating the same valid call must return the same result.
             * If a patch makes the buggy branch unreachable or silently changes bookkeeping/output, repeated calls on the same input must still agree.
             */
            if (!approximatelyEqual(inv1, inv2, TOL)) {
                throw new RuntimeException("[oracle:idempotent] metamorphic violation: repeated inverseCumulativeProbability calls must agree input="
                        + p + " lhs=" + inv1 + " rhs=" + inv2);
            }
        } catch (Throwable t) {
            handleThrowable(t, true);
        }
    }

    private static void handleThrowable(Throwable t, boolean validByConstruction) {
        if (isOracleFailure(t)) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (validByConstruction && isGroundTruthRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isOracleFailure(Throwable t) {
        return t instanceof RuntimeException
                && t.getMessage() != null
                && t.getMessage().startsWith("[oracle:");
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

    private static boolean isGroundTruthRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasBracketInChain(t);
    }

    private static boolean hasBracketInChain(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            StackTraceElement[] stack = cur.getStackTrace();
            if (stack == null) {
                continue;
            }
            for (int i = 0; i < stack.length; i++) {
                StackTraceElement e = stack[i];
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                        && "bracket".equals(e.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean approximatelyEqual(double a, double b, double tol) {
        return Math.abs(a - b) <= tol;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}