package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        runAnchor();

        int mean = data.consumeInt(-8, 8);
        int offset = data.consumeInt(-6, 6);
        if (offset == 0) {
            offset = data.consumeBoolean() ? 2 : -2;
        }

        checkEndpointRoot(mean, offset);

        int extraChecks = 1 + data.consumeInt(0, 3);
        for (int i = 0; i < extraChecks; i++) {
            int m = data.consumeInt(-8, 8);
            int k = data.consumeInt(-6, 6);
            if (k == 0) {
                k = (i & 1) == 0 ? 1 : -1;
            }
            checkEndpointRoot(m, k);
        }
    }

    private static void runAnchor() {
        NormalDistributionImpl normal = new NormalDistributionImpl(0, 1);
        double p = 0.9772498680518209;
        try {
            double q = normal.inverseCumulativeProbability(p);
            if (Math.abs(q - 2.0) > 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:endpoint-root-anchor] metamorphic violation: inverseCumulativeProbability("
                        + p + ") should recover the documented quantile 2.0 but got " + q);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:endpoint-root-anchor] metamorphic violation: valid probability from the regression seed "
                        + "triggered the old endpoint-root bracketing bug",
                    t);
            }
        }
    }

    private static void checkEndpointRoot(int mean, int offset) {
        NormalDistributionImpl dist = new NormalDistributionImpl(mean, 1.0);
        double x = mean + offset;

        try {
            double p = dist.cumulativeProbability(x);
            if (!(p > 0.0 && p < 1.0)) {
                return;
            }

            double q;
            try {
                q = dist.inverseCumulativeProbability(p);
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throw new RuntimeException(
                        "[oracle:endpoint-root-family] metamorphic violation: for a normal distribution, "
                            + "inverseCumulativeProbability(cumulativeProbability(x)) must recover x for this valid x; "
                            + "the endpoint-root boundary still crashes at mean=" + mean + " x=" + x + " p=" + p,
                        t);
                }
                return;
            }

            /*
             * Contract used: inverseCumulativeProbability is the inverse of cumulativeProbability on valid
             * probabilities. We construct p by calling cumulativeProbability(x) on the same real library object,
             * so p is valid by construction. A throw-deleting patch that silently returns the wrong value would
             * violate this relation even if no exception occurs.
             */
            if (Math.abs(q - x) > 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:endpoint-root-family] metamorphic violation: inverseCumulativeProbability("
                        + "cumulativeProbability(x)) != x at the patched boundary"
                        + " mean=" + mean + " x=" + x + " p=" + p + " q=" + q);
            }
        } catch (MathException e) {
            if (isRootCause(e)) {
                throw new RuntimeException(
                    "[oracle:endpoint-root-family] metamorphic violation: validly constructed endpoint-root input "
                        + "should not fail"
                        + " mean=" + mean + " x=" + x,
                    e);
            }
        } catch (IllegalArgumentException e) {
            return;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
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
        boolean hasMathException = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                hasMathException = true;
            }
            if (hasBracketFrame(cur)) {
                return hasMathException;
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
}