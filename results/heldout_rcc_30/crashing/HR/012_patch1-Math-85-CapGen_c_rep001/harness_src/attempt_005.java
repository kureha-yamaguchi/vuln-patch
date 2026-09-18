package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int root = data.consumeInt(-1000, 1000);
        int extraUpper = data.consumeInt(1, 20);
        int maxIterations = data.consumeInt(1, 20);

        double r = root;
        double initial = r - 1.0;
        double lower = r - 2.0;
        double upper = r + extraUpper;

        PolynomialFunction f = new PolynomialFunction(new double[] { -r, 1.0 });
        PolynomialFunction negF = new PolynomialFunction(new double[] { r, -1.0 });

        double[] left;
        double[] right;

        try {
            left = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, maxIterations);
            right = UnivariateRealSolverUtils.bracket(negF, initial, lower, upper, maxIterations);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isBracketRootCause(t)) {
                throw new RuntimeException(
                    "[oracle:sign-invariant] valid endpoint-root bracketing threw from bracket "
                        + "root=" + r
                        + " initial=" + initial
                        + " lower=" + lower
                        + " upper=" + upper
                        + " maxIterations=" + maxIterations,
                    t);
            }
            return;
        }

        if (left == null || right == null || left.length != 2 || right.length != 2) {
            return;
        }

        /*
         * Sound oracle:
         * bracket grows [a,b] using only bounds and the sign-product fa*fb.
         * Replacing f by -f flips both endpoint signs but preserves fa*fb exactly,
         * so a correct implementation must return the same [a,b] for f and -f.
         * A patch that merely suppresses the known throw but corrupts endpoint-root
         * handling can violate this relation even when no exception escapes.
         */
        if (Double.compare(left[0], right[0]) != 0 || Double.compare(left[1], right[1]) != 0) {
            throw new RuntimeException(
                "[oracle:sign-invariant] metamorphic violation: bracket(f)==bracket(-f) "
                    + "root=" + r
                    + " lhs=[" + left[0] + "," + left[1] + "]"
                    + " rhs=[" + right[0] + "," + right[1] + "]");
        }

        try {
            double fa = f.value(left[0]);
            double fb = f.value(left[1]);
            double nfa = negF.value(right[0]);
            double nfb = negF.value(right[1]);
            boolean leftBrackets = fa == 0.0 || fb == 0.0 || fa * fb < 0.0;
            boolean rightBrackets = nfa == 0.0 || nfb == 0.0 || nfa * nfb < 0.0;
            if (!leftBrackets || !rightBrackets) {
                throw new RuntimeException(
                    "[oracle:sign-invariant] metamorphic violation: returned interval does not bracket "
                        + "root=" + r
                        + " f(a)=" + fa + " f(b)=" + fb
                        + " -f(a)=" + nfa + " -f(b)=" + nfb);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double p = 0.9772498680518209;
            double x = normal.inverseCumulativeProbability(p);
            if (Math.abs(x - 2.0) > 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:anchor-value] metamorphic violation: inverseCumulativeProbability(seed) "
                        + "expected 2.0 got " + x);
            }
        } catch (Throwable t) {
            if (isKnownInverseRootCause(t) || isCleanRejection(t)) {
                return;
            }
        }

        try {
            NormalDistribution shifted = new NormalDistributionImpl(1, 1);
            NormalDistribution standard = new NormalDistributionImpl(0, 1);
            double p = standard.cumulativeProbability(2.0);
            double a = shifted.inverseCumulativeProbability(p);
            double b = standard.inverseCumulativeProbability(p) + 1.0;
            if (Math.abs(a - b) > 1.0e-12) {
                throw new RuntimeException(
                    "[oracle:normal-shift] metamorphic violation: shifted inverse mismatch "
                        + "lhs=" + a + " rhs=" + b + " p=" + p);
            }
        } catch (Throwable t) {
            if (isKnownInverseRootCause(t) || isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean isBracketRootCause(Throwable t) {
        if (!hasClassInChain(t, ConvergenceException.class) && !hasClassInChain(t, MathException.class)) {
            return false;
        }
        return hasFrame(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")
            || hasFrame(t, "org.apache.commons.math.analysis.UnivariateRealFunction", "value")
            || hasFrame(t, "org.apache.commons.math.ConvergenceException", "<init>");
    }

    private static boolean isKnownInverseRootCause(Throwable t) {
        if (!hasClassInChain(t, MathException.class) && !hasClassInChain(t, ConvergenceException.class)) {
            return false;
        }
        return hasFrame(t, "org.apache.commons.math.distribution.AbstractContinuousDistribution", "inverseCumulativeProbability")
            || hasFrame(t, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket");
    }

    private static boolean hasClassInChain(Throwable t, Class cls) {
        Throwable cur = t;
        while (cur != null) {
            if (cls.isInstance(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean hasFrame(Throwable t, String className, String methodName) {
        Throwable cur = t;
        while (cur != null) {
            StackTraceElement[] trace = cur.getStackTrace();
            if (trace != null) {
                for (int i = 0; i < trace.length; i++) {
                    StackTraceElement e = trace[i];
                    if (className.equals(e.getClassName()) && methodName.equals(e.getMethodName())) {
                        return true;
                    }
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String n = cur.getClass().getName();
            if (n != null) {
                String l = n.toLowerCase();
                if (l.contains("illegalargument")
                    || l.contains("numberformat")
                    || l.contains("invalid")
                    || l.contains("parse")) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }
}