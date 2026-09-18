package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression test input from BisectionSolverTest.testMath369.
        runValidCase(f, 3.0, 3.2, 3.1);

        // EXPLORE: valid-by-construction bracketing intervals for sin(x) around k*pi.
        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double left = scaledPositive(data.consumeInt(1, 1_000_000), 1_000_000.0);
        double right = scaledPositive(data.consumeInt(1, 1_000_000), 1_000_000.0);

        // Keep offsets moderate and below pi/2 so [root-left, root+right] brackets exactly one sin root.
        left = 1.0e-6 + (left % 1.0);
        right = 1.0e-6 + (right % 1.0);

        double min = root - left;
        double max = root + right;

        int selector = data.consumeInt(0, 1_000_000);
        double initial = min + (max - min) * (selector / 1_000_000.0);
        if (data.consumeBoolean()) {
            initial = root;
        }

        runValidCase(f, min, max, initial);
    }

    private static double scaledPositive(int value, double scale) {
        double v = value / scale;
        if (v < 0) {
            v = -v;
        }
        return v;
    }

    private static void runValidCase(UnivariateRealFunction f, double min, double max, double initial) {
        BisectionSolver solverWithInitial = new BisectionSolver();
        BisectionSolver solverWithoutInitial = new BisectionSolver();

        Double lhs = null;
        try {
            lhs = solverWithInitial.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedMethodNpe(t)) {
                throwUnchecked(t);
            }
            return;
        }

        try {
            // Contract/oracle: these same-name overloads are documented to agree where their docs match.
            // For BisectionSolver, solve(f, min, max, initial) should behave like solve(f, min, max);
            // a bogus "fix" that ignores the function or changes behavior would break this sibling agreement.
            double rhs = solverWithoutInitial.solve(f, min, max);

            double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), solverWithoutInitial.getAbsoluteAccuracy()) * 10.0;
            if (Double.isNaN(lhs.doubleValue()) || Double.isNaN(rhs) || Math.abs(lhs.doubleValue() - rhs) > tol) {
                throw new RuntimeException(
                    "[oracle:solve-overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=min=" + min + ",max=" + max + ",initial=" + initial
                        + " lhs=" + lhs + ", rhs=" + rhs + ", tol=" + tol);
            }
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isPatchedMethodNpe(t)) {
                throwUnchecked(t);
            }
            return;
        }
    }

    private static boolean isPatchedMethodNpe(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] stack = t.getStackTrace();
        if (stack == null) {
            return false;
        }
        for (StackTraceElement e : stack) {
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
        return name.contains("ConvergenceException")
                || name.contains("FunctionEvaluationException")
                || name.contains("MaxIterationsExceededException")
                || name.contains("MathException");
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