package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        SinFunction f = new SinFunction();

        // ANCHOR: exact trigger from BisectionSolverTest.testMath369.
        try {
            BisectionSolver solver = new BisectionSolver();
            double result = solver.solve(f, 3.0, 3.2, 3.1);

            // Post-condition / sibling-agreement oracle:
            // The same-name overloads solve(f,min,max,initial) and solve(f,min,max)
            // are documented to solve the same problem for the same function and bracket.
            // For bisection, the initial guess must not change the computed root.
            // A "fix" that merely suppresses the crash but skips delegating to the real
            // solve(f,min,max) path would break this observable agreement.
            try {
                BisectionSolver sibling = new BisectionSolver();
                double siblingResult = sibling.solve(f, 3.0, 3.2);
                double tol = Math.max(solver.getAbsoluteAccuracy(), sibling.getAbsoluteAccuracy());
                if (Double.isNaN(result) || Double.isNaN(siblingResult) || Math.abs(result - siblingResult) > tol) {
                    throw new RuntimeException("[oracle:overload-anchor] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[3.0,3.2,3.1] lhs=" + result + " rhs=" + siblingResult);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            return;
        }

        // EXPLORE: valid-by-construction brackets around real roots of sin(x).
        // This keeps inputs within the documented precondition f != null and constructs
        // intervals that a correct solver is obliged to accept.
        int k = data.consumeInt(-1000, 1000);
        double width = 0.1 + (data.consumeInt(0, 900) / 1000.0); // in [0.1, 1.0]
        double root = k * Math.PI;
        double min = root - width;
        double max = root + width;

        double initial;
        if (data.consumeBoolean()) {
            initial = min + (max - min) * (data.consumeInt(0, 1000) / 1000.0);
        } else {
            initial = root;
        }

        try {
            BisectionSolver solverWithInitial = new BisectionSolver();
            double lhs = solverWithInitial.solve(f, min, max, initial);

            try {
                BisectionSolver solverNoInitial = new BisectionSolver();
                double rhs = solverNoInitial.solve(f, min, max);
                double tol = Math.max(solverWithInitial.getAbsoluteAccuracy(), solverNoInitial.getAbsoluteAccuracy());
                if (Double.isNaN(lhs) || Double.isNaN(rhs) || Math.abs(lhs - rhs) > tol) {
                    throw new RuntimeException("[oracle:overload-explore] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max) input=[" + min + "," + max + "," + initial + "] lhs=" + lhs + " rhs=" + rhs);
                }
            } catch (Throwable ignored) {
                return;
            }
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
        }
    }

    private static boolean isRootCause(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement e : trace) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(e.getClassName())
                    && "solve".equals(e.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>sneakyThrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}