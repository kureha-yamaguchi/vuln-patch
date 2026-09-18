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
        runPublicAnchorFirst();
        runTranslatedEndpointProperty(data);
    }

    private static void runPublicAnchorFirst() {
        NormalDistribution normal = new NormalDistributionImpl(0, 1);
        try {
            normal.inverseCumulativeProbability(0.9772498680518209);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isMathExceptionThroughInverseOrBracket(t)) {
                throw new RuntimeException("[oracle:public-anchor-throw] valid NormalDistribution inverseCumulativeProbability unexpectedly threw through bracket", t);
            }
        }
    }

    private static void runTranslatedEndpointProperty(FuzzedDataProvider data) {
        int root = data.consumeInt(-1000, 1000);
        int shiftA = data.consumeInt(1, 8);
        int shiftB = data.consumeInt(1, 8);
        int padA = data.consumeInt(1, 8);
        int padB = data.consumeInt(1, 8);

        PolynomialFunction function = new PolynomialFunction(new double[] { -((double) root), 1.0 });

        double[] intervalA = mustBracketTranslatedEndpoint(function, root, shiftA, padA);
        double[] intervalB = mustBracketTranslatedEndpoint(function, root, shiftB, padB);

        /*
         * Contract/oracle:
         * For f(x)=x-root on [root, upper], the only root in-range is exactly the lower bound.
         * A correct bracketing routine that accepts endpoint roots must therefore return an
         * interval whose lower endpoint is that exact root, regardless of how far to the right
         * the valid initial guess starts, as long as enough iterations are provided.
         * This catches throw-deleting/special-case patches: they may stop throwing for the seed
         * yet still fail to preserve the endpoint-root bracketing semantics for translated initials.
         */
        if (intervalA[0] != (double) root || intervalB[0] != (double) root) {
            throw new RuntimeException(
                "[oracle:translated-endpoint-lower] metamorphic violation: exact lower-endpoint root was not preserved"
                    + " root=" + root
                    + " shiftA=" + shiftA
                    + " shiftB=" + shiftB
                    + " a0=" + intervalA[0]
                    + " b0=" + intervalB[0]);
        }

        double valueA;
        double valueB;
        try {
            valueA = function.value(intervalA[0]);
            valueB = function.value(intervalB[0]);
        } catch (Throwable t) {
            return;
        }

        if (valueA != 0.0 || valueB != 0.0) {
            throw new RuntimeException(
                "[oracle:translated-endpoint-value] metamorphic violation: returned lower endpoint is not an exact root"
                    + " root=" + root
                    + " shiftA=" + shiftA
                    + " shiftB=" + shiftB
                    + " valueA=" + valueA
                    + " valueB=" + valueB);
        }
    }

    private static double[] mustBracketTranslatedEndpoint(PolynomialFunction function, int root, int shift, int pad) {
        double lower = root;
        double initial = root + shift;
        double upper = root + (2.0 * shift) + pad;
        int maximumIterations = shift;

        try {
            return UnivariateRealSolverUtils.bracket(function, initial, lower, upper, maximumIterations);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                throw new RuntimeException(
                    "[oracle:translated-endpoint-reject] valid-by-construction bracketing was rejected"
                        + " root=" + root
                        + " shift=" + shift
                        + " pad=" + pad, t);
            }
            if (isThroughBracket(t)) {
                throw new RuntimeException(
                    "[oracle:translated-endpoint-throw] exact endpoint root should be bracketed for translated valid initial"
                        + " root=" + root
                        + " shift=" + shift
                        + " pad=" + pad, t);
            }
            throwUnchecked(t);
            return null;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMathExceptionThroughInverseOrBracket(Throwable t) {
        boolean hasMathException = false;
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException) {
                hasMathException = true;
                break;
            }
        }
        if (!hasMathException) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            String cls = ste.getClassName();
            String method = ste.getMethodName();
            if (("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                    && "inverseCumulativeProbability".equals(method))
                || ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                    && "bracket".equals(method))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isThroughBracket(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            for (StackTraceElement ste : cur.getStackTrace()) {
                if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(ste.getClassName())
                        && "bracket".equals(ste.getMethodName())) {
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