package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MathException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        runAnchorNormalSeed();
        runSinEndpointSolveSiblingOracle(data);
    }

    private static void runAnchorNormalSeed() {
        try {
            NormalDistribution normal = new NormalDistributionImpl(0, 1);
            double p = 0.9772498680518209d;
            double q = normal.inverseCumulativeProbability(p);
            if (Math.abs(q - 2.0d) > 1.0e-12d) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:seed-fixedpoint] inverseCumulativeProbability(cdf(2)) must recover 2.0 for the documented regression seed; got " + q
                );
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            if (isCleanRejection(t)) {
                return;
            }
            if (isMathBugFromInverseOrBracket(t)) {
                return;
            }
        }
    }

    private static void runSinEndpointSolveSiblingOracle(FuzzedDataProvider data) {
        SinFunction f = new SinFunction();

        double initial = data.consumeInt(0, 1000) / 1000.0d;
        double lower = 0.0d;
        double upper = data.consumeInt(2, 16);
        int maximumIterations = data.consumeInt(1, 32);

        double[] interval;
        try {
            interval = UnivariateRealSolverUtils.bracket(f, initial, lower, upper, maximumIterations);
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            if (isCleanRejection(t)) {
                return;
            }
            if (isMathBugFromBracket(t)) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:solve-sibling] bracket rejected a valid interval where the real library function has an exact endpoint root: "
                        + "initial=" + initial + " lower=" + lower + " upper=" + upper + " maxIter=" + maximumIterations,
                    t
                );
            }
            return;
        }

        if (interval == null || interval.length != 2) {
            throw new FuzzerSecurityIssueLow(
                "[oracle:solve-sibling] bracket returned malformed interval"
            );
        }

        try {
            double a = interval[0];
            double b = interval[1];
            double fa = f.value(a);
            double fb = f.value(b);

            if (fa * fb > 0.0d) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:solve-sibling] bracket post-condition violated: returned endpoints do not bracket a root: "
                        + "a=" + a + " b=" + b + " f(a)=" + fa + " f(b)=" + fb
                );
            }

            /*
             * Documented sibling-overload guarantee: solve(f, x0, x1) and solve(f, x0, x1, absoluteAccuracy)
             * solve the same problem on the same bracketing interval, so on a valid interval they must agree.
             * A band-aid patch that merely suppresses the throw but returns a wrong interval or skips the intended
             * endpoint-root handling can leave the top-level throw gone while the recovered root differs.
             */
            double rootDefault = UnivariateRealSolverUtils.solve(f, a, b);
            double rootAccurate = UnivariateRealSolverUtils.solve(f, a, b, 1.0e-12d);

            if (Math.abs(rootDefault - rootAccurate) > 1.0e-9d) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:solve-sibling] metamorphic violation: solve overloads disagree on the same bracketed interval "
                        + "a=" + a + " b=" + b + " rootDefault=" + rootDefault + " rootAccurate=" + rootAccurate
                );
            }

            double residual1 = Math.abs(f.value(rootDefault));
            double residual2 = Math.abs(f.value(rootAccurate));
            if (residual1 > 1.0e-9d || residual2 > 1.0e-9d) {
                throw new FuzzerSecurityIssueLow(
                    "[oracle:solve-sibling] returned root does not satisfy the real function closely enough "
                        + "rootDefault=" + rootDefault + " residual1=" + residual1
                        + " rootAccurate=" + rootAccurate + " residual2=" + residual2
                );
            }
        } catch (Throwable t) { /*__vpRepair*/ if (t instanceof com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) throw (com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow) t;
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof FuzzerSecurityIssueLow) {
                throw (FuzzerSecurityIssueLow) t;
            }
            return;
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof IllegalArgumentException || cur instanceof NumberFormatException) {
                return true;
            }
            String name = cur.getClass().getName();
            if (name.contains("Invalid") || name.contains("Illegal") || name.contains("OutOfRange")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isMathBugFromInverseOrBracket(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && stackHas(cur, "org.apache.commons.math.distribution.AbstractContinuousDistribution", "inverseCumulativeProbability")) {
                return true;
            }
            if (cur instanceof MathException && stackHas(cur, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean isMathBugFromBracket(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof MathException && stackHas(cur, "org.apache.commons.math.analysis.solvers.UnivariateRealSolverUtils", "bracket")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static boolean stackHas(Throwable t, String className, String methodName) {
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement ste = stack[i];
            if (className.equals(ste.getClassName()) && methodName.equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}