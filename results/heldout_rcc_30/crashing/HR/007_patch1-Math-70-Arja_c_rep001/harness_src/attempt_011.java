package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.QuinticFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int signedK = data.consumeInt(-8, 8);
        if (signedK == 0) {
            signedK = 1;
        }
        double center = signedK * Math.PI;

        double width = 0.05 + (data.consumeInt(0, 250) / 1000.0);
        double min = center - width;
        double max = center + width;

        int initialMode = data.consumeInt(0, 5);
        double initial;
        switch (initialMode) {
            case 0:
                initial = min;
                break;
            case 1:
                initial = max;
                break;
            case 2:
                initial = (min + max) / 2.0;
                break;
            case 3:
                initial = min - width;
                break;
            case 4:
                initial = max + width;
                break;
            default:
                initial = center + (data.consumeInt(-100, 100) / 1000.0);
                break;
        }

        runConflictingStoredFunctionCheck(min, max, initial, center);
        runTwoArgKnownRootCheck(min, max, center);
    }

    private static void anchor() {
        try {
            UnivariateRealFunction f = new SinFunction();
            UnivariateRealSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isGroundTruthRootCause(t)) {
                return;
            }
        }
    }

    private static void runConflictingStoredFunctionCheck(double min, double max, double initial, double expectedRoot) {
        try {
            BisectionSolver solver = new BisectionSolver(new QuinticFunction());
            double result = solver.solve(new SinFunction(), min, max, initial);

            double tol = Math.max(1e-6, solver.getAbsoluteAccuracy() * 8.0);
            /* Contract/oracle: on the constructed interval [k*pi-w, k*pi+w] with 0<w<pi/2,
             * sin has the known root k*pi inside the interval, and a correct solve(f, min, max, initial)
             * must solve for the passed-in function f, not any receiver-stored function.
             * A band-aid that merely suppresses the original NPE but still consults the wrong function
             * will return a point far from the known root. */
            if (Math.abs(result - expectedRoot) > tol) {
                throw new RuntimeException(
                    "[oracle:passed-f-wins] metamorphic violation: solve(f,min,max,initial) must use the passed function on a valid sin bracket"
                        + " min=" + min
                        + " max=" + max
                        + " initial=" + initial
                        + " expectedRoot=" + expectedRoot
                        + " result=" + result
                        + " tol=" + tol);
            }

            /* Consistency check: after a successful solve, the solver's cached result must match
             * the value just returned by the same call. A throw-deleting patch that skips result
             * bookkeeping would violate this observable post-condition. */
            double cached = solver.getResult();
            if (Math.abs(cached - result) > tol) {
                throw new RuntimeException(
                    "[oracle:cache-result] metamorphic violation: returned result and cached solver result disagree"
                        + " returned=" + result
                        + " cached=" + cached
                        + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof RuntimeException && ((RuntimeException) t).getMessage() != null
                    && ((RuntimeException) t).getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
            if (isGroundTruthRootCause(t)) {
                return;
            }
        }
    }

    private static void runTwoArgKnownRootCheck(double min, double max, double expectedRoot) {
        try {
            BisectionSolver solver = new BisectionSolver(new SinFunction());
            double result = solver.solve(min, max);
            double tol = Math.max(1e-6, solver.getAbsoluteAccuracy() * 8.0);
            if (Math.abs(result - expectedRoot) > tol) {
                throw new RuntimeException(
                    "[oracle:twoarg-known-root] metamorphic violation: solve(min,max) on stored sin function missed the known k*pi root"
                        + " min=" + min
                        + " max=" + max
                        + " expectedRoot=" + expectedRoot
                        + " result=" + result
                        + " tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (t instanceof RuntimeException && ((RuntimeException) t).getMessage() != null
                    && ((RuntimeException) t).getMessage().startsWith("[oracle:")) {
                throw (RuntimeException) t;
            }
        }
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("ConvergenceException")
                || name.contains("FunctionEvaluationException")
                || name.contains("MaxIterationsExceededException");
    }

    private static boolean isGroundTruthRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < st.length; i++) {
            StackTraceElement e = st[i];
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}