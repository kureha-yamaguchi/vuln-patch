package org.apache.commons.math.analysis.solvers;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.SinFunction;
import org.apache.commons.math.analysis.UnivariateRealFunction;

public class FuzzHarness {
    public static void fuzzerTestOneInput(com.code_intelligence.jazzer.api.FuzzedDataProvider data) {
        UnivariateRealFunction f = new SinFunction();

        // ANCHOR: exact failing test input from BisectionSolverTest.testMath369.
        runOneValidCase(f, 3.0, 3.2, 3.1, true);

        // EXPLORE: valid-by-construction intervals that bracket a real root of sin(x).
        // Property: f is non-null and [min, max] brackets k*pi, with initial inside [min, max].
        int k = data.consumeInt(-1000, 1000);
        double root = k * Math.PI;

        double left = 0.01 + (data.consumeInt(0, 10000) / 10000.0) * 5.0;
        double right = 0.01 + (data.consumeInt(0, 10000) / 10000.0) * 5.0;

        double min = root - left;
        double max = root + right;

        double fraction = data.consumeInt(0, 10000) / 10000.0;
        double initial = min + (max - min) * fraction;

        runOneValidCase(f, min, max, initial, false);
    }

    private static void runOneValidCase(UnivariateRealFunction f, double min, double max, double initial, boolean anchor) {
        Double withInitial = callSolveWithInitial(f, min, max, initial);
        Double withoutInitial = callSolveWithoutInitial(f, min, max);

        // Documented same-name overload agreement: solve(f,min,max,initial) and solve(f,min,max)
        // are sibling APIs for the same solving task on the same function and interval.
        // For a correct BisectionSolver implementation, the "initial" value is not semantically
        // required to change the root returned for a valid bracketing interval. A patch that
        // merely suppresses the crash but skips the real solving behavior would break this.
        if (withInitial == null || withoutInitial == null) {
            return;
        }

        double lhs = withInitial.doubleValue();
        double rhs = withoutInitial.doubleValue();

        if (!Double.isNaN(lhs) && !Double.isNaN(rhs) && Double.isFinite(lhs) && Double.isFinite(rhs)) {
            double tol = Math.max(new BisectionSolver().getAbsoluteAccuracy(), 1.0e-6);
            if (Math.abs(lhs - rhs) > tol) {
                throw new RuntimeException(
                    "[oracle:overload-agreement] metamorphic violation: solve(f,min,max,initial) must agree with solve(f,min,max)"
                        + " input=min=" + min
                        + ", max=" + max
                        + ", initial=" + initial
                        + ", anchor=" + anchor
                        + ", lhs=" + lhs
                        + ", rhs=" + rhs);
            }
        }

        // Stronger anchor-specific post-condition from the original regression test:
        // the root of sin(x) in [3.0, 3.2] is pi, so the solver should return approximately pi.
        if (anchor) {
            double tol = Math.max(new BisectionSolver().getAbsoluteAccuracy(), 1.0e-6);
            if (Math.abs(lhs - Math.PI) > tol) {
                throw new RuntimeException(
                    "[oracle:anchor-pi] metamorphic violation: expected root near PI for sin(x) on [3.0,3.2]"
                        + " input=min=" + min
                        + ", max=" + max
                        + ", initial=" + initial
                        + ", result=" + lhs
                        + ", expected=" + Math.PI);
            }
        }
    }

    private static Double callSolveWithInitial(UnivariateRealFunction f, double min, double max, double initial) {
        try {
            BisectionSolver solver = new BisectionSolver();
            return Double.valueOf(solver.solve(f, min, max, initial));
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return null;
            }
            return null;
        }
    }

    private static Double callSolveWithoutInitial(UnivariateRealFunction f, double min, double max) {
        try {
            BisectionSolver solver = new BisectionSolver();
            return Double.valueOf(solver.solve(f, min, max));
        } catch (Throwable t) {
            if (isRootCause(t)) {
                throwUnchecked(t);
            }
            if (isCleanRejection(t)) {
                return null;
            }
            return null;
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
        for (int i = 0; i < trace.length; i++) {
            StackTraceElement e = trace[i];
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
                || name.contains("Invalid")
                || name.contains("OutOfRange")
                || name.contains("NoBracketing")
                || name.contains("TooMany")
                || name.contains("Illegal");
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