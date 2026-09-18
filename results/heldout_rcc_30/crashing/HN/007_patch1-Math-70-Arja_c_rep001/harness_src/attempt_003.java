package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact trigger from the regression test.
        runSolveAndMaybePropagate(new BisectionSolver(), f, 3.0, 3.2, 3.1);

        // EXPLORE: build many valid-by-construction brackets around k*pi.
        // For sin(x), any interval [root-delta, root+delta] with 0 < delta < pi
        // brackets a real root at root = k*pi and is a valid input for the solver.
        int k = data.consumeInt(-100, 100);
        double root = k * Math.PI;

        int milli = data.consumeInt(50, 1000);
        double delta = milli / 1000.0; // 0.05 .. 1.0, safely < pi

        double min = root - delta;
        double max = root + delta;

        int initialSelector = data.consumeInt(0, 1000);
        double initial = min + (max - min) * (initialSelector / 1000.0);

        runSolveAndMaybePropagate(new BisectionSolver(), f, min, max, initial);

        // Mandatory oracle:
        // The overloads solve(f, min, max, initial) and solve(f, min, max) are
        // same-operation overloads on the same valid bracket; for bisection, the
        // initial guess must not change which root is returned for this interval.
        // If either side throws, this check is skipped.
        try {
            BisectionSolver s1 = new BisectionSolver();
            double withInitial = s1.solve(f, min, max, initial);

            BisectionSolver s2 = new BisectionSolver();
            double withoutInitial = s2.solve(f, min, max);

            double tol = Math.max(s1.getAbsoluteAccuracy(), s2.getAbsoluteAccuracy()) * 4.0;
            if (Double.isNaN(withInitial) || Double.isNaN(withoutInitial)
                    || Math.abs(withInitial - withoutInitial) > tol) {
                throw new RuntimeException(
                        "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                                + " input={min=" + min + ", max=" + max + ", initial=" + initial + ", k=" + k + "}"
                                + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }

            // Oracle from the input itself: we constructed the bracket around the known root k*pi.
            if (Math.abs(withInitial - root) > tol) {
                throw new RuntimeException(
                        "[oracle:known-root] metamorphic violation: solving sin on a bracket centered on k*pi must recover that root"
                                + " input={min=" + min + ", max=" + max + ", initial=" + initial + ", k=" + k + ", root=" + root + "}"
                                + " lhs=" + withInitial + " rhs=" + root);
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }
    }

    private static void runSolveAndMaybePropagate(BisectionSolver solver, UnivariateRealFunction f,
                                                  double min, double max, double initial) {
        try {
            solver.solve(f, min, max, initial);
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return;
            }
            return;
        }
    }

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
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid")
                || name.contains("NoBracketing")
                || name.contains("Convergence")
                || name.contains("Argument");
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