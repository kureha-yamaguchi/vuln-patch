package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runOneCase(0.0, 2.0, 1.0e-12, true);

        int cases = 1 + data.consumeInt(0, 4);
        for (int i = 0; i < cases; i++) {
            int meanInt = data.consumeInt(-1000, 1000);
            int offset = data.consumeInt(1, 20);
            boolean positiveSide = data.consumeBoolean();

            double mean = (double) meanInt;
            double x = positiveSide ? mean + offset : mean - offset;

            runOneCase(mean, x, 1.0e-9, false);
        }
    }

    private static void runOneCase(double mean, double x, double tol, boolean anchor) {
        try {
            NormalDistribution normal = new NormalDistributionImpl(mean, 1.0);
            double p = normal.cumulativeProbability(x);
            double inv = normal.inverseCumulativeProbability(p);

            /*
             * Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid inputs.
             * We construct valid inputs by choosing a real x, computing p = CDF(x) from the same
             * real library object, then requiring inverseCumulativeProbability(p) ~= x.
             * A patch that merely suppresses the buggy throw but returns the wrong endpoint/value
             * violates this observable post-condition.
             */
            if (Double.isNaN(inv) || Math.abs(inv - x) > tol) {
                throw new RuntimeException(
                    "[oracle:normal-inverse-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) != x input="
                    + "mean=" + mean + ",x=" + x + ",p=" + p + " lhs=" + inv + " rhs=" + x
                );
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && t.getMessage() != null
                    && t.getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                sneakyThrow(t);
            }
            if (anchor) {
                return;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("IllegalArgument") || name.contains("Invalid")
                    || name.contains("OutOfRange") || name.contains("NoData")
                    || name.contains("NotStrictlyPositive") || name.contains("NullArgument")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        boolean sawMathException = false;
        boolean sawBracket = false;

        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                sawMathException = true;
            }
            for (StackTraceElement ste : cur.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                        && "bracket".equals(ste.getMethodName())) {
                    sawBracket = true;
                }
            }
        }

        return sawMathException && sawBracket;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}