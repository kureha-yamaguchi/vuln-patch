package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorThroughPublicApi();

        exploreExactEndpointRootComposition(data);
    }

    private static void runAnchorThroughPublicApi() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            normal.inverseCumulativeProbability(0.9772498680518209d);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isBracketRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:inverse-anchor-bracket-root] valid NormalDistribution inverse on the regression seed propagated a rooted bracketing failure through the real public API",
                    t);
            }
        }
    }

    private static void exploreExactEndpointRootComposition(FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        boolean rootAtLower = data.consumeBoolean();
        int maximumIterations = data.consumeInt(1, 64);

        double initial;
        double lowerBound;
        double upperBound;

        if (rootAtLower) {
            initial = 1.0d;
            lowerBound = 0.0d;
            upperBound = 2.0d + data.consumeInt(0, 1000);
        } else {
            initial = -1.0d;
            lowerBound = -2.0d - data.consumeInt(0, 1000);
            upperBound = 0.0d;
        }

        try {
            double[] bracket = UnivariateRealSolverUtils.bracket(f, initial, lowerBound, upperBound, maximumIterations);

            if (bracket == null || bracket.length != 2) {
                throw new RuntimeException("[oracle:bracket-solver-composition] bracket returned malformed interval");
            }

            double a = bracket[0];
            double b = bracket[1];
            double fa = f.value(a);
            double fb = f.value(b);

            if (!(a <= b)) {
                throw new RuntimeException(
                    "[oracle:bracket-solver-composition] metamorphic violation: bracket interval must be ordered input="
                        + initial + "," + lowerBound + "," + upperBound + " lhs=" + a + " rhs=" + b);
            }

            double knownRoot = rootAtLower ? 0.0d : 0.0d;

            if (knownRoot < a || knownRoot > b) {
                throw new RuntimeException(
                    "[oracle:bracket-solver-composition] metamorphic violation: interval returned by bracket lost a known endpoint root input="
                        + initial + "," + lowerBound + "," + upperBound + " lhs=" + a + " rhs=" + b);
            }

            if (fa * fb > 0.0d) {
                throw new RuntimeException(
                    "[oracle:bracket-solver-composition] metamorphic violation: bracket must return endpoints whose function values do not have the same sign input="
                        + initial + "," + lowerBound + "," + upperBound + " lhs=" + fa + " rhs=" + fb);
            }

            /*
             * Sound post-condition:
             * bracket(...) promises a bracketing interval for the same real function.
             * Independently composing that returned interval with a real solver on the same function
             * must recover a root inside the interval. A throw-deleting patch that returns a wrong
             * interval would violate this even if it no longer throws.
             */
            BrentSolver solver = new BrentSolver();
            double root = solver.solve(f, a, b);

            if (root < a || root > b) {
                throw new RuntimeException(
                    "[oracle:bracket-solver-composition] metamorphic violation: solver root escaped returned bracket input="
                        + initial + "," + lowerBound + "," + upperBound + " lhs=" + root + " rhs=[" + a + "," + b + "]");
            }

            double fRoot = f.value(root);
            if (Math.abs(fRoot) > 1.0e-8d) {
                throw new RuntimeException(
                    "[oracle:bracket-solver-composition] metamorphic violation: solving on the returned bracket did not recover a root input="
                        + initial + "," + lowerBound + "," + upperBound + " lhs=" + root + " rhs=" + fRoot);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isBracketRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:bracket-solver-composition] valid-by-construction exact-endpoint-root input should be accepted by bracket and compose with a solver",
                    t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("OutOfRange")
                || name.contains("NoData")
                || name.contains("NotPositive")
                || name.contains("NullArgument")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBracketRootCause(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException || cur instanceof ConvergenceException || cur instanceof FunctionEvaluationException) {
                for (StackTraceElement ste : cur.getStackTrace()) {
                    if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                        && "bracket".equals(ste.getMethodName())) {
                        return true;
                    }
                    if ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(ste.getClassName())
                        && "value".equals(ste.getMethodName())) {
                        return true;
                    }
                    if ("org.apache.commons.math.ConvergenceException".equals(ste.getClassName())
                        && "<init>".equals(ste.getMethodName())) {
                        return true;
                    }
                    if ("org.apache.commons.math.MathRuntimeException".equals(ste.getClassName())
                        && ste.getMethodName().startsWith("createIllegalArgumentException")) {
                        return true;
                    }
                    if ("org.apache.commons.math.MathRuntimeException".equals(ste.getClassName())
                        && "buildMessage".equals(ste.getMethodName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}