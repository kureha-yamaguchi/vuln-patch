package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int cases = 1 + Math.min(4, data.remainingBytes() / 8);
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-1000, 1000);
            double left = data.consumeInt(1, 1500) / 1000.0;
            double right = data.consumeInt(1, 1500) / 1000.0;
            double root = k * Math.PI;
            double min = root - left;
            double max = root + right;
            double frac = data.consumeInt(-1000, 1000) / 1000.0;
            double initial = root + frac * Math.min(left, right);
            if (initial < min) {
                initial = min;
            } else if (initial > max) {
                initial = max;
            }
            exerciseCase(new SinFunction(), min, max, initial);
        }
    }

    private static void anchor() {
        exerciseCase(new SinFunction(), 3.0, 3.2, 3.1);
    }

    private static void exerciseCase(UnivariateRealFunction function, double min, double max, double initial) {
        BisectionSolver explicitSolver = new BisectionSolver();
        BisectionSolver storedSolver = new BisectionSolver();
        storedSolver.f = function;

        final double explicitRoot;
        try {
            explicitRoot = explicitSolver.solve(function, min, max, initial);
        } catch (Throwable t) {
            handleLibraryThrowable(t, true);
            return;
        }

        final double storedRoot;
        try {
            storedRoot = storedSolver.solve(min, max);
        } catch (Throwable t) {
            handleLibraryThrowable(t, true);
            return;
        }

        double tol = Math.max(explicitSolver.getAbsoluteAccuracy(), storedSolver.getAbsoluteAccuracy()) * 2.0;
        if (Math.abs(explicitRoot - storedRoot) > tol) {
            throw new RuntimeException(
                "[oracle:explicit-stored-root] metamorphic violation: " +
                "the overload taking an explicit function and the overload using the stored function " +
                "must solve the same equation on the same interval when the stored function is set to that same function; " +
                "input=[" + min + "," + max + "] initial=" + initial +
                " explicit=" + explicitRoot + " stored=" + storedRoot + " tol=" + tol
            );
        }

        int explicitIters = explicitSolver.getIterationCount();
        int storedIters = storedSolver.getIterationCount();
        if (explicitIters != storedIters) {
            throw new RuntimeException(
                "[oracle:explicit-stored-iters] consistency violation: " +
                "deterministic bisection with the same function, interval, and accuracy must take the same number of iterations " +
                "across equivalent overloads; input=[" + min + "," + max + "] initial=" + initial +
                " explicitIters=" + explicitIters + " storedIters=" + storedIters
            );
        }
    }

    private static void handleLibraryThrowable(Throwable t, boolean validByConstruction) {
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return;
        }
        if (t instanceof RuntimeException) {
            if (t instanceof NullPointerException && validByConstruction && isRootCauseLocation(t)) {
                throw (RuntimeException) t;
            }
            if (isOracleThrowable(t)) {
                throw (RuntimeException) t;
            }
            return;
        }
        if (t instanceof MaxIterationsExceededException || t instanceof FunctionEvaluationException) {
            return;
        }
    }

    private static boolean isRootCauseLocation(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleThrowable(Throwable t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }
}