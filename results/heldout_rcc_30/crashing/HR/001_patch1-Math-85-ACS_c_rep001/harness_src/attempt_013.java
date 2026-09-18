package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runPublicApiAnchor();

        int trials = 1 + data.consumeInt(1, 4);
        for (int i = 0; i < trials; i++) {
            checkEndpointRootViaRealFunction(data);
        }
    }

    private static void runPublicApiAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            normal.inverseCumulativeProbability(0.9772498680518209d);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isBracketRootCause(t)) {
                return;
            }
        }
    }

    private static void checkEndpointRootViaRealFunction(FuzzedDataProvider data) {
        int rootInt = data.consumeInt(-1000, 1000);
        int scaleInt = data.consumeInt(-1000, 1000);
        if (scaleInt == 0) {
            scaleInt = 1;
        }

        double root = rootInt;
        double scale = scaleInt;
        double lowerBound = root - 2.0d;
        double initial = root - 1.0d;
        double upperBound = root;
        int maximumIterations = data.consumeInt(1, 8);

        PolynomialFunction function = new PolynomialFunction(new double[] { -scale * root, scale });

        try {
            double[] bracket = UnivariateRealSolverUtils.bracket(
                    function, initial, lowerBound, upperBound, maximumIterations);

            if (bracket == null || bracket.length != 2) {
                throw new RuntimeException("[oracle:endpoint-root-post] metamorphic violation: invalid bracket array");
            }

            double a = bracket[0];
            double b = bracket[1];
            double fa = function.value(a);
            double fb = function.value(b);

            /* Contract being asserted:
             * bracket(...) returns an interval bracketing a root. For the constructed linear
             * PolynomialFunction scale*(x-root), the unique root is known exactly as 'root'.
             * Therefore any correct returned bracket must (1) stay within the requested bounds,
             * (2) contain that known root, and (3) satisfy f(a)*f(b) <= 0, including the
             * patched endpoint-root case where one endpoint is exactly zero. A patch that merely
             * deletes the throw or returns an arbitrary interval violates these observables.
             */
            if (a < lowerBound || b > upperBound || a > b) {
                throw new RuntimeException(
                        "[oracle:endpoint-root-post] metamorphic violation: returned interval escaped requested bounds"
                                + " lower=" + lowerBound + " upper=" + upperBound + " a=" + a + " b=" + b);
            }
            if (root < a || root > b) {
                throw new RuntimeException(
                        "[oracle:endpoint-root-post] metamorphic violation: known root not contained"
                                + " root=" + root + " a=" + a + " b=" + b);
            }
            if (fa * fb > 0.0d) {
                throw new RuntimeException(
                        "[oracle:endpoint-root-post] metamorphic violation: returned interval does not bracket a root"
                                + " a=" + a + " b=" + b + " fa=" + fa + " fb=" + fb + " root=" + root);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isDirectBracketBug(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isBracketRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        return hasBracketFrame(t);
    }

    private static boolean isDirectBracketBug(Throwable t) {
        if (!(t instanceof ConvergenceException || t instanceof MathException)) {
            return false;
        }
        return hasBracketFrame(t);
    }

    private static boolean hasBracketFrame(Throwable t) {
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement e : stack) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(e.getClassName())
                    && "inverseCumulativeProbability".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("IllegalArgument")
                || name.contains("NumberFormat")
                || name.contains("Invalid")
                || name.contains("OutOfRange");
    }

    private static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}