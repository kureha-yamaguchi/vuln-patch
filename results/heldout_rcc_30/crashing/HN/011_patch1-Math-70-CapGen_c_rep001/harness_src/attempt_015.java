package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input. On the buggy build this must hit
        // BisectionSolver.solve(f, min, max, initial) and surface as NPE from solve.
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(f, 3.0, 3.2, 3.1);
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                throw propagate(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
        }

        // EXPLORE: valid-by-construction brackets around roots of sin(x) = 0 at k*pi.
        // We keep values moderate and ensure min < max with the root inside the interval,
        // so a correct implementation is obliged to handle them.
        int trials = 1 + Math.max(0, data.consumeInt(0, 4));
        for (int i = 0; i < trials; i++) {
            int k = data.consumeInt(-100, 100);
            double root = k * Math.PI;

            double width = data.consumeInt(1, 1000) / 1000.0; // (0, 1]
            double min = root - width;
            double max = root + width;

            int frac = data.consumeInt(0, 1000);
            double initial = min + (max - min) * (frac / 1000.0);

            // Slightly vary surrounding content while preserving the key property:
            // a valid bracket around a real root for the real SinFunction.
            if (data.consumeBoolean()) {
                int leftNudge = data.consumeInt(0, 200);
                int rightNudge = data.consumeInt(0, 200);
                min -= leftNudge / 10000.0;
                max += rightNudge / 10000.0;
                if (initial < min) {
                    initial = min;
                } else if (initial > max) {
                    initial = max;
                }
            }

            runOneValidCase(f, min, max, initial);
        }
    }

    private static void runOneValidCase(UnivariateRealFunction f, double min, double max, double initial) {
        try {
            BisectionSolver s1 = new BisectionSolver();
            double withInitial = s1.solve(f, min, max, initial);

            // Oracle: the documented same-name overloads solve the same mathematical problem.
            // For a correct BisectionSolver, the "initial" parameter does not change the bracketing
            // root that is solved for here; therefore solve(f,min,max,initial) must agree with
            // solve(f,min,max) on the same valid interval. A patch that merely suppresses the crash
            // or dispatches incorrectly can violate this observable relation.
            BisectionSolver s2 = new BisectionSolver();
            double withoutInitial = s2.solve(f, min, max);

            double tol = Math.max(s1.getAbsoluteAccuracy(), s2.getAbsoluteAccuracy()) * 4.0;
            if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                    || Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agree] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                    + " input=min=" + min + ",max=" + max + ",initial=" + initial
                    + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }
        } catch (Throwable t) {
            if (shouldPropagateRootCause(t, true)) {
                throw propagate(t);
            }
            // If either side throws, the oracle does not apply for that input.
            if (isCleanRejection(t)) {
                return;
            }
        }
    }

    private static boolean shouldPropagateRootCause(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof NullPointerException)) {
            return t instanceof RuntimeException && isOracleRuntime((RuntimeException) t);
        }
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOracleRuntime(RuntimeException t) {
        String msg = t.getMessage();
        return msg != null && msg.startsWith("[oracle:");
    }

    private static boolean isCleanRejection(Throwable t) {
        return t instanceof IllegalArgumentException
                || t instanceof NumberFormatException
                || t instanceof FunctionEvaluationException
                || t instanceof MaxIterationsExceededException;
    }

    private static RuntimeException propagate(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
    }
}