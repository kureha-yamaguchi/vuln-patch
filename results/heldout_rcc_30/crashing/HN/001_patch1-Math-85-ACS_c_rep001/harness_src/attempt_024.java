package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        double mean = (double) data.consumeInt(-1000, 1000);
        double sd = 1.0;

        try {
            NormalDistribution normal = new NormalDistributionImpl(mean, sd);

            int shape = data.consumeInt(0, 4);
            double x;
            if (shape == 0) {
                x = mean + 2.0;
            } else if (shape == 1) {
                x = mean;
            } else if (shape == 2) {
                x = mean - 2.0;
            } else if (shape == 3) {
                x = mean + data.consumeInt(-10, 10);
            } else {
                x = mean + (data.consumeBoolean() ? 2.0 : -2.0);
            }

            double p;
            try {
                p = normal.cumulativeProbability(x);
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwAsUnchecked(t);
                }
                return;
            }

            try {
                double inv = normal.inverseCumulativeProbability(p);

                /*
                 * Contract asserted: inverseCumulativeProbability(p) returns a point whose
                 * cumulative probability is p; therefore for p constructed as
                 * cumulativeProbability(x) on a real NormalDistribution, the round-trip
                 * inverseCumulativeProbability(cumulativeProbability(x)) must recover x
                 * (within the test tolerance 1E-6 used by NormalDistributionTest).
                 * A patch that merely deletes/suppresses the throw but returns a wrong value
                 * will violate this observable API-level relation.
                 */
                if (!approximatelyEqual(inv, x, 1.0e-6)) {
                    throw new RuntimeException(
                        "[oracle:norm-roundtrip] metamorphic violation: inverseCumulativeProbability(cumulativeProbability(x)) ~= x input="
                            + x + " lhs=" + inv + " rhs=" + x + " mean=" + mean + " p=" + p);
                }
            } catch (Throwable t) {
                if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
                    throw (RuntimeException) t;
                }
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCause(t)) {
                    throwAsUnchecked(t);
                }
                return;
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwAsUnchecked(t);
            }
        }
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0.0, 1.0);
            double result = normal.inverseCumulativeProbability(0.9772498680518209);
            if (!approximatelyEqual(result, 2.0, 1.0e-12)) {
                throw new RuntimeException(
                    "[oracle:anchor-exact] metamorphic violation: failing-test seed should recover x=2.0 input=0.9772498680518209 lhs="
                        + result + " rhs=2.0");
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
                throw (RuntimeException) t;
            }
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCause(t)) {
                throwAsUnchecked(t);
            }
        }
    }

    private static boolean approximatelyEqual(double a, double b, double tol) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return false;
        }
        if (Double.isInfinite(a) || Double.isInfinite(b)) {
            return a == b;
        }
        return Math.abs(a - b) <= tol;
    }

    private static boolean isOracleViolation(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.indexOf("[oracle:") >= 0;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.indexOf("IllegalArgument") >= 0
                    || name.indexOf("Invalid") >= 0
                    || name.indexOf("OutOfRange") >= 0
                    || name.indexOf("NoData") >= 0
                    || name.indexOf("NullArgument") >= 0) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isRootCause(Throwable t) {
        boolean sawMathException = false;
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException) {
                sawMathException = true;
            }
            StackTraceElement[] st = cur.getStackTrace();
            if (st != null) {
                int i;
                for (i = 0; i < st.length; i++) {
                    StackTraceElement e = st[i];
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                            && "bracket".equals(e.getMethodName())) {
                        return sawMathException;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static void throwAsUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }
}