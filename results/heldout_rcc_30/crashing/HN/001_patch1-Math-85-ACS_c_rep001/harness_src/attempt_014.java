package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        double mean = data.consumeInt(-1000, 1000);
        double sd = data.consumeInt(1, 1000) / 10.0;
        int k = data.consumeInt(2, 50);

        runExplore(mean, sd, k);

        if (data.consumeBoolean()) {
            double mean2 = data.consumeInt(-1000, 1000);
            double sd2 = data.consumeInt(1, 1000) / 10.0;
            int k2 = data.consumeInt(2, 50);
            runExplore(mean2, sd2, k2);
        }
    }

    private static void runAnchor() {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
        double p = 0.9772498680518209;
        try {
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0) > 1.0e-12) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: exact regression result input=" + p + " lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void runExplore(double mean, double sd, int k) {
        NormalDistribution normal = new NormalDistributionImpl(mean, sd);
        try {
            double target = mean + (sd * k);
            double p = normal.cumulativeProbability(target);

            double recovered;
            try {
                recovered = normal.inverseCumulativeProbability(p);
            } catch (Throwable t) {
                handleThrowable(t);
                return;
            }

            /* Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability on valid probabilities.
             * We build p by construction from cumulativeProbability(target), so p is valid and accepted by any
             * correct implementation. A "fix" that merely suppresses the throw or skips proper bracketing can
             * return the wrong quantile; then this round-trip observable fails.
             */
            if (Math.abs(recovered - target) > 1.0e-9) {
                throw new RuntimeException("[oracle:normal-roundtrip] metamorphic violation: inverse(cdf(x)) != x input=mean=" + mean + ",sd=" + sd + ",k=" + k + ",p=" + p + " lhs=" + recovered + " rhs=" + target);
            }

            try {
                double p2 = normal.cumulativeProbability(recovered);
                if (Math.abs(p2 - p) > 1.0e-12) {
                    throw new RuntimeException("[oracle:cdf-inverse-cdf] metamorphic violation: cdf(inverse(p)) != p input=mean=" + mean + ",sd=" + sd + ",k=" + k + ",p=" + p + " lhs=" + p2 + " rhs=" + p);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t instanceof RuntimeException) {
            String msg = t.getMessage();
            if (msg != null && msg.startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }

        if (isCleanRejection(t)) {
            return;
        }

        if (isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("NotStrictlyPositive")
                    || name.contains("OutOfRange")
                    || name.contains("Invalid")
                    || name.contains("NoData")
                    || name.contains("NullArgument")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        boolean mathExceptionFamily = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                mathExceptionFamily = true;
                break;
            }
        }
        return mathExceptionFamily && stackPassesThroughBracket(t);
    }

    private static boolean stackPassesThroughBracket(Throwable t) {
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
}