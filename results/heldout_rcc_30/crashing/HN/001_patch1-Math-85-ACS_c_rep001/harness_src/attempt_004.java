package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();
        runExplore(data);
    }

    private static void runAnchor() {
        try {
            NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
            double p = 0.9772498680518209d;
            double result = normal.inverseCumulativeProbability(p);

            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-inverse] metamorphic violation: inverseCumulativeProbability(cdf(2)) must recover 2.0 input=" + p + " lhs=" + result + " rhs=2.0");
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

    private static void runExplore(FuzzedDataProvider data) {
        double mean = data.consumeInt(-100, 100);
        double sd = data.consumeInt(1, 1000) / 10.0d;

        try {
            NormalDistributionImpl dist = new NormalDistributionImpl(mean, sd);

            double x;
            int mode = data.consumeInt(0, 3);
            if (mode == 0) {
                x = mean;
            } else if (mode == 1) {
                x = mean + 2.0d * sd;
            } else if (mode == 2) {
                x = mean - 2.0d * sd;
            } else {
                int k = data.consumeInt(-3, 3);
                x = mean + (2.0d * k) * sd;
            }

            double p = dist.cumulativeProbability(x);
            double inv = dist.inverseCumulativeProbability(p);

            /* Contract/oracle:
             * inverseCumulativeProbability is the inverse of cumulativeProbability.
             * For p constructed from the same distribution as p = cumulativeProbability(x),
             * a correct implementation must recover x (within numeric tolerance).
             * A patch that merely suppresses the bracket failure but returns a wrong value
             * would violate this observable post-condition without throwing.
             */
            double tol = 1.0e-6d * Math.max(1.0d, Math.abs(x));
            if (Double.isNaN(inv) || Math.abs(inv - x) > tol) {
                throw new RuntimeException("[oracle:roundtrip-normal] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) must recover x input=" + x + " p=" + p + " lhs=" + inv + " rhs=" + x);
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

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("IllegalArgument") || name.contains("Invalid") || name.contains("OutOfRange")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasBracketFrame(t);
    }

    private static boolean hasBracketFrame(Throwable t) {
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