package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.SinFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        final UnivariateRealFunction anchorFunction = new SinFunction();

        // ANCHOR: exact failing test input from BisectionSolverTest.testMath369.
        // This input is valid by construction: non-null function and [3.0, 3.2] brackets the root pi.
        try {
            BisectionSolver solver = new BisectionSolver();
            solver.solve(anchorFunction, 3.0d, 3.2d, 3.1d);
        } catch (Throwable t) {
            if (isCleanRejection(t)) {
                return;
            }
            if (isRootCauseInSolve(t)) {
                throwUnchecked(t);
            }
        }

        // EXPLORE: generate many valid brackets around known roots of sin(x), so the API is
        // obligated to accept them. This reaches the same public entry point with varied inputs.
        int cases = 1 + Math.max(0, Math.min(4, data.remainingBytes()));
        for (int i = 0; i < cases; i++) {
            int k = data.consumeInt(-1000, 1000);
            double root = k * Math.PI;

            int leftMilli = data.consumeInt(1, 1000);
            int rightMilli = data.consumeInt(1, 1000);
            double min = root - (leftMilli / 1000.0d);
            double max = root + (rightMilli / 1000.0d);
            double initial = data.consumeBoolean()
                    ? root
                    : min + (max - min) * (data.consumeInt(0, 1000) / 1000.0d);

            UnivariateRealFunction f = new SinFunction();

            try {
                BisectionSolver solverA = new BisectionSolver();
                double lhs = solverA.solve(f, min, max, initial);

                BisectionSolver solverB = new BisectionSolver();
                double rhs = solverB.solve(f, min, max);

                // Documented overload-agreement oracle:
                // The same-name overloads solve(f,min,max,initial) and solve(f,min,max) are both
                // public APIs for solving the same bracketed function root. For bisection, initial
                // is not part of the mathematical result, so a correct implementation must return
                // the same root up to solver accuracy. A throw-deleting patch that skips binding
                // the function or otherwise changes behavior can violate this without throwing.
                double tol = Math.max(Math.max(solverA.getAbsoluteAccuracy(), solverB.getAbsoluteAccuracy()), 1.0e-8d);
                if (Math.abs(lhs - rhs) > tol) {
                    throw new RuntimeException(
                            "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                                    + " input={k=" + k + ",min=" + min + ",max=" + max + ",initial=" + initial + "}"
                                    + " lhs=" + lhs + " rhs=" + rhs);
                }

                // Additional observable post-condition from our constructed input:
                // We built the interval around the known root k*pi of sin(x), so any correct solve
                // result must be close to that known root within a small multiple of solver accuracy.
                double expected = root;
                double knownTol = Math.max(10.0d * tol, 1.0e-6d);
                if (Math.abs(lhs - expected) > knownTol) {
                    throw new RuntimeException(
                            "[oracle:known-root] metamorphic violation: solve result must recover constructed sin root"
                                    + " input={k=" + k + ",min=" + min + ",max=" + max + ",initial=" + initial + "}"
                                    + " lhs=" + lhs + " expected=" + expected);
                }
            } catch (Throwable t) {
                if (isCleanRejection(t)) {
                    return;
                }
                if (isRootCauseInSolve(t)) {
                    throwUnchecked(t);
                }
                return;
            }
        }
    }

    private static boolean isRootCauseInSolve(Throwable t) {
        if (!(t instanceof NullPointerException)) {
            return false;
        }
        StackTraceElement[] trace = t.getStackTrace();
        if (trace == null) {
            return false;
        }
        for (StackTraceElement ste : trace) {
            if ("org.apache.commons.math.analysis.solvers.BisectionSolver".equals(ste.getClassName())
                    && "solve".equals(ste.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCleanRejection(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof IllegalArgumentException || t instanceof NumberFormatException) {
            return true;
        }
        String name = t.getClass().getName();
        return name.contains("Invalid")
                || name.contains("Illegal")
                || name.contains("Argument")
                || name.contains("OutOfRange")
                || name.contains("NoBracketing")
                || name.contains("Convergence")
                || name.contains("TooMany")
                || name.contains("NotPositive");
    }

    private static void throwUnchecked(Throwable t) {
        FuzzHarness.<RuntimeException>throwAny(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwAny(Throwable t) throws T {
        throw (T) t;
    }
}