package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    private static final double ANCHOR_P = 0.9772498680518209d;
    private static final double TOL = 1.0e-12d;

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);

        try {
            double result = normal.inverseCumulativeProbability(ANCHOR_P);
            if (Math.abs(result - 2.0d) > TOL) {
                throw new RuntimeException("[oracle:anchor] metamorphic violation: inverseCumulativeProbability(0.9772498680518209) must equal 2.0 input=0.9772498680518209 lhs=" + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            handleThrowable(t);
        }

        int trials = 1 + data.consumeInt(0, 8);
        for (int i = 0; i < trials; i++) {
            int k = data.consumeInt(1, 6);
            if (data.consumeBoolean()) {
                k = 2;
            }

            try {
                double p = normal.cumulativeProbability((double) k);
                double inv = normal.inverseCumulativeProbability(p);

                if (Math.abs(inv - (double) k) > TOL) {
                    throw new RuntimeException("[oracle:roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) == x for the continuous normal distribution input=" + k + " lhs=" + inv + " rhs=" + k);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }

        if (data.remainingBytes() <= 0) {
            return;
        }

        int extraTrials = 1 + data.consumeInt(0, 6);
        for (int i = 0; i < extraTrials; i++) {
            int base = data.consumeInt(1, 6);
            int deltaNum = data.consumeInt(-1000, 1000);
            double x = base + (deltaNum / 1000.0d);

            try {
                double p = normal.cumulativeProbability(x);
                if (!(p > 0.0d && p < 1.0d)) {
                    continue;
                }
                double inv = normal.inverseCumulativeProbability(p);

                if (Math.abs(inv - x) > 1.0e-9d) {
                    throw new RuntimeException("[oracle:roundtrip-fuzz] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) should recover x for moderate x input=" + x + " lhs=" + inv + " rhs=" + x);
                }
            } catch (Throwable t) {
                handleThrowable(t);
            }
        }
    }

    private static void handleThrowable(Throwable t) {
        if (t == null) {
            return;
        }
        if (t instanceof RuntimeException && isOracleRuntime((RuntimeException) t)) {
            throw (RuntimeException) t;
        }
        if (isCleanRejection(t)) {
            return;
        }
        if (isRootCause(t)) {
            sneakyThrow(t);
        }
    }

    private static boolean isOracleRuntime(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isRootCause(Throwable t) {
        return (t instanceof MathException) && hasBracketInChain(t);
    }

    private static boolean hasBracketInChain(Throwable t) {
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
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("OutOfRange")
                || name.contains("NoData")
                || name.contains("NotPositive")
                || name.contains("NotStrictlyPositive");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}