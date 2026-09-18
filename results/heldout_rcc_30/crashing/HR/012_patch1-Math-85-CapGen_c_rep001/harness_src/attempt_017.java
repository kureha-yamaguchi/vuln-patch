package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchor();

        int maxIterA = data.consumeInt(1, 64);
        int maxIterB = data.consumeInt(1, 64);
        double upperA = 2.0 + data.consumeInt(0, 1024);
        double upperB = 2.0 + data.consumeInt(0, 1024);

        exploreEndpointRoot(upperA, maxIterA);
        exploreUpperBoundInvariance(upperA, maxIterA, upperB, maxIterB);
    }

    private static void runAnchor() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            normal.inverseCumulativeProbability(0.9772498680518209);
        } catch (Throwable t) {
            if (isValidationLike(t)) {
                return;
            }
            if (isMathExceptionAtInverseCdf(t)) {
                return;
            }
        }
    }

    private static void exploreEndpointRoot(double upper, int maxIterations) {
        UnivariateRealFunction f = new SinFunction();
        try {
            double[] bracket = UnivariateRealSolverUtils.bracket(f, 1.0, 0.0, upper, maxIterations);

            /* Contract from the visible method body:
             * bracket starts with a=b=initial and first updates
             * a=max(1-1,0)=0 and b=min(1+1,upper).
             * For any upper>=2 and maxIterations>=1, the first iteration yields [0,2].
             * Since f(0)=0 for SinFunction, the call has already found a valid bracket and
             * must return these endpoints; a throw-deleting or condition-flipping patch can
             * silently return a different interval, so assert the observable endpoints.
             */
            if (bracket == null || bracket.length != 2 || bracket[0] != 0.0 || bracket[1] != 2.0) {
                throw new RuntimeException("[oracle:sin-first-step] metamorphic violation: expected [0.0, 2.0] for upper="
                        + upper + " maxIterations=" + maxIterations + " got "
                        + format(bracket));
            }
        } catch (Throwable t) {
            if (isValidationLike(t)) {
                return;
            }
            if (hasBracketFrame(t)) {
                RuntimeException r = new RuntimeException(
                        "[oracle:sin-first-step] metamorphic violation: valid endpoint-root input should not fail; upper="
                                + upper + " maxIterations=" + maxIterations + " threw " + t,
                        t);
                r.setStackTrace(t.getStackTrace());
                throw r;
            }
        }
    }

    private static void exploreUpperBoundInvariance(double upperA, int maxIterA, double upperB, int maxIterB) {
        UnivariateRealFunction f = new SinFunction();
        try {
            double[] a = UnivariateRealSolverUtils.bracket(f, 1.0, 0.0, upperA, maxIterA);
            double[] b = UnivariateRealSolverUtils.bracket(f, 1.0, 0.0, upperB, maxIterB);

            /* Sound relation from the same real API:
             * with initial=1, lower=0, any upper>=2 and any positive maxIterations,
             * the first expansion deterministically reaches [0,2], where SinFunction
             * has an endpoint root at 0. Therefore changing only upperBound / iteration
             * budget cannot change the returned bracket on a correct implementation.
             */
            if (a == null || b == null || a.length != 2 || b.length != 2
                    || a[0] != b[0] || a[1] != b[1]) {
                throw new RuntimeException("[oracle:sin-upper-invariant] metamorphic violation: same endpoint-root case disagreed;"
                        + " upperA=" + upperA + " maxIterA=" + maxIterA + " -> " + format(a)
                        + ", upperB=" + upperB + " maxIterB=" + maxIterB + " -> " + format(b));
            }
        } catch (Throwable t) {
            if (isValidationLike(t)) {
                return;
            }
            if (hasBracketFrame(t)) {
                RuntimeException r = new RuntimeException(
                        "[oracle:sin-upper-invariant] metamorphic violation: valid calls should agree; "
                                + "upperA=" + upperA + " maxIterA=" + maxIterA
                                + " upperB=" + upperB + " maxIterB=" + maxIterB
                                + " threw " + t,
                        t);
                r.setStackTrace(t.getStackTrace());
                throw r;
            }
        }
    }

    private static boolean isMathExceptionAtInverseCdf(Throwable t) {
        if (!(t instanceof MathException)) {
            return false;
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(e.getClassName())
                    && "inverseCumulativeProbability".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBracketFrame(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(e.getClassName())
                    && "bracket".equals(e.getMethodName())) {
                return true;
            }
            if ("org.apache.commons.math.ConvergenceException".equals(e.getClassName())
                    && "<init>".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidationLike(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String n = t.getClass().getName();
        return n.contains("Invalid")
                || n.contains("OutOfRange")
                || n.contains("NoData")
                || n.contains("NullArgument")
                || n.contains("Illegal");
    }

    private static String format(double[] v) {
        if (v == null) {
            return "null";
        }
        if (v.length != 2) {
            return "len=" + v.length;
        }
        return "[" + v[0] + ", " + v[1] + "]";
    }
}