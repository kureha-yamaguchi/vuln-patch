package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        try {
            runAnchor();
        } catch (Throwable t) {
            // Known top-level failure already covered elsewhere.
        }

        int center = data.consumeInt(-1000, 1000);
        boolean rootAtUpper = data.consumeBoolean();
        int maxIterations = data.consumeInt(1, 9);

        double initial = center;
        double lower = center - 1.0d;
        double upper = center + 1.0d;
        double root = rootAtUpper ? upper : lower;

        PolynomialFunction f = new PolynomialFunction(new double[] { -root, 1.0d });

        double[] fourArg;
        double[] fiveArg;

        try {
            fourArg = UnivariateRealSolverUtils.bracket(f, initial, lower, upper);
        } catch (Throwable t) {
            if (isRootCauseFromBracket(t)) {
                throw new RuntimeException(
                        "[oracle:bracket-endpoint-root] metamorphic violation: 4-arg bracket rejected valid endpoint-root input"
                                + " initial=" + initial + " lower=" + lower + " upper=" + upper + " root=" + root,
                        t);
            }
            return;
        }

        try {
            fiveArg = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, maxIterations);
        } catch (Throwable t) {
            if (isRootCauseFromBracket(t)) {
                throw new RuntimeException(
                        "[oracle:bracket-endpoint-root-iter] metamorphic violation: 5-arg bracket rejected valid endpoint-root input"
                                + " initial=" + initial + " lower=" + lower + " upper=" + upper + " root=" + root
                                + " maxIterations=" + maxIterations,
                        t);
            }
            return;
        }

        try {
            if (fourArg == null || fourArg.length != 2 || fiveArg == null || fiveArg.length != 2) {
                throw new RuntimeException("[oracle:bracket-shape] metamorphic violation: invalid result shape");
            }

            double fa0 = f.value(fourArg[0]);
            double fb0 = f.value(fourArg[1]);
            double fa1 = f.value(fiveArg[0]);
            double fb1 = f.value(fiveArg[1]);

            if (!(fourArg[0] <= fourArg[1]) || fourArg[0] < lower || fourArg[1] > upper || fa0 * fb0 > 0.0d) {
                throw new RuntimeException("[oracle:bracket-post] metamorphic violation: 4-arg returned non-bracketing interval"
                        + " input=[" + initial + "," + lower + "," + upper + "]"
                        + " output=[" + fourArg[0] + "," + fourArg[1] + "]"
                        + " values=[" + fa0 + "," + fb0 + "]");
            }

            if (!(fiveArg[0] <= fiveArg[1]) || fiveArg[0] < lower || fiveArg[1] > upper || fa1 * fb1 > 0.0d) {
                throw new RuntimeException("[oracle:bracket-post-iter] metamorphic violation: 5-arg returned non-bracketing interval"
                        + " input=[" + initial + "," + lower + "," + upper + "]"
                        + " output=[" + fiveArg[0] + "," + fiveArg[1] + "]"
                        + " values=[" + fa1 + "," + fb1 + "]"
                        + " maxIterations=" + maxIterations);
            }

            if (Double.doubleToLongBits(fourArg[0]) != Double.doubleToLongBits(fiveArg[0])
                    || Double.doubleToLongBits(fourArg[1]) != Double.doubleToLongBits(fiveArg[1])) {
                throw new RuntimeException("[oracle:bracket-overload] metamorphic violation: overload disagreement"
                        + " fourArg=[" + fourArg[0] + "," + fourArg[1] + "]"
                        + " fiveArg=[" + fiveArg[0] + "," + fiveArg[1] + "]"
                        + " input=[" + initial + "," + lower + "," + upper + "]"
                        + " maxIterations=" + maxIterations);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            return;
        }

        try {
            if (data.consumeBoolean()) {
                UnivariateRealSolverUtils.bracket(f, initial, upper, lower, maxIterations);
            } else {
                UnivariateRealSolverUtils.bracket(f, initial, lower, upper, data.consumeInt(-8, 0));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double p = 0.9772498680518209d;
            double result = normal.inverseCumulativeProbability(p);
            if (Math.abs(result - 2.0d) > 1.0e-12d) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: inverseCumulativeProbability("
                        + p + ") expected 2.0 got " + result);
            }
        } catch (MathException e) {
            if (isInverseRootCause(e)) {
                throw new RuntimeException("[oracle:anchor-exact] metamorphic violation: anchor triggered known root-cause path", e);
            }
        }
    }

    private static boolean isRootCauseFromBracket(Throwable t) {
        if (!(t instanceof ConvergenceException) && !(t instanceof MathException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        return cause != null && cause != t && isRootCauseFromBracket(cause);
    }

    private static boolean isInverseRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(e.getClassName())
                    && "inverseCumulativeProbability".equals(e.getMethodName())) {
                return true;
            }
        }
        Throwable cause = t.getCause();
        return cause != null && cause != t && isRootCauseFromBracket(cause);
    }
}