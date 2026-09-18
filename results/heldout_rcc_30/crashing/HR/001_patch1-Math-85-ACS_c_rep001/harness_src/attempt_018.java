package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runRegressionAnchor();
        probeEndpointRootAcceptance(data);
        probeValidationBuilders(data);
    }

    private static void runRegressionAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            normal.inverseCumulativeProbability(0.9772498680518209);
        } catch (Throwable t) {
            if (isMathRootCauseFromBracket(t)) {
                return;
            }
        }
    }

    private static void probeEndpointRootAcceptance(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        double initial = 1.0;
        double lower = 0.0;
        double upper = 2.0 + Math.abs(data.consumeInt(0, 8));
        int maxIterations = 1 + data.consumeInt(0, 16);

        try {
            double[] interval = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, maxIterations);

            if (interval == null || interval.length != 2) {
                throw new RuntimeException("[oracle:sin-endpoint-accept] metamorphic violation: bracket must return two endpoints");
            }

            double a = interval[0];
            double b = interval[1];
            double fa = f.value(a);
            double fb = f.value(b);

            /* bracket's contract is to return an interval bracketing a root; endpoint roots are valid,
               so for this valid-by-construction input we must have fa * fb <= 0. A throw-deleting or
               wrong-return patch would violate this observable post-condition. */
            if (!(fa * fb <= 0.0)) {
                throw new RuntimeException(
                    "[oracle:sin-endpoint-accept] metamorphic violation: returned interval does not bracket a root"
                        + " a=" + a + " b=" + b + " fa=" + fa + " fb=" + fb);
            }

            if (Math.abs(Math.sin(a)) > 1e-15 && Math.abs(Math.sin(b)) > 1e-15 && !(fa * fb < 0.0)) {
                throw new RuntimeException(
                    "[oracle:sin-endpoint-accept] metamorphic violation: endpoint root or opposite signs required"
                        + " a=" + a + " b=" + b + " fa=" + fa + " fb=" + fb);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (hasBracketFrame(t)) {
                throw new RuntimeException(
                    "[oracle:sin-endpoint-accept] metamorphic violation: valid endpoint-root input must be accepted via bracket, got "
                        + t.getClass().getName(),
                    t);
            }
        }
    }

    private static void probeValidationBuilders(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        try {
            UnivariateRealSolverUtils.bracket(null, 1.0, 0.0, 2.0, 1 + data.consumeInt(0, 4));
            throw new RuntimeException("[oracle:null-function-reject] metamorphic violation: null function was accepted");
        } catch (Throwable t) {
            if (!isCleanRejection(t)) {
                if (hasMathRuntimeBuilderFrame(t) || hasBracketFrame(t)) {
                    throw new RuntimeException(
                        "[oracle:null-function-reject] metamorphic violation: null function must reject with validation exception family",
                        t);
                }
            }
        }

        try {
            UnivariateRealSolverUtils.bracket(f, 1.0, 0.0, 2.0, 0);
            throw new RuntimeException("[oracle:iter-count-reject] metamorphic violation: non-positive iteration count was accepted");
        } catch (Throwable t) {
            if (!isCleanRejection(t)) {
                if (hasMathRuntimeBuilderFrame(t) || hasBracketFrame(t)) {
                    throw new RuntimeException(
                        "[oracle:iter-count-reject] metamorphic violation: bad iteration count must reject with validation exception family",
                        t);
                }
            }
        }

        double x = data.consumeInt(-4, 4);
        double y = x + (data.consumeBoolean() ? 0.0 : -1.0);
        try {
            UnivariateRealSolverUtils.bracket(f, x, x, y, 1 + data.consumeInt(0, 4));
            throw new RuntimeException("[oracle:bounds-reject] metamorphic violation: invalid bracketing parameters were accepted");
        } catch (Throwable t) {
            if (!isCleanRejection(t)) {
                if (hasMathRuntimeBuilderFrame(t) || hasBracketFrame(t)) {
                    throw new RuntimeException(
                        "[oracle:bounds-reject] metamorphic violation: invalid bounds must reject with validation exception family",
                        t);
                }
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.startsWith("org.apache.commons.math") &&
                (name.contains("Illegal") || name.contains("Invalid") || name.contains("Argument"))) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isMathRootCauseFromBracket(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && (hasBracketFrame(cur) || hasInverseCdfFrame(cur))) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasBracketFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                && "bracket".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInverseCdfFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(ste.getClassName())
                && "inverseCumulativeProbability".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMathRuntimeBuilderFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.MathRuntimeException".equals(ste.getClassName())) {
                String m = ste.getMethodName();
                if ("createIllegalArgumentException".equals(m) || "buildMessage".equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }
}