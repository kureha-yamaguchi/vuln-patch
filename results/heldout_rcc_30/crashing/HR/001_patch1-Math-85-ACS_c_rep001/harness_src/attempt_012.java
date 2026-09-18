package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        invokeRegressionSeedPublic();
        checkConstructedEndpointQuantile(data);
        checkIterationBudgetStability(data);
    }

    private static void invokeRegressionSeedPublic() {
        try {
            NormalDistributionImpl normal = new NormalDistributionImpl(0.0, 1.0);
            normal.inverseCumulativeProbability(0.9772498680518209);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isKnownPublicRootCause(t)) {
                return;
            }
        }
    }

    private static void checkConstructedEndpointQuantile(FuzzedDataProvider data) {
        double mean = data.consumeInt(-100, 100);
        double sd = data.consumeInt(1, 50);
        /*
         * For NormalDistribution.inverseCumulativeProbability on p > 0.5, the real code
         * reaches bracketing with an initial guess near mean + sd and expands by 1.0.
         * Building p from x = mean + sd + 1.0 makes the first expanded upper endpoint an
         * exact root of CDF(x) - p. A correct implementation must accept this valid input
         * and return that same x; deleting the throw without honoring endpoint roots would
         * violate this post-condition.
         */
        double expectedX = mean + sd + 1.0;
        NormalDistributionImpl normal = new NormalDistributionImpl(mean, sd);
        final double p;
        try {
            p = normal.cumulativeProbability(expectedX);
        } catch (Throwable t) {
            return;
        }

        try {
            double actualX = normal.inverseCumulativeProbability(p);
            if (!closeEnough(actualX, expectedX, 1.0e-12)) {
                throw new RuntimeException(
                    "[oracle:seedplus1-recovery] metamorphic violation: inverse(cdf(mean+sd+1)) must recover the constructed x"
                    + " mean=" + mean
                    + " sd=" + sd
                    + " p=" + p
                    + " expected=" + expectedX
                    + " actual=" + actualX);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isKnownPublicRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:seedplus1-recovery] metamorphic violation: valid constructed probability rejected"
                    + " mean=" + mean
                    + " sd=" + sd
                    + " x=" + expectedX
                    + " p=" + p, t);
            }
        }
    }

    private static void checkIterationBudgetStability(FuzzedDataProvider data) {
        int root = data.consumeInt(-50, 50);
        double exactRoot = (double) root;
        double initial = exactRoot - 1.0;
        double lower = exactRoot - 3.0;
        double upper = exactRoot + 3.0;
        PolynomialFunction f = new PolynomialFunction(new double[] { -exactRoot, 1.0 });

        /*
         * bracket(...) must return a bracketing interval once one endpoint is an exact root.
         * That fact is independent of any larger iteration budget, so maxIterations=1 and 3
         * must agree on this valid-by-construction input. The old bug throws instead when
         * fa*fb == 0 at the endpoint; a throw-deleting band-aid that returns inconsistent
         * intervals would also violate this stability check.
         */
        try {
            double[] one = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, 1);
            double[] three = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, 3);
            if (!sameBracket(one, three)) {
                throw new RuntimeException(
                    "[oracle:iter-budget-stability] metamorphic violation: endpoint-root bracketing must not depend on extra iterations"
                    + " root=" + exactRoot
                    + " initial=" + initial
                    + " lower=" + lower
                    + " upper=" + upper
                    + " one=[" + one[0] + "," + one[1] + "]"
                    + " three=[" + three[0] + "," + three[1] + "]");
            }
            if (!closeEnough(one[1], exactRoot, 0.0) && !closeEnough(one[0], exactRoot, 0.0)) {
                throw new RuntimeException(
                    "[oracle:iter-budget-stability] metamorphic violation: returned bracket must include the exact endpoint root"
                    + " root=" + exactRoot
                    + " bracket=[" + one[0] + "," + one[1] + "]");
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isBracketRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:iter-budget-stability] metamorphic violation: exact endpoint root should be accepted"
                    + " root=" + exactRoot
                    + " initial=" + initial
                    + " lower=" + lower
                    + " upper=" + upper, t);
            }
        }
    }

    private static boolean sameBracket(double[] a, double[] b) {
        return a != null && b != null
            && a.length == 2 && b.length == 2
            && closeEnough(a[0], b[0], 0.0)
            && closeEnough(a[1], b[1], 0.0);
    }

    private static boolean closeEnough(double a, double b, double tol) {
        return Math.abs(a - b) <= tol;
    }

    private static boolean isKnownPublicRootCause(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                && "inverseCumulativeProbability".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                && "bracket".equals(method)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBracketRootCause(Throwable t) {
        if (!(t instanceof ConvergenceException
              || t instanceof MathException
              || t instanceof FunctionEvaluationException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                && "bracket".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.analysis.UnivariateRealFunction".equals(cls)
                && "value".equals(method)) {
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
            || name.contains("Invalid")
            || name.contains("OutOfRange")
            || name.contains("NoData")
            || name.contains("NotPositive")
            || name.contains("NullArgument");
    }
}