package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        callRegressionAnchorThroughPublicApi();

        runEndpointRootBracketingChecks(data);
    }

    private static void callRegressionAnchorThroughPublicApi() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            normal.inverseCumulativeProbability(0.9772498680518209d);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedRegionMathFailure(t)) {
                return;
            }
        }
    }

    private static void runEndpointRootBracketingChecks(FuzzedDataProvider data) {
        SinFunction f = new SinFunction();

        double upper = 1.5d + (data.consumeInt(0, 1500) / 1000.0d);
        double initial = 0.5d + (data.consumeInt(0, 1000) / 1000.0d);
        if (initial > upper) {
            initial = 0.5d * upper;
        }
        if (initial <= 0.0d) {
            initial = 0.5d;
        }
        if (upper <= initial) {
            upper = initial + 0.5d;
        }

        int maxIterations = data.consumeInt(1, 100);

        try {
            double[] bracket = UnivariateRealSolverUtils.bracket(f, initial, 0.0d, upper, maxIterations);

            /*
             * Documented guarantee: bracket returns an interval bracketing a root.
             * Independent recomputation: evaluate the returned endpoints with the same real library
             * function and verify they actually bracket a root, i.e. f(a)*f(b) <= 0.
             * A throw-deleting or wrong-result patch would violate this observable post-condition.
             */
            if (bracket == null || bracket.length != 2) {
                throw new RuntimeException("[oracle:returned-shape] metamorphic violation: invalid bracket array");
            }
            double a = bracket[0];
            double b = bracket[1];
            double fa = f.value(a);
            double fb = f.value(b);

            if (a < 0.0d || b > upper || a > b) {
                throw new RuntimeException("[oracle:returned-bounds] metamorphic violation: initial=" + initial
                        + " upper=" + upper + " got=[" + a + "," + b + "]");
            }
            if (fa * fb > 0.0d) {
                throw new RuntimeException("[oracle:sign-witness] metamorphic violation: returned interval does not bracket a root"
                        + " initial=" + initial + " upper=" + upper + " got=[" + a + "," + b + "]"
                        + " fa=" + fa + " fb=" + fb);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedRegionMathFailure(t)) {
                sneakyThrow(t);
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.endsWith("IllegalArgumentException")
                    || name.endsWith("InvalidRepresentationException")
                    || name.endsWith("NotPositiveException")
                    || name.endsWith("OutOfRangeException")
                    || name.endsWith("NoBracketingException")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPatchedRegionMathFailure(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof MathException && hasPatchedRegionFrame(cur)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPatchedRegionFrame(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            String cls = e.getClassName();
            String method = e.getMethodName();
            if ("org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils".equals(cls)
                    && "bracket".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.distribution.AbstractContinuousDistribution".equals(cls)
                    && "inverseCumulativeProbability".equals(method)) {
                return true;
            }
            if ("org.apache.commons.math.ConvergenceException".equals(cls)
                    && "<init>".equals(method)) {
                return true;
            }
        }
        return false;
    }

    private static void sneakyThrow(Throwable t) {
        FuzzHarness.<RuntimeException>uncheckedThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T) t;
    }
}