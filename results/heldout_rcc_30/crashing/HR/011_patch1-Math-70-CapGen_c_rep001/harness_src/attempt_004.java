package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] st = t.getStackTrace();
        if (st == null) {
            return false;
        }
        for (StackTraceElement e : st) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException || t instanceof NumberFormatException;
    }

    private static void maybePropagate(Throwable t) {
        if (isRootCause(t)) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    private static double consumeFraction(FuzzedDataProvider data) {
        int raw = data.consumeInt(0, 1_000_000);
        return raw / 1_000_000.0;
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
            maybePropagate(t);
        }
    }

    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        anchor();

        int k = data.consumeInt(1, 100);
        double center = k * Math.PI;
        double width = 0.05 + consumeFraction(data) * 1.0;
        double min = center - width;
        double max = center + width;
        double initial = min + consumeFraction(data) * (max - min);

        if (!(min < initial && initial < max)) {
            return;
        }

        double storedResult;
        try {
            BisectionSolver storedSolver = new BisectionSolver(new SinFunction());
            storedResult = storedSolver.solve(min, max);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            maybePropagate(t);
            return;
        }

        double argResult;
        try {
            BisectionSolver argSolver = new BisectionSolver();
            argResult = argSolver.solve(new SinFunction(), min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            maybePropagate(t);
            return;
        }

        double directResult;
        try {
            BisectionSolver directSolver = new BisectionSolver();
            directResult = directSolver.solve(new SinFunction(), min, max);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            maybePropagate(t);
            return;
        }

        double tol = 1e-6;

        /*
         * Contract used for this oracle:
         * all BisectionSolver overloads solve the same bracketed root of the same
         * function on the same interval. A correct implementation must therefore
         * report the same root (up to solver accuracy) whether the function is
         * supplied in the constructor and solve(min,max) is used, or supplied as
         * an argument to solve(f,min,max,initial), or to solve(f,min,max).
         * This catches a throw-deleting/band-aid patch that suppresses the NPE but
         * still loses the function or computes a different root.
         */
        if (Math.abs(storedResult - argResult) > tol) {
            throw new RuntimeException(
                    "[oracle:ctor-arg-consistency] metamorphic violation: stored-function solve(min,max) disagrees with solve(f,min,max,initial)"
                            + " min=" + min + " max=" + max + " initial=" + initial
                            + " stored=" + storedResult + " arg=" + argResult);
        }

        if (Math.abs(directResult - argResult) > tol) {
            throw new RuntimeException(
                    "[oracle:arg-overload-consistency] metamorphic violation: solve(f,min,max) disagrees with solve(f,min,max,initial)"
                            + " min=" + min + " max=" + max + " initial=" + initial
                            + " direct=" + directResult + " arg=" + argResult);
        }
    }
}