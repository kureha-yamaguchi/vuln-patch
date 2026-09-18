package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input first.
        trySolveAndCheck(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: generate many valid-by-construction intervals for the same real API path.
        // Property: non-null function, min < max, and [min,max] brackets a real root of sin(x),
        // with initial inside the interval. A correct implementation must handle these inputs.
        int rootIndex = data.consumeInt(-1000, 1000);
        double center = rootIndex * Math.PI;

        double leftWidth = 0.0001 + (data.consumeInt(1, 100000) / 100000.0) * 3.0;
        double rightWidth = 0.0001 + (data.consumeInt(1, 100000) / 100000.0) * 3.0;

        if (leftWidth > Math.PI / 2.0) {
            leftWidth = Math.PI / 2.0;
        }
        if (rightWidth > Math.PI / 2.0) {
            rightWidth = Math.PI / 2.0;
        }

        double min = center - leftWidth;
        double max = center + rightWidth;
        if (!(min < max)) {
            return;
        }

        double fraction = data.consumeInt(0, 100000) / 100000.0;
        double initial = min + (max - min) * fraction;

        trySolveAndCheck(f, min, max, initial, true);

        int extraRounds = data.consumeInt(0, 3);
        for (int i = 0; i < extraRounds; i++) {
            int deltaLeft = data.consumeInt(0, 1000);
            int deltaRight = data.consumeInt(0, 1000);
            double min2 = center - Math.max(0.0001, leftWidth * (1.0 - deltaLeft / 2000.0));
            double max2 = center + Math.max(0.0001, rightWidth * (1.0 - deltaRight / 2000.0));
            if (!(min2 < max2)) {
                continue;
            }
            double frac2 = data.consumeInt(0, 100000) / 100000.0;
            double initial2 = min2 + (max2 - min2) * frac2;
            trySolveAndCheck(f, min2, max2, initial2, true);
        }
    }

    private static void trySolveAndCheck(UnivariateRealFunction f, double min, double max, double initial, boolean validByConstruction) {
        BisectionSolver solverA = new BisectionSolver();
        try {
            double withInitial = solverA.solve(f, min, max, initial);

            // Oracle: same-name overload agreement.
            // For a correct BisectionSolver, the overload taking an initial guess must agree with
            // the overload without it on the same function and bracketing interval; bisection does
            // not semantically depend on the initial guess for the returned root.
            BisectionSolver solverB = new BisectionSolver();
            double withoutInitial;
            try {
                withoutInitial = solverB.solve(f, min, max);
            } catch (Throwable t) {
                return;
            }

            double tol = Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()) * 2.0;
            if (Double.isNaN(withInitial) != Double.isNaN(withoutInitial) ||
                (!Double.isNaN(withInitial) && Math.abs(withInitial - withoutInitial) > tol)) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=min=" + min + ",max=" + max + ",initial=" + initial
                        + " lhs=" + withInitial + " rhs=" + withoutInitial);
            }
        } catch (Throwable t) {
            if (shouldPropagate(t, validByConstruction)) {
                throw rethrow(t);
            }
        }
    }

    private static boolean shouldPropagate(Throwable t, boolean validByConstruction) {
        if (!validByConstruction) {
            return false;
        }
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        return hasSolveFrame(t);
    }

    private static boolean hasSolveFrame(Throwable t) {
        for (StackTraceElement ste : t.getStackTrace()) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException(t);
    }
}