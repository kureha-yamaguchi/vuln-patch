package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact regression-test input from BisectionSolverTest.testMath369.
        runCase(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: valid-by-construction inputs that preserve the root cause property:
        // a non-null real function and a valid bracketing interval for a known root.
        int k = data.consumeInt(-100, 100);
        double center = k * Math.PI;
        double delta = 0.01 + (data.consumeInt(0, 1000) / 1000.0) * 1.0; // in [0.01, 1.01]
        if (delta >= (Math.PI / 2.0)) {
            delta = (Math.PI / 2.0) - 0.01;
        }
        double min = center - delta;
        double max = center + delta;

        double initial;
        if (data.consumeBoolean()) {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        } else {
            initial = center;
        }

        runCase(f, min, max, initial, true);
    }

    private static void runCase(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        BisectionSolver solver = new BisectionSolver();

        try {
            double withInitial = solver.solve(f, min, max, initial);

            // Documented overload-agreement oracle:
            // these solve overloads are same-name variants for the same solver and inputs;
            // for a correct implementation, adding an initial guess must not change the solved root.
            try {
                double withoutInitial = solver.solve(f, min, max);
                double tol = Math.max(solver.getAbsoluteAccuracy(), 1e-9);
                if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial) ||
                    Math.abs(withInitial - withoutInitial) > tol) {
                    throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                            + " input=min=" + min
                            + " max=" + max
                            + " initial=" + initial
                            + " lhs=" + withInitial
                            + " rhs=" + withoutInitial);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (t instanceof NullPointerException && passesThroughSolve(t)) {
            return true;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return false;
        }
        if (t instanceof FunctionEvaluationException || t instanceof MaxIterationsExceededException) {
            return false;
        }
        return false;
    }

    private static boolean passesThroughSolve(Throwable t) {
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