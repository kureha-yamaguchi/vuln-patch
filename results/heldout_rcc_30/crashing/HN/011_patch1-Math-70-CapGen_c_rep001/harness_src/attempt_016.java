package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression test input from BisectionSolverTest.testMath369.
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                throwUnchecked(t);
            }
            return;
        }

        // EXPLORE: build many valid-by-construction inputs with the same triggering property:
        // non-null function and a bracketing interval around a real root of sin(x).
        double root = Math.PI * data.consumeInt(-100, 100);
        double halfWidth = 0.01 + (data.consumeInt(0, 990) / 1000.0); // 0.01..1.0
        double min = root - halfWidth;
        double max = root + halfWidth;

        // Keep the initial guess inside the interval.
        double fraction = data.consumeInt(0, 1000) / 1000.0;
        double initial = min + (max - min) * fraction;

        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                throwUnchecked(t);
            }
            return;
        }

        // Oracle: the documented same-name solve overloads are required to agree on equivalent inputs.
        // For BisectionSolver, the "initial" value is not part of the bisection state transition, so
        // solve(f, min, max, initial) must compute the same root as solve(f, min, max) on the same
        // valid bracketing interval. A throw-deleting or wrong-delegation patch would violate this.
        try {
            BisectionSolver withInitial = new BisectionSolver();
            double lhs = withInitial.solve(f, min, max, initial);

            BisectionSolver withoutInitial = new BisectionSolver();
            double rhs = withoutInitial.solve(f, min, max);

            double tol = Math.max(withInitial.getAbsoluteAccuracy(), withoutInitial.getAbsoluteAccuracy()) + 1e-12;
            if (Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input={min=" + min + ", max=" + max + ", initial=" + initial + "}"
                        + " lhs=" + lhs + " rhs=" + rhs + " tol=" + tol
                );
            }
        } catch (Throwable t) {
            if (t instanceof RuntimeException && isOracleViolation((RuntimeException) t)) {
                throw (RuntimeException) t;
            }
            if (shouldPropagateRootCause(t, true)) {
                throwUnchecked(t);
            }
            return;
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleViolation(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
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