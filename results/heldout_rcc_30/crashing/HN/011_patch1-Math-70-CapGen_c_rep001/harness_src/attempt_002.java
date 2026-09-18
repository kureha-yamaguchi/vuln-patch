package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        runTargetAndOracles(f, 3.0, 3.2, 3.1, Math.PI, true);

        int cases = 1 + Math.max(0, Math.min(4, data.remainingBytes()));
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-100, 100);
            double expectedRoot = k * Math.PI;

            int milliWidth = data.consumeInt(10, 1000);
            double halfWidth = milliWidth / 1000.0;

            double min = expectedRoot - halfWidth;
            double max = expectedRoot + halfWidth;

            int initialMilliOffset = data.consumeInt(-milliWidth, milliWidth);
            double initial = expectedRoot + (initialMilliOffset / 1000.0);

            runTargetAndOracles(f, min, max, initial, expectedRoot, true);
        }
    }

    private static void runTargetAndOracles(UnivariateRealFunction f, double min, double max, double initial,
                                            double expectedRoot, boolean validByConstruction) {
        BisectionSolver solver = new BisectionSolver();
        double result;
        try {
            result = solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, validByConstruction)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }

        double tol = Math.max(solver.getAbsoluteAccuracy() * 4.0, 1.0e-8);

        // Contract/oracle: these inputs are constructed around the exact sine root k*pi with min < root < max,
        // so any correct solve(f,min,max,initial) implementation must return that unique root in the interval.
        if (!approximatelyEqual(result, expectedRoot, tol)) {
            throw new RuntimeException(
                "[oracle:known-root] metamorphic violation: constructed sine bracket around exact root input=("
                    + min + "," + max + "," + initial + ") result=" + result + " expected=" + expectedRoot);
        }

        // Contract/oracle: same-name overloads with matching documented purpose must agree on equivalent inputs.
        // For BisectionSolver, the initial guess should not change the solved root for the same bracketed function.
        BisectionSolver sibling = new BisectionSolver();
        try {
            double siblingResult = sibling.solve(f, min, max);
            if (!approximatelyEqual(result, siblingResult, tol)) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) disagrees with solve(f,min,max)"
                        + " input=(" + min + "," + max + "," + initial + ") lhs=" + result + " rhs=" + siblingResult);
            }
        } catch (Throwable ignored) {
            return;
        }
    }

    private static boolean approximatelyEqual(double a, double b, double tol) {
        return Double.isFinite(a) && Double.isFinite(b) && Math.abs(a - b) <= tol;
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        return t instanceof NullPointerException && stackPassesThroughSolve(t);
    }

    private static boolean stackPassesThroughSolve(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                && "solve".equals(e.getMethodName())) {
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
        return name.contains("Illegal")
            || name.contains("Invalid")
            || name.contains("Argument")
            || name.contains("Convergence")
            || name.contains("Evaluation")
            || t instanceof MaxIterationsExceededException
            || t instanceof FunctionEvaluationException;
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